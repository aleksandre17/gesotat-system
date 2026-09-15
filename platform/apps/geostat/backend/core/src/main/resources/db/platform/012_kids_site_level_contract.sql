/* One Control-Plane-first, multi-dataset Kids site contract. No source data is read or changed. */
DECLARE @product BIGINT=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');
DECLARE @source BIGINT=(SELECT source_system_id FROM platform.source_system WHERE source_code=N'KIDS_LEGACY_SQL');

IF NOT EXISTS (SELECT 1 FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_GOAL_CATEGORY_REFERENCE')
  INSERT platform.dataset(product_id,dataset_code,dataset_family,business_grain,lifecycle_status) VALUES(@product,N'KIDS_GOAL_CATEGORY_REFERENCE','REFERENCE',N'ერთი წყაროს goal-category code/label (goals_titles.ID)','DRAFT');
DECLARE @categoryDataset BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_GOAL_CATEGORY_REFERENCE');
IF NOT EXISTS (SELECT 1 FROM platform.dataset_version WHERE dataset_id=@categoryDataset AND version=1)
  INSERT platform.dataset_version(dataset_id,version,status) VALUES(@categoryDataset,1,'DRAFT');
DECLARE @categoryVersion BIGINT=(SELECT dataset_version_id FROM platform.dataset_version WHERE dataset_id=@categoryDataset AND version=1);

DECLARE @goalDataset BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_GOAL');
DECLARE @fileDataset BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_FILE_RESOURCE');
DECLARE @glossaryDataset BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_GLOSSARY_ENTRY');
DECLARE @goalVersion BIGINT=(SELECT dataset_version_id FROM platform.dataset_version WHERE dataset_id=@goalDataset AND version=1);
DECLARE @fileVersion BIGINT=(SELECT dataset_version_id FROM platform.dataset_version WHERE dataset_id=@fileDataset AND version=1);
DECLARE @glossaryVersion BIGINT=(SELECT dataset_version_id FROM platform.dataset_version WHERE dataset_id=@glossaryDataset AND version=1);

/* Canonical field catalogue: source spellings remain only in mapping_spec_json and immutable raw payload. */
DECLARE @attributes TABLE(version_id BIGINT,code NVARCHAR(128),logical_type VARCHAR(24),role VARCHAR(24),cardinality VARCHAR(16),required BIT,label_ka NVARCHAR(255),label_en NVARCHAR(255),json_path NVARCHAR(512));
INSERT @attributes VALUES
(@categoryVersion,N'SOURCE_CODE','CODE','IDENTIFIER','SINGLE',1,N'წყაროს კოდი',N'Source code',N'ID'),
(@categoryVersion,N'CATEGORY_CODE','CLASSIFICATION','DIMENSION','SINGLE',1,N'კატეგორიის კოდი',N'Category code',N'category'),
(@categoryVersion,N'TITLE','TEXT','LOCALIZED_TEXT','MULTIPLE',1,N'დასახელება',N'Title',N'title_*'),
(@goalVersion,N'SOURCE_ID','INTEGER','IDENTIFIER','SINGLE',1,N'წყაროს იდენტიფიკატორი',N'Source identifier',N'ID'),
(@goalVersion,N'GOAL_CATEGORY','CLASSIFICATION','DIMENSION','SINGLE',1,N'მდგრადი განვითარების მიზანი',N'Sustainable development goal',N'category'),
(@goalVersion,N'TITLE','TEXT','LOCALIZED_TEXT','MULTIPLE',1,N'დასახელება',N'Title',N'title_*'),
(@goalVersion,N'LOCATOR','URI','LOCATOR','MULTIPLE',0,N'მისამართი',N'Locator',N'path_*'),
(@fileVersion,N'SOURCE_ID','INTEGER','IDENTIFIER','SINGLE',1,N'წყაროს იდენტიფიკატორი',N'Source identifier',N'ID'),
(@fileVersion,N'GOAL_CATEGORY','CLASSIFICATION','DIMENSION','SINGLE',1,N'მდგრადი განვითარების მიზანი',N'Sustainable development goal',N'category'),
(@fileVersion,N'TITLE','TEXT','LOCALIZED_TEXT','MULTIPLE',1,N'დასახელება',N'Title',N'title_*'),
(@fileVersion,N'LOCATOR','URI','LOCATOR','MULTIPLE',0,N'მისამართი',N'Locator',N'path_*'),
(@fileVersion,N'SUBCATEGORY_RAW','TEXT','RAW_PAYLOAD','SINGLE',0,N'წყაროს ქვეკატეგორია',N'Raw source sub-category',N'sub_category'),
(@fileVersion,N'CHARTDATA_RAW','JSON','RAW_PAYLOAD','SINGLE',0,N'წყაროს chart მონაცემი',N'Raw chart data',N'chartdata'),
(@glossaryVersion,N'SOURCE_ID','INTEGER','IDENTIFIER','SINGLE',1,N'წყაროს იდენტიფიკატორი',N'Source identifier',N'ID'),
(@glossaryVersion,N'LANGUAGE','CLASSIFICATION','DIMENSION','SINGLE',1,N'ენა',N'Language',N'lang'),
(@glossaryVersion,N'BODY','TEXT','LOCALIZED_TEXT','SINGLE',1,N'ტექსტი',N'Body text',N'text');
INSERT platform.attribute(dataset_version_id,attribute_code,logical_type,attribute_role,cardinality,required,filterable,label_ka,label_en,json_path)
SELECT version_id,code,logical_type,role,cardinality,required,1,label_ka,label_en,json_path FROM @attributes a
WHERE NOT EXISTS (SELECT 1 FROM platform.attribute p WHERE p.dataset_version_id=a.version_id AND p.attribute_code=a.code);

IF NOT EXISTS (SELECT 1 FROM platform.relationship_type WHERE code=N'CLASSIFIED_BY')
  INSERT platform.relationship_type(code,category,directional,transitive,temporal,description) VALUES(N'CLASSIFIED_BY','SEMANTIC',1,0,0,N'Entity or dataset membership in a governed classification vocabulary');
DECLARE @classifiedBy BIGINT=(SELECT relationship_type_id FROM platform.relationship_type WHERE code=N'CLASSIFIED_BY');
DECLARE @goalCategoryAttr BIGINT=(SELECT attribute_id FROM platform.attribute WHERE dataset_version_id=@goalVersion AND attribute_code=N'GOAL_CATEGORY');
DECLARE @fileCategoryAttr BIGINT=(SELECT attribute_id FROM platform.attribute WHERE dataset_version_id=@fileVersion AND attribute_code=N'GOAL_CATEGORY');
DECLARE @categoryCodeAttr BIGINT=(SELECT attribute_id FROM platform.attribute WHERE dataset_version_id=@categoryVersion AND attribute_code=N'CATEGORY_CODE');
IF NOT EXISTS (SELECT 1 FROM platform.dataset_relationship WHERE from_dataset_version_id=@goalVersion AND to_dataset_version_id=@categoryVersion AND relationship_type_id=@classifiedBy)
  INSERT platform.dataset_relationship(from_dataset_version_id,to_dataset_version_id,relationship_type_id,source_attribute_id,target_attribute_id,cardinality,required,enforcement_policy,load_priority) VALUES(@goalVersion,@categoryVersion,@classifiedBy,@goalCategoryAttr,@categoryCodeAttr,'MANY_TO_ONE',1,'VALIDATE',10);
IF NOT EXISTS (SELECT 1 FROM platform.dataset_relationship WHERE from_dataset_version_id=@fileVersion AND to_dataset_version_id=@categoryVersion AND relationship_type_id=@classifiedBy)
  INSERT platform.dataset_relationship(from_dataset_version_id,to_dataset_version_id,relationship_type_id,source_attribute_id,target_attribute_id,cardinality,required,enforcement_policy,load_priority) VALUES(@fileVersion,@categoryVersion,@classifiedBy,@fileCategoryAttr,@categoryCodeAttr,'MANY_TO_ONE',1,'VALIDATE',10);

/* The contract anchor is a product dataset only because the existing generic model requires one; the active sources span all four datasets. */
IF NOT EXISTS (SELECT 1 FROM platform.ingestion_contract WHERE source_system_id=@source AND dataset_id=@fileDataset)
  INSERT platform.ingestion_contract(source_system_id,dataset_id,format_profile,ingestion_method,auto_publish,status) VALUES(@source,@fileDataset,'ACCESS_SEMANTIC_V3_OR_SQL','SCHEDULED',0,'REVIEW_REQUIRED');
DECLARE @contract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE source_system_id=@source AND dataset_id=@fileDataset);
UPDATE platform.ingestion_contract SET contract_code=N'KIDS_PORTAL_V1',contract_revision=1,status='REVIEW_REQUIRED',auto_publish=0 WHERE contract_id=@contract;

