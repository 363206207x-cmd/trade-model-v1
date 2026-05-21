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
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.EntryFixtureStatus;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.ForbiddenSource;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.NumericSource;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.RiskActionGuardBlocker;
import org.example.trademodel.dto.planboundary.EntrySourceOwnedCandidateFixtureHelper.SourceWindow;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class EntrySourceOwnedCandidateFixtureHelperTest {

    private static final List<String> FORBIDDEN_OUTPUT_SURFACE = List.of(
            "tradeReady",
            "readyToTrade",
            "order",
            "execution",
            "automation",
            "autoTrading"
    );

    @TestFactory
    Stream<DynamicTest> fixtureValidCandidateSupportsAllEntryFamiliesAsReviewOnlyFixtureOutput() {
        return Arrays.stream(EntryFamily.values())
                .map(family -> DynamicTest.dynamicTest(family.name(), () -> {
                    EntryFixture fixture = EntrySourceOwnedCandidateFixtureHelper.completeFixture(family);

                    assertThat(fixture.fixtureStatus()).isEqualTo(EntryFixtureStatus.FIXTURE_VALID_CANDIDATE);
                    assertThat(fixture.fixtureStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
                    assertThat(fixture.symbol()).isEqualTo("BTCUSDT_FIXTURE");
                    assertThat(fixture.timeframe()).isEqualTo("1h");
                    assertThat(fixture.entrySourceType()).isEqualTo(family.name());
                    assertThat(fixture.entrySourceTimeframe()).isEqualTo("1h");
                    assertThat(fixture.entrySourceReason()).isNotBlank();
                    assertThat(fixture.entrySourceRef()).isNotBlank();
                    assertThat(fixture.sourceWindow()).isNotNull();
                    assertThat(fixture.sourceWindow().stale()).isFalse();
                    assertThat(fixture.ruleId()).isNotBlank();
                    assertThat(fixture.ruleVersion()).isNotBlank();
                    assertThat(fixture.freshnessOwnership()).isNotBlank();
                    assertThat(fixture.conflictFamilyOwnership()).isNotBlank();
                    assertThat(fixture.sourceOwner()).isNotBlank();
                    assertThat(fixture.numericSource()).isNotNull();
                    assertThat(fixture.numericSource().fixtureOnly()).isTrue();
                    assertThat(fixture.numericSource().marketDerived()).isFalse();
                    assertThat(fixture.numericSource().valueToken()).startsWith("fixture-entry-token:");
                    assertMandatoryReviewOnlyFlags(fixture);
                    assertThat(fixture.blockerEvidence()).isEmpty();
                }));
    }

    @Test
    void missingSourceOwnerFailsIncompleteWithBlockerEvidence() {
        EntryFixture fixture = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                .missingSourceOwner()
                .build();

        assertThat(fixture.fixtureStatus()).isEqualTo(EntryFixtureStatus.INCOMPLETE);
        assertMandatoryReviewOnlyFlags(fixture);
        assertThat(fixture.blockerEvidence()).contains("sourceOwner", "missing_source_owner");
    }

    @Test
    void missingNumericSourceFailsIncompleteWithBlockerEvidence() {
        EntryFixture fixture = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.BREAKOUT_RETEST_ZONE)
                .missingNumericSource()
                .build();

        assertThat(fixture.fixtureStatus()).isEqualTo(EntryFixtureStatus.INCOMPLETE);
        assertMandatoryReviewOnlyFlags(fixture);
        assertThat(fixture.blockerEvidence()).contains("numericSource", "missing_numeric_source");
    }

    @Test
    void staleSourceWindowFailsIncompleteWhenNoUnsafeEvidenceExists() {
        EntryFixture fixture = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.SUPPORT_RESISTANCE_FLIP_ZONE)
                .staleSourceWindow()
                .build();

        assertThat(fixture.fixtureStatus()).isEqualTo(EntryFixtureStatus.INCOMPLETE);
        assertMandatoryReviewOnlyFlags(fixture);
        assertThat(fixture.blockerEvidence()).contains("sourceWindow", "stale_source_window");
        assertThat(fixture.blockerEvidence()).doesNotContain("unsafe_stale_source_window");
    }

    @Test
    void staleSourceWindowFailsBlockedWhenUnsafeEvidenceExists() {
        EntryFixture fixture = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.SUPPORT_RESISTANCE_FLIP_ZONE)
                .staleUnsafeSourceWindow()
                .build();

        assertThat(fixture.fixtureStatus()).isEqualTo(EntryFixtureStatus.BLOCKED);
        assertMandatoryReviewOnlyFlags(fixture);
        assertThat(fixture.blockerEvidence()).contains(
                "sourceWindow",
                "stale_source_window",
                "unsafe_stale_source_window"
        );
    }

    @Test
    void unsupportedSourceFamilyFailsBlockedWithFieldSpecificEvidence() {
        EntryFixture fixture = EntrySourceOwnedCandidateFixtureHelper
                .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                .unsupportedSourceFamily("LATEST_PRICE_DIRECT_ENTRY")
                .build();

        assertThat(fixture.fixtureStatus()).isEqualTo(EntryFixtureStatus.BLOCKED);
        assertMandatoryReviewOnlyFlags(fixture);
        assertThat(fixture.blockerEvidence()).contains(
                "entrySourceType",
                "unsupported_source_family",
                "unsupported_source_family:LATEST_PRICE_DIRECT_ENTRY"
        );
    }

    @TestFactory
    Stream<DynamicTest> forbiddenSourcesFailBlockedWithBlockerEvidence() {
        return Arrays.stream(ForbiddenSource.values())
                .map(source -> DynamicTest.dynamicTest(source.name(), () -> {
                    EntryFixture fixture = EntrySourceOwnedCandidateFixtureHelper
                            .forFamily(EntryFamily.STRUCTURE_CONFIRMATION_ZONE)
                            .forbiddenSource(source)
                            .build();

                    assertThat(fixture.fixtureStatus()).isEqualTo(EntryFixtureStatus.BLOCKED);
                    assertMandatoryReviewOnlyFlags(fixture);
                    assertThat(fixture.blockerEvidence()).contains("forbidden_source", source.blockerEvidence());
                }));
    }

    @TestFactory
    Stream<DynamicTest> riskActionGuardBlockersFailBlockedAndStayReviewOnly() {
        return Arrays.stream(RiskActionGuardBlocker.values())
                .map(blocker -> DynamicTest.dynamicTest(blocker.name(), () -> {
                    EntryFixture fixture = EntrySourceOwnedCandidateFixtureHelper
                            .forFamily(EntryFamily.BREAKOUT_RETEST_ZONE)
                            .riskActionGuardBlocker(blocker)
                            .build();

                    assertThat(fixture.fixtureStatus()).isEqualTo(EntryFixtureStatus.BLOCKED);
                    assertMandatoryReviewOnlyFlags(fixture);
                    assertThat(fixture.blockerEvidence()).contains("riskActionGuard", blocker.blockerEvidence());
                }));
    }

    @Test
    void helperDoesNotExposeProductionValidTradingSurfaceOrMarketDerivedNumericValues() throws Exception {
        String helperSource = Files.readString(Path.of(
                "src/test/java/org/example/trademodel/dto/planboundary/"
                        + "EntrySourceOwnedCandidateFixtureHelper.java"
        ));

        assertThat(helperSource).doesNotContain("BoundaryCandidateDTO.valid(");
        assertThat(helperSource).doesNotContain("BoundaryStatusEnum.VALID");
        assertThat(returnTypesOf(EntrySourceOwnedCandidateFixtureHelper.class)).doesNotContain(BoundaryCandidateDTO.class);
        assertThat(returnTypesOf(EntryFixture.class)).doesNotContain(BoundaryCandidateDTO.class);
        assertThat(fieldTypesOf(EntryFixture.class)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        assertThat(fieldTypesOf(NumericSource.class)).doesNotContain(BigDecimal.class);
        assertThat(returnTypesOf(NumericSource.class)).doesNotContain(BigDecimal.class);

        assertThat(publicSurfaceOf(EntryFixture.class)).noneMatch(this::containsForbiddenSurfaceToken);
        assertThat(publicSurfaceOf(NumericSource.class)).noneMatch(this::containsForbiddenSurfaceToken);

        EntryFixture fixture = EntrySourceOwnedCandidateFixtureHelper.completeFixture(
                EntryFamily.STRUCTURE_CONFIRMATION_ZONE
        );
        assertThat(fixture.numericSource().fixtureOnly()).isTrue();
        assertThat(fixture.numericSource().marketDerived()).isFalse();
        assertThat(fixture.numericSource().valueToken()).isNotBlank();
    }

    private void assertMandatoryReviewOnlyFlags(EntryFixture fixture) {
        assertThat(fixture.manualReviewRequired()).isTrue();
        assertThat(fixture.notTradeInstruction()).isTrue();
        assertThat(fixture.reviewMode()).isEqualTo(EntrySourceOwnedCandidateFixtureHelper.REVIEW_ONLY);
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
}
