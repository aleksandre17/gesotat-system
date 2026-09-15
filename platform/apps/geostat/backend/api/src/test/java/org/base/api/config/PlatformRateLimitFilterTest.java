package org.base.api.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlatformRateLimitFilterTest {
    @Test
    void rejectsSecondRequestInSameWindowAndEmitsStandardHeaders() throws Exception {
        var filter = new PlatformRateLimitFilter(1, 60_000, 100, new BoundedInMemoryRateLimitStore());
        FilterChain chain = mock(FilterChain.class);
        var first = request(); var firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, chain);
        var secondResponse = new MockHttpServletResponse();
        filter.doFilter(request(), secondResponse, chain);

        assertEquals(200, firstResponse.getStatus());
        assertEquals(429, secondResponse.getStatus());
        assertEquals("1", secondResponse.getHeader("RateLimit-Limit"));
        assertEquals("0", secondResponse.getHeader("RateLimit-Remaining"));
        assertEquals("1;w=60", secondResponse.getHeader("RateLimit-Policy"));
        assertNotNull(secondResponse.getHeader("Retry-After"));
        assertEquals("no-store", secondResponse.getHeader("Cache-Control"));
        assertNotNull(secondResponse.getHeader("X-Correlation-Id"));
        verify(chain, times(1)).doFilter(any(), any());
    }

    private static MockHttpServletRequest request() { var r = new MockHttpServletRequest("POST", "/platform/access/ingest"); r.setRemoteAddr("127.0.0.9"); return r; }
}
