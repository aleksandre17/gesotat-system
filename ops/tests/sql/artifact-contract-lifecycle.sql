/* SQL Server integration test for migration 089.
   Each expected trigger rejection is isolated in its own transaction because
   SQL Server marks the transaction uncommittable after a trigger THROW. */
SET NOCOUNT ON;
SET XACT_ABORT OFF;

DECLARE @suffix NVARCHAR(32) = REPLACE(CONVERT(NVARCHAR(36), NEWID()), N'-', N'');
DECLARE @policy_code NVARCHAR(120) = N'AIR_014_POLICY_' + @suffix;
DECLARE @relation_code NVARCHAR(64) = N'AIR_014_REL_' + @suffix;
DECLARE @dataset_version_id BIGINT;
DECLARE @policy_id BIGINT;
DECLARE @relation_id BIGINT;
DECLARE @rejection INT;

SELECT TOP (1) @dataset_version_id = dataset_version_id
FROM platform.artifact_relation_definition ORDER BY dataset_version_id;
IF @dataset_version_id IS NULL
  THROW 52000, N'Lifecycle test requires an artifact relation seed (migration 088).', 1;

BEGIN TRANSACTION;
INSERT platform.artifact_policy(policy_code,revision,access_mode,required_authority,allowed_media_types_json,
    max_bytes,signed_url_ttl_seconds,retention_class,lifecycle_status)
VALUES(@policy_code,1,'AUTHENTICATED',NULL,N'["application/pdf"]',1024,300,'ARCHIVE_1Y','DRAFT');
SET @policy_id = SCOPE_IDENTITY();
UPDATE platform.artifact_policy SET lifecycle_status='APPROVED' WHERE artifact_policy_id=@policy_id;
SET @rejection = 0;
BEGIN TRY
  UPDATE platform.artifact_policy SET revision=2 WHERE artifact_policy_id=@policy_id;
END TRY
BEGIN CATCH
  SET @rejection = ERROR_NUMBER();
END CATCH;
IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
IF @rejection <> 51020
  THROW 52001, N'Approved policy revision mutation was not rejected by migration 089.', 1;

BEGIN TRANSACTION;
INSERT platform.artifact_policy(policy_code,revision,access_mode,required_authority,allowed_media_types_json,
    max_bytes,signed_url_ttl_seconds,retention_class,lifecycle_status)
VALUES(@policy_code + N'_REL',1,'AUTHENTICATED',NULL,N'["application/pdf"]',1024,300,'ARCHIVE_1Y','APPROVED');
SET @policy_id = SCOPE_IDENTITY();
INSERT platform.artifact_relation_definition(dataset_version_id,relation_code,artifact_role,artifact_policy_id,
    min_per_row,max_per_row,ordered,match_rule_json,lifecycle_status)
VALUES(@dataset_version_id,@relation_code,'SUPPORTING',@policy_id,0,1,0,
    N'{"type":"SOURCE_PATH","bindings":[{"language":"und","field":"path"}]}','DRAFT');
SET @relation_id = SCOPE_IDENTITY();
UPDATE platform.artifact_relation_definition SET lifecycle_status='APPROVED'
WHERE artifact_relation_definition_id=@relation_id;
SET @rejection = 0;
BEGIN TRY
  UPDATE platform.artifact_relation_definition SET match_rule_json=N'{"type":"CHANGED"}'
  WHERE artifact_relation_definition_id=@relation_id;
END TRY
BEGIN CATCH
  SET @rejection = ERROR_NUMBER();
END CATCH;
IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
IF @rejection <> 51021
  THROW 52002, N'Approved relation mutation was not rejected by migration 089.', 1;

PRINT 'ARTIFACT_CONTRACT_LIFECYCLE_PASS';
