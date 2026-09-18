/* SQL Server integration test for migration 089.
   Runs entirely inside a transaction and leaves no records behind. */
SET NOCOUNT ON;
SET XACT_ABORT OFF;
BEGIN TRANSACTION;

DECLARE @suffix NVARCHAR(32) = REPLACE(CONVERT(NVARCHAR(36), NEWID()), N'-', N'');
DECLARE @policy_code NVARCHAR(120) = N'AIR_014_POLICY_' + @suffix;
DECLARE @relation_code NVARCHAR(64) = N'AIR_014_REL_' + @suffix;
DECLARE @dataset_version_id BIGINT;
DECLARE @policy_id BIGINT;

SELECT TOP (1) @dataset_version_id = dataset_version_id
FROM platform.artifact_relation_definition
ORDER BY dataset_version_id;
IF @dataset_version_id IS NULL
  THROW 52000, N'Lifecycle test requires an artifact relation seed (migration 088).', 1;

INSERT platform.artifact_policy(policy_code,revision,access_mode,required_authority,allowed_media_types_json,
    max_bytes,signed_url_ttl_seconds,retention_class,lifecycle_status)
VALUES(@policy_code,1,'AUTHENTICATED',NULL,N'["application/pdf"]',1024,300,'ARCHIVE_1Y','DRAFT');
SET @policy_id = SCOPE_IDENTITY();

UPDATE platform.artifact_policy SET lifecycle_status='APPROVED' WHERE artifact_policy_id=@policy_id;
BEGIN TRY
  UPDATE platform.artifact_policy SET revision=2 WHERE artifact_policy_id=@policy_id;
  THROW 52000, N'Approved policy revision mutation was accepted.', 1;
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=52000 THROW;
  IF ERROR_NUMBER()<>51020 THROW;
END CATCH;
BEGIN TRY
  UPDATE platform.artifact_policy SET policy_code=policy_code+N'_MUT' WHERE artifact_policy_id=@policy_id;
  THROW 52000, N'Approved policy identity mutation was accepted.', 1;
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=52000 THROW;
  IF ERROR_NUMBER()<>51020 THROW;
END CATCH;
BEGIN TRY
  DELETE platform.artifact_policy WHERE artifact_policy_id=@policy_id;
  THROW 52000, N'Approved policy deletion was accepted.', 1;
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=52000 THROW;
  IF ERROR_NUMBER()<>51020 THROW;
END CATCH;

UPDATE platform.artifact_policy SET lifecycle_status='RETIRED' WHERE artifact_policy_id=@policy_id;
BEGIN TRY
  UPDATE platform.artifact_policy SET lifecycle_status='APPROVED' WHERE artifact_policy_id=@policy_id;
  THROW 52000, N'Retired policy reactivation was accepted.', 1;
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=52000 THROW;
  IF ERROR_NUMBER()<>51020 THROW;
END CATCH;
BEGIN TRY
  DELETE platform.artifact_policy WHERE artifact_policy_id=@policy_id;
  THROW 52000, N'Retired policy deletion was accepted.', 1;
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=52000 THROW;
  IF ERROR_NUMBER()<>51020 THROW;
END CATCH;

INSERT platform.artifact_relation_definition(dataset_version_id,relation_code,artifact_role,artifact_policy_id,
    min_per_row,max_per_row,ordered,match_rule_json,lifecycle_status)
VALUES(@dataset_version_id,@relation_code,'SUPPORTING',@policy_id,0,1,0,
    N'{"type":"SOURCE_PATH","bindings":[{"language":"und","field":"path"}]}','DRAFT');
DECLARE @relation_id BIGINT = SCOPE_IDENTITY();
UPDATE platform.artifact_relation_definition SET lifecycle_status='APPROVED'
WHERE artifact_relation_definition_id=@relation_id;
BEGIN TRY
  UPDATE platform.artifact_relation_definition SET relation_code=relation_code+N'_MUT'
  WHERE artifact_relation_definition_id=@relation_id;
  THROW 52000, N'Approved relation identity mutation was accepted.', 1;
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=52000 THROW;
  IF ERROR_NUMBER()<>51021 THROW;
END CATCH;
BEGIN TRY
  DELETE platform.artifact_relation_definition WHERE artifact_relation_definition_id=@relation_id;
  THROW 52000, N'Approved relation deletion was accepted.', 1;
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=52000 THROW;
  IF ERROR_NUMBER()<>51021 THROW;
END CATCH;
BEGIN TRY
  UPDATE platform.artifact_relation_definition SET match_rule_json=N'{"type":"CHANGED"}'
  WHERE artifact_relation_definition_id=@relation_id;
  THROW 52000, N'Approved relation semantics mutation was accepted.', 1;
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=52000 THROW;
  IF ERROR_NUMBER()<>51021 THROW;
END CATCH;

UPDATE platform.artifact_relation_definition SET lifecycle_status='RETIRED'
WHERE artifact_relation_definition_id=@relation_id;
BEGIN TRY
  UPDATE platform.artifact_relation_definition SET lifecycle_status='APPROVED'
  WHERE artifact_relation_definition_id=@relation_id;
  THROW 52000, N'Retired relation reactivation was accepted.', 1;
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=52000 THROW;
  IF ERROR_NUMBER()<>51021 THROW;
END CATCH;

ROLLBACK TRANSACTION;
PRINT 'ARTIFACT_CONTRACT_LIFECYCLE_PASS';
