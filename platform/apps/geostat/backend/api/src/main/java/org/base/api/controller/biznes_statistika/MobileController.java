package org.base.api.controller.biznes_statistika;

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
@RequestMapping("/automobilebis-statistikis-portali")
@Api
@FolderPrefix
@PreAuthorize("hasAuthority('WRITE_RESOURCE')")
public class MobileController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = {
            "/slider-data",
            "/cars-current-period",
            "/cars-end-period",
            "/cars-per-1000",
            "/road-accidents",
            "/vehicles-export-import",
            "/fuel-import-export",
            "/roads",
            "/fuel",
            "/sagareo-vachrobis-portali",
            "/driver-licenses"
    }, consumes = "multipart/form-data")
    public ResponseEntity<String> uploadExcelFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart String payload) {
        return fileUploadService.handleUpload(file, payload);
    }
}

