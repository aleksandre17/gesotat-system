package org.base.mobile.strategy.text;

import org.base.mobile.TableConfigText;
import org.base.mobile.arcitecture.FilterContext;
import org.base.mobile.arcitecture.FilterContextFactory;
import org.base.mobile.arcitecture.FilterRule;
import org.base.mobile.arcitecture.RuleExpression;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.text.RatingsParams;
import org.base.mobile.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component("ratingsStrategy")
public class RatingsQueryStrategy implements TableQueryStrategy<RatingsParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RatingsQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[dbo].[auto_main]";
    private String tableName;
    private final JdbcTemplate jdbcTemplate;
    //private final Translations translations;
    private final FilterContext<RatingsParams> filterContext;

    public RatingsQueryStrategy(JdbcTemplate jdbcTemplate) {
        this.tableName = DEFAULT_TABLE_NAME;
        this.jdbcTemplate = jdbcTemplate;

        this.filterContext = FilterContextFactory.defaultCommonContextText();
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<RatingsParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<RatingsParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }

        RatingsParams p = params.getParams();
        TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(p.getTableName(), TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));
        boolean isMainTreeMap = Objects.equals(tableConfig.tableName(), DEFAULT_TABLE_NAME);

        // Max year query
        QueryBuilder maxYearQuery = new QueryBuilder();
        maxYearQuery.append("SELECT MAX(t.").append(tableConfig.maxYearColumn()).append(") AS maxYear ")
                .append("FROM ").append(sanitizeTableName(tableName)).append(" t");
        query.addCte("max_year", maxYearQuery);

        // Year selector query
        QueryBuilder yearQuery = new QueryBuilder();
        yearQuery.append("SELECT DISTINCT CAST(t.year AS VARCHAR) AS code, CAST(t.year AS VARCHAR) AS name ")
                .append("FROM ").append(sanitizeTableName(tableName)).append(" t");
        query.addCte("year_selector", yearQuery);

        // Quarter selector query (for MainTreeMap)
        if (isMainTreeMap) {
            QueryBuilder quarterQuery = new QueryBuilder();
            quarterQuery.append("SELECT DISTINCT CAST(t.quarter AS VARCHAR) AS code, CAST(t.quarter AS VARCHAR) AS name ")
                    .append("FROM ").append(sanitizeTableName(tableName)).append(" t");
            if (p.getYear() != null) {
                FilterContext<RatingsParams> quarterContext = filterContext.withAdditionalRule(
                        tableName,
                        new FilterRule.Builder<RatingsParams, Integer>("year", "t.year", RatingsParams::getYear)
                                .withPredicateCondition(rp -> rp.getYear() != null)
                                .build()
                );
                RuleExpression<RatingsParams> quarterCondition = RuleExpression.fromPredicate(rp -> rp.getYear() != null);
                quarterQuery.append(" WHERE 1=1 ");
                quarterContext.applyFilters(quarterQuery, tableName, p, quarterCondition);
            }
            query.addCte("quarter_selector", quarterQuery);
        }

        // Main query with ORDER BY
        query.append("SELECT 'year' AS selector_type, code, name, ")
                .append("CAST((SELECT maxYear FROM max_year) AS VARCHAR) AS default_code, ")
                .append("CAST((SELECT maxYear FROM max_year) AS VARCHAR) AS default_name ")
                .append("FROM year_selector ");
        if (isMainTreeMap) {
            query.append("UNION ALL ")
                    .append("SELECT 'quarter' AS selector_type, code, name, NULL AS default_code, 'All' AS default_name ")
                    .append("FROM quarter_selector ");
        }
        query.append("ORDER BY selector_type, code");

        LOGGER.debug("Configured ratings query for table: {}, year: {}", tableName, p.getYear());
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("selector_type", "code", "name", "default_code", "default_name");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of();
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }
}
