package org.example.trademodel.service.impl;

import org.example.trademodel.common.EvidenceTypeConstants;
import org.example.trademodel.entity.HotResetEventDO;
import org.example.trademodel.mapper.EvidenceItemMapper;
import org.example.trademodel.mapper.HotResetEventMapper;
import org.example.trademodel.vo.AssetAnalysisVO;
import org.example.trademodel.vo.EvidenceBriefVO;
import org.example.trademodel.vo.EvidenceItemVO;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.example.trademodel.market.RealMarketEnvironmentService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("smoke")
class EvidenceServiceImplTest {

    @Mock
    private EvidenceItemMapper evidenceItemMapper;
    @Mock
    private HotResetEventMapper hotResetEventMapper;

    @Test
    void listTopEvidenceBriefByAnalysisId_returnsRowsWhenExists() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        EvidenceBriefVO row = new EvidenceBriefVO();
        row.setEvidenceType("价格结构");
        row.setDescription("趋势延续");
        when(evidenceItemMapper.selectTop3BriefByAnalysisId("ana-1")).thenReturn(List.of(row));

        List<EvidenceBriefVO> result = service.listTopEvidenceBriefByAnalysisId("ana-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEvidenceType()).isEqualTo("价格结构");
        assertThat(result.get(0).getDescription()).isEqualTo("趋势延续");
    }

    @Test
    void listTopEvidenceBriefByAnalysisId_returnsEmptyListWhenNoData() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        when(evidenceItemMapper.selectTop3BriefByAnalysisId("ana-empty")).thenReturn(Collections.emptyList());

        List<EvidenceBriefVO> result = service.listTopEvidenceBriefByAnalysisId("ana-empty");

