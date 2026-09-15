/* Run in geostat-system. Replace the discovery-only monolith with logical datasets; no legacy data is changed. */
DECLARE @product BIGINT=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');

IF NOT EXISTS (SELECT 1 FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_GOAL')
  INSERT platform.dataset(product_id,dataset_code,dataset_family,business_grain,lifecycle_status) VALUES(@product,N'KIDS_GOAL','ENTITY',N'ერთი პორტალის მიზანი (goals.ID)','DRAFT');
IF NOT EXISTS (SELECT 1 FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_FILE_RESOURCE')
  INSERT platform.dataset(product_id,dataset_code,dataset_family,business_grain,lifecycle_status) VALUES(@product,N'KIDS_FILE_RESOURCE','ENTITY',N'ერთი პორტალური რესურსი (files.ID), სურვილისამებრ სტატისტიკური projection-ით','DRAFT');
IF NOT EXISTS (SELECT 1 FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_GLOSSARY_ENTRY')
  INSERT platform.dataset(product_id,dataset_code,dataset_family,business_grain,lifecycle_status) VALUES(@product,N'KIDS_GLOSSARY_ENTRY','ENTITY',N'ერთი glossary source entry (glossary.ID)','DRAFT');

DECLARE @goals BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_GOAL');
DECLARE @files BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_FILE_RESOURCE');
DECLARE @glossary BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_GLOSSARY_ENTRY');
IF NOT EXISTS (SELECT 1 FROM platform.dataset_version WHERE dataset_id=@goals AND version=1) INSERT platform.dataset_version(dataset_id,version,status) VALUES(@goals,1,'DRAFT');
IF NOT EXISTS (SELECT 1 FROM platform.dataset_version WHERE dataset_id=@files AND version=1) INSERT platform.dataset_version(dataset_id,version,status) VALUES(@files,1,'DRAFT');
IF NOT EXISTS (SELECT 1 FROM platform.dataset_version WHERE dataset_id=@glossary AND version=1) INSERT platform.dataset_version(dataset_id,version,status) VALUES(@glossary,1,'DRAFT');
DECLARE @goalsVersion BIGINT=(SELECT dataset_version_id FROM platform.dataset_version WHERE dataset_id=@goals AND version=1);
DECLARE @filesVersion BIGINT=(SELECT dataset_version_id FROM platform.dataset_version WHERE dataset_id=@files AND version=1);
DECLARE @glossaryVersion BIGINT=(SELECT dataset_version_id FROM platform.dataset_version WHERE dataset_id=@glossary AND version=1);

IF NOT EXISTS (SELECT 1 FROM platform.attribute WHERE dataset_version_id=@goalsVersion AND attribute_code=N'GOAL_CATEGORY') INSERT platform.attribute(dataset_version_id,attribute_code,logical_type,attribute_role,cardinality,required,filterable,label_ka,label_en,json_path) VALUES(@goalsVersion,N'GOAL_CATEGORY','CLASSIFICATION','DIMENSION','SINGLE',1,1,N'მდგრადი განვითარების მიზანი',N'Sustainable development goal',N'category');
IF NOT EXISTS (SELECT 1 FROM platform.attribute WHERE dataset_version_id=@filesVersion AND attribute_code=N'GOAL_CATEGORY') INSERT platform.attribute(dataset_version_id,attribute_code,logical_type,attribute_role,cardinality,required,filterable,label_ka,label_en,json_path) VALUES(@filesVersion,N'GOAL_CATEGORY','CLASSIFICATION','DIMENSION','SINGLE',1,1,N'მდგრადი განვითარების მიზანი',N'Sustainable development goal',N'category');
IF NOT EXISTS (SELECT 1 FROM platform.attribute WHERE dataset_version_id=@glossaryVersion AND attribute_code=N'LANGUAGE') INSERT platform.attribute(dataset_version_id,attribute_code,logical_type,attribute_role,cardinality,required,filterable,label_ka,label_en,json_path) VALUES(@glossaryVersion,N'LANGUAGE','CLASSIFICATION','DIMENSION','SINGLE',1,1,N'ენა',N'Language',N'lang');

DECLARE @source BIGINT=(SELECT source_system_id FROM platform.source_system WHERE source_code=N'KIDS_LEGACY_SQL');
IF NOT EXISTS (SELECT 1 FROM platform.ingestion_contract WHERE source_system_id=@source AND dataset_id=@goals) INSERT platform.ingestion_contract(source_system_id,dataset_id,format_profile,ingestion_method,auto_publish,status) VALUES(@source,@goals,'SQL_SERVER_TABLE','SCHEDULED',0,'DRAFT');
IF NOT EXISTS (SELECT 1 FROM platform.ingestion_contract WHERE source_system_id=@source AND dataset_id=@files) INSERT platform.ingestion_contract(source_system_id,dataset_id,format_profile,ingestion_method,auto_publish,status) VALUES(@source,@files,'SQL_SERVER_TABLE','SCHEDULED',0,'DRAFT');
IF NOT EXISTS (SELECT 1 FROM platform.ingestion_contract WHERE source_system_id=@source AND dataset_id=@glossary) INSERT platform.ingestion_contract(source_system_id,dataset_id,format_profile,ingestion_method,auto_publish,status) VALUES(@source,@glossary,'SQL_SERVER_TABLE','SCHEDULED',0,'DRAFT');
DECLARE @goalsContract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE source_system_id=@source AND dataset_id=@goals);
DECLARE @filesContract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE source_system_id=@source AND dataset_id=@files);
DECLARE @glossaryContract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE source_system_id=@source AND dataset_id=@glossary);
IF NOT EXISTS (SELECT 1 FROM platform.contract_source WHERE contract_id=@goalsContract AND source_locator=N'dbo.goals') INSERT platform.contract_source(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json) VALUES(@goalsContract,N'dbo.goals','SQL_TABLE',@goalsVersion,N'id','ENTITY',100,N'{"approvalState":"DRAFT","family":"ENTITY","recordType":"KIDS_GOAL","key":"id","title":"title_geo"}');
IF NOT EXISTS (SELECT 1 FROM platform.contract_source WHERE contract_id=@filesContract AND source_locator=N'dbo.files') INSERT platform.contract_source(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json) VALUES(@filesContract,N'dbo.files','SQL_TABLE',@filesVersion,N'id','ENTITY',100,N'{"approvalState":"DRAFT","family":"ENTITY","recordType":"KIDS_FILE_RESOURCE","key":"id","title":"title_geo"}');
IF NOT EXISTS (SELECT 1 FROM platform.contract_source WHERE contract_id=@glossaryContract AND source_locator=N'dbo.glossary') INSERT platform.contract_source(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json) VALUES(@glossaryContract,N'dbo.glossary','SQL_TABLE',@glossaryVersion,N'id','ENTITY',100,N'{"approvalState":"DRAFT","family":"ENTITY","recordType":"KIDS_GLOSSARY_ENTRY","key":"id"}');

/* Preserve discovery evidence but make the old all-in-one contract impossible to execute. */
UPDATE c SET status='SUPERSEDED',auto_publish=0 FROM platform.ingestion_contract c JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE d.product_id=@product AND d.dataset_code=N'KIDS_CONTENT';
UPDATE cs SET active=0 FROM platform.contract_source cs JOIN platform.ingestion_contract c ON c.contract_id=cs.contract_id JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE d.product_id=@product AND d.dataset_code=N'KIDS_CONTENT';
