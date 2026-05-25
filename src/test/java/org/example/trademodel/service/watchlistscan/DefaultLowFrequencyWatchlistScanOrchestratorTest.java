package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanStatusEnum;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.junit.jupiter.api.Test;

class DefaultLowFrequencyWatchlistScanOrchestratorTest {

    @Test
    void disabledByDefaultReturnsIncomplete() {
        DefaultLowFrequencyWatchlistScanOrchestrator orchestrator =
                new DefaultLowFrequencyWatchlistScanOrchestrator(null, null, false);

        WatchlistScanResultDTO result = orchestrator.scanSingleSymbol(watchlistRequest("BTCUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons())
                .contains("LOW_FREQUENCY_SCAN_ORCHESTRATOR_DISABLED_BY_DEFAULT");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nullRequestFailsClosed() {
        DefaultLowFrequencyWatchlistScanOrchestrator orchestrator =
                new DefaultLowFrequencyWatchlistScanOrchestrator(
                        request -> RuntimeSourceReadResultDTO.sourceUnavailable(null, List.of("unused")),
                        result -> WatchlistScanResultDTO.reviewOnly(null, List.of("unused")),
                        true
                );

        WatchlistScanResultDTO result = orchestrator.scanSingleSymbol(null);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("REQUEST_MISSING", "ORCHESTRATOR_BLOCKED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nonWatchlistPoolOnlyRequestFailsClosed() throws Exception {
        DefaultLowFrequencyWatchlistScanOrchestrator orchestrator =
                new DefaultLowFrequencyWatchlistScanOrchestrator(
                        request -> RuntimeSourceReadResultDTO.sourceUnavailable(request.getSymbol(), List.of("unused")),
                        result -> WatchlistScanResultDTO.reviewOnly("BTCUSDT", List.of("unused")),
                        true
                );

        WatchlistScanResultDTO result = orchestrator.scanSingleSymbol(nonWatchlistPoolRequest("BTCUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("WATCHLIST_POOL_ONLY_REQUIRED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void missingRuntimeSourceServiceFailsClosed() {
        DefaultLowFrequencyWatchlistScanOrchestrator orchestrator =
                new DefaultLowFrequencyWatchlistScanOrchestrator(
                        null,
                        result -> WatchlistScanResultDTO.reviewOnly("BTCUSDT", List.of("unused")),
                        true
                );

        WatchlistScanResultDTO result = orchestrator.scanSingleSymbol(watchlistRequest("BTCUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("RUNTIME_SOURCE_SERVICE_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void missingScanResultAssemblerFailsClosed() {
        DefaultLowFrequencyWatchlistScanOrchestrator orchestrator =
                new DefaultLowFrequencyWatchlistScanOrchestrator(
                        request -> RuntimeSourceReadResultDTO.sourceUnavailable(request.getSymbol(), List.of("unused")),
                        null,
                        true
                );

        WatchlistScanResultDTO result = orchestrator.scanSingleSymbol(watchlistRequest("BTCUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SCAN_RESULT_ASSEMBLER_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void runtimeSourceServiceResultIsPassedToAssembler() {
        RuntimeSourceReadResultDTO runtimeResult = RuntimeSourceReadResultDTO.sourceUnavailable(
                "ETHUSDT",
                List.of("source_unavailable")
        );
        CapturingAssembler assembler = new CapturingAssembler(WatchlistScanResultDTO.incomplete(
                "ETHUSDT",
                List.of("assembled")
        ));
        DefaultLowFrequencyWatchlistScanOrchestrator orchestrator =
                new DefaultLowFrequencyWatchlistScanOrchestrator(
                        request -> runtimeResult,
                        assembler,
                        true
                );

        orchestrator.scanSingleSymbol(watchlistRequest("ETHUSDT"));

        assertThat(assembler.capturedResult).isSameAs(runtimeResult);
    }

    @Test
    void assemblerResultIsReturnedSafely() {
        WatchlistScanResultDTO assemblerResult = WatchlistScanResultDTO.reviewOnly(
                "SOLUSDT",
                List.of("AVAILABLE_REVIEW_ONLY")
        );
        DefaultLowFrequencyWatchlistScanOrchestrator orchestrator =
                new DefaultLowFrequencyWatchlistScanOrchestrator(
                        request -> RuntimeSourceReadResultDTO.sourceUnavailable(request.getSymbol(), List.of("unused")),
                        result -> assemblerResult,
                        true
                );

        WatchlistScanResultDTO result = orchestrator.scanSingleSymbol(watchlistRequest("SOLUSDT"));

        assertThat(result).isSameAs(assemblerResult);
        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void assemblerReturnsNullFailsClosed() {
        DefaultLowFrequencyWatchlistScanOrchestrator orchestrator =
                new DefaultLowFrequencyWatchlistScanOrchestrator(
                        request -> RuntimeSourceReadResultDTO.sourceUnavailable(request.getSymbol(), List.of("unused")),
                        result -> null,
                        true
                );

        WatchlistScanResultDTO result = orchestrator.scanSingleSymbol(watchlistRequest("ADAUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SCAN_RESULT_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void assemblerThrowsExceptionFailsClosed() {
        DefaultLowFrequencyWatchlistScanOrchestrator orchestrator =
                new DefaultLowFrequencyWatchlistScanOrchestrator(
                        request -> RuntimeSourceReadResultDTO.sourceUnavailable(request.getSymbol(), List.of("unused")),
                        result -> {
                            throw new IllegalStateException("assembler unavailable");
                        },
                        true
                );

        WatchlistScanResultDTO result = orchestrator.scanSingleSymbol(watchlistRequest("BNBUSDT"));

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("ORCHESTRATOR_ASSEMBLY_FAILED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void interfaceDeclaresOnlySingleSymbolScanContract() {
        Method[] methods = LowFrequencyWatchlistScanOrchestrator.class.getDeclaredMethods();

        assertThat(methods).hasSize(1);
        assertThat(methods[0].getName()).isEqualTo("scanSingleSymbol");
        for (Method method : methods) {
            assertThat(method.getName()).doesNotContain("Batch");
            assertThat(method.getName()).doesNotContain("Scheduled");
            assertThat(method.getName()).doesNotContain("Scheduler");
        }
    }

    @Test
    void defaultOrchestratorDeclaresNoForbiddenFields() {
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

        for (Field field : DefaultLowFrequencyWatchlistScanOrchestrator.class.getDeclaredFields()) {
            String fieldName = field.getName();
            String fieldTypeName = field.getType().getName();
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(fieldName).doesNotContain(forbiddenFragment);
                assertThat(fieldTypeName).doesNotContain(forbiddenFragment);
            }
        }
    }

    private static RuntimeSourceReadRequestDTO watchlistRequest(String symbol) {
        return RuntimeSourceReadRequestDTO.forWatchlistPool(
                symbol,
                "unit-test",
                "low-frequency-watchlist-scan-orchestrator"
        );
    }

    private static RuntimeSourceReadRequestDTO nonWatchlistPoolRequest(String symbol) throws Exception {
        Constructor<RuntimeSourceReadRequestDTO> constructor =
                RuntimeSourceReadRequestDTO.class.getDeclaredConstructor(
                        String.class,
                        Boolean.class,
                        String.class,
                        String.class,
                        List.class,
                        List.class
                );
        constructor.setAccessible(true);
        return constructor.newInstance(
                symbol,
                false,
                "unit-test",
                "non-watchlist-pool",
                List.of(),
                List.of()
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

    private static final class CapturingAssembler implements WatchlistScanResultAssembler {
        private final WatchlistScanResultDTO result;
        private RuntimeSourceReadResultDTO capturedResult;

        private CapturingAssembler(WatchlistScanResultDTO result) {
            this.result = result;
        }

        @Override
        public WatchlistScanResultDTO assemble(RuntimeSourceReadResultDTO runtimeSourceReadResult) {
            capturedResult = runtimeSourceReadResult;
            return result;
        }
    }
}
