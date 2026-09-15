package org.base.api.controller;

import lombok.RequiredArgsConstructor;
import org.base.api.service.platform.PlatformIngestReceipt;
import org.base.api.service.platform.PlatformIngestRequest;
import org.base.api.service.platform.PlatformIngestionService;
import org.base.api.service.platform.PlatformSnapshotPreparationService;
import org.base.api.service.platform.PlatformValidationService;
import org.base.api.service.platform.PrepareSnapshotRequest;
import org.base.api.service.platform.ValidationReceipt;
import org.base.api.service.platform.MaterializationReceipt;
import org.base.api.service.platform.MaterializeRequest;
import org.base.api.service.platform.SemanticMaterializationService;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Staging endpoint for a contract-resolved artifact; publication is a separate governed operation. */
@Api
@RestController
@RequestMapping("/platform/ingestion")
@RequiredArgsConstructor
public class PlatformIngestionController {
    private final PlatformIngestionService ingestionService;
    private final PlatformValidationService validationService;
    private final PlatformSnapshotPreparationService snapshotPreparationService;
    private final SemanticMaterializationService semanticMaterializationService;

    @PostMapping("/stage")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PlatformIngestReceipt> stage(@RequestBody PlatformIngestRequest request) {
        return ResponseEntity.ok(ingestionService.stage(request));
    }

    @PostMapping("/validate/{datasetLoadId}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ValidationReceipt> validate(@org.springframework.web.bind.annotation.PathVariable long datasetLoadId) {
        return ResponseEntity.ok(validationService.validate(datasetLoadId));
    }

    @PostMapping("/prepare-snapshot")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<Long> prepareSnapshot(@RequestBody PrepareSnapshotRequest request) {
        return ResponseEntity.ok(snapshotPreparationService.prepare(request));
    }

    @PostMapping("/materialize")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<MaterializationReceipt> materialize(@RequestBody MaterializeRequest request) {
        return ResponseEntity.ok(semanticMaterializationService.materialize(request));
    }
}
