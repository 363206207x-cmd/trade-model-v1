package org.example.trademodel.service.dashboard;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.service.RuntimeKlineContextAssemblyService;
import org.example.trademodel.service.impl.RuntimeKlineContextAssemblyServiceImpl;
import org.example.trademodel.vo.DecisionResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Explicit dashboard RuntimeKline boundary for BACKEND-P8.
 * It describes unavailable runtime kline context without fabricating OHLCV or boundary sources.
 */
@Component
public class DefaultDashboardRuntimeKlineContextAdapter implements DashboardRuntimeKlineContextAdapter {
    private static final int DEFAULT_REQUIRED_WINDOW_SIZE = 50;
    private static final long FALLBACK_MAX_READ_LAG_MS = 15L * 60_000L;

    private final PersistedOhlcvQueryService persistedOhlcvQueryService;
    private final RuntimeKlineContextAssemblyService runtimeKlineContextAssemblyService;

    public DefaultDashboardRuntimeKlineContextAdapter() {
        this(null, new RuntimeKlineContextAssemblyServiceImpl());
    }

    public DefaultDashboardRuntimeKlineContextAdapter(PersistedOhlcvQueryService persistedOhlcvQueryService) {
        this(persistedOhlcvQueryService, new RuntimeKlineContextAssemblyServiceImpl());
    }

    @Autowired
    public DefaultDashboardRuntimeKlineContextAdapter(
            PersistedOhlcvQueryService persistedOhlcvQueryService,
            RuntimeKlineContextAssemblyService runtimeKlineContextAssemblyService
    ) {
        this.persistedOhlcvQueryService = persistedOhlcvQueryService;
        this.runtimeKlineContextAssemblyService = runtimeKlineContextAssemblyService == null
                ? new RuntimeKlineContextAssemblyServiceImpl()
                : runtimeKlineContextAssemblyService;
    }

    @Override
    public RuntimeKlineContextDTO buildUnavailableContext(String symbol, DecisionResultVO decision) {
        RuntimeKlineContextDTO context = new RuntimeKlineContextDTO();
        String decisionSymbol = decision == null ? null : decision.getSymbol();
        String decisionTimeframe = decision == null ? null : decision.getTimeframe();

        context.setSymbol(hasText(decisionSymbol) ? decisionSymbol : symbol);
        if (hasText(decisionTimeframe)) {
        context.setTimeframe(decisionTimeframe);
        }
        RuntimeKlineContextDTO assembled = assembleReadiness(evaluateReadiness(context));
        copySafeRuntimeAssembly(context, assembled);
        if (isSafeRuntimeAssembly(assembled)) {
            context.setFallbackStatus(null);
            context.setMissingFields(List.of());
        } else {
            context.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
            context.setMissingFields(mergedMissingFields(
                    missingFields(context.getSymbol(), context.getTimeframe(), decision),
                    assembled
            ));
        }
        context.setManualReviewRequired(true);
        context.setNotTradeInstruction(true);
        return context;
    }

    private PersistedOhlcvReadinessResult evaluateReadiness(RuntimeKlineContextDTO context) {
        if (context == null) {
            return null;
        }
        if (persistedOhlcvQueryService == null) {
            return fallbackUnknownReadiness(
                    context.getSymbol(),
                    context.getTimeframe(),
                    "Persisted OHLCV readiness query service is not available.",
                    List.of("persistedOhlcvReadinessService")
            );
        }
        try {
            return persistedOhlcvQueryService.evaluateReadiness(
                    context.getSymbol(),
                    context.getTimeframe(),
                    DEFAULT_REQUIRED_WINDOW_SIZE,
                    maxReadLagMs(context.getTimeframe())
            );
        } catch (RuntimeException e) {
            return fallbackUnknownReadiness(
                    context.getSymbol(),
                    context.getTimeframe(),
                    "Persisted OHLCV readiness query failed closed.",
                    List.of("persistedOhlcvReadinessQuery")
            );
        }
    }

    private RuntimeKlineContextDTO assembleReadiness(PersistedOhlcvReadinessResult readiness) {
        if (runtimeKlineContextAssemblyService == null) {
            return null;
        }
        return runtimeKlineContextAssemblyService.assemble(readiness);
    }

