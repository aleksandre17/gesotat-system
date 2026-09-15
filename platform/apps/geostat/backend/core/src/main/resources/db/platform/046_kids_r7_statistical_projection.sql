UPDATE platform.contract_revision_source
SET mapping_spec_json=N'{"approvalState":"READY","family":"STATISTICAL","projectionModel":"SDMX_COMPATIBLE_LONG","carrierField":"carrier_code","periodField":"period_normalized","ageField":"age_group_item_ref","valueField":"value_decimal","ageDimensionId":1,"dataflowCode":"KIDS_FILES_STATISTICS","dsdCode":"KIDS_FILES_STATISTICS_DSD","qualityPolicy":"KIDS_AGGREGATE_QUALITY_V1","confidentialityPolicy":"KIDS_PUBLIC_AGGREGATE_V1"}'
WHERE source_locator=N'__stat_kids_statistical_input';
