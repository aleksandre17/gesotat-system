/* Run in geostat-data. Shared multilingual content and locators; never site-specific tables. */
IF OBJECT_ID(N'entity.localized_text',N'U') IS NULL
CREATE TABLE entity.localized_text (
  entity_id BIGINT NOT NULL,
  field_code VARCHAR(64) NOT NULL,
  language_tag VARCHAR(35) NOT NULL,
  text_value NVARCHAR(MAX) NOT NULL,
  source_record_id BIGINT NOT NULL,
  CONSTRAINT pk_entity_localized_text PRIMARY KEY(entity_id,field_code,language_tag),
  CONSTRAINT fk_entity_localized_text_entity FOREIGN KEY(entity_id) REFERENCES entity.entity_record(entity_id),
  CONSTRAINT fk_entity_localized_text_source FOREIGN KEY(source_record_id) REFERENCES raw.source_record(source_record_id)
);

IF OBJECT_ID(N'entity.resource_locator',N'U') IS NULL
CREATE TABLE entity.resource_locator (
  entity_id BIGINT NOT NULL,
  locator_kind VARCHAR(32) NOT NULL,
  language_tag VARCHAR(35) NOT NULL DEFAULT 'und',
  locator NVARCHAR(2048) NOT NULL,
  source_record_id BIGINT NOT NULL,
  CONSTRAINT pk_entity_resource_locator PRIMARY KEY(entity_id,locator_kind,language_tag),
  CONSTRAINT fk_entity_resource_locator_entity FOREIGN KEY(entity_id) REFERENCES entity.entity_record(entity_id),
  CONSTRAINT fk_entity_resource_locator_source FOREIGN KEY(source_record_id) REFERENCES raw.source_record(source_record_id)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'entity.localized_text') AND name=N'ix_entity_localized_text_source')
  CREATE INDEX ix_entity_localized_text_source ON entity.localized_text(source_record_id);
IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE object_id=OBJECT_ID(N'entity.resource_locator') AND name=N'ix_entity_resource_locator_source')
  CREATE INDEX ix_entity_resource_locator_source ON entity.resource_locator(source_record_id);

/* An immutable publication cannot gain, lose or mutate translated content or locators. */
IF OBJECT_ID(N'entity.tr_localized_text_published_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER entity.tr_localized_text_published_immutable ON entity.localized_text AFTER INSERT, UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (SELECT 1 FROM inserted i JOIN entity.entity_record e ON e.entity_id=i.entity_id JOIN publication.dataset_snapshot s ON s.dataset_snapshot_id=e.dataset_snapshot_id WHERE s.status=''PUBLISHED'')
     OR EXISTS (SELECT 1 FROM deleted d JOIN entity.entity_record e ON e.entity_id=d.entity_id JOIN publication.dataset_snapshot s ON s.dataset_snapshot_id=e.dataset_snapshot_id WHERE s.status=''PUBLISHED'')
    THROW 51005, ''Published localized entity text is immutable.'', 1;
END');
IF OBJECT_ID(N'entity.tr_resource_locator_published_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER entity.tr_resource_locator_published_immutable ON entity.resource_locator AFTER INSERT, UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (SELECT 1 FROM inserted i JOIN entity.entity_record e ON e.entity_id=i.entity_id JOIN publication.dataset_snapshot s ON s.dataset_snapshot_id=e.dataset_snapshot_id WHERE s.status=''PUBLISHED'')
     OR EXISTS (SELECT 1 FROM deleted d JOIN entity.entity_record e ON e.entity_id=d.entity_id JOIN publication.dataset_snapshot s ON s.dataset_snapshot_id=e.dataset_snapshot_id WHERE s.status=''PUBLISHED'')
    THROW 51006, ''Published entity resource locators are immutable.'', 1;
END');
