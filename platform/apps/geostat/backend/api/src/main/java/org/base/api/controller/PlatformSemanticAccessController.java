package org.base.api.controller;

import org.base.api.service.platform.access.SemanticAccessPreview;
import org.base.api.service.platform.access.SemanticAccessPreviewService;
import org.base.api.service.platform.access.SemanticAccessPackageIngestionService;
import org.base.api.service.platform.PlatformPackageIngestReceipt;
import org.base.api.service.artifact.ArtifactMalwareAdmission;
import org.base.api.service.storage.ObjectStorageService;
import org.base.api.service.storage.StoredArtifact;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/** The package preflight endpoint intentionally has no import side effect. */
@Api
@RestController
@RequestMapping("/platform/access/semantic")
public class PlatformSemanticAccessController {
    private final SemanticAccessPreviewService previews;
    private final SemanticAccessPackageIngestionService ingestion;
    private final ObjectProvider<ObjectStorageService> storageProvider;
    private final ArtifactMalwareAdmission malwareAdmission;
    public PlatformSemanticAccessController(SemanticAccessPreviewService previews, SemanticAccessPackageIngestionService ingestion, ObjectProvider<ObjectStorageService> storageProvider,
                                            ArtifactMalwareAdmission malwareAdmission) { this.previews=previews; this.ingestion=ingestion; this.storageProvider=storageProvider; this.malwareAdmission=malwareAdmission; }

    @PostMapping(value="/preview",consumes="multipart/form-data")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<SemanticAccessPreview> preview(@RequestParam MultipartFile file) throws Exception { return ResponseEntity.ok(previews.preview(file)); }

    /** One artifact creates one batch and stages every contract-bound Access table with independent checkpoints. */
    @PostMapping(value="/ingest",consumes="multipart/form-data")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PlatformPackageIngestReceipt> ingest(@RequestParam MultipartFile file) throws Exception {
        if(file==null||file.isEmpty()) throw new IllegalArgumentException("Access package must not be empty");
        ObjectStorageService storage=storageProvider.getIfAvailable();
        if(storage==null) throw new IllegalStateException("Private S3 storage is not configured");
        File temporary=File.createTempFile("semantic-access-ingest-", ".accdb");
        try {
            file.transferTo(temporary);
            malwareAdmission.admit(temporary.toPath(),storage,"SEMANTIC_ACCESS",String.valueOf(file.getOriginalFilename()),"ACCESS_PACKAGE");
            StoredArtifact artifact;
            try(var input=Files.newInputStream(temporary.toPath())) { artifact=storage.storeOriginal(file.getOriginalFilename(),file.getContentType(),input,file.getSize()); }
            return ResponseEntity.ok(ingestion.ingest(temporary,artifact));
        } finally { Files.deleteIfExists(temporary.toPath()); }
    }

    @PostMapping("/batches/{batchId}/resume")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PlatformPackageIngestReceipt> resume(@org.springframework.web.bind.annotation.PathVariable long batchId) throws Exception {
        ObjectStorageService storage=storageProvider.getIfAvailable();
        if(storage==null) throw new IllegalStateException("Private S3 storage is not configured");
        Path temporary=Files.createTempFile("semantic-access-resume-", ".accdb");
        try {
            storage.materialize(ingestion.artifactUri(batchId),temporary);
            return ResponseEntity.ok(ingestion.resume(temporary.toFile(),batchId));
        } finally { Files.deleteIfExists(temporary); }
    }
}
