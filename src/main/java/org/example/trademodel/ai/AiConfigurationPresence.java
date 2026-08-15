package org.example.trademodel.ai;

import java.math.BigDecimal;

public enum AiConfigurationPresence {
    MISSING,
    EXPLICIT_ZERO,
    POSITIVE_VALUE;

    public static AiConfigurationPresence of(Integer value) {
        if (value == null) return MISSING;
        return value > 0 ? POSITIVE_VALUE : EXPLICIT_ZERO;
    }

    public static AiConfigurationPresence of(BigDecimal value) {
        if (value == null) return MISSING;
        return value.signum() > 0 ? POSITIVE_VALUE : EXPLICIT_ZERO;
    }
}
