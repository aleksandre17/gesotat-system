---
id: REC-PASS3
type: REPORT
title: PASS 3 closure: protection, falsification, design handoff
status: COMPLETE
authority: CANONICAL
scope: protection requirements, clean-build proof spec, temporary architecture, design handoff
owner: architecture recovery
created: 2026-09-20
updated: 2026-09-20
---

# GEOSTAT API — PASS 3 Closure: Protection, Falsification, Design Handoff

**Created:** 2026-09-20. Additive under GOV-R9. Supersedes nothing.
**Baseline:** `HEAD b46c043`, target **26 modified / 29 untracked** — unchanged since batch 1.

**Owns:** protection requirements, the clean-build proof specification, the temporary
architecture register, document dispositions, and the Recovery → Design handoff.
**Does not own:** findings (Consolidation §1), authority (Doctrine CAD-02), coverage
(Layer Coverage Ledger), evidence (Checkpoint, Completeness Audit).

---

## 1. Protection requirements

Each row converts a recovered risk into the protection that must exist **before** the
corresponding migration. **Protection defends the invariant, not the current
implementation.** Nothing here is implemented.

| Risk | Protection required | Mechanism |
|---|---|---|
| **RC-001** two liveness authorities | serving resolves through publication membership only; a snapshot `PUBLISHED` without membership is impossible | DB constraint or trigger · **reconciliation** · architecture fitness test |
| **RC-002** migrations carry declaration duty | no new semantic declaration may enter via migration once a producer exists | **repository check** (migration lint) · manual governance gate |
| **RC-003** missing upper stack | every canonical registry has exactly one governed producer | **architecture fitness function** over CAD-02 · repository check |
| **CF-018** identity by string + latest | every code lookup is scoped and version-pinned; no `ORDER BY … DESC` as identity resolution | **static analysis** · unit + negative tests |
| **CF-023** publication scope ≠ gate scope | published set ≡ gate-evidenced set ≡ membership | **integration test** (publish one, assert one) · runtime integrity check · alert |
| **CF-031** chart data bypasses ServingPolicy | every observation read passes the serving authority check | integration test · architecture fitness test |
| **CF-032** governed publication closed to statistical | a statistically-loaded dataset is publishable through the governed path | integration test · contract test |
| **CF-033 / CF-034** package defines canonical metadata | a package may reference a metadata schema, never define or approve one | **DB constraint** (producer-scoped write) · integration test · static analysis on `state()` |
| **CF-038** provider limits, three authorities | the envelope is read from the capability registry only | architecture fitness test · static analysis (forbid envelope properties) |
| **CF-039** untyped entity properties | declared entity type/nullability/uniqueness enforced below the contract | **DB constraints** · contract test · migration test |
| **CF-040** cardinality unenforced | declared cardinality holds in storage | **DB unique constraint** · property test |
| **CF-041** outbox carries three categories | intents, domain events and audit evidence are separable and an intent cannot be read as evidence of effect | contract test · reconciliation · observability |
| **CF-042** (new, §4) documents look equally current | no two documents assert current authority over one responsibility | **repository check** (status-marker lint) · manual gate |
| **QF-012 / INV-014** | an assertion binds to the exact schema revision it was validated against | **DB FK to the exact revision** · PATTERN C (digest + write-once) · unit test |
| **DR-004** contract linkage | a `STATISTICAL` site-contract dataset references an approved statistical revision by digest | `ContractApprovalCheck` (existing pluggable mechanism) · contract test |
| **DR-005** grammar parity | schema and parser accept exactly the same documents | **bidirectional conformance corpus in CI** · build check |
| **DR-006** numeric envelope | declaration ≤ provider capability; storage a strict superset; aggregation cannot overflow silently | DB type + capability check · property test on `SUM` |
| **DR-007** package correspondence | package content digest ≡ approved revision plan digest before any canonical influence | **generated-artifact verification** · integration test |
| **DR-008** physical pattern | realization follows a declared pattern; no site literal in a generic generator | **static analysis** (site-literal lint) · fitness test |
| **AMS-001** atomic set | no member ships alone | manual governance gate · migration test |
| **CAD-01/12/16** anti-parallelism | one responsibility, one authority | **architecture fitness function over CAD-02** — the single highest-leverage protection |

