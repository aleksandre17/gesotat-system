/* Explicit owner approval for the bounded KIDS r7 proposal set.  This is
   scoped by contract revision and source-generated version; no other registry
   version is promoted. */
DECLARE @owner BIGINT=(SELECT TOP 1 id FROM dbo.users WHERE username=N'admin');
DECLARE @scheme BIGINT,@version BIGINT;
DECLARE schemes CURSOR LOCAL FAST_FORWARD FOR
  SELECT DISTINCT scheme_code FROM platform.classifier_proposal
  WHERE contract_code=N'KIDS_PORTAL_V1' AND contract_revision=7 AND proposal_type=N'ITEM' AND scheme_code IN (N'KIDS_GOAL_CATEGORY',N'KIDS_RESOURCE_SUBCATEGORY',N'AGE_GROUP',N'LANGUAGE');
DECLARE @code NVARCHAR(128); OPEN schemes; FETCH NEXT FROM schemes INTO @code;
WHILE @@FETCH_STATUS=0 BEGIN
  SELECT @scheme=scheme_id FROM platform.classification_scheme WHERE scheme_code=@code;
  IF @scheme IS NULL BEGIN INSERT platform.classification_scheme(scheme_code,title_ka,title_en,standard_reference,owner_user_id) VALUES(@code,@code,@code,N'KIDS_R7_APPROVED_FROM_ACCESS_PROPOSALS',@owner); SET @scheme=SCOPE_IDENTITY(); END;
  SELECT @version=classification_version_id FROM platform.classification_version WHERE scheme_id=@scheme AND version=N'KIDS_PORTAL_V1_SOURCE_2026_09';
  IF @version IS NULL BEGIN INSERT platform.classification_version(scheme_id,version,status,valid_from) VALUES(@scheme,N'KIDS_PORTAL_V1_SOURCE_2026_09',N'APPROVED',SYSUTCDATETIME()); SET @version=SCOPE_IDENTITY(); END ELSE UPDATE platform.classification_version SET status=N'APPROVED' WHERE classification_version_id=@version;
  INSERT platform.classification_item(classification_version_id,code,label_ka,label_en,status,valid_from)
  SELECT @version,p.item_code,COALESCE(p.label_ka,p.item_code),p.label_en,N'ACTIVE',SYSUTCDATETIME()
  FROM platform.classifier_proposal p WHERE p.contract_revision=7 AND p.proposal_type=N'ITEM' AND p.scheme_code=@code AND p.version_code=N'KIDS_PORTAL_V1_SOURCE_2026_09' AND NOT EXISTS(SELECT 1 FROM platform.classification_item i WHERE i.classification_version_id=@version AND i.code=p.item_code);
  INSERT platform.classification_alias(classification_item_id,external_system_code,external_code,valid_from)
  SELECT i.classification_item_id,N'KIDS_LEGACY_ACCESS',i.code,SYSUTCDATETIME() FROM platform.classification_item i WHERE i.classification_version_id=@version AND NOT EXISTS(SELECT 1 FROM platform.classification_alias a WHERE a.classification_item_id=i.classification_item_id AND a.external_system_code=N'KIDS_LEGACY_ACCESS' AND a.external_code=i.code);
  FETCH NEXT FROM schemes INTO @code;
END; CLOSE schemes; DEALLOCATE schemes;
UPDATE platform.classifier_proposal SET state=N'ACCEPTED',reviewed_at=SYSUTCDATETIME(),reviewed_by_user_id=@owner,review_note=N'KIDS r7 bounded owner approval' WHERE contract_code=N'KIDS_PORTAL_V1' AND contract_revision=7 AND state=N'DRAFT' AND proposal_type=N'ITEM';
