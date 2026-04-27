package org.example.trademodel.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.math.BigDecimal;
import java.util.StringJoiner;

import org.example.trademodel.common.EvidenceTypeConstants;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.service.ScoreService;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreBriefVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.springframework.stereotype.Service;

@Service
public class ScoreServiceImpl implements ScoreService {
    private static final double TREND_BASE_SCORE = 50.0;
    private static final double TREND_BULL_BONUS = 15.0;
    private static final double TREND_BEAR_PENALTY = 15.0;
    private static final double EVIDENCE_BULL_BONUS = 5.0;
    private static final double EVIDENCE_BEAR_PENALTY = 5.0;
    private static final double CREDIBILITY_BASE_SCORE = 55.0;
    private static final double CREDIBILITY_SUMMARY_BONUS = 10.0;
    private static final double CREDIBILITY_ENV_FIELDS_BONUS = 5.0;
    private static final double CREDIBILITY_VALID_EVIDENCE_BONUS = 10.0;
    private static final double CREDIBILITY_PRICE_STRUCTURE_SIGNAL_BONUS = 10.0;
    private static final double CREDIBILITY_CONFLICT_PENALTY = 10.0;
    private static final double SCORE_MIN = 0.0;
    private static final double SCORE_MAX = 100.0;
    private static final String TREND_SCORE_TYPE = "趋势结构分";
    private static final String TREND_SCORE_DESCRIPTION = "基于市场环境摘要关键词的轻规则打分";
    private static final String CREDIBILITY_SCORE_TYPE = "综合可信度分";
    private static final String CREDIBILITY_SCORE_DESCRIPTION = "基于市场环境字段完整性与价格结构证据一致性的轻规则打分";
    private static final String FUNDING_SCORE_TYPE = "资金推动分";
    private static final String FUNDING_SCORE_DESCRIPTION = "基于 Funding 真值与价格结构方向一致性的轻规则打分";
    private static final String LEVERAGE_RISK_SCORE_TYPE = "杠杆风险分";
    private static final String LEVERAGE_RISK_SCORE_DESCRIPTION =
            "基于杠杆建议与波动体制的轻规则打分（最小真实扩展，非完整风险建模）";
    private static final String LIQUIDITY_QUALITY_SCORE_TYPE = "流动性质量分";
    private static final String LIQUIDITY_QUALITY_SCORE_DESCRIPTION =
            "基于24h振幅与波动体制的轻规则打分（最小真实扩展，非完整流动性模型）";
    private static final String SENTIMENT_TEMPERATURE_SCORE_TYPE = "情绪温度分";
    private static final String SENTIMENT_TEMPERATURE_SCORE_DESCRIPTION_PREFIX =
            "基于白名单输入的情绪温度轻规则打分（第一刀，不引入外部事件源）";
    private static final String MACRO_ENVIRONMENT_SCORE_TYPE = "宏观环境分";
    private static final String MACRO_ENVIRONMENT_SCORE_DESCRIPTION_PREFIX =
            "基于宏观白名单字段的轻规则评分（单项增量，不等于宏观模块完成，也不改变当前 decision 主路径）";
    private static final String EVENT_IMPACT_SCORE_TYPE = "事件冲击分";
    private static final String EVENT_IMPACT_SCORE_DESCRIPTION_PREFIX =
            "基于现有 event evidence 命中的轻规则评分（单项负向惩罚，不等于事件系统完成，也不改变当前 decision 主路径）";
    private static final String EVENT_IMPACT_DESCRIPTION_TEMPLATE_WITH_INPUT =
            "%s | 命中: %s; eventFactCount=%d; eventLatestTime=%s; eventReasonCode=%s; eventTriggerType=%s; eventVersion=%s; eventTraceId=%s";
    private static final String EVENT_IMPACT_DESCRIPTION_TEMPLATE_WITHOUT_INPUT =
            "%s | 命中: eventEvidence=%s";
    private static final String PRICE_STRUCTURE_EVIDENCE_TYPE = "价格结构";
    private static final double FUNDING_BASE_SCORE = 50.0;
    private static final double FUNDING_LIGHT_BONUS = 5.0;
    private static final double FUNDING_LIGHT_PENALTY = 5.0;
    private static final double FUNDING_MEDIUM_BONUS = 10.0;
    private static final double FUNDING_MEDIUM_PENALTY = 10.0;
    private static final double FUNDING_CONFLICT_PENALTY = 5.0;
    private static final BigDecimal FUNDING_MEDIUM_THRESHOLD = new BigDecimal("0.0001");
    private static final double LEVERAGE_RISK_BASE_SCORE = 50.0;
    private static final double LEVERAGE_LOW_PENALTY = 10.0;
    private static final double LEVERAGE_MODERATE_BONUS = 5.0;
    private static final double VOLATILITY_HIGH_PENALTY = 5.0;
    private static final double RANGE_HIGH_PENALTY = 5.0;
    private static final double RISK_MODE_ELEVATED_PENALTY = 5.0;
    private static final double RANGE_HIGH_THRESHOLD_PCT = 6.0;
    private static final double LIQUIDITY_QUALITY_BASE_SCORE = 50.0;
    private static final double LIQUIDITY_REGIME_NARROW_BONUS = 10.0;
    private static final double LIQUIDITY_REGIME_HIGH_PENALTY = 10.0;
    private static final double LIQUIDITY_RANGE_NARROW_BONUS = 5.0;
    private static final double LIQUIDITY_RANGE_HIGH_PENALTY = 5.0;
    private static final double LIQUIDITY_RANGE_NARROW_MAX_PCT = 2.0;
    private static final double LIQUIDITY_RANGE_HIGH_MIN_PCT = 6.0;
    private static final double SENTIMENT_BASE_SCORE = 50.0;
    private static final double SENTIMENT_PRICE_CHANGE_POSITIVE_BONUS = 10.0;
    private static final double SENTIMENT_PRICE_CHANGE_NEGATIVE_PENALTY = 10.0;
    private static final double SENTIMENT_VOLATILITY_HIGH_PENALTY = 10.0;
    private static final double SENTIMENT_VOLATILITY_NARROW_BONUS = 5.0;
    private static final double SENTIMENT_FUNDING_POSITIVE_BONUS = 5.0;
    private static final double SENTIMENT_FUNDING_NEGATIVE_PENALTY = 5.0;
    private static final double SENTIMENT_OI_FUNDING_SAME_SIGN_BONUS = 5.0;
    private static final double SENTIMENT_OI_FUNDING_OPPOSITE_SIGN_PENALTY = 5.0;
    private static final double SENTIMENT_PRICE_STRUCTURE_CONFLICT_PENALTY = 5.0;
    private static final double MACRO_ENVIRONMENT_BASE_SCORE = 50.0;
    private static final double MACRO_ENVIRONMENT_VOLATILITY_NARROW_BONUS = 10.0;
    private static final double MACRO_ENVIRONMENT_VOLATILITY_HIGH_PENALTY = 10.0;
    private static final double MACRO_ENVIRONMENT_RANGE_NARROW_BONUS = 5.0;
    private static final double MACRO_ENVIRONMENT_RANGE_HIGH_PENALTY = 5.0;
    private static final double MACRO_ENVIRONMENT_CROWDING_PENALTY = 5.0;
    private static final double MACRO_ENVIRONMENT_RANGE_NARROW_MAX_PCT = 2.0;
    private static final double MACRO_ENVIRONMENT_RANGE_HIGH_MIN_PCT = 6.0;
    private static final String EVENT_EVIDENCE_TYPE = "事件";
    private static final double EVENT_IMPACT_BASE_SCORE = 50.0;
    private static final double EVENT_IMPACT_HIT_PENALTY = 10.0;
    private static final double EVENT_IMPACT_HIGH_VOLATILITY_EXTRA_PENALTY = 5.0;
    private static final double EVENT_IMPACT_ELEVATED_RISK_EXTRA_PENALTY = 5.0;
    private static final double EVENT_IMPACT_CROWDING_EXTRA_PENALTY = 5.0;

