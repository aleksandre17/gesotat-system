# Access package — intrinsic assessment and canonical standard

Date: 2026-09-20
Status: **PROPOSED.** Part A is measured fact about one artefact. Part B is a standard offered for approval; nothing in
it is in force until the owner approves it and a contract revision carries it.
Artefact assessed: `C:\Users\Test-User\Desktop\KIDS_PACKAGE_candidate_3.accdb`, sha256 `7F0515B5…E297`, 4 915 200 bytes.
Related: [composer design](ACCESS-PACKAGE-COMPOSER-DESIGN.md) · [independent audit](evidence/kids-package-full-audit-2026-09-20/AUDIT-CONCLUSION.md)
· [response to it](evidence/kids-package-full-audit-2026-09-20/AUDIT-RESPONSE.md)
Language note: English on purpose, so the text is exact.


old upload zip file : C:\Users\Test-User\IdeaProjects\gesotat-system\samples\kids-r8-resource-package.zip. საბოლოოდ ასეთი უნდა იყოს ასატვირთი ზიპ ფაილი, ოღნდ ახალი სტრუქტურით, ის რაც დამტკიცდება აქსესში, და ავა უმაღ₾ს ლერებზე, კონტრაქტში
---

*** პირველი ეს უნდა გაეიკვეს ***

სრული დავალება და evidence-bound კონტექსტი. არ შემოიფარგლოს მხოლოდ mismatch-ების ჩამოთვლით: შეაფასოს თვითონ ფაილის შინაგანი ხარისხი, multi-dimension/multi-measure შესაძლებლობები,
ურთიერთობები, chart/serving readiness, Access usability, migration/rollback და შექმნას მომავალი site/provider/data family-ებისთვის განმეორებადი canonical standard. პარალელურად audit evidence-
სა და governance gate-ს ვინარჩუნებ.
-----


## Part A — what this file is, measured

Every number below was read from the artefact by `Facts2.java`, `VerifyPackage.java`, `Audit.java` or by Microsoft
Access 16.0.4229 itself (`provenance_test.ps1`, `see-access.ps1`). Tools: `ops/scripts/java/prototype/`.

### A1. Verdict per dimension

| Dimension | Verdict | Measured basis |
|---|---|---|
| Data and lineage | **Strong** | 880 observations, 880 exact against R8's *lexical* source (not against rounded values); 0 empty values; 880 provenance records, 43 raw fragments, 456 raw documents; 24 R8 tables / 184 R8 columns accounted for, 0 lost |
| Relational structure | **Good, two gaps** | 32 of 32 tables have a primary key; 36 enforced relationships (R8: 21); duplicates refused in all four authority tables (negative tests). Gaps: A3-1, A3-2 |
| Contract identity discipline | **Weak by nature of the artefact** | It is a candidate built without a contract, at the owner's request. It now says so itself (`8+CANDIDATE`, `NOT_APPROVED`, `derivedFrom`). It cannot be more than that until a contract revision exists |
| Statistical semantic model | **Right shape, demonstrated once** | 1 structure; 3 dimensions, 1 measure, 3 attributes; 10 units bound per indicator; aggregation `NONE`. Multi-structure and multi-measure are *modelled* and **not demonstrated** — see A2 |
| Chart / serving readiness | **Not ready** | see A4 |
| Usability in Access | **Workable for a trained person, not for a casual one** | see A5 |
| Migration and rollback | **Safe** | R8 file, R8 contract and published snapshot 36 are untouched; the candidate is a separate file with its own identity; the way back is to do nothing |
| Production / canonical readiness | **Not ready** | agrees with the independent audit |

### A2. Multi-dimension and multi-measure — what is proven and what is not

Proven in this file: three dimensions, one of them time, two of them coded against pinned codelist versions; one exact
measure (NUMERIC 28,16); a measure-attached attribute (`OBS_STATUS`), a dimension-group attribute whose value varies by
code (`UNIT_MEASURE` per `INDICATOR`, 43 values over 10 units) and a dataset attribute (`CONF_STATUS`).

Modelled but **not** proven here: a second structure in the same package (the catalog keys everything by
`structure_ref`, and `__stat_structure.observation_table` names the table, but the builder writes exactly one);
several measures on one grain (the component table admits any number of `MEASURE` rows; no table with two was
generated); a non-time structure; mixed grains correctly refused. The server-side compiler proves these
(`StatisticalContractCompilerTest`, `StatisticalIngestionRoundTripTest`); **the package layer does not yet.** Until a
second, differently shaped site goes through the same generator with zero code change (composer acceptance test C),
"repeatable" is a design claim, not a result.

