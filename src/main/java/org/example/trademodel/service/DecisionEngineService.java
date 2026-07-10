package org.example.trademodel.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

/**
 * V3 决策引擎 - 规则层基础方向 + AI review-only 编排（真实K线 + 多时间框架）。
 * AI 仅能支持 / 质疑 / 弃权，并通过冲突通道降级，不能覆盖规则层方向。
 *
 * <p>本 run 将 {@link DecisionContext} 接入 {@link AiConflictResolverService}、{@link ConfusedStateService}、
 * {@link AssetStateService}，供 {@link org.example.trademodel.service.impl.AnalysisAssemblerServiceImpl} 落库。</p>
 */
@Service
public class DecisionEngineService {

    private static final Logger logger = LoggerFactory.getLogger(DecisionEngineService.class);

    private final DecisionOhlcvSnapshotSource ohlcvSnapshotSource;
    private final AiConflictResolverService aiConflictResolverService;
    private final ConfusedStateService confusedStateService;
    private final AssetStateService assetStateService;
    private final RuleConfigService ruleConfigService;
    private final AiDecisionOrchestratorService aiDecisionOrchestratorService;
    private final AiRoleResultsCodec aiRoleResultsCodec;

    // ========= 最小规则键集合（仅限本阶段允许 keys） =========
    private static final String KEY_WORTH_OPENING_MIN_SCORE = "decision.worth_opening_min_score";
    private static final int DEFAULT_WORTH_OPENING_MIN_SCORE = 72;

    private static final String KEY_CONFIDENCE_HIGH_MIN_SCORE = "decision.confidence.high_min_score";
    private static final int DEFAULT_CONFIDENCE_HIGH_MIN_SCORE = 88;

    private static final String KEY_CONFIDENCE_MEDIUM_MIN_SCORE = "decision.confidence.medium_min_score";
    private static final int DEFAULT_CONFIDENCE_MEDIUM_MIN_SCORE = 75;

    private static final String KEY_RISK_TIER_LOW_MIN_SCORE = "decision.risk_tier.low_min_score";
    private static final int DEFAULT_RISK_TIER_LOW_MIN_SCORE = 80;

    private static final String KEY_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW = "decision.risk_level.high_final_score_below";
    private static final int DEFAULT_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW = 45;

    private static final String KEY_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE = "decision.action_priority.high_min_score_exclusive";
    private static final int DEFAULT_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE = 85;
    private static final int MIN_DATA_QUALITY_SCORE_FOR_OPENING = 60;
    private static final int MIN_TREND_STRUCTURE_SCORE_FOR_OPENING = 50;

    public DecisionEngineService(DecisionOhlcvSnapshotSource ohlcvSnapshotSource,
                                 AiConflictResolverService aiConflictResolverService,
                                 ConfusedStateService confusedStateService,
                                 AssetStateService assetStateService,
                                 RuleConfigService ruleConfigService) {
        this(ohlcvSnapshotSource, aiConflictResolverService, confusedStateService,
                assetStateService, ruleConfigService, null, new AiRoleResultsCodec(new ObjectMapper()));
    }

    public DecisionEngineService(DecisionOhlcvSnapshotSource ohlcvSnapshotSource,
                                 AiConflictResolverService aiConflictResolverService,
                                 ConfusedStateService confusedStateService,
                                 AssetStateService assetStateService,
                                 RuleConfigService ruleConfigService,
                                 AiDecisionOrchestratorService aiDecisionOrchestratorService) {
        this(ohlcvSnapshotSource, aiConflictResolverService, confusedStateService, assetStateService,
                ruleConfigService, aiDecisionOrchestratorService,
                new AiRoleResultsCodec(new ObjectMapper()));
    }

