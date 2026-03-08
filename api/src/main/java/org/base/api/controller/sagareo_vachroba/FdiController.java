package org.base.api.controller.sagareo_vachroba;

import lombok.RequiredArgsConstructor;
import org.base.api.service.FileUploadService;
import org.base.core.anotation.Api;
import org.base.core.anotation.FolderPrefix;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/pui-portali")
@Api
@FolderPrefix
public class FdiController {

    private final FileUploadService fileUploadService;

    @PostMapping(value = {
            "/country_sector",
            "/fdi_components",
            "/fdi_countreis",
            "/fdi_data",
            "/fdi_sector",
            "/fdi_total",
            "fdi_sector_countries",
            "/sectors"
    }, consumes = "multipart/form-data")
    public ResponseEntity<String> uploadExcelFile(
            @RequestPart("file") MultipartFile file,
            @RequestPart String payload) {
        return fileUploadService.handleUpload(file, payload);
    }
}

