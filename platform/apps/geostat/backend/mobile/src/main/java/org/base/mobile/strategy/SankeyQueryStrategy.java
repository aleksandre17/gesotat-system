package org.base.mobile.strategy;

import org.base.mobile.ClassificationTableConfig;
import org.base.mobile.params.RoadLengthParams;
import org.base.mobile.params.SankeyParams;
import org.base.mobile.params.QueryParams;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Strategy for sankey endpoint query, optimized for MSSQL.
 */
/**
 * Strategy for sankey query, optimized for MSSQL.
 */
@Component("sankeyStrategy")
public class SankeyQueryStrategy implements TableQueryStrategy<SankeyParams> {
    private static final Logger logger = LoggerFactory.getLogger(SankeyQueryStrategy.class);
    private String tableName;

    public SankeyQueryStrategy() {
        this.tableName = "[dbo].[eoyes]";
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<SankeyParams> params) {
        SankeyParams p = params.getParams();
        String filter = sanitizeFilter(p.getFilter());
        Map<String, ClassificationTableConfig> clConfigs = Map.of(
                "fuel", new ClassificationTableConfig("CL.cl_fuel", "cl_fuel", "fuel", true),
                "color", new ClassificationTableConfig("CL.cl_color", "cl_color", "color", true),
                "body", new ClassificationTableConfig("CL.cl_body", "cl_body", "body", true),
                "engine", new ClassificationTableConfig("CL.cl_engine", "cl_engine", "engine", true)
        );
        ClassificationTableConfig clConfig = clConfigs.getOrDefault(filter, null);

        // CTE 1: Top 3 brands
        query.addCte("top_brands",
                "SELECT TOP 3 brand, SUM(quantity) AS totalQuantity " +
                        "FROM " + tableName + " t " +
                        "WHERE 1=1 " + buildCommonFilters(query, p) +
                        "GROUP BY brand " +
                        "ORDER BY SUM(quantity) DESC");

        // CTE 2: Top 3 models per brand
        query.addCte("top_models_ranked",
                "SELECT t.brand, t.model, SUM(t.quantity) AS total_quantity, " +
                        "ROW_NUMBER() OVER (PARTITION BY t.brand ORDER BY SUM(t.quantity) DESC) AS rn " +
                        "FROM " + tableName + " t " +
                        "JOIN top_brands tb ON t.brand = tb.brand " +
                        "WHERE 1=1 " + buildCommonFilters(query, p) +
                        "GROUP BY t.brand, t.model");

        query.addCte("top_models",
                "SELECT brand, model, total_quantity " +
                        "FROM top_models_ranked " +
                        "WHERE rn <= 3");

        // CTE 3: Top 3 filter values per model
        String filterColumn = clConfig != null ? clConfig.alias() + ".name_" + sanitizeColumn(p.getLangName()) :
                "CAST(t." + sanitizeColumn(filter) + " AS NVARCHAR)";
        query.addCte("third_level",
                "SELECT t.model, " + filterColumn + " AS filter_value, SUM(t.quantity) AS total_quantity, " +
                        "ROW_NUMBER() OVER (PARTITION BY t.model ORDER BY SUM(t.quantity) DESC) AS rn " +
                        "FROM " + tableName + " t " +
                        (clConfig != null ? "LEFT JOIN " + clConfig.tableName() + " " + clConfig.alias() +
                                " ON t." + sanitizeColumn(filter) + " = " + clConfig.alias() + ".id " : "") +
                        "JOIN top_models tm ON t.model = tm.model " +
                        "WHERE 1=1 " + buildCommonFilters(query, p) +
                        "GROUP BY t.model, " + filterColumn);

        // Main query: Brand → Model and Model → Filter links
        query.append("SELECT 'brand_model' AS link_type, tm.brand AS from_node, tm.model AS to_node, " +
                "tm.total_quantity AS value, CONCAT(tm.brand, '-', tm.model) AS id " +
                "FROM top_models tm " +
                "UNION ALL " +
                "SELECT 'model_filter' AS link_type, tl.model AS from_node, tl.filter_value AS to_node, " +
                "tl.total_quantity AS value, CONCAT(tl.model, '-', tl.filter_value) AS id " +
                "FROM third_level tl " +
                "WHERE tl.rn <= 3");

        logger.debug("Generated Sankey query: {} with parameters: {}", query.getSql(), query.getParameters());
    }

    public QueryBuilder configureMaxYearQuery() {
        QueryBuilder query = new QueryBuilder();
        query.append("SELECT MAX(year) AS maxYear FROM " + tableName);
        return query;
    }

    private String buildCommonFilters(QueryBuilder query, SankeyParams p) {
        StringBuilder filters = new StringBuilder();
        if (p.getYear() != null) {
            filters.append("AND t.year = ? ");
            query.mergeParameters(List.of(p.getYear()));
        }
        if (isAutoMainTable()) {
            if (p.getQuarter() != null && !p.getQuarter().equals("99")) {
                filters.append("AND t.quarter = ? ");
                query.mergeParameters(List.of(convertQuarterToFloat(p.getQuarter())));
            } else {
                filters.append("AND t.quarter IN (?, ?, ?, ?) ");
                query.mergeParameters(List.of(1, 2, 3, 4));
            }
        }
        return filters.toString();
    }

    public static Float convertQuarterToFloat(String quarter) {
        if (quarter == null) {
            return null;
        }
        // Handle Roman numerals
        switch (quarter.trim().toUpperCase()) {
            case "I":
                return 1f;
            case "II":
                return 2f;
            case "III":
                return 3f;
            case "IV":
                return 4f;
            default:
                // Try parsing as a numeric string (e.g., '1', '2.0')
                try {
                    return Float.parseFloat(quarter.trim());
                } catch (NumberFormatException e) {
                    // Return null for invalid values
                    return null;
                }
        }
    }

    private void addCommonFilters(QueryBuilder query, SankeyParams p) {
        if (p.getYear() != null) {
            query.addFilter("t.year", p.getYear(), "=");
        }
        if (isAutoMainTable()) {
            if (p.getQuarter() != null && !p.getQuarter().equals("99")) {
                query.addFilter("t.quarter", p.getQuarter(), "=");
            } else {
                query.addFilter("t.quarter", null, "=", null, List.of(1, 2, 3, 4));
            }
        }
    }

    private boolean isAutoMainTable() {
        return Objects.equals("[dbo].[auto_main]", tableName);
    }

    private String sanitizeFilter(String filter) {
        String sanitized = filter.isEmpty() ? "year_of_production" : filter;
        List<String> validFilters = List.of("fuel", "color", "body", "engine", "year_of_production");
        if (!validFilters.contains(sanitized)) {
            throw new IllegalArgumentException("Invalid filter: " + filter);
        }
        return sanitized;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("link_type", "from_node", "to_node", "value", "id");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of();
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }

    private String sanitizeColumn(String column) {
        return column.replaceAll("[^a-zA-Z0-9_.\\[\\]]", "");
    }

    @Override
    public TableQueryStrategy<SankeyParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
