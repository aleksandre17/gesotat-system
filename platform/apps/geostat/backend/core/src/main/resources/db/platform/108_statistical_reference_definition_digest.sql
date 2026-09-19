/* Run in the Control Plane database.
   Common statistical contract (decision register Q09): an idempotent proposal needs to tell "the same definition
   proposed again" from "the same identity with another definition". The digest of the proposed definition is
   stored beside the identity; it is written once and never changes. Additive and idempotent. */
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;

IF NOT EXISTS(SELECT 1 FROM sys.columns WHERE object_id=OBJECT_ID(N'platform.statistical_reference') AND name=N'definition_digest')
  ALTER TABLE platform.statistical_reference ADD definition_digest CHAR(64) NULL;

IF OBJECT_ID(N'platform.tr_statistical_reference_definition_immutable',N'TR') IS NULL EXEC(N'
CREATE TRIGGER platform.tr_statistical_reference_definition_immutable ON platform.statistical_reference AFTER UPDATE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS(SELECT 1 FROM deleted d JOIN inserted i ON i.reference_id=d.reference_id
            WHERE d.definition_digest IS NOT NULL AND ISNULL(i.definition_digest,'''')<>d.definition_digest)
    THROW 51204, ''The definition of a statistical reference is written once.'', 1;
END');
