/* geostat-system: control-plane metadata. Existing legacy tables are untouched. */
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name = N'platform') EXEC(N'CREATE SCHEMA platform');

IF OBJECT_ID(N'platform.data_product', N'U') IS NULL CREATE TABLE platform.data_product (
  product_id BIGINT IDENTITY PRIMARY KEY, product_code NVARCHAR(120) NOT NULL UNIQUE,
  page_id BIGINT NULL, title_ka NVARCHAR(255) NOT NULL, title_en NVARCHAR(255) NULL,
  owner_user_id BIGINT NULL, lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  sensitivity VARCHAR(24) NOT NULL DEFAULT 'INTERNAL', created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT fk_platform_product_page FOREIGN KEY(page_id) REFERENCES dbo.page_nodes(id),
  CONSTRAINT fk_platform_product_owner FOREIGN KEY(owner_user_id) REFERENCES dbo.users(id)
);

IF OBJECT_ID(N'platform.dataset', N'U') IS NULL CREATE TABLE platform.dataset (
  dataset_id BIGINT IDENTITY PRIMARY KEY, product_id BIGINT NOT NULL, dataset_code NVARCHAR(120) NOT NULL,
  dataset_family VARCHAR(24) NOT NULL, business_grain NVARCHAR(500) NULL, retention_policy NVARCHAR(64) NOT NULL DEFAULT 'ARCHIVE_1Y', lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_platform_dataset UNIQUE(product_id,dataset_code), CONSTRAINT fk_platform_dataset_product FOREIGN KEY(product_id) REFERENCES platform.data_product(product_id)
);

IF OBJECT_ID(N'platform.dataset_version', N'U') IS NULL CREATE TABLE platform.dataset_version (
  dataset_version_id BIGINT IDENTITY PRIMARY KEY, dataset_id BIGINT NOT NULL, version INT NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'DRAFT', contract_checksum CHAR(64) NULL,
  effective_from DATETIME2 NULL, effective_to DATETIME2 NULL, approved_by_user_id BIGINT NULL, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_platform_dataset_version UNIQUE(dataset_id,version), CONSTRAINT fk_platform_version_dataset FOREIGN KEY(dataset_id) REFERENCES platform.dataset(dataset_id)
);

IF OBJECT_ID(N'platform.attribute', N'U') IS NULL CREATE TABLE platform.attribute (
  attribute_id BIGINT IDENTITY PRIMARY KEY, dataset_version_id BIGINT NOT NULL, attribute_code NVARCHAR(128) NOT NULL,
  logical_type VARCHAR(24) NOT NULL, attribute_role VARCHAR(24) NOT NULL, cardinality VARCHAR(16) NOT NULL DEFAULT 'SINGLE', required BIT NOT NULL DEFAULT 0,
  searchable BIT NOT NULL DEFAULT 0, filterable BIT NOT NULL DEFAULT 0, groupable BIT NOT NULL DEFAULT 0,
  label_ka NVARCHAR(255) NOT NULL, label_en NVARCHAR(255) NULL, definition_ka NVARCHAR(MAX) NULL, definition_en NVARCHAR(MAX) NULL, unit_code NVARCHAR(64) NULL, json_path NVARCHAR(512) NULL,
  CONSTRAINT uq_platform_attribute UNIQUE(dataset_version_id,attribute_code), CONSTRAINT fk_platform_attribute_version FOREIGN KEY(dataset_version_id) REFERENCES platform.dataset_version(dataset_version_id)
);
IF OBJECT_ID(N'platform.validation_rule', N'U') IS NULL CREATE TABLE platform.validation_rule (
  validation_rule_id BIGINT IDENTITY PRIMARY KEY, dataset_version_id BIGINT NOT NULL, rule_code NVARCHAR(128) NOT NULL,
  rule_type VARCHAR(32) NOT NULL, severity VARCHAR(16) NOT NULL, expression_json NVARCHAR(MAX) NOT NULL,
  active BIT NOT NULL DEFAULT 1, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_platform_validation_rule UNIQUE(dataset_version_id,rule_code),
  CONSTRAINT fk_platform_validation_rule_version FOREIGN KEY(dataset_version_id) REFERENCES platform.dataset_version(dataset_version_id)
);

IF OBJECT_ID(N'platform.relationship_type', N'U') IS NULL CREATE TABLE platform.relationship_type (
  relationship_type_id BIGINT IDENTITY PRIMARY KEY, code NVARCHAR(64) NOT NULL UNIQUE, category VARCHAR(32) NOT NULL, directional BIT NOT NULL DEFAULT 1, transitive BIT NOT NULL DEFAULT 0, temporal BIT NOT NULL DEFAULT 0, description NVARCHAR(1000) NULL
);

