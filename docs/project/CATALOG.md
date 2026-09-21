---
id: CTRL-CATALOG
type: CONTROL
title: Governed artifact catalog
status: ACTIVE
authority: CANONICAL
scope: whole repository
created: 2026-09-20
updated: 2026-09-21
---

# CATALOG — governed artifacts

Navigation metadata only. **This file never duplicates artifact content.** For what an
artifact says, open it.

Vocabularies are RCP §5. `READ_WHEN` uses the progressive-disclosure levels of RCP §11.

---

## Control plane

| ID | Path | Type | Authority | Owns | READ_WHEN |
|---|---|---|---|---|---|
| `CTRL-CURRENT` | `docs/project/CURRENT.md` | CONTROL | CANONICAL | current state | **L0 — always** |
| `CTRL-MANIFEST` | `docs/project/MANIFEST.md` | CONTROL | CANONICAL | lifecycle, gates | **L0 — always** |
| `CTRL-CATALOG` | this file | CONTROL | CANONICAL | artifact navigation | when locating an artifact |
| `CTRL-README` | `docs/project/README.md` | GUIDE | CANONICAL | how to use the control plane | first visit; when adding an artifact |
| `CTRL-ADOPTION` | `docs/project/ADOPTION.md` | REGISTER | CANONICAL | pre-existing artifact mapping | bootstrap only |

## Governance

| ID | Path | Type | Authority | Owns | READ_WHEN |
|---|---|---|---|---|---|
| `STD-RCP-001` | `docs/standards/PROJECT-OPERATING-SYSTEM.md` | REFERENCE | CANONICAL | the generic protocol | adding an artifact type or changing the control plane |
| `STD-BENCH-001` | `docs/standards/ARCHITECTURE-QUALITY-BENCHMARK.md` | REFERENCE | CANONICAL | architecture quality benchmark — quality attributes, hard invariants, evaluation and rejection criteria, verdict method | **L2** — before evaluating, comparing or designing an architecture |
| `STD-AUTO-001` | `docs/standards/AUTOMATION-CLASSIFICATION-MODEL.md` | REFERENCE | CANONICAL | automation classification — target class per responsibility and the rule that assigns it | when deciding whether something should be automated |
| `GOV-AGENTS` | `/AGENTS.md` | CONTROL | CANONICAL | operating constitution, engineering policy | **L0 — always** |
| `CTRL-WORK` | `docs/project/work/README.md` | CONTROL | CANONICAL | the work-item mechanism | before creating or closing a work item |
| `CTRL-VERIFY` | `ops/cli/validation/rcp-verify.py` | CONTROL | CANONICAL | **the deterministic enforcement of everything in this catalog** | before claiming the control plane is consistent |
| `CTRL-VERIFY-TEST` | `ops/tests/governance/test_rcp_verify.py` | EVIDENCE | SUPPORTING | proof that each validator check actually fires | when changing `CTRL-VERIFY` |
| `CTRL-BOOTSTRAP-TEST` | `ops/tests/governance/test_rcp_bootstrap.py` | EVIDENCE | SUPPORTING | proof that the repository alone answers every continuation question | when changing any control artifact |
| `CTRL-POS-COVERAGE` | `ops/tests/governance/test_pos_coverage.py` | EVIDENCE | SUPPORTING | proof that every mechanism the commissioning specification required still exists | when removing or restructuring a control mechanism |
| `DEC-RCP` | `docs/decisions/ADR-repository-control-protocol.md` | DECISION | CANONICAL | **why RCP was adopted** — context, alternatives, consequences, what it must never become | before changing the protocol or proposing a parallel mechanism |
| `CARD-RCP` | `docs/work/cards/repository-control-protocol/governance.json` | EVIDENCE | SUPPORTING | the control-protocol change record: baseline, file scope, digests, test evidence, handoff — **`SUPERSEDED` → `CARD-BENCH`** since 2026-09-21 | to audit what the control-protocol work changed |
| `CARD-BENCH` | `docs/work/cards/standards-architecture-quality-benchmark/governance.json` | EVIDENCE | SUPPORTING | the benchmark change record and its first `GATE-STANDARDS` evidence bundle — **`SUPERSEDED` → `CARD-REVIEW`** since 2026-09-21 | to audit what the benchmark work changed |
| `CARD-REVIEW` | `docs/work/cards/phase-002-acceptance-review/governance.json` | EVIDENCE | SUPPORTING | the acceptance-review change record and the `GATE-STANDARDS` re-confirmation evidence | to audit what the review changed and why the gate was re-confirmed |
| `CARD-AUDIT` | `docs/work/cards/artifact-comparative-audit/governance.json` | EVIDENCE | SUPPORTING | the `PHASE-003` change record and the `GATE-COMPARATIVE` evidence bundle | to audit how the artifacts were inspected and what was measured |
| `CARD-ENTRY` | `docs/work/cards/design-entry-decisions/governance.json` | EVIDENCE | SUPPORTING | the design-entry decision change record and its evidence | to audit how ADR-014/015/016 and AQ-05 were decided |
| `CARD-DESIGN` | `docs/work/cards/canonical-design/governance.json` | EVIDENCE | SUPPORTING | the `PHASE-004` change record — `OPEN` while the phase is active | to audit what canonical design has and has not yet established |
| `DOC-CAD` | `docs/work/GEOSTAT-API-CANONICAL-AUTHORITY-DOCTRINE-2026-09-20.md` | REGISTER | CANONICAL | **Authority Registry (CAD-02)**, CAD-01…18 | **L2** — before creating any mechanism |

