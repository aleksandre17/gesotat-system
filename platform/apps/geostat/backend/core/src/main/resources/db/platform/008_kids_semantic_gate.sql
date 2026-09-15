/* Run in geostat-system. The discovery seed is not an executable semantic contract. */
UPDATE cs
SET mapping_spec_json=JSON_MODIFY(cs.mapping_spec_json,'$.approvalState','DRAFT')
FROM platform.contract_source cs
JOIN platform.ingestion_contract c ON c.contract_id=cs.contract_id
JOIN platform.dataset d ON d.dataset_id=c.dataset_id
WHERE d.dataset_code=N'KIDS_CONTENT'
  AND JSON_VALUE(cs.mapping_spec_json,'$.approvalState') IS NULL;
