package org.base.core.exeption.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** RFC 9457 body and RFC 6750 challenge on the API boundary; nothing about the refused token is disclosed. */
class ApiAccessDeniedHandlerTest {
    private final ApiAccessDeniedHandler handler = new ApiAccessDeniedHandler();

    @Test
    void anonymousRequestGetsABearerChallengeAndAProblemBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, new BadCredentialsException("internal reason that must not leak"));

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertEquals("application/problem+json;charset=UTF-8", response.getContentType());
        assertEquals("no-store", response.getHeader("Cache-Control"));
        assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
        JsonNode body = new ObjectMapper().readTree(response.getContentAsString());
        assertEquals(401, body.path("status").asInt());
        assertEquals("Unauthorized", body.path("title").asText());
        assertFalse(response.getContentAsString().contains("internal reason"));
    }

    @Test
    void refusedBearerTokenIsAnInvalidTokenChallenge() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        request.addHeader("Authorization", "bearer not-a-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(request, response, new BadCredentialsException("x"));

        assertEquals("Bearer error=\"invalid_token\"", response.getHeader("WWW-Authenticate"));
    }

    @Test
    void emptyBearerCredentialIsStillARefusedToken() throws Exception {
        for (String header : new String[]{"Bearer", "Bearer ", "bearer   "}) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
            request.addHeader("Authorization", header);
            MockHttpServletResponse response = new MockHttpServletResponse();
            handler.commence(request, response, new BadCredentialsException("x"));
            assertEquals("Bearer error=\"invalid_token\"", response.getHeader("WWW-Authenticate"), header);
        }
    }

    @Test
    void missingAuthorityIsInsufficientScopeAndAnUnsafeCorrelationIdIsNotReflected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/anything");
        request.addHeader("X-Correlation-Id", "<script>alert(1)</script>");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response, new AccessDeniedException("x"));

        assertEquals(403, response.getStatus());
        assertEquals("Bearer error=\"insufficient_scope\"", response.getHeader("WWW-Authenticate"));
        assertNotEquals("<script>alert(1)</script>", response.getHeader("X-Correlation-Id"));
        assertFalse(response.getContentAsString().contains("<script>"));
    }
}
