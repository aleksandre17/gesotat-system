/* Give each site-contract dataset an explicit, immutable link to its physical
   Access table definition. AIR: package admission previously used the legacy
   site_contract_dataset.access_table_name instead of the canonical physical
   definition. */
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF COL_LENGTH(N'platform.site_contract_dataset',N'contract_table_definition_id') IS NULL
  ALTER TABLE platform.site_contract_dataset ADD contract_table_definition_id BIGINT NULL;

IF NOT EXISTS (SELECT 1 FROM sys.foreign_keys WHERE name=N'fk_site_contract_dataset_table_definition')
  ALTER TABLE platform.site_contract_dataset WITH CHECK
    ADD CONSTRAINT fk_site_contract_dataset_table_definition FOREIGN KEY(contract_table_definition_id)
    REFERENCES platform.contract_table_definition(table_definition_id);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'ix_site_contract_dataset_table_definition'
               AND object_id=OBJECT_ID(N'platform.site_contract_dataset'))
  CREATE INDEX ix_site_contract_dataset_table_definition
    ON platform.site_contract_dataset(contract_table_definition_id);

/* Backfill only an unambiguous match. Ambiguity is an invalid contract state,
   so fail the migration instead of guessing between physical definitions. */
;WITH candidates AS (
  SELECT d.contract_dataset_id,t.table_definition_id,
         COUNT_BIG(*) OVER(PARTITION BY d.contract_dataset_id) AS match_count
  FROM platform.site_contract_dataset d
  JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id
  JOIN platform.contract_table_definition t ON t.logical_table_code=d.dataset_code
       AND t.revision=r.revision AND t.lifecycle_status='APPROVED'
  JOIN platform.contract_structure s ON s.structure_id=t.structure_id
       AND s.structure_code=d.dataset_code AND s.revision=t.revision
       AND s.lifecycle_status='APPROVED'
)
UPDATE d SET contract_table_definition_id=c.table_definition_id
FROM platform.site_contract_dataset d JOIN candidates c ON c.contract_dataset_id=d.contract_dataset_id
WHERE d.contract_table_definition_id IS NULL AND c.match_count=1;

IF EXISTS (
  SELECT 1 FROM platform.site_contract_dataset d
  JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id
  JOIN platform.contract_table_definition t ON t.logical_table_code=d.dataset_code
       AND t.revision=r.revision AND t.lifecycle_status='APPROVED'
  JOIN platform.contract_structure s ON s.structure_id=t.structure_id
       AND s.structure_code=d.dataset_code AND s.revision=t.revision
       AND s.lifecycle_status='APPROVED'
  WHERE d.contract_table_definition_id IS NULL
  GROUP BY d.contract_dataset_id HAVING COUNT_BIG(*)>1
)
  THROW 51097, 'A site-contract dataset maps to multiple approved physical table definitions', 1;

COMMIT TRANSACTION;
