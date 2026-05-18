package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationCompletionContext;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationMissingReasonEnum;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.junit.jupiter.api.Test;

class EntryCompletionValidationContextAssemblerTest {

    private final EntryCompletionValidationContextAssembler assembler =
            new EntryCompletionValidationContextAssembler();

    @Test
    void assemblerPresenceAloneDoesNotCompleteSourceTraceOrBecomeReady() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource(
                        "BTCUSDT",
                        "15m",
                        List.of("sourceTraceEntryOwnershipCompletionPath")
                );
        SourceTraceEntryCompletionResult completionResult =
                SourceTraceEntryCompletionResult.unwired("BTCUSDT", "15m");

        EntryOwnershipValidationCompletionContext context =
                assembler.assemble(validationResult, completionResult);

        assertFailClosedValidation(context.getValidationResult());
        assertFailClosedContext(context);
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
    }

    @Test
    void nullValidationResultFailsClosed() {
        EntryOwnershipValidationCompletionContext context =
                assembler.assemble(null, SourceTraceEntryCompletionResult.unwired("BTCUSDT", "15m"));

        assertFailClosedValidation(context.getValidationResult());
        assertFailClosedContext(context);
        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("entryOwnershipValidationResult");
    }

    @Test
    void nullCompletionResultFailsClosed() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource("BTCUSDT", "15m", List.of("validation"));

        EntryOwnershipValidationCompletionContext context =
                assembler.assemble(validationResult, null);

        assertFailClosedContext(context);
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
        assertThat(context.getCompletionResult().getMissingFields())
                .containsExactly("sourceTraceEntryCompletionResult");
    }

    @Test
    void incompleteValidationResultFailsClosed() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource("BTCUSDT", "15m");

        EntryOwnershipValidationCompletionContext context =
                assembler.assemble(validationResult, SourceTraceEntryCompletionResult.unwired("BTCUSDT", "15m"));

        assertFailClosedValidation(context.getValidationResult());
        assertFailClosedContext(context);
        assertThat(context.getMissingFields()).contains(
                "entryOwnershipRequest",
                "runtimeKlineContext",
                "ruleOwnedEntryCandidate",
                "freshness",
                "conflict",
                "sourceTraceEntryCompletionPath"
        );
    }

    @Test
    void unwiredCompletionPathFailsClosed() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource(
                        "BTCUSDT",
                        "15m",
                        List.of("sourceTraceEntryOwnershipCompletionPath")
                );
        SourceTraceEntryCompletionResult completionResult =
                SourceTraceEntryCompletionResult.incomplete(
                        "BTCUSDT",
                        "15m",
                        SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED,
                        List.of("sourceTraceEntryOwnershipCompletionPath")
                );

        EntryOwnershipValidationCompletionContext context =
                assembler.assemble(validationResult, completionResult);

        assertFailClosedContext(context);
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
        assertThat(context.getMissingFields()).contains(
                "sourceTraceEntryOwnershipCompletionPath",
                "sourceTraceEntryOwnershipCompletionPath"
        );
    }

    @Test
    void mixedMissingFieldsRemainFailClosed() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource(
                        "BTCUSDT",
                        "15m",
                        List.of("ruleOwnedEntryCandidate.entrySourceType", "sourceTraceEntryOwnershipCompletionPath")
                );
        SourceTraceEntryCompletionResult completionResult =
                SourceTraceEntryCompletionResult.incomplete(
                        "BTCUSDT",
                        "15m",
                        SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED,
                        List.of("freshness.observedAtMs", "sourceTraceEntryCompletionPath")
                );

        EntryOwnershipValidationCompletionContext context =
                assembler.assemble(validationResult, completionResult);

        assertFailClosedContext(context);
        assertThat(context.getMissingFields()).contains(
                "ruleOwnedEntryCandidate.entrySourceType",
                "sourceTraceEntryOwnershipCompletionPath",
                "freshness.observedAtMs"
        );
    }

    @Test
    void assemblerSurfaceIsNotSpringServiceAndHasNoTradingMethodNames() {
        assertThat(Arrays.stream(EntryCompletionValidationContextAssembler.class.getAnnotations())
                        .map(annotation -> annotation.annotationType().getSimpleName()))
                .doesNotContain("Service", "Component");
        assertThat(EntryCompletionValidationContextAssembler.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactly("assemble");
        assertNoForbiddenMethodNames(EntryCompletionValidationContextAssembler.class);
    }

    private void assertFailClosedValidation(EntryOwnershipValidationResult validationResult) {
        assertThat(validationResult.getValidationStatus()).isEqualTo(EntryOwnershipValidationStatusEnum.INCOMPLETE);
        assertThat(validationResult.getMissingReason()).isEqualTo(EntryOwnershipValidationMissingReasonEnum.MISSING_SOURCE);
        assertThat(validationResult.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(validationResult.isManualReviewRequired()).isTrue();
        assertThat(validationResult.isNotTradeInstruction()).isTrue();
    }

    private void assertFailClosedContext(EntryOwnershipValidationCompletionContext context) {
        assertThat(context.isCompletionReady()).isFalse();
        assertThat(context.getCompletionResult().getCompletionStatus())
                .isEqualTo(SourceTraceEntryCompletionStatusEnum.INCOMPLETE);
        assertThat(context.getCompletionResult().getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.getCompletionResult().isSourceTraceEntryCompleted()).isFalse();
        assertThat(context.getCompletionResult().isCompletionReady()).isFalse();
        assertThat(context.getCompletionResult().isManualReviewRequired()).isTrue();
        assertThat(context.getCompletionResult().isNotTradeInstruction()).isTrue();
        assertThat(context.getCompletionResult().getEntryPriceSource()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceType()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceTimeframe()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceReason()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceRef()).isNull();
        assertThat(context.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.isManualReviewRequired()).isTrue();
        assertThat(context.isNotTradeInstruction()).isTrue();
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
