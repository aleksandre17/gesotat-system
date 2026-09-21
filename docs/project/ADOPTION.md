---
id: CTRL-ADOPTION
type: REGISTER
title: Existing artifact adoption mapping
status: ACTIVE
authority: CANONICAL
scope: bootstrap — retires when mapping completes
created: 2026-09-20
updated: 2026-09-21
---

# ADOPTION — mapping pre-existing artifacts into the control plane

**Bootstrap artifact.** It exists because the repository predates the protocol. It records
logical ownership and future disposition **without physically relocating anything**.

Dispositions: `KEEP_IN_PLACE` · `INDEX` · `MOVE_LATER` · `MERGE_LATER` · `SUPERSEDE_LATER`
· `ARCHIVE_LATER` · `DELETE_AFTER_MIGRATION`.

> **No knowledge loss rule (RCP §20):** nothing is moved, merged, superseded or archived
> until its unique durable knowledge has a canonical destination. Cleaner folders are not
> the goal.

---

## 1. Recovery artifacts — created by `PHASE-001`

All six already carry a canonical owner and are catalogued. Disposition `KEEP_IN_PLACE`.

They live under `docs/work/` because that is where this repository's governance protocol
(`/AGENTS.md`) places bounded work records. Moving them to `docs/project/` would break
inbound references for no navigational gain — **navigation correctness outranks folder
purity**.

## 2. Platform doctrine — mandated by `/AGENTS.md`

| Path | Authority | Disposition |
|---|---|---|
| `docs/reference/CANONICAL-FULL-TREE.md` | CANONICAL | `KEEP_IN_PLACE` |
| `docs/reference/ENGINEERING-QUALITY-DOCTRINE.md` | CANONICAL | `KEEP_IN_PLACE` |
| `docs/reference/engineering/{README,ARCHITECTURE,REQUIREMENTS,ANTI-PATTERNS,CHANGE-PROTOCOL,STANDARDS}.md` | CANONICAL | `KEEP_IN_PLACE` |
| `docs/platform-capability-and-architecture-audit-2026-09-13.md` | SUPPORTING | `KEEP_IN_PLACE` |

These are the M3 layer. The control plane routes to them; it does not restate them.

## 3. Decision plane

| Path | Authority | Disposition | Note |
|---|---|---|---|
| `docs/platform-decisions.md` | CANONICAL | `KEEP_IN_PLACE` | ADR-011 legacy surface retirement |
| `docs/decisions/ADR-legacy-surface-retirement.md` | CANONICAL | `KEEP_IN_PLACE` | ADR-011 |
| `docs/decisions/ADR-access-package-integrity-automation.md` | CANONICAL | `KEEP_IN_PLACE` | **ADR-012** — CF-001 rejected; CF-036 records migrations 111/112 citing ADR-011 for its decision D-3 |
| `docs/decisions/ADR-tenant-scoped-authorization.md` | CANONICAL | `KEEP_IN_PLACE` | **ADR-010** — also serves as the controller tenancy-exemption register |
| `docs/decisions/ADR-repository-control-protocol.md` | CANONICAL | `KEEP_IN_PLACE` | **ADR-013** — why RCP was adopted, alternatives, consequences; the decision-plane owner for the protocol itself |
| `docs/decisions/ADR-lifecycle-program-separation.md` | CANONICAL | `KEEP_IN_PLACE` | **ADR-014** — two programs, one authority plane; closes `DEF-08` |
| `docs/decisions/ADR-sdmx-conformance-boundary.md` | CANONICAL | `KEEP_IN_PLACE` | **ADR-015** — which responsibilities SDMX governs; resolves `BM-Q-01` |
| `docs/decisions/ADR-access-provider-status.md` | CANONICAL | `KEEP_IN_PLACE` | **ADR-016** — Access is a supported provider with no semantic authority; resolves `BM-Q-02` |

**Numbering.** ADR-001…011 live inline in `docs/platform-decisions.md`; ADR-012 onward are
separate files under `docs/decisions/`. Next free identifier: **ADR-017**. Identifiers are
never reused (`/AGENTS.md`, architecture integrity constitution §1).

## 4. Registers that already exist — do not clone

