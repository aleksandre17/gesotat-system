/* Run in the Control Plane database.
   Common statistical contract, increment 2 (decision register Q07, Q29, Q30; plan §6).

   Problem: the semantic registry (platform.measure, platform.statistical_unit, platform.statistical_dsd,
   platform.classification_version, policies) identifies entries by a globally unique code without a version,
   a namespace or an owner scope, so a contract cannot pin "exactly this immutable revision".

   Decision: one additive identity layer. platform.statistical_reference holds ONLY the maintainable-artefact
   identity (kind, namespace, code, semantic version), its lifecycle and its visibility scope, and points at the
   existing row that keeps the meaning. It is not a second semantic registry: no unit, type, label or rule lives
   here. Concepts and profiles have no properties beyond identity and labels (labels live in the metadata plane),
   so they need no target row.

   Additive and idempotent. No existing column, constraint or row is changed or removed. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF OBJECT_ID(N'platform.statistical_reference',N'U') IS NULL
CREATE TABLE platform.statistical_reference(
  reference_id BIGINT IDENTITY PRIMARY KEY,
  kind VARCHAR(16) NOT NULL,
  namespace_id BIGINT NOT NULL,
  code NVARCHAR(120) NOT NULL,
  version_major INT NOT NULL,
  version_minor INT NOT NULL,
  version_patch INT NOT NULL,
  lifecycle_status VARCHAR(16) NOT NULL CONSTRAINT df_statistical_reference_lifecycle DEFAULT 'PROPOSED',
  /* NULL = visible to every product (GLOBAL); otherwise visible to the owning product only. */
  owner_product_id BIGINT NULL,
  /* The existing row that holds the meaning; the pair is NULL for identity-only kinds. */
  target_type VARCHAR(48) NULL,
  target_id BIGINT NULL,
  created_at DATETIME2 NOT NULL CONSTRAINT df_statistical_reference_created DEFAULT SYSUTCDATETIME(),
  decided_at DATETIME2 NULL,
  decided_by NVARCHAR(255) NULL,
  CONSTRAINT uq_statistical_reference UNIQUE(kind,namespace_id,code,version_major,version_minor,version_patch),
  CONSTRAINT fk_statistical_reference_namespace FOREIGN KEY(namespace_id) REFERENCES platform.contract_namespace(namespace_id),
  CONSTRAINT fk_statistical_reference_owner FOREIGN KEY(owner_product_id) REFERENCES platform.data_product(product_id),
  CONSTRAINT ck_statistical_reference_kind CHECK(kind IN('DSD','CONCEPT','MEASURE','UNIT','CODELIST','POLICY','PROFILE')),
  CONSTRAINT ck_statistical_reference_lifecycle CHECK(lifecycle_status IN('PROPOSED','APPROVED','SUPERSEDED')),
  CONSTRAINT ck_statistical_reference_version CHECK(version_major>=0 AND version_minor>=0 AND version_patch>=0),
  CONSTRAINT ck_statistical_reference_code CHECK(code LIKE N'[A-Za-z]%' AND code NOT LIKE N'%[^A-Za-z0-9_]%'),
  CONSTRAINT ck_statistical_reference_target CHECK(
       (kind IN('CONCEPT','PROFILE') AND target_type IS NULL AND target_id IS NULL)
    OR (kind='DSD'      AND target_type='STATISTICAL_DSD'        AND target_id IS NOT NULL)
    OR (kind='MEASURE'  AND target_type='MEASURE'                AND target_id IS NOT NULL)
    OR (kind='UNIT'     AND target_type='STATISTICAL_UNIT'       AND target_id IS NOT NULL)
    OR (kind='CODELIST' AND target_type='CLASSIFICATION_VERSION' AND target_id IS NOT NULL)
    OR (kind='POLICY'   AND target_type IN('DATA_QUALITY_POLICY','CONFIDENTIALITY_POLICY','VALIDATION_RULE') AND target_id IS NOT NULL)),
  CONSTRAINT ck_statistical_reference_decision CHECK(
       (lifecycle_status='PROPOSED' AND decided_at IS NULL)
    OR (lifecycle_status<>'PROPOSED' AND decided_at IS NOT NULL AND decided_by IS NOT NULL))
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'platform.statistical_reference') AND name=N'ix_statistical_reference_target')
  CREATE INDEX ix_statistical_reference_target ON platform.statistical_reference(target_type,target_id);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'platform.statistical_reference') AND name=N'ix_statistical_reference_owner')
  CREATE INDEX ix_statistical_reference_owner ON platform.statistical_reference(owner_product_id,kind,lifecycle_status);

/* A decided version is immutable: its identity, scope and target never change again. The only legal step after
   approval is APPROVED -> SUPERSEDED. A correction is a new version. Rows are never deleted. */
