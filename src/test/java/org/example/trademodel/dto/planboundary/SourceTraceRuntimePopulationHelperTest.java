package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SourceTraceRuntimePopulationHelperTest {

    private static final List<String> FORBIDDEN_ACTION_SURFACE_TOKENS = List.of(
            "order",
            "execution",
            "automation",
            "autoTrading",
            "autoTrade"
    );

    private final MarketReadOnlyCandidateGenerator generator = new InertMarketReadOnlyCandidateGenerator();

    @Test
    void readOnlyEvidenceSnapshotMapsToReviewOnlySourceTrace() {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = completeSnapshotBuilder().build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        SourceTraceDTO trace = SourceTraceRuntimePopulationHelper.populate(snapshot, result);

        assertThat(trace.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(trace.getSymbolSource()).isEqualTo("market_read_only_snapshot.symbol");
        assertThat(trace.getTimeframe()).isEqualTo("1h");
        assertThat(trace.getTimeframeSource()).isEqualTo("market_read_only_snapshot.timeframe");
        assertThat(trace.getSourceOwner()).isEqualTo("p158-runtime-source-owner");
        assertThat(trace.getSourceRef()).isEqualTo("p158-runtime-source-ref");
        assertThat(trace.getSourceTimeframe()).isEqualTo("1h");
        assertThat(trace.getSourceWindow()).isEqualTo("p158-runtime-source-window");
        assertThat(trace.getFreshnessStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.FRESH.name());
        assertThat(trace.getQuoteFreshnessStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.FRESH.name());
        assertThat(trace.getMissingFields()).isEmpty();
        assertThat(trace.getBlockingReasons()).isEmpty();
        assertThat(trace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertReviewOnly(trace, result);
        assertNoReadinessOrRealTradeBoundary(trace);
    }

    @Test
    void missingEvidenceStaysIncompleteAndCannotBecomeValidReadyOrTradeInstruction() {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = completeSnapshotBuilder()
                .sourceOwner(null)
                .sourceRef(null)
                .sourceTimeframe(null)
                .sourceWindow(null)
                .build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        SourceTraceDTO trace = SourceTraceRuntimePopulationHelper.populate(snapshot, result);

        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(trace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(trace.getSourceOwner()).isNull();
        assertThat(trace.getSourceRef()).isNull();
        assertThat(trace.getSourceTimeframe()).isNull();
        assertThat(trace.getSourceWindow()).isNull();
        assertThat(trace.getMissingFields())
                .contains("sourceOwner", "sourceRef", "sourceTimeframe", "sourceWindow");
        assertThat(trace.getBlockingReasons()).contains(
                "snapshot_missing:sourceOwner",
                "snapshot_missing:sourceRef",
                "snapshot_missing:sourceTimeframe",
                "snapshot_missing:sourceWindow"
        );
        assertReviewOnly(trace, result);
        assertNoReadinessOrRealTradeBoundary(trace);
    }

    @Test
    void conflictingUnsafeAndStaleUnsafeEvidenceStayBlockedAndFailClosed() {
        List<BlockedCase> blockedCases = List.of(
                new BlockedCase(
                        "conflicting evidence",
                        completeSnapshotBuilder().conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CONFLICT).build(),
                        "snapshot_blocked:conflictFamilyStatus:CONFLICT"
                ),
                new BlockedCase(
                        "unsafe evidence",
                        completeSnapshotBuilder().forbiddenInputMarkers(List.of("latest_price_only")).build(),
                        "direct_forbidden_input:latest_price_only"
                ),
                new BlockedCase(
                        "stale unsafe source window",
                        completeSnapshotBuilder()
                                .sourceWindow("p158-stale-unsafe-source-window")
                                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.STALE)
                                .riskActionGuardBlockers(List.of("stale_unsafe_source_window"))
                                .build(),
                        "direct_risk_action_guard:stale_unsafe_source_window"
                )
        );

        for (BlockedCase blockedCase : blockedCases) {
            MarketReadOnlyCandidateResultDTO result = generator.review(blockedCase.snapshot());
            SourceTraceDTO trace = SourceTraceRuntimePopulationHelper.populate(blockedCase.snapshot(), result);

            assertThat(result.getCandidateStatus())
                    .as(blockedCase.name())
                    .isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
            assertThat(result.getCandidateStatus().name())
                    .as(blockedCase.name())
                    .isNotEqualTo(BoundaryStatusEnum.VALID.name());
            assertThat(trace.getFallbackStatus())
                    .as(blockedCase.name())
                    .isEqualTo(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
            assertThat(trace.getBlockingReasons())
                    .as(blockedCase.name())
                    .contains(blockedCase.expectedBlockingReason());
            assertReviewOnly(trace, result);
            assertNoReadinessOrRealTradeBoundary(trace);
        }
    }

    @Test
    void helperDoesNotExposeProductionValidActionOrAutomationSurface() throws Exception {
        String helperSource = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/dto/planboundary/SourceTraceRuntimePopulationHelper.java"
        ));

        assertThat(helperSource).doesNotContain("BoundaryCandidateDTO.valid");
        assertThat(helperSource).doesNotContain("BoundaryStatusEnum.VALID");
        assertNoOrderExecutionAutomationSurface(SourceTraceRuntimePopulationHelper.class);
        assertNoOrderExecutionAutomationSurface(SourceTraceDTO.class);
    }

    @Test
    void nullInputsFailClosedToIncompleteReviewOnlySourceTrace() {
        SourceTraceDTO trace = SourceTraceRuntimePopulationHelper.populate(null, null);

        assertThat(trace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(trace.getMissingFields()).containsExactly("snapshot");
        assertThat(trace.getBlockingReasons()).containsExactly("missing_snapshot");
        assertThat(trace.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name());
        assertThat(trace.isManualReviewRequired()).isTrue();
        assertThat(trace.isNotTradeInstruction()).isTrue();
        assertNoReadinessOrRealTradeBoundary(trace);
    }

    private MarketReadOnlyEvidenceSnapshotDTO.Builder completeSnapshotBuilder() {
        return MarketReadOnlyEvidenceSnapshotDTO.builder()
                .symbol("BTCUSDT")
                .timeframe("1h")
                .evidenceRefs(List.of(
                        "p158-source-trace-owner-ref",
                        "p158-source-trace-window-ref",
                        "p158-source-trace-freshness-ref"
                ))
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
                .sourceOwner("p158-runtime-source-owner")
                .sourceRef("p158-runtime-source-ref")
                .sourceTimeframe("1h")
                .sourceReason("p158 review-only SourceTrace runtime population helper")
                .sourceWindow("p158-runtime-source-window")
                .ruleId("p158-source-trace-runtime-population-helper")
                .ruleVersion("p158.v1")
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.FRESH)
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CLEAR)
                .dataQualityScore(80)
                .eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .riskActionGuardContext("review_only_no_direct_action");
    }

    private void assertReviewOnly(SourceTraceDTO trace, MarketReadOnlyCandidateResultDTO result) {
        assertThat(trace.isManualReviewRequired()).isTrue();
        assertThat(trace.isNotTradeInstruction()).isTrue();
        assertThat(trace.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name());
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
    }

    private void assertNoReadinessOrRealTradeBoundary(SourceTraceDTO trace) {
        assertThat(trace.getRuntimeKlineReadinessStatus()).isNull();
        assertThat(trace.getRuntimeKlineReadinessMissingFields()).isEmpty();
        assertThat(trace.getEntryPriceSource()).isNull();
        assertThat(trace.getStopPriceSource()).isNull();
        assertThat(trace.getTpPriceSources()).isEmpty();
        assertThat(trace.getRrSource()).isNull();
        assertThat(trace.hasRequiredBoundarySources()).isFalse();
    }

    private void assertNoOrderExecutionAutomationSurface(Class<?> type) {
        assertThat(Stream.of(type.getDeclaredFields())
                        .map(Field::getName))
                .noneMatch(this::containsForbiddenActionSurfaceToken);
        assertThat(Stream.of(type.getDeclaredMethods())
                        .map(Method::getName))
                .noneMatch(this::containsForbiddenActionSurfaceToken);
    }

    private boolean containsForbiddenActionSurfaceToken(String surfaceName) {
        String normalizedSurfaceName = surfaceName.toLowerCase(Locale.ROOT);
        return FORBIDDEN_ACTION_SURFACE_TOKENS.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedSurfaceName::contains);
    }

    private record BlockedCase(
            String name,
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            String expectedBlockingReason
    ) {
    }
}
