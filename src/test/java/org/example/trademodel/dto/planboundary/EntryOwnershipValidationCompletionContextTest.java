package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.service.SourceTraceEntryCompletionContract;
import org.example.trademodel.service.SourceTraceEntryOwnershipValidator;
import org.example.trademodel.service.impl.FailClosedSourceTraceEntryOwnershipValidator;
import org.junit.jupiter.api.Test;

class EntryOwnershipValidationCompletionContextTest {

    private final SourceTraceEntryOwnershipValidator validator =
            new FailClosedSourceTraceEntryOwnershipValidator();

    @Test
    void completionContractPresenceAloneDoesNotMakeValidationPass() {
        SourceTraceEntryCompletionContract completionContract = validationResult ->
                SourceTraceEntryCompletionResult.unwired(validationResult.getSymbol(), validationResult.getTimeframe());

        EntryOwnershipValidationResult validationResult =
                validator.validateEntryOwnership(completeSkeletonRequest());
        SourceTraceEntryCompletionResult completionResult =
                completionContract.resolveEntryCompletion(validationResult);
        EntryOwnershipValidationCompletionContext context =
                EntryOwnershipValidationCompletionContext.from(validationResult, completionResult);

        assertFailClosedValidation(validationResult);
        assertThat(validationResult.getMissingFields()).containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertFailClosedContext(context);
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
    }

    @Test
    void completionContractPresenceAloneDoesNotMakeCompletionReady() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource("BTCUSDT", "15m", List.of("validation"));
        SourceTraceEntryCompletionResult completionResult =
                SourceTraceEntryCompletionResult.unwired("BTCUSDT", "15m");

        EntryOwnershipValidationCompletionContext context =
                EntryOwnershipValidationCompletionContext.from(validationResult, completionResult);

        assertFailClosedContext(context);
        assertThat(context.isCompletionReady()).isFalse();
        assertThat(context.getCompletionResult().isCompletionReady()).isFalse();
    }

    @Test
    void missingCompletionStateFailsClosedInValidatorFacingContext() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource("BTCUSDT", "15m", List.of("validation"));

        EntryOwnershipValidationCompletionContext context =
                EntryOwnershipValidationCompletionContext.from(validationResult, null);

        assertFailClosedContext(context);
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
        assertThat(context.getCompletionResult().getMissingFields())
                .containsExactly("sourceTraceEntryCompletionResult");
        assertThat(context.getMissingFields()).contains("validation", "sourceTraceEntryCompletionResult");
    }

    @Test
    void ambiguousUnsafeAndUnwiredCompletionStatesFailClosedInValidatorFacingContext() {
        List<SourceTraceEntryCompletionMissingReasonEnum> reasons = List.of(
                SourceTraceEntryCompletionMissingReasonEnum.AMBIGUOUS_COMPLETION,
                SourceTraceEntryCompletionMissingReasonEnum.UNSAFE_COMPLETION,
                SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED
        );

        for (SourceTraceEntryCompletionMissingReasonEnum reason : reasons) {
            EntryOwnershipValidationCompletionContext context =
                    EntryOwnershipValidationCompletionContext.from(
                            EntryOwnershipValidationResult.missingSource("BTCUSDT", "15m", List.of("validation")),
                            SourceTraceEntryCompletionResult.incomplete(
                                    "BTCUSDT",
                                    "15m",
                                    reason,
                                    List.of(reason.name())
                            )
                    );

            assertFailClosedContext(context);
            assertThat(context.getCompletionResult().getMissingReason()).isEqualTo(reason);
            assertThat(context.getCompletionResult().isCompletionReady()).isFalse();
        }
    }

    @Test
    void validatorCompletionContextDoesNotBecomeTradeReadyOrInstructional() {
        EntryOwnershipValidationCompletionContext context =
                EntryOwnershipValidationCompletionContext.from(
                        EntryOwnershipValidationResult.missingSource("BTCUSDT", "15m", List.of("validation")),
                        SourceTraceEntryCompletionResult.unwired("BTCUSDT", "15m")
                );

        assertFailClosedContext(context);
        assertThat(context.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.isManualReviewRequired()).isTrue();
        assertThat(context.isNotTradeInstruction()).isTrue();
        assertNoForbiddenMethodNames(EntryOwnershipValidationCompletionContext.class);
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
        assertThat(context.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.isManualReviewRequired()).isTrue();
        assertThat(context.isNotTradeInstruction()).isTrue();
    }

    private EntryOwnershipRequest completeSkeletonRequest() {
        EntryOwnershipRequest request = new EntryOwnershipRequest();
        request.setRuntimeKlineContext(runtimeKlineContext());
        request.setRuleOwnedEntryCandidate(ruleOwnedEntryCandidate());
        request.setFreshness(freshness());
        request.setConflict(nonConflictingMetadata());
        return request;
    }

    private RuntimeKlineContextDTO runtimeKlineContext() {
        RuntimeKlineContextDTO runtimeKlineContext = new RuntimeKlineContextDTO();
        runtimeKlineContext.setSymbol("BTCUSDT");
        runtimeKlineContext.setTimeframe("15m");
        return runtimeKlineContext;
    }

    private RuleOwnedEntryCandidateDTO ruleOwnedEntryCandidate() {
        RuleOwnedEntryCandidateDTO candidate = new RuleOwnedEntryCandidateDTO();
        candidate.setSymbol("BTCUSDT");
        candidate.setDecisionTimeframe("15m");
        candidate.setCandidateEntryBoundary(BigDecimal.ONE);
        candidate.setEntrySourceType("rule-owned-boundary");
        candidate.setEntrySourceTimeframe("15m");
        candidate.setEntrySourceReason("fixture-only");
        candidate.setEntrySourceRef("source-ref");
        return candidate;
    }

    private EntrySourceFreshnessDTO freshness() {
        EntrySourceFreshnessDTO freshness = new EntrySourceFreshnessDTO();
        freshness.setFreshnessStatus("FRESH");
        freshness.setObservedAtMs(100L);
        freshness.setDecisionCreateTimeMs(200L);
        return freshness;
    }

    private EntrySourceConflictDTO nonConflictingMetadata() {
        EntrySourceConflictDTO conflict = new EntrySourceConflictDTO();
        conflict.setConflictsWithStop(Boolean.FALSE);
        conflict.setConflictsWithTakeProfit(Boolean.FALSE);
        conflict.setConflictsWithRiskReward(Boolean.FALSE);
        conflict.setConflictsWithLiquidity(Boolean.FALSE);
        conflict.setConflictsWithMultiTimeframe(Boolean.FALSE);
        conflict.setConflictsWithEvent(Boolean.FALSE);
        conflict.setConflictsWithWick(Boolean.FALSE);
        return conflict;
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