IF OBJECT_ID(N'platform.dataset_relationship', N'U') IS NULL CREATE TABLE platform.dataset_relationship (
  relationship_id BIGINT IDENTITY PRIMARY KEY, from_dataset_version_id BIGINT NOT NULL, to_dataset_version_id BIGINT NOT NULL, relationship_type_id BIGINT NOT NULL,
  source_attribute_id BIGINT NULL, target_attribute_id BIGINT NULL, cardinality VARCHAR(24) NOT NULL, required BIT NOT NULL DEFAULT 0, enforcement_policy VARCHAR(24) NOT NULL DEFAULT 'VALIDATE', load_priority INT NOT NULL DEFAULT 100,
  CONSTRAINT fk_platform_rel_from FOREIGN KEY(from_dataset_version_id) REFERENCES platform.dataset_version(dataset_version_id),
  CONSTRAINT fk_platform_rel_to FOREIGN KEY(to_dataset_version_id) REFERENCES platform.dataset_version(dataset_version_id),
  CONSTRAINT fk_platform_rel_type FOREIGN KEY(relationship_type_id) REFERENCES platform.relationship_type(relationship_type_id)
);

IF OBJECT_ID(N'platform.classification_scheme', N'U') IS NULL CREATE TABLE platform.classification_scheme (
  scheme_id BIGINT IDENTITY PRIMARY KEY, scheme_code NVARCHAR(120) NOT NULL UNIQUE, title_ka NVARCHAR(255) NOT NULL, title_en NVARCHAR(255) NULL, standard_reference NVARCHAR(255) NULL, owner_user_id BIGINT NULL, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);
IF OBJECT_ID(N'platform.classification_version', N'U') IS NULL CREATE TABLE platform.classification_version (
  classification_version_id BIGINT IDENTITY PRIMARY KEY, scheme_id BIGINT NOT NULL, version NVARCHAR(64) NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'DRAFT', valid_from DATE NULL, valid_to DATE NULL,
  CONSTRAINT uq_platform_classification_version UNIQUE(scheme_id,version), CONSTRAINT fk_platform_classification_version_scheme FOREIGN KEY(scheme_id) REFERENCES platform.classification_scheme(scheme_id)
);
IF OBJECT_ID(N'platform.classification_item', N'U') IS NULL CREATE TABLE platform.classification_item (
  classification_item_id BIGINT IDENTITY PRIMARY KEY, classification_version_id BIGINT NOT NULL, code NVARCHAR(128) NOT NULL, label_ka NVARCHAR(255) NOT NULL, label_en NVARCHAR(255) NULL, status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', sort_order INT NULL, valid_from DATE NULL, valid_to DATE NULL,
  CONSTRAINT uq_platform_classification_item UNIQUE(classification_version_id,code), CONSTRAINT fk_platform_classification_item_version FOREIGN KEY(classification_version_id) REFERENCES platform.classification_version(classification_version_id)
);
IF OBJECT_ID(N'platform.classification_hierarchy', N'U') IS NULL CREATE TABLE platform.classification_hierarchy (
  parent_item_id BIGINT NOT NULL, child_item_id BIGINT NOT NULL, hierarchy_type VARCHAR(32) NOT NULL DEFAULT 'PARENT_OF', valid_from DATE NULL, valid_to DATE NULL,
  CONSTRAINT pk_platform_classification_hierarchy PRIMARY KEY(parent_item_id,child_item_id,hierarchy_type),
  CONSTRAINT fk_platform_hierarchy_parent FOREIGN KEY(parent_item_id) REFERENCES platform.classification_item(classification_item_id),
  CONSTRAINT fk_platform_hierarchy_child FOREIGN KEY(child_item_id) REFERENCES platform.classification_item(classification_item_id)
);
IF OBJECT_ID(N'platform.classification_alias', N'U') IS NULL CREATE TABLE platform.classification_alias (
  alias_id BIGINT IDENTITY PRIMARY KEY, classification_item_id BIGINT NOT NULL, external_system_code NVARCHAR(120) NOT NULL, external_code NVARCHAR(255) NOT NULL, valid_from DATE NULL, valid_to DATE NULL,
  CONSTRAINT uq_platform_classification_alias UNIQUE(external_system_code,external_code), CONSTRAINT fk_platform_alias_item FOREIGN KEY(classification_item_id) REFERENCES platform.classification_item(classification_item_id)
);

