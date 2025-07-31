package org.base.core.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.base.core.entity.Permission;
import org.base.core.entity.Token;
import org.base.core.entity.User;
import org.base.core.model.response.TokenResponse;
import org.base.core.repository.TokenRepository;
import org.base.core.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 *
 Key features of this implementation:
 1. **Token Storage**:
 - Stores both access and refresh tokens
 - Tracks token status (revoked/expired)
 - Maintains user association

 2. **Token Management**:
 - Automatically revokes old tokens when generating new ones
 - Scheduled cleanup of expired tokens
 - Supports token revocation on logout

 3. **Security Features**:
 - Token validation against database
 - User-token association
 - Token type differentiation

 4. **Database Maintenance**:
 - Automatic cleanup of expired tokens
 - Efficient queries for token validation
 */
@AllArgsConstructor
@NoArgsConstructor
@Component
public class JwtTokenUtil {

    private TokenRepository tokenRepository;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;


    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        //claims.put("roles", userDetails.getAuthorities());

        List<Map<String, Object>> roles = ((UserPrincipal)userDetails).getUser().getRoles().stream()
                .map(role -> Map.of(
                        "name", role.getName(),
                        "permissions", role.getPermissions().stream()
                                .map(Permission::getName)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());

        claims.put("roles", roles);
        return createToken(claims, userDetails.getUsername(), expiration);
    }


    public String refreshToken(UserDetails userDetails, String refreshToken) {
        try {

            if (!validateToken(refreshToken, userDetails)) {
                throw new IllegalArgumentException("Refresh token is expired or invalid");
            }
            // Generate new access token
            return generateToken(userDetails);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error refreshing token", e);
        }
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return createToken(new HashMap<>(), userDetails.getUsername(), refreshExpiration);
    }

    private String createToken(Map<String, Object> roles, String subject, Long expiration) {
        return Jwts
                .builder()
                .claim("roles", roles)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS512)
                .compact();
    }


    public TokenResponse generateTokens(UserDetails userDetails, User user) {

        // Generate new tokens
        String accessToken = generateToken(userDetails);
        String refreshToken = generateRefreshToken(userDetails);

        // Save both tokens
        saveToken(user, accessToken, Token.TokenType.ACCESS);
        saveToken(user, refreshToken, Token.TokenType.REFRESH);

        return new TokenResponse(
                accessToken,
                refreshToken,
                extractExpiration(accessToken).getTime()
        );
    }

    private void saveToken(User user, String tokenString, Token.TokenType tokenType) {
        // First, revoke any existing tokens of the same type for this user
        tokenRepository.updateAllTokensToRevokedByUserAndType(user.getId(), tokenType.name());

        // Create and save new token
        Token token = new Token();
        token.setToken(tokenString);
        token.setTokenType(tokenType.name());
        token.setUser(user);
        token.setRevoked(false);
        token.setExpired(false);
        token.setExpirationTime(extractExpiration(tokenString).toInstant());

        tokenRepository.save(token);
    }



    public boolean isTokenValid(String token) {
        try {
            if (tokenRepository.existsByToken(token)) {
                Token tokenEntity = tokenRepository.findByToken(token)
                        .orElseThrow();
                return !tokenEntity.isRevoked() && !tokenEntity.isExpired()
                        && !isTokenExpired(token);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void revokeAllUserTokens(User user) {
        List<Token> validUserTokens = tokenRepository.findAllValidTokensByUser(user.getId());
        if (validUserTokens.isEmpty()) {
            return;
        }

        validUserTokens.forEach(token -> {
            token.setRevoked(true);
            token.setExpired(true);
        });

        tokenRepository.saveAll(validUserTokens);
    }

    @Scheduled(cron = "0 0 */1 * * *") // Run every hour
    public void removeExpiredTokens() {
        tokenRepository.deleteAllExpiredTokens(Instant.now());
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

}