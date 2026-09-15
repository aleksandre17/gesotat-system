/* Idempotent reconciliation after the historical 037 partial application. */
DELETE old FROM platform.contract_source old
WHERE old.source_locator=N'ACCESS.kids_goal' AND EXISTS (SELECT 1 FROM platform.contract_source newer WHERE newer.contract_id=old.contract_id AND newer.source_locator=N'ACCESS.__ent_kids_goal');
DELETE old FROM platform.contract_source old
WHERE old.source_locator=N'ACCESS.kids_resource' AND EXISTS (SELECT 1 FROM platform.contract_source newer WHERE newer.contract_id=old.contract_id AND newer.source_locator=N'ACCESS.__ent_kids_resource');
DELETE old FROM platform.contract_source old
WHERE old.source_locator=N'ACCESS.kids_resource_subcategory_assignment' AND EXISTS (SELECT 1 FROM platform.contract_source newer WHERE newer.contract_id=old.contract_id AND newer.source_locator=N'ACCESS.__rel_kids_resource_subcategory_assignment');
DELETE old FROM platform.contract_source old
WHERE old.source_locator=N'ACCESS.kids_glossary_entry' AND EXISTS (SELECT 1 FROM platform.contract_source newer WHERE newer.contract_id=old.contract_id AND newer.source_locator=N'ACCESS.__ent_kids_glossary_entry');
DELETE old FROM platform.contract_source old
WHERE old.source_locator=N'ACCESS.kids_statistical_carrier' AND EXISTS (SELECT 1 FROM platform.contract_source newer WHERE newer.contract_id=old.contract_id AND newer.source_locator=N'ACCESS.__raw_kids_statistical_carrier');
DELETE old FROM platform.contract_source old
WHERE old.source_locator=N'ACCESS.kids_statistical_input' AND EXISTS (SELECT 1 FROM platform.contract_source newer WHERE newer.contract_id=old.contract_id AND newer.source_locator=N'ACCESS.__stat_kids_statistical_input');
DELETE old FROM platform.contract_source old
WHERE old.source_locator=N'ACCESS.kids_statistical_semantic_binding' AND EXISTS (SELECT 1 FROM platform.contract_source newer WHERE newer.contract_id=old.contract_id AND newer.source_locator=N'ACCESS.__stat_kids_statistical_semantic_binding');
UPDATE platform.contract_source SET source_locator=N'ACCESS.__ent_kids_goal' WHERE source_locator=N'ACCESS.kids_goal';
UPDATE platform.contract_source SET source_locator=N'ACCESS.__ent_kids_resource' WHERE source_locator=N'ACCESS.kids_resource';
UPDATE platform.contract_source SET source_locator=N'ACCESS.__rel_kids_resource_subcategory_assignment' WHERE source_locator=N'ACCESS.kids_resource_subcategory_assignment';
UPDATE platform.contract_source SET source_locator=N'ACCESS.__ent_kids_glossary_entry' WHERE source_locator=N'ACCESS.kids_glossary_entry';
UPDATE platform.contract_source SET source_locator=N'ACCESS.__raw_kids_statistical_carrier' WHERE source_locator=N'ACCESS.kids_statistical_carrier';
UPDATE platform.contract_source SET source_locator=N'ACCESS.__stat_kids_statistical_input' WHERE source_locator=N'ACCESS.kids_statistical_input';
UPDATE platform.contract_source SET source_locator=N'ACCESS.__stat_kids_statistical_semantic_binding' WHERE source_locator=N'ACCESS.kids_statistical_semantic_binding';
