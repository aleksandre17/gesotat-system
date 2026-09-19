package org.base.api.security.tenancy.scope;

import org.base.api.security.tenancy.ProductTenancy;
import org.base.api.security.tenancy.ProductTenancyRepository;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Data Plane objects that carry their product through {@code dataset_version_id} or through the
 * ingest batch: dataset versions, snapshots, manifests, package runs, dataset loads, batches and
 * resumable upload sessions.
 */
@Component
public class DataPlaneObjectScopeResolver implements TenantScopeResolver {
    private final ProductTenancyRepository products;
    private final Map<String, Function<Long, Optional<ProductTenancy>>> byNumber = new LinkedHashMap<>();
    private final Set<String> identities;

    public DataPlaneObjectScopeResolver(ProductTenancyRepository products) {
        this.products = products;
        byNumber.put("datasetVersionId", products::byDatasetVersionId);
        byNumber.put("snapshotId", products::bySnapshotId);
        byNumber.put("datasetSnapshotId", products::bySnapshotId);
        byNumber.put("manifestId", products::byManifestId);
        byNumber.put("runId", products::byPackageRunId);
        byNumber.put("datasetLoadId", products::byDatasetLoadId);
        byNumber.put("batchId", products::byIngestBatchId);
        Set<String> names = new LinkedHashSet<>(byNumber.keySet());
        names.add("uploadSessionId");
        this.identities = Set.copyOf(names);
    }

    @Override public String name() { return "dataPlaneObject"; }
    @Override public int order() { return 30; }
    @Override public Set<String> identities() { return identities; }

    @Override
    public Optional<ResolvedScope> resolve(TenantScopeRequest request) {
        for (Map.Entry<String, Function<Long, Optional<ProductTenancy>>> entry : byNumber.entrySet()) {
            Long key = request.number(entry.getKey());
            if (key != null) return Optional.of(new ResolvedScope(entry.getKey(), key, entry.getValue().apply(key).orElse(null)));
        }
        UUID uploadSessionId = request.uuid("uploadSessionId");
        return uploadSessionId == null ? Optional.empty()
                : Optional.of(new ResolvedScope("uploadSessionId", uploadSessionId, products.byUploadSessionId(uploadSessionId).orElse(null)));
    }
}
