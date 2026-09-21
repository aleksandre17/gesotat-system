---
id: ADR-016
type: DECISION
title: Access provider status
status: ACTIVE
authority: CANONICAL
scope: the architectural status of Microsoft Access, and what may and may not depend on it
created: 2026-09-21
updated: 2026-09-21
related: AUD-CAPABILITY, STD-BENCH-001, ADR-015
---

# ADR-016 — Access is a supported provider with no semantic authority

**Status:** ACCEPTED 2026-09-21. Resolves `BM-Q-02`.

## Context, and why the original question was the wrong shape

`BM-Q-02` asked whether Access is **permanent** or **transitional**. Both answers are traps:
*permanent* licenses Access limits to shape canonical semantics; *transitional* licenses
neglect — under-investment in a provider that carries live work today, and a standing excuse to
treat its defects as temporary. `PHASE-003` measured that Access is doing real relational work
(24–36 enforced relationships, 31–41 unique indexes, enforced 1:1 cardinality, `NUMERIC(28,16)`)
and also that its container is byte-non-deterministic and its `MEMO` columns are where typed
semantics leaked out. Neither "permanent" nor "transitional" captures that.

## Decision

**Access is a currently supported, potentially long-lived, first-class provider and physical
representation. It holds no canonical semantic authority, and the Canonical Architecture makes
no assumption that it is permanent.**

Seven rules follow, each with the test that makes it enforceable rather than aspirational:

| # | Rule | Test |
|---|---|---|
| 1 | Access is treated as a real provider, not a temporary hack: its artifacts are generated, validated and governed like any other output | generation is contract-driven, with no site literal in the generic generator |
| 2 | Access-specific strengths **may** be used behind the provider boundary — typed columns, referential integrity, unique and 1:1 enforcement, navigation grouping, local offline editing | a strength is used only through a declared provider capability, never by a branch on the provider's name |
| 3 | Access limits **may not** weaken canonical semantics | for every capability the canonical model lacks, the reason must be a requirement, not a provider limit (`BM-EC-085`) |
| 4 | Bounding a **declaration** by the weakest provider required for a dataset's round trip is legitimate; lowering **canonical storage or semantics** to it is prohibited | the `DR-006` shape: `declaration ≤ provider capability ≤ canonical storage`, which `PHASE-003` confirmed is what the 28-digit ceiling actually does |
| 5 | Lowest-common-denominator provider abstraction is prohibited; provider-specific optimisation and materialisation are permitted | `BM-EC-085`, `BM-REJ-09` |
| 6 | The canonical architecture must survive the **addition** of a provider, not only the replacement of one | `SCH-003`: a second, differently shaped provider passes the same contract suite with zero core change |
| 7 | No canonical semantic authority: Access may **reference** a definition and may never **define or approve** one | `BM-INV-08`; `CF-033`/`CF-034` are the live violations |

## What the original framing missed, and this decision adds

**Access is three responsibilities, not one, and only the first is about being a database.**

| Responsibility | What actually depends on it | Replaceable independently? |
|---|---|---|
| **Physical provider** — typed relational materialisation of a declared plan | the physical pattern and provider capability registry | yes — rule 6 |
| **Authoring surface** — a domain expert fills a file offline, with pickers, grouping and validation | `REQ-012`, `BM-QA-11`, and the owner's stated finish line | **the dependency is on *an* authoring surface with offline, local, typed, relational editing — Access is today's implementation of it, not the requirement** |
| **Round-trip carrier** — the filled file returns and is read back | `DR-007`, and where `CF-033`/`CF-034` did their damage | yes, and it is where authority leakage must be blocked |

Conflating these is how "replace Access" becomes unanswerable: replacing the *provider* is a
capability-registry exercise; replacing the *authoring surface* is a product decision about how
domain experts work. **`PHASE-004` must treat them as three boundaries.**

**Rules 3–5 are unenforceable until provider capabilities have exactly one producer.** `CF-038`
records three competing authorities for provider limits (registry, Spring properties, physical
planner) and `RC-003` records that `platform.provider_capability` has **no producer at all**.
Until that is fixed, "Access limits must not weaken canonical semantics" is a sentence, not a
control. This is a dependency of the decision, recorded here and owned by
`REC-PASS3` §4 authority #5.

## Alternatives considered

| Alternative | Rejected because |
|---|---|
| `PERMANENT` | licenses provider limits to become canonical constraints; contradicts `CAD-17` |
| `TRANSITIONAL` | licenses neglect of a provider carrying live work, and makes every Access defect someone else's future problem |
| *"Access is only an export format"* | false to evidence: it is the authoring surface and the round-trip carrier, and the platform reads canonical data back from it |
| *"Abstract over all providers now"* | premature and unfalsifiable: one provider is implemented, so any abstraction would be designed against a sample of one — and `BM-REJ-01` rejects a mechanism with no observed variation |

## Consequences

- `PHASE-004` may design freely for canonical strength, and must isolate the five
  provider-specific concerns `AUD-CAPABILITY` §4 names.
- The 28-digit ceiling stays a **declaration** bound, not a storage bound.
- `SCH-003` generality remains **evidenced for one provider** — the audit found no
  lowest-common-denominator distortion, but also found no second provider. Rule 6 is therefore
  an obligation with no evidence yet, and must not be reported as satisfied.
- An Access-specific capability may be used and **must not** be required: if a canonical
  behaviour can only be expressed through an Access feature, the canonical model is wrong.

## Falsification attempt

*Is "no assumption of permanence" hollow — would anything actually break if Access vanished?*
Testing it: the canonical model would lose nothing (`AUD-COMPARATIVE` §12 answers the *if Access
did not exist* test — the dimension-keyed observation model, exact decimal, codelist FKs and the
provenance chain would all still be chosen). **The authoring surface would break**, and no
replacement exists. So the decision is not hollow, and its exposure is precisely located: the
platform's real dependency is the authoring surface, not the database engine. **Confidence:
HIGH** on the provider rules, **MEDIUM** on rule 6 until a second provider exists.
