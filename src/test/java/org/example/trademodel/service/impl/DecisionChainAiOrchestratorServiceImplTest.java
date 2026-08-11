package org.example.trademodel.service.impl;

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
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.service.AiCallLogService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("core-regression")
class DecisionChainAiOrchestratorServiceImplTest {

    @Test
    void eachDecisionRoleIsRoutedOnlyToItsAuthorizedProviderRoleAndAudited() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiProviderClient gemini = client(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW);
        AiProviderClient grok = client(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.getProviderTimeouts().setOpenaiMs(3_000);
        properties.getProviderTimeouts().setGeminiMs(4_000);
        properties.getProviderTimeouts().setXaiMs(5_000);
        when(usageGuard.evaluate(any(), eq("analysis-1")))
                .thenReturn(AiUsageGuardResult.allowed(new BigDecimal("0.01")));
        when(callLogService.startDecisionChainCall(any(), any(), any())).thenReturn(new AiCallLogDO());
        when(gpt.executeDecisionChain(any(), anyLong()))
                .thenAnswer(invocation -> success(AiDecisionChainRole.GPT_FINAL, AiProviderName.OPENAI));
        when(gemini.executeDecisionChain(any(), anyLong()))
                .thenAnswer(invocation -> success(AiDecisionChainRole.GEMINI_REVIEW, AiProviderName.GEMINI));
        when(grok.executeDecisionChain(any(), anyLong()))
                .thenAnswer(invocation -> success(AiDecisionChainRole.GROK_CHALLENGE, AiProviderName.XAI));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt, gemini, grok), usageGuard, callLogService, properties);

        assertThat(service.invoke(request(AiDecisionChainRole.GPT_FINAL)).getProvider())
                .isEqualTo(AiProviderName.OPENAI);
        assertThat(service.invoke(request(AiDecisionChainRole.GEMINI_REVIEW)).getProvider())
                .isEqualTo(AiProviderName.GEMINI);
        assertThat(service.invoke(request(AiDecisionChainRole.GROK_CHALLENGE)).getProvider())
                .isEqualTo(AiProviderName.XAI);

        verify(gpt).executeDecisionChain(any(), eq(3_000L));
        verify(gemini).executeDecisionChain(any(), eq(4_000L));
        verify(grok).executeDecisionChain(any(), eq(5_000L));
        verify(callLogService, org.mockito.Mockito.times(3)).startDecisionChainCall(any(), any(), any());
        verify(callLogService, org.mockito.Mockito.times(3)).completeDecisionChainCall(any(), any());
    }

    @Test
    void missingAuthorizedRoleFailsClosedAndPersistsFallbackTrace() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());

        AiDecisionChainResult result = service.invoke(request(AiDecisionChainRole.GEMINI_REVIEW));

        assertThat(result.successful()).isFalse();
        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.FAILED);
        assertThat(result.getFallbackReason()).isEqualTo("DECISION_CHAIN_PROVIDER_MISSING");
        verify(gpt, never()).executeDecisionChain(any(), anyLong());
        verify(callLogService, never()).startDecisionChainCall(any(), any(), any());
        verify(callLogService).recordDecisionChainResult(
                any(), eq(AiProviderName.GEMINI), eq("NOT_CONFIGURED"), eq(result), eq(BigDecimal.ZERO));
    }

    @Test
    void timeoutAndProviderExceptionBothCompleteAuditableFallbackTraces() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO(), new AiCallLogDO());
        when(gpt.executeDecisionChain(any(), anyLong()))
                .thenReturn(AiDecisionChainResult.failed(AiProviderName.OPENAI,
                        AiDecisionChainRole.GPT_FINAL, AiProviderCallStatus.TIMEOUT, "PROVIDER_TIMEOUT"))
                .thenThrow(new IllegalStateException("provider down"));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());

        AiDecisionChainResult timeout = service.invoke(request(AiDecisionChainRole.GPT_FINAL));
        AiDecisionChainResult exception = service.invoke(request(AiDecisionChainRole.GPT_FINAL));

        assertThat(timeout.getCallStatus()).isEqualTo(AiProviderCallStatus.TIMEOUT);
        assertThat(timeout.isFallback()).isTrue();
        assertThat(timeout.getLatencyMs()).isNotNull();
        assertThat(exception.getCallStatus()).isEqualTo(AiProviderCallStatus.FAILED);
        assertThat(exception.getFallbackReason()).isEqualTo("DECISION_CHAIN_PROVIDER_EXCEPTION");
        ArgumentCaptor<AiDecisionChainResult> traces = ArgumentCaptor.forClass(AiDecisionChainResult.class);
        verify(callLogService, org.mockito.Mockito.times(2))
                .completeDecisionChainCall(any(), traces.capture());
        assertThat(traces.getAllValues()).extracting(AiDecisionChainResult::getCallStatus)
                .containsExactly(AiProviderCallStatus.TIMEOUT, AiProviderCallStatus.FAILED);
    }

    @Test
    void callLogStartFailureStillAttemptsTerminalFallbackTrace() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenThrow(new IllegalStateException("start failed"));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());

        AiDecisionChainResult result = service.invoke(request(AiDecisionChainRole.GPT_FINAL));

        assertThat(result.getFallbackReason()).isEqualTo("AI_CALL_LOG_START_FAILED");
        verify(callLogService).recordDecisionChainResult(
                any(), eq(AiProviderName.OPENAI), eq("UNAVAILABLE"), eq(result), eq(BigDecimal.ZERO));
    }

    private static AiProviderClient client(AiProviderName provider, AiProviderRole role) {
        AiProviderClient client = mock(AiProviderClient.class);
        when(client.provider()).thenReturn(provider);
        when(client.role()).thenReturn(role);
        return client;
    }

    private static AiDecisionChainRequest request(AiDecisionChainRole role) {
        AiDecisionChainRequest request = new AiDecisionChainRequest();
        request.setRole(role);
        request.setAnalysisId("analysis-1");
        request.setTraceId("trace-1");
        request.setCandidateId("candidate-1");
        request.setSymbol("BTCUSDT");
        request.setTimeframe("5m");
        return request;
    }

    private static AiDecisionChainResult success(AiDecisionChainRole role, AiProviderName provider) {
        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setRole(role);
        result.setProvider(provider);
        result.setCallStatus(AiProviderCallStatus.SUCCESS);
        result.setPayloadJson("{}");
        return result;
    }
}
