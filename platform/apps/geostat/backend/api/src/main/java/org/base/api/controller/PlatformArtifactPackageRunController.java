package org.base.api.controller;

import lombok.RequiredArgsConstructor;
import org.base.api.service.artifact.run.PackageRunService;
import org.base.core.anotation.Api;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * One-click package experience (artifact contract §26): confirm an admitted manifest, track progress,
 * retry. A run stops at steward review; it never publishes.
 */
@Api
@RestController
@RequestMapping("/platform/artifacts/package-runs")
@RequiredArgsConstructor
public class PlatformArtifactPackageRunController {
    private final PackageRunService runs;

    public record StartRequest(long manifestId) {}

    /** Accepted for asynchronous execution; starting the same manifest again returns the same run. */
    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PackageRunService.RunView> start(@RequestBody StartRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).cacheControl(CacheControl.noStore()).body(runs.start(request.manifestId(), authentication.getName()));
    }

    @GetMapping("/{runId}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PackageRunService.RunView> view(@PathVariable long runId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(runs.view(runId));
    }

    @PostMapping("/{runId}/retry")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<PackageRunService.RunView> retry(@PathVariable long runId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).cacheControl(CacheControl.noStore()).body(runs.retry(runId));
    }
}
