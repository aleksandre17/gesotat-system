package org.base.mobile.strategy;

import org.base.mobile.arcitecture.FilterBuilder;
import org.base.mobile.arcitecture.FilterBuilderFactory;
import org.base.mobile.arcitecture.FilterContext;
import org.base.mobile.arcitecture.FilterContextFactory;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.RaceParams;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Strategy for race endpoint query, optimized for MSSQL.
 */
@Component("raceStrategy")
public class RaceQueryStrategy implements TableQueryStrategy<RaceParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RaceQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[dbo].[eoyes]";
    private static final String MAIN_TABLE_NAME = "[dbo].[auto_main]";
    private static final int TOP_BRANDS_LIMIT = 20;

    private String tableName;

    private final FilterContext<RaceParams> filterContext;

    public RaceQueryStrategy() {
        this(FilterContextFactory.defaultRaceContext());
    }

    public RaceQueryStrategy(FilterContext filterContext) {
        this.tableName = DEFAULT_TABLE_NAME;
        this.filterContext = Objects.requireNonNull(filterContext, "FilterContext cannot be null");
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<RaceParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<RaceParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        RaceParams p = params.getParams();
        boolean isMainTreeMap = isAutoMainTable();

        // Preserve original logic
        String partitionBy = isMainTreeMap ? "t.year" : "t.year"; //"t.year, t.quarter"
        String groupBy = isMainTreeMap ? "t.year, t.brand" : "t.year, t.brand"; //"t.year, t.quarter, t.brand"
        String orderBy = isMainTreeMap ? "year ASC, value DESC" : "year ASC, value DESC"; //"year ASC, quarter ASC, value DESC"

        // Ranked brands CTE
        QueryBuilder rankedBrands = new QueryBuilder();
        rankedBrands.append("SELECT t.year")
                //.append(isMainTreeMap ? ", CAST(t.quarter AS NVARCHAR) AS quarter" : "")
                .append(", t.brand AS name, SUM(t.quantity) AS value, ")
                .append("ROW_NUMBER() OVER (PARTITION BY ").append(partitionBy)
                .append(" ORDER BY SUM(t.quantity) DESC) AS rn")
                .append(" FROM ").append(sanitizeTableName(tableName)).append(" t ")
                .append("WHERE 1=1 ");

        filterContext.applyFilters(rankedBrands, tableName, p);
        rankedBrands.append(" GROUP BY ").append(groupBy);
        query.addCte("ranked_brands", rankedBrands);

        // Main query: top brands
        QueryBuilder mainQuery = new QueryBuilder();
        mainQuery.append("SELECT year")
                //.append(isMainTreeMap ? ", quarter" : "")
                .append(", name, value")
                .append(" FROM ranked_brands")
                .append(" WHERE rn <= ").append(TOP_BRANDS_LIMIT + " ")
                .append(" ORDER BY ").append(orderBy);

        // Combine query
        query.append(mainQuery.getSql()).mergeParameters(List.of(mainQuery.getParams()));

        LOGGER.debug("Configured race query for table: {}, year: {}, quarter: {}", tableName, p.getYear(), p.getQuarter());
    }

    private boolean isAutoMainTable() {
        return Objects.equals(MAIN_TABLE_NAME, tableName);
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return isAutoMainTable()
                ? List.of("year", "quarter", "name", "value")
                : List.of("year", "name", "value");
    }

    @Override
    public List<String> getGroupBy() {
        return isAutoMainTable()
                ? List.of("year", "quarter", "brand")
                : List.of("year", "brand");
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }
}
