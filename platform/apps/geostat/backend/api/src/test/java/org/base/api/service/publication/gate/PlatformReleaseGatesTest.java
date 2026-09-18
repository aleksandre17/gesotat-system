package org.base.api.service.publication.gate;

import org.base.api.service.artifact.GateResult;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Each gate: clean facts pass (or are not applicable), a targeted defect fails. */
class PlatformReleaseGatesTest {
    private static final String A = "a".repeat(64);
    private static final ReleaseGate.Context NONE = new ReleaseGate.Context(Set.of());

    /** Builder over the clean snapshot with one field replaced by index. */
    private static SnapshotFacts with(int field, Object value) {
        Object[] v = {52L, 73L, "SEMANTIC_REVIEW", 127L, 225L, A, A, 1L, 225L, 225L, 0L, 225L, 225L, 0L, 0L, 225L, 225L, 0L, 0L,
                0L, 0L, 0L, Set.of(), 0L, 0L, 0L, 0L};
        v[field] = value;
        @SuppressWarnings("unchecked") Set<Long> items = (Set<Long>) v[22];
        return new SnapshotFacts((long) v[0], (long) v[1], (String) v[2], (long) v[3], (long) v[4], (String) v[5], (String) v[6], (long) v[7],
                (long) v[8], (long) v[9], (long) v[10], (long) v[11], (long) v[12], (long) v[13], (long) v[14], (long) v[15], (long) v[16],
                (long) v[17], (long) v[18], (long) v[19], (long) v[20], (long) v[21], items, (long) v[23], (long) v[24], (long) v[25], (long) v[26]);
    }

    private static GateResult run(ReleaseGate gate, SnapshotFacts facts) {
        return gate.evaluate(facts, NONE).result();
    }

    @Test
    void schemaValid() {
        ReleaseGate g = new PlatformReleaseGates.SchemaValid();
        assertEquals(GateResult.PASS, run(g, with(2, "SEMANTIC_REVIEW")));
        assertEquals(GateResult.FAIL, run(g, with(10, 1L)));
        assertEquals(GateResult.FAIL, run(g, with(4, 224L)));
    }

    @Test
    void keysValid() {
        ReleaseGate g = new PlatformReleaseGates.KeysValid();
        assertEquals(GateResult.FAIL, run(g, with(13, 3L)));
        assertEquals(GateResult.FAIL, run(g, with(16, 200L)));
    }

    @Test
    void relationsValid() {
        ReleaseGate g = new PlatformReleaseGates.RelationsValid();
        assertEquals(GateResult.NOT_APPLICABLE, run(g, with(19, 0L)));
        assertEquals(GateResult.PASS, run(g, with(19, 5L)));
        SnapshotFacts retired = new SnapshotFacts(52, 73, "SEMANTIC_REVIEW", 127, 225, A, A, 1, 225, 225, 0, 225, 225, 0, 0, 225, 225, 0, 0, 5, 1, 0, Set.of(), 0, 0, 0, 0);
        assertEquals(GateResult.FAIL, run(g, retired));
    }

    @Test
    void classifiersValid() {
        ReleaseGate g = new PlatformReleaseGates.ClassifiersValid();
        SnapshotFacts assigned = new SnapshotFacts(52, 73, "SEMANTIC_REVIEW", 127, 225, A, A, 1, 225, 225, 0, 225, 225, 0, 0, 225, 225, 0, 0, 0, 0, 4, Set.of(7L, 8L), 0, 0, 0, 0);
        assertEquals(GateResult.NOT_APPLICABLE, run(g, with(21, 0L)));
        assertEquals(GateResult.PASS, g.evaluate(assigned, new ReleaseGate.Context(Set.of(7L, 8L))).result());
        assertEquals(GateResult.FAIL, g.evaluate(assigned, new ReleaseGate.Context(Set.of(7L))).result());
    }

    @Test
    void statisticalSemanticsValid() {
        ReleaseGate g = new PlatformReleaseGates.StatisticalSemanticsValid();
        assertEquals(GateResult.NOT_APPLICABLE, run(g, with(23, 0L)));
        assertEquals(GateResult.PASS, run(g, with(23, 880L)));
        SnapshotFacts empty = new SnapshotFacts(52, 77, "SEMANTIC_REVIEW", 131, 880, A, A, 1, 880, 880, 0, 880, 880, 0, 0, 0, 0, 0, 0, 0, 0, 0, Set.of(), 880, 0, 0, 3);
        assertEquals(GateResult.FAIL, run(g, empty));
    }

    @Test
    void rawLineageValid() {
        ReleaseGate g = new PlatformReleaseGates.RawLineageValid();
        assertEquals(GateResult.PASS, run(g, with(2, "SEMANTIC_REVIEW")));
        assertEquals(GateResult.FAIL, run(g, with(11, 224L)));
        assertEquals(GateResult.FAIL, run(g, with(14, 1L)));
        assertEquals(GateResult.FAIL, run(g, with(18, 2L)));
    }

    @Test
    void publicationAtomic() {
        ReleaseGate g = new PlatformReleaseGates.PublicationAtomic();
        assertEquals(GateResult.PASS, run(g, with(2, "SEMANTIC_REVIEW")));
        assertEquals(GateResult.FAIL, run(g, with(7, 2L)));
        assertEquals(GateResult.FAIL, run(g, with(6, "b".repeat(64))));
    }
}
