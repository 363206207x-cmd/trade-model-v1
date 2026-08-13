package org.example.trademodel.service.watchlistsource;

import java.util.Locale;

/** Owner-scoped internal target for persistent Asset Pool analysis scheduling. */
public record AssetPoolScanTarget(
        String ownerType,
        Long ownerId,
        Long assetId,
        String symbol) {

    public AssetPoolScanTarget {
        ownerType = normalizeOwnerType(ownerType);
        ownerId = "SYSTEM".equals(ownerType) ? 0L : requireUserId(ownerId);
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol is required");
        }
        symbol = symbol.trim().toUpperCase(Locale.ROOT);
    }

    public static AssetPoolScanTarget system(String symbol) {
        return new AssetPoolScanTarget("SYSTEM", 0L, null, symbol);
    }

    private static String normalizeOwnerType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!"SYSTEM".equals(normalized) && !"USER".equals(normalized)) {
            throw new IllegalArgumentException("ownerType must be SYSTEM or USER");
        }
        return normalized;
    }

    private static Long requireUserId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("user ownerId is required");
        }
        return value;
    }
}
