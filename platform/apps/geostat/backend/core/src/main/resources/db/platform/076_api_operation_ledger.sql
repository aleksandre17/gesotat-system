IF OBJECT_ID(N'platform.api_operation',N'U') IS NULL
CREATE TABLE platform.api_operation(
 operation_id BIGINT IDENTITY PRIMARY KEY,
 operation_type VARCHAR(32) NOT NULL,
 status VARCHAR(24) NOT NULL,
 contract_code NVARCHAR(120) NULL,
 page_id BIGINT NULL,
 request_json NVARCHAR(MAX) NOT NULL,
 result_json NVARCHAR(MAX) NULL,
 result_uri NVARCHAR(1000) NULL,
 result_checksum CHAR(64) NULL,
 progress INT NOT NULL DEFAULT 0,
 error_code VARCHAR(120) NULL,
 error_detail NVARCHAR(2000) NULL,
 created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
 started_at DATETIME2 NULL,
 completed_at DATETIME2 NULL,
 CONSTRAINT ck_api_operation_status CHECK(status IN('QUEUED','RUNNING','SUCCEEDED','FAILED','CANCELLED')),
 CONSTRAINT ck_api_operation_type CHECK(operation_type IN('EXPORT','QUERY')),
 CONSTRAINT ck_api_operation_json CHECK(ISJSON(request_json)=1)
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ix_api_operation_status' AND object_id=OBJECT_ID(N'platform.api_operation'))
 CREATE INDEX ix_api_operation_status ON platform.api_operation(status,created_at);
