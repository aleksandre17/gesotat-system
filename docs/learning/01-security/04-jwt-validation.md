# JWT და JWKS validation

## სწორი ალგორითმი

Keycloak გასცემს asymmetric `RS256` token-ს. API-მ უნდა გამოიყენოს issuer-ის JWKS public key.

```text
JWT header kid
 → JWKS key lookup
 → RS256 signature verification
 → issuer check
 → audience check
 → expiry/not-before check
 → claims/policy check
```

## რეალური აღმოჩენილი პრობლემა

PKCE token წარმატებით გაიცა, მაგრამ API-მ დააბრუნა:

```text
RS256 token cannot be verified with configured HMAC key
```

ეს ნიშნავს, რომ API verifier ჯერ HMAC რეჟიმშია. ეს არ არის token-ის პრობლემა; ეს არის security configuration mismatch. სწორი რეაქციაა 401 და verifier-ის OIDC/JWKS-ზე გადართვა — არა token-ის დასუსტება.
