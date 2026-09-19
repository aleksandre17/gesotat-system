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

/* The backfill and its ambiguity check live in 098: they name the column added above and
   therefore cannot compile in this batch. */
COMMIT TRANSACTION;