| Path | Authority | Owns | Disposition |
|---|---|---|---|
| `docs/work/ARCHITECTURE-IMPROVEMENT-REGISTER.md` | CANONICAL | AIR-#### improvement/debt register | `KEEP_IN_PLACE` — the doctrine forbids a second debt register |
| `docs/work/STATISTICAL-CONTRACT-IMPLEMENTATION-CHECKLIST.md` | SUPPORTING | statistical contract implementation state | `KEEP_IN_PLACE` → `SUPERSEDE_LATER` in scope by `REC-CONSOLIDATION` |
| `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` | **CANONICAL** | the statistical domain's decision register `Q01…Q50` **and its standards profile `S1…S16`** | `KEEP_IN_PLACE` |
| `docs/work/cards/<id>/governance.json` | SUPPORTING | bounded change records required by `/AGENTS.md` | `KEEP_IN_PLACE` |

Sibling `docs/work/STATISTICAL-CONTRACT-*.md` and `docs/work/ACCESS-PACKAGE-*.md` documents
share these dispositions; the members whose names assert currency are classified
individually in §9, because a group row cannot carry a per-document authority.

**`STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` is promoted to `CANONICAL` (2026-09-21, under
ADR-014).** It was covered by the group row above as `SUPPORTING` while describing itself as the
domain's design authority and holding the platform's only domain standards profile — the
`CF-043` shape, found in a register rather than in a filename. ADR-014 rule 1 establishes that a
program owns its domain profile under the shared platform profile, so the classification now
follows the rule: **canonical for the statistical domain's decisions and standards, and for
nothing else.** It states no project state, declares no rehabilitation gate, and does not
compete with `CTRL-CURRENT` or `CTRL-MANIFEST`.

## 4a. Live platform obligations — the documents that hold what is still open

Found by the CF-044 extraction audit: three documents own **open platform obligations**,
two of them previously unclassified and none reachable from the control plane. Their
filenames assert no currency, so the CF-042/CF-043 sweeps could not have caught them —
they were invisible to filename-based detection by construction.

| Path | Authority | Owns | Disposition |
|---|---|---|---|
| `docs/platform-capability-and-architecture-audit-2026-09-13.md` | SUPPORTING | **W-01…W-07** — the declared complete remaining production scope, with its dependency order `W-01 → W-02/W-04/W-05 → W-03 → W-06 → W-07` | `KEEP_IN_PLACE` |
| `docs/contract-driven-metadata-schema-agnostic-completion-plan.md` | CANONICAL | **C-01…C-14** completion plan and acceptance checklist (122 open/partial markers) | `KEEP_IN_PLACE` |
| `docs/api-modernization-capability-gap.md` | SUPPORTING | API modernization capability gaps: keyset pagination, provider capability registry, export conformance | `KEEP_IN_PLACE` |

These are **platform** obligations, not rehabilitation-lifecycle phases. `CTRL-MANIFEST`
references them under *Inherited platform obligations*; it does not restate them, and it
does not schedule them — the rehabilitation lifecycle and the platform production backlog
are separate programs that `DEF-08` will reconcile.

## 5. CF-042 — the eight unmarked `final-*` documents

Eight documents assert architectural finality and **none carries a status marker**. They
are the single largest navigation hazard in the repository, and precisely what
`CTRL-CATALOG` and this mapping exist to neutralise.

| Path | Authority | Disposition | Successor / note |
|---|---|---|---|
| `docs/final-statistical-contract.md` | HISTORICAL | `ARCHIVE_LATER` | — |
| `docs/final-unified-physical-virtual-contract.md` | SUPERSEDED | `SUPERSEDE_LATER` | → `DOC-CAD` §2 |
| `docs/final-physical-database-architecture.md` | SUPPORTING | `MOVE_LATER` | update before moving |
| `docs/unified-canonical-platform-final.md` | HISTORICAL | `ARCHIVE_LATER` | — |
| `docs/final-kids-canonical-model.md` | HISTORICAL | `ARCHIVE_LATER` | — |
| `docs/final-classifier-contract.md` | SUPPORTING | `MOVE_LATER` | update before moving |
| `docs/final-import-contract.md` | SUPERSEDED | `SUPERSEDE_LATER` | → `REC-CONSOLIDATION` PA-004 |
| `docs/final-access-control-plane-doctrine.md` | SUPPORTING | `MERGE_LATER` | merge target `DOC-CAD`; holds unique content until merged |

