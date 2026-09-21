---
id: ADR-015
type: DECISION
title: SDMX conformance boundary
status: ACTIVE
authority: CANONICAL
scope: which platform responsibilities SDMX governs, and which it must not shape
created: 2026-09-21
updated: 2026-09-21
related: STD-BENCH-001, AUD-COMPARATIVE, ADR-016
---

# ADR-015 — SDMX conformance boundary

**Status:** ACCEPTED 2026-09-21. Resolves `BM-Q-01`. **Owns the boundary only** — see *What this
decision does not own*, which is the load-bearing part.

## Context, and a correction to the question

`BM-Q-01` was posed as *"what SDMX conformance level is required?"*. Two findings reshape it.

**First: the repository has already decided the conformance claim.** `Q47` in the statistical
decision register (`docs/work/STATISTICAL-CONTRACT-OPEN-QUESTIONS.md`) is `DECIDED`:

> conformance claim — **only proven**. v1: Information Model profile **3.1 (subset, declared)**;
> export `SDMX-CSV 2.0`, `SDMX-JSON 2.0`. Existing facade (F12) — labelled `compatibility`, **no
> conformance claim**, until validated. Multi-measure → 2.1 — **rejected**
> (`UNSUPPORTED_CONVERSION`), no silent pivot. Evidence: conformance matrix; official schema
> validation; rejection test.

That is already operationally defined, already version-pinned, and already bans the vague term
this decision was warned against. **Writing a second conformance claim here would create exactly
the duplicate authority this programme exists to eliminate.** It is adopted by reference.

**Second: SDMX does not publish ranked conformance levels.** The SDMX standards package is
sectioned — Section 1 Framework, Section 2 Information Model, Section 5 Registry Specification,
Section 6 Technical Notes, with the REST API, SDMX-ML, SDMX-JSON and SDMX-CSV format
specifications maintained separately; the current version is **3.1 (May 2025)**, which is what
`Q47`/`S1` already pin. Conformance is declared by an implementation, per artefact class and
interface, in an implementation conformance statement — not selected from a ladder. **So the
question "which level?" has no answer in the standard, and the answerable question is: over
which platform responsibilities does SDMX have any say at all.** That question is genuinely
unowned, and it is what this ADR decides. *(Confidence on the framework's conformance mechanism:
MEDIUM-HIGH — taken from the published section structure and framework description; the 3.1
PDF's text layer was not extractable in this environment. Nothing in this decision depends on
the exact wording, only on conformance being declared rather than ranked.)*

## Decision

**SDMX governs the statistical responsibility and nothing else. Its concepts may not be imposed
on any other family, and no other family's needs may weaken the statistical conformance claim.**

### In scope — SDMX is the authority for these responsibilities

| Responsibility | SDMX correspondence required | Tested by |
|---|---|---|
| Structural metadata for a cube-shaped statistical dataset | Information Model 3.1 subset: data structure definition, dimensions, measures, data attributes **with attachment level**, concepts, representations | the `Q47` conformance matrix + official schema validation |
| Controlled vocabularies used as statistical dimensions, measures or attributes | codelists / concept schemes; cross-domain codelists where one exists (`CL_OBS_STATUS`, `CL_CONF_STATUS`, `CL_FREQ`, `CL_UNIT_MULT`, `CL_DECIMALS`) | codelist reference resolves to an exact pinned version |
| Statistical time semantics | the SDMX reporting-period formats already fixed by `Q19` | `Q19`'s format set, with lexical original retained |
| Statistical exchange at the export boundary | `SDMX-CSV 2.0`, `SDMX-JSON 2.0` | round-trip against the canonical values |

### Out of scope — SDMX has no say, and saying so is the point

