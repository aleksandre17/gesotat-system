INSERT serving.metric_cache(snapshot_id,metric_id,dimension_signature,period_start,period_end,aggregate_value)
SELECT p.snapshot_id,s.metric_id,CONVERT(CHAR(64),HASHBYTES('SHA2_256',CONCAT(p.snapshot_id,'|',s.metric_id,'|',o.period_start)),2),o.period_start,o.period_start,SUM(o.numeric_value)
FROM publication.snapshot p JOIN publication.snapshot_member sm ON sm.snapshot_id=p.snapshot_id JOIN [statistics].series s ON s.dataset_snapshot_id=sm.dataset_snapshot_id JOIN [statistics].observation o ON o.series_id=s.series_id
WHERE p.status='PUBLISHED' AND NOT EXISTS(SELECT 1 FROM serving.metric_cache c WHERE c.snapshot_id=p.snapshot_id AND c.metric_id=s.metric_id AND c.period_start=o.period_start)
GROUP BY p.snapshot_id,s.metric_id,o.period_start;
