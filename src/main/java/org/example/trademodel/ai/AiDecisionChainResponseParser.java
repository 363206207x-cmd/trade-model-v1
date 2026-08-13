package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AiDecisionChainResponseParser {
    static final int MAX_RESPONSE_CHARS = 65_536;
    static final int MAX_TEXT_CHARS = 4_096;
    static final int MAX_REASON_COUNT = 8;

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "finalExecutionPlan", "finalPlan", "stateMachineState",
            "userPosition", "order", "orderAction", "autoTrade", "autoClose", "autoReverse");

    private final ObjectMapper objectMapper;

    public AiDecisionChainResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiDecisionChainResult parse(AiProviderName provider, AiDecisionChainRole role, String content) {
        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setProvider(provider);
        result.setRole(role);
        result.setAuditOutput(boundedAuditOutput(content));
        if (content == null || content.isBlank()) {
            return invalid(result, "INVALID_EMPTY_RESPONSE");
        }
        if (content.length() > MAX_RESPONSE_CHARS) {
            return invalid(result, "INVALID_RESPONSE_TOO_LARGE");
        }
        try {
            JsonNode root = objectMapper.readTree(unwrapJson(content));
            if (root == null || !root.isObject()) return invalid(result, "INVALID_RESPONSE_NOT_OBJECT");
            String forbidden = findForbidden(root);
            if (forbidden != null) return invalid(result, "INVALID_FORBIDDEN_FIELD_" + code(forbidden));
            String valueError = validateSchema(root, AiDecisionChainSchema.responseJsonSchema(role), "");
            if (valueError == null) valueError = validateCollectionStates(role, root);
            if (valueError != null) return invalid(result, valueError);
            result.setCallStatus(AiProviderCallStatus.SUCCESS);
            String payloadJson = objectMapper.writeValueAsString(root);
            result.setPayloadJson(payloadJson);
            result.setAuditOutput(payloadJson);
            return result;
        } catch (Exception exception) {
            return invalid(result, "INVALID_RESPONSE_PARSE");
        }
    }

    @SuppressWarnings("unchecked")
    private static String validateSchema(JsonNode value, Map<String, Object> schema, String path) {
        String type = String.valueOf(schema.get("type"));
        String fieldCode = code(path.isBlank() ? "ROOT" : path);
        if ("object".equals(type)) {
            if (!value.isObject()) return "INVALID_FIELD_TYPE_" + fieldCode;
            Map<String, Object> properties = (Map<String, Object>) schema.getOrDefault("properties", Map.of());
            if (Boolean.FALSE.equals(schema.get("additionalProperties"))) {
                for (var fields = value.fieldNames(); fields.hasNext(); ) {
                    String field = fields.next();
                    if (!properties.containsKey(field)) return "INVALID_UNKNOWN_FIELD_" + code(join(path, field));
                }
            }
            List<String> required = (List<String>) schema.getOrDefault("required", List.of());
            for (String requiredField : required) {
                if (!value.has(requiredField) || value.get(requiredField).isNull()) {
                    return "INVALID_MISSING_FIELD_" + code(join(path, requiredField));
                }
            }
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                if (value.has(entry.getKey()) && !value.get(entry.getKey()).isNull()) {
                    String nested = validateSchema(value.get(entry.getKey()),
                            (Map<String, Object>) entry.getValue(), join(path, entry.getKey()));
                    if (nested != null) return nested;
                }
            }
            return null;
        }
        if ("array".equals(type)) {
            if (!value.isArray()) return "INVALID_FIELD_TYPE_" + fieldCode;
            int maxItems = ((Number) schema.getOrDefault("maxItems", MAX_REASON_COUNT)).intValue();
            int minItems = ((Number) schema.getOrDefault("minItems", 0)).intValue();
            if (value.size() < minItems) return "INVALID_FIELD_SIZE_" + fieldCode;
            if (value.size() > maxItems) return "INVALID_FIELD_SIZE_" + fieldCode;
            Map<String, Object> items = (Map<String, Object>) schema.get("items");
            for (int index = 0; index < value.size(); index++) {
                String nested = validateSchema(value.get(index), items, path + "[" + index + "]");
                if (nested != null) return nested;
            }
            return null;
        }
        if ("string".equals(type)) {
            if (!value.isTextual()) return "INVALID_FIELD_TYPE_" + fieldCode;
            String text = value.asText();
            if (text.isBlank()) return "INVALID_FIELD_VALUE_" + fieldCode;
            int maxLength = ((Number) schema.getOrDefault("maxLength", MAX_TEXT_CHARS)).intValue();
            if (text.length() > maxLength) return "INVALID_FIELD_SIZE_" + fieldCode;
            if (schema.get("enum") instanceof List<?> allowed
                    && allowed.stream().map(String::valueOf).noneMatch(item -> item.equalsIgnoreCase(text.trim()))) {
                return "INVALID_FIELD_VALUE_" + fieldCode;
            }
            return null;
        }
        if ("boolean".equals(type)) {
            return value.isBoolean() ? null : "INVALID_FIELD_TYPE_" + fieldCode;
        }
        if ("number".equals(type)) {
            if (!value.isNumber()) return "INVALID_FIELD_TYPE_" + fieldCode;
            double number = value.asDouble();
            if (schema.get("minimum") instanceof Number minimum && number < minimum.doubleValue()) {
                return "INVALID_FIELD_VALUE_" + fieldCode;
            }
            if (schema.get("maximum") instanceof Number maximum && number > maximum.doubleValue()) {
                return "INVALID_FIELD_VALUE_" + fieldCode;
            }
            return null;
        }
        return "INVALID_SCHEMA_TYPE_" + fieldCode;
    }

    private static String validateCollectionStates(AiDecisionChainRole role, JsonNode root) {
        return switch (role) {
            case GPT_FINAL -> firstError(
                    collectionState(root, "supportingEvidenceState", "supportingEvidence", false),
                    collectionState(root, "opposingEvidenceState", "opposingEvidence", false));
            case GEMINI_REVIEW -> firstError(
                    collectionState(root, "evidenceGapsState", "evidenceGaps", false),
                    collectionState(root, "logicConflictsState", "logicConflicts", false),
                    collectionState(root, "underestimatedRisksState", "underestimatedRisks", false));
            case GROK_CHALLENGE -> firstError(
                    collectionState(root, "failurePathState", "failurePaths", true),
                    collectionState(root, "opposingScenariosState", "opposingScenarios", false),
                    collectionState(root, "externalEventRisksState", "externalEventRisks", false),
                    collectionState(root, "microstructureRisksState", "microstructureRisks", false),
                    collectionState(root, "watchIndicatorsState", "watchIndicators", false));
        };
    }

    private static String collectionState(JsonNode root, String stateField, String collectionField,
                                          boolean grokFailurePath) {
        String state = root.path(stateField).asText("").trim().toUpperCase(Locale.ROOT);
        JsonNode collection = root.path(collectionField);
        if ("FOUND".equals(state) && collection.isEmpty()) {
            return "INVALID_COLLECTION_STATE_" + code(collectionField);
        }
        if (!"FOUND".equals(state) && !collection.isEmpty()) {
            return "INVALID_COLLECTION_STATE_" + code(collectionField);
        }
        if ("NO_VERIFIABLE_FAILURE_PATH".equals(state) && !grokFailurePath) {
            return "INVALID_COLLECTION_STATE_" + code(stateField);
        }
        return null;
    }

    private static String firstError(String... errors) {
        for (String error : errors) if (error != null) return error;
        return null;
    }

    private static String join(String path, String field) {
        return path == null || path.isBlank() ? field : path + "." + field;
    }

    private static String findForbidden(JsonNode node) {
        if (node == null) return null;
        if (node.isObject()) {
            for (var fields = node.fieldNames(); fields.hasNext(); ) {
                String field = fields.next();
                if (FORBIDDEN_FIELDS.stream().anyMatch(value -> normalize(value).equals(normalize(field)))) {
                    return field;
                }
                String nested = findForbidden(node.get(field));
                if (nested != null) return nested;
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                String nested = findForbidden(child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    private static AiDecisionChainResult invalid(AiDecisionChainResult result, String reason) {
        result.setCallStatus(AiProviderCallStatus.INVALID_RESPONSE);
        result.setFallback(true);
        result.setFallbackReason(reason);
        result.setErrorCode(reason);
        return result;
    }

    private static String unwrapJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int first = trimmed.indexOf('\n');
            int last = trimmed.lastIndexOf("```");
            if (first >= 0 && last > first) return trimmed.substring(first + 1, last).trim();
        }
        return trimmed;
    }

    private static String boundedAuditOutput(String content) {
        if (content == null) return null;
        return content.length() <= MAX_RESPONSE_CHARS
                ? content : content.substring(0, MAX_RESPONSE_CHARS);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private static String code(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9_\\-]", "_").toUpperCase(Locale.ROOT);
    }
}
