package org.example.trademodel.service;

import org.example.trademodel.config.FundamentalAiV41Properties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketBiasPolicyTest {

    @Test
    void coversAllEightFormalMarketBiasLevels() {
        assertThat(MarketBiasPolicy.classifyStructuralBias(new BigDecimal("70"))).isEqualTo("STRONG_BULLISH");
        assertThat(MarketBiasPolicy.classifyStructuralBias(new BigDecimal("35"))).isEqualTo("BULLISH");
        assertThat(MarketBiasPolicy.classifyStructuralBias(new BigDecimal("15"))).isEqualTo("WEAK_BULLISH");
        assertThat(MarketBiasPolicy.classifyStructuralBias(BigDecimal.ZERO)).isEqualTo("RANGE");
        assertThat(MarketBiasPolicy.classifyStructuralBias(new BigDecimal("-15"))).isEqualTo("WEAK_BEARISH");
        assertThat(MarketBiasPolicy.classifyStructuralBias(new BigDecimal("-35"))).isEqualTo("BEARISH");
        assertThat(MarketBiasPolicy.classifyStructuralBias(new BigDecimal("-70"))).isEqualTo("STRONG_BEARISH");
        assertThat(classify(up(), up(), List.of(), up())).isEqualTo("WAIT");
    }

    @Test
    void usesOnlyFourHourAndOneHourForStructuralDirection() {
        String bullish = classify(down(), down(), up(), up());
        String bearish = classify(up(), up(), down(), down());

        assertThat(bullish).isEqualTo("STRONG_BULLISH");
        assertThat(bearish).isEqualTo("STRONG_BEARISH");
    }

    @Test
    void missingWeightConfigurationFailsClosed() {
        FundamentalAiV41Properties.MultiTimeframe config =
                FundamentalAiV41Properties.contractFixture().getMultiTimeframe();
        config.setFourHourWeight(null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        MarketBiasPolicy.classify(up(), up(), up(), up(), config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configuration is required");
    }

    @Test
    void usesWholeWindowInsteadOfLatestCandleShortcut() {
        List<String[]> window = List.of(
                bar("100", "108"),
                bar("108", "110"),
                bar("115", "112"));

        assertThat(MarketBiasPolicy.direction(window))
                .isEqualTo(MarketBiasPolicy.WindowDirection.BULLISH);
    }

    @Test
    void strongLabelDowngradesWhenCoreScoreDispersionExceedsContract() {
        FundamentalAiV41Properties properties = FundamentalAiV41Properties.contractFixture();
        MarketBiasPolicy.DirectionAssessment assessment = MarketBiasPolicy.assessDirection(
                flat(), flat(), rankedWindow(50), rankedWindow(58),
                properties.getMultiTimeframe(), properties.getNormalization());

        assertThat(assessment.normalized4hDirectionScore()
                .subtract(assessment.normalized1hDirectionScore()).abs())
                .isGreaterThan(properties.getMultiTimeframe().getMaximumTrendScoreDifference());
        assertThat(assessment.ruleMarketBias()).isEqualTo("BULLISH");
    }

    private static String classify(List<String[]> five, List<String[]> fifteen,
                                   List<String[]> oneHour, List<String[]> fourHour) {
        return MarketBiasPolicy.classify(five, fifteen, oneHour, fourHour);
    }

    private static List<String[]> up() {
        return directionalWindow(true);
    }

    private static List<String[]> down() {
        return directionalWindow(false);
    }

    private static List<String[]> flat() {
        List<String[]> bars = new ArrayList<>();
        for (int index = 0; index < 60; index++) bars.add(bar("100", "100"));
        return bars;
    }

    private static List<String[]> directionalWindow(boolean bullish) {
        List<String[]> bars = new ArrayList<>();
        for (int index = 0; index < 60; index++) {
            BigDecimal price = bullish
                    ? BigDecimal.valueOf(100).add(BigDecimal.valueOf(index + 1L, 1))
                    : BigDecimal.valueOf(100).subtract(BigDecimal.valueOf(index + 1L, 1));
            bars.add(bar("100", price.toPlainString()));
        }
        return bars;
    }

    private static List<String[]> rankedWindow(int finalCloseRank) {
        List<String[]> bars = new ArrayList<>();
        for (int index = 0; index < 59; index++) {
            bars.add(bar("100", String.valueOf(101 + index)));
        }
        bars.add(bar("100", String.valueOf(101 + finalCloseRank)));
        return bars;
    }

    private static String[] bar(String open, String close) {
        return new String[]{"0", open, open, close, close};
    }
}
