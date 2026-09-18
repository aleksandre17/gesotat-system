package org.base.api.service.artifact;

/**
 * Release-gate result as stored in {@code publication.release_gate_audit.result}.
 * NOT_APPLICABLE is an evaluated outcome (the snapshot has nothing the gate governs), not a skip.
 */
public enum GateResult {
    PASS, FAIL, NOT_APPLICABLE;

    public boolean permitsPublication() {
        return this != FAIL;
    }
}
