package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.ClassificationTableConfig;
import org.base.api.service.mobile_services.arcitecture.FilterContext;
import org.base.api.service.mobile_services.arcitecture.FilterContextFactory;
import org.base.api.service.mobile_services.params.AccidentsMainParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.ColorsParams;
import org.base.api.service.mobile_services.params.TreemapParams;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Strategy for colors endpoint query, optimized for MSSQL.
 */
@Component("colorsStrategy")
public class ColorsQueryStrategy implements TableQueryStrategy<ColorsParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColorsQueryStrategy.class);
    private String tableName;
    private final ClassificationTableConfig colorConfig;
    private static final int COLOR_LIMIT = 7;
    private static final String OTHER_HEX_CODE = "#8DA399";

    private final FilterContext<ColorsParams> filterContext;

    public ColorsQueryStrategy() {
        this(FilterContextFactory.defaultColorMapContext());
    }

    public ColorsQueryStrategy(FilterContext filterContext) {
        this.filterContext = Objects.requireNonNull(filterContext, "FilterContext cannot be null");
        this.colorConfig = new ClassificationTableConfig("CL.cl_color", "cl_color", "id", true);
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<ColorsParams> params) {
        ColorsParams p = params.getParams();

        QueryBuilder cteQuery = new QueryBuilder();
        cteQuery.addSelect("t.color", null)
                .addSelect("SUM(t.quantity)", "value")
                .addSelect(colorConfig.alias() + "." + sanitizeColumn(p.getLangName()), "color_name")
                .addSelect(colorConfig.alias() + ".hex_code", "hex_code")
                .append(" FROM ").append(tableName).append(" t ")
                .append("LEFT JOIN ").append(sanitizeTableName(colorConfig.tableName())).append(" ").append(colorConfig.alias())
                .append(" ON t.").append(sanitizeColumn("color"))
                .append(" = ").append(colorConfig.alias()).append(".").append(sanitizeColumn(colorConfig.keyColumn())).append(" ")
                .append("WHERE 1=1 ") //.append(buildCommonFilters(cteQuery, p))
                .addGroupBy("t.color")
                .addGroupBy(colorConfig.alias() + "." + sanitizeColumn(p.getLangName()))
                .addGroupBy(colorConfig.alias() + ".hex_code");
        filterContext.applyFilters(cteQuery, tableName, p);
        query.addCte("all_colors", cteQuery);


        // CTE for top 7 colors
        QueryBuilder topColorsQuery = new QueryBuilder();
        topColorsQuery.addTop(COLOR_LIMIT)
                .addSelect("color_name", "name")
                .addSelect("value", null)
                .addSelect("hex_code", "hex")
                .append(" FROM all_colors ")
                .addOrderByExpression("value", "DESC");
        query.addCte("top_colors", topColorsQuery);

        // Main query: select from top_colors
        QueryBuilder mainQuery = new QueryBuilder();
        mainQuery.addSelect("*", null)
                .append(" FROM top_colors ");
        String mainQuerySql = mainQuery.getSql();

        LOGGER.debug("Main query SQL: {}", mainQuerySql);

        // Other colors query
        QueryBuilder otherQuery = new QueryBuilder();
        otherQuery.addSelect("?", "name", true)
                .addSelect("SUM(value)", "value")
                .addSelect("?", "hex", true)
                .append(" FROM all_colors ")
                .append("WHERE color_name NOT IN (")
                .append("SELECT name FROM top_colors")
                .append(") ")
                .append("HAVING SUM(value) > 0");
        otherQuery.mergeParameters(List.of(p.getOtherTranslation(), OTHER_HEX_CODE));
        String otherQuerySql = otherQuery.getSql();
        if (otherQuerySql == null || otherQuerySql.trim().isEmpty()) {
            throw new IllegalStateException("Other query SQL is empty");
        }
        LOGGER.debug("Other query SQL: {}", otherQuerySql);

        // Combine queries
        query.append(mainQuerySql).append(" UNION ALL ").append(otherQuerySql);
        query.mergeParameters(List.of(mainQuery.getParams()));
        query.mergeParameters(List.of(otherQuery.getParams()));
    }

    private String buildCommonFilters(QueryBuilder query, ColorsParams p) {
        StringBuilder filters = new StringBuilder();
        if (p.getYear() != null) {
            filters.append("AND t.year = ? ");
            query.mergeParameters(List.of(p.getYear()));
        }
        if (tableName.equals("auto_main")) {
            if (p.getQuarter() != null && !p.getQuarter().equals("99")) {
                filters.append("AND t.quarter = ? ");
                query.mergeParameters(List.of(p.getQuarter()));
            } else {
                filters.append("AND t.quarter IN (?, ?, ?, ?) ");
                query.mergeParameters(List.of(1, 2, 3, 4));
            }
        }
        return filters.toString();
    }


    @Override
    public List<String> getAttributes() {
        return List.of("color");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("color");
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }

    private String sanitizeColumn(String column) {
        return column.replaceAll("[^a-zA-Z0-9_.\\[\\]]", "");
    }


    private String sanitizeTableName(String tableName) {
        return tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "");
    }

    @Override
    public TableQueryStrategy<ColorsParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
