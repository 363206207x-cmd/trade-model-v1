package org.example.trademodel.localreal;

import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.dto.ohlcv.PublicProviderHealthSnapshot;
import org.example.trademodel.market.PersistedRealMarketEnvironmentAssessment;
import org.example.trademodel.market.PersistedRealMarketEnvironmentService;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.market.client.impl.RoutedPublicOhlcvProvider;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Profile("local-real")
public class LocalRealDataStatusService {
    private static final long DEFAULT_FRESHNESS_TOLERANCE_MS = 30_000L;
    private final LocalRealReadinessService readiness;
    private final PersistedOhlcvBarMapper ohlcvMapper;
    private final AnalysisRunMapper analysisRunMapper;
    private final DecisionResultMapper decisionResultMapper;
    private final RoutedPublicOhlcvProvider routedProvider;
    private final PersistedRealMarketEnvironmentService realMarketEnvironmentService;
    private AssetPoolService assetPoolService;
    private final Clock clock;
    private final long freshnessToleranceMs;

    public LocalRealDataStatusService(LocalRealReadinessService readiness,
                                      PersistedOhlcvBarMapper ohlcvMapper,
                                      AnalysisRunMapper analysisRunMapper,
                                      DecisionResultMapper decisionResultMapper,
                                      RoutedPublicOhlcvProvider routedProvider,
                                      PersistedRealMarketEnvironmentService realMarketEnvironmentService) {
        this(readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper, routedProvider,
                realMarketEnvironmentService, Clock.systemUTC(), DEFAULT_FRESHNESS_TOLERANCE_MS);
    }

    @Autowired
    public LocalRealDataStatusService(LocalRealReadinessService readiness,
                                      PersistedOhlcvBarMapper ohlcvMapper,
                                      AnalysisRunMapper analysisRunMapper,
                                      DecisionResultMapper decisionResultMapper,
                                      RoutedPublicOhlcvProvider routedProvider,
                                      PersistedRealMarketEnvironmentService realMarketEnvironmentService,
                                      @Value("${trade-model.ohlcv.freshness-tolerance-ms:30000}")
                                      long freshnessToleranceMs) {
        this(readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper, routedProvider,
                realMarketEnvironmentService, Clock.systemUTC(), freshnessToleranceMs);
    }

    LocalRealDataStatusService(LocalRealReadinessService readiness,
                               PersistedOhlcvBarMapper ohlcvMapper,
                               AnalysisRunMapper analysisRunMapper,
                               DecisionResultMapper decisionResultMapper,
                               RoutedPublicOhlcvProvider routedProvider,
                               PersistedRealMarketEnvironmentService realMarketEnvironmentService,
                               Clock clock) {
        this(readiness, ohlcvMapper, analysisRunMapper, decisionResultMapper, routedProvider,
                realMarketEnvironmentService, clock, DEFAULT_FRESHNESS_TOLERANCE_MS);
    }

    LocalRealDataStatusService(LocalRealReadinessService readiness,
                               PersistedOhlcvBarMapper ohlcvMapper,
                               AnalysisRunMapper analysisRunMapper,
                               DecisionResultMapper decisionResultMapper,
                               RoutedPublicOhlcvProvider routedProvider,
                               PersistedRealMarketEnvironmentService realMarketEnvironmentService,
                               Clock clock,
                               long freshnessToleranceMs) {
        this.readiness = readiness;
        this.ohlcvMapper = ohlcvMapper;
        this.analysisRunMapper = analysisRunMapper;
        this.decisionResultMapper = decisionResultMapper;
        this.routedProvider = routedProvider;
        this.realMarketEnvironmentService = realMarketEnvironmentService;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.freshnessToleranceMs = Math.max(0L, freshnessToleranceMs);
    }

    @Autowired(required = false)
    void setAssetPoolService(AssetPoolService assetPoolService) {
        this.assetPoolService = assetPoolService;
    }

