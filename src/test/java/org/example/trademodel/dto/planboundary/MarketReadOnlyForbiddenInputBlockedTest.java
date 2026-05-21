package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class MarketReadOnlyForbiddenInputBlockedTest {

    private static final List<Path> GUARDED_SOURCE_PATHS = List.of(
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyEvidenceSnapshotDTO.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateResultDTO.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateGenerator.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/InertMarketReadOnlyCandidateGenerator.java")
    );
    private static final List<String> RUNTIME_LIVE_EXTERNAL_TERMS = List.of(
            "runtimeData",
            "liveMarket",
            "externalFetch",
            "exchangeClient",
            "binance",
            "okx",
            "coinglass",
            "restTemplate",
            "webClient"
    );
    private static final List<String> FORBIDDEN_SURFACE_TERMS = List.of(
            "tradeReady",
            "readyToTrade",
            "order",
            "execution",
            "automation",
            "autoTrading",
            "autoTrade",
            "open",
            "close",
            "reverse",
            "signal",
            "buy",
            "sell",
            "generatedEntry",
            "generatedStop",
            "generatedTakeProfit",
            "generatedRiskReward",
            "stopValue",
            "takeProfitValue",
            "riskRewardValue",
            "readiness"
    );
    private static final List<Class<? extends Annotation>> SPRING_ANNOTATIONS = List.of(
            Service.class,
            Component.class,
            Repository.class,
            Controller.class,
            RestController.class,
            Configuration.class
    );
    private static final List<Class<? extends Annotation>> ENDPOINT_ANNOTATIONS = List.of(
            RequestMapping.class,
            GetMapping.class,
            PostMapping.class,
            PutMapping.class,
            DeleteMapping.class,
            PatchMapping.class
    );

    private final MarketReadOnlyCandidateGenerator generator = new InertMarketReadOnlyCandidateGenerator();

    @Test
    void forbiddenInputMarkersShouldBlockSnapshotAndCandidate() {
        assertForbiddenInputBlocked("latest_price_only");
        assertForbiddenInputBlocked("ai_text");
        assertForbiddenInputBlocked("dashboard_text");
        assertForbiddenInputBlocked("order_execution_backfill");
        assertForbiddenInputBlocked("wick_pin_bar_direct_trend_reversal");
    }

    @Test
    void noGoEvidenceMarkersShouldBlockSnapshotAndCandidate() {
        assertNoGoBlocked("liquidity_stress_stampede");
        assertNoGoBlocked("missing_event_data");
        assertNoGoBlocked("multi_timeframe_conflict");
    }

    @Test
    void riskActionGuardBlockersShouldBlockSnapshotAndCandidate() {
        assertRiskGuardBlocked("risk_high_liquidity_deteriorating_no_one_shot_exit");
        assertRiskGuardBlocked("risk_high_stampede_forbid_reverse_new_position_opportunity_push");
        assertRiskGuardBlocked("risk_high_wick_pin_bar_no_direct_trend_reversal_no_reverse_entry");
    }

    @Test
    void riskHighLiquidityNormalShouldRemainReviewOnlySuggestionWithoutBlocker() {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = validSnapshotBuilder()
                .riskActionGuardContext("review_only_reduce_size_move_stop_reduce_leverage")
                .build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        assertThat(snapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.COMPLETE_FOR_REVIEW);
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getRiskActionGuardReview()).isEqualTo("risk_action_guard:review_only_no_direct_blocker");
        assertThat(result.getBlockingReasons()).isEmpty();
        assertReviewOnly(result);
    }

    @Test
    void blockingEvidenceStatusesShouldBlockSnapshotAndCandidate() {
        assertStatusBlocked(
                builder -> builder.freshnessStatus(MarketReadOnlyEvidenceStatusEnum.CONFLICT),
                "freshnessStatus:CONFLICT"
        );
        assertStatusBlocked(
                builder -> builder.conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CONFLICT),
                "conflictFamilyStatus:CONFLICT"
        );
        assertStatusBlocked(
                builder -> builder.eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.NO_GO),
                "eventEvidenceStatus:NO_GO"
        );
        assertStatusBlocked(
                builder -> builder.liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.BLOCKED),
                "liquidityEvidenceStatus:BLOCKED"
        );
        assertStatusBlocked(
                builder -> builder.wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.FORBIDDEN_INPUT),
                "wickPinBarEvidenceStatus:FORBIDDEN_INPUT"
        );
        assertStatusBlocked(
                builder -> builder.multiTimeframeEvidenceStatus(
                        MarketReadOnlyEvidenceStatusEnum.RISK_ACTION_GUARD_BLOCKER
                ),
                "multiTimeframeEvidenceStatus:RISK_ACTION_GUARD_BLOCKER"
        );
    }

    @Test
    void blockedInputsShouldNeverBecomeReviewOnlyCandidateOrValid() {
        List<MarketReadOnlyCandidateResultDTO> results = List.of(
                generator.review(validSnapshotBuilder().forbiddenInputMarkers(List.of("latest_price_only")).build()),
                generator.review(validSnapshotBuilder().forbiddenInputMarkers(List.of("ai_text")).build()),
                generator.review(validSnapshotBuilder().forbiddenInputMarkers(List.of("dashboard_text")).build()),
                generator.review(validSnapshotBuilder().forbiddenInputMarkers(List.of("order_execution_backfill")).build()),
                generator.review(validSnapshotBuilder().noGoEvidenceMarkers(List.of("missing_event_data")).build()),
                generator.review(validSnapshotBuilder().noGoEvidenceMarkers(List.of("liquidity_stress_stampede")).build()),
                generator.review(validSnapshotBuilder().noGoEvidenceMarkers(List.of("multi_timeframe_conflict")).build()),
                generator.review(validSnapshotBuilder().riskActionGuardBlockers(List.of("stampede_reverse_blocked")).build())
        );

        for (MarketReadOnlyCandidateResultDTO result : results) {
            assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
            assertThat(result.getCandidateStatus()).isNotEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
            assertThat(result.getCandidateStatus().name()).isNotEqualTo("VALID");
            assertThat(result.getBlockingReasons()).isNotEmpty();
            assertReviewOnly(result);
        }
    }

    @Test
    void blockedContractsShouldKeepForbiddenProductionAndRuntimeSurfaceAbsent() throws Exception {
        for (Path path : GUARDED_SOURCE_PATHS) {
            String source = Files.readString(path);
            String normalizedSource = source.toLowerCase(Locale.ROOT);

            assertThat(source).doesNotContain("BoundaryCandidateDTO.valid(");
            assertThat(source).doesNotContain("BoundaryStatusEnum.VALID");
            for (String term : RUNTIME_LIVE_EXTERNAL_TERMS) {
                assertThat(normalizedSource).doesNotContain(term.toLowerCase(Locale.ROOT));
            }
        }
        for (Class<?> type : guardedTypes()) {
            assertThat(returnTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(parameterTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(fieldTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(publicSurfaceOf(type)).noneMatch(this::containsForbiddenSurfaceTerm);
            assertThat(fieldNamesOf(type)).noneMatch(this::containsForbiddenSurfaceTerm);
            for (Class<? extends Annotation> annotation : SPRING_ANNOTATIONS) {
                assertThat(type.getAnnotation(annotation)).isNull();
            }
            for (Class<? extends Annotation> annotation : ENDPOINT_ANNOTATIONS) {
                assertThat(type.getAnnotation(annotation)).isNull();
            }
        }
    }

    private void assertForbiddenInputBlocked(String marker) {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = validSnapshotBuilder()
                .forbiddenInputMarkers(List.of(marker))
                .build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        assertBlockedSnapshotAndCandidate(
                snapshot,
                result,
                marker,
                "direct_forbidden_input:" + marker,
                "snapshot_blocked:" + marker
        );
    }

    private void assertNoGoBlocked(String marker) {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = validSnapshotBuilder()
                .noGoEvidenceMarkers(List.of(marker))
                .build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        assertBlockedSnapshotAndCandidate(
                snapshot,
                result,
                marker,
                "direct_no_go:" + marker,
                "snapshot_blocked:" + marker
        );
    }

    private void assertRiskGuardBlocked(String marker) {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = validSnapshotBuilder()
                .riskActionGuardBlockers(List.of(marker))
                .build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        assertBlockedSnapshotAndCandidate(
                snapshot,
                result,
                marker,
                "direct_risk_action_guard:" + marker,
                "snapshot_blocked:" + marker
        );
    }

    private void assertStatusBlocked(
            Function<MarketReadOnlyEvidenceSnapshotDTO.Builder, MarketReadOnlyEvidenceSnapshotDTO.Builder> mutator,
            String blockerEvidence
    ) {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = mutator.apply(validSnapshotBuilder()).build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        assertBlockedSnapshotAndCandidate(
                snapshot,
                result,
                blockerEvidence,
                "snapshot_blocked:" + blockerEvidence
        );
    }

    private void assertBlockedSnapshotAndCandidate(
            MarketReadOnlyEvidenceSnapshotDTO snapshot,
            MarketReadOnlyCandidateResultDTO result,
            String snapshotEvidence,
            String... candidateEvidence
    ) {
        assertThat(snapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.BLOCKED);
        assertThat(snapshot.getBlockerEvidence()).contains(snapshotEvidence);
        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.BLOCKED);
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
        assertThat(result.getCandidateStatus()).isNotEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo("VALID");
        assertThat(result.getBlockingReasons()).contains(candidateEvidence);
        assertReviewOnly(result);
    }

    private void assertReviewOnly(MarketReadOnlyCandidateResultDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
    }

    private MarketReadOnlyEvidenceSnapshotDTO.Builder validSnapshotBuilder() {
        return MarketReadOnlyEvidenceSnapshotDTO.builder()
                .symbol("BTCUSDT")
                .timeframe("1h")
                .evidenceRefs(List.of("structure-window-1", "event-window-1"))
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
                .sourceOwner("market-structure-snapshot")
                .sourceRef("snapshot-ref-001")
                .sourceTimeframe("1h")
                .sourceReason("already-ingested structure evidence")
                .sourceWindow("2026-05-21T08:00Z/2026-05-21T12:00Z")
                .ruleId("market-read-only-snapshot")
                .ruleVersion("p118-forbidden-no-go-blocked-v1")
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.FRESH)
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CLEAR)
                .dataQualityScore(90)
                .eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .riskActionGuardContext("review_only_no_blocker");
    }

    private List<Class<?>> guardedTypes() {
        return List.of(
                MarketReadOnlyEvidenceSnapshotDTO.class,
                MarketReadOnlyCandidateResultDTO.class,
                MarketReadOnlyCandidateGenerator.class,
                InertMarketReadOnlyCandidateGenerator.class
        );
    }

    private List<String> publicSurfaceOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getName)
                .toList();
    }

    private List<String> fieldNamesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getName)
                .toList();
    }

    private List<Class<?>> returnTypesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .map(Method::getReturnType)
                .toList();
    }

    private List<Class<?>> parameterTypesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
                .flatMap(method -> Arrays.stream(method.getParameterTypes()))
                .toList();
    }

    private List<Class<?>> fieldTypesOf(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
                .map(Field::getType)
                .toList();
    }

    private boolean containsForbiddenSurfaceTerm(String surfaceName) {
        String normalizedSurfaceName = surfaceName.toLowerCase(Locale.ROOT);
        return FORBIDDEN_SURFACE_TERMS.stream()
                .map(term -> term.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedSurfaceName::contains);
    }
}
