package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiProviderStructuredOutputContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalGeminiJsonTextPassesStrictParserAndRequestUsesOfficialSchemaFields() throws Exception {
        CapturingTransport transport = new CapturingTransport(response(reviewJson()));

        AiProviderReviewResult result = client(transport).review(request(), 15_000L);

        assertThat(result.successful()).isTrue();
        JsonNode requestBody = objectMapper.readTree(transport.request.getBody());
        assertThat(transport.request.getUrl()).isEqualTo(
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent");
        assertThat(transport.request.getHeaders()).containsEntry("x-goog-api-key", "test-gemini-key");
        assertThat(requestBody.path("contents").isArray()).isTrue();
        String instruction = requestBody.path("systemInstruction").path("parts").get(0).path("text").asText();
        assertThat(instruction).contains(
                "JSON only", "no Markdown", "no code fence", "no prose", "no explanation",
                "stance, conflictLevel, reasonCodes, summary");

        JsonNode generation = requestBody.path("generationConfig");
        assertThat(generation.path("responseMimeType").asText()).isEqualTo("application/json");
        JsonNode schema = generation.path("responseJsonSchema");
        assertThat(schema.path("type").asText()).isEqualTo("object");
        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("required")).hasSize(4);
        List<String> fields = new ArrayList<>();
        schema.path("properties").fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactly("stance", "conflictLevel", "reasonCodes", "summary");
    }

    @Test
    void geminiJsonWithWhitespacePassesStrictParser() throws Exception {
        AiProviderReviewResult result = review(response("  \n" + reviewJson() + "\n  "));

        assertThat(result.successful()).isTrue();
    }

    @Test
    void missingCandidatesFailsClosed() throws Exception {
        assertInvalid("{}", "INVALID_EMPTY_RESPONSE");
    }

    @Test
    void missingContentFailsClosed() throws Exception {
        assertInvalid("{\"candidates\":[{}]}", "INVALID_EMPTY_RESPONSE");
    }

    @Test
    void missingPartsFailsClosed() throws Exception {
        assertInvalid("{\"candidates\":[{\"content\":{}}]}", "INVALID_EMPTY_RESPONSE");
    }

    @Test
    void emptyTextFailsClosed() throws Exception {
        assertInvalid(response("  ").getBody(), "INVALID_EMPTY_RESPONSE");
    }

    @Test
    void markdownFencedJsonFailsClosed() throws Exception {
        assertInvalid(response("```json\n" + reviewJson() + "\n```").getBody(),
                "INVALID_EMPTY_RESPONSE");
    }

    @Test
    void naturalLanguagePlusJsonFailsClosed() throws Exception {
        assertInvalid(response("Here is the result: " + reviewJson()).getBody(),
                "INVALID_RESPONSE_PARSE");
    }

    @Test
    void missingRequiredFieldFailsClosedWithSanitizedDiagnostic() throws Exception {
        AiProviderReviewResult result = review(response("""
                {"stance":"ABSTAIN","reasonCodes":["SCHEMA_GAP"],"summary":"review only"}
                """));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("INVALID_MISSING_FIELD_CONFLICTLEVEL");
        assertThat(result.getSchemaDiagnostic().missingFields()).containsExactly("conflictLevel");
    }

    @Test
    void extraFieldFailsClosedWithSanitizedDiagnostic() throws Exception {
        AiProviderReviewResult result = review(response("""
                {"stance":"ABSTAIN","conflictLevel":"NONE","reasonCodes":["SCHEMA_GAP"],
                 "summary":"review only","extraProviderField":"private value"}
                """));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("INVALID_UNKNOWN_FIELD_EXTRAPROVIDERFIELD");
        assertThat(result.getSchemaDiagnostic().unexpectedFields()).containsExactly("extraProviderField");
    }

    @Test
    void wrongFieldTypeFailsClosedWithSanitizedDiagnostic() throws Exception {
        AiProviderReviewResult result = review(response("""
                {"stance":"ABSTAIN","conflictLevel":"NONE","reasonCodes":"SCHEMA_GAP",
                 "summary":"review only"}
                """));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("INVALID_FIELD_TYPE_REASONCODES");
        assertThat(result.getSchemaDiagnostic().typeMismatchFields())
                .containsExactly("reasonCodes expected ARRAY got STRING");
    }

    private void assertInvalid(String responseBody, String errorCode) {
        AiProviderReviewResult result = review(new AiHttpResponse(200, responseBody, Map.of()));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo(errorCode);
    }

    private AiProviderReviewResult review(AiHttpResponse response) {
        return client(new CapturingTransport(response)).review(request(), 15_000L);
    }

    private GeminiProviderClient client(CapturingTransport transport) {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.setMaxInputChars(4_000);
        properties.setMaxOutputTokens(128);
        AiProviderProperties gemini = properties.getGemini();
        gemini.setEnabled(true);
        gemini.setApiKey("test-gemini-key");
        gemini.setModel("gemini-3.5-flash");
        gemini.setBaseUrl("https://generativelanguage.googleapis.com");
        return new GeminiProviderClient(properties, transport, objectMapper);
    }

    private static AiProviderRequest request() {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("gemini-structured-contract");
        request.setTraceId("trace-gemini-structured-contract");
        request.setSymbol("NON_MARKET_TEST");
        request.setTimeframe("NOT_APPLICABLE");
        request.setRuleMarketBias("NEUTRAL");
        request.setRuleRiskLevel("LOW");
        request.setDecisionFacts(Map.of("reviewOnly", true, "ruleDirectionPreserved", true));
        return request;
    }

    private AiHttpResponse response(String text) throws Exception {
        Map<String, Object> body = Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of("parts", List.of(Map.of("text", text))))),
                "usageMetadata", Map.of(
                        "promptTokenCount", 4,
                        "candidatesTokenCount", 8,
                        "totalTokenCount", 12),
                "responseId", "test-gemini-response-id");
        return new AiHttpResponse(200, objectMapper.writeValueAsString(body), Map.of());
    }

    private static String reviewJson() {
        return "{\"stance\":\"ABSTAIN\",\"conflictLevel\":\"NONE\","
                + "\"reasonCodes\":[\"SCHEMA_OK\"],\"summary\":\"schema review only\"}";
    }

    private static final class CapturingTransport implements AiHttpTransport {
        private final AiHttpResponse response;
        private AiHttpRequest request;

        private CapturingTransport(AiHttpResponse response) {
            this.response = response;
        }

        @Override
        public AiHttpResponse post(AiHttpRequest request) throws IOException {
            this.request = request;
            return response;
        }
    }
}
