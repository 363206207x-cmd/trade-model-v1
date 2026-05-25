package org.example.trademodel.service.watchlistscan;

import org.example.trademodel.dto.watchlistscan.BatchWatchlistScanResultEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanStatusEnum;
import org.example.trademodel.dto.watchlistsource.RuntimeSourceReadRequestDTO;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBatchWatchlistScanOrchestratorTest {

    @Test
    void disabledDefaultFailsClosed() {
        DefaultBatchWatchlistScanOrchestrator orchestrator = new DefaultBatchWatchlistScanOrchestrator(
                request -> WatchlistScanResultDTO.reviewOnly(request.getSymbol(), List.of("AVAILABLE_REVIEW_ONLY")),
                false
        );

        BatchWatchlistScanResultEnvelopeDTO result = orchestrator.scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT")
        );

        assertThat(result.getBlockingReasons()).contains("BATCH_SCAN_DISABLED_BY_DEFAULT", "INCOMPLETE");
        assertThat(result.getResults()).isEmpty();
        assertSafeEnvelope(result);
    }

    @Test
    void nullRequestedSymbolsFailsClosed() {
        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(alwaysReviewOnly()).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                null
        );

        assertThat(result.getBlockingReasons()).contains("REQUESTED_SYMBOLS_MISSING", "INCOMPLETE");
        assertSafeEnvelope(result);
    }

    @Test
    void emptyRequestedSymbolsFailsClosed() {
        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(alwaysReviewOnly()).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of()
        );

        assertThat(result.getBlockingReasons()).contains("REQUESTED_SYMBOLS_MISSING", "INCOMPLETE");
        assertSafeEnvelope(result);
    }

    @Test
    void missingSingleSymbolOrchestratorFailsClosed() {
        BatchWatchlistScanResultEnvelopeDTO result = new DefaultBatchWatchlistScanOrchestrator(null, true).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT")
        );

        assertThat(result.getBlockingReasons()).contains("SINGLE_SYMBOL_ORCHESTRATOR_MISSING", "INCOMPLETE");
        assertSafeEnvelope(result);
    }

    @Test
    void blankSymbolRejected() {
        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(alwaysReviewOnly()).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of(" ")
        );

        assertThat(result.getAcceptedSymbols()).isEmpty();
        assertThat(result.getInvalidSymbols()).contains(" ");
        assertThat(result.getRejectedSymbols()).contains(" ");
        assertThat(result.getBlockingReasons()).contains("INVALID_SYMBOL", "ALL_SYMBOLS_REJECTED");
        assertSafeEnvelope(result);
    }

    @Test
    void duplicateSymbolDedupedAndRecorded() {
        CapturingSingleSymbolOrchestrator single = new CapturingSingleSymbolOrchestrator(
                request -> WatchlistScanResultDTO.reviewOnly(request.getSymbol(), List.of("AVAILABLE_REVIEW_ONLY"))
        );

        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(single).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("btcusdt", " BTCUSDT ")
        );

        assertThat(result.getAcceptedSymbols()).containsExactly("BTCUSDT");
        assertThat(result.getDuplicateSymbols()).containsExactly("BTCUSDT");
        assertThat(result.getRejectedSymbols()).containsExactly("BTCUSDT");
        assertThat(result.getBlockingReasons()).contains("DUPLICATE_SYMBOL");
        assertThat(single.requests).hasSize(1);
        assertSafeEnvelopeAndResults(result);
    }

    @Test
    void symbolsNormalizedUppercase() {
        CapturingSingleSymbolOrchestrator single = new CapturingSingleSymbolOrchestrator(
                request -> WatchlistScanResultDTO.reviewOnly(request.getSymbol(), List.of("AVAILABLE_REVIEW_ONLY"))
        );

        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(single).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of(" ethusdt ")
        );

        assertThat(result.getAcceptedSymbols()).containsExactly("ETHUSDT");
        assertThat(single.requests).extracting(RuntimeSourceReadRequestDTO::getSymbol).containsExactly("ETHUSDT");
        assertSafeEnvelopeAndResults(result);
    }

    @Test
    void noDefaultSixInjection() {
        CapturingSingleSymbolOrchestrator single = new CapturingSingleSymbolOrchestrator(
                request -> WatchlistScanResultDTO.reviewOnly(request.getSymbol(), List.of("AVAILABLE_REVIEW_ONLY"))
        );

        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(single).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT")
        );

        assertThat(result.getAcceptedSymbols()).containsExactly("BTCUSDT");
        assertThat(single.requests).hasSize(1);
        assertThat(single.requests).extracting(RuntimeSourceReadRequestDTO::getSymbol).containsExactly("BTCUSDT");
    }

    @Test
    void noDisplaySlotsUniverse() {
        CapturingSingleSymbolOrchestrator single = new CapturingSingleSymbolOrchestrator(
                request -> WatchlistScanResultDTO.reviewOnly(request.getSymbol(), List.of("AVAILABLE_REVIEW_ONLY"))
        );

        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(single).scanBatch(
                "batch-1",
                "request-1",
                "display-slots",
                List.of("ADAUSDT")
        );

        assertThat(result.getAcceptedSymbols()).containsExactly("ADAUSDT");
        assertThat(single.requests).hasSize(1);
        assertThat(single.requests).extracting(RuntimeSourceReadRequestDTO::getSymbol).doesNotContain("BTCUSDT", "ETHUSDT");
        assertSafeEnvelopeAndResults(result);
    }

    @Test
    void singleSymbolOrchestratorCalledOncePerAcceptedSymbol() {
        CapturingSingleSymbolOrchestrator single = new CapturingSingleSymbolOrchestrator(
                request -> WatchlistScanResultDTO.reviewOnly(request.getSymbol(), List.of("AVAILABLE_REVIEW_ONLY"))
        );

        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(single).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT", "ETHUSDT")
        );

        assertThat(result.getAcceptedSymbols()).containsExactly("BTCUSDT", "ETHUSDT");
        assertThat(single.requests).hasSize(2);
        assertThat(single.requests).extracting(RuntimeSourceReadRequestDTO::getWatchlistPoolOnly).containsOnly(true);
        assertThat(single.requests).extracting(RuntimeSourceReadRequestDTO::getRequestReason).containsOnly("batch-watchlist-scan");
    }

    @Test
    void orchestratorNullResultBecomesIncompleteSymbolResult() {
        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(request -> null).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT")
        );

        assertThat(result.getResults()).hasSize(1);
        WatchlistScanResultDTO symbolResult = result.getResults().get(0);
        assertThat(symbolResult.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(symbolResult.getBlockingReasons()).contains("SINGLE_SYMBOL_RESULT_MISSING");
        assertThat(result.getBlockingReasons()).contains("ALL_RESULTS_INCOMPLETE");
        assertSafeEnvelopeAndResults(result);
    }

    @Test
    void orchestratorExceptionBecomesIncompleteSymbolResult() {
        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(request -> {
            throw new IllegalStateException("boom");
        }).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT")
        );

        WatchlistScanResultDTO symbolResult = result.getResults().get(0);
        assertThat(symbolResult.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(symbolResult.getBlockingReasons()).contains("SINGLE_SYMBOL_ORCHESTRATOR_FAILED");
        assertSafeEnvelopeAndResults(result);
    }

    @Test
    void unsafeResultBecomesIncompleteSymbolResult() {
        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(
                request -> WatchlistScanResultDTO.candidateAttentionReviewOnly(
                        request.getSymbol(),
                        List.of("UNSAFE_CANDIDATE")
                )
        ).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT")
        );

        WatchlistScanResultDTO symbolResult = result.getResults().get(0);
        assertThat(symbolResult.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.INCOMPLETE);
        assertThat(symbolResult.getBlockingReasons()).contains("SINGLE_SYMBOL_RESULT_UNSAFE");
        assertSafeEnvelopeAndResults(result);
    }

    @Test
    void safeReviewOnlyResultPreserved() {
        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(alwaysReviewOnly()).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT")
        );

        WatchlistScanResultDTO symbolResult = result.getResults().get(0);
        assertThat(symbolResult.getScanStatus()).isEqualTo(WatchlistScanStatusEnum.REVIEW_ONLY);
        assertThat(symbolResult.getBlockingReasons()).contains("AVAILABLE_REVIEW_ONLY");
        assertSafeEnvelopeAndResults(result);
    }

    @Test
    void acceptedSymbolsEmptyReturnsBatchIncomplete() {
        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(alwaysReviewOnly()).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("", " ")
        );

        assertThat(result.getAcceptedSymbols()).isEmpty();
        assertThat(result.getBlockingReasons()).contains("ALL_SYMBOLS_REJECTED");
        assertThat(result.getResults()).isEmpty();
        assertSafeEnvelope(result);
    }

    @Test
    void nonWatchlistResultRecordedButNotPromoted() {
        BatchWatchlistScanResultEnvelopeDTO result = enabledOrchestrator(
                request -> WatchlistScanResultDTO.blockedNotWatchlist(
                        request.getSymbol(),
                        List.of("BLOCKED_NOT_WATCHLIST")
                )
        ).scanBatch(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("DOGEUSDT")
        );

        assertThat(result.getNonWatchlistSymbols()).containsExactly("DOGEUSDT");
        assertThat(result.getRejectedSymbols()).containsExactly("DOGEUSDT");
        assertThat(result.getBlockingReasons()).contains("BLOCKED_NOT_WATCHLIST");
        assertThat(result.getResults().get(0).getScanStatus()).isEqualTo(WatchlistScanStatusEnum.BLOCKED_NOT_WATCHLIST);
        assertSafeEnvelopeAndResults(result);
    }

    @Test
    void allOutputsPreserveNoExecutionDefaults() {
        List<BatchWatchlistScanResultEnvelopeDTO> outputs = List.of(
                new DefaultBatchWatchlistScanOrchestrator(alwaysReviewOnly(), false).scanBatch(
                        "batch-1",
                        "request-1",
                        "watchlist",
                        List.of("BTCUSDT")
                ),
                enabledOrchestrator(request -> null).scanBatch(
                        "batch-2",
                        "request-2",
                        "watchlist",
                        List.of("ETHUSDT")
                ),
                enabledOrchestrator(alwaysReviewOnly()).scanBatch(
                        "batch-3",
                        "request-3",
                        "watchlist",
                        List.of("SOLUSDT")
                )
        );

        for (BatchWatchlistScanResultEnvelopeDTO output : outputs) {
            assertSafeEnvelopeAndResults(output);
        }
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

        for (Field field : DefaultBatchWatchlistScanOrchestrator.class.getDeclaredFields()) {
            for (String token : forbidden) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        for (Method method : BatchWatchlistScanOrchestrator.class.getDeclaredMethods()) {
            assertThat(method.getName()).isEqualTo("scanBatch");
            for (String token : forbidden) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
        }

        for (Method method : DefaultBatchWatchlistScanOrchestrator.class.getDeclaredMethods()) {
            for (String token : forbidden) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("Scheduled");
            }
        }
    }

    private static DefaultBatchWatchlistScanOrchestrator enabledOrchestrator(
            LowFrequencyWatchlistScanOrchestrator singleSymbolOrchestrator
    ) {
        return new DefaultBatchWatchlistScanOrchestrator(singleSymbolOrchestrator, true);
    }

    private static LowFrequencyWatchlistScanOrchestrator alwaysReviewOnly() {
        return request -> WatchlistScanResultDTO.reviewOnly(request.getSymbol(), List.of("AVAILABLE_REVIEW_ONLY"));
    }

    private static void assertSafeEnvelopeAndResults(BatchWatchlistScanResultEnvelopeDTO result) {
        assertSafeEnvelope(result);
        for (WatchlistScanResultDTO symbolResult : result.getResults()) {
            assertSafeResult(symbolResult);
        }
    }

    private static void assertSafeEnvelope(BatchWatchlistScanResultEnvelopeDTO result) {
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
        assertThat(result.isOpportunityPushAllowed()).isFalse();
        assertThat(result.isCandidateAttentionAllowed()).isFalse();
        assertThat(result.isPromoteToHomeAllowed()).isFalse();
        assertThat(result.isReadinessUpgraded()).isFalse();
        assertThat(result.isTradingActionCreated()).isFalse();
        assertThat(result.isEntryStopTpRrGenerated()).isFalse();
        assertThat(result.isWatchlistPoolOnly()).isTrue();
        assertThat(result.isDisabledByDefault()).isTrue();
    }

    private static void assertSafeResult(WatchlistScanResultDTO result) {
        assertThat(result.getManualReviewRequired()).isTrue();
        assertThat(result.getNotTradeInstruction()).isTrue();
        assertThat(result.getOpportunityPushAllowed()).isFalse();
        assertThat(result.getCandidateAttentionAllowed()).isFalse();
        assertThat(result.getPromoteToHomeAllowed()).isFalse();
        assertThat(result.getReadinessUpgraded()).isFalse();
        assertThat(result.getTradingActionCreated()).isFalse();
        assertThat(result.getEntryStopTpRrGenerated()).isFalse();
    }

    private static class CapturingSingleSymbolOrchestrator implements LowFrequencyWatchlistScanOrchestrator {
        private final Function<RuntimeSourceReadRequestDTO, WatchlistScanResultDTO> response;
        private final List<RuntimeSourceReadRequestDTO> requests = new ArrayList<>();

        private CapturingSingleSymbolOrchestrator(
                Function<RuntimeSourceReadRequestDTO, WatchlistScanResultDTO> response
        ) {
            this.response = response;
        }

        @Override
        public WatchlistScanResultDTO scanSingleSymbol(RuntimeSourceReadRequestDTO request) {
            requests.add(request);
            return response.apply(request);
        }
    }
}
