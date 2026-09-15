package org.base.api.service.platform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Resolves governed, localized and extensible metadata without coupling it to a data family. */
@Service
public class PlatformMetadataService {
 private final JdbcTemplate db; private final ObjectMapper json;
 public PlatformMetadataService(@Qualifier("primaryJdbcTemplate") JdbcTemplate db,ObjectMapper json){this.db=db;this.json=json;}
 public Map<String,Object> subject(String type,String code){
  Map<String,Object> s=db.query("SELECT TOP 1 metadata_subject_id,subject_type,subject_code,subject_revision,lifecycle_status,visibility,owner_code FROM platform.metadata_subject WHERE subject_type=? AND subject_code=? AND lifecycle_status IN ('APPROVED','PUBLISHED') AND visibility IN ('PUBLIC','INTERNAL') ORDER BY subject_revision DESC",r->r.next()?m("id",r.getLong(1),"type",r.getString(2),"code",r.getString(3),"revision",r.getInt(4),"status",r.getString(5),"visibility",r.getString(6),"owner",r.getString(7)):Map.of(),type,code);
  if(s.isEmpty()) return Map.of(); long id=((Number)s.get("id")).longValue(); Map<String,Object> out=new LinkedHashMap<>(s); out.remove("id");
  List<Map<String,Object>> rows=db.query("SELECT namespace_code,property_code,language_tag,value_type,value_text,value_number,value_boolean,value_json,ordinal FROM platform.metadata_assertion WHERE metadata_subject_id=? AND lifecycle_status IN ('APPROVED','PUBLISHED') ORDER BY namespace_code,property_code,ordinal",(r,n)->m("namespace",r.getString(1),"property",r.getString(2),"language",r.getString(3),"type",r.getString(4),"text",r.getString(5),"number",r.getObject(6),"boolean",r.getObject(7),"json",r.getString(8),"ordinal",r.getInt(9)),id);
  for(Map<String,Object> a:rows){String ns=String.valueOf(a.get("namespace")), p=String.valueOf(a.get("property")), lang=(String)a.get("language"); Object v=value(a); @SuppressWarnings("unchecked") Map<String,Object> n=(Map<String,Object>)out.computeIfAbsent(ns,k->new LinkedHashMap<>()); if(lang!=null&&!lang.isBlank()){ @SuppressWarnings("unchecked") Map<String,Object> localized=(Map<String,Object>)n.computeIfAbsent(p,k->new LinkedHashMap<>()); localized.put(lang,v);} else n.put(p,v); }
  return out;
 }
 public Map<String,Object> fromConfig(String config){try{return json.readValue(config,new TypeReference<Map<String,Object>>(){});}catch(Exception e){return Map.of();}}
 private Object value(Map<String,Object> r){String t=String.valueOf(r.get("type"));return switch(t){case "NUMBER"->r.get("number");case "BOOLEAN"->r.get("boolean");case "JSON"->parse((String)r.get("json"));default->r.get("text");};}
 private Object parse(String x){try{return json.readValue(x,Object.class);}catch(Exception e){return x;}}
 private static Map<String,Object> m(Object...v){Map<String,Object>x=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)x.put(String.valueOf(v[i]),v[i+1]);return x;}
}
