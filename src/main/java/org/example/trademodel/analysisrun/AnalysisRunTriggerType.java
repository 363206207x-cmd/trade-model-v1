package org.example.trademodel.analysisrun;

import java.util.Locale;

public enum AnalysisRunTriggerType {
    SCHEDULED,
    ASSET_POOL_SCAN,
    MANUAL_API,
    HOT_RESET_REBUILD,
    MARKET_DATA_COMPATIBILITY;

    public static AnalysisRunTriggerType normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return MANUAL_API;
        }
        String t = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (t.startsWith("HOT_RESET")) {
            return HOT_RESET_REBUILD;
        }
        if (t.startsWith("SCHEDULE")) {
            return SCHEDULED;
        }
        if (t.startsWith("ASSET_POOL")) {
            return ASSET_POOL_SCAN;
        }
        if (t.startsWith("MARKET_DATA") || t.startsWith("REAL_MARKET")) {
            return MARKET_DATA_COMPATIBILITY;
        }
        if (t.startsWith("MANUAL")) {
            return MANUAL_API;
        }
        return MANUAL_API;
    }
}
