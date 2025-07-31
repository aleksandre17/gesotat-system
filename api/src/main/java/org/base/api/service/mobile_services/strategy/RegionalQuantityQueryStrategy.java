package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.RegionalMapParams;
import org.base.api.service.mobile_services.params.RegionalQuantityParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("regionalQuantityItemStrategy")
public class RegionalQuantityQueryStrategy implements TableQueryStrategy<RegionalQuantityParams> {
    private  String tableName;

    public RegionalQuantityQueryStrategy() {
       // this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<RegionalQuantityParams> params) {
        RegionalQuantityParams p = params.getParams();
        query.append("SELECT t.year, ")
                .append(p.getBrand() != null ? "t.brand, " : "")
                .append(p.getYearOfProduction() != null ? "t.year_of_production, " : "")
                .append(p.getRegion() != null && !"1".equals(p.getRegion()) ? "t.region, rc." + params.getLangName() + " AS region_name, rc.id AS region_id, " : "")
                .append("SUM(t.quantity) AS value ")
                .append("FROM ").append(tableName).append(" t ");
        if (p.getRegion() != null && !"1".equals(p.getRegion())) {
            query.append("LEFT JOIN CL.cl_region rc ON t.region = rc.id ");
        }
        query.append("WHERE 1=1 ")
                .addFilter("t.brand", p.getBrand(), "=")
                .addFilter("t.year_of_production", p.getYearOfProduction(), "=")
                .addFilter("t.region", p.getRegion() != null && !"1".equals(p.getRegion()) ? p.getRegion() : null, "=")
                .addGroupBy("t.year");
        if (p.getBrand() != null) query.addGroupBy("t.brand");
        if (p.getYearOfProduction() != null) query.addGroupBy("t.year_of_production");
        if (p.getRegion() != null && !"1".equals(p.getRegion())) {
            query.addGroupBy("t.region", "rc." + params.getLangName(), "rc.id");
        }
        query.addOrderBy("t.year", "ASC");
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
        return false;
    }

    @Override
    public TableQueryStrategy<RegionalQuantityParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
