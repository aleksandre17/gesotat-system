/* Complete R8 Access source registry. Idempotent: only missing bindings are
   inserted, and existing revisions are never mutated. */
DECLARE @contract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE contract_code=N'KIDS_PORTAL_V1');
DECLARE @revision BIGINT=(SELECT TOP 1 ingestion_contract_revision_id FROM platform.ingestion_contract_revision WHERE contract_id=@contract AND revision=8 ORDER BY ingestion_contract_revision_id DESC);
DECLARE @product BIGINT=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');
DECLARE @spec TABLE(code NVARCHAR(120),locator NVARCHAR(1024),key_expr NVARCHAR(128),role VARCHAR(32),ord INT);
INSERT @spec VALUES
(N'KIDS_RAW_DOCUMENT',N'ACCESS.__raw_document',N'source_row_key',N'RAW',60),
(N'KIDS_GOAL',N'ACCESS.__ent_kids_goal',N'source_goal_id',N'ENTITY',100),
(N'KIDS_RESOURCE',N'ACCESS.__ent_kids_resource',N'source_resource_id',N'ENTITY',110),
(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',N'ACCESS.__rel_kids_resource_subcategory_assignment',N'assignment_key',N'RELATION',120),
(N'KIDS_GLOSSARY_ENTRY',N'ACCESS.__ent_kids_glossary_entry',N'source_glossary_id',N'ENTITY',130),
(N'KIDS_STATISTICAL_CARRIER',N'ACCESS.__raw_kids_statistical_carrier',N'carrier_code',N'RAW',140),
(N'KIDS_STATISTICAL_INPUT',N'ACCESS.__stat_kids_statistical_input',N'input_key',N'STATISTICAL',150),
(N'KIDS_STATISTICAL_SEMANTIC_BINDING',N'ACCESS.__rel_kids_statistical_semantic_binding',N'carrier_code',N'RELATION',160);
INSERT platform.contract_revision_source(ingestion_contract_revision_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json)
SELECT @revision,s.locator,N'ACCESS_TABLE',dv.dataset_version_id,s.key_expr,s.role,s.ord,N'{"approvalState":"READY","canonicalAccessRevision":8,"rawLineageRequired":true}'
FROM @spec s JOIN platform.dataset d ON d.product_id=@product AND d.dataset_code=s.code
JOIN platform.dataset_version dv ON dv.dataset_id=d.dataset_id AND dv.version=(SELECT MAX(version) FROM platform.dataset_version x WHERE x.dataset_id=d.dataset_id)
WHERE @revision IS NOT NULL AND NOT EXISTS (SELECT 1 FROM platform.contract_revision_source x WHERE x.ingestion_contract_revision_id=@revision AND x.source_locator=s.locator);
INSERT platform.contract_source(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json,active)
SELECT @contract,s.locator,N'ACCESS_TABLE',dv.dataset_version_id,s.key_expr,s.role,s.ord,N'{"approvalState":"READY","canonicalAccessRevision":8,"rawLineageRequired":true}',1
FROM @spec s JOIN platform.dataset d ON d.product_id=@product AND d.dataset_code=s.code
JOIN platform.dataset_version dv ON dv.dataset_id=d.dataset_id AND dv.version=(SELECT MAX(version) FROM platform.dataset_version x WHERE x.dataset_id=d.dataset_id)
WHERE @contract IS NOT NULL AND NOT EXISTS (SELECT 1 FROM platform.contract_source x WHERE x.contract_id=@contract AND x.source_locator=s.locator);
