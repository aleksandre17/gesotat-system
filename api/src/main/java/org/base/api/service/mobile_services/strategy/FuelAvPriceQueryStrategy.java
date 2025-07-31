package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.EquityParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.FuelAvPriceParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for fuel-av-price endpoint queries.
 */
@Component("fuelAvPriceStrategy")
public class FuelAvPriceQueryStrategy implements TableQueryStrategy<FuelAvPriceParams> {
    private String tableName;

    public FuelAvPriceQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<FuelAvPriceParams> params) {
        FuelAvPriceParams p = (FuelAvPriceParams) params.getParams();
        String currencyColumn = p.getCurrency() != null && p.getCurrency() ? "gel1000" : "usd1000";

        query.addSubQuery("minYearQuery", new QueryBuilder()
                .append("SELECT MIN(t.year) AS minYear ")
                .append("FROM ").append(tableName).append(" t "));

        query.addSubQuery("dataQuery", new QueryBuilder()
                .append("SELECT t.year, SUM(t.").append(currencyColumn).append(") AS currency, SUM(t.tone) AS tone ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE t.year != 2026 ")
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
        return true;
    }

    @Override
    public TableQueryStrategy<FuelAvPriceParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
