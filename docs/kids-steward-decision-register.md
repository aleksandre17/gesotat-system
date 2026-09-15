# Kids contract — steward decision register

This register closes every currently unresolved semantic edge explicitly. `DRAFT` means the platform knows the question and blocks only the affected projection; it never means “guess a value”.

| ID | Subject | Evidence | Required decision | Enforcement |
|---|---|---|---|---|
| KIDS-STAT-001 | 43 JSON carrier files | direct/escaped arrays; exact payloads, titles and source IDs recorded | metric concept, unit, aggregation, methodology, confidentiality | no `statistics.observation` or publication without an approved metric binding |
| KIDS-STAT-002 | eight age-band keys | `0-12 `, `0-5`, `0-17`, `3-17`, `13-17`, `15-17`, `15-24`, `15-29` | official age-band codelist/version and inclusive-boundary semantics | raw key retained; no observation without a published alias |
| KIDS-REF-001 | `sub_category` tokens | values `1`, `2`, `3`, `1,3`, `1,4` | code meanings, codelist ownership and whether `4` is valid | split only after approval; otherwise raw-only |
| KIDS-LANG-001 | language `ena` | exactly one glossary row | valid language/locale or source correction | no normalized language classification for that row |

## Contract status

`KIDS_PORTAL_V1` is structurally complete and `REVIEW_REQUIRED`. The entity/reference path may become executable once its classifications and language aliases pass their corresponding decisions. The statistical path remains separately gated by `KIDS-STAT-001` and `KIDS-STAT-002`; this prevents a non-statistical correction from being blocked by a chart methodology decision, while preserving one site-level contract and one audit trail.

## Access package rule

The panel-generated Access v3 template contains the immutable contract identity, source schema declarations and a statistical projection reference. It does **not** contain decision values for the four records above. At preview/import, the resolver receives the approved Core revision; any unresolved decision produces a deterministic `SEMANTIC_DECISION_REQUIRED` result with this register ID.
