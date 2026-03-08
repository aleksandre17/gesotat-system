package org.base.mobile.strategy;

import org.base.mobile.TableConfigText;
import org.base.mobile.arcitecture.FilterContext;
import org.base.mobile.arcitecture.FilterContextFactory;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.RaceParams;
import org.base.mobile.params.TradeParams;
import org.base.mobile.params.TreemapParams;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Strategy for treemap endpoint query, optimized for MSSQL.
 */
/**
 * Strategy for treemap endpoint query, optimized for MSSQL with dynamic GROUP BY.
 */
@Component("treemapStrategy")
public class TreemapQueryStrategy implements TableQueryStrategy<TreemapParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TreemapQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[dbo].[eoyes]";
    private static final String MAIN_TABLE_NAME = "[dbo].[auto_main]";
    private static final int TOP_LIMIT = 25;
    private static final String DEFAULT_OTHER_LABEL = "Other";

    private String tableName;
    private final FilterContext<TreemapParams> filterContext;

    // Dynamic GROUP BY configuration
    private final Function<TreemapParams, List<String>> groupByConfig = params ->
            params != null && params.getQuarter() != null && !params.getQuarter().equals("99") && isAutoMainTable()
                    ? List.of("year", "quarter", "brand", "model")
                    : List.of("year", "brand", "model");

    public TreemapQueryStrategy() {
        this.tableName = DEFAULT_TABLE_NAME;
        this.filterContext = FilterContextFactory.defaultTreeMapContext();
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<TreemapParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        LOGGER.debug("Validating table: {}", this.tableName);
        TableConfigText.getTableMetadata(this.tableName); // Validate table
        return this;
    }

    public QueryBuilder configureMaxQuarterQuery(Integer year) {
        if (year == null) {
            throw new IllegalArgumentException("Year cannot be null");
        }
        QueryBuilder query = new QueryBuilder();
        query.addSelect("MAX(quarter)", "maxQuarter")
                .append("FROM ").append(MAIN_TABLE_NAME)
                .addFilter("year", year, "=");
        LOGGER.debug("Configured max quarter query for year: {}", year);
        return query;
    }


    @Override
    public void configureQuery(QueryBuilder query, QueryParams<TreemapParams> params) {

        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        TreemapParams p = params.getParams();

        if (p.getYear() == null) {
            throw new IllegalArgumentException("Year parameter is required");
        }

        // Validate required columns
        //        for (String col : List.of("brand", "model", "quantity")) {
        //            if (!TableConfigText.isValidColumn(tableName, col)) {
        //                throw new IllegalArgumentException("Invalid column '" + col + "' for table: " + tableName);
        //            }
        //        }

        String otherLabel = p.getOtherTranslation() != null ? p.getOtherTranslation() : DEFAULT_OTHER_LABEL;
        List<String> columns = groupByConfig.apply(p);

        // Base data CTE
        QueryBuilder baseData = new QueryBuilder();
        addDynamicColumns(baseData, columns, "t", false, false);
        baseData.addSelect("t.model", null)
                .addSelect("SUM(t.quantity)", "total_quantity")
                .append(" FROM ").append(tableName).append(" t ")
                .append("WHERE 1=1 ");
        filterContext.applyFilters(baseData, tableName, p);
        addDynamicColumns(baseData, columns, "t", true, false);
        baseData.addGroupBy("t.model");
        query.addCte("base_data", baseData);

        // Top brands CTE
        QueryBuilder topBrands = new QueryBuilder();
        topBrands.addTop(TOP_LIMIT)
                .addSelect("brand", null)
                .append(" FROM base_data ")
                .addGroupBy("brand")
                .addOrderByExpression("SUM(total_quantity)", "DESC");
        query.addCte("top_brands", topBrands);

        // Ranked CTE
        QueryBuilder ranked = new QueryBuilder();
        addDynamicColumns(ranked, columns, "bd", false, false);
        ranked.addSelect("bd.model", null)
                .addSelect("bd.total_quantity", null)
                .addSelect("ROW_NUMBER() OVER (PARTITION BY bd.brand ORDER BY bd.total_quantity DESC)", "rn")
                .addSelect("SUM(bd.total_quantity) OVER (PARTITION BY bd.brand)", "brand_total")
                .append(" FROM base_data bd ")
                .append("WHERE bd.brand IN (SELECT brand FROM top_brands)");
        query.addCte("ranked", ranked);

        // Final CTE
        QueryBuilder finalCte = new QueryBuilder();
        finalCte.addSelect("brand", null)
                .addSelect("CASE WHEN rn > " + TOP_LIMIT + " THEN N'" + otherLabel + "' ELSE model END", "model")
                .addSelect("SUM(total_quantity)", "total_quantity")
                .addSelect("MAX(CASE WHEN rn > " + TOP_LIMIT + " THEN 1 ELSE 0 END)", "sort")
                .addSelect("MAX(brand_total)", "sort_value")
                .append(" FROM ranked ")
                .addGroupBy("brand")
                .addGroupByExpression("CASE WHEN rn > " + TOP_LIMIT + " THEN N'" + otherLabel + "' ELSE model END");
        query.addCte("final", finalCte);

        // Main query
        query.addSelect("brand", null)
                .addSelect("model", null)
                .addSelect("total_quantity", "totalQuantity")
                .addSelect("sort_value", "sortValue")
                .append(" FROM final ")
                .addOrderByExpression("sort", "ASC")
                .addOrderByExpression("sort_value", "DESC");

        LOGGER.debug("Configured treemap query for table: {}, year: {}, quarter: {}", tableName, p.getYear(), p.getQuarter());
    }

    private void addDynamicColumns(QueryBuilder query, List<String> columns, String tableAlias, boolean isGroupBy, boolean isOther) {
        List<String> targetColumns = new ArrayList<>();
        if (isOther) {
            if (columns.contains("year")) targetColumns.add("year");
            if (columns.contains("quarter")) targetColumns.add("quarter");
            targetColumns.add("brand");
        } else {
            for (String col : columns) {
                if (!col.equals("model")) {
                    targetColumns.add(col);
                }
            }
        }

        for (String col : targetColumns) {
            String columnRef = col.equals("quarter") ? "CAST(" + tableAlias + ".quarter AS NVARCHAR)" : tableAlias + "." + col;
            String alias = col.equals("quarter") && !isOther ? "quarter" : null;
            if (isGroupBy) {
                query.addGroupBy(columnRef);
            } else {
                query.addSelect(columnRef, alias);
            }
        }
    }

    public QueryBuilder configureOtherBrandsQuery(List<String> topBrands, TreemapParams p) {
        QueryBuilder query = new QueryBuilder();
        query.addSelect("SUM(quantity)", "totalQuantity")
                .append(" FROM ").append(sanitizeTableName(tableName)).append(" t ")
                .append("WHERE 1=1 ");

        if (topBrands != null && !topBrands.isEmpty()) {
            query.append(" AND t.brand NOT IN (")
                    .append(topBrands.stream().map(b -> "?").collect(Collectors.joining(", ")))
                    .append(") ");
            query.mergeParameters((List<Object>) (List<?>) topBrands);
        } else {
            query.append(" AND t.brand IS NOT NULL ");
        }

        addFilters(query, p);
        LOGGER.debug("Configured other brands query with topBrands size: {}", topBrands != null ? topBrands.size() : 0);
        return query;
    }

    private void addFilters(QueryBuilder query, TreemapParams p) {
        if (p != null && p.getYear() != null) {
            query.addFilter("t.year", p.getYear(), "=");
        }
        if (isAutoMainTable() && p != null && p.getQuarter() != null && !p.getQuarter().equals("99")) {
            query.addFilter("CAST(t.quarter AS NVARCHAR)", convertQuarterToFloat(p.getQuarter()), "=");
        }
    }

    public static Integer convertQuarterToFloat(String quarter) {
        if (quarter == null) {
            return null;
        }
        // Handle Roman numerals
        switch (quarter.trim().toUpperCase()) {
            case "I":
                return 1;
            case "II":
                return 2;
            case "III":
                return 3;
            case "IV":
                return 4;
            default:
                // Try parsing as a numeric string (e.g., '1', '2.0')
                try {
                    return Integer.valueOf(quarter.trim());
                } catch (NumberFormatException e) {
                    // Return null for invalid values
                    return null;
                }
        }
    }

    private boolean isAutoMainTable() {
        return Objects.equals(MAIN_TABLE_NAME, tableName);
    }

    private String sanitizeTableName(String tableName) {
        String sanitized = tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
        LOGGER.debug("Sanitized table name: {} -> {}", tableName, sanitized);
        return sanitized;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("brand", "model", "totalQuantity", "sortValue");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of(); // Handled in CTEs
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }
}
