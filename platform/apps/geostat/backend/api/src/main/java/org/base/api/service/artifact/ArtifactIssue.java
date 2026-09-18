package org.base.api.service.artifact;

/** Exact, machine-readable finding. ERROR blocks binding/publication; WARNING is reported evidence. */
public record ArtifactIssue(Code code, Severity severity, String subject, String detail) {
    public enum Severity { ERROR, WARNING }

    public enum Code {
        UNMATCHED_ROW,
        MISSING_SOURCE_VALUE,
        CASE_MISMATCH,
        ORPHAN_ARTIFACT,
        CARDINALITY_VIOLATION,
        POLICY_MEDIA_TYPE,
        POLICY_MAX_BYTES,
        OBJECT_MISSING,
        UNVERIFIED_OBJECT,
        CHECKSUM_MISMATCH,
        SLOT_CONFLICT
    }

    public static ArtifactIssue error(Code code, String subject, String detail) {
        return new ArtifactIssue(code, Severity.ERROR, subject, detail);
    }

    public static ArtifactIssue warning(Code code, String subject, String detail) {
        return new ArtifactIssue(code, Severity.WARNING, subject, detail);
    }
}
