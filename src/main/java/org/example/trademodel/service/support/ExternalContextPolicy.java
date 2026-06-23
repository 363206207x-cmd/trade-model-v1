package org.example.trademodel.service.support;

import org.example.trademodel.entity.ExternalContextEventDO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

public final class ExternalContextPolicy {
    public static final int HIGH_IMPACT_SCORE = 70;
    public static final int BLOCKING_IMPACT_SCORE = 85;
    public static final int SAFETY_PRE_WINDOW_MINUTES = 60;
    public static final String RISK_LOW = "LOW";
    public static final String RISK_HIGH = "HIGH";
    public static final String SOURCE_HEALTH_OK = "OK";
    public static final String SOURCE_HEALTH_BLOCKED = "BLOCKED";
    public static final String REASON_WINDOW_BLOCKED = "EXTERNAL_EVENT_WINDOW_BLOCKED";
    public static final String REASON_MISSING_SOURCE = "MISSING_EXTERNAL_EVENT_SOURCE";
    public static final String REASON_HIGH_IMPACT_REVIEW = "EXTERNAL_EVENT_HIGH_IMPACT_REVIEW";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_NEAR = "NEAR";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_RETRACTED = "RETRACTED";

    private ExternalContextPolicy() {}

    public static String windowState(ExternalContextEventDO event, LocalDateTime contextTime) {
        if (event == null || contextTime == null) {
            return STATUS_EXPIRED;
        }
        String status = normalize(event.getStatus());
        if (STATUS_CANCELLED.equals(status) || STATUS_RETRACTED.equals(status)) {
            return status;
        }
        if (event.getSourcePublishedAt() != null && event.getSourcePublishedAt().isAfter(contextTime)) {
            return STATUS_EXPIRED;
        }
        if (event.getWindowStart() != null && event.getWindowEnd() != null
                && !contextTime.isBefore(event.getWindowStart()) && !contextTime.isAfter(event.getWindowEnd())) {
            return STATUS_ACTIVE;
        }
        if (event.getWindowStart() != null && contextTime.isBefore(event.getWindowStart())) {
            long minutes = Duration.between(contextTime, event.getWindowStart()).toMinutes();
            if (minutes >= 0 && minutes <= SAFETY_PRE_WINDOW_MINUTES) {
                return STATUS_NEAR;
            }
        }
        return STATUS_EXPIRED;
    }

    public static boolean hasCompleteSource(ExternalContextEventDO event) {
        return event != null
                && hasText(event.getProvider())
                && hasText(event.getSourceType())
                && hasText(event.getSourceReference())
                && hasText(event.getSourceTraceId())
                && event.getSourcePublishedAt() != null
                && (hasText(event.getSourceEventId()) || hasText(event.getSourceHash()));
    }

    public static boolean isHighImpact(ExternalContextEventDO event) {
        return event != null && event.getImpactScore() != null && event.getImpactScore() >= HIGH_IMPACT_SCORE;
    }

    public static boolean isBlocking(ExternalContextEventDO event, String windowState) {
        if (event == null || !STATUS_ACTIVE.equals(windowState)) {
            return false;
        }
        int score = event.getImpactScore() != null ? event.getImpactScore() : 0;
        return score >= BLOCKING_IMPACT_SCORE || Boolean.TRUE.equals(event.getExecutionBlocking());
    }

    public static String lowerConfidenceOneLevel(String confidence) {
        String normalized = normalize(confidence);
        if ("HIGH".equals(normalized)) {
            return "MEDIUM";
        }
        if ("MEDIUM".equals(normalized)) {
            return "LOW";
        }
        return hasText(confidence) ? confidence : "LOW";
    }

    public static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
