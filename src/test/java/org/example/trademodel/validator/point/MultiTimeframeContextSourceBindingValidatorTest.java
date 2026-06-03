package org.example.trademodel.validator.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.MultiTimeframeContextSourceBindingDTO;
import org.junit.jupiter.api.Test;

class MultiTimeframeContextSourceBindingValidatorTest {

    private final MultiTimeframeContextSourceBindingValidator validator =
            new MultiTimeframeContextSourceBindingValidator();

    @Test
    void nullContextReturnsIncomplete() {
        assertIncompleteFor(validator.validate(null), "MULTITIMEFRAME_CONTEXT_BINDING_MISSING");
    }

    @Test
    void incompleteContextWithMissingReasonReturnsIncomplete() {
        assertIncompleteFor(validator.validate(incompleteContext()), "MULTITIMEFRAME_CONTEXT_MISSING");
    }

    @Test
    void incompleteContextWithoutMissingReasonIsSafelyIncomplete() throws Exception {
        MultiTimeframeContextSourceBindingDTO context = incompleteContext();
        forceField(context, "missingReason", null);

        assertIncompleteFor(validator.validate(context), "MISSING_REASON_REQUIRED");
    }

    @Test
    void blockedContextWithBlockedReasonReturnsBlockedFailClosed() {
        assertBlockedFor(validator.validate(blockedContext()), "MULTITIMEFRAME_CONTEXT_BLOCKED");
    }

    @Test
    void blockedContextWithoutBlockedReasonReturnsBlockedFailClosed() throws Exception {
        MultiTimeframeContextSourceBindingDTO context = blockedContext();
        forceField(context, "blockedReason", null);

        assertBlockedFor(validator.validate(context), "BLOCKED_REASON_REQUIRED");
    }

    @Test
    void completeContextReturnsReviewOnlyMultiTimeframeBinding() {
        MultiTimeframeContextSourceBindingValidator.ValidationResult result = validator.validate(completeContext());

        assertThat(result.getStatus())
                .isEqualTo(
                        MultiTimeframeContextSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_MULTITIMEFRAME_BINDING
                );
        assertThat(result.isValidForReviewOnly()).isTrue();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
    }

    @Test
    void degradedContextWithMissingReasonReturnsReviewOnlyMultiTimeframeBindingDegraded() {
        MultiTimeframeContextSourceBindingValidator.ValidationResult result = validator.validate(degradedContext());

        assertThat(result.getStatus())
                .isEqualTo(
                        MultiTimeframeContextSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
                );
        assertThat(result.getReasons()).containsExactly("WARNING_THRESHOLD_DEGRADED");
    }

    @Test
    void degradedContextWithoutMissingReasonReturnsIncomplete() throws Exception {
        MultiTimeframeContextSourceBindingDTO context = degradedContext();
        forceField(context, "missingReason", null);
        forceField(context, "degradedReasons", List.of());

        assertIncompleteFor(validator.validate(context), "MISSING_REASON_REQUIRED");
    }

