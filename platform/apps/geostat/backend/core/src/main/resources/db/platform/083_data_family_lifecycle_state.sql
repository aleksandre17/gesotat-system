IF OBJECT_ID(N'platform.data_family_lifecycle',N'U') IS NULL
BEGIN
    CREATE TABLE platform.data_family_lifecycle (
        lifecycle_id NVARCHAR(256) NOT NULL CONSTRAINT pk_data_family_lifecycle PRIMARY KEY,
        state_code NVARCHAR(32) NOT NULL,
        updated_at DATETIME2(7) NOT NULL CONSTRAINT df_data_family_lifecycle_updated DEFAULT SYSUTCDATETIME(),
        CONSTRAINT ck_data_family_lifecycle_state CHECK (state_code IN
          (N'RECEIVED',N'STAGED',N'VALIDATED',N'MATERIALIZED',N'RECONCILED',N'PUBLISHED',N'ROLLED_BACK',N'QUARANTINED'))
    );
END;
