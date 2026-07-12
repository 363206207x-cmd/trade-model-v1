package org.example.trademodel.localreal;

import jakarta.annotation.PreDestroy;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.service.PersistedOhlcvIngestionScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Profile("local-real")
public class LocalRealDataCoordinator {
    public static final List<String> SYMBOLS = List.of(
            "BTCUSDT", "ETHUSDT", "SOLUSDT", "BNBUSDT", "XRPUSDT", "DOGEUSDT");
    public static final List<String> TIMEFRAMES = List.of("5m", "15m", "1h", "4h");

    private static final Logger log = LoggerFactory.getLogger(LocalRealDataCoordinator.class);
    private final PersistedOhlcvIngestionScheduler ingestionScheduler;
    private final AnalysisSchedulerService analysisSchedulerService;
    private final LocalRealReadinessService readiness;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "local-real-bootstrap");
        thread.setDaemon(true);
        return thread;
    });

    public LocalRealDataCoordinator(PersistedOhlcvIngestionScheduler ingestionScheduler,
                                    AnalysisSchedulerService analysisSchedulerService,
                                    LocalRealReadinessService readiness) {
        this.ingestionScheduler = ingestionScheduler;
        this.analysisSchedulerService = analysisSchedulerService;
        this.readiness = readiness;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        worker.submit(this::bootstrap);
    }

    void bootstrap() {
        readiness.transition(LocalRealReadinessState.MARKET_BOOTSTRAPPING, "PUBLIC_OHLCV_BOOTSTRAP_RUNNING");
        boolean ingestionAccepted = true;
        for (String symbol : SYMBOLS) {
            for (String timeframe : TIMEFRAMES) {
                OhlcvIngestionResult result = ingestionScheduler.ingestOne(symbol, timeframe);
                if (result == null || (!result.ready() && result.insertedCount() == 0 && result.idempotentCount() == 0)) {
                    ingestionAccepted = false;
                    log.warn("local-real bootstrap incomplete symbol={} timeframe={} reasons={}", symbol, timeframe,
                            result == null ? List.of("INGESTION_RESULT_MISSING") : result.reasonCodes());
                }
            }
        }

        boolean allReady = SYMBOLS.stream().allMatch(analysisSchedulerService::marketDataReady);
        if (!allReady) {
            readiness.transition(LocalRealReadinessState.DEGRADED,
                    ingestionAccepted ? "MARKET_WINDOW_INCOMPLETE" : "PUBLIC_OHLCV_BOOTSTRAP_DEGRADED");
            return;
        }

        runInitialAnalysis();
    }

    @Scheduled(initialDelay = 60000L, fixedDelay = 60000L)
    public void recoverWhenMarketBecomesReady() {
        if (readiness.state() != LocalRealReadinessState.DEGRADED) {
            return;
        }
        if (SYMBOLS.stream().allMatch(analysisSchedulerService::marketDataReady)) {
            runInitialAnalysis();
        }
    }

    private void runInitialAnalysis() {
        readiness.transition(LocalRealReadinessState.MARKET_READY, "ALL_REQUIRED_MARKET_WINDOWS_READY");
        readiness.transition(LocalRealReadinessState.ANALYSIS_RUNNING, "INITIAL_RULE_ANALYSIS_RUNNING");
        List<AnalysisRunResult> results = analysisSchedulerService.runScheduledCycle();
        long completed = results.stream().filter(result -> result != null && result.isSuccessfulAnalysisAvailable()).count();
        if (completed == SYMBOLS.size()) {
            readiness.transition(LocalRealReadinessState.DASHBOARD_READY, "REAL_DATA_AVAILABLE");
        } else {
            readiness.transition(LocalRealReadinessState.DEGRADED, "INITIAL_ANALYSIS_INCOMPLETE");
        }
    }

    @PreDestroy
    public void shutdown() {
        worker.shutdownNow();
    }
}
