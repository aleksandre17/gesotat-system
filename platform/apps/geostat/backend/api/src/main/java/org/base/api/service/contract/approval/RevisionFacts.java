package org.base.api.service.contract.approval;

import java.util.List;

/**
 * Measured state of one site contract revision at approval time. Checks are pure functions of these
 * facts; nothing here names a site, provider or schema.
 *
 * @param currentApprovedRevision the revision consumers are served today, or {@code null} for a first approval
 * @param breakingChanges         differences from {@code currentApprovedRevision} that break consumers
 */
public record RevisionFacts(long revisionId, String contractCode, int revision, String status, String compatibilityMode,
                            String checksum, boolean checksumMatchesDocument, int datasetCount, int foreignProductDatasetCount,
                            Integer currentApprovedRevision, List<String> breakingChanges, String breakingAcknowledgement) {
    public RevisionFacts {
        breakingChanges = List.copyOf(breakingChanges);
    }

    public boolean breaking() {
        return !breakingChanges.isEmpty();
    }

    public String compatibility() {
        return currentApprovedRevision == null ? "INITIAL" : breaking() ? "BREAKING" : "BACKWARD_COMPATIBLE";
    }
}
