/* KIDS R8 delegated owner authorization is recorded as a governed READY
   mapping state.  This is deliberately scoped to the immutable R8 revision
   and does not weaken the fail-closed rule for any other contract. */
UPDATE rs
SET mapping_spec_json = JSON_MODIFY(
      JSON_MODIFY(CASE WHEN ISJSON(rs.mapping_spec_json)=1 THEN rs.mapping_spec_json ELSE N'{}' END,
                  '$.approvalState', N'READY'),
      '$.canonicalAccessRevision', 8)
FROM platform.contract_revision_source rs
JOIN platform.ingestion_contract_revision r
  ON r.ingestion_contract_revision_id=rs.ingestion_contract_revision_id
JOIN platform.ingestion_contract c ON c.contract_id=r.contract_id
WHERE c.contract_code=N'KIDS_PORTAL_V1' AND r.revision=7 AND rs.source_locator LIKE N'ACCESS.__%';

UPDATE c
SET status='ACTIVE', raw_ingest_enabled=1, auto_publish=0
FROM platform.ingestion_contract c
WHERE c.contract_code=N'KIDS_PORTAL_V1' AND c.contract_revision=7;