### A3. Relationships — the two gaps

1. `__gs_relation.from_field` / `to_field` are not enforced against `__gs_field`, because the nine `__gs_*` tables
   have no field declarations of their own (measured: 9 datasets without field rows). The package does not fully
   describe itself.
2. `__gs_page.dataset_code` has no relationship: R8 stores an empty string, not NULL, on the root page, and Access
   refuses a foreign key to an empty string. An R8 data defect, carried.

By design, not a gap: `__raw_source_record.target_key` is not a foreign key. It addresses a row of *any* dataset, so
it cannot be one; Access data macros keep it whole instead (live-tested: insert, edit, re-key, delete).

### A4. Chart and serving readiness — why "not ready"

What a chart needs is present: time axis 2016–2025, a series dimension, exact values, a unit per indicator, and the
rule that nothing may be summed (so a share-of-total chart is refused, a line per indicator is allowed; six indicators
are percentages and three are rates). What blocks serving: the statistics page and its projection are
`REVIEW_REQUIRED` (their mapping was approved against the retired shape); three more projections are `DRAFT` in R8
itself; `AGE_GROUP` has 0 of 12 labels and `KIDS_RESOURCE_SUBCATEGORY` 0 of 4, so a legend would show raw codes;
indicators have between 6 and 32 observations each, so any chart must tolerate gaps in time.

### A5. Usability — measured, without flattery

In favour: the file opens on the custom category with four groups; of 159 declared fields a person fills 30 (20 typed,
10 picked) and the system fills 129; 10 columns are pick-lists generated from the relationships, and show labels where
labels exist; the period accepts four digits only; Access itself refuses a duplicate observation; a typed row gets its
provenance at once. Against: there are **no forms**; headers are standard component codes with no captions (an explicit
owner decision — a casual author will not know what `OBS_VALUE` means); table names are technical; nothing in Access
stops a person from editing a `SYSTEM` or `LINEAGE` table; this Access installation reports "Product Activation
Failed", so editing may be restricted on this machine regardless of the file.

---

## Part B — the canonical standard (proposed)

Normative words: **MUST**, **MUST NOT**, **SHOULD**. Every rule names the check that proves it; a rule without a
machine check is not a rule. Basis: SDMX 3.1 information model; W3C PROV-O (entity, activity, derivation,
invalidation); ISO/IEC 11179 (value domains); RFC 8785 + SHA-256 (digests); the platform's completion plan §1
(a new site is a contract, never a code change).

### B1. Identity and authority

| Rule | Statement | Check |
|---|---|---|
| I-1 | A package MUST state the contract code, revision and digest it was generated from, and MUST NOT carry the identity of any revision it merely derives from | `__gs_package` + digest compare; candidate marker |
| I-2 | The package is a **projection** of one approved contract revision; nothing edited inside it becomes authority | server re-derives on load; stale or edited metadata -> refusal |
| I-3 | Every table MUST have a primary key; every authority table MUST refuse a duplicate | structural audit + negative insert |
| I-4 | Every reference between package tables MUST be an enforced relationship, unless it addresses rows of *any* dataset, in which case a tested mechanism keeps it whole | relationship inventory; live re-key / delete test |
| I-5 | The package MUST describe itself completely: every table is a dataset, every column a field, every key and relation declared — including the description tables themselves | "table without description" = 0; "dataset without fields" = 0 |

### B2. Names and shape — what is the same everywhere and what is generated

| Rule | Statement |
|---|---|
| N-1 | **Identical on every site** (tables and columns; only rows differ): `__gs_*`, `__cl_*`, the `__stat_*` catalog, `__raw_document`, `__raw_activity`, `__raw_fragment`, `__raw_source_record` |
| N-2 | **Generated from the site's contract**: `__ent_*`, `__rel_*`, and the dimension columns of each observation table. No list of site table names exists in code |
| N-3 | A table's prefix states its family: `__ent_` entity, `__rel_` relation, `__stat_` statistical, `__cl_` classifier, `__raw_` raw and provenance, `__gs_` contract copy. No other prefix |
| N-4 | Statistical component names come from a shared concept registry (SDMX cross-domain concepts): `TIME_PERIOD`, `REF_AREA`, `SEX`, `AGE`, `INDICATOR`, `OBS_VALUE`, `OBS_STATUS`, `UNIT_MEASURE`, `CONF_STATUS`. A site never invents a synonym |
| N-5 | One observation table per structure, named in `__stat_structure.observation_table`. Measures of one grain share a table; measures of different grains MUST NOT |
| N-6 | A quantity that differs per code is a **dimension** (`INDICATOR`), never one column per quantity. Measured cost of getting this wrong: 88 columns and 3 291 empty cells instead of 5 columns and none |

