# KIDS R8 page inventory — runtime evidence

Authenticated discovery against `KIDS_PORTAL_V1` revision `8` returned the
complete declared page surface:

| pageId | node | family | dataset | projection |
|---:|---|---|---|---|
| 7 | `KIDS_ROOT` | site root | — | — |
| 8 | `KIDS_GOALS` | ENTITY | `KIDS_GOAL` | `KIDS_GOAL_ENTITY` |
| 9 | `KIDS_RESOURCES` | ENTITY | `KIDS_RESOURCE` | `KIDS_RESOURCE_ENTITY` |
| 10 | `KIDS_GLOSSARY` | ENTITY | `KIDS_GLOSSARY_ENTRY` | `KIDS_GLOSSARY_ENTRY_ENTITY` |
| 11 | `KIDS_STATISTICS` | STATISTICAL | `KIDS_STATISTICAL_INPUT` | `KIDS_STATS_INPUT` |
| 12 | `KIDS_CLASSIFIERS` | REFERENCE | `KIDS_CLASSIFIER_ITEM` | `KIDS_CLASSIFIER_ITEM` |

Runtime discovery also exposed the declared fields, filters, aggregations and
relation include names for every data page. Page 8 was replayed through OIDC
and returned HTTP 200 from the Data Plane-backed `__ent_kids_goal` binding.

This inventory is descriptive evidence; it does not claim publication approval
for datasets whose quality, privacy, reconciliation or steward gates remain
open.
