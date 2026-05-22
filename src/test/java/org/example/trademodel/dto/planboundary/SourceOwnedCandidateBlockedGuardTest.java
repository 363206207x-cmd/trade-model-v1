package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.example.trademodel.dto.planboundary.BoundaryCandidateFixtureAssemblerHelper.AssemblerStatus;
import org.example.trademodel.dto.planboundary.BoundaryCandidateFixtureAssemblerHelper.BoundaryCandidateFixture;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFamily;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixture;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixtureStatus;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.ForbiddenSource;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.FixtureStatus;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.RrFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.StopFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFamily;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFixture;
import org.junit.jupiter.api.Test;

class SourceOwnedCandidateBlockedGuardTest {

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

    @Test
    void conflictingSourceOwnershipBlocksCandidatePromotion() {
        StopFixture blockedStop = StopTpRrSourceOwnedCandidateFixtureHelper.stop()
                .entryStopInversion()
                .build();

        BoundaryCandidateFixture output = BoundaryCandidateFixtureAssemblerHelper.assemble(
                validEntry(),
                blockedStop,
                validTp(),
                validRr(validEntry(), validStop(), validTp())
        );

        assertThat(blockedStop.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(blockedStop.fixtureStatus()).isNotEqualTo(FixtureStatus.INCOMPLETE);
        assertBlockedBoundary(output, "blocked_stop_dependency", "entry_stop_inversion");
    }

    @Test
    void staleSourceWindowWithUnsafeEvidenceBlocksCandidatePromotion() {
        EntryFixture blockedEntry = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                .staleUnsafeSourceWindow()
                .build();

        BoundaryCandidateFixture output = BoundaryCandidateFixtureAssemblerHelper.assemble(
                blockedEntry,
                validStop(),
                validTp(),
                validRr(validEntry(), validStop(), validTp())
        );

        assertThat(blockedEntry.fixtureStatus()).isEqualTo(EntryFixtureStatus.BLOCKED);
        assertThat(blockedEntry.fixtureStatus()).isNotEqualTo(EntryFixtureStatus.INCOMPLETE);
        assertBlockedBoundary(
                output,
                "blocked_entry_dependency",
                "stale_source_window",
                "unsafe_stale_source_window"
        );
    }

    @Test
    void unsafeSubstitutionBlocksCandidatePromotion() {
        EntryFixture blockedEntry = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.BREAKOUT_RETEST_ZONE)
                .forbiddenSource(ForbiddenSource.LATEST_PRICE_ONLY)
                .build();

        BoundaryCandidateFixture output = BoundaryCandidateFixtureAssemblerHelper.assemble(
                blockedEntry,
                validStop(),
                validTp(),
                validRr(validEntry(), validStop(), validTp())
        );

        assertThat(blockedEntry.fixtureStatus()).isEqualTo(EntryFixtureStatus.BLOCKED);
        assertThat(blockedEntry.fixtureStatus()).isNotEqualTo(EntryFixtureStatus.INCOMPLETE);
        assertBlockedBoundary(output, "blocked_entry_dependency", "forbidden_source", "latest_price_only");
    }

    @Test
    void explicitBlockedStatusPreservesFailClosedOutput() {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = sourceOwnedSnapshotBuilder()
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.BLOCKED)
                .build();

        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        assertThat(snapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.BLOCKED);
        assertThat(snapshot.getSnapshotStatus()).isNotEqualTo(MarketReadOnlySnapshotStatusEnum.INCOMPLETE);
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
        assertThat(result.getCandidateStatus()).isNotEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertThat(result.getCandidateStatus()).isNotEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(result.getBlockingReasons()).contains("snapshot_blocked:conflictFamilyStatus:BLOCKED");
        assertReviewOnly(result);
        assertNoActionSurface(MarketReadOnlyCandidateResultDTO.class);
    }

    private void assertBlockedBoundary(BoundaryCandidateFixture output, String... expectedEvidence) {
        assertThat(output.assemblerStatus()).isEqualTo(AssemblerStatus.BLOCKED);
        assertThat(output.assemblerStatus()).isNotEqualTo(AssemblerStatus.INCOMPLETE);
        assertThat(output.assemblerStatus()).isNotEqualTo(AssemblerStatus.FIXTURE_VALID_CANDIDATE);
        assertThat(output.assemblerStatus().name())
                .isNotEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE.name());
        assertThat(output.assemblerStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(output.blockerEvidence()).contains(expectedEvidence);
        assertBoundaryReviewOnly(output);
        assertNoActionSurface(BoundaryCandidateFixture.class);
        assertNoActionSurface(BoundaryCandidateFixtureAssemblerHelper.ReviewField.class);
        assertThat(output.boundaryCandidateStyleFieldNames()).noneMatch(this::containsForbiddenSurfaceToken);
    }

    private MarketReadOnlyEvidenceSnapshotDTO.Builder sourceOwnedSnapshotBuilder() {
        return MarketReadOnlyEvidenceSnapshotDTO.builder()
                .symbol("BTCUSDT")
                .timeframe("1h")
                .evidenceRefs(List.of("source-owned-blocked-evidence-ref"))
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
                .sourceOwner("source-owned-blocked-snapshot")
                .sourceRef("source-owned-blocked-ref")
                .sourceTimeframe("1h")
                .sourceReason("source-owned blocked review evidence")
                .sourceWindow("source-owned-blocked-window")
                .ruleId("source-owned-blocked-guard")
                .ruleVersion("p149.v1")
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.FRESH)
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CLEAR)
                .dataQualityScore(80)
                .eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .riskActionGuardContext("review_only_no_direct_action");
    }

    private EntryFixture validEntry() {
        return EntrySourceOwnedCandidateFixtureHelper.completeFixture(EntryFamily.STRUCTURE_CONFIRMATION_ZONE);
    }

    private StopFixture validStop() {
        return StopTpRrSourceOwnedCandidateFixtureHelper.completeStopFixture();
    }

    private TpFixture validTp() {
        return StopTpRrSourceOwnedCandidateFixtureHelper.completeTpFixture(TpFamily.STRUCTURE_TARGET);
    }

    private RrFixture validRr(EntryFixture entry, StopFixture stop, TpFixture tp) {
        return StopTpRrSourceOwnedCandidateFixtureHelper.rr(entry, stop, tp).build();
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
}
