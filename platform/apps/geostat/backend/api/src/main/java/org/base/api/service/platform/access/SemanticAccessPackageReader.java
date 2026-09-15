package org.base.api.service.platform.access;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.StreamSupport;

/** Reads v3 declarations only. It never executes embedded SQL or uses a package-supplied target connection. */
@Service
public class SemanticAccessPackageReader {
    private static final List<String> REQUIRED = List.of("__gs_package", "__gs_dataset", "__gs_field", "__gs_key", "__gs_projection");

    public SemanticAccessPackage read(File accessFile) throws IOException {
        try (Database database = new DatabaseBuilder(accessFile).setReadOnly(true).open()) {
            Map<String, String> tables = database.getTableNames().stream().collect(java.util.stream.Collectors.toMap(name -> name.toLowerCase(Locale.ROOT), name -> name));
            for (String name : REQUIRED) if (!tables.containsKey(name)) throw new IllegalArgumentException("Missing v3 package metadata table: " + name);
            List<Row> packageRows = rows(database.getTable(tables.get("__gs_package"))).toList();
            if (packageRows.size() != 1) throw new IllegalArgumentException("__gs_package must contain exactly one row");
            Row header = packageRows.get(0);
            List<SemanticAccessPage> pages = optional(database, tables, "__gs_page").map(this::page).toList();
            List<SemanticAccessMetadataSchema> metadataSchemas = optional(database, tables, "__gs_metadata_schema").map(this::metadataSchema).toList();
            List<SemanticAccessMetadataAssertion> metadataAssertions = optional(database, tables, "__gs_metadata").map(this::metadataAssertion).toList();
            return new SemanticAccessPackage(required(header, "product_code"), required(header, "contract_code"), integer(header, "contract_revision"), required(header, "package_code"), required(header, "package_version"),
                    rows(database.getTable(tables.get("__gs_dataset"))).map(this::dataset).toList(),
                    rows(database.getTable(tables.get("__gs_field"))).map(this::field).toList(),
                    rows(database.getTable(tables.get("__gs_key"))).map(this::key).toList(),
                    optional(database, tables, "__gs_relation").map(this::relation).toList(),
                    rows(database.getTable(tables.get("__gs_projection"))).map(this::projection).toList(),
                    optional(database, tables, "__gs_statistical_binding").map(this::statisticalBinding).toList(), pages, metadataSchemas, metadataAssertions);
        }
    }

    private SemanticAccessDataset dataset(Row row) { return new SemanticAccessDataset(required(row,"dataset_code"),required(row,"access_table_name"),required(row,"data_family"),required(row,"business_grain")); }
    private SemanticAccessField field(Row row) { return new SemanticAccessField(required(row,"dataset_code"),required(row,"field_name"),required(row,"logical_type"),required(row,"semantic_role"),bool(row,"required")); }
    private SemanticAccessKey key(Row row) { return new SemanticAccessKey(required(row,"dataset_code"),required(row,"field_name"),required(row,"key_role"),integer(row,"key_order")); }
    private SemanticAccessRelation relation(Row row) { return new SemanticAccessRelation(required(row,"relationship_code"),required(row,"from_dataset_code"),required(row,"from_field"),required(row,"to_dataset_code"),required(row,"to_field"),required(row,"cardinality"),bool(row,"required")); }
    private SemanticAccessProjection projection(Row row) { return new SemanticAccessProjection(required(row,"projection_code"),required(row,"dataset_code"),required(row,"projection_family"),required(row,"mapping_json"),required(row,"approval_state")); }
    private SemanticAccessStatisticalBinding statisticalBinding(Row row) { return new SemanticAccessStatisticalBinding(required(row,"source_dataset_code"),required(row,"source_external_key"),required(row,"projection_code"),required(row,"dataflow_code"),required(row,"binding_state"),required(row,"evidence_kind")); }
    private SemanticAccessPage page(Row row) { return new SemanticAccessPage(required(row,"page_code"),required(row,"node_code"),required(row,"contract_code"),integer(row,"contract_revision"),value(row,"parent_page_code"),required(row,"node_kind"),value(row,"dataset_code"),required(row,"path_segment"),value(row,"response_projection_code"),integer(row,"sort_order"),bool(row,"required"),required(row,"approval_state")); }
    private SemanticAccessMetadataSchema metadataSchema(Row row) { return new SemanticAccessMetadataSchema(required(row,"schema_code"),required(row,"namespace_code"),integer(row,"schema_revision"),required(row,"schema_json"),required(row,"approval_state")); }
    private SemanticAccessMetadataAssertion metadataAssertion(Row row) { return new SemanticAccessMetadataAssertion(required(row,"subject_type"),required(row,"subject_code"),integer(row,"subject_revision"),required(row,"namespace_code"),required(row,"property_code"),value(row,"language_tag"),required(row,"value_type"),value(row,"value_text"),value(row,"value_json"),integer(row,"ordinal"),required(row,"lifecycle_status"),value(row,"source_reference")); }
    private java.util.stream.Stream<Row> rows(Table table) { return StreamSupport.stream(table.spliterator(), false); }
    private java.util.stream.Stream<Row> optional(Database database, Map<String,String> tables, String name) throws IOException { return tables.containsKey(name) ? rows(database.getTable(tables.get(name))) : java.util.stream.Stream.empty(); }
    private String required(Row row,String name) { String value=value(row,name); if(value==null||value.isBlank())throw new IllegalArgumentException("Missing metadata value: "+name); return value; }
    private String value(Row row,String name) { Object value=row.get(name); return value==null?null:String.valueOf(value).trim(); }
    private boolean bool(Row row,String name) { String value=value(row,name); return value!=null && ("true".equalsIgnoreCase(value)||"1".equals(value)||"yes".equalsIgnoreCase(value)); }
    private int integer(Row row,String name) { try{return Integer.parseInt(required(row,name));}catch(NumberFormatException e){throw new IllegalArgumentException("Invalid integer metadata value: "+name,e);} }
}
