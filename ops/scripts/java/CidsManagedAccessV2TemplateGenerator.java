import com.healthmarketscience.jackcess.*;

import java.io.File;

/** Creates a visible reference implementation of the Managed Access Package v2 contract. */
public final class CidsManagedAccessV2TemplateGenerator {
    private CidsManagedAccessV2TemplateGenerator() { }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length == 0 ? "samples/cids-managed-access-package-v2-template.accdb" : args[0]);
        if (out.exists()) throw new IllegalStateException("Refusing to overwrite: " + out.getAbsolutePath());
        File parent = out.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IllegalStateException("Cannot create " + parent);
        try (Database db = DatabaseBuilder.create(Database.FileFormat.V2010, out)) {
            manifest(db);
            contentData(db);
            statisticalData(db);
        }
        System.out.println("Created: " + out.getAbsolutePath());
    }

    private static void manifest(Database db) throws Exception {
        text(db, "__gs_package", "package_code", "package_version", "product_code", "manifest_version", "schema_change_mode", "requested_publication", "source_name", "created_at")
                .addRow("cids-reference", "2.0.0", "cids", "2", "CREATE_ONLY", "REVIEW", "CIDS reference package", "2026-09-08T00:00:00Z");

        Table datasets = text(db, "__gs_dataset", "dataset_code", "access_table_name", "profile_code", "dataset_kind", "target_logical_name", "load_mode", "required", "source_row_key");
        datasets.addRow("CIDS_FILES", "files", "cids-files", "CONTENT", "content_files", "SNAPSHOT_REPLACE", "true", "ID");
        datasets.addRow("CIDS_GLOSSARY", "glossary", "cids-glossary", "CONTENT", "glossary_entries", "SNAPSHOT_REPLACE", "true", "ID");
        datasets.addRow("CIDS_GOALS", "goals", "cids-goals", "CONTENT", "goals", "SNAPSHOT_REPLACE", "true", "ID");
        datasets.addRow("CIDS_GOAL_TITLES", "goal_titles", "cids-goal-titles", "LOOKUP", "goal_titles", "SNAPSHOT_REPLACE", "true", "ID");
        datasets.addRow("CIDS_CATEGORIES", "categories", "cids-categories", "LOOKUP", "categories", "UPSERT", "true", "category");
        datasets.addRow("CIDS_SUBCATEGORIES", "subcategories", "cids-subcategories", "LOOKUP", "subcategories", "UPSERT", "true", "sub_category_code");
        datasets.addRow("CIDS_AGE_GROUPS", "age_groups", "cids-age-groups", "LOOKUP", "age_groups", "UPSERT", "true", "age_group_code");
        datasets.addRow("CIDS_REGIONS", "regions", "cids-regions", "LOOKUP", "regions", "UPSERT", "true", "region_code");
        datasets.addRow("CIDS_SEXES", "sexes", "cids-sexes", "LOOKUP", "sexes", "UPSERT", "true", "sex_code");
        datasets.addRow("CIDS_INDICATORS", "indicators", "cids-indicators", "LOOKUP", "indicators", "UPSERT", "true", "indicator_code");
        datasets.addRow("CIDS_POPULATION", "population_statistics", "cids-population", "STATISTICAL", "observations", "SNAPSHOT_REPLACE", "true", "source_id");

        Table fields = text(db, "__gs_field", "dataset_code", "source_field", "target_field", "logical_type", "role", "nullable", "max_length", "precision", "scale", "label_ka", "label_en", "unit", "is_filterable", "is_groupable", "is_visible");
        fields(fields, "CIDS_FILES", new String[][]{{"ID","file_id","INTEGER","IDENTIFIER","false"},{"category","category_code","INTEGER","DIMENSION","false"},{"sub_category","sub_category_code","STRING","DIMENSION","false"},{"title_geo","title_ka","STRING","CONTENT","false"},{"title_eng","title_en","STRING","CONTENT","false"},{"path_geo","path_ka","STRING","CONTENT","false"},{"path_eng","path_en","STRING","CONTENT","false"},{"chartdata","chart_data_json","JSON","JSON","false"}});
        fields(fields, "CIDS_GLOSSARY", new String[][]{{"ID","glossary_id","INTEGER","IDENTIFIER","false"},{"lang","language_code","STRING","DIMENSION","false"},{"text","body_text","STRING","CONTENT","false"}});
        fields(fields, "CIDS_GOALS", new String[][]{{"ID","goal_id","INTEGER","IDENTIFIER","false"},{"category","category_code","INTEGER","DIMENSION","false"},{"title_geo","title_ka","STRING","CONTENT","false"},{"title_eng","title_en","STRING","CONTENT","false"},{"path_geo","path_ka","STRING","CONTENT","false"},{"path_eng","path_en","STRING","CONTENT","false"}});
        fields(fields, "CIDS_GOAL_TITLES", new String[][]{{"ID","goal_title_id","INTEGER","IDENTIFIER","false"},{"category","category_code","INTEGER","DIMENSION","false"},{"title_geo","title_ka","STRING","LABEL","false"},{"title_eng","title_en","STRING","LABEL","false"}});
        fields(fields, "CIDS_CATEGORIES", new String[][]{{"category","category_code","INTEGER","IDENTIFIER","false"},{"category_key","category_key","STRING","DIMENSION","false"},{"parent_category","parent_category_code","INTEGER","DIMENSION","true"},{"label_ka","label_ka","STRING","LABEL","false"},{"label_en","label_en","STRING","LABEL","false"}});
        fields(fields, "CIDS_SUBCATEGORIES", new String[][]{{"sub_category_code","sub_category_code","STRING","IDENTIFIER","false"},{"category","category_code","INTEGER","DIMENSION","false"},{"label_ka","label_ka","STRING","LABEL","false"},{"label_en","label_en","STRING","LABEL","false"}});
        fields(fields, "CIDS_AGE_GROUPS", new String[][]{{"age_group_code","age_group_code","STRING","IDENTIFIER","false"},{"label_ka","label_ka","STRING","LABEL","false"},{"label_en","label_en","STRING","LABEL","false"},{"age_from","age_from","INTEGER","DIMENSION","true"},{"age_to","age_to","INTEGER","DIMENSION","true"}});
        fields(fields, "CIDS_REGIONS", new String[][]{{"region_code","region_code","STRING","IDENTIFIER","false"},{"parent_region_code","parent_region_code","STRING","DIMENSION","true"},{"label_ka","label_ka","STRING","LABEL","false"},{"label_en","label_en","STRING","LABEL","false"}});
        fields(fields, "CIDS_SEXES", new String[][]{{"sex_code","sex_code","STRING","IDENTIFIER","false"},{"label_ka","label_ka","STRING","LABEL","false"},{"label_en","label_en","STRING","LABEL","false"}});
        fields(fields, "CIDS_INDICATORS", new String[][]{{"indicator_code","indicator_code","STRING","IDENTIFIER","false"},{"title_ka","title_ka","STRING","LABEL","false"},{"title_en","title_en","STRING","LABEL","false"},{"unit_code","unit_code","STRING","LABEL","true"}});
        fields(fields, "CIDS_POPULATION", new String[][]{{"source_id","source_id","INTEGER","IDENTIFIER","false"},{"indicator_code","indicator_code","STRING","DIMENSION","false"},{"observation_year","observation_year","INTEGER","DIMENSION","false"},{"age_group_code","age_group_code","STRING","DIMENSION","false"},{"sex_code","sex_code","STRING","DIMENSION","true"},{"region_code","region_code","STRING","DIMENSION","true"},{"value","value_decimal","DECIMAL","MEASURE","false"}});

        Table keys = text(db, "__gs_key", "dataset_code", "key_name", "key_type", "field_name", "ordinal", "is_enforced");
        key(keys, "CIDS_FILES", "pk_content_files", "PRIMARY", "ID"); key(keys, "CIDS_GLOSSARY", "pk_glossary", "PRIMARY", "ID");
        key(keys, "CIDS_GOALS", "pk_goals", "PRIMARY", "ID"); key(keys, "CIDS_GOAL_TITLES", "pk_goal_titles", "PRIMARY", "ID");
        key(keys, "CIDS_CATEGORIES", "pk_categories", "PRIMARY", "category"); key(keys, "CIDS_SUBCATEGORIES", "pk_subcategories", "PRIMARY", "sub_category_code");
        key(keys, "CIDS_AGE_GROUPS", "pk_age_groups", "PRIMARY", "age_group_code"); key(keys, "CIDS_INDICATORS", "pk_indicators", "PRIMARY", "indicator_code");
        key(keys, "CIDS_REGIONS", "pk_regions", "PRIMARY", "region_code"); key(keys, "CIDS_SEXES", "pk_sexes", "PRIMARY", "sex_code");
        key(keys, "CIDS_POPULATION", "pk_population_source", "PRIMARY", "source_id");

        Table indexes = text(db, "__gs_index", "dataset_code", "index_name", "field_name", "ordinal", "unique_flag");
        index(indexes, "CIDS_POPULATION", "ix_population_chart", "indicator_code", 1, false); index(indexes, "CIDS_POPULATION", "ix_population_chart", "observation_year", 2, false);
        index(indexes, "CIDS_POPULATION", "ix_population_chart", "age_group_code", 3, false); index(indexes, "CIDS_GOALS", "ix_goals_category", "category", 1, false);

        Table relations = text(db, "__gs_relation", "relation_name", "relation_kind", "from_dataset_code", "from_field", "to_dataset_code", "to_field", "cardinality", "required", "enforcement", "load_order");
        relation(relations, "files_category", "FOREIGN_KEY", "CIDS_FILES", "category", "CIDS_CATEGORIES", "category", "MANY_TO_ONE", true, "DATABASE", 10);
        relation(relations, "files_subcategory", "FOREIGN_KEY", "CIDS_FILES", "sub_category", "CIDS_SUBCATEGORIES", "sub_category_code", "MANY_TO_ONE", true, "DATABASE", 20);
        relation(relations, "goals_category", "FOREIGN_KEY", "CIDS_GOALS", "category", "CIDS_CATEGORIES", "category", "MANY_TO_ONE", true, "DATABASE", 10);
        relation(relations, "category_hierarchy", "HIERARCHY", "CIDS_CATEGORIES", "parent_category", "CIDS_CATEGORIES", "category", "MANY_TO_ONE", false, "METADATA", 0);
        relation(relations, "population_indicator", "FOREIGN_KEY", "CIDS_POPULATION", "indicator_code", "CIDS_INDICATORS", "indicator_code", "MANY_TO_ONE", true, "DATABASE", 30);
        relation(relations, "population_age_group", "FOREIGN_KEY", "CIDS_POPULATION", "age_group_code", "CIDS_AGE_GROUPS", "age_group_code", "MANY_TO_ONE", true, "DATABASE", 30);
        relation(relations, "population_region", "FOREIGN_KEY", "CIDS_POPULATION", "region_code", "CIDS_REGIONS", "region_code", "MANY_TO_ONE", false, "DATABASE", 30);
        relation(relations, "population_sex", "FOREIGN_KEY", "CIDS_POPULATION", "sex_code", "CIDS_SEXES", "sex_code", "MANY_TO_ONE", false, "DATABASE", 30);

        Table codelists = text(db, "__gs_codelist", "codelist_code", "dataset_code", "code_field", "parent_code_field", "label_ka_field", "label_en_field", "version");
        codelists.addRow("CIDS_CATEGORY", "CIDS_CATEGORIES", "category", "parent_category", "label_ka", "label_en", "1");
        codelists.addRow("CIDS_SUBCATEGORY", "CIDS_SUBCATEGORIES", "sub_category_code", null, "label_ka", "label_en", "1");
        codelists.addRow("CIDS_AGE_GROUP", "CIDS_AGE_GROUPS", "age_group_code", null, "label_ka", "label_en", "1");
        codelists.addRow("CIDS_REGION", "CIDS_REGIONS", "region_code", "parent_region_code", "label_ka", "label_en", "1");
        codelists.addRow("CIDS_SEX", "CIDS_SEXES", "sex_code", null, "label_ka", "label_en", "1");

        Table rules = text(db, "__gs_validation_rule", "dataset_code", "field_name", "rule_code", "rule_value", "severity");
        rules.addRow("CIDS_POPULATION", "value", "RANGE", "0:999999999999", "ERROR"); rules.addRow("CIDS_POPULATION", "observation_year", "RANGE", "1900:2100", "ERROR");
        rules.addRow("CIDS_POPULATION", "indicator_code", "FOREIGN_KEY_EXISTS", "CIDS_INDICATORS.indicator_code", "ERROR");

        Table charts = text(db, "__gs_chart", "chart_code", "dataset_code", "chart_type", "title_ka", "title_en", "x_field", "y_field", "series_field", "aggregation", "publication_mode");
        charts.addRow("population-by-year", "CIDS_POPULATION", "LINE", "ბავშვების მოსახლეობა წლების მიხედვით", "Child population by year", "observation_year", "value", "age_group_code", "SUM", "DRAFT");
        charts.addRow("population-by-age", "CIDS_POPULATION", "BAR", "მოსახლეობა ასაკობრივი ჯგუფით", "Population by age group", "age_group_code", "value", null, "SUM", "DRAFT");
        charts.addRow("latest-child-population", "CIDS_POPULATION", "KPI", "ბავშვების მოსახლეობა", "Child population", "observation_year", "value", null, "SUM", "DRAFT");
        Table series = text(db, "__gs_chart_series", "chart_code", "field_code", "label_ka", "label_en", "color", "sort_order");
        series.addRow("population-by-year", "AGE_0_17", "0-17", "0-17", "#2563EB", "1"); series.addRow("population-by-year", "AGE_15_24", "15-24", "15-24", "#16A34A", "2");
        Table filters = text(db, "__gs_chart_filter", "chart_code", "field_code", "operator", "value", "sort_order");
        filters.addRow("latest-child-population", "age_group_code", "EQ", "AGE_0_17", "1");
        text(db, "__gs_publication", "requested_status", "requires_steward_review", "notes")
                .addRow("REVIEW", "true", "Reference package: charts publish only after semantic and quality review.");
    }

    private static void contentData(Database db) throws Exception {
        Table files = table(db, "files", col("ID", DataType.LONG), col("category", DataType.LONG), col("sub_category", DataType.TEXT), col("title_geo", DataType.MEMO), col("title_eng", DataType.MEMO), col("path_geo", DataType.MEMO), col("path_eng", DataType.MEMO), col("chartdata", DataType.MEMO));
        files.addRow(1, 1, "population", "ბავშვების მოსახლეობა", "Child population", "/ka/population", "/en/population", "{\"source\":\"CIDS\",\"semantic_status\":\"CURATED\"}");
        Table glossary = table(db, "glossary", col("ID", DataType.LONG), col("lang", DataType.TEXT), col("text", DataType.MEMO));
        glossary.addRow(1, "ka", "ინდიკატორის განმარტება"); glossary.addRow(2, "en", "Indicator definition");
        Table goals = table(db, "goals", col("ID", DataType.LONG), col("category", DataType.LONG), col("title_geo", DataType.MEMO), col("title_eng", DataType.MEMO), col("path_geo", DataType.MEMO), col("path_eng", DataType.MEMO));
        goals.addRow(1, 1, "ჯანმრთელი ბავშვობა", "Healthy childhood", "/ka/goals/1", "/en/goals/1");
        Table titles = table(db, "goal_titles", col("ID", DataType.LONG), col("category", DataType.LONG), col("title_geo", DataType.MEMO), col("title_eng", DataType.MEMO));
        titles.addRow(1, 1, "განვითარების მიზნები", "Development goals");
    }

    private static void statisticalData(Database db) throws Exception {
        Table categories = table(db, "categories", col("category", DataType.LONG), col("category_key", DataType.TEXT), col("parent_category", DataType.LONG), col("label_ka", DataType.MEMO), col("label_en", DataType.MEMO));
        categories.addRow(1, "POPULATION", null, "მოსახლეობა", "Population");
        Table subcategories = table(db, "subcategories", col("sub_category_code", DataType.TEXT), col("category", DataType.LONG), col("label_ka", DataType.MEMO), col("label_en", DataType.MEMO));
        subcategories.addRow("population", 1, "ბავშვების მოსახლეობა", "Child population");
        Table age = table(db, "age_groups", col("age_group_code", DataType.TEXT), col("label_ka", DataType.TEXT), col("label_en", DataType.TEXT), col("age_from", DataType.LONG), col("age_to", DataType.LONG));
        age.addRow("AGE_0_17", "0-17", "0-17", 0, 17); age.addRow("AGE_15_24", "15-24", "15-24", 15, 24);
        Table regions = table(db, "regions", col("region_code", DataType.TEXT), col("parent_region_code", DataType.TEXT), col("label_ka", DataType.MEMO), col("label_en", DataType.MEMO));
        regions.addRow("GE", null, "საქართველო", "Georgia");
        Table sexes = table(db, "sexes", col("sex_code", DataType.TEXT), col("label_ka", DataType.MEMO), col("label_en", DataType.MEMO));
        sexes.addRow("TOTAL", "სულ", "Total");
        Table indicators = table(db, "indicators", col("indicator_code", DataType.TEXT), col("title_ka", DataType.MEMO), col("title_en", DataType.MEMO), col("unit_code", DataType.TEXT));
        indicators.addRow("CHILD_POPULATION", "ბავშვების მოსახლეობა", "Child population", "PERSONS");
        Table facts = table(db, "population_statistics", col("source_id", DataType.LONG), col("indicator_code", DataType.TEXT), col("observation_year", DataType.LONG), col("age_group_code", DataType.TEXT), col("sex_code", DataType.TEXT), col("region_code", DataType.TEXT), col("value", DataType.DOUBLE));
        facts.addRow(1, "CHILD_POPULATION", 2023, "AGE_0_17", "TOTAL", "GE", 900792d);
        facts.addRow(2, "CHILD_POPULATION", 2024, "AGE_0_17", "TOTAL", "GE", 929711d);
        facts.addRow(3, "CHILD_POPULATION", 2023, "AGE_15_24", "TOTAL", "GE", 422890d);
        facts.addRow(4, "CHILD_POPULATION", 2024, "AGE_15_24", "TOTAL", "GE", 466900d);
    }

    private static void fields(Table table, String dataset, String[][] specs) throws Exception {
        for (String[] spec : specs) table.addRow(dataset, spec[0], spec[1], spec[2], spec[3], spec[4], null, null, null, spec[1], spec[1], null, "true", "true", "true");
    }
    private static void key(Table table, String dataset, String name, String type, String field) throws Exception { table.addRow(dataset, name, type, field, "1", "true"); }
    private static void index(Table table, String dataset, String name, String field, int ordinal, boolean unique) throws Exception { table.addRow(dataset, name, field, Integer.toString(ordinal), Boolean.toString(unique)); }
    private static void relation(Table table, String name, String kind, String fromDataset, String fromField, String toDataset, String toField, String cardinality, boolean required, String enforcement, int loadOrder) throws Exception { table.addRow(name, kind, fromDataset, fromField, toDataset, toField, cardinality, Boolean.toString(required), enforcement, Integer.toString(loadOrder)); }
    private static ColumnBuilder col(String name, DataType type) { return new ColumnBuilder(name, type); }
    private static Table table(Database db, String name, ColumnBuilder... columns) throws Exception { TableBuilder b = new TableBuilder(name); for (ColumnBuilder c : columns) b.addColumn(c); return b.toTable(db); }
    private static Table text(Database db, String name, String... columns) throws Exception { TableBuilder b = new TableBuilder(name); for (String c : columns) b.addColumn(new ColumnBuilder(c, DataType.TEXT)); return b.toTable(db); }
}
