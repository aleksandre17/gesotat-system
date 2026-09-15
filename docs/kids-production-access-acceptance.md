# KIDS production Access acceptance record

**Accepted:** 2026-09-10  
**Artifact:** `platform/apps/geostat/backend/api/kids-portal-v1-canonical-r8-final.accdb`  
**Contract:** `KIDS_PORTAL_V1` revision 8  
**Package:** `kids-portal-canonical` 8.0.0 (`ACCESS_CANONICAL_R8_PAGE_MANIFEST`)

Accepted artifact size: **1,916,928 bytes**. SHA-256: `A2760BE2220816205976C3B2084AAFB2ADA89E96F26A23B22CD4D58E59F6E512`.

## Repository artifact revalidation — 2026-09-15

The canonical repository file at the path above currently measures **2,920,448
bytes** and has SHA-256
`1930EAD852912858F25D85867FC075AAFFECD7F6704C2540E41E623764D27ACC`.
This is recorded as a new artifact fingerprint, not as a silent rewrite of the
historical 2026-09-10 acceptance record. Any production replay must select one
fingerprint explicitly and rerun the ingest/reconciliation gates against that
exact immutable byte stream.

## Source boundary

The governed source is `samples/kids-children-portal-full-data.accdb`: 17 goal-category rows, 36 goals, 225 resources and 178 glossary entries, or 456 identifiable rows. Original bytes remain authoritative in that immutable artifact; the canonical package stores stable row locators only.

`chartjson.dat` is a separate archive of 98 anonymous arrays without a proven resource/chart identifier. It is not joined to KIDS because an ordinal join would fabricate identity. It can be onboarded only through a steward-approved crosswalk and a new contract revision.

## Accepted inventory

| Area | Rows |
|---|---:|
| `__gs_package / dataset / field / key / relation / projection` | `1 / 15 / 122 / 17 / 21 / 6` |
| `__cl_scheme / version / item / alias / hierarchy` | `4 / 4 / 36 / 36 / 0` |
| `__raw_document` | 456 (22 columns) |
| `kids_goal` | 36 |
| `kids_resource` | 225 |
| `kids_resource_subcategory_assignment` | 230 |
| `kids_glossary_entry` | 178 |
| `kids_statistical_carrier` | 43 |
| `kids_statistical_input` | 880 |
| `__stat_unit` | 10 |
| `__stat_metric` | 43 |
| `kids_statistical_semantic_binding` | 43 |

The carrier table has exactly six columns and no raw-payload column. It stores a SHA-256 checksum computed from the source `chartdata`, while the source text remains only in the immutable source artifact. Each parsed non-period member becomes one input row with carrier identity, stable ordinal, raw/normalized period, raw age key, classifier proposal reference, lexical value, optional numeric parse, JSON path, encoding and source-row lineage.

Revision 8 preserves the 880 source observations and adds the complete raw envelope, explicit statistical natural grain and metadata transport. DSD revision 2 uses `TIME_PERIOD`, `AGE_GROUP`, `OBS_VALUE`, carrier-specific unit, `aggregation=NONE`, observation status `A`, confidentiality status `F`, `KIDS_AGGREGATE_QUALITY_V1` and `KIDS_PUBLIC_AGGREGATE_V1`.

## Verified integrity

- All 21 tables have physical primary keys.
- Six additional unique constraints protect classifier versions/items, raw locators, assignment ordinals, one carrier per resource and carrier-cell ordinals.
- All 21 declared relations are enforced by Access referential integrity.
- Category lookup is fail-fast and generation refuses to overwrite an existing artifact.
- Expected entity totals are derived from source tables; there is no fixed statistical carrier list or seeded observation value.
- Full API test suite and the read-only Access audit passed on 2026-09-10.

```powershell
.\gradlew.bat :api:generateKidsCanonicalAccess --console=plain
.\gradlew.bat :api:auditKidsCanonicalAccess :api:test --rerun-tasks --console=plain
```
# Current runtime authority

See [KIDS R8 Current Status and Acceptance](KIDS-R8-current-status-and-acceptance.md) for the post-deployment evidence, migration 071–073 results, rollback replay, archive-object verification and idempotent ingest replay. The inventory below remains the artifact/schema acceptance record.
