package org.example.trademodel.enums;

public enum MarketBiasEnum {
    STRONG_BULLISH,
    BULLISH,
    WEAK_BULLISH,
    RANGE,
    WEAK_BEARISH,
    BEARISH,
    STRONG_BEARISH,
    WAIT;

    public boolean bullishFamily() {
        return this == STRONG_BULLISH || this == BULLISH || this == WEAK_BULLISH;
    }

    public boolean bearishFamily() {
        return this == STRONG_BEARISH || this == BEARISH || this == WEAK_BEARISH;
    }

    public boolean sameDirectionalFamily(MarketBiasEnum other) {
        return other != null && ((bullishFamily() && other.bullishFamily())
                || (bearishFamily() && other.bearishFamily()) || this == other);
    }

    public boolean isSameFamilyDowngradeFrom(MarketBiasEnum before) {
        if (before == null || !before.sameDirectionalFamily(this)) return false;
        if (before.bullishFamily()) return bullishStrength(this) <= bullishStrength(before);
        if (before.bearishFamily()) return bearishStrength(this) <= bearishStrength(before);
        return this == before;
    }

    private static int bullishStrength(MarketBiasEnum value) {
        return switch (value) {
            case STRONG_BULLISH -> 3;
            case BULLISH -> 2;
            case WEAK_BULLISH -> 1;
            default -> 0;
        };
    }

    private static int bearishStrength(MarketBiasEnum value) {
        return switch (value) {
            case STRONG_BEARISH -> 3;
            case BEARISH -> 2;
            case WEAK_BEARISH -> 1;
            default -> 0;
        };
    }
}
