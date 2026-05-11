package org.example.trademodel.service;

import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.vo.DecisionBundleVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

/**
 * V3 Multi-Agent 决策引擎 - 三角色协同版（真实K线 + 多时间框架）
 * 1. GPT-5.4：最终裁决官
 * 2. Gemini 2.5 Pro：冲突复核官
 * 3. Grok 4.20：快讯与反方挑战官
 *
 * <p>本 run 将 {@link DecisionContext} 接入 {@link AiConflictResolverService}、{@link ConfusedStateService}、
 * {@link AssetStateService}，供 {@link org.example.trademodel.service.impl.AnalysisAssemblerServiceImpl} 落库。</p>
 */
@Service
public class DecisionEngineService {

    private static final Logger logger = LoggerFactory.getLogger(DecisionEngineService.class);

    private final RealMarketDataFetcherService marketDataFetcher;
    private final AiConflictResolverService aiConflictResolverService;
    private final ConfusedStateService confusedStateService;
    private final AssetStateService assetStateService;
    private final RuleConfigService ruleConfigService;

    // ========= 最小规则键集合（仅限本阶段允许 keys） =========
    private static final String KEY_WORTH_OPENING_MIN_SCORE = "decision.worth_opening_min_score";
    private static final int DEFAULT_WORTH_OPENING_MIN_SCORE = 72;

    private static final String KEY_CONFIDENCE_HIGH_MIN_SCORE = "decision.confidence.high_min_score";
    private static final int DEFAULT_CONFIDENCE_HIGH_MIN_SCORE = 88;

    private static final String KEY_CONFIDENCE_MEDIUM_MIN_SCORE = "decision.confidence.medium_min_score";
    private static final int DEFAULT_CONFIDENCE_MEDIUM_MIN_SCORE = 75;

    private static final String KEY_RISK_TIER_LOW_MIN_SCORE = "decision.risk_tier.low_min_score";
    private static final int DEFAULT_RISK_TIER_LOW_MIN_SCORE = 80;

    private static final String KEY_CONFUSED_HIGH_MIN_SCORE = "decision.confused_high_min_score";
    private static final int DEFAULT_CONFUSED_HIGH_MIN_SCORE = 70;

    private static final String KEY_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW = "decision.risk_level.high_final_score_below";
    private static final int DEFAULT_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW = 45;

    private static final String KEY_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE = "decision.action_priority.high_min_score_exclusive";
    private static final int DEFAULT_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE = 85;
    private static final int MIN_DATA_QUALITY_SCORE_FOR_OPENING = 60;
    private static final int MIN_TREND_STRUCTURE_SCORE_FOR_OPENING = 50;

    public DecisionEngineService(RealMarketDataFetcherService marketDataFetcher,
                                 AiConflictResolverService aiConflictResolverService,
                                 ConfusedStateService confusedStateService,
                                 AssetStateService assetStateService,
                                 RuleConfigService ruleConfigService) {
        this.marketDataFetcher = marketDataFetcher;
        this.aiConflictResolverService = aiConflictResolverService;
        this.confusedStateService = confusedStateService;
        this.assetStateService = assetStateService;
        this.ruleConfigService = ruleConfigService;
        logger.info("✅ DecisionEngineService V3 Multi-Agent (三角色协同 + 真实K线 + 多TF) initialized successfully");
    }

    public DecisionBundleVO makeDecision(String symbol, String timeframe, String analysisId) {
        return makeDecision(symbol, timeframe, analysisId, null, null);
    }

    public DecisionBundleVO makeDecision(String symbol, String timeframe, String analysisId, Integer dataQualityScore) {
        return makeDecision(symbol, timeframe, analysisId, dataQualityScore, null);
    }

