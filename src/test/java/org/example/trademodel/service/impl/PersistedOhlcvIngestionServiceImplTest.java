package org.example.trademodel.service.impl;

import org.example.trademodel.TradeModelApplication;
import org.example.trademodel.dto.ohlcv.OhlcvBarInput;
import org.example.trademodel.dto.ohlcv.OhlcvFreshnessStatus;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionResult;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.mapper.PersistedOhlcvBarMapper;
import org.example.trademodel.service.PersistedOhlcvIngestionService;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = TradeModelApplication.class)
@Transactional
@Tag("core-regression")
class PersistedOhlcvIngestionServiceImplTest {

    @Autowired
    private PersistedOhlcvIngestionService ingestionService;
    @Autowired
    private PersistedOhlcvBarMapper mapper;
    @Autowired
    private PersistedOhlcvQueryService queryService;

    @Test
    void validatedBarPersistsThroughAuthoritativeWriter() {
        OhlcvIngestionBatch batch = batch("BTCUSDT", "5m", freshBar("BTCUSDT", "5m"));

        OhlcvIngestionResult result = ingestionService.ingest(batch);

        assertThat(result.ready()).isTrue();
        assertThat(result.insertedCount()).isEqualTo(1);
        PersistedOhlcvBarDO stored = mapper.selectBySourceKey("BTCUSDT", "5m",
                batch.bars().get(0).openTimeMs(), "BINANCE_PUBLIC", "SPOT");
        assertThat(stored).isNotNull();
        assertThat(stored.getSourceStatus()).isEqualTo("READY");
        assertThat(stored.getFreshnessStatus()).isEqualTo("FRESH");
        assertThat(stored.getFetchTime()).isNotNull();
        assertThat(stored.getProvenanceVersion()).isEqualTo("test-provider-v1");
        assertThat(stored.getIngestionRunId()).isEqualTo("run-BTCUSDT-5m");
        assertThat(stored.getRawPayloadHash()).hasSize(64);
        assertThat(stored.getOpenPrice()).isEqualByComparingTo("100.00000000");
        assertThat(stored.getVolume()).isEqualByComparingTo("1000.00000000");
    }

    @Test
    void persistedKrakenBarsCarryRealProviderProvenanceAndPersistedBarsRetainSourceTrace() {
        OhlcvBarInput bar = freshBar("BTCUSDT", "5m");
        OhlcvIngestionBatch batch = new OhlcvIngestionBatch(
                "KRAKEN", "SPOT", "/0/public/OHLC", OhlcvSourceState.READY,
                Instant.now(), "kraken-public-ohlc-v1", 1,
                "trace-kraken-real", "run-kraken-real", List.of(bar));

        OhlcvIngestionResult result = ingestionService.ingest(batch);
        PersistedOhlcvBarDO stored = mapper.selectBySourceKey(
                "BTCUSDT", "5m", bar.openTimeMs(), "KRAKEN", "SPOT");

        assertThat(result.ready()).isTrue();
        assertThat(stored).isNotNull();
        assertThat(stored.getProvider()).isEqualTo("KRAKEN");
        assertThat(stored.getProviderMarketType()).isEqualTo("SPOT");
        assertThat(stored.getSourceEndpoint()).isEqualTo("/0/public/OHLC");
        assertThat(stored.getSourceTraceId()).isEqualTo("trace-kraken-real");
        assertThat(stored.getFetchTime()).isNotNull();
        assertThat(stored.getClosed()).isTrue();
        assertThat(stored.getQualityStatus()).isEqualTo("OK");
    }

    @Test
    void identicalBarIngestionIsIdempotent() {
        OhlcvIngestionBatch batch = batch("ETHUSDT", "15m", freshBar("ETHUSDT", "15m"));

        OhlcvIngestionResult first = ingestionService.ingest(batch);
        OhlcvIngestionResult second = ingestionService.ingest(batch);

        assertThat(first.insertedCount()).isEqualTo(1);
        assertThat(second.insertedCount()).isZero();
        assertThat(second.idempotentCount()).isEqualTo(1);
        assertThat(mapper.selectLatestClosedWindow("ETHUSDT", "15m", 10)).hasSize(1);
    }

