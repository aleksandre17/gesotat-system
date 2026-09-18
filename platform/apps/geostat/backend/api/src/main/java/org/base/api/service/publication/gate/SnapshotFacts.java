package org.base.api.service.publication.gate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Measured facts of one dataset snapshot, loaded once and evaluated by every release gate.
 * {@link #digest()} fingerprints the facts so publication can prove nothing changed after evaluation.
 */
public record SnapshotFacts(long datasetSnapshotId, long datasetVersionId, String status, long datasetLoadId,
                            long snapshotRowCount, String snapshotChecksum, String loadArtifactChecksum, long snapshotsForLoad,
                            long stagedTotal, long stagedValid, long stagedRejected,
                            long rawRows, long rawDistinctKeys, long rawNullKeys, long rawForeignArtifactRows,
                            long entityRows, long entityDistinctKeys, long entityNullKeys, long entityForeignSourceRows,
                            long linkRows, long linkRetiredEndpoints,
                            long classificationRows, Set<Long> classificationItemIds,
                            long observationRows, long observationForeignSourceRows, long observationForeignSeriesRows, long observationEmptyValues) {

    public SnapshotFacts {
        classificationItemIds = Set.copyOf(classificationItemIds);
    }

    /** Stable, ordered measures recorded as gate evidence. */
    public Map<String, Object> measures() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("datasetVersionId", datasetVersionId);
        m.put("datasetLoadId", datasetLoadId);
        m.put("snapshotRowCount", snapshotRowCount);
        m.put("snapshotsForLoad", snapshotsForLoad);
        m.put("stagedTotal", stagedTotal);
        m.put("stagedValid", stagedValid);
        m.put("stagedRejected", stagedRejected);
        m.put("rawRows", rawRows);
        m.put("rawDistinctKeys", rawDistinctKeys);
        m.put("rawNullKeys", rawNullKeys);
        m.put("rawForeignArtifactRows", rawForeignArtifactRows);
        m.put("entityRows", entityRows);
        m.put("entityDistinctKeys", entityDistinctKeys);
        m.put("entityNullKeys", entityNullKeys);
        m.put("entityForeignSourceRows", entityForeignSourceRows);
        m.put("linkRows", linkRows);
        m.put("linkRetiredEndpoints", linkRetiredEndpoints);
        m.put("classificationRows", classificationRows);
        m.put("classificationItems", classificationItemIds.size());
        m.put("observationRows", observationRows);
        m.put("observationForeignSourceRows", observationForeignSourceRows);
        m.put("observationForeignSeriesRows", observationForeignSeriesRows);
        m.put("observationEmptyValues", observationEmptyValues);
        return m;
    }

    /** SHA-256 over identity, checksums and measures; any change after evaluation changes it. */
    public String digest() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            sha.update((datasetSnapshotId + "|" + snapshotChecksum + "|" + loadArtifactChecksum + "|" + measures()).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(64);
            for (byte b : sha.digest()) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
