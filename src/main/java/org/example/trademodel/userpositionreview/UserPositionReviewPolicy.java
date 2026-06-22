package org.example.trademodel.userpositionreview;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class UserPositionReviewPolicy {
    public static final BigDecimal EXECUTION_DEVIATION_TOLERANCE_RATIO = new BigDecimal("0.01");

    private UserPositionReviewPolicy() {
    }

    public static BigDecimal parseSingleNumberOrRangeMidpoint(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.matches("\\d+(?:\\.\\d+)?")) {
            return positiveOrNull(new BigDecimal(trimmed));
        }
        if (trimmed.matches("\\d+(?:\\.\\d+)?\\s*(?:-|~|to)\\s*\\d+(?:\\.\\d+)?")) {
            String[] parts = trimmed.split("\\s*(?:-|~|to)\\s*");
            BigDecimal first = positiveOrNull(new BigDecimal(parts[0]));
            BigDecimal second = positiveOrNull(new BigDecimal(parts[1]));
            if (first == null || second == null) {
                return null;
            }
            return first.add(second).divide(new BigDecimal("2"), 8, RoundingMode.HALF_UP);
        }
        return null;
    }

    public static BigDecimal deviationRatio(BigDecimal actual, BigDecimal reference) {
        if (actual == null || reference == null || actual.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return actual.subtract(reference).abs().divide(actual, 8, RoundingMode.HALF_UP);
    }

    public static boolean exceedsTolerance(BigDecimal ratio) {
        return ratio != null && ratio.compareTo(EXECUTION_DEVIATION_TOLERANCE_RATIO) > 0;
    }

    private static BigDecimal positiveOrNull(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? null : value;
    }
}
