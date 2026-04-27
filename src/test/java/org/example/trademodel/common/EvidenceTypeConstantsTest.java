package org.example.trademodel.common;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("smoke")
class EvidenceTypeConstantsTest {

    @Test
    void normalizeEvidenceType_returnsOriginalWhenAllowed() {
        assertThat(EvidenceTypeConstants.normalizeEvidenceType("价格结构")).isEqualTo("价格结构");
        assertThat(EvidenceTypeConstants.normalizeEvidenceType("杠杆")).isEqualTo("杠杆");
        assertThat(EvidenceTypeConstants.normalizeEvidenceType("资金")).isEqualTo("资金");
        assertThat(EvidenceTypeConstants.normalizeEvidenceType("事件")).isEqualTo("事件");
        assertThat(EvidenceTypeConstants.normalizeEvidenceType("风险")).isEqualTo("风险");
    }

    @Test
    void normalizeEvidenceType_fallsBackToRiskWhenInvalidOrBlank() {
        assertThat(EvidenceTypeConstants.normalizeEvidenceType("unknown")).isEqualTo("风险");
        assertThat(EvidenceTypeConstants.normalizeEvidenceType("  ")).isEqualTo("风险");
        assertThat(EvidenceTypeConstants.normalizeEvidenceType(null)).isEqualTo("风险");
    }

    @Test
    void normalizeEvidenceDirection_returnsOriginalWhenAllowed() {
        assertThat(EvidenceTypeConstants.normalizeEvidenceDirection("BULLISH")).isEqualTo("BULLISH");
        assertThat(EvidenceTypeConstants.normalizeEvidenceDirection("BEARISH")).isEqualTo("BEARISH");
        assertThat(EvidenceTypeConstants.normalizeEvidenceDirection("NEUTRAL")).isEqualTo("NEUTRAL");
    }

    @Test
    void normalizeEvidenceDirection_fallsBackToNeutralWhenInvalidOrBlank() {
        assertThat(EvidenceTypeConstants.normalizeEvidenceDirection("UNKNOWN")).isEqualTo("NEUTRAL");
        assertThat(EvidenceTypeConstants.normalizeEvidenceDirection("  ")).isEqualTo("NEUTRAL");
        assertThat(EvidenceTypeConstants.normalizeEvidenceDirection(null)).isEqualTo("NEUTRAL");
    }

    @Test
    void normalizeEvidenceSource_returnsOriginalWhenAllowed() {
        assertThat(EvidenceTypeConstants.normalizeEvidenceSource("SYSTEM_GENERATED")).isEqualTo("SYSTEM_GENERATED");
        assertThat(EvidenceTypeConstants.normalizeEvidenceSource("MARKET_HEURISTIC")).isEqualTo("MARKET_HEURISTIC");
        assertThat(EvidenceTypeConstants.normalizeEvidenceSource("MANUAL_INPUT")).isEqualTo("MANUAL_INPUT");
    }

    @Test
    void normalizeEvidenceSource_fallsBackToSystemGeneratedWhenInvalidOrBlank() {
        assertThat(EvidenceTypeConstants.normalizeEvidenceSource("UNKNOWN_SOURCE")).isEqualTo("SYSTEM_GENERATED");
        assertThat(EvidenceTypeConstants.normalizeEvidenceSource("  ")).isEqualTo("SYSTEM_GENERATED");
        assertThat(EvidenceTypeConstants.normalizeEvidenceSource(null)).isEqualTo("SYSTEM_GENERATED");
    }
}
