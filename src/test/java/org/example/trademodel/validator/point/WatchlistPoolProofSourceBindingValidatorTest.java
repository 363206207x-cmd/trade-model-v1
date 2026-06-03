package org.example.trademodel.validator.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.WatchlistPoolProofSourceBindingDTO;
import org.junit.jupiter.api.Test;

class WatchlistPoolProofSourceBindingValidatorTest {

    private final WatchlistPoolProofSourceBindingValidator validator =
            new WatchlistPoolProofSourceBindingValidator();

    @Test
    void nullContextReturnsIncomplete() {
        assertIncompleteFor(validator.validate(null), "WATCHLIST_POOL_PROOF_BINDING_MISSING");
    }

    @Test
    void incompleteContextWithMissingReasonReturnsIncomplete() {
        assertIncompleteFor(validator.validate(incompleteContext()), "WATCHLIST_POOL_PROOF_MISSING");
    }

    @Test
    void incompleteContextWithoutMissingReasonIsSafelyIncomplete() throws Exception {
        WatchlistPoolProofSourceBindingDTO context = incompleteContext();
        forceField(context, "missingReason", null);

        assertIncompleteFor(validator.validate(context), "MISSING_REASON_REQUIRED");
    }

    @Test
    void blockedContextWithBlockedReasonReturnsBlockedFailClosed() {
        assertBlockedFor(validator.validate(blockedContext()), "WATCHLIST_POOL_DISABLED");
    }

    @Test
    void blockedContextWithoutBlockedReasonReturnsBlockedFailClosed() throws Exception {
        WatchlistPoolProofSourceBindingDTO context = blockedContext();
        forceField(context, "blockedReason", null);

        assertBlockedFor(validator.validate(context), "BLOCKED_REASON_REQUIRED");
    }

    @Test
    void completeContextReturnsReviewOnlyWatchlistPoolProofBinding() {
        WatchlistPoolProofSourceBindingValidator.ValidationResult result =
                validator.validate(reviewOnlyContext());

        assertThat(result.getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING);
        assertThat(result.isValidForReviewOnly()).isTrue();
    }

    @Test
    void degradedContextWithMissingReasonReturnsReviewOnlyWatchlistPoolProofBindingDegraded() {
        WatchlistPoolProofSourceBindingValidator.ValidationResult result =
                validator.validate(degradedContext());

        assertThat(result.getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED);
    }

    @Test
    void degradedContextWithoutMissingReasonReturnsIncomplete() throws Exception {
        WatchlistPoolProofSourceBindingDTO context = degradedContext();
        forceField(context, "missingReason", null);

        assertIncompleteFor(validator.validate(context), "MISSING_REASON_REQUIRED");
    }

