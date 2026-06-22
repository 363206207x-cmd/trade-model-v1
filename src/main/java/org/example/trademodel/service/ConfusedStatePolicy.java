package org.example.trademodel.service;

/**
 * Contract-owned confused-state thresholds. RuleConfig cannot lower or bypass these gates.
 */
public final class ConfusedStatePolicy {

    public static final int CONFUSED_ENTER_THRESHOLD = 70;
    public static final int DIRECTIONAL_PUSH_BLOCK_THRESHOLD = 85;
    public static final int CONFUSED_EXIT_THRESHOLD_EXCLUSIVE = 55;
    public static final int CONFUSED_EXIT_REQUIRED_CONSECUTIVE_CYCLES = 2;

    private ConfusedStatePolicy() {
    }
}
