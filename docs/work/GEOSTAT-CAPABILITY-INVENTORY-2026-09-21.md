---
id: AUD-CAPABILITY
type: REGISTER
title: Artifact capability inventory and canonical-design input package
status: COMPLETE
authority: CANONICAL
scope: CAP-### capability register with dispositions, the capability envelope, and the evidence package PHASE-004 consumes
owner: PHASE-003
created: 2026-09-21
updated: 2026-09-21
related: AUD-COMPARATIVE, STD-BENCH-001, REC-CONSOLIDATION, REC-PASS3
---

# CAPABILITY INVENTORY AND CANONICAL-DESIGN INPUT PACKAGE

**Owns:** the `CAP-###` capability register with dispositions (§2), required-but-missing
capabilities (§3, §14), provider-specific isolation (§4), the `PHASE-004` input package (§5),
and — added by `TASK-005` — the **capability envelope** (§6), the metadata responsibility model
(§7), the raw→published chain coverage (§8), the relationship space (§9), logical/physical
separation (§10), the two governed-chain hypotheses and their verdicts (§11), blind-spot
findings (§12) and the falsification scenarios (§13).
**Does not own:** artifact identity, comparison, diff, loss and falsification — `AUD-COMPARATIVE`;
findings and requirements — `REC-CONSOLIDATION`; authority — `DOC-CAD`; criteria —
`STD-BENCH-001`.

**This is not the Canonical Architecture.** It is the evidence the Canonical Architecture will
consume. Every disposition is at the level of *capability or principle*. Where a row could only
be honoured by choosing a schema, a class or a persistence strategy, the row stops and says so.

---

## 1. How to read a disposition

| Disposition | Means |
|---|---|
| `KEEP` | the capability or principle must survive into the target. **Never "copy the implementation unchanged."** |
| `EVOLVE` | the capability is right and its current form is not sufficient |
| `MERGE` | two or more mechanisms express one responsibility and must converge |
| `REPLACE` | the responsibility is real; this mechanism is the wrong home for it |
| `REJECT` | the mechanism must not carry forward |
| `LEGITIMATE-SPECIALIZATION` | correct precisely because it is narrow; must stay behind its boundary |
| `REQUIRED-BUT-MISSING` | the benchmark or recovered intent needs it and **no artifact provides it** |

Evidence column refers to `AUD-COMPARATIVE` sections; every row is traceable to a measurement,
not to an impression.

---

## 2. Capability register

### 2.1 Structure, integrity and contract self-description

| ID | Capability | Source | Problem it solves | Weakness as it stands | Benchmark | Disposition |
|---|---|---|---|---|---|---|
| `CAP-001` | Transport namespace families `__gs_ __cl_ __ent_ __rel_ __raw_ __stat_` | `A-R7`→ | an operator sees which family a table belongs to without reading a contract | a prefix is a convention, not a boundary; `A-R8` still puts a site name inside generic families (3 tables) | `BM-EC-009` | `KEEP` |
| `CAP-002` | In-artifact contract self-description (`__gs_dataset/field/key/relation`) | `A-R7`→ | the artifact explains itself without the server | `A-R8` describes 15 of 24 tables and has **no RI at all** across `__gs_*`; `A-CAND2` describes 32 of 32 **with** RI | `BM-EC-030`, `BM-EC-039` | `EVOLVE` |
| `CAP-003` | Declared business grain per dataset | `A-R7`→ | states what one row means | **prose only** in every artifact; `A-LEGACY` had a machine-readable `row_key` and it was lost | `BM-INV-24`, `BM-EC-002` | `EVOLVE` |
| `CAP-004` | Primary key and enforced referential integrity on every table | `A-R7`→ | integrity below the contract | none material; 21 / 36 relationships, all with RI, 0 cascades | `BM-INV-05`, `BM-EC-017` | `KEEP` |
| `CAP-029` | Referential integrity across the **contract-metadata** plane itself | `A-CAND2` | the self-description cannot describe a dataset that does not exist | absent in `A-R8` | `BM-EC-039` | `KEEP` |
| `CAP-031` | Declared `ONE_TO_ONE` cardinality **physically enforced** | `A-R8` | proves a declared cardinality can be enforced by a provider — `CF-040` reports the server cannot | only 1:1 and `MANY_TO_ONE`; `MANY_TO_MANY` is never declared | `BM-EC-011` | `KEEP` |
| `CAP-032` | Six-family data classification (`data_family`) | `A-R7`→ | the family decides the physical pattern and the authoring shape | no rule states which family owns a borderline dataset | `BM-EC-086` | `KEEP` |

### 2.2 Semantic and statistical model

