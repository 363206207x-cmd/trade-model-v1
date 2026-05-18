package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
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

class EntryCompletionPositiveContractFixtureSkeletonTest {

    private final SourceTraceEntryOwnershipValidator validator =
            new FailClosedSourceTraceEntryOwnershipValidator();
    private final SourceTraceEntryCompletionContract resolver =
            new FailClosedSourceTraceEntryCompletionResolver();
    private final EntryCompletionValidationContextAssembler assembler =
            new EntryCompletionValidationContextAssembler();

    @Test
    void positiveFixtureRequiresCompletionPath() {
        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(completeFixtureRequest());

        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.COMPLETION_UNWIRED);
    }

    @Test
    void positiveFixtureRequiresEntryPriceSource() {
        SourceTraceEntryCompletionResult unwiredResult =
                SourceTraceEntryCompletionResult.unwired("BTCUSDT", "15m");

        assertFailClosedCompletion(unwiredResult);
        assertThat(unwiredResult.getEntryPriceSource()).isNull();
        assertThat(unwiredResult.getMissingFields()).contains("entryPriceSource");
    }

    @Test
    void positiveFixtureRejectsLatestPriceOnly() {
        EntryOwnershipRequest request = runtimeOnlyRequest();
        request.getRuntimeKlineContext().setLatestPrice(BigDecimal.ONE);

        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(request);

        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("ruleOwnedEntryCandidate");
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
    }

    @Test
    void positiveFixtureRejectsRawKlineOnly() {
        EntryOwnershipRequest request = runtimeOnlyRequest();
        request.getRuntimeKlineContext().setKlineItems(List.of(klineItem()));

        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(request);

        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("ruleOwnedEntryCandidate");
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
    }

    @Test
    void positiveFixtureRequiresAllowedSourceType() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceType",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceType(null)
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceType",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceType("dashboard-text")
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldDowngrades(requestCase);
        }
    }

    @Test
    void positiveFixtureRequiresMatchingSourceTimeframe() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceTimeframe",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceTimeframe(null)
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceTimeframe",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceTimeframe("1h")
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldDowngrades(requestCase);
        }
    }

    @Test
    void positiveFixtureRequiresSourceReason() {
        assertSingleMissingFieldDowngrades(new RequestCase(
                "ruleOwnedEntryCandidate.entrySourceReason",
                request -> request.getRuleOwnedEntryCandidate().setEntrySourceReason(null)
        ));
    }

    @Test
    void positiveFixtureRejectsTradeInstructionReason() {
        EntryOwnershipRequest request = completeFixtureRequest();
        request.getRuleOwnedEntryCandidate().setEntrySourceReason("open long now");

        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(request);

        assertThat(context.getCompletionResult().getEntrySourceReason()).isNull();
        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertThat(context.isNotTradeInstruction()).isTrue();
    }

    @Test
    void positiveFixtureRequiresSingularSourceRef() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceRef",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceRef(null)
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.entrySourceRef",
                        request -> request.getRuleOwnedEntryCandidate().setEntrySourceRef("ref-a,ref-b")
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldDowngrades(requestCase);
        }
    }

    @Test
    void positiveFixtureRequiresCandidateProvenance() {
        List<RequestCase> cases = List.of(
                new RequestCase(
                        "ruleOwnedEntryCandidate.ruleId",
                        request -> request.getRuleOwnedEntryCandidate().setRuleId(null)
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.ruleVersion",
                        request -> request.getRuleOwnedEntryCandidate().setRuleVersion(null)
                ),
                new RequestCase(
                        "ruleOwnedEntryCandidate.sourceWindow",
                        request -> request.getRuleOwnedEntryCandidate().setSourceWindow(null)
                )
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldDowngrades(requestCase);
        }
    }

    @Test
    void positiveFixtureRejectsSymbolMismatch() {
        assertSingleMissingFieldDowngrades(new RequestCase(
                "ruleOwnedEntryCandidate.symbol",
                request -> request.getRuleOwnedEntryCandidate().setSymbol("ETHUSDT")
        ));
    }

    @Test
    void positiveFixtureRejectsTimeframeMismatch() {
        assertSingleMissingFieldDowngrades(new RequestCase(
                "ruleOwnedEntryCandidate.decisionTimeframe",
                request -> request.getRuleOwnedEntryCandidate().setDecisionTimeframe("1h")
        ));
    }

    @Test
    void positiveFixtureRejectsStaleFreshness() {
        assertSingleMissingFieldDowngrades(new RequestCase(
                "freshness.freshnessStatus",
                request -> request.getFreshness().setFreshnessStatus("STALE")
        ));
    }

    @Test
    void positiveFixtureRejectsFutureObservedTime() {
        assertFreshnessClockFailure(request -> {
            request.getFreshness().setObservedAtMs(300L);
            request.getFreshness().setDecisionCreateTimeMs(200L);
        });
    }

    @Test
    void positiveFixtureRejectsClockInversion() {
        assertFreshnessClockFailure(request -> {
            request.getFreshness().setObservedAtMs(201L);
            request.getFreshness().setDecisionCreateTimeMs(200L);
        });
    }

    @Test
    void positiveFixtureRejectsNullConflictFlags() {
        List<RequestCase> cases = List.of(
                new RequestCase("conflictsWithStop",
                        request -> request.getConflict().setConflictsWithStop(null)),
                new RequestCase("conflictsWithTakeProfit",
                        request -> request.getConflict().setConflictsWithTakeProfit(null)),
                new RequestCase("conflictsWithRiskReward",
                        request -> request.getConflict().setConflictsWithRiskReward(null)),
                new RequestCase("conflictsWithLiquidity",
                        request -> request.getConflict().setConflictsWithLiquidity(null)),
                new RequestCase("conflictsWithMultiTimeframe",
                        request -> request.getConflict().setConflictsWithMultiTimeframe(null)),
                new RequestCase("conflictsWithEvent",
                        request -> request.getConflict().setConflictsWithEvent(null)),
                new RequestCase("conflictsWithWick",
                        request -> request.getConflict().setConflictsWithWick(null))
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldDowngrades(requestCase);
        }
    }

    @Test
    void positiveFixtureRejectsTrueConflictFlags() {
        List<RequestCase> cases = List.of(
                new RequestCase("conflictsWithStop",
                        request -> request.getConflict().setConflictsWithStop(true)),
                new RequestCase("conflictsWithTakeProfit",
                        request -> request.getConflict().setConflictsWithTakeProfit(true)),
                new RequestCase("conflictsWithRiskReward",
                        request -> request.getConflict().setConflictsWithRiskReward(true)),
                new RequestCase("conflictsWithLiquidity",
                        request -> request.getConflict().setConflictsWithLiquidity(true)),
                new RequestCase("conflictsWithMultiTimeframe",
                        request -> request.getConflict().setConflictsWithMultiTimeframe(true)),
                new RequestCase("conflictsWithEvent",
                        request -> request.getConflict().setConflictsWithEvent(true)),
                new RequestCase("conflictsWithWick",
                        request -> request.getConflict().setConflictsWithWick(true))
        );

        for (RequestCase requestCase : cases) {
            assertSingleMissingFieldDowngrades(requestCase);
        }
    }

    @Test
    void positiveFixtureRejectsLiquidityStress() {
        assertSingleMissingFieldDowngrades(new RequestCase(
                "conflictsWithLiquidity",
                request -> request.getConflict().setConflictReasons(List.of("LIQUIDITY_STRESS"))
        ));
    }

    @Test
    void positiveFixtureRejectsMissingEventData() {
        assertSingleMissingFieldDowngrades(new RequestCase(
                "conflictsWithEvent",
                request -> request.getConflict().setConflictReasons(List.of("MISSING_EVENT_DATA"))
        ));
    }

    @Test
    void positiveFixtureRejectsMultiTimeframeAgreementOnly() {
        assertSingleMissingFieldDowngrades(new RequestCase(
                "conflictsWithMultiTimeframe",
                request -> request.getConflict().setConflictReasons(List.of("MULTI_TIMEFRAME_AGREEMENT_ONLY"))
        ));
    }

    @Test
    void positiveFixtureRejectsWickPinBarOnly() {
        assertSingleMissingFieldDowngrades(new RequestCase(
                "conflictsWithWick",
                request -> request.getConflict().setConflictReasons(List.of("WICK_PIN_BAR_ONLY"))
        ));
    }

    @Test
    void positiveFixtureRemainsReviewOnly() {
        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(completeFixtureRequest());

        assertThat(context.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.getValidationResult().getReviewMode())
                .isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.getCompletionResult().getReviewMode())
                .isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.isManualReviewRequired()).isTrue();
    }

    @Test
    void positiveFixtureCannotBecomeTradeInstruction() {
        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(completeFixtureRequest());

        assertThat(context.isNotTradeInstruction()).isTrue();
        assertThat(context.getValidationResult().isNotTradeInstruction()).isTrue();
        assertThat(context.getCompletionResult().isNotTradeInstruction()).isTrue();
        assertThat(context.getCompletionResult().getEntrySourceReason()).isNull();
    }

    @Test
    void positiveFixtureDoesNotWireReadiness() {
        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(completeFixtureRequest());

        assertThat(context.isCompletionReady()).isFalse();
        assertThat(context.getCompletionResult().isCompletionReady()).isFalse();
        assertThat(context.getCompletionResult().isSourceTraceEntryCompleted()).isFalse();
        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly("sourceTraceEntryOwnershipCompletionPath");
        assertThatThrownBy(() -> Class.forName(
                        "org.example.trademodel.service.impl.DefaultSourceTraceEntryCompletionContract"
                ))
                .isInstanceOf(ClassNotFoundException.class);
    }

    @Test
    void positiveFixtureDoesNotPersistDashboardOrSchema() {
        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(completeFixtureRequest());
        List<String> methodNames = methodNames(
                SourceTraceEntryCompletionResult.class,
                EntryOwnershipValidationCompletionContext.class,
                EntryCompletionValidationContextAssembler.class,
                FailClosedSourceTraceEntryCompletionResolver.class
        );

        assertThat(methodNames).noneMatch(this::isPersistenceOrDashboardMethodName);
        assertThat(context.getCompletionResult().getEntryPriceSource()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceType()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceTimeframe()).isNull();
        assertThat(context.getCompletionResult().getEntrySourceRef()).isNull();
    }

    @Test
    void positiveFixtureDowngradesDeterministically() {
        EntryOwnershipValidationCompletionContext first =
                validateResolveAndAssemble(completeFixtureRequest());
        EntryOwnershipValidationCompletionContext second =
                validateResolveAndAssemble(completeFixtureRequest());

        assertFailClosedContext(first);
        assertFailClosedContext(second);
        assertThat(second.getValidationResult().getValidationStatus())
                .isEqualTo(first.getValidationResult().getValidationStatus());
        assertThat(second.getValidationResult().getMissingFields())
                .isEqualTo(first.getValidationResult().getMissingFields());
        assertThat(second.getCompletionResult().getCompletionStatus())
                .isEqualTo(first.getCompletionResult().getCompletionStatus());
        assertThat(second.getCompletionResult().getMissingReason())
                .isEqualTo(first.getCompletionResult().getMissingReason());
        assertThat(second.getCompletionResult().getMissingFields())
                .isEqualTo(first.getCompletionResult().getMissingFields());
    }

    private void assertSingleMissingFieldDowngrades(RequestCase requestCase) {
        EntryOwnershipRequest request = completeFixtureRequest();
        requestCase.mutateRequest.accept(request);

        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(request);

        assertThat(context.getValidationResult().getMissingFields())
                .containsExactly(requestCase.expectedMissingField);
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
    }

    private void assertFreshnessClockFailure(Consumer<EntryOwnershipRequest> mutation) {
        EntryOwnershipRequest request = completeFixtureRequest();
        mutation.accept(request);

        EntryOwnershipValidationCompletionContext context =
                assertPositiveFixtureDowngrades(request);

        assertThat(context.getValidationResult().getMissingFields())
                .contains("freshness.observedAtMs", "freshness.decisionCreateTimeMs");
        assertThat(context.getCompletionResult().getMissingReason())
                .isEqualTo(SourceTraceEntryCompletionMissingReasonEnum.MISSING_COMPLETION);
    }

    private EntryOwnershipValidationCompletionContext assertPositiveFixtureDowngrades(
            EntryOwnershipRequest request
    ) {
        EntryOwnershipValidationCompletionContext context = validateResolveAndAssemble(request);
        assertFailClosedContext(context);
        assertThat(context.getMissingFields()).isNotEmpty();
        return context;
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

    private List<String> methodNames(Class<?>... types) {
        return Arrays.stream(types)
                .flatMap(type -> Arrays.stream(type.getMethods()))
                .map(Method::getName)
                .toList();
    }

    private boolean isPersistenceOrDashboardMethodName(String methodName) {
        String lowerName = methodName.toLowerCase();
        return lowerName.contains("dashboard")
                || lowerName.contains("schema")
                || lowerName.contains("persist")
                || lowerName.contains("save");
    }

    private void assertFailClosedContext(EntryOwnershipValidationCompletionContext context) {
        assertFailClosedValidation(context.getValidationResult());
        assertFailClosedCompletion(context.getCompletionResult());
        assertThat(context.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(context.isCompletionReady()).isFalse();
        assertThat(context.isManualReviewRequired()).isTrue();
        assertThat(context.isNotTradeInstruction()).isTrue();
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

    private record RequestCase(String expectedMissingField, Consumer<EntryOwnershipRequest> mutateRequest) {
    }
}
