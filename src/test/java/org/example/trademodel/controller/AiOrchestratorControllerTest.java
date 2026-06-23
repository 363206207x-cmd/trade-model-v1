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
                .andExpect(jsonPath("$.providers[0].ready").value(true))
                .andExpect(content().string(not(containsString("sk-status-secret"))))
                .andExpect(content().string(not(containsString("https://secret-base.test"))));
    }

    @Test
    void callLogsReturnsReadOnlySanitizedLogView() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new AiOrchestratorController(
                new FakeOrchestrator(),
                new FakeLogService(),
                properties(),
                List.of()
        )).build();

        mvc.perform(get("/api/ai/call-logs").param("analysisId", "analysis-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].analysisId").value("analysis-1"))
                .andExpect(jsonPath("$[0].reviewOnly").value(true))
                .andExpect(jsonPath("$[0].notExecutable").value(true))
                .andExpect(jsonPath("$[0].requestSummary").value("{\"ruleMarketBias\":\"BULLISH\"}"))
                .andExpect(content().string(not(containsString("api-key"))));
    }

    private static AiOrchestratorProperties properties() {
        AiOrchestratorProperties properties = new AiOrchestratorProperties();
        properties.setEnabled(true);
        properties.getOpenai().setEnabled(true);
        properties.getOpenai().setApiKey("sk-status-secret");
        properties.getOpenai().setModel("gpt-status");
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
        private FakeClient(AiProviderProperties properties) { this.properties = properties; }
        @Override public AiProviderName provider() { return AiProviderName.OPENAI; }
        @Override public AiProviderRole role() { return AiProviderRole.GPT_RULE_REVIEW; }
        @Override public AiProviderReadiness readiness() {
            return new AiProviderReadiness(provider(), role(), true, true, true, properties.getModel(), List.of());
        }
        @Override public AiProviderReviewResult review(AiProviderRequest request) { return null; }
        @Override public AiProviderProperties providerProperties() { return properties; }
    }

    private static final class FakeLogService implements AiCallLogService {
        @Override public AiCallLogDO startCall(AiProviderRequest request, AiProviderClient client, BigDecimal reservedCostUsd) { return null; }
        @Override public void completeCall(AiCallLogDO log, AiProviderReviewResult result) { }
        @Override public AiCallLogDO recordSkipped(AiProviderRequest request, AiProviderClient client, AiProviderReviewResult result, BigDecimal reservedCostUsd) { return null; }
        @Override public List<AiCallLogDO> query(String analysisId, String traceId, String providerName, String callStatus, LocalDateTime from, LocalDateTime to, int limit) {
            AiCallLogDO log = new AiCallLogDO();
            log.setCallId("call-1");
            log.setAnalysisId("analysis-1");
            log.setTraceId("trace-1");
            log.setProviderName("OPENAI");
            log.setAiRole("GPT_RULE_REVIEW");
            log.setCallStatus("SUCCESS");
            log.setRequestSummary("{\"ruleMarketBias\":\"BULLISH\"}");
            log.setResponseSummary("{\"stance\":\"SUPPORT\"}");
            log.setReviewOnly(true);
            log.setNotExecutable(true);
            return List.of(log);
        }
        @Override public int countProviderAttemptsSince(String providerName, LocalDateTime since) { return 0; }
        @Override public BigDecimal sumChargeableCostSince(LocalDateTime since) { return BigDecimal.ZERO; }
    }
}
