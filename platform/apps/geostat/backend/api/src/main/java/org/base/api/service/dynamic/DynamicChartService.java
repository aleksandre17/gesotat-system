package org.base.api.service.dynamic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.base.core.entity.data.ChartDefinition;
import org.base.core.entity.data.DataMode;
import org.base.core.entity.data.PublicationStatus;
import org.base.core.entity.page_tree.PageLeafNode;
import org.base.core.repository.ChartDefinitionRepository;
import org.base.core.repository.DataProfileRepository;
import org.base.core.repository.PageNodeRepository;
import org.springframework.stereotype.Service;

import java.sql.DriverManager;
import java.util.*;
import java.util.regex.Pattern;

/** Converts a published declarative chart definition into a safe aggregate query. */
@Service
@RequiredArgsConstructor
public class DynamicChartService {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern ENDPOINT = Pattern.compile("[A-Za-z0-9._-]+(?::[0-9]{1,5})?");
    private static final Pattern DATABASE = Pattern.compile("[A-Za-z_][A-Za-z0-9_-]{0,127}");
    private static final Set<String> AGGREGATIONS = Set.of("SUM", "AVG", "COUNT");
    private static final int MAX_FILTER_JSON_BYTES = 64 * 1024;
    private static final int MAX_FILTER_RULES = 32;
    private final DataProfileRepository profileRepository;
    private final ChartDefinitionRepository chartRepository;
    private final PageNodeRepository pageNodeRepository;
    private final ObjectMapper objectMapper;

    public DynamicChartResponse read(Long pageId, String chartCode) {
        var profile = profileRepository.findByPageId(pageId).orElseThrow(() -> new IllegalArgumentException("No data profile for page"));
        if (!profile.isEnabled() || profile.getDataMode() == DataMode.LEGACY) throw new IllegalStateException("Dynamic data is disabled");
        ChartDefinition chart = chartRepository.findByProfileIdAndPublicationStatus(profile.getId(), PublicationStatus.PUBLISHED).stream()
                .filter(candidate -> candidate.getChartCode().equals(chartCode)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Published chart not found: " + chartCode));
        List<String> allowed = allowed(profile.getDisplayColumnsJson());
        assertAllowed(chart.getXField(), allowed);
        assertAllowed(chart.getYField(), allowed);
        if (chart.getSeriesField() != null) assertAllowed(chart.getSeriesField(), allowed);
        String aggregation = chart.getAggregation() == null ? "SUM" : chart.getAggregation().toUpperCase(Locale.ROOT);
        if (!AGGREGATIONS.contains(aggregation)) throw new IllegalStateException("Unsupported chart aggregation");
        PageLeafNode node = pageNodeRepository.findById(pageId).filter(PageLeafNode.class::isInstance).map(PageLeafNode.class::cast)
                .orElseThrow(() -> new IllegalArgumentException("PAGE not found"));
        QuerySpec query = sql(node.getMetaDatabaseType(), profile.getTargetSchema(), profile.getTargetTable(), chart, aggregation, allowed);
        try (var connection = DriverManager.getConnection(url(node.getMetaDatabaseType(), node.getMetaDatabaseUrl(), profile.getTargetDatabase()), node.getMetaDatabaseUser(), node.getMetaDatabasePassword());
             var statement = connection.prepareStatement(query.sql())) {
            for (int i = 0; i < query.parameters().size(); i++) statement.setObject(i + 1, query.parameters().get(i));
            try (var rs = statement.executeQuery()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) { Map<String, Object> row = new LinkedHashMap<>(); for (int i=1;i<=rs.getMetaData().getColumnCount();i++) row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i)); rows.add(row); }
            return new DynamicChartResponse(chart.getChartCode(), chart.getChartType().name(), rows);
            }
        } catch (Exception e) { throw new IllegalStateException("Unable to read chart data", e); }
    }

    private List<String> allowed(String json) { try { List<String> fields=objectMapper.readValue(json, new TypeReference<List<String>>() {}); if(fields.size()>128) throw new IllegalStateException("Chart profile exceeds field budget"); return fields; } catch (Exception e) { throw new IllegalStateException("Invalid displayColumns profile", e); } }
    private void assertAllowed(String field, List<String> allowed) { if (field == null || !allowed.contains(field) || !IDENTIFIER.matcher(field).matches()) throw new IllegalStateException("Chart field is not allowed by profile"); }
    private QuerySpec sql(String type, String schema, String table, ChartDefinition chart, String aggregation, List<String> allowed) {
        String qx = quote(chart.getXField(), type), qy = quote(chart.getYField(), type), qs = chart.getSeriesField() == null ? null : quote(chart.getSeriesField(), type);
        String select = qx + " AS x" + (qs == null ? "" : ", " + qs + " AS series") + ", " + aggregation + "(" + qy + ") AS value";
        String from = "mssql".equalsIgnoreCase(type) ? quote(schema, type) + "." + quote(table, type) : quote(table, type);
        String group = qx + (qs == null ? "" : ", " + qs);
        List<Object> parameters = new ArrayList<>();
        String where = filters(chart.getFiltersJson(), allowed, type, parameters);
        return new QuerySpec("SELECT " + select + " FROM " + from + where + " GROUP BY " + group + " ORDER BY " + group, parameters);
    }
    private String filters(String json, List<String> allowed, String type, List<Object> parameters) {
        if (json == null || json.isBlank()) return "";
        if (json.length() > MAX_FILTER_JSON_BYTES) throw new IllegalStateException("Chart filters exceed size budget");
        try {
            List<Map<String, Object>> rules = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            if (rules.size() > MAX_FILTER_RULES) throw new IllegalStateException("Chart filters exceed rule budget");
            List<String> clauses = new ArrayList<>();
            for (Map<String, Object> rule : rules) {
                String field = String.valueOf(rule.get("field")); String op = String.valueOf(rule.get("operator")).toUpperCase(Locale.ROOT);
                assertAllowed(field, allowed);
                if (op.equals("IS_NULL")) clauses.add(quote(field, type) + " IS NULL");
                else if (op.equals("NOT_NULL")) clauses.add(quote(field, type) + " IS NOT NULL");
                else if (op.equals("EQ") || op.equals("NE")) { clauses.add(quote(field, type) + (op.equals("EQ") ? " = ?" : " <> ?")); parameters.add(rule.get("value")); }
                else throw new IllegalStateException("Unsupported chart filter operator");
            }
            return clauses.isEmpty() ? "" : " WHERE " + String.join(" AND ", clauses);
        } catch (IllegalStateException e) { throw e; } catch (Exception e) { throw new IllegalStateException("Invalid chart filters", e); }
    }
    private record QuerySpec(String sql, List<Object> parameters) { }
    private String quote(String id, String type) { if (!IDENTIFIER.matcher(id).matches()) throw new IllegalStateException("Unsafe identifier"); return "mssql".equalsIgnoreCase(type) ? "["+id+"]" : "`"+id+"`"; }
    private String url(String type, String host, String db) { String clean=host==null?"":host.replaceFirst("^jdbc:(sqlserver|mysql)://", "").split(";")[0]; if(!ENDPOINT.matcher(clean).matches()||!DATABASE.matcher(db==null?"":db).matches()) throw new IllegalStateException("Invalid governed database endpoint"); return "mssql".equalsIgnoreCase(type) ? "jdbc:sqlserver://"+clean+";databaseName="+db+";encrypt=true;trustServerCertificate=false" : "jdbc:mysql://"+clean+"/"+db+"?useSSL=true&requireSSL=true&verifyServerCertificate=true&allowPublicKeyRetrieval=false"; }
}
