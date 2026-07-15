package org.example.trademodel.service.support;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.vo.ExecutionPlanVO;

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
