package org.base.api.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CorrelationIdFilterTest {
    @Test
    void preservesSafeCorrelationIdAndCleansMdc() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "trace-123");
        var response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> assertEquals("trace-123", MDC.get(CorrelationIdFilter.MDC_KEY));

        new CorrelationIdFilter().doFilter(request, response, chain);

        assertEquals("trace-123", response.getHeader(CorrelationIdFilter.HEADER));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void replacesUnsafeCorrelationId() throws Exception {
        var request = new MockHttpServletRequest();
        request.addHeader(CorrelationIdFilter.HEADER, "bad value\r\n");
        var response = new MockHttpServletResponse();
        var chain = mock(FilterChain.class);

        new CorrelationIdFilter().doFilter(request, response, chain);

        String generated = response.getHeader(CorrelationIdFilter.HEADER);
        assertNotNull(generated);
        assertTrue(generated.matches("[0-9a-f-]{36}"));
        verify(chain).doFilter(request, response);
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
