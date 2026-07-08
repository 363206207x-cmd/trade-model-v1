package org.example.trademodel.security;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestRateLimitFilter.class);

    private final boolean enabled;
    private final int requestsPerMinute;
    private final long windowMs;
    private final ConcurrentMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RequestRateLimitFilter(
            @Value("${trade-model.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${trade-model.security.rate-limit.requests-per-minute:1200}") int requestsPerMinute,
            @Value("${trade-model.security.rate-limit.window-ms:60000}") long windowMs) {
        this.enabled = enabled;
        this.requestsPerMinute = requestsPerMinute;
        this.windowMs = windowMs;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || requestsPerMinute <= 0 || windowMs <= 0) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!allow(request)) {
            log.warn(SensitiveLogSanitizer.rateLimitLog(
                    request.getMethod(),
                    request.getRequestURI(),
                    RequestIdSupport.currentOrNew(),
                    request.getRemoteAddr()));
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(Math.max(1, windowMs / 1000)));
            response.getWriter().write("{\"code\":429,\"msg\":\"rate limit exceeded\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean allow(HttpServletRequest request) {
        long now = System.currentTimeMillis();
        String key = keyFor(request);
        WindowCounter counter = counters.computeIfAbsent(key, ignored -> new WindowCounter(now));
        synchronized (counter) {
            if (now - counter.windowStartedAt >= windowMs) {
                counter.windowStartedAt = now;
                counter.count = 0;
            }
            counter.count++;
            return counter.count <= requestsPerMinute;
        }
    }

    private String keyFor(HttpServletRequest request) {
        return String.join("|",
                Objects.toString(request.getRemoteAddr(), "-"),
                Objects.toString(request.getMethod(), "-"),
                SensitiveLogSanitizer.sanitizePath(request.getRequestURI()));
    }

    private static final class WindowCounter {
        private long windowStartedAt;
        private int count;

        private WindowCounter(long windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }
    }
}
