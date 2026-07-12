package org.example.trademodel.ai;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.service.AiDecisionOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiParallelOrchestratorControlledSmokeTest {
    private final AiParallelOrchestratorControlledSmoke smoke =
            new AiParallelOrchestratorControlledSmoke();

    @Test
    void controlledLiveSmokeEntryPoint() {
        Map<String, String> environment = System.getenv();
        if (smoke.gateStatus(environment) != null) {
            smoke.run(environment, null, zeroAudit()).sanitizedOutputLines().forEach(System.out::println);
            return;
        }

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                TradeModelApplication.class, ControlledTransportConfiguration.class)
                .web(WebApplicationType.NONE)
                .properties("spring.main.banner-mode=off")
                .run()) {
            AiDecisionOrchestratorService orchestrator =
                    context.getBean(AiDecisionOrchestratorService.class);
            ControlledCountingAiHttpTransport transport =
                    context.getBean(ControlledCountingAiHttpTransport.class);
            AiParallelOrchestratorControlledSmoke.SmokeResult result =
                    smoke.run(environment, orchestrator, transport::snapshot);
            result.sanitizedOutputLines().forEach(System.out::println);
            assertThat(result.liveProviderCalls()).isIn("0", "1", "2", "3", "UNKNOWN_MAX_3");
            assertThat(result.finalResultOrder()).isEqualTo(
                    AiParallelOrchestratorControlledSmoke.FINAL_RESULT_ORDER);
        }
    }

    @Test
    void defaultGateMakesZeroExternalCalls() {
        CountingService service = new CountingService(successResult());

        AiParallelOrchestratorControlledSmoke.SmokeResult result =
                smoke.run(Map.of(), service, zeroAudit());

        assertThat(result.status()).isEqualTo("SKIPPED_EXTERNAL_CALLS_DISABLED");
        assertThat(result.liveProviderCalls()).isEqualTo("0");
        assertThat(result.realKeysRead()).isZero();
        assertThat(service.calls).isZero();
    }

    @Test
    void missingAnyKeyMakesZeroExternalCalls() {
        CountingService service = new CountingService(successResult());
        Map<String, String> environment = enabledEnvironment();
        environment.remove("GEMINI_API_KEY");

        AiParallelOrchestratorControlledSmoke.SmokeResult result =
                smoke.run(environment, service, zeroAudit());

        assertThat(result.status()).isEqualTo("SKIPPED_MISSING_API_KEY");
        assertThat(result.liveProviderCalls()).isEqualTo("0");
        assertThat(service.calls).isZero();
    }

    @Test
    void missingConfirmationGateMakesZeroExternalCalls() {
        CountingService service = new CountingService(successResult());
        Map<String, String> environment = enabledEnvironment();
        environment.remove(AiParallelOrchestratorControlledSmoke.HARNESS_ENTRY);

        AiParallelOrchestratorControlledSmoke.SmokeResult result =
                smoke.run(environment, service, zeroAudit());

        assertThat(result.status()).isEqualTo("SKIPPED_HARNESS_ENTRY_MISSING");
        assertThat(result.liveProviderCalls()).isEqualTo("0");
        assertThat(service.calls).isZero();
    }

    @Test
    void successfulRunUsesFormalServiceAndDeterministicOrder() {
        CountingService service = new CountingService(successResult());
        AiParallelOrchestratorControlledSmoke.SmokeResult result = smoke.run(
                enabledEnvironment(), service,
                () -> Map.of(AiProviderName.OPENAI, "1", AiProviderName.GEMINI, "1", AiProviderName.XAI, "1"));

        assertThat(service.calls).isOne();
        assertThat(result.status()).isEqualTo("PASS");
        assertThat(result.orchestrationMode()).isEqualTo("AI_ASSISTED");
        assertThat(result.finalResultOrder()).isEqualTo(
                AiParallelOrchestratorControlledSmoke.FINAL_RESULT_ORDER);
        assertThat(result.liveProviderCalls()).isEqualTo("3");
    }

    @Test
    void partialAndRuleOnlyModesRemainVisible() {
        AiOrchestratorResult partial = successResult();
        partial.setOrchestrationMode(AiOrchestrationMode.PARTIAL_FALLBACK);
        partial.setPartialFallbackUsed(true);
        partial.setProviderSuccessCount(2);
        partial.setProviderTimeoutCount(1);
        partial.getProviderResults().get(1).setCallStatus(AiProviderCallStatus.TIMEOUT);

        AiOrchestratorResult failed = successResult();
        failed.setOrchestrationMode(AiOrchestrationMode.RULE_ONLY_FALLBACK);
        failed.setProviderSuccessCount(0);
        failed.setProviderFailedCount(3);
        failed.getProviderResults().forEach(result -> result.setCallStatus(AiProviderCallStatus.FAILED));

        assertThat(smoke.run(enabledEnvironment(), new CountingService(partial), zeroAudit()).status())
                .isEqualTo("PASS_PARTIAL_FALLBACK");
        assertThat(smoke.run(enabledEnvironment(), new CountingService(failed), zeroAudit()).status())
                .isEqualTo("PASS_RULE_ONLY_FALLBACK");
    }

    @Test
    void fixedFixtureIsReviewOnlyAndContainsNoExecutionBoundary() {
        AiProviderRequest request = AiParallelOrchestratorControlledSmoke.fixedReviewRequest();

        assertThat(request.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(request.getTimeframe()).isEqualTo("15m");
        assertThat(request.getRuleMarketBias()).isEqualTo("BULLISH");
        assertThat(request.getRuleWorthOpening()).isFalse();
        assertThat(request.getEvidenceSummary()).isNotBlank();
        assertThat(request.getDecisionFacts()).containsEntry("reviewOnly", true)
                .containsEntry("manualReviewOnly", true)
                .containsEntry("notTradeInstruction", true)
                .containsEntry("notExecutable", true)
                .containsEntry("ruleDirectionPreserved", true);
    }

    @Test
    void sanitizedOutputDoesNotExposeSecretsOrProviderContent() {
        String secret = "secret-provider-value";
        Map<String, String> environment = enabledEnvironment();
        environment.put("OPENAI_API_KEY", secret);
        String output = String.join("\n", smoke.run(
                environment, new CountingService(successResult()), zeroAudit()).sanitizedOutputLines());

        assertThat(output).doesNotContain(secret, "Authorization", "Prompt", "raw response",
                        "Interaction ID")
                .contains("PRODUCTION_READINESS: BLOCKED")
                .contains("FINAL_RESULT_ORDER: "
                        + AiParallelOrchestratorControlledSmoke.FINAL_RESULT_ORDER);
    }

    @Test
    void countingTransportRefusesSecondCallAndPersistsBoundedCounts() throws Exception {
        Path marker = Files.createTempFile("ai-parallel-count", ".txt");
        try {
            ControlledCountingAiHttpTransport transport =
                    new ControlledCountingAiHttpTransport(request ->
                            new AiHttpResponse(200, "{}", Map.of()), marker);
            AiHttpRequest request = request("https://api.openai.com/v1/responses");

            transport.post(request);

            assertThat(transport.snapshot()).containsEntry(AiProviderName.OPENAI, "1")
                    .containsEntry(AiProviderName.GEMINI, "0")
                    .containsEntry(AiProviderName.XAI, "0");
            assertThatThrownBy(() -> transport.post(request))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("CALL_LIMIT_EXCEEDED");
            assertThat(Files.readString(marker)).contains("OPENAI=1", "GEMINI=0", "XAI=0");
        } finally {
            Files.deleteIfExists(marker);
        }
    }

    private static Map<String, String> enabledEnvironment() {
        Map<String, String> environment = new java.util.HashMap<>();
        environment.put(AiParallelOrchestratorControlledSmoke.ENABLE_EXTERNAL_CALLS, "true");
        environment.put(AiParallelOrchestratorControlledSmoke.HARNESS_ENTRY,
                AiParallelOrchestratorControlledSmoke.HARNESS_CONFIRMATION);
        environment.put("TRADE_MODEL_AI_ENABLED", "true");
        environment.put("TRADE_MODEL_AI_OPENAI_ENABLED", "true");
        environment.put("TRADE_MODEL_AI_GEMINI_ENABLED", "true");
        environment.put("TRADE_MODEL_AI_XAI_ENABLED", "true");
        environment.put("OPENAI_API_KEY", "test-openai-key");
        environment.put("GEMINI_API_KEY", "test-gemini-key");
        environment.put("XAI_API_KEY", "test-xai-key");
        return environment;
    }

    private static AiParallelOrchestratorControlledSmoke.CallCountAudit zeroAudit() {
        return () -> Map.of(AiProviderName.OPENAI, "0", AiProviderName.GEMINI, "0", AiProviderName.XAI, "0");
    }

    private static AiOrchestratorResult successResult() {
        AiOrchestratorResult result = new AiOrchestratorResult();
        result.setOrchestrationMode(AiOrchestrationMode.AI_ASSISTED);
        result.setProviderResults(List.of(
                success(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW),
                success(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW),
                success(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE)));
        result.setProviderSubmittedCount(3);
        result.setProviderCompletedCount(3);
        result.setProviderSuccessCount(3);
        result.setOrchestrationLatencyMs(25);
        return result;
    }

    private static AiProviderReviewResult success(AiProviderName provider, AiProviderRole role) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setLatencyMs(10L);
        return result;
    }

    private static AiHttpRequest request(String url) {
        AiHttpRequest request = new AiHttpRequest();
        request.setUrl(url);
        request.setBody("{}");
        request.setTimeout(java.time.Duration.ofSeconds(1));
        return request;
    }

    private static final class CountingService implements AiDecisionOrchestratorService {
        private final AiOrchestratorResult result;
        private int calls;

        private CountingService(AiOrchestratorResult result) {
            this.result = result;
        }

        @Override
        public AiOrchestratorResult review(AiProviderRequest request) {
            calls++;
            return result;
        }

        @Override
        public List<AiProviderReadiness> providerReadiness() {
            return List.of();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ControlledTransportConfiguration {
        @Bean
        @Primary
        ControlledCountingAiHttpTransport controlledCountingAiHttpTransport() {
            String marker = System.getenv("AI_PARALLEL_SMOKE_CALL_COUNT_FILE");
            if (marker == null || marker.isBlank()) {
                throw new IllegalStateException("AI_PARALLEL_SMOKE_CALL_COUNT_FILE_REQUIRED");
            }
            return new ControlledCountingAiHttpTransport(new JdkAiHttpTransport(), Path.of(marker));
        }
    }

    static final class ControlledCountingAiHttpTransport implements AiHttpTransport {
        private final AiHttpTransport delegate;
        private final Path marker;
        private final Map<AiProviderName, Integer> counts = new EnumMap<>(AiProviderName.class);

        ControlledCountingAiHttpTransport(AiHttpTransport delegate, Path marker) {
            this.delegate = delegate;
            this.marker = marker;
            counts.put(AiProviderName.OPENAI, 0);
            counts.put(AiProviderName.GEMINI, 0);
            counts.put(AiProviderName.XAI, 0);
            persist();
        }

        @Override
        public AiHttpResponse post(AiHttpRequest request) throws IOException, InterruptedException {
            AiProviderName provider = providerFor(request == null ? null : request.getUrl());
            markAttempt(provider);
            return delegate.post(request);
        }

        synchronized Map<AiProviderName, String> snapshot() {
            Map<AiProviderName, String> result = new EnumMap<>(AiProviderName.class);
            counts.forEach((provider, count) -> result.put(provider, Integer.toString(count)));
            return result;
        }

        private synchronized void markAttempt(AiProviderName provider) throws IOException {
            int current = counts.getOrDefault(provider, 0);
            if (current >= 1) {
                throw new IOException("CONTROLLED_SMOKE_CALL_LIMIT_EXCEEDED");
            }
            counts.put(provider, 1);
            persist();
        }

        private synchronized void persist() {
            try {
                String content = "OPENAI=" + counts.get(AiProviderName.OPENAI) + "\n"
                        + "GEMINI=" + counts.get(AiProviderName.GEMINI) + "\n"
                        + "XAI=" + counts.get(AiProviderName.XAI) + "\n";
                Files.writeString(marker, content, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            } catch (IOException exception) {
                throw new IllegalStateException("CONTROLLED_SMOKE_CALL_AUDIT_UNAVAILABLE", exception);
            }
        }

        private static AiProviderName providerFor(String url) throws IOException {
            String value = url == null ? "" : url.toLowerCase(java.util.Locale.ROOT);
            if (value.contains("api.openai.com")) {
                return AiProviderName.OPENAI;
            }
            if (value.contains("generativelanguage.googleapis.com")) {
                return AiProviderName.GEMINI;
            }
            if (value.contains("api.x.ai")) {
                return AiProviderName.XAI;
            }
            throw new IOException("CONTROLLED_SMOKE_UNKNOWN_PROVIDER_ENDPOINT");
        }
    }
}