        assertThat(result).isEmpty();
    }

    @Test
    void buildEvidence_generatesOnlyAllowedEvidenceTypeAndDirection() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), marketEnvironment());

        assertThat(result).isNotEmpty();
        assertThat(result)
                .extracting(EvidenceItemVO::getEvidenceType)
                .allMatch(EvidenceTypeConstants::isAllowed);
        assertThat(result)
                .extracting(EvidenceItemVO::getDirection)
                .allMatch(EvidenceTypeConstants::isAllowedDirection);
        assertThat(result)
                .extracting(EvidenceItemVO::getSource)
                .allMatch(EvidenceTypeConstants::isAllowedSource);
        assertThat(result).allSatisfy(item -> {
            assertThat(item.getCurrentValue()).isNotBlank();
            assertThat(item.getChangeFromBaseline()).isNotBlank();
            assertThat(item.getStrength()).isNotNull();
            assertThat(item.getConfidence()).isNotNull();
            assertThat(item.getSourceProvider()).isNotBlank();
            assertThat(item.getSourceReference()).isNotBlank();
            assertThat(item.getSourceTraceId()).isNotBlank();
            assertThat(item.getObservedAt()).isNotNull();
            assertThat(item.getFreshness()).isEqualTo("FRESH");
        });
    }

    @Test
    void determinePriceStructureDirection_neutralInsideEpsilon() {
        assertThat(EvidenceServiceImpl.determinePriceStructureDirection(new BigDecimal("0.03")))
                .isEqualTo(EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL);
        assertThat(EvidenceServiceImpl.determinePriceStructureDirection(new BigDecimal("-0.04")))
                .isEqualTo(EvidenceTypeConstants.EVIDENCE_DIRECTION_NEUTRAL);
    }

    @Test
    void buildEvidence_priceStructureBullish_whenPctAboveEpsilon() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setPriceChangePercent24h(new BigDecimal("1.5"));

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        EvidenceItemVO ps = result.get(0);
        assertThat(ps.getEvidenceType()).isEqualTo("价格结构");
        assertThat(ps.getDirection()).isEqualTo("BULLISH");
        assertThat(ps.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(ps.getDescription()).contains("日内启发式价格结构代理");
        assertThat(ps.getDescription()).contains("+1.50%");
        assertThat(ps.getDescription()).contains("该证据 = 日内启发式价格结构代理，非规格级 K 线结构模块");
        assertThat(ps.getDescription()).doesNotContain("默认证据");
    }

    @Test
    void buildEvidence_priceStructureBearish_whenPctBelowNegativeEpsilon() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setPriceChangePercent24h(new BigDecimal("-2.25"));

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        EvidenceItemVO ps = result.get(0);
        assertThat(ps.getDirection()).isEqualTo("BEARISH");
        assertThat(ps.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(ps.getDescription()).contains("-2.25%");
    }

    @Test
    void buildEvidence_priceStructureNeutral_whenPctInsideEpsilon() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setPriceChangePercent24h(new BigDecimal("0.03"));

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        EvidenceItemVO ps = result.get(0);
        assertThat(ps.getDirection()).isEqualTo("NEUTRAL");
        assertThat(ps.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(ps.getDescription()).contains("+0.03%");
    }

    @Test
    void buildEvidence_failsClosedWithoutPriceChangeFact() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), null);

        assertThat(result).isEmpty();
    }

    @Test
    void buildEvidence_appendsVolatilityRiskEvidence_whenSecondDimensionPresent() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setRangePct24h(4.25);
        env.setVolatilityRegime("中等波动");

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(3);
        EvidenceItemVO macro = result.get(1);
        assertThat(macro.getEvidenceType()).isEqualTo("宏观");
        EvidenceItemVO vol = result.get(2);
        assertThat(vol.getEvidenceType()).isEqualTo("风险");
        assertThat(vol.getDirection()).isEqualTo("NEUTRAL");
        assertThat(vol.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(vol.getDescription()).isEqualTo("24h 价格振幅约 4.25%（中等波动）；口径：Binance 24h ticker 启发式。");
    }

    @Test
    void buildEvidence_noVolatilityEvidence_whenRangePctMissing() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setVolatilityRegime("中等波动");

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(1);
    }

    @Test
    void buildEvidence_noVolatilityEvidence_whenVolatilityRegimeMissing() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setRangePct24h(3.0);

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(1);
    }

    @Test
    void buildEvidence_noVolatilityEvidence_whenVolatilityRegimeBlank() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setRangePct24h(3.0);
        env.setVolatilityRegime("   ");

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(1);
    }

    @Test
    void buildEvidence_appendsFundingEvidence_whenPerpFundingAppliedWithRate() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        BigDecimal rate = new BigDecimal("0.0001");
        env.setPerpFundingApplied(true);
        env.setLastFundingRate(rate);

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(2);
        EvidenceItemVO fund = result.get(1);
        assertThat(fund.getEvidenceType()).isEqualTo("资金");
        assertThat(fund.getDirection()).isEqualTo("NEUTRAL");
        assertThat(fund.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(fund.getDescription()).isEqualTo(RealMarketEnvironmentService.buildFundingAppendix(rate).trim());
    }

    @Test
    void buildEvidence_noFundingEvidence_whenPerpFundingAppliedButRateMissing() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setPerpFundingApplied(true);

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(1);
    }

    @Test
    void buildEvidence_appendsOpenInterestRiskEvidence_whenOiAppliedWithValue() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        BigDecimal oi = new BigDecimal("75797.837");
        env.setOiApplied(true);
        env.setLastOpenInterest(oi);
        env.setOpenInterestDelta(new BigDecimal("125.5"));

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(2);
        EvidenceItemVO oiRow = result.get(1);
        assertThat(oiRow.getEvidenceType()).isEqualTo("风险");
        assertThat(oiRow.getDirection()).isEqualTo("NEUTRAL");
        assertThat(oiRow.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(oiRow.getDescription()).isEqualTo(RealMarketEnvironmentService.buildOpenInterestAppendix(oi).trim());
    }

    @Test
    void buildEvidence_noOpenInterestEvidence_whenOiAppliedButValueMissing() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setOiApplied(true);

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(1);
    }

    @Test
    void buildEvidence_noOpenInterestEvidence_whenOiNotApplied() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setOiApplied(false);
        env.setLastOpenInterest(new BigDecimal("1000"));

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(1);
    }

    @Test
    void buildEvidence_appendsLeverageEvidence_whenLeverageSuggestionLow() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setLeverageSuggestion("low_leverage");

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(2);
        EvidenceItemVO lev = result.get(1);
        assertThat(lev.getEvidenceType()).isEqualTo("杠杆");
        assertThat(lev.getDirection()).isEqualTo("NEUTRAL");
        assertThat(lev.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(lev.getDescription()).isEqualTo("低杠杆建议；口径：Binance 24h 启发式。");
    }

    @Test
    void buildEvidence_appendsLeverageEvidence_whenLeverageSuggestionModerate() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setLeverageSuggestion("moderate_leverage");

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(2);
        EvidenceItemVO lev = result.get(1);
        assertThat(lev.getEvidenceType()).isEqualTo("杠杆");
        assertThat(lev.getDirection()).isEqualTo("NEUTRAL");
        assertThat(lev.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(lev.getDescription()).isEqualTo("适中杠杆建议；口径：Binance 24h 启发式。");
    }

    @Test
    void buildEvidence_returnsNoEvidenceWhenMarketEnvironmentNull() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), null);

        assertThat(result).isEmpty();
    }

    @Test
    void buildEvidence_noLeverageEvidence_whenLeverageSuggestionBlank() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setLeverageSuggestion("   ");

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(1);
    }

    @Test
    void buildEvidence_noLeverageEvidence_whenLeverageSuggestionUnknownToken() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setLeverageSuggestion("high_leverage");

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(1);
    }

    @Test
    void buildEvidence_volatilityAndFundingUnchanged_whenLeveragePresent() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper);
        MarketEnvironmentVO env = marketEnvironment();
        env.setRangePct24h(4.25);
        env.setVolatilityRegime("中等波动");
        env.setLeverageSuggestion("low_leverage");
        env.setPerpFundingApplied(true);
        env.setLastFundingRate(new BigDecimal("0.0001"));

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), env);

        assertThat(result).hasSize(5);
        assertThat(result.get(1).getEvidenceType()).isEqualTo("宏观");
        assertThat(result.get(2).getEvidenceType()).isEqualTo("风险");
        assertThat(result.get(3).getEvidenceType()).isEqualTo("杠杆");
        assertThat(result.get(4).getEvidenceType()).isEqualTo("资金");
    }

    @Test
    void buildEvidence_appendsEventEvidence_whenHotResetEventExistsForAnalysisId() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper, hotResetEventMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId("ana-hot-1");
        HotResetEventDO event = new HotResetEventDO();
        event.setTriggerType("HOT_RESET");
        event.setTriggerReasonCode("CONFUSED_HIGH_MTF_MISALIGNED");
        event.setEventVersion(2);
        event.setEventId("hot-reset-1");
        event.setTraceId("trace-hot-1");
        event.setEventTime(LocalDateTime.of(2026, 8, 12, 1, 0));
        event.setSeverityScore(95);
        when(hotResetEventMapper.selectLatestByAnalysisId("ana-hot-1")).thenReturn(event);
        when(hotResetEventMapper.countByAnalysisId("ana-hot-1")).thenReturn(1);

        List<EvidenceItemVO> result = service.buildEvidence(analysis, marketEnvironment());

        assertThat(result).hasSize(2);
        EvidenceItemVO ev = result.get(1);
        assertThat(ev.getEvidenceType()).isEqualTo("事件");
        assertThat(ev.getDirection()).isEqualTo("NEUTRAL");
        assertThat(ev.getSource()).isEqualTo("SYSTEM_GENERATED");
        assertThat(ev.getDescription())
                .isEqualTo("检测到 Hot Reset 事件：triggerType=HOT_RESET，reasonCode=CONFUSED_HIGH_MTF_MISALIGNED。");
        assertThat(analysis.getEventImpactInput()).isNotNull();
        assertThat(analysis.getEventImpactInput().getEventFactHit()).isTrue();
        assertThat(analysis.getEventImpactInput().getEventFactCount()).isEqualTo(1);
        assertThat(analysis.getEventImpactInput().getEventReasonCode()).isEqualTo("CONFUSED_HIGH_MTF_MISALIGNED");
        assertThat(analysis.getEventImpactInput().getEventTriggerType()).isEqualTo("HOT_RESET");
        assertThat(analysis.getEventImpactInput().getEventVersion()).isEqualTo(2);
        assertThat(analysis.getEventImpactInput().getEventTraceId()).isEqualTo("trace-hot-1");
    }

    @Test
    void buildEvidence_noEventEvidence_whenNoHotResetEventForAnalysisId() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper, hotResetEventMapper);
        AssetAnalysisVO analysis = new AssetAnalysisVO();
        analysis.setAnalysisId("ana-hot-none");
        when(hotResetEventMapper.selectLatestByAnalysisId("ana-hot-none")).thenReturn(null);
        when(hotResetEventMapper.countByAnalysisId("ana-hot-none")).thenReturn(0);

        List<EvidenceItemVO> result = service.buildEvidence(analysis, marketEnvironment());

        assertThat(result).hasSize(1);
        assertThat(result).extracting(EvidenceItemVO::getEvidenceType).doesNotContain("事件");
        assertThat(analysis.getEventImpactInput()).isNotNull();
        assertThat(analysis.getEventImpactInput().getEventFactHit()).isFalse();
        assertThat(analysis.getEventImpactInput().getEventFactCount()).isEqualTo(0);
    }

    @Test
    void buildEvidence_appendsMacroEvidence_whenMarketEnvHasVolatilityAndRange() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper, hotResetEventMapper);
        MarketEnvironmentVO marketEnv = marketEnvironment();
        marketEnv.setVolatilityRegime("中等波动");
        marketEnv.setRangePct24h(3.25);

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), marketEnv);

        assertThat(result).hasSize(3);
        EvidenceItemVO macro = result.get(1);
        assertThat(macro.getEvidenceType()).isEqualTo("宏观");
        assertThat(macro.getDirection()).isEqualTo("NEUTRAL");
        assertThat(macro.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(macro.getDescription()).isEqualTo("当前环境呈现中等波动，24h振幅约3.25%。");
    }

    @Test
    void buildEvidence_appendsMacroEvidenceWithCrowding_whenMarketEnvHasCrowdingState() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper, hotResetEventMapper);
        MarketEnvironmentVO marketEnv = marketEnvironment();
        marketEnv.setVolatilityRegime("高波动");
        marketEnv.setRangePct24h(7.5);
        marketEnv.setDerivativesCrowdingState("CROWDED_LONG");

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), marketEnv);

        assertThat(result).hasSize(3);
        EvidenceItemVO macro = result.get(1);
        assertThat(macro.getEvidenceType()).isEqualTo("宏观");
        assertThat(macro.getDirection()).isEqualTo("NEUTRAL");
        assertThat(macro.getSource()).isEqualTo("MARKET_HEURISTIC");
        assertThat(macro.getDescription()).isEqualTo("当前环境呈现高波动，24h振幅约7.50%；衍生品拥挤状态：CROWDED_LONG。");
    }

    @Test
    void buildEvidence_noMacroEvidence_whenMarketEnvMissing() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper, hotResetEventMapper);
        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), null);

        assertThat(result).isEmpty();
        assertThat(result).extracting(EvidenceItemVO::getEvidenceType).doesNotContain("宏观");
    }

    @Test
    void buildEvidence_noMacroEvidence_whenMarketEnvMissingRequiredWhitelistFields() {
        EvidenceServiceImpl service = new EvidenceServiceImpl(evidenceItemMapper, hotResetEventMapper);
        MarketEnvironmentVO marketEnv = marketEnvironment();
        marketEnv.setVolatilityRegime(" ");
        marketEnv.setRangePct24h(null);

        List<EvidenceItemVO> result = service.buildEvidence(new AssetAnalysisVO(), marketEnv);

        assertThat(result).hasSize(1);
        assertThat(result).extracting(EvidenceItemVO::getEvidenceType).doesNotContain("宏观");
    }

    private static MarketEnvironmentVO marketEnvironment() {
        MarketEnvironmentVO environment = new MarketEnvironmentVO();
        environment.setPriceChangePercent24h(new BigDecimal("1.0"));
        environment.setSourceProvider("TEST_MARKET_PROVIDER");
        environment.setSourceReference("test://market/BTCUSDT/24h");
        environment.setSourceTraceId("trace-market-1");
        environment.setObservedAt(LocalDateTime.of(2026, 8, 12, 0, 0));
        environment.setFreshness("FRESH");
        environment.setDerivativesSourceProvider("TEST_DERIVATIVES_PROVIDER");
        environment.setDerivativesSourceReference("test://derivatives/BTCUSDT");
        environment.setDerivativesSourceTraceId("trace-derivatives-1");
        environment.setDerivativesObservedAt(LocalDateTime.of(2026, 8, 12, 0, 0));
        environment.setDerivativesFreshness("FRESH");
        return environment;
    }
}
