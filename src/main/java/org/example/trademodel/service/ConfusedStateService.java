package org.example.trademodel.service;

public interface ConfusedStateService {
    ConfusedResult calculateConfused(String symbol, DecisionContext context);

    default ConfusedResult calculateConfused(String symbol, String timeframe, DecisionContext context) {
        return calculateConfused(symbol, context);
    }

    default ConfusedResult calculateConfused(OpportunityStateIdentity identity, DecisionContext context) {
        if (identity == null) {
            throw new IllegalArgumentException("opportunity identity is required");
        }
        return calculateConfused(identity.symbol(), identity.timeframe(), context);
    }
}
