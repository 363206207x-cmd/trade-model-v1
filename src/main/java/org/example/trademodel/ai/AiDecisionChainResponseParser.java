package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Set;

public class AiDecisionChainResponseParser {
    static final int MAX_RESPONSE_CHARS = 65_536;
    static final int MAX_TEXT_CHARS = 4_096;
    static final int MAX_REASON_COUNT = 8;

    private static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "finalExecutionPlan", "finalPlan", "opportunityState", "stateMachineState",
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
            Set<String> allowed = AiDecisionChainSchema.fields(role);
            for (var fields = root.fieldNames(); fields.hasNext(); ) {
                String field = fields.next();
                if (!allowed.contains(field)) return invalid(result, "INVALID_UNKNOWN_FIELD_" + code(field));
            }
            String forbidden = findForbidden(root);
            if (forbidden != null) return invalid(result, "INVALID_FORBIDDEN_FIELD_" + code(forbidden));
            for (String required : AiDecisionChainSchema.requiredFields(role)) {
                if (!root.has(required) || root.get(required).isNull()) {
                    return invalid(result, "INVALID_MISSING_FIELD_" + code(required));
                }
            }
            String valueError = validateValues(role, root);
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

    private static String validateValues(AiDecisionChainRole role, JsonNode root) {
        for (String field : AiDecisionChainSchema.fields(role)) {
            JsonNode value = root.get(field);
            if ("worthOpening".equals(field) || "majorCounterEvidence".equals(field)) {
                if (!value.isBoolean()) return "INVALID_FIELD_TYPE_" + code(field);
            } else if ("reasons".equals(field)) {
                if (!value.isArray()) return "INVALID_FIELD_TYPE_" + code(field);
                if (value.size() > MAX_REASON_COUNT) return "INVALID_FIELD_SIZE_" + code(field);
                for (JsonNode reason : value) {
                    if (!reason.isTextual() || reason.asText().isBlank()) {
                        return "INVALID_FIELD_VALUE_" + code(field);
                    }
                    if (reason.asText().length() > MAX_TEXT_CHARS) {
                        return "INVALID_FIELD_SIZE_" + code(field);
                    }
                }
            } else if (!value.isTextual()) {
                return "INVALID_FIELD_TYPE_" + code(field);
            } else if (value.asText().isBlank()) {
                return "INVALID_FIELD_VALUE_" + code(field);
            } else if (value.asText().length() > MAX_TEXT_CHARS) {
                return "INVALID_FIELD_SIZE_" + code(field);
            } else {
                Set<String> allowed = AiDecisionChainSchema.allowedValues(role, field);
                if (!allowed.isEmpty() && !allowed.contains(value.asText().trim().toUpperCase(Locale.ROOT))) {
                    return "INVALID_FIELD_VALUE_" + code(field);
                }
            }
        }
        return null;
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
