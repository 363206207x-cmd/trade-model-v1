package org.example.trademodel.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.trademodel.ai.AiDecisionChainPromptBuilder;
import org.example.trademodel.ai.AiDecisionChainRequest;
import org.example.trademodel.ai.AiDecisionChainResult;
import org.example.trademodel.ai.AiDecisionChainRole;
import org.example.trademodel.ai.AiBackgroundTaskState;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    void promptPublishesExactEvidenceReferencesAndDeterministicCollectionRules() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        AiDecisionChainPromptBuilder builder = new AiDecisionChainPromptBuilder(
                objectMapper, new AiOrchestratorProperties());
        AiDecisionChainRequest request = request(AiDecisionChainRole.GEMINI_REVIEW);
        request.setInput(Map.of("evidence", List.of(Map.of(
                "evidenceId", "evidence-1",
                "source", "BINANCE",
                "sourceReference", "market://BTCUSDT/5m",
                "sourceTraceId", "trace-market-1"))));

        JsonNode root = objectMapper.readTree(builder.build(request).dataJson());

        assertThat(root.path("allowedEvidenceReferences").toString())
                .contains("evidence-1", "BINANCE", "market://BTCUSDT/5m", "trace-market-1");
        assertThat(AiDecisionChainPromptBuilder.systemInstruction(AiDecisionChainRole.GEMINI_REVIEW))
                .contains("allowedEvidenceReferences", "FOUND only when");
        assertThat(AiDecisionChainPromptBuilder.systemInstruction(AiDecisionChainRole.GROK_CHALLENGE))
                .contains("allowedEvidenceReferences", "FOUND only when", "at most one item");
    }

    @Test
    void eachDecisionRoleIsRoutedOnlyToItsAuthorizedProviderRoleAndAudited() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiProviderClient gemini = client(AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW);
        AiProviderClient grok = client(AiProviderName.XAI, AiProviderRole.GROK_ADVERSARIAL_CHALLENGE);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
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

        verify(gpt).executeDecisionChain(any(), eq(180_000L));
        verify(gemini).executeDecisionChain(any(), eq(120_000L));
        verify(grok).executeDecisionChain(any(), eq(120_000L));
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
    void decisionChainUsesVersionedBackgroundDeadlineInsteadOfLegacyTimeoutFields() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setRequestTimeoutMs(1);
        properties.setOverallTimeoutMs(1);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO());
        when(gpt.executeDecisionChain(any(), anyLong()))
                .thenReturn(success(AiDecisionChainRole.GPT_FINAL, AiProviderName.OPENAI));
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, properties);

        service.invoke(request(AiDecisionChainRole.GPT_FINAL));

        verify(gpt).executeDecisionChain(any(), eq(180_000L));
    }

    @Test
    void disabledBackgroundRuntimeFailsClosedWithoutCallingProvider() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.getBackgroundExecution().setEnabled(false);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO());
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, properties);

        AiDecisionChainResult result = service.invoke(request(AiDecisionChainRole.GPT_FINAL));

        assertThat(result.successful()).isFalse();
        assertThat(result.getFallbackReason()).isEqualTo("ORCHESTRATOR_TIMEOUT_CONFIG_INVALID");
        verify(gpt, never()).executeDecisionChain(any(), anyLong());
        verify(callLogService).completeDecisionChainCall(any(), eq(result));
    }

    @Test
    void invalidBackgroundPollingWindowFailsClosedWithoutCallingProvider() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.getBackgroundExecution().setInitialPollIntervalMs(2_000);
        properties.getBackgroundExecution().setMaxPollIntervalMs(1_000);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO());
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, properties);

        AiDecisionChainResult result = service.invoke(request(AiDecisionChainRole.GPT_FINAL));

        assertThat(result.successful()).isFalse();
        assertThat(result.getFallbackReason()).isEqualTo("ORCHESTRATOR_TIMEOUT_CONFIG_INVALID");
        verify(gpt, never()).executeDecisionChain(any(), anyLong());
        verify(callLogService).completeDecisionChainCall(any(), eq(result));
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
        verify(gpt).executeDecisionChain(any(), eq(180_000L));
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
    void validEvidenceIdentityHydratesImmutableFactsFromAuthoritativeInput() {
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

        assertThat(result.successful()).isTrue();
        assertThat(result.getFallbackReason()).isNull();
        assertThat(result.getPayloadJson())
                .contains("\"source\":\"verified-source\"")
                .doesNotContain("fabricated-source");
    }

    @Test
    void canonicalEvidenceHydrationStillRejectsWrongAnalysisIdentity() {
        AiProviderClient gpt = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        when(usageGuard.evaluate(gpt, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any()))
                .thenReturn(new AiCallLogDO());
        AiDecisionChainResult providerResult = traceableGptSuccess("source-1");
        providerResult.setPayloadJson(providerResult.getPayloadJson()
                .replace("\"analysisId\":\"analysis-1\"", "\"analysisId\":\"analysis-2\""));
        when(gpt.executeDecisionChain(any(), anyLong())).thenReturn(providerResult);
        DecisionChainAiOrchestratorServiceImpl service = new DecisionChainAiOrchestratorServiceImpl(
                List.of(gpt), usageGuard, callLogService, new AiOrchestratorProperties());

        AiDecisionChainResult result = service.invoke(traceableRequest(AiDecisionChainRole.GPT_FINAL));

        assertThat(result.successful()).isFalse();
        assertThat(result.getFallbackReason()).isEqualTo("AI_OUTPUT_EVIDENCE_TRACEABILITY_INVALID");
        assertThat(result.getPayloadJson()).isNull();
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
                 "candidateSummary":{"summary":"衍生品证据已纳入候选解释"}}
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
                .contains("\"evidenceId\":\"evidence-2\"")
                .doesNotContain("evidence-1");
        verify(gpt).executeDecisionChain(any(), anyLong());
        verify(callLogService).recordDecisionChainResult(
                eq(secondRequest), eq(AiProviderName.OPENAI), eq("gpt-cache-test"),
                eq(cached), eq(BigDecimal.ZERO));
    }

    @Test
    void nativeBackgroundSubmissionPollsSameResponseToSuccessAndPersistsTerminalTrace() {
        AiProviderClient gpt = nativeBackgroundClient();
        AiUsageGuard usageGuard = allowedUsageGuard(gpt);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO log = backgroundLog("call-native-1", 1);
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any())).thenReturn(log);
        when(gpt.submitDecisionChainBackground(any(), eq(30_000L)))
                .thenReturn(active(AiBackgroundTaskState.QUEUED, "resp-native-1"));
        AiDecisionChainResult completed = cacheableSuccess();
        completed.setTaskState(AiBackgroundTaskState.SUCCEEDED);
        completed.setProviderRequestId("resp-native-1");
        when(gpt.pollDecisionChainBackground(any(), eq("resp-native-1"), eq(30_000L)))
                .thenReturn(completed);
        DecisionChainAiOrchestratorServiceImpl service = service(
                gpt, usageGuard, callLogService, shortPollingProperties());

        AiDecisionChainResult result = service.invoke(cacheableRequest());

        assertThat(result.successful()).isTrue();
        assertThat(result.getTaskState()).isEqualTo(AiBackgroundTaskState.SUCCEEDED);
        assertThat(result.getProviderRequestId()).isEqualTo("resp-native-1");
        verify(gpt).submitDecisionChainBackground(any(), eq(30_000L));
        verify(gpt).pollDecisionChainBackground(any(), eq("resp-native-1"), eq(30_000L));
        verify(callLogService, org.mockito.Mockito.atLeast(2))
                .updateDecisionChainTask(eq(log), any());
        verify(callLogService).completeDecisionChainCall(log, result);
    }

    @Test
    void restartRecoveryPollsDurableResponseIdWithoutResubmissionOrSecondCharge() {
        AiProviderClient gpt = nativeBackgroundClient();
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO log = backgroundLog("call-recovery-1", 1);
        log.setProviderRequestId("resp-recovery-1");
        log.setTaskState(AiBackgroundTaskState.RUNNING.name());
        AiDecisionChainResult restored = active(AiBackgroundTaskState.RUNNING, "resp-recovery-1");
        when(callLogService.findLatestDecisionChainTask(
                eq("analysis-1"), eq(AiDecisionChainRole.GPT_FINAL.name()), any()))
                .thenReturn(log);
        when(callLogService.restoreDecisionChainResult(log)).thenReturn(restored);
        AiDecisionChainResult completed = cacheableSuccess();
        completed.setTaskState(AiBackgroundTaskState.SUCCEEDED);
        completed.setProviderRequestId("resp-recovery-1");
        when(gpt.pollDecisionChainBackground(any(), eq("resp-recovery-1"), eq(30_000L)))
                .thenReturn(completed);
        DecisionChainAiOrchestratorServiceImpl service = service(
                gpt, usageGuard, callLogService, shortPollingProperties());

        AiDecisionChainResult result = service.invoke(cacheableRequest());

        assertThat(result.successful()).isTrue();
        verify(gpt, never()).submitDecisionChainBackground(any(), anyLong());
        verify(gpt).pollDecisionChainBackground(any(), eq("resp-recovery-1"), eq(30_000L));
        verify(usageGuard, never()).evaluate(any(), any());
        verify(callLogService, never()).startDecisionChainCall(any(), any(), any());
        verify(callLogService).completeDecisionChainCall(log, result);
    }

    @Test
    void applicationWorkerRestartFailsClosedWithoutProviderReplay() {
        AiProviderClient gpt = nativeBackgroundClient();
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO log = backgroundLog("call-worker-restart", 1);
        log.setTaskState(AiBackgroundTaskState.RUNNING.name());
        log.setBackgroundMode("APPLICATION_PERSISTED_WORKER");
        AiDecisionChainResult restored = active(AiBackgroundTaskState.RUNNING, null);
        restored.setBackgroundMode("APPLICATION_PERSISTED_WORKER");
        when(callLogService.findLatestDecisionChainTask(
                eq("analysis-1"), eq(AiDecisionChainRole.GPT_FINAL.name()), any()))
                .thenReturn(log);
        when(callLogService.restoreDecisionChainResult(log)).thenReturn(restored);
        DecisionChainAiOrchestratorServiceImpl service = service(
                gpt, usageGuard, callLogService, shortPollingProperties());

        AiDecisionChainResult result = service.invoke(cacheableRequest());

        assertThat(result.successful()).isFalse();
        assertThat(result.getTaskState()).isEqualTo(AiBackgroundTaskState.FAILED);
        assertThat(result.getFailureClassification())
                .isEqualTo("APPLICATION_WORKER_RESTART_RECOVERY_BLOCKED");
        verify(gpt, never()).submitDecisionChainBackground(any(), anyLong());
        verify(gpt, never()).pollDecisionChainBackground(any(), any(), anyLong());
        verify(gpt, never()).executeDecisionChain(any(), anyLong());
        verify(usageGuard, never()).evaluate(any(), any());
        verify(callLogService).completeDecisionChainCall(log, result);
    }

    @Test
    void concurrentApplicationWorkerIsReturnedWithoutProviderReplayOrTraceMutation() {
        AiProviderClient gemini = client(
                AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW);
        AiUsageGuard usageGuard = allowedUsageGuard(gemini);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO concurrent = backgroundLog("call-concurrent-worker", 1);
        concurrent.setTaskState(AiBackgroundTaskState.RUNNING.name());
        concurrent.setBackgroundMode("APPLICATION_PERSISTED_WORKER");
        AiDecisionChainResult restored = active(AiBackgroundTaskState.RUNNING, null);
        restored.setProvider(AiProviderName.GEMINI);
        restored.setRole(AiDecisionChainRole.GEMINI_REVIEW);
        restored.setBackgroundMode("APPLICATION_PERSISTED_WORKER");
        when(callLogService.findLatestDecisionChainTask(
                eq("analysis-1"), eq(AiDecisionChainRole.GEMINI_REVIEW.name()), any()))
                .thenReturn(null, concurrent);
        when(callLogService.startDecisionChainCall(any(), eq(gemini), any()))
                .thenThrow(new IllegalStateException("concurrent trace insert"));
        when(callLogService.restoreDecisionChainResult(concurrent)).thenReturn(restored);
        DecisionChainAiOrchestratorServiceImpl service = service(
                gemini, usageGuard, callLogService, shortPollingProperties());
        AiDecisionChainRequest request = cacheableRequest();
        request.setRole(AiDecisionChainRole.GEMINI_REVIEW);

        AiDecisionChainResult result = service.invoke(request);

        assertThat(result).isSameAs(restored);
        assertThat(result.getTaskState()).isEqualTo(AiBackgroundTaskState.RUNNING);
        verify(gemini, never()).submitDecisionChainBackground(any(), anyLong());
        verify(gemini, never()).pollDecisionChainBackground(any(), any(), anyLong());
        verify(gemini, never()).executeDecisionChain(any(), anyLong());
        verify(callLogService, never()).completeDecisionChainCall(any(), any());
    }

    @Test
    void frozenDeadlineCalculationKeeps29_31And179SecondJobsAliveButExpiresAfter180() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 3, 0, 0);

        assertThat(DecisionChainAiOrchestratorServiceImpl.remainingJobDeadlineMs(
                now.minusSeconds(29), now, 180_000L)).isEqualTo(151_000L);
        assertThat(DecisionChainAiOrchestratorServiceImpl.remainingJobDeadlineMs(
                now.minusSeconds(31), now, 180_000L)).isEqualTo(149_000L);
        assertThat(DecisionChainAiOrchestratorServiceImpl.remainingJobDeadlineMs(
                now.minusSeconds(179), now, 180_000L)).isEqualTo(1_000L);
        assertThat(DecisionChainAiOrchestratorServiceImpl.remainingJobDeadlineMs(
                now.minusSeconds(181), now, 180_000L)).isZero();
    }

    @Test
    void durableTimeoutIsImmutableAndLateProviderCompletionCannotReviveIt() {
        AiProviderClient gpt = nativeBackgroundClient();
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO log = backgroundLog("call-terminal-timeout", 1);
        log.setProviderRequestId("resp-late-completion");
        log.setTaskState(AiBackgroundTaskState.TIMED_OUT.name());
        AiDecisionChainResult timedOut = AiDecisionChainResult.failed(
                AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL,
                AiProviderCallStatus.TIMEOUT, "GPT_JOB_DEADLINE_EXCEEDED");
        timedOut.setTaskState(AiBackgroundTaskState.TIMED_OUT);
        timedOut.setProviderRequestId("resp-late-completion");
        timedOut.setFailureClassification("GPT_JOB_DEADLINE_EXCEEDED");
        when(callLogService.findLatestDecisionChainTask(
                eq("analysis-1"), eq(AiDecisionChainRole.GPT_FINAL.name()), any()))
                .thenReturn(log);
        when(callLogService.restoreDecisionChainResult(log)).thenReturn(timedOut);
        DecisionChainAiOrchestratorServiceImpl service = service(
                gpt, usageGuard, callLogService, shortPollingProperties());

        AiDecisionChainResult result = service.invoke(cacheableRequest());

        assertThat(result).isSameAs(timedOut);
        assertThat(result.getTaskState()).isEqualTo(AiBackgroundTaskState.TIMED_OUT);
        verify(gpt, never()).submitDecisionChainBackground(any(), anyLong());
        verify(gpt, never()).pollDecisionChainBackground(any(), any(), anyLong());
        verify(gpt, never()).executeDecisionChain(any(), anyLong());
        verify(usageGuard, never()).evaluate(any(), any());
        verify(callLogService, never()).completeDecisionChainCall(any(), any());
    }

    @Test
    void gptDeadlineCancelsSameProviderResponseWithoutResubmission() {
        AiProviderClient gpt = nativeBackgroundClient();
        AiUsageGuard usageGuard = allowedUsageGuard(gpt);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO log = backgroundLog("call-deadline", 1);
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any())).thenReturn(log);
        when(gpt.submitDecisionChainBackground(any(), eq(30_000L)))
                .thenReturn(active(AiBackgroundTaskState.SUBMITTED, "resp-deadline"));
        when(gpt.pollDecisionChainBackground(any(), eq("resp-deadline"), eq(30_000L)))
                .thenReturn(active(AiBackgroundTaskState.RUNNING, "resp-deadline"));
        AiOrchestratorProperties properties = shortPollingProperties();
        properties.getBackgroundExecution().setGptJobDeadlineMs(10);
        DecisionChainAiOrchestratorServiceImpl service = service(
                gpt, usageGuard, callLogService, properties);

        AiDecisionChainResult result = service.invoke(cacheableRequest());

        assertThat(result.successful()).isFalse();
        assertThat(result.getTaskState()).isEqualTo(AiBackgroundTaskState.TIMED_OUT);
        assertThat(result.getFailureClassification()).isEqualTo("GPT_JOB_DEADLINE_EXCEEDED");
        assertThat(result.getProviderRequestId()).isEqualTo("resp-deadline");
        verify(gpt).submitDecisionChainBackground(any(), eq(30_000L));
        verify(gpt, org.mockito.Mockito.atLeastOnce())
                .pollDecisionChainBackground(any(), eq("resp-deadline"), eq(30_000L));
        verify(gpt).cancelDecisionChainBackground("resp-deadline", 30_000L);
        verify(callLogService).completeDecisionChainCall(log, result);
    }

    @Test
    void transientSubmitAndPollFailuresRetryAtMostOnceWithoutChangingProviderResponseId() {
        AiProviderClient gpt = nativeBackgroundClient();
        AiUsageGuard usageGuard = allowedUsageGuard(gpt);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO firstLog = backgroundLog("call-attempt-1", 1);
        AiCallLogDO secondLog = backgroundLog("call-attempt-2", 2);
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any())).thenReturn(firstLog);
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any(), eq(2)))
                .thenReturn(secondLog);
        AiDecisionChainResult transientSubmit = failure(
                "PROVIDER_HTTP_503", AiBackgroundTaskState.FAILED, true);
        when(gpt.submitDecisionChainBackground(any(), eq(30_000L)))
                .thenReturn(transientSubmit, active(AiBackgroundTaskState.SUBMITTED, "resp-retry-1"));
        AiDecisionChainResult transientPoll = failure(
                "PROVIDER_IO_FAILURE", AiBackgroundTaskState.RUNNING, true);
        AiDecisionChainResult completed = cacheableSuccess();
        completed.setTaskState(AiBackgroundTaskState.SUCCEEDED);
        completed.setProviderRequestId("resp-retry-1");
        when(gpt.pollDecisionChainBackground(any(), eq("resp-retry-1"), eq(30_000L)))
                .thenReturn(transientPoll, completed);
        DecisionChainAiOrchestratorServiceImpl service = service(
                gpt, usageGuard, callLogService, shortPollingProperties());

        AiDecisionChainResult result = service.invoke(cacheableRequest());

        assertThat(result.successful()).isTrue();
        assertThat(result.getAttempt()).isEqualTo(2);
        verify(gpt, org.mockito.Mockito.times(2))
                .submitDecisionChainBackground(any(), eq(30_000L));
        verify(gpt, org.mockito.Mockito.times(2))
                .pollDecisionChainBackground(any(), eq("resp-retry-1"), eq(30_000L));
        verify(callLogService).completeDecisionChainCall(firstLog, transientSubmit);
        verify(callLogService).completeDecisionChainCall(secondLog, result);
    }

    @Test
    void secondTransientPollFailureEndsTaskWithoutResubmit() {
        AiProviderClient gpt = nativeBackgroundClient();
        AiUsageGuard usageGuard = allowedUsageGuard(gpt);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO log = backgroundLog("call-poll-exhausted", 1);
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any())).thenReturn(log);
        when(gpt.submitDecisionChainBackground(any(), eq(30_000L)))
                .thenReturn(active(AiBackgroundTaskState.SUBMITTED, "resp-poll-exhausted"));
        when(gpt.pollDecisionChainBackground(any(), eq("resp-poll-exhausted"), eq(30_000L)))
                .thenReturn(
                        failure("PROVIDER_IO_FAILURE", AiBackgroundTaskState.RUNNING, true),
                        failure("PROVIDER_HTTP_503", AiBackgroundTaskState.RUNNING, true));
        DecisionChainAiOrchestratorServiceImpl service = service(
                gpt, usageGuard, callLogService, shortPollingProperties());

        AiDecisionChainResult result = service.invoke(cacheableRequest());

        assertThat(result.successful()).isFalse();
        assertThat(result.getTaskState()).isEqualTo(AiBackgroundTaskState.FAILED);
        assertThat(result.getFailureClassification()).isEqualTo("TRANSIENT_POLL_RETRY_EXHAUSTED");
        verify(gpt).submitDecisionChainBackground(any(), eq(30_000L));
        verify(gpt, org.mockito.Mockito.times(2))
                .pollDecisionChainBackground(any(), eq("resp-poll-exhausted"), eq(30_000L));
    }

    @Test
    void providerBackgroundRejectionAloneUsesPersistedApplicationWorkerFallback() {
        AiProviderClient gpt = nativeBackgroundClient();
        AiUsageGuard usageGuard = allowedUsageGuard(gpt);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO log = backgroundLog("call-fallback", 1);
        when(callLogService.startDecisionChainCall(any(), eq(gpt), any())).thenReturn(log);
        when(gpt.submitDecisionChainBackground(any(), eq(30_000L)))
                .thenReturn(failure("BACKGROUND_NOT_SUPPORTED", AiBackgroundTaskState.FAILED, false));
        when(gpt.executeDecisionChain(any(), eq(1_000L))).thenReturn(cacheableSuccess());
        DecisionChainAiOrchestratorServiceImpl service = service(
                gpt, usageGuard, callLogService, shortPollingProperties());

        AiDecisionChainResult result = service.invoke(cacheableRequest());

        assertThat(result.successful()).isTrue();
        assertThat(result.getBackgroundMode()).isEqualTo("APPLICATION_PERSISTED_WORKER");
        verify(gpt).submitDecisionChainBackground(any(), eq(30_000L));
        verify(gpt).executeDecisionChain(any(), eq(1_000L));
        verify(gpt, never()).pollDecisionChainBackground(any(), any(), anyLong());
    }

    @Test
    void existingAnalysisRoleWithDifferentInputHashFailsClosedBeforeProviderCall() {
        AiProviderClient gpt = nativeBackgroundClient();
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        AiCallLogService callLogService = mock(AiCallLogService.class);
        AiCallLogDO previous = backgroundLog("call-existing", 1);
        previous.setRequestHash("different-normalized-input-hash");
        when(callLogService.findLatestDecisionChainTask(
                "analysis-1", AiDecisionChainRole.GPT_FINAL.name())).thenReturn(previous);
        DecisionChainAiOrchestratorServiceImpl service = service(
                gpt, usageGuard, callLogService, shortPollingProperties());

        AiDecisionChainResult result = service.invoke(cacheableRequest());

        assertThat(result.successful()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo("AI_INPUT_HASH_MISMATCH");
        verify(gpt, never()).submitDecisionChainBackground(any(), anyLong());
        verify(gpt, never()).executeDecisionChain(any(), anyLong());
        verify(usageGuard, never()).evaluate(any(), any());
    }

    private static AiProviderClient client(AiProviderName provider, AiProviderRole role) {
        AiProviderClient client = mock(AiProviderClient.class);
        when(client.provider()).thenReturn(provider);
        when(client.role()).thenReturn(role);
        return client;
    }

    private static AiProviderClient nativeBackgroundClient() {
        AiProviderClient client = client(AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW);
        when(client.supportsNativeBackgroundDecisionChain()).thenReturn(true);
        return client;
    }

    private static AiUsageGuard allowedUsageGuard(AiProviderClient client) {
        AiUsageGuard usageGuard = mock(AiUsageGuard.class);
        when(usageGuard.evaluate(client, "analysis-1"))
                .thenReturn(AiUsageGuardResult.allowed(BigDecimal.ZERO));
        return usageGuard;
    }

    private static DecisionChainAiOrchestratorServiceImpl service(AiProviderClient client,
                                                                   AiUsageGuard usageGuard,
                                                                   AiCallLogService callLogService,
                                                                   AiOrchestratorProperties properties) {
        return new DecisionChainAiOrchestratorServiceImpl(
                List.of(client), usageGuard, callLogService, properties);
    }

    private static AiOrchestratorProperties shortPollingProperties() {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setPerAssetRoleMinIntervalMs(0L);
        properties.getBackgroundExecution().setInitialPollIntervalMs(1);
        properties.getBackgroundExecution().setMaxPollIntervalMs(2);
        properties.getBackgroundExecution().setGptJobDeadlineMs(1_000);
        return properties;
    }

    private static AiCallLogDO backgroundLog(String id, int attempt) {
        AiCallLogDO log = new AiCallLogDO();
        log.setCallId(id);
        log.setAttempt(attempt);
        log.setStartedAt(LocalDateTime.now(ZoneOffset.UTC));
        log.setSubmittedAt(LocalDateTime.now(ZoneOffset.UTC));
        log.setReservedCostUsd(BigDecimal.ZERO);
        return log;
    }

    private static AiDecisionChainResult active(AiBackgroundTaskState state, String responseId) {
        AiDecisionChainResult result = new AiDecisionChainResult();
        result.setProvider(AiProviderName.OPENAI);
        result.setRole(AiDecisionChainRole.GPT_FINAL);
        result.setCallStatus(AiProviderCallStatus.STARTED);
        result.setTaskState(state);
        result.setProviderRequestId(responseId);
        result.setBackgroundMode("PROVIDER_NATIVE");
        return result;
    }

    private static AiDecisionChainResult failure(String code,
                                                  AiBackgroundTaskState state,
                                                  boolean retryable) {
        AiDecisionChainResult result = AiDecisionChainResult.failed(
                AiProviderName.OPENAI, AiDecisionChainRole.GPT_FINAL,
                AiProviderCallStatus.FAILED, code);
        result.setTaskState(state);
        result.setFailureClassification(code);
        result.setRetryable(retryable);
        result.setBackgroundMode("PROVIDER_NATIVE");
        return result;
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
                "evidence", List.of(Map.ofEntries(
                        Map.entry("evidenceId", evidenceId),
                        Map.entry("type", "MARKET_STRUCTURE"),
                        Map.entry("source", "MARKET_SOURCE"),
                        Map.entry("sourceReference", "market://BTCUSDT/5m"),
                        Map.entry("sourceTraceId", sourceTraceId),
                        Map.entry("currentValue", "65000"),
                        Map.entry("changeFromBaseline", "+1.2%"),
                        Map.entry("direction", "BULLISH"),
                        Map.entry("strength", 80.0),
                        Map.entry("confidence", 87.0),
                        Map.entry("observedAt", "2026-08-12T00:00:00Z"),
                        Map.entry("freshness", "FRESH"),
                        Map.entry("analysisId", analysisId))),
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
                {"supportingEvidence":[{"evidenceId":"evidence-1","type":"MARKET_STRUCTURE",
                 "source":"MARKET_SOURCE","currentValue":"65000","change":"+1.2%",
                 "direction":"BULLISH","strength":80.0,"confidence":87.0,
                 "observedAt":"2026-08-12T00:00:00Z","freshness":"FRESH","analysisId":"analysis-1"}],
                 "opposingEvidence":[],
                 "biasAdjustment":{"before":"BULLISH","after":"BULLISH","reason":"unchanged"},
                 "candidateSummary":{"summary":"规则候选解释"}}
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
                 "candidateSummary":{"summary":"事件风险解释"}}
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
