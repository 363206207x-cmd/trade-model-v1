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
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

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
        when(assetStateMapper.selectBySymbol("ETHUSDT")).thenReturn(new AssetStateDO());

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
        when(assetStateMapper.selectBySymbol("BTCUSDT")).thenReturn(null);

        OpportunityTransitionResult result = service.transition(
                "btcusdt", AssetStateEnum.CANDIDATE, 12, 0,
                "analysis-1", "trace-1", "SCORE_PROMOTED", OpportunityTriggerSource.ASSET_POOL_SCAN);

        ArgumentCaptor<AssetStateDO> state = ArgumentCaptor.forClass(AssetStateDO.class);
        ArgumentCaptor<OpportunityStateTransitionDO> audit =
                ArgumentCaptor.forClass(OpportunityStateTransitionDO.class);
        verify(assetStateMapper).mergeUpsertCore(state.capture());
        verify(transitionMapper).insert(audit.capture());
        assertThat(result.opportunityId()).isEqualTo("opp-btcusdt");
        assertThat(result.previousState()).isEqualTo(AssetStateEnum.OBSERVING);
        assertThat(result.state()).isEqualTo(AssetStateEnum.CANDIDATE);
        assertThat(result.executionPermission()).isEqualTo("ADVISORY_ALLOWED");
        assertThat(state.getValue().getOpportunityId()).isEqualTo(result.opportunityId());
        assertThat(audit.getValue().getFromState()).isNull();
        assertThat(audit.getValue().getToState()).isEqualTo("CANDIDATE");
        assertThat(audit.getValue().getReason()).isEqualTo("SCORE_PROMOTED");
        assertThat(audit.getValue().getTriggerSource()).isEqualTo("ASSET_POOL_SCAN");
        assertThat(audit.getValue().getTraceId()).isEqualTo("trace-1");
    }

    @Test
    void ordinaryTransitionInsideDebounceWindowIsSuppressedAndAudited() {
        service = new AssetStateServiceImpl(assetStateMapper, hotResetEventMapper, transitionMapper);
        AssetStateDO current = currentState(AssetStateEnum.OBSERVING);
        current.setLastUpdateTime(LocalDateTime.now());
        when(assetStateMapper.selectBySymbol("ETHUSDT")).thenReturn(current);

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
        when(assetStateMapper.selectBySymbol("SOLUSDT")).thenReturn(confused);

        OpportunityTransitionResult confusedResult = service.transition(
                "SOLUSDT", AssetStateEnum.INVALIDATED, 80, 0,
                "analysis-3", "trace-3", "INVALIDATION", OpportunityTriggerSource.INVALIDATION);
        assertThat(confusedResult.state()).isEqualTo(AssetStateEnum.CONFUSED);
        assertThat(confusedResult.executionPermission()).isEqualTo("BLOCKED");

        AssetStateDO invalidated = currentState(AssetStateEnum.INVALIDATED);
        invalidated.setLastUpdateTime(LocalDateTime.now().minusMinutes(5));
        when(assetStateMapper.selectBySymbol("XRPUSDT")).thenReturn(invalidated);
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
        when(assetStateMapper.selectBySymbol("BNBUSDT")).thenReturn(current);

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
        when(assetStateMapper.selectBySymbol("ADAUSDT")).thenReturn(current);

        OpportunityTransitionResult result = service.transition(
                "ADAUSDT", AssetStateEnum.CANDIDATE, 0, 0,
                "analysis-6", "trace-6", "PROMOTION_DURING_COOLING", OpportunityTriggerSource.ANALYSIS);

        assertThat(result.state()).isEqualTo(AssetStateEnum.COOLING);
        assertThat(result.changed()).isFalse();
        assertThat(result.suppressed()).isTrue();
        assertThat(result.reason()).startsWith("PRECEDENCE_PRESERVED:");
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
