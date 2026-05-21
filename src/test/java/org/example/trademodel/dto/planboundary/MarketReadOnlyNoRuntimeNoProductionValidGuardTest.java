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

class MarketReadOnlyNoRuntimeNoProductionValidGuardTest {

    private static final List<Path> GUARDED_SOURCE_PATHS = List.of(
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyEvidenceSnapshotDTO.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyEvidenceFamilyEnum.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyEvidenceStatusEnum.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlySnapshotStatusEnum.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateResultDTO.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateStatusEnum.java"),
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
    private static final List<String> FORBIDDEN_PRODUCTION_TERMS = List.of(
            "BoundaryCandidateDTO.valid(",
            "BoundaryStatusEnum.VALID",
            "BoundaryCandidateService",
            "ExecutionPlan",
            "readiness",
            "generatedEntry",
            "generatedStop",
            "generatedTakeProfit",
            "generatedRiskReward",
            "stopValue",
            "takeProfitValue",
            "riskRewardValue",
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
            "sell"
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
    void readOnlyLineShouldExposeNoRuntimeLiveExternalOrProductionValidSourceTerms() throws Exception {
        for (Path path : GUARDED_SOURCE_PATHS) {
            String source = Files.readString(path);
            String normalizedSource = source.toLowerCase(Locale.ROOT);

            for (String term : RUNTIME_LIVE_EXTERNAL_TERMS) {
                assertThat(normalizedSource).doesNotContain(term.toLowerCase(Locale.ROOT));
            }
            for (String term : FORBIDDEN_PRODUCTION_TERMS) {
                assertThat(normalizedSource).doesNotContain(term.toLowerCase(Locale.ROOT));
            }
        }
    }

    @Test
    void readOnlyLineShouldExposeNoSpringEndpointOrProductionDtoSurface() {
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

    @Test
    void completeSafeFixtureShouldRemainReviewOnlyCandidateOnly() {
        MarketReadOnlyCandidateResultDTO result = generator.review(completeSafeSnapshot().build());

        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.COMPLETE_FOR_REVIEW);
        assertThat(result.getBlockingReasons()).isEmpty();
        assertThat(result.getEntryReview()).isEqualTo("entry_review_token:review_only_source_context");
        assertThat(result.getStopReview()).isEqualTo("stop_review_token:review_only_source_context");
        assertThat(result.getTpReview()).isEqualTo("tp_review_token:review_only_source_context");
        assertThat(result.getRrReview()).isEqualTo("rr_review_token:review_only_source_context");
        assertThat(result.getNumericSourceSummary()).isEqualTo("numeric_source_summary:review_token_only");
        assertReviewOnly(result);
    }

    @Test
    void blockedAndIncompleteFixturesShouldNeverBecomeReviewOnlyCandidateOrValid() {
        List<MarketReadOnlyCandidateResultDTO> results = List.of(
                generator.review(completeSafeSnapshot().toBuilderMissingOwner()),
                generator.review(completeSafeSnapshot().toBuilderForbiddenInput())
        );

        assertThat(results.get(0).getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertThat(results.get(0).getBlockingReasons()).contains("snapshot_missing:sourceOwner");
        assertThat(results.get(1).getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
        assertThat(results.get(1).getBlockingReasons())
                .contains("direct_forbidden_input:latest_price_only", "snapshot_blocked:latest_price_only");
        for (MarketReadOnlyCandidateResultDTO result : results) {
            assertThat(result.getCandidateStatus()).isNotEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
            assertThat(result.getCandidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
            assertThat(result.getBlockingReasons()).isNotEmpty();
            assertReviewOnly(result);
        }
    }

    private ReadOnlySnapshotFixture completeSafeSnapshot() {
        return new ReadOnlySnapshotFixture(MarketReadOnlyEvidenceSnapshotDTO.builder()
                .symbol("ETHUSDT")
                .timeframe("4h")
                .evidenceRefs(List.of("fixture-structure-ref", "fixture-liquidity-ref", "fixture-event-ref"))
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
                .sourceOwner("fixture-market-structure-owner")
                .sourceRef("fixture-snapshot-ref-120")
                .sourceTimeframe("4h")
                .sourceReason("already-ingested fixture structure evidence")
                .sourceWindow("fixture-window-2026-05-21T00:00Z_2026-05-21T04:00Z")
                .ruleId("fixture-no-runtime-no-valid-guard")
                .ruleVersion("p120-no-runtime-no-valid-guard-v1")
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.FRESH)
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CLEAR)
                .dataQualityScore(95)
                .eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .riskActionGuardContext("review_only_fixture_no_blocker"));
    }

    private void assertReviewOnly(MarketReadOnlyCandidateResultDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
    }

    private List<Class<?>> guardedTypes() {
        return List.of(
                MarketReadOnlyEvidenceSnapshotDTO.class,
                MarketReadOnlyEvidenceFamilyEnum.class,
                MarketReadOnlyEvidenceStatusEnum.class,
                MarketReadOnlySnapshotStatusEnum.class,
                MarketReadOnlyCandidateResultDTO.class,
                MarketReadOnlyCandidateStatusEnum.class,
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

    private record ReadOnlySnapshotFixture(MarketReadOnlyEvidenceSnapshotDTO.Builder builder) {

        private MarketReadOnlyEvidenceSnapshotDTO build() {
            return builder.build();
        }

        private MarketReadOnlyEvidenceSnapshotDTO toBuilderMissingOwner() {
            return builder.sourceOwner(null).build();
        }

        private MarketReadOnlyEvidenceSnapshotDTO toBuilderForbiddenInput() {
            return builder.forbiddenInputMarkers(List.of("latest_price_only")).build();
        }
    }
}