**Highest-leverage protections, in order:** (1) a fitness function over CAD-02, because it
defends every row at once; (2) membership-vs-status reconciliation, because it makes RC-001
*detectable*, which PASS 2B proved it currently is not; (3) the grammar conformance corpus,
because drift is otherwise silent.

---

## 2. Clean-build ↔ upgraded-database proof specification

Resolves PASS 2B's `UNPROVEN`. **Specification only — no execution, no data mutation.**

**Existing input:** `ops/tests/sql/migration-chain-fresh-replay.sh` (cited by the checksum
allowlist as proof for one class of correction). No evidence of a current passing run was
found.

**Required proof:** build database **A** from a clean checkout via the registered chain;
take database **B** as a full historical upgrade. Compare:

| Category | Compared |
|---|---|
| Schema objects | tables, views, procedures (none currently), triggers — including `tr_statistical_reference_definition_immutable` |
| Columns | name, ordinal, type, precision/scale, nullability, defaults |
| Constraints | PK, UK, FK (+ cascade), CHECK — **absence is also compared**: `dataset_snapshot.status` and `observation_status` must both lack a CHECK in A and B, or the difference is real |
| Indexes | name, columns, uniqueness, filter |
| Seed / reference data | only where authoritative: `classification_*`, `provider_capability`, `metadata_namespace` |
| Migration ledger | `platform.schema_migration` id + checksum set |
| **Migration-produced declarations** | `site_contract_*`, `contract_page_binding`, `ingestion_contract_revision`, `data_product` — the RC-003 set |

**Unregistered migrations must be handled explicitly, not ignored.** Six exist: `000`
(bootstrap, legitimately outside), `037`, `039`, `045`, `047`, and **`056_activate_kids_r8_supersede_legacy.sql`**.

For each, the proof must state: *does B contain its effect, and does A?* Expected today:
**A lacks the `056` activation while B has it** — that is the divergence, and the proof
exists to make it explicit rather than assumed.

**`alreadyEffective()` must be exercised deliberately**: the probe adopts a migration
without executing it when its effect is already present, so **UNREGISTERED ≠ NEVER
EFFECTIVE**. The proof must distinguish *effect present via adoption* from *effect present
via execution*.

**Status: PROTECTION REQUIRED — EXECUTION PENDING.** Does not block recovery: the
divergence mechanism is understood (CF-020, RC-002); only its exact extent is unmeasured.

---

## 3. NEW CF-042 — eight documents assert finality, none carries a status marker

FACT. `docs/` holds 59 markdown files. Fourteen carry `final` / `canonical` / `complete` /
`unified` in their names. Eight of those assert architectural finality over overlapping
responsibilities:

`final-statistical-contract.md` · `final-unified-physical-virtual-contract.md` ·
`final-physical-database-architecture.md` · `unified-canonical-platform-final.md` ·
`final-kids-canonical-model.md` · `final-classifier-contract.md` ·
`final-import-contract.md` · `final-access-control-plane-doctrine.md`

FACT. **None of the eight carries a status marker** in its opening lines — no `SUPERSEDED`,
no `HISTORICAL`, no `STATUS:`, no successor link. **All eight look equally current.**

§7's decisive question — *could a future developer or AI reasonably choose the wrong
architectural authority because documentation makes two answers appear current?* —
**YES, unambiguously.**

Distinct from CF-029 (a migration documenting absent behaviour) and CF-036 (a migration
citing the wrong ADR): this is **document-level authority ambiguity**, with its own
disposition path. Checkpoint §12 recorded four coexisting "final" documents; this measures
eight with zero markers.

- Risk: **HIGH for a rehabilitation executed by agents** — CAD-16 requires an agent to find
  the canonical authority, and the documentation actively defeats that.
- Status: ARCHITECTURAL-DEFECT (documentation authority). **Identified and bounded**, which
  is what §7 requires.

