package org.base.api.service.platform;
import java.util.*;
/** Contract-safe response shaping; never exposes fields outside the declared projection. */
public final class ContractResponseSerializer {
 private ContractResponseSerializer(){}
 public static List<Map<String,Object>> select(List<Map<String,Object>> rows,List<String> fields){
  if(fields==null||fields.isEmpty())return rows; List<Map<String,Object>> out=new ArrayList<>();
  for(Map<String,Object> row:rows){Map<String,Object>x=new LinkedHashMap<>();for(String f:fields)if(row.containsKey(f))x.put(f,row.get(f));out.add(x);} return out;
 }
 public static List<Map<String,Object>> distinct(List<Map<String,Object>> rows){
  if(rows==null||rows.size()<2)return rows;
  Set<String> seen=new LinkedHashSet<>(); List<Map<String,Object>> out=new ArrayList<>();
  for(Map<String,Object> row:rows){String key=row.toString();if(seen.add(key))out.add(row);} return out;
 }
}
