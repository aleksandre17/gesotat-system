package org.base.api.security.tenancy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * The enforcement facade every boundary calls. It resolves the owning product of the addressed object
 * exactly once, asks {@link TenantAccessPolicy} and reports the result in the two shapes a boundary
 * needs:
 *
 * <ul>
 *   <li>{@code permits*} — a boolean, for boundaries that already answer 404 for an unknown object and
 *       must answer identically for an object of another tenant (no existence oracle);</li>
 *   <li>{@code require*} — throws {@link TenantAccessDeniedException} (403 RFC 9457, {@code no-store})
 *       for boundaries whose unknown-object answer is not a 404.</li>
 * </ul>
 */
@Component
public class TenantAccessGuard {
    private static final Logger log = LoggerFactory.getLogger(TenantAccessGuard.class);

    private final ProductTenancyRepository products;
    private final TenantAccessPolicy policy;
    private final CurrentCaller callers;

    public TenantAccessGuard(ProductTenancyRepository products, TenantAccessPolicy policy, CurrentCaller callers) {
        this.products = products;
        this.policy = policy;
        this.callers = callers;
    }

    public boolean permitsContract(String contractCode) {
        return permits("contract", contractCode, () -> products.byContractCode(contractCode));
    }

    public boolean permitsDatasetVersion(Long datasetVersionId) {
        return permits("datasetVersion", datasetVersionId, () -> products.byDatasetVersionId(datasetVersionId));
    }

    public boolean permitsProductCode(String productCode) {
        return permits("product", productCode, () -> products.byProductCode(productCode));
    }

    public boolean permitsManifest(long manifestId) {
        return permits("manifest", manifestId, () -> products.byManifestId(manifestId));
    }

    public boolean permitsSnapshot(long datasetSnapshotId) {
        return permits("snapshot", datasetSnapshotId, () -> products.bySnapshotId(datasetSnapshotId));
    }

    public boolean permitsPackageRun(long packageRunId) {
        return permits("packageRun", packageRunId, () -> products.byPackageRunId(packageRunId));
    }

    public boolean permitsUploadSession(UUID uploadSessionId) {
        return permits("uploadSession", uploadSessionId, () -> products.byUploadSessionId(uploadSessionId));
    }

    public void requireProductId(long productId) {
        require("productId", productId, () -> products.byProductId(productId));
    }

    public void requireDatasetLoad(long datasetLoadId) {
        require("datasetLoad", datasetLoadId, () -> products.byDatasetLoadId(datasetLoadId));
    }

    /**
     * Decides against a product an enforcement point has already resolved, so a route identity is
     * never looked up twice in one request. A {@code null} product is denied exactly like a foreign
     * one: an identifier that names nothing must not be distinguishable from one that names another
     * tenant's object.
     */
    public void require(String scopeKind, Object scopeKey, ProductTenancy product) {
        Caller caller = callers.current();
        TenantAccessOutcome outcome = policy.decide(caller, product);
        if (!outcome.allowed()) {
            log.warn("tenancy.denied outcome={} caller={} scope={}:{}", outcome, caller.auditLabel(), scopeKind, scopeKey);
            throw new TenantAccessDeniedException(outcome);
        }
    }

    public void requireContract(String contractCode) {
        require("contract", contractCode, () -> products.byContractCode(contractCode));
    }

    public void requireDatasetVersion(Long datasetVersionId) {
        require("datasetVersion", datasetVersionId, () -> products.byDatasetVersionId(datasetVersionId));
    }

    public void requireProductCode(String productCode) {
        require("product", productCode, () -> products.byProductCode(productCode));
    }

    public void requireManifest(long manifestId) {
        require("manifest", manifestId, () -> products.byManifestId(manifestId));
    }

    public void requireSnapshot(long datasetSnapshotId) {
        require("snapshot", datasetSnapshotId, () -> products.bySnapshotId(datasetSnapshotId));
    }

    public void requirePackageRun(long packageRunId) {
        require("packageRun", packageRunId, () -> products.byPackageRunId(packageRunId));
    }

    public void requireUploadSession(UUID uploadSessionId) {
        require("uploadSession", uploadSessionId, () -> products.byUploadSessionId(uploadSessionId));
    }

    private boolean permits(String scopeKind, Object scopeKey, Supplier<Optional<ProductTenancy>> resolver) {
        return decide(scopeKind, scopeKey, resolver).allowed();
    }

    private void require(String scopeKind, Object scopeKey, Supplier<Optional<ProductTenancy>> resolver) {
        TenantAccessOutcome outcome = decide(scopeKind, scopeKey, resolver);
        if (!outcome.allowed()) throw new TenantAccessDeniedException(outcome);
    }

    private TenantAccessOutcome decide(String scopeKind, Object scopeKey, Supplier<Optional<ProductTenancy>> resolver) {
        Caller caller = callers.current();
        ProductTenancy product = resolver.get().orElse(null);
        TenantAccessOutcome outcome = policy.decide(caller, product);
        if (!outcome.allowed()) {
            // Audit line without token content: the decision, the caller's mode and the addressed scope.
            log.warn("tenancy.denied outcome={} caller={} scope={}:{}", outcome, caller.auditLabel(), scopeKind, scopeKey);
        }
        return outcome;
    }
}
