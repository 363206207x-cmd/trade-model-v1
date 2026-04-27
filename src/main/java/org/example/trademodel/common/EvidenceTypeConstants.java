package org.example.trademodel.common;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class EvidenceTypeConstants {
    public static final String PRICE_STRUCTURE = "价格结构";
    public static final String LEVERAGE = "杠杆";
    public static final String FUNDING = "资金";
    public static final String EVENT = "事件";
    public static final String RISK = "风险";
    public static final String MACRO = "宏观";

    public static final Set<String> ALLOWED_EVIDENCE_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Set.of(PRICE_STRUCTURE, LEVERAGE, FUNDING, EVENT, RISK, MACRO))
    );
    public static final String EVIDENCE_DIRECTION_BULLISH = "BULLISH";
    public static final String EVIDENCE_DIRECTION_BEARISH = "BEARISH";
    public static final String EVIDENCE_DIRECTION_NEUTRAL = "NEUTRAL";
    public static final Set<String> ALLOWED_EVIDENCE_DIRECTIONS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Set.of(
                    EVIDENCE_DIRECTION_BULLISH,
                    EVIDENCE_DIRECTION_BEARISH,
                    EVIDENCE_DIRECTION_NEUTRAL
            ))
    );
    public static final String EVIDENCE_SOURCE_SYSTEM_GENERATED = "SYSTEM_GENERATED";
    public static final String EVIDENCE_SOURCE_MARKET_HEURISTIC = "MARKET_HEURISTIC";
    public static final String EVIDENCE_SOURCE_MANUAL_INPUT = "MANUAL_INPUT";
    public static final Set<String> ALLOWED_EVIDENCE_SOURCES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Set.of(
                    EVIDENCE_SOURCE_SYSTEM_GENERATED,
                    EVIDENCE_SOURCE_MARKET_HEURISTIC,
                    EVIDENCE_SOURCE_MANUAL_INPUT
            ))
    );
    public static final String EVENT_TRIGGER_TYPE_CIRCUIT_BREAKER = "CIRCUIT_BREAKER";
    public static final String EVENT_TRIGGER_TYPE_EXCHANGE_OUTAGE = "EXCHANGE_OUTAGE";
    public static final String EVENT_TRIGGER_TYPE_LIQUIDATION_CASCADE = "LIQUIDATION_CASCADE";
    public static final Set<String> EVENT_IMPACT_SEVERE_TRIGGER_TYPES = Collections.unmodifiableSet(
            new LinkedHashSet<>(Set.of(
                    EVENT_TRIGGER_TYPE_CIRCUIT_BREAKER,
                    EVENT_TRIGGER_TYPE_EXCHANGE_OUTAGE,
                    EVENT_TRIGGER_TYPE_LIQUIDATION_CASCADE
            ))
    );
    public static final int EVENT_IMPACT_MULTI_HIT_THRESHOLD = 3;
    public static final double EVENT_IMPACT_MULTI_HIT_EXTRA_PENALTY = 5.0;
    public static final double EVENT_IMPACT_SEVERE_TRIGGER_EXTRA_PENALTY = 5.0;

    private EvidenceTypeConstants() {
    }

    public static String normalizeEvidenceType(String raw) {
        if (raw == null) {
            return RISK;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return RISK;
        }
        return ALLOWED_EVIDENCE_TYPES.contains(trimmed) ? trimmed : RISK;
    }

    public static boolean isAllowed(String raw) {
        if (raw == null) {
            return false;
        }
        return ALLOWED_EVIDENCE_TYPES.contains(raw.trim());
    }

    public static String normalizeEvidenceDirection(String raw) {
        if (raw == null) {
            return EVIDENCE_DIRECTION_NEUTRAL;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return EVIDENCE_DIRECTION_NEUTRAL;
        }
        return ALLOWED_EVIDENCE_DIRECTIONS.contains(trimmed) ? trimmed : EVIDENCE_DIRECTION_NEUTRAL;
    }

    public static boolean isAllowedDirection(String raw) {
        if (raw == null) {
            return false;
        }
        return ALLOWED_EVIDENCE_DIRECTIONS.contains(raw.trim());
    }

    public static String normalizeEvidenceSource(String raw) {
        if (raw == null) {
            return EVIDENCE_SOURCE_SYSTEM_GENERATED;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return EVIDENCE_SOURCE_SYSTEM_GENERATED;
        }
        return ALLOWED_EVIDENCE_SOURCES.contains(trimmed) ? trimmed : EVIDENCE_SOURCE_SYSTEM_GENERATED;
    }

    public static boolean isAllowedSource(String raw) {
        if (raw == null) {
            return false;
        }
        return ALLOWED_EVIDENCE_SOURCES.contains(raw.trim());
    }
}
