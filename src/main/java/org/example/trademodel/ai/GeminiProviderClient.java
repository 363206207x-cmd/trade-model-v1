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
    private final AiOrchestratorProperties properties;

    public GeminiProviderClient(AiOrchestratorProperties properties,
                                AiHttpTransport transport,
                                ObjectMapper objectMapper) {
        super(properties, transport, objectMapper);
        this.properties = properties;
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
    protected AiHttpRequest buildHttpRequest(String promptJson, long timeoutOverrideMs) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", AiPromptBuilder.SYSTEM_INSTRUCTION))));
        body.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", promptJson))
        )));
        body.put("generationConfig", Map.of("maxOutputTokens", maxOutputTokens(), "temperature", 0));

        String model = URLEncoder.encode(providerProperties().getModel(), StandardCharsets.UTF_8);
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
}
