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
    @org.junit.jupiter.api.io.TempDir Path archiveDirectory;
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
        assertThat(mapper.saveSnapshot(symbol, 0, next, "{\"truth\":2}", now)).isEqualTo(1);
        assertThat(mapper.saveSnapshot(symbol, 0, first, "{\"truth\":1}", now.minusSeconds(1))).isZero();
        assertThat(mapper.selectSnapshotJson(symbol)).isEqualTo("{\"truth\":2}");
        assertThat(mapper.saveSnapshot(symbol, next, next + 20, "{}", now)).isZero();
    }

    @Test
    void disabledProductionWriterCannotFallBackToDefaultEvenWhenDefaultContainsWritableCardTables() throws Exception {
        var properties = new org.example.trademodel.assetcard.AssetCardProperties();
        try (var writer = new org.example.trademodel.assetcard.AssetCardDataSourceConfiguration.AssetCardWriter(properties)) {
            var productionRoute = new AssetCardMapper(jdbc, writer);
            String schema = Files.readString(Path.of("src/main/resources/schema.sql"));
            int start = schema.indexOf("CREATE TABLE IF NOT EXISTS tm_persisted_ohlcv_bar (");
            jdbc.execute(schema.substring(start, schema.indexOf(";", start) + 1));
            insertHistory("BINANCE_PUBLIC", "SPOT", "OK", now, now, true);
            assertThat(productionRoute.selectExistingSpotBars(symbol, "5m", now, 10)).hasSize(1);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> productionRoute.nextSnapshotVersion(symbol))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            org.assertj.core.api.Assertions.assertThatThrownBy(() -> productionRoute.saveFeatureHistory(symbol, now, now, "{}"))
                    .isInstanceOf(org.springframework.dao.DataAccessException.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM tm_asset_card_snapshot", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM tm_asset_card_feature_history", Integer.class)).isZero();
            assertThat(productionRoute.inspectWriterPermissions().writable()).isFalse();
            assertThat(productionRoute.inspectWriterPermissions().cleanupAllowed()).isFalse();
        }
    }

    @Test
    void springUsesOnlyExplicitDedicatedWriterConstructorNotTheIsolatedFixtureConstructor() throws Exception {
        assertThat(AssetCardMapper.class.getConstructor(JdbcTemplate.class,
                org.example.trademodel.assetcard.AssetCardDataSourceConfiguration.AssetCardWriter.class)
                .isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class)).isTrue();
        assertThat(AssetCardMapper.class.getConstructor(JdbcTemplate.class)
                .isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class)).isFalse();
    }

    @Test
    void independentWritersMustNotOverwriteTheSameReadVersion() {
        var secondWriter = new AssetCardMapper(jdbc);
        long firstAllocated = mapper.nextSnapshotVersion(symbol);
        long secondAllocated = secondWriter.nextSnapshotVersion(symbol);
        // Both writers read snapshot version 0. A larger allocated number is
        // not permission to replace another writer's committed atomic fields.
        assertThat(mapper.saveSnapshot(symbol, 0, firstAllocated, "{\"signal\":\"LONG\"}", now)).isEqualTo(1);
        assertThat(secondWriter.saveSnapshot(symbol, 0, secondAllocated, "{\"signal\":\"STALE\"}", now)).isZero();
        assertThat(mapper.selectSnapshotJson(symbol)).isEqualTo("{\"signal\":\"LONG\"}");
        long rebasedVersion = secondWriter.nextSnapshotVersion(symbol);
        assertThat(secondWriter.saveSnapshot(symbol, firstAllocated, rebasedVersion,
                "{\"signal\":\"SHORT\"}", now)).isEqualTo(1);
        assertThat(mapper.selectSnapshotJson(symbol)).isEqualTo("{\"signal\":\"SHORT\"}");
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
    void inferenceIdentityNormalizesClosedBoundaryButNeverUsesProcessingTime() {
        assertThat(mapper.saveInference(symbol, now, now.plusSeconds(2), "{\"status\":\"TIMEOUT\"}")).isEqualTo(1);
        assertThat(mapper.saveInference(symbol, now.minusMillis(1), now.plusSeconds(4), "{\"status\":\"RECOMPUTED\"}")).isZero();
        assertThat(mapper.selectInference(symbol, now, now)).isEmpty();
        assertThat(mapper.selectInference(symbol, now.minusMillis(1), now.plusSeconds(2)).orElseThrow().payloadJson())
                .isEqualTo("{\"status\":\"TIMEOUT\"}");
        assertThat(mapper.saveInference(symbol, now.minusSeconds(300), now.plusSeconds(2), "{\"status\":\"OTHER_BAR\"}")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.saveInference(symbol, now.plusSeconds(2), now.plusSeconds(2), "{}"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(mapper.selectFeatureHistory(symbol, now.minusSeconds(600), now, now.plusSeconds(5), 10)).isEmpty();
    }

    @Test
    void concurrentInferenceWritersKeepOneImmutableResult() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        try {
            var tasks = IntStream.range(0, 16).mapToObj(i -> (Callable<Integer>) () ->
                    mapper.saveInference(symbol, now, now.plusSeconds(i), "{\"writer\":" + i + "}")).toList();
            int inserted = 0;
            for (var result : pool.invokeAll(tasks)) inserted += result.get();
            assertThat(inserted).isEqualTo(1);
            assertThat(mapper.selectHistory(symbol, AssetCardMapper.HistoryKind.INFERENCE, now.minusSeconds(1), now,
                    now.plusSeconds(20), 100)).hasSize(1);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void tradeIdentitySeparatesAggregateIdAndSourceVersionAndNeverOverwritesItsObservation() {
        var quote = new org.example.trademodel.assetcard.AssetCardMarketDataService.SpotQuote("BTCUSDT",
                BigDecimal.TEN, BigDecimal.ONE, 42, now, now.plusSeconds(1));
        String instrument = "BINANCE:SPOT:NONE:BTC/USDT";
        assertThat(mapper.saveTradeObservation(quote, instrument, "SPOT_V1", "{\"price\":10}")).isEqualTo(1);
        assertThat(mapper.saveTradeObservation(quote, instrument, "SPOT_V1", "{\"price\":999}")).isZero();
        assertThat(mapper.saveTradeObservation(quote, instrument, "SPOT_V2", "{\"price\":10}")).isEqualTo(1);
        assertThat(mapper.selectHistory("BTCUSDT", AssetCardMapper.HistoryKind.TRADE, now, now, now, 100)).isEmpty();
        assertThat(mapper.selectHistory("BTCUSDT", AssetCardMapper.HistoryKind.TRADE, now, now, now.plusSeconds(1), 100))
                .extracting(AssetCardMapper.TypedHistory::payloadJson).containsOnly("{\"price\":10}").hasSize(2);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.saveTradeObservation(quote,
                "BINANCE:SPOT:NONE:ETH/USDT", "SPOT_V1", "{}" )).isInstanceOf(IllegalArgumentException.class);
        assertThat(mapper.selectFeatureHistory("BTCUSDT", now, now, now.plusSeconds(1), 100)).isEmpty();
    }

    @Test
    void labelsBindSideVersionAndActualSignalTradeAndRequireMaturity() {
        String instrument = "BINANCE:SPOT:NONE:BTC/USDT";
        Instant mature = now.plusSeconds(14400);
        assertThat(mapper.saveLabel("BTCUSDT", now, instrument, "SPOT_V1", 42, "F2", "TP1_SL075_4H", "LONG", mature, "{\"label\":1}")).isEqualTo(1);
        assertThat(mapper.saveLabel("BTCUSDT", now, instrument, "SPOT_V1", 42, "F2", "TP1_SL075_4H", "LONG", mature, "{\"label\":1}")).isZero();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.saveLabel("BTCUSDT", now, instrument, "SPOT_V1", 42, "F2", "TP1_SL075_4H", "LONG", mature.plusSeconds(1), "{\"label\":0}"))
                .isInstanceOf(IllegalStateException.class).hasMessage("ASSET_CARD_IMMUTABLE_LABEL_CONFLICT");
        assertThat(mapper.saveLabel("BTCUSDT", now, instrument, "SPOT_V1", 42, "F2", "TP1_SL075_4H", "SHORT", mature, "{}")).isEqualTo(1);
        assertThat(mapper.saveLabel("BTCUSDT", now, instrument, "SPOT_V1", 43, "F2", "TP1_SL075_4H", "LONG", mature, "{}")).isEqualTo(1);
        assertThat(mapper.saveLabel("BTCUSDT", now, instrument, "SPOT_V1", 42, "F3", "TP1_SL075_4H", "LONG", mature, "{}")).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.saveLabel("BTCUSDT", now, instrument, "SPOT_V1",
                44, "F2", "TP1_SL075_4H", "LONG", mature.minusSeconds(1), "{}")).isInstanceOf(IllegalArgumentException.class);
        assertThat(mapper.selectHistory("BTCUSDT", AssetCardMapper.HistoryKind.LABEL, now, now, mature.minusSeconds(1), 10)).isEmpty();
        assertThat(mapper.selectHistory("BTCUSDT", AssetCardMapper.HistoryKind.LABEL, now, now, mature.plusSeconds(2), 10)).hasSize(4);
    }

    @Test
    void writerPermissionInspectionIsReadOnlyAndH2IsNotProductionReadiness() {
        assertThat(mapper.inspectWriterPermissions().writable()).isFalse();
        assertThat(mapper.inspectWriterPermissions().reason()).isEqualTo("H2_TEST_ONLY_NOT_PRODUCTION_WRITER");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tm_asset_card_snapshot", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tm_asset_card_feature_history", Integer.class)).isZero();
    }

    @Test
    void historyKeysetPagesDoNotLoseRecordsWithIdenticalObservationTimes() {
        String instrument = "BINANCE:SPOT:NONE:BTC/USDT";
        Instant mature = now.plusSeconds(14400);
        for (int i = 0; i < 6; i++) mapper.saveLabel("BTCUSDT", now, instrument, "SPOT_V1", i + 1,
                "F2", "TP1_SL075_4H", "LONG", mature, "{\"fixture\":" + i + "}");
        mapper.saveLabel("BTCUSDT", now.plusSeconds(1), instrument, "SPOT_V1", 20,
                "F2", "TP1_SL075_4H", "LONG", mature.plusSeconds(1), "{\"nextTime\":true}");
        mapper.saveLabel("BTCUSDT", now, instrument, "SPOT_V1", 30,
                "F2", "TP1_SL075_4H", "LONG", mature.plusSeconds(3), "{\"notYetAvailable\":true}");
        mapper.saveLabel("BTCUSDT", now.plusSeconds(2), instrument, "SPOT_V1", 40,
                "F2", "TP1_SL075_4H", "LONG", mature.plusSeconds(2), "{\"exclusiveEnd\":true}");
        mapper.saveFeatureHistory("BTCUSDT", now, now, "{\"otherKind\":true}");
        var expected = mapper.selectHistory("BTCUSDT", AssetCardMapper.HistoryKind.LABEL,
                now, now.plusSeconds(1), mature.plusSeconds(2), 100);
        var collected = new java.util.ArrayList<AssetCardMapper.TypedHistory>();
        Instant cursorAt = null;
        String cursorKey = null;
        for (int pageNumber = 0; pageNumber < 5; pageNumber++) {
            var page = mapper.selectHistoryPage("BTCUSDT", AssetCardMapper.HistoryKind.LABEL,
                    now, now.plusSeconds(2), mature.plusSeconds(2), cursorAt, cursorKey, 2);
            if (page.isEmpty()) break;
            assertThat(page).hasSizeLessThanOrEqualTo(2);
            collected.addAll(page);
            var last = page.get(page.size() - 1);
            cursorAt = last.signalAsOf(); cursorKey = last.recordKey();
        }
        assertThat(collected).containsExactlyElementsOf(expected).hasSize(7);
        assertThat(collected).extracting(AssetCardMapper.TypedHistory::recordKey).doesNotHaveDuplicates();
        assertThat(collected.stream().filter(row -> row.signalAsOf().equals(now))).hasSize(6);
        assertThat(mapper.selectHistoryPage(symbol, AssetCardMapper.HistoryKind.LABEL,
                now, now.plusSeconds(2), mature.plusSeconds(2), null, null, 2)).isEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.selectHistoryPage("BTCUSDT",
                AssetCardMapper.HistoryKind.LABEL, now, now.plusSeconds(2), mature, now, null, 2))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inferenceSymbolEnumerationIncludesUnsubscribedWorkAndExcludesOtherHistoryKinds() {
        mapper.saveInference(symbol, now, now, "{\"unsubscribed\":true}");
        mapper.saveInference(symbol, now.plusSeconds(300), now.plusSeconds(300), "{}");
        mapper.saveInference("OTHERUSDT", now, now.plusSeconds(1), "{}");
        mapper.saveFeatureHistory("FEATUREONLYUSDT", now, now, "{}");
        assertThat(mapper.selectInferenceSymbols()).containsExactlyElementsOf(
                java.util.stream.Stream.of(symbol, "OTHERUSDT").sorted().toList());
        assertThat(mapper.selectInferenceSymbols()).doesNotHaveDuplicates();
    }

    @Test
    void labelBarsRespectExactIntervalRangeCloseAndAvailabilityCutoff() {
        for (int minute = -1; minute <= 4; minute++) {
            Instant open = now.plusSeconds(minute * 60L), close = open.plusSeconds(60).minusMillis(1);
            Instant available = minute == 2 ? now.plusSeconds(181) : open.plusSeconds(60);
            mapper.upsertClosedBar(new SpotBar(symbol, "1m", open, close, BigDecimal.ONE, BigDecimal.ONE,
                    BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, null, null, available));
        }
        mapper.upsertClosedBar(new SpotBar(symbol, "5m", now, now.plusSeconds(300).minusMillis(1),
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, null, null, now.plusSeconds(300)));
        assertThat(mapper.selectLabelBars(symbol, "1m", now, now.plusSeconds(240), now.plusSeconds(180)))
                .extracting(SpotBar::openTime).containsExactly(now, now.plusSeconds(60));
        assertThat(mapper.selectLabelBars(symbol, "1m", now, now.plusSeconds(240), now.plusSeconds(600)))
                .extracting(SpotBar::openTime).containsExactly(now, now.plusSeconds(60), now.plusSeconds(120), now.plusSeconds(180));
        assertThat(mapper.selectLabelBars("OTHERUSDT", "1m", now, now.plusSeconds(240), now.plusSeconds(600))).isEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.selectLabelBars(symbol, "1h", now, now.plusSeconds(240), now))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.selectLabelBars(symbol, "1m", now, now.plusSeconds(18001), now))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void horizonTradeUsesOneLatestPointInTimeObservationAndNeverLateArrival() {
        String instrument = "BINANCE:SPOT:NONE:BTC/USDT";
        Instant end = now.plusSeconds(14400), from = end.minusSeconds(10);
        var observations = java.util.List.of(
                new org.example.trademodel.assetcard.AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.TEN, BigDecimal.ONE, 1, from.minusMillis(1), from),
                new org.example.trademodel.assetcard.AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.TEN, BigDecimal.ONE, 2, end.minusSeconds(1), end),
                new org.example.trademodel.assetcard.AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.TEN, BigDecimal.ONE, 3, end, end.plusMillis(1)),
                new org.example.trademodel.assetcard.AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.TEN, BigDecimal.ONE, 4, end.plusMillis(1), end.plusMillis(1)));
        for (var observation : observations) mapper.saveTradeObservation(observation, instrument, "SPOT_V1", "{\"trade\":" + observation.tradeId() + "}");
        mapper.saveFeatureHistory("BTCUSDT", end, end, "{\"notATrade\":true}");
        assertThat(mapper.selectHorizonTrade("BTCUSDT", from, end).orElseThrow().payloadJson()).isEqualTo("{\"trade\":2}");
        assertThat(mapper.selectHorizonTrade(symbol, from, end)).isEmpty();
        for (long id : new long[]{5, 6}) mapper.saveTradeObservation(
                new org.example.trademodel.assetcard.AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.TEN, BigDecimal.ONE, id, end, end),
                instrument, "SPOT_V1", "{\"trade\":" + id + "}");
        var eligibleAtEnd = mapper.selectHistory("BTCUSDT", AssetCardMapper.HistoryKind.TRADE, end, end, end, 10);
        assertThat(eligibleAtEnd).hasSize(2);
        assertThat(mapper.selectHorizonTrade("BTCUSDT", from, end).orElseThrow())
                .isEqualTo(eligibleAtEnd.get(eligibleAtEnd.size() - 1));
    }

    @Test
    void multipleMapperInstancesKeepIdenticalLabelsIdempotentAndRejectConcurrentConflicts() throws Exception {
        var pool = Executors.newFixedThreadPool(4);
        Instant mature = now.plusSeconds(14400);
        String instrument = "BINANCE:SPOT:NONE:BTC/USDT";
        try {
            var start = new java.util.concurrent.CountDownLatch(1);
            var writers = IntStream.range(0, 8).mapToObj(i -> pool.submit((Callable<Integer>) () -> {
                start.await();
                return new AssetCardMapper(jdbc).saveLabel("BTCUSDT", now, instrument, "SPOT_V1", 42,
                        "F2", "TP1_SL075_4H", "LONG", mature, "{\"label\":1}");
            })).toList();
            start.countDown();
            int inserted = 0;
            for (var writer : writers) inserted += writer.get();
            assertThat(inserted).isEqualTo(1);
            var conflicts = IntStream.range(0, 8).mapToObj(i -> (Callable<String>) () -> {
                try {
                    new AssetCardMapper(jdbc).saveLabel("BTCUSDT", now, instrument, "SPOT_V1", 42,
                            "F2", "TP1_SL075_4H", "LONG", mature.plusSeconds(1), "{\"label\":0}");
                    return "UNEXPECTED_SUCCESS";
                } catch (IllegalStateException rejected) { return rejected.getMessage(); }
            }).toList();
            for (var result : pool.invokeAll(conflicts)) assertThat(result.get()).isEqualTo("ASSET_CARD_IMMUTABLE_LABEL_CONFLICT");
            assertThat(mapper.selectHistory("BTCUSDT", AssetCardMapper.HistoryKind.LABEL, now, now, mature.plusSeconds(1), 10))
                    .extracting(AssetCardMapper.TypedHistory::payloadJson).containsExactly("{\"label\":1}");
        } finally { pool.shutdownNow(); }
    }

    @Test
    void archivedHistoryCleanupRequiresExactConfirmedScopeAndNeverResetsSnapshotCounter() throws Exception {
        Instant old = now.minusSeconds(43200);
        mapper.saveFeatureHistory(symbol, old, old.plusSeconds(1), "{\"archived\":1}");
        mapper.saveFeatureHistory(symbol, old.plusSeconds(1), now.plusSeconds(1), "{\"lateArrival\":1}");
        mapper.saveFeatureHistory("OTHERUSDT", old, old, "{\"other\":1}");
        long version = mapper.nextSnapshotVersion(symbol);
        mapper.saveSnapshot(symbol, 0, version, "{\"live\":1}", now);
        var keys = mapper.selectHistory(symbol, AssetCardMapper.HistoryKind.FEATURE, old, now, now.plusSeconds(2), 10)
                .stream().map(AssetCardMapper.TypedHistory::recordKey).toList();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.pruneArchivedHistory(null, 10))
                .isInstanceOf(IllegalArgumentException.class);
        var rows=mapper.selectHistory(symbol,AssetCardMapper.HistoryKind.FEATURE,old,now,now.plusSeconds(2),10);
        Path file=archive(rows,java.util.List.of()); String sha=file.getFileName().toString().substring(0,64);
        var confirmed = new AssetCardMapper.ArchiveConfirmation(symbol, AssetCardMapper.HistoryKind.FEATURE, keys,
                old, now.minusSeconds(21600), now, sha, now,file);
        assertThat(mapper.pruneArchivedHistory(confirmed, 1)).isEqualTo(1);
        assertThat(mapper.pruneArchivedHistory(confirmed, 10)).isZero();
        assertThat(mapper.selectHistory(symbol, AssetCardMapper.HistoryKind.FEATURE, old, now, now.plusSeconds(2), 10))
                .extracting(AssetCardMapper.TypedHistory::payloadJson).containsExactly("{\"lateArrival\":1}");
        assertThat(mapper.selectFeatureHistory("OTHERUSDT", old, now, now, 10)).hasSize(1);
        assertThat(mapper.selectSnapshotJson(symbol)).isEqualTo("{\"live\":1}");
        assertThat(mapper.nextSnapshotVersion(symbol)).isGreaterThan(version);
    }

    @Test
    void archivedBarsCleanupIsBoundedToConfirmedIntervalsAndAvailability() throws Exception {
        Instant old = now.minusSeconds(43200);
        var bar = new SpotBar(symbol, "5m", old, old.plusSeconds(300).minusMillis(1), BigDecimal.ONE, BigDecimal.ONE,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN, null, null, old.plusSeconds(300));
        mapper.upsertClosedBar(bar);
        Path file=archive(java.util.List.of(),mapper.selectArchiveBars(symbol,now,now,10));
        String sha=file.getFileName().toString().substring(0,64);
        var confirmed = new AssetCardMapper.BarArchiveConfirmation(symbol,
                java.util.List.of(new AssetCardMapper.BarIdentity("1m", old)), old, now.minusSeconds(21600), now, sha, now,file);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.pruneArchivedBars(confirmed,10)).isInstanceOf(IllegalArgumentException.class);
        var exact = new AssetCardMapper.BarArchiveConfirmation(symbol,
                java.util.List.of(new AssetCardMapper.BarIdentity("5m", old)), old, now.minusSeconds(21600), now, sha, now,file);
        assertThat(mapper.pruneArchivedBars(exact, 10)).isEqualTo(1);
        assertThat(mapper.selectClosedBars(symbol, "5m", now, 10)).isEmpty();
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new AssetCardMapper.BarArchiveConfirmation(symbol,
                exact.bars(), old, now.minusSeconds(21600), now, "", now)).isInstanceOf(IllegalArgumentException.class);
    }

    private Path archive(java.util.List<AssetCardMapper.TypedHistory> rows,java.util.List<SpotBar> bars) throws Exception {
        var json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
                .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        byte[] bytes=json.writeValueAsBytes(new AssetCardMapper.VerifiedArchive(1,"ASSET_CARD_HISTORY_ARCHIVE_V1",symbol,rows.size(),bars.size(),rows,bars));
        String sha=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes));
        Path file=archiveDirectory.resolve(sha+".json"); Files.write(file,bytes); return file;
    }

    @Test
    void missingCorruptOrChangedArchiveCannotAuthorizeAnyDeletion() throws Exception {
        Instant old=now.minusSeconds(43200);
        mapper.saveFeatureHistory(symbol,old,old,"{\"immutable\":1}");
        var rows=mapper.selectHistory(symbol,AssetCardMapper.HistoryKind.FEATURE,old,old,now,10);
        var missing=new AssetCardMapper.ArchiveConfirmation(symbol,AssetCardMapper.HistoryKind.FEATURE,
                rows.stream().map(AssetCardMapper.TypedHistory::recordKey).toList(),old,now.minusSeconds(21600),now,"a".repeat(64),now);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.pruneArchivedHistory(missing,10)).isInstanceOf(IllegalArgumentException.class);
        Path file=archive(rows,java.util.List.of()); String sha=file.getFileName().toString().substring(0,64);
        var confirmed=new AssetCardMapper.ArchiveConfirmation(symbol,AssetCardMapper.HistoryKind.FEATURE,missing.recordKeys(),old,
                now.minusSeconds(21600),now,sha,now,file);
        Files.writeString(file,"{\"tampered\":true}");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> mapper.pruneArchivedHistory(confirmed,10)).isInstanceOf(IllegalArgumentException.class);
        assertThat(mapper.selectHistoryRecord(symbol,AssetCardMapper.HistoryKind.FEATURE,rows.get(0).recordKey())).contains(rows.get(0));
        assertThat(mapper.storageUsage().historyRows()).isEqualTo(1);
        assertThat(mapper.storageUsage().postgres()).isFalse();
    }

    @Test
    void exactArchiveDoesNotDeleteChangedDatabaseContentsOrLaterArrival() throws Exception {
        Instant old=now.minusSeconds(43200);
        mapper.saveFeatureHistory(symbol,old,old,"{\"immutable\":1}");
        var rows=mapper.selectHistory(symbol,AssetCardMapper.HistoryKind.FEATURE,old,old,now,10);
        Path file=archive(rows,java.util.List.of()); String sha=file.getFileName().toString().substring(0,64);
        // Disposable fixture deliberately violates production immutable-writer semantics to prove exact-content matching.
        jdbc.update("UPDATE tm_asset_card_feature_history SET payload_json=? WHERE symbol=?","{\"changed\":1}",symbol);
        assertThat(mapper.pruneArchivedHistory(new AssetCardMapper.ArchiveConfirmation(symbol,AssetCardMapper.HistoryKind.FEATURE,
                rows.stream().map(AssetCardMapper.TypedHistory::recordKey).toList(),old,now.minusSeconds(21600),now,sha,now,file),10)).isZero();
        assertThat(mapper.storageUsage().totalRows()).isEqualTo(1);
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
