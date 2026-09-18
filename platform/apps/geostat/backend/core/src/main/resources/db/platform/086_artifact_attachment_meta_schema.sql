/* Run in geostat-system (Control Plane).
   Generic artifact-attachment primitives (ARTIFACT-ATTACHMENT-CONTRACT.md §23).
   A policy governs how bytes may be accepted and distributed; a relation
   definition declares, per dataset version, how rows attach to artifacts.
   No site-specific table or branch: sites bind through seed rows only. */

IF OBJECT_ID(N'platform.artifact_policy',N'U') IS NULL
CREATE TABLE platform.artifact_policy(
  artifact_policy_id BIGINT IDENTITY PRIMARY KEY,
  policy_code NVARCHAR(120) NOT NULL,
  revision INT NOT NULL,
  access_mode VARCHAR(32) NOT NULL,
  required_authority NVARCHAR(64) NULL,
  allowed_media_types_json NVARCHAR(MAX) NOT NULL,
  max_bytes BIGINT NOT NULL,
  signed_url_ttl_seconds INT NOT NULL,
  retention_class VARCHAR(32) NOT NULL,
  checksum_algorithm VARCHAR(16) NOT NULL DEFAULT 'SHA-256',
  lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_artifact_policy UNIQUE(policy_code,revision),
  CONSTRAINT ck_artifact_policy_access CHECK(access_mode IN('PUBLIC_WHEN_PUBLISHED','AUTHENTICATED','RESTRICTED')),
  CONSTRAINT ck_artifact_policy_authority CHECK(access_mode<>'RESTRICTED' OR required_authority IS NOT NULL),
  CONSTRAINT ck_artifact_policy_media CHECK(ISJSON(allowed_media_types_json)=1),
  CONSTRAINT ck_artifact_policy_bytes CHECK(max_bytes>0),
  CONSTRAINT ck_artifact_policy_ttl CHECK(signed_url_ttl_seconds BETWEEN 30 AND 3600),
  CONSTRAINT ck_artifact_policy_retention CHECK(retention_class IN('RETAIN_INDEFINITE','RETAIN_WHILE_REFERENCED','ARCHIVE_1Y','ARCHIVE_7Y')),
  CONSTRAINT ck_artifact_policy_checksum CHECK(checksum_algorithm='SHA-256'),
  CONSTRAINT ck_artifact_policy_lifecycle CHECK(lifecycle_status IN('DRAFT','APPROVED','RETIRED'))
);

IF OBJECT_ID(N'platform.artifact_relation_definition',N'U') IS NULL
CREATE TABLE platform.artifact_relation_definition(
  artifact_relation_definition_id BIGINT IDENTITY PRIMARY KEY,
  dataset_version_id BIGINT NOT NULL,
  relation_code NVARCHAR(64) NOT NULL,
  artifact_role VARCHAR(24) NOT NULL,
  artifact_policy_id BIGINT NOT NULL,
  min_per_row INT NOT NULL DEFAULT 0,
  max_per_row INT NULL,
  ordered BIT NOT NULL DEFAULT 0,
  match_rule_json NVARCHAR(MAX) NOT NULL,
  lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
  CONSTRAINT uq_artifact_relation_definition UNIQUE(dataset_version_id,relation_code),
  CONSTRAINT fk_artifact_relation_version FOREIGN KEY(dataset_version_id) REFERENCES platform.dataset_version(dataset_version_id),
  CONSTRAINT fk_artifact_relation_policy FOREIGN KEY(artifact_policy_id) REFERENCES platform.artifact_policy(artifact_policy_id),
  CONSTRAINT ck_artifact_relation_code CHECK(relation_code NOT LIKE N'%[^A-Z0-9_]%' AND LEN(relation_code)>0),
  CONSTRAINT ck_artifact_relation_role CHECK(artifact_role IN('PRIMARY','SUPPORTING','DERIVED','PREVIEW')),
  CONSTRAINT ck_artifact_relation_cardinality CHECK(min_per_row>=0 AND (max_per_row IS NULL OR max_per_row>=min_per_row AND max_per_row>=1)),
  CONSTRAINT ck_artifact_relation_rule CHECK(ISJSON(match_rule_json)=1),
  CONSTRAINT ck_artifact_relation_lifecycle CHECK(lifecycle_status IN('DRAFT','APPROVED','RETIRED'))
);

/* An approved policy or relation definition is immutable: change requires a new revision. */
IF OBJECT_ID(N'platform.tr_artifact_policy_approved_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER platform.tr_artifact_policy_approved_immutable ON platform.artifact_policy AFTER UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (SELECT 1 FROM deleted d LEFT JOIN inserted i ON i.artifact_policy_id=d.artifact_policy_id
             WHERE d.lifecycle_status=''APPROVED''
               AND (i.artifact_policy_id IS NULL OR i.access_mode<>d.access_mode OR ISNULL(i.required_authority,N'''')<>ISNULL(d.required_authority,N'''')
                    OR i.allowed_media_types_json<>d.allowed_media_types_json OR i.max_bytes<>d.max_bytes
                    OR i.signed_url_ttl_seconds<>d.signed_url_ttl_seconds OR i.retention_class<>d.retention_class))
    THROW 51020, ''Approved artifact policy is immutable; create a new revision.'', 1;
END');

IF OBJECT_ID(N'platform.tr_artifact_relation_approved_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER platform.tr_artifact_relation_approved_immutable ON platform.artifact_relation_definition AFTER UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (SELECT 1 FROM deleted d LEFT JOIN inserted i ON i.artifact_relation_definition_id=d.artifact_relation_definition_id
             WHERE d.lifecycle_status=''APPROVED''
               AND (i.artifact_relation_definition_id IS NULL OR i.artifact_role<>d.artifact_role OR i.artifact_policy_id<>d.artifact_policy_id
                    OR i.min_per_row<>d.min_per_row OR ISNULL(i.max_per_row,-1)<>ISNULL(d.max_per_row,-1)
                    OR i.ordered<>d.ordered OR i.match_rule_json<>d.match_rule_json))
    THROW 51021, ''Approved artifact relation definition is immutable; create a new dataset version.'', 1;
END');
