package org.base.api.controller;

import org.base.api.service.platform.ContractIntrospectionService;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api
@RestController
@RequestMapping("/platform/contracts")
public class ContractIntrospectionController {
    private final ContractIntrospectionService service;

    public ContractIntrospectionController(ContractIntrospectionService service) { this.service = service; }

    @GetMapping("/{contractCode}/revisions/{revision}/introspection")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String, Object>> introspect(@PathVariable String contractCode, @PathVariable Integer revision) {
        return ResponseEntity.ok(service.contract(contractCode, revision));
    }

    @GetMapping("/{contractCode}/pages")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<?> pages(@PathVariable String contractCode, @RequestParam(required = false) Integer revision) {
        return ResponseEntity.ok(service.pages(contractCode, revision));
    }

    @GetMapping("/{contractCode}/pages/{pageId}/query-capabilities")
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String, Object>> capabilities(@PathVariable String contractCode, @PathVariable int pageId,
                                                              @RequestParam(required = false) Integer revision) {
        return ResponseEntity.ok(service.pageCapabilities(contractCode, pageId, revision));
    }
}
