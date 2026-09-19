/* SQL Server integration test for migration 099 (run in geostat-system).
   Every case runs in its own transaction and is rolled back; nothing is retained.
   A trigger THROW leaves the transaction uncommittable, hence the XACT_STATE checks. */
SET NOCOUNT ON;
SET XACT_ABORT OFF;

DECLARE @code NVARCHAR(120) = N'C01_GOV_' + REPLACE(CONVERT(NVARCHAR(36), NEWID()), N'-', N'');
DECLARE @doc NVARCHAR(MAX) = N'{"probe":"c-01"}';
DECLARE @sum CHAR(64) = CONVERT(VARCHAR(64),HASHBYTES('SHA2_256',CONVERT(VARBINARY(MAX),@doc)),2);
DECLARE @product BIGINT, @first BIGINT, @second BIGINT, @rejection INT;

SELECT TOP (1) @product = product_id FROM platform.data_product ORDER BY product_id;
IF @product IS NULL THROW 52040, N'Governance test requires a data product.', 1;

/* 1. approved document is immutable */
BEGIN TRANSACTION;
INSERT platform.site_contract_revision(product_id,contract_code,revision,schema_standard,compatibility_mode,status,contract_checksum,contract_document_json)
VALUES(@product,@code,1,N'TEST','BACKWARD_COMPATIBLE','REVIEW_REQUIRED',@sum,@doc);
SET @first = SCOPE_IDENTITY();
UPDATE platform.site_contract_revision SET status='APPROVED' WHERE site_contract_revision_id=@first;
SET @rejection = 0;
BEGIN TRY
  UPDATE platform.site_contract_revision SET contract_document_json=N'{"probe":"tampered"}' WHERE site_contract_revision_id=@first;
END TRY BEGIN CATCH SET @rejection = ERROR_NUMBER(); END CATCH;
IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
IF @rejection <> 51041 THROW 52041, N'Approved document mutation was not rejected.', 1;

/* 2. approved revision cannot return to review or be deleted */
BEGIN TRANSACTION;
INSERT platform.site_contract_revision(product_id,contract_code,revision,schema_standard,compatibility_mode,status,contract_checksum,contract_document_json)
VALUES(@product,@code,1,N'TEST','BACKWARD_COMPATIBLE','APPROVED',@sum,@doc);
SET @first = SCOPE_IDENTITY();
SET @rejection = 0;
BEGIN TRY
  UPDATE platform.site_contract_revision SET status='REVIEW_REQUIRED' WHERE site_contract_revision_id=@first;
END TRY BEGIN CATCH SET @rejection = ERROR_NUMBER(); END CATCH;
IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
IF @rejection <> 51041 THROW 52042, N'APPROVED -> REVIEW_REQUIRED was not rejected.', 1;

BEGIN TRANSACTION;
INSERT platform.site_contract_revision(product_id,contract_code,revision,schema_standard,compatibility_mode,status,contract_checksum,contract_document_json)
VALUES(@product,@code,1,N'TEST','BACKWARD_COMPATIBLE','APPROVED',@sum,@doc);
SET @first = SCOPE_IDENTITY();
SET @rejection = 0;
BEGIN TRY
  DELETE platform.site_contract_revision WHERE site_contract_revision_id=@first;
END TRY BEGIN CATCH SET @rejection = ERROR_NUMBER(); END CATCH;
IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
IF @rejection <> 51041 THROW 52043, N'Approved revision delete was not rejected.', 1;

