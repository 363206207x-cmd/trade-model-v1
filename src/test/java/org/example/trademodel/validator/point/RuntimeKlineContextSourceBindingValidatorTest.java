package org.example.trademodel.validator.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.RuntimeKlineContextSourceBindingDTO;
import org.junit.jupiter.api.Test;

class RuntimeKlineContextSourceBindingValidatorTest {

    private final RuntimeKlineContextSourceBindingValidator validator =
            new RuntimeKlineContextSourceBindingValidator();

    @Test
    void nullContextReturnsIncomplete() {
        RuntimeKlineContextSourceBindingValidator.ValidationResult result = validator.validate(null);

        assertIncompleteFor(result, "RUNTIME_KLINE_CONTEXT_BINDING_MISSING");
    }

    @Test
    void incompleteContextWithMissingReasonReturnsIncomplete() {
        RuntimeKlineContextSourceBindingValidator.ValidationResult result = validator.validate(incompleteContext());

        assertIncompleteFor(result, "RUNTIME_KLINE_CONTEXT_MISSING");
    }

    @Test
    void incompleteContextWithoutMissingReasonIsSafelyIncomplete() throws Exception {
        RuntimeKlineContextSourceBindingDTO context = incompleteContext();
        forceField(context, "missingReason", null);

        RuntimeKlineContextSourceBindingValidator.ValidationResult result = validator.validate(context);

        assertIncompleteFor(result, "MISSING_REASON_REQUIRED");
    }

    @Test
    void blockedContextWithBlockedReasonReturnsBlockedFailClosed() {
        RuntimeKlineContextSourceBindingValidator.ValidationResult result = validator.validate(blockedContext());

        assertBlockedFor(result, "RUNTIME_CONTEXT_BLOCKED");
    }

    @Test
    void blockedContextWithoutBlockedReasonReturnsBlockedFailClosed() throws Exception {
        RuntimeKlineContextSourceBindingDTO context = blockedContext();
        forceField(context, "blockedReason", null);

        RuntimeKlineContextSourceBindingValidator.ValidationResult result = validator.validate(context);

        assertBlockedFor(result, "BLOCKED_REASON_REQUIRED");
    }

