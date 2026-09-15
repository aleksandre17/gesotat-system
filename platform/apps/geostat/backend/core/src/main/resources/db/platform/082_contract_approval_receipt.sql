/* Durable, idempotent approval evidence for contract publication gating. */
IF OBJECT_ID(N'platform.contract_approval_receipt',N'U') IS NULL
CREATE TABLE platform.contract_approval_receipt(
 receipt_id BIGINT IDENTITY PRIMARY KEY,
 contract_id BIGINT NOT NULL,
 contract_revision INT NOT NULL,
 contract_checksum CHAR(64) NOT NULL,
 decision_code NVARCHAR(40) NOT NULL,
 approved_at DATETIME2(7) NOT NULL CONSTRAINT df_contract_approval_receipt_at DEFAULT SYSUTCDATETIME(),
 evidence_json NVARCHAR(MAX) NOT NULL CONSTRAINT ck_contract_approval_receipt_json CHECK(ISJSON(evidence_json)=1),
 CONSTRAINT uq_contract_approval_receipt UNIQUE(contract_id,contract_revision,contract_checksum)
);
