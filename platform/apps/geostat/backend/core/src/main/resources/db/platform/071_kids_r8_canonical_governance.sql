/* KIDS R8 canonical governance closure.  Additive and idempotent: history is
   preserved, while only one executable revision/source registry is active. */
DECLARE @contract BIGINT=(SELECT TOP 1 contract_id FROM platform.ingestion_contract WHERE contract_code=N'KIDS_PORTAL_V1');
DECLARE @product BIGINT=(SELECT TOP 1 product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');
IF @contract IS NULL OR @product IS NULL THROW 51071,'KIDS R8 prerequisites are missing',1;

IF NOT EXISTS(SELECT 1 FROM platform.ingestion_contract_revision WHERE contract_id=@contract AND revision=8)
 INSERT platform.ingestion_contract_revision(contract_id,revision,format_profile,lifecycle_status,compatibility_mode)
 VALUES(@contract,8,'ACCESS_CANONICAL_R8','APPROVED','BACKWARD_COMPATIBLE');
DECLARE @r8 BIGINT=(SELECT TOP 1 ingestion_contract_revision_id FROM platform.ingestion_contract_revision WHERE contract_id=@contract AND revision=8 ORDER BY ingestion_contract_revision_id DESC);
UPDATE platform.ingestion_contract_revision SET lifecycle_status=CASE WHEN revision=8 THEN 'APPROVED' ELSE 'SUPERSEDED' END WHERE contract_id=@contract;

/* Site contract dataset registry is the authority for the R8 ingestion source list. */
INSERT platform.contract_revision_source(ingestion_contract_revision_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json)
SELECT @r8,CONCAT(N'ACCESS.',sc.access_table_name),N'ACCESS_TABLE',sc.dataset_version_id,sc.natural_key_expression,sc.row_role,sc.load_order,
       N'{"approvalState":"READY","canonicalAccessRevision":8}'
FROM platform.site_contract_dataset sc
WHERE sc.site_contract_revision_id=(SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=8 ORDER BY site_contract_revision_id DESC)
  AND NOT EXISTS(SELECT 1 FROM platform.contract_revision_source x WHERE x.ingestion_contract_revision_id=@r8 AND x.source_locator=CONCAT(N'ACCESS.',sc.access_table_name));
UPDATE rs SET mapping_spec_json=JSON_MODIFY(JSON_MODIFY(rs.mapping_spec_json,'$.approvalState','READY'),'$.canonicalAccessRevision',8)
FROM platform.contract_revision_source rs WHERE rs.ingestion_contract_revision_id=@r8;

/* The generic contract_source table is used by scheduled workers. Keep only
   the prefixed R8 locators executable; preserve old rows for audit history. */
UPDATE cs SET active=0
FROM platform.contract_source cs
WHERE cs.contract_id=@contract AND (cs.source_locator NOT LIKE N'ACCESS.__%' OR cs.source_locator LIKE N'ACCESS.kids_%');
UPDATE cs SET active=1,mapping_spec_json=JSON_MODIFY(JSON_MODIFY(cs.mapping_spec_json,'$.approvalState','READY'),'$.canonicalAccessRevision',8)
FROM platform.contract_source cs WHERE cs.contract_id=@contract AND cs.source_locator LIKE N'ACCESS.__%';
UPDATE platform.ingestion_contract SET contract_revision=8,status='ACTIVE',auto_publish=0,raw_ingest_enabled=1 WHERE contract_id=@contract;

/* Do not allow a stale site revision to be selected as current. */
UPDATE platform.site_contract_revision SET status=CASE WHEN revision=8 THEN 'APPROVED' ELSE 'SUPERSEDED' END WHERE contract_code=N'KIDS_PORTAL_V1';
