package org.example.trademodel.risk;

import java.math.BigDecimal;

public final class UserPositionRiskPolicy {
    public static final BigDecimal HIGH_LEVERAGE_THRESHOLD = new BigDecimal("10");
    public static final BigDecimal POSITION_NOTIONAL_BLOCK_THRESHOLD = new BigDecimal("100000");
    public static final BigDecimal CONCENTRATION_BLOCK_RATIO = new BigDecimal("0.80");
    public static final BigDecimal DIRECTIONAL_CORRELATION_BLOCK_RATIO = new BigDecimal("0.90");
    public static final BigDecimal DRAWDOWN_OR_VAR_BLOCK_AMOUNT = new BigDecimal("5000");
    public static final BigDecimal DRAWDOWN_OR_VAR_BLOCK_RATIO = new BigDecimal("0.20");
    public static final BigDecimal HIGH_RISK_SCORE = new BigDecimal("90");
    public static final BigDecimal MEDIUM_RISK_SCORE = new BigDecimal("70");

    private UserPositionRiskPolicy() {
    }
}
