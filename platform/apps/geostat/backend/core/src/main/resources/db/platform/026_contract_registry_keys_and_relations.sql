/* Finalize physical key/relation materialization metadata for registry-driven Access generation. */
UPDATE td SET primary_key_expression=CASE td.physical_table_name
  WHEN N'__gs_package' THEN N'package_code'
  WHEN N'__gs_dataset' THEN N'dataset_code'
  WHEN N'__gs_field' THEN N'dataset_code,field_name'
  WHEN N'__gs_key' THEN N'dataset_code,key_role,key_order'
  WHEN N'__gs_relation' THEN N'relationship_code'
  WHEN N'__gs_projection' THEN N'projection_code'
  WHEN N'__cl_scheme' THEN N'scheme_code'
  WHEN N'__cl_version' THEN N'version_ref'
  WHEN N'__cl_item' THEN N'item_ref'
  WHEN N'__cl_alias' THEN N'alias_ref'
  WHEN N'__cl_hierarchy' THEN N'version_ref,child_item_ref'
  WHEN N'__stat_unit' THEN N'unit_code'
  WHEN N'__stat_metric' THEN N'metric_code'
  WHEN N'__raw_document' THEN N'source_row_key'
  ELSE td.primary_key_expression END
FROM platform.contract_table_definition td
WHERE td.revision=1;

/* Site contract relations are copied into the generic registry; the registry is
   the sole source used by the Access materializer. */
DECLARE @container BIGINT=(SELECT TOP 1 structure_id FROM platform.contract_structure WHERE structure_code=N'GS_PACKAGE' ORDER BY revision DESC);
INSERT platform.contract_structure_relation(structure_id,relation_code,from_table_code,from_field_name,to_table_code,to_field_name,relation_kind,cardinality,required,enforcement_policy,load_order,lifecycle_status)
SELECT @container,r.relation_code,r.from_dataset_code,r.from_field_name,r.to_dataset_code,r.to_field_name,r.relation_kind,r.cardinality,r.required,r.enforcement_policy,r.load_priority,'APPROVED'
FROM platform.site_contract_relation r JOIN platform.site_contract_revision scr ON scr.site_contract_revision_id=r.site_contract_revision_id AND scr.revision=7
WHERE @container IS NOT NULL AND NOT EXISTS(SELECT 1 FROM platform.contract_structure_relation x WHERE x.structure_id=@container AND x.relation_code=r.relation_code);
