package org.base.api.security.tenancy.scope;

import org.base.api.security.tenancy.ProductTenancyRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/** Control Plane ingestion identities: an ingestion contract and one of its declared sources. */
@Component
public class IngestionContractScopeResolver implements TenantScopeResolver {
    private static final Set<String> IDENTITIES = Set.of("contractSourceId", "contractId");

    private final ProductTenancyRepository products;

    public IngestionContractScopeResolver(ProductTenancyRepository products) {
        this.products = products;
    }

    @Override public String name() { return "ingestionContract"; }
    @Override public int order() { return 40; }
    @Override public Set<String> identities() { return IDENTITIES; }

    @Override
    public Optional<ResolvedScope> resolve(TenantScopeRequest request) {
        Long contractSourceId = request.number("contractSourceId");
        if (contractSourceId != null) {
            return Optional.of(new ResolvedScope("contractSourceId", contractSourceId,
                    products.byContractSourceId(contractSourceId).orElse(null)));
        }
        Long contractId = request.number("contractId");
        return contractId == null ? Optional.empty()
                : Optional.of(new ResolvedScope("contractId", contractId, products.byIngestionContractId(contractId).orElse(null)));
    }
}
