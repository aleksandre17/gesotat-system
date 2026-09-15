import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import java.io.File;
import java.util.*;

/** Read-only source-key reconciliation for legacy category 1..4 resource scope. */
public final class KidsResourceScopeReconciliationAudit {
  public static void main(String[] args) throws Exception {
    File source = new File(args.length > 0 ? args[0] : "../../../../../samples/kids-children-portal-full-data.accdb");
    File canonical = new File(args.length > 1 ? args[1] : "kids-portal-v1-canonical-r8-final.accdb");
    Map<String,String> legacy = new LinkedHashMap<>();
    try (Database db = new DatabaseBuilder(source).setReadOnly(true).open()) {
      for (Row row : db.getTable("files")) {
        String category = text(row, "category").trim();
        if (Set.of("1","2","3","4").contains(category)) legacy.put(text(row,"ID"), category);
      }
    }
    Map<String,String> canonicalRows = new LinkedHashMap<>();
    try (Database db = new DatabaseBuilder(canonical).setReadOnly(true).open()) {
      for (Row row : db.getTable("__ent_kids_resource")) canonicalRows.put(text(row,"source_resource_id"), text(row,"category_item_ref"));
    }
    List<String> missing = new ArrayList<>(), extra = new ArrayList<>();
    for (String key : legacy.keySet()) if (!canonicalRows.containsKey(key)) missing.add(key);
    for (String key : canonicalRows.keySet()) if (!legacy.containsKey(key)) extra.add(key);
    Map<String,Integer> legacyByCategory = counts(legacy.values());
    Map<String,Integer> canonicalScopedByCategory = new TreeMap<>();
    Map<String,Integer> canonicalOutOfScopeByCategory = new TreeMap<>();
    for (Map.Entry<String,String> e : canonicalRows.entrySet()) {
      String code = categoryCode(e.getValue());
      if (Set.of("1","2","3","4").contains(code)) canonicalScopedByCategory.merge(code,1,Integer::sum);
      else canonicalOutOfScopeByCategory.merge(code.isBlank()?"UNRESOLVED":code,1,Integer::sum);
    }
    int scopeMismatch = legacy.size() - (canonicalRows.size() - extra.size());
    int unexpectedInScope = 0;
    for (String key : extra) if (Set.of("1","2","3","4").contains(categoryCode(canonicalRows.get(key)))) unexpectedInScope++;
    System.out.println("legacyScopeRows="+legacy.size());
    System.out.println("canonicalRows="+canonicalRows.size());
    System.out.println("legacyByCategory="+legacyByCategory);
    System.out.println("canonicalScopedByCategory="+canonicalScopedByCategory);
    System.out.println("canonicalOutOfScopeByCategory="+canonicalOutOfScopeByCategory);
    System.out.println("missing="+missing.size()+", extraOutOfScope="+extra.size()+", unexpectedInScope="+unexpectedInScope+", scopeMismatch="+scopeMismatch);
    if (!missing.isEmpty() || unexpectedInScope != 0 || scopeMismatch != 0) throw new IllegalStateException("RESOURCE_SCOPE_RECONCILIATION_FAILED");
    System.out.println("status=RESOURCE_SCOPE_RECONCILIATION_PASS");
  }
  private static Map<String,Integer> counts(Collection<String> values){Map<String,Integer> out=new TreeMap<>();for(String v:values)out.merge(v,1,Integer::sum);return out;}
  private static String categoryCode(String ref){if(ref==null)return "";int p=ref.lastIndexOf('|');return p<0?ref.trim():ref.substring(p+1).trim();}
  private static String text(Row row,String col){Object value=row.get(col);return value==null?"":String.valueOf(value);}
}
