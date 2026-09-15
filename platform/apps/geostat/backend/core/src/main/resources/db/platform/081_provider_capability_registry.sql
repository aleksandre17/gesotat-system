IF OBJECT_ID(N'platform.provider_capability', N'U') IS NULL
BEGIN
    CREATE TABLE platform.provider_capability (
        provider_capability_id BIGINT IDENTITY(1,1) NOT NULL CONSTRAINT pk_provider_capability PRIMARY KEY,
        provider_code NVARCHAR(128) NOT NULL,
        family_code NVARCHAR(64) NOT NULL,
        operation_code NVARCHAR(64) NOT NULL,
        feature_code NVARCHAR(64) NOT NULL,
        max_page_size INT NOT NULL CONSTRAINT ck_provider_capability_page CHECK (max_page_size BETWEEN 1 AND 1000000),
        transactional_supported BIT NOT NULL CONSTRAINT df_provider_capability_tx DEFAULT 0,
        lifecycle_status NVARCHAR(32) NOT NULL CONSTRAINT df_provider_capability_status DEFAULT N'ACTIVE',
        effective_from DATETIME2(7) NOT NULL CONSTRAINT df_provider_capability_effective DEFAULT SYSUTCDATETIME(),
        retired_at DATETIME2(7) NULL,
        CONSTRAINT uq_provider_capability UNIQUE(provider_code, family_code, operation_code, feature_code),
        CONSTRAINT ck_provider_capability_code CHECK (provider_code NOT LIKE N'%[^A-Za-z0-9_.-]%' AND provider_code NOT LIKE N'[0-9]%'),
        CONSTRAINT ck_provider_capability_status CHECK (lifecycle_status IN (N'ACTIVE',N'RETIRED'))
    );
    CREATE INDEX ix_provider_capability_lookup ON platform.provider_capability(provider_code, family_code, operation_code, lifecycle_status);
END;
