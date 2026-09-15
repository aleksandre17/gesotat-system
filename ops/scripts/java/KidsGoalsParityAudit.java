import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only stable-key and localized-field parity audit for KIDS page 8. */
public final class KidsGoalsParityAudit {
  public static void main(String[] args) throws Exception {
    File sourceFile = new File(args.length > 0 ? args[0] : "../../../../../samples/kids-children-portal-full-data.accdb");
    File canonicalFile = new File(args.length > 1 ? args[1] : "api/kids-portal-v1-canonical-r8-final.accdb");
    Map<String, Row> source = new LinkedHashMap<>();
    try (Database db = new DatabaseBuilder(sourceFile).setReadOnly(true).open()) {
      for (Row row : db.getTable("goals")) source.put(text(row, "ID"), row);
    }
    int missing = 0, extra = 0, fieldMismatch = 0, categoryMismatch = 0, unresolvedCategory = 0;
    Map<String, Row> canonical = new LinkedHashMap<>();
    try (Database db = new DatabaseBuilder(canonicalFile).setReadOnly(true).open()) {
      for (Row row : db.getTable("__ent_kids_goal")) canonical.put(text(row, "source_goal_id"), row);
      Map<String, String> classifierCodes = new LinkedHashMap<>();
      for (Row row : db.getTable("__cl_item")) classifierCodes.put(text(row, "item_ref"), text(row, "item_code"));
      for (Map.Entry<String, Row> entry : source.entrySet()) {
        Row actual = canonical.get(entry.getKey());
        if (actual == null) continue;
        String expectedCategory = text(entry.getValue(), "category");
        String actualCategory = classifierCodes.get(text(actual, "category_item_ref"));
        if (actualCategory == null) { unresolvedCategory++; System.out.println("UNRESOLVED category key=" + entry.getKey()); }
        else if (!expectedCategory.equals(actualCategory)) { categoryMismatch++; System.out.println("CATEGORY_MISMATCH key=" + entry.getKey()); }
      }
      System.out.println("categoryUnresolved=" + unresolvedCategory + ", categoryMismatch=" + categoryMismatch);
    }
    for (Map.Entry<String, Row> entry : source.entrySet()) {
      Row actual = canonical.get(entry.getKey());
      if (actual == null) { missing++; System.out.println("MISSING canonical key=" + entry.getKey()); continue; }
      Row expected = entry.getValue();
      compare(entry.getKey(), "title_ka", text(expected, "title_geo"), text(actual, "title_ka"));
      compare(entry.getKey(), "title_en", text(expected, "title_eng"), text(actual, "title_en"));
      compare(entry.getKey(), "path_ka", text(expected, "path_geo"), text(actual, "path_ka"));
      compare(entry.getKey(), "path_en", text(expected, "path_eng"), text(actual, "path_en"));
      if (!equal(text(expected, "title_geo"), text(actual, "title_ka"))
          || !equal(text(expected, "title_eng"), text(actual, "title_en"))
          || !equal(text(expected, "path_geo"), text(actual, "path_ka"))
          || !equal(text(expected, "path_eng"), text(actual, "path_en"))) fieldMismatch++;
    }
    for (String key : canonical.keySet()) if (!source.containsKey(key)) { extra++; System.out.println("EXTRA canonical key=" + key); }
    System.out.println("sourceRows=" + source.size());
    System.out.println("canonicalRows=" + canonical.size());
    System.out.println("missing=" + missing + ", extra=" + extra + ", fieldMismatch=" + fieldMismatch);
    if (missing > 0 || extra > 0 || fieldMismatch > 0 || unresolvedCategory > 0 || categoryMismatch > 0) throw new IllegalStateException("GOALS_PARITY_FAILED");
    System.out.println("status=GOALS_PARITY_PASS");
  }

  private static void compare(String key, String field, String expected, String actual) {
    if (!equal(expected, actual)) System.out.println("MISMATCH key=" + key + " field=" + field);
  }
  private static boolean equal(String a, String b) { return a == null ? b == null : a.equals(b); }
  private static String text(Row row, String column) { Object value = row.get(column); return value == null ? null : String.valueOf(value); }
}
