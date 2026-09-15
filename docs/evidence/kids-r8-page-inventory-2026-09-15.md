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
relation include names for every data page. Pages 8, 9 and 10 were replayed
through OIDC and returned HTTP 200 from Data Plane-backed `__ent_kids_goal`,
`__ent_kids_resource` and `__ent_kids_glossary_entry` bindings. Page 10's
full-text glossary content is served as localized content rather than being
forced into the bounded entity title field; its 178 natural keys are unique.

Detailed replay evidence: `kids-r8-page8-runtime-replay-2026-09-15.json`,
`kids-r8-page9-runtime-replay-2026-09-15.json` and
`kids-r8-page10-runtime-replay-2026-09-15.json`.

This inventory is descriptive evidence; it does not claim publication approval
for datasets whose quality, privacy, reconciliation or steward gates remain
open.
