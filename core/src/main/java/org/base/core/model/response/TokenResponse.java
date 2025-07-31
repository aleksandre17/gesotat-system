package org.base.core.model.response;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TokenResponse {
    // Getters and Setters
    private String token;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long expiresIn;

    public TokenResponse(String token, String refreshToken, long expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

}

