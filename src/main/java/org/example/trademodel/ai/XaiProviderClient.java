package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class XaiProviderClient extends AbstractSafeAiProviderClient {
    private final AiOrchestratorProperties properties;

    public XaiProviderClient(AiOrchestratorProperties properties,
                             AiHttpTransport transport,
                             ObjectMapper objectMapper) {
        super(properties, transport, objectMapper);
        this.properties = properties;
    }

    @Override
    public AiProviderName provider() {
        return AiProviderName.XAI;
    }

    @Override
    public AiProviderRole role() {
        return AiProviderRole.GROK_ADVERSARIAL_CHALLENGE;
    }

    @Override
    public AiProviderProperties providerProperties() {
        return properties.getXai();
    }

    @Override
    protected AiHttpRequest buildHttpRequest(String promptJson, long timeoutOverrideMs) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", providerProperties().getModel());
        body.put("messages", List.of(
                Map.of("role", "system", "content", AiPromptBuilder.SYSTEM_INSTRUCTION),
                Map.of("role", "user", "content", promptJson)
        ));
        body.put("max_tokens", maxOutputTokens());
        body.put("temperature", 0);

        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(), "/v1/chat/completions"),
                json(body), timeoutOverrideMs);
        Map<String, String> headers = jsonHeaders();
        headers.put("Authorization", "Bearer " + providerProperties().getApiKey());
        request.setHeaders(headers);
        return request;
    }

    @Override
    protected ProviderPayload extractPayload(AiHttpResponse response) throws Exception {
        JsonNode root = readTree(response.getBody());
        String content = null;
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            content = text(choices.get(0).path("message"), "content");
        }
        JsonNode usage = root.path("usage");
        return new ProviderPayload(content, text(root, "id"),
                longValue(usage, "prompt_tokens"),
                longValue(usage, "completion_tokens"),
                longValue(usage, "total_tokens"));
    }
}
