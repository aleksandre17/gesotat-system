/* Behaviour proof for migrations 106 and 107 on a real SQL Server. Runs in a disposable Control Plane database
   right after the chain replay (POST_CONTROL_SQL hook of migration-chain-fresh-replay.sh).
   Every case prints PASS or FAIL; the batch ends with THROW when any case failed, so sqlcmd -b exits non-zero.
   No site, product or measure of a real dataset is named. */
SET NOCOUNT ON;
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
DECLARE @failed INT = 0, @err INT, @ns BIGINT, @product BIGINT, @other BIGINT, @ref BIGINT, @unit BIGINT;

INSERT platform.contract_namespace(namespace_code,title_ka,authority_mode) VALUES(N'ZZ_TEST',N'test',N'AUTHORITATIVE');
SET @ns = SCOPE_IDENTITY();
INSERT platform.data_product(product_code,title_ka) VALUES(N'ZZ_TEST_PRODUCT_A',N'a'); SET @product = SCOPE_IDENTITY();
INSERT platform.data_product(product_code,title_ka) VALUES(N'ZZ_TEST_PRODUCT_B',N'b'); SET @other = SCOPE_IDENTITY();

/* ---- 106: identity grammar and target shape are enforced by the engine */
BEGIN TRY INSERT platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch) VALUES('CONCEPT',@ns,N'1BAD',1,0,0); SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=547 PRINT 'PASS 106 code grammar' ELSE BEGIN PRINT 'FAIL 106 code grammar'; SET @failed+=1; END

BEGIN TRY INSERT platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch) VALUES('TABLE',@ns,N'X',1,0,0); SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=547 PRINT 'PASS 106 closed kind set' ELSE BEGIN PRINT 'FAIL 106 closed kind set'; SET @failed+=1; END

BEGIN TRY INSERT platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch) VALUES('MEASURE',@ns,N'NO_TARGET',1,0,0); SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=547 PRINT 'PASS 106 measure needs a target' ELSE BEGIN PRINT 'FAIL 106 measure needs a target'; SET @failed+=1; END

INSERT platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch) VALUES('CONCEPT',@ns,N'C1',1,0,0);
SET @ref = SCOPE_IDENTITY();
BEGIN TRY INSERT platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch) VALUES('CONCEPT',@ns,N'C1',1,0,0); SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err IN(2627,2601) PRINT 'PASS 106 one row per exact version' ELSE BEGIN PRINT 'FAIL 106 one row per exact version'; SET @failed+=1; END

INSERT platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch) VALUES('CONCEPT',@ns,N'C1',1,1,0);
PRINT 'PASS 106 another version is another row';

BEGIN TRY UPDATE platform.statistical_reference SET lifecycle_status='APPROVED' WHERE reference_id=@ref; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=547 PRINT 'PASS 106 a decision needs who and when' ELSE BEGIN PRINT 'FAIL 106 a decision needs who and when'; SET @failed+=1; END

UPDATE platform.statistical_reference SET owner_product_id=@product WHERE reference_id=@ref;
UPDATE platform.statistical_reference SET lifecycle_status='APPROVED',decided_at=SYSUTCDATETIME(),decided_by=N'steward' WHERE reference_id=@ref;
PRINT 'PASS 106 proposed row may be completed and approved';

BEGIN TRY UPDATE platform.statistical_reference SET owner_product_id=@other WHERE reference_id=@ref; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=51203 PRINT 'PASS 106 approved scope is immutable' ELSE BEGIN PRINT CONCAT('FAIL 106 approved scope is immutable ',@err); SET @failed+=1; END

BEGIN TRY UPDATE platform.statistical_reference SET code=N'C2' WHERE reference_id=@ref; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=51202 PRINT 'PASS 106 identity is immutable' ELSE BEGIN PRINT CONCAT('FAIL 106 identity is immutable ',@err); SET @failed+=1; END

