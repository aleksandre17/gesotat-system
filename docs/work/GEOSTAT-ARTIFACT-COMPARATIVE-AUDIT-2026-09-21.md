---
id: AUD-COMPARATIVE
type: REPORT
title: Artifact comparative audit
status: COMPLETE
authority: CANONICAL
scope: identity, responsibility, multi-level comparison, semantic diff, loss and innovation analysis of the candidate artifacts
owner: PHASE-003
created: 2026-09-21
updated: 2026-09-21
related: AUD-CAPABILITY, STD-BENCH-001, REC-CONSOLIDATION, DOC-CAD, REC-PASS3
---

# GEOSTAT — ARTIFACT COMPARATIVE AUDIT

**`PHASE-003`, guarded by `GATE-COMPARATIVE`.** Every artifact here is **evidence**. None is
architecture authority, and none is selected as canonical — that is `PHASE-004`'s decision and
this document deliberately does not make it.

**Owns:** artifact identity and provenance (§1), responsibility classification (§2), the
multi-level comparison (§3–§14), the semantic structural diff (§15), loss analysis (§16),
innovation analysis (§17), falsification (§18), benchmark traceability (§19), unresolved
questions (§20), gate evidence (§21).
**Does not own:** the capability register and dispositions, or the `PHASE-004` input package —
those are `AUD-CAPABILITY`. Findings, requirements and invariants remain owned by
`REC-CONSOLIDATION`; authority classification by `DOC-CAD`; evaluation criteria by
`STD-BENCH-001`. Nothing here restates them; rows cite them.

---

## 1. Artifact identity and provenance

Inspected **read-only**: every Access file was copied to a scratch directory and opened with
`DatabaseBuilder.setReadOnly(true)` (Jackcess 4.0.7, the same library the platform's own
generator uses). **No source artifact was modified**; the digests below were taken before and
after inspection and are unchanged.

| ID | Artifact | SHA-256 | Bytes | Declared identity (`__gs_package` / manifest) |
|---|---|---|---|---|
| `A-LEGACY` | `samples/managed-access-package-pilot.accdb` | `af4efd17…d447145` | 450 560 | Managed Access Package v1 pilot; `__gs_package` carries 2 columns only |
| `A-R7` | `samples/kids-portal-v1-canonical-r7.accdb` | `c2556600…8a5d9a77` | 1 941 504 | contract `KIDS_PORTAL_V1` rev 7, package 6.0.0 |
| `A-R8` | `samples/kids-portal-v1-canonical-r8-final.accdb` | `746487ce…be6cb8211` | 2 953 216 | `KIDS_PORTAL`/`KIDS_PORTAL_V1` rev **8**, package **8.0.0**, `SNAPSHOT`, profile `ACCESS_CANONICAL_R8_PAGE_MANIFEST` |
| `A-R8-API` | `platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb` | `926fedfc…400cc1a12` | 2 953 216 | **identical declared identity to `A-R8`** |
| `A-R8-BUILD` | `samples/kids-portal-v1-canonical-r8-build-8.0.1.accdb` | `99840f3f…3e00c17469` | 2 953 216 | same, package **8.0.1** |
| `A-PKG` | `samples/kids-r8-resource-package-8.0.1.zip` | `427d12e9…f5414f4cc` | 7 629 698 | manifest `geostat.artifact-package-manifest.v1`, contract `KIDS_PORTAL_V1` rev 8, dataset `KIDS_RESOURCE` |
| `A-CAND2` | `samples/KIDS_PACKAGE_candidate_2.accdb` | `979ebe5d…cc15ba25` | 4 915 200 | contract revision **`8+CANDIDATE`**, package **`8.0.0+CANDIDATE`**, profile `ACCESS_PACKAGE_COMPOSER_CANDIDATE` |

**Provenance authority:** `samples/README.md` (repository-owned) states what each sample is and
publishes `A-R8`'s SHA-256. **That published digest verifies exactly** — one of the few
artifact-level integrity claims in this repository that survives checking.

### 1.1 An identity defect found while establishing identity

`A-R8` and `A-R8-API` **share a filename and a declared identity but not their bytes**. Both
declare `contract_revision 8`, `package_version 8.0.0`, the same profile. A full structural
and metadata comparison (24 tables, every column, every index, every relationship, every
`__gs_*` row) shows **no semantic difference whatsoever** — the differing bytes are Access
container state, not content.

`A-R8-BUILD` versus `A-R8` differs in **exactly one row value**: `__gs_package.package_version`
`8.0.0` → `8.0.1`. The version stamp is surgical, and nothing else moved.

**Consequence, and it is architectural, not cosmetic:** a byte digest of an `.accdb` is **not**
a valid correspondence proof. Two files can be byte-different and semantically identical, and
(nothing prevents the converse) byte-identical files cannot occur across independent builds. The
package manifest hashes bytes, which is correct for the 450 immutable resource files and
**insufficient for the Access artifact itself**. This is `DR-007` and `REQ-028` observed
directly rather than inferred. See `CAP-M01`.

### 1.2 Artifacts named in repository context but not inspected

`REC-CONSOLIDATION` §15 also lists ~62 unclassified test files and `frontend/web` /
`backend/mobile`. They are **protection-coverage and elimination-sequencing inputs, not
representations of the data model**, and are out of this audit's responsibility (they belong to
`PHASE-009` and `PHASE-011`). Recorded here so their absence is a decision, not an oversight.

**`KIDS_PACKAGE_candidate_3` is not in the repository.** It is referenced by the Access-package
work stream, which runs outside this lifecycle (`TASK-002`, `DEF-08`). No substitution was made:
this audit compares what repository authority names, and records the gap rather than reaching
outside for a file the control plane does not govern.

---

## 2. Responsibility classification — applied before any comparison

**The fairness rule (`PHASE-003` §32) decides half the conclusions in this document.** An
artifact is judged against the responsibility it actually holds.

| ID | Intended responsibility | Therefore judged on | Explicitly **not** judged on |
|---|---|---|---|
| `A-LEGACY` | transport of one dataset plus a chart definition into a managed import | whether the import contract is expressible | absence of canonical semantics |
| `A-R7` / `A-R8` | **authoring and round-trip artifact** for an approved contract revision: a human fills it, the platform reads it back | source fidelity, self-description, integrity within the file, provider usability | not being canonical persistence; not owning approval |
| `A-PKG` | **interchange contract** — the artifact plus every file its rows reference | self-description, per-file integrity, declared edges, portability | not containing canonical semantics |
| `A-CAND2` | **candidate authoring artifact** for the same responsibility as `A-R8`, composed by the package composer | the same criteria as `A-R8`, plus what it adds | not being deployed; `8+CANDIDATE` is honest about its status |

