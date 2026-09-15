package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Read-only contract compiler input. No business/site constants live here. */
@Service
public class ContractMetadataService {
  private final JdbcTemplate db;
  public ContractMetadataService(@Qualifier("primaryJdbcTemplate") JdbcTemplate db){this.db=db;}
  public Map<String,Object> metadata(long revisionId,String datasetCode){
    Map<String,Object> out=new LinkedHashMap<>();
    out.put("physicalTables",db.query("SELECT t.logical_table_code,t.physical_table_name,t.access_table_name,t.table_role,t.storage_plane,t.primary_key_expression,t.load_order,t.required FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=? OR t.physical_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC,t.load_order",(r,n)->m("logicalCode",r.getString(1),"physicalTable",r.getString(2),"accessTable",r.getString(3),"role",r.getString(4),"plane",r.getString(5),"primaryKey",r.getString(6),"loadOrder",r.getInt(7),"required",r.getBoolean(8)),datasetCode,datasetCode,datasetCode,revisionId));
    out.put("indexes",db.query("SELECT i.index_code,i.index_kind,i.field_list,i.is_unique,i.required FROM platform.contract_index_definition i JOIN platform.contract_table_definition t ON t.table_definition_id=i.table_definition_id WHERE (t.logical_table_code=? OR t.access_table_name=? OR t.physical_table_name=?) AND i.lifecycle_status IN ('APPROVED','ACTIVE') AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY i.index_code",(r,n)->m("code",r.getString(1),"kind",r.getString(2),"fields",r.getString(3),"unique",r.getBoolean(4),"required",r.getBoolean(5)),datasetCode,datasetCode,datasetCode,revisionId));
    out.put("fields",db.query("SELECT f.field_name,f.logical_type,f.semantic_role,f.ordinal,f.required,f.key_role,f.classifier_scheme_code,f.source_expression,f.normalization_rule FROM platform.site_contract_field f JOIN platform.site_contract_dataset d ON d.contract_dataset_id=f.contract_dataset_id WHERE d.site_contract_revision_id=? AND d.dataset_code=? ORDER BY f.ordinal",(r,n)->m("name",r.getString(1),"logicalType",r.getString(2),"semanticRole",r.getString(3),"ordinal",r.getInt(4),"required",r.getBoolean(5),"keyRole",r.getString(6),"classifierScheme",r.getString(7),"sourceExpression",r.getString(8),"normalization",r.getString(9)),revisionId,datasetCode));
    out.put("relations",db.query("SELECT relation_code,from_dataset_code,from_field_name,to_dataset_code,to_field_name,relation_kind,cardinality,required,enforcement_policy,load_priority FROM platform.site_contract_relation WHERE site_contract_revision_id=? AND (from_dataset_code=? OR to_dataset_code=?) ORDER BY load_priority",(r,n)->m("code",r.getString(1),"fromDataset",r.getString(2),"fromField",r.getString(3),"toDataset",r.getString(4),"toField",r.getString(5),"kind",r.getString(6),"cardinality",r.getString(7),"required",r.getBoolean(8),"enforcement",r.getString(9),"priority",r.getInt(10)),revisionId,datasetCode,datasetCode));
    return out;
  }
  private static Map<String,Object> m(Object... v){Map<String,Object>x=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)x.put(String.valueOf(v[i]),v[i+1]);return x;}
}
