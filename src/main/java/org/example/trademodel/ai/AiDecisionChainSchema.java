package org.example.trademodel.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AiDecisionChainSchema {
    private AiDecisionChainSchema() {
    }

    public static Set<String> fields(AiDecisionChainRole role) {
        return switch (role) {
            case GPT_FINAL -> Set.of("direction", "planMode", "confidence", "riskLevel", "worthOpening",
                    "recommendedAction", "entryZone", "stopLoss", "takeProfitRules", "leverageSuggestion",
                    "positionSuggestion", "invalidCondition", "validity", "summary");
            case GEMINI_REVIEW -> Set.of("verdict", "conflictLevel", "confidenceAdjustment", "riskAdjustment",
                    "planModeAdjustment", "reasons", "summary");
            case GROK_CHALLENGE -> Set.of("opposingView", "riskLevel", "challengeLevel",
                    "majorCounterEvidence", "planModeImpact", "reasons", "summary");
        };
    }

    public static List<String> requiredFields(AiDecisionChainRole role) {
        return fields(role).stream().sorted().toList();
    }

    public static Map<String, Object> responseJsonSchema(AiDecisionChainRole role) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String field : fields(role)) {
            properties.put(field, property(role, field));
        }
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", requiredFields(role));
        return schema;
    }

    public static Set<String> allowedValues(AiDecisionChainRole role, String field) {
        if (role == AiDecisionChainRole.GPT_FINAL && "direction".equals(field)) {
            return Set.of("STRONG_BULLISH", "BULLISH", "WEAK_BULLISH", "RANGE",
                    "WEAK_BEARISH", "BEARISH", "STRONG_BEARISH", "WAIT");
        }
        if (role == AiDecisionChainRole.GPT_FINAL && "planMode".equals(field)) {
            return Set.of("CONFIRM", "PREPARE", "REDUCE", "WATCH", "BLOCKED");
        }
        if (role == AiDecisionChainRole.GPT_FINAL && "confidence".equals(field)) {
            return Set.of("LOW", "MEDIUM", "HIGH");
        }
        if ("riskLevel".equals(field)) {
            return Set.of("LOW", "MEDIUM", "HIGH", "EXTREME");
        }
        if (role == AiDecisionChainRole.GEMINI_REVIEW && "verdict".equals(field)) {
            return Set.of("APPROVE", "DOWNGRADE", "REJECT", "RISK_WARNING");
        }
        if ("conflictLevel".equals(field) || "challengeLevel".equals(field)) {
            return Set.of("LEVEL_1_CONSISTENT", "LEVEL_2_MINOR_DISAGREEMENT",
                    "LEVEL_3_SIGNIFICANT_DISAGREEMENT", "LEVEL_4_EXTREME_CONFLICT");
        }
        if ("confidenceAdjustment".equals(field)) {
            return Set.of("UNCHANGED", "DOWNGRADE_ONE", "DOWNGRADE_TWO");
        }
        if ("riskAdjustment".equals(field)) {
            return Set.of("UNCHANGED", "RAISE_ONE", "RAISE_TWO");
        }
        if ("planModeAdjustment".equals(field) || "planModeImpact".equals(field)) {
            return Set.of("UNCHANGED", "DOWNGRADE_ONE", "DOWNGRADE_TWO", "BLOCKED");
        }
        return Set.of();
    }

    private static Map<String, Object> property(AiDecisionChainRole role, String field) {
        if ("worthOpening".equals(field) || "majorCounterEvidence".equals(field)) {
            return Map.of("type", "boolean");
        }
        if ("reasons".equals(field)) {
            return Map.of("type", "array",
                    "items", Map.of("type", "string", "maxLength", AiDecisionChainResponseParser.MAX_TEXT_CHARS),
                    "maxItems", AiDecisionChainResponseParser.MAX_REASON_COUNT);
        }
        Set<String> allowed = allowedValues(role, field);
        if (!allowed.isEmpty()) {
            return Map.of("type", "string", "enum", allowed.stream().sorted().toList(),
                    "maxLength", AiDecisionChainResponseParser.MAX_TEXT_CHARS);
        }
        return Map.of("type", "string", "maxLength", AiDecisionChainResponseParser.MAX_TEXT_CHARS);
    }
}
