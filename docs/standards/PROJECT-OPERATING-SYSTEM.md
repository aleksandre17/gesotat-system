---
id: STD-RCP-001
type: REFERENCE
title: Repository Control Protocol (RCP)
status: ACTIVE
authority: CANONICAL
scope: generic — reusable across projects
created: 2026-09-20
updated: 2026-09-20
---

# Repository Control Protocol (RCP)

**A reusable protocol for keeping a long-lived engineering repository self-describing,
resumable and free of duplicate truth — for humans and AI agents alike.**

This document is the **generic standard**. It contains no project state. A project's
instantiation lives in its own control plane (see §9) and never inside this file.

---

## 1. The invariant

> After any substantial work session, a competent new engineer or agent must be able to
> continue correctly **from repository state alone**, without the previous conversation.

If continuation requires chat history to reconstruct current state, authority, unfinished
work, decisions or the next permitted action, **the previous session was not closed**.

## 2. The information law

```text
ONE FACT -> ONE CANONICAL OWNER -> MANY REFERENCES
```

Never maintain the same semantic fact independently in two artifacts. Reference the owner.
If a copy is needed for presentation, it must be **mechanically derived** or **visibly
non-authoritative**.

## 3. Six distinct concepts — never merged

| Concept | Answers | Owner |
|---|---|---|
| KNOWLEDGE | What is true about the system? | architecture / contracts / reference |
| STATE | Where are we now? | `CURRENT` |
| WORK | What are we changing? | work items |
| DECISION | Why did we choose this? | decision records |
| EVIDENCE | How do we know? | evidence artifacts |
| HISTORY | What happened before? | archived / historical artifacts |

**No artifact may own more than one of these.** The most common failure is a "master
document" that becomes all six.

## 4. Planes

Responsibilities stay distinct even when directories are shared.

**CONTROL** — navigation and current state · **KNOWLEDGE** — durable system truth ·
**DECISION** — choices and rationale · **WORK** — change activity · **GOVERNANCE** —
authority, lifecycle, rules · **IMPLEMENTATION** — code, schema, config · **EVIDENCE** —
tests, audits, verification.

## 5. Vocabularies — controlled, not emergent

**Artifact type:** `CONTROL` · `ARCHITECTURE` · `DECISION` · `CONTRACT` · `REFERENCE` ·
`GUIDE` · `WORK` · `EVIDENCE` · `REGISTER` · `REPORT` · `HISTORICAL`

**Authority:** `CANONICAL` · `SUPPORTING` · `HISTORICAL` · `SUPERSEDED` · `MIGRATION_ONLY`

**Status (lifecycle/work):** `NOT_STARTED` · `READY` · `ACTIVE` · `BLOCKED` · `VERIFYING` ·
`PASS` · `FAIL` · `COMPLETE` · `SUPERSEDED` · `CANCELLED`

**These vocabularies are closed.** Adding a value is a governed change to this standard.

> **Authority is never inferred from filename, recency or detail.** Not "this looks newer",
> not "this says final", not "this AI wrote it last".

## 6. Artifact metadata contract

Every governed artifact carries a YAML header. Only `id`, `type`, `title`, `status`,
`authority` are mandatory; the rest are used when meaningful.

```yaml
id: ADR-014            # stable, survives file moves
type: DECISION
title: ...
status: ACTIVE
authority: CANONICAL
owner: ...             # role or team
scope: ...
created: / updated:
supersedes: / superseded_by:
related: [...]         # DEPENDS_ON, IMPLEMENTS, EVIDENCE_FOR, ...
```

**Identity is the `id`, never the path.** Files may move; IDs may not change.

**ID namespaces** (extend per project): `ADR-###` decisions · `INV-###` invariants ·
`REQ-###` requirements · `RISK-###` risks · `FIND-###` findings · `TASK-###` work ·
`PHASE-###` lifecycle · `STD-###` standards.

## 7. The three control artifacts

