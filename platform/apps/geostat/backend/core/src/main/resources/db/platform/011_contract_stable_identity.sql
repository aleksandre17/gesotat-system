/* Stable, portable identity for a Control-Plane-first ingestion contract.
   IDs remain internal; packages bind by code + revision, never by database ID. */
IF COL_LENGTH(N'platform.ingestion_contract', N'contract_code') IS NULL
  ALTER TABLE platform.ingestion_contract ADD contract_code NVARCHAR(160) NULL;

EXEC(N'UPDATE platform.ingestion_contract
      SET contract_code=CONCAT(N''CONTRACT_'',contract_id)
      WHERE contract_code IS NULL;');

IF COL_LENGTH(N'platform.ingestion_contract', N'contract_revision') IS NULL
  ALTER TABLE platform.ingestion_contract ADD contract_revision INT NOT NULL CONSTRAINT df_platform_contract_revision DEFAULT 1;

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'uq_platform_contract_code' AND object_id=OBJECT_ID(N'platform.ingestion_contract'))
  EXEC(N'CREATE UNIQUE INDEX uq_platform_contract_code ON platform.ingestion_contract(contract_code);');

IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'platform.ingestion_contract') AND name=N'contract_code' AND is_nullable=1)
  EXEC(N'ALTER TABLE platform.ingestion_contract ALTER COLUMN contract_code NVARCHAR(160) NOT NULL;');
