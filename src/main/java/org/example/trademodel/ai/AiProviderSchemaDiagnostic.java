package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record AiProviderSchemaDiagnostic(
        List<String> expectedFields,
        List<String> actualFields,
        List<String> missingFields,
        List<String> unexpectedFields,
        List<String> typeMismatchFields) {

    private static final Map<String, String> EXPECTED_TYPES = expectedTypes();
    private static final int MAX_ACTUAL_FIELDS = 16;
    private static final int MAX_FIELD_NAME_LENGTH = 64;

    public AiProviderSchemaDiagnostic {
        expectedFields = immutable(expectedFields);
        actualFields = immutable(actualFields);
        missingFields = immutable(missingFields);
        unexpectedFields = immutable(unexpectedFields);
        typeMismatchFields = immutable(typeMismatchFields);
    }

    public static AiProviderSchemaDiagnostic analyze(ObjectMapper objectMapper, String content) {
        List<String> expected = List.copyOf(EXPECTED_TYPES.keySet());
        if (content == null || content.isBlank()) {
            return new AiProviderSchemaDiagnostic(expected, List.of(), expected, List.of(), List.of());
        }
        try {
            JsonNode root = objectMapper.readTree(content);
            if (root == null || !root.isObject()) {
                return new AiProviderSchemaDiagnostic(expected, List.of(), expected, List.of(), List.of());
            }

            List<String> actual = new ArrayList<>();
            List<String> unexpected = new ArrayList<>();
            var fieldNames = root.fieldNames();
            while (fieldNames.hasNext() && actual.size() < MAX_ACTUAL_FIELDS) {
                String rawName = fieldNames.next();
                String safeName = safeFieldName(rawName);
                actual.add(safeName);
                if (!EXPECTED_TYPES.containsKey(rawName)) {
                    unexpected.add(safeName);
                }
            }

            List<String> missing = new ArrayList<>();
            List<String> mismatches = new ArrayList<>();
            for (Map.Entry<String, String> expectedType : EXPECTED_TYPES.entrySet()) {
                JsonNode value = root.get(expectedType.getKey());
                if (value == null) {
                    missing.add(expectedType.getKey());
                } else if (!matches(value, expectedType.getValue())) {
                    mismatches.add(expectedType.getKey() + " expected " + expectedType.getValue()
                            + " got " + actualType(value));
                }
            }
            return new AiProviderSchemaDiagnostic(expected, actual, missing, unexpected, mismatches);
        } catch (Exception ignored) {
            return new AiProviderSchemaDiagnostic(expected, List.of(), expected, List.of(), List.of());
        }
    }

    private static boolean matches(JsonNode value, String expectedType) {
        return switch (expectedType) {
            case "STRING" -> value.isTextual();
            case "ARRAY" -> value.isArray();
            default -> false;
        };
    }

    private static String actualType(JsonNode value) {
        if (value == null) {
            return "MISSING";
        }
        return value.getNodeType().name().toUpperCase(Locale.ROOT);
    }

    private static String safeFieldName(String fieldName) {
        String safe = fieldName == null ? "" : fieldName.replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (safe.length() > MAX_FIELD_NAME_LENGTH) {
            return safe.substring(0, MAX_FIELD_NAME_LENGTH);
        }
        return safe;
    }

    private static Map<String, String> expectedTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("stance", "STRING");
        types.put("conflictLevel", "STRING");
        types.put("reasonCodes", "ARRAY");
        types.put("summary", "STRING");
        return Collections.unmodifiableMap(types);
    }

    private static List<String> immutable(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
