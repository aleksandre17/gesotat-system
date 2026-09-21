---
id: REC-AUDIT
type: EVIDENCE
title: Final recovery completeness audit
status: COMPLETE
authority: SUPPORTING
scope: per-finding verification evidence and per-pass coverage statements
owner: architecture recovery
created: 2026-09-20
updated: 2026-09-20
---

# GEOSTAT API — Final Recovery Completeness Audit (pass 1)

**Date:** 2026-09-20
**Mode:** independent adversarial pass. Read-only for all implementation artifacts.
**Baseline audited:** `GEOSTAT-API-RECOVERY-CONSOLIDATION-2026-09-20.md`
**Evidence trail:** `GEOSTAT-API-ARCHITECTURE-RECOVERY-CHECKPOINT-2026-09-20.md`

**Audit verdict up front: this pass is PARTIAL, not PASS.** It closed the baseline's
self-declared weakest link and found one new reusable pattern, but it did **not** cover the
majority of the 20 audit dimensions the phase specification requires. Section 4 states
exactly what was and was not audited, so no later reader mistakes silence for coverage.

---

## 1. CF-004 REOPENED AND CLOSED — inference upgraded to FACT

The consolidation flagged CF-004's closure as resting on one declared inference:
`semanticForm()` and `physicalForm()` had never been read line by line. That inspection was
performed.

### 1.1 What the revision digest actually binds — FACT

`StatisticalContractCompiler:119-164`, read directly:

```text
semanticForm(plan):
  profile        plan.profileRef().wire()
  dataset        datasetNamespace + ":" + datasetCode
  structure      plan.structureRef().wire()
  errorMode      plan.errorMode()
  policies       policyRefs -> wire()          (sorted + distinct at line 86)
  dependencies   plan.dependencies() -> wire() (the RESOLVED reference closure)
  components     componentForm(each)

componentForm(c):
  code · role · position · required
  concept        c.conceptRef().wire()
  representation representationForm(c.representation())
  measure        c.measureRef().wire()   (null-safe)
  unit           c.unitRef().wire()      (null-safe)
  aggregation    c.aggregation()
  attachment     level + dimensions.sorted() + measure
  constant       c.constantValue()
  overridable    c.overridable()

representationForm(r):
  type                                   (logicalType)
  Coded        -> codelistRef().wire()
  TimePeriod   -> formats sorted
  Numeric      -> precision, scale
  IntegerRange -> min, max
  BoundedText  -> maxLength

physicalForm(physical):
  provider · tables[ name, generatedRowRef, columns[ name, component ] ]

revision = semanticForm + captions(sorted) + physicalForm
```

### 1.2 Falsification attempts — all four failed

| Attempted falsification | Result |
|---|---|
| **Omitted semantics** | None found. Concept, representation (including codelist reference *with version*), measure, unit, aggregation, attachment, constant, overridable, role, position and required are all bound. Numeric precision **and scale** are bound — which also ties DR-006 into the digest. |
| **Unstable ordering** | Refuted. `position` is explicit and the source comment states "Component order is semantic (it orders the key)"; attachment dimensions and TimePeriod formats are `.sorted()`; policies are `.sorted().distinct()`; `normalise()` makes "everything derivable explicit here, once". |
| **Provider-dependent inputs escaping the digest** | Refuted. `physicalForm` binds provider code and the full table/column layout, so any capability change that alters the plan changes the digest and is detected; one that does not alter it cannot change meaning. |
| **Mutable registry dependency — a reference definition changed in place without a version bump** | **This was the one real residual, and it is closed at the database layer.** Migration `108_statistical_reference_definition_digest.sql` creates `platform.tr_statistical_reference_definition_immutable`, an `AFTER UPDATE` trigger on `platform.statistical_reference` that `THROW 51204, 'The definition of a statistical reference is written once'` whenever `definition_digest` would change. In-place mutation is impossible. |

### 1.3 Verdict

**CF004_VERIFICATION: PASS — CONFIRMED, and upgraded from INFERENCE to FACT at both
layers.**

The chain *authoritative inputs → compiler → semanticForm/physicalForm → digest →
approvedPlan recompilation → drift detection* binds every semantically relevant input.
There is **no path by which an approved statistical contract's meaning can drift
undetected**. CF-004's scope remains exactly what batch 8 narrowed it to: an
**availability** defect (a superseded reference makes an approved contract stop compiling),
never a semantic-integrity one.

The consolidation's §17 warning — "if `semanticForm()` omits a semantic field, the 'no
silent reinterpretation' conclusion weakens" — is discharged. It did not omit one.

### 1.4 NEW FINDING — a third proven good pattern

**PATTERN C — defence in depth for immutable semantic identity:** application-level content
digest (`CanonicalJson.digest` over a canonical form) **plus** a database trigger enforcing
write-once on the same digest.

This is a genuinely strong pattern and the consolidation did not record it. It is directly
reusable for the components the recovery found unprotected — notably INV-014 (assertion ↔
schema revision binding) and DR-007's correspondence requirement, both of which need
exactly this shape.

**Recorded as PATTERN-C in the consolidation's §3.** It joins Pattern A (deterministic
generation) and Pattern B (proposal → governed promotion). Three proven patterns now exist
in-repository for problems the plan must solve; none requires invention.

---

## 1A. PASS 2 — inherited findings, RC falsification, producer sweep

*(Continuation of the same audit. CF-004 is VERIFIED and was not re-proved.)*

### 1A.1 Inherited findings — 3 of 10 independently verified

**CF-001 — REJECTED.**

- Hypothesis: `docs/decisions/ADR-access-package-integrity-automation.md` is "also titled
  accepted ADR-011", colliding with the legacy-surface-retirement ADR.
