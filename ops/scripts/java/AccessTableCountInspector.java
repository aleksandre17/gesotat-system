import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import java.io.File;

/** Small read-only verifier for generated Access packages. */
public final class AccessTableCountInspector {
  public static void main(String[] args) throws Exception {
    if(args.length != 1) throw new IllegalArgumentException("Usage: AccessTableCountInspector <file.accdb>");
    try(Database database = DatabaseBuilder.open(new File(args[0]))) {
      for(String name : new String[]{"__gs_package","__gs_dataset","__gs_field","__gs_key","__gs_relation","__gs_projection","__gs_statistical_binding","goals_titles","goals","files","glossary"}) {
        Table table = database.getTable(name);
        if(table == null) throw new IllegalStateException("Missing required table: " + name);
        long count = 0; for(Object ignored : table) count++;
        System.out.println(name + "=" + count);
      }
    }
  }
}