**Authority is assigned here and now.** Writing the marker into each file's header is
`PHASE-011` (Legacy Elimination) work under CAD-13/GOV-R6 — a content change to eight
documents, which is outside this bootstrap's permitted scope. Marking a document historical
*is* an elimination act and follows the CAD-13 sequence, which is why it belongs there and
not with the approved-plan phase (`DEF-02`).

Until then, **this table is the authority**: if a `final-*` document and this table
disagree about currency, this table wins.

## 6. Learning guides

| Path | Authority | Note |
|---|---|---|
| `docs/control-plane-and-site-schema-learning-guide.md` | SUPPORTING | **sole source naming `Platform Meta-Contract`** — primary evidence for RC-003; must not be archived without that fact having a canonical home (it has one: `REC-CONSOLIDATION` RC-003) |
| `docs/system-complete-learning-guide.md` | SUPPORTING | `KEEP_IN_PLACE` |

## 7. CF-042 — the remaining currency-asserting filenames

The `final-*` set in §5 was the *visible* part of CF-042. A mechanical sweep for the whole
class (`final` · `canonical` · `complete` · `unified` · `master` as a name token) found
thirteen more documents whose filenames assert currency or authority. **Every one is
classified below from its own content**, and several turn out not to mean what their name
implies — which is the entire point of the finding.

| Path | Authority | Disposition | Basis, from the document itself |
|---|---|---|---|
| `docs/access-driven-data-platform-master-plan.md` | HISTORICAL | `ARCHIVE_LATER` | its own title carries `(ჩანაცვლებულია)` — self-declared replaced; successor not named in-file |
| `docs/contract-plan-isolated-final-status.md` | HISTORICAL | `ARCHIVE_LATER` | names another document as its "Canonical source"; it is a dated status snapshot, not a contract |
| `docs/kids-complete-site-contract-r6.md` | HISTORICAL | `ARCHIVE_LATER` | describes revision 6; the governed revision is 8 |
| `docs/kids-legacy-canonical-control-plane-comparison-2026-09-15.md` | HISTORICAL | `KEEP_IN_PLACE` | self-declared `read-only observation`, dated 2026-09-15; retains evidence value, asserts no authority |
| `docs/work/ACCESS-PACKAGE-CANONICAL-STANDARD.md` | SUPPORTING | `KEEP_IN_PLACE` | self-declared **PROPOSED**, "nothing in it is in force until the owner approves it" — the name says canonical, the content says proposal |
| `docs/work/ACCESS-PACKAGE-MASTER-CHECKLIST.md` | SUPPORTING | `KEEP_IN_PLACE` | live checklist, `OPEN / NOT READY`; bounded by its governance card |
| `docs/kids-canonical-access-package-contract.md` | SUPPORTING | `KEEP_IN_PLACE` | describes the R8 package shape; realization authority is the generator (CAD-02) |
| `docs/api-complete-input-output-contract.md` | SUPPORTING | `KEEP_IN_PLACE` | implementation contract pinned to "KIDS revision 8 / 2026-09-13"; describes a surface it does not own |
| `docs/canonical-page-api-contract.md` | SUPPORTING | `KEEP_IN_PLACE` | describes page serving; the declaration authority is `platform.site_contract_*` (CAD-02) |
| `docs/reference/CANONICAL-FULL-TREE-DESIGN.md` | SUPPORTING | `KEEP_IN_PLACE` | explicit "companion დოკუმენტი" to the mandated `CANONICAL-FULL-TREE.md` |
| `docs/reference/canonical-directory-blueprint.md` | SUPPORTING | `MERGE_LATER` | **see the CAD-01 note below** |
| `docs/reference/CANONICAL-OBJECT-STORAGE-FLOW.md` | SUPPORTING | `KEEP_IN_PLACE` | **see the registry-gap note below** |
| `docs/learning/00-foundation/03-canonical-naming.md` | SUPPORTING | `KEEP_IN_PLACE` | 13-line learning note on the `__raw_`/`__ent_` transport prefixes |

### 7.1 Two findings this sweep produced

