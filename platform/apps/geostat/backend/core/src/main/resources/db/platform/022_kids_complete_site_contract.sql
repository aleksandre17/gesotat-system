/*
 * KIDS_PORTAL_V1 revision 6 — complete, versioned site contract.
 * Additive only: revisions 1-5 and their artifacts remain reproducible.
 * This migration is the authority for hierarchy, datasets, fields, relations,
 * classifiers, SDMX-compatible structure, validation gates and Access bindings.
 */
IF OBJECT_ID(N'platform.site_contract_revision',N'U') IS NULL CREATE TABLE platform.site_contract_revision(
  site_contract_revision_id BIGINT IDENTITY PRIMARY KEY, product_id BIGINT NOT NULL,
  contract_code NVARCHAR(120) NOT NULL, revision INT NOT NULL, parent_revision_id BIGINT NULL,
  schema_standard NVARCHAR(120) NOT NULL, compatibility_mode VARCHAR(32) NOT NULL,
  status VARCHAR(24) NOT NULL, contract_checksum CHAR(64) NOT NULL, contract_document_json NVARCHAR(MAX) NOT NULL,
  effective_from DATETIME2 NULL, approved_by_user_id BIGINT NULL, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_site_contract_revision UNIQUE(contract_code,revision),
  CONSTRAINT fk_site_contract_product FOREIGN KEY(product_id) REFERENCES platform.data_product(product_id),
  CONSTRAINT fk_site_contract_parent FOREIGN KEY(parent_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id),
  CONSTRAINT ck_site_contract_compat CHECK(compatibility_mode IN('BACKWARD_COMPATIBLE','BREAKING_NEW_REVISION')),
  CONSTRAINT ck_site_contract_status CHECK(status IN('DRAFT','REVIEW_REQUIRED','APPROVED','SUPERSEDED')),
  CONSTRAINT ck_site_contract_document_json CHECK(ISJSON(contract_document_json)=1)
);
IF OBJECT_ID(N'platform.site_contract_node',N'U') IS NULL CREATE TABLE platform.site_contract_node(
  node_id BIGINT IDENTITY PRIMARY KEY, site_contract_revision_id BIGINT NOT NULL, node_code NVARCHAR(120) NOT NULL,
  parent_node_id BIGINT NULL, node_kind VARCHAR(32) NOT NULL, dataset_code NVARCHAR(120) NULL,
  path_segment NVARCHAR(255) NOT NULL, sort_order INT NOT NULL, required BIT NOT NULL DEFAULT 1,
  title_ka NVARCHAR(255) NOT NULL, title_en NVARCHAR(255) NULL,
  CONSTRAINT uq_site_contract_node UNIQUE(site_contract_revision_id,node_code),
  CONSTRAINT fk_site_contract_node_revision FOREIGN KEY(site_contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id),
  CONSTRAINT fk_site_contract_node_parent FOREIGN KEY(parent_node_id) REFERENCES platform.site_contract_node(node_id)
);
IF OBJECT_ID(N'platform.site_contract_dataset',N'U') IS NULL CREATE TABLE platform.site_contract_dataset(
  contract_dataset_id BIGINT IDENTITY PRIMARY KEY, site_contract_revision_id BIGINT NOT NULL,
  dataset_version_id BIGINT NOT NULL, dataset_code NVARCHAR(120) NOT NULL, access_table_name NVARCHAR(128) NOT NULL,
  data_family VARCHAR(24) NOT NULL, business_grain NVARCHAR(500) NOT NULL, natural_key_expression NVARCHAR(1000) NOT NULL,
  row_role VARCHAR(32) NOT NULL, load_order INT NOT NULL, required BIT NOT NULL DEFAULT 1,
  CONSTRAINT uq_site_contract_dataset UNIQUE(site_contract_revision_id,dataset_code),
  CONSTRAINT uq_site_contract_access_table UNIQUE(site_contract_revision_id,access_table_name),
  CONSTRAINT fk_site_contract_dataset_revision FOREIGN KEY(site_contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id),
  CONSTRAINT fk_site_contract_dataset_version FOREIGN KEY(dataset_version_id) REFERENCES platform.dataset_version(dataset_version_id)
);
IF OBJECT_ID(N'platform.site_contract_field',N'U') IS NULL CREATE TABLE platform.site_contract_field(
  contract_field_id BIGINT IDENTITY PRIMARY KEY, contract_dataset_id BIGINT NOT NULL, field_name NVARCHAR(128) NOT NULL,
  logical_type VARCHAR(24) NOT NULL, semantic_role VARCHAR(32) NOT NULL, ordinal INT NOT NULL,
  required BIT NOT NULL, key_role VARCHAR(24) NULL, classifier_scheme_code NVARCHAR(120) NULL,
  source_expression NVARCHAR(1000) NULL, normalization_rule NVARCHAR(120) NULL,
  CONSTRAINT uq_site_contract_field UNIQUE(contract_dataset_id,field_name),
  CONSTRAINT uq_site_contract_field_ordinal UNIQUE(contract_dataset_id,ordinal),
  CONSTRAINT fk_site_contract_field_dataset FOREIGN KEY(contract_dataset_id) REFERENCES platform.site_contract_dataset(contract_dataset_id)
);
IF OBJECT_ID(N'platform.site_contract_relation',N'U') IS NULL CREATE TABLE platform.site_contract_relation(
  contract_relation_id BIGINT IDENTITY PRIMARY KEY, site_contract_revision_id BIGINT NOT NULL,
  relation_code NVARCHAR(120) NOT NULL, from_dataset_code NVARCHAR(120) NOT NULL, from_field_name NVARCHAR(128) NOT NULL,
  to_dataset_code NVARCHAR(120) NOT NULL, to_field_name NVARCHAR(128) NOT NULL,
  relation_kind VARCHAR(32) NOT NULL, cardinality VARCHAR(24) NOT NULL, required BIT NOT NULL,
  enforcement_policy VARCHAR(24) NOT NULL, load_priority INT NOT NULL,
  CONSTRAINT uq_site_contract_relation UNIQUE(site_contract_revision_id,relation_code),
  CONSTRAINT fk_site_contract_relation_revision FOREIGN KEY(site_contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id)
);
IF OBJECT_ID(N'platform.site_contract_classifier',N'U') IS NULL CREATE TABLE platform.site_contract_classifier(
  contract_classifier_id BIGINT IDENTITY PRIMARY KEY, site_contract_revision_id BIGINT NOT NULL,
  scheme_code NVARCHAR(120) NOT NULL, version_policy VARCHAR(32) NOT NULL, authority_mode VARCHAR(32) NOT NULL,
  unknown_value_policy VARCHAR(32) NOT NULL, publication_policy VARCHAR(32) NOT NULL,
  CONSTRAINT uq_site_contract_classifier UNIQUE(site_contract_revision_id,scheme_code),
  CONSTRAINT fk_site_contract_classifier_revision FOREIGN KEY(site_contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id)
);
IF OBJECT_ID(N'platform.statistical_dataflow',N'U') IS NULL CREATE TABLE platform.statistical_dataflow(
  dataflow_id BIGINT IDENTITY PRIMARY KEY, product_id BIGINT NOT NULL, dataflow_code NVARCHAR(120) NOT NULL,
  source_dataset_id BIGINT NOT NULL, title_ka NVARCHAR(255) NOT NULL, title_en NVARCHAR(255) NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'DRAFT', CONSTRAINT uq_stat_dataflow UNIQUE(product_id,dataflow_code),
  CONSTRAINT fk_stat_dataflow_product FOREIGN KEY(product_id) REFERENCES platform.data_product(product_id),
  CONSTRAINT fk_stat_dataflow_dataset FOREIGN KEY(source_dataset_id) REFERENCES platform.dataset(dataset_id)
);
IF OBJECT_ID(N'platform.statistical_dsd',N'U') IS NULL CREATE TABLE platform.statistical_dsd(
  dsd_id BIGINT IDENTITY PRIMARY KEY, dataflow_id BIGINT NOT NULL, dsd_code NVARCHAR(120) NOT NULL, revision INT NOT NULL,
  observation_grain NVARCHAR(1000) NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'DRAFT', valid_from DATE NULL, valid_to DATE NULL,
  CONSTRAINT uq_stat_dsd UNIQUE(dataflow_id,dsd_code,revision),
  CONSTRAINT fk_stat_dsd_dataflow FOREIGN KEY(dataflow_id) REFERENCES platform.statistical_dataflow(dataflow_id)
);
IF OBJECT_ID(N'platform.statistical_component',N'U') IS NULL CREATE TABLE platform.statistical_component(
  component_id BIGINT IDENTITY PRIMARY KEY, dsd_id BIGINT NOT NULL, component_code NVARCHAR(120) NOT NULL,
  component_role VARCHAR(24) NOT NULL, component_order INT NOT NULL, required BIT NOT NULL,
  dimension_id BIGINT NULL, measure_id BIGINT NULL, classification_version_id BIGINT NULL,
  attachment_level VARCHAR(24) NOT NULL, source_field NVARCHAR(128) NULL, normalized_field NVARCHAR(128) NULL,
  constraint_json NVARCHAR(MAX) NULL, status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  CONSTRAINT uq_stat_component UNIQUE(dsd_id,component_code), CONSTRAINT uq_stat_component_order UNIQUE(dsd_id,component_order),
  CONSTRAINT fk_stat_component_dsd FOREIGN KEY(dsd_id) REFERENCES platform.statistical_dsd(dsd_id),
  CONSTRAINT fk_stat_component_dimension FOREIGN KEY(dimension_id) REFERENCES platform.dimension(dimension_id),
  CONSTRAINT fk_stat_component_measure FOREIGN KEY(measure_id) REFERENCES platform.measure(measure_id),
  CONSTRAINT fk_stat_component_classification FOREIGN KEY(classification_version_id) REFERENCES platform.classification_version(classification_version_id),
  CONSTRAINT ck_stat_component_constraint_json CHECK(constraint_json IS NULL OR ISJSON(constraint_json)=1)
);
IF OBJECT_ID(N'platform.site_contract_gate',N'U') IS NULL CREATE TABLE platform.site_contract_gate(
  gate_id BIGINT IDENTITY PRIMARY KEY, site_contract_revision_id BIGINT NOT NULL, gate_code NVARCHAR(120) NOT NULL,
  gate_type VARCHAR(32) NOT NULL, severity VARCHAR(16) NOT NULL, blocking BIT NOT NULL,
  rule_json NVARCHAR(MAX) NOT NULL, CONSTRAINT uq_site_contract_gate UNIQUE(site_contract_revision_id,gate_code),
  CONSTRAINT fk_site_contract_gate_revision FOREIGN KEY(site_contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id),
  CONSTRAINT ck_site_contract_gate_json CHECK(ISJSON(rule_json)=1)
);
IF OBJECT_ID(N'platform.ingestion_contract_revision',N'U') IS NULL CREATE TABLE platform.ingestion_contract_revision(
  ingestion_contract_revision_id BIGINT IDENTITY PRIMARY KEY, contract_id BIGINT NOT NULL, revision INT NOT NULL,
  format_profile VARCHAR(48) NOT NULL, lifecycle_status VARCHAR(24) NOT NULL, compatibility_mode VARCHAR(32) NOT NULL,
  captured_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_ingestion_contract_revision UNIQUE(contract_id,revision),
  CONSTRAINT fk_ingestion_contract_revision_contract FOREIGN KEY(contract_id) REFERENCES platform.ingestion_contract(contract_id),
  CONSTRAINT ck_ingestion_contract_revision_compat CHECK(compatibility_mode IN('BACKWARD_COMPATIBLE','BREAKING_NEW_REVISION'))
);
IF OBJECT_ID(N'platform.contract_revision_source',N'U') IS NULL CREATE TABLE platform.contract_revision_source(
  contract_revision_source_id BIGINT IDENTITY PRIMARY KEY, ingestion_contract_revision_id BIGINT NOT NULL,
  source_locator NVARCHAR(1024) NOT NULL, source_kind VARCHAR(32) NOT NULL, target_dataset_version_id BIGINT NOT NULL,
  source_key_expression NVARCHAR(1024) NULL, row_role VARCHAR(32) NOT NULL, load_order INT NOT NULL,
  mapping_spec_json NVARCHAR(MAX) NOT NULL,
  CONSTRAINT uq_contract_revision_source UNIQUE(ingestion_contract_revision_id,source_locator),
  CONSTRAINT fk_contract_revision_source_revision FOREIGN KEY(ingestion_contract_revision_id) REFERENCES platform.ingestion_contract_revision(ingestion_contract_revision_id),
  CONSTRAINT fk_contract_revision_source_dataset_version FOREIGN KEY(target_dataset_version_id) REFERENCES platform.dataset_version(dataset_version_id),
  CONSTRAINT ck_contract_revision_source_mapping CHECK(ISJSON(mapping_spec_json)=1)
);

