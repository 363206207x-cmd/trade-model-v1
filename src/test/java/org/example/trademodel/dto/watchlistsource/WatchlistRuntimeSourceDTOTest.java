package org.example.trademodel.dto.watchlistsource;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class WatchlistRuntimeSourceDTOTest {

    @Test
    void enumsShouldContainOnlyAuthorizedValues() {
        assertThat(Arrays.stream(WatchlistRuntimeSourceTypeEnum.values()).map(Enum::name))
                .containsExactly(
                        "WATCHLIST_CONFIG",
                        "DB_WATCHLIST_READ",
                        "CACHE_SNAPSHOT",
                        "MARKET_QUOTE_CLIENT",
                        "SCHEDULER_TRIGGER",
                        "MANUAL_REVIEW_INPUT",
                        "UNKNOWN"
                );
        assertThat(Arrays.stream(WatchlistRuntimeFreshnessStatusEnum.values()).map(Enum::name))
                .containsExactly(
                        "FRESH",
                        "STALE",
                        "EXPIRED",
                        "UNKNOWN",
                        "NOT_AVAILABLE"
                );
        assertThat(Arrays.stream(WatchlistRuntimeSourceStatusEnum.values()).map(Enum::name))
                .containsExactly(
                        "AVAILABLE_REVIEW_ONLY",
                        "BLOCKED_NOT_WATCHLIST",
                        "INCOMPLETE",
                        "STALE_REVIEW_ONLY",
                        "SOURCE_UNAVAILABLE",
                        "NOT_IMPLEMENTED"
                );
    }

    @Test
    void blockedNotWatchlistShouldRemainSafe() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.blockedNotWatchlist(
                "BTCUSDT",
                List.of("not_in_watchlist_pool")
        );

        assertThat(source.getWatchlistMember()).isFalse();
        assertThat(source.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertThat(source.getDataQualityStatus()).isEqualTo("BLOCKED");
        assertThat(source.getBlockingReasons())
                .containsExactly("not_in_watchlist_pool", "BLOCKED_NOT_WATCHLIST");
        assertSafeNoExecutionDefaults(source);
    }

    @Test
    void incompleteShouldRemainSafe() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.incomplete(
                "ETHUSDT",
                List.of("watchlistMember", "sourceRef"),
                List.of("membership_unknown")
        );

        assertThat(source.getWatchlistMember()).isNull();
        assertThat(source.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(source.getDataQualityStatus()).isEqualTo("INCOMPLETE");
        assertThat(source.getMissingFields()).containsExactly("watchlistMember", "sourceRef");
        assertThat(source.getBlockingReasons()).containsExactly("membership_unknown", "INCOMPLETE");
        assertSafeNoExecutionDefaults(source);
    }

    @Test
    void staleReviewOnlyShouldRemainSafe() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.staleReviewOnly(
                "SOLUSDT",
                List.of("sourceUpdatedAt"),
                List.of("source_stale")
        );

        assertThat(source.getWatchlistMember()).isTrue();
        assertThat(source.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.STALE_REVIEW_ONLY);
        assertThat(source.getFreshnessStatus()).isEqualTo(WatchlistRuntimeFreshnessStatusEnum.STALE);
        assertThat(source.getStaleStatus()).isEqualTo("REVIEW_ONLY");
        assertThat(source.getStaleFields()).containsExactly("sourceUpdatedAt");
        assertSafeNoExecutionDefaults(source);
    }

    @Test
    void sourceUnavailableShouldRemainSafe() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.sourceUnavailable(
                "ADAUSDT",
                List.of("runtime_source_missing")
        );

        assertThat(source.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.SOURCE_UNAVAILABLE);
        assertThat(source.getFreshnessStatus()).isEqualTo(WatchlistRuntimeFreshnessStatusEnum.NOT_AVAILABLE);
        assertThat(source.getDataQualityStatus()).isEqualTo("INCOMPLETE");
        assertThat(source.getMissingFields()).containsExactly("sourceRef");
        assertThat(source.getBlockingReasons()).containsExactly("runtime_source_missing", "SOURCE_UNAVAILABLE");
        assertSafeNoExecutionDefaults(source);
    }

    @Test
    void availableReviewOnlyShouldRemainSafeAndNotAllowPushReadinessOrTrading() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.availableReviewOnly(
                "BNBUSDT",
                WatchlistRuntimeSourceTypeEnum.CACHE_SNAPSHOT,
                "cache:watchlist:BNBUSDT",
                List.of("review_only_source")
        );

        assertThat(source.getWatchlistMember()).isTrue();
        assertThat(source.getSourceType()).isEqualTo(WatchlistRuntimeSourceTypeEnum.CACHE_SNAPSHOT);
        assertThat(source.getSourceRef()).isEqualTo("cache:watchlist:BNBUSDT");
        assertThat(source.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.AVAILABLE_REVIEW_ONLY);
        assertThat(source.getFreshnessStatus()).isEqualTo(WatchlistRuntimeFreshnessStatusEnum.FRESH);
        assertSafeNoExecutionDefaults(source);
    }

    @Test
    void freshShouldNotMeanPushReadinessOrTrading() {
        WatchlistRuntimeSourceDTO source = WatchlistRuntimeSourceDTO.availableReviewOnly(
                "XRPUSDT",
                WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT,
                "manual:XRPUSDT",
                List.of("fresh_review_only")
        );

        assertThat(source.getFreshnessStatus()).isEqualTo(WatchlistRuntimeFreshnessStatusEnum.FRESH);
        assertThat(source.getOpportunityPushAllowed()).isFalse();
        assertThat(source.getReadinessUpgraded()).isFalse();
        assertThat(source.getTradingActionCreated()).isFalse();
        assertThat(source.getEntryStopTpRrGenerated()).isFalse();
    }

    @Test
    void availableReviewOnlyShouldFailClosedWhenSourceIsMissing() {
        WatchlistRuntimeSourceDTO missingType = WatchlistRuntimeSourceDTO.availableReviewOnly(
                "DOGEUSDT",
                WatchlistRuntimeSourceTypeEnum.UNKNOWN,
                "manual:DOGEUSDT",
                List.of("unknown_type")
        );
        WatchlistRuntimeSourceDTO missingRef = WatchlistRuntimeSourceDTO.availableReviewOnly(
                "DOGEUSDT",
                WatchlistRuntimeSourceTypeEnum.MANUAL_REVIEW_INPUT,
                " ",
                List.of("blank_ref")
        );

        assertThat(missingType.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(missingType.getMissingFields()).containsExactly("sourceType");
        assertThat(missingType.getBlockingReasons()).contains("MISSING_SOURCE", "INCOMPLETE");
        assertThat(missingRef.getSourceStatus()).isEqualTo(WatchlistRuntimeSourceStatusEnum.INCOMPLETE);
        assertThat(missingRef.getMissingFields()).containsExactly("sourceRef");
        assertThat(missingRef.getBlockingReasons()).contains("MISSING_SOURCE", "INCOMPLETE");
        assertSafeNoExecutionDefaults(missingType);
        assertSafeNoExecutionDefaults(missingRef);
    }

    @Test
    void listsShouldBeDefensivelyCopied() {
        List<String> missingFields = new ArrayList<>(List.of("sourceRef"));
        List<String> staleFields = new ArrayList<>(List.of("sourceUpdatedAt"));
        List<String> blockingReasons = new ArrayList<>(List.of("partial_source"));

        WatchlistRuntimeSourceDTO incomplete = WatchlistRuntimeSourceDTO.incomplete(
                "AVAXUSDT",
                missingFields,
                blockingReasons
        );
        WatchlistRuntimeSourceDTO stale = WatchlistRuntimeSourceDTO.staleReviewOnly(
                "AVAXUSDT",
                staleFields,
                blockingReasons
        );

        missingFields.add("mutated");
        staleFields.add("mutated");
        blockingReasons.add("mutated");

        assertThat(incomplete.getMissingFields()).containsExactly("sourceRef");
        assertThat(stale.getStaleFields()).containsExactly("sourceUpdatedAt");
        assertThat(incomplete.getBlockingReasons()).containsExactly("partial_source", "INCOMPLETE");

        List<String> returnedMissingFields = incomplete.getMissingFields();
        List<String> returnedStaleFields = stale.getStaleFields();
        List<String> returnedBlockingReasons = stale.getBlockingReasons();
        returnedMissingFields.add("mutated");
        returnedStaleFields.add("mutated");
        returnedBlockingReasons.add("mutated");

        assertThat(incomplete.getMissingFields()).containsExactly("sourceRef");
        assertThat(stale.getStaleFields()).containsExactly("sourceUpdatedAt");
        assertThat(stale.getBlockingReasons()).containsExactly("partial_source");
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
                WatchlistRuntimeSourceDTO.class,
                WatchlistRuntimeSourceStatusEnum.class,
                WatchlistRuntimeSourceTypeEnum.class,
                WatchlistRuntimeFreshnessStatusEnum.class
        )) {
            for (Field field : type.getDeclaredFields()) {
                String fieldTypeName = field.getType().getName();
                for (String forbiddenTerm : forbiddenFieldTypeTerms) {
                    assertThat(fieldTypeName).doesNotContain(forbiddenTerm);
                }
            }
        }
    }

    private void assertSafeNoExecutionDefaults(WatchlistRuntimeSourceDTO source) {
        assertThat(source.getManualReviewRequired()).isTrue();
        assertThat(source.getNotTradeInstruction()).isTrue();
        assertThat(source.getOpportunityPushAllowed()).isFalse();
        assertThat(source.getReadinessUpgraded()).isFalse();
        assertThat(source.getTradingActionCreated()).isFalse();
        assertThat(source.getEntryStopTpRrGenerated()).isFalse();
    }
}
