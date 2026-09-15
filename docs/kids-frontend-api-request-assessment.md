# KIDS frontend API request — managerial and logistical assessment

**Assessment date:** 2026-09-15  
**System:** `platform/apps/geostat/frontend/kids`  
**Decision status:** assessment complete; implementation requires phased approval

## Governing principle

KIDS must conform to the platform; the platform must not be degraded to match
KIDS. The current KIDS API shape, hardcoded routes, client-side parsing and
legacy URLs are observations to migrate, not design constraints. All changes
must retain the platform's strongest guarantees: contract-first execution,
metadata-driven behavior, provider/site/schema agnosticism, deny-by-default
security, versioning, lineage, observability and test/evidence gates. A
conflict is resolved by changing the KIDS adapter or contract mapping, never by
adding a KIDS-specific shortcut to the generic engine.

## Executive assessment

The site has a small HTTP surface (three dynamic requests), but its effective
data surface is larger because response parsing, file URL construction, chart
decoding, static publication links and eleven statistics routes are embedded in
components. The current system is operationally a **legacy API consumer**, not
yet a canonical contract consumer. Replacing the base URL alone would break
the site: the platform endpoints are under `/api/v1`, require authorization,
and return contract-shaped envelopes rather than the legacy arrays.

## Request register

| ID | Current request | Input | Current response assumption | Consumers | Risk | Canonical destination |
|---|---|---|---|---|---|---|
| RQ-01 | `GET /api/goals` | none | array with `ID`, `category`, localized titles and paths | `Goals.jsx` | medium: grouping and downloads depend on legacy names | page 8 / `KIDS_GOAL` |
| RQ-02 | `GET /api/files?category={coverPdfNumber}` | numeric category | array with `ID`, `sub_category`, localized titles/paths, `chartdata` | `SectionDataPage.jsx` | high: JSON-in-string parsing and path synthesis | page 9 resources + page 11 statistics |
| RQ-03 | `GET /api/glossary?lang={ka\|en}` | language query | array with `text` and implicit ordering | `GlossaryModal.jsx` | low/medium: localization and search are client assumptions | page 10 / `KIDS_GLOSSARY_ENTRY` |

### RQ-01 logistics

The UI groups rows by a numeric category and builds download URLs from relative
paths. The canonical adapter must provide a stable goal identity, localized
labels, category relation and governed resource reference. Category numbers
must be contract data; the frontend must not infer the existence of 17 groups.

### RQ-02 logistics

This is the largest migration item. One legacy request mixes resource metadata,
source-category filters, file paths and chart payloads. The canonical model
separates resources from statistical observations and lineage. A migration
mapper is required; chart JSON must be represented as observations/dimensions,
not reparsed from an arbitrary string in the browser. Relative URL generation
must move to a governed resource/payload pointer.

### RQ-03 logistics

The current client sorts locally and searches `text`. The canonical response
must declare language, label fields, sort capability and pagination. The UI
should consume the declared localized field rather than assuming `text`.

## Non-HTTP data dependencies

- Static publication PDFs and legislation links in `Main.jsx` are not API
  requests. Each must be classified as governed `KIDS_RESOURCE` metadata or
  explicitly retained as static presentation content.
- Pyramid and external institutional links are cross-system dependencies;
  they require link ownership, availability monitoring and a deprecation owner.
- Local images, fonts and cover PDFs are build artifacts and should remain
  inside the frontend package unless publication governance requires archive
  storage.

## Management ownership

| Work item | Responsible owner | Approval/evidence |
|---|---|---|
| contract/page/field mapping | data steward | approved R8 mapping revision |
| response adapter and client | frontend/backend engineering | unit + fixture parity tests |
| OIDC/CORS/browser policy | security/platform owner | negative auth and CORS evidence |
| resource URL and archive policy | content/data owner | checksum/provenance register |
| statistics semantic mapping | statistical steward | metric/unit/aggregation approval |
| migration window and fallback | release owner | dated deprecation and rollback plan |
| production switch | deploy authority | signed release and acceptance bundle |

## Recommended execution order

1. Freeze request/response fixtures and baseline current site behavior.
2. Approve the RQ-01/RQ-02/RQ-03 field and relation mapping.
3. Implement one canonical client and normalized view-model boundary.
4. Implement goals and glossary adapters first; they have the lowest semantic
   risk and validate authentication/discovery mechanics.
5. Implement resources, then statistics; separate chart observations from file
   resources and validate counts/checksums.
6. Run legacy/canonical shadow comparison and resolve mismatches by data
   decisions, not UI patches.
7. Enable canonical mode in staging, then production; keep fallback only during
   the approved window.
8. Remove fallback after consumer-impact review and rollback evidence.

## Go/no-go criteria

**Go:** every request has a declared contract mapping, fixture parity passes,
OIDC/CORS policy is approved, resource pointers resolve, statistics semantics
are steward-approved, and rollback is tested.

**No-go:** guessed API origin, anonymous access where policy is undefined,
client-side reconstruction of governed URLs, chart payload duplication,
hardcoded page/category semantics, missing lineage, or an unbounded legacy
fallback.

## Current decision

The environment registration is complete, but implementation should begin at
Phase A of the companion plan. No frontend endpoint is switched yet because
the canonical API currently returns `401` without an approved browser auth
policy and the production API origin is not declared for the KIDS site.
