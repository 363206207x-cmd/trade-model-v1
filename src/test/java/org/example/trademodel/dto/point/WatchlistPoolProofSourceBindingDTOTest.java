package org.example.trademodel.dto.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WatchlistPoolProofSourceBindingDTOTest {

    @Test
    void factoriesForceSafetyFlagsTrue() {
        List<WatchlistPoolProofSourceBindingDTO> contexts = List.of(
                incompleteContext(),
                blockedContext(),
                degradedContext(),
                reviewOnlyContext()
        );

        for (WatchlistPoolProofSourceBindingDTO context : contexts) {
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
        assertThatThrownBy(() -> WatchlistPoolProofSourceBindingDTO.incomplete(
                "proof-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-ref",
                List.of("watchlistPoolVersion"),
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blockedFailClosedFactoryRequiresBlockedReason() {
        assertThatThrownBy(() -> WatchlistPoolProofSourceBindingDTO.blockedFailClosed(
                "proof-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-ref",
                List.of("WATCHLIST_POOL_DISABLED"),
                "WATCHLIST_POOL_BLOCKED",
                null
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void degradedFactoryRequiresMissingReason() {
        assertThatThrownBy(() -> WatchlistPoolProofSourceBindingDTO.degraded(
                "proof-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-ref",
                "v1",
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                "WATCHLIST_POOL",
                "2026-06-03T00:00:00Z",
                "2026-06-04T00:00:00Z",
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                "display-slot-ref",
                Boolean.TRUE,
                Boolean.TRUE,
                "audit-ref",
                "operator-ref",
                "POOL_MEMBER_REVIEW",
                "PROOF_STALE",
                "REVIEW_ONLY_CANDIDATE_BOUNDARY",
                null,
                List.of(),
                List.of("PROOF_STALE"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "",
                Boolean.TRUE
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reviewOnlyFactoryCarriesExplicitFieldsWithoutReadingRuntime() {
        WatchlistPoolProofSourceBindingDTO context = reviewOnlyContext();

        assertThat(context.getWatchlistPoolProofContextId()).isEqualTo("proof-1");
        assertThat(context.getWatchlistPoolRef()).isEqualTo("watchlist-pool-ref");
        assertThat(context.getWatchlistPoolVersion()).isEqualTo("v1");
        assertThat(context.getWatchlistPoolEnabled()).isTrue();
        assertThat(context.getWatchlistPoolMember()).isTrue();
        assertThat(context.getPromotedToHomeCandidate()).isTrue();
        assertThat(context.getLowFrequencyScanCandidate()).isTrue();
        assertThat(context.getBindingStatus())
                .isEqualTo(WatchlistPoolProofSourceBindingDTO.BindingStatus
                        .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING);
    }

    @Test
    void listFieldsAreDefensivelyCopiedAndImmutable() {
        List<String> refs = new ArrayList<>();
        refs.add("source-trace-ref");
        WatchlistPoolProofSourceBindingDTO context = WatchlistPoolProofSourceBindingDTO.reviewOnly(
                "proof-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                refs,
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-ref",
                "v1",
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                "WATCHLIST_POOL",
                "2026-06-03T00:00:00Z",
                "2026-06-04T00:00:00Z",
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                "display-slot-ref",
                Boolean.TRUE,
                Boolean.TRUE,
                "audit-ref",
                "operator-ref",
                "POOL_MEMBER_REVIEW",
                "PROOF_FRESH",
                "REVIEW_ONLY_CANDIDATE_BOUNDARY",
                null,
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                Boolean.TRUE
        );
        refs.add("mutated-ref");

        assertThat(context.getSourceTraceRefs()).containsExactly("source-trace-ref");
        assertThatThrownBy(() -> context.getSourceTraceRefs().add("another-ref"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void bindingStatusEnumCoversRequiredStatuses() {
        assertThat(WatchlistPoolProofSourceBindingDTO.BindingStatus.values())
                .contains(
                        WatchlistPoolProofSourceBindingDTO.BindingStatus.INCOMPLETE,
                        WatchlistPoolProofSourceBindingDTO.BindingStatus.BLOCKED_FAIL_CLOSED,
                        WatchlistPoolProofSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING,
                        WatchlistPoolProofSourceBindingDTO.BindingStatus
                                .REVIEW_ONLY_WATCHLIST_POOL_PROOF_BINDING_DEGRADED
                );
    }

    @Test
    void noSetterBuilderOrFactoryCanDisableSafetyFlags() {
        for (Method method : WatchlistPoolProofSourceBindingDTO.class.getDeclaredMethods()) {
            assertThat(method.getName()).doesNotStartWith("set");
            assertThat(method.getName().toLowerCase()).doesNotContain("builder");
            assertThat(Modifier.isPublic(method.getModifiers()) && method.getName().contains("Safety"))
                    .isFalse();
        }
    }

    @Test
    void dtoHasNoSpringAnnotations() {
        assertNoAnnotations(WatchlistPoolProofSourceBindingDTO.class);
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
                "Mapper",
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

    private WatchlistPoolProofSourceBindingDTO incompleteContext() {
        return WatchlistPoolProofSourceBindingDTO.incomplete(
                "proof-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-ref",
                List.of("watchlistPoolVersion"),
                "WATCHLIST_POOL_PROOF_MISSING"
        );
    }

    private WatchlistPoolProofSourceBindingDTO blockedContext() {
        return WatchlistPoolProofSourceBindingDTO.blockedFailClosed(
                "proof-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-ref",
                List.of("WATCHLIST_POOL_DISABLED"),
                "WATCHLIST_POOL_BLOCKED",
                "WATCHLIST_POOL_DISABLED"
        );
    }

    private WatchlistPoolProofSourceBindingDTO degradedContext() {
        return WatchlistPoolProofSourceBindingDTO.degraded(
                "proof-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-ref",
                "v1",
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                "WATCHLIST_POOL",
                "2026-06-03T00:00:00Z",
                "2026-06-04T00:00:00Z",
                Boolean.FALSE,
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                "display-slot-ref",
                Boolean.TRUE,
                Boolean.TRUE,
                "audit-ref",
                "operator-ref",
                "POOL_MEMBER_REVIEW",
                "PROOF_STALE",
                "REVIEW_ONLY_CANDIDATE_BOUNDARY",
                null,
                List.of(),
                List.of("PROOF_STALE"),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                "WATCHLIST_POOL_PROOF_DEGRADED",
                Boolean.TRUE
        );
    }

    private WatchlistPoolProofSourceBindingDTO reviewOnlyContext() {
        return WatchlistPoolProofSourceBindingDTO.reviewOnly(
                "proof-1",
                "BTCUSDT",
                "SPOT",
                "15m",
                List.of("source-trace-ref"),
                "runtime-kline-ref",
                "data-quality-ref",
                "multi-timeframe-ref",
                "risk-action-guard-ref",
                "watchlist-pool-ref",
                "v1",
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.TRUE,
                "WATCHLIST_POOL",
                "2026-06-03T00:00:00Z",
                "2026-06-04T00:00:00Z",
                Boolean.TRUE,
                Boolean.FALSE,
                Boolean.FALSE,
                Boolean.FALSE,
                "display-slot-ref",
                Boolean.TRUE,
                Boolean.TRUE,
                "audit-ref",
                "operator-ref",
                "POOL_MEMBER_REVIEW",
                "PROOF_FRESH",
                "REVIEW_ONLY_CANDIDATE_BOUNDARY",
                null,
                List.of(),
                List.of(),
                List.of(),
                "2026-06-03T00:00:00Z",
                "2026-06-03T00:01:00Z",
                Boolean.TRUE
        );
    }

    private static void assertNoAnnotations(Class<?> type) {
        Annotation[] annotations = type.getAnnotations();
        assertThat(annotations).isEmpty();
    }

    private static void assertSourceDoesNotContain(List<String> forbiddenTokens) throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/org/example/trademodel/dto/point/WatchlistPoolProofSourceBindingDTO.java"
        ));
        for (String forbiddenToken : forbiddenTokens) {
            assertThat(source).doesNotContain(forbiddenToken);
        }
    }
}
