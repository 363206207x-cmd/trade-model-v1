package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.WatchlistRuntimeSnapshotDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanStatusEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistScanGuardWiringAssemblerTest {

    private final DefaultWatchlistScanGuardWiringAssembler assembler =
            new DefaultWatchlistScanGuardWiringAssembler();

    @Test
    void defaultAssemblerWithNullSnapshotReturnsSafeIncomplete() {
        WatchlistScanResultDTO result = assembler.assembleReviewOnlyResult(null);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("NULL_SNAPSHOT");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void defaultAssemblerBlocksNonWatchlistSnapshot() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.blockedNotWatchlist(
                "BTCUSDT",
                List.of("not_in_watchlist")
        );

        WatchlistScanResultDTO result = assembler.assembleReviewOnlyResult(snapshot);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(result.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void defaultAssemblerReturnsIncompleteForUnknownMembershipAndMissingFields() {
        WatchlistRuntimeSnapshotDTO unknownMembership = WatchlistRuntimeSnapshotDTO.incomplete(
                "ETHUSDT",
                List.of(),
                List.of("source_trace_incomplete")
        );
        WatchlistRuntimeSnapshotDTO missingFields = WatchlistRuntimeSnapshotDTO.incomplete(
                "SOLUSDT",
                List.of("watchlistMember", "dataQualityStatus"),
                List.of("partial_data")
        );

        WatchlistScanResultDTO unknownResult = assembler.assembleReviewOnlyResult(unknownMembership);
        WatchlistScanResultDTO missingFieldsResult = assembler.assembleReviewOnlyResult(missingFields);

        assertThat(unknownResult.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(unknownResult.getBlockingReasons()).contains("WATCHLIST_MEMBERSHIP_UNKNOWN");
        assertThat(missingFieldsResult.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(missingFieldsResult.getBlockingReasons()).contains("MISSING_FIELDS", "watchlistMember");
        assertSafeNoExecutionDefaults(unknownResult);
        assertSafeNoExecutionDefaults(missingFieldsResult);
    }

    @Test
    void defaultAssemblerReturnsReviewOnlyForReviewOnlySnapshot() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.reviewOnly(
                "ADAUSDT",
                true,
                List.of("stale_data_review_only")
        );

        WatchlistScanResultDTO result = assembler.assembleReviewOnlyResult(snapshot);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(result.getBlockingReasons()).contains("REVIEW_ONLY", "stale_data_review_only");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void customGuardCanBeInjectedAsLocalObjectAndStillPreservesNoExecutionDefaults() {
        WatchlistScanGuardValidator customGuard = snapshot -> WatchlistScanResultDTO.reviewOnly(
                "CUSTOM",
                List.of("custom_guard_review_only")
        );
        DefaultWatchlistScanGuardWiringAssembler customAssembler =
                new DefaultWatchlistScanGuardWiringAssembler(customGuard);

        WatchlistScanResultDTO result = customAssembler.assembleReviewOnlyResult(
                WatchlistRuntimeSnapshotDTO.reviewOnly("CUSTOM", true, List.of())
        );

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(result.getBlockingReasons()).contains("custom_guard_review_only");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nullGuardFailsClosed() {
        DefaultWatchlistScanGuardWiringAssembler nullGuardAssembler =
                new DefaultWatchlistScanGuardWiringAssembler(null);

        WatchlistScanResultDTO result = nullGuardAssembler.assembleReviewOnlyResult(
                WatchlistRuntimeSnapshotDTO.reviewOnly("BNBUSDT", true, List.of())
        );

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("GUARD_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void guardReturningNullFailsClosed() {
        DefaultWatchlistScanGuardWiringAssembler nullResultAssembler =
                new DefaultWatchlistScanGuardWiringAssembler(snapshot -> null);

        WatchlistScanResultDTO result = nullResultAssembler.assembleReviewOnlyResult(
                WatchlistRuntimeSnapshotDTO.reviewOnly("DOTUSDT", true, List.of())
        );

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("GUARD_RESULT_MISSING");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void allOutputsPreserveSafeNoExecutionDefaults() {
        List<WatchlistScanResultDTO> results = List.of(
                assembler.assembleReviewOnlyResult(null),
                assembler.assembleReviewOnlyResult(
                        WatchlistRuntimeSnapshotDTO.blockedNotWatchlist("BTCUSDT", List.of("blocked"))
                ),
                assembler.assembleReviewOnlyResult(
                        WatchlistRuntimeSnapshotDTO.incomplete("ETHUSDT", List.of(), List.of("unknown"))
                ),
                assembler.assembleReviewOnlyResult(
                        WatchlistRuntimeSnapshotDTO.incomplete(
                                "SOLUSDT",
                                List.of("watchlistMember"),
                                List.of("missing")
                        )
                ),
                assembler.assembleReviewOnlyResult(
                        WatchlistRuntimeSnapshotDTO.reviewOnly("ADAUSDT", true, List.of("review_only"))
                ),
                new DefaultWatchlistScanGuardWiringAssembler(null).assembleReviewOnlyResult(null),
                new DefaultWatchlistScanGuardWiringAssembler(snapshot -> null).assembleReviewOnlyResult(null)
        );

        for (WatchlistScanResultDTO result : results) {
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void defaultAssemblerDeclaresNoForbiddenRuntimeFields() {
        List<String> forbiddenFragments = List.of(
                "MarketQuoteClient",
                "Mapper",
                "Controller",
                "Scheduler",
                "BinanceMarketQuoteClient",
                "PushRecheckService",
                "PushSnapshotService",
                "ExternalRuntimeService"
        );

        for (Field field : DefaultWatchlistScanGuardWiringAssembler.class.getDeclaredFields()) {
            String fieldName = field.getName();
            String fieldTypeName = field.getType().getName();
            for (String forbiddenFragment : forbiddenFragments) {
                assertThat(fieldName).doesNotContain(forbiddenFragment);
                assertThat(fieldTypeName).doesNotContain(forbiddenFragment);
            }
        }
    }

    private void assertSafeNoExecutionDefaults(WatchlistScanResultDTO result) {
        assertThat(result.getManualReviewRequired()).isTrue();
        assertThat(result.getNotTradeInstruction()).isTrue();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertThat(result.getEntryStopTpRrGenerated()).isFalse();
        assertThat(result.getReadinessUpgraded()).isFalse();
        assertThat(result.getTradingActionCreated()).isFalse();
    }
}