DECLARE @product BIGINT=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');
DECLARE @contract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE contract_code=N'KIDS_PORTAL_V1');
IF @product IS NULL OR @contract IS NULL THROW 51000,'KIDS contract prerequisites are missing',1;

/* Preserve the previously issued executable revision before switching the active pointer. */
IF NOT EXISTS(SELECT 1 FROM platform.ingestion_contract_revision WHERE contract_id=@contract AND revision=5)
 INSERT platform.ingestion_contract_revision(contract_id,revision,format_profile,lifecycle_status,compatibility_mode)
 SELECT contract_id,5,format_profile,status,'BACKWARD_COMPATIBLE' FROM platform.ingestion_contract WHERE contract_id=@contract AND contract_revision=5;
DECLARE @ingestionR5 BIGINT=(SELECT ingestion_contract_revision_id FROM platform.ingestion_contract_revision WHERE contract_id=@contract AND revision=5);
IF @ingestionR5 IS NOT NULL INSERT platform.contract_revision_source(ingestion_contract_revision_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json)
SELECT @ingestionR5,cs.source_locator,cs.source_kind,cs.target_dataset_version_id,cs.source_key_expression,cs.row_role,cs.load_order,cs.mapping_spec_json
FROM platform.contract_source cs WHERE cs.contract_id=@contract AND cs.active=1
AND NOT EXISTS(SELECT 1 FROM platform.contract_revision_source x WHERE x.ingestion_contract_revision_id=@ingestionR5 AND x.source_locator=cs.source_locator);

