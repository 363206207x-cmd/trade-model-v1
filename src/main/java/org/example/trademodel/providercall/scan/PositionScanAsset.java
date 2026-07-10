package org.example.trademodel.providercall.scan;

public record PositionScanAsset(String symbol, String status) {
    public boolean active() {
        return "OPEN".equalsIgnoreCase(status) || "PARTIALLY_CLOSED".equalsIgnoreCase(status);
    }
}
