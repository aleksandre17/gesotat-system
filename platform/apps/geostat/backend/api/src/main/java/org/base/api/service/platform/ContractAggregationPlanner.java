package org.base.api.service.platform;
import java.math.BigDecimal;
import java.util.*;
/** Database-neutral aggregation semantics shared by every adapter. */
public final class ContractAggregationPlanner {
 private ContractAggregationPlanner(){}
 public static List<Map<String,Object>> aggregate(List<Map<String,Object>> rows,List<String> groups,String field,String operation){
  String op=operation==null?"COUNT":operation.toUpperCase(Locale.ROOT);if(!Set.of("SUM","COUNT","AVG","MIN","MAX").contains(op))throw new IllegalArgumentException("Unsupported aggregation");
  Map<List<Object>,List<Map<String,Object>>> buckets=new LinkedHashMap<>();for(Map<String,Object> r:rows){List<Object> k=groups.stream().map(r::get).toList();buckets.computeIfAbsent(k,x->new ArrayList<>()).add(r);} List<Map<String,Object>> out=new ArrayList<>();
  for(var e:buckets.entrySet()){Map<String,Object> x=new LinkedHashMap<>();for(int i=0;i<groups.size();i++)x.put(groups.get(i),e.getKey().get(i));List<BigDecimal> vals=e.getValue().stream().map(r->r.get(field)).filter(Objects::nonNull).map(v->new BigDecimal(v.toString())).toList();Object result=op.equals("COUNT")?e.getValue().size():vals.isEmpty()?null:op.equals("SUM")?vals.stream().reduce(BigDecimal.ZERO,BigDecimal::add):op.equals("AVG")?vals.stream().reduce(BigDecimal.ZERO,BigDecimal::add).divide(BigDecimal.valueOf(vals.size()),10,java.math.RoundingMode.HALF_UP):op.equals("MIN")?Collections.min(vals):Collections.max(vals);x.put("aggregate_value",result);out.add(x);}return out;
 }
}
