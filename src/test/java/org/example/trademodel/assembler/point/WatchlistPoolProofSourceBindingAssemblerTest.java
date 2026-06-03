package org.example.trademodel.assembler.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.WatchlistPoolProofSourceBindingDTO;
import org.example.trademodel.validator.point.WatchlistPoolProofSourceBindingValidator;
import org.junit.jupiter.api.Test;

class WatchlistPoolProofSourceBindingAssemblerTest {

    private final WatchlistPoolProofSourceBindingAssembler assembler =
            new WatchlistPoolProofSourceBindingAssembler();

    @Test
    void nullInputCreatesIncompleteContextAndValidation() {
        WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled =
                assembler.assemble(null);

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingDTO.BindingStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus.INCOMPLETE);
    }

    @Test
    void incompleteInputCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(incompleteInput()), "WATCHLIST_POOL_PROOF_MISSING");
    }

    @Test
    void blockedInputCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(blockedInput()), "WATCHLIST_POOL_DISABLED");
    }

    @Test
    void completeReviewOnlyInputCreatesReviewOnlyValidation() {
        WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getBindingStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingDTO.BindingStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING);
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING);
    }

    @Test
    void degradedInputCreatesReviewOnlyWatchlistPoolProofBindingDegradedValidation() {
        WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled =
                assembler.assemble(degradedInput());

        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED);
    }

    @Test
    void missingRequiredRefsCreateIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithSourceTraceRefs(List.of())),
                "SOURCE_TRACE_REFS_MISSING");
        assertIncompleteFor(assembler.assemble(inputWithRuntimeKlineContextRef(null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
        assertIncompleteFor(assembler.assemble(inputWithDataQualityContextRef(null)),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
        assertIncompleteFor(assembler.assemble(inputWithMultiTimeframeContextRef(null)),
                "MULTITIMEFRAME_CONTEXT_REF_MISSING");
        assertIncompleteFor(assembler.assemble(inputWithRiskActionGuardContextRef(null)),
                "RISK_ACTION_GUARD_CONTEXT_REF_MISSING");
    }

    @Test
    void watchlistPoolDisabledEmptyOrNonMemberCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithWatchlistPoolEnabled(Boolean.FALSE)),
                "WATCHLIST_POOL_DISABLED");
        assertBlockedFor(assembler.assemble(inputWithWatchlistPoolEmpty(Boolean.TRUE)),
                "WATCHLIST_POOL_EMPTY");
        assertBlockedFor(assembler.assemble(inputWithWatchlistPoolMember(Boolean.FALSE)),
                "WATCHLIST_POOL_MEMBER_MISSING");
    }

    @Test
    void displaySlotOnlyOrDefaultSlotWithoutPoolMembershipCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithDisplayOnlyAndMembership(Boolean.TRUE, Boolean.FALSE)),
                "DISPLAY_SLOT_NOT_WATCHLIST_POOL_PROOF");
        assertBlockedFor(assembler.assemble(inputWithDefaultSlotAndMembership(Boolean.TRUE, Boolean.FALSE)),
                "DEFAULT_SLOT_NOT_WATCHLIST_POOL_PROOF");
    }

    @Test
    void staleOrNotFreshProofCreatesIncompleteOrDegradedValidation() {
        assertIncompleteFor(assembler.assemble(inputWithProofStale(Boolean.TRUE)),
                "WATCHLIST_POOL_PROOF_STALE");
        assertIncompleteFor(assembler.assemble(inputWithProofFresh(Boolean.FALSE)),
                "WATCHLIST_POOL_PROOF_FRESH_REQUIRED");
        assertThat(assembler.assemble(degradedInput()).getValidationResult().getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED);
    }

    @Test
    void auditRefMissingWithMembershipCreatesIncompleteValidation() {
        assertIncompleteFor(assembler.assemble(inputWithAuditRef(null)), "WATCHLIST_POOL_AUDIT_REF_REQUIRED");
    }

    @Test
    void promotedToHomeCandidateDoesNotEqualExternalPush() {
        WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled =
                assembler.assemble(inputWithPromoteAndLowFrequency(Boolean.TRUE, Boolean.TRUE));

        assertThat(assembled.getContext().getPromotedToHomeCandidate()).isTrue();
        assertThat(assembled.getContext().getLowFrequencyScanCandidate()).isTrue();
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING);
    }

    @Test
    void forbiddenExecutableSemanticCreatesBlockedFailClosedValidation() {
        assertBlockedFor(assembler.assemble(inputWithAllowedBoundary("push send")),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(assembler.assemble(inputWithBlockedBoundary("external channel")),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(assembler.assemble(inputWithSourceTraceRefs(List.of("send order"))),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void explicitWatchlistFieldsArePreserved() {
        WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled =
                assembler.assemble(completeInput());

        assertThat(assembled.getContext().getWatchlistPoolRef()).isEqualTo("watchlist-pool-ref");
        assertThat(assembled.getContext().getWatchlistPoolVersion()).isEqualTo("v1");
        assertThat(assembled.getContext().getWatchlistMembershipSource()).isEqualTo("WATCHLIST_POOL");
        assertThat(assembled.getContext().getAllowedCandidateBoundaryLabel())
                .isEqualTo("REVIEW_ONLY_CANDIDATE_BOUNDARY");
    }

    @Test
    void sourceTraceRefsArePreservedAndDefensivelyCopied() {
        List<String> refs = new ArrayList<>();
        refs.add("source-trace-ref");

        WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled =
                assembler.assemble(inputWithSourceTraceRefs(refs));
        refs.add("mutated-ref");

        assertThat(assembled.getContext().getSourceTraceRefs()).containsExactly("source-trace-ref");
    }

    @Test
    void assemblerCallsValidatorAndReturnsValidationResult() {
        CountingValidator countingValidator = new CountingValidator();
        WatchlistPoolProofSourceBindingAssembler countingAssembler =
                new WatchlistPoolProofSourceBindingAssembler(countingValidator);

        WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled =
                countingAssembler.assemble(completeInput());

        assertThat(countingValidator.invocationCount).isEqualTo(1);
        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult()).isNotNull();
    }

    @Test
    void assemblerHandlesNullFieldsWithoutException() {
        WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled =
                assembler.assemble(inputWithNullFields());

        assertThat(assembled.getContext()).isNotNull();
        assertThat(assembled.getValidationResult().getStatus())
                .isIn(
                        WatchlistPoolProofSourceBindingValidator.ValidationStatus.INCOMPLETE,
                        WatchlistPoolProofSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED
                );
    }

    @Test
    void assemblerClassHasNoSpringAnnotations() {
        assertNoAnnotations(WatchlistPoolProofSourceBindingAssembler.class);
        assertNoAnnotations(WatchlistPoolProofSourceBindingAssembler.AssemblyInput.class);
        assertNoAnnotations(WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding.class);
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
                "Mapper",
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
                "submitOrder",
                "WatchlistService",
                "RuleConfigService"
        ));
    }

    @Test
    void assemblerSafeOutputDoesNotContainForbiddenExecutableSemantics() {
        List<WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding> results = List.of(
                assembler.assemble(incompleteInput()),
                assembler.assemble(blockedInput()),
                assembler.assemble(degradedInput()),
                assembler.assemble(completeInput())
        );

        for (WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(assembled.getContext().getBindingStatus().name());
            outputs.add(assembled.getContext().getMissingReason());
            outputs.add(assembled.getContext().getBlockedReason());
            outputs.add(assembled.getValidationResult().getStatus().name());
            outputs.addAll(assembled.getValidationResult().getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput incompleteInput() {
        Object[] values = completeValues();
        values[10] = null;
        values[30] = List.of("watchlistPoolVersion");
        values[35] = "WATCHLIST_POOL_PROOF_MISSING";
        values[38] = WatchlistPoolProofSourceBindingDTO.BindingStatus.INCOMPLETE;
        return inputFrom(values);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput blockedInput() {
        Object[] values = completeValues();
        values[32] = List.of("WATCHLIST_POOL_DISABLED");
        values[29] = "WATCHLIST_POOL_BLOCKED";
        values[36] = "WATCHLIST_POOL_DISABLED";
        values[37] = Boolean.FALSE;
        values[38] = WatchlistPoolProofSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED;
        return inputFrom(values);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput degradedInput() {
        Object[] values = completeValues();
        values[17] = Boolean.FALSE;
        values[18] = Boolean.TRUE;
        values[31] = List.of("PROOF_STALE");
        values[35] = "WATCHLIST_POOL_PROOF_DEGRADED";
        values[38] = WatchlistPoolProofSourceBindingDTO.BindingStatus
                .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED;
        return inputFrom(values);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput completeInput() {
        return inputFrom(completeValues());
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithSourceTraceRefs(List<String> value) {
        return inputWith(4, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithRuntimeKlineContextRef(String value) {
        return inputWith(5, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithDataQualityContextRef(String value) {
        return inputWith(6, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithMultiTimeframeContextRef(String value) {
        return inputWith(7, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithRiskActionGuardContextRef(String value) {
        return inputWith(8, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithWatchlistPoolEnabled(Boolean value) {
        return inputWith(11, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithWatchlistPoolEmpty(Boolean value) {
        return inputWith(12, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithWatchlistPoolMember(Boolean value) {
        return inputWith(13, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithDisplayOnlyAndMembership(
            Boolean displaySlotOnly,
            Boolean poolMember
    ) {
        Object[] values = completeValues();
        values[19] = displaySlotOnly;
        values[13] = poolMember;
        return inputFrom(values);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithDefaultSlotAndMembership(
            Boolean defaultDisplaySlot,
            Boolean poolMember
    ) {
        Object[] values = completeValues();
        values[20] = defaultDisplaySlot;
        values[13] = poolMember;
        return inputFrom(values);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithProofStale(Boolean value) {
        return inputWith(18, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithProofFresh(Boolean value) {
        return inputWith(17, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithAuditRef(String value) {
        return inputWith(24, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithPromoteAndLowFrequency(
            Boolean promoted,
            Boolean lowFrequency
    ) {
        Object[] values = completeValues();
        values[22] = promoted;
        values[23] = lowFrequency;
        return inputFrom(values);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithAllowedBoundary(String value) {
        return inputWith(28, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithBlockedBoundary(String value) {
        return inputWith(29, value);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWithNullFields() {
        Object[] values = new Object[39];
        values[38] = WatchlistPoolProofSourceBindingDTO.BindingStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING;
        return inputFrom(values);
    }

    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputWith(int index, Object value) {
        Object[] values = completeValues();
        values[index] = value;
        return inputFrom(values);
    }

    private Object[] completeValues() {
        return new Object[] {
                "proof-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-ref",
                "v1",
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                "WATCHLIST_POOL",
                "2026-06-03T00:00:00Z",
                "2026-06-04T00:00:00Z",
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                "display-slot-ref",
                Boolean.TRUE,
                Boolean.TRUE,
                "audit-ref",
                "operator-ref",
                "POOL_MEMBER_REVIEW",
                "PROOF_FRESH",
                "REVIEW_ONLY_CANDIDATE_BOUNDARY",
                null,
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                null,
                null,
                Boolean.TRUE,
                WatchlistPoolProofSourceBindingDTO.BindingStatus.REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING
        };
    }

    @SuppressWarnings("unchecked")
    private WatchlistPoolProofSourceBindingAssembler.AssemblyInput inputFrom(Object[] values) {
        return WatchlistPoolProofSourceBindingAssembler.AssemblyInput.of(
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
                (Boolean) values[11],
                (Boolean) values[12],
                (Boolean) values[13],
                (String) values[14],
                (String) values[15],
                (String) values[16],
                (Boolean) values[17],
                (Boolean) values[18],
                (Boolean) values[19],
                (Boolean) values[20],
                (String) values[21],
                (Boolean) values[22],
                (Boolean) values[23],
                (String) values[24],
                (String) values[25],
                (String) values[26],
                (String) values[27],
                (String) values[28],
                (String) values[29],
                (List<String>) values[30],
                (List<String>) values[31],
                (List<String>) values[32],
                (String) values[33],
                (String) values[34],
                (String) values[35],
                (String) values[36],
                (Boolean) values[37],
                (WatchlistPoolProofSourceBindingDTO.BindingStatus) values[38]
        );
    }

    private static void assertIncompleteFor(
            WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private static void assertBlockedFor(
            WatchlistPoolProofSourceBindingAssembler.AssembledWatchlistPoolProofSourceBinding assembled,
            String reason
    ) {
        assertThat(assembled.getValidationResult().getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(assembled.getValidationResult().getReasons()).contains(reason);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/assembler/point/WatchlistPoolProofSourceBindingAssembler.java"
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
                "push send",
                "external channel",
                "send order"
        );
        String joined = String.join(" ", outputs).toLowerCase();
        for (String forbiddenToken : forbidden) {
            assertThat(joined).doesNotContain(forbiddenToken);
        }
    }

    private static class CountingValidator extends WatchlistPoolProofSourceBindingValidator {
        private int invocationCount;

        @Override
        public ValidationResult validate(WatchlistPoolProofSourceBindingDTO context) {
            invocationCount++;
            return super.validate(context);
        }
    }
}
