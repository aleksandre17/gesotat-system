package org.base.mobile.strategy.text;

import org.base.mobile.TableConfigText;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.text.FullRatingsParams;
import org.base.mobile.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("fullRatingsStrategy")
public class FullRatingsQueryStrategy implements TableQueryStrategy<FullRatingsParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullRatingsQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[dbo].[auto_main]";
    private String tableName;

    public FullRatingsQueryStrategy(JdbcTemplate jdbcTemplate) {
        this.tableName = DEFAULT_TABLE_NAME;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<FullRatingsParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<FullRatingsParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(params.getParams().getTableName(), TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));

        // Max year query
        QueryBuilder maxYearQuery = new QueryBuilder();
        maxYearQuery.append("SELECT MAX(CAST(t.").append(tableConfig.maxYearColumn()).append(" AS VARCHAR)) AS maxYear ")
                .append("FROM ").append(sanitizeTableName(tableName)).append(" t");
        query.addCte("max_year", maxYearQuery);

        // Year selector query
        QueryBuilder yearQuery = new QueryBuilder();
        yearQuery.append("SELECT DISTINCT CAST(t.year AS VARCHAR) AS code, CAST(t.year AS VARCHAR) AS name ")
                .append("FROM ").append(sanitizeTableName(tableName)).append(" t");
        query.addCte("year_selector", yearQuery);

        // Main query
        query.append("SELECT code, name, ")
                .append("(SELECT maxYear FROM max_year) AS default_code, ")
                .append("(SELECT maxYear FROM max_year) AS default_name ")
                .append("FROM year_selector ")
                .append("ORDER BY code DESC");

        LOGGER.debug("Configured full ratings query for table: {}", tableName);
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("code", "name", "default_code", "default_name");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of();
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }
}
