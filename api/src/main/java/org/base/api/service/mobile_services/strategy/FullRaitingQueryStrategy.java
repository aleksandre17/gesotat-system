package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.QueryParams;
import org.base.api.service.mobile_services.params.FullRaitingParams;
import org.base.core.service.QueryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Strategy for full-raiting endpoint query.
 */
@Component("fullRaitingStrategy")
public class FullRaitingQueryStrategy implements TableQueryStrategy<FullRaitingParams> {
    private String tableName;
    private final int pageSize = 10; // Default page size

    //@Value("${app.pagination.page-size:10}") int pageSize,
    public FullRaitingQueryStrategy() {
        this.tableName = "main_auto";
        //this.pageSize = pageSize;
    }

    @Override
    public String getTableName() {
        return tableName;
    }

    @Override
    public void configureQuery(QueryBuilder query, QueryParams<FullRaitingParams> params) {
        FullRaitingParams p = (FullRaitingParams) params.getParams();

        query.append("SELECT t.brand, t.model, SUM(t.quantity) AS value ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE 1=1 ");

        if (p.getYear() != null) {
            query.addFilter("t.year", p.getYear(), "=");
        }
        if (p.getTransport() != null && !p.getTransport().equals("99")) {
            query.addFilter("t.transport", p.getTransport(), "=");
        }
        if (p.getSearch() != null && !p.getSearch().isEmpty()) {
            query.addFilter("t.model", p.getSearch(), "LIKE");
        }

        query.addGroupBy("t.model", "t.brand", "t.year"); // GROUP BY before ORDER BY

        switch (p.getSort()) {
            case "ascQuantity" -> query.addOrderBy("SUM(t.quantity)", "ASC");
            case "ascModel" -> query.addOrderBy("t.brand", "ASC").addOrderBy("t.model", "ASC");
            case "descModel" -> query.addOrderBy("t.brand", "DESC").addOrderBy("t.model", "DESC");
            default -> query.addOrderBy("SUM(t.quantity)", "DESC");
        }

// Pagination AFTER ORDER BY
        int limit = 10;
        int page = p.getPage() != null && p.getPage() > 0 ? p.getPage() : 1;
        query.setPagination(limit * (page - 1), limit);
    }

    public QueryBuilder configureCountQuery(QueryParams<FullRaitingParams> params) {
        FullRaitingParams p = params.getParams();
        QueryBuilder countQuery = new QueryBuilder();
        countQuery.append("SELECT COUNT(DISTINCT t.model) AS count ")
                .append("FROM ").append(tableName).append(" t ")
                .append("WHERE 1=1 ");
        if (p.getYear() != null) {
            countQuery.addFilter("t.year", p.getYear(), "=");
        }
        if (p.getTransport() != null && !p.getTransport().equals("99")) {
            countQuery.addFilter("t.transport", p.getTransport(), "=");
        }
        if (p.getSearch() != null && !p.getSearch().isEmpty()) {
            countQuery.addFilter("t.model", p.getSearch(), "LIKE"); //"%?%"
        }
        return countQuery;
    }

    @Override
    public List<String> getAttributes() {
        return List.of("brand", "model");
    }

    @Override
    public List<String> getGroupBy() {
        return List.of("brand", "model", "year");
    }

    @Override
    public boolean isCombinedQuery() {
        return false;
    }

    @Override
    public TableQueryStrategy<FullRaitingParams> setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

}
