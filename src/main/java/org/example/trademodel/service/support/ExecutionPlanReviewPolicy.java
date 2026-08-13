package org.example.trademodel.service.support;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.enums.MarketBiasEnum;
import org.example.trademodel.enums.PlanModeEnum;
import org.example.trademodel.vo.ExecutionPlanVO;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Set;

/** Shared fail-closed interpretation of persisted execution-plan state and boundaries. */
public final class ExecutionPlanReviewPolicy {
    private static final Set<String> MISSING_BOUNDARY_VALUES = Set.of("暂无", "—", "待生成");

    private ExecutionPlanReviewPolicy() {
    }

    public static boolean isConcreteBoundary(String value) {
        String normalized = trimToNull(value);
        return normalized != null && !MISSING_BOUNDARY_VALUES.contains(normalized);
    }

    public static boolean hasCompleteBoundaries(ExecutionPlanDO plan) {
        return plan != null
                && isConcreteBoundary(plan.getEntryZone())
                && isConcreteBoundary(plan.getStopLoss())
                && isConcreteBoundary(plan.getTakeProfitRules());
    }

    public static boolean hasCompleteBoundaries(ExecutionPlanVO plan) {
        return plan != null
                && isConcreteBoundary(plan.getEntryZone())
                && isConcreteBoundary(plan.getStopLoss())
                && isConcreteBoundary(plan.getTakeProfitRules());
    }

    public static PersistedPlanState persistedPlanState(ExecutionPlanDO plan) {
        if (plan == null) {
            return PersistedPlanState.MISSING;
        }
        String executionStatus = upper(plan.getExecutionPlanStatus());
        String sourceGateStatus = upper(plan.getSourceGateStatus());
        if ("INVALID".equals(executionStatus) || "INVALID".equals(sourceGateStatus)) {
            return PersistedPlanState.INVALID;
        }
        if ("BLOCKED".equals(executionStatus) || "BLOCKED".equals(sourceGateStatus)) {
            return PersistedPlanState.BLOCKED;
        }
        if (Boolean.TRUE.equals(plan.getNeedsRevalidation())) {
            return PersistedPlanState.REVALIDATION_REQUIRED;
        }
        if (!hasCompleteFrozenFinalContract(plan)) {
            return PersistedPlanState.INCOMPLETE;
        }
        if ("INCOMPLETE".equals(executionStatus)
                || "INCOMPLETE".equals(sourceGateStatus)
                || !Boolean.TRUE.equals(plan.getSourceGateComplete())
                || !hasCompleteBoundaries(plan)) {
            return PersistedPlanState.INCOMPLETE;
        }
        if ("REVIEW_ONLY".equals(executionStatus) || "REVIEW_ONLY".equals(sourceGateStatus)) {
            return PersistedPlanState.REVIEW_ONLY;
        }
        if ("VALID".equals(executionStatus) && "VALID".equals(sourceGateStatus)) {
            return PersistedPlanState.ACTIVE;
        }
        return PersistedPlanState.INCOMPLETE;
    }

