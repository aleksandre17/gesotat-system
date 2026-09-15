/* Governed publication authority required for semantic approval. */
SET NOCOUNT ON;
IF NOT EXISTS (SELECT 1 FROM dbo.permissions WHERE name=N'PUBLISH_RESOURCE')
    INSERT dbo.permissions(name) VALUES(N'PUBLISH_RESOURCE');
INSERT dbo.role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM dbo.roles r CROSS JOIN dbo.permissions p
WHERE r.name=N'ADMIN' AND p.name=N'PUBLISH_RESOURCE'
  AND NOT EXISTS (SELECT 1 FROM dbo.role_permissions x WHERE x.role_id=r.id AND x.permission_id=p.id);
