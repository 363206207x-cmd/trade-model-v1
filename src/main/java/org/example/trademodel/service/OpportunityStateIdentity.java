package org.example.trademodel.service;

import java.util.Locale;

/** Stable owner + asset + timeframe identity for one opportunity state row. */
public record OpportunityStateIdentity(
        String ownerType,
        Long ownerId,
        Long assetId,
        String symbol,
        String timeframe) {

    public OpportunityStateIdentity {
        ownerType = normalizeOwnerType(ownerType);
        ownerId = normalizeOwnerId(ownerType, ownerId);
        symbol = require(symbol, "symbol").toUpperCase(Locale.ROOT);
        timeframe = require(timeframe, "timeframe").toLowerCase(Locale.ROOT);
    }

    public static OpportunityStateIdentity system(String symbol, String timeframe) {
        return new OpportunityStateIdentity("SYSTEM", 0L, null, symbol, timeframe);
    }

    private static String normalizeOwnerType(String raw) {
        String value = require(raw, "ownerType").toUpperCase(Locale.ROOT);
        if (!"SYSTEM".equals(value) && !"USER".equals(value)) {
            throw new IllegalArgumentException("ownerType must be SYSTEM or USER");
        }
        return value;
    }

    private static Long normalizeOwnerId(String ownerType, Long ownerId) {
        if ("SYSTEM".equals(ownerType)) return 0L;
        if (ownerId == null || ownerId <= 0) throw new IllegalArgumentException("user ownerId is required");
        return ownerId;
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value.trim();
    }
}
