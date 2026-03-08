package org.base.mobile.strategy;

import org.base.mobile.params.RegionalBarParams;
import org.base.mobile.params.RegionalMapParams;
import org.base.mobile.params.QueryParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("regionalMapStrategy")
public class RegionalMapQueryStrategy implements TableQueryStrategy<RegionalMapParams> {
    private  String tableName;

    public RegionalMapQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() { return tableName; }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<RegionalMapParams> params) {
        RegionalMapParams p = params.getParams();
        query.append("SELECT t.region, ")
                .append(p.getBrand() != null ? "t.brand, " : "")
                .append(p.getQuarter() != null && tableName.equals("[dbo].[auto_main]") ? "t.quarter, " : "")
                .append(p.getYearOfProduction() != null ? "t.year_of_production, " : "")
                .append("SUM(t.quantity) AS quantity, ")
                .append("rc.").append(params.getLangName()).append(" AS name, ")
                .append("rc.id AS code ")
                .append("FROM ").append(tableName).append(" t ")
                .append("LEFT JOIN CL.cl_region rc ON t.region = rc.id ")
                .append("WHERE 1=1 ");
        query.addFilter("t.year", p.getYear(), "=");
        if (tableName.equals("[dbo].[auto_main]")) {
            if (p.getQuarter() != null && !p.getQuarter().equals("99")) {
                query.addFilter("t.quarter", p.getQuarter(), "=");
            } else {
                query.append("AND t.quarter IN (1, 2, 3, 4) ");
            }
        }
        query.addFilter("t.brand", p.getBrand(), "=")
                .addFilter("t.year_of_production", p.getYearOfProduction(), "=")
                .addGroupBy("t.region")
                .addGroupBy("rc." + params.getLangName())
                .addGroupBy("rc.id");
        if (p.getBrand() != null) query.addGroupBy("t.brand");
        if (p.getQuarter() != null && tableName.equals("[dbo].[auto_main]")) query.addGroupBy("t.quarter");
        if (p.getYearOfProduction() != null) query.addGroupBy("t.year_of_production");
        query.addOrderBy("SUM(t.quantity)", "DESC");
    }


    @Override
    public List<String> getAttributes() {
        return List.of("region");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("region");
    }

    @Override
    public boolean isCombinedQuery() { return false; }

    @Override
    public TableQueryStrategy<RegionalMapParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
