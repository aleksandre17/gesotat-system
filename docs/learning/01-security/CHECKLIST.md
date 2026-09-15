# Security checklist

- [x] Keycloak discovery ხელმისაწვდომია;
- [x] PKCE authorization-code flow შესრულდა;
- [x] callback port conflict უსაფრთხოდ მოგვარდა;
- [x] secret/token დოკუმენტში არ გაჟონა;
- [x] API-მ RS256/HMAC mismatch fail-closed-ად დაბლოკა;
- [ ] API verifier-ის issuer/JWKS RS256 activation;
- [ ] positive/negative security suite-ის სრული PASS;
- [ ] production OIDC/ABAC authority და signed evidence.
