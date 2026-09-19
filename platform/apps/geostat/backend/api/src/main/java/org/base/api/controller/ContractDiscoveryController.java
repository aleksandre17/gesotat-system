package org.base.api.controller;

import org.base.api.service.platform.*;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import org.base.api.security.tenancy.TenantScoped;

@TenantScoped
@Api @RestController @RequestMapping("/platform/contracts")
public class ContractDiscoveryController {
    private final ContractOpenApiService openApi; private final ContractCompatibilityService compatibility; private final ContractClientGeneratorService clients;
    public ContractDiscoveryController(ContractOpenApiService openApi,ContractCompatibilityService compatibility,ContractClientGeneratorService clients){this.openApi=openApi;this.compatibility=compatibility;this.clients=clients;}
    @GetMapping("/{contractCode}/revisions/{revision}/openapi") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String,Object>> openApi(@PathVariable String contractCode,@PathVariable int revision){return ResponseEntity.ok(openApi.document(contractCode,revision));}
    @GetMapping("/{contractCode}/compatibility") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String,Object>> compatibility(@PathVariable String contractCode,@RequestParam int fromRevision,@RequestParam int toRevision){return ResponseEntity.ok(compatibility.compare(contractCode,fromRevision,toRevision));}
    @GetMapping("/{contractCode}/revisions/{revision}/json-schema") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<Map<String,Object>> jsonSchema(@PathVariable String contractCode,@PathVariable int revision){return ResponseEntity.ok(clients.jsonSchema(contractCode,revision));}
    @GetMapping(value="/{contractCode}/revisions/{revision}/typescript",produces="text/plain") @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public ResponseEntity<String> typescript(@PathVariable String contractCode,@PathVariable int revision){return ResponseEntity.ok(clients.typescript(contractCode,revision));}
}
