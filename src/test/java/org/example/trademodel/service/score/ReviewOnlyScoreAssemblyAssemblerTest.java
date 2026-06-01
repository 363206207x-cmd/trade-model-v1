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
import org.example.trademodel.dto.score.ReviewOnlyScoreAssemblyDTO;
import org.example.trademodel.dto.score.ReviewOnlyScoreInputPrecheckDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyScoreAssemblyAssemblerTest {

    private final ReviewOnlyScoreAssemblyAssembler assembler =
            new ReviewOnlyScoreAssemblyAssembler();

    @Test
    void validReviewOnlyScoreInputPrecheckAssemblesReviewOnlyScoreAssembly() {
        ReviewOnlyScoreAssemblyDTO assembly = assembler.assemble(validPrecheck());

        assertThat(assembly.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(assembly.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(assembly.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(assembly.getScoreInputPrecheckStatus()).isEqualTo("REVIEW_ONLY_SCORE_INPUT_PRECHECK");
        assertThat(assembly.getScoreAssemblyStatus()).isEqualTo("REVIEW_ONLY_SCORE_ASSEMBLY");
        assertThat(assembly.getAllowedNextStep())
                .isEqualTo("READY_FOR_SCORE_TO_CANDIDATE_HANDOFF_REVIEW_ONLY");
        assertThat(assembly.isBlocked()).isFalse();
        assertThat(assembly.isFailClosed()).isFalse();
        assertSafetyFlags(assembly);
    }

    @Test
    void blockedScoreInputPrecheckAssemblesBlockedScoreAssembly() {
        ReviewOnlyScoreAssemblyDTO assembly = assembler.assemble(blockedPrecheck());

        assertThat(assembly.getScoreInputPrecheckStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(assembly.getScoreAssemblyStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(assembly.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(assembly.isBlocked()).isTrue();
        assertThat(assembly.isFailClosed()).isTrue();
        assertSafetyFlags(assembly);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyScoreAssemblyDTO assembly = assembler.assemble(blockedPrecheck());

        assertThat(assembly.getBlockingReasons())
                .contains("REVIEW_ONLY_SCORE_INPUT_PRECHECK_FAIL_CLOSED",
                        "REVIEW_ONLY_NORMALIZED_EVIDENCE_FAIL_CLOSED");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyScoreAssemblyDTO assembly = assembler.assemble(validPrecheck());

        assertThat(assembly.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyScoreAssemblyDTO assembly = assembler.assemble(validPrecheck());

        assertThat(assembly.getRequestedTimeframes()).containsExactly("15m", "1h");
        assembly.getRequestedTimeframes().add("mutated");
        assertThat(assembly.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyScoreAssemblyDTO assembly = assembler.assemble(validPrecheck());

        assertThat(assembly.getBlockingReasons())
                .contains("REVIEW_ONLY_SCORE_INPUT_PRECHECK", "REVIEW_ONLY_SCORE_ASSEMBLY");
        assembly.getBlockingReasons().add("mutated");
        assertThat(assembly.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyScoreAssemblyDTO assembly = assembler.assemble(validPrecheckWithRiskBlockers());

        assertThat(assembly.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(assembly.getAllowedNextStep()).isEqualTo("WAIT_FOR_SCORE_CALCULATION_AUTHORIZATION");
        assembly.getRiskBlockers().add("mutated");
        assertThat(assembly.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void scoreAssemblyOutputIsReviewOnly() {
        assertThat(assembler.assemble(validPrecheck()).isReviewOnly()).isTrue();
    }

    @Test
    void scoreAssemblyOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validPrecheck()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void scoreAssemblyOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validPrecheck()).isManualReviewRequired()).isTrue();
    }

    @Test
    void scoreAssemblyOutputDoesNotContainRealScoreFieldsByReflection() {
        assertNoFragmentInFields(
                ReviewOnlyScoreAssemblyDTO.class,
                List.of(
                        "scoreitem",
                        "trend",
                        "liquidity",
                        "macro",
                        "confidence",
                        "leverage",
                        "sentiment",
                        "finalscore",
                        "direction",
                        "longshort",
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
        assertNoExactField(ReviewOnlyScoreAssemblyDTO.class, "score");
        assertNoExactField(ReviewOnlyScoreAssemblyDTO.class, "tp");
        assertNoExactField(ReviewOnlyScoreAssemblyDTO.class, "rr");
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
                "CandidateAttention",
                "PromoteToHome",
                "OpportunityPush",
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
                "CandidateAttention",
                "PromoteToHome",
                "OpportunityPush",
                "Readiness",
                "PointGeneration",
                "Trading",
                "Order",
                "Execution"
        ));
    }

    @Test
    void assemblerWorksWithoutSpringContext() {
        ReviewOnlyScoreAssemblyAssembler plainAssembler =
                new ReviewOnlyScoreAssemblyAssembler();

        assertThat(plainAssembler.assemble(validPrecheck()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyScoreAssemblyAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedBeforeAnyRead() {
        ReviewOnlyScoreAssemblyDTO assembly = assembler.assemble(null);

        assertThat(assembly.getScoreAssemblyStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(assembly.getAllowedNextStep()).isEqualTo("BLOCKED_BY_SCORE_PRECHECK");
        assertThat(assembly.getBlockingReasons()).contains("REVIEW_ONLY_SCORE_INPUT_PRECHECK_MISSING");
        assertThat(assembly.isBlocked()).isTrue();
        assertThat(assembly.isFailClosed()).isTrue();
        assertSafetyFlags(assembly);
    }

    private ReviewOnlyScoreInputPrecheckDTO validPrecheck() {
        return ReviewOnlyScoreInputPrecheckDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_EVIDENCE_NORMALIZATION",
                "REVIEW_ONLY_SCORE_INPUT_PRECHECK",
                List.of("REVIEW_ONLY_SCORE_INPUT_PRECHECK"),
                List.of(),
                "READY_FOR_REVIEW_ONLY_SCORE_ASSEMBLY",
                "Review-only normalized evidence is ready for score assembly review only."
        );
    }

    private ReviewOnlyScoreInputPrecheckDTO validPrecheckWithRiskBlockers() {
        return ReviewOnlyScoreInputPrecheckDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_EVIDENCE_NORMALIZATION",
                "REVIEW_ONLY_SCORE_INPUT_PRECHECK",
                List.of("REVIEW_ONLY_SCORE_INPUT_PRECHECK"),
                List.of("stampede_review", "risk_action_guard_required"),
                "READY_FOR_REVIEW_ONLY_SCORE_ASSEMBLY",
                "Review-only normalized evidence is ready for score assembly review only."
        );
    }

    private ReviewOnlyScoreInputPrecheckDTO blockedPrecheck() {
        return ReviewOnlyScoreInputPrecheckDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                List.of("REVIEW_ONLY_NORMALIZED_EVIDENCE_FAIL_CLOSED", "BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "FIX_INPUT_CONTRACT",
                "Review-only score input precheck remains blocked and fail-closed."
        );
    }

    private void assertSafetyFlags(ReviewOnlyScoreAssemblyDTO assembly) {
        assertThat(assembly.isReviewOnly()).isTrue();
        assertThat(assembly.isNotTradeInstruction()).isTrue();
        assertThat(assembly.isManualReviewRequired()).isTrue();
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
                ReviewOnlyScoreAssemblyAssembler.class,
                ReviewOnlyScoreAssemblyDTO.class
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
                                    + "ReviewOnlyScoreAssemblyAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/score/"
                                    + "ReviewOnlyScoreAssemblyDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only score assembly source files", ex);
        }
    }

    private Constructor<ReviewOnlyScoreAssemblyAssembler> defaultConstructor() {
        try {
            return ReviewOnlyScoreAssemblyAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyScoreAssemblyAssembler.class.getDeclaredFields()).stream()
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
