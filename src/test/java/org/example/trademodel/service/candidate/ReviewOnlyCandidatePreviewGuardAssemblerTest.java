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
import org.example.trademodel.dto.candidate.ReviewOnlyCandidateAttentionDTO;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidatePreviewGuardDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyCandidatePreviewGuardAssemblerTest {

    private final ReviewOnlyCandidatePreviewGuardAssembler assembler =
            new ReviewOnlyCandidatePreviewGuardAssembler();

    @Test
    void validReviewOnlyCandidateAttentionAssemblesReviewOnlyCandidatePreviewGuard() {
        ReviewOnlyCandidatePreviewGuardDTO guard = assembler.assemble(validAttention());

        assertThat(guard.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(guard.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(guard.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(guard.getCandidateAttentionStatus()).isEqualTo("REVIEW_ONLY_CANDIDATE_ATTENTION");
        assertThat(guard.getCandidatePreviewGuardStatus()).isEqualTo("REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD");
        assertThat(guard.getAllowedNextStep()).isEqualTo("READY_FOR_INTERNAL_PUSH_PREVIEW_REVIEW_ONLY");
        assertThat(guard.isBlocked()).isFalse();
        assertThat(guard.isFailClosed()).isFalse();
        assertSafetyFlags(guard);
    }

    @Test
    void blockedCandidateAttentionAssemblesBlockedCandidatePreviewGuard() {
        ReviewOnlyCandidatePreviewGuardDTO guard = assembler.assemble(blockedAttention());

        assertThat(guard.getCandidateAttentionStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(guard.getCandidatePreviewGuardStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(guard.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(guard.isBlocked()).isTrue();
        assertThat(guard.isFailClosed()).isTrue();
        assertSafetyFlags(guard);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyCandidatePreviewGuardDTO guard = assembler.assemble(blockedAttention());

        assertThat(guard.getBlockingReasons())
                .contains("REVIEW_ONLY_CANDIDATE_ATTENTION_FAIL_CLOSED",
                        "REVIEW_ONLY_CANDIDATE_HANDOFF_FAIL_CLOSED");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyCandidatePreviewGuardDTO guard = assembler.assemble(validAttention());

        assertThat(guard.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyCandidatePreviewGuardDTO guard = assembler.assemble(validAttention());

        assertThat(guard.getRequestedTimeframes()).containsExactly("15m", "1h");
        guard.getRequestedTimeframes().add("mutated");
        assertThat(guard.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyCandidatePreviewGuardDTO guard = assembler.assemble(validAttention());

        assertThat(guard.getBlockingReasons())
                .contains("REVIEW_ONLY_CANDIDATE_ATTENTION", "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD");
        guard.getBlockingReasons().add("mutated");
        assertThat(guard.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyCandidatePreviewGuardDTO guard = assembler.assemble(validAttentionWithRiskBlockers());

        assertThat(guard.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(guard.getAllowedNextStep()).isEqualTo("WAIT_FOR_CANDIDATE_RANKING_AUTHORIZATION");
        guard.getRiskBlockers().add("mutated");
        assertThat(guard.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void candidatePreviewGuardOutputIsReviewOnly() {
        assertThat(assembler.assemble(validAttention()).isReviewOnly()).isTrue();
    }

    @Test
    void candidatePreviewGuardOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validAttention()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void candidatePreviewGuardOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validAttention()).isManualReviewRequired()).isTrue();
    }

    @Test
    void candidatePreviewGuardOutputDoesNotContainRankScorePushReadinessPointOrTradeFieldsByReflection() {
        assertNoFragmentInFields(
                ReviewOnlyCandidatePreviewGuardDTO.class,
                List.of(
                        "candidateRank",
                        "candidateScore",
                        "rankingResult",
                        "rankValue",
                        "scoreValue",
                        "promoteToHome",
                        "pushMessage",
                        "externalChannel",
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
        assertNoExactField(ReviewOnlyCandidatePreviewGuardDTO.class, "candidate");
        assertNoExactField(ReviewOnlyCandidatePreviewGuardDTO.class, "rank");
        assertNoExactField(ReviewOnlyCandidatePreviewGuardDTO.class, "score");
        assertNoExactField(ReviewOnlyCandidatePreviewGuardDTO.class, "push");
        assertNoExactField(ReviewOnlyCandidatePreviewGuardDTO.class, "tp");
        assertNoExactField(ReviewOnlyCandidatePreviewGuardDTO.class, "rr");
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
        ReviewOnlyCandidatePreviewGuardAssembler plainAssembler =
                new ReviewOnlyCandidatePreviewGuardAssembler();

        assertThat(plainAssembler.assemble(validAttention()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyCandidatePreviewGuardAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedBeforeAnyRead() {
        ReviewOnlyCandidatePreviewGuardDTO guard = assembler.assemble(null);

        assertThat(guard.getCandidatePreviewGuardStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(guard.getAllowedNextStep()).isEqualTo("BLOCKED_BY_CANDIDATE_ATTENTION");
        assertThat(guard.getBlockingReasons()).contains("REVIEW_ONLY_CANDIDATE_ATTENTION_MISSING");
        assertThat(guard.isBlocked()).isTrue();
        assertThat(guard.isFailClosed()).isTrue();
        assertSafetyFlags(guard);
    }

    private ReviewOnlyCandidateAttentionDTO validAttention() {
        return ReviewOnlyCandidateAttentionDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_CANDIDATE_HANDOFF",
                "REVIEW_ONLY_CANDIDATE_ATTENTION",
                List.of("REVIEW_ONLY_CANDIDATE_ATTENTION"),
                List.of(),
                "READY_FOR_CANDIDATE_PREVIEW_REVIEW_ONLY",
                "Review-only candidate handoff is ready for candidate preview review only."
        );
    }

    private ReviewOnlyCandidateAttentionDTO validAttentionWithRiskBlockers() {
        return ReviewOnlyCandidateAttentionDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_CANDIDATE_HANDOFF",
                "REVIEW_ONLY_CANDIDATE_ATTENTION",
                List.of("REVIEW_ONLY_CANDIDATE_ATTENTION"),
                List.of("stampede_review", "risk_action_guard_required"),
                "READY_FOR_CANDIDATE_PREVIEW_REVIEW_ONLY",
                "Review-only candidate handoff is ready for candidate preview review only."
        );
    }

    private ReviewOnlyCandidateAttentionDTO blockedAttention() {
        return ReviewOnlyCandidateAttentionDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                List.of("REVIEW_ONLY_CANDIDATE_HANDOFF_FAIL_CLOSED", "BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "FIX_INPUT_CONTRACT",
                "Review-only candidate attention remains blocked and fail-closed."
        );
    }

    private void assertSafetyFlags(ReviewOnlyCandidatePreviewGuardDTO guard) {
        assertThat(guard.isReviewOnly()).isTrue();
        assertThat(guard.isNotTradeInstruction()).isTrue();
        assertThat(guard.isManualReviewRequired()).isTrue();
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
                ReviewOnlyCandidatePreviewGuardAssembler.class,
                ReviewOnlyCandidatePreviewGuardDTO.class
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
                                    + "ReviewOnlyCandidatePreviewGuardAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/candidate/"
                                    + "ReviewOnlyCandidatePreviewGuardDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only candidate preview guard source files", ex);
        }
    }

    private Constructor<ReviewOnlyCandidatePreviewGuardAssembler> defaultConstructor() {
        try {
            return ReviewOnlyCandidatePreviewGuardAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyCandidatePreviewGuardAssembler.class.getDeclaredFields()).stream()
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
