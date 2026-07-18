package org.example.trademodel.providercall.instrument;

import java.util.Locale;
import java.util.Objects;

public record CanonicalInstrumentId(
        String baseAsset,
        String quoteAsset,
        MarketType marketType,
        String venue,
        ContractType contractType
) {
    public CanonicalInstrumentId {
        baseAsset = normalizeAsset(baseAsset, "baseAsset");
        quoteAsset = normalizeAsset(quoteAsset, "quoteAsset");
        marketType = Objects.requireNonNull(marketType, "marketType");
        venue = normalizeToken(venue, "venue");
        contractType = Objects.requireNonNull(contractType, "contractType");
        if (marketType == MarketType.SPOT && contractType != ContractType.NONE) {
            throw new IllegalArgumentException("spot instruments must use contractType NONE");
        }
        if (marketType == MarketType.PERPETUAL && contractType != ContractType.LINEAR) {
            throw new IllegalArgumentException("perpetual instruments must use contractType LINEAR");
        }
    }

    public String canonical() {
        return venue + ":" + marketType + ":" + contractType + ":" + displaySymbol();
    }

    public String displaySymbol() {
        return baseAsset + "/" + quoteAsset;
    }

    public CanonicalInstrumentId withVenue(String targetVenue) {
        return new CanonicalInstrumentId(baseAsset, quoteAsset, marketType, targetVenue, contractType);
    }

    private static String normalizeAsset(String value, String field) {
        String normalized = normalizeToken(value, field);
        if (!normalized.matches("[A-Z0-9]{2,15}")) {
            throw new IllegalArgumentException(field + " has unsupported characters");
        }
        return normalized;
    }

    private static String normalizeToken(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
