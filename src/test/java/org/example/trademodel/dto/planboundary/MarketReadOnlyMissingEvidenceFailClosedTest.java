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

class MarketReadOnlyMissingEvidenceFailClosedTest {

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
    void nullSnapshotShouldFailClosedWithMissingSnapshot() {
        MarketReadOnlyCandidateResultDTO result = generator.review(null);

        assertIncompleteMissing(result, "missing_snapshot");
    }

    @Test
    void missingEvidenceRefsShouldFailClosedAndPreserveEvidence() {
        assertMissingField(
                builder -> builder.evidenceRefs(null),
                "evidenceRefs"
        );
    }

    @Test
    void emptyEvidenceRefsShouldFailClosedAndPreserveEvidence() {
        assertMissingField(
                builder -> builder.evidenceRefs(List.of()),
                "evidenceRefs"
        );
    }

    @Test
    void missingEvidenceFamiliesShouldFailClosedAndPreserveEvidence() {
        assertMissingField(
                builder -> builder.evidenceFamilies(null),
                "evidenceFamilies"
        );
    }

    @Test
    void missingSourceOwnershipFieldsShouldFailClosedAndPreserveEvidence() {
        assertMissingField(builder -> builder.sourceOwner(null), "sourceOwner");
        assertMissingField(builder -> builder.sourceRef(null), "sourceRef");
        assertMissingField(builder -> builder.sourceTimeframe(null), "sourceTimeframe");
        assertMissingField(builder -> builder.sourceReason(null), "sourceReason");
        assertMissingField(builder -> builder.sourceWindow(null), "sourceWindow");
    }

    @Test
    void missingRuleFreshnessAndQualityFieldsShouldFailClosedAndPreserveEvidence() {
        assertMissingField(builder -> builder.ruleId(null), "ruleId");
        assertMissingField(builder -> builder.ruleVersion(null), "ruleVersion");
        assertMissingField(builder -> builder.freshnessStatus(null), "freshnessStatus");
        assertMissingField(builder -> builder.dataQualityScore(null), "dataQualityScore");
    }

    @Test
    void missingEvidenceStatusFieldsShouldFailClosedAndPreserveEvidence() {
        assertMissingField(builder -> builder.eventEvidenceStatus(null), "eventEvidenceStatus");
        assertMissingField(builder -> builder.liquidityEvidenceStatus(null), "liquidityEvidenceStatus");
        assertMissingField(builder -> builder.wickPinBarEvidenceStatus(null), "wickPinBarEvidenceStatus");
        assertMissingField(builder -> builder.multiTimeframeEvidenceStatus(null), "multiTimeframeEvidenceStatus");
    }

    @Test
    void explicitMissingEvidenceStatusesShouldFailClosedAndPreserveEvidence() {
        assertMissingField(
                builder -> builder.eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.MISSING),
                "eventEvidenceStatus"
        );
        assertMissingField(
                builder -> builder.liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.MISSING),
                "liquidityEvidenceStatus"
        );
        assertMissingField(
                builder -> builder.wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.MISSING),
                "wickPinBarEvidenceStatus"
        );
        assertMissingField(
                builder -> builder.multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.MISSING),
                "multiTimeframeEvidenceStatus"
        );
    }

    @Test
    void missingEvidenceAndOwnershipShouldNeverBecomeReviewOnlyCandidate() {
        List<MarketReadOnlyCandidateResultDTO> results = List.of(
                generator.review(validSnapshotBuilder().evidenceRefs(List.of()).build()),
                generator.review(validSnapshotBuilder().evidenceFamilies(List.of()).build()),
                generator.review(validSnapshotBuilder().sourceOwner(null).build()),
                generator.review(validSnapshotBuilder().sourceRef(null).build()),
                generator.review(validSnapshotBuilder().sourceTimeframe(null).build()),
                generator.review(validSnapshotBuilder().sourceReason(null).build()),
                generator.review(validSnapshotBuilder().sourceWindow(null).build()),
                generator.review(validSnapshotBuilder().ruleId(null).build()),
                generator.review(validSnapshotBuilder().ruleVersion(null).build()),
                generator.review(validSnapshotBuilder().freshnessStatus(null).build()),
                generator.review(validSnapshotBuilder().dataQualityScore(null).build()),
                generator.review(validSnapshotBuilder().eventEvidenceStatus(null).build()),
                generator.review(validSnapshotBuilder().liquidityEvidenceStatus(null).build()),
                generator.review(validSnapshotBuilder().wickPinBarEvidenceStatus(null).build()),
                generator.review(validSnapshotBuilder().multiTimeframeEvidenceStatus(null).build())
        );

        for (MarketReadOnlyCandidateResultDTO result : results) {
            assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
            assertThat(result.getCandidateStatus()).isNotEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
            assertThat(result.getCandidateStatus().name()).isNotEqualTo("VALID");
            assertThat(result.getBlockingReasons()).isNotEmpty();
            assertReviewOnly(result);
        }
    }

    @Test
    void failClosedContractsShouldKeepForbiddenProductionAndRuntimeSurfaceAbsent() throws Exception {
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

    private void assertMissingField(
            Function<MarketReadOnlyEvidenceSnapshotDTO.Builder, MarketReadOnlyEvidenceSnapshotDTO.Builder> mutator,
            String missingField
    ) {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = mutator.apply(validSnapshotBuilder()).build();
        MarketReadOnlyCandidateResultDTO result = generator.review(snapshot);

        assertThat(snapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.INCOMPLETE);
        assertThat(snapshot.getMissingFields()).contains(missingField);
        assertIncompleteMissing(result, "snapshot_missing:" + missingField);
    }

    private void assertIncompleteMissing(MarketReadOnlyCandidateResultDTO result, String missingEvidence) {
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertThat(result.getCandidateStatus()).isNotEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo("VALID");
        assertThat(result.getBlockingReasons()).contains(missingEvidence);
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
                .ruleVersion("p117-missing-evidence-fail-closed-v1")
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
