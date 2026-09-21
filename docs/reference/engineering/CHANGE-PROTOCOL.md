# Change and session continuity protocol

## Before implementation

Read the authority chain. Capture HEAD, dirty paths, applicable status and known failures. Search source, contracts, ADRs and AIR with `rg` before creating anything. Open relevant implementations, not only filenames. Select one existing owner. Create or update `docs/work/cards/<id>/governance.json`; retain the same card across sessions. Follow the shape of the governance-package card, with status OPEN until evidence exists.

Record problem, bounded scope/non-goals, owner layer, authority paths, reuse findings, end-to-end impact, compatibility, failure/retry/idempotency/timeout/rollback behavior, security/tenancy/privacy/cost, and handoff. Map every requirement ID to APPLY or NA with a substantive reason. List exact repository paths owned by this change. Do not absorb unrelated dirty files into the change's completion claim.

## Implementation and validation

Establish acceptance before editing. Select tests by risk: grammar/compiler changes need malformed/unsupported/reference and semantic round-trip cases; persistence needs constraints, transactions and migration; authorization needs actual denied/allowed calls across tenant boundaries; stateful workflows need duplicate/conflict/concurrency/recovery; serving/pages need contract/capability and budget coverage. Document why a category is inapplicable. A mock can isolate a dependency, but cannot prove the mocked integration. Structural source scans complement behavioral tests.

For every failed check determine defect, environment limitation or stale test expectation with evidence. Fix the cause; do not lower assertions, globally serialize tests, broaden roles or disable checks without an independently justified invariant-preserving change. A legitimate behavior change updates the approved contract and regression tests together. Capture test commands, exit codes, environment/inputs and output paths. Evidence digests prevent accidental drift; they are not signatures or proof of an honest runner.

## Completion and handoff

Use OPEN while work remains and VERIFIED only when all declared acceptance is supported. VERIFIED requires at least one evidence file, exact SHA-256, covered source file digests, nonempty test records and all exit codes zero. Record failures and NOT_RUN honestly in OPEN cards. Generated reports go under `build/`; retain durable review evidence under `docs/work/evidence/<id>/` when necessary. Do not hand-edit generated reports to manufacture a pass.

Update the existing checklist, AIR/ADR/contract when affected. Handoff MUST name what changed, what was proven, unresolved issues, exact next action and relevant paths. The next session verifies source/evidence freshness before continuing; it does not recreate the system or reset the status. VERIFIED describes the card's scope only, never platform/release completion.

When later work legitimately changes covered source, set the old card to SUPERSEDED with `supersededBy` naming its successor card. Preserve old evidence as historical; do not rewrite past results. Supersession must be acyclic and reference an existing card. A SUPERSEDED or OPEN card provides no review-range coverage. The successor must re-prove affected invariants before VERIFIED. This permits deliberate evolution without treating old hashes as a permanent source freeze.

## Review-range gate and adoption

The governance checker defaults to structural validation of all cards. In CI provide immutable `--base` and `--head` commits; it rejects changed paths without card coverage, including deleted and renamed paths. Run against a checkout of the candidate commit. Require VERIFIED coverage for review-range changes. Source digests bind evidence to covered files (UTF-8 text normalized to LF for checkout portability, binary bytes unchanged). A card does not hash itself to avoid recursive hashes; its claims must be reviewed.

The present dirty worktree predates this policy package. It is not grandfathered as correct and is not claimed as covered by this card. Create bounded cards for those changes before merging them. No retroactive invented test results. Branch protection and reviewer identity are external controls; deployment operators must require existing release evidence as well as this gate.
