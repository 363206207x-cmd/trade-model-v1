package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Stream;
import org.example.trademodel.dto.planboundary.BoundaryCandidateFixtureAssemblerHelper.AssemblerStatus;
import org.example.trademodel.dto.planboundary.BoundaryCandidateFixtureAssemblerHelper.BoundaryCandidateFixture;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFamily;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixture;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixtureStatus;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.EntryStopDistanceState;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.FixtureStatus;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.RrFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.StopFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFamily;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFixture;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class SourceOwnedCandidateIncompleteGuardTest {

    private static final List<String> P146_MISSING_SOURCE_OWNED_EVIDENCE = List.of(
            "sourceOwner",
            "sourceRef",
            "sourceTimeframe",
            "sourceWindow",
            "observedTime",
            "decisionTimeRelationship",
            "freshness",
            "ohlcvKlineContext",
            "persistedOhlcvReadinessMetadata",
            "dataQualityScore",
            "dataQualityScoreOwner",
            "evidenceCompleteness",
            "sourceTraceCompleteness",
            "numericSourceOwnership",
            "entrySourceReason",
            "stopSourceReason",
            "tpSourceReason",
            "rrRuleRef",
            "ruleId",
            "ruleVersion",
            "conflictFamilyState",
            "liquidityEvidence",
            "multiTimeframeEvidence",
            "eventEvidenceStatus",
            "wickEvidenceStatus",
            "rollbackSafeEvidenceTrail",
            "runtimeSourceTracePopulatedFromSourceOwnedEvidence"
    );
    private static final List<String> FORBIDDEN_OUTPUT_SURFACE = List.of(
            "tradeReady",
            "readyToTrade",
            "readiness",
            "executable",
            "execute",
            "order",
            "execution",
            "automation",
            "autoTrading",
            "autoTrade"
    );

    private final MarketReadOnlyCandidateGenerator generator = new InertMarketReadOnlyCandidateGenerator();

    @TestFactory
    Stream<DynamicTest> missingSnapshotEvidenceFailsClosedToIncompleteCandidate() {
        return Stream.of(
                missingSnapshotCase("source owner", builder -> builder.sourceOwner(null), "sourceOwner"),
                missingSnapshotCase("source ref", builder -> builder.sourceRef(null), "sourceRef"),
                missingSnapshotCase("source timeframe", builder -> builder.sourceTimeframe(null), "sourceTimeframe"),
                missingSnapshotCase("source window", builder -> builder.sourceWindow(null), "sourceWindow"),
                missingSnapshotCase("freshness", builder -> builder.freshnessStatus(null), "freshnessStatus"),
                missingSnapshotCase("evidence completeness", builder -> builder.evidenceRefs(List.of()), "evidenceRefs"),
                missingSnapshotCase("source reason", builder -> builder.sourceReason(null), "sourceReason"),
                missingSnapshotCase("rule id", builder -> builder.ruleId(null), "ruleId"),
                missingSnapshotCase("rule version", builder -> builder.ruleVersion(null), "ruleVersion"),
                missingSnapshotCase(
                        "conflict family state",
                        builder -> builder.conflictFamilyStatus(null),
                        "conflictFamilyStatus"
                ),
                missingSnapshotCase(
                        "data quality score",
                        builder -> builder.dataQualityScore(null),
                        "dataQualityScore"
                ),
                missingSnapshotCase(
                        "liquidity evidence",
                        builder -> builder.liquidityEvidenceStatus(null),
                        "liquidityEvidenceStatus"
                ),
                missingSnapshotCase(
                        "multi-timeframe evidence",
                        builder -> builder.multiTimeframeEvidenceStatus(null),
                        "multiTimeframeEvidenceStatus"
                ),
                missingSnapshotCase(
                        "event evidence status",
                        builder -> builder.eventEvidenceStatus(null),
                        "eventEvidenceStatus"
                ),
                missingSnapshotCase(
                        "wick evidence status",
                        builder -> builder.wickPinBarEvidenceStatus(null),
                        "wickPinBarEvidenceStatus"
                )
        ).map(testCase -> DynamicTest.dynamicTest(testCase.name(), () -> {
            MarketReadOnlyEvidenceSnapshotDTO snapshot = testCase.mutator()
                    .apply(sourceOwnedSnapshotBuilder())
                    .build();
            MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

            assertThat(snapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.INCOMPLETE);
            assertThat(snapshot.getMissingFields()).contains(testCase.missingField());
            assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
            assertThat(result.getCandidateStatus()).isNotEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
            assertThat(result.getCandidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
            assertThat(result.getBlockingReasons()).contains("snapshot_missing:" + testCase.missingField());
            assertReviewOnly(result);
            assertNoActionSurface(MarketReadOnlyCandidateResultDTO.class);
        }));
    }

    @Test
    void missingRuntimeSourceTraceAuditKeepsBoundarySourcesIncomplete() {
        SourceTraceDTO sourceTrace = new SourceTraceDTO();
        sourceTrace.setFallbackStatus(SourceTraceFallbackStatusEnum.INCOMPLETE);
        sourceTrace.setMissingFields(P146_MISSING_SOURCE_OWNED_EVIDENCE);

        assertThat(sourceTrace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(sourceTrace.isComplete()).isFalse();
        assertThat(sourceTrace.hasRequiredBoundarySources()).isFalse();
        assertThat(sourceTrace.getMissingFields()).containsExactlyElementsOf(P146_MISSING_SOURCE_OWNED_EVIDENCE);
        assertThat(sourceTrace.getEntryPriceSource()).isNull();
        assertThat(sourceTrace.getStopPriceSource()).isNull();
        assertThat(sourceTrace.getTpPriceSources()).isEmpty();
        assertThat(sourceTrace.getRrSource()).isNull();
        assertThat(sourceTrace.isManualReviewRequired()).isTrue();
        assertThat(sourceTrace.isNotTradeInstruction()).isTrue();
    }

    @Test
    void incompleteSourceOwnedFixtureDependenciesStayIncompleteAndReviewOnly() {
        EntryFixture entry = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                .missingSourceOwner()
                .missingNumericSource()
                .staleSourceWindow()
                .build();
        StopFixture stop = StopTpRrSourceOwnedCandidateFixtureHelper.stop()
                .missingStopOwner()
                .missingNumericSource()
                .staleSourceWindow()
                .build();
        TpFixture tp = StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                .missingTpOwner()
                .missingNumericSource()
                .staleSourceWindow()
                .build();
        RrFixture rr = StopTpRrSourceOwnedCandidateFixtureHelper.rr(entry, stop, tp)
                .missingRrOwner()
                .missingNumericSource()
                .entryStopDistance(EntryStopDistanceState.MISSING)
                .staleSourceWindow()
                .build();

        BoundaryCandidateFixture output = BoundaryCandidateFixtureAssemblerHelper.assemble(entry, stop, tp, rr);

        assertThat(entry.fixtureStatus()).isEqualTo(EntryFixtureStatus.INCOMPLETE);
        assertThat(stop.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(tp.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(rr.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(output.assemblerStatus()).isEqualTo(AssemblerStatus.INCOMPLETE);
        assertThat(output.assemblerStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(output.blockerEvidence()).contains(
                "incomplete_entry_dependency",
                "missing_source_owner",
                "incomplete_stop_dependency",
                "missing_stop_owner",
                "incomplete_tp_dependency",
                "missing_tp_owner",
                "incomplete_rr_dependency",
                "missing_rr_owner",
                "missing_numeric_source",
                "missing_entry_stop_distance",
                "stale_source_window"
        );
        assertThat(output.numericSourceTokenSummary().entryNumericSourceToken()).isNull();
        assertThat(output.numericSourceTokenSummary().stopNumericSourceToken()).isNull();
        assertThat(output.numericSourceTokenSummary().tpNumericSourceToken()).isNull();
        assertThat(output.numericSourceTokenSummary().rrNumericSourceToken()).isNull();
        assertBoundaryReviewOnly(output);
        assertNoActionSurface(BoundaryCandidateFixture.class);
        assertNoActionSurface(BoundaryCandidateFixtureAssemblerHelper.ReviewField.class);
    }

    private MarketReadOnlyEvidenceSnapshotDTO.Builder sourceOwnedSnapshotBuilder() {
        return MarketReadOnlyEvidenceSnapshotDTO.builder()
                .symbol("BTCUSDT")
                .timeframe("1h")
                .evidenceRefs(List.of("source-owned-evidence-ref"))
                .evidenceFamilies(List.of(
                        MarketReadOnlyEvidenceFamilyEnum.MARKET_STRUCTURE,
                        MarketReadOnlyEvidenceFamilyEnum.KLINE_DERIVED_STRUCTURE,
                        MarketReadOnlyEvidenceFamilyEnum.ATR_VOLATILITY,
                        MarketReadOnlyEvidenceFamilyEnum.LIQUIDITY_TARGET,
                        MarketReadOnlyEvidenceFamilyEnum.PRIOR_HIGH_LOW,
                        MarketReadOnlyEvidenceFamilyEnum.EVENT,
                        MarketReadOnlyEvidenceFamilyEnum.WICK_PIN_BAR,
                        MarketReadOnlyEvidenceFamilyEnum.MULTI_TIMEFRAME
                ))
                .sourceOwner("source-owned-snapshot")
                .sourceRef("source-owned-ref")
                .sourceTimeframe("1h")
                .sourceReason("source-owned review evidence")
                .sourceWindow("source-owned-window")
                .ruleId("source-owned-incomplete-guard")
                .ruleVersion("p146.v1")
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.FRESH)
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CLEAR)
                .dataQualityScore(80)
                .eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .riskActionGuardContext("review_only_no_direct_action");
    }

    private void assertReviewOnly(MarketReadOnlyCandidateResultDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
    }

    private void assertBoundaryReviewOnly(BoundaryCandidateFixture output) {
        assertThat(output.manualReviewRequired()).isTrue();
        assertThat(output.notTradeInstruction()).isTrue();
        assertThat(output.reviewMode()).isEqualTo(BoundaryCandidateFixtureAssemblerHelper.REVIEW_ONLY);
    }

    private void assertNoActionSurface(Class<?> type) {
        assertThat(Stream.of(type.getDeclaredMethods())
                        .map(Method::getName))
                .noneMatch(this::containsForbiddenSurfaceToken);
    }

    private boolean containsForbiddenSurfaceToken(String surfaceName) {
        String normalizedSurfaceName = surfaceName.toLowerCase(Locale.ROOT);
        return FORBIDDEN_OUTPUT_SURFACE.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedSurfaceName::contains);
    }

    private MissingSnapshotCase missingSnapshotCase(
            String name,
            Function<MarketReadOnlyEvidenceSnapshotDTO.Builder, MarketReadOnlyEvidenceSnapshotDTO.Builder> mutator,
            String missingField
    ) {
        return new MissingSnapshotCase(name, mutator, missingField);
    }

    private record MissingSnapshotCase(
            String name,
            Function<MarketReadOnlyEvidenceSnapshotDTO.Builder, MarketReadOnlyEvidenceSnapshotDTO.Builder> mutator,
            String missingField
    ) {
    }
}
