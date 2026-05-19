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
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.junit.jupiter.api.Test;

class SourceTraceEntryReadOnlyIntegrationSeamTest {

    private final SourceTraceEntryReadOnlyIntegrationSeam seam =
            new SourceTraceEntryReadOnlyIntegrationSeam();

    @Test
    void seamPresenceAloneFailsClosed() {
        SourceTraceEntryPositiveCompletionContractDTO dto = seam.combine(null, null);

        assertThat(dto.getCompletionStatus()).isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getCompletionTransition()).isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.NONE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
        assertThat(dto.getMissingFields()).containsExactly(
                "entryOwnershipValidationCompletionContext",
                "sourceTraceEntryReadOnlyCompletionRequest"
        );
        assertStillNonProduction(dto);
    }

    @Test
    void seamRequiresAlreadyBuiltContextAndReadOnlyAssemblerInput() {
        SourceTraceEntryPositiveCompletionContractDTO missingContext =
                seam.combine(null, completeRequest());
        SourceTraceEntryPositiveCompletionContractDTO missingReadOnlyInput =
                seam.combine(failClosedContext(), null);

        assertMissing(missingContext, "entryOwnershipValidationCompletionContext");
        assertMissing(missingReadOnlyInput, "sourceTraceEntryReadOnlyCompletionRequest");
    }

    @Test
    void completeReadOnlyInputWithFailClosedContextRemainsUnwiredAndIncomplete() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                seam.combine(failClosedContext(), completeRequest());

        assertThat(dto.getCompletionStatus()).isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getCompletionTransition()).isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.NONE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED);
        assertThat(dto.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(dto.getTimeframe()).isEqualTo("15m");
        assertThat(dto.getSourceTraceEntryOwnershipCompletionPath())
                .isEqualTo("read-only-completion-path");
        assertThat(dto.getEntryPriceSource()).isNull();
        assertThat(dto.getMissingFields()).contains(
                "entryOwnershipValidationResult",
                "sourceTraceEntryCompletionPath",
                "readOnlyCompletionProductionPathUnwired",
                "entryPriceSource",
                "readOnlyIntegrationSeamUnwired"
        );
        assertStillNonProduction(dto);
    }

    @Test
    void missingReadOnlyAssemblerInputRemainsMissingFailClosed() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                seam.combine(failClosedContext(), SourceTraceEntryReadOnlyCompletionRequest.builder().build());

        assertThat(dto.getCompletionStatus()).isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
        assertThat(dto.getMissingFields()).contains(
                "entryOwnershipValidationResult",
                "sourceTraceEntryOwnershipCompletionPath",
                "entrySourceType",
                "readOnlyIntegrationSeamUnwired"
        );
        assertStillNonProduction(dto);
    }

    @Test
    void runtimeLikeReadOnlyAssemblerInputRemainsUnsafeFailClosed() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                seam.combine(
                        failClosedContext(),
                        completeRequestBuilder().sourceTags(List.of("BoundaryCandidateService VALID")).build()
                );

        assertThat(dto.getCompletionStatus()).isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION);
        assertThat(dto.getMissingFields()).contains(
                "BOUNDARYCANDIDATESERVICE_VALID",
                "readOnlyIntegrationSeamUnwired"
        );
        assertStillNonProduction(dto);
    }

    @Test
    void seamDoesNotImplyRuntimeCompletionBoundaryCandidateValidReadinessOrTradeInstruction() {
        SourceTraceEntryPositiveCompletionContractDTO dto =
                seam.combine(failClosedContext(), completeRequest());

        assertStillNonProduction(dto);
        assertThat(dto.getMissingFields()).contains("readOnlyIntegrationSeamUnwired");
        assertThat(dto.isSourceTraceEntryCompleted()).isFalse();
        assertThat(dto.isCompletionReady()).isFalse();
        assertThat(dto.isNotTradeInstruction()).isTrue();
    }

    @Test
    void seamSurfaceHasNoTradingOrProductionWiringShape() {
        assertNoForbiddenMethodNames(SourceTraceEntryReadOnlyIntegrationSeam.class);
        assertThat(SourceTraceEntryReadOnlyIntegrationSeam.class.getAnnotation(Service.class)).isNull();
        assertThat(SourceTraceEntryReadOnlyIntegrationSeam.class.getAnnotation(Component.class)).isNull();
        assertThat(SourceTraceEntryCompletionContract.class
                .isAssignableFrom(SourceTraceEntryReadOnlyIntegrationSeam.class)).isFalse();
        assertThat(SourceTraceEntryOwnershipAdapter.class
                .isAssignableFrom(SourceTraceEntryReadOnlyIntegrationSeam.class)).isFalse();
        assertThat(SourceTraceEntryOwnershipValidator.class
                .isAssignableFrom(SourceTraceEntryReadOnlyIntegrationSeam.class)).isFalse();
        assertThat(EntryCompletionValidationContextAssembler.class
                .isAssignableFrom(SourceTraceEntryReadOnlyIntegrationSeam.class)).isFalse();
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
        return completeRequestBuilder().build();
    }

    private SourceTraceEntryReadOnlyCompletionRequest.Builder completeRequestBuilder() {
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
                .sourceTags(List.of("READ_ONLY_INTERNAL"));
    }

    private void assertMissing(SourceTraceEntryPositiveCompletionContractDTO dto, String expectedMissingField) {
        assertThat(dto.getCompletionStatus()).isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getCompletionTransition()).isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.NONE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
        assertThat(dto.getMissingFields()).containsExactly(expectedMissingField);
        assertStillNonProduction(dto);
    }

    private void assertStillNonProduction(SourceTraceEntryPositiveCompletionContractDTO dto) {
        assertThat(dto.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(dto.isManualReviewRequired()).isTrue();
        assertThat(dto.isNotTradeInstruction()).isTrue();
        assertThat(dto.isSourceTraceEntryCompleted()).isFalse();
        assertThat(dto.isCompletionReady()).isFalse();
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
                });
    }
}