    private final ScoreItemMapper scoreItemMapper;

    public ScoreServiceImpl(ScoreItemMapper scoreItemMapper) {
        this.scoreItemMapper = scoreItemMapper;
    }

    @Override
    public List<ScoreItemVO> buildScoreList(AssetAnalysisVO assetAnalysis, MarketEnvironmentVO marketEnv) {
        List<ScoreItemVO> list = new ArrayList<>();
        ScoreItemVO trendScore = new ScoreItemVO();
        trendScore.setScoreType(TREND_SCORE_TYPE);
        trendScore.setScoreValue(computeTrendStructureScore(assetAnalysis, marketEnv));
        trendScore.setWeight(1.0);
        trendScore.setDescription(TREND_SCORE_DESCRIPTION);
        list.add(trendScore);

        ScoreItemVO credibilityScore = new ScoreItemVO();
        credibilityScore.setScoreType(CREDIBILITY_SCORE_TYPE);
        credibilityScore.setScoreValue(computeOverallCredibilityScore(assetAnalysis, marketEnv));
        credibilityScore.setWeight(1.0);
        credibilityScore.setDescription(CREDIBILITY_SCORE_DESCRIPTION);
        list.add(credibilityScore);

        ScoreItemVO fundingScore = new ScoreItemVO();
        fundingScore.setScoreType(FUNDING_SCORE_TYPE);
        fundingScore.setScoreValue(computeFundingMomentumScore(assetAnalysis, marketEnv));
        fundingScore.setWeight(1.0);
        fundingScore.setDescription(FUNDING_SCORE_DESCRIPTION);
        list.add(fundingScore);

        ScoreItemVO leverageRiskScore = new ScoreItemVO();
        leverageRiskScore.setScoreType(LEVERAGE_RISK_SCORE_TYPE);
        leverageRiskScore.setScoreValue(computeLeverageRiskScore(marketEnv));
        leverageRiskScore.setWeight(1.0);
        leverageRiskScore.setDescription(LEVERAGE_RISK_SCORE_DESCRIPTION);
        list.add(leverageRiskScore);

        ScoreItemVO liquidityQualityScore = new ScoreItemVO();
        liquidityQualityScore.setScoreType(LIQUIDITY_QUALITY_SCORE_TYPE);
        liquidityQualityScore.setScoreValue(computeLiquidityQualityScore(marketEnv));
        liquidityQualityScore.setWeight(1.0);
        liquidityQualityScore.setDescription(LIQUIDITY_QUALITY_SCORE_DESCRIPTION);
        list.add(liquidityQualityScore);

        SentimentTemperatureEval sentimentEval = evaluateSentimentTemperature(assetAnalysis, marketEnv);
        ScoreItemVO sentimentTemperatureScore = new ScoreItemVO();
        sentimentTemperatureScore.setScoreType(SENTIMENT_TEMPERATURE_SCORE_TYPE);
        sentimentTemperatureScore.setScoreValue(sentimentEval.score);
        sentimentTemperatureScore.setWeight(1.0);
        sentimentTemperatureScore.setDescription(sentimentEval.description);
        list.add(sentimentTemperatureScore);

        MacroEnvironmentEval macroEnvironmentEval = evaluateMacroEnvironmentScore(marketEnv);
        ScoreItemVO macroEnvironmentScore = new ScoreItemVO();
        macroEnvironmentScore.setScoreType(MACRO_ENVIRONMENT_SCORE_TYPE);
        macroEnvironmentScore.setScoreValue(macroEnvironmentEval.score);
        macroEnvironmentScore.setWeight(1.0);
        macroEnvironmentScore.setDescription(macroEnvironmentEval.description);
        list.add(macroEnvironmentScore);

        EventImpactEval eventImpactEval = evaluateEventImpactScore(assetAnalysis, marketEnv);
        ScoreItemVO eventImpactScore = new ScoreItemVO();
        eventImpactScore.setScoreType(EVENT_IMPACT_SCORE_TYPE);
        eventImpactScore.setScoreValue(eventImpactEval.score);
        eventImpactScore.setWeight(1.0);
        eventImpactScore.setDescription(eventImpactEval.description);
        list.add(eventImpactScore);
        return list;
    }

