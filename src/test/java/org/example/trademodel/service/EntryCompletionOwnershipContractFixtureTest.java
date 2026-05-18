package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;
import org.example.trademodel.dto.planboundary.EntryOwnershipRequest;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationCompletionContext;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationMissingReasonEnum;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationStatusEnum;
import org.example.trademodel.dto.planboundary.EntrySourceConflictDTO;
import org.example.trademodel.dto.planboundary.EntrySourceFreshnessDTO;
import org.example.trademodel.dto.planboundary.RuleOwnedEntryCandidateDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.example.trademodel.service.impl.FailClosedSourceTraceEntryCompletionResolver;
import org.example.trademodel.service.impl.FailClosedSourceTraceEntryOwnershipValidator;
import org.junit.jupiter.api.Test;

class EntryCompletionOwnershipContractFixtureTest {

    private final SourceTraceEntryOwnershipValidator validator =
            new FailClosedSourceTraceEntryOwnershipValidator();
    private final SourceTraceEntryCompletionContract resolver =
            new FailClosedSourceTraceEntryCompletionResolver();
    private final EntryCompletionValidationContextAssembler assembler =
            new EntryCompletionValidationContextAssembler();

    @Test
    void conflictFlagsExplicitlyTrueFailClosedOneAtATime() {
        List<RequestCase> cases = List.of(
                new RequestCase("conflictsWithStop", request -> request.getConflict().setConflictsWithStop(true)),
                new RequestCase("conflictsWithTakeProfit",
                        request -> request.getConflict().setConflictsWithTakeProfit(true)),
                new RequestCase("conflictsWithRiskReward",
                        request -> request.getConflict().setConflictsWithRiskReward(true)),
                new RequestCase("conflictsWithLiquidity",
                        request -> request.getConflict().setConflictsWithLiquidity(true)),
                new RequestCase("conflictsWithMultiTimeframe",
                        request -> request.getConflict().setConflictsWithMultiTimeframe(true)),
                new RequestCase("conflictsWithEvent", request -> request.getConflict().setConflictsWithEvent(true)),
                new RequestCase("conflictsWithWick", request -> request.getConflict().setConflictsWithWick(true))
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldFailsClosed(requestCase);
        }
    }

    @Test
    void mixedNullAndFalseConflictFlagsFailClosed() {
        EntryOwnershipRequest request = completeFixtureRequest();
        request.getConflict().setConflictsWithStop(false);
        request.getConflict().setConflictsWithTakeProfit(false);
        request.getConflict().setConflictsWithRiskReward(null);
        request.getConflict().setConflictsWithLiquidity(false);
        request.getConflict().setConflictsWithMultiTimeframe(false);
        request.getConflict().setConflictsWithEvent(false);
        request.getConflict().setConflictsWithWick(false);

        EntryOwnershipValidationCompletionContext context =
                validateResolveAndAssemble(request);

        assertFailClosedContext(context);
        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("conflictsWithRiskReward");
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
    }