/* Classifier authorities. Source values enter as proposals; migration never invents source items. */
MERGE platform.classification_scheme t USING(VALUES
 (N'KIDS_GOAL_CATEGORY',N'KIDS მიზნის კატეგორია',N'KIDS goal category',N'UN SDG crosswalk candidate'),
 (N'LANGUAGE',N'ენა',N'Language',N'IETF BCP 47 / ISO 639'),
 (N'KIDS_RESOURCE_SUBCATEGORY',N'KIDS რესურსის ქვეკატეგორია',N'KIDS resource subcategory',N'KIDS source-managed vocabulary'),
 (N'AGE_GROUP',N'ასაკობრივი ჯგუფი',N'Age group',N'SDMX codelist proposal')
)s(code,ka,en,std) ON t.scheme_code=s.code
WHEN MATCHED THEN UPDATE SET title_ka=s.ka,title_en=s.en,standard_reference=s.std
WHEN NOT MATCHED THEN INSERT(scheme_code,title_ka,title_en,standard_reference)VALUES(s.code,s.ka,s.en,s.std);
DECLARE @schemeCode NVARCHAR(120),@schemeId BIGINT;
DECLARE scheme_cursor CURSOR LOCAL FAST_FORWARD FOR SELECT scheme_code FROM platform.classification_scheme WHERE scheme_code IN(N'KIDS_GOAL_CATEGORY',N'LANGUAGE',N'KIDS_RESOURCE_SUBCATEGORY',N'AGE_GROUP');
OPEN scheme_cursor;FETCH NEXT FROM scheme_cursor INTO @schemeCode;
WHILE @@FETCH_STATUS=0 BEGIN SELECT @schemeId=scheme_id FROM platform.classification_scheme WHERE scheme_code=@schemeCode;
 IF NOT EXISTS(SELECT 1 FROM platform.classification_version WHERE scheme_id=@schemeId AND version=N'KIDS_PORTAL_V1_R6') INSERT platform.classification_version(scheme_id,version,status,valid_from)VALUES(@schemeId,N'KIDS_PORTAL_V1_R6','DRAFT','2026-09-10');
 FETCH NEXT FROM scheme_cursor INTO @schemeCode;END CLOSE scheme_cursor;DEALLOCATE scheme_cursor;

DECLARE @ageScheme BIGINT=(SELECT scheme_id FROM platform.classification_scheme WHERE scheme_code=N'AGE_GROUP');
DECLARE @goalScheme BIGINT=(SELECT scheme_id FROM platform.classification_scheme WHERE scheme_code=N'KIDS_GOAL_CATEGORY');
IF NOT EXISTS(SELECT 1 FROM platform.dimension WHERE dimension_code=N'TIME_PERIOD') INSERT platform.dimension(dimension_code,value_type,title_ka,title_en)VALUES(N'TIME_PERIOD','YEAR',N'საანგარიშო პერიოდი',N'Time period');
IF NOT EXISTS(SELECT 1 FROM platform.dimension WHERE dimension_code=N'AGE_GROUP') INSERT platform.dimension(dimension_code,classification_scheme_id,value_type,title_ka,title_en)VALUES(N'AGE_GROUP',@ageScheme,'CLASSIFICATION',N'ასაკობრივი ჯგუფი',N'Age group'); ELSE UPDATE platform.dimension SET classification_scheme_id=@ageScheme,value_type='CLASSIFICATION' WHERE dimension_code=N'AGE_GROUP';
IF NOT EXISTS(SELECT 1 FROM platform.dimension WHERE dimension_code=N'GOAL_CATEGORY') INSERT platform.dimension(dimension_code,classification_scheme_id,value_type,title_ka,title_en)VALUES(N'GOAL_CATEGORY',@goalScheme,'CLASSIFICATION',N'მიზნის კატეგორია',N'Goal category');
IF NOT EXISTS(SELECT 1 FROM platform.measure WHERE measure_code=N'KIDS_OBS_VALUE_INPUT') INSERT platform.measure(measure_code,value_type,unit_code,decimal_precision,aggregation_default,title_ka,title_en)VALUES(N'KIDS_OBS_VALUE_INPUT','DECIMAL',NULL,28,'NONE',N'KIDS დაუმტკიცებელი დაკვირვების მნიშვნელობა',N'KIDS unapproved observation value');
/* Revision 6 does not reuse the old generic SUM measure or its hard-coded file metrics. */
UPDATE m SET status='SUPERSEDED'
FROM platform.metric m JOIN platform.dataset d ON d.dataset_id=m.source_dataset_id
WHERE d.product_id=@product AND m.metric_code LIKE N'KIDS_FILE_%' AND m.status<>N'SUPERSEDED';

