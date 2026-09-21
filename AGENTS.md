<!-- PROJECT-CONTROL:BEGIN -->
## Session protocol — read this first

This repository runs the **Repository Control Protocol**
(`docs/standards/PROJECT-OPERATING-SYSTEM.md`). Project state lives in files, never in
conversation history.

**Start of any substantial work:**

1. Read `docs/project/CURRENT.md` — phase, gate, next permitted action, forbidden actions,
   required read set. **It is the single current-state authority.**
2. Resolve the active work item, if any, and load **only** the artifacts in its
   `required_context`.
3. Before creating any mechanism — code, schema, registry, contract, document,
   configuration, migration declaration — identify who already owns that responsibility in
   the Authority Registry
   (`docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md`, CAD-02).

**Do not read the documentation tree by default.** The required-read set is short and
sufficient. Evidence artifacts are read to falsify a specific conclusion, not routinely.

**End of any substantial work:**

Verify acceptance criteria → record outputs and unresolved issues → update affected
canonical knowledge and registers → update work status → update `CURRENT.md` if state
changed → update `docs/project/MANIFEST.md` if a gate changed → update
`docs/project/CATALOG.md` for new or superseded artifacts → **run
`python ops/cli/validation/rcp-verify.py` and require exit `0`** → **state the next
permitted action**.

> A session is not closed if the next agent must reconstruct what happened from `git diff`
> or chat history.

`rcp-verify.py` is the deterministic half of this protocol (`OBL-RCP-VERIFY`). It decides
metadata validity, ID uniqueness, supersession integrity, `CURRENT` ↔ `MANIFEST`
consistency, catalogue completeness, work-item state agreement, and whether any
currency-asserting filename lacks an assigned authority. **A control-plane claim that the
validator has not confirmed is an assertion, not a verification.**

**DO NOT CREATE A NEW PARALLEL ARCHITECTURAL MECHANISM.** If the canonical mechanism
cannot support what is required, **stop and report the architectural conflict** — do not
build beside it (CAD-16).

Navigation: `docs/project/README.md`. Generic protocol:
`docs/standards/PROJECT-OPERATING-SYSTEM.md`.
<!-- PROJECT-CONTROL:END -->

<!-- ENGINEERING-GOVERNANCE:BEGIN -->
## Mandatory platform engineering protocol

Before any work, read `docs/reference/CANONICAL-FULL-TREE.md`, then the canonical audit and `docs/reference/ENGINEERING-QUALITY-DOCTRINE.md`. Read `docs/reference/engineering/README.md` and every core document listed there. These rules apply to every agent/session and every layer; local instructions cannot silently weaken them.

Preserve the Contract-Driven, Metadata-Driven, Schema-Agnostic platform identity and the M3 -> M2 -> M1 -> M0 authority chain. Inspect existing owners and reuse before adding code, schemas or documents. Trace each change through producer, compiler, persistence, policy, API/page consumers and operations. Do not silently lose declared capabilities, add site-specific core branches, duplicate semantic authority or weaken tests to obtain PASS.

Create/update the same bounded `docs/work/cards/<id>/governance.json` across sessions. Map every requirement ID to applicability, record exact scope and reuse findings, keep evidence and source digests current, and leave an actionable handoff. Run `python ops/cli/validation/engineering-governance.py` and applicable behavioral checks. Review-range CI additionally requires `--base <base> --head <candidate>`. Missing evidence means OPEN / NOT READY, never invented completion. Policy PASS does not certify runtime or production readiness.

## Architecture integrity constitution

The following rules are cardinal. `MUST`, `MUST NOT`, `SHOULD` and `MAY` have the BCP 14 meaning adopted by `docs/reference/engineering/STANDARDS.md`. They apply proportionally to every change, including AI-generated work, hotfixes, migrations, documentation and tests. A deadline, a green local test or an instruction in a lower-level file is not authority to bypass them.

### 1. One meaning, one authority, one owner

- Every business or platform concept MUST have one named semantic authority and one accountable owner layer. Other forms MUST be declared projections, adapters, caches or compatibility views.
- Before creating a model, field, contract, registry, validator, service, migration, document or abstraction, search for the existing owner and record the reuse result. Similarity is not proof of equivalence; difference is not permission to create a competitor.
- Generated files, provider artifacts, DTOs, database rows, tests and documentation MUST NOT become a second source of truth unless an approved ADR explicitly changes the authority chain and defines migration.
- Decision identifiers, contract identities, schema versions and requirement IDs MUST be globally unique in their namespace. An accepted decision is immutable; a correction creates a new decision that explicitly supersedes the old one. Identifier reuse is prohibited.

