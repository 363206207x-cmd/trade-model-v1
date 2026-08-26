package org.example.trademodel.service;

import org.example.trademodel.analysisrun.AnalysisRunProperties;
import org.example.trademodel.analysisrun.AnalysisRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarketDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(MarketDataScheduler.class);

    private final AnalysisSchedulerService analysisSchedulerService;
    private final AnalysisRunProperties properties;
    private final boolean schedulersEnabled;
    private final boolean marketDataSchedulerEnabled;

    public MarketDataScheduler(AnalysisSchedulerService analysisSchedulerService,
                               AnalysisRunProperties properties,
                               @Value("${trade-model.schedulers.enabled:false}") boolean schedulersEnabled,
                               @Value("${trade-model.schedulers.market-data.enabled:false}") boolean marketDataSchedulerEnabled) {
        this.analysisSchedulerService = analysisSchedulerService;
        this.properties = properties;
        this.schedulersEnabled = schedulersEnabled;
        this.marketDataSchedulerEnabled = marketDataSchedulerEnabled;
    }

    @Scheduled(
            initialDelayString = "${trade-model.analysis.scheduler.initial-delay-ms:60000}",
            fixedDelayString = "${trade-model.analysis.scheduler.fixed-delay-ms:60000}")
    public void fetchRealMarketDataScheduled() {
        if (!scheduledExecutionEnabled()) {
            return;
        }
        if (!properties.getScheduler().isEnabled()) {
            log.debug("analysis scheduler disabled by default; skipping scheduled analysis cycle");
            return;
        }
        List<AnalysisRunResult> results = analysisSchedulerService.runScheduledCycle();
        log.info("analysis scheduler cycle completed count={}", results.size());
    }

    private boolean scheduledExecutionEnabled() {
        return schedulersEnabled && marketDataSchedulerEnabled;
    }
}
