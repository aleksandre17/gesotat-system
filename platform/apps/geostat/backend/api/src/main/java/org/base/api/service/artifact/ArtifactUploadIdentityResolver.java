package org.base.api.service.artifact;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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
        return new Identity(hash("tenant\u0000" + tenant), hash("subject\u0000" + subject));
    }

    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte b : digest) result.append(String.format("%02x", b));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }

    public record Identity(String tenantKeyHash, String ownerKeyHash) {}
}
