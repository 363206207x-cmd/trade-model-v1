package org.example.trademodel.assembler.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.MultiTimeframeContextSourceBindingDTO;
import org.example.trademodel.validator.point.MultiTimeframeContextSourceBindingValidator;
import org.junit.jupiter.api.Test;

class MultiTimeframeContextSourceBindingAssemblerTest {

    private final MultiTimeframeContextSourceBindingAssembler assembler =
            new MultiTimeframeContextSourceBindingAssembler();

    @Test
    void nullInputCreatesIncompleteContextAndValidation() {
        MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled =
                assembler.assemble(null);

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(MultiTimeframeContextSourceBindingDTO.BindingStatus.INCOMPLETE);
        assertThat(assembled.getContext().getMissingReason()).isEqualTo("MULTITIMEFRAME_BINDING_INPUT_MISSING");
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(MultiTimeframeContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
    }

    @Test
    void incompleteInputCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(incompleteInput()), "MULTITIMEFRAME_BINDING_MISSING");
    }

    @Test
    void blockedInputCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(blockedInput()), "MULTITIMEFRAME_BINDING_BLOCKED");
    }

    @Test
    void completeReviewOnlyInputCreatesReviewOnlyValidation() {
        MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(MultiTimeframeContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        MultiTimeframeContextSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_MULTITIMEFRAME_BINDING
                );
        assertThat(assembled.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void degradedInputCreatesReviewOnlyMultiTimeframeBindingDegradedValidation() {
        MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled =
                assembler.assemble(degradedInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(
                        MultiTimeframeContextSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
                );
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        MultiTimeframeContextSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
                );
    }

    @Test
    void missingMultiTimeframeContextIdCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithMultiTimeframeContextId(null)),
                "MULTITIMEFRAME_CONTEXT_ID_MISSING");
    }

    @Test
    void missingSymbolCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSymbol(" ")), "SYMBOL_MISSING");
    }

    @Test
    void missingPrimaryTimeframeCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithPrimaryTimeframe(null)),
                "PRIMARY_TIMEFRAME_MISSING");
    }

    @Test
    void missingSourceTraceRefsCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSourceTraceRefs(List.of())), "SOURCE_TRACE_REFS_MISSING");
    }

    @Test
    void blankSourceTraceRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSourceTraceRefs(List.of("source-ref", " "))),
                "SOURCE_TRACE_REF_BLANK");
    }

    @Test
    void missingRuntimeKlineContextRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithRuntimeKlineContextRef(null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
    }

    @Test
    void missingDataQualityContextRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithDataQualityContextRef(" ")),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
    }

    @Test
    void missingTimeframeRefsCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithTimeframeRefs(List.of())), "TIMEFRAME_REFS_MISSING");
    }

    @Test
    void lowAlignmentScoreCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithAlignmentScore(bd("69"))), "ALIGNMENT_SCORE_LOW");
    }

    @Test
    void highConflictScoreWithoutReasonCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithConflictScore(bd("60"))), "MISSING_REASON_REQUIRED");
    }

    @Test
    void minimumRequiredTimeframesFalseCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithMinimumRequiredTimeframesPassed(Boolean.FALSE)),
                "MINIMUM_TIMEFRAMES_NOT_PASSED");
    }

    @Test
    void hardThresholdFalseCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithHardThresholdPassed(Boolean.FALSE)),
                "HARD_THRESHOLD_BLOCKED");
    }

    @Test
    void staleTimeframesWithoutReasonCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithStaleTimeframes(List.of("1h"))),
                "STALE_TIMEFRAMES_PRESENT");
    }

    @Test
    void forbiddenExecutableSemanticInSourceTraceRefsCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithSourceTraceRefs(List.of("send order ref"))),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void forbiddenExecutableSemanticInRuntimeKlineRefCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithRuntimeKlineContextRef("execute-feed-ref")),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void explicitScoresAndTimeframeFieldsArePreserved() {
        MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getAlignmentScore()).isEqualByComparingTo("91");
        assertThat(assembled.getContext().getConflictScore()).isEqualByComparingTo("12");
        assertThat(assembled.getContext().getWeightedAgreementScore()).isEqualByComparingTo("89");
        assertThat(assembled.getContext().getTimeframeScores()).containsExactly(bd("88"), bd("92"));
        assertThat(assembled.getContext().getTimeframeWeights()).containsExactly(bd("0.55"), bd("0.45"));
    }

    @Test
    void explicitRefsArePreservedAndDefensivelyCopied() {
        List<String> sourceTraceRefs = new ArrayList<>();
        sourceTraceRefs.add("source-trace-ref");
        List<String> timeframeRefs = new ArrayList<>();
        timeframeRefs.add("15m");

        MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled =
                assembler.assemble(inputWithRefs(sourceTraceRefs, timeframeRefs));
        sourceTraceRefs.add("mutated-source");
        timeframeRefs.add("1h");

        assertThat(assembled.getContext().getSourceTraceRefs()).containsExactly("source-trace-ref");
        assertThat(assembled.getContext().getTimeframeRefs()).containsExactly("15m");
    }

    @Test
    void assemblerCallsValidatorAndReturnsValidationResult() {
        CountingValidator countingValidator = new CountingValidator();
        MultiTimeframeContextSourceBindingAssembler countingAssembler =
                new MultiTimeframeContextSourceBindingAssembler(countingValidator);

        MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled =
                countingAssembler.assemble(completeInput());

        assertThat(countingValidator.invocationCount).isEqualTo(1);
        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerHandlesNullFieldsWithoutException() {
        MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled =
                assembler.assemble(inputWithNullFields());

        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult().getStatus())
                .isIn(
                        MultiTimeframeContextSourceBindingValidator.ValidationStatus.INCOMPLETE,
                        MultiTimeframeContextSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED
                );
    }

    @Test
    void assemblerClassHasNoSpringAnnotations() {
        assertNoAnnotations(MultiTimeframeContextSourceBindingAssembler.class);
        assertNoAnnotations(MultiTimeframeContextSourceBindingAssembler.AssemblyInput.class);
        assertNoAnnotations(
                MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding.class
        );
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
                "AutoTrading",
                "placeOrder",
                "createOrder",
                "closePosition",
                "reversePosition"
        ));
    }

    @Test
    void assemblerSafeOutputDoesNotContainForbiddenExecutableSemantics() {
        List<MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding> results = List.of(
                assembler.assemble(incompleteInput()),
                assembler.assemble(blockedInput()),
                assembler.assemble(degradedInput()),
                assembler.assemble(completeInput())
        );

        for (MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled
                : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(assembled.getContext().getBindingStatus().name());
            outputs.add(assembled.getContext().getMissingReason());
            outputs.add(assembled.getContext().getBlockedReason());
            outputs.add(assembled.getValidationResult().getStatus().name());
            outputs.addAll(assembled.getValidationResult().getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput incompleteInput() {
        return input(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of(),
                List.of(),
                List.of(),
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
                null,
                null,
                List.of("alignmentScore"),
                List.of(),
                List.of(),
                null,
                null,
                "MULTITIMEFRAME_BINDING_MISSING",
                null,
                Boolean.TRUE,
                MultiTimeframeContextSourceBindingDTO.BindingStatus.INCOMPLETE
        );
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput blockedInput() {
        return input(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of(),
                List.of(),
                List.of(),
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
                null,
                null,
                List.of(),
                List.of(),
                List.of("HARD_THRESHOLD_BLOCKED"),
                null,
                null,
                null,
                "MULTITIMEFRAME_BINDING_BLOCKED",
                Boolean.FALSE,
                MultiTimeframeContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED
        );
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput degradedInput() {
        return inputWithWarningThreshold(Boolean.FALSE,
                MultiTimeframeContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput completeInput() {
        return input(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of(bd("88"), bd("92")),
                List.of("UP", "UP"),
                List.of(bd("0.55"), bd("0.45")),
                List.of("15m", "1h"),
                List.of(),
                List.of(),
                List.of(),
                "UP",
                bd("91"),
                bd("12"),
                bd("89"),
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                MultiTimeframeContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING
        );
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithMultiTimeframeContextId(String value) {
        return inputWith(0, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithSymbol(String value) {
        return inputWith(1, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithPrimaryTimeframe(String value) {
        return inputWith(3, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithSourceTraceRefs(List<String> value) {
        return inputWith(4, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithRuntimeKlineContextRef(String value) {
        return inputWith(5, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithDataQualityContextRef(String value) {
        return inputWith(6, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithTimeframeRefs(List<String> value) {
        return inputWith(7, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithAlignmentScore(BigDecimal value) {
        return inputWith(16, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithConflictScore(BigDecimal value) {
        return inputWith(17, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithMinimumRequiredTimeframesPassed(
            Boolean value
    ) {
        return inputWith(19, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithHardThresholdPassed(Boolean value) {
        return inputWith(21, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithStaleTimeframes(List<String> value) {
        return inputWith(14, value);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithRefs(
            List<String> sourceTraceRefs,
            List<String> timeframeRefs
    ) {
        Object[] values = completeValues();
        values[4] = sourceTraceRefs;
        values[7] = timeframeRefs;
        return inputFrom(values);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithWarningThreshold(
            Boolean warningThresholdPassed,
            MultiTimeframeContextSourceBindingDTO.BindingStatus status
    ) {
        Object[] values = completeValues();
        values[22] = warningThresholdPassed;
        values[24] = List.of("WARNING_THRESHOLD_DEGRADED");
        values[28] = "MULTITIMEFRAME_BINDING_DEGRADED";
        values[31] = status;
        return inputFrom(values);
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWithNullFields() {
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
                null,
                null,
                null,
                MultiTimeframeContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING
        );
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputWith(int index, Object value) {
        Object[] values = completeValues();
        values[index] = value;
        return inputFrom(values);
    }

    private Object[] completeValues() {
        return new Object[] {
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of(bd("88"), bd("92")),
                List.of("UP", "UP"),
                List.of(bd("0.55"), bd("0.45")),
                List.of("15m", "1h"),
                List.of(),
                List.of(),
                List.of(),
                "UP",
                bd("91"),
                bd("12"),
                bd("89"),
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                MultiTimeframeContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING
        };
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput inputFrom(Object[] values) {
        return input(
                (String) values[0],
                (String) values[1],
                (String) values[2],
                (String) values[3],
                castStringList(values[4]),
                (String) values[5],
                (String) values[6],
                castStringList(values[7]),
                castBigDecimalList(values[8]),
                castStringList(values[9]),
                castBigDecimalList(values[10]),
                castStringList(values[11]),
                castStringList(values[12]),
                castStringList(values[13]),
                castStringList(values[14]),
                (String) values[15],
                (BigDecimal) values[16],
                (BigDecimal) values[17],
                (BigDecimal) values[18],
                (Boolean) values[19],
                (Boolean) values[20],
                (Boolean) values[21],
                (Boolean) values[22],
                castStringList(values[23]),
                castStringList(values[24]),
                castStringList(values[25]),
                (String) values[26],
                (String) values[27],
                (String) values[28],
                (String) values[29],
                (Boolean) values[30],
                (MultiTimeframeContextSourceBindingDTO.BindingStatus) values[31]
        );
    }

    private MultiTimeframeContextSourceBindingAssembler.AssemblyInput input(
            String multiTimeframeContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            List<String> timeframeRefs,
            List<BigDecimal> timeframeScores,
            List<String> timeframeDirections,
            List<BigDecimal> timeframeWeights,
            List<String> alignedTimeframes,
            List<String> conflictedTimeframes,
            List<String> missingTimeframes,
            List<String> staleTimeframes,
            String dominantDirection,
            BigDecimal alignmentScore,
            BigDecimal conflictScore,
            BigDecimal weightedAgreementScore,
            Boolean minimumRequiredTimeframesPassed,
            Boolean dataQualityPassed,
            Boolean hardThresholdPassed,
            Boolean warningThresholdPassed,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            String blockedReason,
            Boolean trustedSource,
            MultiTimeframeContextSourceBindingDTO.BindingStatus requestedStatus
    ) {
        return MultiTimeframeContextSourceBindingAssembler.AssemblyInput.of(
                multiTimeframeContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                timeframeRefs,
                timeframeScores,
                timeframeDirections,
                timeframeWeights,
                alignedTimeframes,
                conflictedTimeframes,
                missingTimeframes,
                staleTimeframes,
                dominantDirection,
                alignmentScore,
                conflictScore,
                weightedAgreementScore,
                minimumRequiredTimeframesPassed,
                dataQualityPassed,
                hardThresholdPassed,
                warningThresholdPassed,
                missingFields,
                degradedReasons,
                blockedReasons,
                observedAt,
                createdAt,
                missingReason,
                blockedReason,
                trustedSource,
                requestedStatus
        );
    }

    @SuppressWarnings("unchecked")
    private List<String> castStringList(Object value) {
        return (List<String>) value;
    }

    @SuppressWarnings("unchecked")
    private List<BigDecimal> castBigDecimalList(Object value) {
        return (List<BigDecimal>) value;
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertIncompleteFor(
            MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(MultiTimeframeContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private static void assertBlockedFor(
            MultiTimeframeContextSourceBindingAssembler.AssembledMultiTimeframeContextSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(MultiTimeframeContextSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/assembler/point/"
                        + "MultiTimeframeContextSourceBindingAssembler.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }

    private static void assertNoForbiddenExecutableSemantics(List<String> outputs) {
        List<String> forbidden = List.of(
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
        String joined = String.join(" ", outputs).toLowerCase();
        for (String forbiddenToken : forbidden) {
            assertThat(joined).doesNotContain(forbiddenToken);
        }
    }

    private static class CountingValidator extends MultiTimeframeContextSourceBindingValidator {
        private int invocationCount;

        @Override
        public ValidationResult validate(MultiTimeframeContextSourceBindingDTO context) {
            invocationCount++;
            return super.validate(context);
        }
    }
}
