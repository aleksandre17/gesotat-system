/* Run in geostat-system (Control Plane).
   Strengthen approved artifact policy/relation identity immutability without rewriting
   migration 086, which has already been applied in shared development environments. */

EXEC(N'
CREATE OR ALTER TRIGGER platform.tr_artifact_policy_approved_immutable ON platform.artifact_policy AFTER UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (
    SELECT 1
    FROM deleted d
    LEFT JOIN inserted i ON i.artifact_policy_id=d.artifact_policy_id
    WHERE (i.artifact_policy_id IS NULL AND d.lifecycle_status<>''DRAFT'')
       OR (d.lifecycle_status=''APPROVED'' AND (
            i.policy_code<>d.policy_code OR i.revision<>d.revision OR i.access_mode<>d.access_mode
            OR ISNULL(i.required_authority,N'''')<>ISNULL(d.required_authority,N'''')
            OR CONVERT(VARBINARY(MAX),i.allowed_media_types_json)<>CONVERT(VARBINARY(MAX),d.allowed_media_types_json)
            OR i.max_bytes<>d.max_bytes OR i.signed_url_ttl_seconds<>d.signed_url_ttl_seconds
            OR i.retention_class<>d.retention_class OR i.checksum_algorithm<>d.checksum_algorithm
            OR i.created_at<>d.created_at
            OR (i.lifecycle_status<>d.lifecycle_status AND i.lifecycle_status<>''RETIRED'')
       ))
       OR (d.lifecycle_status=''RETIRED'' AND (
            i.policy_code<>d.policy_code OR i.revision<>d.revision OR i.access_mode<>d.access_mode
            OR ISNULL(i.required_authority,N'''')<>ISNULL(d.required_authority,N'''')
            OR CONVERT(VARBINARY(MAX),i.allowed_media_types_json)<>CONVERT(VARBINARY(MAX),d.allowed_media_types_json)
            OR i.max_bytes<>d.max_bytes OR i.signed_url_ttl_seconds<>d.signed_url_ttl_seconds
            OR i.retention_class<>d.retention_class OR i.checksum_algorithm<>d.checksum_algorithm
            OR i.created_at<>d.created_at OR i.lifecycle_status<>d.lifecycle_status
       ))
       OR (d.lifecycle_status=''DRAFT'' AND i.lifecycle_status NOT IN(''DRAFT'',''APPROVED'',''RETIRED''))
  )
    THROW 51020, ''Artifact policy identity and approved semantics are immutable; create a new revision.'', 1;
END');

EXEC(N'
CREATE OR ALTER TRIGGER platform.tr_artifact_relation_approved_immutable ON platform.artifact_relation_definition AFTER UPDATE, DELETE AS
BEGIN
  SET NOCOUNT ON;
  IF EXISTS (
    SELECT 1
    FROM deleted d
    LEFT JOIN inserted i ON i.artifact_relation_definition_id=d.artifact_relation_definition_id
    WHERE (i.artifact_relation_definition_id IS NULL AND d.lifecycle_status<>''DRAFT'')
       OR (d.lifecycle_status=''APPROVED'' AND (
            i.dataset_version_id<>d.dataset_version_id OR i.relation_code<>d.relation_code
            OR i.artifact_role<>d.artifact_role OR i.artifact_policy_id<>d.artifact_policy_id
            OR i.min_per_row<>d.min_per_row OR ISNULL(i.max_per_row,-1)<>ISNULL(d.max_per_row,-1)
            OR i.ordered<>d.ordered
            OR CONVERT(VARBINARY(MAX),i.match_rule_json)<>CONVERT(VARBINARY(MAX),d.match_rule_json)
            OR i.created_at<>d.created_at
            OR (i.lifecycle_status<>d.lifecycle_status AND i.lifecycle_status<>''RETIRED'')
       ))
       OR (d.lifecycle_status=''RETIRED'' AND (
            i.dataset_version_id<>d.dataset_version_id OR i.relation_code<>d.relation_code
            OR i.artifact_role<>d.artifact_role OR i.artifact_policy_id<>d.artifact_policy_id
            OR i.min_per_row<>d.min_per_row OR ISNULL(i.max_per_row,-1)<>ISNULL(d.max_per_row,-1)
            OR i.ordered<>d.ordered
            OR CONVERT(VARBINARY(MAX),i.match_rule_json)<>CONVERT(VARBINARY(MAX),d.match_rule_json)
            OR i.created_at<>d.created_at OR i.lifecycle_status<>d.lifecycle_status
       ))
       OR (d.lifecycle_status=''DRAFT'' AND i.lifecycle_status NOT IN(''DRAFT'',''APPROVED'',''RETIRED''))
  )
    THROW 51021, ''Artifact relation identity and approved semantics are immutable; create a new dataset version.'', 1;
END');
