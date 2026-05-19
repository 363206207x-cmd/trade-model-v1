package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationCompletionContext;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionContractDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionTransitionEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyCompletionRequest;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyDisplayDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

class SourceTraceEntryReadOnlyDisplayMapperTest {

    private final SourceTraceEntryReadOnlyDisplayMapper mapper =
            new SourceTraceEntryReadOnlyDisplayMapper();
    private final SourceTraceEntryReadOnlyIntegrationSeam seam =
            new SourceTraceEntryReadOnlyIntegrationSeam();

    @Test
    void nullSeamOutputFailsClosed() {
        SourceTraceEntryReadOnlyDisplayDTO display = mapper.map(null);

        assertBaseDisplaySafety(display);
        assertThat(display.getCompletionStatus()).isEqualTo("INCOMPLETE");
        assertThat(display.getCompletionTransition()).isEqualTo("NONE");
        assertThat(display.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
        assertThat(display.isReadOnlyIntegrationSeamUnwired()).isFalse();
        assertThat(display.getMissingFields()).containsExactly(
                "sourceTraceEntryReadOnlyIntegrationSeamOutput",
                "readOnlyIntegrationSeamUnwired"
        );
        assertThat(display.getBlockingFields()).containsExactly(
                "sourceTraceEntryReadOnlyIntegrationSeamOutput",
                "readOnlyIntegrationSeamUnwired"
        );
    }

    @Test
    void missingReadOnlyIntegrationSeamUnwiredFailsClosed() {
        SourceTraceEntryReadOnlyDisplayDTO display = mapper.map(seamOutput(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED,
                List.of("entryPriceSource")
        ));

        assertBaseDisplaySafety(display);
        assertThat(display.isReadOnlyIntegrationSeamUnwired()).isFalse();
        assertThat(display.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
        assertThat(display.getMissingFields()).containsExactly(
                "entryPriceSource",
                "readOnlyIntegrationSeamUnwired"
        );
    }

    @Test
    void missingReviewOnlyFlagFailsClosed() {
        SourceTraceEntryPositiveCompletionContractDTO seamOutput =
                seamOutput(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED);
        SourceTraceEntryPositiveCompletionContractDTO unsafe = new SourceTraceEntryPositiveCompletionContractDTO() {
            @Override
            public SourceTraceEntrySourceReviewModeEnum getReviewMode() {
                return null;
            }

            @Override
            public List<String> getMissingFields() {
                return seamOutput.getMissingFields();
            }

            @Override
            public SourceTraceEntryPositiveCompletionDowngradeReasonEnum getDowngradeReason() {
                return seamOutput.getDowngradeReason();
            }
        };

        SourceTraceEntryReadOnlyDisplayDTO display = mapper.map(unsafe);

        assertBaseDisplaySafety(display);
        assertThat(display.getMissingFields()).contains("reviewMode");
        assertThat(display.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
    }

    @Test
    void unsafeSafetyFlagsFailClosedIndependently() {
        assertUnsafeFlag("manualReviewRequired", new SourceTraceEntryPositiveCompletionContractDTO() {
            @Override
            public boolean isManualReviewRequired() {
                return false;
            }
        });
        assertUnsafeFlag("notTradeInstruction", new SourceTraceEntryPositiveCompletionContractDTO() {
            @Override
            public boolean isNotTradeInstruction() {
                return false;
            }
        });
        assertUnsafeFlag("sourceTraceEntryCompleted", new SourceTraceEntryPositiveCompletionContractDTO() {
            @Override
            public boolean isSourceTraceEntryCompleted() {
                return true;
            }
        });
        assertUnsafeFlag("completionReady", new SourceTraceEntryPositiveCompletionContractDTO() {
            @Override
            public boolean isCompletionReady() {
                return true;
            }
        });
    }

    @Test
    void missingOrEmptyMissingFieldListDoesNotImplyCompletion() {
        SourceTraceEntryReadOnlyDisplayDTO display = mapper.map(new SourceTraceEntryPositiveCompletionContractDTO() {
            @Override
            public List<String> getMissingFields() {
                return List.of();
            }
        });

        assertBaseDisplaySafety(display);
        assertThat(display.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
        assertThat(display.getMissingFields()).containsExactly(
                "missingFields",
                "readOnlyIntegrationSeamUnwired"
        );
    }

    @Test
    void unsafeFieldsRemainBlockingReviewEvidence() {
        SourceTraceEntryReadOnlyDisplayDTO display = mapper.map(seamOutput(
                SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION,
                List.of("BOUNDARYCANDIDATESERVICE_VALID", "readOnlyIntegrationSeamUnwired")
        ));

        assertBaseDisplaySafety(display);
        assertThat(display.getDowngradeReason()).isEqualTo("UNSAFE_COMPLETION");
        assertThat(display.getDowngradeLabel()).isEqualTo("Unsafe completion evidence");
        assertThat(display.getUnsafeFields()).containsExactly("BOUNDARYCANDIDATESERVICE_VALID");
        assertThat(display.getBlockingFields()).contains(
                "BOUNDARYCANDIDATESERVICE_VALID",
                "readOnlyIntegrationSeamUnwired"
        );
    }

    @Test
    void runtimeLikeAndProductionLikeTagsRemainUnsafe() {
        String[] unsafeFields = {
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
                "TRADE_READY"
        };

        for (String unsafeField : unsafeFields) {
            SourceTraceEntryReadOnlyDisplayDTO display = mapper.map(seamOutput(
                    SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION,
                    List.of(unsafeField, "readOnlyIntegrationSeamUnwired")
            ));

            assertBaseDisplaySafety(display);
            assertThat(display.getDowngradeReason()).isEqualTo("UNSAFE_COMPLETION");
            assertThat(display.getUnsafeFields()).containsExactly(unsafeField);
        }
    }

    @Test
    void completeSafeLookingSeamOutputStillDisplaysUnwiredReviewOnly() {
        SourceTraceEntryReadOnlyDisplayDTO display =
                mapper.map(seam.combine(failClosedContext(), completeRequest()));

        assertBaseDisplaySafety(display);
        assertThat(display.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(display.getTimeframe()).isEqualTo("15m");
        assertThat(display.getCompletionStatus()).isEqualTo("INCOMPLETE");
        assertThat(display.getCompletionTransition()).isEqualTo("NONE");
        assertThat(display.getDowngradeReason()).isEqualTo("COMPLETION_UNWIRED");
        assertThat(display.isReadOnlyIntegrationSeamUnwired()).isTrue();
        assertThat(display.getDowngradeLabel()).isEqualTo("Completion path unwired");
        assertThat(display.getDowngradeHelperCopy())
                .isEqualTo("The read-only seam is present, but completion wiring is not active.");
        assertThat(display.getMissingFields()).contains(
                "entryOwnershipValidationResult",
                "readOnlyCompletionProductionPathUnwired",
                "entryPriceSource",
                "readOnlyIntegrationSeamUnwired"
        );
    }

    @Test
    void requiredLabelsAndHelperCopyComeFromDisplayDesign() {
        SourceTraceEntryReadOnlyDisplayDTO display =
                mapper.map(seam.combine(failClosedContext(), completeRequest()));

        assertThat(display.getStatusLabel()).isEqualTo("Incomplete - review only");
        assertThat(display.getStatusHelperCopy())
                .isEqualTo("SourceTrace entry completion is not complete.");
        assertThat(display.getTransitionLabel()).isEqualTo("No completion transition");
        assertThat(display.getTransitionHelperCopy()).isEqualTo("No completion transition has occurred.");
        assertThat(display.getReviewModeLabel()).isEqualTo("Review only");
        assertThat(display.getManualReviewLabel()).isEqualTo("Manual review required");
        assertThat(display.getNonInstructionLabel()).isEqualTo("Not a trade instruction");
        assertThat(display.getSourceTraceLabel()).isEqualTo("SourceTrace entry not completed");
        assertThat(display.getReadinessLabel()).isEqualTo("Completion not ready");
        assertThat(display.getSeamLabel()).isEqualTo("Read-only seam unwired");
        assertThat(display.getSeverity()).isEqualTo("blocking_review");
        assertThat(display.getReadinessEffect()).isEqualTo("blocks_completion_ready");
        assertThat(display.getSourceTraceEffect()).isEqualTo("source_trace_entry_completed_false");
        assertThat(display.getInstructionEffect()).isEqualTo("not_trade_instruction");
    }

    @Test
    void displayDtoDefensivelyCopiesBlockingEvidence() {
        SourceTraceEntryReadOnlyDisplayDTO display =
                mapper.map(seamOutput(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED));

        List<String> missingFields = display.getMissingFields();
        List<String> blockingFields = display.getBlockingFields();
        missingFields.add("external-mutation");
        blockingFields.add("external-mutation");

        assertThat(display.getMissingFields()).doesNotContain("external-mutation");
        assertThat(display.getBlockingFields()).doesNotContain("external-mutation");
    }

    @Test
    void forbiddenDisplayLabelsAndGeneratedValueSurfacesAreAbsent() {
        SourceTraceEntryReadOnlyDisplayDTO display =
                mapper.map(seam.combine(failClosedContext(), completeRequest()));

        List<String> labels = List.of(
                display.getStatusLabel(),
                display.getTransitionLabel(),
                display.getDowngradeLabel(),
                display.getReviewModeLabel(),
                display.getManualReviewLabel(),
                display.getNonInstructionLabel(),
                display.getSourceTraceLabel(),
                display.getReadinessLabel(),
                display.getSeamLabel(),
                display.getMissingFieldsLabel(),
                display.getUnsafeFieldsLabel()
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
            assertThat(label).doesNotContain("Auto");
        });

        assertThat(Arrays.stream(SourceTraceEntryReadOnlyDisplayDTO.class.getDeclaredMethods())
                        .map(Method::getName)
                        .map(name -> name.toLowerCase(Locale.ROOT)))
                .allSatisfy(name -> {
                    assertThat(name).doesNotContain("entryprice");
                    assertThat(name).doesNotContain("stopprice");
                    assertThat(name).doesNotContain("takeprofit");
                    assertThat(name).doesNotContain("riskreward");
                });
    }

    @Test
    void displayDtoAndMapperExposeNoTradingOrSpringWiringSurface() {
        assertNoForbiddenMethodNames(SourceTraceEntryReadOnlyDisplayDTO.class);
        assertNoForbiddenMethodNames(SourceTraceEntryReadOnlyDisplayMapper.class);
        assertNoSpringAnnotations(SourceTraceEntryReadOnlyDisplayDTO.class);
        assertNoSpringAnnotations(SourceTraceEntryReadOnlyDisplayMapper.class);
        assertThat(SourceTraceEntryCompletionContract.class
                .isAssignableFrom(SourceTraceEntryReadOnlyDisplayMapper.class)).isFalse();
        assertThat(SourceTraceEntryOwnershipAdapter.class
                .isAssignableFrom(SourceTraceEntryReadOnlyDisplayMapper.class)).isFalse();
        assertThat(SourceTraceEntryOwnershipValidator.class
                .isAssignableFrom(SourceTraceEntryReadOnlyDisplayMapper.class)).isFalse();
        assertThat(EntryCompletionValidationContextAssembler.class
                .isAssignableFrom(SourceTraceEntryReadOnlyDisplayMapper.class)).isFalse();
    }

    @Test
    void productionAdapterAndProductionCompletionContractRemainAbsent() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private void assertUnsafeFlag(String expectedMissingField, SourceTraceEntryPositiveCompletionContractDTO unsafe) {
        unsafe.setDowngradeReason(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED);
        unsafe.setMissingFields(List.of("readOnlyIntegrationSeamUnwired"));

        SourceTraceEntryReadOnlyDisplayDTO display = mapper.map(unsafe);

        assertBaseDisplaySafety(display);
        assertThat(display.getMissingFields()).contains(expectedMissingField);
        assertThat(display.getDowngradeReason()).isEqualTo("MISSING_REQUIRED_FIELD");
    }

    private SourceTraceEntryPositiveCompletionContractDTO seamOutput(
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason
    ) {
        return seamOutput(downgradeReason, List.of("readOnlyIntegrationSeamUnwired"));
    }

    private SourceTraceEntryPositiveCompletionContractDTO seamOutput(
            SourceTraceEntryPositiveCompletionDowngradeReasonEnum downgradeReason,
            List<String> missingFields
    ) {
        SourceTraceEntryPositiveCompletionContractDTO dto = new SourceTraceEntryPositiveCompletionContractDTO();
        dto.setCompletionStatus(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        dto.setCompletionTransition(SourceTraceEntryPositiveCompletionTransitionEnum.NONE);
        dto.setDowngradeReason(downgradeReason);
        dto.setMissingFields(missingFields);
        return dto;
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

    private void assertBaseDisplaySafety(SourceTraceEntryReadOnlyDisplayDTO display) {
        assertThat(display.getReviewMode()).isEqualTo("REVIEW_ONLY");
        assertThat(display.isManualReviewRequired()).isTrue();
        assertThat(display.isNotTradeInstruction()).isTrue();
        assertThat(display.isSourceTraceEntryCompleted()).isFalse();
        assertThat(display.isCompletionReady()).isFalse();
        assertThat(display.getSeverity()).isEqualTo("blocking_review");
        assertThat(display.getReadinessEffect()).isEqualTo("blocks_completion_ready");
        assertThat(display.getSourceTraceEffect()).isEqualTo("source_trace_entry_completed_false");
        assertThat(display.getInstructionEffect()).isEqualTo("not_trade_instruction");
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

    private void assertNoSpringAnnotations(Class<?> type) {
        assertThat(type.getAnnotation(Controller.class)).isNull();
        assertThat(type.getAnnotation(Service.class)).isNull();
        assertThat(type.getAnnotation(Component.class)).isNull();
        assertThat(type.getAnnotation(Repository.class)).isNull();
    }
}
