package org.base.core.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Fail-closed guard for claims required by the authorization policy. */
public final class OidcRequiredClaimValidator implements OAuth2TokenValidator<Jwt> {
    private final String claimName;

    public OidcRequiredClaimValidator(String claimName) {
        if (claimName == null || claimName.isBlank()) {
            throw new IllegalArgumentException("required OIDC claim name must not be blank");
        }
        this.claimName = claimName;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String value = token == null ? null : token.getClaimAsString(claimName);
        return value != null && !value.isBlank()
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required claim is missing: " + claimName, null));
    }
}
