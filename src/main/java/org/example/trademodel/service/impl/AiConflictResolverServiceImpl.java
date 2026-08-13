package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.entity.ConflictResolverResultDO;
import org.example.trademodel.entity.ExecutionPlanCandidateDO;
import org.example.trademodel.enums.AiConflictLevelEnum;
import org.example.trademodel.enums.MarketBiasEnum;
import org.example.trademodel.enums.PlanModeEnum;
import org.example.trademodel.service.AiConflictResolverService;
import org.example.trademodel.service.AiConflictResult;
import org.example.trademodel.service.DecisionContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class AiConflictResolverServiceImpl implements AiConflictResolverService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final FundamentalAiV41Properties v41Properties;

    public AiConflictResolverServiceImpl(FundamentalAiV41Properties v41Properties) {
        this.v41Properties = v41Properties;
    }

    @Override
    public AiConflictResult resolve(DecisionContext context) {
        if (context == null || !context.isHasRuleBaseOutput() || isBlank(context.getRuleMarketBias())) {
            throw new IllegalStateException("规则层必须先产出基础方向");
        }

        int successfulProviderCount = clampCount(context.getAiSuccessfulProviderCount());
        int objectionCount = Math.min(successfulProviderCount, clampCount(context.getAiObjectionCount()));
        int supportCount = Math.min(successfulProviderCount - objectionCount,
                clampCount(context.getAiSupportCount()));
        context.setAiObjectionCount(objectionCount);
        context.setAiSupportCount(supportCount);

        if (successfulProviderCount == 0 || supportCount + objectionCount == 0) {
            context.setAiConflictScore(0);
            return result(context, null, context.getRuleConfidenceLevel(),
                    "UNCHANGED", null, 0, 0, 0);
        }

        int directionConflict = calculateDirectionConflict(context, objectionCount);
        int riskConflict = calculateRiskConflict(context);
        int planConflict = calculatePlanConflict(context);
        int providerConflict = providerConflictContribution(context);

        int aiConflictScore = Math.min(100, directionConflict + riskConflict + planConflict + providerConflict);
        if (objectionCount == 1) {
            aiConflictScore = Math.min(aiConflictScore, 35);
        }
        context.setAiConflictScore(aiConflictScore);

        if (aiConflictScore <= 20 && objectionCount == 0) {
            return result(context, AiConflictLevelEnum.LEVEL_1_CONSISTENT, context.getRuleConfidenceLevel(),
                    "UNCHANGED", PlanModeEnum.CONFIRMATION.name(), aiConflictScore, objectionCount, 0);
        } else if (aiConflictScore <= 45) {
            return result(context, AiConflictLevelEnum.LEVEL_2_MINOR_DISAGREEMENT, downgradeConfidence(context, 1),
                    "SLIGHTLY_RAISED", PlanModeEnum.REDUCED.name(), aiConflictScore, objectionCount, aiConflictScore);
        } else if (aiConflictScore <= 70) {
            return result(context, AiConflictLevelEnum.LEVEL_3_SIGNIFICANT_DISAGREEMENT, downgradeConfidence(context, 2),
                    "RAISED", PlanModeEnum.OBSERVATION.name(), aiConflictScore, objectionCount, aiConflictScore);
        } else {
            return result(context, AiConflictLevelEnum.LEVEL_4_EXTREME_CONFLICT, downgradeConfidence(context, 2),
                    "HIGH", PlanModeEnum.BLOCKED.name(), aiConflictScore, objectionCount, aiConflictScore);
        }
    }

    @Override
    public ConflictResolverResultDO resolveDecisionChain(ExecutionPlanCandidateDO candidate,
                                                         String geminiReviewJson,
                                                         String grokChallengeJson,
                                                         Integer dataQualityScore,
                                                         Integer confusedScore,
                                                         String accountRiskState) {
        if (candidate == null || isBlank(candidate.getRuleDirection())) {
            throw new IllegalStateException("规则层必须先产出基础方向和 Candidate");
        }
        JsonNode gemini = parse(geminiReviewJson);
        JsonNode grok = parse(grokChallengeJson);
        int score = 0;
        int requestedConfidenceDowngrade = 0;
        int requestedRiskRaise = 0;
        int requestedPlanDowngrade = 0;
        int aiConflictScore = 0;
        boolean requestedBlock = false;
        List<String> reasons = new ArrayList<>();

        if (availableAny(gemini, "reviewResult", "verdict")) {
            String geminiVerdict = upperText(gemini, gemini.hasNonNull("reviewResult")
                    ? "reviewResult" : "verdict", "APPROVE");
            int verdictContribution = switch (geminiVerdict) {
                case "REJECT", "REJECT_CANDIDATE" -> 45;
                case "DOWNGRADE" -> 25;
                case "RISK_WARNING" -> 10;
                default -> 0;
            };
            int conflictContribution = conflictContribution(upperText(
                    gemini, "conflictLevel", AiConflictLevelEnum.LEVEL_1_CONSISTENT.name()));
            score += verdictContribution + conflictContribution;
            aiConflictScore += verdictContribution + conflictContribution;
            requestedConfidenceDowngrade = adjustmentLevels(
                    upperText(gemini, "confidenceAdjustment", "UNCHANGED"), "DOWNGRADE");
            requestedRiskRaise = adjustmentLevels(
                    upperText(gemini, "riskAdjustment", "UNCHANGED"), "RAISE");
            String planAdjustment = upperText(gemini, "planModeAdjustment", "UNCHANGED");
            if (gemini.has("downgradeSuggestion") && gemini.path("downgradeSuggestion").isObject()) {
                String after = upperText(gemini.path("downgradeSuggestion"), "after", candidate.getPlanMode());
                requestedPlanDowngrade = Math.max(requestedPlanDowngrade,
                        modeDistance(candidate.getPlanMode(), after));
            }
            requestedPlanDowngrade = Math.max(requestedPlanDowngrade,
                    adjustmentLevels(planAdjustment, "DOWNGRADE"));
            requestedBlock = "BLOCKED".equals(planAdjustment);
            if (!"APPROVE".equals(geminiVerdict)) reasons.add("GEMINI_" + geminiVerdict);
        } else {
            reasons.add("GEMINI_UNAVAILABLE_RULE_FALLBACK");
        }

        if (availableAny(grok, "conflictLevel", "challengeLevel")) {
            String challenge = upperText(
                    grok, grok.hasNonNull("conflictLevel") ? "conflictLevel" : "challengeLevel",
                    AiConflictLevelEnum.LEVEL_1_CONSISTENT.name());
            int challengeContribution = conflictContribution(challenge);
            score += challengeContribution;
            aiConflictScore += challengeContribution;
            if (grok.path("majorCounterEvidence").asBoolean(false)) {
                score += 20;
                aiConflictScore += 20;
                reasons.add("GROK_MAJOR_COUNTER_EVIDENCE");
            }
            String grokPlanImpact = upperText(grok, "planModeImpact", "UNCHANGED");
            requestedPlanDowngrade = Math.max(requestedPlanDowngrade,
                    adjustmentLevels(grokPlanImpact, "DOWNGRADE"));
            requestedBlock = requestedBlock || "BLOCKED".equals(grokPlanImpact);
            requestedRiskRaise = Math.max(requestedRiskRaise,
                    adjustmentLevels(upperText(grok, "riskAdjustment", "UNCHANGED"), "RAISE"));
        } else {
            reasons.add("GROK_UNAVAILABLE_RULE_FALLBACK");
        }
        if (dataQualityScore == null
                || dataQualityScore < v41Properties.getAiGate().getCircuitBreakerScore()) {
            score += 20;
            reasons.add("DATA_QUALITY_BLOCKED");
        }
        if (accountRiskState != null && accountRiskState.toUpperCase(Locale.ROOT).contains("BLOCK")) {
            score += 20;
            reasons.add("ACCOUNT_RISK_BLOCKED");
        }
        boolean independentConfused = confusedScore != null
                && confusedScore >= v41Properties.getOpportunityState().getConfusedEnterThreshold();
        if (independentConfused) reasons.add("INDEPENDENT_CONFUSED_THRESHOLD");
        score = Math.max(0, Math.min(100, score));

        String ruleVeto = permittedBiasAdjustment(candidate.getRuleDirection(), candidate.getCandidateDirection())
                ? null : "CANDIDATE_DIRECTION_CROSSES_RULE_FAMILY";
        if (ruleVeto != null) {
            score = Math.max(score, 90);
            reasons.add(ruleVeto);
        }
        AiConflictLevelEnum conflictLevel = conflictLevel(score);
        boolean confused = independentConfused
                || conflictLevel == AiConflictLevelEnum.LEVEL_4_EXTREME_CONFLICT;
        int scoreDowngrades = score <= 15 ? 0 : score <= 40 ? 1 : score <= 70 ? 2 : 4;
        int confidenceDowngrades = Math.max(scoreDowngrades, requestedConfidenceDowngrade);
        int riskRaises = Math.max(scoreDowngrades, requestedRiskRaise);
        int planDowngrades = Math.max(scoreDowngrades, requestedPlanDowngrade);
        String planModeAfter = downgradeMode(candidate.getPlanMode(), planDowngrades);
        if (conflictLevel == AiConflictLevelEnum.LEVEL_2_MINOR_DISAGREEMENT) {
            planModeAfter = downgradeToAtLeast(planModeAfter, PlanModeEnum.PREPARATION);
        }
        if (conflictLevel == AiConflictLevelEnum.LEVEL_3_SIGNIFICANT_DISAGREEMENT) {
            planModeAfter = downgradeToAtLeast(planModeAfter, PlanModeEnum.OBSERVATION);
        }
        if (requestedBlock && !confused && ruleVeto == null) {
            planModeAfter = downgradeToAtLeast(planModeAfter, PlanModeEnum.OBSERVATION);
            reasons.add("AI_BLOCK_REQUEST_PRESERVED_AS_OBSERVATION");
        }
        if (confused || ruleVeto != null) planModeAfter = PlanModeEnum.BLOCKED.name();

        String biasBefore = normalize(candidate.getRuleDirection());
        String biasAfter = ruleVeto == null ? normalize(candidate.getCandidateDirection()) : biasBefore;
        String adjustmentReason = biasBefore.equals(biasAfter) ? null
                : defaultText(candidate.getBiasAdjustmentReason(), "GPT_SAME_FAMILY_INTENSITY_DOWNGRADE");

        ConflictResolverResultDO result = new ConflictResolverResultDO();
        result.setResolverResultId("resolver-" + UUID.randomUUID());
        result.setCandidateId(candidate.getCandidateId());
        result.setAnalysisId(candidate.getAnalysisId());
        result.setTraceId(candidate.getTraceId());
        result.setRuleDirection(candidate.getRuleDirection());
        result.setRuleConfidence(candidate.getRuleConfidence());
        result.setRuleRisk(candidate.getRuleRisk());
        result.setRulePlanMode(candidate.getRulePlanMode());
        result.setRuleCanExecute(candidate.getRuleCanExecute());
        result.setDataQualityScore(dataQualityScore);
        result.setConfusedScore(confusedScore);
        result.setAccountRiskState(accountRiskState);
        result.setGeminiReviewJson(nonBlankJson(geminiReviewJson));
        result.setGrokChallengeJson(nonBlankJson(grokChallengeJson));
        result.setConflictLevel(conflictLevel.name());
        result.setConflictScore(score);
        result.setPlanModeBefore(candidate.getPlanMode());
        result.setPlanModeAfter(planModeAfter);
        result.setConfidenceBefore(candidate.getConfidenceLevel());
        result.setConfidenceAfter(downgradeConfidence(candidate.getConfidenceLevel(), confidenceDowngrades));
        result.setRiskBefore(candidate.getRiskLevel());
        result.setRiskAfter(raiseRisk(candidate.getRiskLevel(), riskRaises));
        result.setBiasBefore(biasBefore);
        result.setBiasAfter(biasAfter);
        result.setAdjustmentReason(adjustmentReason);
        result.setDowngradeReason(reasons.isEmpty() ? null : String.join(";", reasons));
        result.setRecoveryCondition(recoveryCondition(gemini, grok, conflictLevel));
        result.setConfusedDecision(confused);
        result.setRuleVetoReason(ruleVeto);
        // A cross-family Candidate is vetoed and the resolver restores the rule-layer bias.
        // The audit flag describes the persisted resolver outcome, not the rejected AI attempt.
        result.setRuleDirectionPreserved(true);
        result.setCreatedAt(LocalDateTime.now());
        return result;
    }

    /** 来源：规则层 1m 方向 vs 综合裁决方向、多周期是否对齐（由 DecisionEngineService 写入 context） */
    private int calculateDirectionConflict(DecisionContext ctx, int objectionCount) {
        int c;
        if (objectionCount <= 0) {
            c = 0;
        } else if (objectionCount == 1) {
            c = 18;
        } else if (objectionCount == 2) {
            c = 42;
        } else {
            c = 68;
        }
        if (!ctx.isMultiTimeframeAligned()) {
            c += 14;
        }
        return Math.min(82, c);
    }

    /** 来源：本 run 风险档位 riskTier */
    private int calculateRiskConflict(DecisionContext ctx) {
        if ("HIGH".equalsIgnoreCase(ctx.getRiskTier())) {
            return 18;
        }
        if ("MEDIUM".equalsIgnoreCase(ctx.getRiskTier())) {
            return 8;
        }
        return 0;
    }

    /** 来源：本 run 是否 worthOpening 与计划张力 */
    private int calculatePlanConflict(DecisionContext ctx) {
        if (Boolean.TRUE.equals(ctx.getWorthOpening())) {
            return 0;
        }
        return 6;
    }

    private int providerConflictContribution(DecisionContext ctx) {
        Integer contribution = ctx.getAiProviderConflictContribution();
        if (contribution == null) {
            return 0;
        }
        return Math.max(0, Math.min(25, contribution));
    }

    private int clampCount(Integer value) {
        return value == null ? 0 : Math.max(0, Math.min(3, value));
    }

    private AiConflictResult result(DecisionContext context, AiConflictLevelEnum level, String adjustedConfidence,
                                    String riskAdjustment, String planMode, int aiConflictScore,
                                    int aiObjectionCount, int confusedContribution) {
        return new AiConflictResult(
                level,
                context.getRuleMarketBias(),
                adjustedConfidence,
                riskAdjustment,
                planMode,
                aiConflictScore,
                aiObjectionCount,
                aiObjectionCount == 1,
                confusedContribution
        );
    }

    private String downgradeConfidence(DecisionContext ctx, int levels) {
        String confidence = ctx.getRuleConfidenceLevel();
        if (confidence == null || confidence.isBlank()) {
            confidence = "MEDIUM";
        }
        String normalized = confidence.trim().toUpperCase();
        for (int i = 0; i < levels; i++) {
            if ("HIGH".equals(normalized)) {
                normalized = "MEDIUM";
            } else {
                normalized = "LOW";
            }
        }
        return normalized;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(nonBlankJson(json));
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private static boolean available(JsonNode node, String requiredField) {
        return node != null && node.isObject() && !node.path("fallback").asBoolean(false)
                && node.hasNonNull(requiredField) && !node.path(requiredField).asText("").isBlank();
    }

    private static boolean availableAny(JsonNode node, String... requiredFields) {
        if (node == null || !node.isObject() || node.path("fallback").asBoolean(false)) return false;
        for (String field : requiredFields) {
            if (node.hasNonNull(field) && !node.path(field).asText("").isBlank()) return true;
        }
        return false;
    }

    private static int conflictContribution(String level) {
        return switch (normalize(level)) {
            case "LEVEL_4_EXTREME_CONFLICT" -> 45;
            case "LEVEL_3_SIGNIFICANT_DISAGREEMENT" -> 30;
            case "LEVEL_2_MINOR_DISAGREEMENT" -> 12;
            default -> 0;
        };
    }

    private static AiConflictLevelEnum conflictLevel(int score) {
        if (score <= 15) return AiConflictLevelEnum.LEVEL_1_CONSISTENT;
        if (score <= 40) return AiConflictLevelEnum.LEVEL_2_MINOR_DISAGREEMENT;
        if (score <= 70) return AiConflictLevelEnum.LEVEL_3_SIGNIFICANT_DISAGREEMENT;
        return AiConflictLevelEnum.LEVEL_4_EXTREME_CONFLICT;
    }

    private static int adjustmentLevels(String adjustment, String prefix) {
        String normalized = normalize(adjustment);
        if ((prefix + "_TWO").equals(normalized)) return 2;
        if ((prefix + "_ONE").equals(normalized)) return 1;
        return 0;
    }

    private static int riskDistance(String before, String after) {
        List<String> values = List.of("LOW", "MEDIUM", "HIGH", "EXTREME");
        int beforeIndex = values.indexOf(normalize(before));
        int afterIndex = values.indexOf(normalize(after));
        if (beforeIndex < 0 || afterIndex < 0) return 0;
        return Math.max(0, afterIndex - beforeIndex);
    }

    private static String upperText(JsonNode node, String field, String fallback) {
        String value = node == null ? null : node.path(field).asText(null);
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase(Locale.ROOT);
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

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String downgradeMode(String mode, int levels) {
        try {
            return PlanModeEnum.require(mode).downgrade(levels).name();
        } catch (RuntimeException ignored) {
            return PlanModeEnum.BLOCKED.name();
        }
    }

    private static int modeDistance(String before, String after) {
        try {
            PlanModeEnum from = PlanModeEnum.require(before);
            PlanModeEnum to = PlanModeEnum.require(after);
            return Math.max(0, to.ordinal() - from.ordinal());
        } catch (RuntimeException ignored) {
            return 0;
        }
    }

    private static String downgradeToAtLeast(String value, PlanModeEnum minimum) {
        try {
            PlanModeEnum current = PlanModeEnum.require(value);
            return (current.ordinal() < minimum.ordinal() ? minimum : current).name();
        } catch (RuntimeException ignored) {
            return PlanModeEnum.BLOCKED.name();
        }
    }

    private static String recoveryCondition(JsonNode gemini, JsonNode grok,
                                            AiConflictLevelEnum conflictLevel) {
        String geminiRecovery = gemini == null ? null
                : gemini.path("downgradeSuggestion").path("recoveryCondition").asText(null);
        if (geminiRecovery != null && !geminiRecovery.isBlank()) return geminiRecovery.trim();
        String grokRecovery = grok == null ? null : grok.path("recoveryCondition").asText(null);
        if (grokRecovery != null && !grokRecovery.isBlank()) return grokRecovery.trim();
        return conflictLevel == AiConflictLevelEnum.LEVEL_1_CONSISTENT
                ? null : "REQUIRES_NEW_VERIFIED_ANALYSIS_AND_RULE_REVALIDATION";
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String downgradeConfidence(String confidence, int levels) {
        List<String> values = List.of("HIGH", "MEDIUM", "LOW");
        int index = values.indexOf(normalize(confidence));
        if (index < 0) index = 1;
        return values.get(Math.min(values.size() - 1, index + Math.min(2, Math.max(0, levels))));
    }

    private static String raiseRisk(String risk, int levels) {
        List<String> values = List.of("LOW", "MEDIUM", "HIGH", "EXTREME");
        int index = values.indexOf(normalize(risk));
        if (index < 0) index = 2;
        return values.get(Math.min(values.size() - 1, index + Math.min(3, Math.max(0, levels))));
    }

    private static String nonBlankJson(String value) {
        return value == null || value.isBlank() ? "{}" : value;
    }
}