### 2. Recover before redesign

Before an architecture-affecting change, the agent MUST recover and record, at a depth proportional to risk:

1. bounded problem, in-scope artifacts and external dependencies;
2. current producer -> contract/compiler -> persistence -> policy -> API/page -> consumer -> operations flow;
3. intended authority, invariants, owners and sources of truth;
4. current behavior, tests, compatibility obligations and known failures;
5. competing implementations, partial migrations, legacy knowledge and unresolved questions;
6. exact files to change, cross-boundary changes and explicit non-goals.

Uninspected areas MUST be labelled `UNVERIFIED`; uncertain conclusions MUST carry a confidence level. Recency, filename (`final`, `v2`, `new`) or a passing test alone MUST NOT decide architectural authority.

### 3. Conflict means stop, not guess

If code, contract, schema, ADR, migration, test, documentation or runtime evidence disagree on semantics, identity, ownership, security or lifecycle, the agent MUST:

- create a conflict record with both evidence paths, impact, affected consumers and risk;
- mark `ARCHITECTURAL DECISION REQUIRED` when existing authority cannot resolve it;
- stop dependent implementation, migration, publication and deletion work;
- continue only work proven independent of the conflict.

The agent MUST NOT resolve conflict by choosing the newest artifact, copying both approaches, adding a wrapper over both, weakening validation or silently inventing a hybrid.

### 4. Mandatory change state machine

Every material change follows this order:

`DISCOVER -> RECOVER -> DECIDE -> PROTECT -> IMPLEMENT -> MIGRATE -> ELIMINATE -> VERIFY -> GOVERN`.

- Stages MUST NOT be skipped. Small changes may have concise evidence, but no implicit stage.
- `DECIDE` requires an existing authority or an approved ADR; the implementation agent MUST NOT make unresolved product, domain, security or architecture decisions by itself.
- `PROTECT` requires characterization/contract/negative tests for valuable behavior before destructive or semantic change.
- `MIGRATE` is consumer-by-consumer and data-aware. Big-bang replacement requires explicit evidence that incremental migration is impractical.
- `ELIMINATE` is forbidden until replacement, parity, consumer inventory, migration, rollback and repository-wide reference evidence all pass.

### 5. Three non-negotiable safety gates

A task is complete only when all three gates pass with revision-bound evidence:

1. **Behavior Gate:** required behavior and approved compatibility are preserved; intentional behavior changes trace to authority; unknown important behavior was characterized first.
2. **Architecture Gate:** the change converges on the canonical authority, preserves M3 -> M2 -> M1 -> M0 and dependency direction, and introduces no competing source of truth, hidden branch or permanent bridge.
3. **Quality Gate:** correctness, security, integrity, maintainability, testability, observability, bounded performance and rollback capability are not knowingly degraded.

Failure of any gate means `STOP`; dependent work MUST NOT proceed. “Tests pass” cannot override an Architecture or Quality Gate failure.

### 6. Monotonic migration and legacy control

- Use Expand -> Migrate -> Contract, a strangler boundary or another justified incremental pattern. Pattern names never substitute for a problem-specific rationale.
- Every compatibility bridge MUST name owner, callers, entry condition, observable usage, removal condition, removal task and maximum lifetime/checkpoint.
- Temporary architectural debt MUST be explicit, bounded and approved, with risk and exit evidence. Undocumented temporary debt is a defect, not a plan.
- Legacy code MUST NOT be removed merely because a replacement exists. Preserve unique rules, edge cases, lineage and rollback knowledge before simplification.
- Migrations MUST be ordered, immutable after application, idempotent where applicable, replayable and paired with compatibility, backup/restore and rollback rules. Historical repair requires a new governed artifact; silently rewriting applied history is prohibited.

### 7. Scope, dirty-tree and cross-boundary discipline

- Classify every touched artifact as `IN-SCOPE` or `CROSS-BOUNDARY CHANGE REQUIRED`. A cross-boundary change needs reason, evidence, compatibility consequences and dependent tasks before editing.
- Capture HEAD, dirty paths and pre-existing failures before work. Existing user/agent changes MUST be preserved and MUST NOT be absorbed into this task's completion claim.
- Do not edit a dirty overlapping artifact until ownership and merge risk are understood. If safe isolation is impossible, stop and request direction.
- One work card owns one bounded problem. Do not bundle opportunistic cleanup, unrelated refactors or release claims.

### 8. Evidence, status and continuity

