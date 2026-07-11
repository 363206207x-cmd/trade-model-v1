package org.example.trademodel.ai;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Field-name, path, and type evidence only; provider values are never retained. */
public record GeminiResponseShapeDiagnostic(
        List<String> topLevelFields,
        List<String> nestedObjectPaths,
        List<String> fieldTypes,
        List<String> expectedFields,
        List<String> actualFields,
        List<String> missingFields,
        List<String> unexpectedFields,
        List<String> typeMismatchFields) {

    private static final Map<String, String> EXPECTED_TYPES = expectedTypes();
    private static final Set<String> SUPPORTED_WRAPPERS = Set.of("result", "analysis");
    private static final int MAX_FIELDS = 32;
    private static final int MAX_DEPTH = 4;
    private static final int MAX_PATH_LENGTH = 160;

    public GeminiResponseShapeDiagnostic {
        topLevelFields = immutable(topLevelFields);
        nestedObjectPaths = immutable(nestedObjectPaths);
        fieldTypes = immutable(fieldTypes);
        expectedFields = immutable(expectedFields);
        actualFields = immutable(actualFields);
        missingFields = immutable(missingFields);
        unexpectedFields = immutable(unexpectedFields);
        typeMismatchFields = immutable(typeMismatchFields);
    }

    public static GeminiResponseShapeDiagnostic analyze(ObjectMapper objectMapper, String content) {
        List<String> expected = List.copyOf(EXPECTED_TYPES.keySet());
        JsonNode root = parseSingleJsonValue(objectMapper, content);
        if (root == null || !root.isObject()) {
            return new GeminiResponseShapeDiagnostic(
                    List.of(), List.of(), List.of(), expected, List.of(), expected, List.of(), List.of());
        }

        List<String> topLevel = fieldNames(root);
        List<String> nestedPaths = new ArrayList<>();
        List<String> fieldTypes = new ArrayList<>();
        collectShape(root, "", 0, nestedPaths, fieldTypes);

        JsonNode candidate = supportedCandidate(root);
        List<String> actual = candidate != null && candidate.isObject()
                ? fieldNames(candidate) : List.of();
        List<String> missing = new ArrayList<>();
        List<String> unexpected = new ArrayList<>();
        List<String> mismatches = new ArrayList<>();
        if (candidate == null || !candidate.isObject()) {
            missing.addAll(expected);
        } else {
            var candidateFields = candidate.fieldNames();
            while (candidateFields.hasNext()) {
                String rawName = candidateFields.next();
                if (!EXPECTED_TYPES.containsKey(rawName)) {
                    unexpected.add(safeName(rawName));
                }
            }
            for (Map.Entry<String, String> entry : EXPECTED_TYPES.entrySet()) {
                JsonNode value = candidate.get(entry.getKey());
                if (value == null) {
                    missing.add(entry.getKey());
                } else if (!matches(value, entry.getValue())) {
                    mismatches.add(entry.getKey() + ":" + entry.getValue()
                            + "->" + actualType(value));
                }
            }
        }
        return new GeminiResponseShapeDiagnostic(
                topLevel, nestedPaths, fieldTypes, expected, actual, missing, unexpected, mismatches);
    }

    private static JsonNode parseSingleJsonValue(ObjectMapper objectMapper, String content) {
        if (content == null || content.isBlank() || content.contains("```")) {
            return null;
        }
        try (JsonParser parser = objectMapper.getFactory().createParser(content.trim())) {
            JsonNode root = objectMapper.readTree(parser);
            return root != null && parser.nextToken() == null ? root : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static JsonNode supportedCandidate(JsonNode root) {
        if (root.size() != 1) {
            return root;
        }
        String onlyField = root.fieldNames().next();
        return SUPPORTED_WRAPPERS.contains(onlyField) ? root.get(onlyField) : root;
    }

    private static void collectShape(
            JsonNode node, String prefix, int depth,
            List<String> nestedPaths, List<String> fieldTypes) {
        if (!node.isObject() || depth >= MAX_DEPTH || fieldTypes.size() >= MAX_FIELDS) {
            return;
        }
        var fields = node.fields();
        while (fields.hasNext() && fieldTypes.size() < MAX_FIELDS) {
            var field = fields.next();
            String name = safeName(field.getKey());
            String path = prefix.isEmpty() ? name : prefix + "." + name;
            if (path.length() > MAX_PATH_LENGTH) {
                path = path.substring(0, MAX_PATH_LENGTH);
            }
            JsonNode value = field.getValue();
            fieldTypes.add(path + ":" + actualType(value));
            if (!prefix.isEmpty() && nestedPaths.size() < MAX_FIELDS) {
                nestedPaths.add(path);
            }
            if (value != null && value.isObject()) {
                collectShape(value, path, depth + 1, nestedPaths, fieldTypes);
            }
        }
    }

    private static List<String> fieldNames(JsonNode object) {
        List<String> names = new ArrayList<>();
        var fields = object.fieldNames();
        while (fields.hasNext() && names.size() < MAX_FIELDS) {
            names.add(safeName(fields.next()));
        }
        return names;
    }

    private static boolean matches(JsonNode value, String expectedType) {
        return switch (expectedType) {
            case "string" -> value.isTextual();
            case "array" -> value.isArray();
            default -> false;
        };
    }

    private static String actualType(JsonNode value) {
        if (value == null) {
            return "missing";
        }
        return value.getNodeType().name().toLowerCase(Locale.ROOT);
    }

    private static String safeName(String value) {
        String safe = value == null ? "" : value.replaceAll("[^A-Za-z0-9_\\-]", "_");
        return safe.length() <= 64 ? safe : safe.substring(0, 64);
    }

    private static Map<String, String> expectedTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("stance", "string");
        types.put("conflictLevel", "string");
        types.put("reasonCodes", "array");
        types.put("summary", "string");
        return Collections.unmodifiableMap(types);
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
