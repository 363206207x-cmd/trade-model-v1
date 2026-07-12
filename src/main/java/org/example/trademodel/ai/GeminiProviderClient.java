package org.example.trademodel.ai;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class GeminiProviderClient extends AbstractSafeAiProviderClient {
    static final int INTERACTIONS_MAX_OUTPUT_TOKENS = 256;
    private static final String COMPLETED_STATUS = "completed";
    private static final String JSON_OUTPUT_INSTRUCTION = AiPromptBuilder.SYSTEM_INSTRUCTION + """

            You are GEMINI_REVIEW. Return ONLY one valid JSON object and nothing else.
            Do not return Markdown, a code fence, an explanation, prose, a refusal, a prefix, or a suffix.
            The JSON object must contain exactly these AI_ROLE_RESULTS_SCHEMA_V1 role-fragment fields and no others:
            stance, conflictLevel, reasonCodes, summary.
            Use only these values for stance: SUPPORT, CHALLENGE, ABSTAIN.
            Use only these values for conflictLevel: NONE, MINOR, MAJOR, EXTREME.
            reasonCodes must be an array of at most 8 strings.
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
    protected boolean validModelSelection(AiProviderProperties providerProperties) {
        try {
            canonicalModelName(providerProperties == null ? null : providerProperties.getEffectiveModel());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    protected String effectiveModelForReadiness(AiProviderProperties providerProperties) {
        try {
            return canonicalModelName(providerProperties == null ? null : providerProperties.getEffectiveModel());
        } catch (IllegalArgumentException exception) {
            return providerProperties == null ? null : providerProperties.getEffectiveModel();
        }
    }

    @Override
    protected AiHttpRequest buildHttpRequest(String promptJson, long timeoutOverrideMs,
                                             String selectedModel) throws Exception {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("max_output_tokens", INTERACTIONS_MAX_OUTPUT_TOKENS);
        generationConfig.put("temperature", 0);
        generationConfig.put("seed", 42);
        generationConfig.put("thinking_level", "low");
        generationConfig.put("thinking_summaries", "none");

        Map<String, Object> responseFormat = new LinkedHashMap<>();
        responseFormat.put("type", "text");
        responseFormat.put("mime_type", "application/json");
        responseFormat.put("schema", responseJsonSchema());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", canonicalModelName(selectedModel));
        body.put("store", false);
        body.put("stream", false);
        body.put("system_instruction", JSON_OUTPUT_INSTRUCTION);
        body.put("input", promptJson);
        body.put("generation_config", generationConfig);
        body.put("response_format", responseFormat);

        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(),
                "/v1/interactions"), json(body), timeoutOverrideMs);
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

    @Override
    protected ProviderPayload extractPayload(AiHttpResponse response) throws Exception {
        JsonNode root = readTree(response.getBody());
        String status = text(root, "status");
        if (!COMPLETED_STATUS.equalsIgnoreCase(status)) {
            throw contractFailure("GEMINI_INTERACTION_NOT_COMPLETED");
        }

        JsonNode finalModelOutput = null;
        JsonNode steps = root.path("steps");
        if (steps.isArray()) {
            for (JsonNode step : steps) {
                if (step.isObject() && "model_output".equals(text(step, "type"))) {
                    finalModelOutput = step;
                }
            }
        }
        if (finalModelOutput == null) {
            throw contractFailure("GEMINI_INTERACTION_MODEL_OUTPUT_MISSING");
        }

        StringBuilder finalText = new StringBuilder();
        JsonNode contentItems = finalModelOutput.path("content");
        if (contentItems.isArray()) {
            for (JsonNode item : contentItems) {
                String itemText = text(item, "text");
                if (item.isObject() && "text".equals(text(item, "type")) && !blank(itemText)) {
                    finalText.append(itemText);
                }
            }
        }
        String extractedContent = finalText.toString().trim();
        if (blank(extractedContent)) {
            throw contractFailure("GEMINI_INTERACTION_FINAL_TEXT_MISSING");
        }

        GeminiResponseShapeDiagnostic responseShapeDiagnostic =
                GeminiResponseShapeDiagnostic.analyze(objectMapper, extractedContent, null);
        String normalizedContent = roleResultNormalizer.normalize(extractedContent);
        JsonNode usage = root.path("usage");
        return new ProviderPayload(normalizedContent, text(root, "id"),
                longValue(usage, "total_input_tokens"),
                longValue(usage, "total_output_tokens"),
                longValue(usage, "total_tokens"),
                responseShapeDiagnostic);
    }

    @Override
    protected void enrichParsedResult(AiProviderReviewResult result, ProviderPayload providerPayload) {
        if (result == null) {
            return;
        }
        if (result.getCallStatus() == AiProviderCallStatus.INVALID_RESPONSE) {
            result.setSchemaDiagnostic(AiProviderSchemaDiagnostic.analyze(
                    objectMapper, providerPayload == null ? null : providerPayload.content()));
            result.setGeminiResponseShapeDiagnostic(providerPayload == null
                    ? null : providerPayload.geminiResponseShapeDiagnostic());
        }
        if (result.successful() && (providerPayload == null || blank(providerPayload.providerRequestId()))) {
            result.setReasonCodes(appendReason(result.getReasonCodes(), "GEMINI_INTERACTION_ID_MISSING"));
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

    static String canonicalModelName(String selectedModel) {
        String model = selectedModel == null ? "" : selectedModel.trim();
        if (model.startsWith("models/") && model.length() > "models/".length()) {
            return model;
        }
        if (!model.isBlank() && !model.contains("/")) {
            return "models/" + model;
        }
        throw new IllegalArgumentException("GEMINI_MODEL_NAME_INVALID");
    }

    static Map<String, Object> responseJsonSchema() {
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
                || text.contains("response format")
                || text.contains("response_format");
        return structuredMarker && (text.contains("unsupported") || text.contains("not supported"));
    }

    private static boolean isModelCapabilityError(String text) {
        return text.contains("model")
                && (text.contains("unsupported") || text.contains("not supported")
                || text.contains("capability"));
    }

    private static JsonProcessingException contractFailure(String code) {
        return new JsonParseException(null, code);
    }
}
