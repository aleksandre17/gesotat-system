IF COL_LENGTH(N'platform.api_operation',N'requested_by') IS NULL
    ALTER TABLE platform.api_operation ADD requested_by NVARCHAR(256) NOT NULL CONSTRAINT df_api_operation_requested_by DEFAULT N'system';
IF EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ux_api_operation_idempotency_key' AND object_id=OBJECT_ID(N'platform.api_operation'))
    DROP INDEX ux_api_operation_idempotency_key ON platform.api_operation;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ux_api_operation_owner_idempotency' AND object_id=OBJECT_ID(N'platform.api_operation'))
    CREATE UNIQUE INDEX ux_api_operation_owner_idempotency ON platform.api_operation(requested_by,idempotency_key) WHERE idempotency_key IS NOT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ix_api_operation_owner' AND object_id=OBJECT_ID(N'platform.api_operation'))
    CREATE INDEX ix_api_operation_owner ON platform.api_operation(requested_by,created_at);