    public static boolean hasCompleteFrozenFinalContract(ExecutionPlanDO plan) {
        if (plan == null
                || !Boolean.TRUE.equals(plan.getFinalPlan())
                || !"PASS".equals(upper(plan.getRuleValidationStatus()))
                || !"FINAL_VALIDATED".equals(upper(plan.getChainStatus()))
                || !"VALID".equals(upper(plan.getSourceStatus()))
                || trimToNull(plan.getPlanId()) == null
                || trimToNull(plan.getCandidateId()) == null
                || trimToNull(plan.getOpportunityId()) == null
                || trimToNull(plan.getResolverResultId()) == null
                || trimToNull(plan.getValidationResultId()) == null
                || trimToNull(plan.getAnalysisId()) == null
                || plan.getAssetId() == null
                || trimToNull(plan.getTraceId()) == null
                || trimToNull(plan.getRuleVersion()) == null
                || !knownMarketBias(plan.getRuleMarketBias())
                || !knownMarketBias(plan.getFinalMarketBias())
                || !knownFinalPlanMode(plan.getCandidatePlanMode())
                || !knownFinalPlanMode(plan.getFinalPlanMode())
                || trimToNull(plan.getAdjustmentReason()) == null
                || trimToNull(plan.getOpportunityType()) == null
                || trimToNull(plan.getRecommendedAction()) == null
                || trimToNull(plan.getEntryLogic()) == null
                || !isConcreteBoundary(plan.getEntryZone())
                || trimToNull(plan.getEntrySource()) == null
                || trimToNull(plan.getEntryReason()) == null
                || trimToNull(plan.getTriggerCondition()) == null
                || trimToNull(plan.getStopLogic()) == null
                || !isConcreteBoundary(plan.getStopLoss())
                || trimToNull(plan.getStopSource()) == null
                || trimToNull(plan.getStopReason()) == null
                || trimToNull(plan.getTargetLogic()) == null
                || !isConcreteBoundary(plan.getTakeProfitRules())
                || trimToNull(plan.getTargetSource()) == null
                || trimToNull(plan.getTargetReason()) == null
                || trimToNull(plan.getAddPositionCondition()) == null
                || trimToNull(plan.getReducePositionCondition()) == null
                || trimToNull(plan.getAbandonCondition()) == null
                || trimToNull(plan.getInvalidCondition()) == null
                || trimToNull(plan.getInvalidationSource()) == null
                || trimToNull(plan.getInvalidationReason()) == null
                || trimToNull(plan.getRiskExplanation()) == null
                || trimToNull(plan.getLeverageLimit()) == null
                || trimToNull(plan.getPositionLimit()) == null
                || plan.getRiskLimit() == null || plan.getRiskLimit().signum() <= 0
                || plan.getExpectedRiskReward() == null || plan.getExpectedRiskReward().signum() <= 0
                || trimToNull(plan.getExpectedRiskRewardSource()) == null
                || trimToNull(plan.getExpectedRiskRewardReason()) == null
                || plan.getAccountRiskSnapshotId() == null
                || trimToNull(plan.getAnalysisTimeframesJson()) == null
                || trimToNull(plan.getTriggerTimeframe()) == null
                || plan.getValidFrom() == null || plan.getValidUntil() == null
                || !plan.getValidFrom().isBefore(plan.getValidUntil())
                || trimToNull(plan.getHoldingHorizon()) == null
                || trimToNull(plan.getRevalidationRule()) == null
                || plan.getDataQuality() == null || plan.getDataQuality() < 0 || plan.getDataQuality() > 100
                || trimToNull(plan.getSourceRefsJson()) == null
                || trimToNull(plan.getEvidenceRefsJson()) == null
                || trimToNull(plan.getScoreRefsJson()) == null
                || plan.getFinalizedAt() == null
                || !Boolean.TRUE.equals(plan.getManualReviewRequired())
                || !Boolean.TRUE.equals(plan.getNotTradeInstruction())
                || !Boolean.TRUE.equals(plan.getNotExecutable())
                || !Boolean.TRUE.equals(plan.getNotAutoTrading())
                || !Boolean.TRUE.equals(plan.getNotOrderExecution())
                || !Boolean.TRUE.equals(plan.getNotUserPositionCreation())) {
            return false;
        }
        return true;
    }

    /**
     * Interprets a final plan for a current Home/advisory projection. Historical
     * position monitoring uses {@link #persistedPlanState(ExecutionPlanDO)} so
     * expiry of current execution conditions cannot rewrite the source plan's
     * original validation outcome.
     */
    public static PersistedPlanState currentProjectionPlanState(ExecutionPlanDO plan) {
        return currentProjectionPlanState(plan, LocalDateTime.now(ZoneOffset.UTC));
    }

    public static PersistedPlanState currentProjectionPlanState(ExecutionPlanDO plan, LocalDateTime now) {
        PersistedPlanState persisted = persistedPlanState(plan);
        if (persisted != PersistedPlanState.ACTIVE) {
            return persisted;
        }
        ExecutionFeasibilityContract.Assessment feasibility =
                ExecutionFeasibilityContract.assess(plan, now);
        if (feasibility.allowed()) {
            return PersistedPlanState.ACTIVE;
        }
        if (ExecutionFeasibilityContract.INVALID.equals(feasibility.status())) {
            return PersistedPlanState.INVALID;
        }
        if (ExecutionFeasibilityContract.STALE.equals(feasibility.status())) {
            return PersistedPlanState.REVALIDATION_REQUIRED;
        }
        return PersistedPlanState.INCOMPLETE;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String upper(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed.toUpperCase(Locale.ROOT);
    }

    private static boolean knownMarketBias(String value) {
        try {
            MarketBiasEnum.valueOf(upper(value));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean knownFinalPlanMode(String value) {
        try {
            return PlanModeEnum.require(value) != PlanModeEnum.BLOCKED;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    public enum PersistedPlanState {
        MISSING,
        INVALID,
        BLOCKED,
        REVALIDATION_REQUIRED,
        INCOMPLETE,
        REVIEW_ONLY,
        ACTIVE
    }
}
