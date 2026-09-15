/* Provisional inferred Kids statistical contract: versioned metadata only; no legacy writes. */
DECLARE @product BIGINT=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL');
DECLARE @files BIGINT=(SELECT dataset_id FROM platform.dataset WHERE product_id=@product AND dataset_code=N'KIDS_FILE_RESOURCE');
IF NOT EXISTS(SELECT 1 FROM platform.dimension WHERE dimension_code=N'AGE_GROUP') INSERT platform.dimension(dimension_code,value_type,title_ka,title_en) VALUES(N'AGE_GROUP','CLASSIFICATION',N'ასაკობრივი ჯგუფი',N'Age group');
IF NOT EXISTS(SELECT 1 FROM platform.measure WHERE measure_code=N'OBS_VALUE') INSERT platform.measure(measure_code,value_type,aggregation_default,title_ka,title_en) VALUES(N'OBS_VALUE','DECIMAL','SUM',N'მნიშვნელობა',N'Observation value');
INSERT platform.metric(metric_code,source_dataset_id,measure_id,aggregation,status)
SELECT CONCAT(N'KIDS_FILE_',f.ID),@files,m.measure_id,'SUM','DRAFT' FROM (VALUES(162),(163),(164),(165),(166),(167),(168),(169),(170),(171),(172),(173),(174),(175),(176),(177),(178),(179),(180),(181),(183),(297),(299),(301),(303),(305),(306),(308),(310),(385),(386),(387))f(ID) CROSS JOIN platform.measure m
WHERE m.measure_code=N'OBS_VALUE' AND NOT EXISTS(SELECT 1 FROM platform.metric x WHERE x.metric_code=CONCAT(N'KIDS_FILE_',f.ID));
