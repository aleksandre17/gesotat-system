package org.base.api.service.mobile_services.strategy.text;

import org.base.api.controller.TableConfigText;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.text.AccidentsParams;
import org.base.api.service.mobile_services.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("accidentsStrategy")
public class AccidentsQueryStrategy implements TableQueryStrategy<AccidentsParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccidentsQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = TableConfigText.DEFAULT_ACCIDENTS_TABLE_NAME;
    private String tableName;


    public AccidentsQueryStrategy(JdbcTemplate jdbcTemplate) {
        this.tableName = DEFAULT_TABLE_NAME;

    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<AccidentsParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        TableConfigText.getTableMetadata(this.tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<AccidentsParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        AccidentsParams p = params.getParams();
        String queryType = params.getQueryType();

        switch (queryType) {
            case "regions":
                String regionLangColumn = TableConfigText.getLanguageColumn("[CL].[cl_region]", p.getLang() != null ? p.getLang() : "en");
                query.append("SELECT t.").append(regionLangColumn).append(" AS name, t.id AS code ")
                        .append("FROM [CL].[cl_region] t ")
                        .append("WHERE t.id NOT IN (12, 26, 35, 99, 98) ")
                        .append("ORDER BY t.id ASC");
                break;
            case "accidents":
                String langColumn = TableConfigText.getLanguageColumn(tableName, p.getLang() != null ? p.getLang() : "en");
                query.append("SELECT t.").append(langColumn).append(" AS name, t.id AS code ")
                        .append("FROM ").append(tableName).append(" t ")
                        .append("ORDER BY t.id DESC");
                break;
            default:
                throw new IllegalArgumentException("Unknown query type: " + queryType);
        }

        LOGGER.debug("Configured accidents query for table: {}, type: {}", tableName, queryType);
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("ID", "en", "ka");
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