Exactly one of each. Never two.

| Artifact | Owns | Must not contain |
|---|---|---|
| `CURRENT` | **current state only** — phase, stage, gate, active work, blockers, unknowns, next permitted action, forbidden actions, required reads | history, architecture, findings, decisions |
| `MANIFEST` | lifecycle phases, entry/exit criteria, gates, dependencies, outputs | current state detail, work-item detail |
| `CATALOG` | navigation metadata for every governed artifact | the artifacts' semantic content |

`CURRENT` is deliberately small. It is a **pointer**, not a narrative.

## 8. Lifecycle and gates

A project defines its own phases (e.g. `DISCOVERY → RECOVERY → DESIGN → PLANNING →
IMPLEMENTATION → MIGRATION → VERIFICATION → RELEASE → CLEANUP → MAINTENANCE`).

Every phase has: ID · status · purpose · entry criteria · exit criteria · dependencies ·
outputs · gate.

A **gate** has entry requirements, checks, result, evidence, failure consequence and next
permitted state.

> **`PASS` means the evidence was satisfied. `PASS` is never a compliment.**

## 9. Project control plane layout

```text
docs/project/
  README.md      entry point and how-to
  CURRENT.md     the single current-state authority
  MANIFEST.md    lifecycle and gates
  CATALOG.md     artifact catalog
  ADOPTION.md    mapping of pre-existing artifacts (bootstrap only)
  work/
    active/      small — only in-flight items
    blocked/
    completed/   history; not in the default read path
```

Create only what responsibility requires. **Do not create empty ceremonial folders.**

## 10. Work items

Work state must never be inferred from prose. A work item carries: `id` · `title` · `type`
· `status` · objective · rationale · scope · out-of-scope · dependencies ·
**`required_context`** · affected authorities · constraints · risks · acceptance criteria ·
verification · outputs · blockers · next action.

Hierarchy (`INITIATIVE → PHASE → WORK PACKAGE → TASK`) is used only where it earns its
keep.

`required_context` is the mechanism that makes agent work reliable: it names **exactly**
the canonical artifacts needed — no more, no less.

## 11. Progressive disclosure

```text
L0  AGENTS.md + CURRENT                      always
L1  the active work item                     always
L2  artifacts in its required_context        always
L3  decisions / contracts it touches         when relevant
L4  evidence / history                       only to falsify a conclusion
```

**Never instruct an agent to read the documentation tree by default.** Human attention and
context windows are finite engineering resources; optimise for *minimum sufficient context
with maximum correctness*.

## 12. Session protocols

**START** — read the AGENTS.md hierarchy → read `CURRENT` → resolve the active work item →
load its `required_context` → identify affected canonical authorities → check gates and
dependencies → **search for an existing mechanism before creating one** → execute within
scope.

**END** — verify acceptance criteria → record outputs and unresolved issues → update
affected canonical knowledge → update registers → update work status → update `CURRENT` if
state changed → update `MANIFEST` if a gate changed → update `CATALOG` for new or
superseded artifacts → confirm no duplicate authority was introduced → **state the next
permitted action**.

> A session is incomplete if the next agent must reconstruct what happened from `git diff`
> or chat history.

**Session notes**, if kept, are explicitly non-authoritative. Any durable conclusion is
promoted to its canonical owner before closure.

## 13. Anti-parallelism

The protocol enforces, for artifacts exactly as for code:

```text
ONE RESPONSIBILITY -> ONE CANONICAL AUTHORITY -> DERIVED REPRESENTATIONS
```

Before adding any artifact, code, schema, registry, contract, config or rule, ask: **who
already owns this responsibility?** If an owner exists, extend or reference it. If the
canonical mechanism cannot support the need, **stop and report the conflict** — do not
build beside it.

**The control plane obeys its own rule:** exactly one current-state authority, one
manifest, one catalog. Documentation chaos is never solved by adding documentation about
documentation.

## 14. Supersession

