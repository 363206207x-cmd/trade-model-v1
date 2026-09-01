package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
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
        AiCallLogDO log = newDecisionChainLog(request, client.provider(),
                client.providerProperties() == null ? null : client.providerProperties().getEffectiveModel(),
                reservedCostUsd);
        log.setCallStatus("STARTED");
        mapper.insert(log);
        return log;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeDecisionChainCall(AiCallLogDO log, AiDecisionChainResult result) {
        if (log == null || result == null) return;
        fillDecisionChainCompletion(log, result);
        mapper.updateCompletion(log);
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
        AiCallLogDO log = newDecisionChainLog(request, provider, modelName, reservedCostUsd);
        fillDecisionChainCompletion(log, result);
        mapper.insert(log);
        return log;
    }

    private void fillDecisionChainCompletion(AiCallLogDO log, AiDecisionChainResult result) {
        LocalDateTime now = LocalDateTime.now();
        if (result.getSelectedModel() != null && !result.getSelectedModel().isBlank()) {
            log.setModelName(safe(result.getSelectedModel(), 128));
        }
        log.setCallStatus(result.getCallStatus() == null ? "FAILED" : result.getCallStatus().name());
        log.setProviderRequestId(safe(result.getProviderRequestId(), 128));
        log.setCompletedAt(now);
        log.setLatencyMs(result.getLatencyMs());
        log.setInputTokens(result.getInputTokens());
        log.setOutputTokens(result.getOutputTokens());
        log.setTotalTokens(result.getTotalTokens());
        log.setCalculatedCostUsd(result.getCalculatedCostUsd());
        log.setFallbackFlag(result.isFallback());
        log.setFallbackReason(safe(result.getFallbackReason(), 512));
        log.setTimeoutFlag(result.getCallStatus() == org.example.trademodel.ai.AiProviderCallStatus.TIMEOUT);
        log.setErrorCode(safe(result.getErrorCode(), 128));
        log.setErrorMessage(safe(result.getFallbackReason(), 512));
        log.setCacheHit(result.isCacheHit());
        log.setObservedAt(result.getGeneratedAt() == null
                ? now : result.getGeneratedAt().toLocalDateTime());
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
        LocalDateTime now = LocalDateTime.now();
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
                                            BigDecimal reservedCostUsd) {
        LocalDateTime now = LocalDateTime.now();
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
        String canonicalInput = canonicalDecisionChainRequest(request);
        log.setRequestSummary(truncate(canonicalInput, DECISION_CHAIN_SUMMARY_CHARS));
        log.setRequestHash(hash(canonicalInput));
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

    private String canonicalDecisionChainRequest(AiDecisionChainRequest request) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("analysisId", request.getAnalysisId());
        summary.put("traceId", request.getTraceId());
        summary.put("candidateId", request.getCandidateId());
        summary.put("opportunityId", request.getOpportunityId());
        summary.put("requestId", request.getRequestId());
        summary.put("ruleVersion", request.getRuleVersion());
        summary.put("role", request.getRole() == null ? null : request.getRole().name());
        summary.put("symbol", request.getSymbol());
        summary.put("timeframe", request.getTimeframe());
        summary.put("input", request.getInput());
        try {
            String value = objectMapper.writer()
                    .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                    .writeValueAsString(summary);
            return sanitize(value);
        } catch (Exception exception) {
            return "{}";
        }
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
}
