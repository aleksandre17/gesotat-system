package org.base.mobile.strategy;

import org.base.mobile.ClassificationTableConfig;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.SlidersDataParams;
import org.base.mobile.params.StackedParams;
import org.base.core.model.ClassificationTableType;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for stacked endpoint query, optimized for MSSQL.
 */
@Component("stackedStrategy")
public class StackedQueryStrategy implements TableQueryStrategy<StackedParams> {
    private final String tableName;
    private final String vehicleImpExpTable;
    private static final int TOP_CATEGORIES_LIMIT = 5;
    private static final String OTHER_HEX_CODE = "#8DA399";

    public StackedQueryStrategy() {
        this.tableName = "[dbo].[auto_main]";
        this.vehicleImpExpTable = "[dbo].[auto_main]"; //[dbo].[vehicles_imp_exp]
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    public QueryBuilder configureMinMaxYearQuery() {
        QueryBuilder query = new QueryBuilder();
        query.append("SELECT MIN(year) AS minYear, MAX(year) AS maxYear " +
                "FROM " + vehicleImpExpTable);
        return query;
    }

//    @Override
//    public void configureQuery(QueryBuilder query, QueryParams<StackedParams> params) {
//        StackedParams p = params.getParams();
//        String filter = sanitizeColumn(p.getFilter());
//        ClassificationTableConfig clConfig = getClassificationConfig(p.getFilter());
//
//        String categoryNameExpr = clConfig != null
//                ? clConfig.alias() + "." + sanitizeColumn(p.getLangName())
//                : "t." + filter;
//        String hexCodeExpr = clConfig != null && "color".equals(p.getFilter())
//                ? clConfig.alias() + ".hex_code"
//                : "''";
//        String groupByExtra = clConfig != null && "color".equals(p.getFilter())
//                ? ", " + clConfig.alias() + ".hex_code"
//                : "";
//
//        // CTE for aggregated data
//        query.addCte("agg_data",
//                "SELECT " + categoryNameExpr + " AS category_name, " +
//                        hexCodeExpr + " AS hex_code, t.year, SUM(t.quantity) AS quantity " +
//                        "FROM " + tableName + " t " +
//                        (clConfig != null
//                                ? "LEFT JOIN " + clConfig.tableName() + " " + clConfig.alias() +
//                                " ON t." + sanitizeColumn(clConfig.keyColumn()) + " = " +
//                                clConfig.alias() + "." + sanitizeColumn(clConfig.keyColumn()) + " "
//                                : "") +
//                        "WHERE t.year != 2025 " +
//                        "GROUP BY " + categoryNameExpr + groupByExtra + ", t.year");
//
//        // CTE for top categories
//        query.addCte("top_categories",
//                "SELECT TOP " + topCategoriesLimit + " category_name, hex_code " +
//                        "FROM agg_data " +
//                        "GROUP BY category_name, hex_code " +
//                        "ORDER BY SUM(quantity) DESC");
//
//        // Main query: top categories and "Other"
//        query.append("SELECT tc.category_name AS name, tc.hex_code AS hex, ad.year, ad.quantity " +
//                "FROM top_categories tc " +
//                "JOIN agg_data ad ON tc.category_name = ad.category_name " +
//                "UNION ALL " +
//                "SELECT ? AS name, ? AS hex, ad.year, SUM(ad.quantity) AS quantity " +
//                "FROM agg_data ad " +
//                "WHERE ad.category_name NOT IN (SELECT category_name FROM top_categories) " +
//                "GROUP BY ad.year " +
//                "HAVING SUM(ad.quantity) > 0");
//        query.mergeParameters(List.of(p.getOtherTranslation(), otherHexCode));
//    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<StackedParams> params) {
        StackedParams p = params.getParams();
        String filter = sanitizeColumn(p.getFilter());
        ClassificationTableType clConfig = ClassificationTableType.fromFilter(p.getFilter());

        String categoryNameExpr = clConfig != null
                ? clConfig.getAlias() + "." + sanitizeColumn(p.getLangName())
                : "CAST(t." + filter + " AS VARCHAR)";
        String hexCodeExpr = clConfig != null && "color".equals(p.getFilter())
                ? clConfig.getAlias() + ".hex_code"
                : "''";
        String groupByCategoryExpr = clConfig != null
                ? clConfig.getAlias() + "." + sanitizeColumn(p.getLangName())
                : "t." + filter;
        String groupByHexExpr = clConfig != null && "color".equals(p.getFilter())
                ? ", " + clConfig.getAlias() + ".hex_code"
                : "";

        // CTE: all_categories
        query.addCte("all_categories",
                "SELECT " + categoryNameExpr + " AS category_name, " +
                        hexCodeExpr + " AS hex_code, t.year, SUM(t.quantity) AS quantity " +
                        "FROM " + tableName + " t " +
                        (clConfig != null ?
                                "LEFT JOIN " + clConfig.getTableName() + " " + clConfig.getAlias() +
                                        " ON t." + sanitizeColumn(clConfig.getKeyColumn()) + " = " +
                                        clConfig.getAlias() + ".id " : "") +
                        "WHERE t.year != 2026 " +
                        "GROUP BY " + groupByCategoryExpr + groupByHexExpr + ", t.year");

        // CTE: top_categories
        query.addCte("top_categories",
                "SELECT TOP " + TOP_CATEGORIES_LIMIT + " category_name, hex_code, SUM(quantity) AS total_quantity " +
                        "FROM all_categories " +
                        "GROUP BY category_name, hex_code " +
                        "ORDER BY SUM(quantity) DESC");

        // CTE: with_others
        query.addCte("with_others",
                "SELECT " +
                        "tc.category_name AS name, " +
                        "tc.hex_code AS hex, " +
                        "ac.year, " +
                        "ac.quantity, " +
                        "0 AS sort, " +
                        "SUM(tc.total_quantity) OVER (PARTITION BY tc.category_name) AS sort_value " +
                        "FROM top_categories tc " +
                        "LEFT JOIN all_categories ac ON tc.category_name = ac.category_name " +
                        "WHERE ac.quantity IS NOT NULL " +
                        "UNION ALL " +
                        "SELECT " +
                        "? AS name, " +
                        "? AS hex, " +
                        "ac.year, " +
                        "SUM(ac.quantity) AS quantity, " +
                        "1 AS sort, " +
                        "SUM(ac.quantity) AS sort_value " +
                        "FROM all_categories ac " +
                        "WHERE ac.category_name NOT IN (SELECT category_name FROM top_categories) " +
                        "GROUP BY ac.year " +
                        "HAVING SUM(ac.quantity) > 0");

        // Final query
        query.append("SELECT name, year, quantity, hex FROM with_others ORDER BY sort ASC, sort_value DESC");

        // Parameters for "Other"
        query.mergeParameters(List.of(p.getOtherTranslation(), OTHER_HEX_CODE));
    }


    @Override
    public List<String> getAttributes() {
        return List.of("year");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year");
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }

    private String sanitizeColumn(String column) {
        return column.replaceAll("[^a-zA-Z0-9_.\\[\\]]", "");
    }

    @Override
    public TableQueryStrategy<StackedParams> setTableName(String tableName) {
        //this.tableName = tableName;
        return this;
    }
}
