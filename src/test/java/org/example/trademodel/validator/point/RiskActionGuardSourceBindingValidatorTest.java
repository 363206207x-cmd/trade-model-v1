package org.example.trademodel.validator.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.RiskActionGuardSourceBindingDTO;
import org.junit.jupiter.api.Test;

class RiskActionGuardSourceBindingValidatorTest {

    private final RiskActionGuardSourceBindingValidator validator =
            new RiskActionGuardSourceBindingValidator();

    @Test
    void nullContextReturnsIncomplete() {
        assertIncompleteFor(validator.validate(null), "RISK_ACTION_GUARD_BINDING_MISSING");
    }

    @Test
    void incompleteContextWithMissingReasonReturnsIncomplete() {
        assertIncompleteFor(validator.validate(incompleteContext()), "RISK_ACTION_GUARD_MISSING");
    }

    @Test
    void incompleteContextWithoutMissingReasonIsSafelyIncomplete() throws Exception {
        RiskActionGuardSourceBindingDTO context = incompleteContext();
        forceField(context, "missingReason", null);

        assertIncompleteFor(validator.validate(context), "MISSING_REASON_REQUIRED");
    }

    @Test
    void blockedContextWithBlockedReasonReturnsBlockedFailClosed() {
        assertBlockedFor(validator.validate(blockedContext()), "RISK_ACTION_GUARD_BLOCKED");
    }

    @Test
    void blockedContextWithoutBlockedReasonReturnsBlockedFailClosed() throws Exception {
        RiskActionGuardSourceBindingDTO context = blockedContext();
        forceField(context, "blockedReason", null);

        assertBlockedFor(validator.validate(context), "BLOCKED_REASON_REQUIRED");
    }

    @Test
    void completeContextReturnsReviewOnlyRiskActionGuardBinding() {
        RiskActionGuardSourceBindingValidator.ValidationResult result = validator.validate(completeContext());

        assertThat(result.getStatus())
                .isEqualTo(
                        RiskActionGuardSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_RISK_ACTION_GUARD_BINDING
                );
        assertThat(result.isValidForReviewOnly()).isTrue();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
    }

    @Test
    void degradedContextWithMissingReasonReturnsReviewOnlyRiskActionGuardBindingDegraded() {
        RiskActionGuardSourceBindingValidator.ValidationResult result = validator.validate(degradedContext());

        assertThat(result.getStatus())
                .isEqualTo(
                        RiskActionGuardSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
                );
        assertThat(result.getReasons()).containsExactly("RISK_ACTION_GUARD_DEGRADED");
    }

    @Test
    void degradedContextWithoutMissingReasonReturnsIncomplete() throws Exception {
        RiskActionGuardSourceBindingDTO context = degradedContext();
        forceField(context, "missingReason", null);
        forceField(context, "degradedReasons", List.of());

        assertIncompleteFor(validator.validate(context), "MISSING_REASON_REQUIRED");
    }

