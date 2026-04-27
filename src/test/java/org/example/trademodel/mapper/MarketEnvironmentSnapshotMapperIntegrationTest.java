package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.MarketEnvironmentSnapshotDO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class MarketEnvironmentSnapshotMapperIntegrationTest {

    @Autowired
    private MarketEnvironmentSnapshotMapper marketEnvironmentSnapshotMapper;

    @Test
    void insert_roundTrips_secondDimensionStructuredColumns_fromVoSemantics() {
        MarketEnvironmentSnapshotDO row = baselineRow("ana-mev-second-dim");
        row.setRangePct24h(4.0);
        row.setVolatilityRegime("中等波动");
        row.setLastFundingRate(new BigDecimal("0.000125"));
        row.setPerpFundingApplied(Boolean.TRUE);
        row.setLastOpenInterest(new BigDecimal("75797.837"));
        row.setOpenInterestDelta(new BigDecimal("12.5"));
        row.setOiApplied(Boolean.TRUE);
        row.setDerivativesCrowdingState("CROWDED_LONG");

        marketEnvironmentSnapshotMapper.insert(row);

        MarketEnvironmentSnapshotDO loaded = marketEnvironmentSnapshotMapper.selectByAnalysisId("ana-mev-second-dim");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getRangePct24h()).isEqualTo(4.0);
        assertThat(loaded.getVolatilityRegime()).isEqualTo("中等波动");
        assertThat(loaded.getLastFundingRate()).isEqualByComparingTo("0.0001250000");
        assertThat(loaded.getPerpFundingApplied()).isTrue();
        assertThat(loaded.getLastOpenInterest()).isEqualByComparingTo("75797.83700000");
        assertThat(loaded.getOpenInterestDelta()).isEqualByComparingTo("12.50000000");
        assertThat(loaded.getOiApplied()).isTrue();
        assertThat(loaded.getDerivativesCrowdingState()).isEqualTo("CROWDED_LONG");
        assertThat(loaded.getSummary()).isEqualTo("snap summary");
    }

    @Test
    void insert_keeps_secondDimension_null_whenUnset_likeVoWithoutHighLow() {
        MarketEnvironmentSnapshotDO row = baselineRow("ana-mev-second-dim-null");
        row.setRangePct24h(null);
        row.setVolatilityRegime(null);
        row.setLastFundingRate(null);
        row.setPerpFundingApplied(null);
        row.setLastOpenInterest(null);
        row.setOpenInterestDelta(null);
        row.setOiApplied(null);
        row.setDerivativesCrowdingState(null);

        marketEnvironmentSnapshotMapper.insert(row);

        MarketEnvironmentSnapshotDO loaded = marketEnvironmentSnapshotMapper.selectByAnalysisId("ana-mev-second-dim-null");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getRangePct24h()).isNull();
        assertThat(loaded.getVolatilityRegime()).isNull();
        assertThat(loaded.getLastFundingRate()).isNull();
        assertThat(loaded.getPerpFundingApplied()).isNull();
        assertThat(loaded.getLastOpenInterest()).isNull();
        assertThat(loaded.getOpenInterestDelta()).isNull();
        assertThat(loaded.getOiApplied()).isNull();
        assertThat(loaded.getDerivativesCrowdingState()).isNull();
    }

    @Test
    void selectLatestBySymbolAndTimeframe_returnsLatestSnapshot() {
        MarketEnvironmentSnapshotDO early = baselineRow("ana-mev-prev-early");
        early.setCreateTime(LocalDateTime.of(2026, 4, 21, 12, 0, 0));
        early.setLastOpenInterest(new BigDecimal("100"));
        marketEnvironmentSnapshotMapper.insert(early);

        MarketEnvironmentSnapshotDO latest = baselineRow("ana-mev-prev-latest");
        latest.setCreateTime(LocalDateTime.of(2026, 4, 21, 12, 5, 0));
        latest.setLastOpenInterest(new BigDecimal("135"));
        marketEnvironmentSnapshotMapper.insert(latest);

        MarketEnvironmentSnapshotDO loaded = marketEnvironmentSnapshotMapper
                .selectLatestBySymbolAndTimeframe("BTCUSDT", "1h");
        assertThat(loaded).isNotNull();
        assertThat(loaded.getAnalysisId()).isEqualTo("ana-mev-prev-latest");
        assertThat(loaded.getLastOpenInterest()).isEqualByComparingTo("135.00000000");
    }

    private static MarketEnvironmentSnapshotDO baselineRow(String analysisId) {
        MarketEnvironmentSnapshotDO row = new MarketEnvironmentSnapshotDO();
        row.setAnalysisId(analysisId);
        row.setSymbol("BTCUSDT");
        row.setTimeframe("1h");
        row.setEnvironmentType("range_market");
        row.setRiskMode("normal");
        row.setTrendFriendliness(50);
        row.setLeverageSuggestion("moderate_leverage");
        row.setSummary("snap summary");
        row.setSourceType("BINANCE_24H_HEURISTIC");
        row.setCreateTime(LocalDateTime.of(2026, 4, 21, 12, 0, 0));
        return row;
    }
}
