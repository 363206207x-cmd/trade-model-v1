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
import org.example.trademodel.dto.candidate.ReviewOnlyCandidateHandoffDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyCandidateAttentionAssemblerTest {

    private final ReviewOnlyCandidateAttentionAssembler assembler =
            new ReviewOnlyCandidateAttentionAssembler();

    @Test
    void validReviewOnlyCandidateHandoffAssemblesReviewOnlyCandidateAttention() {
        ReviewOnlyCandidateAttentionDTO attention = assembler.assemble(validHandoff());

        assertThat(attention.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(attention.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(attention.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(attention.getCandidateHandoffStatus()).isEqualTo("REVIEW_ONLY_CANDIDATE_HANDOFF");
        assertThat(attention.getCandidateAttentionStatus()).isEqualTo("REVIEW_ONLY_CANDIDATE_ATTENTION");
        assertThat(attention.getAllowedNextStep()).isEqualTo("READY_FOR_CANDIDATE_PREVIEW_REVIEW_ONLY");
        assertThat(attention.isBlocked()).isFalse();
        assertThat(attention.isFailClosed()).isFalse();
        assertSafetyFlags(attention);
    }

    @Test
    void blockedCandidateHandoffAssemblesBlockedCandidateAttention() {
        ReviewOnlyCandidateAttentionDTO attention = assembler.assemble(blockedHandoff());

        assertThat(attention.getCandidateHandoffStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(attention.getCandidateAttentionStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(attention.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(attention.isBlocked()).isTrue();
        assertThat(attention.isFailClosed()).isTrue();
        assertSafetyFlags(attention);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyCandidateAttentionDTO attention = assembler.assemble(blockedHandoff());

        assertThat(attention.getBlockingReasons())
                .contains("REVIEW_ONLY_CANDIDATE_HANDOFF_FAIL_CLOSED",
                        "REVIEW_ONLY_SCORE_ASSEMBLY_FAIL_CLOSED");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyCandidateAttentionDTO attention = assembler.assemble(validHandoff());

        assertThat(attention.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyCandidateAttentionDTO attention = assembler.assemble(validHandoff());

        assertThat(attention.getRequestedTimeframes()).containsExactly("15m", "1h");
        attention.getRequestedTimeframes().add("mutated");
        assertThat(attention.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyCandidateAttentionDTO attention = assembler.assemble(validHandoff());

        assertThat(attention.getBlockingReasons())
                .contains("REVIEW_ONLY_CANDIDATE_HANDOFF", "REVIEW_ONLY_CANDIDATE_ATTENTION");
        attention.getBlockingReasons().add("mutated");
        assertThat(attention.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyCandidateAttentionDTO attention = assembler.assemble(validHandoffWithRiskBlockers());

        assertThat(attention.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(attention.getAllowedNextStep()).isEqualTo("WAIT_FOR_CANDIDATE_AUTHORIZATION");
        attention.getRiskBlockers().add("mutated");
        assertThat(attention.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void candidateAttentionOutputIsReviewOnly() {
        assertThat(assembler.assemble(validHandoff()).isReviewOnly()).isTrue();
    }

    @Test
    void candidateAttentionOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validHandoff()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void candidateAttentionOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validHandoff()).isManualReviewRequired()).isTrue();
    }

    @Test
    void candidateAttentionOutputDoesNotContainRankPushReadinessPointOrTradeFieldsByReflection() {
        assertNoFragmentInFields(
                ReviewOnlyCandidateAttentionDTO.class,
                List.of(
                        "candidateRank",
                        "candidateScore",
                        "ranking",
                        "rank",
                        "scoreValue",
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
        assertNoExactField(ReviewOnlyCandidateAttentionDTO.class, "candidate");
        assertNoExactField(ReviewOnlyCandidateAttentionDTO.class, "push");
        assertNoExactField(ReviewOnlyCandidateAttentionDTO.class, "tp");
        assertNoExactField(ReviewOnlyCandidateAttentionDTO.class, "rr");
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
        ReviewOnlyCandidateAttentionAssembler plainAssembler =
                new ReviewOnlyCandidateAttentionAssembler();

        assertThat(plainAssembler.assemble(validHandoff()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyCandidateAttentionAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedBeforeAnyRead() {
        ReviewOnlyCandidateAttentionDTO attention = assembler.assemble(null);

        assertThat(attention.getCandidateAttentionStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(attention.getAllowedNextStep()).isEqualTo("BLOCKED_BY_CANDIDATE_HANDOFF");
        assertThat(attention.getBlockingReasons()).contains("REVIEW_ONLY_CANDIDATE_HANDOFF_MISSING");
        assertThat(attention.isBlocked()).isTrue();
        assertThat(attention.isFailClosed()).isTrue();
        assertSafetyFlags(attention);
    }

    private ReviewOnlyCandidateHandoffDTO validHandoff() {
        return ReviewOnlyCandidateHandoffDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCORE_ASSEMBLY",
                "REVIEW_ONLY_CANDIDATE_HANDOFF",
                List.of("REVIEW_ONLY_CANDIDATE_HANDOFF"),
                List.of(),
                "READY_FOR_REVIEW_ONLY_CANDIDATE_ATTENTION",
                "Review-only score assembly is ready for candidate attention review only."
        );
    }

    private ReviewOnlyCandidateHandoffDTO validHandoffWithRiskBlockers() {
        return ReviewOnlyCandidateHandoffDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_SCORE_ASSEMBLY",
                "REVIEW_ONLY_CANDIDATE_HANDOFF",
                List.of("REVIEW_ONLY_CANDIDATE_HANDOFF"),
                List.of("stampede_review", "risk_action_guard_required"),
                "READY_FOR_REVIEW_ONLY_CANDIDATE_ATTENTION",
                "Review-only score assembly is ready for candidate attention review only."
        );
    }

    private ReviewOnlyCandidateHandoffDTO blockedHandoff() {
        return ReviewOnlyCandidateHandoffDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                List.of("REVIEW_ONLY_SCORE_ASSEMBLY_FAIL_CLOSED", "BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "FIX_INPUT_CONTRACT",
                "Review-only candidate handoff remains blocked and fail-closed."
        );
    }

    private void assertSafetyFlags(ReviewOnlyCandidateAttentionDTO attention) {
        assertThat(attention.isReviewOnly()).isTrue();
        assertThat(attention.isNotTradeInstruction()).isTrue();
        assertThat(attention.isManualReviewRequired()).isTrue();
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
                ReviewOnlyCandidateAttentionAssembler.class,
                ReviewOnlyCandidateAttentionDTO.class
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
                                    + "ReviewOnlyCandidateAttentionAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/candidate/"
                                    + "ReviewOnlyCandidateAttentionDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only candidate attention source files", ex);
        }
    }

    private Constructor<ReviewOnlyCandidateAttentionAssembler> defaultConstructor() {
        try {
            return ReviewOnlyCandidateAttentionAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyCandidateAttentionAssembler.class.getDeclaredFields()).stream()
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
