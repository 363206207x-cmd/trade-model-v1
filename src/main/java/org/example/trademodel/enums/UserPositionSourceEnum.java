package org.example.trademodel.enums;

public enum UserPositionSourceEnum {
    MANUAL_INDEPENDENT,
    SYSTEM_PLAN_POSITION;

    public static UserPositionSourceEnum fromSourceType(UserPositionSourceTypeEnum sourceType) {
        if (sourceType == null) {
            throw new IllegalArgumentException("source_type is required");
        }
        return UserPositionSourceEnum.valueOf(sourceType.name());
    }
}
