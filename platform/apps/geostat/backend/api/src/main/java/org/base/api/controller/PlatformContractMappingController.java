package org.base.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.base.api.service.platform.ContractMappingUpdateReceipt;
import org.base.api.service.platform.PlatformContractMappingService;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.base.api.security.tenancy.TenantScoped;

/** Review-only mutation endpoint; publishing remains a separate authority-gated action. */
@TenantScoped
@Api
@RestController
@RequestMapping("/platform/contracts")
public class PlatformContractMappingController {
    private final PlatformContractMappingService mappings;
    public PlatformContractMappingController(PlatformContractMappingService mappings) { this.mappings = mappings; }

    @PutMapping("/{contractId}/sources/{contractSourceId}/mapping")
    @PreAuthorize("hasAuthority('PUBLISH_RESOURCE')")
    public ResponseEntity<ContractMappingUpdateReceipt> replace(@PathVariable long contractId, @PathVariable long contractSourceId, @RequestBody JsonNode mapping) {
        return ResponseEntity.ok(mappings.replace(contractId, contractSourceId, mapping));
    }
}
