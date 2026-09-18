package org.base.api.service.artifact;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArtifactUploadIdentityResolverTest {
    private final ArtifactUploadIdentityResolver resolver = new ArtifactUploadIdentityResolver("tenant_id");

    @Test
    void derivesStablePseudonymousTenantAndOwnerKeysFromOidcClaims() {
        var first = resolver.resolve(jwt("tenant-a", "user-17"));
        var replay = resolver.resolve(jwt("tenant-a", "user-17"));
        var otherTenant = resolver.resolve(jwt("tenant-b", "user-17"));

        assertEquals(first, replay);
        assertNotEquals(first.tenantKeyHash(), otherTenant.tenantKeyHash());
        assertEquals(64, first.tenantKeyHash().length());
        assertEquals(64, first.ownerKeyHash().length());
        assertNotEquals("tenant-a", first.tenantKeyHash());
    }

    @Test
    void rejectsLegacyAuthenticationAndMissingTenantClaims() {
        assertThrows(ArtifactAccessDeniedException.class,
                () -> resolver.resolve(new UsernamePasswordAuthenticationToken("user-17", "n/a")));
        assertThrows(ArtifactAccessDeniedException.class,
                () -> resolver.resolve(jwt(null, "user-17")));
        assertThrows(ArtifactAccessDeniedException.class,
                () -> resolver.resolve(jwt("tenant-a", null)));
    }

    private static JwtAuthenticationToken jwt(String tenant, String subject) {
        var claims = new java.util.HashMap<String, Object>();
        claims.put("sub", subject);
        if (tenant != null) claims.put("tenant_id", tenant);
        Jwt token = Jwt.withTokenValue("test-only")
                .header("alg", "none")
                .issuedAt(Instant.EPOCH)
                .expiresAt(Instant.parse("2099-01-01T00:00:00Z"))
                .claims(values -> values.putAll(claims))
                .build();
        return new JwtAuthenticationToken(token);
    }
}
