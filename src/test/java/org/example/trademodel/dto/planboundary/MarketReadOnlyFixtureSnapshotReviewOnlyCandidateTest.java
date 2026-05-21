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

class MarketReadOnlyFixtureSnapshotReviewOnlyCandidateTest {

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
    void completeFixtureSnapshotShouldFlowToReviewOnlyCandidate() {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = completeFixtureSnapshot();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        assertThat(snapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.COMPLETE_FOR_REVIEW);
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo("VALID");
        assertThat(result.getSymbol()).isEqualTo("ETHUSDT");
        assertThat(result.getTimeframe()).isEqualTo("4h");
        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.COMPLETE_FOR_REVIEW);
        assertThat(result.getSourceOwnershipSummary()).isEqualTo("source_owner_summary:fixture-market-structure-owner");
        assertThat(result.getFreshnessStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.FRESH);
        assertThat(result.getSourceWindow()).isEqualTo("fixture-window-2026-05-21T00:00Z_2026-05-21T04:00Z");
        assertThat(result.getRuleVersion()).isEqualTo("p119-fixture-review-only-candidate-v1");
        assertThat(result.getConflictFamilyStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.CLEAR);
        assertThat(result.getDataQualityScore()).isEqualTo(95);
        assertThat(result.getRiskActionGuardReview()).isEqualTo("risk_action_guard:review_only_no_direct_blocker");
        assertThat(result.getBlockingReasons()).isEmpty();
        assertReviewOnly(result);
    }

    @Test
    void reviewFieldsAndNumericSummaryShouldRemainStringTokensOnly() throws Exception {
        MarketReadOnlyCandidateResultDTO result = generator.review(completeFixtureSnapshot());

        assertThat(result.getEntryReview()).isEqualTo("entry_review_token:review_only_source_context");
        assertThat(result.getStopReview()).isEqualTo("stop_review_token:review_only_source_context");
        assertThat(result.getTpReview()).isEqualTo("tp_review_token:review_only_source_context");
        assertThat(result.getRrReview()).isEqualTo("rr_review_token:review_only_source_context");
        assertThat(result.getNumericSourceSummary()).isEqualTo("numeric_source_summary:review_token_only");
        assertThat(MarketReadOnlyCandidateResultDTO.class.getDeclaredField("entryReview").getType())
                .isEqualTo(String.class);
        assertThat(MarketReadOnlyCandidateResultDTO.class.getDeclaredField("stopReview").getType())
                .isEqualTo(String.class);
        assertThat(MarketReadOnlyCandidateResultDTO.class.getDeclaredField("tpReview").getType())
                .isEqualTo(String.class);
        assertThat(MarketReadOnlyCandidateResultDTO.class.getDeclaredField("rrReview").getType())
                .isEqualTo(String.class);
        assertThat(MarketReadOnlyCandidateResultDTO.class.getDeclaredField("numericSourceSummary").getType())
                .isEqualTo(String.class);
    }

    @Test
    void reviewOnlyCandidateShouldNotExposeProductionValidReadinessOrTradingSurface() {
        MarketReadOnlyCandidateResultDTO result = generator.review(completeFixtureSnapshot());

        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo(BoundaryStatusEnum.VALID.name());
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
        assertThat(publicSurfaceOf(MarketReadOnlyCandidateResultDTO.class))
                .noneMatch(this::containsForbiddenSurfaceTerm);
        assertThat(fieldNamesOf(MarketReadOnlyCandidateResultDTO.class))
                .noneMatch(this::containsForbiddenSurfaceTerm);
    }

    @Test
    void fixtureCandidateContractsShouldHaveNoBigDecimalOrProductionDtoSurface() {
        for (Class<?> type : guardedTypes()) {
            assertThat(returnTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(parameterTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(fieldTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
        }
    }

    @Test
    void fixtureCandidateSourcesShouldNotCallProductionFactoriesOrRuntimeApis() throws Exception {
        for (Path path : GUARDED_SOURCE_PATHS) {
            String source = Files.readString(path);
            String normalizedSource = source.toLowerCase(Locale.ROOT);

            assertThat(source).doesNotContain("BoundaryCandidateDTO.valid(");
            assertThat(source).doesNotContain("BoundaryStatusEnum.VALID");
            for (String term : RUNTIME_LIVE_EXTERNAL_TERMS) {
                assertThat(normalizedSource).doesNotContain(term.toLowerCase(Locale.ROOT));
            }
        }
    }

    @Test
    void fixtureCandidateContractsShouldHaveNoSpringOrEndpointAnnotations() {
        for (Class<?> type : guardedTypes()) {
            for (Class<? extends Annotation> annotation : SPRING_ANNOTATIONS) {
                assertThat(type.getAnnotation(annotation)).isNull();
            }
            for (Class<? extends Annotation> annotation : ENDPOINT_ANNOTATIONS) {
                assertThat(type.getAnnotation(annotation)).isNull();
            }
        }
    }

    private MarketReadOnlyEvidenceSnapshotDTO completeFixtureSnapshot() {
        return MarketReadOnlyEvidenceSnapshotDTO.builder()
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
                .sourceRef("fixture-snapshot-ref-119")
                .sourceTimeframe("4h")
                .sourceReason("already-ingested fixture structure evidence")
                .sourceWindow("fixture-window-2026-05-21T00:00Z_2026-05-21T04:00Z")
                .ruleId("fixture-review-only-candidate")
                .ruleVersion("p119-fixture-review-only-candidate-v1")
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.FRESH)
                .conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CLEAR)
                .dataQualityScore(95)
                .eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.PRESENT)
                .riskActionGuardContext("review_only_fixture_no_blocker")
                .build();
    }

    private void assertReviewOnly(MarketReadOnlyCandidateResultDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
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
