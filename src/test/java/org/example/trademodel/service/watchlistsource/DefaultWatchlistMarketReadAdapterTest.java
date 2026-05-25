package org.example.trademodel.service.watchlistsource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistMarketReadAdapterTest {

    private final DefaultWatchlistMarketReadAdapter adapter = new DefaultWatchlistMarketReadAdapter();

    @Test
    void nullRequestFailsClosed() {
        RuntimeSourceReadResultDTO result = adapter.readMarket(null);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("request");
        assertThat(result.getBlockingReasons()).contains("MARKET_READ_REQUEST_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nonWatchlistPoolOnlyRequestFailsClosed() throws Exception {
        RuntimeSourceReadRequestDTO request = requestWithWatchlistPoolOnly("BTCUSDT", false);

        RuntimeSourceReadResultDTO result = adapter.readMarket(request);

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("watchlistPoolOnly");
        assertThat(result.getBlockingReasons()).contains("WATCHLIST_POOL_ONLY_REQUIRED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void blankSymbolFailsClosed() {
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                " ",
                "unit-test",
                "market-read"
        );

        RuntimeSourceReadResultDTO result = adapter.readMarket(request);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("symbol");
        assertThat(result.getBlockingReasons()).contains("SYMBOL_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void validWatchlistRequestReturnsSourceUnavailableNoOp() {
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                "ETHUSDT",
                "unit-test",
                "market-read"
        );

        RuntimeSourceReadResultDTO result = adapter.readMarket(request);

        assertThat(result.getSymbol()).isEqualTo("ETHUSDT");
        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(result.getBlockingReasons())
                .contains("MARKET_READ_ADAPTER_NO_OP", "MARKET_CLIENT_NOT_CONNECTED", "SOURCE_UNAVAILABLE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void defaultAdapterHasNoRuntimeLiveExternalDataDependencies() {
        Constructor<?>[] constructors = DefaultWatchlistMarketReadAdapter.class.getDeclaredConstructors();

        assertThat(constructors).hasSize(1);
        assertThat(constructors[0].getParameterCount()).isZero();
        for (Field field : DefaultWatchlistMarketReadAdapter.class.getDeclaredFields()) {
            assertThat(field.getType()).isEqualTo(String.class);
            assertThat(Modifier.isStatic(field.getModifiers())).isTrue();
            assertThat(Modifier.isFinal(field.getModifiers())).isTrue();
        }
    }

    @Test
    void defaultAdapterDeclaresNoMarketClientFields() {
        assertNoForbiddenFields("MarketQuoteClient", "BinanceMarketQuoteClient");
    }

    @Test
    void defaultAdapterDeclaresNoSchedulerFieldsOrMethods() {
        assertNoForbiddenFields("Scheduler", "Scheduled");
        assertNoForbiddenMethods("Scheduler", "Scheduled");
    }

    @Test
    void defaultAdapterDeclaresNoDatasourceOrJdbcTemplateFields() {
        assertNoForbiddenFields("DataSource", "JdbcTemplate");
    }

    @Test
    void outputsDoNotCarryScorePushReadinessOrTradingSemantics() {
        List<RuntimeSourceReadResultDTO> results = List.of(
                adapter.readMarket(null),
                adapter.readMarket(RuntimeSourceReadRequestDTO.forWatchlistPool(
                        "SOLUSDT",
                        "unit-test",
                        "market-read"
                )),
                adapter.readMarket(RuntimeSourceReadRequestDTO.forWatchlistPool(
                        " ",
                        "unit-test",
                        "market-read"
                ))
        );

        for (RuntimeSourceReadResultDTO result : results) {
            assertThat(result.getReadStatus())
                    .isIn(
                            WatchlistRuntimeSourceStatusEnum.INCOMPLETE,
                            WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE
                    );
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void adapterContractDeclaresOnlyReadMarketMethod() throws Exception {
        Method readMarket = WatchlistMarketReadAdapter.class.getDeclaredMethod(
                "readMarket",
                RuntimeSourceReadRequestDTO.class
        );

        assertThat(WatchlistMarketReadAdapter.class.isInterface()).isTrue();
        assertThat(readMarket.getReturnType()).isEqualTo(RuntimeSourceReadResultDTO.class);
        assertThat(WatchlistMarketReadAdapter.class.getDeclaredMethods())
                .extracting(Method::getName)
                .containsExactly("readMarket");
    }

    @Test
    void adapterDeclaresNoForbiddenFieldsOrMethods() {
        List<String> forbiddenFragments = List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "Scheduler",
                "Controller",
                "PushRecheckService",
                "PushSnapshotService",
                "DataSource",
                "JdbcTemplate",
                "Scheduled"
        );

        for (String forbiddenFragment : forbiddenFragments) {
            assertNoForbiddenFields(forbiddenFragment);
            assertNoForbiddenMethods(forbiddenFragment);
        }
    }

    private RuntimeSourceReadRequestDTO requestWithWatchlistPoolOnly(
            String symbol,
            Boolean watchlistPoolOnly
    ) throws Exception {
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
                watchlistPoolOnly,
                "unit-test",
                "market-read",
                List.of(),
                List.of()
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

    private static void assertNoForbiddenFields(String... forbiddenFragments) {
        for (Field field : DefaultWatchlistMarketReadAdapter.class.getDeclaredFields()) {
            String fieldName = field.getName();
            String fieldTypeName = field.getType().getName();
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(fieldName).doesNotContain(forbiddenFragment);
                assertThat(fieldTypeName).doesNotContain(forbiddenFragment);
            }
        }
    }

    private static void assertNoForbiddenMethods(String... forbiddenFragments) {
        for (Class<?> type : List.of(DefaultWatchlistMarketReadAdapter.class, WatchlistMarketReadAdapter.class)) {
            for (Method method : type.getDeclaredMethods()) {
                for (String forbiddenFragment : forbiddenFragments) {
                    assertThat(method.getName()).doesNotContain(forbiddenFragment);
                    assertThat(method.getReturnType().getName()).doesNotContain(forbiddenFragment);
                    for (Class<?> parameterType : method.getParameterTypes()) {
                        assertThat(parameterType.getName()).doesNotContain(forbiddenFragment);
                    }
                    for (Annotation annotation : method.getDeclaredAnnotations()) {
                        assertThat(annotation.annotationType().getName()).doesNotContain(forbiddenFragment);
                    }
                }
            }
        }
    }
}
