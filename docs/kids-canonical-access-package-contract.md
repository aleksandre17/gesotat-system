# KIDS canonical Access package — revision 8

Revision 8 is the current governed KIDS Access shape. Revision 7 and earlier remain immutable and resolvable. R8 adds the complete raw-document envelope, explicit statistical natural-grain keys and governed metadata transport without rewriting source observations.

## Identity

| Property | Value |
|---|---|
| Product | `KIDS_PORTAL` |
| Contract | `KIDS_PORTAL_V1`, revision `8` |
| Package | `kids-portal-canonical`, `8.0.0` |
| Format profile | `ACCESS_CANONICAL_R8_PAGE_MANIFEST` |
| Mode | `SNAPSHOT` |
| Artifact | `samples/kids-portal-v1-canonical-r7.accdb` |
| Generator | `./gradlew.bat :api:generateKidsCanonicalAccess` |

## Contract boundary

The Control Plane owns hierarchy, versioned dataset/field specifications, relations, classifier policy, SDMX-compatible DSD components and blocking publication gates. Access executes that issued shape and carries source-derived data. It does not define site semantics independently.

The package contains 25 physical tables. Its metadata declares exactly 15 datasets, 122 fields, 17 key rows, 21 relations and six projections. Revision 8 adds the raw envelope, `__gs_metadata_schema` and `__gs_metadata`, and declares the statistical input natural grain.

## Data guarantees

- All 456 source rows have a stable locator in `__raw_document`; the immutable original Access file is the raw-byte authority.
- `kids_statistical_carrier` contains only identity, resource link, payload checksum, parse status, source-row link and operation. It never duplicates `files.chartdata` or any raw JSON.
- Goals, resources and glossary entries are separate entities; resource subcategories are normalized assignments.
- Goal category, language, subcategory and age-group values travel as versioned classifier proposals and aliases. Unknown semantics remain `DRAFT`.
- Parseable source chart arrays yield 43 carriers and 880 lossless input cells. Values are source-derived, never seeded statistical facts.
- `TIME_PERIOD` and `AGE_GROUP` are dimensions. `OBS_VALUE` is the DSD component backed by the KIDS-specific `KIDS_OBS_VALUE_INPUT` measure whose default aggregation is `NONE`.
- All 43 carriers have explicit metric/unit bindings with confidence and rationale. Because age bands overlap, aggregation is `NONE` for every metric.
- Quality policy is `KIDS_AGGREGATE_QUALITY_V1`; confidentiality policy is `KIDS_PUBLIC_AGGREGATE_V1`.

Every table has a physical primary key; additional unique indexes protect natural/composite uniqueness; all 21 declared parent-child links are physical Access relationships with referential integrity.

See [revision-7 semantic decisions](kids-statistical-semantics-r7.md), [column audit](kids-column-audit.md), [KIDS model](final-kids-canonical-model.md), and [acceptance record](kids-production-access-acceptance.md).

## Revision 8 metadata extension

The production page contract is `KIDS_PORTAL_V1` revision 8. The canonical R8 artifact additionally carries the technical metadata transport tables `__gs_metadata_schema` and `__gs_metadata`. They are not business datasets and therefore do not alter the 15 data-dataset contract boundary. They carry versioned namespace/schema declarations and typed, localized assertions for pages, visualizations and future resources. On ingest, the reader validates these tables and materializes approved/ready assertions into `platform.metadata_schema`, `platform.metadata_subject` and `platform.metadata_assertion`; API publication exposes only governed public metadata. The artifact is [kids-portal-v1-canonical-r8-final-metadata-v3.accdb](../api/kids-portal-v1-canonical-r8-final-metadata-v3.accdb).