    public DecisionBundleVO makeDecision(String symbol, String timeframe, String analysisId,
                                         Integer dataQualityScore, Integer trendStructureScore) {
        String decisionId = "dec-" + Instant.now().toEpochMilli();
        logger.info("[AI决策] === 开始为 {} {} analysisId={} 生成决策 ===", symbol, timeframe, analysisId);

        try {
            Map<String, RuleConfigDO> ruleMap = ruleConfigService != null
                    ? ruleConfigService.getRuleConfigMap()
                    : null;

            // 缺失/禁用/不可解析全部回退默认值，避免配置缺失导致行为崩掉
            int worthOpeningMinScore = getInt(ruleMap, KEY_WORTH_OPENING_MIN_SCORE, DEFAULT_WORTH_OPENING_MIN_SCORE);
            int confidenceHighMinScore = getInt(ruleMap, KEY_CONFIDENCE_HIGH_MIN_SCORE, DEFAULT_CONFIDENCE_HIGH_MIN_SCORE);
            int confidenceMediumMinScore = getInt(ruleMap, KEY_CONFIDENCE_MEDIUM_MIN_SCORE, DEFAULT_CONFIDENCE_MEDIUM_MIN_SCORE);
            int riskTierLowMinScore = getInt(ruleMap, KEY_RISK_TIER_LOW_MIN_SCORE, DEFAULT_RISK_TIER_LOW_MIN_SCORE);
            int confusedHighMinScore = getInt(ruleMap, KEY_CONFUSED_HIGH_MIN_SCORE, DEFAULT_CONFUSED_HIGH_MIN_SCORE);
            int highFinalScoreBelow = getInt(ruleMap, KEY_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW,
                    DEFAULT_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW);
            int actionPriorityHighMinScoreExclusive = getInt(ruleMap,
                    KEY_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE,
                    DEFAULT_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE);

            // ==================== 1. 真实 K 线数据 ====================
            List<String[]> klines1m = marketDataFetcher.fetchKlines(symbol, "1m", 3);
            List<String[]> klines5m = marketDataFetcher.fetchKlines(symbol, "5m", 3);

            // 最后一根 K 线判断涨跌（close > open = 看涨）
            boolean isBullish1m = !klines1m.isEmpty() && 
                Double.parseDouble(klines1m.get(klines1m.size()-1)[4]) > 
                Double.parseDouble(klines1m.get(klines1m.size()-1)[1]);

            boolean isBullish5m = !klines5m.isEmpty() && 
                Double.parseDouble(klines5m.get(klines5m.size()-1)[4]) > 
                Double.parseDouble(klines5m.get(klines5m.size()-1)[1]);

            boolean multiTfConvergence = isBullish1m == isBullish5m;
            int convergenceScore = multiTfConvergence ? 15 : -10;

            // ==================== 2. 三角色协同 ====================

            // Grok 4.20：快讯与反方挑战官
            String grokOpinion = isBullish1m ? 
                "快讯：1m K线阳线，短期情绪看涨。但反方挑战：5m 未完全收敛，需警惕假突破。" : 
                "快讯：1m K线阴线，短期情绪偏空。但反方挑战：可能仅为洗盘，关注支撑位。";

            // Gemini 2.5 Pro：冲突复核官
            String geminiReview = multiTfConvergence ? 
                "复核通过：1m与5m方向一致，无明显冲突，维持原判断。" : 
                "复核警告：1m与5m方向冲突，建议降级置信度或转为观望。";

            // GPT-5.4：最终裁决官（汇总打分）
            int baseScore = isBullish1m ? 82 : 58;
            int finalScore = baseScore + convergenceScore;

            // finalBias 门槛：仅使用 decision.confidence.medium_min_score 作为 BULLISH 阈值
            String finalBias = finalScore >= confidenceMediumMinScore ? "BULLISH" : "BEARISH";
            String confidenceLevel = finalScore >= confidenceHighMinScore ? "HIGH" :
                    (finalScore >= confidenceMediumMinScore ? "MEDIUM" : "LOW");
            boolean worthOpening = finalScore >= worthOpeningMinScore;
            if (trendStructureScore != null && trendStructureScore < MIN_TREND_STRUCTURE_SCORE_FOR_OPENING) {
                worthOpening = false;
            }
            if (dataQualityScore != null && dataQualityScore < MIN_DATA_QUALITY_SCORE_FOR_OPENING) {
                worthOpening = false;
            }

            String conclusion = String.format(
                "GPT-5.4 最终裁决：%s | 总分 %d | Gemini复核：%s | Grok快讯：%s | 多TF收敛：%s",
                finalBias, finalScore, geminiReview, grokOpinion, multiTfConvergence ? "STRONG" : "WEAK");

            // ==================== 3. 决策上下文：冲突 / 困惑 / 快照（本 run K 线事实） ====================
            String ruleMarketBias = isBullish1m ? "BULLISH" : "BEARISH";
            String riskTier = finalScore >= riskTierLowMinScore ? "LOW" : "MEDIUM";

            DecisionContext ctx = new DecisionContext();
            ctx.setSymbol(symbol);
            ctx.setRuleMarketBias(ruleMarketBias);
            ctx.setRuleConfidenceLevel(confidenceLevel);
            ctx.setHasRuleBaseOutput(true);
            ctx.setGptConsistentWithRule(ruleMarketBias.equals(finalBias));
            ctx.setMultiTimeframeAligned(multiTfConvergence);
            ctx.setRiskTier(riskTier);
            ctx.setDataQualityScore(dataQualityScore);
            ctx.setWorthOpening(worthOpening);
            ctx.setDriverConflictScore(multiTfConvergence ? 18 : 48);
            ctx.setExecutionInstabilityScore(26);
            ctx.setMicrostructureTrapScore(multiTfConvergence ? 22 : 38);
            ctx.setCauseEffectDivergenceScore(multiTfConvergence ? 12 : 40);
            ctx.setConsecutiveLowConfusedCount(0);

            AiConflictResult conflict = aiConflictResolverService.resolve(ctx);
            ConfusedResult confused = confusedStateService.calculateConfused(symbol, ctx);

            AssetStateEnum syntheticState;
            if (confused.getConfusedScore() >= confusedHighMinScore) {
                syntheticState = AssetStateEnum.CONFUSED;
            } else if (worthOpening) {
                syntheticState = AssetStateEnum.CANDIDATE;
            } else {
                syntheticState = AssetStateEnum.OBSERVING;
            }
            String snapshot = assetStateService.buildSnapshotAtDecision(symbol, analysisId != null ? analysisId : "",
                    syntheticState, confused.getConfusedScore(), multiTfConvergence);

            String riskLevelLabel;
            if (confused.getConfusedScore() >= confusedHighMinScore || finalScore < highFinalScoreBelow) {
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

            // Push 快照专用：与本 run 1m K 线一致，供 tm_push_snapshot / Recheck 漂移、过期、结构化失效
            BigDecimal pushTriggerPrice = null;
            LocalDateTime pushExpiresAt = LocalDateTime.now().plusHours(24);
            BigDecimal pushInvalidPriceBelow = null;
            BigDecimal pushInvalidPriceAbove = null;
            String pushInvalidationSummary = null;
            if (!klines1m.isEmpty()) {
                String[] lastBar = klines1m.get(klines1m.size() - 1);
                if (lastBar.length > 4) {
                    pushTriggerPrice = new BigDecimal(lastBar[4]);
                }
                BigDecimal minLow = null;
                BigDecimal maxHigh = null;
                for (String[] bar : klines1m) {
                    if (bar.length > 4) {
                        BigDecimal high = new BigDecimal(bar[2]);
                        BigDecimal low = new BigDecimal(bar[3]);
                        minLow = minLow == null ? low : minLow.min(low);
                        maxHigh = maxHigh == null ? high : maxHigh.max(high);
                    }
                }
                if ("BULLISH".equals(finalBias)) {
                    pushInvalidPriceBelow = minLow;
                    pushInvalidationSummary = "结构失效：当前价低于近端 1m 摆动低点";
                } else {
                    pushInvalidPriceAbove = maxHigh;
                    pushInvalidationSummary = "结构失效：当前价高于近端 1m 摆动高点";
                }
            }

            // ==================== 4. 输出最终决策 ====================
            DecisionBundleVO decision = new DecisionBundleVO();
            decision.setDecisionId(decisionId);
            decision.setMarketBiasHierarchy(finalBias);
            decision.setTradeType("SPOT");
            decision.setConfidenceLevel(confidenceLevel);
            decision.setRiskLevel(riskLevelLabel);
            decision.setActionPriority(finalScore > actionPriorityHighMinScoreExclusive ? "HIGH" : "MEDIUM");
            decision.setConclusionSummary(conclusion);
            decision.setIsWorthOpening(worthOpening);
            decision.setMultiTfConvergence(multiTfLabel);
            decision.setAiRoleResults(
                "SYNTHETIC_RULE_COMPOSED: no external AI API call | Grok: " + grokOpinion
                        + " | Gemini: " + geminiReview
                        + " | GPT-5.4: 最终裁决 " + finalBias + " (Score=" + finalScore + ")");
            decision.setReviewReasons(reviewJson);
            decision.setAiConflictLevel(conflict.getLevel() != null ? conflict.getLevel().name() : null);
            decision.setAiConflictScore(conflict.getAiConflictScore());
            decision.setAiPlanMode(conflict.getPlanMode());
            decision.setConfusedScore(confused.getConfusedScore());
            decision.setAssetState(syntheticState);
            decision.setAssetStateSnapshot(snapshot);
            decision.setMultiTimeframeAligned(multiTfConvergence);
            decision.setPushTriggerPrice(pushTriggerPrice);
            decision.setPushExpiresAt(pushExpiresAt);
            decision.setPushInvalidPriceBelow(pushInvalidPriceBelow);
            decision.setPushInvalidPriceAbove(pushInvalidPriceAbove);
            decision.setPushInvalidationSummary(pushInvalidationSummary);

            logger.info("[AI决策] 生成完成 → {} | ConfidenceLevel = {} | Score = {} | MultiTF = {} | Worth Open: {} | aiConflict={}/{} | confused={}",
                    finalBias, confidenceLevel, finalScore, decision.getMultiTfConvergence(), worthOpening,
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
}
