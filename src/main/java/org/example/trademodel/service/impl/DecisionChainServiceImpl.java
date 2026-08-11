package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.DecisionChainBuildResult;
import org.example.trademodel.decisionchain.RuleValidationResult;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.mapper.ConflictResolverResultMapper;
import org.example.trademodel.mapper.ExecutionPlanCandidateMapper;
import org.example.trademodel.service.AssetStateService;
import org.example.trademodel.service.ConfusedStatePolicy;
import org.example.trademodel.service.DecisionChainAiOrchestratorService;
import org.example.trademodel.service.AiConflictResolverService;
import org.example.trademodel.service.DecisionChainRuleValidator;
import org.example.trademodel.service.DecisionChainService;
import org.example.trademodel.service.OpportunityTransitionResult;
import org.example.trademodel.service.OpportunityTriggerSource;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class DecisionChainServiceImpl implements DecisionChainService {
    private final AssetPoolService assetPoolService;
    private final AssetStateService assetStateService;
    private final DecisionChainAiOrchestratorService aiOrchestratorService;
    private final AiConflictResolverService conflictResolver;
    private final DecisionChainRuleValidator ruleValidator;
    private final ExecutionPlanCandidateMapper candidateMapper;
    private final ConflictResolverResultMapper conflictMapper;
    private final ObjectMapper objectMapper;
    private final AiRoleResultsCodec aiRoleResultsCodec;

    public DecisionChainServiceImpl(AssetPoolService assetPoolService,
                                    AssetStateService assetStateService,
                                    DecisionChainAiOrchestratorService aiOrchestratorService,
                                    AiConflictResolverService conflictResolver,
                                    DecisionChainRuleValidator ruleValidator,
                                    ExecutionPlanCandidateMapper candidateMapper,
                                    ConflictResolverResultMapper conflictMapper,
                                    ObjectMapper objectMapper,
                                    AiRoleResultsCodec aiRoleResultsCodec) {
        this.assetPoolService = assetPoolService;
        this.assetStateService = assetStateService;
        this.aiOrchestratorService = aiOrchestratorService;
        this.conflictResolver = conflictResolver;
        this.ruleValidator = ruleValidator;
        this.candidateMapper = candidateMapper;
        this.conflictMapper = conflictMapper;
        this.objectMapper = objectMapper;
        this.aiRoleResultsCodec = aiRoleResultsCodec;
    }

    @Override
    public DecisionChainBuildResult build(DecisionChainBuildInput input) {
        requireInput(input);
        DecisionBundleVO decision = input.decision();
        boolean assetPoolSource = assetPoolService.isOpportunitySource(input.symbol());
        if (!assetPoolSource) {
            RuleValidationResult blocked = RuleValidationResult.blocked(List.of("ASSET_POOL_SOURCE_REQUIRED"));
            decision.setIsWorthOpening(false);
            decision.setDirectionalPushBlocked(true);
            decision.setDirectionalPushBlockReason("ASSET_POOL_SOURCE_REQUIRED");
            return new DecisionChainBuildResult(null, null, null, blocked,
                    blockedPlan(input.rulePlan(), null, null, null, blocked));
        }

        AssetStateEnum requestedState = defaultState(decision.getAssetState(), decision.getIsWorthOpening());
        OpportunityTransitionResult opportunity = assetStateService.transition(
                input.symbol(), input.timeframe(), requestedState,
                integer(decision.getConfusedScore()), integer(decision.getConfusedLowStreak()),
                input.analysisId(), input.traceId(),
                "ANALYSIS_DECISION_PROMOTION",
                triggerSource(input.triggerType(), requestedState));

        String candidateId = "candidate-" + UUID.randomUUID();
        Map<String, Object> facts = commonFacts(input, opportunity);
        AiDecisionChainResult gpt = invoke(AiDecisionChainRole.GPT_FINAL, input, candidateId, facts);
        ExecutionPlanCandidateDO candidate = gpt.successful()
                ? candidateFromAi(input, opportunity, candidateId, gpt)
                : candidateFromRule(input, opportunity, candidateId, gpt);

        Map<String, Object> reviewFacts = new LinkedHashMap<>(facts);
        reviewFacts.put("executionPlanCandidate", jsonNode(candidate.getPayloadJson()));
        AiDecisionChainResult gemini = invoke(AiDecisionChainRole.GEMINI_REVIEW,
                input, candidateId, reviewFacts);
        String geminiJson = gemini.successful() ? gemini.getPayloadJson() : fallbackGemini(gemini);

        Map<String, Object> challengeFacts = new LinkedHashMap<>(reviewFacts);
        challengeFacts.put("geminiReview", jsonNode(geminiJson));
        AiDecisionChainResult grok = invoke(AiDecisionChainRole.GROK_CHALLENGE,
                input, candidateId, challengeFacts);
        String grokJson = grok.successful() ? grok.getPayloadJson() : fallbackGrok(grok);

        ConflictResolverResultDO conflict = conflictResolver.resolveDecisionChain(
                candidate, geminiJson, grokJson, input.dataQualityScore(), decision.getConfusedScore(),
                accountRiskState(input.rulePlan()));
        if (Boolean.TRUE.equals(conflict.getConfusedDecision())
                && opportunity.state() != AssetStateEnum.CONFUSED) {
            opportunity = assetStateService.transition(
                    input.symbol(), input.timeframe(), AssetStateEnum.CONFUSED,
                    Math.max(integer(decision.getConfusedScore()), ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD),
                    integer(decision.getConfusedLowStreak()),
                    input.analysisId(), input.traceId(),
                    defaultText(conflict.getDowngradeReason(), "CONFLICT_RESOLVER_BLOCKED"),
                    OpportunityTriggerSource.CONFUSED);
        }
        RuleValidationResult validation = ruleValidator.validate(input, opportunity, candidate, conflict);
        candidate.setCandidateStatus(validation.passed() ? "VALIDATED" : "REJECTED");
        ExecutionPlanVO finalPlan = validation.passed()
                ? validatedPlan(input.rulePlan(), candidate, opportunity, conflict)
                : blockedPlan(input.rulePlan(), opportunity, candidate, conflict, validation);
        applyDecisionAdjustments(decision, conflict, validation);
        applyOpportunityState(decision, input, opportunity, validation);
        applyAiRoleResults(decision, input, candidate, conflict, gpt, gemini, grok);
        return new DecisionChainBuildResult(opportunity, candidate, conflict, validation, finalPlan);
    }

    @Override
    public void persist(DecisionChainBuildResult result) {
        if (result == null) return;
        if (result.candidate() != null && candidateMapper.insert(result.candidate()) != 1) {
            throw new IllegalStateException("ExecutionPlanCandidate insert failed");
        }
        if (result.conflict() != null && conflictMapper.insert(result.conflict()) != 1) {
            throw new IllegalStateException("ConflictResolverResult insert failed");
        }
    }

    private AiDecisionChainResult invoke(AiDecisionChainRole role,
                                         DecisionChainBuildInput input,
                                         String candidateId,
                                         Map<String, Object> facts) {
        AiDecisionChainRequest request = new AiDecisionChainRequest();
        request.setRole(role);
        request.setAnalysisId(input.analysisId());
        request.setTraceId(input.traceId());
        request.setCandidateId(candidateId);
        request.setSymbol(input.symbol());
        request.setTimeframe(input.timeframe());
        request.setInput(facts);
        return aiOrchestratorService.invoke(request);
    }

    private ExecutionPlanCandidateDO candidateFromAi(DecisionChainBuildInput input,
                                                      OpportunityTransitionResult opportunity,
                                                      String candidateId,
                                                      AiDecisionChainResult ai) {
        JsonNode payload = jsonNode(ai.getPayloadJson());
        ExecutionPlanCandidateDO candidate = baseCandidate(input, opportunity, candidateId);
        candidate.setCandidateDirection(text(payload, "direction", input.decision().getMarketBiasHierarchy()));
        candidate.setPlanMode(text(payload, "planMode", rulePlanMode(input)));
        candidate.setConfidenceLevel(text(payload, "confidence", input.decision().getConfidenceLevel()));
        candidate.setRiskLevel(text(payload, "riskLevel", input.decision().getRiskLevel()));
        candidate.setWorthOpening(payload.path("worthOpening").asBoolean(
                Boolean.TRUE.equals(input.decision().getIsWorthOpening())));
        candidate.setRecommendedAction(text(payload, "recommendedAction", input.rulePlan().getRecommendedAction()));
        candidate.setEntryZone(text(payload, "entryZone", input.rulePlan().getEntryZone()));
        candidate.setStopLoss(text(payload, "stopLoss", input.rulePlan().getStopLoss()));
        candidate.setTakeProfitRules(text(payload, "takeProfitRules", input.rulePlan().getTakeProfitRules()));
        candidate.setLeverageSuggestion(text(payload, "leverageSuggestion", input.rulePlan().getLeverageSuggestion()));
        candidate.setPositionSuggestion(text(payload, "positionSuggestion", input.rulePlan().getPositionSuggestion()));
        candidate.setInvalidCondition(text(payload, "invalidCondition", input.rulePlan().getInvalidCondition()));
        candidate.setValidity(text(payload, "validity", validity(input)));
        candidate.setSummary(text(payload, "summary", input.decision().getConclusionSummary()));
        candidate.setCandidateSource("GPT_FINAL");
        candidate.setCandidateStatus("GENERATED");
        candidate.setPayloadJson(ai.getPayloadJson());
        return candidate;
    }

    private ExecutionPlanCandidateDO candidateFromRule(DecisionChainBuildInput input,
                                                        OpportunityTransitionResult opportunity,
                                                        String candidateId,
                                                        AiDecisionChainResult failure) {
        ExecutionPlanCandidateDO candidate = baseCandidate(input, opportunity, candidateId);
        ExecutionPlanVO rulePlan = input.rulePlan();
        candidate.setCandidateDirection(input.decision().getMarketBiasHierarchy());
        candidate.setPlanMode(rulePlanMode(input));
        candidate.setConfidenceLevel(defaultText(input.decision().getConfidenceLevel(), "LOW"));
        candidate.setRiskLevel(defaultText(input.decision().getRiskLevel(), "HIGH"));
        candidate.setWorthOpening(Boolean.TRUE.equals(input.decision().getIsWorthOpening()));
        candidate.setRecommendedAction(rulePlan.getRecommendedAction());
        candidate.setEntryZone(rulePlan.getEntryZone());
        candidate.setStopLoss(rulePlan.getStopLoss());
        candidate.setTakeProfitRules(rulePlan.getTakeProfitRules());
        candidate.setLeverageSuggestion(rulePlan.getLeverageSuggestion());
        candidate.setPositionSuggestion(rulePlan.getPositionSuggestion());
        candidate.setInvalidCondition(rulePlan.getInvalidCondition());
        candidate.setValidity(validity(input));
        candidate.setSummary(input.decision().getConclusionSummary());
        candidate.setCandidateSource("RULE_FALLBACK");
        candidate.setCandidateStatus("FALLBACK");
        candidate.setFallbackReason(failure == null ? "GPT_RESULT_MISSING" : failure.getFallbackReason());
        candidate.setPayloadJson(json(candidatePayload(candidate)));
        return candidate;
    }

    private ExecutionPlanCandidateDO baseCandidate(DecisionChainBuildInput input,
                                                    OpportunityTransitionResult opportunity,
                                                    String candidateId) {
        ExecutionPlanCandidateDO candidate = new ExecutionPlanCandidateDO();
        candidate.setCandidateId(candidateId);
        candidate.setOpportunityId(opportunity.opportunityId());
        candidate.setAnalysisId(input.analysisId());
        candidate.setTraceId(input.traceId());
        candidate.setRuleDirection(input.decision().getMarketBiasHierarchy());
        candidate.setRuleConfidence(defaultText(input.decision().getConfidenceLevel(), "LOW"));
        candidate.setRuleRisk(defaultText(input.decision().getRiskLevel(), "HIGH"));
        candidate.setNotFinalPlan(true);
        candidate.setNotStateMachineMutation(true);
        candidate.setNotUserPositionCreation(true);
        candidate.setCreatedAt(LocalDateTime.now());
        return candidate;
    }

    private Map<String, Object> commonFacts(DecisionChainBuildInput input,
                                            OpportunityTransitionResult opportunity) {
        Map<String, Object> facts = new LinkedHashMap<>();
        DecisionBundleVO decision = input.decision();
        facts.put("analysis", Map.of(
                "analysisId", input.analysisId(),
                "symbol", input.symbol(),
                "timeframe", input.timeframe()));
        facts.put("evidence", evidenceFacts(input.evidence()));
        facts.put("scores", scoreFacts(input.scores()));
        facts.put("decisionBundle", Map.of(
                "ruleDirection", defaultText(decision.getMarketBiasHierarchy(), "WAIT"),
                "ruleConfidence", defaultText(decision.getConfidenceLevel(), "LOW"),
                "ruleRisk", defaultText(decision.getRiskLevel(), "HIGH"),
                "worthOpening", Boolean.TRUE.equals(decision.getIsWorthOpening()),
                "multiTimeframe", defaultText(decision.getMultiTfConvergence(), "UNKNOWN"),
                "dataQuality", input.dataQualityScore() == null ? -1 : input.dataQualityScore(),
                "confusedScore", integer(decision.getConfusedScore()),
                "riskState", opportunity.state().name()));
        facts.put("rulePlanBoundaries", candidatePayloadFromRule(input));
        facts.put("accountRisk", accountRiskState(input.rulePlan()));
        return facts;
    }

    private List<Map<String, Object>> evidenceFacts(List<EvidenceItemVO> evidence) {
        if (evidence == null) return List.of();
        return evidence.stream().filter(item -> item != null).limit(20).map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", item.getEvidenceType());
            value.put("direction", item.getDirection());
            value.put("strength", item.getStrength());
            value.put("confidence", item.getConfidence());
            value.put("source", item.getSource());
            value.put("description", item.getDescription());
            return value;
        }).toList();
    }

    private List<Map<String, Object>> scoreFacts(List<ScoreItemVO> scores) {
        if (scores == null) return List.of();
        return scores.stream().filter(item -> item != null).limit(16).map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", item.getScoreType());
            value.put("value", item.getScoreValue());
            value.put("weight", item.getWeight());
            value.put("direction", item.getDirection());
            return value;
        }).toList();
    }

    private Map<String, Object> candidatePayloadFromRule(DecisionChainBuildInput input) {
        ExecutionPlanVO plan = input.rulePlan();
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("direction", input.decision().getMarketBiasHierarchy());
        value.put("planMode", rulePlanMode(input));
        value.put("entryZone", plan.getEntryZone());
        value.put("stopLoss", plan.getStopLoss());
        value.put("takeProfitRules", plan.getTakeProfitRules());
        value.put("leverageSuggestion", plan.getLeverageSuggestion());
        value.put("positionSuggestion", plan.getPositionSuggestion());
        value.put("invalidCondition", plan.getInvalidCondition());
        value.put("validity", validity(input));
        return value;
    }

    private Map<String, Object> candidatePayload(ExecutionPlanCandidateDO candidate) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("direction", candidate.getCandidateDirection());
        value.put("planMode", candidate.getPlanMode());
        value.put("confidence", candidate.getConfidenceLevel());
        value.put("riskLevel", candidate.getRiskLevel());
        value.put("worthOpening", candidate.getWorthOpening());
        value.put("recommendedAction", candidate.getRecommendedAction());
        value.put("entryZone", candidate.getEntryZone());
        value.put("stopLoss", candidate.getStopLoss());
        value.put("takeProfitRules", candidate.getTakeProfitRules());
        value.put("leverageSuggestion", candidate.getLeverageSuggestion());
        value.put("positionSuggestion", candidate.getPositionSuggestion());
        value.put("invalidCondition", candidate.getInvalidCondition());
        value.put("validity", candidate.getValidity());
        value.put("summary", candidate.getSummary());
        return value;
    }

    private ExecutionPlanVO validatedPlan(ExecutionPlanVO rulePlan,
                                          ExecutionPlanCandidateDO candidate,
                                          OpportunityTransitionResult opportunity,
                                          ConflictResolverResultDO conflict) {
        ExecutionPlanVO plan = copyPlan(rulePlan);
        plan.setPlanMode(conflict.getPlanModeAfter());
        plan.setRecommendedAction(candidate.getRecommendedAction());
        plan.setEntryZone(candidate.getEntryZone());
        plan.setStopLoss(candidate.getStopLoss());
        plan.setTakeProfitRules(candidate.getTakeProfitRules());
        plan.setLeverageSuggestion(candidate.getLeverageSuggestion());
        plan.setPositionSuggestion(candidate.getPositionSuggestion());
        plan.setInvalidCondition(candidate.getInvalidCondition());
        plan.setCandidateId(candidate.getCandidateId());
        plan.setOpportunityId(opportunity.opportunityId());
        plan.setResolverResultId(conflict.getResolverResultId());
        plan.setTraceId(candidate.getTraceId());
        plan.setChainStatus("RULE_FALLBACK".equals(candidate.getCandidateSource())
                ? "RULE_FALLBACK_VALIDATED" : "FINAL_VALIDATED");
        plan.setRuleValidationStatus("PASS");
        plan.setRuleVetoReason(null);
        plan.setFinalizedAt(LocalDateTime.now());
        plan.setFinalPlan(true);
        return plan;
    }

    private ExecutionPlanVO blockedPlan(ExecutionPlanVO rulePlan,
                                        OpportunityTransitionResult opportunity,
                                        ExecutionPlanCandidateDO candidate,
                                        ConflictResolverResultDO conflict,
                                        RuleValidationResult validation) {
        ExecutionPlanVO plan = copyPlan(rulePlan);
        boolean sourceIncomplete = rulePlan == null
                || !Boolean.TRUE.equals(rulePlan.getSourceGateComplete())
                || ExecutionPlanVO.EXECUTION_PLAN_STATUS_INCOMPLETE.equalsIgnoreCase(
                rulePlan.getExecutionPlanStatus());
        if (!sourceIncomplete) {
            plan.setExecutionPlanStatus(ExecutionPlanVO.EXECUTION_PLAN_STATUS_BLOCKED);
            plan.setReadinessStatus(ExecutionPlanVO.READINESS_WATCH_ONLY);
        }
        plan.setCandidateId(candidate == null ? null : candidate.getCandidateId());
        plan.setOpportunityId(opportunity == null ? null : opportunity.opportunityId());
        plan.setResolverResultId(conflict == null ? null : conflict.getResolverResultId());
        plan.setTraceId(candidate == null ? null : candidate.getTraceId());
        plan.setChainStatus("RULE_VALIDATION_BLOCKED");
        plan.setRuleValidationStatus("BLOCKED");
        plan.setRuleVetoReason(String.join(";", validation.reasons()));
        plan.setFinalPlan(false);
        plan.setFinalizedAt(null);
        return plan;
    }

    private static ExecutionPlanVO copyPlan(ExecutionPlanVO source) {
        ExecutionPlanVO target = new ExecutionPlanVO();
        if (source == null) return target;
        target.setPlanId(source.getPlanId());
        target.setPlanMode(source.getPlanMode());
        target.setExecutionPlanStatus(source.getExecutionPlanStatus());
        target.setReadinessStatus(source.getReadinessStatus());
        target.setSourceGateStatus(source.getSourceGateStatus());
        target.setSourceGateComplete(source.getSourceGateComplete());
        target.setSourceCompletenessSummary(source.getSourceCompletenessSummary());
        target.setMissingSourceReasons(source.getMissingSourceReasons());
        target.setSourceBlockerReasons(source.getSourceBlockerReasons());
        target.setSourceTraceStatus(source.getSourceTraceStatus());
        target.setSourceTraceComplete(source.getSourceTraceComplete());
        target.setNotExecutableReason(source.getNotExecutableReason());
        target.setManualReviewRequired(true);
        target.setNotTradeInstruction(true);
        target.setNotExecutable(true);
        target.setNotAutoTrading(true);
        target.setNotOrderExecution(true);
        target.setNotUserPositionCreation(true);
        target.setRiskActionGuardStatus(source.getRiskActionGuardStatus());
        target.setRiskActionGuardBlockingReason(source.getRiskActionGuardBlockingReason());
        target.setRiskActionGuardReady(source.getRiskActionGuardReady());
        target.setRecommendedAction(source.getRecommendedAction());
        target.setEntryZone(source.getEntryZone());
        target.setStopLoss(source.getStopLoss());
        target.setTakeProfitRules(source.getTakeProfitRules());
        target.setAddPositionCondition(source.getAddPositionCondition());
        target.setReducePositionCondition(source.getReducePositionCondition());
        target.setAbandonCondition(source.getAbandonCondition());
        target.setInvalidCondition(source.getInvalidCondition());
        target.setLeverageSuggestion(source.getLeverageSuggestion());
        target.setPositionSuggestion(source.getPositionSuggestion());
        target.setNeedsRevalidation(source.getNeedsRevalidation());
        target.setRevalidationReason(source.getRevalidationReason());
        target.setDerivativesStatus(source.getDerivativesStatus());
        target.setDerivativesFreshness(source.getDerivativesFreshness());
        target.setDerivativesReasonCodes(source.getDerivativesReasonCodes());
        target.setDerivativesProviderDataTime(source.getDerivativesProviderDataTime());
        target.setDerivativesTraceId(source.getDerivativesTraceId());
        return target;
    }

    private void applyDecisionAdjustments(DecisionBundleVO decision,
                                          ConflictResolverResultDO conflict,
                                          RuleValidationResult validation) {
        decision.setConfidenceLevel(conflict.getConfidenceAfter());
        decision.setRiskLevel(conflict.getRiskAfter());
        decision.setAiPlanMode(conflict.getPlanModeAfter());
        decision.setAiConflictLevel(conflict.getConflictLevel());
        decision.setAiConflictScore(conflict.getConflictScore());
        if (!validation.passed()) decision.setIsWorthOpening(false);
    }

    private void applyOpportunityState(DecisionBundleVO decision,
                                       DecisionChainBuildInput input,
                                       OpportunityTransitionResult opportunity,
                                       RuleValidationResult validation) {
        decision.setAssetState(opportunity.state());
        boolean blocked = "BLOCKED".equals(opportunity.executionPermission()) || !validation.passed();
        if (opportunity.state() == AssetStateEnum.CONFUSED) {
            decision.setConfusedScore(Math.max(integer(decision.getConfusedScore()),
                    ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD));
        }
        decision.setDirectionalPushBlocked(blocked);
        decision.setDirectionalPushBlockReason(blocked
                ? String.join(";", validation.reasons())
                : null);
        decision.setAssetStateSnapshot(assetStateService.buildSnapshotAtDecision(
                input.symbol(), input.analysisId(), opportunity.previousState(), opportunity.state(),
                integer(decision.getConfusedScore()), integer(decision.getConfusedLowStreak()), blocked,
                decision.isMultiTimeframeAligned()));
    }

    private void applyAiRoleResults(DecisionBundleVO decision,
                                    DecisionChainBuildInput input,
                                    ExecutionPlanCandidateDO candidate,
                                    ConflictResolverResultDO conflict,
                                    AiDecisionChainResult gpt,
                                    AiDecisionChainResult gemini,
                                    AiDecisionChainResult grok) {
        Map<AiDecisionChainRole, AiDecisionChainResult> roles = new LinkedHashMap<>();
        roles.put(AiDecisionChainRole.GPT_FINAL, gpt);
        roles.put(AiDecisionChainRole.GEMINI_REVIEW, gemini);
        roles.put(AiDecisionChainRole.GROK_CHALLENGE, grok);
        AiRoleResultsPayload.SynthesisPayload synthesis = new AiRoleResultsPayload.SynthesisPayload(
                candidate.getRuleDirection(),
                conflict.getConfidenceAfter(),
                conflict.getRiskAfter(),
                Boolean.TRUE.equals(candidate.getWorthOpening()),
                conflict.getConflictLevel(),
                conflict.getConflictScore(),
                conflict.getConfidenceAfter(),
                conflict.getRiskAfter(),
                conflict.getPlanModeAfter(),
                conflict.getConfusedDecision(),
                conflict.getDowngradeReason());
        decision.setAiRoleResults(aiRoleResultsCodec.serializeDecisionChain(
                input.analysisId(), input.traceId(), candidate.getRuleDirection(), roles, synthesis));
    }

    private String fallbackGemini(AiDecisionChainResult result) {
        return json(Map.of(
                "fallback", true,
                "role", "GEMINI_REVIEW",
                "fallbackReason", fallbackReason(result)));
    }

    private String fallbackGrok(AiDecisionChainResult result) {
        return json(Map.of(
                "fallback", true,
                "role", "GROK_CHALLENGE",
                "fallbackReason", fallbackReason(result)));
    }

    private static String fallbackReason(AiDecisionChainResult result) {
        return result == null || result.getFallbackReason() == null
                ? "AI_RESULT_UNAVAILABLE" : result.getFallbackReason();
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            return "{}";
        }
    }

    private JsonNode jsonNode(String value) {
        try {
            return objectMapper.readTree(value == null ? "{}" : value);
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private static String text(JsonNode node, String field, String fallback) {
        String value = node == null ? null : node.path(field).asText(null);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String validity(DecisionChainBuildInput input) {
        return input.decision().getExpiresAt() == null ? "SOURCE_VALIDITY_UNAVAILABLE"
                : input.decision().getExpiresAt().toString();
    }

    private static String accountRiskState(ExecutionPlanVO plan) {
        if (plan == null) return "UNAVAILABLE";
        if (Boolean.TRUE.equals(plan.getRiskActionGuardReady())) return "READY";
        return defaultText(plan.getRiskActionGuardBlockingReason(),
                defaultText(plan.getRiskActionGuardStatus(), "UNAVAILABLE"));
    }

    private static String rulePlanMode(DecisionChainBuildInput input) {
        if (input.decision().isDirectionalPushBlocked()) return "BLOCKED";
        if (Boolean.TRUE.equals(input.decision().getIsWorthOpening())
                && Boolean.TRUE.equals(input.rulePlan().getSourceGateComplete())) return "CONFIRM";
        if (Boolean.TRUE.equals(input.decision().getIsWorthOpening())) return "PREPARE";
        if ("HIGH".equalsIgnoreCase(input.decision().getRiskLevel())) return "REDUCE";
        return "WATCH";
    }

    private static OpportunityTriggerSource triggerSource(AnalysisRunTriggerType triggerType,
                                                          AssetStateEnum requestedState) {
        if (triggerType == AnalysisRunTriggerType.HOT_RESET_REBUILD) return OpportunityTriggerSource.HOT_RESET;
        if (requestedState == AssetStateEnum.CONFUSED) return OpportunityTriggerSource.CONFUSED;
        if (requestedState == AssetStateEnum.INVALIDATED) return OpportunityTriggerSource.INVALIDATION;
        if (triggerType == AnalysisRunTriggerType.ASSET_POOL_SCAN) return OpportunityTriggerSource.ASSET_POOL_SCAN;
        return OpportunityTriggerSource.ANALYSIS;
    }

    private static AssetStateEnum defaultState(AssetStateEnum state, Boolean worthOpening) {
        if (state != null) return state;
        return Boolean.TRUE.equals(worthOpening) ? AssetStateEnum.CANDIDATE : AssetStateEnum.OBSERVING;
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int integer(Integer value) {
        return value == null ? 0 : value;
    }

    private static void requireInput(DecisionChainBuildInput input) {
        if (input == null || input.decision() == null || input.rulePlan() == null
                || input.analysisId() == null || input.analysisId().isBlank()
                || input.traceId() == null || input.traceId().isBlank()
                || input.symbol() == null || input.symbol().isBlank()) {
            throw new IllegalArgumentException("decision-chain input is incomplete");
        }
    }
}