| ID | Capability | Source | Problem it solves | Weakness | Benchmark | Disposition |
|---|---|---|---|---|---|---|
| `CAP-019` | Structural metadata (DSD): `structure` · `component` (role/concept/representation/position) · `measure` | `A-CAND2` | the artifact can state *what a fact means*, not only store values | one structure and one measure only — the multi-structure case is unproven | `BM-EC-014`, `BM-EC-003` | `KEEP` |
| `CAP-020` | Observation keyed by the **dimension tuple**, value `NUMERIC(28,16)` | `A-CAND2` | semantic identity == physical identity; exact decimal | none found; 880 facts, values identical to `A-R8` | `BM-INV-09/10/24` | `KEEP` |
| `CAP-021` | **Attribute attachment levels** — `DATASET` / `DIMENSION_GROUP` / `MEASURE` | `A-CAND2` | an attribute attaches where it actually varies | only three attributes exercised | `BM-EC-005` | `KEEP` |
| `CAP-022` | **Per-combination attribute values** (`group_item_ref` + `unit_ref`) | `A-CAND2` | **closes `CF-015`**, recorded `REQUIRED-BUT-MISSING` by `PHASE-001`: a constant that varies by dimension combination without per-observation repetition. 10 units over 43 indicators in 44 rows | value is `value_text` or a typed ref depending on the attribute — the typed/untyped split is undeclared | `BM-EC-006` | `KEEP` |
| `CAP-023` | Representation: `logical_type`, `codelist_version_ref`, `numeric_precision`, `numeric_scale` | `A-CAND2` | typing and domain declared once, reused by every component | no length, pattern or range | `BM-EC-021`, `BM-INV-09` | `KEEP` |
| `CAP-024` | Observation dimensions **FK-checked against codelist items** | `A-CAND2` | a dimension value cannot be a typo | `TIME_PERIOD` has no codelist and no format check | `BM-EC-007` | `KEEP` |
| `CAP-025` | Versioned semantic references `kind:agency:CODE(version)` | `A-CAND2` | identity resolution is scoped and version-pinned, never "latest" | convention only — no grammar validates the shape | `BM-INV-06` | `KEEP` |
| `CAP-013` | Source **lexical** value retained beside the parsed value | `A-R7`→ | the reason all 880 values round-trip exactly, including 11–16 decimals | stored beside `value_decimal DOUBLE`, which is the wrong parsed type | `BM-INV-09` | `EVOLVE` |
| `CAP-014` | Carrier/cell **positional** parse model | `A-R7`/`A-R8` | records where in a source file a value came from | it is a **raw-plane** grain living in the `__stat_` family; unusable as canonical identity | `BM-INV-24` | `LEGITIMATE-SPECIALIZATION` — raw plane only |
| `CAP-008` | Unit registry: quantity kind, scale factor, base unit | `A-R7`→, extended by `A-CAND2` | units are governed, convertible references | `A-R8` scale factor is `DOUBLE`; `A-CAND2` makes it `NUMERIC(28,16)` and adds `base_unit_ref` | `BM-EC-005` | `EVOLVE` |
| `CAP-009` | Per-carrier semantic binding: unit, aggregation, obs/conf status, quality and confidentiality policy | `A-R8` | one place holding a metric's full semantic envelope | all six values are **constant across all 43 carriers** — the per-carrier shape is unneeded precision, and it duplicates `__stat_metric` | `BM-INV-01` | `MERGE` → attachment levels + rule bindings |
| `CAP-028` | Rule bindings by reference (`QUALITY`, `CONFIDENTIALITY` → a named rule) | `A-CAND2` | policy is referenced, not inlined | the referenced rules exist nowhere in the artifact; nothing validates the reference | `BM-EC-024` | `EVOLVE` |

### 2.3 Classification

| ID | Capability | Source | Problem it solves | Weakness | Benchmark | Disposition |
|---|---|---|---|---|---|---|
| `CAP-006` | Scheme / version / item / alias with **`authority_mode = PROPOSAL`** and `standard_reference` | **`A-R8`** | the artifact declares its classifiers are **proposals, not authority** — the strongest in-artifact answer to `CF-033`/`CF-034` found anywhere | nothing consumes `authority_mode`; the promotion half does not exist (`CF-016`) | `BM-INV-08`, `BM-EC-007` | `KEEP` |
| `CAP-007` | Classifier hierarchy (parent/child edge table) | `A-R7`→ | hierarchical codelists | **0 rows and 0 parents in every artifact** — declared, never exercised, correctness unproven | `BM-EC-007` | `KEEP` (unproven) |

### 2.4 Raw, lineage and provenance

| ID | Capability | Source | Problem it solves | Weakness | Benchmark | Disposition |
|---|---|---|---|---|---|---|
| `CAP-005` | Raw document envelope: checksum, mime, byte size, retention, confidentiality class, `supersedes_source_row_key`, parser/validation status | `A-R8`→ | source fidelity and the immutable-artifact boundary | `payload_reference` and `provenance` are `MEMO` free text | `BM-EC-012` | `KEEP` |
| `CAP-026` | **PROV-O raw chain**: `activity` (kind/agent/software) → `source_record` (`derivation_kind`, `invalidated_at`, `target_dataset_code`) → `fragment` (checksum, parse status) → `document` | `A-CAND2` | lineage, not timestamps; 880 raw records ↔ 880 observations, FK-chained | `agent`/`software` are free text; no digest binds a derivation to a contract revision | `BM-INV-23`, `BM-EC-040/041` | `KEEP` |
| `CAP-010` | Inference provenance: method, **confidence (0.85–0.99)**, rationale | `A-R8`, scaled in `A-CAND2` | where a semantic was inferred, the rationale is the only mitigation available | it normalises inference: `MSA-R2` forbids inferring semantic intent, and 43 units were inferred | `BM-REJ-11` | `KEEP` as a **control**, never as a licence |

### 2.5 Package and interchange

| ID | Capability | Source | Problem it solves | Weakness | Benchmark | Disposition |
|---|---|---|---|---|---|---|
| `CAP-015` | Manifest with per-file `sha256` + bytes + media type | `A-PKG` | integrity checkable without the platform — **451/451 verified by recomputation** | hashes bytes; cannot express semantic correspondence for the `.accdb` | `BM-EC-046/047` | `KEEP` |
| `CAP-016` | Declared row→file edges with `relationCode`, `language`, `ordinal` | `A-PKG` | the row↔file relation is data, not a filename convention — 450 edges, 0 dangling, 225 rows × 2 languages | one relation code only (`PRIMARY_FILE`) | `BM-EC-046` | `KEEP` |
| `CAP-017` | Deterministic assembly from an approved contract descriptor, no hardcoded table/key/path | `A-PKG` | onboarding a package by declaration | assembly evidence lives outside the package | `BM-EC-048` | `KEEP` |
| `CAP-018` | Surgical version stamping (`package_version` only) | `A-R8-BUILD` | a rebuild changes exactly one declared value — verified | byte-level determinism still absent | `BM-QA-04` | `KEEP` |

