/* Idempotent, non-destructive registration of the Kids source pilot. Run in geostat-system. */
IF NOT EXISTS (SELECT 1 FROM platform.source_system WHERE source_code=N'KIDS_LEGACY_SQL')
  INSERT platform.source_system(source_code,source_type,title,trust_level,enabled) VALUES(N'KIDS_LEGACY_SQL','SQL_SERVER',N'Kids legacy portal','REVIEWED',1);
IF NOT EXISTS (SELECT 1 FROM platform.source_connection WHERE source_system_id=(SELECT source_system_id FROM platform.source_system WHERE source_code=N'KIDS_LEGACY_SQL') AND endpoint=N'192.168.1.29' AND database_name=N'kids')
  INSERT platform.source_connection(source_system_id,connection_kind,endpoint,database_name,username,secret_reference,encryption_mode,enabled) VALUES((SELECT source_system_id FROM platform.source_system WHERE source_code=N'KIDS_LEGACY_SQL'),'SQL_SERVER',N'192.168.1.29',N'kids',N'sa',N'KIDS_SQL_PASSWORD','TLS',1);
UPDATE platform.source_connection SET username=N'sa', secret_reference=N'KIDS_SQL_PASSWORD', enabled=1 WHERE source_system_id=(SELECT source_system_id FROM platform.source_system WHERE source_code=N'KIDS_LEGACY_SQL') AND endpoint=N'192.168.1.29' AND database_name=N'kids';
IF NOT EXISTS (SELECT 1 FROM platform.data_product WHERE product_code=N'KIDS_PORTAL')
  INSERT platform.data_product(product_code,title_ka,title_en,lifecycle_status,sensitivity) VALUES(N'KIDS_PORTAL',N'ბავშვების პორტალი',N'Kids portal','PILOT','INTERNAL');
DECLARE @product BIGINT=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');
IF NOT EXISTS (SELECT 1 FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_CONTENT')
  INSERT platform.dataset(product_id,dataset_code,dataset_family,business_grain,lifecycle_status) VALUES(@product,N'KIDS_CONTENT','ENTITY',N'ერთი პორტალური შინაარსის ერთეული','PILOT');
DECLARE @dataset BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_CONTENT');
IF NOT EXISTS (SELECT 1 FROM platform.dataset_version WHERE dataset_id=@dataset AND version=1)
  INSERT platform.dataset_version(dataset_id,version,status) VALUES(@dataset,1,'DRAFT');
DECLARE @version BIGINT=(SELECT dataset_version_id FROM platform.dataset_version WHERE dataset_id=@dataset AND version=1);
IF NOT EXISTS (SELECT 1 FROM platform.ingestion_contract WHERE source_system_id=(SELECT source_system_id FROM platform.source_system WHERE source_code=N'KIDS_LEGACY_SQL') AND dataset_id=@dataset)
  INSERT platform.ingestion_contract(source_system_id,dataset_id,format_profile,ingestion_method,auto_publish,status) VALUES((SELECT source_system_id FROM platform.source_system WHERE source_code=N'KIDS_LEGACY_SQL'),@dataset,'SQL_SERVER_TABLES','SCHEDULED',0,'REVIEW_REQUIRED');
DECLARE @contract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE source_system_id=(SELECT source_system_id FROM platform.source_system WHERE source_code=N'KIDS_LEGACY_SQL') AND dataset_id=@dataset);
UPDATE platform.ingestion_contract SET status='REVIEW_REQUIRED', auto_publish=0 WHERE contract_id=@contract AND status='DRAFT';
MERGE platform.contract_source AS target
USING (VALUES
 (N'dbo.files',N'SQL_TABLE',N'id',N'ENTITY',100,N'{"family":"ENTITY","recordType":"FILE","key":"id"}'),
 (N'dbo.glossary',N'SQL_TABLE',N'id',N'ENTITY',110,N'{"family":"ENTITY","recordType":"GLOSSARY","key":"id"}'),
 (N'dbo.goals_titles',N'SQL_TABLE',N'id',N'ENTITY',120,N'{"family":"ENTITY","recordType":"GOAL_TITLE","key":"id"}'),
 (N'dbo.goals',N'SQL_TABLE',N'id',N'ENTITY',130,N'{"family":"ENTITY","recordType":"GOAL","key":"id"}')
) AS source(source_locator,source_kind,source_key_expression,row_role,load_order,mapping_spec_json)
ON target.contract_id=@contract AND target.source_locator=source.source_locator
WHEN MATCHED THEN UPDATE SET source_kind=source.source_kind, target_dataset_version_id=@version, source_key_expression=source.source_key_expression, row_role=source.row_role, load_order=source.load_order, mapping_spec_json=source.mapping_spec_json, active=1
WHEN NOT MATCHED THEN INSERT(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json) VALUES(@contract,source.source_locator,source.source_kind,@version,source.source_key_expression,source.row_role,source.load_order,source.mapping_spec_json);