    @Test
    void missingMultiTimeframeContextIdReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("multiTimeframeContextId", null)),
                "MULTITIMEFRAME_CONTEXT_ID_MISSING");
    }

    @Test
    void missingSymbolReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("symbol", " ")), "SYMBOL_MISSING");
    }

    @Test
    void missingMarketReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("market", null)), "MARKET_MISSING");
    }

    @Test
    void missingPrimaryTimeframeReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("primaryTimeframe", "")),
                "PRIMARY_TIMEFRAME_MISSING");
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
        assertIncompleteFor(validator.validate(contextWithField("dataQualityContextRef", " ")),
                "DATA_QUALITY_CONTEXT_REF_MISSING");
    }

    @Test
    void missingTimeframeRefsReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("timeframeRefs", List.of())),
                "TIMEFRAME_REFS_MISSING");
    }

    @Test
    void blankTimeframeRefReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("timeframeRefs", List.of("15m", " "))),
                "TIMEFRAME_REF_BLANK");
    }

    @Test
    void missingAlignmentScoreReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("alignmentScore", null)),
                "ALIGNMENT_SCORE_MISSING");
    }

    @Test
    void lowAlignmentScoreWithoutReasonReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("alignmentScore", bd("69"))),
                "ALIGNMENT_SCORE_LOW");
    }

    @Test
    void lowAlignmentScoreWithDegradedReasonReturnsDegraded() throws Exception {
        MultiTimeframeContextSourceBindingDTO context = contextWithField("alignmentScore", bd("69"));
        forceField(context, "degradedReasons", List.of("ALIGNMENT_SCORE_LOW"));

        assertThat(validator.validate(context).getStatus())
                .isEqualTo(
                        MultiTimeframeContextSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
                );
    }

    @Test
    void missingConflictScoreReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("conflictScore", null)),
                "CONFLICT_SCORE_MISSING");
    }

    @Test
    void highConflictScoreWithoutReasonReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("conflictScore", bd("60"))),
                "MISSING_REASON_REQUIRED");
    }

    @Test
    void highConflictScoreWithBlockedReasonReturnsBlockedFailClosed() throws Exception {
        MultiTimeframeContextSourceBindingDTO context = contextWithField("conflictScore", bd("90"));
        forceField(context, "blockedReasons", List.of("HIGH_TIMEFRAME_CONFLICT_BLOCKED"));

        assertBlockedFor(validator.validate(context), "HIGH_TIMEFRAME_CONFLICT_BLOCKED");
    }

    @Test
    void missingWeightedAgreementScoreReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("weightedAgreementScore", null)),
                "WEIGHTED_AGREEMENT_SCORE_MISSING");
    }

    @Test
    void lowWeightedAgreementScoreReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("weightedAgreementScore", bd("69"))),
                "WEIGHTED_AGREEMENT_SCORE_LOW");
    }

    @Test
    void minimumRequiredTimeframesFalseReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("minimumRequiredTimeframesPassed", Boolean.FALSE)),
                "MINIMUM_TIMEFRAMES_NOT_PASSED");
    }

    @Test
    void dataQualityFalseReturnsIncomplete() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("dataQualityPassed", Boolean.FALSE)),
                "DATA_QUALITY_NOT_PASSED");
    }

    @Test
    void hardThresholdFalseReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("hardThresholdPassed", Boolean.FALSE)),
                "HARD_THRESHOLD_BLOCKED");
    }

    @Test
    void warningThresholdFalseWithReasonReturnsDegraded() throws Exception {
        MultiTimeframeContextSourceBindingDTO context = contextWithField("warningThresholdPassed", Boolean.FALSE);
        forceField(context, "degradedReasons", List.of("WARNING_THRESHOLD_DEGRADED"));

        assertThat(validator.validate(context).getStatus())
                .isEqualTo(
                        MultiTimeframeContextSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
                );
    }

    @Test
    void missingTimeframesCannotPassSilently() throws Exception {
        assertIncompleteFor(validator.validate(contextWithField("missingTimeframes", List.of("4h"))),
                "MISSING_TIMEFRAMES_PRESENT");
    }

    @Test
    void staleTimeframesWithReasonReturnDegraded() throws Exception {
        MultiTimeframeContextSourceBindingDTO context = contextWithField("staleTimeframes", List.of("1h"));
        forceField(context, "degradedReasons", List.of("STALE_TIMEFRAMES_PRESENT"));

        assertThat(validator.validate(context).getStatus())
                .isEqualTo(
                        MultiTimeframeContextSourceBindingValidator.ValidationStatus
                                .REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
                );
    }

    @Test
    void untrustedSourceReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("trustedSource", false)),
                "MULTITIMEFRAME_SOURCE_UNTRUSTED");
    }

    @Test
    void safetyFlagFalseReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("reviewOnly", false)), "SAFETY_FLAG_REQUIRED");
    }

    @Test
    void forbiddenExecutableSemanticInReasonReturnsBlockedFailClosed() throws Exception {
        MultiTimeframeContextSourceBindingDTO context = completeContext();
        forceField(context, "degradedReasons", List.of("send order"));

        assertBlockedFor(validator.validate(context), "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void forbiddenExecutableSemanticInSourceTraceRefsReturnsBlockedFailClosed() throws Exception {
        assertBlockedFor(validator.validate(contextWithField("sourceTraceRefs", List.of("open long ref"))),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void validatorResultNormalSafeOutputsDoNotContainForbiddenExecutableSemantics() {
        List<MultiTimeframeContextSourceBindingValidator.ValidationResult> results = List.of(
                validator.validate(incompleteContext()),
                validator.validate(blockedContext()),
                validator.validate(degradedContext()),
                validator.validate(completeContext())
        );

        for (MultiTimeframeContextSourceBindingValidator.ValidationResult result : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(result.getStatus().name());
            outputs.addAll(result.getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    @Test
    void validatorClassHasNoSpringAnnotations() {
        assertNoAnnotations(MultiTimeframeContextSourceBindingValidator.class);
        assertNoAnnotations(MultiTimeframeContextSourceBindingValidator.ValidationResult.class);
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
    void validatorDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
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

    private MultiTimeframeContextSourceBindingDTO incompleteContext() {
        return MultiTimeframeContextSourceBindingDTO.incomplete(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of("alignmentScore"),
                "MULTITIMEFRAME_CONTEXT_MISSING"
        );
    }

    private MultiTimeframeContextSourceBindingDTO blockedContext() {
        return MultiTimeframeContextSourceBindingDTO.blockedFailClosed(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of("HARD_THRESHOLD_BLOCKED"),
                "MULTITIMEFRAME_CONTEXT_BLOCKED"
        );
    }

    private MultiTimeframeContextSourceBindingDTO degradedContext() {
        return MultiTimeframeContextSourceBindingDTO.degraded(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of(bd("88"), bd("86")),
                List.of("UP", "UP"),
                List.of(bd("0.55"), bd("0.45")),
                List.of("15m", "1h"),
                List.of(),
                List.of(),
                List.of(),
                "UP",
                bd("82"),
                bd("30"),
                bd("84"),
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.FALSE,
                List.of(),
                List.of("WARNING_THRESHOLD_DEGRADED"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "MULTITIMEFRAME_CONTEXT_DEGRADED",
                Boolean.TRUE
        );
    }

    private MultiTimeframeContextSourceBindingDTO completeContext() {
        return MultiTimeframeContextSourceBindingDTO.reviewOnly(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of(bd("88"), bd("92")),
                List.of("UP", "UP"),
                List.of(bd("0.55"), bd("0.45")),
                List.of("15m", "1h"),
                List.of(),
                List.of(),
                List.of(),
                "UP",
                bd("91"),
                bd("12"),
                bd("89"),
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                Boolean.TRUE,
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                Boolean.TRUE
        );
    }

    private MultiTimeframeContextSourceBindingDTO contextWithField(String fieldName, Object value) throws Exception {
        MultiTimeframeContextSourceBindingDTO context = completeContext();
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
            MultiTimeframeContextSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus()).isEqualTo(MultiTimeframeContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.isIncomplete()).isTrue();
        assertThat(result.isBlockedFailClosed()).isFalse();
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertBlockedFor(
            MultiTimeframeContextSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus())
                .isEqualTo(MultiTimeframeContextSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
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
                "src/main/java/org/example/trademodel/validator/point/"
                        + "MultiTimeframeContextSourceBindingValidator.java"
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