Never overwrite rationale. Use `A --superseded_by--> B`. Navigation points to B; history
preserves A. Applies to decisions, designs, plans, standards and governance artifacts.

**`HISTORICAL ≠ CURRENT`**, and the difference must be visible in metadata — not inferable
only by reading.

## 15. Registers

A register that can grow without bound starts as one file and is promoted to a directory
with an index when navigation degrades:

```text
findings/README.md · FIND-001.md · FIND-002.md · index.yaml
```

**Split by entity or responsibility — never by `-2`, `-new`, `-final`.**

## 16. Naming discipline

Banned in filenames: `final`, `final-final`, `new`, `latest`, `latest2`, `fixed`, `real-`.
Status belongs in metadata; names carry stable semantics.

## 17. Granularity

**Split** when responsibilities, lifecycles or ownership diverge, or when navigation
degrades. **Do not** split to satisfy a line count, or combine to reduce file count.

A large cohesive file is fine. A file holding unrelated responsibilities is not. Use size
as a **signal to inspect**, never as a rule.

## 18. Automation philosophy

```text
deterministically derivable -> AUTOMATE
partially derivable         -> automate the mechanical part
human judgement required    -> KEEP EXPLICIT
```

Generated indexes are navigation, **never semantic authority**.

## 19. Validation — `project verify`

A validator should progressively check: exactly one CURRENT / MANIFEST / CATALOG · valid
metadata · unique IDs · resolvable references · closed-vocabulary values · no completed
work in the active set · no superseded artifact in a `required_context` · acyclic
supersession · valid work dependencies · `CURRENT` agrees with `MANIFEST` · no
canonical-looking artifact without classification · no duplicate canonical ownership.

Start simple. A specification with no implementation is still a protection requirement.

**The validator must itself be proven**, by one negative test per check that injects the
defect and asserts that exact check fires. A validator only ever run against a healthy
repository is unproven: its `PASS` cannot be distinguished from an inability to detect
anything. The test suite is a required component of the validator, not an optional extra.

**Classification requires an assigned value, not a mention.** A check that accepts "this
path appears somewhere in the register" as classification can be satisfied by a footnote,
which is how classification decays into name-dropping.

## 20. Adoption and change safety

**Bootstrap** may require manual classification of pre-existing artifacts. **Steady state**
relies on templates, metadata, validation and generated indexes. Bootstrap complexity is
not preserved permanently.

Changing the control plane follows migration discipline: **discover → classify → index →
establish navigation → validate references**, and only then move or merge under a work
item. **Navigation correctness outranks folder aesthetics**, and no artifact is moved,
merged or archived until its unique durable knowledge has a canonical destination.

## 21. Humans and agents share one knowledge base

There is no `docs-for-humans` and `docs-for-ai`. Agent-specific files describe **operating
behaviour and context routing** — never duplicated project knowledge.

## 22. Failure modes this protocol exists to prevent

Documentation sprawl · giant markdown files · duplicate truth · parallel architecture ·
stale current-state documents · orphan decisions · hidden blockers · historical documents
appearing current · chat-only decisions · unbounded temporary architecture · multiple
roadmaps · multiple current-state files · drifting manual indexes · copy-pasted registers ·
ambiguous ownership · "latest file wins" · **a new agent creating a new mechanism beside an
existing one** · context-window overload.

## 23. Design test

Before creating any artifact, folder or rule: *What responsibility does this own? Why must
it exist? Who consumes it? Can it be derived? Does something already own this? Will it stay
cohesive as the project grows? Can a new agent find it without guessing?*

**If these cannot be answered, do not create it.**

## 24. Quality bar

Clarity · cohesion · discoverability · traceability · low cognitive and context load · no
duplicate truth · deterministic navigation · safe evolution · human/AI continuity ·
scalability · minimal ceremony · automation · auditability.

> **Best does not mean most files. Best means the minimum structure that sustains maximum
> reliable control.**