`A-PKG` is classified **`INTERCHANGE CONTRACT`**, not `DATA ARCHIVE` and not
`EXECUTABLE RESOURCE PACKAGE`: it carries a schema identity, a contract binding, per-file
digests and a declared row→file edge set, and it carries no executable element.

---

## 3. Structural baseline — measured

| Measure | `A-LEGACY` | `A-R7` | `A-R8` | `A-CAND2` |
|---|---|---|---|---|
| Tables | 5 | 21 | 24 | **32** |
| Tables with a primary key | **0** | 21 | 24 | 32 |
| Distinct relationships (all with referential integrity) | **0** | 21 | 21 | **36** |
| Non-PK unique indexes | 0 | 31 | 31 | **41** |
| Site-named tables in *generic* families (`__stat_`/`__raw_`/`__gs_`) | 1 plain | 7 plain | **3** | **0** |
| Declared families present | `__gs_` only | `__gs_ __cl_ __raw_ __stat_` + plain | `__gs_ __cl_ __ent_ __rel_ __raw_ __stat_` | same six |
| Statistical facts carried | 3 | 880 | 880 | 880 |

`A-LEGACY` has **no key and no relationship of any kind** — 5 tables, zero integrity. It is the
`WEAK DYNAMICITY` extreme in its purest form, and it is also the ancestor of the chart-authority
problem: `__gs_chart` declares `chart_type`, `x_field`, `y_field`, `series_field`, `aggregation`
and `publication_mode` **inside the transport artifact**.

`A-R7 → A-R8` is a transport-namespace migration: `kids_goal` → `__ent_kids_goal` etc., plus
`__gs_metadata` (267 rows), `__gs_page`, `__gs_metadata_schema`, and `__raw_document` widened
from 4 columns to 22. Integrity is unchanged (21 relationships both).

---

## 4. Semantic model

The central structural difference, stated in the artifacts' **own declared grain**:

| | `A-R8` | `A-CAND2` |
|---|---|---|
| Statistical table | `__stat_kids_statistical_input` | `__stat_observation` |
| Declared grain (`__gs_dataset.business_grain`) | *"one parsed source statistical cell"* | *"one observation per period, age and indicator"* |
| Primary key | `(carrier_code, cell_ordinal)` — a **position inside a carrier** | `(TIME_PERIOD, AGE, INDICATOR)` — the **dimension tuple** |
| Value column | `value_decimal DOUBLE` + `value_lexical MEMO` | `OBS_VALUE NUMERIC(28,16)` |
| Dimensions | `period_normalized`, `age_group_item_ref`, `dimension_key_raw` (MEMO/JSON) | `TIME_PERIOD`, `AGE`, `INDICATOR` as columns |
| Structural metadata | none in-file; semantics live in `__stat_metric` + `__rel_kids_statistical_semantic_binding` per carrier | a full DSD: `__stat_structure`, `__stat_component` (role/concept/representation/position), `__stat_measure`, `__stat_representation` |

**`A-R8`'s statistical family is a parse model, not an observation model — and it says so.** Its
grain declaration is faithful to its key. The finding is therefore *not* that R8 misrepresents
its grain; it is that a **raw-plane grain is carried in the `__stat_` family**, so the artifact
cannot express *what a fact is* without the server-side binding tables.

**`A-CAND2` expresses meaning, not only values.** `__stat_component` declares each component's
role (`DIMENSION` / `MEASURE` / `ATTRIBUTE`), concept reference, representation and position;
`__stat_representation` declares `logical_type`, `codelist_version_ref`, `numeric_precision` and
`numeric_scale`. `rep:DECIMAL_28_16` is the declaration envelope of `DR-006`, expressed as
metadata and materialised as `NUMERIC(28,16)`.

### 4.1 The capability `PHASE-001` recorded as missing, found implemented

`REC-CONSOLIDATION` §9 lists **"per-combination constant value declaration — declare a constant
that varies by dimension combination (e.g. unit per INDICATOR) without per-observation
repetition"** as `REQUIRED-BUT-MISSING` (`CF-015`).

`A-CAND2` implements it:

```text
__stat_attribute_attachment:  UNIT_MEASURE  attachment_level=DIMENSION_GROUP  target=INDICATOR
                              OBS_STATUS    attachment_level=MEASURE          target=OBS_VALUE
                              CONF_STATUS   attachment_level=DATASET          target=(null)
__stat_attribute_value:       43 rows, one per INDICATOR, each carrying unit_ref
                              + 1 dataset-level CONF_STATUS
```

Ten distinct units across 43 indicators are carried in 44 rows instead of 880 repetitions, with
`__stat_unit(unit_ref)` enforcing the reference. **This is `CF-015` closed by an artifact, and
it is the single most valuable discovery of this audit** (`CAP-021`, `CAP-022`).

---

## 5. Grain and identity

| Test (`BM-INV-24`, `BM-INV-10`) | `A-R8` | `A-CAND2` |
|---|---|---|
| Grain declared | yes, in prose | yes, in prose |
| Grain enforced by a **business-key** constraint | **no** — PK is positional | **yes** — PK *is* the dimension tuple |
| Can two rows assert the same fact? | **yes, structurally** | no |
| Do they, in the data? | **no** — 0 duplicate `(carrier, period, age)` triples in 880 rows | not possible |
| Business identity == physical identity | no (`input_key` surrogate + positional PK) | yes |
| Identity stable across imports/providers | positional keys shift if a source cell moves | dimension tuple is source-position independent |

**`BM-INV-24`: `A-R8` FAILS (unenforced, not violated); `A-CAND2` HOLDS.** The distinction
matters for `PHASE-004`: R8's model is not producing wrong data today, and an architecture that
inherits its key shape would have no defence the day a source file is re-ordered.

**Versioned identity.** `A-CAND2` uses `kind:agency:CODE(version)` references throughout —
`dsd:STAT:INDICATORS_BY_AGE(1.0.0)`, `concept:STAT:AGE(1.0.0)`, `unit:STAT:PERCENT(1.0.0)`,
`rep:DECIMAL_28_16` — and binds representations to an exact codelist version
(`__cl_version → __stat_representation.codelist_version_ref`, FK-enforced). `A-R8` uses bare
codes (`unit_code`, `metric_code`). This is `BM-INV-06` visible in the artifact layer.

---

## 6. Type and constraint integrity

**The typed-EAV test (`BM-EC-021`) applied to both.** Neither artifact stores declared fields as
attribute rows *in its data families* — both use real columns with real types, so both pass at
the data plane.

