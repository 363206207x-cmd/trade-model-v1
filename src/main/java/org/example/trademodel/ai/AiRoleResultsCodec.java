package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class AiRoleResultsCodec {
    private static final Set<String> ROLE_KEYS = Set.of("GPT_FINAL", "GEMINI_REVIEW", "GROK_CHALLENGE");
    private static final Pattern BEARER = Pattern.compile("(?i)Bearer\\s+[^\\s,;]+");
    private static final Pattern AUTHORIZATION = Pattern.compile("(?i)Authorization\\s*[:=]\\s*[^\\s,;]+");
    private static final Pattern NAMED_SECRET = Pattern.compile(
            "(?i)(api[_-]?key|provider[_-]?secret|secret|password|access[_-]?token)\\s*[:=]\\s*[^\\s,;]+");
    private static final Pattern PROVIDER_KEY = Pattern.compile("(?i)(sk-|xai-|AIza)[A-Za-z0-9_\\-]+");

    private final ObjectMapper objectMapper;

    public AiRoleResultsCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serialize(AiOrchestratorResult result, String ruleVersion,
                            AiRoleResultsPayload.SynthesisPayload synthesis) {
        AiRoleResultsPayload payload = new AiRoleResultsPayload(
                AiRoleResultsPayload.AI_ROLE_RESULTS_SCHEMA_V1,
                sanitize(result != null ? result.getAnalysisId() : null, 128),
                sanitize(result != null ? result.getTraceId() : null, 128),
                sanitize(ruleVersion, 64),
                result != null ? enumName(result.getOrchestrationMode()) : null,
                result != null ? sanitizeReasonCodes(result.getReasonCodes()) : List.of(),
                rolePayloads(result),
                sanitizeSynthesis(synthesis),
                AiRoleResultsPayload.SafetyBoundary.defaults());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("AI role result serialization failed", e);
        }
    }

    public String serializeDecisionChain(String analysisId,
                                         String traceId,
                                         String ruleDirection,
                                         Map<AiDecisionChainRole, AiDecisionChainResult> results,
                                         AiRoleResultsPayload.SynthesisPayload synthesis) {
        Map<String, AiRoleResultsPayload.RolePayload> roles = new LinkedHashMap<>();
        for (AiDecisionChainRole role : AiDecisionChainRole.values()) {
            AiDecisionChainResult result = results == null ? null : results.get(role);
            if (result == null) {
                result = unavailableRoleResult(role);
            }
            roles.put(role.name(), decisionChainRolePayload(
                    role, result, ruleDirection, analysisId, traceId));
        }
        List<String> reasons = roles.values().stream()
                .filter(role -> Boolean.TRUE.equals(role.fallback()))
                .map(AiRoleResultsPayload.RolePayload::fallbackReason)
                .filter(value -> value != null && !value.isBlank())
                .map(value -> sanitize(value, 64))
                .distinct()
                .limit(8)
                .toList();
        AiRoleResultsPayload payload = new AiRoleResultsPayload(
                AiRoleResultsPayload.CURRENT_SCHEMA_VERSION,
                sanitize(analysisId, 128),
                sanitize(traceId, 128),
                "v4.1",
                "DECISION_CHAIN_V4_1",
                reasons,
                roles,
                sanitizeSynthesis(synthesis),
                AiRoleResultsPayload.SafetyBoundary.decisionChainV41());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Decision-chain AI role serialization failed", e);
        }
    }

    private static AiDecisionChainResult unavailableRoleResult(AiDecisionChainRole role) {
        AiDecisionChainResult result = AiDecisionChainResult.failed(
                switch (role) {
                    case GPT_FINAL -> AiProviderName.OPENAI;
                    case GEMINI_REVIEW -> AiProviderName.GEMINI;
                    case GROK_CHALLENGE -> AiProviderName.XAI;
                }, role, AiProviderCallStatus.FAILED, "ROLE_RESULT_UNAVAILABLE");
        result.setRoleState(AiRoleState.UNAVAILABLE);
        result.setDataState(AiRoleDataState.SOURCE_UNAVAILABLE);
        return result;
    }

    public ParseResult parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ParseResult(ParseStatus.EMPTY, null);
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("{")) {
            return new ParseResult(ParseStatus.LEGACY_PLAIN_TEXT, null);
        }
        try {
            JsonNode root = objectMapper.readTree(trimmed);
            if (root == null || !root.isObject()) {
                return new ParseResult(ParseStatus.MALFORMED, null);
            }
            String schemaVersion = root.path("schemaVersion").asText(null);
            if (!AiRoleResultsPayload.AI_ROLE_RESULTS_SCHEMA_V1.equals(schemaVersion)
                    && !AiRoleResultsPayload.AI_ROLE_RESULTS_SCHEMA_V2.equals(schemaVersion)) {
                return new ParseResult(ParseStatus.UNSUPPORTED_SCHEMA, null);
            }
            JsonNode roles = root.get("roles");
            if (roles == null || !roles.isObject()) {
                return new ParseResult(ParseStatus.MALFORMED, null);
            }
            for (var names = roles.fieldNames(); names.hasNext(); ) {
                String role = names.next();
                JsonNode roleNode = roles.get(role);
                if (!ROLE_KEYS.contains(role) || !roleNode.isObject()
                        || !role.equals(roleNode.path("role").asText(null))) {
                    return new ParseResult(ParseStatus.MALFORMED, null);
                }
                if (AiRoleResultsPayload.AI_ROLE_RESULTS_SCHEMA_V2.equals(schemaVersion)
                        && (!roleNode.hasNonNull("analysisId")
                        || !roleNode.hasNonNull("traceId")
                        || !roleNode.hasNonNull("roleState")
                        || !roleNode.hasNonNull("dataState")
                        || !roleNode.hasNonNull("generatedAt"))) {
                    return new ParseResult(ParseStatus.MALFORMED, null);
                }
            }
            AiRoleResultsPayload payload = objectMapper.treeToValue(root, AiRoleResultsPayload.class);
            return new ParseResult(ParseStatus.CURRENT, payload);
        } catch (Exception ignored) {
            return new ParseResult(ParseStatus.MALFORMED, null);
        }
    }

    private Map<String, AiRoleResultsPayload.RolePayload> rolePayloads(AiOrchestratorResult result) {
        Map<String, AiRoleResultsPayload.RolePayload> roles = new LinkedHashMap<>();
        if (result == null) {
            return roles;
        }
        for (AiProviderReviewResult providerResult : result.getProviderResults()) {
            String roleKey = roleKey(providerResult != null ? providerResult.getRole() : null);
            if (roleKey == null || roles.containsKey(roleKey)) {
                continue;
            }
            roles.put(roleKey, new AiRoleResultsPayload.RolePayload(
                    roleKey,
                    enumName(providerResult.getProvider()),
                    enumName(providerResult.getRole()),
                    enumName(providerResult.getCallStatus()),
                    enumName(providerResult.getStance()),
                    enumName(providerResult.getConflictLevel()),
                    sanitizeReasonCodes(providerResult.getReasonCodes()),
                    sanitize(providerResult.getSummary(), 512),
                    providerResult.isFallback() ? Boolean.TRUE : null,
                    sanitize(providerResult.getFallbackReason(), 128),
                    Boolean.TRUE));
        }
        return roles;
    }

    private AiRoleResultsPayload.RolePayload decisionChainRolePayload(AiDecisionChainRole role,
                                                                      AiDecisionChainResult result,
                                                                      String ruleDirection,
                                                                      String analysisId,
                                                                      String traceId) {
        JsonNode payload = parsePayload(result.getPayloadJson());
        String stance = switch (role) {
            case GPT_FINAL -> sameBiasFamily(ruleDirection,
                    payload.path("biasAdjustment").path("after").asText(
                            payload.path("coreJudgment").path("marketBias").asText(null)))
                    ? "SUPPORT" : "CHALLENGE";
            case GEMINI_REVIEW -> "APPROVE".equalsIgnoreCase(payload.path("reviewResult").asText())
                    ? "SUPPORT" : "CHALLENGE";
            case GROK_CHALLENGE -> payload.path("majorCounterEvidence").asBoolean(false)
                    || !"LEVEL_1_CONSISTENT".equalsIgnoreCase(
                            payload.path("conflictLevel").asText("LEVEL_1_CONSISTENT"))
                    ? "CHALLENGE" : "ABSTAIN";
        };
        String conflictLevel = switch (role) {
            case GPT_FINAL -> "NONE";
            case GEMINI_REVIEW -> payload.path("conflictLevel").asText(null);
            case GROK_CHALLENGE -> payload.path("conflictLevel").asText(null);
        };
        ObjectNode rolePayload = result.successful() && payload.isObject()
                ? ((ObjectNode) payload).deepCopy()
                : objectMapper.createObjectNode();
        if (!result.successful()) initializeEmptyCollections(role, rolePayload, collectionStateFor(result));
        rolePayload.put("role", role.name());
        if (result.getProvider() != null) rolePayload.put("provider", result.getProvider().name());
        rolePayload.put("sourceRole", role.name());
        if (result.getCallStatus() != null) rolePayload.put("callStatus", result.getCallStatus().name());
        rolePayload.put("analysisId", sanitize(defaultText(result.getAnalysisId(), analysisId), 128));
        rolePayload.put("traceId", sanitize(defaultText(result.getTraceId(), traceId), 128));
        AiRoleState roleState = defaultRoleState(result);
        rolePayload.put("roleState", enumName(roleState));
        rolePayload.put("dataState", enumName(defaultDataState(result)));
        rolePayload.put("generatedAt", result.getGeneratedAt() == null
                ? java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString()
                : result.getGeneratedAt().toString());
        rolePayload.put("resultAvailable", result.successful()
                && (roleState == AiRoleState.READY || roleState == AiRoleState.PARTIAL));
        if (result.successful()) rolePayload.put("stance", stance);
        if (result.successful() && conflictLevel != null) rolePayload.put("conflictLevel", sanitize(conflictLevel, 64));
        rolePayload.set("reasonCodes", objectMapper.valueToTree(List.of()));
        String summary = structuredSummary(role, payload);
        if (summary != null) rolePayload.put("summary", sanitize(summary, 512));
        if (result.isFallback()) rolePayload.put("fallback", true);
        String fallbackReason = sanitize(result.getFallbackReason(), 128);
        if (fallbackReason != null) rolePayload.put("fallbackReason", fallbackReason);
        rolePayload.put("manualReviewRequired", true);
        try {
            return objectMapper.treeToValue(rolePayload, AiRoleResultsPayload.RolePayload.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Decision-chain role payload mapping failed", exception);
        }
    }

    private void initializeEmptyCollections(AiDecisionChainRole role,
                                            ObjectNode payload,
                                            String collectionState) {
        switch (role) {
            case GPT_FINAL -> {
                emptyCollection(payload, "supportingEvidence", "supportingEvidenceState", collectionState);
                emptyCollection(payload, "opposingEvidence", "opposingEvidenceState", collectionState);
            }
            case GEMINI_REVIEW -> {
                emptyCollection(payload, "evidenceGaps", "evidenceGapsState", collectionState);
                emptyCollection(payload, "logicConflicts", "logicConflictsState", collectionState);
                emptyCollection(payload, "underestimatedRisks", "underestimatedRisksState", collectionState);
            }
            case GROK_CHALLENGE -> {
                emptyCollection(payload, "failurePaths", "failurePathState", collectionState);
                emptyCollection(payload, "opposingScenarios", "opposingScenariosState", collectionState);
                emptyCollection(payload, "externalEventRisks", "externalEventRisksState", collectionState);
                emptyCollection(payload, "microstructureRisks", "microstructureRisksState", collectionState);
                emptyCollection(payload, "watchIndicators", "watchIndicatorsState", collectionState);
            }
        }
    }

    private void emptyCollection(ObjectNode payload, String collection, String state, String value) {
        ArrayNode empty = objectMapper.createArrayNode();
        payload.set(collection, empty);
        payload.put(state, value);
    }

    private static String collectionStateFor(AiDecisionChainResult result) {
        if (result != null && result.getDataState() == AiRoleDataState.STALE) return AiCollectionState.STALE.name();
        if (result != null && result.getDataState() == AiRoleDataState.INSUFFICIENT_DATA) {
            return AiCollectionState.INSUFFICIENT_DATA.name();
        }
        return AiCollectionState.SOURCE_UNAVAILABLE.name();
    }

    private static String structuredSummary(AiDecisionChainRole role, JsonNode payload) {
        return switch (role) {
            case GPT_FINAL -> payload.path("candidateSummary").path("summary").asText(null);
            case GEMINI_REVIEW -> payload.path("downgradeSuggestion").path("reason").asText(null);
            case GROK_CHALLENGE -> payload.path("challengeSummary").asText(null);
        };
    }

    private static AiRoleState defaultRoleState(AiDecisionChainResult result) {
        if (result.getRoleState() != null) return result.getRoleState();
        if (result.successful()) return AiRoleState.READY;
        return result.isFallback() ? AiRoleState.FALLBACK : AiRoleState.ERROR;
    }

    private static AiRoleDataState defaultDataState(AiDecisionChainResult result) {
        if (result.getDataState() != null) return result.getDataState();
        if (result.successful()) return AiRoleDataState.READY;
        if (result.getCallStatus() == AiProviderCallStatus.TIMEOUT) return AiRoleDataState.AI_TIMEOUT;
        return AiRoleDataState.AI_FAILED;
    }

    private static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private JsonNode parsePayload(String raw) {
        try {
            JsonNode parsed = objectMapper.readTree(raw == null ? "{}" : raw);
            return parsed == null || !parsed.isObject() ? objectMapper.createObjectNode() : parsed;
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private static boolean sameBiasFamily(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        String before = left.trim().toUpperCase(Locale.ROOT);
        String after = right.trim().toUpperCase(Locale.ROOT);
        if (before.equals(after)) return true;
        if ("WAIT".equals(before) || "WAIT".equals(after)) return false;
        return (before.contains("BULLISH") && after.contains("BULLISH"))
                || (before.contains("BEARISH") && after.contains("BEARISH"))
                || ("RANGE".equals(before) && "RANGE".equals(after));
    }

    private AiRoleResultsPayload.SynthesisPayload sanitizeSynthesis(
            AiRoleResultsPayload.SynthesisPayload source) {
        if (source == null) {
            return AiRoleResultsPayload.SynthesisPayload.empty();
        }
        return new AiRoleResultsPayload.SynthesisPayload(
                sanitize(source.finalMarketBias(), 64),
                sanitize(source.finalConfidence(), 64),
                sanitize(source.finalRiskLevel(), 64),
                sanitize(source.finalPlanMode(), 64),
                source.worthOpening(),
                sanitize(source.conflictLevel(), 64),
                source.conflictScore(),
                sanitize(source.confidenceAdjustment(), 64),
                sanitize(source.riskAdjustment(), 64),
                sanitize(source.planModeAdjustment(), 64),
                source.confused(),
                sanitize(source.downgradeReason(), 256),
                sanitize(source.mainReason(), 256),
                sanitize(source.recoveryCondition(), 512));
    }

    private List<String> sanitizeReasonCodes(List<String> reasonCodes) {
        if (reasonCodes == null) {
            return List.of();
        }
        return reasonCodes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> sanitize(value, 64))
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.replaceAll("[^A-Za-z0-9_\\-]", "_").toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .map(value -> value.length() <= 64 ? value : value.substring(0, 64))
                .distinct()
                .limit(8)
                .toList();
    }

    private String sanitize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String sanitized = BEARER.matcher(value).replaceAll("Bearer ***");
        sanitized = AUTHORIZATION.matcher(sanitized).replaceAll("Authorization=***");
        sanitized = NAMED_SECRET.matcher(sanitized).replaceAll("$1=***");
        sanitized = PROVIDER_KEY.matcher(sanitized).replaceAll("provider-key-***");
        sanitized = sanitized.replace('\n', ' ').replace('\r', ' ').trim();
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength);
    }

    private String roleKey(AiProviderRole role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case GPT_RULE_REVIEW -> "GPT_FINAL";
            case GEMINI_CONSISTENCY_REVIEW -> "GEMINI_REVIEW";
            case GROK_ADVERSARIAL_CHALLENGE -> "GROK_CHALLENGE";
        };
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    public enum ParseStatus {
        CURRENT,
        EMPTY,
        LEGACY_PLAIN_TEXT,
        UNSUPPORTED_SCHEMA,
        MALFORMED
    }

    public record ParseResult(ParseStatus status, AiRoleResultsPayload payload) {
        public boolean current() {
            return status == ParseStatus.CURRENT && payload != null;
        }
    }
}