**CAD-01 exposure — two normative directory authorities.**
`docs/reference/canonical-directory-blueprint.md` declares itself *"Status: normative and
enforceable"* over *"the complete … platform repository"*. `/AGENTS.md` mandates
`docs/reference/CANONICAL-FULL-TREE.md` for the same responsibility. That is the CAD-01
violation shape — one responsibility, two documents each readable as binding.

**Resolution, decided here:** `CANONICAL-FULL-TREE.md` is the canonical authority, because
the operating constitution routes to it. The blueprint is reclassified `SUPPORTING`, and
its unique content (the one-physical-owner invariant, the no-junction/no-vendored-duplicate
rules) merges into the canonical tree under `PHASE-008`. Until that merge, **this row is
the authority**, and the blueprint's own status line is superseded by it.

**Registry gap — object storage has no CAD-02 row. CLOSED 2026-09-21 (`DEF-04`).**
`CANONICAL-OBJECT-STORAGE-FLOW.md` is the only document describing object-storage flow, and
CAD-02 contained no responsibility row for it. Assigning it `CANONICAL` here would invent an
authority the recovery never verified, so it stayed `SUPPORTING` and the gap was recorded
instead: **object-storage flow was an unregistered responsibility**, to be classified against
CAD-02 in `PHASE-002`. Recording the gap was the honest outcome; silently promoting the
document was not.

`PHASE-002` closed it by classification: `DOC-CAD` §2.2 now carries the row *Stored artifact
bytes & content identity*, with the producer, ports, correspondence mechanism and the
residual lifecycle-alignment defect, and the reasoning in `DOC-CAD` §2.3. **This document's
row is unchanged** — the flow document remains `SUPPORTING`, because a document that
describes a mechanism is not that mechanism's authority (CAD-07).

## 8. CF-043 / CF-044 — documents that assert current state or continuation

§5 and §7 covered filenames asserting *finality*. This section covers the sibling class
found during the durable-knowledge closure: filenames and opening lines asserting **current
state or continuation authority** — what a new session reads first.

### 8.1 The collision

Four documents claimed that role over partly overlapping scope, and none referenced the
others:

| Document | Its own claim |
|---|---|
| `docs/project/CURRENT.md` | *"The single current-state authority for this repository."* |
| `docs/CONTINUATION_HANDOFF.md` | *"the authoritative continuation note for the next session. Read it before changing platform code or databases."* |
| `docs/production-evidence-handoff.md` | *"This document is the canonical handoff …"* |
| `docs/KIDS-R8-current-status-and-acceptance.md` | *"is the current runtime status authority for KIDS and supersedes older R7 execution notes and handoff files."* |

**Resolution, decided here.** `CTRL-CURRENT` is the canonical authority for **repository
project state** — which phase, which gate, what is next, what is forbidden — by ADR-013 and
`/AGENTS.md` routing. The other three do **not** all collapse into it, because two of them
own genuinely different responsibilities:

- **Deployed runtime status** (counts, lifecycle state of a deployed KIDS after migrations
  071/072) is not project state. `KIDS-R8-current-status-and-acceptance.md` keeps it.
- **Production evidence obligations** (ten audit items closable only with runtime proof)
  are not project state either. `production-evidence-handoff.md` keeps them.
- **Continuation instructions** *are* project state, and that is `CTRL-CURRENT`'s. The
  2026-09-09 continuation note is therefore superseded, not re-scoped.

Retaining two documents with narrowed scope is the correct outcome, not a compromise:
collapsing them into `CURRENT` would put runtime facts and evidence obligations into the
control plane, which `CTRL-README` forbids.

### 8.2 Dispositions

