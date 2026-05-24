package org.example.trademodel.service.watchlistsource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadResultDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceTypeEnum;
import org.example.trademodel.entity.RuleConfigDO;
import org.example.trademodel.service.RuleConfigService;
import org.junit.jupiter.api.Test;

class RuleConfigWatchlistPoolReadAdapterTest {

    private static final String WATCHLIST_RULE_KEY = "push.watchlist.symbols";

    @Test
    void nullRequestFailsClosed() {
        RuleConfigWatchlistPoolReadAdapter adapter = new RuleConfigWatchlistPoolReadAdapter(
                new StubRuleConfigService(Map.of(WATCHLIST_RULE_KEY, ruleConfig("BTCUSDT")))
        );

        RuntimeSourceReadResultDTO result = adapter.read(null);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("request");
        assertThat(result.getBlockingReasons()).contains("REQUEST_MISSING", "WATCHLIST_POOL_READ_BLOCKED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nonWatchlistPoolOnlyRequestFailsClosed() throws Exception {
        RuleConfigWatchlistPoolReadAdapter adapter = new RuleConfigWatchlistPoolReadAdapter(
                new StubRuleConfigService(Map.of(WATCHLIST_RULE_KEY, ruleConfig("BTCUSDT")))
        );
        RuntimeSourceReadRequestDTO request = requestWithWatchlistPoolOnly("BTCUSDT", false);

        RuntimeSourceReadResultDTO result = adapter.read(request);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("watchlistPoolOnly");
        assertThat(result.getBlockingReasons()).contains("WATCHLIST_POOL_ONLY_REQUIRED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void incompleteRequestFailsClosed() {
        RuleConfigWatchlistPoolReadAdapter adapter = new RuleConfigWatchlistPoolReadAdapter(
                new StubRuleConfigService(Map.of(WATCHLIST_RULE_KEY, ruleConfig("BTCUSDT")))
        );
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.incomplete(
                "BTCUSDT",
                List.of("sourceRef"),
                List.of("source_missing")
        );

        RuntimeSourceReadResultDTO result = adapter.read(request);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("sourceRef");
        assertThat(result.getBlockingReasons()).contains("source_missing", "REQUEST_INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void missingRuleConfigServiceReturnsSourceUnavailable() {
        RuleConfigWatchlistPoolReadAdapter adapter = new RuleConfigWatchlistPoolReadAdapter(null);
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                "BTCUSDT",
                "unit-test",
                "db-watchlist-read"
        );

        RuntimeSourceReadResultDTO result = adapter.read(request);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(result.getBlockingReasons()).contains("RULE_CONFIG_SERVICE_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void ruleConfigReadThrowsExceptionReturnsSourceUnavailable() {
        RuleConfigWatchlistPoolReadAdapter adapter = new RuleConfigWatchlistPoolReadAdapter(
                new ThrowingRuleConfigService()
        );
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                "BTCUSDT",
                "unit-test",
                "db-watchlist-read"
        );

        RuntimeSourceReadResultDTO result = adapter.read(request);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(result.getBlockingReasons()).contains("RULE_CONFIG_READ_FAILED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void missingOrEmptyWatchlistConfigReturnsIncomplete() {
        List<RuleConfigService> services = List.of(
                new StubRuleConfigService(Map.of()),
                new StubRuleConfigService(Map.of(WATCHLIST_RULE_KEY, ruleConfig(" ")))
        );

        for (RuleConfigService service : services) {
            RuleConfigWatchlistPoolReadAdapter adapter = new RuleConfigWatchlistPoolReadAdapter(service);
            RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                    "BTCUSDT",
                    "unit-test",
                    "db-watchlist-read"
            );

            RuntimeSourceReadResultDTO result = adapter.read(request);

            assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
            assertThat(result.getMissingFields()).containsExactly(WATCHLIST_RULE_KEY);
            assertThat(result.getBlockingReasons()).contains("WATCHLIST_CONFIG_MISSING_OR_EMPTY");
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void symbolNotInWatchlistReturnsIncomplete() {
        RuleConfigWatchlistPoolReadAdapter adapter = new RuleConfigWatchlistPoolReadAdapter(
                new StubRuleConfigService(Map.of(WATCHLIST_RULE_KEY, ruleConfig("BTCUSDT,ETHUSDT")))
        );
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                "SOLUSDT",
                "unit-test",
                "db-watchlist-read"
        );

        RuntimeSourceReadResultDTO result = adapter.read(request);

        assertThat(result.getReadStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("symbol");
        assertThat(result.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void symbolInWatchlistReturnsAvailableReviewOnly() {
        RuleConfigWatchlistPoolReadAdapter adapter = new RuleConfigWatchlistPoolReadAdapter(
                new StubRuleConfigService(Map.of(WATCHLIST_RULE_KEY, ruleConfig("BTCUSDT,ETHUSDT")))
        );
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                "ETHUSDT",
                "unit-test",
                "db-watchlist-read"
        );

        RuntimeSourceReadResultDTO result = adapter.read(request);

        assertThat(result.getReadStatus())
                .isEqualTo(WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY);
        assertThat(result.getRuntimeSource().getSourceType())
                .isEqualTo(WatchlistRuntimeSourceTypeEnum.WATCHLIST_CONFIG);
        assertThat(result.getRuntimeSource().getSourceRef()).isEqualTo(WATCHLIST_RULE_KEY);
        assertThat(result.getBlockingReasons()).containsExactly("REVIEW_ONLY_DB_WATCHLIST_READ");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void parsingTrimsSymbolsAndMatchesCaseInsensitively() {
        RuleConfigWatchlistPoolReadAdapter adapter = new RuleConfigWatchlistPoolReadAdapter(
                new StubRuleConfigService(Map.of(WATCHLIST_RULE_KEY, ruleConfig("BTCUSDT, ethusdt")))
        );
        RuntimeSourceReadRequestDTO request = RuntimeSourceReadRequestDTO.forWatchlistPool(
                "ETHUSDT",
                "unit-test",
                "db-watchlist-read"
        );

        RuntimeSourceReadResultDTO result = adapter.read(request);

        assertThat(result.getReadStatus())
                .isEqualTo(WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void adapterDeclaresNoForbiddenFields() {
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

        for (Field field : RuleConfigWatchlistPoolReadAdapter.class.getDeclaredFields()) {
            String fieldName = field.getName();
            String fieldTypeName = field.getType().getName();
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(fieldName).doesNotContain(forbiddenFragment);
                assertThat(fieldTypeName).doesNotContain(forbiddenFragment);
            }
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
                "db-watchlist-read",
                List.of(),
                List.of()
        );
    }

    private static RuleConfigDO ruleConfig(String ruleValue) {
        RuleConfigDO ruleConfig = new RuleConfigDO();
        ruleConfig.setRuleKey(WATCHLIST_RULE_KEY);
        ruleConfig.setRuleValue(ruleValue);
        ruleConfig.setEnabled(true);
        return ruleConfig;
    }

    private static void assertSafeNoExecutionDefaults(RuntimeSourceReadResultDTO result) {
        assertThat(result.getManualReviewRequired()).isTrue();
        assertThat(result.getNotTradeInstruction()).isTrue();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertThat(result.getReadinessUpgraded()).isFalse();
        assertThat(result.getTradingActionCreated()).isFalse();
        assertThat(result.getEntryStopTpRrGenerated()).isFalse();
    }

    private static final class StubRuleConfigService implements RuleConfigService {
        private final Map<String, RuleConfigDO> ruleConfigMap;

        private StubRuleConfigService(Map<String, RuleConfigDO> ruleConfigMap) {
            this.ruleConfigMap = ruleConfigMap;
        }

        @Override
        public Map<String, RuleConfigDO> getRuleConfigMap() {
            return ruleConfigMap;
        }

        @Override
        public void reloadRules() {
            // Test stub only. No DB read.
        }
    }

    private static final class ThrowingRuleConfigService implements RuleConfigService {
        @Override
        public Map<String, RuleConfigDO> getRuleConfigMap() {
            throw new IllegalStateException("rule config unavailable");
        }

        @Override
        public void reloadRules() {
            // Test stub only. No DB read.
        }
    }
}
