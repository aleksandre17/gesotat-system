package org.base.api.security.tenancy.scope;

import org.base.api.security.tenancy.ProductTenancyRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * An asynchronous operation stores the contract it was submitted for, so it resolves to that
 * contract's product. An operation row without a stored contract resolves to no product and is
 * therefore denied; the service's own owner check remains the second, independent control.
 */
@Component
public class AsyncOperationScopeResolver implements TenantScopeResolver {
    private static final Set<String> IDENTITIES = Set.of("operationId");

    private final ProductTenancyRepository products;

    public AsyncOperationScopeResolver(ProductTenancyRepository products) {
        this.products = products;
    }

    @Override public String name() { return "asyncOperation"; }
    @Override public int order() { return 50; }
    @Override public Set<String> identities() { return IDENTITIES; }

    @Override
    public Optional<ResolvedScope> resolve(TenantScopeRequest request) {
        Long operationId = request.number("operationId");
        return operationId == null ? Optional.empty()
                : Optional.of(new ResolvedScope("operationId", operationId, products.byAsyncOperationId(operationId).orElse(null)));
    }
}
