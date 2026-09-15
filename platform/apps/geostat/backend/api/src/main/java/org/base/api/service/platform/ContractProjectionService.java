package org.base.api.service.platform;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
/** Loads an approved, versioned projection definition and applies its allow-list to a response. */
@Service
public class ContractProjectionService {
 private final JdbcTemplate db; private final ObjectMapper json;
 public ContractProjectionService(@Qualifier("primaryJdbcTemplate") JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 public Map<String,Object> definition(long revisionId,String code){return db.query("SELECT TOP 1 projection_code,dataset_code,projection_family,mapping_json,approval_state,revision FROM platform.api_projection WHERE site_contract_revision_id=? AND projection_code=? AND approval_state='APPROVED' ORDER BY revision DESC",r->r.next()?m("code",r.getString(1),"dataset",r.getString(2),"family",r.getString(3),"mapping",r.getString(4),"state",r.getString(5),"revision",r.getInt(6)):Map.of(),revisionId,code);}
 public Map<String,Object> apply(Map<String,Object> envelope,String code,long revisionId){Map<String,Object>d=definition(revisionId,code); if(!d.isEmpty()) envelope.put("projection",d); return envelope;}
 public void applyRows(Map<String,Object> envelope,String code,long revisionId){
  Map<String,Object>d=definition(revisionId,code); if(d.isEmpty())return; Object raw=d.get("mapping");
  try { var root=json.readTree(String.valueOf(raw)); Object value=envelope.get("data"); if(value instanceof List<?> list){List<Map<String,Object>> shaped=new ArrayList<>(); for(Object item:list)if(item instanceof Map<?,?> row){@SuppressWarnings("unchecked") Map<String,Object> source=(Map<String,Object>)row; shaped.add(project(source,root,""));} envelope.put("data",shaped);} } catch(Exception ignored) { /* invalid projection is rejected by migration JSON check; preserve data defensively */ }
 }
 private Map<String,Object> project(Map<String,Object> source,com.fasterxml.jackson.databind.JsonNode spec,String prefix){
  com.fasterxml.jackson.databind.JsonNode inc=spec.get("include"); List<String> paths=new ArrayList<>(); if(inc!=null&&inc.isArray())inc.forEach(n->paths.add(n.asText()));
  Map<String,Object> out=new LinkedHashMap<>(); for(var e:source.entrySet()){String k=e.getKey(); String p=prefix.isEmpty()?k:prefix+"."+k; boolean keep=paths.isEmpty()||paths.stream().anyMatch(x->x.equals(k)||x.equals(p)||x.startsWith(p+".")); if(!keep&&isIdentity(k))keep=true; if(!keep)continue; Object v=e.getValue(); if(v instanceof Map<?,?> m && paths.stream().anyMatch(x->x.startsWith(p+"."))){@SuppressWarnings("unchecked") Map<String,Object> mm=(Map<String,Object>)m;v=project(mm,spec,p);} else if(v instanceof List<?> l && paths.stream().anyMatch(x->x.startsWith(p+"."))){List<Object> z=new ArrayList<>();for(Object i:l)if(i instanceof Map<?,?> m){@SuppressWarnings("unchecked") Map<String,Object> mm=(Map<String,Object>)m;z.add(project(mm,spec,p));}else z.add(i);v=z;} out.put(k,v);}
  com.fasterxml.jackson.databind.JsonNode fields=spec.get("fields"); if(fields!=null&&fields.isObject()){Map<String,Object> renamed=new LinkedHashMap<>();fields.fields().forEachRemaining(f->{if(source.containsKey(f.getValue().asText()))renamed.put(f.getKey(),source.get(f.getValue().asText()));}); if(!renamed.isEmpty())out.putAll(renamed);} return out;
 }
 private static boolean isIdentity(String k){return k.equalsIgnoreCase("id")||k.endsWith("Id")||k.endsWith("_id")||k.equals("external_key")||k.equals("externalKey");}
 private static Map<String,Object> m(Object...v){Map<String,Object>x=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)x.put(String.valueOf(v[i]),v[i+1]);return x;}
}
