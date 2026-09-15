/* Revision 5 removes generated row JSON completely: immutable original Access artifact is the sole raw-byte authority. */
DECLARE @contract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE contract_code=N'KIDS_PORTAL_V1');
IF @contract IS NULL THROW 51000, 'KIDS canonical contract prerequisite is missing', 1;
DECLARE @spec TABLE(dataset_code NVARCHAR(120), source_locator NVARCHAR(1024), source_key NVARCHAR(1024), row_role VARCHAR(32), load_order INT);
INSERT @spec VALUES
(N'KIDS_CLASSIFIER_SCHEME',N'ACCESS.__cl_scheme',N'scheme_code','REFERENCE',10),(N'KIDS_CLASSIFIER_VERSION',N'ACCESS.__cl_version',N'version_ref','REFERENCE',20),(N'KIDS_CLASSIFIER_ITEM',N'ACCESS.__cl_item',N'item_ref','REFERENCE',30),(N'KIDS_CLASSIFIER_ALIAS',N'ACCESS.__cl_alias',N'alias_ref','REFERENCE',40),(N'KIDS_CLASSIFIER_HIERARCHY',N'ACCESS.__cl_hierarchy',N'child_item_ref','REFERENCE',50),(N'KIDS_RAW_DOCUMENT',N'ACCESS.__raw_document',N'source_row_key','RAW',60),(N'KIDS_GOAL',N'ACCESS.kids_goal',N'source_goal_id','ENTITY',100),(N'KIDS_RESOURCE',N'ACCESS.kids_resource',N'source_resource_id','ENTITY',110),(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',N'ACCESS.kids_resource_subcategory_assignment',N'assignment_key','RELATION',120),(N'KIDS_GLOSSARY_ENTRY',N'ACCESS.kids_glossary_entry',N'source_glossary_id','ENTITY',130),(N'KIDS_STATISTICAL_CARRIER',N'ACCESS.kids_statistical_carrier',N'carrier_code','RAW',140),(N'KIDS_STATISTICAL_INPUT',N'ACCESS.kids_statistical_input',N'input_key','STATISTICAL',150);
DECLARE @versions TABLE(dataset_code NVARCHAR(120) PRIMARY KEY, dataset_version_id BIGINT);
DECLARE @code NVARCHAR(120),@dataset BIGINT,@next INT,@version BIGINT;
DECLARE c CURSOR LOCAL FAST_FORWARD FOR SELECT dataset_code FROM @spec;
OPEN c; FETCH NEXT FROM c INTO @code;
WHILE @@FETCH_STATUS=0 BEGIN
  SELECT @dataset=dataset_id FROM platform.dataset WHERE dataset_code=@code AND product_id=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');
  IF @dataset IS NULL THROW 51001, 'KIDS canonical dataset prerequisite is missing', 1;
  SELECT @next=ISNULL(MAX(version),0)+1 FROM platform.dataset_version WHERE dataset_id=@dataset;
  INSERT platform.dataset_version(dataset_id,version,status,effective_from) VALUES(@dataset,@next,'DRAFT',SYSUTCDATETIME());
  SET @version=SCOPE_IDENTITY(); INSERT @versions VALUES(@code,@version);
  FETCH NEXT FROM c INTO @code;
END
CLOSE c; DEALLOCATE c;
UPDATE platform.contract_source SET active=0 WHERE contract_id=@contract;
INSERT platform.contract_source(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json,active)
SELECT @contract,s.source_locator,'ACCESS_TABLE',v.dataset_version_id,s.source_key,s.row_role,s.load_order,
 CONCAT(N'{"approvalState":"DRAFT","canonicalAccessRevision":5,"datasetCode":"',s.dataset_code,N'","rawArtifactAuthority":"IMMUTABLE_OBJECT_STORAGE","rowLocatorOnly":true}'),1
FROM @spec s JOIN @versions v ON v.dataset_code=s.dataset_code;
UPDATE platform.ingestion_contract SET contract_revision=5,status='REVIEW_REQUIRED',auto_publish=0,raw_ingest_enabled=1 WHERE contract_id=@contract;