**`A-CAND2` fails it at the metadata plane, and `A-R8` fails it less because it asserts less.**
R8 carries per-metric semantics as **typed columns with foreign keys**
(`__rel_kids_statistical_semantic_binding.unit_code → __stat_unit`, FK-enforced;
`__stat_metric.unit_code → __stat_unit`, FK-enforced). `A-CAND2` relocates those same facts into
`__gs_metadata` as `(subject_type, subject_code, namespace, property_code, language_tag,
ordinal) → value` rows:

| `__gs_metadata` namespace/property | `A-R8` rows | `A-CAND2` rows |
|---|---|---|
| `CORE/title` | 34 | 112 |
| `SEMANTIC/unitCode` · `aggregation` · `measureComponent` · `obsStatus` · `confStatus` | 6 + 5 | 43 each (215) |
| `PROVENANCE/inferenceRationale` · `inferenceConfidence` · `inferenceMethod` | 5 | 43 each (129) |
| `GOVERNANCE/lifecycleState` · `operation` · `approvalState` | 0 | 43 + 43 + 1 |
| `METHODOLOGY/denominatorText`, `PROVENANCE/standardReference`, `SEMANTIC/mapping`, `PRESENTATION/projectionFamily` | 0 | 5 + 4 + 6 + 6 |
| **total** | **267** | **575** |

`__gs_metadata_schema` declares **two** schemas in both artifacts: `CORE_RESOURCE_V1`
(namespace `CORE`) and `VISUALIZATION_V1` (namespace `PRESENTATION`). **The `SEMANTIC`,
`PROVENANCE`, `GOVERNANCE` and `METHODOLOGY` namespaces have no schema in either artifact.**

**`OBSERVATION → CONCLUSION`:** a defect shared by both artifacts, *amplified* by the newer one.
`A-R8` puts 11 assertions into unschema'd namespaces; `A-CAND2` puts **386**. No constraint,
no validation and no schema governs any of them. A value that was FK-checked in R8
(`unit_code → __stat_unit`) is, in candidate_2, a text row whose only integrity is that a
generator wrote it — while the *same* fact is simultaneously held, correctly and FK-enforced, in
`__stat_attribute_value.unit_ref`. **That is two representations of one fact inside one file**
(`CAD-01`, `BM-INV-01`). Disposition: `CAP-030` `REJECT` the metadata-assertion route for facts
the typed model already owns; `CAP-M03` for the missing schemas.

---

## 7. Relationship model

| Property | `A-LEGACY` | `A-R8` | `A-CAND2` |
|---|---|---|---|
| Relationships | 0 | 21 | 36 |
| Referential integrity | — | 21/21 | 36/36 |
| Cascade delete/update | — | 0 | 0 |
| Declared cardinalities (`__gs_relation`) | — | 18 `MANY_TO_ONE`, **3 `ONE_TO_ONE`** | 32 `MANY_TO_ONE` |
| 1:1 **enforced physically** | — | **3** (unique index on the FK side) | 0 (the 1:1 pairs no longer exist) |
| `MANY_TO_MANY` declared | — | **never** | **never** |
| Contract-metadata plane has its own RI | — | **no relationships among `__gs_*` at all** | **`__gs_dataset → __gs_field → __gs_key`, `__gs_dataset → __gs_relation(from/to)`** |
| Dimension values FK-checked against codelists | — | **no** (`age_group_item_ref` is free text) | **yes** — `__cl_item → __stat_observation(AGE)`, `(INDICATOR)`, `(OBS_STATUS)` |

Three distinct kinds of relationship are visible, and the audit keeps them apart:

- **relationship as semantic fact** — `__gs_relation` rows with `cardinality` and `required`;
- **relationship as physical foreign key** — the Access relationship graph;
- **relationship as generic metadata edge** — `__gs_metadata` subject references, and the
  manifest's `edges[]`.

`A-R8` is the only artifact where a declared cardinality (`ONE_TO_ONE`) is *also* physically
enforced — evidence that the Access provider **can** enforce declared cardinality, which
`CF-040` reports as unenforced on the server side. The provider is ahead of the platform here.

**`MANY_TO_MANY` is representable but not declarable.** `__rel_kids_resource_indicator`
(43 rows, PK `(source_resource_id, indicator_item_ref)`) *is* a many-to-many link, declared as
two `MANY_TO_ONE` relations. The concept survives as decomposition; the *semantics* of an M:N
relation — ownership, lifecycle, whether the link itself carries attributes — cannot be
declared. `CAP-M10`.

---

## 8. Classifier / codelist model

| Property | `A-R8` | `A-CAND2` |
|---|---|---|
| Schemes | 4 | **6** (adds `INDICATOR`, `OBS_STATUS`) |
| Versions | 4 (`lifecycle_state=DRAFT`, `valid_from`/`valid_to`) | 6, same shape |
| Items | 36 | **85** |
| Aliases | 36 | **122** |
| `authority_mode` on a scheme | **`PROPOSAL`** | `PROPOSAL` |
| `standard_reference` | ISO/IEC 11179; (none SDMX) | adds **`SDMX cross-domain concept INDICATOR`**, **`SDMX CL_OBS_STATUS`** |
| Hierarchy table | present, **0 rows** | present, **0 rows**; 0 items carry a parent |

**`authority_mode = PROPOSAL` is an R8 capability, not a candidate_2 invention** — a hypothesis
this audit formed and then falsified (§18, H-3). The artifact declares that its classifiers are
**proposals, not authority**, which is `REQ-022` and `PATTERN B` expressed inside the transport.
It is the strongest single answer to `CF-033`/`CF-034` found in any artifact, and it was already
there.

**What candidate_2 changes is the *scope* of classification**: R8's 43 metrics are a *table*
(`__stat_metric`); candidate_2's 43 indicators are **codelist items** under an `INDICATOR`
scheme with an SDMX standard reference, referenced by observations through a foreign key. The
metric ceases to be a bespoke entity and becomes a governed vocabulary member.

**Hierarchy is declared by both and exercised by neither.** A structural capability with zero
evidence of correctness. `CAP-007` keeps the capability and records it as unproven.

---

## 9. Raw / canonical / derived