| Responsibility | Why SDMX must not shape it |
|---|---|
| Entity, relation, resource and geo families | four of six declared families are not cube-shaped. Forcing them into dimensions and observations would destroy their semantics — this is the `BM-EC-009`/`BM-REJ-04` failure mode |
| Product-like and arbitrary relational structures, many-to-many relations | SDMX has no model for them; their model is relational and typed |
| The platform's serving API | the SDMX REST API is **not adopted** as the platform's API contract. `OpenAPI 3.1.1` + `RFC 9457` own that (platform standards profile) |
| The control plane | the SDMX Registry Specification (Section 5) is **not adopted** as the platform's registry. `CAD-02` owns authority; `platform.*` registries own structure |
| Internal canonical persistence | SDMX is an *information model and an exchange standard*, not a storage design. Canonical storage answers to `BM-INV-05/09/24`, not to SDMX |
| Validation and transformation language | VTL is **not adopted**. The platform's rule bindings own validation; adopting VTL would add a second expression authority with no demonstrated need |
| Provider architecture and operational lifecycle | Access, object storage, publication, snapshots and approval are platform concerns with no SDMX counterpart |

### The boundary rule, stated so it can be applied mechanically

> **A responsibility is inside the SDMX boundary if, and only if, its data is an observation set
> keyed by a declared dimension tuple.** Everything else is outside. A capability that is needed
> by a family outside the boundary is designed on its own terms; a capability needed by a family
> inside it is designed to correspond to the SDMX artefact class, and the correspondence is
> declared and tested — never asserted.

**Banned vocabulary:** "SDMX-compatible", "SDMX-like", "SDMX-shaped" as a claim. The existing
export facade (`F12`) is already labelled `compatibility` with no conformance claim; that label
is the only permitted use, and it must name what is *not* claimed.

## What this decision does not own

- **The conformance claim itself** — `Q47` owns it: version, subset, formats, evidence.
- **Version pinning** — `S1`/`Q47` own it. This ADR names 3.1 only to state that it did not
  re-pin it.
- **The canonical model** — `PHASE-004`. Correspondence to an SDMX artefact class is an
  obligation on the design, not the design.

## Consequences

- `PHASE-004` may design a general-purpose platform with a statistical family that has declared,
  tested SDMX correspondence — and is not obliged to make the rest of the platform statistical.
- **Multi-measure becomes a boundary obligation with a known gap.** `Q47` rejects multi-measure
  *conversion to SDMX-ML 2.1* explicitly, and `AUD-COMPARATIVE`/`ACCESS-PACKAGE-CANONICAL-STANDARD`
  §A2 both measure that multi-measure and multi-structure are **modelled but not demonstrated**
  in any artifact (`CAP-M07`). The two records agree; design must treat multi-measure as
  in-boundary and unproven, not as decided.
- **A correspondence worth evaluating, recorded as an input and not as a decision:** the
  platform's metadata-assertion plane (`__gs_metadata`, 4 namespaces with no schema — `CAP-M03`)
  is the same shape as SDMX *reference metadata* (metadata structure definitions). If that
  correspondence holds, `CAP-M03` gains an existing standard instead of a bespoke schema
  language. **Confidence MEDIUM — not verified against the 3.1 text here.** `PHASE-004` evaluates
  it; this ADR does not decide it.

## Falsification attempt

*Could the boundary rule be too narrow — is there a non-cube responsibility where SDMX genuinely
helps?* The strongest candidate is classification: SDMX codelists and concept schemes are a
mature model for vocabularies used **outside** statistics too. The rule as written keeps a
codelist inside the boundary only when it is used as a statistical component, which could split
one vocabulary across two models. *Resolution:* the artifacts already contradict the split —
`__cl_*` is one classifier model serving entity, relation and statistical families alike, with
`standard_reference` naming ISO/IEC 11179 for the general case and SDMX for the statistical one.
The boundary therefore runs through **usage**, not through the vocabulary, and one classifier
model may carry both references. **Confidence: MEDIUM-HIGH**, and `PHASE-004` should re-test it
when it designs the classifier family.
