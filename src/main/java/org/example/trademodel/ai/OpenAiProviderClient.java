package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiProviderClient extends AbstractSafeAiProviderClient {
    private final AiOrchestratorProperties properties;

    public OpenAiProviderClient(AiOrchestratorProperties properties,
                                AiHttpTransport transport,
                                ObjectMapper objectMapper) {
        super(properties, transport, objectMapper);
        this.properties = properties;
    }

    @Override
    public AiProviderName provider() {
        return AiProviderName.OPENAI;
    }

    @Override
    public AiProviderRole role() {
        return AiProviderRole.GPT_RULE_REVIEW;
    }

    @Override
    public AiProviderProperties providerProperties() {
        return properties.getOpenai();
    }

    @Override
    protected AiHttpRequest buildHttpRequest(String promptJson, long timeoutOverrideMs) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", providerProperties().getModel());
        body.put("instructions", AiPromptBuilder.SYSTEM_INSTRUCTION);
        body.put("input", List.of(Map.of("role", "user", "content", promptJson)));
        body.put("max_output_tokens", maxOutputTokens());
        body.put("temperature", 0);

        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(), "/v1/responses"),
                json(body), timeoutOverrideMs);
        Map<String, String> headers = jsonHeaders();
        headers.put("Authorization", "Bearer " + providerProperties().getApiKey());
        request.setHeaders(headers);
        return request;
    }

    @Override
    protected ProviderPayload extractPayload(AiHttpResponse response) throws Exception {
        JsonNode root = readTree(response.getBody());
        String content = text(root, "output_text");
        if (blank(content)) {
            JsonNode output = root.path("output");
            if (output.isArray()) {
                for (JsonNode item : output) {
                    JsonNode contentNodes = item.path("content");
                    if (contentNodes.isArray()) {
                        for (JsonNode contentNode : contentNodes) {
                            content = text(contentNode, "text");
                            if (!blank(content)) {
                                break;
                            }
                        }
                    }
                    if (!blank(content)) {
                        break;
                    }
                }
            }
        }
        JsonNode usage = root.path("usage");
        String requestId = response.firstHeader("x-request-id");
        if (blank(requestId)) {
            requestId = text(root, "id");
        }
        return new ProviderPayload(content, requestId,
                longValue(usage, "input_tokens"),
                longValue(usage, "output_tokens"),
                longValue(usage, "total_tokens"));
    }
}