### 3.1 Governed dispositions

| Document | Disposition | Basis |
|---|---|---|
| `final-statistical-contract.md` | **HISTORICAL** | KIDS-specific; superseded by the statistical contract subsystem (DR-004) |
| `final-unified-physical-virtual-contract.md` | **SUPERSEDE** → CAD-02 | claims unified authority the registry now holds |
| `final-physical-database-architecture.md` | **UPDATE** | schema truth is the migration chain + Layer Coverage |
| `unified-canonical-platform-final.md` | **HISTORICAL** | checkpoint records it as explicitly replaced |
| `final-kids-canonical-model.md` | **HISTORICAL** | site-specific |
| `final-classifier-contract.md` | **UPDATE** | classifier authority is CF-016 + PATTERN B |
| `final-import-contract.md` | **SUPERSEDE** | seven ingestion mechanisms; PA-004 holds the truth |
| `final-access-control-plane-doctrine.md` | **MERGE** → CAD doctrine | overlaps CAD-01…CAD-18 |
| `control-plane-and-site-schema-learning-guide.md` | **KEEP — SUPPORTING** | the only source naming `Platform Meta-Contract`; RC-003's evidence |

**No document is deleted.** Dispositions are obligations for the elimination phase under
CAD-13.

---

## 4. Five required-but-missing canonical authorities — bounded

§5 requires proving each is *bounded*, not designed.

| # | Authority | Must govern | Must NOT govern | Producers known | Consumers known | Legacy substitute | Invariants | Why bounded, not unknown |
|---|---|---|---|---|---|---|---|---|
| 1 | **Control Plane Schema** (contract creator) | creation + approval of site-contract declarations | dataset semantics (DR-004), physical realization | none | composer, serving, query, introspection, compatibility | KIDS seed migrations | INV-001, INV-002, CAD-01 | responsibility named in the repository's own learning guide; consumers all built and tested (CF-024b); approval half already exists |
| 2 | **Site declaration producer** | `site_contract_revision/dataset/field/relation/classifier`, `contract_page_binding` | approval (exists), statistical semantics | none | same as above | migrations 022/023/054/055/057 | INV-001, SCH-003 | shape proven by `ContractWorkflow`; target schema fully known |
| 3 | **Product registration** | `platform.data_product` identity + tenant binding | contracts, datasets | none (`ProductTenancyRepository` writes tenant key only) | everything | migrations | INV-009 | smallest of the five; schema and tenancy semantics verified |
| 4 | **Classifier promotion** | `classifier_proposal` → `classification_scheme/version/item/hierarchy` | proposal import (exists, correct) | none | serving, charts, statistical dimensions | migrations | INV-003, INV-004 | **PATTERN B proves the upstream half**; only promotion is absent |
| 5 | **Provider capability producer** | precision, scale, column/name limits, page size per provider+family | domain semantics | none (`ProviderCapabilityDiscoveryRunner` reads, fails closed) | compiler, physical planner | Spring properties, planner constants | DR-006, CF-038 | required dimensions enumerated by DR-006; existing table + reader present |

**All five are bounded.** Each has a named responsibility, a known consumer set, a known
legacy substitute, known invariants and a known target schema. **None requires
architectural guessing.** §5's failure condition is not met.

---

## 5. Temporary architecture register

Every temporary mechanism needs an exit condition. **An unbounded temporary mechanism is
permanent accidental architecture.**

| Mechanism | Reason | Exit condition | Status |
|---|---|---|---|
| Statistical loader bypassing the run pipeline | delivered before the pipeline covered statistical families | statistical load runs as a package-run stage with gate evaluation | **EXIT CONDITION NOW SET** (was unbounded) |
| Migrations as declaration producer | no producer exists | Control Plane Schema producer exists and migrations stop declaring | bounded by RC-003 |
| Checksum allowlist (10 entries) | pre-release corrections | each entry retired as its migration passes fresh replay | **UNBOUNDED — no per-entry expiry** |
| `state()` package→canonical lifecycle mapping | no metadata producer | metadata schemas become producer-governed (CF-033) | bounded by DR-007 |
| Legacy surface gates (4 families) | live consumers remain | `geostat-system-app` migrated off `/test/*` and `/import/mssql-to-access` | bounded by INV-012 |
| `dataset_snapshot.status` as liveness | historical | membership becomes sole authority (RC-001) | bounded by AMS-001 |

