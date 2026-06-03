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

class SourceOwnedCandidateIntegrationSourceBindingDTOTest {

    @Test
    void factoriesKeepSafetyFlagsForcedTrue() {
        List<SourceOwnedCandidateIntegrationSourceBindingDTO> contexts = List.of(
                incompleteContext(),
                blockedContext(),
                degradedContext(),
                reviewOnlyContext()
        );

        for (SourceOwnedCandidateIntegrationSourceBindingDTO context : contexts) {
            assertThat(context.isReviewOnly()).isTrue();
            assertThat(context.isNotTradeInstruction()).isTrue();
            assertThat(context.isManualReviewRequired()).isTrue();
            assertThat(context.isIncompleteSafe()).isTrue();
        }
    }

    @Test
    void blockedFailClosedFactorySetsFailClosedTrue() {
        SourceOwnedCandidateIntegrationSourceBindingDTO context = blockedContext();

        assertThat(context.isFailClosed()).isTrue();
        assertThat(context.getBindingStatus())
                .isEqualTo(SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED);
    }

    @Test
    void incompleteFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> SourceOwnedCandidateIntegrationSourceBindingDTO.incomplete(
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
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockedFailClosedFactoryRequiresBlockedReason() {
        assertThatThrownBy(() -> SourceOwnedCandidateIntegrationSourceBindingDTO.blockedFailClosed(
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
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void degradedFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> SourceOwnedCandidateIntegrationSourceBindingDTO.degraded(
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
                List.of("MULTI_TIMEFRAME_DEGRADED"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "",
                Boolean.TRUE
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewOnlyFactoryCarriesExplicitFieldsAndDoesNotCalculateScores() {
        SourceOwnedCandidateIntegrationSourceBindingDTO context = reviewOnlyContext();

        assertThat(context.getCandidateIntegrationContextId()).isEqualTo("candidate-integration-1");
        assertThat(context.getSourceBindingCompletenessScore()).isEqualByComparingTo("96");
        assertThat(context.getAllRequiredSourcesPresent()).isTrue();
        assertThat(context.getAllRequiredSourcesTrusted()).isTrue();
        assertThat(context.getAllRequiredSourcesReviewOnly()).isTrue();
        assertThat(context.getAllRequiredSourcesNotTradeInstruction()).isTrue();
        assertThat(context.getAllRequiredSourcesManualReviewRequired()).isTrue();
        assertThat(context.getAllRequiredSourcesIncompleteSafe()).isTrue();
        assertThat(context.getAnySourceBlocked()).isFalse();
        assertThat(context.getAnySourceIncomplete()).isFalse();
        assertThat(context.getAnySourceDegraded()).isFalse();
        assertThat(context.getCandidateBoundaryLabel()).isEqualTo("REVIEW_ONLY_CANDIDATE_BOUNDARY");
        assertThat(context.getMissingReason()).isNull();
        assertThat(context.getBlockedReason()).isNull();
    }

    @Test
    void listFieldsAreDefensivelyCopiedAndImmutable() {
        List<String> sourceTraceRefs = new ArrayList<>();
        sourceTraceRefs.add("source-trace-ref");
        List<String> sourceOwnedTraceRefs = new ArrayList<>();
        sourceOwnedTraceRefs.add("source-owned-trace-ref");
        List<String> missingFields = new ArrayList<>();
        missingFields.add("runtimeKlineContextRef");

        SourceOwnedCandidateIntegrationSourceBindingDTO context =
                SourceOwnedCandidateIntegrationSourceBindingDTO.incomplete(
                        "candidate-integration-1",
                        "BTCUSDT",
                        "SPOT",
                        "15m",
                        sourceTraceRefs,
                        "runtime-kline-ref",
                        "data-quality-ref",
                        "multi-timeframe-ref",
                        "risk-action-guard-ref",
                        "watchlist-pool-proof-ref",
                        missingFields,
                        "SOURCE_BINDING_MISSING"
                );
        sourceTraceRefs.add("mutated-ref");
        sourceOwnedTraceRefs.add("mutated-owned-ref");
        missingFields.add("mutated-field");

        assertThat(context.getSourceTraceRefs()).containsExactly("source-trace-ref");
        assertThat(context.getSourceOwnedTraceRefs()).isEmpty();
        assertThat(context.getMissingFields()).containsExactly("runtimeKlineContextRef");
        assertThatThrownBy(() -> context.getSourceTraceRefs().add("new-ref"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void enumsCoverRequiredStatuses() {
        assertThat(List.of(SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.values()))
                .contains(
                        SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.INCOMPLETE,
                        SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED,
                        SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING,
                        SourceOwnedCandidateIntegrationSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_SOURCE_OWNED_CANDIDATE_INTEGRATION_BINDING_DEGRADED
                );
    }

    @Test
    void noSetterBuilderOrFactoryCanDisableSafetyFlags() {
        for (Method method : SourceOwnedCandidateIntegrationSourceBindingDTO.class.getDeclaredMethods()) {
            assertThat(method.getName()).doesNotStartWith("set");
            assertThat(method.getName().toLowerCase()).doesNotContain("builder");
            assertThat(method.getName()).doesNotContain("ReviewOnlyFalse");
            assertThat(method.getName()).doesNotContain("TradeInstructionFalse");
        }
        for (Field field : SourceOwnedCandidateIntegrationSourceBindingDTO.class.getDeclaredFields()) {
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
        assertNoAnnotations(SourceOwnedCandidateIntegrationSourceBindingDTO.class);
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
    void safeOutputDoesNotContainForbiddenExecutableSemantics() {
        SourceOwnedCandidateIntegrationSourceBindingDTO context = reviewOnlyContext();
        String safeOutput = String.join(" ",
                context.getBindingStatus().name(),
                context.getCandidateBoundaryLabel(),
                String.valueOf(context.getCandidateUnavailableReason()),
                String.valueOf(context.getCandidateBlockedReason()),
                String.valueOf(context.getCandidateDegradedReason()),
                context.getSourceTraceRefs().toString(),
                context.getSourceOwnedTraceRefs().toString()
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
                        + "SourceOwnedCandidateIntegrationSourceBindingDTO.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }
}
