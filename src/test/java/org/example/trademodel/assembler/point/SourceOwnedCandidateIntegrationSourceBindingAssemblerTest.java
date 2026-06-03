package org.example.trademodel.assembler.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.SourceOwnedCandidateIntegrationSourceBindingDTO;
import org.example.trademodel.validator.point.SourceOwnedCandidateIntegrationSourceBindingValidator;
import org.junit.jupiter.api.Test;

class SourceOwnedCandidateIntegrationSourceBindingAssemblerTest {

    private final SourceOwnedCandidateIntegrationSourceBindingAssembler assembler =
            new SourceOwnedCandidateIntegrationSourceBindingAssembler();

    @Test
    void nullInputCreatesIncompleteContextAndValidation() {
        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled = assembler.assemble(null);

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.INCOMPLETE);
        assertThat(assembled.getContext().getMissingReason())
                .isEqualTo("SOURCE_OWNED_CANDIDATE_INTEGRATION_INPUT_MISSING");
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons())
                .containsExactly("SOURCE_OWNED_CANDIDATE_INTEGRATION_INPUT_MISSING");
    }

    @Test
    void incompleteInputCreatesIncompleteValidation() {
        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                assembler.assemble(incompleteInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons())
                .containsExactly("CANDIDATE_BINDING_INPUT_MISSING");
    }

    @Test
    void blockedInputCreatesBlockedFailClosedValidation() {
        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                assembler.assemble(blockedInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getContext().isFailClosed()).isTrue();
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED
                );
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("CANDIDATE_BINDING_BLOCKED");
    }

    @Test
    void degradedInputCreatesReviewOnlyDegradedValidation() {
        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                assembler.assemble(degradedInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(
                        SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED
                );
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED
                );
        assertThat(assembled.getValidationResult().getReasons()).containsExactly("CANDIDATE_BINDING_DEGRADED");
    }

    @Test
    void completeReviewOnlyInputCreatesReviewOnlyValidation() {
        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(
                        SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING
                );
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING
                );
        assertThat(assembled.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void unsupportedStatusFallsBackToIncomplete() {
        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                assembler.assemble(inputWithRequestedStatus(null));

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.INCOMPLETE);
        assertThat(assembled.getContext().getMissingReason())
                .isEqualTo("UNSUPPORTED_SOURCE_OWNED_CANDIDATE_INTEGRATION_STATUS");
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus.INCOMPLETE);
    }

    @Test
    void assemblerCallsValidatorAndReturnsValidationResult() {
        CountingValidator countingValidator = new CountingValidator();
        SourceOwnedCandidateIntegrationSourceBindingAssembler countingAssembler =
                new SourceOwnedCandidateIntegrationSourceBindingAssembler(countingValidator);

        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                countingAssembler.assemble(completeInput());

        assertThat(countingValidator.invocationCount).isEqualTo(1);
        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerDoesNotModifyDtoAfterValidation() {
        OverridingValidator overridingValidator = new OverridingValidator();
        SourceOwnedCandidateIntegrationSourceBindingAssembler checkedAssembler =
                new SourceOwnedCandidateIntegrationSourceBindingAssembler(overridingValidator);

        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                checkedAssembler.assemble(completeInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(
                        SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING
                );
        assertThat(assembled.getContext().getBlockedReason()).isNull();
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED
                );
    }

    @Test
    void explicitRefsAndStatusesArePreserved() {
        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getSourceTraceRefs()).containsExactly("source-trace-ref");
        assertThat(assembled.getContext().getRuntimeKlineContextRef()).isEqualTo("runtime-kline-ref");
        assertThat(assembled.getContext().getDataQualityContextRef()).isEqualTo("data-quality-ref");
        assertThat(assembled.getContext().getMultiTimeframeContextRef()).isEqualTo("multi-timeframe-ref");
        assertThat(assembled.getContext().getRiskActionGuardContextRef()).isEqualTo("risk-action-guard-ref");
        assertThat(assembled.getContext().getWatchlistPoolProofContextRef()).isEqualTo("watchlist-pool-proof-ref");
        assertThat(assembled.getContext().getSourceTraceStatus()).isEqualTo("REVIEW_ONLY_SOURCE_TRACE");
        assertThat(assembled.getContext().getRuntimeKlineStatus()).isEqualTo("REVIEW_ONLY_RUNTIME_KLINE");
        assertThat(assembled.getContext().getDataQualityStatus()).isEqualTo("REVIEW_ONLY_DATA_QUALITY");
        assertThat(assembled.getContext().getMultiTimeframeStatus()).isEqualTo("REVIEW_ONLY_MULTITIMEFRAME");
        assertThat(assembled.getContext().getRiskActionGuardStatus()).isEqualTo("REVIEW_ONLY_RISK_GUARD");
        assertThat(assembled.getContext().getWatchlistPoolProofStatus()).isEqualTo("REVIEW_ONLY_WATCHLIST_POOL");
    }

    @Test
    void explicitLabelsAndReasonsArePreserved() {
        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getCandidateBoundaryLabel()).isEqualTo("REVIEW_ONLY_BOUNDARY");
        assertThat(assembled.getContext().getCandidateUnavailableReason()).isEqualTo("NONE");
        assertThat(assembled.getContext().getCandidateBlockedReason()).isEqualTo("NONE");
        assertThat(assembled.getContext().getCandidateDegradedReason()).isEqualTo("NONE");
        assertThat(assembled.getContext().getSourceOwnedTraceRefs()).containsExactly("source-owned-trace-ref");
        assertThat(assembled.getContext().getMissingFields()).containsExactly("none");
        assertThat(assembled.getContext().getDegradedReasons()).containsExactly("none");
        assertThat(assembled.getContext().getBlockedReasons()).containsExactly("none");
    }

    @Test
    void listFieldsAreDefensivelyCopied() {
        List<String> sourceTraceRefs = new ArrayList<>();
        sourceTraceRefs.add("source-trace-ref");
        SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput input =
                inputWithSourceTraceRefs(sourceTraceRefs);
        sourceTraceRefs.add("mutated-before-assemble");

        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                assembler.assemble(input);

        assertThat(assembled.getContext().getSourceTraceRefs()).containsExactly("source-trace-ref");
    }

    @Test
    void missingRequiredRefsRemainIncomplete() {
        assertIncompleteFor(assembler.assemble(inputWithRuntimeKlineContextRef(null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
    }

    @Test
    void sourceBlockedRemainsBlockedFailClosed() {
        assertBlockedFor(assembler.assemble(inputWithAnySourceBlocked(Boolean.TRUE)),
                "UPSTREAM_SOURCE_BLOCKED");
    }

    @Test
    void sourceIncompleteRemainsIncomplete() {
        assertIncompleteFor(assembler.assemble(inputWithAnySourceIncomplete(Boolean.TRUE)),
                "UPSTREAM_SOURCE_INCOMPLETE");
    }

    @Test
    void sourceDegradedRequiresDegradedReason() {
        assertIncompleteFor(assembler.assemble(inputWithAnySourceDegradedWithoutReason()),
                "DEGRADED_REASON_REQUIRED");
    }

    @Test
    void forbiddenExecutableSemanticCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithCandidateBoundaryLabel("buy signal")),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void resultIsNotCandidateRuntimeOrPointProposal() {
        SourceOwnedCandidateIntegrationSourceBindingAssembler
                .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getClass().getSimpleName()).doesNotContain("Runtime");
        assertThat(assembled.getClass().getSimpleName()).doesNotContain("PointProposal");
        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerClassHasNoSpringMyBatisJpaJacksonLombokAnnotations() {
        assertNoAnnotations(SourceOwnedCandidateIntegrationSourceBindingAssembler.class);
        assertNoAnnotations(SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput.class);
        assertNoAnnotations(
                SourceOwnedCandidateIntegrationSourceBindingAssembler
                        .AssembledSourceOwnedCandidateIntegrationSourceBinding.class
        );
    }

    @Test
    void assemblerDoesNotReferenceServiceControllerMapperRepositoryScheduler() throws Exception {
        assertSourceDoesNotContain(List.of(
                "@Service",
                "@Component",
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "Service",
                "Mapper",
                "Repository",
                "Scheduler"
        ));
    }

    @Test
    void assemblerDoesNotReferenceMarketQuoteHttpOrDataSourceProviders() throws Exception {
        assertSourceDoesNotContain(List.of(
                "MarketQuoteClient",
                "WebClient",
                "RestTemplate",
                "HttpClient",
                "OkHttp",
                "DataSource",
                "Binance",
                "OKX",
                "Bybit"
        ));
    }

    @Test
    void assemblerDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
        assertSourceDoesNotContain(List.of(
                "Telegram",
                "Webhook",
                "PushSend",
                "OrderIntent",
                "ExecutionIntent",
                "AutoTrading",
                "placeOrder",
                "createOrder",
                "closePosition",
                "reversePosition",
                "openPosition",
                "submitOrder",
                "WatchlistService",
                "RuleConfigService"
        ));
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput incompleteInput() {
        return input(
                "candidate-integration-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of(),
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
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                null,
                "SOURCE_BINDING_UNAVAILABLE",
                null,
                null,
                List.of(),
                List.of("sourceTraceRefs"),
                List.of(),
                List.of(),
                null,
                null,
                "CANDIDATE_BINDING_INPUT_MISSING",
                null,
                Boolean.TRUE,
                SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.INCOMPLETE
        );
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput blockedInput() {
        return input(
                "candidate-integration-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                null,
                null,
                "CANDIDATE_BINDING_BLOCKED",
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of("CANDIDATE_BINDING_BLOCKED"),
                null,
                null,
                null,
                "CANDIDATE_BINDING_BLOCKED",
                Boolean.FALSE,
                SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED
        );
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput degradedInput() {
        return input(
                "candidate-integration-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                "REVIEW_ONLY_SOURCE_TRACE",
                "REVIEW_ONLY_RUNTIME_KLINE",
                "REVIEW_ONLY_DATA_QUALITY",
                "REVIEW_ONLY_MULTITIMEFRAME",
                "REVIEW_ONLY_RISK_GUARD",
                "REVIEW_ONLY_WATCHLIST_POOL",
                bd("90"),
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                "REVIEW_ONLY_BOUNDARY",
                "NONE",
                null,
                "CANDIDATE_BINDING_DEGRADED",
                List.of("source-owned-trace-ref"),
                List.of("none"),
                List.of("CANDIDATE_BINDING_DEGRADED"),
                List.of(),
                "2026-06-04T00:00:00Z",
                "2026-06-04T00:01:00Z",
                "CANDIDATE_BINDING_DEGRADED",
                null,
                Boolean.TRUE,
                SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus
                        .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED
        );
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput completeInput() {
        return inputFrom(completeValues());
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput inputWithRequestedStatus(
            SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus value
    ) {
        return inputWith(39, value);
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput inputWithRuntimeKlineContextRef(
            String value
    ) {
        return inputWith(5, value);
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput inputWithAnySourceBlocked(
            Boolean value
    ) {
        return inputWith(23, value);
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput inputWithAnySourceIncomplete(
            Boolean value
    ) {
        return inputWith(24, value);
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput inputWithSourceTraceRefs(
            List<String> value
    ) {
        return inputWith(4, value);
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput inputWithCandidateBoundaryLabel(
            String value
    ) {
        return inputWith(26, value);
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput inputWithAnySourceDegradedWithoutReason() {
        Object[] values = completeValues();
        values[25] = Boolean.TRUE;
        values[29] = null;
        values[32] = List.of();
        return inputFrom(values);
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput inputWith(int index, Object value) {
        Object[] values = completeValues();
        values[index] = value;
        return inputFrom(values);
    }

    private Object[] completeValues() {
        return new Object[] {
                "candidate-integration-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                "REVIEW_ONLY_SOURCE_TRACE",
                "REVIEW_ONLY_RUNTIME_KLINE",
                "REVIEW_ONLY_DATA_QUALITY",
                "REVIEW_ONLY_MULTITIMEFRAME",
                "REVIEW_ONLY_RISK_GUARD",
                "REVIEW_ONLY_WATCHLIST_POOL",
                bd("95"),
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                "REVIEW_ONLY_BOUNDARY",
                "NONE",
                "NONE",
                "NONE",
                List.of("source-owned-trace-ref"),
                List.of("none"),
                List.of("none"),
                List.of("none"),
                "2026-06-04T00:00:00Z",
                "2026-06-04T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus
                        .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING
        };
    }

    @SuppressWarnings("unchecked")
    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput inputFrom(Object[] values) {
        return input(
                (String) values[0],
                (String) values[1],
                (String) values[2],
                (String) values[3],
                (List<String>) values[4],
                (String) values[5],
                (String) values[6],
                (String) values[7],
                (String) values[8],
                (String) values[9],
                (String) values[10],
                (String) values[11],
                (String) values[12],
                (String) values[13],
                (String) values[14],
                (String) values[15],
                (BigDecimal) values[16],
                (Boolean) values[17],
                (Boolean) values[18],
                (Boolean) values[19],
                (Boolean) values[20],
                (Boolean) values[21],
                (Boolean) values[22],
                (Boolean) values[23],
                (Boolean) values[24],
                (Boolean) values[25],
                (String) values[26],
                (String) values[27],
                (String) values[28],
                (String) values[29],
                (List<String>) values[30],
                (List<String>) values[31],
                (List<String>) values[32],
                (List<String>) values[33],
                (String) values[34],
                (String) values[35],
                (String) values[36],
                (String) values[37],
                (Boolean) values[38],
                (SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus) values[39]
        );
    }

    private SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput input(
            String candidateIntegrationContextId,
            String symbol,
            String market,
            String primaryTimeframe,
            List<String> sourceTraceRefs,
            String runtimeKlineContextRef,
            String dataQualityContextRef,
            String multiTimeframeContextRef,
            String riskActionGuardContextRef,
            String watchlistPoolProofContextRef,
            String sourceTraceStatus,
            String runtimeKlineStatus,
            String dataQualityStatus,
            String multiTimeframeStatus,
            String riskActionGuardStatus,
            String watchlistPoolProofStatus,
            BigDecimal sourceBindingCompletenessScore,
            Boolean allRequiredSourcesPresent,
            Boolean allRequiredSourcesTrusted,
            Boolean allRequiredSourcesReviewOnly,
            Boolean allRequiredSourcesNotTradeInstruction,
            Boolean allRequiredSourcesManualReviewRequired,
            Boolean allRequiredSourcesIncompleteSafe,
            Boolean anySourceBlocked,
            Boolean anySourceIncomplete,
            Boolean anySourceDegraded,
            String candidateBoundaryLabel,
            String candidateUnavailableReason,
            String candidateBlockedReason,
            String candidateDegradedReason,
            List<String> sourceOwnedTraceRefs,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons,
            String observedAt,
            String createdAt,
            String missingReason,
            String blockedReason,
            Boolean trustedSource,
            SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus requestedStatus
    ) {
        return SourceOwnedCandidateIntegrationSourceBindingAssembler.AssemblyInput.of(
                candidateIntegrationContextId,
                symbol,
                market,
                primaryTimeframe,
                sourceTraceRefs,
                runtimeKlineContextRef,
                dataQualityContextRef,
                multiTimeframeContextRef,
                riskActionGuardContextRef,
                watchlistPoolProofContextRef,
                sourceTraceStatus,
                runtimeKlineStatus,
                dataQualityStatus,
                multiTimeframeStatus,
                riskActionGuardStatus,
                watchlistPoolProofStatus,
                sourceBindingCompletenessScore,
                allRequiredSourcesPresent,
                allRequiredSourcesTrusted,
                allRequiredSourcesReviewOnly,
                allRequiredSourcesNotTradeInstruction,
                allRequiredSourcesManualReviewRequired,
                allRequiredSourcesIncompleteSafe,
                anySourceBlocked,
                anySourceIncomplete,
                anySourceDegraded,
                candidateBoundaryLabel,
                candidateUnavailableReason,
                candidateBlockedReason,
                candidateDegradedReason,
                sourceOwnedTraceRefs,
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

    private void assertIncompleteFor(
            SourceOwnedCandidateIntegrationSourceBindingAssembler
                    .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private void assertBlockedFor(
            SourceOwnedCandidateIntegrationSourceBindingAssembler
                    .AssembledSourceOwnedCandidateIntegrationSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(
                        SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED
                );
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
                        + "SourceOwnedCandidateIntegrationSourceBindingAssembler.java"
        ));
        for (String forbiddenSnippet : forbiddenSnippets) {
            assertThat(source).doesNotContain(forbiddenSnippet);
        }
    }

    private static class CountingValidator extends SourceOwnedCandidateIntegrationSourceBindingValidator {
        private int invocationCount;

        @Override
        public ValidationResult validate(SourceOwnedCandidateIntegrationSourceBindingDTO context) {
            invocationCount++;
            return super.validate(context);
        }
    }

    private static class OverridingValidator extends SourceOwnedCandidateIntegrationSourceBindingValidator {
        @Override
        public ValidationResult validate(SourceOwnedCandidateIntegrationSourceBindingDTO context) {
            return ValidationResult.blockedFailClosed(List.of("VALIDATOR_OVERRIDE"));
        }
    }
}
