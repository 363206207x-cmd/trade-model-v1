package org.example.trademodel.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.example.trademodel.service.SourceTraceEntryCompletionContract;
import org.junit.jupiter.api.Test;

class FailClosedSourceTraceEntryCompletionResolverTest {

    private final SourceTraceEntryCompletionContract resolver =
            new FailClosedSourceTraceEntryCompletionResolver();

    @Test
    void resolverPresenceAloneDoesNotCompleteSourceTraceOrBecomeReady() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource(
                        "BTCUSDT",
                        "15m",
                        List.of("sourceTraceEntryOwnershipCompletionPath")
                );

        SourceTraceEntryCompletionResult result = resolver.resolveEntryCompletion(validationResult);

        assertFailClosedCompletion(result);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
        assertThat(result.getMissingFields()).containsExactly("sourceTraceEntryOwnershipCompletionPath");
    }

    @Test
    void nullValidationResultFailsClosedAsUnsafeCompletion() {
        SourceTraceEntryCompletionResult result = resolver.resolveEntryCompletion(null);

        assertFailClosedCompletion(result);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.UNSAFE_COMPLETION);
        assertThat(result.getMissingFields()).containsExactly("entryOwnershipValidationResult");
    }

    @Test
    void missingValidationFieldsFailClosedAsMissingCompletion() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource(
                        "BTCUSDT",
                        "15m",
                        List.of("ruleOwnedEntryCandidate.entrySourceRef")
                );

        SourceTraceEntryCompletionResult result = resolver.resolveEntryCompletion(validationResult);

        assertFailClosedValidation(validationResult);
        assertFailClosedCompletion(result);
        assertThat(result.getMissingReason()).isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
        assertThat(result.getMissingFields()).containsExactly("ruleOwnedEntryCandidate.entrySourceRef");
    }

    @Test
    void incompleteValidationResultFailsClosedAndDoesNotBecomeTradeReady() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource("BTCUSDT", "15m");

        SourceTraceEntryCompletionResult result = resolver.resolveEntryCompletion(validationResult);

        assertFailClosedValidation(validationResult);
        assertFailClosedCompletion(result);
        assertThat(result.getEntryPriceSource()).isNull();
        assertThat(result.getEntrySourceType()).isNull();
        assertThat(result.getEntrySourceTimeframe()).isNull();
        assertThat(result.getEntrySourceReason()).isNull();
        assertThat(result.getEntrySourceRef()).isNull();
    }

    @Test
    void unwiredCompletionPathFailsClosedInValidatorCompletionContext() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource(
                        "BTCUSDT",
                        "15m",
                        List.of("sourceTraceEntryOwnershipCompletionPath")
                );
        SourceTraceEntryCompletionResult result = resolver.resolveEntryCompletion(validationResult);

        EntryOwnershipValidationCompletionContext context =
                EntryOwnershipValidationCompletionContext.from(validationResult, result);

        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
        assertThat(context.isCompletionReady()).isFalse();
        assertThat(context.getCompletionResult().isCompletionReady()).isFalse();
        assertThat(context.getCompletionResult().isSourceTraceEntryCompleted()).isFalse();
        assertThat(context.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.isManualReviewRequired()).isTrue();
        assertThat(context.isNotTradeInstruction()).isTrue();
    }

    @Test
    void missingCompletionStateStillFailsClosedWithoutResolverOutput() {
        EntryOwnershipValidationCompletionContext context =
                EntryOwnershipValidationCompletionContext.from(
                        EntryOwnershipValidationResult.missingSource("BTCUSDT", "15m", List.of("validation")),
                        null
                );

        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
        assertThat(context.isCompletionReady()).isFalse();
        assertThat(context.getCompletionResult().isCompletionReady()).isFalse();
        assertThat(context.getCompletionResult().isSourceTraceEntryCompleted()).isFalse();
        assertThat(context.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.isManualReviewRequired()).isTrue();
        assertThat(context.isNotTradeInstruction()).isTrue();
    }

    @Test
    void resolverSurfaceExposesNoOrderExecutionCloseReverseOrAutoTradingMethods() {
        assertNoForbiddenMethodNames(FailClosedSourceTraceEntryCompletionResolver.class);
    }

    @Test
    void resolverDoesNotIntroduceDefaultProductionCompletionOrOwnershipAdapter() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private void assertFailClosedValidation(EntryOwnershipValidationResult validationResult) {
        assertThat(validationResult.getValidationStatus()).isEqualTo(EntryOwnershipValidationStatusEnum.INCOMPLETE);
        assertThat(validationResult.getMissingReason()).isEqualTo(EntryOwnershipValidationMissingReasonEnum.MISSING_SOURCE);
        assertThat(validationResult.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(validationResult.isManualReviewRequired()).isTrue();
        assertThat(validationResult.isNotTradeInstruction()).isTrue();
    }

    private void assertFailClosedCompletion(SourceTraceEntryCompletionResult result) {
        assertThat(result.getCompletionStatus()).isEqualTo(SourceTraceEntryCompletionStatusEnum.INCOMPLETE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isSourceTraceEntryCompleted()).isFalse();
        assertThat(result.isCompletionReady()).isFalse();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
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