    @Test
    void missingRiskActionGuardContextIdReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("riskActionGuardContextId", null)),
                "RISK_ACTION_GUARD_CONTEXT_ID_MISSING");
    }

    @Test
    void missingSourceTraceRefsReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("sourceTraceRefs", List.of())),
                "SOURCE_TRACE_REFS_MISSING");
    }

    @Test
    void blankSourceTraceRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("sourceTraceRefs", List.of("source-ref", " "))),
                "SOURCE_TRACE_REF_BLANK");
    }

    @Test
    void missingRuntimeKlineContextRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("runtimeKlineContextRef", null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
    }

    @Test
    void missingDataQualityContextRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("dataQualityContextRef", "")),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
    }

    @Test
    void missingMultiTimeframeContextRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("multiTimeframeContextRef", " ")),
                "MULTITIMEFRAME_CONTEXT_REF_MISSING");
    }

    @Test
    void missingPrimaryTimeframeReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("primaryTimeframe", null)),
                "PRIMARY_TIMEFRAME_MISSING");
    }

    @Test
    void missingRiskLevelReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("riskLevel", null)), "RISK_LEVEL_MISSING");
    }

    @Test
    void missingRiskScoreReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("riskScore", null)), "RISK_SCORE_MISSING");
    }

    @Test
    void missingActionRiskScoreReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("actionRiskScore", null)),
                "ACTION_RISK_SCORE_MISSING");
    }

    @Test
    void missingLiquidityStateReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("liquidityState", null)),
                "LIQUIDITY_STATE_MISSING");
    }

    @Test
    void missingBooleanRiskStateReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("stampedeDetected", null)),
                "STAMPEDE_STATUS_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("wickOnlyDetected", null)),
                "WICK_ONLY_STATUS_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("multiTimeframeConfirmed", null)),
                "MULTITIMEFRAME_CONFIRMATION_MISSING");
        assertIncompleteFor(validator.validate(contextWithField("strongReversalClaimed", null)),
                "STRONG_REVERSAL_STATUS_MISSING");
    }

    @Test
    void stampedeDetectedReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("stampedeDetected", Boolean.TRUE)),
                "STAMPEDE_BLOCKED");
    }

    @Test
    void wickOnlyWithReverseSemanticReturnsBlockedFailClosed() throws Exception {
        RiskActionGuardSourceBindingDTO context = contextWithField("wickOnlyDetected", Boolean.TRUE);
        forceField(context, "proposedActionLabel", "reverse bias");

        assertBlockedFor(validator.validate(context), "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void strongReversalWithoutMultiTimeframeConfirmationReturnsIncomplete() throws Exception {
        RiskActionGuardSourceBindingDTO context = contextWithField("strongReversalClaimed", Boolean.TRUE);
        forceField(context, "multiTimeframeConfirmed", Boolean.FALSE);

        assertIncompleteFor(validator.validate(context), "STRONG_REVERSAL_UNCONFIRMED");
    }

    @Test
    void liquidityDegradedWithMarketCloseSemanticReturnsBlockedFailClosed() throws Exception {
        RiskActionGuardSourceBindingDTO context = contextWithField("liquidityDegraded", Boolean.TRUE);
        forceField(context, "proposedActionLabel", "market close");

        assertBlockedFor(validator.validate(context), "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void highRiskWithNormalLiquidityAndReasonReturnsDegraded() throws Exception {
        RiskActionGuardSourceBindingDTO context = contextWithField("riskScore", bd("85"));
        forceField(context, "riskLevel", RiskActionGuardSourceBindingDTO.RiskLevel.HIGH);
        forceField(context, "degradedReasons", List.of("HIGH_RISK_REVIEW_ONLY"));

        assertThat(validator.validate(context).getStatus())
                .isEqualTo(
                        RiskActionGuardSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
                );
    }

    @Test
    void highRiskWithoutReasonReturnsIncomplete() throws Exception {
        RiskActionGuardSourceBindingDTO context = contextWithField("riskScore", bd("85"));
        forceField(context, "riskLevel", RiskActionGuardSourceBindingDTO.RiskLevel.HIGH);

        assertIncompleteFor(validator.validate(context), "WARNING_RISK_REQUIRES_DEGRADED_REASON");
    }

    @Test
    void untrustedSourceReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("trustedSource", false)),
                "RISK_ACTION_GUARD_SOURCE_UNTRUSTED");
    }

    @Test
    void safetyFlagFalseReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("reviewOnly", false)), "SAFETY_FLAG_REQUIRED");
    }

    @Test
    void forbiddenExecutableSemanticInSourceTraceRefsReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("sourceTraceRefs", List.of("send order"))),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void forbiddenExecutableSemanticInProposedActionLabelReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("proposedActionLabel", "open position")),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void validatorResultNormalSafeOutputsDoNotContainForbiddenExecutableSemantics() {
        List<RiskActionGuardSourceBindingValidator.ValidationResult> results = List.of(
                validator.validate(incompleteContext()),
                validator.validate(blockedContext()),
                validator.validate(degradedContext()),
                validator.validate(completeContext())
        );

        for (RiskActionGuardSourceBindingValidator.ValidationResult result : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(result.getStatus().name());
            outputs.addAll(result.getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    @Test
    void validatorClassHasNoSpringAnnotations() {
        assertNoAnnotations(RiskActionGuardSourceBindingValidator.class);
        assertNoAnnotations(RiskActionGuardSourceBindingValidator.ValidationResult.class);
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
                "submitOrder"
        ));
    }

    @Test
    void validatorSourceDoesNotContainDirectForbiddenRuntimeMethodTokens() throws Exception {
        assertSourceDoesNotContain(List.of(
                "placeOrder",
                "createOrder",
                "closePosition",
                "reversePosition",
                "openPosition",
                "submitOrder"
        ));
    }

    private RiskActionGuardSourceBindingDTO incompleteContext() {
        return RiskActionGuardSourceBindingDTO.incomplete(
                "rag-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                List.of("riskScore"),
                "RISK_ACTION_GUARD_MISSING"
        );
    }

    private RiskActionGuardSourceBindingDTO blockedContext() {
        return RiskActionGuardSourceBindingDTO.blockedFailClosed(
                "rag-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                List.of("ACTION_BLOCKED"),
                List.of("STAMPEDE_BLOCKED"),
                "RISK_ACTION_GUARD_BLOCKED"
        );
    }

    private RiskActionGuardSourceBindingDTO degradedContext() {
        return RiskActionGuardSourceBindingDTO.degraded(
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
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "RISK_ACTION_GUARD_DEGRADED",
                Boolean.TRUE
        );
    }

    private RiskActionGuardSourceBindingDTO completeContext() {
        return RiskActionGuardSourceBindingDTO.reviewOnly(
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
                Boolean.TRUE
        );
    }

    private RiskActionGuardSourceBindingDTO contextWithField(String fieldName, Object value) throws Exception {
        RiskActionGuardSourceBindingDTO context = completeContext();
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
            RiskActionGuardSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus()).isEqualTo(RiskActionGuardSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.isIncomplete()).isTrue();
        assertThat(result.isBlockedFailClosed()).isFalse();
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertBlockedFor(
            RiskActionGuardSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus())
                .isEqualTo(RiskActionGuardSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(result.isBlockedFailClosed()).isTrue();
        assertThat(result.isIncomplete()).isFalse();
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/validator/point/RiskActionGuardSourceBindingValidator.java"
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
}
