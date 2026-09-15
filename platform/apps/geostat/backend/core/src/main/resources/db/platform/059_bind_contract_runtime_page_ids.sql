/* Explicit runtime identity for contract-driven APIs.  The mapping is contract data, not application code. */
DECLARE @r BIGINT=(SELECT site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=8);
UPDATE b SET runtime_page_id=CASE b.page_code WHEN N'KIDS_ROOT' THEN 7 WHEN N'KIDS_GOALS' THEN 8 WHEN N'KIDS_RESOURCES' THEN 9 WHEN N'KIDS_GLOSSARY' THEN 10 WHEN N'KIDS_STATISTICS' THEN 11 WHEN N'KIDS_CLASSIFIERS' THEN 12 END
FROM platform.contract_page_binding b WHERE b.site_contract_revision_id=@r AND b.runtime_page_id IS NULL;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ux_contract_page_binding_runtime_active' AND object_id=OBJECT_ID(N'platform.contract_page_binding'))
CREATE UNIQUE INDEX ux_contract_page_binding_runtime_active ON platform.contract_page_binding(site_contract_revision_id,runtime_page_id) WHERE runtime_page_id IS NOT NULL;