## Recovery knowledge — `PHASE-001` outputs

| ID | Path | Type | Authority | Owns | READ_WHEN |
|---|---|---|---|---|---|
| `REC-CONSOLIDATION` | `docs/work/GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md` | REGISTER | CANONICAL | findings CF-001…042, RC-001…003, DR-001…008, INV-001…014, QF, AMS-001, REQ-001…041, legacy register, automation baseline, orphans, contradictions | **L2** |
| `REC-PASS3` | `docs/work/GEOSTAT-API-RECOVERY-PASS3-CLOSURE-2026-09-20.md` | REPORT | CANONICAL | protection requirements, clean-build proof spec, temporary-architecture register, document dispositions, **Design handoff §10** | **L2** — design entry |
| `REC-COVERAGE` | `docs/work/GEOSTAT-API-RECOVERY-LAYER-COVERAGE-2026-09-20.md` | REGISTER | CANONICAL | L0–L47 coverage, GOV-R1…R10 | **L3** |
| `REC-AUDIT` | `docs/work/GEOSTAT-API-RECOVERY-COMPLETENESS-AUDIT-2026-09-20.md` | EVIDENCE | SUPPORTING | per-finding verification evidence; what each pass did not cover | **L4** — to falsify a conclusion |
| `REC-CHECKPOINT` | `docs/work/GEOSTAT-API-ARCHITECTURE-RECOVERY-CHECKPOINT-2026-09-20.md` | EVIDENCE | SUPPORTING | batch-by-batch evidence trail | **L4** |

**Ownership rule:** a finding lives in `REC-CONSOLIDATION`; its *evidence* lives in
`REC-AUDIT` or `REC-CHECKPOINT`; its *authority impact* lives in `DOC-CAD`. Never restate a
finding in a second register.

## Comparative knowledge — `PHASE-003` outputs

| ID | Path | Type | Authority | Owns | READ_WHEN |
|---|---|---|---|---|---|
| `AUD-COMPARATIVE` | `docs/work/GEOSTAT-ARTIFACT-COMPARATIVE-AUDIT-2026-09-21.md` | REPORT | CANONICAL | artifact identity and provenance, responsibility classification, multi-level comparison, semantic structural diff, loss and innovation analysis, comparative falsification | **L2** — before judging any artifact |
| `AUD-CAPABILITY` | `docs/work/GEOSTAT-CAPABILITY-INVENTORY-2026-09-21.md` | REGISTER | CANONICAL | CAP-### capability register with dispositions, required-but-missing capabilities, the canonical-design input package | **L2** — design entry |

## Design knowledge — `PHASE-004` (in progress)

