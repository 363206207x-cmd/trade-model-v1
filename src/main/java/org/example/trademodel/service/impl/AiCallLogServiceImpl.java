package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
import org.example.trademodel.ai.AiDecisionChainPromptBuilder;
import org.example.trademodel.ai.AiBackgroundTaskState;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiRoleDataState;
import org.example.trademodel.ai.AiRoleState;
import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.mapper.AiCallLogMapper;
import org.example.trademodel.service.AiCallLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiCallLogServiceImpl implements AiCallLogService {
    private static final int DECISION_CHAIN_SUMMARY_CHARS = 8_000;
    private static final int DECISION_CHAIN_OUTPUT_CHARS = 65_536;

    private final AiCallLogMapper mapper;
    private final ObjectMapper objectMapper;

    public AiCallLogServiceImpl(AiCallLogMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiCallLogDO startCall(AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd) {
        AiCallLogDO log = baseLog(request, client, reservedCostUsd);
        log.setCallStatus("STARTED");
        mapper.insert(log);
        return log;
    }

    @Override
    public void completeCall(AiCallLogDO log, AiProviderReviewResult result) {
        if (log == null || result == null) {
            return;
        }
        fillCompletion(log, result);
        mapper.updateCompletion(log);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiCallLogDO startDecisionChainCall(AiDecisionChainRequest request,
                                              AiProviderClient client,
                                              BigDecimal reservedCostUsd) {
        return startDecisionChainCall(request, client, reservedCostUsd, 1);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiCallLogDO startDecisionChainCall(AiDecisionChainRequest request,
                                              AiProviderClient client,
                                              BigDecimal reservedCostUsd,
                                              int attempt) {
        AiCallLogDO log = newDecisionChainLog(request, client.provider(),
                decisionChainModel(request, client),
                reservedCostUsd, attempt);
        log.setCallStatus("STARTED");
        if (mapper.insert(log) != 1) {
            throw new IllegalStateException("AI_TRACE_START_NOT_PERSISTED");
        }
        return log;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeDecisionChainCall(AiCallLogDO log, AiDecisionChainResult result) {
        if (log == null || result == null) return;
        fillDecisionChainCompletion(log, result);
        if (mapper.updateCompletion(log) != 1) {
            throw new IllegalStateException("AI_TRACE_COMPLETION_NOT_PERSISTED");
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDecisionChainTask(AiCallLogDO log, AiDecisionChainResult result) {
        if (log == null || result == null) return;
        fillDecisionChainCompletion(log, result);
        if (mapper.updateDecisionChainTask(log) != 1) {
            throw new IllegalStateException("AI_TRACE_TASK_STATE_NOT_PERSISTED");
        }
    }

    @Override
    public AiCallLogDO findLatestDecisionChainTask(String analysisId, String role) {
        if (analysisId == null || analysisId.isBlank() || role == null || role.isBlank()) return null;
        return mapper.selectLatestDecisionChainTask(analysisId.trim(), role.trim());
    }

    @Override
    public AiCallLogDO findLatestDecisionChainTask(String analysisId, String role,
                                                   String inputHash) {
        if (analysisId == null || analysisId.isBlank() || role == null || role.isBlank()
                || inputHash == null || inputHash.isBlank()) return null;
        return mapper.selectLatestDecisionChainTaskByInputHash(
                analysisId.trim(), role.trim(), inputHash.trim());
    }

    @Override
    public AiDecisionChainResult restoreDecisionChainResult(AiCallLogDO log) {
        if (log == null) return null;
        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setProvider(enumValue(AiProviderName.class, log.getProviderName()));
        result.setRole(enumValue(AiDecisionChainRole.class, log.getAiRole()));
        result.setCallStatus(enumValue(AiProviderCallStatus.class, log.getCallStatus()));
        result.setProviderRequestId(log.getProviderRequestId());
        result.setSelectedModel(log.getModelName());
        result.setPayloadJson(log.getOutputPayload());
        result.setAuditOutput(log.getOutputPayload());
        result.setLatencyMs(log.getLatencyMs());
        result.setInputTokens(log.getInputTokens());
        result.setOutputTokens(log.getOutputTokens());
        result.setTotalTokens(log.getTotalTokens());
        result.setReasoningTokens(log.getReasoningTokens());
        result.setCalculatedCostUsd(log.getCalculatedCostUsd());
        result.setReservedCostUsd(log.getReservedCostUsd());
        result.setFallback(Boolean.TRUE.equals(log.getFallbackFlag()));
        result.setFallbackReason(log.getFallbackReason());
        result.setErrorCode(log.getErrorCode());
        result.setCacheHit(Boolean.TRUE.equals(log.getCacheHit()));
        result.setAnalysisId(log.getAnalysisId());
        result.setTraceId(log.getTraceId());
        result.setTaskState(enumValue(AiBackgroundTaskState.class, log.getTaskState()));
        result.setAttempt(log.getAttempt());
        result.setRoleState(enumValue(AiRoleState.class, log.getRoleState()));
        result.setDataState(enumValue(AiRoleDataState.class, log.getDataState()));
        result.setFailureClassification(log.getFailureClassification());
        result.setSubmittedAt(utc(log.getSubmittedAt()));
        result.setStartedAt(utc(log.getStartedAt()));
        result.setCompletedAt(utc(log.getCompletedAt()));
        result.setGeneratedAt(utc(log.getObservedAt()));
        result.setBackgroundMode(log.getBackgroundMode());
        return result;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AiCallLogDO recordDecisionChainResult(AiDecisionChainRequest request,
                                                 AiProviderName provider,
                                                 String modelName,
                                                 AiDecisionChainResult result,
                                                 BigDecimal reservedCostUsd) {
        if (request == null || request.getRole() == null || result == null) {
            throw new IllegalArgumentException("decision-chain request, role and result are required");
        }
        AiCallLogDO log = newDecisionChainLog(request, provider, modelName, reservedCostUsd, 1);
        fillDecisionChainCompletion(log, result);
        if (mapper.insert(log) != 1) {
            throw new IllegalStateException("AI_TRACE_TERMINAL_RESULT_NOT_PERSISTED");
        }
        return log;
    }

    private void fillDecisionChainCompletion(AiCallLogDO log, AiDecisionChainResult result) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (result.getSelectedModel() != null && !result.getSelectedModel().isBlank()) {
            log.setModelName(safe(result.getSelectedModel(), 128));
        }
        AiBackgroundTaskState taskState = result.getTaskState();
        if (taskState == null) {
            taskState = result.successful() ? AiBackgroundTaskState.SUCCEEDED
                    : result.getCallStatus() == AiProviderCallStatus.TIMEOUT
                    ? AiBackgroundTaskState.TIMED_OUT : AiBackgroundTaskState.FAILED;
        }
        log.setTaskState(taskState.name());
        log.setAttempt(result.getAttempt() == null ? log.getAttempt() : result.getAttempt());
        log.setCallStatus(taskState.active() ? "STARTED"
                : result.getCallStatus() == null ? "FAILED" : result.getCallStatus().name());
        log.setProviderRequestId(safe(result.getProviderRequestId(), 128));
        if (result.getSubmittedAt() != null && log.getSubmittedAt() == null) {
            log.setSubmittedAt(result.getSubmittedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        } else if (taskState == AiBackgroundTaskState.SUBMITTED || taskState == AiBackgroundTaskState.RUNNING) {
            if (log.getSubmittedAt() == null) log.setSubmittedAt(now);
        }
        if (result.getStartedAt() != null) {
            log.setStartedAt(result.getStartedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        }
        log.setCompletedAt(taskState.terminal() ? now : null);
        log.setLatencyMs(result.getLatencyMs());
        log.setInputTokens(result.getInputTokens());
        log.setOutputTokens(result.getOutputTokens());
        log.setTotalTokens(result.getTotalTokens());
        log.setReasoningTokens(result.getReasoningTokens());
        log.setCalculatedCostUsd(result.getCalculatedCostUsd());
        log.setFallbackFlag(result.isFallback());
        log.setFallbackReason(safe(result.getFallbackReason(), 512));
        log.setTimeoutFlag(result.getCallStatus() == org.example.trademodel.ai.AiProviderCallStatus.TIMEOUT);
        log.setErrorCode(safe(result.getErrorCode(), 128));
        log.setErrorMessage(safe(result.getFallbackReason(), 512));
        log.setFailureClassification(safe(result.getFailureClassification(), 128));
        log.setCacheHit(result.isCacheHit());
        log.setRoleState(result.getRoleState() == null ? null : result.getRoleState().name());
        log.setDataState(result.getDataState() == null ? null : result.getDataState().name());
        log.setBackgroundMode(safe(result.getBackgroundMode(), 64));
        log.setActiveTaskKey(taskState.active()
                ? activeTaskKey(log.getAnalysisId(), log.getAiRole(), log.getRequestHash()) : null);
        log.setObservedAt(result.getGeneratedAt() == null
                ? now : result.getGeneratedAt().withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
        log.setResponseSummary(decisionChainResponseSummary(result));
        String auditOutput = result.getAuditOutput() == null
                ? result.getPayloadJson() : result.getAuditOutput();
        log.setOutputPayload(safe(auditOutput, DECISION_CHAIN_OUTPUT_CHARS));
        log.setUpdatedAt(now);
    }

    @Override
    public AiCallLogDO recordSkipped(AiProviderRequest request, AiProviderClient client,
                                     AiProviderReviewResult result, BigDecimal reservedCostUsd) {
        AiCallLogDO log = baseLog(request, client, reservedCostUsd);
        fillCompletion(log, result);
        mapper.insert(log);
        return log;
    }

    @Override
    public List<AiCallLogDO> query(String analysisId, String traceId, String providerName, String callStatus,
                                   LocalDateTime from, LocalDateTime to, int limit) {
        return mapper.query(analysisId, traceId, null, null, providerName, callStatus,
                from, to, Math.max(1, Math.min(500, limit)));
    }

    @Override
    public List<AiCallLogDO> queryOwned(Long userId, String analysisId, String traceId,
                                        String candidateId, String role, String providerName,
                                        String callStatus, LocalDateTime from, LocalDateTime to,
                                        int limit) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId is required");
        }
        return mapper.queryOwned(userId, safe(analysisId, 64), safe(traceId, 128),
                safe(candidateId, 64), safe(role, 64), safe(providerName, 32),
                safe(callStatus, 32), from, to, Math.max(1, Math.min(500, limit)));
    }

    @Override
    public int countProviderAttemptsSince(String providerName, LocalDateTime since) {
        return mapper.countProviderAttemptsSince(providerName, since);
    }

    @Override
    public int countDecisionChainAttemptsSince(LocalDateTime since) {
        return mapper.countDecisionChainAttemptsSince(since);
    }

    @Override
    public int countDecisionChainRoleAttempts(String analysisId, String role) {
        return mapper.countDecisionChainRoleAttempts(analysisId, role);
    }

    @Override
    public long sumDecisionChainTokensSince(LocalDateTime since) {
        return mapper.sumDecisionChainTokensSince(since);
    }

    @Override
    public long sumDecisionChainTokensByAnalysisId(String analysisId) {
        return mapper.sumDecisionChainTokensByAnalysisId(analysisId);
    }

    @Override
    public BigDecimal sumChargeableCostSince(LocalDateTime since) {
        BigDecimal sum = mapper.sumChargeableCostSince(since);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    @Override
    public BigDecimal sumChargeableCostByAnalysisId(String analysisId) {
        BigDecimal sum = mapper.sumChargeableCostByAnalysisId(analysisId);
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private AiCallLogDO baseLog(AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        AiCallLogDO log = new AiCallLogDO();
        log.setCallId("ai-call-" + UUID.randomUUID());
        log.setAnalysisId(safe(request.getAnalysisId(), 64));
        log.setTraceId(safe(request.getTraceId(), 64));
        log.setRequestId("ai-req-" + UUID.randomUUID());
        log.setProviderName(client.provider().name());
        log.setModelName(safe(client.providerProperties().getEffectiveModel(), 128));
        log.setAiRole(client.role().name());
        log.setStartedAt(now);
        log.setReservedCostUsd(reservedCostUsd == null ? BigDecimal.ZERO : reservedCostUsd);
        log.setCalculatedCostUsd(BigDecimal.ZERO);
        log.setRequestSummary(requestSummary(request));
        log.setRequestHash(hash(log.getRequestSummary()));
        log.setRuleVersion("P2-2");
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        return log;
    }

    private AiCallLogDO newDecisionChainLog(AiDecisionChainRequest request,
                                            AiProviderName provider,
                                            String modelName,
                                            BigDecimal reservedCostUsd,
                                            int attempt) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        AiCallLogDO log = new AiCallLogDO();
        log.setCallId("ai-call-" + UUID.randomUUID());
        log.setAnalysisId(safe(request.getAnalysisId(), 64));
        log.setTraceId(safe(request.getTraceId(), 128));
        log.setRequestId(safe(request.getRequestId(), 128) == null
                ? "ai-req-" + UUID.randomUUID() : safe(request.getRequestId(), 128));
        log.setOpportunityId(safe(request.getOpportunityId(), 64));
        log.setProviderName(provider == null ? "UNKNOWN" : provider.name());
        log.setModelName(safe(modelName == null || modelName.isBlank() ? "UNAVAILABLE" : modelName, 128));
        log.setAiRole(request.getRole().name());
        log.setStartedAt(now);
        log.setReservedCostUsd(reservedCostUsd == null ? BigDecimal.ZERO : reservedCostUsd);
        log.setCalculatedCostUsd(BigDecimal.ZERO);
        log.setContractType("DECISION_CHAIN_V4_1");
        log.setCandidateId(safe(request.getCandidateId(), 64));
        log.setCacheHit(false);
        log.setObservedAt(now);
        String canonicalInput = AiDecisionChainPromptBuilder.canonicalInputJson(objectMapper, request);
        log.setRequestSummary(truncate(canonicalInput, DECISION_CHAIN_SUMMARY_CHARS));
        log.setRequestHash(hash(canonicalInput));
        log.setTaskState(AiBackgroundTaskState.QUEUED.name());
        log.setAttempt(Math.max(1, attempt));
        log.setPromptVersion(AiOrchestratorProperties.BackgroundExecution.PROMPT_VERSION);
        log.setSchemaVersion(AiOrchestratorProperties.BackgroundExecution.SCHEMA_VERSION);
        log.setInputContractVersion(AiOrchestratorProperties.BackgroundExecution.INPUT_CONTRACT_VERSION);
        log.setRuntimeConfigVersion(AiOrchestratorProperties.BackgroundExecution.RUNTIME_CONFIG_VERSION);
        log.setActiveTaskKey(activeTaskKey(
                log.getAnalysisId(), log.getAiRole(), log.getRequestHash()));
        log.setRuleVersion(safe(request.getRuleVersion(), 32) == null
                ? "FUNDAMENTAL_AI_V4_1" : safe(request.getRuleVersion(), 32));
        boolean reviewOnly = request.getRole() != AiDecisionChainRole.GPT_FINAL;
        log.setReviewOnly(reviewOnly);
        log.setNotExecutionPlanCreation(reviewOnly);
        log.setNotFinalExecutionPlanCreation(true);
        log.setManualReviewOnly(true);
        log.setNotTradeInstruction(true);
        log.setNotExecutable(true);
        log.setNotAutoTrading(true);
        log.setNotOrderExecution(true);
        log.setNotUserPositionCreation(true);
        log.setNotPositionMutation(true);
        log.setNotStateMachineOverride(true);
        log.setRuleDirectionPreserved(true);
        log.setCreatedAt(now);
        log.setUpdatedAt(now);
        return log;
    }

    private void fillCompletion(AiCallLogDO log, AiProviderReviewResult result) {
        LocalDateTime completedAt = LocalDateTime.now();
        if (result.getSelectedModel() != null && !result.getSelectedModel().isBlank()) {
            log.setModelName(safe(result.getSelectedModel(), 128));
        }
        log.setCallStatus(result.getCallStatus() == null ? "FAILED" : result.getCallStatus().name());
        log.setProviderRequestId(safe(result.getProviderRequestId(), 128));
        log.setCompletedAt(completedAt);
        log.setLatencyMs(result.getLatencyMs());
        log.setInputTokens(result.getInputTokens());
        log.setOutputTokens(result.getOutputTokens());
        log.setTotalTokens(result.getTotalTokens());
        log.setCalculatedCostUsd(result.getCalculatedCostUsd() == null ? BigDecimal.ZERO : result.getCalculatedCostUsd());
        log.setFallbackFlag(result.isFallback());
        log.setFallbackReason(safe(result.getFallbackReason(), 512));
        log.setRateLimited(result.isRateLimited());
        log.setBudgetBlocked(result.isBudgetBlocked());
        log.setTimeoutFlag(result.isTimeout());
        log.setErrorCode(safe(result.getErrorCode(), 128));
        log.setErrorMessage(safe(result.getFallbackReason(), 512));
        log.setResponseSummary(responseSummary(result));
        log.setUpdatedAt(completedAt);
    }

    private String requestSummary(AiProviderRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("analysisId", request.getAnalysisId());
        summary.put("traceId", request.getTraceId());
        summary.put("symbol", request.getSymbol());
        summary.put("timeframe", request.getTimeframe());
        summary.put("ruleMarketBias", request.getRuleMarketBias());
        summary.put("ruleConfidence", request.getRuleConfidence());
        summary.put("ruleRiskLevel", request.getRuleRiskLevel());
        summary.put("ruleWorthOpening", request.getRuleWorthOpening());
        summary.put("multiTimeframeState", request.getMultiTimeframeState());
        summary.put("externalContextState", request.getExternalContextState());
        return json(summary, 1200);
    }

    private String responseSummary(AiProviderReviewResult result) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("callStatus", result.getCallStatus() == null ? null : result.getCallStatus().name());
        summary.put("stance", result.getStance().name());
        summary.put("conflictLevel", result.getConflictLevel().name());
        summary.put("reasonCodes", result.getReasonCodes());
        summary.put("summary", result.getSummary());
        summary.put("fallback", result.isFallback());
        summary.put("originalModel", result.getOriginalModel());
        summary.put("selectedModel", result.getSelectedModel());
        summary.put("fallbackLevel", result.getFallbackLevel());
        summary.put("fallbackReason", result.getFallbackReason());
        summary.put("modelStrategy", result.getModelStrategy());
        summary.put("modelRoutingTimestamp", result.getModelRoutingTimestamp() == null
                ? null : result.getModelRoutingTimestamp().toString());
        summary.put("modelRoutingTraceId", result.getModelRoutingTraceId());
        return json(summary, 1200);
    }

    private String decisionChainResponseSummary(AiDecisionChainResult result) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("role", result.getRole() == null ? null : result.getRole().name());
        summary.put("status", result.getCallStatus() == null ? null : result.getCallStatus().name());
        summary.put("fallback", result.isFallback());
        summary.put("fallbackReason", result.getFallbackReason());
        summary.put("selectedModel", result.getSelectedModel());
        return json(summary, 1200);
    }

    private String json(Object value, int max) {
        try {
            return safe(objectMapper.writeValueAsString(value), max);
        } catch (Exception e) {
            return "{}";
        }
    }

    private static String safe(String value, int max) {
        return truncate(sanitize(value), max);
    }

    private static String sanitize(String value) {
        if (value == null) return null;
        return value.replaceAll(
                        "(?i)(\\\"(?:api[_-]?key|api[_-]?secret|access[_-]?token|refresh[_-]?token|id[_-]?token|authorization|password|passphrase|client[_-]?secret|private[_-]?key|cookie|credentials?)\\\"\\s*:\\s*\\\")[^\\\"]*(\\\")",
                        "$1***$2")
                .replaceAll("(?i)((?:api[_-]?key|api[_-]?secret|access[_-]?token|refresh[_-]?token|authorization|password|client[_-]?secret)=)[^&\\s]+", "$1***")
                .replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-]{8,}", "Bearer ***")
                .replaceAll("sk-[A-Za-z0-9]+", "sk-***")
                .replaceAll("AIza[A-Za-z0-9_\\-]+", "AIza***")
                .replaceAll("xai-[A-Za-z0-9]+", "xai-***");
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            return "";
        }
    }

    private static OffsetDateTime utc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private static <T extends Enum<T>> T enumValue(Class<T> type, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.trim());
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static String activeTaskKey(String analysisId, String role, String inputHash) {
        if (analysisId == null || role == null || inputHash == null) return null;
        return hash(analysisId + "|" + role + "|" + inputHash);
    }

    private static String decisionChainModel(AiDecisionChainRequest request,
                                             AiProviderClient client) {
        if (client == null || client.providerProperties() == null) return null;
        if (request != null && request.getRole() == AiDecisionChainRole.GPT_FINAL
                && client.provider() == AiProviderName.OPENAI) {
            String reasoningModel = client.providerProperties().getGptFinal().getReasoningModel();
            if (reasoningModel != null && !reasoningModel.isBlank()) return reasoningModel.trim();
        }
        return client.providerProperties().getEffectiveModel();
    }
}
