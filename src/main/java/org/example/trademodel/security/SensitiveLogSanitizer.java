package org.example.trademodel.security;

import java.util.Locale;
import java.util.Set;

final class SensitiveLogSanitizer {

    private static final String REDACTED = "[REDACTED]";
    private static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "api-key",
            "x-auth-token",
            "x-access-token"
    );

    private SensitiveLogSanitizer() {
    }

    static String sanitizeHeaderValue(String headerName, String value) {
        if (value == null) {
            return "";
        }
        String normalizedName = headerName == null ? "" : headerName.trim().toLowerCase(Locale.ROOT);
        if (SENSITIVE_HEADER_NAMES.contains(normalizedName)
                || normalizedName.contains("token")
                || normalizedName.contains("secret")
                || normalizedName.contains("password")) {
            return REDACTED;
        }
        return stripControlChars(value);
    }

    static String sanitizePath(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return "/";
        }
        return stripControlChars(requestUri.split("\\?", 2)[0]);
    }

    static String accessLog(String method, String path, int status, long durationMs, String requestId, String remote) {
        return "ACCESS_LOG method=" + safe(method)
                + " path=" + sanitizePath(path)
                + " status=" + status
                + " durationMs=" + durationMs
                + " requestId=" + safe(requestId)
                + " remote=" + safe(remote);
    }

    static String authFailureLog(String method, String path, String requestId, String remote, String reason) {
        return "AUTH_AUDIT outcome=FAILURE method=" + safe(method)
                + " path=" + sanitizePath(path)
                + " requestId=" + safe(requestId)
                + " remote=" + safe(remote)
                + " reason=" + safe(reason);
    }

    static String rateLimitLog(String method, String path, String requestId, String remote) {
        return "RATE_LIMIT_BLOCKED method=" + safe(method)
                + " path=" + sanitizePath(path)
                + " requestId=" + safe(requestId)
                + " remote=" + safe(remote);
    }

    private static String safe(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return stripControlChars(value);
    }

    private static String stripControlChars(String value) {
        return value.replaceAll("[\\r\\n\\t]", "_");
    }
}
