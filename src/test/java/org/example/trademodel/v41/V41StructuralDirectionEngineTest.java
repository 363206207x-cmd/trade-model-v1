package org.example.trademodel.v41;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class V41StructuralDirectionEngineTest {

    @Test
    void usesFrozenVersionWeightsAndRobustFeatures() {
        var result = V41StructuralDirectionEngine.assess(
                trend(96, new BigDecimal("100"), new BigDecimal("0.45")),
                trend(48, new BigDecimal("100"), new BigDecimal("1.10")));

        assertThat(result.version()).isEqualTo("V41-DIRECTION-4H1H-2");
        assertThat(result.directionScore()).isEqualByComparingTo(
                result.trend4h().multiply(new BigDecimal("0.55"))
                        .add(result.state1h().multiply(new BigDecimal("0.45"))));
        assertThat(result.direction()).isIn("BULLISH", "STRONG_BULLISH");
        assertThat(result.invalidationLevel()).isPositive();
    }

    @Test
    void oneHourReversalAgainstFourHourTrendIsConflictNotAveragedDirection() {
        var result = V41StructuralDirectionEngine.assess(
                trend(96, new BigDecimal("150"), new BigDecimal("-0.85")),
                trend(48, new BigDecimal("100"), new BigDecimal("1.20")));

        assertThat(result.direction()).isEqualTo("WAIT");
        assertThat(result.state()).isEqualTo("MULTI_TIMEFRAME_CONFLICT");
        assertThat(result.contextLabel()).contains("1小时偏空").contains("4小时趋势偏多");
    }

    @Test
    void bnbShallowPullbackDoesNotBecomeBearish() {
        List<String[]> fourHour = trend(48, new BigDecimal("700"), new BigDecimal("1.70"));
        List<String[]> oneHour = trend(92, new BigDecimal("700"), new BigDecimal("0.88"));
        oneHour.add(bar(92, "780", "782", "758", "762"));

        var result = V41StructuralDirectionEngine.assess(oneHour, fourHour);

        assertThat(result.direction()).isIn("WEAK_BULLISH", "BULLISH", "STRONG_BULLISH", "WAIT");
        assertThat(result.direction()).isNotIn("BEARISH", "STRONG_BEARISH");
    }

    @Test
    void ethDeepReversalExitsOldStrongBearishInsteadOfMixingNewPrice() {
        List<String[]> fourHour = trend(48, new BigDecimal("2850"), new BigDecimal("-9.0"));
        List<String[]> oneHour = trend(84, new BigDecimal("2800"), new BigDecimal("-4.9"));
        for (int i = 0; i < 12; i++) {
            BigDecimal close = new BigDecimal("2391").add(new BigDecimal(i).multiply(new BigDecimal("12.55")));
            oneHour.add(bar(84 + i, close.subtract(BigDecimal.TEN).toPlainString(),
                    close.add(new BigDecimal("8")).toPlainString(),
                    close.subtract(new BigDecimal("14")).toPlainString(), close.toPlainString()));
        }

        var result = V41StructuralDirectionEngine.assess(oneHour, fourHour);

        assertThat(result.direction()).isNotEqualTo("STRONG_BEARISH");
        assertThat(result.snapshotFingerprint()).isNotBlank();
    }

    private static List<String[]> trend(int count, BigDecimal start, BigDecimal step) {
        List<String[]> bars = new ArrayList<>();
        BigDecimal previous = start;
        for (int i = 0; i < count; i++) {
            BigDecimal close = previous.add(step);
            bars.add(bar(i, previous.toPlainString(), close.add(step.abs()).toPlainString(),
                    previous.subtract(step.abs()).toPlainString(), close.toPlainString()));
            previous = close;
        }
        return bars;
    }

    private static String[] bar(int index, String open, String high, String low, String close) {
        return new String[]{String.valueOf(index), open, high, low, close, "0", String.valueOf(index + 1)};
    }
}
