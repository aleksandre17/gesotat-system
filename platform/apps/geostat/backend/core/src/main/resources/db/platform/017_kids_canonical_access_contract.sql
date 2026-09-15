/* Canonical KIDS Access revision. This changes metadata only; legacy source tables and raw history remain untouched. */
DECLARE @product BIGINT=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');
DECLARE @contract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE contract_code=N'KIDS_PORTAL_V1');
IF @product IS NULL OR @contract IS NULL THROW 51000, 'KIDS canonical contract prerequisites are missing', 1;

DECLARE @spec TABLE(dataset_code NVARCHAR(120), family VARCHAR(24), grain NVARCHAR(500), source_locator NVARCHAR(1024), source_kind VARCHAR(32), source_key NVARCHAR(1024), row_role VARCHAR(32), load_order INT);
INSERT @spec VALUES
(N'KIDS_CLASSIFIER_SCHEME','REFERENCE',N'one classifier scheme',N'ACCESS.__cl_scheme','ACCESS_TABLE',N'scheme_code','REFERENCE',10),
(N'KIDS_CLASSIFIER_VERSION','REFERENCE',N'one classifier scheme version',N'ACCESS.__cl_version','ACCESS_TABLE',N'version_ref','REFERENCE',20),
(N'KIDS_CLASSIFIER_ITEM','REFERENCE',N'one versioned classifier item',N'ACCESS.__cl_item','ACCESS_TABLE',N'item_ref','REFERENCE',30),
(N'KIDS_CLASSIFIER_ALIAS','REFERENCE',N'one external classifier alias',N'ACCESS.__cl_alias','ACCESS_TABLE',N'alias_ref','REFERENCE',40),
(N'KIDS_CLASSIFIER_HIERARCHY','REFERENCE',N'one classifier parent child edge',N'ACCESS.__cl_hierarchy','ACCESS_TABLE',N'child_item_ref','REFERENCE',50),
(N'KIDS_RAW_DOCUMENT','RAW',N'one immutable original source row',N'ACCESS.__raw_document','ACCESS_TABLE',N'source_row_key','RAW',60),
(N'KIDS_GOAL','ENTITY',N'one KIDS goal',N'ACCESS.kids_goal','ACCESS_TABLE',N'source_goal_id','ENTITY',100),
(N'KIDS_RESOURCE','ENTITY',N'one KIDS file resource',N'ACCESS.kids_resource','ACCESS_TABLE',N'source_resource_id','ENTITY',110),
(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT','RELATION',N'one resource subcategory token',N'ACCESS.kids_resource_subcategory_assignment','ACCESS_TABLE',N'assignment_key','RELATION',120),
(N'KIDS_GLOSSARY_ENTRY','ENTITY',N'one independent glossary entry',N'ACCESS.kids_glossary_entry','ACCESS_TABLE',N'source_glossary_id','ENTITY',130),
(N'KIDS_STATISTICAL_CARRIER','RAW',N'one resource JSON carrier',N'ACCESS.kids_statistical_carrier','ACCESS_TABLE',N'carrier_code','RAW',140),
(N'KIDS_STATISTICAL_INPUT','STATISTICAL',N'one parsed source statistical input cell',N'ACCESS.kids_statistical_input','ACCESS_TABLE',N'input_key','STATISTICAL',150);

INSERT platform.dataset(product_id,dataset_code,dataset_family,business_grain,lifecycle_status)
SELECT @product,s.dataset_code,s.family,s.grain,'DRAFT' FROM @spec s
WHERE NOT EXISTS(SELECT 1 FROM platform.dataset d WHERE d.product_id=@product AND d.dataset_code=s.dataset_code);

DECLARE @versions TABLE(dataset_code NVARCHAR(120) PRIMARY KEY, dataset_version_id BIGINT);
DECLARE @code NVARCHAR(120), @dataset BIGINT, @next INT, @version BIGINT;
DECLARE c CURSOR LOCAL FAST_FORWARD FOR SELECT s.dataset_code FROM @spec s;
OPEN c; FETCH NEXT FROM c INTO @code;
WHILE @@FETCH_STATUS=0 BEGIN
  SELECT @dataset=dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=@code;
  SELECT @next=ISNULL(MAX(version),0)+1 FROM platform.dataset_version WHERE dataset_id=@dataset;
  INSERT platform.dataset_version(dataset_id,version,status,effective_from) VALUES(@dataset,@next,'DRAFT',SYSUTCDATETIME());
  SET @version=SCOPE_IDENTITY(); INSERT @versions VALUES(@code,@version);
  FETCH NEXT FROM c INTO @code;
END
CLOSE c; DEALLOCATE c;

UPDATE platform.contract_source SET active=0 WHERE contract_id=@contract;
INSERT platform.contract_source(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json,active)
SELECT @contract,s.source_locator,s.source_kind,v.dataset_version_id,s.source_key,s.row_role,s.load_order,
  CONCAT(N'{"approvalState":"DRAFT","canonicalAccessRevision":3,"datasetCode":"',s.dataset_code,N'","rawLineageRequired":true}'),1
FROM @spec s JOIN @versions v ON v.dataset_code=s.dataset_code;

UPDATE platform.ingestion_contract SET contract_revision=3,status='REVIEW_REQUIRED',auto_publish=0,raw_ingest_enabled=1 WHERE contract_id=@contract;
