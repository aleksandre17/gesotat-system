package org.base.api.controller;

import lombok.RequiredArgsConstructor;
import org.base.api.service.artifact.ArtifactAttachmentService;
import org.base.api.service.artifact.BindingStatus;
import org.base.api.service.artifact.ArtifactDistributionService;
import org.base.api.service.artifact.ArtifactPackageDescriptor;
import org.base.api.service.artifact.ArtifactPackageDescriptors;
import org.base.api.service.artifact.ArtifactPackageService;
import org.base.api.service.artifact.ArtifactReconciliationService;
import org.base.api.service.artifact.ArtifactUploadIdentityResolver;
import org.base.api.service.artifact.ArtifactUploadSessionService;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jakarta.servlet.http.HttpServletRequest;
import org.base.api.security.tenancy.TenantScoped;
import org.base.api.security.tenancy.TenantScopeExemption;

/** Governed artifact lifecycle: package → manifest → attachment → reconciliation → signed distribution. */
@TenantScoped
@Api
@RestController
@RequestMapping("/platform/artifacts")
@RequiredArgsConstructor
public class PlatformArtifactController {
    private final ArtifactPackageService packages;
    private final ArtifactAttachmentService attachments;
    private final ArtifactReconciliationService reconciliation;
    private final ArtifactDistributionService distribution;
    private final ArtifactUploadSessionService uploadSessions;
    private final ArtifactUploadIdentityResolver uploadIdentity;
    private final ArtifactPackageDescriptors descriptors;
    private final org.base.api.service.artifact.ArtifactManifestDocuments manifestDocuments;
    private static final Pattern CONTENT_RANGE = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)");

    public record InventoryImportRequest(String packageCode, String inventoryKey, String objectPrefix) {}
    public record StartPackageUploadRequest(String packageCode, String contractCode, int revision, String datasetCode, long expectedBytes) {}

    @PostMapping("/upload-sessions")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    @TenantScopeExemption(value = TenantScopeExemption.Kind.SERVICE_ENFORCED, reason = "contractCode arrives in the request body; ArtifactPackageContractResolver.resolve enforces it before a session is opened.")
    public ResponseEntity<ArtifactUploadSessionService.SessionReceipt> startPackageUpload(
            @RequestHeader("Idempotency-Key") String idempotencyKey, @RequestBody StartPackageUploadRequest request,
            Authentication authentication) {
        var identity = uploadIdentity.resolve(authentication);
        var receipt = uploadSessions.start(new ArtifactUploadSessionService.StartRequest(request.packageCode(), request.contractCode(),
                request.revision(), request.datasetCode(), request.expectedBytes()), identity, idempotencyKey);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(receipt);
    }

    @PostMapping(value = "/upload-sessions/{uploadSessionId}/parts/{partNumber}", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactUploadSessionService.PartReceipt> uploadPackagePart(
            @PathVariable UUID uploadSessionId, @PathVariable int partNumber,
            @RequestHeader("Content-Range") String contentRange, @RequestHeader("X-Checksum-SHA256") String sha256,
            HttpServletRequest request, Authentication authentication) throws IOException {
        Matcher range = CONTENT_RANGE.matcher(contentRange);
        if (!range.matches()) throw new IllegalArgumentException("Content-Range must use bytes start-end/total");
        var receipt = uploadSessions.uploadPart(uploadSessionId, partNumber, Long.parseLong(range.group(1)),
                Long.parseLong(range.group(2)), Long.parseLong(range.group(3)), sha256, request.getInputStream(), request.getContentLengthLong(),
                uploadIdentity.resolve(authentication));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(receipt);
    }

    @PostMapping("/upload-sessions/{uploadSessionId}/complete")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactUploadSessionService.SessionReceipt> completePackageUpload(
            @PathVariable UUID uploadSessionId, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(uploadSessions.complete(uploadSessionId, uploadIdentity.resolve(authentication)));
    }

    @GetMapping("/upload-sessions/{uploadSessionId}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactUploadSessionService.SessionReceipt> packageUploadStatus(
            @PathVariable UUID uploadSessionId, Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(uploadSessions.status(uploadSessionId, uploadIdentity.resolve(authentication)));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/upload-sessions/{uploadSessionId}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<Void> cancelPackageUpload(@PathVariable UUID uploadSessionId, Authentication authentication) {
        uploadSessions.cancel(uploadSessionId, uploadIdentity.resolve(authentication));
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/manifests/inventory")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    @TenantScopeExemption(value = TenantScopeExemption.Kind.SERVICE_ENFORCED, reason = "An inventory manifest is not contract-bound at admission and names no product; it becomes reachable only through a contract-bound manifest, which ArtifactPackageContractResolver.resolve enforces.")
    public ResponseEntity<ArtifactPackageService.ManifestReceipt> importInventory(@RequestBody InventoryImportRequest request) {
        return ResponseEntity.ok(packages.importInventory(request.packageCode(), request.inventoryKey(), request.objectPrefix()));
    }

    @PostMapping(value = "/manifests/package", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactPackageService.ManifestReceipt> uploadPackage(@RequestParam String packageCode,
                                                                                @RequestParam String contractCode,
                                                                                @RequestParam int revision,
                                                                                @RequestParam String datasetCode,
                                                                                @RequestPart("package") MultipartFile archive) throws IOException {
        return ResponseEntity.ok(packages.uploadPackage(packageCode, contractCode, revision, datasetCode, archive.getInputStream()));
    }

    /** Validate-only admission: preview of structure, manifest and relations; nothing is stored. */
    @PostMapping(value = "/manifests/package/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ArtifactPackageService.PackagePreview> previewPackage(@RequestParam String packageCode,
                                                                                @RequestParam String contractCode,
                                                                                @RequestParam int revision,
                                                                                @RequestParam String datasetCode,
                                                                                @RequestPart("package") MultipartFile archive) throws IOException {
        var preview = packages.previewPackage(packageCode, contractCode, revision, datasetCode, archive.getInputStream());
        return preview.blocked() ? ResponseEntity.unprocessableEntity().body(preview) : ResponseEntity.ok(preview);
    }

    /** Approved contract-derived descriptor from which any producer assembles a conformant package. */
    @GetMapping("/contracts/{contractCode}/revisions/{revision}/datasets/{datasetCode}/package-descriptor")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<ArtifactPackageDescriptor> packageDescriptor(@PathVariable String contractCode, @PathVariable int revision,
                                                                       @PathVariable String datasetCode) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(descriptors.describe(contractCode, revision, datasetCode));
    }

    /** Accepted manifest document: derived file claims and row-to-file edges, persisted at admission. */
    @GetMapping("/manifests/{manifestId}/document")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<org.base.api.service.artifact.PackageManifestDocument> manifestDocument(@PathVariable long manifestId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(manifestDocuments.read(manifestId));
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
    @TenantScopeExemption(value = TenantScopeExemption.Kind.SERVICE_ENFORCED, reason = "recordType/externalKey resolve to a published snapshot only inside ArtifactDistributionService.published, which masks a foreign entity as an absent one.")
    public ResponseEntity<ArtifactDistributionService.PublishedArtifacts> list(@PathVariable String recordType, @PathVariable String externalKey) {
        return ResponseEntity.ok(distribution.list(recordType, externalKey));
    }

    @GetMapping("/entities/{recordType}/{externalKey}/{relationCode}/{language}/{ordinal}/download")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    @TenantScopeExemption(value = TenantScopeExemption.Kind.SERVICE_ENFORCED, reason = "recordType/externalKey resolve to a published snapshot only inside ArtifactDistributionService.published, which masks a foreign entity as an absent one.")
    public ResponseEntity<ArtifactDistributionService.SignedDownload> download(@PathVariable String recordType, @PathVariable String externalKey,
                                                                               @PathVariable String relationCode, @PathVariable String language,
                                                                               @PathVariable int ordinal, Authentication authentication) {
        Set<String> authorities = authentication == null ? Set.of()
                : authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(distribution.download(recordType, externalKey, relationCode, language, ordinal, authorities));
    }
}
