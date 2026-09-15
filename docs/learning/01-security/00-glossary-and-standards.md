# Security glossary, standards და ტექნოლოგიები

## Identity და access-ის ძირითადი ცნებები

### Authentication

მარტივად: ვინ ხარ?  
პროფესიულად: principal-ის იდენტობის კრიპტოგრაფიული ან სხვა სანდო დადასტურება.  
ჩვენთან: Keycloak ამოწმებს KIDS runtime user-ს.

### Authorization

მარტივად: რისი გაკეთების უფლება გაქვს?  
ჩვენთან: API ამოწმებს role-ს, tenant-ს, site-ს, contract-სა და page-ს.

### Principal

იდენტიფიცირებული user, service ან workload, რომელსაც policy მიემართება.

### Claim

Token-ში ჩაწერილი მტკიცება principal-ზე, მაგალითად `sub`, `iss`, `aud`, `exp`, role ან tenant.

### Session

დროებითი კავშირი browser-სა და identity provider-ს შორის. PKCE flow-ში login session-ის cookie და ერთჯერადი code ერთმანეთს ეკუთვნის.

## OIDC, OAuth 2.0 და SAML

### OAuth 2.0

Authorization framework-ია: როგორ მიიღოს client-მა access token resource server-ისთვის. OAuth თვითონ identity-ის ფორმატი არ არის.

### OpenID Connect (OIDC)

OAuth 2.0-ზე აგებული authentication layer-ია. ამატებს issuer-ს, ID token-ს, discovery-სა და user claims-ს.

### SAML

ძველი, XML-ზე დაფუძნებული federation standard-ია, რომელიც ხშირად enterprise SSO-ში გვხვდება. ჩვენი API runtime-ისთვის OIDC უფრო მსუბუქი და თანამედროვე არჩევანია.

### Keycloak

Open-source Identity and Access Management server-ია. ის არ არის ჩვენი business database; ის არის authentication/policy authority.

## Tokens და კრიპტოგრაფია

### JWT

JSON Web Token — ხელმოწერილი claims-ის სტრუქტურა. JWT-ის წაკითხვა ხელმოწერის შემოწმებას არ ნიშნავს.

### JWS

JSON Web Signature — JWT-ის ხელმოწერის ფორმატი.

### JWKS

JSON Web Key Set — public keys-ის დოკუმენტი, რომლითაც API ამოწმებს JWT signature-ს.

### RS256

RSA + SHA-256 asymmetric signature. Keycloak ფლობს private key-ს, API იღებს public key-ს JWKS-იდან.

### HMAC

Symmetric signature: signer-ს და verifier-ს ერთი secret სჭირდებათ. OIDC provider-ის RS256 token-ისთვის HMAC verifier არასწორია.

### TLS/HTTPS

ტრანსპორტის encryption და server identity. HTTP tunnel ლოკალურ staging-ში შეიძლება იყოს დაშვებული, მაგრამ production public ingress-ს TLS სჭირდება.

## თანამედროვე security პრინციპები

- **Zero Trust** — არც internal network-ს ვენდობით ავტომატურად;
- **Identity-centric security** — access ეფუძნება verified identity-ს, არა მხოლოდ IP-ს;
- **Deny by default** — policy-ში გამოუცხადებელი ქცევა აკრძალულია;
- **Policy as code** — წესები versioned და testable ფორმით ინახება;
- **Secret separation** — პაროლი/token/config source code-ში არ ინახება;
- **Key rotation** — public/private key ცვლილება runtime cache refresh-ით უნდა იმუშაოს;
- **Tenant isolation** — ერთი tenant-ის მონაცემი მეორეში არ უნდა გადავიდეს;
- **Auditability** — authorization გადაწყვეტილება უნდა იყოს აღრიცხული.

## გამოყენებული საერთაშორისო ორიენტირები

- **OAuth 2.0 / RFC 6749** — authorization framework;
- **PKCE / RFC 7636** — authorization-code interception protection;
- **OAuth Security BCP / RFC 9700** — თანამედროვე OAuth security recommendations;
- **OpenID Connect Core** — identity layer;
- **JWT / RFC 7519** — token claims format;
- **JWS / RFC 7515** — JSON signatures;
- **JWK/JWKS / RFC 7517** — key representation;
- **NIST SP 800-63** — digital identity assurance;
- **OWASP ASVS** — application security verification;
- **OWASP API Security Top 10** — API threat catalogue;
- **CIS Controls** — operational security controls;
- **ISO/IEC 27001** — information security management;
- **SLSA/SBOM practices** — software supply-chain provenance.

სტანდარტის ჩამოთვლა ავტომატურად შესაბამისობას არ ნიშნავს. შესაბამისობა დასტურდება implementation, tests, evidence და review-ით.
