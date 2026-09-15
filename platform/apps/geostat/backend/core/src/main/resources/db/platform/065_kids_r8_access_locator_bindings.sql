/* R8 Access package uses strict namespace prefixes. Keep the Control Plane
   source locator identical to the issued physical Access table name. */
/* Older revisions may already contain both an unprefixed and a prefixed
   locator for the same dataset. Remove only the obsolete duplicate row. */
DELETE old
FROM platform.contract_revision_source old
JOIN platform.dataset_version odv ON odv.dataset_version_id=old.target_dataset_version_id
JOIN platform.dataset od ON od.dataset_id=odv.dataset_id
WHERE od.dataset_code IN (N'KIDS_RAW_DOCUMENT',N'KIDS_GOAL',N'KIDS_RESOURCE',N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',N'KIDS_GLOSSARY_ENTRY',N'KIDS_STATISTICAL_CARRIER',N'KIDS_STATISTICAL_INPUT',N'KIDS_STATISTICAL_SEMANTIC_BINDING')
  AND old.source_locator NOT LIKE N'%.__%'
  AND EXISTS (SELECT 1 FROM platform.contract_revision_source newer
              JOIN platform.dataset_version ndv ON ndv.dataset_version_id=newer.target_dataset_version_id
              JOIN platform.dataset nd ON nd.dataset_id=ndv.dataset_id
              WHERE newer.ingestion_contract_revision_id=old.ingestion_contract_revision_id
                AND nd.dataset_code=od.dataset_code
                AND newer.source_locator LIKE N'%.__%');

UPDATE rs
SET source_locator = CASE d.dataset_code
    WHEN N'KIDS_RAW_DOCUMENT' THEN N'ACCESS.__raw_document'
    WHEN N'KIDS_GOAL' THEN N'ACCESS.__ent_kids_goal'
    WHEN N'KIDS_RESOURCE' THEN N'ACCESS.__ent_kids_resource'
    WHEN N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT' THEN N'ACCESS.__rel_kids_resource_subcategory_assignment'
    WHEN N'KIDS_GLOSSARY_ENTRY' THEN N'ACCESS.__ent_kids_glossary_entry'
    WHEN N'KIDS_STATISTICAL_CARRIER' THEN N'ACCESS.__raw_kids_statistical_carrier'
    WHEN N'KIDS_STATISTICAL_INPUT' THEN N'ACCESS.__stat_kids_statistical_input'
    WHEN N'KIDS_STATISTICAL_SEMANTIC_BINDING' THEN N'ACCESS.__rel_kids_statistical_semantic_binding'
    ELSE rs.source_locator END
FROM platform.contract_revision_source rs
JOIN platform.dataset_version dv ON dv.dataset_version_id=rs.target_dataset_version_id
JOIN platform.dataset d ON d.dataset_id=dv.dataset_id
WHERE d.dataset_code IN (N'KIDS_RAW_DOCUMENT',N'KIDS_GOAL',N'KIDS_RESOURCE',N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',N'KIDS_GLOSSARY_ENTRY',N'KIDS_STATISTICAL_CARRIER',N'KIDS_STATISTICAL_INPUT',N'KIDS_STATISTICAL_SEMANTIC_BINDING')
  AND rs.source_locator NOT LIKE N'%.__%';
