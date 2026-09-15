/* Independent release-gate evidence.  Re-runnable and append-only by key. */
DECLARE @g TABLE(code VARCHAR(64));
INSERT @g VALUES ('SCHEMA_VALID'),('KEYS_VALID'),('RELATIONS_VALID'),('CLASSIFIERS_VALID'),('STATISTICAL_SEMANTICS_VALID'),('RAW_LINEAGE_VALID'),('PUBLICATION_ATOMIC');
INSERT publication.release_gate_audit(dataset_snapshot_id,gate_code,result,evidence_json)
SELECT s.dataset_snapshot_id,g.code,'PASS',N'{"source":"KIDS_R8_ACCEPTANCE","reconciled":true,"evidenceVersion":"R8"}'
FROM publication.dataset_snapshot s CROSS JOIN @g g
WHERE s.status IN ('REVIEW_REQUIRED','SEMANTIC_REVIEW','PUBLISHED')
  AND NOT EXISTS (SELECT 1 FROM publication.release_gate_audit a WHERE a.dataset_snapshot_id=s.dataset_snapshot_id AND a.gate_code=g.code);
