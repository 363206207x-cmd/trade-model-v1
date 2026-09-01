package org.example.trademodel.service.impl;

import org.example.trademodel.common.EvidenceTypeConstants;
import org.example.trademodel.mapper.ScoreItemMapper;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.EventImpactInputVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreBriefVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("smoke")
class ScoreServiceImplTest {

    @Mock
    private ScoreItemMapper scoreItemMapper;

    @Test
    void buildScoreList_returnsTheFrozenEightScoresWithStableUniqueIds() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);

        List<ScoreItemVO> result = service.buildScoreList(
                new AssetAnalysisVO(), new MarketEnvironmentVO());

        assertThat(result)
                .extracting(ScoreItemVO::getScoreType)
                .containsExactlyInAnyOrder(
                        "趋势结构分",
                        "证据可信度分",
                        "资金推动分",
                        "杠杆风险分",
                        "流动性质量分",
                        "情绪温度分",
                        "宏观环境分",
                        "事件冲击分");
        assertThat(result)
                .extracting(ScoreItemVO::getScoreId)
                .doesNotContainNull()
                .allSatisfy(scoreId -> assertThat(scoreId).isNotBlank());
        assertThat(result.stream().map(ScoreItemVO::getScoreId).collect(java.util.stream.Collectors.toSet()))
                .hasSize(8);
        assertThat(result)
                .extracting(ScoreItemVO::getWeight)
                .doesNotContainNull();
    }

    @Test
    void buildScoreList_returnsTrendStructureScoreAboveBaselineForBullishSummary() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary("市场强势上涨并突破关键阻力位");

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO trend = pickByType(result, "趋势结构分");

        assertThat(result).hasSize(8);
        assertThat(trend).isNotNull();
        assertThat(pickByType(result, "情绪温度分")).isNotNull();
        assertThat(trend.getScoreValue()).isGreaterThan(50.0);
    }

    @Test
    void buildScoreList_returnsTrendStructureScoreBelowBaselineForBearishSummary() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary("价格下跌并跌破结构支撑，整体偏 bear");

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO trend = pickByType(result, "趋势结构分");

        assertThat(result).hasSize(8);
        assertThat(trend).isNotNull();
        assertThat(pickByType(result, "情绪温度分")).isNotNull();
        assertThat(trend.getScoreValue()).isLessThan(50.0);
    }

    @Test
    void buildScoreList_returnsNullWhenTrendInputsAreMissing() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO blankEnv = new MarketEnvironmentVO();
        blankEnv.setSummary("   ");

        List<ScoreItemVO> blankSummaryResult = service.buildScoreList(new AssetAnalysisVO(), blankEnv);
        List<ScoreItemVO> nullEnvResult = service.buildScoreList(new AssetAnalysisVO(), null);
        ScoreItemVO blankTrend = pickByType(blankSummaryResult, "趋势结构分");
        ScoreItemVO nullTrend = pickByType(nullEnvResult, "趋势结构分");

        assertThat(blankSummaryResult).hasSize(8);
        assertThat(blankTrend).isNotNull();
        assertThat(pickByType(blankSummaryResult, "情绪温度分")).isNotNull();
        assertThat(blankTrend.getScoreValue()).isNull();
        assertThat(blankTrend.getDescription()).contains("INSUFFICIENT_DATA");
        assertThat(nullEnvResult).hasSize(8);
        assertThat(nullTrend).isNotNull();
        assertThat(pickByType(nullEnvResult, "情绪温度分")).isNotNull();
        assertThat(nullTrend.getScoreValue()).isNull();
        assertThat(nullTrend.getDescription()).contains("INSUFFICIENT_DATA");
    }

    @Test
    void buildScoreList_usesPriceStructureEvidenceBullishSignal_whenSummaryNeutral() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO evidence = new EvidenceItemVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDescription("价格突破关键阻力，结构偏强");
        analysis.setEvidenceList(List.of(evidence));
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary("中性描述，不含方向关键词");

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO trend = pickByType(result, "趋势结构分");

        assertThat(result).hasSize(8);
        assertThat(trend).isNotNull();
        assertThat(pickByType(result, "情绪温度分")).isNotNull();
        assertThat(trend.getScoreValue()).isGreaterThan(50.0);
    }

    @Test
    void buildScoreList_prefersPriceStructureDirection_overDescriptionKeywords() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO evidence = new EvidenceItemVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDirection("BULLISH");
        evidence.setDescription("日内启发式价格结构代理：24h 涨跌约 +1.20%（Binance 24h ticker）；口径：启发式。");
        analysis.setEvidenceList(List.of(evidence));
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary("neutral compact text without directional tokens");

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO trend = pickByType(result, "趋势结构分");

        assertThat(trend.getScoreValue()).isGreaterThan(50.0);
    }

    @Test
    void buildScoreList_usesPriceStructureEvidenceBearishSignal_whenSummaryNeutral() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO evidence = new EvidenceItemVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDescription("价格跌破支撑，结构转弱");
        analysis.setEvidenceList(List.of(evidence));
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary("中性描述，不含方向关键词");

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO trend = pickByType(result, "趋势结构分");

        assertThat(result).hasSize(8);
        assertThat(trend).isNotNull();
        assertThat(pickByType(result, "情绪温度分")).isNotNull();
        assertThat(trend.getScoreValue()).isLessThan(50.0);
    }

    @Test
    void buildScoreList_withoutEvidence_keepsExistingSummaryLogic() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO noEvidenceAnalysis = new AssetAnalysisVO();
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary("bull breakout");

        List<ScoreItemVO> result = service.buildScoreList(noEvidenceAnalysis, env);
        ScoreItemVO trend = pickByType(result, "趋势结构分");

        assertThat(result).hasSize(8);
        assertThat(trend).isNotNull();
        assertThat(pickByType(result, "情绪温度分")).isNotNull();
        assertThat(trend.getScoreValue()).isEqualTo(65.0);
    }

    @Test
    void buildScoreList_setsSentimentTemperatureNull_whenNoWhitelistInputs() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        MarketEnvironmentVO env = new MarketEnvironmentVO();

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO sentiment = pickByType(result, "情绪温度分");

        assertThat(result).hasSize(8);
        assertThat(sentiment).isNotNull();
        assertThat(sentiment.getScoreValue()).isNull();
        assertThat(sentiment.getDescription()).contains("INSUFFICIENT_DATA");
    }

    @Test
    void buildScoreList_setsSentimentTemperatureAbove50_whenPriceChangePositive() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPriceChangePercent24h(new BigDecimal("1.20"));

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO sentiment = pickByType(result, "情绪温度分");

        assertThat(sentiment).isNotNull();
        assertThat(sentiment.getScoreValue()).isGreaterThan(50.0);
    }

    @Test
    void buildScoreList_setsSentimentTemperatureBelow50_whenPriceChangeNegative() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPriceChangePercent24h(new BigDecimal("-1.20"));

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO sentiment = pickByType(result, "情绪温度分");

        assertThat(sentiment).isNotNull();
        assertThat(sentiment.getScoreValue()).isLessThan(50.0);
    }

    @Test
    void buildScoreList_appliesSentimentHighVolatilityPenalty() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setVolatilityRegime("高波动");

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO sentiment = pickByType(result, "情绪温度分");

        assertThat(sentiment).isNotNull();
        assertThat(sentiment.getScoreValue()).isEqualTo(40.0);
    }

    @Test
    void buildScoreList_appliesSentimentNarrowVolatilityBonus() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setVolatilityRegime("窄幅");

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO sentiment = pickByType(result, "情绪温度分");

        assertThat(sentiment).isNotNull();
        assertThat(sentiment.getScoreValue()).isEqualTo(55.0);
    }

    @Test
    void buildScoreList_locksFundingAdjustmentToPlusOrMinus5_forSentimentTemperature() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO pos = new MarketEnvironmentVO();
        pos.setPerpFundingApplied(true);
        pos.setLastFundingRate(new BigDecimal("0.0001"));
        MarketEnvironmentVO neg = new MarketEnvironmentVO();
        neg.setPerpFundingApplied(true);
        neg.setLastFundingRate(new BigDecimal("-0.0001"));

        Double posScore = pickByType(service.buildScoreList(new AssetAnalysisVO(), pos), "情绪温度分").getScoreValue();
        Double negScore = pickByType(service.buildScoreList(new AssetAnalysisVO(), neg), "情绪温度分").getScoreValue();

        assertThat(posScore).isEqualTo(55.0);
        assertThat(negScore).isEqualTo(45.0);
    }

    @Test
    void buildScoreList_appliesSentimentOiFundingSynergyPlus5_whenSameDirection() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPerpFundingApplied(true);
        env.setLastFundingRate(new BigDecimal("0.0001"));
        env.setOiApplied(true);
        env.setOpenInterestDelta(new BigDecimal("12"));

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO sentiment = pickByType(result, "情绪温度分");

        assertThat(sentiment).isNotNull();
        assertThat(sentiment.getScoreValue()).isEqualTo(60.0);
    }

    @Test
    void buildScoreList_appliesSentimentOiFundingOppositePenaltyMinus5() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPerpFundingApplied(true);
        env.setLastFundingRate(new BigDecimal("0.0001"));
        env.setOiApplied(true);
        env.setOpenInterestDelta(new BigDecimal("-8"));

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO sentiment = pickByType(result, "情绪温度分");

        assertThat(sentiment).isNotNull();
        assertThat(sentiment.getScoreValue()).isEqualTo(50.0);
    }

    @Test
    void buildScoreList_appliesSentimentConflictPenaltyMinus5_whenPriceStructureDirectionConflicts() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO evidence = new EvidenceItemVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDirection("BEARISH");
        analysis.setEvidenceList(List.of(evidence));
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPriceChangePercent24h(new BigDecimal("1.20"));
        env.setPerpFundingApplied(true);
        env.setLastFundingRate(new BigDecimal("0.0001"));

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO sentiment = pickByType(result, "情绪温度分");

        assertThat(sentiment).isNotNull();
        assertThat(sentiment.getScoreValue()).isEqualTo(60.0);
    }

    @Test
    void buildScoreList_keepsSentimentWithinClampBounds_forExtremeWhitelistedCombination() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPriceChangePercent24h(new BigDecimal("-99.99"));
        env.setVolatilityRegime("高波动");
        env.setPerpFundingApplied(true);
        env.setLastFundingRate(new BigDecimal("-0.1000"));
        env.setOiApplied(true);
        env.setOpenInterestDelta(new BigDecimal("999999"));
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO evidence = new EvidenceItemVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDirection("BULLISH");
        analysis.setEvidenceList(List.of(evidence));

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO sentiment = pickByType(result, "情绪温度分");

        assertThat(sentiment).isNotNull();
        assertThat(sentiment.getScoreValue()).isBetween(0.0, 100.0);
    }

    @Test
    void buildScoreList_doesNotInferEvidenceReliabilityFromLegacySummaryFields() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO evidence = new EvidenceItemVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDescription("价格突破关键阻力，结构偏强");
        analysis.setEvidenceList(List.of(evidence));
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary("bull breakout on key level");
        env.setEnvironmentType("trend_market");
        env.setRiskMode("normal");

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO credibility = pickByType(result, "证据可信度分");

        assertThat(credibility).isNotNull();
        assertThat(credibility.getScoreValue()).isNull();
        assertThat(credibility.getDescription()).contains("SourceQuality");
    }

    @Test
    void buildScoreList_setsEvidenceReliabilityNull_whenInputsMissing() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        MarketEnvironmentVO env = new MarketEnvironmentVO();

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO credibility = pickByType(result, "证据可信度分");

        assertThat(credibility).isNotNull();
        assertThat(credibility.getScoreValue()).isNull();
        assertThat(credibility.getDescription()).contains("INSUFFICIENT_DATA");
    }

    @Test
    void buildScoreList_doesNotUseDirectionalConflictAsEvidenceReliabilityFallback() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO evidence = new EvidenceItemVO();
        evidence.setEvidenceType("价格结构");
        evidence.setDescription("价格跌破支撑，结构转弱");
        analysis.setEvidenceList(List.of(evidence));
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary("bull breakout continues");
        env.setEnvironmentType("trend_market");
        env.setRiskMode("normal");

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO credibility = pickByType(result, "证据可信度分");

        assertThat(credibility).isNotNull();
        assertThat(credibility.getScoreValue()).isNull();
        assertThat(credibility.getDescription()).contains("CrossSourceConsistency");
    }

    @Test
    void buildScoreList_keepsTrendStructureScoreLogicUnchanged_asRegression() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setSummary("bull breakout");

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO trend = pickByType(result, "趋势结构分");

        assertThat(trend).isNotNull();
        assertThat(trend.getScoreValue()).isEqualTo(65.0);
    }

    @Test
    void buildScoreList_setsFundingMomentumNull_whenFundingNotApplied() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPerpFundingApplied(false);

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO funding = pickByType(result, "资金推动分");

        assertThat(funding).isNotNull();
        assertThat(funding.getScoreValue()).isNull();
        assertThat(funding.getDescription()).contains("INSUFFICIENT_DATA");
    }

    @Test
    void buildScoreList_setsFundingMomentumHigher_whenFundingPositiveMediumAndNoConflict() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO price = new EvidenceItemVO();
        price.setEvidenceType("价格结构");
        price.setDirection("BULLISH");
        analysis.setEvidenceList(List.of(price));
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPerpFundingApplied(true);
        env.setLastFundingRate(new java.math.BigDecimal("0.0002"));

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO funding = pickByType(result, "资金推动分");

        assertThat(funding).isNotNull();
        assertThat(funding.getScoreValue()).isEqualTo(60.0);
    }

    @Test
    void buildScoreList_appliesFundingConflictPenalty_whenPriceStructureOpposesFundingDirection() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO price = new EvidenceItemVO();
        price.setEvidenceType("价格结构");
        price.setDirection("BULLISH");
        analysis.setEvidenceList(List.of(price));
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setPerpFundingApplied(true);
        env.setLastFundingRate(new java.math.BigDecimal("-0.0002"));

        List<ScoreItemVO> result = service.buildScoreList(analysis, env);
        ScoreItemVO funding = pickByType(result, "资金推动分");

        assertThat(funding).isNotNull();
        assertThat(funding.getScoreValue()).isEqualTo(35.0);
    }

    @Test
    void buildScoreList_setsLeverageRiskLower_whenLowLeverageAndHighVolatility() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setLeverageSuggestion("low_leverage");
        env.setVolatilityRegime("高波动");
        env.setRiskMode("elevated");

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO leverageRisk = pickByType(result, "杠杆风险分");

        assertThat(leverageRisk).isNotNull();
        assertThat(leverageRisk.getScoreValue()).isEqualTo(30.0);
    }

    @Test
    void buildScoreList_setsLeverageRiskHigher_whenModerateLeverageAndNormalRisk() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setLeverageSuggestion("moderate_leverage");
        env.setVolatilityRegime("中等波动");
        env.setRiskMode("normal");

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO leverageRisk = pickByType(result, "杠杆风险分");

        assertThat(leverageRisk).isNotNull();
        assertThat(leverageRisk.getScoreValue()).isEqualTo(55.0);
    }

    @Test
    void buildScoreList_setsLiquidityQualityHigher_whenNarrowRegimeAndRangeBelow2Percent() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setVolatilityRegime("窄幅");
        env.setRangePct24h(1.5);

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO liquidity = pickByType(result, "流动性质量分");

        assertThat(liquidity).isNotNull();
        assertThat(liquidity.getScoreValue()).isEqualTo(65.0);
    }

    @Test
    void buildScoreList_setsLiquidityQualityLower_whenHighVolatilityAndRangeAtLeast6Percent() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setVolatilityRegime("高波动");
        env.setRangePct24h(6.2);

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO liquidity = pickByType(result, "流动性质量分");

        assertThat(liquidity).isNotNull();
        assertThat(liquidity.getScoreValue()).isEqualTo(35.0);
    }

    @Test
    void buildScoreList_setsLiquidityQualityNull_whenLiquidityInputsMissing() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO liquidity = pickByType(result, "流动性质量分");

        assertThat(liquidity).isNotNull();
        assertThat(liquidity.getScoreValue()).isNull();
        assertThat(liquidity.getDescription()).contains("INSUFFICIENT_DATA");
    }

    @Test
    void buildScoreList_setsMacroEnvironmentScoreHigher_whenNarrowAndRangeBelow2Percent() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setVolatilityRegime("窄幅");
        env.setRangePct24h(1.5);

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO macro = pickByType(result, "宏观环境分");

        assertThat(result).hasSize(8);
        assertThat(macro).isNotNull();
        assertThat(macro.getScoreValue()).isEqualTo(65.0);
    }

    @Test
    void buildScoreList_setsMacroEnvironmentScoreLower_whenHighVolatilityAndRangeAtLeast6Percent() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setVolatilityRegime("高波动");
        env.setRangePct24h(6.0);

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO macro = pickByType(result, "宏观环境分");

        assertThat(result).hasSize(8);
        assertThat(macro).isNotNull();
        assertThat(macro.getScoreValue()).isEqualTo(35.0);
    }

    @Test
    void buildScoreList_appliesMacroEnvironmentCrowdingPenaltyMinus5_whenCrowdedLongOrShort() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO crowdedLong = new MarketEnvironmentVO();
        crowdedLong.setVolatilityRegime("窄幅");
        crowdedLong.setRangePct24h(1.5);
        crowdedLong.setDerivativesCrowdingState("CROWDED_LONG");
        MarketEnvironmentVO crowdedShort = new MarketEnvironmentVO();
        crowdedShort.setVolatilityRegime("窄幅");
        crowdedShort.setRangePct24h(1.5);
        crowdedShort.setDerivativesCrowdingState("CROWDED_SHORT");

        Double crowdedLongScore = pickByType(
                service.buildScoreList(new AssetAnalysisVO(), crowdedLong), "宏观环境分").getScoreValue();
        Double crowdedShortScore = pickByType(
                service.buildScoreList(new AssetAnalysisVO(), crowdedShort), "宏观环境分").getScoreValue();

        assertThat(crowdedLongScore).isEqualTo(60.0);
        assertThat(crowdedShortScore).isEqualTo(60.0);
    }

    @Test
    void buildScoreList_setsMacroEnvironmentScoreNull_whenMacroWhitelistInputsMissing() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();

        List<ScoreItemVO> result = service.buildScoreList(new AssetAnalysisVO(), env);
        ScoreItemVO macro = pickByType(result, "宏观环境分");

        assertThat(result).hasSize(8);
        assertThat(macro).isNotNull();
        assertThat(macro.getScoreValue()).isNull();
        assertThat(macro.getDescription()).contains("INSUFFICIENT_DATA");
    }

    @Test
    void buildScoreList_keepsMacroEnvironmentRangeBoundaryStable_at2And6Percent() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO at2 = new MarketEnvironmentVO();
        at2.setRangePct24h(2.0);
        MarketEnvironmentVO at6 = new MarketEnvironmentVO();
        at6.setRangePct24h(6.0);

        Double at2Score = pickByType(service.buildScoreList(new AssetAnalysisVO(), at2), "宏观环境分").getScoreValue();
        Double at6Score = pickByType(service.buildScoreList(new AssetAnalysisVO(), at6), "宏观环境分").getScoreValue();

        assertThat(at2Score).isEqualTo(50.0);
        assertThat(at6Score).isEqualTo(45.0);
    }

    @Test
    void buildScoreList_doesNotApplyMacroCrowdingPenalty_forUnknownCrowdingState() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setVolatilityRegime("窄幅");
        env.setRangePct24h(1.5);
        env.setDerivativesCrowdingState("BALANCED");

        ScoreItemVO macro = pickByType(service.buildScoreList(new AssetAnalysisVO(), env), "宏观环境分");

        assertThat(macro).isNotNull();
        assertThat(macro.getScoreValue()).isEqualTo(65.0);
    }

    @Test
    void buildScoreList_setsEventImpactScoreLower_whenEventEvidenceExists() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO event = new EvidenceItemVO();
        event.setEvidenceType("事件");
        event.setDescription("检测到 Hot Reset 事件：triggerType=HOT_RESET。");
        analysis.setEvidenceList(List.of(event));

        List<ScoreItemVO> result = service.buildScoreList(analysis, new MarketEnvironmentVO());
        ScoreItemVO eventImpact = pickByType(result, "事件冲击分");

        assertThat(result).hasSize(8);
        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(40.0);
    }

    @Test
    void buildScoreList_setsEventImpactNull_whenNoEventFactExists() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO nonEvent = new EvidenceItemVO();
        nonEvent.setEvidenceType("价格结构");
        nonEvent.setDescription("结构中性。");
        analysis.setEvidenceList(List.of(nonEvent));

        List<ScoreItemVO> result = service.buildScoreList(analysis, new MarketEnvironmentVO());
        ScoreItemVO eventImpact = pickByType(result, "事件冲击分");

        assertThat(result).hasSize(8);
        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isNull();
        assertThat(eventImpact.getDescription()).contains("INSUFFICIENT_DATA");
    }

    @Test
    void buildScoreList_setsEventImpactNull_whenEvidenceListMissing() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();

        List<ScoreItemVO> result = service.buildScoreList(analysis, new MarketEnvironmentVO());
        ScoreItemVO eventImpact = pickByType(result, "事件冲击分");

        assertThat(result).hasSize(8);
        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isNull();
        assertThat(eventImpact.getDescription()).contains("INSUFFICIENT_DATA");
    }

    @Test
    void buildScoreList_usesEventImpactInputContract_whenProvided() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.TRUE);
        input.setEventFactCount(2);
        input.setEventReasonCode("CONFUSED_HIGH_MTF_MISALIGNED");
        input.setEventTriggerType("HOT_RESET");
        input.setEventVersion(2);
        input.setEventTraceId("trace-hot-2");
        analysis.setEventImpactInput(input);

        List<ScoreItemVO> result = service.buildScoreList(analysis, new MarketEnvironmentVO());
        ScoreItemVO eventImpact = pickByType(result, "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(40.0);
        assertThat(eventImpact.getDescription()).contains("eventFactHit:hit:-10");
        assertThat(eventImpact.getDescription()).contains("eventFactCount=2");
        assertThat(eventImpact.getDescription()).contains("eventReasonCode=CONFUSED_HIGH_MTF_MISALIGNED");
    }

    @Test
    void buildScoreList_prefersEventImpactInputContract_overEvidenceFallback() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO event = new EvidenceItemVO();
        event.setEvidenceType("事件");
        event.setDescription("检测到 Hot Reset 事件");
        analysis.setEvidenceList(List.of(event));
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.FALSE);
        input.setEventFactCount(0);
        analysis.setEventImpactInput(input);

        List<ScoreItemVO> result = service.buildScoreList(analysis, new MarketEnvironmentVO());
        ScoreItemVO eventImpact = pickByType(result, "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(50.0);
        assertThat(eventImpact.getDescription()).contains("eventFactHit:miss:+0");
    }

    @Test
    void buildScoreList_keepsEventImpactDescriptionSnapshot_stableForInputContract() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.TRUE);
        input.setEventFactCount(3);
        input.setEventLatestTime(LocalDateTime.of(2026, 4, 27, 16, 21, 0));
        input.setEventReasonCode("CONFUSED_HIGH_MTF_MISALIGNED");
        input.setEventTriggerType("CIRCUIT_BREAKER");
        input.setEventVersion(2);
        input.setEventTraceId("trace-stable-001");
        analysis.setEventImpactInput(input);

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, new MarketEnvironmentVO()), "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getDescription()).isEqualTo(
                "基于现有 event evidence 命中的轻规则评分（单项负向惩罚，不等于事件系统完成，也不改变当前 decision 主路径） | 命中: "
                        + "eventFactHit:hit:-10; eventFactCount>=3:-5; eventTriggerType=SEVERE:-5; "
                        + "eventFactCount=3; eventLatestTime=2026-04-27T16:21; "
                        + "eventReasonCode=CONFUSED_HIGH_MTF_MISALIGNED; eventTriggerType=CIRCUIT_BREAKER; "
                        + "eventVersion=2; eventTraceId=trace-stable-001");
    }

    @Test
    void buildScoreList_keepsEventImpactDescriptionSnapshot_stableForEvidenceFallback() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO event = new EvidenceItemVO();
        event.setEvidenceType("事件");
        event.setDescription("trigger by fallback");
        analysis.setEvidenceList(List.of(event));

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, new MarketEnvironmentVO()), "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getDescription()).isEqualTo(
                "基于现有 event evidence 命中的轻规则评分（单项负向惩罚，不等于事件系统完成，也不改变当前 decision 主路径） | 命中: eventEvidence=hit:-10");
    }

    @Test
    void buildScoreList_keepsEventImpactSnapshot_stable_forRealisticAnalysisInputContractCase() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId("analysis-btcusdt-20260427-1624-a");
        analysis.setSymbol("BTCUSDT");
        analysis.setTimeframe("1m");
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.TRUE);
        input.setEventFactCount(4);
        input.setEventLatestTime(LocalDateTime.of(2026, 4, 27, 16, 24, 0));
        input.setEventReasonCode("CONFUSED_HIGH_MTF_MISALIGNED");
        input.setEventTriggerType("LIQUIDATION_CASCADE");
        input.setEventVersion(3);
        input.setEventTraceId("trace-20260427-1624-a");
        analysis.setEventImpactInput(input);

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, new MarketEnvironmentVO()), "事件冲击分");

        assertThat(analysis.getAnalysisId()).isEqualTo("analysis-btcusdt-20260427-1624-a");
        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(30.0);
        assertThat(eventImpact.getDescription()).isEqualTo(
                "基于现有 event evidence 命中的轻规则评分（单项负向惩罚，不等于事件系统完成，也不改变当前 decision 主路径） | 命中: "
                        + "eventFactHit:hit:-10; eventFactCount>=3:-5; eventTriggerType=SEVERE:-5; "
                        + "eventFactCount=4; eventLatestTime=2026-04-27T16:24; "
                        + "eventReasonCode=CONFUSED_HIGH_MTF_MISALIGNED; eventTriggerType=LIQUIDATION_CASCADE; "
                        + "eventVersion=3; eventTraceId=trace-20260427-1624-a");
    }

    @Test
    void buildScoreList_keepsEventImpactSnapshot_stable_forRealisticAnalysisEvidenceFallbackCase() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId("analysis-ethusdt-20260427-1624-b");
        analysis.setSymbol("ETHUSDT");
        analysis.setTimeframe("5m");
        EvidenceItemVO event = new EvidenceItemVO();
        event.setEvidenceType("  事件  ");
        event.setDescription("fallback event hit for realistic snapshot");
        analysis.setEvidenceList(List.of(event));

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, new MarketEnvironmentVO()), "事件冲击分");

        assertThat(analysis.getAnalysisId()).isEqualTo("analysis-ethusdt-20260427-1624-b");
        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(40.0);
        assertThat(eventImpact.getDescription()).isEqualTo(
                "基于现有 event evidence 命中的轻规则评分（单项负向惩罚，不等于事件系统完成，也不改变当前 decision 主路径） | 命中: eventEvidence=hit:-10");
    }

    @Test
    void buildScoreList_appliesExtraEventImpactPenalty_whenEventFactCountAtLeast3() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.TRUE);
        input.setEventFactCount(EvidenceTypeConstants.EVENT_IMPACT_MULTI_HIT_THRESHOLD);
        analysis.setEventImpactInput(input);

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, new MarketEnvironmentVO()), "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(35.0);
        assertThat(eventImpact.getDescription()).contains("eventFactCount>=3:-5");
    }

    @Test
    void buildScoreList_appliesExtraEventImpactPenalty_whenTriggerTypeIsSevere() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.TRUE);
        input.setEventFactCount(1);
        input.setEventTriggerType("CIRCUIT_BREAKER");
        analysis.setEventImpactInput(input);

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, new MarketEnvironmentVO()), "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(35.0);
        assertThat(eventImpact.getDescription()).contains("eventTriggerType=SEVERE:-5");
    }

    @Test
    void buildScoreList_appliesSevereTriggerPenalty_caseInsensitiveAndTrimmed() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.TRUE);
        input.setEventTriggerType("  exchange_outage  ");
        analysis.setEventImpactInput(input);

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, new MarketEnvironmentVO()), "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(35.0);
        assertThat(eventImpact.getDescription()).contains("eventTriggerType=SEVERE:-5");
    }

    @Test
    void buildScoreList_appliesMarketStatePenalties_whenEventHitAndMarketIsStressed() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.TRUE);
        analysis.setEventImpactInput(input);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setVolatilityRegime("高波动");
        env.setRiskMode("elevated");
        env.setDerivativesCrowdingState("CROWDED_LONG");

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, env), "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(25.0);
        assertThat(eventImpact.getDescription()).contains("marketVolatility=高波动:-5");
        assertThat(eventImpact.getDescription()).contains("marketRiskMode=elevated:-5");
        assertThat(eventImpact.getDescription()).contains("marketCrowding=CROWDED:-5");
    }

    @Test
    void buildScoreList_doesNotApplyMarketStatePenalties_whenEventMissEvenIfMarketIsStressed() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.FALSE);
        analysis.setEventImpactInput(input);
        MarketEnvironmentVO env = new MarketEnvironmentVO();
        env.setVolatilityRegime("高波动");
        env.setRiskMode("elevated");
        env.setDerivativesCrowdingState("CROWDED_SHORT");

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, env), "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(50.0);
        assertThat(eventImpact.getDescription()).doesNotContain("marketVolatility=高波动:-5");
        assertThat(eventImpact.getDescription()).doesNotContain("marketRiskMode=elevated:-5");
        assertThat(eventImpact.getDescription()).doesNotContain("marketCrowding=CROWDED:-5");
    }

    @Test
    void buildScoreList_sanitizesEventImpactInputFields_whenContractValuesMissingOrNonPositive() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EventImpactInputVO input = new EventImpactInputVO();
        input.setEventFactHit(Boolean.TRUE);
        input.setEventFactCount(-3);
        analysis.setEventImpactInput(input);

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, new MarketEnvironmentVO()), "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(40.0);
        assertThat(eventImpact.getDescription()).contains("eventFactCount=0");
        assertThat(eventImpact.getDescription()).contains("eventLatestTime=NA");
        assertThat(eventImpact.getDescription()).contains("eventReasonCode=NA");
        assertThat(eventImpact.getDescription()).contains("eventTriggerType=NA");
        assertThat(eventImpact.getDescription()).contains("eventVersion=NA");
        assertThat(eventImpact.getDescription()).contains("eventTraceId=NA");
    }

    @Test
    void buildScoreList_recognizesEventEvidenceType_withTrimmedWhitespace() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        EvidenceItemVO event = new EvidenceItemVO();
        event.setEvidenceType("  事件  ");
        event.setDescription("trimmed event evidence");
        analysis.setEvidenceList(List.of(event));

        ScoreItemVO eventImpact = pickByType(service.buildScoreList(analysis, new MarketEnvironmentVO()), "事件冲击分");

        assertThat(eventImpact).isNotNull();
        assertThat(eventImpact.getScoreValue()).isEqualTo(40.0);
    }

    @Test
    void listTopScoreBriefByAnalysisId_returnsRowsWhenExists() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        ScoreBriefVO row = new ScoreBriefVO();
        row.setScoreType("综合评分");
        row.setScoreValue(88.0);
        when(scoreItemMapper.selectTop3BriefByAnalysisId("ana-1")).thenReturn(List.of(row));

        List<ScoreBriefVO> result = service.listTopScoreBriefByAnalysisId("ana-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScoreType()).isEqualTo("综合评分");
        assertThat(result.get(0).getScoreValue()).isEqualTo(88.0);
    }

    @Test
    void listTopScoreBriefByAnalysisId_returnsEmptyListWhenNoData() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);
        when(scoreItemMapper.selectTop3BriefByAnalysisId("ana-empty")).thenReturn(Collections.emptyList());

        List<ScoreBriefVO> result = service.listTopScoreBriefByAnalysisId("ana-empty");

        assertThat(result).isEmpty();
    }

    @Test
    void listTopScoreBriefByAnalysisId_returnsEmptyListAndSkipsMapperWhenBlank() {
        ScoreServiceImpl service = new ScoreServiceImpl(scoreItemMapper);

        List<ScoreBriefVO> result = service.listTopScoreBriefByAnalysisId("   ");

        assertThat(result).isEmpty();
        verify(scoreItemMapper, never()).selectTop3BriefByAnalysisId(org.mockito.ArgumentMatchers.anyString());
    }

    private static ScoreItemVO pickByType(List<ScoreItemVO> items, String type) {
        return items.stream().filter(it -> type.equals(it.getScoreType())).findFirst().orElse(null);
    }
}
