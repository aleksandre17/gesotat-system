package org.base.api.service.publication.gate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.base.api.service.artifact.GateResult;
import org.base.api.service.artifact.ReleaseGateEvidenceRepository;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReleaseGateServiceTest {
    private static final String CHECKSUM = "a".repeat(64);
    private static final List<ReleaseGate> GATES = List.of(new PlatformReleaseGates.SchemaValid(), new PlatformReleaseGates.KeysValid(),
            new PlatformReleaseGates.RelationsValid(), new PlatformReleaseGates.ClassifiersValid(), new PlatformReleaseGates.StatisticalSemanticsValid(),
            new PlatformReleaseGates.RawLineageValid(), new PlatformReleaseGates.PublicationAtomic());

    /** A clean ENTITY snapshot like KIDS_RESOURCE snapshot 52: 225 rows, no relations, classifications or observations. */
    static SnapshotFacts entitySnapshot(String status) {
        return new SnapshotFacts(52, 73, status, 127, 225, CHECKSUM, CHECKSUM, 1, 225, 225, 0,
                225, 225, 0, 0, 225, 225, 0, 0, 0, 0, 0, Set.of(), 0, 0, 0, 0);
    }

    /** In-memory evidence store keeping the latest row per gate. */
    static final class MemoryEvidence extends ReleaseGateEvidenceRepository {
        final Map<String, Evidence> latest = new HashMap<>();
        MemoryEvidence() { super(null); }
        @Override public void record(long snapshotId, String gate, GateResult result, String json) { latest.put(gate, new Evidence(result, json)); }
        @Override public Optional<Evidence> latest(long snapshotId, String gate) { return Optional.ofNullable(latest.get(gate)); }
    }

    private static ReleaseGateService service(SnapshotFactsRepository repository, ReleaseGateEvidenceRepository evidence) {
        return new ReleaseGateService(GATES, repository, evidence, new ObjectMapper());
    }

    private static SnapshotFactsRepository repository(SnapshotFacts facts) {
        SnapshotFactsRepository repository = mock(SnapshotFactsRepository.class);
        when(repository.load(52)).thenReturn(Optional.of(facts));
        when(repository.activeClassificationItems(any())).thenReturn(Set.of());
        when(repository.markReviewRequired(52)).thenReturn(true);
        return repository;
    }

    @Test
    void cleanEntitySnapshotPassesAllGatesAndMovesToReview() {
        MemoryEvidence evidence = new MemoryEvidence();
        SnapshotFactsRepository repository = repository(entitySnapshot("SEMANTIC_REVIEW"));
        ReleaseGateService.GateReport report = service(repository, evidence).evaluate(52);
        assertTrue(report.releasable());
        assertEquals("REVIEW_REQUIRED", report.status());
        assertEquals(7, evidence.latest.size());
        assertEquals(Set.of("RELATIONS_VALID", "CLASSIFIERS_VALID", "STATISTICAL_SEMANTICS_VALID"),
                Set.copyOf(report.gates().stream().filter(g -> g.result() == GateResult.NOT_APPLICABLE).map(GateEvaluation::gateCode).toList()));
        assertDoesNotThrow(() -> service(repository, evidence).requireReleasable(52));
    }

    @Test
    void anyFailingGateKeepsSnapshotOutOfReviewAndBlocksPublication() {
        SnapshotFacts duplicateKeys = new SnapshotFacts(52, 73, "SEMANTIC_REVIEW", 127, 225, CHECKSUM, CHECKSUM, 1, 225, 225, 0,
                225, 224, 0, 0, 225, 225, 0, 0, 0, 0, 0, Set.of(), 0, 0, 0, 0);
        MemoryEvidence evidence = new MemoryEvidence();
        SnapshotFactsRepository repository = repository(duplicateKeys);
        ReleaseGateService.GateReport report = service(repository, evidence).evaluate(52);
        assertFalse(report.releasable());
        verify(repository, never()).markReviewRequired(anyLong());
        assertThrows(IllegalStateException.class, () -> service(repository, evidence).requireReleasable(52));
    }

    @Test
    void legacyAssertedEvidenceIsNotAccepted() {
        MemoryEvidence evidence = new MemoryEvidence();
        for (ReleaseGate gate : GATES)
            evidence.record(52, gate.code(), GateResult.PASS, "{\"source\":\"KIDS_R8_ACCEPTANCE\",\"reconciled\":true,\"evidenceVersion\":\"R8\"}");
        assertThrows(IllegalStateException.class, () -> service(repository(entitySnapshot("REVIEW_REQUIRED")), evidence).requireReleasable(52));
    }

    @Test
    void factsDriftAfterEvaluationBlocksPublication() {
        MemoryEvidence evidence = new MemoryEvidence();
        service(repository(entitySnapshot("SEMANTIC_REVIEW")), evidence).evaluate(52);
        SnapshotFacts drifted = new SnapshotFacts(52, 73, "REVIEW_REQUIRED", 127, 225, CHECKSUM, CHECKSUM, 1, 225, 225, 0,
                225, 225, 0, 0, 226, 226, 0, 0, 0, 0, 0, Set.of(), 0, 0, 0, 0);
        assertThrows(IllegalStateException.class, () -> service(repository(drifted), evidence).requireReleasable(52));
    }

    @Test
    void publishedSnapshotIsNotReEvaluated() {
        MemoryEvidence evidence = new MemoryEvidence();
        assertThrows(IllegalStateException.class, () -> service(repository(entitySnapshot("PUBLISHED")), evidence).evaluate(52));
        assertTrue(evidence.latest.isEmpty());
    }

    @Test
    void duplicateGateCodesAreRejectedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new ReleaseGateService(List.of(new PlatformReleaseGates.SchemaValid(), new PlatformReleaseGates.SchemaValid()),
                mock(SnapshotFactsRepository.class), new MemoryEvidence(), new ObjectMapper()));
    }

    @Test
    void recordsOneEvidenceRowPerGate() {
        ReleaseGateEvidenceRepository evidence = mock(ReleaseGateEvidenceRepository.class);
        service(repository(entitySnapshot("SEMANTIC_REVIEW")), evidence).evaluate(52);
        verify(evidence, times(7)).record(eq(52L), anyString(), any(GateResult.class), anyString());
    }
}
