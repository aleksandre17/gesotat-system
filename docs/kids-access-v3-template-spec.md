# KIDS_PORTAL_V1 Access v3 template

The generated package header is immutable:

```text
product_code=KIDS_PORTAL
contract_code=KIDS_PORTAL_V1
contract_revision=2
package_version=3.0.0
```

It contains `goals_titles`, `goals`, `files`, and `glossary` source tables plus complete `__gs_package`, `__gs_dataset`, `__gs_field`, `__gs_key`, `__gs_relation`, and `__gs_projection` declarations. The source tables retain their original field spelling. `files.chartdata` is declared RAW_PAYLOAD and has a second `STATISTICAL_WIDE_JSON` projection bound to `KIDS_FILES_STATISTICS`; it never becomes an Access-managed target table.

## Complete data-bearing artifact

`KidsPortalAccessV3TemplateGenerator` takes an optional second argument: a read-only legacy KIDS Access export.  In that mode it creates a data-bearing Access v3 package: metadata is generated from the approved contract and every source row from `goals_titles`, `goals`, `files`, and `glossary` is copied into the identically named contracted table.  The copy does not parse, transform, or fabricate `files.chartdata`; its later statistical interpretation remains governed by its approved projection and DSD decisions.

The superseded v3 prototype has `__gs_statistical_binding` for the 32 direct-JSON values known to that earlier profile. It is retained only as historical test coverage. The current canonical r5 production artifact exhaustively discovers 43 direct-or-escaped array carriers. In either shape, binding metadata identifies evidence and DRAFT lifecycle only; it never fabricates metric, unit, aggregation, or age-band meaning.

The package is generated from the contract revision, not authored manually. Its preview must validate every source column, source key and relationship against the approved Control Plane bindings before upload.
