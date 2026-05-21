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

class InertMarketReadOnlyCandidateGeneratorTest {

    private static final List<Path> SKELETON_SOURCE_PATHS = List.of(
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
            "riskRewardValue"
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
    void nullSnapshotShouldReturnIncompleteReviewOnlyResult() {
        MarketReadOnlyCandidateResultDTO result = generator.review(null);

        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertThat(result.getSnapshotStatus()).isNull();
        assertThat(result.getBlockingReasons()).containsExactly("missing_snapshot");
        assertThat(result.getSourceOwnershipSummary()).isEqualTo("source_owner_summary:missing_snapshot_or_owner");
        assertReviewOnly(result);
    }

    @Test
    void incompleteSnapshotShouldReturnIncompleteAndPreserveBlockingReasons() {
        MarketReadOnlyCandidateResultDTO result = generator.review(validSnapshotBuilder()
                .sourceOwner(null)
                .build());

        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("snapshot_missing:sourceOwner");
        assertReviewOnly(result);
    }

    @Test
    void blockedSnapshotShouldReturnBlockedAndPreserveBlockerEvidence() {
        MarketReadOnlyCandidateResultDTO result = generator.review(validSnapshotBuilder()
                .forbiddenInputMarkers(List.of("latest_price_only"))
                .build());

        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.BLOCKED);
        assertThat(result.getBlockingReasons())
                .contains("direct_forbidden_input:latest_price_only", "snapshot_blocked:latest_price_only");
        assertReviewOnly(result);
    }

    @Test
    void completeSnapshotShouldReturnReviewOnlyCandidateWhenNoDirectBlockersExist() {
        MarketReadOnlyCandidateResultDTO result = generator.review(validSnapshotBuilder().build());

        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo("VALID");
        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.COMPLETE_FOR_REVIEW);
        assertThat(result.getEntryReview()).isEqualTo("entry_review_token:review_only_source_context");
        assertThat(result.getStopReview()).isEqualTo("stop_review_token:review_only_source_context");
        assertThat(result.getTpReview()).isEqualTo("tp_review_token:review_only_source_context");
        assertThat(result.getRrReview()).isEqualTo("rr_review_token:review_only_source_context");
        assertThat(result.getNumericSourceSummary()).isEqualTo("numeric_source_summary:review_token_only");
        assertThat(result.getBlockingReasons()).isEmpty();
        assertReviewOnly(result);
    }

    @Test
    void directForbiddenNoGoAndRiskGuardBlockersShouldReturnBlocked() {
        MarketReadOnlyCandidateResultDTO forbidden = generator.review(validSnapshotBuilder()
                .forbiddenInputMarkers(List.of("ai_text_entry"))
                .build());
        MarketReadOnlyCandidateResultDTO noGo = generator.review(validSnapshotBuilder()
                .noGoEvidenceMarkers(List.of("missing_event_data"))
                .build());
        MarketReadOnlyCandidateResultDTO riskGuard = generator.review(validSnapshotBuilder()
                .riskActionGuardBlockers(List.of("stampede_reverse_blocked"))
                .build());

        assertBlockedWithEvidence(forbidden, "direct_forbidden_input:ai_text_entry");
        assertBlockedWithEvidence(noGo, "direct_no_go:missing_event_data");
        assertBlockedWithEvidence(riskGuard, "direct_risk_action_guard:stampede_reverse_blocked");
    }

    @Test
    void skeletonShouldReturnOnlyStringTokenReviewFields() throws Exception {
        assertThat(MarketReadOnlyCandidateResultDTO.class.getDeclaredField("entryReview").getType())
                .isEqualTo(String.class);
        assertThat(MarketReadOnlyCandidateResultDTO.class.getDeclaredField("stopReview").getType())
                .isEqualTo(String.class);
        assertThat(MarketReadOnlyCandidateResultDTO.class.getDeclaredField("tpReview").getType())
                .isEqualTo(String.class);
        assertThat(MarketReadOnlyCandidateResultDTO.class.getDeclaredField("rrReview").getType())
                .isEqualTo(String.class);
    }

    @Test
    void skeletonSourcesShouldNotCallProductionFactoriesOrDataFetchApis() throws Exception {
        for (Path path : SKELETON_SOURCE_PATHS) {
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
    void skeletonTypesShouldHaveNoSpringEndpointBigDecimalOrActionSurface() {
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

    private void assertBlockedWithEvidence(MarketReadOnlyCandidateResultDTO result, String blockerEvidence) {
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
        assertThat(result.getBlockingReasons()).contains(blockerEvidence);
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
                .ruleVersion("p116-inert-skeleton-v1")
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
