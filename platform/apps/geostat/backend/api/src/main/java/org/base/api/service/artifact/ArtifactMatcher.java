package org.base.api.service.artifact;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic row -> artifact matching for one declared relation. Pure function: the result
 * does not depend on input order and never picks between candidates; anything uncertain is an issue.
 */
public final class ArtifactMatcher {
    private static final int MAX_LISTED_ORPHANS = 50;

    /** Materialized entity row with its source payload. */
    public record SourceRow(long entityId, String externalKey, long sourceRecordId, JsonNode payload) {}

    /** One attachment to write. */
    public record PlannedEdge(long entityId, String externalKey, long sourceRecordId, String language, int ordinal, ArtifactManifest.Entry entry) {}

    public record Plan(String relationCode, List<PlannedEdge> edges, List<ArtifactIssue> issues, int orphanCount) {
        public boolean blocked() {
            return issues.stream().anyMatch(i -> i.severity() == ArtifactIssue.Severity.ERROR);
        }
    }

    private ArtifactMatcher() {}

    public static Plan match(ArtifactRelationDefinition definition, List<SourceRow> rows, List<ArtifactManifest.Entry> entries) {
        Map<String, ArtifactManifest.Entry> byPath = new HashMap<>();
        Map<String, List<String>> byFoldedPath = new HashMap<>();
        for (ArtifactManifest.Entry entry : entries) {
            byPath.put(entry.originalPath(), entry);
            byFoldedPath.computeIfAbsent(entry.originalPath().toLowerCase(Locale.ROOT), k -> new ArrayList<>()).add(entry.originalPath());
        }
        ArtifactMatchRule rule = definition.matchRule();
        List<PlannedEdge> edges = new ArrayList<>();
        List<ArtifactIssue> issues = new ArrayList<>();
        Set<String> referenced = new HashSet<>();
        List<SourceRow> ordered = rows.stream().sorted(Comparator.comparing(SourceRow::externalKey)).toList();
        for (SourceRow row : ordered) {
            for (ArtifactMatchRule.Binding binding : rule.bindings()) {
                String subject = row.externalKey() + "/" + definition.relationCode() + "/" + binding.language();
                List<String> values = new ArrayList<>(sourceValues(row.payload().path(binding.field())));
                if (!definition.ordered())
                    values.sort(Comparator.comparing(value -> canonicalSortKey(rule, value)));
                if (values.isEmpty()) {
                    if (binding.required() && definition.minPerRow() > 0)
                        issues.add(ArtifactIssue.error(ArtifactIssue.Code.MISSING_SOURCE_VALUE, subject, "Required field " + binding.field() + " is empty"));
                    continue;
                }
                if (values.size() < definition.minPerRow() || (definition.maxPerRow() != null && values.size() > definition.maxPerRow()))
                    issues.add(ArtifactIssue.error(ArtifactIssue.Code.CARDINALITY_VIOLATION, subject,
                            values.size() + " values, declared " + definition.minPerRow() + ".." + (definition.maxPerRow() == null ? "n" : definition.maxPerRow())));
                /* Source order is the ordinal: array position for 1:N, 1 for a scalar. */
                for (int index = 0; index < values.size(); index++) {
                    String value = values.get(index);
                    String slot = values.size() == 1 ? subject : subject + "#" + (index + 1);
                    String path;
                    try {
                        path = rule.resolve(value);
                    } catch (IllegalArgumentException unsafe) {
                        issues.add(ArtifactIssue.error(ArtifactIssue.Code.UNMATCHED_ROW, slot, unsafe.getMessage()));
                        continue;
                    }
                    ArtifactManifest.Entry entry = path == null ? null : byPath.get(path);
                    if (entry == null) {
                        List<String> folded = path == null ? List.of() : byFoldedPath.getOrDefault(path.toLowerCase(Locale.ROOT), List.of());
                        if (!folded.isEmpty())
                            issues.add(ArtifactIssue.error(ArtifactIssue.Code.CASE_MISMATCH, slot, "Only case-insensitive candidates exist: " + folded));
                        else
                            issues.add(ArtifactIssue.error(ArtifactIssue.Code.UNMATCHED_ROW, slot, "No package entry for " + (path == null ? value : path)));
                        continue;
                    }
                    referenced.add(entry.originalPath());
                    issues.addAll(definition.policy().evaluate(slot, entry.mediaType(), entry.byteSize()));
                    edges.add(new PlannedEdge(row.entityId(), row.externalKey(), row.sourceRecordId(), binding.language(), index + 1, entry));
                }
            }
        }
        List<String> orphans = entries.stream().map(ArtifactManifest.Entry::originalPath)
                .filter(p -> rule.covers(p) && !referenced.contains(p)).sorted().toList();
        if (!orphans.isEmpty()) {
            issues.add(ArtifactIssue.warning(ArtifactIssue.Code.ORPHAN_ARTIFACT, definition.relationCode(),
                    orphans.size() + " package entries in " + rule.type() + " scope are not referenced; first: "
                            + orphans.subList(0, Math.min(MAX_LISTED_ORPHANS, orphans.size()))));
        }
        return new Plan(definition.relationCode(), List.copyOf(edges), List.copyOf(issues), orphans.size());
    }

    /** A scalar field yields one value; a JSON array yields its non-blank elements in source order. */
    public static List<String> sourceValues(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) return List.of();
        if (!node.isArray()) return node.asText().isBlank() ? List.of() : List.of(node.asText());
        List<String> values = new ArrayList<>();
        for (JsonNode element : node) if (!element.isNull() && !element.asText().isBlank()) values.add(element.asText());
        return values;
    }

    private static String canonicalSortKey(ArtifactMatchRule rule, String value) {
        try {
            String resolved = rule.resolve(value);
            return resolved == null ? "\u0001" + value : "\u0000" + resolved;
        } catch (IllegalArgumentException invalid) {
            return "\u0002" + value;
        }
    }
}
