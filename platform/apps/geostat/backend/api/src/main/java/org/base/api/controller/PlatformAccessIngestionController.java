package org.base.api.controller;

import org.base.api.service.platform.PlatformAccessIngestionService;
import org.base.api.service.platform.PlatformIngestReceipt;
import org.base.api.service.artifact.ArtifactMalwareAdmission;
import org.base.api.service.storage.ObjectStorageService;
import org.base.api.service.storage.StoredArtifact;
import org.base.core.anotation.Api;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** New governed Access entry point. Legacy /imports/access endpoints remain untouched. */
@Api
@RestController
@RequestMapping("/platform/access")
public class PlatformAccessIngestionController {
    private final long maxAccessArtifactBytes;
    private final PlatformAccessIngestionService ingestion;
    private final ObjectProvider<ObjectStorageService> storageProvider;
    private final ArtifactMalwareAdmission malwareAdmission;

    public PlatformAccessIngestionController(PlatformAccessIngestionService ingestion, ObjectProvider<ObjectStorageService> storageProvider,
                                              ArtifactMalwareAdmission malwareAdmission,
                                              @Value("${platform.ingest.max-artifact-bytes:1073741824}") long maxAccessArtifactBytes) {
        if (maxAccessArtifactBytes < 1) throw new IllegalArgumentException("Invalid platform ingest artifact size limit");
        this.ingestion = ingestion;
        this.storageProvider = storageProvider;
        this.malwareAdmission = malwareAdmission;
        this.maxAccessArtifactBytes = maxAccessArtifactBytes;
    }

    @PostMapping(value = "/ingest", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PlatformIngestReceipt> ingest(@RequestParam long contractSourceId, @RequestParam MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Access artifact must not be empty");
        if (file.getSize() > maxAccessArtifactBytes) throw new IllegalArgumentException("Access artifact exceeds the configured platform limit");
        String originalName = file.getOriginalFilename();
        String normalizedName = originalName == null ? "" : originalName.toLowerCase(Locale.ROOT);
        if (!normalizedName.endsWith(".accdb") && !normalizedName.endsWith(".mdb")) {
            throw new IllegalArgumentException("Only .accdb and .mdb artifacts are accepted by this endpoint");
        }
        ObjectStorageService storage = storageProvider.getIfAvailable();
        if (storage == null) throw new IllegalStateException("Private S3 storage is not configured");
        File temporary = File.createTempFile("platform-access-", ".accdb");
        try {
            file.transferTo(temporary);
            malwareAdmission.admit(temporary.toPath(), storage, "ACCESS_SOURCE_" + contractSourceId, String.valueOf(originalName), "ACCESS_PACKAGE");
            StoredArtifact artifact;
            try (var input = Files.newInputStream(temporary.toPath())) {
                artifact = storage.storeOriginal(file.getOriginalFilename(), file.getContentType(), input, file.getSize());
            }
            return ResponseEntity.ok(ingestion.ingest(temporary, contractSourceId, artifact));
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }

    /** Resume exclusively from the immutable original object recorded for the load. */
    @PostMapping("/loads/{datasetLoadId}/resume")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PlatformIngestReceipt> resume(@org.springframework.web.bind.annotation.PathVariable long datasetLoadId) throws Exception {
        ObjectStorageService storage = storageProvider.getIfAvailable();
        if (storage == null) throw new IllegalStateException("Private S3 storage is not configured");
        Path temporary = Files.createTempFile("platform-access-resume-", ".accdb");
        try {
            storage.materialize(ingestion.artifactUri(datasetLoadId), temporary);
            return ResponseEntity.ok(ingestion.resume(temporary.toFile(), datasetLoadId));
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
