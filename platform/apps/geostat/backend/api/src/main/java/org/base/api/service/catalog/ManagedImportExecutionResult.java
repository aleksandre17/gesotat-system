package org.base.api.service.catalog;

import org.base.core.entity.data.ImportJobStatus;

public record ManagedImportExecutionResult(Long importJobId, ImportJobStatus status) {
}
