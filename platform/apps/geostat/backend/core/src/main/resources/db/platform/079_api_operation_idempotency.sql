IF COL_LENGTH(N'platform.api_operation',N'idempotency_key') IS NULL
    ALTER TABLE platform.api_operation ADD idempotency_key VARCHAR(128) NULL, request_hash CHAR(64) NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ux_api_operation_idempotency_key' AND object_id=OBJECT_ID(N'platform.api_operation'))
    CREATE UNIQUE INDEX ux_api_operation_idempotency_key ON platform.api_operation(idempotency_key) WHERE idempotency_key IS NOT NULL;
