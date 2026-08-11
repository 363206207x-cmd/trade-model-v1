package org.example.trademodel.enums;

public enum UserPositionSourceTypeEnum {
    MANUAL_POSITION(false),
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
        if ("MANUAL".equals(normalized)) {
            return MANUAL_POSITION;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "UserPosition source_type must be MANUAL_POSITION or SYSTEM_PLAN_POSITION");
        }
    }
}
