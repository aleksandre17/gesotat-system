/* Run in the Control Plane database.
   Common statistical contract, increment 3 (decision register Q40, Q41, Q42; lifecycle §6).

   One row per authored contract with a compare-and-set version, plus an append-only event history.
   State values are the existing contract-revision lifecycle vocabulary (084); no new state is introduced.
   Additive and idempotent. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF OBJECT_ID(N'platform.statistical_contract_draft',N'U') IS NULL
CREATE TABLE platform.statistical_contract_draft(
  draft_id NVARCHAR(64) NOT NULL PRIMARY KEY,
  product_id BIGINT NOT NULL,
  dataset_key NVARCHAR(256) NULL,
  version BIGINT NOT NULL,
  state_code VARCHAR(32) NOT NULL,
  document NVARCHAR(MAX) NOT NULL,
  author NVARCHAR(255) NOT NULL,
  idempotency_key NVARCHAR(128) NOT NULL,
  request_hash CHAR(64) NOT NULL,
  semantic_digest CHAR(64) NULL,
  revision_digest CHAR(64) NULL,
  decided_by NVARCHAR(255) NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_statistical_contract_draft_created DEFAULT SYSUTCDATETIME(),
  updated_at DATETIME2 NOT NULL CONSTRAINT df_statistical_contract_draft_updated DEFAULT SYSUTCDATETIME(),
  CONSTRAINT fk_statistical_contract_draft_product FOREIGN KEY(product_id) REFERENCES platform.data_product(product_id),
  CONSTRAINT uq_statistical_contract_draft_idempotency UNIQUE(product_id,idempotency_key),
  CONSTRAINT ck_statistical_contract_draft_state CHECK(state_code IN('DRAFT','REVIEW_REQUIRED','APPROVED','SUPERSEDED','ROLLED_BACK')),
  CONSTRAINT ck_statistical_contract_draft_version CHECK(version>=1),
  /* A reviewed or decided contract always carries the digests it was reviewed under. */
  CONSTRAINT ck_statistical_contract_draft_digest CHECK(
       state_code='DRAFT' OR (dataset_key IS NOT NULL AND semantic_digest IS NOT NULL AND revision_digest IS NOT NULL)),
  CONSTRAINT ck_statistical_contract_draft_decision CHECK(state_code<>'APPROVED' OR decided_by IS NOT NULL)
);
/* At most one approved contract per dataset of a product: approval and supersession are one transaction. */
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'platform.statistical_contract_draft') AND name=N'ux_statistical_contract_draft_approved')
  EXEC(N'CREATE UNIQUE INDEX ux_statistical_contract_draft_approved ON platform.statistical_contract_draft(product_id,dataset_key) WHERE state_code=''APPROVED''');

IF OBJECT_ID(N'platform.statistical_contract_draft_event',N'U') IS NULL
CREATE TABLE platform.statistical_contract_draft_event(
  event_id BIGINT IDENTITY PRIMARY KEY,
  draft_id NVARCHAR(64) NOT NULL,
  version BIGINT NOT NULL,
  state_code VARCHAR(32) NOT NULL,
  actor NVARCHAR(255) NOT NULL,
  revision_digest CHAR(64) NULL,
  recorded_at DATETIME2 NOT NULL CONSTRAINT df_statistical_contract_draft_event_at DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_statistical_contract_draft_event UNIQUE(draft_id,version),
  CONSTRAINT fk_statistical_contract_draft_event_draft FOREIGN KEY(draft_id) REFERENCES platform.statistical_contract_draft(draft_id)
);
IF OBJECT_ID(N'platform.tr_statistical_contract_draft_event_append_only',N'TR') IS NULL EXEC(N'
CREATE TRIGGER platform.tr_statistical_contract_draft_event_append_only ON platform.statistical_contract_draft_event AFTER UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  THROW 51211, ''Statistical contract history is append-only evidence.'', 1;
END');

/* A decided contract text is immutable; only APPROVED -> SUPERSEDED / ROLLED_BACK may follow. */
IF OBJECT_ID(N'platform.tr_statistical_contract_draft_decided_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER platform.tr_statistical_contract_draft_decided_immutable ON platform.statistical_contract_draft AFTER UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS(SELECT 1 FROM deleted d WHERE d.state_code IN(''APPROVED'',''SUPERSEDED'',''ROLLED_BACK'')
            AND NOT EXISTS(SELECT 1 FROM inserted i WHERE i.draft_id=d.draft_id))
    THROW 51212, ''A decided statistical contract is never deleted.'', 1;
  IF EXISTS(SELECT 1 FROM deleted d JOIN inserted i ON i.draft_id=d.draft_id
            WHERE d.state_code IN(''APPROVED'',''SUPERSEDED'',''ROLLED_BACK'')
              AND (d.document<>i.document OR d.revision_digest<>i.revision_digest OR d.product_id<>i.product_id
                OR NOT (d.state_code=i.state_code OR (d.state_code=''APPROVED'' AND i.state_code IN(''SUPERSEDED'',''ROLLED_BACK'')))))
    THROW 51213, ''A decided statistical contract is immutable.'', 1;
END');
