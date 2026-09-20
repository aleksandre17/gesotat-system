package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Read-only, machine-readable capability view of an approved contract.
 * This is the public discovery boundary for generated clients and form builders;
 * it never exposes physical table names or SQL.
 */
@Service
public class ContractIntrospectionService {
    /** The table an include would expand into; capabilities advertise only what this caller may read. */
    private String relationTarget(long revisionId, String relationCode) {
        List<String> target = db.query("SELECT TOP 1 to_dataset_code FROM platform.site_contract_relation WHERE site_contract_revision_id=? AND relation_code=?",
                (r, n) -> r.getString(1), revisionId, relationCode);
        return target.isEmpty() ? relationCode : target.get(0);
    }

    private final JdbcTemplate db;
    private final ServingPolicy serving;

    public ContractIntrospectionService(@Qualifier("primaryJdbcTemplate") JdbcTemplate db, ServingPolicy serving) {
        this.serving = serving;
        this.db = db;
    }

    public Map<String, Object> contract(String code, Integer requestedRevision) {
        Map<String, Object> revision = revision(code, requestedRevision);
        long revisionId = ((Number) revision.get("siteContractRevisionId")).longValue();
        List<Map<String, Object>> pages = db.query(
                "SELECT b.runtime_page_id,n.node_code,n.node_kind,n.dataset_code,n.path_segment,b.response_projection_code " +
                        "FROM platform.contract_page_binding b JOIN platform.site_contract_node n ON n.node_id=b.node_id " +
                        "WHERE b.site_contract_revision_id=? AND b.status='ACTIVE' ORDER BY b.runtime_page_id",
                (r, n) -> map("pageId", r.getInt(1), "nodeCode", r.getString(2), "nodeKind", r.getString(3),
                        "datasetCode", r.getString(4), "path", "/" + r.getString(5), "responseProjection", r.getString(6)), revisionId);
        for (Map<String, Object> page : pages) {
            String dataset = (String) page.get("datasetCode");
            if (dataset != null) page.put("capabilities", capabilities(revisionId, dataset, String.valueOf(page.get("nodeKind"))));
        }
        Map<String, Object> out = new LinkedHashMap<>(revision);
        out.put("pages", pages);
        out.put("schema", map("id", "urn:geostat:" + code.toLowerCase(Locale.ROOT) + ":v" + revision.get("revision"),
                "version", String.valueOf(revision.get("revision")), "mediaType", "application/json"));
        return out;
    }

