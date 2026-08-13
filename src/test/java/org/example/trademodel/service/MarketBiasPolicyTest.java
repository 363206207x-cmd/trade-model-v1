package org.example.trademodel.service;

import org.example.trademodel.config.FundamentalAiV41Properties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarketBiasPolicyTest {

    @Test
    void coversAllEightFormalMarketBiasLevels() {
        assertThat(classify(up(), up(), up(), up())).isEqualTo("STRONG_BULLISH");
        assertThat(classify(up(), up(), down(), up())).isEqualTo("BULLISH");
        assertThat(classify(flat(), flat(), flat(), flat())).isEqualTo("RANGE");
        assertThat(classify(up(), flat(), flat(), up())).isEqualTo("WEAK_BULLISH");
        assertThat(classify(down(), flat(), flat(), down())).isEqualTo("WEAK_BEARISH");
        assertThat(classify(down(), down(), up(), down())).isEqualTo("BEARISH");
        assertThat(classify(down(), down(), down(), down())).isEqualTo("STRONG_BEARISH");
        assertThat(classify(List.of(), up(), up(), up())).isEqualTo("WAIT");
    }

    @Test
    void usesFrozenWeightsSoFourHourAndOneHourOutweighLowerTimeframes() {
        assertThat(classify(down(), down(), up(), up())).isEqualTo("WEAK_BULLISH");
        assertThat(classify(up(), up(), down(), down())).isEqualTo("WEAK_BEARISH");
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

    private static String classify(List<String[]> five, List<String[]> fifteen,
                                   List<String[]> oneHour, List<String[]> fourHour) {
        return MarketBiasPolicy.classify(five, fifteen, oneHour, fourHour);
    }

    private static List<String[]> up() {
        return List.of(bar("100", "102"), bar("102", "105"));
    }

    private static List<String[]> down() {
        return List.of(bar("105", "102"), bar("102", "100"));
    }

    private static List<String[]> flat() {
        return List.of(bar("100", "101"), bar("101", "100"));
    }

    private static String[] bar(String open, String close) {
        return new String[]{"0", open, open, close, close};
    }
}
