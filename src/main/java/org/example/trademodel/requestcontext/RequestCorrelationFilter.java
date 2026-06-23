package org.example.trademodel.requestcontext;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incoming = request.getHeader(RequestIdSupport.HEADER);
        if (incoming != null && !RequestIdSupport.isLegal(incoming)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":400,\"msg\":\"illegal X-Request-Id\"}");
            return;
        }
        String requestId = RequestIdSupport.normalizeOrGenerate(incoming);
        RequestIdSupport.setCurrent(requestId);
        MDC.put("requestId", requestId);
        response.setHeader(RequestIdSupport.HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            RequestIdSupport.clear();
        }
    }
}
