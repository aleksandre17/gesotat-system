/* Registry objects materialized into the issued KIDS revision are approved. */
UPDATE platform.contract_structure SET lifecycle_status='APPROVED'
WHERE structure_code IN (SELECT structure_code FROM platform.contract_structure WHERE revision IN (1,7));
UPDATE platform.contract_table_definition SET lifecycle_status='APPROVED'
WHERE revision IN (1,7);
UPDATE platform.contract_field_definition SET lifecycle_status='APPROVED'
WHERE table_definition_id IN (SELECT table_definition_id FROM platform.contract_table_definition WHERE revision IN (1,7));
UPDATE platform.contract_structure_relation SET lifecycle_status='APPROVED';
