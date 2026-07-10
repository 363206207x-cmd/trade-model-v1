package org.example.trademodel.providercall.snapshot;

import java.math.BigDecimal;
import java.util.List;

public record AccountRiskSnapshot(
        Boolean riskAllowed,
        String riskLevel,
        BigDecimal accountImpactPct,
        List<String> reasonCodes
) {
    public AccountRiskSnapshot {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
