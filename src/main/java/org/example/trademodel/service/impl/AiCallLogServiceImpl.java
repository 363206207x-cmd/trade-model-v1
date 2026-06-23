package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.mapper.AiCallLogMapper;
import org.example.trademodel.service.AiCallLogService;
import org.springframework.stereotype.Service;

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
        return mapper.query(analysisId, traceId, providerName, callStatus,
                from, to, Math.max(1, Math.min(500, limit)));
    }

    @Override
    public int countProviderAttemptsSince(String providerName, LocalDateTime since) {
        return mapper.countProviderAttemptsSince(providerName, since);
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
        log.setModelName(safe(client.providerProperties().getModel(), 128));
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

    private void fillCompletion(AiCallLogDO log, AiProviderReviewResult result) {
        LocalDateTime completedAt = LocalDateTime.now();
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
        if (value == null) {
            return null;
        }
        String sanitized = value.replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-]{8,}", "Bearer ***")
                .replaceAll("sk-[A-Za-z0-9]+", "sk-***")
                .replaceAll("AIza[A-Za-z0-9_\\-]+", "AIza***")
                .replaceAll("xai-[A-Za-z0-9]+", "xai-***");
        return sanitized.length() <= max ? sanitized : sanitized.substring(0, max);
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
