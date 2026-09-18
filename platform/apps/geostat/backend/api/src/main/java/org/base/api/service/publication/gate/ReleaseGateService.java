package org.base.api.service.publication.gate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.artifact.ArtifactNotFoundException;
import org.base.api.service.artifact.GateResult;
import org.base.api.service.artifact.ReleaseGateEvidenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates every registered {@link ReleaseGate} against measured snapshot facts, records the
 * evidence, and moves a fully passing snapshot from SEMANTIC_REVIEW to REVIEW_REQUIRED.
 * Publication accepts only this service's evidence, and only while the facts are unchanged.
 */
@Service
public class ReleaseGateService {
    public static final String EVIDENCE_SCHEMA = "geostat.release-gate.v1";
    static final Set<String> EVALUABLE_STATES = Set.of("SEMANTIC_REVIEW", "REVIEW_REQUIRED");
    private static final Logger log = LoggerFactory.getLogger(ReleaseGateService.class);

    private final List<ReleaseGate> gates;
    private final SnapshotFactsRepository facts;
    private final ReleaseGateEvidenceRepository evidence;
    private final ObjectMapper json;

    public record GateReport(long datasetSnapshotId, String factsDigest, boolean releasable, String status, List<GateEvaluation> gates) {}

    public ReleaseGateService(List<ReleaseGate> gates, SnapshotFactsRepository facts, ReleaseGateEvidenceRepository evidence, ObjectMapper json) {
        this.gates = gates.stream().sorted(Comparator.comparing(ReleaseGate::code)).toList();
        if (this.gates.stream().map(ReleaseGate::code).distinct().count() != this.gates.size())
            throw new IllegalStateException("Release gate codes must be unique");
        this.facts = facts;
        this.evidence = evidence;
        this.json = json;
    }

    public List<String> gateCodes() {
        return gates.stream().map(ReleaseGate::code).toList();
    }

    @Transactional(transactionManager = "dataPlaneTransactionManager")
    public GateReport evaluate(long snapshotId) {
        SnapshotFacts measured = facts.load(snapshotId).orElseThrow(() -> new ArtifactNotFoundException("Snapshot " + snapshotId + " not found"));
        if (!EVALUABLE_STATES.contains(measured.status()))
            throw new IllegalStateException("Snapshot " + snapshotId + " is " + measured.status() + "; gates are evaluated only before publication");
        ReleaseGate.Context context = new ReleaseGate.Context(facts.activeClassificationItems(measured.classificationItemIds()));
        String digest = measured.digest();
        List<GateEvaluation> results = new ArrayList<>(gates.size());
        for (ReleaseGate gate : gates) {
            GateEvaluation result = gate.evaluate(measured, context);
            results.add(result);
            evidence.record(snapshotId, gate.code(), result.result(), evidenceJson(result, measured, digest));
        }
        boolean releasable = results.stream().allMatch(r -> r.result().permitsPublication());
        String status = measured.status();
        if (releasable && "SEMANTIC_REVIEW".equals(status) && facts.markReviewRequired(snapshotId)) status = "REVIEW_REQUIRED";
        log.info("release.gates snapshot={} releasable={} status={} digest={} results={}", snapshotId, releasable, status, digest,
                results.stream().map(r -> r.gateCode() + "=" + r.result()).toList());
        return new GateReport(snapshotId, digest, releasable, status, List.copyOf(results));
    }

    /** Publication guard: the latest evidence of every gate is this evaluator's, permits release, and matches today's facts. */
    public void requireReleasable(long snapshotId) {
        SnapshotFacts measured = facts.load(snapshotId).orElseThrow(() -> new ArtifactNotFoundException("Snapshot " + snapshotId + " not found"));
        String digest = measured.digest();
        for (ReleaseGate gate : gates) {
            ReleaseGateEvidenceRepository.Evidence latest = evidence.latest(snapshotId, gate.code())
                    .orElseThrow(() -> new IllegalStateException("Publication requires evaluated evidence for " + gate.code()));
            JsonNode recorded = read(latest.evidenceJson());
            if (!EVIDENCE_SCHEMA.equals(recorded.path("schema").asText()))
                throw new IllegalStateException(gate.code() + " evidence was not produced by the release-gate evaluator");
            if (!latest.result().permitsPublication())
                throw new IllegalStateException(gate.code() + " is " + latest.result());
            if (!digest.equals(recorded.path("factsDigest").asText()))
                throw new IllegalStateException("Snapshot facts changed after " + gate.code() + " was evaluated; evaluate again");
        }
    }

    private String evidenceJson(GateEvaluation result, SnapshotFacts measured, String digest) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("schema", EVIDENCE_SCHEMA);
        record.put("gate", result.gateCode());
        record.put("result", result.result().name());
        record.put("findings", result.findings());
        record.put("factsDigest", digest);
        record.put("measures", measured.measures());
        try {
            return json.writeValueAsString(record);
        } catch (JsonProcessingException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private JsonNode read(String value) {
        try {
            return json.readTree(value);
        } catch (JsonProcessingException invalid) {
            throw new IllegalStateException("Release gate evidence is not valid JSON", invalid);
        }
    }
}