    public Map<String, Object> status() {
        String persistedProvider = primaryPersistedProvider();
        long closedBars = ohlcvMapper.countAllClosedBarsBySource(persistedProvider, "SPOT");
        PersistedOhlcvBarDO latest = ohlcvMapper.selectLatestClosedBarBySource(persistedProvider, "SPOT");
        int completedAssets = value(analysisRunMapper.countLocalRealSuccessfulSymbols());
        long readyAssets = readiness.readyAssetCount();
        int trackedAssetCount = trackedSymbols().size();
        Map<String, Object> providers = providerStatuses();
        ProviderReadinessSnapshot providerReadiness = providerReadinessSnapshot(
                latest, completedAssets, readyAssets, trackedAssetCount, providers);
        boolean dashboardReady = providerReadiness.dashboardReady();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("mode", "LOCAL_REAL_DATA");
        response.put("state", readiness.state().name());
        response.put("health", "UP");
        response.put("failureReasonCode", readiness.reasonCode());
        response.put("updatedAt", readiness.updatedAt());
        response.put("database", Map.of("type", "H2_FILE", "persistent", true));

        Map<String, Object> market = new LinkedHashMap<>();
        market.put("provider", routedProvider.primaryProvider());
        List<Map<String, Object>> assets = assetStatuses();
        market.put("providers", providers);
        market.put("enabled", true);
        market.put("assetCount", trackedAssetCount);
        market.put("readyAssetCount", readyAssets);
        market.put("degradedAssets", readiness.assets().values().stream()
                .filter(item -> item.state() != LocalRealAssetReadinessState.READY)
                .map(LocalRealAssetReadiness::symbol).toList());
        market.put("timeframeCount", LocalRealDataCoordinator.TIMEFRAMES.size());
        market.put("closedBarCount", closedBars);
        market.put("latestClosedBarAt", closedBarAt(latest));
        market.put("freshnessStatus", providerReadiness.freshnessStatus());
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
                "reason", providerReadiness.reasonCode()));
        response.put("ai", Map.of("enabled", false, "status", "DISABLED"));
        response.put("reviewOnly", true);
        response.put("notTradeInstruction", true);
        response.put("notAutoTrading", true);
        response.put("notOrderExecution", true);
        response.put("notUserPositionCreation", true);
        response.put("notExternalPush", true);
        return response;
    }

    public ProviderReadinessSnapshot providerReadinessSnapshot() {
        PersistedOhlcvBarDO latest = ohlcvMapper.selectLatestClosedBarBySource(
                primaryPersistedProvider(), "SPOT");
        int completedAssets = value(analysisRunMapper.countLocalRealSuccessfulSymbols());
        long readyAssets = readiness.readyAssetCount();
        return providerReadinessSnapshot(
                latest, completedAssets, readyAssets, trackedSymbols().size(), providerStatuses());
    }

    public Instant latestClosedBarAt() {
        return closedBarAt(ohlcvMapper.selectLatestClosedBarBySource(
                primaryPersistedProvider(), "SPOT"));
    }

    private ProviderReadinessSnapshot providerReadinessSnapshot(PersistedOhlcvBarDO latest,
                                                                 int completedAssets,
                                                                 long readyAssets,
                                                                 int trackedAssetCount,
                                                                 Map<String, Object> providers) {
        String provider = normalizeProvider(routedProvider.primaryProvider());
        PublicProviderHealthSnapshot providerHealth = providerHealth(providers, provider);
        String freshness = currentFreshness(latest);
        boolean runtimeProviderUp = providerHealth != null
                && "UP".equalsIgnoreCase(providerHealth.status())
                && providerHealth.lastSuccessAt() != null
                && !providerHealth.circuitOpen();
        boolean providerUp = runtimeProviderUp || persistedProviderReady(latest, provider, freshness);
        boolean dashboardReady = readiness.state() == LocalRealReadinessState.DASHBOARD_READY
                && trackedAssetCount > 0
                && readyAssets >= trackedAssetCount
                && completedAssets >= trackedAssetCount
                && "FRESH".equals(freshness)
                && providerUp;
        String reason = dashboardReady
                ? "REAL_DATA_AVAILABLE"
                : readinessReason(freshness, providerHealth);
        return new ProviderReadinessSnapshot(provider, readiness.state().name(), dashboardReady,
                freshness, providerHealth, reason, readyAssets, completedAssets);
    }

    private static boolean persistedProviderReady(PersistedOhlcvBarDO latest,
                                                    String provider,
                                                    String freshness) {
        return latest != null
                && persistedProvider(provider).equalsIgnoreCase(latest.getProvider())
                && "READY".equalsIgnoreCase(latest.getSourceStatus())
                && "FRESH".equals(freshness);
    }

    private String currentFreshness(PersistedOhlcvBarDO latest) {
        if (latest == null || latest.getCloseTimeMs() == null) {
            return "NO_DATA";
        }
        if (!"READY".equalsIgnoreCase(latest.getSourceStatus())
                || !"FRESH".equalsIgnoreCase(latest.getFreshnessStatus())) {
            return "INVALID";
        }
        long ageMs = clock.millis() - latest.getCloseTimeMs();
        if (ageMs < 0) {
            return "INVALID";
        }
        Long timeframeMs = timeframeMs(latest.getTimeframe());
        if (timeframeMs == null) {
            return "INVALID";
        }
        return ageMs <= timeframeMs + freshnessToleranceMs ? "FRESH" : "STALE";
    }

    private static Long timeframeMs(String timeframe) {
        return switch (timeframe == null ? "" : timeframe) {
            case "5m" -> 5L * 60_000L;
            case "15m" -> 15L * 60_000L;
            case "1h" -> 60L * 60_000L;
            case "4h" -> 4L * 60L * 60_000L;
            default -> null;
        };
    }

    private String readinessReason(String freshness, PublicProviderHealthSnapshot providerHealth) {
        if (readiness.state() == LocalRealReadinessState.FAILED) {
            return "LOCAL_REAL_RUNTIME_FAILED";
        }
        if ("STALE".equals(freshness)) {
            return "LOCAL_REAL_MARKET_DATA_STALE";
        }
        if ("INVALID".equals(freshness)) {
            return "LOCAL_REAL_MARKET_DATA_INVALID";
        }
        if (providerHealth != null && providerHealth.circuitOpen()) {
            return "LOCAL_REAL_PROVIDER_CIRCUIT_OPEN";
        }
        if (providerHealth == null || !"UP".equalsIgnoreCase(providerHealth.status())) {
            return "LOCAL_REAL_PROVIDER_NOT_READY";
        }
        return readiness.reasonCode();
    }

    private static PublicProviderHealthSnapshot providerHealth(Map<String, Object> providers, String provider) {
        Object value = providers.get(provider.toLowerCase(Locale.ROOT));
        return value instanceof PublicProviderHealthSnapshot snapshot ? snapshot : null;
    }

    private static String normalizeProvider(String provider) {
        return provider == null || provider.isBlank()
                ? "UNKNOWN" : provider.trim().toUpperCase(Locale.ROOT);
    }

    private String primaryPersistedProvider() {
        return persistedProvider(routedProvider.primaryProvider());
    }

    private static String persistedProvider(String provider) {
        String normalized = normalizeProvider(provider);
        return "BINANCE".equals(normalized) ? "BINANCE_PUBLIC" : normalized;
    }

    private List<Map<String, Object>> assetStatuses() {
        return trackedSymbols().stream().map(symbol -> {
            String persistedProvider = primaryPersistedProvider();
            PersistedOhlcvBarDO latest = ohlcvMapper.selectLatestClosedBarBySymbolAndSource(
                    symbol, persistedProvider, "SPOT");
            LocalRealAssetReadiness item = readiness.asset(symbol);
            PersistedRealMarketEnvironmentAssessment marketAssessment =
                    realMarketEnvironmentService == null ? null : realMarketEnvironmentService.assess(symbol, "5m");
            AnalysisRunDO latestAnalysis = analysisRunMapper.selectLatestBySymbol(symbol);
            Map<String, Object> asset = new LinkedHashMap<>();
            asset.put("symbol", symbol);
            asset.put("provider", latest == null ? routedProvider.primaryProvider() : latest.getProvider());
            asset.put("requestPair", routedProvider.requestPair(symbol));
            asset.put("status", item == null ? LocalRealAssetReadinessState.NO_DATA.name() : item.state().name());
            asset.put("reasonCode", item == null ? "NO_DATA" : item.reasonCode());
            asset.put("marketDataStatus", marketDataStatus(latest, marketAssessment));
            asset.put("realMarketEnvironment", marketAssessment != null && marketAssessment.ready());
            asset.put("analysisStatus", analysisStatus(latestAnalysis));
            asset.put("latestAnalysisFailureCode", latestAnalysisFailureCode(latestAnalysis));
            asset.put("closedBarCount", ohlcvMapper.countClosedBarsBySymbolAndSource(
                    symbol, persistedProvider, "SPOT"));
            asset.put("latestClosedBarAt", closedBarAt(latest));
            return asset;
        }).toList();
    }

    private List<String> trackedSymbols() {
        List<String> symbols = assetPoolService == null
                ? readiness.assets().keySet().stream().toList()
                : assetPoolService.listScanSymbols();
        if (symbols == null) {
            return List.of();
        }
        return symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
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

    private static Instant closedBarAt(PersistedOhlcvBarDO bar) {
        return bar == null || bar.getCloseTimeMs() == null
                ? null : Instant.ofEpochMilli(bar.getCloseTimeMs());
    }

    private static String marketDataStatus(PersistedOhlcvBarDO latest,
                                           PersistedRealMarketEnvironmentAssessment assessment) {
        if (latest == null) return "MARKET_DATA_NOT_READY";
        if (assessment != null && assessment.ready()) return "READY";
        if ("STALE".equals(latest.getFreshnessStatus())) return "STALE";
        return assessment != null && assessment.reasonCode() != null
                ? assessment.reasonCode() : "REAL_MARKET_PROVENANCE_INCOMPLETE";
    }

    private static String analysisStatus(AnalysisRunDO latest) {
        if (latest == null || latest.getStatus() == null) return "WAITING";
        return "SUCCESS".equals(latest.getStatus()) ? "READY" : latest.getStatus();
    }

    private static String latestAnalysisFailureCode(AnalysisRunDO latest) {
        if (latest == null || !"FAILED".equals(latest.getStatus())) return null;
        if (latest.getErrorMessage() != null
                && latest.getErrorMessage().contains("REAL_MARKET_ENVIRONMENT_REQUIRED")) {
            return "REAL_MARKET_ENVIRONMENT_REQUIRED";
        }
        return latest.getErrorCode() == null || latest.getErrorCode().isBlank()
                ? "LATEST_ANALYSIS_FAILED" : latest.getErrorCode();
    }

    public record ProviderReadinessSnapshot(
            String provider,
            String runtimeState,
            boolean dashboardReady,
            String freshnessStatus,
            PublicProviderHealthSnapshot providerHealth,
            String reasonCode,
            long readyAssetCount,
            int completedAssetCount
    ) {
    }
}
