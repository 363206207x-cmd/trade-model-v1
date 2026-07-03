package org.example.trademodel.analysisrun;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

public final class AnalysisTimePolicy {
    private static final ZoneOffset UTC = ZoneOffset.UTC;
    private static final Map<String, BucketSpec> SUPPORTED = Map.ofEntries(
            Map.entry("1m", new BucketSpec(ChronoUnit.MINUTES, 1)),
            Map.entry("3m", new BucketSpec(ChronoUnit.MINUTES, 3)),
            Map.entry("5m", new BucketSpec(ChronoUnit.MINUTES, 5)),
            Map.entry("15m", new BucketSpec(ChronoUnit.MINUTES, 15)),
            Map.entry("30m", new BucketSpec(ChronoUnit.MINUTES, 30)),
            Map.entry("1h", new BucketSpec(ChronoUnit.HOURS, 1)),
            Map.entry("2h", new BucketSpec(ChronoUnit.HOURS, 2)),
            Map.entry("4h", new BucketSpec(ChronoUnit.HOURS, 4)),
            Map.entry("6h", new BucketSpec(ChronoUnit.HOURS, 6)),
            Map.entry("8h", new BucketSpec(ChronoUnit.HOURS, 8)),
            Map.entry("12h", new BucketSpec(ChronoUnit.HOURS, 12)),
            Map.entry("1d", new BucketSpec(ChronoUnit.DAYS, 1)));
    private static final Set<String> EXECUTION_PLAN_PRIMARY_TIMEFRAMES = Set.of("5m", "15m", "1h", "4h");

    private AnalysisTimePolicy() {
    }

    public static Set<String> supportedTimeframes() {
        return SUPPORTED.keySet();
    }

    public static Set<String> executionPlanPrimaryTimeframes() {
        return EXECUTION_PLAN_PRIMARY_TIMEFRAMES;
    }

    public static boolean isExecutionPlanPrimaryTimeframe(String raw) {
        return raw != null && EXECUTION_PLAN_PRIMARY_TIMEFRAMES.contains(raw.trim());
    }

    public static String unsupportedExecutionPlanTimeframeMessage() {
        return "周期不支持，需使用 5m / 15m / 1h / 4h";
    }

    public static String requireSupportedTimeframe(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AnalysisRunInputException("TIMEFRAME_REQUIRED", "timeframe is required");
        }
        String timeframe = raw.trim();
        if (!SUPPORTED.containsKey(timeframe)) {
            throw new AnalysisRunInputException("TIMEFRAME_UNSUPPORTED", "unsupported timeframe: " + timeframe);
        }
        return timeframe;
    }

    public static LocalDateTime normalize(String raw, String timeframe, Clock clock) {
        requireSupportedTimeframe(timeframe);
        if (raw == null || raw.isBlank()) {
            return nowUtc(clock);
        }
        return parseStrict(raw.trim());
    }

    public static LocalDateTime canonicalBucket(String raw, String timeframe, Clock clock) {
        return canonicalBucket(normalize(raw, timeframe, clock), timeframe);
    }

    public static LocalDateTime canonicalBucket(LocalDateTime analysisTime, String timeframe) {
        if (analysisTime == null) {
            throw new AnalysisRunInputException("ANALYSIS_TIME_REQUIRED", "analysisTime is required");
        }
        BucketSpec spec = SUPPORTED.get(requireSupportedTimeframe(timeframe));
        LocalDateTime t = analysisTime.truncatedTo(ChronoUnit.SECONDS);
        if (spec.unit() == ChronoUnit.DAYS) {
            return t.toLocalDate().atStartOfDay();
        }
        if (spec.unit() == ChronoUnit.HOURS) {
            int hour = (t.getHour() / spec.size()) * spec.size();
            return t.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        }
        if (spec.unit() == ChronoUnit.MINUTES) {
            int minute = (t.getMinute() / spec.size()) * spec.size();
            return t.withMinute(minute).withSecond(0).withNano(0);
        }
        throw new AnalysisRunInputException("TIMEFRAME_UNSUPPORTED", "unsupported timeframe: " + timeframe);
    }

    private static LocalDateTime parseStrict(String raw) {
        try {
            return OffsetDateTime.parse(raw).withOffsetSameInstant(UTC).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS);
        } catch (DateTimeParseException ignored) {
            // Try local date-time formats below.
        }
        try {
            return LocalDateTime.parse(raw).truncatedTo(ChronoUnit.SECONDS);
        } catch (DateTimeParseException ignored) {
            // Try legacy space-separated format below.
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .truncatedTo(ChronoUnit.SECONDS);
        } catch (DateTimeParseException ignored) {
            throw new AnalysisRunInputException("ANALYSIS_TIME_INVALID", "analysisTime is invalid");
        }
    }

    private static LocalDateTime nowUtc(Clock clock) {
        Clock effective = clock != null ? clock : Clock.systemUTC();
        Instant instant = effective.instant();
        return LocalDateTime.ofInstant(instant, UTC).truncatedTo(ChronoUnit.SECONDS);
    }

    private record BucketSpec(ChronoUnit unit, int size) {
    }
}
