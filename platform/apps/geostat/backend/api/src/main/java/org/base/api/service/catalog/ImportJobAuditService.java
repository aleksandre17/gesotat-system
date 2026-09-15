package org.base.api.service.catalog;

import lombok.RequiredArgsConstructor;
import org.base.core.entity.data.ImportJob;
import org.base.core.entity.data.ImportJobItem;
import org.base.core.entity.data.ImportJobItemStatus;
import org.base.core.entity.data.ImportJobStatus;
import org.base.core.repository.ImportJobItemRepository;
import org.base.core.repository.ImportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

/** Creates immutable import audit records before child-database writes begin. */
@Service
@RequiredArgsConstructor
@Transactional
public class ImportJobAuditService {
    private final ImportJobRepository importJobRepository;
    private final ImportJobItemRepository importJobItemRepository;

    public ImportJob open(MultipartFile file, ManagedAccessPackage accessPackage) throws IOException {
        ImportJob job = new ImportJob();
        job.setPackageCode(accessPackage == null ? null : accessPackage.packageCode());
        job.setPackageVersion(accessPackage == null ? null : accessPackage.packageVersion());
        job.setOriginalFileName(file.getOriginalFilename() == null ? "access-package" : file.getOriginalFilename());
        job.setFileChecksum(sha256(file));
        job.setFileSizeBytes(file.getSize());
        job.setStatus(ImportJobStatus.RECEIVED);
        return importJobRepository.save(job);
    }

    public void recordCatalog(ImportJob job, AccessCatalog catalog) {
        job.setStatus(ImportJobStatus.CATALOGED);
        importJobRepository.save(job);
        for (AccessTableCatalog table : catalog.tables()) {
            if (table.systemTable() || table.name().startsWith("__gs_")) {
                continue;
            }
            ImportJobItem item = new ImportJobItem();
            item.setImportJobId(job.getId());
            item.setAccessTableName(table.name());
            item.setSourceRowCount(table.rowCount());
            item.setStatus(ImportJobItemStatus.VALIDATING);
            importJobItemRepository.save(item);
        }
    }

    public void completeValidation(ImportJob job, PackageValidationResult validation) {
        if (validation.valid()) {
            job.setStatus(ImportJobStatus.VALIDATED);
        } else {
            job.setStatus(ImportJobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorSummary(validation.issues().stream()
                    .map(issue -> issue.code() + ": " + issue.message())
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("Package validation failed"));
        }
        importJobRepository.save(job);
    }

    private String sha256(MultipartFile file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (var input = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                for (int read; (read = input.read(buffer)) >= 0; ) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
