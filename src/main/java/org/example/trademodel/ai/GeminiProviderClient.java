package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class GeminiProviderClient extends AbstractSafeAiProviderClient {
    private static final String STRUCTURED_OUTPUT_INSTRUCTION = AiPromptBuilder.SYSTEM_INSTRUCTION + """

            For Gemini, return JSON only: no Markdown, no code fence, no prose, and no explanation.
            Return exactly these AI_ROLE_RESULTS_SCHEMA_V1 role-fragment fields and no others:
            stance, conflictLevel, reasonCodes, summary.
            """;

    private final AiOrchestratorProperties properties;
    private final ObjectMapper objectMapper;

    public GeminiProviderClient(AiOrchestratorProperties properties,
                                AiHttpTransport transport,
                                ObjectMapper objectMapper) {
        super(properties, transport, objectMapper);
        this.properties = properties;
        this.objectMapper = objectMapper;
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
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", STRUCTURED_OUTPUT_INSTRUCTION))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", promptJson))
        )));
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("maxOutputTokens", maxOutputTokens());
        generationConfig.put("temperature", 0);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseJsonSchema", responseJsonSchema());
        body.put("generationConfig", generationConfig);

        String model = URLEncoder.encode(selectedModel, StandardCharsets.UTF_8);
        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(),
                "/v1beta/models/" + model + ":generateContent"), json(body), timeoutOverrideMs);
        Map<String, String> headers = jsonHeaders();
        headers.put("x-goog-api-key", providerProperties().getApiKey());
        request.setHeaders(headers);
        return request;
    }

    @Override
    protected ProviderPayload extractPayload(AiHttpResponse response) throws Exception {
        JsonNode root = readTree(response.getBody());
        String content = null;
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && !parts.isEmpty()) {
                content = text(parts.get(0), "text");
            }
        }
        if (!blank(content) && content.contains("```")) {
            content = null;
        }
        JsonNode usage = root.path("usageMetadata");
        String requestId = text(root, "responseId");
        if (blank(requestId)) {
            requestId = response.firstHeader("x-request-id");
        }
        return new ProviderPayload(content, requestId,
                longValue(usage, "promptTokenCount"),
                longValue(usage, "candidatesTokenCount"),
                longValue(usage, "totalTokenCount"));
    }

    @Override
    protected void enrichParsedResult(AiProviderReviewResult result, ProviderPayload providerPayload) {
        if (result != null && result.getCallStatus() == AiProviderCallStatus.INVALID_RESPONSE) {
            result.setSchemaDiagnostic(AiProviderSchemaDiagnostic.analyze(
                    objectMapper, providerPayload == null ? null : providerPayload.content()));
        }
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