### B3. Statistical semantics

| Rule | Statement |
|---|---|
| S-1 | Values are exact decimals. The scale MUST hold the source without rounding (measured here: 221 values need 11–16 decimals; an envelope of 10 would have altered them) |
| S-2 | Empty, zero, missing and suppressed are different facts; an empty measure cell MUST carry a status |
| S-3 | Aggregation is declared, defaults to `NONE`, and percentages, rates and indices are never summed |
| S-4 | A unit that varies by code is an attribute attached to that dimension, stored once per code — never typed per row |
| S-5 | Every coded value pins a codelist **version** |

### B4. Authoring surface

| Rule | Statement |
|---|---|
| A-1 | Every table and every field carries one authoring class: `FILL`, `PICK`, `AUTO`, `SYSTEM`, `LINEAGE`. Grouping, pick-lists and protections are derived from it |
| A-2 | Every `PICK` field MUST be a pick-list generated from its relationship, showing a label and storing the stable reference |
| A-3 | Every codelist MUST be visible as an object of its own (`CL_<SCHEME>`), generated from `__cl_scheme` |
| A-4 | Whether headers show standard codes or localized captions is a per-deployment **presentation** choice recorded in the contract; it never changes a column name |
| A-5 | Every generated file MUST be opened in the real provider and captured before it is handed over. A structural read is not acceptance |

### B5. Provenance (W3C PROV)

| Rule | Statement |
|---|---|
| P-1 | Every row of every fill dataset MUST have at least one provenance record at all times |
| P-2 | The system writes provenance; a person never does. A typed row is recorded by the provider itself the moment it is saved |
| P-3 | Provenance is never deleted: a removed row leaves its records marked invalidated |
| P-4 | A re-keyed row keeps its provenance |
| P-5 | Provenance tables name no site, no carrier and no source format; those are values in rows |
| P-6 | The server rebuilds provenance from the bytes it received and never trusts what a file says about itself |

### B6. Provider facts every generator must respect (found the hard way, 2026-09-20)

| # | Fact | Consequence |
|---|---|---|
| F-1 | A custom Navigation Pane group needs `Object Type Group = -1`, and its category an "Unassigned Objects" group with `Flags = 4` | otherwise the pane opens empty |
| F-2 | A Java source launched as `java File.java` on Windows is decoded as cp1252 | non-ASCII literals MUST be `\uXXXX` escapes |
| F-3 | `Like "####"` matches digits only in ANSI-89 mode | use `[0-9]` classes in validation rules |
| F-4 | Access evaluates text between two `|` inside an expression | a value holding pipes may be assigned whole, never concatenated; `Replace()` on it crashes the engine |
| F-5 | A data macro field named `[table].[field]` is read as `[database].[table]` | field names stay unqualified |
| F-6 | A file with no `USysApplicationLog` fails its first data-macro `CreateRecord` | the generator spends that first run itself and removes every trace |
| F-7 | DAO SQL literals containing pipes fail | write through recordsets |

### B7. Acceptance — a package is accepted only when all of these pass

1. Structural audit: 0 findings (primary keys, indexes, required tables of the *approved* revision, numeric key order).
2. Self-description complete (I-5).
3. Parity or mapping: either table-by-table parity with the previous approved package, or a column-level mapping with 0 unaccounted columns, carried by a contract revision.
4. Value proof against the **lexical** source, not against stored numbers.
5. Live provider test: open, initial view, pick-lists execute, insert / edit / re-key / delete keep provenance whole, provider error log empty — on fresh copies, repeatedly.
6. Server round-trip: validate -> load -> reconcile -> release gates, plus the negatives (stale contract, other tenant, replay, rollback).
7. Second site: a differently shaped contract through the same generator with **zero code change**.
8. Repeat generation: the same revision twice gives the same content digest.

Today this candidate passes 4 and 5, passes 3 as a mapping without its contract revision, and fails or has not run
1 (required-table names await the revision), 2 (nine datasets undescribed), 6, 7 and 8.
