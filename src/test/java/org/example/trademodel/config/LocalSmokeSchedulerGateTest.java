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
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.reset;
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
    void pushRecheckSchedulerDisabledPropertyKeepsBeanAndNoopsExecution() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks)
                .withPropertyValues("trade-model.schedulers.push-recheck.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(PushRecheckScheduler.class);
                    assertThat(context).hasSingleBean(PositionSyncScheduler.class);
                    assertThat(context).hasSingleBean(MarketDataScheduler.class);
                    reset(mocks.pushSnapshotMapper, mocks.pushRecheckService);

                    context.getBean(PushRecheckScheduler.class).recheckPendingPushesScheduled();

                    verify(mocks.pushSnapshotMapper, never()).listPendingRecheckNext(
                            any(), any(), any(), anyInt(), any(), any(), anyInt());
                    verify(mocks.pushRecheckService, never()).recheck(
                            anyLong(), any(BigDecimal.class), any(RecheckExecutionCommand.class));
                    verify(mocks.dispatchConfigService, never()).loadOrInit(anyInt(), anyInt(), anyInt());
                });
    }

    @Test
    void positionSyncSchedulerDisabledPropertyKeepsBeanAndNoopsExecution() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks)
                .withPropertyValues("trade-model.schedulers.position-sync.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(PushRecheckScheduler.class);
                    assertThat(context).hasSingleBean(PositionSyncScheduler.class);
                    assertThat(context).hasSingleBean(MarketDataScheduler.class);
                    context.getBean(PositionSyncScheduler.class).syncPositionsScheduled();

                    verify(mocks.positionSyncService, never()).syncPositions();
                });
    }

    @Test
    void localSmokePropertiesKeepBeansAndDisableAllNonAnalysisSchedulerExecution() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks)
                .withPropertyValues(
                        "trade-model.schedulers.push-recheck.enabled=false",
                        "trade-model.schedulers.position-sync.enabled=false",
                        "trade-model.schedulers.market-data.enabled=false",
                        "trade-model.schedulers.watchlist.enabled=false",
                        "trade-model.analysis.scheduler.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(PushRecheckScheduler.class);
                    assertThat(context).hasSingleBean(PositionSyncScheduler.class);
                    assertThat(context).hasSingleBean(MarketDataScheduler.class);
                    assertThat(context).hasSingleBean(WatchlistLowFrequencyScanScheduler.class);
                    assertThat(context.getBean(AnalysisRunProperties.class).getScheduler().isEnabled()).isFalse();
                    reset(mocks.pushSnapshotMapper, mocks.pushRecheckService, mocks.positionSyncService,
                            mocks.analysisSchedulerService);

                    context.getBean(PushRecheckScheduler.class).recheckPendingPushesScheduled();
                    context.getBean(PositionSyncScheduler.class).syncPositionsScheduled();
                    context.getBean(MarketDataScheduler.class).fetchRealMarketDataScheduled();
                    WatchlistLowFrequencyScanScheduler.ScanRunResult result = context
                            .getBean(WatchlistLowFrequencyScanScheduler.class)
                            .runScheduledScan();

                    assertThat(result.getStatus())
                            .isEqualTo(WatchlistLowFrequencyScanScheduler.ScanStatus.DISABLED);
                    assertThat(result.getReason()).isEqualTo("SCHEDULER_DISABLED_BY_CONFIG");
                    verify(mocks.pushSnapshotMapper, never()).listPendingRecheckNext(
                            any(), any(), any(), anyInt(), any(), any(), anyInt());
                    verify(mocks.pushRecheckService, never()).recheck(
                            anyLong(), any(BigDecimal.class), any(RecheckExecutionCommand.class));
                    verify(mocks.positionSyncService, never()).syncPositions();
                    verify(mocks.analysisSchedulerService, never()).runScheduledCycle();
                });
    }

    @Test
    void globalSchedulerDisabledPropertyKeepsBeansAndNoopsExecution() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks)
                .withPropertyValues("trade-model.schedulers.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(PushRecheckScheduler.class);
                    assertThat(context).hasSingleBean(PositionSyncScheduler.class);
                    assertThat(context).hasSingleBean(MarketDataScheduler.class);
                    assertThat(context).hasSingleBean(WatchlistLowFrequencyScanScheduler.class);
                    verify(mocks.dispatchConfigService, never()).loadOrInit(anyInt(), anyInt(), anyInt());
                });
    }

    @Test
    void defaultEnabledSchedulerMethodsStillDelegateToExistingDependencies() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks, () -> {
                    AnalysisRunProperties properties = new AnalysisRunProperties();
                    properties.getScheduler().setEnabled(true);
                    return properties;
                })
                .withPropertyValues("trade-model.schedulers.position-sync.enabled=true")
                .run(context -> {
                    reset(mocks.pushSnapshotMapper, mocks.positionSyncService, mocks.analysisSchedulerService);

                    context.getBean(PushRecheckScheduler.class).recheckPendingPushesScheduled();
                    context.getBean(PositionSyncScheduler.class).syncPositionsScheduled();
                    context.getBean(MarketDataScheduler.class).fetchRealMarketDataScheduled();

                    verify(mocks.pushSnapshotMapper).listPendingRecheckNext(
                            any(), any(), any(), anyInt(), any(), any(), anyInt());
                    verify(mocks.positionSyncService).syncPositions();
                    verify(mocks.analysisSchedulerService).runScheduledCycle();
                });
    }

    @Test
    void contextStartupDoesNotInvokeSchedulerExecutionMethods() {
        SchedulerMocks mocks = new SchedulerMocks();

        runner(mocks).run(context -> {
            assertThat(context).hasSingleBean(PushRecheckScheduler.class);
            verify(mocks.dispatchConfigService).loadOrInit(50, 3, 5);
            verify(mocks.positionSyncService, never()).syncPositions();
            verify(mocks.analysisSchedulerService, never()).runScheduledCycle();
            verify(mocks.pushRecheckService, never()).recheck(
                    anyLong(), any(BigDecimal.class), any(RecheckExecutionCommand.class));
            verify(mocks.pushSnapshotMapper, never()).listPendingRecheckNext(
                    any(), any(), any(), anyInt(), any(), any(), anyInt());
        });
    }

    private static ApplicationContextRunner runner(SchedulerMocks mocks) {
        return runner(mocks, AnalysisRunProperties::new);
    }

    private static ApplicationContextRunner runner(
            SchedulerMocks mocks,
            Supplier<AnalysisRunProperties> analysisRunPropertiesSupplier) {
        return new ApplicationContextRunner()
                .withUserConfiguration(SchedulerGateTestConfig.class)
                .withBean(PositionSyncService.class, () -> mocks.positionSyncService)
                .withBean(PushSnapshotMapper.class, () -> mocks.pushSnapshotMapper)
                .withBean(PushRecheckLogMapper.class, () -> mocks.pushRecheckLogMapper)
                .withBean(MarketQuoteClient.class, () -> mocks.marketQuoteClient)
                .withBean(PushRecheckService.class, () -> mocks.pushRecheckService)
                .withBean(PushRecheckDispatchConfigService.class, () -> mocks.dispatchConfigService)
                .withBean(AnalysisSchedulerService.class, () -> mocks.analysisSchedulerService)
                .withBean(AnalysisRunProperties.class, analysisRunPropertiesSupplier);
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
