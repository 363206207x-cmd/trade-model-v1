package org.example.trademodel.service.watchlistsource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceTypeEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistRuntimeSourceServiceTest {

    @Test
    void nullRequestFailsClosed() {
        DefaultWatchlistRuntimeSourceService service = new DefaultWatchlistRuntimeSourceService(
                adapterReturning(sourceUnavailable("BTCUSDT")),
                source -> source
        );

        RuntimeSourceReadResultDTO result = service.readWatchlistRuntimeSource(null);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("request");
        assertThat(result.getBlockingReasons()).contains("REQUEST_MISSING", "RUNTIME_SOURCE_SERVICE_BLOCKED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void missingReadAdapterReturnsSourceUnavailable() {
        DefaultWatchlistRuntimeSourceService service = new DefaultWatchlistRuntimeSourceService(
                null,
                source -> source
        );

        RuntimeSourceReadResultDTO result = service.readWatchlistRuntimeSource(watchlistRequest("BTCUSDT"));

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(result.getBlockingReasons()).contains("READ_ADAPTER_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void missingGuardValidatorReturnsIncomplete() {
        DefaultWatchlistRuntimeSourceService service = new DefaultWatchlistRuntimeSourceService(
                adapterReturning(availableResult("BTCUSDT")),
                null
        );

        RuntimeSourceReadResultDTO result = service.readWatchlistRuntimeSource(watchlistRequest("BTCUSDT"));

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("guardValidator");
        assertThat(result.getBlockingReasons()).contains("GUARD_VALIDATOR_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void readAdapterReturnsNullFailsClosed() {
        DefaultWatchlistRuntimeSourceService service = new DefaultWatchlistRuntimeSourceService(
                adapterReturning(null),
                source -> source
        );

        RuntimeSourceReadResultDTO result = service.readWatchlistRuntimeSource(watchlistRequest("ETHUSDT"));

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(result.getBlockingReasons()).contains("READ_RESULT_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void readAdapterResultWithoutRuntimeSourceReturnsSafely() throws Exception {
        RuntimeSourceReadResultDTO readResult = resultWithoutRuntimeSource(
                "SOLUSDT",
                WatchlistRuntimeSourceStatusEnum.INCOMPLETE,
                List.of("runtimeSource"),
                List.of("NO_RUNTIME_SOURCE")
        );
        DefaultWatchlistRuntimeSourceService service = new DefaultWatchlistRuntimeSourceService(
                adapterReturning(readResult),
                source -> {
                    throw new AssertionError("guard should not be called when runtimeSource is null");
                }
        );

        RuntimeSourceReadResultDTO result = service.readWatchlistRuntimeSource(watchlistRequest("SOLUSDT"));

        assertThat(result).isSameAs(readResult);
        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void guardValidatorReturnsNullFailsClosed() {
        DefaultWatchlistRuntimeSourceService service = new DefaultWatchlistRuntimeSourceService(
                adapterReturning(availableResult("ADAUSDT")),
                source -> null
        );

        RuntimeSourceReadResultDTO result = service.readWatchlistRuntimeSource(watchlistRequest("ADAUSDT"));

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("guardResult");
        assertThat(result.getBlockingReasons()).contains("GUARD_RESULT_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void guardValidatorReturnsAvailableReviewOnlySourceSafely() {
        DefaultWatchlistRuntimeSourceService service = new DefaultWatchlistRuntimeSourceService(
                adapterReturning(availableResult("LINKUSDT")),
                source -> WatchlistRuntimeSourceDTO.availableReviewOnly(
                        "LINKUSDT",
                        WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT,
                        "manual:LINKUSDT",
                        List.of("guard_review_only")
                )
        );

        RuntimeSourceReadResultDTO result = service.readWatchlistRuntimeSource(watchlistRequest("LINKUSDT"));

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY);
        assertThat(result.getRuntimeSource().getSourceType())
                .isEqualTo(WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void serviceDoesNotCreateDisplaySlotsOrDefaultSixByItself() {
        CapturingAdapter adapter = new CapturingAdapter();
        DefaultWatchlistRuntimeSourceService service = new DefaultWatchlistRuntimeSourceService(
                adapter,
                source -> source
        );
        RuntimeSourceReadRequestDTO request = watchlistRequest("UNIUSDT");

        RuntimeSourceReadResultDTO result = service.readWatchlistRuntimeSource(request);

        assertThat(adapter.capturedRequest).isSameAs(request);
        assertThat(result.getSymbol()).isEqualTo("UNIUSDT");
        assertThat(result.getBlockingReasons()).containsExactly("adapter_uses_request_symbol");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void defaultServiceDeclaresNoForbiddenFields() {
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

        for (Field field : DefaultWatchlistRuntimeSourceService.class.getDeclaredFields()) {
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
                "runtime-source-service"
        );
    }

    private static RuntimeSourceReadResultDTO availableResult(String symbol) {
        return RuntimeSourceReadResultDTO.fromRuntimeSource(
                WatchlistRuntimeSourceDTO.availableReviewOnly(
                        symbol,
                        WatchlistRuntimeSourceTypeEnum.WATCHLIST_CONFIG,
                        "push.watchlist.symbols",
                        List.of("REVIEW_ONLY_DB_WATCHLIST_READ")
                )
        );
    }

    private static RuntimeSourceReadResultDTO sourceUnavailable(String symbol) {
        return RuntimeSourceReadResultDTO.sourceUnavailable(
                symbol,
                List.of("source_unavailable")
        );
    }

    private static RuleConfigWatchlistPoolReadAdapter adapterReturning(RuntimeSourceReadResultDTO result) {
        return new StubAdapter(result);
    }

    private static RuntimeSourceReadResultDTO resultWithoutRuntimeSource(
            String symbol,
            WatchlistRuntimeSourceStatusEnum readStatus,
            List<String> missingFields,
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
                missingFields,
                blockingReasons
        );
    }

    private static void assertSafeNoExecutionDefaults(RuntimeSourceReadResultDTO result) {
        assertThat(result.getManualReviewRequired()).isTrue();
        assertThat(result.getNotTradeInstruction()).isTrue();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertThat(result.getReadinessUpgraded()).isFalse();
        assertThat(result.getTradingActionCreated()).isFalse();
        assertThat(result.getEntryStopTpRrGenerated()).isFalse();
    }

    private static class StubAdapter extends RuleConfigWatchlistPoolReadAdapter {
        private final RuntimeSourceReadResultDTO result;

        private StubAdapter(RuntimeSourceReadResultDTO result) {
            super(null);
            this.result = result;
        }

        @Override
        public RuntimeSourceReadResultDTO read(RuntimeSourceReadRequestDTO request) {
            return result;
        }
    }

    private static final class CapturingAdapter extends RuleConfigWatchlistPoolReadAdapter {
        private RuntimeSourceReadRequestDTO capturedRequest;

        private CapturingAdapter() {
            super(null);
        }

        @Override
        public RuntimeSourceReadResultDTO read(RuntimeSourceReadRequestDTO request) {
            capturedRequest = request;
            return RuntimeSourceReadResultDTO.fromRuntimeSource(
                    WatchlistRuntimeSourceDTO.availableReviewOnly(
                            request.getSymbol(),
                            WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT,
                            "manual:" + request.getSymbol(),
                            List.of("adapter_uses_request_symbol")
                    )
            );
        }
    }
}
