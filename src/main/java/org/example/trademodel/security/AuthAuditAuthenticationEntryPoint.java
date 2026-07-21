package org.example.trademodel.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class AuthAuditAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(AuthAuditAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.warn(SensitiveLogSanitizer.authFailureLog(
                request.getMethod(),
                request.getRequestURI(),
                RequestIdSupport.currentOrNew(),
                request.getRemoteAddr(),
                "authentication_required"));
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"code\":401,\"msg\":\"authentication required\"}");
    }
}
