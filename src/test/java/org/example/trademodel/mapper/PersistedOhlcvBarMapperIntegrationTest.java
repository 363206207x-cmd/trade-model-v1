package org.example.trademodel.mapper;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class PersistedOhlcvBarMapperIntegrationTest {

    @Autowired
    private PersistedOhlcvBarMapper persistedOhlcvBarMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void selectLatestClosedWindow_returnsClosedNonDeletedRowsOrderedByCloseDescAndLimited() {
        insertBar("BTCUSDT", "1m", 1000L, 1999L, "100.00", true, 0, "batch-window", "trace-1");
        insertBar("BTCUSDT", "1m", 2000L, 2999L, "101.00", true, 0, "batch-window", "trace-2");
        insertBar("BTCUSDT", "1m", 3000L, 3999L, "102.00", true, 0, "batch-window", "trace-3");
        insertBar("BTCUSDT", "1m", 4000L, 4999L, "103.00", false, 0, "batch-window", "trace-open");
        insertBar("BTCUSDT", "1m", 5000L, 5999L, "104.00", true, 1, "batch-window", "trace-deleted");

        List<PersistedOhlcvBarDO> rows = persistedOhlcvBarMapper
                .selectLatestClosedWindow("BTCUSDT", "1m", 2);

        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(PersistedOhlcvBarDO::getCloseTimeMs)
                .containsExactly(3999L, 2999L);
        assertThat(rows).extracting(PersistedOhlcvBarDO::getClosed)
                .containsExactly(Boolean.TRUE, Boolean.TRUE);
        assertThat(rows).extracting(PersistedOhlcvBarDO::getClosePrice)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("102.50000000"), new BigDecimal("101.50000000"));
        assertThat(rows.get(0).getProvider()).isEqualTo("BINANCE");
        assertThat(rows.get(0).getProviderMarketType()).isEqualTo("SPOT");
        assertThat(rows.get(0).getSourceEndpoint()).isEqualTo("persisted.ohlcv.fixture");
        assertThat(rows.get(0).getSourceBatchId()).isEqualTo("batch-window");
        assertThat(rows.get(0).getQualityStatus()).isEqualTo("OK");
    }

    @Test
    void selectBySymbolTimeframeAndOpenTime_returnsAuditRowWithSourceOwnershipAndQuality() {
        insertBar("ETHUSDT", "5m", 10_000L, 14_999L, "2500.00", true, 0, "batch-audit", "trace-audit");

        PersistedOhlcvBarDO row = persistedOhlcvBarMapper
                .selectBySymbolTimeframeAndOpenTime("ETHUSDT", "5m", 10_000L);

        assertThat(row).isNotNull();
        assertThat(row.getSymbol()).isEqualTo("ETHUSDT");
        assertThat(row.getTimeframe()).isEqualTo("5m");
        assertThat(row.getOpenPrice()).isEqualByComparingTo("2500.00000000");
        assertThat(row.getHighPrice()).isEqualByComparingTo("2501.00000000");
        assertThat(row.getLowPrice()).isEqualByComparingTo("2499.00000000");
        assertThat(row.getClosePrice()).isEqualByComparingTo("2500.50000000");
        assertThat(row.getVolume()).isEqualByComparingTo("123.45000000");
        assertThat(row.getQuoteVolume()).isEqualByComparingTo("308765.43210000");
        assertThat(row.getTradeCount()).isEqualTo(87L);
        assertThat(row.getTakerBuyBaseVolume()).isEqualByComparingTo("61.00000000");
        assertThat(row.getTakerBuyQuoteVolume()).isEqualByComparingTo("152500.00000000");
        assertThat(row.getSourceBatchId()).isEqualTo("batch-audit");
        assertThat(row.getSourceTraceId()).isEqualTo("trace-audit");
        assertThat(row.getSourceVersion()).isEqualTo(1);
        assertThat(row.getQualityStatus()).isEqualTo("OK");
        assertThat(row.getRawPayloadHash()).isEqualTo("hash-trace-audit");
    }

    @Test
    void selectLatestIngestionBatch_returnsNewestNonDeletedBatchMetadata() {
        insertBar("SOLUSDT", "15m", 20_000L, 34_999L, "150.00", true, 0, "batch-older", "trace-old",
                LocalDateTime.of(2026, 5, 17, 10, 0, 0));
        insertBar("SOLUSDT", "15m", 35_000L, 49_999L, "151.00", true, 0, "batch-newer", "trace-new",
                LocalDateTime.of(2026, 5, 17, 10, 5, 0));
        insertBar("SOLUSDT", "15m", 50_000L, 64_999L, "152.00", true, 1, "batch-deleted", "trace-deleted",
                LocalDateTime.of(2026, 5, 17, 10, 10, 0));

        PersistedOhlcvBarDO row = persistedOhlcvBarMapper
                .selectLatestIngestionBatch("SOLUSDT", "15m");

        assertThat(row).isNotNull();
        assertThat(row.getSourceBatchId()).isEqualTo("batch-newer");
        assertThat(row.getSourceTraceId()).isEqualTo("trace-new");
        assertThat(row.getIngestedAt()).isEqualTo(LocalDateTime.of(2026, 5, 17, 10, 5, 0));
    }

    private void insertBar(
            String symbol,
            String timeframe,
            long openTimeMs,
            long closeTimeMs,
            String openPrice,
            boolean closed,
            int isDeleted,
            String sourceBatchId,
            String sourceTraceId
    ) {
        insertBar(symbol, timeframe, openTimeMs, closeTimeMs, openPrice, closed, isDeleted, sourceBatchId,
                sourceTraceId, LocalDateTime.of(2026, 5, 17, 10, 0, 0));
    }

    private void insertBar(
            String symbol,
            String timeframe,
            long openTimeMs,
            long closeTimeMs,
            String openPrice,
            boolean closed,
            int isDeleted,
            String sourceBatchId,
            String sourceTraceId,
            LocalDateTime ingestedAt
    ) {
        BigDecimal open = new BigDecimal(openPrice);
        jdbcTemplate.update(
                "INSERT INTO tm_persisted_ohlcv_bar("
                        + "symbol, timeframe, open_time_ms, close_time_ms, open_price, high_price, low_price, "
                        + "close_price, volume, quote_volume, trade_count, taker_buy_base_volume, "
                        + "taker_buy_quote_volume, is_closed, provider, provider_market_type, source_endpoint, "
                        + "source_batch_id, source_trace_id, source_version, ingested_at, updated_at, "
                        + "quality_status, quality_reason, raw_payload_hash, is_deleted) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                symbol,
                timeframe,
                openTimeMs,
                closeTimeMs,
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
                "BINANCE",
                "SPOT",
                "persisted.ohlcv.fixture",
                sourceBatchId,
                sourceTraceId,
                1,
                ingestedAt,
                ingestedAt,
                "OK",
                null,
                "hash-" + sourceTraceId,
                isDeleted
        );
    }
}
