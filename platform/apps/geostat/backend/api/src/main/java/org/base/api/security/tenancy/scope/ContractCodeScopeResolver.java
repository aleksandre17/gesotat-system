package org.base.api.security.tenancy.scope;

import org.base.api.security.tenancy.ProductTenancyRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/** A site contract code names its product directly; it is the most precise identity a route carries. */
@Component
public class ContractCodeScopeResolver implements TenantScopeResolver {
    private static final Set<String> IDENTITIES = Set.of("contractCode");

    private final ProductTenancyRepository products;

    public ContractCodeScopeResolver(ProductTenancyRepository products) {
        this.products = products;
    }

    @Override public String name() { return "contractCode"; }
    @Override public int order() { return 10; }
    @Override public Set<String> identities() { return IDENTITIES; }

    @Override
    public Optional<ResolvedScope> resolve(TenantScopeRequest request) {
        String contractCode = request.text("contractCode");
        return contractCode == null ? Optional.empty()
                : Optional.of(new ResolvedScope(name(), contractCode, products.byContractCode(contractCode).orElse(null)));
    }
}