| Plane | `A-R8` | `A-CAND2` |
|---|---|---|
| Source envelope | `__raw_document` (22 cols: checksum, mime, retention, confidentiality, `supersedes_source_row_key`, parser/validation status) | identical |
| Parse layer | `__raw_kids_statistical_carrier` (checksum, parse_status) | `__raw_fragment` (checksum, parse_status, locator_kind) — **site name removed** |
| Derivation record | **none** | `__raw_source_record` — 880 rows, `derivation_kind`, `activity_ref`, `fragment_ref`, `target_dataset_code`, `target_key`, `source_lexical`, `source_ordinal`, `recorded_at`, **`invalidated_at`** |
| Agent / activity | **none** | `__raw_activity` — `activity_kind`, `agent`, `software`, `started_at` |
| Declared grain of the derivation record | — | *"one derivation: a row of this package and where it came from (**prov:wasDerivedFrom**)"* |
| Canonical facts re-derivable from raw inside the artifact | **no** | **yes** — 880 raw records ↔ 880 observations, FK-chained |

`A-CAND2` carries a **PROV-O-shaped lineage chain** with referential integrity:
`__raw_activity → __raw_source_record → __raw_fragment → __raw_document`, plus
`__gs_dataset → __raw_source_record.target_dataset_code`. This is the difference between
*timestamps* and *lineage* that `BM-EC-040`/`§20` asks for, and it satisfies `BM-INV-23`
(raw retained, canonical re-derivable) **inside a single transport artifact** — which no other
artifact in this repository does.

---

## 10. Contract fidelity

`SEMANTIC DEFINITION ↔ LOGICAL CONTRACT ↔ PHYSICAL REPRESENTATION`, tested in both:

| Check | `A-R8` | `A-CAND2` |
|---|---|---|
| `__gs_dataset` row per physical table | 15 rows / 24 tables — the 9 `__gs_*` tables are not listed | 32 rows / 32 tables — every table is listed |
| `__gs_field` rows for the **`__gs_*` tables themselves** | none | **none — 23 of 32 datasets have field rows; the nine `__gs_*` datasets have none** |
| `__gs_field` covers every column | 122 fields | 159 fields |
| Field declares type / role / required | `logical_type`, `semantic_role`, `required` | same **+ `authoring_class`** |
| Field declares length, precision, nullability, domain | **no** | no (except statistical, via `__stat_representation`) |
| Contract metadata itself protected by RI | **no** | **yes** |
| Artifact carries a digest binding it to its contract | **no** | **no** |
| Artifact carries generator identity/version | **no** | **no** |
| Artifact asserts approval state | **yes** — `__gs_projection.mapping_json` carries `approvalState`, `approvedBy`, `approvalBasis`; `__gs_metadata_schema.approval_state='READY'` | **yes**, same, plus `GOVERNANCE/approvalState` metadata |

**Both artifacts assert governance state they cannot own** (`CF-033`/`CF-034`, `DR-007`). This is
the clearest example in the audit of a property that must **not** carry forward: the transport
declaring `approvedBy` and `approvalBasis` is the platform reading its own approval from the file
being approved. `CAP-011` `REPLACE`.

**Correction to this audit, 2026-09-21 (design-entry review).** The first version of this
section said `A-CAND2` declares *"every table, including the metadata tables"* and called the
artifact self-complete. **Measured again: 32 of 32 datasets are listed, but only 23 carry field
declarations — the nine `__gs_*` datasets describe every other table and not themselves.** The
improvement over `A-R8` is real (32 listed vs 15, plus referential integrity across the
contract plane) and it is **not** self-completeness. The error was generosity in wording, and it
was caught by cross-checking against `docs/work/ACCESS-PACKAGE-CANONICAL-STANDARD.md` §A3-1,
which measured the same nine-dataset gap on the sibling artifact `candidate_3` — a document
outside this phase's `required_context`, belonging to the other programme.

**That corroboration is itself evidence, and it cuts both ways.** Its structural measurements of
`candidate_3` — 32 tables, 36 enforced relationships, 880 observations exact against R8's
lexical source, 1 structure / 3 dimensions / 1 measure / 3 attributes, 10 units over 43
indicators — match this audit's independent measurements of `candidate_2` exactly. Two
independent readings of two sibling artifacts agreeing to the row is the strongest confirmation
available here. It also means **this audit's conclusions transfer to `candidate_3`**, which the
repository does not govern, and that the nine-dataset self-description gap persists in the newer
file.

---

## 11. Package and interchange (`A-PKG`)

**Measured, not asserted.** Every claim below was verified by recomputation:

| Property | Result |
|---|---|
| Entries | 452 = 1 `.accdb` + 450 resource files + `manifest.json` |
| Manifest schema identity | `geostat.artifact-package-manifest.v1` |
| Contract binding | `contractCode KIDS_PORTAL_V1`, `contractRevision 8`, `datasetCode KIDS_RESOURCE`, `datasetEntry` names the `.accdb` |
| Per-file digests | **451 declared, 451 recomputed and matched, 0 mismatches, 0 missing, 0 undeclared entries** |
| Embedded `.accdb` digest | `99840f3f…` — matches `A-R8-BUILD` exactly |
| Declared edges | 450, **0 dangling**; `relationCode PRIMARY_FILE`; `language` 225 `en` + 225 `ka`; 225 distinct `rowKey`, max 2 files per row |
| Row correspondence | 225 `rowKey`s ↔ `__ent_kids_resource` 225 rows |
| Executable content | none |

**`A-PKG` is the strongest artifact in this audit at its own responsibility.** It is
self-describing, integrity-checkable without the platform, portable, and its row→file edges
carry language and ordinal. The gap is the one §1.1 names: file-level byte integrity is proven,
**contract-level semantic correspondence is not** — the manifest pins *which bytes*, never
*which meaning*.

---

## 12. Access as a provider

> *If Access did not exist, would we still choose this canonical semantic design?*

| Property | Verdict |
|---|---|
| Tables, typed columns, PK, FK with referential integrity, unique indexes, **enforced 1:1** | **Genuinely good.** 21–36 relationships enforced, 31–41 unique indexes. Access is a real relational provider here, not a spreadsheet |
| `NUMERIC(28,16)` | supported and used by `A-CAND2` — the 28-digit ceiling is a real provider limit and the reason `DR-006` bounds *declarations* at (28,16) |
| Navigation-pane grouping | provider presentation metadata, derived from the plan (`MSysNavPaneGroups` present in every canonical artifact) — write-only, no semantics |
| Local usability and inspectability | the reason the authoring workflow exists at all; a domain author opens one file |
| `MEMO` for JSON (`dimension_key_raw`, `mapping_json`, `payload_reference`) | **provider-shaped escape hatch**; every one of them is a place where semantics left the typed model |
| Byte-level reproducibility | **absent** — §1.1 |
| System artifacts (`MSysACEs`, `MSysComplexType_*`, `f_12D7…_Data`) | provider noise; must never reach canonical semantics |

