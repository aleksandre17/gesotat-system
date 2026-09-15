package org.base.mobile.strategy.text;

import org.base.mobile.params.QueryParams;
import org.base.mobile.params.text.ExportImportParams;
import org.base.mobile.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("exportImportStrategy")
public class ExportImportQueryStrategy implements TableQueryStrategy<ExportImportParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportImportQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[CL].[cl_fuel]";
    private String tableName;

    public ExportImportQueryStrategy(JdbcTemplate jdbcTemplate) {
        this.tableName = DEFAULT_TABLE_NAME;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<ExportImportParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<ExportImportParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        ExportImportParams p = params.getParams();
        String lang = p.getLang() != null && p.getLang().equalsIgnoreCase("ka") ? "ka" : "en";

        query.append("SELECT name_").append(lang).append(" AS name, ID AS code ")
                .append("FROM ").append(sanitizeTableName(tableName)).append(" ")
                .append("WHERE ID IN (1, 2, 3, 4) ")
                .append("ORDER BY ID ASC");

        LOGGER.debug("Configured export-import query for table: {}, lang: {}", tableName, lang);
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("name", "code");
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
