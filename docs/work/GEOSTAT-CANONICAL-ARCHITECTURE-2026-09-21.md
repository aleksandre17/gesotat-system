---
id: ARCH-CANONICAL
type: ARCHITECTURE
title: Canonical architecture
status: COMPLETE
authority: CANONICAL
scope: the derived target architecture — semantic kernel, specializations, contracts, relationships, temporality, provenance, providers, materialization
owner: PHASE-004
created: 2026-09-21
updated: 2026-09-21
related: AUD-CAPABILITY, STD-BENCH-001, DOC-CAD, REC-PASS3, ADR-014, ADR-015, ADR-016
---

# GEOSTAT — CANONICAL ARCHITECTURE

> **REVISED AND CLOSED 2026-09-21.** Sections 1–29 are the first derivation, written before the
> physical model had been reconstructed. **Part II (§30–§40) revises them against
> `ARCH-BASELINE` and `ARCH-DOSSIER` and completes the phase.** Six contradictions were found
> and all six are resolved there, each re-derived rather than patched; the original text is kept
> so the correction remains auditable. **Where Part I and Part II disagree, Part II governs.**
> All 19 physical anti-regression guarantees are preserved or strengthened.
>
> **CORRECTED UNDER INDEPENDENT REVIEW, 2026-09-21.** Two enforcement claims did not survive
> falsification and are corrected in place: **`R-8` / §31.6** — a unique index over
> `business_key_digest` proves that two identical digests cannot coexist, **not** that a digest
> matches the record's actual values; `INV-006` is therefore **`PARTIAL`**, not engine-enforced,
> and `Q39` is withdrawn as the grain-key rule. **`R-9` / §31.7** — declared cardinality does not
> compile to one endpoint-tuple index; the rule is derived per cardinality. **The grain tuple
> remains the semantic authority; the digest is a physical representation only.**

**`PHASE-004`. Derivation, not selection.** No inspected artifact is the target: `PHASE-003`
proved that none passes the benchmark's invariant gate. This document derives the target from
requirements, invariants, standards and evidence, and says explicitly where it preserves,
transforms, supersedes or rejects what exists.

**Owns:** the canonical architecture — thesis, kernel, specializations, identity, grain,
relationships, metadata, temporal model, provenance, dependency, materialization, contracts,
providers, packages, validation, generation, automation, confidentiality boundary, standards
mapping, and the design-level falsification of all of it.
**Does not own:** findings (`REC-CONSOLIDATION`), authority registry (`DOC-CAD`), evaluation
criteria (`STD-BENCH-001`), automation target classes (`STD-AUTO-001`), capability dispositions
(`AUD-CAPABILITY`), statistical domain decisions (`DOM-STATISTICAL` `Q01…Q50`), the
implementation plan (`PHASE-005/006`).

---

## 1. Architecture thesis

> **Every governed thing in this platform is a `Structure` of typed `Component`s over shared
> `Concept`s and `Representation`s. Families differ in their component *roles* and their
> physical *pattern*, never in their declaration mechanism. Declarations are versioned and
> immutable; state, acts and outcomes are records that reference a declaration revision and
> are never fields inside it. Physical location is never identity.**

Four sentences, and each one kills a class of defect this repository actually has:

| Thesis clause | Kills |
|---|---|
| one declaration mechanism, family-specific roles | seven ingestion paths, four chart models, entity data as untyped JSON (`CF-039`), a DSD model unrelated to the entity model |
| shared concepts and representations | the same unit declared in a column, a metadata row and a codelist at once |
| declarations immutable, state referenced not embedded | approval state inside a transport artifact (`CF-033/034`, `CAP-011`), liveness as a mutable column (`RC-001`) |
| location is never identity | `AQ-05`; `__gs_dataset.access_table_name`; byte digests used as correspondence |

**What this is not.** It is not a universal table, not a metadata bag, not a graph database and
not one physical schema. It is *one grammar* with *six proven physical patterns*.

---

## 2. Method and the five alternatives

`UNDERSTAND → MODEL → GENERATE ALTERNATIVES → COMPARE → ATTACK → SIMPLIFY → SYNTHESIZE →
FALSIFY → DECIDE → PROVE`. Each alternative below is given its strongest plausible form, not a
straw man; two of the five are what this repository has actually built at different times.

| | `AL-1` Per-family independent models | `AL-2` Generic metadata / EAV kernel | `AL-3` **Typed structure kernel + family specializations** | `AL-4` Graph-native (RDF or property graph) | `AL-5` Document + schema-per-dataset |
|---|---|---|---|---|---|
| Semantic precision | high per family | **low** — meaning lives in rows | **high** — concept + representation + role | medium — typing is by convention | medium |
| Typed integrity | high | **absent** | high — constraints in storage | weak — SHACL-style, outside the store | weak across documents |
| Dynamicity | **low** — new family = new model | very high | high — new dataset = declaration only | very high | high |
| Cross-family reuse (concepts, codelists) | **none** | accidental | designed | designed | none |
| Grain enforceable | per family | **no** | yes (`BM-INV-24`) | no | no |
| Traversal | per family joins | uniform, meaningless | typed joins + recorded derivation | **strongest** | weak |
| Provider fit (SQL, Access) | good | good | good | **poor** | poor |
| Cost | N× maintenance | one mechanism, no guarantees | **a compiler and a role registry** | new engine + skills | schema drift |
| Benchmark | fails `BM-EC-019/020` | fails `BM-INV-05/24`, `BM-REJ-07` | passes the invariant gate by construction | fails `BM-INV-05`, `BM-EC-017` | fails `BM-INV-05`, `BM-EC-017` |

**Decision: `AL-3`.** Not because it is the middle, but because it is the only one that satisfies
the invariant gate *and* the dynamicity requirement at once. Its cost is real and named: a
semantic compiler, a role vocabulary that must stay closed, and the standing risk that "role"
becomes a place to hide meaning. `§4` and `§21` are the controls on that risk.

