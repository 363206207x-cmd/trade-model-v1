package org.example.trademodel.requestcontext;

import java.util.UUID;
import java.util.regex.Pattern;

public final class RequestIdSupport {
    public static final String HEADER = "X-Request-Id";
    private static final Pattern LEGAL = Pattern.compile("^[A-Za-z0-9._:-]{1,128}$");
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private RequestIdSupport() {
    }

    public static boolean isLegal(String raw) {
        return raw != null && LEGAL.matcher(raw.trim()).matches();
    }

    public static String normalizeOrGenerate(String raw) {
        if (raw != null && isLegal(raw)) {
            return raw.trim();
        }
        return generate();
    }

    public static String generate() {
        return "req-" + UUID.randomUUID().toString().replace("-", "");
    }

    public static void setCurrent(String requestId) {
        CURRENT.set(normalizeOrGenerate(requestId));
    }

    public static String currentOrNew() {
        String current = CURRENT.get();
        if (current == null || current.isBlank()) {
            current = generate();
            CURRENT.set(current);
        }
        return current;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
