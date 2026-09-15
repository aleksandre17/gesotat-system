package org.base.api.service.platform;

import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Registry of provider/family execution ports. Selection is made from the
 * contract-declared family; the orchestration service never switches on a
 * concrete family or physical table name.
 */
@Component
public final class ContractPageExecutionRegistry {
    private final EnumMap<PageFamily, ContractPageExecutionAdapter> adapters = new EnumMap<>(PageFamily.class);

    public ContractPageExecutionRegistry(ContractPhysicalQueryService physical) {
        ContractPageExecutionAdapter relational = new ContractPageExecutionAdapter() {
            @Override public PageFamily family() { return PageFamily.ENTITY; }
            @Override public List<Map<String,Object>> read(PageDataAdapterRegistry.ReadContext c, long rid, ContractPhysicalQueryService p) {
                // Relations are an explicit query concern.  Expanding every
                // declared edge for a plain page read makes an entity query
                // depend on unrelated reference/child tables and turns a
                // bounded request into an accidental fan-out.  Relation
                // predicates still use the graph evaluator below.
                return c.where().isEmpty() ? p.rows(rid, dataset(c), c.page(), c.limit(), c.filters(), null, false)
                        : p.rowsWithRelationsWhere(rid, dataset(c), c.page(), c.limit(), c.where(), null, false);
            }
            @Override public Long total(PageDataAdapterRegistry.ReadContext c, long rid, ContractPhysicalQueryService p) { return p.count(rid, dataset(c), c.filters()); }
            private String dataset(PageDataAdapterRegistry.ReadContext c) { return String.valueOf(c.metadata().get("dataset_code")); }
        };
        for (PageFamily family : new PageFamily[]{PageFamily.ENTITY, PageFamily.STATISTICAL,
                PageFamily.REFERENCE, PageFamily.RAW, PageFamily.GEO, PageFamily.RELATION, PageFamily.UNKNOWN}) {
            final PageFamily selected = family;
            adapters.put(selected, new ContractPageExecutionAdapter() {
                @Override public PageFamily family() { return selected; }
                @Override public List<Map<String,Object>> read(PageDataAdapterRegistry.ReadContext c, long rid, ContractPhysicalQueryService p) {
                    return c.where().isEmpty() ? p.rows(rid, dataset(c), c.page(), c.limit(), c.filters(), null, false)
                            : p.rowsWithRelationsWhere(rid, dataset(c), c.page(), c.limit(), c.where(), null, false);
                }
                @Override public Long total(PageDataAdapterRegistry.ReadContext c, long rid, ContractPhysicalQueryService p) { return p.count(rid, dataset(c), c.filters()); }
                private String dataset(PageDataAdapterRegistry.ReadContext c) { return String.valueOf(c.metadata().get("dataset_code")); }
            });
        }
    }

    public ContractPageExecutionAdapter require(PageFamily family) {
        ContractPageExecutionAdapter adapter = adapters.get(Objects.requireNonNull(family, "family"));
        if (adapter == null) throw new IllegalArgumentException("No contract page adapter for " + family);
        return adapter;
    }
}
