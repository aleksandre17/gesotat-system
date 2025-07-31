package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.FullRaitingParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.AccidentsGenderParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for accidents-gender endpoint queries.
 */
@Component("accidentsGenderStrategy")
public class AccidentsGenderQueryStrategy implements TableQueryStrategy<AccidentsGenderParams> {
    private  String tableName;

    public AccidentsGenderQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<AccidentsGenderParams> params) {
        AccidentsGenderParams p = (AccidentsGenderParams) params.getParams();
        query.append("SELECT t.year, t.gender, t.accidents, SUM(t.quantity) AS quantity, ")
                .append("gc.").append(params.getLangName()).append(" AS gender_name, gc.ID AS gender_id ")
                .append("FROM ").append(tableName).append(" t ")
                .append("LEFT JOIN gender_cl gc ON t.gender = gc.ID ")
                .append("WHERE t.year != 2026 ")
                .addFilter("t.accidents", p.getAccidents(), "?")
                .addGroupBy("t.year", "t.gender", "gc." + params.getLangName(), "gc.ID")
                .addOrderBy("t.year", null);
    }

    @Override
    public List<String> getAttributes() {
        return List.of("year", "gender", "accidents");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("year", "gender");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<AccidentsGenderParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
