package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

@Component
public class XaiProviderClient extends AbstractSafeAiProviderClient {
    private static final List<String> EVIDENCE_REFERENCE_FIELDS = List.of(
            "evidenceId", "source", "sourceReference", "sourceTraceId");
    private static final List<String> OUTPUT_REFERENCE_FIELDS = List.of(
            "evidenceRefs", "sourceRefs");
    private static final int GROK_REFERENCE_SCHEMA_COUNT = 5;

    private final AiOrchestratorProperties properties;
    private final ObjectMapper objectMapper;

    public XaiProviderClient(AiOrchestratorProperties properties,
                             AiHttpTransport transport,
                             ObjectMapper objectMapper) {
        super(properties, transport, objectMapper);
        this.properties = properties;
        this.objectMapper = objectMapper;
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
        body.put("max_output_tokens", properties.getBackgroundExecution().getStructuredMaxOutputTokens());
        body.put("reasoning", Map.of("effort", "low"));
        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "fundamental_ai_v41_grok_challenge");
        format.put("strict", true);
        format.put("schema", constrainedDecisionChainSchema(promptJson, role));
        body.put("text", Map.of("format", format));
        body.put("store", false);
        AiHttpRequest request = baseRequest(joinUrl(providerProperties().getBaseUrl(), "/v1/responses"),
                json(body), timeoutOverrideMs);
        Map<String, String> headers = jsonHeaders();
        headers.put("Authorization", "Bearer " + providerProperties().getApiKey());
        request.setHeaders(headers);
        return request;
    }

    private JsonNode constrainedDecisionChainSchema(String promptJson,
                                                    AiDecisionChainRole role) throws Exception {
        JsonNode schema = objectMapper.valueToTree(AiDecisionChainSchema.responseJsonSchema(role));
        if (role != AiDecisionChainRole.GROK_CHALLENGE) {
            return schema;
        }

        List<String> allowedReferences = allowedEvidenceReferences(promptJson);
        if (allowedReferences.isEmpty()) {
            throw new IllegalArgumentException("GROK_EVIDENCE_REFERENCES_REQUIRED");
        }
        ArrayNode allowedValues = objectMapper.valueToTree(allowedReferences);
        int constrainedSchemas = constrainReferenceItems(schema, allowedValues);
        if (constrainedSchemas != GROK_REFERENCE_SCHEMA_COUNT) {
            throw new IllegalStateException("GROK_REFERENCE_SCHEMA_INCOMPLETE");
        }
        return schema;
    }

    private List<String> allowedEvidenceReferences(String promptJson) throws Exception {
        JsonNode evidence = objectMapper.readTree(promptJson).path("input").path("evidence");
        TreeSet<String> references = new TreeSet<>();
        if (evidence.isArray()) {
            for (JsonNode row : evidence) {
                for (String field : EVIDENCE_REFERENCE_FIELDS) {
                    JsonNode value = row.path(field);
                    if (value.isTextual() && !value.asText().isBlank()) {
                        references.add(value.asText().trim());
                    }
                }
            }
        }
        return List.copyOf(references);
    }

    private static int constrainReferenceItems(JsonNode node, ArrayNode allowedValues) {
        if (node == null) {
            return 0;
        }
        int constrained = 0;
        if (node.isObject()) {
            JsonNode properties = node.path("properties");
            if (properties instanceof ObjectNode propertyObject) {
                for (String field : OUTPUT_REFERENCE_FIELDS) {
                    JsonNode items = propertyObject.path(field).path("items");
                    if (items instanceof ObjectNode itemSchema) {
                        itemSchema.set("enum", allowedValues.deepCopy());
                        constrained++;
                    }
                }
            }
            for (JsonNode child : node) {
                constrained += constrainReferenceItems(child, allowedValues);
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                constrained += constrainReferenceItems(child, allowedValues);
            }
        }
        return constrained;
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
