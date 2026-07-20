package org.example.trademodel.security;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

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
                safeAuditValue(username), eventTime, outcome, safeAuditValue(reason));
    }

    static String safeAuditValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        StringBuilder sanitized = new StringBuilder();
        value.codePoints().forEach(codePoint -> appendAuditCodePoint(sanitized, codePoint));
        return sanitized.isEmpty() ? "-" : sanitized.toString();
    }

    private static void appendAuditCodePoint(StringBuilder target, int codePoint) {
        String fragment;
        if (isSafeAuditCodePoint(codePoint)) {
            fragment = new String(Character.toChars(codePoint));
        } else {
            String hex = Integer.toHexString(codePoint).toUpperCase(Locale.ROOT);
            fragment = "_u" + "0".repeat(Math.max(0, 4 - hex.length())) + hex + "_";
        }
        if (target.length() + fragment.length() <= 128) {
            target.append(fragment);
        }
    }

    private static boolean isSafeAuditCodePoint(int codePoint) {
        return codePoint >= 'a' && codePoint <= 'z'
                || codePoint >= 'A' && codePoint <= 'Z'
                || codePoint >= '0' && codePoint <= '9'
                || codePoint == '.'
                || codePoint == '_'
                || codePoint == '@'
                || codePoint == '-';
    }
}
