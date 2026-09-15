package org.base.api.controller;

import lombok.RequiredArgsConstructor;
import org.base.api.service.platform.PlatformPublicationService;
import org.base.api.service.platform.PublicationReceipt;
import org.base.api.service.platform.PublishSnapshotRequest;
import org.base.api.service.platform.RollbackPublicationRequest;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api
@RestController
@RequestMapping("/platform/publication")
@RequiredArgsConstructor
public class PlatformPublicationController {
    private final PlatformPublicationService publicationService;

    @PostMapping("/publish")
    @PreAuthorize("hasAuthority('PUBLISH_RESOURCE')")
    public ResponseEntity<PublicationReceipt> publish(@RequestBody PublishSnapshotRequest request) {
        return ResponseEntity.ok(publicationService.publish(request));
    }

    @PostMapping("/rollback")
    @PreAuthorize("hasAuthority('PUBLISH_RESOURCE')")
    public ResponseEntity<PublicationReceipt> rollback(@RequestBody RollbackPublicationRequest request) {
        return ResponseEntity.ok(publicationService.rollback(request));
    }
}
