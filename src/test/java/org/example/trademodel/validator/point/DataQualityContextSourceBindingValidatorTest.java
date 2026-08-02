package org.example.trademodel.validator.point;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.point.DataQualityContextSourceBindingDTO;
import org.junit.jupiter.api.Test;

class DataQualityContextSourceBindingValidatorTest {

    private final DataQualityContextSourceBindingValidator validator =
            new DataQualityContextSourceBindingValidator();

    @Test
    void nullContextReturnsIncomplete() {
        assertIncompleteFor(validator.validate(null), "DATA_QUALITY_CONTEXT_BINDING_MISSING");
    }

    @Test
    void incompleteWithMissingReasonReturnsIncomplete() {
        assertIncompleteFor(validator.validate(incompleteContext()), "DATA_QUALITY_MISSING");
    }

    @Test
    void blockedWithBlockedReasonReturnsBlockedFailClosed() {
        assertBlockedFor(validator.validate(blockedContext()), "DATA_QUALITY_BLOCKED");
    }

    @Test
    void completeHighQualityContextReturnsReviewOnlyDataQualityBinding() {
        DataQualityContextSourceBindingValidator.ValidationResult result = validator.validate(completeContext());

        assertThat(result.getStatus())
                .isEqualTo(DataQualityContextSourceBindingValidator.ValidationStatus.REVIEW_ONLY_DATA_QUALITY_BINDING);
        assertThat(result.isValidForReviewOnly()).isTrue();
        assertThat(result.isReviewOnly()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isManualReviewRequired()).isTrue();
    }