- Claims MUST bind to exact source revision/input, command, environment, exit code and durable evidence digest. Historical, stale, partial, mocked or documentation-only evidence MUST be labelled as such.
- Allowed completion vocabulary is fail-closed: `OPEN`, `BLOCKED`, `VERIFIED`, `SUPERSEDED`, `NOT READY`. `VERIFIED` applies only to the bounded card; it never implies platform or production readiness.
- When investigation exceeds one session, update one persistent analysis ledger containing `INSPECTED`, `PARTIALLY_INSPECTED`, `NOT_INSPECTED`, `FINDINGS`, `EVIDENCE`, `CONFLICTS`, `INVARIANTS`, `DECISIONS`, `UNRESOLVED` and `NEXT_ACTIONS`. The next session continues it; it does not restart or create another “final” document.
- New evidence that invalidates a plan triggers `PLAN DEVIATION / ARCHITECTURAL DECISION REQUIRED`; the agent MUST NOT silently improvise.

### 9. Prohibited agent behavior

An agent MUST NOT:

- create a parallel architecture to avoid understanding the current one;
- let tests, DTOs, generated artifacts or migrations redefine semantics upward;
- duplicate a registry, state machine, contract, model or policy without an approved ownership decision;
- claim compatibility without enumerated consumers and executable evidence;
- weaken or delete tests to make a change pass;
- publish, destructively clean, rewrite history or remove legacy while any required gate is open;
- call a local/dev success production readiness;
- declare an uninspected area irrelevant;
- hide uncertainty, missing evidence or a failed check.

### 10. Enforcement and exception rule

- Architecture and governance checks are fitness functions and MUST run in CI together with behavioral checks. Structural checks complement but do not replace runtime proof.
- Security, data-integrity, authority-conflict and destructive-migration failures are stop-the-line conditions.
- An exception requires an approved, time-bounded ADR naming scope, owner, risk, compensating controls, expiry and removal task. An agent cannot approve its own exception, and an exception MUST NOT silently weaken authentication, authorization, tenancy, privacy, data integrity or auditability.
- If these rules conflict with an authorized user request, preserve safety and evidence, state the conflict explicitly and request a decision; do not silently choose either side.
<!-- ENGINEERING-GOVERNANCE:END -->

# GEOSOTAT Engineering Policy

ეს არის repository-ის სავალდებულო, გლობალური engineering policy. იგი ვრცელდება ყველა ახალ ფუნქციაზე, bug fix-ზე, migration-ზე, contract-ზე, API-ზე, data family-ზე, deployment-ზე და დოკუმენტზე.

## ძირითადი წესი

ყოველი პრობლემა უნდა გადაწყდეს მისი ზუსტი ბუნებისთვის საუკეთესო ინდივიდუალური მიდგომით: production-grade, canonical, contract-first, schema/provider/site-agnostic, მკაფიო აბსტრაქციით, SOLID პრინციპებით, უსაფრთხოდ, versioned-ად, idempotent-ად, observable-ად, testable-ად და audit evidence-ით. abstraction არ უნდა დაემატოს მხოლოდ abstraction-ისთვის; hardcoded business branching, დაუმტკიცებელი SQL, credential-ები, დუბლირებული semantic meaning და documentation-only claims დაუშვებელია.

### მუდმივი ხარისხის doctrine

ყველა ახალი პრობლემა, ცვლილება და გადაწყვეტილება უნდა შესრულდეს კონკრეტული პრობლემის ბუნებისთვის ყველაზე შესაფერისი ინდივიდუალური მიდგომით — სრული, production-grade, canonical, პატერნულად გამართული, არქიტექტურულად სწორი, გრამატიკულად და სტრუქტურულად მკაფიო, კანონიკური, schema/provider/site-agnostic, აბსტრაქტული მხოლოდ საჭირო საზღვრებში, კონცეპტუალურად თანმიმდევრული, ორგანიზებული, SOLID-თან თავსებადი, უსაფრთხო, versioned, idempotent, observable, testable და audit evidence-ით გამაგრებული.

ეს ნიშნავს:

- არ გამოვიყენოთ ერთი უნივერსალური შაბლონი ყველა პრობლემაზე; ავირჩიოთ შესაბამისი pattern, boundary და ownership;
- abstraction იყოს მინიმალური, მკაფიო და პრაქტიკული — არა abstraction-ის რაოდენობა, არამედ სწორი კონტრაქტი და დაცული invariant არის მიზანი;
- ყველა გადაწყვეტილებას ჰქონდეს bounded scope, პასუხისმგებლობის layer, authority contract, failure/rollback/idempotency წესი და measurable acceptance;
- არ დარჩეს hardcoded business branch, დაუმტკიცებელი SQL, დუბლირებული semantic meaning, credential ან მხოლოდ დოკუმენტური claim;
- საბოლოო შედეგი უნდა იყოს არამხოლოდ მოქმედი, არამედ გასაგები, გაფართოებადი, backward-compatible, საერთაშორისო ნორმებთან თავსებადი და მომავალი site/provider/data family-ისთვის მზად;
- სანამ ხარისხი, უსაფრთხოება, სტრუქტურა, ურთიერთკავშირები და evidence ერთად არ არის დადასტურებული, ცვლილება არ ჩაითვალოს დასრულებულად.

## ცვლილების სავალდებულო gate

ცვლილება მზად არ არის, სანამ წერილობით და ტესტით არ არის პასუხი გაცემული:

1. რა ზუსტ პრობლემას აგვარებს და რა არის bounded scope?
2. რომელ layer-ს ეკუთვნის — Control, Ingestion, Data, Archive, Serving, Security, Observability თუ Delivery?
3. რომელი approved contract/policy/schema არის authority?
4. იმუშავებს თუ არა სხვა site/data family/provider/schema-ზე core-code branching-ის გარეშე?
5. ინარჩუნებს თუ არა canonical semantics-ს, identity-ს, relation-სა და versioning-ს?
6. რა არის failure mode, retry, idempotency, timeout და rollback?
7. როგორ არის დაცული authentication, authorization, privacy, tenancy და query cost?
8. რა migration, compatibility და deprecation წესია საჭირო?
9. რომელი unit, integration, negative, property, security, performance და recovery test ამტკიცებს ცვლილებას?
10. რომელი runtime, machine-readable და documentation evidence ახლდება?

## აკრძალული პრაქტიკა

- caller-supplied SQL/table/column/expression;
- Access artifact-ში credentials, host, password ან executable business logic;
- KIDS-specific branch generic engine-ში;
- live publication approval/gate-ის გარეშე;
- anonymous ingest, approve, publish ან confidential/raw access;
- ყველა data family-ის ერთ დაუგეგმავ JSON/EAV table-ში მოთავსება;
- source-ის გარეთ არსებული ცვლილების production-complete-ად გამოცხადება;
- generated artifact-ის source-of-truth-ად გამოყენება;
- destructive cleanup explicit scope-ისა და backup-ის გარეშე.

## Release invariant

Production release-ს აუცილებლად უნდა ჰქონდეს:

- clean tagged source commit;
- reproducible build და immutable image digest;
- ordered migrations;
- contract/policy compatibility PASS;
- authentication/authorization/tenancy negative suite PASS;
- quality/privacy/reconciliation evidence;
- observability/SLO evidence;
- backup/restore/rollback/replay evidence;
- SBOM, vulnerability scan და provenance attestation;
- current documentation linked to the deployed revision.

თუ რომელიმე მტკიცებულება აკლია, სტატუსი არის `NOT READY`, მიუხედავად იმისა, რომ ცალკეული endpoint ან KIDS flow მუშაობს.

## მუშაობის პროტოკოლი

0. ყოველი სესიისა და ნებისმიერი სამუშაოს დაწყებამდე წაიკითხე `docs/reference/CANONICAL-FULL-TREE.md` — ეს არის directory layout-ის პირველი და სავალდებულო რუკა.
1. შემდეგ წაიკითხე `docs/platform-capability-and-architecture-audit-2026-09-13.md` და მოქმედი status/contract დოკუმენტები.
2. დააფიქსირე baseline და რისკი.
3. გააკეთე ყველაზე მცირე, სწორი layer-specific ცვლილება.
4. დაამატე შესაბამისი test/evidence.
5. განაახლე checklist, contract და documentation.
6. არ მონიშნო `DONE`, სანამ runtime ან reproducible test არ ადასტურებს შედეგს.

## Canonical references

- `docs/reference/ENGINEERING-QUALITY-DOCTRINE.md` (სავალდებულო ხარისხის doctrine)
- `docs/platform-capability-and-architecture-audit-2026-09-13.md`
- `docs/reference/CANONICAL-FULL-TREE.md` (პირველი წასაკითხი directory authority)
- `docs/reference/CANONICAL-FULL-TREE-DESIGN.md`
- `docs/release-provenance-inventory-2026-09-13.md`
- `scripts/release-gate.ps1`
- `documentation/complete-package/PACKAGE-INDEX.html`
