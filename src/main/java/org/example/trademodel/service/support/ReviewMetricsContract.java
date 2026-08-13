package org.example.trademodel.service.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Set;

/** Validates the versioned v4.1 review-metrics payload without inventing missing values. */
public final class ReviewMetricsContract {
    public static final String SCHEMA_VERSION = "FUNDAMENTAL_AI_V4_1_REVIEW_METRICS";

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> DATA_STATES = Set.of("READY", "INSUFFICIENT_DATA");
    private static final List<String> ROLES = List.of(
            "GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
    private static final List<String> PLAN_MODES = List.of(
            "CONFIRMATION", "REDUCED", "PREPARATION", "OBSERVATION");
    private static final List<String> OMISSION_OUTCOMES = List.of(
            "MISSED_VALID", "PUSHED_NOT_FILLED_VALID", "BLOCKED_BY_RISK_VALID");

    private ReviewMetricsContract() {
    }

    public static String normalizeOrThrow(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            JsonNode root = JSON.readTree(raw);
            if (root == null || !root.isObject()) {
                throw invalid("must be a JSON object");
            }
            requireText(root, "schemaVersion", SCHEMA_VERSION);
            String dataState = requireEnum(root, "dataState", DATA_STATES);
            boolean ready = "READY".equals(dataState);

            requireRate(root, "evidenceTraceabilityRate", ready);
            JsonNode completeness = requireObject(root, "structuredOutputCompletenessRate");
            requireRateMap(requireObject(completeness, "byRole"), ROLES, ready,
                    "structuredOutputCompletenessRate.byRole");
            requireRateMap(requireObject(completeness, "byPlanMode"), PLAN_MODES, ready,
                    "structuredOutputCompletenessRate.byPlanMode");
            requireRate(root, "unsupportedConclusionRate", ready);
            requireRate(root, "fabricatedFillRate", ready);
            requireRate(root, "confidenceCalibration", ready);
            requireCount(root, "falsePositiveCount", ready);
            requireCount(root, "falseNegativeCount", ready);
            requireCount(root, "missedValidOpportunityCount", ready);
            requireRateMap(requireObject(root, "planModeEffectiveness"), PLAN_MODES, ready,
                    "planModeEffectiveness");
            requireRate(root, "effectiveDowngradeRate", ready);
            requireRate(root, "failurePathHitRate", ready);
            requireCountMap(requireObject(root, "opportunityOmissionQuality"),
                    OMISSION_OUTCOMES, ready, "opportunityOmissionQuality");
            return JSON.writeValueAsString(root);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw invalid("must be valid JSON", exception);
        }
    }

    private static void requireText(JsonNode root, String field, String expected) {
        JsonNode value = required(root, field);
        if (!value.isTextual() || !expected.equals(value.asText())) {
            throw invalid(field + " must equal " + expected);
        }
    }

    private static String requireEnum(JsonNode root, String field, Set<String> values) {
        JsonNode value = required(root, field);
        if (!value.isTextual() || !values.contains(value.asText())) {
            throw invalid(field + " must be one of " + values);
        }
        return value.asText();
    }

    private static JsonNode requireObject(JsonNode root, String field) {
        JsonNode value = required(root, field);
        if (!value.isObject()) throw invalid(field + " must be a JSON object");
        return value;
    }

    private static void requireRateMap(JsonNode root,
                                       List<String> keys,
                                       boolean requiredValue,
                                       String path) {
        for (String key : keys) requireRate(root, key, requiredValue, path + "." + key);
    }

    private static void requireCountMap(JsonNode root,
                                        List<String> keys,
                                        boolean requiredValue,
                                        String path) {
        for (String key : keys) requireCount(root, key, requiredValue, path + "." + key);
    }

    private static void requireRate(JsonNode root, String field, boolean requiredValue) {
        requireRate(root, field, requiredValue, field);
    }

    private static void requireRate(JsonNode root,
                                    String field,
                                    boolean requiredValue,
                                    String path) {
        JsonNode value = required(root, field);
        if (value.isNull()) {
            if (requiredValue) throw invalid(path + " is required when dataState=READY");
            return;
        }
        if (!value.isNumber() || !Double.isFinite(value.asDouble())
                || value.asDouble() < 0.0d || value.asDouble() > 1.0d) {
            throw invalid(path + " must be null or a number in [0, 1]");
        }
    }

    private static void requireCount(JsonNode root, String field, boolean requiredValue) {
        requireCount(root, field, requiredValue, field);
    }

    private static void requireCount(JsonNode root,
                                     String field,
                                     boolean requiredValue,
                                     String path) {
        JsonNode value = required(root, field);
        if (value.isNull()) {
            if (requiredValue) throw invalid(path + " is required when dataState=READY");
            return;
        }
        if (!value.isIntegralNumber() || value.asLong() < 0L) {
            throw invalid(path + " must be null or a non-negative integer");
        }
    }

    private static JsonNode required(JsonNode root, String field) {
        if (!root.has(field)) throw invalid(field + " is required");
        return root.get(field);
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("metricsJson " + message);
    }

    private static IllegalArgumentException invalid(String message, Exception cause) {
        return new IllegalArgumentException("metricsJson " + message, cause);
    }
}
