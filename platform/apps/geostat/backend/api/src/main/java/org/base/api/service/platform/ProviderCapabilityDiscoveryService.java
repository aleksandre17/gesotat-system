package org.base.api.service.platform;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.*;

/** Loads only ACTIVE, approved provider capabilities from Control Plane metadata. */
@Service
public class ProviderCapabilityDiscoveryService {
    private final JdbcTemplate controlPlane;

    public ProviderCapabilityDiscoveryService(@Qualifier("primaryJdbcTemplate") JdbcTemplate controlPlane) {
        this.controlPlane = controlPlane;
    }

    public int loadActive(ProviderCapabilityRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        List<Map<String, Object>> rows = controlPlane.queryForList(
                "SELECT provider_code,family_code,operation_code,feature_code,max_page_size,transactional_supported " +
                "FROM platform.provider_capability WHERE lifecycle_status='ACTIVE' ORDER BY provider_code,family_code,operation_code,feature_code");
        Map<String, Group> groups = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String provider = String.valueOf(row.get("provider_code"));
            Group group = groups.computeIfAbsent(provider, ignored -> new Group(provider));
            group.families.add(String.valueOf(row.get("family_code")));
            group.operations.add(String.valueOf(row.get("operation_code")));
            group.features.add(String.valueOf(row.get("feature_code")));
            group.maxPageSize = Math.min(group.maxPageSize, ((Number) row.get("max_page_size")).intValue());
            group.transactional &= Boolean.TRUE.equals(row.get("transactional_supported"));
        }
        List<ProviderCapabilityRegistry.Capability> discovered = new ArrayList<>();
        for (Group group : groups.values()) {
            discovered.add(new ProviderCapabilityRegistry.Capability(group.provider, group.families, group.operations,
                    group.features, group.maxPageSize, group.transactional));
        }
        registry.replaceAll(discovered);
        return groups.size();
    }

    private static final class Group {
        final String provider; final Set<String> families = new HashSet<>(); final Set<String> operations = new HashSet<>(); final Set<String> features = new HashSet<>();
        int maxPageSize = 1_000_000; boolean transactional = true;
        Group(String provider) { this.provider = provider; }
    }
}
