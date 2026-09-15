/* Select the nearest prior definition that actually owns field metadata. */
INSERT platform.contract_field_definition(table_definition_id,field_name,ordinal,logical_type,physical_type,semantic_role,required,nullable,default_expression,classifier_scheme_code,relation_target,ui_control_type,ui_visible,ui_editable,ui_filterable,ui_sortable,ui_groupable,validation_json,lifecycle_status)
SELECT n.table_definition_id,f.field_name,f.ordinal,f.logical_type,f.physical_type,f.semantic_role,f.required,f.nullable,f.default_expression,f.classifier_scheme_code,f.relation_target,f.ui_control_type,f.ui_visible,f.ui_editable,f.ui_filterable,f.ui_sortable,f.ui_groupable,f.validation_json,'APPROVED'
FROM platform.contract_table_definition n
CROSS APPLY (SELECT TOP 1 o.table_definition_id FROM platform.contract_table_definition o JOIN platform.contract_field_definition fx ON fx.table_definition_id=o.table_definition_id WHERE o.logical_table_code=n.logical_table_code AND o.revision<n.revision ORDER BY o.revision DESC,o.table_definition_id DESC) src
JOIN platform.contract_field_definition f ON f.table_definition_id=src.table_definition_id
WHERE n.revision=8 AND NOT EXISTS(SELECT 1 FROM platform.contract_field_definition z WHERE z.table_definition_id=n.table_definition_id AND z.field_name=f.field_name);
