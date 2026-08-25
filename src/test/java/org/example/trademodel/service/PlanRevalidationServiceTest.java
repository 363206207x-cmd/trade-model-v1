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
        when(planMapper.selectByPlanIdForUser("plan-1", 41L)).thenReturn(plan);
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
        verify(planMapper, never()).selectByPlanIdForUser(any(), any());
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
