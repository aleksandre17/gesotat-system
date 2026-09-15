# GEOSOTAT — Production Blockers and Required Authority Register

**სტატუსი:** OPEN / NORMATIVE HANDOFF  
**თარიღი:** 2026-09-14  
**მიზანი:** ყველა იმ საკითხის ერთ კანონიკურ ფაილში თავმოყრა, რომლის დახურვა მხოლოდ ტექნიკური კოდის ცვლილებით არ შეიძლება.

## გამოყენების წესი

ეს ფაილი აერთიანებს audit-ში, capability-gap-ში, runtime evidence-ში და production-authority contract-ში გაფანტულ blocker-ებს. `OPEN` ნიშნავს, რომ ტექნიკური საფუძველი შეიძლება არსებობდეს, მაგრამ production closure-ისთვის საჭირო გარე authority ან დამოუკიდებელი evidence ჯერ არ არსებობს. მნიშვნელობების გამოცნობა აკრძალულია.

საბაზო authority contract: [`production-authority-input-contract.md`](production-authority-input-contract.md)  
ძირითადი audit: [`platform-capability-and-architecture-audit-2026-09-13.md`](platform-capability-and-architecture-audit-2026-09-13.md)  
ტექნიკური acceptance report: `build/technical-acceptance-report.json`

## ერთიანი blocker register

| ID | Blocker | რაც უკვე არსებობს | ახლა რატომ ვერ იხურება | აუცილებელი input/evidence | სტატუსი |
|---|---|---|---|---|---|
| B-01 | Release provenance | release-gate, image-revision check, evidence generator/verifier, reproducible build command | working tree dirtyა და approved release owner/tag არ არსებობს | signed immutable tag/commit; clean-checkout replay; OCI digest/SHA; deploy ledger; deploy authority | NOW NOT CLOSABLE |
| B-02 | OIDC/RBAC/ABAC/tenancy | secret-free Keycloak realm policy; fail-closed JWT guards; local regression tests; realm discovery is reachable from the API network | რეალური issuer/JWKS validation, audience და tenant policy API-ში არ არის მიბმული | HTTPS issuer/JWKS; client/audience; claim-to-role/tenant mapping; field/dataset boundary policy; expired/wrong-audience/revoked/cross-tenant tests | NOW NOT CLOSABLE |
| B-03 | Independent provider/site + cross-family execution | contract-only onboarding, H2 database replay and cross-family contract/projection acceptance (`ENTITY→REFERENCE`, `RAW→STATISTICAL`); immutable baseline bundle `build/geostat-staging-evidence-bundle-r42.json` verified | მეორე რეალური DB/provider, ყველა family-ის physical execution და production replay evidence არ არსებობს | provider endpoint/credentials reference; different schema/table names; composite key; relation/classifier data; persisted contract→physical query→relation graph→projection→response replay; ingest→publish→export→rollback report | NOW NOT CLOSABLE |
| B-04 | Distributed quota/HA | query-cost admission; Redis adapter; atomic Lua counter; rate-limit telemetry; isolated `geostat-redis` service is running | API production Redis binding, HA topology და quota owner policy დაუმტკიცებელია; API runtime flag is not enabled | TLS Redis endpoint; secret-manager reference; per-tenant budgets; failover topology; fairness/noisy-neighbor policy; overload/soak report | NOW NOT CLOSABLE |
| B-05 | Durable observability/SLO | OTel Collector; OTLP metrics wiring; redaction hooks; isolated collector is running | API OTLP binding, durable backend, SLO profile და alert routes არ აქვს დამტკიცებული; API runtime flag is not enabled; isolated infra compose replay currently fails on missing `GRAFANA_ADMIN_PASSWORD` | durable metrics/traces/log backend; endpoint; retention; SLO/error budget; alert destinations; delivery/firing evidence; Grafana secret reference | NOW NOT CLOSABLE |
| B-06 | Backup/restore/DR/publication | evidence bundle and checksum verification; publication gate framework | backup target, retention, timed restore და RPO/RTO არ არის რეალურად განსაზღვრული/გაზომილი | encrypted backup target; retention class; approved RPO/RTO; timed restore/replay; rollback/publication report | NOW NOT CLOSABLE |
| B-07 | Security/load/chaos acceptance | technical acceptance runner; API regression; synthetic checks | approved test window და signed production test authority არ არსებობს | UTC test window; scope; abort/rollback threshold; supply-chain scan; security/load/chaos/DR signed reports | NOW NOT CLOSABLE |

