package org.example.trademodel.service.support;

import org.example.trademodel.entity.ExecutionPlanDO;
import org.example.trademodel.vo.ExecutionPlanVO;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

/**
 * Frozen v4.1 execution-feasibility boundary. Account risk is evaluated by a
 * separate policy and must never be used as a substitute for these market and
 * trigger checks.
 */
public final class ExecutionFeasibilityContract {
    public static final String VERIFIED = "VERIFIED";
    public static final String PENDING = "PENDING";
    public static final String UNAVAILABLE = "UNAVAILABLE";
    public static final String INVALID = "INVALID";
    public static final String STALE = "STALE";
    public static final String DEFAULT_REASON = "EXECUTION_FEASIBILITY_SOURCE_UNAVAILABLE";

    private static final List<String> ALLOWED_STATES = List.of(
            VERIFIED, PENDING, UNAVAILABLE, INVALID, STALE);

    private ExecutionFeasibilityContract() {
    }

    public static void initializeUnavailable(ExecutionPlanVO plan, String reason) {
        if (plan == null) return;
        plan.setExecutionFeasibilityStatus(UNAVAILABLE);
        plan.setSlippageStatus(UNAVAILABLE);
        plan.setDepthStatus(UNAVAILABLE);
        plan.setEntryDriftStatus(UNAVAILABLE);
        plan.setTriggerStatus(UNAVAILABLE);
        plan.setExecutionFeasibilityReason(hasText(reason) ? reason.trim() : DEFAULT_REASON);
        plan.setExecutionFeasibilityObservedAt(null);
        plan.setExecutionFeasibilityFreshUntil(null);
        plan.setExecutionFeasibilitySourceRefsJson(null);
        applyCompatibilityGuard(plan, false);
    }

    public static Assessment assess(ExecutionPlanVO plan, LocalDateTime now) {
        if (plan == null) return Assessment.blocked(UNAVAILABLE, "EXECUTION_PLAN_MISSING");
        return assess(plan.getExecutionFeasibilityStatus(), plan.getSlippageStatus(),
                plan.getDepthStatus(), plan.getEntryDriftStatus(), plan.getTriggerStatus(),
                plan.getExecutionFeasibilityReason(), plan.getExecutionFeasibilityObservedAt(),
                plan.getExecutionFeasibilityFreshUntil(), plan.getExecutionFeasibilitySourceRefsJson(), now);
    }

    public static Assessment assess(ExecutionPlanDO plan, LocalDateTime now) {
        if (plan == null) return Assessment.blocked(UNAVAILABLE, "EXECUTION_PLAN_MISSING");
        return assess(plan.getExecutionFeasibilityStatus(), plan.getSlippageStatus(),
                plan.getDepthStatus(), plan.getEntryDriftStatus(), plan.getTriggerStatus(),
                plan.getExecutionFeasibilityReason(), plan.getExecutionFeasibilityObservedAt(),
                plan.getExecutionFeasibilityFreshUntil(), plan.getExecutionFeasibilitySourceRefsJson(), now);
    }

    public static Assessment assess(ExecutionPlanVO plan) {
        return assess(plan, LocalDateTime.now(ZoneOffset.UTC));
    }

    /** Applies a verified upstream assessment without inventing any numeric market value. */
    public static void applyVerifiedAssessment(ExecutionPlanVO plan,
                                               LocalDateTime observedAt,
                                               LocalDateTime freshUntil,
                                               String sourceRefsJson) {
        if (plan == null) throw new IllegalArgumentException("execution plan is required");
        plan.setExecutionFeasibilityStatus(VERIFIED);
        plan.setSlippageStatus(VERIFIED);
        plan.setDepthStatus(VERIFIED);
        plan.setEntryDriftStatus(VERIFIED);
        plan.setTriggerStatus(VERIFIED);
        plan.setExecutionFeasibilityReason(null);
        plan.setExecutionFeasibilityObservedAt(observedAt);
        plan.setExecutionFeasibilityFreshUntil(freshUntil);
        plan.setExecutionFeasibilitySourceRefsJson(sourceRefsJson);
        Assessment assessment = assess(plan);
        if (!assessment.allowed()) {
            initializeUnavailable(plan, assessment.reasonCode());
            throw new IllegalArgumentException(assessment.reasonCode());
        }
        applyCompatibilityGuard(plan, true);
    }

