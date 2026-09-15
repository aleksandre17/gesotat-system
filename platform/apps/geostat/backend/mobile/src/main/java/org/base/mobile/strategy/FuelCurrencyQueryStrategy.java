package org.base.mobile.strategy;

import org.base.mobile.params.FuelColumnParams;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.FuelCurrencyParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for fuel-currency endpoint queries.
 */
@Component("fuelCurrencyStrategy")
public class FuelCurrencyQueryStrategy implements TableQueryStrategy<FuelCurrencyParams> {
    private  String tableName;

    public FuelCurrencyQueryStrategy() {

    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<FuelCurrencyParams> params) {
        FuelCurrencyParams p = (FuelCurrencyParams) params.getParams();
        String currencyColumn = p.getCurrency() != null && p.getCurrency() ? "gel1000" : "usd1000";

        query.addSubQuery("minYearQuery", new QueryBuilder()
                .append("SELECT MIN(t.year) AS minYear ")
                .append("FROM ").append(tableName).append(" t "));

        query.addSubQuery("dataQuery", new QueryBuilder()
                .append("SELECT t.year, ROUND(SUM(t.").append(currencyColumn).append("), 1) AS currency ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE t.year != 2026 ")
                .addFilter("t.e_i", p.getE_i() != null ? p.getE_i() : "I", "=")
                .addFilter("t.fuel", p.getFuel() != null ? p.getFuel() : "1", "=")
                .addGroupBy("t.year")
                .addOrderBy("t.year", "ASC"));
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
    public TableQueryStrategy<FuelCurrencyParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
