/* Universal extensible metadata plane.  Metadata is governed contract data,
   not an unvalidated bag attached to a chart or page. */
IF OBJECT_ID(N'platform.metadata_namespace',N'U') IS NULL
CREATE TABLE platform.metadata_namespace(
 metadata_namespace_id BIGINT IDENTITY PRIMARY KEY,
 namespace_code NVARCHAR(120) NOT NULL UNIQUE,
 namespace_uri NVARCHAR(500) NULL,
 title_ka NVARCHAR(255) NOT NULL,
 title_en NVARCHAR(255) NULL,
 authority_mode VARCHAR(24) NOT NULL DEFAULT 'EXTENSION',
 status VARCHAR(24) NOT NULL DEFAULT 'ACTIVE'
);
IF OBJECT_ID(N'platform.metadata_schema',N'U') IS NULL
CREATE TABLE platform.metadata_schema(
 metadata_schema_id BIGINT IDENTITY PRIMARY KEY,
 metadata_namespace_id BIGINT NOT NULL,
 schema_code NVARCHAR(160) NOT NULL,
 revision INT NOT NULL,
 schema_json NVARCHAR(MAX) NOT NULL,
 lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
 definition_hash CHAR(64) NULL,
 created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
 CONSTRAINT uq_metadata_schema UNIQUE(metadata_namespace_id,schema_code,revision),
 CONSTRAINT fk_metadata_schema_namespace FOREIGN KEY(metadata_namespace_id) REFERENCES platform.metadata_namespace(metadata_namespace_id),
 CONSTRAINT ck_metadata_schema_json CHECK(ISJSON(schema_json)=1)
);
IF OBJECT_ID(N'platform.metadata_subject',N'U') IS NULL
CREATE TABLE platform.metadata_subject(
 metadata_subject_id BIGINT IDENTITY PRIMARY KEY,
 subject_type VARCHAR(48) NOT NULL,
 subject_code NVARCHAR(255) NOT NULL,
 subject_revision INT NOT NULL DEFAULT 1,
 contract_revision_id BIGINT NULL,
 lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
 visibility VARCHAR(24) NOT NULL DEFAULT 'INTERNAL',
 owner_code NVARCHAR(160) NULL,
 valid_from DATETIME2 NULL,
 valid_to DATETIME2 NULL,
 created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
 CONSTRAINT uq_metadata_subject UNIQUE(subject_type,subject_code,subject_revision),
 CONSTRAINT fk_metadata_subject_contract FOREIGN KEY(contract_revision_id) REFERENCES platform.site_contract_revision(site_contract_revision_id)
);
IF OBJECT_ID(N'platform.metadata_assertion',N'U') IS NULL
CREATE TABLE platform.metadata_assertion(
 metadata_assertion_id BIGINT IDENTITY PRIMARY KEY,
 metadata_subject_id BIGINT NOT NULL,
 metadata_schema_id BIGINT NULL,
 namespace_code NVARCHAR(120) NOT NULL,
 property_code NVARCHAR(160) NOT NULL,
 language_tag NVARCHAR(32) NULL,
 value_type VARCHAR(24) NOT NULL,
 value_text NVARCHAR(MAX) NULL,
 value_number DECIMAL(38,12) NULL,
 value_boolean BIT NULL,
 value_json NVARCHAR(MAX) NULL,
 ordinal INT NOT NULL DEFAULT 1,
 lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
 source_reference NVARCHAR(500) NULL,
 definition_hash CHAR(64) NULL,
 created_at DATETIME2 NOT NULL DEFAULT SYSUTCDATETIME(),
 CONSTRAINT fk_metadata_assertion_subject FOREIGN KEY(metadata_subject_id) REFERENCES platform.metadata_subject(metadata_subject_id),
 CONSTRAINT fk_metadata_assertion_schema FOREIGN KEY(metadata_schema_id) REFERENCES platform.metadata_schema(metadata_schema_id),
 CONSTRAINT ck_metadata_assertion_type CHECK(value_type IN('TEXT','NUMBER','BOOLEAN','JSON','URI','DATE','DATETIME','CODE')),
 CONSTRAINT ck_metadata_assertion_json CHECK(value_json IS NULL OR ISJSON(value_json)=1),
 CONSTRAINT uq_metadata_assertion UNIQUE(metadata_subject_id,namespace_code,property_code,language_tag,ordinal)
);
IF OBJECT_ID(N'platform.metadata_relation',N'U') IS NULL
CREATE TABLE platform.metadata_relation(
 metadata_relation_id BIGINT IDENTITY PRIMARY KEY,
 from_subject_id BIGINT NOT NULL,
 relation_code NVARCHAR(160) NOT NULL,
 to_subject_id BIGINT NOT NULL,
 relation_role VARCHAR(24) NOT NULL DEFAULT 'RELATED',
 lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
 CONSTRAINT fk_metadata_relation_from FOREIGN KEY(from_subject_id) REFERENCES platform.metadata_subject(metadata_subject_id),
 CONSTRAINT fk_metadata_relation_to FOREIGN KEY(to_subject_id) REFERENCES platform.metadata_subject(metadata_subject_id),
 CONSTRAINT uq_metadata_relation UNIQUE(from_subject_id,relation_code,to_subject_id)
);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ix_metadata_assertion_lookup' AND object_id=OBJECT_ID(N'platform.metadata_assertion')) CREATE INDEX ix_metadata_assertion_lookup ON platform.metadata_assertion(metadata_subject_id,namespace_code,property_code,lifecycle_status);
IF NOT EXISTS(SELECT 1 FROM sys.indexes WHERE name=N'ix_metadata_subject_public' AND object_id=OBJECT_ID(N'platform.metadata_subject')) CREATE INDEX ix_metadata_subject_public ON platform.metadata_subject(subject_type,subject_code,lifecycle_status,visibility);
MERGE platform.metadata_namespace AS t USING (VALUES
 (N'CORE',N'https://geostat.example/metadata/core',N'ძირითადი metadata',N'Core metadata','AUTHORITATIVE'),
 (N'SEMANTIC',N'https://geostat.example/metadata/semantic',N'სემანტიკური metadata',N'Semantic metadata','AUTHORITATIVE'),
 (N'PRESENTATION',N'https://geostat.example/metadata/presentation',N'პრეზენტაციის metadata',N'Presentation metadata','EXTENSION'),
 (N'PROVENANCE',N'https://geostat.example/metadata/provenance',N'წარმოშობის metadata',N'Provenance metadata','AUTHORITATIVE'),
 (N'GOVERNANCE',N'https://geostat.example/metadata/governance',N'მართვის metadata',N'Governance metadata','SYSTEM'),
 (N'ACCESSIBILITY',N'https://geostat.example/metadata/accessibility',N'ხელმისაწვდომობის metadata',N'Accessibility metadata','EXTENSION'),
 (N'DISTRIBUTION',N'https://geostat.example/metadata/distribution',N'გავრცელების metadata',N'Distribution metadata','EXTENSION')
)s(code,uri,ka,en,mode) ON t.namespace_code=s.code
WHEN MATCHED THEN UPDATE SET namespace_uri=s.uri,title_ka=s.ka,title_en=s.en,authority_mode=s.mode
WHEN NOT MATCHED THEN INSERT(namespace_code,namespace_uri,title_ka,title_en,authority_mode) VALUES(s.code,s.uri,s.ka,s.en,s.mode);
MERGE platform.metadata_schema AS t USING (SELECT n.metadata_namespace_id,N'CORE_RESOURCE_V1' code,1 rev,N'{"type":"object","required":["title","description"],"properties":{"title":{"type":"localizedText"},"description":{"type":"localizedText"},"keywords":{"type":"array","items":{"type":"string"}}}}' body FROM platform.metadata_namespace n WHERE n.namespace_code=N'CORE')s ON t.metadata_namespace_id=s.metadata_namespace_id AND t.schema_code=s.code AND t.revision=s.rev
WHEN NOT MATCHED THEN INSERT(metadata_namespace_id,schema_code,revision,schema_json,lifecycle_status) VALUES(s.metadata_namespace_id,s.code,s.rev,s.body,'APPROVED');
MERGE platform.metadata_schema AS t USING (SELECT n.metadata_namespace_id,N'VISUALIZATION_V1' code,1 rev,N'{"type":"object","required":["title","description","datasetCode"],"properties":{"title":{"type":"localizedText"},"description":{"type":"localizedText"},"accessibilityText":{"type":"localizedText"},"datasetCode":{"type":"code"},"metricCode":{"type":"code"},"unit":{"type":"code"},"aggregation":{"type":"code"},"chartType":{"type":"code"}}}' body FROM platform.metadata_namespace n WHERE n.namespace_code=N'PRESENTATION')s ON t.metadata_namespace_id=s.metadata_namespace_id AND t.schema_code=s.code AND t.revision=s.rev
WHEN NOT MATCHED THEN INSERT(metadata_namespace_id,schema_code,revision,schema_json,lifecycle_status) VALUES(s.metadata_namespace_id,s.code,s.rev,s.body,'APPROVED');
