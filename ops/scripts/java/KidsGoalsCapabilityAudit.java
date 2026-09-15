import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/** Read-only contract capability consistency audit for KIDS page 8. */
public final class KidsGoalsCapabilityAudit {
  public static void main(String[] args) throws Exception {
    File file = new File(args.length == 0 ? "api/kids-portal-v1-canonical-r8-final.accdb" : args[0]);
    try (Database db = new DatabaseBuilder(file).setReadOnly(true).open()) {
      Row page = null;
      for (Row row : db.getTable("__gs_page")) if ("KIDS_GOALS".equals(text(row,"page_code"))) page = row;
      if (page == null) throw new IllegalStateException("Missing KIDS_GOALS page");
      require(page, "contract_code", "KIDS_PORTAL_V1"); require(page, "contract_revision", "8");
      require(page, "dataset_code", "KIDS_GOAL"); require(page, "approval_state", "READY");
      Set<String> fields = new HashSet<>();
      for (Row row : db.getTable("__gs_field")) if ("KIDS_GOAL".equals(text(row,"dataset_code"))) fields.add(text(row,"field_name"));
      Set<String> expected = Set.of("source_goal_id","category_item_ref","title_ka","title_en","path_ka","path_en","source_row_key","operation");
      if (!fields.equals(expected)) throw new IllegalStateException("KIDS_GOAL capability fields mismatch: " + fields);
      boolean projection = false;
      for (Row row : db.getTable("__gs_projection")) if ("KIDS_GOAL".equals(text(row,"dataset_code"))) { System.out.println("projection=" + text(row,"projection_code") + ", state=" + text(row,"approval_state")); if ("READY".equals(text(row,"approval_state"))) projection = true; }
      if (!projection) throw new IllegalStateException("No READY KIDS_GOAL projection");
      System.out.println("page=KIDS_GOALS, contract=KIDS_PORTAL_V1, revision=8, dataset=KIDS_GOAL");
      System.out.println("fields=" + fields.size() + ", readyProjection=true");
      System.out.println("status=GOALS_CAPABILITY_PASS");
    }
  }
  private static String text(Row row, String col) { Object v=row.get(col); return v == null ? "" : String.valueOf(v); }
  private static void require(Row row, String col, String expected) { if (!expected.equals(text(row,col))) throw new IllegalStateException(col + " expected " + expected); }
}
