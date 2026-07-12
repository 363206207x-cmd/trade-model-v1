package org.example.trademodel.localreal;

import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.market.client.impl.RoutedPublicOhlcvProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Profile("local-real")
public class LocalRealDataStatusService {
    private final LocalRealReadinessService readiness;
    private final PersistedOhlcvBarMapper ohlcvMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final RoutedPublicOhlcvProvider routedProvider;

    public LocalRealDataStatusService(LocalRealReadinessService readiness,
                                      PersistedOhlcvBarMapper ohlcvMapper,
                                      AnalysisRunMapper analysisRunMapper,
                                      DecisionResultMapper decisionResultMapper,
                                      RoutedPublicOhlcvProvider routedProvider) {
        this.readiness = readiness;
        this.ohlcvMapper = ohlcvMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.routedProvider = routedProvider;
    }

    public Map<String, Object> status() {
        long closedBars = ohlcvMapper.countAllClosedBars();
        PersistedOhlcvBarDO latest = ohlcvMapper.selectLatestClosedBar();
        int completedAssets = value(analysisRunMapper.countLocalRealSuccessfulSymbols());
        long readyAssets = readiness.readyAssetCount();
        boolean dashboardReady = readiness.state() == LocalRealReadinessState.DASHBOARD_READY
                && readyAssets >= 5 && completedAssets >= 5;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "LOCAL_REAL_DATA");
        response.put("state", readiness.state().name());
        response.put("health", "UP");
        response.put("failureReasonCode", readiness.reasonCode());
        response.put("updatedAt", readiness.updatedAt());
        response.put("database", Map.of("type", "H2_FILE", "persistent", true));

        Map<String, Object> market = new LinkedHashMap<>();
        market.put("provider", routedProvider.primaryProvider());
        Map<String, Object> providers = providerStatuses();
        List<Map<String, Object>> assets = assetStatuses();
        market.put("providers", providers);
        market.put("enabled", true);
        market.put("assetCount", LocalRealDataCoordinator.SYMBOLS.size());
        market.put("readyAssetCount", readyAssets);
        market.put("degradedAssets", readiness.assets().values().stream()
                .filter(item -> item.state() != LocalRealAssetReadinessState.READY)
                .map(LocalRealAssetReadiness::symbol).toList());
        market.put("timeframeCount", LocalRealDataCoordinator.TIMEFRAMES.size());
        market.put("closedBarCount", closedBars);
        market.put("latestClosedBarAt", latest == null || latest.getCloseTimeMs() == null
                ? null : java.time.Instant.ofEpochMilli(latest.getCloseTimeMs()));
        market.put("freshnessStatus", latest == null ? "NO_DATA" : latest.getFreshnessStatus());
        market.put("assets", assets);
        response.put("marketData", market);
        response.put("providers", providers);
        response.put("assets", assets);

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

    private List<Map<String, Object>> assetStatuses() {
        return LocalRealDataCoordinator.SYMBOLS.stream().map(symbol -> {
            PersistedOhlcvBarDO latest = ohlcvMapper.selectLatestClosedBarBySymbol(symbol);
            LocalRealAssetReadiness item = readiness.asset(symbol);
            Map<String, Object> asset = new LinkedHashMap<>();
            asset.put("symbol", symbol);
            asset.put("provider", latest == null ? routedProvider.primaryProvider() : latest.getProvider());
            asset.put("requestPair", routedProvider.requestPair(symbol));
            asset.put("status", item == null ? LocalRealAssetReadinessState.NO_DATA.name() : item.state().name());
            asset.put("reasonCode", item == null ? "NO_DATA" : item.reasonCode());
            asset.put("closedBarCount", ohlcvMapper.countClosedBarsBySymbol(symbol));
            asset.put("latestClosedBarAt", latest == null || latest.getCloseTimeMs() == null
                    ? null : java.time.Instant.ofEpochMilli(latest.getCloseTimeMs()));
            return asset;
        }).toList();
    }

    private Map<String, Object> providerStatuses() {
        Map<String, Object> providers = new LinkedHashMap<>();
        providers.put("primary", routedProvider.primaryProvider());
        providers.put("krakenPairCacheState", routedProvider.krakenPairCacheState().name());
        providers.putAll(routedProvider.health());
        return providers;
    }

    private static int value(Integer value) {
        return value == null ? 0 : value;
    }
}
