package org.example.trademodel.dto.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MultiTimeframeContextSourceBindingDTOTest {

    @Test
    void factoriesKeepSafetyFlagsForcedTrue() {
        List<MultiTimeframeContextSourceBindingDTO> contexts = List.of(
                incompleteContext(),
                blockedContext(),
                degradedContext(),
                reviewOnlyContext()
        );

        for (MultiTimeframeContextSourceBindingDTO context : contexts) {
            assertThat(context.isReviewOnly()).isTrue();
            assertThat(context.isNotTradeInstruction()).isTrue();
            assertThat(context.isManualReviewRequired()).isTrue();
            assertThat(context.isIncompleteSafe()).isTrue();
        }
    }

    @Test
    void blockedFailClosedFactorySetsFailClosedTrueAndOtherStatusesDoNot() {
        assertThat(blockedContext().isFailClosed()).isTrue();
        assertThat(incompleteContext().isFailClosed()).isFalse();
        assertThat(degradedContext().isFailClosed()).isFalse();
        assertThat(reviewOnlyContext().isFailClosed()).isFalse();
    }

    @Test
    void incompleteFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> MultiTimeframeContextSourceBindingDTO.incomplete(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of("alignmentScore"),
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockedFailClosedFactoryRequiresBlockedReason() {
        assertThatThrownBy(() -> MultiTimeframeContextSourceBindingDTO.blockedFailClosed(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                List.of("15m", "1h"),
                List.of("HARD_THRESHOLD_BLOCKED"),
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void degradedFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> MultiTimeframeContextSourceBindingDTO.degraded(
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
                List.of(bd("0.6"), bd("0.4")),
                List.of("15m", "1h"),
                List.of(),
                List.of(),
                List.of("4h"),
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
                "",
                Boolean.TRUE
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewOnlyFactoryCarriesExplicitFieldsAndDoesNotCalculateScores() {
        MultiTimeframeContextSourceBindingDTO context = reviewOnlyContext();

        assertThat(context.getAlignmentScore()).isEqualByComparingTo("91");
        assertThat(context.getConflictScore()).isEqualByComparingTo("12");
        assertThat(context.getWeightedAgreementScore()).isEqualByComparingTo("89");
        assertThat(context.getTimeframeScores()).containsExactly(bd("88"), bd("92"));
        assertThat(context.getTimeframeWeights()).containsExactly(bd("0.55"), bd("0.45"));
        assertThat(context.getMissingReason()).isNull();
        assertThat(context.getBlockedReason()).isNull();
    }

    @Test
    void listFieldsAreDefensivelyCopiedAndImmutable() {
        List<String> sourceTraceRefs = new ArrayList<>();
        sourceTraceRefs.add("source-trace-ref");
        List<String> timeframeRefs = new ArrayList<>();
        timeframeRefs.add("15m");

        MultiTimeframeContextSourceBindingDTO context = MultiTimeframeContextSourceBindingDTO.incomplete(
                "mtf-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                sourceTraceRefs,
                "runtime-kline-ref",
                "data-quality-ref",
                timeframeRefs,
                List.of("alignmentScore"),
                "MULTITIMEFRAME_CONTEXT_MISSING"
        );
        sourceTraceRefs.add("mutated-ref");
        timeframeRefs.add("1h");

        assertThat(context.getSourceTraceRefs()).containsExactly("source-trace-ref");
        assertThat(context.getTimeframeRefs()).containsExactly("15m");
        assertThatThrownBy(() -> context.getSourceTraceRefs().add("new-ref"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void enumCoversRequiredStatuses() {
        assertThat(List.of(MultiTimeframeContextSourceBindingDTO.BindingStatus.values()))
                .contains(
                        MultiTimeframeContextSourceBindingDTO.BindingStatus.INCOMPLETE,
                        MultiTimeframeContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED,
                        MultiTimeframeContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_MULTITIMEFRAME_BINDING,
                        MultiTimeframeContextSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_MULTITIMEFRAME_BINDING_DEGRADED
                );
    }

    @Test
    void noSetterBuilderOrFactoryCanDisableSafetyFlags() {
        for (Method method : MultiTimeframeContextSourceBindingDTO.class.getDeclaredMethods()) {
            assertThat(method.getName()).doesNotStartWith("set");
            assertThat(method.getName().toLowerCase()).doesNotContain("builder");
            assertThat(method.getName()).doesNotContain("ReviewOnlyFalse");
            assertThat(method.getName()).doesNotContain("TradeInstructionFalse");
        }
        for (Field field : MultiTimeframeContextSourceBindingDTO.class.getDeclaredFields()) {
            if (field.getName().equals("reviewOnly")
                    || field.getName().equals("notTradeInstruction")
                    || field.getName().equals("manualReviewRequired")
                    || field.getName().equals("incompleteSafe")) {
                assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
            }
        }
    }

    @Test
    void dtoHasNoSpringAnnotations() {
        assertNoAnnotations(MultiTimeframeContextSourceBindingDTO.class);
    }

    @Test
    void dtoDoesNotReferenceServiceControllerMapperRepositoryOrScheduler() throws Exception {
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
    void dtoDoesNotReferenceMarketQuoteHttpOrDataSourceProviders() throws Exception {
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
    void dtoDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
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
                "reversePosition"
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
                List.of("4h"),
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

    private MultiTimeframeContextSourceBindingDTO reviewOnlyContext() {
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

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/dto/point/MultiTimeframeContextSourceBindingDTO.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }
}