### 2.6 Presentation, authoring and governance

| ID | Capability | Source | Problem it solves | Weakness | Benchmark | Disposition |
|---|---|---|---|---|---|---|
| `CAP-027` | **`authoring_class`** declared per dataset (`SYSTEM`/`FILL`/`PICK`/`LINEAGE`) and per component (`FILL`/`PICK`/`AUTO`) | `A-CAND2` | the artifact declares *who fills what* — the automation model expressed as metadata | no owner in `CAD-02`; the vocabulary is undeclared and unvalidated | `BM-QA-11`, `STD-AUTO-001` | `KEEP` |
| `CAP-012` | Navigation-pane grouping derived from the semantic plan | `A-R8`→ | an author opens the file and finds the four groups already organised | write-only provider metadata; no consumer, no semantics | `BM-QA-11` | `LEGITIMATE-SPECIALIZATION` — provider presentation |
| `CAP-011` | Approval state asserted **inside the artifact** (`approvalState`, `approvedBy`, `approvalBasis`, `__gs_metadata_schema.approval_state`) | `A-R8`, `A-CAND2` | the responsibility — knowing a projection is approved — is real | a transport artifact cannot own approval; this is `CF-033`/`CF-034` in artifact form, present in **both** | `BM-INV-08`, `BM-EC-039` | `REPLACE` |
| `CAP-030` | Semantic facts carried as **unvalidated metadata assertions** (`__gs_metadata`: 386 rows in 4 namespaces with no schema) | `A-CAND2` (11 rows in `A-R8`) | flexibility for properties nobody modelled | a fact that was FK-checked becomes a text row; the same fact exists twice in one file, once typed and once untyped | `BM-EC-021`, `BM-REJ-06` | `REJECT` for facts the typed model owns |

**Totals — 32 present capabilities:** `KEEP` 22 · `EVOLVE` 5 · `MERGE` 1 · `REPLACE` 1 ·
`REJECT` 1 · `LEGITIMATE-SPECIALIZATION` 2.

---

## 3. `REQUIRED-BUT-MISSING` — no artifact provides these

**The union of the artifacts is not sufficient.** `PHASE-004` must not be limited to
recombining what exists.

| ID | Missing capability | Why required | Evidence that it is absent |
|---|---|---|---|
| `CAP-M01` | **Semantic** artifact↔contract correspondence digest | a byte digest cannot prove meaning: two byte-different files were proven semantically identical | `AUD-COMPARATIVE` §1.1, H-4; `DR-007`, `REQ-028` |
| `CAP-M02` | Generator identity and version inside the artifact | no artifact can answer *"which generator made me"* | `__gs_package` has 8 columns, none of them generator; the manifest names none either |
| `CAP-M03` | Metadata schemas for the `SEMANTIC`, `PROVENANCE`, `GOVERNANCE`, `METHODOLOGY` namespaces | 386 assertions in `A-CAND2` and 11 in `A-R8` are governed by nothing | only `CORE_RESOURCE_V1` and `VISUALIZATION_V1` exist |
| `CAP-M04` | Machine-checkable grain declaration | grain is prose in every artifact; `A-LEGACY`'s `row_key` column list was the last machine-readable form | `BM-INV-24`; §5, §16 |
| `CAP-M05` | Classifier promotion path, and an exercised hierarchy | `authority_mode=PROPOSAL` has no counterpart that promotes; hierarchy has 0 rows everywhere | `CF-016`; §8 |
| `CAP-M06` | Approval state held outside the transport | `CAP-011`'s responsibility needs a home that is not the file being approved | §10 |
| `CAP-M07` | Multi-structure / multi-measure package | every artifact carries exactly one DSD and one measure; the general case is **unproven, not refuted** | `AQ-02`; `SCH-003` |
| `CAP-M08` | Non-cube statistical shapes | one cube is proven; entity/relation families exist but no non-cube *statistical* shape does | `BM-EC-009`; `DR-008` (1 of 6 families proven) |
| `CAP-M09` | Time dimension validation | `TIME_PERIOD` has no codelist, no format constraint, no FK — the only unchecked dimension | §7 |
| `CAP-M10` | `MANY_TO_MANY` as a **declarable** relation with its own semantics | representable by decomposition, never declarable; `__gs_relation` never carries it | §7 |
| `CAP-M11` | Domain / `CHECK` constraints derived from declared field rules | the provider supports validation rules; the generator emits none, so `required`/domain live only in metadata | §13; `BM-INV-05` |

---

## 4. Provider-specific — must stay behind the Access boundary

| Concern | Why it is provider-specific | Rule for `PHASE-004` |
|---|---|---|
| 28-digit precision ceiling | an Access limit, correctly handled as `declaration ≤ provider ≤ canonical` | keep the *pattern*; never let it set canonical storage |
| `MEMO`-JSON columns (`dimension_key_raw`, `mapping_json`, `payload_reference`) | a provider escape hatch where the typed model ran out | every one is a place the canonical model must type instead |
| Navigation-pane grouping, `MSys*` tables, `f_…_Data` | provider presentation and internals | derived output only; never an input to semantics |
| Byte-level non-determinism of `.accdb` | container behaviour, not content | correspondence must be semantic (`CAP-M01`) |
| Carrier/cell positional parse model | an artefact of reading spreadsheets | raw plane only (`CAP-014`) |

---

## 5. Canonical-design input package for `PHASE-004`

### 5.1 Must preserve

