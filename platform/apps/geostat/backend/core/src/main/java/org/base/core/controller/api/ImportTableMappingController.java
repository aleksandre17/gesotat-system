package org.base.core.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.base.core.entity.data.ImportTableMapping;
import org.base.core.model.request.ImportTableMappingRequest;
import org.base.core.service.ImportTableMappingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api
@RestController
@RequestMapping("/import-table-mappings")
@RequiredArgsConstructor
public class ImportTableMappingController {
    private final ImportTableMappingService service;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<List<ImportTableMapping>> findByProfile(@RequestParam Long profileId) {
        return ResponseEntity.ok(service.findByProfile(profileId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ImportTableMapping> create(@Valid @RequestBody ImportTableMappingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ImportTableMapping> update(@PathVariable Long id, @Valid @RequestBody ImportTableMappingRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }
}
