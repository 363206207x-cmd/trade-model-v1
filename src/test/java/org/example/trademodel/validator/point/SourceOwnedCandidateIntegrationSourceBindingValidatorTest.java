package org.example.trademodel.validator.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.SourceOwnedCandidateIntegrationSourceBindingDTO;
import org.junit.jupiter.api.Test;

class SourceOwnedCandidateIntegrationSourceBindingValidatorTest {

    private final SourceOwnedCandidateIntegrationSourceBindingValidator validator =
            new SourceOwnedCandidateIntegrationSourceBindingValidator();

    @Test
    void nullContextCreatesIncompleteValidation() {
        assertIncompleteFor(validator.validate(null), "CANDIDATE_INTEGRATION_BINDING_MISSING");
    }

    @Test
    void missingBindingStatusCreatesIncompleteValidation() throws Exception {
        SourceOwnedCandidateIntegrationSourceBindingDTO context = reviewOnlyContext();
        forceField(context, "bindingStatus", null);

        assertIncompleteFor(validator.validate(context), "BINDING_STATUS_MISSING");
    }

    @Test
    void incompleteContextKeepsIncomplete() {
        assertIncompleteFor(validator.validate(incompleteContext()), "SOURCE_BINDING_MISSING");
    }

    @Test
    void blockedFailClosedContextKeepsBlockedFailClosed() {
        assertBlockedFor(validator.validate(blockedContext()), "SOURCE_BINDING_BLOCKED");
    }

    @Test
    void reviewOnlyContextCreatesReviewOnlyValidation() {
        SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult result =
                validator.validate(reviewOnlyContext());

        assertThat(result.getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING);
        assertThat(result.isValidForReviewOnly()).isTrue();
    }

    @Test
    void degradedContextCreatesReviewOnlyDegradedValidation() {
        SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult result =
                validator.validate(degradedContext());

        assertThat(result.getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED);
    }

