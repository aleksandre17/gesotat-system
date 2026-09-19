/* Run in geostat-data (Data Plane).
   Accepted manifest document (artifact contract §25): the platform-derived description of an admitted
   package - every file claim and every deterministic row-to-file edge - persisted immutably at admission
   and reused for idempotent retries. The bytes live in Object Storage under their checksum; this table
   holds identity only. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF OBJECT_ID(N'ingest.artifact_manifest_document',N'U') IS NULL
CREATE TABLE ingest.artifact_manifest_document(
  artifact_manifest_id BIGINT NOT NULL PRIMARY KEY,
  document_schema VARCHAR(64) NOT NULL,
  sha256 CHAR(64) NOT NULL,
  byte_size BIGINT NOT NULL,
  bucket NVARCHAR(63) NOT NULL,
  object_key NVARCHAR(1024) NOT NULL,
  file_count INT NOT NULL,
  edge_count INT NOT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_artifact_manifest_document_created DEFAULT SYSUTCDATETIME(),
  CONSTRAINT fk_artifact_manifest_document_manifest FOREIGN KEY(artifact_manifest_id) REFERENCES ingest.artifact_manifest(artifact_manifest_id),
  CONSTRAINT ck_artifact_manifest_document_sha CHECK(LEN(sha256)=64 AND sha256 NOT LIKE '%[^0-9a-f]%' COLLATE Latin1_General_BIN2),
  CONSTRAINT ck_artifact_manifest_document_counts CHECK(byte_size>0 AND file_count>=0 AND edge_count>=0)
);

IF OBJECT_ID(N'ingest.tr_artifact_manifest_document_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER ingest.tr_artifact_manifest_document_immutable ON ingest.artifact_manifest_document AFTER UPDATE,DELETE AS
BEGIN
  SET NOCOUNT ON;
  THROW 51102,''An accepted manifest document is immutable.'',1;
END');
