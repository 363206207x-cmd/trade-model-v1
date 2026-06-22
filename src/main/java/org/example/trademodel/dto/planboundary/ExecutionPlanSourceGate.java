package org.example.trademodel.dto.planboundary;

import java.util.ArrayList;
import java.util.List;

public final class ExecutionPlanSourceGate {

    private ExecutionPlanSourceGate() {
    }

    public static ExecutionPlanSourceGateResultDTO validate(SourceTraceDTO sourceTrace) {
        if (sourceTrace == null) {
            return ExecutionPlanSourceGateResultDTO.incomplete(List.of("sourceTrace missing"));
        }

        if (sourceTrace.getFallbackStatus() == SourceTraceFallbackStatusEnum.WATCH_ONLY) {
            return ExecutionPlanSourceGateResultDTO.reviewOnly(List.of("sourceTrace fallbackStatus=WATCH_ONLY"));
        }
        if (sourceTrace.getFallbackStatus() == SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY) {
            return ExecutionPlanSourceGateResultDTO.blocked(List.of("sourceTrace fallbackStatus=SAFE_FAIL_CLOSED_ONLY"));
        }

        List<String> missing = new ArrayList<>();
        if (sourceTrace.getFallbackStatus() == SourceTraceFallbackStatusEnum.INCOMPLETE) {
            missing.add("sourceTrace fallbackStatus=INCOMPLETE");
        }
        if (sourceTrace.getMissingFields() != null && !sourceTrace.getMissingFields().isEmpty()) {
            missing.add("sourceTrace missingFields present");
        }
        if (sourceTrace.getBlockingReasons() != null && !sourceTrace.getBlockingReasons().isEmpty()) {
            return ExecutionPlanSourceGateResultDTO.blocked(sourceTrace.getBlockingReasons());
        }

        validateTraceSources(sourceTrace, missing);
        if (missing.isEmpty()) {
            return ExecutionPlanSourceGateResultDTO.valid();
        }
        return ExecutionPlanSourceGateResultDTO.incomplete(missing);
    }

    private static void validateTraceSources(SourceTraceDTO sourceTrace, List<String> missing) {
        if (!hasText(sourceTrace.getTimeframe()) && !hasText(sourceTrace.getSourceTimeframe())) {
            missing.add("source timeframe missing");
        }

        addWhenNull(sourceTrace.getEntryPriceSource(), "entry source missing", missing);
        addWhenBlank(sourceTrace.getEntrySourceType(), "entry source type missing", missing);
        addWhenBlank(sourceTrace.getEntrySourceTimeframe(), "entry source timeframe missing", missing);
        addWhenBlank(sourceTrace.getEntrySourceReason(), "entry source reason missing", missing);
        addWhenBlank(sourceTrace.getEntrySourceRef(), "entry source ref missing", missing);

        addWhenNull(sourceTrace.getStopPriceSource(), "stop source missing", missing);
        addWhenBlank(sourceTrace.getStopSourceType(), "stop source type missing", missing);
        addWhenBlank(sourceTrace.getStopSourceTimeframe(), "stop source timeframe missing", missing);
        addWhenBlank(sourceTrace.getStopSourceReason(), "stop source reason missing", missing);
        addWhenBlank(sourceTrace.getStopSourceRef(), "stop source ref missing", missing);

        if (sourceTrace.getTpPriceSources() == null || sourceTrace.getTpPriceSources().isEmpty()) {
            missing.add("TP source missing");
        }
        addWhenBlank(sourceTrace.getTpSourceType(), "TP source type missing", missing);
        addWhenBlank(sourceTrace.getTpSourceTimeframe(), "TP source timeframe missing", missing);
        addWhenBlank(sourceTrace.getTpSourceReason(), "TP source reason missing", missing);
        addWhenBlank(sourceTrace.getTpSourceRef(), "TP source ref missing", missing);

        addWhenNull(sourceTrace.getRrSource(), "RR source missing", missing);
        addWhenBlank(sourceTrace.getRrRuleRef(), "RR rule source missing", missing);
        addWhenBlank(sourceTrace.getLiquiditySource(), "liquidity source missing", missing);
        addWhenBlank(sourceTrace.getMultiTimeframeSource(), "multi-timeframe source missing", missing);
        addWhenBlank(sourceTrace.getEventSource(), "event window source missing", missing);
        addWhenBlank(sourceTrace.getWickSource(), "wick confirmation source missing", missing);
    }

    private static void addWhenNull(Object value, String reason, List<String> missing) {
        if (value == null) {
            missing.add(reason);
        }
    }

    private static void addWhenBlank(String value, String reason, List<String> missing) {
        if (!hasText(value)) {
            missing.add(reason);
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
