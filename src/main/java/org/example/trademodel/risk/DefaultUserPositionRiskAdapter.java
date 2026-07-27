package org.example.trademodel.risk;

import org.example.trademodel.entity.UserPositionDO;
import org.example.trademodel.mapper.UserPositionMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class DefaultUserPositionRiskAdapter implements UserPositionRiskAdapter {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final UserPositionMapper userPositionMapper;

    public DefaultUserPositionRiskAdapter(UserPositionMapper userPositionMapper) {
        this.userPositionMapper = userPositionMapper;
    }

    @Override
    public UserPositionRiskResult currentRiskForUser(Long userId) {
        if (userId == null || userId <= 0) {
            return UserPositionRiskResult.failClosed("OWNER_SCOPE_REQUIRED");
        }
        int closedCount = Math.max(0, userPositionMapper.countClosedByUserId(userId));
        List<UserPositionDO> rows = Optional.ofNullable(userPositionMapper.listOpenByUserId(userId)).orElse(List.of());
        return calculate(rows, closedCount);
    }

    @Override
    public UserPositionRiskResult currentRiskForSystem() {
        int closedCount = Math.max(0, userPositionMapper.countClaimedClosedForSystem());
        List<UserPositionDO> rows = Optional.ofNullable(
                userPositionMapper.listClaimedOpenForSystemMonitoring()).orElse(List.of());
        return calculate(rows, closedCount);
    }

    private UserPositionRiskResult calculate(List<UserPositionDO> rows, int closedCount) {
        if (rows.isEmpty()) {
            return UserPositionRiskResult.noOpenPosition(closedCount);
        }

        List<String> reasons = new ArrayList<>();
        Map<String, BigDecimal> symbolNotional = new HashMap<>();
        Map<String, BigDecimal> sideNotional = new HashMap<>();
        int openCount = 0;
        int partiallyClosedCount = 0;
        int includedCount = 0;
        int closedFromReadSet = 0;
        BigDecimal grossNotional = BigDecimal.ZERO;
        BigDecimal maxLeverage = BigDecimal.ZERO;
        BigDecimal maxPositionNotional = BigDecimal.ZERO;
        BigDecimal stopLossPotentialLoss = BigDecimal.ZERO;
        boolean blocked = false;

        for (UserPositionDO row : rows) {
            if (row == null) {
                blocked = true;
                reasons.add("INVALID_USER_POSITION_ROW");
                continue;
            }
            String status = normalized(row.getStatus());
            if ("CLOSED".equals(status)) {
                closedFromReadSet++;
                continue;
            }
            if ("OPEN".equals(status)) {
                openCount++;
            } else if ("PARTIALLY_CLOSED".equals(status)) {
                partiallyClosedCount++;
            } else {
                blocked = true;
                reasons.add("INVALID_USER_POSITION_STATUS");
                continue;
            }
            includedCount++;

            List<String> invalidReasons = validatePosition(row);
            if (!invalidReasons.isEmpty()) {
                blocked = true;
                reasons.addAll(invalidReasons);
                continue;
            }

            BigDecimal notional = row.getEntryPrice().multiply(row.getQuantity()).multiply(row.getLeverage());
            grossNotional = grossNotional.add(notional);
            maxPositionNotional = max(maxPositionNotional, notional);
            maxLeverage = max(maxLeverage, row.getLeverage());
            symbolNotional.merge(normalized(row.getAssetSymbol()), notional, BigDecimal::add);
            sideNotional.merge(normalized(row.getSide()), notional, BigDecimal::add);
            stopLossPotentialLoss = stopLossPotentialLoss.add(stopLossLoss(row));
        }

        BigDecimal leverageRisk = percent(maxLeverage, UserPositionRiskPolicy.HIGH_LEVERAGE_THRESHOLD);
        BigDecimal positionSizeRisk = percent(maxPositionNotional, UserPositionRiskPolicy.POSITION_NOTIONAL_BLOCK_THRESHOLD);
        BigDecimal concentrationRatio = maxShare(symbolNotional, grossNotional);
        BigDecimal concentrationRisk = concentrationRatio.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
        BigDecimal directionRatio = maxShare(sideNotional, grossNotional);
        BigDecimal correlationRisk = directionRatio.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
        BigDecimal lossAmountRisk = percent(stopLossPotentialLoss, UserPositionRiskPolicy.DRAWDOWN_OR_VAR_BLOCK_AMOUNT);
        BigDecimal lossRatioRisk = grossNotional.signum() > 0
                ? stopLossPotentialLoss.divide(grossNotional, 8, RoundingMode.HALF_UP).multiply(HUNDRED)
                : BigDecimal.ZERO;
        BigDecimal drawdownRisk = max(lossAmountRisk, lossRatioRisk).setScale(2, RoundingMode.HALF_UP);

        if (maxLeverage.compareTo(UserPositionRiskPolicy.HIGH_LEVERAGE_THRESHOLD) >= 0) {
            blocked = true;
            reasons.add("HIGH_LEVERAGE_RISK");
        }
        if (maxPositionNotional.compareTo(UserPositionRiskPolicy.POSITION_NOTIONAL_BLOCK_THRESHOLD) >= 0) {
            blocked = true;
            reasons.add("POSITION_SIZE_RISK_BLOCKED");
        }
        if (includedCount > 1 && concentrationRatio.compareTo(UserPositionRiskPolicy.CONCENTRATION_BLOCK_RATIO) >= 0) {
            blocked = true;
            reasons.add("CONCENTRATION_RISK_BLOCKED");
        }
        if (includedCount > 1 && directionRatio.compareTo(UserPositionRiskPolicy.DIRECTIONAL_CORRELATION_BLOCK_RATIO) >= 0) {
            blocked = true;
            reasons.add("CORRELATION_DIRECTIONAL_PROXY_BLOCKED");
        }
        if (stopLossPotentialLoss.compareTo(UserPositionRiskPolicy.DRAWDOWN_OR_VAR_BLOCK_AMOUNT) >= 0
                || lossRatioRisk.compareTo(UserPositionRiskPolicy.DRAWDOWN_OR_VAR_BLOCK_RATIO.multiply(HUNDRED)) >= 0) {
            blocked = true;
            reasons.add("DRAWDOWN_OR_VAR_RISK_BLOCKED");
        }

        BigDecimal aggregate = max(max(leverageRisk, positionSizeRisk), max(concentrationRisk, max(correlationRisk, drawdownRisk)))
                .setScale(2, RoundingMode.HALF_UP);
        if (aggregate.compareTo(UserPositionRiskPolicy.HIGH_RISK_SCORE) >= 0) {
            blocked = true;
            reasons.add("AGGREGATE_HIGH_RISK");
        }

        if (reasons.isEmpty()) {
            reasons.add("USER_POSITION_RISK_READ_ONLY_ALLOWED");
        }

        UserPositionRiskResult result = UserPositionRiskResult.base(
                blocked ? "RISK_BLOCKED" : "RISK_ALLOWED",
                blocked || aggregate.compareTo(UserPositionRiskPolicy.HIGH_RISK_SCORE) >= 0
                        ? "HIGH"
                        : aggregate.compareTo(UserPositionRiskPolicy.MEDIUM_RISK_SCORE) >= 0 ? "MEDIUM" : "LOW",
                blocked);
        result.setIncludedPositionCount(includedCount);
        result.setExcludedClosedPositionCount(closedCount + closedFromReadSet);
        result.setOpenPositionCount(openCount);
        result.setPartiallyClosedPositionCount(partiallyClosedCount);
        result.setGrossNotional(grossNotional.setScale(8, RoundingMode.HALF_UP));
        result.setLeverageRisk(leverageRisk);
        result.setPositionSizeRisk(positionSizeRisk);
        result.setConcentrationRisk(concentrationRisk);
        result.setCorrelationRisk(correlationRisk);
        result.setDrawdownOrVarRisk(drawdownRisk);
        result.setAggregateRiskScore(aggregate);
        result.setReasonCodes(distinct(reasons));
        return result;
    }

    private static List<String> validatePosition(UserPositionDO row) {
        List<String> reasons = new ArrayList<>();
        if (blank(row.getAssetSymbol())) {
            reasons.add("ASSET_SYMBOL_REQUIRED_FAIL_CLOSED");
        }
        String side = normalized(row.getSide());
        if (!"LONG".equals(side) && !"SHORT".equals(side)) {
            reasons.add("SIDE_REQUIRED_FAIL_CLOSED");
        }
        if (nonPositive(row.getEntryPrice())) {
            reasons.add("ENTRY_PRICE_REQUIRED_FAIL_CLOSED");
        }
        if (nonPositive(row.getQuantity())) {
            reasons.add("QUANTITY_REQUIRED_FAIL_CLOSED");
        }
        if (nonPositive(row.getLeverage())) {
            reasons.add("LEVERAGE_REQUIRED_FAIL_CLOSED");
        }
        if (nonPositive(row.getStopLoss())) {
            reasons.add("STOP_LOSS_REQUIRED_FAIL_CLOSED");
        } else if ("LONG".equals(side) && row.getEntryPrice() != null
                && row.getStopLoss().compareTo(row.getEntryPrice()) >= 0) {
            reasons.add("LONG_STOP_LOSS_DIRECTION_FAIL_CLOSED");
        } else if ("SHORT".equals(side) && row.getEntryPrice() != null
                && row.getStopLoss().compareTo(row.getEntryPrice()) <= 0) {
            reasons.add("SHORT_STOP_LOSS_DIRECTION_FAIL_CLOSED");
        }
        return reasons;
    }

    private static BigDecimal stopLossLoss(UserPositionDO row) {
        BigDecimal delta = "SHORT".equals(normalized(row.getSide()))
                ? row.getStopLoss().subtract(row.getEntryPrice())
                : row.getEntryPrice().subtract(row.getStopLoss());
        return delta.abs().multiply(row.getQuantity()).multiply(row.getLeverage());
    }

    private static BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
        if (numerator == null || denominator == null || denominator.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal raw = numerator.divide(denominator, 8, RoundingMode.HALF_UP).multiply(HUNDRED);
        return raw.min(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal maxShare(Map<String, BigDecimal> values, BigDecimal total) {
        if (values.isEmpty() || total == null || total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal max = BigDecimal.ZERO;
        for (BigDecimal value : values.values()) {
            max = max(max, value);
        }
        return max.divide(total, 8, RoundingMode.HALF_UP);
    }

    private static BigDecimal max(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right == null ? BigDecimal.ZERO : right;
        }
        if (right == null) {
            return left;
        }
        return left.compareTo(right) >= 0 ? left : right;
    }

    private static boolean nonPositive(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String normalized(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private static List<String> distinct(List<String> values) {
        return values.stream().distinct().toList();
    }
}
