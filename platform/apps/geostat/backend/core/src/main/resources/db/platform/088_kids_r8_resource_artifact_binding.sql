/* Run in geostat-system (Control Plane).
   KIDS R8 conformance binding for the generic artifact-attachment primitives (086).
   Evidence: docs/evidence/kids-r8-resource-artifact-binding-2026-09-18.json
   - source_resource_id: 225/225 unique, non-null (stable key proof);
   - path_ka/path_en: 450/450 exact matches to package entries under mainstat/
     after removing the transport prefix files/; 450 distinct content objects.
   Access mode reproduces the legacy exposure (published static files) without
   widening it; the API boundary stays authenticated until EXT-1 decides otherwise. */

MERGE platform.artifact_policy AS t
USING (SELECT N'KIDS_PUBLIC_STATISTICAL_FILE' code, 1 rev) s
ON t.policy_code=s.code AND t.revision=s.rev
WHEN NOT MATCHED THEN INSERT(policy_code,revision,access_mode,required_authority,allowed_media_types_json,max_bytes,signed_url_ttl_seconds,retention_class,lifecycle_status)
VALUES(s.code,s.rev,'PUBLIC_WHEN_PUBLISHED',NULL,
       N'["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel"]',
       52428800,300,'RETAIN_INDEFINITE','APPROVED');

MERGE platform.artifact_relation_definition AS t
USING (
  SELECT sc.dataset_version_id, N'PRIMARY_FILE' relation_code, p.artifact_policy_id
  FROM platform.site_contract_dataset sc
  JOIN platform.site_contract_revision r ON r.site_contract_revision_id=sc.site_contract_revision_id
  JOIN platform.artifact_policy p ON p.policy_code=N'KIDS_PUBLIC_STATISTICAL_FILE' AND p.revision=1
  WHERE r.contract_code=N'KIDS_PORTAL_V1' AND r.revision=8 AND sc.dataset_code=N'KIDS_RESOURCE'
) s
ON t.dataset_version_id=s.dataset_version_id AND t.relation_code=s.relation_code
WHEN NOT MATCHED THEN INSERT(dataset_version_id,relation_code,artifact_role,artifact_policy_id,min_per_row,max_per_row,ordered,match_rule_json,lifecycle_status)
VALUES(s.dataset_version_id,s.relation_code,'PRIMARY',s.artifact_policy_id,1,1,0,
       N'{"type":"SOURCE_PATH","normalization":"NFC","stripPrefix":"files/","packageRoot":"mainstat/","bindings":[{"language":"ka","field":"path_ka","required":true},{"language":"en","field":"path_en","required":true}]}',
       'APPROVED');
