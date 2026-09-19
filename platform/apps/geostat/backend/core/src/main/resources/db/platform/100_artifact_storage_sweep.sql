/* Run in geostat-data (Data Plane).
   Storage-to-registry reconciliation (artifact contract §19, checklist 14.3): objects that exist in
   Object Storage under a governed prefix but are unknown to ingest.artifact_object.
   The sweep records evidence only. It never deletes bytes. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF OBJECT_ID(N'ingest.artifact_storage_sweep',N'U') IS NULL
CREATE TABLE ingest.artifact_storage_sweep(
  storage_sweep_id BIGINT IDENTITY PRIMARY KEY,
  bucket NVARCHAR(63) NOT NULL,
  prefix NVARCHAR(512) NOT NULL,
  cycle INT NOT NULL CONSTRAINT df_artifact_storage_sweep_cycle DEFAULT 1,
  cursor_key NVARCHAR(1024) NULL,
  cycle_objects_listed BIGINT NOT NULL CONSTRAINT df_artifact_storage_sweep_listed DEFAULT 0,
  cycle_orphans_detected BIGINT NOT NULL CONSTRAINT df_artifact_storage_sweep_orphans DEFAULT 0,
  cycle_started_at DATETIME2 NOT NULL CONSTRAINT df_artifact_storage_sweep_started DEFAULT SYSUTCDATETIME(),
  last_progress_at DATETIME2 NOT NULL CONSTRAINT df_artifact_storage_sweep_progress DEFAULT SYSUTCDATETIME(),
  last_completed_at DATETIME2 NULL,
  last_completed_objects_listed BIGINT NULL,
  last_completed_orphans_detected BIGINT NULL,
  CONSTRAINT uq_artifact_storage_sweep_scope UNIQUE(bucket,prefix),
  CONSTRAINT ck_artifact_storage_sweep_counts CHECK(cycle>=1 AND cycle_objects_listed>=0 AND cycle_orphans_detected>=0)
);

IF OBJECT_ID(N'ingest.artifact_storage_orphan',N'U') IS NULL
CREATE TABLE ingest.artifact_storage_orphan(
  storage_orphan_id BIGINT IDENTITY PRIMARY KEY,
  bucket NVARCHAR(63) NOT NULL,
  object_key NVARCHAR(1024) NOT NULL,
  byte_size BIGINT NOT NULL,
  detected_at DATETIME2 NOT NULL CONSTRAINT df_artifact_storage_orphan_detected DEFAULT SYSUTCDATETIME(),
  last_detected_at DATETIME2 NOT NULL CONSTRAINT df_artifact_storage_orphan_last DEFAULT SYSUTCDATETIME(),
  occurrence_count INT NOT NULL CONSTRAINT df_artifact_storage_orphan_occurrences DEFAULT 1,
  resolved_at DATETIME2 NULL,
  resolution VARCHAR(16) NULL,
  CONSTRAINT ck_artifact_storage_orphan_size CHECK(byte_size>=0 AND occurrence_count>0),
  CONSTRAINT ck_artifact_storage_orphan_resolution CHECK((resolved_at IS NULL AND resolution IS NULL)
      OR (resolved_at IS NOT NULL AND resolution IN('REGISTERED','ABSENT')))
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact_storage_orphan') AND name=N'ux_artifact_storage_orphan_open')
  CREATE UNIQUE INDEX ux_artifact_storage_orphan_open ON ingest.artifact_storage_orphan(bucket,object_key) WHERE resolved_at IS NULL;
