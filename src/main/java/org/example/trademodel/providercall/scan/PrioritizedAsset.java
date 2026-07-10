package org.example.trademodel.providercall.scan;

import org.example.trademodel.providercall.AssetPriority;

public record PrioritizedAsset(String symbol, AssetPriority priority) {
}
