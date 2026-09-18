/* Tighten the nullable legacy-compatible manifest binding to an all-or-none tuple. */
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'ingest.artifact_manifest') AND name=N'ck_artifact_manifest_contract_binding_complete')
  EXEC(N'ALTER TABLE ingest.artifact_manifest ADD CONSTRAINT ck_artifact_manifest_contract_binding_complete CHECK
    ((contract_code IS NULL AND contract_revision IS NULL AND dataset_version_id IS NULL) OR
     (contract_code IS NOT NULL AND LEN(contract_code)>0 AND contract_revision IS NOT NULL AND contract_revision>0 AND dataset_version_id IS NOT NULL AND dataset_version_id>0))');
