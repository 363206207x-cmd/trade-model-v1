package org.example.trademodel.providercall.profile;

import java.math.BigDecimal;

public record ProfileTransitionSignal(
        BigDecimal priceMovement1m,
        BigDecimal atrMultiple5m,
        BigDecimal volumeSpike,
        BigDecimal spreadSpike,
        BigDecimal nearStopDistance,
        BigDecimal nearTargetDistance,
        BigDecimal openInterestChange,
        BigDecimal liquidationSpike,
        BigDecimal fundingExtremity,
        boolean highImpactEvent,
        BigDecimal confusedScore,
        boolean hotReset,
        boolean strongReversal,
        BigDecimal dataQualityScore
) {
    public static ProfileTransitionSignal recovery() {
        return new ProfileTransitionSignal(null, null, null, null, null, null, null, null, null,
                false, null, false, false, null);
    }
}
