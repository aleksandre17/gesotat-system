# Resumable Access ingest

Each dataset load has a durable checkpoint. Rows are uniquely identified by `(dataset_load_id, source_row_number)`; a restart may replay a chunk safely without duplicating staged rows. Chunks commit independently and update `ingest.load_checkpoint.last_source_row_number` only after their insert batch succeeds.

Resume opens the same artifact/load, seeks past the recorded row number, and continues. A completed checkpoint is immutable. Raw artifact checksum and each row payload hash remain the lineage/idempotency evidence.
