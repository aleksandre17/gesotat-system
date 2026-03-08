package org.base.mobile.strategy;

import org.base.mobile.params.DualParams;
import org.base.mobile.params.QueryParams;
import org.base.mobile.params.EquityParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for equity endpoint queries.
 * Handles min year and dual quantity queries for ratio calculations.
 */
@Component("equityStrategy")
public class EquityQueryStrategy implements TableQueryStrategy<EquityParams> {
    private  String tableName;

    public EquityQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<EquityParams> params) {
        EquityParams p = (EquityParams) params.getParams();
        // Min year query
        query.addSubQuery("minYearQuery", new QueryBuilder()
                .append("SELECT MIN(t.year) AS minYear ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE 1=1 ")
                .addFilter("t.brand", p.getBrand(), "=")
                .addFilter("t.year_of_production", p.getYearOfProduction(), "=")
                .addFilter("t.region", p.getRegion() != null && !"1".equals(p.getRegion()) ? p.getRegion() : null, "="));

        // Query 1: Quantities with all filters
        query.addSubQuery("query1", new QueryBuilder()
                .append("SELECT t.year, ")
                .append(p.getBrand() != null ? "t.brand, " : "")
                .append(p.getYearOfProduction() != null ? "t.year_of_production, " : "")
                .append(p.getRegion() != null && !"1".equals(p.getRegion()) ? "t.region, " : "")
                .append("SUM(t.quantity) AS quantity ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE 1=1 ")
                .addFilter("t.brand", p.getBrand(), "=")
                .addFilter("t.year_of_production", p.getYearOfProduction(), "=")
                .addFilter("t.region", p.getRegion() != null && !"1".equals(p.getRegion()) ? p.getRegion() : null, "=")
                .addGroupBy(
                        "t.year",
                        p.getBrand() != null ? ", t.brand" : "",
                        p.getYearOfProduction() != null ? ", t.year_of_production" : "",
                        p.getRegion() != null && !"1".equals(p.getRegion()) ? ", t.region" : ""
                )
                .addOrderBy("t.year", "ASC"));

        // Query 2: Quantities without region filter
        query.addSubQuery("query2", new QueryBuilder()
                .append("SELECT t.year, ")
                .append(p.getBrand() != null ? "t.brand, " : "")
                .append(p.getYearOfProduction() != null ? "t.year_of_production, " : "")
                .append("SUM(t.quantity) AS quantity ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE 1=1 ")
                .addFilter("t.brand", p.getBrand(), "=")
                .addFilter("t.year_of_production", p.getYearOfProduction(), "=")
                .addGroupBy(
                        "t.year",
                        p.getBrand() != null ? ", t.brand" : "",
                        p.getYearOfProduction() != null ? ", t.year_of_production" : ""
                )
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
        return true; // Indicates multiple sub-queries
    }

    @Override
    public TableQueryStrategy<EquityParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
