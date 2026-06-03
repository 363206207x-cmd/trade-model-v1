package org.example.trademodel.dto.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuntimeKlineContextSourceBindingDTOTest {

    @Test
    void incompleteFactoryKeepsReviewOnlyTrue() {
        assertThat(incompleteBinding().isReviewOnly()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsNotTradeInstructionTrue() {
        assertThat(incompleteBinding().isNotTradeInstruction()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsManualReviewRequiredTrue() {
        assertThat(incompleteBinding().isManualReviewRequired()).isTrue();
    }

    @Test
    void incompleteFactoryKeepsIncompleteSafeTrue() {
        assertThat(incompleteBinding().isIncompleteSafe()).isTrue();
    }

    @Test
    void incompleteFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> RuntimeKlineContextSourceBindingDTO.incomplete(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                List.of("source-trace-entry"),
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockedFailClosedFactorySetsFailClosedTrue() {
        RuntimeKlineContextSourceBindingDTO binding = blockedBinding();

        assertThat(binding.isFailClosed()).isTrue();
        assertThat(binding.getBindingStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED);
    }

    @Test
    void blockedFailClosedFactoryRequiresBlockedReason() {
        assertThatThrownBy(() -> RuntimeKlineContextSourceBindingDTO.blockedFailClosed(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                List.of("source-trace-entry"),
                ""
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void degradedFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> RuntimeKlineContextSourceBindingDTO.degraded(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                new BigDecimal("100.25"),
                new BigDecimal("100.10"),
                new BigDecimal("99.90"),
                new BigDecimal("101.20"),
                new BigDecimal("99.50"),
                new BigDecimal("100.10"),
                new BigDecimal("120.0"),
                new BigDecimal("12000.0"),
                Boolean.TRUE,
                new BigDecimal("0.95"),
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.NORMAL,
                "LOW",
                RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                List.of("source-trace-entry"),
                "market-data-ref-1",
                "2026-06-02T07:15:01Z",
                "2026-06-02T07:15:02Z",
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewOnlyFactoryCarriesOnlyExplicitFieldsAndDoesNotCalculateMarketValues() {
        RuntimeKlineContextSourceBindingDTO binding = reviewOnlyBinding();

        assertThat(binding.getBindingStatus())
                .isEqualTo(RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING);
        assertThat(binding.getLatestPrice()).isEqualByComparingTo("100.25");
        assertThat(binding.getLatestClose()).isEqualByComparingTo("100.10");
        assertThat(binding.getOpen()).isEqualByComparingTo("99.90");
        assertThat(binding.getHigh()).isEqualByComparingTo("101.20");
        assertThat(binding.getLow()).isEqualByComparingTo("99.50");
        assertThat(binding.getClose()).isEqualByComparingTo("100.10");
        assertThat(binding.getMissingReason()).isNull();
        assertThat(binding.getBlockedReason()).isNull();
    }

    @Test
    void latestPriceAndLatestCloseCanBeCarriedButDoNotRepresentPoints() {
        RuntimeKlineContextSourceBindingDTO binding = reviewOnlyBinding();

        assertThat(binding.getLatestPrice()).isEqualByComparingTo("100.25");
        assertThat(binding.getLatestClose()).isEqualByComparingTo("100.10");
        assertThat(binding.isReviewOnly()).isTrue();
        assertThat(binding.isNotTradeInstruction()).isTrue();
    }

    @Test
    void ohlcvFieldsCanBeCarriedButDoNotRepresentPoints() {
        RuntimeKlineContextSourceBindingDTO binding = reviewOnlyBinding();

        assertThat(binding.getOpen()).isEqualByComparingTo("99.90");
        assertThat(binding.getHigh()).isEqualByComparingTo("101.20");
        assertThat(binding.getLow()).isEqualByComparingTo("99.50");
        assertThat(binding.getClose()).isEqualByComparingTo("100.10");
        assertThat(binding.getVolume()).isEqualByComparingTo("120.0");
        assertThat(binding.getQuoteVolume()).isEqualByComparingTo("12000.0");
        assertThat(binding.isNotTradeInstruction()).isTrue();
    }

    @Test
    void sourceTraceRefsAreDefensivelyCopied() {
        List<String> refs = new ArrayList<>();
        refs.add("source-trace-entry");

        RuntimeKlineContextSourceBindingDTO binding = RuntimeKlineContextSourceBindingDTO.incomplete(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                refs,
                "RUNTIME_CONTEXT_MISSING"
        );
        refs.add("source-trace-stop");

        assertThat(binding.getSourceTraceRefs()).containsExactly("source-trace-entry");
        assertThatThrownBy(() -> binding.getSourceTraceRefs().add("source-trace-tp"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void sourceTraceRefsCanBeEmptyAndRemainIncompleteSafe() {
        RuntimeKlineContextSourceBindingDTO binding = RuntimeKlineContextSourceBindingDTO.incomplete(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                null,
                "SOURCE_TRACE_REFS_MISSING"
        );

        assertThat(binding.getSourceTraceRefs()).isEmpty();
        assertThat(binding.isIncompleteSafe()).isTrue();
        assertThat(binding.isReviewOnly()).isTrue();
    }

    @Test
    void bindingStatusEnumCoversRequiredStatuses() {
        assertThat(List.of(RuntimeKlineContextSourceBindingDTO.BindingStatus.values()))
                .contains(
                        RuntimeKlineContextSourceBindingDTO.BindingStatus.INCOMPLETE,
                        RuntimeKlineContextSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED,
                        RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING,
                        RuntimeKlineContextSourceBindingDTO.BindingStatus.REVIEW_ONLY_RUNTIME_KLINE_BINDING_DEGRADED
                );
    }

    @Test
    void freshnessStatusEnumCoversFreshStaleAndUnknown() {
        assertThat(List.of(RuntimeKlineContextSourceBindingDTO.FreshnessStatus.values()))
                .contains(
                        RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                        RuntimeKlineContextSourceBindingDTO.FreshnessStatus.STALE,
                        RuntimeKlineContextSourceBindingDTO.FreshnessStatus.UNKNOWN
                );
    }

    @Test
    void wickStatusEnumCoversRequiredStatuses() {
        assertThat(List.of(RuntimeKlineContextSourceBindingDTO.WickStatus.values()))
                .contains(
                        RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                        RuntimeKlineContextSourceBindingDTO.WickStatus.WICK_ONLY,
                        RuntimeKlineContextSourceBindingDTO.WickStatus.WICK_CONFIRMED,
                        RuntimeKlineContextSourceBindingDTO.WickStatus.UNKNOWN
                );
    }

    @Test
    void gapStatusEnumCoversRequiredStatuses() {
        assertThat(List.of(RuntimeKlineContextSourceBindingDTO.GapStatus.values()))
                .contains(
                        RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                        RuntimeKlineContextSourceBindingDTO.GapStatus.MINOR_GAP,
                        RuntimeKlineContextSourceBindingDTO.GapStatus.SEVERE_GAP,
                        RuntimeKlineContextSourceBindingDTO.GapStatus.UNKNOWN
                );
    }

    @Test
    void liquidityStateEnumCoversRequiredStatuses() {
        assertThat(List.of(RuntimeKlineContextSourceBindingDTO.LiquidityState.values()))
                .contains(
                        RuntimeKlineContextSourceBindingDTO.LiquidityState.NORMAL,
                        RuntimeKlineContextSourceBindingDTO.LiquidityState.DEGRADED,
                        RuntimeKlineContextSourceBindingDTO.LiquidityState.SEVERELY_DEGRADED,
                        RuntimeKlineContextSourceBindingDTO.LiquidityState.UNKNOWN
                );
    }

    @Test
    void stampedeStateEnumCoversRequiredStatuses() {
        assertThat(List.of(RuntimeKlineContextSourceBindingDTO.StampedeState.values()))
                .contains(
                        RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                        RuntimeKlineContextSourceBindingDTO.StampedeState.SUSPECTED,
                        RuntimeKlineContextSourceBindingDTO.StampedeState.CONFIRMED,
                        RuntimeKlineContextSourceBindingDTO.StampedeState.UNKNOWN
                );
    }

    @Test
    void noSetterBuilderOrFactoryParameterCanDisableSafetyFlags() {
        for (Method method : RuntimeKlineContextSourceBindingDTO.class.getDeclaredMethods()) {
            assertThat(method.getName()).doesNotStartWith("set");
            assertThat(method.getName().toLowerCase()).doesNotContain("builder");
            if (Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())) {
                assertThat(List.of(method.getParameterTypes())).doesNotContain(boolean.class);
            }
        }
        for (Constructor<?> constructor : RuntimeKlineContextSourceBindingDTO.class.getDeclaredConstructors()) {
            assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        }
    }

    @Test
    void dtoHasNoSpringAnnotations() {
        assertNoAnnotations(RuntimeKlineContextSourceBindingDTO.class);
    }

    @Test
    void dtoHasNoServiceControllerMapperRepositoryOrSchedulerDependency() throws Exception {
        assertSourceDoesNotContain(List.of(
                "@Controller",
                "@RestController",
                "@Mapper",
                "@Repository",
                "@Scheduled",
                "Service",
                "Provider"
        ));
    }

    @Test
    void dtoHasNoMarketQuoteOrHttpOrDataSourceDependency() throws Exception {
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
                "AutoTrading"
        ));
    }

    @Test
    void dtoPublicStatusAndReasonOutputsDoNotContainForbiddenExecutableSemantics() {
        for (RuntimeKlineContextSourceBindingDTO binding : List.of(
                incompleteBinding(),
                blockedBinding(),
                degradedBinding(),
                reviewOnlyBinding()
        )) {
            assertNoForbiddenExecutableSemantics(List.of(
                    binding.getBindingStatus().name(),
                    valueOrEmpty(binding.getMissingReason()),
                    valueOrEmpty(binding.getBlockedReason())
            ));
        }
    }

    private RuntimeKlineContextSourceBindingDTO incompleteBinding() {
        return RuntimeKlineContextSourceBindingDTO.incomplete(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                List.of("source-trace-entry"),
                "RUNTIME_CONTEXT_MISSING"
        );
    }

    private RuntimeKlineContextSourceBindingDTO blockedBinding() {
        return RuntimeKlineContextSourceBindingDTO.blockedFailClosed(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                List.of("source-trace-entry"),
                "RUNTIME_CONTEXT_BLOCKED"
        );
    }

    private RuntimeKlineContextSourceBindingDTO degradedBinding() {
        return RuntimeKlineContextSourceBindingDTO.degraded(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                new BigDecimal("100.25"),
                new BigDecimal("100.10"),
                new BigDecimal("99.90"),
                new BigDecimal("101.20"),
                new BigDecimal("99.50"),
                new BigDecimal("100.10"),
                new BigDecimal("120.0"),
                new BigDecimal("12000.0"),
                Boolean.TRUE,
                new BigDecimal("0.95"),
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.NORMAL,
                "LOW",
                RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                List.of("source-trace-entry", "source-trace-stop"),
                "market-data-ref-1",
                "2026-06-02T07:15:01Z",
                "2026-06-02T07:15:02Z",
                "RUNTIME_CONTEXT_DEGRADED"
        );
    }

    private RuntimeKlineContextSourceBindingDTO reviewOnlyBinding() {
        return RuntimeKlineContextSourceBindingDTO.reviewOnly(
                "runtime-15m",
                "BTCUSDT",
                "SPOT",
                "15m",
                "2026-06-02T07:00:00Z/2026-06-02T07:15:00Z",
                new BigDecimal("100.25"),
                new BigDecimal("100.10"),
                new BigDecimal("99.90"),
                new BigDecimal("101.20"),
                new BigDecimal("99.50"),
                new BigDecimal("100.10"),
                new BigDecimal("120.0"),
                new BigDecimal("12000.0"),
                Boolean.TRUE,
                new BigDecimal("0.95"),
                RuntimeKlineContextSourceBindingDTO.FreshnessStatus.FRESH,
                RuntimeKlineContextSourceBindingDTO.WickStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.GapStatus.NONE,
                RuntimeKlineContextSourceBindingDTO.LiquidityState.NORMAL,
                "LOW",
                RuntimeKlineContextSourceBindingDTO.StampedeState.NONE,
                List.of("source-trace-entry", "source-trace-stop"),
                "market-data-ref-1",
                "2026-06-02T07:15:01Z",
                "2026-06-02T07:15:02Z"
        );
    }

    private void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();

        assertThat(annotations).isEmpty();
    }

    private void assertSourceDoesNotContain(List<String> fragments) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/dto/point/RuntimeKlineContextSourceBindingDTO.java"
        ));

        for (String fragment : fragments) {
            assertThat(source).doesNotContain(fragment);
        }
    }

    private void assertNoForbiddenExecutableSemantics(List<String> outputs) {
        List<String> forbiddenWords = List.of(
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

        for (String output : outputs) {
            String lowerOutput = output == null ? "" : output.toLowerCase();
            for (String forbiddenWord : forbiddenWords) {
                assertThat(lowerOutput).doesNotContain(forbiddenWord);
            }
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
