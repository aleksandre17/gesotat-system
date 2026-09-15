package org.base.api.controller;

import org.base.api.service.platform.PlatformIngestReceipt;
import org.base.api.service.platform.PlatformSqlIngestionService;
import org.base.api.service.storage.ObjectStorageService;
import org.base.core.anotation.Api;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api
@RestController
@RequestMapping("/platform/sql")
public class PlatformSqlIngestionController {
    private final PlatformSqlIngestionService ingestion;
    private final ObjectProvider<ObjectStorageService> storage;
    public PlatformSqlIngestionController(PlatformSqlIngestionService ingestion, ObjectProvider<ObjectStorageService> storage) { this.ingestion=ingestion; this.storage=storage; }
    @PostMapping("/ingest") @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PlatformIngestReceipt> ingest(@RequestParam long contractSourceId, @RequestParam long sourceConnectionId) throws Exception {
        ObjectStorageService configured = storage.getIfAvailable(); if (configured == null) throw new IllegalStateException("Private S3 storage is not configured");
        return ResponseEntity.ok(ingestion.ingest(contractSourceId, sourceConnectionId, configured));
    }
}
