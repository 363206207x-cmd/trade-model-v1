package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.WatchlistRuntimeSnapshotDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanStatusEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistScanGuardValidatorTest {

    private final DefaultWatchlistScanGuardValidator validator = new DefaultWatchlistScanGuardValidator();

    @Test
    void nullSnapshotReturnsSafeIncomplete() {
        WatchlistScanResultDTO result = validator.validate(null);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("NULL_SNAPSHOT");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nonWatchlistSnapshotReturnsBlockedNotWatchlist() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.blockedNotWatchlist(
                "BTCUSDT",
                List.of("not_in_watchlist_pool")
        );

        WatchlistScanResultDTO result = validator.validate(snapshot);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(result.getWatchlistMember()).isFalse();
        assertThat(result.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void unknownMembershipReturnsIncomplete() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.incomplete(
                "ETHUSDT",
                List.of(),
                List.of("source_trace_incomplete")
        );

        WatchlistScanResultDTO result = validator.validate(snapshot);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getWatchlistMember()).isNull();
        assertThat(result.getBlockingReasons()).contains("WATCHLIST_MEMBERSHIP_UNKNOWN");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void missingFieldsReturnIncomplete() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.incomplete(
                "SOLUSDT",
                List.of("lastUpdatedAt", "dataQualityStatus"),
                List.of("partial_data")
        );

        WatchlistScanResultDTO result = validator.validate(snapshot);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains(
                "WATCHLIST_MEMBERSHIP_UNKNOWN",
                "MISSING_FIELDS",
                "lastUpdatedAt",
                "dataQualityStatus",
                "INCOMPLETE"
        );
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void staleOrReviewOnlySnapshotReturnsReviewOnly() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.reviewOnly(
                "ADAUSDT",
                true,
                List.of("stale_data_review_only")
        );

        WatchlistScanResultDTO result = validator.validate(snapshot);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(result.getBlockingReasons()).contains("stale_data_review_only", "REVIEW_ONLY");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void safeWatchlistSnapshotStillReturnsReviewOnlyNotCandidateOrPush() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.reviewOnly(
                "BNBUSDT",
                true,
                List.of()
        );

        WatchlistScanResultDTO result = validator.validate(snapshot);

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(result.getCandidateAttentionAllowed()).isFalse();
        assertThat(result.getPromoteToHomeAllowed()).isFalse();
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void allOutputsPreserveSafeNoExecutionDefaults() {
        List<WatchlistScanResultDTO> results = List.of(
                validator.validate(null),
                validator.validate(WatchlistRuntimeSnapshotDTO.blockedNotWatchlist("BTCUSDT", List.of("blocked"))),
                validator.validate(WatchlistRuntimeSnapshotDTO.incomplete("ETHUSDT", List.of(), List.of("unknown"))),
                validator.validate(WatchlistRuntimeSnapshotDTO.incomplete(
                        "SOLUSDT",
                        List.of("watchlistMember"),
                        List.of("missing")
                )),
                validator.validate(WatchlistRuntimeSnapshotDTO.reviewOnly("ADAUSDT", true, List.of("review_only")))
        );

        for (WatchlistScanResultDTO result : results) {
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void defaultValidatorDeclaresNoForbiddenWiringFields() {
        List<String> forbiddenFragments = List.of(
                "MarketQuoteClient",
                "Mapper",
                "Controller",
                "Scheduler",
                "External",
                "Service"
        );

        for (Field field : DefaultWatchlistScanGuardValidator.class.getDeclaredFields()) {
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
