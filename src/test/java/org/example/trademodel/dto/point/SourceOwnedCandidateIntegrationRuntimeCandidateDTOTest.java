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

class SourceOwnedCandidateIntegrationRuntimeCandidateDTOTest {

    @Test
    void factoriesKeepSafetyFlagsForcedTrue() {
        List<SourceOwnedCandidateIntegrationRuntimeCandidateDTO> contexts = List.of(
                incompleteContext(),
                blockedContext(),
                degradedContext(),
                reviewOnlyContext()
        );

        for (SourceOwnedCandidateIntegrationRuntimeCandidateDTO context : contexts) {
            assertThat(context.isReviewOnly()).isTrue();
            assertThat(context.isNotTradeInstruction()).isTrue();
            assertThat(context.isManualReviewRequired()).isTrue();
            assertThat(context.isIncompleteSafe()).isTrue();
        }
    }

    @Test
    void blockedFailClosedFactorySetsFailClosedTrue() {
        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context = blockedContext();

        assertThat(context.isFailClosed()).isTrue();
        assertThat(context.getCandidateRuntimeStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus.BLOCKED_FAIL_CLOSED);
    }

    @Test
    void incompleteFactoryRequiresMissingReasonOrUnavailableReason() {
        assertThatThrownBy(() -> SourceOwnedCandidateIntegrationRuntimeCandidateDTO.incomplete(
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
                " ",
                null
        )).isInstanceOf(IllegalArgumentException.class);

        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context =
                SourceOwnedCandidateIntegrationRuntimeCandidateDTO.incomplete(
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

        assertThat(context.getMissingReason()).isEqualTo("RUNTIME_CANDIDATE_UNAVAILABLE");
    }

    @Test
    void blockedFailClosedFactoryRequiresBlockedReason() {
        assertThatThrownBy(() -> SourceOwnedCandidateIntegrationRuntimeCandidateDTO.blockedFailClosed(
                "runtime-candidate-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "source-binding-ref",
                "BLOCKED_FAIL_CLOSED",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                List.of("RUNTIME_CANDIDATE_BLOCKED"),
                "",
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void degradedFactoryRequiresDegradedReason() {
        assertThatThrownBy(() -> SourceOwnedCandidateIntegrationRuntimeCandidateDTO.degraded(
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
                "",
                "RUNTIME_CANDIDATE_DEGRADED",
                List.of(),
                List.of(),
                "2026-06-04T00:00:00Z",
                "2026-06-04T00:01:00Z"
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewOnlyFactoryCarriesExplicitFieldsAndDoesNotCalculateScores() {
        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context = reviewOnlyContext();

        assertThat(context.getRuntimeCandidateContextId()).isEqualTo("runtime-candidate-1");
        assertThat(context.getSourceBindingCompletenessScore()).isEqualByComparingTo("96");
        assertThat(context.getSourceBindingCompletenessSummary()).isEqualTo("SOURCE_BINDING_READY");
        assertThat(context.getAllRequiredSourcesPresent()).isTrue();
        assertThat(context.getAllRequiredSourcesTrusted()).isTrue();
        assertThat(context.getAllRequiredSourcesReviewOnly()).isTrue();
        assertThat(context.getAllRequiredSourcesNotTradeInstruction()).isTrue();
        assertThat(context.getAllRequiredSourcesManualReviewRequired()).isTrue();
        assertThat(context.getAllRequiredSourcesIncompleteSafe()).isTrue();
        assertThat(context.getAnySourceBlocked()).isFalse();
        assertThat(context.getAnySourceIncomplete()).isFalse();
        assertThat(context.getAnySourceDegraded()).isFalse();
        assertThat(context.getMissingReason()).isNull();
        assertThat(context.getBlockedReason()).isNull();
    }

    @Test
    void listFieldsAreDefensivelyCopiedAndImmutable() {
        List<String> sourceTraceRefs = new ArrayList<>();
        sourceTraceRefs.add("source-trace-ref");
        List<String> validationReasons = new ArrayList<>();
        validationReasons.add("SOURCE_BINDING_READY");
        List<String> missingFields = new ArrayList<>();
        missingFields.add("sourceBindingCompletenessScore");

        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context =
                SourceOwnedCandidateIntegrationRuntimeCandidateDTO.reviewOnly(
                        "runtime-candidate-1",
                        "BTCUSDT",
                        "SPOT",
                        "15m",
                        "source-binding-ref",
                        "REVIEW_ONLY",
                        validationReasons,
                        bd("96"),
                        "SOURCE_BINDING_READY",
                        sourceTraceRefs,
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
                        missingFields,
                        List.of(),
                        List.of(),
                        "2026-06-04T00:00:00Z",
                        "2026-06-04T00:01:00Z"
                );
        sourceTraceRefs.add("mutated-ref");
        validationReasons.add("MUTATED_REASON");
        missingFields.add("mutated-field");

        assertThat(context.getSourceTraceRefs()).containsExactly("source-trace-ref");
        assertThat(context.getSourceOwnedCandidateIntegrationValidationReasons())
                .containsExactly("SOURCE_BINDING_READY");
        assertThat(context.getMissingFields()).containsExactly("sourceBindingCompletenessScore");
        assertThatThrownBy(() -> context.getSourceTraceRefs().add("new-ref"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void enumsCoverRequiredRuntimeStatuses() {
        assertThat(List.of(SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus.values()))
                .contains(
                        SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus.INCOMPLETE,
                        SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus.BLOCKED_FAIL_CLOSED,
                        SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus
                                .REVIEW_ONLY_RUNTIME_CANDIDATE,
                        SourceOwnedCandidateIntegrationRuntimeCandidateDTO.RuntimeStatus
                                .REVIEW_ONLY_RUNTIME_CANDIDATE_DEGRADED
                );
    }

    @Test
    void noSetterBuilderOrFactoryCanDisableSafetyFlags() {
        for (Method method : SourceOwnedCandidateIntegrationRuntimeCandidateDTO.class.getDeclaredMethods()) {
            assertThat(method.getName()).doesNotStartWith("set");
            assertThat(method.getName().toLowerCase()).doesNotContain("builder");
            assertThat(method.getName()).doesNotContain("ReviewOnlyFalse");
            assertThat(method.getName()).doesNotContain("TradeInstructionFalse");
        }
        for (Field field : SourceOwnedCandidateIntegrationRuntimeCandidateDTO.class.getDeclaredFields()) {
            if (field.getName().equals("reviewOnly")
                    || field.getName().equals("notTradeInstruction")
                    || field.getName().equals("manualReviewRequired")
                    || field.getName().equals("incompleteSafe")) {
                assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
            }
        }
    }

    @Test
    void dtoHasNoSpringMyBatisJpaJacksonLombokAnnotations() throws Exception {
        assertNoAnnotations(SourceOwnedCandidateIntegrationRuntimeCandidateDTO.class);
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
    void dtoSourceDoesNotReferenceServiceControllerMapperRepositoryScheduler() throws Exception {
        assertSourceDoesNotContain(List.of(
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
    void dtoSourceDoesNotReferenceMarketQuoteHttpOrDataSourceProviders() throws Exception {
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
    void dtoSourceDoesNotReferenceExternalPushOrderExecutionOrAutoTradingClasses() throws Exception {
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
    void dtoSourceDoesNotContainForbiddenExecutionFields() throws Exception {
        assertSourceDoesNotContain(List.of(
                "entry",
                "stop",
                "takeProfit",
                "TP",
                "RR",
                "leverage",
                "positionSize",
                "orderId",
                "orderIntent",
                "executionIntent",
                "autoTradingAction",
                "pushPayload",
                "externalChannelMessage",
                "finalDirection",
                "tradeAction",
                "openPosition",
                "closePosition",
                "reversePosition"
        ));
    }

    @Test
    void safeOutputDoesNotContainForbiddenExecutableSemantics() {
        SourceOwnedCandidateIntegrationRuntimeCandidateDTO context = reviewOnlyContext();
        String safeOutput = String.join(" ",
                context.getCandidateRuntimeStatus().name(),
                context.getSourceBindingCompletenessSummary(),
                String.valueOf(context.getCandidateUnavailableReason()),
                String.valueOf(context.getCandidateBlockedReason()),
                String.valueOf(context.getCandidateDegradedReason()),
                context.getSourceTraceRefs().toString()
        ).toLowerCase();

        for (String forbidden : List.of(
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
        )) {
            assertThat(safeOutput).doesNotContain(forbidden);
        }
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
                "RUNTIME_CANDIDATE_MISSING"
        );
    }

    private SourceOwnedCandidateIntegrationRuntimeCandidateDTO blockedContext() {
        return SourceOwnedCandidateIntegrationRuntimeCandidateDTO.blockedFailClosed(
                "runtime-candidate-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                "source-binding-ref",
                "BLOCKED_FAIL_CLOSED",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-proof-ref",
                List.of("RUNTIME_CANDIDATE_BLOCKED"),
                "RUNTIME_CANDIDATE_BLOCKED",
                "RUNTIME_CANDIDATE_BLOCKED"
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
                "RUNTIME_CANDIDATE_DEGRADED",
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
                "REVIEW_ONLY",
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

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/dto/point/"
                        + "SourceOwnedCandidateIntegrationRuntimeCandidateDTO.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }
}
