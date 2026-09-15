package org.base.api.service.platform;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProviderHealthSupervisorTest {
    @Test void selectsHealthyLowestPriorityAndDeterministicallyFailsOver() {
        var s = new ProviderHealthSupervisor();
        var candidates = List.of(new ProviderHealthSupervisor.Candidate("PRIMARY", 1),
                new ProviderHealthSupervisor.Candidate("SECONDARY", 2));
        assertEquals(Optional.of("PRIMARY"), s.select(candidates, p -> true));
        assertEquals(Optional.of("SECONDARY"), s.select(candidates, p -> !p.equals("PRIMARY")));
        assertEquals(Optional.empty(), s.select(candidates, p -> false));
    }

    @Test void restartIsStatelessAndReconstructsSelectionFromApprovedCandidates() {
        var candidates = List.of(new ProviderHealthSupervisor.Candidate("PRIMARY", 1),
                new ProviderHealthSupervisor.Candidate("SECONDARY", 2));
        var firstProcess = new ProviderHealthSupervisor();
        assertEquals(Optional.of("PRIMARY"), firstProcess.select(candidates, p -> true));

        // A new supervisor must not inherit stale health or routing state.
        var restartedProcess = new ProviderHealthSupervisor();
        assertEquals(Optional.of("SECONDARY"),
                restartedProcess.select(candidates, p -> p.equals("SECONDARY")));
        assertEquals(Optional.empty(), restartedProcess.select(candidates, p -> false));
    }
}
