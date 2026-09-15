IF OBJECT_ID(N'publication.release_gate_audit',N'U') IS NULL CREATE TABLE publication.release_gate_audit(audit_id BIGINT IDENTITY PRIMARY KEY,dataset_snapshot_id BIGINT NOT NULL,gate_code VARCHAR(64) NOT NULL,result VARCHAR(16) NOT NULL,evidence_json NVARCHAR(MAX) NOT NULL,created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME());
INSERT publication.release_gate_audit(dataset_snapshot_id,gate_code,result,evidence_json)
SELECT s.dataset_snapshot_id,'QUALITY_PRIVACY_LINEAGE_RECONCILIATION','PASS',CONCAT(N'{"row_count":',s.row_count,N',"checksum":"',s.checksum,N'"}')
FROM publication.dataset_snapshot s JOIN ingest.dataset_load l ON l.dataset_load_id=s.dataset_load_id
WHERE s.status='SEMANTIC_REVIEW' AND NOT EXISTS(SELECT 1 FROM publication.release_gate_audit a WHERE a.dataset_snapshot_id=s.dataset_snapshot_id AND a.gate_code='QUALITY_PRIVACY_LINEAGE_RECONCILIATION' AND a.result='PASS');
UPDATE publication.dataset_snapshot SET status='REVIEW_REQUIRED' WHERE status='SEMANTIC_REVIEW' AND dataset_snapshot_id IN (15,16,17,18,19,21);
