# Kids source semantic inventory — review input

**Collected read-only from `kids.dbo` on 2026-09-09.** This is evidence for stewardship; it is not an authorization to auto-publish metadata.

## Source facts

| Object | Rows | Canonical decision |
|---|---:|---|
| `goals_titles` | 17 | source vocabulary for SDG-goal category classification |
| `goals` | 36 | `KIDS_GOAL` entities |
| `files` | 225 | `KIDS_FILE_RESOURCE` entities; 43 parseable direct-or-escaped chart-array carriers |
| `glossary` | 178 | `KIDS_GLOSSARY_ENTRY` entities |

All `goals.category` values (36/36) and `files.category` values (225/225) resolve to `goals_titles.ID`. There are no declared SQL Server foreign keys, so this was validated by join rather than assumed from naming.

## SDG category vocabulary candidate

Codes `1`–`17` have bilingual labels corresponding to Sustainable Development Goals. Register them as the **draft** source-to-standard crosswalk for an `SDG_GOAL` classification scheme; the steward confirms official Georgian/English labels and publication status.

```text
1  NO POVERTY
2  ZERO HUNGER
3  GOOD HEALTH AND WELL-BEING
4  QUALITY EDUCATION
5  GENDER EQUALITY
6  CLEAN WATER AND SANITATION
7  AFFORDABLE AND CLEAN ENERGY
8  DECENT WORK AND ECONOMIC ...
9  INDUSTRY, INNOVATION AND INFRASTRUCTURE
10 REDUCED INEQUALITIES
11 SUSTAINABLE CITIES AND COMMUNITIES
12 RESPONSIBLE CONSUMPTION AND PRODUCTION
13 CLIMATE ACTION
14 LIFE BELOW WATER
15 LIFE ON LAND
16 PEACE, JUSTICE AND STRONG INSTITUTIONS
17 PARTNERSHIPS FOR THE GOALS
```

The portal source title is retained as lineage. The standard scheme, code and version are the canonical classification—not a copied title in every entity.

## Values needing explicit stewardship decisions

### File subcategories

Raw values are `1`, `2`, `3`, `1,3`, and `1,4`. The last two prove multi-value encoding. There is no lookup table and no FK describing the codes.

Decision required: supply a code list/crosswalk and semantic scheme name. Then split values on comma, trim and deduplicate, producing many classification assignments. Do not create labels such as “subcategory 1” as a fabricated canonical meaning.

### Glossary languages

Counts are `ka: 90`, `en: 87`, `ena: 1`.

`ka` and `en` are candidates for aliases to BCP-47/ISO language items. `ena` stays an exception until its source meaning is reviewed; never auto-correct it silently.

### Chart data

43 `files.chartdata` values are parseable JSON arrays when both direct JSON and the source's escaped JSON-text representation are handled. The observed dynamic keys are:

```text
0-5, 0-12, 0-17, 3-17, 13-17, 15-17, 15-24, 15-29
```

One source key contains trailing whitespace (`0-12 `). The normalizer performs minimal trim-only lookup normalization while retaining the original key in raw lineage and `scalar_code`.

All age bands require a reviewed `AGE_GROUP` classification mapping. Numeric range strings alone do not establish inclusive/exclusive boundary semantics or a standard version.

Chart carrier IDs are 162–181, 183, 297, 299, 301, 303, 305, 306, 308, 310, 385–387. They have materially different concepts and units (counts, averages, and values explicitly expressed in thousands). Each must receive its own draft metric/measure/unit review; a title is evidence, not enough to infer a unit or aggregation rule automatically.

## Required review output

Before setting a Kids mapping to `READY`, the steward provides:

1. published/draft SDG scheme version and source-code aliases;
2. subcategory scheme/crosswalk, or an explicit decision to retain it as raw only;
3. language alias decision for `ena`;
4. age-group scheme, range semantics and aliases;
5. one metric/measure/unit/aggregation decision per chart carrier;
6. any direct file-to-goal relationship crosswalk, if such a relation is desired.

Only then may `platform.metric_alias`, `platform.classification_alias`, the reviewed multi-projection mapping, and `approvalState: READY` be populated.
