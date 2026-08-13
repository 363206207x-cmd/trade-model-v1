package org.example.trademodel.controller;

import org.example.trademodel.ai.AiOrchestratorProperties;
import org.example.trademodel.ai.AiProviderClient;
import org.example.trademodel.ai.AiProviderName;
import org.example.trademodel.ai.AiProviderProperties;
import org.example.trademodel.ai.AiProviderReadiness;
import org.example.trademodel.ai.AiProviderRequest;
import org.example.trademodel.ai.AiProviderReviewResult;
import org.example.trademodel.ai.AiProviderRole;
import org.example.trademodel.entity.AiCallLogDO;
import org.example.trademodel.security.AuthenticatedUserIdResolver;
import org.example.trademodel.service.AiCallLogService;
import org.example.trademodel.service.AiDecisionOrchestratorService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AiOrchestratorControllerTest {
    @Test
    void statusDoesNotExposeApiKeyOrBaseUrl() throws Exception {
        AiOrchestratorProperties properties = properties();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiOrchestratorController(
                new FakeOrchestrator(),
                new FakeLogService(),
                properties,
                List.of(new FakeClient(properties.getOpenai()))
        )).build();

        mvc.perform(get("/api/ai/orchestrator/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[0].provider").value("OPENAI"))
                .andExpect(jsonPath("$.providers[0].ready").value(false))
                .andExpect(jsonPath("$.providers[0].modelReadiness").value("MODEL_CONFIGURED"))
                .andExpect(jsonPath("$.providers[0].configuredModel").value("gpt-5.6-luna"))
                .andExpect(jsonPath("$.providers[0].effectiveModel").value("gpt-5.6-luna"))
                .andExpect(jsonPath("$.providers[0].fallbackUsed").value(false))
                .andExpect(jsonPath("$.providers[0].modelStrategy").value("FAST_DECISION_MODEL"))
                .andExpect(jsonPath("$.providers[0].reasonCodes[0]")
                        .value("MODEL_AVAILABILITY_UNVERIFIED"))
                .andExpect(jsonPath("$.modelStrategy.GPT_FINAL").value("QUALITY_FIRST"))
                .andExpect(jsonPath("$.providerTimeouts.openaiMs").value(10000))
                .andExpect(jsonPath("$.providerTimeouts.geminiMs").value(25000))
                .andExpect(jsonPath("$.providerTimeouts.xaiMs").value(10000))
                .andExpect(jsonPath("$.providerTimeouts.overallMs").value(30000))
                .andExpect(jsonPath("$.providerTimeouts.configurationValid").value(true))
                .andExpect(content().string(not(containsString("sk-status-secret"))))
                .andExpect(content().string(not(containsString("https://secret-base.test"))));
    }

    @Test
    void controllerStatusAfterSuccessIsConsistent() throws Exception {
        AiOrchestratorProperties properties = properties();
        AiProviderReadiness readiness = new AiProviderReadiness(
                AiProviderName.GEMINI, AiProviderRole.GEMINI_CONSISTENCY_REVIEW,
                true, true, true, "gemini-3.5-flash", "models/gemini-3.5-flash",
                false, null, "BALANCED",
                org.example.trademodel.ai.AiModelReadinessStatus.MODEL_ACTIVE,
                List.of("MODEL_CALL_VERIFIED"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiOrchestratorController(
                new FakeOrchestrator(), new FakeLogService(), properties,
                List.of(new FakeClient(properties.getGemini(), readiness)))).build();

        mvc.perform(get("/api/ai/orchestrator/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[0].ready").value(true))
                .andExpect(jsonPath("$.providers[0].modelReadiness").value("MODEL_ACTIVE"))
                .andExpect(jsonPath("$.providers[0].reasonCodes[0]").value("MODEL_CALL_VERIFIED"))
                .andExpect(content().string(not(containsString("MODEL_AVAILABILITY_UNVERIFIED"))));
    }

    @Test
    void controllerStatusForSuccessfulFallbackIsConsistent() throws Exception {
        AiOrchestratorProperties properties = properties();
        AiProviderReadiness readiness = new AiProviderReadiness(
                AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                true, true, true, "gpt-5.6-luna", "gpt-5.5",
                true, "OPENAI_FALLBACK_GPT55", "FAST_DECISION_MODEL",
                org.example.trademodel.ai.AiModelReadinessStatus.MODEL_FALLBACK_ACTIVE,
                List.of("OPENAI_FALLBACK_GPT55"));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiOrchestratorController(
                new FakeOrchestrator(), new FakeLogService(), properties,
                List.of(new FakeClient(properties.getOpenai(), readiness)))).build();

        mvc.perform(get("/api/ai/orchestrator/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers[0].ready").value(true))
                .andExpect(jsonPath("$.providers[0].modelReadiness")
                        .value("MODEL_FALLBACK_ACTIVE"))
                .andExpect(jsonPath("$.providers[0].fallbackUsed").value(true))
                .andExpect(jsonPath("$.providers[0].fallbackReason")
                        .value("OPENAI_FALLBACK_GPT55"))
                .andExpect(jsonPath("$.providers[0].reasonCodes[0]")
                        .value("OPENAI_FALLBACK_GPT55"));
    }

    @Test
    void callLogsReturnsReadOnlySanitizedLogView() throws Exception {
        AuthenticatedUserIdResolver userIdResolver = mock(AuthenticatedUserIdResolver.class);
        when(userIdResolver.requireCurrentUserId()).thenReturn(7L);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiOrchestratorController(
                new FakeOrchestrator(),
                new FakeLogService(),
                properties(),
                List.of(),
                userIdResolver
        )).build();

        mvc.perform(get("/api/ai/call-logs").param("analysisId", "analysis-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].analysisId").value("analysis-1"))
                .andExpect(jsonPath("$[0].reviewOnly").value(false))
                .andExpect(jsonPath("$[0].notExecutable").value(true))
                .andExpect(jsonPath("$[0].notExecutionPlanCreation").value(false))
                .andExpect(jsonPath("$[0].notFinalExecutionPlanCreation").value(true))
                .andExpect(jsonPath("$[0].requestSummary").value("{\"ruleMarketBias\":\"BULLISH\"}"))
                .andExpect(jsonPath("$[0].contractType").value("DECISION_CHAIN_V4_1"))
                .andExpect(jsonPath("$[0].candidateId").value("candidate-1"))
                .andExpect(jsonPath("$[0].outputPayload").value("{\"summary\":\"candidate\"}"))
                .andExpect(jsonPath("$[0].errorMessage").value("provider timeout"))
                .andExpect(jsonPath("$[0].ruleVersion").value("FUNDAMENTAL_AI_V4_1"))
                .andExpect(jsonPath("$[0].createdAt").exists())
                .andExpect(content().string(not(containsString("api-key"))));
        verify(userIdResolver).requireCurrentUserId();
    }

    private static AiOrchestratorProperties properties() {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.getOpenai().setEnabled(true);
        properties.getOpenai().setApiKey("sk-status-secret");
        properties.getOpenai().getGptFinal().setFastModel("gpt-5.6-luna");
        properties.getOpenai().getGptFinal().setReasoningModel("gpt-5.6-sol");
        properties.getOpenai().getGptFinal().setFallbackModels(List.of("gpt-5.5", "gpt-5.4"));
        properties.getOpenai().setBaseUrl("https://secret-base.test");
        properties.getOpenai().setRequestsPerMinute(1);
        properties.getOpenai().setInputCostPerMillionUsd(BigDecimal.ONE);
        properties.getOpenai().setOutputCostPerMillionUsd(BigDecimal.ONE);
        return properties;
    }

    private static final class FakeOrchestrator implements AiDecisionOrchestratorService {
        @Override public org.example.trademodel.ai.AiOrchestratorResult review(AiProviderRequest request) { return null; }
        @Override public List<AiProviderReadiness> providerReadiness() { return List.of(); }
    }

    private static final class FakeClient implements AiProviderClient {
        private final AiProviderProperties properties;
        private final AiProviderReadiness readiness;
        private FakeClient(AiProviderProperties properties) {
            this(properties, new AiProviderReadiness(
                    AiProviderName.OPENAI, AiProviderRole.GPT_RULE_REVIEW,
                    true, true, false,
                    properties.getConfiguredModel(), properties.getEffectiveModel(), false, null,
                    "FAST_DECISION_MODEL",
                    org.example.trademodel.ai.AiModelReadinessStatus.MODEL_CONFIGURED,
                    List.of("MODEL_AVAILABILITY_UNVERIFIED")));
        }
        private FakeClient(AiProviderProperties properties, AiProviderReadiness readiness) {
            this.properties = properties;
            this.readiness = readiness;
        }
        @Override public AiProviderName provider() { return readiness.getProvider(); }
        @Override public AiProviderRole role() { return readiness.getRole(); }
        @Override public AiProviderReadiness readiness() {
            return readiness;
        }
        @Override public AiProviderReviewResult review(AiProviderRequest request) { return null; }
        @Override public AiProviderProperties providerProperties() { return properties; }
    }

    private static final class FakeLogService implements AiCallLogService {
        @Override public AiCallLogDO startCall(AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd) { return null; }
        @Override public void completeCall(AiCallLogDO log, AiProviderReviewResult result) { }
        @Override public AiCallLogDO recordSkipped(AiProviderRequest request, AiProviderClient client, AiProviderReviewResult result, BigDecimal reservedCostUsd) { return null; }
        @Override public List<AiCallLogDO> query(String analysisId, String traceId, String providerName, String callStatus, LocalDateTime from, LocalDateTime to, int limit) {
            return fixture();
        }
        @Override public List<AiCallLogDO> queryOwned(Long userId, String analysisId, String traceId,
                                                       String candidateId, String role, String providerName,
                                                       String callStatus, LocalDateTime from, LocalDateTime to,
                                                       int limit) {
            if (!Long.valueOf(7L).equals(userId)) {
                throw new AssertionError("authenticated owner was not propagated");
            }
            return fixture();
        }
        private static List<AiCallLogDO> fixture() {
            AiCallLogDO log = new AiCallLogDO();
            log.setCallId("call-1");
            log.setAnalysisId("analysis-1");
            log.setTraceId("trace-1");
            log.setProviderName("OPENAI");
            log.setAiRole("GPT_FINAL");
            log.setCallStatus("SUCCESS");
            log.setRequestSummary("{\"ruleMarketBias\":\"BULLISH\"}");
            log.setResponseSummary("{\"stance\":\"SUPPORT\"}");
            log.setContractType("DECISION_CHAIN_V4_1");
            log.setCandidateId("candidate-1");
            log.setOutputPayload("{\"summary\":\"candidate\"}");
            log.setErrorMessage("provider timeout");
            log.setReviewOnly(false);
            log.setNotExecutable(true);
            log.setNotExecutionPlanCreation(false);
            log.setNotFinalExecutionPlanCreation(true);
            log.setRuleVersion("FUNDAMENTAL_AI_V4_1");
            log.setCreatedAt(LocalDateTime.of(2026, 8, 12, 5, 0));
            return List.of(log);
        }
        @Override public int countProviderAttemptsSince(String providerName, LocalDateTime since) { return 0; }
        @Override public BigDecimal sumChargeableCostSince(LocalDateTime since) { return BigDecimal.ZERO; }
    }
}