    public List<Map<String, Object>> pages(String code, Integer revision) {
        Object value = contract(code, revision).get("pages");
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    public Map<String, Object> pageCapabilities(String code, int pageId, Integer requestedRevision) {
        Map<String, Object> c = contract(code, requestedRevision);
        for (Map<String, Object> page : pagesFrom(c)) {
            if (Objects.equals(((Number) page.get("pageId")).intValue(), pageId)) return page;
        }
        throw new IllegalArgumentException("Page is not declared by the approved contract: " + pageId);
    }

    private List<Map<String, Object>> pagesFrom(Map<String, Object> c) {
        Object value = c.get("pages");
        return value instanceof List<?> list ? (List<Map<String, Object>>) list : List.of();
    }

    private Map<String, Object> revision(String code, Integer requested) {
        String sql = "SELECT TOP 1 site_contract_revision_id,contract_code,revision,status,effective_from,contract_checksum " +
                "FROM platform.site_contract_revision WHERE contract_code=? AND status='APPROVED' " +
                (requested == null ? "" : "AND revision=? ") + "ORDER BY revision DESC";
        Map<String, Object> row = requested == null
                ? db.queryForMap(sql, code)
                : db.queryForMap(sql, code, requested);
        return map("siteContractRevisionId", row.get("site_contract_revision_id"), "contractCode", row.get("contract_code"),
                "revision", row.get("revision"), "status", row.get("status"), "effectiveFrom", row.get("effective_from"),
                "checksum", row.get("contract_checksum"));
    }

    private Map<String, Object> capabilities(long revisionId, String dataset, String nodeKind) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("datasetCode", dataset);
        out.put("nodeKind", nodeKind);
        out.put("fields", db.query("SELECT f.field_name,f.logical_type,f.semantic_role,f.required,f.key_role,f.classifier_scheme_code FROM platform.site_contract_field f JOIN platform.site_contract_dataset d ON d.contract_dataset_id=f.contract_dataset_id WHERE d.site_contract_revision_id=? AND d.dataset_code=? ORDER BY f.ordinal",
                (r, n) -> map("name", r.getString(1), "logicalType", r.getString(2), "semanticRole", r.getString(3),
                        "required", r.getBoolean(4), "keyRole", r.getString(5), "classifierScheme", r.getString(6)), revisionId, dataset));
        out.put("relations", db.query("SELECT relation_code,from_dataset_code,from_field_name,to_dataset_code,to_field_name,relation_kind,cardinality,required FROM platform.site_contract_relation WHERE site_contract_revision_id=? AND (from_dataset_code=? OR to_dataset_code=?) ORDER BY load_priority",
                (r, n) -> map("code", r.getString(1), "fromDataset", r.getString(2), "fromField", r.getString(3),
                        "toDataset", r.getString(4), "toField", r.getString(5), "kind", r.getString(6),
                        "cardinality", r.getString(7), "required", r.getBoolean(8)), revisionId, dataset, dataset));
        out.put("allowedFilters", db.query("SELECT f.field_name FROM platform.site_contract_field f JOIN platform.site_contract_dataset d ON d.contract_dataset_id=f.contract_dataset_id WHERE d.site_contract_revision_id=? AND d.dataset_code=? ORDER BY f.ordinal",
                (r, n) -> r.getString(1), revisionId, dataset));
        List<String> includes=new ArrayList<>(); includes.addAll(db.query("SELECT relation_code FROM platform.site_contract_relation WHERE site_contract_revision_id=? AND (from_dataset_code=? OR to_dataset_code=?) ORDER BY load_priority",(r,n)->r.getString(1),revisionId,dataset,dataset)); includes.removeIf(code -> !serving.permits(revisionId, relationTarget(revisionId, code))); out.put("allowedIncludes", includes);
        // Aggregation capability is contract/registry driven.  COUNT is the
        // universal cardinality operation; measure-specific operations come
        // only from the approved metric registry for this dataset.  This
        // deliberately avoids family/node-name branching so a new provider
        // or data family can advertise its own semantic capabilities.
        List<String> aggregations = new ArrayList<>(List.of("COUNT"));
        aggregations.addAll(db.query("SELECT DISTINCT m.aggregation FROM platform.metric m JOIN platform.dataset d ON d.dataset_id=m.source_dataset_id JOIN platform.site_contract_dataset cd ON cd.dataset_code=d.dataset_code WHERE cd.site_contract_revision_id=? AND d.dataset_code=? AND m.status IN ('APPROVED','PROVISIONAL_APPROVED') AND m.aggregation IS NOT NULL ORDER BY m.aggregation",
                (r, n) -> r.getString(1), revisionId, dataset));
        out.put("allowedAggregations", aggregations.stream().distinct().toList());
        List<String> aliases=db.query("SELECT alias_code FROM platform.contract_query_alias WHERE site_contract_revision_id=? AND dataset_code=? AND status='APPROVED' ORDER BY alias_code",(r,n)->r.getString(1),revisionId,dataset);
        @SuppressWarnings("unchecked") List<String> filters=(List<String>)out.get("allowedFilters"); List<String> virtual=new ArrayList<>(filters); virtual.addAll(aliases); out.put("allowedFilters",virtual);
        return out;
    }

    private static Map<String, Object> map(Object... values) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) out.put(String.valueOf(values[i]), values[i + 1]);
        return out;
    }
}
