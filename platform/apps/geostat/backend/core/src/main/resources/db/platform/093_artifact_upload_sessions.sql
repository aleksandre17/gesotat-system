/* Durable resumable artifact upload sessions, tenant quota reservations, and part checkpoints. */
IF OBJECT_ID(N'ingest.artifact_upload_quota',N'U') IS NULL
CREATE TABLE ingest.artifact_upload_quota(
  tenant_key_hash CHAR(64) NOT NULL PRIMARY KEY,
  reserved_bytes BIGINT NOT NULL CONSTRAINT df_artifact_upload_quota_reserved DEFAULT 0,
  active_sessions INT NOT NULL CONSTRAINT df_artifact_upload_quota_sessions DEFAULT 0,
  updated_at DATETIME2 NOT NULL CONSTRAINT df_artifact_upload_quota_updated DEFAULT SYSUTCDATETIME(),
  CONSTRAINT ck_artifact_upload_quota_hash CHECK(LEN(tenant_key_hash)=64 AND tenant_key_hash NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2),
  CONSTRAINT ck_artifact_upload_quota_reserved CHECK(reserved_bytes>=0),
  CONSTRAINT ck_artifact_upload_quota_sessions CHECK(active_sessions>=0)
);

IF OBJECT_ID(N'ingest.artifact_upload_session',N'U') IS NULL
CREATE TABLE ingest.artifact_upload_session(
  upload_session_id UNIQUEIDENTIFIER NOT NULL CONSTRAINT df_artifact_upload_session_id DEFAULT NEWSEQUENTIALID() PRIMARY KEY,
  tenant_key_hash CHAR(64) NOT NULL,
  owner_key_hash CHAR(64) NOT NULL,
  idempotency_key NVARCHAR(128) COLLATE Latin1_General_BIN2 NOT NULL,
  request_fingerprint CHAR(64) NOT NULL,
  package_code NVARCHAR(160) NOT NULL,
  contract_code NVARCHAR(120) NOT NULL,
  contract_revision INT NOT NULL,
  dataset_code NVARCHAR(120) NOT NULL,
  dataset_version_id BIGINT NOT NULL,
  expected_bytes BIGINT NOT NULL,
  part_size BIGINT NOT NULL,
  expected_parts INT NOT NULL,
  received_bytes BIGINT NOT NULL CONSTRAINT df_artifact_upload_session_received DEFAULT 0,
  status VARCHAR(24) NOT NULL CONSTRAINT df_artifact_upload_session_status DEFAULT 'OPEN',
  quota_released BIT NOT NULL CONSTRAINT df_artifact_upload_session_quota_released DEFAULT 0,
  last_error_code NVARCHAR(64) NULL,
  artifact_manifest_id BIGINT NULL,
  package_checksum CHAR(64) NULL,
  expires_at DATETIME2 NOT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_artifact_upload_session_created DEFAULT SYSUTCDATETIME(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_artifact_upload_session_updated DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_artifact_upload_session_idempotency UNIQUE(tenant_key_hash,owner_key_hash,idempotency_key),
  CONSTRAINT ck_artifact_upload_session_hashes CHECK(tenant_key_hash NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2 AND owner_key_hash NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2 AND request_fingerprint NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2),
  CONSTRAINT ck_artifact_upload_session_sizes CHECK(expected_bytes>0 AND part_size>0 AND expected_parts>0 AND received_bytes>=0 AND received_bytes<=expected_bytes),
  CONSTRAINT ck_artifact_upload_session_contract CHECK(contract_revision>0 AND dataset_version_id>0),
  CONSTRAINT ck_artifact_upload_session_state CHECK(status IN('OPEN','RETRYABLE','PROCESSING','COMMITTED','REJECTED','CANCELLED','EXPIRED')),
  CONSTRAINT ck_artifact_upload_session_quota CHECK((quota_released=0 AND status IN('OPEN','RETRYABLE','PROCESSING')) OR quota_released=1),
  CONSTRAINT ck_artifact_upload_session_receipt CHECK((status<>'COMMITTED') OR (artifact_manifest_id IS NOT NULL AND package_checksum IS NOT NULL))
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact_upload_session') AND name=N'ix_artifact_upload_session_expiry')
  CREATE INDEX ix_artifact_upload_session_expiry ON ingest.artifact_upload_session(status,expires_at) INCLUDE(tenant_key_hash,expected_bytes,quota_released);

IF OBJECT_ID(N'ingest.artifact_upload_part',N'U') IS NULL
CREATE TABLE ingest.artifact_upload_part(
  upload_session_id UNIQUEIDENTIFIER NOT NULL,
  part_number INT NOT NULL,
  sha256 CHAR(64) NOT NULL,
  byte_size BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL CONSTRAINT df_artifact_upload_part_status DEFAULT 'RECEIVING',
  received_at DATETIME2 NULL,
  CONSTRAINT pk_artifact_upload_part PRIMARY KEY(upload_session_id,part_number),
  CONSTRAINT fk_artifact_upload_part_session FOREIGN KEY(upload_session_id) REFERENCES ingest.artifact_upload_session(upload_session_id),
  CONSTRAINT ck_artifact_upload_part_number CHECK(part_number>=1),
  CONSTRAINT ck_artifact_upload_part_sha CHECK(LEN(sha256)=64 AND sha256 NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2),
  CONSTRAINT ck_artifact_upload_part_size CHECK(byte_size>0),
  CONSTRAINT ck_artifact_upload_part_state CHECK(status IN('RECEIVING','RECEIVED')),
  CONSTRAINT ck_artifact_upload_part_receipt CHECK((status='RECEIVING' AND received_at IS NULL) OR (status='RECEIVED' AND received_at IS NOT NULL))
);
