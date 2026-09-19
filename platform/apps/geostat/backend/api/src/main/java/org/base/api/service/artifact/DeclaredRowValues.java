package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Supplies row values that a package declares outside the row payload (see {@link PackageDeclaredValues}).
 * The matcher stays unchanged: the values arrive as resolved package paths in the rule's synthetic field, in
 * ordinal order. Two sources produce the same shape - the package tables at admission and the accepted
 * manifest document at snapshot binding - so preview and binding cannot disagree.
 */
public final class DeclaredRowValues {

    /** relation code → row key → language → package paths in ordinal order. */
    public record Values(Map<String, Map<String, Map<String, List<String>>>> byRelation) {
        public static Values none() {
            return new Values(Map.of());
        }
    }

    private DeclaredRowValues() {}

    /** Admission: every package-declared rule reads its own edges through the carrier's table capability. */
    public static Values fromPackage(List<ArtifactRelationDefinition> definitions, PackageDatasetCarrier carrier, File datasetFile) throws IOException {
        Map<String, Map<String, Map<String, List<String>>>> byRelation = new HashMap<>();
        for (ArtifactRelationDefinition definition : definitions) {
            if (!(definition.matchRule() instanceof PackageDeclaredValues declared)) continue;
            if (!(carrier instanceof PackageTableReader tables))
                throw new IllegalArgumentException("Relation " + definition.relationCode() + " declares its files in package tables, which this dataset format cannot supply");
            byRelation.put(definition.relationCode(), group(declared.declaredEdges(definition.relationCode(), tables, datasetFile)));
        }
        return new Values(byRelation);
    }

    /** Snapshot binding: the edges accepted at admission, replayed for the relations that need them. */
    public static Values fromDocument(List<ArtifactRelationDefinition> definitions, PackageManifestDocument document) {
        Map<String, Map<String, Map<String, List<String>>>> byRelation = new HashMap<>();
        for (ArtifactRelationDefinition definition : definitions) {
            if (!(definition.matchRule() instanceof PackageDeclaredValues)) continue;
            List<PackageDeclaredValues.DeclaredEdge> edges = document.edges().stream().filter(edge -> edge.relationCode().equals(definition.relationCode()))
                    .map(edge -> new PackageDeclaredValues.DeclaredEdge(edge.rowKey(), edge.language(), edge.ordinal(), edge.path())).toList();
            byRelation.put(definition.relationCode(), group(edges));
        }
        return new Values(byRelation);
    }

    public static boolean needed(List<ArtifactRelationDefinition> definitions) {
        return definitions.stream().anyMatch(definition -> definition.matchRule() instanceof PackageDeclaredValues);
    }

    /** Copies of the rows with each package-declared rule's synthetic fields filled in; other rows and fields are untouched. */
    public static List<ArtifactMatcher.SourceRow> apply(List<ArtifactRelationDefinition> definitions, List<ArtifactMatcher.SourceRow> rows, Values values) {
        if (!needed(definitions)) return rows;
        List<ArtifactMatcher.SourceRow> enriched = new ArrayList<>(rows.size());
        for (ArtifactMatcher.SourceRow row : rows) {
            ObjectNode payload = row.payload().deepCopy();
            for (ArtifactRelationDefinition definition : definitions) {
                if (!(definition.matchRule() instanceof PackageDeclaredValues declared)) continue;
                var byLanguage = values.byRelation().getOrDefault(definition.relationCode(), Map.of()).getOrDefault(row.externalKey(), Map.of());
                for (String language : declared.languages()) {
                    ArrayNode paths = payload.putArray(declared.valueField(language));
                    byLanguage.getOrDefault(language, List.of()).forEach(paths::add);
                }
            }
            enriched.add(new ArtifactMatcher.SourceRow(row.entityId(), row.externalKey(), row.sourceRecordId(), payload));
        }
        return enriched;
    }

    /** Ordinal order per row and language; two edges on one ordinal are a declaration error, never a silent pick. */
    private static Map<String, Map<String, List<String>>> group(List<PackageDeclaredValues.DeclaredEdge> edges) {
        Map<String, Map<String, TreeMap<Long, String>>> ordered = new HashMap<>();
        for (PackageDeclaredValues.DeclaredEdge edge : edges) {
            String previous = ordered.computeIfAbsent(edge.rowKey(), key -> new HashMap<>()).computeIfAbsent(edge.language(), key -> new TreeMap<>())
                    .put(edge.ordinal(), edge.path());
            if (previous != null && !Objects.equals(previous, edge.path()))
                throw new IllegalArgumentException("Row " + edge.rowKey() + " declares two files for ordinal " + edge.ordinal() + " (" + edge.language() + ")");
        }
        Map<String, Map<String, List<String>>> grouped = new HashMap<>();
        ordered.forEach((rowKey, byLanguage) -> byLanguage.forEach((language, byOrdinal) -> {
            // The stored ordinal is the position; a gap would silently renumber the declared order.
            if (byOrdinal.lastKey() != byOrdinal.size())
                throw new IllegalArgumentException("Row " + rowKey + " (" + language + ") must declare ordinals 1.." + byOrdinal.size() + " without gaps");
            grouped.computeIfAbsent(rowKey, key -> new HashMap<>()).put(language, List.copyOf(byOrdinal.values()));
        }));
        return grouped;
    }

    /** Dataset rows as the matcher must see them at admission: payload rows plus every package-declared value. */
    public static List<ArtifactMatcher.SourceRow> packageRows(List<ArtifactRelationDefinition> definitions, PackageDatasetCarrier carrier, File datasetFile,
                                                              ArtifactPackageContractResolver.DatasetContract contract) throws IOException {
        List<ArtifactMatcher.SourceRow> rows = carrier.rows(datasetFile, contract);
        if (!needed(definitions)) return rows;
        Values values = fromPackage(definitions, carrier, datasetFile);
        List<String> unknown = unknownRowKeys(values, rows);
        if (!unknown.isEmpty())
            throw new IllegalArgumentException(unknown.size() + " declared attachment row key(s) name no dataset row; first: " + unknown.subList(0, Math.min(10, unknown.size())));
        return apply(definitions, rows, values);
    }

    /** Rows of a relation whose entity key names no dataset row; reported by the caller as a blocking issue. */
    public static List<String> unknownRowKeys(Values values, List<ArtifactMatcher.SourceRow> rows) {
        java.util.Set<String> known = new java.util.HashSet<>();
        rows.forEach(row -> known.add(row.externalKey()));
        return values.byRelation().values().stream().flatMap(byRow -> byRow.keySet().stream()).filter(key -> !known.contains(key))
                .distinct().sorted(Comparator.naturalOrder()).toList();
    }
}
