package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiProviderStructuredOutputContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void stableV1InteractionsUsesCanonicalModelName() throws Exception {
        CapturingTransport transport = transport(completed(reviewJson("canonical")));

        client(transport).review(request(), 15_000L);

        JsonNode body = objectMapper.readTree(transport.request.getBody());
        assertThat(transport.request.getUrl())
                .isEqualTo("https://generativelanguage.googleapis.com/v1/interactions");
        assertThat(body.path("model").asText()).isEqualTo("models/gemini-3.5-flash");
        assertThat(GeminiProviderClient.canonicalModelName("models/gemini-3.5-flash"))
                .isEqualTo("models/gemini-3.5-flash");
    }

    @Test
    void interactionsRequestUsesResponseFormatSchema() throws Exception {
        JsonNode body = requestBody();
        JsonNode format = body.path("response_format");
        JsonNode schema = format.path("schema");

        assertThat(format.path("type").asText()).isEqualTo("text");
        assertThat(format.path("mime_type").asText()).isEqualTo("application/json");
        assertThat(schema.path("type").asText()).isEqualTo("object");
        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("required")).hasSize(4);
        assertThat(schema.path("properties").path("reasonCodes").path("maxItems").asInt())
                .isEqualTo(8);
    }

    @Test
    void geminiInteractionsUses512OutputTokenBound() throws Exception {
        JsonNode generation = requestBody().path("generation_config");

        assertThat(generation.path("max_output_tokens").asInt()).isEqualTo(512);
    }

    @Test
    void geminiInteractionsUsesMinimalThinking() throws Exception {
        JsonNode generation = requestBody().path("generation_config");

        assertThat(generation.path("temperature").asInt()).isZero();
        assertThat(generation.path("seed").asInt()).isEqualTo(42);
        assertThat(generation.path("thinking_level").asText()).isEqualTo("minimal");
    }

    @Test
    void geminiInteractionsStillUsesNoThinkingSummary() throws Exception {
        JsonNode generation = requestBody().path("generation_config");

        assertThat(generation.path("thinking_summaries").asText()).isEqualTo("none");
    }

    @Test
    void interactionsRequestUsesStoreFalseAndNoTools() throws Exception {
        JsonNode body = requestBody();

        assertThat(body.path("store").asBoolean()).isFalse();
        assertThat(body.path("stream").asBoolean()).isFalse();
        assertThat(body.path("system_instruction").isTextual()).isTrue();
        assertThat(body.path("input").isTextual()).isTrue();
        assertThat(body.has("tools")).isFalse();
        assertThat(body.has("orderAction")).isFalse();
        assertThat(body.has("positionAction")).isFalse();
        assertThat(body.has("pushAction")).isFalse();
    }

    @Test
    void finalModelOutputStepIsSelected() {
        AiProviderReviewResult result = review(completed(List.of(
                modelOutput(reviewJson("first")),
                modelOutput(reviewJson("last")))));

        assertThat(result.successful()).isTrue();
        assertThat(result.getSummary()).isEqualTo("last");
    }

    @Test
    void earlierModelOutputStepsAreNotConcatenated() {
        AiProviderReviewResult result = review(completed(List.of(
                modelOutput("non-json explanatory text"),
                modelOutput(reviewJson("final valid")))));

        assertThat(result.successful()).isTrue();
        assertThat(result.getSummary()).isEqualTo("final valid");
    }

    @Test
    void multipleTextBlocksInsideFinalStepAreConcatenated() {
        String json = reviewJson("split blocks");
        int midpoint = json.length() / 2;
        AiProviderReviewResult result = review(completed(List.of(
                modelOutput(json.substring(0, midpoint), json.substring(midpoint)))));

        assertThat(result.successful()).isTrue();
        assertThat(result.getSummary()).isEqualTo("split blocks");
    }

    @Test
    void multipleIncompatibleFinalOutputsFailClosed() {
        AiProviderReviewResult result = review(completed(List.of(
                modelOutput(reviewJson("output A"), reviewJson("output B")))));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("GEMINI_INTERACTION_FINAL_JSON_INVALID");
    }

    @Test
    void validFinalStructuredOutputPasses() {
        AiProviderReviewResult result = review(completed(reviewJson("valid")));

        assertThat(result.successful()).isTrue();
        assertThat(result.getStance()).isEqualTo(AiReviewStance.ABSTAIN);
        assertThat(result.getConflictLevel()).isEqualTo(AiReviewConflictLevel.NONE);
    }

    @Test
    void malformedFinalOutputFailsClosed() {
        AiProviderReviewResult result = review(completed("{\"stance\":\"ABSTAIN\""));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("GEMINI_INTERACTION_FINAL_JSON_INVALID");
        assertThat(result.getGeminiInteractionDiagnostic().finalJsonParseStatus()).isEqualTo("FAIL");
    }

    @ParameterizedTest
    @CsvSource({
            "in_progress,GEMINI_INTERACTION_STATUS_IN_PROGRESS",
            "requires_action,GEMINI_INTERACTION_STATUS_REQUIRES_ACTION",
            "failed,GEMINI_INTERACTION_STATUS_FAILED",
            "cancelled,GEMINI_INTERACTION_STATUS_CANCELLED",
            "incomplete,GEMINI_INTERACTION_STATUS_INCOMPLETE"
    })
    void nonCompletedInteractionStatusPreservesExactReason(String status, String expectedReason) {
        AiProviderReviewResult result = review(
                interaction(status, "interaction-id", List.of(modelOutput(reviewJson(status))), true));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo(expectedReason);
        assertThat(result.getGeminiInteractionDiagnostic().failureReason().name())
                .isEqualTo(expectedReason);
        assertThat(result.getGeminiInteractionDiagnostic().interactionStatus())
                .isEqualTo(status.toUpperCase());
    }

    @Test
    void missingInteractionStatusPreservesExactReason() {
        AiProviderReviewResult result = review(
                interaction(null, "interaction-id", List.of(modelOutput(reviewJson("missing"))), true));

        assertThat(result.getErrorCode()).isEqualTo("GEMINI_INTERACTION_STATUS_MISSING");
        assertThat(result.getGeminiInteractionDiagnostic().interactionStatus()).isEqualTo("MISSING");
    }

    @Test
    void geminiIncompleteDoesNotAttemptJsonOrV1ParsingAndDoesNotRetry() {
        CapturingTransport transport = transport(interaction(
                "incomplete", "interaction-id", List.of(modelOutput("not-inspected")), true));

        AiProviderReviewResult result = client(transport).review(request(), 15_000L);

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("GEMINI_INTERACTION_STATUS_INCOMPLETE");
        assertThat(result.getGeminiInteractionDiagnostic().finalJsonParseStatus())
                .isEqualTo("NOT_CHECKED");
        assertThat(result.getGeminiInteractionDiagnostic().v1ContractStatus())
                .isEqualTo("NOT_CHECKED");
        assertThat(result.getGeminiInteractionDiagnostic().finalTextBlockCount()).isZero();
        assertThat(result.getGeminiInteractionDiagnostic().finalTextLength()).isZero();
        assertThat(transport.calls).isEqualTo(1);
        assertThat(transport.request.getUrl()).endsWith("/v1/interactions");
        assertThat(transport.request.getUrl()).doesNotContain("generateContent", "v1beta");
    }

    @Test
    void missingFinalTextFailsClosed() {
        Map<String, Object> step = Map.of(
                "type", "model_output",
                "content", List.of(Map.of("type", "thought", "text", "private thought")));
        AiProviderReviewResult result = review(interaction("completed", "interaction-id", List.of(step), true));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("GEMINI_INTERACTION_FINAL_TEXT_MISSING");
        assertThat(result.getGeminiInteractionDiagnostic().finalTextBlockCount()).isZero();
    }

    @Test
    void missingModelOutputPreservesExactReason() {
        AiProviderReviewResult result = review(interaction(
                "completed", "interaction-id", List.of(Map.of("type", "tool_output")), true));

        assertThat(result.getErrorCode()).isEqualTo("GEMINI_INTERACTION_MODEL_OUTPUT_MISSING");
        assertThat(result.getGeminiInteractionDiagnostic().modelOutputStepCount()).isZero();
        assertThat(result.getGeminiInteractionDiagnostic().finalModelOutputPresent()).isFalse();
    }

    @Test
    void jsonValidButV1InvalidPreservesExactReason() {
        AiProviderReviewResult result = review(completed("{\"stance\":\"ABSTAIN\"}"));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("GEMINI_INTERACTION_V1_CONTRACT_INVALID");
        assertThat(result.getGeminiInteractionDiagnostic().finalJsonParseStatus()).isEqualTo("PASS");
        assertThat(result.getGeminiInteractionDiagnostic().v1ContractStatus()).isEqualTo("FAIL");
    }

    @Test
    void interactionDiagnosticStoresPresenceAndCountsWithoutValues() {
        AiProviderReviewResult result = review(completed("not-json-private-provider-output"));
        GeminiInteractionDiagnostic diagnostic = result.getGeminiInteractionDiagnostic();

        assertThat(diagnostic.interactionIdPresent()).isTrue();
        assertThat(diagnostic.usagePresent()).isTrue();
        assertThat(diagnostic.totalInputTokensPresent()).isTrue();
        assertThat(diagnostic.totalOutputTokensPresent()).isTrue();
        assertThat(diagnostic.totalThoughtTokensPresent()).isTrue();
        assertThat(diagnostic.totalTokensPresent()).isTrue();
        assertThat(diagnostic.stepCount()).isEqualTo(1);
        assertThat(diagnostic.modelOutputStepCount()).isEqualTo(1);
        assertThat(diagnostic.finalTextBlockCount()).isEqualTo(1);
        assertThat(diagnostic.finalTextLength()).isPositive();
        assertThat(diagnostic.toString()).doesNotContain(
                "interaction-id", "not-json-private-provider-output", "private thought");
    }

    @Test
    void malformedResponseEnvelopeUsesGenericSchemaFallback() {
        AiProviderReviewResult result = review(new AiHttpResponse(200, "{", Map.of()));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("PROVIDER_RESPONSE_SCHEMA");
        assertThat(result.getGeminiInteractionDiagnostic()).isNull();
    }

    @Test
    void interactionIdMapsToProviderRequestId() {
        AiProviderReviewResult result = review(completed(reviewJson("id")));

        assertThat(result.getProviderRequestId()).isEqualTo("interaction-id");
        assertThat(result.getReasonCodes()).doesNotContain("GEMINI_INTERACTION_ID_MISSING");
    }

    @Test
    void absentInteractionIdIsNotInventedAndAddsReason() {
        AiProviderReviewResult result = review(
                interaction("completed", null, List.of(modelOutput(reviewJson("no id"))), true));

        assertThat(result.successful()).isTrue();
        assertThat(result.getProviderRequestId()).isNull();
        assertThat(result.getReasonCodes()).contains("GEMINI_INTERACTION_ID_MISSING");
    }

    @Test
    void interactionUsageMapsCorrectly() {
        AiProviderReviewResult result = review(completed(reviewJson("usage")));

        assertThat(result.getInputTokens()).isEqualTo(13L);
        assertThat(result.getOutputTokens()).isEqualTo(5L);
        assertThat(result.getTotalTokens()).isEqualTo(18L);
    }

    @Test
    void geminiNormalizerDoesNotRepairInvalidJson() {
        GeminiRoleResultNormalizer normalizer = new GeminiRoleResultNormalizer(objectMapper);

        assertThat(normalizer.normalize("```json\n" + reviewJson("markdown") + "\n```")).isNull();
        assertThat(normalizer.normalize("prefix " + reviewJson("prose"))).isNull();
        assertThat(normalizer.normalize("{\"stance\":")).isNull();
    }

    @Test
    void noGenerateContentAutomaticFallback() {
        CapturingTransport transport = transport(new AiHttpResponse(404, "{}", Map.of()));

        AiProviderReviewResult result = client(transport).review(request(), 15_000L);

        assertThat(result.successful()).isFalse();
        assertThat(transport.calls).isEqualTo(1);
        assertThat(transport.request.getUrl()).endsWith("/v1/interactions");
        assertThat(transport.request.getUrl()).doesNotContain("generateContent", "v1beta");
    }

    @Test
    void geminiReadinessBeforeCallIsConfiguredAndNotReady() {
        GeminiProviderClient client = client(transport(completed(reviewJson("unused"))));

        AiProviderReadiness readiness = client.readiness();

        assertThat(readiness.getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_CONFIGURED);
        assertThat(readiness.getConfiguredModel()).isEqualTo("gemini-3.5-flash");
        assertThat(readiness.getEffectiveModel()).isEqualTo("models/gemini-3.5-flash");
        assertThat(readiness.isReady()).isFalse();
    }

    @Test
    void geminiReadinessAfterContractValidInteractionIsActiveAndReady() {
        GeminiProviderClient client = client(transport(completed(reviewJson("verified"))));

        assertThat(client.review(request()).successful()).isTrue();

        AiProviderReadiness readiness = client.readiness();
        assertThat(readiness.getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_ACTIVE);
        assertThat(readiness.isReady()).isTrue();
        assertThat(readiness.getReasonCodes()).containsExactly("MODEL_CALL_VERIFIED");
        assertThat(readiness.getReasonCodes()).doesNotContain("MODEL_AVAILABILITY_UNVERIFIED");
    }

    @Test
    void failedInteractionRemainsConfiguredAndNotReady() {
        GeminiProviderClient client = client(transport(
                interaction("failed", "interaction-id", List.of(modelOutput(reviewJson("failed"))), true)));

        assertThat(client.review(request()).successful()).isFalse();

        AiProviderReadiness readiness = client.readiness();
        assertThat(readiness.getModelReadinessStatus())
                .isEqualTo(AiModelReadinessStatus.MODEL_CONFIGURED);
        assertThat(readiness.isReady()).isFalse();
        assertThat(readiness.getReasonCodes()).containsExactly("MODEL_AVAILABILITY_UNVERIFIED");
    }

    @Test
    void dashboardRoleSemanticsAndNoTradingBoundariesRemainUnchanged() throws Exception {
        String dashboard = Files.readString(Path.of("src/main/resources/templates/dashboard.html"));
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/ai/GeminiProviderClient.java"));

        assertThat(dashboard).contains("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
        assertThat(source).doesNotContain(
                "UserPosition", "ExecutionPlan", "OrderService", "PushService", "Telegram",
                "generateContent", "v1beta");
    }

    private JsonNode requestBody() throws Exception {
        CapturingTransport transport = transport(completed(reviewJson("request")));
        client(transport).review(request(), 15_000L);
        return objectMapper.readTree(transport.request.getBody());
    }

    private AiProviderReviewResult review(AiHttpResponse response) {
        return client(transport(response)).review(request(), 15_000L);
    }

    private GeminiProviderClient client(CapturingTransport transport) {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setMaxInputChars(4_000);
        properties.setMaxOutputTokens(500);
        AiProviderProperties gemini = properties.getGemini();
        gemini.setEnabled(true);
        gemini.setApiKey("test-gemini-key");
        gemini.setModel("gemini-3.5-flash");
        gemini.setBaseUrl("https://generativelanguage.googleapis.com");
        return new GeminiProviderClient(properties, transport, objectMapper);
    }

    private static AiProviderRequest request() {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("gemini-interactions-contract");
        request.setTraceId("trace-gemini-interactions-contract");
        request.setSymbol("NON_MARKET_TEST");
        request.setTimeframe("NOT_APPLICABLE");
        request.setRuleMarketBias("NEUTRAL");
        request.setRuleRiskLevel("LOW");
        request.setDecisionFacts(Map.of("reviewOnly", true, "ruleDirectionPreserved", true));
        return request;
    }

    private AiHttpResponse completed(String text) {
        return completed(List.of(modelOutput(text)));
    }

    private AiHttpResponse completed(List<Map<String, Object>> steps) {
        return interaction("completed", "interaction-id", steps, true);
    }

    private AiHttpResponse interaction(
            String status, String id, List<Map<String, Object>> steps, boolean includeUsage) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            if (id != null) {
                body.put("id", id);
            }
            body.put("status", status);
            body.put("steps", steps);
            if (includeUsage) {
                body.put("usage", Map.of(
                        "total_input_tokens", 13,
                        "total_output_tokens", 5,
                        "total_tokens", 18,
                        "total_thought_tokens", 2));
            }
            return new AiHttpResponse(200, objectMapper.writeValueAsString(body), Map.of());
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }

    private static Map<String, Object> modelOutput(String... texts) {
        List<Map<String, Object>> content = new ArrayList<>();
        for (String text : texts) {
            content.add(Map.of("type", "text", "text", text));
        }
        return Map.of("type", "model_output", "content", content);
    }

    private static String reviewJson(String summary) {
        return "{\"stance\":\"ABSTAIN\",\"conflictLevel\":\"NONE\","
                + "\"reasonCodes\":[\"SCHEMA_OK\"],\"summary\":\"" + summary + "\"}";
    }

    private static CapturingTransport transport(AiHttpResponse response) {
        return new CapturingTransport(response);
    }

    private static final class CapturingTransport implements AiHttpTransport {
        private final AiHttpResponse response;
        private int calls;
        private AiHttpRequest request;

        private CapturingTransport(AiHttpResponse response) {
            this.response = response;
        }

        @Override
        public AiHttpResponse post(AiHttpRequest request) throws IOException {
            calls++;
            this.request = request;
            return response;
        }
    }
}