    @Override
    public List<ScoreItemVO> buildScoreListFromEnvironment(MarketEnvironmentVO env) {
        return buildScoreList(new AssetAnalysisVO(), env);
    }

    @Override
    public List<ScoreBriefVO> listTopScoreBriefByAnalysisId(String analysisId) {
        if (analysisId == null || analysisId.isBlank()) {
            return Collections.emptyList();
        }
        List<ScoreBriefVO> rows = scoreItemMapper.selectTop3BriefByAnalysisId(analysisId.trim());
        return rows != null ? rows : Collections.emptyList();
    }

    private double computeTrendStructureScore(AssetAnalysisVO assetAnalysis, MarketEnvironmentVO marketEnv) {
        double score = TREND_BASE_SCORE;
        if (marketEnv != null && marketEnv.getSummary() != null && !marketEnv.getSummary().isBlank()) {
            String summary = marketEnv.getSummary().toLowerCase(Locale.ROOT);
            if (containsAny(summary, "上涨", "突破", "强势", "bull", "bullish")) {
                score += TREND_BULL_BONUS;
            }
            if (containsAny(summary, "下跌", "跌破", "弱势", "bear", "bearish")) {
                score -= TREND_BEAR_PENALTY;
            }
        }

        int evidenceSignal = extractPriceStructureSignal(assetAnalysis != null ? assetAnalysis.getEvidenceList() : null);
        if (evidenceSignal > 0) {
            score += EVIDENCE_BULL_BONUS;
        } else if (evidenceSignal < 0) {
            score -= EVIDENCE_BEAR_PENALTY;
        }
        if (score < SCORE_MIN) {
            return SCORE_MIN;
        }
        if (score > SCORE_MAX) {
            return SCORE_MAX;
        }
        return score;
    }

