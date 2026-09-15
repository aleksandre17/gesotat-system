# Self-signed TLS და `oidc-truststore.jks` — სრული სასწავლო თავი

ეს თავი ხსნის იმ TLS მექანიზმს, რომელიც geostat-ის staging OIDC flow-სთვის მოვამზადეთ. მიზანია მკითხველმა შეძლოს არა მხოლოდ ბრძანების გამეორება, არამედ გაიგოს:

```text
რა არის certificate
→ როგორ ამტკიცებს server identity-ს
→ რა განსხვავებაა public/private key-ს შორის
→ რატომ არ ენდობა JVM self-signed certificate-ს ავტომატურად
→ რა არის truststore
→ როგორ იყენებს მას API OIDC/JWKS discovery-ისას
→ რატომ არის ეს staging და არა საბოლოო production PKI
```

## 1. პრობლემა, რომელსაც TLS აგვარებს

როდესაც API უკავშირდება:

```text
https://keycloak:8443/realms/geostat/.well-known/openid-configuration
```

მან უნდა იცოდეს ორი რამ:

1. ნამდვილად იმ server-ს ვუკავშირდები, რომელსაც hostname ამბობს?
2. ქსელში პასუხი ხომ არ შეცვალა attacker-მა?

HTTPS პასუხობს ამ კითხვებს TLS handshake-ით. TLS ქმნის encrypted channel-ს და server-ის identity-ს certificate-ით ამოწმებს.

## 2. Certificate მარტივი ენით

TLS certificate არის server-ის ციფრული პირადობის მოწმობა. მასში წერია:

- hostname, მაგალითად `keycloak`;
- public key;
- მოქმედების ვადა;
- issuer (ვინ გასცა certificate);
- signature;
- Subject Alternative Name (SAN), რომლითაც რეალური hostname მოწმდება.

ჩვენი staging certificate-ში SAN-ებია:

```text
DNS:keycloak
DNS:localhost
IP:127.0.0.1
```

## 3. Public key და private key

TLS იყენებს asymmetric cryptography-ს:

```text
private key — საიდუმლო, მხოლოდ Keycloak-ზე
public key  — certificate-ში, სხვებისთვის წაკითხვადი
```

Private key-ით server ამტკიცებს, რომ certificate-ის მფლობელია. Public key-ით client ამოწმებს ამ მტკიცებას.

კრიტიკული წესი:

- private key არასდროს შედის git-ში;
- private key არასდროს იწერება დოკუმენტში;
- private key არ უნდა მოხვდეს log-ში, image-ში ან evidence bundle-ში.

## 4. CA-signed და self-signed certificate

### CA-signed certificate

სანდო Certificate Authority (CA) ამოწმებს domain ownership-ს და certificate-ს თავისი trusted root-ით აწერს ხელს. Browser-ებსა და operating system-ებს ასეთი root CA-ები წინასწარ აქვთ.

```text
Browser/OS trust store
  → trusted Root CA
  → server certificate
  → hostname + signature PASS
```

### Self-signed certificate

Self-signed certificate-ს თვითონ server-ის certificate აწერს ხელს. არ არსებობს მესამე, უკვე trusted CA:

```text
certificate signs itself
```

ამიტომ browser და JVM მას ავტომატურად არ ენდობა. ეს არ ნიშნავს, რომ cryptography სუსტია; ნიშნავს, რომ trust anchor წინასწარ არ არის განაწილებული.

## 5. რატომ გამოვიყენეთ self-signed

ჩვენ გვჭირდებოდა დროებითი HTTPS endpoint, რათა production policy-ის ეს მოთხოვნა რეალურად შეგვემოწმებინა:

```text
Production profile → HTTPS issuer required
```

Self-signed certificate გვაძლევს:

- TLS handshake-ის შემოწმებას;
- HTTPS Keycloak issuer-ს;
- JWKS discovery-ის დაშიფრულ არხს;
- OIDC integration-ის staging replay-ს;
- production code path-ის წინასწარ ტესტირებას.

