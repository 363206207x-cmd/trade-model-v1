package org.example.trademodel.assembler.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.RiskActionGuardSourceBindingDTO;
import org.example.trademodel.validator.point.RiskActionGuardSourceBindingValidator;
import org.junit.jupiter.api.Test;

class RiskActionGuardSourceBindingAssemblerTest {

    private final RiskActionGuardSourceBindingAssembler assembler =
            new RiskActionGuardSourceBindingAssembler();

    @Test
    void nullInputCreatesIncompleteContextAndValidation() {
        RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled =
                assembler.assemble(null);

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(RiskActionGuardSourceBindingDTO.BindingStatus.INCOMPLETE);
        assertThat(assembled.getContext().getMissingReason()).isEqualTo("RISK_ACTION_GUARD_BINDING_INPUT_MISSING");
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RiskActionGuardSourceBindingValidator.ValidationStatus.INCOMPLETE);
    }

    @Test
    void incompleteInputCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(incompleteInput()), "RISK_ACTION_GUARD_BINDING_MISSING");
    }

    @Test
    void blockedInputCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(blockedInput()), "RISK_ACTION_GUARD_BINDING_BLOCKED");
    }

    @Test
    void completeReviewOnlyInputCreatesReviewOnlyValidation() {
        RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(RiskActionGuardSourceBindingDTO.BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        RiskActionGuardSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_RISK_ACTION_GUARD_BINDING
                );
        assertThat(assembled.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void degradedInputCreatesReviewOnlyRiskActionGuardBindingDegradedValidation() {
        RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled =
                assembler.assemble(degradedInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(
                        RiskActionGuardSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
                );
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        RiskActionGuardSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
                );
    }

    @Test
    void missingRequiredRefsCreateIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSourceTraceRefs(List.of())), "SOURCE_TRACE_REFS_MISSING");
        assertIncompleteFor(assembler.assemble(inputWithRuntimeKlineContextRef(null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
        assertIncompleteFor(assembler.assemble(inputWithDataQualityContextRef(null)),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
        assertIncompleteFor(assembler.assemble(inputWithMultiTimeframeContextRef(" ")),
                "MULTITIMEFRAME_CONTEXT_REF_MISSING");
    }

    @Test
    void stampedeDetectedCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithStampedeDetected(Boolean.TRUE)), "STAMPEDE_BLOCKED");
    }

    @Test
    void wickOnlyWithReverseSemanticCreatesBlockedFailClosedValidation() {
        RiskActionGuardSourceBindingAssembler.AssemblyInput input =
                inputWithWickAndProposedLabel(Boolean.TRUE, "reverse bias");

        assertBlockedFor(assembler.assemble(input), "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void strongReversalWithoutMultiTimeframeConfirmationCreatesIncompleteValidation() {
        RiskActionGuardSourceBindingAssembler.AssemblyInput input =
                inputWithStrongReversalAndConfirmation(Boolean.TRUE, Boolean.FALSE);

        assertIncompleteFor(assembler.assemble(input), "STRONG_REVERSAL_UNCONFIRMED");
    }

    @Test
    void liquidityDegradedWithMarketCloseSemanticCreatesBlockedFailClosedValidation() {
        RiskActionGuardSourceBindingAssembler.AssemblyInput input =
                inputWithLiquidityAndLabel(Boolean.TRUE, "market close");

        assertBlockedFor(assembler.assemble(input), "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void highRiskWithoutReasonCreatesIncompleteValidation() {
        RiskActionGuardSourceBindingAssembler.AssemblyInput input =
                inputWithRiskScoreAndLevel(bd("85"), RiskActionGuardSourceBindingDTO.RiskLevel.HIGH);

        assertIncompleteFor(assembler.assemble(input), "WARNING_RISK_REQUIRES_DEGRADED_REASON");
    }

    @Test
    void highRiskWithReasonCreatesDegradedValidation() {
        RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled =
                assembler.assemble(degradedInput());

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        RiskActionGuardSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
                );
    }

    @Test
    void explicitScoresAndLabelsArePreserved() {
        RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getRiskScore()).isEqualByComparingTo("42");
        assertThat(assembled.getContext().getActionRiskScore()).isEqualByComparingTo("35");
        assertThat(assembled.getContext().getProposedActionLabel()).isEqualTo("REVIEW_ONLY_RECHECK");
        assertThat(assembled.getContext().getGuardDecisionLabel()).isEqualTo("MANUAL_REVIEW");
    }

    @Test
    void sourceTraceRefsArePreservedAndDefensivelyCopied() {
        List<String> refs = new ArrayList<>();
        refs.add("source-trace-ref");

        RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled =
                assembler.assemble(inputWithSourceTraceRefs(refs));
        refs.add("mutated-ref");

        assertThat(assembled.getContext().getSourceTraceRefs()).containsExactly("source-trace-ref");
    }

    @Test
    void assemblerCallsValidatorAndReturnsValidationResult() {
        CountingValidator countingValidator = new CountingValidator();
        RiskActionGuardSourceBindingAssembler countingAssembler =
                new RiskActionGuardSourceBindingAssembler(countingValidator);

        RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled =
                countingAssembler.assemble(completeInput());

        assertThat(countingValidator.invocationCount).isEqualTo(1);
        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerHandlesNullFieldsWithoutException() {
        RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled =
                assembler.assemble(inputWithNullFields());

        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult().getStatus())
                .isIn(
                        RiskActionGuardSourceBindingValidator.ValidationStatus.INCOMPLETE,
                        RiskActionGuardSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED
                );
    }

    @Test
    void assemblerClassHasNoSpringAnnotations() {
        assertNoAnnotations(RiskActionGuardSourceBindingAssembler.class);
        assertNoAnnotations(RiskActionGuardSourceBindingAssembler.AssemblyInput.class);
        assertNoAnnotations(RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding.class);
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
                "Binance",
                "OKX",
                "Bybit",
                "market client",
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
    void assemblerDoesNotReferenceExternalPushExecutionOrAutoTradingClasses() throws Exception {
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
                "reversePosition",
                "openPosition",
                "submitOrder"
        ));
    }

    @Test
    void assemblerSafeOutputDoesNotContainForbiddenExecutableSemantics() {
        List<RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding> results = List.of(
                assembler.assemble(incompleteInput()),
                assembler.assemble(blockedInput()),
                assembler.assemble(degradedInput()),
                assembler.assemble(completeInput())
        );

        for (RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(assembled.getContext().getBindingStatus().name());
            outputs.add(assembled.getContext().getMissingReason());
            outputs.add(assembled.getContext().getBlockedReason());
            outputs.add(assembled.getValidationResult().getStatus().name());
            outputs.addAll(assembled.getValidationResult().getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput incompleteInput() {
        return input(
                "rag-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
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
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                List.of("riskScore"),
                List.of(),
                List.of(),
                null,
                null,
                "RISK_ACTION_GUARD_BINDING_MISSING",
                null,
                Boolean.TRUE,
                RiskActionGuardSourceBindingDTO.BindingStatus.INCOMPLETE
        );
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput blockedInput() {
        return input(
                "rag-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
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
                List.of(),
                List.of("ACTION_BLOCKED"),
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of("STAMPEDE_BLOCKED"),
                null,
                null,
                null,
                "RISK_ACTION_GUARD_BINDING_BLOCKED",
                Boolean.FALSE,
                RiskActionGuardSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED
        );
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput degradedInput() {
        return input(
                "rag-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                RiskActionGuardSourceBindingDTO.LiquidityState.NORMAL,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                RiskActionGuardSourceBindingDTO.RiskLevel.HIGH,
                bd("85"),
                bd("72"),
                "REVIEW_ONLY_RISK_DOWNGRADE",
                List.of("REVIEW_ONLY_RECHECK"),
                List.of(),
                "MANUAL_REVIEW",
                "HIGH_RISK_REVIEW_ONLY",
                "RISK_REVIEW",
                "risk-boundary-ref",
                List.of(),
                List.of("HIGH_RISK_REVIEW_ONLY"),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "RISK_ACTION_GUARD_BINDING_DEGRADED",
                null,
                Boolean.TRUE,
                RiskActionGuardSourceBindingDTO.BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
        );
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput completeInput() {
        return input(
                "rag-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                RiskActionGuardSourceBindingDTO.LiquidityState.NORMAL,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                RiskActionGuardSourceBindingDTO.RiskLevel.MEDIUM,
                bd("42"),
                bd("35"),
                "REVIEW_ONLY_RECHECK",
                List.of("REVIEW_ONLY_RECHECK"),
                List.of(),
                "MANUAL_REVIEW",
                "RISK_REVIEW_ONLY",
                "RISK_REVIEW",
                "risk-boundary-ref",
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                RiskActionGuardSourceBindingDTO.BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING
        );
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithSourceTraceRefs(List<String> value) {
        return inputWith(4, value);
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithRuntimeKlineContextRef(String value) {
        return inputWith(5, value);
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithDataQualityContextRef(String value) {
        return inputWith(6, value);
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithMultiTimeframeContextRef(String value) {
        return inputWith(7, value);
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithStampedeDetected(Boolean value) {
        return inputWith(10, value);
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithWickAndProposedLabel(
            Boolean wickOnly,
            String proposedActionLabel
    ) {
        Object[] values = completeValues();
        values[11] = wickOnly;
        values[17] = proposedActionLabel;
        return inputFrom(values);
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithStrongReversalAndConfirmation(
            Boolean strongReversalClaimed,
            Boolean multiTimeframeConfirmed
    ) {
        Object[] values = completeValues();
        values[12] = multiTimeframeConfirmed;
        values[13] = strongReversalClaimed;
        return inputFrom(values);
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithLiquidityAndLabel(
            Boolean liquidityDegraded,
            String proposedActionLabel
    ) {
        Object[] values = completeValues();
        values[9] = liquidityDegraded;
        values[17] = proposedActionLabel;
        return inputFrom(values);
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithRiskScoreAndLevel(
            BigDecimal riskScore,
            RiskActionGuardSourceBindingDTO.RiskLevel riskLevel
    ) {
        Object[] values = completeValues();
        values[14] = riskLevel;
        values[15] = riskScore;
        return inputFrom(values);
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWithNullFields() {
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
                null,
                RiskActionGuardSourceBindingDTO.BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING
        );
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputWith(int index, Object value) {
        Object[] values = completeValues();
        values[index] = value;
        return inputFrom(values);
    }

    private Object[] completeValues() {
        return new Object[] {
                "rag-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                RiskActionGuardSourceBindingDTO.LiquidityState.NORMAL,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                RiskActionGuardSourceBindingDTO.RiskLevel.MEDIUM,
                bd("42"),
                bd("35"),
                "REVIEW_ONLY_RECHECK",
                List.of("REVIEW_ONLY_RECHECK"),
                List.of(),
                "MANUAL_REVIEW",
                "RISK_REVIEW_ONLY",
                "RISK_REVIEW",
                "risk-boundary-ref",
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                RiskActionGuardSourceBindingDTO.BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING
        };
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput inputFrom(Object[] values) {
        return input(
                (String) values[0],
                (String) values[1],
                (String) values[2],
                (String) values[3],
                castStringList(values[4]),
                (String) values[5],
                (String) values[6],
                (String) values[7],
                (RiskActionGuardSourceBindingDTO.LiquidityState) values[8],
                (Boolean) values[9],
                (Boolean) values[10],
                (Boolean) values[11],
                (Boolean) values[12],
                (Boolean) values[13],
                (RiskActionGuardSourceBindingDTO.RiskLevel) values[14],
                (BigDecimal) values[15],
                (BigDecimal) values[16],
                (String) values[17],
                castStringList(values[18]),
                castStringList(values[19]),
                (String) values[20],
                (String) values[21],
                (String) values[22],
                (String) values[23],
                castStringList(values[24]),
                castStringList(values[25]),
                castStringList(values[26]),
                (String) values[27],
                (String) values[28],
                (String) values[29],
                (String) values[30],
                (Boolean) values[31],
                (RiskActionGuardSourceBindingDTO.BindingStatus) values[32]
        );
    }

    private RiskActionGuardSourceBindingAssembler.AssemblyInput input(
            String riskActionGuardContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            RiskActionGuardSourceBindingDTO.LiquidityState liquidityState,
            Boolean liquidityDegraded,
            Boolean stampedeDetected,
            Boolean wickOnlyDetected,
            Boolean multiTimeframeConfirmed,
            Boolean strongReversalClaimed,
            RiskActionGuardSourceBindingDTO.RiskLevel riskLevel,
            BigDecimal riskScore,
            BigDecimal actionRiskScore,
            String proposedActionLabel,
            List<String> allowedReviewOnlyActionLabels,
            List<String> blockedActionLabels,
            String guardDecisionLabel,
            String guardReason,
            String riskActionCategory,
            String riskActionBoundaryRef,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            String blockedReason,
            Boolean trustedSource,
            RiskActionGuardSourceBindingDTO.BindingStatus requestedStatus
    ) {
        return RiskActionGuardSourceBindingAssembler.AssemblyInput.of(
                riskActionGuardContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                liquidityState,
                liquidityDegraded,
                stampedeDetected,
                wickOnlyDetected,
                multiTimeframeConfirmed,
                strongReversalClaimed,
                riskLevel,
                riskScore,
                actionRiskScore,
                proposedActionLabel,
                allowedReviewOnlyActionLabels,
                blockedActionLabels,
                guardDecisionLabel,
                guardReason,
                riskActionCategory,
                riskActionBoundaryRef,
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

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertIncompleteFor(
            RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RiskActionGuardSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private static void assertBlockedFor(
            RiskActionGuardSourceBindingAssembler.AssembledRiskActionGuardSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(RiskActionGuardSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/assembler/point/RiskActionGuardSourceBindingAssembler.java"
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

    private static class CountingValidator extends RiskActionGuardSourceBindingValidator {
        private int invocationCount;

        @Override
        public ValidationResult validate(RiskActionGuardSourceBindingDTO context) {
            invocationCount++;
            return super.validate(context);
        }
    }
}
