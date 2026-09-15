package org.base.api.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ContractContentNegotiationFilterTest {
    @Test
    void rejectsGovernedMutationWithoutJsonContentType() throws Exception {
        var filter = new ContractContentNegotiationFilter();
        var request = new MockHttpServletRequest("POST", "/api/v1/platform/contracts/X/pages/1/query");
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, mock(FilterChain.class));
        assertEquals(415, response.getStatus());
    }

    @Test
    void acceptsJsonGovernedMutation() throws Exception {
        var filter = new ContractContentNegotiationFilter();
        var request = new MockHttpServletRequest("POST", "/api/v1/platform/contracts/X/pages/1/query");
        request.setContentType("application/json");
        var response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }
}
