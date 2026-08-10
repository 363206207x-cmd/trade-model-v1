package org.example.trademodel.ai;

import com.fasterxml.jackson.annotation.JsonInclude;

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

    public AiRoleResultsPayload {
        orchestrationReasonCodes = orchestrationReasonCodes == null ? List.of() : List.copyOf(orchestrationReasonCodes);
        roles = roles == null ? Map.of() : Map.copyOf(roles);
        synthesis = synthesis == null ? SynthesisPayload.empty() : synthesis;
        safety = safety == null ? SafetyBoundary.defaults() : safety;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record RolePayload(
            String role,
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

        public RolePayload {
            reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public record SynthesisPayload(
            String finalMarketBias,
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

        public static SynthesisPayload empty() {
            return new SynthesisPayload(null, null, null, null, null, null,
                    null, null, null, null, null);
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
