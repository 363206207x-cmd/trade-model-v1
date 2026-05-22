package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SourceTraceRuntimePopulationFixtureTest {

    private static final List<String> FORBIDDEN_ACTION_SURFACE_TOKENS = List.of(
            "order",
            "execution",
            "automation",
            "autoTrading",
            "autoTrade"
    );

    @Test
    void sourceTraceCarriesRuntimeEvidenceSourceInformationAsReviewOnlyFixture() {
        SourceTraceRuntimePopulationFixtureHelper.RuntimePopulationFixture fixture =
                SourceTraceRuntimePopulationFixtureHelper.completeReviewOnlyFixture();
        SourceTraceDTO trace = fixture.sourceTrace();

        assertThat(trace.getSourceOwner()).isEqualTo("p155-runtime-source-owner");
        assertThat(trace.getSourceRef()).isEqualTo("p155-runtime-source-ref");
        assertThat(trace.getSourceTimeframe()).isEqualTo("1h");
        assertThat(trace.getSourceWindow()).isEqualTo("p155-runtime-source-window");
        assertThat(trace.getFreshnessStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.FRESH.name());
        assertThat(trace.getQuoteFreshnessStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.FRESH.name());
        assertThat(trace.getMissingFields()).isEmpty();
        assertThat(trace.getBlockingReasons()).isEmpty();
        assertThat(fixture.candidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(fixture.candidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertReviewOnly(trace, fixture);
        assertNoReadinessOrRealTradeBoundary(trace);
        assertNoOrderExecutionAutomationSurface(SourceTraceDTO.class);
        assertNoOrderExecutionAutomationSurface(
                SourceTraceRuntimePopulationFixtureHelper.RuntimePopulationFixture.class
        );
    }

    @Test
    void missingRuntimeEvidenceIdentityStaysIncompleteAndCannotBecomeValidOrReady() {
        SourceTraceRuntimePopulationFixtureHelper.RuntimePopulationFixture fixture =
                SourceTraceRuntimePopulationFixtureHelper.missingSourceIdentityFixture();
        SourceTraceDTO trace = fixture.sourceTrace();

        assertThat(fixture.candidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertThat(fixture.candidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
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
        assertReviewOnly(trace, fixture);
        assertNoReadinessOrRealTradeBoundary(trace);
    }

    @Test
    void conflictingUnsafeAndStaleUnsafeRuntimeEvidenceStayBlockedAndFailClosed() {
        List<BlockedCase> blockedCases = List.of(
                new BlockedCase(
                        "conflicting source",
                        SourceTraceRuntimePopulationFixtureHelper.conflictingSourceFixture(),
                        "snapshot_blocked:conflictFamilyStatus:CONFLICT"
                ),
                new BlockedCase(
                        "unsafe substitution",
                        SourceTraceRuntimePopulationFixtureHelper.unsafeSubstitutionFixture(),
                        "direct_forbidden_input:latest_price_only"
                ),
                new BlockedCase(
                        "stale unsafe source window",
                        SourceTraceRuntimePopulationFixtureHelper.staleUnsafeSourceWindowFixture(),
                        "direct_risk_action_guard:stale_unsafe_source_window"
                )
        );

        for (BlockedCase blockedCase : blockedCases) {
            SourceTraceDTO trace = blockedCase.fixture().sourceTrace();

            assertThat(blockedCase.fixture().candidateStatus())
                    .as(blockedCase.name())
                    .isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
            assertThat(blockedCase.fixture().candidateStatus().name())
                    .as(blockedCase.name())
                    .isNotEqualTo(BoundaryStatusEnum.VALID.name());
            assertThat(trace.getFallbackStatus())
                    .as(blockedCase.name())
                    .isEqualTo(SourceTraceFallbackStatusEnum.SAFE_FAIL_CLOSED_ONLY);
            assertThat(trace.getBlockingReasons())
                    .as(blockedCase.name())
                    .contains(blockedCase.expectedBlockingReason());
            assertReviewOnly(trace, blockedCase.fixture());
            assertNoReadinessOrRealTradeBoundary(trace);
        }
    }

    private void assertReviewOnly(
            SourceTraceDTO trace,
            SourceTraceRuntimePopulationFixtureHelper.RuntimePopulationFixture fixture
    ) {
        assertThat(trace.isManualReviewRequired()).isTrue();
        assertThat(trace.isNotTradeInstruction()).isTrue();
        assertThat(trace.getReviewMode()).isEqualTo(SourceTraceRuntimePopulationFixtureHelper.REVIEW_ONLY);
        assertThat(fixture.candidateResult().isManualReviewRequired()).isTrue();
        assertThat(fixture.candidateResult().isNotTradeInstruction()).isTrue();
        assertThat(fixture.candidateResult().getReviewMode())
                .isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
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
            SourceTraceRuntimePopulationFixtureHelper.RuntimePopulationFixture fixture,
            String expectedBlockingReason
    ) {
    }
}