**One unbounded item found: the checksum allowlist.** Ten entries with justifications but
**no expiry and no per-entry retirement trigger** — it grows by editing Java. Recorded as a
temporary-architecture debt requiring an exit condition, consistent with CF-012's revised
statement.

---

## 6. Orphan capability sweep — final

| Capability | Classification |
|---|---|
| `service/platform/packaging/*` (composer) | **INTENDED FUTURE CANONICAL** — DR-003, blocked by CF-024a |
| `statistical-contract-draft.schema.json` | **REQUIRED-BUT-UNWIRED** — DR-005 canonical shape authority |
| `SdmxCsvExporter` | **REQUIRED-BUT-UNWIRED** — blocked by CF-005 |
| `platform.classifier_proposal` rows | **REQUIRED-BUT-UNWIRED** — no promotion path (CF-016) |
| `PageDataAdapterRegistry` | **ABANDONED** — except `ReadContext`, which is live |
| `relational` ENTITY adapter | **ABANDONED** — dead code |
| `CarsMigrationRunner` | **ABANDONED** — shadow bypass surface (CF-037) |
| `PageFamily.ROOT` | **ABANDONED** — enum constant with no adapter |
| `contract_revision_lifecycle` (migration 084) | **ABANDONED** — not written by the approval path |
| `'APPROVED'` / `'PREPARED'` snapshot statuses, `ROLLED_BACK` | **ABANDONED** — dead vocabulary |

**No `UNKNOWN` orphan remains.** The pattern is consistent and worth naming for design:
**four orphans are correct components awaiting a missing producer**, not mistakes —
exactly RC-003's signature.

---

## 7. Cross-layer invariants

| Invariant | Status |
|---|---|
| approved contract revision → generated artifact revision | **MISSING** (DR-007, no digest) |
| statistical revision → site contract binding | **MISSING** (CF-006 / DR-004) |
| ingestion → exact contract revision | **ENFORCED** (`dataset_version`, `revisionDigest`) |
| canonical record → provenance | **ENFORCED** (`source_record_id`, artifact checksum) |
| snapshot → exact membership | **CONTRADICTED** (CF-023) |
| publication → exact snapshot membership | **CONTRADICTED** (CF-023) |
| serving → publication authority | **CONTRADICTED** (CF-021, CF-022) |
| rollback → publication authority | **CONTRADICTED** (partial restore) |
| metadata assertion → exact schema revision | **MISSING** (QF-012 / INV-014) |
| provider artifact → contract + pattern + generator + capability | **MISSING** (REQ-025/026, DR-007) |
| archive record → payload | **VALIDATED** (repair service) |
| tenant identity → data scope | **ENFORCED**, one leak risk (CF-018 second instance) |

**Every relationship is known.** Four enforced/validated, five contradicted, four missing —
none unknown.

---

## 8. Information-loss final challenge

PASS 2B introduced **no new semantic loss axis**. Checked: retry (CAS/key-guarded, no
value mutation) · resume (from the immutable artifact) · rollback (loses no data; leaves
`status` residue — state divergence, already CF-023) · cache (snapshot-keyed, derived) ·
provider conversion (bounded by DR-006) · object-store lifecycle (convention-only alignment
— an *availability* risk, not semantic loss) · archive purge (retention policy, intentional)
· tenant resolution (CF-018 instance — identity, already counted) · migration upgrade
(state divergence, CF-020) · parallel ingestion (adapters, no new transformation).

**Stated explicitly: no new loss axis exists.** No finding inflated.

---

## 9. Failure-injection — adversarial reasoning