მაგრამ ის **არ არის production trust authority**. Production-ში საჭიროა approved DNS და CA-signed certificate.

## 6. რა არის truststore

Truststore არის client-ის სანდო public certificates-ის საცავი. ის არ არის private key-ის საცავი და არ არის user password database.

ჩვენი ფაილი:

```text
oidc-truststore.jks
```

არის Java KeyStore/PKCS12 ფორმატის truststore, რომელშიც მოთავსებულია staging Keycloak-ის public certificate.

ლოგიკური შინაარსი:

```text
alias: geostat-oidc-staging
type: trustedCertEntry
certificate: Keycloak staging public certificate
```

## 7. რატომ სჭირდება ის API-ს

Spring Security-ის `NimbusJwtDecoder.withIssuerLocation(issuer)` აკეთებს შემდეგს:

```text
issuer URL
  → GET .well-known/openid-configuration
  → read jwks_uri
  → GET JWKS
  → cache public signing keys
  → validate RS256 JWT
```

თუ issuer არის HTTPS და certificate self-signed-ია, JVM იტყვის:

```text
certificate path not trusted
```

Truststore ეუბნება JVM-ს:

```text
ამ კონკრეტულ staging certificate-ს ენდე
```

## 8. JVM trust chain-ის მექანიზმი

API container-ში მითითებულია:

```text
-Djavax.net.ssl.trustStore=/run/secrets/oidc-truststore.jks
-Djavax.net.ssl.trustStorePassword=<secret>
-Djavax.net.ssl.trustStoreType=PKCS12
```

ამის შემდეგ JVM-ის TLS stack:

1. ხსნის truststore-ს;
2. ეძებს server certificate-ის issuer/ფაქტობრივ certificate-ს;
3. ამოწმებს certificate signature-ს;
4. ამოწმებს ვადას;
5. ამოწმებს hostname/SAN-ს;
6. მხოლოდ PASS-ის შემდეგ უშვებს HTTPS response-ს.

`trustStoreType` აუცილებელია, რადგან Java 17-ის `keytool`-მა ფაილი PKCS12 ფორმატში შექმნა, მიუხედავად `.jks` extension-ისა. Extension მარტო ფორმატს არ განსაზღვრავს.

## 9. როგორ შეიქმნა truststore

სერვერზე public certificate-ის import-ის კონცეფციაა:

```bash
keytool -importcert \
  -noprompt \
  -alias geostat-oidc-staging \
  -file tls/tls.crt \
  -keystore tls/oidc-truststore.jks \
  -storepass '<secret>'
```

ამ ოპერაციაში private key არ იმპორტირდება. truststore-ში შედის მხოლოდ public certificate.

შემოწმება:

```bash
keytool -list \
  -keystore tls/oidc-truststore.jks \
  -storetype PKCS12
```

მოსალოდნელი შედეგი არის ერთი `trustedCertEntry` alias-ით `geostat-oidc-staging`.

## 10. Docker-ში მიწოდება

API container-ს truststore მიეწოდება read-only bind mount-ით:

```text
host:
/home/administrator/geostat/backend/infra/geostat-platform/tls/oidc-truststore.jks

container:
/run/secrets/oidc-truststore.jks:ro
```

`ro` ნიშნავს read-only-ს. API ვერ შეცვლის truststore-ს runtime-ში.

ფაილის permission უნდა იყოს ისეთი, რომ container-ის `app` user-ს წაკითხვა შეეძლოს, მაგრამ private key-ის permissions ცალკე მკაცრი უნდა დარჩეს.

## 11. Keycloak-ის HTTPS configuration

Keycloak იღებს:

```text
tls/tls.crt — public certificate
tls/tls.key — private key
```

და უსმენს:

```text
HTTP  :8080  — temporary internal compatibility
HTTPS :8443  — staging OIDC issuer
```

Canonical staging issuer გახდა:

```text
https://keycloak:8443/realms/geostat
```

