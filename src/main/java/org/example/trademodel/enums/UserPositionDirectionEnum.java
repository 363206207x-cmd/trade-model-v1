package org.example.trademodel.enums;

public enum UserPositionDirectionEnum {
    LONG,
    SHORT;

    public static UserPositionDirectionEnum fromSide(UserPositionSideEnum side) {
        if (side == null) {
            throw new IllegalArgumentException("side is required");
        }
        return UserPositionDirectionEnum.valueOf(side.name());
    }
}
