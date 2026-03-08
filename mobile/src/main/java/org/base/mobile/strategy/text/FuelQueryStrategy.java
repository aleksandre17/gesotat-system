package org.base.mobile.strategy.text;

import org.base.mobile.TableConfigText;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.text.FuelParams;
import org.base.mobile.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("fuelStrategy")
public class FuelQueryStrategy implements TableQueryStrategy<FuelParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FuelQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[CL].[cl_fuel]";
    private String tableName;

    public FuelQueryStrategy() {
        this.tableName = DEFAULT_TABLE_NAME;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<FuelParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<FuelParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        FuelParams p = params.getParams();
        TableConfigText.TableMetadata tableConfig = TableConfigText.TABLES.getOrDefault(tableName, TableConfigText.TABLES.get(DEFAULT_TABLE_NAME));

        query.append("SELECT t.name_").append(p.getLang() != null && p.getLang().equals("ka") ? "ka" : "en").append(" AS name, t.id AS code ")
                .append("FROM ").append(sanitizeTableName(tableName)).append(" t ")
                .append("WHERE t.id IN (1, 2, 8) ")
                .append("ORDER BY t.id ASC");

        LOGGER.debug("Configured fuel query for table: {}", tableName);
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
