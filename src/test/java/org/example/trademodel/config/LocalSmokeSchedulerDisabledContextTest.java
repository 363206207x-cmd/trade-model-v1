package org.example.trademodel.config;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.service.MarketDataScheduler;
import org.example.trademodel.service.PositionSyncScheduler;
import org.example.trademodel.service.PushRecheckScheduler;
import org.example.trademodel.service.watchlist.WatchlistLowFrequencyScanScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class, properties = {
        "trade-model.schedulers.push-recheck.enabled=false",
        "trade-model.schedulers.position-sync.enabled=false",
        "trade-model.schedulers.market-data.enabled=false",
        "trade-model.schedulers.watchlist.enabled=false",
        "trade-model.analysis.scheduler.enabled=false"
})
class LocalSmokeSchedulerDisabledContextTest {

    @Autowired
    private PushRecheckScheduler pushRecheckScheduler;

    @Autowired
    private PositionSyncScheduler positionSyncScheduler;

    @Autowired
    private MarketDataScheduler marketDataScheduler;

    @Autowired
    private WatchlistLowFrequencyScanScheduler watchlistLowFrequencyScanScheduler;

    @Test
    void appContextStartsWithSchedulerExecutionDisabledAndBeansAvailable() {
        assertThat(pushRecheckScheduler).isNotNull();
        assertThat(positionSyncScheduler).isNotNull();
        assertThat(marketDataScheduler).isNotNull();
        assertThat(watchlistLowFrequencyScanScheduler).isNotNull();
    }
}
