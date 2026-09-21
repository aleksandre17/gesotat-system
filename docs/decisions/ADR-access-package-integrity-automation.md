# ADR-012 — Integrity automation inside an Access package

**Status:** ACCEPTED (2026-09-20) · **Owner:** project owner (decision D-1 of the master checklist, given in session)
**Authority:** `AGENTS.md` ("Access artifact-ში credentials, host, password ან executable business logic" is forbidden);
`docs/work/ACCESS-PACKAGE-CANONICAL-STANDARD.md` (B5, P-1…P-6); `docs/work/COMMON-STATISTICAL-CONTRACT-PLAN.md` §4, §10.
**Standards:** W3C PROV-O (derivation, invalidation); deny by default; the server as the only authority.
**Layer:** Delivery (package), with a Security boundary it must not cross.
Language note: English on purpose, so the text is exact.

## 1. Problem (bounded scope)

The owner requires that the provenance trail of every row is present **inside the package at all times and never
breaks**, including for a row a person types. Only the provider itself can record a typed row at the moment it is
saved; in Microsoft Access that mechanism is a table data macro — executable content. `AGENTS.md` forbids executable
*business logic* in an Access artefact. The two requirements meet exactly here, so the line must be drawn explicitly.

## 2. Decision

**Integrity automation is permitted; business logic remains forbidden.** A package MAY carry table data macros that
satisfy *all* of the following, and MUST NOT carry any other executable content:

1. **Generated, never authored.** The macros are produced by the generator from contract metadata (`__gs_dataset`,
   `__gs_key`). No table or column is named by hand. A macro that a person edited is not a valid package.
2. **Provenance only.** They may write to the provenance tables (`__raw_source_record`) and nothing else. They compute
   no value, enforce no business rule, transform no data, call nothing external, and contain no credential, host or path.
3. **Closed shape.** Two kinds of macro, and no third:
   - *row macros* on every fill table — AfterInsert (record HAND_ENTERED), AfterUpdate (record HAND_EDITED; re-point
     the records of a re-keyed row), AfterDelete (mark records invalidated; never delete them);
   - *guard macros* (added 2026-09-20) on every table of authoring class SYSTEM or LINEAGE — BeforeChange and
     BeforeDelete raise one fixed error and do nothing else. They make "not fillable" a fact instead of a label. On the
     provenance record table the guard lets an existing record change only through an update that moves `target_key`
     or sets `invalidated_at` (what the row macros do) and never on a frozen non-memo column; deletion is refused.
     Stated limits: a hand-made INSERT into the provenance table cannot be told from a macro's; a hand edit that
     rewrites a memo column and moves `target_key` in one save passes (memo columns offer neither `Updated()` nor
     `[Old]`). The server rebuilds provenance from the bytes it receives, so neither limit can mislead it.
4. **Never an authority.** The server does not trust what a file says about itself (standard P-6): on load it rebuilds
   provenance from the bytes it received. A package whose macros were removed or altered loses nothing the server
   relies on; it only loses the convenience of an always-visible trail.
5. **Proven in the real provider.** A package is accepted only after the live test passes on fresh copies in
   Microsoft Access: insert, edit, re-key, delete keep the trail whole, and the provider's error log is empty.

VBA modules, forms with code, embedded macros that do anything else, external data connections and linked tables
remain forbidden.

## 3. Why this is not a loophole

The prohibition exists so that a generated artefact never becomes a second place where the business decides
something. Rule 2 and rule 4 keep that intact: the macros decide nothing, and the platform depends on nothing they do.

## 4. Consequences

- The generator needs a step that runs in the real provider (saved queries and data macros cannot be written by
  Jackcess on a Linux server). This is accepted: standard rule A-5 already requires every package to be opened in the
  real provider before hand-over, so a Windows "finishing and acceptance" worker is part of the pipeline regardless
  (master checklist, risk G-1).
- Provider hazards that this mechanism exposed are normative for every generator: standard B6, facts F-3…F-7.
- Rollback: generate the package without the finishing step. Nothing on the server changes.

## 5. Related owner decisions recorded the same day

- **D-2 — column headers.** Headers are the standard component codes (`TIME_PERIOD`, `AGE`, `INDICATOR`,
  `OBS_VALUE`, `OBS_STATUS`); localized captions are a presentation option a contract may switch on (standard A-4),
  off by default.
- **D-3 — numeric envelope.** No digit of the source is lost. The observation value is exact with scale 16, because
  221 of 880 legacy values carry 11–16 decimals. Register decision Q-numeric-envelope (28,10) is superseded for
  migrated data; a contract that wants rounding must declare the rounding rule.
- **D-4 — steward items** stay open: three DRAFT projections, 43 PROVISIONAL_APPROVED inferences, missing labels of
  `AGE_GROUP` (0 of 12) and `KIDS_RESOURCE_SUBCATEGORY` (0 of 4). A generator must not invent approvals or names.

## 6. Evidence

`docs/work/evidence/access-package-candidate-3-2026-09-20.json` — live test 7 of 7 steps on 3 of 3 fresh copies;
`ops/scripts/java/prototype/provenance_macros.ps1`, `provenance_test.ps1`.
