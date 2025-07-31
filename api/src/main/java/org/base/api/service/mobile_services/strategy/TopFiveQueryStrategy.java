package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.SlidersDataParams;
import org.base.api.service.mobile_services.params.TopFiveParams;
import org.base.core.service.QueryBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Strategy for top-five endpoint query.
 */
@Component("topFiveStrategy")
public class TopFiveQueryStrategy implements TableQueryStrategy<TopFiveParams> {
    private  String tableName;

    public TopFiveQueryStrategy() {
        this.tableName = "[dbo].[eoyes]";
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<TopFiveParams> params) {
        TopFiveParams p = (TopFiveParams) params.getParams();

        query.append("SELECT t.brand, t.model, SUM(t.quantity) AS value ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE 1=1 ");

        if (p.getYear() != null) {
            query.addFilter("t.year", p.getYear(), "=");
        }

        if (Objects.equals(tableName, "[dbo].[auto_main]")) {
            if (p.getQuarter() != null && !p.getQuarter().equals("99")) {
                query.addFilter("t.quarter", p.getQuarter(), "=");
            } else {
                query.addFilter(new QueryBuilder.InFilter("t.quarter", List.of(1, 2, 3, 4)));
            }
        }

        if (p.getTransport() != null && !p.getTransport().equals("99")) {
            query.addFilter("t.transport", p.getTransport(), "=");
        }

        query.addGroupBy("t.brand", "t.model")
                .addOrderByExpression("SUM(t.quantity)", "DESC")
                .setPagination(0, 5); // MSSQL pagination
    }

    @Override
    public List<String> getAttributes() {
        return List.of("brand", "model");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("brand", "model");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<TopFiveParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }
}
