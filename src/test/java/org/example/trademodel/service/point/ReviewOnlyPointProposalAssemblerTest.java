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
import org.example.trademodel.dto.point.ReviewOnlyPointProposalDTO;
import org.junit.jupiter.api.Test;

class ReviewOnlyPointProposalAssemblerTest {

    private final ReviewOnlyPointProposalAssembler assembler =
            new ReviewOnlyPointProposalAssembler();

    @Test
    void validReviewOnlyPointBoundaryGateAssemblesReviewOnlyPointProposal() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(validPointBoundaryGate());

        assertThat(proposal.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(proposal.getRequestId()).isEqualTo("market-read-request-001");
        assertThat(proposal.getSourceContractId()).isEqualTo("real-scan-input-contract-001");
        assertThat(proposal.getReadinessGateStatus()).isEqualTo("REVIEW_ONLY_READINESS_GATE");
        assertThat(proposal.getPointBoundaryGateStatus()).isEqualTo("REVIEW_ONLY_POINT_BOUNDARY_GATE");
        assertThat(proposal.getPointProposalStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(proposal.isPointProposalAllowed()).isTrue();
        assertThat(proposal.getPointProposalBlockedReason()).isEqualTo("INCOMPLETE_SOURCE_OWNED_POINT_INPUT");
        assertThat(proposal.getAllowedNextStep()).isEqualTo("WAIT_FOR_SOURCE_OWNED_POINT_INPUT");
        assertThat(proposal.isIncomplete()).isTrue();
        assertProposalValuesAbsent(proposal);
        assertRequiredReviewFlags(proposal);
    }

    @Test
    void blockedPointBoundaryGateAssemblesBlockedPointProposal() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(blockedPointBoundaryGate());

        assertThat(proposal.getPointBoundaryGateStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(proposal.getPointProposalStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertThat(proposal.getAllowedNextStep()).isEqualTo("BLOCKED_BY_RISK_ACTION_GUARD");
        assertThat(proposal.isPointProposalAllowed()).isFalse();
        assertThat(proposal.getPointProposalBlockedReason()).isEqualTo("BLOCKED_BY_READINESS_GATE");
        assertThat(proposal.isBlocked()).isTrue();
        assertThat(proposal.isFailClosed()).isTrue();
        assertThat(proposal.isIncomplete()).isFalse();
        assertProposalValuesAbsent(proposal);
        assertRequiredReviewFlags(proposal);
    }

    @Test
    void incompletePointBoundaryGateAssemblesIncompletePointProposal() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(incompletePointBoundaryGate());

        assertThat(proposal.getPointBoundaryGateStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(proposal.getPointProposalStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(proposal.getAllowedNextStep()).isEqualTo("INCOMPLETE_SOURCE_TRACE");
        assertThat(proposal.isPointProposalAllowed()).isFalse();
        assertThat(proposal.getPointProposalBlockedReason())
                .isEqualTo("REVIEW_ONLY_POINT_BOUNDARY_GATE_INCOMPLETE");
        assertThat(proposal.isBlocked()).isTrue();
        assertThat(proposal.isFailClosed()).isTrue();
        assertThat(proposal.isIncomplete()).isTrue();
        assertProposalValuesAbsent(proposal);
        assertRequiredReviewFlags(proposal);
    }

    @Test
    void failClosedReasonIsPreserved() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(blockedPointBoundaryGate());

        assertThat(proposal.getBlockingReasons())
                .contains(
                        "REVIEW_ONLY_POINT_BOUNDARY_GATE_FAIL_CLOSED",
                        "RISK_ACTION_GUARD_REQUIRED"
                );
    }

    @Test
    void watchlistPoolProofIsPreserved() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(validPointBoundaryGate());

        assertThat(proposal.getWatchlistPoolProof()).isEqualTo("watchlist:BTCUSDT:v1");
    }

    @Test
    void requestedTimeframesArePreserved() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(validPointBoundaryGate());

        assertThat(proposal.getRequestedTimeframes()).containsExactly("15m", "1h");
        proposal.getRequestedTimeframes().add("mutated");
        assertThat(proposal.getRequestedTimeframes()).containsExactly("15m", "1h");
    }

