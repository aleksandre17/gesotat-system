IF OBJECT_ID(N'platform.contract_query_alias','U') IS NULL
CREATE TABLE platform.contract_query_alias(
 alias_id BIGINT IDENTITY PRIMARY KEY,
 site_contract_revision_id BIGINT NOT NULL,
 dataset_code NVARCHAR(120) NOT NULL,
 alias_code NVARCHAR(160) NOT NULL,
 canonical_role NVARCHAR(160) NOT NULL,
 family VARCHAR(32) NOT NULL,
 value_type VARCHAR(24) NOT NULL,
 status VARCHAR(24) NOT NULL DEFAULT 'APPROVED',
 CONSTRAINT uq_contract_query_alias UNIQUE(site_contract_revision_id,dataset_code,alias_code),
 CONSTRAINT fk_contract_query_alias_revision FOREIGN KEY(site_contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id)
);
DECLARE @r BIGINT=(SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=8);
INSERT platform.contract_query_alias(site_contract_revision_id,dataset_code,alias_code,canonical_role,family,value_type)
SELECT @r,v.dataset_code,v.alias_code,v.canonical_role,v.family,v.value_type FROM (VALUES
(N'KIDS_STATISTICAL_INPUT',N'metricCode',N'METRIC_CODE',N'STATISTICAL',N'CODE'),
(N'KIDS_STATISTICAL_INPUT',N'carrierCode',N'CARRIER_CODE',N'STATISTICAL',N'CODE'),
(N'KIDS_STATISTICAL_INPUT',N'unitCode',N'UNIT_CODE',N'STATISTICAL',N'CODE'),
(N'KIDS_STATISTICAL_INPUT',N'periodFrom',N'PERIOD_FROM',N'STATISTICAL',N'DATE'),
(N'KIDS_STATISTICAL_INPUT',N'periodTo',N'PERIOD_TO',N'STATISTICAL',N'DATE'),
(N'KIDS_STATISTICAL_INPUT',N'ageGroup',N'DIMENSION:AGE_GROUP',N'STATISTICAL',N'CODE'),
(N'KIDS_STATISTICAL_INPUT',N'dimension.*',N'DIMENSION:*',N'STATISTICAL',N'CODE')
)v(dataset_code,alias_code,canonical_role,family,value_type) WHERE @r IS NOT NULL AND NOT EXISTS(SELECT 1 FROM platform.contract_query_alias a WHERE a.site_contract_revision_id=@r AND a.dataset_code=v.dataset_code AND a.alias_code=v.alias_code);
