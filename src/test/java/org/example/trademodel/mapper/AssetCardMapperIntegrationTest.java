package org.example.trademodel.mapper;

import org.example.trademodel.assetcard.AssetCardMarketDataService.SpotBar;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@org.junit.jupiter.api.Tag("core-regression")
class AssetCardMapperIntegrationTest {
    private JdbcTemplate jdbc;
    private AssetCardMapper mapper;
    private final String symbol = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    private final Instant now = Instant.parse("2026-09-10T12:00:00Z");

    @BeforeEach
    void isolatedDisposableDatabaseOnly() throws Exception {
        var datasource = new DriverManagerDataSource("jdbc:h2:mem:card_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        try (var connection = datasource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:card_");
        }
        jdbc = new JdbcTemplate(datasource);
        String migration = Files.readString(Path.of("src/main/resources/db/migration/V24__asset_card_live_signal.sql"));
        for (String sql : migration.split(";")) {
            if (!sql.isBlank()) jdbc.execute(sql);
        }
        mapper = new AssetCardMapper(jdbc);
    }

    @org.junit.jupiter.api.AfterEach
    void destroyDisposableDatabase() { if (jdbc != null) jdbc.execute("SHUTDOWN"); }

    @Test
    void versionsRemainMonotonicAcrossInstancesAndOldPublishIsRejected() {
        long first = mapper.nextSnapshotVersion(symbol);
        long next = new AssetCardMapper(jdbc).nextSnapshotVersion(symbol);
        assertThat(next).isGreaterThan(first);
        assertThat(mapper.saveSnapshot(symbol, next, "{\"truth\":2}", now)).isEqualTo(1);
        assertThat(mapper.saveSnapshot(symbol, first, "{\"truth\":1}", now.minusSeconds(1))).isZero();
        assertThat(mapper.selectSnapshotJson(symbol)).isEqualTo("{\"truth\":2}");
        assertThat(mapper.saveSnapshot(symbol, next + 20, "{}", now)).isZero();
    }

    @Test
    void concurrentVersionAllocationHasNoDuplicates() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        try {
            var tasks = IntStream.range(0, 24).mapToObj(i -> (Callable<Long>) () -> mapper.nextSnapshotVersion(symbol)).toList();
            var values = pool.invokeAll(tasks).stream().map(f -> {
                try { return f.get(); } catch (Exception error) { throw new IllegalStateException(error); }
            }).toList();
            assertThat(values).doesNotHaveDuplicates().hasSize(24);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void barIdentityAndAvailabilityPreventDuplicateOrFutureKnowledge() {
        var bar = new SpotBar(symbol, "5m", now.minusSeconds(300), now.minusMillis(1),
                new BigDecimal("100"), new BigDecimal("102"), new BigDecimal("99"), new BigDecimal("101"),
                new BigDecimal("10"), new BigDecimal("6"), 20L, now.plusSeconds(2));
        assertThat(mapper.upsertClosedBar(bar)).isEqualTo(1);
        assertThat(mapper.upsertClosedBar(bar)).isZero();
        assertThat(mapper.selectClosedBars(symbol, "5m", now, 100)).isEmpty();
        var stored = mapper.selectClosedBars(symbol, "5m", now.plusSeconds(2), 100);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).symbol()).isEqualTo(symbol);
        assertThat(stored.get(0).close()).isEqualByComparingTo(bar.close());
        assertThat(stored.get(0).availableAt()).isEqualTo(now.plusSeconds(2));
    }

    @Test
    void featureAuditIsImmutableAndCannotLeakLaterAvailabilityIntoTraining() {
        assertThat(mapper.saveFeatureHistory(symbol, now, now.plusSeconds(2), "{\"spread\":1}")).isEqualTo(1);
        assertThat(mapper.saveFeatureHistory(symbol, now, now.plusSeconds(3), "{\"spread\":999}")).isZero();
        assertThat(mapper.selectFeatureHistory(symbol, now, now, now, 100)).isEmpty();
        assertThat(mapper.selectFeatureHistory(symbol, now, now, now.plusSeconds(2), 100))
                .extracting(AssetCardMapper.FeatureHistory::payloadJson).containsExactly("{\"spread\":1}");
    }

    @Test
    void existingHistoryRequiresSpotProvenanceQualityAndBothAvailabilityTimes() throws Exception {
        String schema = Files.readString(Path.of("src/main/resources/schema.sql"));
        int start = schema.indexOf("CREATE TABLE IF NOT EXISTS tm_persisted_ohlcv_bar (");
        jdbc.execute(schema.substring(start, schema.indexOf(";", start) + 1));
        insertHistory("BINANCE_PUBLIC", "SPOT", "OK", now.minusSeconds(1), now, true);
        insertHistory("BINANCE_PUBLIC", "USDT_M_PERP", "OK", now, now, true);
        insertHistory("KRAKEN_PUBLIC", "SPOT", "OK", now, now, true);
        insertHistory("BINANCE_PUBLIC", "SPOT", "BAD", now, now, true);
        insertHistory("BINANCE_PUBLIC", "SPOT", "OK", now.plusSeconds(1), now, true);
        insertHistory("BINANCE_PUBLIC", "SPOT", "OK", now, now.plusSeconds(1), true);
        insertHistory("BINANCE_PUBLIC", "SPOT", "OK", now, now, false);
        var rows = mapper.selectExistingSpotBars(symbol, "5m", now, 100);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).availableAt()).isEqualTo(now);
        assertThat(rows.get(0).close()).isEqualByComparingTo("101");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM tm_persisted_ohlcv_bar", Integer.class)).isEqualTo(7);
    }

    private void insertHistory(String provider, String marketType, String quality, Instant fetched, Instant ingested, boolean closed) {
        jdbc.update("""
                INSERT INTO tm_persisted_ohlcv_bar(symbol,timeframe,open_time_ms,close_time_ms,
                    open_price,high_price,low_price,close_price,volume,is_closed,provider,provider_market_type,
                    source_endpoint,source_batch_id,source_trace_id,source_version,fetch_time,source_status,
                    freshness_status,provenance_version,ingestion_run_id,ingested_at,quality_status)
                VALUES (?,'5m',?,?,100,102,99,101,10,?,?,?,'public-spot-kline','isolated','isolated',1,?,'READY',
                    'FRESH','v1','isolated',?,?)
                """, symbol, now.minusSeconds(300).toEpochMilli(), now.minusMillis(1).toEpochMilli(), closed, provider, marketType,
                java.time.LocalDateTime.ofInstant(fetched, java.time.ZoneOffset.UTC),
                java.time.LocalDateTime.ofInstant(ingested, java.time.ZoneOffset.UTC), quality);
    }
}
