package org.base.mobile.strategy;


import org.base.mobile.params.RoadLengthParams;
import org.base.mobile.params.QueryParams;
import org.base.core.model.ClassificationTableType;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("roadLengthStrategy")
public class RoadLengthQueryStrategy implements TableQueryStrategy<RoadLengthParams> {
    private  String tableName;

    public RoadLengthQueryStrategy() {
        //this.tableName = tableName;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<RoadLengthParams> params) {
        RoadLengthParams p = (RoadLengthParams) params.getParams();
        ClassificationTableType tableType = ClassificationTableType.fromFilter("road");
        query.append("SELECT SUM(t.length) AS length, t.category, t.region, t.year, ")
                .append("rc.name_").append(params.getLangName()).append(" AS name, rc.id AS code ")
                .append("FROM ").append(tableName).append(" t ")
                .append("LEFT JOIN "+tableType.getTableName()+" rc ON t.category = rc.id ")
                .append("WHERE 1=1 ")
                .addFilter("t.year", p.getYear(), "=")
                .addFilter("t.region", p.getRegion(), "=")
                .addGroupBy("t.year", "t.region", "t.category", "rc.name_" + params.getLangName(), "rc.id");
    }

    @Override
    public List<String> getAttributes() {
        return List.of("category");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("category");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<RoadLengthParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
