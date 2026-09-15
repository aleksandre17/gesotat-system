/* Final unified data-plane completion — geostat-data / additive only. */
IF NOT EXISTS (SELECT 1 FROM sys.schemas WHERE name=N'audit') EXEC(N'CREATE SCHEMA audit');
IF OBJECT_ID(N'audit.event',N'U') IS NULL CREATE TABLE audit.event(
 event_id BIGINT IDENTITY PRIMARY KEY, event_time DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), actor_type VARCHAR(32) NOT NULL,
 actor_id NVARCHAR(255) NULL, action_code VARCHAR(64) NOT NULL, object_type VARCHAR(64) NOT NULL, object_id NVARCHAR(255) NULL,
 structure_id BIGINT NULL, batch_id BIGINT NULL, before_hash CHAR(64) NULL, after_hash CHAR(64) NULL,
 reason NVARCHAR(2000) NULL, client_ref NVARCHAR(255) NULL, result VARCHAR(24) NOT NULL, metadata_json NVARCHAR(MAX) NULL,
 CONSTRAINT ck_audit_event_metadata_json CHECK(metadata_json IS NULL OR ISJSON(metadata_json)=1)
);
IF OBJECT_ID(N'audit.access_decision',N'U') IS NULL CREATE TABLE audit.access_decision(
 decision_id BIGINT IDENTITY PRIMARY KEY, principal_id NVARCHAR(255) NOT NULL, object_type VARCHAR(64) NOT NULL,
 object_id NVARCHAR(255) NOT NULL, purpose_code VARCHAR(64) NOT NULL, policy_code VARCHAR(128) NOT NULL,
 decision VARCHAR(24) NOT NULL, decided_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), expires_at DATETIME2 NULL,
 evidence_json NVARCHAR(MAX) NULL, CONSTRAINT ck_audit_access_evidence_json CHECK(evidence_json IS NULL OR ISJSON(evidence_json)=1)
);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_audit_event_object' AND object_id=OBJECT_ID(N'audit.event')) CREATE INDEX ix_audit_event_object ON audit.event(object_type,object_id,event_time);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_audit_event_batch' AND object_id=OBJECT_ID(N'audit.event')) CREATE INDEX ix_audit_event_batch ON audit.event(batch_id,event_time);
