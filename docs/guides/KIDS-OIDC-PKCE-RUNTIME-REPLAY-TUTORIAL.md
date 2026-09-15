# KIDS OIDC/PKCE Runtime Replay — სასწავლო გზამკვლევი

ეს დოკუმენტი ხსნის, როგორ მოვამზადეთ KIDS-ის pageId=8 (Goals) runtime access-ის შემოწმება და რატომ არის ეს მნიშვნელოვანი contract-driven, metadata-driven და schema-agnostic პლატფორმისთვის.

დოკუმენტის მიზანია არა მხოლოდ ბრძანებების გამეორება, არამედ სრული მიზეზ-შედეგობრივი ხაზის გააზრება:

```text
KIDS site
  → OIDC configuration
  → Keycloak authentication
  → PKCE authorization-code flow
  → signed JWT access token
  → API JWT/JWKS validation
  → tenant/role authorization
  → contract introspection
  → page query
  → response/evidence
```

## 1. რა პრობლემას ვხსნით

საიტმა API-სთან მონაცემის მოთხოვნამდე უნდა დაამტკიცოს სამი რამ:

1. ვინ არის caller (authentication);
2. რისი გაკეთების უფლება აქვს (authorization);
3. რომელი contract/revision/page-ის მიხედვით უნდა შესრულდეს მოთხოვნა (contract resolution).

ჩვენი სისტემა არ უნდა დაეყრდნოს KIDS-ის hardcoded branching-ს. KIDS არის ერთი consumer/provider profile. იგივე runtime protocol უნდა იმუშაოს სხვა საიტისთვისაც, თუ მას საკუთარი contract და policy აქვს.

## 2. მონაწილე კომპონენტები

### 2.1 KIDS frontend

Frontend კითხულობს environment profile-ს:

```env
VITE_PLATFORM_API_URL=https://<approved-api-origin>/api/v1
VITE_SITE_CONTRACT_CODE=KIDS_PORTAL_V1
VITE_SITE_CONTRACT_REVISION=8
VITE_PLATFORM_AUTH_MODE=oidc
```

ეს კონფიგურაცია განსაზღვრავს მხოლოდ runtime context-ს. ის არ შეიცავს მონაცემის SQL-ს და არ წყვეტს რომელ ფიზიკურ ცხრილზე იმუშაოს.

### 2.2 Keycloak

Keycloak არის OIDC Identity Provider. ის:

- ამოწმებს მომხმარებლის იდენტობას;
- გასცემს authorization code-ს;
- code-ს ცვლის signed access token-ზე;
- token-ში ათავსებს subject, issuer, roles, tenant და სხვა claims-ს.

ამ გარემოში Keycloak გაშვებულია მხოლოდ `geostat` პროექტის იზოლირებულ infrastructure-ში.

### 2.3 API

API უნდა ამოწმებდეს JWT-ის ხელმოწერას Keycloak-ის JWKS public key-ით და შემდეგ ამოწმებდეს:

- issuer-ს;
- audience-ს;
- expiry-ს;
- signature algorithm-ს;
- role/tenant policy-ს;
- contract/page authorization-ს.

მნიშვნელოვანია: API-მ token არ უნდა ენდოს მხოლოდ იმიტომ, რომ ის სინტაქსურად JWT-ია.

### 2.4 SSH tunnel

Keycloak production ქსელში პირდაპირ საჯაროდ არ გამოგვიტანია. ტესტისთვის შეიქმნა ლოკალური forward tunnel:

```text
Windows localhost:8080
        │ SSH tunnel
        ▼
remote geostat-keycloak:8080
```

ამიტომ ბრაუზერი ხედავს `http://keycloak:8080`-ს, მაგრამ პორტი რეალურად remote container-მდე მიდის.

## 3. რატომ ვიყენებთ hostname-ს `keycloak`

Keycloak-ის discovery document აბრუნებს issuer-სა და JWKS URL-ს. თუ token-ში issuer არის `http://keycloak:8080/realms/geostat`, კლიენტმა და API-მ იგივე canonical issuer უნდა გამოიყენონ.

Windows hosts-ში დაემატა:

```text
127.0.0.1 keycloak
```

ეს მხოლოდ ლოკალური სახელის resolution-ია. ის არ ცვლის production DNS-ს და არ ეხება სხვა პროექტებს.

## 4. Keycloak-ის restart-loop და მისი მიზეზი

პირველ კონფიგურაციაში hostname ასე იყო:

```env
KEYCLOAK_HOSTNAME=keycloak:8080
```

ამ Keycloak ვერსიამ ეს მნიშვნელობა plain hostname-ად ჩათვალა და `:`-იანი მნიშვნელობა უარყო:

```text
Provided hostname is neither a plain hostname nor a valid URL
```

