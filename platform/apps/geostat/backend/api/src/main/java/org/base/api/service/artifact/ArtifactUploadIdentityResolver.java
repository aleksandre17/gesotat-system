package org.base.api.service.artifact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;


/** Resolves an authenticated OIDC subject and required tenant claim, storing only pseudonymous keys. */
@Component
public class ArtifactUploadIdentityResolver {
    private final String tenantClaim;

    public ArtifactUploadIdentityResolver(@Value("${platform.oidc.tenant-claim:tenant_id}") String tenantClaim) {
        if (tenantClaim == null || tenantClaim.isBlank()) throw new IllegalArgumentException("Tenant claim name is required");
        this.tenantClaim = tenantClaim;
    }

    public Identity resolve(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt))
            throw new ArtifactAccessDeniedException("Resumable upload requires an authenticated tenant-scoped identity");
        String tenant = jwt.getToken().getClaimAsString(tenantClaim);
        String subject = jwt.getToken().getSubject();
        if (tenant == null || tenant.isBlank() || subject == null || subject.isBlank())
            throw new ArtifactAccessDeniedException("Resumable upload requires tenant and subject claims");
        return new Identity(Sha256.ofUtf8("tenant\u0000" + tenant), Sha256.ofUtf8("subject\u0000" + subject));
    }

    public record Identity(String tenantKeyHash, String ownerKeyHash) {}
}
