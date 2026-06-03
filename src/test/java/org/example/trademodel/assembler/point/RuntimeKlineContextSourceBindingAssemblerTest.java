package org.example.trademodel.assembler.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.RuntimeKlineContextSourceBindingDTO;
import org.example.trademodel.validator.point.RuntimeKlineContextSourceBindingValidator;
import org.junit.jupiter.api.Test;

class RuntimeKlineContextSourceBindingAssemblerTest {

    private final RuntimeKlineContextSourceBindingAssembler assembler =
            new RuntimeKlineContextSourceBindingAssembler();

    @Test
    void nullInputCreatesIncompleteContext() {
        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(null);

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingDTO.BindingStatus.INCOMPLETE);
        assertThat(assembled.getContext().getMissingReason()).isEqualTo("RUNTIME_KLINE_BINDING_INPUT_MISSING");
    }

    @Test
    void nullInputCreatesIncompleteValidation() {
        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(null);

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons())
                .containsExactly("RUNTIME_KLINE_BINDING_INPUT_MISSING");
    }

    @Test
    void incompleteInputWithMissingReasonCreatesIncompleteValidation() {
        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(incompleteInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingDTO.BindingStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("RUNTIME_BINDING_MISSING");
    }

    @Test
    void blockedInputWithBlockedReasonCreatesBlockedFailClosedValidation() {
        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(blockedInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getContext().isFailClosed()).isTrue();
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("RUNTIME_BINDING_BLOCKED");
    }

    @Test
    void reviewOnlyInputWithCompleteContextCreatesReviewOnlyValidation() {
        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING);
        assertThat(assembled.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void degradedInputWithMissingReasonCreatesReviewOnlyRuntimeKlineBindingDegradedValidation() {
        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(degradedInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        RuntimeKlineContextSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED
                );
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("RUNTIME_BINDING_DEGRADED");
    }

    @Test
    void missingRuntimeKlineContextIdCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithRuntimeKlineContextId(null)),
                "RUNTIME_KLINE_CONTEXT_ID_MISSING");
    }

    @Test
    void missingSymbolCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSymbol(" ")), "SYMBOL_MISSING");
    }

    @Test
    void missingLatestPriceCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithLatestPrice(null)), "LATEST_PRICE_MISSING");
    }

    @Test
    void missingLatestCloseCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithLatestClose(null)), "LATEST_CLOSE_MISSING");
    }

    @Test
    void missingOhlcvFieldCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithOpen(null)), "OPEN_MISSING");
    }

    @Test
    void candleClosedFalseCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithCandleClosed(Boolean.FALSE)), "CANDLE_NOT_CLOSED");
    }

    @Test
    void staleFreshnessCreatesIncompleteValidation() {
        assertIncompleteFor(
                assembler.assemble(inputWithFreshness(RuntimeKlineContextSourceBindingDTO.FreshnessStatus.STALE)),
                "FRESHNESS_STALE"
        );
    }

    @Test
    void wickOnlyCreatesIncompleteValidation() {
        assertIncompleteFor(
                assembler.assemble(inputWithWick(RuntimeKlineContextSourceBindingDTO.WickStatus.WICK_ONLY)),
                "WICK_ONLY_INCOMPLETE"
        );
    }

    @Test
    void severeGapCreatesBlockedFailClosedValidation() {
        assertBlockedFor(
                assembler.assemble(inputWithGap(RuntimeKlineContextSourceBindingDTO.GapStatus.SEVERE_GAP)),
                "SEVERE_GAP"
        );
    }

    @Test
    void stampedeConfirmedCreatesBlockedFailClosedValidation() {
        assertBlockedFor(
                assembler.assemble(inputWithStampede(RuntimeKlineContextSourceBindingDTO.StampedeState.CONFIRMED)),
                "STAMPEDE_CONFIRMED"
        );
    }

    @Test
    void sourceTraceRefsEmptyCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSourceTraceRefs(List.of())),
                "SOURCE_TRACE_REFS_MISSING");
    }

    @Test
    void sourceTraceRefsBlankItemCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSourceTraceRefs(List.of("runtime-source-ref", " "))),
                "SOURCE_TRACE_REF_BLANK");
    }

    @Test
    void forbiddenExecutableSemanticInSourceTraceRefsCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithSourceTraceRefs(List.of("buy-signal-ref"))),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void forbiddenExecutableSemanticInMarketDataSourceRefCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithMarketDataSourceRef("send order feed")),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void explicitBigDecimalLatestPriceAndLatestCloseArePreserved() {
        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getLatestPrice()).isEqualByComparingTo("100.25");
        assertThat(assembled.getContext().getLatestClose()).isEqualByComparingTo("100.10");
    }

    @Test
    void explicitOhlcvFieldsArePreserved() {
        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getOpen()).isEqualByComparingTo("99.90");
        assertThat(assembled.getContext().getHigh()).isEqualByComparingTo("101.00");
        assertThat(assembled.getContext().getLow()).isEqualByComparingTo("99.00");
        assertThat(assembled.getContext().getClose()).isEqualByComparingTo("100.10");
        assertThat(assembled.getContext().getVolume()).isEqualByComparingTo("1200.00");
        assertThat(assembled.getContext().getQuoteVolume()).isEqualByComparingTo("120000.00");
    }

    @Test
    void explicitSourceTraceRefsArePreservedAndDefensivelyCopied() {
        List<String> refs = new ArrayList<>();
        refs.add("runtime-source-ref");

        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(inputWithSourceTraceRefs(refs));
        refs.add("mutated-ref");

        assertThat(assembled.getContext().getSourceTraceRefs()).containsExactly("runtime-source-ref");
    }

    @Test
    void assemblerCallsValidatorAndReturnsValidationResult() {
        CountingValidator countingValidator = new CountingValidator();
        RuntimeKlineContextSourceBindingAssembler countingAssembler =
                new RuntimeKlineContextSourceBindingAssembler(countingValidator);

        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                countingAssembler.assemble(completeInput());

        assertThat(countingValidator.invocationCount).isEqualTo(1);
        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerHandlesNullFieldsWithoutException() {
        RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled =
                assembler.assemble(inputWithNullFields());

        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
    }

    @Test
    void assemblerClassHasNoSpringAnnotations() {
        assertNoAnnotations(RuntimeKlineContextSourceBindingAssembler.class);
        assertNoAnnotations(RuntimeKlineContextSourceBindingAssembler.AssemblyInput.class);
        assertNoAnnotations(RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding.class);
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
    void assemblerDoesNotReferenceMarketQuoteHttpOrDataSourceProviders() throws Exception {
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
    void assemblerSafeOutputDoesNotContainForbiddenExecutableSemantics() {
        List<RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding> assembledResults =
                List.of(
                        assembler.assemble(incompleteInput()),
                        assembler.assemble(blockedInput()),
                        assembler.assemble(degradedInput()),
                        assembler.assemble(completeInput())
                );

        for (RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled
                : assembledResults) {
            List<String> outputs = new ArrayList<>();
            outputs.add(assembled.getContext().getBindingStatus().name());
            outputs.add(assembled.getContext().getMissingReason());
            outputs.add(assembled.getContext().getBlockedReason());
            outputs.add(assembled.getValidationResult().getStatus().name());
            outputs.addAll(assembled.getValidationResult().getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput incompleteInput() {
        return input(
                "runtime-kline-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "15m-window",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.UNKNOWN,
                RuntimeKlineContextSourceBindingDTO.WickStatus.UNKNOWN,
                RuntimeKlineContextSourceBindingDTO.GapStatus.UNKNOWN,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.UNKNOWN,
                null,
                RuntimeKlineContextSourceBindingDTO.StampedeState.UNKNOWN,
                List.of(),
                null,
                null,
                null,
                "RUNTIME_BINDING_MISSING",
                null,
                Boolean.TRUE,
                RuntimeKlineContextSourceBindingDTO.BindingStatus.INCOMPLETE
        );
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput blockedInput() {
        return input(
                "runtime-kline-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "15m-window",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.UNKNOWN,
                RuntimeKlineContextSourceBindingDTO.WickStatus.UNKNOWN,
                RuntimeKlineContextSourceBindingDTO.GapStatus.UNKNOWN,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.UNKNOWN,
                null,
                RuntimeKlineContextSourceBindingDTO.StampedeState.UNKNOWN,
                List.of("runtime-source-ref"),
                null,
                null,
                null,
                null,
                "RUNTIME_BINDING_BLOCKED",
                Boolean.FALSE,
                RuntimeKlineContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED
        );
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput degradedInput() {
        return input(
                "runtime-kline-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "15m-window",
                bd("100.25"),
                bd("100.10"),
                bd("99.90"),
                bd("101.00"),
                bd("99.00"),
                bd("100.10"),
                bd("1200.00"),
                null,
                Boolean.TRUE,
                bd("95"),
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.DEGRADED,
                "WATCH_ONLY_DEGRADED",
                RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                List.of("runtime-source-ref"),
                "market-data-source-ref",
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "RUNTIME_BINDING_DEGRADED",
                null,
                Boolean.TRUE,
                RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED
        );
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput completeInput() {
        return input(
                "runtime-kline-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "15m-window",
                bd("100.25"),
                bd("100.10"),
                bd("99.90"),
                bd("101.00"),
                bd("99.00"),
                bd("100.10"),
                bd("1200.00"),
                bd("120000.00"),
                Boolean.TRUE,
                bd("95"),
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.NORMAL,
                "NORMAL",
                RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                List.of("runtime-source-ref"),
                "market-data-source-ref",
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING
        );
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithRuntimeKlineContextId(String value) {
        return inputWith(0, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithSymbol(String value) {
        return inputWith(1, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithLatestPrice(BigDecimal value) {
        return inputWith(5, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithLatestClose(BigDecimal value) {
        return inputWith(6, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithOpen(BigDecimal value) {
        return inputWith(7, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithCandleClosed(Boolean value) {
        return inputWith(13, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithFreshness(
            RuntimeKlineContextSourceBindingDTO.FreshnessStatus value
    ) {
        return inputWith(15, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithWick(
            RuntimeKlineContextSourceBindingDTO.WickStatus value
    ) {
        return inputWith(16, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithGap(
            RuntimeKlineContextSourceBindingDTO.GapStatus value
    ) {
        return inputWith(17, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithStampede(
            RuntimeKlineContextSourceBindingDTO.StampedeState value
    ) {
        return inputWith(20, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithSourceTraceRefs(List<String> value) {
        return inputWith(21, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithMarketDataSourceRef(String value) {
        return inputWith(22, value);
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWithNullFields() {
        return input(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING
        );
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputWith(int index, Object value) {
        Object[] values = completeValues();
        values[index] = value;
        return inputFrom(values);
    }

    private Object[] completeValues() {
        return new Object[] {
                "runtime-kline-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "15m-window",
                bd("100.25"),
                bd("100.10"),
                bd("99.90"),
                bd("101.00"),
                bd("99.00"),
                bd("100.10"),
                bd("1200.00"),
                bd("120000.00"),
                Boolean.TRUE,
                bd("95"),
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.NORMAL,
                "NORMAL",
                RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                List.of("runtime-source-ref"),
                "market-data-source-ref",
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING
        };
    }

    @SuppressWarnings("unchecked")
    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput inputFrom(Object[] values) {
        return input(
                (String) values[0],
                (String) values[1],
                (String) values[2],
                (String) values[3],
                (String) values[4],
                (BigDecimal) values[5],
                (BigDecimal) values[6],
                (BigDecimal) values[7],
                (BigDecimal) values[8],
                (BigDecimal) values[9],
                (BigDecimal) values[10],
                (BigDecimal) values[11],
                (BigDecimal) values[12],
                (Boolean) values[13],
                (BigDecimal) values[14],
                (RuntimeKlineContextSourceBindingDTO.FreshnessStatus) values[15],
                (RuntimeKlineContextSourceBindingDTO.WickStatus) values[16],
                (RuntimeKlineContextSourceBindingDTO.GapStatus) values[17],
                (RuntimeKlineContextSourceBindingDTO.LiquidityState) values[18],
                (String) values[19],
                (RuntimeKlineContextSourceBindingDTO.StampedeState) values[20],
                (List<String>) values[21],
                (String) values[22],
                (String) values[23],
                (String) values[24],
                (String) values[25],
                (String) values[26],
                (Boolean) values[27],
                (RuntimeKlineContextSourceBindingDTO.BindingStatus) values[28]
        );
    }

    private RuntimeKlineContextSourceBindingAssembler.AssemblyInput input(
            String runtimeKlineContextId,
            String symbol,
            String market,
            String timeframe,
            String klineWindow,
            BigDecimal latestPrice,
            BigDecimal latestClose,
            BigDecimal open,
            BigDecimal high,
            BigDecimal low,
            BigDecimal close,
            BigDecimal volume,
            BigDecimal quoteVolume,
            Boolean candleClosed,
            BigDecimal ohlcvCompleteness,
            RuntimeKlineContextSourceBindingDTO.FreshnessStatus freshnessStatus,
            RuntimeKlineContextSourceBindingDTO.WickStatus wickStatus,
            RuntimeKlineContextSourceBindingDTO.GapStatus gapStatus,
            RuntimeKlineContextSourceBindingDTO.LiquidityState liquidityState,
            String liquiditySeverity,
            RuntimeKlineContextSourceBindingDTO.StampedeState stampedeState,
            List<String> sourceTraceRefs,
            String marketDataSourceRef,
            String observedAt,
            String createdAt,
            String missingReason,
            String blockedReason,
            Boolean trustedSource,
            RuntimeKlineContextSourceBindingDTO.BindingStatus requestedStatus
    ) {
        return RuntimeKlineContextSourceBindingAssembler.AssemblyInput.of(
                runtimeKlineContextId,
                symbol,
                market,
                timeframe,
                klineWindow,
                latestPrice,
                latestClose,
                open,
                high,
                low,
                close,
                volume,
                quoteVolume,
                candleClosed,
                ohlcvCompleteness,
                freshnessStatus,
                wickStatus,
                gapStatus,
                liquidityState,
                liquiditySeverity,
                stampedeState,
                sourceTraceRefs,
                marketDataSourceRef,
                observedAt,
                createdAt,
                missingReason,
                blockedReason,
                trustedSource,
                requestedStatus
        );
    }

    private void assertIncompleteFor(
            RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private void assertBlockedFor(
            RuntimeKlineContextSourceBindingAssembler.AssembledRuntimeKlineContextSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertNoAnnotations(Class<?> target) {
        Annotation[] annotations = target.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenSnippets) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/assembler/point/"
                        + "RuntimeKlineContextSourceBindingAssembler.java"
        ));
        for (String forbiddenSnippet : forbiddenSnippets) {
            assertThat(source).doesNotContain(forbiddenSnippet);
        }
    }

    private static void assertNoForbiddenExecutableSemantics(List<String> outputs) {
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
            String normalizedOutput = output == null ? "" : output.toLowerCase();
            for (String forbiddenWord : forbiddenWords) {
                assertThat(normalizedOutput).doesNotContain(forbiddenWord);
            }
        }
    }

    private static class CountingValidator extends RuntimeKlineContextSourceBindingValidator {
        private int invocationCount;

        @Override
        public ValidationResult validate(RuntimeKlineContextSourceBindingDTO context) {
            invocationCount++;
            return super.validate(context);
        }
    }
}
