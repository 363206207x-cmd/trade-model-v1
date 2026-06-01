package org.example.trademodel.service.evidence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.trademodel.dto.evidence.ReviewOnlyEvidenceScoreEntryDTO;
import org.example.trademodel.dto.evidence.ReviewOnlyNormalizedEvidenceDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyEvidenceNormalizationAssemblerTest {

    private final ReviewOnlyEvidenceNormalizationAssembler assembler =
            new ReviewOnlyEvidenceNormalizationAssembler();

    @Test
    void validReviewOnlyEvidenceScoreEntryAssemblesReviewOnlyNormalizedEvidence() {
        ReviewOnlyNormalizedEvidenceDTO normalized = assembler.assemble(validEntry());

        assertThat(normalized.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(normalized.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(normalized.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(normalized.getScanOutputStatus()).isEqualTo("REVIEW_ONLY_SCAN_OUTPUT");
        assertThat(normalized.getEvidenceEntryStatus()).isEqualTo("REVIEW_ONLY_EVIDENCE_SCORE_ENTRY");
        assertThat(normalized.getEvidenceNormalizationStatus()).isEqualTo("REVIEW_ONLY_EVIDENCE_NORMALIZATION");
        assertThat(normalized.getAllowedNextStep()).isEqualTo("READY_FOR_SCORE_INPUT_PRECHECK_REVIEW_ONLY");
        assertThat(normalized.isBlocked()).isFalse();
        assertThat(normalized.isFailClosed()).isFalse();
        assertSafetyFlags(normalized);
    }

    @Test
    void blockedEvidenceScoreEntryAssemblesBlockedNormalizedEvidence() {
        ReviewOnlyNormalizedEvidenceDTO normalized = assembler.assemble(blockedEntry());

        assertThat(normalized.getScanOutputStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(normalized.getEvidenceEntryStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(normalized.getEvidenceNormalizationStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(normalized.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(normalized.isBlocked()).isTrue();
        assertThat(normalized.isFailClosed()).isTrue();
        assertSafetyFlags(normalized);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyNormalizedEvidenceDTO normalized = assembler.assemble(blockedEntry());

        assertThat(normalized.getBlockingReasons())
                .contains("REVIEW_ONLY_SCAN_OUTPUT_FAIL_CLOSED", "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_FAIL_CLOSED");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyNormalizedEvidenceDTO normalized = assembler.assemble(validEntry());

        assertThat(normalized.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyNormalizedEvidenceDTO normalized = assembler.assemble(validEntry());

        assertThat(normalized.getRequestedTimeframes()).containsExactly("15m", "1h");
        normalized.getRequestedTimeframes().add("mutated");
        assertThat(normalized.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyNormalizedEvidenceDTO normalized = assembler.assemble(validEntry());

        assertThat(normalized.getBlockingReasons())
                .contains("REVIEW_ONLY_EVIDENCE_SCORE_ENTRY", "REVIEW_ONLY_EVIDENCE_NORMALIZATION");
        normalized.getBlockingReasons().add("mutated");
        assertThat(normalized.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyNormalizedEvidenceDTO normalized = assembler.assemble(validEntryWithRiskBlockers());

        assertThat(normalized.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(normalized.getAllowedNextStep()).isEqualTo("WAIT_FOR_SCORE_AUTHORIZATION");
        normalized.getRiskBlockers().add("mutated");
        assertThat(normalized.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void normalizedEvidenceIsReviewOnly() {
        assertThat(assembler.assemble(validEntry()).isReviewOnly()).isTrue();
    }

    @Test
    void normalizedEvidenceIsNotTradeInstruction() {
        assertThat(assembler.assemble(validEntry()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void normalizedEvidenceIsManualReviewRequired() {
        assertThat(assembler.assemble(validEntry()).isManualReviewRequired()).isTrue();
    }

    @Test
    void normalizedEvidenceDoesNotContainRealEvidenceItemScoreItemOrPointFieldsByReflection() {
        assertNoFragmentInFields(
                ReviewOnlyNormalizedEvidenceDTO.class,
                List.of(
                        "evidenceitem",
                        "scoreitem",
                        "trend",
                        "liquidity",
                        "macro",
                        "confidence",
                        "candidate",
                        "push",
                        "readiness",
                        "pointgeneration",
                        "entryzone",
                        "stopzone",
                        "takeprofit",
                        "targetprofit",
                        "riskreward",
                        "orderintent",
                        "executionintent"
                )
        );
        assertNoExactField(ReviewOnlyNormalizedEvidenceDTO.class, "tp");
        assertNoExactField(ReviewOnlyNormalizedEvidenceDTO.class, "rr");
    }

    @Test
    void assemblerHasNoMarketQuoteClientOrBinanceMarketQuoteClientDependency() {
        assertNoForbiddenSurface(List.of("MarketQuoteClient", "BinanceMarketQuoteClient"));
        assertMainSourcesDoNotContain(List.of("MarketQuoteClient", "BinanceMarketQuoteClient"));
    }

    @Test
    void assemblerDoesNotReadRuntimeLiveOrExternalData() {
        assertNoForbiddenSurface(List.of(
                "Runtime",
                "Live",
                "External",
                "DataSource",
                "Provider",
                "Jdbc",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp"
        ));
        assertMainSourcesDoNotContain(List.of(
                "Runtime",
                "Live",
                "External",
                "DataSource",
                "Provider",
                "Jdbc",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp"
        ));
    }

    @Test
    void assemblerDoesNotCreateScoreCandidatePushReadinessPointOrTradingBehavior() {
        assertNoForbiddenSurface(List.of(
                "EvidenceItem",
                "ScoreItem",
                "Candidate",
                "Push",
                "Readiness",
                "Point",
                "Trading",
                "Order",
                "Execution"
        ));
        assertMainSourcesDoNotContain(List.of(
                "EvidenceItem",
                "ScoreItem",
                "WatchlistScanScore",
                "ScoreService",
                "EvidenceService",
                "Candidate",
                "Push",
                "Readiness",
                "PointGeneration",
                "Trading",
                "Order",
                "Execution"
        ));
    }

    @Test
    void assemblerWorksWithoutSpringContext() {
        ReviewOnlyEvidenceNormalizationAssembler plainAssembler =
                new ReviewOnlyEvidenceNormalizationAssembler();

        assertThat(plainAssembler.assemble(validEntry()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyEvidenceNormalizationAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedBeforeAnyRead() {
        ReviewOnlyNormalizedEvidenceDTO normalized = assembler.assemble(null);

        assertThat(normalized.getEvidenceNormalizationStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(normalized.getAllowedNextStep()).isEqualTo("BLOCKED_BY_EVIDENCE_ENTRY");
        assertThat(normalized.getBlockingReasons()).contains("REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_MISSING");
        assertThat(normalized.isBlocked()).isTrue();
        assertThat(normalized.isFailClosed()).isTrue();
        assertSafetyFlags(normalized);
    }

    private ReviewOnlyEvidenceScoreEntryDTO validEntry() {
        return ReviewOnlyEvidenceScoreEntryDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCAN_OUTPUT",
                List.of("REVIEW_ONLY_EVIDENCE_SCORE_ENTRY"),
                List.of(),
                "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY",
                "READY_FOR_EVIDENCE_NORMALIZATION_REVIEW_ONLY",
                "Review-only scan output is ready for evidence/score normalization review only."
        );
    }

    private ReviewOnlyEvidenceScoreEntryDTO validEntryWithRiskBlockers() {
        return ReviewOnlyEvidenceScoreEntryDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCAN_OUTPUT",
                List.of("REVIEW_ONLY_EVIDENCE_SCORE_ENTRY"),
                List.of("stampede_review", "risk_action_guard_required"),
                "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY",
                "READY_FOR_EVIDENCE_NORMALIZATION_REVIEW_ONLY",
                "Review-only scan output is ready for evidence/score normalization review only."
        );
    }

    private ReviewOnlyEvidenceScoreEntryDTO blockedEntry() {
        return ReviewOnlyEvidenceScoreEntryDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                List.of("REVIEW_ONLY_SCAN_OUTPUT_FAIL_CLOSED", "BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "BLOCKED_FAIL_CLOSED",
                "FIX_INPUT_CONTRACT",
                "Review-only evidence/score entry remains blocked and fail-closed."
        );
    }

    private void assertSafetyFlags(ReviewOnlyNormalizedEvidenceDTO normalized) {
        assertThat(normalized.isReviewOnly()).isTrue();
        assertThat(normalized.isNotTradeInstruction()).isTrue();
        assertThat(normalized.isManualReviewRequired()).isTrue();
    }

    private void assertNoFragmentInFields(Class<?> type, List<String> forbiddenFragments) {
        for (Field field : type.getDeclaredFields()) {
            assertNoFragment(field.getName(), forbiddenFragments);
        }
    }

    private void assertNoExactField(Class<?> type, String fieldName) {
        for (Field field : type.getDeclaredFields()) {
            assertThat(field.getName()).isNotEqualToIgnoringCase(fieldName);
        }
    }

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : List.of(
                ReviewOnlyEvidenceNormalizationAssembler.class,
                ReviewOnlyNormalizedEvidenceDTO.class
        )) {
            for (Annotation annotation : type.getAnnotations()) {
                assertNoFragment(annotation.annotationType().getName(), forbiddenFragments);
            }
            for (Field field : type.getDeclaredFields()) {
                assertNoFragment(field.getName(), forbiddenFragments);
                assertNoFragment(field.getType().getSimpleName(), forbiddenFragments);
            }
            for (Method method : type.getDeclaredMethods()) {
                assertNoFragment(method.getName(), forbiddenFragments);
                assertNoFragment(method.getReturnType().getSimpleName(), forbiddenFragments);
                for (Class<?> parameterType : method.getParameterTypes()) {
                    assertNoFragment(parameterType.getSimpleName(), forbiddenFragments);
                }
            }
        }
    }

    private void assertMainSourcesDoNotContain(List<String> forbiddenFragments) {
        for (String source : mainSources()) {
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(source).doesNotContain(forbiddenFragment);
            }
        }
    }

    private List<String> mainSources() {
        try {
            return List.of(
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/service/evidence/"
                                    + "ReviewOnlyEvidenceNormalizationAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/evidence/"
                                    + "ReviewOnlyNormalizedEvidenceDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only evidence normalization source files", ex);
        }
    }

    private Constructor<ReviewOnlyEvidenceNormalizationAssembler> defaultConstructor() {
        try {
            return ReviewOnlyEvidenceNormalizationAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyEvidenceNormalizationAssembler.class.getDeclaredFields()).stream()
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .toList();
    }

    private void assertNoFragment(String value, List<String> forbiddenFragments) {
        String normalized = value.toLowerCase();
        for (String forbiddenFragment : forbiddenFragments) {
            assertThat(normalized).doesNotContain(forbiddenFragment.toLowerCase());
        }
    }
}
