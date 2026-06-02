package org.example.trademodel.validator.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.ReviewOnlyNumericPointProposalDTO;
import org.junit.jupiter.api.Test;

class NumericPointSafetyValidatorTest {

    private final NumericPointSafetyValidator validator = new NumericPointSafetyValidator();

    @Test
    void nullProposalReturnsIncomplete() {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(null);

        assertThat(result.getStatus()).isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.isIncomplete()).isTrue();
        assertThat(result.getReasons()).containsExactly("PROPOSAL_MISSING");
    }

    @Test
    void incompleteProposalWithMissingReasonReturnsIncompleteAndNotBlocked() {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(incompleteProposal());

        assertThat(result.getStatus()).isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.isIncomplete()).isTrue();
        assertThat(result.isBlockedFailClosed()).isFalse();
        assertThat(result.getReasons()).containsExactly("SOURCE_TRACE_MISSING");
    }

    @Test
    void incompleteProposalWithoutMissingReasonIsSafelyIncomplete() throws Exception {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(
                proposalWith(
                        ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE,
                        List.of(),
                        List.of(),
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
                )
        );

        assertIncompleteFor(result, "MISSING_REASON_REQUIRED");
    }

    @Test
    void blockedProposalWithBlockedReasonReturnsBlockedFailClosed() {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(blockedProposal());

        assertThat(result.getStatus()).isEqualTo(NumericPointSafetyValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(result.isBlockedFailClosed()).isTrue();
        assertThat(result.getReasons()).containsExactly("RISK_ACTION_GUARD_BLOCKED");
    }

    @Test
    void blockedProposalWithoutBlockedReasonReturnsBlockedFailClosed() throws Exception {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(
                proposalWith(
                        ReviewOnlyNumericPointProposalDTO.ProposalStatus.BLOCKED_FAIL_CLOSED,
                        List.of(),
                        List.of(),
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
                )
        );

        assertBlockedFor(result, "BLOCKED_REASON_REQUIRED");
    }

    @Test
    void candidateWithAllRefsAndPointFieldsReturnsReviewOnlyCandidate() {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(candidateProposal());

        assertThat(result.getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.REVIEW_ONLY_NUMERIC_POINT_CANDIDATE);
        assertThat(result.isValidForReviewOnly()).isTrue();
        assertThat(result.isIncomplete()).isFalse();
        assertThat(result.isBlockedFailClosed()).isFalse();
        assertThat(result.isRecheckRequired()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
    }

    @Test
    void candidateMissingSourceTraceRefsReturnsIncomplete() {
        assertIncompleteFor(validator.validate(candidateWithSourceTraceRefs(List.of())), "SOURCE_TRACE_REF_MISSING");
    }

    @Test
    void candidateMissingRuntimeKlineContextRefsReturnsIncomplete() {
        assertIncompleteFor(
                validator.validate(candidateWithRuntimeKlineContextRefs(List.of())),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING"
        );
    }

    @Test
    void candidateMissingDataQualityContextRefReturnsIncomplete() {
        assertIncompleteFor(
                validator.validate(candidateWithDataQualityContextRef(null)),
                "DATA_QUALITY_CONTEXT_REF_MISSING"
        );
    }

    @Test
    void candidateMissingMultiTimeframeContextRefReturnsIncomplete() {
        assertIncompleteFor(
                validator.validate(candidateWithMultiTimeframeContextRef(" ")),
                "MULTI_TIMEFRAME_CONTEXT_REF_MISSING"
        );
    }

    @Test
    void candidateMissingRiskActionGuardRefReturnsIncomplete() {
        assertIncompleteFor(
                validator.validate(candidateWithRiskActionGuardRef(null)),
                "RISK_ACTION_GUARD_REF_MISSING"
        );
    }

    @Test
    void candidateMissingWatchlistPoolProofReturnsIncomplete() {
        assertIncompleteFor(
                validator.validate(candidateWithWatchlistPoolProof("")),
                "WATCHLIST_POOL_PROOF_MISSING"
        );
    }

    @Test
    void candidateMissingEntryReturnsIncomplete() {
        assertIncompleteFor(validator.validate(candidateWithEntry(null)), "ENTRY_REVIEW_POINT_MISSING");
    }

    @Test
    void candidateMissingStopReturnsIncomplete() {
        assertIncompleteFor(validator.validate(candidateWithStop(null)), "STOP_REVIEW_POINT_MISSING");
    }

    @Test
    void candidateMissingTakeProfitReturnsIncomplete() {
        assertIncompleteFor(
                validator.validate(candidateWithTakeProfitLevels(List.of())),
                "TAKE_PROFIT_REVIEW_LEVEL_MISSING"
        );
    }

    @Test
    void candidateMissingRiskRewardReturnsIncomplete() {
        assertIncompleteFor(
                validator.validate(candidateWithRiskReward(null)),
                "RISK_REWARD_REVIEW_FIELD_MISSING"
        );
    }

    @Test
    void degradedProposalWithMissingReasonReturnsReviewOnlyDegraded() {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(degradedProposal());

        assertThat(result.getStatus())
                .isEqualTo(NumericPointSafetyValidator.ValidationStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED);
        assertThat(result.isValidForReviewOnly()).isTrue();
        assertThat(result.isRecheckRequired()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.getReasons()).containsExactly("TP_SOURCE_MISSING");
    }

    @Test
    void degradedProposalWithoutMissingReasonReturnsIncomplete() throws Exception {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(
                proposalWith(
                        ReviewOnlyNumericPointProposalDTO.ProposalStatus.REVIEW_ONLY_NUMERIC_POINT_DEGRADED,
                        List.of("source-entry", "source-stop"),
                        List.of("runtime-15m"),
                        List.of(),
                        List.of(),
                        "dq-1",
                        "mtf-1",
                        "rag-1",
                        "watchlist:BTCUSDT:v1",
                        entry(),
                        stop(),
                        List.of(),
                        null,
                        List.of()
                )
        );

        assertIncompleteFor(result, "MISSING_REASON_REQUIRED");
    }

    @Test
    void forbiddenExecutableSemanticInReasonReturnsBlockedFailClosed() throws Exception {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(
                proposalWith(
                        ReviewOnlyNumericPointProposalDTO.ProposalStatus.INCOMPLETE,
                        List.of(),
                        List.of(),
                        List.of("buy"),
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
                )
        );

        assertBlockedFor(result, "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void forbiddenExecutableSemanticInForbiddenSemanticsReturnsBlockedFailClosed() {
        NumericPointSafetyValidator.ValidationResult result = validator.validate(
                ReviewOnlyNumericPointProposalDTO.reviewOnlyCandidate(
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
                        List.of("send order")
                )
        );

        assertBlockedFor(result, "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void dtoSafetyFlagsAreRequiredTrue() throws Exception {
        ReviewOnlyNumericPointProposalDTO proposal = candidateProposal();
        forceBoolean(proposal, "reviewOnly", false);

        NumericPointSafetyValidator.ValidationResult result = validator.validate(proposal);

        assertBlockedFor(result, "SAFETY_FLAG_REQUIRED");
    }

    @Test
    void validatorResultNeverContainsForbiddenExecutableSemanticsInNormalSafeResults() {
        List<NumericPointSafetyValidator.ValidationResult> results = List.of(
                validator.validate(incompleteProposal()),
                validator.validate(blockedProposal()),
                validator.validate(degradedProposal()),
                validator.validate(candidateProposal())
        );

        for (NumericPointSafetyValidator.ValidationResult result : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(result.getStatus().name());
            outputs.addAll(result.getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    @Test
    void validatorHasNoSpringAnnotations() {
        assertNoAnnotations(NumericPointSafetyValidator.class);
        assertNoAnnotations(NumericPointSafetyValidator.ValidationResult.class);
    }

    @Test
    void validatorDoesNotReferenceServiceControllerMapperRepositoryOrScheduler() throws Exception {
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
    void validatorDoesNotReferenceMarketQuoteHttpOrDataSource() throws Exception {
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
    void validatorDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
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

    private ReviewOnlyNumericPointProposalDTO candidateWithSourceTraceRefs(List<String> sourceTraceRefs) {
        return candidateWith(sourceTraceRefs, List.of("runtime-15m", "runtime-1h"), "dq-1", "mtf-1", "rag-1",
                "watchlist:BTCUSDT:v1", entry(), stop(), List.of(takeProfit()), riskReward());
    }

    private ReviewOnlyNumericPointProposalDTO candidateWithRuntimeKlineContextRefs(List<String> runtimeRefs) {
        return candidateWith(List.of("source-entry", "source-stop", "source-tp"), runtimeRefs, "dq-1", "mtf-1",
                "rag-1", "watchlist:BTCUSDT:v1", entry(), stop(), List.of(takeProfit()), riskReward());
    }

    private ReviewOnlyNumericPointProposalDTO candidateWithDataQualityContextRef(String dataQualityContextRef) {
        return candidateWith(List.of("source-entry", "source-stop", "source-tp"), List.of("runtime-15m"), 
                dataQualityContextRef, "mtf-1", "rag-1", "watchlist:BTCUSDT:v1", entry(), stop(),
                List.of(takeProfit()), riskReward());
    }

    private ReviewOnlyNumericPointProposalDTO candidateWithMultiTimeframeContextRef(String multiTimeframeContextRef) {
        return candidateWith(List.of("source-entry", "source-stop", "source-tp"), List.of("runtime-15m"), "dq-1",
                multiTimeframeContextRef, "rag-1", "watchlist:BTCUSDT:v1", entry(), stop(), List.of(takeProfit()),
                riskReward());
    }

    private ReviewOnlyNumericPointProposalDTO candidateWithRiskActionGuardRef(String riskActionGuardRef) {
        return candidateWith(List.of("source-entry", "source-stop", "source-tp"), List.of("runtime-15m"), "dq-1",
                "mtf-1", riskActionGuardRef, "watchlist:BTCUSDT:v1", entry(), stop(), List.of(takeProfit()),
                riskReward());
    }

    private ReviewOnlyNumericPointProposalDTO candidateWithWatchlistPoolProof(String watchlistPoolProof) {
        return candidateWith(List.of("source-entry", "source-stop", "source-tp"), List.of("runtime-15m"), "dq-1",
                "mtf-1", "rag-1", watchlistPoolProof, entry(), stop(), List.of(takeProfit()), riskReward());
    }

    private ReviewOnlyNumericPointProposalDTO candidateWithEntry(
            ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry
    ) {
        return candidateWith(List.of("source-entry", "source-stop", "source-tp"), List.of("runtime-15m"), "dq-1",
                "mtf-1", "rag-1", "watchlist:BTCUSDT:v1", entry, stop(), List.of(takeProfit()), riskReward());
    }

    private ReviewOnlyNumericPointProposalDTO candidateWithStop(
            ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop
    ) {
        return candidateWith(List.of("source-entry", "source-stop", "source-tp"), List.of("runtime-15m"), "dq-1",
                "mtf-1", "rag-1", "watchlist:BTCUSDT:v1", entry(), stop, List.of(takeProfit()), riskReward());
    }

    private ReviewOnlyNumericPointProposalDTO candidateWithTakeProfitLevels(
            List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels
    ) {
        return candidateWith(List.of("source-entry", "source-stop", "source-tp"), List.of("runtime-15m"), "dq-1",
                "mtf-1", "rag-1", "watchlist:BTCUSDT:v1", entry(), stop(), takeProfitLevels, riskReward());
    }

    private ReviewOnlyNumericPointProposalDTO candidateWithRiskReward(
            ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward
    ) {
        return candidateWith(List.of("source-entry", "source-stop", "source-tp"), List.of("runtime-15m"), "dq-1",
                "mtf-1", "rag-1", "watchlist:BTCUSDT:v1", entry(), stop(), List.of(takeProfit()), riskReward);
    }

    private ReviewOnlyNumericPointProposalDTO candidateWith(
            List<String> sourceTraceRefs,
            List<String> runtimeKlineContextRefs,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String watchlistPoolProof,
            ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry,
            ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop,
            List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels,
            ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward
    ) {
        return ReviewOnlyNumericPointProposalDTO.reviewOnlyCandidate(
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

    private ReviewOnlyNumericPointProposalDTO proposalWith(
            ReviewOnlyNumericPointProposalDTO.ProposalStatus status,
            List<String> sourceTraceRefs,
            List<String> runtimeKlineContextRefs,
            List<String> missingReasons,
            List<String> blockedReasons,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String watchlistPoolProof,
            ReviewOnlyNumericPointProposalDTO.EntryReviewPoint entry,
            ReviewOnlyNumericPointProposalDTO.StopReviewPoint stop,
            List<ReviewOnlyNumericPointProposalDTO.TakeProfitReviewLevel> takeProfitLevels,
            ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField riskReward,
            List<String> forbiddenSemantics
    ) throws Exception {
        Constructor<ReviewOnlyNumericPointProposalDTO> constructor =
                ReviewOnlyNumericPointProposalDTO.class.getDeclaredConstructor(
                        String.class,
                        String.class,
                        List.class,
                        ReviewOnlyNumericPointProposalDTO.ProposalStatus.class,
                        List.class,
                        List.class,
                        String.class,
                        String.class,
                        String.class,
                        String.class,
                        ReviewOnlyNumericPointProposalDTO.EntryReviewPoint.class,
                        ReviewOnlyNumericPointProposalDTO.StopReviewPoint.class,
                        List.class,
                        ReviewOnlyNumericPointProposalDTO.RiskRewardReviewField.class,
                        List.class,
                        List.class,
                        List.class
                );
        constructor.setAccessible(true);
        return constructor.newInstance(
                "BTCUSDT",
                "SPOT",
                List.of("15m", "1h"),
                status,
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
                forbiddenSemantics
        );
    }

    private void forceBoolean(ReviewOnlyNumericPointProposalDTO proposal, String fieldName, boolean value)
            throws Exception {
        Field field = ReviewOnlyNumericPointProposalDTO.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setBoolean(proposal, value);
    }

    private void assertIncompleteFor(NumericPointSafetyValidator.ValidationResult result, String reason) {
        assertThat(result.getStatus()).isEqualTo(NumericPointSafetyValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.isIncomplete()).isTrue();
        assertThat(result.isBlockedFailClosed()).isFalse();
        assertThat(result.getReasons()).contains(reason);
    }

    private void assertBlockedFor(NumericPointSafetyValidator.ValidationResult result, String reason) {
        assertThat(result.getStatus()).isEqualTo(NumericPointSafetyValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(result.isBlockedFailClosed()).isTrue();
        assertThat(result.getReasons()).contains(reason);
    }

    private void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();

        assertThat(annotations).isEmpty();
    }

    private void assertSourceDoesNotContain(List<String> fragments) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/validator/point/NumericPointSafetyValidator.java"
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
}
