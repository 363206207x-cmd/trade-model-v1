package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.RuleValidationResult;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.service.support.AccountRiskPlanPolicy;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.MarketBiasEnum;
import org.example.trademodel.enums.PlanModeEnum;
import org.example.trademodel.service.DecisionChainRuleValidator;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.support.DataQualityCircuitBreakerPolicy;
import org.example.trademodel.service.support.ExecutionFeasibilityContract;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
public class DecisionChainRuleValidatorImpl implements DecisionChainRuleValidator {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> FROZEN_TIMEFRAMES = Set.of("4h", "1h", "15m", "5m");
    private static final Set<String> FROZEN_EIGHT_SCORE_TYPES = Set.of(
            "趋势结构分", "资金推动分", "杠杆风险分", "流动性质量分",
            "情绪温度分", "事件冲击分", "宏观环境分", "综合可信度分");
    private FundamentalAiV41Properties properties = FundamentalAiV41Properties.contractFixture();

    @Autowired(required = false)
    void setFundamentalAiV41Properties(FundamentalAiV41Properties properties) {
        if (properties != null) this.properties = properties;
    }

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
        if (!DataQualityCircuitBreakerPolicy.isValid(input.dataQualityScore())
                || input.dataQualityScore() < properties.getAiGate().getCircuitBreakerScore()) {
            reasons.add("DATA_QUALITY_BLOCKED");
        }
        if (!hasText(input.timeframe()) || !FROZEN_TIMEFRAMES.contains(input.timeframe().trim())) {
            reasons.add("ANALYSIS_TIMEFRAME_UNSUPPORTED");
        }
        Integer confused = input.decision().getConfusedScore();
        if (confused != null
                && confused >= properties.getOpportunityState().getConfusedEnterThreshold()) {
            reasons.add("CONFUSED_BLOCKED");
        }
        if (!permittedBiasAdjustment(candidate.getRuleDirection(), candidate.getCandidateDirection())) {
            reasons.add("RULE_DIRECTION_FAMILY_MISMATCH");
        }
        validateIdentityChain(reasons, input, opportunity, candidate, conflict);
        if (!knownMarketBias(conflict.getBiasBefore()) || !knownMarketBias(conflict.getBiasAfter())) {
            reasons.add("FINAL_MARKET_BIAS_UNKNOWN");
        } else {
            if (!same(conflict.getBiasBefore(), candidate.getRuleDirection())) {
                reasons.add("RESOLVER_BIAS_BEFORE_SOURCE_MISMATCH");
            }
            if (!same(conflict.getBiasAfter(), candidate.getCandidateDirection())) {
                reasons.add("RESOLVER_BIAS_AFTER_CANDIDATE_MISMATCH");
            }
            if (!permittedBiasAdjustment(candidate.getRuleDirection(), conflict.getBiasAfter())) {
                reasons.add("FINAL_MARKET_BIAS_FAMILY_MISMATCH");
            }
            if (!same(conflict.getBiasBefore(), conflict.getBiasAfter())
                    && !hasText(conflict.getAdjustmentReason())) {
                reasons.add("BIAS_ADJUSTMENT_REASON_MISSING");
            }
        }
        if (!"GPT_FINAL".equals(candidate.getCandidateSource())) {
            reasons.add("GPT_CANDIDATE_REQUIRED");
        }
        if (!same(candidate.getRuleDirection(), input.decision().getRuleMarketBias())) {
            reasons.add("CANDIDATE_RULE_DIRECTION_SOURCE_MISMATCH");
        }
        if (!same(candidate.getRuleConfidence(), input.decision().getRuleConfidence())) {
            reasons.add("CANDIDATE_RULE_CONFIDENCE_SOURCE_MISMATCH");
        }
        if (!same(candidate.getRuleRisk(), input.decision().getRuleRisk())) {
            reasons.add("CANDIDATE_RULE_RISK_SOURCE_MISMATCH");
        }
        if (!same(candidate.getRulePlanMode(), input.decision().getRulePlanMode())) {
            reasons.add("CANDIDATE_RULE_PLAN_MODE_SOURCE_MISMATCH");
        }
        if (!java.util.Objects.equals(candidate.getRuleCanExecute(), input.decision().getRuleCanExecute())) {
            reasons.add("CANDIDATE_RULE_EXECUTION_PERMISSION_SOURCE_MISMATCH");
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
        if (!knownPlanMode(conflict.getPlanModeAfter())) {
            reasons.add("FINAL_PLAN_MODE_UNKNOWN");
        }
        if (!knownPlanMode(candidate.getPlanMode())) {
            reasons.add("CANDIDATE_PLAN_MODE_UNKNOWN");
        }
        if (!knownValue(List.of("HIGH", "MEDIUM", "LOW"), conflict.getConfidenceAfter())) {
            reasons.add("FINAL_CONFIDENCE_UNKNOWN");
        }
        if (!knownValue(List.of("LOW", "MEDIUM", "HIGH", "EXTREME"), conflict.getRiskAfter())) {
            reasons.add("FINAL_RISK_UNKNOWN");
        }
        if (morePermissivePlanMode(conflict.getPlanModeAfter(), candidate.getRulePlanMode())) {
            reasons.add("FINAL_PLAN_MODE_MORE_PERMISSIVE_THAN_RULE");
        }
        if (moreConfident(conflict.getConfidenceAfter(), input.decision().getRuleConfidence())) {
            reasons.add("FINAL_CONFIDENCE_EXCEEDS_RULE");
        }
        if (lowerRisk(conflict.getRiskAfter(), input.decision().getRuleRisk())) {
            reasons.add("FINAL_RISK_BELOW_RULE");
        }
        if (knownMarketBias(conflict.getBiasAfter()) && knownPlanMode(conflict.getPlanModeAfter())
                && !validBiasPlanMode(conflict.getBiasAfter(), conflict.getPlanModeAfter())) {
            reasons.add("BIAS_PLAN_MODE_COMBINATION_INVALID");
        }
        if (knownPlanMode(conflict.getPlanModeAfter())
                && !validOpportunityPlanMode(opportunity.state(), conflict.getPlanModeAfter())) {
            reasons.add("OPPORTUNITY_STATE_PLAN_MODE_COMBINATION_INVALID");
        }
        validateWorthOpeningSemantics(reasons, candidate, conflict.getPlanModeAfter());

        ExecutionPlanVO rulePlan = input.rulePlan();
        if (!Boolean.TRUE.equals(rulePlan.getSourceGateComplete())
                || !ExecutionPlanVO.EXECUTION_PLAN_STATUS_VALID.equals(rulePlan.getSourceGateStatus())) {
            reasons.add("RULE_SOURCE_GATE_BLOCKED");
        }
        ExecutionFeasibilityContract.Assessment feasibility =
                ExecutionFeasibilityContract.assess(rulePlan, LocalDateTime.now(java.time.ZoneOffset.UTC));
        if (!feasibility.allowed()) reasons.add(feasibility.reasonCode());
        AccountRiskPlanPolicy.Assessment accountRisk = AccountRiskPlanPolicy.assess(
                input.accountRiskSnapshot(), candidate, conflict.getRiskAfter(),
                properties.getAccountRisk(), LocalDateTime.now(java.time.ZoneOffset.UTC));
        if (!accountRisk.allowed()) reasons.add(accountRisk.reasonCode());
        if (!hasText(candidate.getRecommendedAction())) reasons.add("RECOMMENDED_ACTION_MISSING");
        if (containsAutomaticTrading(candidate.getRecommendedAction())) reasons.add("AUTOMATIC_TRADING_ACTION_FORBIDDEN");
        requireText(reasons, candidate.getOpportunityType(), "OPPORTUNITY_TYPE_MISSING");
        requireText(reasons, candidate.getEntryLogic(), "ENTRY_LOGIC_MISSING");
        requireText(reasons, candidate.getEntryZone(), "ENTRY_ZONE_MISSING");
        requireText(reasons, candidate.getEntrySource(), "ENTRY_SOURCE_MISSING");
        requireText(reasons, candidate.getEntryReason(), "ENTRY_REASON_MISSING");
        requireText(reasons, candidate.getTriggerCondition(), "TRIGGER_CONDITION_MISSING");
        requireText(reasons, candidate.getStopLogic(), "STOP_LOGIC_MISSING");
        requireText(reasons, candidate.getStopLoss(), "STOP_ZONE_MISSING");
        requireText(reasons, candidate.getStopSource(), "STOP_SOURCE_MISSING");
        requireText(reasons, candidate.getStopReason(), "STOP_REASON_MISSING");
        requireText(reasons, candidate.getTargetLogic(), "TARGET_LOGIC_MISSING");
        requireText(reasons, candidate.getTakeProfitRules(), "TARGET_ZONES_MISSING");
        requireText(reasons, candidate.getTargetSource(), "TARGET_SOURCE_MISSING");
        requireText(reasons, candidate.getTargetReason(), "TARGET_REASON_MISSING");
        requireText(reasons, candidate.getAddPositionCondition(), "ADD_POSITION_CONDITION_MISSING");
        requireText(reasons, candidate.getReducePositionCondition(), "REDUCE_POSITION_CONDITION_MISSING");
        requireText(reasons, candidate.getAbandonCondition(), "ABANDON_CONDITION_MISSING");
        requireText(reasons, candidate.getLeverageSuggestion(), "LEVERAGE_SUGGESTION_MISSING");
        requireText(reasons, candidate.getPositionSuggestion(), "POSITION_SUGGESTION_MISSING");
        requireText(reasons, candidate.getRiskExplanation(), "RISK_EXPLANATION_MISSING");
        if (candidate.getExpectedRiskReward() == null
                || candidate.getExpectedRiskReward().signum() <= 0) {
            reasons.add("EXPECTED_RISK_REWARD_MISSING");
        }
        requireText(reasons, candidate.getExpectedRiskRewardSource(), "EXPECTED_RISK_REWARD_SOURCE_MISSING");
        requireText(reasons, candidate.getExpectedRiskRewardReason(), "EXPECTED_RISK_REWARD_REASON_MISSING");
        requireText(reasons, candidate.getAnalysisTimeframesJson(), "MULTI_TIMEFRAME_EXPLANATION_MISSING");
        requireText(reasons, candidate.getTriggerTimeframe(), "TRIGGER_TIMEFRAME_MISSING");
        if (hasText(candidate.getTriggerTimeframe())
                && !FROZEN_TIMEFRAMES.contains(candidate.getTriggerTimeframe().trim())) {
            reasons.add("TRIGGER_TIMEFRAME_UNSUPPORTED");
        }
        if (hasText(candidate.getAnalysisTimeframesJson())
                && !hasFrozenMultiTimeframeContract(candidate.getAnalysisTimeframesJson())) {
            reasons.add("MULTI_TIMEFRAME_CONTRACT_INCOMPLETE");
        }
        requireText(reasons, candidate.getHoldingHorizon(), "HOLDING_HORIZON_MISSING");
        requireText(reasons, candidate.getRevalidationRule(), "REVALIDATION_RULE_MISSING");
        requireText(reasons, candidate.getEvidenceRefsJson(), "EVIDENCE_REFS_MISSING");
        requireText(reasons, candidate.getScoreRefsJson(), "SCORE_REFS_MISSING");
        requireText(reasons, candidate.getSourceRefsJson(), "SOURCE_REFS_MISSING");
        validateFrozenInputLineage(reasons, input, candidate);
        validateCandidateSources(reasons, input, candidate);
        requireText(reasons, candidate.getInvalidCondition(), "INVALIDATION_MISSING");
        requireText(reasons, candidate.getInvalidationSource(), "INVALIDATION_SOURCE_MISSING");
        requireText(reasons, candidate.getInvalidationReason(), "INVALIDATION_REASON_MISSING");
        if (!hasText(candidate.getValidity())) reasons.add("VALIDITY_MISSING");
        if (candidate.getValidFrom() == null || candidate.getValidUntil() == null) {
            reasons.add("VALIDITY_WINDOW_MISSING");
        } else if (!candidate.getValidFrom().isBefore(candidate.getValidUntil())) {
            reasons.add("VALIDITY_WINDOW_INVALID");
        }
        return reasons.isEmpty() ? RuleValidationResult.pass() : RuleValidationResult.blocked(reasons);
    }

