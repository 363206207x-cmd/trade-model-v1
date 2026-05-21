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

class MarketReadOnlyCandidateResultDTOTest {

    private static final List<Path> DTO_SOURCE_PATHS = List.of(
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateResultDTO.java"),
            Path.of("src/main/java/org/example/trademodel/dto/planboundary/MarketReadOnlyCandidateStatusEnum.java")
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
    void completeSnapshotShouldProduceReviewOnlyCandidateOnly() {
        MarketReadOnlyCandidateResultDTO result = completeCandidate().build();

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getTimeframe()).isEqualTo("1h");
        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.COMPLETE_FOR_REVIEW);
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE);
        assertThat(result.getCandidateStatus().name()).isNotEqualTo("VALID");
        assertThat(result.getEntryReview()).isEqualTo("entry_review_token:structure_confirmation_zone");
        assertThat(result.getStopReview()).isEqualTo("stop_review_token:structural_invalidation_buffer");
        assertThat(result.getTpReview()).isEqualTo("tp_review_token:structure_target");
        assertThat(result.getRrReview()).isEqualTo("rr_review_token:fixture_ratio_context");
        assertThat(result.getSourceOwnershipSummary()).isEqualTo("source_owner:market-structure-snapshot");
        assertThat(result.getNumericSourceSummary()).isEqualTo("numeric_source:review_token_only");
        assertThat(result.getFreshnessStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.FRESH);
        assertThat(result.getSourceWindow()).isEqualTo("2026-05-21T08:00Z/2026-05-21T12:00Z");
        assertThat(result.getRuleVersion()).isEqualTo("p115-read-only-candidate-contract-v1");
        assertThat(result.getConflictFamilyStatus()).isEqualTo(MarketReadOnlyEvidenceStatusEnum.CLEAR);
        assertThat(result.getDataQualityScore()).isEqualTo(90);
        assertThat(result.getRiskActionGuardReview()).isEqualTo("risk_action_guard:review_only_no_blocker");
        assertThat(result.getBlockingReasons()).isEmpty();
        assertReviewOnly(result);
    }

    @Test
    void missingSnapshotShouldRemainIncomplete() {
        MarketReadOnlyCandidateResultDTO result = completeCandidate()
                .snapshot(null)
                .build();

        assertThat(result.getSnapshotStatus()).isNull();
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).containsExactly("missing_snapshot");
        assertReviewOnly(result);
    }

    @Test
    void incompleteSnapshotShouldKeepCandidateIncompleteAndPreserveEvidence() {
        MarketReadOnlyEvidenceSnapshotDTO incompleteSnapshot = validSnapshotBuilder()
                .sourceOwner(null)
                .build();
        MarketReadOnlyCandidateResultDTO result = completeCandidate()
                .snapshot(incompleteSnapshot)
                .build();

        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.INCOMPLETE);
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("snapshot_missing:sourceOwner");
        assertReviewOnly(result);
    }

    @Test
    void blockedSnapshotShouldKeepCandidateBlockedAndPreserveEvidence() {
        MarketReadOnlyEvidenceSnapshotDTO blockedSnapshot = validSnapshotBuilder()
                .forbiddenInputMarkers(List.of("latest_price_only"))
                .build();
        MarketReadOnlyCandidateResultDTO result = completeCandidate()
                .snapshot(blockedSnapshot)
                .build();

        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.BLOCKED);
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
        assertThat(result.getBlockingReasons()).contains("snapshot_blocked:latest_price_only");
        assertReviewOnly(result);
    }

    @Test
    void directForbiddenNoGoOrRiskGuardEvidenceShouldBlockCandidate() {
        assertBlockedByDirectEvidence("forbidden_input:ai_text");
        assertBlockedByDirectEvidence("no_go:missing_event_data");
        assertBlockedByDirectEvidence("risk_action_guard:stampede_reverse_blocked");
    }

    @Test
    void blockingReasonsShouldBeDefensivelyCopied() {
        List<String> reasons = new ArrayList<>(List.of("risk_action_guard:blocked"));
        MarketReadOnlyCandidateResultDTO result = completeCandidate()
                .blockingReasons(reasons)
                .build();

        reasons.add("mutated");

        assertThat(result.getBlockingReasons()).containsExactly("risk_action_guard:blocked");

        List<String> returnedReasons = result.getBlockingReasons();
        returnedReasons.add("mutated");

        assertThat(result.getBlockingReasons()).containsExactly("risk_action_guard:blocked");
    }

    @Test
    void candidateStatusesShouldRemainReviewOnlyStatuses() {
        assertThat(MarketReadOnlyCandidateStatusEnum.values()).containsExactly(
                MarketReadOnlyCandidateStatusEnum.INCOMPLETE,
                MarketReadOnlyCandidateStatusEnum.BLOCKED,
                MarketReadOnlyCandidateStatusEnum.REVIEW_ONLY_CANDIDATE
        );
        assertThat(Arrays.stream(MarketReadOnlyCandidateStatusEnum.values()).map(Enum::name))
                .doesNotContain("VALID");
    }

    @Test
    void reviewFieldsShouldRemainStringTokensOnly() throws Exception {
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

    private void assertBlockedByDirectEvidence(String reason) {
        MarketReadOnlyCandidateResultDTO result = completeCandidate()
                .blockingReasons(List.of(reason))
                .build();

        assertThat(result.getSnapshotStatus()).isEqualTo(MarketReadOnlySnapshotStatusEnum.COMPLETE_FOR_REVIEW);
        assertThat(result.getCandidateStatus()).isEqualTo(MarketReadOnlyCandidateStatusEnum.BLOCKED);
        assertThat(result.getBlockingReasons()).contains(reason);
        assertReviewOnly(result);
    }

    private void assertReviewOnly(MarketReadOnlyCandidateResultDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.getReviewMode()).isEqualTo(SourceTraceEntrySourceReviewModeEnum.REVIEW_ONLY);
    }

    private MarketReadOnlyCandidateResultDTO.Builder completeCandidate() {
        return MarketReadOnlyCandidateResultDTO.builder()
                .snapshot(validSnapshotBuilder().build())
                .entryReview("entry_review_token:structure_confirmation_zone")
                .stopReview("stop_review_token:structural_invalidation_buffer")
                .tpReview("tp_review_token:structure_target")
                .rrReview("rr_review_token:fixture_ratio_context")
                .sourceOwnershipSummary("source_owner:market-structure-snapshot")
                .numericSourceSummary("numeric_source:review_token_only")
                .riskActionGuardReview("risk_action_guard:review_only_no_blocker");
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
                .ruleVersion("p115-read-only-candidate-contract-v1")
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
        addWithNested(types, MarketReadOnlyCandidateResultDTO.class);
        types.add(MarketReadOnlyCandidateStatusEnum.class);
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
