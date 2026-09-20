package org.base.api.service.platform.statistical.access;

import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.IndexBuilder;
import com.healthmarketscience.jackcess.PropertyMap;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.ProviderCapabilities;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PhysicalColumn;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PhysicalTable;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Access (ACCDB) authoring adapter. Emits the typed tables of an approved plan and reads a filled file back
 * as component-keyed rows. Exact measures use the Access Decimal type, never Double. The embedded contract
 * stamp is a transport copy: a file whose stamp differs from the approved plan is refused, so edited
 * metadata can never become authority.
 */
public final class AccessAuthoringAdapter {
    public static final String SOURCE_PROFILE = "ACCESS_ACCDB";
    public static final String STAMP_TABLE = "__stat_contract";
    /**
     * ACCDB 2010 format: opened by Access 2010 and every later build, and the format of every other generator in
     * this repository. The 2016 format flag is refused by Access 2016 release builds ("requires a newer version").
     */
    public static final Database.FileFormat FILE_FORMAT = Database.FileFormat.V2010;
    private static final short COMBO_BOX = 111;
    /** Navigation Pane vocabulary of Access: a custom category holds named groups of objects. */
    private static final int CUSTOM_CATEGORY = 4, TABLE_OBJECT = 1;
    public static final String CATEGORY = "GEOSTAT";
    public static final String GROUP_DATA = "შესავსები მონაცემები";
    public static final String GROUP_SCHEMA = "იდენტობა და სქემა";
    public static final String GROUP_CODELISTS = "კლასიფიკატორები";
    public static final String GROUP_LINEAGE = "წყაროს კვალი";
    /** Identity and schema copy of the approved contract, under the canonical names of the Access package plan. */
    public static final String PACKAGE_TABLE = "__gs_package", DATASET_TABLE = "__gs_dataset", FIELD_TABLE = "__gs_field",
            KEY_TABLE = "__gs_key", RELATION_TABLE = "__gs_relation", PAGE_TABLE = "__gs_page", PROJECTION_TABLE = "__gs_projection",
            RAW_DOCUMENT_TABLE = "__raw_document";

    public record CodeItem(String code, String label) { }

    public enum ReadIssue { CONTRACT_STAMP_MISSING, CONTRACT_STAMP_MISMATCH, TABLE_MISSING, COLUMN_MISSING, SPLIT_PART_INCOMPLETE }

    public record ReadResult(List<Map<String, Object>> rows, List<String> issues) {
        public boolean accepted() { return issues.isEmpty(); }
    }

    /** The adapter's own declaration of what an ACCDB can hold; the capability registry stores this declaration. */
    public static ProviderCapabilities capabilities() {
        return new ProviderCapabilities(SOURCE_PROFILE, 255, 64, 10, 28, true, Set.of(
                "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "BETWEEN", "BY", "COLUMN", "COUNT", "CREATE", "CURRENCY", "DATE",
                "DELETE", "DESC", "DISTINCT", "DROP", "EXISTS", "FROM", "GROUP", "HAVING", "IN", "INDEX", "INSERT", "INTO", "IS",
                "JOIN", "KEY", "LEVEL", "LIKE", "MEMO", "NAME", "NOT", "NULL", "NUMBER", "ON", "OPTION", "OR", "ORDER", "PERCENT",
                "SELECT", "SET", "TABLE", "TEXT", "TIME", "TOP", "UNION", "UPDATE", "USER", "VALUE", "VALUES", "WHERE", "YEAR", "YES", "NO"));
    }

    public static ProviderCapabilities.Catalog catalog() {
        return profile -> SOURCE_PROFILE.equals(profile) ? Optional.of(capabilities()) : Optional.empty();
    }

    public void emit(SemanticPlan plan, Map<Ref, List<CodeItem>> codelists, String captionLanguage, File target) throws IOException {
        if (!SOURCE_PROFILE.equals(plan.physical().providerCode())) throw new IllegalArgumentException("plan was not compiled for " + SOURCE_PROFILE);
        try (Database db = DatabaseBuilder.create(FILE_FORMAT, target)) {
            Map<Ref, String> lookupTables = new LinkedHashMap<>();
            for (PlannedComponent c : plan.components())
                if (c.isAuthoringColumn() && c.representation() instanceof Representation.Coded coded && !lookupTables.containsKey(coded.codelistRef()))
                    lookupTables.put(coded.codelistRef(), codelistTable(db, coded.codelistRef(), codelists, lookupTables.size()));
            for (PhysicalTable table : plan.physical().tables()) dataTable(db, plan, table, lookupTables, captionLanguage);
            stamp(db, plan);
            contractCopy(db, plan);
            navigationGroups(db, plan, lookupTables.values());
        }
    }

