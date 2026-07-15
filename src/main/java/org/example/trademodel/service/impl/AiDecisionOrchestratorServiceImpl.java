package org.example.trademodel.service.impl;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderExecutor;
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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class AiDecisionOrchestratorServiceImpl implements AiDecisionOrchestratorService {
    private final List<AiProviderClient> providerClients;
    private final AiUsageGuard usageGuard;
    private final AiCallLogService callLogService;
    private final AiOrchestratorProperties properties;
    private final AiProviderExecutor providerExecutor;
    private final TimeSource timeSource;
    private final CompletionWaiter completionWaiter;

    @Autowired
    public AiDecisionOrchestratorServiceImpl(List<AiProviderClient> providerClients,
                                             AiUsageGuard usageGuard,
                                             AiCallLogService callLogService,
                                             AiOrchestratorProperties properties,
                                             AiProviderExecutor providerExecutor) {
        this(providerClients, usageGuard, callLogService, properties, providerExecutor,
                System::nanoTime, (service, waitNanos) ->
                        service.poll(Math.max(1L, waitNanos), TimeUnit.NANOSECONDS));
    }

    AiDecisionOrchestratorServiceImpl(List<AiProviderClient> providerClients,
                                      AiUsageGuard usageGuard,
                                      AiCallLogService callLogService,
                                      AiOrchestratorProperties properties,
                                      AiProviderExecutor providerExecutor,
                                      TimeSource timeSource,
                                      CompletionWaiter completionWaiter) {
        this.providerClients = providerClients == null ? List.of() : providerClients.stream()
                .sorted(Comparator.comparingInt(client -> roleOrder(client.role())))
                .limit(AiProviderExecutor.THREAD_COUNT)
                .toList();
        this.usageGuard = usageGuard;
        this.callLogService = callLogService;
        this.properties = properties;
        this.providerExecutor = providerExecutor;
        this.timeSource = timeSource == null ? System::nanoTime : timeSource;
        this.completionWaiter = completionWaiter == null
                ? (service, waitNanos) -> service.poll(Math.max(1L, waitNanos), TimeUnit.NANOSECONDS)
                : completionWaiter;
    }

    @Override
    public AiOrchestratorResult review(AiProviderRequest request) {
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setAnalysisId(request.getAnalysisId());
        result.setTraceId(request.getTraceId());
        result.setOrchestrationStartedAt(LocalDateTime.now());
        long startedNanos = timeSource.nanoTime();
        Map<AiProviderRole, AiProviderReviewResult> resultsByRole = new EnumMap<>(AiProviderRole.class);
        List<String> reasonCodes = new ArrayList<>();
        Counters counters = new Counters();

        if (!properties.isEnabled()) {
            for (AiProviderClient client : providerClients) {
                AiProviderReviewResult skipped = AiProviderReviewResult.skipped(
                        client.provider(), client.role(), AiProviderCallStatus.DISABLED,
                        "AI_ORCHESTRATOR_DISABLED");
                resultsByRole.put(client.role(), recordSkipped(request, client, skipped, BigDecimal.ZERO));
            }
            return finalizeResult(result, orderedResults(resultsByRole), reasonCodes,
                    startedNanos, counters);
        }

        AiOrchestratorProperties.ProviderTimeouts timeouts = properties.getProviderTimeouts();
        if (timeouts == null || !timeouts.validOverall()) {
            for (AiProviderClient client : providerClients) {
                AiProviderReviewResult skipped = failedResult(
                        client, "ORCHESTRATOR_TIMEOUT_CONFIG_INVALID");
                resultsByRole.put(client.role(), recordSkipped(request, client, skipped, BigDecimal.ZERO));
            }
            return finalizeResult(result, orderedResults(resultsByRole), reasonCodes,
                    startedNanos, counters);
        }

        long globalDeadlineNanos = deadlineNanos(startedNanos, timeouts.getOverallMs());
        CompletionService<ProviderCompletion> completionService =
                new ExecutorCompletionService<>(providerExecutor.executorService());
        Map<Future<ProviderCompletion>, ProviderInvocation> pending = new LinkedHashMap<>();

        for (AiProviderClient client : providerClients) {
            if (!timeouts.validProvider(client.provider())) {
                AiProviderReviewResult skipped = failedResult(client, "PROVIDER_TIMEOUT_CONFIG_INVALID");
                resultsByRole.put(client.role(), recordSkipped(request, client, skipped, BigDecimal.ZERO));
                continue;
            }

            AiUsageGuardResult guard = usageGuard.evaluate(client, request.getAnalysisId());
            if (!guard.isAllowed()) {
                AiProviderReviewResult skipped = AiProviderReviewResult.skipped(
                        client.provider(), client.role(), guard.getStatus(), guard.getReasonCode());
                skipped.setReservedCostUsd(guard.getReservedCostUsd());
                resultsByRole.put(client.role(),
                        recordSkipped(request, client, skipped, guard.getReservedCostUsd()));
                continue;
            }

            AiCallLogDO log;
            try {
                log = callLogService.startCall(request, client, guard.getReservedCostUsd());
            } catch (Exception exception) {
                AiProviderReviewResult skipped = failedResult(client, "AI_CALL_LOG_START_FAILED");
                skipped.setReservedCostUsd(guard.getReservedCostUsd());
                resultsByRole.put(client.role(), skipped);
                continue;
            }

            long submittedNanos = timeSource.nanoTime();
            int providerTimeoutMs = timeouts.timeoutMs(client.provider());
            ProviderInvocation invocation = new ProviderInvocation(
                    client, log, guard.getReservedCostUsd(), submittedNanos,
                    deadlineNanos(submittedNanos, providerTimeoutMs), globalDeadlineNanos);
            try {
                Future<ProviderCompletion> future = completionService.submit(
                        providerExecutor.namedTask(client.provider(),
                                () -> executeProvider(request, invocation, providerTimeoutMs)));
                pending.put(future, invocation);
                counters.submitted++;
            } catch (RejectedExecutionException exception) {
                ProviderCompletion rejected = invocation.settle(
                        failedResult(client, "AI_PROVIDER_EXECUTOR_REJECTED"), false);
                resultsByRole.put(client.role(), rejected.result());
            }
        }

        waitForProviders(completionService, pending, resultsByRole, counters, globalDeadlineNanos);
        return finalizeResult(result, orderedResults(resultsByRole), reasonCodes,
                startedNanos, counters);
    }

    @Override
    public List<AiProviderReadiness> providerReadiness() {
        return providerClients.stream().map(AiProviderClient::readiness).toList();
    }

    private ProviderCompletion executeProvider(AiProviderRequest request,
                                               ProviderInvocation invocation,
                                               int providerTimeoutMs) {
        long beforeCallNanos = timeSource.nanoTime();
        if (beforeCallNanos >= invocation.globalDeadlineNanos) {
            return invocation.settle(timeoutResult(invocation.client,
                    "ORCHESTRATOR_OVERALL_TIMEOUT", invocation.startedNanos), false);
        }
        if (beforeCallNanos >= invocation.providerDeadlineNanos) {
            return invocation.settle(timeoutResult(invocation.client,
                    "PROVIDER_TIMEOUT", invocation.startedNanos), false);
        }
        long effectiveTimeoutMs = Math.min(providerTimeoutMs,
                Math.min(remainingTimeoutMs(invocation.providerDeadlineNanos, beforeCallNanos),
                        remainingTimeoutMs(invocation.globalDeadlineNanos, beforeCallNanos)));
        AiProviderReviewResult providerResult;
        try {
            providerResult = invocation.client.review(request, effectiveTimeoutMs);
            if (providerResult == null) {
                providerResult = failedResult(invocation.client, "PROVIDER_NULL_RESULT");
            }
        } catch (Exception exception) {
            providerResult = failedResult(invocation.client, "PROVIDER_THROWN_FAILURE");
        }
        providerResult.setReservedCostUsd(invocation.reservedCostUsd);

        long completedNanos = timeSource.nanoTime();
        if (completedNanos >= invocation.globalDeadlineNanos) {
            return invocation.settle(timeoutResult(invocation.client,
                    "ORCHESTRATOR_OVERALL_TIMEOUT", invocation.startedNanos), true);
        }
        if (completedNanos >= invocation.providerDeadlineNanos) {
            return invocation.settle(timeoutResult(invocation.client,
                    "PROVIDER_TIMEOUT", invocation.startedNanos), true);
        }
        return invocation.settle(providerResult, true);
    }

    private void waitForProviders(CompletionService<ProviderCompletion> completionService,
                                  Map<Future<ProviderCompletion>, ProviderInvocation> pending,
                                  Map<AiProviderRole, AiProviderReviewResult> resultsByRole,
                                  Counters counters,
                                  long globalDeadlineNanos) {
        while (!pending.isEmpty()) {
            Future<ProviderCompletion> completedFuture;
            while ((completedFuture = completionService.poll()) != null) {
                acceptCompletion(completedFuture, pending, resultsByRole, counters);
            }
            if (pending.isEmpty()) {
                return;
            }

            long now = timeSource.nanoTime();
            if (now >= globalDeadlineNanos) {
                counters.globalDeadlineExceeded = true;
                timeoutPending(pending, resultsByRole, "ORCHESTRATOR_OVERALL_TIMEOUT", true);
                return;
            }
            timeoutExpiredProviders(pending, resultsByRole, now);
            if (pending.isEmpty()) {
                return;
            }

            long nextDeadline = globalDeadlineNanos;
            for (ProviderInvocation invocation : pending.values()) {
                nextDeadline = Math.min(nextDeadline, invocation.providerDeadlineNanos);
            }
            try {
                completedFuture = completionWaiter.poll(
                        completionService, Math.max(1L, nextDeadline - timeSource.nanoTime()));
                if (completedFuture != null) {
                    acceptCompletion(completedFuture, pending, resultsByRole, counters);
                }
            } catch (InterruptedException exception) {
                timeoutPending(pending, resultsByRole, "ORCHESTRATOR_INTERRUPTED", true);
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void timeoutExpiredProviders(
            Map<Future<ProviderCompletion>, ProviderInvocation> pending,
            Map<AiProviderRole, AiProviderReviewResult> resultsByRole,
            long now) {
        List<Map.Entry<Future<ProviderCompletion>, ProviderInvocation>> expired = pending.entrySet().stream()
                .filter(entry -> now >= entry.getValue().providerDeadlineNanos)
                .toList();
        for (Map.Entry<Future<ProviderCompletion>, ProviderInvocation> entry : expired) {
            settleTimeout(entry.getKey(), entry.getValue(), resultsByRole, "PROVIDER_TIMEOUT");
            pending.remove(entry.getKey());
        }
    }

    private void timeoutPending(Map<Future<ProviderCompletion>, ProviderInvocation> pending,
                                Map<AiProviderRole, AiProviderReviewResult> resultsByRole,
                                String reason,
                                boolean cancel) {
        for (Map.Entry<Future<ProviderCompletion>, ProviderInvocation> entry
                : List.copyOf(pending.entrySet())) {
            settleTimeout(entry.getKey(), entry.getValue(), resultsByRole, reason);
            if (cancel) {
                entry.getKey().cancel(true);
            }
            pending.remove(entry.getKey());
        }
    }

    private void settleTimeout(Future<ProviderCompletion> future,
                               ProviderInvocation invocation,
                               Map<AiProviderRole, AiProviderReviewResult> resultsByRole,
                               String reason) {
        ProviderCompletion completion = invocation.settle(
                timeoutResult(invocation.client, reason, invocation.startedNanos), false);
        resultsByRole.put(invocation.client.role(), completion.result());
        future.cancel(true);
    }

    private void acceptCompletion(Future<ProviderCompletion> future,
                                  Map<Future<ProviderCompletion>, ProviderInvocation> pending,
                                  Map<AiProviderRole, AiProviderReviewResult> resultsByRole,
                                  Counters counters) {
        ProviderInvocation invocation = pending.remove(future);
        if (invocation == null) {
            return;
        }
        try {
            ProviderCompletion completion = future.get();
            resultsByRole.put(invocation.client.role(), completion.result());
            if (completion.providerReturned() && completion.accepted()) {
                counters.completed++;
            }
        } catch (CancellationException ignored) {
            resultsByRole.put(invocation.client.role(), invocation.terminalResult());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            ProviderCompletion completion = invocation.settle(
                    timeoutResult(invocation.client, "ORCHESTRATOR_INTERRUPTED", invocation.startedNanos), false);
            resultsByRole.put(invocation.client.role(), completion.result());
        } catch (ExecutionException exception) {
            ProviderCompletion completion = invocation.settle(
                    failedResult(invocation.client, "PROVIDER_TASK_FAILURE"), false);
            resultsByRole.put(invocation.client.role(), completion.result());
        }
    }

    private AiProviderReviewResult recordSkipped(AiProviderRequest request, AiProviderClient client,
                                                 AiProviderReviewResult result,
                                                 BigDecimal reservedCostUsd) {
        try {
            callLogService.recordSkipped(request, client, result, reservedCostUsd);
        } catch (Exception exception) {
            result.setErrorCode("AI_CALL_LOG_SKIPPED_FAILED");
            result.setFallbackReason("AI_CALL_LOG_SKIPPED_FAILED");
        }
        return result;
    }

    private AiProviderReviewResult timeoutResult(AiProviderClient client, String reason,
                                                 long startedNanos) {
        AiProviderReviewResult result = AiProviderReviewResult.skipped(
                client.provider(), client.role(), AiProviderCallStatus.TIMEOUT, reason);
        result.setErrorCode(reason);
        result.setTimeout(true);
        result.setLatencyMs(elapsedMs(startedNanos));
        return result;
    }

    private static AiProviderReviewResult failedResult(AiProviderClient client, String reason) {
        AiProviderReviewResult result = AiProviderReviewResult.skipped(
                client.provider(), client.role(), AiProviderCallStatus.FAILED, reason);
        result.setErrorCode(reason);
        return result;
    }

    private List<AiProviderReviewResult> orderedResults(
            Map<AiProviderRole, AiProviderReviewResult> resultsByRole) {
        List<AiProviderReviewResult> ordered = new ArrayList<>();
        for (AiProviderClient client : providerClients) {
            AiProviderReviewResult result = resultsByRole.get(client.role());
            if (result != null) {
                ordered.add(result);
            }
        }
        return ordered;
    }

    private AiOrchestratorResult finalizeResult(AiOrchestratorResult result,
                                                List<AiProviderReviewResult> providerResults,
                                                List<String> reasonCodes,
                                                long startedNanos,
                                                Counters counters) {
        int success = 0;
        int fallback = 0;
        int objection = 0;
        int support = 0;
        int conflictContribution = 0;
        int timeout = 0;
        int failed = 0;
        boolean overallTimeoutObserved = false;
        boolean gptConsistent = false;
        boolean geminiConsistent = false;
        boolean grokConsistent = false;

        for (AiProviderReviewResult providerResult : providerResults) {
            if (providerResult.successful()) {
                success++;
            } else if (providerResult.getCallStatus() == AiProviderCallStatus.TIMEOUT) {
                timeout++;
                overallTimeoutObserved = overallTimeoutObserved
                        || "ORCHESTRATOR_OVERALL_TIMEOUT".equals(providerResult.getErrorCode());
            } else {
                failed++;
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
                if (providerResult.getRole() == AiProviderRole.GPT_RULE_REVIEW) {
                    gptConsistent = true;
                } else if (providerResult.getRole() == AiProviderRole.GEMINI_CONSISTENCY_REVIEW) {
                    geminiConsistent = true;
                } else if (providerResult.getRole() == AiProviderRole.GROK_ADVERSARIAL_CHALLENGE) {
                    grokConsistent = true;
                }
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

        LocalDateTime completedAt = LocalDateTime.now();
        result.setCompletedAt(completedAt);
        result.setOrchestrationCompletedAt(completedAt);
        result.setOrchestrationLatencyMs(elapsedMs(startedNanos));
        result.setProviderSubmittedCount(counters.submitted);
        result.setProviderCompletedCount(counters.completed);
        result.setProviderTimeoutCount(timeout);
        result.setProviderFailedCount(failed);
        result.setProviderSuccessCount(success);
        result.setGlobalDeadlineExceeded(counters.globalDeadlineExceeded || overallTimeoutObserved);

        if (success == 0) {
            result.setOrchestrationMode(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        } else if (success == providerResults.size() && fallback == 0) {
            result.setOrchestrationMode(AiOrchestrationMode.AI_ASSISTED);
        } else {
            result.setOrchestrationMode(AiOrchestrationMode.PARTIAL_FALLBACK);
        }
        result.setPartialFallbackUsed(result.getOrchestrationMode() == AiOrchestrationMode.PARTIAL_FALLBACK);
        return result;
    }

    private long elapsedMs(long startedNanos) {
        long elapsedNanos = Math.max(0L, timeSource.nanoTime() - startedNanos);
        return TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
    }

    private static long remainingTimeoutMs(long deadlineNanos, long nowNanos) {
        long remainingNanos = Math.max(1L, deadlineNanos - nowNanos);
        long wholeMillis = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
        return remainingNanos % TimeUnit.MILLISECONDS.toNanos(1L) == 0
                ? Math.max(1L, wholeMillis)
                : Math.max(1L, wholeMillis + 1L);
    }

    private static long deadlineNanos(long startedNanos, long timeoutMs) {
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        if (timeoutNanos > Long.MAX_VALUE - startedNanos) {
            return Long.MAX_VALUE;
        }
        return startedNanos + timeoutNanos;
    }

    private static int roleOrder(AiProviderRole role) {
        if (role == AiProviderRole.GPT_RULE_REVIEW) {
            return 0;
        }
        if (role == AiProviderRole.GEMINI_CONSISTENCY_REVIEW) {
            return 1;
        }
        if (role == AiProviderRole.GROK_ADVERSARIAL_CHALLENGE) {
            return 2;
        }
        return 3;
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

    private final class ProviderInvocation {
        private final AiProviderClient client;
        private final AiCallLogDO log;
        private final BigDecimal reservedCostUsd;
        private final long startedNanos;
        private final long providerDeadlineNanos;
        private final long globalDeadlineNanos;
        private boolean settled;
        private AiProviderReviewResult terminalResult;

        private ProviderInvocation(AiProviderClient client,
                                   AiCallLogDO log,
                                   BigDecimal reservedCostUsd,
                                   long startedNanos,
                                   long providerDeadlineNanos,
                                   long globalDeadlineNanos) {
            this.client = client;
            this.log = log;
            this.reservedCostUsd = reservedCostUsd;
            this.startedNanos = startedNanos;
            this.providerDeadlineNanos = providerDeadlineNanos;
            this.globalDeadlineNanos = globalDeadlineNanos;
        }

        private synchronized ProviderCompletion settle(
                AiProviderReviewResult result, boolean providerReturned) {
            if (settled) {
                return new ProviderCompletion(this, terminalResult, false, providerReturned);
            }
            settled = true;
            result.setReservedCostUsd(reservedCostUsd);
            terminalResult = result;
            try {
                callLogService.completeCall(log, result);
            } catch (Exception exception) {
                result.setReasonCodes(appendReason(result.getReasonCodes(), "AI_CALL_LOG_COMPLETE_FAILED"));
                if (result.successful()) {
                    result.setCallStatus(AiProviderCallStatus.FAILED);
                    result.setFallback(true);
                    result.setFallbackReason("AI_CALL_LOG_COMPLETE_FAILED");
                    result.setErrorCode("AI_CALL_LOG_COMPLETE_FAILED");
                }
            }
            return new ProviderCompletion(this, result, true, providerReturned);
        }

        private synchronized AiProviderReviewResult terminalResult() {
            return terminalResult == null
                    ? timeoutResult(client, "ORCHESTRATOR_OVERALL_TIMEOUT", startedNanos)
                    : terminalResult;
        }

    }

    private static List<String> appendReason(List<String> reasons, String reason) {
        List<String> copy = new ArrayList<>(reasons == null ? List.of() : reasons);
        copy.add(reason);
        return copy;
    }

    private static final class Counters {
        private int submitted;
        private int completed;
        private boolean globalDeadlineExceeded;
    }

    record ProviderCompletion(ProviderInvocation invocation,
                              AiProviderReviewResult result,
                              boolean accepted,
                              boolean providerReturned) {
    }

    interface TimeSource {
        long nanoTime();
    }

    interface CompletionWaiter {
        Future<ProviderCompletion> poll(CompletionService<ProviderCompletion> completionService,
                                        long waitNanos) throws InterruptedException;
    }
}
