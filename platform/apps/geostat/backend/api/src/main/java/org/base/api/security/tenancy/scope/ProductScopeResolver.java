package org.base.api.security.tenancy.scope;

import org.base.api.security.tenancy.ProductTenancyRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/** Routes that address the data product itself, by code or by identifier. */
@Component
public class ProductScopeResolver implements TenantScopeResolver {
    private static final Set<String> IDENTITIES = Set.of("productCode", "productId");

    private final ProductTenancyRepository products;

    public ProductScopeResolver(ProductTenancyRepository products) {
        this.products = products;
    }

    @Override public String name() { return "product"; }
    @Override public int order() { return 20; }
    @Override public Set<String> identities() { return IDENTITIES; }

    @Override
    public Optional<ResolvedScope> resolve(TenantScopeRequest request) {
        String productCode = request.text("productCode");
        if (productCode != null) {
            return Optional.of(new ResolvedScope("productCode", productCode, products.byProductCode(productCode).orElse(null)));
        }
        Long productId = request.number("productId");
        return productId == null ? Optional.empty()
                : Optional.of(new ResolvedScope("productId", productId, products.byProductId(productId).orElse(null)));
    }
}
