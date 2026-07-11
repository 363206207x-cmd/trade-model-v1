package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiProviderStructuredOutputContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void normalGeminiJsonMimePassesNormalizerAndStrictInternalParser() throws Exception {
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
                "GEMINI_REVIEW", "Return ONLY one valid JSON object", "Do not return Markdown",
                "a code fence", "an explanation", "a refusal", "a prefix", "a suffix",
                "stance, conflictLevel, reasonCodes, summary",
                "SUPPORT, CHALLENGE, ABSTAIN", "NONE, MINOR, MAJOR, EXTREME",
                "\"stance\":\"ABSTAIN\"", "\"reasonCodes\":[\"INSUFFICIENT_DATA\"]",
                "\"summary\":\"Insufficient evidence\"", "Never replace that JSON fallback with plain text");
        assertThat(instruction).doesNotContain("\"stance\":\"NEUTRAL\"");

        JsonNode generation = requestBody.path("generationConfig");
        assertThat(generation.path("responseMimeType").asText()).isEqualTo("application/json");
        assertThat(generation.has("responseJsonSchema")).isFalse();
    }

    @Test
    void geminiJsonWithWhitespacePassesStrictParser() throws Exception {
        AiProviderReviewResult result = review(response("  \n" + reviewJson() + "\n  "));

        assertThat(result.successful()).isTrue();
    }

    @Test
    void nestedResultFragmentIsNormalizedBeforeStrictParser() throws Exception {
        AiProviderReviewResult result = review(response("{\"result\":" + reviewJson() + "}"));

        assertThat(result.successful()).isTrue();
        assertThat(result.getReasonCodes()).containsExactly("SCHEMA_OK");
    }

    @Test
    void nestedAnalysisFragmentIsNormalizedBeforeStrictParser() throws Exception {
        AiProviderReviewResult result = review(response("{\"analysis\":" + reviewJson() + "}"));

        assertThat(result.successful()).isTrue();
        assertThat(result.getStance()).isEqualTo(AiReviewStance.ABSTAIN);
    }

    @Test
    void deterministicSnakeCaseFieldNamesAreNormalized() throws Exception {
        AiProviderReviewResult result = review(response("""
                {"analysis":{"stance":"ABSTAIN","conflict_level":"NONE",
                 "reason_codes":["SCHEMA_OK"],"summary":"review only"}}
                """));

        assertThat(result.successful()).isTrue();
        assertThat(result.getConflictLevel()).isEqualTo(AiReviewConflictLevel.NONE);
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
                "INVALID_EMPTY_RESPONSE");
    }

    @Test
    void plainTextRefusalFailsClosedWithoutAutomaticRepair() throws Exception {
        assertInvalid(response("I cannot provide that review.").getBody(),
                "INVALID_EMPTY_RESPONSE");
    }

    @Test
    void extractionDiagnosticNormalStructurePasses() throws Exception {
        GeminiExtractionDiagnostic diagnostic = extraction(response(reviewJson()));

        assertThat(diagnostic.successful()).isTrue();
        assertThat(diagnostic.candidatesPresent()).isTrue();
        assertThat(diagnostic.candidateCount()).isEqualTo(1);
        assertThat(diagnostic.contentPresent()).isTrue();
        assertThat(diagnostic.partsPresent()).isTrue();
        assertThat(diagnostic.textNodePresent()).isTrue();
        assertThat(diagnostic.textLength()).isEqualTo(reviewJson().length());
        assertThat(diagnostic.emptyText()).isFalse();
        assertThat(diagnostic.extractedJsonParsePassed()).isTrue();
    }

    @Test
    void extractionDiagnosticMissingCandidatesFails() throws Exception {
        GeminiExtractionDiagnostic diagnostic = extraction(
                new AiHttpResponse(200, "{}", Map.of()));

        assertThat(diagnostic.successful()).isFalse();
        assertThat(diagnostic.candidatesPresent()).isFalse();
        assertThat(diagnostic.candidateCount()).isZero();
        assertThat(diagnostic.contentPresent()).isFalse();
        assertThat(diagnostic.textNodePresent()).isFalse();
        assertThat(diagnostic.emptyText()).isTrue();
        assertThat(diagnostic.extractedJsonParsePassed()).isFalse();
    }

    @Test
    void extractionDiagnosticEmptyTextFails() throws Exception {
        GeminiExtractionDiagnostic diagnostic = extraction(response("  "));

        assertThat(diagnostic.successful()).isFalse();
        assertThat(diagnostic.candidatesPresent()).isTrue();
        assertThat(diagnostic.contentPresent()).isTrue();
        assertThat(diagnostic.partsPresent()).isTrue();
        assertThat(diagnostic.textNodePresent()).isTrue();
        assertThat(diagnostic.textLength()).isEqualTo(2);
        assertThat(diagnostic.emptyText()).isTrue();
        assertThat(diagnostic.extractedJsonParsePassed()).isFalse();
    }

    @Test
    void extractionDiagnosticUnexpectedContentStructureFails() throws Exception {
        GeminiExtractionDiagnostic diagnostic = extraction(new AiHttpResponse(200, """
                {"candidates":[{"content":{"parts":{"text":"{}"}}}]}
                """, Map.of()));

        assertThat(diagnostic.successful()).isFalse();
        assertThat(diagnostic.candidatesPresent()).isTrue();
        assertThat(diagnostic.candidateCount()).isEqualTo(1);
        assertThat(diagnostic.contentPresent()).isTrue();
        assertThat(diagnostic.partsPresent()).isTrue();
        assertThat(diagnostic.textNodePresent()).isFalse();
        assertThat(diagnostic.textLength()).isZero();
        assertThat(diagnostic.emptyText()).isTrue();
        assertThat(diagnostic.extractedJsonParsePassed()).isFalse();
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

    @Test
    void nestedTradingInstructionFieldFailsClosed() throws Exception {
        AiProviderReviewResult result = review(response("""
                {"result":{"stance":"ABSTAIN","conflictLevel":"NONE",
                 "reasonCodes":["SCHEMA_GAP"],"summary":"review only","orderAction":"BUY"}}
                """));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("INVALID_UNKNOWN_FIELD_ORDERACTION");
    }

    @Test
    void nestedUnknownUnsafeFieldFailsClosedWithoutSilentRemoval() throws Exception {
        AiProviderReviewResult result = review(response("""
                {"analysis":{"stance":"ABSTAIN","conflictLevel":"NONE",
                 "reasonCodes":["SCHEMA_GAP"],"summary":"review only",
                 "providerPayload":{"private":"value"}}}
                """));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("INVALID_UNKNOWN_FIELD_PROVIDERPAYLOAD");
    }

    @Test
    void nestedUnknownObjectFailsAndExposesOnlySanitizedShape() throws Exception {
        AiProviderReviewResult result = review(response("""
                {"review":{"stance":"ABSTAIN","conflictLevel":"NONE",
                 "reasonCodes":["PRIVATE_REASON"],"summary":"PRIVATE_SUMMARY"}}
                """));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo("INVALID_UNKNOWN_FIELD_REVIEW");
        assertThat(result.getGeminiResponseShapeDiagnostic().topLevelFields()).containsExactly("review");
        assertThat(result.getGeminiResponseShapeDiagnostic().nestedObjectPaths()).containsExactly(
                "review.stance", "review.conflictLevel", "review.reasonCodes", "review.summary");
        assertThat(result.getGeminiResponseShapeDiagnostic().fieldTypes()).containsExactly(
                "review:object", "review.stance:string", "review.conflictLevel:string",
                "review.reasonCodes:array", "review.summary:string");
        assertThat(result.getGeminiResponseShapeDiagnostic().toString()).doesNotContain(
                "PRIVATE_REASON", "PRIVATE_SUMMARY", "ABSTAIN", "NONE");
    }

    @Test
    void testOnlyRequestVariantsIsolatePlainJsonMimeAndStrictSchemaWithoutNetwork() throws Exception {
        CapturingTransport plainTransport = new CapturingTransport(response("plain capability fixture"));
        CapturingTransport jsonMimeTransport = new CapturingTransport(response("{\"mode\":\"json-only\"}"));
        CapturingTransport strictTransport = new CapturingTransport(response(reviewJson()));

        AiHttpResponse plainResponse = plainTransport.post(
                diagnosticRequest(plainTransport, DiagnosticVariant.PLAIN_TEXT));
        AiHttpResponse jsonMimeResponse = jsonMimeTransport.post(
                diagnosticRequest(jsonMimeTransport, DiagnosticVariant.JSON_MIME_ONLY));
        AiHttpResponse strictResponse = strictTransport.post(
                diagnosticRequest(strictTransport, DiagnosticVariant.STRICT_SCHEMA));

        JsonNode plainBody = objectMapper.readTree(plainTransport.request.getBody());
        JsonNode jsonMimeBody = objectMapper.readTree(jsonMimeTransport.request.getBody());
        JsonNode strictBody = objectMapper.readTree(strictTransport.request.getBody());

        assertThat(plainResponse.getStatusCode()).isEqualTo(200);
        assertThat(jsonMimeResponse.getStatusCode()).isEqualTo(200);
        assertThat(strictResponse.getStatusCode()).isEqualTo(200);
        assertThat(plainBody.path("generationConfig").has("responseMimeType")).isFalse();
        assertThat(plainBody.path("generationConfig").has("responseJsonSchema")).isFalse();
        assertThat(plainBody.path("systemInstruction").toString()).contains("plain-text capability response");
        assertThat(jsonMimeBody.path("generationConfig").path("responseMimeType").asText())
                .isEqualTo("application/json");
        assertThat(jsonMimeBody.path("generationConfig").has("responseJsonSchema")).isFalse();
        assertThat(jsonMimeBody.path("systemInstruction").toString()).contains("JSON capability response");
        assertThat(strictBody.path("generationConfig").path("responseMimeType").asText())
                .isEqualTo("application/json");
        assertThat(strictBody.path("generationConfig").path("responseJsonSchema").isObject()).isTrue();
        assertThat(plainBody.path("contents")).isEqualTo(strictBody.path("contents"));
        assertThat(jsonMimeBody.path("contents")).isEqualTo(strictBody.path("contents"));
    }

    @Test
    void strictSchemaUsesOnlyOfficiallySupportedJsonSchemaFeatures() throws Exception {
        CapturingTransport transport = new CapturingTransport(response(reviewJson()));
        JsonNode body = objectMapper.readTree(
                diagnosticRequest(transport, DiagnosticVariant.STRICT_SCHEMA).getBody());
        JsonNode schema = body.path("generationConfig").path("responseJsonSchema");
        JsonNode properties = schema.path("properties");

        assertThat(schema.path("type").asText()).isEqualTo("object");
        assertThat(schema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(schema.path("required")).hasSize(4);
        assertThat(properties.path("stance").path("type").asText()).isEqualTo("string");
        assertThat(properties.path("stance").path("enum").isArray()).isTrue();
        assertThat(properties.path("conflictLevel").path("type").asText()).isEqualTo("string");
        assertThat(properties.path("conflictLevel").path("enum").isArray()).isTrue();
        assertThat(properties.path("reasonCodes").path("type").asText()).isEqualTo("array");
        assertThat(properties.path("reasonCodes").path("items").path("type").asText())
                .isEqualTo("string");
        assertThat(properties.path("summary").path("type").asText()).isEqualTo("string");
    }

    private AiHttpRequest diagnosticRequest(
            CapturingTransport transport, DiagnosticVariant variant) throws Exception {
        GeminiProviderClient client = client(transport);
        AiHttpRequest request = client.buildHttpRequest("{}", 30_000L, "gemini-3.5-flash");
        ObjectNode body = (ObjectNode) objectMapper.readTree(request.getBody());
        ObjectNode generation = (ObjectNode) body.path("generationConfig");
        if (variant == DiagnosticVariant.PLAIN_TEXT) {
            generation.remove(List.of("responseMimeType", "responseJsonSchema"));
            setDiagnosticInstruction(body, "Return one short plain-text capability response.");
        } else if (variant == DiagnosticVariant.JSON_MIME_ONLY) {
            generation.remove("responseJsonSchema");
            setDiagnosticInstruction(body, "Return one JSON capability response without a schema contract.");
        }
        request.setBody(objectMapper.writeValueAsString(body));
        if (variant == DiagnosticVariant.STRICT_SCHEMA) {
            client.applyStrictSchemaForDiagnostic(request);
        }
        return request;
    }

    private static void setDiagnosticInstruction(ObjectNode body, String instruction) {
        ((ObjectNode) body.path("systemInstruction").path("parts").get(0)).put("text", instruction);
    }

    private void assertInvalid(String responseBody, String errorCode) {
        AiProviderReviewResult result = review(new AiHttpResponse(200, responseBody, Map.of()));

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.getErrorCode()).isEqualTo(errorCode);
    }

    private AiProviderReviewResult review(AiHttpResponse response) {
        return client(new CapturingTransport(response)).review(request(), 15_000L);
    }

    private GeminiExtractionDiagnostic extraction(AiHttpResponse response) throws Exception {
        GeminiProviderClient client = client(new CapturingTransport(response));
        return client.extractPayload(response)
                .geminiResponseShapeDiagnostic()
                .extractionDiagnostic();
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

    private enum DiagnosticVariant {
        PLAIN_TEXT,
        JSON_MIME_ONLY,
        STRICT_SCHEMA
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
