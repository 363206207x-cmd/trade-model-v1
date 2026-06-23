package org.example.trademodel.opportunitylog;

public final class OpportunityLogStatus {
    private OpportunityLogStatus() {
    }

    public static final String PENDING_EVALUATION = "PENDING_EVALUATION";
    public static final String RESOLVED = "RESOLVED";
    public static final String SOURCE_INCOMPLETE = "SOURCE_INCOMPLETE";
    public static final String MARKET_PATH_UNAVAILABLE = "MARKET_PATH_UNAVAILABLE";
    public static final String AMBIGUOUS_MARKET_PATH = "AMBIGUOUS_MARKET_PATH";
    public static final String REVIEW_REQUIRED = "REVIEW_REQUIRED";

    public static final String EXECUTED_VALID = "EXECUTED_VALID";
    public static final String EXECUTED_INVALID = "EXECUTED_INVALID";
    public static final String MISSED_VALID = "MISSED_VALID";
    public static final String MISSED_INVALID = "MISSED_INVALID";
    public static final String PUSHED_NOT_FILLED_VALID = "PUSHED_NOT_FILLED_VALID";
    public static final String BLOCKED_BY_RISK_VALID = "BLOCKED_BY_RISK_VALID";

    public static final String TARGET_FIRST = "TARGET_FIRST";
    public static final String INVALIDATION_FIRST = "INVALIDATION_FIRST";
    public static final String AMBIGUOUS_SAME_BAR = "AMBIGUOUS_SAME_BAR";
}
