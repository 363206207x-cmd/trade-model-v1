package org.example.trademodel.assembler.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.assembler.point.SourceOwnedCandidateIntegrationRuntimeCandidateAssembler
        .AssembledSourceOwnedCandidateIntegrationRuntimeCandidate;
import org.example.trademodel.assembler.point.SourceOwnedCandidateIntegrationRuntimeCandidateAssembler
        .RuntimeAssemblyInput;
import org.example.trademodel.dto.point.SourceOwnedCandidateIntegrationRuntimeCandidateDTO;
import org.example.trademodel.dto.point.SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus;
import org.example.trademodel.validator.point.SourceOwnedCandidateIntegrationRuntimeCandidateValidator;
import org.junit.jupiter.api.Test;

class SourceOwnedCandidateIntegrationRuntimeCandidateAssemblerTest {

    private final SourceOwnedCandidateIntegrationRuntimeCandidateAssembler assembler =
            new SourceOwnedCandidateIntegrationRuntimeCandidateAssembler();

    @Test
    void nullInputCreatesIncompleteContextAndValidation() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(null);

        assertThat(result.getContext().getCandidateRuntimeStatus()).isEqualTo(RuntimeStatus.INCOMPLETE);
        assertThat(result.getContext().getMissingReason())
                .isEqualTo("SOURCE_OWNED_CANDIDATE_RUNTIME_INPUT_MISSING");
        assertIncomplete(result);
    }

    @Test
    void incompleteInputCreatesIncompleteValidation() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result =
                assembler.assemble(inputWithStatus(RuntimeStatus.INCOMPLETE));

        assertThat(result.getContext().getCandidateRuntimeStatus()).isEqualTo(RuntimeStatus.INCOMPLETE);
        assertIncomplete(result);
    }

    @Test
    void blockedInputCreatesBlockedFailClosedValidation() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result =
                assembler.assemble(inputWithStatus(RuntimeStatus.BLOCKED_FAIL_CLOSED));

        assertThat(result.getContext().getCandidateRuntimeStatus())
                .isEqualTo(RuntimeStatus.BLOCKED_FAIL_CLOSED);
        assertBlocked(result);
    }

    @Test
    void degradedInputCreatesReviewOnlyDegradedValidation() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result =
                assembler.assemble(inputWithStatus(RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED));

        assertThat(result.getContext().getCandidateRuntimeStatus())
                .isEqualTo(RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED);
        assertThat(result.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED);
    }

    @Test
    void completeReviewOnlyInputCreatesReviewOnlyValidValidation() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(completeInput());

        assertThat(result.getContext().getCandidateRuntimeStatus())
                .isEqualTo(RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE);
        assertThat(result.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .REVIEW_ONLY_RUNTIME_CANDIDATE_VALID);
        assertThat(result.getValidationResult().isValidForReviewOnly()).isTrue();
    }

    @Test
    void unsupportedStatusFallsBackToIncomplete() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result =
                assembler.assemble(inputWithStatus(null));

        assertThat(result.getContext().getCandidateRuntimeStatus()).isEqualTo(RuntimeStatus.INCOMPLETE);
        assertThat(result.getContext().getMissingReason())
                .isEqualTo("UNSUPPORTED_SOURCE_OWNED_CANDIDATE_RUNTIME_STATUS");
        assertIncomplete(result);
    }

    @Test
    void assemblerCallsValidatorAndReturnsValidationResult() {
        CountingValidator validator = new CountingValidator();
        SourceOwnedCandidateIntegrationRuntimeCandidateAssembler localAssembler =
                new SourceOwnedCandidateIntegrationRuntimeCandidateAssembler(validator);

        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result =
                localAssembler.assemble(completeInput());

        assertThat(validator.callCount).isEqualTo(1);
        assertThat(validator.seenContext).isSameAs(result.getContext());
        assertThat(result.getValidationResult()).isSameAs(validator.returnedResult);
    }

    @Test
    void assemblerDoesNotModifyDtoAfterValidation() {
        OverridingValidator validator = new OverridingValidator();
        SourceOwnedCandidateIntegrationRuntimeCandidateAssembler localAssembler =
                new SourceOwnedCandidateIntegrationRuntimeCandidateAssembler(validator);

        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result =
                localAssembler.assemble(completeInput());

        assertThat(result.getContext().getCandidateRuntimeStatus())
                .isEqualTo(RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE);
        assertThat(result.getContext().isFailClosed()).isFalse();
        assertThat(result.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .BLOCKED_FAIL_CLOSED);
    }

    @Test
    void explicitRefsAndStatusesArePreserved() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(completeInput());

        assertThat(result.getContext().getRuntimeCandidateContextId()).isEqualTo("runtime-candidate-1");
        assertThat(result.getContext().getSourceOwnedCandidateIntegrationSourceBindingRef())
                .isEqualTo("source-binding-ref");
        assertThat(result.getContext().getSourceOwnedCandidateIntegrationValidationStatus())
                .isEqualTo("REVIEW_ONLY_RUNTIME");
        assertThat(result.getContext().getRuntimeKlineContextRef()).isEqualTo("runtime-kline-ref");
        assertThat(result.getContext().getDataQualityContextRef()).isEqualTo("data-quality-ref");
        assertThat(result.getContext().getMultiTimeframeContextRef()).isEqualTo("multi-timeframe-ref");
        assertThat(result.getContext().getRiskActionGuardContextRef()).isEqualTo("risk-action-guard-ref");
        assertThat(result.getContext().getWatchlistPoolProofContextRef()).isEqualTo("watchlist-pool-proof-ref");
    }

    @Test
    void explicitLabelsAndReasonsArePreserved() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(completeInput(
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE,
                List.of("source-trace-ref"),
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                "CANDIDATE_UNAVAILABLE_SAFE",
                "CANDIDATE_BLOCKED_SAFE",
                "CANDIDATE_DEGRADED_SAFE",
                "MISSING_SAFE",
                "BLOCKED_SAFE",
                List.of("missing-safe"),
                List.of("degraded-safe"),
                List.of("blocked-safe")
        ));

        assertThat(result.getContext().getCandidateUnavailableReason())
                .isEqualTo("CANDIDATE_UNAVAILABLE_SAFE");
        assertThat(result.getContext().getCandidateBlockedReason()).isEqualTo("CANDIDATE_BLOCKED_SAFE");
        assertThat(result.getContext().getCandidateDegradedReason()).isEqualTo("CANDIDATE_DEGRADED_SAFE");
        assertThat(result.getContext().getMissingFields()).containsExactly("missing-safe");
        assertThat(result.getContext().getDegradedReasons()).containsExactly("degraded-safe");
        assertThat(result.getContext().getBlockedReasons()).containsExactly("blocked-safe");
    }

    @Test
    void listFieldsAreDefensivelyCopied() {
        List<String> sourceTraceRefs = new ArrayList<>();
        sourceTraceRefs.add("source-trace-ref");
        RuntimeAssemblyInput input = completeInputWithRefs(sourceTraceRefs);

        sourceTraceRefs.add("mutated-ref");

        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(input);

        assertThat(result.getContext().getSourceTraceRefs()).containsExactly("source-trace-ref");
        assertThat(result.getContext().getSourceTraceRefs()).isUnmodifiable();
    }

    @Test
    void missingRequiredRefsRemainIncomplete() {
        RuntimeAssemblyInput input = completeInput(
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE,
                List.of(),
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                null,
                null,
                null,
                "MISSING_SAFE",
                null,
                List.of("sourceTraceRefs"),
                List.of(),
                List.of()
        );

        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(input);

        assertIncomplete(result);
        assertThat(result.getValidationResult().getReasons()).contains("SOURCE_TRACE_REFS_MISSING");
    }

    @Test
    void sourceBlockedRemainsBlockedFailClosed() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(completeInput(
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE,
                List.of("source-trace-ref"),
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                null,
                null,
                null,
                "MISSING_SAFE",
                null,
                List.of(),
                List.of(),
                List.of("blocked-safe")
        ));

        assertBlocked(result);
        assertThat(result.getValidationResult().getReasons()).contains("UPSTREAM_SOURCE_BLOCKED");
    }

    @Test
    void sourceIncompleteRemainsIncomplete() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(completeInput(
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE,
                List.of("source-trace-ref"),
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                null,
                null,
                null,
                "MISSING_SAFE",
                null,
                List.of("source-incomplete"),
                List.of(),
                List.of()
        ));

        assertIncomplete(result);
        assertThat(result.getValidationResult().getReasons()).contains("UPSTREAM_SOURCE_INCOMPLETE");
    }

    @Test
    void sourceDegradedRequiresDegradedReason() {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(completeInput(
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED,
                List.of("source-trace-ref"),
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                null,
                null,
                null,
                "MISSING_SAFE",
                null,
                List.of(),
                List.of(),
                List.of()
        ));

        assertThat(result.getContext().getCandidateDegradedReason())
                .isEqualTo("DEGRADED_REASON_REQUIRED");
        assertThat(result.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED);
    }

    @Test
    void forbiddenExecutableSemanticCreatesBlockedFailClosedValidation() {
        RuntimeAssemblyInput input = completeInput(
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE,
                List.of("source-trace-ref"),
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                "market buy",
                null,
                null,
                "MISSING_SAFE",
                null,
                List.of(),
                List.of(),
                List.of()
        );

        assertBlocked(assembler.assemble(input));
    }

    @Test
    void resultIsNotPointProposalFinalDirectionPushPayloadOrTradeAction() throws Exception {
        AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result = assembler.assemble(completeInput());

        assertThat(result.getContext()).isNotNull();
        assertThat(result.getValidationResult()).isNotNull();
        assertSourceDoesNotContain(List.of(
                "PointProposal",
                "finalDirection",
                "pushPayload",
                "tradeAction"
        ));
    }

    @Test
    void assemblerClassHasNoSpringMyBatisJpaJacksonLombokAnnotations() throws Exception {
        assertNoAnnotations(SourceOwnedCandidateIntegrationRuntimeCandidateAssembler.class);
        assertNoAnnotations(RuntimeAssemblyInput.class);
        assertNoAnnotations(AssembledSourceOwnedCandidateIntegrationRuntimeCandidate.class);
        assertSourceDoesNotContain(List.of(
                "@Service",
                "@Component",
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Entity",
                "@Table",
                "@Json",
                "lombok"
        ));
    }

    @Test
    void assemblerDoesNotReferenceServiceControllerMapperRepositoryScheduler() throws Exception {
        assertSourceDoesNotContain(List.of(
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "Service",
                "Controller",
                "Mapper",
                "Repository",
                "Scheduler"
        ));
    }

    @Test
    void assemblerDoesNotReferenceMarketQuoteHttpOrDataSourceProviders() throws Exception {
        assertSourceDoesNotContain(List.of(
                "MarketQuoteClient",
                "market client",
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
                "entry",
                "stop",
                "takeProfit",
                "leverage",
                "positionSize",
                "orderId",
                "finalDirection",
                "tradeAction"
        ));
    }

    private RuntimeAssemblyInput completeInput() {
        return completeInput(
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE,
                List.of("source-trace-ref"),
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                null,
                null,
                null,
                "MISSING_SAFE",
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private RuntimeAssemblyInput inputWithStatus(RuntimeStatus status) {
        if (status == RuntimeStatus.INCOMPLETE) {
            return incompleteInput();
        }
        if (status == RuntimeStatus.BLOCKED_FAIL_CLOSED) {
            return blockedInput();
        }
        if (status == RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED) {
            return degradedInput();
        }
        if (status == RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE) {
            return completeInput();
        }
        return completeInput(
                null,
                List.of("source-trace-ref"),
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private RuntimeAssemblyInput incompleteInput() {
        return completeInput(
                RuntimeStatus.INCOMPLETE,
                List.of("source-trace-ref"),
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                "RUNTIME_CANDIDATE_UNAVAILABLE",
                null,
                null,
                "RUNTIME_CANDIDATE_UNAVAILABLE",
                null,
                List.of("sourceBindingCompletenessScore"),
                List.of(),
                List.of()
        );
    }

    private RuntimeAssemblyInput blockedInput() {
        return completeInput(
                RuntimeStatus.BLOCKED_FAIL_CLOSED,
                List.of("source-trace-ref"),
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                null,
                "RUNTIME_CANDIDATE_BLOCKED",
                null,
                null,
                "RUNTIME_CANDIDATE_BLOCKED",
                List.of(),
                List.of(),
                List.of("RUNTIME_CANDIDATE_BLOCKED")
        );
    }

    private RuntimeAssemblyInput degradedInput() {
        return completeInput(
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED,
                List.of("source-trace-ref"),
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                null,
                null,
                "RUNTIME_CANDIDATE_DEGRADED",
                "RUNTIME_CANDIDATE_DEGRADED",
                null,
                List.of(),
                List.of("RUNTIME_CANDIDATE_DEGRADED"),
                List.of()
        );
    }

    private RuntimeAssemblyInput completeInputWithRefs(List<String> sourceTraceRefs) {
        return completeInput(
                RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE,
                sourceTraceRefs,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.TRUE,
                null,
                null,
                null,
                "MISSING_SAFE",
                null,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private RuntimeAssemblyInput completeInput(
            RuntimeStatus requestedStatus,
            List<String> sourceTraceRefs,
            Boolean anySourceBlocked,
            Boolean anySourceIncomplete,
            Boolean anySourceDegraded,
            Boolean watchlistPoolMember,
            Boolean watchlistPoolProofFresh,
            Boolean riskActionGuardBlocked,
            Boolean riskActionGuardStampede,
            Boolean runtimeKlineStale,
            Boolean dataQualityPassed,
            Boolean multiTimeframeConfirmed,
            String candidateUnavailableReason,
            String candidateBlockedReason,
            String candidateDegradedReason,
            String missingReason,
            String blockedReason,
            List<String> missingFields,
            List<String> degradedReasons,
            List<String> blockedReasons
    ) {
        return RuntimeAssemblyInput.of(
                "runtime-candidate-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                requestedStatus,
                "source-binding-ref",
                sourceBindingStatusFor(requestedStatus),
                List.of("SOURCE_BINDING_READY"),
                requestedStatus == RuntimeStatus.INCOMPLETE ? null : bd("96"),
                "SOURCE_BINDING_READY",
                sourceTraceRefs,
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                requestedStatus != RuntimeStatus.INCOMPLETE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                anySourceBlocked,
                anySourceIncomplete,
                anySourceDegraded,
                watchlistPoolMember,
                watchlistPoolProofFresh,
                riskActionGuardBlocked,
                riskActionGuardStampede,
                runtimeKlineStale,
                dataQualityPassed,
                multiTimeframeConfirmed,
                candidateUnavailableReason,
                candidateBlockedReason,
                candidateDegradedReason,
                missingReason,
                blockedReason,
                missingFields,
                degradedReasons,
                blockedReasons,
                requestedStatus == RuntimeStatus.INCOMPLETE ? null : "2026-06-04T00:00:00Z",
                "2026-06-04T00:01:00Z"
        );
    }

    private static String sourceBindingStatusFor(RuntimeStatus status) {
        if (status == RuntimeStatus.INCOMPLETE) {
            return "INCOMPLETE";
        }
        if (status == RuntimeStatus.BLOCKED_FAIL_CLOSED) {
            return "BLOCKED_FAIL_CLOSED";
        }
        if (status == RuntimeStatus.REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED) {
            return "REVIEW_ONLY_DEGRADED";
        }
        return "REVIEW_ONLY_RUNTIME";
    }

    private static void assertIncomplete(AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result) {
        assertThat(result.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.getValidationResult().isIncomplete()).isTrue();
    }

    private static void assertBlocked(AssembledSourceOwnedCandidateIntegrationRuntimeCandidate result) {
        assertThat(result.getValidationResult().getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .BLOCKED_FAIL_CLOSED);
        assertThat(result.getValidationResult().isBlockedFailClosed()).isTrue();
        assertThat(result.getValidationResult().isFailClosed()).isTrue();
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
                "src/main/java/org/example/trademodel/assembler/point/"
                        + "SourceOwnedCandidateIntegrationRuntimeCandidateAssembler.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }

    private static class CountingValidator extends SourceOwnedCandidateIntegrationRuntimeCandidateValidator {
        private int callCount;
        private SourceOwnedCandidateIntegrationRuntimeCandidateDTO seenContext;
        private ValidationResult returnedResult;

        @Override
        public ValidationResult validate(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
            callCount++;
            seenContext = context;
            returnedResult = super.validate(context);
            return returnedResult;
        }
    }

    private static class OverridingValidator extends SourceOwnedCandidateIntegrationRuntimeCandidateValidator {
        @Override
        public ValidationResult validate(SourceOwnedCandidateIntegrationRuntimeCandidateDTO context) {
            return ValidationResult.blockedFailClosed(List.of("VALIDATION_OVERRIDE"));
        }
    }
}
