/* Canonical storage authority is separate from Access transport locator. */
IF COL_LENGTH(N'platform.contract_table_definition',N'canonical_storage_name') IS NULL
    ALTER TABLE platform.contract_table_definition ADD canonical_storage_name NVARCHAR(128) NULL;

UPDATE t SET canonical_storage_name=CASE
    WHEN t.logical_table_code IN (N'KIDS_GOAL',N'KIDS_RESOURCE',N'KIDS_GLOSSARY_ENTRY') THEN N'entity.entity_record'
    WHEN t.logical_table_code=N'KIDS_STATISTICAL_INPUT' THEN N'statistics.observation'
    ELSE t.canonical_storage_name END
FROM platform.contract_table_definition t
WHERE t.revision=8 AND t.storage_plane='DATA'
  AND t.logical_table_code IN (N'KIDS_GOAL',N'KIDS_RESOURCE',N'KIDS_GLOSSARY_ENTRY',N'KIDS_STATISTICAL_INPUT');