/* Immutable dataset versions for revision 6. */
DECLARE @spec TABLE(code NVARCHAR(120),family VARCHAR(24),grain NVARCHAR(500),access_table NVARCHAR(128),natural_key NVARCHAR(1000),row_role VARCHAR(32),load_order INT);
INSERT @spec VALUES
(N'KIDS_CLASSIFIER_SCHEME','REFERENCE',N'one classifier scheme',N'__cl_scheme',N'scheme_code','REFERENCE',10),(N'KIDS_CLASSIFIER_VERSION','REFERENCE',N'one classifier version',N'__cl_version',N'version_ref','REFERENCE',20),(N'KIDS_CLASSIFIER_ITEM','REFERENCE',N'one versioned classifier item',N'__cl_item',N'item_ref','REFERENCE',30),(N'KIDS_CLASSIFIER_ALIAS','REFERENCE',N'one source alias',N'__cl_alias',N'alias_ref','REFERENCE',40),(N'KIDS_CLASSIFIER_HIERARCHY','REFERENCE',N'one parent-child edge',N'__cl_hierarchy',N'child_item_ref','REFERENCE',50),(N'KIDS_RAW_DOCUMENT','RAW',N'one locator into an immutable source artifact',N'__raw_document',N'source_row_key','RAW',60),(N'KIDS_GOAL','ENTITY',N'one KIDS goal',N'kids_goal',N'source_goal_id','ENTITY',100),(N'KIDS_RESOURCE','ENTITY',N'one KIDS resource',N'kids_resource',N'source_resource_id','ENTITY',110),(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT','RELATION',N'one resource × subcategory token',N'kids_resource_subcategory_assignment',N'assignment_key','RELATION',120),(N'KIDS_GLOSSARY_ENTRY','ENTITY',N'one independent glossary entry',N'kids_glossary_entry',N'source_glossary_id','ENTITY',130),(N'KIDS_STATISTICAL_CARRIER','RAW',N'one resource-bound statistical carrier locator',N'kids_statistical_carrier',N'carrier_code','RAW',140),(N'KIDS_STATISTICAL_INPUT','STATISTICAL',N'one carrier × source cell',N'kids_statistical_input',N'input_key','STATISTICAL',150);
INSERT platform.dataset(product_id,dataset_code,dataset_family,business_grain,lifecycle_status) SELECT @product,s.code,s.family,s.grain,'DRAFT' FROM @spec s WHERE NOT EXISTS(SELECT 1 FROM platform.dataset d WHERE d.product_id=@product AND d.dataset_code=s.code);
DECLARE @code NVARCHAR(120),@dataset BIGINT;
DECLARE dataset_cursor CURSOR LOCAL FAST_FORWARD FOR SELECT code FROM @spec;OPEN dataset_cursor;FETCH NEXT FROM dataset_cursor INTO @code;
WHILE @@FETCH_STATUS=0 BEGIN SELECT @dataset=dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=@code;IF NOT EXISTS(SELECT 1 FROM platform.dataset_version WHERE dataset_id=@dataset AND version=6)INSERT platform.dataset_version(dataset_id,version,status,effective_from)VALUES(@dataset,6,'DRAFT',SYSUTCDATETIME());FETCH NEXT FROM dataset_cursor INTO @code;END CLOSE dataset_cursor;DEALLOCATE dataset_cursor;

DECLARE @doc NVARCHAR(MAX)=N'{"standard":"ISO_IEC_11179_SDMX_COMPATIBLE","product":"KIDS_PORTAL","contract":"KIDS_PORTAL_V1","revision":6,"accessProfile":"ACCESS_CANONICAL_R6","rawAuthority":"IMMUTABLE_OBJECT_STORAGE","families":["RAW","REFERENCE","ENTITY","RELATION","STATISTICAL"],"statisticalModel":{"dataflow":"KIDS_FILES_STATISTICS","dsd":"KIDS_FILES_STATISTICS_DSD","revision":1,"grain":["carrier_code","cell_ordinal"],"dimensions":["TIME_PERIOD","AGE_GROUP"],"primaryMeasureComponent":"OBS_VALUE","measureRegistryCode":"KIDS_OBS_VALUE_INPUT"},"publication":"BLOCK_UNTIL_ALL_SEMANTICS_APPROVED"}';
DECLARE @parent BIGINT=(SELECT site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=5);
IF NOT EXISTS(SELECT 1 FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=6) INSERT platform.site_contract_revision(product_id,contract_code,revision,parent_revision_id,schema_standard,compatibility_mode,status,contract_checksum,contract_document_json)VALUES(@product,N'KIDS_PORTAL_V1',6,@parent,N'ISO/IEC 11179 + SDMX information model',N'BACKWARD_COMPATIBLE','REVIEW_REQUIRED',CONVERT(VARCHAR(64),HASHBYTES('SHA2_256',CONVERT(VARBINARY(MAX),@doc)),2),@doc);
DECLARE @siteContract BIGINT=(SELECT site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=6);

INSERT platform.site_contract_dataset(site_contract_revision_id,dataset_version_id,dataset_code,access_table_name,data_family,business_grain,natural_key_expression,row_role,load_order,required)
SELECT @siteContract,v.dataset_version_id,s.code,s.access_table,s.family,s.grain,s.natural_key,s.row_role,s.load_order,1 FROM @spec s JOIN platform.dataset d ON d.product_id=@product AND d.dataset_code=s.code JOIN platform.dataset_version v ON v.dataset_id=d.dataset_id AND v.version=6 WHERE NOT EXISTS(SELECT 1 FROM platform.site_contract_dataset x WHERE x.site_contract_revision_id=@siteContract AND x.dataset_code=s.code);

/* Full physical/logical field contract. Optionality is explicit; no data values are embedded. */
DECLARE @fields TABLE(dataset_code NVARCHAR(120),ordinal INT,field_name NVARCHAR(128),logical_type VARCHAR(24),semantic_role VARCHAR(32),required BIT,key_role VARCHAR(24),scheme NVARCHAR(120));
INSERT @fields VALUES
(N'KIDS_CLASSIFIER_SCHEME',1,N'scheme_code','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_CLASSIFIER_SCHEME',2,N'authority_mode','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_CLASSIFIER_SCHEME',3,N'title','TEXT','CONTENT',1,NULL,NULL),(N'KIDS_CLASSIFIER_SCHEME',4,N'standard_reference','TEXT','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_CLASSIFIER_VERSION',1,N'version_ref','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_CLASSIFIER_VERSION',2,N'scheme_code','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_CLASSIFIER_VERSION',3,N'version_code','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_CLASSIFIER_VERSION',4,N'lifecycle_state','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_CLASSIFIER_VERSION',5,N'valid_from','DATE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_CLASSIFIER_VERSION',6,N'valid_to','DATE','ATTRIBUTE',0,NULL,NULL),
(N'KIDS_CLASSIFIER_ITEM',1,N'item_ref','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_CLASSIFIER_ITEM',2,N'version_ref','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_CLASSIFIER_ITEM',3,N'item_code','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_CLASSIFIER_ITEM',4,N'label_ka','TEXT','LOCALIZED_TEXT',0,NULL,NULL),(N'KIDS_CLASSIFIER_ITEM',5,N'label_en','TEXT','LOCALIZED_TEXT',0,NULL,NULL),(N'KIDS_CLASSIFIER_ITEM',6,N'lifecycle_state','CODE','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_CLASSIFIER_ALIAS',1,N'alias_ref','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_CLASSIFIER_ALIAS',2,N'item_ref','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_CLASSIFIER_ALIAS',3,N'source_system','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_CLASSIFIER_ALIAS',4,N'source_code_raw','TEXT','RAW_PAYLOAD',1,NULL,NULL),(N'KIDS_CLASSIFIER_ALIAS',5,N'lookup_code_normalized','TEXT','ATTRIBUTE',1,NULL,NULL),(N'KIDS_CLASSIFIER_ALIAS',6,N'normalization_rule','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_CLASSIFIER_ALIAS',7,N'lifecycle_state','CODE','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_CLASSIFIER_HIERARCHY',1,N'version_ref','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_CLASSIFIER_HIERARCHY',2,N'parent_item_ref','CODE','FOREIGN_KEY',0,NULL,NULL),(N'KIDS_CLASSIFIER_HIERARCHY',3,N'child_item_ref','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_CLASSIFIER_HIERARCHY',4,N'ordinal','INTEGER','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_RAW_DOCUMENT',1,N'source_row_key','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_RAW_DOCUMENT',2,N'source_table','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_RAW_DOCUMENT',3,N'source_primary_key','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_RAW_DOCUMENT',4,N'extract_sequence','INTEGER','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_GOAL',1,N'source_goal_id','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_GOAL',2,N'category_item_ref','CODE','CLASSIFICATION',1,NULL,N'KIDS_GOAL_CATEGORY'),(N'KIDS_GOAL',3,N'title_ka','TEXT','LOCALIZED_TEXT',0,NULL,NULL),(N'KIDS_GOAL',4,N'title_en','TEXT','LOCALIZED_TEXT',0,NULL,NULL),(N'KIDS_GOAL',5,N'path_ka','URI','LOCATOR',0,NULL,NULL),(N'KIDS_GOAL',6,N'path_en','URI','LOCATOR',0,NULL,NULL),(N'KIDS_GOAL',7,N'source_row_key','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_GOAL',8,N'operation','CODE','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_RESOURCE',1,N'source_resource_id','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_RESOURCE',2,N'category_item_ref','CODE','CLASSIFICATION',1,NULL,N'KIDS_GOAL_CATEGORY'),(N'KIDS_RESOURCE',3,N'title_ka','TEXT','LOCALIZED_TEXT',0,NULL,NULL),(N'KIDS_RESOURCE',4,N'title_en','TEXT','LOCALIZED_TEXT',0,NULL,NULL),(N'KIDS_RESOURCE',5,N'path_ka','URI','LOCATOR',0,NULL,NULL),(N'KIDS_RESOURCE',6,N'path_en','URI','LOCATOR',0,NULL,NULL),(N'KIDS_RESOURCE',7,N'source_row_key','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_RESOURCE',8,N'operation','CODE','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',1,N'assignment_key','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',2,N'source_resource_id','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',3,N'subcategory_item_ref','CODE','CLASSIFICATION',1,NULL,N'KIDS_RESOURCE_SUBCATEGORY'),(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',4,N'source_token_raw','TEXT','RAW_PAYLOAD',1,NULL,NULL),(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',5,N'ordinal','INTEGER','ATTRIBUTE',1,NULL,NULL),(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',6,N'source_row_key','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',7,N'operation','CODE','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_GLOSSARY_ENTRY',1,N'source_glossary_id','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_GLOSSARY_ENTRY',2,N'language_item_ref','CODE','CLASSIFICATION',1,NULL,N'LANGUAGE'),(N'KIDS_GLOSSARY_ENTRY',3,N'language_raw','TEXT','RAW_PAYLOAD',1,NULL,NULL),(N'KIDS_GLOSSARY_ENTRY',4,N'entry_text','TEXT','CONTENT',1,NULL,NULL),(N'KIDS_GLOSSARY_ENTRY',5,N'source_row_key','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_GLOSSARY_ENTRY',6,N'operation','CODE','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_STATISTICAL_CARRIER',1,N'carrier_code','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_STATISTICAL_CARRIER',2,N'source_resource_id','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_STATISTICAL_CARRIER',3,N'payload_checksum','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_STATISTICAL_CARRIER',4,N'parse_status','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_STATISTICAL_CARRIER',5,N'source_row_key','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_STATISTICAL_CARRIER',6,N'operation','CODE','ATTRIBUTE',1,NULL,NULL),
(N'KIDS_STATISTICAL_INPUT',1,N'input_key','CODE','NATURAL_KEY',1,'NATURAL',NULL),(N'KIDS_STATISTICAL_INPUT',2,N'carrier_code','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',3,N'cell_ordinal','INTEGER','ATTRIBUTE',1,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',4,N'period_raw','TEXT','PERIOD',1,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',5,N'period_normalized','TEXT','ATTRIBUTE',0,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',6,N'dimension_key_raw','TEXT','RAW_PAYLOAD',1,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',7,N'age_group_item_ref','CODE','DIMENSION',1,NULL,N'AGE_GROUP'),(N'KIDS_STATISTICAL_INPUT',8,N'value_lexical','TEXT','RAW_PAYLOAD',1,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',9,N'value_decimal','DECIMAL','MEASURE',0,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',10,N'json_path','TEXT','ATTRIBUTE',1,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',11,N'source_encoding','CODE','ATTRIBUTE',1,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',12,N'source_row_key','CODE','FOREIGN_KEY',1,NULL,NULL),(N'KIDS_STATISTICAL_INPUT',13,N'operation','CODE','ATTRIBUTE',1,NULL,NULL);
INSERT platform.site_contract_field(contract_dataset_id,field_name,logical_type,semantic_role,ordinal,required,key_role,classifier_scheme_code)
SELECT d.contract_dataset_id,f.field_name,f.logical_type,f.semantic_role,f.ordinal,f.required,f.key_role,f.scheme FROM @fields f JOIN platform.site_contract_dataset d ON d.site_contract_revision_id=@siteContract AND d.dataset_code=f.dataset_code WHERE NOT EXISTS(SELECT 1 FROM platform.site_contract_field x WHERE x.contract_dataset_id=d.contract_dataset_id AND x.field_name=f.field_name);

DECLARE @relations TABLE(code NVARCHAR(120),fd NVARCHAR(120),ff NVARCHAR(128),td NVARCHAR(120),tf NVARCHAR(128),card VARCHAR(24),req BIT,priority INT);
INSERT @relations VALUES
(N'VERSION_FOR_SCHEME',N'KIDS_CLASSIFIER_VERSION',N'scheme_code',N'KIDS_CLASSIFIER_SCHEME',N'scheme_code','MANY_TO_ONE',1,10),(N'ITEM_FOR_VERSION',N'KIDS_CLASSIFIER_ITEM',N'version_ref',N'KIDS_CLASSIFIER_VERSION',N'version_ref','MANY_TO_ONE',1,20),(N'ALIAS_FOR_ITEM',N'KIDS_CLASSIFIER_ALIAS',N'item_ref',N'KIDS_CLASSIFIER_ITEM',N'item_ref','MANY_TO_ONE',1,30),(N'GOAL_CATEGORY',N'KIDS_GOAL',N'category_item_ref',N'KIDS_CLASSIFIER_ITEM',N'item_ref','MANY_TO_ONE',1,100),(N'RESOURCE_CATEGORY',N'KIDS_RESOURCE',N'category_item_ref',N'KIDS_CLASSIFIER_ITEM',N'item_ref','MANY_TO_ONE',1,110),(N'SUBCATEGORY_FOR_RESOURCE',N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',N'source_resource_id',N'KIDS_RESOURCE',N'source_resource_id','MANY_TO_ONE',1,120),(N'SUBCATEGORY_ITEM',N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',N'subcategory_item_ref',N'KIDS_CLASSIFIER_ITEM',N'item_ref','MANY_TO_ONE',1,121),(N'GLOSSARY_LANGUAGE',N'KIDS_GLOSSARY_ENTRY',N'language_item_ref',N'KIDS_CLASSIFIER_ITEM',N'item_ref','MANY_TO_ONE',1,130),(N'CARRIER_FOR_RESOURCE',N'KIDS_STATISTICAL_CARRIER',N'source_resource_id',N'KIDS_RESOURCE',N'source_resource_id','ONE_TO_ONE',1,140),(N'INPUT_FOR_CARRIER',N'KIDS_STATISTICAL_INPUT',N'carrier_code',N'KIDS_STATISTICAL_CARRIER',N'carrier_code','MANY_TO_ONE',1,150),(N'GOAL_RAW_LINEAGE',N'KIDS_GOAL',N'source_row_key',N'KIDS_RAW_DOCUMENT',N'source_row_key','MANY_TO_ONE',1,200),(N'RESOURCE_RAW_LINEAGE',N'KIDS_RESOURCE',N'source_row_key',N'KIDS_RAW_DOCUMENT',N'source_row_key','MANY_TO_ONE',1,201),(N'SUBCATEGORY_RAW_LINEAGE',N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',N'source_row_key',N'KIDS_RAW_DOCUMENT',N'source_row_key','MANY_TO_ONE',1,202),(N'GLOSSARY_RAW_LINEAGE',N'KIDS_GLOSSARY_ENTRY',N'source_row_key',N'KIDS_RAW_DOCUMENT',N'source_row_key','MANY_TO_ONE',1,203),(N'CARRIER_RAW_LINEAGE',N'KIDS_STATISTICAL_CARRIER',N'source_row_key',N'KIDS_RAW_DOCUMENT',N'source_row_key','MANY_TO_ONE',1,204),(N'INPUT_RAW_LINEAGE',N'KIDS_STATISTICAL_INPUT',N'source_row_key',N'KIDS_RAW_DOCUMENT',N'source_row_key','MANY_TO_ONE',1,205);
INSERT platform.site_contract_relation(site_contract_revision_id,relation_code,from_dataset_code,from_field_name,to_dataset_code,to_field_name,relation_kind,cardinality,required,enforcement_policy,load_priority) SELECT @siteContract,code,fd,ff,td,tf,'FOREIGN_KEY',card,req,'ACCESS_AND_INGEST',priority FROM @relations r WHERE NOT EXISTS(SELECT 1 FROM platform.site_contract_relation x WHERE x.site_contract_revision_id=@siteContract AND x.relation_code=r.code);

INSERT platform.site_contract_classifier(site_contract_revision_id,scheme_code,version_policy,authority_mode,unknown_value_policy,publication_policy) SELECT @siteContract,v.code,'EXACT_VERSION','CONTROL_PLANE','CREATE_DRAFT_PROPOSAL','BLOCK_UNTIL_PUBLISHED' FROM(VALUES(N'KIDS_GOAL_CATEGORY'),(N'LANGUAGE'),(N'KIDS_RESOURCE_SUBCATEGORY'),(N'AGE_GROUP'))v(code) WHERE NOT EXISTS(SELECT 1 FROM platform.site_contract_classifier x WHERE x.site_contract_revision_id=@siteContract AND x.scheme_code=v.code);

DECLARE @resourceDataset BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_RESOURCE');
IF NOT EXISTS(SELECT 1 FROM platform.statistical_dataflow WHERE product_id=@product AND dataflow_code=N'KIDS_FILES_STATISTICS') INSERT platform.statistical_dataflow(product_id,dataflow_code,source_dataset_id,title_ka,title_en,status)VALUES(@product,N'KIDS_FILES_STATISTICS',@resourceDataset,N'KIDS ფაილების სტატისტიკა',N'KIDS file statistics','DRAFT');
DECLARE @dataflow BIGINT=(SELECT dataflow_id FROM platform.statistical_dataflow WHERE product_id=@product AND dataflow_code=N'KIDS_FILES_STATISTICS');
IF NOT EXISTS(SELECT 1 FROM platform.statistical_dsd WHERE dataflow_id=@dataflow AND dsd_code=N'KIDS_FILES_STATISTICS_DSD' AND revision=1) INSERT platform.statistical_dsd(dataflow_id,dsd_code,revision,observation_grain,status)VALUES(@dataflow,N'KIDS_FILES_STATISTICS_DSD',1,N'carrier_code + cell_ordinal','REVIEW_REQUIRED');
DECLARE @dsd BIGINT=(SELECT dsd_id FROM platform.statistical_dsd WHERE dataflow_id=@dataflow AND dsd_code=N'KIDS_FILES_STATISTICS_DSD' AND revision=1);
DECLARE @timeDim BIGINT=(SELECT dimension_id FROM platform.dimension WHERE dimension_code=N'TIME_PERIOD'),@ageDim BIGINT=(SELECT dimension_id FROM platform.dimension WHERE dimension_code=N'AGE_GROUP'),@measure BIGINT=(SELECT measure_id FROM platform.measure WHERE measure_code=N'KIDS_OBS_VALUE_INPUT'),@ageVersion BIGINT=(SELECT classification_version_id FROM platform.classification_version WHERE scheme_id=@ageScheme AND version=N'KIDS_PORTAL_V1_R6');
MERGE platform.statistical_component t USING(VALUES
(N'TIME_PERIOD','DIMENSION',1,1,@timeDim,NULL,NULL,'OBSERVATION',N'period_raw',N'period_normalized',N'{"pattern":"^[0-9]{4}$","rawPreserved":true}'),
(N'AGE_GROUP','DIMENSION',2,1,@ageDim,NULL,@ageVersion,'SERIES',N'dimension_key_raw',N'age_group_item_ref',N'{"normalization":"TRIM_FOR_ALIAS_LOOKUP_ONLY","publishedAliasRequired":true}'),
(N'OBS_VALUE','MEASURE',3,1,NULL,@measure,NULL,'OBSERVATION',N'value_lexical',N'value_decimal',N'{"metricBinding":"REQUIRED_PER_CARRIER","unit":"REQUIRED","aggregation":"REQUIRED","defaultAggregation":"NONE"}'),
(N'UNIT_MEASURE','ATTRIBUTE',4,1,NULL,NULL,NULL,'SERIES',NULL,NULL,N'{"binding":"CONTROL_PLANE_REQUIRED"}'),
(N'UNIT_MULTIPLIER','ATTRIBUTE',5,1,NULL,NULL,NULL,'SERIES',NULL,NULL,N'{"binding":"CONTROL_PLANE_REQUIRED"}'),
(N'DECIMALS','ATTRIBUTE',6,1,NULL,NULL,NULL,'SERIES',NULL,NULL,N'{"binding":"CONTROL_PLANE_REQUIRED"}'),
(N'AGGREGATION','ATTRIBUTE',7,1,NULL,NULL,NULL,'SERIES',NULL,NULL,N'{"binding":"CONTROL_PLANE_REQUIRED","default":"NONE"}'),
(N'OBS_STATUS','ATTRIBUTE',8,1,NULL,NULL,NULL,'OBSERVATION',NULL,NULL,N'{"binding":"CONTROL_PLANE_REQUIRED"}'),
(N'CONF_STATUS','ATTRIBUTE',9,1,NULL,NULL,NULL,'OBSERVATION',NULL,NULL,N'{"binding":"CONTROL_PLANE_REQUIRED"}'),
(N'SOURCE_CARRIER','ATTRIBUTE',10,1,NULL,NULL,NULL,'OBSERVATION',N'carrier_code',NULL,N'{"lineage":true}'),
(N'SOURCE_CELL_KEY','ATTRIBUTE',11,1,NULL,NULL,NULL,'OBSERVATION',N'input_key',NULL,N'{"lineage":true}'),
(N'SOURCE_JSON_PATH','ATTRIBUTE',12,1,NULL,NULL,NULL,'OBSERVATION',N'json_path',NULL,N'{"lineage":true}')
)s(code,role,ord,req,dim,meas,clv,attach,src,norm,rules) ON t.dsd_id=@dsd AND t.component_code=s.code
WHEN MATCHED THEN UPDATE SET component_role=s.role,component_order=s.ord,required=s.req,dimension_id=s.dim,measure_id=s.meas,classification_version_id=s.clv,attachment_level=s.attach,source_field=s.src,normalized_field=s.norm,constraint_json=s.rules,status='DRAFT'
WHEN NOT MATCHED THEN INSERT(dsd_id,component_code,component_role,component_order,required,dimension_id,measure_id,classification_version_id,attachment_level,source_field,normalized_field,constraint_json,status)VALUES(@dsd,s.code,s.role,s.ord,s.req,s.dim,s.meas,s.clv,s.attach,s.src,s.norm,s.rules,'DRAFT');

/* Site hierarchy is structural, not fabricated content hierarchy. */
IF NOT EXISTS(SELECT 1 FROM platform.site_contract_node WHERE site_contract_revision_id=@siteContract AND node_code=N'KIDS_ROOT') INSERT platform.site_contract_node(site_contract_revision_id,node_code,node_kind,path_segment,sort_order,title_ka,title_en)VALUES(@siteContract,N'KIDS_ROOT','SITE_ROOT',N'kids',1,N'ბავშვებისა და მოზარდების სტატისტიკა',N'Children and youth statistics');
DECLARE @root BIGINT=(SELECT node_id FROM platform.site_contract_node WHERE site_contract_revision_id=@siteContract AND node_code=N'KIDS_ROOT');
INSERT platform.site_contract_node(site_contract_revision_id,node_code,parent_node_id,node_kind,dataset_code,path_segment,sort_order,title_ka,title_en)
SELECT @siteContract,v.code,@root,v.kind,v.dataset,v.path,v.ord,v.ka,v.en FROM(VALUES
(N'KIDS_GOALS','ENTITY_COLLECTION',N'KIDS_GOAL',N'goals',10,N'მიზნები',N'Goals'),(N'KIDS_RESOURCES','ENTITY_COLLECTION',N'KIDS_RESOURCE',N'resources',20,N'რესურსები',N'Resources'),(N'KIDS_GLOSSARY','ENTITY_COLLECTION',N'KIDS_GLOSSARY_ENTRY',N'glossary',30,N'ლექსიკონი',N'Glossary'),(N'KIDS_STATISTICS','STATISTICAL_DATAFLOW',N'KIDS_STATISTICAL_INPUT',N'statistics',40,N'სტატისტიკა',N'Statistics'),(N'KIDS_CLASSIFIERS','REFERENCE_REGISTRY',N'KIDS_CLASSIFIER_ITEM',N'classifiers',50,N'კლასიფიკატორები',N'Classifiers'))v(code,kind,dataset,path,ord,ka,en) WHERE NOT EXISTS(SELECT 1 FROM platform.site_contract_node n WHERE n.site_contract_revision_id=@siteContract AND n.node_code=v.code);

INSERT platform.site_contract_gate(site_contract_revision_id,gate_code,gate_type,severity,blocking,rule_json)
SELECT @siteContract,v.code,v.type,'ERROR',1,v.rules FROM(VALUES
(N'SCHEMA_EXACT','STRUCTURAL',N'{"rejectUndeclaredTables":true,"rejectUndeclaredColumns":true,"requireExactContractRevision":true}'),(N'KEYS_UNIQUE','INTEGRITY',N'{"naturalKeysUnique":true,"compositeKeysUnique":true}'),(N'RELATIONS_RESOLVED','INTEGRITY',N'{"allRequiredForeignKeysResolve":true,"noOrphans":true}'),(N'CLASSIFIERS_PUBLISHED','SEMANTIC',N'{"allClassificationAssignmentsResolveToPublishedVersion":true,"unknownValuesRemainProposals":true}'),(N'STATISTICAL_SEMANTICS','SEMANTIC',N'{"metric":true,"unit":true,"aggregation":true,"dsd":true,"dimensionAliases":true,"qualityPolicy":true,"confidentialityPolicy":true}'),(N'RAW_LINEAGE','LINEAGE',N'{"immutableArtifactRequired":true,"rowLocatorRequired":true,"generatedRawJsonForbidden":true}'),(N'PUBLICATION_ATOMIC','PUBLICATION',N'{"allDatasetsValidated":true,"snapshotImmutable":true,"rollbackPointerRequired":true}'))v(code,type,rules) WHERE NOT EXISTS(SELECT 1 FROM platform.site_contract_gate g WHERE g.site_contract_revision_id=@siteContract AND g.gate_code=v.code);

/* Bind the issued Access shape to the immutable v6 dataset versions. */
UPDATE platform.contract_source SET active=0 WHERE contract_id=@contract;
MERGE platform.contract_source t USING(
 SELECT @contract contract_id,N'ACCESS.'+s.access_table source_locator,'ACCESS_TABLE' source_kind,v.dataset_version_id,s.natural_key source_key_expression,s.row_role,s.load_order,
 CASE WHEN s.code=N'KIDS_STATISTICAL_INPUT' THEN N'{"approvalState":"DRAFT","projectionModel":"SDMX_COMPATIBLE_LONG","dataflowCode":"KIDS_FILES_STATISTICS","dsdCode":"KIDS_FILES_STATISTICS_DSD","dsdRevision":1,"dimensions":[{"code":"TIME_PERIOD","source":"period_raw","normalized":"period_normalized"},{"code":"AGE_GROUP","source":"dimension_key_raw","classificationItem":"age_group_item_ref"}],"measure":{"componentCode":"OBS_VALUE","registryCode":"KIDS_OBS_VALUE_INPUT","lexical":"value_lexical","numeric":"value_decimal","defaultAggregation":"NONE"},"publicationGate":"ALL_SEMANTICS_APPROVED"}'
 WHEN s.code=N'KIDS_STATISTICAL_CARRIER' THEN N'{"approvalState":"DRAFT","projectionModel":"CARRIER_LOCATOR","payloadStorage":"IMMUTABLE_OBJECT_STORAGE","payloadChecksum":"payload_checksum","sourceResource":"source_resource_id","sourceRow":"source_row_key"}'
 ELSE CONCAT(N'{"approvalState":"DRAFT","datasetCode":"',s.code,N'","canonicalAccessRevision":6,"naturalKey":"',s.natural_key,N'","rawLineageRequired":true}') END mapping_spec_json
 FROM @spec s JOIN platform.dataset d ON d.product_id=@product AND d.dataset_code=s.code JOIN platform.dataset_version v ON v.dataset_id=d.dataset_id AND v.version=6
)s ON t.contract_id=s.contract_id AND t.source_locator=s.source_locator
WHEN MATCHED THEN UPDATE SET source_kind=s.source_kind,target_dataset_version_id=s.dataset_version_id,source_key_expression=s.source_key_expression,row_role=s.row_role,load_order=s.load_order,mapping_spec_json=s.mapping_spec_json,active=1
WHEN NOT MATCHED THEN INSERT(contract_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json,active)VALUES(s.contract_id,s.source_locator,s.source_kind,s.dataset_version_id,s.source_key_expression,s.row_role,s.load_order,s.mapping_spec_json,1);
UPDATE platform.ingestion_contract SET contract_revision=6,format_profile='ACCESS_CANONICAL_R6',status='REVIEW_REQUIRED',auto_publish=0,raw_ingest_enabled=1 WHERE contract_id=@contract;
IF NOT EXISTS(SELECT 1 FROM platform.ingestion_contract_revision WHERE contract_id=@contract AND revision=6)
 INSERT platform.ingestion_contract_revision(contract_id,revision,format_profile,lifecycle_status,compatibility_mode)VALUES(@contract,6,'ACCESS_CANONICAL_R6','REVIEW_REQUIRED','BACKWARD_COMPATIBLE');
DECLARE @ingestionR6 BIGINT=(SELECT ingestion_contract_revision_id FROM platform.ingestion_contract_revision WHERE contract_id=@contract AND revision=6);
INSERT platform.contract_revision_source(ingestion_contract_revision_id,source_locator,source_kind,target_dataset_version_id,source_key_expression,row_role,load_order,mapping_spec_json)
SELECT @ingestionR6,cs.source_locator,cs.source_kind,cs.target_dataset_version_id,cs.source_key_expression,cs.row_role,cs.load_order,cs.mapping_spec_json
FROM platform.contract_source cs WHERE cs.contract_id=@contract AND cs.active=1
AND NOT EXISTS(SELECT 1 FROM platform.contract_revision_source x WHERE x.ingestion_contract_revision_id=@ingestionR6 AND x.source_locator=cs.source_locator);

IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ix_site_contract_field_role' AND object_id=OBJECT_ID(N'platform.site_contract_field')) CREATE INDEX ix_site_contract_field_role ON platform.site_contract_field(semantic_role,contract_dataset_id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ix_site_contract_relation_endpoint' AND object_id=OBJECT_ID(N'platform.site_contract_relation')) CREATE INDEX ix_site_contract_relation_endpoint ON platform.site_contract_relation(site_contract_revision_id,from_dataset_code,to_dataset_code);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ix_stat_component_role' AND object_id=OBJECT_ID(N'platform.statistical_component')) CREATE INDEX ix_stat_component_role ON platform.statistical_component(dsd_id,component_role,component_order);