    @Test
    void scoreMissingReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithScore(null)), "DATA_QUALITY_SCORE_MISSING");
    }

    @Test
    void scoreBelowSeventyReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithScore(bd("69.99"))), "DATA_QUALITY_SCORE_LOW");
    }

    @Test
    void scoreOutsideTheValidRangeFailsClosed() {
        assertIncompleteFor(validator.validate(contextWithScore(bd("100.01"))), "DATA_QUALITY_SCORE_LOW");
    }

    @Test
    void scoreSeventyToEightyFourWithDegradedReasonReturnsDegraded() {
        DataQualityContextSourceBindingValidator.ValidationResult result =
                validator.validate(degradedContextWithScore(bd("80")));

        assertThat(result.getStatus()).isEqualTo(
                DataQualityContextSourceBindingValidator.ValidationStatus.REVIEW_ONLY_DATA_QUALITY_BINDING_DEGRADED
        );
    }

    @Test
    void scoreSeventyToEightyFourWithoutDegradedReasonReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithScore(bd("80"))), "MISSING_REASON_REQUIRED");
    }

    @Test
    void scoreAtLeastEightyFiveReturnsReviewOnly() {
        assertThat(validator.validate(contextWithScore(bd("85"))).getStatus())
                .isEqualTo(DataQualityContextSourceBindingValidator.ValidationStatus.REVIEW_ONLY_DATA_QUALITY_BINDING);
    }

    @Test
    void hardThresholdFalseReturnsBlockedFailClosed() {
        assertBlockedFor(validator.validate(contextWithHardThreshold(Boolean.FALSE)), "HARD_THRESHOLD_BLOCKED");
    }

    @Test
    void warningThresholdFalseWithoutReasonReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithWarningThreshold(Boolean.FALSE)),
                "WARNING_THRESHOLD_DEGRADED");
    }

    @Test
    void missingSourceTraceRefsReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithSourceTraceRefs(List.of())), "SOURCE_TRACE_REFS_MISSING");
    }

    @Test
    void blankSourceTraceRefReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithSourceTraceRefs(List.of("source-trace-ref", " "))),
                "SOURCE_TRACE_REF_BLANK");
    }

    @Test
    void missingRuntimeKlineContextRefReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithRuntimeKlineContextRef(null)),
                "RUNTIME_KLINE_CONTEXT_REF_MISSING");
    }

    @Test
    void missingCompletenessScoreReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithSourceTraceCompleteness(null)),
                "SOURCE_TRACE_COMPLETENESS_MISSING");
    }

    @Test
    void lowCompletenessScoreReturnsIncomplete() {
        assertIncompleteFor(validator.validate(contextWithSourceTraceCompleteness(bd("69"))),
                "COMPLETENESS_SCORE_LOW");
    }

    @Test
    void untrustedSourceReturnsBlockedFailClosed() {
        assertBlockedFor(validator.validate(contextWithTrustedSource(Boolean.FALSE)), "DATA_QUALITY_SOURCE_UNTRUSTED");
    }

    @Test
    void safetyFlagFalseReturnsBlockedFailClosed() throws Exception {
        DataQualityContextSourceBindingDTO context = completeContext();
        forceField(context, "reviewOnly", Boolean.FALSE);

        assertBlockedFor(validator.validate(context), "SAFETY_FLAG_REQUIRED");
    }

    @Test
    void forbiddenExecutableSemanticReturnsBlockedFailClosed() {
        assertBlockedFor(validator.validate(degradedContextWithMissingReason("send order now")),
                "FORBIDDEN_SEMANTIC_DETECTED");
    }

    @Test
    void validatorNormalSafeOutputsDoNotContainForbiddenExecutableSemantics() {
        List<DataQualityContextSourceBindingValidator.ValidationResult> results = List.of(
                validator.validate(incompleteContext()),
                validator.validate(blockedContext()),
                validator.validate(degradedContext()),
                validator.validate(completeContext())
        );

        for (DataQualityContextSourceBindingValidator.ValidationResult result : results) {
            List<String> outputs = new ArrayList<>();
            outputs.add(result.getStatus().name());
            outputs.addAll(result.getReasons());
            assertNoForbiddenExecutableSemantics(outputs);
        }
    }

    @Test
    void validatorClassHasNoSpringAnnotations() {
        assertNoAnnotations(DataQualityContextSourceBindingValidator.class);
        assertNoAnnotations(DataQualityContextSourceBindingValidator.ValidationResult.class);
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

    private DataQualityContextSourceBindingDTO incompleteContext() {
        return DataQualityContextSourceBindingDTO.incomplete(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                List.of("dataQualityScore"),
                "DATA_QUALITY_MISSING"
        );
    }

    private DataQualityContextSourceBindingDTO blockedContext() {
        return DataQualityContextSourceBindingDTO.blockedFailClosed(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                List.of("HARD_THRESHOLD_BLOCKED"),
                "DATA_QUALITY_BLOCKED"
        );
    }

    private DataQualityContextSourceBindingDTO degradedContext() {
        return degradedContextWithScore(bd("80"));
    }

    private DataQualityContextSourceBindingDTO degradedContextWithScore(BigDecimal score) {
        return DataQualityContextSourceBindingDTO.degraded(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                score,
                DataQualityContextSourceBindingDTO.DataQualityGrade.MEDIUM,
                Boolean.TRUE,
                Boolean.FALSE,
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                List.of(),
                List.of("QUALITY_DEGRADED"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "DATA_QUALITY_DEGRADED",
                Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO degradedContextWithMissingReason(String missingReason) {
        return DataQualityContextSourceBindingDTO.degraded(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                bd("80"),
                DataQualityContextSourceBindingDTO.DataQualityGrade.MEDIUM,
                Boolean.TRUE,
                Boolean.FALSE,
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                bd("90"),
                List.of(),
                List.of("QUALITY_DEGRADED"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                missingReason,
                Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO completeContext() {
        return DataQualityContextSourceBindingDTO.reviewOnly(
                "dq-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                bd("92"),
                DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH,
                Boolean.TRUE,
                Boolean.TRUE,
                bd("91"),
                bd("93"),
                bd("94"),
                bd("95"),
                bd("90"),
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO contextWithScore(BigDecimal score) {
        return DataQualityContextSourceBindingDTO.reviewOnly(
                "dq-1", "BTCUSDT", "SPOT", "15m", List.of("source-trace-ref"), "runtime-kline-ref",
                score, DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH, Boolean.TRUE, Boolean.TRUE,
                bd("91"), bd("93"), bd("94"), bd("95"), bd("90"), List.of(), List.of(), List.of(),
                "2026-06-03T00:00:00Z", "2026-06-03T00:01:00Z", Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO contextWithHardThreshold(Boolean value) {
        return DataQualityContextSourceBindingDTO.reviewOnly(
                "dq-1", "BTCUSDT", "SPOT", "15m", List.of("source-trace-ref"), "runtime-kline-ref",
                bd("92"), DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH, value, Boolean.TRUE,
                bd("91"), bd("93"), bd("94"), bd("95"), bd("90"), List.of(), List.of(), List.of(),
                "2026-06-03T00:00:00Z", "2026-06-03T00:01:00Z", Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO contextWithWarningThreshold(Boolean value) {
        return DataQualityContextSourceBindingDTO.reviewOnly(
                "dq-1", "BTCUSDT", "SPOT", "15m", List.of("source-trace-ref"), "runtime-kline-ref",
                bd("92"), DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH, Boolean.TRUE, value,
                bd("91"), bd("93"), bd("94"), bd("95"), bd("90"), List.of(), List.of(), List.of(),
                "2026-06-03T00:00:00Z", "2026-06-03T00:01:00Z", Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO contextWithSourceTraceRefs(List<String> refs) {
        return DataQualityContextSourceBindingDTO.reviewOnly(
                "dq-1", "BTCUSDT", "SPOT", "15m", refs, "runtime-kline-ref",
                bd("92"), DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH, Boolean.TRUE, Boolean.TRUE,
                bd("91"), bd("93"), bd("94"), bd("95"), bd("90"), List.of(), List.of(), List.of(),
                "2026-06-03T00:00:00Z", "2026-06-03T00:01:00Z", Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO contextWithRuntimeKlineContextRef(String ref) {
        return DataQualityContextSourceBindingDTO.reviewOnly(
                "dq-1", "BTCUSDT", "SPOT", "15m", List.of("source-trace-ref"), ref,
                bd("92"), DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH, Boolean.TRUE, Boolean.TRUE,
                bd("91"), bd("93"), bd("94"), bd("95"), bd("90"), List.of(), List.of(), List.of(),
                "2026-06-03T00:00:00Z", "2026-06-03T00:01:00Z", Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO contextWithSourceTraceCompleteness(BigDecimal score) {
        return DataQualityContextSourceBindingDTO.reviewOnly(
                "dq-1", "BTCUSDT", "SPOT", "15m", List.of("source-trace-ref"), "runtime-kline-ref",
                bd("92"), DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH, Boolean.TRUE, Boolean.TRUE,
                score, bd("93"), bd("94"), bd("95"), bd("90"), List.of(), List.of(), List.of(),
                "2026-06-03T00:00:00Z", "2026-06-03T00:01:00Z", Boolean.TRUE
        );
    }

    private DataQualityContextSourceBindingDTO contextWithTrustedSource(Boolean trustedSource) {
        return DataQualityContextSourceBindingDTO.reviewOnly(
                "dq-1", "BTCUSDT", "SPOT", "15m", List.of("source-trace-ref"), "runtime-kline-ref",
                bd("92"), DataQualityContextSourceBindingDTO.DataQualityGrade.HIGH, Boolean.TRUE, Boolean.TRUE,
                bd("91"), bd("93"), bd("94"), bd("95"), bd("90"), List.of(), List.of(), List.of(),
                "2026-06-03T00:00:00Z", "2026-06-03T00:01:00Z", trustedSource
        );
    }

    private static void assertIncompleteFor(
            DataQualityContextSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus()).isEqualTo(DataQualityContextSourceBindingValidator.ValidationStatus.INCOMPLETE);
        assertThat(result.isIncomplete()).isTrue();
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertBlockedFor(
            DataQualityContextSourceBindingValidator.ValidationResult result,
            String reason
    ) {
        assertThat(result.getStatus())
                .isEqualTo(DataQualityContextSourceBindingValidator.ValidationStatus.BLOCKED_FAIL_CLOSED);
        assertThat(result.isBlockedFailClosed()).isTrue();
        assertThat(result.getReasons()).contains(reason);
    }

    private static void assertNoForbiddenExecutableSemantics(List<String> outputs) {
        List<String> forbidden = List.of("buy", "sell", "long", "short", "order", "execute", "auto-trade");
        for (String output : outputs) {
            String normalized = output == null ? "" : output.toLowerCase();
            for (String forbiddenWord : forbidden) {
                assertThat(normalized).doesNotContain(forbiddenWord);
            }
        }
    }

    private static void forceField(Object target, String fieldName, Object value) throws Exception {
        Field field = DataQualityContextSourceBindingDTO.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
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
                "src/main/java/org/example/trademodel/validator/point/DataQualityContextSourceBindingValidator.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }
}
