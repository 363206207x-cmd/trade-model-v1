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
import org.example.trademodel.dto.planboundary.BoundaryCandidateFixtureAssemblerHelper.AssemblerStatus;
import org.example.trademodel.dto.planboundary.BoundaryCandidateFixtureAssemblerHelper.BoundaryCandidateFixture;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFamily;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.RrFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.StopFixture;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFamily;
import org.example.trademodel.dto.planboundary.StopTpRrSourceOwnedCandidateFixtureHelper.TpFixture;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class BoundaryCandidateFixtureAssemblerHelperTest {

    private static final List<String> FORBIDDEN_OUTPUT_SURFACE = List.of(
            "tradeReady",
            "readyToTrade",
            "order",
            "execution",
            "automation",
            "autoTrading"
    );

    @Test
    void fixtureValidAssemblerCombinesDependenciesIntoReviewOnlyBoundaryCandidateStyleOutput() {
        EntryFixture entry = validEntry();
        StopFixture stop = validStop();
        TpFixture tp = validTp();
        RrFixture rr = validRr(entry, stop, tp);

        BoundaryCandidateFixture output = BoundaryCandidateFixtureAssemblerHelper.assemble(entry, stop, tp, rr);

        assertThat(output.assemblerStatus()).isEqualTo(AssemblerStatus.FIXTURE_VALID_CANDIDATE);
        assertThat(output.assemblerStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertMandatoryReviewOnlyFlags(output);
        assertThat(output.entryReview().sourceOwner()).isEqualTo(entry.sourceOwner());
        assertThat(output.stopReview().sourceOwner()).isEqualTo(stop.stopSourceOwner());
        assertThat(output.takeProfitReview().sourceOwner()).isEqualTo(tp.tpSourceOwner());
        assertThat(output.riskRewardReview().sourceOwner()).isEqualTo(rr.rrSourceOwner());
        assertThat(output.sourceOwnerSummary().entrySourceOwner()).isEqualTo(entry.sourceOwner());
        assertThat(output.sourceOwnerSummary().stopSourceOwner()).isEqualTo(stop.stopSourceOwner());
        assertThat(output.sourceOwnerSummary().tpSourceOwner()).isEqualTo(tp.tpSourceOwner());
        assertThat(output.sourceOwnerSummary().rrSourceOwner()).isEqualTo(rr.rrSourceOwner());
        assertThat(output.sourceFamilySummary().entrySourceFamily()).isEqualTo(entry.entrySourceType());
        assertThat(output.sourceFamilySummary().stopSourceFamily()).isEqualTo(stop.stopCandidateFamily());
        assertThat(output.sourceFamilySummary().tpSourceFamily()).isEqualTo(tp.tpCandidateFamily());
        assertThat(output.numericSourceTokenSummary().entryNumericSourceToken())
                .isEqualTo(entry.numericSource().valueToken());
        assertThat(output.numericSourceTokenSummary().stopNumericSourceToken())
                .isEqualTo(stop.numericSource().valueToken());
        assertThat(output.numericSourceTokenSummary().tpNumericSourceToken())
                .isEqualTo(tp.numericSource().valueToken());
        assertThat(output.numericSourceTokenSummary().rrNumericSourceToken())
                .isEqualTo(rr.numericSource().valueToken());
        assertThat(output.boundaryCandidateStyleFieldNames()).contains(
                "entry",
                "stop",
                "takeProfitLevels",
                "sourceFields",
                "blockingReasons"
        );
        assertThat(output.blockerEvidence()).isEmpty();
    }

    @TestFactory
    Stream<DynamicTest> missingDependenciesFailIncomplete() {
        EntryFixture entry = validEntry();
        StopFixture stop = validStop();
        TpFixture tp = validTp();
        RrFixture rr = validRr(entry, stop, tp);

        return Stream.of(
                missingCase("entry", null, stop, tp, rr, "missing_entry_dependency"),
                missingCase("stop", entry, null, tp, rr, "missing_stop_dependency"),
                missingCase("tp", entry, stop, null, rr, "missing_tp_dependency"),
                missingCase("rr", entry, stop, tp, null, "missing_rr_dependency")
        ).map(testCase -> DynamicTest.dynamicTest(testCase.name(), () -> {
            BoundaryCandidateFixture output = BoundaryCandidateFixtureAssemblerHelper.assemble(
                    testCase.entry(),
                    testCase.stop(),
                    testCase.tp(),
                    testCase.rr()
            );

            assertThat(output.assemblerStatus()).isEqualTo(AssemblerStatus.INCOMPLETE);
            assertMandatoryReviewOnlyFlags(output);
            assertThat(output.blockerEvidence()).contains(testCase.expectedEvidence());
        }));
    }

    @Test
    void incompleteDependenciesFailIncompleteAndPreserveEvidence() {
        EntryFixture incompleteEntry = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                .missingSourceOwner()
                .build();
        StopFixture incompleteStop = StopTpRrSourceOwnedCandidateFixtureHelper.stop()
                .missingNumericSource()
                .build();
        TpFixture incompleteTp = StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                .missingTpOwner()
                .build();
        RrFixture incompleteRr = StopTpRrSourceOwnedCandidateFixtureHelper.rr(validEntry(), validStop(), validTp())
                .missingRrOwner()
                .build();

        assertIncompletePreservesEvidence(
                BoundaryCandidateFixtureAssemblerHelper.assemble(incompleteEntry, validStop(), validTp(),
                        validRr(validEntry(), validStop(), validTp())),
                "incomplete_entry_dependency",
                "missing_source_owner"
        );
        assertIncompletePreservesEvidence(
                BoundaryCandidateFixtureAssemblerHelper.assemble(validEntry(), incompleteStop, validTp(),
                        validRr(validEntry(), validStop(), validTp())),
                "incomplete_stop_dependency",
                "missing_numeric_source"
        );
        assertIncompletePreservesEvidence(
                BoundaryCandidateFixtureAssemblerHelper.assemble(validEntry(), validStop(), incompleteTp,
                        validRr(validEntry(), validStop(), validTp())),
                "incomplete_tp_dependency",
                "missing_tp_owner"
        );
        assertIncompletePreservesEvidence(
                BoundaryCandidateFixtureAssemblerHelper.assemble(validEntry(), validStop(), validTp(), incompleteRr),
                "incomplete_rr_dependency",
                "missing_rr_owner"
        );
    }

    @Test
    void blockedDependenciesFailBlockedAndPreserveConflictEvidence() {
        EntryFixture blockedEntry = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                .riskActionGuardBlocker(
                        EntrySourceOwnedCandidateFixtureHelper.RiskActionGuardBlocker.HIGH_RISK_STAMPEDE
                )
                .build();
        StopFixture blockedStop = StopTpRrSourceOwnedCandidateFixtureHelper.stop().entryStopInversion().build();
        TpFixture blockedTp = StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                .stopTpOverlap()
                .build();
        RrFixture blockedRr = StopTpRrSourceOwnedCandidateFixtureHelper.rr(validEntry(), validStop(), validTp())
                .entryTpDirectionConflict()
                .riskActionGuardBlocker(
                        StopTpRrSourceOwnedCandidateFixtureHelper.RiskActionGuardBlocker.HIGH_RISK_STAMPEDE
                )
                .build();

        assertBlockedPreservesEvidence(
                BoundaryCandidateFixtureAssemblerHelper.assemble(blockedEntry, validStop(), validTp(),
                        validRr(validEntry(), validStop(), validTp())),
                "blocked_entry_dependency",
                "risk_action_guard_stampede"
        );
        assertBlockedPreservesEvidence(
                BoundaryCandidateFixtureAssemblerHelper.assemble(validEntry(), blockedStop, validTp(),
                        validRr(validEntry(), validStop(), validTp())),
                "blocked_stop_dependency",
                "entry_stop_inversion"
        );
        assertBlockedPreservesEvidence(
                BoundaryCandidateFixtureAssemblerHelper.assemble(validEntry(), validStop(), blockedTp,
                        validRr(validEntry(), validStop(), validTp())),
                "blocked_tp_dependency",
                "stop_tp_overlap"
        );
        assertBlockedPreservesEvidence(
                BoundaryCandidateFixtureAssemblerHelper.assemble(validEntry(), validStop(), validTp(), blockedRr),
                "blocked_rr_dependency",
                "entry_tp_direction_conflict",
                "risk_action_guard_stampede"
        );
    }

    @Test
    void assemblerDoesNotExposeProductionValidTradingSurfaceOrMarketDerivedNumericValues() throws Exception {
        String helperSource = Files.readString(Path.of(
                "src/test/java/org/example/trademodel/dto/planboundary/"
                        + "BoundaryCandidateFixtureAssemblerHelper.java"
        ));

        assertThat(helperSource).doesNotContain("BoundaryCandidateDTO.valid(");
        assertThat(helperSource).doesNotContain("BoundaryStatusEnum.VALID");
        assertThat(returnTypesOf(BoundaryCandidateFixtureAssemblerHelper.class))
                .doesNotContain(BoundaryCandidateDTO.class);
        assertThat(returnTypesOf(BoundaryCandidateFixture.class)).doesNotContain(BoundaryCandidateDTO.class);
        assertThat(fieldTypesOf(BoundaryCandidateFixture.class)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);

        assertThat(publicSurfaceOf(BoundaryCandidateFixture.class)).noneMatch(this::containsForbiddenSurfaceToken);
        assertThat(publicSurfaceOf(BoundaryCandidateFixtureAssemblerHelper.ReviewField.class))
                .noneMatch(this::containsForbiddenSurfaceToken);

        BoundaryCandidateFixture output = BoundaryCandidateFixtureAssemblerHelper.assemble(
                validEntry(),
                validStop(),
                validTp(),
                validRr(validEntry(), validStop(), validTp())
        );
        assertThat(output.numericSourceTokenSummary().entryNumericSourceToken()).startsWith("fixture-entry-token:");
        assertThat(output.numericSourceTokenSummary().stopNumericSourceToken()).startsWith("fixture-stop-token:");
        assertThat(output.numericSourceTokenSummary().tpNumericSourceToken()).startsWith("fixture-tp-token:");
        assertThat(output.numericSourceTokenSummary().rrNumericSourceToken()).startsWith("fixture-rr-token:");
    }

    private void assertIncompletePreservesEvidence(
            BoundaryCandidateFixture output,
            String dependencyEvidence,
            String preservedEvidence
    ) {
        assertThat(output.assemblerStatus()).isEqualTo(AssemblerStatus.INCOMPLETE);
        assertMandatoryReviewOnlyFlags(output);
        assertThat(output.blockerEvidence()).contains(dependencyEvidence, preservedEvidence);
    }

    private void assertBlockedPreservesEvidence(BoundaryCandidateFixture output, String... expectedEvidence) {
        assertThat(output.assemblerStatus()).isEqualTo(AssemblerStatus.BLOCKED);
        assertMandatoryReviewOnlyFlags(output);
        assertThat(output.blockerEvidence()).contains(expectedEvidence);
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

    private void assertMandatoryReviewOnlyFlags(BoundaryCandidateFixture output) {
        assertThat(output.manualReviewRequired()).isTrue();
        assertThat(output.notTradeInstruction()).isTrue();
        assertThat(output.reviewMode()).isEqualTo(BoundaryCandidateFixtureAssemblerHelper.REVIEW_ONLY);
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

    private MissingCase missingCase(
            String name,
            EntryFixture entry,
            StopFixture stop,
            TpFixture tp,
            RrFixture rr,
            String expectedEvidence
    ) {
        return new MissingCase(name, entry, stop, tp, rr, expectedEvidence);
    }

    private record MissingCase(
            String name,
            EntryFixture entry,
            StopFixture stop,
            TpFixture tp,
            RrFixture rr,
            String expectedEvidence
    ) {
    }
}
