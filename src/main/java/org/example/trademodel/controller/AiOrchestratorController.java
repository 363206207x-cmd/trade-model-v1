package org.example.trademodel.controller;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderProperties;
import org.example.trademodel.ai.AiProviderReadiness;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderReadinessService;
import org.example.trademodel.ai.AiProviderRuntimeReadiness;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.service.AiCallLogService;
import org.example.trademodel.service.AiDecisionOrchestratorService;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiOrchestratorController {
    private final AiDecisionOrchestratorService orchestratorService;
    private final AiCallLogService callLogService;
    private final AiOrchestratorProperties properties;
    private final List<AiProviderClient> providerClients;
    private final AuthenticatedUserIdResolver userIdResolver;
    private final AiProviderReadinessService readinessService;

    public AiOrchestratorController(AiDecisionOrchestratorService orchestratorService,
                                    AiCallLogService callLogService,
                                    AiOrchestratorProperties properties,
                                    List<AiProviderClient> providerClients) {
        this(orchestratorService, callLogService, properties, providerClients, null);
    }

    public AiOrchestratorController(AiDecisionOrchestratorService orchestratorService,
                                    AiCallLogService callLogService,
                                    AiOrchestratorProperties properties,
                                    List<AiProviderClient> providerClients,
                                    AuthenticatedUserIdResolver userIdResolver) {
        this(orchestratorService, callLogService, properties, providerClients, userIdResolver, null);
    }

    @Autowired
    public AiOrchestratorController(AiDecisionOrchestratorService orchestratorService,
                                    AiCallLogService callLogService,
                                    AiOrchestratorProperties properties,
                                    List<AiProviderClient> providerClients,
                                    AuthenticatedUserIdResolver userIdResolver,
                                    AiProviderReadinessService readinessService) {
        this.orchestratorService = orchestratorService;
        this.callLogService = callLogService;
        this.properties = properties;
        this.providerClients = providerClients == null ? List.of() : providerClients.stream()
                .sorted(Comparator.comparing(client -> client.role().name()))
                .toList();
        this.userIdResolver = userIdResolver;
        this.readinessService = readinessService;
    }

    @GetMapping("/orchestrator/status")
    public Map<String, Object> status() {
        Map<String, Object> map = safetyMap();
        map.put("globalEnabled", properties.isEnabled());
        map.put("requestTimeoutMs", properties.getRequestTimeoutMs());
        map.put("overallTimeoutMs", properties.getOverallTimeoutMs());
        AiOrchestratorProperties.ProviderTimeouts providerTimeouts = properties.getProviderTimeouts();
        map.put("providerTimeouts", Map.of(
                "openaiMs", providerTimeouts.getOpenaiMs(),
                "geminiMs", providerTimeouts.getGeminiMs(),
                "xaiMs", providerTimeouts.getXaiMs(),
                "overallMs", providerTimeouts.getOverallMs(),
                "configurationValid", providerTimeouts.validOverall()
                        && providerTimeouts.validProvider(org.example.trademodel.ai.AiProviderName.OPENAI)
                        && providerTimeouts.validProvider(org.example.trademodel.ai.AiProviderName.GEMINI)
                        && providerTimeouts.validProvider(org.example.trademodel.ai.AiProviderName.XAI)));
        map.put("maxInputChars", properties.getMaxInputChars());
        map.put("maxOutputTokens", properties.getMaxOutputTokens());
        map.put("dailyBudgetConfiguration", properties.dailyBudgetPresence().name());
        map.put("perAnalysisBudgetConfiguration", properties.perAnalysisBudgetPresence().name());
        map.put("fallbackMode", AiOrchestrationMode.RULE_ONLY_FALLBACK.name());
        map.put("modelStrategy", Map.of(
                "GPT_FINAL", properties.getModelStrategy().getGptFinal().getPriority().name(),
                "GEMINI_REVIEW", properties.getModelStrategy().getGeminiReview().getPriority().name(),
                "GROK_CHALLENGE", properties.getModelStrategy().getGrokChallenge().getPriority().name()));
        map.put("providers", readinessService == null
                ? providerClients.stream().map(this::providerStatus).toList()
                : readinessService.readiness().stream().map(AiOrchestratorController::runtimeProviderStatus).toList());
        return map;
    }

    @PostMapping("/providers/{provider}/reverify")
    public AiProviderRuntimeReadiness reverify(@PathVariable String provider) {
        if (userIdResolver == null || readinessService == null) {
            throw new IllegalStateException("Authenticated AI provider verification is unavailable");
        }
        userIdResolver.requireCurrentUserId();
        return readinessService.reverify(AiProviderName.valueOf(normalize(provider)));
    }

    @GetMapping("/call-logs")
    public List<Map<String, Object>> callLogs(@RequestParam(required = false) String analysisId,
                                              @RequestParam(required = false) String traceId,
                                              @RequestParam(required = false) String candidateId,
                                              @RequestParam(required = false) String role,
                                              @RequestParam(required = false) String providerName,
                                              @RequestParam(required = false) String callStatus,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                              @RequestParam(defaultValue = "100") int limit) {
        if (userIdResolver == null) {
            throw new IllegalStateException("Authenticated AI trace query is unavailable");
        }
        return callLogService.queryOwned(userIdResolver.requireCurrentUserId(), analysisId, traceId,
                        candidateId, normalize(role), normalize(providerName), normalize(callStatus),
                        from, to, limit)
                .stream()
                .map(AiOrchestratorController::logMap)
                .toList();
    }

    private Map<String, Object> providerStatus(AiProviderClient client) {
        AiProviderReadiness readiness = client.readiness();
        AiProviderProperties providerProperties = client.providerProperties();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("provider", readiness.getProvider().name());
        map.put("role", readiness.getRole().name());
        map.put("enabled", readiness.isEnabled());
        map.put("configured", readiness.isConfigured());
        map.put("ready", readiness.isReady() && !readiness.isFallbackUsed());
        map.put("modelReadiness", readiness.getModelReadinessStatus().name());
        map.put("configuredModel", readiness.getConfiguredModel());
        map.put("effectiveModel", readiness.getEffectiveModel());
        map.put("fallbackUsed", readiness.isFallbackUsed());
        map.put("fallbackReason", readiness.getFallbackReason());
        map.put("modelStrategy", readiness.getModelStrategy());
        map.put("requestsPerMinuteConfiguration", providerProperties.requestsPerMinutePresence().name());
        map.put("inputCostConfiguration", providerProperties.inputCostPresence().name());
        map.put("outputCostConfiguration", providerProperties.outputCostPresence().name());
        map.put("reasonCodes", readiness.getReasonCodes());
        return map;
    }

    private static Map<String, Object> runtimeProviderStatus(AiProviderRuntimeReadiness readiness) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("provider", readiness.provider());
        map.put("model", readiness.model());
        map.put("state", readiness.state().name());
        map.put("ready", readiness.ready());
        map.put("verifiedAt", readiness.verifiedAt());
        map.put("expiresAt", readiness.expiresAt());
        map.put("reasonCode", readiness.reasonCode());
        map.put("requestId", readiness.requestId());
        map.put("configVersion", readiness.configVersion());
        map.put("rpmConfiguration", readiness.rpmConfiguration().name());
        map.put("inputCostConfiguration", readiness.inputCostConfiguration().name());
        map.put("outputCostConfiguration", readiness.outputCostConfiguration().name());
        map.put("dailyBudgetConfiguration", readiness.dailyBudgetConfiguration().name());
        map.put("perAnalysisBudgetConfiguration", readiness.perAnalysisBudgetConfiguration().name());
        map.put("fallbackReady", false);
        return map;
    }

    private static Map<String, Object> logMap(AiCallLogDO log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reviewOnly", log.getReviewOnly());
        map.put("manualReviewOnly", log.getManualReviewOnly());
        map.put("notTradeInstruction", log.getNotTradeInstruction());
        map.put("notExecutable", log.getNotExecutable());
        map.put("notAutoTrading", log.getNotAutoTrading());
        map.put("notOrderExecution", log.getNotOrderExecution());
        map.put("notUserPositionCreation", log.getNotUserPositionCreation());
        map.put("notPositionMutation", log.getNotPositionMutation());
        map.put("notStateMachineOverride", log.getNotStateMachineOverride());
        map.put("notExecutionPlanCreation", log.getNotExecutionPlanCreation());
        map.put("notFinalExecutionPlanCreation", log.getNotFinalExecutionPlanCreation());
        map.put("ruleDirectionPreserved", log.getRuleDirectionPreserved());
        map.put("callId", log.getCallId());
        map.put("analysisId", log.getAnalysisId());
        map.put("traceId", log.getTraceId());
        map.put("requestId", log.getRequestId());
        map.put("opportunityId", log.getOpportunityId());
        map.put("providerName", log.getProviderName());
        map.put("modelName", log.getModelName());
        map.put("aiRole", log.getAiRole());
        map.put("callStatus", log.getCallStatus());
        map.put("providerRequestId", log.getProviderRequestId());
        map.put("startedAt", log.getStartedAt());
        map.put("completedAt", log.getCompletedAt());
        map.put("latencyMs", log.getLatencyMs());
        map.put("inputTokens", log.getInputTokens());
        map.put("outputTokens", log.getOutputTokens());
        map.put("totalTokens", log.getTotalTokens());
        map.put("reservedCostUsd", log.getReservedCostUsd());
        map.put("calculatedCostUsd", log.getCalculatedCostUsd());
        map.put("costCurrency", log.getCostCurrency());
        map.put("fallbackFlag", log.getFallbackFlag());
        map.put("fallbackReason", log.getFallbackReason());
        map.put("rateLimited", log.getRateLimited());
        map.put("budgetBlocked", log.getBudgetBlocked());
        map.put("timeoutFlag", log.getTimeoutFlag());
        map.put("errorCode", log.getErrorCode());
        map.put("errorMessage", log.getErrorMessage());
        map.put("requestHash", log.getRequestHash());
        map.put("requestSummary", log.getRequestSummary());
        map.put("responseSummary", log.getResponseSummary());
        map.put("contractType", log.getContractType());
        map.put("candidateId", log.getCandidateId());
        map.put("cacheHit", log.getCacheHit());
        map.put("observedAt", log.getObservedAt());
        map.put("createdAt", log.getCreatedAt());
        map.put("ruleVersion", log.getRuleVersion());
        map.put("outputPayload", log.getOutputPayload());
        return map;
    }

    private static Map<String, Object> safetyMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("reviewOnly", true);
        map.put("manualReviewOnly", true);
        map.put("notTradeInstruction", true);
        map.put("notExecutable", true);
        map.put("notAutoTrading", true);
        map.put("notOrderExecution", true);
        map.put("notUserPositionCreation", true);
        map.put("notPositionMutation", true);
        map.put("notStateMachineOverride", true);
        map.put("notExecutionPlanCreation", true);
        map.put("notFinalExecutionPlanCreation", true);
        map.put("ruleDirectionPreserved", true);
        return map;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
