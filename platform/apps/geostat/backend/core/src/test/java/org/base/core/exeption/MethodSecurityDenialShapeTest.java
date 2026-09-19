package org.base.core.exeption;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A missing authority refused by method security answers exactly like one refused by the filter chain. */
class MethodSecurityDenialShapeTest {

    @Test
    void apiRequestGetsTheBoundaryProblemAndInsufficientScopeChallenge() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        request.addHeader("Accept", "application/json");
        MockHttpServletResponse response = new MockHttpServletResponse();

        var returned = new GlobalExceptionHandler().handleAccessDeniedException(new AccessDeniedException("x"), request, response, null);

        assertNull(returned, "the response is written by the boundary handler");
        assertEquals(403, response.getStatus());
        assertEquals("Bearer error=\"insufficient_scope\"", response.getHeader("WWW-Authenticate"));
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertTrue(response.getContentType().toLowerCase().contains("utf-8"));
        assertEquals("no-store", response.getHeader("Cache-Control"));
    }
}
