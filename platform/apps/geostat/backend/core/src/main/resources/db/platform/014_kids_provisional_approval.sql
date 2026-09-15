DECLARE @product BIGINT=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');
UPDATE m SET status='PROVISIONAL_APPROVED' FROM platform.metric m JOIN platform.dataset d ON d.dataset_id=m.source_dataset_id WHERE d.product_id=@product AND m.metric_code LIKE N'KIDS_FILE_%';
UPDATE platform.ingestion_contract SET status='PROVISIONAL_APPROVED',contract_revision=2 WHERE contract_code=N'KIDS_PORTAL_V1';
