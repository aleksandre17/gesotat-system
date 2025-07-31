package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.FuelCurrencyParams;
import org.base.api.service.mobile_services.params.FuelLineParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.core.model.ClassificationTableType;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for fuel-line endpoint queries.
 */
@Component("fuelLineStrategy")
public class FuelLineQueryStrategy implements TableQueryStrategy<FuelLineParams> {
    private  String tableName;

    public FuelLineQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<FuelLineParams> params) {

        ClassificationTableType cl = ClassificationTableType.fromFilter("fuel");

        query.addSubQuery("minYearQuery", new QueryBuilder()
                .append("SELECT MIN(t.year) AS minYear ")
                .append("FROM ").append(tableName).append(" t "));

        query.addSubQuery("dataQuery", new QueryBuilder()
                .append("SELECT t.year, t.fuel, ROUND(SUM(t.average_price) / 12, 1) AS average_price, ")
                .append("fc.name_").append(params.getLangName()).append(" AS fuel_name, fc.id AS fuel_id ")
                .append("FROM ").append(tableName).append(" t ")
                .append("LEFT JOIN "+cl.getTableName()+" fc ON t.fuel = fc.id ")
                .append("WHERE t.year != 2026 ")
                .addGroupBy("t.year", "t.fuel", "fc.name_" + params.getLangName(), "fc.id")
                .addOrderBy("t.year", "ASC"));
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "fuel");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year", "fuel");
    }

    @Override
    public boolean isCombinedQuery() {
        return true;
    }

    @Override
    public TableQueryStrategy<FuelLineParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
