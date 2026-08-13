package org.example.trademodel.localreal;

import jakarta.annotation.PreDestroy;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.service.PersistedOhlcvIngestionScheduler;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Profile("local-real")
public class LocalRealDataCoordinator {
    public static final List<String> TIMEFRAMES = List.of("5m", "15m", "1h", "4h");

    private static final Logger log = LoggerFactory.getLogger(LocalRealDataCoordinator.class);
    private final PersistedOhlcvIngestionScheduler ingestionScheduler;
    private final AnalysisSchedulerService analysisSchedulerService;
    private final AssetPoolService assetPoolService;
    private final LocalRealReadinessService readiness;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "local-real-bootstrap");
        thread.setDaemon(true);
        return thread;
    });

    public LocalRealDataCoordinator(PersistedOhlcvIngestionScheduler ingestionScheduler,
                                    AnalysisSchedulerService analysisSchedulerService,
                                    AssetPoolService assetPoolService,
                                    LocalRealReadinessService readiness) {
        this.ingestionScheduler = ingestionScheduler;
        this.analysisSchedulerService = analysisSchedulerService;
        this.assetPoolService = assetPoolService;
        this.readiness = readiness;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        worker.submit(this::bootstrap);
    }

    void bootstrap() {
        List<String> symbols = trackedSymbols();
        readiness.retainAssets(symbols);
        if (symbols.isEmpty()) {
            readiness.transition(LocalRealReadinessState.DEGRADED, "ASSET_POOL_EMPTY");
            return;
        }
        readiness.transition(LocalRealReadinessState.MARKET_BOOTSTRAPPING, "PUBLIC_OHLCV_BOOTSTRAP_RUNNING");
        Map<String, String> failures = new LinkedHashMap<>();
        for (String symbol : symbols) {
            readiness.updateAsset(symbol, LocalRealAssetReadinessState.BOOTSTRAPPING, null,
                    "PUBLIC_OHLCV_BOOTSTRAP_RUNNING");
            if (analysisSchedulerService.marketDataReady(symbol)) {
                log.info("local-real reuses persisted market window symbol={}", symbol);
                continue;
            }
            for (String timeframe : TIMEFRAMES) {
                OhlcvIngestionResult result = ingestionScheduler.ingestOne(symbol, timeframe);
                if (result == null || (!result.ready() && result.insertedCount() == 0 && result.idempotentCount() == 0)) {
                    String reason = result == null || result.reasonCodes().isEmpty()
                            ? "INGESTION_RESULT_MISSING" : result.reasonCodes().get(0);
                    failures.putIfAbsent(symbol, reason);
                    log.warn("local-real bootstrap incomplete symbol={} timeframe={} reasons={}", symbol, timeframe,
                            result == null ? List.of("INGESTION_RESULT_MISSING") : result.reasonCodes());
                }
            }
        }

        Set<String> marketReady = evaluateMarketReadiness(symbols, failures);
        if (marketReady.isEmpty()) {
            readiness.transition(LocalRealReadinessState.DEGRADED, "PUBLIC_OHLCV_BOOTSTRAP_DEGRADED");
            return;
        }
        runInitialAnalysis(marketReady);
    }

    @Scheduled(initialDelay = 60000L, fixedDelay = 60000L)
    public void recoverWhenMarketBecomesReady() {
        if (readiness.state() != LocalRealReadinessState.DEGRADED
                && readiness.state() != LocalRealReadinessState.DASHBOARD_PARTIAL) {
            return;
        }
        List<String> symbols = trackedSymbols();
        readiness.retainAssets(symbols);
        Set<String> marketReady = evaluateMarketReadiness(symbols, Map.of());
        if (!marketReady.isEmpty()) {
            runInitialAnalysis(marketReady);
        }
    }

    private Set<String> evaluateMarketReadiness(List<String> symbols, Map<String, String> failures) {
        Set<String> ready = new LinkedHashSet<>();
        for (String symbol : symbols) {
            if (analysisSchedulerService.marketDataReady(symbol)) {
                ready.add(symbol);
                readiness.updateAsset(symbol, LocalRealAssetReadinessState.BOOTSTRAPPING, null,
                        "MARKET_WINDOW_READY_ANALYSIS_PENDING");
            } else {
                LocalRealAssetReadiness previous = readiness.asset(symbol);
                String preserved = previous != null && previous.state() == LocalRealAssetReadinessState.UNAVAILABLE
                        ? previous.reasonCode() : "MARKET_WINDOW_INCOMPLETE";
                String reason = failures.getOrDefault(symbol, preserved);
                readiness.updateAsset(symbol, unavailable(reason)
                        ? LocalRealAssetReadinessState.UNAVAILABLE : LocalRealAssetReadinessState.DEGRADED,
                        null, reason);
            }
        }
        return ready;
    }

    private void runInitialAnalysis(Set<String> marketReady) {
        readiness.transition(LocalRealReadinessState.MARKET_READY, "AVAILABLE_MARKET_WINDOWS_READY");
        readiness.transition(LocalRealReadinessState.ANALYSIS_RUNNING, "INITIAL_RULE_ANALYSIS_RUNNING");
        List<AnalysisRunResult> results = analysisSchedulerService.runScheduledCycle();
        Set<String> completedSymbols = new LinkedHashSet<>();
        for (AnalysisRunResult result : results) {
            if (result != null && result.isSuccessfulAnalysisAvailable() && result.getSymbol() != null) {
                completedSymbols.add(result.getSymbol().trim().toUpperCase(java.util.Locale.ROOT));
            }
        }
        for (String symbol : marketReady) {
            if (completedSymbols.contains(symbol)) {
                readiness.updateAsset(symbol, LocalRealAssetReadinessState.READY, null, "REAL_DATA_AVAILABLE");
            } else {
                readiness.updateAsset(symbol, LocalRealAssetReadinessState.DEGRADED, null,
                        "INITIAL_ANALYSIS_INCOMPLETE");
            }
        }
        if (!marketReady.isEmpty() && completedSymbols.containsAll(marketReady)) {
            readiness.transition(LocalRealReadinessState.DASHBOARD_READY, "REAL_DATA_AVAILABLE");
        } else if (!completedSymbols.isEmpty()) {
            readiness.transition(LocalRealReadinessState.DASHBOARD_PARTIAL, "PARTIAL_REAL_DATA_AVAILABLE");
        } else {
            readiness.transition(LocalRealReadinessState.DEGRADED, "INITIAL_ANALYSIS_INCOMPLETE");
        }
    }

    private static boolean unavailable(String reason) {
        return reason != null && (reason.contains("PAIR_NOT_SUPPORTED")
                || reason.contains("GEO_RESTRICTED")
                || reason.contains("PROVIDER_UNAVAILABLE_FOR_LOCATION"));
    }

    private List<String> trackedSymbols() {
        if (assetPoolService == null) {
            return List.of();
        }
        List<String> symbols = assetPoolService.listScanSymbols();
        if (symbols == null) {
            return List.of();
        }
        return symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    @PreDestroy
    public void shutdown() {
        worker.shutdownNow();
    }
}
