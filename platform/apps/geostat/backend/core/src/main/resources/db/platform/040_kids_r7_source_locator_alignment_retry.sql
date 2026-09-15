/* Idempotent retry of the KIDS revision-7 locator alignment. */
SET NOCOUNT ON;
DECLARE @revisionId BIGINT;
SELECT TOP (1) @revisionId=ingestion_contract_revision_id FROM platform.ingestion_contract_revision WHERE revision=7 AND lifecycle_status='PROVISIONAL_APPROVED' ORDER BY ingestion_contract_revision_id DESC;
IF @revisionId IS NULL RETURN;
;WITH ranked AS (
 SELECT rs.contract_revision_source_id, ROW_NUMBER() OVER (PARTITION BY d.dataset_code ORDER BY CASE WHEN rs.source_locator LIKE N'%.__%' THEN 0 ELSE 1 END, rs.contract_revision_source_id) rn
 FROM platform.contract_revision_source rs JOIN platform.dataset_version dv ON dv.dataset_version_id=rs.target_dataset_version_id JOIN platform.dataset d ON d.dataset_id=dv.dataset_id
 WHERE rs.ingestion_contract_revision_id=@revisionId)
DELETE rs FROM platform.contract_revision_source rs JOIN ranked x ON x.contract_revision_source_id=rs.contract_revision_source_id WHERE x.rn>1;
UPDATE rs SET source_locator=v.source_locator
FROM platform.contract_revision_source rs JOIN platform.dataset_version dv ON dv.dataset_version_id=rs.target_dataset_version_id JOIN platform.dataset d ON d.dataset_id=dv.dataset_id
JOIN (VALUES (N'KIDS_GOAL',N'__ent_kids_goal'),(N'KIDS_RESOURCE',N'__ent_kids_resource'),(N'KIDS_RESOURCE_SUBCATEGORY_ASSIGNMENT',N'__rel_kids_resource_subcategory_assignment'),(N'KIDS_GLOSSARY_ENTRY',N'__ent_kids_glossary_entry'),(N'KIDS_STATISTICAL_CARRIER',N'__raw_kids_statistical_carrier'),(N'KIDS_STATISTICAL_INPUT',N'__stat_kids_statistical_input'),(N'KIDS_STATISTICAL_SEMANTIC_BINDING',N'__rel_kids_statistical_semantic_binding')) v(dataset_code,source_locator) ON v.dataset_code=d.dataset_code
WHERE rs.ingestion_contract_revision_id=@revisionId;
