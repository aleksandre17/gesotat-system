package org.base.mobile.strategy;

import org.base.mobile.params.RaceParams;
import org.base.mobile.params.RegionalBarParams;
import org.base.mobile.params.QueryParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("regionalBarStrategy")
public class RegionalBarQueryStrategy implements TableQueryStrategy<RegionalBarParams> {
    private  String tableName;

    public RegionalBarQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() { return tableName; }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<RegionalBarParams> params) {
        RegionalBarParams p = params.getParams();
        query.append("SELECT TOP 10 t.brand, t.year, ")
                .append(p.getBrand() != null ? "t.model, " : "")
                .append(p.getYearOfProduction() != null ? "t.year_of_production, " : "")
                .append(p.getRegion() != null && !"1".equals(p.getRegion()) ? "t.region, " : "")
                .append(p.getQuarter() != null && tableName.equals("[dbo].[auto_main]") ? "t.quarter, " : "")
                .append("SUM(t.quantity) AS value ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE 1=1 ");
        query.addFilter("t.year", p.getYear(), "=");
        if (tableName.equals("[dbo].[auto_main]")) {
            if (p.getQuarter() != null) {
                query.addFilter("t.quarter", p.getQuarter(), "=");
            } else {
                query.append("AND t.quarter IN (1, 2, 3, 4) ");
            }
        }
        query.addFilter("t.brand", p.getBrand(), "=")
                .addFilter("t.year_of_production", p.getYearOfProduction(), "=")
                .addFilter("t.region", p.getRegion() != null && !"1".equals(p.getRegion()) ? p.getRegion() : null, "=")
                .addGroupBy("t.brand", "t.year");
        if (p.getBrand() != null) query.addGroupBy("t.model");
        if (p.getYearOfProduction() != null) query.addGroupBy("t.year_of_production");
        if (p.getRegion() != null && !"1".equals(p.getRegion())) query.addGroupBy("t.region");
        if (p.getQuarter() != null && tableName.equals("[dbo].[auto_main]")) query.addGroupBy("t.quarter");
        query.addOrderBy("SUM(t.quantity)", "DESC");
    }

    @Override
    public List<String> getAttributes() {
        return List.of("brand", "year");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("brand", "year");
    }

    @Override
    public boolean isCombinedQuery() { return false; }

    @Override
    public TableQueryStrategy<RegionalBarParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
