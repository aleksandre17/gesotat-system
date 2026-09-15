package org.base.api.service.platform;

import java.util.*;

/** Builds a parameterized lexicographic keyset predicate from approved sort metadata. */
public final class KeysetPredicateBuilder {
 public record Result(String sql,List<Object> parameters){}
 private KeysetPredicateBuilder(){}
 public static Result build(List<String> columns,List<Object> last,boolean descending){
  if(columns==null||columns.isEmpty()||last==null||columns.size()!=last.size()) throw new IllegalArgumentException("Stable keyset requires matching sort tuple");
  List<Object> p=new ArrayList<>(); List<String> terms=new ArrayList<>(); String op=descending?"<":">";
  for(int i=0;i<columns.size();i++){StringBuilder t=new StringBuilder("(");for(int j=0;j<i;j++){if(j>0)t.append(" AND ");t.append("[").append(identifier(columns.get(j))).append("] = ?");p.add(last.get(j));}if(i>0)t.append(" AND ");t.append("[").append(identifier(columns.get(i))).append("] ").append(op).append(" ?)");p.add(last.get(i));terms.add(t.toString());}
  return new Result(" AND ("+String.join(" OR ",terms)+")",p);
 }
 /** Builds a tuple predicate from the contract-declared mixed-direction sort spec. */
 public static Result build(StableSortSpec spec,List<Object> last,boolean backward){
  Objects.requireNonNull(spec,"sort spec");
  if(last==null||spec.columns().size()!=last.size()) throw new IllegalArgumentException("Stable keyset requires matching sort tuple");
  List<Object> p=new ArrayList<>(); List<String> terms=new ArrayList<>();
  for(int i=0;i<spec.columns().size();i++){
   StringBuilder t=new StringBuilder("(");
   for(int j=0;j<i;j++){ if(j>0)t.append(" AND "); t.append("[").append(identifier(spec.columns().get(j).name())).append("]");
    Object value=last.get(j); if(value==null)t.append(" IS NULL"); else {t.append(" = ?");p.add(value);} }
   if(i>0)t.append(" AND ");
   var c=spec.columns().get(i); Object value=last.get(i);
   String col="["+identifier(c.name())+"]";
   if(value==null){
    // NULL placement is contract-defined; never rely on vendor NULL ordering.
    boolean afterNull = c.nullOrder()==StableSortSpec.NullOrder.FIRST ? !backward : backward;
    t.append(afterNull ? col+" IS NOT NULL" : "1=0");
   } else {
    // A non-null cursor is followed/preceded by NULL according to the
    // declared absolute NULLS FIRST/LAST policy, otherwise by value order.
    String op=c.direction()==StableSortSpec.Direction.ASC?">":"<"; if(backward)op=op.equals(">")?"<":">";
    boolean nullIsOnRequestedSide = backward
            ? c.nullOrder()==StableSortSpec.NullOrder.FIRST
            : c.nullOrder()==StableSortSpec.NullOrder.LAST;
    String nullBranch = nullIsOnRequestedSide ? col+" IS NULL" : "1=0";
    // Keep the value and NULL branches explicit; the equality prefix already
    // guarantees that this term represents one lexicographic position.
    t.append("(").append(nullBranch).append(" OR ").append(col).append(" ").append(op).append(" ?)"); p.add(value);
   }
   t.append(")"); terms.add(t.toString());
  }
  return new Result(" AND ("+String.join(" OR ",terms)+")",p);
 }
 private static String identifier(String x){if(x==null||!x.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalArgumentException("Unapproved keyset column");return x;}
}
