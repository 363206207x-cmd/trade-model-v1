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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
    void recordHotResetEvent_blankAnalysisId_updatesStateButSkipsEventInsert() {
        when(assetStateMapper.selectBySymbolAndTimeframe("ETHUSDT", "global")).thenReturn(new AssetStateDO());

        service.recordHotResetEvent("   ", "tr-3", "ETHUSDT", "CONFUSED", "40",
                "d-3", AssetStateEnum.CONFUSED, 40, false,
                "HOT_RESET_MIN_RULE", "reason", 1, LocalDateTime.now(),
                AssetStateEnum.CONFUSED, AssetStateEnum.OBSERVING);

        verify(assetStateMapper).updateHotResetColumns(any(AssetStateDO.class));
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
    void expiredCoolingWindowAllowsPromotionEvenAfterRecentSuppressedEvaluation() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.COOLING);
        current.setCoolingUntil(LocalDateTime.now().minusSeconds(1));
        current.setStateEnteredAt(LocalDateTime.now().minusMinutes(16));
        current.setLastUpdateTime(LocalDateTime.now());
        when(assetStateMapper.selectBySymbolAndTimeframe("ADAUSDT", "5m")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "ADAUSDT", "5m", AssetStateEnum.CANDIDATE, 0, 0,
                "analysis-after-cooling", "trace-after-cooling", "COOLING_COMPLETED",
                OpportunityTriggerSource.ANALYSIS);

        assertThat(result.state()).isEqualTo(AssetStateEnum.CANDIDATE);
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
    void confusedCanEnterCoolingWithoutOrdinaryDebounceBlockingRecovery() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.CONFUSED);
        current.setStateEnteredAt(LocalDateTime.now());
        when(assetStateMapper.selectBySymbolAndTimeframe("ETHUSDT", "1h")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "ETHUSDT", "1h", AssetStateEnum.COOLING, 30, 3,
                "analysis-confused-cooling", "trace-confused-cooling", "CONFUSED_RECOVERY",
                OpportunityTriggerSource.ANALYSIS);

        assertThat(result.previousState()).isEqualTo(AssetStateEnum.CONFUSED);
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