**Answer to the test:** the dimension-keyed observation model, exact decimal, codelist FKs and
the PROV-O chain would all be chosen **without** Access. The carrier/cell positional model, the
28-digit ceiling and the `MEMO`-JSON columns exist **because of** the provider and its history.
Those three are isolated as provider-specific in `AUD-CAPABILITY`.

---

## 13. Provider agnosticism — the lowest-common-denominator test (`BM-EC-085`)

For every capability the artifacts lack, the reason must be a requirement, not a provider limit.

| Capability absent | Reason | Verdict |
|---|---|---|
| Precision beyond 28 digits | Access ceiling; **canonical storage is a strict superset (`DECIMAL(38,16)` server-side)** and only the *declaration* is bounded | **legitimate** — the `DR-006` shape, exactly what `BM-EC-085` permits |
| `CHECK` constraints / domains | Access supports validation rules; the generator emits none | **provider capability left unused**, not an LCD decision — recorded as `CAP-M11` |
| Cascade rules | none declared anywhere | deliberate (deletion is governed, not cascaded) — no evidence of LCD |
| M:N as a declared concept | not a provider limit — Access represents it fine by decomposition | model gap, `CAP-M10` |

**No lowest-common-denominator distortion was found.** The one place where the provider shapes
canonical semantics — numeric precision — follows the permitted `declaration ≤ provider ≤
canonical` pattern. This is the audit's answer to `BM-Q-02`'s risk and it is a genuinely good
result for the existing design.

---

## 14. Automation, generation and validation symmetry

| Question | `A-R8` | `A-CAND2` | `A-PKG` |
|---|---|---|---|
| Human-authored content | the filled data cells | the filled data cells | the 450 source files |
| Generated content | every table, key, relation, classifier, metadata row | same, plus the DSD | the manifest |
| Declared *who fills what* | **no** | **yes — `authoring_class` per dataset** (`SYSTEM` 22, `FILL` 6, `PICK` 1, `LINEAGE` 4) and **per component** (`FILL`/`PICK`/`AUTO`) | n/a |
| Runtime-interpreted semantics | `dimension_key_raw` JSON, `mapping_json` | `mapping_json`, unschema'd `__gs_metadata` | none |
| Regeneration proven | structurally (§1.1) | not tested here | **assembly is deterministic per `samples/README.md`; digests verify** |
| Generator identity in the artifact | **absent** | **absent** | manifest names no generator either |
| Validation symmetric with generation | the reader validates the package **against itself**, never against the contract (`REC-COVERAGE` L23/L24) | same | **the manifest is fully self-verifying — the only symmetric case found** |

`authoring_class` is the automation model (`STD-AUTO-001`) expressed *inside the artifact*:
`SYSTEM` ≈ `AUTOMATE`, `PICK` ≈ `ASSIST` over a governed vocabulary, `FILL` ≈ `HUMAN DECISION`,
`LINEAGE` ≈ `AUTOMATE` with provenance. That a transport artifact can declare this is a
significant, reusable idea (`CAP-027`).

**Asymmetry found:** generation produces far more than validation can check. Nothing validates
`__gs_metadata` assertions (no schema for 4 of 6 namespaces), nothing validates declared
cardinality, nothing validates the DSD against the contract. `CAP-M01`, `CAP-M03`.

---

## 15. Semantic structural diff

`CONCEPT → A-LEGACY → A-R8 → A-CAND2 → recovered requirement → benchmark implication`

| Concept | `A-LEGACY` | `A-R8` | `A-CAND2` | Requirement | Benchmark |
|---|---|---|---|---|---|
| Fact | row in a business table, no key | `(carrier, cell_ordinal)` parsed cell | `(TIME_PERIOD, AGE, INDICATOR)` observation | `REQ-019` | `BM-INV-24`: only candidate_2 holds |
| Value | untyped column | `DOUBLE` + lexical text | `NUMERIC(28,16)` | `REQ-005`, `DR-006` | `BM-INV-09`: R8 structurally at risk, candidate_2 exact |
| Dimension | implicit in column names | 2 columns + JSON `MEMO` | 3 FK-checked columns | `REQ-017`, `REQ-019` | `BM-EC-003` |
| Measure | `y_field` in a chart row | `measure_component_code` text | `__stat_measure` with representation + aggregation | `REQ-019` | `BM-EC-003` |
| Unit | absent | `unit_code` FK per metric | `unit_ref` per indicator via `DIMENSION_GROUP` attachment | `REQ-019`, `CF-015` | `BM-EC-005/006` |
| Observation status | absent | one constant column per carrier | `OBS_STATUS` per observation, FK to codelist | `INV-007` | `BM-EC-004` |
| Classifier | absent | 4 schemes, `PROPOSAL` | 6 schemes incl. SDMX-referenced | `REQ-018` | `BM-EC-007` |
| Hierarchy | absent | table, 0 rows | table, 0 rows | `REQ-018` | `BM-EC-007` — unproven in all |
| Relationship | absent | 21 FK, 3 enforced 1:1 | 36 FK, none 1:1 | `REQ-013`, `REQ-029` | `BM-EC-011` |
| Raw → canonical | absent | envelope + carrier, no derivation link | full PROV-O chain, 880↔880 | `REQ-028` | `BM-INV-23`: only candidate_2 |
| Structural metadata | absent | none in-file | DSD (structure/component/representation) | `REQ-001`, `REQ-019` | `BM-EC-014` |
| Presentation | chart row in the package | 6 pages + 6 projections with approval state | 6 pages + 6 projections | `REQ-020` | `BM-INV-15`, `CAP-011` |
| Package identity | `package_code` only | code + version, **no digest** | same, `+CANDIDATE` suffix | `REQ-026/028` | `BM-INV-07`: all fail |
| Interchange | none | none | none | `REQ-028` | `A-PKG` holds this alone, and holds it well |

---

## 16. Loss analysis

Every capability present on the left and absent on the right, classified. **Each row was
checked against the data, not assumed.**

### `A-LEGACY → A-R8`

| Capability | Classification | Evidence |
|---|---|---|
| Chart declaration inside the package (`__gs_chart`) | **INTENTIONAL IMPROVEMENT** | presentation moved to `__gs_page`/`__gs_projection`; chart authority is `DR-001`'s subject, and a transport should not declare charts |
| `row_key` grain as a simple column list (`country_id,year`) | **REGRESSION, then partially recovered** | R8 replaced it with prose `business_grain`; candidate_2 keeps prose. A machine-checkable grain declaration existed in v1 and has not existed since — `CAP-M04` |
| Zero integrity | INTENTIONAL IMPROVEMENT | 0 → 21 enforced relationships |

### `A-R7 → A-R8`

