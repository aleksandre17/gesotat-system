package org.base.api.service.catalog;

/** Safe pre-import response; no child database write has occurred at this stage. */
public record ManagedImportPreview(Long importJobId, boolean managedPackage, AccessCatalog catalog,
                                   PackageValidationResult validation) {
}