| Failure | Prevents corruption | Detects | Recovers | Known defect | Protection required |
|---|---|---|---|---|---|
| Die mid-ingestion | data-plane tx | batch `FAILED` | `resumePackage` | — | — |
| Die after commit, before outbox | intent committed first; `release_id` idempotent | outbox attempts | retry | — | — |
| Duplicate command | CAS claim | attempts | dedup | — | — |
| Two nodes, one scheduler | 6 leases + CAS + guards | — | — | — | — |
| Publication fails halfway | one data-plane tx | — | — | **set is wrong to begin with** | CF-023 test |
| Rollback races publication | `publication.snapshot` CAS | — | converges | `status` residue | reconciliation |
| Provider generation fails | in-memory, nothing emitted | operation fails | — | — | — |
| Object store OK, DB fails | — | integrity audit | sweep/repair | — | — |
| DB OK, object store down | — | integrity audit | repair | — | — |
| Migration interrupted | ledger per-migration | fail-closed checksum | re-run | — | fresh-replay proof |
| Stale contract revision used | `approvedPlan` digest check | `DIGEST_MISMATCH` | re-approve | availability (CF-004) | — |
| Wrong tenant supplied | `TenantAccessGuard` at data access | denial without oracle | — | CF-018 instance | static analysis |
| Legacy ingestion path invoked | `@LegacySurfaceGate` default-off | metric key | — | — | — |
| **Obsolete document followed by a future AI** | **nothing** | **nothing** | **nothing** | **CF-042** | status-marker lint |

**The last row is the one with no defence at all** — and it is the failure mode most likely
to recur in an agent-executed rehabilitation.

---

## 10. Recovery → Design handoff

**PROVEN.** Six architecture generations coexist. Authority chain M3→M0 intended, top two
levels unimplemented (RC-003). Two liveness authorities (RC-001). Migrations are the only
declaration producer (RC-002). Statistical family fully typed and digest-protected; entity
properties untyped (CF-039). Access generation is contract-driven and site-literal-free;
the return path validates the package against itself, never against the contract. Seven
ingestion mechanisms, four chart mechanisms, two grammars — all classified.

**REQUIRED.** One canonical authority per responsibility (CAD-01). Membership as sole
liveness authority. Typed canonical storage for every family. Exact-revision binding
everywhere identity is resolved. Package correspondence by deterministic digest. Governed
producers for the five missing authorities. Per-combination constant semantics (CF-015).
Governed reprocess-under-corrected-revision.

**FORBIDDEN.** A transport artifact defining or approving canonical semantics (DR-007). A
derived plan persisted as authority (INV-013). Site literals in a generic generator
(DR-008). Configuration as semantic authority (CF-038). A new mechanism beside an existing
authority (CAD-16). Weakening `assertApprovedContract` to unblock publication — explicitly
rejected during AMS-001 falsification.

**REQUIRED-BUT-MISSING.** The five authorities of §4 · per-combination constants ·
typed entity properties · cardinality enforcement · assertion↔revision binding ·
package↔contract correspondence · governed reprocessing · legacy chart converter ·
contract withdrawal · grammar parity corpus · generator identity in artifacts ·
membership↔status reconciliation (unnecessary if RC-001 is resolved by removal).

**LEGACY THAT MUST NOT SHAPE THE TARGET.** `dataset_snapshot.status` as liveness · the
43-measure KIDS model · carrier/DOUBLE statistical model · Managed v1 + four import
strategies · core `ChartDefinition` · migrations as declaration producer.

**EXTERNAL COMPATIBILITY TO PRESERVE.** Microsoft Access as a required round-trip provider
(it sets the 28-precision ceiling) · the R8 package and its published snapshot until parity
is proven (§14 item 11) · `frontend/kids` on `/platform/pages/` · `geostat-system-app` on
`/test/*` and `/import/mssql-to-access` until migrated · SDMX 3 multi-measure layout.

**MUST BE AUTOMATED.** Contract→physical realization · authoring organisation ·
revision/digest propagation · package correspondence verification · authority-boundary
reconciliation · clean-build/upgrade verification · grammar parity.

