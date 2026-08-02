package org.example.trademodel.service.support;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class DataQualityCircuitBreakerPolicyTest {

    @Test
    void integerBoundaryFailsClosedBelowSeventyAndForInvalidValues() {
        assertThat(DataQualityCircuitBreakerPolicy.passes((Integer) null)).isFalse();
        assertThat(DataQualityCircuitBreakerPolicy.passes(-1)).isFalse();
        assertThat(DataQualityCircuitBreakerPolicy.passes(59)).isFalse();
        assertThat(DataQualityCircuitBreakerPolicy.passes(60)).isFalse();
        assertThat(DataQualityCircuitBreakerPolicy.passes(69)).isFalse();
        assertThat(DataQualityCircuitBreakerPolicy.passes(70)).isTrue();
        assertThat(DataQualityCircuitBreakerPolicy.passes(100)).isTrue();
        assertThat(DataQualityCircuitBreakerPolicy.passes(101)).isFalse();
    }

    @Test
    void decimalBoundaryUsesTheSameProductContract() {
        assertThat(DataQualityCircuitBreakerPolicy.passes(new BigDecimal("69.99"))).isFalse();
        assertThat(DataQualityCircuitBreakerPolicy.passes(new BigDecimal("70.00"))).isTrue();
        assertThat(DataQualityCircuitBreakerPolicy.passes(new BigDecimal("100.01"))).isFalse();
    }
}
