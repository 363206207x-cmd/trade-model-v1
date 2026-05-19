package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationCompletionContext;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyApiResponseDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyCompletionRequest;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyDisplayDTO;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

class SourceTraceEntryReadOnlyApiResponseMapperTest {

    private final SourceTraceEntryReadOnlyApiResponseMapper mapper =
            new SourceTraceEntryReadOnlyApiResponseMapper();
    private final SourceTraceEntryReadOnlyDisplayMapper displayMapper =
            new SourceTraceEntryReadOnlyDisplayMapper();
    private final SourceTraceEntryReadOnlyIntegrationSeam seam =
            new SourceTraceEntryReadOnlyIntegrationSeam();

    @Test
    void nullDisplayDtoFailsClosed() {
        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(null);

        assertBaseApiSafety(response);
        assertThat(response.getCompletionStatus()).isEqualTo("INCOMPLETE");
        assertThat(response.getCompletionTransition()).isEqualTo("NONE");
        assertThat(response.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
        assertThat(response.isReadOnlyIntegrationSeamUnwired()).isFalse();
        assertThat(response.getMissingFields()).containsExactly(
                "sourceTraceEntryReadOnlyDisplayOutput",
                "readOnlyIntegrationSeamUnwired",
                "blockingFields"
        );
        assertThat(response.getBlockingFields()).containsExactly(
                "sourceTraceEntryReadOnlyDisplayOutput",
                "readOnlyIntegrationSeamUnwired",
                "blockingFields"
        );
    }

    @Test
    void missingDisplaySafetyFlagsFailClosedIndependently() {
        assertDisplaySafetyGap("reviewMode", display -> display.setReviewMode(null));
        assertDisplaySafetyGap("manualReviewRequired", display -> display.setManualReviewRequired(false));
        assertDisplaySafetyGap("notTradeInstruction", display -> display.setNotTradeInstruction(false));
        assertDisplaySafetyGap("sourceTraceEntryCompleted", display -> display.setSourceTraceEntryCompleted(true));
        assertDisplaySafetyGap("completionReady", display -> display.setCompletionReady(true));
        assertDisplaySafetyGap(
                "readOnlyIntegrationSeamUnwired",
                display -> display.setReadOnlyIntegrationSeamUnwired(false)
        );
    }

    @Test
    void unsafeStatusTransitionAndDowngradeMetadataFailClosedIndependently() {
        assertDisplaySafetyGap("completionStatus", display -> display.setCompletionStatus("POSITIVE_FIXTURE_READY"));
        assertDisplaySafetyGap(
                "completionTransition",
                display -> display.setCompletionTransition("INCOMPLETE_TO_POSITIVE_FIXTURE_READY")
        );
        assertDisplaySafetyGap("downgradeReason", display -> display.setDowngradeReason(null));
        assertDisplaySafetyGap("downgradeReason", display -> display.setDowngradeReason("DEFAULT_FAIL_CLOSED"));
        assertDisplaySafetyGap("downgradeReason", display -> display.setDowngradeReason("READY_TO_TRADE"));
    }

    @Test
    void emptyMissingFieldsDoesNotImplyCompletion() {
        SourceTraceEntryReadOnlyDisplayDTO display = completeDisplay();
        display.setMissingFields(List.of());

        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(display);

        assertBaseApiSafety(response);
        assertThat(response.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
        assertThat(response.getMissingFields()).contains("missingFields");
        assertThat(response.getCompletionStatus()).isEqualTo("INCOMPLETE");
        assertThat(response.getCompletionTransition()).isEqualTo("NONE");
    }

    @Test
    void emptyBlockingFieldsDoesNotImplyCompletion() {
        SourceTraceEntryReadOnlyDisplayDTO display = completeDisplay();
        display.setBlockingFields(List.of());

        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(display);

        assertBaseApiSafety(response);
        assertThat(response.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
        assertThat(response.getMissingFields()).contains("blockingFields");
        assertThat(response.getCompletionStatus()).isEqualTo("INCOMPLETE");
        assertThat(response.getCompletionTransition()).isEqualTo("NONE");
    }

    @Test
    void missingOrEmptyBlockerListsDoNotImplyCompletion() {
        SourceTraceEntryReadOnlyDisplayDTO display = completeDisplay();
        display.setMissingFields(List.of());
        display.setBlockingFields(List.of());
        display.setUnsafeFields(List.of());

        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(display);

        assertBaseApiSafety(response);
        assertThat(response.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
        assertThat(response.getMissingFields()).contains(
                "missingFields",
                "blockingFields"
        );
    }

    @Test
    void unsafeFieldsSerializeAsBlockingReviewEvidence() {
        SourceTraceEntryReadOnlyDisplayDTO display = displayWith(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION,
                List.of("BOUNDARYCANDIDATESERVICE_VALID", "readOnlyIntegrationSeamUnwired"),
                List.of("BOUNDARYCANDIDATESERVICE_VALID"),
                List.of("BOUNDARYCANDIDATESERVICE_VALID", "readOnlyIntegrationSeamUnwired")
        );

        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(display);

        assertBaseApiSafety(response);
        assertThat(response.getDowngradeReason()).isEqualTo("UNSAFE_COMPLETION");
        assertThat(response.getUnsafeFields()).containsExactly("BOUNDARYCANDIDATESERVICE_VALID");
        assertThat(response.getBlockingFields()).contains(
                "BOUNDARYCANDIDATESERVICE_VALID",
                "readOnlyIntegrationSeamUnwired"
        );
    }

    @Test
    void runtimeLikeFieldsSerializeOnlyAsBlockers() {
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
    void productionLikeFieldsSerializeOnlyAsBlockers() {
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
    void tradeReadyLookingValuesSerializeOnlyAsBlockers() {
        String[] tradeReadyLookingValues = {
                "tradeReady",
                "readyToTrade",
                "entryReady",
                "executionReady",
                "Valid",
                "Completed",
                "Signal",
                "Buy",
                "Sell",
                "Open",
                "Close",
                "Reverse",
                "trade advice"
        };

        for (String unsafeValue : tradeReadyLookingValues) {
            assertUnsafeValueSerializesOnlyAsBlocker(unsafeValue);
        }
    }

    @Test
    void runtimeLikeProductionLikeAndTradeReadyLookingValuesSerializeAsBlockers() {
        String[] unsafeValues = {
                "LATEST_PRICE_ONLY",
                "RAW_KLINE_ONLY",
                "AI_TEXT",
                "DASHBOARD_TEXT",
                "EXTERNAL_DATA",
                "ORDER_DATA",
                "EXECUTION_DATA",
                "BOUNDARYCANDIDATESERVICE_VALID",
                "EXECUTIONPLAN_READY",
                "SOURCETRACE_RUNTIME_COMPLETION",
                "PRODUCTION_COMPLETION",
                "tradeReady",
                "readyToTrade",
                "entryReady",
                "executionReady",
                "Valid",
                "Completed",
                "Signal",
                "Buy",
                "Sell",
                "Open",
                "Close",
                "Reverse",
                "trade advice"
        };

        for (String unsafeValue : unsafeValues) {
            SourceTraceEntryReadOnlyDisplayDTO display = displayWith(
                    SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION,
                    List.of(unsafeValue, "readOnlyIntegrationSeamUnwired"),
                    List.of(unsafeValue),
                    List.of(unsafeValue, "readOnlyIntegrationSeamUnwired")
            );

            SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(display);

            assertBaseApiSafety(response);
            assertThat(response.getDowngradeReason()).isEqualTo("UNSAFE_COMPLETION");
            assertThat(response.getUnsafeFields()).containsExactly(unsafeValue);
            assertThat(response.getBlockingFields()).contains(unsafeValue);
            assertNoForbiddenPositiveLabels(response);
        }
    }

    @Test
    void requiredDowngradeStatesArePreserved() {
        assertDowngrade(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD,
                "Missing required source evidence",
                "Required display evidence is missing or malformed."
        );
        assertDowngrade(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION,
                "Unsafe completion evidence",
                "Unsafe or runtime-like evidence blocks completion and requires review."
        );
        assertDowngrade(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED,
                "Completion path unwired",
                "The read-only response is present, but completion wiring is not active."
        );
    }

    @Test
    void completeSafeLookingDisplayOutputStillSerializesUnwiredReviewOnly() {
        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(completeDisplay());

        assertBaseApiSafety(response);
        assertThat(response.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(response.getTimeframe()).isEqualTo("15m");
        assertThat(response.getCompletionStatus()).isEqualTo("INCOMPLETE");
        assertThat(response.getCompletionTransition()).isEqualTo("NONE");
        assertThat(response.getDowngradeReason()).isEqualTo("COMPLETION_UNWIRED");
        assertThat(response.isReadOnlyIntegrationSeamUnwired()).isTrue();
        assertThat(response.getMissingFields()).contains(
                "entryOwnershipValidationResult",
                "readOnlyCompletionProductionPathUnwired",
                "entryPriceSource",
                "readOnlyIntegrationSeamUnwired"
        );
        assertThat(response.getBlockingFields()).contains(
                "entryOwnershipValidationResult",
                "readOnlyIntegrationSeamUnwired"
        );
    }

    @Test
    void apiResponseDefensivelyCopiesBlockingEvidence() {
        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(completeDisplay());

        List<String> missingFields = response.getMissingFields();
        List<String> unsafeFields = response.getUnsafeFields();
        List<String> blockingFields = response.getBlockingFields();
        missingFields.add("external-mutation");
        unsafeFields.add("external-mutation");
        blockingFields.add("external-mutation");

        assertThat(response.getMissingFields()).doesNotContain("external-mutation");
        assertThat(response.getUnsafeFields()).doesNotContain("external-mutation");
        assertThat(response.getBlockingFields()).doesNotContain("external-mutation");
    }

    @Test
    void apiResponseExposesNoForbiddenSurfacesOrGeneratedValues() {
        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(completeDisplay());

        assertNoForbiddenPositiveLabels(response);
        assertNoForbiddenMethodNames(SourceTraceEntryReadOnlyApiResponseDTO.class);
        assertNoForbiddenMethodNames(SourceTraceEntryReadOnlyApiResponseMapper.class);
        assertNoForbiddenFieldNames(SourceTraceEntryReadOnlyApiResponseDTO.class);
        assertNoForbiddenFieldNames(SourceTraceEntryReadOnlyApiResponseMapper.class);

        assertThat(Arrays.stream(SourceTraceEntryReadOnlyApiResponseDTO.class.getDeclaredMethods())
                        .map(Method::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("entryprice");
                    assertThat(name).doesNotContain("stopprice");
                    assertThat(name).doesNotContain("takeprofit");
                    assertThat(name).doesNotContain("riskreward");
                });
        assertThat(Arrays.stream(SourceTraceEntryReadOnlyApiResponseDTO.class.getDeclaredFields())
                        .map(Field::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("entryprice");
                    assertThat(name).doesNotContain("stopprice");
                    assertThat(name).doesNotContain("takeprofit");
                    assertThat(name).doesNotContain("riskreward");
                });
    }

    @Test
    void apiDtoAndMapperHaveNoSpringOrProductionBoundaryShape() {
        assertNoSpringAnnotations(SourceTraceEntryReadOnlyApiResponseDTO.class);
        assertNoSpringAnnotations(SourceTraceEntryReadOnlyApiResponseMapper.class);
        assertThat(SourceTraceEntryCompletionContract.class
                .isAssignableFrom(SourceTraceEntryReadOnlyApiResponseMapper.class)).isFalse();
        assertThat(SourceTraceEntryOwnershipAdapter.class
                .isAssignableFrom(SourceTraceEntryReadOnlyApiResponseMapper.class)).isFalse();
        assertThat(SourceTraceEntryOwnershipValidator.class
                .isAssignableFrom(SourceTraceEntryReadOnlyApiResponseMapper.class)).isFalse();
        assertThat(EntryCompletionValidationContextAssembler.class
                .isAssignableFrom(SourceTraceEntryReadOnlyApiResponseMapper.class)).isFalse();
    }

    @Test
    void controllerEndpointProductionAdapterAndProductionCompletionContractRemainAbsent() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.controller.SourceTraceEntryReadOnlyApiController"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private void assertDisplaySafetyGap(
            String expectedMissingField,
            DisplayMutation displayMutation
    ) {
        SourceTraceEntryReadOnlyDisplayDTO display = completeDisplay();
        displayMutation.apply(display);

        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(display);

        assertBaseApiSafety(response);
        assertThat(response.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
        assertThat(response.getMissingFields()).contains(expectedMissingField);
    }

    private void assertDowngrade(
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason,
            String expectedLabel,
            String expectedHelperCopy
    ) {
        SourceTraceEntryReadOnlyDisplayDTO display = downgradeReason == SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION
                ? displayWith(
                downgradeReason,
                List.of("BOUNDARYCANDIDATESERVICE_VALID", "readOnlyIntegrationSeamUnwired"),
                List.of("BOUNDARYCANDIDATESERVICE_VALID"),
                List.of("BOUNDARYCANDIDATESERVICE_VALID", "readOnlyIntegrationSeamUnwired")
        )
                : displayWith(
                downgradeReason,
                List.of("readOnlyIntegrationSeamUnwired"),
                List.of(),
                List.of("readOnlyIntegrationSeamUnwired")
        );

        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(display);

        assertBaseApiSafety(response);
        assertThat(response.getDowngradeReason()).isEqualTo(downgradeReason.name());
        assertThat(response.getDowngradeLabel()).isEqualTo(expectedLabel);
        assertThat(response.getHelperCopy()).isEqualTo(expectedHelperCopy);
    }

    private void assertUnsafeValueSerializesOnlyAsBlocker(String unsafeValue) {
        SourceTraceEntryReadOnlyDisplayDTO display = displayWith(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION,
                List.of(unsafeValue, "readOnlyIntegrationSeamUnwired"),
                List.of(unsafeValue),
                List.of(unsafeValue, "readOnlyIntegrationSeamUnwired")
        );

        SourceTraceEntryReadOnlyApiResponseDTO response = mapper.map(display);

        assertBaseApiSafety(response);
        assertThat(response.getDowngradeReason()).isEqualTo("UNSAFE_COMPLETION");
        assertThat(response.getUnsafeFields()).containsExactly(unsafeValue);
        assertThat(response.getBlockingFields()).contains(unsafeValue);
        assertThat(response.getMissingFields()).contains(unsafeValue);
        assertNoForbiddenPositiveLabels(response);
    }

    private SourceTraceEntryReadOnlyDisplayDTO completeDisplay() {
        return displayMapper.map(seam.combine(failClosedContext(), completeRequest()));
    }

    private SourceTraceEntryReadOnlyDisplayDTO displayWith(
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason,
            List<String> missingFields,
            List<String> unsafeFields,
            List<String> blockingFields
    ) {
        SourceTraceEntryReadOnlyDisplayDTO display = new SourceTraceEntryReadOnlyDisplayDTO();
        display.setCompletionStatus("INCOMPLETE");
        display.setCompletionTransition("NONE");
        display.setDowngradeReason(downgradeReason.name());
        display.setReviewMode("REVIEW_ONLY");
        display.setReadOnlyIntegrationSeamUnwired(missingFields.contains("readOnlyIntegrationSeamUnwired"));
        display.setManualReviewRequired(true);
        display.setNotTradeInstruction(true);
        display.setSourceTraceEntryCompleted(false);
        display.setCompletionReady(false);
        display.setMissingFields(missingFields);
        display.setUnsafeFields(unsafeFields);
        display.setBlockingFields(blockingFields);
        return display;
    }

    private EntryOwnershipValidationCompletionContext failClosedContext() {
        return EntryOwnershipValidationCompletionContext.from(
                EntryOwnershipValidationResult.missingSource(
                        "BTCUSDT",
                        "15m",
                        List.of("entryOwnershipValidationResult")
                ),
                SourceTraceEntryCompletionResult.unwired("BTCUSDT", "15m")
        );
    }

    private SourceTraceEntryReadOnlyCompletionRequest completeRequest() {
        return SourceTraceEntryReadOnlyCompletionRequest.builder()
                .symbol("BTCUSDT")
                .timeframe("15m")
                .sourceTraceEntryOwnershipCompletionPath("read-only-completion-path")
                .entrySourceType("rule-owned-boundary")
                .entrySourceTimeframe("15m")
                .entrySourceReason("read-only-source-reason")
                .entrySourceRef("read-only-source-ref")
                .ruleId("read-only-rule")
                .ruleVersion("read-only-v1")
                .sourceWindow("read-only-window")
                .freshnessStatus("FRESH")
                .observedAtMs(100L)
                .decisionCreateTimeMs(200L)
                .conflictsWithStop(Boolean.FALSE)
                .conflictsWithTakeProfit(Boolean.FALSE)
                .conflictsWithRiskReward(Boolean.FALSE)
                .conflictsWithLiquidity(Boolean.FALSE)
                .conflictsWithMultiTimeframe(Boolean.FALSE)
                .conflictsWithEvent(Boolean.FALSE)
                .conflictsWithWick(Boolean.FALSE)
                .sourceRefs(List.of("read-only-source-ref"))
                .sourceTags(List.of("READ_ONLY_INTERNAL"))
                .build();
    }

    private void assertBaseApiSafety(SourceTraceEntryReadOnlyApiResponseDTO response) {
        assertThat(response.getReviewMode()).isEqualTo("REVIEW_ONLY");
        assertThat(response.isManualReviewRequired()).isTrue();
        assertThat(response.isNotTradeInstruction()).isTrue();
        assertThat(response.isSourceTraceEntryCompleted()).isFalse();
        assertThat(response.isCompletionReady()).isFalse();
        assertThat(response.getSeverity()).isEqualTo("blocking_review");
        assertThat(response.getReadinessEffect()).isEqualTo("blocks_completion_ready");
        assertThat(response.getSourceTraceEffect()).isEqualTo("source_trace_entry_completed_false");
        assertThat(response.getInstructionEffect()).isEqualTo("not_trade_instruction");
    }

    private void assertNoForbiddenPositiveLabels(SourceTraceEntryReadOnlyApiResponseDTO response) {
        List<String> labels = List.of(
                response.getStatusLabel(),
                response.getTransitionLabel(),
                response.getDowngradeLabel(),
                response.getReviewModeLabel(),
                response.getManualReviewLabel(),
                response.getNonInstructionLabel(),
                response.getSourceTraceLabel(),
                response.getReadinessLabel(),
                response.getSeamLabel(),
                response.getBlockerLabel()
        );

        assertThat(labels).allSatisfy(label -> {
            assertThat(label).doesNotContain("Ready");
            assertThat(label).doesNotContain("Valid");
            assertThat(label).doesNotContain("Completed");
            assertThat(label).doesNotContain("Signal");
            assertThat(label).doesNotContain("Buy");
            assertThat(label).doesNotContain("Sell");
            assertThat(label).doesNotContain("Open");
            assertThat(label).doesNotContain("Close");
            assertThat(label).doesNotContain("Reverse");
        });
    }

    private void assertNoForbiddenMethodNames(Class<?> type) {
        assertThat(Arrays.stream(type.getDeclaredMethods())
                        .map(Method::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("order");
                    assertThat(name).doesNotContain("execution");
                    assertThat(name).doesNotContain("execute");
                    assertThat(name).doesNotContain("close");
                    assertThat(name).doesNotContain("reverse");
                    assertThat(name).doesNotContain("autotrading");
                    assertThat(name).doesNotContain("auto");
                    assertThat(name).doesNotContain("tradeready");
                    assertThat(name).doesNotContain("readytotrade");
                    assertThat(name).doesNotContain("entryready");
                    assertThat(name).doesNotContain("executionready");
                });
    }

    private void assertNoForbiddenFieldNames(Class<?> type) {
        assertThat(Arrays.stream(type.getDeclaredFields())
                        .map(Field::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("tradeready");
                    assertThat(name).doesNotContain("readytotrade");
                    assertThat(name).doesNotContain("entryready");
                    assertThat(name).doesNotContain("executionready");
                    assertThat(name).doesNotContain("valid");
                    assertThat(name).doesNotContain("signal");
                    assertThat(name).doesNotContain("buy");
                    assertThat(name).doesNotContain("sell");
                    assertThat(name).doesNotContain("open");
                    assertThat(name).doesNotContain("close");
                    assertThat(name).doesNotContain("reverse");
                });
    }

    private void assertNoSpringAnnotations(Class<?> type) {
        assertThat(type.getAnnotation(Controller.class)).isNull();
        assertThat(type.getAnnotation(Service.class)).isNull();
        assertThat(type.getAnnotation(Component.class)).isNull();
        assertThat(type.getAnnotation(Repository.class)).isNull();
    }

    private interface DisplayMutation {
        void apply(SourceTraceEntryReadOnlyDisplayDTO display);
    }
}
