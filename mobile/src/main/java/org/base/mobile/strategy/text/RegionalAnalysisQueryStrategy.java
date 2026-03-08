package org.base.mobile.strategy.text;

import org.base.mobile.TableConfigText;
import org.base.mobile.arcitecture.FilterContext;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.text.RegionalAnalysisParams;
import org.base.mobile.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("regionalAnalysisStrategy")
public class RegionalAnalysisQueryStrategy implements TableQueryStrategy<RegionalAnalysisParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegionalAnalysisQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[dbo].[auto_main]";
    private String tableName;
    private final JdbcTemplate jdbcTemplate;

    public RegionalAnalysisQueryStrategy(JdbcTemplate jdbcTemplate) {
        this.tableName = DEFAULT_TABLE_NAME;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<RegionalAnalysisParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        return this;
    }

    public String getMaxYear() {
        QueryBuilder query = new QueryBuilder();
        query.append("SELECT MAX(CAST(t.year AS VARCHAR)) AS maxYear ").append("FROM ").append(sanitizeTableName(tableName)).append(" t");
        List<String> maxYear = jdbcTemplate.query(query.getSql(), (rs, rowNum) -> rs.getString("maxYear"));
        return maxYear.isEmpty() ? null : maxYear.get(0);
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<RegionalAnalysisParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        RegionalAnalysisParams p = params.getParams();
        String queryType = params.getLangName();
        TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));

        switch (queryType) {
            case "years":
                query.append("SELECT DISTINCT CAST(t.year AS VARCHAR) AS year ")
                        .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                        .append("ORDER BY year DESC");
                break;
            case "quarters":
                query.append("SELECT DISTINCT t.quarter AS quarter ")
                        .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                        .append("WHERE 1=1 ");
                if (p.getYear() != null) {
                    query.append("AND t.year = ").append(p.getYear()).append(" ");
                }
                if (p.getBrand() != null) {
                    query.append("AND t.brand = '").append(p.getBrand()).append("' ");
                }
                query.append("ORDER BY quarter ASC");
                break;
            case "brands":
                query.append("SELECT t.brand AS brand, SUM(t.quantity) AS value ")
                        .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                        .append("WHERE t.body != 19 ")
                        .append("GROUP BY t.brand ")
                        .append("ORDER BY value DESC");
                break;
            case "yearOfProduction":
                query.append("SELECT DISTINCT CAST(t.year_of_production AS VARCHAR) AS year ")
                        .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                        .append("WHERE t.year_of_production > 0 AND t.year_of_production < 3000 ");
                if (p.getYear() != null) {
                    query.append("AND t.year = ").append(p.getYear()).append(" ");
                }
                if (p.getBrand() != null) {
                    query.append("AND t.brand = '").append(p.getBrand()).append("' ");
                }
                query.append("ORDER BY year DESC");
                break;
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }

        LOGGER.debug("Configured regional analysis query for table: {}, type: {}", tableName, queryType);
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "quarter", "brand", "year_of_production");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("brand");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }
}
