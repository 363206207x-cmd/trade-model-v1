package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.analysisrun.AnalysisRunTriggerType;
import org.example.trademodel.common.EvidenceTypeConstants;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.DecisionChainBuildResult;
import org.example.trademodel.decisionchain.RuleValidationResult;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.entity.TmAccountRiskSnapshotDO;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.enums.PlanModeEnum;
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
import org.example.trademodel.service.OpportunityStateIdentity;
import org.example.trademodel.service.watchlistsource.AssetPoolService;
import org.example.trademodel.service.support.ExecutionFeasibilityContract;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.ExecutionPlanVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DecisionChainServiceImpl implements DecisionChainService {
    private static final Set<String> FROZEN_EIGHT_SCORE_TYPES = Set.of(
            "趋势结构分", "资金推动分", "杠杆风险分", "流动性质量分",
            "情绪温度分", "事件冲击分", "宏观环境分", "综合可信度分");
    private static final Set<String> FROZEN_TIMEFRAMES = Set.of("4h", "1h", "15m", "5m");
    private final AssetPoolService assetPoolService;
    private final AssetStateService assetStateService;
    private final DecisionChainAiOrchestratorService aiOrchestratorService;
    private final AiConflictResolverService conflictResolver;
    private final DecisionChainRuleValidator ruleValidator;
    private final ExecutionPlanCandidateMapper candidateMapper;
    private final ConflictResolverResultMapper conflictMapper;
    private final ObjectMapper objectMapper;
    private final AiRoleResultsCodec aiRoleResultsCodec;
    private final FundamentalAiV41Properties v41Properties;

    public DecisionChainServiceImpl(AssetPoolService assetPoolService,
                                    AssetStateService assetStateService,
                                    DecisionChainAiOrchestratorService aiOrchestratorService,
                                    AiConflictResolverService conflictResolver,
                                    DecisionChainRuleValidator ruleValidator,
                                    ExecutionPlanCandidateMapper candidateMapper,
                                    ConflictResolverResultMapper conflictMapper,
                                    ObjectMapper objectMapper,
                                    AiRoleResultsCodec aiRoleResultsCodec) {
        this(assetPoolService, assetStateService, aiOrchestratorService, conflictResolver,
                ruleValidator, candidateMapper, conflictMapper, objectMapper, aiRoleResultsCodec,
                FundamentalAiV41Properties.contractFixture());
    }

    @Autowired
    public DecisionChainServiceImpl(AssetPoolService assetPoolService,
                                    AssetStateService assetStateService,
                                    DecisionChainAiOrchestratorService aiOrchestratorService,
                                    AiConflictResolverService conflictResolver,
                                    DecisionChainRuleValidator ruleValidator,
                                    ExecutionPlanCandidateMapper candidateMapper,
                                    ConflictResolverResultMapper conflictMapper,
                                    ObjectMapper objectMapper,
                                    AiRoleResultsCodec aiRoleResultsCodec,
                                    FundamentalAiV41Properties v41Properties) {
        this.assetPoolService = assetPoolService;
        this.assetStateService = assetStateService;
        this.aiOrchestratorService = aiOrchestratorService;
        this.conflictResolver = conflictResolver;
        this.ruleValidator = ruleValidator;
        this.candidateMapper = candidateMapper;
        this.conflictMapper = conflictMapper;
        this.objectMapper = objectMapper;
        this.aiRoleResultsCodec = aiRoleResultsCodec;
        this.v41Properties = v41Properties;
    }

    @Override
    public DecisionChainBuildResult build(DecisionChainBuildInput input) {
        requireInput(input);
        DecisionBundleVO decision = input.decision();
        materializeRuleResult(input);
        boolean assetPoolSource = assetPoolService.isOpportunitySource(
                input.ownerType(), input.ownerId(), input.assetId(), input.symbol());
        if (!assetPoolSource && !input.preview()) {
            RuleValidationResult blocked = RuleValidationResult.blocked(List.of("ASSET_POOL_SOURCE_REQUIRED"));
            decision.setIsWorthOpening(false);
            decision.setDirectionalPushBlocked(true);
            decision.setDirectionalPushBlockReason("ASSET_POOL_SOURCE_REQUIRED");
            return new DecisionChainBuildResult(null, null, null, blocked,
                    blockedPlan(input.rulePlan(), null, null, null, blocked));
        }

        if (input.preview()) {
            return buildPreview(input);
        }

        AssetStateEnum requestedState = defaultState(decision.getAssetState(), decision.getIsWorthOpening());
        ExecutionFeasibilityContract.Assessment executionFeasibility =
                ExecutionFeasibilityContract.assess(input.rulePlan());
        String transitionReason = "ANALYSIS_DECISION_PROMOTION";
        if (requestedState == AssetStateEnum.TRIGGERED && !executionFeasibility.allowed()) {
            requestedState = AssetStateEnum.WAITING_TRIGGER;
            transitionReason = defaultText(executionFeasibility.reasonCode(),
                    "EXECUTION_FEASIBILITY_NOT_VERIFIED");
        }
        OpportunityTransitionResult opportunity = assetStateService.transition(
                new OpportunityStateIdentity(input.ownerType(), input.ownerId(), input.assetId(),
                        input.symbol(), input.timeframe()), requestedState,
                integer(decision.getConfusedScore()), integer(decision.getConfusedLowStreak()),
                input.analysisId(), input.traceId(), input.ruleVersion(),
                transitionReason,
                triggerSource(input.triggerType(), requestedState));

        String candidateId = "candidate-" + UUID.randomUUID();
        Map<String, Object> facts = commonFacts(input, opportunity);
        AiDecisionChainResult gpt = invoke(AiDecisionChainRole.GPT_FINAL, input, candidateId, facts);
        ExecutionPlanCandidateDO candidate = gpt.successful()
                ? candidateFromAi(input, opportunity, candidateId, gpt)
                : candidateFromRule(input, opportunity, candidateId, gpt);

        Map<String, Object> reviewFacts = new LinkedHashMap<>(facts);
        reviewFacts.put("executionPlanCandidate", jsonNode(candidate.getPayloadJson()));
        reviewFacts.put("candidateSource", candidate.getCandidateSource());
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
                accountRiskState(input.accountRiskSnapshot()));
        if (Boolean.TRUE.equals(conflict.getConfusedDecision())
                && opportunity.state() != AssetStateEnum.CONFUSED) {
            opportunity = assetStateService.transition(
                    new OpportunityStateIdentity(input.ownerType(), input.ownerId(), input.assetId(),
                            input.symbol(), input.timeframe()), AssetStateEnum.CONFUSED,
                    Math.max(integer(decision.getConfusedScore()), ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD),
                    integer(decision.getConfusedLowStreak()),
                    input.analysisId(), input.traceId(), input.ruleVersion(),
                    defaultText(conflict.getDowngradeReason(), "CONFLICT_RESOLVER_BLOCKED"),
                    OpportunityTriggerSource.CONFUSED);
        }
        RuleValidationResult validation = ruleValidator.validate(input, opportunity, candidate, conflict);
        if (!"GPT_FINAL".equals(candidate.getCandidateSource()) && validation.passed()) {
            validation = RuleValidationResult.blocked(List.of("GPT_CANDIDATE_REQUIRED"));
        }
        candidate.setCandidateStatus(validation.passed() ? "VALIDATED" : "REJECTED");
        ExecutionPlanVO finalPlan = validation.passed()
                ? validatedPlan(input, candidate, opportunity, conflict)
                : blockedPlan(input.rulePlan(), opportunity, candidate, conflict, validation);
        applyDecisionAdjustments(decision, conflict, validation);
        applyOpportunityState(decision, input, opportunity, validation);
        applyAiRoleResults(decision, input, candidate, conflict, gpt, gemini, grok);
        persistOpportunityProjection(input, opportunity, conflict, validation);
        return new DecisionChainBuildResult(opportunity, candidate, conflict, validation, finalPlan);
    }

    @Override
    public void persist(DecisionChainBuildResult result) {
        if (result == null || result.preview()) return;
        if (result.candidate() != null && candidateMapper.insert(result.candidate()) != 1) {
            throw new IllegalStateException("ExecutionPlanCandidate insert failed");
        }
        if (result.conflict() != null && conflictMapper.insert(result.conflict()) != 1) {
            throw new IllegalStateException("ConflictResolverResult insert failed");
        }
    }

    private DecisionChainBuildResult buildPreview(DecisionChainBuildInput input) {
        OpportunityTransitionResult previewContext = new OpportunityTransitionResult(
                null, input.symbol(), AssetStateEnum.OBSERVING, AssetStateEnum.OBSERVING,
                false, false, "ANALYSIS_PREVIEW_NON_PERSISTENT", "ANALYSIS_PREVIEW",
                "NOT_ELIGIBLE", LocalDateTime.now());
        String candidateId = "preview-candidate-" + UUID.randomUUID();
        Map<String, Object> facts = commonFacts(input, previewContext);
        facts.put("preview", true);
        facts.put("persistenceBoundary", "NO_PERSISTED_OPPORTUNITY_CANDIDATE_OR_FINAL");

        AiDecisionChainResult gpt = invoke(AiDecisionChainRole.GPT_FINAL, input, candidateId, facts);
        ExecutionPlanCandidateDO candidate = gpt.successful()
                ? candidateFromAi(input, previewContext, candidateId, gpt)
                : candidateFromRule(input, previewContext, candidateId, gpt);
        candidate.setOpportunityId(null);
        candidate.setCandidateStatus("PREVIEW_ONLY");

        Map<String, Object> reviewFacts = new LinkedHashMap<>(facts);
        reviewFacts.put("executionPlanCandidate", jsonNode(candidate.getPayloadJson()));
        reviewFacts.put("candidateSource", candidate.getCandidateSource());
        AiDecisionChainResult gemini = invoke(AiDecisionChainRole.GEMINI_REVIEW,
                input, candidateId, reviewFacts);
        String geminiJson = gemini.successful() ? gemini.getPayloadJson() : fallbackGemini(gemini);
        Map<String, Object> challengeFacts = new LinkedHashMap<>(reviewFacts);
        challengeFacts.put("geminiReview", jsonNode(geminiJson));
        AiDecisionChainResult grok = invoke(AiDecisionChainRole.GROK_CHALLENGE,
                input, candidateId, challengeFacts);
        String grokJson = grok.successful() ? grok.getPayloadJson() : fallbackGrok(grok);
        ConflictResolverResultDO conflict = conflictResolver.resolveDecisionChain(
                candidate, geminiJson, grokJson, input.dataQualityScore(),
                input.decision().getConfusedScore(), accountRiskState(input.accountRiskSnapshot()));
        applyDecisionAdjustments(input.decision(), conflict,
                RuleValidationResult.blocked(List.of("ANALYSIS_PREVIEW_NON_FINAL")));
        applyAiRoleResults(input.decision(), input, candidate, conflict, gpt, gemini, grok);
        return new DecisionChainBuildResult(null, candidate, conflict,
                RuleValidationResult.blocked(List.of("ANALYSIS_PREVIEW_NON_FINAL")), null, true);
    }

    private void materializeRuleResult(DecisionChainBuildInput input) {
        DecisionBundleVO decision = input.decision();
        String ruleBias = requiredRuleText(decision.getRuleMarketBias(), "RULE_MARKET_BIAS_MISSING");
        String ruleConfidence = requiredRuleText(decision.getRuleConfidence(), "RULE_CONFIDENCE_MISSING");
        String ruleRisk = requiredRuleText(decision.getRuleRisk(), "RULE_RISK_MISSING");
        String ruleMode = rulePlanMode(input);
        Integer dataQuality = input.dataQualityScore();
        boolean circuitOpen = dataQuality == null
                || dataQuality < v41Properties.getAiGate().getCircuitBreakerScore();
        boolean aiQualityEligible = !circuitOpen
                && dataQuality >= v41Properties.getAiGate().getMinimumDataQuality();
        boolean executionFeasibilityReady = ExecutionFeasibilityContract.assess(input.rulePlan()).allowed();
        if (!aiQualityEligible) {
            ruleConfidence = downgradeConfidence(ruleConfidence);
            ruleMode = circuitOpen ? PlanModeEnum.BLOCKED.name()
                    : moreRestrictiveMode(ruleMode, PlanModeEnum.OBSERVATION);
        }
        decision.setRuleMarketBias(ruleBias);
        decision.setRuleConfidence(ruleConfidence);
        decision.setRuleRisk(ruleRisk);
        decision.setRulePlanMode(ruleMode);
        decision.setConfidenceLevel(ruleConfidence);
        decision.setRuleCanExecute(aiQualityEligible
                && Boolean.TRUE.equals(decision.getIsWorthOpening())
                && Boolean.TRUE.equals(input.rulePlan().getSourceGateComplete())
                && executionFeasibilityReady
                && accountRiskInputReady(input.accountRiskSnapshot())
                && Boolean.TRUE.equals(input.accountRiskSnapshot().getRiskAllowed())
                && !decision.isDirectionalPushBlocked());
    }

    private AiDecisionChainResult invoke(AiDecisionChainRole role,
                                         DecisionChainBuildInput input,
                                         String candidateId,
                                         Map<String, Object> facts) {
        AiDecisionChainRequest request = new AiDecisionChainRequest();
        request.setRole(role);
        request.setAnalysisId(input.analysisId());
        request.setTraceId(input.traceId());
        request.setRequestId(input.requestId());
        request.setOpportunityId(opportunityId(facts));
        request.setCandidateId(candidateId);
        request.setRuleVersion(input.ruleVersion());
        request.setSymbol(input.symbol());
        request.setTimeframe(input.timeframe());
        request.setInput(facts);
        List<String> failures = aiInputFailures(role, input, facts);
        request.setInputContractSatisfied(failures.isEmpty());
        request.setInputContractFailures(failures);
        return aiOrchestratorService.invoke(request);
    }

    private List<String> aiInputFailures(AiDecisionChainRole role,
                                         DecisionChainBuildInput input,
                                         Map<String, Object> facts) {
        List<String> failures = new ArrayList<>();
        if (input.dataQualityScore() == null
                || input.dataQualityScore() < v41Properties.getAiGate().getMinimumDataQuality()) {
            failures.add("DATA_QUALITY_BELOW_AI_THRESHOLD");
        }
        if (input.evidence() == null || input.evidence().isEmpty()
                || input.evidence().stream().anyMatch(item -> item == null
                || !hasText(item.getEvidenceId()) || !hasText(item.getSource())
                || !java.util.Objects.equals(item.getAnalysisId(), input.analysisId())
                || (!hasText(item.getSourceReference()) && !hasText(item.getSourceTraceId()))
                || item.getStrength() == null || item.getConfidence() == null
                || !hasText(item.getCurrentValue()) || !hasText(item.getChangeFromBaseline())
                || item.getObservedAt() == null || !hasText(item.getFreshness()))) {
            failures.add("EVIDENCE_CONTRACT_INCOMPLETE");
        }
        if (input.evidence() == null || input.evidence().stream()
                .noneMatch(item -> isSignificantAiTriggerEvidence(item,
                        v41Properties.getAiGate().getMinimumSignificantEvidenceStrength()))) {
            failures.add("SIGNIFICANT_EVIDENCE_CHANGE_MISSING");
        }
        Set<String> scoreTypes = input.scores() == null ? Set.of() : input.scores().stream()
                .filter(item -> item != null && hasText(item.getScoreType()))
                .map(item -> item.getScoreType().trim())
                .collect(java.util.stream.Collectors.toSet());
        boolean scoresComplete = input.scores() != null && input.scores().size() == FROZEN_EIGHT_SCORE_TYPES.size()
                && scoreTypes.equals(FROZEN_EIGHT_SCORE_TYPES)
                && input.scores().stream().allMatch(item -> item != null
                && hasText(item.getScoreId()) && item.getScoreValue() != null && item.getWeight() != null);
        if (!scoresComplete) failures.add("EIGHT_SCORE_CONTRACT_INCOMPLETE");
        DecisionBundleVO decision = input.decision();
        if (!hasText(decision.getRuleMarketBias()) || !hasText(decision.getRuleConfidence())
                || !hasText(decision.getRuleRisk()) || !hasText(decision.getRulePlanMode())
                || decision.getRuleCanExecute() == null || !hasText(decision.getMultiTfConvergence())) {
            failures.add("RULE_DECISION_CONTEXT_INCOMPLETE");
        }
        if (decision.getMultiTimeframeDetails() == null
                || !decision.getMultiTimeframeDetails().keySet().equals(FROZEN_TIMEFRAMES)
                || decision.getMultiTimeframeDetails().values().stream().anyMatch(value -> value == null
                || !"FOUND".equals(value.get("state")) || value.get("direction") == null
                || value.get("trendScore") == null)) {
            failures.add("MULTI_TIMEFRAME_CONTRACT_INCOMPLETE");
        }
        if (!Boolean.TRUE.equals(input.rulePlan().getSourceGateComplete())) {
            failures.add("RULE_SOURCE_GATE_INCOMPLETE");
        }
        if (!hasText(input.ruleVersion())) failures.add("RULE_VERSION_MISSING");
        if (!accountRiskInputReady(input.accountRiskSnapshot())) {
            failures.add("ACCOUNT_RISK_CONTEXT_UNAVAILABLE");
        }
        if (role != AiDecisionChainRole.GPT_FINAL && !facts.containsKey("executionPlanCandidate")) {
            failures.add("CANDIDATE_CONTEXT_MISSING");
        }
        if (role != AiDecisionChainRole.GPT_FINAL
                && !"GPT_FINAL".equals(facts.get("candidateSource"))) {
            failures.add("GPT_CANDIDATE_UNAVAILABLE");
        }
        if (role == AiDecisionChainRole.GROK_CHALLENGE && !facts.containsKey("geminiReview")) {
            failures.add("GEMINI_REVIEW_CONTEXT_MISSING");
        }
        return failures;
    }

    private static boolean isSignificantAiTriggerEvidence(EvidenceItemVO item, int minimumStrength) {
        if (item == null || item.getStrength() == null || item.getStrength() < minimumStrength
                || !"FRESH".equalsIgnoreCase(item.getFreshness())) {
            return false;
        }
        String type = upper(item.getEvidenceType());
        if (type.equals(upper(EvidenceTypeConstants.PRICE_STRUCTURE))
                || type.equals(upper(EvidenceTypeConstants.FUNDING))
                || type.equals(upper(EvidenceTypeConstants.EVENT))
                || type.equals(upper(EvidenceTypeConstants.MACRO))
                || type.equals(upper(EvidenceTypeConstants.NEWS))) {
            return true;
        }
        if (type.equals(upper(EvidenceTypeConstants.LEVERAGE))) {
            return false;
        }
        String signal = String.join("|",
                type,
                upper(item.getChangeFromBaseline()),
                upper(item.getDescription()),
                upper(item.getSourceReference()));
        return containsAny(signal,
                "PRICE", "VOLUME", "HIGH_LOW_RANGE", "VOLATILITY",
                "OPEN_INTEREST", "OI_", "FUNDING", "EVENT", "LIQUIDATION");
    }

    private static boolean containsAny(String value, String... needles) {
        if (value == null || needles == null) return false;
        for (String needle : needles) {
            if (needle != null && value.contains(needle)) return true;
        }
        return false;
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private ExecutionPlanCandidateDO candidateFromAi(DecisionChainBuildInput input,
                                                      OpportunityTransitionResult opportunity,
                                                      String candidateId,
                                                      AiDecisionChainResult ai) {
        JsonNode payload = jsonNode(ai.getPayloadJson());
        JsonNode coreJudgment = payload.path("coreJudgment");
        JsonNode biasAdjustment = payload.path("biasAdjustment");
        JsonNode summary = payload.path("candidateSummary");
        ExecutionPlanCandidateDO candidate = baseCandidate(input, opportunity, candidateId);
        candidate.setCandidateDirection(text(biasAdjustment, "after", null));
        candidate.setBiasAdjustmentReason(text(biasAdjustment, "reason", null));
        candidate.setPlanMode(text(summary, "planMode", null));
        candidate.setConfidenceLevel(text(summary, "confidence", null));
        candidate.setRiskLevel(text(summary, "riskLevel", null));
        candidate.setWorthOpening(summary.has("worthOpening")
                ? summary.path("worthOpening").asBoolean() : null);
        candidate.setRecommendedAction(text(summary, "recommendedAction", null));
        candidate.setOpportunityType(text(summary, "opportunityType", null));
        candidate.setEntryLogic(text(summary, "entryLogic", null));
        candidate.setEntryZone(text(summary, "entryZone", null));
        candidate.setEntrySource(text(summary, "entrySource", null));
        candidate.setEntryReason(text(summary, "entryReason", null));
        candidate.setTriggerCondition(text(summary, "triggerCondition", null));
        candidate.setStopLogic(text(summary, "stopLogic", null));
        candidate.setStopLoss(text(summary, "stopZone", null));
        candidate.setStopSource(text(summary, "stopSource", null));
        candidate.setStopReason(text(summary, "stopReason", null));
        candidate.setTargetLogic(text(summary, "targetLogic", null));
        candidate.setTakeProfitRules(text(summary, "targetZones", null));
        candidate.setTargetSource(text(summary, "targetSource", null));
        candidate.setTargetReason(text(summary, "targetReason", null));
        candidate.setAddPositionCondition(text(summary, "addPositionCondition", null));
        candidate.setReducePositionCondition(text(summary, "reducePositionCondition", null));
        candidate.setAbandonCondition(text(summary, "abandonCondition", null));
        candidate.setLeverageSuggestion(text(summary, "leverageSuggestion", null));
        candidate.setPositionSuggestion(text(summary, "positionSuggestion", null));
        candidate.setRiskExplanation(text(summary, "riskExplanation", null));
        candidate.setInvalidCondition(text(summary, "invalidCondition", null));
        candidate.setInvalidationSource(text(summary, "invalidationSource", null));
        candidate.setInvalidationReason(text(summary, "invalidationReason", null));
        candidate.setExpectedRiskReward(decimal(summary, "expectedRiskReward"));
        candidate.setExpectedRiskRewardSource(text(summary, "expectedRiskRewardSource", null));
        candidate.setExpectedRiskRewardReason(text(summary, "expectedRiskRewardReason", null));
        candidate.setValidity(text(summary, "validity", null));
        candidate.setAnalysisTimeframesJson(json(payload.path("multiTimeframeExplanation")));
        candidate.setTriggerTimeframe(text(summary, "triggerTimeframe", null));
        candidate.setHoldingHorizon(text(summary, "holdingHorizon", null));
        candidate.setRevalidationRule(text(summary, "revalidationRule", null));
        candidate.setSummary(text(summary, "summary", null));
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
        candidate.setCandidateDirection(input.decision().getRuleMarketBias());
        candidate.setPlanMode(input.decision().getRulePlanMode());
        candidate.setConfidenceLevel(input.decision().getRuleConfidence());
        candidate.setRiskLevel(input.decision().getRuleRisk());
        candidate.setWorthOpening(false);
        candidate.setTriggerTimeframe(input.timeframe());
        candidate.setSummary(null);
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
        candidate.setOpportunityId(opportunity == null ? null : opportunity.opportunityId());
        candidate.setAnalysisId(input.analysisId());
        candidate.setTraceId(input.traceId());
        candidate.setAssetId(input.assetId());
        candidate.setRuleVersion(input.ruleVersion());
        candidate.setRuleDirection(input.decision().getRuleMarketBias());
        candidate.setRuleConfidence(input.decision().getRuleConfidence());
        candidate.setRuleRisk(input.decision().getRuleRisk());
        candidate.setRulePlanMode(input.decision().getRulePlanMode());
        candidate.setRuleCanExecute(input.decision().getRuleCanExecute());
        candidate.setDataQuality(input.dataQualityScore());
        candidate.setConfusedScore(input.decision().getConfusedScore());
        candidate.setAccountRiskSnapshotId(input.accountRiskSnapshot() == null
                ? null : input.accountRiskSnapshot().getId());
        candidate.setTriggerTimeframe(input.timeframe());
        candidate.setValidFrom(input.decision().getValidFrom() == null ? null
                : input.decision().getValidFrom().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        candidate.setValidUntil(input.decision().getExpiresAt() == null ? null
                : input.decision().getExpiresAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        candidate.setEvidenceRefsJson(json(input.evidence() == null ? List.of() : input.evidence().stream()
                .filter(item -> item != null && item.getEvidenceId() != null)
                .map(EvidenceItemVO::getEvidenceId).distinct().toList()));
        candidate.setScoreRefsJson(json(input.scores() == null ? List.of() : input.scores().stream()
                .filter(item -> item != null && item.getScoreId() != null)
                .map(ScoreItemVO::getScoreId).distinct().toList()));
        candidate.setSourceRefsJson(json(input.evidence() == null ? List.of() : input.evidence().stream()
                .filter(item -> item != null && item.getSourceTraceId() != null)
                .map(EvidenceItemVO::getSourceTraceId).distinct().toList()));
        candidate.setVersion(1);
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
        Map<String, Object> opportunityFacts = new LinkedHashMap<>();
        opportunityFacts.put("opportunityId", opportunity == null ? null : opportunity.opportunityId());
        opportunityFacts.put("state", opportunity == null || opportunity.state() == null
                ? null : opportunity.state().name());
        opportunityFacts.put("executionPermission", opportunity == null
                ? null : opportunity.executionPermission());
        facts.put("opportunity", opportunityFacts);
        facts.put("evidence", evidenceFacts(input.evidence(), input.analysisId()));
        facts.put("scores", scoreFacts(input.scores()));
        Map<String, Object> decisionFacts = new LinkedHashMap<>();
        decisionFacts.put("ruleDirection", decision.getRuleMarketBias());
        decisionFacts.put("ruleConfidence", decision.getRuleConfidence());
        decisionFacts.put("ruleRisk", decision.getRuleRisk());
        decisionFacts.put("rulePlanMode", decision.getRulePlanMode());
        decisionFacts.put("ruleCanExecute", decision.getRuleCanExecute());
        decisionFacts.put("worthOpening", decision.getIsWorthOpening());
        decisionFacts.put("multiTimeframeConvergence", decision.getMultiTfConvergence());
        decisionFacts.put("multiTimeframe", decision.getMultiTimeframeDetails());
        decisionFacts.put("dataQuality", input.dataQualityScore());
        decisionFacts.put("confusedScore", decision.getConfusedScore());
        decisionFacts.put("riskState", opportunity == null || opportunity.state() == null
                ? null : opportunity.state().name());
        facts.put("decisionBundle", decisionFacts);
        Map<String, Object> executionFeasibility = new LinkedHashMap<>();
        executionFeasibility.put("status", input.rulePlan().getExecutionFeasibilityStatus());
        executionFeasibility.put("slippageStatus", input.rulePlan().getSlippageStatus());
        executionFeasibility.put("depthStatus", input.rulePlan().getDepthStatus());
        executionFeasibility.put("entryDriftStatus", input.rulePlan().getEntryDriftStatus());
        executionFeasibility.put("triggerStatus", input.rulePlan().getTriggerStatus());
        executionFeasibility.put("reason", input.rulePlan().getExecutionFeasibilityReason());
        executionFeasibility.put("observedAt", input.rulePlan().getExecutionFeasibilityObservedAt());
        executionFeasibility.put("freshUntil", input.rulePlan().getExecutionFeasibilityFreshUntil());
        executionFeasibility.put("sourceRefs", jsonNode(
                input.rulePlan().getExecutionFeasibilitySourceRefsJson()));
        executionFeasibility.put("sourceGateStatus", input.rulePlan().getSourceGateStatus());
        executionFeasibility.put("sourceGateComplete", input.rulePlan().getSourceGateComplete());
        facts.put("executionFeasibility", executionFeasibility);
        facts.put("accountRisk", accountRiskFacts(input.accountRiskSnapshot()));
        return facts;
    }

    private List<Map<String, Object>> evidenceFacts(List<EvidenceItemVO> evidence, String analysisId) {
        if (evidence == null) return List.of();
        return evidence.stream().filter(item -> item != null).limit(20).map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", item.getEvidenceType());
            value.put("evidenceId", item.getEvidenceId());
            value.put("analysisId", item.getAnalysisId());
            value.put("direction", item.getDirection());
            value.put("strength", item.getStrength());
            value.put("confidence", item.getConfidence());
            value.put("source", item.getSource());
            value.put("sourceReference", item.getSourceReference());
            value.put("sourceTraceId", item.getSourceTraceId());
            value.put("externalEventId", item.getExternalEventId());
            value.put("externalEventType", item.getExternalEventType());
            value.put("eventWindowStart", item.getEventWindowStart());
            value.put("eventWindowEnd", item.getEventWindowEnd());
            value.put("eventWindow", eventWindow(item));
            value.put("description", item.getDescription());
            value.put("currentValue", item.getCurrentValue());
            value.put("changeFromBaseline", item.getChangeFromBaseline());
            value.put("observedAt", item.getObservedAt());
            value.put("freshness", item.getFreshness());
            return value;
        }).toList();
    }

    private static String eventWindow(EvidenceItemVO item) {
        if (item == null || item.getEventWindowStart() == null || item.getEventWindowEnd() == null) {
            return null;
        }
        return item.getEventWindowStart() + "/" + item.getEventWindowEnd();
    }

    private List<Map<String, Object>> scoreFacts(List<ScoreItemVO> scores) {
        if (scores == null) return List.of();
        return scores.stream().filter(item -> item != null).limit(16).map(item -> {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("type", item.getScoreType());
            value.put("scoreId", item.getScoreId());
            value.put("value", item.getScoreValue());
            value.put("weight", item.getWeight());
            value.put("direction", item.getDirection());
            return value;
        }).toList();
    }

    private Map<String, Object> candidatePayload(ExecutionPlanCandidateDO candidate) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("direction", candidate.getCandidateDirection());
        value.put("planMode", candidate.getPlanMode());
        value.put("opportunityType", candidate.getOpportunityType());
        value.put("entryLogic", candidate.getEntryLogic());
        value.put("confidence", candidate.getConfidenceLevel());
        value.put("riskLevel", candidate.getRiskLevel());
        value.put("worthOpening", candidate.getWorthOpening());
        value.put("recommendedAction", candidate.getRecommendedAction());
        value.put("entryZone", candidate.getEntryZone());
        value.put("entrySource", candidate.getEntrySource());
        value.put("entryReason", candidate.getEntryReason());
        value.put("triggerCondition", candidate.getTriggerCondition());
        value.put("stopLogic", candidate.getStopLogic());
        value.put("stopLoss", candidate.getStopLoss());
        value.put("stopSource", candidate.getStopSource());
        value.put("stopReason", candidate.getStopReason());
        value.put("targetLogic", candidate.getTargetLogic());
        value.put("takeProfitRules", candidate.getTakeProfitRules());
        value.put("targetSource", candidate.getTargetSource());
        value.put("targetReason", candidate.getTargetReason());
        value.put("addPositionCondition", candidate.getAddPositionCondition());
        value.put("reducePositionCondition", candidate.getReducePositionCondition());
        value.put("abandonCondition", candidate.getAbandonCondition());
        value.put("leverageSuggestion", candidate.getLeverageSuggestion());
        value.put("positionSuggestion", candidate.getPositionSuggestion());
        value.put("riskExplanation", candidate.getRiskExplanation());
        value.put("invalidCondition", candidate.getInvalidCondition());
        value.put("invalidationSource", candidate.getInvalidationSource());
        value.put("invalidationReason", candidate.getInvalidationReason());
        value.put("expectedRiskReward", candidate.getExpectedRiskReward());
        value.put("expectedRiskRewardSource", candidate.getExpectedRiskRewardSource());
        value.put("expectedRiskRewardReason", candidate.getExpectedRiskRewardReason());
        value.put("validity", candidate.getValidity());
        value.put("summary", candidate.getSummary());
        return value;
    }

    private ExecutionPlanVO validatedPlan(DecisionChainBuildInput input,
                                          ExecutionPlanCandidateDO candidate,
                                          OpportunityTransitionResult opportunity,
                                          ConflictResolverResultDO conflict) {
        ExecutionPlanVO plan = copyPlan(input.rulePlan());
        plan.setPlanMode(conflict.getPlanModeAfter());
        plan.setRecommendedAction(candidate.getRecommendedAction());
        plan.setEntryZone(candidate.getEntryZone());
        plan.setStopLoss(candidate.getStopLoss());
        plan.setTakeProfitRules(candidate.getTakeProfitRules());
        plan.setLeverageSuggestion(candidate.getLeverageSuggestion());
        plan.setPositionSuggestion(candidate.getPositionSuggestion());
        plan.setInvalidCondition(candidate.getInvalidCondition());
        plan.setInvalidationSource(candidate.getInvalidationSource());
        plan.setInvalidationReason(candidate.getInvalidationReason());
        plan.setCandidateId(candidate.getCandidateId());
        plan.setOpportunityId(opportunity.opportunityId());
        plan.setResolverResultId(conflict.getResolverResultId());
        plan.setTraceId(candidate.getTraceId());
        plan.setChainStatus("FINAL_VALIDATED");
        plan.setRuleValidationStatus("PASS");
        plan.setRuleVetoReason(null);
        plan.setFinalizedAt(LocalDateTime.now());
        plan.setFinalPlan(true);
        copyFrozenFinalContract(plan, candidate, conflict);
        plan.setValidationResultId("validation-" + UUID.randomUUID());
        plan.setValidationReasons(List.of());
        plan.setRiskLimit(input.accountRiskSnapshot() == null
                ? null : input.accountRiskSnapshot().getMaxAllowedExposure());
        plan.setSourceStatus("VALID");
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
        plan.setValidationResultId("validation-" + UUID.randomUUID());
        plan.setValidationReasons(validation.reasons());
        plan.setFinalPlan(false);
        plan.setFinalizedAt(null);
        return plan;
    }

    private static void copyFrozenFinalContract(ExecutionPlanVO plan,
                                                ExecutionPlanCandidateDO candidate,
                                                ConflictResolverResultDO conflict) {
        plan.setAssetId(candidate.getAssetId());
        plan.setRuleVersion(candidate.getRuleVersion());
        plan.setRuleMarketBias(candidate.getRuleDirection());
        plan.setFinalMarketBias(conflict.getBiasAfter());
        plan.setCandidatePlanMode(candidate.getPlanMode());
        plan.setFinalPlanMode(conflict.getPlanModeAfter());
        plan.setBiasAdjustmentReason(conflict.getAdjustmentReason());
        plan.setPlanModeAdjustmentReason(conflict.getDowngradeReason());
        plan.setAdjustmentReason(conflict.getAdjustmentReason());
        plan.setDowngradeReason(conflict.getDowngradeReason());
        plan.setOpportunityType(candidate.getOpportunityType());
        plan.setEntryLogic(candidate.getEntryLogic());
        plan.setEntrySource(candidate.getEntrySource());
        plan.setEntryReason(candidate.getEntryReason());
        plan.setTriggerCondition(candidate.getTriggerCondition());
        plan.setStopLogic(candidate.getStopLogic());
        plan.setStopSource(candidate.getStopSource());
        plan.setStopReason(candidate.getStopReason());
        plan.setTargetLogic(candidate.getTargetLogic());
        plan.setTargetSource(candidate.getTargetSource());
        plan.setTargetReason(candidate.getTargetReason());
        plan.setAddPositionCondition(candidate.getAddPositionCondition());
        plan.setReducePositionCondition(candidate.getReducePositionCondition());
        plan.setAbandonCondition(candidate.getAbandonCondition());
        plan.setRiskExplanation(candidate.getRiskExplanation());
        plan.setLeverageLimit(candidate.getLeverageSuggestion());
        plan.setPositionLimit(candidate.getPositionSuggestion());
        plan.setExpectedRiskReward(candidate.getExpectedRiskReward());
        plan.setExpectedRiskRewardSource(candidate.getExpectedRiskRewardSource());
        plan.setExpectedRiskRewardReason(candidate.getExpectedRiskRewardReason());
        plan.setAccountRiskSnapshotId(candidate.getAccountRiskSnapshotId());
        plan.setAnalysisTimeframesJson(candidate.getAnalysisTimeframesJson());
        plan.setTriggerTimeframe(candidate.getTriggerTimeframe());
        plan.setValidFrom(candidate.getValidFrom());
        plan.setValidUntil(candidate.getValidUntil());
        plan.setHoldingHorizon(candidate.getHoldingHorizon());
        plan.setRevalidationRule(candidate.getRevalidationRule());
        plan.setDataQuality(candidate.getDataQuality());
        plan.setSourceRefsJson(candidate.getSourceRefsJson());
        plan.setEvidenceRefsJson(candidate.getEvidenceRefsJson());
        plan.setScoreRefsJson(candidate.getScoreRefsJson());
        plan.setSourceStatus("VALID");
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
        target.setInvalidationSource(source.getInvalidationSource());
        target.setInvalidationReason(source.getInvalidationReason());
        target.setLeverageSuggestion(source.getLeverageSuggestion());
        target.setPositionSuggestion(source.getPositionSuggestion());
        target.setAccountRiskJson(source.getAccountRiskJson());
        target.setExecutionFeasibilityStatus(source.getExecutionFeasibilityStatus());
        target.setSlippageStatus(source.getSlippageStatus());
        target.setDepthStatus(source.getDepthStatus());
        target.setEntryDriftStatus(source.getEntryDriftStatus());
        target.setTriggerStatus(source.getTriggerStatus());
        target.setExecutionFeasibilityReason(source.getExecutionFeasibilityReason());
        target.setExecutionFeasibilityObservedAt(source.getExecutionFeasibilityObservedAt());
        target.setExecutionFeasibilityFreshUntil(source.getExecutionFeasibilityFreshUntil());
        target.setExecutionFeasibilitySourceRefsJson(source.getExecutionFeasibilitySourceRefsJson());
        target.setNeedsRevalidation(source.getNeedsRevalidation());
        target.setRevalidationReason(source.getRevalidationReason());
        target.setDerivativesStatus(source.getDerivativesStatus());
        target.setDerivativesFreshness(source.getDerivativesFreshness());
        target.setDerivativesReasonCodes(source.getDerivativesReasonCodes());
        target.setDerivativesProviderDataTime(source.getDerivativesProviderDataTime());
        target.setDerivativesTraceId(source.getDerivativesTraceId());
        target.setAssetId(source.getAssetId());
        target.setRuleVersion(source.getRuleVersion());
        target.setRuleMarketBias(source.getRuleMarketBias());
        target.setFinalMarketBias(source.getFinalMarketBias());
        target.setCandidatePlanMode(source.getCandidatePlanMode());
        target.setFinalPlanMode(source.getFinalPlanMode());
        target.setBiasAdjustmentReason(source.getBiasAdjustmentReason());
        target.setPlanModeAdjustmentReason(source.getPlanModeAdjustmentReason());
        target.setAdjustmentReason(source.getAdjustmentReason());
        target.setDowngradeReason(source.getDowngradeReason());
        target.setOpportunityType(source.getOpportunityType());
        target.setEntryLogic(source.getEntryLogic());
        target.setEntrySource(source.getEntrySource());
        target.setEntryReason(source.getEntryReason());
        target.setTriggerCondition(source.getTriggerCondition());
        target.setStopLogic(source.getStopLogic());
        target.setStopSource(source.getStopSource());
        target.setStopReason(source.getStopReason());
        target.setTargetLogic(source.getTargetLogic());
        target.setTargetSource(source.getTargetSource());
        target.setTargetReason(source.getTargetReason());
        target.setRiskExplanation(source.getRiskExplanation());
        target.setLeverageLimit(source.getLeverageLimit());
        target.setPositionLimit(source.getPositionLimit());
        target.setRiskLimit(source.getRiskLimit());
        target.setExpectedRiskReward(source.getExpectedRiskReward());
        target.setExpectedRiskRewardSource(source.getExpectedRiskRewardSource());
        target.setExpectedRiskRewardReason(source.getExpectedRiskRewardReason());
        target.setAccountRiskSnapshotId(source.getAccountRiskSnapshotId());
        target.setAnalysisTimeframesJson(source.getAnalysisTimeframesJson());
        target.setTriggerTimeframe(source.getTriggerTimeframe());
        target.setValidFrom(source.getValidFrom());
        target.setValidUntil(source.getValidUntil());
        target.setHoldingHorizon(source.getHoldingHorizon());
        target.setRevalidationRule(source.getRevalidationRule());
        target.setDataQuality(source.getDataQuality());
        target.setSourceRefsJson(source.getSourceRefsJson());
        target.setEvidenceRefsJson(source.getEvidenceRefsJson());
        target.setScoreRefsJson(source.getScoreRefsJson());
        target.setValidationResultId(source.getValidationResultId());
        target.setValidationReasons(source.getValidationReasons());
        target.setSourceStatus(source.getSourceStatus());
        return target;
    }

    private void applyDecisionAdjustments(DecisionBundleVO decision,
                                          ConflictResolverResultDO conflict,
                                          RuleValidationResult validation) {
        decision.setConfidenceLevel(conflict.getConfidenceAfter());
        decision.setRiskLevel(conflict.getRiskAfter());
        decision.setAiPlanMode(conflict.getPlanModeAfter());
        decision.setCandidatePlanMode(conflict.getPlanModeBefore());
        decision.setFinalPlanMode(conflict.getPlanModeAfter());
        decision.setFinalMarketBias(conflict.getBiasAfter());
        decision.setBiasAdjustmentReason(conflict.getAdjustmentReason());
        decision.setPlanModeAdjustmentReason(conflict.getDowngradeReason());
        decision.setMarketBiasHierarchy(conflict.getBiasAfter());
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
                conflict.getBiasAfter(),
                conflict.getConfidenceAfter(),
                conflict.getRiskAfter(),
                conflict.getPlanModeAfter(),
                Boolean.TRUE.equals(candidate.getWorthOpening()),
                conflict.getConflictLevel(),
                conflict.getConflictScore(),
                conflict.getConfidenceAfter(),
                conflict.getRiskAfter(),
                conflict.getPlanModeAfter(),
                conflict.getConfusedDecision(),
                conflict.getDowngradeReason(),
                conflict.getAdjustmentReason(),
                conflict.getRecoveryCondition());
        decision.setAiRoleResults(aiRoleResultsCodec.serializeDecisionChain(
                input.analysisId(), input.traceId(), candidate.getRuleDirection(), roles, synthesis));
    }

    private void persistOpportunityProjection(DecisionChainBuildInput input,
                                              OpportunityTransitionResult opportunity,
                                              ConflictResolverResultDO conflict,
                                              RuleValidationResult validation) {
        Long poolItemId = assetPoolService.resolvePoolItemId(
                input.ownerType(), input.ownerId(), input.assetId(), input.symbol());
        if (poolItemId == null) {
            throw new IllegalStateException("Asset Pool source has no poolItemId");
        }
        Map<String, Object> ext = new LinkedHashMap<>();
        ext.put("schemaVersion", "FUNDAMENTAL_AI_V4_1_OPPORTUNITY_V1");
        ext.put("finalMarketBias", input.decision().getFinalMarketBias());
        ext.put("finalPlanMode", input.decision().getFinalPlanMode());
        ext.put("conflictLevel", conflict == null ? null : conflict.getConflictLevel());
        ext.put("ruleValidationPassed", validation != null && validation.passed());
        ext.put("dataQuality", input.dataQualityScore());
        assetStateService.recordOpportunityProjection(
                new OpportunityStateIdentity(input.ownerType(), input.ownerId(), input.assetId(),
                        input.symbol(), input.timeframe()),
                poolItemId,
                input.analysisId(),
                input.traceId(),
                input.ruleVersion(),
                opportunityScore(input.scores()),
                input.decision().getConfidenceLevel(),
                input.decision().getRiskLevel(),
                json(ext));
    }

    private static int opportunityScore(List<ScoreItemVO> scores) {
        if (scores == null || scores.isEmpty()) {
            throw new IllegalArgumentException("scores are required for opportunity projection");
        }
        return (int) Math.round(scores.stream()
                .filter(item -> item != null && item.getScoreValue() != null)
                .mapToDouble(ScoreItemVO::getScoreValue)
                .average()
                .orElseThrow(() -> new IllegalArgumentException(
                        "score values are required for opportunity projection")));
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

    private static java.math.BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || !value.isNumber() ? null : value.decimalValue();
    }

    private static String accountRiskState(TmAccountRiskSnapshotDO snapshot) {
        if (snapshot == null) return "UNAVAILABLE";
        if (!"VERIFIED".equals(snapshot.getSourceStatus())) return "INVALID";
        return hasText(snapshot.getAccountRiskStatus())
                ? snapshot.getAccountRiskStatus().trim() : "UNAVAILABLE";
    }

    private static boolean accountRiskInputReady(TmAccountRiskSnapshotDO snapshot) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        return snapshot != null
                && "VERIFIED".equals(snapshot.getSourceStatus())
                && snapshot.getObservedAt() != null
                && snapshot.getFreshUntil() != null
                && snapshot.getRiskAllowed() != null
                && hasText(snapshot.getAccountRiskStatus())
                && !now.isBefore(snapshot.getObservedAt())
                && now.isBefore(snapshot.getFreshUntil());
    }

    private static Map<String, Object> accountRiskFacts(TmAccountRiskSnapshotDO snapshot) {
        Map<String, Object> facts = new LinkedHashMap<>();
        if (snapshot == null) {
            facts.put("sourceStatus", "UNAVAILABLE");
            return facts;
        }
        facts.put("snapshotId", snapshot.getId());
        facts.put("ownerType", snapshot.getOwnerType());
        facts.put("accountRiskStatus", snapshot.getAccountRiskStatus());
        facts.put("riskLevel", snapshot.getRiskLevelSnapshot());
        facts.put("riskAllowed", snapshot.getRiskAllowed());
        facts.put("grossNotional", snapshot.getGrossNotional());
        facts.put("leverageRisk", snapshot.getLeverageRisk());
        facts.put("positionSizeRisk", snapshot.getPositionSizeRisk());
        facts.put("concentrationRisk", snapshot.getConcentrationRisk());
        facts.put("correlationRisk", snapshot.getCorrelationRisk());
        facts.put("drawdownOrVarRisk", snapshot.getDrawdownOrVarRisk());
        facts.put("aggregateRiskScore", snapshot.getAggregateRiskScore());
        facts.put("maxAllowedExposure", snapshot.getMaxAllowedExposure());
        facts.put("maxAllowedLeverage", snapshot.getMaxAllowedLeverage());
        facts.put("sourceStatus", snapshot.getSourceStatus());
        facts.put("observedAt", snapshot.getObservedAt());
        facts.put("freshUntil", snapshot.getFreshUntil());
        return facts;
    }

    private static String rulePlanMode(DecisionChainBuildInput input) {
        if (input.decision().isDirectionalPushBlocked()) return PlanModeEnum.BLOCKED.name();
        if (Boolean.TRUE.equals(input.decision().getIsWorthOpening())
                && Boolean.TRUE.equals(input.rulePlan().getSourceGateComplete())
                && ExecutionFeasibilityContract.assess(input.rulePlan()).allowed()) {
            return PlanModeEnum.CONFIRMATION.name();
        }
        if (Boolean.TRUE.equals(input.decision().getIsWorthOpening())) return PlanModeEnum.PREPARATION.name();
        if ("HIGH".equalsIgnoreCase(input.decision().getRuleRisk())
                || "EXTREME".equalsIgnoreCase(input.decision().getRuleRisk())) return PlanModeEnum.REDUCED.name();
        return PlanModeEnum.OBSERVATION.name();
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

    private static String requiredRuleText(String value, String failureCode) {
        if (!hasText(value)) throw new IllegalStateException(failureCode);
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String moreRestrictiveMode(String value, PlanModeEnum minimum) {
        try {
            PlanModeEnum current = PlanModeEnum.require(value);
            return (current.ordinal() < minimum.ordinal() ? minimum : current).name();
        } catch (RuntimeException ignored) {
            return PlanModeEnum.BLOCKED.name();
        }
    }

    private static String downgradeConfidence(String value) {
        return "HIGH".equalsIgnoreCase(value) ? "MEDIUM" : "LOW";
    }

    private static String opportunityId(Map<String, Object> facts) {
        if (facts == null || !(facts.get("opportunity") instanceof Map<?, ?> opportunity)) {
            return null;
        }
        Object value = opportunity.get("opportunityId");
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
