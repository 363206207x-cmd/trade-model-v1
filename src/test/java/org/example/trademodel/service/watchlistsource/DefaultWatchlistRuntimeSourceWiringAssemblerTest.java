package org.example.trademodel.service.watchlistsource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeFreshnessStatusEnum;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceDTO;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceStatusEnum;
import org.example.trademodel.dto.watchlistsource.WatchlistRuntimeSourceTypeEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistRuntimeSourceWiringAssemblerTest {

    private final DefaultWatchlistRuntimeSourceWiringAssembler assembler =
            new DefaultWatchlistRuntimeSourceWiringAssembler();

    @Test
    void defaultAssemblerWithNullSourceReturnsSafeIncomplete() {
        WatchlistRuntimeSourceDTO result = assembler.assembleReviewOnlySource(null);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SOURCE_MISSING", "NULL_SOURCE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void defaultAssemblerBlocksNonWatchlistSource() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.blockedNotWatchlist(
                "BTCUSDT",
                List.of("not_in_watchlist_pool")
        );

        WatchlistRuntimeSourceDTO result = assembler.assembleReviewOnlySource(source);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(result.getWatchlistMember()).isFalse();
        assertThat(result.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void defaultAssemblerReturnsIncompleteForUnknownMembershipAndMissingFields() {
        WatchlistRuntimeSourceDTO unknownMembership = WatchlistRuntimeSourceDTO.incomplete(
                "ETHUSDT",
                List.of(),
                List.of("membership_not_loaded")
        );
        WatchlistRuntimeSourceDTO missingFields = WatchlistRuntimeSourceDTO.incomplete(
                "SOLUSDT",
                List.of("sourceRef", "sourceUpdatedAt"),
                List.of("partial_source")
        );

        WatchlistRuntimeSourceDTO unknownResult = assembler.assembleReviewOnlySource(unknownMembership);
        WatchlistRuntimeSourceDTO missingResult = assembler.assembleReviewOnlySource(missingFields);

        assertThat(unknownResult.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(unknownResult.getMissingFields()).contains("watchlistMember");
        assertThat(unknownResult.getBlockingReasons()).contains("WATCHLIST_MEMBERSHIP_UNKNOWN");
        assertSafeNoExecutionDefaults(unknownResult);

        assertThat(missingResult.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(missingResult.getMissingFields()).containsExactly("sourceRef", "sourceUpdatedAt");
        assertThat(missingResult.getBlockingReasons()).contains("MISSING_FIELDS");
        assertSafeNoExecutionDefaults(missingResult);
    }

    @Test
    void defaultAssemblerReturnsSourceUnavailableSafely() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.sourceUnavailable(
                "ADAUSDT",
                List.of("runtime_source_missing")
        );

        WatchlistRuntimeSourceDTO result = assembler.assembleReviewOnlySource(source);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(result.getBlockingReasons()).contains("SOURCE_UNAVAILABLE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void defaultAssemblerReturnsStaleReviewOnlySafely() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.staleReviewOnly(
                "BNBUSDT",
                List.of("sourceUpdatedAt"),
                List.of("stale_source")
        );

        WatchlistRuntimeSourceDTO result = assembler.assembleReviewOnlySource(source);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.STALE_REVIEW_ONLY);
        assertThat(result.getFreshnessStatus()).isEqualTo(WatchlistRuntimeFreshnessStatusEnum.STALE);
        assertThat(result.getStaleFields()).containsExactly("sourceUpdatedAt");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void defaultAssemblerReturnsAvailableReviewOnlySafely() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.availableReviewOnly(
                "XRPUSDT",
                WatchlistRuntimeSourceTypeEnum.CACHE_SNAPSHOT,
                "cache:watchlist:XRPUSDT",
                List.of("fresh_review_only")
        );

        WatchlistRuntimeSourceDTO result = assembler.assembleReviewOnlySource(source);

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY);
        assertThat(result.getFreshnessStatus()).isEqualTo(WatchlistRuntimeFreshnessStatusEnum.FRESH);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void customGuardCanBeInjectedAsLocalObject() {
        WatchlistRuntimeSourceGuardValidator reviewOnlyGuard = source -> WatchlistRuntimeSourceDTO.availableReviewOnly(
                "DOGEUSDT",
                WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT,
                "manual:DOGEUSDT",
                List.of("custom_local_guard")
        );
        WatchlistRuntimeSourceGuardValidator incompleteGuard = source -> WatchlistRuntimeSourceDTO.incomplete(
                "AVAXUSDT",
                List.of("sourceRef"),
                List.of("custom_incomplete_guard")
        );

        WatchlistRuntimeSourceDTO reviewOnlyResult =
                new DefaultWatchlistRuntimeSourceWiringAssembler(reviewOnlyGuard).assembleReviewOnlySource(null);
        WatchlistRuntimeSourceDTO incompleteResult =
                new DefaultWatchlistRuntimeSourceWiringAssembler(incompleteGuard).assembleReviewOnlySource(null);

        assertThat(reviewOnlyResult.getSourceStatus())
                .isEqualTo(WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY);
        assertThat(incompleteResult.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertSafeNoExecutionDefaults(reviewOnlyResult);
        assertSafeNoExecutionDefaults(incompleteResult);
    }

    @Test
    void nullGuardFailsClosed() {
        DefaultWatchlistRuntimeSourceWiringAssembler nullGuardAssembler =
                new DefaultWatchlistRuntimeSourceWiringAssembler(null);

        WatchlistRuntimeSourceDTO result = nullGuardAssembler.assembleReviewOnlySource(
                WatchlistRuntimeSourceDTO.availableReviewOnly(
                        "LINKUSDT",
                        WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT,
                        "manual:LINKUSDT",
                        List.of("input")
                )
        );

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).containsAnyOf("GUARD_MISSING", "NULL_GUARD");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void guardReturnsNullFailsClosed() {
        WatchlistRuntimeSourceGuardValidator nullReturningGuard = source -> null;
        DefaultWatchlistRuntimeSourceWiringAssembler nullResultAssembler =
                new DefaultWatchlistRuntimeSourceWiringAssembler(nullReturningGuard);

        WatchlistRuntimeSourceDTO result = nullResultAssembler.assembleReviewOnlySource(
                WatchlistRuntimeSourceDTO.availableReviewOnly(
                        "MATICUSDT",
                        WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT,
                        "manual:MATICUSDT",
                        List.of("input")
                )
        );

        assertThat(result.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("GUARD_RESULT_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void allOutputsPreserveSafeNoExecutionDefaults() {
        List<WatchlistRuntimeSourceDTO> results = List.of(
                assembler.assembleReviewOnlySource(null),
                assembler.assembleReviewOnlySource(WatchlistRuntimeSourceDTO.blockedNotWatchlist(
                        "BTCUSDT",
                        List.of("blocked")
                )),
                assembler.assembleReviewOnlySource(WatchlistRuntimeSourceDTO.incomplete(
                        "ETHUSDT",
                        List.of(),
                        List.of("unknown")
                )),
                assembler.assembleReviewOnlySource(WatchlistRuntimeSourceDTO.incomplete(
                        "SOLUSDT",
                        List.of("sourceRef"),
                        List.of("missing")
                )),
                assembler.assembleReviewOnlySource(WatchlistRuntimeSourceDTO.sourceUnavailable(
                        "ADAUSDT",
                        List.of("unavailable")
                )),
                assembler.assembleReviewOnlySource(WatchlistRuntimeSourceDTO.staleReviewOnly(
                        "BNBUSDT",
                        List.of("sourceUpdatedAt"),
                        List.of("stale")
                )),
                assembler.assembleReviewOnlySource(WatchlistRuntimeSourceDTO.availableReviewOnly(
                        "XRPUSDT",
                        WatchlistRuntimeSourceTypeEnum.CACHE_SNAPSHOT,
                        "cache:watchlist:XRPUSDT",
                        List.of("fresh")
                )),
                new DefaultWatchlistRuntimeSourceWiringAssembler(null).assembleReviewOnlySource(null),
                new DefaultWatchlistRuntimeSourceWiringAssembler(source -> null).assembleReviewOnlySource(null)
        );

        for (WatchlistRuntimeSourceDTO result : results) {
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void defaultAssemblerDeclaresNoForbiddenWiringFields() {
        List<String> forbiddenFragments = List.of(
                "MarketQuoteClient",
                "BinanceMarketQuoteClient",
                "Mapper",
                "Controller",
                "Scheduler",
                "PushRecheckService",
                "PushSnapshotService",
                "ExternalRuntimeService",
                "RuntimeDataClient"
        );

        for (Field field : DefaultWatchlistRuntimeSourceWiringAssembler.class.getDeclaredFields()) {
            String fieldName = field.getName();
            String fieldTypeName = field.getType().getName();
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(fieldName).doesNotContain(forbiddenFragment);
                assertThat(fieldTypeName).doesNotContain(forbiddenFragment);
            }
        }
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
