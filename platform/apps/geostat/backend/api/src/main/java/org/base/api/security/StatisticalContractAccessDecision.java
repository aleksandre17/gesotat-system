package org.base.api.security;

import org.base.api.security.tenancy.TenantAccessGuard;
import org.base.api.service.platform.statistical.workflow.ContractWorkflow;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Execution-time authorization of the statistical contract workflow: the tenant boundary of the product
 * (ADR-010) AND a function-level authority. Authoring maps to the platform's write authority and approval to
 * its publish-grade authority; both names are properties, so no new permission vocabulary is hard-coded.
 * Deny by default: no caller, another subject than the authenticated one, or a missing authority is a refusal.
 */
@Component
public class StatisticalContractAccessDecision implements ContractWorkflow.AccessDecision {
    private final TenantAccessGuard tenants;
    private final Map<ContractWorkflow.Authority, String> authorities;

    public StatisticalContractAccessDecision(TenantAccessGuard tenants,
                                             @Value("${platform.statistical-contract.authority.author:WRITE_RESOURCE}") String author,
                                             @Value("${platform.statistical-contract.authority.approve:PUBLISH_RESOURCE}") String approve,
                                             @Value("${platform.statistical-contract.authority.import:WRITE_RESOURCE}") String importer) {
        this.tenants = tenants;
        this.authorities = Map.of(ContractWorkflow.Authority.AUTHOR, author, ContractWorkflow.Authority.APPROVE, approve, ContractWorkflow.Authority.IMPORT, importer);
    }

    /** The configured authority behind a workflow role; used by method security on the registry surface. */
    public String authorityName(String role) { return authorities.get(ContractWorkflow.Authority.valueOf(role)); }

    @Override
    public boolean allowed(ContractWorkflow.Actor actor, String productCode, ContractWorkflow.Authority authority) {
        Authentication caller = SecurityContextHolder.getContext().getAuthentication();
        if (caller == null || !caller.isAuthenticated() || !caller.getName().equals(actor.subject())) return false;
        String required = authorities.get(authority);
        boolean granted = caller.getAuthorities().stream().map(GrantedAuthority::getAuthority).anyMatch(required::equals);
        return granted && tenants.permitsProductCode(productCode);
    }
}
