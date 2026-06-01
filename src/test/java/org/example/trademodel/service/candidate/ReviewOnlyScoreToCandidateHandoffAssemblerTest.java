package org.example.trademodel.service.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidateHandoffDTO;
import org.example.trademodel.dto.score.ReviewOnlyScoreAssemblyDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyScoreToCandidateHandoffAssemblerTest {

    private final ReviewOnlyScoreToCandidateHandoffAssembler assembler =
            new ReviewOnlyScoreToCandidateHandoffAssembler();

    @Test
    void validReviewOnlyScoreAssemblyAssemblesReviewOnlyCandidateHandoff() {
        ReviewOnlyCandidateHandoffDTO handoff = assembler.assemble(validAssembly());

        assertThat(handoff.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(handoff.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(handoff.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(handoff.getScoreAssemblyStatus()).isEqualTo("REVIEW_ONLY_SCORE_ASSEMBLY");
        assertThat(handoff.getCandidateHandoffStatus()).isEqualTo("REVIEW_ONLY_CANDIDATE_HANDOFF");
        assertThat(handoff.getAllowedNextStep()).isEqualTo("READY_FOR_REVIEW_ONLY_CANDIDATE_ATTENTION");
        assertThat(handoff.isBlocked()).isFalse();
        assertThat(handoff.isFailClosed()).isFalse();
        assertSafetyFlags(handoff);
    }

    @Test
    void blockedScoreAssemblyAssemblesBlockedCandidateHandoff() {
        ReviewOnlyCandidateHandoffDTO handoff = assembler.assemble(blockedAssembly());

        assertThat(handoff.getScoreAssemblyStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(handoff.getCandidateHandoffStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(handoff.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(handoff.isBlocked()).isTrue();
        assertThat(handoff.isFailClosed()).isTrue();
        assertSafetyFlags(handoff);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyCandidateHandoffDTO handoff = assembler.assemble(blockedAssembly());

        assertThat(handoff.getBlockingReasons())
                .contains("REVIEW_ONLY_SCORE_ASSEMBLY_FAIL_CLOSED",
                        "REVIEW_ONLY_SCORE_INPUT_PRECHECK_FAIL_CLOSED");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyCandidateHandoffDTO handoff = assembler.assemble(validAssembly());

        assertThat(handoff.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyCandidateHandoffDTO handoff = assembler.assemble(validAssembly());

        assertThat(handoff.getRequestedTimeframes()).containsExactly("15m", "1h");
        handoff.getRequestedTimeframes().add("mutated");
        assertThat(handoff.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyCandidateHandoffDTO handoff = assembler.assemble(validAssembly());

        assertThat(handoff.getBlockingReasons())
                .contains("REVIEW_ONLY_SCORE_ASSEMBLY", "REVIEW_ONLY_CANDIDATE_HANDOFF");
        handoff.getBlockingReasons().add("mutated");
        assertThat(handoff.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyCandidateHandoffDTO handoff = assembler.assemble(validAssemblyWithRiskBlockers());

        assertThat(handoff.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(handoff.getAllowedNextStep()).isEqualTo("WAIT_FOR_CANDIDATE_AUTHORIZATION");
        handoff.getRiskBlockers().add("mutated");
        assertThat(handoff.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void candidateHandoffOutputIsReviewOnly() {
        assertThat(assembler.assemble(validAssembly()).isReviewOnly()).isTrue();
    }

    @Test
    void candidateHandoffOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validAssembly()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void candidateHandoffOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validAssembly()).isManualReviewRequired()).isTrue();
    }

    @Test
    void candidateHandoffOutputDoesNotContainRealCandidatePushReadinessPointOrTradeFieldsByReflection() {
        assertNoFragmentInFields(
                ReviewOnlyCandidateHandoffDTO.class,
                List.of(
                        "candidateRank",
                        "candidateScore",
                        "promoteToHome",
                        "pushMessage",
                        "readiness",
                        "pointGeneration",
                        "entryZone",
                        "stopZone",
                        "takeProfit",
                        "targetProfit",
                        "riskReward",
                        "finalDirection",
                        "longShort",
                        "orderIntent",
                        "executionIntent"
                )
        );
        assertNoExactField(ReviewOnlyCandidateHandoffDTO.class, "candidate");
        assertNoExactField(ReviewOnlyCandidateHandoffDTO.class, "push");
        assertNoExactField(ReviewOnlyCandidateHandoffDTO.class, "tp");
        assertNoExactField(ReviewOnlyCandidateHandoffDTO.class, "rr");
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
                "BoundaryCandidate",
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
                "BoundaryCandidate",
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
        ReviewOnlyScoreToCandidateHandoffAssembler plainAssembler =
                new ReviewOnlyScoreToCandidateHandoffAssembler();

        assertThat(plainAssembler.assemble(validAssembly()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyScoreToCandidateHandoffAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedBeforeAnyRead() {
        ReviewOnlyCandidateHandoffDTO handoff = assembler.assemble(null);

        assertThat(handoff.getCandidateHandoffStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(handoff.getAllowedNextStep()).isEqualTo("BLOCKED_BY_SCORE_ASSEMBLY");
        assertThat(handoff.getBlockingReasons()).contains("REVIEW_ONLY_SCORE_ASSEMBLY_MISSING");
        assertThat(handoff.isBlocked()).isTrue();
        assertThat(handoff.isFailClosed()).isTrue();
        assertSafetyFlags(handoff);
    }

    private ReviewOnlyScoreAssemblyDTO validAssembly() {
        return ReviewOnlyScoreAssemblyDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCORE_INPUT_PRECHECK",
                "REVIEW_ONLY_SCORE_ASSEMBLY",
                List.of("REVIEW_ONLY_SCORE_ASSEMBLY"),
                List.of(),
                "READY_FOR_SCORE_TO_CANDIDATE_HANDOFF_REVIEW_ONLY",
                "Review-only score input precheck is ready for score handoff review only."
        );
    }

    private ReviewOnlyScoreAssemblyDTO validAssemblyWithRiskBlockers() {
        return ReviewOnlyScoreAssemblyDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCORE_INPUT_PRECHECK",
                "REVIEW_ONLY_SCORE_ASSEMBLY",
                List.of("REVIEW_ONLY_SCORE_ASSEMBLY"),
                List.of("stampede_review", "risk_action_guard_required"),
                "READY_FOR_SCORE_TO_CANDIDATE_HANDOFF_REVIEW_ONLY",
                "Review-only score input precheck is ready for score handoff review only."
        );
    }

    private ReviewOnlyScoreAssemblyDTO blockedAssembly() {
        return ReviewOnlyScoreAssemblyDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                List.of("REVIEW_ONLY_SCORE_INPUT_PRECHECK_FAIL_CLOSED", "BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "FIX_INPUT_CONTRACT",
                "Review-only score assembly remains blocked and fail-closed."
        );
    }

    private void assertSafetyFlags(ReviewOnlyCandidateHandoffDTO handoff) {
        assertThat(handoff.isReviewOnly()).isTrue();
        assertThat(handoff.isNotTradeInstruction()).isTrue();
        assertThat(handoff.isManualReviewRequired()).isTrue();
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
                ReviewOnlyScoreToCandidateHandoffAssembler.class,
                ReviewOnlyCandidateHandoffDTO.class
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
                            "src/main/java/org/example/trademodel/service/candidate/"
                                    + "ReviewOnlyScoreToCandidateHandoffAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/candidate/"
                                    + "ReviewOnlyCandidateHandoffDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only candidate handoff source files", ex);
        }
    }

    private Constructor<ReviewOnlyScoreToCandidateHandoffAssembler> defaultConstructor() {
        try {
            return ReviewOnlyScoreToCandidateHandoffAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyScoreToCandidateHandoffAssembler.class.getDeclaredFields()).stream()
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
