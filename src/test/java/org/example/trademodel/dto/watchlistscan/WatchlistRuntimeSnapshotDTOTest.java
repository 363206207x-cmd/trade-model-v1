package org.example.trademodel.dto.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WatchlistRuntimeSnapshotDTOTest {

    @Test
    void blockedNotWatchlistShouldRemainManualReviewAndNotTradeInstruction() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.blockedNotWatchlist(
                "BTCUSDT",
                List.of("not_in_watchlist_pool")
        );

        assertThat(snapshot.getManualReviewRequired()).isTrue();
        assertThat(snapshot.getNotTradeInstruction()).isTrue();
        assertThat(snapshot.getWatchlistMember()).isFalse();
        assertThat(snapshot.getDataQualityStatus()).isEqualTo("BLOCKED");
        assertThat(snapshot.getBlockingReasons())
                .containsExactly("not_in_watchlist_pool", "BLOCKED_NOT_WATCHLIST");
    }

    @Test
    void incompleteShouldKeepUnknownMembershipAndMissingDataSafe() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.incomplete(
                "ETHUSDT",
                List.of("watchlistMember", "lastUpdatedAt"),
                List.of("membership_unknown")
        );

        assertThat(snapshot.getWatchlistMember()).isNull();
        assertThat(snapshot.getDataQualityStatus()).isEqualTo("INCOMPLETE");
        assertThat(snapshot.getMissingFields()).containsExactly("watchlistMember", "lastUpdatedAt");
        assertThat(snapshot.getBlockingReasons())
                .containsExactly("membership_unknown", "WATCHLIST_MEMBERSHIP_UNKNOWN");
        assertThat(snapshot.getManualReviewRequired()).isTrue();
        assertThat(snapshot.getNotTradeInstruction()).isTrue();
    }

    @Test
    void reviewOnlyShouldNotRepresentTradeInstruction() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.reviewOnly(
                "SOLUSDT",
                true,
                List.of("stale_data_review_only")
        );

        assertThat(snapshot.getWatchlistMember()).isTrue();
        assertThat(snapshot.getDataQualityStatus()).isEqualTo("REVIEW_ONLY");
        assertThat(snapshot.getStaleStatus()).isEqualTo("REVIEW_ONLY");
        assertThat(snapshot.getManualReviewRequired()).isTrue();
        assertThat(snapshot.getNotTradeInstruction()).isTrue();
    }

    @Test
    void reviewOnlyShouldFailClosedForUnknownMembership() {
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.reviewOnly(
                "ADAUSDT",
                null,
                List.of("source_unknown")
        );

        assertThat(snapshot.getWatchlistMember()).isNull();
        assertThat(snapshot.getDataQualityStatus()).isEqualTo("INCOMPLETE");
        assertThat(snapshot.getMissingFields()).containsExactly("watchlistMember");
        assertThat(snapshot.getBlockingReasons()).contains("source_unknown", "WATCHLIST_MEMBERSHIP_UNKNOWN");
        assertThat(snapshot.getManualReviewRequired()).isTrue();
        assertThat(snapshot.getNotTradeInstruction()).isTrue();
    }

    @Test
    void missingFieldsShouldBeDefensivelyCopied() {
        List<String> missingFields = new ArrayList<>(List.of("dataQualityStatus"));
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.incomplete(
                "BNBUSDT",
                missingFields,
                List.of("partial_data")
        );

        missingFields.add("mutated");

        assertThat(snapshot.getMissingFields()).containsExactly("dataQualityStatus");

        List<String> returnedMissingFields = snapshot.getMissingFields();
        returnedMissingFields.add("mutated");

        assertThat(snapshot.getMissingFields()).containsExactly("dataQualityStatus");
    }

    @Test
    void blockingReasonsShouldBeDefensivelyCopied() {
        List<String> blockingReasons = new ArrayList<>(List.of("stale_snapshot"));
        WatchlistRuntimeSnapshotDTO snapshot = WatchlistRuntimeSnapshotDTO.reviewOnly(
                "XRPUSDT",
                true,
                blockingReasons
        );

        blockingReasons.add("mutated");

        assertThat(snapshot.getBlockingReasons()).containsExactly("stale_snapshot");

        List<String> returnedBlockingReasons = snapshot.getBlockingReasons();
        returnedBlockingReasons.add("mutated");

        assertThat(snapshot.getBlockingReasons()).containsExactly("stale_snapshot");
    }
}