| Capability | Classification | Evidence |
|---|---|---|
| Plain business table names | SAFE SIMPLIFICATION | renamed into `__ent_`/`__rel_` families; same rows, same keys |
| `__raw_document` 4 columns | INTENTIONAL IMPROVEMENT | widened to 22 (checksum, retention, confidentiality, supersession) |
| — | NOT APPLICABLE | no capability was lost; 21 relationships, 880 facts, 43 carriers preserved |

### `A-R8 → A-CAND2` — the decisive pairing

| Capability on the left | Classification | Evidence |
|---|---|---|
| 880 statistical facts | **NOT LOST** | 880 ↔ 880, **value multisets identical** (recomputed as exact decimals) |
| Value fidelity | **INTENTIONAL IMPROVEMENT** | `DOUBLE` → `NUMERIC(28,16)`; and R8's lexical/DOUBLE round trip was lossless for all 880 today, so the risk was latent, not realised |
| Unit per metric (10 distinct across 43) | **NOT LOST — improved** | every unit preserved with identical distribution, via `DIMENSION_GROUP` attachment instead of a denormalised column |
| `aggregation`, `obs_status`, `conf_status`, `quality_policy_code`, `confidentiality_policy_code`, `inference_method` per carrier | **SAFE SIMPLIFICATION** | **verified constant across all 43 carriers** (`NONE`, `A`, `F`, `KIDS_AGGREGATE_QUALITY_V1`, `KIDS_PUBLIC_AGGREGATE_V1`, `TITLE_RANGE_STANDARD_INFERENCE`); collapsing to dataset-level attachment and `__stat_rule_binding` loses nothing **while the values stay constant** — if a future dataset varies them, the attachment level must move, which the model supports |
| `inference_confidence` (varies 0.85–0.99), `inference_rationale`, `lifecycle_state`, titles | **NOT LOST — relocated** | all 43 present in `__gs_metadata` (`PROVENANCE`, `GOVERNANCE`, `CORE`) — but relocated **from typed FK-checked columns into unvalidated assertions** (§6). Classified **UNRESOLVED**: the information survives, its integrity does not |
| `__stat_metric` as an entity with FK to `__ent_kids_resource` | **INTENTIONAL IMPROVEMENT** | the metric becomes a governed codelist item; the resource↔indicator link becomes `__rel_kids_resource_indicator` (43 rows), which is the semantically honest shape |
| 3 physically enforced `ONE_TO_ONE` relationships | **NOT APPLICABLE** | the 1:1 pairs (`resource↔carrier`, `resource↔metric`, `carrier↔binding`) no longer exist as concepts |
| Carrier/cell positional model | **INTENTIONAL IMPROVEMENT** | replaced by the dimension tuple; the parse position survives in `__raw_source_record.source_ordinal` — **preserved on the raw plane where it belongs** |

**One `UNRESOLVED` loss**, and it is the audit's main warning: candidate_2 modernises the
statistical plane and simultaneously **weakens the metric-attribute plane** by moving typed,
FK-checked facts into schemaless metadata rows. Both directions happened in one artifact.

---

## 17. Innovation analysis — ideas present in only one artifact

| Innovation | Source | Requirement fit | Complexity | Verdict |
|---|---|---|---|---|
| Attribute **attachment level** (`DATASET`/`DIMENSION_GROUP`/`MEASURE`) + per-combination values | `A-CAND2` | closes `CF-015`, a recorded `REQUIRED-BUT-MISSING` | low — 2 small tables | **keep; highest-value find** |
| `authoring_class` as declared metadata, dataset and component level | `A-CAND2` | `REQ-011/012`, `BM-QA-11`, `STD-AUTO-001` | very low — one column | **keep** |
| PROV-O raw chain with `derivation_kind` + `invalidated_at` | `A-CAND2` | `REQ-028`, `BM-INV-23` | moderate — 3 tables | **keep** |
| `authority_mode = PROPOSAL` + `standard_reference` on a classifier scheme | **`A-R8`** | `REQ-022`, `DR-007`, `CF-016` | negligible — 2 columns | **keep; already present, do not lose it** |
| Per-file `sha256` + declared `edges[]` with language/ordinal in a manifest | `A-PKG` | `REQ-028`, `BM-EC-046/047` | low | **keep** |
| Lexical + parsed value pair | `A-R7`/`A-R8` | why 880/880 are exact | negligible | **keep the principle** (retain source lexical form), drop `DOUBLE` |
| Physically enforced `ONE_TO_ONE` from a declared cardinality | `A-R8` | `CF-040` says the server cannot do this; the provider already does | negligible | **keep the principle** |
| Inference confidence + rationale recorded next to an inferred semantic | `A-R8`, scaled in `A-CAND2` | `MSA-R2` forbids inference; where it happened anyway, the rationale is the only mitigation | low | **keep as a control**, not as a licence to infer |

**An idea appearing in one artifact is not thereby weaker.** Five of these eight exist in exactly
one artifact, and four of them are the best answers found to recovered requirements.

---

## 18. Falsification

| # | Attractive hypothesis | Attempt | Result |
|---|---|---|---|
| H-1 | *"candidate_2 is more relational than R8."* | counted relationships, unique indexes, FK coverage, **and** looked for places where it is *less* relational | **REVISED.** 36 vs 21 relationships and FK-checked dimensions — but its metric attributes moved from FK-checked columns to unconstrained metadata rows. **More relational on the observation plane, less on the metric plane.** Confidence: HIGH |
| H-2 | *"R8 does not preserve grain."* | read `business_grain`, compared with the PK, then searched the 880 rows for duplicate semantic triples | **REVISED.** R8's declared grain is *"one parsed source statistical cell"* and its PK matches it exactly; 0 duplicates exist. R8 is internally consistent and carries a **raw-plane grain in a `__stat_` family** — a different, fairer finding. Confidence: HIGH |
| H-3 | *"candidate_2 introduced declared classifier authority (`PROPOSAL`)."* | read `__cl_scheme` in both | **FALSIFIED.** R8 already carries `authority_mode = PROPOSAL` and `standard_reference`. Candidate_2 inherits and extends it. Confidence: HIGH |
| H-4 | *"Access generation is deterministic."* | compared two files with identical declared identity, then the version-stamped build | **SPLIT.** **Semantically** deterministic — `A-R8` and `A-R8-API` are byte-different and structurally/metadata identical; `A-R8-BUILD` differs in exactly one cell. **Byte-level determinism does not hold**, so a byte digest cannot serve as artifact↔contract correspondence. Confidence: HIGH |
| H-5 | *"The package manifest is a sufficient integrity mechanism."* | recomputed all 451 digests and every edge | **PARTIALLY CONFIRMED.** 451/451 verified, 0 dangling edges — excellent for *files*. It binds a contract code and revision but no plan digest, so it cannot prove the `.accdb` **means** what the contract says. Confidence: HIGH |
| H-6 | *"The metadata mechanism is universal."* | mapped every namespace to a declared schema | **FALSIFIED.** 4 of 6 namespaces carrying 386 assertions in candidate_2 have no schema at all. It is not universal; it is unvalidated. Confidence: HIGH |
| H-7 | *"The newer artifact is the better starting point."* | §16 loss analysis, per capability | **NOT SUPPORTED AS STATED.** Candidate_2 is stronger on grain, typing, lineage, classification and authoring declaration; weaker on metric-attribute integrity; equal on approval leakage and generator provenance. **No artifact dominates.** Confidence: HIGH |
| H-8 | *"Provider limits have distorted the canonical model."* | `BM-EC-085` test over every absent capability | **NOT SUPPORTED.** The single provider-driven bound (28 digits) follows the permitted `declaration ≤ provider ≤ canonical` shape. Confidence: MEDIUM-HIGH — one provider was examined |

