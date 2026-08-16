package org.example.trademodel.telegram;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

public final class TelegramSecretSanitizer {
    private static final Pattern TOKEN_PATH = Pattern.compile("bot[^/\\s]+", Pattern.CASE_INSENSITIVE);
    private static final int MAX_ERROR_LENGTH = 512;

    private TelegramSecretSanitizer() {
    }

    public static String sanitize(String value, TelegramProperties properties) {
        if (value == null) return null;
        String sanitized = value;
        if (properties != null) {
            sanitized = replaceLiteral(sanitized, properties.getBotToken(), "[REDACTED_TOKEN]");
            sanitized = replaceLiteral(sanitized, properties.getChatId(), "[REDACTED_RECIPIENT]");
        }
        sanitized = TOKEN_PATH.matcher(sanitized).replaceAll("bot[REDACTED_TOKEN]");
        return sanitized.length() <= MAX_ERROR_LENGTH ? sanitized : sanitized.substring(0, MAX_ERROR_LENGTH);
    }

    public static String recipientFingerprint(String chatId) {
        if (chatId == null || chatId.isBlank()) return null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(chatId.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 8);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    public static String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String replaceLiteral(String source, String secret, String replacement) {
        return secret == null || secret.isBlank() ? source : source.replace(secret, replacement);
    }
}
