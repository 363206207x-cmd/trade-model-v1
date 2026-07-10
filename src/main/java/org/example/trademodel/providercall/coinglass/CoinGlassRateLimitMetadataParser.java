package org.example.trademodel.providercall.coinglass;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CoinGlassRateLimitMetadataParser {
    public CoinGlassRateLimitMetadata parse(Map<String, List<String>> headers) {
        return new CoinGlassRateLimitMetadata(
                integerHeader(headers, "API-KEY-MAX-LIMIT"),
                integerHeader(headers, "API-KEY-USE-LIMIT"),
                longHeader(headers, "RETRY-AFTER"));
    }

    private static Integer integerHeader(Map<String, List<String>> headers, String name) {
        Long value = longHeader(headers, name);
        return value == null || value > Integer.MAX_VALUE ? null : value.intValue();
    }

    private static Long longHeader(Map<String, List<String>> headers, String name) {
        if (headers == null) return null;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey() == null || !name.equals(entry.getKey().toUpperCase(Locale.ROOT))) continue;
            if (entry.getValue() == null || entry.getValue().isEmpty()) return null;
            try {
                return Long.parseLong(entry.getValue().get(0).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
