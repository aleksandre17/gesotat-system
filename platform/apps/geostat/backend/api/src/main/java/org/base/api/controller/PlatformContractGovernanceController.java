package org.base.api.controller;

import org.base.api.service.platform.ContractApprovalReceipt;
import org.base.api.service.platform.PlatformContractGovernanceService;
import org.base.core.anotation.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api
@RestController
@RequestMapping("/platform/contracts")
public class PlatformContractGovernanceController {
    private final PlatformContractGovernanceService governance;
    public PlatformContractGovernanceController(PlatformContractGovernanceService governance) { this.governance = governance; }
    @PostMapping("/{contractId}/approve")
    @PreAuthorize("hasAuthority('PUBLISH_RESOURCE')")
    public ResponseEntity<ContractApprovalReceipt> approve(@PathVariable long contractId) { return ResponseEntity.ok(governance.approve(contractId)); }
}
