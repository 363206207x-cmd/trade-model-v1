package org.example.trademodel.service;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PlanRevalidationRecordDO;
import org.example.trademodel.enums.PlanRevalidationTriggerTypeEnum;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.PlanRevalidationRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("core-regression")
class PlanRevalidationServiceTest {
    @Mock
    private PlanRevalidationRecordMapper recordMapper;
    @Mock
    private ExecutionPlanMapper planMapper;
    @Mock
    private AsyncTaskService asyncTaskService;

    private PlanRevalidationService service;

    @BeforeEach
    void setUp() {
        service = new PlanRevalidationService(recordMapper, planMapper, asyncTaskService);
    }

    @Test
    void userRequestCanOnlyCreateManualRevalidation() {
        ExecutionPlanDO plan = finalPlan();
        when(planMapper.selectByPlanId("plan-1")).thenReturn(plan);
        when(planMapper.markNeedsRevalidation(eq("plan-1"), eq("USER_REQUESTED"), any())).thenReturn(1);

        PlanRevalidationRecordDO record = service.request(
                41L, "plan-1", "MANUAL_REVALIDATION", "USER_REQUESTED");

        assertThat(record.getTriggerType()).isEqualTo("MANUAL_REVALIDATION");
        assertThat(record.getRequestedByUserId()).isEqualTo(41L);
        assertThat(record.getNotTradeInstruction()).isTrue();
        assertThat(record.getNotOrderExecution()).isTrue();
        verify(asyncTaskService).queueForUser(
                eq(41L), eq("PLAN_REVALIDATION"), eq("FINAL_PLAN"), eq("plan-1"), any());
    }

    @Test
    void userCannotImpersonateHotResetOrEventWindow() {
        assertThatThrownBy(() -> service.request(41L, "plan-1", "HOT_RESET", "USER_REQUESTED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MANUAL_REVALIDATION");
        assertThatThrownBy(() -> service.request(41L, "plan-1", "EVENT_WINDOW", "USER_REQUESTED"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MANUAL_REVALIDATION");
        verify(planMapper, never()).selectByPlanId(any());
    }

    @Test
    void hotResetAndEventWindowUseSystemOwnedRecordsAndTasks() {
        ExecutionPlanDO plan = finalPlan();
        LocalDateTime now = LocalDateTime.of(2026, 8, 14, 12, 0);

        PlanRevalidationRecordDO hotReset = service.recordSystemTrigger(
                plan, PlanRevalidationTriggerTypeEnum.HOT_RESET, "EXTREME_PRICE_MOVE", now);
        PlanRevalidationRecordDO eventWindow = service.recordSystemTrigger(
                plan, PlanRevalidationTriggerTypeEnum.EVENT_WINDOW, "MACRO_EVENT_WINDOW", now);

        assertThat(hotReset.getTriggerType()).isEqualTo("HOT_RESET");
        assertThat(eventWindow.getTriggerType()).isEqualTo("EVENT_WINDOW");
        assertThat(hotReset.getRequestedByUserId()).isNull();
        assertThat(eventWindow.getRequestedByUserId()).isNull();
        verify(asyncTaskService).queueForSystem(
                eq("HOT_RESET"), eq("FINAL_PLAN"), eq("plan-1"), any());
        verify(asyncTaskService).queueForSystem(
                eq("PLAN_REVALIDATION"), eq("FINAL_PLAN"), eq("plan-1"), any());
        ArgumentCaptor<PlanRevalidationRecordDO> captor =
                ArgumentCaptor.forClass(PlanRevalidationRecordDO.class);
        verify(recordMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(record -> {
            assertThat(record.getState()).isEqualTo("QUEUED");
            assertThat(record.getNotTradeInstruction()).isTrue();
            assertThat(record.getNotOrderExecution()).isTrue();
        });
    }

    @Test
    void repeatedSystemRequestReturnsExistingRecordWithoutDuplicateQueue() {
        ExecutionPlanDO plan = finalPlan();
        plan.setPlanLifecycleState("NEEDS_REVALIDATION");
        PlanRevalidationRecordDO existing = new PlanRevalidationRecordDO();
        existing.setRecordId("revalidation-existing");
        when(planMapper.selectByPlanId("plan-1")).thenReturn(plan);
        when(recordMapper.listByPlanId("plan-1", 1)).thenReturn(java.util.List.of(existing));

        PlanRevalidationRecordDO first = service.requestSystem(
                "plan-1", PlanRevalidationTriggerTypeEnum.DATA_REFRESH, "STALE_ANALYSIS");
        PlanRevalidationRecordDO second = service.requestSystem(
                "plan-1", PlanRevalidationTriggerTypeEnum.DATA_REFRESH, "STALE_ANALYSIS");

        assertThat(first).isSameAs(existing);
        assertThat(second).isSameAs(existing);
        verify(planMapper, never()).markNeedsRevalidation(any(), any(), any());
        verify(recordMapper, never()).insert(any());
        verify(asyncTaskService, never()).queueForSystem(any(), any(), any(), any());
    }

    @Test
    void losingAtomicSystemTransitionReturnsConcurrentCanonicalRecord() {
        ExecutionPlanDO initial = finalPlan();
        initial.setPlanLifecycleState("CURRENT");
        ExecutionPlanDO transitioned = finalPlan();
        transitioned.setPlanLifecycleState("NEEDS_REVALIDATION");
        PlanRevalidationRecordDO canonical = new PlanRevalidationRecordDO();
        canonical.setRecordId("revalidation-concurrent");
        when(planMapper.selectByPlanId("plan-1")).thenReturn(initial, transitioned);
        when(planMapper.markNeedsRevalidation(eq("plan-1"), eq("STALE_ANALYSIS"), any())).thenReturn(0);
        when(recordMapper.listByPlanId("plan-1", 1)).thenReturn(java.util.List.of(canonical));

        PlanRevalidationRecordDO actual = service.requestSystem(
                "plan-1", PlanRevalidationTriggerTypeEnum.DATA_REFRESH, "STALE_ANALYSIS");

        assertThat(actual).isSameAs(canonical);
        verify(planMapper, times(2)).selectByPlanId("plan-1");
        verify(recordMapper, never()).insert(any());
        verify(asyncTaskService, never()).queueForSystem(any(), any(), any(), any());
    }

    private static ExecutionPlanDO finalPlan() {
        ExecutionPlanDO plan = new ExecutionPlanDO();
        plan.setPlanId("plan-1");
        plan.setAnalysisId("analysis-1");
        plan.setTraceId("trace-1");
        plan.setFinalPlan(true);
        plan.setPlanVersion(3);
        return plan;
    }
}
