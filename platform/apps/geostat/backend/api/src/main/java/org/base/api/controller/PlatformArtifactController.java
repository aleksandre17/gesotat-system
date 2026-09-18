package org.base.api.controller;

import lombok.RequiredArgsConstructor;
import org.base.api.service.artifact.ArtifactAttachmentService;
import org.base.api.service.artifact.BindingStatus;
import org.base.api.service.artifact.ArtifactDistributionService;
import org.base.api.service.artifact.ArtifactPackageService;
import org.base.api.service.artifact.ArtifactReconciliationService;
import org.base.core.anotation.Api;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/** Governed artifact lifecycle: package → manifest → attachment → reconciliation → signed distribution. */
@Api
@RestController
@RequestMapping("/platform/artifacts")
@RequiredArgsConstructor
public class PlatformArtifactController {
    private final ArtifactPackageService packages;
    private final ArtifactAttachmentService attachments;
    private final ArtifactReconciliationService reconciliation;
    private final ArtifactDistributionService distribution;

    public record InventoryImportRequest(String packageCode, String inventoryKey, String objectPrefix) {}

    @PostMapping("/manifests/inventory")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactPackageService.ManifestReceipt> importInventory(@RequestBody InventoryImportRequest request) {
        return ResponseEntity.ok(packages.importInventory(request.packageCode(), request.inventoryKey(), request.objectPrefix()));
    }

    @PostMapping(value = "/manifests/package", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactPackageService.ManifestReceipt> uploadPackage(@RequestParam String packageCode,
                                                                                @RequestPart("package") MultipartFile archive) throws IOException {
        return ResponseEntity.ok(packages.uploadPackage(packageCode, archive.getInputStream()));
    }

    @PostMapping("/manifests/{manifestId}/verification")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactPackageService.ManifestReceipt> verify(@PathVariable long manifestId) {
        return ResponseEntity.ok(packages.verify(manifestId));
    }

    @PostMapping("/snapshots/{snapshotId}/attachments")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactAttachmentService.BindingReport> bind(@PathVariable long snapshotId, @RequestParam long manifestId,
                                                                        @RequestParam(defaultValue = "true") boolean dryRun) {
        ArtifactAttachmentService.BindingReport report = attachments.bind(snapshotId, manifestId, dryRun);
        return report.status() == BindingStatus.BLOCKED ? ResponseEntity.unprocessableEntity().body(report) : ResponseEntity.ok(report);
    }

    @PostMapping("/snapshots/{snapshotId}/reconciliation")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactReconciliationService.ReconciliationReport> reconcile(@PathVariable long snapshotId) {
        return ResponseEntity.ok(reconciliation.reconcile(snapshotId));
    }

    @GetMapping("/entities/{recordType}/{externalKey}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<ArtifactDistributionService.PublishedArtifacts> list(@PathVariable String recordType, @PathVariable String externalKey) {
        return ResponseEntity.ok(distribution.list(recordType, externalKey));
    }

    @GetMapping("/entities/{recordType}/{externalKey}/{relationCode}/{language}/{ordinal}/download")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<ArtifactDistributionService.SignedDownload> download(@PathVariable String recordType, @PathVariable String externalKey,
                                                                               @PathVariable String relationCode, @PathVariable String language,
                                                                               @PathVariable int ordinal, Authentication authentication) {
        Set<String> authorities = authentication == null ? Set.of()
                : authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(distribution.download(recordType, externalKey, relationCode, language, ordinal, authorities));
    }
}
