package org.example.trademodel.service.impl;

import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.ai.AiUsageGuard;
import org.example.trademodel.ai.AiUsageGuardResult;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.service.AiCallLogService;
import org.example.trademodel.service.DecisionChainAiOrchestratorService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class DecisionChainAiOrchestratorServiceImpl implements DecisionChainAiOrchestratorService {
    private final Map<AiProviderRole, AiProviderClient> clients;
    private final AiUsageGuard usageGuard;
    private final AiCallLogService callLogService;
    private final AiOrchestratorProperties properties;

    public DecisionChainAiOrchestratorServiceImpl(List<AiProviderClient> providerClients,
                                                  AiUsageGuard usageGuard,
                                                  AiCallLogService callLogService,
                                                  AiOrchestratorProperties properties) {
        Map<AiProviderRole, AiProviderClient> indexed = new EnumMap<>(AiProviderRole.class);
        if (providerClients != null) {
            for (AiProviderClient client : providerClients) {
                indexed.putIfAbsent(client.role(), client);
            }
        }
        this.clients = Map.copyOf(indexed);
        this.usageGuard = usageGuard;
        this.callLogService = callLogService;
        this.properties = properties;
    }

    @Override
    public AiDecisionChainResult invoke(AiDecisionChainRequest request) {
        long startedNanos = System.nanoTime();
        if (request == null || request.getRole() == null) {
            return AiDecisionChainResult.failed(null, null, AiProviderCallStatus.FAILED,
                    "DECISION_CHAIN_REQUEST_INVALID");
        }
        AiProviderClient client = clients.get(providerRole(request.getRole()));
        if (client == null) {
            AiDecisionChainResult missing = AiDecisionChainResult.failed(providerName(request.getRole()), request.getRole(),
                    AiProviderCallStatus.FAILED, "DECISION_CHAIN_PROVIDER_MISSING");
            missing.setLatencyMs(elapsedMs(startedNanos));
            recordTerminalTrace(request, missing.getProvider(), "NOT_CONFIGURED", missing, BigDecimal.ZERO);
            return missing;
        }
        AiUsageGuardResult guard;
        try {
            guard = usageGuard.evaluate(client, request.getAnalysisId());
        } catch (Exception exception) {
            AiDecisionChainResult failed = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    AiProviderCallStatus.FAILED, "AI_USAGE_GUARD_FAILED");
            failed.setLatencyMs(elapsedMs(startedNanos));
            recordTerminalTrace(request, client.provider(), modelName(client), failed, BigDecimal.ZERO);
            return failed;
        }
        BigDecimal reserved = guard.getReservedCostUsd();
        AiCallLogDO log;
        try {
            log = callLogService.startDecisionChainCall(request, client, reserved);
        } catch (Exception exception) {
            AiDecisionChainResult failed = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    AiProviderCallStatus.FAILED, "AI_CALL_LOG_START_FAILED");
            failed.setLatencyMs(elapsedMs(startedNanos));
            recordTerminalTrace(request, client.provider(), modelName(client), failed, reserved);
            return failed;
        }
        AiDecisionChainResult result;
        if (!guard.isAllowed()) {
            result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    guard.getStatus(), guard.getReasonCode());
        } else {
            long timeoutMs = properties.getProviderTimeouts() != null
                    ? properties.getProviderTimeouts().timeoutMs(client.provider())
                    : properties.getRequestTimeoutMs();
            try {
                result = client.executeDecisionChain(request, Math.max(1L, timeoutMs));
            } catch (Exception exception) {
                result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                        AiProviderCallStatus.FAILED, "DECISION_CHAIN_PROVIDER_EXCEPTION");
                result.setLatencyMs(elapsedMs(startedNanos));
            }
            if (result == null) {
                result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                        AiProviderCallStatus.FAILED, "DECISION_CHAIN_PROVIDER_NULL_RESULT");
            }
        }
        if (result.getLatencyMs() == null) result.setLatencyMs(elapsedMs(startedNanos));
        result.setReservedCostUsd(reserved);
        try {
            callLogService.completeDecisionChainCall(log, result);
        } catch (Exception exception) {
            result.setFallback(true);
            result.setFallbackReason("AI_CALL_LOG_COMPLETE_FAILED");
            result.setErrorCode("AI_CALL_LOG_COMPLETE_FAILED");
            recordTerminalTrace(request, client.provider(), modelName(client), result, reserved);
        }
        return result;
    }

    private void recordTerminalTrace(AiDecisionChainRequest request,
                                     AiProviderName provider,
                                     String modelName,
                                     AiDecisionChainResult result,
                                     BigDecimal reservedCost) {
        try {
            callLogService.recordDecisionChainResult(request, provider, modelName, result, reservedCost);
        } catch (Exception ignored) {
            // The rule fallback still proceeds when audit persistence itself is unavailable.
        }
    }

    private static String modelName(AiProviderClient client) {
        if (client == null || client.providerProperties() == null
                || client.providerProperties().getEffectiveModel() == null
                || client.providerProperties().getEffectiveModel().isBlank()) {
            return "UNAVAILABLE";
        }
        return client.providerProperties().getEffectiveModel();
    }

    private static long elapsedMs(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private static AiProviderRole providerRole(AiDecisionChainRole role) {
        return switch (role) {
            case GPT_FINAL -> AiProviderRole.GPT_RULE_REVIEW;
            case GEMINI_REVIEW -> AiProviderRole.GEMINI_CONSISTENCY_REVIEW;
            case GROK_CHALLENGE -> AiProviderRole.GROK_ADVERSARIAL_CHALLENGE;
        };
    }

    private static AiProviderName providerName(AiDecisionChainRole role) {
        return switch (role) {
            case GPT_FINAL -> AiProviderName.OPENAI;
            case GEMINI_REVIEW -> AiProviderName.GEMINI;
            case GROK_CHALLENGE -> AiProviderName.XAI;
        };
    }
}