Dimension-tuple observation identity with exact decimal (`CAP-020`) · attribute attachment
levels and per-combination values, which close `CF-015` (`CAP-021`, `CAP-022`) · typed
representations bound to an exact codelist version (`CAP-023`, `CAP-024`, `CAP-025`) ·
enforced referential integrity including the contract-metadata plane (`CAP-004`, `CAP-029`) ·
declared cardinality that is actually enforced (`CAP-031`) · the PROV-O raw chain and raw
re-derivability (`CAP-005`, `CAP-026`) · classifier `authority_mode` and standard references
(`CAP-006`) · source lexical fidelity (`CAP-013`) · package manifest integrity and declared
edges (`CAP-015`, `CAP-016`, `CAP-017`) · `authoring_class` (`CAP-027`) · inference rationale
as a control (`CAP-010`).

### 5.2 Must eliminate

Approval state asserted by a transport artifact (`CAP-011`) · semantic facts as unvalidated
metadata assertions where a typed model already owns them (`CAP-030`) · floating-point in the
statistical path (`CAP-013`'s `DOUBLE`) · positional grain as canonical identity (`CAP-014`) ·
site names inside generic families · presentation declared by the transport (`A-LEGACY`'s
`__gs_chart`).

### 5.3 Must reconcile

| Competing mechanisms | Why `PHASE-004` must decide |
|---|---|
| Per-carrier semantic binding (`CAP-009`) **vs** attachment levels + rule bindings (`CAP-021`/`CAP-028`) | both express a metric's semantic envelope; one authority must own it |
| Typed columns **vs** metadata assertions for metric attributes (`CAP-002` vs `CAP-030`) | the same fact exists both ways inside one artifact today |
| `__stat_metric` as an entity **vs** `INDICATOR` as a codelist item | a metric is either a governed vocabulary member or a first-class entity — not both |
| Package-level byte integrity (`CAP-015`) **vs** artifact-level semantic correspondence (`CAP-M01`) | two different mechanisms; only the first exists |
| `data_family` **vs** `authoring_class` | one classifies what data *is*, the other who *fills* it; their overlap is undeclared |

### 5.4 Must add

`CAP-M01` … `CAP-M11` — eleven capabilities no artifact provides.

### 5.5 Open decisions carried into `PHASE-004`

`AQ-01` metric attributes: typed columns or schema-governed metadata · `AQ-02` one DSD per
package or many · `AQ-03` whether the hierarchy model works · `AQ-04` who owns `authoring_class`
· `AQ-05` which `kids-portal-v1-canonical-r8-final.accdb` is authoritative · `BM-Q-01` SDMX
conformance level (the only open `BM-Q` that would change a verdict in this audit) · `BM-Q-02`
Access permanence.

### 5.6 What this package deliberately does not say

No target schema, no table list, no class model, no persistence strategy, no generator design,
no UI. Every row above is a capability, an invariant or a decision to be made — the moment a row
would have to name a table, it stops and becomes a `Must reconcile` or an open decision instead.
That boundary is the difference between `PHASE-003` and `PHASE-004`, and it is deliberate.

---

## 6. Capability envelope — what `PHASE-004` is free to reason about

Added 2026-09-21 by `TASK-005`. `PHASE-003` derived the problem space from four artifacts; an
artifact can only reveal what somebody already built. This section states the envelope
**independently of the artifacts**, so that design is not silently bounded by R8, `candidate_2`,
Access, SDMX or the questions anybody happened to ask.

**The envelope test, and it is a test and not a slogan:**

> ONE COHERENT SEMANTIC AND CONTRACT SYSTEM WITH JUSTIFIED SPECIALIZED REPRESENTATIONS —
> NOT ONE GENERIC PHYSICAL STRUCTURE.

A design satisfies it when a new concern can be expressed by **declaration within the one
semantic system**, and its *physical* form is chosen per family with a stated reason. It fails
it in both directions: a new physical structure per concern (`BM-REJ-03` rigidity), or one
generic structure for all concerns (`BM-REJ-05`/`BM-REJ-07`).

| # | Concern `PHASE-004` may reason about | Already owned by | Envelope status |
|---|---|---|---|
| 1 | Statistical cubes and observation sets | `Q17…Q28`; `CAP-019/020` | in scope, evidenced |
| 2 | **Tidy / long statistical data** | — | **in scope, no owner** — see `DDI-CDI` in `STANDARDS.md` |
| 3 | Ordinary tabular data | `__ent_*` families; `CAP-032` | in scope |
| 4 | Domain / entity / object data | `entity.*`, `CF-039` | in scope, typing is a known defect |
| 5 | **Product-like objects** (variants, options, configurations) | — | **in scope, no owner, never built** |
| 6 | Typed 1:1, 1:N relationships | `CAP-031` | in scope, enforced in the provider |
| 7 | **Typed M:N relationships** | `CAP-M10` | in scope, **declarable nowhere** |
| 8 | Classifiers, codelists, hierarchies | `CAP-006/007`, `Q26` | in scope; hierarchy unexercised |
| 9 | Observation-, dataset-, structure- and concept-level attributes | `CAP-021/022` | in scope, evidenced |
| 10 | Units, status, flags, confidentiality, quality metadata | `Q12`, `Q22`, `Q24`, `CAP-008` | in scope |
| 11 | Raw / source data | `CAP-005/026` | in scope |
| 12 | Canonical / normalized data | `statistics.*`, `entity.*` | in scope |
| 13 | Derived data | `Q25` closed operator set | in scope |
| 14 | Published / served data | publication + serving policy | in scope |
| 15 | Provenance and lineage | `CAP-026`, PROV-O `ADAPT` | in scope |
| 16 | Transformations / mappings | `Q25`, `MSA-021` | in scope |
| 17 | Validation evidence | release gates, `Q47` | in scope |
| 18 | **Logical resources** vs | §10 below | in scope, **no artifact separates them** |
| 19 | **Physical materializations** | §10 below | in scope |
| 20 | Storage locations / providers | `CAD-02` object-storage row, `ADR-016` | in scope |
| 21 | Packages / files / databases / object resources | `CAP-015/016/017` | in scope, strongest measured area |
| 22 | APIs, exports, tables, charts and other projections | `BM-INV-15`, `Q13` | in scope |
| 23 | Versioning / evolution | `Q26`, `Q50`, SemVer | in scope |
| 24 | **Impact / dependency analysis** | — | **in scope, no consumer registry exists** (`D4`) |

**Four concerns are in the envelope with no owner and no artifact**: tidy/long structure
description (2), product-like objects (5), declarable M:N (7), impact analysis (24). They are
not defects of any artifact — nothing ever needed them here. They are exactly what the artifact
survey could not reveal, and they are the reason this section exists.

**No physical representation is prescribed for any row.** A row being "in scope" means design
may reason about it, not that it must be built.

---

## 7. Metadata responsibility model

Eight responsibilities were named for testing; the design space distinguishes them, and three
more are materially distinct and were missing from the list.

| Responsibility | Question it answers | Distinct because | Evidence in artifacts |
|---|---|---|---|
| `SEMANTIC ATTRIBUTE` | what a concept *means* | changes only by contract revision | `__stat_component.concept_ref` |
| `OBSERVATION ATTRIBUTE` | what is true of **one fact** | varies per row | `OBS_STATUS` attached at `MEASURE` |
| `DATASET ATTRIBUTE` | what is true of **the whole set** | one value, whole-set scope | `CONF_STATUS` attached at `DATASET` |
| `STRUCTURE ATTRIBUTE` | what is true of **the structure** | survives every data refresh | `__stat_attribute_attachment` |
| `PROVENANCE` | where it came from, by what activity | append-only, never corrected in place | `__raw_activity`, `__raw_source_record` |
| `QUALITY METADATA` | how good it is, against which rule | an outcome of evaluation, not a declaration | `__stat_rule_binding` (`QUALITY`) |
| `OPERATIONAL METADATA` | what happened at runtime | per run, not per meaning | `parse_status`, `operation`, batch ids |
| `PRESENTATION METADATA` | how it should be shown | may change with no semantic change | `__gs_page`, `__gs_projection` |
| **`GOVERNANCE METADATA`** *(added)* | who approved what, when, on what basis | it is an **act**, not a description; `CAP-011` is the live violation of treating it as description | `approvalState`, `approvedBy`, `approvalBasis` — in the transport, where it must not be |
| **`INFERENCE METADATA`** *(added)* | what the machine guessed, how confidently, by which method | it is an assertion **about** a semantic, not the semantic; `MSA-R2` forbids the act, so recording it is the only control | `inferenceMethod/Confidence/Rationale`, 43 rows |
| **`ACCESS/CONFIDENTIALITY POLICY METADATA`** *(added)* | who may see it, from when | it is enforced rather than displayed, and it is **temporal** (`Q12` `publish_not_before`) | `conf_status`, `confidentiality_policy_code` |

**The one rule this model must carry into design:** these eleven differ in *lifecycle*, not in
shape. A generic metadata bag can hold all of them and can express none of their rules — which
is why `CAP-030` is `REJECT` and `CAP-M03` is `REQUIRED-BUT-MISSING`. **Do not design their
tables here; do not collapse them anywhere.**

---

## 8. Raw → canonical → derived → published chain

| Stage transition | Requirement that exists | Owner | Sufficient for design entry? |
|---|---|---|---|
| `EXTERNAL SOURCE → INGESTED RAW` | receipt, fingerprint, checksum, quarantine; immutable envelope | `ING-001`, `CAP-005` | yes |
| source fidelity | lexical original retained; 880/880 exact | `CAP-013`, `Q22` | yes |
| `RAW → NORMALIZED/CANONICAL` | declared mapping, never inferred | `MSA-021`, `Q25` | yes |
| transformation identity | derivation rule is versioned, closed-operator, acyclic, inputs declared | **`Q25`** | yes |
| validation | grammar, semantic, quality and confidentiality gates before publication | `Q47`, release gates | yes |
| provenance | activity → source record → fragment → document, FK-chained | `CAP-026` | yes |
| reproducibility | canonical digest over canonical form; two digests separating semantics from presentation | **`Q39`** | yes |
| `CANONICAL → DERIVED` | derived observation records the rule revision in lineage | `Q25` | yes |
| `DERIVED → PUBLISHED/SERVED` | publication membership is the release boundary | `RC-001`, `BM-INV-12` | yes — as a *requirement*; the mechanism is defective today |
| version relationships across the chain | codelist version pinned in DSD; new version ⇒ new DSD revision; typed crosswalk | **`Q26`** | yes |
| **as-of / valid-time / late-arriving / tombstone** | named as required | `OBL-WORKSTREAMS` (SUPPORTING) | **no — known but unrouted; `D2`** |

**Raw must not become semantic authority**, and the artifacts show both the correct and the
incorrect shape: `__raw_source_record` derives *into* canonical (correct), while
`__stat_kids_statistical_input` carries a raw-plane grain inside the `__stat_` family
(`CAP-014`, `LEGITIMATE-SPECIALIZATION` on the raw plane only).

---

## 9. Relationship space

### 9.1 Statistical ↔ non-statistical relationship classes

| Class | Exists? | Evidence |
|---|---|---|
| observation ↔ domain entity | **yes** | `__rel_kids_resource_indicator` — 43 rows, resource ↔ indicator |
| observation ↔ raw/source record | **yes** | 880 ↔ 880, FK-chained |
| canonical record ↔ raw record | **yes** | `__raw_document → __ent_*` on every entity family |
| derived value ↔ source values | declared rule only | `Q25`; no per-value edge in any artifact |
| dataset/data product ↔ source dataset | **no** | nothing declares dataset-level derivation |
| logical resource ↔ physical materialization | **no** | §10 |
| version ↔ derivation/publication | partial | snapshot membership; no contract-revision ↔ published-output edge |
| **entity ↔ entity across families** *(added)* | partial | `entity_link` exists; cardinality unenforced (`CF-040`) |
| **classifier item ↔ classifier item across schemes** *(added)* | **no** | `Q26` defines a typed crosswalk between *versions*; nothing relates items across *schemes* |
| **resource ↔ resource containment** *(added)* | convention | package contains files; expressed in a manifest, not in the model |

### 9.2 Relationship semantics a design may need

Tested against `__gs_relation` (`relationship_code`, from/to, `cardinality`, `required`) and
the manifest's `edges[]` (`rowKey`, `relationCode`, `language`, `ordinal`):

| Dimension | Expressible today | Where |
|---|---|---|
| role | partial | `relationCode` in the manifest; absent in `__gs_relation` |
| direction | yes | from/to |
| cardinality | declared; 1:1 and N:1 enforced; **M:N not declarable** | `CAP-031`, `CAP-M10` |
| ownership | **no** | — |
| containment / composition | **no** | — |
| dependency | **no** | — |
| referential integrity | yes | provider FKs |
| temporal validity | **no** | `D2` |
| version scope | partial | codelist version pinned; relations are not version-scoped |
| classification semantics | yes | classifier FKs |
| derivation semantics | rule-level only | `Q25` |
| provenance semantics | yes | `CAP-026` |
| materialization semantics | **no** | §10 |

**Seven of thirteen are not expressible.** A universal `source_id / target_id /
relationship_type` triple would make all thirteen equally inexpressible — it can *record* a
relationship and cannot *constrain* one. The requirement carried into design is unchanged and is
now evidenced: **dynamic relationships must stay typed, constrained, inspectable, enforceable
and evolvable.**

---

## 10. Logical identity vs physical materialization

| | Logical | Physical |
|---|---|---|
| Concepts | data product, dataset, resource, observation set, classifier scheme | table, file, object key, Access database, package, storage location |
| Identity today | `dataset_code`, `contract_code` + revision, `structure_ref` | `access_table_name`, `bucket`+`object_key`, ZIP path, `artifact_object_id` |
| Binding | `__gs_dataset.access_table_name` — **a logical code pointing directly at a physical name** | |
| Survives relocation? | **no** — a table rename changes the contract row | content identity survives: `ingest.artifact_object` keys by `sha256` (`CAD-02` §2.3) |
| Survives provider addition? | untested — one provider exists (`ADR-016` rule 6) | |

**The repository already contains both the failure and the fix.** `AQ-05` is the failure at
artifact scale: a *path* stood in for an identity, and two days later the path was wrong while
the digest was still right (`AUD-COMPARATIVE` §23). `ingest.artifact_object` is the fix at byte
scale: *"content identity is the checksum; a second location for the same bytes is not a new
object."* **`Q39` states it a third time, independently:** the ACCDB binary checksum is
*"build-specific, recorded, not compared"*.

Design entry therefore carries a settled requirement and an unsettled mechanism: canonical
meaning must survive relocation and provider change, **and** every materialization must stay
traceable where reproducibility, lineage or operations need it. No artifact separates the two.

---

## 11. The two hypotheses

### 11.1 End-to-end governed chain — **MODIFY**

*Hypothesis:* a governed data resource should be traceable and traversable through its complete
semantic, contractual, relational, provenance, transformation, materialization and delivery
chain.

**Falsification attempts and what survived:**

| Attempt | Result |
|---|---|
| *Is it a chain?* | **No.** Measured fan-out (225 rows → 450 files) and fan-in (a derived statistic from several upstream resources). It is a **DAG**; the linear illustration is one path through it |
| *Is it one graph?* | **No — and this is the material correction.** At least **three** distinct graphs share the same nodes: **derivation/provenance** (acyclic, historical, append-only), **dependency/impact** (contract → consumers, forward-looking, changes as consumers change), and **composition/containment** (dataset contains observations, package contains files). They differ in direction, mutability and lifetime. One universal edge type collapses all three and can constrain none |
| *Does traversability require graph storage?* | **No.** `candidate_2` delivers most traversals with typed FKs, and the manifest delivers row→file with a typed edge list. Graph-**like** traversability is achieved, at this scale, by typed relations |
| *Where does traversal actually break?* | At **cross-boundary hops**: file → row, object → registry row, published output → contract revision. Each exists today as a convention or a digest, not as a traversable typed relation |
| *Is there a stronger established model?* | Partly. The hypothesis is the union of three mature models, each owning one graph: **PROV-O** (derivation), **OpenLineage** (runtime dataset/job/run events), **DDI-CDI** (a datum's roles across wide/long/dimensional/key-value structures). None of the three covers all three graphs |

**Verdict: MODIFY, do not canonize.** Retain traversability as a requirement. Reject *one chain*
and *one edge type*. Carry into `PHASE-004`: **three separately typed relationship families**, and
**evidence-bound traversal** — every hop must state how it is known (foreign key, digest,
declaration, or recorded event), because a traversal that cannot cite its hops returns plausible
paths rather than true ones. Universal nodes, universal edges, EAV, arbitrary JSON and
unconstrained graph modelling remain rejected (`BM-REJ-05/06/07`).

### 11.2 Contract-governed lifecycle — **MODIFY**

*Can contracts govern the chain without becoming a second semantic authority?* **Yes, under one
rule the evidence supports:**

> A contract is authority for **declaration**. It is never authority for **observation** (what
> is), for **acts** (what was approved), or for **outcomes** (what was measured).
> **Test: does any contract field change without a new revision? If yes, it is state
> masquerading as declaration.**

Evidence both ways: derived plans are digested and never persisted as authority (`INV-013`,
honoured); and `CAP-011` — `approvalState`, `approvedBy`, `approvalBasis` inside the transport —
is precisely a contract field carrying an act, which is how `CF-033`/`CF-034` happened.

**Three stages are missing from the proposed chain**, and each is where a real failure already
occurred or is recorded as missing:

1. **APPROVAL / AUTHORIZATION** — between *validation* and *canonical data*. It is what makes a
   contract binding, it is an act rather than a declaration, and it is the stage `CAP-011`
   violates.
2. **CORRECTION / REPROCESSING** — a governed loop from *published* back to *canonical* under a
   corrected revision. `REC-PASS3` §10 records it as `REQUIRED-BUT-MISSING`; a forward-only
   chain cannot express it.
3. **RETIREMENT / DEPRECATION** — distinct from *evolution*: a contract that must stop being
   used is not a contract that changed.

**Verdict: MODIFY and retain.** The twelve stages plus these three, governed by the
declaration-vs-state test.

---

## 12. Blind-spot findings

Independently searched, then tested against the repository rather than assumed. **Four
candidates were falsified** — the statistical register already owns measure additivity and
`SUM`-blocking (`Q24`), missing-value semantics including *not applicable* and *suppressed*
(`Q22`), embargo via `publish_not_before` (`Q12`), and concurrent authoring via strong `ETag` /
`If-Match` (`Q40`). Seven survived.

| ID | Dimension | Why it is material | Status in the repository | Routed to |
|---|---|---|---|---|
| `D1` | **Secondary / complementary disclosure control** | suppressing one cell is insufficient: the value is recoverable by subtraction from published totals. Requires knowing aggregation relationships *across* published outputs | **zero occurrences repository-wide**; `Q12`/`Q22` cover declaring and flagging suppression, never protecting it | `AUD-CAPABILITY` `CAP-M12`; `BM-EC-087` |
| `D2` | **Bitemporality: valid-time vs transaction-time, as-of queries, late-arriving data, tombstones, classifier effective periods** | without it, a restatement is indistinguishable from a correction, and a historical figure cannot be reproduced | **already identified** in `OBL-WORKSTREAMS` (SUPPORTING) — and **not routed** into design entry | `MANIFEST` `PHASE-004` inputs; `CAP-M13` |
| `D3` | **Legal erasure against immutability** | an erasure obligation collides with immutable snapshots, retained raw and append-only lineage. It is a legal/product decision with an architectural consequence | only "semantic erasure" appears, a different meaning | owner question, `AUD-CAPABILITY` §13 |
| `D4` | **Consumer registration** | impact traversal needs to know *who consumes a dataset*. Nothing registers consumers, so forward impact analysis is not derivable at all | `CF-024b` names consumers in prose; no mechanism | `CAP-M14` |
| `D5` | **Cross-dataset consistency constraints** | `Q27` constrains combinations *within* a cube; nothing expresses "the total in A must equal the sum in B" | absent | `CAP-M15` |
| `D6` | **Reference-data ownership across sites/tenants** | when two sites share a codelist and one extends it, ownership is undefined; `QF-003` shows a single-primary-site assumption | absent | `BM-Q-04` (already open) |
| `D7` | **Three distinct relationship families** | §11.1 — derivation, dependency, composition differ in direction, mutability and lifetime | absent as a concept | `CAP-M16` |

### 12.1 The finding that mattered most, and it is a routing defect

**`PHASE-004`'s input list did not include the statistical domain register (`Q01…Q50`).**

`ADR-014` rule 1 establishes that its `DECIDED` entries bind M1 semantics. `TASK-005`'s own
blind-spot pass then falsified four of its candidate discoveries against that register, and
found that `Q25`, `Q26`, `Q39`, `Q47` and `Q48` already specify derivation rules, codelist
version crosswalks, canonical digests, the conformance claim and a build manifest carrying
**generator identity and semantic correspondence** — which are `CAP-M01` and `CAP-M02`,
"missing from every artifact" and *specified in the register nobody was going to read*.

Design entering without it would have re-decided fifty questions that are already decided and
evidenced: duplicate authority at the largest scale this programme has produced. **Fixed by
adding `DOM-STATISTICAL` to `MANIFEST`'s `PHASE-004` input table and to `CTRL-CURRENT`'s read
set.** It was not a design defect and not an artifact defect — it was a consequence of deriving
the problem space from artifacts, which is what this section exists to correct.

---

## 13. Falsification scenarios

Twelve scenarios, simple and complex. Three expose a responsibility that had no home.

| # | Scenario | Result |
|---|---|---|
| 1 | A very simple statistical dataset — one measure, two dimensions, no attributes | **holds.** `Q17…Q28` + `CAP-019/020`. Simplicity is expressible without ceremony |
| 2 | Rich observation metadata — status, confidentiality, quality and unit varying per observation | **holds.** Attachment levels (`CAP-021/022`) express all four at their correct scope |
| 3 | A domain/product-like object model with variants and options | **partial.** The entity family exists and is untyped (`CF-039`); variant/option structure has never been built. Envelope row 5 |
| 4 | A many-to-many domain relationship carrying its own attributes | **fails as declaration.** Representable by decomposition, declarable nowhere — `CAP-M10`, already preserved |
| 5 | Statistical observations linked to raw/source records | **holds, measured.** 880 ↔ 880, FK-chained |
| 6 | A derived statistic computed from several upstream resources | **partial.** `Q25` governs the rule; the *per-value* fan-in edge has no home — `D7`/`CAP-M16` |
| 7 | One logical resource materialized in two providers at once | **fails.** §10 — no artifact separates logical identity from physical location |
| 8 | Adding a second provider without changing canonical semantics | **untested.** `ADR-016` rule 6 is an obligation with no evidence; one provider exists |
| 9 | A versioned contract/codelist evolution with a 1:n crosswalk | **holds.** `Q26` types the crosswalk and blocks automatic migration for `1:n` |
| 10 | Lineage traversal from a published output back to source | **partial.** Complete inside `candidate_2`; breaks at the published→contract-revision hop — `CAP-M01` |
| 11 | Impact traversal from a changed contract forward to consumers | **fails.** No consumer registry — `D4`/`CAP-M14` |
| 12 | A suppressed cell that is recoverable by subtraction from a published total | **fails.** `D1`/`CAP-M12` — nothing protects a suppression |

**Three failures requiring a new requirement: 4 → already preserved (`CAP-M10`); 11 → new
(`CAP-M14`); 12 → new (`CAP-M12`).** Scenarios 6, 7 and 10 fail against capabilities now added
or already recorded. **No scenario was resolved by weakening a criterion.**

---

## 14. `REQUIRED-BUT-MISSING` — 11/11 preserved, and five more

Every capability `PHASE-003` reported is traced to an owner that `PHASE-004` reads. **None
disappeared for want of an implementation.**

| ID | Capability | Preserved in | Reinforced by `TASK-005` |
|---|---|---|---|
| `CAP-M01` | semantic artifact↔contract correspondence digest | `AUD-CAPABILITY` §3, §5.4 | **`Q48` already specifies it** in the build manifest |
| `CAP-M02` | generator identity/version in the artifact | §3, §5.4 | **`Q48`** specifies generator version + config + template digest |
| `CAP-M03` | metadata schemas for the unschema'd namespaces | §3, §5.4 | §7 — eleven responsibilities, one bag, no rules |
| `CAP-M04` | machine-checkable grain declaration | §3, §5.4 | `BM-INV-24`; `Q18` same-grain test |
| `CAP-M05` | classifier promotion path and an exercised hierarchy | §3, §5.4 | `CF-016` |
| `CAP-M06` | approval state outside the transport | §3, §5.4 | §11.2 — the missing `APPROVAL` stage |
| `CAP-M07` | multi-structure / multi-measure package | §3, §5.4 | `ADR-015` — inside the SDMX boundary and unproven |
| `CAP-M08` | non-cube statistical shapes | §3, §5.4 | envelope row 2 — `DDI-CDI` names the model |
| `CAP-M09` | time dimension validation | §3, §5.4 | `Q19` fixes the formats; nothing validates them in-artifact |
| `CAP-M10` | `MANY_TO_MANY` as a declarable relation | §3, §5.4 | scenario 4 |
| `CAP-M11` | domain/`CHECK` constraints from declared field rules | §3, §5.4 | §7 — a bag cannot carry a constraint |

**`REQUIRED_BUT_MISSING_PRESERVED: 11/11`.**

**Five added by this pass** — same status, same obligation:

| ID | Capability | Source |
|---|---|---|
| `CAP-M12` | **Complementary (secondary) disclosure control** — protecting a suppression, not merely declaring it | `D1`, scenario 12 |
| `CAP-M13` | **Bitemporal semantics** — valid-time vs transaction-time, as-of query, late-arriving data, tombstone, classifier effective period | `D2`; known in `OBL-WORKSTREAMS`, unrouted |
| `CAP-M14` | **Consumer registration** — the forward half of dependency/impact traversal | `D4`, scenario 11 |
| `CAP-M15` | **Cross-dataset consistency constraints** | `D5` |
| `CAP-M16` | **Three separately typed relationship families** — derivation, dependency, composition | `D7`, §11.1 |

**`AUD-CAPABILITY` now carries 32 present + 16 required-but-missing = 48 capabilities.**

---

## 15. Maximum Safe Automation applied to the envelope

For every responsibility above: *if the system already knows this from semantic authority,
contract, relationship, lineage or provider capability, why would a human enter it again?*

| Derivable, therefore a human must not retype it | Known from |
|---|---|
| physical table and column names, keys, indexes, relationships in a provider artifact | contract + physical pattern + provider capability (`MSA-010/012`) |
| codelist items, aliases and their pinned versions inside an authoring file | the classifier registry (`MSA-014`) |
| unit, obs-status and conf-status defaults per indicator | attachment-level declarations (`CAP-021/022`) |
| lineage rows for every canonical record | the load itself (`MSA-037`) |
| package manifest entries, digests and row→file edges | the assembly (`CAP-015/017`) |
| impact sets, once consumers are registered | `CAP-M14` + the dependency graph |
| conformance matrix rows | the DSD plus the declared subset (`Q47`) |

| Must stay human, and automating it would fabricate business semantics |
|---|
| what a dataset **means**, its grain and its population scope (`MSA-001`, `Q18`) |
| promotion of a proposed classifier to authority (`MSA-025`) |
| approval, four-eyes, and publication (`MSA-028`, `Q06`) |
| **whether a suppression is sufficient** (`CAP-M12`) — a disclosure judgement, not a computation, even though its *complement set* is computable |
| the values a domain author types (`MSA-015`) |

**`AUTHOR GENUINE MEANING ONCE → REUSE → DERIVE → GENERATE → MATERIALIZE → VALIDATE → TRACE`
holds across the envelope**, with one addition this pass makes explicit: *derive* must produce
an **inspectable artifact**, never a runtime-only interpretation (`MSA-R8`).

---

## 16. What this section does not do

It names no table, no class, no storage engine and no graph implementation. Every row above is a
concern, a requirement or a question. The moment a row would have to choose a representation, it
stops — that boundary is what separates design entry from `PHASE-004`.