## საჭირო მნიშვნელობების ერთიანი ფორმა (secrets-ის გარეშე)

ეს არის მოთხოვნის ფორმა. აქ მხოლოდ URI, policy code, secret reference და approval metadata იწერება; secret value არასოდეს ჩაიწეროს Git-ში ან ამ ფაილში.

```yaml
authorityRevision: null
status: DRAFT # APPROVED → VERIFIED მხოლოდ evidence-ის შემდეგ
oidc:
  issuerUrl: null
  jwksUrl: null
  audience: null
  claimMapping: null
tenancy:
  policyCode: null
  boundary: null
quota:
  provider: REDIS # ან SQL
  endpoint: null
  secretRef: null
  haMode: null
observability:
  otelEndpoint: null
  durableBackend: null
  sloProfile: null
  alertDestinations: []
recovery:
  backupTarget: null
  retention: null
  rpo: null
  rto: null
release:
  reference: null
  imageDigest: null
  deployAuthority: null
testing:
  windowUtc: null
  abortThreshold: null
  approver: null
```

## დახურვის კონტროლი

### Cross-family/provider production closure evidence

Local contract-only acceptance is complete, but production closure of B-03
requires a real independent provider and immutable evidence for the complete
chain: approved persisted contract → physical reads → cross-family relation
graph → declared projection/include → response serialization. The evidence
bundle must cover every enabled family/provider combination, relation
cardinality, projection allow-list/redaction, row/checksum reconciliation,
publication and rollback. A local H2 or synthetic fixture is implementation
evidence only and cannot promote B-03 to `VERIFIED`.

## Linux/Docker-ის როლი და საზღვარი

Linux და Docker ნამდვილად გვეხმარება — მათი საშუალებით შეგვიძლია:

- მეორე დამოუკიდებელი provider-ის კონტეინერით გაშვება;
- MySQL/SQL Server/non-default schema portability matrix;
- Redis HA/failover test;
- OTel durable backend-ის დამატება;
- backup/restore და replay rehearsal;
- load/chaos/security test harness;
- reproducible image build და digest verification.

ანუ ბევრი დარჩენილი ტექნიკური implementation და staging acceptance შეგვიძლია Docker-ით დავხუროთ.

მაგრამ Docker თავისით ვერ ქმნის:

- approved release authority-ს;
- ნამდვილ OIDC ბიზნეს-პოლიტიკას;
- დამტკიცებულ tenant boundary-ს;
- ოფიციალურ RPO/RTO-ს;
- ხელმოწერილ production test approval-ს;
- production deploy-ის ნებართვას.

ამიტომ Linux/Docker-ით დასახური ტექნიკური ნაწილი უნდა შესრულდეს იზოლირებულ `geostat-net` გარემოში, ხოლო authority-dependent production gate-ები ცალკე დარჩეს `VERIFIED` ჩანაწერამდე.

1. სანამ რომელიმე სავალდებულო input არის `null`, შესაბამისი blocker რჩება `NOW NOT CLOSABLE`.
2. `APPROVED` ჩანაწერი ჯერ კიდევ არ არის საკმარისი — evidence-ის მიმაგრების შემდეგ უნდა გახდეს `VERIFIED`.
3. production runtime activation აკრძალულია `VERIFIED` authority record-ის გარეშე.
4. ყველა evidence უნდა იყოს immutable, checksum-ით დაცული, timestamped და release reference-ზე მიბმული.
5. ამ ფაილში blocker-ის დახურვა არ ნიშნავს თვითნებურ production approval-ს; დახურვა ხდება მხოლოდ authority owner-ის დამტკიცებისა და განმეორებადი ტესტის შემდეგ.

## ამ ეტაპზე საბოლოო ვერდიქტი

ტექნიკური implementation baseline და local/synthetic acceptance მზად არის. Production closure-ისთვის დარჩენილი blocker-ები არის **B-01–B-07**. მათი მნიშვნელობების გამოგონება ან უბრალოდ „დასრულებულად“ მონიშვნა დაუშვებელია.
