package org.base.api.service.platform;

import java.util.*;
import java.util.regex.Pattern;

/** Compiles a restricted declarative filter/sort DSL to parameterized SQL fragments. */
public final class ContractQueryCompiler {
    private static final Pattern IDENT=Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private ContractQueryCompiler() { }
    public record Compiled(String whereSql,List<Object> parameters,String orderSql) { }
    public static Compiled compile(Map<String,Object> filters, Set<String> allowedFields, String sort, boolean desc){
        List<String> clauses=new ArrayList<>(); List<Object> params=new ArrayList<>();
        if(filters!=null) for(var e:filters.entrySet()){
            if(!allowedFields.contains(e.getKey()) || !safe(e.getKey())) throw new IllegalArgumentException("Field is not declared by contract: "+e.getKey());
            Object v=e.getValue();
            if (v instanceof Map<?,?> condition) {
                Object rawOp=condition.containsKey("op") ? condition.get("op") : "EQ";
                String op=String.valueOf(rawOp).toUpperCase(Locale.ROOT);
                Object value=condition.get("value");
                switch (op) {
                    case "EQ" -> { clauses.add("["+e.getKey()+"]=?"); params.add(value); }
                    case "NE" -> { clauses.add("["+e.getKey()+"]<>?"); params.add(value); }
                    case "GT", "GTE", "LT", "LTE" -> { clauses.add("["+e.getKey()+"]"+switch(op){case "GT"->">";case "GTE"->">=";case "LT"->"<";default->"<=";}+"?"); params.add(value); }
                    case "CONTAINS" -> { clauses.add("["+e.getKey()+"] LIKE ?"); params.add("%"+String.valueOf(value)+"%"); }
                    case "STARTS_WITH" -> { clauses.add("["+e.getKey()+"] LIKE ?"); params.add(String.valueOf(value)+"%"); }
                    case "IS_NULL" -> clauses.add("["+e.getKey()+"] IS NULL");
                    case "NOT_NULL" -> clauses.add("["+e.getKey()+"] IS NOT NULL");
                    case "BETWEEN" -> { if (!(value instanceof Collection<?> c) || c.size()!=2) throw new IllegalArgumentException("BETWEEN requires two values"); clauses.add("["+e.getKey()+"] BETWEEN ? AND ?"); params.addAll(c); }
                    case "IN" -> { if (!(value instanceof Collection<?> c) || c.isEmpty()) throw new IllegalArgumentException("IN requires at least one value"); clauses.add("["+e.getKey()+"] IN ("+String.join(",",Collections.nCopies(c.size(),"?"))+")"); params.addAll(c); }
                    default -> throw new IllegalArgumentException("Unsupported filter operator: "+op);
                }
            } else if(v instanceof Collection<?> c){ if(c.isEmpty()) continue; clauses.add("["+e.getKey()+"] IN ("+String.join(",",Collections.nCopies(c.size(),"?"))+")");params.addAll(c); }
            else if(v==null) clauses.add("["+e.getKey()+"] IS NULL"); else { clauses.add("["+e.getKey()+"]=?");params.add(v); }
        }
        String order=""; if(sort!=null&&!sort.isBlank()){if(!allowedFields.contains(sort)||!safe(sort))throw new IllegalArgumentException("Sort field is not declared by contract: "+sort);order=" ORDER BY ["+sort+"] "+(desc?"DESC":"ASC");}
        return new Compiled(clauses.isEmpty()?"":(" WHERE "+String.join(" AND ",clauses)),List.copyOf(params),order);
    }
    /** Compiles nested {and:[...]} / {or:[...]} / {field:{op,value}} expressions. */
    public static Compiled compileWhere(Map<String,Object> expression, Set<String> allowedFields){
        List<Object> params=new ArrayList<>(); String sql=expressionSql(expression,allowedFields,params); return new Compiled(sql.isBlank()?"":" WHERE "+sql,List.copyOf(params),"");
    }
    private static String expressionSql(Object node,Set<String> allowed,List<Object> params){
        if(!(node instanceof Map<?,?> map)) throw new IllegalArgumentException("where must be an object");
        List<String> parts=new ArrayList<>();
        for(var entry:map.entrySet()){
            String key=String.valueOf(entry.getKey()); Object value=entry.getValue();
            if("and".equalsIgnoreCase(key)||"or".equalsIgnoreCase(key)){
                if(!(value instanceof Collection<?> list)||list.isEmpty()) throw new IllegalArgumentException(key+" requires a non-empty array");
                List<String> children=new ArrayList<>(); for(Object child:list) children.add("("+expressionSql(child,allowed,params)+")"); parts.add(String.join(" "+key.toUpperCase(Locale.ROOT)+" ",children)); continue;
            }
            if("relation".equalsIgnoreCase(key)){
                Map<?,?> rel=value instanceof Map<?,?> m?m:null; Object child=rel==null?map.get("where"):rel.get("where"); Object name=rel==null?value:rel.get("name");
                if(name==null || !(child instanceof Map<?,?> childMap)) throw new IllegalArgumentException("relation requires name and where");
                // Relation predicates are evaluated by ContractWhereEvaluator after the
                // declared graph is expanded; this compiler still validates child fields.
                expressionSql(childMap,allowed,params); parts.add("1=1"); continue;
            }
            if("where".equalsIgnoreCase(key) && map.containsKey("relation") && map.get("relation") instanceof String) continue;
            if(!allowed.contains(key)||!safe(key)) throw new IllegalArgumentException("Field is not declared by contract: "+key);
            if(value instanceof Map<?,?> condition){Object op=condition.containsKey("op")?condition.get("op"):"EQ";Object val=condition.get("value");String operator=String.valueOf(op).toUpperCase(Locale.ROOT);switch(operator){case "EQ"-> {parts.add("["+key+"]=?");params.add(val);}case "NE"->{parts.add("["+key+"]<>?");params.add(val);}case "GT","GTE","LT","LTE"->{parts.add("["+key+"]"+(operator.equals("GT")?">":operator.equals("GTE")?">=":operator.equals("LT")?"<":"<=")+"?");params.add(val);}case "IS_NULL"->parts.add("["+key+"] IS NULL");case "NOT_NULL"->parts.add("["+key+"] IS NOT NULL");case "CONTAINS","STARTS_WITH"->{parts.add("["+key+"] LIKE ?");params.add(operator.equals("CONTAINS")?"%"+val+"%":val+"%");}case "IN"->{if(!(val instanceof Collection<?> c)||c.isEmpty())throw new IllegalArgumentException("IN requires values");parts.add("["+key+"] IN ("+String.join(",",Collections.nCopies(c.size(),"?"))+")");params.addAll(c);}case "BETWEEN"->{if(!(val instanceof Collection<?> c)||c.size()!=2)throw new IllegalArgumentException("BETWEEN requires two values");parts.add("["+key+"] BETWEEN ? AND ?");params.addAll(c);}default->throw new IllegalArgumentException("Unsupported filter operator: "+operator);}} else if(value==null){parts.add("["+key+"] IS NULL");}else{parts.add("["+key+"]=?");params.add(value);}
        }
        return String.join(" AND ",parts);
    }
    private static boolean safe(String value){return IDENT.matcher(value).matches() || (value.startsWith("dimension.") && IDENT.matcher(value.substring("dimension.".length())).matches());}
}
