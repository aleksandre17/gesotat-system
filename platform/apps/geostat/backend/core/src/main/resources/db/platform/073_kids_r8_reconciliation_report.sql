/* Deterministic reconciliation report used by release acceptance and DR replay. */
EXEC(N'CREATE OR ALTER PROCEDURE publication.usp_kids_r8_reconciliation AS
BEGIN
  SET NOCOUNT ON;
  SELECT ''PUBLISHED_SNAPSHOT'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM publication.snapshot WHERE status=''PUBLISHED'';
  SELECT ''PUBLISHED_MEMBERS'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM publication.snapshot_member sm JOIN publication.snapshot s ON s.snapshot_id=sm.snapshot_id WHERE s.status=''PUBLISHED'';
  SELECT ''STATISTICAL_ROWS'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM [statistics].observation o WHERE o.is_current=1;
  SELECT ''CACHE_ROWS'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM serving.metric_cache c JOIN publication.snapshot s ON s.snapshot_id=c.snapshot_id WHERE s.status=''PUBLISHED'';
  SELECT ''GATE_FAILURES'' AS check_code, COUNT_BIG(*) AS observed_count
    FROM publication.release_gate_audit WHERE result<>''PASS'';
END');
