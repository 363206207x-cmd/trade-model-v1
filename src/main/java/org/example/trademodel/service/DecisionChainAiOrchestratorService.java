package org.example.trademodel.service;

import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;

public interface DecisionChainAiOrchestratorService {
    AiDecisionChainResult invoke(AiDecisionChainRequest request);
}
