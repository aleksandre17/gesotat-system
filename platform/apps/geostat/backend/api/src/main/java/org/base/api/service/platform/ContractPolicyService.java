package org.base.api.service.platform;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;
/** Exposes and enforces the immutable contract gate declaration at the API boundary. */
@Service
public class ContractPolicyService {
 private final JdbcTemplate db;
 public ContractPolicyService(@Qualifier("primaryJdbcTemplate") JdbcTemplate db){this.db=db;}
 public List<Map<String,Object>> gates(long revisionId){return db.query("SELECT gate_code,gate_type,severity,blocking,rule_json FROM platform.site_contract_gate WHERE site_contract_revision_id=? ORDER BY gate_code",(r,n)->m("code",r.getString(1),"type",r.getString(2),"severity",r.getString(3),"blocking",r.getBoolean(4),"rule",r.getString(5)),revisionId);}
 public void requireGoverned(long revisionId){
  List<Map<String,Object>> gs=gates(revisionId); if(gs.isEmpty()) throw new IllegalStateException("Approved contract has no governance gates");
  Set<String> codes=new HashSet<>(); boolean semantic=false, blocking=false; for(Map<String,Object> g:gs){codes.add(String.valueOf(g.get("code"))); semantic|="SEMANTIC".equals(g.get("type")); blocking|=Boolean.TRUE.equals(g.get("blocking"));}
  if(!blocking || !semantic || !codes.contains("SCHEMA_EXACT") || !codes.contains("RELATIONS_RESOLVED") || !codes.contains("PUBLICATION_ATOMIC")) throw new IllegalStateException("Contract governance gates are incomplete");
 }
 private static Map<String,Object> m(Object...v){Map<String,Object>x=new LinkedHashMap<>();for(int i=0;i<v.length;i+=2)x.put(String.valueOf(v[i]),v[i+1]);return x;}
}
