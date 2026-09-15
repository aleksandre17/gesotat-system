package org.base.api.service.platform;

import java.util.*;

/** Evaluates the non-SQL relation part of a governed where expression on an expanded graph. */
final class ContractWhereEvaluator {
    private ContractWhereEvaluator() { }
    static boolean matches(Map<String,Object> row, Map<String,Object> expression) {
        if (expression == null || expression.isEmpty()) return true;
        for (var e : expression.entrySet()) {
            String key=e.getKey(); Object value=e.getValue();
            if ("and".equalsIgnoreCase(key)) { if (!(value instanceof Collection<?> c) || c.stream().anyMatch(x -> !(x instanceof Map<?,?> m) || !matches(row,cast(m)))) return false; continue; }
            if ("or".equalsIgnoreCase(key)) { if (!(value instanceof Collection<?> c) || c.stream().noneMatch(x -> x instanceof Map<?,?> m && matches(row,cast(m)))) return false; continue; }
            if ("relation".equalsIgnoreCase(key)) { String name; Object where; if(value instanceof Map<?,?> rel){name=String.valueOf(rel.get("name"));where=rel.get("where");}else{name=String.valueOf(value);where=expression.get("where");} Object child=row.get(name); if (!(where instanceof Map<?,?> w)) return false; if (child instanceof Collection<?> list) { if (list.stream().noneMatch(x -> x instanceof Map<?,?> m && matches(cast(m),cast(w)))) return false; } else if (!(child instanceof Map<?,?> m) || !matches(cast(m),cast(w))) return false; continue; }
            if ("where".equalsIgnoreCase(key) && expression.get("relation") instanceof String) continue;
            if (!test(row.get(key),value)) return false;
        }
        return true;
    }
    private static boolean test(Object actual,Object condition){
        if (condition instanceof Map<?,?> c) {
            Object rawOp=c.containsKey("op")?c.get("op"):"EQ";
            String op=String.valueOf(rawOp).toUpperCase(Locale.ROOT);
            Object expected=c.get("value");
            switch(op){
                case "EQ": return Objects.equals(str(actual),str(expected));
                case "NE": return !Objects.equals(str(actual),str(expected));
                case "IN": return expected instanceof Collection<?> x && x.stream().anyMatch(v->Objects.equals(str(actual),str(v)));
                case "CONTAINS": return actual!=null&&String.valueOf(actual).contains(String.valueOf(expected));
                case "STARTS_WITH": return actual!=null&&String.valueOf(actual).startsWith(String.valueOf(expected));
                case "IS_NULL": return actual==null;
                case "NOT_NULL": return actual!=null;
                case "GT", "GTE", "LT", "LTE": {
                    if(actual instanceof Comparable<?> a&&expected instanceof Comparable<?> b){
                        int n=((Comparable<Object>)a).compareTo(b);
                        return switch(op){case "GT"->n>0;case "GTE"->n>=0;case "LT"->n<0;default->n<=0;};
                    }
                    return false;
                }
                default: return false;
            }
        }
        return Objects.equals(str(actual),str(condition));
    }
    private static String str(Object x){return x==null?null:String.valueOf(x);}
    @SuppressWarnings("unchecked") private static Map<String,Object> cast(Map<?,?> m){return (Map<String,Object>)(Map<?,?>)m;}
}
