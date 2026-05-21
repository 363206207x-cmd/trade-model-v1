package org.example.trademodel.dto.planboundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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

class MarketReadOnlyEvidenceSnapshotDTOTest {

    private static final List<Path> DTO_SOURCE_PATHS = List.of(
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyEvidenceSnapshotDTO.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyEvidenceFamilyEnum.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyEvidenceStatusEnum.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlySnapshotStatusEnum.java")
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

    @Test
    void completeSnapshotShouldRemainReviewOnlyAndCompleteForReview() {
        MarketReadOnlyEvidenceSnapshotDTO snapshot = validBuilder().build();

        assertThat(snapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.COMPLETE_FOR_REVIEW);
        assertThat(snapshot.getSnapshotStatus().name()).isNotEqualTo("VALID");
        assertThat(snapshot.getEvidenceFamilies()).containsExactly(
                MarketReadOnlyEvidenceFamilyEnum.MARKET_STRUCTURE,
                MarketReadOnlyEvidenceFamilyEnum.KLINE_DERIVED_STRUCTURE,
                MarketReadOnlyEvidenceFamilyEnum.ATR_VOLATILITY,
                MarketReadOnlyEvidenceFamilyEnum.LIQUIDITY_TARGET,
                MarketReadOnlyEvidenceFamilyEnum.PRIOR_HIGH_LOW,
                MarketReadOnlyEvidenceFamilyEnum.EVENT,
                MarketReadOnlyEvidenceFamilyEnum.WICK_PIN_BAR,
                MarketReadOnlyEvidenceFamilyEnum.MULTI_TIMEFRAME
        );
        assertThat(snapshot.getMissingFields()).isEmpty();
        assertThat(snapshot.getBlockerEvidence()).isEmpty();
        assertReviewOnly(snapshot);
    }

    @Test
    void missingRequiredOwnerEvidenceShouldRemainIncomplete() {
        assertIncompleteWithMissingField(validBuilder().sourceOwner(null).build(), "sourceOwner");
        assertIncompleteWithMissingField(validBuilder().evidenceRefs(List.of()).build(), "evidenceRefs");
        assertIncompleteWithMissingField(validBuilder().ruleId(null).build(), "ruleId");
        assertIncompleteWithMissingField(validBuilder().ruleVersion(null).build(), "ruleVersion");
        assertIncompleteWithMissingField(validBuilder().freshnessStatus(null).build(), "freshnessStatus");
        assertIncompleteWithMissingField(validBuilder().sourceRef(null).build(), "sourceRef");
        assertIncompleteWithMissingField(validBuilder().sourceTimeframe(null).build(), "sourceTimeframe");
        assertIncompleteWithMissingField(validBuilder().sourceReason(null).build(), "sourceReason");
        assertIncompleteWithMissingField(validBuilder().sourceWindow(null).build(), "sourceWindow");
        assertIncompleteWithMissingField(validBuilder().dataQualityScore(null).build(), "dataQualityScore");
        assertIncompleteWithMissingField(validBuilder().eventEvidenceStatus(null).build(), "eventEvidenceStatus");
    }

    @Test
    void missingEvidenceStatusValuesShouldRemainIncomplete() {
        assertIncompleteWithMissingField(
                validBuilder().eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.MISSING).build(),
                "eventEvidenceStatus"
        );
        assertIncompleteWithMissingField(
                validBuilder().liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.MISSING).build(),
                "liquidityEvidenceStatus"
        );
        assertIncompleteWithMissingField(
                validBuilder().wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.MISSING).build(),
                "wickPinBarEvidenceStatus"
        );
        assertIncompleteWithMissingField(
                validBuilder().multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.MISSING).build(),
                "multiTimeframeEvidenceStatus"
        );
    }

    @Test
    void staleSourceWindowShouldRemainIncompleteUnlessUnsafeEvidenceIsPresent() {
        MarketReadOnlyEvidenceSnapshotDTO staleSnapshot = validBuilder()
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.STALE)
                .build();

        assertThat(staleSnapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.INCOMPLETE);
        assertThat(staleSnapshot.getBlockerEvidence()).contains("stale_source_window");
        assertReviewOnly(staleSnapshot);

        MarketReadOnlyEvidenceSnapshotDTO unsafeStaleSnapshot = validBuilder()
                .freshnessStatus(MarketReadOnlyEvidenceStatusEnum.STALE)
                .noGoEvidenceMarkers(List.of("liquidity_stress_stampede"))
                .build();

        assertThat(unsafeStaleSnapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.BLOCKED);
        assertThat(unsafeStaleSnapshot.getBlockerEvidence())
                .contains("stale_source_window", "liquidity_stress_stampede");
        assertReviewOnly(unsafeStaleSnapshot);
    }

    @Test
    void forbiddenNoGoAndRiskActionGuardMarkersShouldBlockReviewCompletion() {
        assertBlockedWithEvidence(
                validBuilder().forbiddenInputMarkers(List.of("latest_price_only")).build(),
                "latest_price_only"
        );
        assertBlockedWithEvidence(
                validBuilder().noGoEvidenceMarkers(List.of("missing_event_data")).build(),
                "missing_event_data"
        );
        assertBlockedWithEvidence(
                validBuilder().riskActionGuardBlockers(List.of("risk_action_guard_stampede")).build(),
                "risk_action_guard_stampede"
        );
    }

    @Test
    void noGoEvidenceStatusesShouldBlockReviewCompletion() {
        assertBlockedWithEvidence(
                validBuilder().eventEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.NO_GO).build(),
                "eventEvidenceStatus:NO_GO"
        );
        assertBlockedWithEvidence(
                validBuilder().liquidityEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.BLOCKED).build(),
                "liquidityEvidenceStatus:BLOCKED"
        );
        assertBlockedWithEvidence(
                validBuilder().wickPinBarEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.FORBIDDEN_INPUT).build(),
                "wickPinBarEvidenceStatus:FORBIDDEN_INPUT"
        );
        assertBlockedWithEvidence(
                validBuilder().multiTimeframeEvidenceStatus(MarketReadOnlyEvidenceStatusEnum.CONFLICT).build(),
                "multiTimeframeEvidenceStatus:CONFLICT"
        );
        assertBlockedWithEvidence(
                validBuilder().conflictFamilyStatus(MarketReadOnlyEvidenceStatusEnum.CONFLICT).build(),
                "conflictFamilyStatus:CONFLICT"
        );
    }

    @Test
    void snapshotShouldDefensivelyCopyMutableInputsAndOutputs() {
        List<String> refs = new ArrayList<>(List.of("structure-window-1"));
        List<MarketReadOnlyEvidenceFamilyEnum> families =
                new ArrayList<>(List.of(MarketReadOnlyEvidenceFamilyEnum.MARKET_STRUCTURE));
        MarketReadOnlyEvidenceSnapshotDTO snapshot = validBuilder()
                .evidenceRefs(refs)
                .evidenceFamilies(families)
                .build();

        refs.add("structure-window-2");
        families.add(MarketReadOnlyEvidenceFamilyEnum.EVENT);

        assertThat(snapshot.getEvidenceRefs()).containsExactly("structure-window-1");
        assertThat(snapshot.getEvidenceFamilies()).containsExactly(MarketReadOnlyEvidenceFamilyEnum.MARKET_STRUCTURE);

        List<String> returnedRefs = snapshot.getEvidenceRefs();
        returnedRefs.add("mutated");

        assertThat(snapshot.getEvidenceRefs()).containsExactly("structure-window-1");
    }

    @Test
    void dtoSourcesShouldNotCallProductionFactoriesOrDataFetchApis() throws Exception {
        for (Path path : DTO_SOURCE_PATHS) {
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
    void dtoTypesShouldHaveNoProductionDtoRealTradingValueOrActionSurface() {
        for (Class<?> type : guardedTypes()) {
            assertThat(returnTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(parameterTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(fieldTypesOf(type)).doesNotContain(BoundaryCandidateDTO.class, BigDecimal.class);
            assertThat(publicSurfaceOf(type)).noneMatch(this::containsForbiddenSurfaceTerm);
            assertThat(fieldNamesOf(type)).noneMatch(this::containsForbiddenSurfaceTerm);
        }
    }

    @Test
    void dtoTypesShouldHaveNoSpringOrEndpointAnnotations() {
        for (Class<?> type : guardedTypes()) {
            for (Class<? extends Annotation> annotation : SPRING_ANNOTATIONS) {
                assertThat(type.getAnnotation(annotation)).isNull();
            }
            for (Class<? extends Annotation> annotation : ENDPOINT_ANNOTATIONS) {
                assertThat(type.getAnnotation(annotation)).isNull();
            }
        }
    }

    private void assertIncompleteWithMissingField(MarketReadOnlyEvidenceSnapshotDTO snapshot, String missingField) {
        assertThat(snapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.INCOMPLETE);
        assertThat(snapshot.getMissingFields()).contains(missingField);
        assertReviewOnly(snapshot);
    }

    private void assertBlockedWithEvidence(MarketReadOnlyEvidenceSnapshotDTO snapshot, String blockerEvidence) {
        assertThat(snapshot.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.BLOCKED);
        assertThat(snapshot.getBlockerEvidence()).contains(blockerEvidence);
        assertReviewOnly(snapshot);
    }

    private void assertReviewOnly(MarketReadOnlyEvidenceSnapshotDTO snapshot) {
        assertThat(snapshot.isManualReviewRequired()).isTrue();
        assertThat(snapshot.isNotTradeInstruction()).isTrue();
        assertThat(snapshot.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
    }

    private MarketReadOnlyEvidenceSnapshotDTO.Builder validBuilder() {
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
                .ruleVersion("p114-fixture-contract-v1")
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
        List<Class<?>> types = new ArrayList<>();
        addWithNested(types, MarketReadOnlyEvidenceSnapshotDTO.class);
        types.add(MarketReadOnlyEvidenceFamilyEnum.class);
        types.add(MarketReadOnlyEvidenceStatusEnum.class);
        types.add(MarketReadOnlySnapshotStatusEnum.class);
        return types;
    }

    private void addWithNested(List<Class<?>> types, Class<?> type) {
        types.add(type);
        for (Class<?> nestedType : type.getDeclaredClasses()) {
            addWithNested(types, nestedType);
        }
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
