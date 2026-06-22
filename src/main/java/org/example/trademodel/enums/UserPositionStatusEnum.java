package org.example.trademodel.enums;

public enum UserPositionStatusEnum {
    OPEN,
    PARTIALLY_CLOSED,
    CLOSED;

    public boolean visibleInOpenPositions() {
        return this == OPEN || this == PARTIALLY_CLOSED;
    }

    public static UserPositionStatusEnum requireClosable(String value) {
        UserPositionStatusEnum status = parse(value);
        if (!status.visibleInOpenPositions()) {
            throw new IllegalArgumentException("Only OPEN or PARTIALLY_CLOSED UserPosition can be manually closed");
        }
        return status;
    }

    public static UserPositionStatusEnum parse(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserPosition status is required");
        }
        try {
            return UserPositionStatusEnum.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unsupported UserPosition status: " + value);
        }
    }
}
