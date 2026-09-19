package org.base.api.service.artifact.upload;

/** Lifecycle of a resumable upload session; the rules of each state live here, not in string comparisons. */
public enum UploadSessionStatus {
    OPEN(true, false),
    /** Completion failed on a dependency; parts are kept and the session accepts work again. */
    RETRYABLE(true, false),
    PROCESSING(false, false),
    COMMITTED(false, true),
    REJECTED(false, true),
    CANCELLED(false, true),
    EXPIRED(false, true);

    private final boolean active;
    private final boolean terminal;

    UploadSessionStatus(boolean active, boolean terminal) {
        this.active = active;
        this.terminal = terminal;
    }

    /** Accepts parts, completion and cancellation. */
    public boolean active() {
        return active;
    }

    /** Terminal states give the tenant's reserved quota back. */
    public boolean releasesQuota() {
        return terminal;
    }
}
