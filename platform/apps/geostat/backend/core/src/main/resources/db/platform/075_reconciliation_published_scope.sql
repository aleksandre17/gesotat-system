/* Correct the reconciliation scope: observations are counted only when they
   belong to a currently published snapshot, never from staging/replay rows. */
EXEC(N'CREATE OR ALTER PROCEDURE publication.usp_kids_r8_reconciliation AS
BEGIN
  SET NOCOUNT ON;
  SELECT ''PUBLISHED_SNAPSHOT'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM publication.snapshot WHERE status=''PUBLISHED'';
  SELECT ''PUBLISHED_MEMBERS'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM publication.snapshot_member sm JOIN publication.snapshot s ON s.snapshot_id=sm.snapshot_id WHERE s.status=''PUBLISHED'';
  SELECT ''PUBLISHED_STATISTICAL_ROWS'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM [statistics].observation o JOIN [statistics].series sr ON sr.series_id=o.series_id
    JOIN publication.snapshot_member sm ON sm.dataset_snapshot_id=sr.dataset_snapshot_id
    JOIN publication.snapshot ps ON ps.snapshot_id=sm.snapshot_id WHERE ps.status=''PUBLISHED'' AND o.is_current=1;
  SELECT ''PUBLISHED_CACHE_ROWS'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM serving.metric_cache c JOIN publication.snapshot s ON s.snapshot_id=c.snapshot_id WHERE s.status=''PUBLISHED'';
  SELECT ''GATE_FAILURES'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM publication.release_gate_audit WHERE result<>''PASS'';
END');
