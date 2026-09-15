/* Canonical storage authority is separate from Access transport locator. */
IF COL_LENGTH(N'platform.contract_table_definition',N'canonical_storage_name') IS NULL
    ALTER TABLE platform.contract_table_definition ADD canonical_storage_name NVARCHAR(128) NULL;
IF COL_LENGTH(N'platform.contract_table_definition',N'canonical_dataset_version_id') IS NULL
    ALTER TABLE platform.contract_table_definition ADD canonical_dataset_version_id BIGINT NULL;

UPDATE t SET canonical_storage_name=CASE
    WHEN t.logical_table_code IN (N'KIDS_GOAL',N'KIDS_RESOURCE',N'KIDS_GLOSSARY_ENTRY') THEN N'entity.entity_record'
    WHEN t.logical_table_code=N'KIDS_STATISTICAL_INPUT' THEN N'statistics.observation'
    ELSE t.canonical_storage_name END
FROM platform.contract_table_definition t
WHERE t.revision=8 AND t.storage_plane='DATA'
  AND t.logical_table_code IN (N'KIDS_GOAL',N'KIDS_RESOURCE',N'KIDS_GLOSSARY_ENTRY',N'KIDS_STATISTICAL_INPUT');

UPDATE t SET canonical_dataset_version_id=sc.dataset_version_id
FROM platform.contract_table_definition t
JOIN platform.site_contract_dataset sc
  ON sc.site_contract_revision_id=(SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE revision=8 ORDER BY site_contract_revision_id DESC)
 AND sc.dataset_code=t.logical_table_code
WHERE t.revision=8 AND t.storage_plane='DATA';
