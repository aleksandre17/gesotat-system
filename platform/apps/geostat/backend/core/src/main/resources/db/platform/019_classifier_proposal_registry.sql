/* Access may submit vocabulary evidence; only governed review may promote it into the authoritative registry. */
IF OBJECT_ID(N'platform.classifier_proposal',N'U') IS NULL CREATE TABLE platform.classifier_proposal (
  classifier_proposal_id BIGINT IDENTITY PRIMARY KEY,
  contract_code NVARCHAR(120) NOT NULL,
  contract_revision INT NOT NULL,
  package_batch_id BIGINT NOT NULL,
  proposal_type VARCHAR(24) NOT NULL,
  subject_key NVARCHAR(512) NOT NULL,
  scheme_code NVARCHAR(120) NOT NULL,
  version_code NVARCHAR(128) NOT NULL,
  item_code NVARCHAR(255) NULL,
  item_ref NVARCHAR(512) NULL,
  label_ka NVARCHAR(1000) NULL,
  label_en NVARCHAR(1000) NULL,
  source_system_code NVARCHAR(120) NULL,
  external_code_raw NVARCHAR(1000) NULL,
  normalized_code NVARCHAR(1000) NULL,
  normalization_rule NVARCHAR(120) NULL,
  state VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  reviewed_at DATETIME2 NULL,
  reviewed_by_user_id BIGINT NULL,
  review_note NVARCHAR(2000) NULL,
  CONSTRAINT uq_classifier_proposal_subject UNIQUE(contract_code,contract_revision,package_batch_id,proposal_type,subject_key),
  CONSTRAINT ck_classifier_proposal_type CHECK(proposal_type IN ('ITEM','ALIAS','HIERARCHY')),
  CONSTRAINT ck_classifier_proposal_state CHECK(state IN ('DRAFT','ACCEPTED','REJECTED','SUPERSEDED'))
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ix_classifier_proposal_review' AND object_id=OBJECT_ID(N'platform.classifier_proposal'))
  CREATE INDEX ix_classifier_proposal_review ON platform.classifier_proposal(state,scheme_code,version_code,classifier_proposal_id);
