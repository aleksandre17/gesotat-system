package org.base.api.service.platform.statistical.access;

import com.healthmarketscience.jackcess.ColumnBuilder;
import com.healthmarketscience.jackcess.DataType;
import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.IndexBuilder;
import com.healthmarketscience.jackcess.PropertyMap;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;
import com.healthmarketscience.jackcess.TableBuilder;
import org.base.api.service.platform.statistical.model.Component;
import org.base.api.service.platform.statistical.model.Ref;
import org.base.api.service.platform.statistical.model.Representation;
import org.base.api.service.platform.statistical.plan.ProviderCapabilities;
import org.base.api.service.platform.statistical.plan.SemanticPlan;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PhysicalColumn;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PhysicalTable;
import org.base.api.service.platform.statistical.plan.SemanticPlan.PlannedComponent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Access (ACCDB) authoring adapter. Emits the typed tables of an approved plan and reads a filled file back
 * as component-keyed rows. Exact measures use the Access Decimal type, never Double. The embedded contract
 * stamp is a transport copy: a file whose stamp differs from the approved plan is refused, so edited
 * metadata can never become authority.
 */
public final class AccessAuthoringAdapter {
    public static final String SOURCE_PROFILE = "ACCESS_ACCDB";
    public static final String STAMP_TABLE = "__stat_contract";
    /**
     * ACCDB 2010 format: opened by Access 2010 and every later build, and the format of every other generator in
     * this repository. The 2016 format flag is refused by Access 2016 release builds ("requires a newer version").
     */
    public static final Database.FileFormat FILE_FORMAT = Database.FileFormat.V2010;
    private static final short COMBO_BOX = 111;

    public record CodeItem(String code, String label) { }

    public enum ReadIssue { CONTRACT_STAMP_MISSING, CONTRACT_STAMP_MISMATCH, TABLE_MISSING, COLUMN_MISSING, SPLIT_PART_INCOMPLETE }

    public record ReadResult(List<Map<String, Object>> rows, List<String> issues) {
        public boolean accepted() { return issues.isEmpty(); }
    }

    /** The adapter's own declaration of what an ACCDB can hold; the capability registry stores this declaration. */
    public static ProviderCapabilities capabilities() {
        return new ProviderCapabilities(SOURCE_PROFILE, 255, 64, 10, 28, true, Set.of(
                "ADD", "ALL", "ALTER", "AND", "ANY", "AS", "ASC", "BETWEEN", "BY", "COLUMN", "COUNT", "CREATE", "CURRENCY", "DATE",
                "DELETE", "DESC", "DISTINCT", "DROP", "EXISTS", "FROM", "GROUP", "HAVING", "IN", "INDEX", "INSERT", "INTO", "IS",
                "JOIN", "KEY", "LEVEL", "LIKE", "MEMO", "NAME", "NOT", "NULL", "NUMBER", "ON", "OPTION", "OR", "ORDER", "PERCENT",
                "SELECT", "SET", "TABLE", "TEXT", "TIME", "TOP", "UNION", "UPDATE", "USER", "VALUE", "VALUES", "WHERE", "YEAR", "YES", "NO"));
    }

    public static ProviderCapabilities.Catalog catalog() {
        return profile -> SOURCE_PROFILE.equals(profile) ? Optional.of(capabilities()) : Optional.empty();
    }

    public void emit(SemanticPlan plan, Map<Ref, List<CodeItem>> codelists, String captionLanguage, File target) throws IOException {
        if (!SOURCE_PROFILE.equals(plan.physical().providerCode())) throw new IllegalArgumentException("plan was not compiled for " + SOURCE_PROFILE);
        try (Database db = DatabaseBuilder.create(FILE_FORMAT, target)) {
            Map<Ref, String> lookupTables = new LinkedHashMap<>();
            for (PlannedComponent c : plan.components())
                if (c.isAuthoringColumn() && c.representation() instanceof Representation.Coded coded && !lookupTables.containsKey(coded.codelistRef()))
                    lookupTables.put(coded.codelistRef(), codelistTable(db, coded.codelistRef(), codelists, lookupTables.size()));
            for (PhysicalTable table : plan.physical().tables()) dataTable(db, plan, table, lookupTables, captionLanguage);
            stamp(db, plan);
        }
    }

