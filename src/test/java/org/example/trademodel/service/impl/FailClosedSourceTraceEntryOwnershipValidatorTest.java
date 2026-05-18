package org.example.trademodel.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.EntryOwnershipRequest;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationMissingReasonEnum;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationResult;
import org.example.trademodel.dto.planboundary.EntryOwnershipValidationStatusEnum;
import org.example.trademodel.dto.planboundary.EntrySourceConflictDTO;
import org.example.trademodel.dto.planboundary.EntrySourceFreshnessDTO;
import org.example.trademodel.dto.planboundary.RuleOwnedEntryCandidateDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineContextDTO;
import org.example.trademodel.dto.planboundary.RuntimeKlineItemDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.example.trademodel.service.SourceTraceEntryOwnershipValidator;
import org.junit.jupiter.api.Test;

class FailClosedSourceTraceEntryOwnershipValidatorTest {

    private final SourceTraceEntryOwnershipValidator validator =
            new FailClosedSourceTraceEntryOwnershipValidator();

    @Test
    void defaultValidationResultIsFailClosedReviewOnly() {
        EntryOwnershipValidationResult result = EntryOwnershipValidationResult.missingSource(null, null);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).contains(
                "entryOwnershipRequest",
                "runtimeKlineContext",
                "ruleOwnedEntryCandidate",
                "freshness",
                "conflict"
        );
    }

    @Test
    void missingRequestFailsClosed() {
        EntryOwnershipValidationResult result = validator.validateEntryOwnership(null);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("entryOwnershipRequest");
    }

    @Test
    void missingRuntimeKlineContextFailsClosed() {
        EntryOwnershipValidationResult result = validator.validateEntryOwnership(new EntryOwnershipRequest());

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("runtimeKlineContext");
    }

    @Test
    void missingRuleOwnedCandidateFailsClosed() {
        EntryOwnershipRequest request = completeRequest();
        request.setRuleOwnedEntryCandidate(null);

        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("15m");
        assertThat(result.getMissingFields()).containsExactly("ruleOwnedEntryCandidate");
    }

    @Test
    void missingCandidateBoundaryFailsClosedWithNamedField() {
        EntryOwnershipRequest request = completeRequest();
        request.getRuleOwnedEntryCandidate().setCandidateEntryBoundary(null);

        assertMissingField(request, "ruleOwnedEntryCandidate.candidateEntryBoundary");
    }

    @Test
    void missingEntrySourceTypeFailsClosedWithNamedField() {
        EntryOwnershipRequest request = completeRequest();
        request.getRuleOwnedEntryCandidate().setEntrySourceType(null);

        assertMissingField(request, "ruleOwnedEntryCandidate.entrySourceType");
    }

    @Test
    void missingEntrySourceTimeframeFailsClosedWithNamedField() {
        EntryOwnershipRequest request = completeRequest();
        request.getRuleOwnedEntryCandidate().setEntrySourceTimeframe(null);

        assertMissingField(request, "ruleOwnedEntryCandidate.entrySourceTimeframe");
    }

    @Test
    void missingEntrySourceReasonFailsClosedWithNamedField() {
        EntryOwnershipRequest request = completeRequest();
        request.getRuleOwnedEntryCandidate().setEntrySourceReason(null);

        assertMissingField(request, "ruleOwnedEntryCandidate.entrySourceReason");
    }

    @Test
    void missingEntrySourceRefFailsClosedWithNamedField() {
        EntryOwnershipRequest request = completeRequest();
        request.getRuleOwnedEntryCandidate().setEntrySourceRef(null);

        assertMissingField(request, "ruleOwnedEntryCandidate.entrySourceRef");
    }

    @Test
    void missingFreshnessMetadataFailsClosed() {
        EntryOwnershipRequest request = completeRequest();
        request.setFreshness(null);

        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("freshness");
    }

    @Test
    void missingFreshnessStatusFailsClosedWithNamedField() {
        EntryOwnershipRequest request = completeRequest();
        request.getFreshness().setFreshnessStatus(null);

        assertMissingField(request, "freshness.freshnessStatus");
    }

    @Test
    void missingObservedAtMsFailsClosedWithNamedField() {
        EntryOwnershipRequest request = completeRequest();
        request.getFreshness().setObservedAtMs(null);

        assertMissingField(request, "freshness.observedAtMs");
    }

    @Test
    void missingDecisionCreateTimeMsFailsClosedWithNamedField() {
        EntryOwnershipRequest request = completeRequest();
        request.getFreshness().setDecisionCreateTimeMs(null);

        assertMissingField(request, "freshness.decisionCreateTimeMs");
    }

    @Test
    void missingConflictMetadataFailsClosed() {
        EntryOwnershipRequest request = completeRequest();
        request.setConflict(null);

        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("conflict");
    }

    @Test
    void nullConflictFlagsRemainMissingUnevaluatedAndFailClosed() {
        EntryOwnershipRequest request = completeRequest();
        request.setConflict(new EntrySourceConflictDTO());

        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly(
                "conflictsWithStop",
                "conflictsWithTakeProfit",
                "conflictsWithRiskReward",
                "conflictsWithLiquidity",
                "conflictsWithMultiTimeframe",
                "conflictsWithEvent",
                "conflictsWithWick"
        );
    }

    @Test
    void trueConflictFlagFailsClosed() {
        EntryOwnershipRequest request = completeRequest();
        request.getConflict().setConflictsWithStop(Boolean.TRUE);

        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("conflictsWithStop");
    }

    @Test
    void unsafeManualReviewFalseFailsClosed() {
        EntryOwnershipRequest request = completeRequestWithManualReview(false);

        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("manualReviewRequired");
    }

    @Test
    void unsafeNotTradeInstructionFalseFailsClosed() {
        EntryOwnershipRequest request = completeRequestWithNotTradeInstruction(false);

        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("notTradeInstruction");
    }

    @Test
    void runtimeLatestPriceAloneIsNotSufficientToPassValidation() {
        EntryOwnershipRequest request = new EntryOwnershipRequest();
        RuntimeKlineContextDTO runtimeKlineContext = runtimeKlineContext();
        runtimeKlineContext.setLatestPrice(BigDecimal.ONE);
        request.setRuntimeKlineContext(runtimeKlineContext);

        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("ruleOwnedEntryCandidate");
    }

    @Test
    void runtimeKlineItemsAloneAreNotSufficientToPassValidation() {
        EntryOwnershipRequest request = new EntryOwnershipRequest();
        RuntimeKlineContextDTO runtimeKlineContext = runtimeKlineContext();
        runtimeKlineContext.setKlineItems(List.of(new RuntimeKlineItemDTO()));
        request.setRuntimeKlineContext(runtimeKlineContext);

        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("ruleOwnedEntryCandidate");
    }

    @Test
    void completeRequestStillFailsClosedBecauseSourceTraceCompletionRemainsUnwired() {
        EntryOwnershipValidationResult result = validator.validateEntryOwnership(completeRequest());

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly("sourceTraceEntryOwnershipCompletionPath");
    }

    @Test
    void validatorDoesNotExposeOrderExecutionCloseReverseOrAutoTradingMethodNames() {
        assertNoForbiddenMethodNames(SourceTraceEntryOwnershipValidator.class);
        assertNoForbiddenMethodNames(FailClosedSourceTraceEntryOwnershipValidator.class);
        assertNoForbiddenMethodNames(EntryOwnershipValidationResult.class);
    }

    @Test
    void productionAdapterImplementationIsNotRequiredByValidatorSkeleton() {
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryOwnershipAdapter"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private void assertFailClosed(EntryOwnershipValidationResult result) {
        assertThat(result.getValidationStatus()).isEqualTo(EntryOwnershipValidationStatusEnum.INCOMPLETE);
        assertThat(result.getMissingReason()).isEqualTo(EntryOwnershipValidationMissingReasonEnum.MISSING_SOURCE);
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    private void assertMissingField(EntryOwnershipRequest request, String field) {
        EntryOwnershipValidationResult result = validator.validateEntryOwnership(request);

        assertFailClosed(result);
        assertThat(result.getMissingFields()).containsExactly(field);
    }

    private EntryOwnershipRequest completeRequest() {
        EntryOwnershipRequest request = new EntryOwnershipRequest();
        request.setRuntimeKlineContext(runtimeKlineContext());
        request.setRuleOwnedEntryCandidate(ruleOwnedEntryCandidate());
        request.setFreshness(freshness());
        request.setConflict(nonConflictingMetadata());
        return request;
    }

    private EntryOwnershipRequest completeRequestWithManualReview(boolean manualReviewRequired) {
        EntryOwnershipRequest request = new EntryOwnershipRequest() {
            @Override
            public boolean isManualReviewRequired() {
                return manualReviewRequired;
            }
        };
        request.setRuntimeKlineContext(runtimeKlineContext());
        request.setRuleOwnedEntryCandidate(ruleOwnedEntryCandidate());
        request.setFreshness(freshness());
        request.setConflict(nonConflictingMetadata());
        return request;
    }

    private EntryOwnershipRequest completeRequestWithNotTradeInstruction(boolean notTradeInstruction) {
        EntryOwnershipRequest request = new EntryOwnershipRequest() {
            @Override
            public boolean isNotTradeInstruction() {
                return notTradeInstruction;
            }
        };
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
        candidate.setRuleId("entry-rule");
        candidate.setRuleVersion("v1");
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
                });
    }
}
