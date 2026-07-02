package org.example.trademodel.config;

import org.example.trademodel.analysisrun.AnalysisRunProperties;
import org.example.trademodel.mapper.PushRecheckLogMapper;
import org.example.trademodel.mapper.PushSnapshotMapper;
import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.service.MarketDataScheduler;
import org.example.trademodel.service.PositionSyncScheduler;
import org.example.trademodel.service.PositionSyncService;
import org.example.trademodel.service.PushRecheckDispatchConfigService;
import org.example.trademodel.service.PushRecheckScheduler;
import org.example.trademodel.service.PushRecheckService;
import org.example.trademodel.service.RecheckExecutionCommand;
import org.example.trademodel.service.watchlist.WatchlistLowFrequencyScanScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalSmokeSchedulerGateTest {

    @Test
    void defaultPropertiesPreserveSchedulerBeanCreation() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks).run(context -> {
            assertThat(context).hasSingleBean(PushRecheckScheduler.class);
            assertThat(context).hasSingleBean(PositionSyncScheduler.class);
            assertThat(context).hasSingleBean(MarketDataScheduler.class);
            assertThat(context).hasSingleBean(WatchlistLowFrequencyScanScheduler.class);
            assertThat(context.getBean(AnalysisRunProperties.class).getScheduler().isEnabled()).isFalse();
        });
    }

    @Test
    void pushRecheckSchedulerDisabledPropertyPreventsBeanCreation() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks)
                .withPropertyValues("trade-model.schedulers.push-recheck.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PushRecheckScheduler.class);
                    assertThat(context).hasSingleBean(PositionSyncScheduler.class);
                    assertThat(context).hasSingleBean(MarketDataScheduler.class);
                });
    }

    @Test
    void positionSyncSchedulerDisabledPropertyPreventsBeanCreation() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks)
                .withPropertyValues("trade-model.schedulers.position-sync.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(PushRecheckScheduler.class);
                    assertThat(context).doesNotHaveBean(PositionSyncScheduler.class);
                    assertThat(context).hasSingleBean(MarketDataScheduler.class);
                });
    }

    @Test
    void localSmokePropertiesDisableAllNonAnalysisSchedulerBeans() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks)
                .withPropertyValues(
                        "trade-model.schedulers.push-recheck.enabled=false",
                        "trade-model.schedulers.position-sync.enabled=false",
                        "trade-model.schedulers.market-data.enabled=false",
                        "trade-model.schedulers.watchlist.enabled=false",
                        "trade-model.analysis.scheduler.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PushRecheckScheduler.class);
                    assertThat(context).doesNotHaveBean(PositionSyncScheduler.class);
                    assertThat(context).doesNotHaveBean(MarketDataScheduler.class);
                    assertThat(context).doesNotHaveBean(WatchlistLowFrequencyScanScheduler.class);
                    assertThat(context.getBean(AnalysisRunProperties.class).getScheduler().isEnabled()).isFalse();
                });
    }

    @Test
    void globalSchedulerDisabledPropertyPreventsSchedulerBeanCreation() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks)
                .withPropertyValues("trade-model.schedulers.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(PushRecheckScheduler.class);
                    assertThat(context).doesNotHaveBean(PositionSyncScheduler.class);
                    assertThat(context).doesNotHaveBean(MarketDataScheduler.class);
                    assertThat(context).doesNotHaveBean(WatchlistLowFrequencyScanScheduler.class);
                });
    }

    @Test
    void contextStartupDoesNotInvokeSchedulerExecutionMethods() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks).run(context -> {
            assertThat(context).hasSingleBean(PushRecheckScheduler.class);
            verify(mocks.positionSyncService, never()).syncPositions();
            verify(mocks.analysisSchedulerService, never()).runScheduledCycle();
            verify(mocks.pushRecheckService, never()).recheck(
                    anyLong(), any(BigDecimal.class), any(RecheckExecutionCommand.class));
            verify(mocks.pushSnapshotMapper, never()).listPendingRecheckNext(
                    any(), any(), any(), anyInt(), anyInt(), anyInt());
        });
    }

    private static ApplicationContextRunner runner(SchedulerMocks mocks) {
        return new ApplicationContextRunner()
                .withUserConfiguration(SchedulerGateTestConfig.class)
                .withBean(PositionSyncService.class, () -> mocks.positionSyncService)
                .withBean(PushSnapshotMapper.class, () -> mocks.pushSnapshotMapper)
                .withBean(PushRecheckLogMapper.class, () -> mocks.pushRecheckLogMapper)
                .withBean(MarketQuoteClient.class, () -> mocks.marketQuoteClient)
                .withBean(PushRecheckService.class, () -> mocks.pushRecheckService)
                .withBean(PushRecheckDispatchConfigService.class, () -> mocks.dispatchConfigService)
                .withBean(AnalysisSchedulerService.class, () -> mocks.analysisSchedulerService)
                .withBean(AnalysisRunProperties.class, AnalysisRunProperties::new);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            PushRecheckScheduler.class,
            PositionSyncScheduler.class,
            MarketDataScheduler.class,
            WatchlistLowFrequencyScanScheduler.class
    })
    static class SchedulerGateTestConfig {
    }

    private static final class SchedulerMocks {
        private final PositionSyncService positionSyncService = mock(PositionSyncService.class);
        private final PushSnapshotMapper pushSnapshotMapper = mock(PushSnapshotMapper.class);
        private final PushRecheckLogMapper pushRecheckLogMapper = mock(PushRecheckLogMapper.class);
        private final MarketQuoteClient marketQuoteClient = mock(MarketQuoteClient.class);
        private final PushRecheckService pushRecheckService = mock(PushRecheckService.class);
        private final PushRecheckDispatchConfigService dispatchConfigService = mock(PushRecheckDispatchConfigService.class);
        private final AnalysisSchedulerService analysisSchedulerService = mock(AnalysisSchedulerService.class);

        private SchedulerMocks() {
            when(dispatchConfigService.loadOrInit(anyInt(), anyInt(), anyInt()))
                    .thenReturn(Map.of("limit", 50, "maxAttempts", 3, "minRetryMinutes", 5));
        }
    }
}
