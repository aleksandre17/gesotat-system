package org.base.api.service.platform;

import java.util.*;

/** Pure, database-neutral relation executor. It joins contract-shaped rows without knowing a site. */
public final class ContractRelationGraphExecutor {
    private ContractRelationGraphExecutor() { }
    public static List<Map<String,Object>> attach(List<Map<String,Object>> parents, String relationCode,
                                                  List<Map<String,Object>> children, String parentKey,
                                                  String childKey, boolean many) {
        Map<Object,List<Map<String,Object>>> index=new LinkedHashMap<>();
        for(Map<String,Object> c:children) index.computeIfAbsent(c.get(childKey),k->new ArrayList<>()).add(c);
        List<Map<String,Object>> result=new ArrayList<>();
        for(Map<String,Object> p:parents){ Map<String,Object> copy=new LinkedHashMap<>(p); List<Map<String,Object>> matches=index.getOrDefault(p.get(parentKey),List.of()); copy.put(relationCode,many?List.copyOf(matches):(matches.isEmpty()?null:matches.get(0))); result.add(copy); }
        return result;
    }
    public static void assertAcyclic(Map<String,List<String>> graph){
        Set<String> visiting=new HashSet<>(), done=new HashSet<>();
        for(String n:graph.keySet()) dfs(n,graph,visiting,done);
    }
    private static void dfs(String n,Map<String,List<String>> g,Set<String> v,Set<String> d){ if(d.contains(n))return; if(!v.add(n))throw new IllegalArgumentException("Contract relation cycle: "+n); for(String x:g.getOrDefault(n,List.of()))dfs(x,g,v,d); v.remove(n);d.add(n); }
}
