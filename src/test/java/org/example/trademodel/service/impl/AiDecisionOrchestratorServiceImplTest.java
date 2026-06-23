package org.example.trademodel.service.impl;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderProperties;
import org.example.trademodel.ai.AiProviderReadiness;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.ai.AiReviewConflictLevel;
import org.example.trademodel.ai.AiReviewStance;
import org.example.trademodel.ai.AiUsageGuard;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.service.AiCallLogService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AiDecisionOrchestratorServiceImplTest {
    @Test
    void review_allSuccessReturnsAiAssistedAndStartsLogBeforeProviderCall() {
        AiOrchestratorProperties properties = properties(true);
        RecordingLogService logs = new RecordingLogService();
        List<AiProviderClient> clients = List.of(
                successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW, properties.getOpenai(), logs, AiReviewStance.SUPPORT),
                successClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW, properties.getGemini(), logs, AiReviewStance.SUPPORT),
                successClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE, properties.getXai(), logs, AiReviewStance.ABSTAIN)
        );
        AiDecisionOrchestratorServiceImpl service = service(properties, logs, clients);

        AiOrchestratorResult result = service.review(request());

        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.AI_ASSISTED);
        assertThat(result.getSuccessfulProviderCount()).isEqualTo(3);
        assertThat(logs.events).containsSubsequence("start:OPENAI", "review:OPENAI", "complete:OPENAI");
        assertThat(result.isRuleDirectionPreserved()).isTrue();
    }

    @Test
    void review_oneProviderFailureReturnsPartialFallback() {
        AiOrchestratorProperties properties = properties(true);
        RecordingLogService logs = new RecordingLogService();
        List<AiProviderClient> clients = List.of(
                successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW, properties.getOpenai(), logs, AiReviewStance.SUPPORT),
                failureClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW, properties.getGemini(), logs),
                successClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE, properties.getXai(), logs, AiReviewStance.ABSTAIN)
        );
        AiDecisionOrchestratorServiceImpl service = service(properties, logs, clients);

        AiOrchestratorResult result = service.review(request());

        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.PARTIAL_FALLBACK);
        assertThat(result.getFailedProviderCount()).isEqualTo(1);
        assertThat(result.toSanitizedSummary()).contains("GEMINI:FAILED");
    }

    @Test
    void review_disabledGlobalReturnsRuleOnlyFallbackAndLogsSkippedProviders() {
        AiOrchestratorProperties properties = properties(false);
        RecordingLogService logs = new RecordingLogService();
        List<AiProviderClient> clients = List.of(
                successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW, properties.getOpenai(), logs, AiReviewStance.SUPPORT)
        );
        AiDecisionOrchestratorServiceImpl service = service(properties, logs, clients);

        AiOrchestratorResult result = service.review(request());

        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        assertThat(result.getSuccessfulProviderCount()).isZero();
        assertThat(logs.events).contains("skipped:OPENAI");
    }

    @Test
    void review_challengeOnlyDowngradesThroughConflictContribution() {
        AiOrchestratorProperties properties = properties(true);
        RecordingLogService logs = new RecordingLogService();
        List<AiProviderClient> clients = List.of(
                challengeClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE, properties.getXai(), logs)
        );
        AiDecisionOrchestratorServiceImpl service = service(properties, logs, clients);

        AiOrchestratorResult result = service.review(request());

        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.AI_ASSISTED);
        assertThat(result.isGrokConsistentWithRule()).isFalse();
        assertThat(result.getAiObjectionCount()).isEqualTo(1);
        assertThat(result.getConflictContribution()).isEqualTo(10);
    }

    @Test
    void review_passesRemainingTimeoutToLaterProvidersAndSkipsAfterDeadline() {
        AiOrchestratorProperties properties = properties(true);
        properties.setRequestTimeoutMs(5000);
        properties.setOverallTimeoutMs(8000);
        RecordingLogService logs = new RecordingLogService();
        MutableTimeSource clock = new MutableTimeSource();
        TimeoutAwareClient gemini = timeoutAwareSuccessClient(AiProviderName.GEMINI,
                AiProviderRole.GEMINI_CONSISTENCY_REVIEW, properties.getGemini(), logs, clock, 4000);
        TimeoutAwareClient openai = timeoutAwareSuccessClient(AiProviderName.OPENAI,
                AiProviderRole.GPT_RULE_REVIEW, properties.getOpenai(), logs, clock, 4000);
        TimeoutAwareClient xai = timeoutAwareSuccessClient(AiProviderName.XAI,
                AiProviderRole.GROK_ADVERSARIAL_CHALLENGE, properties.getXai(), logs, clock, 0);
        AiDecisionOrchestratorServiceImpl service = service(properties, logs, List.of(openai, gemini, xai), clock);

        AiOrchestratorResult result = service.review(request());

        assertThat(gemini.timeouts).containsExactly(5000L);
        assertThat(openai.timeouts).containsExactly(4000L);
        assertThat(xai.timeouts).isEmpty();
        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.PARTIAL_FALLBACK);
        assertThat(result.getProviderResults()).filteredOn(providerResult ->
                providerResult.getProvider() == AiProviderName.XAI)
                .singleElement()
                .satisfies(providerResult -> {
                    assertThat(providerResult.getCallStatus()).isEqualTo(AiProviderCallStatus.TIMEOUT);
                    assertThat(providerResult.getReasonCodes()).contains("ORCHESTRATOR_OVERALL_TIMEOUT");
                });
    }

    @Test
    void review_deadlineExhaustedAfterFirstProviderSkipsLaterProvidersWithoutCallingThem() {
        AiOrchestratorProperties properties = properties(true);
        properties.setRequestTimeoutMs(5000);
        properties.setOverallTimeoutMs(1000);
        RecordingLogService logs = new RecordingLogService();
        MutableTimeSource clock = new MutableTimeSource();
        TimeoutAwareClient gemini = timeoutAwareSuccessClient(AiProviderName.GEMINI,
                AiProviderRole.GEMINI_CONSISTENCY_REVIEW, properties.getGemini(), logs, clock, 1000);
        TimeoutAwareClient openai = timeoutAwareSuccessClient(AiProviderName.OPENAI,
                AiProviderRole.GPT_RULE_REVIEW, properties.getOpenai(), logs, clock, 0);
        TimeoutAwareClient xai = timeoutAwareSuccessClient(AiProviderName.XAI,
                AiProviderRole.GROK_ADVERSARIAL_CHALLENGE, properties.getXai(), logs, clock, 0);
        AiDecisionOrchestratorServiceImpl service = service(properties, logs, List.of(openai, gemini, xai), clock);

        AiOrchestratorResult result = service.review(request());

        assertThat(gemini.timeouts).containsExactly(1000L);
        assertThat(openai.timeouts).isEmpty();
        assertThat(xai.timeouts).isEmpty();
        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.PARTIAL_FALLBACK);
        assertThat(result.getProviderResults()).filteredOn(providerResult ->
                providerResult.getCallStatus() == AiProviderCallStatus.TIMEOUT)
                .hasSize(2);
    }

    @Test
    void review_allProvidersSkippedWhenOverallTimeoutIsNonPositiveReturnsRuleOnlyFallback() {
        AiOrchestratorProperties properties = properties(true);
        properties.setOverallTimeoutMs(0);
        RecordingLogService logs = new RecordingLogService();
        MutableTimeSource clock = new MutableTimeSource();
        TimeoutAwareClient openai = timeoutAwareSuccessClient(AiProviderName.OPENAI,
                AiProviderRole.GPT_RULE_REVIEW, properties.getOpenai(), logs, clock, 0);
        TimeoutAwareClient gemini = timeoutAwareSuccessClient(AiProviderName.GEMINI,
                AiProviderRole.GEMINI_CONSISTENCY_REVIEW, properties.getGemini(), logs, clock, 0);
        AiDecisionOrchestratorServiceImpl service = service(properties, logs, List.of(openai, gemini), clock);

        AiOrchestratorResult result = service.review(request());

        assertThat(openai.timeouts).isEmpty();
        assertThat(gemini.timeouts).isEmpty();
        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        assertThat(result.getProviderResults()).allSatisfy(providerResult -> {
            assertThat(providerResult.getCallStatus()).isEqualTo(AiProviderCallStatus.TIMEOUT);
            assertThat(providerResult.getReasonCodes()).contains("ORCHESTRATOR_OVERALL_TIMEOUT");
        });
    }

    @Test
    void review_providerTimeoutDoesNotStopLaterProviderWhenOverallDeadlineRemains() {
        AiOrchestratorProperties properties = properties(true);
        properties.setRequestTimeoutMs(5000);
        properties.setOverallTimeoutMs(8000);
        RecordingLogService logs = new RecordingLogService();
        MutableTimeSource clock = new MutableTimeSource();
        AiProviderReviewResult timeout = AiProviderReviewResult.skipped(
                AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                AiProviderCallStatus.TIMEOUT, "PROVIDER_TIMEOUT");
        TimeoutAwareClient gemini = new TimeoutAwareClient(AiProviderName.GEMINI,
                AiProviderRole.GEMINI_CONSISTENCY_REVIEW, properties.getGemini(), logs, timeout, clock, 3000);
        TimeoutAwareClient openai = timeoutAwareSuccessClient(AiProviderName.OPENAI,
                AiProviderRole.GPT_RULE_REVIEW, properties.getOpenai(), logs, clock, 0);
        AiDecisionOrchestratorServiceImpl service = service(properties, logs, List.of(openai, gemini), clock);

        AiOrchestratorResult result = service.review(request());

        assertThat(gemini.timeouts).containsExactly(5000L);
        assertThat(openai.timeouts).containsExactly(5000L);
        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.PARTIAL_FALLBACK);
        assertThat(result.getSuccessfulProviderCount()).isEqualTo(1);
    }

    private static AiDecisionOrchestratorServiceImpl service(AiOrchestratorProperties properties,
                                                            RecordingLogService logs,
                                                            List<AiProviderClient> clients) {
        return new AiDecisionOrchestratorServiceImpl(clients, new AiUsageGuard(properties, logs), logs, properties);
    }

    private static AiDecisionOrchestratorServiceImpl service(AiOrchestratorProperties properties,
                                                            RecordingLogService logs,
                                                            List<AiProviderClient> clients,
                                                            AiDecisionOrchestratorServiceImpl.TimeSource timeSource) {
        return new AiDecisionOrchestratorServiceImpl(clients, new AiUsageGuard(properties, logs),
                logs, properties, timeSource);
    }

    private static AiProviderClient successClient(AiProviderName provider, AiProviderRole role,
                                                  AiProviderProperties properties, RecordingLogService logs,
                                                  AiReviewStance stance) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setStance(stance);
        result.setConflictLevel(AiReviewConflictLevel.NONE);
        result.setReasonCodes(List.of("OK"));
        return new FakeClient(provider, role, properties, logs, result);
    }

    private static AiProviderClient challengeClient(AiProviderName provider, AiProviderRole role,
                                                    AiProviderProperties properties, RecordingLogService logs) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setStance(AiReviewStance.CHALLENGE);
        result.setConflictLevel(AiReviewConflictLevel.MAJOR);
        result.setReasonCodes(List.of("CONFLICT"));
        return new FakeClient(provider, role, properties, logs, result);
    }

    private static AiProviderClient failureClient(AiProviderName provider, AiProviderRole role,
                                                  AiProviderProperties properties, RecordingLogService logs) {
        AiProviderReviewResult result = AiProviderReviewResult.skipped(provider, role, AiProviderCallStatus.FAILED, "FAIL");
        return new FakeClient(provider, role, properties, logs, result);
    }

    private static TimeoutAwareClient timeoutAwareSuccessClient(AiProviderName provider, AiProviderRole role,
                                                               AiProviderProperties properties, RecordingLogService logs,
                                                               MutableTimeSource clock, long advanceMs) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setStance(AiReviewStance.SUPPORT);
        result.setConflictLevel(AiReviewConflictLevel.NONE);
        result.setReasonCodes(List.of("OK"));
        return new TimeoutAwareClient(provider, role, properties, logs, result, clock, advanceMs);
    }

    private static AiOrchestratorProperties properties(boolean enabled) {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(enabled);
        properties.setDailyBudgetUsd(new BigDecimal("5.00"));
        properties.setPerAnalysisBudgetUsd(new BigDecimal("1.00"));
        configure(properties.getOpenai());
        configure(properties.getGemini());
        configure(properties.getXai());
        return properties;
    }

    private static void configure(AiProviderProperties properties) {
        properties.setEnabled(true);
        properties.setApiKey("key");
        properties.setModel("model");
        properties.setBaseUrl("https://ai.test");
        properties.setRequestsPerMinute(10);
        properties.setInputCostPerMillionUsd(new BigDecimal("1.00"));
        properties.setOutputCostPerMillionUsd(new BigDecimal("2.00"));
    }

    private static AiProviderRequest request() {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("analysis-1");
        request.setTraceId("trace-1");
        request.setRuleMarketBias("BULLISH");
        return request;
    }

    private record FakeClient(AiProviderName provider, AiProviderRole role, AiProviderProperties providerProperties,
                              RecordingLogService logs, AiProviderReviewResult reviewResult) implements AiProviderClient {
        @Override public AiProviderReadiness readiness() {
            return new AiProviderReadiness(provider, role, providerProperties.isEnabled(),
                    providerProperties.hasKeyAndModel(), providerProperties.isEnabled() && providerProperties.hasKeyAndModel(),
                    providerProperties.getModel(), List.of());
        }
        @Override public AiProviderReviewResult review(AiProviderRequest request) {
            logs.events.add("review:" + provider.name());
            assertThat(logs.events).contains("start:" + provider.name());
            return reviewResult;
        }
    }

    private static final class RecordingLogService implements AiCallLogService {
        final List<String> events = new ArrayList<>();

        @Override public AiCallLogDO startCall(AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd) {
            events.add("start:" + client.provider().name());
            AiCallLogDO log = new AiCallLogDO();
            log.setCallId(client.provider().name());
            return log;
        }
        @Override public void completeCall(AiCallLogDO log, AiProviderReviewResult result) { events.add("complete:" + result.getProvider().name()); }
        @Override public AiCallLogDO recordSkipped(AiProviderRequest request, AiProviderClient client, AiProviderReviewResult result, BigDecimal reservedCostUsd) {
            events.add("skipped:" + client.provider().name());
            return null;
        }
        @Override public List<AiCallLogDO> query(String analysisId, String traceId, String providerName, String callStatus, LocalDateTime from, LocalDateTime to, int limit) { return List.of(); }
        @Override public int countProviderAttemptsSince(String providerName, LocalDateTime since) { return 0; }
        @Override public BigDecimal sumChargeableCostSince(LocalDateTime since) { return BigDecimal.ZERO; }
        @Override public BigDecimal sumChargeableCostByAnalysisId(String analysisId) { return BigDecimal.ZERO; }
    }

    private static final class TimeoutAwareClient implements AiProviderClient {
        private final AiProviderName provider;
        private final AiProviderRole role;
        private final AiProviderProperties providerProperties;
        private final RecordingLogService logs;
        private final AiProviderReviewResult reviewResult;
        private final MutableTimeSource clock;
        private final long advanceMs;
        private final List<Long> timeouts = new ArrayList<>();

        private TimeoutAwareClient(AiProviderName provider, AiProviderRole role,
                                   AiProviderProperties providerProperties,
                                   RecordingLogService logs,
                                   AiProviderReviewResult reviewResult,
                                   MutableTimeSource clock,
                                   long advanceMs) {
            this.provider = provider;
            this.role = role;
            this.providerProperties = providerProperties;
            this.logs = logs;
            this.reviewResult = reviewResult;
            this.clock = clock;
            this.advanceMs = advanceMs;
        }

        @Override public AiProviderName provider() { return provider; }
        @Override public AiProviderRole role() { return role; }
        @Override public AiProviderReadiness readiness() {
            return new AiProviderReadiness(provider, role, providerProperties.isEnabled(),
                    providerProperties.hasKeyAndModel(), providerProperties.isEnabled() && providerProperties.hasKeyAndModel(),
                    providerProperties.getModel(), List.of());
        }
        @Override public AiProviderReviewResult review(AiProviderRequest request) {
            return review(request, 0);
        }
        @Override public AiProviderReviewResult review(AiProviderRequest request, long timeoutOverrideMs) {
            logs.events.add("review:" + provider.name());
            assertThat(logs.events).contains("start:" + provider.name());
            timeouts.add(timeoutOverrideMs);
            clock.advanceMs(advanceMs);
            return reviewResult;
        }
        @Override public AiProviderProperties providerProperties() { return providerProperties; }
    }

    private static final class MutableTimeSource implements AiDecisionOrchestratorServiceImpl.TimeSource {
        private long nanos;

        @Override
        public long nanoTime() {
            return nanos;
        }

        private void advanceMs(long millis) {
            nanos += TimeUnit.MILLISECONDS.toNanos(millis);
        }
    }
}