---

## 19. Benchmark traceability

Scored with `STD-BENCH-001` §2: invariant gate first, binary; then evidence-bound criteria.
**This is an artifact audit, not an architecture verdict** — an artifact cannot hold an
invariant that belongs to the server, and rows below are marked `N/A` where the responsibility
is not the artifact's (`§32` fairness rule).

| Invariant | `A-LEGACY` | `A-R8` | `A-CAND2` | `A-PKG` |
|---|---|---|---|---|
| `BM-INV-05` typed storage, constraints enforced | **VIOLATED** | HOLDS (data plane) · **VIOLATED** (metadata plane, 11 rows) | HOLDS (data plane) · **VIOLATED** (metadata plane, 386 rows) | N/A |
| `BM-INV-06` scoped, version-pinned identity | VIOLATED | **NOT DEMONSTRATED** — bare codes | **HOLDS** — versioned refs + FK to codelist version | HOLDS (contract code + revision) |
| `BM-INV-07` derived artifact names source, generator, digest | VIOLATED | **VIOLATED** | **VIOLATED** | **NOT DEMONSTRATED** — digests yes, generator no |
| `BM-INV-08` transport never defines or approves canonical semantics | VIOLATED (declares charts) | **VIOLATED** (`approvalState`, `approvedBy`) | **VIOLATED** (same + `GOVERNANCE/approvalState`) | HOLDS |
| `BM-INV-09` exact decimal, bounded by provider capability | N/A | **VIOLATED** — `DOUBLE` | **HOLDS** — `NUMERIC(28,16)` | N/A |
| `BM-INV-10` one observation per tuple + measure | VIOLATED | **VIOLATED** — positional key | **HOLDS** | N/A |
| `BM-INV-23` raw retained, canonical re-derivable | VIOLATED | **NOT DEMONSTRATED** — no derivation link | **HOLDS** — 880↔880 chained | HOLDS (immutable files + digests) |
| `BM-INV-24` declared grain enforced by business key | VIOLATED | **VIOLATED** | **HOLDS** | N/A |
| `BM-INV-21` grammar parity | N/A | N/A | N/A | **HOLDS** — manifest self-verifies |

| Criterion | `A-R8` | `A-CAND2` | Evidence |
|---|---|---|---|
| `BM-EC-002` grain declared and constrained | FAIL | **PASS** | §5 |
| `BM-EC-004` observation status constrained | PARTIAL | **PASS** | FK to `CL_OBS_STATUS` |
| `BM-EC-006` constant varying by combination | **FAIL** | **PASS** | §4.1 |
| `BM-EC-007` versioned codelists, exact-version resolution | PARTIAL | **PASS** | `codelist_version_ref` FK |
| `BM-EC-011` M:N first-class with cardinality | PARTIAL | PARTIAL | §7 — declarable in neither |
| `BM-EC-012` raw/canonical/derived distinct, re-derivable | PARTIAL | **PASS** | §9 |
| `BM-EC-017` referential integrity in the database | PASS | **PASS** | 21 / 36 |
| `BM-EC-021` no EAV, incl. typed disguise | **PARTIAL** | **FAIL** | §6 — 386 unvalidated assertions |
| `BM-EC-039` every registry write producer-scoped | FAIL | FAIL | both assert approval |
| `BM-EC-040` backward chain terminates at a declaration | FAIL | **PASS** | §9 |
| `BM-EC-046` package self-description | N/A | N/A | **PASS** for `A-PKG` |
| `BM-EC-047` correspondence by deterministic digest | FAIL | FAIL | FAIL — byte digests only |
| `BM-EC-048` deterministic regeneration | PARTIAL | UNKNOWN | §1.1, H-4 |
| `BM-EC-085` no provider-capability floor | PASS | PASS | §13 |
| `BM-EC-086` specialization boundary declared | PARTIAL | PARTIAL | `data_family` declared; no rule says which family owns a borderline dataset |

**No artifact passes the invariant gate.** Under `STD-BENCH-001` §2.1 that means **no artifact
may be adopted as the canonical model as it stands** — which is the expected and correct result
for a comparative audit, and is precisely why `PHASE-004` exists.

---

## 20. Performance and scale — structural only

`BM-Q-03` is unresolved, so **no performance objective is invented and nothing is claimed as
measured.** Vocabulary per `PHASE-003` §23:

| Risk | Classification | Basis |
|---|---|---|
| `__gs_metadata` scan for every semantic lookup (575 rows here; grows per metric × property × language) | **STRUCTURALLY LIKELY** | a 6-column composite PK; any property lookup is a prefix scan. At KIDS scale it is irrelevant; the shape is what scales badly |
| Dimension FK checks on every observation insert | **STRUCTURALLY LIKELY**, benign | 880 rows; cost is per-row index probe |
| `MEMO` JSON parsing (`dimension_key_raw`, `mapping_json`) | **STRUCTURALLY LIKELY** | per-row parse on read |
| Package regeneration cost | **UNKNOWN** | not measured; 7.6 MB / 452 entries assembles in one pass |
| Access file size | **MEASURED** | 2.95 MB (R8) → 4.92 MB (candidate_2) for the same 880 facts: **+67 % for the added structure**, itself a real cost |
| Join depth for one served value | **MEASURED, structural** | R8: observation → carrier → binding → metric → unit = 4 hops. candidate_2: observation → attribute_value → unit = 2 hops, plus codelist FKs |
| Aggregation over a dimension | **UNKNOWN** in both; neither artifact is a query engine | — |

---

