package org.example.trademodel.service.impl;

import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.entity.PositionMonitorRecordDO;
import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.DecisionResultMapper;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PositionMonitorRecordMapper;
import org.example.trademodel.mapper.RealPositionMapper;
import org.example.trademodel.vo.DecisionResultVO;
import org.example.trademodel.vo.PositionMonitorOpenRowVO;
import org.example.trademodel.vo.RealPositionVO;
import org.example.trademodel.service.support.PlanBoundaryDisplayContext;
import org.example.trademodel.service.support.PlanBoundaryDisplayHelper;
import org.example.trademodel.service.support.PlanBoundaryDisplayInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PositionMonitorServiceImplTest {
    @Mock
    private RealPositionMapper realPositionMapper;
    @Mock
    private DecisionResultMapper decisionResultMapper;
    @Mock
    private ExecutionPlanMapper executionPlanMapper;
    @Mock
    private PositionMonitorRecordMapper positionMonitorRecordMapper;
    @Mock
    private AnalysisRunMapper analysisRunMapper;

    private PositionMonitorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PositionMonitorServiceImpl(
                realPositionMapper,
                decisionResultMapper,
                executionPlanMapper,
                analysisRunMapper,
                positionMonitorRecordMapper
        );
    }

    /** Align display VO boundary fields with persisted plan_boundary_json on {@link ExecutionPlanDO}. */
    private void stubSelectByPlanId(ExecutionPlanDO plan) {
        if (plan != null && plan.getPlanId() != null) {
            when(executionPlanMapper.selectByPlanId(plan.getPlanId())).thenReturn(plan);
        }
    }

    @Test
    void evaluate_for_symbol_only_processes_target_symbol_manual_open_positions() {
        RealPositionVO manual = openPosition("pos-manual", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        RealPositionVO nonManual = openPosition("pos-non-manual", "BTCUSDT", "SIMULATED", "BOT", "LONG");
        when(realPositionMapper.selectOpenManualPositionsBySymbol("BTCUSDT")).thenReturn(List.of(manual, nonManual));
        when(realPositionMapper.selectOpenPositionById("pos-manual")).thenReturn(manual);
        when(realPositionMapper.selectOpenPositionById("pos-non-manual")).thenReturn(nonManual);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-manual")).thenReturn(null);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(null);

        service.evaluateForSymbol("btcusdt");

        verify(analysisRunMapper, never()).selectById(any());

        verify(positionMonitorRecordMapper).insert(any(PositionMonitorRecordDO.class));
        verify(positionMonitorRecordMapper, never()).selectLatestByPositionId("pos-non-manual");
    }

    @Test
    void evaluate_for_position_force_persist_inserts_even_without_decision() {
        RealPositionVO manual = openPosition("pos-force", "XRPUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        when(realPositionMapper.selectOpenPositionById("pos-force")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("XRPUSDT")).thenReturn(null);

        service.evaluateForPosition("pos-force", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getMonitorSummary()).contains("输入不足");
    }

    @Test
    void evaluate_for_position_no_change_does_not_insert_when_not_forced() {
        RealPositionVO manual = openPosition("pos-no-change", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-1", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        ExecutionPlanDO plan = plan("plan-1");
        PositionMonitorRecordDO previous = previous("pos-no-change", "BTCUSDT", LocalDateTime.now().minusMinutes(5));

        when(realPositionMapper.selectOpenPositionById("pos-no-change")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-1")).thenReturn(plan);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-no-change")).thenReturn(previous);

        service.evaluateForPosition("pos-no-change", false);

        verify(positionMonitorRecordMapper, never()).insert(any(PositionMonitorRecordDO.class));
    }

    @Test
    void should_persist_first_record_true() {
        assertTrue(PositionMonitorServiceImpl.shouldPersist(null, candidate(LocalDateTime.now()), LocalDateTime.now()));
    }

    @Test
    void should_persist_no_change_false() {
        LocalDateTime now = LocalDateTime.now();
        PositionMonitorRecordDO previous = candidate(now.minusMinutes(5));
        PositionMonitorRecordDO next = candidate(now);
        assertFalse(PositionMonitorServiceImpl.shouldPersist(previous, next, now));
    }

    @Test
    void should_persist_entry_logic_changed_true() {
        LocalDateTime now = LocalDateTime.now();
        PositionMonitorRecordDO previous = candidate(now.minusMinutes(5));
        PositionMonitorRecordDO next = candidate(now);
        next.setEntryLogicState("WEAKENED");
        assertTrue(PositionMonitorServiceImpl.shouldPersist(previous, next, now));
    }

    @Test
    void should_persist_reversal_upgraded_true() {
        LocalDateTime now = LocalDateTime.now();
        PositionMonitorRecordDO previous = candidate(now.minusMinutes(5));
        PositionMonitorRecordDO next = candidate(now);
        previous.setReversalState("NONE");
        next.setReversalState("STRONG");
        assertTrue(PositionMonitorServiceImpl.shouldPersist(previous, next, now));
    }

    @Test
    void should_persist_risk_upgraded_true() {
        LocalDateTime now = LocalDateTime.now();
        PositionMonitorRecordDO previous = candidate(now.minusMinutes(5));
        PositionMonitorRecordDO next = candidate(now);
        previous.setPositionRiskLevel("MEDIUM");
        next.setPositionRiskLevel("HIGH");
        assertTrue(PositionMonitorServiceImpl.shouldPersist(previous, next, now));
    }

    @Test
    void should_persist_action_changed_true() {
        LocalDateTime now = LocalDateTime.now();
        PositionMonitorRecordDO previous = candidate(now.minusMinutes(5));
        PositionMonitorRecordDO next = candidate(now);
        previous.setSystemSuggestedAction("NO_ADD");
        next.setSystemSuggestedAction("TIGHTEN_STOP");
        assertTrue(PositionMonitorServiceImpl.shouldPersist(previous, next, now));
    }

    @Test
    void should_persist_heartbeat_over_30m_true() {
        LocalDateTime now = LocalDateTime.now();
        PositionMonitorRecordDO previous = candidate(now.minusMinutes(31));
        PositionMonitorRecordDO next = candidate(now);
        assertTrue(PositionMonitorServiceImpl.shouldPersist(previous, next, now));
    }

    @Test
    void monitor_summary_is_chinese_not_technical_key_value() {
        String summary = PositionMonitorServiceImpl.buildMonitorSummary(
                false, "INVALIDATED", "RANGE", "STRONG", "EXTREME", "CLOSE_AND_ENTER_REVIEW", null);
        assertThat(summary).contains("监控摘要：");
        assertThat(summary).doesNotContain("entry_logic=");
        assertThat(summary).doesNotContain("direction_support=");
    }

    @Test
    void confused_score_at_least_70_enters_wait_confirm_and_high_risk() {
        RealPositionVO manual = openPosition("pos-confused", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-c", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        decision.setConfusedScore(72);
        ExecutionPlanDO plan = plan("plan-c");

        when(realPositionMapper.selectOpenPositionById("pos-confused")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-c")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-c")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-confused")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-confused", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getPositionRiskLevel()).isEqualTo("HIGH");
        assertThat(captor.getValue().getSystemSuggestedAction()).isEqualTo("PLAN_INVALID_WAIT_CONFIRM");
        assertThat(captor.getValue().getMonitorSummary()).contains("困惑");
        assertThat(captor.getValue().getMonitorSummary()).contains("人工确认");
    }

    @Test
    void confused_score_at_least_85_forces_extreme_risk_and_wait_confirm() {
        RealPositionVO manual = openPosition("pos-confused-85", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-c85", "BULLISH", "LOW", true, "STRONG", "LEVEL_2_LIGHT_DIVERGENCE", 45, null);
        decision.setConfusedScore(86);
        ExecutionPlanDO plan = plan("plan-c85");

        when(realPositionMapper.selectOpenPositionById("pos-confused-85")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-c85")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-c85")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-confused-85")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-confused-85", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getPositionRiskLevel()).isEqualTo("EXTREME");
        assertThat(captor.getValue().getSystemSuggestedAction()).isEqualTo("PLAN_INVALID_WAIT_CONFIRM");
    }

    @Test
    void ai_conflict_level_3_enters_wait_confirm() {
        RealPositionVO manual = openPosition("pos-l3", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-l3", "BULLISH", "LOW", true, "STRONG", "LEVEL_3_SIGNIFICANT_DIVERGENCE", 60, null);
        ExecutionPlanDO plan = plan("plan-l3");

        when(realPositionMapper.selectOpenPositionById("pos-l3")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-l3")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-l3")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-l3")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-l3", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getSystemSuggestedAction()).isEqualTo("PLAN_INVALID_WAIT_CONFIRM");
        assertThat(captor.getValue().getPositionRiskLevel()).isEqualTo("HIGH");
    }

    @Test
    void ai_conflict_level_4_forces_extreme_and_wait_confirm() {
        RealPositionVO manual = openPosition("pos-l4", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-l4", "BULLISH", "LOW", true, "STRONG", "LEVEL_4_EXTREME_DIVERGENCE", 88, null);
        ExecutionPlanDO plan = plan("plan-l4");

        when(realPositionMapper.selectOpenPositionById("pos-l4")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-l4")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-l4")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-l4")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-l4", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getSystemSuggestedAction()).isEqualTo("PLAN_INVALID_WAIT_CONFIRM");
        assertThat(captor.getValue().getPositionRiskLevel()).isEqualTo("EXTREME");
        assertThat(captor.getValue().getReversalState()).isEqualTo("STRONG");
    }

    @Test
    void asset_state_confused_forces_wait_confirm() {
        RealPositionVO manual = openPosition("pos-as-confused", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-as-confused", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        decision.setAssetStateSnapshot("{\"assetState\":\"CONFUSED\"}");
        ExecutionPlanDO plan = plan("plan-as-confused");

        when(realPositionMapper.selectOpenPositionById("pos-as-confused")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-as-confused")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-as-confused")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-as-confused")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-as-confused", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getSystemSuggestedAction()).isEqualTo("PLAN_INVALID_WAIT_CONFIRM");
    }

    @Test
    void asset_state_high_risk_forces_tighten_stop_or_wait_confirm() {
        RealPositionVO manual = openPosition("pos-as-high", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-as-high", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        decision.setAssetStateSnapshot("HIGH_RISK");
        ExecutionPlanDO plan = plan("plan-as-high");

        when(realPositionMapper.selectOpenPositionById("pos-as-high")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-as-high")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-as-high")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-as-high")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-as-high", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getPositionRiskLevel()).isEqualTo("HIGH");
        assertThat(captor.getValue().getSystemSuggestedAction()).isEqualTo("TIGHTEN_STOP");
        assertThat(captor.getValue().getReversalState()).isEqualTo("WEAK");
    }

    @Test
    void asset_state_invalidated_maps_to_wait_confirm_not_close() {
        RealPositionVO manual = openPosition("pos-as-invalid", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-as-invalid", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        decision.setAssetStateSnapshot("{\"state\":\"INVALIDATED\"}");
        ExecutionPlanDO plan = plan("plan-as-invalid");

        when(realPositionMapper.selectOpenPositionById("pos-as-invalid")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-as-invalid")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-as-invalid")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-as-invalid")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-as-invalid", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getSystemSuggestedAction()).isEqualTo("PLAN_INVALID_WAIT_CONFIRM");
        assertThat(captor.getValue().getPositionRiskLevel()).isEqualTo("EXTREME");
        assertThat(captor.getValue().getReversalState()).isEqualTo("STRONG");
    }

    @Test
    void monitor_summary_contains_reversal_watch_hint_on_weak_no_add_case() {
        String summary = PositionMonitorServiceImpl.buildMonitorSummary(
                false, "VALID", "SUPPORT_ORIGINAL", "WEAK", "MEDIUM", "NO_ADD", null);
        String merged = PositionMonitorServiceImpl.mergeSupplementalAndBoundaryNotes(summary, null);
        String finalSummary = PositionMonitorServiceImpl.appendReversalHint(
                merged, "VALID", "SUPPORT_ORIGINAL", "WEAK", "MEDIUM", "NO_ADD");
        assertThat(finalSummary).contains("反转观察");
    }

    @Test
    void monitor_summary_contains_reversal_warning_hint_on_weak_tighten_stop_case() {
        String summary = PositionMonitorServiceImpl.buildMonitorSummary(
                false, "WEAKENED", "CONFLICT_EXPANDING", "WEAK", "HIGH", "TIGHTEN_STOP", null);
        String finalSummary = PositionMonitorServiceImpl.appendReversalHint(
                summary, "WEAKENED", "CONFLICT_EXPANDING", "WEAK", "HIGH", "TIGHTEN_STOP");
        assertThat(finalSummary).contains("反转预警");
    }

    @Test
    void monitor_summary_contains_plan_invalidated_reversal_hint_on_wait_confirm_case() {
        String summary = PositionMonitorServiceImpl.buildMonitorSummary(
                false, "INVALIDATED", "RANGE", "STRONG", "EXTREME", "PLAN_INVALID_WAIT_CONFIRM", null);
        String finalSummary = PositionMonitorServiceImpl.appendReversalHint(
                summary, "INVALIDATED", "RANGE", "STRONG", "EXTREME", "PLAN_INVALID_WAIT_CONFIRM");
        assertThat(finalSummary).contains("原计划失效型反转");
    }

    @Test
    void asset_state_cooling_enters_wait_confirm() {
        RealPositionVO manual = openPosition("pos-as-cooling", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-as-cooling", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        decision.setAssetStateSnapshot("COOLING");
        ExecutionPlanDO plan = plan("plan-as-cooling");

        when(realPositionMapper.selectOpenPositionById("pos-as-cooling")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-as-cooling")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-as-cooling")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-as-cooling")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-as-cooling", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getSystemSuggestedAction()).isEqualTo("PLAN_INVALID_WAIT_CONFIRM");
    }

    @Test
    void low_data_quality_from_analysis_run_elevates_risk_without_close_action() {
        RealPositionVO manual = openPosition("pos-dq", "ETHUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-dq", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        ExecutionPlanDO plan = plan("plan-dq");
        AnalysisRunDO run = new AnalysisRunDO();
        run.setDataQualityScore(55);

        when(realPositionMapper.selectOpenPositionById("pos-dq")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("ETHUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-dq")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-dq")).thenReturn(run);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-dq")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-dq", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getPositionRiskLevel()).isEqualTo("HIGH");
        assertThat(captor.getValue().getSystemSuggestedAction()).isNotEqualTo("CLOSE_AND_ENTER_REVIEW");
        assertThat(captor.getValue().getMonitorSummary()).contains("数据质量");
        assertThat(captor.getValue().getMonitorSummary()).contains("参考");
    }

    @Test
    void null_confused_and_null_data_quality_does_not_break_evaluation() {
        RealPositionVO manual = openPosition("pos-nulls", "SOLUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-n", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        decision.setConfusedScore(null);
        ExecutionPlanDO plan = plan("plan-n");

        when(realPositionMapper.selectOpenPositionById("pos-nulls")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("SOLUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-n")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-n")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-nulls")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-nulls", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getSystemSuggestedAction()).isEqualTo("CONTINUE_HOLD");
        assertThat(PositionMonitorServiceImpl.buildSupplementalNotes(null, false)).isEmpty();
    }

    @Test
    void service_has_no_decision_engine_or_ai_dependency() {
        assertThat(PositionMonitorServiceImpl.class.getDeclaredFields())
                .extracting(f -> f.getType().getSimpleName())
                .doesNotContain("DecisionEngineService")
                .doesNotContain("AiConflictResolverService");
    }

    @Test
    void monitor_summary_contains_unstructured_boundary_note_when_plan_boundary_json_unstructured() {
        RealPositionVO manual = openPosition("pos-b-un", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-un", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        ExecutionPlanDO plan = plan("plan-un");
        plan.setPlanBoundaryJson("{\"boundaryParseStatus\":\"UNSTRUCTURED_TEXT_ONLY\",\"boundarySource\":\"AI_PLAN\"}");

        when(realPositionMapper.selectOpenPositionById("pos-b-un")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-un")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-un")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-b-un")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-b-un", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getMonitorSummary()).contains("计划价位边界当前仅为文本参考，尚未参与数值监护。");
    }

    @Test
    void monitor_summary_contains_parse_error_note_when_plan_boundary_json_invalid() {
        RealPositionVO manual = openPosition("pos-b-bad", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-bad", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        ExecutionPlanDO plan = plan("plan-bad");
        plan.setPlanBoundaryJson("{not json");

        when(realPositionMapper.selectOpenPositionById("pos-b-bad")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-bad")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-bad")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-b-bad")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-b-bad", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getMonitorSummary()).contains("计划价位边界结构无效，当前仅按文本参考。");
    }

    @Test
    void monitor_summary_contains_missing_boundary_note_when_plan_boundary_json_null() {
        RealPositionVO manual = openPosition("pos-b-null", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-bn", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        ExecutionPlanDO plan = plan("plan-bn");
        plan.setPlanBoundaryJson(null);

        when(realPositionMapper.selectOpenPositionById("pos-b-null")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-bn")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-bn")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-b-null")).thenReturn(null);
        stubSelectByPlanId(plan);

        service.evaluateForPosition("pos-b-null", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper).insert(captor.capture());
        assertThat(captor.getValue().getMonitorSummary()).contains("计划价位边界未返回，当前不参与数值监护。");
    }

    @Test
    void boundary_parse_status_does_not_change_action_or_risk_compared_to_same_inputs_without_boundary_field() {
        RealPositionVO manualA = openPosition("pos-cmp-a", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        RealPositionVO manualB = openPosition("pos-cmp-b", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-cmp", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        ExecutionPlanDO planWith = plan("plan-cmp");
        planWith.setPlanBoundaryJson("{\"boundaryParseStatus\":\"UNSTRUCTURED_TEXT_ONLY\"}");
        ExecutionPlanDO planWithout = plan("plan-cmp");
        planWithout.setPlanBoundaryJson(null);

        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(analysisRunMapper.selectById("ana-cmp")).thenReturn(null);

        when(realPositionMapper.selectOpenPositionById("pos-cmp-a")).thenReturn(manualA);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-cmp")).thenReturn(planWith);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-cmp-a")).thenReturn(null);
        stubSelectByPlanId(planWith);
        service.evaluateForPosition("pos-cmp-a", true);

        when(realPositionMapper.selectOpenPositionById("pos-cmp-b")).thenReturn(manualB);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-cmp")).thenReturn(planWithout);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-cmp-b")).thenReturn(null);
        stubSelectByPlanId(planWithout);
        service.evaluateForPosition("pos-cmp-b", true);

        ArgumentCaptor<PositionMonitorRecordDO> captor = ArgumentCaptor.forClass(PositionMonitorRecordDO.class);
        verify(positionMonitorRecordMapper, times(2)).insert(captor.capture());
        List<PositionMonitorRecordDO> inserted = captor.getAllValues();
        assertThat(inserted).hasSize(2);
        assertThat(inserted.get(1).getSystemSuggestedAction()).isEqualTo(inserted.get(0).getSystemSuggestedAction());
        assertThat(inserted.get(1).getPositionRiskLevel()).isEqualTo(inserted.get(0).getPositionRiskLevel());
        assertThat(inserted.get(0).getEntryLogicState()).isEqualTo(inserted.get(1).getEntryLogicState());
    }

    @Test
    void buildBoundaryParseStatusNote_structured_partial_invalid_unknown() {
        assertThat(PositionMonitorServiceImpl.buildBoundaryParseStatusNote("{\"boundaryParseStatus\":\"STRUCTURED\"}"))
                .isEqualTo("计划价位边界已结构化，但本阶段仍未启用价格比较。");
        assertThat(PositionMonitorServiceImpl.buildBoundaryParseStatusNote("{\"boundaryParseStatus\":\"PARTIAL\"}"))
                .isEqualTo("计划价位边界部分结构化，本阶段仅记录状态，尚未启用价格比较。");
        assertThat(PositionMonitorServiceImpl.buildBoundaryParseStatusNote("{\"boundaryParseStatus\":\"INVALID\"}"))
                .isEqualTo("计划价位边界结构无效，当前仅按文本参考。");
        assertThat(PositionMonitorServiceImpl.buildBoundaryParseStatusNote("{\"boundaryParseStatus\":\"OTHER\"}"))
                .isEqualTo("计划价位边界结构无效，当前仅按文本参考。");
    }

    @Test
    void merge_supplemental_and_boundary_appends_with_space() {
        String merged = PositionMonitorServiceImpl.mergeSupplementalAndBoundaryNotes(
                "存在轻度困惑，建议人工复核。",
                "计划价位边界当前仅为文本参考，尚未参与数值监护。");
        assertThat(merged).isEqualTo("存在轻度困惑，建议人工复核。 计划价位边界当前仅为文本参考，尚未参与数值监护。");
    }

    @Test
    void buildBoundaryDisplayInfo_unstructured_maps_text_reference_label() {
        PlanBoundaryDisplayInfo b = PlanBoundaryDisplayHelper.parse(
                "{\"boundaryParseStatus\":\"UNSTRUCTURED_TEXT_ONLY\",\"boundarySource\":\"AI_PLAN\"}",
                PlanBoundaryDisplayContext.POSITION_MONITOR);
        assertThat(b.parseStatus()).isEqualTo("UNSTRUCTURED_TEXT_ONLY");
        assertThat(b.stateLabel()).isEqualTo("文本参考");
        assertThat(b.warningText()).contains("未启用价格比较");
        assertThat(b.invalidPriceDirection()).isNull();
    }

    @Test
    void buildBoundaryDisplayInfo_partial_above_sets_invalid_price_fields() {
        PlanBoundaryDisplayInfo b = PlanBoundaryDisplayHelper.parse(
                "{\"boundaryParseStatus\":\"PARTIAL\",\"invalidPriceDirection\":\"ABOVE\",\"invalidPriceThreshold\":78500.44}",
                PlanBoundaryDisplayContext.POSITION_MONITOR);
        assertThat(b.stateLabel()).isEqualTo("部分结构化");
        assertThat(b.invalidPriceDirection()).isEqualTo("ABOVE");
        assertThat(b.invalidPriceThreshold()).isEqualByComparingTo(new BigDecimal("78500.44"));
    }

    @Test
    void buildBoundaryDisplayInfo_partial_below_sets_invalid_price_fields() {
        PlanBoundaryDisplayInfo b = PlanBoundaryDisplayHelper.parse(
                "{\"boundaryParseStatus\":\"PARTIAL\",\"invalidPriceDirection\":\"BELOW\",\"invalidPriceThreshold\":\"100.5\"}",
                PlanBoundaryDisplayContext.POSITION_MONITOR);
        assertThat(b.invalidPriceDirection()).isEqualTo("BELOW");
        assertThat(b.invalidPriceThreshold()).isEqualByComparingTo(new BigDecimal("100.5"));
    }

    @Test
    void buildBoundaryDisplayInfo_null_json_is_missing() {
        PlanBoundaryDisplayInfo b = PlanBoundaryDisplayHelper.parse(null, PlanBoundaryDisplayContext.POSITION_MONITOR);
        assertThat(b.parseStatus()).isEqualTo("MISSING");
        assertThat(b.stateLabel()).isEqualTo("未返回");
    }

    @Test
    void buildBoundaryDisplayInfo_malformed_json_fail_open() {
        PlanBoundaryDisplayInfo b = PlanBoundaryDisplayHelper.parse("{not json", PlanBoundaryDisplayContext.POSITION_MONITOR);
        assertThat(b.parseStatus()).isEqualTo("INVALID");
        assertThat(b.stateLabel()).isEqualTo("结构无效");
    }

    @Test
    void evaluate_returns_boundary_labels_without_changing_action_fields() {
        RealPositionVO manual = openPosition("pos-bd-vo", "BTCUSDT", "MANUAL_INPUT", "USER_MANUAL", "LONG");
        DecisionResultVO decision = decision("ana-bd", "BULLISH", "LOW", true, "STRONG", "LEVEL_1_CONSISTENT", 10, null);
        ExecutionPlanDO plan = plan("plan-bd");
        plan.setPlanBoundaryJson(
                "{\"boundaryParseStatus\":\"PARTIAL\",\"invalidPriceDirection\":\"BELOW\",\"invalidPriceThreshold\":\"43210.12\"}");

        when(realPositionMapper.selectOpenPositionById("pos-bd-vo")).thenReturn(manual);
        when(decisionResultMapper.findLatestDecisionResultBaseBySymbol("BTCUSDT")).thenReturn(decision);
        when(executionPlanMapper.selectLatestByAnalysisIdTieBreak("ana-bd")).thenReturn(plan);
        when(analysisRunMapper.selectById("ana-bd")).thenReturn(null);
        when(positionMonitorRecordMapper.selectLatestByPositionId("pos-bd-vo")).thenReturn(null);
        stubSelectByPlanId(plan);

        PositionMonitorOpenRowVO row = service.evaluateForPosition("pos-bd-vo", true);

        assertThat(row.getLatestMonitorRecord().getBoundaryStateLabel()).isEqualTo("部分结构化");
        assertThat(row.getLatestMonitorRecord().getInvalidPriceDirection()).isEqualTo("BELOW");
        assertThat(row.getLatestMonitorRecord().getInvalidPriceThreshold()).isEqualByComparingTo(new BigDecimal("43210.12"));
        assertThat(row.getLatestMonitorRecord().getSystemSuggestedAction()).isEqualTo("CONTINUE_HOLD");
        assertThat(row.getLatestMonitorRecord().getPositionRiskLevel()).isEqualTo("LOW");
    }

    private static RealPositionVO openPosition(String id, String symbol, String sourceType, String sourceName, String side) {
        RealPositionVO p = new RealPositionVO();
        p.setPositionId(id);
        p.setSymbol(symbol);
        p.setSourceType(sourceType);
        p.setSourceName(sourceName);
        p.setPositionStatus("OPEN");
        p.setPositionSide(side);
        p.setAvgOpenPrice(new BigDecimal("100"));
        p.setMarkPrice(new BigDecimal("100"));
        p.setUnrealizedPnlPct(BigDecimal.ZERO);
        p.setPositionQuantity(new BigDecimal("1"));
        p.setPositionOpenTime(LocalDateTime.now().minusHours(1));
        return p;
    }

    private static DecisionResultVO decision(String analysisId,
                                             String marketBias,
                                             String riskLevel,
                                             Boolean worthOpen,
                                             String multiTf,
                                             String aiLevel,
                                             Integer aiScore,
                                             String invalidCondition) {
        DecisionResultVO d = new DecisionResultVO();
        d.setAnalysisId(analysisId);
        d.setMarketBiasHierarchy(marketBias);
        d.setRiskLevel(riskLevel);
        d.setIsWorthOpening(worthOpen);
        d.setMultiTfConvergence(multiTf);
        d.setAiConflictLevel(aiLevel);
        d.setAiConflictScore(aiScore);
        d.setInvalidCondition(invalidCondition);
        return d;
    }

    private static ExecutionPlanDO plan(String planId) {
        ExecutionPlanDO p = new ExecutionPlanDO();
        p.setPlanId(planId);
        p.setRecommendedAction("CONTINUE");
        return p;
    }

    private static PositionMonitorRecordDO previous(String positionId, String symbol, LocalDateTime monitorTime) {
        PositionMonitorRecordDO p = candidate(monitorTime);
        p.setPositionId(positionId);
        p.setSymbol(symbol);
        return p;
    }

    private static PositionMonitorRecordDO candidate(LocalDateTime monitorTime) {
        PositionMonitorRecordDO c = new PositionMonitorRecordDO();
        c.setEntryLogicState("VALID");
        c.setDirectionSupportState("SUPPORT_ORIGINAL");
        c.setReversalState("NONE");
        c.setPositionRiskLevel("LOW");
        c.setAiSupportState("SUPPORT");
        c.setSystemSuggestedAction("CONTINUE_HOLD");
        c.setReviewEntryStatus("NOT_ENTERED");
        c.setMonitorTime(monitorTime);
        return c;
    }

}
