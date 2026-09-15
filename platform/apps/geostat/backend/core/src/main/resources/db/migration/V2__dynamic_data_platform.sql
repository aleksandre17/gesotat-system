/*
  Dynamic Data Platform foundation for the core SQL Server database.
  This script is deliberately idempotent and must be applied through the
  deployment migration procedure, not by an uploaded Access package.
*/

IF OBJECT_ID(N'dbo.data_profiles', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.data_profiles (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        page_id BIGINT NOT NULL,
        profile_code NVARCHAR(120) NOT NULL,
        data_mode VARCHAR(16) NOT NULL,
        data_kind VARCHAR(16) NOT NULL,
        target_database NVARCHAR(128) NOT NULL,
        target_schema NVARCHAR(128) NOT NULL,
        target_table NVARCHAR(128) NOT NULL,
        row_key_json NVARCHAR(MAX) NULL,
        display_columns_json NVARCHAR(MAX) NULL,
        allowed_query_json NVARCHAR(MAX) NULL,
        enabled BIT NOT NULL CONSTRAINT df_data_profiles_enabled DEFAULT 1,
        version INT NOT NULL CONSTRAINT df_data_profiles_version DEFAULT 1,
        CONSTRAINT uk_data_profiles_page UNIQUE (page_id),
        CONSTRAINT uk_data_profiles_code UNIQUE (profile_code)
    );
END;

IF OBJECT_ID(N'dbo.import_table_mappings', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.import_table_mappings (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        profile_id BIGINT NOT NULL,
        access_table_name NVARCHAR(128) NOT NULL,
        table_role VARCHAR(16) NOT NULL,
        import_mode VARCHAR(16) NOT NULL,
        column_mapping_json NVARCHAR(MAX) NULL,
        validation_rules_json NVARCHAR(MAX) NULL,
        enabled BIT NOT NULL CONSTRAINT df_import_table_mappings_enabled DEFAULT 1,
        CONSTRAINT uk_import_mapping_profile_table UNIQUE (profile_id, access_table_name),
        CONSTRAINT fk_import_table_mappings_profile FOREIGN KEY (profile_id)
            REFERENCES dbo.data_profiles(id)
    );
END;

IF OBJECT_ID(N'dbo.chart_definitions', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.chart_definitions (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        profile_id BIGINT NOT NULL,
        chart_code NVARCHAR(120) NOT NULL,
        chart_type VARCHAR(32) NOT NULL,
        x_field NVARCHAR(128) NULL,
        y_field NVARCHAR(128) NULL,
        series_field NVARCHAR(128) NULL,
        aggregation NVARCHAR(16) NULL,
        filters_json NVARCHAR(MAX) NULL,
        display_json NVARCHAR(MAX) NULL,
        publication_status VARCHAR(16) NOT NULL,
        version INT NOT NULL CONSTRAINT df_chart_definitions_version DEFAULT 1,
        CONSTRAINT uk_chart_definition_profile_code UNIQUE (profile_id, chart_code, version),
        CONSTRAINT fk_chart_definitions_profile FOREIGN KEY (profile_id)
            REFERENCES dbo.data_profiles(id)
    );
END;

IF OBJECT_ID(N'dbo.import_jobs', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.import_jobs (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        package_code NVARCHAR(120) NULL,
        package_version NVARCHAR(64) NULL,
        original_file_name NVARCHAR(255) NOT NULL,
        file_checksum NVARCHAR(128) NOT NULL,
        file_size_bytes BIGINT NOT NULL,
        status VARCHAR(24) NOT NULL,
        submitted_by_user_id BIGINT NULL,
        created_at DATETIME2 NOT NULL,
        completed_at DATETIME2 NULL,
        error_summary NVARCHAR(MAX) NULL
    );
END;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_import_jobs_status' AND object_id = OBJECT_ID(N'dbo.import_jobs'))
    CREATE INDEX ix_import_jobs_status ON dbo.import_jobs(status);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'ix_import_jobs_checksum' AND object_id = OBJECT_ID(N'dbo.import_jobs'))
    CREATE INDEX ix_import_jobs_checksum ON dbo.import_jobs(file_checksum);

IF OBJECT_ID(N'dbo.import_job_items', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.import_job_items (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        import_job_id BIGINT NOT NULL,
        profile_id BIGINT NULL,
        mapping_id BIGINT NULL,
        access_table_name NVARCHAR(128) NOT NULL,
        target_database NVARCHAR(128) NULL,
        target_schema NVARCHAR(128) NULL,
        target_table NVARCHAR(128) NULL,
        status VARCHAR(16) NOT NULL,
        source_row_count BIGINT NULL,
        inserted_row_count BIGINT NULL,
        rejected_row_count BIGINT NULL,
        error_detail NVARCHAR(MAX) NULL,
        CONSTRAINT uk_import_job_item_table UNIQUE (import_job_id, access_table_name),
        CONSTRAINT fk_import_job_items_job FOREIGN KEY (import_job_id)
            REFERENCES dbo.import_jobs(id),
        CONSTRAINT fk_import_job_items_profile FOREIGN KEY (profile_id)
            REFERENCES dbo.data_profiles(id),
        CONSTRAINT fk_import_job_items_mapping FOREIGN KEY (mapping_id)
            REFERENCES dbo.import_table_mappings(id)
    );
END;