## 21. `GATE-COMPARATIVE` exit evidence

| Exit test | Result |
|---|---|
| All required real artifacts inspected | **7 artifacts, 5 Access files + 1 ZIP + 1 duplicate-identity file**; §1 |
| Responsibilities correctly distinguished before comparison | §2, applied throughout; 14 rows marked `N/A` on that basis |
| Comparison used the `PHASE-002` benchmark | §19, invariant gate first per `STD-BENCH-001` §2 |
| Semantic **and** structural differences captured | §15 diff; §3–§14 per level |
| Capabilities mined, not only defects | `AUD-CAPABILITY` — 30 present + 11 missing |
| Regressions and losses checked | §16, per capability, verified against data |
| Unique innovations evaluated | §17 — 8, of which 4 are the best answers found |
| Required-but-missing identified | `AUD-CAPABILITY` §3 — 11 |
| Provider-specific concerns isolated | §12, §13; `AUD-CAPABILITY` §4 |
| No artifact silently selected as canonical | §19 — none passes the invariant gate; no selection made |
| No `PHASE-004` architecture designed | dispositions are capability-level; no schema, class, table or persistence choice appears in either artifact |
| Durable knowledge materialized | this document + `AUD-CAPABILITY`; `REC-CONSOLIDATION` §15 updated to point here |
| No parallel authority introduced | two new artifacts, both catalogued with distinct ownership; every finding cites its existing owner |

### 21.1 The final adversarial question

> *What important architectural knowledge in these artifacts would be lost if the source
> artifacts disappeared immediately after this audit?*

Four things were checked and captured; one is honestly incomplete:

1. **The attachment-level mechanism** — captured structurally (`CAP-021`/`CAP-022`, §4.1) with
   the exact attachment levels and the 44-row shape.
2. **The PROV-O chain** — captured as table shapes, FK chain and the 880↔880 correspondence.
3. **The verified integrity of `A-PKG`** — captured as a recomputed result, not a claim.
4. **The constancy of the 43 semantic bindings** — captured as measured distributions, which is
   what makes the `SAFE SIMPLIFICATION` verdict falsifiable later.
5. **Incomplete:** the **row-level content** of the 450 resource files, the 178 glossary entries
   and the 225 resource rows is *not* captured here, and should not be — this audit is about
   architecture, and that content is data whose authority is the artifacts and the database.
   **Recorded as a bounded statement rather than a hidden gap:** if the artifacts disappeared,
   the *data* would be lost; the *architectural knowledge* in them would not.

---

## 22. Unresolved comparative questions

| ID | Question | Why it is not answerable here | Owner |
|---|---|---|---|
| `AQ-01` | Should the metric-attribute facts return to typed columns, or should the metadata plane gain schemas and constraints? | both are legitimate designs; choosing is `PHASE-004` | `PHASE-004` |
| `AQ-02` | Is one DSD per package sufficient, or must a package carry several structures and measures? | every artifact carries exactly one structure and one measure — the multi-structure case is **unproven by evidence**, not decided against | `PHASE-004` |
| `AQ-03` | Does the classifier hierarchy work? | declared by every artifact, exercised by none (0 rows, 0 parents) | `PHASE-004` + a dataset that needs it |
| `AQ-04` | Should `authoring_class` be contract metadata or a projection concern? | it is a genuinely new concept with no owner in `CAD-02` | `PHASE-004` |
| ~~`AQ-05`~~ | ~~Is `A-R8-API` a stale copy, a deliberate deployment artifact, or a defect?~~ | **RESOLVED 2026-09-21** — see §23. Authority attaches to the content `746487ce…`, proven by commit, published digest and two dated runtime records; the api-path file is a gitignored working copy whose bytes no evidence binds to anything | — |

**`BM-Q` sensitivity, as `TASK-003` requires.** `BM-Q-01` (SDMX conformance level) is the only
open question whose answer would change a verdict in this audit: if structural SDMX conformance
were required, candidate_2's DSD moves from *"a strong idea"* to *"the obligation"*, and R8's
statistical family moves from *"a raw-plane grain"* to *"non-conformant"*. Every other verdict
here is stable under all answers to `BM-Q-02…08`.

---

## 23. `AQ-05` resolved — provenance is recoverable, and it is content-addressed

**Question:** which of the two byte-different files named `kids-portal-v1-canonical-r8-final.accdb`
is authoritative? **Semantic equivalence proves nothing about authority**, so identity had to be
established from provenance, not from content, path, filename or modification time.

**Evidence, repository-only:**

| Fact | Source |
|---|---|
| `samples/kids-portal-v1-canonical-r8-final.accdb` is **git-tracked, committed and clean** | `git log` → `d4ec057`; `git status` reports no modification |
| `platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb` is **ignored by `.gitignore:148` (`*.accdb`) and unknown to git** | `git check-ignore -v`, `git ls-files --error-unmatch` fails |
| The digest `746487ce…` is published as the artifact's identity | `samples/README.md` |
| `746487ce…` is bound to a **dated runtime record of contract-bound package admission** | `docs/evidence/kids-r8-contract-bound-package-admission-runtime-2026-09-18.json` — `"source": "samples/kids-portal-v1-canonical-r8-final.accdb"`, `"sourceSha256": "746487ce…"` |
| `746487ce…` is bound to a **second, independent dated runtime record** of live artifact binding | `docs/evidence/kids-r8-artifact-binding-live-runtime-2026-09-18.json` — and it names the file as `platform/apps/geostat/backend/api/… (local, untracked)` with that same digest |
| `926fedfc…` — the bytes at the api path **today** — is referenced by **nothing** in the repository | full-text digest sweep |

**Conclusion (HIGH confidence).** The authoritative artifact is **the content whose digest is
`746487ce…`**. On 2026-09-18 that content sat at *both* paths; the api path is a gitignored
workspace location that has since been overwritten by a different build of the same meaning,
recorded nowhere. `A-R8-API` is therefore **an unrecorded working copy, not a second authority
and not a defect in the artifact** — the defect is that a location was allowed to stand in for
an identity.

**The resolution proves the architectural point rather than merely settling a file.** The only
reason the artifact is still identifiable is that a **digest** was recorded next to the path. The
path lied within two days; the digest did not. This is `BM-INV-22` (*content identity is the
checksum; a locator is never identity*) demonstrated end to end, and it is the strongest
available argument for `CAP-M01`: correspondence must be carried by the artifact, because the
filesystem will not carry it.

**Residual, stated precisely:** *why* the api-path bytes changed on 2026-09-20 is **not
recoverable from the repository** and is not worth recovering — no decision depends on it. The
provenance question is closed; the incident is not investigated.
