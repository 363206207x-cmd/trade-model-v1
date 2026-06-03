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

class RiskActionGuardSourceBindingDTOTest {

    @Test
    void factoriesKeepSafetyFlagsForcedTrue() {
        List<RiskActionGuardSourceBindingDTO> contexts = List.of(
                incompleteContext(),
                blockedContext(),
                degradedContext(),
                reviewOnlyContext()
        );

        for (RiskActionGuardSourceBindingDTO context : contexts) {
            assertThat(context.isReviewOnly()).isTrue();
            assertThat(context.isNotTradeInstruction()).isTrue();
            assertThat(context.isManualReviewRequired()).isTrue();
            assertThat(context.isIncompleteSafe()).isTrue();
        }
    }

    @Test
    void failClosedOnlyTrueForBlockedStatus() {
        assertThat(blockedContext().isFailClosed()).isTrue();
        assertThat(incompleteContext().isFailClosed()).isFalse();
        assertThat(degradedContext().isFailClosed()).isFalse();
        assertThat(reviewOnlyContext().isFailClosed()).isFalse();
    }

    @Test
    void incompleteFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> RiskActionGuardSourceBindingDTO.incomplete(
                "rag-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                List.of("riskScore"),
                ""
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockedFailClosedFactoryRequiresBlockedReason() {
        assertThatThrownBy(() -> RiskActionGuardSourceBindingDTO.blockedFailClosed(
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
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void degradedFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> RiskActionGuardSourceBindingDTO.degraded(
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
                null,
                Boolean.TRUE
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewOnlyFactoryCarriesExplicitFieldsAndDoesNotCalculateRisk() {
        RiskActionGuardSourceBindingDTO context = reviewOnlyContext();

        assertThat(context.getRiskScore()).isEqualByComparingTo("42");
        assertThat(context.getActionRiskScore()).isEqualByComparingTo("35");
        assertThat(context.getRiskLevel()).isEqualTo(RiskActionGuardSourceBindingDTO.RiskLevel.MEDIUM);
        assertThat(context.getProposedActionLabel()).isEqualTo("REVIEW_ONLY_RECHECK");
        assertThat(context.getMissingReason()).isNull();
        assertThat(context.getBlockedReason()).isNull();
    }

    @Test
    void listFieldsAreDefensivelyCopiedAndImmutable() {
        List<String> sourceTraceRefs = new ArrayList<>();
        sourceTraceRefs.add("source-trace-ref");
        List<String> labels = new ArrayList<>();
        labels.add("REVIEW_ONLY_RECHECK");

        RiskActionGuardSourceBindingDTO context = RiskActionGuardSourceBindingDTO.reviewOnly(
                "rag-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                sourceTraceRefs,
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
                labels,
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
        sourceTraceRefs.add("mutated-ref");
        labels.add("MUTATED_LABEL");

        assertThat(context.getSourceTraceRefs()).containsExactly("source-trace-ref");
        assertThat(context.getAllowedReviewOnlyActionLabels()).containsExactly("REVIEW_ONLY_RECHECK");
        assertThatThrownBy(() -> context.getSourceTraceRefs().add("new-ref"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void enumsCoverRequiredStatusesRiskLevelsAndLiquidityStates() {
        assertThat(List.of(RiskActionGuardSourceBindingDTO.BindingStatus.values()))
                .contains(
                        RiskActionGuardSourceBindingDTO.BindingStatus.INCOMPLETE,
                        RiskActionGuardSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED,
                        RiskActionGuardSourceBindingDTO.BindingStatus.REVIEW_ONLY_RISK_ACTION_GUARD_BINDING,
                        RiskActionGuardSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_RISK_ACTION_GUARD_BINDING_DEGRADED
                );
        assertThat(List.of(RiskActionGuardSourceBindingDTO.RiskLevel.values()))
                .contains(
                        RiskActionGuardSourceBindingDTO.RiskLevel.LOW,
                        RiskActionGuardSourceBindingDTO.RiskLevel.MEDIUM,
                        RiskActionGuardSourceBindingDTO.RiskLevel.HIGH,
                        RiskActionGuardSourceBindingDTO.RiskLevel.CRITICAL,
                        RiskActionGuardSourceBindingDTO.RiskLevel.UNKNOWN
                );
        assertThat(List.of(RiskActionGuardSourceBindingDTO.LiquidityState.values()))
                .contains(
                        RiskActionGuardSourceBindingDTO.LiquidityState.NORMAL,
                        RiskActionGuardSourceBindingDTO.LiquidityState.DEGRADED,
                        RiskActionGuardSourceBindingDTO.LiquidityState.SEVERELY_DEGRADED,
                        RiskActionGuardSourceBindingDTO.LiquidityState.UNKNOWN
                );
    }

    @Test
    void noSetterBuilderOrFactoryCanDisableSafetyFlags() {
        for (Method method : RiskActionGuardSourceBindingDTO.class.getDeclaredMethods()) {
            assertThat(method.getName()).doesNotStartWith("set");
            assertThat(method.getName().toLowerCase()).doesNotContain("builder");
            assertThat(method.getName()).doesNotContain("ReviewOnlyFalse");
            assertThat(method.getName()).doesNotContain("TradeInstructionFalse");
        }
        for (Field field : RiskActionGuardSourceBindingDTO.class.getDeclaredFields()) {
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
        assertNoAnnotations(RiskActionGuardSourceBindingDTO.class);
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
                "Binance",
                "OKX",
                "Bybit",
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
    void dtoDoesNotReferenceExternalPushExecutionOrAutoTradingClasses() throws Exception {
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

    private RiskActionGuardSourceBindingDTO reviewOnlyContext() {
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

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/dto/point/RiskActionGuardSourceBindingDTO.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }
}
