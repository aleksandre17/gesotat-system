/* Explicit index contract. Every materialized table has at least one primary index. */
IF OBJECT_ID(N'platform.contract_index_definition',N'U') IS NULL CREATE TABLE platform.contract_index_definition(
  index_definition_id BIGINT IDENTITY PRIMARY KEY,
  table_definition_id BIGINT NOT NULL,
  index_code NVARCHAR(160) NOT NULL,
  index_kind VARCHAR(24) NOT NULL,
  field_list NVARCHAR(1000) NOT NULL,
  is_unique BIT NOT NULL,
  required BIT NOT NULL DEFAULT 1,
  lifecycle_status VARCHAR(24) NOT NULL DEFAULT 'APPROVED',
  CONSTRAINT uq_contract_index UNIQUE(table_definition_id,index_code),
  CONSTRAINT fk_contract_index_table FOREIGN KEY(table_definition_id) REFERENCES platform.contract_table_definition(table_definition_id),
  CONSTRAINT ck_contract_index_kind CHECK(index_kind IN('PRIMARY','UNIQUE','SECONDARY'))
);

INSERT platform.contract_index_definition(table_definition_id,index_code,index_kind,field_list,is_unique,required)
SELECT td.table_definition_id,CONCAT(td.logical_table_code,'__PK'),'PRIMARY',td.primary_key_expression,1,1
FROM platform.contract_table_definition td
WHERE td.revision IN (1,7) AND td.primary_key_expression IS NOT NULL AND td.primary_key_expression<>N'contract-declared key'
AND NOT EXISTS(SELECT 1 FROM platform.contract_index_definition x WHERE x.table_definition_id=td.table_definition_id AND x.index_code=CONCAT(td.logical_table_code,'__PK'));

/* Foreign-key/classifier fields always receive a lookup index unless already part of PK. */
INSERT platform.contract_index_definition(table_definition_id,index_code,index_kind,field_list,is_unique,required)
SELECT td.table_definition_id,CONCAT(td.logical_table_code,'__IX__',REPLACE(f.field_name,N' ',N'_')),'SECONDARY',f.field_name,0,1
FROM platform.contract_field_definition f JOIN platform.contract_table_definition td ON td.table_definition_id=f.table_definition_id
WHERE td.revision IN (1,7) AND (f.semantic_role IN('FOREIGN_KEY','CLASSIFICATION') OR f.classifier_scheme_code IS NOT NULL)
AND NOT EXISTS(SELECT 1 FROM platform.contract_index_definition x WHERE x.table_definition_id=td.table_definition_id AND x.index_code=CONCAT(td.logical_table_code,'__IX__',REPLACE(f.field_name,N' ',N'_')));
