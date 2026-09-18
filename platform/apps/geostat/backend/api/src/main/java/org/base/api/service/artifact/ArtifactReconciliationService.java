package org.base.api.service.artifact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Release gate {@value #GATE_CODE}. Evaluation is delegated to {@link ArtifactReconciler}; this
 * service loads state, records evidence and guards publication.
 */
@Service
public class ArtifactReconciliationService {
    public static final String GATE_CODE = "ARTIFACT_RECONCILIATION";
    public static final String EVIDENCE_SCHEMA = "geostat.artifact-reconciliation.v1";
    private static final Logger log = LoggerFactory.getLogger(ArtifactReconciliationService.class);

    private final ArtifactAttachmentRepository attachments;
    private final ReleaseGateEvidenceRepository evidence;
    private final ArtifactContractResolver contracts;
    private final ArtifactProperties properties;
    private final ArtifactMetrics metrics;
    private final ObjectMapper json;

    public record ReconciliationReport(long datasetSnapshotId, GateResult result, String attachmentChecksum, int entities, int attachments,
                                       Map<String, Map<String, Integer>> attachedBySlot, Map<String, Integer> issueCounts, List<ArtifactIssue> issues) {}

    public ArtifactReconciliationService(ArtifactAttachmentRepository attachments, ReleaseGateEvidenceRepository evidence, ArtifactContractResolver contracts,
                                         ArtifactProperties properties, ArtifactMetrics metrics, ObjectMapper json) {
        this.attachments = attachments;
        this.evidence = evidence;
        this.contracts = contracts;
        this.properties = properties;
        this.metrics = metrics;
        this.json = json;
    }

    public ReconciliationReport reconcile(long datasetSnapshotId) {
        long datasetVersionId = attachments.snapshot(datasetSnapshotId)
                .orElseThrow(() -> new ArtifactNotFoundException("Snapshot " + datasetSnapshotId + " not found")).datasetVersionId();
        List<ArtifactRelationDefinition> definitions = contracts.forReconciliation(datasetVersionId);
        if (definitions.isEmpty()) throw new IllegalStateException("Dataset version " + datasetVersionId + " declares no approved artifact relation");
        List<ArtifactReconciler.SlotCounts> counts = new ArrayList<>();
        for (ArtifactRelationDefinition definition : definitions)
            for (ArtifactMatchRule.Binding binding : definition.matchRule().bindings())
                counts.add(new ArtifactReconciler.SlotCounts(definition.relationCode(), binding.language(), binding.required(),
                        attachments.countsPerEntity(datasetSnapshotId, definition.relationCode(), binding.language())));
        List<ArtifactAttachmentRepository.AttachedObject> attached = attachments.attachedObjects(datasetSnapshotId);
        ArtifactReconciler.Result result = ArtifactReconciler.evaluate(definitions, counts, attached);

        Map<String, Integer> issueCounts = new TreeMap<>();
        for (ArtifactIssue issue : result.issues()) issueCounts.merge(issue.code().name(), 1, Integer::sum);
        int entities = attachments.entityCount(datasetSnapshotId);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("schema", EVIDENCE_SCHEMA);
        record.put("datasetVersionId", datasetVersionId);
        record.put("attachmentChecksum", result.attachmentChecksum());
        record.put("entities", entities);
        record.put("attachments", attached.size());
        record.put("attachedBySlot", result.attachedBySlot());
        record.put("issueCounts", issueCounts);
        evidence.record(datasetSnapshotId, GATE_CODE, result.result(), toJson(record));
        metrics.reconciliation(result.result().name());
        log.info("artifact.reconciliation snapshot={} result={} entities={} attachments={} checksum={} issues={}", datasetSnapshotId,
                result.result(), entities, attached.size(), result.attachmentChecksum(), issueCounts);
        List<ArtifactIssue> issues = result.issues();
        return new ReconciliationReport(datasetSnapshotId, result.result(), result.attachmentChecksum(), entities, attached.size(),
                result.attachedBySlot(), issueCounts, List.copyOf(issues.subList(0, Math.min(properties.getReportIssueLimit(), issues.size()))));
    }

    /**
     * Publication guard. Datasets without declared artifact relations are unaffected; declared ones
     * need a latest PASS whose checksum still equals the current attachment set.
     */
    public void requirePassIfDeclared(long datasetSnapshotId, long datasetVersionId) {
        if (contracts.approved(datasetVersionId).isEmpty()) return;
        ReleaseGateEvidenceRepository.Evidence latest = evidence.latest(datasetSnapshotId, GATE_CODE)
                .filter(e -> e.result() == GateResult.PASS)
                .orElseThrow(() -> new IllegalStateException("Publication requires PASS evidence for " + GATE_CODE));
        String recorded;
        try {
            recorded = json.readTree(latest.evidenceJson()).path("attachmentChecksum").asText();
        } catch (java.io.IOException invalid) {
            throw new IllegalStateException(GATE_CODE + " evidence is not valid JSON", invalid);
        }
        if (!ArtifactReconciler.checksum(attachments.attachedObjects(datasetSnapshotId)).equals(recorded))
            throw new IllegalStateException("Attachments changed after " + GATE_CODE + "; reconcile again before publication");
    }

    private String toJson(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
