/* Final unified archive completion — geostat-archive / additive only. */
IF OBJECT_ID(N'archive.retention_register',N'U') IS NULL CREATE TABLE archive.retention_register(
 retention_id BIGINT IDENTITY PRIMARY KEY, object_type VARCHAR(64) NOT NULL, object_id NVARCHAR(255) NOT NULL,
 retention_policy_code NVARCHAR(128) NOT NULL, retain_until DATETIME2 NULL, legal_hold_flag BIT NOT NULL DEFAULT 0,
 destruction_eligible_at DATETIME2 NULL, destruction_event_id NVARCHAR(255) NULL, status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE',
 created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(), CONSTRAINT uq_archive_retention_object UNIQUE(object_type,object_id)
);
IF OBJECT_ID(N'archive.payload_pointer',N'U') IS NULL CREATE TABLE archive.payload_pointer(
 payload_pointer_id BIGINT IDENTITY PRIMARY KEY, source_record_id BIGINT NULL, document_id BIGINT NULL,
 storage_uri NVARCHAR(2048) NOT NULL, storage_tier VARCHAR(32) NOT NULL DEFAULT 'ARCHIVE', checksum CHAR(64) NOT NULL,
 encryption_code VARCHAR(64) NULL, byte_size BIGINT NULL, immutable_flag BIT NOT NULL DEFAULT 1, created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
 CONSTRAINT uq_archive_payload_pointer UNIQUE(storage_uri,checksum)
);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_archive_retention_due' AND object_id=OBJECT_ID(N'archive.retention_register')) CREATE INDEX ix_archive_retention_due ON archive.retention_register(destruction_eligible_at,status,legal_hold_flag);
