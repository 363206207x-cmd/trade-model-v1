package org.example.trademodel.analysisrun;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.common.ApiResponse;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.providercall.coinglass.CoinGlassProperties;
import org.example.trademodel.service.AnalysisSchedulerService;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.OpportunityStateIdentity;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.service.watchlistsource.AssetPoolScanTarget;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AnalysisSchedulerServiceTest {
    @Test
    void scheduledCycleIsDisabledByDefault() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisRunProperties properties = new AnalysisRunProperties();
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, properties);

        assertThat(service.runScheduledCycle()).isEmpty();
        assertThat(orchestrator.commands).isEmpty();
        assertThat(properties.getScheduler().getTimeframes())
                .containsExactly("5m", "15m", "1h", "4h");
        assertThat(service.status()).containsEntry("enabled", false)
                .containsEntry("assetPoolOnly", true)
                .containsEntry("persistentScanClaim", true)
                .containsEntry("notAutoTrading", true);
    }

    @Test
    void enabledSchedulerRunsLightweightScanBeforePromotionAnalysis() {
        SchedulerFixture fixture = schedulerFixture(AssetStateEnum.OBSERVING, null, true);
        fixture.stubFreshTrends("UP", "UP", "UP", "UP", 1_000L);

        List<AnalysisRunResult> results = fixture.service.runScheduledCycle();

        assertThat(results).hasSize(1);
        assertThat(fixture.orchestrator.commands).singleElement().satisfies(command -> {
            assertThat(command.getTriggerType()).isEqualTo(AnalysisRunTriggerType.ASSET_POOL_SCAN);
            assertThat(command.getOwnerType()).isEqualTo("USER");
            assertThat(command.getOwnerId()).isEqualTo(42L);
            assertThat(command.getAssetId()).isEqualTo(9001L);
            assertThat(command.getTimeframe()).isEqualTo("5m");
        });
        verify(fixture.assetStateService).completeScheduledScan(
                any(), any(), eq("PROMOTION_SIGNAL:EXECUTED"), eq(null),
                eq("FRESH:BINANCE_PUBLIC:SPOT"), anyString(), eq(1_000L), anyString(),
                eq(true), eq(true));
    }

    @Test
    void invalidSchedulerConfigFailsClosedWithoutCallingOrchestrator() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisRunProperties properties = new AnalysisRunProperties();
        properties.getScheduler().setEnabled(true);
        properties.getScheduler().setSymbols(List.of("BTCUSDT"));
        properties.getScheduler().setTimeframes(List.of("7m"));
        AnalysisSchedulerService service = scheduler(orchestrator, properties, List.of("BTCUSDT"));

        assertThat(service.runScheduledCycle()).isEmpty();
        assertThat(orchestrator.commands).isEmpty();
        assertThat(service.status()).containsEntry("configValid", false);
    }

    @Test
    void schedulerPreservesPoolOwnerAndUsesConfiguredStateCadence() {
        SchedulerFixture fixture = schedulerFixture(
                AssetStateEnum.WAITING_TRIGGER,
                scanAuditWithCoreCloses(
                        "5m=FLAT;15m=FLAT;1h=FLAT;4h=FLAT", 2_000L, 2_000L, 2_000L), true);
        fixture.stubFreshTrends("FLAT", "FLAT", "FLAT", "FLAT", 2_000L);

        List<AnalysisRunResult> results = fixture.service.runScheduledCycle();

        assertThat(results).isEmpty();
        verify(fixture.assetStateService).claimScheduledScan(
                any(), eq(77L), any(), eq(120L), anyString(), eq("rules-v1"));
        assertThat(fixture.orchestrator.commands).isEmpty();
    }

    @Test
    void ordinaryObservingScanDoesNotCallFullAnalysis() {
        SchedulerFixture fixture = schedulerFixture(AssetStateEnum.OBSERVING, null, true);
        fixture.stubFreshTrends("UP", "DOWN", "FLAT", "UP", 3_000L);

        assertThat(fixture.service.runScheduledCycle()).isEmpty();

        assertThat(fixture.orchestrator.commands).isEmpty();
        assertThat(fixture.service.status()).containsEntry("lightweightScanCount", 1L)
                .containsEntry("fullAnalysisRequestCount", 0L);
        verify(fixture.assetStateService).completeScheduledScan(
                any(), any(), eq("NO_MATERIAL_CHANGE"), eq(null),
                eq("FRESH:BINANCE_PUBLIC:SPOT"), anyString(), eq(3_000L), eq(null),
                eq(false), eq(false));
    }

    @Test
    void allPoolTargetsIncludingNonTopSixAreVisitedAndOneFailureIsIsolated() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisRunProperties properties = new AnalysisRunProperties();
        properties.getScheduler().setEnabled(true);
        AssetPoolService assetPoolService = mock(AssetPoolService.class);
        AssetStateMapper assetStateMapper = mock(AssetStateMapper.class);
        PersistedOhlcvQueryService query = mock(PersistedOhlcvQueryService.class);
        AssetStateService assetStateService = mock(AssetStateService.class);
        RuleConfigService rules = mock(RuleConfigService.class);
        List<AssetPoolScanTarget> targets = java.util.stream.IntStream.rangeClosed(1, 8)
                .mapToObj(index -> new AssetPoolScanTarget(
                        "USER", 42L, (long) index, index == 1 ? "BADUSDT" : "ASSET" + index + "USDT"))
                .toList();
        when(assetPoolService.listScanTargets()).thenReturn(targets);
        when(assetPoolService.listScanSymbols()).thenReturn(
                targets.stream().map(AssetPoolScanTarget::symbol).toList());
        when(assetPoolService.resolvePoolItemId(anyString(), anyLong(), anyLong(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        when(assetStateMapper.selectByIdentity(anyString(), anyLong(), anyString(), eq("5m")))
                .thenAnswer(invocation -> {
                    if ("BADUSDT".equals(invocation.getArgument(2))) {
                        throw new IllegalStateException("isolated failure");
                    }
                    return state(AssetStateEnum.OBSERVING, LocalDateTime.ofInstant(
                            Instant.parse("2026-08-11T23:00:00Z"), ZoneOffset.UTC));
                });
        when(rules.resolveActiveRuleVersion()).thenReturn("rules-v1");
        when(assetStateService.claimScheduledScan(
                any(), anyLong(), any(), eq(900L), anyString(), eq("rules-v1")))
                .thenReturn(null);
        AnalysisSchedulerService service = new AnalysisSchedulerService(
                orchestrator, properties,
                Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC),
                assetPoolService, assetStateMapper);
        service.setPersistedOhlcvQueryService(query);
        service.setScheduledScanDependencies(
                assetStateService, rules, new CoinGlassProperties(),
                FundamentalAiV41Properties.contractFixture());

        assertThat(service.runScheduledCycle()).isEmpty();

        verify(assetStateService, org.mockito.Mockito.times(7)).claimScheduledScan(
                any(), anyLong(), any(), eq(900L), anyString(), eq("rules-v1"));
        assertThat(orchestrator.commands).isEmpty();
    }

    @Test
    void triggeredMinutePollWithUnchangedEvidenceCallsNoFullAnalysis() {
        SchedulerFixture fixture = schedulerFixture(
                AssetStateEnum.TRIGGERED,
                scanAuditWithCoreCloses("5m=UP;15m=UP;1h=UP;4h=UP", 3_500L,
                        4_000L, 4_000L, "FINAL_MISSING"), true);
        fixture.stubFreshTrends("UP", "UP", "UP", "UP", 4_000L);

        assertThat(fixture.service.runScheduledCycle()).isEmpty();

        assertThat(fixture.orchestrator.commands).isEmpty();
        assertThat(fixture.service.status()).containsEntry("triggeredLightweightCount", 1L)
                .containsEntry("triggeredFullAnalysisRequestCount", 0L);
    }

    @Test
    void triggeredMaterialEvidenceChangeMayRequestOneFullAnalysis() {
        SchedulerFixture fixture = schedulerFixture(
                AssetStateEnum.TRIGGERED,
                scanAudit("5m=DOWN;15m=DOWN;1h=DOWN;4h=DOWN", 4_500L, "FINAL_MISSING"), true);
        fixture.stubFreshTrends("UP", "UP", "UP", "UP", 5_000L);

        assertThat(fixture.service.runScheduledCycle()).hasSize(1);

        assertThat(fixture.orchestrator.commands).hasSize(1);
        assertThat(fixture.service.status()).containsEntry("triggeredFullAnalysisRequestCount", 1L);
    }

    @Test
    void triggeredPlanLifecycleChangeRequestsRuleOwnedReanalysis() {
        SchedulerFixture fixture = schedulerFixture(
                AssetStateEnum.TRIGGERED,
                scanAuditWithCoreCloses(
                        "5m=UP;15m=UP;1h=UP;4h=UP", 5_000L, 5_000L, 5_000L, "READY"), true);
        fixture.stubFreshTrends("UP", "UP", "UP", "UP", 5_000L);
        ExecutionPlanDO needsRevalidation = new ExecutionPlanDO();
        needsRevalidation.setPlanLifecycleState("NEEDS_REVALIDATION");
        when(fixture.executionPlanMapper.selectLatestFinalByOpportunityId("opp-test"))
                .thenReturn(needsRevalidation);

        assertThat(fixture.service.runScheduledCycle()).hasSize(1);

        assertThat(fixture.orchestrator.commands).hasSize(1);
        verify(fixture.assetStateService).completeScheduledScan(
                any(), any(), eq("TRIGGERED_MATERIAL_EVIDENCE_CHANGE:EXECUTED"), eq(null),
                eq("FRESH:BINANCE_PUBLIC:SPOT"),
                org.mockito.ArgumentMatchers.contains("PLAN=LIFECYCLE_NEEDS_REVALIDATION"),
                eq(5_000L), anyString(), eq(true), eq(true));
    }

    @Test
    void failedFullAnalysisDoesNotAdvanceSuccessAndRemainsRetryable() {
        SchedulerFixture fixture = schedulerFixture(AssetStateEnum.OBSERVING, null, true);
        fixture.stubFreshTrends("UP", "UP", "UP", "UP", 5_250L);
        fixture.orchestrator.results.add(AnalysisRunResult.failed(run("ana-failed", "FAILED"), "provider failed"));

        assertThat(fixture.service.runScheduledCycle()).hasSize(1);
        assertThat(fixture.service.runScheduledCycle()).hasSize(1);

        assertThat(fixture.orchestrator.commands).hasSize(2);
        verify(fixture.assetStateService).completeScheduledScan(
                any(), any(), eq("PROMOTION_SIGNAL:FAILED"), anyString(),
                eq("FRESH:BINANCE_PUBLIC:SPOT"), anyString(), eq(5_250L),
                anyString(), eq(true), eq(false));
    }

    @Test
    void newHotResetHasPriorityOverOrdinaryLightweightOutcome() {
        LocalDateTime started = LocalDateTime.ofInstant(
                Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC);
        String previous = scanAudit(
                "5m=UP;15m=DOWN;1h=FLAT;4h=UP", 5_400L, "NOT_REQUIRED",
                "2026-08-11T23:00");
        SchedulerFixture fixture = schedulerFixture(AssetStateEnum.OBSERVING, previous, true);
        fixture.stubFreshTrends("UP", "DOWN", "FLAT", "UP", 5_400L);
        when(fixture.assetStateService.claimScheduledScan(any(), eq(77L), any(), eq(900L),
                anyString(), eq("rules-v1"))).thenReturn(new AssetStateService.ScheduledScanClaim(
                new OpportunityStateIdentity("USER", 42L, 9001L, "BTCUSDT", "5m"),
                "opp-test", AssetStateEnum.OBSERVING, "analysis-before", "HIGH",
                LocalDateTime.of(2026, 8, 11, 23, 30), "trace-hot-reset", "rules-v1",
                started.minusMinutes(15), started, started.plusMinutes(15), previous));

        assertThat(fixture.service.runScheduledCycle()).hasSize(1);

        verify(fixture.assetStateService).completeScheduledScan(
                any(), any(), eq("HOT_RESET_RECALCULATION:EXECUTED"), eq(null),
                eq("FRESH:BINANCE_PUBLIC:SPOT"), anyString(), eq(5_400L),
                anyString(), eq(true), eq(true));
    }

    @Test
    void candidateNewClosedCandleAllowsLegalRecalculation() {
        SchedulerFixture fixture = schedulerFixture(
                AssetStateEnum.CANDIDATE,
                scanAudit("5m=UP;15m=UP;1h=UP;4h=UP", 5_500L), true);
        fixture.stubFreshTrends("UP", "UP", "UP", "UP", 6_000L);

        assertThat(fixture.service.runScheduledCycle()).hasSize(1);
        assertThat(fixture.orchestrator.commands).hasSize(1);
    }

    @Test
    void newOneHourCloseTriggersRecalculationWhenFiveMinuteCloseIsUnchanged() {
        SchedulerFixture fixture = schedulerFixture(
                AssetStateEnum.CANDIDATE,
                scanAuditWithCoreCloses("5m=UP;15m=UP;1h=UP;4h=UP", 6_000L, 5_000L, 4_000L), true);
        fixture.stubFreshTrendCloses("UP", "UP", "UP", "UP",
                6_000L, 6_000L, 6_000L, 4_000L);

        assertThat(fixture.service.runScheduledCycle()).hasSize(1);
        assertThat(fixture.orchestrator.commands).singleElement().satisfies(command ->
                assertThat(command.getTriggerReference())
                        .contains("NEW_CORE_CLOSED_CANDLE_RECALCULATION"));
    }

    @Test
    void legacySuccessfulScanWithoutCoreCloseIdentityTriggersOneCatchUpRecalculation() {
        SchedulerFixture fixture = schedulerFixture(
                AssetStateEnum.CANDIDATE,
                scanAudit("5m=UP;15m=UP;1h=UP;4h=UP", 6_000L), true);
        fixture.stubFreshTrendCloses("UP", "UP", "UP", "UP",
                6_000L, 6_000L, 5_000L, 4_000L);

        assertThat(fixture.service.runScheduledCycle()).hasSize(1);
        assertThat(fixture.orchestrator.commands).singleElement().satisfies(command ->
                assertThat(command.getTriggerReference())
                        .contains("NEW_CORE_CLOSED_CANDLE_RECALCULATION"));
    }

    @Test
    void staleSourceFailsClosedBeforeFullAnalysis() {
        SchedulerFixture fixture = schedulerFixture(AssetStateEnum.CANDIDATE, null, true);
        PersistedOhlcvReadinessResult stale = new PersistedOhlcvReadinessResult();
        stale.setStatus(PersistedOhlcvReadinessStatus.STALE);
        stale.setStaleReasonCode(PersistedOhlcvStaleReasonCode.LATEST_BAR_TOO_OLD);
        when(fixture.query.evaluateReadinessForSource(
                anyString(), anyString(), eq(100), anyLong(),
                eq("BINANCE_PUBLIC"), eq("SPOT"))).thenReturn(stale);

        assertThat(fixture.service.runScheduledCycle()).isEmpty();

        assertThat(fixture.orchestrator.commands).isEmpty();
        verify(fixture.assetStateService).completeScheduledScan(
                any(), any(), eq("DATA_NOT_READY"), eq("LATEST_BAR_TOO_OLD"),
                eq("NOT_FRESH"), eq(null), eq(null), eq(null), eq(false), eq(false));
    }

    @Test
    void missingCoinGlassDoesNotBlockCoreStructuralRecalculation() {
        SchedulerFixture fixture = schedulerFixture(AssetStateEnum.OBSERVING, null, false);
        fixture.stubFreshTrends("UP", "UP", "UP", "UP", 7_000L);

        assertThat(fixture.service.runScheduledCycle()).hasSize(1);

        assertThat(fixture.orchestrator.commands).hasSize(1);
        verify(fixture.assetStateService).completeScheduledScan(
                any(), any(), eq("PROMOTION_SIGNAL:EXECUTED"), eq(null),
                eq("FRESH:BINANCE_PUBLIC:SPOT"), anyString(), eq(7_000L), anyString(),
                eq(true), eq(true));
    }

    @Test
    void hotResetCompatibilityMethodUsesHotResetTriggerType() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, new AnalysisRunProperties());

        service.executeAnalysis("BTCUSDT", "1m", "HOT_RESET:hre-1");

        assertThat(orchestrator.commands).hasSize(1);
        assertThat(orchestrator.commands.get(0).getTriggerType()).isEqualTo(AnalysisRunTriggerType.HOT_RESET_REBUILD);
        assertThat(orchestrator.commands.get(0).getTriggerReference()).isEqualTo("hre-1");
    }

    @Test
    void executeAnalysisReturnsFailureWithoutMinimalAnalysisForNonExecutedResult() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator(
                AnalysisRunResult.inProgress(run("ana-in-progress", "STARTED")));
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, new AnalysisRunProperties());

        ApiResponse<AssetAnalysisVO> response = service.executeAnalysis("BTCUSDT", "1m", "HOT_RESET:hre-blocked");

        assertThat(response.getCode()).isEqualTo(500);
        assertThat(response.getData()).isNull();
        assertThat(response.getMsg()).contains("IDEMPOTENCY_IN_PROGRESS");
    }

    @Test
    void executeAnalysisAllowsReusableDuplicateSuccessWithoutExecutingAnalysisAgain() {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator(
                AnalysisRunResult.duplicateSuccess(run("ana-existing-success", "SUCCESS")));
        AnalysisSchedulerService service = new AnalysisSchedulerService(orchestrator, new AnalysisRunProperties());

        ApiResponse<AssetAnalysisVO> response = service.executeAnalysis("BTCUSDT", "1m", "HOT_RESET:hre-duplicate");

        assertThat(response.getCode()).isEqualTo(200);
        assertThat(response.getMsg()).isEqualTo("EXISTING_SUCCESS");
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getAnalysisId()).isEqualTo("ana-existing-success");
    }

    private static SchedulerFixture schedulerFixture(AssetStateEnum state,
                                                     String previousExtJson,
                                                     boolean coinGlassConfigured) {
        CapturingOrchestrator orchestrator = new CapturingOrchestrator();
        AnalysisRunProperties properties = new AnalysisRunProperties();
        properties.getScheduler().setEnabled(true);
        AssetPoolService assetPoolService = mock(AssetPoolService.class);
        AssetStateMapper assetStateMapper = mock(AssetStateMapper.class);
        PersistedOhlcvQueryService query = mock(PersistedOhlcvQueryService.class);
        ExecutionPlanMapper executionPlanMapper = mock(ExecutionPlanMapper.class);
        AssetStateService assetStateService = mock(AssetStateService.class);
        RuleConfigService rules = mock(RuleConfigService.class);
        AssetPoolScanTarget target = new AssetPoolScanTarget("USER", 42L, 9001L, "BTCUSDT");
        when(assetPoolService.listScanTargets()).thenReturn(List.of(target));
        when(assetPoolService.listScanSymbols()).thenReturn(List.of(target.symbol()));
        when(assetPoolService.resolvePoolItemId("USER", 42L, 9001L, "BTCUSDT")).thenReturn(77L);
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "5m"))
                .thenReturn(state(state, LocalDateTime.ofInstant(
                        Instant.parse("2026-08-11T23:00:00Z"), ZoneOffset.UTC)));
        when(rules.resolveActiveRuleVersion()).thenReturn("rules-v1");
        when(assetStateService.claimScheduledScan(
                any(), eq(77L), any(), eq(properties.getScheduler().intervalSeconds(state.name())),
                anyString(), eq("rules-v1")))
                .thenAnswer(invocation -> scheduledClaim(
                        target, state, previousExtJson,
                        invocation.getArgument(2), invocation.getArgument(4)));
        when(assetStateService.completeScheduledScan(
                any(), any(), anyString(), any(), any(), any(), any(), any(), anyBoolean(), anyBoolean()))
                .thenReturn(true);

        CoinGlassProperties coinGlass = new CoinGlassProperties();
        if (coinGlassConfigured) {
            coinGlass.setEnabled(true);
            coinGlass.setExternalCallsEnabled(true);
            coinGlass.setApiKey("configured-test-key");
            coinGlass.setAdvertisedRpm(300);
        }
        AnalysisSchedulerService service = new AnalysisSchedulerService(
                orchestrator, properties,
                Clock.fixed(Instant.parse("2026-08-12T00:00:00Z"), ZoneOffset.UTC),
                assetPoolService, assetStateMapper);
        service.setPersistedOhlcvQueryService(query);
        service.setScheduledScanDependencies(
                assetStateService, rules, coinGlass, FundamentalAiV41Properties.contractFixture());
        service.setExecutionPlanMapper(executionPlanMapper);
        return new SchedulerFixture(
                service, orchestrator, assetStateService, query, executionPlanMapper);
    }

    private static AssetStateService.ScheduledScanClaim scheduledClaim(
            AssetPoolScanTarget target,
            AssetStateEnum state,
            String previousExtJson,
            LocalDateTime startedAt,
            String traceId) {
        OpportunityStateIdentity identity = new OpportunityStateIdentity(
                target.ownerType(), target.ownerId(), target.assetId(), target.symbol(), "5m");
        return new AssetStateService.ScheduledScanClaim(
                identity, "opp-test", state, "analysis-before", "HIGH",
                null, traceId, "rules-v1", startedAt, startedAt,
                startedAt.plusMinutes(1), previousExtJson);
    }

    private static PersistedOhlcvReadinessResult freshReadiness(
            String timeframe, String trend, long latestClose) {
        PersistedOhlcvBarDO newest = bar(
                timeframe, latestClose, "DOWN".equals(trend) ? "90" : "110");
        PersistedOhlcvBarDO oldest = bar(
                timeframe, latestClose - 1L, "UP".equals(trend) ? "100" : "110");
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setSymbol("BTCUSDT");
        result.setTimeframe(timeframe);
        result.setRequiredWindowSize(100);
        result.setStatus(PersistedOhlcvReadinessStatus.FRESH);
        result.setStaleReasonCode(PersistedOhlcvStaleReasonCode.NONE);
        result.setBars(List.of(newest, oldest));
        result.setLatestCloseTimeMs(latestClose);
        return result;
    }

    private static PersistedOhlcvBarDO bar(String timeframe, long closeTime, String close) {
        PersistedOhlcvBarDO row = new PersistedOhlcvBarDO();
        row.setSymbol("BTCUSDT");
        row.setTimeframe(timeframe);
        row.setCloseTimeMs(closeTime);
        row.setClosePrice(new BigDecimal(close));
        return row;
    }

    private static String scanAudit(String signature, long latestFullClose) {
        return scanAudit(signature, latestFullClose, "NOT_REQUIRED");
    }

    private static String scanAudit(String signature, long latestFullClose, String planStatus) {
        return scanAudit(signature, latestFullClose, planStatus, null);
    }

    private static String scanAudit(String signature, long latestFullClose,
                                    String planStatus, String latestFullHotResetAt) {
        String fullSignature = signature + ";RISK=HIGH;PLAN=" + planStatus;
        String hotReset = latestFullHotResetAt == null ? ""
                : ",\"latestFullHotResetAt\":\"" + latestFullHotResetAt + "\"";
        return "{\"schedulerScan\":{\"structureSignature\":\"" + fullSignature
                + "\",\"latestFullStructureSignature\":\"" + fullSignature
                + "\",\"latestFullAnalysisCloseTimeMs\":" + latestFullClose + hotReset + "}}";
    }

    private static String scanAuditWithCoreCloses(String signature, long latestFiveMinuteClose,
                                                   long latestOneHourClose, long latestFourHourClose) {
        return scanAuditWithCoreCloses(signature, latestFiveMinuteClose,
                latestOneHourClose, latestFourHourClose, "NOT_REQUIRED");
    }

    private static String scanAuditWithCoreCloses(String signature, long latestFiveMinuteClose,
                                                   long latestOneHourClose, long latestFourHourClose,
                                                   String planStatus) {
        String fullSignature = signature + ";RISK=HIGH;PLAN=" + planStatus;
        return "{\"schedulerScan\":{\"structureSignature\":\"" + fullSignature
                + "\",\"latestFullStructureSignature\":\"" + fullSignature
                + "\",\"latestFullAnalysisCloseTimeMs\":" + latestFiveMinuteClose
                + ",\"latestFull1hCloseTimeMs\":" + latestOneHourClose
                + ",\"latestFull4hCloseTimeMs\":" + latestFourHourClose + "}}";
    }

    private record SchedulerFixture(AnalysisSchedulerService service,
                                    CapturingOrchestrator orchestrator,
                                    AssetStateService assetStateService,
                                    PersistedOhlcvQueryService query,
                                    ExecutionPlanMapper executionPlanMapper) {
        void stubFreshTrends(String fiveMinute, String fifteenMinute,
                             String oneHour, String fourHour, long latestClose) {
            stubFreshTrendCloses(fiveMinute, fifteenMinute, oneHour, fourHour,
                    latestClose, latestClose, latestClose, latestClose);
        }

        void stubFreshTrendCloses(String fiveMinute, String fifteenMinute,
                                  String oneHour, String fourHour,
                                  long fiveMinuteClose, long fifteenMinuteClose,
                                  long oneHourClose, long fourHourClose) {
            when(query.evaluateReadinessForSource(
                    anyString(), anyString(), eq(100), anyLong(),
                    eq("BINANCE_PUBLIC"), eq("SPOT")))
                    .thenAnswer(invocation -> {
                        String timeframe = invocation.getArgument(1);
                        String trend = switch (timeframe) {
                            case "5m" -> fiveMinute;
                            case "15m" -> fifteenMinute;
                            case "1h" -> oneHour;
                            case "4h" -> fourHour;
                            default -> "FLAT";
                        };
                        long latestClose = switch (timeframe) {
                            case "5m" -> fiveMinuteClose;
                            case "15m" -> fifteenMinuteClose;
                            case "1h" -> oneHourClose;
                            case "4h" -> fourHourClose;
                            default -> fiveMinuteClose;
                        };
                        return freshReadiness(timeframe, trend, latestClose);
                    });
        }
    }

    private static class CapturingOrchestrator implements AnalysisRunOrchestrator {
        private final List<AnalysisRunCommand> commands = new ArrayList<>();
        private final List<AnalysisRunResult> results = new ArrayList<>();

        private CapturingOrchestrator(AnalysisRunResult... results) {
            this.results.addAll(List.of(results));
        }

        @Override
        public AnalysisRunResult run(AnalysisRunCommand command) {
            commands.add(command);
            if (!results.isEmpty()) {
                return results.remove(0);
            }
            AnalysisRunDO run = new AnalysisRunDO();
            run.setAnalysisId("ana-" + commands.size());
            run.setTraceId("trace-" + commands.size());
            run.setRequestId(command.getRequestId());
            run.setSymbol(command.getSymbol());
            run.setTimeframe(command.getTimeframe());
            run.setTriggerType(command.getTriggerType().name());
            run.setTriggerReference(command.getTriggerReference());
            AssetAnalysisVO analysis = new AssetAnalysisVO();
            analysis.setAnalysisId(run.getAnalysisId());
            analysis.setSymbol(run.getSymbol());
            analysis.setTimeframe(run.getTimeframe());
            return AnalysisRunResult.executed(run, analysis, false, false);
        }
    }

    private static AnalysisSchedulerService scheduler(CapturingOrchestrator orchestrator,
                                                      AnalysisRunProperties properties,
                                                      List<String> poolSymbols) {
        AssetPoolService assetPoolService = mock(AssetPoolService.class);
        when(assetPoolService.listScanSymbols()).thenReturn(poolSymbols);
        return new AnalysisSchedulerService(orchestrator, properties, Clock.systemUTC(), assetPoolService);
    }

    private static AnalysisRunDO run(String analysisId, String status) {
        AnalysisRunDO run = new AnalysisRunDO();
        run.setAnalysisId(analysisId);
        run.setTraceId("trace-" + analysisId);
        run.setRequestId("req-" + analysisId);
        run.setSymbol("BTCUSDT");
        run.setTimeframe("1m");
        run.setTriggerType(AnalysisRunTriggerType.HOT_RESET_REBUILD.name());
        run.setTriggerReference("hre-test");
        run.setStatus(status);
        return run;
    }

    private static AssetStateDO state(AssetStateEnum state, LocalDateTime lastUpdateTime) {
        AssetStateDO row = new AssetStateDO();
        row.setState(state);
        row.setLastUpdateTime(lastUpdateTime);
        return row;
    }
}
