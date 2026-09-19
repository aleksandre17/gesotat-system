package org.base.api.security.tenancy.scope;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;
import java.util.UUID;

/**
 * The identities a request carries, as a resolver sees them: the route's URI template variables and
 * its query/form parameters. A resolver never touches the request body, so it can never consume it.
 */
public record TenantScopeRequest(String requestUri, Map<String, String> pathVariables, HttpServletRequest request) {

    public TenantScopeRequest {
        pathVariables = pathVariables == null ? Map.of() : Map.copyOf(pathVariables);
    }

    /** First non-blank value of the named identity, from the path first and then the parameters. */
    public String text(String... names) {
        for (String name : names) {
            String value = pathVariables.get(name);
            if (value != null && !value.isBlank()) return value.trim();
        }
        if (request == null) return null;
        for (String name : names) {
            String value = request.getParameter(name);
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    public Long number(String... names) {
        String value = text(names);
        if (value == null) return null;
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException notAnIdentifier) {
            // The controller's own binding reports the malformed value; it names no product.
            return null;
        }
    }

    public UUID uuid(String... names) {
        String value = text(names);
        if (value == null) return null;
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException notAnIdentifier) {
            return null;
        }
    }
}