    private static void validateIdentityChain(List<String> reasons,
                                              DecisionChainBuildInput input,
                                              OpportunityTransitionResult opportunity,
                                              ExecutionPlanCandidateDO candidate,
                                              ConflictResolverResultDO conflict) {
        requireText(reasons, candidate.getCandidateId(), "CANDIDATE_ID_MISSING");
        requireText(reasons, candidate.getOpportunityId(), "CANDIDATE_OPPORTUNITY_ID_MISSING");
        requireText(reasons, candidate.getAnalysisId(), "CANDIDATE_ANALYSIS_ID_MISSING");
        requireText(reasons, candidate.getTraceId(), "CANDIDATE_TRACE_ID_MISSING");
        requireText(reasons, candidate.getRuleVersion(), "CANDIDATE_RULE_VERSION_MISSING");
        if (candidate.getAssetId() == null) reasons.add("CANDIDATE_ASSET_ID_MISSING");
        if (!same(candidate.getAnalysisId(), input.analysisId())) reasons.add("CANDIDATE_ANALYSIS_ID_MISMATCH");
        if (!same(candidate.getTraceId(), input.traceId())) reasons.add("CANDIDATE_TRACE_ID_MISMATCH");
        if (!same(candidate.getRuleVersion(), input.ruleVersion())) reasons.add("CANDIDATE_RULE_VERSION_MISMATCH");
        if (!Objects.equals(candidate.getAssetId(), input.assetId())) reasons.add("CANDIDATE_ASSET_ID_MISMATCH");
        if (!same(candidate.getOpportunityId(), opportunity.opportunityId())) {
            reasons.add("CANDIDATE_OPPORTUNITY_ID_MISMATCH");
        }
        requireText(reasons, conflict.getResolverResultId(), "RESOLVER_RESULT_ID_MISSING");
        if (!same(conflict.getCandidateId(), candidate.getCandidateId())) reasons.add("RESOLVER_CANDIDATE_ID_MISMATCH");
        if (!same(conflict.getAnalysisId(), input.analysisId())) reasons.add("RESOLVER_ANALYSIS_ID_MISMATCH");
        if (!same(conflict.getTraceId(), input.traceId())) reasons.add("RESOLVER_TRACE_ID_MISMATCH");
    }

