/* Run in geostat-system (Control Plane). AIR-2026-016.
   Reconciles the KIDS_PORTAL_V1 revision 8 ingestion source registry with its
   approved site contract, the version authority for every dataset:
   - 067 bound the package tables (ACCESS.__*) to whatever dataset version was
     newest at the time (inflated by AIR-2026-015), not the approved one;
   - 068 added locators from the site contract's legacy access_table_name
     (ACCESS.kids_*), which do not exist in the revision 8 Access package.
   Result: each dataset keeps exactly one source, its package table, bound to the
   approved dataset version. Fail-closed if that cannot be established. */
SET XACT_ABORT ON;
BEGIN TRANSACTION;

DECLARE @contract BIGINT=(SELECT contract_id FROM platform.ingestion_contract WHERE contract_code=N'KIDS_PORTAL_V1');
DECLARE @rev BIGINT=(SELECT TOP 1 ingestion_contract_revision_id FROM platform.ingestion_contract_revision WHERE contract_id=@contract AND revision=8 ORDER BY ingestion_contract_revision_id DESC);
DECLARE @site BIGINT=(SELECT TOP 1 site_contract_revision_id FROM platform.site_contract_revision WHERE contract_code=N'KIDS_PORTAL_V1' AND revision=8 AND status='APPROVED' ORDER BY site_contract_revision_id DESC);

IF @rev IS NOT NULL AND @site IS NOT NULL
BEGIN
  DECLARE @authority TABLE(dataset_id BIGINT PRIMARY KEY, dataset_version_id BIGINT NOT NULL);
  INSERT @authority(dataset_id,dataset_version_id)
  SELECT dv.dataset_id, sc.dataset_version_id
  FROM platform.site_contract_dataset sc JOIN platform.dataset_version dv ON dv.dataset_version_id=sc.dataset_version_id
  WHERE sc.site_contract_revision_id=@site;

  /* 1. Package-table sources point at the approved version. */
  UPDATE rs SET target_dataset_version_id=a.dataset_version_id
  FROM platform.contract_revision_source rs
  JOIN platform.dataset_version dv ON dv.dataset_version_id=rs.target_dataset_version_id
  JOIN @authority a ON a.dataset_id=dv.dataset_id
  WHERE rs.ingestion_contract_revision_id=@rev AND rs.source_locator LIKE N'ACCESS.[_][_]%' AND rs.target_dataset_version_id<>a.dataset_version_id;

  /* 2. Legacy locators are removed where the dataset has a package-table source. */
  DELETE legacy FROM platform.contract_revision_source legacy
  JOIN platform.dataset_version dv ON dv.dataset_version_id=legacy.target_dataset_version_id
  WHERE legacy.ingestion_contract_revision_id=@rev AND legacy.source_locator NOT LIKE N'ACCESS.[_][_]%'
    AND EXISTS (SELECT 1 FROM platform.contract_revision_source p
                JOIN platform.dataset_version pv ON pv.dataset_version_id=p.target_dataset_version_id
                WHERE p.ingestion_contract_revision_id=@rev AND p.source_locator LIKE N'ACCESS.[_][_]%' AND pv.dataset_id=dv.dataset_id);

  /* 3. The contract-level registry mirrors the same decision (legacy rows deactivated, not deleted). */
  UPDATE cs SET target_dataset_version_id=a.dataset_version_id
  FROM platform.contract_source cs
  JOIN platform.dataset_version dv ON dv.dataset_version_id=cs.target_dataset_version_id
  JOIN @authority a ON a.dataset_id=dv.dataset_id
  WHERE cs.contract_id=@contract AND cs.active=1 AND cs.source_locator LIKE N'ACCESS.[_][_]%' AND cs.target_dataset_version_id<>a.dataset_version_id;

  UPDATE legacy SET active=0
  FROM platform.contract_source legacy
  JOIN platform.dataset_version dv ON dv.dataset_version_id=legacy.target_dataset_version_id
  WHERE legacy.contract_id=@contract AND legacy.active=1 AND legacy.source_locator NOT LIKE N'ACCESS.[_][_]%'
    AND EXISTS (SELECT 1 FROM platform.contract_source p
                JOIN platform.dataset_version pv ON pv.dataset_version_id=p.target_dataset_version_id
                WHERE p.contract_id=@contract AND p.active=1 AND p.source_locator LIKE N'ACCESS.[_][_]%' AND pv.dataset_id=dv.dataset_id);

  /* Fail closed: every approved dataset has exactly one revision source, on its approved version. */
  IF EXISTS (SELECT 1 FROM @authority a
             WHERE (SELECT COUNT(*) FROM platform.contract_revision_source rs
                    JOIN platform.dataset_version dv ON dv.dataset_version_id=rs.target_dataset_version_id
                    WHERE rs.ingestion_contract_revision_id=@rev AND dv.dataset_id=a.dataset_id)<>1
                OR NOT EXISTS (SELECT 1 FROM platform.contract_revision_source rs
                               WHERE rs.ingestion_contract_revision_id=@rev AND rs.target_dataset_version_id=a.dataset_version_id))
    THROW 51030, 'KIDS R8 source registry could not be reconciled to one approved source per dataset', 1;
END;

COMMIT TRANSACTION;
