# KIDS_RESOURCE contract/schema design review

The R8 resource surface is operationally valid (`225` rows, `225` distinct
natural keys, no missing key/category/lineage values), but operational validity
is not the end of design review.

## Findings

- `source_resource_id` is a unique, non-null natural key.
- `source_row_key` is present for every row and preserves raw lineage.
- `category_item_ref` is complete, but its value carries a structured
  `scheme|version|item` identity. The contract currently treats it as one CODE.
- Titles and paths are correctly separated into localized text and locator
  fields; no legacy language parameter is needed in the canonical response.

## Elevation decision

Do not silently split or rewrite `category_item_ref` in R8: that would change
identity semantics without a compatibility migration. The next contract
revision should evaluate a typed classifier-reference projection
(`schemeCode`, `versionRef`, `itemCode`) while retaining the original
`category_item_ref` as an immutable source value and preserving the declared
foreign-key relation. Any adoption requires a versioned migration, dual-read
compatibility, reconciliation and rollback evidence.

This review is therefore an explicit improvement item, not a reason to lower
the current contract or mutate the approved Access artifact in place.
