package org.example.trademodel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyApiResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

class SourceTraceEntryReadOnlyReviewControllerTest {

    private static final String ROUTE = "/api/review/source-trace-entry-completion/state";

    private MockMvc mockMvc;
    private SourceTraceEntryReadOnlyApiResponseDTO alreadyBuiltResponse;

    @BeforeEach
    void setUp() {
        alreadyBuiltResponse = safeAlreadyBuiltResponse();
        SourceTraceEntryReadOnlyReviewController controller =
                new SourceTraceEntryReadOnlyReviewController(() -> alreadyBuiltResponse);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void endpointPreservesAlreadyBuiltReviewOutputOnly() throws Exception {
        mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BTCUSDT"))
                .andExpect(jsonPath("$.timeframe").value("15m"))
                .andExpect(jsonPath("$.completionStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.completionTransition").value("NONE"))
                .andExpect(jsonPath("$.downgradeReason").value("COMPLETION_UNWIRED"))
                .andExpect(jsonPath("$.readOnlyIntegrationSeamUnwired").value(true))
                .andExpect(jsonPath("$.reviewMode").value("REVIEW_ONLY"))
                .andExpect(jsonPath("$.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTraceEntryCompleted").value(false))
                .andExpect(jsonPath("$.completionReady").value(false))
                .andExpect(jsonPath("$.missingFields[0]").value("readOnlyIntegrationSeamUnwired"))
                .andExpect(jsonPath("$.unsafeFields").isArray())
                .andExpect(jsonPath("$.blockingFields[0]").value("readOnlyIntegrationSeamUnwired"))
                .andExpect(jsonPath("$.downgradeLabel").value("Completion path unwired"));
    }

    @Test
    void nullAlreadyBuiltOutputFailsClosed() throws Exception {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SourceTraceEntryReadOnlyReviewController(() -> null))
                .build();

        mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.completionTransition").value("NONE"))
                .andExpect(jsonPath("$.downgradeReason").value("MISSING_REQUIRED_FIELD"))
                .andExpect(jsonPath("$.readOnlyIntegrationSeamUnwired").value(false))
                .andExpect(jsonPath("$.reviewMode").value("REVIEW_ONLY"))
                .andExpect(jsonPath("$.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTraceEntryCompleted").value(false))
                .andExpect(jsonPath("$.completionReady").value(false))
                .andExpect(jsonPath("$.missingFields[0]").value("sourceTraceEntryReadOnlyApiResponseOutput"))
                .andExpect(jsonPath("$.missingFields[1]").value("readOnlyIntegrationSeamUnwired"))
                .andExpect(jsonPath("$.blockingFields[2]").value("blockingFields"));
    }

    @Test
    void unavailableAlreadyBuiltOutputFailsClosed() throws Exception {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SourceTraceEntryReadOnlyReviewController(() -> {
                    throw new IllegalStateException("unavailable");
                }))
                .build();

        mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.completionTransition").value("NONE"))
                .andExpect(jsonPath("$.downgradeReason").value("MISSING_REQUIRED_FIELD"))
                .andExpect(jsonPath("$.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTraceEntryCompleted").value(false))
                .andExpect(jsonPath("$.completionReady").value(false))
                .andExpect(jsonPath("$.blockingFields[0]").value("sourceTraceEntryReadOnlyApiResponseOutput"));
    }

    @Test
    void unsafeAlreadyBuiltOutputRemainsBlockingReviewEvidence() throws Exception {
        alreadyBuiltResponse.setCompletionStatus("POSITIVE_FIXTURE_READY");
        alreadyBuiltResponse.setCompletionTransition("INCOMPLETE_TO_POSITIVE_FIXTURE_READY");
        alreadyBuiltResponse.setDowngradeReason(null);
        alreadyBuiltResponse.setReviewMode(null);
        alreadyBuiltResponse.setReadOnlyIntegrationSeamUnwired(false);
        alreadyBuiltResponse.setManualReviewRequired(false);
        alreadyBuiltResponse.setNotTradeInstruction(false);
        alreadyBuiltResponse.setSourceTraceEntryCompleted(true);
        alreadyBuiltResponse.setCompletionReady(true);
        alreadyBuiltResponse.setMissingFields(List.of());
        alreadyBuiltResponse.setUnsafeFields(List.of("tradeReady"));
        alreadyBuiltResponse.setBlockingFields(List.of());

        mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.completionTransition").value("NONE"))
                .andExpect(jsonPath("$.downgradeReason").value("UNSAFE_COMPLETION"))
                .andExpect(jsonPath("$.reviewMode").value("REVIEW_ONLY"))
                .andExpect(jsonPath("$.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTraceEntryCompleted").value(false))
                .andExpect(jsonPath("$.completionReady").value(false))
                .andExpect(jsonPath("$.missingFields").isArray())
                .andExpect(jsonPath("$.unsafeFields[0]").value("tradeReady"))
                .andExpect(jsonPath("$.blockingFields").isArray())
                .andExpect(jsonPath("$.downgradeLabel").value("Unsafe completion evidence"));
    }

    @Test
    void endpointSerializesNoGeneratedEntryStopTakeProfitOrRiskRewardValues() throws Exception {
        MvcResult result = mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("entryPrice");
        assertThat(body).doesNotContain("stopPrice");
        assertThat(body).doesNotContain("takeProfit");
        assertThat(body).doesNotContain("riskReward");
    }

    @Test
    void routeUsesReadOnlyReviewWordingAndAvoidsForbiddenNames() throws NoSuchMethodException {
        RequestMapping requestMapping = SourceTraceEntryReadOnlyReviewController.class.getAnnotation(RequestMapping.class);
        GetMapping getMapping = SourceTraceEntryReadOnlyReviewController.class
                .getDeclaredMethod("reviewState")
                .getAnnotation(GetMapping.class);

        String route = requestMapping.value()[0] + getMapping.value()[0];
        assertThat(route).isEqualTo(ROUTE);
        assertThat(route).contains("/api/review/");
        assertThat(route).contains("source-trace-entry-completion");
        assertNoForbiddenSurface(route);
    }

    @Test
    void controllerEndpointExposesNoForbiddenMethodSurfaceOrProductionDependencies() {
        assertThat(SourceTraceEntryReadOnlyReviewController.class.getAnnotation(Service.class)).isNull();
        assertThat(SourceTraceEntryReadOnlyReviewController.class.getAnnotation(Component.class)).isNull();
        assertThat(SourceTraceEntryReadOnlyReviewController.class.getAnnotation(Repository.class)).isNull();

        assertThat(Arrays.stream(SourceTraceEntryReadOnlyReviewController.class.getDeclaredMethods())
                        .map(Method::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(this::assertNoForbiddenSurface);
        assertThat(Arrays.stream(SourceTraceEntryReadOnlyReviewController.class.getDeclaredFields())
                        .map(field -> field.getType().getName()))
                .allSatisfy(typeName -> {
                    assertThat(typeName).doesNotContain("resolver");
                    assertThat(typeName).doesNotContain("validator");
                    assertThat(typeName).doesNotContain("readiness");
                    assertThat(typeName).doesNotContain("dashboard");
                    assertThat(typeName).doesNotContain("schema");
                    assertThat(typeName).doesNotContain("automation");
                    assertThat(typeName).doesNotContain("external");
                    assertThat(typeName).doesNotContain("order");
                });
    }

    private SourceTraceEntryReadOnlyApiResponseDTO safeAlreadyBuiltResponse() {
        SourceTraceEntryReadOnlyApiResponseDTO response = new SourceTraceEntryReadOnlyApiResponseDTO();
        response.setSymbol("BTCUSDT");
        response.setTimeframe("15m");
        response.setCompletionStatus("INCOMPLETE");
        response.setCompletionTransition("NONE");
        response.setDowngradeReason("COMPLETION_UNWIRED");
        response.setReviewMode("REVIEW_ONLY");
        response.setReadOnlyIntegrationSeamUnwired(true);
        response.setManualReviewRequired(true);
        response.setNotTradeInstruction(true);
        response.setSourceTraceEntryCompleted(false);
        response.setCompletionReady(false);
        response.setMissingFields(List.of("readOnlyIntegrationSeamUnwired"));
        response.setUnsafeFields(List.of());
        response.setBlockingFields(List.of("readOnlyIntegrationSeamUnwired"));
        return response;
    }

    private void assertNoForbiddenSurface(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        assertThat(normalized).doesNotContain("trade-ready");
        assertThat(normalized).doesNotContain("tradeready");
        assertThat(normalized).doesNotContain("ready-to-trade");
        assertThat(normalized).doesNotContain("readytotrade");
        assertThat(normalized).doesNotContain("entry-ready");
        assertThat(normalized).doesNotContain("entryready");
        assertThat(normalized).doesNotContain("execution-ready");
        assertThat(normalized).doesNotContain("executionready");
        assertThat(normalized).doesNotContain("valid");
        assertThat(normalized).doesNotContain("completed");
        assertThat(normalized).doesNotContain("signal");
        assertThat(normalized).doesNotContain("buy");
        assertThat(normalized).doesNotContain("sell");
        assertThat(normalized).doesNotContain("open");
        assertThat(normalized).doesNotContain("close");
        assertThat(normalized).doesNotContain("reverse");
        assertThat(normalized).doesNotContain("order");
        assertThat(normalized).doesNotContain("execute");
        assertThat(normalized).doesNotContain("auto-trade");
        assertThat(normalized).doesNotContain("autotrade");
    }
}
