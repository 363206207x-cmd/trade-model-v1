package org.example.trademodel.market;

import org.example.trademodel.market.client.MarketQuoteClient;
import org.example.trademodel.market.dto.MarketQuoteSnapshot;
import org.example.trademodel.vo.MarketEnvironmentVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import java.util.Optional;

import org.example.trademodel.market.client.OpenInterestClient;
import org.example.trademodel.market.client.PerpFundingRateClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.snapshotService;
import static org.example.trademodel.testsupport.MarketPriceSnapshotTestSupport.derivativesService;

class RealMarketEnvironmentServiceTest {

    private static final OpenInterestClient NO_OI = __ -> Optional.empty();

    private static MarketQuoteSnapshot baseSnap() {
        MarketQuoteSnapshot q = new MarketQuoteSnapshot();
        q.setProvider("binance");
        q.setSymbolNormalized("BTCUSDT");
        q.setPriceChangePercent24h(BigDecimal.ONE);
        return q;
    }

    @Test
    void computeRangePercent24h_matchesFormula() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        q.setHighPrice(new BigDecimal("103"));
        q.setLowPrice(new BigDecimal("100"));
        assertEquals(3.0, RealMarketEnvironmentService.computeRangePercent24h(q), 1e-9);
    }

    @Test
    void computeRangePercent24h_returnsNull_whenHighLowMissing() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        assertNull(RealMarketEnvironmentService.computeRangePercent24h(q));
    }

    @Test
    void priceChangePercent24h_mappedFromTicker_sameAsSnapshot() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        q.setHighPrice(new BigDecimal("100.5"));
        q.setLowPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient noFunding = __ -> Optional.empty();
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(noFunding, NO_OI));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        assertEquals(BigDecimal.ONE, env.get().getPriceChangePercent24h());
        assertEquals("binance", env.get().getSourceProvider());
        assertEquals("test-key", env.get().getSourceReference());
        assertEquals("test-trace", env.get().getSourceTraceId());
        assertNotNull(env.get().getObservedAt());
        assertEquals("FRESH", env.get().getFreshness());
    }

    @Test
    void summary_containsNarrowBand_whenRangeLow() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        q.setHighPrice(new BigDecimal("100.5"));
        q.setLowPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient noFunding = __ -> Optional.empty();
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(noFunding, NO_OI));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        String summary = env.get().getSummary();
        assertTrue(summary.contains("窄幅"), summary);
        assertTrue(summary.contains("24h 价格振幅约"), summary);
    }

    @Test
    void summary_containsHighVolatility_whenRangeHigh() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        q.setHighPrice(new BigDecimal("110"));
        q.setLowPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient noFunding = __ -> Optional.empty();
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(noFunding, NO_OI));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        String summary = env.get().getSummary();
        assertTrue(summary.contains("高波动"), summary);
    }

    @Test
    void summary_omitsAmplitudeLine_whenHighLowMissing_doesNotBreak() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient noFunding = __ -> Optional.empty();
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(noFunding, NO_OI));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        String summary = env.get().getSummary();
        assertTrue(summary.contains("Real feed (Binance 24h)"), summary);
        assertTrue(!summary.contains("24h 价格振幅约"));
        assertNotNull(env.get().getEnvironmentType());
        assertNull(env.get().getRangePct24h());
        assertNull(env.get().getVolatilityRegime());
    }

    @Test
    void secondDimensionFields_setWhenRangeComputable_matchDescribeRegime() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        q.setHighPrice(new BigDecimal("104"));
        q.setLowPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient noFunding = __ -> Optional.empty();
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(noFunding, NO_OI));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        assertEquals(4.0, env.get().getRangePct24h(), 1e-9);
        assertEquals("中等波动", env.get().getVolatilityRegime());
        assertEquals(RealMarketEnvironmentService.describeVolatilityRegime(4.0), env.get().getVolatilityRegime());
    }

    @Test
    void secondDimensionFields_narrowBand_whenRangeBelowTwoPercent() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        q.setHighPrice(new BigDecimal("100.5"));
        q.setLowPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient noFunding = __ -> Optional.empty();
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(noFunding, NO_OI));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("ETH", "15m");
        assertTrue(env.isPresent());
        assertEquals(0.5, env.get().getRangePct24h(), 1e-9);
        assertEquals("窄幅", env.get().getVolatilityRegime());
    }

    @Test
    void describeVolatilityRegime_threeBands() {
        assertEquals("窄幅", RealMarketEnvironmentService.describeVolatilityRegime(1.0));
        assertEquals("中等波动", RealMarketEnvironmentService.describeVolatilityRegime(4.0));
        assertEquals("高波动", RealMarketEnvironmentService.describeVolatilityRegime(7.0));
    }

    @Test
    void summary_appendsFundingClause_whenPerpFundingAvailable() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient funding = __ -> Optional.of(new BigDecimal("0.0001"));
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(funding, NO_OI));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        assertTrue(Boolean.TRUE.equals(env.get().getPerpFundingApplied()));
        String summary = env.get().getSummary();
        assertTrue(summary.contains("Perp（USDⓈ-M）"), summary);
        assertTrue(summary.contains("Binance USDT-M 启发式"), summary);
    }

    @Test
    void summary_unchangedFundingLine_whenPerpFundingFails() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient funding = __ -> Optional.empty();
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(funding, NO_OI));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        assertFalse(Boolean.TRUE.equals(env.get().getPerpFundingApplied()));
        assertTrue(!env.get().getSummary().contains("Perp（USDⓈ-M）"));
    }

    @Test
    void buildFundingAppendix_formatsRateAndDirection() {
        String line = RealMarketEnvironmentService.buildFundingAppendix(new BigDecimal("0.0001"));
        assertTrue(line.contains("+0.010000%/8h"));
        assertTrue(line.contains("多头付费"));
    }

    @Test
    void summary_appendsOpenInterestClause_whenOiAvailable() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient noFunding = __ -> Optional.empty();
        OpenInterestClient oi = __ -> Optional.of(new BigDecimal("75797.837"));
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(noFunding, oi));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        assertTrue(Boolean.TRUE.equals(env.get().getOiApplied()));
        String summary = env.get().getSummary();
        assertTrue(summary.contains("USDⓈ-M 未平仓量约"), summary);
        assertTrue(summary.contains("API 字段 openInterest"), summary);
        assertTrue(summary.contains("75797.837"), summary);
    }

    @Test
    void summary_unchangedOiLine_whenOpenInterestFails() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient noFunding = __ -> Optional.empty();
        OpenInterestClient oi = __ -> Optional.empty();
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(noFunding, oi));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        assertFalse(Boolean.TRUE.equals(env.get().getOiApplied()));
        assertTrue(!env.get().getSummary().contains("未平仓量"));
    }

    @Test
    void summary_appendsFundingThenOi_whenBothAvailable() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient funding = __ -> Optional.of(new BigDecimal("0.0001"));
        OpenInterestClient oi = __ -> Optional.of(new BigDecimal("1000"));
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(funding, oi));
        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");
        assertTrue(env.isPresent());
        assertTrue(Boolean.TRUE.equals(env.get().getPerpFundingApplied()));
        assertTrue(Boolean.TRUE.equals(env.get().getOiApplied()));
        String summary = env.get().getSummary();
        int idxFund = summary.indexOf("Perp（USDⓈ-M）");
        int idxOi = summary.indexOf("USDⓈ-M 未平仓量");
        assertTrue(idxFund >= 0 && idxOi > idxFund, summary);
        assertNull(env.get().getDerivativesCrowdingState());
        assertEquals("CROWDED_LONG",
                RealMarketEnvironmentService.computeDerivativesCrowdingState(env.get()));
        assertEquals("TEST", env.get().getDerivativesSourceProvider());
        assertEquals("test-derivatives", env.get().getDerivativesSourceReference());
        assertEquals("test-trace", env.get().getDerivativesSourceTraceId());
        assertNotNull(env.get().getDerivativesObservedAt());
        assertEquals("FRESH", env.get().getDerivativesFreshness());
    }

    @Test
    void derivativesCrowdingState_isCrowdedShort_whenFundingNegative_andBothApplied() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient funding = __ -> Optional.of(new BigDecimal("-0.0002"));
        OpenInterestClient oi = __ -> Optional.of(new BigDecimal("800"));
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(funding, oi));

        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");

        assertTrue(env.isPresent());
        assertNull(env.get().getDerivativesCrowdingState());
        assertEquals("CROWDED_SHORT",
                RealMarketEnvironmentService.computeDerivativesCrowdingState(env.get()));
    }

    @Test
    void derivativesCrowdingState_fallbackNeutral_whenAnyAppliedFlagOrValueMissing() {
        MarketQuoteSnapshot q = baseSnap();
        q.setLastPrice(new BigDecimal("100"));
        MarketQuoteClient client = s -> Optional.of(q);
        PerpFundingRateClient funding = __ -> Optional.of(new BigDecimal("0.0002"));
        RealMarketEnvironmentService svc = new RealMarketEnvironmentService(snapshotService(client), derivativesService(funding, __ -> Optional.empty()));

        Optional<MarketEnvironmentVO> env = svc.tryBuildFromRealQuote("BTC", "1h");

        assertTrue(env.isPresent());
        assertFalse(Boolean.TRUE.equals(env.get().getOiApplied()));
        assertNull(env.get().getDerivativesCrowdingState());
        assertEquals("NEUTRAL",
                RealMarketEnvironmentService.computeDerivativesCrowdingState(env.get()));
    }

    @Test
    void buildOpenInterestAppendix_containsContractWording() {
        String line = RealMarketEnvironmentService.buildOpenInterestAppendix(new BigDecimal("1234.5"));
        assertTrue(line.contains("1234.5"));
        assertTrue(line.contains("openInterest"));
    }

    @Test
    void computeOpenInterestDelta_returnsCurrentMinusPrevious_whenBothAvailableAndOiApplied() {
        BigDecimal delta = RealMarketEnvironmentService.computeOpenInterestDelta(
                true,
                new BigDecimal("210"),
                new BigDecimal("200"));
        assertEquals(new BigDecimal("10"), delta);
    }

    @Test
    void computeOpenInterestDelta_returnsNull_whenPreviousMissing() {
        BigDecimal delta = RealMarketEnvironmentService.computeOpenInterestDelta(
                true,
                new BigDecimal("210"),
                null);
        assertNull(delta);
    }

    @Test
    void computeOpenInterestDelta_returnsNull_whenOiNotAppliedOrCurrentMissing() {
        BigDecimal byFlag = RealMarketEnvironmentService.computeOpenInterestDelta(
                false,
                new BigDecimal("210"),
                new BigDecimal("200"));
        BigDecimal byCurrent = RealMarketEnvironmentService.computeOpenInterestDelta(
                true,
                null,
                new BigDecimal("200"));
        assertNull(byFlag);
        assertNull(byCurrent);
    }

    @Test
    void computeDerivativesCrowdingState_strategyB_deltaNull_preservesFundingOnlyLong() {
        MarketEnvironmentVO e = new MarketEnvironmentVO();
        e.setOiApplied(true);
        e.setPerpFundingApplied(true);
        e.setLastOpenInterest(BigDecimal.ONE);
        e.setLastFundingRate(new BigDecimal("0.0001"));
        e.setOpenInterestDelta(null);
        assertEquals("CROWDED_LONG", RealMarketEnvironmentService.computeDerivativesCrowdingState(e));
    }

    @Test
    void computeDerivativesCrowdingState_strategyB_deltaNull_preservesFundingOnlyShort() {
        MarketEnvironmentVO e = new MarketEnvironmentVO();
        e.setOiApplied(true);
        e.setPerpFundingApplied(true);
        e.setLastOpenInterest(BigDecimal.ONE);
        e.setLastFundingRate(new BigDecimal("-0.0001"));
        e.setOpenInterestDelta(null);
        assertEquals("CROWDED_SHORT", RealMarketEnvironmentService.computeDerivativesCrowdingState(e));
    }

    @Test
    void computeDerivativesCrowdingState_strategyB_sameSignKeepsCrowdedLong() {
        MarketEnvironmentVO e = new MarketEnvironmentVO();
        e.setOiApplied(true);
        e.setPerpFundingApplied(true);
        e.setLastOpenInterest(BigDecimal.ONE);
        e.setLastFundingRate(new BigDecimal("0.0001"));
        e.setOpenInterestDelta(new BigDecimal("50"));
        assertEquals("CROWDED_LONG", RealMarketEnvironmentService.computeDerivativesCrowdingState(e));
    }

    @Test
    void computeDerivativesCrowdingState_strategyB_oppositeSignFundingLongDeltaNegativeNeutral() {
        MarketEnvironmentVO e = new MarketEnvironmentVO();
        e.setOiApplied(true);
        e.setPerpFundingApplied(true);
        e.setLastOpenInterest(BigDecimal.ONE);
        e.setLastFundingRate(new BigDecimal("0.0001"));
        e.setOpenInterestDelta(new BigDecimal("-10"));
        assertEquals("NEUTRAL", RealMarketEnvironmentService.computeDerivativesCrowdingState(e));
    }

    @Test
    void computeDerivativesCrowdingState_strategyB_zeroDeltaNeutral() {
        MarketEnvironmentVO e = new MarketEnvironmentVO();
        e.setOiApplied(true);
        e.setPerpFundingApplied(true);
        e.setLastOpenInterest(BigDecimal.ONE);
        e.setLastFundingRate(new BigDecimal("0.0001"));
        e.setOpenInterestDelta(BigDecimal.ZERO);
        assertEquals("NEUTRAL", RealMarketEnvironmentService.computeDerivativesCrowdingState(e));
    }

    @Test
    void computeDerivativesCrowdingState_strategyB_sameSignKeepsCrowdedShort() {
        MarketEnvironmentVO e = new MarketEnvironmentVO();
        e.setOiApplied(true);
        e.setPerpFundingApplied(true);
        e.setLastOpenInterest(BigDecimal.ONE);
        e.setLastFundingRate(new BigDecimal("-0.0001"));
        e.setOpenInterestDelta(new BigDecimal("-5"));
        assertEquals("CROWDED_SHORT", RealMarketEnvironmentService.computeDerivativesCrowdingState(e));
    }

    @Test
    void computeDerivativesCrowdingState_strategyB_fundingShortDeltaPositiveNeutral() {
        MarketEnvironmentVO e = new MarketEnvironmentVO();
        e.setOiApplied(true);
        e.setPerpFundingApplied(true);
        e.setLastOpenInterest(BigDecimal.ONE);
        e.setLastFundingRate(new BigDecimal("-0.0001"));
        e.setOpenInterestDelta(new BigDecimal("3"));
        assertEquals("NEUTRAL", RealMarketEnvironmentService.computeDerivativesCrowdingState(e));
    }
}
