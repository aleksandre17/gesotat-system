/* Complete executable mapping specs for every R8 prefixed binding.  The
   binding registry is the single source of truth; no family is inferred from
   a table name at runtime. */
UPDATE rs SET mapping_spec_json = CASE rs.source_locator
WHEN N'ACCESS.__ent_kids_goal' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"ENTITY","recordType":"KIDS_GOAL","key":"source_goal_id","title":"title_ka","localizedText":[{"fieldCode":"TITLE","language":"ka","path":"title_ka","required":true},{"fieldCode":"TITLE","language":"en","path":"title_en","required":false}],"locators":[{"kind":"PATH","language":"ka","path":"path_ka"},{"kind":"PATH","language":"en","path":"path_en"}]}'
WHEN N'ACCESS.__ent_kids_resource' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"ENTITY","recordType":"KIDS_RESOURCE","key":"source_resource_id","title":"title_ka","localizedText":[{"fieldCode":"TITLE","language":"ka","path":"title_ka","required":true},{"fieldCode":"TITLE","language":"en","path":"title_en","required":false}],"locators":[{"kind":"PATH","language":"ka","path":"path_ka"},{"kind":"PATH","language":"en","path":"path_en"}]}'
WHEN N'ACCESS.__ent_kids_glossary_entry' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"ENTITY","recordType":"KIDS_GLOSSARY_ENTRY","key":"source_glossary_id","title":"entry_text","localizedText":[{"fieldCode":"TEXT","language":"und","path":"entry_text","required":true}]}'
WHEN N'ACCESS.__rel_kids_resource_subcategory_assignment' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"RELATION","relationshipTypeId":1,"fromKey":"source_resource_id","toKey":"subcategory_item_ref"}'
WHEN N'ACCESS.__raw_document' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"RAW"}'
WHEN N'ACCESS.__raw_kids_statistical_carrier' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"RAW"}'
WHEN N'ACCESS.__stat_kids_statistical_input' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"STATISTICAL","projectionModel":"SDMX_COMPATIBLE_LONG","carrierField":"carrier_code","periodField":"period_normalized","ageField":"age_group_item_ref","valueField":"value_decimal","ageDimensionId":1,"dataflowCode":"KIDS_FILES_STATISTICS","dsdCode":"KIDS_FILES_STATISTICS_DSD"}'
WHEN N'ACCESS.__rel_kids_statistical_semantic_binding' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"REFERENCE","registry":"platform.statistical_semantic_binding"}'
WHEN N'ACCESS.__cl_scheme' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"REFERENCE"}'
WHEN N'ACCESS.__cl_version' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"REFERENCE"}'
WHEN N'ACCESS.__cl_item' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"REFERENCE"}'
WHEN N'ACCESS.__cl_alias' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"REFERENCE"}'
WHEN N'ACCESS.__cl_hierarchy' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"REFERENCE"}'
WHEN N'ACCESS.__stat_metric' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"REFERENCE"}'
WHEN N'ACCESS.__stat_unit' THEN N'{"approvalState":"READY","canonicalAccessRevision":8,"family":"REFERENCE"}'
ELSE rs.mapping_spec_json END
FROM platform.contract_revision_source rs
JOIN platform.ingestion_contract_revision r ON r.ingestion_contract_revision_id=rs.ingestion_contract_revision_id
JOIN platform.ingestion_contract c ON c.contract_id=r.contract_id
WHERE c.contract_code=N'KIDS_PORTAL_V1' AND r.revision=7 AND rs.source_locator LIKE N'ACCESS.__%';
