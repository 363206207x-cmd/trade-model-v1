package org.example.trademodel.controller;

import org.example.trademodel.ai.AiOrchestrationMode;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderProperties;
import org.example.trademodel.ai.AiProviderReadiness;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.service.AiCallLogService;
import org.example.trademodel.service.AiDecisionOrchestratorService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
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

    public AiOrchestratorController(AiDecisionOrchestratorService orchestratorService,
                                    AiCallLogService callLogService,
                                    AiOrchestratorProperties properties,
                                    List<AiProviderClient> providerClients) {
        this.orchestratorService = orchestratorService;
        this.callLogService = callLogService;
        this.properties = properties;
        this.providerClients = providerClients == null ? List.of() : providerClients.stream()
                .sorted(Comparator.comparing(client -> client.role().name()))
                .toList();
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
        map.put("dailyBudgetConfigured", properties.getDailyBudgetUsd().signum() > 0);
        map.put("perAnalysisBudgetConfigured", properties.getPerAnalysisBudgetUsd().signum() > 0);
        map.put("fallbackMode", AiOrchestrationMode.RULE_ONLY_FALLBACK.name());
        map.put("modelStrategy", Map.of(
                "GPT_FINAL", properties.getModelStrategy().getGptFinal().getPriority().name(),
                "GEMINI_REVIEW", properties.getModelStrategy().getGeminiReview().getPriority().name(),
                "GROK_CHALLENGE", properties.getModelStrategy().getGrokChallenge().getPriority().name()));
        map.put("providers", providerClients.stream().map(this::providerStatus).toList());
        return map;
    }

    @GetMapping("/call-logs")
    public List<Map<String, Object>> callLogs(@RequestParam(required = false) String analysisId,
                                              @RequestParam(required = false) String traceId,
                                              @RequestParam(required = false) String providerName,
                                              @RequestParam(required = false) String callStatus,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
                                              @RequestParam(required = false)
                                              @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
                                              @RequestParam(defaultValue = "100") int limit) {
        return callLogService.query(analysisId, traceId, normalize(providerName), normalize(callStatus), from, to, limit)
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
        map.put("ready", readiness.isReady());
        map.put("modelReadiness", readiness.getModelReadinessStatus().name());
        map.put("configuredModel", readiness.getConfiguredModel());
        map.put("effectiveModel", readiness.getEffectiveModel());
        map.put("fallbackUsed", readiness.isFallbackUsed());
        map.put("fallbackReason", readiness.getFallbackReason());
        map.put("modelStrategy", readiness.getModelStrategy());
        map.put("requestsPerMinuteConfigured", providerProperties.getRequestsPerMinute() > 0);
        map.put("costRateConfigured", providerProperties.getInputCostPerMillionUsd().signum() > 0
                && providerProperties.getOutputCostPerMillionUsd().signum() > 0);
        map.put("reasonCodes", readiness.getReasonCodes());
        return map;
    }

    private static Map<String, Object> logMap(AiCallLogDO log) {
        Map<String, Object> map = safetyMap();
        map.put("callId", log.getCallId());
        map.put("analysisId", log.getAnalysisId());
        map.put("traceId", log.getTraceId());
        map.put("requestId", log.getRequestId());
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
        map.put("ruleDirectionPreserved", true);
        return map;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
