package org.base.mobile.strategy;

import org.base.mobile.params.QueryParams;
import org.base.mobile.params.FiltersParams;
import org.base.core.model.ClassificationTableType;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Strategy for filters endpoint queries.
 */
@Component("filtersStrategy")
public class FiltersQueryStrategyImpl implements FiltersQueryStrategy {
    private String tableName;

    public FiltersQueryStrategyImpl() {
//        this.tableName = "[dbo].[eoyes]";
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    /**
     * Configures the query to find the top model.
     */
    public QueryBuilder configureTopModelQuery(QueryParams<FiltersParams> params) {
        FiltersParams p = params.getParams();
        QueryBuilder query = new QueryBuilder();

        query.append("SELECT TOP 1 t.model, SUM(t.quantity) AS totalQuantity ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE 1=1 ");

        addCommonFilters(query, p);

        query.addGroupBy("t.model")
                .addOrderBy("totalQuantity", "DESC");

        return query;
    }

    /**
     * Configures the main query for filter data.
     */
    @Override
    public void configureQuery(QueryBuilder query, QueryParams<FiltersParams> params) {
        FiltersParams p = params.getParams();
        String filter = p.getFilter().isEmpty() ? "year_of_production" : p.getFilter();

        ClassificationTableType clConfig = ClassificationTableType.fromFilter(filter);

        query.append("SELECT t.").append(sanitizeColumn(filter)).append(", SUM(t.quantity) AS totalQuantity");
        if (clConfig != null) {
            query.append(", ").append(clConfig.getAlias()).append(".").append(sanitizeColumn(p.getLangName()));
            if (clConfig.hasHexCode()) {
                query.append(", ").append(clConfig.getAlias()).append(".hex_code");
            }
        }
        query.append(" FROM ").append(tableName).append(" t ");
        if (clConfig != null) {
            query.append(" LEFT JOIN ").append(clConfig.getTableName()).append(" ").append(clConfig.getAlias())
                    .append(" ON t.").append(sanitizeColumn(filter))
                    .append(" = ").append(clConfig.getAlias()).append(".").append("id");
        }
        query.append(" WHERE t.model = ? ");
        addCommonFilters(query, p);
        query.addGroupBy("t." + sanitizeColumn(filter));
        if (clConfig != null) {
            query.addGroupBy(clConfig.getAlias() + "." + sanitizeColumn(p.getLangName()));
            if (clConfig.hasHexCode()) {
                query.addGroupBy(clConfig.getAlias() + ".hex_code");
            }
        }
        query.addOrderBy("totalQuantity", "DESC");
    }

    private void addCommonFilters(QueryBuilder query, FiltersParams p) {
        if (p.getYear() != null) {
            query.addFilter("t.year", p.getYear(), "=");
        }
        if (p.getTransport() != null && !p.getTransport().equals("99")) {
            query.addFilter("t.transport", p.getTransport(), "=");
        }
        if (Objects.equals("[dbo].[auto_main]", tableName)) {
            if (p.getQuarter() != null && !p.getQuarter().equals("99")) {
                query.addFilter("t.quarter", convertQuarterToFloat(p.getQuarter()), "=");
            } else {
                query.addFilter("t.quarter", null, "=", null, List.of(1, 2, 3, 4));
            }
        }
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

    @Override
    public List<String> getAttributes() {
        return List.of("model");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("model");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    private String sanitizeColumn(String column) {
        return column.replaceAll("[^a-zA-Z0-9_.]", "");
    }

    @Override
    public TableQueryStrategy<FiltersParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
