/* Clarify the R8 statistical input contract: source/typed pairs are intentional
   lossless boundaries; the DSD natural grain is carrier_code + cell_ordinal. */
DECLARE @r BIGINT=(SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=8);
DECLARE @d BIGINT=(SELECT TOP 1 contract_dataset_id FROM platform.site_contract_dataset WHERE site_contract_revision_id=@r AND dataset_code=N'KIDS_STATISTICAL_INPUT');
UPDATE platform.site_contract_field SET semantic_role='IDENTIFIER',key_role='PRIMARY' WHERE contract_dataset_id=@d AND field_name=N'input_key';
UPDATE platform.site_contract_field SET semantic_role='PERIOD' WHERE contract_dataset_id=@d AND field_name=N'period_normalized';
UPDATE platform.site_contract_field SET key_role='NATURAL' WHERE contract_dataset_id=@d AND field_name IN(N'carrier_code',N'cell_ordinal');
/* The opaque input_key is a physical Access primary key; the semantic natural grain is explicit. */