**MUST REMAIN EXPLICIT HUMAN SEMANTICS.** Contract authoring · four-eyes approval ·
classifier promotion · publication approval · retirement decisions · the data an author
types into the package.

**PROTECTION BEFORE MIGRATION.** §1, with the CF-023 integration test and the CAD-02
fitness function first — the publication writer, service and store are entirely untested
today.

**TRUE DESIGN QUESTIONS** (not recovery unknowns): the authoring input format for the
Control Plane Schema producer · whether the physical pattern is declared per family or per
provider+family · whether entity typing is per-family physical tables or a typed-column
projection · the promotion workflow shape for classifiers · whether `metadata_schema`
merges into the contract grammar or stays a separate registry.

---

## 11. CAD-18 — the mandatory two-status distinction

| # | Condition | Recovery knowledge | Repository compliance |
|---|---|---|---|
| 1 | canonical authority identified per responsibility | **PASS** — CAD-02, 5 marked † | FAIL |
| 2 | parallel mechanisms classified | **PASS** | PASS |
| 3 | competing authorities eliminated | **PASS** (known) | FAIL |
| 4 | temporary paths have exit conditions | **PASS** (§5; one unbounded item named) | FAIL |
| 5 | compatibility justified | **PASS** | PASS |
| 6 | historical artifacts non-authoritative | **PASS** (§3) | FAIL |
| 7 | obsolete docs not current | **PASS** (dispositions set) | FAIL |
| 8 | obsolete tests classified | **PARTIAL** — ~62 unclassified | FAIL |
| 9 | migrations not mistaken for authority | **PASS** | FAIL |
| 10 | generated artifacts traceable | **PASS** (known missing) | FAIL |
| 11 | runtime agrees with authority | **PASS** (known divergences) | FAIL |
| 12 | contradiction sweep passes | **PASS** — 1 benign classified | PARTIAL |

**CAD18_RECOVERY_KNOWLEDGE_STATUS: PASS** (11 of 12; condition 8 partial and cannot change
canonical design — test classification is protection-coverage work).

**CAD18_REPOSITORY_COMPLIANCE_STATUS: FAIL** — expected. Rehabilitation has not run.

---

## 12. The final adversarial question

*What material fact could we still discover tomorrow that would force a material redesign
rather than refinement?*

Searched candidates, and why each does not qualify:

| Candidate | Why not |
|---|---|
| An undiscovered producer for a canonical registry | falsified on five independent axes (PASS 1); would *narrow* RC-003, not redesign |
| A hidden second serving path | dispatch fully read; all 7 adapters identical; no site branch |
| Fresh-replay revealing wider divergence | would change *extent*, not architecture; mechanism known (CF-020) |
| An external consumer of a legacy surface | would change elimination sequencing, not canonical design |
| `metadata_schema` proving to be the intended Control Plane Schema | physically refuted — 2 rows, shape registry only |
| Entity typing being deliberate | no ADR, no requirement, no evidence; contradicts REQ-006/007/008 |
| The R8 metadata-plane producer existing somewhere | exhaustive behavioural search: only 2 classes create Access tables |

**No concrete unresolved category remains** that would force redesign. What remains is
design choice, implementation defect, migration work, protection execution and legacy
elimination.

---

## 13. Root cause falsification — final

**RC-001 SURVIVED.** No trigger, no constraint, no reconciliation job, no scheduled
comparison; a known writer actively breaks equivalence. Re-challenged: could
`dataset_snapshot.status` be a legitimate *derived* representation rather than an
authority? No — three readers consult it **instead of** membership, which is the definition
of an authority.

**RC-002 SURVIVED.** Declarations hand-authored (0 of 113 generated markers, no generator);
`alreadyEffective()` makes the record tolerant rather than authoritative.

**RC-003 SURVIVED.** Five axes; strengthened twice (provider capability as a fourth
registry; `data_product` as a fifth responsibility). Re-challenged: is RC-003 merely a
symptom of RC-002? **No — the reverse.** Migrations carry declarations *because* the
producing layer was never built. RC-002 is the symptom; RC-003 is the cause.
