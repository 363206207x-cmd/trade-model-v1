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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setPerAssetRoleMinIntervalMs(0L);
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, properties);

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
    void decisionChainProviderTimeoutCannotExceedEightSecondContract() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setOverallTimeoutMs(25_000);
        properties.getProviderTimeouts().setOverallMs(25_000);
        properties.getProviderTimeouts().setOpenaiMs(25_000);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO());
        when(gpt.executeDecisionChain(any(), anyLong()))
                .thenReturn(success(AiDecisionChainRole.GPT_FINAL, AiProviderName.OPENAI));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, properties);

        service.invoke(request(AiDecisionChainRole.GPT_FINAL));

        verify(gpt).executeDecisionChain(any(), eq(8_000L));
    }

    @Test
    void repeatedCallForSameAssetTimeframeAndRoleIsRateLimitedAndAudited() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setPerAssetRoleMinIntervalMs(60_000L);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO(), new AiCallLogDO());
        when(gpt.executeDecisionChain(any(), anyLong()))
                .thenReturn(success(AiDecisionChainRole.GPT_FINAL, AiProviderName.OPENAI));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, properties);

        AiDecisionChainResult first = service.invoke(request(AiDecisionChainRole.GPT_FINAL));
        AiDecisionChainResult limited = service.invoke(request(AiDecisionChainRole.GPT_FINAL));

        assertThat(first.getCallStatus()).isNotEqualTo(AiProviderCallStatus.RATE_LIMITED);
        assertThat(limited.getCallStatus()).isEqualTo(AiProviderCallStatus.RATE_LIMITED);
        assertThat(limited.getFallbackReason()).isEqualTo("ASSET_ROLE_FREQUENCY_LIMITED");
        verify(gpt).executeDecisionChain(any(), eq(8_000L));
        verify(callLogService, org.mockito.Mockito.times(2)).completeDecisionChainCall(any(), any());
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

    @Test
    void inputContractGatePersistsFallbackTraceWithoutCallingProvider() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());
        AiDecisionChainRequest request = request(AiDecisionChainRole.GPT_FINAL);
        request.setInputContractSatisfied(false);
        request.setInputContractFailures(List.of("EVIDENCE_MISSING"));

        AiDecisionChainResult result = service.invoke(request);

        assertThat(result.getCallStatus()).isEqualTo(AiProviderCallStatus.INVALID_RESPONSE);
        assertThat(result.isFallback()).isTrue();
        assertThat(result.getFallbackReason()).contains("AI_INPUT_CONTRACT_BLOCKED");
        verify(usageGuard, never()).evaluate(any(), any());
        verify(gpt, never()).executeDecisionChain(any(), anyLong());
        verify(callLogService).recordDecisionChainResult(
                eq(request), eq(AiProviderName.OPENAI), eq("NOT_CALLED_INPUT_GATE"),
                eq(result), eq(BigDecimal.ZERO));
    }

    @Test
    void terminalTracePersistenceFailureStopsTheDecisionChain() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(callLogService.recordDecisionChainResult(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("database unavailable"));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());

        assertThatThrownBy(() -> service.invoke(request(AiDecisionChainRole.GEMINI_REVIEW)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("AI_TRACE_PERSISTENCE_FAILED")
                .hasRootCauseMessage("database unavailable");
        verify(gpt, never()).executeDecisionChain(any(), anyLong());
    }

    @Test
    void validEvidenceIdCannotHideFabricatedEvidenceFacts() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO());
        when(gpt.executeDecisionChain(any(), anyLong()))
                .thenReturn(traceableGptSuccess("fabricated-source"));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());

        AiDecisionChainResult result = service.invoke(traceableRequest(AiDecisionChainRole.GPT_FINAL));

        assertThat(result.successful()).isFalse();
        assertThat(result.getFallbackReason()).isEqualTo("AI_OUTPUT_EVIDENCE_FACT_MISMATCH");
        assertThat(result.getPayloadJson()).isNull();
        assertThat(result.getAuditOutput()).contains("fabricated-source");
    }

    @Test
    void coinglassDerivedEvidenceOutsideGenericWindowRemainsTraceable() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO());
        AiDecisionChainResult providerResult = success(
                AiDecisionChainRole.GPT_FINAL, AiProviderName.OPENAI);
        providerResult.setPayloadJson("""
                {"supportingEvidence":[{"evidenceId":"coinglass-oi-1",
                 "type":"OPEN_INTEREST_PRICE_CONFIRMATION","source":"PROVIDER_SNAPSHOT",
                 "currentValue":"1.8","change":"+1.8%","direction":"BULLISH",
                 "strength":85.0,"confidence":82.0,"observedAt":"2026-08-20T06:26:00",
                 "freshness":"FRESH","analysisId":"analysis-1"}],"opposingEvidence":[],
                 "biasAdjustment":{"before":"BULLISH","after":"BULLISH","reason":"衍生品证据确认"},
                 "candidateSummary":{"entrySource":"coinglass-oi-1","stopSource":"coinglass-oi-1",
                 "targetSource":"coinglass-oi-1","invalidationSource":"coinglass-oi-1",
                 "expectedRiskRewardSource":"coinglass-oi-1"}}
                """);
        when(gpt.executeDecisionChain(any(), anyLong())).thenReturn(providerResult);
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());
        AiDecisionChainRequest request = request(AiDecisionChainRole.GPT_FINAL);
        request.setInput(Map.of(
                "evidence", List.of(),
                "derivativesContext", Map.of("derivedEvidence", List.of(Map.ofEntries(
                        Map.entry("evidenceId", "coinglass-oi-1"),
                        Map.entry("type", "OPEN_INTEREST_PRICE_CONFIRMATION"),
                        Map.entry("source", "PROVIDER_SNAPSHOT"),
                        Map.entry("sourceReference", "sourceField=openInterestChange5m"),
                        Map.entry("sourceTraceId", "coinglass-trace-1"),
                        Map.entry("currentValue", "1.8"),
                        Map.entry("changeFromBaseline", "+1.8%"),
                        Map.entry("direction", "BULLISH"),
                        Map.entry("strength", 85.0),
                        Map.entry("confidence", 82.0),
                        Map.entry("observedAt", "2026-08-20T06:26:00"),
                        Map.entry("freshness", "FRESH"),
                        Map.entry("analysisId", "analysis-1")))),
                "decisionBundle", Map.of("ruleDirection", "BULLISH")));

        AiDecisionChainResult result = service.invoke(request);

        assertThat(result.successful()).isTrue();
        assertThat(result.getFallbackReason()).isNull();
    }

    @Test
    void externalEventChallengeMustMatchReferencedSourceTimeAndWindow() {
        AiProviderClient grok = client(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(grok, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(grok), any()))
                .thenReturn(new AiCallLogDO());
        when(grok.executeDecisionChain(any(), anyLong()))
                .thenReturn(externalEventChallenge("2026-08-12T01:00:00Z"));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(grok), usageGuard, callLogService, new AiOrchestratorProperties());

        AiDecisionChainResult result = service.invoke(traceableRequest(AiDecisionChainRole.GROK_CHALLENGE));

        assertThat(result.successful()).isFalse();
        assertThat(result.getFallbackReason()).isEqualTo("AI_OUTPUT_EXTERNAL_EVENT_PROVENANCE_INVALID");
    }

    @Test
    void externalEventNoneFoundRequiresFreshExternalEventCoverage() {
        AiProviderClient grok = client(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(grok, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(grok), any()))
                .thenReturn(new AiCallLogDO());
        when(grok.executeDecisionChain(any(), anyLong()))
                .thenReturn(externalEventStateClaim("NONE_FOUND"));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(grok), usageGuard, callLogService, new AiOrchestratorProperties());

        AiDecisionChainResult result = service.invoke(
                traceableMarketRequest(AiDecisionChainRole.GROK_CHALLENGE, "FRESH"));

        assertThat(result.successful()).isFalse();
        assertThat(result.getFallbackReason())
                .isEqualTo("AI_OUTPUT_EXTERNAL_EVENT_STATE_REQUIRES_FRESH_COVERAGE");
    }

    @Test
    void externalEventSourceUnavailableIsAnExplicitFailClosedCollectionState() {
        AiProviderClient grok = client(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(grok, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(grok), any()))
                .thenReturn(new AiCallLogDO());
        when(grok.executeDecisionChain(any(), anyLong()))
                .thenReturn(externalEventStateClaim("SOURCE_UNAVAILABLE"));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(grok), usageGuard, callLogService, new AiOrchestratorProperties());

        AiDecisionChainResult result = service.invoke(
                traceableMarketRequest(AiDecisionChainRole.GROK_CHALLENGE, "FRESH"));

        assertThat(result.successful()).isTrue();
    }

    @Test
    void externalEventSourceUnavailableCannotHideSuppliedEventEvidence() {
        AiProviderClient grok = client(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(grok, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(grok), any()))
                .thenReturn(new AiCallLogDO());
        when(grok.executeDecisionChain(any(), anyLong()))
                .thenReturn(externalEventStateClaim("SOURCE_UNAVAILABLE"));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(grok), usageGuard, callLogService, new AiOrchestratorProperties());

        AiDecisionChainResult result = service.invoke(
                traceableRequest(AiDecisionChainRole.GROK_CHALLENGE));

        assertThat(result.successful()).isFalse();
        assertThat(result.getFallbackReason())
                .isEqualTo("AI_OUTPUT_EXTERNAL_EVENT_SOURCE_UNAVAILABLE_CONTRADICTS_INPUT");
    }

    @Test
    void cacheHitProducesIndependentAuditableTraceWithoutSecondProviderCall() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO());
        when(gpt.executeDecisionChain(any(), anyLong())).thenReturn(cacheableSuccess());
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());
        AiDecisionChainRequest request = cacheableRequest();

        AiDecisionChainResult first = service.invoke(request);
        AiDecisionChainResult cached = service.invoke(request);

        assertThat(first.successful()).isTrue();
        assertThat(first.isCacheHit()).isFalse();
        assertThat(cached.successful()).isTrue();
        assertThat(cached.isCacheHit()).isTrue();
        verify(gpt).executeDecisionChain(any(), anyLong());
        verify(callLogService).completeDecisionChainCall(any(), eq(first));
        verify(callLogService).recordDecisionChainResult(
                eq(request), eq(AiProviderName.OPENAI), eq("gpt-cache-test"),
                eq(cached), eq(BigDecimal.ZERO));
    }

    @Test
    void cacheUsesStableEvidenceContentAndRebindsCurrentAnalysisReferences() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO());
        when(gpt.executeDecisionChain(any(), anyLong())).thenReturn(cacheableSuccess());
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());
        AiDecisionChainRequest firstRequest = cacheableRequest(
                "analysis-1", "trace-1", "evidence-1", "source-trace-1");
        AiDecisionChainRequest secondRequest = cacheableRequest(
                "analysis-2", "trace-2", "evidence-2", "source-trace-2");

        AiDecisionChainResult first = service.invoke(firstRequest);
        AiDecisionChainResult cached = service.invoke(secondRequest);

        assertThat(first.successful()).isTrue();
        assertThat(cached.successful()).isTrue();
        assertThat(cached.isCacheHit()).isTrue();
        assertThat(cached.getAnalysisId()).isEqualTo("analysis-2");
        assertThat(cached.getTraceId()).isEqualTo("trace-2");
        assertThat(cached.getPayloadJson())
                .contains("\"entrySource\":\"evidence-2\"")
                .doesNotContain("evidence-1");
        verify(gpt).executeDecisionChain(any(), anyLong());
        verify(callLogService).recordDecisionChainResult(
                eq(secondRequest), eq(AiProviderName.OPENAI), eq("gpt-cache-test"),
                eq(cached), eq(BigDecimal.ZERO));
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

    private static AiDecisionChainRequest cacheableRequest() {
        return cacheableRequest("analysis-1", "trace-1", "evidence-1", "source-trace-1");
    }

    private static AiDecisionChainRequest cacheableRequest(String analysisId,
                                                           String traceId,
                                                           String evidenceId,
                                                           String sourceTraceId) {
        AiDecisionChainRequest request = request(AiDecisionChainRole.GPT_FINAL);
        request.setAnalysisId(analysisId);
        request.setTraceId(traceId);
        request.setInput(Map.of(
                "evidence", List.of(Map.of(
                        "evidenceId", evidenceId,
                        "source", "MARKET_SOURCE",
                        "sourceReference", "market://BTCUSDT/5m",
                        "sourceTraceId", sourceTraceId,
                        "currentValue", "65000",
                        "changeFromBaseline", "+1.2%",
                        "analysisId", analysisId)),
                "decisionBundle", Map.of("ruleDirection", "BULLISH")));
        return request;
    }

    private static AiDecisionChainRequest traceableRequest(AiDecisionChainRole role) {
        AiDecisionChainRequest request = request(role);
        request.setInput(Map.of(
                "evidence", List.of(Map.ofEntries(
                        Map.entry("evidenceId", "evidence-1"),
                        Map.entry("type", "EVENT_RISK"),
                        Map.entry("source", "verified-source"),
                        Map.entry("sourceReference", "event://verified/1"),
                        Map.entry("sourceTraceId", "source-trace-1"),
                        Map.entry("currentValue", "event active"),
                        Map.entry("changeFromBaseline", "new event"),
                        Map.entry("direction", "BEARISH"),
                        Map.entry("strength", 80.0),
                        Map.entry("confidence", 87.0),
                        Map.entry("observedAt", "2026-08-12T00:00:00Z"),
                        Map.entry("freshness", "FRESH"),
                        Map.entry("analysisId", "analysis-1"),
                        Map.entry("eventWindow", "2026-08-12T00:00:00Z/2026-08-12T04:00:00Z"))),
                "decisionBundle", Map.of("ruleDirection", "BULLISH")));
        return request;
    }

    private static AiDecisionChainRequest traceableMarketRequest(AiDecisionChainRole role,
                                                                 String freshness) {
        AiDecisionChainRequest request = request(role);
        request.setInput(Map.of(
                "evidence", List.of(Map.ofEntries(
                        Map.entry("evidenceId", "evidence-1"),
                        Map.entry("type", "MARKET_STRUCTURE"),
                        Map.entry("source", "verified-source"),
                        Map.entry("sourceReference", "market://verified/1"),
                        Map.entry("sourceTraceId", "source-trace-1"),
                        Map.entry("currentValue", "structure intact"),
                        Map.entry("changeFromBaseline", "unchanged"),
                        Map.entry("direction", "BULLISH"),
                        Map.entry("strength", 80.0),
                        Map.entry("confidence", 87.0),
                        Map.entry("observedAt", "2026-08-12T00:00:00Z"),
                        Map.entry("freshness", freshness),
                        Map.entry("analysisId", "analysis-1"))),
                "decisionBundle", Map.of("ruleDirection", "BULLISH")));
        return request;
    }

    private static AiDecisionChainResult cacheableSuccess() {
        AiDecisionChainResult result = success(AiDecisionChainRole.GPT_FINAL, AiProviderName.OPENAI);
        result.setSelectedModel("gpt-cache-test");
        result.setPayloadJson("""
                {"supportingEvidence":[],"opposingEvidence":[],
                 "biasAdjustment":{"before":"BULLISH","after":"BULLISH","reason":"unchanged"},
                 "candidateSummary":{"entrySource":"evidence-1","stopSource":"evidence-1",
                 "targetSource":"evidence-1","invalidationSource":"evidence-1",
                 "expectedRiskRewardSource":"evidence-1"}}
                """);
        return result;
    }

    private static AiDecisionChainResult traceableGptSuccess(String source) {
        AiDecisionChainResult result = success(AiDecisionChainRole.GPT_FINAL, AiProviderName.OPENAI);
        result.setPayloadJson("""
                {"supportingEvidence":[{"evidenceId":"evidence-1","type":"EVENT_RISK",
                 "source":"%s","currentValue":"event active","change":"new event",
                 "direction":"BEARISH","strength":80.0,"confidence":87.0,
                 "observedAt":"2026-08-12T00:00:00Z","freshness":"FRESH","analysisId":"analysis-1"}],
                 "opposingEvidence":[],
                 "biasAdjustment":{"before":"BULLISH","after":"BULLISH","reason":"unchanged"},
                 "candidateSummary":{"entrySource":"evidence-1","stopSource":"evidence-1",
                 "targetSource":"evidence-1","invalidationSource":"evidence-1",
                 "expectedRiskRewardSource":"evidence-1"}}
                """.formatted(source));
        return result;
    }

    private static AiDecisionChainResult externalEventChallenge(String observedAt) {
        AiDecisionChainResult result = success(AiDecisionChainRole.GROK_CHALLENGE, AiProviderName.XAI);
        result.setPayloadJson("""
                {"failurePaths":[],"opposingScenarios":[],"microstructureRisks":[],"watchIndicators":[],
                 "externalEventRisks":[{"findingId":"event-1","category":"EVENT","text":"event risk",
                 "impact":"may weaken the candidate","evidenceRefs":["evidence-1"],
                 "source":"verified-source","observedAt":"%s",
                 "eventWindow":"2026-08-12T00:00:00Z/2026-08-12T04:00:00Z"}],
                 "conflictLevel":"LEVEL_2_MINOR_DISAGREEMENT"}
                """.formatted(observedAt));
        return result;
    }

    private static AiDecisionChainResult externalEventStateClaim(String state) {
        AiDecisionChainResult result = success(AiDecisionChainRole.GROK_CHALLENGE, AiProviderName.XAI);
        result.setPayloadJson("""
                {"failurePaths":[],"opposingScenarios":[],"externalEventRisks":[],
                 "externalEventRisksState":"%s","microstructureRisks":[],"watchIndicators":[],
                 "conflictLevel":"LEVEL_1_CONSISTENT"}
                """.formatted(state));
        return result;
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
