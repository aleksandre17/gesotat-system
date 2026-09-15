package org.base.api.service.platform;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Loads approved provider capabilities once during startup when explicitly enabled. */
@Component
@ConditionalOnProperty(name = "platform.provider.discovery.enabled", havingValue = "true")
public final class ProviderCapabilityDiscoveryRunner implements ApplicationRunner {
    private final ProviderCapabilityDiscoveryService discovery;
    private final ProviderCapabilityRegistry registry;

    public ProviderCapabilityDiscoveryRunner(ProviderCapabilityDiscoveryService discovery,
                                             ProviderCapabilityRegistry registry) {
        this.discovery = discovery;
        this.registry = registry;
    }

    @Override
    public void run(ApplicationArguments args) {
        int loaded = discovery.loadActive(registry);
        if (loaded == 0) {
            throw new IllegalStateException("Provider discovery enabled but no ACTIVE provider capability exists");
        }
    }
}