| ID | Path | Type | Authority | Owns | READ_WHEN |
|---|---|---|---|---|---|
| `ARCH-BASELINE` | `docs/work/GEOSTAT-PHYSICAL-RELATIONAL-BASELINE-2026-09-21.md` | EVIDENCE | CANONICAL | the reconstructed SQL physical model — tables, constraints, indexes, triggers, topology — and the anti-regression analysis | **L2** — before generalizing, replacing or removing any relational structure |
| `ARCH-DOSSIER` | `docs/work/GEOSTAT-PHYSICAL-DATA-DOSSIER-2026-09-21.md` | EVIDENCE | CANONICAL | the database universe, effective schema, ORM correspondence, legacy boundary, anti-regression matrix and draft contradiction matrix | **L2** — before changing any physical structure or finalizing the draft |

| `ARCH-CANONICAL` | `docs/work/GEOSTAT-CANONICAL-ARCHITECTURE-2026-09-21.md` | ARCHITECTURE | CANONICAL | the canonical architecture — semantic kernel, families, identity, grain, relationships, metadata, temporality, contracts, providers, materialization, and the stable-kernel/evolvable-logical-model design | **L2** — before designing, planning or implementing anything downstream |

| `PLAN-MASTER` | `docs/work/GEOSTAT-MASTER-REHABILITATION-PLAN-2026-09-21.md` | REGISTER | CANONICAL | how the existing system converges — responsibility inventory, parallel-authority register, dispositions, convergence map, ordered roadmap, elimination register, protection plan, decision register, `PHASE-006` entry contract | **L2** — before planning, ordering or scheduling any rehabilitation work |

It was `BLOCKED` while its §3, §4 and §11 stood contradicted by `ARCH-BASELINE`; Part II (§30–§40)
resolved all six contradictions against physical evidence, so it is now catalogued as canonical.
**Part II governs where the two parts disagree.**

**These judge artifacts; they do not own findings.** A defect found during the audit is recorded
against its existing owner (`REC-CONSOLIDATION`, `DOC-CAD`), never re-registered here.

## Decisions

Each ADR owns one decision. Listed individually so that duplicate decision ownership is
detectable (RCP-404) rather than hidden behind a glob.

| ID | Path | Type | Authority | Owns | READ_WHEN |
|---|---|---|---|---|---|
| `DEC-REGISTER` | `docs/platform-decisions.md` | DECISION | CANONICAL | ADR-001…011, inline | tracing an early platform decision |
| `DEC-TENANCY` | `docs/decisions/ADR-tenant-scoped-authorization.md` | DECISION | CANONICAL | ADR-010 tenant-scoped authorization; controller tenancy exemptions | touching authorization or tenancy |
| `DEC-LEGACY` | `docs/decisions/ADR-legacy-surface-retirement.md` | DECISION | CANONICAL | ADR-011 legacy surface retirement | retiring a legacy surface |
| `DEC-ACCESS-INTEGRITY` | `docs/decisions/ADR-access-package-integrity-automation.md` | DECISION | CANONICAL | ADR-012 integrity automation inside an Access package | changing Access package integrity |
| `DEC-RCP` | `docs/decisions/ADR-repository-control-protocol.md` | DECISION | CANONICAL | ADR-013 adoption of the control protocol | changing the protocol itself |
| `DEC-LIFECYCLE` | `docs/decisions/ADR-lifecycle-program-separation.md` | DECISION | CANONICAL | ADR-014 the relationship between the two programs and their shared authority plane | before scheduling work that crosses the two programs |
| `DEC-SDMX` | `docs/decisions/ADR-sdmx-conformance-boundary.md` | DECISION | CANONICAL | ADR-015 which responsibilities SDMX governs and which it must not shape | before applying a statistical standard to a non-statistical family |
| `DEC-ACCESS-PROVIDER` | `docs/decisions/ADR-access-provider-status.md` | DECISION | CANONICAL | ADR-016 the architectural status of Access as a provider | before letting a provider capability or limit reach canonical semantics |

## Live platform obligations

**Not rehabilitation phases.** Referenced by `CTRL-MANIFEST` under *Inherited platform
obligations*; owned here, restated nowhere.

