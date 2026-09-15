# KIDS frontend — environment-first integration plan

**Status:** DRAFT FOR STEP-BY-STEP AGREEMENT  
**Scope:** `platform/apps/geostat/frontend/kids` and its isolated environment
configuration under `ops/config/projects/geostat/services/kids`.  
**Out of scope:** visual redesign, JWT implementation in the API, and any
other project/service.

## Non-negotiable quality doctrine

KIDS is a reference consumer, never the authority that lowers or reshapes the
platform standard. The integration must preserve the platform's highest
contract-first, metadata-driven, schema/provider/site-agnostic guarantees. We
adapt KIDS to the canonical system; we do not add KIDS-specific branches,
weaken security, duplicate physical structures, accept legacy semantics as
truth, or lower test/evidence requirements to make the existing app fit. Any
KIDS mismatch is recorded as a mapping, contract, or migration decision and is
resolved at the platform boundary. If the existing KIDS behavior conflicts
with a platform invariant, the invariant wins and the KIDS consumer is
changed.

No guarantee is negotiable during this migration. Existing platform controls
remain mandatory, and any KIDS mismatch is an opportunity to improve the
adapter, contract metadata or platform capability. This rule applies equally
to every future site and project; KIDS is only the first conformance case.

## 1. What was discovered

The KIDS application is a React 19 + Vite site. Its current runtime contract is
legacy and is configured through `VITE_API_URL=https://youth-api.geostat.ge`.
The application currently issues three HTTP requests:

| Caller | Current request | Purpose | Target canonical page |
|---|---|---|---:|
| `pages/sections/Goals.jsx` | `GET /api/goals` | grouped goals and downloadable resources | 8 |
| `components/SectionDataPage.jsx` | `GET /api/files?category={coverPdfNumber}` | section files and chart payloads | 9 / 11 |
| `components/GlossaryModal.jsx` | `GET /api/glossary?lang={ka\|en}` | glossary search | 10 |

The root and navigation are currently static React routes.  Statistics have
eleven local routes, while the platform contract exposes one statistics page
(page 11); the section discriminator must therefore become a declared contract
filter, not a new API route or hardcoded SQL branch. Publications, legislation,
external links, pyramid links, and local PDF assets are currently static links
and must be classified as either governed resources or intentionally static
presentation assets.

## 2. Target environment contract

The environment profile is the single deployment input:

```text
ops/config/projects/geostat/services/kids/
├── service.manifest.json
├── templates/app.env.example
└── overlays/{local,staging,production}.env
```

The manifest binds `KIDS_PORTAL_V1` revision 8 and page IDs 7–12.  No token,
database credential, signing key, or secret value belongs in the frontend or
this repository.  Production uses OIDC; local development may use an explicit
non-production mode only.

## 3. Phased execution checklist

### Phase A — Freeze and inventory (no behavior change)

- [ ] Record every request, response shape, error state, download URL and
  static data source.
- [ ] Capture legacy API fixtures for goals, files and glossary.
- [ ] Map every visible route/component to a contract page, dataset and field.
- [ ] Identify fields with no approved R8 binding; do not guess mappings.
- [ ] Establish baseline build, lint and browser smoke results.

**Exit evidence:** versioned inventory and fixtures; current site behavior is
reproducible.

### Phase B — Canonical client boundary

- [ ] Add one typed/request-safe platform client; components must not construct
  API URLs directly.
- [ ] Read API base URL, contract code/revision and auth mode only from the
  KIDS environment profile.
- [ ] Add runtime-only bearer-token injection and 401/403 handling.
- [ ] Add request cancellation, timeout, correlation ID and safe error mapping.
- [ ] Keep legacy calls behind an explicit migration adapter and feature flag.

**Exit evidence:** unit tests prove no component contains a legacy URL or SQL;
legacy and canonical adapters have identical domain-level output types.

### Phase C — Discovery before data

- [ ] Fetch contract pages and capabilities before rendering data controls.
- [ ] Use declared fields, filters, relations, includes, limits and response
  shape to configure the page.
- [ ] Hide unsupported controls instead of sending undeclared fields.
- [ ] Bind routes to page IDs through the manifest, not numeric literals spread
  through components.

**Exit evidence:** discovery fixture drives a complete page configuration with
no KIDS-specific execution branch.

### Phase D — Dataset adapters

- [ ] Goals: page 8 response → grouped goal view and governed resource links.
- [ ] Resources: page 9 response → file cards, localization and lineage.
- [ ] Glossary: page 10 response → localized searchable entries.
- [ ] Statistics: page 11 response → dimensions, observations, metric/unit and
  chart series; section category is a contract-declared filter.
- [ ] Classifiers: page 12 response → labels/options used by filters.
- [ ] Preserve lineage, quality and publication metadata where supplied.

**Exit evidence:** fixture replay for every page and language produces stable
view-models without legacy response assumptions.

### Phase E — Authentication and browser policy

- [ ] Bind the approved OIDC issuer/audience and CORS origin.
- [ ] Store access tokens only in the approved runtime mechanism; never in
  committed `.env` files.
- [ ] Verify scopes/roles and tenant/site boundary for every canonical request.
- [ ] Test expired, wrong-audience, missing-scope and cross-tenant responses.

**Exit evidence:** negative authorization suite and browser CORS evidence.

### Phase F — Migration and compatibility

- [ ] Run canonical and legacy adapters in shadow mode.
- [ ] Compare normalized rows, counts, links, chart series and checksums.
- [ ] Resolve every mismatch through an approved mapping or data-quality
  decision; never patch the UI to hide it.
- [ ] Enable canonical mode by environment overlay; retain legacy fallback only
  for a dated deprecation window.
- [ ] Remove fallback after consumer-impact review and rollback rehearsal.

**Exit evidence:** signed parity report, migration window, rollback plan and
retirement telemetry.

### Phase G — Build, deploy and acceptance

- [ ] Build from a clean signed release tag with immutable frontend revision.
- [ ] Validate environment schema and secret references before build.
- [ ] Deploy only the `kids` frontend namespace.
- [ ] Run route, discovery, query, download, localization, accessibility,
  performance and security smoke tests.
- [ ] Record release/image digest, browser evidence and rollback result.

**Exit evidence:** KIDS frontend acceptance bundle linked to the platform
contract revision and release authority.

## 4. Decisions required before Phase B

1. Is the KIDS public site allowed to read pages 8–12 anonymously, or must it
   obtain an OIDC token through a browser flow?
2. What is the approved public API origin for production? The current staging
   address `http://192.168.1.199:8083` is not a public production origin.
3. Which existing KIDS section categories map to the statistics contract's
   declared dimension/filter codes?
4. Which publication, legislation and external links should be migrated into
   governed resource metadata, and which remain static links?

Until these decisions are recorded in Control Plane/authority evidence, the
production overlay remains intentionally non-deployable.  No guessed URL or
semantic mapping may be substituted.

## 5. Definition of done

The KIDS frontend is integrated when all phases have evidence, every request is
contract/discovery-driven, all data transformations are tested, legacy fallback
has an approved retirement decision, and the exact frontend release is bound to
the approved KIDS contract revision.  Environment registration alone is not
considered API integration.
