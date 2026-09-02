package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.ai.AiUsageGuard;
import org.example.trademodel.ai.AiUsageGuardResult;
import org.example.trademodel.ai.AiRoleDataState;
import org.example.trademodel.ai.AiRoleState;
import org.example.trademodel.config.FundamentalAiV41Properties;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.service.AiCallLogService;
import org.example.trademodel.service.DecisionChainAiOrchestratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class DecisionChainAiOrchestratorServiceImpl implements DecisionChainAiOrchestratorService {
    private static final Set<String> CACHE_VOLATILE_FIELDS = Set.of(
            "analysisId", "traceId", "requestId", "opportunityId", "candidateId",
            "evidenceId", "scoreId", "sourceTraceId");

    private final Map<AiProviderRole, AiProviderClient> clients;
    private final AiUsageGuard usageGuard;
    private final AiCallLogService callLogService;
    private final AiOrchestratorProperties properties;
    private final FundamentalAiV41Properties v41Properties;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, CachedResult> decisionChainCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> lastAssetRoleCallEpochMs = new ConcurrentHashMap<>();
    private final Semaphore concurrencyGate;

    public DecisionChainAiOrchestratorServiceImpl(List<AiProviderClient> providerClients,
                                                  AiUsageGuard usageGuard,
                                                  AiCallLogService callLogService,
                                                  AiOrchestratorProperties properties) {
        this(providerClients, usageGuard, callLogService, properties,
                constructorContract(properties), new ObjectMapper());
    }

    @Autowired
    public DecisionChainAiOrchestratorServiceImpl(List<AiProviderClient> providerClients,
                                                  AiUsageGuard usageGuard,
                                                  AiCallLogService callLogService,
                                                  AiOrchestratorProperties properties,
                                                  FundamentalAiV41Properties v41Properties,
                                                  ObjectMapper objectMapper) {
        Map<AiProviderRole, AiProviderClient> indexed = new EnumMap<>(AiProviderRole.class);
        if (providerClients != null) {
            for (AiProviderClient client : providerClients) {
                indexed.putIfAbsent(client.role(), client);
            }
        }
        this.clients = Map.copyOf(indexed);
        this.usageGuard = usageGuard;
        this.callLogService = callLogService;
        this.properties = properties;
        this.v41Properties = v41Properties;
        this.objectMapper = objectMapper;
        this.concurrencyGate = new Semaphore(Math.max(1, Math.min(
                properties.getMaxConcurrentCalls(),
                v41Properties.getAiGate().getConcurrencyLimit())), true);
    }

    @Override
    public AiDecisionChainResult invoke(AiDecisionChainRequest request) {
        long startedNanos = System.nanoTime();
        if (request == null || request.getRole() == null) {
            AiDecisionChainResult invalid = AiDecisionChainResult.failed(null, null, AiProviderCallStatus.FAILED,
                    "DECISION_CHAIN_REQUEST_INVALID");
            return attachRuntimeMetadata(request, invalid);
        }
        if (!request.isInputContractSatisfied()) {
            AiDecisionChainResult blocked = AiDecisionChainResult.failed(
                    providerName(request.getRole()), request.getRole(), AiProviderCallStatus.INVALID_RESPONSE,
                    "AI_INPUT_CONTRACT_BLOCKED:" + String.join(",", request.getInputContractFailures()));
            blocked.setRoleState(AiRoleState.FALLBACK);
            blocked.setDataState(AiRoleDataState.FALLBACK_RULE_ONLY);
            attachRuntimeMetadata(request, blocked);
            recordTerminalTrace(request, blocked.getProvider(), "NOT_CALLED_INPUT_GATE", blocked, BigDecimal.ZERO);
            return blocked;
        }
        String cacheKey = cacheKey(request);
        CachedResult cachedEntry = cachedResult(cacheKey);
        if (cachedEntry != null) {
            AiDecisionChainResult cached = rebindCachedResult(cachedEntry, request);
            if (cached != null && traceabilityFailure(request, cached) == null) {
                cached.setLatencyMs(elapsedMs(startedNanos));
                cached.setCacheHit(true);
                attachRuntimeMetadata(request, cached);
                recordTerminalTrace(request, cached.getProvider(), cached.getSelectedModel(),
                        cached, BigDecimal.ZERO);
                return cached;
            }
            decisionChainCache.remove(cacheKey, cachedEntry);
        }
        AiProviderClient client = clients.get(providerRole(request.getRole()));
        if (client == null) {
            AiDecisionChainResult missing = AiDecisionChainResult.failed(providerName(request.getRole()), request.getRole(),
                    AiProviderCallStatus.FAILED, "DECISION_CHAIN_PROVIDER_MISSING");
            missing.setLatencyMs(elapsedMs(startedNanos));
            attachRuntimeMetadata(request, missing);
            recordTerminalTrace(request, missing.getProvider(), "NOT_CONFIGURED", missing, BigDecimal.ZERO);
            return missing;
        }
        AiUsageGuardResult guard;
        try {
            guard = usageGuard.evaluate(client, request.getAnalysisId());
        } catch (Exception exception) {
            AiDecisionChainResult failed = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    AiProviderCallStatus.FAILED, "AI_USAGE_GUARD_FAILED");
            failed.setLatencyMs(elapsedMs(startedNanos));
            attachRuntimeMetadata(request, failed);
            recordTerminalTrace(request, client.provider(), modelName(client), failed, BigDecimal.ZERO);
            return failed;
        }
        if (guard == null) {
            AiDecisionChainResult failed = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    AiProviderCallStatus.FAILED, "AI_USAGE_GUARD_RESULT_MISSING");
            failed.setLatencyMs(elapsedMs(startedNanos));
            attachRuntimeMetadata(request, failed);
            recordTerminalTrace(request, client.provider(), modelName(client), failed, BigDecimal.ZERO);
            return failed;
        }
        BigDecimal reserved = guard.getReservedCostUsd() == null
                ? BigDecimal.ZERO : guard.getReservedCostUsd();
        QuotaBlock quotaBlock = guard.isAllowed() ? machineQuotaBlock(request, reserved) : null;
        AiCallLogDO log;
        try {
            log = callLogService.startDecisionChainCall(request, client, reserved);
        } catch (Exception exception) {
            AiDecisionChainResult failed = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    AiProviderCallStatus.FAILED, "AI_CALL_LOG_START_FAILED");
            failed.setLatencyMs(elapsedMs(startedNanos));
            attachRuntimeMetadata(request, failed);
            recordTerminalTrace(request, client.provider(), modelName(client), failed, reserved);
            return failed;
        }
        AiDecisionChainResult result;
        if (!guard.isAllowed()) {
            result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    guard.getStatus(), guard.getReasonCode());
        } else if (quotaBlock != null) {
            result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    quotaBlock.status(), quotaBlock.reasonCode());
        } else if (!acquireAssetRoleFrequency(request)) {
            result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    AiProviderCallStatus.RATE_LIMITED, "ASSET_ROLE_FREQUENCY_LIMITED");
        } else if (!concurrencyGate.tryAcquire()) {
            result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                    AiProviderCallStatus.RATE_LIMITED, "AI_CONCURRENCY_LIMITED");
        } else {
            try {
                String timeoutFailure = timeoutConfigurationFailure(client.provider());
                if (timeoutFailure != null) {
                    result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                            AiProviderCallStatus.FAILED, timeoutFailure);
                } else {
                    result = client.executeDecisionChain(request, effectiveTimeoutMs(client.provider()));
                }
            } catch (Exception exception) {
                result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                        AiProviderCallStatus.FAILED, "DECISION_CHAIN_PROVIDER_EXCEPTION");
                result.setLatencyMs(elapsedMs(startedNanos));
            } finally {
                concurrencyGate.release();
            }
            if (result == null) {
                result = AiDecisionChainResult.failed(client.provider(), request.getRole(),
                        AiProviderCallStatus.FAILED, "DECISION_CHAIN_PROVIDER_NULL_RESULT");
            }
        }
        if (result.getLatencyMs() == null) result.setLatencyMs(elapsedMs(startedNanos));
        result.setReservedCostUsd(reserved);
        if (result.successful()) {
            String traceabilityFailure = traceabilityFailure(request, result);
            if (traceabilityFailure != null) {
                result.setAuditOutput(result.getPayloadJson());
                result.setPayloadJson(null);
                result.setCallStatus(AiProviderCallStatus.INVALID_RESPONSE);
                result.setFallback(true);
                result.setFallbackReason(traceabilityFailure);
                result.setErrorCode(traceabilityFailure);
            }
        }
        attachRuntimeMetadata(request, result);
        try {
            callLogService.completeDecisionChainCall(log, result);
        } catch (Exception exception) {
            result.setFallback(true);
            result.setFallbackReason("AI_CALL_LOG_COMPLETE_FAILED");
            result.setErrorCode("AI_CALL_LOG_COMPLETE_FAILED");
            recordTerminalTrace(request, client.provider(), modelName(client), result, reserved);
        }
        if (result.successful() && !result.isCacheHit() && cacheKey != null) {
            cache(cacheKey, request, result);
        }
        return result;
    }

    private long effectiveTimeoutMs(AiProviderName provider) {
        return properties.getProviderTimeouts().timeoutMs(provider);
    }

    private String timeoutConfigurationFailure(AiProviderName provider) {
        AiOrchestratorProperties.ProviderTimeouts timeouts = properties.getProviderTimeouts();
        if (timeouts == null || !timeouts.validOverall()) {
            return "ORCHESTRATOR_TIMEOUT_CONFIG_INVALID";
        }
        return timeouts.validProvider(provider) ? null : "PROVIDER_TIMEOUT_CONFIG_INVALID";
    }

    private boolean acquireAssetRoleFrequency(AiDecisionChainRequest request) {
        long contractInterval = Math.multiplyExact(
                v41Properties.getAiGate().getPerAssetCooldownSeconds().longValue(), 1_000L);
        long minimumInterval = Math.max(properties.getPerAssetRoleMinIntervalMs(), contractInterval);
        if (minimumInterval <= 0L) {
            return true;
        }
        String symbol = request.getSymbol() == null ? "" : request.getSymbol().trim().toUpperCase();
        String timeframe = request.getTimeframe() == null ? "" : request.getTimeframe().trim().toLowerCase();
        if (symbol.isEmpty() || timeframe.isEmpty() || request.getRole() == null) {
            return false;
        }
        String key = symbol + "|" + timeframe + "|" + request.getRole().name();
        long now = System.currentTimeMillis();
        AtomicBoolean allowed = new AtomicBoolean(false);
        lastAssetRoleCallEpochMs.compute(key, (ignored, previous) -> {
            if (previous == null || now - previous >= minimumInterval) {
                allowed.set(true);
                return now;
            }
            return previous;
        });
        return allowed.get();
    }

    private QuotaBlock machineQuotaBlock(AiDecisionChainRequest request, BigDecimal reservedCost) {
        try {
            FundamentalAiV41Properties.AiGate gate = v41Properties.getAiGate();
            long estimatedTokens = Math.max(1L, properties.getMaxInputChars() / 4L)
                    + Math.max(1L, properties.getMaxOutputTokens());
            int maximumRoleAttempts = 1 + gate.getMaxRetryPerRole();
            if (callLogService.countDecisionChainRoleAttempts(
                    request.getAnalysisId(), request.getRole().name()) >= maximumRoleAttempts) {
                return new QuotaBlock(AiProviderCallStatus.RATE_LIMITED, "AI_ROLE_ATTEMPT_LIMIT_REACHED");
            }
            if (callLogService.sumDecisionChainTokensByAnalysisId(request.getAnalysisId()) + estimatedTokens
                    > gate.getPerRunTokenLimit()) {
                return new QuotaBlock(AiProviderCallStatus.BUDGET_BLOCKED, "AI_PER_RUN_TOKEN_LIMIT_REACHED");
            }

            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            java.time.LocalDateTime hourStart = now.minusHours(1).toLocalDateTime();
            java.time.LocalDateTime dayStart = now.toLocalDate().atStartOfDay();
            if (callLogService.countDecisionChainAttemptsSince(hourStart) >= gate.getHourlyCallLimit()) {
                return new QuotaBlock(AiProviderCallStatus.RATE_LIMITED, "AI_HOURLY_CALL_LIMIT_REACHED");
            }
            if (callLogService.sumDecisionChainTokensSince(hourStart) + estimatedTokens
                    > gate.getHourlyTokenLimit()) {
                return new QuotaBlock(AiProviderCallStatus.BUDGET_BLOCKED, "AI_HOURLY_TOKEN_LIMIT_REACHED");
            }
            if (callLogService.countDecisionChainAttemptsSince(dayStart) >= gate.getDailyCallLimit()) {
                return new QuotaBlock(AiProviderCallStatus.RATE_LIMITED, "AI_DAILY_CALL_LIMIT_REACHED");
            }
            if (callLogService.sumDecisionChainTokensSince(dayStart) + estimatedTokens
                    > gate.getDailyTokenLimit()) {
                return new QuotaBlock(AiProviderCallStatus.BUDGET_BLOCKED, "AI_DAILY_TOKEN_LIMIT_REACHED");
            }
            BigDecimal hardDailyCostLimit = BigDecimal.valueOf(gate.getDailyCostMicrosLimit(), 6);
            BigDecimal spent = callLogService.sumChargeableCostSince(dayStart);
            if (spent == null) spent = BigDecimal.ZERO;
            if (spent.add(reservedCost == null ? BigDecimal.ZERO : reservedCost)
                    .compareTo(hardDailyCostLimit) > 0) {
                return new QuotaBlock(AiProviderCallStatus.BUDGET_BLOCKED, "AI_DAILY_COST_LIMIT_REACHED");
            }
            return null;
        } catch (Exception quotaReadFailure) {
            return new QuotaBlock(AiProviderCallStatus.BUDGET_BLOCKED, "AI_MACHINE_QUOTA_UNAVAILABLE");
        }
    }

    private static FundamentalAiV41Properties constructorContract(AiOrchestratorProperties properties) {
        FundamentalAiV41Properties contract = FundamentalAiV41Properties.contractFixture();
        long cooldownMs = properties == null ? 0L : properties.getPerAssetRoleMinIntervalMs();
        contract.getAiGate().setPerAssetCooldownSeconds((int) Math.min(
                Integer.MAX_VALUE, (cooldownMs + 999L) / 1_000L));
        if (properties != null) {
            contract.getAiGate().setConcurrencyLimit(properties.getMaxConcurrentCalls());
        }
        return contract;
    }

    private static AiDecisionChainResult attachRuntimeMetadata(AiDecisionChainRequest request,
                                                                AiDecisionChainResult result) {
        if (result == null) return null;
        result.setAnalysisId(request == null ? null : request.getAnalysisId());
        result.setTraceId(request == null ? null : request.getTraceId());
        result.setGeneratedAt(OffsetDateTime.now(ZoneOffset.UTC));
        AiProviderCallStatus status = result.getCallStatus();
        if (result.getDataState() == AiRoleDataState.FALLBACK_RULE_ONLY) {
            result.setRoleState(AiRoleState.FALLBACK);
        } else if (result.successful()) {
            result.setRoleState(AiRoleState.READY);
            result.setDataState(AiRoleDataState.READY);
        } else if (status == AiProviderCallStatus.TIMEOUT) {
            result.setRoleState(AiRoleState.ERROR);
            result.setDataState(AiRoleDataState.AI_TIMEOUT);
        } else if ("DECISION_CHAIN_PROVIDER_MISSING".equals(result.getFallbackReason())) {
            result.setRoleState(AiRoleState.UNAVAILABLE);
            result.setDataState(AiRoleDataState.SOURCE_UNAVAILABLE);
        } else if (result.isFallback()) {
            result.setRoleState(AiRoleState.FALLBACK);
            result.setDataState(AiRoleDataState.AI_FAILED);
        } else {
            result.setRoleState(AiRoleState.ERROR);
            result.setDataState(AiRoleDataState.AI_FAILED);
        }
        return result;
    }

    private void recordTerminalTrace(AiDecisionChainRequest request,
                                     AiProviderName provider,
                                     String modelName,
                                     AiDecisionChainResult result,
                                     BigDecimal reservedCost) {
        try {
            callLogService.recordDecisionChainResult(request, provider, modelName, result, reservedCost);
        } catch (Exception persistenceFailure) {
            throw new IllegalStateException("AI_TRACE_PERSISTENCE_FAILED", persistenceFailure);
        }
    }

    private static String modelName(AiProviderClient client) {
        if (client == null || client.providerProperties() == null
                || client.providerProperties().getEffectiveModel() == null
                || client.providerProperties().getEffectiveModel().isBlank()) {
            return "UNAVAILABLE";
        }
        return client.providerProperties().getEffectiveModel();
    }

    private String traceabilityFailure(AiDecisionChainRequest request, AiDecisionChainResult result) {
        try {
            JsonNode root = objectMapper.readTree(result.getPayloadJson());
            Map<String, Map<String, Object>> evidenceById = allowedEvidenceById(request);
            Set<String> allowedRefs = allowedEvidenceRefs(request);
            if (request.getRole() == AiDecisionChainRole.GPT_FINAL) {
                String failure = validateEvidenceArray(root.path("supportingEvidence"), request, evidenceById);
                if (failure != null) return failure;
                failure = validateEvidenceArray(root.path("opposingEvidence"), request, evidenceById);
                if (failure != null) return failure;
                String ruleBias = nestedText(request.getInput(), "decisionBundle", "ruleDirection");
                String before = root.path("biasAdjustment").path("before").asText(null);
                String after = root.path("biasAdjustment").path("after").asText(null);
                if (ruleBias == null || !ruleBias.equals(before) || !sameBiasFamily(ruleBias, after)) {
                    return "AI_OUTPUT_RULE_DIRECTION_VIOLATION";
                }
                JsonNode candidate = root.path("candidateSummary");
                for (String forbiddenField : List.of(
                        "entryZone", "stopZone", "targetZones", "expectedRiskReward",
                        "entrySource", "stopSource", "targetSource")) {
                    if (candidate.has(forbiddenField)) {
                        return "AI_OUTPUT_RULE_OWNED_PLAN_FIELD_FORBIDDEN";
                    }
                }
            } else if (request.getRole() == AiDecisionChainRole.GEMINI_REVIEW) {
                Set<String> reviewRefs = allowedReviewRefs(request);
                for (String field : List.of("evidenceGaps", "logicConflicts", "underestimatedRisks")) {
                    String failure = validateFindingRefs(root.path(field), reviewRefs, true);
                    if (failure != null) return failure;
                }
            } else if (request.getRole() == AiDecisionChainRole.GROK_CHALLENGE) {
                for (String field : List.of("opposingScenarios", "externalEventRisks",
                        "microstructureRisks", "watchIndicators")) {
                    String failure = validateFindingRefs(root.path(field), allowedRefs, true);
                    if (failure != null) return failure;
                }
                for (JsonNode path : root.path("failurePaths")) {
                    String failure = validateRefs(path.path("sourceRefs"), allowedRefs, true);
                    if (failure != null) return failure;
                }
                String externalEventFailure = validateExternalEventRisks(
                        root.path("externalEventRisks"), request);
                if (externalEventFailure != null) return externalEventFailure;
                String externalEventStateFailure = validateExternalEventCollectionState(root, request);
                if (externalEventStateFailure != null) return externalEventStateFailure;
            }
            return null;
        } catch (Exception exception) {
            return "AI_OUTPUT_TRACEABILITY_PARSE_FAILED";
        }
    }

    private static String validateEvidenceArray(JsonNode rows,
                                                AiDecisionChainRequest request,
                                                Map<String, Map<String, Object>> evidenceById) {
        for (JsonNode row : rows) {
            String evidenceId = row.path("evidenceId").asText(null);
            String analysisId = row.path("analysisId").asText(null);
            Map<String, Object> expected = evidenceId == null ? null : evidenceById.get(evidenceId);
            if (expected == null
                    || analysisId == null || !analysisId.equals(request.getAnalysisId())) {
                return "AI_OUTPUT_EVIDENCE_TRACEABILITY_INVALID";
            }
            if (!matchesText(row, "type", expected.get("type"))
                    || !matchesText(row, "source", expected.get("source"))
                    || !matchesText(row, "currentValue", expected.get("currentValue"))
                    || !matchesText(row, "change", expected.get("changeFromBaseline"))
                    || !matchesText(row, "direction", expected.get("direction"))
                    || !matchesNumber(row, "strength", expected.get("strength"))
                    || !matchesNumber(row, "confidence", expected.get("confidence"))
                    || !matchesText(row, "observedAt", expected.get("observedAt"))
                    || !matchesText(row, "freshness", expected.get("freshness"))
                    || !matchesText(row, "analysisId", expected.get("analysisId"))) {
                return "AI_OUTPUT_EVIDENCE_FACT_MISMATCH";
            }
        }
        return null;
    }

    private static String validateFindingRefs(JsonNode rows,
                                              Set<String> allowedRefs,
                                              boolean requireSource) {
        for (JsonNode row : rows) {
            String failure = validateRefs(row.path("evidenceRefs"), allowedRefs, requireSource);
            if (failure != null) return failure;
        }
        return null;
    }

    private static String validateRefs(JsonNode refs,
                                       Set<String> allowedRefs,
                                       boolean requireSource) {
        if (!refs.isArray() || (requireSource && refs.isEmpty())) {
            return "AI_OUTPUT_SOURCE_REFERENCE_REQUIRED";
        }
        for (JsonNode ref : refs) {
            String value = ref.asText(null);
            if (value == null || !allowedRefs.contains(value)) {
                return "AI_OUTPUT_SOURCE_REFERENCE_INVALID";
            }
        }
        return null;
    }

    private static String validateExternalEventRisks(JsonNode rows,
                                                     AiDecisionChainRequest request) {
        Map<String, Map<String, Object>> evidenceByRef = allowedEvidenceByReference(request);
        for (JsonNode row : rows) {
            boolean matched = false;
            for (JsonNode ref : row.path("evidenceRefs")) {
                Map<String, Object> expected = evidenceByRef.get(ref.asText());
                if (expected != null
                        && matchesAnyText(row, "source", expected,
                        "source", "sourceReference", "sourceTraceId")
                        && matchesText(row, "observedAt", expected.get("observedAt"))
                        && matchesText(row, "eventWindow", expected.get("eventWindow"))) {
                    matched = true;
                    break;
                }
            }
            if (!matched) return "AI_OUTPUT_EXTERNAL_EVENT_PROVENANCE_INVALID";
        }
        return null;
    }

    private static String validateExternalEventCollectionState(JsonNode root,
                                                               AiDecisionChainRequest request) {
        String state = root.path("externalEventRisksState").asText("").trim().toUpperCase(java.util.Locale.ROOT);
        if ("NONE_FOUND".equals(state) && !hasExternalEventCoverage(request, "FRESH")) {
            return "AI_OUTPUT_EXTERNAL_EVENT_STATE_REQUIRES_FRESH_COVERAGE";
        }
        if ("STALE".equals(state) && !hasExternalEventCoverage(request, "STALE")) {
            return "AI_OUTPUT_EXTERNAL_EVENT_STATE_REQUIRES_STALE_SOURCE";
        }
        if ("FOUND".equals(state) && !hasExternalEventCoverage(request, "FRESH")) {
            return "AI_OUTPUT_EXTERNAL_EVENT_FOUND_REQUIRES_FRESH_SOURCE";
        }
        if ("SOURCE_UNAVAILABLE".equals(state) && hasExternalEventSource(request)) {
            return "AI_OUTPUT_EXTERNAL_EVENT_SOURCE_UNAVAILABLE_CONTRADICTS_INPUT";
        }
        return null;
    }

    private static boolean hasExternalEventCoverage(AiDecisionChainRequest request,
                                                    String requiredFreshness) {
        for (Map<String, Object> evidence : inputEvidence(request)) {
            if (isExternalEventEvidence(evidence) && requiredFreshness.equalsIgnoreCase(
                    normalizedScalar(evidence.get("freshness")))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExternalEventSource(AiDecisionChainRequest request) {
        return inputEvidence(request).stream()
                .anyMatch(DecisionChainAiOrchestratorServiceImpl::isExternalEventEvidence);
    }

    private static boolean isExternalEventEvidence(Map<String, Object> evidence) {
        if (evidence == null) return false;
        String type = normalizedScalar(evidence.get("type")).toUpperCase(java.util.Locale.ROOT);
        return type.contains("EVENT") || type.contains("NEWS") || type.contains("MACRO")
                || type.contains("事件") || type.contains("新闻") || type.contains("宏观")
                || evidence.get("externalEventId") != null || evidence.get("externalEventType") != null;
    }

    private static boolean matchesText(JsonNode row, String field, Object expected) {
        if (expected == null || !row.hasNonNull(field)) return false;
        return normalizedScalar(expected).equals(row.path(field).asText().trim());
    }

    private static boolean matchesAnyText(JsonNode row,
                                          String field,
                                          Map<String, Object> expected,
                                          String... expectedFields) {
        if (!row.hasNonNull(field)) return false;
        String actual = row.path(field).asText().trim();
        for (String expectedField : expectedFields) {
            Object value = expected.get(expectedField);
            if (value != null && normalizedScalar(value).equals(actual)) return true;
        }
        return false;
    }

    private static boolean matchesNumber(JsonNode row, String field, Object expected) {
        if (!(expected instanceof Number) || !row.path(field).isNumber()) return false;
        try {
            BigDecimal expectedNumber = new BigDecimal(expected.toString()).stripTrailingZeros();
            BigDecimal actualNumber = row.path(field).decimalValue().stripTrailingZeros();
            return expectedNumber.compareTo(actualNumber) == 0;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static String normalizedScalar(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static Map<String, Map<String, Object>> allowedEvidenceById(AiDecisionChainRequest request) {
        Map<String, Map<String, Object>> indexed = new LinkedHashMap<>();
        for (Map<String, Object> evidence : inputEvidence(request)) {
            Object evidenceId = evidence.get("evidenceId");
            if (evidenceId != null && !evidenceId.toString().isBlank()) {
                indexed.putIfAbsent(evidenceId.toString().trim(), evidence);
            }
        }
        return Map.copyOf(indexed);
    }

    private static Map<String, Map<String, Object>> allowedEvidenceByReference(
            AiDecisionChainRequest request) {
        Map<String, Map<String, Object>> indexed = new LinkedHashMap<>();
        for (Map<String, Object> evidence : inputEvidence(request)) {
            for (String key : List.of("evidenceId", "source", "sourceReference", "sourceTraceId")) {
                Object value = evidence.get(key);
                if (value != null && !value.toString().isBlank()) {
                    indexed.putIfAbsent(value.toString().trim(), evidence);
                }
            }
        }
        return Map.copyOf(indexed);
    }

    private static List<Map<String, Object>> inputEvidence(AiDecisionChainRequest request) {
        if (request == null || request.getInput() == null) return List.of();
        List<Map<String, Object>> evidence = new ArrayList<>();
        appendInputEvidence(evidence, request.getInput().get("evidence"));
        Object derivatives = request.getInput().get("derivativesContext");
        if (derivatives instanceof Map<?, ?> derivativeContext) {
            appendInputEvidence(evidence, derivativeContext.get("derivedEvidence"));
        }
        return List.copyOf(evidence);
    }

    private static void appendInputEvidence(List<Map<String, Object>> evidence, Object candidateRows) {
        if (!(candidateRows instanceof List<?> rows)) return;
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> raw)) continue;
            Map<String, Object> mapped = new LinkedHashMap<>();
            raw.forEach((key, value) -> mapped.put(String.valueOf(key), value));
            evidence.add(java.util.Collections.unmodifiableMap(mapped));
        }
    }

    private static Set<String> allowedEvidenceRefs(AiDecisionChainRequest request) {
        Set<String> refs = new HashSet<>();
        for (Map<String, Object> map : inputEvidence(request)) {
            for (String key : List.of("evidenceId", "source", "sourceReference", "sourceTraceId")) {
                Object value = map.get(key);
                if (value != null && !value.toString().isBlank()) refs.add(value.toString().trim());
            }
        }
        return Set.copyOf(refs);
    }

    private static Set<String> allowedReviewRefs(AiDecisionChainRequest request) {
        Set<String> refs = new HashSet<>(allowedEvidenceRefs(request));
        if (request != null) {
            if (request.getCandidateId() != null && !request.getCandidateId().isBlank()) {
                refs.add(request.getCandidateId().trim());
            }
            if (request.getInput() != null && request.getInput().get("scores") instanceof List<?> scores) {
                for (Object row : scores) {
                    if (row instanceof Map<?, ?> score) {
                        Object scoreId = score.get("scoreId");
                        if (scoreId != null && !scoreId.toString().isBlank()) {
                            refs.add(scoreId.toString().trim());
                        }
                    }
                }
            }
        }
        return Set.copyOf(refs);
    }

    private static String nestedText(Map<String, Object> source, String objectKey, String field) {
        if (source == null || !(source.get(objectKey) instanceof Map<?, ?> nested)) return null;
        Object value = nested.get(field);
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }

    private static boolean sameBiasFamily(String before, String after) {
        if (before == null || after == null) return false;
        if (before.equals(after)) return true;
        if ("WAIT".equals(before) || "WAIT".equals(after)) return false;
        return (before.contains("BULLISH") && after.contains("BULLISH"))
                || (before.contains("BEARISH") && after.contains("BEARISH"))
                || ("RANGE".equals(before) && "RANGE".equals(after));
    }

    private String cacheKey(AiDecisionChainRequest request) {
        try {
            Map<String, Object> canonical = new java.util.TreeMap<>();
            canonical.put("role", request.getRole().name());
            canonical.put("symbol", normalizeCacheText(request.getSymbol(), true));
            canonical.put("timeframe", normalizeCacheText(request.getTimeframe(), false));
            canonical.put("ruleVersion", request.getRuleVersion());
            canonical.put("input", canonicalCacheNode(objectMapper.valueToTree(request.getInput())));
            String json = objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(canonical);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return java.util.HexFormat.of().formatHex(digest.digest(json.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            return null;
        }
    }

    private CachedResult cachedResult(String key) {
        if (key == null) return null;
        long now = System.currentTimeMillis();
        CachedResult cached = decisionChainCache.get(key);
        if (cached == null) return null;
        if (cached.expiresAtEpochMs() <= now) {
            decisionChainCache.remove(key, cached);
            return null;
        }
        return cached;
    }

    private void cache(String key, AiDecisionChainRequest request, AiDecisionChainResult result) {
        long now = System.currentTimeMillis();
        decisionChainCache.entrySet().removeIf(entry -> entry.getValue().expiresAtEpochMs() <= now);
        int maxEntries = v41Properties.getAiGate().getCacheMaxEntries();
        if (decisionChainCache.size() >= maxEntries) {
            decisionChainCache.entrySet().stream()
                    .min(Comparator.comparingLong(entry -> entry.getValue().expiresAtEpochMs()))
                    .ifPresent(entry -> decisionChainCache.remove(entry.getKey(), entry.getValue()));
        }
        long ttlMs = Math.multiplyExact(v41Properties.getAiGate().getCacheTtlSeconds().longValue(), 1_000L);
        JsonNode input = objectMapper.valueToTree(request.getInput());
        decisionChainCache.put(key, new CachedResult(copy(result), input.deepCopy(), now + ttlMs));
    }

    private JsonNode canonicalCacheNode(JsonNode source) {
        if (source == null || source.isNull()) return objectMapper.nullNode();
        if (source.isObject()) {
            ObjectNode canonical = objectMapper.createObjectNode();
            java.util.TreeSet<String> names = new java.util.TreeSet<>();
            source.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                if (!CACHE_VOLATILE_FIELDS.contains(name)) {
                    canonical.set(name, canonicalCacheNode(source.get(name)));
                }
            }
            return canonical;
        }
        if (source.isArray()) {
            ArrayNode canonical = objectMapper.createArrayNode();
            source.forEach(value -> canonical.add(canonicalCacheNode(value)));
            return canonical;
        }
        return source.deepCopy();
    }

    private AiDecisionChainResult rebindCachedResult(CachedResult cached,
                                                      AiDecisionChainRequest request) {
        try {
            AiDecisionChainResult result = copy(cached.result());
            JsonNode payload = objectMapper.readTree(result.getPayloadJson());
            if (payload == null || !payload.isObject()) return null;
            JsonNode currentInput = objectMapper.valueToTree(request.getInput());
            Map<String, String> referenceMap = evidenceReferenceMap(cached.input(), currentInput);
            rebindPayload(payload, referenceMap, request.getAnalysisId(), request.getTraceId());
            result.setPayloadJson(objectMapper.writeValueAsString(payload));
            return result;
        } catch (Exception invalidCachedPayload) {
            return null;
        }
    }

    private static Map<String, String> evidenceReferenceMap(JsonNode previousInput,
                                                            JsonNode currentInput) {
        JsonNode previousEvidence = previousInput == null ? null : previousInput.path("evidence");
        JsonNode currentEvidence = currentInput == null ? null : currentInput.path("evidence");
        if (previousEvidence == null || currentEvidence == null
                || !previousEvidence.isArray() || !currentEvidence.isArray()
                || previousEvidence.size() != currentEvidence.size()) {
            return Map.of();
        }
        Map<String, String> references = new LinkedHashMap<>();
        for (int index = 0; index < previousEvidence.size(); index++) {
            JsonNode previous = previousEvidence.get(index);
            JsonNode current = currentEvidence.get(index);
            for (String field : List.of("evidenceId", "sourceTraceId", "sourceReference", "source")) {
                String before = previous.path(field).asText(null);
                String after = current.path(field).asText(null);
                if (before != null && !before.isBlank() && after != null && !after.isBlank()) {
                    references.put(before, after);
                }
            }
        }
        return Map.copyOf(references);
    }

    private static void rebindPayload(JsonNode node,
                                      Map<String, String> references,
                                      String analysisId,
                                      String traceId) {
        if (node instanceof ObjectNode object) {
            List<String> names = new ArrayList<>();
            object.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                JsonNode value = object.get(name);
                if (value != null && value.isTextual()) {
                    String replacement = "analysisId".equals(name) ? analysisId
                            : "traceId".equals(name) ? traceId
                            : references.get(value.asText());
                    if (replacement != null) object.put(name, replacement);
                } else {
                    rebindPayload(value, references, analysisId, traceId);
                }
            }
        } else if (node instanceof ArrayNode array) {
            for (int index = 0; index < array.size(); index++) {
                JsonNode value = array.get(index);
                if (value != null && value.isTextual()) {
                    String replacement = references.get(value.asText());
                    if (replacement != null) array.set(index, objectTextNode(replacement));
                } else {
                    rebindPayload(value, references, analysisId, traceId);
                }
            }
        }
    }

    private static JsonNode objectTextNode(String value) {
        return com.fasterxml.jackson.databind.node.TextNode.valueOf(value);
    }

    private static String normalizeCacheText(String value, boolean upperCase) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        return upperCase ? normalized.toUpperCase(java.util.Locale.ROOT)
                : normalized.toLowerCase(java.util.Locale.ROOT);
    }

    private static AiDecisionChainResult copy(AiDecisionChainResult source) {
        AiDecisionChainResult target = new AiDecisionChainResult();
        target.setProvider(source.getProvider());
        target.setRole(source.getRole());
        target.setCallStatus(source.getCallStatus());
        target.setPayloadJson(source.getPayloadJson());
        target.setAuditOutput(source.getAuditOutput());
        target.setSelectedModel(source.getSelectedModel());
        target.setFallback(source.isFallback());
        target.setFallbackReason(source.getFallbackReason());
        target.setErrorCode(source.getErrorCode());
        target.setCalculatedCostUsd(BigDecimal.ZERO);
        target.setReservedCostUsd(BigDecimal.ZERO);
        return target;
    }

    private record CachedResult(AiDecisionChainResult result, JsonNode input, long expiresAtEpochMs) {
    }

    private record QuotaBlock(AiProviderCallStatus status, String reasonCode) {
    }

    private static long elapsedMs(long startedNanos) {
        return Math.max(0L, (System.nanoTime() - startedNanos) / 1_000_000L);
    }

    private static AiProviderRole providerRole(AiDecisionChainRole role) {
        return switch (role) {
            case GPT_FINAL -> AiProviderRole.GPT_RULE_REVIEW;
            case GEMINI_REVIEW -> AiProviderRole.GEMINI_CONSISTENCY_REVIEW;
            case GROK_CHALLENGE -> AiProviderRole.GROK_ADVERSARIAL_CHALLENGE;
        };
    }

    private static AiProviderName providerName(AiDecisionChainRole role) {
        return switch (role) {
            case GPT_FINAL -> AiProviderName.OPENAI;
            case GEMINI_REVIEW -> AiProviderName.GEMINI;
            case GROK_CHALLENGE -> AiProviderName.XAI;
        };
    }
}
