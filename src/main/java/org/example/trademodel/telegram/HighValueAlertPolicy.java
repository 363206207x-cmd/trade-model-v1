package org.example.trademodel.telegram;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;

@Component
public class HighValueAlertPolicy {
    private static final Set<String> BLOCKED_OPPORTUNITY_STATES = Set.of(
            "HIGH_RISK", "INVALIDATED", "COOLING", "CONFUSED");
    private static final Set<String> POSITION_MATERIAL_CONCLUSIONS = Set.of(
            "NEAR_STOP_LOSS", "NEAR_TAKE_PROFIT", "HIGH_RISK_OBSERVATION", "PLAN_INVALIDATED");

    public boolean allowsOpportunity(OpportunityQualification value) {
        if (value == null || value.userId() == null || value.userId() <= 0) return false;
        String mode = normalized(value.finalPlanMode());
        boolean permittedMode = "CONFIRMATION".equals(mode)
                || ("REDUCED".equals(mode) && value.highQualityReducedAllowed());
        return value.assetInPool()
                && value.persistedOpportunity()
                && value.finalPlan()
                && value.ruleValidated()
                && permittedMode
                && !BLOCKED_OPPORTUNITY_STATES.contains(normalized(value.opportunityState()))
                && !value.expired()
                && value.dataQualityPassed()
                && value.fresh()
                && value.sourceGatePassed()
                && value.executionFeasibilityPassed()
                && value.traceable()
                && value.pushSnapshotPresent()
                && !value.preview()
                && !value.candidateOnly()
                && value.notTradeInstruction()
                && value.notOrderExecution();
    }

    public boolean allowsSafetyChange(SafetyQualification value) {
        return value != null && value.userId() != null && value.userId() > 0
                && value.changeType() != null && value.traceable()
                && value.notTradeInstruction() && value.notOrderExecution();
    }

    public boolean allowsPosition(PositionQualification value) {
        if (value == null || value.userId() == null || value.userId() <= 0
                || !value.activeManualPosition() || !value.verified() || !value.fresh()) return false;
        return Set.of("WEAKENED", "INVALIDATED").contains(normalized(value.entryLogicStatus()))
                || "STRONG_REVERSAL".equals(normalized(value.reversalStatus()))
                || Set.of("HIGH", "EXTREME").contains(normalized(value.riskLevel()))
                || Set.of("INCREASED", "SHARPLY_INCREASED").contains(normalized(value.riskTrend()))
                || POSITION_MATERIAL_CONCLUSIONS.contains(normalized(value.monitorConclusion()));
    }

    public enum SafetyChangeType {
        CONFUSED,
        HIGH_CONFUSED,
        LIQUIDITY_TRAP,
        HOT_RESET,
        FINAL_INVALIDATED,
        RISK_BLOCKED,
        EXECUTION_DRIFT,
        PLAN_EXPIRED,
        DATA_QUALITY_BLOCKED,
        SOURCE_INVALID,
        NEEDS_REVALIDATION
    }

    public record OpportunityQualification(
            Long userId,
            boolean assetInPool,
            boolean persistedOpportunity,
            boolean finalPlan,
            boolean ruleValidated,
            String finalPlanMode,
            boolean highQualityReducedAllowed,
            String opportunityState,
            boolean expired,
            boolean dataQualityPassed,
            boolean fresh,
            boolean sourceGatePassed,
            boolean executionFeasibilityPassed,
            boolean traceable,
            boolean pushSnapshotPresent,
            boolean preview,
            boolean candidateOnly,
            boolean notTradeInstruction,
            boolean notOrderExecution) {
    }

    public record SafetyQualification(Long userId, SafetyChangeType changeType, boolean traceable,
                                      boolean notTradeInstruction, boolean notOrderExecution) {
    }

    public record PositionQualification(Long userId, boolean activeManualPosition,
                                        boolean verified, boolean fresh,
                                        String entryLogicStatus, String reversalStatus,
                                        String riskLevel, String riskTrend,
                                        String monitorConclusion) {
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
