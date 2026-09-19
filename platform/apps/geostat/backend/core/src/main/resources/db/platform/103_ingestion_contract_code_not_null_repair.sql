/* Run in geostat-system (Control Plane).
   Repair for installations where migration 011 was recorded although its last statement never ran:
   the unique index was created before ALTER COLUMN ... NOT NULL, the ALTER failed and the runner of
   that time did not see the failure. Fresh installs get the corrected 011 and skip every branch here. */
IF EXISTS (SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'platform.ingestion_contract') AND name=N'contract_code' AND is_nullable=1)
BEGIN
  IF EXISTS (SELECT 1 FROM platform.ingestion_contract WHERE contract_code IS NULL)
    THROW 51103, 'platform.ingestion_contract has rows without contract_code; assign codes before this repair.', 1;
  IF EXISTS (SELECT 1 FROM sys.indexes WHERE name=N'uq_platform_contract_code' AND object_id=OBJECT_ID(N'platform.ingestion_contract'))
    EXEC(N'DROP INDEX uq_platform_contract_code ON platform.ingestion_contract;');
  EXEC(N'ALTER TABLE platform.ingestion_contract ALTER COLUMN contract_code NVARCHAR(160) NOT NULL;');
  EXEC(N'CREATE UNIQUE INDEX uq_platform_contract_code ON platform.ingestion_contract(contract_code);');
END;