    @Autowired
    public DecisionEngineService(DecisionOhlcvSnapshotSource ohlcvSnapshotSource,
                                 AiConflictResolverService aiConflictResolverService,
                                 ConfusedStateService confusedStateService,
                                 AssetStateService assetStateService,
                                 RuleConfigService ruleConfigService,
                                 AiDecisionOrchestratorService aiDecisionOrchestratorService,
                                 AiRoleResultsCodec aiRoleResultsCodec) {
        this.ohlcvSnapshotSource = ohlcvSnapshotSource;
        this.aiConflictResolverService = aiConflictResolverService;
        this.confusedStateService = confusedStateService;
        this.assetStateService = assetStateService;
        this.ruleConfigService = ruleConfigService;
        this.aiDecisionOrchestratorService = aiDecisionOrchestratorService;
        this.aiRoleResultsCodec = aiRoleResultsCodec != null
                ? aiRoleResultsCodec
                : new AiRoleResultsCodec(new ObjectMapper());
        logger.info("DecisionEngineService V3 (rule-layer direction + AI review-only orchestrator + real klines + multiTF) initialized successfully");
    }

    public DecisionBundleVO makeDecision(String symbol, String timeframe, String analysisId) {
        return makeDecision(symbol, timeframe, analysisId, null, null);
    }

    public DecisionBundleVO makeDecision(String symbol, String timeframe, String analysisId, Integer dataQualityScore) {
        return makeDecision(symbol, timeframe, analysisId, dataQualityScore, null);
    }

    public DecisionBundleVO makeDecision(String symbol, String timeframe, String analysisId,
                                         Integer dataQualityScore, Integer trendStructureScore) {
        return makeDecision(symbol, timeframe, analysisId, dataQualityScore, trendStructureScore, null);
    }

