package org.example.trademodel.assembler.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyNumericPointProposalDTO;
import org.example.trademodel.validator.point.NumericPointSafetyValidator;
import org.junit.jupiter.api.Test;

class ReviewOnlyNumericPointProposalAssemblerTest {

    private final ReviewOnlyNumericPointProposalAssembler assembler =
            new ReviewOnlyNumericPointProposalAssembler();

    @Test
    void nullInputCreatesIncompleteProposal() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(null);

        assertThat(assembled.getProposal().getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE);
        assertThat(assembled.getProposal().getMissingReasons()).containsExactly("INPUT_MISSING");
    }

    @Test
    void nullInputCreatesIncompleteValidation() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(null);

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("INPUT_MISSING");
    }

    @Test
    void incompleteInputWithMissingReasonCreatesIncompleteProposal() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(incompleteInput());

        assertThat(assembled.getProposal().getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE);
        assertThat(assembled.getProposal().getMissingReasons()).containsExactly("SOURCE_TRACE_MISSING");
    }

    @Test
    void incompleteInputWithMissingReasonCreatesIncompleteValidation() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(incompleteInput());

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("SOURCE_TRACE_MISSING");
    }

    @Test
    void candidateInputWithAllRefsAndPointFieldsCreatesCandidateProposal() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(candidateInput());

        assertThat(assembled.getProposal().getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE);
        assertThat(assembled.getProposal().getSourceTraceRefs())
                .containsExactly("source-entry", "source-stop", "source-tp");
        assertThat(assembled.getProposal().getRuntimeKlineContextRefs())
                .containsExactly("runtime-15m", "runtime-1h");
    }

    @Test
    void candidateInputWithAllRefsAndPointFieldsCreatesCandidateValidation() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(candidateInput());

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE);
        assertThat(assembled.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void candidateMissingSourceTraceRefsCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithSourceTraceRefs(List.of())),
                "SOURCE_TRACE_REF_MISSING");
    }

    @Test
    void candidateMissingRuntimeKlineContextRefsCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithRuntimeKlineContextRefs(List.of())),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
    }

    @Test
    void candidateMissingDataQualityContextRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithDataQualityContextRef(null)),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
    }

    @Test
    void candidateMissingMultiTimeframeContextRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithMultiTimeframeContextRef(" ")),
                "MULTI_TIMEFRAME_CONTEXT_REF_MISSING");
    }

    @Test
    void candidateMissingRiskActionGuardRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithRiskActionGuardRef(null)),
                "RISK_ACTION_GUARD_REF_MISSING");
    }

    @Test
    void candidateMissingWatchlistPoolProofCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithWatchlistPoolProof("")),
                "WATCHLIST_POOL_PROOF_MISSING");
    }

    @Test
    void candidateMissingEntryCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithEntry(null)), "ENTRY_REVIEW_POINT_MISSING");
    }

    @Test
    void candidateMissingStopCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithStop(null)), "STOP_REVIEW_POINT_MISSING");
    }

    @Test
    void candidateMissingTakeProfitCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithTakeProfitLevels(List.of())),
                "TAKE_PROFIT_REVIEW_LEVEL_MISSING");
    }

    @Test
    void candidateMissingRiskRewardCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(candidateWithRiskReward(null)),
                "RISK_REWARD_REVIEW_FIELD_MISSING");
    }

    @Test
    void degradedInputWithMissingReasonCreatesDegradedValidation() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(degradedInput());

        assertThat(assembled.getProposal().getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("TP_SOURCE_MISSING");
    }

    @Test
    void blockedInputWithBlockedReasonCreatesBlockedFailClosedValidation() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(blockedInput());

        assertThat(assembled.getProposal().getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("RISK_ACTION_GUARD_BLOCKED");
    }

    @Test
    void forbiddenExecutableSemanticInInputCreatesBlockedFailClosedValidation() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(candidateWithForbiddenSemantics(List.of("buy")));

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void assemblerCallsValidatorAndReturnsProposalAndValidationResult() {
        CountingValidator countingValidator = new CountingValidator();
        ReviewOnlyNumericPointProposalAssembler countingAssembler =
                new ReviewOnlyNumericPointProposalAssembler(countingValidator);

        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                countingAssembler.assemble(candidateInput());

        assertThat(countingValidator.invocationCount).isEqualTo(1);
        assertThat(assembled.getProposal()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerDoesNotCalculatePointValuesAndPreservesExplicitBigDecimalValues() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(candidateInput());

        assertThat(assembled.getProposal().getEntry().getEntryPrice()).isEqualByComparingTo("100.25");
        assertThat(assembled.getProposal().getEntry().getEntryZoneLow()).isEqualByComparingTo("99.50");
        assertThat(assembled.getProposal().getEntry().getEntryZoneHigh()).isEqualByComparingTo("101.00");
        assertThat(assembled.getProposal().getStop().getStopPrice()).isEqualByComparingTo("96.00");
        assertThat(assembled.getProposal().getTakeProfitLevels().get(0).getTakeProfitPrice())
                .isEqualByComparingTo("108.00");
        assertThat(assembled.getProposal().getRiskReward().getRiskRewardValue()).isEqualByComparingTo("2.00");
    }

    @Test
    void assemblerHandlesNullPointFieldsWithoutException() {
        ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled =
                assembler.assemble(candidateWith(null, null, List.of(), null));

        assertThat(assembled.getProposal().getEntry()).isNull();
        assertThat(assembled.getProposal().getStop()).isNull();
        assertThat(assembled.getProposal().getTakeProfitLevels()).isEmpty();
        assertThat(assembled.getProposal().getRiskReward()).isNull();
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
    }

    @Test
    void assemblerHasNoSpringAnnotations() {
        assertNoAnnotations(ReviewOnlyNumericPointProposalAssembler.class);
        assertNoAnnotations(ReviewOnlyNumericPointProposalAssembler.AssemblyInput.class);
        assertNoAnnotations(ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint.class);
    }

    @Test
    void assemblerDoesNotReferenceServiceControllerMapperRepositoryOrScheduler() throws Exception {
        assertSourceDoesNotContain(List.of(
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "Service",
                "Repository",
                "Scheduler"
        ));
    }

    @Test
    void assemblerDoesNotReferenceMarketQuoteHttpOrDataSource() throws Exception {
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
    void assemblerDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
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
    void assemblerSafeResultDoesNotContainForbiddenExecutableSemantics() {
        List<ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint> assembledResults = List.of(
                assembler.assemble(incompleteInput()),
                assembler.assemble(blockedInput()),
                assembler.assemble(degradedInput()),
                assembler.assemble(candidateInput())
        );

        for (ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled : assembledResults) {
            List<String> outputs = new ArrayList<>();
            outputs.add(assembled.getProposal().getProposalStatus().name());
            outputs.addAll(assembled.getProposal().getMissingReasons());
            outputs.addAll(assembled.getProposal().getBlockedReasons());
            outputs.add(assembled.getValidationResult().getStatus().name());
            outputs.addAll(assembled.getValidationResult().getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput incompleteInput() {
        return input(
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
                List.of("SOURCE_TRACE_MISSING"),
                List.of(),
                List.of(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE
        );
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput blockedInput() {
        return input(
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
                List.of(),
                List.of("RISK_ACTION_GUARD_BLOCKED"),
                List.of(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.BLOCKED_FAIL_CLOSED
        );
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput degradedInput() {
        return input(
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
                List.of("TP_SOURCE_MISSING"),
                List.of(),
                List.of(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED
        );
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateInput() {
        return input(
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
                List.of(),
                List.of(),
                List.of(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
        );
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithSourceTraceRefs(
            List<String> sourceTraceRefs
    ) {
        return input(
                sourceTraceRefs,
                List.of("runtime-15m", "runtime-1h"),
                "dq-1",
                "mtf-1",
                "rag-1",
                "watchlist:BTCUSDT:v1",
                entry(),
                stop(),
                List.of(takeProfit()),
                riskReward(),
                List.of(),
                List.of(),
                List.of(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
        );
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithRuntimeKlineContextRefs(
            List<String> runtimeRefs
    ) {
        return input(
                List.of("source-entry", "source-stop", "source-tp"),
                runtimeRefs,
                "dq-1",
                "mtf-1",
                "rag-1",
                "watchlist:BTCUSDT:v1",
                entry(),
                stop(),
                List.of(takeProfit()),
                riskReward(),
                List.of(),
                List.of(),
                List.of(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
        );
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithDataQualityContextRef(
            String dataQualityContextRef
    ) {
        return candidateWithRefs(dataQualityContextRef, "mtf-1", "rag-1", "watchlist:BTCUSDT:v1");
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithMultiTimeframeContextRef(
            String multiTimeframeContextRef
    ) {
        return candidateWithRefs("dq-1", multiTimeframeContextRef, "rag-1", "watchlist:BTCUSDT:v1");
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithRiskActionGuardRef(
            String riskActionGuardRef
    ) {
        return candidateWithRefs("dq-1", "mtf-1", riskActionGuardRef, "watchlist:BTCUSDT:v1");
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithWatchlistPoolProof(
            String watchlistPoolProof
    ) {
        return candidateWithRefs("dq-1", "mtf-1", "rag-1", watchlistPoolProof);
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithRefs(
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String watchlistPoolProof
    ) {
        return input(
                List.of("source-entry", "source-stop", "source-tp"),
                List.of("runtime-15m"),
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                watchlistPoolProof,
                entry(),
                stop(),
                List.of(takeProfit()),
                riskReward(),
                List.of(),
                List.of(),
                List.of(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
        );
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithEntry(
            ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry
    ) {
        return candidateWith(entry, stop(), List.of(takeProfit()), riskReward());
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithStop(
            ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop
    ) {
        return candidateWith(entry(), stop, List.of(takeProfit()), riskReward());
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithTakeProfitLevels(
            List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels
    ) {
        return candidateWith(entry(), stop(), takeProfitLevels, riskReward());
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithRiskReward(
            ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward
    ) {
        return candidateWith(entry(), stop(), List.of(takeProfit()), riskReward);
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWithForbiddenSemantics(
            List<String> forbiddenSemantics
    ) {
        return input(
                List.of("source-entry", "source-stop", "source-tp"),
                List.of("runtime-15m"),
                "dq-1",
                "mtf-1",
                "rag-1",
                "watchlist:BTCUSDT:v1",
                entry(),
                stop(),
                List.of(takeProfit()),
                riskReward(),
                List.of(),
                List.of(),
                forbiddenSemantics,
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
        );
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput candidateWith(
            ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry,
            ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop,
            List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels,
            ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward
    ) {
        return input(
                List.of("source-entry", "source-stop", "source-tp"),
                List.of("runtime-15m"),
                "dq-1",
                "mtf-1",
                "rag-1",
                "watchlist:BTCUSDT:v1",
                entry,
                stop,
                takeProfitLevels,
                riskReward,
                List.of(),
                List.of(),
                List.of(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
        );
    }

    private ReviewOnlyNumericPointProposalAssembler.AssemblyInput input(
            List<String> sourceTraceRefs,
            List<String> runtimeKlineContextRefs,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String watchlistPoolProof,
            ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry,
            ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop,
            List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels,
            ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward,
            List<String> missingReasons,
            List<String> blockedReasons,
            List<String> forbiddenSemantics,
            ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus
    ) {
        return ReviewOnlyNumericPointProposalAssembler.AssemblyInput.of(
                "BTCUSDT",
                "SPOT",
                List.of("15m", "1h"),
                sourceTraceRefs,
                runtimeKlineContextRefs,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                watchlistPoolProof,
                entry,
                stop,
                takeProfitLevels,
                riskReward,
                missingReasons,
                blockedReasons,
                forbiddenSemantics,
                requestedStatus
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

    private void assertIncompleteFor(
            ReviewOnlyNumericPointProposalAssembler.AssembledReviewOnlyNumericPoint assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();

        assertThat(annotations).isEmpty();
    }

    private void assertSourceDoesNotContain(List<String> fragments) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/assembler/point/ReviewOnlyNumericPointProposalAssembler.java"
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
                "send order",
                "push opportunity"
        );

        for (String output : outputs) {
            String lowerOutput = output == null ? "" : output.toLowerCase();
            for (String forbiddenWord : forbiddenWords) {
                assertThat(lowerOutput).doesNotContain(forbiddenWord);
            }
        }
    }

    private static class CountingValidator extends NumericPointSafetyValidator {
        private int invocationCount;

        @Override
        public ValidationResult validate(ReviewOnlyNumericPointProposalDTO proposal) {
            invocationCount++;
            return super.validate(proposal);
        }
    }
}
