package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiProviderModelStrategyTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void gptFinalUsesConfiguredQualityFirstModelAndAvailabilityRemainsUnknown() throws Exception {
        AiOrchestratorProperties properties = properties();
        properties.getOpenai().setModel("gpt-5.6-sol");
        CapturingTransport transport = new CapturingTransport(openAiResponse());
        OpenAiProviderClient client = new OpenAiProviderClient(properties, transport, objectMapper);

        AiProviderReadiness readiness = client.readiness();
        client.review(request());

        assertThat(readiness.getConfiguredModel()).isEqualTo("gpt-5.6-sol");
        assertThat(readiness.getEffectiveModel()).isEqualTo("gpt-5.6-sol");
        assertThat(readiness.getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_AVAILABLE_UNKNOWN);
        assertThat(readiness.isReady()).isFalse();
        assertThat(transport.request.getBody())
                .contains("\"model\":\"gpt-5.6-sol\"")
                .contains("\"reasoning\":{\"effort\":\"high\"}")
                .doesNotContain("\"temperature\"");
        assertThat(properties.getModelStrategy().getGptFinal().getPriority())
                .isEqualTo(AiRoleModelPriority.QUALITY_FIRST);
    }

    @Test
    void gptCompatibilityFallbackMustBeExplicitAndEmitsReasonCode() {
        AiProviderProperties provider = properties().getOpenai();
        provider.setModel("gpt-5.6-sol");
        provider.setCompatibilityFallbackModel("gpt-4.1-mini");
        provider.setFallbackReason(AiProviderProperties.OPENAI_COMPATIBILITY_FALLBACK_REASON);

        assertThat(provider.getEffectiveModel()).isEqualTo("gpt-5.6-sol");
        assertThat(provider.isFallbackUsed()).isFalse();

        provider.setCompatibilityFallbackActive(true);
        OpenAiProviderClient client = new OpenAiProviderClient(propertiesWith(provider),
                new CapturingTransport(openAiResponse()), objectMapper);
        AiProviderReadiness readiness = client.readiness();

        assertThat(readiness.getConfiguredModel()).isEqualTo("gpt-5.6-sol");
        assertThat(readiness.getEffectiveModel()).isEqualTo("gpt-4.1-mini");
        assertThat(readiness.isFallbackUsed()).isTrue();
        assertThat(readiness.getFallbackReason())
                .isEqualTo("OPENAI_MODEL_FALLBACK_COMPATIBILITY");
        assertThat(readiness.getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_FALLBACK_ACTIVE);
        assertThat(readiness.getReasonCodes())
                .containsExactly("OPENAI_MODEL_FALLBACK_COMPATIBILITY");
    }

    @Test
    void malformedOrMissingModelSelectionFailsClosed() {
        AiOrchestratorProperties properties = properties();
        properties.getOpenai().setModel(" ");
        OpenAiProviderClient client = new OpenAiProviderClient(properties,
                new CapturingTransport(openAiResponse()), objectMapper);

        assertThat(client.readiness().isConfigured()).isFalse();
        assertThat(client.readiness().isReady()).isFalse();
        assertThat(client.readiness().getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_NOT_CONFIGURED);
        assertThat(client.readiness().getReasonCodes()).containsExactly("MODEL_NOT_CONFIGURED");

        properties.getOpenai().setModel("gpt-5.6-sol");
        properties.getOpenai().setCompatibilityFallbackActive(true);
        properties.getOpenai().setCompatibilityFallbackModel("gpt-4.1-mini");
        properties.getOpenai().setFallbackReason("UNDECLARED_DOWNGRADE");
        assertThat(client.readiness().isConfigured()).isFalse();
        assertThat(client.readiness().getEffectiveModel()).isEmpty();
    }

    @Test
    void providerDefaultsAndEndpointsMatchRoleStrategy() throws Exception {
        String application = Files.readString(Path.of("src/main/resources/application.yml"));
        String xaiClient = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/ai/XaiProviderClient.java"));
        String dashboard = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));

        assertThat(application)
                .contains("model: ${TRADE_MODEL_AI_OPENAI_MODEL:gpt-5.6-sol}")
                .contains("model: ${TRADE_MODEL_AI_GEMINI_MODEL:gemini-3.5-flash}")
                .contains("model: ${TRADE_MODEL_AI_XAI_MODEL:grok-4.5}")
                .contains("priority: ${TRADE_MODEL_AI_GPT_FINAL_PRIORITY:QUALITY_FIRST}")
                .contains("priority: ${TRADE_MODEL_AI_GEMINI_REVIEW_PRIORITY:BALANCED}")
                .contains("priority: ${TRADE_MODEL_AI_GROK_CHALLENGE_PRIORITY:CHALLENGE_FIRST}")
                .doesNotContain("gemini-1.5-flash");
        assertThat(xaiClient).contains("/v1/responses").doesNotContain("/v1/chat/completions");
        assertThat(dashboard).contains("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
    }

    @Test
    void aiModelSelectionRemainsReviewOnlyAndCannotCreateTradingRecords() throws Exception {
        AiOrchestratorResult result = new AiOrchestratorResult();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotExecutable()).isTrue();
        assertThat(result.isNotOrderExecution()).isTrue();
        assertThat(result.isNotUserPositionCreation()).isTrue();
        assertThat(result.isNotPositionMutation()).isTrue();
        assertThat(result.isRuleDirectionPreserved()).isTrue();

        String orchestrator = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/impl/AiDecisionOrchestratorServiceImpl.java"));
        assertThat(orchestrator)
                .doesNotContain("UserPositionMapper", "ExecutionPlanMapper", "OrderMapper")
                .doesNotContain("manualOpen(", "manualClose(", "placeOrder(");
    }

    private static AiOrchestratorProperties properties() {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        configure(properties.getOpenai());
        return properties;
    }

    private static AiOrchestratorProperties propertiesWith(AiProviderProperties openai) {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setOpenai(openai);
        return properties;
    }

    private static void configure(AiProviderProperties provider) {
        provider.setEnabled(true);
        provider.setApiKey("test-key-not-a-secret");
        provider.setModel("gpt-5.6-sol");
        provider.setBaseUrl("https://provider.test");
        provider.setRequestsPerMinute(1);
    }

    private static AiProviderRequest request() {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("analysis-model-strategy");
        request.setTraceId("trace-model-strategy");
        request.setSymbol("NON_MARKET_TEST");
        request.setTimeframe("NOT_APPLICABLE");
        request.setRuleMarketBias("NEUTRAL");
        request.setDecisionFacts(Map.of("ruleDirectionPreserved", true));
        return request;
    }

    private static AiHttpResponse openAiResponse() {
        String payload = "{\"stance\":\"ABSTAIN\",\"conflictLevel\":\"NONE\","
                + "\"reasonCodes\":[\"MODEL_STRATEGY_TEST\"],\"summary\":\"review only\"}";
        return new AiHttpResponse(200, "{\"output_text\":" + quote(payload) + "}", Map.of());
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static final class CapturingTransport implements AiHttpTransport {
        private final AiHttpResponse response;
        private AiHttpRequest request;

        private CapturingTransport(AiHttpResponse response) {
            this.response = response;
        }

        @Override
        public AiHttpResponse post(AiHttpRequest request) {
            this.request = request;
            return response;
        }
    }
}
