package org.example.trademodel.v41;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class V41RiskVectorPolicyTest {

    @Test
    void returnsConcreteIndependentRiskTypesAndKeepsHighestTwo() {
        var vector = V41RiskVectorPolicy.assess(new V41RiskVectorPolicy.Input(
                82, 76, 35, 71, 66, 22, 10, 18, "BINANCE+COINGLASS", Instant.parse("2026-09-06T10:00:00Z")));

        assertThat(vector.version()).isEqualTo("V41-RISK-VECTOR-1");
        assertThat(vector.items()).extracting(V41RiskVectorPolicy.RiskItem::riskType)
                .contains("CHASE_RISK", "RAPID_MOVE_RISK", "CROWDING_RISK", "LIQUIDITY_RISK");
        assertThat(vector.primaryItems()).hasSize(2);
        assertThat(vector.primaryItems().get(0).score()).isGreaterThanOrEqualTo(vector.primaryItems().get(1).score());
        assertThat(vector.displayLabel()).contains("（高）");
    }

    @Test
    void overallRiskDoesNotMutateDirectionConfidence() {
        var vector = V41RiskVectorPolicy.assess(new V41RiskVectorPolicy.Input(
                95, 20, 10, 10, 10, 10, 10, 10, "BINANCE", Instant.now()));
        assertThat(vector.overallRisk()).isEqualTo(95);
        assertThat(vector.primaryItems().get(0).riskType()).isEqualTo("CHASE_RISK");
    }
}
