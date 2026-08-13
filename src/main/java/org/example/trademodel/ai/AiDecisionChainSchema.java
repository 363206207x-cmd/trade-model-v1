package org.example.trademodel.ai;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Frozen structured output contracts for the three v4.1 AI roles. */
public final class AiDecisionChainSchema {
    private static final List<String> MARKET_BIASES = List.of(
            "STRONG_BULLISH", "BULLISH", "WEAK_BULLISH", "RANGE",
            "WEAK_BEARISH", "BEARISH", "STRONG_BEARISH", "WAIT");
    private static final List<String> OPPORTUNITY_STATES = List.of(
            "OBSERVING", "CANDIDATE", "WAITING_TRIGGER", "TRIGGERED",
            "HIGH_RISK", "INVALIDATED", "COOLING", "CONFUSED");
    private static final List<String> PLAN_MODES = List.of(
            "CONFIRMATION", "PREPARATION", "REDUCED", "OBSERVATION", "BLOCKED");
    private static final List<String> COLLECTION_STATES = List.of(
            "FOUND", "NONE_FOUND", "INSUFFICIENT_DATA", "SOURCE_UNAVAILABLE", "STALE");

    private AiDecisionChainSchema() {
    }

    public static Set<String> fields(AiDecisionChainRole role) {
        return responseProperties(role).keySet();
    }

    public static List<String> requiredFields(AiDecisionChainRole role) {
        return fields(role).stream().sorted().toList();
    }

    public static Map<String, Object> responseJsonSchema(AiDecisionChainRole role) {
        return object(responseProperties(role), requiredFields(role));
    }

    public static Set<String> allowedValues(AiDecisionChainRole role, String field) {
        Object property = responseProperties(role).get(field);
        if (!(property instanceof Map<?, ?> map) || !(map.get("enum") instanceof List<?> values)) {
            return Set.of();
        }
        return values.stream().map(String::valueOf).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Map<String, Object> responseProperties(AiDecisionChainRole role) {
        return switch (role) {
            case GPT_FINAL -> gptProperties();
            case GEMINI_REVIEW -> geminiProperties();
            case GROK_CHALLENGE -> grokProperties();
        };
    }

    private static Map<String, Object> gptProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("coreJudgment", object(Map.of(
                "marketBias", enumString(MARKET_BIASES),
                "opportunityState", enumString(OPPORTUNITY_STATES),
                "text", text()), List.of("marketBias", "opportunityState", "text")));
        properties.put("supportingEvidenceState", enumString(COLLECTION_STATES));
        properties.put("supportingEvidence", array(evidence(), 20));
        properties.put("opposingEvidenceState", enumString(COLLECTION_STATES));
        properties.put("opposingEvidence", array(evidence(), 20));
        properties.put("multiTimeframeExplanation", object(Map.of(
                "4h", text(), "1h", text(), "15m", text(), "5m", text()),
                List.of("4h", "1h", "15m", "5m")));
        properties.put("biasAdjustment", object(Map.of(
                "before", enumString(MARKET_BIASES),
                "after", enumString(MARKET_BIASES),
                "reason", text()), List.of("before", "after", "reason")));
        properties.put("candidateSummary", candidateSummary());
        return properties;
    }

