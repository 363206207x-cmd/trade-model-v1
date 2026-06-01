package org.example.trademodel.service.score;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.trademodel.dto.evidence.ReviewOnlyNormalizedEvidenceDTO;
import org.example.trademodel.dto.score.ReviewOnlyScoreInputPrecheckDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyScoreInputPrecheckAssemblerTest {

    private final ReviewOnlyScoreInputPrecheckAssembler assembler =
            new ReviewOnlyScoreInputPrecheckAssembler();

    @Test
    void validReviewOnlyNormalizedEvidenceAssemblesReviewOnlyScoreInputPrecheck() {
        ReviewOnlyScoreInputPrecheckDTO precheck = assembler.assemble(validNormalizedEvidence());

        assertThat(precheck.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(precheck.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(precheck.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(precheck.getEvidenceNormalizationStatus()).isEqualTo("REVIEW_ONLY_EVIDENCE_NORMALIZATION");
        assertThat(precheck.getScoreInputPrecheckStatus()).isEqualTo("REVIEW_ONLY_SCORE_INPUT_PRECHECK");
        assertThat(precheck.getAllowedNextStep()).isEqualTo("READY_FOR_REVIEW_ONLY_SCORE_ASSEMBLY");
        assertThat(precheck.isBlocked()).isFalse();
        assertThat(precheck.isFailClosed()).isFalse();
        assertSafetyFlags(precheck);
    }

    @Test
    void blockedNormalizedEvidenceAssemblesBlockedScoreInputPrecheck() {
        ReviewOnlyScoreInputPrecheckDTO precheck = assembler.assemble(blockedNormalizedEvidence());

        assertThat(precheck.getEvidenceNormalizationStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(precheck.getScoreInputPrecheckStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(precheck.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(precheck.isBlocked()).isTrue();
        assertThat(precheck.isFailClosed()).isTrue();
        assertSafetyFlags(precheck);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyScoreInputPrecheckDTO precheck = assembler.assemble(blockedNormalizedEvidence());

        assertThat(precheck.getBlockingReasons())
                .contains("REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_FAIL_CLOSED",
                        "REVIEW_ONLY_NORMALIZED_EVIDENCE_FAIL_CLOSED");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyScoreInputPrecheckDTO precheck = assembler.assemble(validNormalizedEvidence());

        assertThat(precheck.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyScoreInputPrecheckDTO precheck = assembler.assemble(validNormalizedEvidence());

        assertThat(precheck.getRequestedTimeframes()).containsExactly("15m", "1h");
        precheck.getRequestedTimeframes().add("mutated");
        assertThat(precheck.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyScoreInputPrecheckDTO precheck = assembler.assemble(validNormalizedEvidence());

        assertThat(precheck.getBlockingReasons())
                .contains("REVIEW_ONLY_EVIDENCE_NORMALIZATION", "REVIEW_ONLY_SCORE_INPUT_PRECHECK");
        precheck.getBlockingReasons().add("mutated");
        assertThat(precheck.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyScoreInputPrecheckDTO precheck = assembler.assemble(validNormalizedEvidenceWithRiskBlockers());

        assertThat(precheck.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(precheck.getAllowedNextStep()).isEqualTo("WAIT_FOR_SCORE_AUTHORIZATION");
        precheck.getRiskBlockers().add("mutated");
        assertThat(precheck.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void precheckOutputIsReviewOnly() {
        assertThat(assembler.assemble(validNormalizedEvidence()).isReviewOnly()).isTrue();
    }

    @Test
    void precheckOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validNormalizedEvidence()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void precheckOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validNormalizedEvidence()).isManualReviewRequired()).isTrue();
    }

    @Test
    void precheckOutputDoesNotContainRealScoreFieldsByReflection() {
        assertNoFragmentInFields(
                ReviewOnlyScoreInputPrecheckDTO.class,
                List.of(
                        "scoreitem",
                        "trend",
                        "liquidity",
                        "macro",
                        "confidence",
                        "leverage",
                        "sentiment",
                        "finalscore",
                        "evidenceitem",
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
        assertNoExactField(ReviewOnlyScoreInputPrecheckDTO.class, "score");
        assertNoExactField(ReviewOnlyScoreInputPrecheckDTO.class, "tp");
        assertNoExactField(ReviewOnlyScoreInputPrecheckDTO.class, "rr");
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
    void assemblerDoesNotCreateEvidenceScoreCandidatePushReadinessPointOrTradingBehavior() {
        assertNoForbiddenSurface(List.of(
                "EvidenceItem",
                "ScoreItem",
                "WatchlistScanScore",
                "ScoreService",
                "EvidenceService",
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
        ReviewOnlyScoreInputPrecheckAssembler plainAssembler =
                new ReviewOnlyScoreInputPrecheckAssembler();

        assertThat(plainAssembler.assemble(validNormalizedEvidence()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyScoreInputPrecheckAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedBeforeAnyRead() {
        ReviewOnlyScoreInputPrecheckDTO precheck = assembler.assemble(null);

        assertThat(precheck.getScoreInputPrecheckStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(precheck.getAllowedNextStep()).isEqualTo("BLOCKED_BY_EVIDENCE_NORMALIZATION");
        assertThat(precheck.getBlockingReasons()).contains("REVIEW_ONLY_NORMALIZED_EVIDENCE_MISSING");
        assertThat(precheck.isBlocked()).isTrue();
        assertThat(precheck.isFailClosed()).isTrue();
        assertSafetyFlags(precheck);
    }

    private ReviewOnlyNormalizedEvidenceDTO validNormalizedEvidence() {
        return ReviewOnlyNormalizedEvidenceDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCAN_OUTPUT",
                "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY",
                "REVIEW_ONLY_EVIDENCE_NORMALIZATION",
                List.of("REVIEW_ONLY_EVIDENCE_NORMALIZATION"),
                List.of(),
                "READY_FOR_SCORE_INPUT_PRECHECK_REVIEW_ONLY",
                "Review-only evidence/score entry is ready for score input precheck review only."
        );
    }

    private ReviewOnlyNormalizedEvidenceDTO validNormalizedEvidenceWithRiskBlockers() {
        return ReviewOnlyNormalizedEvidenceDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCAN_OUTPUT",
                "REVIEW_ONLY_EVIDENCE_SCORE_ENTRY",
                "REVIEW_ONLY_EVIDENCE_NORMALIZATION",
                List.of("REVIEW_ONLY_EVIDENCE_NORMALIZATION"),
                List.of("stampede_review", "risk_action_guard_required"),
                "READY_FOR_SCORE_INPUT_PRECHECK_REVIEW_ONLY",
                "Review-only evidence/score entry is ready for score input precheck review only."
        );
    }

    private ReviewOnlyNormalizedEvidenceDTO blockedNormalizedEvidence() {
        return ReviewOnlyNormalizedEvidenceDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                List.of("REVIEW_ONLY_EVIDENCE_SCORE_ENTRY_FAIL_CLOSED", "BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "FIX_INPUT_CONTRACT",
                "Review-only evidence normalization remains blocked and fail-closed."
        );
    }

    private void assertSafetyFlags(ReviewOnlyScoreInputPrecheckDTO precheck) {
        assertThat(precheck.isReviewOnly()).isTrue();
        assertThat(precheck.isNotTradeInstruction()).isTrue();
        assertThat(precheck.isManualReviewRequired()).isTrue();
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
                ReviewOnlyScoreInputPrecheckAssembler.class,
                ReviewOnlyScoreInputPrecheckDTO.class
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
                            "src/main/java/org/example/trademodel/service/score/"
                                    + "ReviewOnlyScoreInputPrecheckAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/score/"
                                    + "ReviewOnlyScoreInputPrecheckDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only score input precheck source files", ex);
        }
    }

    private Constructor<ReviewOnlyScoreInputPrecheckAssembler> defaultConstructor() {
        try {
            return ReviewOnlyScoreInputPrecheckAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyScoreInputPrecheckAssembler.class.getDeclaredFields()).stream()
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
