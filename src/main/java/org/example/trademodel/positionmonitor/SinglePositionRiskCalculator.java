package org.example.trademodel.positionmonitor;

import org.example.trademodel.entity.UserPositionDO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SinglePositionRiskCalculator {
    static final BigDecimal MEDIUM_LEVERAGE = new BigDecimal("5");
    static final BigDecimal HIGH_LEVERAGE = new BigDecimal("10");
    static final BigDecimal EXTREME_LEVERAGE = new BigDecimal("20");
    static final BigDecimal MEDIUM_NOTIONAL = new BigDecimal("25000");
    static final BigDecimal HIGH_NOTIONAL = new BigDecimal("50000");
    static final BigDecimal EXTREME_NOTIONAL = new BigDecimal("100000");
    static final BigDecimal MEDIUM_ADVERSE_MOVE = new BigDecimal("5");
    static final BigDecimal HIGH_ADVERSE_MOVE = new BigDecimal("10");
    static final BigDecimal EXTREME_ADVERSE_MOVE = new BigDecimal("20");

    public Assessment calculate(UserPositionDO position,
                                BigDecimal markPrice,
                                boolean marketContextHighRisk,
                                boolean marketContextBlocked) {
        validate(position, markPrice);
        PositionRiskLevelEnum level = PositionRiskLevelEnum.LOW;
        List<String> reasons = new ArrayList<>();

        BigDecimal leverage = position.getLeverage();
        BigDecimal notional = markPrice.multiply(position.getQuantity()).multiply(leverage);
        BigDecimal adverseMove = adverseMovePercent(position.getSide(), position.getEntryPrice(), markPrice)
                .multiply(leverage);

        PositionRiskLevelEnum leverageRisk = thresholdLevel(
                leverage, MEDIUM_LEVERAGE, HIGH_LEVERAGE, EXTREME_LEVERAGE);
        PositionRiskLevelEnum notionalRisk = thresholdLevel(
                notional, MEDIUM_NOTIONAL, HIGH_NOTIONAL, EXTREME_NOTIONAL);
        PositionRiskLevelEnum adverseMoveRisk = thresholdLevel(
                adverseMove, MEDIUM_ADVERSE_MOVE, HIGH_ADVERSE_MOVE, EXTREME_ADVERSE_MOVE);
        level = max(level, leverageRisk);
        level = max(level, notionalRisk);
        level = max(level, adverseMoveRisk);
        addThresholdReason(reasons, "LEVERAGE", leverageRisk);
        addThresholdReason(reasons, "POSITION_NOTIONAL", notionalRisk);
        addThresholdReason(reasons, "ADVERSE_MOVE", adverseMoveRisk);

        if (!positive(position.getStopLoss())) {
            level = max(level, PositionRiskLevelEnum.HIGH);
            reasons.add("STOP_LOSS_MISSING");
        } else if (!validStopLossDirection(position)) {
            level = PositionRiskLevelEnum.EXTREME;
            reasons.add("STOP_LOSS_DIRECTION_INVALID");
        } else if (PositionMonitorPolicy.stopLossBreached(position.getSide(), markPrice, position.getStopLoss())) {
            level = PositionRiskLevelEnum.EXTREME;
            reasons.add("STOP_LOSS_BREACHED");
        } else if (PositionMonitorPolicy.nearStopLoss(position.getSide(), markPrice, position.getStopLoss())) {
            level = max(level, PositionRiskLevelEnum.HIGH);
            reasons.add("NEAR_STOP_LOSS");
        }
        if (!positive(position.getTakeProfit())) {
            level = max(level, PositionRiskLevelEnum.MEDIUM);
            reasons.add("TAKE_PROFIT_MISSING");
        } else if (!validTakeProfitDirection(position)) {
            level = max(level, PositionRiskLevelEnum.HIGH);
            reasons.add("TAKE_PROFIT_DIRECTION_INVALID");
        }
        if (marketContextBlocked) {
            level = PositionRiskLevelEnum.EXTREME;
            reasons.add("MARKET_CONTEXT_BLOCKED");
        } else if (marketContextHighRisk) {
            level = max(level, PositionRiskLevelEnum.HIGH);
            reasons.add("MARKET_CONTEXT_HIGH_RISK");
        }
        if (reasons.isEmpty()) {
            reasons.add("POSITION_RISK_CALCULATED");
        }
        return new Assessment(level, level == PositionRiskLevelEnum.EXTREME, notional, adverseMove,
                List.copyOf(reasons));
    }

    private static void validate(UserPositionDO position, BigDecimal markPrice) {
        if (position == null || !positive(position.getEntryPrice()) || !positive(markPrice)
                || !positive(position.getQuantity()) || !positive(position.getLeverage())) {
            throw new IllegalArgumentException("position risk inputs are incomplete");
        }
        String side = normalize(position.getSide());
        if (!"LONG".equals(side) && !"SHORT".equals(side)) {
            throw new IllegalArgumentException("position side must be LONG or SHORT");
        }
    }

    private static BigDecimal adverseMovePercent(String side, BigDecimal entryPrice, BigDecimal markPrice) {
        BigDecimal adverse = "SHORT".equals(normalize(side))
                ? markPrice.subtract(entryPrice)
                : entryPrice.subtract(markPrice);
        if (adverse.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return adverse.divide(entryPrice, 8, RoundingMode.HALF_UP).multiply(new BigDecimal("100"));
    }

    private static PositionRiskLevelEnum thresholdLevel(BigDecimal value,
                                                        BigDecimal medium,
                                                        BigDecimal high,
                                                        BigDecimal extreme) {
        if (value.compareTo(extreme) >= 0) return PositionRiskLevelEnum.EXTREME;
        if (value.compareTo(high) >= 0) return PositionRiskLevelEnum.HIGH;
        if (value.compareTo(medium) >= 0) return PositionRiskLevelEnum.MEDIUM;
        return PositionRiskLevelEnum.LOW;
    }

    private static PositionRiskLevelEnum max(PositionRiskLevelEnum left, PositionRiskLevelEnum right) {
        return left.ordinal() >= right.ordinal() ? left : right;
    }

    private static void addThresholdReason(List<String> reasons,
                                           String prefix,
                                           PositionRiskLevelEnum level) {
        if (level != PositionRiskLevelEnum.LOW) {
            reasons.add(prefix + "_RISK_" + level.name());
        }
    }

    private static boolean validStopLossDirection(UserPositionDO position) {
        return "SHORT".equals(normalize(position.getSide()))
                ? position.getStopLoss().compareTo(position.getEntryPrice()) > 0
                : position.getStopLoss().compareTo(position.getEntryPrice()) < 0;
    }

    private static boolean validTakeProfitDirection(UserPositionDO position) {
        return "SHORT".equals(normalize(position.getSide()))
                ? position.getTakeProfit().compareTo(position.getEntryPrice()) < 0
                : position.getTakeProfit().compareTo(position.getEntryPrice()) > 0;
    }

    private static boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    public record Assessment(PositionRiskLevelEnum level,
                             boolean riskBlocked,
                             BigDecimal positionNotional,
                             BigDecimal leveragedAdverseMovePercent,
                             List<String> reasonCodes) {
    }
}
