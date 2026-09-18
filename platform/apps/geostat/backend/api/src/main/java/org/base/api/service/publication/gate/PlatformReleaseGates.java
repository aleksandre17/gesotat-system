package org.base.api.service.publication.gate;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** The platform's release gates. Each is a small rule over {@link SnapshotFacts}, testable on its own. */
public final class PlatformReleaseGates {
    private PlatformReleaseGates() {}

    /** Every staged row passed technical validation and exactly those rows form the snapshot. */
    @Component
    public static class SchemaValid implements ReleaseGate {
        public String code() { return "SCHEMA_VALID"; }

        public GateEvaluation evaluate(SnapshotFacts f, Context c) {
            List<String> x = new ArrayList<>();
            if (f.stagedRejected() > 0) x.add(f.stagedRejected() + " staged rows were rejected");
            if (f.stagedValid() != f.stagedTotal()) x.add((f.stagedTotal() - f.stagedValid()) + " staged rows are not VALID");
            if (f.stagedValid() != f.snapshotRowCount()) x.add("snapshot row count " + f.snapshotRowCount() + " differs from " + f.stagedValid() + " valid staged rows");
            return GateEvaluation.of(code(), x);
        }
    }

    /** Source keys and materialized entity keys are present and unique within the snapshot. */
    @Component
    public static class KeysValid implements ReleaseGate {
        public String code() { return "KEYS_VALID"; }

        public GateEvaluation evaluate(SnapshotFacts f, Context c) {
            List<String> x = new ArrayList<>();
            if (f.rawNullKeys() > 0) x.add(f.rawNullKeys() + " raw rows have no source key");
            if (f.rawDistinctKeys() != f.rawRows() - f.rawNullKeys()) x.add("raw source keys are not unique");
            if (f.entityNullKeys() > 0) x.add(f.entityNullKeys() + " entities have no external key");
            if (f.entityDistinctKeys() != f.entityRows() - f.entityNullKeys()) x.add("entity external keys are not unique");
            return GateEvaluation.of(code(), x);
        }
    }

    /** Relations materialized from this snapshot reference current entities only. */
    @Component
    public static class RelationsValid implements ReleaseGate {
        public String code() { return "RELATIONS_VALID"; }

        public GateEvaluation evaluate(SnapshotFacts f, Context c) {
            if (f.linkRows() == 0) return GateEvaluation.notApplicable(code(), "snapshot materialized no entity relations");
            return GateEvaluation.of(code(), f.linkRetiredEndpoints() > 0 ? List.of(f.linkRetiredEndpoints() + " relation endpoints are not current") : List.of());
        }
    }

    /** Every classification assigned from this snapshot uses an ACTIVE controlled-vocabulary item. */
    @Component
    public static class ClassifiersValid implements ReleaseGate {
        public String code() { return "CLASSIFIERS_VALID"; }

        public GateEvaluation evaluate(SnapshotFacts f, Context c) {
            if (f.classificationRows() == 0) return GateEvaluation.notApplicable(code(), "snapshot assigned no classifications");
            long inactive = f.classificationItemIds().stream().filter(id -> !c.activeClassificationItemIds().contains(id)).count();
            return GateEvaluation.of(code(), inactive > 0 ? List.of(inactive + " referenced classification items are not ACTIVE") : List.of());
        }
    }

    /** Observations belong to this snapshot's own series and carry a value. */
    @Component
    public static class StatisticalSemanticsValid implements ReleaseGate {
        public String code() { return "STATISTICAL_SEMANTICS_VALID"; }

        public GateEvaluation evaluate(SnapshotFacts f, Context c) {
            if (f.observationRows() == 0) return GateEvaluation.notApplicable(code(), "snapshot materialized no observations");
            List<String> x = new ArrayList<>();
            if (f.observationForeignSeriesRows() > 0) x.add(f.observationForeignSeriesRows() + " observations use a series of another snapshot");
            if (f.observationEmptyValues() > 0) x.add(f.observationEmptyValues() + " observations carry no value");
            return GateEvaluation.of(code(), x);
        }
    }

    /** Raw rows come from the load's own artifact and every materialized row traces back to this snapshot. */
    @Component
    public static class RawLineageValid implements ReleaseGate {
        public String code() { return "RAW_LINEAGE_VALID"; }

        public GateEvaluation evaluate(SnapshotFacts f, Context c) {
            List<String> x = new ArrayList<>();
            if (f.rawRows() != f.snapshotRowCount()) x.add("raw rows " + f.rawRows() + " differ from snapshot row count " + f.snapshotRowCount());
            if (f.rawForeignArtifactRows() > 0) x.add(f.rawForeignArtifactRows() + " raw rows reference an artifact outside the load batch");
            if (f.entityForeignSourceRows() > 0) x.add(f.entityForeignSourceRows() + " entities trace to raw rows of another snapshot");
            if (f.observationForeignSourceRows() > 0) x.add(f.observationForeignSourceRows() + " observations trace to raw rows of another snapshot");
            return GateEvaluation.of(code(), x);
        }
    }

    /** One load yields exactly one snapshot, whose checksum is the checksum of its source artifact. */
    @Component
    public static class PublicationAtomic implements ReleaseGate {
        public String code() { return "PUBLICATION_ATOMIC"; }

        public GateEvaluation evaluate(SnapshotFacts f, Context c) {
            List<String> x = new ArrayList<>();
            if (f.snapshotsForLoad() != 1) x.add(f.snapshotsForLoad() + " snapshots exist for load " + f.datasetLoadId());
            if (f.loadArtifactChecksum() == null || !f.loadArtifactChecksum().equalsIgnoreCase(f.snapshotChecksum()))
                x.add("snapshot checksum differs from its load artifact checksum");
            return GateEvaluation.of(code(), x);
        }
    }
}