| Path | Authority | Disposition | Basis |
|---|---|---|---|
| `docs/CONTINUATION_HANDOFF.md` | SUPERSEDED | `SUPERSEDE_LATER` | → `CTRL-CURRENT`. Its authority claim is dated 2026-09-09 and is now false; this row overrides it |
| `docs/NEXT_SESSION_HANDOFF.md` | HISTORICAL | `ARCHIVE_LATER` | same class, no explicit claim line |
| `docs/KIDS-R8-current-status-and-acceptance.md` | CANONICAL | `KEEP_IN_PLACE` | **scope narrowed to deployed KIDS runtime status.** Not project state, not architecture |
| `docs/production-evidence-handoff.md` | CANONICAL | `KEEP_IN_PLACE` | **scope narrowed to the ten production-evidence gate items.** Not project state |
| `docs/work/ACCESS-PACKAGE-HANDOFF.md` | SUPPORTING | `KEEP_IN_PLACE` | live entry point for the Access-package work stream; see §8.3 |
| `docs/work/HANDOFF-2026-09-19.md` | HISTORICAL | `KEEP_IN_PLACE` | self-declared *"summary, not a normative source"*; retains runtime evidence value |
| `docs/work/HANDOFF-CLAUDE-TO-CODEX-2026-09-17.md` | HISTORICAL | `KEEP_IN_PLACE` | self-declared non-normative session-close report |
| `docs/work/PLATFORM-STATE-2026-09-17.md` | HISTORICAL | `KEEP_IN_PLACE` | self-declared *"snapshot (არა normative authority)"* |
| `docs/work/STATISTICAL-CONTRACT-SESSION-2026-09-19.md` | HISTORICAL | `KEEP_IN_PLACE` | dated session report; `dev works / Release NOT READY` |
| `docs/evidence/snapshot-binding-runtime-2026-09-15.md` | SUPPORTING | `KEEP_IN_PLACE` | dated runtime evidence record |
| `codex-session-01a087dd-4b78-7e03-9786-72e05800e68a.md` | HISTORICAL | `ARCHIVE_LATER` | raw transcript, 505 677 lines — see §8.4 |
| `codex-session-01a0b347-c527-79c3-bdfc-887f28ebd941.md` | HISTORICAL | `ARCHIVE_LATER` | raw transcript, 69 614 lines |
| `codex-session-01a0b347-c527-79c3-bdfc-887f28ebd942.md` | HISTORICAL | `ARCHIVE_LATER` | raw transcript, 72 082 lines |

### 8.3 Work streams outside the RCP lifecycle

`ACCESS-PACKAGE-HANDOFF.md` and `ACCESS-PACKAGE-MASTER-CHECKLIST.md` drive an
**in-flight work stream that is not a phase of the rehabilitation lifecycle**. This is
recorded rather than resolved: the lifecycle in `CTRL-MANIFEST` covers rehabilitation, and
the Access-package line predates it and runs under its own governance card
(`docs/work/cards/access-package-composer/governance.json`).

It is **not** a second roadmap — it schedules no rehabilitation phase and declares no gate
over one. Reconciling the two work streams is `PHASE-002` input (`DEF-08`).

### 8.4 CF-044 — the transcripts

Three raw AI conversation transcripts, **647 373 lines**, sit at the repository root. They
are the anti-pattern RCP §22 and §14 name directly.

**Knowledge-preservation audit: CLOSED 2026-09-20.** The transcripts were decomposed by
turn role, reduced to 164 substantive unique user turns, read in full, and 22 durable items
were extracted and tested against the repository. **All 22 have canonical owners; 0 are
unowned.** Method and the full item→owner table: `REC-CONSOLIDATION` §1.4a.

The transcripts are therefore **not required for correct continuation**. They keep
`HISTORICAL` authority and `ARCHIVE_LATER` disposition — archival is now ordinary legacy
elimination under `PHASE-011` rather than a blocked action, because the knowledge it would
remove has been proven to exist elsewhere.

**They are still not deleted in this closure**, and that is deliberate: archival is a
CAD-13 elimination act with its own sequence, not something to do opportunistically inside
a governance task.

## 9. Not adopted in this bootstrap

`platform/`, `ops/`, `samples/`, `artifacts/` — implementation and evidence planes.
Governed by `/AGENTS.md` and the canonical tree; they need no control-plane mapping until a
work item touches them.

Remaining uncatalogued `docs/*.md` (roughly 35 files) are SUPPORTING reference material
with no authority claim. They are `KEEP_IN_PLACE` and will be classified on demand — **no
file is at risk of being treated as canonical**, because canonical status now requires an
explicit entry in `CTRL-CATALOG`.

## 10. Retirement of this artifact

`CTRL-ADOPTION` retires when every material artifact carries its own metadata header and
`CTRL-CATALOG` alone is sufficient for navigation. Bootstrap complexity is not preserved
permanently (RCP §20).