BEGIN TRY UPDATE platform.statistical_reference SET lifecycle_status='PROPOSED',decided_at=NULL,decided_by=NULL WHERE reference_id=@ref; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=51203 PRINT 'PASS 106 approval cannot be undone' ELSE BEGIN PRINT CONCAT('FAIL 106 approval cannot be undone ',@err); SET @failed+=1; END

BEGIN TRY DELETE platform.statistical_reference WHERE reference_id=@ref; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=51201 PRINT 'PASS 106 references are never deleted' ELSE BEGIN PRINT CONCAT('FAIL 106 references are never deleted ',@err); SET @failed+=1; END

UPDATE platform.statistical_reference SET lifecycle_status='SUPERSEDED' WHERE reference_id=@ref;
PRINT 'PASS 106 approved may be superseded';

/* measure envelope is bounded by the canonical store */
INSERT platform.statistical_unit(unit_code,quantity_kind,scale_factor,title,status) VALUES(N'ZZ_TEST_UNIT','COUNT',1,N'test','APPROVED'); SET @unit = SCOPE_IDENTITY();
BEGIN TRY INSERT platform.measure(measure_code,value_type,aggregation_default,title_ka,numeric_precision,numeric_scale,unit_id) VALUES(N'ZZ_TEST_WIDE','DECIMAL','NONE',N't',30,12,@unit); SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=547 PRINT 'PASS 106 numeric envelope 28/10' ELSE BEGIN PRINT 'FAIL 106 numeric envelope 28/10'; SET @failed+=1; END
INSERT platform.measure(measure_code,value_type,aggregation_default,title_ka,numeric_precision,numeric_scale,unit_id,concept_reference_id) VALUES(N'ZZ_TEST_OK','DECIMAL','NONE',N't',28,10,@unit,@ref);
PRINT 'PASS 106 measure with envelope, unit and concept';

/* ---- 107: contract store */
INSERT platform.statistical_contract_draft(draft_id,product_id,version,state_code,document,author,idempotency_key,request_hash)
VALUES(N'd1',@product,1,'DRAFT',N'{}',N'author',N'k1',REPLICATE('a',64));

BEGIN TRY INSERT platform.statistical_contract_draft(draft_id,product_id,version,state_code,document,author,idempotency_key,request_hash) VALUES(N'd2',@product,1,'DRAFT',N'{}',N'author',N'k1',REPLICATE('b',64)); SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err IN(2627,2601) PRINT 'PASS 107 idempotency key is unique per product' ELSE BEGIN PRINT 'FAIL 107 idempotency key is unique per product'; SET @failed+=1; END
INSERT platform.statistical_contract_draft(draft_id,product_id,version,state_code,document,author,idempotency_key,request_hash) VALUES(N'd3',@other,1,'DRAFT',N'{}',N'author',N'k1',REPLICATE('b',64));
PRINT 'PASS 107 same key in another product is another request';

BEGIN TRY UPDATE platform.statistical_contract_draft SET state_code='REVIEW_REQUIRED',version=2 WHERE draft_id=N'd1'; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=547 PRINT 'PASS 107 review needs the reviewed digests' ELSE BEGIN PRINT 'FAIL 107 review needs the reviewed digests'; SET @failed+=1; END

BEGIN TRY UPDATE platform.statistical_contract_draft SET state_code='IN_REVIEW' WHERE draft_id=N'd1'; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=547 PRINT 'PASS 107 only existing lifecycle values' ELSE BEGIN PRINT 'FAIL 107 only existing lifecycle values'; SET @failed+=1; END

