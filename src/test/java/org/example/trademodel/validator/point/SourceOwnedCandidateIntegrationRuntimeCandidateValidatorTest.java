package org.example.trademodel.validator.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.SourceOwnedCandidateIntegrationRuntimeCandidateDTO;
import org.junit.jupiter.api.Test;

class SourceOwnedCandidateIntegrationRuntimeCandidateValidatorTest {

    private final SourceOwnedCandidateIntegrationRuntimeCandidateValidator validator =
            new SourceOwnedCandidateIntegrationRuntimeCandidateValidator();

    @Test
    void nullContextCreatesIncompleteValidation() {
        assertIncompleteFor(validator.validate(null), "RUNTIME_CANDIDATE_CONTEXT_MISSING");
    }

    @Test
    void missingRuntimeStatusCreatesIncompleteValidation() throws Exception {
        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context = reviewOnlyContext();
        forceField(context, "candidateRuntimeStatus", null);

        assertIncompleteFor(validator.validate(context), "RUNTIME_CANDIDATE_STATUS_MISSING");
    }

    @Test
    void incompleteContextKeepsIncomplete() {
        assertIncompleteFor(validator.validate(incompleteContext()), "RUNTIME_CANDIDATE_UNAVAILABLE");
    }

    @Test
    void blockedFailClosedContextKeepsBlockedFailClosed() {
        assertBlocked(validator.validate(blockedContext()));
    }

    @Test
    void degradedContextCreatesReviewOnlyDegradedValidation() {
        SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult result =
                validator.validate(degradedContext());

        assertThat(result.getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED);
        assertThat(result.isDegraded()).isTrue();
    }

    @Test
    void reviewOnlyContextCreatesReviewOnlyValidValidation() {
        SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult result =
                validator.validate(reviewOnlyContext());

        assertThat(result.getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .REVIEW_ONLY_RUNTIME_CANDIDATE_VALID);
        assertThat(result.isValidForReviewOnly()).isTrue();
    }

