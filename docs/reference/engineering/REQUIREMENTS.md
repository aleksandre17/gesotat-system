# Requirement catalogue v1

Each row is normative. Evidence is scoped to the affected behavior, not a requirement to run every test type on every typo. Record applicability for every ID in a change record. Reviewers assess substantive adequacy; the structural gate cannot do that.

| ID | Owner | Obligation | Required acceptance evidence |
|---|---|---|---|
| GOV-001 | Control | MUST read canonical authority and inspect current source/status before editing; MUST inventory reusable implementations before adding one. | Search paths, existing owner, reuse/extension/replacement decision and baseline revision |
| GOV-002 | Control | MUST map producer, compiler, persistence, consumers and operations affected by a change, including unchanged boundaries with reasons. | End-to-end impact and capability crosswalk; linked existing AIR/ADR when relevant |
| GOV-003 | Delivery | MUST preserve unrelated work and maintain a durable bounded change record and handoff. | Exact changed paths, commands/results, unresolved findings and next action |
| SCH-001 | Control | MUST preserve M3 -> M2 -> M1 -> M0 authority, versioned grammar and exact reference resolution; reject unsupported required semantics. | Positive and negative compiler/grammar/reference tests, including incompatible revision |
| SCH-002 | Control | MUST define each concept once, with stable identity, grain, keys, units, dimensions, language/classifier links and null/missing semantics where applicable. | Semantic catalogue/table passport and round-trip/reconciliation tests |
| SCH-003 | Control | MUST prove supported new site/provider/family declarations run without site-name branching in generic core. New primitives require explicit grammar evolution. | Two materially different fixtures plus adapter substitution; no claim of universal capability |
| ARC-001 | Control | MUST keep cohesive layer ownership and inward domain dependency; abstractions MUST solve observed variation. | Dependency review, bounded interfaces and substitutability tests |
| ING-001 | Ingestion | MUST bound parsing/upload/fetch resources; validate receipts and source fingerprints; quarantine invalid data without partial publication. | Malformed/oversized input, interrupted ingest and retry tests; row lineage |
| DAT-001 | Data | MUST preserve typed family semantics, tenant scope, identity, relation integrity and immutable snapshot consistency. | Constraint, cross-tenant, concurrent-write and reconciliation tests |
| DAT-002 | Data | MUST make retry/transaction/timeout behavior explicit; idempotency keys include semantic scope and conflicting payloads fail. | Duplicate, conflict, concurrency, rollback and crash-recovery cases |
| API-001 | Serving | MUST use approved typed request/response/projection contracts and bounded query plans; no caller SQL/identifiers. | Contract, negative query-budget, injection, pagination/snapshot and compatibility tests |
| UI-001 | Serving | MUST preserve declared pages/views/charts/maps/exports/locales and policy-aware capability discovery. | Before/after capability crosswalk and relevant API/render tests; explicit unsupported handling |
| SEC-001 | Security | MUST enforce authentication, operation authorization and tenant/object scope at execution boundaries; fail closed. | Behavioral HTTP/use-case tests for anonymous, wrong role, wrong tenant, revoked and allowed access |
| SEC-002 | Security | MUST protect credentials, confidential/raw data, upload/fetch destinations and logs; secret injection belongs to deployment. | Negative privacy, path/SSRF/injection tests and secret scan relevant to the change |
| EVO-001 | Delivery | MUST define compatibility for producers, stored revisions and consumers; migrations are ordered and replayable. | Upgrade from previous supported state, clean rebuild, incompatibility rejection and rollback/forward recovery |
| OBS-001 | Observability | MUST expose actionable correlated outcomes and bounded telemetry with privacy-safe audit attribution. | Failure-path log/metric/trace evidence and measurable latency/resource/error objectives |
| ARC-002 | Archive | MUST preserve artifact provenance, retention authority and reproducible replay/restore. | Content identity, source-to-output lineage, backup/restore and authorized deletion evidence |
| TST-001 | Delivery | MUST NOT weaken, skip, delete or replace behavioral tests with static assertions to obtain PASS. | Failing-before/passing-after regression where applicable; changed tests justified against invariant |
| TST-002 | Delivery | MUST distinguish static, unit, integration, security, property, performance and recovery evidence; missing execution is NOT_RUN. | Exact commands, exit codes, environment, relevant inputs and evidence files tied to source |
| REL-001 | Delivery | MUST retain existing release invariants: clean tagged source, immutable image, migrations, compatibility, security/privacy, SLO, recovery, SBOM/scan/provenance and deployed docs. | Existing release gate and current evidence bundle; missing evidence means NOT READY |
| GOV-004 | Control | SHOULD choose the smallest complete solution and explain significant alternatives; MUST record exceptions and unresolved conflicts explicitly. | ADR for architectural decisions; owner, scope, expiry and compensating control for exceptions |

No row alone establishes certification against an external standard. The standard selection and local interpretation are in `STANDARDS.md`.