    private String codelistTable(Database db, Ref ref, Map<Ref, List<CodeItem>> codelists, int ordinal) throws IOException {
        List<CodeItem> items = codelists.get(ref);
        if (items == null) throw new IllegalArgumentException("codelist content is required for " + ref);
        String name = "__cl_" + (ordinal + 1) + "_" + ref.code().substring(0, Math.min(40, ref.code().length()));
        Table table = new TableBuilder(name)
                .addColumn(new ColumnBuilder("code", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("label", DataType.TEXT).setLengthInUnits(255))
                .addIndex(new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME).addColumns("code").setPrimaryKey())
                .putProperty(PropertyMap.DESCRIPTION_PROP, ref.wire())
                .toTable(db);
        for (CodeItem item : items) table.addRow(item.code(), item.label());
        return name;
    }

    private void dataTable(Database db, SemanticPlan plan, PhysicalTable physical, Map<Ref, String> lookups, String language) throws IOException {
        TableBuilder builder = new TableBuilder(physical.name());
        List<String> key = new ArrayList<>();
        for (PhysicalColumn column : physical.columns()) {
            if (column.componentCode() == null) {
                builder.addColumn(new ColumnBuilder(column.name(), DataType.LONG).setAutoNumber(true));
                builder.addIndex(new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME).addColumns(column.name()).setPrimaryKey());
                continue;
            }
            PlannedComponent c = plan.component(column.componentCode());
            ColumnBuilder col = typed(column.name(), c.representation());
            String caption = plan.captions().getOrDefault(c.code(), Map.of()).get(language);
            if (caption != null) col.putProperty(PropertyMap.CAPTION_PROP, caption);
            col.putProperty(PropertyMap.DESCRIPTION_PROP, c.code());
            if (c.role() != Component.Role.DIMENSION) col.putProperty(PropertyMap.REQUIRED_PROP, false);
            if (c.representation() instanceof Representation.Coded coded) lookup(col, lookups.get(coded.codelistRef()));
            builder.addColumn(col);
            if (c.role() == Component.Role.DIMENSION) key.add(column.name());
        }
        if (!physical.generatedRowRef())
            builder.addIndex(new IndexBuilder("uq_observation_key").addColumns(key.toArray(String[]::new)).setUnique());
        builder.toTable(db);
    }

    private static ColumnBuilder typed(String name, Representation r) {
        if (r instanceof Representation.Numeric n)
            return n.approximate() ? new ColumnBuilder(name, DataType.DOUBLE)
                    : new ColumnBuilder(name, DataType.NUMERIC).setPrecision(n.precision()).setScale(n.scale());
        if (r instanceof Representation.IntegerRange range)
            return range.min() >= Integer.MIN_VALUE && range.max() <= Integer.MAX_VALUE ? new ColumnBuilder(name, DataType.LONG)
                    : new ColumnBuilder(name, DataType.NUMERIC).setPrecision(19).setScale(0);
        int length = r instanceof Representation.BoundedText text ? text.maxLength() : r instanceof Representation.TimePeriod ? 32 : 120;
        return new ColumnBuilder(name, DataType.TEXT).setLengthInUnits(length);
    }

    /** Stores the stable code, shows code and label; declarative field properties only, no VBA or macro. */
    private static void lookup(ColumnBuilder col, String lookupTable) {
        col.putProperty("DisplayControl", DataType.INT, COMBO_BOX);
        col.putProperty("RowSourceType", DataType.TEXT, "Table/Query");
        col.putProperty("RowSource", DataType.MEMO, "SELECT [code], [label] FROM [" + lookupTable + "] ORDER BY [code]");
        col.putProperty("BoundColumn", DataType.INT, (short) 1);
        col.putProperty("ColumnCount", DataType.INT, (short) 2);
        col.putProperty("LimitToList", DataType.BOOLEAN, true);
    }

    /**
     * The identity and schema of the approved contract, carried into the file as a read-only copy (Access package
     * plan): what this package is, which dataset it fills, which fields and key the contract declares, and the
     * places provenance will be recorded. The copy is never authority — import compares it with the Control Plane
     * and refuses a file whose copy was edited — so it is written once and shown, not filled.
     */
    private void contractCopy(Database db, SemanticPlan plan) throws IOException {
        Table packages = new TableBuilder(PACKAGE_TABLE)
                .addColumn(new ColumnBuilder("package_code", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("profile_ref", DataType.TEXT).setLengthInUnits(255))
                .addColumn(new ColumnBuilder("contract_revision_digest", DataType.TEXT).setLengthInUnits(64))
                .addColumn(new ColumnBuilder("generated_at", DataType.TEXT).setLengthInUnits(32))
                .putProperty(PropertyMap.DESCRIPTION_PROP, "Identity of this authoring package; read-only copy of the approved contract.")
                .toTable(db);
        packages.addRow(plan.datasetNamespace() + "." + plan.datasetCode(), plan.profileRef().wire(), plan.revisionDigest(),
                java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());

        Table datasets = new TableBuilder(DATASET_TABLE)
                .addColumn(new ColumnBuilder("dataset_code", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("namespace_code", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("structure_ref", DataType.TEXT).setLengthInUnits(255))
                .addColumn(new ColumnBuilder("physical_table_name", DataType.TEXT).setLengthInUnits(120))
                .putProperty(PropertyMap.DESCRIPTION_PROP, "The dataset this package fills and the table that holds it.")
                .toTable(db);
        for (PhysicalTable table : plan.physical().tables())
            datasets.addRow(plan.datasetCode(), plan.datasetNamespace(), plan.structureRef().wire(), table.name());

        Table fields = new TableBuilder(FIELD_TABLE)
                .addColumn(new ColumnBuilder("table_name", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("field_name", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("component_role", DataType.TEXT).setLengthInUnits(24))
                .addColumn(new ColumnBuilder("value_type", DataType.TEXT).setLengthInUnits(32))
                .addColumn(new ColumnBuilder("required", DataType.BOOLEAN))
                .addColumn(new ColumnBuilder("concept_ref", DataType.TEXT).setLengthInUnits(255))
                .addColumn(new ColumnBuilder("measure_ref", DataType.TEXT).setLengthInUnits(255))
                .addColumn(new ColumnBuilder("unit_ref", DataType.TEXT).setLengthInUnits(255))
                .addColumn(new ColumnBuilder("codelist_ref", DataType.TEXT).setLengthInUnits(255))
                .addColumn(new ColumnBuilder("caption", DataType.TEXT).setLengthInUnits(255))
                .putProperty(PropertyMap.DESCRIPTION_PROP, "Every field the contract declares, with the meaning behind it.")
                .toTable(db);
        Table keys = new TableBuilder(KEY_TABLE)
                .addColumn(new ColumnBuilder("table_name", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("field_name", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("key_position", DataType.LONG))
                .putProperty(PropertyMap.DESCRIPTION_PROP, "The dimensions that identify one row; their order is the contract's.")
                .toTable(db);
        for (PhysicalTable table : plan.physical().tables()) {
            int keyPosition = 0;
            for (PhysicalColumn column : table.columns()) {
                if (column.componentCode() == null) continue;
                PlannedComponent c = plan.component(column.componentCode());
                fields.addRow(table.name(), column.name(), c.role().name(), c.representation().logicalType(), c.required(),
                        c.conceptRef() == null ? null : c.conceptRef().wire(), c.measureRef() == null ? null : c.measureRef().wire(),
                        c.unitRef() == null ? null : c.unitRef().wire(),
                        c.representation() instanceof Representation.Coded coded ? coded.codelistRef().wire() : null,
                        plan.captions().getOrDefault(c.code(), Map.of()).get(captionLanguageOf(plan)));
                if (c.role() == Component.Role.DIMENSION) keys.addRow(table.name(), column.name(), ++keyPosition);
            }
        }

        // Declared, and empty in an authoring file: a statistical structure carries no relations, pages or
        // projections of its own, and provenance is written when the filled file is loaded, not before.
        new TableBuilder(RELATION_TABLE)
                .addColumn(new ColumnBuilder("relation_code", DataType.TEXT).setLengthInUnits(160))
                .addColumn(new ColumnBuilder("from_field", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("to_table", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("to_field", DataType.TEXT).setLengthInUnits(120))
                .putProperty(PropertyMap.DESCRIPTION_PROP, "Declared relations of the contract; empty for a single-structure authoring file.")
                .toTable(db);
        new TableBuilder(PAGE_TABLE)
                .addColumn(new ColumnBuilder("page_code", DataType.TEXT).setLengthInUnits(160))
                .addColumn(new ColumnBuilder("dataset_code", DataType.TEXT).setLengthInUnits(120))
                .putProperty(PropertyMap.DESCRIPTION_PROP, "Pages that serve this dataset; filled by the platform, not by the author.")
                .toTable(db);
        new TableBuilder(PROJECTION_TABLE)
                .addColumn(new ColumnBuilder("projection_code", DataType.TEXT).setLengthInUnits(160))
                .addColumn(new ColumnBuilder("dataset_code", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("projection_model", DataType.TEXT).setLengthInUnits(64))
                .putProperty(PropertyMap.DESCRIPTION_PROP, "Declared projections over this dataset; filled by the platform.")
                .toTable(db);
        new TableBuilder(RAW_DOCUMENT_TABLE)
                .addColumn(new ColumnBuilder("document_identity", DataType.TEXT).setLengthInUnits(255))
                .addColumn(new ColumnBuilder("original_filename", DataType.TEXT).setLengthInUnits(255))
                .addColumn(new ColumnBuilder("payload_checksum", DataType.TEXT).setLengthInUnits(64))
                .addColumn(new ColumnBuilder("received_at", DataType.TEXT).setLengthInUnits(32))
                .putProperty(PropertyMap.DESCRIPTION_PROP, "Provenance of the loaded file; written by the platform on load.")
                .toTable(db);
    }

    /** The language whose captions this file carries; the generator wrote them, so any caption identifies it. */
    private static String captionLanguageOf(SemanticPlan plan) {
        return plan.captions().values().stream().flatMap(m -> m.keySet().stream()).findFirst().orElse("ka");
    }

    /**
     * Groups the objects in the Navigation Pane so the author sees where data is entered and which tables only
     * describe the approved contract (Access package plan). Access keeps this in its own system tables; the rows
     * are written here, so no template file and no macro is needed. Grouping is presentation: it grants no right
     * and hides nothing, and a file whose groups were lost still opens and still loads.
     */
    private void navigationGroups(Database db, SemanticPlan plan, Collection<String> codelistTables) throws IOException {
        Table categories = db.getSystemTable("MSysNavPaneGroupCategories");
        Table groups = db.getSystemTable("MSysNavPaneGroups");
        Table links = db.getSystemTable("MSysNavPaneGroupToObjects");
        Table objectIds = db.getSystemTable("MSysNavPaneObjectIDs");
        if (categories == null || groups == null || links == null || objectIds == null) return; // older provider: tables only

        Map<String, Integer> knownObjects = new LinkedHashMap<>();
        int maxObjectId = 0;
        for (Row row : objectIds) {
            knownObjects.put(String.valueOf(row.get("Name")), (Integer) row.get("Id"));
            maxObjectId = Math.max(maxObjectId, (Integer) row.get("Id"));
        }
        Map<String, List<String>> plannedGroups = new LinkedHashMap<>();
        plannedGroups.put(GROUP_DATA, plan.physical().tables().stream().map(PhysicalTable::name).toList());
        plannedGroups.put(GROUP_SCHEMA, List.of(PACKAGE_TABLE, DATASET_TABLE, FIELD_TABLE, KEY_TABLE, RELATION_TABLE, PAGE_TABLE, PROJECTION_TABLE, STAMP_TABLE));
        plannedGroups.put(GROUP_CODELISTS, List.copyOf(codelistTables));
        plannedGroups.put(GROUP_LINEAGE, List.of(RAW_DOCUMENT_TABLE));

        int categoryId = nextId(categories), groupId = nextId(groups), linkId = nextId(links), position = 0;
        categories.addRow(null, 0, categoryId, CATEGORY, 0, null, CUSTOM_CATEGORY);
        // Opening the file shows this category, whose first group is the one the author fills (Access package plan).
        db.getDatabaseProperties().put("NavPane Category", DataType.LONG, categoryId);
        db.getDatabaseProperties().save();
        for (Map.Entry<String, List<String>> group : plannedGroups.entrySet()) {
            if (group.getValue().isEmpty()) continue;
            int id = groupId++;
            groups.addRow(0, categoryId, id, group.getKey(), 0, 0, position++);
            int inner = 0;
            for (String object : group.getValue()) {
                Integer objectId = knownObjects.get(object);
                if (objectId == null) {
                    objectId = ++maxObjectId;
                    objectIds.addRow(objectId, object, TABLE_OBJECT);
                    knownObjects.put(object, objectId);
                }
                links.addRow(0, id, 0, linkId++, null, objectId, inner++);
            }
        }
    }

    private static int nextId(Table table) throws IOException {
        int max = 0;
        for (Row row : table) max = Math.max(max, (Integer) row.get("Id"));
        return max + 1;
    }

    private static void stamp(Database db, SemanticPlan plan) throws IOException {
        Table table = new TableBuilder(STAMP_TABLE)
                .addColumn(new ColumnBuilder("stamp_key", DataType.TEXT).setLengthInUnits(64))
                .addColumn(new ColumnBuilder("stamp_value", DataType.MEMO))
                .addIndex(new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME).addColumns("stamp_key").setPrimaryKey())
                .toTable(db);
        for (Map.Entry<String, String> e : stampOf(plan).entrySet()) table.addRow(e.getKey(), e.getValue());
    }

    private static Map<String, String> stampOf(SemanticPlan plan) {
        Map<String, String> stamp = new TreeMap<>();
        stamp.put("profile", plan.profileRef().wire());
        stamp.put("dataset", plan.datasetNamespace() + ":" + plan.datasetCode());
        stamp.put("structure", plan.structureRef().wire());
        stamp.put("semanticDigest", plan.semanticDigest());
        stamp.put("revisionDigest", plan.revisionDigest());
        return stamp;
    }

    public ReadResult read(File filled, SemanticPlan plan) throws IOException {
        List<String> issues = new ArrayList<>();
        try (Database db = new DatabaseBuilder(filled).setReadOnly(true).open()) {
            Table stampTable = db.getTableNames().contains(STAMP_TABLE) ? db.getTable(STAMP_TABLE) : null;
            if (stampTable == null) return new ReadResult(List.of(), List.of(ReadIssue.CONTRACT_STAMP_MISSING.name()));
            Map<String, String> found = new TreeMap<>();
            for (Row row : stampTable) found.put(row.getString("stamp_key"), row.getString("stamp_value"));
            if (!found.equals(stampOf(plan))) return new ReadResult(List.of(), List.of(ReadIssue.CONTRACT_STAMP_MISMATCH.name()));

            List<String> keyCodes = plan.components().stream()
                    .filter(c -> c.role() == Component.Role.DIMENSION && c.isAuthoringColumn()).map(PlannedComponent::code).toList();
            Map<List<Object>, Map<String, Object>> joined = new LinkedHashMap<>();
            Map<List<Object>, Integer> partsSeen = new LinkedHashMap<>();
            List<Map<String, Object>> single = new ArrayList<>();
            for (PhysicalTable physical : plan.physical().tables()) {
                if (!db.getTableNames().contains(physical.name())) { issues.add(ReadIssue.TABLE_MISSING + ":" + physical.name()); continue; }
                Table table = db.getTable(physical.name());
                for (PhysicalColumn column : physical.columns())
                    if (table.getColumns().stream().noneMatch(c -> c.getName().equals(column.name()))) issues.add(ReadIssue.COLUMN_MISSING + ":" + physical.name() + "." + column.name());
                if (!issues.isEmpty()) continue;
                for (Row row : table) {
                    Map<String, Object> values = new LinkedHashMap<>();
                    for (PhysicalColumn column : physical.columns())
                        if (column.componentCode() != null) values.put(column.componentCode(), row.get(column.name()));
                    if (plan.physical().tables().size() == 1) { single.add(values); continue; } // duplicates are the normalizer's finding
                    List<Object> key = keyCodes.stream().map(code -> (Object) String.valueOf(values.get(code))).toList();
                    joined.computeIfAbsent(key, k -> new LinkedHashMap<>()).putAll(values);
                    partsSeen.merge(key, 1, Integer::sum);
                }
            }
            int parts = plan.physical().tables().size();
            partsSeen.forEach((key, seen) -> { if (seen != parts) issues.add(ReadIssue.SPLIT_PART_INCOMPLETE + ":" + key); });
            if (!issues.isEmpty()) return new ReadResult(List.of(), List.copyOf(issues));
            return new ReadResult(parts == 1 ? List.copyOf(single) : List.copyOf(joined.values()), List.of());
        }
    }
}
