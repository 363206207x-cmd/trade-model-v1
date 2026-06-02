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

class SourceOwnedNumericPointCandidateAssemblerTest {

    private final SourceOwnedNumericPointCandidateAssembler assembler =
            new SourceOwnedNumericPointCandidateAssembler();

    @Test
    void nullContextCreatesIncompleteProposal() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(null);

        assertThat(assembled.getProposal().getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE);
        assertThat(assembled.getProposal().getMissingReasons())
                .containsExactly("SOURCE_OWNED_CONTEXT_MISSING");
    }

    @Test
    void nullContextCreatesIncompleteValidation() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(null);

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons())
                .containsExactly("SOURCE_OWNED_CONTEXT_MISSING");
    }

    @Test
    void completeSourceOwnedContextCreatesCandidateProposal() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(completeContext());

        assertThat(assembled.getProposal().getProposalStatus())
                .isEqualTo(ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE);
        assertThat(assembled.getProposal().getSourceTraceRefs())
                .containsExactly("source-entry", "source-stop", "source-tp");
    }

    @Test
    void completeSourceOwnedContextCreatesCandidateValidation() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(completeContext());

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE);
        assertThat(assembled.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void completeSourceOwnedContextPreservesExplicitEntryValues() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(completeContext());

        assertThat(assembled.getProposal().getEntry().getEntryPrice()).isEqualByComparingTo("100.25");
        assertThat(assembled.getProposal().getEntry().getEntryZoneLow()).isEqualByComparingTo("99.50");
        assertThat(assembled.getProposal().getEntry().getEntryZoneHigh()).isEqualByComparingTo("101.00");
    }

    @Test
    void completeSourceOwnedContextPreservesExplicitStopValues() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(completeContext());

        assertThat(assembled.getProposal().getStop().getStopPrice()).isEqualByComparingTo("96.00");
        assertThat(assembled.getProposal().getStop().getStopZoneLow()).isEqualByComparingTo("95.50");
        assertThat(assembled.getProposal().getStop().getStopZoneHigh()).isEqualByComparingTo("96.50");
    }

    @Test
    void completeSourceOwnedContextPreservesExplicitTakeProfitValues() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(completeContext());

        assertThat(assembled.getProposal().getTakeProfitLevels()).hasSize(1);
        assertThat(assembled.getProposal().getTakeProfitLevels().get(0).getTakeProfitPrice())
                .isEqualByComparingTo("108.00");
        assertThat(assembled.getProposal().getTakeProfitLevels().get(0).getTakeProfitZoneLow())
                .isEqualByComparingTo("107.50");
        assertThat(assembled.getProposal().getTakeProfitLevels().get(0).getTakeProfitZoneHigh())
                .isEqualByComparingTo("108.50");
    }

    @Test
    void completeSourceOwnedContextPreservesExplicitRiskRewardValue() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(completeContext());

        assertThat(assembled.getProposal().getRiskReward().getRiskRewardValue())
                .isEqualByComparingTo("2.00");
    }

    @Test
    void entryMissingSourceRefCreatesIncompleteValidation() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(contextWithEntry(entryWithSourceRef(null)));

        assertIncompleteFor(assembled, "ENTRY_SOURCE_REF_MISSING");
    }

    @Test
    void stopMissingSourceRefCreatesIncompleteValidation() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(contextWithStop(stopWithSourceRef(" ")));

        assertIncompleteFor(assembled, "STOP_SOURCE_REF_MISSING");
    }

    @Test
    void missingTakeProfitWithMissingReasonCreatesDegradedValidation() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(contextWithTakeProfitContexts(List.of(), List.of("TP_SOURCE_MISSING")));

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("TP_SOURCE_MISSING");
    }

    @Test
    void missingTakeProfitWithoutMissingReasonCreatesIncompleteValidation() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(contextWithTakeProfitContexts(List.of(), List.of()));

        assertIncompleteFor(assembled, "TAKE_PROFIT_REVIEW_LEVEL_MISSING");
    }

    @Test
    void missingRiskRewardWithMissingReasonCreatesDegradedValidation() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(contextWithRiskReward(null, List.of("RR_SOURCE_MISSING")));

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("RR_SOURCE_MISSING");
    }

    @Test
    void missingRiskRewardWithoutMissingReasonCreatesIncompleteValidation() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(contextWithRiskReward(null, List.of()));

        assertIncompleteFor(assembled, "RISK_REWARD_REVIEW_FIELD_MISSING");
    }

    @Test
    void missingSourceTraceRefsCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(contextWithSourceTraceRefs(List.of())),
                "SOURCE_TRACE_REF_MISSING");
    }

    @Test
    void missingRuntimeKlineContextRefsCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(contextWithRuntimeKlineContextRefs(List.of())),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
    }

    @Test
    void missingDataQualityContextRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(contextWithRefs(null, "mtf-1", "rag-1", "watchlist:BTCUSDT:v1")),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
    }

    @Test
    void missingMultiTimeframeContextRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(contextWithRefs("dq-1", "", "rag-1", "watchlist:BTCUSDT:v1")),
                "MULTI_TIMEFRAME_CONTEXT_REF_MISSING");
    }

    @Test
    void missingRiskActionGuardRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(contextWithRefs("dq-1", "mtf-1", null, "watchlist:BTCUSDT:v1")),
                "RISK_ACTION_GUARD_REF_MISSING");
    }

    @Test
    void missingWatchlistPoolProofCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(contextWithRefs("dq-1", "mtf-1", "rag-1", " ")),
                "WATCHLIST_POOL_PROOF_MISSING");
    }

    @Test
    void untrustedEntrySourceCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(contextWithEntry(untrustedEntry())), "SOURCE_UNTRUSTED");
    }

    @Test
    void untrustedStopSourceCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(contextWithStop(untrustedStop())), "SOURCE_UNTRUSTED");
    }

    @Test
    void untrustedTakeProfitSourceCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(contextWithTakeProfitContexts(List.of(untrustedTakeProfit()), List.of())),
                "SOURCE_UNTRUSTED");
    }

    @Test
    void untrustedRiskRewardSourceCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(contextWithRiskReward(untrustedRiskReward(), List.of())),
                "SOURCE_UNTRUSTED");
    }

    @Test
    void forbiddenExecutableSemanticInInputCreatesBlockedFailClosedValidation() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(contextWithForbiddenSemantics(List.of("buy")));

        assertBlockedFor(assembled, "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void assemblerCallsReviewOnlyNumericPointProposalAssembler() {
        CountingProposalAssembler countingProposalAssembler = new CountingProposalAssembler();
        SourceOwnedNumericPointCandidateAssembler countingAssembler =
                new SourceOwnedNumericPointCandidateAssembler(countingProposalAssembler);

        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                countingAssembler.assemble(completeContext());

        assertThat(countingProposalAssembler.invocationCount).isEqualTo(1);
        assertThat(assembled.getAssembled()).isNotNull();
        assertThat(assembled.getProposal()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerDoesNotCalculatePointValuesAndPreservesExplicitBigDecimalValues() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(completeContext());

        assertThat(assembled.getProposal().getEntry().getEntryPrice()).isEqualByComparingTo("100.25");
        assertThat(assembled.getProposal().getStop().getStopPrice()).isEqualByComparingTo("96.00");
        assertThat(assembled.getProposal().getTakeProfitLevels().get(0).getTakeProfitPrice())
                .isEqualByComparingTo("108.00");
        assertThat(assembled.getProposal().getRiskReward().getRiskRewardValue()).isEqualByComparingTo("2.00");
    }

    @Test
    void assemblerHandlesNullSubContextsWithoutException() {
        SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled =
                assembler.assemble(contextWith(null, null, List.of(), null, List.of()));

        assertThat(assembled.getProposal().getEntry()).isNull();
        assertThat(assembled.getProposal().getStop()).isNull();
        assertThat(assembled.getProposal().getTakeProfitLevels()).isEmpty();
        assertThat(assembled.getProposal().getRiskReward()).isNull();
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
    }

    @Test
    void assemblerHasNoSpringAnnotations() {
        assertNoAnnotations(SourceOwnedNumericPointCandidateAssembler.class);
        assertNoAnnotations(SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext.class);
        assertNoAnnotations(SourceOwnedNumericPointCandidateAssembler.SourceOwnedEntryContext.class);
        assertNoAnnotations(SourceOwnedNumericPointCandidateAssembler.SourceOwnedStopContext.class);
        assertNoAnnotations(SourceOwnedNumericPointCandidateAssembler.SourceOwnedTakeProfitContext.class);
        assertNoAnnotations(SourceOwnedNumericPointCandidateAssembler.SourceOwnedRiskRewardContext.class);
        assertNoAnnotations(SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint.class);
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
        List<SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint> assembledResults = List.of(
                assembler.assemble(null),
                assembler.assemble(completeContext()),
                assembler.assemble(contextWithTakeProfitContexts(List.of(), List.of("TP_SOURCE_MISSING"))),
                assembler.assemble(contextWithEntry(untrustedEntry()))
        );

        for (SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled : assembledResults) {
            List<String> outputs = new ArrayList<>();
            outputs.add(assembled.getProposal().getProposalStatus().name());
            outputs.addAll(assembled.getProposal().getMissingReasons());
            outputs.addAll(assembled.getProposal().getBlockedReasons());
            outputs.add(assembled.getValidationResult().getStatus().name());
            outputs.addAll(assembled.getValidationResult().getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext completeContext() {
        return contextWith(entry(), stop(), List.of(takeProfit()), riskReward(), List.of());
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext contextWithEntry(
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedEntryContext entryContext
    ) {
        return contextWith(entryContext, stop(), List.of(takeProfit()), riskReward(), List.of());
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext contextWithStop(
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedStopContext stopContext
    ) {
        return contextWith(entry(), stopContext, List.of(takeProfit()), riskReward(), List.of());
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext contextWithTakeProfitContexts(
            List<SourceOwnedNumericPointCandidateAssembler.SourceOwnedTakeProfitContext> takeProfitContexts,
            List<String> missingReasons
    ) {
        return contextWith(entry(), stop(), takeProfitContexts, riskReward(), missingReasons);
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext contextWithRiskReward(
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedRiskRewardContext riskRewardContext,
            List<String> missingReasons
    ) {
        return contextWith(entry(), stop(), List.of(takeProfit()), riskRewardContext, missingReasons);
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext contextWith(
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedEntryContext entryContext,
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedStopContext stopContext,
            List<SourceOwnedNumericPointCandidateAssembler.SourceOwnedTakeProfitContext> takeProfitContexts,
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedRiskRewardContext riskRewardContext,
            List<String> missingReasons
    ) {
        return input(
                List.of("source-entry", "source-stop", "source-tp"),
                List.of("runtime-15m", "runtime-1h"),
                "dq-1",
                "mtf-1",
                "rag-1",
                "watchlist:BTCUSDT:v1",
                entryContext,
                stopContext,
                takeProfitContexts,
                riskRewardContext,
                missingReasons,
                List.of(),
                List.of(),
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext contextWithSourceTraceRefs(
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

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext contextWithRuntimeKlineContextRefs(
            List<String> runtimeKlineContextRefs
    ) {
        return input(
                List.of("source-entry", "source-stop", "source-tp"),
                runtimeKlineContextRefs,
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

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext contextWithRefs(
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String watchlistPoolProof
    ) {
        return input(
                List.of("source-entry", "source-stop", "source-tp"),
                List.of("runtime-15m", "runtime-1h"),
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

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext contextWithForbiddenSemantics(
            List<String> forbiddenSemantics
    ) {
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
                forbiddenSemantics,
                ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext input(
            List<String> sourceTraceRefs,
            List<String> runtimeKlineContextRefs,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String watchlistPoolProof,
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedEntryContext entryContext,
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedStopContext stopContext,
            List<SourceOwnedNumericPointCandidateAssembler.SourceOwnedTakeProfitContext> takeProfitContexts,
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedRiskRewardContext riskRewardContext,
            List<String> missingReasons,
            List<String> blockedReasons,
            List<String> forbiddenSemantics,
            ReviewOnlyNumericPointProposalDTO.ProposalStatus requestedStatus
    ) {
        return SourceOwnedNumericPointCandidateAssembler.SourceOwnedNumericPointContext.of(
                "BTCUSDT",
                "SPOT",
                List.of("15m", "1h"),
                sourceTraceRefs,
                runtimeKlineContextRefs,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                watchlistPoolProof,
                entryContext,
                stopContext,
                takeProfitContexts,
                riskRewardContext,
                missingReasons,
                blockedReasons,
                forbiddenSemantics,
                requestedStatus
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedEntryContext entry() {
        return entryWithSourceRef("source-entry");
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedEntryContext entryWithSourceRef(
            String entrySourceRef
    ) {
        return SourceOwnedNumericPointCandidateAssembler.SourceOwnedEntryContext.of(
                new BigDecimal("100.25"),
                new BigDecimal("99.50"),
                new BigDecimal("101.00"),
                "15m",
                entrySourceRef,
                null,
                true
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedEntryContext untrustedEntry() {
        return SourceOwnedNumericPointCandidateAssembler.SourceOwnedEntryContext.of(
                new BigDecimal("100.25"),
                new BigDecimal("99.50"),
                new BigDecimal("101.00"),
                "15m",
                "source-entry",
                null,
                false
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedStopContext stop() {
        return stopWithSourceRef("source-stop");
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedStopContext stopWithSourceRef(
            String stopSourceRef
    ) {
        return SourceOwnedNumericPointCandidateAssembler.SourceOwnedStopContext.of(
                new BigDecimal("96.00"),
                new BigDecimal("95.50"),
                new BigDecimal("96.50"),
                "15m",
                stopSourceRef,
                null,
                true
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedStopContext untrustedStop() {
        return SourceOwnedNumericPointCandidateAssembler.SourceOwnedStopContext.of(
                new BigDecimal("96.00"),
                new BigDecimal("95.50"),
                new BigDecimal("96.50"),
                "15m",
                "source-stop",
                null,
                false
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedTakeProfitContext takeProfit() {
        return SourceOwnedNumericPointCandidateAssembler.SourceOwnedTakeProfitContext.of(
                1,
                new BigDecimal("108.00"),
                new BigDecimal("107.50"),
                new BigDecimal("108.50"),
                "1h",
                "source-tp",
                null,
                true
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedTakeProfitContext untrustedTakeProfit() {
        return SourceOwnedNumericPointCandidateAssembler.SourceOwnedTakeProfitContext.of(
                1,
                new BigDecimal("108.00"),
                new BigDecimal("107.50"),
                new BigDecimal("108.50"),
                "1h",
                "source-tp",
                null,
                false
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedRiskRewardContext riskReward() {
        return SourceOwnedNumericPointCandidateAssembler.SourceOwnedRiskRewardContext.of(
                new BigDecimal("2.00"),
                "REVIEW_ONLY_RISK_REWARD_TRACE",
                "source-entry",
                "source-stop",
                "source-tp",
                "source-owned-calculation-trace",
                null,
                true
        );
    }

    private SourceOwnedNumericPointCandidateAssembler.SourceOwnedRiskRewardContext untrustedRiskReward() {
        return SourceOwnedNumericPointCandidateAssembler.SourceOwnedRiskRewardContext.of(
                new BigDecimal("2.00"),
                "REVIEW_ONLY_RISK_REWARD_TRACE",
                "source-entry",
                "source-stop",
                "source-tp",
                "source-owned-calculation-trace",
                null,
                false
        );
    }

    private void assertIncompleteFor(
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private void assertBlockedFor(
            SourceOwnedNumericPointCandidateAssembler.SourceOwnedAssembledNumericPoint assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();

        assertThat(annotations).isEmpty();
    }

    private void assertSourceDoesNotContain(List<String> fragments) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/assembler/point/SourceOwnedNumericPointCandidateAssembler.java"
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

    private static class CountingProposalAssembler extends ReviewOnlyNumericPointProposalAssembler {
        private int invocationCount;

        @Override
        public AssembledReviewOnlyNumericPoint assemble(AssemblyInput input) {
            invocationCount++;
            return super.assemble(input);
        }
    }
}
