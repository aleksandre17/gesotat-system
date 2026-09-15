/* Activate final KIDS r8 without deleting immutable historical revisions. */
DECLARE @r8 BIGINT=(SELECT site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=8);
UPDATE platform.site_contract_revision SET status='SUPERSEDED' WHERE contract_code=N'KIDS_PORTAL_V1' AND revision IN(6,7) AND status<>'SUPERSEDED';
UPDATE platform.ingestion_contract SET contract_revision=8,status='REVIEW_REQUIRED',auto_publish=0 WHERE contract_code=N'KIDS_PORTAL_V1';
UPDATE platform.ingestion_contract_revision SET lifecycle_status='SUPERSEDED' WHERE contract_id=(SELECT contract_id FROM platform.ingestion_contract WHERE contract_code=N'KIDS_PORTAL_V1') AND revision IN(6,7) AND lifecycle_status<>'SUPERSEDED';