**Falsification of the decision.** *Could `AL-1` be right, given that per-family models score
well on integrity?* It is right for one family and wrong for six: `REQ-015/040` require a new
site or family with no core change, and `AL-1` requires a new model each time. *Could `AL-4`
be right, given that `§11` wants traversability?* It buys traversal by giving up storage-level
constraints — and `PHASE-003` measured that typed FKs already deliver the traversals this
platform needs (`candidate_2`: 36 enforced relationships; the manifest's typed edges). **A graph
database is not implied and is not adopted.**

---

## 3. The semantic kernel

Fourteen first-class concepts. Each earns its place by having an identity, a lifecycle and at
least one invariant that nothing else can hold. **Names are responsibilities, not table names.**

| ID | Concept | Identity | Declared or recorded | The invariant only it can hold |
|---|---|---|---|---|
| `K-01` | **Concept** | `concept:<agency>:<code>(<version>)` | declared | one meaning has one definition, reused across every family (`INV-002`) |
| `K-02` | **Representation** | `rep:<code>` | declared | how a concept's values are expressed: logical type, precision/scale, format, or a codelist version. **Exactness lives here** (`BM-INV-09`) |
| `K-03` | **Codelist** (scheme · version · item · hierarchy) | `item:<scheme>:<version>:<code>` | declared + governed promotion | a value domain is versioned, and a code resolves against the exact version it was authored under (`BM-INV-06`) |
| `K-04` | **Structure** | `struct:<agency>:<code>(<version>)` | declared | an ordered set of components; **the single declaration mechanism for every family** |
| `K-05` | **Component** | `(structure, code)` | declared | binds concept + representation + **role** + position + requiredness. The unification point |
| `K-06` | **Grain** | subset of components of `K-04` | declared | what one record *is*; machine-checkable, enforced by a uniqueness constraint over the business key (`BM-INV-24`, `CAP-M04`) |
| `K-07` | **Dataset** (data product) | `dataset:<agency>:<code>` | declared | a governed container: one structure revision, one lifecycle, one owner |
| `K-08` | **Record** (observation / entity row / link / raw row) | its grain tuple | recorded | conforms to exactly one structure revision at its grain (`INV-001`) |
| `K-09` | **Resource** | `resource:<agency>:<code>` | declared | a *logical* thing that may be materialized zero or many times. **Never a path** (`AQ-05`) |
| `K-10` | **Materialization** | `(resource, provider, revision, digest)` | recorded | where and how a resource exists physically, with the generator identity that produced it (`CAP-M01/M02`) |
| `K-11` | **Contract** | `contract:<kind>:<code>(<revision>)` | declared, immutable per revision | the approved declaration of one responsibility; **revisions never mutate** |
| `K-12` | **Derivation** | `(activity, inputs, outputs, rule revision)` | recorded, append-only | what produced what, by which rule version (`BM-INV-23`) |
| `K-13` | **Policy** | `policy:<kind>:<code>(<version>)` | declared | confidentiality, quality, retention, erasability — *referenced* by structures, *evaluated* at gates, never inlined |
| `K-14` | **Publication** (snapshot + membership) | `snapshot:<id>` | recorded | the single release boundary and the **vintage axis** (`RC-001`, `BM-INV-12`) |

**Merged deliberately.** *Dimension, measure, attribute, property, identifier, reference* are
**not** separate concepts — they are values of `K-05`'s `role`. *DSD* is a `Structure` whose
role vocabulary is statistical. *Entity type* is a `Structure` whose role vocabulary is
relational. This merge is the architecture's main simplification and its main risk; `§4.2`
bounds it.

**Separated deliberately.** *Concept* from *Representation* (the same concept may be coded in
one dataset and free text in another); *Resource* from *Materialization* (`AQ-05`); *Contract*
from *Approval* (`CAP-011`); *Record* from *Publication* (a fact exists before it is released).

**Rejected as first-class.** *Metric* (it is a `Concept` used in a measure role — `A-R8`'s
`__stat_metric` becomes a codelist item plus a component, which `candidate_2` already
demonstrated); *Carrier* (a raw-plane parse position, `CAP-014`); *Chart* (a projection, `§28`);
*Metadata assertion* (`CAP-030` — replaced by `§10`).

---

## 4. Specializations — six families, one grammar

`FAM-01 STATISTICAL` · `FAM-02 ENTITY` · `FAM-03 RELATION` · `FAM-04 REFERENCE` ·
`FAM-05 RAW` · `FAM-06 RESOURCE`

A family fixes three things and nothing else:

1. its **role vocabulary** (which `K-05.role` values are legal),
2. its **grain rule** (what may form `K-06`),
3. its **physical pattern** (how a structure of that family is materialized by a provider).

| Family | Legal roles | Grain rule | Physical pattern |
|---|---|---|---|
| `FAM-01` statistical | `DIMENSION`, `MEASURE`, `ATTRIBUTE` (+ attachment level) | all dimensions; measure never in the key | observation table keyed by the dimension tuple, exact-decimal measures |
| `FAM-02` entity | `IDENTIFIER`, `PROPERTY`, `REFERENCE` | declared business key | typed columns per structure; **no document column** (`CF-039` corrected) |
| `FAM-03` relation | `ENDPOINT` (≥2), `PROPERTY`, `REFERENCE` | endpoint tuple, or endpoints + a discriminator | link table with a uniqueness constraint that *is* the declared cardinality (`CAP-M10` closed) |
| `FAM-04` reference | `CODE`, `LABEL`, `PARENT`, `VALIDITY` | `(version, code)` | scheme/version/item/hierarchy, with application-time validity |
| `FAM-05` raw | `LOCATOR`, `LEXICAL`, `ORDINAL`, `FINGERPRINT` | source locator + ordinal | append-only, never normalized, never authority (`§13`) |
| `FAM-06` resource | `LOCATOR`, `DIGEST`, `MEDIA`, `ROLE` | content digest | registry row + provider object; **content identity is the checksum** (`BM-INV-22`) |

### 4.1 Why this is a specialization and not a parallel architecture

`BM-EC-086` demands a declared boundary. Here it is, and it is decidable: **a structure belongs
to the family whose grain rule its declared grain satisfies.** A structure keyed by a dimension
tuple over coded concepts is `FAM-01`; one keyed by two endpoints is `FAM-03`; one keyed by a
source locator is `FAM-05`. A structure that satisfies two rules is a declaration error, not a
judgement call — the compiler rejects it.

### 4.2 The bounded risk of the merge

*Role* could become the place where meaning hides — the `BM-REJ-06` failure in a new costume.
Three controls, all mechanical:

1. **The role vocabulary is closed per family.** A new role is a grammar change (`M2`), with an
   ADR, a compiler change and a migration — never a new row.
2. **A role carries no behaviour of its own.** Behaviour comes from the family's physical
   pattern and from policies; if a role needs a code path, it is a family, not a role.
3. **Every component must resolve to a concept and a representation.** A component with a role
   and no concept is meaningless, and is rejected at validation.

---

## 5. Identity model

| Layer | Identity | Stable across | Never |
|---|---|---|---|
| Semantic | `kind:agency:CODE(version)` — concepts, representations, structures, codelist items, policies | renames, relocation, provider change, regeneration | reused for a different meaning |
| Logical resource | `resource:agency:CODE` | materialization, provider, storage layout | a path, a table name, a URL |
| Record | its declared **grain tuple** | re-ingestion, re-ordering of the source, provider change | a surrogate key alone (`BM-INV-24`) |
| Physical content | **SHA-256 over the canonical form** | copying, moving, re-packaging | a filename, an mtime, a byte-level rebuild (`AQ-05`, `Q39`) |
| Publication | snapshot id + membership | — | inferred from a status column (`RC-001`) |

**The exactness rule (`BM-INV-06`).** Every reference carries the version it was resolved
against, and approval binds the resolved closure by digest. *Latest* never resolves an identity.
`candidate_2` already demonstrates the reference form; this architecture makes it mandatory and
gives it a grammar, which `candidate_2` lacked.

**Surrogates are permitted and are never identity.** A physical key may exist for storage
efficiency; it is not a business key, it never appears in a contract, and it never crosses a
package boundary.

---

## 6. Grain model

Grain is declared (`K-06`), compiled and enforced, in that order.

```text
declared grain            = ordered component subset, in the contract
        ↓ compile
uniqueness constraint     = over exactly those columns, in the provider
        ↓ verify
duplicate-insert test     = must fail, per structure, in CI
```

Three consequences that are corrections of observed defects:

- **Positional grain is a raw-plane concept only.** `(carrier, cell_ordinal)` is legal in
  `FAM-05` and illegal in `FAM-01` — the exact defect `AUD-COMPARATIVE` §5 measured in `A-R8`.
- **A surrogate key never satisfies a declared grain** (`BM-INV-24`).
- **Grain is machine-checkable prose no longer** (`CAP-M04`): `business_grain` as free text is
  replaced by a component list, and the prose survives as a label.

---

## 7. Relationship architecture — and the three families resolved

`AUD-CAPABILITY` §11.1 established that derivation, dependency and composition are three graphs
with different direction, mutability and lifetime. **This design goes one step further, and the
step is the important one:**

> **Two are stored and one is derived.**
> **Composition is *declared*** (it is part of a structure/contract, versioned with it).
> **Derivation is *recorded*** (append-only, at execution, about instances).
> **Dependency/impact is *computed*** from the first two plus consumer registrations — storing
> it would duplicate deterministically derivable information (`BM-REJ-10`, `CAD-05`).

| Family | Authority | Mutability | Storage | Answers |
|---|---|---|---|---|
| **Composition / structural** | the contract | immutable per revision | components, endpoints, containment declarations | *what is this made of; what does it reference* |
| **Derivation / provenance** | the run that produced it | append-only, never corrected in place | derivation records (`K-12`) with rule revision | *where did this come from; what produced it* |
| **Dependency / impact** | **derived** | recomputed on demand | none — a query | *what breaks if this changes; what must be regenerated* |

**This is the answer to "do they share one storage mechanism?" — no, and not because they are
three tables: because only two are facts at all.** A universal edge type would have stored all
three and constrained none (`§4` anti-pattern list).

### 7.1 Relationship semantics, all thirteen

`AUD-CAPABILITY` §9.2 measured that seven of thirteen are inexpressible today. Where each lives
here:

| Dimension | Where it is expressed |
|---|---|
| role, direction | `K-05.role` on a `FAM-03` endpoint |
| cardinality | declared on the relation structure; **compiled into the uniqueness constraint** |
| ownership, composition/containment | a declared component property (`OWNS` / `CONTAINS`) on the endpoint; drives cascade and retention |
| dependency | derived (`§7`) |
| referential integrity | provider constraint, compiled from the reference component |
| temporal validity | `§14` — declared per structure, not universal |
| version scope | every reference carries its resolved version (`§5`) |
| classification | a reference to a codelist item |
| derivation | `K-12`, not a relation |
| provenance | `K-12` |
| materialization | `K-10`, not a relation |
| **many-to-many** | a `FAM-03` structure whose grain is the endpoint tuple — **declarable, typed, constrained** (`CAP-M10` closed) |

**M:N carrying its own attributes falls out for free**: a relation structure may declare
`PROPERTY` components, so the link is a first-class record rather than a join table with extras
bolted on. This is what `AUD-CAPABILITY` scenario 4 failed on.

---

## 8. Metadata architecture

The eleven responsibilities of `AUD-CAPABILITY` §7 are not eleven tables and are not one bag.
They resolve into **four architectural homes**, chosen by *lifecycle*, which is what actually
differs:

| Home | Holds | Lifecycle | Authority |
|---|---|---|---|
| **The declaration** (`K-04/05/11`) | semantic, structure and dataset metadata; presentation declarations | changes only with a revision | the contract |
| **The record** (`K-08`) | observation metadata — anything that varies per fact | changes with the data | the data |
| **The evidence** (`K-12`, gate records) | provenance, quality outcomes, operational metadata, **inference metadata** | append-only, per run | the run |
| **The policy** (`K-13`) | confidentiality, retention, erasability, quality rules | versioned, referenced, evaluated | the steward |

**Governance metadata has no home in any of the four, and that is the point.** Approval is an
*act*: it is a record that references a contract revision and a digest, held by the approval
authority — never a field inside the thing being approved (`CAP-011` `REPLACE`, `CF-033/034`).

**The declaration-vs-state test, applied here:** *if a field changes without a new revision it
belongs in the record, the evidence or the policy — not in the declaration.* Applying the test
to today's artifacts moves `approvalState`, `lifecycleState`, `operation` and `parse_status` out
of the declaration, which is exactly where they should never have been.

**`CAP-M03` closed by construction:** there is no unschema'd namespace, because there is no
generic assertion mechanism. A fact that a structure's declaration cannot express is either a
component (add it, with a revision), a policy (reference it) or evidence (record it).

---

## 9. Representation roles — tidy, long, wide, dimensional

Adapted from DDI-CDI's insight (`STANDARDS.md`: concepts `ADAPT`, serialization `REJECT`):

> **The same semantic datum may appear in several structural representations, playing a
> different role in each. Semantic identity is the datum's; representation role is the
> structure's.**

| Representation | Where it is legitimate here | Constraint |
|---|---|---|
| **Long / tidy** — one row per fact | the **canonical** form of `FAM-01` | the only form with an enforceable dimension-tuple grain |
| **Wide** — one row per entity, one column per period/indicator | authoring surfaces and projections | a *declared derivation* of a long structure; never the canonical store |
| **Dimensional** — cube addressing | query and export projections | derived from long by the declared component roles |
| **Key-value** | never in canonical state | the `BM-REJ-07` boundary |

A wide authoring sheet and the long canonical table are therefore **one structure with two
representations**, related by a declared pivot over named components — not two models and not a
transformation script. This is the design's answer to `AUD-CAPABILITY` envelope row 2, and it is
also why an Access authoring file can be wide while the canonical store is long, **without a
second semantic model**.

---

## 10. Raw → canonical → derived → published

| Plane | Identity | Mutability | Authority for | Never |
|---|---|---|---|---|
| **External source** | outside the platform | — | nothing | trusted |
| **Ingested raw** (`FAM-05`) | locator + ordinal + fingerprint | append-only, immutable | *fidelity* — what the source literally said | semantic authority (`§13`) |
| **Canonical** (`FAM-01…04`) | declared grain | corrected only by a new record under a new revision | *meaning* | a place to keep source formatting |
| **Derived** | declared grain + rule revision | recomputable, disposable | nothing — it is a projection of canonical | an independent truth |
| **Published** (`K-14`) | snapshot + membership | immutable once released | *what the public may rely on* | inferred from a status column |

**Source fidelity is preserved by keeping the lexical form in `FAM-05`, not by weakening the
canonical type.** This is the one capability `A-R7`/`A-R8` got exactly right (`CAP-013`) and the
reason 880 of 880 values round-tripped; the architecture keeps the lexical value and drops the
`DOUBLE`.

**Correction is a first-class path, not an exception** (`§21` lifecycle): a corrected value is a
new canonical record produced by a governed reprocess under a corrected revision, published in a
later snapshot. Nothing is edited in place; nothing is silently replaced.

---

## 11. Temporal model — `D2` resolved

**Do not make everything bitemporal.** Three different temporal needs exist and only one of them
requires a temporal mechanism at all:

| Responsibility | Temporal need | Mechanism | Why not more |
|---|---|---|---|
| Declarations (contracts, structures, policies) | transaction time only | **immutable revisions** — a revision *is* a transaction-time interval | valid time is meaningless for a declaration: it applies when approved |
| Codelist items, entity validity (`FAM-04`, `FAM-02`) | **application (valid) time** | declared `VALIDITY` components: `valid_from` / `valid_to` | a region that merged in 2020 must remain resolvable for 2015 data |
| Observations (`FAM-01`) | both — **and they are already there** | **valid time is the `TIME_PERIOD` dimension** (it is data, in the key); **transaction time is the snapshot** that published it | a system-versioned table would duplicate the snapshot axis and create a second vintage authority |

**The consequence is the strongest simplification in this design:** as-of reconstruction,
late-arriving data, corrections and historical reproduction are all expressed by *snapshot
selection plus the period dimension*, with **no bitemporal tables anywhere**.

- *as-of query* → serve snapshot S (already the publication model).
- *late-arriving datum* → a new record in a later snapshot; its period is unchanged.
- *correction* → same grain tuple, later snapshot, different value, derivation records the rule.
- *restatement vs correction* → distinguishable, because the derivation record says which rule
  ran; nothing else has to encode the distinction.
- *tombstone* → a record with a withdrawal status in a later snapshot; the earlier snapshot stays
  true of its own time.

`SQL:2011` system-versioning stays `CONDITIONAL` in the standards profile and is **not adopted
for the canonical core** — its responsibility is covered. It remains available to a provider that
wants it for operational history, behind the provider boundary.

---

## 12. Provenance and derivation

`K-12` is one append-only structure with a PROV-O-adapted shape (`STANDARDS.md`: concepts
`ADAPT`, RDF `REJECT`), generalized from what `candidate_2` demonstrated:

```text
Activity (kind, agent, software+version, started, finished)
   ├── used      → input records / resources / contract revisions   (by identity + digest)
   └── generated → output records / resources                       (by identity + digest)
                    with rule revision, and invalidated_at for withdrawal
```

**Three requirements this shape must meet and `candidate_2`'s does not:**

1. every `used`/`generated` edge carries the **digest** of what it points at, so the chain is
   verifiable and not merely navigable (`BM-EC-040`, evidence-bound traversal);
2. multi-input derivation is native — an activity may `use` many inputs across datasets
   (`AUD-CAPABILITY` scenario 6);
3. it never carries semantics: a derivation says *what happened*, never *what something means*.

`OpenLineage` stays `CONDITIONAL`: if runtime events are emitted, they are an **emission
format** for this same model, and PROV-O concepts remain the authority. Two lineage models are
forbidden.

---

## 13. Raw must not become authority — the enforcement

Stated as a rule with a test rather than an aspiration:

> **No canonical or published read path may resolve a value from `FAM-05`.**
> *Test:* a static check that no serving or projection query references a raw structure, plus a
> negative test per family.

`FAM-05` exists to answer *"what did the source literally say"* — nothing else. `A-R8`'s
`__stat_kids_statistical_input` violated this by holding a raw grain in a statistical family;
here the same information lives in `FAM-05` and the canonical observation is derived from it
with a recorded derivation, which is what `candidate_2` demonstrated at 880↔880.

---

## 14. Contract architecture

**A contract is a versioned declaration of one responsibility.** Not a document that grows.

| Kind | Declares | Family scope |
|---|---|---|
| `STRUCTURE` | structure, components, grain, policies referenced | any |
| `SOURCE` | where data comes from, admission rules, mapping to components | `FAM-05` → canonical |
| `PROJECTION` | a declared view: selection, pivot, aggregation, presentation binding | serving |
| `PACKAGE` | which resources travel together, in which provider form | interchange |
| `POLICY` | confidentiality, quality, retention, erasability rules | referenced by the above |

**All five share one lifecycle machinery and nothing else:**

```text
DECLARATION → VALIDATION → APPROVAL → MATERIALIZATION/EXECUTION → PUBLICATION
                                   ↘ CORRECTION / REPROCESSING ↗
                                                               → RETIREMENT
```

Shared: identity, revision, digest (`semanticDigest` + `revisionDigest`, per `Q39`),
references-by-exact-revision, approval as an external record, retirement as a terminal state.
**Not shared: payload schema, and which stages apply.** A `POLICY` contract has no
materialization stage; a `PACKAGE` contract has no correction stage. Forcing identical states on
different semantics is explicitly rejected.

**The declaration-vs-state test is a build-time check, not advice:** a field that can change
without a new revision fails the check and must move to a record.

---

## 15. Classifier and reference-data ownership — `D6`

| Question | Answer |
|---|---|
| Who owns a codelist's meaning? | exactly one **owning agency**, declared on the scheme (`K-03`) |
| May another site use it? | yes, **by reference to an exact version** — never by copying |
| May another site extend it? | only by declaring its own scheme that **references** the base, with a declared crosswalk (`Q26` typed cardinality); it may not add items to somebody else's version |
| Must it be physically central? | **no.** Semantic authority is central; a local materialization is a cache with a digest and a refresh rule (`§17`) |
| What if the base changes? | a new version; existing references keep resolving to the version they pinned (`BM-INV-06`) |

**Semantic authority centralized, physical storage free.** This is the answer to *"do not assume
central physical storage is required merely because semantic authority is centralized"*.

---

## 16. Resource, materialization and packages

```text
Resource (logical, K-09)
   └── Materialization (K-10)  = provider + locator + digest + generator id/version + contract revision
                                 ├── Access .accdb
                                 ├── SQL table
                                 ├── object-store object
                                 └── package entry
```

- A resource may have **many** materializations; none of them is its identity.
- A materialization is **verifiable**: digest + generator identity + the contract revision it
  realizes. This closes `CAP-M01` and `CAP-M02` — and `Q48` already specifies the manifest that
  carries them, so the design adopts that specification rather than inventing one.
- **Correspondence is semantic, not byte-level**: a materialization matches its contract when the
  *semantic digest* of its content equals the semantic digest of the approved plan. `AQ-05` and
  `Q39` both proved byte digests cannot do this job; byte digests remain correct for immutable
  files (`A-PKG` verified 451/451).

**Package architecture** keeps what `A-PKG` demonstrated and adds what it lacked: manifest with
per-file digests ✔, declared typed edges ✔, contract code + revision ✔, **plus** the semantic
correspondence digest and the generator identity ✘→✔. A package is an `INTERCHANGE CONTRACT`
(`AUD-COMPARATIVE` §2), never canonical persistence.

---

## 17. Provider capability architecture — `CF-038` / `RC-003` resolved at design level

**One producer, one registry, one rule.**

```text
declared requirement  ≤  provider capability  ≤  canonical capability
```

| Element | Design |
|---|---|
| Authority | a single provider-capability registry — the **only** place a provider limit is stated |
| Producer | one governed producer; Spring properties and planner constants are **eliminated**, not mirrored (`CF-038`'s three authorities collapse to one) |
| Content | per `(provider, family, capability)`: numeric precision/scale, identifier length, column count, supported types, constraint kinds, index kinds, page/row limits |
| Use | the compiler consults it to plan a materialization; a declaration exceeding it is **rejected at authoring time**, never silently truncated (`BM-INV-18`) |
| Canonical storage | always the **superset** — provider limits never reach it (`ADR-016` rule 3/4) |

**Provider specialization is permitted and bounded:** a provider may use a stronger capability
(a better index, a native type) as long as the canonical semantics are unchanged and the choice
is recorded in the materialization. Lowest-common-denominator abstraction stays prohibited.

---

## 18. Access — canonical boundary only

Per `ADR-016` and `§25` of the phase instruction, this design states **only what the canonical
architecture requires of an Access implementation**. The dedicated refinement remains a later
work item.

| Access responsibility | What canonical architecture requires |
|---|---|
| **Physical provider** | declare its capabilities in the one registry; materialize a structure through the family's physical pattern; no canonical semantics anywhere in the file |
| **Authoring surface** | present the *wide* representation (`§9`) of a long canonical structure; fill everything derivable (codes, units, references); declare `authoring_class` per component so the human fills only genuine meaning (`CAP-027`) |
| **Round-trip carrier** | on return, validate against the **contract**, not against itself; reject a package whose semantic digest does not match the approved plan; **may reference canonical definitions, may never define or approve one** |

**What Access may no longer do:** carry `approvalState`, define a metadata schema, hold a raw
grain in a statistical family, or let a table name be an identity.

---

## 19. Validation and generation symmetry

```text
contract revision ──generator──▶ materialization ──validator──▶ contract revision
```

| Case | Requirement |
|---|---|
| Structure and constraints | **round-trip equivalence** — what the generator emits, the validator accepts, and nothing else |
| Values | **semantic equivalence** — lexical form may differ (`0100` vs `100`), meaning may not |
| Presentation (layout, navigation grouping, column order) | **loss allowed and declared** — it is regenerable and carries no semantics |
| Grammar ↔ published schema | **bidirectional conformance corpus in CI** (`DR-005`, `BM-INV-21`) |

**Asymmetry is legal only where declared.** The current state — a reader that validates a
package against itself — is the defect this replaces (`REC-COVERAGE` L23/L24).

---

## 20. Maximum safe automation, applied

Everything below is derived from authority; a human never types it:

physical table/column/key/index/relationship names · codelist items and pinned versions inside
an authoring artifact · unit/status/confidentiality defaults from attachment declarations ·
lineage records · package manifest entries, digests and typed edges · projections for API,
table, chart and export from one declaration · impact sets (`§7`) · conformance matrix rows ·
migration DDL from a structure diff.

A human declares only genuine meaning: what a dataset means and its grain · concept definitions ·
promotion of a proposed classifier · approval, four-eyes, publication · whether a suppression is
sufficient · the data values themselves.

**The rule that keeps this safe** (`MSA-R8`): derivation must land in an **inspectable
artifact** — a compiled plan, a constraint, a generated schema — never in a runtime-only
interpretation. `DECLARE ONCE → DERIVE` must not become `DECLARE GENERIC METADATA → INTERPRET AT
RUNTIME`.

---

## 21. Confidentiality and disclosure — `D1`

**Confidentiality is enforcement, not presentation.**

| Layer | Responsibility |
|---|---|
| Declaration | a confidentiality policy (`K-13`) referenced by a structure, attachable at dataset, dimension-group or cell level |
| Canonical | the value is stored; confidentiality is not a reason to lose data |
| **Publication gate** | evaluates the policy **over the publication closure** — the set being published *plus what is already published for that structure* — and computes the complement set that must also be withheld |
| Serving | may only serve what publication released; it never re-evaluates policy |

**This is why the gate, not the projection, owns it:** complementary suppression is only decidable
against everything already public, and the publication authority is the only component that knows
that set (`K-14` membership). The architecture provides the hook and the closure; **the
disclosure-control policy itself is the steward's and is not invented here** (`BM-INV-25`,
`BM-EC-087`).

---

## 22. Legal erasure vs immutability — `D3`

Three architectures were compared; the third is adopted as the *architectural* answer, with the
policy half explicitly left to the owner.

| Option | Verdict |
|---|---|
| Mutable history (edit the past) | **rejected** — destroys reproducibility, auditability and every invariant that depends on immutability |
| Full crypto-erasure (encrypt everything, drop keys) | **rejected as the general mechanism** — key management becomes a second authority over data availability, and it erases far more than required |
| **Separation of erasable payload from immutable structure** | **adopted** |

```text
immutable, never erased:  identity · grain tuple · digests · derivation records · membership
erasable payload:         personal or restricted values, raw lexical content, source documents
```

- Structures **reference** payload by digest; they never embed it.
- Erasure removes the payload and leaves a **tombstone**: the digest, the fact of erasure, its
  authority and its time. Lineage stays traversable; reproduction becomes provably impossible
  rather than silently wrong.
- **Erasability is a declared property of a data class** (a `K-13` policy), so it is known at
  design time which structures must keep payload separable — you cannot retrofit separability.

**Owner decision required (bounded):** which data classes are erasable, under which jurisdiction
and retention rule, and whether a published statistical aggregate is ever in scope. Architecture
cannot answer these; it can only guarantee they are answerable.

---

## 23. Consumers and impact — `D4`

The minimum that makes impact derivable, and no more:

| Element | Design |
|---|---|
| Consumer registration | a consumer declares a dependency on a **contract revision** (not on a table). Kinds: internal projection, package, external API client, downstream dataset |
| Impact query | for a proposed change: walk composition (declared) + consumer registrations, classify each as *compatible*, *regeneration required*, *breaking* |
| Staleness | a materialization whose contract revision is superseded is **derivably stale** — no flag needed |

**Not built:** a general dependency graph engine. The impact set is a query over two things that
already exist for other reasons, which is exactly why `§7` refuses to store it.

---

## 24. Cross-dataset constraints — `D5`

| Aspect | Design |
|---|---|
| Authority | a constraint contract, owned by the steward of the *asserting* dataset, referencing both sides by exact revision |
| Scope | within one publication closure; cross-snapshot constraints are explicitly out of scope until a requirement exists |
| Evaluation | at the **publication gate**, with the same closure the disclosure rule uses |
| Failure | fail-closed: publication is refused, with the violating tuples as evidence |
| Versioning | a constraint pins both revisions; a new revision on either side requires re-approval |

This keeps cross-resource integrity out of application code (`§18` of the instruction) without
inventing a distributed constraint engine.

---

## 25. Versioning and evolution

| Change | Requires |
|---|---|
| New component, optional | new **minor** structure revision; existing records remain valid |
| Grain change, type narrowing, component removal, role change | new **major** revision + migration + consumer impact review (`§23`) |
| New codelist item | new codelist **version**; structures pinning the old version are unaffected |
| Label, description, presentation | new revision, `semanticDigest` unchanged (`Q39`'s two-digest rule makes this mechanical) |
| Policy change | new policy version; structures referencing it re-evaluate at the next gate |
| Provider capability change | registry update; may make an existing declaration unmaterializable — reported, never silently degraded |

**One number, one meaning.** Semantic revision, package version and provider build are three
identities and are never conflated — `A-R8` conflated the last two and `AQ-05` was the result.

---

## 26. Performance and physical reality

Design-level bounds; **no numbers are invented** (`BM-Q-03` is unresolved and remains so).

| Path | Shape | Bound |
|---|---|---|
| Observation read by dimension tuple | index on the grain | index seek; scales with selectivity, not table size |
| Aggregation over a dimension | pushdown to the provider; **never in-memory over a page** (`CF-027` corrected) | declared query budget, fail-closed |
| Codelist resolution | small, cacheable per version; immutable per version so caching is safe | O(1) per lookup |
| Relationship traversal | typed joins over declared endpoints | bounded by declared cardinality |
| Derivation/lineage traversal | append-only table, indexed by output identity | bounded by depth, which the derivation graph makes finite |
| Impact query | composition + consumer registry | bounded by declared consumers, not by data volume |
| Validation | per structure, per constraint | linear in components, not in rows, except grain checks |
| Generation | one pass over the compiled plan | deterministic, regenerable |

**The one structural risk this design accepts:** the compiler is on the critical path of every
materialization. It is mitigated by compiling to an immutable plan with a digest (`INV-013`) and
caching by that digest — never by skipping compilation.

---

## 27. Security and governance boundary

Canonical architecture owns only what semantics require:

- **which structures carry confidentiality policy** and at what attachment level (`§21`);
- **tenant scope as a declared property** of a dataset, enforced at the data-access boundary,
  deny-by-default, with a register of exemptions (`INV-009`, `QF-003`);
- **approval as an act** with an identity, a time and a bound digest (`§8`);
- **publication restrictions** including embargo (`Q12`'s `publish_not_before`), evaluated at the
  gate.

It does **not** own the application's authentication, role model or session handling. Those are
`SEC-001/002`'s and are referenced, not redesigned.

---

## 28. Presentation and serving

**One canonical semantics, many declared projections.** A projection contract (`§14`) declares
selection, pivot, aggregation and presentation binding over a structure; API, table, chart,
export and package are **projection kinds**, not separate models (`BM-INV-15`, `REQ-020`).

Rules: a projection may not introduce a dimension, unit, code or permission that its structure
does not declare; a projection carries no semantic authority; presentation metadata lives in the
declaration and never in the canonical record.

**The Admin/UI blueprint remains parked.** Nothing here designs it.

---

## 29. Standards mapping

| Standard | Responsibility | Boundary | Deviation | Interoperability consequence |
|---|---|---|---|---|
| **SDMX** (`ADOPT` bounded, `ADR-015`) | `FAM-01` structural metadata and exchange | **only** where data is an observation set keyed by a declared dimension tuple | roles are the platform's closed vocabulary, mapped to SDMX roles; the claim itself stays `Q47`'s | SDMX-CSV/JSON export conformant within the declared subset |
| **DDI-CDI** (`ADAPT`, concepts) | wide/long/dimensional representation roles (`§9`) | concepts only; **no XMI/RDF** | representation roles are declared in the contract, not in a DDI document | none claimed — internal model only |
| **PROV-O** (`ADAPT`) | derivation model (`§12`) | entity/activity/agent concepts; **no RDF** | digests added to every edge | conceptual alignment only |
| **OpenLineage** (`CONDITIONAL`) | runtime lineage emission | emission format only, if `W-05` lands | PROV-O remains the authority | tools can consume events |
| **ISO/IEC 11179** (`CONDITIONAL`) | concept and value-domain vocabulary (`K-01/03`) | vocabulary only | — | none claimed |
| **ISO/IEC 25012** (`ADAPT`) | quality characteristics in policies (`K-13`) | characteristics only; outcomes are evidence | — | none claimed |
| **JSON Schema 2020-12** (`ADOPT`) | contract grammar (`M2`) | shape validation only; never authorization | parity corpus required (`BM-INV-21`) | contracts are externally validatable |
| **SemVer** (`ADOPT`) | revision compatibility intent (`§25`) | intent; evidence still decides | major/minor rules stated above | consumers can reason about breakage |
| **FIPS 180-4 + RFC 8785 + NFC** (`ADOPT`) | canonical digests (`§5`, `§16`) | canonicalize, then hash | two digests per `Q39` | digests are reproducible by third parties |
| **ISO 8601** (`ADOPT`) | instants and intervals | statistical reporting periods stay `Q19`'s | — | — |
| **Data Package v2** (`REFERENCE ONLY`) | package design prior art | not adopted — no contract-revision binding, no typed edges | — | would require a mapping if ever needed |
| **SQL:2011 temporal** (`CONDITIONAL`) | provider-side operational history | **not used for canonical temporality** (`§11`) | — | — |
| **SHACL, RDF Data Cube** | — | `REFERENCE ONLY` / `REJECT` | — | — |

**No settled disposition was reopened.** No serialization was adopted for a vocabulary that is
only conceptually useful.

---
---

# PART II — REVISION AGAINST PHYSICAL EVIDENCE, AND CLOSURE

**Sections 1–29 above are the first derivation. Part II revises them against `ARCH-BASELINE`
and `ARCH-DOSSIER`, then completes the phase.** Where Part II and Part I disagree, **Part II
governs**; the original text is retained because a revision that erases what it corrected is not
auditable.

---

## 30. Revisions — each one re-derived, not patched

### R-1 · Observation temporality

| | |
|---|---|
| **Old draft decision** | §11: bitemporality falls out of *period dimension × snapshot*; **no temporal columns are needed anywhere**. |
| **Physical evidence** | `statistics.observation` carries `valid_from`, `valid_to`, `is_current` (11 288 rows). Dossier guarantee #7. |
| **Why it failed** | The snapshot axis answers *"what did publication S say"*. It cannot answer *"what did this fact say between T1 and T2 independently of publication"*, and it cannot represent a supersession **inside** a dataset version. The draft mistook one temporal axis for both. |
| **Revised decision** | **Three temporal axes, each with a named owner, and none universal.** (a) *Business time* — the `TIME_PERIOD` dimension, part of the grain, owned by the data. (b) *Assertion time* — `valid_from` / `valid_to` / `is_current` on the canonical record, owned by the record: when the platform asserted this value. (c) *Release time* — snapshot membership, owned by publication. A structure declares which axes it needs; `FAM-01` requires (a) and (b), `FAM-04` requires (b) for item validity, `FAM-02` declares (b) only when the domain has business validity. |
| **Preserved / strengthened** | Guarantee #7 preserved **and strengthened**: `is_current` becomes a derived consequence of `valid_to IS NULL` rather than an independently writable flag, removing the class of defect where the two disagree. |
| **Falsification test** | Supersede a fact inside one dataset version without publishing: the prior row must become non-current with `valid_to` set, the new row current, both retained, and snapshot S must still serve the value it published. A design that cannot do this fails. |

### R-2 · The kernel and `platform.contract_structure`

| | |
|---|---|
| **Old draft decision** | §3: a fourteen-concept kernel presented as a derivation from requirements. |
| **Physical evidence** | `platform.contract_structure` already carries `structure_code`, `structure_kind`, **`data_class`**, **`grain`**, `authority_mode`, `lifecycle_policy`, `allowed_content`, `forbidden_content`, `lineage_policy`, `quality_policy_code`, `confidentiality_policy_code`, `standard_code`, `UNIQUE(namespace_id, structure_code, revision)` — with 5 inbound FKs. Dossier guarantee #14. |
| **Why it failed** | Not wrong — **overstated**. Six of the fourteen concepts already exist physically in stronger form than the draft described, including the one the draft treated as its centrepiece. Claiming novelty where a working implementation exists is how a redesign discards a proven pattern by accident. |
| **Revised decision** | The kernel is **the governed evolution of `platform.contract_structure` and its neighbours**, not an invention. `K-04` Structure = `contract_structure` + an explicit component list; `K-06` Grain = its `grain` column **promoted from prose to a component reference set**; `K-13` Policy = its existing policy-code references **promoted from loose codes to versioned references**; `authority_mode` and `lifecycle_policy` are adopted unchanged, because they already express what the draft was about to re-invent. |
| **Preserved / strengthened** | `UNIQUE(namespace, code, revision)` preserved verbatim. `grain` strengthened from `NVARCHAR(1000)` prose to a machine-checkable component set (`CAP-M04`). |
| **Falsification test** | For every column of `contract_structure`, name where it lives in the revised kernel. A column with no home is a capability about to be lost. **Run: 13 of 13 have a home.** |

### R-3 · Typed patterns the family model did not express

| | |
|---|---|
| **Old draft decision** | §4: six families, `FAM-02` entity = "typed columns per structure". |
| **Physical evidence** | `entity.localized_text` PK `(entity_id, field_code, language_tag)` — 2 822 rows; `entity.resource_locator` PK `(entity_id, locator_kind, language_tag)` — 2 466; `entity.entity_classification` PK `(entity_id, attribute_id, classification_item_id)`; `reference.classification_item_snapshot` PK `(snapshot_id, classification_item_id)`. Guarantees #8–#11. |
| **Why it failed** | The draft had no concept of a *typed narrow table keyed by (row, field, discriminator)*. It would have modelled localization as either per-structure columns — impossible for an open language set — or as metadata, which is the `CAP-030` defect it had just rejected. These four tables are the answer to a problem the draft had not noticed it had. |
| **Revised decision** | Promote the pattern to a kernel mechanism: **value tables partitioned by value domain, keyed by `(record, component, discriminator)`**, where the component is contract-governed and the discriminator is a declared axis (language, locator kind, ordinal). `localized_text` is the *text* value table with `language_tag` as discriminator; `resource_locator` is the *locator* value table; `entity_classification` is the *reference* value table. See §31. |
| **Preserved / strengthened** | All four PKs preserved as instances of one rule. `classification_item_snapshot` preserved unchanged — a published snapshot pins codelist content, which nothing else provides. |
| **Falsification test** | Add a localized field to a structure and a new language: neither may require DDL, and `(record, component, language)` uniqueness must still be engine-enforced. |

### R-4 · Enumerated-domain enforcement

| | |
|---|---|
| **Old draft decision** | §3 `K-03`: coded domains become codelist references. |
| **Physical evidence** | 115 CHECK constraints live, 39 of them enumerated domains across 34 columns — **and every one of them is on a platform-kernel table** (`verification_status`, `classifier_proposal.state`, `site_contract_revision.status`, `contract_namespace.authority_mode` …), not on user-dataset data. |
| **Why it failed** | The draft did not distinguish *platform state vocabularies* from *user value domains*. Converting the former to codelist rows would replace an engine-enforced CHECK with a join — a regression — while leaving the latter as CHECKs would require DDL per dataset. |
| **Revised decision** | **Two rules, by owner.** Platform state vocabularies (lifecycle states, verification statuses, authority modes) **stay `CHECK` constraints on kernel tables**: they change only with a platform migration, which is exactly when a CHECK should change. User value domains **are codelist references with an FK to an exact version**: engine-enforced *and* versioned. **Neither may become a string validated in application code.** |
| **Preserved / strengthened** | All 39 enumerated CHECKs preserved untouched. User domains strengthened from "CHECK if we had one" to FK + version pinning. |
| **Falsification test** | Insert an invalid platform state → rejected by CHECK. Insert an invalid user code → rejected by FK. Neither rejection may depend on application code being correct. |

### R-5 · Generic relationship abstraction

| | |
|---|---|
| **Old draft decision** | §7: three relationship families; composition declared, derivation recorded, dependency computed. |
| **Physical evidence** | 135 FKs, all `NO_ACTION`; `entity.entity_link` has a surrogate PK and **no endpoint uniqueness** (`CF-040`); no dependency table exists anywhere. |
| **Why it (mostly) held** | Confirmed by evidence — the draft's structure matches the physical reality, including the absence of a stored dependency graph. |
| **Revised decision** | Unchanged in structure, **strengthened in enforcement**: a `FAM-03` relation's declared cardinality compiles to uniqueness in the link value table, closing `CF-040`. **Superseded in detail by `R-9`/§31.7** — "a uniqueness constraint over the endpoint tuple" is the correct rule for `MANY_TO_MANY` only; the per-cardinality rule is derived in §31.7. The zero-cascade policy is **adopted as an invariant**, not merely inherited: deletion of governed data is refused while referenced. |
| **Preserved / strengthened** | Guarantee #2 (135 FKs, no cascades) preserved as policy. `CF-040` strengthened from unenforced to enforced, **for all four cardinalities** (`R-9`). |
| **Falsification test** | Declare `ONE_TO_MANY`, then attempt a second parent for one child → rejected by `ux_rel_target_one`, not by code. Three further tests, one per remaining cardinality, in §31.7. |

### R-6 · A second presentation substrate exists

| | |
|---|---|
| **Old draft decision** | §28: one canonical semantics, many declared projections. |
| **Physical evidence** | `dbo.chart_definitions` and `dbo.page_nodes` exist physically with JPA ownership — a live second presentation substrate. |
| **Why it failed** | The draft asserted the property as though it held. It does not hold today. |
| **Revised decision** | The property is a **target with a named exception**: `dbo.chart_definitions` / `dbo.page_nodes` are `LEGACY, OUTSIDE THE CANONICAL BOUNDARY`, scheduled by `DR-001` and `PHASE-011`, and the canonical core declares `dbo.*` outside itself explicitly so proximity cannot be mistaken for membership. |
| **Preserved / strengthened** | No guarantee lost; an untrue claim replaced by a bounded obligation. |
| **Falsification test** | A canonical projection must never read `dbo.*`. Static check. |

### R-7 · Recovery-evidence correction (routed, not absorbed)

`REC-CONSOLIDATION` RC-003 states *"stored procedures (none exist in 113 migrations)"*;
`publication.usp_kids_r8_reconciliation` exists (migrations 073, 075). **RC-003's conclusion
stands on its other four axes**; the supporting statement is false and is routed to its owner.
The architectural consequence: the database is a legitimate place for **reconciliation**, and
§24's cross-dataset constraints may be evaluated there.

### R-8 · Grain digest correspondence — **the enforcement claim was overstated**

*Source: independent review falsification, not physical evidence. Derivation in §31.6.*

| | |
|---|---|
| **Old Part II decision** | §31.2 classified declared-grain uniqueness as *engine-enforced*, citing `UNIQUE(structure_revision_id, business_key_digest)` and attributing the digest rule to `Q39` (JCS + SHA-256). |
| **The falsification** | A unique index over a digest proves only that **two identical digests cannot coexist** (invariant A). It does not prove that the digest **is** the digest of the record's actual persisted grain values (invariant B). Grain values live across typed value-domain tables; nothing in the draft prevented persisting values `X` alongside `digest(Y)`. |
| **Why it failed** | The draft inferred B from application correctness. A SQL Server `CHECK` cannot reference another table, a persisted computed column cannot leave its own row, and an indexed view cannot assemble a multi-row tuple into one hashable row. **No declarative constraint can bind B** unless the grain values sit in the record row as columns — which is a per-structure table, i.e. exactly what the stable kernel forbids by default. |
| **Revised decision** | A is declarative and unchanged. B is enforced by **generation, not acceptance** (§31.6): the digest has no writer, is derived in-transaction from the persisted rows, and is re-verified by trigger. `Q39` is **withdrawn as the grain-key rule** — it is a contract-document rule the engine cannot recompute, and reusing it would have forced either a second JCS implementation in T-SQL or the loss of engine verification. |
| **Preserved / strengthened** | Baseline `INV-006` is **application-only today**; this moves it to permission-plus-trigger enforcement, the same class as the 20 existing immutability triggers. Strengthening, not regression. |
| **Corrected claim** | `INV_006_ENGINE_ENFORCED: PARTIAL` — A declarative, B procedural-in-engine. The blanket "engine-enforced" is withdrawn wherever it appeared (§31.2, §31.5, §34, §36). |

### R-9 · Cardinality compilation — **under-specified, now derived per cardinality**

*Source: independent review falsification, not physical evidence. Derivation in §31.7.*

| | |
|---|---|
| **Old decision** | Part I §7.1 and Part II R-5: declared cardinality "compiles into the uniqueness constraint over the endpoint tuple". |
| **The falsification** | `UNIQUE(source, target)` prevents duplicate **pairs** and nothing else. It is the correct rule for `MANY_TO_MANY` and is **insufficient for the other three**: it admits a second parent for a child under `ONE_TO_MANY`, a second target under `MANY_TO_ONE`, and both under `ONE_TO_ONE`. The draft named one rule for four semantics. |
| **Second problem the correction exposed** | The obvious fix — a per-relationship unique index — is **DDL per relationship**, which would defeat `ORDINARY_NEW_RELATIONSHIP_REQUIRES_PLATFORM_DDL: NO`. |
| **Revised decision** | Three **fixed** indexes on the shared relation value table, two of them filtered on an FK-locked projection of the declared cardinality (§31.7). All four cardinalities become engine-enforced with **zero per-relationship DDL**. |
| **Preserved / strengthened** | Typed endpoints, real FKs and zero-cascade all preserved. `CF-040` closed for all four cardinalities rather than one. The composite FK additionally makes the engine refuse an in-place cardinality change while rows exist. |
| **Falsification test** | Per cardinality, four attempts, §31.7 — each rejected by index, not by code. |

---

## 31. The stable kernel / evolvable logical model — derivation and falsification

The phase instruction states a hypothesis to falsify, not adopt:

> NEW DATASET ≠ NEW PHYSICAL TABLE · NEW LOGICAL TABLE ≠ NEW PHYSICAL TABLE ·
> NEW COLUMN ≠ ALTER TABLE · NEW RELATIONSHIP ≠ NEW PHYSICAL TABLE ·
> NEW CLASSIFIER ≠ NEW PHYSICAL TABLE

### 31.1 The evidence decides it, and it already did

**This platform has already answered the question three times, with three different qualities**,
and the answer must be derived from which one is strongest — not from preference:

| Existing mechanism | Dynamic without DDL? | Typed? | Constrained? | Verdict |
|---|---|---|---|---|
| `statistics.observation` + `observation_dimension` (11 288 / 13 928 rows) | **yes** — a new dataset with new dimensions needs no DDL | dimension values are FK to `classification_item` | grain **not** enforceable (surrogate PK + child rows) | the right shape, missing one guarantee |
| `entity.entity_record.payload_json` | yes | **no** | **no** | `CF-039` — the failure mode |
| `entity.localized_text` / `resource_locator` / `entity_classification` | **yes** — a new localized field needs no DDL | **yes** — one value domain per table | **yes** — PK includes the component and the discriminator | **the strongest pattern in the repository** |

**The third pattern is not EAV, and the distinction is precise:** EAV is *one* table with
`(entity, attribute, value)` where `value` is a string or variant for every type and `attribute`
is free text. Here each table holds **one value domain with its correct physical type**, the
component identifier is **contract-governed** (it must resolve to a declared component of the
structure revision), and the primary key **includes the component and its discriminator**, so
uniqueness is engine-enforced per field.

### 31.2 The revised physical architecture

```text
PLATFORM PHYSICAL KERNEL          (stable; changes only when the PLATFORM gains a capability)
  declaration:  namespace · structure · component · grain · policy · codelist · contract
  record:       record header (structure revision, business key digest, assertion time)
  values:       one typed table per value domain —
                  text · localized text · decimal · integer · boolean · temporal ·
                  reference (FK to codelist item) · relation endpoint · locator · binary ref
  provenance:   activity · derivation · source record
  publication:  snapshot · membership · classifier item snapshot
  archive:      record · payload pointer · retention
        ↓ compiled from contracts
LOGICAL MODEL                     (evolves by declaration: databases, schemas, structures,
                                   fields, relationships, classifiers, datasets)
        ↓ optional, declared, justified
PHYSICAL SPECIALIZATION           (a materialized per-structure table, when §31.4 criteria hold)
```

**Where each guarantee is enforced — the question the instruction asks directly:**

| Guarantee | Enforced | How |
|---|---|---|
| Identity / addressability | **engine, once in the kernel** | record header PK |
| **Declared grain uniqueness — no two identical grain tuples** (invariant A) | **engine, declarative** | `UNIQUE(structure_revision_id, business_key_digest)` |
| **Digest ↔ actual persisted values correspondence** (invariant B) | **engine, procedural — not declarative** | the digest is **generated, never accepted**: no writer holds the column, it is derived in-transaction from the persisted grain rows, and a verification trigger rejects any mismatch. **Not** `Q39` — see §31.6 and `R-8`. This is the honest boundary that makes `INV-006` `PARTIAL`, not `YES` |
| Referential integrity of coded values | **engine, once in the kernel** | the reference value table has a real FK to `classification_item`; every logical reference field inherits it |
| Relationship cardinality, **all four** | **engine, once in the kernel** | **three fixed indexes**, two filtered on an FK-locked projection of the declared cardinality — not one endpoint-tuple index, which covers only `M:N`. Derived per cardinality in §31.7 (`R-9`) |
| Value typing and precision | **engine, per value domain** | the decimal table is `DECIMAL`, the temporal table is `DATETIME2`; a logical field cannot be stored in the wrong domain |
| Platform state vocabularies | **engine** | the 39 existing CHECKs, untouched (R-4) |
| Required / optional | **compiled validation** at write time | a NOT NULL per logical field is not expressible in a shared table — **stated as a real limitation**, and mitigated by validation plus the fact that a missing required component is a rejected record, not a silent null |
| Scalar range / pattern domains | **compiled validation**, or **engine if specialized** | the honest boundary — see §31.3 |
| Immutability of published / approved rows | **engine** | the 20 triggers, preserved |

### 31.3 What this genuinely costs — stated, not hidden

Two guarantees are **weaker** in the generic kernel than in a per-structure table, and the
constitutional rule forbids pretending otherwise:

1. **Per-field `NOT NULL`** becomes a validation rule rather than a column constraint.
2. **Per-field `CHECK` ranges and patterns** become compiled validation rather than engine
   constraints.

**Why this is not a regression against the baseline:** all 115 existing CHECK constraints and
every existing NOT NULL sit on **platform kernel tables**, which remain physical tables with
their constraints intact (R-4). **No user-dataset field today has a physical range CHECK,
because no user dataset has its own table** — `observation` and `entity_record` are already
generic. The comparison is therefore *generic-and-typed* versus *generic-and-untyped*, and the
revised design wins it decisively. **Against a hypothetical per-structure-table design** the
weakening is real, which is exactly why §31.4 exists.

**These two remain exactly two.** A third limitation exists, of a different kind — it concerns
the *tuple-to-digest correspondence* rather than per-field value constraints — and is derived,
bounded and given its residuals in §31.6. It is listed there rather than here so that the two
value-constraint limitations accepted by review are not silently widened.

### 31.4 The specialization boundary — when a logical structure earns a physical table

A materialization is **declared, governed, reversible and never the default**. Criteria, any one
sufficient:

| # | Criterion | Evidence required |
|---|---|---|
| 1 | Volume: the structure exceeds a declared row threshold | measured row count |
| 2 | A query pattern needs a composite or covering index the kernel cannot express | query plan evidence |
| 3 | A constraint must be engine-enforced for regulatory or safety reasons | the rule, and who requires it |
| 4 | A provider round trip needs a real table of that shape | the provider contract |

**What a materialization does not change:** the contract, the semantics, the grain, the
identity, or where authority lives. It is a `K-10` materialization of the same logical
structure, and dropping it changes performance, never meaning. **That reversibility is the test
that separates specialization from schema proliferation.**

### 31.5 Falsification of the hypothesis

| Attack | Result |
|---|---|
| *Does this collapse into EAV?* | **No** — §31.1's three distinctions: typed per domain, contract-governed component, component in the primary key. A design that loses any one of the three **is** EAV and is rejected |
| *Does it become stringly typed?* | **No** — a logical field declares a representation; the compiler routes it to the matching value table; there is no string fallback |
| *Is it a universal edge table?* | **No** — relation endpoints live in a typed relation value table whose uniqueness is the declared cardinality; there is no untyped `(from, to, type)` triple |
| *Is it metadata magic?* | **No** — every derivation lands in an inspectable compiled artifact (`MSA-R8`): a constraint, a plan, a generated schema |
| *Can grain really be enforced?* | **Partly, and the halves must be named separately** (`R-8`, §31.6). *No two identical grain tuples* — **yes, declaratively**, `UNIQUE(structure_revision, business_key_digest)`. *The digest is the digest of the actual values* — **not declaratively provable in any SQL Server construct**; enforced by generation, permission and trigger. Still **stronger than today**, where `INV-006` is application-only, but the earlier "by construction" claim was false |
| *Does it destroy query performance?* | **Not proven either way.** A wide read touches one value table per domain — a bounded join count, not per-column. Where that is insufficient, §31.4 materializes. **No numbers are claimed; `BM-Q-03` is unresolved** |
| *Would we choose it without legacy?* | **Yes** — the strongest existing pattern (`localized_text`) was chosen without legacy pressure, and the design generalizes it |

**`STABLE_PHYSICAL_KERNEL_HYPOTHESIS: PASS`**, with two named limitations (§31.3), the grain
correspondence boundary (§31.6) and a declared specialization boundary (§31.4).

### 31.6 Grain digest correspondence — the enforcement boundary, derived

**The authority statement first, because everything below depends on it:**

> **The semantic grain is the ordered tuple of declared grain components in the contract. It is
> the canonical authority. The `business_key_digest` is a physical enforcement and indexing
> representation of that tuple and nothing else.**
>
> The digest is never published, never exchanged, never a business identifier, never an API key,
> never compared across systems, and carries no meaning the tuple does not already carry.
> **The test that proves it is not semantic authority:** changing the encoding is a physical
> migration that rebuilds every digest and changes no meaning. If that were not true, the digest
> would be semantic, and the design would be wrong.

**Two invariants, and only one of them was proven.**

| | Invariant | Proven by |
|---|---|---|
| **A** | two identical canonical grain tuples cannot coexist in one structure revision | `UNIQUE(structure_revision_id, business_key_digest)` — **declarative, engine** |
| **B** | `business_key_digest` equals the digest of the **actual persisted values** of **exactly the declared grain components** of that record | **nothing, as drafted** |

A unique index over a digest column is indifferent to what the digest means. Without B, a writer
persisting values `X` with `digest(Y)` produces a row that is unique, well-typed, FK-valid — and
a duplicate of an existing record in every sense a user cares about.

**Why B cannot be declarative — engine facts, not preferences (SQL Server 2022):**

| Declarative device | Why it cannot bind B |
|---|---|
| `CHECK` constraint | cannot reference another table; no subqueries |
| persisted computed column | must be a deterministic function of **its own row**; the grain values are rows in other tables |
| `FOREIGN KEY` | relates rows, cannot compute a hash |
| indexed view | cannot assemble a multi-row tuple into one hashable row: no `STRING_AGG`, no `HASHBYTES` over an aggregate, no subqueries, aggregates limited to `COUNT_BIG`/`SUM` |
| `UNIQUE` over the values themselves | requires the tuple in one row — i.e. grain columns on the record — which **is a per-structure table** |

**This is the case the review anticipated: full declarative enforcement of B is impossible
without defeating the stable-kernel requirement.** Stated plainly, not engineered around.

**The strongest non-regressive mechanism — three layers, and the first is the one that matters:**

| Layer | Mechanism | Class |
|---|---|---|
| **L1 · generated, never accepted** | no application role holds `INSERT`/`UPDATE` on `platform.record`; the only write path is `platform.usp_record_write`, which **has no digest parameter**. A false digest has no entry point to the system | **engine authorization** |
| **L2 · derived in-transaction from persisted rows** | the procedure's final statement, and an `AFTER INSERT, UPDATE, DELETE` trigger on each value-domain table, recompute the digest by reading the **actual persisted** grain rows of the affected records. There is no statement boundary at which `X` and `digest(Y)` coexist | **engine, procedural** |
| **L3 · verification trigger on the record table** | `tr_record_grain_digest_verify` (`AFTER INSERT, UPDATE`) recomputes and `THROW`s on mismatch, so even a principal holding direct table rights is rejected | **engine, procedural** |
| **L4 · reconciliation** | an `ops` check recomputes every digest and reports drift — the detective control for a disabled trigger | **detective, out of band** |

**The encoding, and the `Q39` correction.** `Q39` canonicalizes a **contract JSON document** with
RFC 8785 JCS and NFC on the JVM. A trigger cannot recompute it — JCS and NFC are not T-SQL
operations. Reusing `Q39` for the grain key would have forced one of two failures: **a second JCS
implementation in T-SQL**, which is a parallel canonicalizer and forbidden by the one-authority
rule, or **the loss of L2 and L3**, collapsing B to application-only. Therefore:

> **The grain key is not `Q39` and must not be.** It is an engine-local physical encoding:
> for each declared grain component in declared ordinal order,
> `ordinal ‖ domain_tag ‖ len32 ‖ canonical_bytes(value)`, absent marked by a reserved tag,
> concatenated as `VARBINARY(MAX)`, then `HASHBYTES('SHA2_256', …) → BINARY(32)`.
> Length-prefixing makes the encoding **injective**, so distinct tuples cannot collide by
> construction rather than by probability.

`Q39` keeps what it owns — `semanticDigest` and `revisionDigest` over contract documents. The two
rules have different inputs, different owners and different purposes, and neither is now a second
implementation of the other. **This corrects the `Q39` attribution in §31.2 as originally written.**

**Falsification — the review's exact test.** *Persist correct typed component rows with a
deliberately false but unique digest:*

| # | Attempt | Rejected where | Atomicity |
|---|---|---|---|
| 1 | `INSERT`/`UPDATE platform.record` directly with `digest(Y)` | **permission check, before any row is touched** — no role holds the right (error 229) | nothing executes |
| 2 | call `usp_record_write` passing a digest | **no such parameter** — the digest is computed inside from the rows just written | no entry point |
| 3 | a DDL-privileged principal sets `record.business_key_digest = digest(Y)` while values are `X` | **`tr_record_grain_digest_verify`**, same transaction, `THROW 51006` | `SET XACT_ABORT ON` → full transaction rollback |
| 4 | change a grain **value** row and leave the header stale | **the value-table trigger re-derives** the header digest in the same transaction; on a published record the existing immutability triggers reject the change first | same transaction |
| 5 | disable the trigger, then do #3 | **not preventable in-database** — see the residual below | detected out of band |

**Residuals, named rather than absorbed:**

1. **DDL-privileged bypass.** A principal who can `DISABLE TRIGGER` defeats L2/L3. This is the
   enforcement class of **all 20 existing immutability triggers** (guarantee #6), so it is not a
   regression — it is the platform's existing ceiling. Controls: DDL audit + L4 reconciliation.
2. **Unicode normalization of text grain components.** Byte-exact hashing means NFC and NFD forms
   of the same text yield different digests and **both would be admitted as distinct grain**.
   T-SQL has no normalization function. Mitigation: NFC is a **storage invariant of the text value
   domain**, applied on the sealed write path; L4 cannot detect it, so a dedicated scan is needed.
   **Owner decision required (§39).**
3. **A record written before its grain rows exist.** Closed by ordering inside the sealed write
   path — values first, digest last, one transaction — and by `business_key_digest NOT NULL`.

**Verdict:** `INV-006` is **`PARTIAL`** — A declarative, B procedural-in-engine. Against the
baseline, where `INV-006` is application-only, this is a strengthening. The unqualified
"engine-enforced" claim is withdrawn.

### 31.7 Cardinality — the exact physical rule for each of the four

The relation value table holds typed endpoints:
`relation_value(relationship_revision_id, source_record_id, target_record_id, …)`, both endpoints
real FKs to `platform.record`, all `NO_ACTION` (zero cascade preserved).

**What the draft claimed, and why it was wrong.** `UNIQUE(relationship_revision_id,
source_record_id, target_record_id)` prevents duplicate **pairs**. That is the whole rule for
`MANY_TO_MANY` and **none of the rule** for the other three.

**Derivation.** For a relationship over ordered roles `(a, b)`, exactly two independent questions
decide the physical rule:

- *may a given `b` have more than one `a`?* — if not, **`b` must be unique**
- *may a given `a` have more than one `b`?* — if not, **`a` must be unique**

| Cardinality | max `b` per `a` | max `a` per `b` | Required uniqueness |
|---|---|---|---|
| `ONE_TO_ONE` | 1 | 1 | **both** endpoint roles unique |
| `ONE_TO_MANY` (one `a`, many `b`) | N | **1** | **target** unique — a child cannot acquire a second parent |
| `MANY_TO_ONE` (many `a`, one `b`) | **1** | N | **source** unique — the inverse |
| `MANY_TO_MANY` | N | N | neither role; **pair** unique only |

**The physical problem, and why the obvious fix is rejected.** An index is table-wide, so
`UNIQUE(relationship_revision_id, source_record_id)` applied to the shared table would force every
relationship to be `N:1`. A per-relationship filtered index would be correct but is **DDL per
relationship**, defeating `ORDINARY_NEW_RELATIONSHIP_REQUIRES_PLATFORM_DDL: NO`. Both rejected.

**The derived mechanism — three fixed indexes, no per-relationship DDL:**

```sql
-- declaration side: cardinality_code is the semantic authority (closed vocabulary, CHECK - R-4)
-- the two flags are its PERSISTED, deterministic physical projection
platform.relationship_revision(
  relationship_revision_id  PK,
  cardinality_code          CHECK (cardinality_code IN
                              ('ONE_TO_ONE','ONE_TO_MANY','MANY_TO_ONE','MANY_TO_MANY')),
  enforce_source_unique AS CONVERT(BIT, CASE WHEN cardinality_code
                              IN ('ONE_TO_ONE','MANY_TO_ONE') THEN 1 ELSE 0 END) PERSISTED,
  enforce_target_unique AS CONVERT(BIT, CASE WHEN cardinality_code
                              IN ('ONE_TO_ONE','ONE_TO_MANY') THEN 1 ELSE 0 END) PERSISTED,
  UNIQUE (relationship_revision_id, enforce_source_unique, enforce_target_unique))

-- instance side: the flags are FK-locked to the declaration, so they cannot disagree with it
platform.relation_value(
  …,
  FOREIGN KEY (relationship_revision_id, enforce_source_unique, enforce_target_unique)
    REFERENCES platform.relationship_revision
               (relationship_revision_id, enforce_source_unique, enforce_target_unique))

CREATE UNIQUE INDEX ux_rel_pair        ON platform.relation_value
  (relationship_revision_id, source_record_id, target_record_id);
CREATE UNIQUE INDEX ux_rel_source_one  ON platform.relation_value
  (relationship_revision_id, source_record_id) WHERE enforce_source_unique = 1;
CREATE UNIQUE INDEX ux_rel_target_one  ON platform.relation_value
  (relationship_revision_id, target_record_id) WHERE enforce_target_unique = 1;
```

**Every device here is already proven in this database**, which is why it is a generalization and
not an invention: the live catalogue carries filtered indexes whose predicates are exactly
`col = 'literal'` (`ux_statistical_contract_draft_approved`) and `col IS NOT NULL`
(`ux_api_operation_owner_idempotency`), and `WHERE flag = 1` is the same single-equality form — no
`OR`, no computed column in the predicate, both of which SQL Server forbids there.

**Per-cardinality proof and falsification:**

| Cardinality | Active indexes | Attack | Result |
|---|---|---|---|
| `ONE_TO_ONE` | pair + source + target | give `a₁` a second target; give `b₁` a second source | **both rejected** — `ux_rel_source_one`, `ux_rel_target_one` |
| `ONE_TO_MANY` | pair + target | give child `b₁` a second parent | **rejected** — `ux_rel_target_one`. A second child for `a₁` is **accepted**, correctly |
| `MANY_TO_ONE` | pair + source | give `a₁` a second target | **rejected** — `ux_rel_source_one`. A second source for `b₁` is **accepted**, correctly |
| `MANY_TO_MANY` | pair | insert the same `(a₁, b₁)` twice | **rejected** — `ux_rel_pair`. Multiple counterparts on both sides **accepted**, correctly |

**Three properties this buys beyond the requirement:**

1. **The engine refuses an in-place cardinality change while rows exist.** Editing
   `cardinality_code` on a revision recomputes the persisted flags, which breaks the composite FK
   from every existing child row — so SQL Server rejects the `UPDATE`. Cardinality change becomes
   a new revision and a migration, which is the contract semantics anyway (`§25`).
2. **The flags can never disagree with the declaration**, because they are not copied — they are
   FK-locked to a computed projection of the single authority, `cardinality_code`.
3. **Endpoint structure conformance uses the identical device**: carry
   `target_structure_revision_id` on the child, FK it both to `record(record_id,
   structure_revision_id)` and to the relationship declaration, and an endpoint of the wrong
   structure becomes impossible rather than merely validated.

**Verdict:** all four cardinalities **engine-enforced, declaratively**, with typed endpoints, real
FKs, zero cascade and contract authority intact, and **zero DDL per new relationship**. `CF-040`
closes for all four rather than for `M:N` alone.

---

## 32. Simple evolution test — exactly what changes physically

**Add a logical structure**: several typed fields, required and optional, a classifier field,
a uniqueness rule, a 1:N relationship, an M:N relationship, a localized value, a resource
attachment.

| Step | What happens physically |
|---|---|
| create logical namespace / schema | **rows** in the namespace declaration |
| create logical structure with 8 fields | **rows**: 1 structure + 8 components + grain component references |
| required / optional | a component flag; enforced by compiled validation |
| classifier field | a component whose representation references a codelist version; values land in the reference value table **with a real FK** |
| uniqueness rule | the declared grain compiles to the existing `UNIQUE(structure_revision, business_key_digest)` — **no new index object per structure** |
| 1:N relationship | a relation structure + endpoint components; cardinality compiles to uniqueness over the child endpoint |
| M:N relationship | the same mechanism with no uniqueness on either endpoint alone and uniqueness on the pair — **declarable, which it is not today** (`CAP-M10`) |
| localized value | a component with a text representation and a language discriminator; rows in the localized text value table |
| resource attachment | a component with a locator representation; rows in the locator value table, FK to the artifact registry |
| **physical DDL** | **none** |
| **new JPA entity** | **none** |
| **new repository / DTO** | **none** — the query compiler consumes the contract |
| **platform migration** | **none** |

**`LOGICAL_EVOLUTION_WITHOUT_DEFAULT_PLATFORM_DDL: PASS`.**

**When DDL *is* required, and it must stay exceptional:** a new **value domain** (say, native
geometry) is a platform capability, so it is a kernel table and a migration. A declared
**materialization** (§31.4) is DDL by design, generated from the contract, and reversible.
Those two cases, and nothing else.

---

## 33. Complex statistical test

Add a statistical dataset with declared grain, multiple dimensions, a measure, status,
attributes, a classifier hierarchy, temporal revision, correction, publication, lineage and a
confidentiality policy.

| Requirement | Mechanism | Typed? | Deterministic? | Constrained? |
|---|---|---|---|---|
| declared grain | components + `business_key_digest` unique | yes | yes — injective length-prefixed tuple encoding + `HASHBYTES` SHA-256, **engine-local, not `Q39`** (§31.6) | **engine declarative** for tuple uniqueness; **engine procedural** for digest correspondence (`R-8`) |
| multiple dimensions | reference components → reference value table | yes | yes | **engine FK to codelist version** |
| measure | decimal component → decimal value table, `DECIMAL` with declared precision | yes | yes | engine type |
| status, attributes | reference / text components, attachment level declared (`CAP-021`) | yes | yes | engine FK |
| classifier hierarchy | `FAM-04` parent component + validity | yes | yes | engine FK |
| temporal revision | assertion time (R-1) | yes | yes | engine |
| correction / reprocessing | new record, new assertion interval, derivation records the rule revision | yes | yes — replayable | engine + append-only trigger |
| publication snapshot | membership + `classification_item_snapshot` pins the codelist | yes | yes | **engine** |
| lineage | derivation edges carrying digests | yes | yes | engine FK |
| confidentiality | policy reference; evaluated at the publication gate over the published closure | — | yes | gate |

**Semantics remain typed, deterministic, reproducible and constrained.** The one element that is
*not* engine-enforced is the disclosure rule itself, which is a policy the steward owns
(`CAP-M12`, `BM-INV-25`).

---

## 34. Integrity test — where each invalid state is rejected

| Attempt | Rejected by | Layer |
|---|---|---|
| orphan relationship endpoint | FK on the relation value table | **engine** |
| duplicate logical business key | `UNIQUE(structure_revision, business_key_digest)` | **engine, declarative** |
| **a digest that does not match the record's actual grain values** | the digest has no writer (permission), is derived from the persisted rows, and `tr_record_grain_digest_verify` re-checks it | **engine, procedural — §31.6, `R-8`** |
| violating declared cardinality, any of the four | `ux_rel_pair` + the two FK-locked filtered indexes | **engine, declarative — §31.7, `R-9`** |
| invalid classifier member | FK to `classification_item` at the pinned version | **engine** |
| invalid decimal precision | the decimal value table's declared type + the provider capability check at authoring | **engine + compile** |
| duplicate current observation | the same unique grain digest, scoped to current assertions | **engine** |
| mutating an approved or published row | the existing immutability triggers (guarantee #6) | **engine** |
| deleting referenced governed data | FK with `NO_ACTION` — zero cascades (guarantee #2, #19) | **engine** |
| ambiguous contract revision | `UNIQUE(namespace, code, revision)` + exact-revision references (guarantees #14, #15) | **engine** |
| cross-dataset inconsistency | the constraint contract at the publication gate | **gate** |
| **missing required field** | compiled validation at write | **compile — the honest weak point (§31.3)** |
| **out-of-range scalar** | compiled validation, or engine if materialized | **compile / engine** |

**Thirteen rows: ten engine — eight declaratively, two procedurally** (the grain digest
correspondence `R-8`, and the existing immutability triggers). **One is a publication gate**
(cross-dataset consistency, which is not a row-level property and cannot be). **Two remain
compile-time**: the §31.3 pair, named with their mitigation. The earlier "nine of eleven" both
counted a smaller table and asserted the digest row it did not contain.

---

## 35. Evolution lifecycle — what each step costs

| Step | Declaration | Validation | Compiled artifacts | Data migration | Materialization | **Platform DDL** |
|---|---|---|---|---|---|---|
| create logical structure | ✔ | ✔ | ✔ | — | — | **no** |
| add field | ✔ | ✔ | ✔ | — | — | **no** |
| change a field's contract (widen) | ✔ new revision | ✔ | ✔ | — | — | **no** |
| change a field's contract (narrow / retype) | ✔ new major revision | ✔ | ✔ | **✔** | — | **no** |
| add relationship | ✔ | ✔ | ✔ | — | — | **no** |
| change classifier version | ✔ | ✔ + crosswalk (`Q26`) | ✔ | only if `1:n` | — | **no** |
| publish | — | ✔ gates | — | — | — | **no** |
| revise | ✔ | ✔ | ✔ | maybe | — | **no** |
| retire | ✔ | ✔ consumer impact | — | — | drop materialization | **no** |
| **declare a materialization** | ✔ | ✔ | ✔ generated DDL | — | ✔ | **yes, generated and reversible** |
| **add a value domain to the kernel** | — | — | — | — | — | **yes — a platform capability change** |

**Physical platform DDL is exceptional and justified, never the ordinary logical lifecycle.**

---

## 36. Coverage

| Dimension | Result |
|---|---|
| **19 baseline guarantees** | **19/19 preserved or strengthened.** Strengthened: #7 (`is_current` derived, not writable), #14 (grain prose → components), #3/#5 (grain uniqueness moves from application-only to **declarative for the tuple invariant and permission-plus-trigger for the correspondence invariant** — `PARTIAL`, `R-8`/§31.6, corrected from the earlier unqualified claim), relation cardinality (`CF-040` closed **for all four cardinalities**, `R-9`/§31.7) |
| **6 draft/baseline contradictions** | **6/6 resolved** — R-1…R-6 |
| **2 review falsifications** | **2/2 resolved** — `R-8` grain digest correspondence (claim corrected and mechanism derived), `R-9` cardinality (rule derived per cardinality) |
| **Recovery-evidence correction** | routed to `REC-CONSOLIDATION` (R-7) |
| **Capabilities** | **48/48** placed; the 16 `REQUIRED-BUT-MISSING` are designed: `M01/M02` §16+`Q48` · `M03` §8 · `M04` §31.2 · `M05` §15 · `M06` §8 · `M07` §29 · `M08` §9 · `M09` §11 · `M10` §31.5/R-5 · `M11` §31.3 (bounded) · `M12` §21 · `M13` R-1 · `M14` §23 · `M15` §24 · `M16` §7 |
| **`Q01…Q50`** | **50/50 by adoption**, 16 verified individually (`Q26` crosswalk → evolution). **`Q39` is no longer claimed as the grain-key rule** — it keeps its own scope, contract-document digests, and §31.6 owns the engine-local grain encoding (`R-8`) |
| **`D1…D7`** | **7/7** |
| **Invariants** | `INV-001…014` + `BM-INV-01…25` = **39/39 addressed by design; 0 implementation-proven**, which is the honest state at design time |
| **Alternatives** | 5 in §2, **plus the sixth forced by the evolution requirement**: per-structure physical tables — rejected in §31.3/§31.4 as the default, retained as declared specialization |

---

## 37. Final architectural test

| Question | Answer |
|---|---|
| Stronger than every inspected artifact, preserving every justified capability? | **yes** — 48/48 capabilities, 19/19 guarantees, and it passes the invariant gate none of the artifacts passes |
| Simple case simple? | **yes** — §32: twelve declarations, zero DDL, zero classes |
| Complex cases expressible? | **yes** — §33 |
| Trace meaning, source, derivation, materialization, consumption without generic-graph/EAV collapse? | **yes** — §31.5 falsifies each collapse individually |
| One semantics driving contracts, validation, providers, projections without parallel authority? | **yes** — with one named legacy exception (R-6) |
| Future provider addable without redesign? | **by design yes; unevidenced** — one provider exists |
| Automation from authority, not magic? | **yes** — `MSA-R8`; every derivation lands in an inspectable artifact |
| Did we discover problems nobody handed us? | **yes** — the typed-narrow-table pattern as the answer to dynamic evolution; that all 115 CHECKs sit on kernel tables, which is what makes the generic kernel non-regressive; that a digest under a unique index proves tuple uniqueness but **not** tuple correspondence (`R-8`); and that one endpoint-tuple index enforces one of four cardinalities, not four (`R-9`) |

**Three answers are qualified and stay qualified:** the second-provider property is designed for
and unevidenced; per-field `NOT NULL`/range constraints are compile-enforced in the generic
kernel (§31.3) unless a structure is materialized; and **grain correspondence is engine-enforced
procedurally, not declaratively** (§31.6), which is a strengthening over today's
application-only `INV-006` but is **not** the "by construction" guarantee this section claimed
before review.

---

## 38. Prompt conflicts encountered and rejected

**One.** The instruction to make ordinary logical evolution require no platform DDL, taken
naively, points at EAV or JSON — which the same instruction forbids, and which `BM-INV-05`,
`BM-REJ-07` and `CF-039` forbid independently. **The conflict was not obeyed and not refused:**
it was resolved by deriving the typed-value-domain kernel from the strongest pattern the
repository already contains (`entity.localized_text`), which satisfies both halves. The two
guarantees that genuinely weaken are named in §31.3 rather than hidden, and bounded by §31.4.

No instruction in this phase authorized a regression, and none was accepted.

---

## 39. Owner decisions still required

Unchanged from Part I §38 — disclosure-control policy · erasability classes and retention ·
`BM-Q-03` performance objectives · `BM-Q-05` ASVS subset · reference-data agency assignment.
**Plus one raised by the revision:** the **row-count threshold and query-plan criteria** that
trigger a declared materialization (§31.4) — an operations decision, not an architectural one.

**Plus one raised by the review correction (§31.6 residual 2):** **how Unicode NFC normalization
of text grain components is to be enforced.** T-SQL cannot normalize, so the choice is between
write-path-only normalization with a detection scan, a SQLCLR normalizer, or restricting text
grain components to a normalization-safe character class. This is a **security- and
integrity-relevant decision** — unnormalized text admits two records the platform considers
distinct and a user considers identical — and an agent must not settle it alone.

---

## 40. Downstream obligations

`PHASE-005` plan · `PHASE-006` execution manifest · **Access provider / authoring / round-trip
refinement** (still deliberately not performed) · SQL↔Access correspondence table · protection
before migration (`PHASE-009`) · provider-capability producer · second-provider proof ·
hierarchy first exercise · `DEF-06` clean-build equivalence · `REC-CONSOLIDATION` correction
(R-7) · the 19 `dbo.*` legacy elimination inventory with its ORM entity pairs (`PHASE-011`).

**Added by the review correction:** the **write-path permission model** that makes L1 of §31.6
real (`DENY` on `platform.record`, `EXECUTE`-only on the sealed procedure, ownership chaining) —
without it the grain correspondence degrades to L3 alone · the **digest reconciliation check**
(§31.6 L4) as an `ops` validator · the **per-cardinality constraint tests** of §31.7 and the
**five falsification attempts** of §31.6 as executable negative tests, since both are currently
derived-and-argued, **not executed**.