    @Test
    void freshnessClockInversionAndFutureObservedTimeFailClosed() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "freshness.observedAtMs",
                        request -> {
                            request.getFreshness().setObservedAtMs(300L);
                            request.getFreshness().setDecisionCreateTimeMs(200L);
                        }
                ),
                new RequestCase(
                        "freshness.observedAtMs",
                        request -> {
                            request.getFreshness().setObservedAtMs(201L);
                            request.getFreshness().setDecisionCreateTimeMs(200L);
                        }
                )
        );

        for (RequestCase requestCase : cases) {
            EntryOwnershipRequest request = completeFixtureRequest();
            requestCase.mutateRequest.accept(request);

            EntryOwnershipValidationCompletionContext context =
                    validateResolveAndAssemble(request);

            assertFailClosedContext(context);
            assertThat(context.getValidationResult().getMissingFields())
                    .contains("freshness.observedAtMs", "freshness.decisionCreateTimeMs");
            assertThat(context.getCompletionResult().getMissingReason())
                    .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
        }
    }

    @Test
    void staleFreshnessStatusFailsClosed() {
        assertSingleMissingFieldFailsClosed(new RequestCase(
                "freshness.freshnessStatus",
                request -> request.getFreshness().setFreshnessStatus("STALE")
        ));
    }

    @Test
    void runtimeSymbolAndTimeframeMismatchesFailClosed() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "ruleOwnedEntryCandidate.symbol",
                        request -> request.getRuleOwnedEntryCandidate().setSymbol("ETHUSDT")
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.decisionTimeframe",
                        request -> request.getRuleOwnedEntryCandidate().setDecisionTimeframe("1h")
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldFailsClosed(requestCase);
        }
    }

    @Test
    void unsupportedEntrySourceTypeAndTimeframeFailClosed() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceType",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceType("unsupported")
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceTimeframe",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceTimeframe("1h")
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldFailsClosed(requestCase);
        }
    }

    @Test
    void ambiguousEntrySourceRefFailsClosed() {
        assertSingleMissingFieldFailsClosed(new RequestCase(
                "ruleOwnedEntryCandidate.entrySourceRef",
                request -> request.getRuleOwnedEntryCandidate().setEntrySourceRef("source-a|source-b")
        ));
    }

    @Test
    void requiredProvenanceFieldsFailClosedWhenMissing() {
        List<RequestCase> cases = List.of(
                new RequestCase("ruleOwnedEntryCandidate.ruleId",
                        request -> request.getRuleOwnedEntryCandidate().setRuleId(null)),
                new RequestCase("ruleOwnedEntryCandidate.ruleVersion",
                        request -> request.getRuleOwnedEntryCandidate().setRuleVersion(null)),
                new RequestCase("ruleOwnedEntryCandidate.sourceWindow",
                        request -> request.getRuleOwnedEntryCandidate().setSourceWindow(null))
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldFailsClosed(requestCase);
        }
    }

    @Test
    void liquidityStressAndStampedeBlockCompletionAndRequireReview() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "conflictsWithLiquidity",
                        request -> request.getConflict().setConflictReasons(List.of("LIQUIDITY_STRESS"))
                ),
                new RequestCase(
                        "conflictsWithLiquidity",
                        request -> request.getConflict().setConflictReasons(List.of("STAMPEDE"))
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldFailsClosed(requestCase);
        }
    }

    @Test
    void missingEventDataIsNotNoEventRisk() {
        assertSingleMissingFieldFailsClosed(new RequestCase(
                "conflictsWithEvent",
                request -> request.getConflict().setConflictReasons(List.of("MISSING_EVENT_DATA"))
        ));
    }

    @Test
    void multiTimeframeAgreementAloneDoesNotCompleteSourceTrace() {
        assertSingleMissingFieldFailsClosed(new RequestCase(
                "conflictsWithMultiTimeframe",
                request -> request.getConflict().setConflictReasons(List.of("MULTI_TIMEFRAME_AGREEMENT_ONLY"))
        ));
    }

    @Test
    void wickOrPinBarAloneDoesNotProveReversalOrCompletion() {
        assertSingleMissingFieldFailsClosed(new RequestCase(
                "conflictsWithWick",
                request -> request.getConflict().setConflictReasons(List.of("WICK_PIN_BAR_ONLY"))
        ));
    }

    @Test
    void positiveLookingFixtureRemainsReviewOnlyUntilCompletedContractExists() {
        EntryOwnershipValidationCompletionContext context =
                validateResolveAndAssemble(completeFixtureRequest());

        assertFailClosedContext(context);
        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
        assertThat(context.getCompletionResult().getEntryPriceSource()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceType()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceTimeframe()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceReason()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceRef()).isNull();
    }

    private void assertSingleMissingFieldFailsClosed(RequestCase requestCase) {
        EntryOwnershipRequest request = completeFixtureRequest();
        requestCase.mutateRequest.accept(request);

        EntryOwnershipValidationCompletionContext context =
                validateResolveAndAssemble(request);

        assertFailClosedContext(context);
        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly(requestCase.expectedMissingField);
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
    }

    private EntryOwnershipValidationCompletionContext validateResolveAndAssemble(EntryOwnershipRequest request) {
        EntryOwnershipValidationResult validationResult = validator.validateEntryOwnership(request);
        SourceTraceEntryCompletionResult completionResult = resolver.resolveEntryCompletion(validationResult);
        return assembler.assemble(validationResult, completionResult);
    }

    private EntryOwnershipRequest completeFixtureRequest() {
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
        candidate.setEntrySourceRef("fixture-source-ref");
        candidate.setRuleId("fixture-entry-rule");
        candidate.setRuleVersion("fixture-v1");
        candidate.setSourceWindow("fixture-window");
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

    private void assertFailClosedValidation(EntryOwnershipValidationResult validationResult) {
        assertThat(validationResult.getValidationStatus()).isEqualTo(EntryOwnershipValidationStatusEnum.INCOMPLETE);
        assertThat(validationResult.getMissingReason()).isEqualTo(EntryOwnershipValidationMissingReasonEnum.MISSING_SOURCE);
        assertThat(validationResult.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(validationResult.isManualReviewRequired()).isTrue();
        assertThat(validationResult.isNotTradeInstruction()).isTrue();
    }

    private void assertFailClosedCompletion(SourceTraceEntryCompletionResult completionResult) {
        assertThat(completionResult.getCompletionStatus()).isEqualTo(SourceTraceEntryCompletionStatusEnum.INCOMPLETE);
        assertThat(completionResult.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(completionResult.isSourceTraceEntryCompleted()).isFalse();
        assertThat(completionResult.isCompletionReady()).isFalse();
        assertThat(completionResult.isManualReviewRequired()).isTrue();
        assertThat(completionResult.isNotTradeInstruction()).isTrue();
        assertThat(completionResult.getEntryPriceSource()).isNull();
        assertThat(completionResult.getEntrySourceType()).isNull();
        assertThat(completionResult.getEntrySourceTimeframe()).isNull();
        assertThat(completionResult.getEntrySourceReason()).isNull();
        assertThat(completionResult.getEntrySourceRef()).isNull();
    }

    private void assertFailClosedContext(EntryOwnershipValidationCompletionContext context) {
        assertFailClosedValidation(context.getValidationResult());
        assertFailClosedCompletion(context.getCompletionResult());
        assertThat(context.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.isCompletionReady()).isFalse();
        assertThat(context.isManualReviewRequired()).isTrue();
        assertThat(context.isNotTradeInstruction()).isTrue();
    }

    private record RequestCase(String expectedMissingField, Consumer<EntryOwnershipRequest> mutateRequest) {
    }
}
