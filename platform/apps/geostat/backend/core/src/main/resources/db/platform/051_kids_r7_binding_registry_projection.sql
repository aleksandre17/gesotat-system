/* Binding rows are governance metadata; authoritative bindings live in the
   control-plane registry and are validated, not coerced into entity links. */
UPDATE platform.contract_revision_source SET mapping_spec_json=N'{"approvalState":"READY","family":"REFERENCE","registry":"platform.statistical_semantic_binding","qualityPolicy":"KIDS_AGGREGATE_QUALITY_V1","confidentialityPolicy":"KIDS_PUBLIC_AGGREGATE_V1"}' WHERE source_locator=N'__rel_kids_statistical_semantic_binding';
