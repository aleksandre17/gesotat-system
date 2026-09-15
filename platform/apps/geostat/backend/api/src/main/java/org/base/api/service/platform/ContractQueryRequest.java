package org.base.api.service.platform;
import java.util.*;
/** Transport-neutral declarative query request; identifiers are validated against the approved contract. */
public record ContractQueryRequest(Map<String,Object> filters, String sort, boolean descending,
                                   List<String> groupBy, String aggregation, Integer page, Integer limit, List<String> select, List<String> include, String cursor, Map<String,Object> where, List<Map<String,Object>> orderBy, Boolean distinct, Map<String,Integer> includeLimits) {
    public ContractQueryRequest { filters=filters==null?Map.of():Map.copyOf(filters); groupBy=groupBy==null?List.of():List.copyOf(groupBy); select=select==null?List.of():List.copyOf(select); include=include==null?List.of():List.copyOf(include); where=where==null?Map.of():Map.copyOf(where); orderBy=orderBy==null?List.of():List.copyOf(orderBy); distinct=distinct!=null&&distinct; includeLimits=includeLimits==null?Map.of():Map.copyOf(includeLimits); }
    public ContractQueryRequest(Map<String,Object> filters,String sort,boolean descending,List<String> groupBy,String aggregation,Integer page,Integer limit){this(filters,sort,descending,groupBy,aggregation,page,limit,List.of(),List.of(),null,Map.of(),List.of(),false,Map.of());}
}