    private static void validateWorthOpeningSemantics(List<String> reasons,
                                                      ExecutionPlanCandidateDO candidate,
                                                      String finalPlanMode) {
        if (candidate.getWorthOpening() == null) {
            reasons.add("WORTH_OPENING_MISSING");
            return;
        }
        try {
            boolean directional = switch (PlanModeEnum.require(finalPlanMode)) {
                case CONFIRMATION, PREPARATION, REDUCED -> true;
                case OBSERVATION, BLOCKED -> false;
            };
            if (candidate.getWorthOpening() != directional) {
                reasons.add("WORTH_OPENING_PLAN_MODE_MISMATCH");
            }
        } catch (RuntimeException ignored) {
            // FINAL_PLAN_MODE_UNKNOWN already captures the enum failure.
        }
    }

    private static void validateFrozenInputLineage(List<String> reasons,
                                                   DecisionChainBuildInput input,
                                                   ExecutionPlanCandidateDO candidate) {
        Set<String> scoreTypes = new HashSet<>();
        Set<String> scoreIds = new HashSet<>();
        if (input.scores() != null) {
            input.scores().stream().filter(Objects::nonNull).forEach(score -> {
                addText(scoreTypes, score.getScoreType());
                if (score.getScoreValue() != null) addText(scoreIds, score.getScoreId());
            });
        }
        if (!scoreTypes.equals(FROZEN_EIGHT_SCORE_TYPES) || scoreIds.size() != FROZEN_EIGHT_SCORE_TYPES.size()) {
            reasons.add("EIGHT_SCORE_CONTRACT_INCOMPLETE");
        }
        Set<String> evidenceIds = new HashSet<>();
        if (input.evidence() != null) {
            input.evidence().stream().filter(Objects::nonNull).forEach(evidence -> {
                if (hasText(evidence.getEvidenceId()) && hasText(evidence.getAnalysisId())
                        && same(evidence.getAnalysisId(), input.analysisId())
                        && hasText(evidence.getSource()) && hasText(evidence.getSourceReference())
                        && evidence.getObservedAt() != null && hasText(evidence.getFreshness())) {
                    evidenceIds.add(evidence.getEvidenceId().trim());
                }
            });
        }
        if (evidenceIds.isEmpty()) reasons.add("TRACEABLE_EVIDENCE_MISSING");
        if (!jsonArrayContainsAll(candidate.getEvidenceRefsJson(), evidenceIds)) {
            reasons.add("EVIDENCE_REFS_INCOMPLETE");
        }
        if (!jsonArrayContainsAll(candidate.getScoreRefsJson(), scoreIds)) {
            reasons.add("SCORE_REFS_INCOMPLETE");
        }
        if (!Objects.equals(candidate.getDataQuality(), input.dataQualityScore())) {
            reasons.add("CANDIDATE_DATA_QUALITY_MISMATCH");
        }
        Long expectedRiskId = input.accountRiskSnapshot() == null ? null : input.accountRiskSnapshot().getId();
        if (!Objects.equals(candidate.getAccountRiskSnapshotId(), expectedRiskId)) {
            reasons.add("CANDIDATE_ACCOUNT_RISK_SNAPSHOT_MISMATCH");
        }
    }

