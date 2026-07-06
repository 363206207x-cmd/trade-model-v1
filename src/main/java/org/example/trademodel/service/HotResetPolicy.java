package org.example.trademodel.service;

import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.HotResetEventTypeEnum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class HotResetPolicy {

    private HotResetPolicy() {
    }

    public static Evaluation evaluate(HotResetCommand command) {
        return Evaluation.notTriggered("HOT_RESET_CONFIG_NOT_READY");
    }

    public static Evaluation evaluate(HotResetCommand command, Thresholds thresholds) {
        List<String> reasons = new ArrayList<>();
        if (thresholds == null) {
            return Evaluation.notTriggered("HOT_RESET_CONFIG_NOT_READY");
        }
        if (command == null) {
            return Evaluation.notTriggered("COMMAND_MISSING");
        }
        if (isBlank(command.getEventKey())) {
            return Evaluation.notTriggered("EVENT_KEY_MISSING");
        }
        if (isBlank(command.getSymbol())) {
            return Evaluation.notTriggered("SYMBOL_MISSING");
        }
        if (command.getEventType() == null) {
            return Evaluation.notTriggered("EVENT_TYPE_MISSING");
        }
        HotResetEventTypeEnum type = command.getEventType();
        return switch (type) {
            case EXTREME_PRICE_MOVE -> evaluateExtremePriceMove(command, reasons, thresholds);
            case OI_COLLAPSE -> evaluateOiCollapse(command, reasons, thresholds);
            case LIQUIDITY_DRAIN -> evaluateLiquidityDrain(command, reasons, thresholds);
            case SYSTEMIC_SHOCK -> evaluateSystemicShock(command, reasons, thresholds);
        };
    }

    public static AssetStateEnum resolvePostState(HotResetCommand command, ConfusedResult confusedResult,
                                                  boolean riskBlocked) {
        if (riskBlocked) {
            return AssetStateEnum.HIGH_RISK;
        }
        AssetStateEnum confusedState = safeState(confusedResult != null ? confusedResult.getNextState() : null);
        if (confusedState == AssetStateEnum.CONFUSED || confusedState == AssetStateEnum.COOLING) {
            return confusedState;
        }
        if (command == null || command.getEventType() == null) {
            return AssetStateEnum.INVALIDATED;
        }
        return switch (command.getEventType()) {
            case SYSTEMIC_SHOCK -> AssetStateEnum.HIGH_RISK;
            case OI_COLLAPSE -> AssetStateEnum.CONFUSED;
            case LIQUIDITY_DRAIN -> AssetStateEnum.COOLING;
            case EXTREME_PRICE_MOVE -> AssetStateEnum.INVALIDATED;
        };
    }

    public static boolean isUnsafePreState(AssetStateEnum state) {
        return state == AssetStateEnum.CANDIDATE
                || state == AssetStateEnum.WAITING_TRIGGER
                || state == AssetStateEnum.TRIGGERED;
    }

    public static AssetStateEnum safeState(String raw) {
        if (raw == null || raw.isBlank()) {
            return AssetStateEnum.OBSERVING;
        }
        try {
            return AssetStateEnum.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return AssetStateEnum.OBSERVING;
        }
    }

    private static Evaluation evaluateExtremePriceMove(HotResetCommand command, List<String> reasons,
                                                       Thresholds thresholds) {
        BigDecimal ratio = absoluteRatio(command.getPriceMoveRatio());
        if (ratio == null) {
            ratio = absoluteRatio(calculateRatio(command.getCurrentPrice(), command.getReferencePrice()));
        }
        if (ratio == null) {
            return Evaluation.notTriggered("PRICE_MOVE_EVIDENCE_MISSING");
        }
        reasons.add("PRICE_MOVE_RATIO=" + ratio);
        reasons.add("CONFIG_THRESHOLD=" + thresholds.getExtremePriceMoveRatioThreshold());
        if (ratio.compareTo(thresholds.getExtremePriceMoveRatioThreshold()) >= 0) {
            return Evaluation.triggered("EXTREME_PRICE_MOVE_THRESHOLD_REACHED", reasons);
        }
        return Evaluation.notTriggered("PRICE_MOVE_BELOW_THRESHOLD", reasons);
    }

    private static Evaluation evaluateOiCollapse(HotResetCommand command, List<String> reasons,
                                                 Thresholds thresholds) {
        if (isBlank(command.getSourceType()) || isBlank(command.getSourceReference())) {
            return Evaluation.notTriggered("OI_SOURCE_MISSING");
        }
        BigDecimal ratio = command.getOpenInterestChangeRatio();
        if (ratio == null) {
            ratio = calculateRatio(command.getCurrentOpenInterest(), command.getPreviousOpenInterest());
        }
        if (ratio == null) {
            return Evaluation.notTriggered("OI_EVIDENCE_MISSING");
        }
        reasons.add("OPEN_INTEREST_CHANGE_RATIO=" + ratio);
        reasons.add("CONFIG_THRESHOLD=" + thresholds.getOiCollapseChangeRatioThreshold());
        if (ratio.compareTo(thresholds.getOiCollapseChangeRatioThreshold()) <= 0) {
            return Evaluation.triggered("OI_COLLAPSE_THRESHOLD_REACHED", reasons);
        }
        return Evaluation.notTriggered("OI_COLLAPSE_BELOW_THRESHOLD", reasons);
    }

    private static Evaluation evaluateLiquidityDrain(HotResetCommand command, List<String> reasons,
                                                     Thresholds thresholds) {
        BigDecimal ratio = command.getLiquidityChangeRatio();
        if (ratio == null) {
            ratio = calculateRatio(command.getCurrentLiquidity(), command.getBaselineLiquidity());
        }
        if (ratio == null || isBlank(command.getSourceType()) || isBlank(command.getSourceReference())) {
            return Evaluation.notTriggered("LIQUIDITY_EVIDENCE_MISSING");
        }
        reasons.add("LIQUIDITY_CHANGE_RATIO=" + ratio);
        reasons.add("CONFIG_THRESHOLD=" + thresholds.getLiquidityDrainChangeRatioThreshold());
        if (ratio.compareTo(thresholds.getLiquidityDrainChangeRatioThreshold()) <= 0) {
            return Evaluation.triggered("LIQUIDITY_DRAIN_THRESHOLD_REACHED", reasons);
        }
        return Evaluation.notTriggered("LIQUIDITY_DRAIN_BELOW_THRESHOLD", reasons);
    }

    private static Evaluation evaluateSystemicShock(HotResetCommand command, List<String> reasons,
                                                    Thresholds thresholds) {
        if (!Boolean.TRUE.equals(command.getSystemicShock())) {
            return Evaluation.notTriggered("SYSTEMIC_SHOCK_FALSE");
        }
        if (isBlank(command.getSourceType()) || isBlank(command.getSourceReference())) {
            return Evaluation.notTriggered("SYSTEMIC_SHOCK_SOURCE_MISSING");
        }
        int severity = command.getSeverityScore() != null ? command.getSeverityScore() : 0;
        reasons.add("SYSTEMIC_SHOCK_SEVERITY=" + severity);
        reasons.add("CONFIG_THRESHOLD=" + thresholds.getSystemicShockSeverityThreshold());
        if (severity >= thresholds.getSystemicShockSeverityThreshold()) {
            return Evaluation.triggered("SYSTEMIC_SHOCK_THRESHOLD_REACHED", reasons);
        }
        return Evaluation.notTriggered("SYSTEMIC_SHOCK_BELOW_THRESHOLD", reasons);
    }

    private static BigDecimal calculateRatio(BigDecimal current, BigDecimal reference) {
        if (current == null || reference == null || reference.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return current.subtract(reference).divide(reference, 8, RoundingMode.HALF_UP);
    }

    private static BigDecimal absoluteRatio(BigDecimal ratio) {
        return ratio == null ? null : ratio.abs();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static final class Evaluation {
        private final boolean triggered;
        private final String reasonCode;
        private final List<String> reasonCodes;

        private Evaluation(boolean triggered, String reasonCode, List<String> reasonCodes) {
            this.triggered = triggered;
            this.reasonCode = reasonCode;
            this.reasonCodes = reasonCodes;
        }

        public static Evaluation triggered(String reasonCode, List<String> reasons) {
            List<String> all = new ArrayList<>();
            all.add(reasonCode);
            if (reasons != null) {
                all.addAll(reasons);
            }
            return new Evaluation(true, reasonCode, all);
        }

        public static Evaluation notTriggered(String reasonCode) {
            return notTriggered(reasonCode, null);
        }

        public static Evaluation notTriggered(String reasonCode, List<String> reasons) {
            List<String> all = new ArrayList<>();
            all.add(reasonCode);
            if (reasons != null) {
                all.addAll(reasons);
            }
            return new Evaluation(false, reasonCode, all);
        }

        public boolean isTriggered() { return triggered; }
        public String getReasonCode() { return reasonCode; }
        public List<String> getReasonCodes() { return reasonCodes; }
    }

    public static final class Thresholds {
        private final BigDecimal extremePriceMoveRatioThreshold;
        private final BigDecimal oiCollapseChangeRatioThreshold;
        private final BigDecimal liquidityDrainChangeRatioThreshold;
        private final int systemicShockSeverityThreshold;

        public Thresholds(BigDecimal extremePriceMoveRatioThreshold,
                          BigDecimal oiCollapseChangeRatioThreshold,
                          BigDecimal liquidityDrainChangeRatioThreshold,
                          int systemicShockSeverityThreshold) {
            this.extremePriceMoveRatioThreshold = requirePositive(
                    extremePriceMoveRatioThreshold, "hot_reset_config.extreme_price_move_ratio_threshold");
            this.oiCollapseChangeRatioThreshold = requireNegative(
                    oiCollapseChangeRatioThreshold, "hot_reset_config.oi_collapse_change_ratio_threshold");
            this.liquidityDrainChangeRatioThreshold = requireNegative(
                    liquidityDrainChangeRatioThreshold, "hot_reset_config.liquidity_drain_change_ratio_threshold");
            if (systemicShockSeverityThreshold < 0 || systemicShockSeverityThreshold > 100) {
                throw new IllegalStateException("hot_reset_config.systemic_shock_severity_threshold must be 0..100");
            }
            this.systemicShockSeverityThreshold = systemicShockSeverityThreshold;
        }

        public BigDecimal getExtremePriceMoveRatioThreshold() { return extremePriceMoveRatioThreshold; }
        public BigDecimal getOiCollapseChangeRatioThreshold() { return oiCollapseChangeRatioThreshold; }
        public BigDecimal getLiquidityDrainChangeRatioThreshold() { return liquidityDrainChangeRatioThreshold; }
        public int getSystemicShockSeverityThreshold() { return systemicShockSeverityThreshold; }

        private static BigDecimal requirePositive(BigDecimal value, String key) {
            if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException(key + " must be positive");
            }
            return value;
        }

        private static BigDecimal requireNegative(BigDecimal value, String key) {
            if (value == null || value.compareTo(BigDecimal.ZERO) >= 0) {
                throw new IllegalStateException(key + " must be negative");
            }
            return value;
        }
    }
}
