import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;

import java.io.File;

/**
 * Creates a strict, self-describing Semantic Access Package v3 example.
 *
 * It is deliberately a generic reference package: its DRAFT codes and mappings
 * are proposals only, never a substitute for a steward-approved Core contract.
 * Usage: java ... SemanticAccessPackageV3Generator [output.accdb]
 */
public final class SemanticAccessPackageV3Generator {
    private SemanticAccessPackageV3Generator() { }

    public static void main(String[] args) throws Exception {
        File output = new File(args.length == 0 ? "samples/semantic-access-package-v3-template.accdb" : args[0]);
        if (output.exists()) throw new IllegalStateException("Refusing to overwrite: " + output.getAbsolutePath());
        File parent = output.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Cannot create: " + parent);
        try (Database database = DatabaseBuilder.create(Database.FileFormat.V2010, output)) {
            manifest(database);
            sourceTables(database);
        }
        System.out.println("Created: " + output.getAbsolutePath());
    }

    private static void manifest(Database db) throws Exception {
        text(db, "__gs_package", "product_code", "contract_code", "contract_revision", "package_code", "package_version")
                .addRow("EXAMPLE_PORTAL", "EXAMPLE_PORTAL_CONTENT_V1", "1", "example-semantic-package", "3.0.0");

        Table datasets = text(db, "__gs_dataset", "dataset_code", "access_table_name", "data_family", "business_grain");
        datasets.addRow("EXAMPLE_GOAL", "goal", "ENTITY", "one sustainable-development goal");
        datasets.addRow("EXAMPLE_RESOURCE", "resource", "ENTITY", "one localized portal resource");
        datasets.addRow("EXAMPLE_CHILD_OBSERVATION", "child_observation", "STATISTICAL", "one measure by goal and reference period");

        Table fields = text(db, "__gs_field", "dataset_code", "field_name", "logical_type", "semantic_role", "required");
        field(fields, "EXAMPLE_GOAL", "goal_code", "CODE", "NATURAL_KEY", true);
        field(fields, "EXAMPLE_GOAL", "title_ka", "TEXT", "LOCALIZED_TEXT", true);
        field(fields, "EXAMPLE_GOAL", "title_en", "TEXT", "LOCALIZED_TEXT", true);
        field(fields, "EXAMPLE_RESOURCE", "resource_code", "CODE", "NATURAL_KEY", true);
        field(fields, "EXAMPLE_RESOURCE", "goal_code", "CODE", "FOREIGN_KEY", true);
        field(fields, "EXAMPLE_RESOURCE", "title_ka", "TEXT", "LOCALIZED_TEXT", true);
        field(fields, "EXAMPLE_RESOURCE", "title_en", "TEXT", "LOCALIZED_TEXT", true);
        field(fields, "EXAMPLE_RESOURCE", "uri_ka", "URI", "LOCATOR", true);
        field(fields, "EXAMPLE_RESOURCE", "uri_en", "URI", "LOCATOR", true);
        field(fields, "EXAMPLE_CHILD_OBSERVATION", "source_row_code", "CODE", "NATURAL_KEY", true);
        field(fields, "EXAMPLE_CHILD_OBSERVATION", "goal_code", "CODE", "DIMENSION", true);
        field(fields, "EXAMPLE_CHILD_OBSERVATION", "reference_year", "INTEGER", "PERIOD", true);
        field(fields, "EXAMPLE_CHILD_OBSERVATION", "age_band_code", "CODE", "DIMENSION", true);
        field(fields, "EXAMPLE_CHILD_OBSERVATION", "value", "DECIMAL", "MEASURE", true);

        Table keys = text(db, "__gs_key", "dataset_code", "field_name", "key_role", "key_order");
        key(keys, "EXAMPLE_GOAL", "goal_code", "NATURAL", 1);
        key(keys, "EXAMPLE_RESOURCE", "resource_code", "NATURAL", 1);
        key(keys, "EXAMPLE_CHILD_OBSERVATION", "source_row_code", "NATURAL", 1);

        Table relations = text(db, "__gs_relation", "relationship_code", "from_dataset_code", "from_field", "to_dataset_code", "to_field", "cardinality", "required");
        relation(relations, "EXAMPLE_RESOURCE_FOR_GOAL", "EXAMPLE_RESOURCE", "goal_code", "EXAMPLE_GOAL", "goal_code", "MANY_TO_ONE", true);
        relation(relations, "EXAMPLE_OBSERVATION_FOR_GOAL", "EXAMPLE_CHILD_OBSERVATION", "goal_code", "EXAMPLE_GOAL", "goal_code", "MANY_TO_ONE", true);

        Table projections = text(db, "__gs_projection", "projection_code", "dataset_code", "projection_family", "mapping_json", "approval_state");
        projections.addRow("EXAMPLE_GOAL_ENTITY", "EXAMPLE_GOAL", "ENTITY", "{\"approvalState\":\"DRAFT\",\"entityTypeCode\":\"EXAMPLE_GOAL\",\"externalKeyField\":\"goal_code\",\"localizedText\":[{\"field\":\"title_ka\",\"language\":\"ka\"},{\"field\":\"title_en\",\"language\":\"en\"}]}", "DRAFT");
        projections.addRow("EXAMPLE_RESOURCE_ENTITY", "EXAMPLE_RESOURCE", "ENTITY", "{\"approvalState\":\"DRAFT\",\"entityTypeCode\":\"EXAMPLE_RESOURCE\",\"externalKeyField\":\"resource_code\",\"localizedText\":[{\"field\":\"title_ka\",\"language\":\"ka\"},{\"field\":\"title_en\",\"language\":\"en\"}],\"locators\":[{\"field\":\"uri_ka\",\"language\":\"ka\"},{\"field\":\"uri_en\",\"language\":\"en\"}]}", "DRAFT");
        projections.addRow("EXAMPLE_CHILD_STATISTIC", "EXAMPLE_CHILD_OBSERVATION", "STATISTICAL", "{\"approvalState\":\"DRAFT\",\"metricCode\":\"EXAMPLE_CHILD_COUNT\",\"periodField\":\"reference_year\",\"valueField\":\"value\",\"dimensions\":[{\"field\":\"goal_code\",\"dimensionCode\":\"GOAL\"},{\"field\":\"age_band_code\",\"dimensionCode\":\"AGE_BAND\"}]}", "DRAFT");
    }

