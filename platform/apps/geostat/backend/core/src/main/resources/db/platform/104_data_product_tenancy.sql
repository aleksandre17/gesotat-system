/* Run in geostat-system (Control Plane).
   Tenant ownership of a data product (audit P1.5, checklist 14.2, OWASP API1/API5, NIST ABAC).

   1. A data product belongs to at most one tenant. The owning tenant is the opaque value the
      identity provider puts in the configured tenant claim; no tenant or site value is written here.
   2. Existing products stay UNASSIGNED (tenant_key IS NULL). The access policy denies an unassigned
      product to every ordinary caller, so the platform is fail-closed until a governed assignment.
   3. Assignment is a governed, audited operation. Every assignment and transfer appends one
      immutable row recording the previous tenant, the new tenant, the actor and the reason.

   The script runs as one batch, so every statement that names the column added here is deferred
   through EXEC(N'...') and compiles only once the ALTER TABLE has run. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'platform.data_product') AND name=N'tenant_key')
  ALTER TABLE platform.data_product ADD tenant_key NVARCHAR(160) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.check_constraints WHERE name=N'ck_data_product_tenant_key_not_blank' AND parent_object_id=OBJECT_ID(N'platform.data_product'))
  EXEC(N'ALTER TABLE platform.data_product ADD CONSTRAINT ck_data_product_tenant_key_not_blank
          CHECK(tenant_key IS NULL OR LEN(LTRIM(RTRIM(tenant_key)))>0);');

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'platform.data_product') AND name=N'ix_data_product_tenant_key')
  EXEC(N'CREATE INDEX ix_data_product_tenant_key ON platform.data_product(tenant_key) INCLUDE(product_code);');

IF OBJECT_ID(N'platform.data_product_tenant_assignment',N'U') IS NULL
CREATE TABLE platform.data_product_tenant_assignment(
  data_product_tenant_assignment_id BIGINT IDENTITY PRIMARY KEY,
  product_id BIGINT NOT NULL,
  previous_tenant_key NVARCHAR(160) NULL,
  tenant_key NVARCHAR(160) NOT NULL,
  transfer BIT NOT NULL CONSTRAINT df_data_product_tenant_assignment_transfer DEFAULT 0,
  reason NVARCHAR(1000) NULL,
  assigned_by NVARCHAR(255) NOT NULL,
  assigned_at DATETIME2(7) NOT NULL CONSTRAINT df_data_product_tenant_assignment_at DEFAULT SYSUTCDATETIME(),
  CONSTRAINT fk_data_product_tenant_assignment_product FOREIGN KEY(product_id) REFERENCES platform.data_product(product_id),
  CONSTRAINT ck_data_product_tenant_assignment_key CHECK(LEN(LTRIM(RTRIM(tenant_key)))>0),
  CONSTRAINT ck_data_product_tenant_assignment_actor CHECK(LEN(LTRIM(RTRIM(assigned_by)))>0),
  /* A transfer away from an existing owner is only admissible with a recorded reason. */
  CONSTRAINT ck_data_product_tenant_assignment_transfer_reason
    CHECK(previous_tenant_key IS NULL OR previous_tenant_key=tenant_key
          OR (transfer=1 AND LEN(LTRIM(RTRIM(ISNULL(reason,N''))))>0))
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'platform.data_product_tenant_assignment') AND name=N'ix_data_product_tenant_assignment_product')
  CREATE INDEX ix_data_product_tenant_assignment_product ON platform.data_product_tenant_assignment(product_id,data_product_tenant_assignment_id);

IF OBJECT_ID(N'platform.tr_data_product_tenant_assignment_append_only',N'TR') IS NULL EXEC(N'
CREATE TRIGGER platform.tr_data_product_tenant_assignment_append_only ON platform.data_product_tenant_assignment AFTER UPDATE,DELETE AS
BEGIN
  SET NOCOUNT ON;
  THROW 51104, ''Data product tenant assignment history is append-only evidence.'', 1;
END');
