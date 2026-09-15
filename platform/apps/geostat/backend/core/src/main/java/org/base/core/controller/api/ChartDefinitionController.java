package org.base.core.controller.api;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.base.core.anotation.Api;
import org.base.core.entity.data.ChartDefinition;
import org.base.core.model.request.ChartDefinitionRequest;
import org.base.core.service.ChartDefinitionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Api
@RestController
@RequestMapping("/chart-definitions")
@RequiredArgsConstructor
public class ChartDefinitionController {
    private final ChartDefinitionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<List<ChartDefinition>> findPublishedByProfile(@RequestParam Long profileId) {
        return ResponseEntity.ok(service.findPublishedByProfile(profileId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ChartDefinition> create(@Valid @RequestBody ChartDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public ResponseEntity<ChartDefinition> update(@PathVariable Long id, @Valid @RequestBody ChartDefinitionRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }
}
