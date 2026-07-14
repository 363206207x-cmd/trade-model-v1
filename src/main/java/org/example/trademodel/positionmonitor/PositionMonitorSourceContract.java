package org.example.trademodel.positionmonitor;

import java.util.Locale;

public final class PositionMonitorSourceContract {
    public static final String EXECUTION_PLAN_PREFIX = "EXECUTION_PLAN:";
    public static final String ANALYSIS_PREFIX = "ANALYSIS:";
    public static final String UNVERIFIED_ANALYSIS_ID = "POSITION_SOURCE_UNVERIFIED";

    private PositionMonitorSourceContract() {
    }

    public static SourceReference parse(String sourceRefId) {
        String value = trimToNull(sourceRefId);
        if (value == null) {
            return null;
        }
        String upper = value.toUpperCase(Locale.ROOT);
        if (upper.startsWith(EXECUTION_PLAN_PREFIX)) {
            return reference(SourceType.EXECUTION_PLAN, value.substring(EXECUTION_PLAN_PREFIX.length()));
        }
        if (upper.startsWith(ANALYSIS_PREFIX)) {
            return reference(SourceType.ANALYSIS, value.substring(ANALYSIS_PREFIX.length()));
        }
        return null;
    }

    public static String executionPlanReference(String planId) {
        return EXECUTION_PLAN_PREFIX + requireId(planId);
    }

    public static String analysisReference(String analysisId) {
        return ANALYSIS_PREFIX + requireId(analysisId);
    }

    public static boolean isUnverifiedAnalysisId(String analysisId) {
        return UNVERIFIED_ANALYSIS_ID.equalsIgnoreCase(trimToNull(analysisId));
    }

    private static SourceReference reference(SourceType type, String id) {
        String normalizedId = trimToNull(id);
        return normalizedId == null ? null : new SourceReference(type, normalizedId);
    }

    private static String requireId(String value) {
        String id = trimToNull(value);
        if (id == null) {
            throw new IllegalArgumentException("source reference id is required");
        }
        return id;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public enum SourceType {
        EXECUTION_PLAN,
        ANALYSIS
    }

    public record SourceReference(SourceType type, String id) {
    }
}