    public DecisionBundleVO makeDecision(String symbol, String timeframe, String analysisId,
                                         Integer dataQualityScore, Integer trendStructureScore,
                                         EventImpactInputVO externalContextInput) {
        String decisionId = "dec-" + Instant.now().toEpochMilli();
        logger.info("[AI决策] === 开始为 {} {} analysisId={} 生成决策 ===", symbol, timeframe, analysisId);

        try {
            Map<String, RuleConfigDO> ruleMap = ruleConfigService != null
                    ? ruleConfigService.getRuleConfigMap()
                    : null;
            String ruleVersion = ruleConfigService != null
                    ? ruleConfigService.resolveActiveRuleVersion()
                    : "v1.0";

            // 缺失/禁用/不可解析全部回退默认值，避免配置缺失导致行为崩掉
            int worthOpeningMinScore = getInt(ruleMap, KEY_WORTH_OPENING_MIN_SCORE, DEFAULT_WORTH_OPENING_MIN_SCORE);
            int confidenceHighMinScore = getInt(ruleMap, KEY_CONFIDENCE_HIGH_MIN_SCORE, DEFAULT_CONFIDENCE_HIGH_MIN_SCORE);
            int confidenceMediumMinScore = getInt(ruleMap, KEY_CONFIDENCE_MEDIUM_MIN_SCORE, DEFAULT_CONFIDENCE_MEDIUM_MIN_SCORE);
            int riskTierLowMinScore = getInt(ruleMap, KEY_RISK_TIER_LOW_MIN_SCORE, DEFAULT_RISK_TIER_LOW_MIN_SCORE);
            int highFinalScoreBelow = getInt(ruleMap, KEY_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW,
                    DEFAULT_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW);
            int actionPriorityHighMinScoreExclusive = getInt(ruleMap,
                    KEY_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE,
                    DEFAULT_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE);

            // ==================== 1. 权威落库 K 线快照（同一 run trace） ====================
            String marketTraceId = analysisId == null || analysisId.isBlank() ? decisionId : analysisId;
            List<String[]> klines5m = ohlcvSnapshotSource.readClosedBars(symbol, "5m", 3, marketTraceId);
            List<String[]> klines15m = ohlcvSnapshotSource.readClosedBars(symbol, "15m", 3, marketTraceId);
            List<String[]> klines1h = ohlcvSnapshotSource.readClosedBars(symbol, "1h", 3, marketTraceId);
            List<String[]> klines4h = ohlcvSnapshotSource.readClosedBars(symbol, "4h", 3, marketTraceId);

            // 最后一根 K 线判断涨跌（close > open = 看涨）
            boolean isBullish5m = !klines5m.isEmpty() && 
                Double.parseDouble(klines5m.get(klines5m.size()-1)[4]) > 
                Double.parseDouble(klines5m.get(klines5m.size()-1)[1]);
            boolean isBullish15m = bullish(klines15m);
            boolean isBullish1h = bullish(klines1h);
            boolean isBullish4h = bullish(klines4h);

            boolean multiTfConvergence = isBullish5m == isBullish15m
                    && isBullish5m == isBullish1h && isBullish5m == isBullish4h;
            int convergenceScore = multiTfConvergence ? 15 : -10;

            // ==================== 2. 规则层基础方向 + AI review-only 编排 ====================
            int baseScore = isBullish5m ? 82 : 58;
            int finalScore = baseScore + convergenceScore;

            String ruleMarketBias = isBullish5m ? "BULLISH" : "BEARISH";
            String confidenceLevel = finalScore >= confidenceHighMinScore ? "HIGH" :
                    (finalScore >= confidenceMediumMinScore ? "MEDIUM" : "LOW");
            boolean worthOpening = finalScore >= worthOpeningMinScore;
            if (trendStructureScore != null && trendStructureScore < MIN_TREND_STRUCTURE_SCORE_FOR_OPENING) {
                worthOpening = false;
            }
            if (dataQualityScore != null && dataQualityScore < MIN_DATA_QUALITY_SCORE_FOR_OPENING) {
                worthOpening = false;
            }
            boolean externalContextBlocked = isEffectiveExternalBlocked(externalContextInput);
            boolean effectiveWorthOpening = worthOpening && !externalContextBlocked;
            String riskTier = finalScore >= riskTierLowMinScore ? "LOW" : "MEDIUM";

            // ==================== 3. 决策上下文：冲突 / 困惑 / 快照（本 run K 线事实） ====================
            DecisionContext ctx = new DecisionContext();
            ctx.setSymbol(symbol);
            ctx.setRuleMarketBias(ruleMarketBias);
            ctx.setRuleConfidenceLevel(confidenceLevel);
            ctx.setHasRuleBaseOutput(true);
            ctx.setMultiTimeframeAligned(multiTfConvergence);
            ctx.setRiskTier(riskTier);
            ctx.setDataQualityScore(dataQualityScore);
            ctx.setWorthOpening(effectiveWorthOpening);
            if (externalContextInput != null) {
                ctx.setExternalContextRiskLevel(externalContextInput.getExternalContextRiskLevel());
                ctx.setExternalContextBlocked(externalContextInput.getExternalContextBlocked());
                ctx.setExternalContextSourceHealth(externalContextInput.getExternalContextSourceHealth());
            }
            ctx.setDriverConflictScore(multiTfConvergence ? 18 : 48);
            ctx.setExecutionInstabilityScore(26);
            ctx.setMicrostructureTrapScore(multiTfConvergence ? 22 : 38);
            ctx.setCauseEffectDivergenceScore(multiTfConvergence ? 12 : 40);
            ctx.setConsecutiveLowConfusedCount(0);

            AiOrchestratorResult aiReview = runAiReview(symbol, timeframe, analysisId, decisionId,
                    ruleMarketBias, confidenceLevel, riskTier, effectiveWorthOpening,
                    dataQualityScore, trendStructureScore, multiTfConvergence,
                    externalContextInput, baseScore, convergenceScore, finalScore,
                    isBullish5m, isBullish4h, externalContextBlocked);
            ctx.setGptConsistentWithRule(aiReview.isGptConsistentWithRule());
            ctx.setGeminiConsistentWithRule(aiReview.isGeminiConsistentWithRule());
            ctx.setGrokConsistentWithRule(aiReview.isGrokConsistentWithRule());
            ctx.setAiProviderConflictContribution(aiReview.getConflictContribution());
            ctx.setAiOrchestrationMode(aiReview.getOrchestrationMode().name());
            ctx.setAiOrchestrationSummary(aiReview.toSanitizedSummary());

            AiConflictResult conflict = aiConflictResolverService.resolve(ctx);
            ctx.setAiConflictScore(conflict.getConfusedContribution());
            ConfusedResult confused = confusedStateService.calculateConfused(symbol, ctx);

            AssetStateEnum previousState = parseAssetState(confused.getPreviousState(), AssetStateEnum.OBSERVING);
            AssetStateEnum syntheticState = parseAssetState(confused.getNextState(),
                    effectiveWorthOpening ? AssetStateEnum.CANDIDATE : AssetStateEnum.OBSERVING);
            AssetStateEnum finalAssetState = failClosedExternalState(syntheticState, externalContextBlocked);
            String snapshot = assetStateService.buildSnapshotAtDecision(
                    symbol,
                    analysisId != null ? analysisId : "",
                    previousState,
                    finalAssetState,
                    confused.getConfusedScore(),
                    confused.getConfusedLowStreak(),
                    confused.isDirectionalPushBlocked(),
                    multiTfConvergence);

            String riskLevelLabel;
            if (confused.getConfusedScore() >= ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD
                    || finalScore < highFinalScoreBelow
                    || externalContextBlocked
                    || "HIGH".equalsIgnoreCase(conflict.getRiskAdjustment())) {
                riskLevelLabel = "HIGH";
            } else if (finalScore >= riskTierLowMinScore) {
                riskLevelLabel = "LOW";
            } else {
                riskLevelLabel = "MEDIUM";
            }

            String multiTfLabel = multiTfConvergence ? "STRONG" : "WEAK";
            String reviewJson = ReviewReasonsBuilder.toJsonArray(
                    riskLevelLabel,
                    multiTfLabel,
                    conflict.getLevel(),
                    conflict.getAiConflictScore(),
                    dataQualityScore,
                    trendStructureScore);

            // Push 快照专用：使用正式 5m 主周期边界，不使用 1m 作为执行计划失效条件。
            BigDecimal pushTriggerPrice = null;
            LocalDateTime pushExpiresAt = LocalDateTime.now().plusHours(24);
            BigDecimal pushInvalidPriceBelow = null;
            BigDecimal pushInvalidPriceAbove = null;
            String pushInvalidationSummary = null;
            if (!klines5m.isEmpty()) {
                String[] lastBar = klines5m.get(klines5m.size() - 1);
                if (lastBar.length > 4) {
                    pushTriggerPrice = new BigDecimal(lastBar[4]);
                }
                BigDecimal minLow = null;
                BigDecimal maxHigh = null;
                for (String[] bar : klines5m) {
                    if (bar.length > 4) {
                        BigDecimal high = new BigDecimal(bar[2]);
                        BigDecimal low = new BigDecimal(bar[3]);
                        minLow = minLow == null ? low : minLow.min(low);
                        maxHigh = maxHigh == null ? high : maxHigh.max(high);
                    }
                }
                if ("BULLISH".equals(ruleMarketBias)) {
                    pushInvalidPriceBelow = minLow;
                    pushInvalidationSummary = "结构失效：当前价低于近端 5m 摆动低点";
                } else {
                    pushInvalidPriceAbove = maxHigh;
                    pushInvalidationSummary = "结构失效：当前价高于近端 5m 摆动高点";
                }
            }

            // ==================== 4. 输出最终决策 ====================
            String conclusion = String.format(
                    "规则层基础方向：%s | AI编排模式：%s | 总分 %d | 多TF收敛：%s | 外部上下文阻塞：%s",
                    ruleMarketBias,
                    aiReview.getOrchestrationMode(),
                    finalScore,
                    multiTfConvergence ? "STRONG" : "WEAK",
                    externalContextBlocked);
            DecisionBundleVO decision = new DecisionBundleVO();
            decision.setDecisionId(decisionId);
            decision.setMarketBiasHierarchy(ruleMarketBias);
            decision.setTradeType("SPOT");
            decision.setConfidenceLevel(conflict.getAdjustedConfidence() != null
                    ? conflict.getAdjustedConfidence()
                    : confidenceLevel);
            decision.setRiskLevel(riskLevelLabel);
            decision.setActionPriority(finalScore > actionPriorityHighMinScoreExclusive ? "HIGH" : "MEDIUM");
            decision.setConclusionSummary(conclusion);
            decision.setIsWorthOpening(effectiveWorthOpening && !confused.isDirectionalPushBlocked());
            decision.setMultiTfConvergence(multiTfLabel);
            AiRoleResultsPayload.SynthesisPayload aiSynthesis = new AiRoleResultsPayload.SynthesisPayload(
                    ruleMarketBias,
                    conflict.getAdjustedConfidence() != null ? conflict.getAdjustedConfidence() : confidenceLevel,
                    riskLevelLabel,
                    effectiveWorthOpening && !confused.isDirectionalPushBlocked(),
                    conflict.getLevel() != null ? conflict.getLevel().name() : null,
                    conflict.getAiConflictScore(),
                    conflict.getAdjustedConfidence(),
                    conflict.getRiskAdjustment(),
                    conflict.getPlanMode(),
                    AssetStateEnum.CONFUSED.equals(finalAssetState) || confused.isDirectionalPushBlocked(),
                    firstAiDowngradeReason(aiReview));
            decision.setAiRoleResults(aiRoleResultsCodec.serialize(aiReview, ruleVersion, aiSynthesis));
            decision.setReviewReasons(reviewJson);
            decision.setAiConflictLevel(conflict.getLevel() != null ? conflict.getLevel().name() : null);
            decision.setAiConflictScore(conflict.getAiConflictScore());
            decision.setAiPlanMode(conflict.getPlanMode());
            decision.setConfusedScore(confused.getConfusedScore());
            decision.setConfusedLowStreak(confused.getConfusedLowStreak());
            decision.setDirectionalPushBlocked(confused.isDirectionalPushBlocked());
            decision.setDirectionalPushBlockReason(confused.isDirectionalPushBlocked()
                    ? "CONFUSED_SCORE_BLOCK_THRESHOLD"
                    : null);
            decision.setAssetState(finalAssetState);
            decision.setAssetStateSnapshot(snapshot);
            decision.setMultiTimeframeAligned(multiTfConvergence);
            decision.setPushTriggerPrice(pushTriggerPrice);
            decision.setPushExpiresAt(pushExpiresAt);
            decision.setPushInvalidPriceBelow(pushInvalidPriceBelow);
            decision.setPushInvalidPriceAbove(pushInvalidPriceAbove);
            decision.setPushInvalidationSummary(pushInvalidationSummary);
            applyExternalContext(decision, externalContextInput);

            logger.info("[AI决策] 生成完成 → {} | ConfidenceLevel = {} | Score = {} | MultiTF = {} | Worth Open: {} | aiConflict={}/{} | confused={}",
                    ruleMarketBias, decision.getConfidenceLevel(), finalScore, decision.getMultiTfConvergence(), decision.getIsWorthOpening(),
                    decision.getAiConflictLevel(), decision.getAiConflictScore(), decision.getConfusedScore());

            return decision;

        } catch (Exception e) {
            logger.error("[AI决策] 生成失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    private static int getInt(Map<String, RuleConfigDO> cfgMap, String key, int defaultVal) {
        if (cfgMap == null || key == null) {
            return defaultVal;
        }
        RuleConfigDO cfg = cfgMap.get(key);
        if (cfg == null || cfg.getRuleValue() == null) {
            return defaultVal;
        }
        String raw = cfg.getRuleValue().trim();
        if (raw.isEmpty()) {
            return defaultVal;
        }
        try {
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return defaultVal;
        }
    }

    private static boolean bullish(List<String[]> bars) {
        if (bars == null || bars.isEmpty()) return false;
        String[] last = bars.get(bars.size() - 1);
        return last != null && last.length > 4
                && new BigDecimal(last[4]).compareTo(new BigDecimal(last[1])) > 0;
    }

    private static AssetStateEnum parseAssetState(String raw, AssetStateEnum fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return AssetStateEnum.valueOf(raw.trim().toUpperCase());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean isEffectiveExternalBlocked(EventImpactInputVO input) {
        return input != null
                && (Boolean.TRUE.equals(input.getExternalContextBlocked())
                || ExternalContextPolicy.SOURCE_HEALTH_BLOCKED.equalsIgnoreCase(input.getExternalContextSourceHealth()));
    }

    private static AssetStateEnum failClosedExternalState(AssetStateEnum syntheticState, boolean externalBlocked) {
        if (!externalBlocked) {
            return syntheticState;
        }
        if (AssetStateEnum.CONFUSED.equals(syntheticState)
                || AssetStateEnum.COOLING.equals(syntheticState)
                || AssetStateEnum.INVALIDATED.equals(syntheticState)) {
            return syntheticState;
        }
        return AssetStateEnum.HIGH_RISK;
    }

    private AiOrchestratorResult runAiReview(String symbol, String timeframe, String analysisId, String decisionId,
                                             String ruleMarketBias, String confidenceLevel, String riskTier,
                                             boolean effectiveWorthOpening,
                                             Integer dataQualityScore, Integer trendStructureScore,
                                             boolean multiTfConvergence,
                                             EventImpactInputVO externalContextInput,
                                             int baseScore, int convergenceScore, int finalScore,
                                             boolean isBullish5m, boolean isBullish4h,
                                             boolean externalContextBlocked) {
        String traceId = analysisId != null && !analysisId.isBlank()
                ? analysisId + "-ai-review"
                : decisionId + "-ai-review";
        if (aiDecisionOrchestratorService == null) {
            return ruleOnlyFallback(analysisId, traceId, "AI_ORCHESTRATOR_NOT_WIRED");
        }
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId(analysisId);
        request.setTraceId(traceId);
        request.setSymbol(symbol);
        request.setTimeframe(timeframe);
        request.setRuleMarketBias(ruleMarketBias);
        request.setRuleConfidence(confidenceLevel);
        request.setRuleRiskLevel(riskTier);
        request.setRuleWorthOpening(effectiveWorthOpening);
        request.setDataQualityScore(dataQualityScore);
        request.setTrendStructureScore(trendStructureScore);
        request.setMultiTimeframeState(multiTfConvergence ? "ALIGNED" : "MISALIGNED");
        request.setExternalContextState(externalContextSummary(externalContextInput, externalContextBlocked));
        request.setEvidenceSummary("Rule layer produced base direction from authoritative 5m/15m/1h/4h klines; AI may only review or challenge.");
        request.setScoreSummary("baseScore=" + baseScore + ", convergenceScore=" + convergenceScore + ", finalScore=" + finalScore);
        Map<String, Object> facts = new LinkedHashMap<>();
        facts.put("isBullish5m", isBullish5m);
        facts.put("isBullish4h", isBullish4h);
        facts.put("multiTimeframeAligned", multiTfConvergence);
        facts.put("externalContextBlocked", externalContextBlocked);
        facts.put("reviewOnly", true);
        facts.put("notExecutable", true);
        facts.put("ruleDirectionPreserved", true);
        request.setDecisionFacts(facts);
        request.setRequestTime(LocalDateTime.now());
        try {
            return aiDecisionOrchestratorService.review(request);
        } catch (Exception e) {
            AiOrchestratorResult fallback = ruleOnlyFallback(analysisId, traceId, "AI_ORCHESTRATOR_FAILED");
            fallback.setReasonCodes(List.of("AI_ORCHESTRATOR_FAILED"));
            return fallback;
        }
    }

    private static AiOrchestratorResult ruleOnlyFallback(String analysisId, String traceId, String reasonCode) {
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setAnalysisId(analysisId);
        result.setTraceId(traceId);
        result.setOrchestrationMode(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        result.setReasonCodes(List.of(reasonCode));
        result.setCompletedAt(LocalDateTime.now());
        return result;
    }

    private static String firstAiDowngradeReason(AiOrchestratorResult result) {
        if (result == null) {
            return null;
        }
        for (AiProviderReviewResult providerResult : result.getProviderResults()) {
            if (providerResult == null || !providerResult.challengesRule()) {
                continue;
            }
            for (String reasonCode : providerResult.getReasonCodes()) {
                if (reasonCode != null && !reasonCode.isBlank()) {
                    return reasonCode;
                }
            }
        }
        return null;
    }

    private static String externalContextSummary(EventImpactInputVO input, boolean effectiveBlocked) {
        if (input == null) {
            return "ABSENT";
        }
        return "status=" + input.getExternalContextStatus()
                + ", risk=" + input.getExternalContextRiskLevel()
                + ", blocked=" + effectiveBlocked
                + ", sourceHealth=" + input.getExternalContextSourceHealth()
                + ", reasonCodes=" + input.getExternalContextReasonCodes();
    }

    private static void applyExternalContext(DecisionBundleVO decision, EventImpactInputVO input) {
        if (decision == null || input == null) {
            return;
        }
        decision.setExternalContextStatus(input.getExternalContextStatus());
        decision.setActiveExternalEventCount(input.getActiveExternalEventCount());
        decision.setActiveMacroEventCount(input.getActiveMacroEventCount());
        decision.setActiveNewsEventCount(input.getActiveNewsEventCount());
        decision.setExternalContextRiskLevel(input.getExternalContextRiskLevel());
        decision.setExternalContextBlocked(input.getExternalContextBlocked());
        decision.setExternalEventIds(input.getExternalEventIds());
        decision.setExternalContextReasonCodes(input.getExternalContextReasonCodes());
        decision.setNextExternalEventTime(input.getNextExternalEventTime());
        decision.setLatestExternalEventTime(input.getLatestExternalEventTime());
        decision.setLatestExternalEventLabel(input.getLatestExternalEventLabel());
        decision.setExternalEventWindowStart(input.getExternalEventWindowStart());
        decision.setExternalEventWindowEnd(input.getExternalEventWindowEnd());
        decision.setExternalContextSourceHealth(input.getExternalContextSourceHealth());
        boolean sourceBlocked = ExternalContextPolicy.SOURCE_HEALTH_BLOCKED.equalsIgnoreCase(input.getExternalContextSourceHealth());
        boolean externallyBlocked = Boolean.TRUE.equals(input.getExternalContextBlocked()) || sourceBlocked;
        boolean highRisk = ExternalContextPolicy.RISK_HIGH.equalsIgnoreCase(input.getExternalContextRiskLevel()) || externallyBlocked;
        if (highRisk) {
            decision.setRiskLevel("HIGH");
            decision.setConfidenceLevel(ExternalContextPolicy.lowerConfidenceOneLevel(decision.getConfidenceLevel()));
            decision.setReviewReasons(appendExternalReviewReasons(decision.getReviewReasons(), input));
            decision.setConclusionSummary(decision.getConclusionSummary() + " | External context risk="
                    + input.getExternalContextRiskLevel() + " reasonCodes=" + input.getExternalContextReasonCodes());
        }
        if (externallyBlocked) {
            decision.setIsWorthOpening(false);
        }
    }

    private static String appendExternalReviewReasons(String reviewReasons, EventImpactInputVO input) {
        List<String> codes = input.getExternalContextReasonCodes();
        if (codes == null || codes.isEmpty()) {
            return reviewReasons == null || reviewReasons.isBlank() ? "[]" : reviewReasons;
        }
        StringBuilder additions = new StringBuilder();
        for (String code : codes) {
            if (additions.length() > 0) {
                additions.append(',');
            }
            additions.append("{\"code\":\"").append(code.replace("\"", "")).append("\",\"source\":\"EXTERNAL_CONTEXT\"}");
        }
        if (reviewReasons == null || reviewReasons.isBlank() || "[]".equals(reviewReasons.trim())) {
            return "[" + additions + "]";
        }
        String trimmed = reviewReasons.trim();
        if (trimmed.endsWith("]")) {
            return trimmed.substring(0, trimmed.length() - 1) + "," + additions + "]";
        }
        return "[" + additions + "]";
    }
}
