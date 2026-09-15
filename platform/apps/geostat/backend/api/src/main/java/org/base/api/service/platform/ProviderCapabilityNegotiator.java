package org.base.api.service.platform;

import java.util.Set;

/** Performs fail-closed capability negotiation before query/materialization compilation. */
public final class ProviderCapabilityNegotiator {
    private ProviderCapabilityNegotiator() { }

    public static ProviderCapabilityRegistry.Capability require(ProviderCapabilityRegistry registry,
                                                                  String providerCode, String family, String operation,
                                                                  Set<String> requiredFeatures, int requestedPageSize) {
        if (registry == null || requiredFeatures == null || requestedPageSize < 1) throw new IllegalArgumentException("Invalid capability request");
        var capability = registry.require(providerCode, family, operation);
        if (requestedPageSize > capability.maxPageSize()) throw new IllegalArgumentException("Requested page exceeds provider capability");
        if (!capability.features().containsAll(requiredFeatures)) throw new IllegalArgumentException("Provider lacks required capability feature");
        return capability;
    }
}