API-ს env-ში ეს მნიშვნელობა უნდა ემთხვეოდეს discovery document-ის `issuer`-ს ზუსტად, character-by-character.

## 12. ყველაზე ხშირი შეცდომები

### Wrong hostname

თუ URL-ში წერია `https://localhost:8443`, მაგრამ certificate-ში `localhost` SAN არ არის, hostname validation ჩავარდება.

### Wrong truststore type

თუ PKCS12 truststore-ს JKS-ად გავხსნით:

```text
Keystore was tampered with, or password was incorrect
```

ეს შეიძლება password-ის პრობლემა არც იყოს — format mismatch იყოს.

### Wrong file permission

თუ truststore root-only `0600`-ია და Java process `app` user-ით მუშაობს, JVM ვერ წაიკითხავს მას.

### Wrong issuer

Token-ის `iss`, discovery-ის `issuer` და API `OIDC_ISSUER_URL` ერთი authority უნდა იყოს.

### Missing SAN

თანამედროვე TLS hostname validation CN-ს მარტო აღარ ენდობა; SAN აუცილებელია.

## 13. Certificate rotation

Certificate მუდმივი არ არის. rotation-ის სწორი მიმდევრობაა:

```text
new certificate
  → import new public cert into truststore
  → verify old + new overlap if required
  → deploy truststore
  → restart/reload API
  → verify discovery/JWKS
  → remove old cert after safe window
```

ეს უნდა იყოს versioned და evidence-იანი ოპერაცია.

## 14. Self-signed staging-ის საზღვრები

Self-signed certificate შეიძლება გამოყენებულ იქნეს:

- local development;
- isolated staging;
- integration/contract tests;
- temporary PKI rehearsal.

ის არ უნდა ჩაითვალოს production completion-ად, რადგან:

- არ არსებობს external CA trust;
- domain ownership არ არის დადასტურებული;
- browser trust ყველა consumer-ზე არ არის განაწილებული;
- certificate rotation/incident authority არ არის production-grade.

## 15. Production გადასვლა

Production sequence:

```text
approved DNS
  → CA-issued certificate
  → approved ingress/Keycloak HTTPS
  → API JVM/OS trust chain
  → OIDC discovery HTTPS 200
  → JWKS reachable
  → valid RS256 token 200
  → negative tests 401/403
  → signed release evidence
```

Production-ში აღარ უნდა იყოს დამოკიდებულება თვითნაკეთ self-signed root-ზე.

## 16. სასწავლო საკონტროლო კითხვები

**Certificate password-ს ინახავს truststore?**  
არა. truststore password იცავს ფაილის გახსნას; მასში არსებული certificate public-ია.

**რატომ არ კმარა HTTPS-ის ჩართვა?**  
HTTPS encryption-ს ქმნის, მაგრამ client-ს უნდა შეეძლოს certificate chain-ის ნდობა.

**რატომ არ შეიძლება `trustAll`?**  
ეს hostname/signature verification-ს აუქმებს და TLS-ს ფორმალურ დეკორაციად აქცევს.

**რატომ არ ვდებ truststore-ს git-ში?**  
მართალია certificate public-ია, მაგრამ გარემოსთან მიბმული trust material-ის lifecycle და secret password repository-სგან განცალკევებული უნდა იყოს.

**რა განსხვავებაა truststore-სა და keystore-ს შორის?**  
Truststore ინახავს trusted public certificates-ს; keystore შეიძლება შეიცავდეს private key-სა და საკუთარი identity certificate-ს.

**რა ამტკიცებს, რომ TLS მზადაა?**  
არა მხოლოდ container-ის `Up` სტატუსი, არამედ HTTPS discovery, certificate validation, JWKS retrieval და API RS256 replay evidence.

## 17. მიმდინარე სტატუსი

```text
Self-signed certificate generated:      STAGING READY
Keycloak HTTPS listener :8443:          CONFIGURED
API truststore mount:                   CONFIGURED
API truststore type:                    PKCS12 CONFIGURED
Production CA/DNS authority:            OPEN
Final production certificate:           OPEN
```
