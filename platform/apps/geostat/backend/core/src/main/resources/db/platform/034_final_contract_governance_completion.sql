/* Final unified contract completion — geostat-system / additive only. */
IF OBJECT_ID(N'platform.contract_object_alias',N'U') IS NULL CREATE TABLE platform.contract_object_alias(
 alias_id BIGINT IDENTITY PRIMARY KEY, object_type VARCHAR(48) NOT NULL, object_id BIGINT NOT NULL,
 namespace_id BIGINT NULL, alias_name NVARCHAR(255) NOT NULL, alias_version INT NOT NULL DEFAULT 1,
 valid_from DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), valid_to DATETIME2 NULL, is_preferred BIT NOT NULL DEFAULT 0,
 reason NVARCHAR(1000) NULL, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), created_by NVARCHAR(128) NOT NULL DEFAULT SUSER_SNAME(),
 CONSTRAINT uq_contract_object_alias UNIQUE(object_type,alias_name,alias_version)
);
IF OBJECT_ID(N'platform.contract_approval',N'U') IS NULL CREATE TABLE platform.contract_approval(
 approval_id BIGINT IDENTITY PRIMARY KEY, structure_id BIGINT NOT NULL, revision INT NOT NULL,
 approval_type VARCHAR(32) NOT NULL, decision VARCHAR(24) NOT NULL, approver_principal NVARCHAR(255) NOT NULL,
 decision_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), comment NVARCHAR(2000) NULL, evidence_uri NVARCHAR(2048) NULL,
 approval_hash CHAR(64) NULL, CONSTRAINT fk_contract_approval_structure FOREIGN KEY(structure_id) REFERENCES platform.contract_structure(structure_id)
);
IF OBJECT_ID(N'platform.contract_migration',N'U') IS NULL CREATE TABLE platform.contract_migration(
 migration_id BIGINT IDENTITY PRIMARY KEY, structure_id BIGINT NOT NULL, from_revision INT NULL, to_revision INT NOT NULL,
 migration_type VARCHAR(32) NOT NULL, forward_plan_json NVARCHAR(MAX) NOT NULL, rollback_plan_json NVARCHAR(MAX) NULL,
 breaking_change_flag BIT NOT NULL DEFAULT 0, executed_at DATETIME2 NULL, executed_by NVARCHAR(128) NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'PLANNED', migration_hash CHAR(64) NULL,
 CONSTRAINT fk_contract_migration_structure FOREIGN KEY(structure_id) REFERENCES platform.contract_structure(structure_id),
 CONSTRAINT ck_contract_migration_forward_json CHECK(ISJSON(forward_plan_json)=1)
);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_contract_approval_structure' AND object_id=OBJECT_ID(N'platform.contract_approval')) CREATE INDEX ix_contract_approval_structure ON platform.contract_approval(structure_id,revision,approval_type,decision);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_contract_alias_object' AND object_id=OBJECT_ID(N'platform.contract_object_alias')) CREATE INDEX ix_contract_alias_object ON platform.contract_object_alias(object_type,object_id,is_preferred);
