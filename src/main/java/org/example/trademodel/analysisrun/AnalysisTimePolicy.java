package org.example.trademodel.analysisrun;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

public final class AnalysisTimePolicy {
    private AnalysisTimePolicy() {
    }

    public static LocalDateTime normalize(String raw, LocalDateTime fallback) {
        LocalDateTime base = fallback != null ? fallback : LocalDateTime.now();
        if (raw == null || raw.isBlank()) {
            return base.truncatedTo(ChronoUnit.SECONDS);
        }
        String t = raw.trim();
        try {
            return LocalDateTime.parse(t).truncatedTo(ChronoUnit.SECONDS);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(t, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        .truncatedTo(ChronoUnit.SECONDS);
            } catch (DateTimeParseException ignoredAgain) {
                return base.truncatedTo(ChronoUnit.SECONDS);
            }
        }
    }

    public static LocalDateTime idempotencyBucket(LocalDateTime analysisTime) {
        LocalDateTime t = analysisTime != null ? analysisTime : LocalDateTime.now();
        return t.truncatedTo(ChronoUnit.MINUTES);
    }
}
