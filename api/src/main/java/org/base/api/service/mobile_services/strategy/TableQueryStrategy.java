package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.QueryParams;
import org.base.core.service.QueryBuilder;

import java.util.List;

public interface TableQueryStrategy<T> {
    String getTableName();
    void configureQuery(QueryBuilder query, QueryParams<T> params);
    List<String> getAttributes();
    List<String> getGroupBy();
    boolean isCombinedQuery();
    TableQueryStrategy<T> setTableName(String tableName);
}

