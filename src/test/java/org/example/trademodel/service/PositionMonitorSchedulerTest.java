package org.example.trademodel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.example.trademodel.positionmonitor.PositionMonitorBatchResultDTO;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class PositionMonitorSchedulerTest {

    @Test
    void periodicMonitorBuildsManualPositionContextWithoutAnyDashboardRequest() {
        // In-memory mocks only: no datasource, external provider or Owner record is used.
        long userId = java.util.concurrent.ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        var positions = mock(org.example.trademodel.mapper.UserPositionMapper.class);
        var decisions = mock(org.example.trademodel.mapper.DecisionResultMapper.class);
        var runs = mock(org.example.trademodel.mapper.AnalysisRunMapper.class);
        var live = new org.example.trademodel.v41.DashboardLiveEventService();
        var monitor = mock(PositionMonitorService.class);
        var scheduler = new PositionMonitorScheduler(monitor, true, true);
        scheduler.setDashboardLiveEventService(live);
        scheduler.setStructuralContextSources(positions, decisions, runs, new com.fasterxml.jackson.databind.ObjectMapper());
        var position = new org.example.trademodel.entity.UserPositionDO();
        position.setUserId(userId);
        position.setAssetSymbol("ETHUSDT");
        position.setStatus("OPEN");
        position.setSourceType("MANUAL_INDEPENDENT");
        var decision = new org.example.trademodel.vo.DecisionResultVO();
        decision.setAnalysisId(java.util.UUID.randomUUID().toString());
        decision.setDecisionId(java.util.UUID.randomUUID().toString());
        decision.setSymbol("ETHUSDT");
        decision.setTimeframe("1h");
        decision.setDirectionDataState("READY");
        decision.setValidatedMarketBias("WEAK_BULLISH");
        decision.setFinalConfidence(72);
        decision.setCreateTime(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        decision.setExplanationJson("{\"atr1h\":12.5}");
        var run = new org.example.trademodel.entity.AnalysisRunDO();
        run.setAnalysisId(decision.getAnalysisId());
        run.setSymbol(decision.getSymbol());
        run.setTimeframe("1h");
        run.setStatus("SUCCESS");
        run.setOwnerType("USER");
        run.setOwnerId(userId);
        run.setTraceId(java.util.UUID.randomUUID().toString());
        when(positions.listClaimedOpenForSystemMonitoring()).thenReturn(java.util.List.of(position));
        when(decisions.findLatestDecisionResultsForSymbolsJoined(java.util.List.of("ETHUSDT"), "USER", userId))
                .thenReturn(java.util.List.of(decision));
        when(runs.selectReadableByUser(decision.getAnalysisId(), userId)).thenReturn(run);

        scheduler.monitorOpenUserPositionsScheduled();

        assertThat(live.structuralContexts("ETHUSDT")).singleElement().satisfies(scoped -> {
            assertThat(scoped.userId()).isEqualTo(userId);
            assertThat(scoped.context().analysisId()).isEqualTo(decision.getAnalysisId());
            assertThat(scoped.context().decisionId()).isEqualTo(decision.getDecisionId());
            assertThat(scoped.context().traceId()).isEqualTo(run.getTraceId());
            assertThat(scoped.context().atr1h()).isEqualByComparingTo("12.5");
            assertThat(scoped.context().baseConfidence()).isEqualTo(72);
        });
        verify(monitor).monitorClaimedOpenPositionsForSystem();
        verify(positions, never()).insert(org.mockito.ArgumentMatchers.any());

        when(positions.listOpenByUserId(userId)).thenReturn(java.util.List.of());
        scheduler.refreshStructuralContextsForUser(userId);
        assertThat(live.structuralContexts("ETHUSDT")).isEmpty();
        verify(monitor).monitorClaimedOpenPositionsForSystem(); // lifecycle refresh adds no batch
    }

    @Test
    void defaultDisabledSchedulerDoesNotRunMonitorBatch() {
        PositionMonitorService service = mock(PositionMonitorService.class);
        PositionMonitorScheduler scheduler = new PositionMonitorScheduler(service, true, false);

        scheduler.monitorOpenUserPositionsScheduled();

        verify(service, never()).monitorClaimedOpenPositionsForSystem();
    }

    @Test
    void enabledSchedulerRunsOpenPositionMonitorBatchOnly() {
        PositionMonitorService service = mock(PositionMonitorService.class);
        PositionMonitorScheduler scheduler = new PositionMonitorScheduler(service, true, true);

        scheduler.monitorOpenUserPositionsScheduled();

        verify(service).monitorClaimedOpenPositionsForSystem();
    }

    @Test
    void enabledSchedulerLogsSanitizedBatchCounts(CapturedOutput output) {
        PositionMonitorService service = mock(PositionMonitorService.class);
        PositionMonitorBatchResultDTO batch = new PositionMonitorBatchResultDTO();
        batch.setTotalCount(28);
        batch.setSuccessCount(0);
        batch.setFailureCount(28);
        batch.setBlockedCount(0);
        batch.setFailures(java.util.List.of(
                new PositionMonitorBatchResultDTO.FailureItem(31L, "ETH", "QUOTE_UNAVAILABLE"),
                new PositionMonitorBatchResultDTO.FailureItem(32L, "ETH", "QUOTE_UNAVAILABLE"),
                new PositionMonitorBatchResultDTO.FailureItem(33L, "BTC", "POSITION_MONITOR_FAILED:IllegalStateException")));
        when(service.monitorClaimedOpenPositionsForSystem()).thenReturn(batch);
        PositionMonitorScheduler scheduler = new PositionMonitorScheduler(service, true, true);

        scheduler.monitorOpenUserPositionsScheduled();

        assertThat(output).contains("batch completed trigger=PERIODIC_30S total=28 success=0 failure=28 blocked=0")
                .contains("failure summary=POSITION_MONITOR_FAILED:ILLEGALSTATEEXCEPTION=1,QUOTE_UNAVAILABLE=2")
                .doesNotContain("positionId=")
                .doesNotContain("assetSymbol=")
                .doesNotContain("ETH")
                .doesNotContain("BTC");
    }

    @Test
    void realtimeShockCoalescesRequestsAndRunsOneImmediateReadOnlyBatch() {
        PositionMonitorService service = mock(PositionMonitorService.class);
        PositionMonitorScheduler scheduler = new PositionMonitorScheduler(service, true, true);

        scheduler.requestImmediateRiskMonitor("PRICE_SHOCK_1M");
        scheduler.requestImmediateRiskMonitor("PRICE_SHOCK_1M");
        scheduler.monitorImmediateRiskRequestsScheduled();
        scheduler.monitorImmediateRiskRequestsScheduled();

        verify(service).monitorClaimedOpenPositionsForSystem();
    }

    @Test
    void duplicateInitialMonitorRequestsAreCoalescedBeforeTheFirstRun() {
        PositionMonitorService service = mock(PositionMonitorService.class);
        PositionMonitorScheduler scheduler = new PositionMonitorScheduler(service, true, true);

        scheduler.requestInitialMonitor(31L, 41L);
        scheduler.requestInitialMonitor(31L, 41L);

        assertThat(scheduler.pendingInitialMonitorCount()).isEqualTo(1);
        scheduler.monitorInitialRequestsScheduled();
        assertThat(scheduler.pendingInitialMonitorCount()).isZero();
        verify(service).monitorUserPositionForUser(31L, 41L);
    }
}
