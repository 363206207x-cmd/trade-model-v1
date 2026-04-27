package org.example.trademodel.service;

public interface RuleEngineService {
    RuleBaseOutput execute(DecisionContext ctx);
}
