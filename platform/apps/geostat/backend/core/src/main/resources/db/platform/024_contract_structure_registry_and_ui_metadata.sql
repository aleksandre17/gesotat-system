/*
 * KIDS_PORTAL revision 8 — canonical structure registry and UI metadata.
 * Additive only.  The registry is the physical Control Plane representation
 * of table boundaries; Access is generated from an approved contract revision.
 */
IF OBJECT_ID(N'platform.contract_namespace',N'U') IS NULL CREATE TABLE platform.contract_namespace(
  namespace_id BIGINT IDENTITY PRIMARY KEY,
  namespace_code NVARCHAR(64) NOT NULL UNIQUE,
  title_ka NVARCHAR(255) NOT NULL,
  title_en NVARCHAR(255) NULL,
  description_ka NVARCHAR(MAX) NULL,
  authority_mode VARCHAR(32) NOT NULL,
  status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
  CONSTRAINT ck_contract_namespace_authority CHECK(authority_mode IN('AUTHORITATIVE','DERIVED','RAW_EVIDENCE','SYSTEM'))
);

IF OBJECT_ID(N'platform.contract_structure',N'U') IS NULL CREATE TABLE platform.contract_structure(
  structure_id BIGINT IDENTITY PRIMARY KEY,
  namespace_id BIGINT NOT NULL,
  structure_code NVARCHAR(160) NOT NULL,
  structure_kind VARCHAR(40) NOT NULL,
  data_class VARCHAR(32) NOT NULL,
  grain NVARCHAR(1000) NOT NULL,
  authority_mode VARCHAR(32) NOT NULL,
  lifecycle_policy VARCHAR(32) NOT NULL,
  allowed_content NVARCHAR(MAX) NOT NULL,
  forbidden_content NVARCHAR(MAX) NOT NULL,
  lineage_policy NVARCHAR(1000) NOT NULL,
  quality_policy_code NVARCHAR(120) NULL,
  confidentiality_policy_code NVARCHAR(120) NULL,
  standard_code NVARCHAR(120) NULL,
  standard_version NVARCHAR(64) NULL,
  ui_capabilities_json NVARCHAR(MAX) NOT NULL,
  lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  revision INT NOT NULL DEFAULT 1,
  checksum CHAR(64) NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_contract_structure UNIQUE(namespace_id,structure_code,revision),
  CONSTRAINT fk_contract_structure_namespace FOREIGN KEY(namespace_id) REFERENCES platform.contract_namespace(namespace_id),
  CONSTRAINT ck_contract_structure_ui_json CHECK(ISJSON(ui_capabilities_json)=1),
  CONSTRAINT ck_contract_structure_authority CHECK(authority_mode IN('AUTHORITATIVE','DERIVED','RAW_EVIDENCE','CACHE','SYSTEM')),
  CONSTRAINT ck_contract_structure_lifecycle CHECK(lifecycle_policy IN('IMMUTABLE','APPEND_ONLY','MUTABLE','REBUILDABLE','ARCHIVAL'))
);

IF OBJECT_ID(N'platform.contract_table_definition',N'U') IS NULL CREATE TABLE platform.contract_table_definition(
  table_definition_id BIGINT IDENTITY PRIMARY KEY,
  structure_id BIGINT NOT NULL,
  logical_table_code NVARCHAR(160) NOT NULL,
  physical_table_name NVARCHAR(128) NOT NULL,
  access_table_name NVARCHAR(128) NULL,
  table_role VARCHAR(32) NOT NULL,
  storage_plane VARCHAR(32) NOT NULL,
  primary_key_expression NVARCHAR(1000) NOT NULL,
  load_order INT NOT NULL DEFAULT 100,
  required BIT NOT NULL DEFAULT 1,
  lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  revision INT NOT NULL DEFAULT 1,
  CONSTRAINT uq_contract_table_definition UNIQUE(structure_id,logical_table_code,revision),
  CONSTRAINT uq_contract_table_physical UNIQUE(physical_table_name,revision),
  CONSTRAINT fk_contract_table_structure FOREIGN KEY(structure_id) REFERENCES platform.contract_structure(structure_id)
);

