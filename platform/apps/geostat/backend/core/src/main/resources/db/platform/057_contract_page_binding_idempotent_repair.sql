/* Replaces re-execution of 054 on installations where its checksum is already recorded. */
IF OBJECT_ID(N'platform.contract_page_binding',N'U') IS NULL
BEGIN
  CREATE TABLE platform.contract_page_binding(
    page_binding_id BIGINT IDENTITY PRIMARY KEY, site_contract_revision_id BIGINT NOT NULL,
    page_code NVARCHAR(160) NOT NULL, node_id BIGINT NOT NULL, dataset_code NVARCHAR(160) NULL,
    response_projection_code NVARCHAR(160) NULL, runtime_page_id BIGINT NULL, access_page_code NVARCHAR(160) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE', CONSTRAINT uq_contract_page_binding UNIQUE(site_contract_revision_id,page_code),
    CONSTRAINT fk_page_binding_revision FOREIGN KEY(site_contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id),
    CONSTRAINT fk_page_binding_node FOREIGN KEY(node_id) REFERENCES platform.site_contract_node(node_id));
END;
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ix_contract_page_binding_runtime' AND object_id=OBJECT_ID(N'platform.contract_page_binding'))
CREATE INDEX ix_contract_page_binding_runtime ON platform.contract_page_binding(runtime_page_id) WHERE runtime_page_id IS NOT NULL;
DECLARE @r BIGINT=(SELECT site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=7);
INSERT platform.contract_page_binding(site_contract_revision_id,page_code,node_id,dataset_code,response_projection_code,access_page_code)
SELECT @r,n.node_code,n.node_id,n.dataset_code,CASE n.node_code WHEN N'KIDS_GOALS' THEN N'KIDS_GOAL_ENTITY' WHEN N'KIDS_RESOURCES' THEN N'KIDS_RESOURCE_ENTITY' WHEN N'KIDS_GLOSSARY' THEN N'KIDS_GLOSSARY_ENTRY_ENTITY' WHEN N'KIDS_STATISTICS' THEN N'KIDS_STATS_INPUT' END,n.node_code FROM platform.site_contract_node n WHERE n.site_contract_revision_id=@r AND NOT EXISTS(SELECT 1 FROM platform.contract_page_binding b WHERE b.site_contract_revision_id=@r AND b.page_code=n.node_code);
