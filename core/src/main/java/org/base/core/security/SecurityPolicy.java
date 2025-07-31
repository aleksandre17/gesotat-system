package org.base.core.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

@Component
public class SecurityPolicy {
    /**
     * Security policies for various actions.
     * These methods are annotated with @PreAuthorize to enforce security checks.
     */
    @PreAuthorize("hasAuthority('READ_RESOURCE')")
    public void canReadResource() {}

    @PreAuthorize("hasAuthority('WRITE_RESOURCE')")
    public void canWriteResource() {}

    @PreAuthorize("hasAuthority('DELETE_RESOURCE')")
    public void canDeleteResource() {}
}
