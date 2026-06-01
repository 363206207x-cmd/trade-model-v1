package org.example.trademodel.service.readiness;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.trademodel.dto.push.ReviewOnlyInternalPushPreviewDTO;
import org.example.trademodel.dto.readiness.ReviewOnlyReadinessGateDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyReadinessGateAssemblerTest {

    private final ReviewOnlyReadinessGateAssembler assembler =
            new ReviewOnlyReadinessGateAssembler();

    @Test
    void validReviewOnlyInternalPushPreviewAssemblesReviewOnlyReadinessGate() {
        ReviewOnlyReadinessGateDTO gate = assembler.assemble(validInternalPushPreview());

        assertThat(gate.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(gate.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(gate.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(gate.getInternalPushPreviewStatus())
                .isEqualTo("REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK");
        assertThat(gate.getReadinessGateStatus()).isEqualTo("REVIEW_ONLY_READINESS_GATE");
        assertThat(gate.getAllowedNextStep()).isEqualTo("READY_FOR_POINT_BOUNDARY_REVIEW_ONLY");
        assertThat(gate.isBlocked()).isFalse();
        assertThat(gate.isFailClosed()).isFalse();
        assertThat(gate.isIncomplete()).isFalse();
        assertRequiredReviewFlags(gate);
    }

    @Test
    void blockedInternalPushPreviewAssemblesBlockedReadinessGate() {
        ReviewOnlyReadinessGateDTO gate = assembler.assemble(blockedInternalPushPreview());

        assertThat(gate.getInternalPushPreviewStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(gate.getReadinessGateStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(gate.getAllowedNextStep()).isEqualTo("FIX_INPUT_CONTRACT");
        assertThat(gate.isBlocked()).isTrue();
        assertThat(gate.isFailClosed()).isTrue();
        assertThat(gate.isIncomplete()).isFalse();
        assertRequiredReviewFlags(gate);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyReadinessGateDTO gate = assembler.assemble(blockedInternalPushPreview());

        assertThat(gate.getBlockingReasons())
                .contains(
                        "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_FAIL_CLOSED",
                        "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_FAIL_CLOSED"
                );
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyReadinessGateDTO gate = assembler.assemble(validInternalPushPreview());

        assertThat(gate.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyReadinessGateDTO gate = assembler.assemble(validInternalPushPreview());

        assertThat(gate.getRequestedTimeframes()).containsExactly("15m", "1h");
        gate.getRequestedTimeframes().add("mutated");
        assertThat(gate.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyReadinessGateDTO gate = assembler.assemble(validInternalPushPreview());

        assertThat(gate.getBlockingReasons())
                .contains(
                        "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK",
                        "REVIEW_ONLY_READINESS_GATE"
                );
        gate.getBlockingReasons().add("mutated");
        assertThat(gate.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyReadinessGateDTO gate = assembler.assemble(internalPushPreviewWithRiskBlockers());

        assertThat(gate.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(gate.getAllowedNextStep()).isEqualTo("BLOCKED_BY_RISK_ACTION_GUARD");
        assertThat(gate.isBlocked()).isTrue();
        assertThat(gate.isFailClosed()).isTrue();
        gate.getRiskBlockers().add("mutated");
        assertThat(gate.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void readinessGateOutputIsReviewOnly() {
        assertThat(assembler.assemble(validInternalPushPreview()).isReviewOnly()).isTrue();
    }

    @Test
    void readinessGateOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validInternalPushPreview()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void readinessGateOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validInternalPushPreview()).isManualReviewRequired()).isTrue();
    }

    @Test
    void recheckRequiredIsPreservedTrue() {
        assertThat(assembler.assemble(validInternalPushPreview()).isRecheckRequired()).isTrue();
    }

    @Test
    void riskActionGuardRequiredIsPreservedTrue() {
        assertThat(assembler.assemble(validInternalPushPreview()).isRiskActionGuardRequired()).isTrue();
    }

    @Test
    void incompleteSourceContractFailsClosedWithoutPointProgression() {
        ReviewOnlyReadinessGateDTO gate = assembler.assemble(internalPushPreviewWithoutSourceContract());

        assertThat(gate.getReadinessGateStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(gate.getAllowedNextStep()).isEqualTo("INCOMPLETE_SOURCE_TRACE");
        assertThat(gate.getBlockingReasons()).contains("INCOMPLETE_SOURCE_TRACE");
        assertThat(gate.isIncomplete()).isTrue();
        assertThat(gate.isBlocked()).isTrue();
        assertThat(gate.isFailClosed()).isTrue();
        assertRequiredReviewFlags(gate);
    }

    @Test
    void readinessGateOutputDoesNotContainPointEntryStopTpRrOrderOrExecutionFields() {
        assertNoFragmentInFields(
                ReviewOnlyReadinessGateDTO.class,
                List.of(
                        "pointGeneration",
                        "pointProposal",
                        "entryZone",
                        "entry",
                        "stopZone",
                        "stop",
                        "takeProfit",
                        "targetProfit",
                        "riskReward",
                        "orderIntent",
                        "order",
                        "executionIntent",
                        "execution",
                        "finalDirection",
                        "longShort"
                )
        );
        assertNoExactFields(ReviewOnlyReadinessGateDTO.class, List.of("tp", "rr"));
    }

    @Test
    void readinessGateOutputDoesNotContainExternalChannelTelegramEmailWebhookOrSendMessageFields() {
        assertNoFragmentInFields(
                ReviewOnlyReadinessGateDTO.class,
                List.of(
                        "externalChannel",
                        "telegram",
                        "email",
                        "webhook",
                        "appNotification",
                        "localNotification",
                        "notification",
                        "messageToSend",
                        "sendMessage",
                        "sendable",
                        "sender"
                )
        );
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
    void assemblerDoesNotCreateEvidenceScoreCandidatePushPointOrTradingBehavior() {
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
                "Telegram",
                "Email",
                "Webhook",
                "Notification",
                "MessageToSend",
                "SendMessage",
                "PointGeneration",
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
                "Telegram",
                "Email",
                "Webhook",
                "Notification",
                "MessageToSend",
                "SendMessage",
                "PointGeneration",
                "Trading",
                "OrderIntent",
                "ExecutionIntent",
                "Order",
                "Execution"
        ));
    }

    @Test
    void assemblerWorksWithoutSpringContext() {
        ReviewOnlyReadinessGateAssembler plainAssembler =
                new ReviewOnlyReadinessGateAssembler();

        assertThat(plainAssembler.assemble(validInternalPushPreview()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyReadinessGateAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void nullInputFailsClosedAndIncompleteBeforeAnyRead() {
        ReviewOnlyReadinessGateDTO gate = assembler.assemble(null);

        assertThat(gate.getReadinessGateStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(gate.getAllowedNextStep()).isEqualTo("BLOCKED_BY_INTERNAL_PUSH_PREVIEW");
        assertThat(gate.getBlockingReasons()).contains("REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_MISSING");
        assertThat(gate.isBlocked()).isTrue();
        assertThat(gate.isFailClosed()).isTrue();
        assertThat(gate.isIncomplete()).isTrue();
        assertRequiredReviewFlags(gate);
    }

    private ReviewOnlyInternalPushPreviewDTO validInternalPushPreview() {
        return ReviewOnlyInternalPushPreviewDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD",
                "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK",
                List.of("REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK"),
                List.of(),
                "READY_FOR_PUSH_PREVIEW_CLOSURE_REVIEW_ONLY",
                "Review-only candidate preview guard is ready for push preview closure review only."
        );
    }

    private ReviewOnlyInternalPushPreviewDTO internalPushPreviewWithRiskBlockers() {
        return ReviewOnlyInternalPushPreviewDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD",
                "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK",
                List.of("REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK"),
                List.of("stampede_review", "risk_action_guard_required"),
                "WAIT_FOR_RISK_ACTION_GUARD_RECHECK",
                "Review-only candidate preview guard is waiting for Risk Action Guard recheck."
        );
    }

    private ReviewOnlyInternalPushPreviewDTO internalPushPreviewWithoutSourceContract() {
        return ReviewOnlyInternalPushPreviewDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD",
                "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK",
                List.of("REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK"),
                List.of(),
                "READY_FOR_PUSH_PREVIEW_CLOSURE_REVIEW_ONLY",
                "Review-only candidate preview guard is missing source trace context."
        );
    }

    private ReviewOnlyInternalPushPreviewDTO blockedInternalPushPreview() {
        return ReviewOnlyInternalPushPreviewDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                List.of("REVIEW_ONLY_CANDIDATE_PREVIEW_GUARD_FAIL_CLOSED", "BLOCKED_MISSING_SOURCE_CONTRACT_ID"),
                List.of("stampede_review", "risk_action_guard_required"),
                "FIX_INPUT_CONTRACT",
                "Review-only internal push preview remains blocked and fail-closed."
        );
    }

    private void assertRequiredReviewFlags(ReviewOnlyReadinessGateDTO gate) {
        assertThat(gate.isReviewOnly()).isTrue();
        assertThat(gate.isNotTradeInstruction()).isTrue();
        assertThat(gate.isManualReviewRequired()).isTrue();
        assertThat(gate.isRecheckRequired()).isTrue();
        assertThat(gate.isRiskActionGuardRequired()).isTrue();
    }

    private void assertNoFragmentInFields(Class<?> type, List<String> forbiddenFragments) {
        for (Field field : type.getDeclaredFields()) {
            assertNoFragment(field.getName(), forbiddenFragments);
        }
    }

    private void assertNoExactFields(Class<?> type, List<String> fieldNames) {
        for (Field field : type.getDeclaredFields()) {
            for (String fieldName : fieldNames) {
                assertThat(field.getName()).isNotEqualToIgnoringCase(fieldName);
            }
        }
    }

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : List.of(
                ReviewOnlyReadinessGateAssembler.class,
                ReviewOnlyReadinessGateDTO.class
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
                            "src/main/java/org/example/trademodel/service/readiness/"
                                    + "ReviewOnlyReadinessGateAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/readiness/"
                                    + "ReviewOnlyReadinessGateDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only readiness gate source files", ex);
        }
    }

    private Constructor<ReviewOnlyReadinessGateAssembler> defaultConstructor() {
        try {
            return ReviewOnlyReadinessGateAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyReadinessGateAssembler.class.getDeclaredFields()).stream()
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
