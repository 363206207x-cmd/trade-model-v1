package org.example.trademodel.service;

public interface ConfusedStateService {
    ConfusedResult calculateConfused(String symbol, DecisionContext context);
}
