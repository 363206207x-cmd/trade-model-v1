package org.example.trademodel.service;

import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderReadiness;
import org.example.trademodel.ai.AiProviderRequest;

import java.util.List;

public interface AiDecisionOrchestratorService {
    AiOrchestratorResult review(AiProviderRequest request);

    List<AiProviderReadiness> providerReadiness();
}
