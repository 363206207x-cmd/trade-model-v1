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

class FixtureValidIncompleteBlockedMatrixTest {

    private static final List<String> FORBIDDEN_OUTPUT_SURFACE = List.of(
            "tradeReady",
            "readyToTrade",
            "order",
            "execution",
            "automation",
            "autoTrading"
    );

    @Test
    void fullyFixtureValidChainStaysFixtureValidReviewOnlyAndTokenBased() {
        FixtureChain chain = validChain();

        BoundaryCandidateFixture output = assemble(chain);

        assertOutput(output, AssemblerStatus.FIXTURE_VALID_CANDIDATE);
        assertThat(output.blockerEvidence()).isEmpty();
        assertThat(output.numericSourceTokenSummary().entryNumericSourceToken()).startsWith("fixture-entry-token:");
        assertThat(output.numericSourceTokenSummary().stopNumericSourceToken()).startsWith("fixture-stop-token:");
        assertThat(output.numericSourceTokenSummary().tpNumericSourceToken()).startsWith("fixture-tp-token:");
        assertThat(output.numericSourceTokenSummary().rrNumericSourceToken()).startsWith("fixture-rr-token:");
    }

    @TestFactory
    Stream<DynamicTest> incompleteDependenciesKeepAssemblerIncompleteAndPreserveEvidence() {
        return Stream.of(
                matrixCase(
                        "entry incomplete",
                        new FixtureChain(
                                EntrySourceOwnedCandidateFixtureHelper
                                        .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                                        .missingSourceOwner()
                                        .build(),
                                validStop(),
                                validTp(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.INCOMPLETE,
                        "incomplete_entry_dependency",
                        "missing_source_owner"
                ),
                matrixCase(
                        "stop incomplete",
                        new FixtureChain(
                                validEntry(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.stop().missingNumericSource().build(),
                                validTp(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.INCOMPLETE,
                        "incomplete_stop_dependency",
                        "missing_numeric_source"
                ),
                matrixCase(
                        "tp incomplete",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                                        .missingTpOwner()
                                        .build(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.INCOMPLETE,
                        "incomplete_tp_dependency",
                        "missing_tp_owner"
                ),
                matrixCase(
                        "rr incomplete",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                validTp(),
                                StopTpRrSourceOwnedCandidateFixtureHelper
                                        .rr(validEntry(), validStop(), validTp())
                                        .missingRrOwner()
                                        .build()
                        ),
                        AssemblerStatus.INCOMPLETE,
                        "incomplete_rr_dependency",
                        "missing_rr_owner"
                )
        ).map(this::dynamicMatrixTest);
    }

    @TestFactory
    Stream<DynamicTest> blockedDependenciesKeepAssemblerBlockedAndPreserveEvidence() {
        return Stream.of(
                matrixCase(
                        "entry blocked",
                        new FixtureChain(
                                EntrySourceOwnedCandidateFixtureHelper
                                        .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                                        .forbiddenSource(
                                                EntrySourceOwnedCandidateFixtureHelper.ForbiddenSource.AI_TEXT
                                        )
                                        .build(),
                                validStop(),
                                validTp(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_entry_dependency",
                        "ai_text_source"
                ),
                matrixCase(
                        "stop blocked",
                        new FixtureChain(
                                validEntry(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.stop().entryStopInversion().build(),
                                validTp(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_stop_dependency",
                        "entry_stop_inversion"
                ),
                matrixCase(
                        "tp blocked",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                                        .stopTpOverlap()
                                        .build(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_tp_dependency",
                        "stop_tp_overlap"
                ),
                matrixCase(
                        "rr blocked",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                validTp(),
                                StopTpRrSourceOwnedCandidateFixtureHelper
                                        .rr(validEntry(), validStop(), validTp())
                                        .entryTpDirectionConflict()
                                        .build()
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_rr_dependency",
                        "entry_tp_direction_conflict"
                )
        ).map(this::dynamicMatrixTest);
    }

    @TestFactory
    Stream<DynamicTest> forbiddenSourceAnywhereBlocksAssembler() {
        return Stream.of(
                matrixCase(
                        "forbidden source on entry",
                        new FixtureChain(
                                EntrySourceOwnedCandidateFixtureHelper
                                        .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                                        .forbiddenSource(
                                                EntrySourceOwnedCandidateFixtureHelper.ForbiddenSource.DASHBOARD_TEXT
                                        )
                                        .build(),
                                validStop(),
                                validTp(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_entry_dependency",
                        "dashboard_text_source"
                ),
                matrixCase(
                        "forbidden source on stop",
                        new FixtureChain(
                                validEntry(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.stop()
                                        .forbiddenSource(
                                                StopTpRrSourceOwnedCandidateFixtureHelper.ForbiddenSource
                                                        .LATEST_PRICE_ONLY
                                        )
                                        .build(),
                                validTp(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_stop_dependency",
                        "latest_price_only"
                ),
                matrixCase(
                        "forbidden source on tp",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                                        .forbiddenSource(
                                                StopTpRrSourceOwnedCandidateFixtureHelper.ForbiddenSource
                                                        .SINGLE_KLINE_ONLY
                                        )
                                        .build(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_tp_dependency",
                        "single_kline_only"
                ),
                matrixCase(
                        "forbidden source on rr",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                validTp(),
                                StopTpRrSourceOwnedCandidateFixtureHelper
                                        .rr(validEntry(), validStop(), validTp())
                                        .forbiddenSource(
                                                StopTpRrSourceOwnedCandidateFixtureHelper.ForbiddenSource
                                                        .ORDER_EXECUTION_BACKFILL
                                        )
                                        .build()
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_rr_dependency",
                        "order_execution_backfill"
                )
        ).map(this::dynamicMatrixTest);
    }

    @TestFactory
    Stream<DynamicTest> riskActionGuardBlockerAnywhereBlocksAssembler() {
        return Stream.of(
                matrixCase(
                        "risk guard on entry",
                        new FixtureChain(
                                EntrySourceOwnedCandidateFixtureHelper
                                        .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                                        .riskActionGuardBlocker(
                                                EntrySourceOwnedCandidateFixtureHelper.RiskActionGuardBlocker
                                                        .HIGH_RISK_STAMPEDE
                                        )
                                        .build(),
                                validStop(),
                                validTp(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_entry_dependency",
                        "risk_action_guard_stampede"
                ),
                matrixCase(
                        "risk guard on stop",
                        new FixtureChain(
                                validEntry(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.stop()
                                        .riskActionGuardBlocker(
                                                StopTpRrSourceOwnedCandidateFixtureHelper.RiskActionGuardBlocker
                                                        .HIGH_RISK_STAMPEDE
                                        )
                                        .build(),
                                validTp(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_stop_dependency",
                        "risk_action_guard_stampede"
                ),
                matrixCase(
                        "risk guard on tp",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                                        .riskActionGuardBlocker(
                                                StopTpRrSourceOwnedCandidateFixtureHelper.RiskActionGuardBlocker
                                                        .HIGH_RISK_STAMPEDE
                                        )
                                        .build(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_tp_dependency",
                        "risk_action_guard_stampede"
                ),
                matrixCase(
                        "risk guard on rr",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                validTp(),
                                StopTpRrSourceOwnedCandidateFixtureHelper
                                        .rr(validEntry(), validStop(), validTp())
                                        .riskActionGuardBlocker(
                                                StopTpRrSourceOwnedCandidateFixtureHelper.RiskActionGuardBlocker
                                                        .HIGH_RISK_STAMPEDE
                                        )
                                        .build()
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_rr_dependency",
                        "risk_action_guard_stampede"
                )
        ).map(this::dynamicMatrixTest);
    }

    @TestFactory
    Stream<DynamicTest> directionConflictsBlockAssembler() {
        return Stream.of(
                matrixCase(
                        "entry-stop inversion",
                        new FixtureChain(
                                validEntry(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.stop().entryStopInversion().build(),
                                validTp(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_stop_dependency",
                        "entry_stop_inversion"
                ),
                matrixCase(
                        "entry-tp direction conflict",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                                        .entryTpDirectionConflict()
                                        .build(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_tp_dependency",
                        "entry_tp_direction_conflict"
                ),
                matrixCase(
                        "stop-tp overlap",
                        new FixtureChain(
                                validEntry(),
                                validStop(),
                                StopTpRrSourceOwnedCandidateFixtureHelper.tp(TpFamily.STRUCTURE_TARGET)
                                        .stopTpOverlap()
                                        .build(),
                                validRr(validEntry(), validStop(), validTp())
                        ),
                        AssemblerStatus.BLOCKED,
                        "blocked_tp_dependency",
                        "stop_tp_overlap"
                )
        ).map(this::dynamicMatrixTest);
    }

    @TestFactory
    Stream<DynamicTest> missingDependencyKeepsAssemblerIncomplete() {
        FixtureChain validChain = validChain();
        return Stream.of(
                matrixCase(
                        "missing entry dependency",
                        new FixtureChain(null, validChain.stop(), validChain.tp(), validChain.rr()),
                        AssemblerStatus.INCOMPLETE,
                        "missing_entry_dependency"
                ),
                matrixCase(
                        "missing stop dependency",
                        new FixtureChain(validChain.entry(), null, validChain.tp(), validChain.rr()),
                        AssemblerStatus.INCOMPLETE,
                        "missing_stop_dependency"
                ),
                matrixCase(
                        "missing tp dependency",
                        new FixtureChain(validChain.entry(), validChain.stop(), null, validChain.rr()),
                        AssemblerStatus.INCOMPLETE,
                        "missing_tp_dependency"
                ),
                matrixCase(
                        "missing rr dependency",
                        new FixtureChain(validChain.entry(), validChain.stop(), validChain.tp(), null),
                        AssemblerStatus.INCOMPLETE,
                        "missing_rr_dependency"
                )
        ).map(this::dynamicMatrixTest);
    }

    @Test
    void matrixOutputsExposeNoProductionValidOrTradingSurfaceOrRealValueTypes() throws Exception {
        String assemblerSource = Files.readString(Path.of(
                "src/test/java/org/example/trademodel/dto/planboundary/"
                        + "BoundaryCandidateFixtureAssemblerHelper.java"
        ));
        String entrySource = Files.readString(Path.of(
                "src/test/java/org/example/trademodel/dto/planboundary/"
                        + "EntrySourceOwnedCandidateFixtureHelper.java"
        ));
        String stopTpRrSource = Files.readString(Path.of(
                "src/test/java/org/example/trademodel/dto/planboundary/"
                        + "StopTpRrSourceOwnedCandidateFixtureHelper.java"
        ));

        assertThat(assemblerSource).doesNotContain("BoundaryCandidateDTO.valid(");
        assertThat(entrySource).doesNotContain("BoundaryCandidateDTO.valid(");
        assertThat(stopTpRrSource).doesNotContain("BoundaryCandidateDTO.valid(");
        assertThat(assemblerSource).doesNotContain("BoundaryStatusEnum.VALID");
        assertThat(entrySource).doesNotContain("BoundaryStatusEnum.VALID");
        assertThat(stopTpRrSource).doesNotContain("BoundaryStatusEnum.VALID");

        assertThat(returnTypesOf(BoundaryCandidateFixtureAssemblerHelper.class))
                .doesNotContain(BoundaryCandidateDTO.class);
        assertThat(returnTypesOf(BoundaryCandidateFixtureAssemblerHelper.BoundaryCandidateFixture.class))
                .doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(returnTypesOf(EntrySourceOwnedCandidateFixtureHelper.EntryFixture.class))
                .doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(returnTypesOf(StopTpRrSourceOwnedCandidateFixtureHelper.StopFixture.class))
                .doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(returnTypesOf(StopTpRrSourceOwnedCandidateFixtureHelper.TpFixture.class))
                .doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(returnTypesOf(StopTpRrSourceOwnedCandidateFixtureHelper.RrFixture.class))
                .doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(fieldTypesOf(BoundaryCandidateFixtureAssemblerHelper.BoundaryCandidateFixture.class))
                .doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(fieldTypesOf(BoundaryCandidateFixtureAssemblerHelper.NumericSourceTokenSummary.class))
                .doesNotContain(BigDecimal.class);

        assertThat(publicSurfaceOf(BoundaryCandidateFixtureAssemblerHelper.BoundaryCandidateFixture.class))
                .noneMatch(this::containsForbiddenSurfaceToken);
        assertThat(publicSurfaceOf(BoundaryCandidateFixtureAssemblerHelper.ReviewField.class))
                .noneMatch(this::containsForbiddenSurfaceToken);
        assertThat(publicSurfaceOf(BoundaryCandidateFixtureAssemblerHelper.NumericSourceTokenSummary.class))
                .noneMatch(this::containsForbiddenSurfaceToken);
    }

    private DynamicTest dynamicMatrixTest(MatrixCase testCase) {
        return DynamicTest.dynamicTest(testCase.name(), () -> {
            BoundaryCandidateFixture output = assemble(testCase.chain());

            assertOutput(output, testCase.expectedStatus());
            assertThat(output.blockerEvidence()).containsAll(testCase.expectedEvidence());
        });
    }

    private void assertOutput(BoundaryCandidateFixture output, AssemblerStatus expectedStatus) {
        assertThat(output.assemblerStatus()).isEqualTo(expectedStatus);
        assertThat(output.assemblerStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(output.manualReviewRequired()).isTrue();
        assertThat(output.notTradeInstruction()).isTrue();
        assertThat(output.reviewMode()).isEqualTo(BoundaryCandidateFixtureAssemblerHelper.REVIEW_ONLY);
        assertThat(output.boundaryCandidateStyleFieldNames()).doesNotContain(
                "tradeReady",
                "order",
                "execution",
                "automation"
        );
    }

    private BoundaryCandidateFixture assemble(FixtureChain chain) {
        return BoundaryCandidateFixtureAssemblerHelper.assemble(
                chain.entry(),
                chain.stop(),
                chain.tp(),
                chain.rr()
        );
    }

    private FixtureChain validChain() {
        EntryFixture entry = validEntry();
        StopFixture stop = validStop();
        TpFixture tp = validTp();
        RrFixture rr = validRr(entry, stop, tp);
        return new FixtureChain(entry, stop, tp, rr);
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

    private MatrixCase matrixCase(
            String name,
            FixtureChain chain,
            AssemblerStatus expectedStatus,
            String... expectedEvidence
    ) {
        return new MatrixCase(name, chain, expectedStatus, List.of(expectedEvidence));
    }

    private record FixtureChain(
            EntryFixture entry,
            StopFixture stop,
            TpFixture tp,
            RrFixture rr
    ) {
    }

    private record MatrixCase(
            String name,
            FixtureChain chain,
            AssemblerStatus expectedStatus,
            List<String> expectedEvidence
    ) {
    }
}
