package org.base.api.service.artifact;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure binding plan for one snapshot: matches every declared relation, then checks each planned
 * edge against object verification and the edges already stored. Re-binding must reproduce the
 * stored set exactly; any difference is a conflict, never a silent overwrite or leftover.
 */
public final class ArtifactBindingPlanner {

    /** A planned edge resolved to its artifact version; {@code existing} when the identical edge is already stored. */
    public record Edge(ArtifactAttachmentRepository.NewAttachment attachment, boolean existing) {}

    public record RelationPlan(String relationCode, List<Edge> edges, int orphanCount) {}

    public record Plan(List<RelationPlan> relations, List<ArtifactIssue> issues) {
        public boolean blocked() {
            return issues.stream().anyMatch(i -> i.severity() == ArtifactIssue.Severity.ERROR);
        }
    }

    private ArtifactBindingPlanner() {}

    /**
     * @param existingSlots per relation code, stored edges keyed by {@link ArtifactAttachmentRepository#slot}
     */
    public static Plan plan(List<ArtifactRelationDefinition> definitions, List<ArtifactMatcher.SourceRow> rows,
                            List<ArtifactRegistry.StoredEntry> stored, Map<String, Map<String, Long>> existingSlots) {
        Map<String, ArtifactRegistry.StoredEntry> byPath = new HashMap<>();
        for (ArtifactRegistry.StoredEntry entry : stored) byPath.put(entry.entry().originalPath(), entry);
        List<ArtifactManifest.Entry> entries = stored.stream().map(ArtifactRegistry.StoredEntry::entry).toList();
        List<ArtifactIssue> issues = new ArrayList<>();
        List<RelationPlan> relations = new ArrayList<>();
        for (ArtifactRelationDefinition definition : definitions) {
            ArtifactMatcher.Plan match = ArtifactMatcher.match(definition, rows, entries);
            issues.addAll(match.issues());
            Map<String, Long> existing = existingSlots.getOrDefault(definition.relationCode(), Map.of());
            Set<String> planned = new HashSet<>();
            List<Edge> edges = new ArrayList<>();
            for (ArtifactMatcher.PlannedEdge edge : match.edges()) {
                ArtifactRegistry.StoredEntry target = byPath.get(edge.entry().originalPath());
                String subject = edge.externalKey() + "/" + definition.relationCode() + "/" + edge.language() + "/" + edge.ordinal();
                if (target.verificationStatus().issue() != null)
                    issues.add(ArtifactIssue.error(target.verificationStatus().issue(), subject, edge.entry().originalPath()));
                String slot = ArtifactAttachmentRepository.slot(edge.entityId(), edge.language(), edge.ordinal());
                planned.add(slot);
                Long bound = existing.get(slot);
                if (bound != null && bound != target.artifactVersionId())
                    issues.add(ArtifactIssue.error(ArtifactIssue.Code.SLOT_CONFLICT, subject, "Slot already bound to artifact version " + bound));
                edges.add(new Edge(new ArtifactAttachmentRepository.NewAttachment(edge.entityId(), definition.relationCode(), definition.artifactRole(),
                        edge.language(), edge.ordinal(), target.artifactVersionId(), edge.sourceRecordId()), bound != null));
            }
            for (String slot : existing.keySet())
                if (!planned.contains(slot))
                    issues.add(ArtifactIssue.error(ArtifactIssue.Code.SLOT_CONFLICT, definition.relationCode() + "/" + slot, "Stored attachment is not produced by this manifest"));
            relations.add(new RelationPlan(definition.relationCode(), List.copyOf(edges), match.orphanCount()));
        }
        return new Plan(List.copyOf(relations), List.copyOf(issues));
    }
}
