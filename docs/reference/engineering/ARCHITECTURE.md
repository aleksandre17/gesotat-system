# Architecture and schema authority

## Normative model, not a claim of completed implementation

The following levels describe responsibility, not new directories or a mandate to replace existing contracts. Inventory existing compilers/registries and map their artifacts before changing them.

| Level | Authority | Produces / validates | Forbidden shortcut |
|---|---|---|---|
| M3 semantic constitution | Stable concepts: identity, grain, dimensions, measures, units, relations, classification, privacy, lifecycle | Vocabulary and invariant definitions | Equating a database column with a business concept |
| M2 contract grammar | Versioned meta-schemas, vocabulary capabilities, reference grammar | Validates M1 contracts; compiler resolves semantic references and creates an immutable plan | Treating syntactically valid JSON as an approved executable contract |
| M1 approved declarations | Site, dataset, provider mapping, relation, policy, projection, API and page contracts | Constrain M0 data and behavior; drive supported generation | Treating generated DTOs, Access files or physical DDL as authority |
| M0 instances | Source receipts, immutable snapshots, records, observations, responses and rendered views | Evidence of conformance to exact M1 revision | Inferring new semantics from observed values |

A meta-schema validates a schema. Generation requires a separately specified deterministic compiler; validation does not itself generate a platform. Each artifact MUST identify its kind, stable ID, grammar version, exact dependency revisions and semantic owner. Use existing canonical reference grammar and digest implementation. The dependency graph must resolve within an approved registry snapshot; reject missing, ambiguous or unsupported references. Distinguish prohibited dependency cycles from explicitly supported recursive type/relation structures with bounded traversal.

Schema-agnostic means the core executes supported declarative semantics independently of provider layouts. It does not mean schema-less, unrestricted SQL, one universal EAV store, or support for every future semantic primitive without an engine upgrade. A new primitive evolves M2 plus compiler, adapters, tests and migration; a new instance of supported semantics changes M1 only. Physical schemas may evolve through approved family-specific migrations.

## Complete execution and accountability line

```text
author -> grammar validation -> semantic resolution -> policy approval
       -> immutable contract revision / compiled plan
source -> receipt + fingerprint -> bounded adapter -> staging / quarantine
       -> typed canonical materialization -> relations + reconciliation
       -> quality / privacy / publication gate -> immutable snapshot
       -> authorized projection -> API / page / chart / export / cache
       -> lineage / archive / replay / restore / retirement
```

Every arrow needs a contract, owner, identity propagation, failure outcome and test. Carry tenant/site scope, contract revision, dataset/snapshot identity and correlation through the line. Raw artifact identity, resource locator and statistical observation identity are distinct. A hash proves byte integrity, not authorization or semantic truth. No publication before required evidence and approval; retry must not create a second logical publication.

| Layer | Owns | Boundary |
|---|---|---|
| Control | Grammar, registry, semantic compiler, approval state machine | No source parser or transport-specific business rule |
| Ingestion | Receipt, provider adapters, validation, quarantine, normalization | Cannot invent semantic definitions or publish directly |
| Data | Typed canonical storage, keys, grain, relations, snapshot consistency | Physical identifiers are internal and compiled, never caller SQL |
| Archive | Immutable artifacts, retention, provenance, replay | Locator is not identity; cleanup follows retention/backup authority |
| Serving | Authorized bounded queries, projections, API/page/export contracts | No recomputation of an alternative business meaning |
| Security | Identity, policy decisions, tenant isolation, privacy | Enforced at use-case/data boundaries, not UI hiding alone |
| Observability | Correlated outcomes, SLOs, audit evidence | No secrets/raw confidential payload in logs |
| Delivery | Migrations, builds, deployment, restore, rollback | No readiness from source-external edits |

## API and page completeness

An approved page/view declares stable identity, route, revision, localization, supported components, dataset/projection references, relation includes, filter/sort/aggregation semantics, pagination, policy and query budgets. API contracts specify request/response/error schemas, authorization, revision/snapshot behavior and compatibility. Renderer/transport adapters consume these declarations; they must not invent dimensions, units, classifiers or permissions. Preserve tables, charts, maps, downloads, filters, empty/error/loading states and accessibility where declared. Unsupported components fail authoring validation with an actionable error; do not silently omit them. Maintain before/after capability crosswalks, not only screenshot parity.

## Pattern selection and SOLID

| Problem | Preferred boundary | Evidence and limit |
|---|---|---|
| External provider/storage/transport variability | Ports and adapters owned by the consuming domain | Same contract suite across adapters; do not create an interface for every class |
| Multiple supported behaviors | Typed capability registry / strategy | Unknown capability rejected; no provider-name switch in generic engine |
| Declarative execution | Parse -> resolve -> validate -> immutable typed plan -> execute | Negative grammar, semantic and cost tests; no metadata eval/SQL |
| Approval/publication lifecycle | Explicit state machine and atomic transition | Illegal transition, concurrent approval, replay tests |
| Atomic write plus external notification | Transactional outbox when this dual-write exists | Crash/retry/deduplication evidence; no global exactly-once claim |
| Different read/write constraints | Projection/CQRS only with demonstrated need | Rebuild consistency and lag budget; do not mandate event sourcing |
| Legacy migration | Anti-corruption adapter and capability crosswalk | Side-by-side semantic reconciliation before retirement |

Single responsibility means one reason to change per cohesive module; open/closed means extending declared capabilities at intended boundaries; substitution requires identical invariants/failure semantics; interface segregation forbids unsupported-method stubs; dependency inversion keeps domain rules independent of frameworks and storage. Do not mandate microservices, distributed transactions, inheritance trees or speculative plugin frameworks. Document rejected alternatives and measurable constraints in an ADR for significant decisions.
