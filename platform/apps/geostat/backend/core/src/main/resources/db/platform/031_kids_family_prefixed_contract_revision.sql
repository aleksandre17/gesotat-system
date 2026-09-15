/* Immutable KIDS family-prefixed physical naming revision. r7 remains untouched. */
DECLARE @oldRev INT=7,@newRev INT=8;
IF NOT EXISTS(SELECT 1 FROM platform.ingestion_contract WHERE contract_revision=@newRev)
  UPDATE TOP(1) platform.ingestion_contract SET contract_revision=@newRev,status='REVIEW_REQUIRED' WHERE contract_code=N'KIDS_PORTAL_V1';
DECLARE @map TABLE(old_structure BIGINT PRIMARY KEY,new_structure BIGINT);
INSERT platform.contract_structure(namespace_id,structure_code,structure_kind,data_class,grain,authority_mode,lifecycle_policy,allowed_content,forbidden_content,lineage_policy,quality_policy_code,confidentiality_policy_code,standard_code,standard_version,ui_capabilities_json,lifecycle_status,revision,checksum)
SELECT src.namespace_id,src.structure_code,src.structure_kind,src.data_class,src.grain,src.authority_mode,src.lifecycle_policy,src.allowed_content,src.forbidden_content,src.lineage_policy,src.quality_policy_code,src.confidentiality_policy_code,src.standard_code,src.standard_version,src.ui_capabilities_json,'APPROVED',@newRev,src.checksum
FROM platform.contract_structure src WHERE src.revision=@oldRev AND NOT EXISTS(SELECT 1 FROM platform.contract_structure x WHERE x.structure_code=src.structure_code AND x.revision=@newRev);
INSERT @map(old_structure,new_structure)
SELECT oldx.structure_id,newx.structure_id FROM platform.contract_structure oldx JOIN platform.contract_structure newx ON newx.structure_code=oldx.structure_code AND newx.revision=@newRev WHERE oldx.revision=@oldRev;
INSERT platform.contract_table_definition(structure_id,logical_table_code,physical_table_name,access_table_name,table_role,storage_plane,primary_key_expression,load_order,required,lifecycle_status,revision)
SELECT m.new_structure,td.logical_table_code,
CASE td.physical_table_name
 WHEN N'kids_goal' THEN N'__ent_kids_goal'
 WHEN N'kids_resource' THEN N'__ent_kids_resource'
 WHEN N'kids_resource_subcategory_assignment' THEN N'__rel_kids_resource_subcategory_assignment'
 WHEN N'kids_glossary_entry' THEN N'__ent_kids_glossary_entry'
 WHEN N'kids_statistical_carrier' THEN N'__raw_kids_statistical_carrier'
 WHEN N'kids_statistical_input' THEN N'__stat_kids_statistical_input'
 WHEN N'kids_statistical_semantic_binding' THEN N'__stat_kids_statistical_semantic_binding'
 ELSE td.physical_table_name END,
CASE td.access_table_name
 WHEN N'kids_goal' THEN N'__ent_kids_goal'
 WHEN N'kids_resource' THEN N'__ent_kids_resource'
 WHEN N'kids_resource_subcategory_assignment' THEN N'__rel_kids_resource_subcategory_assignment'
 WHEN N'kids_glossary_entry' THEN N'__ent_kids_glossary_entry'
 WHEN N'kids_statistical_carrier' THEN N'__raw_kids_statistical_carrier'
 WHEN N'kids_statistical_input' THEN N'__stat_kids_statistical_input'
 WHEN N'kids_statistical_semantic_binding' THEN N'__stat_kids_statistical_semantic_binding'
 ELSE td.access_table_name END,
td.table_role,td.storage_plane,td.primary_key_expression,td.load_order,td.required,'APPROVED',@newRev
FROM platform.contract_table_definition td JOIN @map m ON m.old_structure=td.structure_id
WHERE td.revision=@oldRev;
INSERT platform.contract_field_definition(table_definition_id,field_name,ordinal,logical_type,physical_type,semantic_role,required,nullable,default_expression,classifier_scheme_code,relation_target,ui_control_type,ui_visible,ui_editable,ui_filterable,ui_sortable,ui_groupable,validation_json,lifecycle_status)
SELECT n.table_definition_id,f.field_name,f.ordinal,f.logical_type,f.physical_type,f.semantic_role,f.required,f.nullable,f.default_expression,f.classifier_scheme_code,f.relation_target,f.ui_control_type,f.ui_visible,f.ui_editable,f.ui_filterable,f.ui_sortable,f.ui_groupable,f.validation_json,'APPROVED'
FROM platform.contract_field_definition f JOIN platform.contract_table_definition o ON o.table_definition_id=f.table_definition_id AND o.revision=@oldRev
JOIN @map m ON m.old_structure=o.structure_id JOIN platform.contract_table_definition n ON n.structure_id=m.new_structure AND n.logical_table_code=o.logical_table_code AND n.revision=@newRev;
INSERT platform.contract_index_definition(table_definition_id,index_code,index_kind,field_list,is_unique,required,lifecycle_status)
SELECT n.table_definition_id,i.index_code,i.index_kind,i.field_list,i.is_unique,i.required,'APPROVED'
FROM platform.contract_index_definition i JOIN platform.contract_table_definition o ON o.table_definition_id=i.table_definition_id AND o.revision=@oldRev
JOIN @map m ON m.old_structure=o.structure_id JOIN platform.contract_table_definition n ON n.structure_id=m.new_structure AND n.logical_table_code=o.logical_table_code AND n.revision=@newRev;
INSERT platform.contract_structure_relation(structure_id,relation_code,from_table_code,from_field_name,to_table_code,to_field_name,relation_kind,cardinality,required,enforcement_policy,load_order,lifecycle_status)
SELECT m.new_structure,r.relation_code,r.from_table_code,r.from_field_name,r.to_table_code,r.to_field_name,r.relation_kind,r.cardinality,r.required,r.enforcement_policy,r.load_order,'APPROVED'
FROM platform.contract_structure_relation r JOIN @map m ON m.old_structure=r.structure_id WHERE r.lifecycle_status IN('APPROVED','PROVISIONAL_APPROVED');
