package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AiProviderResponseParser {
    public static final Set<String> FORBIDDEN_FIELDS = Set.of(
            "finalDirection", "marketBias", "overrideDirection", "forcedDirection", "worthOpening",
            "assetState", "stateMachineState", "triggeredState", "entry", "entryZone", "stop",
            "stopLoss", "takeProfit", "tp", "riskReward", "rr", "leverage", "positionSize",
            "orderAction", "executionAction", "positionAction", "autoTradingAction", "pushAction",
            "providerPayload", "executablePayload"
    );

    private static final Set<String> ALLOWED_TOP_LEVEL_FIELDS = Set.of(
            "stance", "conflictLevel", "reasonCodes", "summary"
    );
    private static final List<String> FORBIDDEN_TEXT = List.of(
            "ignore previous instructions", "place order", "orderaction", "finaldirection",
            "override direction", "stop loss", "take profit", "risk reward"
    );

    private final ObjectMapper objectMapper;

    public AiProviderResponseParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AiProviderReviewResult parse(AiProviderName provider, AiProviderRole role, String content) {
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(provider);
        result.setRole(role);
        if (content == null || content.isBlank()) {
            return invalid(result, "INVALID_EMPTY_RESPONSE");
        }
        try {
            String json = unwrapJson(content);
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                return invalid(result, "INVALID_RESPONSE_NOT_OBJECT");
            }
            for (var fields = root.fieldNames(); fields.hasNext(); ) {
                String field = fields.next();
                if (!ALLOWED_TOP_LEVEL_FIELDS.contains(field)) {
                    return invalid(result, "INVALID_UNKNOWN_FIELD_" + safeCode(field));
                }
            }
            String forbiddenField = findForbiddenField(root);
            if (forbiddenField != null) {
                return invalid(result, "INVALID_FORBIDDEN_FIELD_" + safeCode(forbiddenField));
            }
            String textViolation = findForbiddenText(root.toString());
            if (textViolation != null) {
                return invalid(result, "INVALID_FORBIDDEN_TEXT");
            }

            AiReviewStance stance = enumValue(AiReviewStance.class, root.path("stance").asText(null));
            AiReviewConflictLevel conflictLevel = enumValue(AiReviewConflictLevel.class,
                    root.path("conflictLevel").asText(null));
            if (stance == null || conflictLevel == null) {
                return invalid(result, "INVALID_ENUM");
            }
            result.setCallStatus(AiProviderCallStatus.SUCCESS);
            result.setStance(stance);
            result.setConflictLevel(conflictLevel);
            result.setReasonCodes(reasonCodes(root.path("reasonCodes")));
            result.setSummary(summary(root.path("summary").asText("")));
            return result;
        } catch (Exception e) {
            return invalid(result, "INVALID_RESPONSE_PARSE");
        }
    }

    private static AiProviderReviewResult invalid(AiProviderReviewResult result, String code) {
        result.setCallStatus(AiProviderCallStatus.INVALID_RESPONSE);
        result.setFallback(true);
        result.setFallbackReason(code);
        result.setErrorCode(code);
        result.setStance(AiReviewStance.ABSTAIN);
        result.setConflictLevel(AiReviewConflictLevel.NONE);
        result.setReasonCodes(List.of(code));
        result.setSummary(code);
        return result;
    }

    private static String unwrapJson(String content) {
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int first = trimmed.indexOf('\n');
            int last = trimmed.lastIndexOf("```");
            if (first >= 0 && last > first) {
                return trimmed.substring(first + 1, last).trim();
            }
        }
        return trimmed;
    }

    private static String findForbiddenField(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isObject()) {
            for (var fields = node.fieldNames(); fields.hasNext(); ) {
                String field = fields.next();
                if (containsForbiddenField(field)) {
                    return field;
                }
                String nested = findForbiddenField(node.get(field));
                if (nested != null) {
                    return nested;
                }
            }
        } else if (node.isArray()) {
            for (JsonNode child : node) {
                String nested = findForbiddenField(child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static boolean containsForbiddenField(String field) {
        if (field == null) {
            return false;
        }
        String normalized = field.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        for (String forbidden : FORBIDDEN_FIELDS) {
            String f = forbidden.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
            if (normalized.equals(f)) {
                return true;
            }
        }
        return false;
    }

    private static String findForbiddenText(String text) {
        String lower = text == null ? "" : text.toLowerCase(Locale.ROOT);
        for (String forbidden : FORBIDDEN_TEXT) {
            if (lower.contains(forbidden)) {
                return forbidden;
            }
        }
        return null;
    }

    private static List<String> reasonCodes(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of("AI_REVIEW");
        }
        List<String> codes = new ArrayList<>();
        for (JsonNode child : node) {
            if (codes.size() >= 8) {
                break;
            }
            String code = safeCode(child.asText("AI_REVIEW"));
            if (!code.isBlank()) {
                codes.add(code.length() > 64 ? code.substring(0, 64) : code);
            }
        }
        return codes.isEmpty() ? List.of("AI_REVIEW") : codes;
    }

    private static String summary(String summary) {
        if (summary == null) {
            return "";
        }
        String sanitized = summary.replace('\n', ' ').replace('\r', ' ').trim();
        return sanitized.length() <= 512 ? sanitized : sanitized.substring(0, 512);
    }

    private static String safeCode(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^A-Za-z0-9_\\-]", "_").toUpperCase(Locale.ROOT);
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return null;
        }
    }
}