    private int extractPriceStructureSignal(List<EvidenceItemVO> evidenceList) {
        if (evidenceList == null || evidenceList.isEmpty()) {
            return 0;
        }
        boolean bullish = false;
        boolean bearish = false;
        for (EvidenceItemVO item : evidenceList) {
            if (item == null) {
                continue;
            }
            String type = item.getEvidenceType();
            if (type == null || !PRICE_STRUCTURE_EVIDENCE_TYPE.equals(type.trim())) {
                continue;
            }
            int dirSignal = signalFromPriceStructureDirection(item.getDirection());
            if (dirSignal != 0) {
                if (dirSignal > 0) {
                    bullish = true;
                } else {
                    bearish = true;
                }
                continue;
            }
            String desc = item.getDescription();
            if (desc == null || desc.isBlank()) {
                continue;
            }
            String normalized = desc.toLowerCase(Locale.ROOT);
            if (containsAny(normalized, "上涨", "突破", "强势", "bull", "bullish")) {
                bullish = true;
            }
            if (containsAny(normalized, "下跌", "跌破", "弱势", "bear", "bearish")) {
                bearish = true;
            }
        }
        if (bullish == bearish) {
            return 0;
        }
        return bullish ? 1 : -1;
    }

    /** BULLISH → +1，BEARISH → -1；NEUTRAL / 缺失 / 未知 → 0（交由 description 关键词 fallback）。 */
    private static int signalFromPriceStructureDirection(String direction) {
        if (direction == null || direction.isBlank()) {
            return 0;
        }
        String d = direction.trim();
        if (EvidenceTypeConstants.EVIDENCE_DIRECTION_BULLISH.equals(d)) {
            return 1;
        }
        if (EvidenceTypeConstants.EVIDENCE_DIRECTION_BEARISH.equals(d)) {
            return -1;
        }
        return 0;
    }

    private double computeOverallCredibilityScore(AssetAnalysisVO assetAnalysis, MarketEnvironmentVO marketEnv) {
        double score = CREDIBILITY_BASE_SCORE;
        String summary = marketEnv != null && marketEnv.getSummary() != null ? marketEnv.getSummary().trim() : "";
        if (!summary.isEmpty()) {
            score += CREDIBILITY_SUMMARY_BONUS;
        }
        String environmentType = marketEnv != null && marketEnv.getEnvironmentType() != null
                ? marketEnv.getEnvironmentType().trim()
                : "";
        String riskMode = marketEnv != null && marketEnv.getRiskMode() != null
                ? marketEnv.getRiskMode().trim()
                : "";
        if (!environmentType.isEmpty() && !riskMode.isEmpty()) {
            score += CREDIBILITY_ENV_FIELDS_BONUS;
        }

        List<EvidenceItemVO> evidenceList = assetAnalysis != null ? assetAnalysis.getEvidenceList() : null;
        if (hasAnyValidEvidence(evidenceList)) {
            score += CREDIBILITY_VALID_EVIDENCE_BONUS;
        }
        int priceStructureSignal = extractPriceStructureSignal(evidenceList);
        if (priceStructureSignal != 0) {
            score += CREDIBILITY_PRICE_STRUCTURE_SIGNAL_BONUS;
        }
        if (isSummaryEvidenceDirectionalConflict(summary, priceStructureSignal)) {
            score -= CREDIBILITY_CONFLICT_PENALTY;
        }
        return clampScore(score);
    }

