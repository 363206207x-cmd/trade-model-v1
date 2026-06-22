package org.example.trademodel.enums;

public enum RecheckStatusEnum {
    REVIEW_PASSED,
    REVIEW_WAITING,
    DRIFTED_FROM_ENTRY_ZONE,
    @Deprecated
    DRIFTED,
    INVALIDATED,
    RISK_BLOCKED,
    CONFUSED_BLOCKED,
    EXPIRED
}
