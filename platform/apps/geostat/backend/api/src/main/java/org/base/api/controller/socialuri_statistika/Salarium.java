package org.base.api.controller.socialuri_statistika;

import lombok.RequiredArgsConstructor;
import org.base.api.service.FileUploadService;
import org.base.core.anotation.Api;
import org.base.core.anotation.FolderPrefix;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.base.api.security.tenancy.TenantNeutral;

@TenantNeutral(reason = "Legacy per-domain Access upload that writes through the core profile/page plane and carries no data-product identity. LEGACY: recorded as a gap; migrate onto the governed ingestion line or bind to an explicit operator authority.", legacy = true)
@RequiredArgsConstructor
@RestController
@RequestMapping("/khelpasebis-kalkulatori")
@Api
@FolderPrefix
@PreAuthorize("hasAuthority('WRITE_RESOURCE')")
public class Salarium {

    private final FileUploadService fileUploadService;

    @PostMapping(value = "/salarium", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadExcelFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart String payload) {
        return fileUploadService.handleUpload(file, payload);
    }
}