კანონიკური მნიშვნელობა გახდა:

```env
KEYCLOAK_HOSTNAME=http://keycloak:8080
```

ამის შემდეგ მხოლოდ `geostat-platform` compose stack-ში მოხდა Keycloak-ის recreate. სხვა პროექტების containers და volumes არ შეხებია.

შემოწმება:

```text
GET http://127.0.0.1:8080/realms/geostat/.well-known/openid-configuration
HTTP 200
```

## 5. OIDC discovery რას გვაძლევს

Discovery endpoint:

```text
GET /realms/geostat/.well-known/openid-configuration
```

ის აბრუნებს metadata-ს, მათ შორის:

```json
{
  "issuer": "http://keycloak:8080/realms/geostat",
  "authorization_endpoint": ".../protocol/openid-connect/auth",
  "token_endpoint": ".../protocol/openid-connect/token",
  "jwks_uri": ".../protocol/openid-connect/certs",
  "scopes_supported": ["openid", "offline_access", "tenant"],
  "code_challenge_methods_supported": ["plain", "S256"]
}
```

სარგებელი არის ის, რომ კოდი არ იმახსოვრებს endpoint-ების მთელ სიას. ის issuer-იდან დინამიკურად აღმოაჩენს საჭირო endpoint-ებს.

## 6. Test client და test user

შეიქმნა მხოლოდ staging/runtime verification-ისთვის განკუთვნილი client:

```text
realm:       geostat
client:      kids-runtime-test
flow:        authorization code
PKCE:        S256
redirect:    http://localhost:18766/callback
```

ასევე შეიქმნა test principal შესაბამისი role/tenant ატრიბუტებით. პაროლი ინახება მხოლოდ ლოკალურ/remote secret location-ში და ამ დოკუმენტში არ იწერება.

## 7. PKCE flow — ნაბიჯ-ნაბიჯ

### Step A — verifier

კლიენტი ქმნის შემთხვევით `code_verifier` მნიშვნელობას. ის ინახება მხოლოდ მიმდინარე login session-ისთვის.

### Step B — challenge

```text
code_challenge = BASE64URL(SHA256(code_verifier))
```

ბრაუზერში იგზავნება მხოლოდ challenge, არა verifier.

### Step C — authorization request

```http
GET /realms/geostat/protocol/openid-connect/auth
  ?client_id=kids-runtime-test
  &response_type=code
  &redirect_uri=http://localhost:18766/callback
  &code_challenge=<challenge>
  &code_challenge_method=S256
```

### Step D — user login

Keycloak ამოწმებს test user-ს. წარმატების შემდეგ აბრუნებს ერთჯერად authorization code-ს callback URL-ზე.

### Step E — token exchange

კლიენტი აგზავნის:

```http
POST /realms/geostat/protocol/openid-connect/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code
client_id=kids-runtime-test
code=<one-time-code>
redirect_uri=http://localhost:18766/callback
code_verifier=<original-verifier>
```

Keycloak ამოწმებს, რომ verifier-ის hash ემთხვევა ადრე გაგზავნილ challenge-ს. შემდეგ აბრუნებს signed access token-ს.

## 8. რატომ გვქონდა callback პორტის პრობლემა

`18765` პორტზე Windows HTTP.sys-ს ჰქონდა არსებული reservation (PID 4). ამიტომ listener ვერ გაეშვა:

```text
Failed to listen on prefix ... because it conflicts with an existing registration
```

არ გავაუქმეთ სისტემური reservation. ავირჩიეთ თავისუფალი `18766` პორტი და Keycloak client-ში დავამატეთ შესაბამისი redirect URI. ეს არის უსაფრთხო და არადესტრუქციული გადაწყვეტა.

## 9. რა გააკეთა PKCE script-მა

`ops/scripts/complete-kids-oidc-pkce.ps1`:

1. ამოწმებს verifier-ის სამუშაო ფაილს;
2. ქმნის ახალ verifier/challenge წყვილს;
3. ხსნის callback listener-ს;
4. ბეჭდავს authorization URL-ს;
5. ელოდება callback-ს;
6. code-ს ცვლის token-ზე;
7. წერს მხოლოდ access token-ს დროებით local secret file-ში;
8. აბრუნებს `TOKEN_READY` სტატუსს.

script-ში არ იბეჭდება პაროლი და არ ინახება token repository-ში.

## 10. რატომ ვერ გაიარა API replay-მ

OIDC login წარმატებით დასრულდა და RS256 token შეიქმნა. API replay-მ დააბრუნა:

```text
JWT algorithm RS256, configured verifier expects HMAC
```

ეს ნიშნავს:

```text
Keycloak: RS256 asymmetric signing
API:      HMAC symmetric verifier
```