IF OBJECT_ID(N'platform.contract_field_definition',N'U') IS NULL CREATE TABLE platform.contract_field_definition(
  field_definition_id BIGINT IDENTITY PRIMARY KEY,
  table_definition_id BIGINT NOT NULL,
  field_name NVARCHAR(128) NOT NULL,
  ordinal INT NOT NULL,
  logical_type VARCHAR(32) NOT NULL,
  physical_type VARCHAR(32) NOT NULL,
  semantic_role VARCHAR(40) NOT NULL,
  required BIT NOT NULL,
  nullable BIT NOT NULL,
  default_expression NVARCHAR(1000) NULL,
  classifier_scheme_code NVARCHAR(120) NULL,
  relation_target NVARCHAR(255) NULL,
  ui_control_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
  ui_visible BIT NOT NULL DEFAULT 1,
  ui_editable BIT NOT NULL DEFAULT 0,
  ui_filterable BIT NOT NULL DEFAULT 0,
  ui_sortable BIT NOT NULL DEFAULT 0,
  ui_groupable BIT NOT NULL DEFAULT 0,
  validation_json NVARCHAR(MAX) NULL,
  lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  CONSTRAINT uq_contract_field_definition UNIQUE(table_definition_id,field_name),
  CONSTRAINT uq_contract_field_ordinal UNIQUE(table_definition_id,ordinal),
  CONSTRAINT fk_contract_field_table FOREIGN KEY(table_definition_id) REFERENCES platform.contract_table_definition(table_definition_id),
  CONSTRAINT ck_contract_field_validation_json CHECK(validation_json IS NULL OR ISJSON(validation_json)=1)
);

IF OBJECT_ID(N'platform.contract_structure_relation',N'U') IS NULL CREATE TABLE platform.contract_structure_relation(
  relation_definition_id BIGINT IDENTITY PRIMARY KEY,
  structure_id BIGINT NOT NULL,
  relation_code NVARCHAR(160) NOT NULL,
  from_table_code NVARCHAR(160) NOT NULL,
  from_field_name NVARCHAR(128) NOT NULL,
  to_table_code NVARCHAR(160) NOT NULL,
  to_field_name NVARCHAR(128) NOT NULL,
  relation_kind VARCHAR(32) NOT NULL,
  cardinality VARCHAR(24) NOT NULL,
  required BIT NOT NULL,
  enforcement_policy VARCHAR(32) NOT NULL,
  load_order INT NOT NULL DEFAULT 100,
  lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  CONSTRAINT uq_contract_structure_relation UNIQUE(structure_id,relation_code),
  CONSTRAINT fk_contract_structure_relation_structure FOREIGN KEY(structure_id) REFERENCES platform.contract_structure(structure_id)
);

IF OBJECT_ID(N'platform.contract_ui_surface',N'U') IS NULL CREATE TABLE platform.contract_ui_surface(
  ui_surface_id BIGINT IDENTITY PRIMARY KEY,
  structure_id BIGINT NOT NULL,
  surface_code NVARCHAR(160) NOT NULL,
  surface_kind VARCHAR(32) NOT NULL,
  route_template NVARCHAR(500) NULL,
  mode VARCHAR(24) NOT NULL,
  configuration_json NVARCHAR(MAX) NOT NULL,
  lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  CONSTRAINT uq_contract_ui_surface UNIQUE(structure_id,surface_code),
  CONSTRAINT fk_contract_ui_surface_structure FOREIGN KEY(structure_id) REFERENCES platform.contract_structure(structure_id),
  CONSTRAINT ck_contract_ui_surface_json CHECK(ISJSON(configuration_json)=1)
);

