package org.base.api.controller;

import lombok.RequiredArgsConstructor;
import org.base.api.service.catalog.ManagedImportPreview;
import org.base.api.service.catalog.ManagedImportPreviewService;
import org.base.api.service.catalog.ManagedImportExecutionResult;
import org.base.api.service.catalog.ManagedImportExecutionService;
import org.base.api.service.catalog.ManagedImportJobDetails;
import org.base.api.service.catalog.ManagedImportJobQueryService;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.base.api.security.tenancy.TenantNeutral;

/** New-mode preflight endpoint. It never imports rows into a child database. */
@TenantNeutral(reason = "Legacy managed import over the core profile/page metadata plane (DataProfile, PageLeafNode, per-page credentials); those rows have no data-product identity, so the route cannot be tenant-scoped without migrating the profile plane onto platform.data_product. LEGACY: recorded as a gap, not as safe; it must be migrated or bound to an explicit operator authority.", legacy = true)
@Api
@RestController
@RequestMapping("/imports/access")
@RequiredArgsConstructor
public class ManagedImportController {
    private final ManagedImportPreviewService previewService;
    private final ManagedImportExecutionService executionService;
    private final ManagedImportJobQueryService jobQueryService;

    @PostMapping(value = "/preview", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ManagedImportPreview> preview(@RequestPart("file") MultipartFile file) throws java.io.IOException {
        return ResponseEntity.ok(previewService.preview(file));
    }

    @PostMapping(value = "/execute", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ManagedImportExecutionResult> execute(@RequestPart("file") MultipartFile file) throws java.io.IOException {
        return ResponseEntity.ok(executionService.execute(file));
    }

    @org.springframework.web.bind.annotation.GetMapping("/{jobId}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<ManagedImportJobDetails> findJob(@org.springframework.web.bind.annotation.PathVariable Long jobId) {
        return ResponseEntity.ok(jobQueryService.findById(jobId));
    }
}
