package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionMissingReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionResult;
import org.example.trademodel.dto.planboundary.SourceTraceEntryCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.example.trademodel.service.impl.FailClosedSourceTraceEntryCompletionResolver;
import org.example.trademodel.service.impl.FailClosedSourceTraceEntryOwnershipValidator;
import org.junit.jupiter.api.Test;

class EntryCompletionFixtureMatrixGuardTest {

    private final SourceTraceEntryOwnershipValidator validator =
            new FailClosedSourceTraceEntryOwnershipValidator();
    private final SourceTraceEntryCompletionContract resolver =
            new FailClosedSourceTraceEntryCompletionResolver();
    private final EntryCompletionValidationContextAssembler assembler =
            new EntryCompletionValidationContextAssembler();

    @Test
    void validLookingFixtureInputStillRemainsReviewOnlyAndNotCompletionReady() {
        EntryOwnershipValidationResult validationResult =
                validator.validateEntryOwnership(completeFixtureRequest());
        SourceTraceEntryCompletionResult completionResult =
                resolver.resolveEntryCompletion(validationResult);
        EntryOwnershipValidationCompletionContext context =
                assembler.assemble(validationResult, completionResult);

        assertFailClosedValidation(validationResult);
        assertThat(validationResult.getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertFailClosedCompletion(completionResult);
        assertThat(completionResult.getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
        assertFailClosedContext(context);
    }

    @Test
    void nullValidationAndNullCompletionRemainFailClosed() {
        EntryOwnershipValidationCompletionContext nullValidationContext =
                assembler.assemble(null, SourceTraceEntryCompletionResult.unwired("BTCUSDT", "15m"));
        EntryOwnershipValidationCompletionContext nullCompletionContext =
                assembler.assemble(
                        EntryOwnershipValidationResult.missingSource("BTCUSDT", "15m", List.of("validation")),
                        null
                );
        EntryOwnershipValidationCompletionContext bothNullContext =
                assembler.assemble(null, null);

        assertFailClosedContext(nullValidationContext);
        assertThat(nullValidationContext.getValidationResult().getMissingFields())
                .containsExactly("entryOwnershipValidationResult");
        assertFailClosedContext(nullCompletionContext);
        assertThat(nullCompletionContext.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
        assertFailClosedContext(bothNullContext);
        assertThat(bothNullContext.getValidationResult().getMissingFields())
                .containsExactly("entryOwnershipValidationResult");
        assertThat(bothNullContext.getCompletionResult().getMissingFields())
                .containsExactly("sourceTraceEntryCompletionResult");
    }

    @Test
    void missingCompletionPathRemainsFailClosed() {
        EntryOwnershipValidationCompletionContext context =
                validateResolveAndAssemble(completeFixtureRequest());

        assertFailClosedContext(context);
        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
    }

    @Test
    void candidateOwnershipFieldsAreIndependentlyRequired() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "ruleOwnedEntryCandidate.candidateEntryBoundary",
                        request -> request.getRuleOwnedEntryCandidate().setCandidateEntryBoundary(null)
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceType",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceType(null)
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceTimeframe",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceTimeframe(null)
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceReason",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceReason(null)
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceRef",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceRef(null)
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldFailsClosed(requestCase);
        }
    }

    @Test
    void freshnessFieldsAreIndependentlyRequired() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "freshness.freshnessStatus",
                        request -> request.getFreshness().setFreshnessStatus(null)
                ),
                new RequestCase(
                        "freshness.observedAtMs",
                        request -> request.getFreshness().setObservedAtMs(null)
                ),
                new RequestCase(
                        "freshness.decisionCreateTimeMs",
                        request -> request.getFreshness().setDecisionCreateTimeMs(null)
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldFailsClosed(requestCase);
        }
    }

    @Test
    void nullableConflictMetadataFailsClosedWhenAnyFlagIsNull() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "conflictsWithStop",
                        request -> request.getConflict().setConflictsWithStop(null)
                ),
                new RequestCase(
                        "conflictsWithTakeProfit",
                        request -> request.getConflict().setConflictsWithTakeProfit(null)
                ),
                new RequestCase(
                        "conflictsWithRiskReward",
                        request -> request.getConflict().setConflictsWithRiskReward(null)
                ),
                new RequestCase(
                        "conflictsWithLiquidity",
                        request -> request.getConflict().setConflictsWithLiquidity(null)
                ),
                new RequestCase(
                        "conflictsWithMultiTimeframe",
                        request -> request.getConflict().setConflictsWithMultiTimeframe(null)
                ),
                new RequestCase(
                        "conflictsWithEvent",
                        request -> request.getConflict().setConflictsWithEvent(null)
                ),
                new RequestCase(
                        "conflictsWithWick",
                        request -> request.getConflict().setConflictsWithWick(null)
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldFailsClosed(requestCase);
        }
    }

    @Test
    void explicitNonConflictFlagsDoNotSubstituteForMissingCompletion() {
        EntryOwnershipRequest request = completeFixtureRequest();

        assertThat(request.getConflict().getConflictsWithStop()).isFalse();
        assertThat(request.getConflict().getConflictsWithTakeProfit()).isFalse();
        assertThat(request.getConflict().getConflictsWithRiskReward()).isFalse();
        assertThat(request.getConflict().getConflictsWithLiquidity()).isFalse();
        assertThat(request.getConflict().getConflictsWithMultiTimeframe()).isFalse();
        assertThat(request.getConflict().getConflictsWithEvent()).isFalse();
        assertThat(request.getConflict().getConflictsWithWick()).isFalse();

        EntryOwnershipValidationCompletionContext context =
                validateResolveAndAssemble(request);

        assertFailClosedContext(context);
        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
    }

    @Test
    void latestPriceKlineItemsAndSymbolTimeframeMetadataAloneAreNotSufficient() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "ruleOwnedEntryCandidate",
                        request -> request.getRuntimeKlineContext().setLatestPrice(new BigDecimal("100.00"))
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate",
                        request -> request.getRuntimeKlineContext().setKlineItems(List.of(klineItem()))
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate",
                        request -> {
                        }
                )
        );

        for (RequestCase requestCase : cases) {
            EntryOwnershipRequest request = runtimeOnlyRequest();
            requestCase.mutateRequest.accept(request);

            EntryOwnershipValidationCompletionContext context =
                    validateResolveAndAssemble(request);

            assertFailClosedContext(context);
            assertThat(context.getValidationResult().getMissingFields())
                    .containsExactly(requestCase.expectedMissingField);
            assertThat(context.getCompletionResult().getMissingReason())
                    .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
        }
    }

    @Test
    void resolverAndAssemblerPresenceAloneAreNotSufficient() {
        EntryOwnershipValidationResult validationResult =
                EntryOwnershipValidationResult.missingSource(
                        "BTCUSDT",
                        "15m",
                        List.of("sourceTraceEntryOwnershipCompletionPath")
                );

        SourceTraceEntryCompletionResult resolverResult =
                resolver.resolveEntryCompletion(validationResult);
        EntryOwnershipValidationCompletionContext assemblerContext =
                assembler.assemble(validationResult, resolverResult);

        assertFailClosedCompletion(resolverResult);
        assertThat(resolverResult.getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
        assertFailClosedContext(assemblerContext);
    }

    @Test
    void fixtureOutputsRemainNonInstructionalAndDoNotRequireProductionAdapters() {
        EntryOwnershipValidationCompletionContext context =
                validateResolveAndAssemble(completeFixtureRequest());

        assertFailClosedContext(context);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
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
        EntryOwnershipRequest request = runtimeOnlyRequest();
        request.setRuleOwnedEntryCandidate(ruleOwnedEntryCandidate());
        request.setFreshness(freshness());
        request.setConflict(nonConflictingMetadata());
        return request;
    }

    private EntryOwnershipRequest runtimeOnlyRequest() {
        EntryOwnershipRequest request = new EntryOwnershipRequest();
        request.setRuntimeKlineContext(runtimeKlineContext());
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

    private RuntimeKlineItemDTO klineItem() {
        RuntimeKlineItemDTO item = new RuntimeKlineItemDTO();
        item.setSourceTraceId("fixture-kline-source-trace");
        item.setQualityStatus("FIXTURE_ONLY");
        return item;
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