    private String codelistTable(Database db, Ref ref, Map<Ref, List<CodeItem>> codelists, int ordinal) throws IOException {
        List<CodeItem> items = codelists.get(ref);
        if (items == null) throw new IllegalArgumentException("codelist content is required for " + ref);
        String name = "__cl_" + (ordinal + 1) + "_" + ref.code().substring(0, Math.min(40, ref.code().length()));
        Table table = new TableBuilder(name)
                .addColumn(new ColumnBuilder("code", DataType.TEXT).setLengthInUnits(120))
                .addColumn(new ColumnBuilder("label", DataType.TEXT).setLengthInUnits(255))
                .addIndex(new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME).addColumns("code").setPrimaryKey())
                .putProperty(PropertyMap.DESCRIPTION_PROP, ref.wire())
                .toTable(db);
        for (CodeItem item : items) table.addRow(item.code(), item.label());
        return name;
    }

    private void dataTable(Database db, SemanticPlan plan, PhysicalTable physical, Map<Ref, String> lookups, String language) throws IOException {
        TableBuilder builder = new TableBuilder(physical.name());
        List<String> key = new ArrayList<>();
        for (PhysicalColumn column : physical.columns()) {
            if (column.componentCode() == null) {
                builder.addColumn(new ColumnBuilder(column.name(), DataType.LONG).setAutoNumber(true));
                builder.addIndex(new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME).addColumns(column.name()).setPrimaryKey());
                continue;
            }
            PlannedComponent c = plan.component(column.componentCode());
            ColumnBuilder col = typed(column.name(), c.representation());
            String caption = plan.captions().getOrDefault(c.code(), Map.of()).get(language);
            if (caption != null) col.putProperty(PropertyMap.CAPTION_PROP, caption);
            col.putProperty(PropertyMap.DESCRIPTION_PROP, c.code());
            if (c.role() != Component.Role.DIMENSION) col.putProperty(PropertyMap.REQUIRED_PROP, false);
            if (c.representation() instanceof Representation.Coded coded) lookup(col, lookups.get(coded.codelistRef()));
            builder.addColumn(col);
            if (c.role() == Component.Role.DIMENSION) key.add(column.name());
        }
        if (!physical.generatedRowRef())
            builder.addIndex(new IndexBuilder("uq_observation_key").addColumns(key.toArray(String[]::new)).setUnique());
        builder.toTable(db);
    }

    private static ColumnBuilder typed(String name, Representation r) {
        if (r instanceof Representation.Numeric n)
            return n.approximate() ? new ColumnBuilder(name, DataType.DOUBLE)
                    : new ColumnBuilder(name, DataType.NUMERIC).setPrecision(n.precision()).setScale(n.scale());
        if (r instanceof Representation.IntegerRange range)
            return range.min() >= Integer.MIN_VALUE && range.max() <= Integer.MAX_VALUE ? new ColumnBuilder(name, DataType.LONG)
                    : new ColumnBuilder(name, DataType.NUMERIC).setPrecision(19).setScale(0);
        int length = r instanceof Representation.BoundedText text ? text.maxLength() : r instanceof Representation.TimePeriod ? 32 : 120;
        return new ColumnBuilder(name, DataType.TEXT).setLengthInUnits(length);
    }

    /** Stores the stable code, shows code and label; declarative field properties only, no VBA or macro. */
    private static void lookup(ColumnBuilder col, String lookupTable) {
        col.putProperty("DisplayControl", DataType.INT, COMBO_BOX);
        col.putProperty("RowSourceType", DataType.TEXT, "Table/Query");
        col.putProperty("RowSource", DataType.MEMO, "SELECT [code], [label] FROM [" + lookupTable + "] ORDER BY [code]");
        col.putProperty("BoundColumn", DataType.INT, (short) 1);
        col.putProperty("ColumnCount", DataType.INT, (short) 2);
        col.putProperty("LimitToList", DataType.BOOLEAN, true);
    }

    private static void stamp(Database db, SemanticPlan plan) throws IOException {
        Table table = new TableBuilder(STAMP_TABLE)
                .addColumn(new ColumnBuilder("stamp_key", DataType.TEXT).setLengthInUnits(64))
                .addColumn(new ColumnBuilder("stamp_value", DataType.MEMO))
                .addIndex(new IndexBuilder(IndexBuilder.PRIMARY_KEY_NAME).addColumns("stamp_key").setPrimaryKey())
                .toTable(db);
        for (Map.Entry<String, String> e : stampOf(plan).entrySet()) table.addRow(e.getKey(), e.getValue());
    }

    private static Map<String, String> stampOf(SemanticPlan plan) {
        Map<String, String> stamp = new TreeMap<>();
        stamp.put("profile", plan.profileRef().wire());
        stamp.put("dataset", plan.datasetNamespace() + ":" + plan.datasetCode());
        stamp.put("structure", plan.structureRef().wire());
        stamp.put("semanticDigest", plan.semanticDigest());
        stamp.put("revisionDigest", plan.revisionDigest());
        return stamp;
    }

    public ReadResult read(File filled, SemanticPlan plan) throws IOException {
        List<String> issues = new ArrayList<>();
        try (Database db = new DatabaseBuilder(filled).setReadOnly(true).open()) {
            Table stampTable = db.getTableNames().contains(STAMP_TABLE) ? db.getTable(STAMP_TABLE) : null;
            if (stampTable == null) return new ReadResult(List.of(), List.of(ReadIssue.CONTRACT_STAMP_MISSING.name()));
            Map<String, String> found = new TreeMap<>();
            for (Row row : stampTable) found.put(row.getString("stamp_key"), row.getString("stamp_value"));
            if (!found.equals(stampOf(plan))) return new ReadResult(List.of(), List.of(ReadIssue.CONTRACT_STAMP_MISMATCH.name()));

            List<String> keyCodes = plan.components().stream()
                    .filter(c -> c.role() == Component.Role.DIMENSION && c.isAuthoringColumn()).map(PlannedComponent::code).toList();
            Map<List<Object>, Map<String, Object>> joined = new LinkedHashMap<>();
            Map<List<Object>, Integer> partsSeen = new LinkedHashMap<>();
            List<Map<String, Object>> single = new ArrayList<>();
            for (PhysicalTable physical : plan.physical().tables()) {
                if (!db.getTableNames().contains(physical.name())) { issues.add(ReadIssue.TABLE_MISSING + ":" + physical.name()); continue; }
                Table table = db.getTable(physical.name());
                for (PhysicalColumn column : physical.columns())
                    if (table.getColumns().stream().noneMatch(c -> c.getName().equals(column.name()))) issues.add(ReadIssue.COLUMN_MISSING + ":" + physical.name() + "." + column.name());
                if (!issues.isEmpty()) continue;
                for (Row row : table) {
                    Map<String, Object> values = new LinkedHashMap<>();
                    for (PhysicalColumn column : physical.columns())
                        if (column.componentCode() != null) values.put(column.componentCode(), row.get(column.name()));
                    if (plan.physical().tables().size() == 1) { single.add(values); continue; } // duplicates are the normalizer's finding
                    List<Object> key = keyCodes.stream().map(code -> (Object) String.valueOf(values.get(code))).toList();
                    joined.computeIfAbsent(key, k -> new LinkedHashMap<>()).putAll(values);
                    partsSeen.merge(key, 1, Integer::sum);
                }
            }
            int parts = plan.physical().tables().size();
            partsSeen.forEach((key, seen) -> { if (seen != parts) issues.add(ReadIssue.SPLIT_PART_INCOMPLETE + ":" + key); });
            if (!issues.isEmpty()) return new ReadResult(List.of(), List.copyOf(issues));
            return new ReadResult(parts == 1 ? List.copyOf(single) : List.copyOf(joined.values()), List.of());
        }
    }
}
