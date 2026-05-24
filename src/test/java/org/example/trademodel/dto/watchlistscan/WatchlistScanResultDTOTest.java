package org.example.trademodel.dto.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class WatchlistScanResultDTOTest {

    @Test
    void disabledShouldRemainManualReviewAndNotTradeInstruction() {
        WatchlistScanResultDTO result = WatchlistScanResultDTO.disabled("BTCUSDT", "scheduler_disabled");

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.DISABLED);
        assertThat(result.getManualReviewRequired()).isTrue();
        assertThat(result.getNotTradeInstruction()).isTrue();
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void blockedNotWatchlistShouldUseBlockedStatus() {
        WatchlistScanResultDTO result = WatchlistScanResultDTO.blockedNotWatchlist(
                "ETHUSDT",
                List.of("not_in_watchlist_pool")
        );

        assertThat(result.getWatchlistMember()).isFalse();
        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(result.getBlockingReasons())
                .containsExactly("not_in_watchlist_pool", "BLOCKED_NOT_WATCHLIST");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void incompleteAndReviewOnlyShouldUseSafeStatuses() {
        WatchlistScanResultDTO incomplete = WatchlistScanResultDTO.incomplete(
                "SOLUSDT",
                List.of("missing_core_fields")
        );
        WatchlistScanResultDTO reviewOnly = WatchlistScanResultDTO.reviewOnly(
                "SOLUSDT",
                List.of("stale_data_review_only")
        );

        assertThat(incomplete.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(incomplete.getWatchlistMember()).isNull();
        assertThat(incomplete.getBlockingReasons()).containsExactly("missing_core_fields", "INCOMPLETE");
        assertThat(reviewOnly.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(reviewOnly.getWatchlistMember()).isTrue();
        assertSafeNoExecutionDefaults(incomplete);
        assertSafeNoExecutionDefaults(reviewOnly);
    }

    @Test
    void candidateAttentionReviewOnlyShouldNotAllowOpportunityPush() {
        WatchlistScanResultDTO result = WatchlistScanResultDTO.candidateAttentionReviewOnly(
                "ADAUSDT",
                List.of("review_candidate_only")
        );

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.CANDIDATE_ATTENTION);
        assertThat(result.getCandidateAttentionAllowed()).isTrue();
        assertThat(result.getPromoteToHomeAllowed()).isFalse();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void promoteToHomeReviewOnlyShouldNotAllowOpportunityPushOrTrading() {
        WatchlistScanResultDTO result = WatchlistScanResultDTO.promoteToHomeReviewOnly(
                "BNBUSDT",
                List.of("home_review_only")
        );

        assertThat(result.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.PROMOTE_TO_HOME_REVIEW);
        assertThat(result.getCandidateAttentionAllowed()).isFalse();
        assertThat(result.getPromoteToHomeAllowed()).isTrue();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void allFactoriesShouldKeepExecutionAndTradingDefaultsClosed() {
        List<WatchlistScanResultDTO> results = List.of(
                WatchlistScanResultDTO.disabled("BTCUSDT", "disabled"),
                WatchlistScanResultDTO.blockedNotWatchlist("ETHUSDT", List.of("blocked")),
                WatchlistScanResultDTO.incomplete("SOLUSDT", List.of("incomplete")),
                WatchlistScanResultDTO.reviewOnly("ADAUSDT", List.of("review_only")),
                WatchlistScanResultDTO.candidateAttentionReviewOnly("BNBUSDT", List.of("candidate_review")),
                WatchlistScanResultDTO.promoteToHomeReviewOnly("XRPUSDT", List.of("promote_review"))
        );

        for (WatchlistScanResultDTO result : results) {
            assertSafeNoExecutionDefaults(result);
        }
    }

    @Test
    void blockingReasonsShouldBeDefensivelyCopied() {
        List<String> blockingReasons = new ArrayList<>(List.of("partial_data"));
        WatchlistScanResultDTO result = WatchlistScanResultDTO.reviewOnly("XRPUSDT", blockingReasons);

        blockingReasons.add("mutated");

        assertThat(result.getBlockingReasons()).containsExactly("partial_data");

        List<String> returnedBlockingReasons = result.getBlockingReasons();
        returnedBlockingReasons.add("mutated");

        assertThat(result.getBlockingReasons()).containsExactly("partial_data");
    }

    @Test
    void enumShouldContainOnlyAuthorizedStatuses() {
        assertThat(Arrays.stream(WatchlistScanStatusEnum.values()).map(Enum::name))
                .containsExactly(
                        "DISABLED",
                        "BLOCKED_NOT_WATCHLIST",
                        "INCOMPLETE",
                        "REVIEW_ONLY",
                        "CANDIDATE_ATTENTION",
                        "PROMOTE_TO_HOME_REVIEW",
                        "NOT_IMPLEMENTED"
                );
    }

    @Test
    void dtoShouldDeclareNoForbiddenWiringFields() {
        List<String> forbiddenFieldTypeTerms = List.of(
                "MarketQuoteClient",
                "Mapper",
                "Service",
                "Controller",
                "Scheduler"
        );

        for (Class<?> type : List.of(
                WatchlistScanResultDTO.class,
                WatchlistRuntimeSnapshotDTO.class,
                WatchlistScanStatusEnum.class
        )) {
            for (Field field : type.getDeclaredFields()) {
                String fieldTypeName = field.getType().getName();
                for (String forbiddenTerm : forbiddenFieldTypeTerms) {
                    assertThat(fieldTypeName).doesNotContain(forbiddenTerm);
                }
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
