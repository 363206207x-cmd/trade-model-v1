package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderModelStrategyTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalCheckpointSelectsApprovedGpt56FastModel() {
        SequenceTransport transport = SequenceTransport.responding(openAiResponse());
        OpenAiProviderClient client = client(transport);

        assertThat(client.readiness().getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_CONFIGURED);
        assertThat(client.readiness().getReasonCodes()).containsExactly("MODEL_AVAILABILITY_UNVERIFIED");

        AiProviderReviewResult result = client.review(request(false), 5_000);

        assertThat(result.successful()).isTrue();
        assertThat(result.getModelStrategy()).isEqualTo("FAST_DECISION_MODEL");
        assertThat(result.getOriginalModel()).isEqualTo("gpt-5.6-luna");
        assertThat(result.getSelectedModel()).isEqualTo("gpt-5.6-luna");
        assertThat(result.getFallbackLevel()).isZero();
        assertThat(transport.models()).containsExactly("gpt-5.6-luna");
        AiProviderReadiness readiness = client.readiness();
        assertThat(readiness.getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_ACTIVE);
        assertThat(readiness.isReady()).isTrue();
        assertThat(readiness.getReasonCodes()).containsExactly("MODEL_CALL_VERIFIED");
        assertThat(readiness.getReasonCodes()).doesNotContain("MODEL_AVAILABILITY_UNVERIFIED");
    }

    @Test
    void conflictEscalationSelectsApprovedGpt56ReasoningModel() {
        SequenceTransport transport = SequenceTransport.responding(openAiResponse());
        OpenAiProviderClient client = client(transport);

        AiProviderReviewResult result = client.review(request(true), 5_000);

        assertThat(result.successful()).isTrue();
        assertThat(result.getModelStrategy()).isEqualTo("DEEP_REASONING_MODEL");
        assertThat(result.getOriginalModel()).isEqualTo("gpt-5.6-sol");
        assertThat(result.getSelectedModel()).isEqualTo("gpt-5.6-sol");
        assertThat(transport.models()).containsExactly("gpt-5.6-sol");
    }

    @Test
    void fastTimeoutFallsToGpt55WithExplicitAuditMetadata() {
        SequenceTransport transport = new SequenceTransport();
        transport.addFailure(new HttpTimeoutException("test timeout"));
        transport.addResponse(openAiResponse());

        OpenAiProviderClient client = client(transport);
        AiProviderReviewResult result = client.review(request(false), 5_000);

        assertThat(result.successful()).isTrue();
        assertThat(result.getSelectedModel()).isEqualTo("gpt-5.5");
        assertThat(result.getFallbackLevel()).isEqualTo(1);
        assertThat(result.getFallbackReason()).isEqualTo("OPENAI_FALLBACK_GPT55");
        assertThat(result.getReasonCodes())
                .contains("OPENAI_FAST_MODEL_TIMEOUT", "OPENAI_FALLBACK_GPT55");
        assertThat(result.getModelRoutingTimestamp()).isNotNull();
        assertThat(result.getModelRoutingTraceId()).isEqualTo("trace-model-routing");
        assertThat(transport.models()).containsExactly("gpt-5.6-luna", "gpt-5.5");
        AiProviderReadiness readiness = client.readiness();
        assertThat(readiness.getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_FALLBACK_ACTIVE);
        assertThat(readiness.isReady()).isFalse();
        assertThat(readiness.isFallbackUsed()).isTrue();
        assertThat(readiness.getFallbackReason()).isEqualTo("OPENAI_FALLBACK_GPT55");
        assertThat(readiness.getReasonCodes()).containsExactly("OPENAI_FALLBACK_GPT55");
        assertThat(readiness.getEffectiveModel()).isEqualTo("gpt-5.5");
    }

    @Test
    void gpt55UnavailableFallsToGpt54() {
        SequenceTransport transport = SequenceTransport.responding(
                modelNotFound(), modelNotFound(), openAiResponse());

        AiProviderReviewResult result = client(transport).review(request(false), 5_000);

        assertThat(result.successful()).isTrue();
        assertThat(result.getSelectedModel()).isEqualTo("gpt-5.4");
        assertThat(result.getOriginalModel()).isEqualTo("gpt-5.6-luna");
        assertThat(result.getFallbackLevel()).isEqualTo(2);
        assertThat(result.getFallbackReason()).isEqualTo("OPENAI_FALLBACK_GPT54");
        assertThat(result.getModelRoutingTimestamp()).isNotNull();
        assertThat(result.getModelRoutingTraceId()).isEqualTo("trace-model-routing");
        assertThat(result.getReasonCodes()).contains("OPENAI_FALLBACK_GPT55", "OPENAI_FALLBACK_GPT54");
        assertThat(transport.models()).containsExactly("gpt-5.6-luna", "gpt-5.5", "gpt-5.4");
    }

    @Test
    void noAcceptableModelReturnsModelUnavailable() {
        SequenceTransport transport = SequenceTransport.responding(
                modelNotFound(), modelNotFound(), modelNotFound());
        OpenAiProviderClient client = client(transport);

        AiProviderReviewResult result = client.review(request(true), 5_000);

        assertThat(result.successful()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("MODEL_UNAVAILABLE");
        assertThat(result.getFallbackReason()).isEqualTo("OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE");
        assertThat(result.getReasonCodes()).contains("OPENAI_REASONING_MODEL_UNAVAILABLE",
                "OPENAI_NO_ACCEPTABLE_MODEL_AVAILABLE");
        assertThat(client.readiness().getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_UNAVAILABLE);
        assertThat(client.readiness().isReady()).isFalse();
    }

    @Test
    void failedContractResponseDoesNotActivateConfiguredModel() {
        SequenceTransport transport = SequenceTransport.responding(new AiHttpResponse(401, "{}", Map.of()));
        OpenAiProviderClient client = client(transport);

        AiProviderReviewResult result = client.review(request(false), 5_000);

        assertThat(result.successful()).isFalse();
        assertThat(client.readiness().getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_CONFIGURED);
        assertThat(client.readiness().getReasonCodes()).containsExactly("MODEL_AVAILABILITY_UNVERIFIED");
    }

    @Test
    void modelPolicyRejectsEveryGpt4PathAndMalformedConfiguration() {
        assertThat(OpenAiModelRouter.isApprovedPrimary("gpt-5.6-luna")).isTrue();
        assertThat(OpenAiModelRouter.isApprovedPrimary("gpt-5.6-sol")).isTrue();
        assertThat(OpenAiModelRouter.isApprovedGpt55("gpt-5.5")).isTrue();
        assertThat(OpenAiModelRouter.isApprovedGpt54("gpt-5.4")).isTrue();
        assertThat(OpenAiModelRouter.isApprovedModel("gpt-4.1-mini")).isFalse();
        assertThat(OpenAiModelRouter.isApprovedModel("gpt-4.1")).isFalse();
        assertThat(OpenAiModelRouter.isApprovedModel("gpt-4o")).isFalse();
        assertThat(OpenAiModelRouter.isApprovedModel("gpt-5.3")).isFalse();

        AiOrchestratorProperties properties = properties();
        properties.getOpenai().getGptFinal().setFallbackModels(List.of("gpt-5.5", "gpt-4.1-mini"));
        OpenAiProviderClient client = new OpenAiProviderClient(properties,
                SequenceTransport.responding(openAiResponse()), objectMapper);
        assertThat(client.readiness().isConfigured()).isFalse();
        assertThat(client.readiness().getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_UNAVAILABLE);
    }

    @Test
    void providerDefaultsAndEndpointsMatchRoleStrategy() throws Exception {
        String application = Files.readString(Path.of("src/main/resources/application.yml"));
        String xaiClient = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/ai/XaiProviderClient.java"));
        String dashboard = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));

        assertThat(application)
                .contains("fast-model: ${TRADE_MODEL_AI_OPENAI_GPT_FINAL_FAST_MODEL:gpt-5.6-luna}")
                .contains("reasoning-model: ${TRADE_MODEL_AI_OPENAI_GPT_FINAL_REASONING_MODEL:gpt-5.6-sol}")
                .contains("${TRADE_MODEL_AI_OPENAI_GPT_FINAL_FALLBACK_GPT55_MODEL:gpt-5.5}")
                .contains("${TRADE_MODEL_AI_OPENAI_GPT_FINAL_FALLBACK_GPT54_MODEL:gpt-5.4}")
                .contains("model: ${TRADE_MODEL_AI_GEMINI_MODEL:gemini-3.5-flash}")
                .contains("model: ${TRADE_MODEL_AI_XAI_MODEL:grok-4.5}")
                .doesNotContain("gpt-4.1-mini", "gpt-4o", "gemini-1.5-flash", "gemini-2.5-pro");
        assertThat(xaiClient).contains("/v1/responses").doesNotContain("/v1/chat/completions");
        assertThat(dashboard).contains("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
    }

    @Test
    void aiModelRoutingRemainsReviewOnlyAndCannotCreateTradingRecords() throws Exception {
        AiOrchestratorResult result = new AiOrchestratorResult();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isManualReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotAutoTrading()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotUserPositionCreation()).isTrue();
        assertThat(result.isNotPositionMutation()).isTrue();
        assertThat(result.isNotStateMachineOverride()).isTrue();
        assertThat(result.isRuleDirectionPreserved()).isTrue();

        String orchestrator = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/impl/AiDecisionOrchestratorServiceImpl.java"));
        assertThat(orchestrator)
                .doesNotContain("UserPositionMapper", "ExecutionPlanMapper", "OrderMapper")
                .doesNotContain("manualOpen(", "manualClose(", "placeOrder(");
    }

    private OpenAiProviderClient client(SequenceTransport transport) {
        return new OpenAiProviderClient(properties(), transport, objectMapper);
    }

    private static AiOrchestratorProperties properties() {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        AiProviderProperties provider = properties.getOpenai();
        provider.setEnabled(true);
        provider.setApiKey("test-key-not-a-secret");
        provider.setBaseUrl("https://provider.test");
        provider.setRequestsPerMinute(1);
        GptFinalModelRoutingProperties routing = provider.getGptFinal();
        routing.setFastModel("gpt-5.6-luna");
        routing.setReasoningModel("gpt-5.6-sol");
        routing.setFallbackModels(List.of("gpt-5.5", "gpt-5.4"));
        routing.setFallbackEnabled(true);
        return properties;
    }

    private static AiProviderRequest request(boolean deep) {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("analysis-model-routing");
        request.setTraceId("trace-model-routing");
        request.setSymbol("NON_MARKET_TEST");
        request.setTimeframe("NOT_APPLICABLE");
        request.setRuleMarketBias("NEUTRAL");
        request.setRuleRiskLevel(deep ? "HIGH" : "MEDIUM");
        request.setMultiTimeframeState(deep ? "CONTRADICTION" : "ALIGNED");
        request.setDecisionFacts(Map.of("confused", deep, "ruleDirectionPreserved", true));
        return request;
    }

    private static AiHttpResponse modelNotFound() {
        return new AiHttpResponse(404, "model unavailable", Map.of());
    }

    private static AiHttpResponse openAiResponse() {
        String payload = "{\"stance\":\"ABSTAIN\",\"conflictLevel\":\"NONE\","
                + "\"reasonCodes\":[\"MODEL_ROUTING_TEST\"],\"summary\":\"review only\"}";
        return new AiHttpResponse(200, "{\"output_text\":" + quote(payload) + "}", Map.of());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static final class SequenceTransport implements AiHttpTransport {
        private final Deque<Object> outcomes = new ArrayDeque<>();
        private final List<String> models = new ArrayList<>();

        static SequenceTransport responding(AiHttpResponse... responses) {
            SequenceTransport transport = new SequenceTransport();
            for (AiHttpResponse response : responses) {
                transport.addResponse(response);
            }
            return transport;
        }

        void addResponse(AiHttpResponse response) { outcomes.add(response); }
        void addFailure(IOException failure) { outcomes.add(failure); }
        List<String> models() { return List.copyOf(models); }

        @Override
        public AiHttpResponse post(AiHttpRequest request) throws IOException {
            try {
                models.add(new ObjectMapper().readTree(request.getBody()).path("model").asText());
            } catch (Exception e) {
                throw new IOException("invalid test request", e);
            }
            Object outcome = outcomes.removeFirst();
            if (outcome instanceof IOException failure) {
                throw failure;
            }
            return (AiHttpResponse) outcome;
        }
    }
}
