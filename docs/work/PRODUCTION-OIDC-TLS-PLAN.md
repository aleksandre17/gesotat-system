# Production OIDC TLS preparation

## Current fact

Keycloak is healthy on internal HTTP `:8080`, and RS256 token issuance works. Production API policy correctly rejects an HTTP issuer.

## Required target

```text
https://<approved-oidc-host>/realms/geostat
```

The hostname and certificate must be approved for the geostat environment. A self-signed certificate or an invented hostname is suitable only for local/staging tests, not production authority.

## Implementation sequence

1. obtain DNS name and CA-issued certificate with SAN;
2. terminate TLS at approved ingress or configure Keycloak HTTPS `8443`;
3. publish discovery and JWKS through the same HTTPS issuer;
4. verify certificate chain from browser and API container;
5. set `KEYCLOAK_HOSTNAME=https://<approved-oidc-host>`;
6. set API `OIDC_ISSUER_URL=https://<approved-oidc-host>/realms/geostat`;
7. keep `OIDC_AUDIENCE=geostat-api`;
8. recreate only geostat Keycloak/API services;
9. run positive and negative JWT/tenant tests;
10. capture signed evidence before production activation.

## Configuration contract

```env
PLATFORM_OIDC_ENABLED=true
OIDC_ISSUER_URL=https://<approved-oidc-host>/realms/geostat
OIDC_AUDIENCE=geostat-api
```

## Acceptance gates

- [ ] DNS resolves to approved ingress;
- [ ] TLS hostname/SAN and certificate chain pass;
- [ ] discovery returns HTTP 200 over HTTPS;
- [ ] JWKS is reachable from `geostat-api`;
- [ ] RS256 valid token returns 200;
- [ ] wrong issuer/audience/expiry/signature returns 401;
- [ ] cross-tenant request is denied;
- [ ] pageId=8 introspection/query replay passes;
- [ ] rollback and evidence bundle are recorded.

## Non-negotiable boundary

Do not bypass the HTTPS gate by changing production validation to accept HTTP. Do not place a private key in git, compose logs, or application artifacts.
