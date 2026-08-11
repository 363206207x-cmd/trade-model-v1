package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiProviderCallStatus;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderProperties;
import org.example.trademodel.ai.AiProviderReadiness;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.ai.AiReviewConflictLevel;
import org.example.trademodel.ai.AiReviewStance;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.mapper.AiCallLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class AiCallLogServiceImplTest {
    @Mock
    private AiCallLogMapper mapper;

    @Test
    void startCallInsertsStartedSanitizedSummaryBeforeCompletion() {
        AiCallLogServiceImpl service = new AiCallLogServiceImpl(mapper, new ObjectMapper());

        AiCallLogDO log = service.startCall(request(), client(), new BigDecimal("0.12345678"));

        ArgumentCaptor<AiCallLogDO> captor = ArgumentCaptor.forClass(AiCallLogDO.class);
        verify(mapper).insert(captor.capture());
        assertThat(log.getCallStatus()).isEqualTo("STARTED");
        assertThat(captor.getValue().getProviderName()).isEqualTo("OPENAI");
        assertThat(captor.getValue().getReservedCostUsd()).isEqualByComparingTo("0.12345678");
        assertThat(captor.getValue().getRequestSummary()).contains("ruleMarketBias");
        assertThat(captor.getValue().getRequestSummary()).doesNotContain("sk-");
        assertThat(captor.getValue().getRequestHash()).hasSize(64);
    }

    @Test
    void completeCallUpdatesCompletionFieldsAndRedactsResponseSummary() {
        AiCallLogServiceImpl service = new AiCallLogServiceImpl(mapper, new ObjectMapper());
        AiCallLogDO log = new AiCallLogDO();
        log.setCallId("call-1");
        AiProviderReviewResult result = new AiProviderReviewResult();
        result.setProvider(AiProviderName.OPENAI);
        result.setRole(AiProviderRole.GPT_RULE_REVIEW);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setStance(AiReviewStance.SUPPORT);
        result.setConflictLevel(AiReviewConflictLevel.NONE);
        result.setReasonCodes(List.of("OK"));
        result.setSummary("clean sk-should-redact");
        result.setInputTokens(10L);
        result.setOutputTokens(5L);
        result.setTotalTokens(15L);
        result.setCalculatedCostUsd(new BigDecimal("0.00001234"));
        result.setFallback(true);
        result.setFallbackReason("OPENAI_FALLBACK_GPT55");
        result.setOriginalModel("gpt-5.6-luna");
        result.setSelectedModel("gpt-5.5");
        result.setFallbackLevel(1);
        result.setModelStrategy("FAST_DECISION_MODEL");
        result.setModelRoutingTimestamp(LocalDateTime.of(2026, 7, 11, 12, 0));
        result.setModelRoutingTraceId("trace-1");

        service.completeCall(log, result);

        ArgumentCaptor<AiCallLogDO> captor = ArgumentCaptor.forClass(AiCallLogDO.class);
        verify(mapper).updateCompletion(captor.capture());
        assertThat(captor.getValue().getCallStatus()).isEqualTo("SUCCESS");
        assertThat(captor.getValue().getTotalTokens()).isEqualTo(15L);
        assertThat(captor.getValue().getModelName()).isEqualTo("gpt-5.5");
        assertThat(captor.getValue().getFallbackReason()).isEqualTo("OPENAI_FALLBACK_GPT55");
        assertThat(captor.getValue().getResponseSummary()).contains(
                "\"originalModel\":\"gpt-5.6-luna\"",
                "\"selectedModel\":\"gpt-5.5\"",
                "\"fallbackLevel\":1",
                "\"modelRoutingTraceId\":\"trace-1\"");
        assertThat(captor.getValue().getResponseSummary()).contains("sk-***");
        assertThat(captor.getValue().getResponseSummary()).doesNotContain("should-redact");
    }

    @Test
    void sumChargeableCostByAnalysisIdReturnsZeroWhenMapperReturnsNull() {
        AiCallLogServiceImpl service = new AiCallLogServiceImpl(mapper, new ObjectMapper());
        when(mapper.sumChargeableCostByAnalysisId("analysis-1")).thenReturn(null);
        when(mapper.sumChargeableCostSince(LocalDateTime.MIN)).thenReturn(new BigDecimal("0.60"));

        assertThat(service.sumChargeableCostByAnalysisId("analysis-1")).isEqualByComparingTo("0");
        assertThat(service.sumChargeableCostSince(LocalDateTime.MIN)).isEqualByComparingTo("0.60");
    }

    @Test
    void decisionChainTracePersistsInputHashOutputUsageLatencyAndAuthorityBoundary() {
        AiCallLogServiceImpl service = new AiCallLogServiceImpl(mapper, new ObjectMapper());
        AiDecisionChainRequest request = new AiDecisionChainRequest();
        request.setRole(AiDecisionChainRole.GPT_FINAL);
        request.setAnalysisId("analysis-chain-1");
        request.setTraceId("trace-chain-1");
        request.setCandidateId("candidate-chain-1");
        request.setSymbol("BTCUSDT");
        request.setTimeframe("5m");
        request.setInput(Map.of("evidence", "Bearer abcdefghijklmnop"));

        AiCallLogDO log = service.startDecisionChainCall(request, client(), new BigDecimal("0.10"));

        ArgumentCaptor<AiCallLogDO> started = ArgumentCaptor.forClass(AiCallLogDO.class);
        verify(mapper).insert(started.capture());
        assertThat(started.getValue().getAnalysisId()).isEqualTo("analysis-chain-1");
        assertThat(started.getValue().getTraceId()).isEqualTo("trace-chain-1");
        assertThat(started.getValue().getCandidateId()).isEqualTo("candidate-chain-1");
        assertThat(started.getValue().getContractType()).isEqualTo("DECISION_CHAIN_V4_1");
        assertThat(started.getValue().getRequestHash()).hasSize(64);
        assertThat(started.getValue().getRequestSummary()).doesNotContain("abcdefghijklmnop");
        assertThat(started.getValue().getReviewOnly()).isFalse();
        assertThat(started.getValue().getNotExecutionPlanCreation()).isFalse();
        assertThat(started.getValue().getNotFinalExecutionPlanCreation()).isTrue();
        assertThat(started.getValue().getNotUserPositionCreation()).isTrue();
        assertThat(started.getValue().getNotAutoTrading()).isTrue();

        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setProvider(AiProviderName.OPENAI);
        result.setRole(AiDecisionChainRole.GPT_FINAL);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setPayloadJson("{\"summary\":\"candidate\"}");
        result.setProviderRequestId("provider-request-1");
        result.setLatencyMs(123L);
        result.setInputTokens(100L);
        result.setOutputTokens(20L);
        result.setTotalTokens(120L);
        result.setCalculatedCostUsd(new BigDecimal("0.0012"));
        result.setSelectedModel("gpt-test");

        service.completeDecisionChainCall(log, result);

        ArgumentCaptor<AiCallLogDO> completed = ArgumentCaptor.forClass(AiCallLogDO.class);
        verify(mapper).updateCompletion(completed.capture());
        assertThat(completed.getValue().getOutputPayload()).isEqualTo("{\"summary\":\"candidate\"}");
        assertThat(completed.getValue().getLatencyMs()).isEqualTo(123L);
        assertThat(completed.getValue().getTotalTokens()).isEqualTo(120L);
        assertThat(completed.getValue().getCalculatedCostUsd()).isEqualByComparingTo("0.0012");
    }

    @Test
    void decisionChainHashCoversFullCanonicalInputBeyondSummaryLimit() {
        AiCallLogServiceImpl service = new AiCallLogServiceImpl(mapper, new ObjectMapper());
        AiDecisionChainRequest first = decisionChainRequest("x".repeat(9_000) + "A");
        AiDecisionChainRequest second = decisionChainRequest("x".repeat(9_000) + "B");

        service.startDecisionChainCall(first, client(), BigDecimal.ZERO);
        service.startDecisionChainCall(second, client(), BigDecimal.ZERO);

        ArgumentCaptor<AiCallLogDO> captor = ArgumentCaptor.forClass(AiCallLogDO.class);
        verify(mapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(AiCallLogDO::getRequestSummary)
                .allSatisfy(summary -> assertThat(summary).hasSize(8_000));
        assertThat(captor.getAllValues().get(0).getRequestHash())
                .isNotEqualTo(captor.getAllValues().get(1).getRequestHash());
    }

    @Test
    void decisionChainCompletionRetainsAcceptedOutputBeyondLegacyLimit() {
        AiCallLogServiceImpl service = new AiCallLogServiceImpl(mapper, new ObjectMapper());
        AiCallLogDO log = new AiCallLogDO();
        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setProvider(AiProviderName.OPENAI);
        result.setRole(AiDecisionChainRole.GPT_FINAL);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setPayloadJson("canonical");
        result.setAuditOutput("x".repeat(20_000));

        service.completeDecisionChainCall(log, result);

        ArgumentCaptor<AiCallLogDO> captor = ArgumentCaptor.forClass(AiCallLogDO.class);
        verify(mapper).updateCompletion(captor.capture());
        assertThat(captor.getValue().getOutputPayload()).hasSize(20_000);
    }

    @Test
    void terminalFailureTraceIsInsertedWithErrorFallbackAndLatency() {
        AiCallLogServiceImpl service = new AiCallLogServiceImpl(mapper, new ObjectMapper());
        AiDecisionChainRequest request = decisionChainRequest("evidence");
        AiDecisionChainResult failure = AiDecisionChainResult.failed(
                AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL,
                AiProviderCallStatus.TIMEOUT, "PROVIDER_TIMEOUT");
        failure.setLatencyMs(250L);

        service.recordDecisionChainResult(request, AiProviderName.OPENAI,
                "gpt-test", failure, BigDecimal.ZERO);

        ArgumentCaptor<AiCallLogDO> captor = ArgumentCaptor.forClass(AiCallLogDO.class);
        verify(mapper).insert(captor.capture());
        AiCallLogDO trace = captor.getValue();
        assertThat(trace.getTraceId()).isEqualTo("trace-chain-1");
        assertThat(trace.getAnalysisId()).isEqualTo("analysis-chain-1");
        assertThat(trace.getAiRole()).isEqualTo("GPT_FINAL");
        assertThat(trace.getModelName()).isEqualTo("gpt-test");
        assertThat(trace.getCallStatus()).isEqualTo("TIMEOUT");
        assertThat(trace.getErrorMessage()).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(trace.getFallbackFlag()).isTrue();
        assertThat(trace.getFallbackReason()).isEqualTo("PROVIDER_TIMEOUT");
        assertThat(trace.getLatencyMs()).isEqualTo(250L);
        assertThat(trace.getCompletedAt()).isNotNull();
    }

    private static AiProviderRequest request() {
        AiProviderRequest request = new AiProviderRequest();
        request.setAnalysisId("analysis-1");
        request.setTraceId("trace-1");
        request.setSymbol("BTCUSDT");
        request.setTimeframe("1m");
        request.setRuleMarketBias("BULLISH");
        request.setRuleConfidence("HIGH");
        request.setRuleRiskLevel("LOW");
        request.setRuleWorthOpening(Boolean.TRUE);
        return request;
    }

    private static AiDecisionChainRequest decisionChainRequest(String evidence) {
        AiDecisionChainRequest request = new AiDecisionChainRequest();
        request.setRole(AiDecisionChainRole.GPT_FINAL);
        request.setAnalysisId("analysis-chain-1");
        request.setTraceId("trace-chain-1");
        request.setCandidateId("candidate-chain-1");
        request.setSymbol("BTCUSDT");
        request.setTimeframe("5m");
        request.setInput(Map.of("evidence", evidence));
        return request;
    }

    private static AiProviderClient client() {
        AiProviderProperties properties = new AiProviderProperties();
        properties.setEnabled(true);
        properties.setApiKey("sk-local-test");
        properties.setModel("gpt-test");
        properties.setBaseUrl("https://ai.test");
        return new AiProviderClient() {
            @Override public AiProviderName provider() { return AiProviderName.OPENAI; }
            @Override public AiProviderRole role() { return AiProviderRole.GPT_RULE_REVIEW; }
            @Override public AiProviderReadiness readiness() { return null; }
            @Override public AiProviderReviewResult review(AiProviderRequest request) { return null; }
            @Override public AiProviderProperties providerProperties() { return properties; }
        };
    }
}
