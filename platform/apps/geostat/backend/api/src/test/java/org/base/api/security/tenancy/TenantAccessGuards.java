package org.base.api.security.tenancy;

import java.util.Optional;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Test fixtures for boundaries that take a {@link TenantAccessGuard} but do not test tenancy itself. */
public final class TenantAccessGuards {
    private TenantAccessGuards() {}

    /** A caller port that always answers with the given caller and supports the SYSTEM scope. */
    public static CurrentCaller callerPort(Caller caller) {
        return new CurrentCaller() {
            @Override public Caller current() { return caller; }
            @Override public <T> T asSystem(String reason, Supplier<T> action) { return action.get(); }
        };
    }

    /** Enforcement switched off: every boundary behaves exactly as it did before tenancy existed. */
    public static TenantAccessGuard permitAll() {
        return new TenantAccessGuard(mock(ProductTenancyRepository.class),
                new TenantAccessPolicy(false, false, TenantAccessPolicy.DEFAULT_CROSS_TENANT_AUTHORITY),
                callerPort(Caller.anonymous()));
    }

    /** Enforcing guard: every resolution answers with {@code product}, decided against {@code caller}. */
    public static TenantAccessGuard enforcing(Caller caller, ProductTenancy product) {
        ProductTenancyRepository products = mock(ProductTenancyRepository.class);
        Optional<ProductTenancy> resolved = Optional.ofNullable(product);
        when(products.byContractCode(anyString())).thenReturn(resolved);
        when(products.byProductCode(anyString())).thenReturn(resolved);
        when(products.byProductId(anyLong())).thenReturn(resolved);
        when(products.byDatasetVersionId(any())).thenReturn(resolved);
        when(products.byManifestId(anyLong())).thenReturn(resolved);
        when(products.bySnapshotId(anyLong())).thenReturn(resolved);
        when(products.byPackageRunId(anyLong())).thenReturn(resolved);
        when(products.byUploadSessionId(any())).thenReturn(resolved);
        return new TenantAccessGuard(products,
                new TenantAccessPolicy(true, true, TenantAccessPolicy.DEFAULT_CROSS_TENANT_AUTHORITY),
                callerPort(caller));
    }
}
