package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.BatchWatchlistScanResultEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreStatusEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistScanScoreRuleTest {

    private final DefaultWatchlistScanScoreRule rule = new DefaultWatchlistScanScoreRule();

    @Test
    void nullBatchEnvelopeFailsClosed() {
        WatchlistScanScoreDTO result = rule.evaluate("BTCUSDT", null);

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("BATCH_ENVELOPE_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void blankSymbolFailsClosed() {
        WatchlistScanScoreDTO result = rule.evaluate(" ", reviewOnlyEnvelope("BTCUSDT"));

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SYMBOL_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void batchDisabledOrIncompleteFailsClosed() {
        BatchWatchlistScanResultEnvelopeDTO envelope = BatchWatchlistScanResultEnvelopeDTO.incomplete(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT"),
                List.of("BATCH_SCAN_DISABLED_BY_DEFAULT")
        );

        WatchlistScanScoreDTO result = rule.evaluate("BTCUSDT", envelope);

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons())
                .contains("BATCH_SCAN_DISABLED_BY_DEFAULT", "INCOMPLETE", "BATCH_ENVELOPE_BLOCKED");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void emptyResultsFailsClosed() {
        BatchWatchlistScanResultEnvelopeDTO envelope = BatchWatchlistScanResultEnvelopeDTO.reviewOnly(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT"),
                List.of("BTCUSDT"),
                List.of(),
                List.of(),
                List.of()
        );

        WatchlistScanScoreDTO result = rule.evaluate("BTCUSDT", envelope);

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("BATCH_RESULTS_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void symbolNotFoundFailsClosed() {
        WatchlistScanScoreDTO result = rule.evaluate("ETHUSDT", reviewOnlyEnvelope("BTCUSDT"));

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SYMBOL_RESULT_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void unsafeSymbolResultFailsClosed() {
        BatchWatchlistScanResultEnvelopeDTO envelope = envelopeWithResults(
                List.of(WatchlistScanResultDTO.incomplete("BTCUSDT", List.of("INCOMPLETE_SYMBOL_RESULT"))),
                List.of()
        );

        WatchlistScanScoreDTO result = rule.evaluate("BTCUSDT", envelope);

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SYMBOL_RESULT_UNSAFE", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void safeReviewOnlySymbolResultReturnsReviewOnlyScoreDto() {
        WatchlistScanScoreDTO result = rule.evaluate("btcusdt", reviewOnlyEnvelope("BTCUSDT"));

        assertThat(result.getSymbol()).isEqualTo("BTCUSDT");
        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.REVIEW_ONLY);
        assertThat(result.getScoreReasons()).containsExactly("SCANSCORE_REVIEW_ONLY_SKELETON");
        assertThat(result.getBlockingReasons()).contains("AVAILABLE_REVIEW_ONLY");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void scoreValuesAreReviewOnlyPlaceholdersAndDoNotEnableAnything() {
        WatchlistScanScoreDTO result = rule.evaluate("BTCUSDT", reviewOnlyEnvelope("BTCUSDT"));

        assertThat(result.getScanScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getConfidenceScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getDataQualityScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.REVIEW_ONLY);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void allOutputsPreserveNoExecutionDefaults() {
        List<WatchlistScanScoreDTO> outputs = List.of(
                rule.evaluate("BTCUSDT", null),
                rule.evaluate(" ", reviewOnlyEnvelope("BTCUSDT")),
                rule.evaluate("BTCUSDT", reviewOnlyEnvelope("BTCUSDT")),
                WatchlistScanScoreDTO.disabled("BTCUSDT", List.of("SCANSCORE_DISABLED"))
        );

        for (WatchlistScanScoreDTO output : outputs) {
            assertSafeNoExecutionDefaults(output);
        }
    }

    @Test
    void dtoUsesDefensiveCopies() {
        List<String> scoreReasons = new ArrayList<>(List.of("score_reason"));
        List<String> blockingReasons = new ArrayList<>(List.of("blocking_reason"));

        WatchlistScanScoreDTO result = WatchlistScanScoreDTO.reviewOnly(
                "BTCUSDT",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "unit-test",
                scoreReasons,
                blockingReasons
        );

        scoreReasons.add("mutated");
        blockingReasons.add("mutated");

        assertThat(result.getScoreReasons()).containsExactly("score_reason");
        assertThat(result.getBlockingReasons()).containsExactly("blocking_reason");

        List<String> returnedScoreReasons = result.getScoreReasons();
        List<String> returnedBlockingReasons = result.getBlockingReasons();
        returnedScoreReasons.add("mutated");
        returnedBlockingReasons.add("mutated");

        assertThat(result.getScoreReasons()).containsExactly("score_reason");
        assertThat(result.getBlockingReasons()).containsExactly("blocking_reason");
    }

    @Test
    void enumHasNoTradingOrReadinessNames() {
        List<String> forbiddenNames = List.of("BUY", "SELL", "LONG", "SHORT", "READY", "EXECUTABLE");

        assertThat(Arrays.stream(WatchlistScanScoreStatusEnum.values()).map(Enum::name))
                .doesNotContainAnyElementsOf(forbiddenNames);
    }

    @Test
    void reflectionDeclaresNoForbiddenFieldsOrMethods() {
        List<String> forbidden = List.of(
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

        for (Field field : DefaultWatchlistScanScoreRule.class.getDeclaredFields()) {
            for (String token : forbidden) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        for (Method method : WatchlistScanScoreRule.class.getDeclaredMethods()) {
            assertThat(method.getName()).isEqualTo("evaluate");
            assertThat(method.getReturnType()).isEqualTo(WatchlistScanScoreDTO.class);
            for (String token : forbidden) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
        }

        for (Method method : DefaultWatchlistScanScoreRule.class.getDeclaredMethods()) {
            for (String token : forbidden) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("Scheduled");
            }
        }
    }

    private static BatchWatchlistScanResultEnvelopeDTO reviewOnlyEnvelope(String symbol) {
        return envelopeWithResults(
                List.of(WatchlistScanResultDTO.reviewOnly(symbol, List.of("AVAILABLE_REVIEW_ONLY"))),
                List.of()
        );
    }

    private static BatchWatchlistScanResultEnvelopeDTO envelopeWithResults(
            List<WatchlistScanResultDTO> results,
            List<String> blockingReasons
    ) {
        return BatchWatchlistScanResultEnvelopeDTO.reviewOnly(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT"),
                List.of("BTCUSDT"),
                List.of(),
                results,
                blockingReasons
        );
    }

    private static void assertSafeNoExecutionDefaults(WatchlistScanScoreDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isOpportunityPushAllowed()).isFalse();
        assertThat(result.isCandidateAttentionAllowed()).isFalse();
        assertThat(result.isPromoteToHomeAllowed()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
    }
}
