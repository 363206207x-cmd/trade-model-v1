package org.example.trademodel.service.watchlistsource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceTypeEnum;
import org.junit.jupiter.api.Test;

class ProductionRuntimeSourceReadAdapterTest {

    @Test
    void interfacesDeclareOnlySafeReadContract() throws Exception {
        Method readMethod = ProductionRuntimeSourceReadAdapter.class.getDeclaredMethod(
                "read",
                RuntimeSourceReadRequestDTO.class
        );

        assertThat(ProductionRuntimeSourceReadAdapter.class.isInterface()).isTrue();
        assertThat(readMethod.getReturnType()).isEqualTo(RuntimeSourceReadResultDTO.class);
        assertThat(ProductionRuntimeSourceReadAdapter.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactly("read");
        assertThat(WatchlistPoolRuntimeSourceReadAdapter.class.isInterface()).isTrue();
        assertThat(WatchlistPoolRuntimeSourceReadAdapter.class.getInterfaces())
                .containsExactly(ProductionRuntimeSourceReadAdapter.class);
        assertThat(WatchlistPoolRuntimeSourceReadAdapter.class.getDeclaredMethods()).isEmpty();
    }

    @Test
    void forWatchlistPoolKeepsSafeDefaultsAndNoExecutionFields() {
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                "BTCUSDT",
                "unit-test",
                "manual-review"
        );

        assertThat(request.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(request.getWatchlistPoolOnly()).isTrue();
        assertThat(request.getRequestedBy()).isEqualTo("unit-test");
        assertThat(request.getRequestReason()).isEqualTo("manual-review");
        assertThat(request.getManualReviewRequired()).isTrue();
        assertThat(request.getNotTradeInstruction()).isTrue();
        assertThat(request.getMissingFields()).isEmpty();
        assertThat(request.getBlockingReasons()).isEmpty();
        assertRequestDeclaresNoExecutionFields();
    }

    @Test
    void incompleteRequestKeepsSafeDefaultsAndDefensiveCopiesLists() {
        List<String> missingFields = new ArrayList<>(List.of("symbol"));
        List<String> blockingReasons = new ArrayList<>(List.of("request_missing_symbol"));

        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.incomplete(
                "ETHUSDT",
                missingFields,
                blockingReasons
        );

        missingFields.add("mutated");
        blockingReasons.add("mutated");

        assertThat(request.getWatchlistPoolOnly()).isTrue();
        assertThat(request.getManualReviewRequired()).isTrue();
        assertThat(request.getNotTradeInstruction()).isTrue();
        assertThat(request.getMissingFields()).containsExactly("symbol");
        assertThat(request.getBlockingReasons()).containsExactly("request_missing_symbol", "INCOMPLETE");

        List<String> returnedMissingFields = request.getMissingFields();
        List<String> returnedBlockingReasons = request.getBlockingReasons();
        returnedMissingFields.add("mutated");
        returnedBlockingReasons.add("mutated");

        assertThat(request.getMissingFields()).containsExactly("symbol");
        assertThat(request.getBlockingReasons()).containsExactly("request_missing_symbol", "INCOMPLETE");
    }

    @Test
    void sourceUnavailableResultKeepsSafeDefaults() {
        RuntimeSourceReadResultDTO result = RuntimeSourceReadResultDTO.sourceUnavailable(
                "SOLUSDT",
                List.of("db_read_not_implemented")
        );

        assertThat(result.getSymbol()).isEqualTo("SOLUSDT");
        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(result.getRuntimeSource().getSourceStatus())
                .isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(result.getBlockingReasons()).contains("db_read_not_implemented", "SOURCE_UNAVAILABLE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void incompleteResultKeepsSafeDefaultsAndDefensiveCopiesLists() {
        List<String> missingFields = new ArrayList<>(List.of("sourceRef"));
        List<String> blockingReasons = new ArrayList<>(List.of("source_missing"));

        RuntimeSourceReadResultDTO result = RuntimeSourceReadResultDTO.incomplete(
                "ADAUSDT",
                missingFields,
                blockingReasons
        );

        missingFields.add("mutated");
        blockingReasons.add("mutated");

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("sourceRef");
        assertThat(result.getBlockingReasons()).containsExactly("source_missing", "INCOMPLETE");

        List<String> returnedMissingFields = result.getMissingFields();
        List<String> returnedBlockingReasons = result.getBlockingReasons();
        returnedMissingFields.add("mutated");
        returnedBlockingReasons.add("mutated");

        assertThat(result.getMissingFields()).containsExactly("sourceRef");
        assertThat(result.getBlockingReasons()).containsExactly("source_missing", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void fromRuntimeSourceWrapsSourceWithoutExecutionUpgrade() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.availableReviewOnly(
                "BNBUSDT",
                WatchlistRuntimeSourceTypeEnum.CACHE_SNAPSHOT,
                "cache:watchlist:BNBUSDT",
                List.of("review_only_source")
        );

        RuntimeSourceReadResultDTO result = RuntimeSourceReadResultDTO.fromRuntimeSource(source);

        assertThat(result.getRuntimeSource()).isSameAs(source);
        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY);
        assertThat(result.getBlockingReasons()).containsExactly("review_only_source");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void fromRuntimeSourceFailsClosedWhenSourceMissing() {
        RuntimeSourceReadResultDTO result = RuntimeSourceReadResultDTO.fromRuntimeSource(null);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("runtimeSource");
        assertThat(result.getBlockingReasons()).contains("RUNTIME_SOURCE_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void testOnlyNoOpAdapterCanReturnSafeIncompleteWithoutRuntimeRead() {
        ProductionRuntimeSourceReadAdapter adapter = request -> RuntimeSourceReadResultDTO.incomplete(
                request == null ? null : request.getSymbol(),
                List.of("adapterImplementation"),
                List.of("READ_ADAPTER_NOT_IMPLEMENTED")
        );

        RuntimeSourceReadResultDTO result = adapter.read(RuntimeSourceReadRequestDTO.forWatchlistPool(
                "XRPUSDT",
                "unit-test",
                "no-op-contract"
        ));

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("READ_ADAPTER_NOT_IMPLEMENTED", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void interfacesAndDtosDeclareNoForbiddenFields() {
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

        for (Class<?> type : List.of(
                ProductionRuntimeSourceReadAdapter.class,
                WatchlistPoolRuntimeSourceReadAdapter.class,
                RuntimeSourceReadRequestDTO.class,
                RuntimeSourceReadResultDTO.class
        )) {
            for (Field field : type.getDeclaredFields()) {
                String fieldName = field.getName();
                String fieldTypeName = field.getType().getName();
                for (String forbiddenFragment : forbiddenFragments) {
                    assertThat(fieldName).doesNotContain(forbiddenFragment);
                    assertThat(fieldTypeName).doesNotContain(forbiddenFragment);
                }
            }
        }
    }

    private void assertRequestDeclaresNoExecutionFields() {
        List<String> forbiddenFieldNames = List.of(
                "opportunityPushAllowed",
                "readinessUpgraded",
                "tradingActionCreated",
                "entryStopTpRrGenerated",
                "entry",
                "stop",
                "tp",
                "rr"
        );

        for (Field field : RuntimeSourceReadRequestDTO.class.getDeclaredFields()) {
            assertThat(forbiddenFieldNames).doesNotContain(field.getName());
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
