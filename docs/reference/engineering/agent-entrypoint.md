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
