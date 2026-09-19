package org.base.api.service.artifact.run;

import java.util.Map;

/**
 * One step of a package run. Implementations are discovered as Spring beans and executed in
 * {@link #order()}; adding a step changes neither the run service nor its API. A stage must be
 * idempotent: after a crash it is executed again with the same input state.
 */
public interface PackageRunStage {

    String code();

    /** Position in the pipeline; gaps are intentional so steps can be inserted. */
    int order();

    Result execute(PackageRun run) throws Exception;

    /**
     * @param blockedIssueCode {@code null} when the stage completed
     * @param detail           small, non-sensitive facts recorded as stage evidence
     */
    record Result(PackageRunState state, String blockedIssueCode, Map<String, Object> detail) {
        public Result {
            detail = Map.copyOf(detail);
        }

        public static Result completed(PackageRunState state, Map<String, Object> detail) {
            return new Result(state, null, detail);
        }

        public static Result blocked(PackageRunState state, String issueCode, Map<String, Object> detail) {
            return new Result(state, issueCode, detail);
        }

        public boolean blocked() {
            return blockedIssueCode != null;
        }
    }
}
