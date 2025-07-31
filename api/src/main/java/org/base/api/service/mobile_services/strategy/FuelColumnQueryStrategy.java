package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.FuelAvPriceParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.FuelColumnParams;
import org.base.core.model.ClassificationTableType;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for fuel-column endpoint queries.
 */
@Component("fuelColumnStrategy")
public class FuelColumnQueryStrategy implements TableQueryStrategy<FuelColumnParams> {
    private  String tableName;

    public FuelColumnQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<FuelColumnParams> params) {
        FuelColumnParams p = (FuelColumnParams) params.getParams();
        String currencyColumn = p.getCurrency() != null && p.getCurrency() ? "gel1000" : "usd1000";

        ClassificationTableType cl = ClassificationTableType.fromFilter("country");
        //        query.append("WITH t AS (")
        //                .append("SELECT year, country, ")
        //                .append(currencyColumn).append(", e_i ")
        //                .append("FROM ").append(tableName).append(" ")
        //                .addFilter("fuel", p.getFuel() != null ? p.getFuel() : "1", "=")
        //                .addFilter("e_i", p.getE_i(), "?")
        //                .addFilter("year", p.getYear(), "?")
        //                .addFilter("country", p.getCountry(), "?")
        //                .addFilter("region", p.getRegion(), "?")
        //                .append(") ")
        //                .append("SELECT t.year, t.country, ROUND(SUM(t.").append(currencyColumn).append("), 1) AS currency, ")
        //                .append("cc.").append(params.getLangName()).append(" AS country_name, cc.ID AS country_id ")
        //                .append("FROM t ")
        //                .append("LEFT JOIN country_cl cc ON t.country = cc.ID ");
        query.append("SELECT t.year, t.country, ROUND(SUM(t.").append(currencyColumn).append("), 1) AS currency, ")
                .append("cc.name_").append(params.getLangName()).append(" AS country_name, cc.ID AS country_id ")
                .append("FROM ").append(tableName).append(" t ")
                .append("LEFT JOIN "+cl.getTableName()+" cc ON t.country = cc.id ")
                .append("WHERE t.year != 2026 ")
                .addFilter("t.fuel", p.getFuel() != null ? p.getFuel() : "1", "=")
                .addFilter("t.e_i", p.getE_i(), "=")
                .addGroupBy("t.year", "t.country", "cc.name_" + params.getLangName(), "cc.id")
                .addOrderBy("t.year", "ASC");
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "country");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year", "country");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<FuelColumnParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
