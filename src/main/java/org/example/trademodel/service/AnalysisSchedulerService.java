package org.example.trademodel.service;

import org.example.trademodel.analysisrun.AnalysisRunCommand;
import org.example.trademodel.analysisrun.AnalysisRunOrchestrator;
import org.example.trademodel.analysisrun.AnalysisRunProperties;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.requestcontext.RequestIdSupport;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisSchedulerService {

    private final AnalysisRunOrchestrator analysisRunOrchestrator;
    private final AnalysisRunProperties properties;

    public AnalysisSchedulerService(AnalysisRunOrchestrator analysisRunOrchestrator,
                                    AnalysisRunProperties properties) {
        this.analysisRunOrchestrator = analysisRunOrchestrator;
        this.properties = properties;
    }

    public ApiResponse<AssetAnalysisVO> executeAnalysis(String symbol, String timeframe, String triggerType) {
        AnalysisRunResult result;
        if (triggerType != null && triggerType.startsWith("HOT_RESET:")) {
            String eventId = triggerType.substring("HOT_RESET:".length());
            result = runHotResetRebuild(symbol, timeframe, eventId, null, null);
        } else if ("SCHEDULED".equalsIgnoreCase(triggerType)) {
            String reference = "SCHEDULED:" + LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
            result = analysisRunOrchestrator.run(AnalysisRunCommand.scheduled(
                    symbol, timeframe, RequestIdSupport.generate(), reference));
        } else {
            result = analysisRunOrchestrator.run(AnalysisRunCommand.manual(
                    symbol, timeframe, RequestIdSupport.generate(), null));
        }
        return ApiResponse.success(result.getStatus(), analysisOrMinimal(result));
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
        String reference = "SCHEDULED:" + LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        List<AnalysisRunResult> results = new ArrayList<>();
        for (String symbol : properties.getScheduler().getSymbols()) {
            for (String timeframe : properties.getScheduler().getTimeframes()) {
                results.add(analysisRunOrchestrator.run(AnalysisRunCommand.scheduled(
                        symbol, timeframe, RequestIdSupport.generate(), reference)));
            }
        }
        return results;
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
        return status;
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
}