ეს კარგი fail-closed ქცევაა: API-მ არ მიიღო ისეთი token, რომლის ვალიდაციის policy-ს არ ემთხვეოდა ხელმოწერის ალგორითმი.

## 11. სწორი საბოლოო გამოსავალი API-ში

API-ის security configuration უნდა გადავიდეს issuer/JWKS რეჟიმზე:

```text
issuer = http://keycloak:8080/realms/geostat
jwks   = issuer + /protocol/openid-connect/certs
alg    = RS256
aud    = geostat-api
```

ვალიდაციის თანმიმდევრობა:

```text
parse token
  → resolve issuer metadata
  → load/cache JWKS
  → select matching key id (kid)
  → verify RS256 signature
  → verify issuer
  → verify audience
  → verify expiry/not-before
  → map roles/tenant
  → enforce ABAC policy
```

HMAC secret-ის მოფიქრება ან RS256 token-ის ხელით შეცვლა დაუშვებელია.

## 12. Runtime replay-ის მიზანი

`ops/scripts/kids-page8-runtime-replay.ps1` ამოწმებს pageId=8-ს:

```text
1. contract introspection
2. page listing
3. query capabilities
4. page query
```

მოთხოვნის ლოგიკური ფორმა:

```json
{
  "contractCode": "KIDS_PORTAL_V1",
  "revision": 8,
  "pageId": 8,
  "projection": "default",
  "select": ["id", "title", "path", "category"],
  "sort": [{"field": "id", "direction": "ASC"}],
  "page": {"limit": 5}
}
```

ეს ამტკიცებს არა მხოლოდ authentication-ს, არამედ contract → page → projection → response-ის მთელ ხაზს.

## 13. მიღებული სარგებელი

- authentication და API authorization ერთმანეთისგან გამიჯნულია;
- token-ის ხელმოწერა asymmetric და rotation-ready ხდება;
- KIDS-specific SQL frontend-ში არ ჩნდება;
- contract revision request-ში explicit არის;
- callback/replay flow reproducible ხდება;
- შეცდომები fail-closed-ად ჩანს;
- staging verification production secrets-ის გამჟღავნების გარეშე ტარდება;
- იგივე protocol სხვა site/provider-ისთვისაც გამოიყენება.

## 14. რა არ უნდა გაკეთდეს

- არ გამოიყენო ძველი PKCE URL მეორედ;
- არ აურიო `localhost`, `127.0.0.1` და `keycloak` ერთ login session-ში;
- არ ჩაწერო access token ან password git-ში;
- არ გამორთო JWT validation `--skip-checks`-ით;
- არ შეცვალო RS256 token ხელით HMAC secret-ის მოსარგებად;
- არ გადააკეთო API მხოლოდ KIDS-ისთვის;
- არ შეეხო სხვა პროექტების compose stack-ს.

## 15. მიმდინარე მდგომარეობა

```text
Keycloak container:        HEALTHY
OIDC discovery:             PASS (HTTP 200)
PKCE authorization flow:   PASS
Access token issuance:      PASS
API RS256/JWKS validation:  OPEN — verifier ჯერ HMAC რეჟიმშია
Page 8 runtime replay:      BLOCKED by API verifier mismatch
```

შემდეგი ტექნიკური ნაბიჯი არის API-ის verifier-ის issuer/JWKS-ზე გადაყვანა და ამის შემდეგ იგივე replay-ის ხელახლა გაშვება.

## 16. შემეცნების საკონტროლო კითხვები

**რატომ არ არის საკმარისი მხოლოდ JWT-ის არსებობა?**  
რადგან JWT შეიძლება იყოს არასწორი issuer-ის, ვადაგასული, სხვა audience-ის ან ყალბი ხელმოწერით.

**რატომ არის PKCE საჭირო?**  
რადგან authorization code-ის მოპარვის შემთხვევაშიც attacker-ს verifier არ ექნება.

**რატომ არ ვხსნით Keycloak-ის public port-ს?**  
რადგან staging replay-ისთვის SSH tunnel ნაკლები ზედაპირია და production exposure-ს არ ზრდის.

**რატომ არის API-ის 401 სასარგებლო შედეგი?**  
რადგან fail-closed policy-მ შეუთავსებელი RS256/HMAC კონფიგურაცია დროულად გამოავლინა.

**სად არის contract-ის როლი?**  
Token მხოლოდ caller-ს ამტკიცებს. საბოლოო მონაცემის shape, fields, filters, relations და projection contract/revision-იდან მოდის.

**რა არის საბოლოო acceptance?**  
მხოლოდ მაშინ ჩაითვლება დასრულებულად, როცა OIDC/JWKS validation, tenant policy, introspection, query, response და evidence ყველა ერთად PASS იქნება.

