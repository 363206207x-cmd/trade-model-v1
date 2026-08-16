package org.example.trademodel.service;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.entity.PlanRevalidationRecordDO;
import org.example.trademodel.enums.PlanRevalidationTriggerTypeEnum;
import org.example.trademodel.mapper.ExecutionPlanMapper;
import org.example.trademodel.mapper.AnalysisRunMapper;
import org.example.trademodel.mapper.PlanRevalidationRecordMapper;
import org.example.trademodel.entity.AnalysisRunDO;
import org.example.trademodel.telegram.HighValueAlertMessageService;
import org.example.trademodel.telegram.HighValueAlertPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PlanRevalidationService {
    private final PlanRevalidationRecordMapper recordMapper;
    private final ExecutionPlanMapper planMapper;
    private final AsyncTaskService asyncTaskService;
    private final Clock clock = Clock.systemUTC();
    private HighValueAlertMessageService highValueAlertMessageService;
    private AnalysisRunMapper analysisRunMapper;

    public PlanRevalidationService(PlanRevalidationRecordMapper recordMapper,
                                   ExecutionPlanMapper planMapper,
                                   AsyncTaskService asyncTaskService) {
        this.recordMapper = recordMapper;
        this.planMapper = planMapper;
        this.asyncTaskService = asyncTaskService;
    }

    @Autowired(required = false)
    void setHighValueAlertDependencies(HighValueAlertMessageService value,
                                       AnalysisRunMapper analysisRunMapper) {
        this.highValueAlertMessageService = value;
        this.analysisRunMapper = analysisRunMapper;
    }

    @Transactional
    public PlanRevalidationRecordDO request(Long userId, String rawPlanId,
                                            String rawTriggerType, String reason) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        String planId = required(rawPlanId, "planId");
        PlanRevalidationTriggerTypeEnum triggerType = PlanRevalidationTriggerTypeEnum.valueOf(
                required(rawTriggerType, "triggerType").toUpperCase(Locale.ROOT));
        if (triggerType != PlanRevalidationTriggerTypeEnum.MANUAL_REVALIDATION) {
            throw new IllegalArgumentException("user requests must use MANUAL_REVALIDATION");
        }
        ExecutionPlanDO plan = planMapper.selectByPlanId(planId);
        if (plan == null || !Boolean.TRUE.equals(plan.getFinalPlan())) {
            throw new IllegalArgumentException("validated final plan is required");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String normalizedReason = required(reason, "reason");
        int updated = planMapper.markNeedsRevalidation(planId, normalizedReason, now);
        if (updated != 1) {
            throw new IllegalStateException("plan is not eligible for revalidation");
        }
        PlanRevalidationRecordDO record = newRecord(plan, triggerType, normalizedReason, userId, now);
        recordMapper.insert(record);
        asyncTaskService.queueForUser(userId, "PLAN_REVALIDATION", "FINAL_PLAN", planId, record.getTraceId());
        if (highValueAlertMessageService != null) {
            AnalysisRunDO analysis = analysisRunMapper == null ? null : analysisRunMapper.selectById(plan.getAnalysisId());
            if (analysis != null && hasText(analysis.getSymbol())) {
                highValueAlertMessageService.recordSafetyChange(
                        new HighValueAlertMessageService.SafetyChangeInput(
                                userId, HighValueAlertPolicy.SafetyChangeType.NEEDS_REVALIDATION,
                                "PLAN_REVALIDATION", record.getRecordId(), plan.getAnalysisId(), planId,
                                plan.getOpportunityId(), null, analysis.getSymbol(), record.getTraceId(),
                                "NEEDS_REVALIDATION", 2,
                                normalizedReason, "完成重新验证并再次通过规则与可信来源门禁",
                                now, plan.getValidUntil()));
            }
        }
        return record;
    }

    @Transactional
    public PlanRevalidationRecordDO requestSystem(String rawPlanId,
                                                   PlanRevalidationTriggerTypeEnum triggerType,
                                                   String reason) {
        if (triggerType == null || triggerType == PlanRevalidationTriggerTypeEnum.MANUAL_REVALIDATION) {
            throw new IllegalArgumentException("system triggerType is required");
        }
        String planId = required(rawPlanId, "planId");
        ExecutionPlanDO plan = planMapper.selectByPlanId(planId);
        if (plan == null || !Boolean.TRUE.equals(plan.getFinalPlan())) {
            throw new IllegalArgumentException("validated final plan is required");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String normalizedReason = required(reason, "reason");
        int updated = planMapper.markNeedsRevalidation(planId, normalizedReason, now);
        if (updated != 1) {
            throw new IllegalStateException("plan is not eligible for revalidation");
        }
        return recordSystemTrigger(plan, triggerType, normalizedReason, now);
    }

    @Transactional
    public PlanRevalidationRecordDO recordSystemTrigger(ExecutionPlanDO plan,
                                                         PlanRevalidationTriggerTypeEnum triggerType,
                                                         String reason,
                                                         LocalDateTime requestedAt) {
        if (plan == null || !Boolean.TRUE.equals(plan.getFinalPlan())) {
            throw new IllegalArgumentException("validated final plan is required");
        }
        if (triggerType == null || triggerType == PlanRevalidationTriggerTypeEnum.MANUAL_REVALIDATION) {
            throw new IllegalArgumentException("system triggerType is required");
        }
        LocalDateTime now = requestedAt == null ? LocalDateTime.now(clock) : requestedAt;
        PlanRevalidationRecordDO record = newRecord(
                plan, triggerType, required(reason, "reason"), null, now);
        recordMapper.insert(record);
        asyncTaskService.queueForSystem(
                triggerType == PlanRevalidationTriggerTypeEnum.HOT_RESET ? "HOT_RESET" : "PLAN_REVALIDATION",
                "FINAL_PLAN", plan.getPlanId(), record.getTraceId());
        return record;
    }

    private PlanRevalidationRecordDO newRecord(ExecutionPlanDO plan,
                                                PlanRevalidationTriggerTypeEnum triggerType,
                                                String reason,
                                                Long requestedByUserId,
                                                LocalDateTime now) {
        String planId = required(plan.getPlanId(), "planId");
        PlanRevalidationRecordDO record = new PlanRevalidationRecordDO();
        record.setRecordId("revalidation-" + UUID.randomUUID());
        record.setPlanId(planId);
        record.setAnalysisId(required(plan.getAnalysisId(), "analysisId"));
        record.setTriggerType(triggerType.name());
        record.setState("QUEUED");
        record.setSourcePlanVersion(plan.getPlanVersion() == null ? 1 : plan.getPlanVersion());
        record.setReason(reason);
        record.setTraceId(hasText(plan.getTraceId()) ? plan.getTraceId() : "trace-" + UUID.randomUUID());
        record.setRequestedByUserId(requestedByUserId);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);
        record.setNotTradeInstruction(true);
        record.setNotOrderExecution(true);
        return record;
    }

    public List<PlanRevalidationRecordDO> list(String planId, int limit) {
        return recordMapper.listByPlanId(required(planId, "planId"), Math.max(1, Math.min(limit, 50)));
    }

    private static String required(String value, String field) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
