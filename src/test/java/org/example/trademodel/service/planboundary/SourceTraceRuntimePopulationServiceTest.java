package org.example.trademodel.service.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.example.trademodel.dto.planboundary.BoundaryStatusEnum;
import org.example.trademodel.dto.planboundary.InertMarketReadOnlyCandidateGenerator;
import org.example.trademodel.dto.planboundary.MarketReadOnlyCandidateGenerator;
import org.example.trademodel.dto.planboundary.MarketReadOnlyCandidateResultDTO;
import org.example.trademodel.dto.planboundary.MarketReadOnlyCandidateStatusEnum;
import org.example.trademodel.dto.planboundary.MarketReadOnlyEvidenceFamilyEnum;
import org.example.trademodel.dto.planboundary.MarketReadOnlyEvidenceSnapshotDTO;
import org.example.trademodel.dto.planboundary.MarketReadOnlyEvidenceStatusEnum;
import org.example.trademodel.dto.planboundary.SourceTraceDTO;
import org.example.trademodel.dto.planboundary.SourceTraceEntrySourceReviewModeEnum;
import org.example.trademodel.dto.planboundary.SourceTraceFallbackStatusEnum;
import org.junit.jupiter.api.Test;

class SourceTraceRuntimePopulationServiceTest {

    private static final List<String> FORBIDDEN_ACTION_SURFACE_TOKENS = List.of(
            "order",
            "execution",
            "automation",
            "autoTrading",
            "autoTrade"
    );

    private final SourceTraceRuntimePopulationService service = new SourceTraceRuntimePopulationServiceImpl();
    private final MarketReadOnlyCandidateGenerator generator = new InertMarketReadOnlyCandidateGenerator();

    @Test
    void serviceDelegatesReadOnlySnapshotToHelperAndReturnsSourceTrace() {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = completeSnapshotBuilder().build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        SourceTraceDTO trace = service.populate(snapshot, result);

        assertThat(trace.getSourceOwner()).isEqualTo("p162-service-source-owner");
        assertThat(trace.getSourceRef()).isEqualTo("p162-service-source-ref");
        assertThat(trace.getSourceTimeframe()).isEqualTo("1h");
        assertThat(trace.getSourceWindow()).isEqualTo("p162-service-source-window");
        assertThat(trace.getFreshnessStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.FRESH.name());
        assertThat(trace.getQuoteFreshnessStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.FRESH.name());
        assertThat(trace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.WATCH_ONLY);
        assertThat(trace.getMissingFields()).isEmpty();
        assertThat(trace.getBlockingReasons()).isEmpty();
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertNotValid(result);
        assertReviewOnly(trace);
        assertNoReadinessOrRealTradeBoundary(trace);
    }

    @Test
    void missingEvidenceStaysIncompleteThroughServiceWrapper() {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = completeSnapshotBuilder()
                .sourceOwner(null)
                .sourceRef(null)
                .sourceTimeframe(null)
                .sourceWindow(null)
                .build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        SourceTraceDTO trace = service.populate(snapshot, result);

        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertNotValid(result);
        assertThat(trace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(trace.getMissingFields())
                .contains("sourceOwner", "sourceRef", "sourceTimeframe", "sourceWindow");
        assertThat(trace.getBlockingReasons()).contains(
                "snapshot_missing:sourceOwner",
                "snapshot_missing:sourceRef",
                "snapshot_missing:sourceTimeframe",
                "snapshot_missing:sourceWindow"
        );
        assertReviewOnly(trace);
        assertNoReadinessOrRealTradeBoundary(trace);
    }

    @Test
    void conflictingAndUnsafeEvidenceStayBlockedThroughServiceWrapper() {
        List<BlockedCase> blockedCases = List.of(
                new BlockedCase(
                        completeSnapshotBuilder()
                                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CONFLICT)
                                .build(),
                        "snapshot_blocked:conflictFamilyStatus:CONFLICT"
                ),
                new BlockedCase(
                        completeSnapshotBuilder()
                                .forbiddenInputMarkers(List.of("latest_price_only"))
                                .build(),
                        "direct_forbidden_input:latest_price_only"
                ),
                new BlockedCase(
                        completeSnapshotBuilder()
                                .sourceWindow("p162-stale-unsafe-source-window")
                                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.STALE)
                                .riskActionGuardBlockers(List.of("stale_unsafe_source_window"))
                                .build(),
                        "direct_risk_action_guard:stale_unsafe_source_window"
                )
        );

        for (BlockedCase blockedCase : blockedCases) {
            MarketReadOnlyCandidateResultDTO result = generator.review(blockedCase.snapshot());
            SourceTraceDTO trace = service.populate(blockedCase.snapshot(), result);

            assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
            assertNotValid(result);
            assertThat(trace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
            assertThat(trace.getBlockingReasons()).contains(blockedCase.expectedBlockingReason());
            assertReviewOnly(trace);
            assertNoReadinessOrRealTradeBoundary(trace);
        }
    }

    @Test
    void nullInputFailsClosedToIncompleteReviewOnlySourceTrace() {
        SourceTraceDTO trace = service.populate(null, null);

        assertThat(trace.getFallbackStatus()).isEqualTo(SourceTraceFallbackStatusEnum.INCOMPLETE);
        assertThat(trace.getMissingFields()).containsExactly("snapshot");
        assertThat(trace.getBlockingReasons()).containsExactly("missing_snapshot");
        assertReviewOnly(trace);
        assertNoReadinessOrRealTradeBoundary(trace);
    }

    @Test
    void serviceWrapperDoesNotExposeValidReadinessOrActionSurface() throws Exception {
        String serviceSource = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationService.java"
        ));
        String implSource = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/service/planboundary/SourceTraceRuntimePopulationServiceImpl.java"
        ));

        assertThat(serviceSource).doesNotContain("BoundaryCandidateDTO" + ".valid");
        assertThat(implSource).doesNotContain("BoundaryCandidateDTO" + ".valid");
        assertThat(serviceSource).doesNotContain("BoundaryStatusEnum.VALID");
        assertThat(implSource).doesNotContain("BoundaryStatusEnum.VALID");
        assertNoOrderExecutionAutomationSurface(SourceTraceRuntimePopulationService.class);
        assertNoOrderExecutionAutomationSurface(SourceTraceRuntimePopulationServiceImpl.class);
    }

    private MarketReadOnlyEvidenceSnapshotDTO.Builder completeSnapshotBuilder() {
        return MarketReadOnlyEvidenceSnapshotDTO.builder()
                .symbol("BTCUSDT")
                .timeframe("1h")
                .evidenceRefs(List.of(
                        "p162-service-source-owner-ref",
                        "p162-service-source-window-ref",
                        "p162-service-freshness-ref"
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
                .sourceOwner("p162-service-source-owner")
                .sourceRef("p162-service-source-ref")
                .sourceTimeframe("1h")
                .sourceReason("p162 review-only SourceTrace service minimal wiring")
                .sourceWindow("p162-service-source-window")
                .ruleId("p162-source-trace-service-minimal-wiring")
                .ruleVersion("p162.v1")
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.FRESH)
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CLEAR)
                .dataQualityScore(80)
                .eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .riskActionGuardContext("review_only_no_direct_action");
    }

    private void assertNotValid(MarketReadOnlyCandidateResultDTO result) {
        assertThat(result.getCandidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
    }

    private void assertReviewOnly(SourceTraceDTO trace) {
        assertThat(trace.isManualReviewRequired()).isTrue();
        assertThat(trace.isNotTradeInstruction()).isTrue();
        assertThat(trace.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY.name());
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
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            String expectedBlockingReason
    ) {
    }
}
