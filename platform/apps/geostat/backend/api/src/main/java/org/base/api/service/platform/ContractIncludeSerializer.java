package org.base.api.service.platform;
import java.util.*;
/** Applies contract-approved include paths to nested response maps. */
public final class ContractIncludeSerializer {
 private ContractIncludeSerializer(){}
 public static List<Map<String,Object>> apply(List<Map<String,Object>> rows,List<String> include){
  if(include==null||include.isEmpty())return rows;
  List<Map<String,Object>> out=new ArrayList<>(); for(Map<String,Object> row:rows) out.add(project(row,include,"")); return out;
 }
 public static List<Map<String,Object>> limit(List<Map<String,Object>> rows,Map<String,Integer> limits){
  if(rows==null||limits==null||limits.isEmpty())return rows;
  List<Map<String,Object>> out=new ArrayList<>();
  for(Map<String,Object> row:rows){Map<String,Object> copy=new LinkedHashMap<>(row);for(var e:limits.entrySet()){Object value=copy.get(e.getKey());int max=Math.max(0,Math.min(1000,e.getValue()==null?1000:e.getValue()));if(value instanceof List<?> list&&list.size()>max)copy.put(e.getKey(),new ArrayList<>(list.subList(0,max)));}out.add(copy);}return out;
 }
 private static Map<String,Object> project(Map<String,Object> row,List<String> paths,String prefix){
  Map<String,Object> out=new LinkedHashMap<>();
  for(var e:row.entrySet()) {
   String key=e.getKey(); String path=prefix.isEmpty()?key:prefix+"."+key;
   boolean direct=paths.stream().anyMatch(p->p.equals(path)||p.equals(key));
   boolean nested=paths.stream().anyMatch(p->p.startsWith(path+"."));
   if(direct||isIdentity(key)) out.put(key,e.getValue());
   else if(nested && e.getValue() instanceof Map<?,?> m) {
    @SuppressWarnings("unchecked") Map<String,Object> mm=(Map<String,Object>)m; out.put(key,project(mm,paths,path));
   } else if(nested && e.getValue() instanceof List<?> l) {
    List<Object> projected=new ArrayList<>(); for(Object item:l) if(item instanceof Map<?,?> m){@SuppressWarnings("unchecked") Map<String,Object> mm=(Map<String,Object>)m; projected.add(project(mm,paths,path));} else projected.add(item); out.put(key,projected);
   }
  } return out;
 }
 private static boolean isIdentity(String k){return k.equalsIgnoreCase("id")||k.endsWith("Id")||k.endsWith("_id")||k.equals("externalKey")||k.equals("sourceRecordId");}
}
