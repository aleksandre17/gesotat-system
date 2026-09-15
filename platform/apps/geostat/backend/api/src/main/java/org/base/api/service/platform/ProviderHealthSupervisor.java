package org.base.api.service.platform;

import java.util.*;

/** Provider-neutral health/failover selector. It never invents a provider: candidates come from approved metadata. */
public final class ProviderHealthSupervisor {
    @FunctionalInterface
    public interface HealthProbe { boolean healthy(String providerCode); }

    public record Candidate(String providerCode, int priority) {
        public Candidate {
            if (providerCode == null || !providerCode.matches("[A-Za-z][A-Za-z0-9_.-]{0,127}"))
                throw new IllegalArgumentException("providerCode is invalid");
        }
    }

    public Optional<String> select(List<Candidate> candidates, HealthProbe probe) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(probe, "probe");
        return candidates.stream().sorted(Comparator.comparingInt(Candidate::priority)
                        .thenComparing(Candidate::providerCode))
                .filter(c -> probe.healthy(c.providerCode()))
                .map(Candidate::providerCode).findFirst();
    }
}
