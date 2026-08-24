package org.example.trademodel.service.impl;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@TestPropertySource(properties = "trade-model.ohlcv.provider.primary=binance")
@Transactional
@Tag("core-regression")
class PersistedOhlcvQueryServiceTest {
    private static final long ONE_MINUTE_MS = 60_000L;
    private static final long MAX_READ_LAG_MS = 180_000L;

    @Autowired
    private PersistedOhlcvQueryService persistedOhlcvQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void noRowsReturnsMissingAndFailClosed() {
        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService
                .evaluateReadiness("NOUSDT", "1m", 3, MAX_READ_LAG_MS);

        assertStatus(result, PersistedOhlcvReadinessStatus.MISSING,
                PersistedOhlcvStaleReasonCode.NO_BARS_FOR_SYMBOL_TIMEFRAME);
        assertThat(result.getMissingFields()).contains("persistedOhlcvWindow", "klineItems");
        assertThat(result.isFresh()).isFalse();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void tooFewRowsReturnsPartial() {
        insertContiguousWindow("PARTIALUSDT", 2);

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService
                .evaluateReadiness("PARTIALUSDT", "1m", 3, MAX_READ_LAG_MS);

        assertStatus(result, PersistedOhlcvReadinessStatus.PARTIAL,
                PersistedOhlcvStaleReasonCode.WINDOW_TOO_SHORT);
        assertThat(result.getMissingFields()).contains("persistedOhlcvWindow", "requiredClosedBars");
    }

    @Test
    void nonContiguousWindowReturnsPartialWithReason() {
        long latestOpen = freshLatestOpen();
        insertBar("GAPUSDT", latestOpen, true, 0, "OK", "BINANCE_PUBLIC");
        insertBar("GAPUSDT", latestOpen - (ONE_MINUTE_MS * 2), true, 0, "OK", "BINANCE_PUBLIC");
        insertBar("GAPUSDT", latestOpen - (ONE_MINUTE_MS * 3), true, 0, "OK", "BINANCE_PUBLIC");

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService
                .evaluateReadiness("GAPUSDT", "1m", 3, MAX_READ_LAG_MS);

        assertStatus(result, PersistedOhlcvReadinessStatus.PARTIAL,
                PersistedOhlcvStaleReasonCode.WINDOW_NOT_CONTIGUOUS);
        assertThat(result.getMissingFields()).contains("klineWindow");
    }

    @Test
    void staleLatestBarReturnsStale() {
        long latestOpen = System.currentTimeMillis() - (ONE_MINUTE_MS * 20);
        insertBar("STALEUSDT", latestOpen, true, 0, "OK", "BINANCE_PUBLIC");
        insertBar("STALEUSDT", latestOpen - ONE_MINUTE_MS, true, 0, "OK", "BINANCE_PUBLIC");
        insertBar("STALEUSDT", latestOpen - (ONE_MINUTE_MS * 2), true, 0, "OK", "BINANCE_PUBLIC");

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService
                .evaluateReadiness("STALEUSDT", "1m", 3, MAX_READ_LAG_MS);

        assertStatus(result, PersistedOhlcvReadinessStatus.STALE,
                PersistedOhlcvStaleReasonCode.LATEST_BAR_TOO_OLD);
        assertThat(result.getMissingFields()).contains("klineFreshness");
    }

    @Test
    void missingSourceOwnershipReturnsUnknown() {
        insertContiguousWindow("OWNERUSDT", 3, "BINANCE_PUBLIC");
        jdbcTemplate.update("UPDATE tm_persisted_ohlcv_bar SET source_endpoint = '' WHERE symbol = ?",
                "OWNERUSDT");

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService
                .evaluateReadiness("OWNERUSDT", "1m", 3, MAX_READ_LAG_MS);

        assertStatus(result, PersistedOhlcvReadinessStatus.UNKNOWN,
                PersistedOhlcvStaleReasonCode.SOURCE_OWNER_MISSING);
        assertThat(result.getMissingFields()).contains("sourceEndpoint");
    }

    @Test
    void nonOkQualityReturnsInvalid() {
        insertContiguousWindow("BADQUALITY", 3, "BINANCE_PUBLIC", "CONFLICT");

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService
                .evaluateReadiness("BADQUALITY", "1m", 3, MAX_READ_LAG_MS);

        assertStatus(result, PersistedOhlcvReadinessStatus.INVALID,
                PersistedOhlcvStaleReasonCode.QUALITY_STATUS_NOT_OK);
        assertThat(result.getMissingFields()).contains("qualityStatus");
    }

    @Test
    void freshContiguousClosedWindowReturnsFresh() {
        insertContiguousWindow("FRESHUSDT", 3);

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService
                .evaluateReadiness("FRESHUSDT", "1m", 3, MAX_READ_LAG_MS);

        assertStatus(result, PersistedOhlcvReadinessStatus.FRESH, PersistedOhlcvStaleReasonCode.NONE);
        assertThat(result.isFresh()).isTrue();
        assertThat(result.getMissingFields()).isEmpty();
        assertThat(result.getBars()).hasSize(3);
        assertThat(result.getLatestCloseTimeMs()).isNotNull();
        assertThat(result.getLatestIngestedAt()).isNotNull();
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    @Test
    void openCandlesAreExcludedAndCannotCompleteReadiness() {
        long latestOpen = freshLatestOpen();
        insertBar("OPENUSDT", latestOpen, false, 0, "OK", "BINANCE_PUBLIC");
        insertBar("OPENUSDT", latestOpen - ONE_MINUTE_MS, true, 0, "OK", "BINANCE_PUBLIC");

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService
                .evaluateReadiness("OPENUSDT", "1m", 2, MAX_READ_LAG_MS);

        assertStatus(result, PersistedOhlcvReadinessStatus.PARTIAL,
                PersistedOhlcvStaleReasonCode.WINDOW_TOO_SHORT);
        assertThat(result.getBars()).hasSize(1);
    }

    @Test
    void deletedRowsAreExcludedAndCannotCompleteReadiness() {
        long latestOpen = freshLatestOpen();
        insertBar("DELETEDUSDT", latestOpen, true, 1, "OK", "BINANCE_PUBLIC");
        insertBar("DELETEDUSDT", latestOpen - ONE_MINUTE_MS, true, 0, "OK", "BINANCE_PUBLIC");

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService
                .evaluateReadiness("DELETEDUSDT", "1m", 2, MAX_READ_LAG_MS);

        assertStatus(result, PersistedOhlcvReadinessStatus.PARTIAL,
                PersistedOhlcvStaleReasonCode.WINDOW_TOO_SHORT);
        assertThat(result.getBars()).hasSize(1);
    }

    @Test
    void sameTimestampRowsRemainIsolatedByProvider() {
        long latestOpen = freshLatestOpen();
        insertBar("SOURCEUSDT", "1m", latestOpen, true, 0, "OK", "BINANCE_PUBLIC");
        insertBar("SOURCEUSDT", "1m", latestOpen, true, 0, "OK", "KRAKEN");

        PersistedOhlcvReadinessResult binance = persistedOhlcvQueryService.evaluateReadinessForSource(
                "SOURCEUSDT", "1m", 1, MAX_READ_LAG_MS, "BINANCE", "SPOT");
        PersistedOhlcvReadinessResult kraken = persistedOhlcvQueryService.evaluateReadinessForSource(
                "SOURCEUSDT", "1m", 1, MAX_READ_LAG_MS, "KRAKEN", "SPOT");

        assertThat(binance.isFresh()).isTrue();
        assertThat(binance.getBars()).extracting(bar -> bar.getProvider()).containsOnly("BINANCE_PUBLIC");
        assertThat(kraken.isFresh()).isTrue();
        assertThat(kraken.getBars()).extracting(bar -> bar.getProvider()).containsOnly("KRAKEN");
    }

    @Test
    void missingBinanceWindowDoesNotFallbackToKraken() {
        long latestOpen = freshLatestOpen();
        insertBar("NOFALLBACK", "1m", latestOpen, true, 0, "OK", "KRAKEN");

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService.evaluateReadinessForSource(
                "NOFALLBACK", "1m", 1, MAX_READ_LAG_MS, "BINANCE", "SPOT");

        assertStatus(result, PersistedOhlcvReadinessStatus.MISSING,
                PersistedOhlcvStaleReasonCode.NO_BARS_FOR_SYMBOL_TIMEFRAME);
        assertThat(result.getBars()).isEmpty();
    }

    @Test
    void fiveMinuteWindowCannotBridgeMissingBinanceBarWithKraken() {
        long interval = 5L * ONE_MINUTE_MS;
        long latestOpen = freshLatestOpen(interval);
        insertBar("FIVEMIX", "5m", latestOpen, true, 0, "OK", "BINANCE_PUBLIC");
        insertBar("FIVEMIX", "5m", latestOpen - interval, true, 0, "OK", "KRAKEN");
        insertBar("FIVEMIX", "5m", latestOpen - (2 * interval), true, 0, "OK", "BINANCE_PUBLIC");

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService.evaluateReadinessForSource(
                "FIVEMIX", "5m", 3, interval * 2, "BINANCE", "SPOT");

        assertStatus(result, PersistedOhlcvReadinessStatus.PARTIAL,
                PersistedOhlcvStaleReasonCode.WINDOW_TOO_SHORT);
        assertThat(result.getBars()).hasSize(2).extracting(bar -> bar.getProvider())
                .containsOnly("BINANCE_PUBLIC");
    }

    @Test
    void fifteenMinuteWindowCannotBridgeMissingBinanceBarWithKraken() {
        long interval = 15L * ONE_MINUTE_MS;
        long latestOpen = freshLatestOpen(interval);
        insertBar("FIFTEENMIX", "15m", latestOpen, true, 0, "OK", "BINANCE_PUBLIC");
        insertBar("FIFTEENMIX", "15m", latestOpen - interval, true, 0, "OK", "KRAKEN");
        insertBar("FIFTEENMIX", "15m", latestOpen - (2 * interval), true, 0, "OK", "BINANCE_PUBLIC");

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService.evaluateReadinessForSource(
                "FIFTEENMIX", "15m", 3, interval * 2, "BINANCE", "SPOT");

        assertStatus(result, PersistedOhlcvReadinessStatus.PARTIAL,
                PersistedOhlcvStaleReasonCode.WINDOW_TOO_SHORT);
        assertThat(result.getBars()).hasSize(2).extracting(bar -> bar.getProvider())
                .containsOnly("BINANCE_PUBLIC");
    }

    @Test
    void oneHundredClosedBinanceBarsPassSourceOwnedReadiness() {
        long interval = 5L * ONE_MINUTE_MS;
        long latestOpen = freshLatestOpen(interval);
        for (int index = 0; index < 100; index++) {
            insertBar("HUNDREDBARS", "5m", latestOpen - (index * interval),
                    true, 0, "OK", "BINANCE_PUBLIC");
        }

        PersistedOhlcvReadinessResult result = persistedOhlcvQueryService.evaluateReadinessForSource(
                "HUNDREDBARS", "5m", 100, interval * 2, "BINANCE", "SPOT");

        assertStatus(result, PersistedOhlcvReadinessStatus.FRESH, PersistedOhlcvStaleReasonCode.NONE);
        assertThat(result.getBars()).hasSize(100).extracting(bar -> bar.getProvider())
                .containsOnly("BINANCE_PUBLIC");
    }

    private void assertStatus(
            PersistedOhlcvReadinessResult result,
            PersistedOhlcvReadinessStatus status,
            PersistedOhlcvStaleReasonCode reason
    ) {
        assertThat(result.getStatus()).isEqualTo(status);
        assertThat(result.getStaleReasonCode()).isEqualTo(reason);
        assertThat(result.isManualReviewRequired()).isTrue();
        assertThat(result.isNotTradeInstruction()).isTrue();
    }

    private void insertContiguousWindow(String symbol, int count) {
        insertContiguousWindow(symbol, count, "BINANCE_PUBLIC");
    }

    private void insertContiguousWindow(String symbol, int count, String provider) {
        insertContiguousWindow(symbol, count, provider, "OK");
    }

    private void insertContiguousWindow(String symbol, int count, String provider, String qualityStatus) {
        long latestOpen = freshLatestOpen();
        for (int i = 0; i < count; i++) {
            insertBar(symbol, latestOpen - (ONE_MINUTE_MS * i), true, 0, qualityStatus, provider);
        }
    }

    private long freshLatestOpen() {
        return freshLatestOpen(ONE_MINUTE_MS);
    }

    private long freshLatestOpen(long intervalMs) {
        long latestClose = System.currentTimeMillis() - 1_000L;
        return latestClose - (intervalMs - 1);
    }

    private void insertBar(
            String symbol,
            long openTimeMs,
            boolean closed,
            int isDeleted,
            String qualityStatus,
            String provider
    ) {
        insertBar(symbol, "1m", openTimeMs, closed, isDeleted, qualityStatus, provider);
    }

    private void insertBar(
            String symbol,
            String timeframe,
            long openTimeMs,
            boolean closed,
            int isDeleted,
            String qualityStatus,
            String provider
    ) {
        long intervalMs = switch (timeframe) {
            case "1m" -> ONE_MINUTE_MS;
            case "5m" -> 5L * ONE_MINUTE_MS;
            case "15m" -> 15L * ONE_MINUTE_MS;
            default -> throw new IllegalArgumentException(timeframe);
        };
        BigDecimal open = new BigDecimal("100.00").add(BigDecimal.valueOf(openTimeMs % 1000));
        LocalDateTime ingestedAt = LocalDateTime.of(2026, 5, 17, 10, 0, 0);
        jdbcTemplate.update(
                "INSERT INTO tm_persisted_ohlcv_bar("
                        + "symbol, timeframe, open_time_ms, close_time_ms, open_price, high_price, low_price, "
                        + "close_price, volume, quote_volume, trade_count, taker_buy_base_volume, "
                        + "taker_buy_quote_volume, is_closed, provider, provider_market_type, source_endpoint, "
                        + "source_batch_id, source_trace_id, source_version, ingested_at, updated_at, "
                        + "fetch_time, source_status, freshness_status, provenance_version, ingestion_run_id, "
                        + "quality_status, quality_reason, raw_payload_hash, is_deleted) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                symbol,
                timeframe,
                openTimeMs,
                openTimeMs + (intervalMs - 1),
                open,
                open.add(new BigDecimal("1.00")),
                open.subtract(new BigDecimal("1.00")),
                open.add(new BigDecimal("0.50")),
                new BigDecimal("123.45"),
                new BigDecimal("308765.4321"),
                87L,
                new BigDecimal("61.00"),
                new BigDecimal("152500.00"),
                closed,
                provider,
                "SPOT",
                "persisted.ohlcv.fixture",
                "batch-" + symbol,
                "trace-" + symbol + "-" + openTimeMs,
                1,
                ingestedAt,
                ingestedAt,
                ingestedAt,
                "READY",
                "FRESH",
                "query-fixture-v1",
                "run-" + symbol,
                qualityStatus,
                "OK".equals(qualityStatus) ? null : "fixture-quality-not-ok",
                "hash-" + symbol + "-" + openTimeMs,
                isDeleted
        );
    }
}