    @Test
    void blockingReasonsArePreserved() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(validPointBoundaryGate());

        assertThat(proposal.getBlockingReasons())
                .contains(
                        "REVIEW_ONLY_POINT_BOUNDARY_GATE",
                        "INCOMPLETE_SOURCE_OWNED_POINT_INPUT",
                        "SOURCE_TRACE_REQUIRED",
                        "RUNTIME_KLINE_CONTEXT_REQUIRED"
                );
        proposal.getBlockingReasons().add("mutated");
        assertThat(proposal.getBlockingReasons()).doesNotContain("mutated");
    }

    @Test
    void riskBlockersArePreserved() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(pointBoundaryGateWithRiskBlockers());

        assertThat(proposal.getRiskBlockers()).containsExactly("stampede_review", "liquidity_stress");
        assertThat(proposal.getAllowedNextStep()).isEqualTo("BLOCKED_BY_RISK_ACTION_GUARD");
        assertThat(proposal.isBlocked()).isTrue();
        assertThat(proposal.isFailClosed()).isTrue();
        assertThat(proposal.isPointProposalAllowed()).isFalse();
        proposal.getRiskBlockers().add("mutated");
        assertThat(proposal.getRiskBlockers()).containsExactly("stampede_review", "liquidity_stress");
    }

    @Test
    void pointProposalOutputIsReviewOnly() {
        assertThat(assembler.assemble(validPointBoundaryGate()).isReviewOnly()).isTrue();
    }

    @Test
    void pointProposalOutputIsNotTradeInstruction() {
        assertThat(assembler.assemble(validPointBoundaryGate()).isNotTradeInstruction()).isTrue();
    }

    @Test
    void pointProposalOutputIsManualReviewRequired() {
        assertThat(assembler.assemble(validPointBoundaryGate()).isManualReviewRequired()).isTrue();
    }

    @Test
    void recheckRequiredIsPreservedTrue() {
        assertThat(assembler.assemble(validPointBoundaryGate()).isRecheckRequired()).isTrue();
    }

    @Test
    void riskActionGuardRequiredIsPreservedTrue() {
        assertThat(assembler.assemble(validPointBoundaryGate()).isRiskActionGuardRequired()).isTrue();
    }

    @Test
    void sourceTraceRequiredIsTrue() {
        assertThat(assembler.assemble(validPointBoundaryGate()).isSourceTraceRequired()).isTrue();
    }

    @Test
    void runtimeKlineContextRequiredIsTrue() {
        assertThat(assembler.assemble(validPointBoundaryGate()).isRuntimeKlineContextRequired()).isTrue();
    }

    @Test
    void incompleteInputMustNotIncludeEntryStopTpOrRrValues() {
        assertProposalValuesAbsent(assembler.assemble(incompletePointBoundaryGate()));
    }

    @Test
    void pointProposalOutputDoesNotContainOrderOrExecutionFields() {
        assertNoFragmentInFields(
                ReviewOnlyPointProposalDTO.class,
                List.of(
                        "orderIntent",
                        "executionIntent",
                        "orderApi",
                        "executionApi",
                        "autoTrade",
                        "broker",
                        "exchangeWrite"
                )
        );
    }

    @Test
    void pointProposalOutputDoesNotContainExternalChannelTelegramEmailWebhookOrSendMessageFields() {
        assertNoFragmentInFields(
                ReviewOnlyPointProposalDTO.class,
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
                "RuntimeKlineContextDTO",
                "SourceTraceDTO",
                "Live",
                "DataSource",
                "Provider",
                "Jdbc",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp"
        ));
        assertMainSourcesDoNotContain(List.of(
                "RuntimeKlineContextDTO",
                "SourceTraceDTO",
                "Live",
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
    void assemblerDoesNotCreateEvidenceScoreCandidatePushExecutablePointOrTradingBehavior() {
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
                "ExecutablePoint",
                "TradePlan",
                "Trading",
                "OrderIntent",
                "ExecutionIntent"
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
                "ExecutablePoint",
                "TradePlan",
                "Trading",
                "OrderIntent",
                "ExecutionIntent"
        ));
    }

    @Test
    void assemblerWorksWithoutSpringContext() {
        ReviewOnlyPointProposalAssembler plainAssembler =
                new ReviewOnlyPointProposalAssembler();

        assertThat(plainAssembler.assemble(validPointBoundaryGate()).isReviewOnly()).isTrue();
        assertThat(ReviewOnlyPointProposalAssembler.class.getAnnotations()).isEmpty();
        assertThat(nonStaticFields()).isEmpty();
        assertThat(defaultConstructor()).isNotNull();
    }

    @Test
    void pointProposalAllowedStatusMustNotBeExecutable() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(validPointBoundaryGate());

        assertThat(proposal.isPointProposalAllowed()).isTrue();
        assertThat(proposal.isIncomplete()).isTrue();
        assertThat(proposal.isReviewOnly()).isTrue();
        assertThat(proposal.isManualReviewRequired()).isTrue();
        assertThat(proposal.isNotTradeInstruction()).isTrue();
        assertProposalValuesAbsent(proposal);
    }

    @Test
    void strongReversalDoesNotImplyReverseOrder() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(
                pointBoundaryGateWithRiskBlockers("strong_reversal_not_direct_reverse")
        );

        assertThat(proposal.isBlocked()).isTrue();
        assertThat(proposal.getRiskBlockers()).contains("strong_reversal_not_direct_reverse");
        assertThat(proposal.getAllowedNextStep()).isEqualTo("BLOCKED_BY_RISK_ACTION_GUARD");
        assertProposalValuesAbsent(proposal);
    }

    @Test
    void stampedeOrLiquidityStressReasonRemainsBlockingOrIncomplete() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(
                pointBoundaryGateWithRiskBlockers("stampede_review", "liquidity_stress")
        );

        assertThat(proposal.isBlocked()).isTrue();
        assertThat(proposal.getRiskBlockers()).contains("stampede_review", "liquidity_stress");
        assertThat(proposal.getPointProposalStatus()).isEqualTo("BLOCKED_FAIL_CLOSED");
        assertProposalValuesAbsent(proposal);
    }

    @Test
    void nullInputFailsClosedAndIncompleteBeforeAnyRead() {
        ReviewOnlyPointProposalDTO proposal = assembler.assemble(null);

        assertThat(proposal.getPointProposalStatus()).isEqualTo("INCOMPLETE_FAIL_CLOSED");
        assertThat(proposal.getAllowedNextStep()).isEqualTo("BLOCKED_BY_POINT_BOUNDARY_GATE");
        assertThat(proposal.getBlockingReasons()).contains("REVIEW_ONLY_POINT_BOUNDARY_GATE_MISSING");
        assertThat(proposal.isPointProposalAllowed()).isFalse();
        assertThat(proposal.isBlocked()).isTrue();
        assertThat(proposal.isFailClosed()).isTrue();
        assertThat(proposal.isIncomplete()).isTrue();
        assertProposalValuesAbsent(proposal);
        assertRequiredReviewFlags(proposal);
    }

    private ReviewOnlyPointBoundaryGateDTO validPointBoundaryGate() {
        return ReviewOnlyPointBoundaryGateDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_READINESS_GATE",
                "REVIEW_ONLY_POINT_BOUNDARY_GATE",
                List.of("REVIEW_ONLY_POINT_BOUNDARY_GATE"),
                List.of(),
                "READY_FOR_REVIEW_ONLY_POINT_PROPOSAL",
                "Review-only point boundary may enter source-owned proposal review.",
                true,
                null
        );
    }

    private ReviewOnlyPointBoundaryGateDTO blockedPointBoundaryGate() {
        return ReviewOnlyPointBoundaryGateDTO.blocked(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_READINESS_GATE",
                "BLOCKED_FAIL_CLOSED",
                List.of("RISK_ACTION_GUARD_REQUIRED"),
                List.of("stampede_review", "risk_action_guard_required"),
                "BLOCKED_BY_RISK_ACTION_GUARD",
                "Review-only point boundary remains blocked and fail-closed.",
                "BLOCKED_BY_READINESS_GATE"
        );
    }

    private ReviewOnlyPointBoundaryGateDTO incompletePointBoundaryGate() {
        return ReviewOnlyPointBoundaryGateDTO.incomplete(
                "BTCUSDT",
                "market-read-request-001",
                null,
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_READINESS_GATE",
                "INCOMPLETE_FAIL_CLOSED",
                List.of("INCOMPLETE_SOURCE_TRACE"),
                List.of(),
                "INCOMPLETE_SOURCE_TRACE",
                "Review-only point boundary is incomplete and cannot advance.",
                "REVIEW_ONLY_POINT_BOUNDARY_GATE_INCOMPLETE"
        );
    }

    private ReviewOnlyPointBoundaryGateDTO pointBoundaryGateWithRiskBlockers(String... riskBlockers) {
        return ReviewOnlyPointBoundaryGateDTO.reviewOnly(
                "BTCUSDT",
                "market-read-request-001",
                "real-scan-input-contract-001",
                "watchlist:BTCUSDT:v1",
                List.of("15m", "1h"),
                "REVIEW_ONLY_READINESS_GATE",
                "REVIEW_ONLY_POINT_BOUNDARY_GATE",
                List.of("REVIEW_ONLY_POINT_BOUNDARY_GATE"),
                List.of(riskBlockers),
                "WAIT_FOR_REVIEW",
                "Review-only point boundary is waiting for Risk Action Guard recheck.",
                true,
                null
        );
    }

    private ReviewOnlyPointBoundaryGateDTO pointBoundaryGateWithRiskBlockers() {
        return pointBoundaryGateWithRiskBlockers("stampede_review", "liquidity_stress");
    }

    private void assertRequiredReviewFlags(ReviewOnlyPointProposalDTO proposal) {
        assertThat(proposal.isReviewOnly()).isTrue();
        assertThat(proposal.isNotTradeInstruction()).isTrue();
        assertThat(proposal.isManualReviewRequired()).isTrue();
        assertThat(proposal.isRecheckRequired()).isTrue();
        assertThat(proposal.isRiskActionGuardRequired()).isTrue();
        assertThat(proposal.isSourceTraceRequired()).isTrue();
        assertThat(proposal.isRuntimeKlineContextRequired()).isTrue();
    }

    private void assertProposalValuesAbsent(ReviewOnlyPointProposalDTO proposal) {
        assertThat(proposal.getProposedEntry()).isNull();
        assertThat(proposal.getEntryZone()).isNull();
        assertThat(proposal.getProposedStop()).isNull();
        assertThat(proposal.getStopZone()).isNull();
        assertThat(proposal.getProposedTakeProfit()).isNull();
        assertThat(proposal.getTakeProfitPlan()).isNull();
        assertThat(proposal.getProposedRR()).isNull();
        assertThat(proposal.getRiskReward()).isNull();
    }

    private void assertNoFragmentInFields(Class<?> type, List<String> forbiddenFragments) {
        for (Field field : type.getDeclaredFields()) {
            assertNoFragment(field.getName(), forbiddenFragments);
        }
    }

    private void assertNoForbiddenSurface(List<String> forbiddenFragments) {
        for (Class<?> type : List.of(
                ReviewOnlyPointProposalAssembler.class,
                ReviewOnlyPointProposalDTO.class
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
                                    + "ReviewOnlyPointProposalAssembler.java"
                    )),
                    Files.readString(Path.of(
                            "src/main/java/org/example/trademodel/dto/point/"
                                    + "ReviewOnlyPointProposalDTO.java"
                    ))
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to read review-only point proposal source files", ex);
        }
    }

    private Constructor<ReviewOnlyPointProposalAssembler> defaultConstructor() {
        try {
            return ReviewOnlyPointProposalAssembler.class.getDeclaredConstructor();
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private List<Field> nonStaticFields() {
        return List.of(ReviewOnlyPointProposalAssembler.class.getDeclaredFields()).stream()
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
