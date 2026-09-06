package org.example.trademodel.v41;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class V41StructuralPlanPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"STRONG_BULLISH", "BULLISH", "WEAK_BULLISH", "WEAK_BEARISH", "BEARISH", "STRONG_BEARISH"})
    void allSixDirectionalStatesProduceACompleteConditionalRulePlan(String direction) {
        var result = V41StructuralPlanPolicy.build(new V41StructuralPlanPolicy.Input(
                direction, new BigDecimal("100"), new BigDecimal("4"),
                direction.contains("BULLISH") ? new BigDecimal("94") : new BigDecimal("106"),
                "analysis-1", "decision-1", 42L, Instant.parse("2026-09-06T10:00:00Z"),
                "BINANCE", false, null));

        assertThat(result.version()).isEqualTo("V41-STRUCTURAL-PLAN-2");
        assertThat(result.entryZone()).isNotBlank();
        assertThat(result.stopLoss()).isPositive();
        assertThat(result.takeProfit1()).isPositive();
        assertThat(result.takeProfit2()).isPositive();
        assertThat(result.riskRewardRatio()).isGreaterThanOrEqualTo(new BigDecimal("1.5"));
        assertThat(result.analysisId()).isEqualTo("analysis-1");
        assertThat(result.decisionId()).isEqualTo("decision-1");
    }
}
