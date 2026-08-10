package org.example.trademodel.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
                continue;
            }
            roles.put(role.name(), decisionChainRolePayload(role, result, ruleDirection));
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
                AiRoleResultsPayload.AI_ROLE_RESULTS_SCHEMA_V1,
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
            if (!AiRoleResultsPayload.AI_ROLE_RESULTS_SCHEMA_V1.equals(root.path("schemaVersion").asText(null))) {
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
                                                                      String ruleDirection) {
        JsonNode payload = parsePayload(result.getPayloadJson());
        String stance = switch (role) {
            case GPT_FINAL -> sameDirection(ruleDirection, payload.path("direction").asText(null))
                    ? "SUPPORT" : "CHALLENGE";
            case GEMINI_REVIEW -> "APPROVE".equalsIgnoreCase(payload.path("verdict").asText())
                    ? "SUPPORT" : "CHALLENGE";
            case GROK_CHALLENGE -> payload.path("majorCounterEvidence").asBoolean(false)
                    || !"NONE".equalsIgnoreCase(payload.path("challengeLevel").asText("NONE"))
                    ? "CHALLENGE" : "ABSTAIN";
        };
        String conflictLevel = switch (role) {
            case GPT_FINAL -> "NONE";
            case GEMINI_REVIEW -> payload.path("conflictLevel").asText(null);
            case GROK_CHALLENGE -> payload.path("challengeLevel").asText(null);
        };
        List<String> reasonCodes = payload.has("reasons") && payload.path("reasons").isArray()
                ? objectMapper.convertValue(payload.path("reasons"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class))
                : List.of();
        return new AiRoleResultsPayload.RolePayload(
                role.name(),
                result.getProvider() == null ? null : result.getProvider().name(),
                role.name(),
                result.getCallStatus() == null ? null : result.getCallStatus().name(),
                result.successful() ? stance : null,
                result.successful() ? sanitize(conflictLevel, 64) : null,
                sanitizeReasonCodes(reasonCodes),
                result.successful() ? sanitize(payload.path("summary").asText(null), 512) : null,
                result.isFallback() ? Boolean.TRUE : null,
                sanitize(result.getFallbackReason(), 128),
                Boolean.TRUE);
    }

    private JsonNode parsePayload(String raw) {
        try {
            JsonNode parsed = objectMapper.readTree(raw == null ? "{}" : raw);
            return parsed == null || !parsed.isObject() ? objectMapper.createObjectNode() : parsed;
        } catch (Exception ignored) {
            return objectMapper.createObjectNode();
        }
    }

    private static boolean sameDirection(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
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
                source.worthOpening(),
                sanitize(source.conflictLevel(), 64),
                source.conflictScore(),
                sanitize(source.confidenceAdjustment(), 64),
                sanitize(source.riskAdjustment(), 64),
                sanitize(source.planModeAdjustment(), 64),
                source.confused(),
                sanitize(source.downgradeReason(), 256));
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
