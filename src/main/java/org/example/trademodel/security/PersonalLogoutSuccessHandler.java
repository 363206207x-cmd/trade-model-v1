package org.example.trademodel.security;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class PersonalLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {
    private final LoginAuditLogger loginAuditLogger;

    public PersonalLogoutSuccessHandler(LoginAuditLogger loginAuditLogger) {
        this.loginAuditLogger = loginAuditLogger;
        setDefaultTargetUrl("/login?logout=true");
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        loginAuditLogger.logout(authentication == null ? "-" : authentication.getName());
        super.onLogoutSuccess(request, response, authentication);
    }
}
