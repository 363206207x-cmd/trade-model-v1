package org.example.trademodel.service.impl;

import org.example.trademodel.market.RealMarketEnvironmentService;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.example.trademodel.vo.ScoreItemVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link AnalysisAssemblerServiceImpl#estimateDataQualityScore}：有效 evidence 条数三档 + sourceType 封顶。
 */
class AnalysisAssemblerServiceImplTest {

    private static final String HEURISTIC = "BINANCE_24H_HEURISTIC";
    private static final String SPOT_PERP_MIN = "BINANCE_SPOT_PERP_MIN_HEURISTIC";
    private static final String USDM_OI_MIN = "BINANCE_USDM_OI_MIN_HEURISTIC";
    private static final String SPOT_PERP_OI_MIN = "BINANCE_SPOT_PERP_OI_MIN_HEURISTIC";
    private static final String FALLBACK = "PLACEHOLDER_FALLBACK";

    /** 原条数逻辑会得到 85：有效 ev &gt;= 2；空 VO 不视为第二维排外条。 */
    private static List<EvidenceItemVO> twoEvidences() {
        return List.of(new EvidenceItemVO(), new EvidenceItemVO());
    }

    private static EvidenceItemVO defaultPriceStructureEvidence() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("价格结构");
        e.setDirection("NEUTRAL");
        e.setSource("SYSTEM_GENERATED");
        e.setDescription(
                "日内启发式价格结构代理：当前缺少 24h 涨跌幅标量，无法给出方向代理。"
                        + "该证据 = 日内启发式价格结构代理，非规格级 K 线结构模块。");
        return e;
    }

    /** 与 EvidenceServiceImpl 第二维 description 模板一致。 */
    private static EvidenceItemVO secondDimensionVolatilityEvidence() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("风险");
        e.setDirection("NEUTRAL");
        e.setSource("MARKET_HEURISTIC");
        e.setDescription("24h 价格振幅约 4.25%（中等波动）；口径：Binance 24h ticker 启发式。");
        return e;
    }

    private static EvidenceItemVO otherRiskEvidenceDifferentDescription() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("风险");
        e.setDirection("NEUTRAL");
        e.setSource("MARKET_HEURISTIC");
        e.setDescription("其它风险叙述（非振幅模板）。");
        return e;
    }

    /** 与 EvidenceServiceImpl OI 风险行同源模板（trim）。 */
    private static EvidenceItemVO openInterestExplanatoryEvidence() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("风险");
        e.setDirection("NEUTRAL");
        e.setSource("MARKET_HEURISTIC");
        e.setDescription(RealMarketEnvironmentService.buildOpenInterestAppendix(new BigDecimal("75797.837")).trim());
        return e;
    }

    private static EvidenceItemVO nonTemplateOiRiskEvidence() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("风险");
        e.setDirection("BULLISH");
        e.setSource("MARKET_HEURISTIC");
        e.setDescription("OI 24h 上升 28%，触发拥挤阈值。");
        return e;
    }

    /** 与 EvidenceServiceImpl Funding 行同源模板（trim）。 */
    private static EvidenceItemVO fundingExplanatoryEvidence() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("资金");
        e.setDirection("NEUTRAL");
        e.setSource("MARKET_HEURISTIC");
        e.setDescription(RealMarketEnvironmentService.buildFundingAppendix(new BigDecimal("0.0001")).trim());
        return e;
    }

    private static EvidenceItemVO otherFundingEvidenceDifferentDescription() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("资金");
        e.setDirection("NEUTRAL");
        e.setSource("MARKET_HEURISTIC");
        e.setDescription("其它资金叙述（非 Funding 附录模板）。");
        return e;
    }

    /** 与 EvidenceServiceImpl 当前最小杠杆 description 模板一致（低档位）。 */
    private static EvidenceItemVO leverageExplanatoryEvidenceLow() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("杠杆");
        e.setDirection("NEUTRAL");
        e.setSource("MARKET_HEURISTIC");
        e.setDescription(EvidenceServiceImpl.LEVERAGE_EVIDENCE_DESCRIPTION_LOW);
        return e;
    }

    private static EvidenceItemVO leverageExplanatoryEvidenceModerate() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("杠杆");
        e.setDirection("NEUTRAL");
        e.setSource("MARKET_HEURISTIC");
        e.setDescription(EvidenceServiceImpl.LEVERAGE_EVIDENCE_DESCRIPTION_MODERATE);
        return e;
    }

    private static EvidenceItemVO otherLeverageEvidenceDifferentDescription() {
        EvidenceItemVO e = new EvidenceItemVO();
        e.setEvidenceType("杠杆");
        e.setDirection("NEUTRAL");
        e.setSource("MARKET_HEURISTIC");
        e.setDescription("交易所账户杠杆上限说明（非当前窄模板）。");
        return e;
    }

    @Test
    void heuristic_keepsBase85_whenTwoOrMoreEvidence() {
        assertEquals(85, AnalysisAssemblerServiceImpl.estimateDataQualityScore(twoEvidences(), Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_stays55_whenDefaultPlusSecondDimensionVolatility_only() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), secondDimensionVolatilityEvidence());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_reaches85_whenTwoNonExcludableEvidences() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), otherRiskEvidenceDifferentDescription());
        assertEquals(85, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_stays55_whenDefaultPlusFundingExplanatory_only() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), fundingExplanatoryEvidence());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_stays55_whenDefaultVolatilityAndFundingExplanatory_allCarvedOut() {
        List<EvidenceItemVO> list = List.of(
                defaultPriceStructureEvidence(),
                secondDimensionVolatilityEvidence(),
                fundingExplanatoryEvidence());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_stays55_whenDefaultPlusLeverageExplanatory_only() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), leverageExplanatoryEvidenceLow());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_stays55_whenDefaultPlusLeverageExplanatory_moderateOnly() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), leverageExplanatoryEvidenceModerate());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_stays55_whenDefaultVolatilityFundingLeverageExplanatory_allCarvedOut() {
        List<EvidenceItemVO> list = List.of(
                defaultPriceStructureEvidence(),
                secondDimensionVolatilityEvidence(),
                fundingExplanatoryEvidence(),
                leverageExplanatoryEvidenceLow());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_stays55_whenDefaultPlusOpenInterestExplanatory_only() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), openInterestExplanatoryEvidence());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void usdmOiMin_reaches85_whenDefaultPlusOpenInterestExplanatory_andOiApplied() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), openInterestExplanatoryEvidence());
        assertEquals(85, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), USDM_OI_MIN));
    }

    @Test
    void heuristic_reaches85_whenDefaultPlusNonTemplateOiRiskEvidence() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), nonTemplateOiRiskEvidence());
        assertEquals(85, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_stays55_whenDefaultAndAllEnvironmentExplanatoryAnchorsIncludingOi() {
        List<EvidenceItemVO> list = List.of(
                defaultPriceStructureEvidence(),
                secondDimensionVolatilityEvidence(),
                fundingExplanatoryEvidence(),
                leverageExplanatoryEvidenceLow(),
                openInterestExplanatoryEvidence());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_reaches85_whenDefaultPlusNonTemplateLeverageEvidence() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), otherLeverageEvidenceDifferentDescription());
        assertEquals(85, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void heuristic_reaches85_whenDefaultPlusNonTemplateFundingEvidence() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), otherFundingEvidenceDifferentDescription());
        assertEquals(85, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), HEURISTIC));
    }

    @Test
    void fallback_caps85_to55_whenTwoOrMoreEvidence() {
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(twoEvidences(), Collections.emptyList(), FALLBACK));
    }

    @Test
    void fallback_stays55_whenDefaultPlusSecondDimensionVolatility() {
        List<EvidenceItemVO> list = List.of(defaultPriceStructureEvidence(), secondDimensionVolatilityEvidence());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(list, Collections.emptyList(), FALLBACK));
    }

    @Test
    void fallback_keeps35_whenBothListsEmpty() {
        assertEquals(35, AnalysisAssemblerServiceImpl.estimateDataQualityScore(Collections.emptyList(), Collections.emptyList(), FALLBACK));
    }

    @Test
    void fallback_keeps55_whenSingleEvidence() {
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(List.of(new EvidenceItemVO()), Collections.emptyList(), FALLBACK));
    }

    @Test
    void heuristic_keeps55_whenSingleEvidence() {
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(List.of(new EvidenceItemVO()), Collections.emptyList(), HEURISTIC));
    }

    @Test
    void spotPerpMinSource_sameDqTierAsSpotOnly_whenSingleEvidence() {
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(List.of(new EvidenceItemVO()), Collections.emptyList(), SPOT_PERP_MIN));
    }

    @Test
    void usdmOiMinSource_sameDqTierAsSpotOnly_whenSingleEvidence() {
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(List.of(new EvidenceItemVO()), Collections.emptyList(), USDM_OI_MIN));
    }

    @Test
    void spotPerpOiMinSource_sameDqTierAsSpotOnly_whenSingleEvidence() {
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(List.of(new EvidenceItemVO()), Collections.emptyList(), SPOT_PERP_OI_MIN));
    }

    @Test
    void marketEnvSourceType_spotPerp_whenFundingApplied() {
        MarketEnvironmentVO vo = new MarketEnvironmentVO();
        vo.setPerpFundingApplied(true);
        assertEquals(SPOT_PERP_MIN, AnalysisAssemblerServiceImpl.marketEnvSourceTypeForSuccessfulQuote(vo));
    }

    @Test
    void marketEnvSourceType_spotOnly_whenFundingNotApplied() {
        MarketEnvironmentVO vo = new MarketEnvironmentVO();
        vo.setPerpFundingApplied(false);
        vo.setOiApplied(false);
        assertEquals(HEURISTIC, AnalysisAssemblerServiceImpl.marketEnvSourceTypeForSuccessfulQuote(vo));
    }

    @Test
    void marketEnvSourceType_preservesNonBinanceExplicitSource() {
        MarketEnvironmentVO vo = new MarketEnvironmentVO();
        vo.setSourceType("OKX_24H_FALLBACK");
        vo.setPerpFundingApplied(true);
        vo.setOiApplied(true);
        assertEquals("OKX_24H_FALLBACK", AnalysisAssemblerServiceImpl.marketEnvSourceTypeForSuccessfulQuote(vo));
    }

    @Test
    void marketEnvSourceType_usdmOiMin_whenOiOnly() {
        MarketEnvironmentVO vo = new MarketEnvironmentVO();
        vo.setPerpFundingApplied(false);
        vo.setOiApplied(true);
        assertEquals(USDM_OI_MIN, AnalysisAssemblerServiceImpl.marketEnvSourceTypeForSuccessfulQuote(vo));
    }

    @Test
    void marketEnvSourceType_spotPerpOi_whenFundingAndOi() {
        MarketEnvironmentVO vo = new MarketEnvironmentVO();
        vo.setPerpFundingApplied(true);
        vo.setOiApplied(true);
        assertEquals(SPOT_PERP_OI_MIN, AnalysisAssemblerServiceImpl.marketEnvSourceTypeForSuccessfulQuote(vo));
    }

    @Test
    void marketEnvSourceType_spotPerp_whenFundingOnly() {
        MarketEnvironmentVO vo = new MarketEnvironmentVO();
        vo.setPerpFundingApplied(true);
        vo.setOiApplied(false);
        assertEquals(SPOT_PERP_MIN, AnalysisAssemblerServiceImpl.marketEnvSourceTypeForSuccessfulQuote(vo));
    }

    /** ev=0 且 sc&gt;0 时条数档为 55（非 85）；封顶不改变 55。 */
    @Test
    void fallback_unchanged55_whenTwoScoresNoEvidence() {
        List<ScoreItemVO> twoScores = List.of(new ScoreItemVO(), new ScoreItemVO());
        assertEquals(55, AnalysisAssemblerServiceImpl.estimateDataQualityScore(Collections.emptyList(), twoScores, FALLBACK));
    }
}