    private static Map<String, Object> geminiProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("evidenceGapsState", enumString(COLLECTION_STATES));
        properties.put("evidenceGaps", array(finding(), 20));
        properties.put("logicConflictsState", enumString(COLLECTION_STATES));
        properties.put("logicConflicts", array(finding(), 20));
        properties.put("underestimatedRisksState", enumString(COLLECTION_STATES));
        properties.put("underestimatedRisks", array(finding(), 20));
        properties.put("downgradeSuggestion", object(Map.of(
                "before", enumString(PLAN_MODES),
                "after", enumString(PLAN_MODES),
                "reason", text(),
                "recoveryCondition", text()),
                List.of("before", "after", "reason", "recoveryCondition")));
        properties.put("reviewResult", enumString(List.of(
                "APPROVE", "DOWNGRADE", "REJECT_CANDIDATE", "RISK_WARNING")));
        properties.put("conflictLevel", enumString(conflictLevels()));
        properties.put("finalDirectionImpact", enumString(List.of(
                "UNCHANGED", "SAME_FAMILY_DOWNGRADE", "RULE_REANALYSIS_REQUIRED")));
        properties.put("confidenceAdjustment", enumString(List.of(
                "UNCHANGED", "DOWNGRADE_ONE", "DOWNGRADE_TWO")));
        properties.put("riskAdjustment", enumString(List.of("UNCHANGED", "RAISE_ONE", "RAISE_TWO")));
        properties.put("planModeAdjustment", enumString(List.of(
                "UNCHANGED", "DOWNGRADE_ONE", "DOWNGRADE_TWO", "BLOCKED")));
        properties.put("recoveryCondition", text());
        return properties;
    }

    private static Map<String, Object> grokProperties() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("failurePathState", enumString(List.of(
                "FOUND", "NONE_FOUND", "INSUFFICIENT_DATA", "SOURCE_UNAVAILABLE", "STALE",
                "NO_VERIFIABLE_FAILURE_PATH")));
        properties.put("failurePaths", array(failurePath(), 20));
        properties.put("opposingScenariosState", enumString(COLLECTION_STATES));
        properties.put("opposingScenarios", array(opposingScenario(), 20));
        properties.put("externalEventRisksState", enumString(COLLECTION_STATES));
        properties.put("externalEventRisks", array(externalEventRisk(), 20));
        properties.put("microstructureRisksState", enumString(COLLECTION_STATES));
        properties.put("microstructureRisks", array(microstructureRisk(), 20));
        properties.put("watchIndicatorsState", enumString(COLLECTION_STATES));
        properties.put("watchIndicators", array(watchIndicator(), 20));
        properties.put("challengeSummary", text());
        properties.put("currentDirectionChallenge", text());
        properties.put("majorCounterEvidence", Map.of("type", "boolean"));
        properties.put("conflictLevel", enumString(conflictLevels()));
        properties.put("riskAdjustment", enumString(List.of("UNCHANGED", "RAISE_ONE", "RAISE_TWO")));
        properties.put("planModeImpact", enumString(List.of(
                "UNCHANGED", "DOWNGRADE_ONE", "DOWNGRADE_TWO", "BLOCKED")));
        return properties;
    }

    private static Map<String, Object> candidateSummary() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("planMode", enumString(PLAN_MODES));
        properties.put("confidence", enumString(List.of("LOW", "MEDIUM", "HIGH")));
        properties.put("riskLevel", enumString(List.of("LOW", "MEDIUM", "HIGH", "EXTREME")));
        properties.put("worthOpening", Map.of("type", "boolean"));
        properties.put("opportunityType", text());
        properties.put("recommendedAction", text());
        properties.put("entryLogic", text());
        properties.put("entryZone", text());
        properties.put("entrySource", text());
        properties.put("entryReason", text());
        properties.put("triggerCondition", text());
        properties.put("stopLogic", text());
        properties.put("stopZone", text());
        properties.put("stopSource", text());
        properties.put("stopReason", text());
        properties.put("targetLogic", text());
        properties.put("targetZones", text());
        properties.put("targetSource", text());
        properties.put("targetReason", text());
        properties.put("addPositionCondition", text());
        properties.put("reducePositionCondition", text());
        properties.put("abandonCondition", text());
        properties.put("leverageSuggestion", text());
        properties.put("positionSuggestion", text());
        properties.put("riskExplanation", text());
        properties.put("invalidCondition", text());
        properties.put("invalidationSource", text());
        properties.put("invalidationReason", text());
        properties.put("expectedRiskReward", Map.of("type", "number"));
        properties.put("expectedRiskRewardSource", text());
        properties.put("expectedRiskRewardReason", text());
        properties.put("validity", text());
        properties.put("triggerTimeframe", text());
        properties.put("holdingHorizon", text());
        properties.put("revalidationRule", text());
        properties.put("summary", text());
        return object(properties, List.copyOf(properties.keySet()));
    }

    private static Map<String, Object> evidence() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("evidenceId", text());
        properties.put("type", text());
        properties.put("source", text());
        properties.put("currentValue", text());
        properties.put("change", text());
        properties.put("direction", text());
        properties.put("strength", boundedNumber(0, 100));
        properties.put("confidence", boundedNumber(0, 100));
        properties.put("observedAt", text());
        properties.put("freshness", text());
        properties.put("analysisId", text());
        return object(properties, List.copyOf(properties.keySet()));
    }

    private static Map<String, Object> finding() {
        Map<String, Object> properties = baseFindingProperties(true);
        return object(properties, List.copyOf(properties.keySet()));
    }

    private static Map<String, Object> opposingScenario() {
        Map<String, Object> properties = baseFindingProperties(true);
        properties.put("triggerCondition", text());
        properties.put("observationWindow", text());
        properties.put("validationIndicators", nonEmptyArray(text(), 20));
        return object(properties, List.copyOf(properties.keySet()));
    }

    private static Map<String, Object> externalEventRisk() {
        Map<String, Object> properties = baseFindingProperties(true);
        properties.put("source", text());
        properties.put("observedAt", text());
        properties.put("eventWindow", text());
        return object(properties, List.copyOf(properties.keySet()));
    }

    private static Map<String, Object> microstructureRisk() {
        Map<String, Object> properties = baseFindingProperties(true);
        properties.put("phenomenon", text());
        properties.put("timeframe", text());
        return object(properties, List.copyOf(properties.keySet()));
    }

    private static Map<String, Object> watchIndicator() {
        Map<String, Object> properties = baseFindingProperties(true);
        properties.put("metric", text());
        properties.put("currentState", text());
        properties.put("triggerCondition", text());
        return object(properties, List.copyOf(properties.keySet()));
    }

    private static Map<String, Object> baseFindingProperties(boolean verifiableSourceRequired) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("findingId", text());
        properties.put("category", text());
        properties.put("text", text());
        properties.put("impact", text());
        properties.put("evidenceRefs", verifiableSourceRequired
                ? nonEmptyArray(text(), 20) : array(text(), 20));
        return properties;
    }

    private static Map<String, Object> failurePath() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("failurePathId", text());
        properties.put("hypothesis", text());
        properties.put("triggerCondition", text());
        properties.put("causalPath", text());
        properties.put("observationWindow", text());
        properties.put("validationIndicators", nonEmptyArray(text(), 20));
        properties.put("sourceRefs", nonEmptyArray(text(), 20));
        properties.put("invalidatingEvidence", text());
        return object(properties, List.copyOf(properties.keySet()));
    }

    private static List<String> conflictLevels() {
        return List.of("LEVEL_1_CONSISTENT", "LEVEL_2_MINOR_DISAGREEMENT",
                "LEVEL_3_SIGNIFICANT_DISAGREEMENT", "LEVEL_4_EXTREME_CONFLICT");
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private static Map<String, Object> array(Map<String, Object> items, int maxItems) {
        return Map.of("type", "array", "items", items, "maxItems", maxItems);
    }

    private static Map<String, Object> nonEmptyArray(Map<String, Object> items, int maxItems) {
        return Map.of("type", "array", "items", items, "minItems", 1, "maxItems", maxItems);
    }

    private static Map<String, Object> boundedNumber(int minimum, int maximum) {
        return Map.of("type", "number", "minimum", minimum, "maximum", maximum);
    }

    private static Map<String, Object> enumString(List<String> values) {
        return Map.of("type", "string", "enum", values,
                "maxLength", AiDecisionChainResponseParser.MAX_TEXT_CHARS);
    }

    private static Map<String, Object> text() {
        return Map.of("type", "string", "maxLength", AiDecisionChainResponseParser.MAX_TEXT_CHARS);
    }
}
