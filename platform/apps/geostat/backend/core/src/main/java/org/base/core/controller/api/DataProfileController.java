package org.base.core.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.base.core.entity.data.DataProfile;
import org.base.core.model.request.DataProfileRequest;
import org.base.core.service.DataProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin API; public dynamic table/chart APIs are added in a later phase. */
@Api
@RestController
@RequestMapping("/data-profiles")
@RequiredArgsConstructor
public class DataProfileController {

    private final DataProfileService dataProfileService;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<List<DataProfile>> findAll() {
        return ResponseEntity.ok(dataProfileService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<DataProfile> findById(@PathVariable Long id) {
        return ResponseEntity.ok(dataProfileService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<DataProfile> create(@Valid @RequestBody DataProfileRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dataProfileService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<DataProfile> update(@PathVariable Long id, @Valid @RequestBody DataProfileRequest request) {
        return ResponseEntity.ok(dataProfileService.update(id, request));
    }
}
