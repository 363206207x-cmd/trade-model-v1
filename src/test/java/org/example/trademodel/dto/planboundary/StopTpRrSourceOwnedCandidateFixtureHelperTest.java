package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFamily;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.EntryStopDistanceState;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.FixtureStatus;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.ForbiddenSource;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.NumericSource;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.RiskActionGuardBlocker;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.RrFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.StopFamily;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.StopFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFamily;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFixture;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class StopTpRrSourceOwnedCandidateFixtureHelperTest {

    private static final List<String> FORBIDDEN_OUTPUT_SURFACE = List.of(
            "tradeReady",
            "readyToTrade",
            "order",
            "execution",
            "automation",
            "autoTrading"
    );

    @Test
    void fixtureValidStopSupportsStructuralInvalidationWithBufferAsReviewOnlyFixtureOutput() {
        StopFixture fixture = StopTpRrSourceOwnedCandidateFixtureHelper.completeStopFixture();

        assertThat(fixture.fixtureStatus()).isEqualTo(FixtureStatus.FIXTURE_VALID_CANDIDATE);
        assertThat(fixture.fixtureStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(fixture.stopCandidateFamily()).isEqualTo(StopFamily.STRUCTURAL_INVALIDATION_WITH_BUFFER.name());
        assertThat(fixture.stopSourceOwner()).isNotBlank();
        assertThat(fixture.numericSource()).isNotNull();
        assertThat(fixture.numericSource().fixtureOnly()).isTrue();
        assertThat(fixture.numericSource().marketDerived()).isFalse();
        assertThat(fixture.numericSource().valueToken()).startsWith("fixture-stop-token:");
        assertMandatoryReviewOnlyFlags(fixture.manualReviewRequired(), fixture.notTradeInstruction(), fixture.reviewMode());
        assertThat(fixture.blockerEvidence()).isEmpty();
    }

    @TestFactory
    Stream<DynamicTest> fixtureValidTpSupportsAllTpFamiliesAsReviewOnlyFixtureOutput() {
        return Arrays.stream(TpFamily.values())
                .map(family -> DynamicTest.dynamicTest(family.name(), () -> {
                    TpFixture fixture = StopTpRrSourceOwnedCandidateFixtureHelper.completeTpFixture(family);

                    assertThat(fixture.fixtureStatus()).isEqualTo(FixtureStatus.FIXTURE_VALID_CANDIDATE);
                    assertThat(fixture.fixtureStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
                    assertThat(fixture.tpCandidateFamily()).isEqualTo(family.name());
                    assertThat(fixture.tpSourceOwner()).isNotBlank();
                    assertThat(fixture.numericSource()).isNotNull();
                    assertThat(fixture.numericSource().fixtureOnly()).isTrue();
                    assertThat(fixture.numericSource().marketDerived()).isFalse();
                    assertThat(fixture.numericSource().valueToken()).startsWith("fixture-tp-token:");
                    assertMandatoryReviewOnlyFlags(fixture.manualReviewRequired(), fixture.notTradeInstruction(),
                            fixture.reviewMode());
                    assertThat(fixture.blockerEvidence()).isEmpty();
                }));
    }

    @Test
    void fixtureValidRrRequiresEntryStopAndTpDependenciesAsReviewOnlyFixtureOutput() {
        RrFixture fixture = StopTpRrSourceOwnedCandidateFixtureHelper.completeRrFixture();

        assertThat(fixture.fixtureStatus()).isEqualTo(FixtureStatus.FIXTURE_VALID_CANDIDATE);
        assertThat(fixture.fixtureStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(fixture.entryFixtureDependency()).isNotNull();
        assertThat(fixture.stopFixtureDependency()).isNotNull();
        assertThat(fixture.tpFixtureDependency()).isNotNull();
        assertThat(fixture.rrSourceOwner()).isNotBlank();
        assertThat(fixture.entryStopDistanceState()).isEqualTo(EntryStopDistanceState.PRESENT);
        assertThat(fixture.numericSource().fixtureOnly()).isTrue();
        assertThat(fixture.numericSource().marketDerived()).isFalse();
        assertThat(fixture.numericSource().valueToken()).startsWith("fixture-rr-token:");
        assertMandatoryReviewOnlyFlags(fixture.manualReviewRequired(), fixture.notTradeInstruction(), fixture.reviewMode());
        assertThat(fixture.blockerEvidence()).isEmpty();
    }

    @Test
    void missingOwnersFailIncompleteWithBlockerEvidence() {
        StopFixture stop = StopTpRrSourceOwnedCandidateFixtureHelper.stop().missingStopOwner().build();
        TpFixture tp = StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET).missingTpOwner().build();
        RrFixture rr = StopTpRrSourceOwnedCandidateFixtureHelper.rr(entryFixture(), validStop(), validTp())
                .missingRrOwner()
                .build();

        assertThat(stop.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(stop.blockerEvidence()).contains("stopSourceOwner", "missing_stop_owner");
        assertMandatoryReviewOnlyFlags(stop.manualReviewRequired(), stop.notTradeInstruction(), stop.reviewMode());

        assertThat(tp.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(tp.blockerEvidence()).contains("tpSourceOwner", "missing_tp_owner");
        assertMandatoryReviewOnlyFlags(tp.manualReviewRequired(), tp.notTradeInstruction(), tp.reviewMode());

        assertThat(rr.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(rr.blockerEvidence()).contains("rrSourceOwner", "missing_rr_owner");
        assertMandatoryReviewOnlyFlags(rr.manualReviewRequired(), rr.notTradeInstruction(), rr.reviewMode());
    }

    @Test
    void missingNumericSourcesFailIncompleteWithBlockerEvidence() {
        StopFixture stop = StopTpRrSourceOwnedCandidateFixtureHelper.stop().missingNumericSource().build();
        TpFixture tp = StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.LIQUIDITY_TARGET)
                .missingNumericSource()
                .build();
        RrFixture rr = StopTpRrSourceOwnedCandidateFixtureHelper.rr(entryFixture(), validStop(), validTp())
                .missingNumericSource()
                .build();

        assertThat(stop.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(stop.blockerEvidence()).contains("stopNumericSource", "missing_numeric_source");
        assertThat(tp.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(tp.blockerEvidence()).contains("tpNumericSource", "missing_numeric_source");
        assertThat(rr.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(rr.blockerEvidence()).contains("rrNumericSource", "missing_numeric_source");
    }

    @Test
    void staleSourceWindowFailsIncompleteOrBlockedDependingOnUnsafeEvidence() {
        StopFixture staleStop = StopTpRrSourceOwnedCandidateFixtureHelper.stop().staleSourceWindow().build();
        TpFixture unsafeStaleTp = StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.PRIOR_HIGH_LOW)
                .staleUnsafeSourceWindow()
                .build();
        RrFixture staleRr = StopTpRrSourceOwnedCandidateFixtureHelper.rr(entryFixture(), validStop(), validTp())
                .staleSourceWindow()
                .build();

        assertThat(staleStop.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(staleStop.blockerEvidence()).contains("stopSourceWindow", "stale_source_window");
        assertThat(unsafeStaleTp.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(unsafeStaleTp.blockerEvidence()).contains("tpSourceWindow", "unsafe_stale_source_window");
        assertThat(staleRr.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(staleRr.blockerEvidence()).contains("rrSourceWindow", "stale_source_window");
    }

    @Test
    void unsupportedSourceFamiliesFailBlockedWithFieldSpecificEvidence() {
        StopFixture stop = StopTpRrSourceOwnedCandidateFixtureHelper.stop()
                .unsupportedSourceFamily("LATEST_PRICE_STOP")
                .build();
        TpFixture tp = StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.RR_LADDER)
                .unsupportedSourceFamily("DASHBOARD_TP")
                .build();

        assertThat(stop.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(stop.blockerEvidence()).contains(
                "stopCandidateFamily",
                "unsupported_source_family",
                "unsupported_source_family:LATEST_PRICE_STOP"
        );
        assertThat(tp.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(tp.blockerEvidence()).contains(
                "tpCandidateFamily",
                "unsupported_source_family",
                "unsupported_source_family:DASHBOARD_TP"
        );
    }

    @Test
    void rrFailsClosedWhenDependenciesAreMissingIncompleteOrBlocked() {
        RrFixture missingEntry = StopTpRrSourceOwnedCandidateFixtureHelper.rr(null, validStop(), validTp()).build();
        RrFixture missingStop = StopTpRrSourceOwnedCandidateFixtureHelper.rr(entryFixture(), null, validTp()).build();
        RrFixture missingTp = StopTpRrSourceOwnedCandidateFixtureHelper.rr(entryFixture(), validStop(), null).build();
        RrFixture incompleteEntry = StopTpRrSourceOwnedCandidateFixtureHelper.rr(
                EntrySourceOwnedCandidateFixtureHelper
                        .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                        .missingSourceOwner()
                        .build(),
                validStop(),
                validTp()
        ).build();
        RrFixture blockedStop = StopTpRrSourceOwnedCandidateFixtureHelper.rr(
                entryFixture(),
                StopTpRrSourceOwnedCandidateFixtureHelper.stop().entryStopInversion().build(),
                validTp()
        ).build();
        RrFixture blockedTp = StopTpRrSourceOwnedCandidateFixtureHelper.rr(
                entryFixture(),
                validStop(),
                StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET).stopTpOverlap().build()
        ).build();

        assertThat(missingEntry.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(missingEntry.blockerEvidence()).contains("missing_entry_fixture_dependency");
        assertThat(missingStop.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(missingStop.blockerEvidence()).contains("missing_stop_fixture_dependency");
        assertThat(missingTp.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(missingTp.blockerEvidence()).contains("missing_tp_fixture_dependency");
        assertThat(incompleteEntry.fixtureStatus()).isEqualTo(FixtureStatus.INCOMPLETE);
        assertThat(incompleteEntry.blockerEvidence()).contains("incomplete_entry_fixture_dependency");
        assertThat(blockedStop.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(blockedStop.blockerEvidence()).contains("blocked_stop_fixture_dependency", "entry_stop_inversion");
        assertThat(blockedTp.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(blockedTp.blockerEvidence()).contains("blocked_tp_fixture_dependency", "stop_tp_overlap");
    }

    @TestFactory
    Stream<DynamicTest> rrEntryStopDistanceCasesFailClosed() {
        return Stream.of(
                distanceCase(EntryStopDistanceState.MISSING, FixtureStatus.INCOMPLETE, "missing_entry_stop_distance"),
                distanceCase(EntryStopDistanceState.ZERO, FixtureStatus.BLOCKED, "zero_entry_stop_distance"),
                distanceCase(EntryStopDistanceState.NEGATIVE, FixtureStatus.BLOCKED, "negative_entry_stop_distance"),
                distanceCase(EntryStopDistanceState.AMBIGUOUS, FixtureStatus.BLOCKED,
                        "ambiguous_entry_stop_distance"),
                distanceCase(EntryStopDistanceState.STALE, FixtureStatus.INCOMPLETE, "stale_entry_stop_distance"),
                distanceCase(EntryStopDistanceState.UNSUPPORTED, FixtureStatus.BLOCKED,
                        "unsupported_entry_stop_distance")
        ).map(testCase -> DynamicTest.dynamicTest(testCase.state().name(), () -> {
            RrFixture fixture = StopTpRrSourceOwnedCandidateFixtureHelper
                    .rr(entryFixture(), validStop(), validTp())
                    .entryStopDistance(testCase.state())
                    .build();

            assertThat(fixture.fixtureStatus()).isEqualTo(testCase.expectedStatus());
            assertMandatoryReviewOnlyFlags(fixture.manualReviewRequired(), fixture.notTradeInstruction(),
                    fixture.reviewMode());
            assertThat(fixture.blockerEvidence()).contains("entryStopDistance", testCase.expectedEvidence());
        }));
    }

    @Test
    void conflictsFailBlockedWithBlockerEvidence() {
        StopFixture stopInversion = StopTpRrSourceOwnedCandidateFixtureHelper.stop().entryStopInversion().build();
        TpFixture tpDirection = StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                .entryTpDirectionConflict()
                .build();
        TpFixture tpOverlap = StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                .stopTpOverlap()
                .build();
        RrFixture rrConflicts = StopTpRrSourceOwnedCandidateFixtureHelper.rr(entryFixture(), validStop(), validTp())
                .entryStopInversion()
                .entryTpDirectionConflict()
                .stopTpOverlap()
                .build();

        assertThat(stopInversion.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(stopInversion.blockerEvidence()).contains("entry_stop_inversion");
        assertThat(tpDirection.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(tpDirection.blockerEvidence()).contains("entry_tp_direction_conflict");
        assertThat(tpOverlap.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(tpOverlap.blockerEvidence()).contains("stop_tp_overlap");
        assertThat(rrConflicts.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
        assertThat(rrConflicts.blockerEvidence()).contains(
                "entry_stop_inversion",
                "entry_tp_direction_conflict",
                "stop_tp_overlap"
        );
    }

    @TestFactory
    Stream<DynamicTest> forbiddenSourcesFailBlockedWithBlockerEvidence() {
        return Arrays.stream(ForbiddenSource.values())
                .map(source -> DynamicTest.dynamicTest(source.name(), () -> {
                    RrFixture fixture = StopTpRrSourceOwnedCandidateFixtureHelper
                            .rr(entryFixture(), validStop(), validTp())
                            .forbiddenSource(source)
                            .build();

                    assertThat(fixture.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
                    assertMandatoryReviewOnlyFlags(fixture.manualReviewRequired(), fixture.notTradeInstruction(),
                            fixture.reviewMode());
                    assertThat(fixture.blockerEvidence()).contains("forbidden_source", source.blockerEvidence());
                }));
    }

    @TestFactory
    Stream<DynamicTest> riskActionGuardBlockersFailBlockedAndStayReviewOnly() {
        return Arrays.stream(RiskActionGuardBlocker.values())
                .map(blocker -> DynamicTest.dynamicTest(blocker.name(), () -> {
                    RrFixture fixture = StopTpRrSourceOwnedCandidateFixtureHelper
                            .rr(entryFixture(), validStop(), validTp())
                            .riskActionGuardBlocker(blocker)
                            .build();

                    assertThat(fixture.fixtureStatus()).isEqualTo(FixtureStatus.BLOCKED);
                    assertMandatoryReviewOnlyFlags(fixture.manualReviewRequired(), fixture.notTradeInstruction(),
                            fixture.reviewMode());
                    assertThat(fixture.blockerEvidence()).contains("riskActionGuard", blocker.blockerEvidence());
                }));
    }

    @Test
    void helperDoesNotExposeProductionValidTradingSurfaceOrMarketDerivedNumericValues() throws Exception {
        String helperSource = Files.readString(Path.of(
                "src/test/java/org/example/trademodel/dto/planboundary/"
                        + "StopTpRrSourceOwnedCandidateFixtureHelper.java"
        ));

        assertThat(helperSource).doesNotContain("BoundaryCandidateDTO.valid(");
        assertThat(helperSource).doesNotContain("BoundaryStatusEnum.VALID");
        assertThat(returnTypesOf(StopTpRrSourceOwnedCandidateFixtureHelper.class))
                .doesNotContain(BoundaryCandidateDTO.class);
        assertThat(fieldTypesOf(StopFixture.class)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(fieldTypesOf(TpFixture.class)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(fieldTypesOf(RrFixture.class)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(fieldTypesOf(NumericSource.class)).doesNotContain(BigDecimal.class);
        assertThat(returnTypesOf(NumericSource.class)).doesNotContain(BigDecimal.class);

        assertThat(publicSurfaceOf(StopFixture.class)).noneMatch(this::containsForbiddenSurfaceToken);
        assertThat(publicSurfaceOf(TpFixture.class)).noneMatch(this::containsForbiddenSurfaceToken);
        assertThat(publicSurfaceOf(RrFixture.class)).noneMatch(this::containsForbiddenSurfaceToken);
        assertThat(publicSurfaceOf(NumericSource.class)).noneMatch(this::containsForbiddenSurfaceToken);

        RrFixture fixture = StopTpRrSourceOwnedCandidateFixtureHelper.completeRrFixture();
        assertThat(fixture.numericSource().fixtureOnly()).isTrue();
        assertThat(fixture.numericSource().marketDerived()).isFalse();
        assertThat(fixture.numericSource().valueToken()).isNotBlank();
    }

    private EntryFixture entryFixture() {
        return EntrySourceOwnedCandidateFixtureHelper.completeFixture(EntryFamily.STRUCTURE_CONFIRMATION_ZONE);
    }

    private StopFixture validStop() {
        return StopTpRrSourceOwnedCandidateFixtureHelper.completeStopFixture();
    }

    private TpFixture validTp() {
        return StopTpRrSourceOwnedCandidateFixtureHelper.completeTpFixture(TpFamily.STRUCTURE_TARGET);
    }

    private void assertMandatoryReviewOnlyFlags(
            boolean manualReviewRequired,
            boolean notTradeInstruction,
            String reviewMode
    ) {
        assertThat(manualReviewRequired).isTrue();
        assertThat(notTradeInstruction).isTrue();
        assertThat(reviewMode).isEqualTo(StopTpRrSourceOwnedCandidateFixtureHelper.REVIEW_ONLY);
    }

    private boolean containsForbiddenSurfaceToken(String surfaceName) {
        String normalizedSurfaceName = surfaceName.toLowerCase(Locale.ROOT);
        return FORBIDDEN_OUTPUT_SURFACE.stream()
                .map(token -> token.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedSurfaceName::contains);
    }

    private List<String> publicSurfaceOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .toList();
    }

    private List<Class<?>> returnTypesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getReturnType)
                .toList();
    }

    private List<Class<?>> fieldTypesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getType)
                .toList();
    }

    private DistanceCase distanceCase(
            EntryStopDistanceState state,
            FixtureStatus expectedStatus,
            String expectedEvidence
    ) {
        return new DistanceCase(state, expectedStatus, expectedEvidence);
    }

    private record DistanceCase(
            EntryStopDistanceState state,
            FixtureStatus expectedStatus,
            String expectedEvidence
    ) {
    }
}