MERGE platform.contract_namespace AS t USING (VALUES
 (N'GS',N'კონტრაქტი და governance',N'Contract and governance',N'authoritative schema and lifecycle metadata','AUTHORITATIVE'),
 (N'CL',N'კლასიფიკატორები',N'Classifications',N'controlled vocabularies','AUTHORITATIVE'),
 (N'STAT',N'სტატისტიკური მონაცემები',N'Statistical data',N'DSD, metrics, units and observations','AUTHORITATIVE'),
 (N'RAW',N'წყარო და provenance',N'Raw source evidence',N'immutable source artifacts and records','RAW_EVIDENCE'),
 (N'ENT',N'კანონიკური entity',N'Canonical entities',N'domain entities and typed links','AUTHORITATIVE'),
 (N'REF',N'ჩვეულებრივი reference',N'Reference data',N'non-statistical reference values','AUTHORITATIVE'),
 (N'SERV',N'გამოქვეყნებული projection',N'Serving projections',N'rebuildable read models','DERIVED'),
 (N'ARCH',N'არქივი',N'Archive',N'immutable historical releases','DERIVED'),
 (N'AUDIT',N'აუდიტი',N'Audit',N'append-only decision history','SYSTEM'),
 (N'SYS',N'ტექნიკური runtime',N'System runtime',N'jobs, checksums and leases','SYSTEM'),
 (N'KIDS',N'KIDS domain',N'KIDS domain',N'canonical KIDS business entities','AUTHORITATIVE')
)s(code,ka,en,description,authority_mode) ON t.namespace_code=s.code
WHEN MATCHED THEN UPDATE SET title_ka=s.ka,title_en=s.en,description_ka=s.description,authority_mode=s.authority_mode
WHEN NOT MATCHED THEN INSERT(namespace_code,title_ka,title_en,description_ka,authority_mode) VALUES(s.code,s.ka,s.en,s.description,s.authority_mode);

