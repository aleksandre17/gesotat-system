/* Data Plane SQL Server negative acceptance for migration 092. */
SET NOCOUNT ON;
SET XACT_ABORT OFF;
BEGIN TRANSACTION;

DECLARE @checksum CHAR(64)=LOWER(CONVERT(CHAR(64),HASHBYTES('SHA2_256',CONVERT(VARBINARY(16),NEWID())),2));
DECLARE @rejected BIT=0;
BEGIN TRY
  INSERT ingest.artifact_manifest(manifest_schema,package_code,package_checksum,generator_version,entry_count,contract_code,contract_revision,dataset_version_id)
  VALUES('geostat.artifact-manifest.v1',N'AIR_091_PARTIAL',@checksum,N'artifact-manifest-generator/1',0,N'SITE_A',NULL,17);
END TRY
BEGIN CATCH
  IF ERROR_NUMBER()=547 SET @rejected=1 ELSE THROW;
END CATCH;

IF @rejected=0
BEGIN
  ROLLBACK;
  THROW 52091,N'Partial contract binding was accepted.',1;
END;

ROLLBACK;
PRINT 'ARTIFACT_MANIFEST_CONTRACT_BINDING_INTEGRITY_PASS';
