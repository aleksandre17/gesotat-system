/* Run in geostat-system (Control Plane).
   Site contract revision governance (completion plan C-01).
   1. Durable approval evidence, one row per approved revision, bound to the checksum that was approved.
   2. Approved and superseded revisions are immutable; the only permitted change is APPROVED -> SUPERSEDED.
   3. A newly approved revision may not leave its contract code with more than one APPROVED revision.
      Only new approvals are checked, so pre-existing rows never block startup or their own repair. */

IF OBJECT_ID(N'platform.site_contract_revision_approval',N'U') IS NULL
CREATE TABLE platform.site_contract_revision_approval(
  site_contract_revision_approval_id BIGINT IDENTITY PRIMARY KEY,
  site_contract_revision_id BIGINT NOT NULL,
  contract_checksum CHAR(64) NOT NULL,
  parent_revision_id BIGINT NULL,
  compatibility VARCHAR(32) NOT NULL,
  breaking_acknowledgement NVARCHAR(1000) NULL,
  approved_by NVARCHAR(255) NOT NULL,
  approved_at DATETIME2(7) NOT NULL CONSTRAINT df_site_contract_revision_approval_at DEFAULT SYSUTCDATETIME(),
  evidence_json NVARCHAR(MAX) NOT NULL,
  CONSTRAINT uq_site_contract_revision_approval UNIQUE(site_contract_revision_id),
  CONSTRAINT fk_site_contract_revision_approval_revision FOREIGN KEY(site_contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id),
  CONSTRAINT fk_site_contract_revision_approval_parent FOREIGN KEY(parent_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id),
  CONSTRAINT ck_site_contract_revision_approval_compat CHECK(compatibility IN('INITIAL','BACKWARD_COMPATIBLE','BREAKING')),
  CONSTRAINT ck_site_contract_revision_approval_ack CHECK(compatibility<>'BREAKING' OR LEN(LTRIM(RTRIM(ISNULL(breaking_acknowledgement,N''))))>0),
  CONSTRAINT ck_site_contract_revision_approval_json CHECK(ISJSON(evidence_json)=1)
);

EXEC(N'
CREATE OR ALTER TRIGGER platform.tr_site_contract_revision_approval_append_only ON platform.site_contract_revision_approval AFTER UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  THROW 51040, ''Site contract approval evidence is append-only.'', 1;
END');

EXEC(N'
CREATE OR ALTER TRIGGER platform.tr_site_contract_revision_immutable ON platform.site_contract_revision AFTER INSERT, UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (
    SELECT 1
    FROM deleted d
    LEFT JOIN inserted i ON i.site_contract_revision_id=d.site_contract_revision_id
    WHERE d.status IN(''APPROVED'',''SUPERSEDED'') AND (
         i.site_contract_revision_id IS NULL
      OR i.product_id<>d.product_id OR i.contract_code<>d.contract_code OR i.revision<>d.revision
      OR ISNULL(i.parent_revision_id,-1)<>ISNULL(d.parent_revision_id,-1)
      OR i.schema_standard<>d.schema_standard OR i.compatibility_mode<>d.compatibility_mode
      OR i.contract_checksum<>d.contract_checksum
      OR CONVERT(VARBINARY(MAX),i.contract_document_json)<>CONVERT(VARBINARY(MAX),d.contract_document_json)
      OR i.created_at<>d.created_at
      OR (i.status<>d.status AND NOT (d.status=''APPROVED'' AND i.status=''SUPERSEDED''))
    )
  )
    THROW 51041, ''Approved site contract revisions are immutable; create a new revision.'', 1;
  IF EXISTS (
    SELECT 1 FROM platform.site_contract_revision r
    WHERE r.status=''APPROVED'' AND r.contract_code IN (
      SELECT i.contract_code FROM inserted i
      LEFT JOIN deleted d ON d.site_contract_revision_id=i.site_contract_revision_id
      WHERE i.status=''APPROVED'' AND ISNULL(d.status,'''')<>''APPROVED'')
    GROUP BY r.contract_code HAVING COUNT(*)>1
  )
    THROW 51042, ''A site contract may have only one APPROVED revision; supersede the current one in the same transaction.'', 1;
END');
