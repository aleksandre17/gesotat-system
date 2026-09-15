package org.base.mobile.strategy;

import org.base.mobile.params.FuelLineParams;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.FuelQuantityParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for fuel-quantity endpoint queries.
 */
@Component("fuelQuantityStrategy")
public class FuelQuantityQueryStrategy implements TableQueryStrategy<FuelQuantityParams> {
    private  String tableName;

    public FuelQuantityQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<FuelQuantityParams> params) {
        FuelQuantityParams p = (FuelQuantityParams) params.getParams();
        boolean isMonthly = p.getAnualOrMonthly();
        String groupBy = isMonthly ? "month" : "year";

        query.addSubQuery("minYearQuery", new QueryBuilder()
                .append("SELECT MIN(t.year) AS minYear ")
                .append("FROM ").append(tableName).append(" t "));

        query.addSubQuery("dataQuery", new QueryBuilder()
                .append("SELECT t.").append(groupBy).append(", ROUND(SUM(t.tone), 1) AS tone ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE t.year != 2026 ")
                .addFilter("t.e_i", p.getE_i() != null ? p.getE_i() : "E", "=")
                .addFilter("t.fuel", p.getFuel() != null ? p.getFuel() : "1", "=")
                .addGroupBy("t." + groupBy)
                .addOrderBy("t." + groupBy, "ASC"));
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year");
    }

    @Override
    public boolean isCombinedQuery() {
        return true; // Multiple sub-queries
    }

    @Override
    public TableQueryStrategy<FuelQuantityParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
