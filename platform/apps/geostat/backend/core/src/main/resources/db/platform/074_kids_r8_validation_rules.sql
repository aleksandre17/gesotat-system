/* Typed-value rule used by the quarantine acceptance path.  It is scoped to
   the KIDS statistical input dataset and is idempotent. */
INSERT platform.validation_rule(dataset_version_id,rule_code,rule_type,severity,expression_json,active)
SELECT v.dataset_version_id,N'KIDS_STAT_VALUE_REQUIRED',N'REQUIRED_PATH',N'ERROR',N'{"path":"value_decimal"}',1
FROM platform.dataset_version v
JOIN platform.dataset d ON d.dataset_id=v.dataset_id
WHERE d.dataset_code=N'KIDS_STATISTICAL_INPUT' AND d.product_id=(SELECT product_id FROM platform.data_product WHERE product_code=N'KIDS_PORTAL')
  AND NOT EXISTS (SELECT 1 FROM platform.validation_rule r WHERE r.dataset_version_id=v.dataset_version_id AND r.rule_code=N'KIDS_STAT_VALUE_REQUIRED');
