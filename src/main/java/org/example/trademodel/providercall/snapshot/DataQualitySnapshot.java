package org.example.trademodel.providercall.snapshot;

import java.math.BigDecimal;
import java.util.List;

public record DataQualitySnapshot(BigDecimal score, String status, List<String> reasonCodes) {
    public DataQualitySnapshot {
        reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    }
}