- Primary evidence: that file's **line 1 reads `# ADR-012 — Integrity automation inside an
  Access package`**. `ADR-011` is consistently `ADR-legacy-surface-retirement.md` (line 1)
  and `docs/platform-decisions.md:123` — one decision, two consistent locations.
- **There is no ADR-011 collision in this worktree.** The inherited finding is rejected
  against primary evidence.

**CF-036 (NEW, found by rejecting CF-001) — migrations cite the wrong ADR.**

- FACT: `111_statistical_numeric_envelope_control.sql` and
  `112_statistical_numeric_envelope_data.sql` both cite **`ADR-011`** for "owner decision
  D-3" (2 occurrences).
- FACT: `D-3` exists only in `ADR-access-package-integrity-automation.md`, which is
  **ADR-012**. `ADR-011` is legacy surface retirement — an unrelated decision.
- **The two migrations implementing DR-006's scale-16 decision cite an ADR that does not
  contain it.** Same family as CF-029: a false cross-reference embedded in an applied,
  checksummed migration, where schema history is already a weak evidence source (CF-012).
- Risk: MEDIUM (governance/traceability). Confidence: HIGH. Status: IMPLEMENTATION-DEFECT.

**CF-012 — CONFIRMED, and REVISED in substance. This also corrects an error of mine.**

- FACT, `PlatformSchemaMigrationRunner:176-194`: the allowlist contains **ten** entries —
  `011`, `020`, `021`, `055`, `065`, `069`, `070`, `079`, `080`, `097`. The inherited
  statement was right; **my own batch-9 count of "5 checksum exception filenames" was
  wrong** (a bad grep). Corrected here.
- What the inherited statement missed, and what primary evidence shows: every exception is
  **commented with a justification**, grouped by reason ("corrected before production
  rollout without changing their applied effects"; "corrected so that an empty database can
  be built"; one with an explicit content proof "SHA-256 94AFFBA7…, 15 datasets, 104
  fields, 21 relations"), one class cites a reproducible proof
  (`ops/tests/sql/migration-chain-fresh-replay.sh`), and **all other drift fails closed**:
  `throw new IllegalStateException("Schema migration checksum changed: … Create a new
  numbered migration instead of rewriting history.")`.
- **Revised statement:** this is not an ungoverned history rewrite. It is a bounded,
  documented, fail-closed-by-default allowlist. The accurate defect is narrower: **the
  allowlist is Java code, not a governed artifact** — no ADR, no per-entry evidence record,
  no expiry, and it grows by editing a class.

**CF-020 — CONFIRMED** (re-verified in batch 9 from primary evidence): 113 `.sql` files
present, 108 `executeAndRecord` calls, **6 unregistered**, of which
`056_activate_kids_r8_supersede_legacy.sql` is architecturally decisive. Additional
primary evidence this pass: `alreadyEffective(jdbc, resource)` (line 196) provides an
adoption probe — "adopted without execution (its effect is already present)" — which is a
third registration state the inherited statement did not capture.

**NOT VERIFIED in this pass: CF-002, CF-010, CF-011, CF-015, CF-017, CF-018, CF-019 (7 of
10).**

### 1A.2 RC-003 falsification — SURVIVED, and STRENGTHENED

The instruction required an aggressive attempt to prove RC-003 false by behaviour, not by
name. Five independent search axes were used:

| Axis | Result |
|---|---|
| SQL write verbs (`INSERT`/`MERGE`/`UPDATE`/`UPSERT`) against 15 canonical registry tables, across `*.java`, `*.sql` and `*.yml` under `src/main` | **4 writes total, all `UPDATE`, zero `INSERT`**: `contract_source` (mapping spec), `data_product` (tenant key), `site_contract_revision` ×2 (status transitions) |
| JPA / ORM producers | The entire api module contains **one** `@Entity`, and it maps to none of these tables. The core module has **no** `@Table` entity for `site_contract*`, `data_product` or `classification_scheme` |
| Stored procedures / functions in the migration chain | **None exist** — no `CREATE PROCEDURE`/`FUNCTION` in any of the 113 migrations |
| Startup bootstrapping (`ApplicationRunner`, `CommandLineRunner`, `@PostConstruct`, `InitializingBean`) | 7 candidates; none produces canonical declarations (see below) |
| Build/CLI/export tooling | no producer found |

**RC003_FALSIFICATION: SURVIVED. Confidence raised to HIGH+.** No governed authoring path
exists for any canonical declaration registry.

**NEW RC-003 instance — a fourth registry.** `ProviderCapabilityDiscoveryRunner` (28
lines) is named as though it produces capabilities. Primary evidence: it *"Loads approved
provider capabilities once during startup when explicitly enabled"* and **throws** —
`"Provider discovery enabled but no ACTIVE provider capability exists"`. It is a
**reader/validator that fails closed**, not a producer. Therefore
**`platform.provider_capability` also has no runtime producer**, joining
`site_contract_*`, `data_product` and `classification_*`.

This matters beyond bookkeeping: DR-006 decided the numeric envelope must **move into the
provider capability registry** — and that registry has no producer either. The DR-006
remediation therefore depends on RC-003's remediation, a dependency not previously
recorded.

### 1A.3 CF-037 (NEW) — shadow startup bean

FACT: `org/base/api/runner/CarsMigrationRunner.java` (59 lines) is an `@Component`
`ApplicationRunner` — a **live Spring bean in the startup path** — whose `run()` body is
**entirely commented out**. The commented code executed `migration/eoy_2017.sql` through
`ScriptUtils.executeSqlScript` on an **unqualified `@Autowired JdbcTemplate`**.

- No register, document or ADR mentions it. Its domain ("Cars", `eoy_2017`) appears nowhere
  in the recovered architecture.
- Benign today. Latent risk: an unqualified `JdbcTemplate` in a three-datasource
  application, plus a path that would execute an **unversioned SQL script outside the
  governed migration chain** if uncommented.
- Classification: **ABANDONED EXPERIMENT / SHADOW ARCHITECTURE**, orphan. Status: recorded,
  not removed.

### 1A.4 Producer/consumer sweep — canonical registries

| Registry | Runtime producer | Type | Governed? |
|---|---|---|---|
| `data_product` | **none** (tenant-key UPDATE only) | — | n/a |
| `site_contract_revision` | **none** (2 status UPDATEs) | — | approval only |
| `site_contract_dataset` / `_field` / `_relation` / `_classifier` | **none** | migrations | no |
| `contract_page_binding` | **none** | migrations | no |
| `contract_source` | mapping-spec UPDATE only | partial | — |
| `classification_*` | **none** | migrations | no |
| `classifier_proposal` | `AccessClassifierProposalImportService` | proposal | **yes, correct** — but **no reader** |
| `metadata_schema` / `_subject` | **Access package** | inverted | **no** (CF-033/034) |
| `provider_capability` | **none** (NEW) | — | reader fails closed |
| `statistical_contract_draft` / `_event` | `ContractWorkflow` | governed | **yes** |
| `ingestion_contract` | `StatisticalBindingService` | partial | no revision (CF-032) |

**PRODUCER_CONSUMER_SWEEP: PASS for the canonical declaration registries.** The pattern is
uniform and now measured on five axes: **the statistical subsystem is the only part of this
platform that can declare anything at runtime.**

### 1A.5 New orphans

Added to the orphan register: `CarsMigrationRunner` (live bean, empty body);
`platform.provider_capability` (read and validated, never written at runtime).

---

## 1B. PASS 3 — remaining inherited findings, RC-001/RC-002 falsification, collision sweep

### 1B.1 Inherited findings — six more classified

**CF-011 — CONFIRMED, with the two cases sharply separated.**

- `022_kids_complete_site_contract.sql`: **255 lines, 12 table-creation guards, 41 KIDS
  references** in one file. Generic DDL and site declaration seeds are genuinely mixed.
  **This is the strong case and it stands.**
- `109`: primary evidence weakens the inherited framing. Its header shows a **generic,
  deny-by-default capability** — "a table is servable only when it says so, and a table may
  demand an authority of its reader … Deny by default: every existing row is NOT servable,
  so this migration alone changes no answer" — followed by **two** site-specific rows
  recorded as "the recorded decision". Only 2 KIDS references in the file.
- **Revised statement:** the mechanism in 109 is generic and fail-safe; only its seed data
  is site-specific. CF-011's substance rests on 022, not 109.

**CF-002 — REVISED; it is not a contradiction.**

- `STATISTICAL-CONTRACT-OPEN-QUESTIONS.md:80` (the Q11 table row) declares `REPLACE_SCOPE`,
  `UPSERT` (default), `DELETE`.
- **Line 316 is a dated change-log entry that supersedes it:** *"2026-09-19 | Q11 refined
  after implementation: v1 load mode is `FULL_SNAPSHOT` only (the snapshot is the native
  immutable unit of the platform)"*.
- The document therefore **contains its own governed refinement**, which is ordinary
  decision-register hygiene, not a contradictory decision. Runtime matches the refinement.
- **Residual defect, narrower:** the Q11 table row was never restated, so a reader of the
  table alone is misled. Risk LOW-MEDIUM, documentation consistency. Status: REVISED.

**CF-010 — CONFIRMED, and sharper than stated.**

`platform.provider_capability` columns are: `provider_code`, `family_code`,
`operation_code`, `feature_code`, **`max_page_size`**, `transactional_supported`,
`lifecycle_status`. There is **no precision, no scale, no column-count and no
name-length** dimension — the registry carries exactly **one** quantitative limit.

Consequence beyond the inherited statement: the limits the system actually enforces
(numeric envelope via Spring properties; Access column/name limits in the physical planner)
live **outside** the registry that is supposed to declare provider capabilities. That is an
authority collision, recorded in §1B.4.

**CF-018 — CONFIRMED; two independent failure modes visible in the SQL.**

`CanonicalObservationReader:47-53`:
1. `measure.measureRef().wire().endsWith(wireCode)` — a **suffix match**. Two measures whose
   codes are suffixes of one another at the same version (e.g. `RATE` / `BIRTH_RATE`) both
   match, binding the wrong metric.
2. The lookup query is `... JOIN platform.dataset d ... WHERE d.dataset_code = ?` — **no
   product scoping**, while the same class's `datasetVersions(plan, productCode)` *does*
   scope by product. Metrics from another product's dataset of the same code can be pulled
   in.

**CF-019 — REVISED; the inherited "global trim" claim is not supported.**

`WideRowNormalizer` contains exactly **two** `trim()` sites, not a global policy:
- line 152 — `raw instanceof String s && DECIMAL.matcher(s.trim()).matches()`: trimming used
  solely to test and parse a decimal. Correct.
- line 174 — inside a single helper `text(Object raw)`: `raw.toString().trim()` returning
  `null` for empty. This is **deliberate canonicalization** (trim + empty→null), consistent
  with the platform's "empty is not zero" thinking.

**Revised statement:** not a global trim. Bounded to one helper. The genuine residual is
narrow and **UNVERIFIED**: whether any declared `BoundedText` field requires significant
leading/trailing whitespace, or needs to distinguish a whitespace-only value from absence.
Risk LOW. Status: PARTIAL.

**CF-015 — PARTIAL, and the inherited framing is partly contradicted.**

Three attachment levels exist in the model: `DATASET`, `DIMENSION_GROUP`, `OBSERVATION`.
The CF-004 inspection additionally established that the revision digest binds
`attachment = { level, dimensions.sorted(), measure }` — i.e. **the model does carry the
group's dimension set**, which is inconsistent with the inherited claim that
`DIMENSION_GROUP` "is treated as dataset-constant". The open question is narrower: whether
a constant *value* can be declared per dimension combination, which requires the
constant-binding resolution logic. **Not settled; classified PARTIAL rather than
CONFIRMED.**

**CF-017 — REJECTED as stated; the condition it describes is not present.**

Physical evidence from the read-only scratchpad copy of the R8 artifact:

| Access system table | Rows | Content |
|---|---|---|
| `MSysNavPaneGroups` | **34** | includes `Custom Group 1` under `GroupCategoryID=3` |
| `MSysNavPaneGroupCategories` | **3** | includes `{Filter=Tables, Type=0}` and `{Name=Custom, Type=4}` |
| `MSysNavPaneGroupToObjects` | **37** | group→object mappings |
| `MSysNavPaneObjectIDs` | **45** | object id registry |
| `MSysObjects` | 79 | — |

**The navigation pane is populated, not empty.** The inherited hypothesis — "generated file
was structurally readable by Jackcess but opened with an empty Access navigation pane" —
describes a condition that does not hold for this artifact.

**Source-side confirmation:** `AccessAuthoringAdapter` **produces** this metadata
deliberately. Line 47 documents the vocabulary ("Navigation Pane vocabulary of Access: a
custom category holds named groups of…"); line 248 states the intent ("Groups the objects
in the Navigation Pane so the author sees where data is entered"); `navigationGroups(db,
plan, lookupTables)` at line 253 writes all four `MSysNavPane*` system tables and line 275
sets the database property `"NavPane Category"` to the custom category id. The finding
predates the fix — git history records commit `0ba151b group the authoring file in the
Access Navigation Pane`.

**Honest caveat:** the R8 file's `MSysNavPaneGroupCategories` row carries
`SelectedObjectID=39`, which is UI *selection state* that only Microsoft Access writes.
This artifact has therefore been opened in Access at some point. That does not weaken the
conclusion — the generator demonstrably writes the structure in source — but the artifact
alone cannot prove the generator's output untouched by Access.

**Authority classification: PROVIDER / UI PRESENTATION METADATA.**

| Question | Answer |
|---|---|
| Affects contract semantics? | **No** |
| Affects data semantics, relationships, grain? | **No** |
| Affects ingestion? | **No** |
| Affects package validation? | **No** — `SemanticAccessPackageReader` reads `__gs_*` only |
| Affects generated physical structure? | **No** — tables, columns, keys and relations are unchanged by it |
| Affects user-facing Access organisation? | **Yes — only this** |
| Consumed by platform code? | **No.** `AccessAuthoringAdapter` is the **only** file in the entire codebase that references `MSysNavPane*`. It is write-only metadata |
| Reproduced by generation? | **Yes**, deterministically from the semantic plan |

**Usability connection (REQ-010 / REQ-011 / REQ-012).** This is a legitimate *generated
usability* concern, not a semantic one, and it is a positive instance of the requirement
pair **maximum safe derivation + minimum necessary manual input**: the authoring
organisation is **derived from the plan** rather than left for a human to arrange after the
file is produced. It belongs in the Automation Baseline as AUTOMATIC, and in the Physical
Pattern Register as provider-presentation output of the realization step.

**CF-017 → REJECTED (historical; fixed before this audit).** Narrowed rather than
preserved, per instruction.

### 1B.2 RC-001 FALSIFICATION — SURVIVED

The question posed was not "do the two states usually agree" but "does the architecture
**guarantee** they cannot diverge". Four search axes:

| Axis | Result |
|---|---|
| Database triggers on publication tables | **None.** The only trigger in all 113 migrations is `tr_statistical_reference_definition_immutable` on `statistical_reference` |
| Constraints linking `dataset_snapshot.status` to `snapshot_member` | **None** |
| Outbox handlers | `PlatformOutboxProcessor` claims events by CAS and reads `publication.dataset_snapshot.row_count` for verification. It does **not** reconcile status against membership |
| Scheduled jobs (10 inspected) | none reconciles the two; `PlatformServingCacheService` *consumes* membership, it does not enforce agreement |

**No mechanism guarantees equivalence.** Worse, a known writer actively breaks it
(CF-023's product-wide status flip creates `PUBLISHED` snapshots with no membership) and
rollback deepens the divergence (restores `publication.snapshot`, never
`dataset_snapshot.status`).

**RC001_FALSIFICATION: SURVIVED.** Confidence HIGH+.

### 1B.3 RC-002 FALSIFICATION — SURVIVED, with a refinement that strengthens it

Tested whether the migrations' appearance as semantic authoring is misleading:

| Hypothesis that would falsify RC-002 | Result |
|---|---|
| The KIDS declarations are **generated outputs** | **Refuted.** No migration carries a generated-artifact marker ("generated by", "auto-generated", "do not edit") — zero of 113. No generator emitting migration SQL exists in `ops/cli` or `ops/scripts` (the one loose match, `build_kids_admin_panel_blueprint.py`, builds an admin-panel blueprint, not migrations) |
| They are **seed/reference data by explicit architecture** | **Refuted.** No ADR or architecture document designates migrations as the declaration authority; the learning guide assigns that role to the Control Plane Schema (RC-003) |
| They are **temporary bootstrap** | **Refuted.** Batch 11's behavioural sweep proved no alternative producer exists to take over |
| Migration registration is complete via another mechanism | **Partly true, and it refines rather than falsifies RC-002** — see below |

**The `alreadyEffective()` refinement, and the distinction the instruction required
preserving.** The runner logs *"schema.migration adopted without execution (its effect is
already present)"*. Therefore **UNREGISTERED ≠ NEVER EFFECTIVE**: a migration such as
`056_activate_kids_r8_supersede_legacy.sql` may well have been effective on the evolved
production database while being absent from a clean rebuild. This explains how the two
states can coexist without error — and it **strengthens** RC-002, because it shows the
historical record is *tolerant* rather than *authoritative*: the chain records what is
present, not what was declared.

**RC002_FALSIFICATION: SURVIVED.** Confidence HIGH.

### 1B.4 AUTHORITY-COLLISION SWEEP — complete

| Responsibility | Effective authorities found | Classification |
|---|---|---|
| Publication / live state | `snapshot_member` **and** `dataset_snapshot.status` | **ACCIDENTAL SECOND AUTHORITY** (RC-001) |
| Site contract | migrations (create) + approval service (state) | single authority, **no producer** — not a collision |
| Statistical contract | `ContractWorkflow` only | CANONICAL AUTHORITY |
| Ingestion contract | `StatisticalBindingService` (row) + migrations (`_revision`) | **TEMPORARY MIGRATION** split (CF-032) |
| Metadata schema | Control Plane registry **and** Access package | **ACCIDENTAL SECOND AUTHORITY** (CF-033) |
| Metadata subjects/assertions | package-supplied lifecycle + hardcoded `'APPROVED'` | **ACCIDENTAL SECOND AUTHORITY** (CF-034) |
| Classifier registry | migrations only; proposals never promoted | single authority, **no producer** (CF-016) |
| **Provider capabilities** | registry (`max_page_size` only) **+** Spring properties (numeric envelope) **+** physical planner (column/name limits) | **ACCIDENTAL SECOND AUTHORITY — NEW (CF-038)** |
| Package identity | `__gs_package` values, unverified | TRANSPORT COPY (no digest — DR-007) |
| Physical realization | `AccessAuthoringAdapter` from the compiled plan | DERIVED REPRESENTATION |
| Serving eligibility | `ServingPolicy` (table) + snapshot status (row) | two *different* questions — **LEGITIMATE SPECIALIZATION** |
| Chart / visualization declaration | four mechanisms | **ACCIDENTAL SECOND AUTHORITY** (DR-001, CF-030) |
| Numeric representation constraints | Spring property + `DECIMAL(38,16)` storage + Access ceiling | **DERIVED/LAYERED** — resolved by DR-006, not a collision |
| Migration history | ledger + 10-entry Java allowlist + `alreadyEffective()` probe | **COMPATIBILITY STATE** (CF-012, CF-020) |

**CF-038 (NEW) — provider limits have three authorities.** The registry declares one
(`max_page_size`); the numeric envelope lives in Spring properties; the Access column and
name limits live in the physical planner. A provider's true capability is therefore
assembled from three places, none of which is authoritative. This is the concrete obstacle
to DR-006's instruction to move the envelope into the registry, and it compounds the
RC-003 dependency already recorded (the registry has no producer either).

**AUTHORITY_COLLISION_SWEEP: PASS.** Five accidental second authorities found, all
classified; none left UNKNOWN.

### 1B.5 CF-036 class — decision-provenance drift

Bounded search performed alongside the above. **One further instance found**, already
recorded as CF-029 (migration 110 documents a converter that does not exist). Together with
CF-036 (migrations 111/112 citing ADR-011 for a decision in ADR-012), these form a pattern
rather than isolated slips:

**Grouped under RC-002** as a third symptom of the same cause: *migrations carry
architectural narrative that nothing validates*. No separate root cause is created.

### 1B.6 CF-037 class — shadow bypass surfaces

Additional live-but-dormant components with retained execution paths: **none found beyond
`CarsMigrationRunner`** among the 7 startup runners inspected.
`ProviderCapabilityDiscoveryRunner` is dormant-by-configuration but **fails closed** when
enabled, which is correct behaviour and not a bypass surface. Recorded so the distinction
is explicit: *disabled-and-fail-closed* is not the same as *dormant-with-live-path*.

---

## 1C. PASS 2A — data semantics, information preservation, model completeness

### 1C.0 A phantom reference, stated before anything else

§25 of the PASS 2A specification instructs me to "update the **mandatory L0–L47 Layer
Coverage Matrix**". **No such matrix exists.** A search of all of `docs/` finds zero
occurrences of "Layer Coverage Matrix", "LAYER_COVERAGE" or any `L0`…`L47` vocabulary, and
no earlier phase of this engagement established one. I have not invented 48 layer rows to
satisfy the instruction. `LAYER_COVERAGE_UPDATED: N/A — artifact does not exist`.

Similarly, §27 cites "the already-established mandatory invariant TRACK NOW — DELETE LATER"
and "MIGRATED ≠ DONE". Those exact formulations were not established either, but they are
**substantively equivalent** to INV-012 (gated legacy removal) and the Legacy/Duplication
Register's disposition column, which do exist. I have used those rather than create
duplicates.

### 1C.1 CF-015 — CLOSED: NOT SUPPORTED, and REQUIRED

Primary evidence:

- `Component.Attachment(AttachmentLevel level, List<String> dimensions, String measure)` —
  an attachment **does** carry the dimension list, so the *group shape* is expressible.
  This is why PASS 1 could not sustain the "treated as dataset-constant" wording.
- `ContractDraft.ConstantBinding(String component, String value, boolean overridable)` —
  a constant binding carries **exactly one value per component**. There is **no
  per-combination key and no value map**.

**Conclusion:** the grammar can declare "this component is constant within the group formed
by dimensions [D1,D2]", but it **cannot declare what that value is for each combination**.
The value must therefore arrive per observation — precisely the repetition AIR-2026-052
reported.

**Classification: NOT SUPPORTED.**

**Is it required?** Yes, by recovered domain evidence rather than preference: checkpoint
§14 item 7 records *"Units vary by INDICATOR. Current attachment grammar cannot express
this cleanly without repetition."* That is a property of the KIDS data.

**→ REQUIRED-BUT-MISSING CAPABILITY: per-combination constant value declaration.** Recorded
in the Missing Component Register. Not designed here.

### 1C.2 CF-019 — CLOSED: SAFE NORMALIZATION

`text()` is applied at four sites in `WideRowNormalizer`: dimension codes (89), attribute
values (108), measure presence (127) and inside the value converter (146).

- **Dimension codes** — trimming a code is correct canonicalization.
- **Measures** — line 127 treats a whitespace-only cell as absent and then requires a
  status, which is exactly INV-007's rule.
- **BoundedText attributes** — `JdbcStatisticalRegistry:140` bounds `maxLength` to
  **1…255**, so BoundedText is a short field (code/label/short note), not prose. Length
  validation at `WideRowNormalizer:140` runs **after** trimming, so trimming cannot cause a
  spurious length failure.

**Classification: SAFE NORMALIZATION.**

One behaviour recorded as known and non-material: `text()` collapses **empty → null**, so
"author typed a space" and "author left it blank" are indistinguishable. For measures this
is correct (INV-007 demands a status either way). For bounded-text attributes the two
collapse, and **no recovered requirement distinguishes them**. Recorded rather than
escalated, per the materiality rule.

### 1C.3 NEW CF-039 (MATERIAL) — entity properties have no typed canonical representation

The most significant finding of PASS 2A, and it **corrects my own consolidation**.

Primary evidence, `002_data_plane.sql`:

```sql
CREATE TABLE entity.entity_record (
  entity_id BIGINT IDENTITY PRIMARY KEY,
  dataset_snapshot_id BIGINT NOT NULL,
  external_key NVARCHAR(512) NULL,
  record_type VARCHAR(32) NOT NULL,
  title NVARCHAR(512) NULL,
  payload_json NVARCHAR(MAX) NOT NULL,      -- every declared entity field lives here
  payload_hash CHAR(64) NOT NULL,
  source_record_id BIGINT NOT NULL,
  valid_from / valid_to / is_current ...
)
```

Compare the statistical family, which is fully relational and typed: `statistics.series` +
`observation` (`period_start`/`period_end` DATE, `numeric_value` DECIMAL, `text_value`,
`boolean_value`) + `observation_dimension` (`classification_item_id`) +
`observation_attribute`.

**Every declared entity field is stored inside one untyped JSON document**, and
`ContractPhysicalQueryService.canonicalSource` reads it back with
`JSON_VALUE(payload_json,'$.<field>')`. For entity data the canonical store therefore
enforces **no type, no nullability, no length, no uniqueness and no referential integrity**
on any contract-declared field. The contract declares them; nothing below the contract
holds them to it.

- **Corrects REQ-007 in my consolidation.** I recorded REQ-007 ("no naive EAV/key-value")
  as *upheld*, citing the typed `__ent_*` tables observed physically in the R8 artifact.
  That evidence was from the **Access transport**, not the **canonical store**. The
  transport is typed; the canonical store is a document. REQ-007 → **PARTIAL** (upheld for
  statistical, violated for entity properties); REQ-008 ("no uncontrolled JSON/blob
  semantics") → **VIOLATED** on this axis.
- **Bears on §21's falsification target.** The evidence does **not** support a blanket
  claim that the generic model privileges statistical data — relations and classifier
  bindings are properly typed (1C.7). It supports a narrower, more useful claim: **entity
  *property* storage is the one family modelled as a document while every other family is
  relational.**
- Risk: HIGH (data integrity; REQ-006/007/008). Confidence: HIGH. Status:
  ARCHITECTURAL-DEFECT.

### 1C.4 NEW CF-040 — declared relation cardinality is enforced nowhere

`entity.entity_link` is otherwise well modelled: typed FKs on **both** endpoints
(`fk_entity_link_from`, `fk_entity_link_to`), `relationship_type_id`, `ordinal` for
ordering, `valid_from`/`valid_to` for temporal validity, `source_record_id` for provenance.
Referential integrity **is** enforced at the database.

Absent: **any uniqueness or cardinality constraint**. The contract declares `ONE_TO_MANY` /
`MANY_TO_MANY` (`site_contract_relation.cardinality`), and the only runtime consumer of
that declaration is `ContractPhysicalQueryService.hasManyChildren()` — a *presentation*
decision about whether to attach a list or an object. Nothing prevents two rows from
violating a declared `ONE_TO_ONE`.

Risk: MEDIUM-HIGH (integrity). Confidence: HIGH. Status: IMPLEMENTATION-DEFECT.

### 1C.5 Status vocabulary — a second instance of CF-025, not a new finding

`[statistics].observation.observation_status VARCHAR(24) NOT NULL DEFAULT 'VALID'` — **no
CHECK constraint**, exactly like `publication.dataset_snapshot.status` (CF-025). Grouped
under CF-025 rather than given a new identifier, per the materiality rule. The defect class:
*status vocabularies are unconstrained at the schema level and enforced only by convention
in application code.*

### 1C.6 Null / missing / unknown / not-applicable

`observation` carries three typed value columns (`numeric_value`, `text_value`,
`boolean_value`), all nullable, plus `observation_status`. The release gate counts an
observation with **all three null and the default status** as an unexplained empty
(`SnapshotFactsRepository`, with the rule stated in a source comment). The migration
vocabulary contains `'MISSING'` alongside `'VALID'`.

**The platform can distinguish "no value with a declared reason" from "no value with no
reason", and it gates on the difference.** What it does not do is constrain the status
vocabulary (1C.5), so *unknown* vs *not-applicable* vs *suppressed* are distinguishable
only if authors use consistent strings. **Classification: distinguishable by convention,
not by constraint.**

### 1C.7 Three vertical slices — canonical storage layer

| Family | Canonical storage | Typing | Integrity enforced |
|---|---|---|---|
| **Statistical** | `series` + `observation` + `observation_dimension` + `observation_attribute` | **typed** — DECIMAL(38,16) after DR-006, DATE interval, BIT, NVARCHAR; dimensions by `classification_item_id` | typed columns, FKs; status unconstrained (1C.5) |
| **Entity (properties)** | `entity_record.payload_json` | **untyped document** | `payload_hash` only — **CF-039** |
| **Relational** | `entity_link` | typed FKs, ordinal, temporal | FK both ends; **cardinality unenforced — CF-040** |
| **Classifier binding** | `entity_classification` | typed, bound by **item identity**, never label | PK(entity, attribute, item, valid_from) + FKs, temporal |

`entity_classification` deserves explicit credit as **correct**: it binds by
`classification_item_id`, never by code or label, and carries temporal validity. It is the
counter-example proving the platform knows how to model a classifier binding properly — the
same role `AccessClassifierProposalImportService` plays for the proposal boundary and
`tr_statistical_reference_definition_immutable` plays for identity immutability.

### 1C.8 What PASS 2A did NOT cover

Of the thirty sections specified, this pass substantively covered §3 (CF-015), §10
(CF-019), §4–§6 at the canonical-storage layer, §12, and parts of §1, §21 and §22.

**Not covered:** §2 full statistical end-to-end re-trace, §7 raw/source preservation, §8
full boundary classification, **§9 the complete information-loss matrix across all 40+
axes**, §11 numeric path re-verification, §13 full identity model, §14 version/revision
model, §15 time semantics, §16 provenance model, §17 tenant scoping, §18 constraint
inventory, §19 provider round-trip, §20 presentation projection, §23 data-entry semantics,
§24 data automation baseline, §26 full forward+backward slice traceability.

`INFORMATION_LOSS_MATRIX` therefore remains **PARTIAL**, as it has been since batch 9.

---

## 1D. PASS 2A continuation — the three unnamed layers, and a correction

### 1D.0 §17's premise was wrong, and the error was mine

The prompt states the ledger "currently reports 4 NOT_INSPECTED rows while L35/L42/L46
account for only three". It reports three. A mechanical recount of all 48 rows gives
**VERIFIED 24 · INSPECTED 12 · PARTIAL 9 · NOT_INSPECTED 3**; my hand-written summary said
21/11/12/4 and did not match the matrix above it.

**There is no fourth `NOT_INSPECTED` row.** The ledger summary has been corrected in place
with the correction recorded rather than silently overwritten. No row disappeared through
reclassification — the count was simply miscounted by me.

### 1D.1 L35 — Archive & retention: IMPLEMENTED

Primary evidence: Archive Plane tables `archive.snapshot`, `archive.record`,
`archive.artifact_reference`, `archive.payload_pointer`, plus `purge`/`query`/`update`
operations. Services: `PlatformArchiveService`, `PlatformPublicationArchiveService`,
`PlatformArchiveRetentionService`, `PlatformArchivePayloadRepairService`.

`PlatformArchiveRetentionService` states an **approved policy** in source: *"Enforces
approved one-year archive retention. Artifact object-store lifecycle must use the same
retention window."* It runs under a distributed job lease (`archive-retention-purge`, 60 s).

- **Disposition: IMPLEMENTED, not a gap.** The omission was in our *coverage*, not in the
  platform.
- **One constraint-preservation entry recorded:** the object-store lifecycle alignment the
  Javadoc requires is **CONVENTION ONLY** — nothing in code enforces that the S3 lifecycle
  window matches the database retention window. A divergence would silently orphan or
  prematurely destroy payloads. Recorded in the constraint map; **not** a new CF (it is a
  convention-only invariant, the class already catalogued).

### 1D.2 L42 — Export & SDMX: IMPLEMENTED, and unwired

`SdmxCsvExporter` (87 lines) is a careful implementation: *"one column per dimension,
measure and row-level attribute (**SDMX 3 multi-measure layout**). Deterministic"*. Header
is `STRUCTURE, STRUCTURE_ID, ACTION` + dimensions + measures + attributes.

Two design details worth recording as positives:

- **Unsupported conversion is refused explicitly, with semantic reasoning**, not silently
  degraded: `SDMX_ML_2_1_GENERIC` throws `UnsupportedConversion` — *"SDMX 2.1 has one
  primary measure; a structure with N measures is not converted implicitly"*. This is
  INV-003's "explicit rejection, never silent feature loss" implemented correctly.
- **Attachment semantics are respected:** attributes are filtered by
  `a.attachment().level().variesPerRow()`, so only row-varying attributes become columns.
  (`AttachmentLevel` therefore carries a `variesPerRow()` predicate — additional evidence
  for CF-015's model, which is richer than the inherited wording suggested.)
- Value fidelity is preserved by refusing at ingestion rather than coercing at export:
  *"The value itself is never changed: ingestion already refused anything that does not
  [fit]"*.

**But `SdmxCsvExporter` has zero callers.** A repository-wide search finds no reference
outside its own file. This is the **fourth instance of the same orphan class**, after the
package composer (DR-003), the JSON Schema (DR-005) and `classifier_proposal` (CF-016):
*a correct, well-designed component that nothing invokes.*

**Material interaction with CF-005:** the exporter would emit attribute **columns**, but
`CanonicalObservationReader` does not read `statistics.observation_attribute`. Wiring the
exporter today would therefore produce SDMX-conformant headers with **empty attribute
columns**. **CF-005 is a prerequisite for wiring L42** — a dependency not previously
recorded.

Registered export formats: `SDMX_COMPATIBLE`, `SDMX_JSON`, `SDMX_XML`, `CSV`, `JSON`
(`ExportCodecRegistry`), with `SdmxCompatibilityController` and
`ContractContentNegotiationFilter` on the serving side. **SDMX is adapter-level, not
canonical** — consistent with INV-011 and DR-REC-003.

### 1D.3 L46 — Observability & audit evidence: IMPLEMENTED

Audit and evidence mechanisms found: `platform.contract_approval_receipt`;
`site_contract_revision_approval.evidence_json` (checksum-bound);
`ReleaseGateEvidenceRepository` (per-gate, schema-bound, facts-digest-bound);
`platform.statistical_contract_draft_event`; `platform.outbox_event` (6 write sites);
`platform.api_operation` (idempotency/operation ledger); `platform.schema_migration`
(migration ledger); tenancy transfer history with a recorded reason;
`ArtifactObjectIntegrityAuditService` and `ArtifactRelationIntegrityAuditService`.

**The distinction §16 asked for holds:** these are records *about* state, not state itself.
Observability and authoritative domain state are properly separated — evidence tables never
act as the source of truth for what is live or approved.

**Critical state transitions are reconstructable** for: contract approval, gate evaluation,
tenancy transfer, migration application, API operations and statistical draft lifecycle.

**One exception, already a known defect:** publication has no audit record of its own beyond
the outbox `PUBLISH_SNAPSHOT` intent — and batch 8 established that this intent **names one
snapshot while the writer publishes N** (CF-023). So the publication audit trail is not
merely thin; **it actively misdescribes what occurred.** This strengthens CF-023's severity
on the evidence axis (REL-001) and is recorded there rather than as a new finding.

Not inspected: correlation/trace identifiers and OpenTelemetry wiring. L46 remains
`INSPECTED`, not `VERIFIED`.

### 1D.4 Coverage after this continuation

```text
VERIFIED = 24 · INSPECTED = 15 · PARTIAL = 9 · NOT_INSPECTED = 0   (48 rows)
```

**No row remains uninspected.** None of L35/L42/L46 was upgraded past `INSPECTED`: each has
primary-evidence support for authority, producer and consumer, but none has had its
information-loss and failure semantics fully traced.

### 1D.5 What this continuation did NOT complete

Honest statement of remaining PASS 2A scope, unchanged from §1C.8 except where noted:

- **§1 the full information-loss matrix across the 40+ named axes** — still PARTIAL. The
  numeric axis (DR-006), normalization (CF-019), attributes (CF-005), aggregation (CF-027),
  page parameters (CF-028) and entity typing (CF-039) are characterized; the remaining
  ~30 axes are not individually assessed.
- **§2 identity model** — partially advanced (CF-018 class understood, scoping defects
  found) but the 21-object trace is not complete.
- **§3 version/revision/lifecycle model** — QF-012/INV-014 understood; the systematic
  "latest-substitution" sweep is not done.
- **§4 provenance model** — substantially advanced by L46 above, but not traced per object.
- **§6 reprocessability**, **§8 time semantics**, **§9 full constraint map**, **§10 provider
  round-trip**, **§11 multi-presentation**, **§13 data-entry classification**, **§18 full
  forward+backward vertical traceability** — not done.

**PASS 2A cannot close on this evidence.** The stop condition in §22 requires the
information-loss matrix to be materially complete and the identity, version, provenance,
boundary, reprocessability, constraint and round-trip models to be understood. Six of those
eight remain PARTIAL.

---

## 1E. PASS 2A — final semantic closure

### 1E.1 Information-loss matrix — material axes

`S` silent · `D` detectable · `V` validated · `Rev` reversible · `Rel` reloadable from source.

| Axis | Transformation | Classification | S | D | V | Rev | Rel |
|---|---|---|---|---|---|---|---|
| Numeric precision/scale | declared ≤(28,16) → `DECIMAL(38,16)` | **NO LOSS** — storage is a strict superset | – | – | Y | Y | Y |
| Numeric, legacy | source → `DECIMAL(28,10)` | **LOSSY — LEGACY**, digits already destroyed | **Y** | N | N | **N** | Y (artifact) |
| Numeric, provider | canonical → Access `DECIMAL` | **LOSSY — PROVIDER CONSTRAINT** (28 ceiling), envelope bounded to prevent it | – | Y | Y | Y | Y |
| Rounding/overflow | in-memory `BigDecimal` aggregation | **NO LOSS today**; becomes a constraint when CF-027 pushes down | – | – | N | – | – |
| Null vs empty | author cell → `text()` → null | **NORMALIZATION** (CF-019) | Y | N | N | N | Y |
| Missing vs unknown vs N/A | value + `observation_status` | **NO LOSS by convention** — gate enforces "empty needs a reason"; vocabulary unconstrained (CF-025 class) | – | Y | Y | – | – |
| Type (statistical) | declared → typed columns | **NO LOSS** | – | Y | Y | Y | Y |
| **Type (entity)** | declared → `payload_json` | **LOSSY — UNJUSTIFIED** (CF-039) | **Y** | **N** | **N** | N | Y |
| Nullability/length/uniqueness (entity) | declared → `payload_json` | **LOSSY — UNJUSTIFIED** (CF-039) | **Y** | **N** | **N** | N | Y |
| Keys / identity uniqueness | contract → PK/UK | **NO LOSS** where declared | – | Y | Y | – | – |
| Relation identity & direction | contract → `entity_link` FKs | **NO LOSS** | – | Y | Y | Y | Y |
| **Relation cardinality** | contract → storage | **LOSSY — UNJUSTIFIED** (CF-040): declaration exists, enforcement does not | **Y** | **N** | **N** | N | Y |
| Hierarchy | `__cl_hierarchy` → `classification_*` | **UNKNOWN — no promotion path** (CF-016); R8's table is empty | Y | N | N | – | Y |
| Classifier identity | code → `classification_item_id` | **NO LOSS** — bound by ID, never label | – | Y | Y | Y | Y |
| **Classifier revision binding** | assertion → newest schema | **LOSSY — UNJUSTIFIED** (QF-012 / INV-014) | **Y** | **N** | **N** | N | Y |
| Code vs label / localization | `title_ka`/`title_en`, captions | **NO LOSS** — captions are digest-bound | – | Y | Y | Y | Y |
| Unit | declared `unitRef` per component | **NO LOSS in declaration**; compatibility **NOT ENFORCED** anywhere | Y | N | N | – | – |
| Grain | structure + sorted dimension tuple → `series` identity | **NO LOSS** | – | Y | Y | Y | Y |
| Dimension ordering | `position` in `componentForm` | **NO LOSS** — digest-bound | – | Y | Y | Y | Y |
| Measure/metric identity | wire ref → metric | **LOSSY — UNJUSTIFIED** (CF-018): suffix match + unscoped `dataset_code` | **Y** | N | N | N | Y |
| Status | `observation_status VARCHAR(24)` | **NORMALIZATION**; domain unconstrained (CF-025 class) | Y | N | N | – | – |
| **Attributes** | written, not read back | **LOSSY — UNJUSTIFIED** (CF-005) | **Y** | N | N | N | Y |
| Attachment level | `level + dimensions.sorted() + measure` | **NO LOSS** for group shape; **per-combination value NOT EXPRESSIBLE** (CF-015) | – | Y | Y | – | – |
| Time (business) | `period_start`/`period_end` DATE | **NO LOSS** — interval preserved; lexical form rebuilt, never guessed | – | Y | Y | Y | Y |
| Time (system) | `valid_from`/`valid_to`/`is_current` | **NO LOSS** — bitemporal, separate from business time | – | Y | Y | Y | Y |
| Revision | `revisionDigest` + write-once trigger | **NO LOSS** — PATTERN C | – | Y | Y | Y | Y |
| Source identity | `source_record_id` FK, `artifact.checksum` | **NO LOSS** | – | Y | Y | Y | Y |
| Provenance | `object_uri` + SHA-256 + `received_at` | **NO LOSS** at ingest; **MISSING** for generated artifacts (no generator id) | Y | N | N | – | – |
| Tenant/product scope | product → tenant assignment | **NO LOSS**; but unscoped lookups exist (CF-018 class) | Y | N | N | – | – |
| Raw representation | staged rows → `raw.source_record.payload_json` + hash | **NO LOSS** | – | Y | Y | Y | Y |

**Excluded as non-material with reason:** Unicode (NVARCHAR throughout, no transcoding
step found), case (no case-folding transformation found), row ordering (no semantic row
order outside `ordinal`/`position`, both preserved), duplicates (bounded by PK/UK where
declared; the gap is CF-040's missing uniqueness, already counted).

**INFORMATION_LOSS_MATRIX: COMPLETE for material axes.** Six axes are LOSSY–UNJUSTIFIED
and **all six are already-registered findings** (CF-005, CF-018, CF-039, CF-040, QF-012,
plus legacy numeric). **Every one is SILENT.** That concentration is itself the finding:
the platform's information loss is not scattered — it is five known defects plus one
historical event, and none of them is detected at runtime.

### 1E.2 Identity model — CLOSED

| Object | Identity key | Scope | Lookup risk |
|---|---|---|---|
| product | `product_id` / `product_code` | global | — |
| dataset | `dataset_id` / `dataset_code` | product | **CF-018: unscoped `dataset_code` lookup** |
| dataset version | `dataset_version_id` | dataset | — |
| statistical contract | `draftId`; business key `(productCode, datasetKey)` | product | — |
| site contract | `contract_code` + `revision` | product | approval lookup ignores status (CF-035) |
| contract revision | `site_contract_revision_id` + `contract_checksum` | contract | — |
| entity record | `entity_id`; business `external_key` **NULLABLE** | snapshot | nullable business key |
| entity link | `link_id` + (from, type, to) | snapshot | **no uniqueness (CF-040)** |
| classifier scheme/version/item | `classification_item_id` | scheme+version | — (bound by ID) |
| dimension | `dimension_code` = `namespace.code.version` | global | — |
| metric / measure | `metric_id` ← `measure_id` via `statistical_reference` | dataset | **CF-018: suffix match** |
| unit | `unitRef` wire | registry | — |
| observation | `series_id` + period + `is_current` | snapshot | — |
| snapshot | `dataset_snapshot_id` | dataset version | **status is not identity (CF-025)** |
| publication | `snapshot_id` + `snapshot_member` | product | **RC-001** |
| package | `contract_code`+`revision` in `__gs_package`, **no digest** | — | **DR-007** |
| provider | `provider_code` | global | — |
| metadata subject | `(subject_type, subject_code, subject_revision)` | contract revision | — |

**Defect class (CF-018) instances found: three** — unscoped `dataset_code`, suffix wire
match, and the package's unverified self-reported identity. All three are consequences of
one root pattern: **identity established by matching a string rather than resolving a
scoped key.** No new CF created (GOV §21); recorded as a class with three instances.

**IDENTITY_MODEL: PASS** — understood, with defects named and bounded.

### 1E.3 Version / revision / lifecycle — CLOSED

| Consumer | Binds to | Correct? |
|---|---|---|
| `approvedPlan` | **exact** `revisionDigest` | ✔ |
| Access generation | exact compiled plan | ✔ |
| serving page metadata | `status='APPROVED'` **ORDER BY revision DESC** → latest approved | ✔ intended |
| `ContractPhysicalQueryService` | `lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY revision DESC` | ✔ intended |
| `CanonicalObservationReader` | latest **PUBLISHED** snapshot | ✘ should be membership (CF-022) |
| `canonicalSource` | status set incl. pre-gate | ✘ (CF-021) |
| metadata assertion → schema | **newest revision in namespace** | ✘ (QF-012 / INV-014) |
| `materializeMetadata` → site contract | code+revision, **no status** | ✘ (CF-035) |

**The distinction identity / version / revision / lifecycle-state is modelled correctly
where it was designed and violated where lookup substitutes "latest" or "current" for an
exact binding.** Four violations, all already registered.

**VERSION_REVISION_LIFECYCLE_MODEL: PASS.**

### 1E.4 Provenance — CLOSED

Backward chain, verified: canonical observation → `source_record_id` → `raw.source_record`
(payload + hash) → `dataset_snapshot_id` → `dataset_load` → `ingest.batch` →
`ingest.artifact` (`object_uri`, **SHA-256 `checksum`**, `byte_size`, `received_at`) →
object storage; and separately → `dataset_version` → contract revision → approval receipt.

**Complete for ingested data.** Missing for **generated** artifacts: no generator identity,
generator version, physical-pattern version or plan digest is emitted into the package
(REQ-026, already registered). Classification: **REQUIRED-BUT-MISSING**, not semantic loss.

Provenance and authority remain separate: no provenance record is consulted as a source of
truth. **PROVENANCE_MODEL: PASS.**

### 1E.5 Reprocessability — CLOSED, with a named gap

| Question | Answer |
|---|---|
| Source still available? | **Yes** — `ingest.artifact.object_uri` in object storage |
| Source version identifiable? | **Yes** — SHA-256 `checksum` + `byte_size` |
| Exact contract revision identifiable? | **Yes** — via `dataset_load` → `dataset_version`, and `revisionDigest` for statistical |
| Reprocessing path exists? | **No.** `resumePackage(file, batchId)` resumes a **failed** batch from its stored artifact. No governed "re-ingest under a corrected contract revision" path exists |
| Idempotent? | Yes — and **that is the obstacle**: `StatisticalLoadService` keys idempotency on `artifact checksum + dataset version`, so re-loading the same bytes under the same version is a no-op. A corrected reprocess requires a **new dataset version** |
| Canonical data safely replaceable? | **Unknown** — no replace path inspected |

**The inputs for reprocessing are fully preserved; the mechanism is absent.** This is the
concrete dependency under DR-006's statement that scale-10-truncated values "require reload
from the source artifact" — that reload has **no implementation**.

**→ REQUIRED-BUT-MISSING: governed reprocess-under-corrected-revision path.**
**REPROCESSABILITY: PASS** (understood), the capability itself missing.

### 1E.6 Time semantics — CLOSED, and correct

Distinct concepts, distinct columns, **no collapse found**:

`period_start`/`period_end` DATE = **business/reference time** (interval) · `valid_from`/
`valid_to` DATETIME2 + `is_current` = **system validity** (bitemporal) · `received_at` =
ingestion · `created_at` = record creation · `published_at` = publication ·
`effective_from` = contract effectivity.

The platform is genuinely **bitemporal** on both `observation` and `entity_record`, and
`CanonicalObservationReader.lexicalPeriod` **rebuilds the author's lexical period form from
the stored interval rather than guessing it**. Recorded as a positive.

**TIME_SEMANTICS: PASS.**

### 1E.7 Constraint → enforcement map — CLOSED

| Invariant | Enforcement |
|---|---|
| Identity uniqueness | **DATABASE** (PK/UK) |
| Grain uniqueness (statistical) | **APPLICATION** (writer series identity) + snapshot scope |
| Type / nullability / length — statistical | **DATABASE + CONTRACT + PROVIDER** (reinforcing) |
| **Type / nullability / length — entity** | **NOT ENFORCED** (CF-039) |
| Numeric envelope | **CONTRACT VALIDATION + DATABASE**; provider limit **CONVENTION** (CF-038) |
| Relation referential integrity | **DATABASE** (FKs both ends) |
| **Relation cardinality** | **NOT ENFORCED** (CF-040) |
| Classifier identity | **DATABASE** (FK to `classification_item_id`) |
| **Classifier revision binding** | **NOT ENFORCED** (QF-012 / INV-014) |
| Dimension membership | **CONTRACT VALIDATION** (compiler) |
| **Unit / metric compatibility** | **NOT ENFORCED** — 13 classes reference `unitRef`; **zero** SQL CHECK constraints |
| Duplicate observations | **APPLICATION** (idempotent writer; conflict on changed value) |
| Status validity | **CONVENTION ONLY** (CF-025 class — no CHECK on either status column) |
| Attachment semantics | **CONTRACT VALIDATION** |
| Required fields | **CONTRACT VALIDATION + PROVIDER** |
| Semantic reference immutability | **DATABASE TRIGGER + DIGEST** (PATTERN C — strongest in the platform) |
| Approval integrity | **DIGEST + CAS + LOCK + EVIDENCE** |
| Archive retention window | **APPLICATION**; object-store alignment **CONVENTION ONLY** |

**Five invariants rest on convention or nothing**: entity typing, relation cardinality,
classifier revision binding, unit compatibility, status domains. **CONSTRAINT_PRESERVATION:
PASS** (mapped).

### 1E.8 Provider semantic round-trip — CLOSED

**Semantic**, not byte-for-byte. Declared → Access → read → canonical:

| Aspect | Survives? |
|---|---|
| Types, nullability, keys, relations | **Yes** — typed `__ent_*`/`__stat_*` tables; contract declares, adapter realizes |
| Precision/scale | **Yes** — envelope bounded by the Access ceiling precisely so it can (DR-006) |
| Classifiers | **Yes** outbound (lookup data); **inbound only as DRAFT proposals** (correct) |
| Grain, dimension order | **Yes** — digest-bound |
| Attributes | **Outbound yes; inbound not read back** (CF-005) |
| Identity & revision binding | **No** — `__gs_package` self-reports without a digest (DR-007) |
| Provider metadata (nav pane) | Regenerated, non-semantic (CF-017) |

**PROVIDER_SEMANTIC_ROUND_TRIP: PASS** — understood; two named breaks (CF-005 inbound
attributes, DR-007 unverified correspondence).

### 1E.9 Boundaries, null semantics, multi-presentation, universal model

**Boundaries** — every structure classified with exactly one authority role: RAW
(`raw.source_record`, `ingest.staged_row`) · CANONICAL (`statistics.*`, `entity.*`,
`platform.*` registries) · DERIVED (`serving.metric_cache`, `SemanticPlan`, `PackagePlan`,
`PhysicalPlan`) · PRESENTATION (chart models, projections) · TRANSPORT (`__gs_*`, `.accdb`)
· CONTROL/METADATA (`metadata_*`, contract tables) · AUDIT/EVIDENCE (receipts, gate
evidence, outbox, ledgers). **Two structures currently hold two roles**: `metadata_schema`
(CONTROL that a TRANSPORT writes — CF-033) and `dataset_snapshot.status` (CANONICAL
lifecycle acting as a liveness AUTHORITY — RC-001). Both registered.
**RAW_CANONICAL_DERIVED_PRESENTATION: PASS.**

**Null/missing/unknown** — the platform distinguishes *value absent* from *value absent
with a declared reason*, and the release gate enforces the difference. It does **not**
distinguish unknown / not-applicable / suppressed by constraint. **No recovered requirement
demands that distinction** — section 14 item 5 requires only that "empty is not zero and
status must not be invented", which is satisfied. **NULL_MISSING_UNKNOWN: PASS.**

**Multi-presentation** — one canonical semantic source *can* serve all surfaces, and
`visualization_definition` + `statistical_chart` prove the projection idea works. But four
chart mechanisms with two liveness models (CF-030) mean presentation **is** currently a
second semantic authority. Understood, registered, DR-001 decided.
**MULTI_PRESENTATION_SEMANTICS: PASS** (understood, defective).

**Universal model falsification — SURVIVED.** The requirement is *universal architectural
capability, not universal physical representation*. Evidence **for** it: four families
already coexist with family-appropriate physical models (typed statistical, typed
relational links, typed classifier bindings, document entities) under one contract grammar,
one approval lifecycle, one snapshot/publication model and one serving dispatch. Evidence
**against** the requirement: none found. CF-039 is evidence against the **current
implementation** of one family, not against the requirement — and the fix direction is to
*raise* entity storage to the typing the other three already have, not to collapse all
families into one table.

### 1E.10 Vertical slices — final

**STATISTICAL: PASS.** Every edge classified; defects named (CF-005, CF-018, CF-021,
CF-027, CF-032) — no unexplained edge.

**ENTITY/RELATIONAL: PASS as a recovery result.** Previously FAIL because CF-039/CF-040
were newly found and unbounded. Both now have responsibility, authority, boundary, impact
and canonical requirement established, and the required canonical properties are recorded
as missing components. Per the closure rule, **known defects do not block closure once
understood.** The slice contains no material *unknown* edge.

**CLASSIFIER: PASS.** Proposal boundary correct; promotion absent (CF-016); hierarchy
promotion unknown-but-bounded by the same missing path.

## 1F. PASS 2A GATE — PASS

All thirteen closure conditions of §16 are met. Remaining `PARTIAL` ledger rows belong to
PASS 2B runtime/operational scope and cannot change the recovered **data** architecture:
L5, L9, L16, L17, L25, L26, L40, L43, L44 — their residual uncertainty is runtime wiring,
failure behaviour and operational policy, not data semantics.

**No new CF was created in this pass** (GOV §21): every discovery was an instance of an
existing class or a consequence of a registered finding.

---

## 1G. PASS 2B — runtime, operational and cross-cutting

### 1G.1 Transaction model

**24 annotated transaction boundaries across three managers:** `dataPlaneTransactionManager`
13, `primaryJdbcTransactionManager` 9, `archivePlaneTransactionManager` 2.

| Workflow | Boundary | Cross-plane | Partial-failure behaviour |
|---|---|---|---|
| Contract approval | `primaryJdbc`, single tx | no | atomic; CAS abort |
| Statistical load | own `TransactionTemplate` on data plane | **yes** — Control definitions + object bytes + Data Plane | Control/object survive a later Data Plane failure; definitions idempotent, orphans swept |
| Publication | `dataPlaneTransactionManager` (writer) + separate control-plane intent | **yes** | intent committed independently for retryability |
| **Metadata import** | **none** | no | **partial mutation (CF-033 family)** |
| Snapshot preparation | `dataPlaneTransactionManager` | no | atomic, with row-count assertion |
| Archive purge | `archivePlaneTransactionManager` | no | lease-guarded |

**TRANSACTION_MODEL: PASS.** Cross-plane non-atomicity is **deliberate and documented**
(checkpoint §7.2), with idempotent definitions plus sweep/reconciliation as the
compensation. The single unguarded writer is already registered.

### 1G.2 Concurrency — a correction to my own first read

My first pass through the evidence counted 10 `@Scheduled` jobs against 2 literal
`lease.acquire("…")` matches and inferred that 8 ran unguarded. **That inference was
wrong** — the grep matched only literal-string call sites. Checking each job individually:

| Job | Guard |
|---|---|
| `ArtifactObjectIntegrityAuditService` | lease |
| `ArtifactRelationIntegrityAuditService` | lease |
| `PackageRunWorker` | lease |
| `ArtifactStorageSweepService` | lease |
| `PlatformArchiveRetentionService` | lease (`archive-retention-purge`, 60 s) |
| `PlatformScheduledImportWorker` | lease (`monthly-platform-import`) |
| `PlatformOutboxProcessor` | **CAS claim** — `UPDATE … SET status='PROCESSING' WHERE event_id=? AND status='PENDING'` |
| `PlatformServingCacheService` | idempotent insert guarded by `NOT EXISTS` |
| `PlatformArchivePayloadRepairService` | `NOT EXISTS` |
| `ArtifactUploadSessionService` | CAS on `quotaReleased` (`releaseQuota` returns boolean) |

**10 of 10 scheduled jobs are protected against multi-instance execution.** Add
`UPDLOCK, HOLDLOCK` on contract approval, version CAS throughout `ContractWorkflow`, and
CAS on every snapshot-status transition owned by the gate.

**CONCURRENCY_MODEL: PASS.** This is one of the platform's genuinely strong areas.

### 1G.3 Idempotency

| Command | Class |
|---|---|
| Contract create | **IDEMPOTENT BY IDEMPOTENCY KEY** + request-hash conflict detection |
| Contract approve | **IDEMPOTENT BY DIGEST** — replay returns the receipt |
| Statistical load | **IDEMPOTENT BY CHECKSUM** (artifact + dataset version) |
| Snapshot prepare | **IDEMPOTENT** — returns existing snapshot after provenance assertions |
| Publication | **IDEMPOTENT BY `release_id`** in `publication.snapshot` |
| Outbox processing | **AT-LEAST-ONCE SAFE** via CAS claim + attempts/backoff |
| Metadata merge | **IDEMPOTENT BY KEY** (MERGE), but **not transactional** |
| Classifier proposal | **IDEMPOTENT** — `WHEN NOT MATCHED` only, no UPDATE branch |
| **Corrected reprocess** | **BLOCKED** — checksum idempotency suppresses it (PASS 2A §1E.5) |

**IDEMPOTENCY_MODEL: PASS**, including the explicit distinction the prompt asked for:
duplicate suppression is implemented everywhere; **intentional replay is not a supported
operation**, and the same mechanism that gives the former prevents the latter.

### 1G.4 NEW CF-041 — the outbox carries three incompatible categories

FACT. `platform.outbox_event` carries: `PUBLISH_SNAPSHOT`, `ARCHIVE_PUBLICATION`,
`PUBLISH_RESOURCE` — **command intents** — and `SITE_CONTRACT_REVISION_APPROVED` — a
**domain event**. Batch 8 additionally established that the publication intent is treated
as **audit evidence** of what was released, while CF-023 proves it misstates the effect.

One table therefore serves three roles with different correctness requirements:

| Role | Requirement | Satisfied? |
|---|---|---|
| Command intent | at-least-once delivery, idempotent consumer | yes (CAS + backoff) |
| Domain event | describes what **did** happen, immutable | yes for approval |
| Audit evidence | must not misdescribe the effect | **no** — CF-023 |

- **Why a new CF rather than an instance:** this is an **event-taxonomy** defect affecting
  retry semantics, consumer idempotency and audit correctness. It is architecturally
  distinct from CF-023 (publication scope), though CF-023 is what makes it visible.
- Risk: MEDIUM-HIGH. Confidence: HIGH. Status: ARCHITECTURAL-DEFECT.

**OUTBOX_EVENT_MODEL: PASS** (understood; defect registered).

### 1G.5 Parallel runtime mechanism sweep — the mandatory §24 result

**Ingestion — seven independent implementations**, confirming PA-004 at file level:

| # | Mechanism | Class |
|---|---|---|
| 1 | `PlatformIngestionService` (generic rows) | CANONICAL CANDIDATE |
| 2 | `PlatformAccessIngestionService` (semantic v3) | CANONICAL CANDIDATE |
| 3 | `SemanticAccessPackageIngestionService` | LEGITIMATE SPECIALIZATION |
| 4 | `PlatformSqlIngestionService` (+ `PlatformScheduledImportWorker`) | LEGITIMATE SPECIALIZATION |
| 5 | `ManagedImportExecutionService` + `AccessFileImporter` + `ImportStrategyFactory` / `MySqlImportStrategy` / `SqlServerImportStrategy` / `DatabaseImportStrategy` | **OBSOLETE** (Managed v1 / legacy DB) |
| 6 | `PackageDatasetIngestor` / `AccessPackageDatasetIngestor` / `IngestDatasetStage` | CANONICAL CANDIDATE (run pipeline) |
| 7 | `StatisticalLoadService` | **TEMPORARY MIGRATION** — bypasses the run pipeline (23.19/23.33) |

Other responsibilities swept: publication (1 + the status second authority — RC-001);
approval (2, legitimately distinct per DR-004); serving/query (generic + statistical +
dynamic legacy + SDMX facade — PA-007); export (`SdmxCsvExporter` **orphan**); generation
(`AccessAuthoringAdapter` canonical, `AccessCatalogService` v1 obsolete); migration (runner
+ `alreadyEffective` + allowlist); provider discovery (reader only).

**PARALLEL_RUNTIME_SWEEP: PASS.** No new mechanism class discovered beyond what PA-001…
PA-007 and the Legacy Register already carry; the sweep raised the ingestion count from
"many adapters" to a precise seven with dispositions.

### 1G.6 Multi-datasource

Three qualified datasources (`primaryJdbcTemplate`, `dataPlaneJdbcTemplate`,
`archivePlaneJdbcTemplate`) with matching transaction managers, qualified at every
injection point inspected. **The single unqualified `@Autowired JdbcTemplate` is
`CarsMigrationRunner` (CF-037)** — a dead body, but in a three-datasource application an
unqualified injection is a latent wrong-database write. No second instance found.

**MULTI_DATASOURCE_MODEL: PASS.**

### 1G.7 Startup, migration runtime, clean-build equivalence

Startup order: `PlatformSchemaMigrationRunner` (ledger + checksum + `alreadyEffective`
adoption) → `ProviderCapabilityDiscoveryRunner` (**fail-closed** when enabled with no
ACTIVE capability) → `PlatformStorageProvisioner` → `CarsMigrationRunner` (no-op).

**Startup can mutate semantic state**: the migration runner executes DDL **and** KIDS
declaration seeds (RC-002/RC-003). That is the architectural defect already registered,
observed here in its runtime form. Concurrent multi-instance startup is guarded by the
`schema_migration` ledger insert, but **two instances racing a first-ever migration** were
not proven safe — recorded as a PASS 3 protection requirement, not a new defect.

**CLEAN_BUILD_UPGRADE_EQUIVALENCE: UNPROVEN.** `056_activate_kids_r8_supersede_legacy.sql`
is unregistered (CF-020) and `alreadyEffective()` lets an upgraded database hold effects a
clean build never produces. `ops/tests/sql/migration-chain-fresh-replay.sh` exists and is
cited by the checksum allowlist, but **this audit did not run it** and no evidence of a
current passing run was found. Classified as a **PASS 3 protection requirement**.

**DEPLOYMENT_STARTUP_SAFETY: PASS** · **MIGRATION_RUNTIME: PASS**.

### 1G.8 Failure injection — reasoned from actual boundaries

| Scenario | Invariant survives? |
|---|---|
| Crash after Data Plane write, before outbox | **Yes** — intent is committed first; publication is `release_id`-idempotent |
| Event delivered twice | **Yes** — CAS claim |
| Provider generation fails halfway | **Unknown** — temp-file/atomic-replace behaviour not inspected (**PARTIAL**) |
| Package ingestion fails halfway | **Yes** — batch → `FAILED`, `resumePackage` from the stored artifact |
| Publication writer fails after the first affected snapshot | **Yes for atomicity** (one data-plane tx) — **but the set is wrong to begin with** (CF-023) |
| Rollback retried | **Partially** — `publication.snapshot` converges; `dataset_snapshot.status` never restored |
| Archive payload missing | `PlatformArchivePayloadRepairService` exists — repair path present |
| Two nodes run the same scheduled task | **Yes** — 10/10 guarded (1G.2) |
| Stale provider capability | **Unknown** — no cache invalidation inspected (**PARTIAL**) |

### 1G.9 What PASS 2B did NOT complete

- **§9 security** — authorization boundaries are known from PASS 1 (CF-008, CF-031,
  DR-002), but the **trace-to-data-access sweep was not performed**. `SECURITY_MODEL:
  PARTIAL`.
- **§10 tenant isolation** — tenancy is strong and well tested at the guard level, but the
  **unscoped-lookup sweep across repositories, caches and background tasks was not
  performed**. `TENANT_ISOLATION_MODEL: PARTIAL`.
- **§18 provider failure boundary** — timeouts, temp files, atomic replace, cleanup and
  partial generation not inspected. `PROVIDER_FAILURE_BOUNDARY: PARTIAL`.
- **§19 observability operational adequacy** — L46 established what evidence exists; whether
  an operator can **detect** stuck outbox, partial publication, authority divergence or a
  dead job was not assessed. `OBSERVABILITY_OPERATIONAL: PARTIAL`.
- **§13 caches** — `serving.metric_cache` is membership-resolved and `NOT EXISTS`-guarded;
  invalidation and stale-read risk not traced. `CACHE_MATERIALIZATION_MODEL: PARTIAL`.
- **§16 indexing/lock contention** — not inspected.

These four PARTIAL areas are **runtime-material**: a tenant-scope leak or a provider
half-write would change protection requirements and possibly canonical boundaries.
**PASS 2B cannot close on this evidence.**

---

## 1H. PASS 2B — final runtime closure

### 1H.1 Tenant isolation — swept at both layers

**Controller layer:** 22 controllers carry `@TenantScoped`; 12 do not, and **all twelve are
explained**:

| Group | Controllers | Why unscoped |
|---|---|---|
| Exception handlers | `ArtifactApi`, `ContractApi`, `LegacySurface`, `TenantAccess` | not endpoints |
| Infrastructure | `BuildInfoController`, `HealthController` | carry no tenant data |
| Legacy, gated | `MSSQLToAccess`, `ResponseController`, `XlsxToCsvController`, `ManagedImportController` | all four carry `@LegacySurfaceGate`; `ADR-tenant-scoped-authorization.md` lists them as "NEUTRAL · LEGACY · gated" with no `data_product` binding |
| Tenancy administration | `PlatformProductTenancyController` | it *assigns* tenancy; cannot be scoped by it |
| Deliberately global | `StatisticalGlobalReferenceController` | shared reference vocabulary by design |

**The tenancy ADR already functions as the exemption register** that QF-003 recorded as
missing for controllers. QF-003's residual now applies only to non-controller exemptions.

**Repository layer sweep** — queries selecting by `*_code` with no product/tenant
predicate, classified:

| Query | Classification |
|---|---|
| `contract_namespace` / `metadata_namespace WHERE namespace_code=?` | **SAFE GLOBAL AUTHORITY** |
| `dimension WHERE dimension_code=?` | **GLOBALLY UNIQUE IDENTITY** — `dimension_code` is `namespace.code.version` |
| `site_contract_revision WHERE contract_code=? AND revision=?` | **GLOBALLY UNIQUE IDENTITY** (missing status filter is CF-035, already registered) |
| `contract_page_binding … r.contract_code=? AND r.status='APPROVED'` | **TENANT-SCOPED AND SAFE** |
| `metadata_schema … namespace_code=? ORDER BY s.revision DESC` | **QF-012**, registered |
| **`metric WHERE metric_code=? AND status<>'DRAFT' ORDER BY metric_id DESC`** | **TENANT LEAK RISK** |
| `metric_alias … external_system_code=? AND external_code=?` | **TENANT LEAK RISK** |

**Second confirmed instance of CF-018.** `metric_code` is not namespaced, the query carries
no product or dataset predicate, and `ORDER BY metric_id DESC` resolves ambiguity by
"newest wins". Two products declaring the same metric code collide silently. Same
responsibility (resolve a metric), same failure mode (wrong metric bound), same root
pattern (**identity by string match plus latest-selection**) — therefore recorded as a
**second instance of CF-018, not a new CF**, per §10.

**TENANT_ISOLATION_MODEL: PASS.**

### 1H.2 Security — traced to data access, not stopped at annotations

Enforcement is at the **use-case boundary**, with the HTTP layer as declaration:

| Operation | Data-access-level enforcement |
|---|---|
| Publication / rollback | `tenants.requireProductId(request.productId())` **inside** `PlatformPublicationService`, before any read |
| Snapshot preparation | `tenants.requireDatasetLoad(...)` **inside** the service |
| Contract-table serving | `ServingPolicy.require(revisionId, datasetCode)` **inside** `ContractPhysicalQueryService` — per table, with a required authority from the contract row |
| Statistical contract / chart | `ContractWorkflow.AccessDecision` port — "deny by default", evaluated per call |
| Artifact distribution | `TenantAccessGuard` injected into `ArtifactDistributionService` and `ArtifactPackageContractResolver` |
| Cross-cutting | `TenantScopedAccessInterceptor` + `TenantScopedAccessWebConfiguration` |

This satisfies the standard `ANTI-PATTERNS.md` sets ("annotation check presented as
authorization proof" is rejected): the checks live where the data is fetched. Known
defects — CF-008 (chart read authority), CF-031 (`CanonicalObservationReader` bypasses
`ServingPolicy`) — remain registered, and their ordering constraint is unchanged.

**SECURITY_MODEL: PASS** as recovered architecture.

### 1H.3 Provider failure boundary — the risk class does not apply to generation

**Generation is in-memory and digest-carrying.** `AuthoringFileService` returns
`GeneratedFile(fileName, byte[] content, sha256, revisionDigest)` — not a path. There is no
shared filesystem target, so **no consumer can observe a half-written file**; the byte
array either exists complete with its digest or the operation throws.
**Classification: EFFECTIVELY ATOMIC.**

**Ingestion uses temp files correctly and in the right order:**

```java
File temporary = File.createTempFile("platform-access-", ".accdb");
try { file.transferTo(temporary);
      artifact = storage.storeOriginal(...);   // immutable source persisted FIRST
      return ingestion.ingest(temporary, artifact); }
finally { Files.deleteIfExists(temporary.toPath()); }
```

The **immutable source is stored before any canonical mutation** — which is what makes
PASS 2A's reloadability claim true — and cleanup is in `finally`. The resume path
materializes from object storage into a temp file with the same `finally` discipline.

**PROVIDER_FAILURE_BOUNDARY: PASS.**

### 1H.4 Object store / archive consistency — reconciled

| Mechanism | Javadoc | Kind |
|---|---|---|
| `ArtifactObjectIntegrityAuditService` | *"Bounded, distributed and restartable verification sweep over registered artifact objects"* | DB ↔ object store, **detection** |
| `PlatformArchivePayloadRepairService` | *"Reconciles archive SQL records with immutable S3-compatible payload objects"* | archive ↔ S3, **repair** |
| `ArtifactRelationIntegrityAuditService` | *"Reconciles approved artifact relations for snapshots in bounded, distributed batches"* | relations, **detection** |
| `ArtifactStorageSweepService` | orphan sweep | **repair** |

All four are lease-guarded, bounded and restartable. **OBJECT_STORE_ARCHIVE_CONSISTENCY:
PASS.**

### 1H.5 Reconciliation model — the decisive asymmetry

| Boundary | Reconciliation |
|---|---|
| DB ↔ object store | **YES** — detection + repair, scheduled, lease-guarded |
| Archive record ↔ payload | **YES** — repair service |
| Artifact relations ↔ snapshots | **YES** — audit service |
| **`snapshot_member` ↔ `dataset_snapshot.status`** | **NONE** — no trigger, no constraint, no job (PASS 1 falsification) |
| **Outbox intent ↔ actual effect** | **NONE** (CF-041 / CF-023) |
| **Contract revision ↔ generated artifact** | **NONE** — no digest exists (DR-007) |
| Migration ledger ↔ actual schema | **PARTIAL** — `alreadyEffective()` is an *adoption probe*, not reconciliation |

**This asymmetry is the finding: the platform reconciles what it treats as infrastructure
and does not reconcile what it treats as domain truth.** Every multi-system boundary
involving *files and bytes* has a bounded, restartable reconciler. Every boundary involving
*authority* has none. That is a direct structural explanation for why RC-001 survived
undetected — nothing was ever going to notice it.

**RECONCILIATION_MODEL: PASS** as understanding; three named boundaries have no mechanism.

### 1H.6 Failure / recovery — final

| Workflow | Failure recovery |
|---|---|
| Package ingestion | **RESUME** — batch → `FAILED`, `resumePackage` from the stored artifact |
| Statistical load | **ROLLBACK** (one data-plane tx) + **COMPENSATE** cross-plane (idempotent definitions, orphan sweep) |
| **Metadata ingestion** | **UNDEFINED** — no transaction, no compensation (registered) |
| Snapshot creation | **ROLLBACK**, with row-count assertion |
| Publication | **ROLLBACK** within the data plane; intent retryable by `release_id` |
| Rollback | **PARTIAL** — `publication.snapshot` converges, `dataset_snapshot.status` never restored |
| Outbox | **RETRY** — CAS claim, attempts, 5-minute backoff, `last_error` |
| Archive / purge | **RECONCILE** + repair |
| Provider generation | **ROLLBACK** — in-memory, nothing published on failure |
| Provider ingestion | **RESUME** + temp cleanup |

Searched-for patterns: *partial success reported as success* — **found once**, publication
(CF-023). *Retry repeating unsafe effects* — not found; every retry path is CAS- or
key-guarded. *Evidence without matching state* — **found once**, the outbox publication
intent (CF-041). *State without matching evidence* — **found once**, the collateral
snapshots of CF-023.

**FAILURE_RECOVERY_MODEL: PASS.**

### 1H.7 Cache / materialization

**One cache exists:** `serving.metric_cache`, written by `PlatformServingCacheService`.
Source authority = publication membership (`publication.snapshot` + `snapshot_member`).
Key = `(snapshot_id, metric_id, dimension_signature, period_start)` with the signature a
SHA-256. **Scope: snapshot-scoped**, therefore product- and revision-scoped transitively.
Insert is `NOT EXISTS`-guarded and idempotent. **No TTL and no explicit invalidation** —
but because the key contains `snapshot_id`, a new publication produces **new rows** rather
than stale ones, so structural staleness is prevented by the key rather than by eviction.

Derived plans (`SemanticPlan`, `PackagePlan`, `PhysicalPlan`) are **recomputed per use and
never persisted** (INV-013) — correctly not caches.

**CACHE_MATERIALIZATION_MODEL: PASS.**

### 1H.8 Operational observability adequacy — precise answer

| Failure | Detectable? |
|---|---|
| Failed ingestion | **Yes** — `ingest.batch.status='FAILED'` |
| Stuck / failing outbox | **Yes** — `attempts`, `last_error`, `available_at` |
| Failed archive purge | **Yes** — repair service reports |
| Provider generation failure | **Yes** — operation fails, nothing emitted |
| Migration mismatch | **Yes** — fail-closed checksum exception |
| Approval integrity | **Yes** — receipt + evidence + digest |
| **Partial publication (CF-023)** | **NO** — no reconciliation compares membership to status |
| **Authority divergence (RC-001)** | **NO** — same reason |
| **Event-category ambiguity (CF-041)** | **NO** — the intent looks successful |
| Clean-build vs upgraded divergence | **NO** at runtime |

**Observability is adequate for mechanical failure and inadequate for authority
divergence** — precisely the class of defect this recovery found. Not a new finding: it is
the operational consequence of RC-001, CF-023 and CF-041, and it strengthens the case that
reconciliation is the missing protection.

**OBSERVABILITY_OPERATIONAL: PASS** as understanding; the detectability gap is recorded
against its causes.

### 1H.9 New missing components and automation gaps

**Missing components** (added to the register): **reconciliation between the publication
aggregate and `dataset_snapshot.status`** — the single mechanism whose absence makes RC-001
undetectable. Note it becomes unnecessary if RC-001 is resolved by removing the second
authority; it is required only if both persist.

**Automation gaps:** reconciliation of authority boundaries is **REQUIRED-BUT-MISSING**;
corrected reprocessing is **REQUIRED-BUT-MISSING** (PASS 2A); clean-build/upgrade
equivalence verification is **ASSIST** (script exists, no current passing evidence).

**No parallel mechanisms were found inside the six areas closed here** — one cache, one
tenancy guard, one reconciliation family, one provider boundary.

## 1I. PASS 2B GATE — PASS

All nine closure conditions of §13 are met. `CLEAN_BUILD_UPGRADE_EQUIVALENCE` remains
**UNPROVEN** and is assigned to PASS 3 as an executable protection requirement, per §11 —
it is a *verification* obligation, not an unknown architecture.

**PASS means recovery understanding, not implementation correctness.** CF-023, CF-041,
CF-018, CF-031, CF-037 and the metadata-import transaction gap all remain open defects with
design and migration obligations.

---

## 2. What this pass did NOT audit

The phase specification defines roughly twenty audit dimensions. This pass covered two of
them (CF-004 reopening; one blind-spot search that produced PATTERN-C). The following were
**not** performed, and the baseline must not be read as though they were:

| Dimension | Status |
|---|---|
| §2 Verify the ten INHERITED-UNVERIFIED findings (CF-001, 002, 010, 011, 012, 015, 017, 018, 019, 020) | **NOT AUDITED** |
| §4 Full repository blind-spot search | **PARTIAL** — one targeted search only |
| §5 / §6 Top-down and bottom-up gap searches | **NOT AUDITED** in this pass |
| §7 Authority collision search | **NOT AUDITED** |
| §8 Producer/consumer audit for every canonical registry | **NOT AUDITED** |
| §9 Data-model completeness (26 semantic concepts) | **NOT AUDITED** |
| §10 Raw/canonical/derived/presentation boundary audit | **NOT AUDITED** |
| §11 Information-loss matrix, all axes | **NOT AUDITED** — remains the outstanding obligation |
| §12 Regression-protection model | **NOT AUDITED** |
| §13 Architectural fitness functions | **NOT AUDITED** |
| §14 Automation blind-spot audit | **NOT AUDITED** |
| §15 Dynamicity blind-spot audit | **NOT AUDITED** |
| §16 Performance/scale constraints | **NOT AUDITED** |
| §17 Failure/recovery semantics beyond those already recorded | **NOT AUDITED** |
| §18 Security/governance trace to data access | **NOT AUDITED** |
| §19 Tests as architectural evidence (~62 unclassified files) | **NOT AUDITED** |
| §20 Documentation vs implementation sweep | **PARTIAL** — done for the Meta-Contract layer in batch 10 |
| §22 Requirement completeness challenge | **NOT AUDITED** |
| §23 Orphan/dead/shadow architecture search | **PARTIAL** — 12 orphans already recorded |
| §24 / §25 Root-cause and decision falsification | **NOT AUDITED** in this pass |

**Reason:** session context budget was exhausted. Under the standing strict-mode rule
("if approaching a context limit, prioritize checkpoint integrity"), the remaining budget
was spent closing the single highest-value item — the baseline's own declared weakest
link — and recording this honest coverage map, rather than producing twenty thin sections
that would read as coverage without being it.

---

## 3. Audit report fields

```text
INHERITED_FINDINGS:
  CF-001: REJECTED   — the file is ADR-012; no collision exists
  CF-002: REVISED    — the register contains its own dated refinement (line 316);
                       residual is an unrestated table row, not a contradiction
  CF-010: CONFIRMED  — provider_capability carries ONE quantitative limit
                       (max_page_size); no precision/scale/column/name dimension
  CF-011: CONFIRMED  — 022 mixes 12 DDL guards with 41 KIDS references;
                       109 is a generic deny-by-default mechanism + 2 seed rows
  CF-012: CONFIRMED + REVISED — 10 documented exceptions, fail-closed default;
                                corrects this audit's own earlier count of 5
  CF-015: PARTIAL    — three attachment levels exist and the digest binds the
                       group's dimension set, contradicting "dataset-constant";
                       constant-value-per-combination unresolved
  CF-017: REJECTED   — nav pane is populated (34/3/37/45 rows) and the generator
                       writes it deliberately; provider/UI presentation metadata only
  CF-018: CONFIRMED  — suffix match on wire ref + dataset_code lookup with no
                       product scoping; two independent failure modes
  CF-019: REVISED    — two trim sites, not a global policy; one is decimal
                       parsing, one is a deliberate trim+empty->null helper
  CF-020: CONFIRMED  — 113/108/6; plus a third state (adoption probe)

INHERITED_FINDINGS_VERIFIED:    10 of 10 — PASS
CF004_VERIFICATION:             PASS — CONFIRMED, inference upgraded to FACT
RC001_FALSIFICATION:            SURVIVED — no trigger, no constraint, no outbox
                                reconciliation, no scheduled job; a known writer
                                actively breaks equivalence (CF-023)
RC002_FALSIFICATION:            SURVIVED — declarations are hand-authored (0 of 113
                                migrations carry a generated marker; no generator
                                exists); alreadyEffective() refines and strengthens it
RC003_FALSIFICATION:            SURVIVED — five independent search axes; confidence HIGH+
CANONICAL_REGISTRY_PRODUCER_COVERAGE: COMPLETE for declaration registries
PRODUCER_CONSUMER_SWEEP:        PASS
AUTHORITY_COLLISION_SWEEP:      PASS — 14 responsibilities classified; 5 accidental
                                second authorities; none left UNKNOWN
NEW_MATERIAL_FINDINGS:          PATTERN-C (digest + DB write-once trigger);
                                CF-036 (migrations 111/112 cite the wrong ADR);
                                CF-037 (CarsMigrationRunner shadow startup bean);
                                CF-038 (provider limits have three authorities)
NEW_REQUIREMENTS:               none identified
NEW_AUTHORITIES_FOUND:          none — the sweep found the opposite
NEW_MISSING_PRODUCERS:          platform.provider_capability (4th RC-003 instance)
NEW_ORPHANS:                    CarsMigrationRunner; platform.provider_capability
NEW_MISSING_COMPONENTS:         none beyond the new missing producer
NEW_KNOWN_GAPS:                 none; one existing KNOWN-GAP closed (CF-004 inference gap)
PATTERN_C_RECORDED:             YES (consolidation §3)
NEW_DEPENDENCY:                 DR-006's "move the envelope into provider capability"
                                depends on RC-003 remediation — that registry has no
                                producer either
INFORMATION_LOSS_MATRIX:        PARTIAL (numeric axis only — unchanged)
REGRESSION_PROTECTION_MODEL:    PARTIAL (raw material complete; model not drafted)
ARCHITECTURAL_FITNESS_MODEL:    PARTIAL (candidates listed in the checkpoint; not drafted)
AUTOMATION_BASELINE:            PARTIAL (current state recorded; blind-spot audit not done)
DYNAMICITY_BASELINE:            NOT STARTED
PERFORMANCE_CONSTRAINTS:        PARTIAL (CF-027, QF-001, DR-006 overflow constraint recorded)
FAILURE_SEMANTICS:              PARTIAL (publication, metadata ingestion, classifier import
                                characterized; no systematic sweep)
SECURITY_GOVERNANCE:            PARTIAL (tenancy strong and tested; CF-008/CF-031 recorded;
                                no trace-to-data-access sweep)
ROOT_CAUSE_FALSIFICATION:       NOT PERFORMED this pass
MAJOR_DECISION_FALSIFICATION:   NOT PERFORMED this pass

PASS_1_MATERIAL_UNKNOWNS:       none. Two findings carry bounded, stated residuals
                                (CF-015 constant-value-per-combination; CF-019 whether any
                                BoundedText field needs significant whitespace) — neither
                                can change canonical architecture

DECISION_PROVENANCE_DRIFT:      CF-029 + CF-036 — grouped under RC-002 as a third
                                symptom: migrations carry narrative nothing validates
SHADOW_BYPASS_SURFACES:         CarsMigrationRunner only; ProviderCapabilityDiscoveryRunner
                                is dormant-by-config but FAILS CLOSED (not a bypass)

PASS_1_GATE:                    PASS
COMPLETENESS_GATE:              FAIL
RECOVERY STATUS:                INCOMPLETE
```

### 3A. Remaining evidence targets for THIS pass only

1. **CF-011** — read migrations `109` and `022` for KIDS-specific content in generic
   schema migrations. Cheapest of the seven and feeds RC-002.
2. **CF-002** — the Q11 mutation-mode contradiction in
   `docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md` versus
   `StatisticalLoadService`'s `FULL_SNAPSHOT`-only behaviour (already observed in
   section 7.2 of the checkpoint — needs only the document side read).
3. **CF-010** — inspect the `platform.provider_capability` schema for a max-scale
   dimension. Now doubly relevant: DR-006 depends on it.
4. **CF-019** — `WideRowNormalizer` global trim; determine whether any bounded-text or
   lineage field is affected.
5. **CF-018** — reader reference reconstruction in `CanonicalObservationReader:47-53`.
6. **CF-015** — `DIMENSION_GROUP` attachment level in the compiler's attachment model.
7. **CF-017** — Access navigation portability; the R8 artifact is already inspectable
   read-only, so this is testable.
8. **RC-002 falsification** — search for a governed lifecycle around the checksum
   allowlist and for any documented architecture unifying schema evolution with semantic
   declaration.
9. **RC-001 falsification** — search for any synchronisation mechanism keeping
   `publication.snapshot_member` and `publication.dataset_snapshot.status` provably
   equivalent (outbox handlers, scheduled jobs, triggers).
10. **Authority-collision sweep** — the 15 responsibilities listed in the phase
    specification.

---

## 4. Exact next starting points, in priority order

1. **Verify the ten inherited findings** against primary evidence, cheapest first:
   CF-001 (ADR-011 collision — two files, direct read), CF-012 (checksum-rewrite list in
   `PlatformSchemaMigrationRunner` — already partly observed: 5 exception filenames),
   CF-020 (already partly verified in batch 9: 113 files / 108 registrations / 6
   unregistered), CF-011 (migrations 109 and 022), then CF-002, CF-010, CF-015, CF-017,
   CF-018, CF-019. Propagate any change through RC / DR / INV / REQ / lineage.
2. **Root-cause and decision falsification** (§24, §25) — attempt to disprove RC-001,
   RC-002, RC-003 and DR-001/003/004/005/006/007/008. RC-003 is the one whose falsification
   matters most, because the entire missing-component register depends on it.
3. **Producer/consumer and authority-collision sweeps** (§7, §8) — these are cheap
   (behavioural greps over write verbs per canonical table) and are the most likely source
   of a hidden authority.
4. **Information-loss matrix, all axes** (§11) — the one explicitly outstanding obligation
   carried since batch 9.
5. **Regression-protection model and fitness functions** (§12, §13) — the raw material is
   complete in checkpoint sections 23.37 and 23.39; these are drafting tasks, not
   discovery.
6. **Dynamicity baseline** (§15) — what adding a site / dataset / field / classifier /
   dimension / metric / page / chart currently requires. This is the most direct evidence
   for the canonical design that follows and has not been started.

---

## 5. Standing conclusion

**RECOVERY STATUS: INCOMPLETE.**

The recovery's substantive architectural discovery is in a strong state: three root causes,
thirty-five findings, eight decisions, fourteen invariants and three proven in-repository
patterns, with the lineage mapped in both directions and every material authority either
identified or proven absent. CF-004 — the last self-declared inference — is now fact.

But the phase that was asked for here is an adversarial audit whose entire purpose is to
assume something was missed, and this pass did not execute enough of it to conclude that
nothing was. Declaring completion on the strength of one closed item would be precisely the
failure this engagement has avoided at every previous gate.

**Remaining work is still partly DISCOVERY, not yet only design and implementation.**
