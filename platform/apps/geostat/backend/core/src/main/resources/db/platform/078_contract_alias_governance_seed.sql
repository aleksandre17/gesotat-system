/* Generic, family-agnostic aliases: every approved contract field is queryable by its canonical field name. */
IF OBJECT_ID(N'platform.contract_query_alias','U') IS NOT NULL
BEGIN
 INSERT platform.contract_query_alias(site_contract_revision_id,dataset_code,alias_code,canonical_role,family,value_type,status)
 SELECT d.site_contract_revision_id,d.dataset_code,f.field_name,CONCAT(N'FIELD:',f.field_name),d.data_family,
        CASE WHEN f.logical_type LIKE '%INT%' OR f.logical_type LIKE '%DECIMAL%' OR f.logical_type LIKE '%NUM%' THEN 'NUMBER'
             WHEN f.logical_type LIKE '%DATE%' OR f.logical_type LIKE '%TIME%' THEN 'DATE' ELSE 'TEXT' END,'APPROVED'
 FROM platform.site_contract_dataset d JOIN platform.site_contract_field f ON f.contract_dataset_id=d.contract_dataset_id
 JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id
 WHERE r.status IN('APPROVED','PUBLISHED')
   AND NOT EXISTS(SELECT 1 FROM platform.contract_query_alias a WHERE a.site_contract_revision_id=d.site_contract_revision_id AND a.dataset_code=d.dataset_code AND a.alias_code=f.field_name);
END
