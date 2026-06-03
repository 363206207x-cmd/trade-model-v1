package org.example.trademodel.assembler.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.DataQualityContextSourceBindingDTO;
import org.example.trademodel.validator.point.DataQualityContextSourceBindingValidator;
import org.junit.jupiter.api.Test;

class DataQualityContextSourceBindingAssemblerTest {

    private final DataQualityContextSourceBindingAssembler assembler =
            new DataQualityContextSourceBindingAssembler();

    @Test
    void nullInputCreatesIncompleteContextAndValidation() {
        DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding assembled =
                assembler.assemble(null);

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(DataQualityContextSourceBindingDTO.BindingStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(DataQualityContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("DATA_QUALITY_BINDING_INPUT_MISSING");
    }

    @Test
    void incompleteInputCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(incompleteInput()), "DATA_QUALITY_MISSING");
    }

    @Test
    void blockedInputCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(blockedInput()), "DATA_QUALITY_BLOCKED");
    }

    @Test
    void completeHighQualityInputCreatesReviewOnlyValidation() {
        DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(DataQualityContextSourceBindingValidator.ValidationStatus.REVIEW_ONLY_DATA_QUALITY_BINDING);
        assertThat(assembled.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void degradedInputCreatesReviewOnlyDataQualityBindingDegradedValidation() {
        DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding assembled =
                assembler.assemble(degradedInput());

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        DataQualityContextSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED
                );
    }

    @Test
    void lowScoreInputCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithScore(bd("69"))), "DATA_QUALITY_SCORE_LOW");
    }

    @Test
    void hardThresholdFalseCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithHardThreshold(Boolean.FALSE)), "HARD_THRESHOLD_BLOCKED");
    }

    @Test
    void sourceTraceRefsArePreservedAndDefensivelyCopied() {
        List<String> refs = new ArrayList<>();
        refs.add("source-trace-ref");

        DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding assembled =
                assembler.assemble(inputWithSourceTraceRefs(refs));
        refs.add("mutated-ref");

        assertThat(assembled.getContext().getSourceTraceRefs()).containsExactly("source-trace-ref");
    }

    @Test
    void assemblerCallsValidatorAndReturnsValidationResult() {
        CountingValidator countingValidator = new CountingValidator();
        DataQualityContextSourceBindingAssembler countingAssembler =
                new DataQualityContextSourceBindingAssembler(countingValidator);

        DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding assembled =
                countingAssembler.assemble(completeInput());

        assertThat(countingValidator.invocationCount).isEqualTo(1);
        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerHandlesNullFieldsWithoutException() {
        DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding assembled =
                assembler.assemble(inputWithNullFields());

        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(DataQualityContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
    }

    @Test
    void assemblerClassHasNoSpringAnnotations() {
        assertNoAnnotations(DataQualityContextSourceBindingAssembler.class);
        assertNoAnnotations(DataQualityContextSourceBindingAssembler.AssemblyInput.class);
        assertNoAnnotations(DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding.class);
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
        List<DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding> results = List.of(
                assembler.assemble(incompleteInput()),
                assembler.assemble(blockedInput()),
                assembler.assemble(degradedInput()),
                assembler.assemble(completeInput())
        );

        for (DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding assembled : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(assembled.getContext().getBindingStatus().name());
            outputs.add(assembled.getContext().getMissingReason());
            outputs.add(assembled.getContext().getBlockedReason());
            outputs.add(assembled.getValidationResult().getStatus().name());
            outputs.addAll(assembled.getValidationResult().getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    private DataQualityContextSourceBindingAssembler.AssemblyInput incompleteInput() {
        return input(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                null,
                DataQualityContextSourceBindingDTO.DataQualityGrade.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of("dataQualityScore"),
                List.of(),
                List.of(),
                null,
                null,
                "DATA_QUALITY_MISSING",
                null,
                Boolean.TRUE,
                DataQualityContextSourceBindingDTO.BindingStatus.INCOMPLETE
        );
    }

    private DataQualityContextSourceBindingAssembler.AssemblyInput blockedInput() {
        return input(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                null,
                DataQualityContextSourceBindingDTO.DataQualityGrade.UNKNOWN,
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
                "DATA_QUALITY_BLOCKED",
                Boolean.FALSE,
                DataQualityContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED
        );
    }

    private DataQualityContextSourceBindingAssembler.AssemblyInput degradedInput() {
        return input(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                bd("80"),
                DataQualityContextSourceBindingDTO.DataQualityGrade.MEDIUM,
                Boolean.TRUE,
                Boolean.FALSE,
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                List.of(),
                List.of("QUALITY_DEGRADED"),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "DATA_QUALITY_DEGRADED",
                null,
                Boolean.TRUE,
                DataQualityContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED
        );
    }

    private DataQualityContextSourceBindingAssembler.AssemblyInput completeInput() {
        return input(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                bd("92"),
                DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH,
                Boolean.TRUE,
                Boolean.TRUE,
                bd("91"),
                bd("93"),
                bd("94"),
                bd("95"),
                bd("90"),
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                DataQualityContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING
        );
    }

    private DataQualityContextSourceBindingAssembler.AssemblyInput inputWithScore(BigDecimal score) {
        Object[] values = completeValues();
        values[6] = score;
        return inputFrom(values);
    }

    private DataQualityContextSourceBindingAssembler.AssemblyInput inputWithHardThreshold(Boolean value) {
        Object[] values = completeValues();
        values[8] = value;
        return inputFrom(values);
    }

    private DataQualityContextSourceBindingAssembler.AssemblyInput inputWithSourceTraceRefs(List<String> refs) {
        Object[] values = completeValues();
        values[4] = refs;
        return inputFrom(values);
    }

    private DataQualityContextSourceBindingAssembler.AssemblyInput inputWithNullFields() {
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
                Boolean.TRUE,
                DataQualityContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING
        );
    }

    private Object[] completeValues() {
        return new Object[] {
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                bd("92"),
                DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH,
                Boolean.TRUE,
                Boolean.TRUE,
                bd("91"),
                bd("93"),
                bd("94"),
                bd("95"),
                bd("90"),
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                DataQualityContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_DATA_QUALITY_BINDING
        };
    }

    @SuppressWarnings("unchecked")
    private DataQualityContextSourceBindingAssembler.AssemblyInput inputFrom(Object[] values) {
        return input(
                (String) values[0],
                (String) values[1],
                (String) values[2],
                (String) values[3],
                (List<String>) values[4],
                (String) values[5],
                (BigDecimal) values[6],
                (DataQualityContextSourceBindingDTO.DataQualityGrade) values[7],
                (Boolean) values[8],
                (Boolean) values[9],
                (BigDecimal) values[10],
                (BigDecimal) values[11],
                (BigDecimal) values[12],
                (BigDecimal) values[13],
                (BigDecimal) values[14],
                (List<String>) values[15],
                (List<String>) values[16],
                (List<String>) values[17],
                (String) values[18],
                (String) values[19],
                (String) values[20],
                (String) values[21],
                (Boolean) values[22],
                (DataQualityContextSourceBindingDTO.BindingStatus) values[23]
        );
    }

    private DataQualityContextSourceBindingAssembler.AssemblyInput input(
            String dataQualityContextId,
            String symbol,
            String market,
            String timeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            BigDecimal dataQualityScore,
            DataQualityContextSourceBindingDTO.DataQualityGrade dataQualityGrade,
            Boolean hardThresholdPassed,
            Boolean warningThresholdPassed,
            BigDecimal sourceTraceCompletenessScore,
            BigDecimal runtimeKlineCompletenessScore,
            BigDecimal ohlcvCompletenessScore,
            BigDecimal freshnessScore,
            BigDecimal multiTimeframeConsistencyScore,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            String blockedReason,
            Boolean trustedSource,
            DataQualityContextSourceBindingDTO.BindingStatus requestedStatus
    ) {
        return DataQualityContextSourceBindingAssembler.AssemblyInput.of(
                dataQualityContextId,
                symbol,
                market,
                timeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityScore,
                dataQualityGrade,
                hardThresholdPassed,
                warningThresholdPassed,
                sourceTraceCompletenessScore,
                runtimeKlineCompletenessScore,
                ohlcvCompletenessScore,
                freshnessScore,
                multiTimeframeConsistencyScore,
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

    private static void assertIncompleteFor(
            DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(DataQualityContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private static void assertBlockedFor(
            DataQualityContextSourceBindingAssembler.AssembledDataQualityContextSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(DataQualityContextSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private static void assertNoForbiddenExecutableSemantics(List<String> outputs) {
        List<String> forbidden = List.of("buy", "sell", "long", "short", "order", "execute", "auto-trade");
        for (String output : outputs) {
            String normalized = output == null ? "" : output.toLowerCase();
            for (String forbiddenWord : forbidden) {
                assertThat(normalized).doesNotContain(forbiddenWord);
            }
        }
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/assembler/point/DataQualityContextSourceBindingAssembler.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }

    private static class CountingValidator extends DataQualityContextSourceBindingValidator {
        private int invocationCount;

        @Override
        public ValidationResult validate(DataQualityContextSourceBindingDTO context) {
            invocationCount++;
            return super.validate(context);
        }
    }
}