IF OBJECT_ID(N'platform.tr_statistical_reference_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER platform.tr_statistical_reference_immutable ON platform.statistical_reference AFTER UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS(SELECT 1 FROM deleted d WHERE NOT EXISTS(SELECT 1 FROM inserted i WHERE i.reference_id=d.reference_id))
    THROW 51201, ''Statistical references are never deleted; supersede the version instead.'', 1;
  IF EXISTS(SELECT 1 FROM deleted d JOIN inserted i ON i.reference_id=d.reference_id
            WHERE d.kind<>i.kind OR d.namespace_id<>i.namespace_id OR d.code<>i.code
               OR d.version_major<>i.version_major OR d.version_minor<>i.version_minor OR d.version_patch<>i.version_patch)
    THROW 51202, ''The identity of a statistical reference is immutable.'', 1;
  IF EXISTS(SELECT 1 FROM deleted d JOIN inserted i ON i.reference_id=d.reference_id
            WHERE d.lifecycle_status<>''PROPOSED''
              AND (ISNULL(d.owner_product_id,-1)<>ISNULL(i.owner_product_id,-1)
                OR ISNULL(d.target_type,'''')<>ISNULL(i.target_type,'''') OR ISNULL(d.target_id,-1)<>ISNULL(i.target_id,-1)
                OR NOT (d.lifecycle_status=i.lifecycle_status OR (d.lifecycle_status=''APPROVED'' AND i.lifecycle_status=''SUPERSEDED''))))
    THROW 51203, ''A decided statistical reference is immutable; only APPROVED -> SUPERSEDED is allowed.'', 1;
END');

/* Measure: the single semantic authority gains what a contract must resolve from it (register Q23, Q32).
   All columns are NULL-able: existing measures stay valid and are simply not referenceable until completed. */
IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'platform.measure') AND name=N'unit_id')
  ALTER TABLE platform.measure ADD unit_id BIGINT NULL, concept_reference_id BIGINT NULL,
    numeric_precision INT NULL, numeric_scale INT NULL,
    approximate_numeric BIT NOT NULL CONSTRAINT df_measure_approximate DEFAULT 0,
    rounding_mode VARCHAR(16) NULL;
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'fk_measure_unit')
  EXEC(N'ALTER TABLE platform.measure ADD CONSTRAINT fk_measure_unit FOREIGN KEY(unit_id) REFERENCES platform.statistical_unit(unit_id)');
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'fk_measure_concept_reference')
  EXEC(N'ALTER TABLE platform.measure ADD CONSTRAINT fk_measure_concept_reference FOREIGN KEY(concept_reference_id) REFERENCES platform.statistical_reference(reference_id)');
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'ck_measure_numeric_envelope')
  EXEC(N'ALTER TABLE platform.measure ADD CONSTRAINT ck_measure_numeric_envelope CHECK(
       (numeric_precision IS NULL AND numeric_scale IS NULL)
    OR (numeric_precision BETWEEN 1 AND 28 AND numeric_scale BETWEEN 0 AND 10 AND numeric_scale<=numeric_precision))');
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'ck_measure_rounding_mode')
  EXEC(N'ALTER TABLE platform.measure ADD CONSTRAINT ck_measure_rounding_mode CHECK(rounding_mode IS NULL OR rounding_mode IN(''HALF_EVEN'',''HALF_UP'',''DOWN'',''UNNECESSARY''))');

/* Component: a concept for every role (attributes had none) and the facets of an uncoded representation.
   dimension_id / measure_id / classification_version_id / attachment_level / constraint_json already exist. */
IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'platform.statistical_component') AND name=N'concept_reference_id')
  ALTER TABLE platform.statistical_component ADD concept_reference_id BIGINT NULL, representation_type VARCHAR(24) NULL;
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'fk_stat_component_concept_reference')
  EXEC(N'ALTER TABLE platform.statistical_component ADD CONSTRAINT fk_stat_component_concept_reference FOREIGN KEY(concept_reference_id) REFERENCES platform.statistical_reference(reference_id)');
IF NOT EXISTS(SELECT 1 FROM sys.check_constraints WHERE name=N'ck_stat_component_representation_type')
  EXEC(N'ALTER TABLE platform.statistical_component ADD CONSTRAINT ck_stat_component_representation_type CHECK(representation_type IS NULL OR representation_type IN(''CODED'',''TIME_PERIOD'',''INTEGER'',''TEXT''))');

/* Unit conversion graph (plan §6): optional, exact, acyclicity is enforced by the registry service. */
IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'platform.statistical_unit') AND name=N'base_unit_id')
  ALTER TABLE platform.statistical_unit ADD base_unit_id BIGINT NULL;
IF NOT EXISTS(SELECT 1 FROM sys.foreign_keys WHERE name=N'fk_statistical_unit_base')
  EXEC(N'ALTER TABLE platform.statistical_unit ADD CONSTRAINT fk_statistical_unit_base FOREIGN KEY(base_unit_id) REFERENCES platform.statistical_unit(unit_id)');
