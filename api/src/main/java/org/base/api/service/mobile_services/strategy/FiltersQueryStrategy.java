package org.base.api.service.mobile_services.strategy;

import org.base.api.service.mobile_services.params.FiltersParams;
import org.base.api.service.mobile_services.params.QueryParams;
import org.base.core.service.QueryBuilder;

public interface FiltersQueryStrategy extends TableQueryStrategy<FiltersParams> {
    QueryBuilder configureTopModelQuery(QueryParams<FiltersParams> params);
}