    @Test
    void reviewOnlyBindingWithCompleteContextReturnsReviewOnlyRuntimeKlineBinding() {
        RuntimeKlineContextSourceBindingValidator.ValidationResult result = validator.validate(completeContext());

        assertThat(result.getStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING);
        assertThat(result.isValidForReviewOnly()).isTrue();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isIncomplete()).isFalse();
        assertThat(result.isBlockedFailClosed()).isFalse();
    }

    @Test
    void degradedBindingWithMissingReasonReturnsReviewOnlyRuntimeKlineBindingDegraded() {
        RuntimeKlineContextSourceBindingValidator.ValidationResult result = validator.validate(degradedContext());

        assertThat(result.getStatus()).isEqualTo(
                RuntimeKlineContextSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED
        );
        assertThat(result.isValidForReviewOnly()).isTrue();
        assertThat(result.getReasons()).containsExactly("RUNTIME_CONTEXT_DEGRADED");
    }

    @Test
    void degradedBindingWithoutMissingReasonReturnsIncomplete() throws Exception {
        RuntimeKlineContextSourceBindingDTO context = degradedContext();
        forceField(context, "missingReason", null);

        RuntimeKlineContextSourceBindingValidator.ValidationResult result = validator.validate(context);

        assertIncompleteFor(result, "MISSING_REASON_REQUIRED");
    }

    @Test
    void missingRuntimeKlineContextIdReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("runtimeKlineContextId", null)),
                "RUNTIME_KLINE_CONTEXT_ID_MISSING");
    }

    @Test
    void missingSymbolReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("symbol", " ")), "SYMBOL_MISSING");
    }

    @Test
    void missingMarketReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("market", null)), "MARKET_MISSING");
    }

    @Test
    void missingTimeframeReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("timeframe", "")), "TIMEFRAME_MISSING");
    }

    @Test
    void missingKlineWindowReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("klineWindow", null)), "KLINE_WINDOW_MISSING");
    }

    @Test
    void missingLatestPriceReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("latestPrice", null)), "LATEST_PRICE_MISSING");
    }

    @Test
    void missingLatestCloseReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("latestClose", null)), "LATEST_CLOSE_MISSING");
    }

    @Test
    void missingOpenReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("open", null)), "OPEN_MISSING");
    }

    @Test
    void missingHighReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("high", null)), "HIGH_MISSING");
    }

    @Test
    void missingLowReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("low", null)), "LOW_MISSING");
    }

    @Test
    void missingCloseReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("close", null)), "CLOSE_MISSING");
    }

    @Test
    void missingVolumeReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("volume", null)), "VOLUME_MISSING");
    }

    @Test
    void candleClosedNullReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("candleClosed", null)), "CANDLE_CLOSED_MISSING");
    }

    @Test
    void candleClosedFalseReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("candleClosed", Boolean.FALSE)),
                "CANDLE_NOT_CLOSED");
    }

    @Test
    void missingOhlcvCompletenessReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("ohlcvCompleteness", null)),
                "OHLCV_COMPLETENESS_MISSING");
    }

    @Test
    void lowOhlcvCompletenessReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField("ohlcvCompleteness", new BigDecimal("69.99"))),
                "OHLCV_COMPLETENESS_LOW"
        );
    }

    @Test
    void staleFreshnessReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField(
                        "freshnessStatus",
                        RuntimeKlineContextSourceBindingDTO.FreshnessStatus.STALE
                )),
                "FRESHNESS_STALE"
        );
    }

    @Test
    void unknownFreshnessReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField(
                        "freshnessStatus",
                        RuntimeKlineContextSourceBindingDTO.FreshnessStatus.UNKNOWN
                )),
                "FRESHNESS_UNKNOWN"
        );
    }

    @Test
    void nullFreshnessReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("freshnessStatus", null)), "FRESHNESS_MISSING");
    }

    @Test
    void wickOnlyReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField(
                        "wickStatus",
                        RuntimeKlineContextSourceBindingDTO.WickStatus.WICK_ONLY
                )),
                "WICK_ONLY_INCOMPLETE"
        );
    }

    @Test
    void unknownWickReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField(
                        "wickStatus",
                        RuntimeKlineContextSourceBindingDTO.WickStatus.UNKNOWN
                )),
                "WICK_UNKNOWN"
        );
    }

    @Test
    void severeGapReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(
                validator.validate(contextWithField("gapStatus", RuntimeKlineContextSourceBindingDTO.GapStatus.SEVERE_GAP)),
                "SEVERE_GAP"
        );
    }

    @Test
    void unknownGapReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField("gapStatus", RuntimeKlineContextSourceBindingDTO.GapStatus.UNKNOWN)),
                "GAP_UNKNOWN"
        );
    }

    @Test
    void severelyDegradedLiquidityReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField(
                        "liquidityState",
                        RuntimeKlineContextSourceBindingDTO.LiquidityState.SEVERELY_DEGRADED
                )),
                "LIQUIDITY_SEVERELY_DEGRADED"
        );
    }

    @Test
    void unknownLiquidityReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField(
                        "liquidityState",
                        RuntimeKlineContextSourceBindingDTO.LiquidityState.UNKNOWN
                )),
                "LIQUIDITY_UNKNOWN"
        );
    }

    @Test
    void stampedeSuspectedReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField(
                        "stampedeState",
                        RuntimeKlineContextSourceBindingDTO.StampedeState.SUSPECTED
                )),
                "STAMPEDE_SUSPECTED"
        );
    }

    @Test
    void stampedeConfirmedReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(
                validator.validate(contextWithField(
                        "stampedeState",
                        RuntimeKlineContextSourceBindingDTO.StampedeState.CONFIRMED
                )),
                "STAMPEDE_CONFIRMED"
        );
    }

    @Test
    void stampedeUnknownReturnsIncomplete() throws Exception {
        assertIncompleteFor(
                validator.validate(contextWithField(
                        "stampedeState",
                        RuntimeKlineContextSourceBindingDTO.StampedeState.UNKNOWN
                )),
                "STAMPEDE_UNKNOWN"
        );
    }

    @Test
    void sourceTraceRefsNullReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("sourceTraceRefs", null)),
                "SOURCE_TRACE_REFS_MISSING");
    }

    @Test
    void sourceTraceRefsEmptyReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithSourceTraceRefs(List.of())),
                "SOURCE_TRACE_REFS_MISSING");
    }

    @Test
    void sourceTraceRefsBlankItemReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithSourceTraceRefs(List.of("source-trace-1", " "))),
                "SOURCE_TRACE_REF_BLANK");
    }

    @Test
    void missingMarketDataSourceRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("marketDataSourceRef", "")),
                "MARKET_DATA_SOURCE_REF_MISSING");
    }

    @Test
    void missingObservedAtReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("observedAt", " ")), "OBSERVED_AT_MISSING");
    }

    @Test
    void untrustedSourceReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("trustedSource", false)),
                "RUNTIME_KLINE_SOURCE_UNTRUSTED");
    }

    @Test
    void safetyFlagFalseReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("reviewOnly", false)), "SAFETY_FLAG_REQUIRED");
        assertBlockedFor(validator.validate(contextWithField("notTradeInstruction", false)), "SAFETY_FLAG_REQUIRED");
        assertBlockedFor(validator.validate(contextWithField("manualReviewRequired", false)), "SAFETY_FLAG_REQUIRED");
        assertBlockedFor(validator.validate(contextWithField("incompleteSafe", false)), "SAFETY_FLAG_REQUIRED");
    }

    @Test
    void forbiddenExecutableSemanticInMissingReasonReturnsBlockedFailClosed() throws Exception {
        RuntimeKlineContextSourceBindingDTO context = incompleteContext();
        forceField(context, "missingReason", "buy");

        assertBlockedFor(validator.validate(context), "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void forbiddenExecutableSemanticInBlockedReasonReturnsBlockedFailClosed() throws Exception {
        RuntimeKlineContextSourceBindingDTO context = blockedContext();
        forceField(context, "blockedReason", "send order");

        assertBlockedFor(validator.validate(context), "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void forbiddenExecutableSemanticInSourceTraceRefsReturnsBlockedFailClosed() {
        assertBlockedFor(
                validator.validate(contextWithSourceTraceRefs(List.of("source-trace-buy"))),
                "FORBIDDEN_SEMANTIC_DETECTED"
        );
    }

    @Test
    void forbiddenExecutableSemanticInMarketDataSourceRefReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(
                validator.validate(contextWithField("marketDataSourceRef", "execute-now")),
                "FORBIDDEN_SEMANTIC_DETECTED"
        );
    }

    @Test
    void validatorResultNormalSafeOutputsDoNotContainForbiddenExecutableSemantics() {
        List<RuntimeKlineContextSourceBindingValidator.ValidationResult> results = List.of(
                validator.validate(incompleteContext()),
                validator.validate(blockedContext()),
                validator.validate(degradedContext()),
                validator.validate(completeContext())
        );

        for (RuntimeKlineContextSourceBindingValidator.ValidationResult result : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(result.getStatus().name());
            outputs.addAll(result.getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    @Test
    void validatorHasNoSpringAnnotations() {
        assertNoAnnotations(RuntimeKlineContextSourceBindingValidator.class);
        assertNoAnnotations(RuntimeKlineContextSourceBindingValidator.ValidationResult.class);
    }

    @Test
    void validatorDoesNotReferenceAssemblerServiceControllerMapperRepositoryOrScheduler() throws Exception {
        assertSourceDoesNotContain(List.of(
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "Assembler",
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
                "javax.sql.DataSource",
                "import javax.sql",
                "DataSource ",
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

    private RuntimeKlineContextSourceBindingDTO incompleteContext() {
        return RuntimeKlineContextSourceBindingDTO.incomplete(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                List.of("source-trace-entry"),
                "RUNTIME_KLINE_CONTEXT_MISSING"
        );
    }

    private RuntimeKlineContextSourceBindingDTO blockedContext() {
        return RuntimeKlineContextSourceBindingDTO.blockedFailClosed(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                List.of("source-trace-entry"),
                "RUNTIME_CONTEXT_BLOCKED"
        );
    }

    private RuntimeKlineContextSourceBindingDTO degradedContext() {
        return RuntimeKlineContextSourceBindingDTO.degraded(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                new BigDecimal("100.25"),
                new BigDecimal("100.10"),
                new BigDecimal("99.90"),
                new BigDecimal("101.20"),
                new BigDecimal("99.50"),
                new BigDecimal("100.10"),
                new BigDecimal("120.0"),
                null,
                Boolean.TRUE,
                new BigDecimal("95"),
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.NORMAL,
                "LOW",
                RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                List.of("source-trace-entry", "source-trace-stop"),
                "market-data-ref-1",
                "2026-06-02T07:15:01Z",
                "2026-06-02T07:15:02Z",
                "RUNTIME_CONTEXT_DEGRADED"
        );
    }

    private RuntimeKlineContextSourceBindingDTO completeContext() {
        return RuntimeKlineContextSourceBindingDTO.reviewOnly(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                new BigDecimal("100.25"),
                new BigDecimal("100.10"),
                new BigDecimal("99.90"),
                new BigDecimal("101.20"),
                new BigDecimal("99.50"),
                new BigDecimal("100.10"),
                new BigDecimal("120.0"),
                new BigDecimal("12000.0"),
                Boolean.TRUE,
                new BigDecimal("95"),
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.NORMAL,
                "LOW",
                RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                List.of("source-trace-entry", "source-trace-stop"),
                "market-data-ref-1",
                "2026-06-02T07:15:01Z",
                "2026-06-02T07:15:02Z"
        );
    }

    private RuntimeKlineContextSourceBindingDTO contextWithSourceTraceRefs(List<String> refs) {
        return RuntimeKlineContextSourceBindingDTO.reviewOnly(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                new BigDecimal("100.25"),
                new BigDecimal("100.10"),
                new BigDecimal("99.90"),
                new BigDecimal("101.20"),
                new BigDecimal("99.50"),
                new BigDecimal("100.10"),
                new BigDecimal("120.0"),
                new BigDecimal("12000.0"),
                Boolean.TRUE,
                new BigDecimal("95"),
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.NORMAL,
                "LOW",
                RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                refs,
                "market-data-ref-1",
                "2026-06-02T07:15:01Z",
                "2026-06-02T07:15:02Z"
        );
    }

    private RuntimeKlineContextSourceBindingDTO contextWithField(String fieldName, Object value) throws Exception {
        RuntimeKlineContextSourceBindingDTO context = completeContext();
        forceField(context, fieldName, value);
        return context;
    }

    private void forceField(RuntimeKlineContextSourceBindingDTO context, String fieldName, Object value)
            throws Exception {
        Field field = RuntimeKlineContextSourceBindingDTO.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(context, value);
    }

    private void assertIncompleteFor(
            RuntimeKlineContextSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus()).isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.isIncomplete()).isTrue();
        assertThat(result.isBlockedFailClosed()).isFalse();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.getReasons()).contains(reason);
    }

    private void assertBlockedFor(
            RuntimeKlineContextSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(result.isBlockedFailClosed()).isTrue();
        assertThat(result.isIncomplete()).isFalse();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.getReasons()).contains(reason);
    }

    private void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();

        assertThat(annotations).isEmpty();
    }

    private void assertSourceDoesNotContain(List<String> fragments) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/validator/point/"
                        + "RuntimeKlineContextSourceBindingValidator.java"
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
