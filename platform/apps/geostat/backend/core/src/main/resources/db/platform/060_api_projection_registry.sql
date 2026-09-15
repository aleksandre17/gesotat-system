/* Versioned executable projection registry; projections are contract data, not controller branches. */
IF OBJECT_ID(N'platform.api_projection',N'U') IS NULL
CREATE TABLE platform.api_projection(
 api_projection_id BIGINT IDENTITY PRIMARY KEY, site_contract_revision_id BIGINT NOT NULL,
 projection_code NVARCHAR(160) NOT NULL, dataset_code NVARCHAR(160) NOT NULL,
 projection_family NVARCHAR(64) NOT NULL, mapping_json NVARCHAR(MAX) NOT NULL,
 approval_state VARCHAR(24) NOT NULL, revision INT NOT NULL DEFAULT 1,
 CONSTRAINT uq_api_projection UNIQUE(site_contract_revision_id,projection_code,revision),
 CONSTRAINT fk_api_projection_contract FOREIGN KEY(site_contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id),
 CONSTRAINT ck_api_projection_json CHECK(ISJSON(mapping_json)=1)
);
DECLARE @r BIGINT=(SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=8);
INSERT platform.api_projection(site_contract_revision_id,projection_code,dataset_code,projection_family,mapping_json,approval_state)
SELECT @r,v.code,v.dataset,v.family,v.mapping,'APPROVED' FROM (VALUES
(N'KIDS_GOAL_ENTITY',N'KIDS_GOAL',N'ENTITY',N'{"shape":"ENTITY_COLLECTION","include":["entity_id","external_key","title","payload_json","localizedText","classifications","locators","lineage"]}'),
(N'KIDS_RESOURCE_ENTITY',N'KIDS_RESOURCE',N'ENTITY',N'{"shape":"ENTITY_COLLECTION","include":["entity_id","external_key","title","payload_json","localizedText","classifications","locators","lineage"]}'),
(N'KIDS_GLOSSARY_ENTRY_ENTITY',N'KIDS_GLOSSARY_ENTRY',N'ENTITY',N'{"shape":"ENTITY_COLLECTION","include":["entity_id","external_key","title","payload_json","localizedText","lineage"]}'),
(N'KIDS_STATS_INPUT',N'KIDS_STATISTICAL_INPUT',N'STATISTICAL',N'{"shape":"SERIES_WITH_OBSERVATIONS","include":["seriesId","metricCode","unitCode","aggregation","observations","dimensions","lineage"]}'),
(N'KIDS_CLASSIFIER_ITEM',N'KIDS_CLASSIFIER_ITEM',N'REFERENCE',N'{"shape":"REFERENCE_COLLECTION","include":["classification_item_id","code","label_ka","label_en","parent_item_id","status"]}')
)v(code,dataset,family,mapping) WHERE @r IS NOT NULL AND NOT EXISTS(SELECT 1 FROM platform.api_projection p WHERE p.site_contract_revision_id=@r AND p.projection_code=v.code AND p.revision=1);
