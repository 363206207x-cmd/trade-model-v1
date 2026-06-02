package org.example.trademodel.assembler.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.SourceTraceNumericSourceContextDTO;
import org.example.trademodel.validator.point.SourceTraceNumericSourceReadModelValidator;
import org.junit.jupiter.api.Test;

class SourceTraceNumericSourceReadModelAssemblerTest {

    private final SourceTraceNumericSourceReadModelAssembler assembler =
            new SourceTraceNumericSourceReadModelAssembler();

    @Test
    void nullInputCreatesIncompleteContext() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(null);

        assertThat(assembled.getContext().getSourceTraceStatus())
                .isEqualTo(SourceTraceNumericSourceContextDTO.SourceTraceStatus.INCOMPLETE);
        assertThat(assembled.getContext().getMissingReason()).isEqualTo("SOURCE_TRACE_INPUT_MISSING");
    }

    @Test
    void nullInputCreatesIncompleteValidation() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(null);

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("SOURCE_TRACE_INPUT_MISSING");
    }

    @Test
    void incompleteInputWithMissingReasonCreatesIncompleteContext() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(incompleteInput());

        assertThat(assembled.getContext().getSourceTraceStatus())
                .isEqualTo(SourceTraceNumericSourceContextDTO.SourceTraceStatus.INCOMPLETE);
        assertThat(assembled.getContext().getMissingReason()).isEqualTo("SOURCE_TRACE_MISSING");
    }

    @Test
    void incompleteInputWithMissingReasonCreatesIncompleteValidation() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(incompleteInput());

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("SOURCE_TRACE_MISSING");
    }

    @Test
    void blockedInputWithBlockedReasonCreatesBlockedFailClosedValidation() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(blockedInput());

        assertThat(assembled.getContext().getSourceTraceStatus())
                .isEqualTo(SourceTraceNumericSourceContextDTO.SourceTraceStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("SOURCE_TRACE_BLOCKED");
    }

    @Test
    void reviewOnlyInputWithCompleteAllowedSourceCreatesReviewOnlyValidation() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getSourceTraceStatus())
                .isEqualTo(SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.REVIEW_ONLY_SOURCE_TRACE);
        assertThat(assembled.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void degradedInputWithMissingReasonCreatesReviewOnlyDegradedValidation() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(degradedInput());

        assertThat(assembled.getContext().getSourceTraceStatus())
                .isEqualTo(SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE_DEGRADED);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.REVIEW_ONLY_SOURCE_TRACE_DEGRADED);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("SOURCE_TRACE_DEGRADED");
    }

    @Test
    void missingSourceTraceIdCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSourceTraceId(null)), "SOURCE_TRACE_ID_MISSING");
    }

    @Test
    void missingSourceOwnerCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSourceOwner(" ")), "SOURCE_OWNER_MISSING");
    }

    @Test
    void missingSourceTypeCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSourceType(null)), "SOURCE_TYPE_MISSING");
    }

    @Test
    void forbiddenSourceTypeAiProseOnlyCreatesBlockedFailClosedValidation() {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.AI_PROSE_ONLY);
    }

    @Test
    void forbiddenSourceTypeDashboardTextOnlyCreatesBlockedFailClosedValidation() {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.DASHBOARD_TEXT_ONLY);
    }

    @Test
    void forbiddenSourceTypeLatestPriceOnlyCreatesBlockedFailClosedValidation() {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.LATEST_PRICE_ONLY);
    }

    @Test
    void forbiddenSourceTypeOrderBookDirectCreatesBlockedFailClosedValidation() {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.ORDER_BOOK_DIRECT);
    }

    @Test
    void forbiddenSourceTypeOrderExecutionPathCreatesBlockedFailClosedValidation() {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.ORDER_EXECUTION_PATH);
    }

    @Test
    void forbiddenSourceTypeAutoTradingPathCreatesBlockedFailClosedValidation() {
        assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType.AUTO_TRADING_PATH);
    }

    @Test
    void staleFreshnessCreatesIncompleteValidation() {
        assertIncompleteFor(
                assembler.assemble(inputWithFreshness(SourceTraceNumericSourceContextDTO.FreshnessStatus.STALE)),
                "FRESHNESS_STALE"
        );
    }

    @Test
    void unknownFreshnessCreatesIncompleteValidation() {
        assertIncompleteFor(
                assembler.assemble(inputWithFreshness(SourceTraceNumericSourceContextDTO.FreshnessStatus.UNKNOWN)),
                "FRESHNESS_UNKNOWN"
        );
    }

    @Test
    void missingRuntimeKlineContextRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithRuntimeKlineContextRef(null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
    }

    @Test
    void missingDataQualityContextRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithDataQualityContextRef("")),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
    }

    @Test
    void missingMultiTimeframeContextRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithMultiTimeframeContextRef(null)),
                "MULTI_TIMEFRAME_CONTEXT_REF_MISSING");
    }

    @Test
    void missingRiskActionGuardRefCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithRiskActionGuardRef(" ")),
                "RISK_ACTION_GUARD_REF_MISSING");
    }

    @Test
    void entryPriceMissingNumericValueCreatesIncompleteValidation() {
        assertIncompleteFor(
                assembler.assemble(inputWithNumeric(
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                        null,
                        null,
                        null
                )),
                "NUMERIC_VALUE_MISSING"
        );
    }

    @Test
    void sourceOnlyReferenceWithoutNumericValueButWithRefsCreatesReviewOnlyValidation() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(inputWithNumeric(
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.SOURCE_ONLY_REFERENCE,
                        null,
                        null,
                        null
                ));

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.REVIEW_ONLY_SOURCE_TRACE);
        assertThat(assembled.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void explicitBigDecimalNumericValueIsPreserved() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getNumericValue()).isEqualByComparingTo("100.25");
    }

    @Test
    void explicitNumericValueLowAndHighArePreserved() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(inputWithNumeric(
                        SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_ZONE_LOW,
                        null,
                        new BigDecimal("99.50"),
                        new BigDecimal("101.00")
                ));

        assertThat(assembled.getContext().getNumericValueLow()).isEqualByComparingTo("99.50");
        assertThat(assembled.getContext().getNumericValueHigh()).isEqualByComparingTo("101.00");
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.REVIEW_ONLY_SOURCE_TRACE);
    }

    @Test
    void assemblerCallsValidatorAndReturnsValidationResult() {
        CountingValidator countingValidator = new CountingValidator();
        SourceTraceNumericSourceReadModelAssembler countingAssembler =
                new SourceTraceNumericSourceReadModelAssembler(countingValidator);

        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                countingAssembler.assemble(completeInput());

        assertThat(countingValidator.invocationCount).isEqualTo(1);
        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerHandlesNullFieldsWithoutException() {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(inputWithNullFields());

        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.INCOMPLETE);
    }

    @Test
    void assemblerHasNoSpringAnnotations() {
        assertNoAnnotations(SourceTraceNumericSourceReadModelAssembler.class);
        assertNoAnnotations(SourceTraceNumericSourceReadModelAssembler.AssemblyInput.class);
        assertNoAnnotations(SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource.class);
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
    void assemblerSafeOutputDoesNotContainForbiddenExecutableSemantics() {
        List<SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource> assembledResults =
                List.of(
                        assembler.assemble(incompleteInput()),
                        assembler.assemble(blockedInput()),
                        assembler.assemble(degradedInput()),
                        assembler.assemble(completeInput())
                );

        for (SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled
                : assembledResults) {
            List<String> outputs = new ArrayList<>();
            outputs.add(assembled.getContext().getSourceTraceStatus().name());
            outputs.add(assembled.getContext().getMissingReason());
            outputs.add(assembled.getContext().getBlockedReason());
            outputs.add(assembled.getValidationResult().getStatus().name());
            outputs.addAll(assembled.getValidationResult().getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput incompleteInput() {
        return input(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                null,
                null,
                null,
                null,
                null,
                null,
                SourceTraceNumericSourceContextDTO.FreshnessStatus.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                "SOURCE_TRACE_MISSING",
                null,
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.INCOMPLETE
        );
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput blockedInput() {
        return input(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                null,
                null,
                null,
                null,
                null,
                null,
                SourceTraceNumericSourceContextDTO.FreshnessStatus.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "SOURCE_TRACE_BLOCKED",
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.BLOCKED_FAIL_CLOSED
        );
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput degradedInput() {
        return completeInputWithStatus(
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE_DEGRADED,
                "SOURCE_TRACE_DEGRADED",
                null
        );
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput completeInput() {
        return completeInputWithStatus(
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE,
                null,
                null
        );
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput completeInputWithStatus(
            SourceTraceNumericSourceContextDTO.SourceTraceStatus status,
            String missingReason,
            String blockedReason
    ) {
        return input(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                new BigDecimal("100.25"),
                null,
                null,
                "USDT",
                "2026-06-02T07:00:00Z",
                "2026-06-02T07:01:00Z",
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                new BigDecimal("0.82"),
                "source-ref-1",
                "runtime-15m",
                "dq-1",
                "mtf-1",
                "rag-1",
                missingReason,
                blockedReason,
                status
        );
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithSourceTraceId(String sourceTraceId) {
        return inputWith(sourceTraceId, "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m",
                "dq-1", "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithSourceOwner(String sourceOwner) {
        return inputWith("source-trace-1", sourceOwner, SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m",
                "dq-1", "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithSourceType(
            SourceTraceNumericSourceContextDTO.SourceType sourceType
    ) {
        return inputWith("source-trace-1", "review-read-model", sourceType, "contract-1", "BTCUSDT", "SPOT",
                "15m", "entryPrice", SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE,
                new BigDecimal("100.25"), SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                "source-ref-1", "runtime-15m", "dq-1", "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithFreshness(
            SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus
    ) {
        return inputWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                freshnessStatus, "source-ref-1", "runtime-15m", "dq-1", "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithRuntimeKlineContextRef(
            String runtimeKlineContextRef
    ) {
        return inputWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", runtimeKlineContextRef,
                "dq-1", "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithDataQualityContextRef(
            String dataQualityContextRef
    ) {
        return inputWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m",
                dataQualityContextRef, "mtf-1", "rag-1");
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithMultiTimeframeContextRef(
            String multiTimeframeContextRef
    ) {
        return inputWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m",
                "dq-1", multiTimeframeContextRef, "rag-1");
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithRiskActionGuardRef(
            String riskActionGuardRef
    ) {
        return inputWith("source-trace-1", "review-read-model", SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1", "BTCUSDT", "SPOT", "15m", "entryPrice",
                SourceTraceNumericSourceContextDTO.NumericFieldRole.ENTRY_PRICE, new BigDecimal("100.25"),
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH, "source-ref-1", "runtime-15m",
                "dq-1", "mtf-1", riskActionGuardRef);
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithNumeric(
            SourceTraceNumericSourceContextDTO.NumericFieldRole role,
            BigDecimal value,
            BigDecimal valueLow,
            BigDecimal valueHigh
    ) {
        return input(
                "source-trace-1",
                "review-read-model",
                SourceTraceNumericSourceContextDTO.SourceType.STRUCTURE_LEVEL,
                "contract-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "sourceOnly",
                role,
                value,
                valueLow,
                valueHigh,
                "USDT",
                "2026-06-02T07:00:00Z",
                "2026-06-02T07:01:00Z",
                SourceTraceNumericSourceContextDTO.FreshnessStatus.FRESH,
                new BigDecimal("0.82"),
                "source-ref-1",
                "runtime-15m",
                "dq-1",
                "mtf-1",
                "rag-1",
                null,
                null,
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE
        );
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWithNullFields() {
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
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE
        );
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput inputWith(
            String sourceTraceId,
            String sourceOwner,
            SourceTraceNumericSourceContextDTO.SourceType sourceType,
            String sourceContractId,
            String symbol,
            String market,
            String timeframe,
            String numericFieldName,
            SourceTraceNumericSourceContextDTO.NumericFieldRole numericFieldRole,
            BigDecimal numericValue,
            SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus,
            String sourceRef,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef
    ) {
        return input(
                sourceTraceId,
                sourceOwner,
                sourceType,
                sourceContractId,
                symbol,
                market,
                timeframe,
                numericFieldName,
                numericFieldRole,
                numericValue,
                null,
                null,
                "USDT",
                "2026-06-02T07:00:00Z",
                "2026-06-02T07:01:00Z",
                freshnessStatus,
                new BigDecimal("0.82"),
                sourceRef,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                null,
                null,
                SourceTraceNumericSourceContextDTO.SourceTraceStatus.REVIEW_ONLY_SOURCE_TRACE
        );
    }

    private SourceTraceNumericSourceReadModelAssembler.AssemblyInput input(
            String sourceTraceId,
            String sourceOwner,
            SourceTraceNumericSourceContextDTO.SourceType sourceType,
            String sourceContractId,
            String symbol,
            String market,
            String timeframe,
            String numericFieldName,
            SourceTraceNumericSourceContextDTO.NumericFieldRole numericFieldRole,
            BigDecimal numericValue,
            BigDecimal numericValueLow,
            BigDecimal numericValueHigh,
            String sourceUnit,
            String observedAt,
            String createdAt,
            SourceTraceNumericSourceContextDTO.FreshnessStatus freshnessStatus,
            BigDecimal sourceConfidence,
            String sourceRef,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardRef,
            String missingReason,
            String blockedReason,
            SourceTraceNumericSourceContextDTO.SourceTraceStatus requestedStatus
    ) {
        return SourceTraceNumericSourceReadModelAssembler.AssemblyInput.of(
                sourceTraceId,
                sourceOwner,
                sourceType,
                sourceContractId,
                symbol,
                market,
                timeframe,
                numericFieldName,
                numericFieldRole,
                numericValue,
                numericValueLow,
                numericValueHigh,
                sourceUnit,
                observedAt,
                createdAt,
                freshnessStatus,
                sourceConfidence,
                sourceRef,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardRef,
                missingReason,
                blockedReason,
                requestedStatus
        );
    }

    private void assertForbiddenSourceType(SourceTraceNumericSourceContextDTO.SourceType sourceType) {
        SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled =
                assembler.assemble(inputWithSourceType(sourceType));

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("FORBIDDEN_SOURCE_TYPE");
    }

    private void assertIncompleteFor(
            SourceTraceNumericSourceReadModelAssembler.AssembledSourceTraceNumericSource assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceTraceNumericSourceReadModelValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();

        assertThat(annotations).isEmpty();
    }

    private void assertSourceDoesNotContain(List<String> fragments) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/assembler/point/"
                        + "SourceTraceNumericSourceReadModelAssembler.java"
        ));

        for (String fragment : fragments) {
            assertThat(source).doesNotContain(fragment);
        }
    }

    private void assertNoForbiddenExecutableSemantics(List<String> outputs) {
        List<String> forbiddenSemantics = List.of(
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
            for (String forbiddenSemantic : forbiddenSemantics) {
                assertThat(normalizedOutput).doesNotContain(forbiddenSemantic);
            }
        }
    }

    private static class CountingValidator extends SourceTraceNumericSourceReadModelValidator {
        private int invocationCount;

        @Override
        public ValidationResult validate(SourceTraceNumericSourceContextDTO context) {
            invocationCount++;
            return super.validate(context);
        }
    }
}
