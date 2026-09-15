/* Reconcile the ingest registry from the authoritative approved site
   contract dataset list. This covers environments where the site contract
   exists but the older ingestion-source registry was incomplete. */
DECLARE @contract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE contract_code=N'KIDS_PORTAL_V1');
DECLARE @site BIGINT=(SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=8 ORDER BY site_contract_revision_id DESC);
DECLARE @rev BIGINT=(SELECT TOP 1 ingestion_contract_revision_id FROM platform.ingestion_contract_revision WHERE contract_id=@contract AND revision=8 ORDER BY ingestion_contract_revision_id DESC);
INSERT platform.contract_source(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json,active)
SELECT @contract,CONCAT(N'ACCESS.',sc.access_table_name),N'ACCESS_TABLE',sc.dataset_version_id,sc.natural_key_expression,sc.row_role,sc.load_order,N'{"approvalState":"READY","canonicalAccessRevision":8}',1
FROM platform.site_contract_dataset sc
WHERE sc.site_contract_revision_id=@site AND @contract IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM platform.contract_source x WHERE x.contract_id=@contract AND x.source_locator=CONCAT(N'ACCESS.',sc.access_table_name));
INSERT platform.contract_revision_source(ingestion_contract_revision_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json)
SELECT @rev,CONCAT(N'ACCESS.',sc.access_table_name),N'ACCESS_TABLE',sc.dataset_version_id,sc.natural_key_expression,sc.row_role,sc.load_order,N'{"approvalState":"READY","canonicalAccessRevision":8}'
FROM platform.site_contract_dataset sc
WHERE sc.site_contract_revision_id=@site AND @rev IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM platform.contract_revision_source x WHERE x.ingestion_contract_revision_id=@rev AND x.source_locator=CONCAT(N'ACCESS.',sc.access_table_name));
