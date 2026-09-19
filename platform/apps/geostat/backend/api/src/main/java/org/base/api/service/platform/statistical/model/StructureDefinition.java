package org.base.api.service.platform.statistical.model;

import java.util.List;

/** Ordered components of one immutable DSD revision. */
public record StructureDefinition(Ref ref, List<Component> components) {
    public StructureDefinition { components = List.copyOf(components); }

    public List<Component> withRole(Component.Role role) {
        return components.stream().filter(c -> c.role() == role).toList();
    }
}
