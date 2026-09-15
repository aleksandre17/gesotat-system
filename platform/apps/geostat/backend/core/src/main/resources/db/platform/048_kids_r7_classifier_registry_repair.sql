/* Repair Access proposal field alignment: subject_key is the canonical
   scheme|version|item tuple in the imported proposal rows. */
DECLARE @owner BIGINT=(SELECT TOP 1 id FROM dbo.users WHERE username=N'admin');
DECLARE @code NVARCHAR(128),@scheme BIGINT,@version BIGINT;
DECLARE schemes CURSOR LOCAL FAST_FORWARD FOR SELECT DISTINCT scheme_code FROM platform.classifier_proposal WHERE contract_code=N'KIDS_PORTAL_V1' AND contract_revision=7 AND proposal_type=N'ITEM' AND scheme_code IN (N'KIDS_GOAL_CATEGORY',N'KIDS_RESOURCE_SUBCATEGORY',N'AGE_GROUP',N'LANGUAGE');
OPEN schemes; FETCH NEXT FROM schemes INTO @code;
WHILE @@FETCH_STATUS=0 BEGIN
 SET @scheme=NULL; SET @version=NULL;
 SELECT @scheme=scheme_id FROM platform.classification_scheme WHERE scheme_code=@code;
 IF @scheme IS NULL BEGIN INSERT platform.classification_scheme(scheme_code,title_ka,title_en,standard_reference,owner_user_id) VALUES(@code,@code,@code,N'KIDS_R7_APPROVED_FROM_ACCESS_PROPOSALS',@owner); SET @scheme=SCOPE_IDENTITY(); END;
 SELECT @version=classification_version_id FROM platform.classification_version WHERE scheme_id=@scheme AND version=N'KIDS_PORTAL_V1_SOURCE_2026_09';
 IF @version IS NULL BEGIN INSERT platform.classification_version(scheme_id,version,status,valid_from) VALUES(@scheme,N'KIDS_PORTAL_V1_SOURCE_2026_09',N'APPROVED',SYSUTCDATETIME()); SET @version=SCOPE_IDENTITY(); END ELSE UPDATE platform.classification_version SET status=N'APPROVED' WHERE classification_version_id=@version;
 INSERT platform.classification_item(classification_version_id,code,label_ka,label_en,status,valid_from)
 SELECT @version,x.item_code,MAX(x.label_ka),MAX(x.label_en),N'ACTIVE',SYSUTCDATETIME()
 FROM (SELECT RIGHT(p.subject_key,CHARINDEX('|',REVERSE(p.subject_key))-1) item_code,COALESCE(p.label_ka,RIGHT(p.subject_key,CHARINDEX('|',REVERSE(p.subject_key))-1)) label_ka,p.label_en FROM platform.classifier_proposal p WHERE p.contract_code=N'KIDS_PORTAL_V1' AND p.contract_revision=7 AND p.proposal_type=N'ITEM' AND p.scheme_code=@code) x
 WHERE NOT EXISTS(SELECT 1 FROM platform.classification_item i WHERE i.classification_version_id=@version AND i.code=x.item_code) GROUP BY x.item_code;
 INSERT platform.classification_alias(classification_item_id,external_system_code,external_code,valid_from)
 SELECT i.classification_item_id,CONCAT(N'KIDS_R7_',@code),i.code,SYSUTCDATETIME() FROM platform.classification_item i WHERE i.classification_version_id=@version AND NOT EXISTS(SELECT 1 FROM platform.classification_alias a WHERE a.classification_item_id=i.classification_item_id AND a.external_system_code=CONCAT(N'KIDS_R7_',@code) AND a.external_code=i.code);
 FETCH NEXT FROM schemes INTO @code;
END; CLOSE schemes; DEALLOCATE schemes;
