package org.base.api.service.catalog;

import org.base.core.entity.data.ImportJob;
import org.base.core.entity.data.ImportJobItem;

import java.util.List;

/** API view of the complete, non-secret audit trail for one import job. */
public record ManagedImportJobDetails(ImportJob job, List<ImportJobItem> items) {
}
