import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only stable-key/language/text parity audit for KIDS page 10. */
public final class KidsGlossaryParityAudit {
  public static void main(String[] args) throws Exception {
    File sourceFile = new File(args.length > 0 ? args[0] : "../../../../../samples/kids-children-portal-full-data.accdb");
    File canonicalFile = new File(args.length > 1 ? args[1] : "kids-portal-v1-canonical-r8-final.accdb");
    Map<String, Row> source = new LinkedHashMap<>();
    try (Database db = new DatabaseBuilder(sourceFile).setReadOnly(true).open()) {
      for (Row row : db.getTable("glossary")) source.put(key(row, "ID", "lang"), row);
    }
    Map<String, Row> canonical = new LinkedHashMap<>();
    try (Database db = new DatabaseBuilder(canonicalFile).setReadOnly(true).open()) {
      for (Row row : db.getTable("__ent_kids_glossary_entry")) canonical.put(key(row, "source_glossary_id", "language_raw"), row);
    }
    int missing=0, extra=0, mismatch=0;
    for (var e: source.entrySet()) {
      Row actual=canonical.get(e.getKey());
      if(actual==null){missing++;System.out.println("MISSING canonical key="+e.getKey());continue;}
      if(!eq(text(e.getValue(),"text"),text(actual,"entry_text"))){mismatch++;System.out.println("TEXT_MISMATCH key="+e.getKey());}
    }
    for(String key:canonical.keySet())if(!source.containsKey(key)){extra++;System.out.println("EXTRA canonical key="+key);}
    System.out.println("sourceRows="+source.size());
    System.out.println("canonicalRows="+canonical.size());
    System.out.println("missing="+missing+", extra="+extra+", textMismatch="+mismatch);
    if(missing>0||extra>0||mismatch>0)throw new IllegalStateException("GLOSSARY_PARITY_FAILED");
    System.out.println("status=GLOSSARY_PARITY_PASS");
  }
  private static String key(Row r,String... cols){StringBuilder b=new StringBuilder();for(String c:cols)b.append('|').append(text(r,c));return b.toString();}
  private static String text(Row r,String c){Object v=r.get(c);return v==null?null:String.valueOf(v);}
  private static boolean eq(String a,String b){return a==null?b==null:a.equals(b);}
}
