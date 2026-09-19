package org.base.api.service.artifact.run;

/** One governed execution of an admitted manifest. {@code nextStageCode} is where a resumed run continues. */
public record PackageRun(long runId, long manifestId, long datasetVersionId, Status status, String nextStageCode, int attempt,
                         PackageRunState state, String lastIssueCode) {

    public enum Status {
        /** Accepted, not started. */
        PENDING,
        RUNNING,
        /** Stopped by an infrastructure failure; the same stage runs again. */
        RETRYABLE,
        /** Stopped by the package or contract itself; repeats only when an operator asks for a retry. */
        BLOCKED,
        COMPLETED
    }
}
