package org.example.trademodel.service.support;

import java.math.BigDecimal;

/** Shared product contract for the data-quality circuit breaker. */
public final class DataQualityCircuitBreakerPolicy {

    public static final int MIN_PASS_SCORE = 70;
    public static final int MIN_VALID_SCORE = 0;
    public static final int MAX_VALID_SCORE = 100;
    public static final BigDecimal MIN_PASS_SCORE_DECIMAL = BigDecimal.valueOf(MIN_PASS_SCORE);

    private DataQualityCircuitBreakerPolicy() {
    }

    public static boolean isValid(Integer score) {
        return score != null && score >= MIN_VALID_SCORE && score <= MAX_VALID_SCORE;
    }

    public static boolean isValid(BigDecimal score) {
        return score != null
                && score.compareTo(BigDecimal.valueOf(MIN_VALID_SCORE)) >= 0
                && score.compareTo(BigDecimal.valueOf(MAX_VALID_SCORE)) <= 0;
    }

    public static boolean passes(Integer score) {
        return isValid(score) && score >= MIN_PASS_SCORE;
    }

    public static boolean passes(BigDecimal score) {
        return isValid(score) && score.compareTo(MIN_PASS_SCORE_DECIMAL) >= 0;
    }

    public static boolean isBlocked(Integer score) {
        return !passes(score);
    }
}
