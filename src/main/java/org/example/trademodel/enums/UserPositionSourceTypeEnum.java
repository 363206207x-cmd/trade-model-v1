package org.example.trademodel.enums;

public enum UserPositionSourceTypeEnum {
    MANUAL_INDEPENDENT(false),
    SYSTEM_PLAN_POSITION(true);

    private final boolean finalPlanRequired;

    UserPositionSourceTypeEnum(boolean finalPlanRequired) {
        this.finalPlanRequired = finalPlanRequired;
    }

    public boolean finalPlanRequired() {
        return finalPlanRequired;
    }

    public static UserPositionSourceTypeEnum parseExplicit(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserPosition source_type is required");
        }
        String normalized = value.trim().toUpperCase();
        if ("MANUAL".equals(normalized) || "MANUAL_POSITION".equals(normalized)) {
            return MANUAL_INDEPENDENT;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "UserPosition source_type must be MANUAL_INDEPENDENT or SYSTEM_PLAN_POSITION");
        }
    }
}
