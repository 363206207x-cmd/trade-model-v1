package org.example.trademodel.service.impl;

import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.RuleValidationResult;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.service.ConfusedStatePolicy;
import org.example.trademodel.service.DecisionChainRuleValidator;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.support.DataQualityCircuitBreakerPolicy;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DecisionChainRuleValidatorImpl implements DecisionChainRuleValidator {
    @Override
    public RuleValidationResult validate(DecisionChainBuildInput input,
                                         OpportunityTransitionResult opportunity,
                                         ExecutionPlanCandidateDO candidate,
                                         ConflictResolverResultDO conflict) {
        List<String> reasons = new ArrayList<>();
        if (input == null || input.decision() == null || input.rulePlan() == null) {
            reasons.add("RULE_INPUT_MISSING");
            return RuleValidationResult.blocked(reasons);
        }
        if (opportunity == null) reasons.add("OPPORTUNITY_MISSING");
        if (candidate == null) reasons.add("CANDIDATE_MISSING");
        if (conflict == null) reasons.add("CONFLICT_RESULT_MISSING");
        if (!reasons.isEmpty()) return RuleValidationResult.blocked(reasons);

        if (!eligibleState(opportunity.state())) reasons.add("OPPORTUNITY_STATE_NOT_ELIGIBLE");
        if ("BLOCKED".equals(opportunity.executionPermission())) reasons.add("OPPORTUNITY_EXECUTION_PERMISSION_BLOCKED");
        if (!DataQualityCircuitBreakerPolicy.passes(input.dataQualityScore())) reasons.add("DATA_QUALITY_BLOCKED");
        Integer confused = input.decision().getConfusedScore();
        if (confused != null && confused >= ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD) {
            reasons.add("CONFUSED_BLOCKED");
        }
        if (!same(candidate.getRuleDirection(), candidate.getCandidateDirection())) {
            reasons.add("RULE_DIRECTION_MISMATCH");
        }
        if (!same(candidate.getRuleDirection(), input.decision().getMarketBiasHierarchy())) {
            reasons.add("CANDIDATE_RULE_DIRECTION_SOURCE_MISMATCH");
        }
        if (!same(candidate.getRuleConfidence(), input.decision().getConfidenceLevel())) {
            reasons.add("CANDIDATE_RULE_CONFIDENCE_SOURCE_MISMATCH");
        }
        if (!same(candidate.getRuleRisk(), input.decision().getRiskLevel())) {
            reasons.add("CANDIDATE_RULE_RISK_SOURCE_MISMATCH");
        }
        if (!Boolean.TRUE.equals(candidate.getNotFinalPlan())
                || !Boolean.TRUE.equals(candidate.getNotStateMachineMutation())
                || !Boolean.TRUE.equals(candidate.getNotUserPositionCreation())) {
            reasons.add("CANDIDATE_SAFETY_BOUNDARY_VIOLATION");
        }
        if (Boolean.TRUE.equals(candidate.getWorthOpening())
                && !Boolean.TRUE.equals(input.decision().getIsWorthOpening())) {
            reasons.add("CANDIDATE_CANNOT_ENABLE_RULE_REJECTED_OPENING");
        }
        if (hasText(conflict.getRuleVetoReason())) reasons.add(conflict.getRuleVetoReason());
        if ("BLOCKED".equals(normalize(conflict.getPlanModeAfter()))) reasons.add("PLAN_MODE_BLOCKED");
        if (!Boolean.TRUE.equals(conflict.getRuleDirectionPreserved())) {
            reasons.add("CONFLICT_RESOLVER_RULE_DIRECTION_NOT_PRESERVED");
        }
        if (!knownValue(List.of("CONFIRM", "PREPARE", "REDUCE", "WATCH", "BLOCKED"),
                conflict.getPlanModeAfter())) {
            reasons.add("FINAL_PLAN_MODE_UNKNOWN");
        }
        if (!knownValue(List.of("HIGH", "MEDIUM", "LOW"), conflict.getConfidenceAfter())) {
            reasons.add("FINAL_CONFIDENCE_UNKNOWN");
        }
        if (!knownValue(List.of("LOW", "MEDIUM", "HIGH", "EXTREME"), conflict.getRiskAfter())) {
            reasons.add("FINAL_RISK_UNKNOWN");
        }
        if (morePermissivePlanMode(conflict.getPlanModeAfter(), rulePlanMode(input))) {
            reasons.add("FINAL_PLAN_MODE_MORE_PERMISSIVE_THAN_RULE");
        }
        if (moreConfident(conflict.getConfidenceAfter(), input.decision().getConfidenceLevel())) {
            reasons.add("FINAL_CONFIDENCE_EXCEEDS_RULE");
        }
        if (lowerRisk(conflict.getRiskAfter(), input.decision().getRiskLevel())) {
            reasons.add("FINAL_RISK_BELOW_RULE");
        }

        ExecutionPlanVO rulePlan = input.rulePlan();
        if (!Boolean.TRUE.equals(rulePlan.getSourceGateComplete())
                || !ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID.equals(rulePlan.getSourceGateStatus())
                || !ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID.equals(rulePlan.getExecutionPlanStatus())) {
            reasons.add("RULE_PLAN_SOURCE_GATE_BLOCKED");
        }
        if (!sameText(rulePlan.getEntryZone(), candidate.getEntryZone())) reasons.add("ENTRY_ZONE_NOT_RULE_VALIDATED");
        if (!sameText(rulePlan.getStopLoss(), candidate.getStopLoss())) reasons.add("STOP_LOSS_NOT_RULE_VALIDATED");
        if (!sameText(rulePlan.getTakeProfitRules(), candidate.getTakeProfitRules())) reasons.add("TAKE_PROFIT_NOT_RULE_VALIDATED");
        if (!sameText(rulePlan.getInvalidCondition(), candidate.getInvalidCondition())) reasons.add("INVALIDATION_NOT_RULE_VALIDATED");
        if (!sameText(rulePlan.getLeverageSuggestion(), candidate.getLeverageSuggestion())) reasons.add("LEVERAGE_NOT_RULE_VALIDATED");
        if (!sameText(rulePlan.getPositionSuggestion(), candidate.getPositionSuggestion())) reasons.add("POSITION_SIZE_NOT_RULE_VALIDATED");
        if (!sameText(expectedValidity(input), candidate.getValidity())) reasons.add("VALIDITY_NOT_RULE_VALIDATED");
        if (!hasText(candidate.getRecommendedAction())) reasons.add("RECOMMENDED_ACTION_MISSING");
        if (containsAutomaticTrading(candidate.getRecommendedAction())) reasons.add("AUTOMATIC_TRADING_ACTION_FORBIDDEN");
        if (!hasText(candidate.getInvalidCondition())) reasons.add("INVALIDATION_MISSING");
        if (!hasText(candidate.getValidity())) reasons.add("VALIDITY_MISSING");
        return reasons.isEmpty() ? RuleValidationResult.pass() : RuleValidationResult.blocked(reasons);
    }

    private static String rulePlanMode(DecisionChainBuildInput input) {
        if (input.decision().isDirectionalPushBlocked()) return "BLOCKED";
        if (Boolean.TRUE.equals(input.decision().getIsWorthOpening())
                && Boolean.TRUE.equals(input.rulePlan().getSourceGateComplete())) return "CONFIRM";
        if (Boolean.TRUE.equals(input.decision().getIsWorthOpening())) return "PREPARE";
        if ("HIGH".equalsIgnoreCase(input.decision().getRiskLevel())
                || "EXTREME".equalsIgnoreCase(input.decision().getRiskLevel())) return "REDUCE";
        return "WATCH";
    }

    private static boolean morePermissivePlanMode(String actual, String rule) {
        return index(List.of("CONFIRM", "PREPARE", "REDUCE", "WATCH", "BLOCKED"), actual)
                < index(List.of("CONFIRM", "PREPARE", "REDUCE", "WATCH", "BLOCKED"), rule);
    }

    private static boolean moreConfident(String actual, String rule) {
        return index(List.of("HIGH", "MEDIUM", "LOW"), actual)
                < index(List.of("HIGH", "MEDIUM", "LOW"), rule);
    }

    private static boolean lowerRisk(String actual, String rule) {
        return index(List.of("LOW", "MEDIUM", "HIGH", "EXTREME"), actual)
                < index(List.of("LOW", "MEDIUM", "HIGH", "EXTREME"), rule);
    }

    private static int index(List<String> values, String value) {
        int found = values.indexOf(normalize(value));
        return found < 0 ? values.size() : found;
    }

    private static boolean knownValue(List<String> values, String value) {
        return values.contains(normalize(value));
    }

    private static String expectedValidity(DecisionChainBuildInput input) {
        return input.decision().getExpiresAt() == null
                ? "SOURCE_VALIDITY_UNAVAILABLE" : input.decision().getExpiresAt().toString();
    }

    private static boolean containsAutomaticTrading(String value) {
        String normalized = normalize(value).replace('-', '_').replace(' ', '_');
        return normalized.contains("AUTO_CLOSE") || normalized.contains("AUTO_REVERSE")
                || normalized.contains("AUTO_ORDER") || normalized.contains("AUTO_OPEN")
                || value.contains("自动平仓") || value.contains("自动反手")
                || value.contains("自动下单") || value.contains("自动开仓");
    }

    private static boolean eligibleState(AssetStateEnum state) {
        return state == AssetStateEnum.CANDIDATE
                || state == AssetStateEnum.WAITING_TRIGGER
                || state == AssetStateEnum.TRIGGERED
                || state == AssetStateEnum.HIGH_RISK;
    }

    private static boolean same(String left, String right) {
        return normalize(left).equals(normalize(right));
    }

    private static boolean sameText(String left, String right) {
        return compact(left).equals(compact(right));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String compact(String value) {
        return value == null ? "" : value.replaceAll("\\s+", "").trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
