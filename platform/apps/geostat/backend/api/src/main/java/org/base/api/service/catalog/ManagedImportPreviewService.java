package org.base.api.service.catalog;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.data.ImportJob;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Coordinates cataloging and validation without changing a child database. */
@Service
@RequiredArgsConstructor
public class ManagedImportPreviewService {
    private final AccessCatalogService accessCatalogService;
    private final ManagedAccessPackageReader packageReader;
    private final ManagedPackageValidationService validationService;
    private final ImportJobAuditService auditService;

    public ManagedImportPreview preview(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An Access file is required");
        }

        File tempFile = File.createTempFile("access-catalog-", ".accdb");
        try {
            file.transferTo(tempFile);
            AccessCatalog catalog = accessCatalogService.catalog(tempFile);
            ManagedAccessPackage accessPackage = catalog.managedPackage() ? packageReader.read(tempFile) : null;
            ImportJob job = auditService.open(file, accessPackage);
            auditService.recordCatalog(job, catalog);

            if (!catalog.managedPackage()) {
                return new ManagedImportPreview(job.getId(), false, catalog,
                        new PackageValidationResult(false, java.util.List.of(new PackageValidationIssue(
                                "LEGACY_PACKAGE", "package", "Managed metadata tables are not present"))));
            }

            PackageValidationResult validation = validationService.validate(accessPackage, catalog);
            auditService.completeValidation(job, validation);
            return new ManagedImportPreview(job.getId(), true, catalog, validation);
        } finally {
            Files.deleteIfExists(tempFile.toPath());
        }
    }
}
