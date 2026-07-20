package org.example.trademodel.security;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoginAuditLogger {
    private static final Logger log = LoggerFactory.getLogger(LoginAuditLogger.class);

    private final Clock clock;

    public LoginAuditLogger() {
        this(Clock.systemUTC());
    }

    LoginAuditLogger(Clock clock) {
        this.clock = clock == null ? Clock.systemUTC() : clock;
    }

    void success(String username) {
        audit("SUCCESS", username, "credentials_valid");
    }

    void failure(String username, String reason) {
        audit("FAILURE", username, reason);
    }

    void blocked(String username) {
        audit("TEMPORARILY_BLOCKED", username, "failure_limit_reached");
    }

    void logout(String username) {
        audit("LOGOUT", username, "session_invalidated");
    }

    private void audit(String outcome, String username, String reason) {
        Instant eventTime = clock.instant();
        log.info("LOGIN_AUDIT username={} eventTime={} outcome={} reason={}",
                safe(username), eventTime, outcome, safe(reason));
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value.replaceAll("[\\r\\n\\t]", "_");
        return sanitized.length() <= 128 ? sanitized : sanitized.substring(0, 128);
    }
}
