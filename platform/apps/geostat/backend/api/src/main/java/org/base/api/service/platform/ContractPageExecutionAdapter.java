package org.base.api.service.platform;

import java.util.List;
import java.util.Map;

/** Port for contract-selected page execution. */
public interface ContractPageExecutionAdapter {
    PageFamily family();
    List<Map<String,Object>> read(PageDataAdapterRegistry.ReadContext context, long revisionId,
                                  ContractPhysicalQueryService physical);
    default Long total(PageDataAdapterRegistry.ReadContext context, long revisionId,
                       ContractPhysicalQueryService physical) { return null; }
    default List<Map<String,Object>> aggregate(PageDataAdapterRegistry.ReadContext context, long revisionId,
                                               ContractPhysicalQueryService physical,
                                               ContractQueryRequest request) {
        List<Map<String,Object>> rows = read(new PageDataAdapterRegistry.ReadContext(
                context.metadata(), 1, 1000, request.filters(), Map.of(),
                context.metricCode(), context.carrierCode(), context.periodFrom(),
                context.periodTo(), context.ageGroup()), revisionId, physical);
        String field = request.filters().containsKey("value") ? "value"
                : request.filters().containsKey("numericValue") ? "numericValue" : "value";
        return ContractAggregationPlanner.aggregate(rows, request.groupBy(), field, request.aggregation());
    }
}
