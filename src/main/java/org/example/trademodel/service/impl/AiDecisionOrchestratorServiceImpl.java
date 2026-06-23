package org.example.trademodel.service.impl;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderReadiness;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.ai.AiReviewConflictLevel;
import org.example.trademodel.ai.AiUsageGuard;
import org.example.trademodel.ai.AiUsageGuardResult;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.service.AiCallLogService;
import org.example.trademodel.service.AiDecisionOrchestratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AiDecisionOrchestratorServiceImpl implements AiDecisionOrchestratorService {
    private final List<AiProviderClient> providerClients;
    private final AiUsageGuard usageGuard;
    private final AiCallLogService callLogService;
    private final AiOrchestratorProperties properties;
    private final TimeSource timeSource;

    @Autowired
    public AiDecisionOrchestratorServiceImpl(List<AiProviderClient> providerClients,
                                             AiUsageGuard usageGuard,
                                             AiCallLogService callLogService,
                                             AiOrchestratorProperties properties) {
        this(providerClients, usageGuard, callLogService, properties, System::nanoTime);
    }

    AiDecisionOrchestratorServiceImpl(List<AiProviderClient> providerClients,
                                      AiUsageGuard usageGuard,
                                      AiCallLogService callLogService,
                                      AiOrchestratorProperties properties,
                                      TimeSource timeSource) {
        this.providerClients = providerClients == null ? List.of() : providerClients.stream()
                .sorted(Comparator.comparing(client -> client.role().name()))
                .toList();
        this.usageGuard = usageGuard;
        this.callLogService = callLogService;
        this.properties = properties;
        this.timeSource = timeSource == null ? System::nanoTime : timeSource;
    }

    @Override
    public AiOrchestratorResult review(AiProviderRequest request) {
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setAnalysisId(request.getAnalysisId());
        result.setTraceId(request.getTraceId());
        List<AiProviderReviewResult> providerResults = new ArrayList<>();
        List<String> reasonCodes = new ArrayList<>();
        long startedNanos = timeSource.nanoTime();

        if (!properties.isEnabled()) {
            for (AiProviderClient client : providerClients) {
                AiProviderReviewResult skipped = AiProviderReviewResult.skipped(
                        client.provider(), client.role(), AiProviderCallStatus.DISABLED, "AI_ORCHESTRATOR_DISABLED");
                providerResults.add(recordSkipped(request, client, skipped, BigDecimal.ZERO));
            }
            return finalizeResult(result, providerResults, reasonCodes);
        }
        if (properties.getOverallTimeoutMs() <= 0) {
            for (AiProviderClient client : providerClients) {
                AiProviderReviewResult skipped = overallTimeoutResult(client);
                providerResults.add(recordSkipped(request, client, skipped, BigDecimal.ZERO));
            }
            return finalizeResult(result, providerResults, reasonCodes);
        }

        long deadlineNanos = deadlineNanos(startedNanos, properties.getOverallTimeoutMs());

        for (AiProviderClient client : providerClients) {
            long remainingMs = remainingTimeoutMs(deadlineNanos);
            if (remainingMs <= 0) {
                AiProviderReviewResult skipped = overallTimeoutResult(client);
                providerResults.add(recordSkipped(request, client, skipped, BigDecimal.ZERO));
                continue;
            }

            AiUsageGuardResult guard = usageGuard.evaluate(client, request.getAnalysisId());
            if (!guard.isAllowed()) {
                AiProviderReviewResult skipped = AiProviderReviewResult.skipped(
                        client.provider(), client.role(), guard.getStatus(), guard.getReasonCode());
                skipped.setReservedCostUsd(guard.getReservedCostUsd());
                providerResults.add(recordSkipped(request, client, skipped, guard.getReservedCostUsd()));
                continue;
            }

            AiCallLogDO log;
            try {
                log = callLogService.startCall(request, client, guard.getReservedCostUsd());
            } catch (Exception e) {
                AiProviderReviewResult skipped = AiProviderReviewResult.skipped(
                        client.provider(), client.role(), AiProviderCallStatus.FAILED, "AI_CALL_LOG_START_FAILED");
                skipped.setReservedCostUsd(guard.getReservedCostUsd());
                providerResults.add(skipped);
                continue;
            }

            AiProviderReviewResult providerResult;
            try {
                remainingMs = remainingTimeoutMs(deadlineNanos);
                if (remainingMs <= 0) {
                    providerResult = overallTimeoutResult(client);
                } else {
                    providerResult = client.review(request, effectiveProviderTimeoutMs(remainingMs));
                }
            } catch (Exception e) {
                providerResult = AiProviderReviewResult.skipped(
                        client.provider(), client.role(), AiProviderCallStatus.FAILED, "PROVIDER_THROWN_FAILURE");
            }
            providerResult.setReservedCostUsd(guard.getReservedCostUsd());
            try {
                callLogService.completeCall(log, providerResult);
            } catch (Exception e) {
                providerResult = AiProviderReviewResult.skipped(
                        client.provider(), client.role(), AiProviderCallStatus.FAILED, "AI_CALL_LOG_COMPLETE_FAILED");
                providerResult.setReservedCostUsd(guard.getReservedCostUsd());
            }
            providerResults.add(providerResult);
        }

        return finalizeResult(result, providerResults, reasonCodes);
    }

    @Override
    public List<AiProviderReadiness> providerReadiness() {
        return providerClients.stream().map(AiProviderClient::readiness).toList();
    }

    private AiProviderReviewResult recordSkipped(AiProviderRequest request, AiProviderClient client,
                                                 AiProviderReviewResult result, BigDecimal reservedCostUsd) {
        try {
            callLogService.recordSkipped(request, client, result, reservedCostUsd);
        } catch (Exception e) {
            result.setErrorCode("AI_CALL_LOG_SKIPPED_FAILED");
            result.setFallbackReason("AI_CALL_LOG_SKIPPED_FAILED");
        }
        return result;
    }

    private AiProviderReviewResult overallTimeoutResult(AiProviderClient client) {
        AiProviderReviewResult result = AiProviderReviewResult.skipped(
                client.provider(), client.role(), AiProviderCallStatus.TIMEOUT, "ORCHESTRATOR_OVERALL_TIMEOUT");
        result.setErrorCode("ORCHESTRATOR_OVERALL_TIMEOUT");
        result.setTimeout(true);
        return result;
    }

    private long effectiveProviderTimeoutMs(long remainingMs) {
        long requestTimeoutMs = properties.getRequestTimeoutMs() <= 0
                ? remainingMs
                : properties.getRequestTimeoutMs();
        return Math.max(1, Math.min(requestTimeoutMs, remainingMs));
    }

    private long remainingTimeoutMs(long deadlineNanos) {
        long remainingNanos = deadlineNanos - timeSource.nanoTime();
        if (remainingNanos <= 0) {
            return 0;
        }
        long remainingMs = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
        return remainingMs <= 0 ? 1 : remainingMs;
    }

    private static long deadlineNanos(long startedNanos, long timeoutMs) {
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        if (timeoutNanos > Long.MAX_VALUE - startedNanos) {
            return Long.MAX_VALUE;
        }
        return startedNanos + timeoutNanos;
    }

    private AiOrchestratorResult finalizeResult(AiOrchestratorResult result,
                                                List<AiProviderReviewResult> providerResults,
                                                List<String> reasonCodes) {
        int success = 0;
        int fallback = 0;
        int objection = 0;
        int support = 0;
        int conflictContribution = 0;
        boolean gptConsistent = true;
        boolean geminiConsistent = true;
        boolean grokConsistent = true;

        for (AiProviderReviewResult providerResult : providerResults) {
            if (providerResult.successful()) {
                success++;
            }
            if (!providerResult.successful() || providerResult.isFallback()) {
                fallback++;
            }
            if (providerResult.challengesRule()) {
                objection++;
                conflictContribution += contribution(providerResult.getConflictLevel());
                if (providerResult.getRole() == AiProviderRole.GPT_RULE_REVIEW) {
                    gptConsistent = false;
                } else if (providerResult.getRole() == AiProviderRole.GEMINI_CONSISTENCY_REVIEW) {
                    geminiConsistent = false;
                } else if (providerResult.getRole() == AiProviderRole.GROK_ADVERSARIAL_CHALLENGE) {
                    grokConsistent = false;
                }
            } else if (providerResult.supportsRule()) {
                support++;
            }
            if (providerResult.getReasonCodes() != null) {
                reasonCodes.addAll(providerResult.getReasonCodes());
            }
        }

        result.setProviderResults(providerResults);
        result.setSuccessfulProviderCount(success);
        result.setFailedProviderCount(providerResults.size() - success);
        result.setFallbackProviderCount(fallback);
        result.setAiObjectionCount(objection);
        result.setAiSupportCount(support);
        result.setGptConsistentWithRule(gptConsistent);
        result.setGeminiConsistentWithRule(geminiConsistent);
        result.setGrokConsistentWithRule(grokConsistent);
        result.setConflictContribution(Math.min(25, Math.max(0, conflictContribution)));
        result.setReasonCodes(reasonCodes.stream().distinct().limit(20).toList());
        result.setCompletedAt(LocalDateTime.now());
        if (success == 0) {
            result.setOrchestrationMode(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        } else if (success == providerResults.size() && fallback == 0) {
            result.setOrchestrationMode(AiOrchestrationMode.AI_ASSISTED);
        } else {
            result.setOrchestrationMode(AiOrchestrationMode.PARTIAL_FALLBACK);
        }
        return result;
    }

    private static int contribution(AiReviewConflictLevel level) {
        if (level == AiReviewConflictLevel.MINOR) {
            return 4;
        }
        if (level == AiReviewConflictLevel.MAJOR) {
            return 10;
        }
        if (level == AiReviewConflictLevel.EXTREME) {
            return 18;
        }
        return 0;
    }

    interface TimeSource {
        long nanoTime();
    }
}
