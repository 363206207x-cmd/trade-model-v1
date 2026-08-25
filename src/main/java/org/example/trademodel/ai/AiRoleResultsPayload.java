package org.example.trademodel.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record AiRoleResultsPayload(
        String schemaVersion,
        String analysisId,
        String traceId,
        String ruleVersion,
        String orchestrationMode,
        List<String> orchestrationReasonCodes,
        Map<String, RolePayload> roles,
        SynthesisPayload synthesis,
        SafetyBoundary safety) {

    public static final String AI_ROLE_RESULTS_SCHEMA_V1 = "v1";
    public static final String AI_ROLE_RESULTS_SCHEMA_V2 = "v2";
    public static final String CURRENT_SCHEMA_VERSION = AI_ROLE_RESULTS_SCHEMA_V2;

    public AiRoleResultsPayload {
        orchestrationReasonCodes = orchestrationReasonCodes == null ? List.of() : List.copyOf(orchestrationReasonCodes);
        roles = roles == null ? Map.of() : Map.copyOf(roles);
        synthesis = synthesis == null ? SynthesisPayload.empty() : synthesis;
        safety = safety == null ? SafetyBoundary.defaults() : safety;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RolePayload(
            String role,
            String provider,
            String sourceRole,
            String callStatus,
            String analysisId,
            String traceId,
            String roleState,
            String dataState,
            String generatedAt,
            Boolean resultAvailable,
            String stance,
            String conflictLevel,
            List<String> reasonCodes,
            String summary,
            Boolean fallback,
            String fallbackReason,
            Boolean manualReviewRequired,
            CoreJudgment coreJudgment,
            List<EvidencePayload> supportingEvidence,
            String supportingEvidenceState,
            List<EvidencePayload> opposingEvidence,
            String opposingEvidenceState,
            MultiTimeframeExplanation multiTimeframeExplanation,
            BiasAdjustment biasAdjustment,
            CandidateSummary candidateSummary,
            List<FindingPayload> evidenceGaps,
            String evidenceGapsState,
            List<FindingPayload> logicConflicts,
            String logicConflictsState,
            List<FindingPayload> underestimatedRisks,
            String underestimatedRisksState,
            DowngradeSuggestion downgradeSuggestion,
            String reviewResult,
            String finalDirectionImpact,
            String confidenceAdjustment,
            String riskAdjustment,
            String planModeAdjustment,
            String recoveryCondition,
            List<FailurePathPayload> failurePaths,
            String failurePathState,
            List<FindingPayload> opposingScenarios,
            String opposingScenariosState,
            List<FindingPayload> externalEventRisks,
            String externalEventRisksState,
            List<FindingPayload> microstructureRisks,
            String microstructureRisksState,
            List<FindingPayload> watchIndicators,
            String watchIndicatorsState,
            String challengeSummary,
            String currentDirectionChallenge,
            Boolean majorCounterEvidence,
            String planModeImpact) {

        public RolePayload {
            reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
            supportingEvidence = copy(supportingEvidence);
            opposingEvidence = copy(opposingEvidence);
            evidenceGaps = copy(evidenceGaps);
            logicConflicts = copy(logicConflicts);
            underestimatedRisks = copy(underestimatedRisks);
            failurePaths = copy(failurePaths);
            opposingScenarios = copy(opposingScenarios);
            externalEventRisks = copy(externalEventRisks);
            microstructureRisks = copy(microstructureRisks);
            watchIndicators = copy(watchIndicators);
        }

        /** Compatibility constructor for legacy, non-v4.1 review payloads. */
        public RolePayload(String role,
                           String provider,
                           String sourceRole,
                           String callStatus,
                           String stance,
                           String conflictLevel,
                           List<String> reasonCodes,
                           String summary,
                           Boolean fallback,
                           String fallbackReason,
                           Boolean manualReviewRequired) {
            this(role, provider, sourceRole, callStatus,
                    null, null, null, null, null, null,
                    stance, conflictLevel, reasonCodes, summary, fallback, fallbackReason,
                    manualReviewRequired,
                    null, List.of(), null, List.of(), null, null, null, null,
                    List.of(), null, List.of(), null, List.of(), null,
                    null, null, null, null, null, null, null,
                    List.of(), null, List.of(), null, List.of(), null,
                    List.of(), null, List.of(), null, null, null, null, null);
        }

        private static <T> List<T> copy(List<T> values) {
            return values == null ? List.of() : List.copyOf(values);
        }
    }

    public record CoreJudgment(String marketBias, String opportunityState, String text) {
    }

    public record EvidencePayload(
            String evidenceId,
            String type,
            String source,
            String currentValue,
            String change,
            String direction,
            Double strength,
            Double confidence,
            String observedAt,
            String freshness,
            String analysisId) {
    }

    public record MultiTimeframeExplanation(
            @com.fasterxml.jackson.annotation.JsonProperty("4h") String fourHour,
            @com.fasterxml.jackson.annotation.JsonProperty("1h") String oneHour,
            @com.fasterxml.jackson.annotation.JsonProperty("15m") String fifteenMinute,
            @com.fasterxml.jackson.annotation.JsonProperty("5m") String fiveMinute) {
    }

    public record BiasAdjustment(String before, String after, String reason) {
    }

    public record CandidateSummary(
            String planMode,
            String confidence,
            String riskLevel,
            Boolean worthOpening,
            String opportunityType,
            String recommendedAction,
            String entryLogic,
            String entryZone,
            String entrySource,
            String entryReason,
            String triggerCondition,
            String stopLogic,
            String stopZone,
            String stopSource,
            String stopReason,
            String targetLogic,
            String targetZones,
            String targetSource,
            String targetReason,
            String addPositionCondition,
            String reducePositionCondition,
            String abandonCondition,
            String leverageSuggestion,
            String positionSuggestion,
            String riskExplanation,
            String invalidCondition,
            String invalidationSource,
            String invalidationReason,
            BigDecimal expectedRiskReward,
            String expectedRiskRewardSource,
            String expectedRiskRewardReason,
            String validity,
            String triggerTimeframe,
            String holdingHorizon,
            String revalidationRule,
            String summary) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FindingPayload(
            String findingId,
            String category,
            String text,
            String impact,
            List<String> evidenceRefs,
            String triggerCondition,
            String observationWindow,
            List<String> validationIndicators,
            String source,
            String observedAt,
            String eventWindow,
            String phenomenon,
            String timeframe,
            String metric,
            String currentState) {
        public FindingPayload {
            evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
            validationIndicators = validationIndicators == null
                    ? List.of() : List.copyOf(validationIndicators);
        }
    }

    public record DowngradeSuggestion(
            String before,
            String after,
            String reason,
            String recoveryCondition) {
    }

    public record FailurePathPayload(
            String failurePathId,
            String hypothesis,
            String triggerCondition,
            String causalPath,
            String observationWindow,
            List<String> validationIndicators,
            List<String> sourceRefs,
            String invalidatingEvidence) {
        public FailurePathPayload {
            validationIndicators = validationIndicators == null ? List.of() : List.copyOf(validationIndicators);
            sourceRefs = sourceRefs == null ? List.of() : List.copyOf(sourceRefs);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record SynthesisPayload(
            String finalMarketBias,
            String finalConfidence,
            String finalRiskLevel,
            String finalPlanMode,
            Boolean worthOpening,
            String conflictLevel,
            Integer conflictScore,
            String confidenceAdjustment,
            String riskAdjustment,
            String planModeAdjustment,
            Boolean confused,
            String downgradeReason,
            String mainReason,
            String recoveryCondition) {

        public SynthesisPayload(String finalMarketBias,
                                String finalConfidence,
                                String finalRiskLevel,
                                Boolean worthOpening,
                                String conflictLevel,
                                Integer conflictScore,
                                String confidenceAdjustment,
                                String riskAdjustment,
                                String planModeAdjustment,
                                Boolean confused,
                                String downgradeReason) {
            this(finalMarketBias, finalConfidence, finalRiskLevel, planModeAdjustment,
                    worthOpening, conflictLevel, conflictScore, confidenceAdjustment,
                    riskAdjustment, planModeAdjustment, confused, downgradeReason,
                    downgradeReason, null);
        }

        public static SynthesisPayload empty() {
            return new SynthesisPayload(null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null);
        }
    }

    public record SafetyBoundary(
            boolean reviewOnly,
            boolean manualReviewOnly,
            boolean notTradeInstruction,
            boolean notExecutable,
            boolean notAutoTrading,
            boolean notOrderExecution,
            boolean notUserPositionCreation,
            boolean notPositionMutation,
            boolean notStateMachineOverride,
            boolean notFinalExecutionPlanCreation,
            boolean ruleDirectionPreserved) {

        public static SafetyBoundary defaults() {
            return new SafetyBoundary(true, true, true, true, true,
                    true, true, true, true, true, true);
        }

        public static SafetyBoundary decisionChainV41() {
            return new SafetyBoundary(false, true, true, true, true,
                    true, true, true, true, true, true);
        }
    }
}
