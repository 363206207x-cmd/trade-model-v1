package org.example.trademodel.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
public class AccessLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AccessLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        boolean completedNormally = false;
        try {
            filterChain.doFilter(request, response);
            completedNormally = true;
        } finally {
            long durationMs = Math.max(0, System.currentTimeMillis() - startedAt);
            log.info(SensitiveLogSanitizer.accessLog(
                    request.getMethod(),
                    request.getRequestURI(),
                    statusForLog(response.getStatus(), completedNormally),
                    durationMs,
                    RequestIdSupport.currentOrNew(),
                    request.getRemoteAddr()));
        }
    }

    static int statusForLog(int responseStatus, boolean completedNormally) {
        return !completedNormally && responseStatus < 400
                ? HttpServletResponse.SC_INTERNAL_SERVER_ERROR
                : responseStatus;
    }
}
