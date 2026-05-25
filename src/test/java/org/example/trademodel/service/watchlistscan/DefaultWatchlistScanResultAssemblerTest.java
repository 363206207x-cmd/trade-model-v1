package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanStatusEnum;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceTypeEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistScanResultAssemblerTest {

    @Test
    void nullInputFailsClosed() {
        DefaultWatchlistScanResultAssembler assembler = new DefaultWatchlistScanResultAssembler(passThroughGuard());

        WatchlistScanResultDTO result = assembler.assemble(null);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("ASSEMBLY_INPUT_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void incompleteSourceWithoutRuntimeSourceMapsToIncomplete() throws Exception {
        DefaultWatchlistScanResultAssembler assembler = new DefaultWatchlistScanResultAssembler(passThroughGuard());

        WatchlistScanResultDTO result = assembler.assemble(resultWithoutRuntimeSource(
                "BTCUSDT",
                WatchlistRuntimeSourceStatusEnum.INCOMPLETE,
                List.of("source_missing")
        ));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("source_missing", "MISSING_RUNTIME_SOURCE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void sourceUnavailableMapsToIncomplete() throws Exception {
        DefaultWatchlistScanResultAssembler assembler = new DefaultWatchlistScanResultAssembler(passThroughGuard());

        WatchlistScanResultDTO result = assembler.assemble(resultWithoutRuntimeSource(
                "ETHUSDT",
                WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE,
                List.of("read_failed")
        ));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("read_failed", "SOURCE_UNAVAILABLE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void staleSourceMapsToReviewOnly() {
        DefaultWatchlistScanResultAssembler assembler = new DefaultWatchlistScanResultAssembler(passThroughGuard());

        WatchlistScanResultDTO result = assembler.assemble(RuntimeSourceReadResultDTO.fromRuntimeSource(
                WatchlistRuntimeSourceDTO.staleReviewOnly(
                        "SOLUSDT",
                        List.of("updatedAt"),
                        List.of("stale_runtime_source")
                )
        ));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(result.getBlockingReasons()).contains("stale_runtime_source", "STALE_REVIEW_ONLY");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void availableSourceMapsToReviewOnly() {
        DefaultWatchlistScanResultAssembler assembler = new DefaultWatchlistScanResultAssembler(passThroughGuard());

        WatchlistScanResultDTO result = assembler.assemble(RuntimeSourceReadResultDTO.fromRuntimeSource(
                availableSource("ADAUSDT")
        ));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(result.getBlockingReasons()).contains("db_watchlist_review", "AVAILABLE_REVIEW_ONLY");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nonWatchlistMapsFailClosed() {
        DefaultWatchlistScanResultAssembler assembler = new DefaultWatchlistScanResultAssembler(passThroughGuard());

        WatchlistScanResultDTO result = assembler.assemble(RuntimeSourceReadResultDTO.fromRuntimeSource(
                WatchlistRuntimeSourceDTO.blockedNotWatchlist("DOGEUSDT", List.of("not_in_pool"))
        ));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(result.getScanStatus()).isNotEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(result.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void guardReturnsBlockedMapsFailClosed() {
        DefaultWatchlistScanResultAssembler assembler = new DefaultWatchlistScanResultAssembler(
                snapshot -> WatchlistScanResultDTO.incomplete(
                        snapshot.getSymbol(),
                        List.of("custom_guard_block")
                )
        );

        WatchlistScanResultDTO result = assembler.assemble(RuntimeSourceReadResultDTO.fromRuntimeSource(
                availableSource("BNBUSDT")
        ));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("custom_guard_block", "GUARD_BLOCKED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void exceptionFailsClosed() {
        DefaultWatchlistScanResultAssembler assembler = new DefaultWatchlistScanResultAssembler(snapshot -> {
            throw new IllegalStateException("guard unavailable");
        });

        WatchlistScanResultDTO result = assembler.assemble(RuntimeSourceReadResultDTO.fromRuntimeSource(
                availableSource("LINKUSDT")
        ));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("ASSEMBLY_FAILED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void allOutputsPreserveSafeNoExecutionDefaults() throws Exception {
        DefaultWatchlistScanResultAssembler assembler = new DefaultWatchlistScanResultAssembler(passThroughGuard());

        List<WatchlistScanResultDTO> results = List.of(
                assembler.assemble(null),
                assembler.assemble(resultWithoutRuntimeSource(
                        "BTCUSDT",
                        WatchlistRuntimeSourceStatusEnum.INCOMPLETE,
                        List.of("source_missing")
                )),
                assembler.assemble(resultWithoutRuntimeSource(
                        "ETHUSDT",
                        WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE,
                        List.of("source_unavailable")
                )),
                assembler.assemble(RuntimeSourceReadResultDTO.fromRuntimeSource(
                        WatchlistRuntimeSourceDTO.staleReviewOnly("SOLUSDT", List.of("updatedAt"), List.of("stale"))
                )),
                assembler.assemble(RuntimeSourceReadResultDTO.fromRuntimeSource(availableSource("ADAUSDT"))),
                assembler.assemble(RuntimeSourceReadResultDTO.fromRuntimeSource(
                        WatchlistRuntimeSourceDTO.blockedNotWatchlist("DOGEUSDT", List.of("blocked"))
                ))
        );

        for (WatchlistScanResultDTO result : results) {
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void defaultAssemblerDeclaresNoForbiddenFields() {
        List<String> forbiddenFragments = List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "Controller",
                "Scheduler",
                "PushRecheckService",
                "PushSnapshotService",
                "ExternalRuntimeService",
                "RuntimeDataClient",
                "DataSource",
                "JdbcTemplate"
        );

        for (Field field : DefaultWatchlistScanResultAssembler.class.getDeclaredFields()) {
            String fieldName = field.getName();
            String fieldTypeName = field.getType().getName();
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(fieldName).doesNotContain(forbiddenFragment);
                assertThat(fieldTypeName).doesNotContain(forbiddenFragment);
            }
        }
    }

    private static WatchlistRuntimeSourceDTO availableSource(String symbol) {
        return WatchlistRuntimeSourceDTO.availableReviewOnly(
                symbol,
                WatchlistRuntimeSourceTypeEnum.WATCHLIST_CONFIG,
                "push.watchlist.symbols",
                List.of("db_watchlist_review")
        );
    }

    private static WatchlistScanGuardValidator passThroughGuard() {
        return snapshot -> WatchlistScanResultDTO.reviewOnly(
                snapshot.getSymbol(),
                snapshot.getBlockingReasons()
        );
    }

    private static RuntimeSourceReadResultDTO resultWithoutRuntimeSource(
            String symbol,
            WatchlistRuntimeSourceStatusEnum readStatus,
            List<String> blockingReasons
    ) throws Exception {
        Constructor<RuntimeSourceReadResultDTO> constructor =
                RuntimeSourceReadResultDTO.class.getDeclaredConstructor(
                        String.class,
                        WatchlistRuntimeSourceDTO.class,
                        WatchlistRuntimeSourceStatusEnum.class,
                        List.class,
                        List.class
                );
        constructor.setAccessible(true);
        return constructor.newInstance(
                symbol,
                null,
                readStatus,
                List.of(),
                blockingReasons
        );
    }

    private static void assertSafeNoExecutionDefaults(WatchlistScanResultDTO result) {
        assertThat(result.getManualReviewRequired()).isTrue();
        assertThat(result.getNotTradeInstruction()).isTrue();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertThat(result.getCandidateAttentionAllowed()).isFalse();
        assertThat(result.getPromoteToHomeAllowed()).isFalse();
        assertThat(result.getReadinessUpgraded()).isFalse();
        assertThat(result.getTradingActionCreated()).isFalse();
        assertThat(result.getEntryStopTpRrGenerated()).isFalse();
    }
}
