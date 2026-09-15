package org.base.mobile.strategy;

import org.base.mobile.params.QueryParams;
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

