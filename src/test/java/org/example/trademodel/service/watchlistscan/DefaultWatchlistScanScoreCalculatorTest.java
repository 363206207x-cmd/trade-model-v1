package org.example.trademodel.service.watchlistscan;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import org.example.trademodel.dto.watchlistscan.BatchWatchlistScanResultEnvelopeDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanResultDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreDTO;
import org.example.trademodel.dto.watchlistscan.WatchlistScanScoreStatusEnum;
import org.junit.jupiter.api.Test;

class DefaultWatchlistScanScoreCalculatorTest {

    @Test
    void missingScoreRuleFailsClosed() {
        DefaultWatchlistScanScoreCalculator calculator = new DefaultWatchlistScanScoreCalculator(null);

        WatchlistScanScoreDTO result = calculator.calculate("BTCUSDT", reviewOnlyEnvelope());

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SCANSCORE_RULE_MISSING", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void nullBatchEnvelopeFailsClosed() {
        CountingScoreRule rule = new CountingScoreRule(reviewOnlyScore("BTCUSDT"));
        DefaultWatchlistScanScoreCalculator calculator = new DefaultWatchlistScanScoreCalculator(rule);

        WatchlistScanScoreDTO result = calculator.calculate("BTCUSDT", null);

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("BATCH_ENVELOPE_MISSING", "INCOMPLETE");
        assertThat(rule.calls).isZero();
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void blankSymbolFailsClosed() {
        CountingScoreRule rule = new CountingScoreRule(reviewOnlyScore("BTCUSDT"));
        DefaultWatchlistScanScoreCalculator calculator = new DefaultWatchlistScanScoreCalculator(rule);

        WatchlistScanScoreDTO result = calculator.calculate(" ", reviewOnlyEnvelope());

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SYMBOL_MISSING", "INCOMPLETE");
        assertThat(rule.calls).isZero();
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void scoreRuleReturnsNullFailsClosed() {
        CountingScoreRule rule = new CountingScoreRule((WatchlistScanScoreDTO) null);
        DefaultWatchlistScanScoreCalculator calculator = new DefaultWatchlistScanScoreCalculator(rule);

        WatchlistScanScoreDTO result = calculator.calculate("BTCUSDT", reviewOnlyEnvelope());

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SCANSCORE_RESULT_MISSING", "INCOMPLETE");
        assertThat(rule.calls).isEqualTo(1);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void scoreRuleThrowsFailsClosed() {
        CountingScoreRule rule = new CountingScoreRule(new IllegalStateException("boom"));
        DefaultWatchlistScanScoreCalculator calculator = new DefaultWatchlistScanScoreCalculator(rule);

        WatchlistScanScoreDTO result = calculator.calculate("BTCUSDT", reviewOnlyEnvelope());

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SCANSCORE_CALCULATION_FAILED", "INCOMPLETE");
        assertThat(rule.calls).isEqualTo(1);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void unsafeScoreResultFailsClosed() throws Exception {
        CountingScoreRule rule = new CountingScoreRule(unsafeScoreResult());
        DefaultWatchlistScanScoreCalculator calculator = new DefaultWatchlistScanScoreCalculator(rule);

        WatchlistScanScoreDTO result = calculator.calculate("BTCUSDT", reviewOnlyEnvelope());

        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.INCOMPLETE);
        assertThat(result.getBlockingReasons()).contains("SCANSCORE_RESULT_UNSAFE", "INCOMPLETE");
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void safeReviewOnlyScoreResultIsReturned() {
        WatchlistScanScoreDTO safeScore = reviewOnlyScore("BTCUSDT");
        CountingScoreRule rule = new CountingScoreRule(safeScore);
        DefaultWatchlistScanScoreCalculator calculator = new DefaultWatchlistScanScoreCalculator(rule);

        WatchlistScanScoreDTO result = calculator.calculate("BTCUSDT", reviewOnlyEnvelope());

        assertThat(result).isSameAs(safeScore);
        assertThat(result.getScoreStatus()).isEqualTo(WatchlistScanScoreStatusEnum.REVIEW_ONLY);
        assertThat(result.getScanScore()).isEqualByComparingTo(BigDecimal.ZERO);
        assertSafeNoExecutionDefaults(result);
    }

    @Test
    void allOutputsPreserveNoExecutionDefaults() {
        List<WatchlistScanScoreDTO> outputs = List.of(
                new DefaultWatchlistScanScoreCalculator(null).calculate("BTCUSDT", reviewOnlyEnvelope()),
                new DefaultWatchlistScanScoreCalculator(new CountingScoreRule((WatchlistScanScoreDTO) null))
                        .calculate("BTCUSDT", reviewOnlyEnvelope()),
                new DefaultWatchlistScanScoreCalculator(new CountingScoreRule(reviewOnlyScore("BTCUSDT")))
                        .calculate("BTCUSDT", reviewOnlyEnvelope()),
                new DefaultWatchlistScanScoreCalculator(new CountingScoreRule(new IllegalStateException("boom")))
                        .calculate("BTCUSDT", reviewOnlyEnvelope())
        );

        for (WatchlistScanScoreDTO output : outputs) {
            assertSafeNoExecutionDefaults(output);
        }
    }

    @Test
    void calculatorOnlyCallsRuleOnce() {
        CountingScoreRule rule = new CountingScoreRule(reviewOnlyScore("BTCUSDT"));
        BatchWatchlistScanResultEnvelopeDTO envelope = reviewOnlyEnvelope();
        DefaultWatchlistScanScoreCalculator calculator = new DefaultWatchlistScanScoreCalculator(rule);

        calculator.calculate("BTCUSDT", envelope);

        assertThat(rule.calls).isEqualTo(1);
        assertThat(rule.lastSymbol).isEqualTo("BTCUSDT");
        assertThat(rule.lastEnvelope).isSameAs(envelope);
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

        for (Field field : DefaultWatchlistScanScoreCalculator.class.getDeclaredFields()) {
            for (String token : forbidden) {
                assertThat(field.getType().getName()).doesNotContain(token);
                assertThat(field.getName()).doesNotContain(token);
            }
        }

        assertThat(WatchlistScanScoreCalculator.class.getDeclaredMethods()).hasSize(1);
        Method interfaceMethod = WatchlistScanScoreCalculator.class.getDeclaredMethods()[0];
        assertThat(interfaceMethod.getName()).isEqualTo("calculate");
        assertThat(interfaceMethod.getReturnType()).isEqualTo(WatchlistScanScoreDTO.class);

        for (Method method : DefaultWatchlistScanScoreCalculator.class.getDeclaredMethods()) {
            String methodName = method.getName().toLowerCase();
            assertThat(methodName).doesNotContain("push");
            assertThat(methodName).doesNotContain("readiness");
            assertThat(methodName).doesNotContain("order");
            assertThat(methodName).doesNotContain("execute");
            assertThat(methodName).doesNotContain("trade");
            for (String token : forbidden) {
                assertThat(method.toGenericString()).doesNotContain(token);
            }
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                assertThat(annotation.annotationType().getName()).doesNotContain("Scheduled");
            }
        }
    }

    private static BatchWatchlistScanResultEnvelopeDTO reviewOnlyEnvelope() {
        return BatchWatchlistScanResultEnvelopeDTO.reviewOnly(
                "batch-1",
                "request-1",
                "watchlist",
                List.of("BTCUSDT"),
                List.of("BTCUSDT"),
                List.of(),
                List.of(WatchlistScanResultDTO.reviewOnly("BTCUSDT", List.of("AVAILABLE_REVIEW_ONLY"))),
                List.of()
        );
    }

    private static WatchlistScanScoreDTO reviewOnlyScore(String symbol) {
        return WatchlistScanScoreDTO.reviewOnly(
                symbol,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "unit-test",
                List.of("SCANSCORE_REVIEW_ONLY_SKELETON"),
                List.of("AVAILABLE_REVIEW_ONLY")
        );
    }

    private static WatchlistScanScoreDTO unsafeScoreResult() throws Exception {
        WatchlistScanScoreDTO score = reviewOnlyScore("BTCUSDT");
        Field field = WatchlistScanScoreDTO.class.getDeclaredField("opportunityPushAllowed");
        field.setAccessible(true);
        field.setBoolean(score, true);
        return score;
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

    private static class CountingScoreRule implements WatchlistScanScoreRule {

        private final WatchlistScanScoreDTO result;
        private final RuntimeException exception;
        private int calls;
        private String lastSymbol;
        private BatchWatchlistScanResultEnvelopeDTO lastEnvelope;

        CountingScoreRule(WatchlistScanScoreDTO result) {
            this.result = result;
            this.exception = null;
        }

        CountingScoreRule(RuntimeException exception) {
            this.result = null;
            this.exception = exception;
        }

        @Override
        public WatchlistScanScoreDTO evaluate(
                String symbol,
                BatchWatchlistScanResultEnvelopeDTO batchEnvelope
        ) {
            calls++;
            lastSymbol = symbol;
            lastEnvelope = batchEnvelope;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
