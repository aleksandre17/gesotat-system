/* Run in geostat-data (Data Plane).
   Artifact registry and row<->artifact attachment edges.
   - artifact_object: one row per unique byte content (SHA-256); bytes stay in Object Storage.
   - artifact_manifest: one immutable accepted package manifest.
   - artifact_version: an original path inside a manifest, pointing at its content object.
   - artifact_attachment: typed edge entity -> artifact_version inside one snapshot.
   ingest.artifact (002) remains the per-batch upload receipt; it is a different grain. */

IF OBJECT_ID(N'ingest.artifact_object',N'U') IS NULL
CREATE TABLE ingest.artifact_object(
  artifact_object_id BIGINT IDENTITY PRIMARY KEY,
  sha256 CHAR(64) NOT NULL,
  byte_size BIGINT NOT NULL,
  media_type NVARCHAR(255) NOT NULL,
  bucket NVARCHAR(63) NOT NULL,
  object_key NVARCHAR(1024) NOT NULL,
  verification_status VARCHAR(24) NOT NULL DEFAULT 'REGISTERED',
  verified_at DATETIME2 NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_artifact_object_sha UNIQUE(sha256),
  CONSTRAINT uq_artifact_object_location UNIQUE(bucket,object_key),
  CONSTRAINT ck_artifact_object_sha CHECK(LEN(sha256)=64 AND sha256 NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2),
  CONSTRAINT ck_artifact_object_size CHECK(byte_size>=0),
  CONSTRAINT ck_artifact_object_status CHECK(verification_status IN('REGISTERED','VERIFIED','MISSING','CHECKSUM_MISMATCH'))
);

IF OBJECT_ID(N'ingest.artifact_manifest',N'U') IS NULL
CREATE TABLE ingest.artifact_manifest(
  artifact_manifest_id BIGINT IDENTITY PRIMARY KEY,
  manifest_schema VARCHAR(64) NOT NULL,
  package_code NVARCHAR(160) NOT NULL,
  package_checksum CHAR(64) NOT NULL,
  generator_version NVARCHAR(64) NOT NULL,
  entry_count INT NOT NULL,
  source_reference NVARCHAR(1024) NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_artifact_manifest_checksum UNIQUE(package_checksum),
  CONSTRAINT ck_artifact_manifest_checksum CHECK(LEN(package_checksum)=64 AND package_checksum NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2),
  CONSTRAINT ck_artifact_manifest_entries CHECK(entry_count>=0)
);

IF OBJECT_ID(N'ingest.artifact_version',N'U') IS NULL
CREATE TABLE ingest.artifact_version(
  artifact_version_id BIGINT IDENTITY PRIMARY KEY,
  artifact_manifest_id BIGINT NOT NULL,
  original_path NVARCHAR(1024) NOT NULL,
  original_name NVARCHAR(512) NOT NULL,
  artifact_object_id BIGINT NOT NULL,
  CONSTRAINT uq_artifact_version_path UNIQUE(artifact_manifest_id,original_path),
  CONSTRAINT fk_artifact_version_manifest FOREIGN KEY(artifact_manifest_id) REFERENCES ingest.artifact_manifest(artifact_manifest_id),
  CONSTRAINT fk_artifact_version_object FOREIGN KEY(artifact_object_id) REFERENCES ingest.artifact_object(artifact_object_id)
);

IF OBJECT_ID(N'entity.artifact_attachment',N'U') IS NULL
CREATE TABLE entity.artifact_attachment(
  artifact_attachment_id BIGINT IDENTITY PRIMARY KEY,
  dataset_snapshot_id BIGINT NOT NULL,
  entity_id BIGINT NOT NULL,
  relation_code NVARCHAR(64) NOT NULL,
  artifact_role VARCHAR(24) NOT NULL,
  language_tag VARCHAR(35) NOT NULL DEFAULT 'und',
  ordinal INT NOT NULL DEFAULT 1,
  artifact_version_id BIGINT NOT NULL,
  source_record_id BIGINT NOT NULL,
  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_artifact_attachment_slot UNIQUE(entity_id,relation_code,language_tag,ordinal),
  CONSTRAINT fk_artifact_attachment_snapshot FOREIGN KEY(dataset_snapshot_id) REFERENCES publication.dataset_snapshot(dataset_snapshot_id),
  CONSTRAINT fk_artifact_attachment_entity FOREIGN KEY(entity_id) REFERENCES entity.entity_record(entity_id),
  CONSTRAINT fk_artifact_attachment_version FOREIGN KEY(artifact_version_id) REFERENCES ingest.artifact_version(artifact_version_id),
  CONSTRAINT fk_artifact_attachment_source FOREIGN KEY(source_record_id) REFERENCES raw.source_record(source_record_id),
  CONSTRAINT ck_artifact_attachment_role CHECK(artifact_role IN('PRIMARY','SUPPORTING','DERIVED','PREVIEW')),
  CONSTRAINT ck_artifact_attachment_ordinal CHECK(ordinal>=1)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'entity.artifact_attachment') AND name=N'ix_artifact_attachment_snapshot')
  CREATE INDEX ix_artifact_attachment_snapshot ON entity.artifact_attachment(dataset_snapshot_id,relation_code);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'entity.artifact_attachment') AND name=N'ix_artifact_attachment_version')
  CREATE INDEX ix_artifact_attachment_version ON entity.artifact_attachment(artifact_version_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact_version') AND name=N'ix_artifact_version_object')
  CREATE INDEX ix_artifact_version_object ON ingest.artifact_version(artifact_object_id);

/* A published snapshot cannot gain, lose or mutate attachments. */
IF OBJECT_ID(N'entity.tr_artifact_attachment_published_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER entity.tr_artifact_attachment_published_immutable ON entity.artifact_attachment AFTER INSERT, UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (SELECT 1 FROM inserted i JOIN publication.dataset_snapshot s ON s.dataset_snapshot_id=i.dataset_snapshot_id WHERE s.status=''PUBLISHED'')
     OR EXISTS (SELECT 1 FROM deleted d JOIN publication.dataset_snapshot s ON s.dataset_snapshot_id=d.dataset_snapshot_id WHERE s.status=''PUBLISHED'')
    THROW 51022, ''Published artifact attachments are immutable.'', 1;
END');

/* Manifest entries and content identity are append-only evidence. */
IF OBJECT_ID(N'ingest.tr_artifact_version_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER ingest.tr_artifact_version_immutable ON ingest.artifact_version AFTER UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  THROW 51023, ''Artifact versions are immutable manifest evidence.'', 1;
END');

/* Reconciliation surface: every attachment with its content object state. */
IF OBJECT_ID(N'entity.v_artifact_attachment_reconciliation',N'V') IS NULL EXEC(N'
CREATE VIEW entity.v_artifact_attachment_reconciliation AS
SELECT a.dataset_snapshot_id, a.relation_code, a.language_tag, a.ordinal, a.entity_id, e.external_key, e.record_type,
       v.original_path, o.sha256, o.byte_size, o.media_type, o.bucket, o.object_key, o.verification_status
FROM entity.artifact_attachment a
JOIN entity.entity_record e ON e.entity_id=a.entity_id
JOIN ingest.artifact_version v ON v.artifact_version_id=a.artifact_version_id
JOIN ingest.artifact_object o ON o.artifact_object_id=v.artifact_object_id');
