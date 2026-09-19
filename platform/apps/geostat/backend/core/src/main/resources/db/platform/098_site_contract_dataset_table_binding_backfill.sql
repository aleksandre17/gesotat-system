/* Resolve site dataset identity to a unique versioned physical table definition.
   Physical names come from the approved structural contract, never from a
   caller or the legacy logical access_table_name value. */
SET XACT_ABORT ON;
BEGIN TRANSACTION;

;WITH candidates AS (
  SELECT d.contract_dataset_id,t.table_definition_id,
         COUNT_BIG(*) OVER(PARTITION BY d.contract_dataset_id) AS match_count
  FROM platform.site_contract_dataset d
  JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id
  JOIN platform.contract_table_definition t ON t.logical_table_code=d.dataset_code
       AND t.revision=r.revision AND t.lifecycle_status='APPROVED'
  JOIN platform.contract_structure s ON s.structure_id=t.structure_id
       AND s.structure_code=d.dataset_code AND s.revision=t.revision
       AND s.lifecycle_status='APPROVED'
)
UPDATE d SET contract_table_definition_id=c.table_definition_id
FROM platform.site_contract_dataset d JOIN candidates c ON c.contract_dataset_id=d.contract_dataset_id
WHERE d.contract_table_definition_id IS NULL AND c.match_count=1;

IF EXISTS (
  SELECT 1 FROM platform.site_contract_dataset d
  JOIN platform.site_contract_revision r ON r.site_contract_revision_id=d.site_contract_revision_id
  JOIN platform.contract_table_definition t ON t.logical_table_code=d.dataset_code
       AND t.revision=r.revision AND t.lifecycle_status='APPROVED'
  JOIN platform.contract_structure s ON s.structure_id=t.structure_id
       AND s.structure_code=d.dataset_code AND s.revision=t.revision
       AND s.lifecycle_status='APPROVED'
  WHERE d.contract_table_definition_id IS NULL
  GROUP BY d.contract_dataset_id HAVING COUNT_BIG(*)>1
)
  THROW 51098, 'A site-contract dataset maps to multiple approved physical table definitions', 1;

COMMIT TRANSACTION;
