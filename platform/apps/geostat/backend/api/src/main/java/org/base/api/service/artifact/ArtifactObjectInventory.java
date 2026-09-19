package org.base.api.service.artifact;

import java.time.Instant;
import java.util.List;

/** Port for enumerating stored objects, so storage can be reconciled against the registry. */
public interface ArtifactObjectInventory {

    record ListedObject(String key, long byteSize, Instant lastModified) {}

    /**
     * One page of objects under {@code prefix} in key order, strictly after {@code afterKey}
     * ({@code null} starts from the beginning). Fewer than {@code limit} results means the end.
     */
    List<ListedObject> page(String bucket, String prefix, String afterKey, int limit);
}