    private static boolean jsonArrayContainsAll(String raw, Set<String> expected) {
        if (!hasText(raw) || expected == null || expected.isEmpty()) return false;
        try {
            JsonNode root = JSON.readTree(raw);
            if (!root.isArray()) return false;
            Set<String> actual = new HashSet<>();
            root.forEach(value -> {
                if (value != null && value.isTextual() && !value.asText().isBlank()) {
                    actual.add(value.asText().trim());
                }
            });
            return actual.containsAll(expected);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void validateCandidateSources(List<String> reasons,
                                                 DecisionChainBuildInput input,
                                                 ExecutionPlanCandidateDO candidate) {
        Set<String> allowed = new HashSet<>();
        if (input.evidence() != null) {
            input.evidence().stream().filter(java.util.Objects::nonNull).forEach(evidence -> {
                addText(allowed, evidence.getEvidenceId());
                addText(allowed, evidence.getSource());
                addText(allowed, evidence.getSourceReference());
                addText(allowed, evidence.getSourceTraceId());
            });
        }
        if (!allowed.contains(candidate.getEntrySource())) reasons.add("ENTRY_SOURCE_NOT_TRACEABLE");
        if (!allowed.contains(candidate.getStopSource())) reasons.add("STOP_SOURCE_NOT_TRACEABLE");
        if (!allowed.contains(candidate.getTargetSource())) reasons.add("TARGET_SOURCE_NOT_TRACEABLE");
        if (!allowed.contains(candidate.getInvalidationSource())) {
            reasons.add("INVALIDATION_SOURCE_NOT_TRACEABLE");
        }
        if (!allowed.contains(candidate.getExpectedRiskRewardSource())) {
            reasons.add("EXPECTED_RISK_REWARD_SOURCE_NOT_TRACEABLE");
        }
    }

    private static void addText(Set<String> target, String value) {
        if (hasText(value)) target.add(value.trim());
    }

    private static String rulePlanMode(DecisionChainBuildInput input) {
        if (input.decision().isDirectionalPushBlocked()) return PlanModeEnum.BLOCKED.name();
        if (Boolean.TRUE.equals(input.decision().getIsWorthOpening())
                && Boolean.TRUE.equals(input.rulePlan().getSourceGateComplete())) return PlanModeEnum.CONFIRMATION.name();
        if (Boolean.TRUE.equals(input.decision().getIsWorthOpening())) return PlanModeEnum.PREPARATION.name();
        if ("HIGH".equalsIgnoreCase(input.decision().getRuleRisk())
                || "EXTREME".equalsIgnoreCase(input.decision().getRuleRisk())) return PlanModeEnum.REDUCED.name();
        return PlanModeEnum.OBSERVATION.name();
    }

    private static boolean morePermissivePlanMode(String actual, String rule) {
        try {
            return PlanModeEnum.require(actual).morePermissiveThan(PlanModeEnum.require(rule));
        } catch (RuntimeException ignored) {
            return true;
        }
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

    private static boolean knownPlanMode(String value) {
        try {
            PlanModeEnum.require(value);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean knownMarketBias(String value) {
        try {
            MarketBiasEnum.valueOf(normalize(value));
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean validBiasPlanMode(String biasValue, String modeValue) {
        MarketBiasEnum bias = MarketBiasEnum.valueOf(normalize(biasValue));
        PlanModeEnum mode = PlanModeEnum.require(modeValue);
        // Appendix A is guidance, not a mechanical matrix. Only its explicit
        // non-directional boundary is a hard validation rule.
        return (bias != MarketBiasEnum.RANGE && bias != MarketBiasEnum.WAIT)
                || mode == PlanModeEnum.OBSERVATION || mode == PlanModeEnum.BLOCKED;
    }

    private static boolean validOpportunityPlanMode(AssetStateEnum state, String modeValue) {
        if (state == null) return false;
        PlanModeEnum mode = PlanModeEnum.require(modeValue);
        return switch (state) {
            case OBSERVING -> mode == PlanModeEnum.OBSERVATION || mode == PlanModeEnum.BLOCKED;
            case CANDIDATE -> mode == PlanModeEnum.PREPARATION
                    || mode == PlanModeEnum.REDUCED
                    || mode == PlanModeEnum.OBSERVATION
                    || mode == PlanModeEnum.BLOCKED;
            case WAITING_TRIGGER -> mode == PlanModeEnum.PREPARATION || mode == PlanModeEnum.BLOCKED;
            case TRIGGERED -> true;
            case HIGH_RISK -> mode == PlanModeEnum.REDUCED
                    || mode == PlanModeEnum.OBSERVATION
                    || mode == PlanModeEnum.BLOCKED;
            case INVALIDATED, COOLING -> mode == PlanModeEnum.OBSERVATION || mode == PlanModeEnum.BLOCKED;
            case CONFUSED -> mode == PlanModeEnum.BLOCKED;
        };
    }

    private static boolean permittedBiasAdjustment(String ruleBias, String candidateBias) {
        try {
            MarketBiasEnum before = MarketBiasEnum.valueOf(normalize(ruleBias));
            MarketBiasEnum after = MarketBiasEnum.valueOf(normalize(candidateBias));
            return after.isSameFamilyDowngradeFrom(before);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private static boolean containsAutomaticTrading(String value) {
        if (value == null) return false;
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean hasFrozenMultiTimeframeContract(String raw) {
        try {
            JsonNode root = JSON.readTree(raw);
            if (root == null || !root.isObject()) return false;
            Set<String> fields = new HashSet<>();
            root.fieldNames().forEachRemaining(fields::add);
            if (!fields.equals(FROZEN_TIMEFRAMES)) return false;
            return FROZEN_TIMEFRAMES.stream().allMatch(timeframe -> {
                JsonNode value = root.get(timeframe);
                return value != null && !value.isNull()
                        && (!value.isTextual() || !value.asText().isBlank());
            });
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void requireText(List<String> reasons, String value, String reason) {
        if (!hasText(value) || "[]".equals(value.trim()) || "{}".equals(value.trim())) reasons.add(reason);
    }
}
