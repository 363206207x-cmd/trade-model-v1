package org.example.trademodel.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionContractDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionDowngradeReasonEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryPositiveCompletionTransitionEnum;
import org.example.trademodel.dto.planboundary.SourceTraceEntryReadOnlyCompletionRequest;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.junit.jupiter.api.Test;

class SourceTraceEntryReadOnlyCompletionAssemblerTest {

    private final SourceTraceEntryReadOnlyCompletionAssembler assembler =
            new SourceTraceEntryReadOnlyCompletionAssembler();

    @Test
    void completeReadOnlyInputCarriesMetadataButRemainsUnwiredReviewOnly() {
        SourceTraceEntryPositiveCompletionContractDTO dto = assembler.assemble(completeRequest());

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.POSITIVE_DESIGN_REVIEW_ONLY);
        assertThat(dto.getCompletionTransition())
                .isEqualTo(SourceTraceEntryPositiveCompletionTransitionEnum.INCOMPLETE_TO_POSITIVE_DESIGN_REVIEW_ONLY);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED);
        assertThat(dto.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(dto.getTimeframe()).isEqualTo("15m");
        assertThat(dto.getSourceTraceEntryOwnershipCompletionPath())
                .isEqualTo("read-only-completion-path");
        assertThat(dto.getEntryPriceSource()).isNull();
        assertThat(dto.getMissingFields()).containsExactly(
                "readOnlyCompletionProductionPathUnwired",
                "entryPriceSource"
        );
        assertStillNonProduction(dto);
    }

    @Test
    void nullInputFailsClosed() {
        SourceTraceEntryPositiveCompletionContractDTO dto = assembler.assemble(null);

        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
        assertThat(dto.getMissingFields()).containsExactly("request");
        assertStillNonProduction(dto);
    }

    @Test
    void missingCompletionSourceProvenanceFreshnessAndConflictEvidenceFailClosed() {
        SourceTraceEntryPositiveCompletionContractDTO dto = assembler.assemble(
                SourceTraceEntryReadOnlyCompletionRequest.builder().build()
        );

        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.MISSING_REQUIRED_FIELD);
        assertThat(dto.getMissingFields()).contains(
                "sourceTraceEntryOwnershipCompletionPath",
                "entrySourceType",
                "entrySourceTimeframe",
                "entrySourceReason",
                "entrySourceRef",
                "ruleId",
                "ruleVersion",
                "sourceWindow",
                "freshnessStatus",
                "observedAtMs",
                "decisionCreateTimeMs",
                "conflictsWithStop",
                "conflictsWithTakeProfit",
                "conflictsWithRiskReward",
                "conflictsWithLiquidity",
                "conflictsWithMultiTimeframe",
                "conflictsWithEvent",
                "conflictsWithWick"
        );
        assertStillNonProduction(dto);
    }

    @Test
    void missingCompletionPathFailsClosedIndependently() {
        assertMissing(
                assembler.assemble(
                        completeRequestBuilder()
                                .sourceTraceEntryOwnershipCompletionPath(null)
                                .build()
                ),
                "sourceTraceEntryOwnershipCompletionPath"
        );
    }

    @Test
    void missingSourceFieldsFailClosedIndependently() {
        assertMissing(
                assembler.assemble(completeRequestBuilder().entrySourceType(null).build()),
                "entrySourceType"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().entrySourceTimeframe(null).build()),
                "entrySourceTimeframe"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().entrySourceReason(null).build()),
                "entrySourceReason"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().entrySourceRef(null).build()),
                "entrySourceRef"
        );
    }

    @Test
    void blankRequiredStringsFailClosedIndependently() {
        assertMissing(
                assembler.assemble(completeRequestBuilder().sourceTraceEntryOwnershipCompletionPath(" ").build()),
                "sourceTraceEntryOwnershipCompletionPath"
        );
        assertMissing(assembler.assemble(completeRequestBuilder().entrySourceType(" ").build()), "entrySourceType");
        assertMissing(
                assembler.assemble(completeRequestBuilder().entrySourceTimeframe(" ").build()),
                "entrySourceTimeframe"
        );
        assertMissing(assembler.assemble(completeRequestBuilder().entrySourceReason(" ").build()), "entrySourceReason");
        assertMissing(assembler.assemble(completeRequestBuilder().entrySourceRef(" ").build()), "entrySourceRef");
        assertMissing(assembler.assemble(completeRequestBuilder().ruleId(" ").build()), "ruleId");
        assertMissing(assembler.assemble(completeRequestBuilder().ruleVersion(" ").build()), "ruleVersion");
        assertMissing(assembler.assemble(completeRequestBuilder().sourceWindow(" ").build()), "sourceWindow");
        assertMissing(assembler.assemble(completeRequestBuilder().freshnessStatus(" ").build()), "freshnessStatus");
    }

    @Test
    void missingProvenanceFreshnessAndConflictEvidenceFailClosedIndependently() {
        assertMissing(assembler.assemble(completeRequestBuilder().ruleId(null).build()), "ruleId");
        assertMissing(assembler.assemble(completeRequestBuilder().ruleVersion(null).build()), "ruleVersion");
        assertMissing(assembler.assemble(completeRequestBuilder().sourceWindow(null).build()), "sourceWindow");
        assertMissing(assembler.assemble(completeRequestBuilder().freshnessStatus(null).build()), "freshnessStatus");
        assertMissing(assembler.assemble(completeRequestBuilder().observedAtMs(null).build()), "observedAtMs");
        assertMissing(
                assembler.assemble(completeRequestBuilder().decisionCreateTimeMs(null).build()),
                "decisionCreateTimeMs"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().conflictsWithStop(null).build()),
                "conflictsWithStop"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().conflictsWithTakeProfit(null).build()),
                "conflictsWithTakeProfit"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().conflictsWithRiskReward(null).build()),
                "conflictsWithRiskReward"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().conflictsWithLiquidity(null).build()),
                "conflictsWithLiquidity"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().conflictsWithMultiTimeframe(null).build()),
                "conflictsWithMultiTimeframe"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().conflictsWithEvent(null).build()),
                "conflictsWithEvent"
        );
        assertMissing(
                assembler.assemble(completeRequestBuilder().conflictsWithWick(null).build()),
                "conflictsWithWick"
        );
    }

    @Test
    void staleUnknownFutureAndClockInversionFailClosed() {
        SourceTraceEntryPositiveCompletionContractDTO stale = assembler.assemble(
                completeRequestBuilder().freshnessStatus("STALE").build()
        );
        SourceTraceEntryPositiveCompletionContractDTO unknown = assembler.assemble(
                completeRequestBuilder().freshnessStatus("UNKNOWN").build()
        );
        SourceTraceEntryPositiveCompletionContractDTO future = assembler.assemble(
                completeRequestBuilder().observedAtMs(300L).decisionCreateTimeMs(200L).build()
        );

        assertUnsafe(stale, "freshnessStatus");
        assertUnsafe(unknown, "freshnessStatus");
        assertUnsafe(future, "observedAtMsFuture", "clockInversion");
    }

    @Test
    void runtimeLikeSourceTagsFailClosedOneAtATime() {
        String[][] runtimeLikeTags = {
                {"LATEST_PRICE_ONLY", "LATEST_PRICE_ONLY"},
                {"latest price only", "LATEST_PRICE_ONLY"},
                {"latest-price-only", "LATEST_PRICE_ONLY"},
                {"RAW_KLINE_ONLY", "RAW_KLINE_ONLY"},
                {"raw kline only", "RAW_KLINE_ONLY"},
                {"AI_TEXT", "AI_TEXT"},
                {"DASHBOARD_TEXT", "DASHBOARD_TEXT"},
                {"EXTERNAL_DATA", "EXTERNAL_DATA"},
                {"external", "EXTERNAL"},
                {"ORDER_DATA", "ORDER_DATA"},
                {"order", "ORDER"},
                {"EXECUTION_DATA", "EXECUTION_DATA"},
                {"execution", "EXECUTION"},
                {"BoundaryCandidateService VALID", "BOUNDARYCANDIDATESERVICE_VALID"},
                {"ExecutionPlan ready", "EXECUTIONPLAN_READY"},
                {"SourceTrace runtime completion", "SOURCETRACE_RUNTIME_COMPLETION"},
                {"production completion", "PRODUCTION_COMPLETION"},
                {"trade-ready", "TRADE_READY"}
        };

        for (String[] runtimeLikeTag : runtimeLikeTags) {
            SourceTraceEntryPositiveCompletionContractDTO dto = assembler.assemble(
                    completeRequestBuilder().sourceTags(List.of(runtimeLikeTag[0])).build()
            );

            assertUnsafe(dto, runtimeLikeTag[1]);
        }
    }

    @Test
    void mixedSafeAndRuntimeLikeSourceTagsFailClosed() {
        SourceTraceEntryPositiveCompletionContractDTO dto = assembler.assemble(
                completeRequestBuilder()
                        .sourceTags(List.of("READ_ONLY_INTERNAL", "latest-price-only", "FIXTURE_ONLY"))
                        .build()
        );

        assertUnsafe(dto, "LATEST_PRICE_ONLY");
    }

    @Test
    void emptyDuplicateBlankOrAmbiguousSourceRefsFailClosed() {
        SourceTraceEntryPositiveCompletionContractDTO empty = assembler.assemble(
                completeRequestBuilder().sourceRefs(List.of()).build()
        );
        SourceTraceEntryPositiveCompletionContractDTO duplicate = assembler.assemble(
                completeRequestBuilder().sourceRefs(List.of("ref-a", "ref-a")).build()
        );
        SourceTraceEntryPositiveCompletionContractDTO blank = assembler.assemble(
                completeRequestBuilder().sourceRefs(List.of(" ")).build()
        );
        SourceTraceEntryPositiveCompletionContractDTO ambiguous = assembler.assemble(
                completeRequestBuilder().sourceRefs(List.of("ref-a", "ref-b")).build()
        );

        assertUnsafe(empty, "emptySourceRefs");
        assertUnsafe(duplicate, "ambiguousSourceRefs", "duplicateSourceRefs");
        assertUnsafe(blank, "blankSourceRefs");
        assertUnsafe(ambiguous, "ambiguousSourceRefs");
    }

    @Test
    void liquidityEventMultiTimeframeAndWickGuardsFailClosed() {
        assertUnsafe(
                assembler.assemble(completeRequestBuilder().liquidityStress(true).build()),
                "liquidityStressRequiresReview"
        );
        assertUnsafe(
                assembler.assemble(completeRequestBuilder().liquidityStampede(true).build()),
                "liquidityStampedeRequiresReview"
        );
        assertUnsafe(
                assembler.assemble(completeRequestBuilder().eventDataMissing(true).build()),
                "eventDataMissing"
        );
        assertUnsafe(
                assembler.assemble(completeRequestBuilder().multiTimeframeAgreementOnly(true).build()),
                "multiTimeframeAgreementOnly"
        );
        assertUnsafe(
                assembler.assemble(completeRequestBuilder().wickOrPinBarEvidenceOnly(true).build()),
                "wickOrPinBarEvidenceOnly"
        );
    }

    @Test
    void trueConflictFlagsFailClosedOneAtATime() {
        assertUnsafe(assembler.assemble(completeRequestBuilder().conflictsWithStop(Boolean.TRUE).build()),
                "conflictsWithStop");
        assertUnsafe(assembler.assemble(completeRequestBuilder().conflictsWithTakeProfit(Boolean.TRUE).build()),
                "conflictsWithTakeProfit");
        assertUnsafe(assembler.assemble(completeRequestBuilder().conflictsWithRiskReward(Boolean.TRUE).build()),
                "conflictsWithRiskReward");
        assertUnsafe(assembler.assemble(completeRequestBuilder().conflictsWithLiquidity(Boolean.TRUE).build()),
                "conflictsWithLiquidity");
        assertUnsafe(assembler.assemble(completeRequestBuilder().conflictsWithMultiTimeframe(Boolean.TRUE).build()),
                "conflictsWithMultiTimeframe");
        assertUnsafe(assembler.assemble(completeRequestBuilder().conflictsWithEvent(Boolean.TRUE).build()),
                "conflictsWithEvent");
        assertUnsafe(assembler.assemble(completeRequestBuilder().conflictsWithWick(Boolean.TRUE).build()),
                "conflictsWithWick");
    }

    @Test
    void requestCopiesMutableReadOnlyEvidence() {
        List<String> mutableSourceRefs = new ArrayList<>();
        mutableSourceRefs.add("ref-a");
        List<String> mutableSourceTags = new ArrayList<>();
        mutableSourceTags.add("READ_ONLY_INTERNAL");

        SourceTraceEntryReadOnlyCompletionRequest request = completeRequestBuilder()
                .sourceRefs(mutableSourceRefs)
                .sourceTags(mutableSourceTags)
                .build();
        mutableSourceRefs.add("ref-b");
        mutableSourceTags.add("LATEST_PRICE_ONLY");

        assertThat(request.getSourceRefs()).containsExactly("ref-a");
        assertThat(request.getSourceTags()).containsExactly("READ_ONLY_INTERNAL");

        SourceTraceEntryPositiveCompletionContractDTO dto = assembler.assemble(request);

        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.COMPLETION_UNWIRED);
        assertStillNonProduction(dto);
    }

    @Test
    void assemblerSurfaceHasNoTradingOrProductionWiringShape() {
        assertNoForbiddenMethodNames(SourceTraceEntryReadOnlyCompletionAssembler.class);
        assertThat(SourceTraceEntryReadOnlyCompletionAssembler.class.getAnnotation(Service.class)).isNull();
        assertThat(SourceTraceEntryReadOnlyCompletionAssembler.class.getAnnotation(Component.class)).isNull();
        assertThat(SourceTraceEntryCompletionContract.class
                .isAssignableFrom(SourceTraceEntryReadOnlyCompletionAssembler.class)).isFalse();
        assertThat(SourceTraceEntryOwnershipAdapter.class
                .isAssignableFrom(SourceTraceEntryReadOnlyCompletionAssembler.class)).isFalse();
        assertThat(SourceTraceEntryOwnershipValidator.class
                .isAssignableFrom(SourceTraceEntryReadOnlyCompletionAssembler.class)).isFalse();
        assertThat(EntryCompletionValidationContextAssembler.class
                .isAssignableFrom(SourceTraceEntryReadOnlyCompletionAssembler.class)).isFalse();
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

    private void assertUnsafe(SourceTraceEntryPositiveCompletionContractDTO dto, String... expectedMissingFields) {
        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
        assertThat(dto.getDowngradeReason())
                .isEqualTo(SourceTraceEntryPositiveCompletionDowngradeReasonEnum.UNSAFE_COMPLETION);
        assertThat(dto.getMissingFields()).containsExactly(expectedMissingFields);
        assertStillNonProduction(dto);
    }

    private void assertMissing(SourceTraceEntryPositiveCompletionContractDTO dto, String expectedMissingField) {
        assertThat(dto.getCompletionStatus())
                .isEqualTo(SourceTraceEntryPositiveCompletionStatusEnum.INCOMPLETE);
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