UPDATE platform.statistical_contract_draft SET state_code='APPROVED',version=3,dataset_key=N'NS:DS',semantic_digest=REPLICATE('c',64),revision_digest=REPLICATE('d',64),decided_by=N'approver' WHERE draft_id=N'd1' AND version=1;
IF @@ROWCOUNT=1 PRINT 'PASS 107 compare-and-set update' ELSE BEGIN PRINT 'FAIL 107 compare-and-set update'; SET @failed+=1; END
UPDATE platform.statistical_contract_draft SET version=9 WHERE draft_id=N'd1' AND version=1;
IF @@ROWCOUNT=0 PRINT 'PASS 107 stale version touches nothing' ELSE BEGIN PRINT 'FAIL 107 stale version touches nothing'; SET @failed+=1; END

INSERT platform.statistical_contract_draft(draft_id,product_id,dataset_key,version,state_code,document,author,idempotency_key,request_hash,semantic_digest,revision_digest)
VALUES(N'd4',@product,N'NS:DS',2,'REVIEW_REQUIRED',N'{}',N'author',N'k4',REPLICATE('e',64),REPLICATE('c',64),REPLICATE('f',64));
BEGIN TRY UPDATE platform.statistical_contract_draft SET state_code='APPROVED',version=3,decided_by=N'approver' WHERE draft_id=N'd4'; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err IN(2627,2601) PRINT 'PASS 107 one approved contract per dataset' ELSE BEGIN PRINT CONCAT('FAIL 107 one approved contract per dataset ',@err); SET @failed+=1; END

BEGIN TRY UPDATE platform.statistical_contract_draft SET document=N'{"edited":true}' WHERE draft_id=N'd1'; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=51213 PRINT 'PASS 107 approved text is immutable' ELSE BEGIN PRINT CONCAT('FAIL 107 approved text is immutable ',@err); SET @failed+=1; END

BEGIN TRY DELETE platform.statistical_contract_draft WHERE draft_id=N'd1'; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=51212 PRINT 'PASS 107 approved contract is never deleted' ELSE BEGIN PRINT CONCAT('FAIL 107 approved contract is never deleted ',@err); SET @failed+=1; END

BEGIN TRAN;
UPDATE platform.statistical_contract_draft SET state_code='SUPERSEDED',version=4 WHERE draft_id=N'd1' AND version=3;
UPDATE platform.statistical_contract_draft SET state_code='APPROVED',version=3,decided_by=N'approver' WHERE draft_id=N'd4' AND version=2;
COMMIT;
IF (SELECT COUNT(*) FROM platform.statistical_contract_draft WHERE product_id=@product AND dataset_key=N'NS:DS' AND state_code='APPROVED')=1
  PRINT 'PASS 107 supersede then approve in one transaction' ELSE BEGIN PRINT 'FAIL 107 supersede then approve in one transaction'; SET @failed+=1; END

INSERT platform.statistical_contract_draft_event(draft_id,version,state_code,actor) VALUES(N'd1',1,'DRAFT',N'author');
BEGIN TRY UPDATE platform.statistical_contract_draft_event SET actor=N'someone else' WHERE draft_id=N'd1'; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=51211 PRINT 'PASS 107 history is append-only' ELSE BEGIN PRINT CONCAT('FAIL 107 history is append-only ',@err); SET @failed+=1; END

/* ---- 108: the proposed definition is written once */
DECLARE @def BIGINT;
INSERT platform.statistical_reference(kind,namespace_id,code,version_major,version_minor,version_patch,definition_digest) VALUES('CONCEPT',@ns,N'DEF',1,0,0,REPLICATE('1',64));
SET @def = SCOPE_IDENTITY();
BEGIN TRY UPDATE platform.statistical_reference SET definition_digest=REPLICATE('2',64) WHERE reference_id=@def; SET @err=0; END TRY BEGIN CATCH SET @err=ERROR_NUMBER(); END CATCH
IF @err=51204 PRINT 'PASS 108 definition is written once' ELSE BEGIN PRINT CONCAT('FAIL 108 definition is written once ',@err); SET @failed+=1; END

PRINT CONCAT('BEHAVIOUR_FAILED=',@failed);
IF @failed>0 THROW 51999, 'statistical contract schema behaviour failed', 1;