    /**
     * Applies a source-owned assessment. Only a fully verified, fresh set of
     * component checks can make the compatibility guard ready.
     */
    public static Assessment applyAssessment(ExecutionPlanVO plan,
                                             String status,
                                             String slippageStatus,
                                             String depthStatus,
                                             String entryDriftStatus,
                                             String triggerStatus,
                                             String reason,
                                             LocalDateTime observedAt,
                                             LocalDateTime freshUntil,
                                             String sourceRefsJson,
                                             LocalDateTime now) {
        if (plan == null) throw new IllegalArgumentException("execution plan is required");
        plan.setExecutionFeasibilityStatus(normalizedOrUnavailable(status));
        plan.setSlippageStatus(normalizedOrUnavailable(slippageStatus));
        plan.setDepthStatus(normalizedOrUnavailable(depthStatus));
        plan.setEntryDriftStatus(normalizedOrUnavailable(entryDriftStatus));
        plan.setTriggerStatus(normalizedOrUnavailable(triggerStatus));
        plan.setExecutionFeasibilityReason(hasText(reason) ? reason.trim() : null);
        plan.setExecutionFeasibilityObservedAt(observedAt);
        plan.setExecutionFeasibilityFreshUntil(freshUntil);
        plan.setExecutionFeasibilitySourceRefsJson(sourceRefsJson);
        Assessment assessment = assess(plan.getExecutionFeasibilityStatus(), plan.getSlippageStatus(),
                plan.getDepthStatus(), plan.getEntryDriftStatus(), plan.getTriggerStatus(),
                plan.getExecutionFeasibilityReason(), observedAt, freshUntil, sourceRefsJson, now);
        applyCompatibilityGuard(plan, assessment.allowed(), now);
        return assessment;
    }

    private static Assessment assess(String status,
                                     String slippageStatus,
                                     String depthStatus,
                                     String entryDriftStatus,
                                     String triggerStatus,
                                     String reason,
                                     LocalDateTime observedAt,
                                     LocalDateTime freshUntil,
                                     String sourceRefsJson,
                                     LocalDateTime suppliedNow) {
        LocalDateTime now = suppliedNow == null ? LocalDateTime.now(ZoneOffset.UTC) : suppliedNow;
        String normalized = normalize(status);
        if (normalized.isEmpty()) {
            return Assessment.blocked(UNAVAILABLE, "EXECUTION_FEASIBILITY_STATUS_MISSING");
        }
        if (!ALLOWED_STATES.contains(normalized)) {
            return Assessment.blocked(INVALID, "EXECUTION_FEASIBILITY_STATUS_INVALID");
        }
        if (!VERIFIED.equals(normalized)) {
            return Assessment.blocked(normalized, hasText(reason)
                    ? reason.trim() : "EXECUTION_FEASIBILITY_" + normalized);
        }
        if (!VERIFIED.equals(normalize(slippageStatus))) {
            return Assessment.blocked(INVALID, "SLIPPAGE_ASSESSMENT_NOT_VERIFIED");
        }
        if (!VERIFIED.equals(normalize(depthStatus))) {
            return Assessment.blocked(INVALID, "DEPTH_ASSESSMENT_NOT_VERIFIED");
        }
        if (!VERIFIED.equals(normalize(entryDriftStatus))) {
            return Assessment.blocked(INVALID, "ENTRY_DRIFT_ASSESSMENT_NOT_VERIFIED");
        }
        if (!VERIFIED.equals(normalize(triggerStatus))) {
            return Assessment.blocked(INVALID, "TRIGGER_ASSESSMENT_NOT_VERIFIED");
        }
        if (observedAt == null || freshUntil == null || now.isBefore(observedAt)) {
            return Assessment.blocked(INVALID, "EXECUTION_FEASIBILITY_TIME_INVALID");
        }
        if (!now.isBefore(freshUntil)) {
            return Assessment.blocked(STALE, "EXECUTION_FEASIBILITY_STALE");
        }
        if (!hasText(sourceRefsJson) || "[]".equals(sourceRefsJson.trim()) || "{}".equals(sourceRefsJson.trim())) {
            return Assessment.blocked(INVALID, "EXECUTION_FEASIBILITY_SOURCE_REFS_MISSING");
        }
        return new Assessment(true, VERIFIED, null);
    }

    public static void applyCompatibilityGuard(ExecutionPlanVO plan, boolean ready) {
        applyCompatibilityGuard(plan, ready, LocalDateTime.now(ZoneOffset.UTC));
    }

    private static void applyCompatibilityGuard(ExecutionPlanVO plan,
                                                boolean ready,
                                                LocalDateTime now) {
        if (plan == null) return;
        Assessment assessment = assess(plan, now);
        boolean effectiveReady = ready && assessment.allowed();
        plan.setRiskActionGuardReady(effectiveReady);
        plan.setRiskActionGuardStatus(effectiveReady ? VERIFIED : assessment.status());
        plan.setRiskActionGuardBlockingReason(effectiveReady ? null : assessment.reasonCode());
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizedOrUnavailable(String value) {
        String normalized = normalize(value);
        return ALLOWED_STATES.contains(normalized) ? normalized : UNAVAILABLE;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record Assessment(boolean allowed, String status, String reasonCode) {
        private static Assessment blocked(String status, String reasonCode) {
            return new Assessment(false, status, reasonCode);
        }
    }
}