    @Test
    void missingRequiredRefsReturnIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("sourceTraceRefs", List.of())),
                "SOURCE_TRACE_REFS_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("sourceTraceRefs", List.of(" "))),
                "SOURCE_TRACE_REF_BLANK");
        assertIncompleteFor(validator.validate(contextWithField("runtimeKlineContextRef", null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("dataQualityContextRef", null)),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("multiTimeframeContextRef", null)),
                "MULTITIMEFRAME_CONTEXT_REF_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("riskActionGuardContextRef", null)),
                "RISK_ACTION_GUARD_CONTEXT_REF_MISSING");
    }

    @Test
    void missingWatchlistFieldsReturnIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("watchlistPoolProofContextId", null)),
                "WATCHLIST_POOL_PROOF_CONTEXT_ID_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("primaryTimeframe", null)),
                "PRIMARY_TIMEFRAME_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("watchlistPoolRef", null)),
                "WATCHLIST_POOL_REF_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("watchlistPoolVersion", null)),
                "WATCHLIST_POOL_VERSION_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("watchlistMembershipSource", null)),
                "WATCHLIST_MEMBERSHIP_SOURCE_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("watchlistMembershipObservedAt", null)),
                "WATCHLIST_MEMBERSHIP_OBSERVED_AT_MISSING");
    }

    @Test
    void missingBooleanStatesReturnIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("watchlistPoolEnabled", null)),
                "WATCHLIST_POOL_ENABLED_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("watchlistPoolEmpty", null)),
                "WATCHLIST_POOL_EMPTY_STATUS_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("watchlistPoolMember", null)),
                "WATCHLIST_POOL_MEMBER_STATUS_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("proofFresh", null)),
                "PROOF_FRESH_STATUS_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("proofStale", null)),
                "PROOF_STALE_STATUS_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("displaySlotOnly", null)),
                "DISPLAY_SLOT_ONLY_STATUS_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("defaultDisplaySlot", null)),
                "DEFAULT_DISPLAY_SLOT_STATUS_MISSING");
    }

    @Test
    void disabledEmptyOrNonMemberPoolReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("watchlistPoolEnabled", Boolean.FALSE)),
                "WATCHLIST_POOL_DISABLED");
        assertBlockedFor(validator.validate(contextWithField("watchlistPoolEmpty", Boolean.TRUE)),
                "WATCHLIST_POOL_EMPTY");
        assertBlockedFor(validator.validate(contextWithField("watchlistPoolMember", Boolean.FALSE)),
                "WATCHLIST_POOL_MEMBER_MISSING");
    }

    @Test
    void displaySlotOnlyOrDefaultSlotWithoutPoolMembershipReturnsBlockedFailClosed() throws Exception {
        WatchlistPoolProofSourceBindingDTO displayOnly = reviewOnlyContext();
        forceField(displayOnly, "watchlistPoolMember", Boolean.FALSE);
        forceField(displayOnly, "displaySlotOnly", Boolean.TRUE);
        assertBlockedFor(validator.validate(displayOnly), "DISPLAY_SLOT_NOT_WATCHLIST_POOL_PROOF");

        WatchlistPoolProofSourceBindingDTO defaultSlot = reviewOnlyContext();
        forceField(defaultSlot, "watchlistPoolMember", Boolean.FALSE);
        forceField(defaultSlot, "defaultDisplaySlot", Boolean.TRUE);
        assertBlockedFor(validator.validate(defaultSlot), "DEFAULT_SLOT_NOT_WATCHLIST_POOL_PROOF");
    }

    @Test
    void staleOrNotFreshProofReturnsIncompleteWithoutReason() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("proofStale", Boolean.TRUE)),
                "WATCHLIST_POOL_PROOF_STALE");
        assertIncompleteFor(validator.validate(contextWithField("proofFresh", Boolean.FALSE)),
                "WATCHLIST_POOL_PROOF_FRESH_REQUIRED");
    }

    @Test
    void staleOrNotFreshProofWithReasonReturnsDegraded() {
        assertThat(validator.validate(degradedContext()).getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED);
    }

    @Test
    void missingAuditRefWithValidMembershipReturnsIncompleteOrDegraded() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("auditRef", null)),
                "WATCHLIST_POOL_AUDIT_REF_REQUIRED");

        WatchlistPoolProofSourceBindingDTO degraded = degradedContext();
        forceField(degraded, "proofFresh", Boolean.TRUE);
        forceField(degraded, "proofStale", Boolean.FALSE);
        forceField(degraded, "auditRef", null);
        assertThat(validator.validate(degraded).getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED);
    }

    @Test
    void promotedToHomeCandidateAndLowFrequencyScanCandidateRemainReviewOnlyLabels() {
        WatchlistPoolProofSourceBindingDTO context = reviewOnlyContext();

        assertThat(context.getPromotedToHomeCandidate()).isTrue();
        assertThat(context.getLowFrequencyScanCandidate()).isTrue();
        assertThat(validator.validate(context).getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING);
    }

    @Test
    void untrustedSourceReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("trustedSource", false)),
                "WATCHLIST_POOL_PROOF_SOURCE_UNTRUSTED");
    }

    @Test
    void safetyFlagFalseReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("reviewOnly", false)), "SAFETY_FLAG_REQUIRED");
        assertBlockedFor(validator.validate(contextWithField("notTradeInstruction", false)),
                "SAFETY_FLAG_REQUIRED");
        assertBlockedFor(validator.validate(contextWithField("manualReviewRequired", false)),
                "SAFETY_FLAG_REQUIRED");
        assertBlockedFor(validator.validate(contextWithField("incompleteSafe", false)),
                "SAFETY_FLAG_REQUIRED");
    }

    @Test
    void forbiddenExecutableSemanticReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("allowedCandidateBoundaryLabel", "push send")),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(validator.validate(contextWithField("blockedCandidateBoundaryLabel", "external channel")),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(validator.validate(contextWithField("sourceTraceRefs", List.of("send order"))),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(validator.validate(contextWithField("proofReason", "execute now")),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void validatorResultNormalSafeOutputsDoNotContainForbiddenExecutableSemantics() {
        List<WatchlistPoolProofSourceBindingValidator.ValidationResult> results = List.of(
                validator.validate(incompleteContext()),
                validator.validate(blockedContext()),
                validator.validate(degradedContext()),
                validator.validate(reviewOnlyContext())
        );

        for (WatchlistPoolProofSourceBindingValidator.ValidationResult result : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(result.getStatus().name());
            outputs.addAll(result.getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    @Test
    void validatorClassHasNoSpringAnnotations() {
        assertNoAnnotations(WatchlistPoolProofSourceBindingValidator.class);
        assertNoAnnotations(WatchlistPoolProofSourceBindingValidator.ValidationResult.class);
    }

    @Test
    void validatorDoesNotReferenceAssemblerServiceControllerMapperRepositoryOrScheduler() throws Exception {
        assertSourceDoesNotContain(List.of(
                "Assembler",
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
    void validatorDoesNotReferenceMarketQuoteHttpOrDataSourceProviders() throws Exception {
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
    void validatorDoesNotReferenceExternalPushExecutionOrAutoTradingClasses() throws Exception {
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

    private WatchlistPoolProofSourceBindingDTO incompleteContext() {
        return WatchlistPoolProofSourceBindingDTO.incomplete(
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
                List.of("watchlistPoolVersion"),
                "WATCHLIST_POOL_PROOF_MISSING"
        );
    }

    private WatchlistPoolProofSourceBindingDTO blockedContext() {
        return WatchlistPoolProofSourceBindingDTO.blockedFailClosed(
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
                List.of("WATCHLIST_POOL_DISABLED"),
                "WATCHLIST_POOL_BLOCKED",
                "WATCHLIST_POOL_DISABLED"
        );
    }

    private WatchlistPoolProofSourceBindingDTO degradedContext() {
        return WatchlistPoolProofSourceBindingDTO.degraded(
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
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                "display-slot-ref",
                Boolean.TRUE,
                Boolean.TRUE,
                "audit-ref",
                "operator-ref",
                "POOL_MEMBER_REVIEW",
                "PROOF_STALE",
                "REVIEW_ONLY_CANDIDATE_BOUNDARY",
                null,
                List.of(),
                List.of("PROOF_STALE"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "WATCHLIST_POOL_PROOF_DEGRADED",
                Boolean.TRUE
        );
    }

    private WatchlistPoolProofSourceBindingDTO reviewOnlyContext() {
        return WatchlistPoolProofSourceBindingDTO.reviewOnly(
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
                Boolean.TRUE
        );
    }

    private WatchlistPoolProofSourceBindingDTO contextWithField(String fieldName, Object value) throws Exception {
        WatchlistPoolProofSourceBindingDTO context = reviewOnlyContext();
        forceField(context, fieldName, value);
        return context;
    }

    private static void forceField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static void assertIncompleteFor(
            WatchlistPoolProofSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertBlockedFor(
            WatchlistPoolProofSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/validator/point/WatchlistPoolProofSourceBindingValidator.java"
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
}
