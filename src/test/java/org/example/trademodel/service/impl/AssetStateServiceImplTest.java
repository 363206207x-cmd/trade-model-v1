package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AssetStateDO;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.entity.OpportunityStateTransitionDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.AssetStateMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.mapper.OpportunityStateTransitionMapper;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.OpportunityTriggerSource;
import org.example.trademodel.service.OpportunityStateIdentity;
import org.example.trademodel.service.AssetStateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("smoke")
class AssetStateServiceImplTest {

    @Mock
    private AssetStateMapper assetStateMapper;
    @Mock
    private HotResetEventMapper hotResetEventMapper;
    @Mock
    private OpportunityStateTransitionMapper transitionMapper;

    private AssetStateServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper);
    }

    @Test
    void recordHotResetEvent_blankSymbol_skipsAllWrites() {
        service.recordHotResetEvent("a-1", "tr-1", "   ", "CONFUSED", "42",
                "d-1", AssetStateEnum.CONFUSED, 42, false,
                "HOT_RESET_MIN_RULE", "reason", 1, LocalDateTime.now(),
                AssetStateEnum.CONFUSED, AssetStateEnum.OBSERVING);

        verify(assetStateMapper, never()).mergeUpsertCore(any());
        verify(assetStateMapper, never()).updateHotResetColumns(any());
        verify(hotResetEventMapper, never()).insert(any());
    }

    @Test
    void recordHotResetEvent_missingStateRow_seedsAndWritesEvent() {
        when(assetStateMapper.selectBySymbolAndTimeframe("BTCUSDT", "global")).thenReturn(null);

        service.recordHotResetEvent("a-2", "tr-2", " BTCUSDT ", "CONFUSED", "41",
                "d-2", AssetStateEnum.CONFUSED, 41, false,
                "HOT_RESET_MIN_RULE", "reason", 1, LocalDateTime.now(),
                AssetStateEnum.CONFUSED, AssetStateEnum.OBSERVING);

        verify(assetStateMapper).mergeUpsertCore(any(AssetStateDO.class));
        verify(assetStateMapper).updateHotResetColumns(any(AssetStateDO.class));
        verify(hotResetEventMapper).insert(any(HotResetEventDO.class));
    }

    @Test
    void recordHotResetEvent_blankAnalysisIdFailsClosedBeforeAnyStateWrite() {
        assertThatThrownBy(() -> service.recordHotResetEvent("   ", "tr-3", "ETHUSDT", "CONFUSED", "40",
                "d-3", AssetStateEnum.CONFUSED, 40, false,
                "HOT_RESET_MIN_RULE", "reason", 1, LocalDateTime.now(),
                AssetStateEnum.CONFUSED, AssetStateEnum.OBSERVING))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("analysisId is required");

        verify(assetStateMapper, never()).mergeUpsertCore(any());
        verify(assetStateMapper, never()).updateHotResetColumns(any());
        verify(hotResetEventMapper, never()).insert(any());
    }

    @Test
    void persistAuthoritativeState_trimsSymbolBeforeUpsert() {
        service.persistAuthoritativeState(" SOLUSDT ", AssetStateEnum.CANDIDATE, 12, 1, "tr-9");

        ArgumentCaptor<AssetStateDO> captor = ArgumentCaptor.forClass(AssetStateDO.class);
        verify(assetStateMapper).mergeUpsertCore(captor.capture());
        assertThat(captor.getValue().getSymbol()).isEqualTo("SOLUSDT");
        assertThat(captor.getValue().getConfusedLowStreak()).isEqualTo(1);
    }

    @Test
    void buildSnapshotAtDecision_includesP12TransitionFields() {
        String json = service.buildSnapshotAtDecision(
                "BTCUSDT",
                "ana-9",
                AssetStateEnum.CONFUSED,
                AssetStateEnum.COOLING,
                54,
                0,
                false,
                true);

        assertThat(json).contains("\"previousState\":\"CONFUSED\"");
        assertThat(json).contains("\"nextState\":\"COOLING\"");
        assertThat(json).contains("\"confusedLowStreak\":0");
        assertThat(json).contains("\"directionalPushBlocked\":false");
    }

    @Test
    void canonicalStateServiceIsTheOnlyProductionCallerOfAuthoritativeStateUpsert() throws Exception {
        Path productionRoot = Path.of("src/main/java");
        try (var files = Files.walk(productionRoot)) {
            assertThat(files
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            return Files.readString(path).contains(".mergeUpsertCore(");
                        } catch (java.io.IOException exception) {
                            throw new java.io.UncheckedIOException(exception);
                        }
                    })
                    .map(path -> productionRoot.relativize(path).toString())
                    .toList())
                    .containsExactly("org/example/trademodel/service/impl/AssetStateServiceImpl.java");
        }
    }

    @Test
    void transitionUsesOneCanonicalWriteAndRecordsAuditableReason() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        when(assetStateMapper.selectBySymbolAndTimeframe("BTCUSDT", "global")).thenReturn(null);

        OpportunityTransitionResult result = service.transition(
                "btcusdt", AssetStateEnum.CANDIDATE, 12, 0,
                "analysis-1", "trace-1", "SCORE_PROMOTED", OpportunityTriggerSource.ASSET_POOL_SCAN);

        ArgumentCaptor<AssetStateDO> state = ArgumentCaptor.forClass(AssetStateDO.class);
        ArgumentCaptor<OpportunityStateTransitionDO> audit =
                ArgumentCaptor.forClass(OpportunityStateTransitionDO.class);
        verify(assetStateMapper).mergeUpsertCore(state.capture());
        verify(transitionMapper).insert(audit.capture());
        assertThat(result.opportunityId()).isEqualTo("opp-btcusdt-global");
        assertThat(result.previousState()).isEqualTo(AssetStateEnum.OBSERVING);
        assertThat(result.state()).isEqualTo(AssetStateEnum.CANDIDATE);
        assertThat(result.executionPermission()).isEqualTo("ADVISORY_ALLOWED");
        assertThat(state.getValue().getOpportunityId()).isEqualTo(result.opportunityId());
        assertThat(state.getValue().getTimeframe()).isEqualTo("global");
        assertThat(audit.getValue().getFromState()).isNull();
        assertThat(audit.getValue().getToState()).isEqualTo("CANDIDATE");
        assertThat(audit.getValue().getReason()).isEqualTo("SCORE_PROMOTED");
        assertThat(audit.getValue().getTriggerSource()).isEqualTo("ASSET_POOL_SCAN");
        assertThat(audit.getValue().getTraceId()).isEqualTo("trace-1");
        assertThat(audit.getValue().getTimeframe()).isEqualTo("global");
    }

    @Test
    void ordinaryTransitionInsideDebounceWindowIsSuppressedAndAudited() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.OBSERVING);
        current.setStateEnteredAt(LocalDateTime.now());
        current.setLastUpdateTime(LocalDateTime.now());
        when(assetStateMapper.selectBySymbolAndTimeframe("ETHUSDT", "global")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "ETHUSDT", AssetStateEnum.CANDIDATE, 10, 0,
                "analysis-2", "trace-2", "ORDINARY_PROMOTION", OpportunityTriggerSource.ANALYSIS);

        assertThat(result.state()).isEqualTo(AssetStateEnum.OBSERVING);
        assertThat(result.changed()).isFalse();
        assertThat(result.suppressed()).isTrue();
        assertThat(result.reason()).startsWith("DEBOUNCED:");
        ArgumentCaptor<OpportunityStateTransitionDO> audit =
                ArgumentCaptor.forClass(OpportunityStateTransitionDO.class);
        verify(transitionMapper).insert(audit.capture());
        assertThat(audit.getValue().getSuppressed()).isTrue();
    }

    @Test
    void confusedAndInvalidatedPrecedenceBlockLowerPriorityTransitions() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO confused = currentState(AssetStateEnum.CONFUSED);
        confused.setLastUpdateTime(LocalDateTime.now().minusMinutes(5));
        when(assetStateMapper.selectBySymbolAndTimeframe("SOLUSDT", "global")).thenReturn(confused);

        OpportunityTransitionResult confusedResult = service.transition(
                "SOLUSDT", AssetStateEnum.INVALIDATED, 80, 0,
                "analysis-3", "trace-3", "INVALIDATION", OpportunityTriggerSource.INVALIDATION);
        assertThat(confusedResult.state()).isEqualTo(AssetStateEnum.CONFUSED);
        assertThat(confusedResult.executionPermission()).isEqualTo("BLOCKED");

        AssetStateDO invalidated = currentState(AssetStateEnum.INVALIDATED);
        invalidated.setLastUpdateTime(LocalDateTime.now().minusMinutes(5));
        when(assetStateMapper.selectBySymbolAndTimeframe("XRPUSDT", "global")).thenReturn(invalidated);
        OpportunityTransitionResult invalidatedResult = service.transition(
                "XRPUSDT", AssetStateEnum.CANDIDATE, 5, 0,
                "analysis-4", "trace-4", "LOW_PRIORITY", OpportunityTriggerSource.ANALYSIS);
        assertThat(invalidatedResult.state()).isEqualTo(AssetStateEnum.INVALIDATED);
        assertThat(invalidatedResult.executionPermission()).isEqualTo("NOT_ELIGIBLE");
    }

    @Test
    void hotResetHasHighestPriorityAndCanExitConfused() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.CONFUSED);
        current.setLastUpdateTime(LocalDateTime.now());
        when(assetStateMapper.selectBySymbolAndTimeframe("BNBUSDT", "global")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "BNBUSDT", AssetStateEnum.OBSERVING, 0, 0,
                "analysis-5", "trace-5", "HOT_RESET_REBUILD", OpportunityTriggerSource.HOT_RESET);

        assertThat(result.state()).isEqualTo(AssetStateEnum.OBSERVING);
        assertThat(result.changed()).isTrue();
        ArgumentCaptor<OpportunityStateTransitionDO> audit =
                ArgumentCaptor.forClass(OpportunityStateTransitionDO.class);
        verify(transitionMapper).insert(audit.capture());
        assertThat(audit.getValue().getTransitionPriority()).isEqualTo(400);
    }

    @Test
    void activeCoolingWindowSuppressesOrdinaryPromotion() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.COOLING);
        current.setCoolingUntil(LocalDateTime.now().plusMinutes(10));
        current.setLastUpdateTime(LocalDateTime.now().minusMinutes(5));
        when(assetStateMapper.selectBySymbolAndTimeframe("ADAUSDT", "global")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "ADAUSDT", AssetStateEnum.CANDIDATE, 0, 0,
                "analysis-6", "trace-6", "PROMOTION_DURING_COOLING", OpportunityTriggerSource.ANALYSIS);

        assertThat(result.state()).isEqualTo(AssetStateEnum.COOLING);
        assertThat(result.changed()).isFalse();
        assertThat(result.suppressed()).isTrue();
        assertThat(result.reason()).startsWith("PRECEDENCE_PRESERVED:");

        ArgumentCaptor<AssetStateDO> state = ArgumentCaptor.forClass(AssetStateDO.class);
        verify(assetStateMapper).mergeUpsertCore(state.capture());
        assertThat(state.getValue().getCoolingUntil()).isEqualTo(current.getCoolingUntil());
    }

    @Test
    void expiredCoolingWindowReturnsToObservingBeforeNormalPromotion() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.COOLING);
        current.setCoolingUntil(LocalDateTime.now(Clock.systemUTC()).minusSeconds(1));
        current.setStateEnteredAt(LocalDateTime.now(Clock.systemUTC()).minusMinutes(16));
        current.setLastUpdateTime(LocalDateTime.now(Clock.systemUTC()));
        when(assetStateMapper.selectBySymbolAndTimeframe("ADAUSDT", "5m")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "ADAUSDT", "5m", AssetStateEnum.CANDIDATE, 0, 0,
                "analysis-after-cooling", "trace-after-cooling", "COOLING_COMPLETED",
                OpportunityTriggerSource.ANALYSIS);

        assertThat(result.state()).isEqualTo(AssetStateEnum.OBSERVING);
        assertThat(result.changed()).isTrue();
        assertThat(result.suppressed()).isFalse();
    }

    @Test
    void invalidatedCanEnterCoolingAndAuditRecordsTheTransition() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.INVALIDATED);
        current.setStateEnteredAt(LocalDateTime.now());
        when(assetStateMapper.selectBySymbolAndTimeframe("BTCUSDT", "5m")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "BTCUSDT", "5m", AssetStateEnum.COOLING, 20, 0,
                "analysis-cooling", "trace-cooling", "INVALIDATION_COOLDOWN",
                OpportunityTriggerSource.ANALYSIS);

        assertThat(result.previousState()).isEqualTo(AssetStateEnum.INVALIDATED);
        assertThat(result.state()).isEqualTo(AssetStateEnum.COOLING);
        assertThat(result.changed()).isTrue();
        ArgumentCaptor<OpportunityStateTransitionDO> audit =
                ArgumentCaptor.forClass(OpportunityStateTransitionDO.class);
        verify(transitionMapper).insert(audit.capture());
        assertThat(audit.getValue().getFromState()).isEqualTo("INVALIDATED");
        assertThat(audit.getValue().getToState()).isEqualTo("COOLING");
        assertThat(audit.getValue().getTimeframe()).isEqualTo("5m");
        assertThat(audit.getValue().getOccurredAt()).isNotNull();
    }

    @Test
    void confusedCannotEnterCoolingOrJumpDirectlyToTriggered() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.CONFUSED);
        current.setStateEnteredAt(LocalDateTime.now());
        when(assetStateMapper.selectBySymbolAndTimeframe("ETHUSDT", "1h")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "ETHUSDT", "1h", AssetStateEnum.COOLING, 30, 3,
                "analysis-confused-cooling", "trace-confused-cooling", "CONFUSED_RECOVERY",
                OpportunityTriggerSource.ANALYSIS);

        assertThat(result.previousState()).isEqualTo(AssetStateEnum.CONFUSED);
        assertThat(result.state()).isEqualTo(AssetStateEnum.CONFUSED);
        assertThat(result.changed()).isFalse();
        assertThat(result.suppressed()).isTrue();

        OpportunityTransitionResult triggered = service.transition(
                "ETHUSDT", "1h", AssetStateEnum.TRIGGERED, 30, 3,
                "analysis-confused-trigger", "trace-confused-trigger", "CONFUSED_DIRECT_TRIGGER",
                OpportunityTriggerSource.ANALYSIS);
        assertThat(triggered.state()).isEqualTo(AssetStateEnum.CONFUSED);
        assertThat(triggered.suppressed()).isTrue();
    }

    @Test
    void highRiskCanEnterCoolingWithoutOrdinaryDebounceBlockingTransition() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.HIGH_RISK);
        current.setStateEnteredAt(LocalDateTime.now());
        when(assetStateMapper.selectBySymbolAndTimeframe("LINKUSDT", "4h")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "LINKUSDT", "4h", AssetStateEnum.COOLING, 40, 0,
                "analysis-high-risk-cooling", "trace-high-risk-cooling", "HIGH_RISK_COOLDOWN",
                OpportunityTriggerSource.ANALYSIS);

        assertThat(result.previousState()).isEqualTo(AssetStateEnum.HIGH_RISK);
        assertThat(result.state()).isEqualTo(AssetStateEnum.COOLING);
        assertThat(result.changed()).isTrue();
        assertThat(result.suppressed()).isFalse();
    }

    @Test
    void sameSymbolUsesIndependentDebounceStatePerTimeframe() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO fiveMinute = currentState(AssetStateEnum.OBSERVING);
        fiveMinute.setStateEnteredAt(LocalDateTime.now());
        fiveMinute.setLastUpdateTime(LocalDateTime.now());
        when(assetStateMapper.selectBySymbolAndTimeframe("BTCUSDT", "5m")).thenReturn(fiveMinute);
        when(assetStateMapper.selectBySymbolAndTimeframe("BTCUSDT", "1h")).thenReturn(null);

        OpportunityTransitionResult fiveMinuteResult = service.transition(
                "BTCUSDT", "5m", AssetStateEnum.CANDIDATE, 10, 0,
                "analysis-5m", "trace-5m", "PROMOTE_5M", OpportunityTriggerSource.ANALYSIS);
        OpportunityTransitionResult oneHourResult = service.transition(
                "BTCUSDT", "1h", AssetStateEnum.CANDIDATE, 10, 0,
                "analysis-1h", "trace-1h", "PROMOTE_1H", OpportunityTriggerSource.ANALYSIS);

        assertThat(fiveMinuteResult.state()).isEqualTo(AssetStateEnum.OBSERVING);
        assertThat(fiveMinuteResult.suppressed()).isTrue();
        assertThat(oneHourResult.state()).isEqualTo(AssetStateEnum.CANDIDATE);
        assertThat(oneHourResult.suppressed()).isFalse();
        assertThat(oneHourResult.opportunityId()).isEqualTo("opp-btcusdt-1h");
    }

    @Test
    void dueLightweightScanUsesAtomicPersistentClaimAndComputesNextEligibility() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 12, 0);
        AssetStateDO current = currentState(AssetStateEnum.OBSERVING);
        current.setOpportunityId("opp-user-btc-5m");
        current.setLastUpdateTime(now.minusMinutes(16));
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "5m"))
                .thenReturn(current);
        when(assetStateMapper.claimScheduledScan(
                "USER", 42L, "BTCUSDT", "5m", "OBSERVING",
                current.getLastUpdateTime(), now, "trace-scan", "rules-v1"))
                .thenReturn(1);

        AssetStateService.ScheduledScanClaim claim = service.claimScheduledScan(
                new OpportunityStateIdentity("USER", 42L, 9001L, "BTCUSDT", "5m"),
                77L, now, 900L, "trace-scan", "rules-v1");

        assertThat(claim).isNotNull();
        assertThat(claim.scheduledAt()).isEqualTo(now.minusMinutes(1));
        assertThat(claim.startedAt()).isEqualTo(now);
        assertThat(claim.nextEligibleScanAt()).isEqualTo(now.plusMinutes(15));
        assertThat(claim.state()).isEqualTo(AssetStateEnum.OBSERVING);
    }

    @Test
    void notDueOrLosingAtomicClaimCreatesNoScan() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 12, 12, 0);
        OpportunityStateIdentity identity =
                new OpportunityStateIdentity("USER", 42L, 9001L, "BTCUSDT", "5m");
        AssetStateDO notDue = currentState(AssetStateEnum.CANDIDATE);
        notDue.setLastUpdateTime(now.minusMinutes(4));
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "5m"))
                .thenReturn(notDue);

        assertThat(service.claimScheduledScan(
                identity, 77L, now, 300L, "trace-not-due", "rules-v1")).isNull();
        verify(assetStateMapper, never()).claimScheduledScan(
                any(), any(), any(), any(), any(), any(), any(), any(), any());

        AssetStateDO due = currentState(AssetStateEnum.CANDIDATE);
        due.setLastUpdateTime(now.minusMinutes(6));
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "5m"))
                .thenReturn(due);
        when(assetStateMapper.claimScheduledScan(
                "USER", 42L, "BTCUSDT", "5m", "CANDIDATE",
                due.getLastUpdateTime(), now, "trace-lost-race", "rules-v1"))
                .thenReturn(0);

        assertThat(service.claimScheduledScan(
                identity, 77L, now, 300L, "trace-lost-race", "rules-v1")).isNull();
    }

    @Test
    void scanCompletionPersistsRequiredAuditWithoutChangingOpportunityState() {
        LocalDateTime started = LocalDateTime.of(2026, 8, 12, 12, 0);
        AssetStateService.ScheduledScanClaim claim = new AssetStateService.ScheduledScanClaim(
                new OpportunityStateIdentity("USER", 42L, 9001L, "BTCUSDT", "5m"),
                "opp-user-btc-5m", AssetStateEnum.TRIGGERED, "analysis-1", "HIGH", null,
                "trace-scan", "rules-v1", started.minusMinutes(1), started,
                started.plusMinutes(1), null);
        when(assetStateMapper.completeScheduledScanAudit(any(), eq("trace-scan"), eq(null))).thenReturn(1);

        boolean completed = service.completeScheduledScan(
                claim, started.plusSeconds(2), "NO_MATERIAL_CHANGE", null,
                "FRESH:BINANCE_PUBLIC:SPOT", "5m=UP;15m=UP;1h=UP;4h=UP",
                12345L, null, false, false);

        assertThat(completed).isTrue();
        ArgumentCaptor<AssetStateDO> row = ArgumentCaptor.forClass(AssetStateDO.class);
        verify(assetStateMapper).completeScheduledScanAudit(
                row.capture(), eq("trace-scan"), eq(null));
        assertThat(row.getValue().getExtJson())
                .contains("\"scheduledAt\"")
                .contains("\"startedAt\"")
                .contains("\"finishedAt\"")
                .contains("\"result\":\"NO_MATERIAL_CHANGE\"")
                .contains("\"traceId\":\"trace-scan\"")
                .contains("\"ruleVersion\":\"rules-v1\"")
                .contains("\"dataFreshness\":\"FRESH:BINANCE_PUBLIC:SPOT\"")
                .contains("\"fullAnalysisSucceeded\":false")
                .contains("\"nextEligibleScanAt\"");
        assertThat(row.getValue().getState()).isNull();
    }

    @Test
    void failedFullAnalysisPreservesLastSuccessfulScanWatermarks() {
        LocalDateTime started = LocalDateTime.of(2026, 8, 12, 12, 0);
        String previous = "{\"schedulerScan\":{"
                + "\"latestFullAnalysisCloseTimeMs\":1000,"
                + "\"latestFullStructureSignature\":\"old-signature\","
                + "\"latestFullHotResetAt\":\"2026-08-12T10:00\"}}";
        AssetStateDO current = currentState(AssetStateEnum.TRIGGERED);
        current.setExtJson(previous);
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "5m"))
                .thenReturn(current);
        when(assetStateMapper.completeScheduledScanAudit(
                any(), eq("trace-scan"), eq("analysis-trace"))).thenReturn(1);
        AssetStateService.ScheduledScanClaim claim = new AssetStateService.ScheduledScanClaim(
                new OpportunityStateIdentity("USER", 42L, 9001L, "BTCUSDT", "5m"),
                "opp-user-btc-5m", AssetStateEnum.TRIGGERED, "analysis-1", "HIGH",
                started.minusMinutes(1), "trace-scan", "rules-v1",
                started.minusMinutes(1), started, started.plusMinutes(1), previous);

        boolean completed = service.completeScheduledScan(
                claim, started.plusSeconds(5), "TRIGGERED_RECHECK:FAILED", "PROVIDER_TIMEOUT",
                "FRESH:BINANCE_PUBLIC:SPOT", "new-signature", 2000L,
                "analysis-trace", true, false);

        assertThat(completed).isTrue();
        ArgumentCaptor<AssetStateDO> row = ArgumentCaptor.forClass(AssetStateDO.class);
        verify(assetStateMapper).completeScheduledScanAudit(
                row.capture(), eq("trace-scan"), eq("analysis-trace"));
        assertThat(row.getValue().getExtJson())
                .contains("\"fullAnalysisRequested\":true")
                .contains("\"fullAnalysisSucceeded\":false")
                .contains("\"latestFullAnalysisCloseTimeMs\":1000")
                .contains("\"latestFullStructureSignature\":\"old-signature\"")
                .contains("\"latestFullHotResetAt\":\"2026-08-12T10:00\"");
    }

    @Test
    void opportunityProjectionPreservesSchedulerAuditInSameAssetStateOwner() {
        AssetStateDO current = currentState(AssetStateEnum.CANDIDATE);
        current.setExtJson("{\"schedulerScan\":{\"result\":\"NO_MATERIAL_CHANGE\"},"
                + "\"legacy\":true}");
        when(assetStateMapper.selectByIdentity("USER", 42L, "BTCUSDT", "5m"))
                .thenReturn(current);
        when(assetStateMapper.updateOpportunityProjection(any())).thenReturn(1);

        service.recordOpportunityProjection(
                new OpportunityStateIdentity("USER", 42L, 9001L, "BTCUSDT", "5m"),
                77L, "analysis-new", "trace-new", "rules-v1", 82, "HIGH", "MEDIUM",
                "{\"schemaVersion\":\"FUNDAMENTAL_AI_V4_1_OPPORTUNITY_V1\"}");

        ArgumentCaptor<AssetStateDO> row = ArgumentCaptor.forClass(AssetStateDO.class);
        verify(assetStateMapper).updateOpportunityProjection(row.capture());
        assertThat(row.getValue().getExtJson())
                .contains("\"schemaVersion\":\"FUNDAMENTAL_AI_V4_1_OPPORTUNITY_V1\"")
                .contains("\"schedulerScan\":{\"result\":\"NO_MATERIAL_CHANGE\"}")
                .doesNotContain("\"legacy\":true");
    }

    private static AssetStateDO currentState(AssetStateEnum state) {
        AssetStateDO row = new AssetStateDO();
        row.setSymbol("SYMBOL");
        row.setState(state);
        row.setOpportunityId("opp-existing");
        row.setStateEnteredAt(LocalDateTime.now().minusHours(1));
        row.setLastUpdateTime(LocalDateTime.now().minusMinutes(1));
        row.setTraceId("trace-existing");
        return row;
    }
}