    @Test
    void missingSourceTraceRefsCreatesIncompleteValidation() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("sourceTraceRefs", List.of())),
                "SOURCE_TRACE_REFS_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("sourceTraceRefs", List.of(" "))),
                "SOURCE_TRACE_REF_BLANK");
    }

    @Test
    void missingRuntimeKlineContextRefCreatesIncompleteValidation() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("runtimeKlineContextRef", null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
    }

    @Test
    void missingDataQualityContextRefCreatesIncompleteValidation() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("dataQualityContextRef", null)),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
    }

    @Test
    void missingMultiTimeframeContextRefCreatesIncompleteValidation() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("multiTimeframeContextRef", null)),
                "MULTITIMEFRAME_CONTEXT_REF_MISSING");
    }

    @Test
    void missingRiskActionGuardContextRefCreatesIncompleteValidation() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("riskActionGuardContextRef", null)),
                "RISK_ACTION_GUARD_CONTEXT_REF_MISSING");
    }

    @Test
    void missingWatchlistPoolProofContextRefCreatesIncompleteValidation() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("watchlistPoolProofContextRef", null)),
                "WATCHLIST_POOL_PROOF_CONTEXT_REF_MISSING");
    }

    @Test
    void missingCompletenessScoreCreatesIncompleteValidation() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("sourceBindingCompletenessScore", null)),
                "SOURCE_BINDING_COMPLETENESS_SCORE_MISSING");
    }

    @Test
    void lowCompletenessScoreDoesNotPassSilently() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("sourceBindingCompletenessScore", bd("68"))),
                "SOURCE_BINDING_COMPLETENESS_SCORE_TOO_LOW");

        SourceOwnedCandidateIntegrationSourceBindingDTO degraded = degradedContext();
        forceField(degraded, "sourceBindingCompletenessScore", bd("82"));
        assertThat(validator.validate(degraded).getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED);
    }

    @Test
    void allRequiredSourcesPresentFalseCreatesIncompleteValidation() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("allRequiredSourcesPresent", Boolean.FALSE)),
                "REQUIRED_SOURCE_MISSING");
    }

    @Test
    void allRequiredSourcesTrustedFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("allRequiredSourcesTrusted", Boolean.FALSE)),
                "REQUIRED_SOURCE_UNTRUSTED");
    }

    @Test
    void allRequiredSourcesReviewOnlyFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("allRequiredSourcesReviewOnly", Boolean.FALSE)),
                "REQUIRED_SOURCE_NOT_REVIEW_ONLY");
    }

    @Test
    void allRequiredSourcesNotTradeInstructionFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("allRequiredSourcesNotTradeInstruction", Boolean.FALSE)),
                "REQUIRED_SOURCE_TRADE_INSTRUCTION");
    }

    @Test
    void allRequiredSourcesManualReviewRequiredFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("allRequiredSourcesManualReviewRequired", Boolean.FALSE)),
                "REQUIRED_SOURCE_MANUAL_REVIEW_DISABLED");
    }

    @Test
    void allRequiredSourcesIncompleteSafeFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("allRequiredSourcesIncompleteSafe", Boolean.FALSE)),
                "REQUIRED_SOURCE_NOT_INCOMPLETE_SAFE");
    }

    @Test
    void anySourceBlockedTrueCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("anySourceBlocked", Boolean.TRUE)),
                "UPSTREAM_SOURCE_BLOCKED");
    }

    @Test
    void anySourceIncompleteTrueCreatesIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("anySourceIncomplete", Boolean.TRUE)),
                "UPSTREAM_SOURCE_INCOMPLETE");
    }

    @Test
    void anySourceDegradedTrueRequiresDegradedReason() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("anySourceDegraded", Boolean.TRUE)),
                "DEGRADED_REASON_REQUIRED");

        SourceOwnedCandidateIntegrationSourceBindingDTO degraded = reviewOnlyContext();
        forceField(degraded, "anySourceDegraded", Boolean.TRUE);
        forceField(degraded, "candidateDegradedReason", "SOURCE_BINDING_DEGRADED");
        assertThat(validator.validate(degraded).getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus
                        .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED);
    }

    @Test
    void forbiddenExecutableSemanticCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("candidateBoundaryLabel", "push send")),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(validator.validate(contextWithField("candidateUnavailableReason", "external channel")),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(validator.validate(contextWithField("candidateBlockedReason", "send order")),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(validator.validate(contextWithField("sourceTraceRefs", List.of("execute now"))),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(validator.validate(contextWithField("sourceOwnedTraceRefs", List.of("reverse plan"))),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void untrustedSourceCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("trustedSource", false)),
                "CANDIDATE_INTEGRATION_SOURCE_UNTRUSTED");
    }

    @Test
    void inheritedRiskActionGuardAndWatchlistPoolBoundariesBlockOrIncomplete() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("riskActionGuardStatus", "STAMPEDE")),
                "RISK_ACTION_GUARD_BOUNDARY_BLOCKED");
        assertBlockedFor(validator.validate(contextWithField("riskActionGuardStatus", "WICK_ONLY_REVERSE")),
                "FORBIDDEN_SEMANTIC_DETECTED");
        assertBlockedFor(validator.validate(contextWithField("riskActionGuardStatus",
                        "LIQUIDITY_DEGRADED_ONE_SHOT_EXIT")),
                "RISK_ACTION_GUARD_BOUNDARY_BLOCKED");
        assertBlockedFor(validator.validate(contextWithField("watchlistPoolProofStatus", "DISABLED")),
                "WATCHLIST_POOL_PROOF_BOUNDARY_BLOCKED");
        assertIncompleteFor(validator.validate(contextWithField("watchlistPoolProofStatus", "STALE")),
                "WATCHLIST_POOL_PROOF_STALE");
        assertBlockedFor(validator.validate(contextWithField("candidateBoundaryLabel", "DISPLAY_SLOT_PROOF")),
                "DISPLAY_SLOT_NOT_WATCHLIST_POOL_PROOF");
    }

    @Test
    void validationResultKeepsReviewOnlyAndManualReviewRequired() {
        SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult result =
                validator.validate(reviewOnlyContext());

        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
    }

    @Test
    void validatorClassHasNoSpringMyBatisJpaJacksonLombokAnnotations() throws Exception {
        assertNoAnnotations(SourceOwnedCandidateIntegrationSourceBindingValidator.class);
        assertNoAnnotations(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult.class);
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
    void validatorDoesNotReferenceServiceControllerMapperRepositoryScheduler() throws Exception {
        assertSourceDoesNotContain(List.of(
                "Assembler",
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
    void validatorDoesNotReferenceMarketQuoteHttpOrDataSourceProviders() throws Exception {
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
    void validatorDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
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
                "submitOrder"
        ));
    }

    @Test
    void validatorResultSafeOutputsDoNotContainForbiddenExecutableSemantics() {
        List<SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult> results = List.of(
                validator.validate(incompleteContext()),
                validator.validate(blockedContext()),
                validator.validate(degradedContext()),
                validator.validate(reviewOnlyContext())
        );

        for (SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult result : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(result.getStatus().name());
            outputs.addAll(result.getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    private SourceOwnedCandidateIntegrationSourceBindingDTO incompleteContext() {
        return SourceOwnedCandidateIntegrationSourceBindingDTO.incomplete(
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
                List.of("sourceBindingCompletenessScore"),
                "SOURCE_BINDING_MISSING"
        );
    }

    private SourceOwnedCandidateIntegrationSourceBindingDTO blockedContext() {
        return SourceOwnedCandidateIntegrationSourceBindingDTO.blockedFailClosed(
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
                List.of("SOURCE_BINDING_BLOCKED"),
                "CANDIDATE_BINDING_BLOCKED",
                "SOURCE_BINDING_BLOCKED"
        );
    }

    private SourceOwnedCandidateIntegrationSourceBindingDTO degradedContext() {
        return SourceOwnedCandidateIntegrationSourceBindingDTO.degraded(
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
                "SOURCE_TRACE_REVIEW_ONLY",
                "RUNTIME_KLINE_REVIEW_ONLY",
                "DATA_QUALITY_REVIEW_ONLY",
                "MULTI_TIMEFRAME_DEGRADED",
                "RISK_ACTION_GUARD_REVIEW_ONLY",
                "WATCHLIST_POOL_PROOF_REVIEW_ONLY",
                bd("82"),
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.TRUE,
                "REVIEW_ONLY_CANDIDATE_BOUNDARY",
                "SOURCE_BINDING_DEGRADED",
                "SOURCE_BINDING_DEGRADED",
                List.of("source-owned-trace-ref"),
                List.of(),
                List.of("SOURCE_BINDING_DEGRADED"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "SOURCE_BINDING_DEGRADED",
                Boolean.TRUE
        );
    }

    private SourceOwnedCandidateIntegrationSourceBindingDTO reviewOnlyContext() {
        return SourceOwnedCandidateIntegrationSourceBindingDTO.reviewOnly(
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
                "SOURCE_TRACE_REVIEW_ONLY",
                "RUNTIME_KLINE_REVIEW_ONLY",
                "DATA_QUALITY_REVIEW_ONLY",
                "MULTI_TIMEFRAME_REVIEW_ONLY",
                "RISK_ACTION_GUARD_REVIEW_ONLY",
                "WATCHLIST_POOL_PROOF_REVIEW_ONLY",
                bd("96"),
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                "REVIEW_ONLY_CANDIDATE_BOUNDARY",
                null,
                null,
                null,
                List.of("source-owned-trace-ref"),
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                Boolean.TRUE
        );
    }

    private SourceOwnedCandidateIntegrationSourceBindingDTO contextWithField(
            String fieldName,
            Object value
    ) throws Exception {
        SourceOwnedCandidateIntegrationSourceBindingDTO context = reviewOnlyContext();
        forceField(context, fieldName, value);
        return context;
    }

    private static void forceField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertIncompleteFor(
            SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertBlockedFor(
            SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/validator/point/"
                        + "SourceOwnedCandidateIntegrationSourceBindingValidator.java"
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
