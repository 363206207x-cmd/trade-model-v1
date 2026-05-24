package org.example.trademodel.service.watchlistsource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeFreshnessStatusEnum;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceTypeEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistRuntimeSourceGuardValidatorTest {

    private final DefaultWatchlistRuntimeSourceGuardValidator validator =
            new DefaultWatchlistRuntimeSourceGuardValidator();

    @Test
    void nullSourceReturnsSafeIncomplete() {
        WatchlistRuntimeSourceDTO result = validator.validate(null);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SOURCE_MISSING", "NULL_SOURCE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nonWatchlistSourceReturnsBlockedNotWatchlist() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.blockedNotWatchlist(
                "BTCUSDT",
                List.of("not_in_watchlist_pool")
        );

        WatchlistRuntimeSourceDTO result = validator.validate(source);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(result.getWatchlistMember()).isFalse();
        assertThat(result.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void unknownMembershipReturnsIncomplete() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.incomplete(
                "ETHUSDT",
                List.of(),
                List.of("membership_not_loaded")
        );

        WatchlistRuntimeSourceDTO result = validator.validate(source);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getWatchlistMember()).isNull();
        assertThat(result.getMissingFields()).contains("watchlistMember");
        assertThat(result.getBlockingReasons()).contains("WATCHLIST_MEMBERSHIP_UNKNOWN");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void missingFieldsReturnIncomplete() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.incomplete(
                "SOLUSDT",
                List.of("sourceRef", "sourceUpdatedAt"),
                List.of("partial_source")
        );

        WatchlistRuntimeSourceDTO result = validator.validate(source);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getMissingFields()).containsExactly("sourceRef", "sourceUpdatedAt");
        assertThat(result.getBlockingReasons()).contains(
                "MISSING_FIELDS",
                "sourceRef",
                "sourceUpdatedAt",
                "INCOMPLETE"
        );
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void sourceUnavailableReturnsSafeUnavailableOrIncomplete() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.sourceUnavailable(
                "ADAUSDT",
                List.of("runtime_source_missing")
        );

        WatchlistRuntimeSourceDTO result = validator.validate(source);

        assertThat(result.getSourceStatus())
                .isIn(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE,
                        WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SOURCE_UNAVAILABLE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void staleReviewOnlySourceReturnsStaleReviewOnly() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.staleReviewOnly(
                "BNBUSDT",
                List.of("sourceUpdatedAt"),
                List.of("stale_source")
        );

        WatchlistRuntimeSourceDTO result = validator.validate(source);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.STALE_REVIEW_ONLY);
        assertThat(result.getFreshnessStatus()).isEqualTo(WatchlistRuntimeFreshnessStatusEnum.STALE);
        assertThat(result.getStaleFields()).containsExactly("sourceUpdatedAt");
        assertThat(result.getBlockingReasons()).contains("STALE_REVIEW_ONLY");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void unsafeFreshnessStatesBlockAutomation() throws Exception {
        List<WatchlistRuntimeSourceDTO> results = List.of(
                validator.validate(sourceWithFreshness("UNKNOWNUSDT", WatchlistRuntimeFreshnessStatusEnum.UNKNOWN)),
                validator.validate(sourceWithFreshness(
                        "NOTAVAILABLEUSDT",
                        WatchlistRuntimeFreshnessStatusEnum.NOT_AVAILABLE
                )),
                validator.validate(sourceWithFreshness("EXPIREDUSDT", WatchlistRuntimeFreshnessStatusEnum.EXPIRED))
        );

        assertThat(results).extracting(WatchlistRuntimeSourceDTO::getSourceStatus)
                .containsOnly(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);

        for (WatchlistRuntimeSourceDTO result : results) {
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void freshStillDoesNotAllowPushReadinessOrTrading() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.availableReviewOnly(
                "XRPUSDT",
                WatchlistRuntimeSourceTypeEnum.CACHE_SNAPSHOT,
                "cache:watchlist:XRPUSDT",
                List.of("fresh_review_only")
        );

        WatchlistRuntimeSourceDTO result = validator.validate(source);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY);
        assertThat(result.getFreshnessStatus()).isEqualTo(WatchlistRuntimeFreshnessStatusEnum.FRESH);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void allOutputsPreserveSafeNoExecutionDefaults() throws Exception {
        List<WatchlistRuntimeSourceDTO> results = List.of(
                validator.validate(null),
                validator.validate(WatchlistRuntimeSourceDTO.blockedNotWatchlist("BTCUSDT", List.of("blocked"))),
                validator.validate(WatchlistRuntimeSourceDTO.incomplete(
                        "ETHUSDT",
                        List.of(),
                        List.of("unknown")
                )),
                validator.validate(WatchlistRuntimeSourceDTO.incomplete(
                        "SOLUSDT",
                        List.of("sourceRef"),
                        List.of("missing")
                )),
                validator.validate(WatchlistRuntimeSourceDTO.sourceUnavailable("ADAUSDT", List.of("unavailable"))),
                validator.validate(WatchlistRuntimeSourceDTO.staleReviewOnly(
                        "BNBUSDT",
                        List.of("sourceUpdatedAt"),
                        List.of("stale")
                )),
                validator.validate(WatchlistRuntimeSourceDTO.availableReviewOnly(
                        "XRPUSDT",
                        WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT,
                        "manual:XRPUSDT",
                        List.of("fresh")
                )),
                validator.validate(sourceWithFreshness("EXPIREDUSDT", WatchlistRuntimeFreshnessStatusEnum.EXPIRED))
        );

        for (WatchlistRuntimeSourceDTO result : results) {
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void defaultValidatorDeclaresNoForbiddenWiringFields() {
        List<String> forbiddenFragments = List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "Mapper",
                "Controller",
                "Scheduler",
                "PushRecheckService",
                "PushSnapshotService",
                "ExternalRuntimeService"
        );

        for (Field field : DefaultWatchlistRuntimeSourceGuardValidator.class.getDeclaredFields()) {
            String fieldName = field.getName();
            String fieldTypeName = field.getType().getName();
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(fieldName).doesNotContain(forbiddenFragment);
                assertThat(fieldTypeName).doesNotContain(forbiddenFragment);
            }
        }
    }

    private WatchlistRuntimeSourceDTO sourceWithFreshness(
            String symbol,
            WatchlistRuntimeFreshnessStatusEnum freshnessStatus
    ) throws Exception {
        Constructor<WatchlistRuntimeSourceDTO> constructor = WatchlistRuntimeSourceDTO.class.getDeclaredConstructor(
                String.class,
                Boolean.class,
                String.class,
                WatchlistRuntimeSourceTypeEnum.class,
                String.class,
                String.class,
                String.class,
                WatchlistRuntimeFreshnessStatusEnum.class,
                WatchlistRuntimeSourceStatusEnum.class,
                String.class,
                String.class,
                List.class,
                List.class,
                List.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(
                symbol,
                true,
                "manual-review",
                WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT,
                "manual:" + symbol,
                null,
                null,
                freshnessStatus,
                WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY,
                "UNKNOWN",
                "REVIEW_ONLY",
                List.of(),
                List.of("sourceUpdatedAt"),
                List.of("freshness_guard_fixture")
        );
    }

    private void assertSafeNoExecutionDefaults(WatchlistRuntimeSourceDTO result) {
        assertThat(result.getManualReviewRequired()).isTrue();
        assertThat(result.getNotTradeInstruction()).isTrue();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertThat(result.getReadinessUpgraded()).isFalse();
        assertThat(result.getTradingActionCreated()).isFalse();
        assertThat(result.getEntryStopTpRrGenerated()).isFalse();
    }
}
