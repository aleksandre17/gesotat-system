import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/** Read-only acceptance report for the production KIDS Access artifact. */
public final class KidsPortalProductionAccessAudit {
  private static final List<String> TABLES = List.of(
      "__gs_package", "__gs_page", "__gs_dataset", "__gs_field", "__gs_key", "__gs_relation", "__gs_projection", "__gs_metadata_schema", "__gs_metadata",
      "__cl_scheme", "__cl_version", "__cl_item", "__cl_alias", "__cl_hierarchy", "__raw_document",
      "__stat_unit", "__stat_metric",
      "__ent_kids_goal", "__ent_kids_resource", "__rel_kids_resource_subcategory_assignment", "__ent_kids_glossary_entry",
      "__raw_kids_statistical_carrier", "__stat_kids_statistical_input", "__rel_kids_statistical_semantic_binding");
  private static final Map<String,List<String>> REQUIRED_COLUMNS = Map.of(
      "__raw_document", List.of("source_row_key","source_table","source_primary_key","extract_sequence","source_system","source_feed","document_identity","original_filename","mime_type","byte_size","checksum","received_at","source_uri","ingestion_batch","provenance","retention_policy","confidentiality_class","payload_reference","encryption_reference","parser_status","validation_status","supersedes_source_row_key"),
      "__ent_kids_goal", List.of("source_goal_id","category_item_ref","title_ka","title_en","path_ka","path_en","source_row_key","operation"),
      "__ent_kids_resource", List.of("source_resource_id","category_item_ref","title_ka","title_en","path_ka","path_en","source_row_key","operation"),
      "__rel_kids_resource_subcategory_assignment", List.of("assignment_key","source_resource_id","subcategory_item_ref","source_token_raw","ordinal","source_row_key","operation"),
      "__ent_kids_glossary_entry", List.of("source_glossary_id","language_item_ref","language_raw","entry_text","source_row_key","operation"),
      "__raw_kids_statistical_carrier", List.of("carrier_code","source_resource_id","payload_checksum","parse_status","source_row_key","operation"),
      "__stat_kids_statistical_input", List.of("input_key","carrier_code","cell_ordinal","period_raw","period_normalized","dimension_key_raw","age_group_item_ref","value_lexical","value_decimal","json_path","source_encoding","source_row_key","operation"),
      "__rel_kids_statistical_semantic_binding", List.of("carrier_code","metric_code","unit_code","aggregation","obs_status","conf_status","quality_policy_code","confidentiality_policy_code","inference_method","operation"));

  public static void main(String[] args) throws Exception {
    File file = new File(args.length == 0 ? "samples/kids-portal-v1-canonical-r7.accdb" : args[0]);
    try (Database db = new DatabaseBuilder(file).setReadOnly(true).open()) {
      System.out.println("artifact=" + file.getCanonicalPath());
      System.out.println("bytes=" + file.length());
      for (String name : TABLES) {
        Table table = db.getTable(name);
        if (table == null) throw new IllegalStateException("Missing table: " + name);
        long rows = 0; for (Row ignored : table) rows++;
        int indexes = table.getIndexes().size();
        if (indexes == 0) throw new IllegalStateException("No declared index found: " + name);
        Set<String> actualColumns = new HashSet<>();
        table.getColumns().forEach(c -> { if (!actualColumns.add(c.getName())) throw new IllegalStateException("Duplicate column in " + name + ": " + c.getName()); });
        List<String> required = REQUIRED_COLUMNS.get(name);
        if (required != null && !actualColumns.containsAll(required)) {
          List<String> missing = required.stream().filter(c -> !actualColumns.contains(c)).toList();
          throw new IllegalStateException("Missing governed columns in " + name + ": " + missing);
        }
        System.out.println(name + ": rows=" + rows + ", columns=" + table.getColumnCount()
            + ", indexes=" + indexes + ", primaryKey=" + (table.getPrimaryKeyIndex() != null));
      }
      System.out.println("physicalRelationships=" + db.getRelationships().size());
      System.out.println("status=PRODUCTION_ACCEPTED");
    }
  }
}