/* Supersede the old per-dataset draft contracts; this contract is the only future Kids import entry point. */
UPDATE c SET status='SUPERSEDED',auto_publish=0 FROM platform.ingestion_contract c JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE c.source_system_id=@source AND d.product_id=@product AND d.dataset_code IN(N'KIDS_GOAL',N'KIDS_GLOSSARY_ENTRY');
UPDATE cs SET active=0 FROM platform.contract_source cs JOIN platform.ingestion_contract c ON c.contract_id=cs.contract_id JOIN platform.dataset d ON d.dataset_id=c.dataset_id WHERE c.source_system_id=@source AND d.product_id=@product AND d.dataset_code IN(N'KIDS_GOAL',N'KIDS_GLOSSARY_ENTRY');

MERGE platform.contract_source AS target
USING (VALUES
 (N'dbo.goals_titles',N'SQL_TABLE',@categoryVersion,N'ID',N'REFERENCE',10,N'{"approvalState":"DRAFT","projections":[{"family":"REFERENCE","sourceKey":"ID","classificationSchemeCode":"KIDS_GOAL_CATEGORY","categoryCodeField":"category","localizedLabels":[{"field":"title_geo","language":"ka"},{"field":"title_eng","language":"en"}]}]}'),
 (N'dbo.goals',N'SQL_TABLE',@goalVersion,N'ID',N'ENTITY',20,N'{"approvalState":"DRAFT","projections":[{"family":"ENTITY","recordType":"KIDS_GOAL","key":"ID","classification":{"field":"category","schemeCode":"KIDS_GOAL_CATEGORY"},"localizedText":[{"field":"title_geo","language":"ka"},{"field":"title_eng","language":"en"}],"locators":[{"field":"path_geo","language":"ka"},{"field":"path_eng","language":"en"}]}]}'),
 (N'dbo.files',N'SQL_TABLE',@fileVersion,N'ID',N'ENTITY',30,N'{"approvalState":"DRAFT","projections":[{"family":"ENTITY","recordType":"KIDS_FILE_RESOURCE","key":"ID","classification":{"field":"category","schemeCode":"KIDS_GOAL_CATEGORY"},"localizedText":[{"field":"title_geo","language":"ka"},{"field":"title_eng","language":"en"}],"locators":[{"field":"path_geo","language":"ka"},{"field":"path_eng","language":"en"}]},{"family":"STATISTICAL_WIDE_JSON","status":"SEMANTIC_REVIEW_REQUIRED","payloadField":"chartdata"}]}'),
 (N'dbo.glossary',N'SQL_TABLE',@glossaryVersion,N'ID',N'ENTITY',40,N'{"approvalState":"DRAFT","projections":[{"family":"ENTITY","recordType":"KIDS_GLOSSARY_ENTRY","key":"ID","languageField":"lang","bodyField":"text","languageSchemeCode":"LANGUAGE"}]}')
) AS source(source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json)
ON target.contract_id=@contract AND target.source_locator=source.source_locator
WHEN MATCHED THEN UPDATE SET source_kind=source.source_kind,target_dataset_version_id=source.target_dataset_version_id,source_key_expression=source.source_key_expression,row_role=source.row_role,load_order=source.load_order,mapping_spec_json=source.mapping_spec_json,active=1
WHEN NOT MATCHED THEN INSERT(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json) VALUES(@contract,source.source_locator,source.source_kind,source.target_dataset_version_id,source.source_key_expression,source.row_role,source.load_order,source.mapping_spec_json);
