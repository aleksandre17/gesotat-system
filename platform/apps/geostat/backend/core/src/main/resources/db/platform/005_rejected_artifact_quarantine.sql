/* Run in geostat-data. Additive migration: preserve checksum history of 002_data_plane.sql. */
IF COL_LENGTH(N'ingest.artifact', N'quarantine_uri') IS NULL
    ALTER TABLE ingest.artifact ADD quarantine_uri NVARCHAR(2048) NULL;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'ingest.artifact') AND name=N'ix_artifact_quarantine_uri')
    EXEC(N'CREATE INDEX ix_artifact_quarantine_uri ON ingest.artifact(quarantine_uri) WHERE quarantine_uri IS NOT NULL;');
