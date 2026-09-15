/* Run in geostat-system. Approved external indicator codes resolve to governed metrics. */
IF OBJECT_ID(N'platform.metric_alias', N'U') IS NULL
CREATE TABLE platform.metric_alias (
  metric_alias_id BIGINT IDENTITY PRIMARY KEY,
  metric_id BIGINT NOT NULL,
  external_system_code NVARCHAR(120) NOT NULL,
  external_code NVARCHAR(255) NOT NULL,
  valid_from DATE NULL,
  valid_to DATE NULL,
  CONSTRAINT uq_platform_metric_alias UNIQUE(external_system_code,external_code),
  CONSTRAINT fk_platform_metric_alias_metric FOREIGN KEY(metric_id) REFERENCES platform.metric(metric_id)
);
