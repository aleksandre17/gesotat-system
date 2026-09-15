/* R8: remove executor assumptions about site/resource identifiers.  The
   statistical projection resolves its metric and classifier namespace from
   the issued mapping document. */
UPDATE platform.contract_revision_source
SET mapping_spec_json = JSON_MODIFY(
    JSON_MODIFY(mapping_spec_json, '$.metricLookup', JSON_QUERY(N'{"metricCodePattern":"KIDS_FILE_{resourceId}"}')),
    '$.ageClassificationSystem', N'KIDS_R7_AGE_GROUP')
WHERE source_locator IN (N'__stat_kids_statistical_input', N'ACCESS.__stat_kids_statistical_input')
  AND mapping_spec_json IS NOT NULL
  AND JSON_VALUE(mapping_spec_json,'$.projectionModel') = N'SDMX_COMPATIBLE_LONG';
