/* Separate permission to retain governed raw evidence from semantic-contract approval. */
IF COL_LENGTH(N'platform.ingestion_contract', N'raw_ingest_enabled') IS NULL
  ALTER TABLE platform.ingestion_contract ADD raw_ingest_enabled BIT NOT NULL CONSTRAINT df_ingestion_contract_raw_ingest_enabled DEFAULT(0);

/* KIDS can stage immutable source evidence while its semantic/statistical projections remain under review. */
EXEC(N'UPDATE platform.ingestion_contract
      SET status=''REVIEW_REQUIRED'', raw_ingest_enabled=1
      WHERE contract_code=N''KIDS_PORTAL_V1'';');
