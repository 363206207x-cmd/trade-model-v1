package org.example.trademodel.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiRoleResultsCodec;
import org.example.trademodel.ai.AiRoleResultsPayload;
import org.example.trademodel.analysisrun.AnalysisPersistenceIds;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.derivatives.DerivativesBusinessAssessment;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.example.trademodel.enums.AssetStateEnum;
import org.example.trademodel.service.support.ExternalContextPolicy;
import org.example.trademodel.service.support.DataQualityCircuitBreakerPolicy;
import org.example.trademodel.service.support.UtcLocalTimePolicy;
import org.example.trademodel.service.support.V41DecisionContractPolicy;
import org.example.trademodel.vo.DecisionBundleVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private Clock decisionClock = Clock.systemUTC();
    private FundamentalAiV41Properties v41Properties = FundamentalAiV41Properties.contractFixture();

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
    private static final int MIN_TREND_STRUCTURE_SCORE_FOR_OPENING = 50;
    private static final String KEY_DERIVATIVES_EIGHT_SCORE_ADJUSTMENT_CAP =
            "derivatives_decision_config.eight_score_adjustment_cap";
    private static final String KEY_DERIVATIVES_EIGHT_SCORE_ADJUSTMENT_FACTOR_PERCENT =
            "derivatives_decision_config.eight_score_adjustment_factor_percent";

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

    @Autowired(required = false)
    void setDecisionClock(Clock decisionClock) {
        this.decisionClock = decisionClock != null ? decisionClock : Clock.systemUTC();
    }

    @Autowired(required = false)
    void setFundamentalAiV41Properties(FundamentalAiV41Properties properties) {
        if (properties != null) this.v41Properties = properties;
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
        return makeDecision(symbol, timeframe, analysisId, dataQualityScore, trendStructureScore,
                externalContextInput, null);
    }

    public DecisionBundleVO makeDecision(String symbol, String timeframe, String analysisId,
                                         Integer dataQualityScore, Integer trendStructureScore,
                                         EventImpactInputVO externalContextInput,
                                         DerivativesBusinessAssessment derivativesAssessment) {
        return makeDecision(symbol, timeframe, analysisId, dataQualityScore, trendStructureScore,
                externalContextInput, derivativesAssessment, null);
    }

    public DecisionBundleVO makeDecision(String symbol, String timeframe, String analysisId,
                                         Integer dataQualityScore, Integer trendStructureScore,
                                         EventImpactInputVO externalContextInput,
                                         DerivativesBusinessAssessment derivativesAssessment,
                                         Integer eightScoreComposite) {
        return makeDecisionInternal(symbol, timeframe, analysisId, dataQualityScore, trendStructureScore,
                externalContextInput, derivativesAssessment, eightScoreComposite, null, true, null);
    }

    public DecisionBundleVO makeDecisionForDecisionChain(String symbol, String timeframe, String analysisId,
                                                         Integer dataQualityScore, Integer trendStructureScore,
                                                         EventImpactInputVO externalContextInput,
                                                         DerivativesBusinessAssessment derivativesAssessment,
                                                         Integer eightScoreComposite) {
        return makeDecisionInternal(symbol, timeframe, analysisId, dataQualityScore, trendStructureScore,
                externalContextInput, derivativesAssessment, eightScoreComposite, null, false, null);
    }

    public DecisionBundleVO makeDecisionForDecisionChain(String symbol, String timeframe, String analysisId,
                                                         Integer dataQualityScore, Integer trendStructureScore,
                                                         EventImpactInputVO externalContextInput,
                                                         DerivativesBusinessAssessment derivativesAssessment,
                                                         Integer eightScoreComposite,
                                                         OpportunityStateIdentity opportunityIdentity) {
        return makeDecisionInternal(symbol, timeframe, analysisId, dataQualityScore, trendStructureScore,
                externalContextInput, derivativesAssessment, eightScoreComposite, null, false, opportunityIdentity);
    }

    public DecisionBundleVO makeDecisionForDecisionChain(String symbol, String timeframe, String analysisId,
                                                         Integer dataQualityScore, Integer trendStructureScore,
                                                         EventImpactInputVO externalContextInput,
                                                         DerivativesBusinessAssessment derivativesAssessment,
                                                         List<ScoreItemVO> scores,
                                                         OpportunityStateIdentity opportunityIdentity) {
        return makeDecisionInternal(symbol, timeframe, analysisId, dataQualityScore, trendStructureScore,
                externalContextInput, derivativesAssessment, averageScore(scores), scores, false,
                opportunityIdentity);
    }

    private DecisionBundleVO makeDecisionInternal(String symbol, String timeframe, String analysisId,
                                                  Integer dataQualityScore, Integer trendStructureScore,
                                                  EventImpactInputVO externalContextInput,
                                                  DerivativesBusinessAssessment derivativesAssessment,
                                                  Integer eightScoreComposite,
                                                  List<ScoreItemVO> scores,
                                                  boolean runLegacyAiReview,
                                                  OpportunityStateIdentity opportunityIdentity) {
        String decisionId = AnalysisPersistenceIds.decisionId();
        logger.info("[AI决策] === 开始为 {} {} analysisId={} 生成决策 ===", symbol, timeframe, analysisId);

        try {
            Map<String, RuleConfigDO> ruleMap = ruleConfigService != null
                    ? ruleConfigService.getRuleConfigMap()
                    : null;
            String ruleVersion = ruleConfigService != null
                    ? ruleConfigService.resolveActiveRuleVersion()
                    : "v1.0";

            int worthOpeningMinScore = runLegacyAiReview
                    ? getInt(ruleMap, KEY_WORTH_OPENING_MIN_SCORE, DEFAULT_WORTH_OPENING_MIN_SCORE)
                    : requireCandidatePromotionScore();
            int confidenceHighMinScore = getInt(ruleMap, KEY_CONFIDENCE_HIGH_MIN_SCORE, DEFAULT_CONFIDENCE_HIGH_MIN_SCORE);
            int confidenceMediumMinScore = getInt(ruleMap, KEY_CONFIDENCE_MEDIUM_MIN_SCORE, DEFAULT_CONFIDENCE_MEDIUM_MIN_SCORE);
            int riskTierLowMinScore = getInt(ruleMap, KEY_RISK_TIER_LOW_MIN_SCORE, DEFAULT_RISK_TIER_LOW_MIN_SCORE);
            int highFinalScoreBelow = getInt(ruleMap, KEY_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW,
                    DEFAULT_RISK_LEVEL_HIGH_FINAL_SCORE_BELOW);
            int actionPriorityHighMinScoreExclusive = getInt(ruleMap,
                    KEY_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE,
                    DEFAULT_ACTION_PRIORITY_HIGH_MIN_SCORE_EXCLUSIVE);
            int eightScoreAdjustmentCap = getIntInRange(ruleMap,
                    KEY_DERIVATIVES_EIGHT_SCORE_ADJUSTMENT_CAP, 10, 1, 25);
            int eightScoreAdjustmentFactorPercent = getIntInRange(ruleMap,
                    KEY_DERIVATIVES_EIGHT_SCORE_ADJUSTMENT_FACTOR_PERCENT, 20, 1, 100);

            // ==================== 1. 权威落库 K 线快照（同一 run trace） ====================
            String marketTraceId = analysisId == null || analysisId.isBlank() ? decisionId : analysisId;
            List<String[]> klines5m = readNormalizationWindow(symbol, "5m", marketTraceId);
            List<String[]> klines15m = readNormalizationWindow(symbol, "15m", marketTraceId);
            List<String[]> klines1h = readNormalizationWindow(symbol, "1h", marketTraceId);
            List<String[]> klines4h = readNormalizationWindow(symbol, "4h", marketTraceId);

            MarketBiasPolicy.DirectionAssessment directionAssessment = MarketBiasPolicy.assessDirection(
                    klines5m, klines15m, klines1h, klines4h,
                    v41Properties.getMultiTimeframe(), v41Properties.getNormalization());
            String ruleMarketBias = directionAssessment.ruleMarketBias();
            Map<String, Map<String, Object>> multiTimeframeDetails = MarketBiasPolicy.describeTimeframes(
                    klines5m, klines15m, klines1h, klines4h,
                    v41Properties.getMultiTimeframe(), v41Properties.getNormalization());
            boolean isBullish5m = MarketBiasPolicy.direction(klines5m)
                    == MarketBiasPolicy.WindowDirection.BULLISH;
            boolean isBullish4h = MarketBiasPolicy.direction(klines4h)
                    == MarketBiasPolicy.WindowDirection.BULLISH;
            boolean multiTfConvergence = directionAssessment.structurallyReady()
                    && directional(ruleMarketBias)
                    && sameDirection(directionAssessment.normalized4hDirectionScore(),
                    directionAssessment.normalized1hDirectionScore());
            int convergenceScore = multiTfConvergence ? 15 : -10;

            // ==================== 2. 规则层基础方向 + AI review-only 编排 ====================
            int baseScore = MarketBiasPolicy.bullishFamily(ruleMarketBias)
                    ? 82 : MarketBiasPolicy.bearishFamily(ruleMarketBias) ? 58 : 50;
            int eightScoreAdjustment = eightScoreComposite == null
                    ? 0
                    : Math.max(-eightScoreAdjustmentCap, Math.min(eightScoreAdjustmentCap,
                    (int) Math.round((eightScoreComposite - 50)
                            * eightScoreAdjustmentFactorPercent / 100.0)));
            int finalScore = baseScore + convergenceScore + eightScoreAdjustment;

            String confidenceLevel = finalScore >= confidenceHighMinScore ? "HIGH" :
                    (finalScore >= confidenceMediumMinScore ? "MEDIUM" : "LOW");
            boolean aiQualityEligible = dataQualityScore != null
                    && dataQualityScore >= v41Properties.getAiGate().getMinimumDataQuality();
            if (!aiQualityEligible) confidenceLevel = downgradeConfidenceLevel(confidenceLevel);
            boolean hasUsableMarketStructure = directional(ruleMarketBias)
                    && directionAssessment.structurallyReady();
            boolean dataQualitySufficient = DataQualityCircuitBreakerPolicy.passes(dataQualityScore);
            boolean decisionInputsSufficient = dataQualitySufficient && hasUsableMarketStructure;
            boolean worthOpening = finalScore >= worthOpeningMinScore && multiTfConvergence
                    && decisionInputsSufficient;
            if (trendStructureScore != null && trendStructureScore < MIN_TREND_STRUCTURE_SCORE_FOR_OPENING) {
                worthOpening = false;
            }
            boolean externalContextBlocked = isEffectiveExternalBlocked(externalContextInput);
            boolean effectiveWorthOpening = worthOpening && !externalContextBlocked;
            boolean derivativesMandatory = "MANDATORY".equals(
                    v41Properties.getProviderMatrix().getDerivativesRequirement());
            if (derivativesMandatory && derivativesAssessment != null
                    && (derivativesAssessment.sourceStatus() == org.example.trademodel.providercall.UnifiedSourceStatus.STALE
                    || derivativesAssessment.sourceStatus() == org.example.trademodel.providercall.UnifiedSourceStatus.ERROR
                    || derivativesAssessment.sourceStatus() == org.example.trademodel.providercall.UnifiedSourceStatus.NOT_CONFIGURED
                    || derivativesAssessment.sourceStatus() == org.example.trademodel.providercall.UnifiedSourceStatus.DISABLED)) {
                effectiveWorthOpening = false;
            }
            String riskTier = decisionInputsSufficient
                    ? finalScore >= riskTierLowMinScore ? "LOW" : "MEDIUM"
                    : "HIGH";
            String validatedMarketBias = dataQualitySufficient && hasUsableMarketStructure
                    ? ruleMarketBias : null;
            String userMarketBias = validatedMarketBias == null ? "WAIT" : validatedMarketBias;
            String userConfidenceLevel = dataQualitySufficient ? confidenceLevel : "LOW";

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
            if (derivativesAssessment != null) {
                ctx.setDriverConflictScore(cap100(ctx.getDriverConflictScore()
                        + derivativesAssessment.driverConflictDelta()));
                ctx.setExecutionInstabilityScore(cap100(ctx.getExecutionInstabilityScore()
                        + derivativesAssessment.executionInstabilityDelta()));
                ctx.setMicrostructureTrapScore(cap100(ctx.getMicrostructureTrapScore()
                        + derivativesAssessment.microstructureTrapDelta()));
                ctx.setCauseEffectDivergenceScore(cap100(ctx.getCauseEffectDivergenceScore()
                        + derivativesAssessment.causeEffectDivergenceDelta()));
            }
            ctx.setConsecutiveLowConfusedCount(0);

            AiOrchestratorResult aiReview = runLegacyAiReview
                    ? runAiReview(symbol, timeframe, analysisId, decisionId,
                    ruleMarketBias, confidenceLevel, riskTier, effectiveWorthOpening,
                    dataQualityScore, trendStructureScore, multiTfConvergence,
                    externalContextInput, baseScore, convergenceScore, finalScore,
                    isBullish5m, isBullish4h, externalContextBlocked)
                    : ruleOnlyFallback(analysisId, analysisId + "-decision-chain-v4-1",
                    "SUPERSEDED_BY_DECISION_CHAIN_V4_1");
            ctx.setGptConsistentWithRule(aiReview.isGptConsistentWithRule());
            ctx.setGeminiConsistentWithRule(aiReview.isGeminiConsistentWithRule());
            ctx.setGrokConsistentWithRule(aiReview.isGrokConsistentWithRule());
            ctx.setAiSuccessfulProviderCount(aiReview.getSuccessfulProviderCount());
            ctx.setAiSupportCount(aiReview.getAiSupportCount());
            ctx.setAiObjectionCount(aiReview.getAiObjectionCount());
            ctx.setAiProviderConflictContribution(aiReview.getConflictContribution());
            ctx.setAiOrchestrationMode(aiReview.getOrchestrationMode().name());
            ctx.setAiOrchestrationSummary(aiReview.toSanitizedSummary());

            AiConflictResult conflict = aiConflictResolverService.resolve(ctx);
            ctx.setAiConflictScore(conflict.getConfusedContribution());
            ConfusedResult confused = opportunityIdentity == null
                    ? confusedStateService.calculateConfused(symbol, timeframe, ctx)
                    : confusedStateService.calculateConfused(opportunityIdentity, ctx);

            AssetStateEnum previousState = parseAssetState(confused.getPreviousState(), AssetStateEnum.OBSERVING);
            AssetStateEnum syntheticState = parseAssetState(confused.getNextState(),
                    effectiveWorthOpening ? AssetStateEnum.CANDIDATE : AssetStateEnum.OBSERVING);
            AssetStateEnum finalAssetState = failClosedExternalState(syntheticState, externalContextBlocked);
            finalAssetState = failClosedDecisionInputState(finalAssetState, decisionInputsSufficient);
            finalAssetState = mergeDerivativesState(finalAssetState, derivativesAssessment);
            if (finalAssetState == AssetStateEnum.CONFUSED) {
                validatedMarketBias = null;
                userMarketBias = "WAIT";
            }
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
            if (!decisionInputsSufficient
                    || confused.getConfusedScore() >= ConfusedStatePolicy.CONFUSED_ENTER_THRESHOLD
                    || finalScore < highFinalScoreBelow
                    || externalContextBlocked
                    || derivativesAssessment != null && derivativesAssessment.isHighRisk()
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
            OffsetDateTime validFrom = OffsetDateTime.now(decisionClock).withOffsetSameInstant(ZoneOffset.UTC);
            OffsetDateTime expiresAt = validFrom.plusHours(24);
            LocalDateTime pushExpiresAt = UtcLocalTimePolicy.fromOffsetDateTime(expiresAt);
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
                if (MarketBiasPolicy.bullishFamily(ruleMarketBias)) {
                    pushInvalidPriceBelow = minLow;
                    pushInvalidationSummary = "结构失效：当前价低于近端 5m 摆动低点";
                } else if (MarketBiasPolicy.bearishFamily(ruleMarketBias)) {
                    pushInvalidPriceAbove = maxHigh;
                    pushInvalidationSummary = "结构失效：当前价高于近端 5m 摆动高点";
                }
            }

            // ==================== 4. 输出最终决策 ====================
            String conclusion = String.format(
                    "规则层原始倾向：%s | 用户最终倾向：%s | AI编排模式：%s | 总分 %d | 八项评分修正 %+d | 多TF收敛：%s | 数据质量门控：%s | 外部上下文阻塞：%s",
                    ruleMarketBias,
                    userMarketBias,
                    aiReview.getOrchestrationMode(),
                    finalScore,
                    eightScoreAdjustment,
                    multiTfConvergence ? "STRONG" : "WEAK",
                    dataQualitySufficient ? "PASS" : "BLOCKED",
                    externalContextBlocked);
            DecisionBundleVO decision = new DecisionBundleVO();
            decision.setDecisionId(decisionId);
            decision.setMarketBiasHierarchy(userMarketBias);
            decision.setRuleMarketBias(ruleMarketBias);
            decision.setValidatedMarketBias(validatedMarketBias);
            decision.setDirectionDataState(validatedMarketBias != null
                    ? "READY" : dataQualitySufficient
                    ? directionAssessment.directionDataState() : "INSUFFICIENT_DATA");
            decision.setDataQualityScore(dataQualityScore);
            Map<String, Double> scoreMap = V41DecisionContractPolicy.scoreMap(scores);
            Integer evidenceReliability = integer(scoreMap.get(
                    V41DecisionContractPolicy.EVIDENCE_RELIABILITY));
            decision.setEvidenceReliability(evidenceReliability);
            V41DecisionContractPolicy.Metric opportunityMetric = V41DecisionContractPolicy.opportunityScore(
                    scores, evidenceReliability, conflict.getAiConflictScore(), externalContextBlocked ? 20 : 0);
            decision.setOpportunityScore(opportunityMetric.value());
            V41DecisionContractPolicy.Metric riskMetric = V41DecisionContractPolicy.riskScore(
                    scores, eventRisk(scoreMap), conflict.getAiConflictScore());
            decision.setRiskScore(riskMetric.value());
            decision.setOneHourOpportunityQuality(directionQuality(
                    directionAssessment.normalized1hDirectionScore()));
            decision.setFourHourTrendAlignment(fourHourAlignment(directionAssessment));
            decision.setNormalizationVersion(v41Properties.getNormalization().getVersion());
            decision.setScoreVersion(V41DecisionContractPolicy.SCORE_VERSION);
            decision.setDataQualityVersion(V41DecisionContractPolicy.DATA_QUALITY_VERSION);
            decision.setProviderMatrixVersion(v41Properties.getProviderMatrix().getVersion());
            decision.setRuleConfidence(userConfidenceLevel);
            decision.setRuleRisk(riskLevelLabel);
            decision.setTradeType("SPOT");
            String effectiveConfidence = decisionInputsSufficient
                    ? conflict.getAdjustedConfidence() != null ? conflict.getAdjustedConfidence() : confidenceLevel
                    : "LOW";
            String effectiveAiPlanMode = decisionInputsSufficient
                    && aiReview.getAiSupportCount() + aiReview.getAiObjectionCount() > 0
                    ? conflict.getPlanMode() : null;
            decision.setConfidenceLevel(effectiveConfidence);
            decision.setRiskLevel(riskLevelLabel);
            decision.setActionPriority(finalScore > actionPriorityHighMinScoreExclusive ? "HIGH" : "MEDIUM");
            decision.setConclusionSummary(conclusion);
            decision.setIsWorthOpening(effectiveWorthOpening && !confused.isDirectionalPushBlocked());
            decision.setMultiTfConvergence(multiTfLabel);
            decision.setMultiTimeframeDetails(multiTimeframeDetails);
            AiRoleResultsPayload.SynthesisPayload aiSynthesis = new AiRoleResultsPayload.SynthesisPayload(
                    null,
                    null,
                    null,
                    null,
                    false,
                    conflict.getLevel() != null ? conflict.getLevel().name() : null,
                    conflict.getAiConflictScore(),
                    conflict.getAdjustedConfidence(),
                    conflict.getRiskAdjustment(),
                    effectiveAiPlanMode,
                    AssetStateEnum.CONFUSED.equals(finalAssetState) || confused.isDirectionalPushBlocked(),
                    firstAiDowngradeReason(aiReview),
                    firstAiDowngradeReason(aiReview),
                    null);
            decision.setAiRoleResults(aiRoleResultsCodec.serialize(aiReview, ruleVersion, aiSynthesis));
            decision.setReviewReasons(reviewJson);
            decision.setAiConflictLevel(conflict.getLevel() != null ? conflict.getLevel().name() : null);
            decision.setAiConflictScore(conflict.getAiConflictScore());
            decision.setAiPlanMode(effectiveAiPlanMode);
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
            decision.setValidFrom(validFrom);
            decision.setExpiresAt(expiresAt);
            decision.setPushInvalidPriceBelow(pushInvalidPriceBelow);
            decision.setPushInvalidPriceAbove(pushInvalidPriceAbove);
            decision.setPushInvalidationSummary(pushInvalidationSummary);
            if (derivativesAssessment != null) {
                decision.setDerivativesStatus(derivativesAssessment.sourceStatus() == null
                        ? null : derivativesAssessment.sourceStatus().name());
                decision.setDerivativesFreshness(derivativesAssessment.freshnessStatus() == null
                        ? null : derivativesAssessment.freshnessStatus().name());
                decision.setDerivativesRequired(derivativesMandatory);
                decision.setDerivativesConfirmEligible(derivativesAssessment.confirmEligible());
                decision.setDerivativesPushMode(derivativesAssessment.pushMode());
                decision.setDerivativesReasonCodes(derivativesAssessment.reasonCodes());
                decision.setDerivativesProviderDataTime(derivativesAssessment.providerDataTime());
                decision.setDerivativesTraceId(derivativesAssessment.traceId());
            }
            applyExternalContext(decision, externalContextInput);

            logger.info("[AI决策] 生成完成 → {} | ConfidenceLevel = {} | Score = {} | MultiTF = {} | Worth Open: {} | aiConflict={}/{} | confused={}",
                    userMarketBias, decision.getConfidenceLevel(), finalScore, decision.getMultiTfConvergence(), decision.getIsWorthOpening(),
                    decision.getAiConflictLevel(), decision.getAiConflictScore(), decision.getConfusedScore());

            return decision;

        } catch (Exception e) {
            logger.error("[AI决策] 生成失败: {}", e.getMessage(), e);
            throw e;
        }
    }

    private int requireCandidatePromotionScore() {
        Integer value = v41Properties == null || v41Properties.getOpportunityState() == null
                ? null : v41Properties.getOpportunityState().getCandidatePromotionScore();
        if (value == null || value < 0 || value > 100) {
            throw new IllegalStateException(
                    "trade-model.fundamental-ai-v4-1.opportunity-state.candidate-promotion-score is required");
        }
        return value;
    }

    private List<String[]> readNormalizationWindow(String symbol, String timeframe, String traceId) {
        int lookback = v41Properties.getNormalization().getLookback();
        int minimum = v41Properties.getNormalization().getMinimumSampleCount();
        try {
            return ohlcvSnapshotSource.readClosedBars(symbol, timeframe, lookback, traceId);
        } catch (RuntimeException fullWindowUnavailable) {
            return ohlcvSnapshotSource.readClosedBars(symbol, timeframe, minimum, traceId);
        }
    }

    private static boolean directional(String value) {
        return MarketBiasPolicy.bullishFamily(value) || MarketBiasPolicy.bearishFamily(value);
    }

    private static boolean sameDirection(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.signum() != 0 && left.signum() == right.signum();
    }

    private static Integer averageScore(List<ScoreItemVO> scores) {
        if (scores == null || scores.isEmpty()) return null;
        return (int) Math.round(scores.stream().filter(java.util.Objects::nonNull)
                .map(ScoreItemVO::getScoreValue).filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue).average().orElse(0.0));
    }

    private static Integer integer(Double value) {
        return value == null ? null : (int) Math.round(value);
    }

    private static Integer eventRisk(Map<String, Double> scores) {
        Double alignment = scores.get(V41DecisionContractPolicy.EVENT_IMPACT);
        return alignment == null ? null : (int) Math.round(Math.max(0.0, 100.0 - alignment));
    }

    private static Integer directionQuality(BigDecimal normalizedScore) {
        return normalizedScore == null ? null
                : normalizedScore.abs().min(BigDecimal.valueOf(100)).intValue();
    }

    private static Integer fourHourAlignment(MarketBiasPolicy.DirectionAssessment assessment) {
        if (assessment.normalized4hDirectionScore() == null
                || assessment.normalized1hDirectionScore() == null) return null;
        if (assessment.normalized4hDirectionScore().signum()
                != assessment.normalized1hDirectionScore().signum()) return 0;
        BigDecimal fourHourStrength = assessment.normalized4hDirectionScore().abs();
        BigDecimal oneHourStrength = assessment.normalized1hDirectionScore().abs();
        return fourHourStrength.compareTo(oneHourStrength) >= 0 ? 100 : 80;
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

    private static int getIntInRange(Map<String, RuleConfigDO> cfgMap, String key, int defaultVal,
                                     int minimum, int maximum) {
        int value = getInt(cfgMap, key, defaultVal);
        return value < minimum || value > maximum ? defaultVal : value;
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

    private static AssetStateEnum failClosedDecisionInputState(AssetStateEnum syntheticState,
                                                                boolean decisionInputsSufficient) {
        if (decisionInputsSufficient || AssetStateEnum.CONFUSED.equals(syntheticState)
                || AssetStateEnum.COOLING.equals(syntheticState)
                || AssetStateEnum.INVALIDATED.equals(syntheticState)) {
            return syntheticState;
        }
        return AssetStateEnum.HIGH_RISK;
    }

    private static AssetStateEnum mergeDerivativesState(AssetStateEnum current,
                                                        DerivativesBusinessAssessment assessment) {
        if (assessment == null || assessment.opportunityState() == null) return current;
        if (current == AssetStateEnum.CONFUSED || current == AssetStateEnum.COOLING
                || current == AssetStateEnum.INVALIDATED) return current;
        if (assessment.opportunityState() == AssetStateEnum.HIGH_RISK) return AssetStateEnum.HIGH_RISK;
        if (current == AssetStateEnum.HIGH_RISK) return current;
        return assessment.opportunityState();
    }

    private static int cap100(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private static String downgradeConfidenceLevel(String confidence) {
        if ("HIGH".equalsIgnoreCase(confidence)) return "MEDIUM";
        return "LOW";
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
        request.setRequestTime(utcLocalNow());
        try {
            return aiDecisionOrchestratorService.review(request);
        } catch (Exception e) {
            AiOrchestratorResult fallback = ruleOnlyFallback(analysisId, traceId, "AI_ORCHESTRATOR_FAILED");
            fallback.setReasonCodes(List.of("AI_ORCHESTRATOR_FAILED"));
            return fallback;
        }
    }

    private AiOrchestratorResult ruleOnlyFallback(String analysisId, String traceId, String reasonCode) {
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setAnalysisId(analysisId);
        result.setTraceId(traceId);
        result.setOrchestrationMode(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        result.setReasonCodes(List.of(reasonCode));
        result.setCompletedAt(utcLocalNow());
        return result;
    }

    private LocalDateTime utcLocalNow() {
        return LocalDateTime.ofInstant(decisionClock.instant(), ZoneOffset.UTC);
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
