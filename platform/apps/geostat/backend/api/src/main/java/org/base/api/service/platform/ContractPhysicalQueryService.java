package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Pattern;

/** Executes a contract-registered physical table without site-specific code. */
@Service
public class ContractPhysicalQueryService {
 private static final Pattern ID=Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
 private final JdbcTemplate control; private final JdbcTemplate data;
 public ContractPhysicalQueryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate control,@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data){this.control=control;this.data=data;}
 public List<Map<String,Object>> rows(long revisionId,String datasetCode,int page,int limit,Map<String,Object> filters,String sort,boolean desc){
   Map<String,Object> t=control.queryForMap("SELECT TOP 1 t.physical_table_name,t.storage_plane,t.table_definition_id FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC",datasetCode,datasetCode,revisionId);
   if(!"DATA".equalsIgnoreCase(String.valueOf(t.get("storage_plane")))) throw new IllegalArgumentException("Only data-plane contract tables are queryable");
   String table=identifier(String.valueOf(t.get("physical_table_name")));
   List<String> fields=control.query("SELECT field_name FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY ordinal",(r,n)->r.getString(1),t.get("table_definition_id"));
   if(fields.isEmpty()) throw new IllegalArgumentException("Contract table has no approved fields: "+datasetCode);
   Set<String> allowed=new HashSet<>(fields); var compiled=ContractQueryCompiler.compile(filters,allowed,sort,desc);
   String select=fields.stream().map(x->"["+identifier(x)+"]").collect(java.util.stream.Collectors.joining(","));
   String sql="SELECT "+select+" FROM "+table+compiled.whereSql()+compiled.orderSql()+" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
   List<Object> p=new ArrayList<>(compiled.parameters()); p.add((Math.max(1,page)-1)*Math.min(Math.max(1,limit),1000));p.add(Math.min(Math.max(1,limit),1000));
   return data.query(sql,(rs,n)->{Map<String,Object>x=new LinkedHashMap<>();for(int i=1;i<=fields.size();i++)x.put(fields.get(i-1),rs.getObject(i));return x;},p.toArray());
 }

 /** Count rows using the same contract table and declared filter surface. */
 public long count(long revisionId, String datasetCode, Map<String,Object> filters) {
   Map<String,Object> t=control.queryForMap("SELECT TOP 1 t.physical_table_name,t.storage_plane,t.table_definition_id FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC",datasetCode,datasetCode,revisionId);
   if(!"DATA".equalsIgnoreCase(String.valueOf(t.get("storage_plane")))) throw new IllegalArgumentException("Only data-plane contract tables are queryable");
   String table=identifier(String.valueOf(t.get("physical_table_name")));
   List<String> fields=control.query("SELECT field_name FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY ordinal",(r,n)->r.getString(1),t.get("table_definition_id"));
   var compiled=ContractQueryCompiler.compile(filters==null?Map.of():filters,new HashSet<>(fields),null,false);
   Long value=data.queryForObject("SELECT COUNT_BIG(*) FROM "+table+compiled.whereSql(),Long.class,compiled.parameters().toArray());
   return value==null?0L:value;
 }
 public List<Map<String,Object>> rowsWithRelations(long revisionId,String datasetCode,int page,int limit,Map<String,Object> filters,String sort,boolean desc){
   List<Map<String,Object>> roots=rows(revisionId,datasetCode,page,limit,filters,sort,desc);
   return expand(roots, revisionId, datasetCode, new HashSet<>(), 0);
 }
 public List<Map<String,Object>> rowsWithRelationsWhere(long revisionId,String datasetCode,int page,int limit,Map<String,Object> where,String sort,boolean desc){
   if (containsRelation(where)) {
     List<Map<String,Object>> all=rowsWithRelations(revisionId,datasetCode,1,1000,Map.of(),sort,desc);
     all=all.stream().filter(x->ContractWhereEvaluator.matches(x,where)).toList();
     int size=Math.min(Math.max(1,limit),1000), from=Math.max(0,(Math.max(1,page)-1)*size), to=Math.min(all.size(),from+size);
     return from>=all.size()?List.of():new ArrayList<>(all.subList(from,to));
   }
   Map<String,Object> t=control.queryForMap("SELECT TOP 1 t.physical_table_name,t.storage_plane,t.table_definition_id FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC",datasetCode,datasetCode,revisionId);
   if(!"DATA".equalsIgnoreCase(String.valueOf(t.get("storage_plane")))) throw new IllegalArgumentException("Only data-plane contract tables are queryable");
   String table=identifier(String.valueOf(t.get("physical_table_name")));
   List<String> fields=control.query("SELECT field_name FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY ordinal",(r,n)->r.getString(1),t.get("table_definition_id"));
   Set<String> allowed=new HashSet<>(fields); var compiled=ContractQueryCompiler.compileWhere(where,allowed); int size=Math.min(Math.max(1,limit),1000); List<Object> p=new ArrayList<>(compiled.parameters());p.add((Math.max(1,page)-1)*size);p.add(size);
   String sql="SELECT "+fields.stream().map(x->"["+identifier(x)+"]").collect(java.util.stream.Collectors.joining(","))+" FROM "+table+compiled.whereSql()+" ORDER BY (SELECT NULL) OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
   List<Map<String,Object>> roots=data.query(sql,(rs,n)->{Map<String,Object>x=new LinkedHashMap<>();for(int i=1;i<=fields.size();i++)x.put(fields.get(i-1),rs.getObject(i));return x;},p.toArray());
   return expand(roots,revisionId,datasetCode,new HashSet<>(),0);
 }
 public List<Map<String,Object>> rowsKeyset(long revisionId,String datasetCode,int limit,Map<String,Object> filters,List<String> sortKeys,List<Object> lastValues,boolean desc){
   if(sortKeys==null||sortKeys.isEmpty()||lastValues==null||sortKeys.size()!=lastValues.size()) throw new IllegalArgumentException("Stable keyset requires sort tuple");
   Map<String,Object> t=control.queryForMap("SELECT TOP 1 t.physical_table_name,t.storage_plane,t.table_definition_id FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC",datasetCode,datasetCode,revisionId);
   if(!"DATA".equalsIgnoreCase(String.valueOf(t.get("storage_plane")))) throw new IllegalArgumentException("Only data-plane contract tables are queryable");
   String table=identifier(String.valueOf(t.get("physical_table_name"))); List<String> fields=control.query("SELECT field_name FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY ordinal",(r,n)->r.getString(1),t.get("table_definition_id")); Set<String> allowed=new HashSet<>(fields);
   for(String key:sortKeys) if(!allowed.contains(key)) throw new IllegalArgumentException("Sort key is not declared by contract: "+key);
   var compiled=ContractQueryCompiler.compile(filters==null?Map.of():filters,allowed,null,false); var keyset=KeysetPredicateBuilder.build(sortKeys,lastValues,desc); String select=fields.stream().map(x->"["+identifier(x)+"]").collect(java.util.stream.Collectors.joining(",")); String order=sortKeys.stream().map(x->"["+identifier(x)+"] "+(desc?"DESC":"ASC")).collect(java.util.stream.Collectors.joining(","));
   List<Object> params=new ArrayList<>(compiled.parameters());params.addAll(keyset.parameters());params.add(Math.min(Math.max(1,limit),1000)); String sql="SELECT "+select+" FROM "+table+compiled.whereSql()+keyset.sql()+" ORDER BY "+order+" OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
   return data.query(sql,(rs,n)->{Map<String,Object>x=new LinkedHashMap<>();for(int i=1;i<=fields.size();i++)x.put(fields.get(i-1),rs.getObject(i));return x;},params.toArray());
 }

 /** Executes keyset pagination only when the contract declares the required index. */
 public List<Map<String,Object>> rowsKeyset(long revisionId, String datasetCode, int limit,
                                             Map<String,Object> filters, StableSortSpec spec,
                                             List<Object> lastValues, boolean backward) {
   Objects.requireNonNull(spec, "stable sort spec");
   if (spec.columns().size() != (lastValues == null ? -1 : lastValues.size()))
       throw new IllegalArgumentException("Cursor tuple does not match stable sort contract");
   Map<String,Object> table=control.queryForMap("SELECT TOP 1 t.table_definition_id,t.physical_table_name FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC",datasetCode,datasetCode,revisionId);
   Integer declared=control.queryForObject("SELECT COUNT(*) FROM platform.contract_index_definition WHERE table_definition_id=? AND index_code=? AND required=1 AND lifecycle_status IN ('APPROVED','ACTIVE')",Integer.class,table.get("table_definition_id"),spec.requiredIndexCode());
   if (declared == null || declared == 0) throw new IllegalStateException("Stable keyset index is not declared by contract: " + spec.requiredIndexCode());
   List<String> keys=spec.columns().stream().map(StableSortSpec.Column::name).toList();
   List<String> fields=control.query("SELECT field_name FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY ordinal",(r,n)->r.getString(1),table.get("table_definition_id"));
   Set<String> allowed=new HashSet<>(fields); for(String key:keys) if(!allowed.contains(key)) throw new IllegalArgumentException("Sort key is not declared by contract: "+key);
   var compiled=ContractQueryCompiler.compile(filters==null?Map.of():filters,allowed,null,false);
   var keyset=KeysetPredicateBuilder.build(spec,lastValues,backward);
   String select=fields.stream().map(x->"["+identifier(x)+"]").collect(java.util.stream.Collectors.joining(","));
   String order=spec.columns().stream().map(c->"["+identifier(c.name())+"] "+(backward?(c.direction()==StableSortSpec.Direction.DESC?"ASC":"DESC"):(c.direction()==StableSortSpec.Direction.DESC?"DESC":"ASC"))).collect(java.util.stream.Collectors.joining(","));
   List<Object> params=new ArrayList<>(compiled.parameters());params.addAll(keyset.parameters());params.add(Math.min(Math.max(1,limit),1000));
   String sql="SELECT "+select+" FROM "+identifier(String.valueOf(table.get("physical_table_name")))+compiled.whereSql()+keyset.sql()+" ORDER BY "+order+" OFFSET 0 ROWS FETCH NEXT ? ROWS ONLY";
   return data.query(sql,(rs,n)->{Map<String,Object>x=new LinkedHashMap<>();for(int i=1;i<=fields.size();i++)x.put(fields.get(i-1),rs.getObject(i));return x;},params.toArray());
 }
 private static boolean containsRelation(Object x){if(!(x instanceof Map<?,?> m))return false;for(var e:m.entrySet()){if("relation".equalsIgnoreCase(String.valueOf(e.getKey())))return true;if(containsRelation(e.getValue()))return true;}return false;}
 private List<Map<String,Object>> expand(List<Map<String,Object>> parents,long revisionId,String table,Set<String> path,int depth){
   if(depth>=8 || !path.add(table)) return parents;
   List<Map<String,Object>> rels=control.query("SELECT relation_code,from_table_code,from_field_name,to_table_code,to_field_name,cardinality FROM platform.contract_structure_relation r JOIN platform.contract_structure s ON s.structure_id=r.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND r.lifecycle_status IN ('APPROVED','ACTIVE') AND r.from_table_code=? ORDER BY r.load_order",(rs,n)->m("code",rs.getString(1),"fromField",rs.getString(3),"to",rs.getString(4),"toField",rs.getString(5),"cardinality",rs.getString(6)),table);
   for(Map<String,Object> rel:rels){ String child=String.valueOf(rel.get("to")); if(path.contains(child)) continue;
     // Relation expansion is bounded by the same contract query limit as roots;
     // an include must never create an unbounded fan-out query.
     List<Map<String,Object>> childRows=rows(revisionId,child,1,1000,Map.of(),null,false);
     childRows=expand(childRows,revisionId,child,new HashSet<>(path),depth+1);
     parents=ContractRelationGraphExecutor.attach(parents,String.valueOf(rel.get("code")),childRows,String.valueOf(rel.get("fromField")),String.valueOf(rel.get("toField")),String.valueOf(rel.get("cardinality")).contains("MANY"));
   } return parents;
 }
 private static String identifier(String s){ String[] parts=s.split("\\."); if(parts.length>2)throw new IllegalArgumentException("Unsafe contract identifier"); for(String p:parts)if(!ID.matcher(p).matches())throw new IllegalArgumentException("Unsafe contract identifier"); return String.join(".",parts); }
 private static Map<String,Object> m(Object...v){Map<String,Object>x=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)x.put(String.valueOf(v[i]),v[i+1]);return x;}
}
