package org.base.mobile.strategy.text;

import org.base.mobile.params.QueryParams;
import org.base.mobile.params.text.VehicleQuantityParams;
import org.base.mobile.strategy.TableQueryStrategy;
import org.base.core.service.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("vehicleQuantityStrategy")
public class VehicleQuantityQueryStrategy implements TableQueryStrategy<VehicleQuantityParams> {
    private static final Logger LOGGER = LoggerFactory.getLogger(VehicleQuantityQueryStrategy.class);
    private static final String DEFAULT_TABLE_NAME = "[dbo].[Vehicles1000]";
    private String tableName;

    public VehicleQuantityQueryStrategy(JdbcTemplate jdbcTemplate) {
        this.tableName = DEFAULT_TABLE_NAME;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public TableQueryStrategy<VehicleQuantityParams> setTableName(String tableName) {
        this.tableName = sanitizeTableName(tableName);
        return this;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<VehicleQuantityParams> params) {
        if (params == null || params.getParams() == null) {
            throw new IllegalArgumentException("Query parameters cannot be null");
        }
        VehicleQuantityParams p = params.getParams();
        String lang = p.getLang() != null && p.getLang().equalsIgnoreCase("ka") ? "ka" : "en";

        query.append("SELECT DISTINCT v.type AS code, vt.name_").append(lang).append(" AS name ")
                .append("FROM ").append(sanitizeTableName(tableName)).append(" v ")
                .append("INNER JOIN [cl].[cl_transport] vt ON v.type = vt.id ")
                .append("ORDER BY v.type ASC");

        LOGGER.debug("Configured vehicle quantity query for table: {}, lang: {}", tableName, lang);
    }

    private String sanitizeTableName(String tableName) {
        return tableName != null ? tableName.replaceAll("[^a-zA-Z0-9_\\[\\].]", "") : DEFAULT_TABLE_NAME;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("code", "name");
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