/* 3. a second APPROVED revision of one contract is rejected; supersede-then-approve is accepted */
BEGIN TRANSACTION;
INSERT platform.site_contract_revision(product_id,contract_code,revision,schema_standard,compatibility_mode,status,contract_checksum,contract_document_json)
VALUES(@product,@code,1,N'TEST','BACKWARD_COMPATIBLE','APPROVED',@sum,@doc);
SET @first = SCOPE_IDENTITY();
INSERT platform.site_contract_revision(product_id,contract_code,revision,parent_revision_id,schema_standard,compatibility_mode,status,contract_checksum,contract_document_json)
VALUES(@product,@code,2,@first,N'TEST','BACKWARD_COMPATIBLE','REVIEW_REQUIRED',@sum,@doc);
SET @second = SCOPE_IDENTITY();
SET @rejection = 0;
BEGIN TRY
  UPDATE platform.site_contract_revision SET status='APPROVED' WHERE site_contract_revision_id=@second;
END TRY BEGIN CATCH SET @rejection = ERROR_NUMBER(); END CATCH;
IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
IF @rejection <> 51042 THROW 52044, N'Second APPROVED revision was not rejected.', 1;

BEGIN TRANSACTION;
INSERT platform.site_contract_revision(product_id,contract_code,revision,schema_standard,compatibility_mode,status,contract_checksum,contract_document_json)
VALUES(@product,@code,1,N'TEST','BACKWARD_COMPATIBLE','APPROVED',@sum,@doc);
SET @first = SCOPE_IDENTITY();
INSERT platform.site_contract_revision(product_id,contract_code,revision,parent_revision_id,schema_standard,compatibility_mode,status,contract_checksum,contract_document_json)
VALUES(@product,@code,2,@first,N'TEST','BACKWARD_COMPATIBLE','REVIEW_REQUIRED',@sum,@doc);
SET @second = SCOPE_IDENTITY();
SET @rejection = 0;
BEGIN TRY
  UPDATE platform.site_contract_revision SET status='SUPERSEDED' WHERE site_contract_revision_id=@first;
  UPDATE platform.site_contract_revision SET status='APPROVED' WHERE site_contract_revision_id=@second;
  INSERT platform.site_contract_revision_approval(site_contract_revision_id,contract_checksum,parent_revision_id,compatibility,approved_by,evidence_json)
  VALUES(@second,@sum,@first,'BACKWARD_COMPATIBLE',N'fixture',N'{}');
END TRY BEGIN CATCH SET @rejection = ERROR_NUMBER(); END CATCH;
IF @rejection <> 0 BEGIN IF XACT_STATE() <> 0 ROLLBACK TRANSACTION; THROW 52045, N'Governed supersede-then-approve was rejected.', 1; END;

/* 4. approval evidence is append-only; a breaking approval needs an acknowledgement */
BEGIN TRY
  UPDATE platform.site_contract_revision_approval SET approved_by=N'other' WHERE site_contract_revision_id=@second;
END TRY BEGIN CATCH SET @rejection = ERROR_NUMBER(); END CATCH;
IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
IF @rejection <> 51040 THROW 52046, N'Approval evidence mutation was not rejected.', 1;

BEGIN TRANSACTION;
INSERT platform.site_contract_revision(product_id,contract_code,revision,schema_standard,compatibility_mode,status,contract_checksum,contract_document_json)
VALUES(@product,@code,1,N'TEST','BREAKING_NEW_REVISION','APPROVED',@sum,@doc);
SET @first = SCOPE_IDENTITY();
SET @rejection = 0;
BEGIN TRY
  INSERT platform.site_contract_revision_approval(site_contract_revision_id,contract_checksum,compatibility,approved_by,evidence_json)
  VALUES(@first,@sum,'BREAKING',N'fixture',N'{}');
END TRY BEGIN CATCH SET @rejection = ERROR_NUMBER(); END CATCH;
IF XACT_STATE() <> 0 ROLLBACK TRANSACTION;
IF @rejection <> 547 THROW 52047, N'Breaking approval without acknowledgement was not rejected.', 1;

IF EXISTS (SELECT 1 FROM platform.site_contract_revision WHERE contract_code=@code)
  THROW 52048, N'Governance test left rows behind.', 1;
SELECT N'PASS' AS site_contract_revision_governance;
