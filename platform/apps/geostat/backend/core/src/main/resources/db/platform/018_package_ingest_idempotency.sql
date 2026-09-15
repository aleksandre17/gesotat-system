/* A canonical package has at most one load per batch/dataset/source. This is the database idempotency boundary. */
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'uq_dataset_load_batch_version_source' AND object_id=OBJECT_ID(N'ingest.dataset_load'))
  CREATE UNIQUE INDEX uq_dataset_load_batch_version_source ON ingest.dataset_load(batch_id,dataset_version_id,source_name);