IF OBJECT_ID(N'platform.dimension', N'U') IS NULL CREATE TABLE platform.dimension (dimension_id BIGINT IDENTITY PRIMARY KEY, dimension_code NVARCHAR(120) NOT NULL UNIQUE, classification_scheme_id BIGINT NULL, value_type VARCHAR(24) NOT NULL, title_ka NVARCHAR(255) NOT NULL, title_en NVARCHAR(255) NULL, CONSTRAINT fk_platform_dimension_scheme FOREIGN KEY(classification_scheme_id) REFERENCES platform.classification_scheme(scheme_id));
IF OBJECT_ID(N'platform.measure', N'U') IS NULL CREATE TABLE platform.measure (measure_id BIGINT IDENTITY PRIMARY KEY, measure_code NVARCHAR(120) NOT NULL UNIQUE, value_type VARCHAR(24) NOT NULL, unit_code NVARCHAR(64) NULL, decimal_precision INT NULL, aggregation_default VARCHAR(24) NOT NULL, title_ka NVARCHAR(255) NOT NULL, title_en NVARCHAR(255) NULL);
IF OBJECT_ID(N'platform.metric', N'U') IS NULL CREATE TABLE platform.metric (metric_id BIGINT IDENTITY PRIMARY KEY, metric_code NVARCHAR(120) NOT NULL UNIQUE, source_dataset_id BIGINT NOT NULL, measure_id BIGINT NOT NULL, aggregation VARCHAR(24) NOT NULL, allowed_dimension_set_json NVARCHAR(MAX) NULL, status VARCHAR(24) NOT NULL DEFAULT 'DRAFT', CONSTRAINT fk_platform_metric_dataset FOREIGN KEY(source_dataset_id) REFERENCES platform.dataset(dataset_id), CONSTRAINT fk_platform_metric_measure FOREIGN KEY(measure_id) REFERENCES platform.measure(measure_id));

IF OBJECT_ID(N'platform.source_system', N'U') IS NULL CREATE TABLE platform.source_system (source_system_id BIGINT IDENTITY PRIMARY KEY, source_code NVARCHAR(120) NOT NULL UNIQUE, source_type VARCHAR(24) NOT NULL, title NVARCHAR(255) NOT NULL, trust_level VARCHAR(24) NOT NULL DEFAULT 'UNTRUSTED', enabled BIT NOT NULL DEFAULT 1);
/* Credentials live in a secret manager/environment; this table stores only the stable reference. */
IF OBJECT_ID(N'platform.source_connection', N'U') IS NULL CREATE TABLE platform.source_connection (
  source_connection_id BIGINT IDENTITY PRIMARY KEY, source_system_id BIGINT NOT NULL, connection_kind VARCHAR(32) NOT NULL,
  endpoint NVARCHAR(1024) NOT NULL, database_name NVARCHAR(255) NULL, username NVARCHAR(255) NULL, secret_reference NVARCHAR(255) NOT NULL,
  encryption_mode VARCHAR(32) NOT NULL DEFAULT 'TLS', enabled BIT NOT NULL DEFAULT 1,
  CONSTRAINT uq_platform_source_connection UNIQUE(source_system_id,endpoint,database_name),
  CONSTRAINT fk_platform_source_connection_system FOREIGN KEY(source_system_id) REFERENCES platform.source_system(source_system_id)
);
IF COL_LENGTH(N'platform.source_connection', N'username') IS NULL ALTER TABLE platform.source_connection ADD username NVARCHAR(255) NULL;
IF OBJECT_ID(N'platform.ingestion_contract', N'U') IS NULL CREATE TABLE platform.ingestion_contract (contract_id BIGINT IDENTITY PRIMARY KEY, source_system_id BIGINT NOT NULL, dataset_id BIGINT NOT NULL, format_profile VARCHAR(48) NOT NULL, ingestion_method VARCHAR(32) NOT NULL, auto_publish BIT NOT NULL DEFAULT 0, status VARCHAR(24) NOT NULL DEFAULT 'DRAFT', CONSTRAINT uq_platform_contract UNIQUE(source_system_id,dataset_id), CONSTRAINT fk_platform_contract_source FOREIGN KEY(source_system_id) REFERENCES platform.source_system(source_system_id), CONSTRAINT fk_platform_contract_dataset FOREIGN KEY(dataset_id) REFERENCES platform.dataset(dataset_id));
/* A contract may read many source tables/files and route each into a shared logical dataset. */
IF OBJECT_ID(N'platform.contract_source', N'U') IS NULL CREATE TABLE platform.contract_source (
  contract_source_id BIGINT IDENTITY PRIMARY KEY, contract_id BIGINT NOT NULL, source_locator NVARCHAR(1024) NOT NULL,
  source_kind VARCHAR(32) NOT NULL, target_dataset_version_id BIGINT NOT NULL, source_key_expression NVARCHAR(1024) NULL,
  row_role VARCHAR(32) NOT NULL DEFAULT 'ENTITY', load_order INT NOT NULL DEFAULT 100, mapping_spec_json NVARCHAR(MAX) NOT NULL,
  active BIT NOT NULL DEFAULT 1,
  CONSTRAINT uq_platform_contract_source UNIQUE(contract_id,source_locator),
  CONSTRAINT fk_platform_contract_source_contract FOREIGN KEY(contract_id) REFERENCES platform.ingestion_contract(contract_id),
  CONSTRAINT fk_platform_contract_source_version FOREIGN KEY(target_dataset_version_id) REFERENCES platform.dataset_version(dataset_version_id)
);

