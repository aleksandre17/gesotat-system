package org.base.api.service.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Binds a snapshot's rows to an accepted manifest using the approved relation definitions.
 * Planning is pure ({@link ArtifactBindingPlanner}); this service loads state and writes edges.
 * Fail-closed: any ERROR finding means nothing is written.
 */
@Service
public class ArtifactAttachmentService {
    /** Snapshot states in which edges may still be added; later states are immutable. */
    static final Set<String> BINDABLE_SNAPSHOT_STATES = Set.of("REVIEW_REQUIRED", "SEMANTIC_REVIEW");
    private static final Logger log = LoggerFactory.getLogger(ArtifactAttachmentService.class);

    private final ArtifactAttachmentRepository attachments;
    private final ArtifactContractResolver contracts;
    private final ArtifactRegistry registry;
    private final ArtifactProperties properties;
    private final ArtifactMetrics metrics;

    public record RelationSummary(String relationCode, int plannedEdges, int written, int unchanged, int orphanArtifacts) {}

    public record BindingReport(long datasetSnapshotId, long manifestId, boolean dryRun, BindingStatus status, List<RelationSummary> relations,
                                Map<String, Integer> issueCounts, List<ArtifactIssue> issues, boolean issuesTruncated) {}

    public ArtifactAttachmentService(ArtifactAttachmentRepository attachments, ArtifactContractResolver contracts, ArtifactRegistry registry,
                                     ArtifactProperties properties, ArtifactMetrics metrics) {
        this.attachments = attachments;
        this.contracts = contracts;
        this.registry = registry;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public BindingReport bind(long datasetSnapshotId, long manifestId, boolean dryRun) {
        ArtifactAttachmentRepository.SnapshotState snapshot = attachments.snapshot(datasetSnapshotId)
                .orElseThrow(() -> new ArtifactNotFoundException("Snapshot " + datasetSnapshotId + " not found"));
        if (!BINDABLE_SNAPSHOT_STATES.contains(snapshot.status()))
            throw new IllegalStateException("Snapshot " + datasetSnapshotId + " is " + snapshot.status() + "; attachments bind only before publication");
        if (!registry.manifestExists(manifestId)) throw new ArtifactNotFoundException("Manifest " + manifestId + " not found");
        List<ArtifactRelationDefinition> definitions = contracts.approved(snapshot.datasetVersionId());
        if (definitions.isEmpty()) throw new IllegalStateException("Dataset version " + snapshot.datasetVersionId() + " declares no approved artifact relation");

        Map<String, Map<String, Long>> existing = new HashMap<>();
        for (ArtifactRelationDefinition definition : definitions)
            existing.put(definition.relationCode(), attachments.slots(datasetSnapshotId, definition.relationCode()));
        ArtifactBindingPlanner.Plan plan = ArtifactBindingPlanner.plan(definitions, attachments.sourceRows(datasetSnapshotId), registry.entries(manifestId), existing);

        boolean write = !plan.blocked() && !dryRun;
        List<RelationSummary> summaries = new ArrayList<>();
        for (ArtifactBindingPlanner.RelationPlan relation : plan.relations()) {
            int written = 0, unchanged = 0;
            for (ArtifactBindingPlanner.Edge edge : relation.edges()) {
                if (edge.existing()) { unchanged++; continue; }
                if (write) { attachments.insert(datasetSnapshotId, edge.attachment()); written++; }
            }
            summaries.add(new RelationSummary(relation.relationCode(), relation.edges().size(), written, unchanged, relation.orphanCount()));
        }
        BindingStatus status = plan.blocked() ? BindingStatus.BLOCKED : dryRun ? BindingStatus.PREVIEW : BindingStatus.BOUND;
        Map<String, Integer> counts = new TreeMap<>();
        for (ArtifactIssue issue : plan.issues()) counts.merge(issue.code().name(), 1, Integer::sum);
        metrics.binding(status.name());
        log.info("artifact.binding snapshot={} manifest={} status={} relations={} issues={}", datasetSnapshotId, manifestId, status, summaries, counts);
        int limit = properties.getReportIssueLimit();
        return new BindingReport(datasetSnapshotId, manifestId, dryRun, status, summaries, counts,
                List.copyOf(plan.issues().subList(0, Math.min(limit, plan.issues().size()))), plan.issues().size() > limit);
    }
}