    @Test
    void conflictingDuplicateFailsClosed() {
        OhlcvBarInput original = freshBar("SOLUSDT", "1h");
        ingestionService.ingest(batch("SOLUSDT", "1h", original));
        OhlcvBarInput conflicting = new OhlcvBarInput(original.symbol(), original.timeframe(),
                original.openTimeMs(), original.closeTimeMs(), original.open(), original.high(), original.low(),
                new BigDecimal("101.25"), original.volume(), original.quoteVolume(), original.tradeCount(),
                original.takerBuyBaseVolume(), original.takerBuyQuoteVolume(), true);

        OhlcvIngestionResult result = ingestionService.ingest(batch("SOLUSDT", "1h", conflicting));

        assertThat(result.accepted()).isFalse();
        assertThat(result.insertedCount()).isZero();
        assertThat(result.reasonCodes()).contains("CONFLICTING_DUPLICATE_CONTENT");
        assertThat(mapper.selectLatestClosedWindow("SOLUSDT", "1h", 10)).hasSize(1);
    }

    @Test
    void invalidOhlcvGeometryIsRejected() {
        OhlcvBarInput valid = freshBar("BNBUSDT", "4h");
        OhlcvBarInput invalid = new OhlcvBarInput(valid.symbol(), valid.timeframe(), valid.openTimeMs(),
                valid.closeTimeMs(), new BigDecimal("100"), new BigDecimal("99"), new BigDecimal("98"),
                new BigDecimal("100"), valid.volume(), valid.quoteVolume(), valid.tradeCount(),
                valid.takerBuyBaseVolume(), valid.takerBuyQuoteVolume(), true);

        OhlcvIngestionResult result = ingestionService.ingest(batch("BNBUSDT", "4h", invalid));

        assertThat(result.accepted()).isFalse();
        assertThat(result.reasonCodes()).contains("OHLC_GEOMETRY_INVALID");
        assertThat(mapper.selectLatestClosedWindow("BNBUSDT", "4h", 10)).isEmpty();
    }

    @Test
    void invalidTimestampOrderIsRejected() {
        long now = System.currentTimeMillis() - 1_000L;
        OhlcvBarInput invalid = bar("LTCUSDT", "5m", now, now);

        OhlcvIngestionResult result = ingestionService.ingest(batch("LTCUSDT", "5m", invalid));

        assertThat(result.accepted()).isFalse();
        assertThat(result.reasonCodes()).contains("TIMESTAMP_ORDER_INVALID");
        assertThat(mapper.selectLatestClosedWindow("LTCUSDT", "5m", 10)).isEmpty();
    }

    @Test
    void futureBarIsRejected() {
        long futureClose = System.currentTimeMillis() + 120_000L;
        OhlcvBarInput future = bar("DOTUSDT", "5m", futureClose - 60_000L, futureClose);

        OhlcvIngestionResult result = ingestionService.ingest(batch("DOTUSDT", "5m", future));

        assertThat(result.accepted()).isFalse();
        assertThat(result.reasonCodes()).contains("BAR_IN_FUTURE");
        assertThat(mapper.selectLatestClosedWindow("DOTUSDT", "5m", 10)).isEmpty();
    }

    @Test
    void unsupportedTimeframeIsRejected() {
        long close = System.currentTimeMillis() - 1_000L;
        OhlcvBarInput unsupported = bar("AVAXUSDT", "1m", close - 60_000L + 1L, close);

        OhlcvIngestionResult result = ingestionService.ingest(batch("AVAXUSDT", "1m", unsupported));

        assertThat(result.accepted()).isFalse();
        assertThat(result.reasonCodes()).contains("TIMEFRAME_UNSUPPORTED");
        assertThat(mapper.selectLatestClosedWindow("AVAXUSDT", "1m", 10)).isEmpty();
    }