IF OBJECT_ID(N'platform.release', N'U') IS NULL CREATE TABLE platform.release (release_id BIGINT IDENTITY PRIMARY KEY, product_id BIGINT NOT NULL, release_version INT NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'DRAFT', published_at DATETIME2 NULL, published_by_user_id BIGINT NULL, previous_release_id BIGINT NULL, CONSTRAINT uq_platform_release UNIQUE(product_id,release_version), CONSTRAINT fk_platform_release_product FOREIGN KEY(product_id) REFERENCES platform.data_product(product_id));
/* Transactional outbox makes cross-plane publication recoverable without distributed transactions. */
IF OBJECT_ID(N'platform.outbox_event', N'U') IS NULL CREATE TABLE platform.outbox_event (
  event_id BIGINT IDENTITY PRIMARY KEY, aggregate_type VARCHAR(48) NOT NULL, aggregate_id BIGINT NOT NULL,
  event_type VARCHAR(64) NOT NULL, payload_json NVARCHAR(MAX) NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0, available_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), processed_at DATETIME2 NULL, last_error NVARCHAR(2000) NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);
IF COL_LENGTH(N'platform.outbox_event', N'last_error') IS NULL ALTER TABLE platform.outbox_event ADD last_error NVARCHAR(2000) NULL;
IF OBJECT_ID(N'platform.schema_migration', N'U') IS NULL CREATE TABLE platform.schema_migration (
  migration_id NVARCHAR(255) NOT NULL PRIMARY KEY, checksum CHAR(64) NOT NULL, applied_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), applied_by NVARCHAR(128) NOT NULL DEFAULT SUSER_SNAME()
);
IF OBJECT_ID(N'platform.job_lease', N'U') IS NULL CREATE TABLE platform.job_lease (
  job_name NVARCHAR(128) NOT NULL PRIMARY KEY, lease_owner NVARCHAR(128) NOT NULL, lease_until DATETIME2 NOT NULL, updated_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME()
);
IF OBJECT_ID(N'platform.visualization_definition', N'U') IS NULL CREATE TABLE platform.visualization_definition (visualization_id BIGINT IDENTITY PRIMARY KEY, product_id BIGINT NOT NULL, visualization_code NVARCHAR(120) NOT NULL, metric_id BIGINT NOT NULL, chart_type VARCHAR(32) NOT NULL, config_json NVARCHAR(MAX) NOT NULL, status VARCHAR(24) NOT NULL DEFAULT 'DRAFT', CONSTRAINT uq_platform_visualization UNIQUE(product_id,visualization_code), CONSTRAINT fk_platform_visualization_product FOREIGN KEY(product_id) REFERENCES platform.data_product(product_id), CONSTRAINT fk_platform_visualization_metric FOREIGN KEY(metric_id) REFERENCES platform.metric(metric_id));

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_platform_dataset_product' AND object_id=OBJECT_ID(N'platform.dataset')) CREATE INDEX ix_platform_dataset_product ON platform.dataset(product_id,dataset_code);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_platform_attribute_version' AND object_id=OBJECT_ID(N'platform.attribute')) CREATE INDEX ix_platform_attribute_version ON platform.attribute(dataset_version_id,attribute_code);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_platform_validation_rule' AND object_id=OBJECT_ID(N'platform.validation_rule')) CREATE INDEX ix_platform_validation_rule ON platform.validation_rule(dataset_version_id,active,rule_code);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_platform_contract_source_load' AND object_id=OBJECT_ID(N'platform.contract_source')) CREATE INDEX ix_platform_contract_source_load ON platform.contract_source(contract_id,load_order,active);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_platform_source_connection' AND object_id=OBJECT_ID(N'platform.source_connection')) CREATE INDEX ix_platform_source_connection ON platform.source_connection(source_system_id,enabled);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_platform_outbox_pending' AND object_id=OBJECT_ID(N'platform.outbox_event')) CREATE INDEX ix_platform_outbox_pending ON platform.outbox_event(status,available_at,event_id);
