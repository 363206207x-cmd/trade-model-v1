package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.junit.jupiter.api.Test;

class SourceTraceEntryCompletionContractTest {

    @Test
    void unwiredCompletionResultIsFailClosedReviewOnlyAndNonInstructional() {
        SourceTraceEntryCompletionResult result =
                SourceTraceEntryCompletionResult.unwired("BTCUSDT", "15m");

        assertFailClosedCompletion(result);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("15m");
        assertThat(result.getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
        assertThat(result.getMissingFields()).containsExactly(
                "sourceTraceEntryCompletionPath",
                "entryPriceSource",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef"
        );
    }

    @Test
    void missingAmbiguousUnsafeAndUnwiredCompletionReasonsAllRemainFailClosed() {
        for (SourceTraceEntryCompletionMissingReasonEnum reason
                : SourceTraceEntryCompletionMissingReasonEnum.values()) {
            SourceTraceEntryCompletionResult result =
                    SourceTraceEntryCompletionResult.incomplete("BTCUSDT", "15m", reason, List.of(reason.name()));

            assertFailClosedCompletion(result);
            assertThat(result.getMissingReason()).isEqualTo(reason);
            assertThat(result.getMissingFields()).containsExactly(reason.name());
        }
    }

    @Test
    void completionResultDoesNotPopulateSourceTraceEntryFields() {
        SourceTraceEntryCompletionResult result =
                SourceTraceEntryCompletionResult.incomplete(
                        "BTCUSDT",
                        "15m",
                        SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION,
                        List.of("entryPriceSource")
                );

        assertThat(result.getEntryPriceSource()).isNull();
        assertThat(result.getEntrySourceType()).isNull();
        assertThat(result.getEntrySourceTimeframe()).isNull();
        assertThat(result.getEntrySourceReason()).isNull();
        assertThat(result.getEntrySourceRef()).isNull();
    }

    @Test
    void completionContractExposesOnlyCompletionBoundaryMethod() throws Exception {
        Method method = SourceTraceEntryCompletionContract.class.getMethod(
                "resolveEntryCompletion",
                EntryOwnershipValidationResult.class
        );

        assertThat(method.getReturnType()).isEqualTo(SourceTraceEntryCompletionResult.class);
        assertThat(SourceTraceEntryCompletionContract.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactly("resolveEntryCompletion");
    }

    @Test
    void completionBoundaryDoesNotExposeOrderExecutionCloseReverseOrAutoTradingMethods() {
        assertNoForbiddenMethodNames(SourceTraceEntryCompletionContract.class);
        assertNoForbiddenMethodNames(SourceTraceEntryCompletionResult.class);
    }

    @Test
    void completionContractDoesNotRequireProductionAdapterOrCompletionImplementation() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
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
