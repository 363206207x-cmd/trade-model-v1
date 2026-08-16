package org.example.trademodel.telegram;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class TelegramLinkPolicy {
    private final TelegramProperties properties;

    public TelegramLinkPolicy(TelegramProperties properties) {
        this.properties = properties;
    }

    public String pushRecheckLink(String pushSnapshotId) {
        return safeLink("/recheck/" + pathSegment(pushSnapshotId));
    }

    public String positionDetailLink(Long positionId) {
        return positionId == null ? null : safeLink("/positions/" + positionId);
    }

    String safeLink(String path) {
        String configured = properties.getPublicBaseUrl();
        if (configured == null || configured.isBlank() || path == null || path.contains("?")
                || path.contains("://") || path.startsWith("//") || !canonicalPath(path)) return null;
        try {
            URI base = URI.create(configured.trim());
            String host = base.getHost();
            if (!"https".equalsIgnoreCase(base.getScheme()) || base.getUserInfo() != null
                    || host == null || !isPublicHost(host)) return null;
            URI resolved = base.resolve(path.startsWith("/") ? path : "/" + path);
            if (!"https".equalsIgnoreCase(resolved.getScheme())
                    || !host.equalsIgnoreCase(resolved.getHost())
                    || resolved.getUserInfo() != null || resolved.getQuery() != null
                    || resolved.getFragment() != null) return null;
            return resolved.toASCIIString();
        } catch (RuntimeException invalid) {
            return null;
        }
    }

    private static boolean canonicalPath(String value) {
        String path = value.startsWith("/") ? value : "/" + value;
        return path.matches("/recheck/[^/?#]+") || path.matches("/positions/[1-9][0-9]*");
    }

    private static boolean isPublicHost(String rawHost) {
        String host = rawHost.toLowerCase(Locale.ROOT);
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".local")
                || host.equals("0.0.0.0") || host.equals("::1")
                || host.startsWith("fc") || host.startsWith("fd") || host.startsWith("fe80:")) return false;
        String[] parts = host.split("\\.");
        if (parts.length == 4) {
            try {
                int a = Integer.parseInt(parts[0]);
                int b = Integer.parseInt(parts[1]);
                return a != 10 && a != 127 && a != 0 && !(a == 169 && b == 254)
                        && !(a == 172 && b >= 16 && b <= 31) && !(a == 192 && b == 168);
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return !host.isBlank();
    }

    private static String pathSegment(String value) {
        if (value == null || value.isBlank()) return "";
        return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8).replace("+", "%20");
    }
}
