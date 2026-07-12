package org.example.trademodel.service;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunInputException;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunProperties;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.analysisrun.AnalysisTimePolicy;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisSchedulerService {

    private final AnalysisRunOrchestrator analysisRunOrchestrator;
    private final AnalysisRunProperties properties;
    private final Clock clock;
    private PersistedOhlcvQueryService persistedOhlcvQueryService;

    public AnalysisSchedulerService(AnalysisRunOrchestrator analysisRunOrchestrator,
                                    AnalysisRunProperties properties) {
        this(analysisRunOrchestrator, properties, Clock.systemUTC());
    }

    @Autowired
    public AnalysisSchedulerService(AnalysisRunOrchestrator analysisRunOrchestrator,
                                    AnalysisRunProperties properties,
                                    Clock analysisRunClock) {
        this.analysisRunOrchestrator = analysisRunOrchestrator;
        this.properties = properties;
        this.clock = analysisRunClock != null ? analysisRunClock : Clock.systemUTC();
    }

    @Autowired(required = false)
    void setPersistedOhlcvQueryService(PersistedOhlcvQueryService persistedOhlcvQueryService) {
        this.persistedOhlcvQueryService = persistedOhlcvQueryService;
    }

    public ApiResponse<AssetAnalysisVO> executeAnalysis(String symbol, String timeframe, String triggerType) {
        AnalysisRunResult result;
        if (triggerType != null && triggerType.startsWith("HOT_RESET:")) {
            String eventId = triggerType.substring("HOT_RESET:".length());
            result = runHotResetRebuild(symbol, timeframe, eventId, null, null);
        } else if ("SCHEDULED".equalsIgnoreCase(triggerType)) {
            validateInput(symbol, timeframe);
            String reference = "SCHEDULED:" + LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
            result = analysisRunOrchestrator.run(AnalysisRunCommand.scheduled(
                    symbol, timeframe, RequestIdSupport.generate(), reference));
        } else {
            validateInput(symbol, timeframe);
            result = analysisRunOrchestrator.run(AnalysisRunCommand.manual(
                    symbol, timeframe, RequestIdSupport.generate(), null));
        }
        if (result != null && result.isSuccessfulAnalysisAvailable()) {
            return ApiResponse.success(result.getStatus(), analysisOrMinimal(result));
        }
        return ApiResponse.fail(failureMessage(result));
    }

    public AnalysisRunResult runManual(String symbol, String timeframe, String requestId, String analysisTime) {
        return analysisRunOrchestrator.run(AnalysisRunCommand.manual(symbol, timeframe, requestId, analysisTime));
    }

    public AnalysisRunResult runMarketDataCompatibility(String symbol, String timeframe, String requestId) {
        return analysisRunOrchestrator.run(AnalysisRunCommand.marketDataCompatibility(symbol, timeframe, requestId));
    }

    public AnalysisRunResult runHotResetRebuild(String symbol, String timeframe, String eventId,
                                                String parentAnalysisId, String parentTraceId) {
        return analysisRunOrchestrator.run(AnalysisRunCommand.hotResetRebuild(
                symbol, timeframe, eventId, RequestIdSupport.generate(), parentAnalysisId, parentTraceId));
    }

    public List<AnalysisRunResult> runScheduledCycle() {
        if (!properties.getScheduler().isEnabled()) {
            return List.of();
        }
        if (!schedulerConfigValid()) {
            return List.of();
        }
        String reference = "SCHEDULED:" + LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        List<AnalysisRunResult> results = new ArrayList<>();
        for (String symbol : properties.getScheduler().getSymbols()) {
            if (!marketDataReady(symbol)) {
                continue;
            }
            for (String timeframe : properties.getScheduler().getTimeframes()) {
                results.add(analysisRunOrchestrator.run(AnalysisRunCommand.scheduled(
                        symbol, timeframe, RequestIdSupport.generate(), reference)));
            }
        }
        return results;
    }

    public boolean marketDataReady(String symbol) {
        int requiredBars = properties.getScheduler().getRequiredClosedBars();
        List<String> requiredTimeframes = properties.getScheduler().getRequiredMarketTimeframes();
        if (requiredBars <= 0 && (requiredTimeframes == null || requiredTimeframes.isEmpty())) {
            return true;
        }
        if (requiredBars <= 0 || requiredTimeframes == null || requiredTimeframes.isEmpty()
                || persistedOhlcvQueryService == null) {
            return false;
        }
        for (String timeframe : requiredTimeframes) {
            long maxReadLagMs = maxReadLagMs(timeframe);
            if (!persistedOhlcvQueryService.evaluateReadiness(symbol, timeframe, requiredBars, maxReadLagMs).isFresh()) {
                return false;
            }
        }
        return true;
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("enabled", properties.getScheduler().isEnabled());
        status.put("symbols", properties.getScheduler().getSymbols());
        status.put("timeframes", properties.getScheduler().getTimeframes());
        status.put("leaseSeconds", properties.getIdempotency().getLeaseSeconds());
        status.put("maxRecoveryAttempts", properties.getIdempotency().getMaxRecoveryAttempts());
        status.put("entryUnified", true);
        status.put("inMemoryCacheRemoved", true);
        status.put("manualThreadRemoved", true);
        status.put("reviewOnly", true);
        status.put("notAutoTrading", true);
        status.put("notOrderExecution", true);
        status.put("notUserPositionCreation", true);
        status.put("notUserPositionMutation", true);
        status.put("supportedTimeframes", AnalysisTimePolicy.supportedTimeframes());
        status.put("requiredMarketTimeframes", properties.getScheduler().getRequiredMarketTimeframes());
        status.put("requiredClosedBars", properties.getScheduler().getRequiredClosedBars());
        status.put("configValid", schedulerConfigValid());
        return status;
    }

    private static long maxReadLagMs(String timeframe) {
        return switch (timeframe) {
            case "5m" -> 11L * 60_000L;
            case "15m" -> 31L * 60_000L;
            case "1h" -> 121L * 60_000L;
            case "4h" -> 481L * 60_000L;
            default -> 0L;
        };
    }

    private boolean schedulerConfigValid() {
        try {
            if (properties.getScheduler().getSymbols() == null || properties.getScheduler().getSymbols().isEmpty()
                    || properties.getScheduler().getTimeframes() == null || properties.getScheduler().getTimeframes().isEmpty()) {
                return false;
            }
            for (String symbol : properties.getScheduler().getSymbols()) {
                validateSymbol(symbol);
            }
            for (String timeframe : properties.getScheduler().getTimeframes()) {
                AnalysisTimePolicy.requireSupportedTimeframe(timeframe);
            }
            return true;
        } catch (AnalysisRunInputException ex) {
            return false;
        }
    }

    private static void validateInput(String symbol, String timeframe) {
        validateSymbol(symbol);
        AnalysisTimePolicy.requireSupportedTimeframe(timeframe);
    }

    private static String validateSymbol(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new AnalysisRunInputException("SYMBOL_REQUIRED", "symbol is required");
        }
        return raw.trim().toUpperCase(Locale.ROOT);
    }

    private static AssetAnalysisVO analysisOrMinimal(AnalysisRunResult result) {
        if (result.getAnalysis() != null) {
            return result.getAnalysis();
        }
        AssetAnalysisVO vo = new AssetAnalysisVO();
        vo.setAnalysisId(result.getAnalysisId());
        vo.setSymbol(result.getSymbol());
        vo.setTimeframe(result.getTimeframe());
        vo.setAnalysisTime(LocalDateTime.now().toString());
        return vo;
    }

    private static String failureMessage(AnalysisRunResult result) {
        if (result == null) {
            return "ANALYSIS_REBUILD_RESULT_MISSING";
        }
        String status = hasText(result.getStatus()) ? result.getStatus() : "UNKNOWN";
        String reason = hasText(result.getReasonCode()) ? result.getReasonCode() : "ANALYSIS_REBUILD_NOT_EXECUTED";
        String message = hasText(result.getMessage()) ? result.getMessage() : "analysis rebuild did not execute successfully";
        return status + ": " + reason + ": " + message;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
