package org.example.trademodel.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;

public final class TelegramDedupeKey {
    private static final String PREFIX = "TG1";

    private TelegramDedupeKey() {
    }

    public static String create(String eventType, String state, int severity, int windowMinutes,
                                Long userId, String sourceType, String sourceId, LocalDateTime occurredAt) {
        long seconds = Math.max(60L, Math.max(1, windowMinutes) * 60L);
        long epoch = (occurredAt == null ? LocalDateTime.now(ZoneOffset.UTC) : occurredAt)
                .toEpochSecond(ZoneOffset.UTC);
        long bucket = epoch / seconds;
        return String.join("|", PREFIX, safe(eventType), safe(state), String.valueOf(Math.max(0, severity)),
                String.valueOf(bucket), fingerprint(userId + "|" + sourceType + "|" + sourceId));
    }

    public static boolean managed(String value) {
        return value != null && value.startsWith(PREFIX + "|");
    }

    public static int severity(String value) {
        String[] parts = parts(value);
        if (parts.length < 4) return 0;
        try { return Math.max(0, Integer.parseInt(parts[3])); }
        catch (NumberFormatException ignored) { return 0; }
    }

    public static String eventType(String value) {
        String[] parts = parts(value);
        return parts.length > 1 ? parts[1] : "UNKNOWN";
    }

    private static String[] parts(String value) { return value == null ? new String[0] : value.split("\\|", -1); }
    private static String safe(String value) {
        return value == null ? "UNKNOWN" : value.trim().toUpperCase().replaceAll("[^A-Z0-9_]+", "_");
    }
    private static String fingerprint(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }
}
