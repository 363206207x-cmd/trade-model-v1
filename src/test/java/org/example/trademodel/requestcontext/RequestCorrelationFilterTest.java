package org.example.trademodel.requestcontext;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void legalRequestIdIsPropagatedToHeaderAndThreadLocal() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/analysis/runs");
        request.addHeader(RequestIdSupport.HEADER, "req-manual-001");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, resp) ->
                assertThat(RequestIdSupport.currentOrNew()).isEqualTo("req-manual-001"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(RequestIdSupport.HEADER)).isEqualTo("req-manual-001");
        assertThat(RequestIdSupport.currentOrNew()).startsWith("req-");
        RequestIdSupport.clear();
    }

    @Test
    void illegalRequestIdReturnsBadRequestBeforeWriteApiRuns() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/analysis/runs");
        request.addHeader(RequestIdSupport.HEADER, "bad request id");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (req, resp) -> { });

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getContentAsString()).contains("illegal X-Request-Id");
    }
}
