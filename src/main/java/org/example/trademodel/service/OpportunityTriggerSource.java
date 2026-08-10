package org.example.trademodel.service;

public enum OpportunityTriggerSource {
    ANALYSIS(100),
    ASSET_POOL_SCAN(110),
    INVALIDATION(200),
    CONFUSED(300),
    HOT_RESET(400),
    LEGACY_ANALYSIS(50);

    private final int priority;

    OpportunityTriggerSource(int priority) {
        this.priority = priority;
    }

    public int priority() {
        return priority;
    }
}