/* Register every currently issued Access table as a contract structure/table. */
DECLARE @tables TABLE(namespace_code NVARCHAR(64), structure_code NVARCHAR(160), kind VARCHAR(40), family VARCHAR(32), grain NVARCHAR(1000), authority VARCHAR(32), lifecycle VARCHAR(32), physical_name NVARCHAR(128), role VARCHAR(32), load_order INT);
INSERT @tables VALUES
(N'GS',N'GS_PACKAGE','CONTRACT','METADATA',N'one package manifest','AUTHORITATIVE','IMMUTABLE',N'__gs_package','METADATA',1),
(N'GS',N'GS_DATASET','CONTRACT','METADATA',N'one declared dataset','AUTHORITATIVE','IMMUTABLE',N'__gs_dataset','METADATA',2),
(N'GS',N'GS_FIELD','CONTRACT','METADATA',N'one ordered field declaration','AUTHORITATIVE','IMMUTABLE',N'__gs_field','METADATA',3),
(N'GS',N'GS_KEY','CONTRACT','METADATA',N'one key field binding','AUTHORITATIVE','IMMUTABLE',N'__gs_key','METADATA',4),
(N'GS',N'GS_RELATION','CONTRACT','METADATA',N'one relation declaration','AUTHORITATIVE','IMMUTABLE',N'__gs_relation','METADATA',5),
(N'GS',N'GS_PROJECTION','CONTRACT','METADATA',N'one serving projection','AUTHORITATIVE','IMMUTABLE',N'__gs_projection','METADATA',6),
(N'CL',N'CL_SCHEME','CLASSIFICATION','REFERENCE',N'one classifier scheme','AUTHORITATIVE','IMMUTABLE',N'__cl_scheme','REFERENCE',10),
(N'CL',N'CL_VERSION','CLASSIFICATION','REFERENCE',N'one classifier version','AUTHORITATIVE','IMMUTABLE',N'__cl_version','REFERENCE',11),
(N'CL',N'CL_ITEM','CLASSIFICATION','REFERENCE',N'one classifier item','AUTHORITATIVE','IMMUTABLE',N'__cl_item','REFERENCE',12),
(N'CL',N'CL_ALIAS','CLASSIFICATION','REFERENCE',N'one source alias','AUTHORITATIVE','IMMUTABLE',N'__cl_alias','REFERENCE',13),
(N'CL',N'CL_HIERARCHY','CLASSIFICATION','REFERENCE',N'one hierarchy edge','AUTHORITATIVE','IMMUTABLE',N'__cl_hierarchy','REFERENCE',14),
(N'STAT',N'STAT_UNIT','REFERENCE','REFERENCE',N'one governed statistical unit','AUTHORITATIVE','IMMUTABLE',N'__stat_unit','REFERENCE',20),
(N'STAT',N'STAT_METRIC','REFERENCE','REFERENCE',N'one governed metric','AUTHORITATIVE','IMMUTABLE',N'__stat_metric','REFERENCE',21),
(N'RAW',N'RAW_DOCUMENT','DOCUMENT','RAW',N'one source artifact locator','RAW_EVIDENCE','APPEND_ONLY',N'__raw_document','RAW',30),
(N'KIDS',N'KIDS_GOAL','ENTITY','ENTITY',N'one KIDS goal','AUTHORITATIVE','MUTABLE',N'kids_goal','ENTITY',40),
(N'KIDS',N'KIDS_RESOURCE','ENTITY','ENTITY',N'one KIDS resource','AUTHORITATIVE','MUTABLE',N'kids_resource','ENTITY',41),
(N'KIDS',N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT','RELATION','RELATION',N'one resource x subcategory token','AUTHORITATIVE','MUTABLE',N'kids_resource_subcategory_assignment','RELATION',42),
(N'KIDS',N'KIDS_GLOSSARY_ENTRY','ENTITY','ENTITY',N'one glossary entry','AUTHORITATIVE','MUTABLE',N'kids_glossary_entry','ENTITY',43),
(N'KIDS',N'KIDS_STATISTICAL_CARRIER','RAW_RECORD','RAW',N'one carrier locator','RAW_EVIDENCE','APPEND_ONLY',N'kids_statistical_carrier','RAW',44),
(N'KIDS',N'KIDS_STATISTICAL_INPUT','STATISTICAL_OBSERVATION','STATISTICAL',N'one carrier x source cell','AUTHORITATIVE','APPEND_ONLY',N'kids_statistical_input','STATISTICAL',45),
(N'KIDS',N'KIDS_STATISTICAL_SEMANTIC_BINDING','RELATION','STATISTICAL',N'one semantic binding per carrier','AUTHORITATIVE','IMMUTABLE',N'kids_statistical_semantic_binding','RELATION',46);
INSERT platform.contract_structure(namespace_id,structure_code,structure_kind,data_class,grain,authority_mode,lifecycle_policy,allowed_content,forbidden_content,lineage_policy,ui_capabilities_json,revision)
SELECT n.namespace_id,t.structure_code,t.kind,t.family,t.grain,t.authority,t.lifecycle,N'only declared content',N'undeclared data or foreign domain content',N'source-derived rows require source_row_key/artifact lineage',N'{"list":true,"detail":true,"create":false,"edit":false,"approve":true,"import":true,"export":true}',1
FROM @tables t JOIN platform.contract_namespace n ON n.namespace_code=t.namespace_code
WHERE NOT EXISTS(SELECT 1 FROM platform.contract_structure x WHERE x.namespace_id=n.namespace_id AND x.structure_code=t.structure_code AND x.revision=1);
INSERT platform.contract_table_definition(structure_id,logical_table_code,physical_table_name,access_table_name,table_role,storage_plane,primary_key_expression,load_order,revision)
SELECT s.structure_id,t.structure_code,t.physical_name,t.physical_name,t.role,CASE WHEN t.authority='RAW_EVIDENCE' THEN 'RAW' ELSE 'ACCESS' END,N'contract-declared key',t.load_order,1
FROM @tables t JOIN platform.contract_namespace n ON n.namespace_code=t.namespace_code JOIN platform.contract_structure s ON s.namespace_id=n.namespace_id AND s.structure_code=t.structure_code AND s.revision=1
WHERE NOT EXISTS(SELECT 1 FROM platform.contract_table_definition x WHERE x.structure_id=s.structure_id AND x.logical_table_code=t.structure_code AND x.revision=1);

/* Existing site-contract datasets are also registered, including future extension tables. */
INSERT platform.contract_structure(namespace_id,structure_code,structure_kind,data_class,grain,authority_mode,lifecycle_policy,allowed_content,forbidden_content,lineage_policy,ui_capabilities_json,revision)
SELECT n.namespace_id,UPPER(REPLACE(d.dataset_code,N'-',N'_')),CASE WHEN d.data_family='STATISTICAL' THEN 'STATISTICAL_OBSERVATION' WHEN d.data_family='RAW' THEN 'RAW_RECORD' WHEN d.data_family='REFERENCE' THEN 'REFERENCE' ELSE 'ENTITY' END,d.data_family,d.business_grain,'AUTHORITATIVE','MUTABLE',N'contract-declared dataset rows',N'rows from another dataset',N'source lineage according to ingestion contract',N'{"list":true,"detail":true,"create":true,"edit":true,"approve":true,"import":true,"export":true}',7
FROM platform.site_contract_dataset d JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id AND r.revision=7 JOIN platform.contract_namespace n ON n.namespace_code=CASE WHEN d.data_family='STATISTICAL' THEN N'STAT' WHEN d.data_family='RAW' THEN N'RAW' WHEN d.data_family='REFERENCE' THEN N'REF' ELSE N'KIDS' END
WHERE NOT EXISTS(SELECT 1 FROM platform.contract_structure s WHERE s.namespace_id=n.namespace_id AND s.structure_code=UPPER(REPLACE(d.dataset_code,N'-',N'_')) AND s.revision=7);

INSERT platform.contract_table_definition(structure_id,logical_table_code,physical_table_name,access_table_name,table_role,storage_plane,primary_key_expression,load_order,revision)
SELECT s.structure_id,UPPER(REPLACE(d.dataset_code,N'-',N'_')),d.access_table_name,d.access_table_name,'DATASET','ACCESS',d.natural_key_expression,d.load_order,7
FROM platform.site_contract_dataset d JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id AND r.revision=7
JOIN platform.contract_namespace n ON n.namespace_code=CASE WHEN d.data_family='STATISTICAL' THEN N'STAT' WHEN d.data_family='RAW' THEN N'RAW' WHEN d.data_family='REFERENCE' THEN N'REF' ELSE N'KIDS' END
JOIN platform.contract_structure s ON s.namespace_id=n.namespace_id AND s.structure_code=UPPER(REPLACE(d.dataset_code,N'-',N'_')) AND s.revision=7
WHERE NOT EXISTS(SELECT 1 FROM platform.contract_table_definition x WHERE x.structure_id=s.structure_id AND x.logical_table_code=UPPER(REPLACE(d.dataset_code,N'-',N'_')) AND x.revision=7);

/* Field definitions are copied from the issued site contract, never inferred by Access. */
INSERT platform.contract_field_definition(table_definition_id,field_name,ordinal,logical_type,physical_type,semantic_role,required,nullable,classifier_scheme_code,relation_target,ui_control_type,ui_visible,ui_editable,ui_filterable,ui_sortable,ui_groupable,validation_json,lifecycle_status)
SELECT td.table_definition_id,f.field_name,f.ordinal,f.logical_type,
 CASE f.logical_type WHEN 'TEXT' THEN 'MEMO' WHEN 'CODE' THEN 'TEXT' WHEN 'INTEGER' THEN 'LONG' WHEN 'DECIMAL' THEN 'DOUBLE' WHEN 'BOOLEAN' THEN 'BOOLEAN' WHEN 'DATE' THEN 'SHORT_DATE_TIME' WHEN 'URI' THEN 'TEXT' ELSE 'TEXT' END,
 f.semantic_role,f.required,CASE WHEN f.required=1 THEN 0 ELSE 1 END,f.classifier_scheme_code,NULL,
 CASE WHEN f.classifier_scheme_code IS NOT NULL THEN 'SELECT' WHEN f.logical_type='BOOLEAN' THEN 'CHECKBOX' WHEN f.logical_type='DATE' THEN 'DATE' WHEN f.logical_type IN('DECIMAL','INTEGER') THEN 'NUMBER' ELSE 'TEXT' END,
 1,CASE WHEN f.semantic_role IN('CONTENT','LOCALIZED_TEXT') THEN 1 ELSE 0 END,
 CASE WHEN f.semantic_role IN('ATTRIBUTE','CONTENT','LOCALIZED_TEXT') THEN 1 ELSE 0 END,1,CASE WHEN f.semantic_role IN('ATTRIBUTE','CONTENT') THEN 1 ELSE 0 END,
 NULL,'APPROVED'
FROM platform.site_contract_field f JOIN platform.site_contract_dataset d ON d.contract_dataset_id=f.contract_dataset_id
JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id AND r.revision=7
JOIN platform.contract_table_definition td ON td.logical_table_code=UPPER(REPLACE(d.dataset_code,N'-',N'_')) AND td.revision=7
WHERE NOT EXISTS(SELECT 1 FROM platform.contract_field_definition x WHERE x.table_definition_id=td.table_definition_id AND x.field_name=f.field_name);

/* Registry is intentionally metadata-only; it never inserts business rows. */
