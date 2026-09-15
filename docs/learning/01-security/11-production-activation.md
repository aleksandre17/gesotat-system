# Production OIDC activation contract

ეს ფაილი არის production-ისთვის დასაცავი canonical prerequisite record.

## დადასტურებული მდგომარეობა

```text
Keycloak realm/client       READY
PKCE authorization flow     PASS
RS256 access token          PASS
API OIDC/JWKS implementation READY
```

მიღებული token-ის მაგალითური შედეგი (secret-ის გარეშე):

```json
{
  "algorithm": "RS256",
  "issuer": "<geostat-oidc-issuer>",
  "audience": "geostat-api",
  "status": "ISSUED"
}
```

## აღმოჩენილი production blocker

ამჟამინდელი Keycloak endpoint არის HTTP:

```text
http://keycloak:8080/realms/geostat
```

Production security policy მოითხოვს HTTPS-ს. API-მ ამიტომ სწორად დაბლოკა activation:

```text
Production OIDC issuer must use HTTPS
```

ეს fail-closed ქცევაა და არ უნდა გაითიშოს.

## Production activation sequence

```text
TLS certificate/termination
  → HTTPS Keycloak issuer
  → discovery + JWKS reachable
  → issuer/audience/expiry/RS256 validation
  → roles + tenant ABAC
  → PLATFORM_OIDC_ENABLED=true
  → API recreate with approved image
  → introspection request
  → page query
  → signed evidence
```

## აუცილებელი production configuration

```env
PLATFORM_OIDC_ENABLED=true
OIDC_ISSUER_URL=https://<approved-oidc-host>/realms/geostat
OIDC_AUDIENCE=geostat-api
OIDC_ROLES_CLAIM=realm_access.roles
OIDC_TENANT_CLAIM=tenant_id
```

`<approved-oidc-host>` უნდა იყოს რეალური DNS/TLS authority. `keycloak:8080` და self-signed დაუმტკიცებელი HTTP endpoint production authority-ად არ ჩაითვლება.

## Acceptance gate

- [ ] certificate chain და hostname verification PASS;
- [ ] OIDC discovery HTTPS-ით აბრუნებს HTTP 200-ს;
- [ ] JWKS endpoint reachable არის API container-იდან;
- [ ] valid RS256 token → 200;
- [ ] wrong issuer/audience/signature/expiry → 401;
- [ ] wrong tenant/role → 403/deny;
- [ ] pageId=8 introspection/query → 200;
- [ ] response, checksum და authorization evidence signed bundle-შია;
- [ ] approved deploy authority და rollback plan დადასტურებულია.

## Local/staging distinction

HTTP issuer შეიძლება გამოყენებულ იქნეს მხოლოდ local/staging replay-სთვის, სადაც production profile არ არის ჩართული. Production profile-ში HTTPS validation სავალდებულოა და მისი გამორთვა დაუშვებელია.

## არ შეიძლება

- RS256 token-ის HMAC secret-ით შემოწმება;
- `PLATFORM_OIDC_ENABLED`-ის ჩართვა HTTP production issuer-ზე;
- JWT validation-ის გამორთვა `skip-checks`-ით;
- token/password-ის commit-ში ან evidence-ში ჩაწერა;
- TLS gate-ის გვერდის ავლა მხოლოდ იმისთვის, რომ request-მა 200 დააბრუნოს.
