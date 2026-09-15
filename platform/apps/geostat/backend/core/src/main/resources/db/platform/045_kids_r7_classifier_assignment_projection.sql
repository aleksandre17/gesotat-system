/* KIDS subcategory membership is classifier assignment, never an entity-link. */
UPDATE platform.contract_revision_source
SET mapping_spec_json=N'{"approvalState":"READY","family":"CLASSIFICATION","entityKey":"source_resource_id","classificationCode":"subcategory_item_ref","externalSystemCode":"KIDS_LEGACY_ACCESS","attributeId":1,"qualityPolicy":"KIDS_AGGREGATE_QUALITY_V1","confidentialityPolicy":"KIDS_PUBLIC_AGGREGATE_V1"}'
WHERE source_locator=N'__rel_kids_resource_subcategory_assignment';
