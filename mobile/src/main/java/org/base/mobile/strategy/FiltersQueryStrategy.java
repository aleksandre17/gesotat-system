package org.base.mobile.strategy;

import org.base.mobile.params.FiltersParams;
import org.base.mobile.params.QueryParams;
import org.base.core.service.QueryBuilder;

public interface FiltersQueryStrategy extends TableQueryStrategy<FiltersParams> {
    QueryBuilder configureTopModelQuery(QueryParams<FiltersParams> params);
}
