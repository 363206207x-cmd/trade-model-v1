package org.example.trademodel.service.watchlistsource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistPoolRuntimeSourceReadAdapterTest {

    private final DefaultWatchlistPoolRuntimeSourceReadAdapter adapter =
            new DefaultWatchlistPoolRuntimeSourceReadAdapter();

    @Test
    void nullRequestFailsClosed() {
        RuntimeSourceReadResultDTO result = adapter.read(null);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("request");
        assertThat(result.getBlockingReasons())
                .contains("READ_ADAPTER_NOT_IMPLEMENTED", "REQUEST_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void incompleteRequestFailsClosed() {
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.incomplete(
                "ETHUSDT",
                List.of("sourceRef"),
                List.of("request_incomplete")
        );

        RuntimeSourceReadResultDTO result = adapter.read(request);

        assertThat(result.getSymbol()).isEqualTo("ETHUSDT");
        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("sourceRef");
        assertThat(result.getBlockingReasons())
                .contains("request_incomplete", "READ_ADAPTER_NOT_IMPLEMENTED", "REQUEST_INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void normalWatchlistPoolRequestReturnsSourceUnavailable() {
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                "BTCUSDT",
                "unit-test",
                "no-op-read"
        );

        RuntimeSourceReadResultDTO result = adapter.read(request);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(result.getBlockingReasons())
                .contains("READ_ADAPTER_NOT_IMPLEMENTED", "NO_RUNTIME_READ_IMPLEMENTED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void adapterImplementsWatchlistPoolRuntimeSourceReadAdapter() {
        assertThat(adapter).isInstanceOf(WatchlistPoolRuntimeSourceReadAdapter.class);
    }

    @Test
    void allOutputsPreserveSafeNoExecutionDefaults() {
        List<RuntimeSourceReadResultDTO> results = List.of(
                adapter.read(null),
                adapter.read(RuntimeSourceReadRequestDTO.incomplete(
                        "SOLUSDT",
                        List.of("sourceRef"),
                        List.of("missing_source")
                )),
                adapter.read(RuntimeSourceReadRequestDTO.forWatchlistPool(
                        "ADAUSDT",
                        "unit-test",
                        "normal-no-op"
                ))
        );

        for (RuntimeSourceReadResultDTO result : results) {
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void defaultAdapterDeclaresNoForbiddenFields() {
        List<String> forbiddenFragments = List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "Mapper",
                "Controller",
                "Scheduler",
                "PushRecheckService",
                "PushSnapshotService",
                "ExternalRuntimeService",
                "RuntimeDataClient",
                "DataSource",
                "JdbcTemplate"
        );

        for (Field field : DefaultWatchlistPoolRuntimeSourceReadAdapter.class.getDeclaredFields()) {
            String fieldName = field.getName();
            String fieldTypeName = field.getType().getName();
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(fieldName).doesNotContain(forbiddenFragment);
                assertThat(fieldTypeName).doesNotContain(forbiddenFragment);
            }
        }
    }

    private void assertSafeNoExecutionDefaults(RuntimeSourceReadResultDTO result) {
        assertThat(result.getManualReviewRequired()).isTrue();
        assertThat(result.getNotTradeInstruction()).isTrue();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertThat(result.getReadinessUpgraded()).isFalse();
        assertThat(result.getTradingActionCreated()).isFalse();
        assertThat(result.getEntryStopTpRrGenerated()).isFalse();
    }
}
