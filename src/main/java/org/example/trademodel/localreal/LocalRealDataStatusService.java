package org.example.trademodel.localreal;

import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Profile("local-real")
public class LocalRealDataStatusService {
    private final LocalRealReadinessService readiness;
    private final PersistedOhlcvBarMapper ohlcvMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final DecisionResultMapper decisionResultMapper;

    public LocalRealDataStatusService(LocalRealReadinessService readiness,
                                      PersistedOhlcvBarMapper ohlcvMapper,
                                      AnalysisRunMapper analysisRunMapper,
                                      DecisionResultMapper decisionResultMapper) {
        this.readiness = readiness;
        this.ohlcvMapper = ohlcvMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.decisionResultMapper = decisionResultMapper;
    }

    public Map<String, Object> status() {
        long closedBars = ohlcvMapper.countAllClosedBars();
        PersistedOhlcvBarDO latest = ohlcvMapper.selectLatestClosedBar();
        int completedAssets = value(analysisRunMapper.countLocalRealSuccessfulSymbols());
        boolean dashboardReady = readiness.state() == LocalRealReadinessState.DASHBOARD_READY
                && closedBars >= 2400 && completedAssets >= 6;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "LOCAL_REAL_DATA");
        response.put("state", readiness.state().name());
        response.put("health", "UP");
        response.put("failureReasonCode", readiness.reasonCode());
        response.put("updatedAt", readiness.updatedAt());
        response.put("database", Map.of("type", "H2_FILE", "persistent", true));

        Map<String, Object> market = new LinkedHashMap<>();
        market.put("provider", "BINANCE_PUBLIC");
        market.put("enabled", true);
        market.put("assetCount", LocalRealDataCoordinator.SYMBOLS.size());
        market.put("timeframeCount", LocalRealDataCoordinator.TIMEFRAMES.size());
        market.put("closedBarCount", closedBars);
        market.put("latestClosedBarAt", latest == null || latest.getCloseTimeMs() == null
                ? null : java.time.Instant.ofEpochMilli(latest.getCloseTimeMs()));
        market.put("freshnessStatus", latest == null ? "NO_DATA" : latest.getFreshnessStatus());
        response.put("marketData", market);

        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("completedAssetCount", completedAssets);
        analysis.put("latestAnalysisAt", analysisRunMapper.selectLatestSuccessfulCompletedAt());
        analysis.put("latestDecisionAt", decisionResultMapper.selectLastDecisionTime());
        analysis.put("mode", "RULE_ONLY");
        response.put("analysis", analysis);
        response.put("dashboard", Map.of("ready", dashboardReady,
                "reason", dashboardReady ? "REAL_DATA_AVAILABLE" : readiness.reasonCode()));
        response.put("ai", Map.of("enabled", false, "status", "DISABLED"));
        response.put("reviewOnly", true);
        response.put("notTradeInstruction", true);
        response.put("notAutoTrading", true);
        response.put("notOrderExecution", true);
        response.put("notUserPositionCreation", true);
        response.put("notExternalPush", true);
        return response;
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }
}
