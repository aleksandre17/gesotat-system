/* Bounded Data Plane audit evidence for periodic object existence/checksum verification. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF COL_LENGTH(N'ingest.artifact_object',N'last_audit_attempt_at') IS NULL
  ALTER TABLE ingest.artifact_object ADD last_audit_attempt_at DATETIME2 NULL;

IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact_object') AND name=N'ix_artifact_object_audit_due')
  CREATE INDEX ix_artifact_object_audit_due ON ingest.artifact_object(last_audit_attempt_at,artifact_object_id)
    INCLUDE(sha256,byte_size,bucket,object_key,verification_status);

IF OBJECT_ID(N'ingest.artifact_object_audit_run',N'U') IS NULL
CREATE TABLE ingest.artifact_object_audit_run(
  audit_run_id BIGINT IDENTITY PRIMARY KEY,
  status VARCHAR(16) NOT NULL CONSTRAINT df_artifact_object_audit_run_status DEFAULT 'RUNNING',
  objects_examined INT NOT NULL CONSTRAINT df_artifact_object_audit_run_examined DEFAULT 0,
  objects_verified INT NOT NULL CONSTRAINT df_artifact_object_audit_run_verified DEFAULT 0,
  objects_missing INT NOT NULL CONSTRAINT df_artifact_object_audit_run_missing DEFAULT 0,
  objects_mismatched INT NOT NULL CONSTRAINT df_artifact_object_audit_run_mismatched DEFAULT 0,
  last_error_code VARCHAR(64) NULL,
  started_at DATETIME2 NOT NULL CONSTRAINT df_artifact_object_audit_run_started DEFAULT SYSUTCDATETIME(),
  completed_at DATETIME2 NULL,
  CONSTRAINT ck_artifact_object_audit_run_status CHECK(status IN('RUNNING','COMPLETED','RETRYABLE','ABANDONED')),
  CONSTRAINT ck_artifact_object_audit_run_counts CHECK(objects_examined>=0 AND objects_verified>=0 AND objects_missing>=0 AND objects_mismatched>=0 AND objects_verified+objects_missing+objects_mismatched<=objects_examined),
  CONSTRAINT ck_artifact_object_audit_run_times CHECK((status='RUNNING' AND completed_at IS NULL) OR (status<>'RUNNING' AND completed_at IS NOT NULL))
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact_object_audit_run') AND name=N'ix_artifact_object_audit_run_stale')
  CREATE INDEX ix_artifact_object_audit_run_stale ON ingest.artifact_object_audit_run(status,started_at) INCLUDE(audit_run_id);
IF OBJECT_ID(N'ingest.tr_artifact_object_audit_run_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER ingest.tr_artifact_object_audit_run_immutable ON ingest.artifact_object_audit_run AFTER UPDATE,DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS(SELECT 1 FROM deleted d WHERE d.status<>''RUNNING'')
     OR EXISTS(SELECT 1 FROM deleted d LEFT JOIN inserted i ON i.audit_run_id=d.audit_run_id WHERE i.audit_run_id IS NULL)
    THROW 51094,''Terminal artifact audit runs are immutable and retained as evidence.'',1;
END');

IF OBJECT_ID(N'ingest.artifact_object_audit_issue',N'U') IS NULL
CREATE TABLE ingest.artifact_object_audit_issue(
  audit_issue_id BIGINT IDENTITY PRIMARY KEY,
  audit_run_id BIGINT NULL,
  artifact_object_id BIGINT NOT NULL,
  issue_code VARCHAR(24) NOT NULL,
  expected_sha256 CHAR(64) NOT NULL,
  observed_sha256 CHAR(64) NULL,
  expected_byte_size BIGINT NOT NULL,
  observed_byte_size BIGINT NULL,
  detected_at DATETIME2 NOT NULL CONSTRAINT df_artifact_object_audit_issue_detected DEFAULT SYSUTCDATETIME(),
  last_detected_at DATETIME2 NOT NULL CONSTRAINT df_artifact_object_audit_issue_last_detected DEFAULT SYSUTCDATETIME(),
  resolved_at DATETIME2 NULL,
  occurrence_count INT NOT NULL CONSTRAINT df_artifact_object_audit_issue_occurrences DEFAULT 1,
  CONSTRAINT fk_artifact_object_audit_issue_run FOREIGN KEY(audit_run_id) REFERENCES ingest.artifact_object_audit_run(audit_run_id) ON DELETE SET NULL,
  CONSTRAINT fk_artifact_object_audit_issue_object FOREIGN KEY(artifact_object_id) REFERENCES ingest.artifact_object(artifact_object_id),
  CONSTRAINT ck_artifact_object_audit_issue_code CHECK(issue_code IN('MISSING','CHECKSUM_MISMATCH')),
  CONSTRAINT ck_artifact_object_audit_issue_size CHECK(expected_byte_size>=0 AND (observed_byte_size IS NULL OR observed_byte_size>=0) AND occurrence_count>0),
  CONSTRAINT ck_artifact_object_audit_issue_sha CHECK(expected_sha256 NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2 AND (observed_sha256 IS NULL OR observed_sha256 NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2))
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact_object_audit_issue') AND name=N'ux_artifact_object_audit_issue_open')
  CREATE UNIQUE INDEX ux_artifact_object_audit_issue_open ON ingest.artifact_object_audit_issue(artifact_object_id) WHERE resolved_at IS NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact_object_audit_issue') AND name=N'ix_artifact_object_audit_issue_object')
  CREATE INDEX ix_artifact_object_audit_issue_object ON ingest.artifact_object_audit_issue(artifact_object_id,detected_at DESC)
    INCLUDE(issue_code,audit_run_id,expected_byte_size,observed_byte_size);
