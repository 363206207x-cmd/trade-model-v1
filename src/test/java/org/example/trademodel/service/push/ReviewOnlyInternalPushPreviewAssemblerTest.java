package org.example.trademodel.service.push;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.trademodel.dto.candidate.ReviewOnlyCandidatePreviewGuardDTO;
import org.example.trademodel.dto.push.ReviewOnlyInternalPushPreviewDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyInternalPushPreviewAssemblerTest {

    private final ReviewOnlyInternalPushPreviewAssembler assembler =
            new ReviewOnlyInternalPushPreviewAssembler();

    @Test
    void validReviewOnlyCandidatePreviewGuardAssemblesReviewOnlyInternalPushPreview() {
        ReviewOnlyInternalPushPreviewDTO preview = assembler.assemble(validCandidatePreviewGuard());

        assertThat(preview.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(preview.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(preview.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(preview.getCandidatePreviewGuardStatus())
                .isEqualTo("REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD");
        assertThat(preview.getInternalPushPreviewStatus())
                .isEqualTo("REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK");
        assertThat(preview.getAllowedNextStep()).isEqualTo("READY_FOR_PUSH_PREVIEW_CLOSURE_REVIEW_ONLY");
        assertThat(preview.isBlocked()).isFalse();
        assertThat(preview.isFailClosed()).isFalse();
        assertRequiredReviewFlags(preview);
    }

    @Test
    void blockedCandidatePreviewGuardAssemblesBlockedInternalPushPreview() {
        ReviewOnlyInternalPushPreviewDTO preview = assembler.assemble(blockedCandidatePreviewGuard());

        assertThat(preview.getCandidatePreviewGuardStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(preview.getInternalPushPreviewStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(preview.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(preview.isBlocked()).isTrue();
        assertThat(preview.isFailClosed()).isTrue();
        assertRequiredReviewFlags(preview);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyInternalPushPreviewDTO preview = assembler.assemble(blockedCandidatePreviewGuard());

        assertThat(preview.getBlockingReasons())
                .contains("REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_FAIL_CLOSED",
                        "REVIEW_ONLY_CANDIDATE_ATTENTION_FAIL_CLOSED");
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyInternalPushPreviewDTO preview = assembler.assemble(validCandidatePreviewGuard());

        assertThat(preview.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyInternalPushPreviewDTO preview = assembler.assemble(validCandidatePreviewGuard());

        assertThat(preview.getRequestedTimeframes()).containsExactly("15m", "1h");
        preview.getRequestedTimeframes().add("mutated");
        assertThat(preview.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyInternalPushPreviewDTO preview = assembler.assemble(validCandidatePreviewGuard());

        assertThat(preview.getBlockingReasons())
                .contains("REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD",
                        "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK");
        preview.getBlockingReasons().add("mutated");
        assertThat(preview.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyInternalPushPreviewDTO preview = assembler.assemble(candidatePreviewGuardWithRiskBlockers());

        assertThat(preview.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(preview.getAllowedNextStep()).isEqualTo("WAIT_FOR_RISK_ACTION_GUARD_RECHECK");
        preview.getRiskBlockers().add("mutated");
        assertThat(preview.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void internalPushPreviewOutputIsReviewOnly() {
        assertThat(assembler.assemble(validCandidatePreviewGuard()).isReviewOnly()).isTrue();
    }

    @Test
    void internalPushPreviewOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validCandidatePreviewGuard()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void internalPushPreviewOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validCandidatePreviewGuard()).isManualReviewRequired()).isTrue();
    }

    @Test
    void recheckRequiredIsTrue() {
        assertThat(assembler.assemble(validCandidatePreviewGuard()).isRecheckRequired()).isTrue();
    }

    @Test
    void riskActionGuardRequiredIsTrue() {
        assertThat(assembler.assemble(validCandidatePreviewGuard()).isRiskActionGuardRequired()).isTrue();
    }

    @Test
    void internalPushPreviewOutputDoesNotContainExternalChannelSendingReadinessPointOrTradeFields() {
        assertNoFragmentInFields(
                ReviewOnlyInternalPushPreviewDTO.class,
                List.of(
                        "externalChannel",
                        "telegram",
                        "email",
                        "webhook",
                        "appNotification",
                        "localNotification",
                        "sendable",
                        "sender",
                        "sendResult",
                        "pushMessage",
                        "renderedMessage",
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
        assertNoExactField(ReviewOnlyInternalPushPreviewDTO.class, "push");
        assertNoExactField(ReviewOnlyInternalPushPreviewDTO.class, "message");
        assertNoExactField(ReviewOnlyInternalPushPreviewDTO.class, "tp");
        assertNoExactField(ReviewOnlyInternalPushPreviewDTO.class, "rr");
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
                "ExternalChannel",
                "Notification",
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
                "ExternalChannel",
                "Notification",
                "PointGeneration",
                "Trading",
                "Order",
                "Execution"
        ));
    }

    @Test
    void assemblerWorksWithoutSpringContext() {
        ReviewOnlyInternalPushPreviewAssembler plainAssembler =
                new ReviewOnlyInternalPushPreviewAssembler();

        assertThat(plainAssembler.assemble(validCandidatePreviewGuard()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyInternalPushPreviewAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedBeforeAnyRead() {
        ReviewOnlyInternalPushPreviewDTO preview = assembler.assemble(null);

        assertThat(preview.getInternalPushPreviewStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(preview.getAllowedNextStep()).isEqualTo("BLOCKED_BY_CANDIDATE_PREVIEW_GUARD");
        assertThat(preview.getBlockingReasons()).contains("REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_MISSING");
        assertThat(preview.isBlocked()).isTrue();
        assertThat(preview.isFailClosed()).isTrue();
        assertRequiredReviewFlags(preview);
    }

    private ReviewOnlyCandidatePreviewGuardDTO validCandidatePreviewGuard() {
        return ReviewOnlyCandidatePreviewGuardDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_CANDIDATE_ATTENTION",
                "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD",
                List.of("REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD"),
                List.of(),
                "READY_FOR_INTERNAL_PUSH_PREVIEW_REVIEW_ONLY",
                "Review-only candidate attention is ready for internal push preview review only."
        );
    }

    private ReviewOnlyCandidatePreviewGuardDTO candidatePreviewGuardWithRiskBlockers() {
        return ReviewOnlyCandidatePreviewGuardDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_CANDIDATE_ATTENTION",
                "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD",
                List.of("REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD"),
                List.of("stampede_review", "risk_action_guard_required"),
                "READY_FOR_INTERNAL_PUSH_PREVIEW_REVIEW_ONLY",
                "Review-only candidate attention is ready for internal push preview review only."
        );
    }

    private ReviewOnlyCandidatePreviewGuardDTO blockedCandidatePreviewGuard() {
        return ReviewOnlyCandidatePreviewGuardDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                List.of("REVIEW_ONLY_CANDIDATE_ATTENTION_FAIL_CLOSED", "BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "FIX_INPUT_CONTRACT",
                "Review-only candidate preview guard remains blocked and fail-closed."
        );
    }

    private void assertRequiredReviewFlags(ReviewOnlyInternalPushPreviewDTO preview) {
        assertThat(preview.isReviewOnly()).isTrue();
        assertThat(preview.isNotTradeInstruction()).isTrue();
        assertThat(preview.isManualReviewRequired()).isTrue();
        assertThat(preview.isRecheckRequired()).isTrue();
        assertThat(preview.isRiskActionGuardRequired()).isTrue();
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
                ReviewOnlyInternalPushPreviewAssembler.class,
                ReviewOnlyInternalPushPreviewDTO.class
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
                            "src/main/java/org/example/trademodel/service/push/"
                                    + "ReviewOnlyInternalPushPreviewAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/push/"
                                    + "ReviewOnlyInternalPushPreviewDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only internal push preview source files", ex);
        }
    }

    private Constructor<ReviewOnlyInternalPushPreviewAssembler> defaultConstructor() {
        try {
            return ReviewOnlyInternalPushPreviewAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyInternalPushPreviewAssembler.class.getDeclaredFields()).stream()
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
