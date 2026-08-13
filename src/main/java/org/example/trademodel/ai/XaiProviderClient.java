package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
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
    protected AiHttpRequest buildHttpRequest(String promptJson, long timeoutOverrideMs,
                                             String selectedModel) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", selectedModel);
        body.put("instructions", AiPromptBuilder.SYSTEM_INSTRUCTION);
        body.put("input", promptJson);
        body.put("max_output_tokens", maxOutputTokens());
        body.put("reasoning", Map.of("effort", "low"));
        body.put("store", false);

        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(), "/v1/responses"),
                json(body), timeoutOverrideMs);
        Map<String, String> headers = jsonHeaders();
        headers.put("Authorization", "Bearer " + providerProperties().getApiKey());
        request.setHeaders(headers);
        return request;
    }

    @Override
    protected AiHttpRequest buildDecisionChainHttpRequest(String promptJson,
                                                          AiDecisionChainRole role,
                                                          long timeoutOverrideMs,
                                                          String selectedModel) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", selectedModel);
        body.put("instructions", AiDecisionChainPromptBuilder.systemInstruction(role));
        body.put("input", promptJson);
        body.put("max_output_tokens", maxOutputTokens());
        body.put("reasoning", Map.of("effort", "low"));
        body.put("store", false);
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
                    if (!contentNodes.isArray()) {
                        continue;
                    }
                    for (JsonNode contentNode : contentNodes) {
                        content = text(contentNode, "text");
                        if (!blank(content)) {
                            break;
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
                longValue(usage, "total_tokens"),
                null,
                null);
    }
}
