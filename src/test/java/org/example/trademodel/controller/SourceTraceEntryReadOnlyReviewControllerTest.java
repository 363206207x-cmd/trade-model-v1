package org.example.trademodel.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Field;
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
    void malformedSuppliedDtoFailsClosedIndependently() throws Exception {
        assertMalformedFieldFailsClosed("completionStatus", response -> response.setCompletionStatus("POSITIVE_FIXTURE_READY"));
        assertMalformedFieldFailsClosed(
                "completionTransition",
                response -> response.setCompletionTransition("INCOMPLETE_TO_POSITIVE_FIXTURE_READY")
        );
        assertMalformedFieldFailsClosed("downgradeReason", response -> response.setDowngradeReason(null));
        assertMalformedFieldFailsClosed("downgradeReason", response -> response.setDowngradeReason("READY_TO_TRADE"));
        assertMalformedFieldFailsClosed("reviewMode", response -> response.setReviewMode(null));
        assertMalformedFieldFailsClosed("manualReviewRequired", response -> response.setManualReviewRequired(false));
        assertMalformedFieldFailsClosed("notTradeInstruction", response -> response.setNotTradeInstruction(false));
        assertMalformedFieldFailsClosed(
                "sourceTraceEntryCompleted",
                response -> response.setSourceTraceEntryCompleted(true)
        );
        assertMalformedFieldFailsClosed("completionReady", response -> response.setCompletionReady(true));
        assertMalformedFieldFailsClosed(
                "readOnlyIntegrationSeamUnwired",
                response -> response.setReadOnlyIntegrationSeamUnwired(false)
        );
        assertMalformedFieldFailsClosed("missingFields", response -> response.setMissingFields(List.of()));
        assertMalformedFieldFailsClosed("blockingFields", response -> response.setBlockingFields(List.of()));
    }

    @Test
    void unsafeFieldsSerializeAsBlockingReviewEvidence() throws Exception {
        SourceTraceEntryReadOnlyApiResponseDTO response = safeAlreadyBuiltResponse();
        response.setDowngradeReason("UNSAFE_COMPLETION");
        response.setMissingFields(List.of("readOnlyIntegrationSeamUnwired", "BOUNDARYCANDIDATESERVICE_VALID"));
        response.setUnsafeFields(List.of("BOUNDARYCANDIDATESERVICE_VALID"));
        response.setBlockingFields(List.of("readOnlyIntegrationSeamUnwired", "BOUNDARYCANDIDATESERVICE_VALID"));

        mockMvc = mockMvcWith(response);

        mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.completionTransition").value("NONE"))
                .andExpect(jsonPath("$.downgradeReason").value("UNSAFE_COMPLETION"))
                .andExpect(jsonPath("$.unsafeFields[0]").value("BOUNDARYCANDIDATESERVICE_VALID"))
                .andExpect(jsonPath("$.blockingFields[1]").value("BOUNDARYCANDIDATESERVICE_VALID"))
                .andExpect(jsonPath("$.sourceTraceEntryCompleted").value(false))
                .andExpect(jsonPath("$.completionReady").value(false));
    }

    @Test
    void runtimeLikeFieldsSerializeOnlyAsBlockers() throws Exception {
        String[] runtimeLikeValues = {
                "LATEST_PRICE_ONLY",
                "RAW_KLINE_ONLY",
                "AI_TEXT",
                "DASHBOARD_TEXT",
                "EXTERNAL_DATA",
                "ORDER_DATA",
                "EXECUTION_DATA"
        };

        for (String unsafeValue : runtimeLikeValues) {
            assertUnsafeValueSerializesOnlyAsBlocker(unsafeValue);
        }
    }

    @Test
    void productionLikeFieldsSerializeOnlyAsBlockers() throws Exception {
        String[] productionLikeValues = {
                "BOUNDARYCANDIDATESERVICE_VALID",
                "EXECUTIONPLAN_READY",
                "SOURCETRACE_RUNTIME_COMPLETION",
                "PRODUCTION_COMPLETION"
        };

        for (String unsafeValue : productionLikeValues) {
            assertUnsafeValueSerializesOnlyAsBlocker(unsafeValue);
        }
    }

    @Test
    void tradeReadyLookingValuesSerializeOnlyAsBlockers() throws Exception {
        String[] tradeReadyLookingValues = {
                "tradeReady",
                "readyToTrade",
                "entryReady",
                "executionReady",
                "Valid",
                "Completed",
                "Signal",
                "trade advice",
                "Buy",
                "Sell",
                "Open",
                "Close",
                "Reverse"
        };

        for (String unsafeValue : tradeReadyLookingValues) {
            assertUnsafeValueSerializesOnlyAsBlocker(unsafeValue);
        }
    }

    @Test
    void completeSafeLookingApiResponseRemainsIncompleteNoneUnwiredAndReviewOnly() throws Exception {
        SourceTraceEntryReadOnlyApiResponseDTO response = safeAlreadyBuiltResponse();

        mockMvc = mockMvcWith(response);

        MvcResult result = mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.completionTransition").value("NONE"))
                .andExpect(jsonPath("$.downgradeReason").value("COMPLETION_UNWIRED"))
                .andExpect(jsonPath("$.reviewMode").value("REVIEW_ONLY"))
                .andExpect(jsonPath("$.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTraceEntryCompleted").value(false))
                .andExpect(jsonPath("$.completionReady").value(false))
                .andReturn();

        assertNoForbiddenPositiveResponseSurface(result.getResponse().getContentAsString());
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
    void unsafeAndTradeReadyLookingResponsesSerializeNoGeneratedEntryStopTakeProfitOrRiskRewardValues()
            throws Exception {
        MvcResult result = responseForUnsafeValue("tradeReady");

        String body = result.getResponse().getContentAsString();
        assertNoGeneratedValueSurface(body);
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
        assertThat(Arrays.stream(SourceTraceEntryReadOnlyReviewController.class.getDeclaredFields())
                        .map(Field::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("resolver");
                    assertThat(name).doesNotContain("validator");
                    assertThat(name).doesNotContain("readiness");
                    assertThat(name).doesNotContain("dashboard");
                    assertThat(name).doesNotContain("schema");
                    assertThat(name).doesNotContain("automation");
                    assertThat(name).doesNotContain("external");
                    assertThat(name).doesNotContain("database");
                    assertThat(name).doesNotContain("scheduler");
                    assertThat(name).doesNotContain("order");
                    assertThat(name).doesNotContain("execution");
                });
    }

    @Test
    void controllerHasNoDatabaseSchedulerExternalOrderOrAutomationCallSurface() {
        assertThat(Arrays.stream(SourceTraceEntryReadOnlyReviewController.class.getDeclaredMethods())
                        .map(Method::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("save");
                    assertThat(name).doesNotContain("write");
                    assertThat(name).doesNotContain("persist");
                    assertThat(name).doesNotContain("database");
                    assertThat(name).doesNotContain("scheduler");
                    assertThat(name).doesNotContain("external");
                    assertThat(name).doesNotContain("api");
                    assertThat(name).doesNotContain("order");
                    assertThat(name).doesNotContain("automation");
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

    private void assertMalformedFieldFailsClosed(
            String expectedMissingField,
            ResponseMutation responseMutation
    ) throws Exception {
        SourceTraceEntryReadOnlyApiResponseDTO response = safeAlreadyBuiltResponse();
        responseMutation.apply(response);

        mockMvc = mockMvcWith(response);

        MvcResult result = mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionStatus").value("INCOMPLETE"))
                .andExpect(jsonPath("$.completionTransition").value("NONE"))
                .andExpect(jsonPath("$.downgradeReason").value("MISSING_REQUIRED_FIELD"))
                .andExpect(jsonPath("$.reviewMode").value("REVIEW_ONLY"))
                .andExpect(jsonPath("$.manualReviewRequired").value(true))
                .andExpect(jsonPath("$.notTradeInstruction").value(true))
                .andExpect(jsonPath("$.sourceTraceEntryCompleted").value(false))
                .andExpect(jsonPath("$.completionReady").value(false))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(expectedMissingField);
        assertNoGeneratedValueSurface(body);
        assertNoForbiddenPositiveResponseSurface(body);
    }

    private void assertUnsafeValueSerializesOnlyAsBlocker(String unsafeValue) throws Exception {
        MvcResult result = responseForUnsafeValue(unsafeValue);

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains(unsafeValue);
        assertThat(body).contains("blockingFields");
        assertThat(body).contains("unsafeFields");
        assertThat(body).contains("\"completionStatus\":\"INCOMPLETE\"");
        assertThat(body).contains("\"completionTransition\":\"NONE\"");
        assertThat(body).contains("\"downgradeReason\":\"UNSAFE_COMPLETION\"");
        assertThat(body).contains("\"reviewMode\":\"REVIEW_ONLY\"");
        assertThat(body).contains("\"manualReviewRequired\":true");
        assertThat(body).contains("\"notTradeInstruction\":true");
        assertThat(body).contains("\"sourceTraceEntryCompleted\":false");
        assertThat(body).contains("\"completionReady\":false");
        assertNoGeneratedValueSurface(body);
    }

    private MvcResult responseForUnsafeValue(String unsafeValue) throws Exception {
        SourceTraceEntryReadOnlyApiResponseDTO response = safeAlreadyBuiltResponse();
        response.setDowngradeReason("UNSAFE_COMPLETION");
        response.setMissingFields(List.of(unsafeValue, "readOnlyIntegrationSeamUnwired"));
        response.setUnsafeFields(List.of(unsafeValue));
        response.setBlockingFields(List.of(unsafeValue, "readOnlyIntegrationSeamUnwired"));

        mockMvc = mockMvcWith(response);

        return mockMvc.perform(get(ROUTE))
                .andExpect(status().isOk())
                .andReturn();
    }

    private MockMvc mockMvcWith(SourceTraceEntryReadOnlyApiResponseDTO response) {
        return MockMvcBuilders
                .standaloneSetup(new SourceTraceEntryReadOnlyReviewController(() -> response))
                .build();
    }

    private void assertNoGeneratedValueSurface(String body) {
        assertThat(body).doesNotContain("entryPrice");
        assertThat(body).doesNotContain("stopPrice");
        assertThat(body).doesNotContain("takeProfit");
        assertThat(body).doesNotContain("riskReward");
    }

    private void assertNoForbiddenPositiveResponseSurface(String body) {
        assertThat(body).doesNotContain("\"tradeReady\"");
        assertThat(body).doesNotContain("\"readyToTrade\"");
        assertThat(body).doesNotContain("\"entryReady\"");
        assertThat(body).doesNotContain("\"executionReady\"");
        assertThat(body).doesNotContain("\"Valid\"");
        assertThat(body).doesNotContain("\"Completed\"");
        assertThat(body).doesNotContain("\"Signal\"");
        assertThat(body).doesNotContain("\"Buy\"");
        assertThat(body).doesNotContain("\"Sell\"");
        assertThat(body).doesNotContain("\"Open\"");
        assertThat(body).doesNotContain("\"Close\"");
        assertThat(body).doesNotContain("\"Reverse\"");
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

    private interface ResponseMutation {
        void apply(SourceTraceEntryReadOnlyApiResponseDTO response);
    }
}