    @Test
    void safetyFlagFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("reviewOnly", false), "SAFETY_FLAG_REQUIRED");
        assertBlockedFor(contextWithField("notTradeInstruction", false), "SAFETY_FLAG_REQUIRED");
        assertBlockedFor(contextWithField("manualReviewRequired", false), "SAFETY_FLAG_REQUIRED");
        assertBlockedFor(contextWithField("incompleteSafe", false), "SAFETY_FLAG_REQUIRED");
    }

    @Test
    void failClosedTrueOnNonBlockedStatusCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("failClosed", true), "FAIL_CLOSED_STATUS_MISMATCH");
    }

    @Test
    void missingSourceBindingRefCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("sourceOwnedCandidateIntegrationSourceBindingRef", null),
                "SOURCE_BINDING_REF_MISSING");
    }

    @Test
    void missingValidationStatusCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("sourceOwnedCandidateIntegrationValidationStatus", null),
                "SOURCE_BINDING_VALIDATION_STATUS_MISSING");
    }

    @Test
    void missingSourceTraceRefsCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("sourceTraceRefs", List.of()), "SOURCE_TRACE_REFS_MISSING");
        assertIncompleteFor(contextWithField("sourceTraceRefs", List.of(" ")), "SOURCE_TRACE_REF_BLANK");
    }

    @Test
    void missingRuntimeKlineContextRefCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("runtimeKlineContextRef", null),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
    }

    @Test
    void missingDataQualityContextRefCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("dataQualityContextRef", null),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
    }

    @Test
    void missingMultiTimeframeContextRefCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("multiTimeframeContextRef", null),
                "MULTITIMEFRAME_CONTEXT_REF_MISSING");
    }

    @Test
    void missingRiskActionGuardContextRefCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("riskActionGuardContextRef", null),
                "RISK_ACTION_GUARD_CONTEXT_REF_MISSING");
    }

    @Test
    void missingWatchlistPoolProofContextRefCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("watchlistPoolProofContextRef", null),
                "WATCHLIST_POOL_PROOF_CONTEXT_REF_MISSING");
    }

    @Test
    void missingObservedAtCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("observedAt", null), "OBSERVED_AT_MISSING");
    }

    @Test
    void missingCompletenessScoreCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("sourceBindingCompletenessScore", null),
                "SOURCE_BINDING_COMPLETENESS_SCORE_MISSING");
    }

    @Test
    void lowCompletenessScoreDoesNotPassSilently() throws Exception {
        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context =
                contextWithField("sourceBindingCompletenessScore", bd("64"));

        SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult result =
                validator.validate(context);

        assertThat(result.getStatus())
                .isNotEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .REVIEW_ONLY_RUNTIME_CANDIDATE_VALID);
        assertThat(result.getReasons()).contains("SOURCE_BINDING_COMPLETENESS_SCORE_TOO_LOW");
    }

    @Test
    void anySourceIncompleteCreatesIncomplete() throws Exception {
        assertIncompleteFor(contextWithField("anySourceIncomplete", Boolean.TRUE),
                "UPSTREAM_SOURCE_INCOMPLETE");
    }

    @Test
    void anySourceBlockedCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("anySourceBlocked", Boolean.TRUE),
                "UPSTREAM_SOURCE_BLOCKED");
    }

    @Test
    void allRequiredSourcesTrustedFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("allRequiredSourcesTrusted", Boolean.FALSE),
                "REQUIRED_SOURCE_UNTRUSTED");
    }

    @Test
    void allRequiredSourcesReviewOnlyFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("allRequiredSourcesReviewOnly", Boolean.FALSE),
                "REQUIRED_SOURCE_NOT_REVIEW_ONLY");
    }

    @Test
    void allRequiredSourcesNotTradeInstructionFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("allRequiredSourcesNotTradeInstruction", Boolean.FALSE),
                "REQUIRED_SOURCE_TRADE_INSTRUCTION");
    }

    @Test
    void allRequiredSourcesManualReviewRequiredFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("allRequiredSourcesManualReviewRequired", Boolean.FALSE),
                "REQUIRED_SOURCE_MANUAL_REVIEW_DISABLED");
    }

    @Test
    void allRequiredSourcesIncompleteSafeFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("allRequiredSourcesIncompleteSafe", Boolean.FALSE),
                "REQUIRED_SOURCE_NOT_INCOMPLETE_SAFE");
    }

    @Test
    void riskActionGuardBlockedCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("riskActionGuardBlocked", Boolean.TRUE),
                "RISK_ACTION_GUARD_BLOCKED");
    }

    @Test
    void riskActionGuardStampedeCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("riskActionGuardStampede", Boolean.TRUE),
                "RISK_ACTION_GUARD_STAMPEDE");
    }

    @Test
    void watchlistPoolMemberFalseCreatesBlockedFailClosed() throws Exception {
        assertBlockedFor(contextWithField("watchlistPoolMember", Boolean.FALSE),
                "WATCHLIST_POOL_NOT_MEMBER");
    }

    @Test
    void watchlistPoolProofStaleCreatesIncompleteOrBlockedButDoesNotPass() throws Exception {
        SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult result =
                validator.validate(contextWithField("watchlistPoolProofFresh", Boolean.FALSE));

        assertThat(result.getStatus())
                .isNotEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .REVIEW_ONLY_RUNTIME_CANDIDATE_VALID);
        assertThat(result.getReasons()).contains("WATCHLIST_POOL_PROOF_STALE");
    }

    @Test
    void degradedStatusRequiresDegradedReason() throws Exception {
        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context = degradedContext();
        forceField(context, "candidateDegradedReason", null);
        forceField(context, "degradedReasons", List.of());

        assertIncompleteFor(validator.validate(context), "DEGRADED_REASON_REQUIRED");
    }

    @Test
    void forbiddenExecutableSemanticCreatesBlockedFailClosed() throws Exception {
        assertIncompleteFor(contextWithField("candidateUnavailableReason", "latest price only"),
                "UNSUPPORTED_RUNTIME_INPUT_ONLY");
        assertBlockedFor(contextWithField("candidateBlockedReason", "market buy"),
                "FORBIDDEN_RUNTIME_SEMANTIC_DETECTED");
        assertBlockedFor(contextWithField("candidateDegradedReason", "finalDirection"),
                "FORBIDDEN_RUNTIME_SEMANTIC_DETECTED");
        assertBlockedFor(contextWithField("sourceTraceRefs", List.of("placeOrder")),
                "FORBIDDEN_RUNTIME_SEMANTIC_DETECTED");
        assertBlockedFor(contextWithField("blockedReasons", List.of("DISPLAY_SLOT_PROOF")),
                "DISPLAY_SLOT_NOT_WATCHLIST_POOL_PROOF");
    }

    @Test
    void validationResultKeepsReviewOnlyAndManualReviewRequired() {
        SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult result =
                validator.validate(reviewOnlyContext());

        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isIncompleteSafe()).isTrue();
        assertThat(result.isFailClosed()).isFalse();
        assertThat(result.getReasons()).isUnmodifiable();
    }

    @Test
    void validatorClassHasNoSpringMyBatisJpaJacksonLombokAnnotations() throws Exception {
        assertNoAnnotations(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.class);
        assertNoAnnotations(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult.class);
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

    private SourceOwnedCandidateIntegrationRuntimeCandidateDTO incompleteContext() {
        return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.incomplete(
                "runtime-candidate-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "source-binding-ref",
                "INCOMPLETE",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                List.of("sourceBindingCompletenessScore"),
                "RUNTIME_CANDIDATE_UNAVAILABLE",
                null
        );
    }

    private SourceOwnedCandidateIntegrationRuntimeCandidateDTO blockedContext() {
        return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.blockedFailClosed(
                "runtime-candidate-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "source-binding-ref",
                "REVIEW_ONLY",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                List.of("RUNTIME_CANDIDATE_BLOCKED"),
                "RUNTIME_CANDIDATE_BLOCKED",
                null
        );
    }

    private SourceOwnedCandidateIntegrationRuntimeCandidateDTO degradedContext() {
        return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.degraded(
                "runtime-candidate-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "source-binding-ref",
                "REVIEW_ONLY_DEGRADED",
                List.of("SOURCE_BINDING_DEGRADED"),
                bd("82"),
                "SOURCE_BINDING_DEGRADED",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
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
                "RUNTIME_CANDIDATE_DEGRADED",
                "RUNTIME_CANDIDATE_DEGRADED",
                List.of(),
                List.of("RUNTIME_CANDIDATE_DEGRADED"),
                "2026-06-04T00:00:00Z",
                "2026-06-04T00:01:00Z"
        );
    }

    private SourceOwnedCandidateIntegrationRuntimeCandidateDTO reviewOnlyContext() {
        return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.reviewOnly(
                "runtime-candidate-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "source-binding-ref",
                "REVIEW_ONLY_RUNTIME",
                List.of("SOURCE_BINDING_READY"),
                bd("96"),
                "SOURCE_BINDING_READY",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
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
                List.of(),
                List.of(),
                List.of(),
                "2026-06-04T00:00:00Z",
                "2026-06-04T00:01:00Z"
        );
    }

    private SourceOwnedCandidateIntegrationRuntimeCandidateDTO contextWithField(
            String fieldName,
            Object value
    ) throws Exception {
        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context = reviewOnlyContext();
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

    private void assertIncompleteFor(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context,
            String reason
    ) {
        assertIncompleteFor(validator.validate(context), reason);
    }

    private static void assertIncompleteFor(
            SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.getReasons()).contains(reason);
    }

    private void assertBlockedFor(
            SourceOwnedCandidateIntegrationRuntimeCandidateDTO context,
            String reason
    ) {
        SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult result =
                validator.validate(context);
        assertBlocked(result);
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertBlocked(
            SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationResult result
    ) {
        assertThat(result.getStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateValidator.ValidationStatus
                        .BLOCKED_FAIL_CLOSED);
        assertThat(result.isBlockedFailClosed()).isTrue();
        assertThat(result.isFailClosed()).isTrue();
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/validator/point/"
                        + "SourceOwnedCandidateIntegrationRuntimeCandidateValidator.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }
}