    private void copySafeRuntimeAssembly(RuntimeKlineContextDTO context, RuntimeKlineContextDTO assembled) {
        if (context == null || assembled == null) {
            return;
        }
        context.setPersistedOhlcvReadinessStatus(assembled.getPersistedOhlcvReadinessStatus());
        context.setPersistedOhlcvStaleReasonCode(assembled.getPersistedOhlcvStaleReasonCode());
        context.setPersistedOhlcvStaleReasonText(assembled.getPersistedOhlcvStaleReasonText());
        context.setPersistedOhlcvMissingFields(assembled.getPersistedOhlcvMissingFields());
        if (isSafeRuntimeAssembly(assembled)) {
            context.setLatestPrice(assembled.getLatestPrice());
            context.setKlineItems(assembled.getKlineItems());
        }
    }

    private boolean isSafeRuntimeAssembly(RuntimeKlineContextDTO assembled) {
        return assembled != null
                && assembled.getFallbackStatus() == null
                && assembled.getMissingFields().isEmpty()
                && assembled.getLatestPrice() != null
                && !assembled.getKlineItems().isEmpty();
    }

    private List<String> mergedMissingFields(List<String> baseFields, RuntimeKlineContextDTO assembled) {
        List<String> fields = new ArrayList<>(baseFields);
        if (assembled != null) {
            for (String field : assembled.getMissingFields()) {
                addUnique(field, fields);
            }
            for (String field : assembled.getPersistedOhlcvMissingFields()) {
                addUnique(field, fields);
            }
        }
        return fields;
    }

    private PersistedOhlcvReadinessResult fallbackUnknownReadiness(
            String symbol,
            String timeframe,
            String reasonText,
            List<String> missingFields
    ) {
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setSymbol(symbol);
        result.setTimeframe(timeframe);
        result.setRequiredWindowSize(DEFAULT_REQUIRED_WINDOW_SIZE);
        result.setStatus(PersistedOhlcvReadinessStatus.UNKNOWN);
        result.setStaleReasonCode(PersistedOhlcvStaleReasonCode.POLICY_NOT_CONFIGURED);
        result.setStaleReasonText(reasonText);
        result.setMissingFields(missingFields);
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);
        return result;
    }

    private long maxReadLagMs(String timeframe) {
        Long intervalMs = parseTimeframeMs(timeframe);
        if (intervalMs == null) {
            return FALLBACK_MAX_READ_LAG_MS;
        }
        return Math.max(FALLBACK_MAX_READ_LAG_MS, intervalMs * 2);
    }

    private Long parseTimeframeMs(String timeframe) {
        if (!hasText(timeframe) || timeframe.length() < 2) {
            return null;
        }
        String unit = timeframe.substring(timeframe.length() - 1);
        String amountText = timeframe.substring(0, timeframe.length() - 1);
        long amount;
        try {
            amount = Long.parseLong(amountText);
        } catch (NumberFormatException e) {
            return null;
        }
        if (amount <= 0) {
            return null;
        }
        return switch (unit) {
            case "m" -> amount * 60_000L;
            case "h" -> amount * 60L * 60_000L;
            case "d" -> amount * 24L * 60L * 60_000L;
            default -> null;
        };
    }

    private List<String> missingFields(String symbol, String timeframe, DecisionResultVO decision) {
        List<String> fields = new ArrayList<>();
        if (decision == null) {
            fields.add("decision");
        }
        if (!hasText(symbol)) {
            fields.add("symbol");
        }
        if (!hasText(timeframe)) {
            fields.add("timeframe");
        }
        fields.add("persistedOhlcvWindow");
        fields.add("klineItems");
        fields.add("klineWindow");
        fields.add("klineFreshness");
        fields.add("staleStatus");
        fields.add("runtimeLatestPriceSource");
        fields.add("dataQualityScoreSource");
        fields.add("entryPriceSource");
        fields.add("stopPriceSource");
        fields.add("tpPriceSources");
        fields.add("rrSource");
        fields.add("liquiditySource");
        fields.add("multiTimeframeSource");
        fields.add("eventSource");
        fields.add("wickSource");
        return fields;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private void addUnique(String field, List<String> fields) {
        if (!fields.contains(field)) {
            fields.add(field);
        }
    }
}
