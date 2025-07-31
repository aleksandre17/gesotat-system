package org.base.api.service.mobile_services.strategy.text;

import org.base.api.controller.TableConfigText;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.text.RoadParams;
import org.base.api.service.mobile_services.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("roadStrategy")
public class RoadQueryStrategy implements TableQueryStrategy<RoadParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[dbo].[road_length]";
    private String tableName;


    public RoadQueryStrategy() {
        this.tableName = DEFAULT_TABLE_NAME;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<RoadParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<RoadParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        RoadParams p = params.getParams();
        String queryType = params.getQueryType();
        TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));

        switch (queryType) {
            case "years":
                query.append("SELECT DISTINCT CAST(t.year AS VARCHAR) AS year ")
                        .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                        .append("ORDER BY year DESC");
                break;
            case "regions":
                query.append("SELECT t.name_")
                        .append(p.getLang() != null && p.getLang().equals("ka") ? "ka" : "en")
                        .append(" AS name, t.id AS code ")
                        .append("FROM [CL].[cl_region] t ")
                        .append("WHERE t.id < 48 AND t.id != 12 ")
                        .append("ORDER BY t.id ASC");
                break;
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }

        LOGGER.debug("Configured road query for table: {}, type: {}", tableName, queryType);
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "ID", "en", "ka");
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