    @Test
    void staleBarIsNotReportedReady() {
        long interval = timeframeMs("5m");
        long close = System.currentTimeMillis() - (interval * 10);
        OhlcvBarInput stale = bar("XRPUSDT", "5m", close - interval + 1, close);

        OhlcvIngestionResult result = ingestionService.ingest(batch("XRPUSDT", "5m", stale));

        assertThat(result.sourceState()).isEqualTo(OhlcvSourceState.STALE);
        assertThat(result.freshnessStatus()).isEqualTo(OhlcvFreshnessStatus.STALE);
        assertThat(result.ready()).isFalse();
        assertThat(queryService.evaluateReadinessForSource(
                "XRPUSDT", "5m", 1, interval * 20, "BINANCE", "SPOT").getStatus())
                .isEqualTo(PersistedOhlcvReadinessStatus.STALE);
    }

    @Test
    void missingProviderDoesNotBecomeHealthy() {
        OhlcvIngestionBatch valid = batch("DOGEUSDT", "5m", freshBar("DOGEUSDT", "5m"));
        OhlcvIngestionBatch missingProvider = new OhlcvIngestionBatch("", valid.providerMarketType(),
                valid.sourceEndpoint(), valid.sourceState(), valid.fetchTime(), valid.provenanceVersion(),
                valid.sourceVersion(), valid.traceId(), valid.ingestionRunId(), valid.bars());

        OhlcvIngestionResult result = ingestionService.ingest(missingProvider);

        assertThat(result.ready()).isFalse();
        assertThat(result.reasonCodes()).contains("PROVIDER_MISSING");
        assertThat(mapper.selectLatestClosedWindow("DOGEUSDT", "5m", 10)).isEmpty();
    }

    @Test
    void allFourProductTimeframesCanPersist() {
        for (String timeframe : List.of("5m", "15m", "1h", "4h")) {
            OhlcvIngestionResult result = ingestionService.ingest(
                    batch("ADAUSDT", timeframe, freshBar("ADAUSDT", timeframe)));
            assertThat(result.ready()).as(timeframe).isTrue();
        }
        assertThat(mapper.selectLatestClosedWindow("ADAUSDT", "5m", 10)).hasSize(1);
        assertThat(mapper.selectLatestClosedWindow("ADAUSDT", "15m", 10)).hasSize(1);
        assertThat(mapper.selectLatestClosedWindow("ADAUSDT", "1h", 10)).hasSize(1);
        assertThat(mapper.selectLatestClosedWindow("ADAUSDT", "4h", 10)).hasSize(1);
    }

    private static OhlcvIngestionBatch batch(String symbol, String timeframe, OhlcvBarInput bar) {
        return new OhlcvIngestionBatch("BINANCE_PUBLIC", "SPOT", "/api/v3/klines",
                OhlcvSourceState.READY, Instant.now(), "test-provider-v1", 1,
                "trace-" + symbol + "-" + timeframe, "run-" + symbol + "-" + timeframe, List.of(bar));
    }

    private static OhlcvBarInput freshBar(String symbol, String timeframe) {
        long close = System.currentTimeMillis() - 1_000L;
        long interval = timeframeMs(timeframe);
        return bar(symbol, timeframe, close - interval + 1, close);
    }

    private static OhlcvBarInput bar(String symbol, String timeframe, long openTime, long closeTime) {
        return new OhlcvBarInput(symbol, timeframe, openTime, closeTime,
                new BigDecimal("100.00"), new BigDecimal("102.00"), new BigDecimal("99.00"),
                new BigDecimal("101.00"), new BigDecimal("1000.00"), new BigDecimal("100000.00"),
                100L, new BigDecimal("500.00"), new BigDecimal("50000.00"), true);
    }

    private static long timeframeMs(String timeframe) {
        return switch (timeframe) {
            case "5m" -> 5L * 60_000L;
            case "15m" -> 15L * 60_000L;
            case "1h" -> 60L * 60_000L;
            case "4h" -> 4L * 60L * 60_000L;
            default -> throw new IllegalArgumentException(timeframe);
        };
    }
}