    private boolean hasAnyValidEvidence(List<EvidenceItemVO> evidenceList) {
        if (evidenceList == null || evidenceList.isEmpty()) {
            return false;
        }
        for (EvidenceItemVO item : evidenceList) {
            if (item == null) {
                continue;
            }
            String type = item.getEvidenceType();
            String desc = item.getDescription();
            if (type != null && !type.trim().isEmpty() && desc != null && !desc.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isSummaryEvidenceDirectionalConflict(String summary, int priceStructureSignal) {
        if (priceStructureSignal == 0 || summary == null || summary.isBlank()) {
            return false;
        }
        String normalized = summary.toLowerCase(Locale.ROOT);
        boolean summaryBullish = containsAny(normalized, "上涨", "突破", "强势", "bull", "bullish");
        boolean summaryBearish = containsAny(normalized, "下跌", "跌破", "弱势", "bear", "bearish");
        if (summaryBullish == summaryBearish) {
            return false;
        }
        int summarySignal = summaryBullish ? 1 : -1;
        return summarySignal != priceStructureSignal;
    }

    private double computeFundingMomentumScore(AssetAnalysisVO assetAnalysis, MarketEnvironmentVO marketEnv) {
        double score = FUNDING_BASE_SCORE;
        if (marketEnv == null || !Boolean.TRUE.equals(marketEnv.getPerpFundingApplied())
                || marketEnv.getLastFundingRate() == null) {
            return score;
        }
        BigDecimal fundingRate = marketEnv.getLastFundingRate();
        BigDecimal absRate = fundingRate.abs();
        boolean mediumTier = absRate.compareTo(FUNDING_MEDIUM_THRESHOLD) >= 0;
        if (fundingRate.signum() > 0) {
            score += mediumTier ? FUNDING_MEDIUM_BONUS : FUNDING_LIGHT_BONUS;
        } else if (fundingRate.signum() < 0) {
            score -= mediumTier ? FUNDING_MEDIUM_PENALTY : FUNDING_LIGHT_PENALTY;
        }
        int priceStructureSignal = extractPriceStructureSignal(assetAnalysis != null ? assetAnalysis.getEvidenceList() : null);
        if (isFundingPriceStructureConflict(fundingRate, priceStructureSignal)) {
            score -= FUNDING_CONFLICT_PENALTY;
        }
        return clampScore(score);
    }

    private double computeLeverageRiskScore(MarketEnvironmentVO marketEnv) {
        double score = LEVERAGE_RISK_BASE_SCORE;
        if (marketEnv == null) {
            return score;
        }
        String leverageSuggestion = marketEnv.getLeverageSuggestion() != null
                ? marketEnv.getLeverageSuggestion().trim()
                : "";
        if ("low_leverage".equals(leverageSuggestion)) {
            score -= LEVERAGE_LOW_PENALTY;
        } else if ("moderate_leverage".equals(leverageSuggestion)) {
            score += LEVERAGE_MODERATE_BONUS;
        }

        String volatilityRegime = marketEnv.getVolatilityRegime() != null
                ? marketEnv.getVolatilityRegime().trim()
                : "";
        if ("高波动".equals(volatilityRegime)) {
            score -= VOLATILITY_HIGH_PENALTY;
        } else if (marketEnv.getRangePct24h() != null && marketEnv.getRangePct24h() >= RANGE_HIGH_THRESHOLD_PCT) {
            score -= RANGE_HIGH_PENALTY;
        }

        String riskMode = marketEnv.getRiskMode() != null ? marketEnv.getRiskMode().trim() : "";
        if ("elevated".equalsIgnoreCase(riskMode)) {
            score -= RISK_MODE_ELEVATED_PENALTY;
        }
        return clampScore(score);
    }

    private double computeLiquidityQualityScore(MarketEnvironmentVO marketEnv) {
        double score = LIQUIDITY_QUALITY_BASE_SCORE;
        if (marketEnv == null) {
            return score;
        }

        String volatilityRegime = marketEnv.getVolatilityRegime() != null
                ? marketEnv.getVolatilityRegime().trim()
                : "";
        if ("窄幅".equals(volatilityRegime)) {
            score += LIQUIDITY_REGIME_NARROW_BONUS;
        } else if ("高波动".equals(volatilityRegime)) {
            score -= LIQUIDITY_REGIME_HIGH_PENALTY;
        }

        Double rangePct24h = marketEnv.getRangePct24h();
        if (rangePct24h != null) {
            if (rangePct24h < LIQUIDITY_RANGE_NARROW_MAX_PCT) {
                score += LIQUIDITY_RANGE_NARROW_BONUS;
            } else if (rangePct24h >= LIQUIDITY_RANGE_HIGH_MIN_PCT) {
                score -= LIQUIDITY_RANGE_HIGH_PENALTY;
            }
        }
        return clampScore(score);
    }

    /**
     * 情绪温度分第一刀：仅使用已落库/同源白名单输入，不引入外部事件源。
     */
    private SentimentTemperatureEval evaluateSentimentTemperature(AssetAnalysisVO assetAnalysis, MarketEnvironmentVO marketEnv) {
        double score = SENTIMENT_BASE_SCORE;
        StringJoiner applied = new StringJoiner("; ");

        Integer priceSignal = null;
        Integer fundingSignal = null;

        BigDecimal pct = marketEnv != null ? marketEnv.getPriceChangePercent24h() : null;
        if (pct != null) {
            int sign = pct.signum();
            if (sign > 0) {
                score += SENTIMENT_PRICE_CHANGE_POSITIVE_BONUS;
                priceSignal = 1;
                applied.add("priceChange24h>0:+10");
            } else if (sign < 0) {
                score -= SENTIMENT_PRICE_CHANGE_NEGATIVE_PENALTY;
                priceSignal = -1;
                applied.add("priceChange24h<0:-10");
            } else {
                applied.add("priceChange24h=0:+0");
            }
        } else {
            applied.add("priceChange24h:NA");
        }

        String volatilityRegime = marketEnv != null && marketEnv.getVolatilityRegime() != null
                ? marketEnv.getVolatilityRegime().trim()
                : "";
        if ("高波动".equals(volatilityRegime)) {
            score -= SENTIMENT_VOLATILITY_HIGH_PENALTY;
            applied.add("volatility=高波动:-10");
        } else if ("窄幅".equals(volatilityRegime)) {
            score += SENTIMENT_VOLATILITY_NARROW_BONUS;
            applied.add("volatility=窄幅:+5");
        } else if (!volatilityRegime.isEmpty()) {
            applied.add("volatility=" + volatilityRegime + ":+0");
        } else {
            applied.add("volatility:NA");
        }

        boolean fundingAvailable = marketEnv != null
                && Boolean.TRUE.equals(marketEnv.getPerpFundingApplied())
                && marketEnv.getLastFundingRate() != null;
        if (fundingAvailable) {
            int sign = marketEnv.getLastFundingRate().signum();
            if (sign > 0) {
                score += SENTIMENT_FUNDING_POSITIVE_BONUS;
                fundingSignal = 1;
                applied.add("funding>0:+5");
            } else if (sign < 0) {
                score -= SENTIMENT_FUNDING_NEGATIVE_PENALTY;
                fundingSignal = -1;
                applied.add("funding<0:-5");
            } else {
                fundingSignal = 0;
                applied.add("funding=0:+0");
            }
        } else {
            applied.add("funding:NA");
        }

        boolean oiAvailable = marketEnv != null
                && Boolean.TRUE.equals(marketEnv.getOiApplied())
                && marketEnv.getOpenInterestDelta() != null;
        if (oiAvailable && fundingSignal != null && fundingSignal != 0) {
            int oiSign = marketEnv.getOpenInterestDelta().signum();
            if (oiSign == 0) {
                applied.add("oiDelta=0:+0");
            } else if (oiSign == fundingSignal) {
                score += SENTIMENT_OI_FUNDING_SAME_SIGN_BONUS;
                applied.add("oiDelta同向funding:+5");
            } else {
                score -= SENTIMENT_OI_FUNDING_OPPOSITE_SIGN_PENALTY;
                applied.add("oiDelta反向funding:-5");
            }
        } else if (oiAvailable) {
            applied.add("oiDelta可用但funding不可判:+0");
        } else {
            applied.add("oiDelta:NA");
        }

        int scoreDirectionalSignal = directionalSignalForSentiment(priceSignal, fundingSignal);
        int priceStructureSignal = extractPriceStructureSignal(assetAnalysis != null ? assetAnalysis.getEvidenceList() : null);
        if (priceStructureSignal != 0 && scoreDirectionalSignal != 0 && priceStructureSignal != scoreDirectionalSignal) {
            score -= SENTIMENT_PRICE_STRUCTURE_CONFLICT_PENALTY;
            applied.add("priceStructure方向冲突:-5");
        } else {
            applied.add("priceStructure冲突:无");
        }

        double clamped = clampScore(score);
        String description = SENTIMENT_TEMPERATURE_SCORE_DESCRIPTION_PREFIX + " | 命中: " + applied;
        return new SentimentTemperatureEval(clamped, description);
    }

    private static int directionalSignalForSentiment(Integer priceSignal, Integer fundingSignal) {
        if (priceSignal == null || priceSignal == 0) {
            return fundingSignal != null ? fundingSignal : 0;
        }
        if (fundingSignal == null || fundingSignal == 0) {
            return priceSignal;
        }
        if (priceSignal.equals(fundingSignal)) {
            return priceSignal;
        }
        return 0;
    }

    private static final class SentimentTemperatureEval {
        private final double score;
        private final String description;

        private SentimentTemperatureEval(double score, String description) {
            this.score = score;
            this.description = description;
        }
    }

    private MacroEnvironmentEval evaluateMacroEnvironmentScore(MarketEnvironmentVO marketEnv) {
        double score = MACRO_ENVIRONMENT_BASE_SCORE;
        StringJoiner applied = new StringJoiner("; ");

        String volatilityRegime = marketEnv != null && marketEnv.getVolatilityRegime() != null
                ? marketEnv.getVolatilityRegime().trim()
                : "";
        if ("窄幅".equals(volatilityRegime)) {
            score += MACRO_ENVIRONMENT_VOLATILITY_NARROW_BONUS;
            applied.add("volatilityRegime=窄幅:+10");
        } else if ("高波动".equals(volatilityRegime)) {
            score -= MACRO_ENVIRONMENT_VOLATILITY_HIGH_PENALTY;
            applied.add("volatilityRegime=高波动:-10");
        } else if (!volatilityRegime.isEmpty()) {
            applied.add("volatilityRegime=" + volatilityRegime + ":+0");
        } else {
            applied.add("volatilityRegime:NA");
        }

        Double rangePct24h = marketEnv != null ? marketEnv.getRangePct24h() : null;
        if (rangePct24h != null) {
            if (rangePct24h < MACRO_ENVIRONMENT_RANGE_NARROW_MAX_PCT) {
                score += MACRO_ENVIRONMENT_RANGE_NARROW_BONUS;
                applied.add("rangePct24h<2:+5");
            } else if (rangePct24h >= MACRO_ENVIRONMENT_RANGE_HIGH_MIN_PCT) {
                score -= MACRO_ENVIRONMENT_RANGE_HIGH_PENALTY;
                applied.add("rangePct24h>=6:-5");
            } else {
                applied.add("rangePct24h中性:+0");
            }
        } else {
            applied.add("rangePct24h:NA");
        }

        String derivativesCrowdingState = marketEnv != null && marketEnv.getDerivativesCrowdingState() != null
                ? marketEnv.getDerivativesCrowdingState().trim()
                : "";
        if ("CROWDED_LONG".equals(derivativesCrowdingState)) {
            score -= MACRO_ENVIRONMENT_CROWDING_PENALTY;
            applied.add("derivativesCrowdingState=CROWDED_LONG:-5");
        } else if ("CROWDED_SHORT".equals(derivativesCrowdingState)) {
            score -= MACRO_ENVIRONMENT_CROWDING_PENALTY;
            applied.add("derivativesCrowdingState=CROWDED_SHORT:-5");
        } else if (!derivativesCrowdingState.isEmpty()) {
            applied.add("derivativesCrowdingState=" + derivativesCrowdingState + ":+0");
        } else {
            applied.add("derivativesCrowdingState:NA");
        }

        double clamped = clampScore(score);
        String description = MACRO_ENVIRONMENT_SCORE_DESCRIPTION_PREFIX + " | 命中: " + applied;
        return new MacroEnvironmentEval(clamped, description);
    }

    private static final class MacroEnvironmentEval {
        private final double score;
        private final String description;

        private MacroEnvironmentEval(double score, String description) {
            this.score = score;
            this.description = description;
        }
    }

    private EventImpactEval evaluateEventImpactScore(AssetAnalysisVO assetAnalysis, MarketEnvironmentVO marketEnv) {
        double score = EVENT_IMPACT_BASE_SCORE;
        EventImpactInputVO input = assetAnalysis != null ? assetAnalysis.getEventImpactInput() : null;
        boolean hit = input != null
                ? Boolean.TRUE.equals(input.getEventFactHit())
                : hasEventEvidence(assetAnalysis != null ? assetAnalysis.getEvidenceList() : null);
        StringJoiner applied = new StringJoiner("; ");
        if (hit) {
            score -= EVENT_IMPACT_HIT_PENALTY;
            applied.add("eventFactHit:hit:-10");
            if (input != null && safeInputCount(input.getEventFactCount()) >= EvidenceTypeConstants.EVENT_IMPACT_MULTI_HIT_THRESHOLD) {
                score -= EvidenceTypeConstants.EVENT_IMPACT_MULTI_HIT_EXTRA_PENALTY;
                applied.add("eventFactCount>=3:-5");
            }
            if (input != null && isSevereTriggerType(input.getEventTriggerType())) {
                score -= EvidenceTypeConstants.EVENT_IMPACT_SEVERE_TRIGGER_EXTRA_PENALTY;
                applied.add("eventTriggerType=SEVERE:-5");
            }
            if (isHighVolatilityRegime(marketEnv)) {
                score -= EVENT_IMPACT_HIGH_VOLATILITY_EXTRA_PENALTY;
                applied.add("marketVolatility=高波动:-5");
            }
            if (isElevatedRiskMode(marketEnv)) {
                score -= EVENT_IMPACT_ELEVATED_RISK_EXTRA_PENALTY;
                applied.add("marketRiskMode=elevated:-5");
            }
            if (isDerivativesCrowdingState(marketEnv)) {
                score -= EVENT_IMPACT_CROWDING_EXTRA_PENALTY;
                applied.add("marketCrowding=CROWDED:-5");
            }
        } else {
            applied.add("eventFactHit:miss:+0");
        }
        double clamped = clampScore(score);
        String description = input != null
                ? buildEventImpactDescriptionWithInput(input, applied.toString())
                : buildEventImpactDescriptionWithoutInput(hit);
        return new EventImpactEval(clamped, description);
    }

    private static String buildEventImpactDescriptionWithInput(EventImpactInputVO input, String applied) {
        return String.format(
                Locale.ROOT,
                EVENT_IMPACT_DESCRIPTION_TEMPLATE_WITH_INPUT,
                EVENT_IMPACT_SCORE_DESCRIPTION_PREFIX,
                applied,
                safeInputCount(input.getEventFactCount()),
                safeInputText(input.getEventLatestTime()),
                safeInputText(input.getEventReasonCode()),
                safeInputText(input.getEventTriggerType()),
                safeInputText(input.getEventVersion()),
                safeInputText(input.getEventTraceId())
        );
    }

    private static String buildEventImpactDescriptionWithoutInput(boolean hit) {
        return String.format(
                Locale.ROOT,
                EVENT_IMPACT_DESCRIPTION_TEMPLATE_WITHOUT_INPUT,
                EVENT_IMPACT_SCORE_DESCRIPTION_PREFIX,
                hit ? "hit:-10" : "miss:+0"
        );
    }

    private static boolean isSevereTriggerType(String triggerType) {
        if (triggerType == null || triggerType.isBlank()) {
            return false;
        }
        String normalized = triggerType.trim().toUpperCase(Locale.ROOT);
        return EvidenceTypeConstants.EVENT_IMPACT_SEVERE_TRIGGER_TYPES.contains(normalized);
    }

    private static boolean isHighVolatilityRegime(MarketEnvironmentVO marketEnv) {
        if (marketEnv == null || marketEnv.getVolatilityRegime() == null) {
            return false;
        }
        return "高波动".equals(marketEnv.getVolatilityRegime().trim());
    }

    private static boolean isElevatedRiskMode(MarketEnvironmentVO marketEnv) {
        if (marketEnv == null || marketEnv.getRiskMode() == null) {
            return false;
        }
        return "elevated".equalsIgnoreCase(marketEnv.getRiskMode().trim());
    }

    private static boolean isDerivativesCrowdingState(MarketEnvironmentVO marketEnv) {
        if (marketEnv == null || marketEnv.getDerivativesCrowdingState() == null) {
            return false;
        }
        String state = marketEnv.getDerivativesCrowdingState().trim();
        return "CROWDED_LONG".equals(state) || "CROWDED_SHORT".equals(state);
    }

    private static int safeInputCount(Integer count) {
        return count != null && count > 0 ? count : 0;
    }

    private static String safeInputText(Object value) {
        return value == null ? "NA" : String.valueOf(value);
    }

    private boolean hasEventEvidence(List<EvidenceItemVO> evidenceList) {
        if (evidenceList == null || evidenceList.isEmpty()) {
            return false;
        }
        for (EvidenceItemVO item : evidenceList) {
            if (item == null || item.getEvidenceType() == null) {
                continue;
            }
            if (EVENT_EVIDENCE_TYPE.equals(item.getEvidenceType().trim())) {
                return true;
            }
        }
        return false;
    }

    private static final class EventImpactEval {
        private final double score;
        private final String description;

        private EventImpactEval(double score, String description) {
            this.score = score;
            this.description = description;
        }
    }

    private boolean isFundingPriceStructureConflict(BigDecimal fundingRate, int priceStructureSignal) {
        if (fundingRate == null || priceStructureSignal == 0 || fundingRate.signum() == 0) {
            return false;
        }
        int fundingSignal = fundingRate.signum() > 0 ? 1 : -1;
        return fundingSignal != priceStructureSignal;
    }

    private double clampScore(double score) {
        if (score < SCORE_MIN) {
            return SCORE_MIN;
        }
        if (score > SCORE_MAX) {
            return SCORE_MAX;
        }
        return score;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