| ID | Path | Type | Authority | Owns | READ_WHEN |
|---|---|---|---|---|---|
| `OBL-WORKSTREAMS` | `docs/platform-capability-and-architecture-audit-2026-09-13.md` | REGISTER | SUPPORTING | W-01…W-07 production workstreams and their dependency order | planning production readiness |
| `OBL-COMPLETION` | `docs/contract-driven-metadata-schema-agnostic-completion-plan.md` | REGISTER | CANONICAL | C-01…C-14 completion plan and acceptance checklist | closing a platform capability item |
| `OBL-API-GAP` | `docs/api-modernization-capability-gap.md` | REGISTER | SUPPORTING | API modernization capability gaps | working on the query/serving surface |
| `OBL-PROD-EVIDENCE` | `docs/production-evidence-handoff.md` | REGISTER | CANONICAL | the audit items closable only with deployed-system evidence | closing a production gate |
| `OBL-RUNTIME-STATUS` | `docs/KIDS-R8-current-status-and-acceptance.md` | REPORT | CANONICAL | deployed KIDS runtime status and acceptance counts | needing deployed-system facts |
| `DOM-STATISTICAL` | `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` | REGISTER | CANONICAL | the statistical domain's decision register Q01…Q50 and its standards profile S1…S16 | before deciding anything the statistical contract already decided |

**None of these is project state.** `CTRL-CURRENT` remains the only authority for where the
rehabilitation stands (`ADOPTION` §8.1).

## Where each kind of knowledge lives

The planes are **semantic ownership, not directories**. Nothing below is a folder to create.

| Plane | Question it answers | Canonical owner | Must not own |
|---|---|---|---|
| Control | where are we, what is next, what is blocked | `CTRL-CURRENT`, `CTRL-MANIFEST` | architecture, findings, decisions, history |
| Knowledge | what is true about the architecture now | `REC-CONSOLIDATION`, `DOC-CAD`, `docs/reference/` | current state, schedule |
| Decision | what was decided and why | `docs/platform-decisions.md` (ADR-001…011), `docs/decisions/` (ADR-012+) | findings, state |
| Work | what is changing, under what scope and verification | `CTRL-WORK` + `docs/project/work/`, governance cards | lifecycle, authority |
| Governance | who owns truth, which rules and gates apply | `GOV-AGENTS`, `STD-RCP-001`, `DOC-CAD`, `CTRL-VERIFY` | project state |
| Evidence | how we know | `REC-AUDIT`, `REC-CHECKPOINT`, `CARD-RCP`, `docs/work/evidence/`, the test suites | conclusions in place of their owners |
| Implementation | the running system | `platform/`, `ops/`, migrations, tests | **project state — code is never a state authority** |

The Implementation plane is deliberately **not** catalogued artifact-by-artifact: it is
routed to by `docs/reference/CANONICAL-FULL-TREE.md`, which `/AGENTS.md` already mandates.
Cataloguing it here would create a second tree authority — the `DEF-03` defect, repeated.

## Pre-existing repository artifacts

Classified in `CTRL-ADOPTION`. Summary of what matters for navigation:

| Group | Authority | Note |
|---|---|---|
| `docs/reference/engineering/*`, `ENGINEERING-QUALITY-DOCTRINE.md`, `CANONICAL-FULL-TREE.md` | CANONICAL | M3 platform doctrine; mandated by `/AGENTS.md` |
| `docs/decisions/ADR-*.md`, `docs/platform-decisions.md` | CANONICAL | decision plane — see CF-001/CF-036 for reference defects |
| `docs/platform-capability-and-architecture-audit-2026-09-13.md` | SUPPORTING | platform baseline audit |
| `docs/work/ARCHITECTURE-IMPROVEMENT-REGISTER.md` | CANONICAL | the existing AIR register — **do not clone** |
| `docs/control-plane-and-site-schema-learning-guide.md` | SUPPORTING | sole source naming `Platform Meta-Contract`; RC-003 evidence |
| **8 × `final-*` / `*-final.md`** | authority assigned, `ADOPTION` §5 | **CF-042** — none carries a status marker in its own header |
| **13 × other currency-asserting names** | authority assigned, `ADOPTION` §7 | the rest of the CF-042 class, found by mechanical sweep |
| **13 × state-asserting names** (handoffs, status, session reports, root transcripts) | authority assigned, `ADOPTION` §8 | **CF-043 / CF-044** — four documents each claimed to be what a new session reads first |
| `docs/work/*` statistical & Access planning docs | SUPPORTING | inputs to recovery; superseded in scope by `REC-CONSOLIDATION` |

