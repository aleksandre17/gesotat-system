/* Data Plane evidence for the approved Control Plane contract binding used at package admission. */
IF COL_LENGTH(N'ingest.artifact_manifest',N'contract_code') IS NULL
  EXEC(N'ALTER TABLE ingest.artifact_manifest ADD contract_code NVARCHAR(120) NULL');
IF COL_LENGTH(N'ingest.artifact_manifest',N'contract_revision') IS NULL
  EXEC(N'ALTER TABLE ingest.artifact_manifest ADD contract_revision INT NULL');
IF COL_LENGTH(N'ingest.artifact_manifest',N'dataset_version_id') IS NULL
  EXEC(N'ALTER TABLE ingest.artifact_manifest ADD dataset_version_id BIGINT NULL');
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'ingest.artifact_manifest') AND name=N'ck_artifact_manifest_contract_binding')
  EXEC(N'ALTER TABLE ingest.artifact_manifest ADD CONSTRAINT ck_artifact_manifest_contract_binding CHECK
    ((contract_code IS NULL AND contract_revision IS NULL AND dataset_version_id IS NULL) OR
     (contract_code IS NOT NULL AND LEN(contract_code)>0 AND contract_revision>0 AND dataset_version_id>0))');
