package org.example.trademodel.enums;

import java.util.Locale;

/** Frozen execution-permission levels from the v4.1 final product contract. */
public enum PlanModeEnum {
    CONFIRMATION,
    PREPARATION,
    REDUCED,
    OBSERVATION,
    BLOCKED;

    public static PlanModeEnum require(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("plan mode is required");
        }
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    public PlanModeEnum downgrade(int levels) {
        int target = Math.min(values().length - 1, ordinal() + Math.max(0, levels));
        return values()[target];
    }

    public boolean morePermissiveThan(PlanModeEnum other) {
        return other != null && ordinal() < other.ordinal();
    }
}
