package org.example.trademodel.v41;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class V41RealtimeShockPolicyTest {

    @Test
    void invalidationCrossingSuspendsWithoutReversingStructuralDirection() {
        var result = V41RealtimeShockPolicy.assess(new V41RealtimeShockPolicy.Input(
                "BULLISH", new BigDecimal("770"), new BigDecimal("762"), new BigDecimal("12"),
                new BigDecimal("-0.8"), new BigDecimal("-1.6"), new BigDecimal("99.8"),
                new BigDecimal("45"), new BigDecimal("52"), true, true, true, false, 2));

        assertThat(result.version()).isEqualTo("V41-REALTIME-SHOCK-1");
        assertThat(result.structuralDirection()).isEqualTo("BULLISH");
        assertThat(result.realtimeState()).isEqualTo("STRUCTURE_INVALIDATED");
        assertThat(result.effectiveExecutionState()).isEqualTo("INVALIDATED");
        assertThat(result.reasonCodes()).contains("STRUCTURE_INVALIDATION_CROSSED");
        assertThat(result.confidenceCap()).isEqualTo(20);
    }

    @Test
    void derivativesOutageDoesNotHideTrustedPriceShock() {
        var result = V41RealtimeShockPolicy.assess(new V41RealtimeShockPolicy.Input(
                "BEARISH", new BigDecimal("82000"), new BigDecimal("80000"), new BigDecimal("900"),
                new BigDecimal("1.3"), new BigDecimal("1.7"), new BigDecimal("99.9"),
                null, new BigDecimal("25"), false, true, true, false, 0));

        assertThat(result.effectiveExecutionState()).isEqualTo("SUSPENDED");
        assertThat(result.reasonCodes()).contains("DERIVATIVES_DATA_UNAVAILABLE");
    }

    @Test
    void aSingleInvalidationWickWarnsButDoesNotPermanentlyInvalidate() {
        var result = V41RealtimeShockPolicy.assess(new V41RealtimeShockPolicy.Input(
                "BULLISH", new BigDecimal("770"), new BigDecimal("769"), new BigDecimal("12"),
                new BigDecimal("-0.2"), new BigDecimal("-0.3"), new BigDecimal("80"),
                new BigDecimal("20"), new BigDecimal("20"), true, true, true, false, 1));

        assertThat(result.effectiveExecutionState()).isNotEqualTo("INVALIDATED");
        assertThat(result.reasonCodes()).doesNotContain("STRUCTURE_INVALIDATION_CROSSED");
    }

    @Test
    void extremePricePlusLeverageSuspendsBothLongAndShortWithoutDirectionFlip() {
        var longShock = V41RealtimeShockPolicy.assess(new V41RealtimeShockPolicy.Input(
                "STRONG_BULLISH", new BigDecimal("90"), new BigDecimal("100"), new BigDecimal("5"),
                new BigDecimal("-1"), new BigDecimal("-2"), new BigDecimal("99.7"),
                new BigDecimal("96"), new BigDecimal("30"), true, true, true, false, 0));
        var shortShock = V41RealtimeShockPolicy.assess(new V41RealtimeShockPolicy.Input(
                "STRONG_BEARISH", new BigDecimal("110"), new BigDecimal("100"), new BigDecimal("5"),
                new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("99.7"),
                new BigDecimal("30"), new BigDecimal("96"), true, true, true, false, 0));

        assertThat(longShock.realtimeState()).isEqualTo("RAPID_DROP");
        assertThat(longShock.structuralDirection()).isEqualTo("STRONG_BULLISH");
        assertThat(shortShock.realtimeState()).isEqualTo("RAPID_RISE");
        assertThat(shortShock.structuralDirection()).isEqualTo("STRONG_BEARISH");
        assertThat(longShock.effectiveExecutionState()).isEqualTo("SUSPENDED");
        assertThat(shortShock.effectiveExecutionState()).isEqualTo("SUSPENDED");
    }

    @Test
    void staleDirectionCapsConfidenceAndRequiresReanalysisWithoutFlippingDirection() {
        var result = V41RealtimeShockPolicy.assess(new V41RealtimeShockPolicy.Input(
                "STRONG_BEARISH", new BigDecimal("2520"), new BigDecimal("2529"), new BigDecimal("18"),
                new BigDecimal("0.2"), new BigDecimal("0.4"), null,
                null, null, false, true, false, false, 0));

        assertThat(result.structuralDirection()).isEqualTo("STRONG_BEARISH");
        assertThat(result.realtimeState()).isEqualTo("DIRECTION_STALE");
        assertThat(result.reasonCodes()).contains("DIRECTION_REANALYSIS_REQUIRED");
        assertThat(result.confidenceCap()).isEqualTo(45);
    }
}
