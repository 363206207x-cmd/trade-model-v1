package org.example.trademodel.service.impl;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiOrchestratorResult;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderExecutor;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class AiDecisionOrchestratorServiceImplTest {
    private final List<AiProviderExecutor> executors = new CopyOnWriteArrayList<>();

    @AfterEach
    void shutdownExecutors() {
        executors.forEach(AiProviderExecutor::shutdown);
    }

    @Test
    void allSuccessProducesAiAssistedAndDeterministicResultOrder() {
        AiOrchestratorProperties properties = properties(true);
        RecordingLogService logs = new RecordingLogService();
        FakeClient xai = successClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                properties.getXai(), AiReviewStance.ABSTAIN);
        FakeClient gemini = successClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                properties.getGemini(), AiReviewStance.SUPPORT);
        FakeClient openai = successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                properties.getOpenai(), AiReviewStance.SUPPORT);

        AiOrchestratorResult result = service(properties, logs, List.of(xai, gemini, openai)).review(request());

        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.AI_ASSISTED);
        assertThat(result.getProviderResults()).extracting(AiProviderReviewResult::getRole)
                .containsExactly(
                        AiProviderRole.GPT_RULE_REVIEW,
                        AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        AiProviderRole.GROK_ADVERSARIAL_CHALLENGE);
        assertThat(result.getSuccessfulProviderCount()).isEqualTo(3);
        assertThat(result.getProviderSubmittedCount()).isEqualTo(3);
        assertThat(result.getProviderSuccessCount()).isEqualTo(3);
        assertThat(result.isPartialFallbackUsed()).isFalse();
        assertThat(result.isRuleDirectionPreserved()).isTrue();
    }

    @Test
    void threeProvidersAreSubmittedInParallel() throws Exception {
        AiOrchestratorProperties properties = properties(true);
        RecordingLogService logs = new RecordingLogService();
        CountDownLatch entered = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        List<AiProviderClient> clients = List.of(
                blockingSuccessClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        properties.getOpenai(), entered, release, false, null),
                blockingSuccessClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        properties.getGemini(), entered, release, false, null),
                blockingSuccessClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                        properties.getXai(), entered, release, false, null));
        AiDecisionOrchestratorServiceImpl orchestrator = service(properties, logs, clients);
        FutureTask<AiOrchestratorResult> reviewTask = new FutureTask<>(() -> orchestrator.review(request()));
        Thread caller = new Thread(reviewTask, "test-orchestrator-caller");

        caller.start();
        assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
        release.countDown();
        AiOrchestratorResult result = reviewTask.get(2, TimeUnit.SECONDS);

        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.AI_ASSISTED);
        assertThat(logs.startedCount()).isEqualTo(3);
        assertThat(logs.completedCount()).isEqualTo(3);
    }

    @Test
    void geminiSlowDoesNotDiscardOpenAiAndXaiCompletion() {
        AiOrchestratorProperties properties = properties(true);
        properties.getProviderTimeouts().setGeminiMs(1_000);
        RecordingLogService logs = new RecordingLogService();
        MutableTimeSource clock = new MutableTimeSource();
        CountDownLatch geminiEntered = new CountDownLatch(1);
        CountDownLatch releaseGemini = new CountDownLatch(1);
        FakeClient gemini = blockingSuccessClient(
                AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                properties.getGemini(), geminiEntered, releaseGemini, false, null);
        List<AiProviderClient> clients = List.of(
                successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        properties.getOpenai(), AiReviewStance.SUPPORT),
                gemini,
                successClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                        properties.getXai(), AiReviewStance.ABSTAIN));

        AiOrchestratorResult result = controlledService(
                properties, logs, clients, clock,
                new AdvancingWaiter(clock, geminiEntered)).review(request());
        releaseGemini.countDown();

        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.PARTIAL_FALLBACK);
        assertThat(result.getSuccessfulProviderCount()).isEqualTo(2);
        assertThat(result.getProviderTimeoutCount()).isEqualTo(1);
        assertThat(result.isGlobalDeadlineExceeded()).isFalse();
        assertThat(result.getProviderResults()).filteredOn(item -> item.getProvider() == AiProviderName.GEMINI)
                .singleElement().satisfies(item -> {
                    assertThat(item.getCallStatus()).isEqualTo(AiProviderCallStatus.TIMEOUT);
                    assertThat(item.getErrorCode()).isEqualTo("PROVIDER_TIMEOUT");
                });
    }

    @Test
    void providerSpecificTimeoutIsAppliedAndEachProviderCalledAtMostOnce() {
        AiOrchestratorProperties properties = properties(true);
        RecordingLogService logs = new RecordingLogService();
        FakeClient openai = successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                properties.getOpenai(), AiReviewStance.SUPPORT);
        FakeClient gemini = successClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                properties.getGemini(), AiReviewStance.SUPPORT);
        FakeClient xai = successClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                properties.getXai(), AiReviewStance.ABSTAIN);

        service(properties, logs, List.of(openai, gemini, xai)).review(request());

        assertThat(openai.timeouts).containsExactly(10_000L);
        assertThat(gemini.timeouts).containsExactly(25_000L);
        assertThat(xai.timeouts).containsExactly(10_000L);
        assertThat(List.of(openai.calls.get(), gemini.calls.get(), xai.calls.get()))
                .containsExactly(1, 1, 1);
    }

    @Test
    void globalDeadlineCancelsRemainingTasksAndProducesRuleOnlyFallback() {
        AiOrchestratorProperties properties = properties(true);
        properties.getProviderTimeouts().setOpenaiMs(5_000);
        properties.getProviderTimeouts().setGeminiMs(5_000);
        properties.getProviderTimeouts().setXaiMs(5_000);
        properties.getProviderTimeouts().setOverallMs(5_000);
        RecordingLogService logs = new RecordingLogService();
        MutableTimeSource clock = new MutableTimeSource();
        CountDownLatch entered = new CountDownLatch(3);
        CountDownLatch release = new CountDownLatch(1);
        List<AiProviderClient> clients = List.of(
                blockingSuccessClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        properties.getOpenai(), entered, release, false, null),
                blockingSuccessClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        properties.getGemini(), entered, release, false, null),
                blockingSuccessClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                        properties.getXai(), entered, release, false, null));

        AiOrchestratorResult result = controlledService(
                properties, logs, clients, clock, new AdvancingWaiter(clock, entered)).review(request());
        release.countDown();

        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        assertThat(result.isGlobalDeadlineExceeded()).isTrue();
        assertThat(result.getProviderTimeoutCount()).isEqualTo(3);
        assertThat(result.getProviderResults()).allSatisfy(item -> {
            assertThat(item.getCallStatus()).isEqualTo(AiProviderCallStatus.TIMEOUT);
            assertThat(item.getErrorCode()).isEqualTo("ORCHESTRATOR_OVERALL_TIMEOUT");
        });
    }

    @Test
    void lateProviderResultDoesNotOverwriteTimeoutOrCompleteLogTwice() throws Exception {
        AiOrchestratorProperties properties = properties(true);
        properties.getProviderTimeouts().setGeminiMs(1_000);
        RecordingLogService logs = new RecordingLogService();
        MutableTimeSource clock = new MutableTimeSource();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(1);
        FakeClient gemini = blockingSuccessClient(
                AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                properties.getGemini(), entered, release, true, finished);

        AiOrchestratorResult result = controlledService(
                properties, logs, List.of(gemini), clock,
                new AdvancingWaiter(clock, entered)).review(request());
        release.countDown();
        assertThat(finished.await(2, TimeUnit.SECONDS)).isTrue();

        assertThat(result.getProviderResults()).singleElement().satisfies(item -> {
            assertThat(item.getCallStatus()).isEqualTo(AiProviderCallStatus.TIMEOUT);
            assertThat(item.getErrorCode()).isEqualTo("PROVIDER_TIMEOUT");
        });
        assertThat(logs.completeCount(AiProviderName.GEMINI)).isEqualTo(1);
        assertThat(gemini.calls.get()).isEqualTo(1);
    }

    @Test
    void partialFailureAndAllFailureUseExpectedFallbackModes() {
        AiOrchestratorProperties properties = properties(true);
        RecordingLogService logs = new RecordingLogService();
        FakeClient openai = successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                properties.getOpenai(), AiReviewStance.SUPPORT);
        FakeClient gemini = failureClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                properties.getGemini());
        FakeClient xai = failureClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                properties.getXai());

        AiOrchestratorResult partial = service(properties, logs, List.of(openai, gemini, xai)).review(request());
        AiOrchestratorResult failed = service(properties, new RecordingLogService(), List.of(
                failureClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW, properties.getOpenai()),
                failureClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        properties.getGemini()),
                failureClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                        properties.getXai()))).review(request());

        assertThat(partial.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.PARTIAL_FALLBACK);
        assertThat(partial.isPartialFallbackUsed()).isTrue();
        assertThat(failed.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        assertThat(failed.getSuccessfulProviderCount()).isZero();
    }

    @Test
    void usageGuardFailureDoesNotSubmitOrCallProvider() {
        AiOrchestratorProperties properties = properties(true);
        properties.getGemini().setRequestsPerMinute(0);
        RecordingLogService logs = new RecordingLogService();
        FakeClient gemini = successClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                properties.getGemini(), AiReviewStance.SUPPORT);

        AiOrchestratorResult result = service(properties, logs, List.of(gemini)).review(request());

        assertThat(gemini.calls.get()).isZero();
        assertThat(result.getProviderSubmittedCount()).isZero();
        assertThat(logs.startedCount()).isZero();
        assertThat(logs.skippedCount()).isEqualTo(1);
    }

    @Test
    void disabledOrchestratorMakesZeroCalls() {
        AiOrchestratorProperties properties = properties(false);
        RecordingLogService logs = new RecordingLogService();
        FakeClient openai = successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                properties.getOpenai(), AiReviewStance.SUPPORT);

        AiOrchestratorResult result = service(properties, logs, List.of(openai)).review(request());

        assertThat(openai.calls.get()).isZero();
        assertThat(result.getOrchestrationMode()).isEqualTo(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        assertThat(result.getProviderSubmittedCount()).isZero();
    }

    @Test
    void invalidTimeoutConfigurationFailsClosed() {
        AiOrchestratorProperties properties = properties(true);
        properties.getProviderTimeouts().setOpenaiMs(999);
        RecordingLogService logs = new RecordingLogService();
        FakeClient openai = successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                properties.getOpenai(), AiReviewStance.SUPPORT);

        AiOrchestratorResult result = service(properties, logs, List.of(openai)).review(request());

        assertThat(openai.calls.get()).isZero();
        assertThat(result.getProviderResults()).singleElement().satisfies(item -> {
            assertThat(item.getCallStatus()).isEqualTo(AiProviderCallStatus.FAILED);
            assertThat(item.getErrorCode()).isEqualTo("PROVIDER_TIMEOUT_CONFIG_INVALID");
        });
    }

    @Test
    void providerTimeoutConfigurationUsesRequiredDefaultsBoundsAndEnvironmentNames() throws Exception {
        AiOrchestratorProperties.ProviderTimeouts timeouts =
                new AiOrchestratorProperties.ProviderTimeouts();
        String application = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(timeouts.getOpenaiMs()).isEqualTo(10_000);
        assertThat(timeouts.getGeminiMs()).isEqualTo(25_000);
        assertThat(timeouts.getXaiMs()).isEqualTo(10_000);
        assertThat(timeouts.getOverallMs()).isEqualTo(30_000);
        assertThat(timeouts.validOverall()).isTrue();
        assertThat(timeouts.validProvider(AiProviderName.OPENAI)).isTrue();
        assertThat(timeouts.validProvider(AiProviderName.GEMINI)).isTrue();
        assertThat(timeouts.validProvider(AiProviderName.XAI)).isTrue();
        assertThat(application).contains(
                "openai-ms: ${TRADE_MODEL_AI_OPENAI_TIMEOUT_MS:10000}",
                "gemini-ms: ${TRADE_MODEL_AI_GEMINI_TIMEOUT_MS:25000}",
                "xai-ms: ${TRADE_MODEL_AI_XAI_TIMEOUT_MS:10000}",
                "overall-ms: ${TRADE_MODEL_AI_OVERALL_TIMEOUT_MS:30000}");

        timeouts.setGeminiMs(30_001);
        assertThat(timeouts.validProvider(AiProviderName.GEMINI)).isFalse();
        timeouts.setGeminiMs(25_000);
        timeouts.setOverallMs(4_999);
        assertThat(timeouts.validOverall()).isFalse();
    }

    @Test
    void challengeContributesConflictWithoutChangingRuleAuthority() {
        AiOrchestratorProperties properties = properties(true);
        RecordingLogService logs = new RecordingLogService();
        FakeClient xai = challengeClient(properties.getXai());

        AiOrchestratorResult result = service(properties, logs, List.of(xai)).review(request());

        assertThat(result.isGrokConsistentWithRule()).isFalse();
        assertThat(result.getConflictContribution()).isEqualTo(10);
        assertThat(result.isRuleDirectionPreserved()).isTrue();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isManualReviewOnly()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotUserPositionCreation()).isTrue();
        assertThat(result.isNotPositionMutation()).isTrue();
    }

    @Test
    void callLogsCloseExactlyOnceAndMetricsContainNoPayload() {
        AiOrchestratorProperties properties = properties(true);
        RecordingLogService logs = new RecordingLogService();
        List<AiProviderClient> clients = List.of(
                successClient(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                        properties.getOpenai(), AiReviewStance.SUPPORT),
                successClient(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                        properties.getGemini(), AiReviewStance.SUPPORT),
                failureClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                        properties.getXai()));

        AiOrchestratorResult result = service(properties, logs, clients).review(request());

        assertThat(logs.startedCount()).isEqualTo(3);
        assertThat(logs.completedCount()).isEqualTo(3);
        assertThat(logs.completeCounts.values()).allMatch(count -> count.get() == 1);
        assertThat(result.getOrchestrationStartedAt()).isNotNull();
        assertThat(result.getOrchestrationCompletedAt()).isNotNull();
        assertThat(result.getOrchestrationLatencyMs()).isGreaterThanOrEqualTo(0L);
        assertThat(result.getProviderSubmittedCount()).isEqualTo(3);
        assertThat(result.getProviderCompletedCount()).isEqualTo(3);
        assertThat(result.getProviderSuccessCount()).isEqualTo(2);
        assertThat(result.getProviderFailedCount()).isEqualTo(1);
    }

    @Test
    void executorQueueIsBoundedNamedAndExecutorShutsDownCleanly() throws Exception {
        AiProviderExecutor executor = executor();

        assertThat(executor.getMaximumPoolSize()).isEqualTo(3);
        assertThat(executor.getQueueCapacity()).isEqualTo(3);
        assertThat(executor.getQueueSize()).isZero();
        Future<String> threadName = executor.executorService().submit(
                executor.namedTask(AiProviderName.GEMINI, () -> Thread.currentThread().getName()));
        assertThat(threadName.get(1, TimeUnit.SECONDS)).isEqualTo("ai-provider-gemini");

        executor.shutdown();
        assertThat(executor.isShutdown()).isTrue();
    }

    @Test
    void noTradingOrderPositionPlanPushOrTelegramDependencyIsIntroduced() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/impl/AiDecisionOrchestratorServiceImpl.java"));
        String executorSource = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/ai/AiProviderExecutor.java"));

        assertThat(source + executorSource).doesNotContain(
                "UserPosition", "ExecutionPlanService", "OrderService", "PushService", "Telegram",
                "newCachedThreadPool", "ForkJoinPool.commonPool");
    }

    private AiDecisionOrchestratorServiceImpl service(
            AiOrchestratorProperties properties,
            RecordingLogService logs,
            List<AiProviderClient> clients) {
        return controlledService(properties, logs, clients,
                System::nanoTime,
                (completionService, waitNanos) -> completionService.poll(
                        Math.max(1L, waitNanos), TimeUnit.NANOSECONDS));
    }

    private AiDecisionOrchestratorServiceImpl controlledService(
            AiOrchestratorProperties properties,
            RecordingLogService logs,
            List<AiProviderClient> clients,
            AiDecisionOrchestratorServiceImpl.TimeSource clock,
            AiDecisionOrchestratorServiceImpl.CompletionWaiter waiter) {
        AiProviderExecutor executor = executor();
        return new AiDecisionOrchestratorServiceImpl(
                clients, new org.example.trademodel.ai.AiUsageGuard(properties, logs),
                logs, properties, executor, clock, waiter);
    }

    private AiProviderExecutor executor() {
        AiProviderExecutor executor = new AiProviderExecutor();
        executors.add(executor);
        return executor;
    }

    private static FakeClient successClient(AiProviderName provider, AiProviderRole role,
                                            AiProviderProperties properties,
                                            AiReviewStance stance) {
        return new FakeClient(provider, role, properties, timeout -> success(provider, role, stance));
    }

    private static FakeClient failureClient(AiProviderName provider, AiProviderRole role,
                                            AiProviderProperties properties) {
        return new FakeClient(provider, role, properties, timeout ->
                AiProviderReviewResult.skipped(provider, role, AiProviderCallStatus.FAILED, "FAIL"));
    }

    private static FakeClient challengeClient(AiProviderProperties properties) {
        return new FakeClient(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                properties, timeout -> {
            AiProviderReviewResult result = success(
                    AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE,
                    AiReviewStance.CHALLENGE);
            result.setConflictLevel(AiReviewConflictLevel.MAJOR);
            result.setReasonCodes(List.of("CONFLICT"));
            return result;
        });
    }

    private static FakeClient blockingSuccessClient(
            AiProviderName provider,
            AiProviderRole role,
            AiProviderProperties properties,
            CountDownLatch entered,
            CountDownLatch release,
            boolean ignoreInterrupt,
            CountDownLatch finished) {
        return new FakeClient(provider, role, properties, timeout -> {
            entered.countDown();
            boolean waiting = true;
            while (waiting) {
                try {
                    release.await();
                    waiting = false;
                } catch (InterruptedException exception) {
                    if (!ignoreInterrupt) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            if (finished != null) {
                finished.countDown();
            }
            return success(provider, role, AiReviewStance.SUPPORT);
        });
    }

    private static AiProviderReviewResult success(
            AiProviderName provider, AiProviderRole role, AiReviewStance stance) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setStance(stance);
        result.setConflictLevel(AiReviewConflictLevel.NONE);
        result.setReasonCodes(List.of("OK"));
        return result;
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
        properties.setApiKey("test-key");
        properties.setModel("test-model");
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

    @FunctionalInterface
    private interface ReviewBehavior {
        AiProviderReviewResult review(long timeoutMs);
    }

    private static final class FakeClient implements AiProviderClient {
        private final AiProviderName provider;
        private final AiProviderRole role;
        private final AiProviderProperties providerProperties;
        private final ReviewBehavior behavior;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<Long> timeouts = new CopyOnWriteArrayList<>();

        private FakeClient(AiProviderName provider, AiProviderRole role,
                           AiProviderProperties providerProperties,
                           ReviewBehavior behavior) {
            this.provider = provider;
            this.role = role;
            this.providerProperties = providerProperties;
            this.behavior = behavior;
        }

        @Override public AiProviderName provider() { return provider; }
        @Override public AiProviderRole role() { return role; }
        @Override public AiProviderReadiness readiness() {
            return new AiProviderReadiness(provider, role, providerProperties.isEnabled(),
                    providerProperties.hasKeyAndModel(), true,
                    providerProperties.getModel(), List.of());
        }
        @Override public AiProviderReviewResult review(AiProviderRequest request) {
            return review(request, 0L);
        }
        @Override public AiProviderReviewResult review(AiProviderRequest request, long timeoutOverrideMs) {
            calls.incrementAndGet();
            timeouts.add(timeoutOverrideMs);
            return behavior.review(timeoutOverrideMs);
        }
        @Override public AiProviderProperties providerProperties() { return providerProperties; }
    }

    private static final class RecordingLogService implements AiCallLogService {
        private final List<String> events = new CopyOnWriteArrayList<>();
        private final Map<AiProviderName, AtomicInteger> completeCounts = new ConcurrentHashMap<>();

        @Override public AiCallLogDO startCall(
                AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd) {
            events.add("start:" + client.provider().name());
            AiCallLogDO log = new AiCallLogDO();
            log.setCallId("call-" + client.provider().name());
            log.setProviderName(client.provider().name());
            log.setAnalysisId(request.getAnalysisId());
            log.setTraceId(request.getTraceId());
            return log;
        }

        @Override public void completeCall(AiCallLogDO log, AiProviderReviewResult result) {
            AiProviderName provider = AiProviderName.valueOf(log.getProviderName());
            completeCounts.computeIfAbsent(provider, ignored -> new AtomicInteger()).incrementAndGet();
            events.add("complete:" + provider.name() + ":" + result.getCallStatus().name());
        }

        @Override public AiCallLogDO recordSkipped(
                AiProviderRequest request, AiProviderClient client,
                AiProviderReviewResult result, BigDecimal reservedCostUsd) {
            events.add("skipped:" + client.provider().name());
            return null;
        }

        @Override public List<AiCallLogDO> query(
                String analysisId, String traceId, String providerName, String callStatus,
                LocalDateTime from, LocalDateTime to, int limit) {
            return List.of();
        }
        @Override public int countProviderAttemptsSince(String providerName, LocalDateTime since) { return 0; }
        @Override public BigDecimal sumChargeableCostSince(LocalDateTime since) { return BigDecimal.ZERO; }
        @Override public BigDecimal sumChargeableCostByAnalysisId(String analysisId) { return BigDecimal.ZERO; }

        private int startedCount() {
            return (int) events.stream().filter(event -> event.startsWith("start:")).count();
        }
        private int completedCount() {
            return completeCounts.values().stream().mapToInt(AtomicInteger::get).sum();
        }
        private int skippedCount() {
            return (int) events.stream().filter(event -> event.startsWith("skipped:")).count();
        }
        private int completeCount(AiProviderName provider) {
            AtomicInteger count = completeCounts.get(provider);
            return count == null ? 0 : count.get();
        }
    }

    private static final class MutableTimeSource implements AiDecisionOrchestratorServiceImpl.TimeSource {
        private final AtomicLong nanos = new AtomicLong();

        @Override public long nanoTime() { return nanos.get(); }
        private void advanceNanos(long amount) { nanos.addAndGet(Math.max(1L, amount)); }
    }

    private static final class AdvancingWaiter implements AiDecisionOrchestratorServiceImpl.CompletionWaiter {
        private final MutableTimeSource clock;
        private final CountDownLatch providerEntered;

        private AdvancingWaiter(MutableTimeSource clock, CountDownLatch providerEntered) {
            this.clock = clock;
            this.providerEntered = providerEntered;
        }

        @Override
        public Future<AiDecisionOrchestratorServiceImpl.ProviderCompletion> poll(
                CompletionService<AiDecisionOrchestratorServiceImpl.ProviderCompletion> completionService,
                long waitNanos) throws InterruptedException {
            Future<AiDecisionOrchestratorServiceImpl.ProviderCompletion> completed =
                    completionService.poll(100, TimeUnit.MILLISECONDS);
            if (completed != null) {
                return completed;
            }
            assertThat(providerEntered.await(1, TimeUnit.SECONDS)).isTrue();
            clock.advanceNanos(waitNanos);
            return completionService.poll();
        }
    }
}
