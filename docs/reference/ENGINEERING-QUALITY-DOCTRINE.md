# Engineering Quality Doctrine

- Production-grade — შედეგი უნდა იყოს რეალურად გასაშვები, არა მხოლოდ დემო ან დოკუმენტური.
- Contract-first — კონტრაქტი/metadata არის source of truth; კოდი მას მიჰყვება.
- Metadata-driven — ქცევა განისაზღვრება გამოცხადებული metadata-ით და არა hardcoded KIDS branching-ით.
- Schema/provider/site-agnostic — ახალი provider, site, schema ან data family უნდა დაემატოს core-code-ის გადაკეთების გარეშე.
- Canonical architecture — თითოეულ არტეფაქტს აქვს ერთი owner, ერთი ფიზიკური ადგილი და ერთი authority.
- Layered architecture — Control, Ingestion, Data, Archive, Serving, Security, Observability და Delivery პასუხისმგებლობები გამიჯნულია.
- SOLID და Ports & Adapters — domain/core არ არის მიბმული კონკრეტულ DB-ზე, framework-ზე ან provider-ზე.
- Explicit boundaries — project/service/environment namespace-ები არ ირევა.
- Versioned და immutable — კონტრაქტები, mappings, policies, checksums, snapshots და evidence revision-ებით იმართება.
- Idempotent და fail-closed — განმეორებითი ოპერაცია არ აორმაგებს შედეგს; გაურკვევლობისას სისტემა აჩერებს მოქმედებას.
- Security-by-default — deny-by-default, tenant isolation, authorization, privacy და secret separation.
- Observable და auditable — ყველა მნიშვნელოვანი მოქმედება უნდა ტოვებდეს trace, metric, ledger და evidence-ს.
- Testable — unit, integration, negative, property, security, performance, recovery და conformance tests.
- Backward-compatible — ძველი კონტრაქტის/consumer-ის გავლენა ფასდება migration/deprecation წესით.
- Documentation-as-code — დოკუმენტაცია runtime/migration/contract მდგომარეობას ავტომატურად უნდა ემთხვეოდეს.
- Fail-closed release governance — implementation-ის არსებობა საკმარისი არ არის; საჭიროა test, evidence, approval და reproducible release.

ეს doctrine სავალდებულოა ყველა კოდის, კონტრაქტის, migration-ის, API-ის,
დოკუმენტაციის, ინფრასტრუქტურისა და directory-structure ცვლილებისთვის.

## General engineering standards

- **SOLID** — Single Responsibility, Open/Closed, Liskov Substitution,
  Interface Segregation და Dependency Inversion.
- **Clean/Hexagonal Architecture** — domain core ცენტრში; adapters და
  infrastructure გარეთ; dependency direction ყოველთვის inward.
- **DDD და bounded contexts** — domain language, aggregate boundaries,
  entities, value objects, domain services და explicit anti-corruption layers.
- **Design patterns** — მხოლოდ საჭიროებისას: Strategy, Adapter, Factory,
  Ports & Adapters, Repository, Specification, State Machine, Command,
  Pipeline, Saga/Outbox და Circuit Breaker.
- **GRASP და cohesion/coupling discipline** — მაღალი cohesion, დაბალი coupling,
  მკაფიო ownership და ცვლილების მინიმალური blast radius.
- **API standards** — versioned contract, OpenAPI/JSON Schema, RFC 9457
  errors, idempotency keys, pagination, caching, content negotiation და
  backward-compatible evolution.
- **Data standards** — explicit identity, keys, constraints, normalization,
  lineage, provenance, temporal semantics, quality rules და migration discipline.
- **Security standards** — least privilege, deny-by-default, OWASP ASVS/API
  principles, secret separation, threat modeling, auditability და privacy by design.
- **Reliability standards** — timeout, retry budget, idempotency, circuit
  breaker, bulkhead, graceful degradation, rollback და disaster recovery.
- **Testing standards** — test pyramid, unit/integration/contract/property/
  negative/security/performance/chaos/recovery/conformance coverage.
- **Observability standards** — structured logs, metrics, traces, correlation
  IDs, OpenTelemetry, redaction, SLO/error budget და actionable alerts.
- **Delivery standards** — 12-factor configuration, immutable artifacts,
  reproducible builds, SBOM/provenance, CI gates, progressive rollout და
  evidence-based release.
- **Documentation standards** — Diátaxis, ADR/MADR, docs-as-code, canonical
  ownership, versioning და zero-drift validation.

ეს სტანდარტები გამოიყენება კონტექსტის შესაბამისად; არცერთი pattern ან
abstraction არ ემატება მხოლოდ ფორმალური შესაბამისობისთვის. არჩევანი უნდა იყოს
პრობლემის ბუნებით გამართლებული და acceptance evidence-ით დადასტურებული.
