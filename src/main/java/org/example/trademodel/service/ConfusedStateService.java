package org.example.trademodel.service;

public interface ConfusedStateService {
    ConfusedResult calculateConfused(String symbol, DecisionContext context);

    default ConfusedResult calculateConfused(String symbol, String timeframe, DecisionContext context) {
        return calculateConfused(symbol, context);
    }
}
