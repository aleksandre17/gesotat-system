package org.base.core.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Provider-neutral audience policy for the opt-in OIDC resource server. */
public final class OidcAudienceValidator implements OAuth2TokenValidator<Jwt> {
    private final String requiredAudience;

    public OidcAudienceValidator(String requiredAudience) {
        if (requiredAudience == null || requiredAudience.isBlank()) {
            throw new IllegalArgumentException("required OIDC audience must not be blank");
        }
        this.requiredAudience = requiredAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        return token != null && token.getAudience() != null && token.getAudience().contains(requiredAudience)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required audience is missing", null));
    }
}
