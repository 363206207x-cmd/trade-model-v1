package org.example.trademodel.enums;

public enum UserPositionSideEnum {
    LONG,
    SHORT;

    public static UserPositionSideEnum parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("side is required");
        }
        try {
            return UserPositionSideEnum.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("side must be LONG or SHORT");
        }
    }
}
