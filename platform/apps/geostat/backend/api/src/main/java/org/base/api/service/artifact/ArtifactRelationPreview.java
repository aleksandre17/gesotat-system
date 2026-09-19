package org.base.api.service.artifact;

import java.util.List;

/**
 * Package-time evaluation of every approved relation of a dataset against the package's own rows and
 * entries (contract §25 "deterministic matching → preview"). Pure: it reuses {@link ArtifactMatcher},
 * the same function that later binds the snapshot, so a preview can never disagree with binding.
 */
public final class ArtifactRelationPreview {

    public record RelationResult(String relationCode, int edgeCount, int orphanCount, List<ArtifactIssue> issues) {
        public RelationResult {
            issues = List.copyOf(issues);
        }
    }

    public record Report(List<RelationResult> relations, List<ArtifactMatcher.Plan> plans) {
        public Report {
            relations = List.copyOf(relations);
            plans = List.copyOf(plans);
        }

        public boolean blocked() {
            return plans.stream().anyMatch(ArtifactMatcher.Plan::blocked);
        }

        public List<ArtifactIssue> errors() {
            return relations.stream().flatMap(r -> r.issues().stream()).filter(i -> i.severity() == ArtifactIssue.Severity.ERROR).toList();
        }
    }

    private ArtifactRelationPreview() {}

    public static Report evaluate(List<ArtifactRelationDefinition> definitions, List<ArtifactMatcher.SourceRow> rows,
                                  List<ArtifactManifest.Entry> entries) {
        List<ArtifactMatcher.Plan> plans = definitions.stream().map(d -> ArtifactMatcher.match(d, rows, entries)).toList();
        List<RelationResult> results = plans.stream()
                .map(p -> new RelationResult(p.relationCode(), p.edges().size(), p.orphanCount(), p.issues())).toList();
        return new Report(results, plans);
    }
}
