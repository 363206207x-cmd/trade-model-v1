package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GeminiProviderClient extends AbstractSafeAiProviderClient {
    private static final String JSON_OUTPUT_INSTRUCTION = AiPromptBuilder.SYSTEM_INSTRUCTION + """

            You are GEMINI_REVIEW. Return ONLY one valid JSON object and nothing else.
            Do not return Markdown, a code fence, an explanation, prose, a refusal, a prefix, or a suffix.
            The JSON object must contain exactly these AI_ROLE_RESULTS_SCHEMA_V1 role-fragment fields and no others:
            stance, conflictLevel, reasonCodes, summary.
            Use only these values for stance: SUPPORT, CHALLENGE, ABSTAIN.
            Use only these values for conflictLevel: NONE, MINOR, MAJOR, EXTREME.
            reasonCodes must be an array of strings.
            summary must be a concise string no longer than 100 characters.
            Do not place any explanation outside the JSON object.
            Even when evidence is insufficient or a review conclusion cannot be formed, return exactly this valid JSON object:
            {"stance":"ABSTAIN","conflictLevel":"NONE","reasonCodes":["INSUFFICIENT_DATA"],"summary":"Insufficient evidence"}
            Never replace that JSON fallback with plain text.
            """;

    private final AiOrchestratorProperties properties;
    private final ObjectMapper objectMapper;
    private final GeminiRoleResultNormalizer roleResultNormalizer;

    public GeminiProviderClient(AiOrchestratorProperties properties,
                                AiHttpTransport transport,
                                ObjectMapper objectMapper) {
        super(properties, transport, objectMapper);
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.roleResultNormalizer = new GeminiRoleResultNormalizer(objectMapper);
    }

    @Override
    public AiProviderName provider() {
        return AiProviderName.GEMINI;
    }

    @Override
    public AiProviderRole role() {
        return AiProviderRole.GEMINI_CONSISTENCY_REVIEW;
    }

    @Override
    public AiProviderProperties providerProperties() {
        return properties.getGemini();
    }

    @Override
    protected AiHttpRequest buildHttpRequest(String promptJson, long timeoutOverrideMs,
                                             String selectedModel) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", JSON_OUTPUT_INSTRUCTION))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", promptJson))
        )));
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("maxOutputTokens", maxOutputTokens());
        generationConfig.put("temperature", 0);
        generationConfig.put("responseMimeType", "application/json");
        body.put("generationConfig", generationConfig);

        String model = URLEncoder.encode(selectedModel, StandardCharsets.UTF_8);
        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(),
                "/v1beta/models/" + model + ":generateContent"), json(body), timeoutOverrideMs);
        Map<String, String> headers = jsonHeaders();
        headers.put("x-goog-api-key", providerProperties().getApiKey());
        request.setHeaders(headers);
        return request;
    }

    AiHttpRequest buildControlledSmokeHttpRequest(AiProviderRequest request, long timeoutOverrideMs,
                                                   String selectedModel) throws Exception {
        AiPromptBuilder.PromptPayload prompt = new AiPromptBuilder(objectMapper, properties)
                .build(request, role());
        return buildHttpRequest(prompt.dataJson(), timeoutOverrideMs, selectedModel);
    }

    void applyStrictSchemaForDiagnostic(AiHttpRequest request) throws Exception {
        ObjectNode body = (ObjectNode) objectMapper.readTree(request.getBody());
        ObjectNode generationConfig = (ObjectNode) body.path("generationConfig");
        generationConfig.set("responseJsonSchema", objectMapper.valueToTree(responseJsonSchema()));
        request.setBody(objectMapper.writeValueAsString(body));
    }

    @Override
    protected ProviderPayload extractPayload(AiHttpResponse response) throws Exception {
        return extractPayload(response, true);
    }

    ProviderPayload extractDiagnosticPayload(AiHttpResponse response) throws Exception {
        return extractPayload(response, false);
    }

    private ProviderPayload extractPayload(AiHttpResponse response, boolean normalizeRoleResult) throws Exception {
        JsonNode root = readTree(response.getBody());
        String content = null;
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                content = text(parts.get(0), "text");
            }
        }
        String extractedContent = content;
        GeminiExtractionDiagnostic extractionDiagnostic =
                GeminiExtractionDiagnostic.analyze(objectMapper, root, extractedContent);
        GeminiResponseShapeDiagnostic responseShapeDiagnostic =
                GeminiResponseShapeDiagnostic.analyze(objectMapper, extractedContent, extractionDiagnostic);
        if (!blank(content) && content.contains("```")) {
            content = null;
        }
        if (normalizeRoleResult && !blank(content)) {
            content = roleResultNormalizer.normalize(content);
        }
        JsonNode usage = root.path("usageMetadata");
        String requestId = text(root, "responseId");
        if (blank(requestId)) {
            requestId = response.firstHeader("x-request-id");
        }
        return new ProviderPayload(content, requestId,
                longValue(usage, "promptTokenCount"),
                longValue(usage, "candidatesTokenCount"),
                longValue(usage, "totalTokenCount"),
                responseShapeDiagnostic);
    }

    @Override
    protected void enrichParsedResult(AiProviderReviewResult result, ProviderPayload providerPayload) {
        if (result != null && result.getCallStatus() == AiProviderCallStatus.INVALID_RESPONSE) {
            result.setSchemaDiagnostic(AiProviderSchemaDiagnostic.analyze(
                    objectMapper, providerPayload == null ? null : providerPayload.content()));
            result.setGeminiResponseShapeDiagnostic(providerPayload == null
                    ? null : providerPayload.geminiResponseShapeDiagnostic());
        }
    }

    @Override
    protected AiProviderReviewResult httpFailure(AiHttpResponse response, long latencyMs) {
        AiProviderErrorReason reason = classifyProviderError(response);
        AiProviderCallStatus status = reason == AiProviderErrorReason.GEMINI_RATE_LIMITED
                ? AiProviderCallStatus.RATE_LIMITED
                : AiProviderCallStatus.FAILED;
        return failure(status, reason.name(), latencyMs);
    }

    private AiProviderErrorReason classifyProviderError(AiHttpResponse response) {
        int statusCode = response == null ? 0 : response.getStatusCode();
        if (statusCode == 401 || statusCode == 403) {
            return AiProviderErrorReason.GEMINI_AUTH_REJECTED;
        }
        if (statusCode == 429) {
            return AiProviderErrorReason.GEMINI_RATE_LIMITED;
        }
        if (statusCode == 404) {
            return AiProviderErrorReason.GEMINI_MODEL_CAPABILITY_ERROR;
        }
        if (statusCode >= 500 && statusCode <= 599) {
            return AiProviderErrorReason.GEMINI_HTTP_5XX_INTERNAL;
        }
        if (statusCode == 400) {
            String diagnosticText = sanitizedDiagnosticText(response.getBody());
            if (isStructuredOutputUnsupported(diagnosticText)) {
                return AiProviderErrorReason.GEMINI_STRUCTURED_OUTPUT_UNSUPPORTED;
            }
            if (isModelCapabilityError(diagnosticText)) {
                return AiProviderErrorReason.GEMINI_MODEL_CAPABILITY_ERROR;
            }
            return AiProviderErrorReason.GEMINI_HTTP_400_INVALID_REQUEST;
        }
        return AiProviderErrorReason.GEMINI_UNKNOWN_PROVIDER_ERROR;
    }

    private String sanitizedDiagnosticText(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            return (error.path("status").asText("") + " " + error.path("message").asText(""))
                    .toLowerCase(Locale.ROOT);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static boolean isStructuredOutputUnsupported(String text) {
        boolean structuredMarker = text.contains("structured output")
                || text.contains("structured_output")
                || text.contains("responsejsonschema")
                || text.contains("response_json_schema")
                || text.contains("responseschema")
                || text.contains("response_schema");
        return structuredMarker && (text.contains("unsupported") || text.contains("not supported"));
    }

    private static boolean isModelCapabilityError(String text) {
        return text.contains("model")
                && (text.contains("unsupported") || text.contains("not supported")
                || text.contains("capability"));
    }

    private static Map<String, Object> responseJsonSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("stance", Map.of(
                "type", "string",
                "enum", List.of("SUPPORT", "CHALLENGE", "ABSTAIN")));
        properties.put("conflictLevel", Map.of(
                "type", "string",
                "enum", List.of("NONE", "MINOR", "MAJOR", "EXTREME")));
        properties.put("reasonCodes", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "maxItems", 8));
        properties.put("summary", Map.of("type", "string"));

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of("stance", "conflictLevel", "reasonCodes", "summary"));
        return schema;
    }
}
