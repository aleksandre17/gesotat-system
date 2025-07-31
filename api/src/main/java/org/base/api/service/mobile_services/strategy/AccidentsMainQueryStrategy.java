package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.AccidentsGenderParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.AccidentsMainParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for accidents-main endpoint queries.
 */
@Component("accidentsMainStrategy")
public class AccidentsMainQueryStrategy implements TableQueryStrategy<AccidentsMainParams> {
    private  String tableName;

    public AccidentsMainQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<AccidentsMainParams> params) {
        AccidentsMainParams p = (AccidentsMainParams) params.getParams();
        query.append("SELECT t.year, t.region, t.accidents, SUM(t.quantity) AS quantity ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE t.year != 2026 ")
                .addFilter("t.region", p.getRegion(), "=")
                .addFilter("t.accidents", p.getAccidents(), "=")
                .addGroupBy("t.accidents", "t.year", "t.region")
                .addOrderBy("t.year", "ASC");
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "region", "accidents");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year", "region");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<AccidentsMainParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

}
