# Response to the independent audit of `KIDS_PACKAGE_full.accdb`

Date: 2026-09-20
Audit answered: [`AUDIT-CONCLUSION.md`](AUDIT-CONCLUSION.md) (audited bytes: sha256 `5AD1C5C6…0EBB`, 4 395 008 bytes)
New artefact: `C:\Users\Test-User\Desktop\KIDS_PACKAGE_candidate_3.accdb`
sha256 `7F0515B52FBA4BD40976CDB666282D2F59191C5A5B523A1826290A074AE9E297`, 4 915 200 bytes
Language note: English on purpose, so the text is exact.

## Position

The audit is correct, and its verdict stands: **NOT READY for canonical / production acceptance.** The file was built
at the owner's request as a contract-less prototype of the target package ("just the Access file first, without
contracts"); it was never an approved revision and must not be treated as one. Two findings were regressions I
introduced; they are fixed. The rest are either decisions that belong to a contract revision or states of R8's own
content, and are answered below without pretending otherwise.

Note on scope: the audited bytes predate the provenance rebuild. `__raw_stat_source`, `__raw_stat_crosswalk` and
`__raw_kids_statistical_carrier`, which the audit names, no longer exist in the new artefact (see F-P1a).

## Finding by finding

| # | Audit finding | Verdict | Status in `candidate_2` | Evidence |
|---|---|---|---|---|
| P0 | `__gs_key` has no index / primary key | **Correct — my regression.** R8 had `PK(dataset_code, key_role, key_order)`; it was lost when I rebuilt the table by hand instead of copying it. | **Fixed.** PK restored, plus `UNIQUE(dataset_code, key_role, field_name)` and an enforced FK `(dataset_code, field_name) -> __gs_field`. | `VerifyPackage`: "refused a duplicate key part in __gs_key" |
| P0 | `__gs_relation` has no index / primary key | **Correct — my regression.** R8 had `PK(relationship_code)`. | **Fixed.** PK restored; enforced FKs `from_dataset_code` and `to_dataset_code -> __gs_dataset`. | `VerifyPackage`: "refused a duplicate relation in __gs_relation" |
| P1 | `__gs_key.key_order` is TEXT | **Correct.** Inherited from R8, where it is TEXT too — a defect of R8 that I carried instead of fixing. | **Fixed.** `LONG`, parsed and required positive at build time. | `VerifyPackage`: "key order is numeric: true" |
| P0 | Statistical table names differ from the plan (`__stat_metric`, `__stat_kids_statistical_input`, `__rel_kids_statistical_semantic_binding` absent) | **Correct, and the audit's option 2 is the right one.** A rename or replacement is legitimate only as a versioned contract revision with mapping, lossless lineage, consumer compatibility and a deprecation rule. | **Open — by nature.** Not fixable inside a file. What exists as input to that revision: a column-level mapping of all 24 R8 tables / 184 R8 columns with 0 unaccounted (`KIDS_PACKAGE_full_audit.txt`, tool `Audit.java`), and value-level proof 880/880 exact against R8's lexical source. | `Audit.java`, `VerifyPackage.java` |
| P1 | Several statistical models live side by side; ownership and read/write authority are not separated | **Partly correct.** | **Changed.** The KIDS-shaped lineage tables are gone; provenance is now three generic, W3C-PROV-shaped tables identical on every site (`__raw_activity`, `__raw_fragment`, `__raw_source_record`). Every table and every field carries an authoring class (`FILL / PICK / AUTO / SYSTEM / LINEAGE`) in `__gs_dataset` / `__gs_field`. **Still true:** Access cannot physically lock a table; the server must never accept user-edited metadata as authority (plan §4 already says so). | `__gs_field.authoring_class`: FILL 20, PICK 10, SYSTEM 71, LINEAGE 58 |
| P1 | Three projections are `DRAFT` | **Correct as an observation; not a defect of this file.** These are R8's own approval states, carried truthfully. A generator must not approve content. The statistics page and projection that pointed at the retired dataset are repointed and marked `REVIEW_REQUIRED`, not `READY`. | **Open — steward decision.** | `__gs_projection.approval_state` |
| — | Package identity: the file carries revision 8 and the R8 page-manifest profile | **Not raised as a finding, but it follows from the audit's "contract identity discipline: weak", and it is right.** A candidate must not wear the identity of the approved revision it was derived from. | **Fixed.** `contract_revision = 8+CANDIDATE`, `artifact_profile = ACCESS_PACKAGE_COMPOSER_CANDIDATE`; `derivedFrom` and `approvalState = NOT_APPROVED` recorded in `__gs_metadata`. | `VerifyPackage`: "package identity marked as candidate" |
| U | Every column caption is empty | **Correct as a fact; it is an explicit owner decision made today**, reversing my first build: column headers must be the standard component codes (`TIME_PERIOD`, `AGE`, `INDICATOR`, `OBS_VALUE`, `OBS_STATUS`), not Georgian captions. Descriptions and the Georgian validation message remain. The two requirements conflict; the owner decides. | **Open — owner decision.** | — |
| U | Lookups, Navigation Pane grouping, initial view, fill workflow are not confirmed | **Correct for a Jackcess-only audit; they are confirmed at runtime** in Microsoft Access 16.0.4229 through COM, with window captures: the custom category opens first with the four Georgian groups; 8 pick-lists execute (12 / 43 / 6 / 17 / 17 / 3 / 225 / 4 choices); six `CL_*` codelist views open. | **Done, with runtime evidence.** Forms do **not** exist — that part of the finding stands. | `see-access.ps1`, screenshots on the Desktop |
| U | Real Access fill round-trip not proven | **Partly done.** Live test in real Access, 7 of 7 steps, repeated on 4 fresh copies: a typed row gets its trail at once, a corrected value is recorded, a re-keyed row keeps its trail, a deleted row leaves an invalidated trail, no observation is without a trail, the data is unchanged, Access logged no macro error. | **Open:** validate -> import -> reconciliation of this package through the server API; tenant / retry / rollback / stale-contract negatives; repeat-generation idempotency. | `provenance_test.ps1` |

## Three defects this work found that the audit could not see

1. **The period rule rejected every valid year.** `Like "####"` is a digit wildcard only in ANSI-89 mode; in this
   database it matched nothing, so no person could have added a row. Now `Like "[0-9][0-9][0-9][0-9]"`.
2. **Access evaluates text between two `|` inside an expression.** Item references contain pipes. A value may be
   assigned whole from a local variable, never concatenated; `Replace()` on such a value crashes the Access engine.
   Nothing the data macros write contains a pipe.
3. **A file without `USysApplicationLog` fails its very first data-macro `CreateRecord`** and works ever after. The
   installer spends that first run on a row it removes again, so a person never meets it (4 of 4 fresh copies pass).

## What must happen before anyone calls this ready (agreeing with the audit's list)

1. The statistical mapping becomes a **versioned contract revision** — this file is its draft evidence, not its authority.
2. The builder stops being a script: the same logic moves into the generic composer
   ([design](../../ACCESS-PACKAGE-COMPOSER-DESIGN.md)), generated from the approved site contract revision.
3. Steward decisions: the `DRAFT` projections, the `PROVISIONAL_APPROVED` inference rows, captions versus standard names.
4. Server round-trip of this package, with the negative cases the audit lists.
5. Only then: acceptance checklist of `ACCESS-PACKAGE-ORGANIZATION-PLAN.md`, a new checksum and a provenance artefact.
