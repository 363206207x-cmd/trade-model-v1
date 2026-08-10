package org.example.trademodel.service;

import org.example.trademodel.decisionchain.DecisionChainBuildInput;
import org.example.trademodel.decisionchain.DecisionChainBuildResult;

public interface DecisionChainService {
    DecisionChainBuildResult build(DecisionChainBuildInput input);

    void persist(DecisionChainBuildResult result);
}
