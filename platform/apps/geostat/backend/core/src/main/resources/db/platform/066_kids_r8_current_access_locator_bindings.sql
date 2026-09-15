/* Current mutable contract sources must use the same prefixed Access names
   as the immutable R8 package. */
UPDATE cs
SET source_locator = CASE d.dataset_code
    WHEN N'KIDS_RAW_DOCUMENT' THEN N'ACCESS.__raw_document'
    WHEN N'KIDS_GOAL' THEN N'ACCESS.__ent_kids_goal'
    WHEN N'KIDS_RESOURCE' THEN N'ACCESS.__ent_kids_resource'
    WHEN N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT' THEN N'ACCESS.__rel_kids_resource_subcategory_assignment'
    WHEN N'KIDS_GLOSSARY_ENTRY' THEN N'ACCESS.__ent_kids_glossary_entry'
    WHEN N'KIDS_STATISTICAL_CARRIER' THEN N'ACCESS.__raw_kids_statistical_carrier'
    WHEN N'KIDS_STATISTICAL_INPUT' THEN N'ACCESS.__stat_kids_statistical_input'
    WHEN N'KIDS_STATISTICAL_SEMANTIC_BINDING' THEN N'ACCESS.__rel_kids_statistical_semantic_binding'
    ELSE cs.source_locator END
FROM platform.contract_source cs
JOIN platform.dataset_version dv ON dv.dataset_version_id=cs.target_dataset_version_id
JOIN platform.dataset d ON d.dataset_id=dv.dataset_id
WHERE d.dataset_code IN (N'KIDS_RAW_DOCUMENT',N'KIDS_GOAL',N'KIDS_RESOURCE',N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',N'KIDS_GLOSSARY_ENTRY',N'KIDS_STATISTICAL_CARRIER',N'KIDS_STATISTICAL_INPUT',N'KIDS_STATISTICAL_SEMANTIC_BINDING')
  AND cs.active=1;
