DECLARE @r BIGINT=(SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=8);
UPDATE b SET response_projection_code=CASE b.page_code WHEN N'KIDS_CLASSIFIERS' THEN N'KIDS_CLASSIFIER_ITEM' ELSE b.response_projection_code END
FROM platform.contract_page_binding b WHERE b.site_contract_revision_id=@r;
