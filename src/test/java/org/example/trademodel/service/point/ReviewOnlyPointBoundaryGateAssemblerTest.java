package org.example.trademodel.service.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyPointBoundaryGateDTO;
import org.example.trademodel.dto.readiness.ReviewOnlyReadinessGateDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyPointBoundaryGateAssemblerTest {

    private final ReviewOnlyPointBoundaryGateAssembler assembler =
            new ReviewOnlyPointBoundaryGateAssembler();

    @Test
    void validReviewOnlyReadinessGateAssemblesReviewOnlyPointBoundaryGate() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(validReadinessGate());

        assertThat(gate.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(gate.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(gate.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(gate.getReadinessGateStatus()).isEqualTo("REVIEW_ONLY_READINESS_GATE");
        assertThat(gate.getPointBoundaryGateStatus()).isEqualTo("REVIEW_ONLY_POINT_BOUNDARY_GATE");
        assertThat(gate.getAllowedNextStep()).isEqualTo("READY_FOR_REVIEW_ONLY_POINT_PROPOSAL");
        assertThat(gate.isPointProposalAllowed()).isTrue();
        assertThat(gate.getPointProposalBlockedReason()).isNull();
        assertThat(gate.isBlocked()).isFalse();
        assertThat(gate.isFailClosed()).isFalse();
        assertThat(gate.isIncomplete()).isFalse();
        assertRequiredReviewFlags(gate);
    }

    @Test
    void blockedReadinessGateAssemblesBlockedPointBoundaryGate() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(blockedReadinessGate());

        assertThat(gate.getReadinessGateStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(gate.getPointBoundaryGateStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(gate.getAllowedNextStep()).isEqualTo("BLOCKED_BY_RISK_ACTION_GUARD");
        assertThat(gate.isPointProposalAllowed()).isFalse();
        assertThat(gate.getPointProposalBlockedReason()).isEqualTo("BLOCKED_BY_READINESS_GATE");
        assertThat(gate.isBlocked()).isTrue();
        assertThat(gate.isFailClosed()).isTrue();
        assertThat(gate.isIncomplete()).isFalse();
        assertRequiredReviewFlags(gate);
    }

    @Test
    void incompleteReadinessGateAssemblesIncompletePointBoundaryGate() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(incompleteReadinessGate());

        assertThat(gate.getReadinessGateStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(gate.getPointBoundaryGateStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(gate.getAllowedNextStep()).isEqualTo("INCOMPLETE_SOURCE_TRACE");
        assertThat(gate.isPointProposalAllowed()).isFalse();
        assertThat(gate.getPointProposalBlockedReason()).isEqualTo("REVIEW_ONLY_READINESS_GATE_INCOMPLETE");
        assertThat(gate.isIncomplete()).isTrue();
        assertThat(gate.isBlocked()).isTrue();
        assertThat(gate.isFailClosed()).isTrue();
        assertRequiredReviewFlags(gate);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(blockedReadinessGate());

        assertThat(gate.getBlockingReasons())
                .contains(
                        "REVIEW_ONLY_READINESS_GATE_FAIL_CLOSED",
                        "RISK_ACTION_GUARD_REQUIRED"
                );
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(validReadinessGate());

        assertThat(gate.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(validReadinessGate());

        assertThat(gate.getRequestedTimeframes()).containsExactly("15m", "1h");
        gate.getRequestedTimeframes().add("mutated");
        assertThat(gate.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(validReadinessGate());

        assertThat(gate.getBlockingReasons())
                .contains(
                        "REVIEW_ONLY_READINESS_GATE",
                        "REVIEW_ONLY_POINT_BOUNDARY_GATE"
                );
        gate.getBlockingReasons().add("mutated");
        assertThat(gate.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(readinessGateWithRiskBlockers());

        assertThat(gate.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
        assertThat(gate.getAllowedNextStep()).isEqualTo("BLOCKED_BY_RISK_ACTION_GUARD");
        assertThat(gate.isBlocked()).isTrue();
        assertThat(gate.isFailClosed()).isTrue();
        assertThat(gate.isPointProposalAllowed()).isFalse();
        gate.getRiskBlockers().add("mutated");
        assertThat(gate.getRiskBlockers()).containsExactly("stampede_review", "risk_action_guard_required");
    }

    @Test
    void pointBoundaryGateOutputIsReviewOnly() {
        assertThat(assembler.assemble(validReadinessGate()).isReviewOnly()).isTrue();
    }

    @Test
    void pointBoundaryGateOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validReadinessGate()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void pointBoundaryGateOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validReadinessGate()).isManualReviewRequired()).isTrue();
    }

    @Test
    void recheckRequiredIsPreservedTrue() {
        assertThat(assembler.assemble(validReadinessGate()).isRecheckRequired()).isTrue();
    }

    @Test
    void riskActionGuardRequiredIsPreservedTrue() {
        assertThat(assembler.assemble(validReadinessGate()).isRiskActionGuardRequired()).isTrue();
    }

    @Test
    void incompleteSourceContractFailsClosedWithoutExecutablePointProgression() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(readinessGateWithoutSourceContract());

        assertThat(gate.getPointBoundaryGateStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(gate.getAllowedNextStep()).isEqualTo("INCOMPLETE_SOURCE_TRACE");
        assertThat(gate.getBlockingReasons()).contains("INCOMPLETE_SOURCE_TRACE");
        assertThat(gate.isPointProposalAllowed()).isFalse();
        assertThat(gate.getPointProposalBlockedReason()).isEqualTo("INCOMPLETE_SOURCE_TRACE");
        assertThat(gate.isIncomplete()).isTrue();
        assertThat(gate.isBlocked()).isTrue();
        assertThat(gate.isFailClosed()).isTrue();
        assertRequiredReviewFlags(gate);
    }

    @Test
    void pointBoundaryGateOutputDoesNotContainPriceEntryStopTpRrOrderOrExecutionFields() {
        assertNoExactFields(
                ReviewOnlyPointBoundaryGateDTO.class,
                List.of(
                        "point",
                        "pointPrice",
                        "pointValue",
                        "generatedPoint",
                        "price",
                        "entry",
                        "stop",
                        "takeProfit",
                        "tp",
                        "rr",
                        "orderIntent",
                        "executionIntent"
                )
        );
        assertNoFragmentInFields(
                ReviewOnlyPointBoundaryGateDTO.class,
                List.of(
                        "pointPrice",
                        "pointValue",
                        "generatedPoint",
                        "executablePoint",
                        "entryZone",
                        "entryPrice",
                        "stopZone",
                        "stopPrice",
                        "takeProfit",
                        "targetProfit",
                        "riskReward",
                        "orderIntent",
                        "executionIntent",
                        "finalDirection",
                        "longShort"
                )
        );
    }

    @Test
    void pointBoundaryGateOutputDoesNotContainExternalChannelTelegramEmailWebhookOrSendMessageFields() {
        assertNoFragmentInFields(
                ReviewOnlyPointBoundaryGateDTO.class,
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
                "EntryPrice",
                "StopPrice",
                "TakeProfit",
                "RiskReward",
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
                "EntryPrice",
                "StopPrice",
                "TakeProfit",
                "RiskReward",
                "Trading",
                "OrderIntent",
                "ExecutionIntent",
                "Order",
                "Execution"
        ));
    }

    @Test
    void assemblerWorksWithoutSpringContext() {
        ReviewOnlyPointBoundaryGateAssembler plainAssembler =
                new ReviewOnlyPointBoundaryGateAssembler();

        assertThat(plainAssembler.assemble(validReadinessGate()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyPointBoundaryGateAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void pointProposalAllowedStatusRemainsReviewOnlyAndManualReviewRequired() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(validReadinessGate());

        assertThat(gate.isPointProposalAllowed()).isTrue();
        assertThat(gate.isReviewOnly()).isTrue();
        assertThat(gate.isManualReviewRequired()).isTrue();
        assertThat(gate.isNotTradeInstruction()).isTrue();
        assertThat(gate.getAllowedNextStep()).isEqualTo("READY_FOR_REVIEW_ONLY_POINT_PROPOSAL");
    }

    @Test
    void nullInputFailsClosedAndIncompleteBeforeAnyRead() {
        ReviewOnlyPointBoundaryGateDTO gate = assembler.assemble(null);

        assertThat(gate.getPointBoundaryGateStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(gate.getAllowedNextStep()).isEqualTo("BLOCKED_BY_READINESS_GATE");
        assertThat(gate.getBlockingReasons()).contains("REVIEW_ONLY_READINESS_GATE_MISSING");
        assertThat(gate.isPointProposalAllowed()).isFalse();
        assertThat(gate.isBlocked()).isTrue();
        assertThat(gate.isFailClosed()).isTrue();
        assertThat(gate.isIncomplete()).isTrue();
        assertRequiredReviewFlags(gate);
    }

    private ReviewOnlyReadinessGateDTO validReadinessGate() {
        return ReviewOnlyReadinessGateDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK",
                "REVIEW_ONLY_READINESS_GATE",
                List.of("REVIEW_ONLY_READINESS_GATE"),
                List.of(),
                "READY_FOR_POINT_BOUNDARY_REVIEW_ONLY",
                "Review-only internal push preview can enter point boundary review only."
        );
    }

    private ReviewOnlyReadinessGateDTO blockedReadinessGate() {
        return ReviewOnlyReadinessGateDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "BLOCKED_FAIL_CLOSED",
                "BLOCKED_FAIL_CLOSED",
                List.of("RISK_ACTION_GUARD_REQUIRED"),
                List.of("stampede_review", "risk_action_guard_required"),
                "BLOCKED_BY_RISK_ACTION_GUARD",
                "Review-only readiness gate remains blocked and fail-closed."
        );
    }

    private ReviewOnlyReadinessGateDTO incompleteReadinessGate() {
        return ReviewOnlyReadinessGateDTO.incomplete(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK",
                "INCOMPLETE_FAIL_CLOSED",
                List.of("INCOMPLETE_SOURCE_TRACE"),
                List.of(),
                "INCOMPLETE_SOURCE_TRACE",
                "Review-only readiness gate is incomplete and cannot advance."
        );
    }

    private ReviewOnlyReadinessGateDTO readinessGateWithRiskBlockers() {
        return ReviewOnlyReadinessGateDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK",
                "REVIEW_ONLY_READINESS_GATE",
                List.of("REVIEW_ONLY_READINESS_GATE"),
                List.of("stampede_review", "risk_action_guard_required"),
                "WAIT_FOR_REVIEW",
                "Review-only readiness gate is waiting for Risk Action Guard recheck."
        );
    }

    private ReviewOnlyReadinessGateDTO readinessGateWithoutSourceContract() {
        return ReviewOnlyReadinessGateDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_INTERNAL_PUSH_PREVIEW_RECHECK",
                "REVIEW_ONLY_READINESS_GATE",
                List.of("REVIEW_ONLY_READINESS_GATE"),
                List.of(),
                "READY_FOR_POINT_BOUNDARY_REVIEW_ONLY",
                "Review-only readiness gate is missing source trace context."
        );
    }

    private void assertRequiredReviewFlags(ReviewOnlyPointBoundaryGateDTO gate) {
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
                ReviewOnlyPointBoundaryGateAssembler.class,
                ReviewOnlyPointBoundaryGateDTO.class
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
                            "src/main/java/org/example/trademodel/service/point/"
                                    + "ReviewOnlyPointBoundaryGateAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/point/"
                                    + "ReviewOnlyPointBoundaryGateDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only point boundary gate source files", ex);
        }
    }

    private Constructor<ReviewOnlyPointBoundaryGateAssembler> defaultConstructor() {
        try {
            return ReviewOnlyPointBoundaryGateAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyPointBoundaryGateAssembler.class.getDeclaredFields()).stream()
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
