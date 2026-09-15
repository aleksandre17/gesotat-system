import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import java.io.File;

/** Creates a disposable, structurally valid package with one invalid typed value. */
public class MutateAccessForQuarantine {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) throw new IllegalArgumentException("source and output are required");
    try (Database db = DatabaseBuilder.open(new File(args[0]))) {
      Table t = db.getTable("__stat_kids_statistical_input");
      Row row = t.getNextRow();
      if (row == null) throw new IllegalStateException("statistical input is empty");
      row.put("value_lexical", "NOT_A_NUMBER");
      row.put("value_decimal", null);
      row.put("operation", "DELETE");
      t.updateRow(row);
      db.flush();
    }
    // Jackcess edits in place; the caller supplies a copied disposable path.
    System.out.println(args[1]);
  }
}
