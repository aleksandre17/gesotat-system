package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.SankeyParams;
import org.base.api.service.mobile_services.params.SlidersDataParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for sliders-data endpoint query.
 */
@Component("slidersDataStrategy")
public class SlidersDataQueryStrategy implements TableQueryStrategy<SlidersDataParams> {
    private final String tableName;

    public SlidersDataQueryStrategy() {
        this.tableName = "[dbo].[sliders_data]";
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<SlidersDataParams> params) {
        SlidersDataParams p = (SlidersDataParams) params.getParams();

        query.append("SELECT id, page, ")
                .append(p.getRegion()).append(" AS name, ")
                .append("unit, ")
                .append(p.getPeriod()).append(" AS period, ")
                .append("value, ")
                .append(p.getTitle()).append(" AS title ")
                .append("FROM ").append(tableName)
                .addOrderBy("id", "ASC");
    }

    @Override
    public List<String> getAttributes() {
        return List.of("id", "page", "name", "unit", "period", "value", "title");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of();
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<SlidersDataParams> setTableName(String tableName) {
        //this.tableName = tableName;
        return this;
    }
}
