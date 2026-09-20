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
 private final JdbcTemplate control; private final JdbcTemplate data; private final ServingPolicy serving;
 public ContractPhysicalQueryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate control,@Qualifier("dataPlaneJdbcTemplate") JdbcTemplate data,ServingPolicy serving){this.control=control;this.data=data;this.serving=serving;}
 public List<Map<String,Object>> rows(long revisionId,String datasetCode,int page,int limit,Map<String,Object> filters,String sort,boolean desc){
   Map<String,Object> t=control.queryForMap("SELECT TOP 1 t.physical_table_name,t.storage_plane,t.table_definition_id,t.table_role FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC",datasetCode,datasetCode,revisionId);
   serving.require(revisionId,datasetCode);
   if(!"DATA".equalsIgnoreCase(String.valueOf(t.get("storage_plane"))))
     // Served from the immutable raw record, never from an Access file: same contract fields, same limits.
     return servedRows(datasetCode,t.get("table_definition_id"),page,limit,filters);
   String declaredPhysical=String.valueOf(t.get("physical_table_name"));
   boolean canonicalEntity="ENTITY".equalsIgnoreCase(String.valueOf(t.get("table_role"))) || declaredPhysical.startsWith("__ent_");
   boolean canonicalStat="STATISTICAL".equalsIgnoreCase(String.valueOf(t.get("table_role"))) || declaredPhysical.startsWith("__stat_");
   List<String> fields=control.query("SELECT field_name FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY ordinal",(r,n)->r.getString(1),t.get("table_definition_id"));
   if(fields.isEmpty()) throw new IllegalArgumentException("Contract table has no approved fields: "+datasetCode);
   Set<String> allowed=new HashSet<>(fields); var compiled=ContractQueryCompiler.compile(filters,allowed,sort,desc);
   Long datasetVersionId=(canonicalEntity||canonicalStat)?datasetVersion(revisionId,datasetCode):null;
   if((canonicalEntity||canonicalStat) && datasetVersionId==null) throw new IllegalStateException("No dataset version binding for "+datasetCode);
   String table=(canonicalEntity||canonicalStat)?canonicalSource(fields,canonicalEntity):identifier(String.valueOf(t.get("physical_table_name")));
   String select=fields.stream().map(x->"["+identifier(x)+"]").collect(java.util.stream.Collectors.joining(","));
   // SQL Server requires ORDER BY whenever OFFSET/FETCH is used.  A plain
   // contract read may intentionally omit a sort; use a deterministic,
   // provider-neutral no-order expression rather than emitting invalid SQL.
   String order = compiled.orderSql().isBlank() ? " ORDER BY (SELECT NULL)" : compiled.orderSql();
   String sql="SELECT "+select+" FROM "+table+compiled.whereSql()+order+" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
   List<Object> p=new ArrayList<>(); if(canonicalEntity){p.add(datasetCode);p.add(datasetVersionId);} else if(canonicalStat){p.add(datasetVersionId);} p.addAll(compiled.parameters()); p.add((Math.max(1,page)-1)*Math.min(Math.max(1,limit),1000));p.add(Math.min(Math.max(1,limit),1000));
   return data.query(sql,(rs,n)->{Map<String,Object>x=new LinkedHashMap<>();for(int i=1;i<=fields.size();i++)x.put(fields.get(i-1),rs.getObject(i));return x;},p.toArray());
 }

 /** Count rows using the same contract table and declared filter surface. */
 public long count(long revisionId, String datasetCode, Map<String,Object> filters) {
   Map<String,Object> t=control.queryForMap("SELECT TOP 1 t.physical_table_name,t.storage_plane,t.table_definition_id,t.table_role FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC",datasetCode,datasetCode,revisionId);
   serving.require(revisionId,datasetCode);
   String declaredPhysical=String.valueOf(t.get("physical_table_name"));
   boolean canonicalEntity="ENTITY".equalsIgnoreCase(String.valueOf(t.get("table_role"))) || declaredPhysical.startsWith("__ent_");
   boolean canonicalStat="STATISTICAL".equalsIgnoreCase(String.valueOf(t.get("table_role"))) || declaredPhysical.startsWith("__stat_");
   List<String> fields=control.query("SELECT field_name FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY ordinal",(r,n)->r.getString(1),t.get("table_definition_id"));
   // A servable table outside the Data Plane has no physical table: it is counted over the rows it actually serves.
   if(!"DATA".equalsIgnoreCase(String.valueOf(t.get("storage_plane"))))
     return (long) serving.rawRows(datasetCode,fields,1000).stream().filter(x->ContractWhereEvaluator.matches(x,filters==null?Map.of():filters)).count();
   var compiled=ContractQueryCompiler.compile(filters==null?Map.of():filters,new HashSet<>(fields),null,false);
   Long datasetVersionId=(canonicalEntity||canonicalStat)?datasetVersion(revisionId,datasetCode):null;
   if((canonicalEntity||canonicalStat) && datasetVersionId==null) throw new IllegalStateException("No dataset version binding for "+datasetCode);
   String table=(canonicalEntity||canonicalStat)?canonicalSource(fields,canonicalEntity):identifier(String.valueOf(t.get("physical_table_name")));
   List<Object> params=new ArrayList<>(); if(canonicalEntity){params.add(datasetCode);params.add(datasetVersionId);} else if(canonicalStat){params.add(datasetVersionId);} params.addAll(compiled.parameters());
   Long value=data.queryForObject("SELECT COUNT_BIG(*) FROM "+table+compiled.whereSql(),Long.class,params.toArray());
   return value==null?0L:value;
 }
 /** Rows a servable non-DATA table serves: contract fields only, contract filters applied, same page size. */
 private List<Map<String,Object>> servedRows(String datasetCode,Object tableDefinitionId,int page,int limit,Map<String,Object> filters){
   List<String> fields=control.query("SELECT field_name FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY ordinal",(r,n)->r.getString(1),tableDefinitionId);
   List<Map<String,Object>> all=serving.rawRows(datasetCode,fields,1000).stream().filter(x->ContractWhereEvaluator.matches(x,filters==null?Map.of():filters)).toList();
   int size=Math.min(Math.max(1,limit),1000), from=Math.max(0,(Math.max(1,page)-1)*size);
   return from>=all.size()?List.of():new ArrayList<>(all.subList(from,Math.min(all.size(),from+size)));
 }

 public List<Map<String,Object>> rowsWithRelations(long revisionId,String datasetCode,int page,int limit,Map<String,Object> filters,String sort,boolean desc){
   List<Map<String,Object>> roots=rows(revisionId,datasetCode,page,limit,filters,sort,desc);
   return expand(roots, revisionId, datasetCode, new HashSet<>(), 0);
 }
 /** Loads only explicitly requested, revision-bound relation edges with a fail-closed fan-out cap. */
 public List<Map<String,Object>> rowsWithIncludes(long revisionId,String datasetCode,List<Map<String,Object>> roots,List<String> includeCodes){
   List<Map<String,Object>> result=roots==null?List.of():roots;
   if(result.isEmpty()||includeCodes==null||includeCodes.isEmpty())return result;
   for(String code:includeCodes){
     Map<String,Object> edge=control.query("SELECT TOP 1 from_field_name,to_dataset_code,to_field_name,cardinality FROM platform.site_contract_relation WHERE site_contract_revision_id=? AND relation_code=? AND from_dataset_code=?",(rs,n)->m("fromField",rs.getString(1),"toDataset",rs.getString(2),"toField",rs.getString(3),"cardinality",rs.getString(4)),revisionId,code,datasetCode).stream().findFirst().orElseThrow(()->new IllegalArgumentException("Include relation is not declared for dataset: "+code));
     String fromField=String.valueOf(edge.get("fromField")), toDataset=String.valueOf(edge.get("toDataset")), toField=String.valueOf(edge.get("toField"));
     Set<Object> keys=new LinkedHashSet<>(); for(Map<String,Object> root:result){Object key=root.get(fromField);if(key!=null)keys.add(key);}
     List<Map<String,Object>> children=List.of();
     if(!keys.isEmpty()){
       if(keys.size()>2000)throw new IllegalArgumentException("Include relation exceeds the bounded join-key budget: "+code);
       Map<String,Object> childFilter=Map.of(toField,Map.of("op","IN","value",keys));
       long matching=count(revisionId,toDataset,childFilter);
       if(matching>1000)throw new IllegalArgumentException("Include relation exceeds the 1000-row expansion budget: "+code);
       children=rows(revisionId,toDataset,1,(int)Math.max(1,matching),childFilter,toField,false);
     }
     result=ContractRelationGraphExecutor.attach(result,code,children,fromField,toField,hasManyChildren(String.valueOf(edge.get("cardinality"))));
   }
   return result;
 }
 private static boolean hasManyChildren(String cardinality){return "ONE_TO_MANY".equalsIgnoreCase(cardinality)||"MANY_TO_MANY".equalsIgnoreCase(cardinality);}
 public List<Map<String,Object>> rowsWithRelationsWhere(long revisionId,String datasetCode,int page,int limit,Map<String,Object> where,String sort,boolean desc){
   if (containsRelation(where)) {
     List<Map<String,Object>> all=rowsWithRelations(revisionId,datasetCode,1,1000,Map.of(),sort,desc);
     all=all.stream().filter(x->ContractWhereEvaluator.matches(x,where)).toList();
     int size=Math.min(Math.max(1,limit),1000), from=Math.max(0,(Math.max(1,page)-1)*size), to=Math.min(all.size(),from+size);
     return from>=all.size()?List.of():new ArrayList<>(all.subList(from,to));
   }
   Map<String,Object> t=control.queryForMap("SELECT TOP 1 t.physical_table_name,t.storage_plane,t.table_definition_id FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC",datasetCode,datasetCode,revisionId);
   serving.require(revisionId,datasetCode);
   String table=identifier(String.valueOf(t.get("physical_table_name")));
   List<String> fields=control.query("SELECT field_name FROM platform.contract_field_definition WHERE table_definition_id=? AND lifecycle_status IN ('APPROVED','ACTIVE') ORDER BY ordinal",(r,n)->r.getString(1),t.get("table_definition_id"));
   Set<String> allowed=new HashSet<>(fields); var compiled=ContractQueryCompiler.compileWhere(where,allowed); int size=Math.min(Math.max(1,limit),1000); List<Object> p=new ArrayList<>(compiled.parameters());p.add((Math.max(1,page)-1)*size);p.add(size);
   String sql="SELECT "+fields.stream().map(x->"["+identifier(x)+"]").collect(java.util.stream.Collectors.joining(","))+" FROM "+table+compiled.whereSql()+" ORDER BY (SELECT NULL) OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
   List<Map<String,Object>> roots=data.query(sql,(rs,n)->{Map<String,Object>x=new LinkedHashMap<>();for(int i=1;i<=fields.size();i++)x.put(fields.get(i-1),rs.getObject(i));return x;},p.toArray());
   return expand(roots,revisionId,datasetCode,new HashSet<>(),0);
 }
 public List<Map<String,Object>> rowsKeyset(long revisionId,String datasetCode,int limit,Map<String,Object> filters,List<String> sortKeys,List<Object> lastValues,boolean desc){
   if(sortKeys==null||sortKeys.isEmpty()||lastValues==null||sortKeys.size()!=lastValues.size()) throw new IllegalArgumentException("Stable keyset requires sort tuple");
   if(!serving.require(revisionId,datasetCode).dataPlane()) throw new IllegalArgumentException("Keyset pagination is available only for data-plane contract tables");
   Map<String,Object> t=control.queryForMap("SELECT TOP 1 t.physical_table_name,t.storage_plane,t.table_definition_id FROM platform.contract_table_definition t JOIN platform.contract_structure s ON s.structure_id=t.structure_id WHERE s.lifecycle_status IN ('APPROVED','ACTIVE') AND t.lifecycle_status IN ('APPROVED','ACTIVE') AND (t.logical_table_code=? OR t.access_table_name=?) AND EXISTS (SELECT 1 FROM platform.site_contract_dataset d WHERE d.site_contract_revision_id=? AND (d.dataset_code=t.logical_table_code OR d.dataset_code=t.access_table_name)) ORDER BY t.revision DESC",datasetCode,datasetCode,revisionId);
   serving.require(revisionId,datasetCode);
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
     // An include the caller may not read is left out of the answer; asking for it explicitly is refused at the boundary.
     if(!serving.permits(revisionId,child)) continue;
     // Relation expansion is bounded by the same contract query limit as roots;
     // an include must never create an unbounded fan-out query.
     List<Map<String,Object>> childRows=rows(revisionId,child,1,1000,Map.of(),null,false);
     childRows=expand(childRows,revisionId,child,new HashSet<>(path),depth+1);
     parents=ContractRelationGraphExecutor.attach(parents,String.valueOf(rel.get("code")),childRows,String.valueOf(rel.get("fromField")),String.valueOf(rel.get("toField")),hasManyChildren(String.valueOf(rel.get("cardinality"))));
   } return parents;
 }
 private Long datasetVersion(long revisionId,String datasetCode){
   return control.query("SELECT TOP 1 dataset_version_id FROM platform.site_contract_dataset WHERE site_contract_revision_id=? AND dataset_code=? ORDER BY dataset_version_id DESC",(rs,n)->(Long)rs.getObject(1),revisionId,datasetCode).stream().findFirst().orElse(null);
 }
 private static String canonicalSource(List<String> fields,boolean entity){
   String projection=fields.stream().map(x->entity?"JSON_VALUE(payload_json,'$."+identifier(x)+"') AS ["+identifier(x)+"]":"CASE WHEN '"+identifier(x)+"'='value_decimal' THEN CONVERT(nvarchar(128),o.numeric_value) ELSE JSON_VALUE(r.payload_json,'$."+identifier(x)+"') END AS ["+identifier(x)+"]").collect(java.util.stream.Collectors.joining(","));
   if(entity) return "(SELECT "+projection+" FROM entity.entity_record WHERE record_type=? AND is_current=1 AND dataset_snapshot_id IN (SELECT dataset_snapshot_id FROM publication.dataset_snapshot WHERE dataset_version_id=? AND status IN ('SEMANTIC_REVIEW','PUBLISHED','APPROVED'))) AS canonical";
   return "(SELECT "+projection+" FROM raw.source_record r JOIN [statistics].observation o ON o.source_record_id=r.source_record_id JOIN [statistics].series s ON s.series_id=o.series_id WHERE s.dataset_snapshot_id IN (SELECT dataset_snapshot_id FROM publication.dataset_snapshot WHERE dataset_version_id=? AND status IN ('SEMANTIC_REVIEW','PUBLISHED','APPROVED'))) AS canonical";
 }
 private static String identifier(String s){ String[] parts=s.split("\\."); if(parts.length>2)throw new IllegalArgumentException("Unsafe contract identifier"); for(String p:parts)if(!ID.matcher(p).matches())throw new IllegalArgumentException("Unsafe contract identifier"); return String.join(".",parts); }
 private static Map<String,Object> m(Object...v){Map<String,Object>x=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)x.put(String.valueOf(v[i]),v[i+1]);return x;}
}
