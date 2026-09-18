package org.base.api.service.artifact;

/** State of one content object against Object Storage; mirrors {@code ck_artifact_object_status}. */
public enum VerificationStatus {
    REGISTERED(ArtifactIssue.Code.UNVERIFIED_OBJECT),
    VERIFIED(null),
    MISSING(ArtifactIssue.Code.OBJECT_MISSING),
    CHECKSUM_MISMATCH(ArtifactIssue.Code.CHECKSUM_MISMATCH);

    private final ArtifactIssue.Code issue;

    VerificationStatus(ArtifactIssue.Code issue) {
        this.issue = issue;
    }

    /** Finding raised when an attachment points at an object in this state; null when servable. */
    public ArtifactIssue.Code issue() {
        return issue;
    }
}
