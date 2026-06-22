package org.example.trademodel.enums;

public enum UserPositionSourceTypeEnum {
    MANUAL;

    public static UserPositionSourceTypeEnum requireManual(String value) {
        if (value == null || value.isBlank()) {
            return MANUAL;
        }
        String normalized = value.trim().toUpperCase();
        if (!MANUAL.name().equals(normalized)) {
            throw new IllegalArgumentException("UserPosition source_type must be MANUAL");
        }
        return MANUAL;
    }
}
