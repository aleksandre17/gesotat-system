import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import java.io.File;
import java.util.*;

/** Read-only legacy chartdata vs canonical typed-input shadow parity audit. */
public final class KidsStatisticalShadowParityAudit {
  private static final ObjectMapper JSON=new ObjectMapper();
  public static void main(String[] args)throws Exception{
    File source=new File(args.length>0?args[0]:"../../../../../samples/kids-children-portal-full-data.accdb");
    File canonical=new File(args.length>1?args[1]:"kids-portal-v1-canonical-r8-final.accdb");
    Map<String,String> carrierToResource=new LinkedHashMap<>();
    try(Database db=new DatabaseBuilder(canonical).setReadOnly(true).open()){
      for(Row row:db.getTable("__raw_kids_statistical_carrier")) carrierToResource.put(text(row,"carrier_code"),text(row,"source_resource_id"));
    }
    Map<String,Integer> expected=new LinkedHashMap<>(); int parsed=0;
    try(Database db=new DatabaseBuilder(source).setReadOnly(true).open()){
      for(Row row:db.getTable("files")){String id=text(row,"ID"); if(!carrierToResource.containsKey("CARRIER|"+id)) continue; int n=countNumeric(row.get("chartdata")); if(n>0){expected.put(id,n);parsed+=n;} }
    }
    Map<String,Integer> actual=new LinkedHashMap<>(); int canonicalRows=0;
    try(Database db=new DatabaseBuilder(canonical).setReadOnly(true).open()){
      for(Row row:db.getTable("__stat_kids_statistical_input")){
        String resource=carrierToResource.get(text(row,"carrier_code"));
        if(resource!=null&&resource.contains("|"))resource=resource.substring(resource.lastIndexOf('|')+1);
        actual.merge(resource,1,Integer::sum); canonicalRows++;
      }
    }
    int missing=0,extra=0,mismatch=0;
    for(var e:expected.entrySet()){int a=actual.getOrDefault(e.getKey(),0);if(a==0)missing++;if(a!=e.getValue()){mismatch++;System.out.println("COUNT_MISMATCH resource="+e.getKey()+" legacy="+e.getValue()+" canonical="+a);}}
    for(String k:actual.keySet())if(!expected.containsKey(k))extra++;
    System.out.println("legacyNumericCells="+parsed);System.out.println("canonicalInputRows="+canonicalRows);
    System.out.println("resourceKeys="+expected.size()+", missing="+missing+", extra="+extra+", countMismatch="+mismatch);
    if(parsed!=canonicalRows||missing>0||extra>0||mismatch>0)throw new IllegalStateException("STATISTICAL_SHADOW_PARITY_FAILED");
    System.out.println("status=STATISTICAL_SHADOW_PARITY_PASS");
  }
  private static int countNumeric(Object raw)throws Exception{if(raw==null)return 0;String s=String.valueOf(raw);JsonNode a=null;try{a=JSON.readTree(s);if(a.isTextual())a=JSON.readTree(a.asText());}catch(Exception ignored){}if(a==null||!a.isArray()){try{a=JSON.readTree(s.replace("\\n","\n").replace("\\r","\r").replace("\\\"","\""));}catch(Exception ignored){return 0;}}if(!a.isArray())return 0;int n=0;for(JsonNode o:a)if(o.isObject())for(var it=o.fields();it.hasNext();){var e=it.next();if("year".equals(e.getKey())||e.getValue().isNull())continue;try{Double.parseDouble(e.getValue().asText());n++;}catch(Exception ignored){}}return n;}
  private static String text(Row r,String c){Object v=r.get(c);return v==null?"":String.valueOf(v);}
}
