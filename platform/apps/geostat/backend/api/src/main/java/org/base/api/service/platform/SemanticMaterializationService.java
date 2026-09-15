package org.base.api.service.platform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.HashMap;

/** Contract-governed conversion from raw snapshot records to a named physical data family. */
@Service
public class SemanticMaterializationService {
    private static final Pattern SEMANTIC_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
    private static final Pattern BCP47_TAG = Pattern.compile("[A-Za-z]{2,3}(-[A-Za-z0-9]{2,8})*");
    private final JdbcTemplate controlPlane;
    private final JdbcTemplate dataPlane;
    private final ObjectMapper json;

    public SemanticMaterializationService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane,
                                          @Qualifier("dataPlaneJdbcTemplate") JdbcTemplate dataPlane,
                                          ObjectMapper json) {
        this.controlPlane = controlPlane;
        this.dataPlane = dataPlane;
        this.json = json;
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public MaterializationReceipt materialize(MaterializeRequest request) {
        if (request.datasetSnapshotId() <= 0) throw new IllegalArgumentException("Snapshot identifier must be positive");
        long contractSourceId = request.contractSourceId() > 0 ? request.contractSourceId() : resolveSourceForSnapshot(request.datasetSnapshotId());
        /* IDs live in two independent tables.  Resolve the immutable revision
           binding first (R8 is revision-governed); only fall back to the
           legacy contract_source table for an explicitly supplied legacy id.
           This prevents numeric-ID collisions from silently applying a
           different dataset version or provisional mapping. */
        Map<String, Object> contract = controlPlane.query("SELECT target_dataset_version_id,mapping_spec_json FROM platform.contract_revision_source WHERE contract_revision_source_id=?",
                rs -> rs.next() ? Map.of("version", rs.getLong(1), "mapping", rs.getString(2)) : null, contractSourceId);
        if (contract == null) contract = controlPlane.query("SELECT target_dataset_version_id,mapping_spec_json FROM platform.contract_source WHERE contract_source_id=? AND active=1",
                rs -> rs.next() ? Map.of("version", rs.getLong(1), "mapping", rs.getString(2)) : null, contractSourceId);
        if (contract == null) throw new IllegalArgumentException("Active contract source not found");
        Long actualVersion = dataPlane.query("SELECT dataset_version_id FROM publication.dataset_snapshot WHERE dataset_snapshot_id=? AND status IN ('REVIEW_REQUIRED','SEMANTIC_REVIEW')",
                rs -> rs.next() ? rs.getLong(1) : null, request.datasetSnapshotId());
        if (actualVersion == null || actualVersion.longValue() != ((Long) contract.get("version"))) throw new IllegalStateException("Snapshot is not ready or does not match contract version");
        try {
            JsonNode mapping = json.readTree((String) contract.get("mapping"));
            String approvalState = mapping.path("approvalState").asText();
            /* PROVISIONAL_APPROVED is permitted only through this staging
               boundary; publication remains blocked until the explicit
               quality/privacy/reconciliation and steward gates pass. */
            boolean stagingApproved = "PROVISIONAL_APPROVED".equalsIgnoreCase(approvalState);
            if (!approvalState.isBlank() && !"READY".equalsIgnoreCase(approvalState) && !stagingApproved) {
                throw new IllegalStateException("Semantic mapping is not ready for materialization: " + approvalState);
            }
            long written = 0;
            boolean multiple = mapping.path("projections").isArray();
            if (multiple) {
                if (mapping.path("projections").isEmpty()) throw new IllegalArgumentException("A projection list must not be empty");
                for (JsonNode projection : mapping.path("projections")) written += materialize(request.datasetSnapshotId(), projection);
            } else written = materialize(request.datasetSnapshotId(), mapping);
            String family = multiple ? "MULTI" : mapping.path("family").asText("ENTITY").toUpperCase();
            dataPlane.update("UPDATE publication.dataset_snapshot SET status='SEMANTIC_REVIEW' WHERE dataset_snapshot_id=?", request.datasetSnapshotId());
            return new MaterializationReceipt(request.datasetSnapshotId(), contractSourceId, family, written, "SEMANTIC_REVIEW");
        } catch (java.io.IOException exception) {
            throw new IllegalArgumentException("Invalid mapping_spec_json", exception);
        }
    }

    /** Resolve the source binding from the snapshot's governed dataset version;
     * callers must not guess surrogate contract-source IDs. */
    private long resolveSourceForSnapshot(long snapshotId) {
        Long version = dataPlane.query("SELECT dataset_version_id FROM publication.dataset_snapshot WHERE dataset_snapshot_id=?", rs -> rs.next() ? rs.getLong(1) : null, snapshotId);
        if (version == null) throw new IllegalArgumentException("Snapshot not found: " + snapshotId);
        String sourceName = dataPlane.query("SELECT source_name FROM ingest.dataset_load l JOIN publication.dataset_snapshot s ON s.dataset_load_id=l.dataset_load_id WHERE s.dataset_snapshot_id=?", rs -> rs.next() ? rs.getString(1) : null, snapshotId);
        Long source = sourceName == null ? null : controlPlane.query("SELECT TOP 1 contract_revision_source_id FROM platform.contract_revision_source WHERE target_dataset_version_id=? AND source_locator IN (?,?) AND (JSON_VALUE(mapping_spec_json,'$.approvalState') IN ('READY','PROVISIONAL_APPROVED') OR JSON_VALUE(mapping_spec_json,'$.approvalState') IS NULL) ORDER BY CASE WHEN source_locator=? THEN 0 ELSE 1 END,contract_revision_source_id DESC", rs -> rs.next() ? rs.getLong(1) : null, version, "ACCESS."+sourceName, sourceName, "ACCESS."+sourceName);
        if (source == null) source = controlPlane.query("SELECT TOP 1 contract_revision_source_id FROM platform.contract_revision_source WHERE target_dataset_version_id=? AND (JSON_VALUE(mapping_spec_json,'$.canonicalAccessRevision') IN ('7','8') OR source_locator LIKE 'ACCESS.__%') AND (JSON_VALUE(mapping_spec_json,'$.approvalState') IN ('READY','PROVISIONAL_APPROVED') OR JSON_VALUE(mapping_spec_json,'$.approvalState') IS NULL) ORDER BY contract_revision_source_id DESC", rs -> rs.next() ? rs.getLong(1) : null, version);
        if (source == null) {
            source = controlPlane.query("SELECT TOP 1 contract_revision_source_id FROM platform.contract_revision_source WHERE target_dataset_version_id=? ORDER BY contract_revision_source_id DESC", rs -> rs.next() ? rs.getLong(1) : null, version);
        }
        if (source == null && sourceName != null) {
            source = controlPlane.query("SELECT TOP 1 contract_revision_source_id FROM platform.contract_revision_source WHERE target_dataset_version_id=? AND JSON_VALUE(mapping_spec_json,'$.canonicalAccessRevision')=8 AND (JSON_VALUE(mapping_spec_json,'$.approvalState')='READY' OR JSON_VALUE(mapping_spec_json,'$.approvalState') IS NULL) AND (source_locator=? OR source_locator=? OR source_locator=? OR source_locator=?) ORDER BY contract_revision_source_id DESC", rs -> rs.next() ? rs.getLong(1) : null, version, sourceName, "ACCESS."+sourceName, unprefix(sourceName), "ACCESS."+unprefix(sourceName));
        }
        if (source == null) source = controlPlane.query("SELECT TOP 1 contract_source_id FROM platform.contract_source WHERE target_dataset_version_id=? AND active=1 ORDER BY contract_source_id DESC", rs -> rs.next() ? rs.getLong(1) : null, version);
        if (source == null) throw new IllegalStateException("No governed contract source for dataset version " + version);
        return source;
    }

    private static String unprefix(String value) {
        if (value == null) return null;
        int dot = value.lastIndexOf('.');
        String v = dot >= 0 ? value.substring(dot + 1) : value;
        return v.startsWith("__") ? v.substring(2) : v;
    }

    private long materialize(long datasetSnapshotId, JsonNode mapping) {
        String family = mapping.path("family").asText("ENTITY").toUpperCase();
        return switch (family) {
            case "ENTITY" -> materializeEntities(datasetSnapshotId, mapping);
            case "RELATION" -> materializeRelations(datasetSnapshotId, mapping);
            case "CLASSIFICATION" -> materializeClassificationAssignments(datasetSnapshotId, mapping);
            case "STATISTICAL" -> mapping.path("projectionModel").asText("").startsWith("SDMX_COMPATIBLE")
                    ? materializeKidsStatisticalInputs(datasetSnapshotId, mapping) : materializeObservations(datasetSnapshotId, mapping);
            case "STATISTICAL_WIDE_JSON" -> materializeWideJsonObservations(datasetSnapshotId, mapping);
            case "GEO" -> materializeFeatures(datasetSnapshotId, mapping);
            /* Reference/classifier and raw transport rows are already handled
               by their dedicated proposal/archive pipelines.  They must not
               be coerced into entity facts or silently dropped. */
            case "REFERENCE", "RAW" -> 0L;
            default -> throw new UnsupportedOperationException("Unsupported semantic family: " + family);
        };
    }

    private long materializeEntities(long datasetSnapshotId, JsonNode mapping) {
        String recordType = required(mapping, "recordType");
        String keyField = required(mapping, "key");
        String titleField = mapping.path("title").asText("");
        List<Map<String, Object>> rows = dataPlane.queryForList("SELECT source_record_id,payload_json,payload_hash FROM raw.source_record WHERE dataset_snapshot_id=? ORDER BY source_record_id", datasetSnapshotId);
        long written = 0;
        for (Map<String, Object> row : rows) {
            try {
                JsonNode payload = json.readTree((String) row.get("payload_json"));
                String externalKey = field(payload, keyField);
                if (externalKey == null || externalKey.isBlank()) throw new IllegalArgumentException("Required entity key is absent: " + keyField);
                String title = titleField.isBlank() ? null : field(payload, titleField);
                long entityId = dataPlane.queryForObject("INSERT INTO entity.entity_record(dataset_snapshot_id,external_key,record_type,title,payload_json,payload_hash,source_record_id) " +
                                "OUTPUT INSERTED.entity_id VALUES(?,?,?,?,?,?,?)", Long.class,
                        datasetSnapshotId, externalKey, recordType, title, row.get("payload_json"), row.get("payload_hash"), row.get("source_record_id"));
                bindClassifications(entityId, mapping.path("classifications"), payload, ((Number) row.get("source_record_id")).longValue());
                bindLocalizedTexts(entityId, mapping.path("localizedText"), payload, ((Number) row.get("source_record_id")).longValue());
                bindResourceLocators(entityId, mapping.path("locators"), payload, ((Number) row.get("source_record_id")).longValue());
                written++;
            } catch (Exception conversionError) {
                throw new IllegalArgumentException("Entity materialization failed for raw source row " + row.get("source_record_id"), conversionError);
            }
        }
        return written;
    }

    /** Localized fields are structured, BCP-47 tagged content—not generic key/value rows. */
    private void bindLocalizedTexts(long entityId, JsonNode bindings, JsonNode payload, long sourceRecordId) {
        if (!bindings.isArray()) return;
        for (JsonNode binding : bindings) {
            String fieldCode = required(binding, "fieldCode").toUpperCase();
            String language = required(binding, "language");
            if (!SEMANTIC_CODE.matcher(fieldCode).matches()) throw new IllegalArgumentException("Invalid localized field code: " + fieldCode);
            if (!BCP47_TAG.matcher(language).matches()) throw new IllegalArgumentException("Invalid BCP-47 language tag: " + language);
            String value = field(payload, required(binding, "path"));
            if (value == null || value.isBlank()) {
                if (binding.path("required").asBoolean(true)) throw new IllegalArgumentException("Required localized value is absent: " + fieldCode + "/" + language);
                continue;
            }
            dataPlane.update("INSERT INTO entity.localized_text(entity_id,field_code,language_tag,text_value,source_record_id) VALUES(?,?,?,?,?)",
                    entityId, fieldCode, language, value, sourceRecordId);
        }
    }

    /** A locator is a governed typed URI/path with language context and direct source lineage. */
    private void bindResourceLocators(long entityId, JsonNode bindings, JsonNode payload, long sourceRecordId) {
        if (!bindings.isArray()) return;
        for (JsonNode binding : bindings) {
            String kind = required(binding, "kind").toUpperCase();
            String language = binding.path("language").asText("und");
            if (!SEMANTIC_CODE.matcher(kind).matches()) throw new IllegalArgumentException("Invalid locator kind: " + kind);
            if (!"und".equals(language) && !BCP47_TAG.matcher(language).matches()) throw new IllegalArgumentException("Invalid BCP-47 language tag: " + language);
            String value = field(payload, required(binding, "path"));
            if (value == null || value.isBlank()) {
                if (binding.path("required").asBoolean(true)) throw new IllegalArgumentException("Required resource locator is absent: " + kind + "/" + language);
                continue;
            }
            if (value.length() > 2048) throw new IllegalArgumentException("Resource locator exceeds 2048 characters");
            dataPlane.update("INSERT INTO entity.resource_locator(entity_id,locator_kind,language_tag,locator,source_record_id) VALUES(?,?,?,?,?)",
                    entityId, kind, language, value, sourceRecordId);
        }
    }

    /** Resolves controlled vocabulary values through the authoritative Core alias registry. */
    private void bindClassifications(long entityId, JsonNode bindings, JsonNode payload, long sourceRecordId) {
        if (!bindings.isArray()) return;
        for (JsonNode binding : bindings) {
            long attributeId = binding.path("attributeId").asLong(0);
            if (attributeId <= 0) throw new IllegalArgumentException("Classification binding requires attributeId");
            long itemId = binding.path("classificationItemId").asLong(0);
            if (itemId <= 0) {
                String system = required(binding, "externalSystemCode");
                String code = field(payload, required(binding, "code"));
                if (code == null) throw new IllegalArgumentException("Classification code is absent");
                Long resolved = classificationId(system, code);
                if (resolved == null) throw new IllegalArgumentException("Unmapped classification code: " + system + "/" + code);
                itemId = resolved;
            }
            dataPlane.update("INSERT INTO entity.entity_classification(entity_id,attribute_id,classification_item_id,source_record_id) VALUES(?,?,?,?)",
                    entityId, attributeId, itemId, sourceRecordId);
        }
    }

    private long materializeRelations(long datasetSnapshotId, JsonNode mapping) {
        long relationshipTypeId = mapping.path("relationshipTypeId").asLong(0);
        String fromField = required(mapping, "fromKey");
        String toField = required(mapping, "toKey");
        if (relationshipTypeId <= 0) throw new IllegalArgumentException("Relation mapping requires relationshipTypeId");
        long written = 0;
        for (Map<String, Object> row : dataPlane.queryForList("SELECT source_record_id,payload_json FROM raw.source_record WHERE dataset_snapshot_id=?", datasetSnapshotId)) {
            try {
                JsonNode payload = json.readTree((String) row.get("payload_json"));
                Long from = entityId(field(payload, fromField));
                Long to = entityId(field(payload, toField));
                if (from == null || to == null) throw new IllegalArgumentException("Relation endpoint is absent from this snapshot");
                dataPlane.update("INSERT INTO entity.entity_link(from_entity_id,relationship_type_id,to_entity_id,source_record_id) VALUES(?,?,?,?)", from, relationshipTypeId, to, row.get("source_record_id"));
                written++;
            } catch (Exception error) { throw new IllegalArgumentException("Relation materialization failed for raw source row " + row.get("source_record_id"), error); }
        }
        return written;
    }

    /** Entity-to-controlled-vocabulary membership. This is deliberately distinct
     * from entity_link: a classifier item is not an entity endpoint. */
    private long materializeClassificationAssignments(long datasetSnapshotId, JsonNode mapping) {
        String entityKeyField = required(mapping, "entityKey");
        String codeField = required(mapping, "classificationCode");
        String system = required(mapping, "externalSystemCode");
        long attributeId = mapping.path("attributeId").asLong(0);
        if (attributeId <= 0) throw new IllegalArgumentException("Classification mapping requires attributeId");
        long written = 0;
        for (Map<String,Object> row : dataPlane.queryForList("SELECT source_record_id,payload_json FROM raw.source_record WHERE dataset_snapshot_id=? ORDER BY source_record_id", datasetSnapshotId)) {
            try {
                JsonNode payload = json.readTree((String) row.get("payload_json"));
                Long entity = entityId(field(payload, entityKeyField));
                Long item = classificationId(system, field(payload, codeField));
                if (entity == null) throw new IllegalArgumentException("Classification entity is absent: " + entityKeyField);
                if (item == null) throw new IllegalArgumentException("Unmapped classification code: " + system + "/" + field(payload, codeField));
                dataPlane.update("IF NOT EXISTS (SELECT 1 FROM entity.entity_classification WHERE entity_id=? AND attribute_id=? AND classification_item_id=? AND source_record_id=?) INSERT INTO entity.entity_classification(entity_id,attribute_id,classification_item_id,source_record_id) VALUES(?,?,?,?)",
                        entity, attributeId, item, row.get("source_record_id"), entity, attributeId, item, row.get("source_record_id"));
                written++;
            } catch (Exception error) { throw new IllegalArgumentException("Classification materialization failed for raw source row " + row.get("source_record_id"), error); }
        }
        return written;
    }

    private long materializeObservations(long datasetSnapshotId, JsonNode mapping) {
        long metricId = mapping.path("metricId").asLong(0);
        String valueField = required(mapping, "value");
        String keyField = mapping.path("seriesKey").asText("");
        String periodField = mapping.path("periodStart").asText("");
        if (metricId <= 0) throw new IllegalArgumentException("Statistical mapping requires metricId");
        long written = 0;
        for (Map<String, Object> row : dataPlane.queryForList("SELECT source_record_id,source_key,payload_json FROM raw.source_record WHERE dataset_snapshot_id=?", datasetSnapshotId)) {
            try {
                JsonNode payload = json.readTree((String) row.get("payload_json"));
                String seriesKey = keyField.isBlank() ? String.valueOf(row.get("source_key")) : field(payload, keyField);
                if (seriesKey == null) throw new IllegalArgumentException("Statistical series key is absent");
                String hash = sha256(metricId + "|" + seriesKey);
                Long seriesId = dataPlane.query("SELECT series_id FROM [statistics].series WHERE dataset_snapshot_id=? AND metric_id=? AND series_key_hash=?", rs -> rs.next() ? rs.getLong(1) : null, datasetSnapshotId, metricId, hash);
                if (seriesId == null) seriesId = dataPlane.queryForObject("INSERT INTO [statistics].series(dataset_snapshot_id,metric_id,series_key_hash,status,source_record_id) OUTPUT INSERTED.series_id VALUES(?,?,?,'VALID',?)", Long.class, datasetSnapshotId, metricId, hash, row.get("source_record_id"));
                String value = field(payload, valueField);
                if (value == null) throw new IllegalArgumentException("Statistical value is absent");
                String period = periodField.isBlank() ? null : field(payload, periodField);
                long observationId = dataPlane.queryForObject("INSERT INTO [statistics].observation(series_id,period_start,numeric_value,source_record_id) OUTPUT INSERTED.observation_id VALUES(?,?,?,?)", Long.class, seriesId, period, new BigDecimal(value), row.get("source_record_id"));
                bindObservationDimensions(observationId, mapping.path("dimensions"), payload);
                written++;
            } catch (Exception error) { throw new IllegalArgumentException("Statistical materialization failed for raw source row " + row.get("source_record_id"), error); }
        }
        return written;
    }

    /** KIDS canonical long-form cells: metric is bound per carrier, while each
     * input row contributes one TIME_PERIOD × AGE_GROUP observation. */
    private long materializeKidsStatisticalInputs(long datasetSnapshotId, JsonNode mapping) {
        long dimensionId = mapping.path("ageDimensionId").asLong(1);
        String carrierField = mapping.path("carrierField").asText("carrier_code");
        String periodField = mapping.path("periodField").asText("period_normalized");
        String ageField = mapping.path("ageField").asText("age_group_item_ref");
        String valueField = mapping.path("valueField").asText("value_decimal");
        long written = 0;
        for (Map<String,Object> row : dataPlane.queryForList("SELECT source_record_id,payload_json FROM raw.source_record WHERE dataset_snapshot_id=? ORDER BY source_record_id", datasetSnapshotId)) {
            try {
                JsonNode payload = json.readTree((String) row.get("payload_json"));
                String carrier = field(payload, carrierField);
                if (carrier == null || carrier.isBlank()) throw new IllegalArgumentException("carrier_code is absent");
                String resource = carrier.startsWith("CARRIER|") ? carrier.substring("CARRIER|".length()) : carrier;
                Long metric = resolveMetric(mapping, carrier, resource);
                if (metric == null) throw new IllegalArgumentException("No approved metric for carrier " + carrier);
                String periodText = field(payload, periodField);
                String age = field(payload, ageField);
                String value = field(payload, valueField);
                if (periodText == null || age == null || value == null || value.isBlank()) throw new IllegalArgumentException("Incomplete statistical cell");
                LocalDate period = period(periodText);
                String dimensionSystem = required(mapping, "ageClassificationSystem");
                Long item = classificationId(dimensionSystem, age);
                if (item == null) throw new IllegalArgumentException("Unmapped AGE_GROUP: " + age);
                String hash = sha256(metric + "|" + carrier);
                Long seriesId = dataPlane.query("SELECT series_id FROM [statistics].series WHERE dataset_snapshot_id=? AND metric_id=? AND series_key_hash=?", rs -> rs.next() ? rs.getLong(1) : null, datasetSnapshotId, metric, hash);
                String unit = controlPlane.query("SELECT me.unit_code FROM platform.metric m JOIN platform.measure me ON me.measure_id=m.measure_id WHERE m.metric_id=?", rs -> rs.next() ? rs.getString(1) : null, metric);
                if (seriesId == null) seriesId = dataPlane.queryForObject("INSERT INTO [statistics].series(dataset_snapshot_id,metric_id,series_key_hash,unit_code,status,source_record_id) OUTPUT INSERTED.series_id VALUES(?,?,?,?,'VALID',?)", Long.class, datasetSnapshotId, metric, hash, unit, row.get("source_record_id"));
                Long existing = dataPlane.query("SELECT TOP 1 o.observation_id FROM [statistics].observation o JOIN [statistics].observation_dimension d ON d.observation_id=o.observation_id WHERE o.series_id=? AND o.period_start=? AND d.dimension_id=? AND d.classification_item_id=?", rs -> rs.next() ? rs.getLong(1) : null, seriesId, period, dimensionId, item);
                if (existing == null) {
                    long observation = dataPlane.queryForObject("INSERT INTO [statistics].observation(series_id,period_start,numeric_value,source_record_id) OUTPUT INSERTED.observation_id VALUES(?,?,?,?)", Long.class, seriesId, period, new BigDecimal(value), row.get("source_record_id"));
                    dataPlane.update("INSERT INTO [statistics].observation_dimension(observation_id,dimension_id,classification_item_id,scalar_code) VALUES(?,?,?,?)", observation, dimensionId, item, age);
                    written++;
                }
            } catch (Exception error) { throw new IllegalArgumentException("KIDS statistical materialization failed for raw source row " + row.get("source_record_id") + ": " + error.getMessage(), error); }
        }
        return written;
    }

    /** Resolves the metric exclusively from the issued contract mapping. No site,
     * resource-id switch or KIDS-specific metric list is embedded in execution code. */
    private Long resolveMetric(JsonNode mapping, String carrier, String resource) {
        JsonNode lookup = mapping.path("metricLookup");
        String code = lookup.path("metricCode").asText("");
        if (code.isBlank()) {
            String pattern = lookup.path("metricCodePattern").asText("");
            if (!pattern.isBlank()) code = pattern.replace("{carrierCode}", carrier).replace("{resourceId}", resource);
        }
        if (code.isBlank()) throw new IllegalArgumentException("Statistical mapping must declare metricLookup.metricCode or metricCodePattern");
        return controlPlane.query("SELECT TOP 1 metric_id FROM platform.metric WHERE metric_code=? AND status<> 'DRAFT' ORDER BY metric_id DESC", rs -> rs.next() ? rs.getLong(1) : null, code);
    }

    /**
     * Normalizes a reviewed JSON array whose columns are dimension codes (for example age bands).
     * The method resolves both metric and dimension codes through Core aliases and never creates
     * metadata from incoming values.
     */
    private long materializeWideJsonObservations(long datasetSnapshotId, JsonNode mapping) {
        String arrayPath = required(mapping, "arrayPath");
        String periodField = required(mapping, "periodField");
        JsonNode metric = mapping.path("metric");
        JsonNode pivot = mapping.path("pivotDimension");
        String metricSystem = required(metric, "externalSystemCode");
        String metricCodePath = required(metric, "code");
        String seriesKeyPath = mapping.path("seriesKey").asText(metricCodePath);
        long dimensionId = pivot.path("dimensionId").asLong(0);
        String dimensionSystem = required(pivot, "externalSystemCode");
        if (dimensionId <= 0) throw new IllegalArgumentException("Wide JSON projection requires pivotDimension.dimensionId");
        long written = 0;
        HashSet<String> uniqueness = new HashSet<>();
        for (Map<String, Object> row : dataPlane.queryForList("SELECT source_record_id,source_key,payload_json FROM raw.source_record WHERE dataset_snapshot_id=? ORDER BY source_record_id", datasetSnapshotId)) {
            try {
                JsonNode payload = json.readTree((String) row.get("payload_json"));
                JsonNode embedded = value(payload, arrayPath);
                if (embedded == null || embedded.isNull() || embedded.isMissingNode() || embedded.asText().isBlank()) continue;
                JsonNode array = embeddedJson(embedded);
                if (!array.isArray()) throw new IllegalArgumentException("Wide JSON source is not an array at: " + arrayPath);
                String externalMetricCode = field(payload, metricCodePath);
                Long metricId = metricId(metricSystem, externalMetricCode);
                if (metricId == null) throw new IllegalArgumentException("Unmapped or inactive metric: " + metricSystem + "/" + externalMetricCode);
                String seriesKey = field(payload, seriesKeyPath);
                if (seriesKey == null || seriesKey.isBlank()) seriesKey = String.valueOf(row.get("source_key"));
                String seriesHash = sha256(metricId + "|" + seriesKey);
                Long seriesId = dataPlane.query("SELECT series_id FROM [statistics].series WHERE dataset_snapshot_id=? AND metric_id=? AND series_key_hash=?", rs -> rs.next() ? rs.getLong(1) : null, datasetSnapshotId, metricId, seriesHash);
                if (seriesId == null) seriesId = dataPlane.queryForObject("INSERT INTO [statistics].series(dataset_snapshot_id,metric_id,series_key_hash,status,source_record_id) OUTPUT INSERTED.series_id VALUES(?,?,?,'VALID',?)", Long.class, datasetSnapshotId, metricId, seriesHash, row.get("source_record_id"));
                for (JsonNode item : array) {
                    if (!item.isObject()) throw new IllegalArgumentException("Wide JSON array contains a non-object item");
                    LocalDate period = period(field(item, periodField));
                    var fields = item.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        if (field.getKey().equals(periodField) || field.getValue().isNull()) continue;
                        BigDecimal numeric = new BigDecimal(field.getValue().asText());
                        String normalizedDimensionCode = normalizeExternalCode(field.getKey());
                        Long classificationItemId = classificationId(dimensionSystem, normalizedDimensionCode);
                        if (classificationItemId == null) throw new IllegalArgumentException("Unmapped pivot dimension: " + dimensionSystem + "/" + field.getKey());
                        String duplicateKey = metricId + "|" + seriesKey + "|" + period + "|" + dimensionId + "|" + classificationItemId;
                        if (!uniqueness.add(duplicateKey)) throw new IllegalArgumentException("Duplicate statistical observation tuple: " + duplicateKey);
                        long observationId = dataPlane.queryForObject("INSERT INTO [statistics].observation(series_id,period_start,numeric_value,source_record_id) OUTPUT INSERTED.observation_id VALUES(?,?,?,?)", Long.class, seriesId, period, numeric, row.get("source_record_id"));
                        dataPlane.update("INSERT INTO [statistics].observation_dimension(observation_id,dimension_id,classification_item_id,scalar_code) VALUES(?,?,?,?)", observationId, dimensionId, classificationItemId, field.getKey());
                        written++;
                    }
                }
            } catch (Exception error) { throw new IllegalArgumentException("Wide JSON statistical materialization failed for raw source row " + row.get("source_record_id"), error); }
        }
        return written;
    }

    private Long metricId(String externalSystemCode, String externalCode) {
        if (externalCode == null || externalCode.isBlank()) return null;
        return controlPlane.query("SELECT m.metric_id FROM platform.metric_alias a JOIN platform.metric m ON m.metric_id=a.metric_id WHERE a.external_system_code=? AND a.external_code=? AND m.status='ACTIVE' AND (a.valid_from IS NULL OR a.valid_from<=CAST(SYSUTCDATETIME() AS DATE)) AND (a.valid_to IS NULL OR a.valid_to>=CAST(SYSUTCDATETIME() AS DATE))", rs -> rs.next() ? rs.getLong(1) : null, externalSystemCode, normalizeExternalCode(externalCode));
    }

    private Long classificationId(String externalSystemCode, String externalCode) {
        String code = normalizeExternalCode(externalCode);
        Long resolved = classificationAlias(externalSystemCode, code);
        if (resolved == null && code.contains("|")) {
            String[] parts = code.split("\\|");
            resolved = classificationAlias(externalSystemCode, parts[parts.length - 1]);
        }
        return resolved;
    }

    private Long classificationAlias(String system, String code) {
        return controlPlane.query("SELECT a.classification_item_id FROM platform.classification_alias a JOIN platform.classification_item i ON i.classification_item_id=a.classification_item_id JOIN platform.classification_version v ON v.classification_version_id=i.classification_version_id WHERE a.external_system_code=? AND a.external_code=? AND i.status='ACTIVE' AND v.status IN ('PUBLISHED','APPROVED') AND (a.valid_from IS NULL OR a.valid_from<=CAST(SYSUTCDATETIME() AS DATE)) AND (a.valid_to IS NULL OR a.valid_to>=CAST(SYSUTCDATETIME() AS DATE))", rs -> rs.next() ? rs.getLong(1) : null, system, normalizeExternalCode(code));
    }

    /** Lookup normalization is deliberate and minimal; the unmodified source key stays in scalar_code and raw JSON. */
    private static String normalizeExternalCode(String value) {
        if (value == null) return null;
        String normalized = value.strip();
        if (normalized.isBlank()) throw new IllegalArgumentException("External code is blank after normalization");
        return normalized;
    }

    private static LocalDate period(String source) {
        if (source == null || source.isBlank()) throw new IllegalArgumentException("Statistical period is absent");
        return source.matches("\\d{4}") ? LocalDate.of(Integer.parseInt(source), 1, 1) : LocalDate.parse(source);
    }

    /**
     * Legacy Access exports can carry an array either as JSON text or as an
     * escaped JSON string.  The raw payload stays untouched; only the typed
     * projection decodes the second representation explicitly.
     */
    private JsonNode embeddedJson(JsonNode value) throws java.io.IOException {
        if (!value.isTextual()) return value;
        String raw = value.asText();
        try { return json.readTree(raw); }
        catch (java.io.IOException directFailure) {
            String decoded = json.readValue("\"" + raw.replace("\"", "\\\"") + "\"", String.class);
            return json.readTree(decoded);
        }
    }

    private void bindObservationDimensions(long observationId, JsonNode dimensions, JsonNode payload) {
        if (!dimensions.isArray()) return;
        for (JsonNode dimension : dimensions) {
            long dimensionId = dimension.path("dimensionId").asLong(0);
            if (dimensionId <= 0) throw new IllegalArgumentException("Statistical dimension requires dimensionId");
            String scalar = field(payload, required(dimension, "code"));
            if (scalar == null) throw new IllegalArgumentException("Statistical dimension code is absent");
            Long classificationItem = null;
            if (!dimension.path("externalSystemCode").asText().isBlank()) {
                classificationItem = classificationId(dimension.path("externalSystemCode").asText(), scalar);
                if (classificationItem == null) throw new IllegalArgumentException("Unmapped dimension classification: " + scalar);
            }
            dataPlane.update("INSERT INTO [statistics].observation_dimension(observation_id,dimension_id,classification_item_id,scalar_code) VALUES(?,?,?,?)", observationId, dimensionId, classificationItem, scalar);
        }
    }

    private long materializeFeatures(long datasetSnapshotId, JsonNode mapping) {
        String geometryField = required(mapping, "geometry");
        String typeField = required(mapping, "geometryType");
        String keyField = mapping.path("featureKey").asText("");
        long written = 0;
        for (Map<String, Object> row : dataPlane.queryForList("SELECT source_record_id,source_key,payload_json FROM raw.source_record WHERE dataset_snapshot_id=?", datasetSnapshotId)) {
            try {
                JsonNode payload = json.readTree((String) row.get("payload_json"));
                JsonNode geometry = value(payload, geometryField);
                if (geometry == null || geometry.isMissingNode() || geometry.isNull()) throw new IllegalArgumentException("Geometry is absent");
                String key = keyField.isBlank() ? (String) row.get("source_key") : field(payload, keyField);
                String type = field(payload, typeField);
                if (type == null) throw new IllegalArgumentException("Geometry type is absent");
                dataPlane.update("INSERT INTO geo.feature(dataset_snapshot_id,feature_key,geometry_geojson,geometry_type,source_record_id) VALUES(?,?,?,?,?)", datasetSnapshotId, key, geometry.toString(), type, row.get("source_record_id"));
                written++;
            } catch (Exception error) { throw new IllegalArgumentException("Geo materialization failed for raw source row " + row.get("source_record_id"), error); }
        }
        return written;
    }

    private Long entityId(String externalKey) {
        if (externalKey == null) return null;
        return dataPlane.query("SELECT TOP 1 entity_id FROM entity.entity_record WHERE external_key=? AND is_current=1 ORDER BY entity_id DESC", rs -> rs.next() ? rs.getLong(1) : null, externalKey);
    }

    private static String required(JsonNode mapping, String name) {
        String value = mapping.path(name).asText();
        if (value.isBlank()) throw new IllegalArgumentException("Mapping requires '" + name + "'");
        return value;
    }

    private static String field(JsonNode root, String expression) {
        JsonNode cursor = value(root, expression);
        return cursor == null || cursor.isMissingNode() || cursor.isNull() ? null : cursor.asText();
    }

    private static JsonNode value(JsonNode root, String expression) {
        JsonNode cursor = root;
        for (String segment : expression.replace("$.", "").split("\\.")) {
            cursor = cursor == null ? null : cursor.path(segment);
        }
        return cursor;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder output = new StringBuilder(64);
            for (byte item : digest) output.append(String.format("%02x", item));
            return output.toString();
        } catch (Exception exception) { throw new IllegalStateException("SHA-256 unavailable", exception); }
    }
}
