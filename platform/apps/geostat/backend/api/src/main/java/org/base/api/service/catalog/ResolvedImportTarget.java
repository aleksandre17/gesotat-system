package org.base.api.service.catalog;

import org.base.api.model.request.UploadPayload;

/** Approved server-side target resolved from core metadata, never from an Access secret. */
public record ResolvedImportTarget(Long profileId, Long mappingId, String sourceTableName,
                                   String targetTableName, UploadPayload payload) {
}
