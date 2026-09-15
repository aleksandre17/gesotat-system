# Final classifier contract

## Canonical lifecycle

`SCHEME → VERSION → ITEM → ALIAS → RESOLVED SNAPSHOT`.

- A **scheme** names a value domain (`AGE_GROUP`, `LANGUAGE`, `KIDS_GOAL_CATEGORY`).
- A **version** is time-bounded and immutable after approval.
- An **item** has stable code, multilingual labels, status and optional hierarchy.
- An **alias** maps an external source code to one versioned item; normalization must be explicit.
- A **snapshot** records the resolved version/item used by a publication.

## Access representation

`__cl_scheme`, `__cl_version`, `__cl_item`, `__cl_alias`, `__cl_hierarchy` are optional only when no classifier values are carried. If a source column is declared `CLASSIFICATION`, `DIMENSION`, `CODE`, or a relation target, the corresponding classifier dataset/reference is mandatory.

Every classifier row contains `authority_mode`:

- `SNAPSHOT`: read-only copy of an already approved Control-Plane version;
- `PROPOSAL`: source codelist/alias candidate, always `DRAFT` until reviewed.

An Access package may not mark a `PROPOSAL` as published or replace an existing Control-Plane item.

## Required KIDS classifier scopes

| Code | Source | Access role | Initial state |
|---|---|---|---|
| `KIDS_GOAL_CATEGORY` | `goals_titles` | source codelist + aliases for goal/resource category | DRAFT crosswalk to `UN_SDG_GOAL` |
| `LANGUAGE` | `glossary.lang` | aliases `ka`, `en`; `ena` proposal/exception | mixed READY/DRAFT |
| `KIDS_RESOURCE_SUBCATEGORY` | split `files.sub_category` | source token proposal only | DRAFT |
| `AGE_GROUP` | chart JSON dynamic keys | source alias proposal; raw key retained | DRAFT |

## Validation invariants

1. `(scheme_code, version_code, item_code)` is unique.
2. Alias source-system/code is unique within an effective interval.
3. Every classifier assignment references a declared scheme/version/code or is explicitly `UNRESOLVED` with a quality issue.
4. Parent/child hierarchy is acyclic and version-local.
5. A source alias can be trim-normalized only if the contract declares that exact rule; the unmodified source value remains in lineage.
6. No semantic projection or publication uses an unresolved/draft item unless policy explicitly permits raw-only retention.
