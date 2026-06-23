package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiProviderCallStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

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

        service.completeCall(log, result);

        ArgumentCaptor<AiCallLogDO> captor = ArgumentCaptor.forClass(AiCallLogDO.class);
        verify(mapper).updateCompletion(captor.capture());
        assertThat(captor.getValue().getCallStatus()).isEqualTo("SUCCESS");
        assertThat(captor.getValue().getTotalTokens()).isEqualTo(15L);
        assertThat(captor.getValue().getResponseSummary()).contains("sk-***");
        assertThat(captor.getValue().getResponseSummary()).doesNotContain("should-redact");
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
