package org.example.trademodel.dto.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReviewOnlyNumericPointProposalDTOTest {

    @Test
    void incompleteFactoryKeepsReviewOnlyTrue() {
        assertThat(incompleteProposal().isReviewOnly()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsNotTradeInstructionTrue() {
        assertThat(incompleteProposal().isNotTradeInstruction()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsManualReviewRequiredTrue() {
        assertThat(incompleteProposal().isManualReviewRequired()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsRecheckRequiredTrue() {
        assertThat(incompleteProposal().isRecheckRequired()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsRiskActionGuardRequiredTrue() {
        assertThat(incompleteProposal().isRiskActionGuardRequired()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsSourceTraceRequiredTrue() {
        assertThat(incompleteProposal().isSourceTraceRequired()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsRuntimeKlineContextRequiredTrue() {
        assertThat(incompleteProposal().isRuntimeKlineContextRequired()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsDataQualityRequiredTrue() {
        assertThat(incompleteProposal().isDataQualityRequired()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsMultiTimeframeRequiredTrue() {
        assertThat(incompleteProposal().isMultiTimeframeRequired()).isTrue();
        assertThat(incompleteProposal().isIncompleteSafe()).isTrue();
    }

    @Test
    void incompleteFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> ReviewOnlyNumericPointProposalDTO.incomplete(
                "BTCUSDT",
                "SPOT",
                List.of("15m"),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockedFailClosedFactorySetsFailClosedTrue() {
        assertThat(blockedProposal().isFailClosed()).isTrue();
        assertThat(blockedProposal().getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.BLOCKED_FAIL_CLOSED);
    }

    @Test
    void blockedFailClosedFactoryDoesNotBecomeTradeInstruction() {
        ReviewOnlyNumericPointProposalDTO proposal = blockedProposal();

        assertThat(proposal.isReviewOnly()).isTrue();
        assertThat(proposal.isNotTradeInstruction()).isTrue();
        assertThat(proposal.isManualReviewRequired()).isTrue();
    }

    @Test
    void blockedFailClosedFactoryRequiresBlockedReason() {
        assertThatThrownBy(() -> ReviewOnlyNumericPointProposalDTO.blockedFailClosed(
                "BTCUSDT",
                "SPOT",
                List.of("15m"),
                List.of(),
                List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void degradedFactoryStillRequiresManualReviewAndRecheck() {
        ReviewOnlyNumericPointProposalDTO proposal = degradedProposal();

        assertThat(proposal.getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED);
        assertThat(proposal.isManualReviewRequired()).isTrue();
        assertThat(proposal.isRecheckRequired()).isTrue();
        assertThat(proposal.isNotTradeInstruction()).isTrue();
    }

    @Test
    void degradedFactoryRequiresMissingOrDegradedReason() {
        assertThatThrownBy(() -> ReviewOnlyNumericPointProposalDTO.degraded(
                "BTCUSDT",
                "SPOT",
                List.of("15m"),
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                List.of()
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewOnlyCandidateCarriesOnlyExplicitValuesAndDoesNotCalculatePoints() {
        ReviewOnlyNumericPointProposalDTO proposal = candidateProposal();

        assertThat(proposal.getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE);
        assertThat(proposal.getEntry().getEntryPrice()).isEqualByComparingTo("100.25");
        assertThat(proposal.getEntry().getEntryZoneLow()).isEqualByComparingTo("99.50");
        assertThat(proposal.getEntry().getEntryZoneHigh()).isEqualByComparingTo("101.00");
        assertThat(proposal.getStop().getStopPrice()).isEqualByComparingTo("96.00");
        assertThat(proposal.getTakeProfitLevels().get(0).getTakeProfitPrice()).isEqualByComparingTo("108.00");
        assertThat(proposal.getRiskReward().getRiskRewardValue()).isEqualByComparingTo("2.00");
        assertThat(proposal.getMissingReasons()).isEmpty();
        assertThat(proposal.getBlockedReasons()).isEmpty();
    }

    @Test
    void entryStopTpAndRrFieldsCanBeNullAndRemainIncompleteSafe() {
        ReviewOnlyNumericPointProposalDTO proposal = ReviewOnlyNumericPointProposalDTO.reviewOnlyCandidate(
                "BTCUSDT",
                "SPOT",
                List.of("15m"),
                List.of("source-entry"),
                List.of("runtime-15m"),
                "dq-1",
                "mtf-1",
                "rag-1",
                "watchlist:BTCUSDT:v1",
                null,
                null,
                List.of(),
                null,
                List.of()
        );

        assertThat(proposal.getEntry()).isNull();
        assertThat(proposal.getStop()).isNull();
        assertThat(proposal.getTakeProfitLevels()).isEmpty();
        assertThat(proposal.getRiskReward()).isNull();
        assertThat(proposal.isIncompleteSafe()).isTrue();
        assertThat(proposal.isReviewOnly()).isTrue();
    }

    @Test
    void listFieldsAreDefensivelyCopied() {
        List<String> requestedTimeframes = new ArrayList<>(List.of("15m", "1h"));
        List<String> sourceTraceRefs = new ArrayList<>(List.of("source-entry"));
        List<String> runtimeRefs = new ArrayList<>(List.of("runtime-15m"));
        List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels =
                new ArrayList<>(List.of(takeProfit()));

        ReviewOnlyNumericPointProposalDTO proposal = ReviewOnlyNumericPointProposalDTO.reviewOnlyCandidate(
                "BTCUSDT",
                "SPOT",
                requestedTimeframes,
                sourceTraceRefs,
                runtimeRefs,
                "dq-1",
                "mtf-1",
                "rag-1",
                "watchlist:BTCUSDT:v1",
                entry(),
                stop(),
                takeProfitLevels,
                riskReward(),
                List.of()
        );

        requestedTimeframes.add("mutated");
        sourceTraceRefs.add("mutated");
        runtimeRefs.add("mutated");
        takeProfitLevels.add(takeProfit());

        assertThat(proposal.getRequestedTimeframes()).containsExactly("15m", "1h");
        assertThat(proposal.getSourceTraceRefs()).containsExactly("source-entry");
        assertThat(proposal.getRuntimeKlineContextRefs()).containsExactly("runtime-15m");
        assertThat(proposal.getTakeProfitLevels()).hasSize(1);
    }

    @Test
    void gettersDoNotExposeMutableCollections() {
        ReviewOnlyNumericPointProposalDTO proposal = candidateProposal();

        proposal.getRequestedTimeframes().add("mutated");
        proposal.getSourceTraceRefs().add("mutated");
        proposal.getRuntimeKlineContextRefs().add("mutated");
        proposal.getTakeProfitLevels().clear();
        proposal.getForbiddenSemantics().add("mutated");

        assertThat(proposal.getRequestedTimeframes()).containsExactly("15m", "1h");
        assertThat(proposal.getSourceTraceRefs()).containsExactly("source-entry", "source-stop", "source-tp");
        assertThat(proposal.getRuntimeKlineContextRefs()).containsExactly("runtime-15m", "runtime-1h");
        assertThat(proposal.getTakeProfitLevels()).hasSize(1);
        assertThat(proposal.getForbiddenSemantics()).isEmpty();
    }

    @Test
    void noSetterBuilderOrFactoryParameterCanDisableSafetyFlags() {
        for (Method method : ReviewOnlyNumericPointProposalDTO.class.getDeclaredMethods()) {
            assertThat(method.getName()).doesNotStartWith("set");
            assertThat(method.getName().toLowerCase()).doesNotContain("builder");
            if (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                assertThat(List.of(method.getParameterTypes())).doesNotContain(boolean.class);
            }
        }
        for (Constructor<?> constructor : ReviewOnlyNumericPointProposalDTO.class.getDeclaredConstructors()) {
            assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        }
    }

    @Test
    void dtoHasNoSpringAnnotations() {
        assertNoAnnotations(ReviewOnlyNumericPointProposalDTO.class);
        assertNoAnnotations(ReviewOnlyNumericPointProposalDTO.EntryReviewPoint.class);
        assertNoAnnotations(ReviewOnlyNumericPointProposalDTO.StopReviewPoint.class);
        assertNoAnnotations(ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel.class);
        assertNoAnnotations(ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField.class);
    }

    @Test
    void dtoHasNoControllerMapperRepositorySchedulerOrExternalProviderDependency() throws Exception {
        assertSourceDoesNotContain(List.of(
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "Service",
                "Provider"
        ));
    }

    @Test
    void dtoHasNoMarketQuoteOrHttpOrDataSourceDependency() throws Exception {
        assertSourceDoesNotContain(List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp",
                "DataSource",
                "Jdbc"
        ));
    }

    @Test
    void dtoDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
        assertSourceDoesNotContain(List.of(
                "Telegram",
                "Webhook",
                "MessageSender",
                "PushSend",
                "OrderIntent",
                "ExecutionIntent",
                "AutoTrading"
        ));
    }

    @Test
    void dtoStatusAndReasonOutputsDoNotContainForbiddenExecutableSemantics() {
        for (ReviewOnlyNumericPointProposalDTO proposal : List.of(
                incompleteProposal(),
                blockedProposal(),
                degradedProposal(),
                candidateProposal()
        )) {
            List<String> publicOutputs = new ArrayList<>();
            publicOutputs.add(proposal.getProposalStatus().name());
            publicOutputs.addAll(proposal.getMissingReasons());
            publicOutputs.addAll(proposal.getBlockedReasons());
            assertNoForbiddenExecutableSemantics(publicOutputs);
        }
    }

    private ReviewOnlyNumericPointProposalDTO incompleteProposal() {
        return ReviewOnlyNumericPointProposalDTO.incomplete(
                "BTCUSDT",
                "SPOT",
                List.of("15m", "1h"),
                List.of("SOURCE_TRACE_MISSING")
        );
    }

    private ReviewOnlyNumericPointProposalDTO blockedProposal() {
        return ReviewOnlyNumericPointProposalDTO.blockedFailClosed(
                "BTCUSDT",
                "SPOT",
                List.of("15m", "1h"),
                List.of("RISK_ACTION_GUARD_BLOCKED"),
                List.of()
        );
    }

    private ReviewOnlyNumericPointProposalDTO degradedProposal() {
        return ReviewOnlyNumericPointProposalDTO.degraded(
                "BTCUSDT",
                "SPOT",
                List.of("15m", "1h"),
                List.of("source-entry", "source-stop"),
                List.of("runtime-15m"),
                "dq-1",
                "mtf-1",
                "rag-1",
                "watchlist:BTCUSDT:v1",
                entry(),
                stop(),
                List.of(),
                null,
                List.of("TP_SOURCE_MISSING")
        );
    }

    private ReviewOnlyNumericPointProposalDTO candidateProposal() {
        return ReviewOnlyNumericPointProposalDTO.reviewOnlyCandidate(
                "BTCUSDT",
                "SPOT",
                List.of("15m", "1h"),
                List.of("source-entry", "source-stop", "source-tp"),
                List.of("runtime-15m", "runtime-1h"),
                "dq-1",
                "mtf-1",
                "rag-1",
                "watchlist:BTCUSDT:v1",
                entry(),
                stop(),
                List.of(takeProfit()),
                riskReward(),
                List.of()
        );
    }

    private ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry() {
        return ReviewOnlyNumericPointProposalDTO.EntryReviewPoint.of(
                new BigDecimal("100.25"),
                new BigDecimal("99.50"),
                new BigDecimal("101.00"),
                "15m",
                "source-entry",
                null
        );
    }

    private ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop() {
        return ReviewOnlyNumericPointProposalDTO.StopReviewPoint.of(
                new BigDecimal("96.00"),
                new BigDecimal("95.50"),
                new BigDecimal("96.50"),
                "15m",
                "source-stop",
                null
        );
    }

    private ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel takeProfit() {
        return ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel.of(
                1,
                new BigDecimal("108.00"),
                new BigDecimal("107.50"),
                new BigDecimal("108.50"),
                "1h",
                "source-tp",
                null
        );
    }

    private ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward() {
        return ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField.of(
                new BigDecimal("2.00"),
                "REVIEW_ONLY_RISK_REWARD_TRACE",
                "source-entry",
                "source-stop",
                "source-tp",
                "source-owned-calculation-trace",
                null
        );
    }

    private void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();

        assertThat(annotations).isEmpty();
    }

    private void assertSourceDoesNotContain(List<String> fragments) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/dto/point/ReviewOnlyNumericPointProposalDTO.java"
        ));

        for (String fragment : fragments) {
            assertThat(source).doesNotContain(fragment);
        }
    }

    private void assertNoForbiddenExecutableSemantics(List<String> outputs) {
        List<String> forbiddenWords = List.of(
                "buy",
                "sell",
                "long",
                "short",
                "open long",
                "open short",
                "close position",
                "reverse",
                "market close",
                "market cut",
                "order",
                "execute",
                "execution",
                "auto-trade",
                "auto trading",
                "take-profit order",
                "stop-loss order",
                "send order"
        );

        for (String output : outputs) {
            String lowerOutput = output == null ? "" : output.toLowerCase();
            for (String forbiddenWord : forbiddenWords) {
                assertThat(lowerOutput).doesNotContain(forbiddenWord);
            }
        }
    }
}
