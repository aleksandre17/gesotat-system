IF OBJECT_ID(N'platform.contract_revision_lifecycle', N'U') IS NULL
BEGIN
  CREATE TABLE platform.contract_revision_lifecycle (
    revision_key NVARCHAR(256) NOT NULL PRIMARY KEY,
    state_code NVARCHAR(32) NOT NULL,
    updated_at DATETIME2 NOT NULL CONSTRAINT DF_contract_revision_lifecycle_updated DEFAULT SYSUTCDATETIME(),
    CONSTRAINT CK_contract_revision_lifecycle_state CHECK (state_code IN ('DRAFT','REVIEW_REQUIRED','APPROVED','SUPERSEDED','ROLLED_BACK'))
  );
END;
