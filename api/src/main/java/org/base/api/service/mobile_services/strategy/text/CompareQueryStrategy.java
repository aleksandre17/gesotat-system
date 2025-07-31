package org.base.api.service.mobile_services.strategy.text;

import org.base.api.controller.TableConfigText;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.text.CompareParams;
import org.base.api.service.mobile_services.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("compareStrategy")
public class CompareQueryStrategy implements TableQueryStrategy<CompareParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CompareQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[dbo].[auto_main]";
    private String tableName;


    public CompareQueryStrategy(JdbcTemplate jdbcTemplate) {
        this.tableName = DEFAULT_TABLE_NAME;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<CompareParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<CompareParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        CompareParams p = params.getParams();
        String queryType = params.getLangName();
        TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));

        switch (queryType) {
            case "brands":
                query.append("SELECT t.brand AS brand, SUM(t.quantity) AS value ")
                        .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                        .append("WHERE t.body != 19 ")
                        .append("GROUP BY t.brand ")
                        .append("ORDER BY value DESC");
                break;
            case "models1":
                query.append("SELECT DISTINCT t.model AS model, SUM(t.quantity) AS value ")
                        .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                        .append("WHERE t.brand =  '" + (p.getBrand1()) + "' ")
                        .append("GROUP BY t.model ")
                        .append("ORDER BY value DESC");
                break;
            case "models2":
                query.append("SELECT DISTINCT t.model AS model, SUM(t.quantity) AS value ")
                        .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                        .append("WHERE t.brand =  '" + (p.getBrand2()) + "' ")
                        .append("GROUP BY t.model ")
                        .append("ORDER BY value DESC");
                break;
            case "yearOfProduction1":
            case "yearOfProduction2":
                query.append("SELECT DISTINCT CAST(t.year_of_production AS VARCHAR) AS year ")
                        .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                        .append("WHERE t.year_of_production > 0 AND t.year_of_production < 3000 ");
                if (queryType.equals("yearOfProduction1")) {
                    if (p.getModel1() != null && !p.getModel1().isEmpty() && p.getBrand1() != null) {
                        query.append("AND t.model = '" + p.getModel1() + "' ").append("AND t.brand = '" + p.getBrand1() + "' ");
                    }
                } else {
                    if (p.getModel2() != null && !p.getModel2().isEmpty() && p.getBrand2() != null) {
                        query.append("AND t.model = '" + p.getModel2() + "' ").append("AND t.brand = '" + p.getBrand2() + "' ");
                    }
                }
                query.append("ORDER BY year DESC");
                break;
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }

        LOGGER.debug("Configured compare query for table: {}, type: {}", tableName, queryType);
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("brand", "model", "year_of_production");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("brand", "model");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }
}
