package org.example.trademodel.service.support;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Compatibility policy for database columns that store UTC wall-clock values without an offset.
 */
public final class UtcLocalTimePolicy {

    private UtcLocalTimePolicy() {
    }

    public static LocalDateTime now(Clock clock) {
        Clock effectiveClock = clock != null ? clock : Clock.systemUTC();
        return LocalDateTime.ofInstant(effectiveClock.instant(), ZoneOffset.UTC);
    }

    public static LocalDateTime fromOffsetDateTime(OffsetDateTime value) {
        return value == null ? null : LocalDateTime.ofInstant(value.toInstant(), ZoneOffset.UTC);
    }
}
