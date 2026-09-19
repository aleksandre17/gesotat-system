/* Run in geostat-data (Data Plane).
   Governed package run (artifact contract §7, §12, §26; checklist 14.1/14.4): one durable, resumable
   execution that carries an admitted, contract-bound manifest through ingestion, materialization,
   snapshot binding, reconciliation and release-gate evaluation. Publication is never part of a run. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF OBJECT_ID(N'ingest.artifact_package_run',N'U') IS NULL
CREATE TABLE ingest.artifact_package_run(
  package_run_id BIGINT IDENTITY PRIMARY KEY,
  artifact_manifest_id BIGINT NOT NULL,
  dataset_version_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL CONSTRAINT df_artifact_package_run_status DEFAULT 'PENDING',
  next_stage_code VARCHAR(40) NULL,
  attempt INT NOT NULL CONSTRAINT df_artifact_package_run_attempt DEFAULT 0,
  state_json NVARCHAR(MAX) NOT NULL CONSTRAINT df_artifact_package_run_state DEFAULT N'{}',
  last_issue_code VARCHAR(64) NULL,
  requested_by NVARCHAR(255) NOT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_artifact_package_run_created DEFAULT SYSUTCDATETIME(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_artifact_package_run_updated DEFAULT SYSUTCDATETIME(),
  completed_at DATETIME2 NULL,
  CONSTRAINT uq_artifact_package_run_manifest UNIQUE(artifact_manifest_id),
  CONSTRAINT fk_artifact_package_run_manifest FOREIGN KEY(artifact_manifest_id) REFERENCES ingest.artifact_manifest(artifact_manifest_id),
  CONSTRAINT ck_artifact_package_run_status CHECK(status IN('PENDING','RUNNING','RETRYABLE','BLOCKED','COMPLETED')),
  CONSTRAINT ck_artifact_package_run_state CHECK(ISJSON(state_json)=1),
  CONSTRAINT ck_artifact_package_run_completed CHECK((status='COMPLETED' AND completed_at IS NOT NULL AND next_stage_code IS NULL)
      OR (status<>'COMPLETED' AND completed_at IS NULL))
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact_package_run') AND name=N'ix_artifact_package_run_due')
  CREATE INDEX ix_artifact_package_run_due ON ingest.artifact_package_run(status,updated_at) INCLUDE(package_run_id);

IF OBJECT_ID(N'ingest.artifact_package_run_stage',N'U') IS NULL
CREATE TABLE ingest.artifact_package_run_stage(
  package_run_stage_id BIGINT IDENTITY PRIMARY KEY,
  package_run_id BIGINT NOT NULL,
  attempt INT NOT NULL,
  stage_code VARCHAR(40) NOT NULL,
  outcome VARCHAR(16) NOT NULL,
  issue_code VARCHAR(64) NULL,
  detail_json NVARCHAR(MAX) NULL,
  recorded_at DATETIME2 NOT NULL CONSTRAINT df_artifact_package_run_stage_at DEFAULT SYSUTCDATETIME(),
  CONSTRAINT fk_artifact_package_run_stage_run FOREIGN KEY(package_run_id) REFERENCES ingest.artifact_package_run(package_run_id),
  CONSTRAINT ck_artifact_package_run_stage_outcome CHECK(outcome IN('COMPLETED','RETRYABLE','BLOCKED')),
  CONSTRAINT ck_artifact_package_run_stage_detail CHECK(detail_json IS NULL OR ISJSON(detail_json)=1)
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact_package_run_stage') AND name=N'ix_artifact_package_run_stage_run')
  CREATE INDEX ix_artifact_package_run_stage_run ON ingest.artifact_package_run_stage(package_run_id,package_run_stage_id);

IF OBJECT_ID(N'ingest.tr_artifact_package_run_stage_append_only',N'TR') IS NULL EXEC(N'
CREATE TRIGGER ingest.tr_artifact_package_run_stage_append_only ON ingest.artifact_package_run_stage AFTER UPDATE,DELETE AS
BEGIN
  SET NOCOUNT ON;
  THROW 51101,''Package run stage history is append-only evidence.'',1;
END');