**Every document in this repository whose filename asserts currency now carries an assigned
authority**, and `rcp-verify.py` RCP-701 fails the build if a new one appears without one.

## Work

| Path | State |
|---|---|
| `docs/project/work/README.md` | the mechanism — `CTRL-WORK` |
| `docs/project/work/_TASK-TEMPLATE.md` | the item template |
| `completed/TASK-001-standards-architecture-quality-benchmark.md` | `COMPLETE` — `PHASE-002` / `GATE-STANDARDS` |
| `completed/TASK-003-phase-002-acceptance-review.md` | `COMPLETE` — adversarial acceptance review of `PHASE-002` |
| `completed/TASK-004-artifact-comparative-audit.md` | `COMPLETE` — `PHASE-003` / `GATE-COMPARATIVE` |
| `completed/TASK-002-work-stream-reconciliation.md` | `COMPLETE` — decided by ADR-014; residue handed to the statistical program |
| `completed/TASK-005-design-entry-decisions.md` | `COMPLETE` — ADR-014/015/016, `AQ-05`, design-entry safety check |
| `completed/TASK-006-canonical-design.md` | `COMPLETE` — `PHASE-004` / `GATE-DESIGN-ENTRY` |


Directories are created when the first item enters them (RCP §9 — no ceremonial folders).
Work items are indexed here for navigation only; their state authority is the item's own
`status` plus the folder it sits in, enforced by `RCP-601…603`.
`PHASE-001`'s outputs are the recovery artifacts above; they predate this mechanism and are
not retrofitted into it.

## Known catalog gaps

- The 8 `final-*` documents are dispositioned but **not yet marked** in their own headers.
  Marking them is `PHASE-011` work under CAD-13/GOV-R6, not a navigation change. Until
  then `ADOPTION` is the authority, and RCP-701 is satisfied by the register, not the file.
- `docs/reference/canonical-directory-blueprint.md` still carries its own
  *"normative and enforceable"* status line, now overridden by `ADOPTION` §7.1. The
  override is machine-checked; the stale line inside the file is not.
- ~~**Object-storage flow has no CAD-02 responsibility row**~~ — **CLOSED 2026-09-21**
  (`DEF-04`). `DOC-CAD` §2.2 carries the row and §2.3 the evidence; the flow document stays
  `SUPPORTING` (`ADOPTION` §7.1). The residual lifecycle-alignment defect is an architecture
  acceptance condition (`BM-INV-22`), not a catalogue gap.
- `docs/CONTINUATION_HANDOFF.md` still states in its own body that it is *"the
  authoritative continuation note"*. `ADOPTION` §8.2 marks it `SUPERSEDED` → `CTRL-CURRENT`;
  the override is machine-checked, the stale claim line inside the file is not (`DEF-02`
  class).
- **The Access-package work stream runs outside this lifecycle** (`ADOPTION` §8.3,
  `DEF-08`). It is governed by its own card and is not a second roadmap, but the two
  schedules are not yet reconciled. `PHASE-002` found the gap is wider than `ADOPTION` §8.3
  states — the stream is the delivery arm of the statistical-contract programme, which runs
  its own gates and owns the only domain standards profile. **CLOSED 2026-09-21 by `ADR-014`**:
  two programs, one authority plane, with the meta-level cut and a review condition. See
  `docs/project/work/completed/TASK-002-work-stream-reconciliation.md`.
- **647 373 transcript lines at the repository root are unmined** (`ADOPTION` §8.4,
  CF-044, `DEF-09`). Classified `HISTORICAL`; whether they hold unextracted durable
  knowledge is `UNVERIFIED`.
- `docs/` holds 59 files; only architecturally material ones are catalogued. Full
  classification is `CTRL-ADOPTION`'s remit.