    private static void sourceTables(Database db) throws Exception {
        Table goal = table(db, "goal", col("goal_code", DataType.TEXT), col("title_ka", DataType.MEMO), col("title_en", DataType.MEMO));
        goal.addRow("1", "მიზანი 1", "Goal 1");
        Table resource = table(db, "resource", col("resource_code", DataType.TEXT), col("goal_code", DataType.TEXT), col("title_ka", DataType.MEMO), col("title_en", DataType.MEMO), col("uri_ka", DataType.MEMO), col("uri_en", DataType.MEMO));
        resource.addRow("RES-001", "1", "ბავშვების პორტალი", "Children portal", "/ka/goal/1", "/en/goal/1");
        Table observation = table(db, "child_observation", col("source_row_code", DataType.TEXT), col("goal_code", DataType.TEXT), col("reference_year", DataType.LONG), col("age_band_code", DataType.TEXT), col("value", DataType.DOUBLE));
        observation.addRow("OBS-001", "1", 2025, "0-17", 1000d);
    }

    private static void field(Table table, String dataset, String name, String type, String role, boolean required) throws Exception { table.addRow(dataset, name, type, role, Boolean.toString(required)); }
    private static void key(Table table, String dataset, String field, String role, int order) throws Exception { table.addRow(dataset, field, role, Integer.toString(order)); }
    private static void relation(Table table, String code, String fromDataset, String fromField, String toDataset, String toField, String cardinality, boolean required) throws Exception { table.addRow(code, fromDataset, fromField, toDataset, toField, cardinality, Boolean.toString(required)); }
    private static ColumnBuilder col(String name, DataType type) { return new ColumnBuilder(name, type); }
    private static Table table(Database db, String name, ColumnBuilder... columns) throws Exception { TableBuilder builder = new TableBuilder(name); for (ColumnBuilder column : columns) builder.addColumn(column); return builder.toTable(db); }
    private static Table text(Database db, String name, String... columns) throws Exception { TableBuilder builder = new TableBuilder(name); for (String column : columns) builder.addColumn(new ColumnBuilder(column, DataType.TEXT)); return builder.toTable(db); }
}
