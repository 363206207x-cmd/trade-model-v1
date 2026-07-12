package org.example.trademodel.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Set;

/** Normalizes only deterministic Gemini role-result shapes before strict V1 validation. */
public final class GeminiRoleResultNormalizer {
    private static final Set<String> WRAPPER_FIELDS = Set.of("result", "analysis");
    private static final Map<String, String> FIELD_NAMES = Map.of(
            "stance", "stance",
            "conflictLevel", "conflictLevel",
            "conflict_level", "conflictLevel",
            "reasonCodes", "reasonCodes",
            "reason_codes", "reasonCodes",
            "summary", "summary"
    );

    private final ObjectMapper objectMapper;

    public GeminiRoleResultNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String normalize(String content) {
        if (content == null || content.isBlank() || content.contains("```")) {
            return null;
        }
        JsonNode root = parseSingleJsonValue(content);
        if (root == null) {
            return null;
        }
        if (!root.isObject()) {
            return write(root);
        }

        JsonNode source = roleFragment(root);
        if (source == null || !source.isObject()) {
            return write(root);
        }
        return normalizeFieldNames(source);
    }

    private JsonNode parseSingleJsonValue(String content) {
        try (JsonParser parser = objectMapper.getFactory().createParser(content.trim())) {
            JsonNode root = objectMapper.readTree(parser);
            return root != null && parser.nextToken() == null ? root : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonNode roleFragment(JsonNode root) {
        if (root.size() != 1) {
            return root;
        }
        String onlyField = root.fieldNames().next();
        return WRAPPER_FIELDS.contains(onlyField) ? root.get(onlyField) : root;
    }

    private String normalizeFieldNames(JsonNode source) {
        ObjectNode normalized = objectMapper.createObjectNode();
        for (var fields = source.fields(); fields.hasNext(); ) {
            var field = fields.next();
            String canonicalName = FIELD_NAMES.get(field.getKey());
            if (canonicalName == null || normalized.has(canonicalName)) {
                return write(source);
            }
            normalized.set(canonicalName, field.getValue().deepCopy());
        }
        return write(normalized);
    }

    private String write(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception ignored) {
            return null;
        }
    }
}
