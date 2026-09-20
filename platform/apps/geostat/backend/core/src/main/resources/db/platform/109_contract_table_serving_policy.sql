/* Run in the Control Plane database.
   Page 11 declared two includes whose targets are ACCESS-plane tables; the physical query service refuses every
   non-DATA table, so the page answered 400 (checklist 17.20, AIR-2026-045). Re-labelling those tables as DATA
   would publish raw source documents to every page consumer, so the contract gains an explicit serving policy
   instead: a table is servable only when it says so, and a table may demand an authority of its reader.

   Deny by default: every existing row is NOT servable, so this migration alone changes no answer. The two rows
   below are the recorded decision — the statistical carrier is lineage every reader of the page already sees in
   its own rows, while raw documents are served only to a caller holding the raw-read authority
   (data minimisation: GDPR Art. 25, OWASP API3).

   Additive and idempotent. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'platform.contract_table_definition') AND name=N'servable')
  ALTER TABLE platform.contract_table_definition ADD servable BIT NOT NULL CONSTRAINT df_contract_table_servable DEFAULT 0, serving_authority NVARCHAR(64) NULL;

IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'ck_contract_table_serving_authority')
  EXEC(N'ALTER TABLE platform.contract_table_definition ADD CONSTRAINT ck_contract_table_serving_authority CHECK(
       serving_authority IS NULL
    OR (servable = 1 AND serving_authority NOT LIKE N''%[^A-Z_]%'' AND LEN(serving_authority) BETWEEN 3 AND 64))');

/* The approved decision for the two include targets of page 11; no other row is touched.
   Deferred through EXEC: the statements name a column this same batch adds. */
EXEC(N'UPDATE platform.contract_table_definition
SET servable = 1, serving_authority = NULL
WHERE logical_table_code = N''KIDS_STATISTICAL_CARRIER'' AND storage_plane = N''ACCESS'' AND servable = 0;');

EXEC(N'UPDATE platform.contract_table_definition
SET servable = 1, serving_authority = N''RAW_READ''
WHERE logical_table_code = N''KIDS_RAW_DOCUMENT'' AND storage_plane = N''ACCESS'' AND servable = 0;');
