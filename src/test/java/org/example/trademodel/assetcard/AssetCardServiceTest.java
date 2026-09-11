package org.example.trademodel.assetcard;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.Collections;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import org.springframework.test.util.ReflectionTestUtils;

@org.junit.jupiter.api.Tag("core-regression")
class AssetCardServiceTest {
    @Test
    void cardPublicationNeverEntersGlobalReplayCacheWithoutAnAuthenticatedCardStream() {
        var properties = new AssetCardProperties(); properties.setEnabled(true);
        properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
        var market = mock(AssetCardMarketDataService.class);
        var mapper = mock(org.example.trademodel.mapper.AssetCardMapper.class);
        var pool = mock(org.example.trademodel.service.watchlistsource.AssetPoolService.class);
        var events = new org.example.trademodel.v41.DashboardLiveEventService();
        Instant at = Instant.now();
        when(market.subscribedSymbols()).thenReturn(Set.of("BTCUSDT"));
        when(market.quote("BTCUSDT", at)).thenReturn(Optional.of(new AssetCardMarketDataService.SpotQuote(
                "BTCUSDT", BigDecimal.valueOf(101), BigDecimal.ONE, 101, at, at)));
        when(mapper.nextSnapshotVersion("BTCUSDT")).thenReturn(1L);
        when(mapper.saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), nullable(Instant.class))).thenReturn(1);
        try (var service = new AssetCardService(properties, market, mapper, pool, events,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules())) {
            service.flushPrices(at);
            assertThat(events.latestEvents()).as("card data must never enter the replay cache shared by all users").isEmpty();
            verifyNoInteractions(pool);
        }
    }

    @Test
    void scopedContinuousPriceUsesTradeIdentityAndRechecksMembershipOncePerUserBatch() {
        var properties = new AssetCardProperties(); properties.setEnabled(true);
        properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
        var market = mock(AssetCardMarketDataService.class);
        var mapper = mock(org.example.trademodel.mapper.AssetCardMapper.class);
        var pool = mock(org.example.trademodel.service.watchlistsource.AssetPoolService.class);
        var events = spy(new org.example.trademodel.v41.DashboardLiveEventService());
        Instant at = Instant.now().minusSeconds(5);
        var quote = new java.util.concurrent.atomic.AtomicReference<>(new AssetCardMarketDataService.SpotQuote(
                "BTCUSDT", BigDecimal.valueOf(101), BigDecimal.ONE, 101, at, at));
        when(market.subscribedSymbols()).thenReturn(Set.of("BTCUSDT"));
        when(market.quote(eq("BTCUSDT"), any())).thenAnswer(ignored -> Optional.of(quote.get()));
        when(pool.listForUser(7L)).thenReturn(poolMembers("BTCUSDT"));
        when(pool.listForUser(8L)).thenReturn(poolMembers("ETHUSDT"));
        try (var service = new AssetCardService(properties, market, mapper, pool, events,
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules())) {
            var signal = AssetCardSnapshot.Signal.unavailable("SHADOW", at.minusSeconds(60));
            runtimeMap(service, "snapshots").put("BTCUSDT", new AssetCardSnapshot("BTCUSDT", "Bitcoin", BigDecimal.valueOf(100), at.minusSeconds(1), signal,
                    AssetCardSnapshot.Risk.unknownFor(signal, AssetCardRiskService.RULE_VERSION, "TEST_FIXTURE"),
                    new AssetCardSnapshot.Health("HEALTHY", null, at.minusSeconds(1)), at.minusSeconds(60), 10,
                    AssetCardFeatureService.FEATURE_VERSION, null, null, null, 100L));
            String first = service.registerCardStream(7L), second = service.registerCardStream(7L);
            String other = service.registerCardStream(8L);
            for (int tick = 0; tick < 3; tick++) {
                quote.set(new AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.valueOf(101 + tick), BigDecimal.ONE,
                        101 + tick, at.plusSeconds(tick), at.plusSeconds(tick)));
                service.flushPrices(at.plusSeconds(tick));
            }
            var users = org.mockito.ArgumentCaptor.forClass(Long.class);
            var captured = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            verify(events, atLeastOnce()).publishToUser(users.capture(), captured.capture());
            var prices = new java.util.ArrayList<org.example.trademodel.v41.DashboardLiveEvent>();
            for (int i = 0; i < users.getAllValues().size(); i++) {
                var event = captured.getAllValues().get(i);
                assertThat(event.symbol()).isEqualTo(users.getAllValues().get(i) == 7L ? "BTCUSDT" : "ETHUSDT");
                if (event.eventType().equals("ASSET_CARD_PRICE")) prices.add(event);
            }
            assertThat(prices).extracting(org.example.trademodel.v41.DashboardLiveEvent::snapshotVersion).containsExactly(101L, 102L, 103L);
            assertThat(prices).allSatisfy(event -> assertThat(event.payload()).containsEntry("snapshotVersion", 10L)
                    .containsEntry("transportVersion", event.snapshotVersion()).containsEntry("priceTradeId", event.snapshotVersion()));
            assertThat(events.latestEvents()).isEmpty();
            verify(events, never()).publish(any());
            verify(pool, times(3)).listForUser(7L); verify(pool, times(3)).listForUser(8L);
            verify(mapper, never()).nextSnapshotVersion(anyString());
            verify(mapper, never()).saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), any());
            clearInvocations(events, pool);
            when(pool.listForUser(7L)).thenThrow(new IllegalStateException("TEST_PERMISSION_READ_FAILURE"));
            service.flushPublications(at.plusSeconds(3));
            verify(events, never()).publishToUser(eq(7L), any());
            doReturn(List.of()).when(pool).listForUser(7L);
            service.flushPublications(at.plusSeconds(3));
            verify(events, never()).publishToUser(eq(7L), any());
            doReturn(poolMembers("BTCUSDT")).when(pool).listForUser(7L);
            service.unregisterCardStream(first);
            quote.set(new AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.valueOf(104), BigDecimal.ONE, 104, at.plusSeconds(4), at.plusSeconds(4)));
            service.flushPrices(at.plusSeconds(4));
            verify(events, atLeastOnce()).publishToUser(eq(7L), any());
            service.unregisterCardStream(second); service.unregisterCardStream(other);
            clearInvocations(events, pool);
            service.flushPublications(at.plusSeconds(5));
            verifyNoInteractions(events, pool);
        }
    }

    @Test
    void ownerPreviewIsExplicitSessionOnlyShadowAndCannotExposeAStoredValidatedModel() {
        try (var fixture = new RuntimeFixture()) {
            fixture.properties.setOwnerPreviewUserIds(Set.of(7L));
            Instant at = Instant.now();
            var stored = publicSignalFixture(at, AssetCardFeatureService.FEATURE_VERSION);
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
            installBundle(fixture.service, "BTCUSDT", metadataOnlyBundle("TEST_FIXTURE_MODEL", "TEST_FIXTURE_CALIBRATION", Set.of("BTCUSDT")));
            assertThat(fixture.service.snapshot("BTCUSDT", "Bitcoin").signal().calibratedConfidence()).isNotNull();
            assertThat(fixture.service.usesCardSignalDisplay(7L, "BTCUSDT")).isTrue();
            assertThat(fixture.service.usesCardSignalDisplay(8L, "BTCUSDT")).isFalse();
            assertThat(fixture.service.usesCardSignalDisplay("BTCUSDT")).isFalse();
            var owner = fixture.service.snapshotForUser(7L, "BTCUSDT", "Bitcoin");
            assertThat(owner.spotPrice()).isEqualTo(stored.spotPrice());
            assertThat(owner.signal().status()).isEqualTo("SHADOW");
            assertThat(owner.signal().direction()).isNull(); assertThat(owner.signal().calibratedConfidence()).isNull();
            assertThat(owner.signal().pLong()).isNull(); assertThat(owner.signal().pShort()).isNull();
            assertThat(owner.risk().riskBasisSide()).isEqualTo(AssetCardSnapshot.SignalSide.NON_DIRECTIONAL);
            assertThat(owner.risk().overallLevel()).isNull();
            assertThat(fixture.lastSnapshot()).isEqualTo(stored);
            assertThatThrownBy(() -> fixture.service.snapshotForUser(8L, "BTCUSDT", "Bitcoin")).isInstanceOf(IllegalArgumentException.class);
            setReadFailure(fixture.service, "SIGNAL"); setReadFailure(fixture.service, "RISK");
            var failedOwner = fixture.service.snapshotForUser(7L, "BTCUSDT", "Bitcoin");
            assertThat(failedOwner.health().status()).isEqualTo("SIGNAL_AND_RISK_UNAVAILABLE");
            assertThat(failedOwner.signal().status()).isEqualTo("FAILED");
            assertThat(failedOwner.signal().direction()).isNull();
            assertThat(failedOwner.signal().calibratedConfidence()).isNull();
            assertThat(failedOwner.risk().riskBasisSide()).isEqualTo(AssetCardSnapshot.SignalSide.NON_DIRECTIONAL);
            assertThat(failedOwner.risk().items()).allMatch(item -> "UNKNOWN".equals(item.assessmentStatus()));
            fixture.properties.setEnabled(false);
            assertThat(fixture.service.usesCardSignalDisplay(7L, "BTCUSDT")).isFalse();
            assertThat(fixture.properties.getModelMode()).isEqualTo(AssetCardProperties.ModelMode.SHADOW);
        }
        var properties = new AssetCardProperties();
        assertThat(properties.getOwnerPreviewUserIds()).isEmpty();
        assertThatThrownBy(() -> properties.setOwnerPreviewUserIds(Set.of(0L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties.setOwnerPreviewUserIds(Set.of(-1L))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties.setOwnerPreviewUserIds(Set.of(7L, 8L))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readTimeFieldFailureHasCheckedIdentityAndNeverMasksModelRevocationOrSourceLoss() {
        for (String failedField : List.of("RISK", "SIGNAL", "BOTH")) {
            try (var fixture = new RuntimeFixture()) {
                fixture.properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
                Instant at = Instant.now();
                var stored = publicSignalFixture(at.minusSeconds(1), AssetCardFeatureService.FEATURE_VERSION);
                runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
                installBundle(fixture.service, "BTCUSDT", metadataOnlyBundle("TEST_FIXTURE_MODEL", "TEST_FIXTURE_CALIBRATION", Set.of("BTCUSDT")));
                when(fixture.pool.listForUser(7L)).thenReturn(poolMembers("BTCUSDT"));
                fixture.service.registerCardStream(7L);
                fixture.service.flushPublications(at);
                clearInvocations(fixture.events);
                if (!failedField.equals("SIGNAL")) setReadFailure(fixture.service, "RISK");
                if (!failedField.equals("RISK")) setReadFailure(fixture.service, "SIGNAL");
                setReadFailure(fixture.service, "PERSISTENCE");
                var current = fixture.service.snapshot("BTCUSDT", "Bitcoin");
                String expected = failedField.equals("RISK") ? "RISK_UNAVAILABLE"
                        : failedField.equals("SIGNAL") ? "SIGNAL_FAILED" : "SIGNAL_AND_RISK_UNAVAILABLE";
                assertThat(current.health().status()).isEqualTo(expected);
                assertThat(current.health().asOf()).isAfterOrEqualTo(at);
                assertThat(current.snapshotVersion()).isEqualTo(stored.snapshotVersion());
                assertThat(current.signal().direction()).isEqualTo(stored.signal().direction());
                assertThat(current.signal().signalAsOf()).isEqualTo(stored.signal().signalAsOf());
                assertThat(current.risk().matchesBasis(current.signal())).isTrue();
                assertThat(current.spotPrice()).isEqualTo(stored.spotPrice());
                assertThat(current.signal().calibratedConfidence()).isEqualTo(failedField.equals("RISK") ? 81 : null);
                if (failedField.equals("SIGNAL")) assertThat(current.risk()).isEqualTo(stored.risk());
                else assertThat(current.risk().items()).hasSize(8).allMatch(item -> "UNKNOWN".equals(item.assessmentStatus()) && item.level() == null);
                fixture.service.flushPublications(at.plusMillis(100));
                var published = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
                verify(fixture.events, atLeastOnce()).publishToUser(eq(7L), published.capture());
                assertThat(published.getAllValues()).filteredOn(e -> "ASSET_CARD_HEALTH".equals(e.eventType())).singleElement().satisfies(e -> {
                    assertThat(e.snapshotVersion()).isEqualTo(stored.snapshotVersion());
                    assertThat(((AssetCardSnapshot.Health)e.payload().get("health")).status()).isEqualTo(expected);
                    assertThat(e.payload()).containsEntry("thresholdVersion", stored.thresholdVersion())
                            .containsEntry("riskBasisSignalAsOf", stored.signal().signalAsOf()).containsEntry("riskBasisDirection", stored.signal().direction());
                });
                clearInvocations(fixture.events);
                fixture.service.flushPublications(at.plusMillis(200));
                verifyNoInteractions(fixture.events);
                installBundle(fixture.service, "BTCUSDT", AssetCardModelBundle.unavailable("TEST_REVOKED"));
                var revoked = fixture.service.snapshot("BTCUSDT", "Bitcoin");
                assertThat(revoked.health().status()).isEqualTo("MODEL_UNAVAILABLE");
                assertThat(revoked.signal().status()).isEqualTo("UNVALIDATED");
                assertThat(revoked.signal().calibratedConfidence()).isNull();
                fixture.properties.setPriceTtl(java.time.Duration.ofNanos(1));
                var lost = fixture.service.snapshot("BTCUSDT", "Bitcoin");
                assertThat(lost.health().status()).isEqualTo("SOURCE_UNAVAILABLE");
                assertThat(lost.spotPrice()).isNull();
                assertThat(lost.risk().overallLevel()).isEqualTo("HIGH");
                assertThat(fixture.lastSnapshot()).isSameAs(stored);
                verify(fixture.mapper, never()).saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), any());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void setReadFailure(AssetCardService service, String field) {
        var failures = (Map<?, Map<String, String>>) ReflectionTestUtils.getField(service, "fieldFailures");
        failures.entrySet().stream().filter(entry -> field.equals(entry.getKey().toString())).findFirst().orElseThrow()
                .getValue().put("BTCUSDT", "TEST_" + field + "_FAILURE");
    }

    @Test
    void newOwnerStreamGetsCurrentSafeRiskAndStaticMarketModelRevocationDoesNotReplayOldPercentages() {
        try (var fixture = new RuntimeFixture()) {
            fixture.properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
            var at = Instant.now();
            var stored = publicSignalFixture(at, AssetCardFeatureService.FEATURE_VERSION);
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
            installBundle(fixture.service, "BTCUSDT", metadataOnlyBundle("TEST_FIXTURE_MODEL", "TEST_FIXTURE_CALIBRATION", Set.of("BTCUSDT")));
            when(fixture.pool.listForUser(7L)).thenReturn(poolMembers("BTCUSDT"));
            String first = fixture.service.registerCardStream(7L);
            fixture.service.flushPublications(at);
            clearInvocations(fixture.events);
            installBundle(fixture.service, "BTCUSDT", AssetCardModelBundle.unavailable("TEST_REVOKED"));
            fixture.service.flushPublications(at.plusMillis(500));
            var captured = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            verify(fixture.events, atLeastOnce()).publishToUser(eq(7L), captured.capture());
            assertThat(captured.getAllValues()).filteredOn(event -> event.payload().containsKey("signal"))
                    .allSatisfy(event -> assertThat(((AssetCardSnapshot.Signal) event.payload().get("signal")).calibratedConfidence()).isNull());
            fixture.service.unregisterCardStream(first); fixture.service.registerCardStream(7L);
            clearInvocations(fixture.events);
            fixture.service.flushPublications(at.plusSeconds(1));
            verify(fixture.events, atLeastOnce()).publishToUser(eq(7L), captured.capture());
            assertThat(captured.getAllValues()).filteredOn(event -> event.payload().containsKey("signal"))
                    .allSatisfy(event -> assertThat(((AssetCardSnapshot.Signal) event.payload().get("signal")).pLong()).isNull());
            verify(fixture.events, never()).publish(any());
        }
    }

    private static List<org.example.trademodel.dto.assetpool.AssetPoolAssetDTO> poolMembers(String... symbols) {
        return java.util.Arrays.stream(symbols).map(symbol -> new org.example.trademodel.dto.assetpool.AssetPoolAssetDTO(
                1L, symbol, symbol, "SPOT", "USDT", true, 1, "USER")).toList();
    }

    private static void subscribeFixture(RuntimeFixture fixture) {
        when(fixture.pool.listForUser(7L)).thenReturn(poolMembers("BTCUSDT", "ETHUSDT"));
        fixture.service.registerCardStream(7L);
    }

    @Test
    void shadowWithoutAnyModelAccumulatesRealPipelineRecordsBeforeTrainingCanExist() throws Exception {
        // Synthetic market fixtures and a random disposable database prove wiring only, never real sample readiness.
        try (var fixture = new LabelDatabaseFixture(true, false)) {
            var mapper = spy(fixture.mapper);
            doReturn(new org.example.trademodel.mapper.AssetCardMapper.WriterReadiness(true, true, "ISOLATED_FIXTURE_ONLY"))
                    .when(mapper).inspectWriterPermissions();
            assertNoModelShadowPipeline(fixture, mapper, new AssetCardProperties());
        }
    }

    @Test
    void realDedicatedPostgresWriterPersistsShadowFeaturesAndMaturesBothSidesWithoutModel(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path temporary) throws Exception {
        // Auth, role checks, pool and mapper routing are real; only market inputs are isolated synthetic fixtures.
        try (var writer = AssetCardDataSourceConfigurationTest.WriterFixture.open(temporary)) {
            assertThat(writer.mapper().inspectWriterPermissions().writable()).isTrue();
            assertThat(writer.mapper().inspectWriterPermissions().cleanupAllowed()).isTrue();
            var defaultJdbc = writer.context().getBean(org.springframework.jdbc.core.JdbcTemplate.class);
            try (var connection = defaultJdbc.getDataSource().getConnection()) {
                assertThat(connection.isReadOnly()).isTrue();
                try (var result = connection.createStatement().executeQuery("SELECT current_user")) {
                    assertThat(result.next()).isTrue();
                    assertThat(result.getString(1)).isNotEqualTo("rine_asset_card_writer");
                }
            }
            try (var fixture = new LabelDatabaseFixture(writer.mapper())) {
                assertNoModelShadowPipeline(fixture, writer.mapper(), writer.properties());
                assertThat(writer.admin().queryForObject(
                        "SELECT count(*) FROM public.tm_asset_card_feature_history WHERE record_kind='INFERENCE'", Integer.class))
                        .isEqualTo(1);
                assertThat(writer.admin().queryForObject(
                        "SELECT count(*) FROM public.tm_asset_card_feature_history WHERE record_kind='LABEL'", Integer.class))
                        .isEqualTo(2);
                assertThat(writer.mapper().inspectWriterPermissions().writable()).isTrue();
                // Restart uses the same dedicated route; replay cannot produce a different or duplicate side label.
                var before = writer.admin().queryForList(
                        "SELECT record_key,payload_json FROM public.tm_asset_card_feature_history WHERE record_kind='LABEL' ORDER BY record_key");
                var market = mock(AssetCardMarketDataService.class);
                var pool = mock(org.example.trademodel.service.watchlistsource.AssetPoolService.class);
                var events = mock(org.example.trademodel.v41.DashboardLiveEventService.class);
                try (var restarted = new AssetCardService(writer.properties(), market, writer.mapper(), pool, events, fixture.json)) {
                    restarted.start();
                    ((java.util.concurrent.ScheduledExecutorService) ReflectionTestUtils.getField(restarted, "labelWorker"))
                            .submit(() -> {}).get(10, java.util.concurrent.TimeUnit.SECONDS);
                    restarted.matureLabels(fixture.cutoff.plusSeconds(60));
                    assertThat(writer.admin().queryForList(
                            "SELECT record_key,payload_json FROM public.tm_asset_card_feature_history WHERE record_kind='LABEL' ORDER BY record_key"))
                            .isEqualTo(before);
                    verifyNoInteractions(events);
                }
            }
        }
    }

    private static void assertNoModelShadowPipeline(LabelDatabaseFixture fixture,
            org.example.trademodel.mapper.AssetCardMapper mapper, AssetCardProperties properties) throws Exception {
            assertThat(fixture.mapper.selectInferenceSymbols()).isEmpty();
            properties.setEnabled(true); properties.setWriterEnabled(true);
            properties.setBarRetention(java.time.Duration.ofDays(1));
            properties.setFeatureRetention(java.time.Duration.ofDays(1));
            properties.setTradeRetention(java.time.Duration.ofDays(1));
            properties.setLabelRetention(java.time.Duration.ofDays(1));
            assertThat(properties.getModelMode()).isEqualTo(AssetCardProperties.ModelMode.SHADOW);
            assertThat(properties.getModelBundles()).isEmpty();
            assertThat(properties.getModelBundlePath()).isNull();
            assertThat(properties.isExternalCallsEnabled()).isFalse();
            var market = mock(AssetCardMarketDataService.class);
            var pool = mock(org.example.trademodel.service.watchlistsource.AssetPoolService.class);
            var events = mock(org.example.trademodel.v41.DashboardLiveEventService.class);
            Instant close = fixture.start.minusMillis(124);
            when(market.bars(eq("BTCUSDT"), anyString(), any(), eq(24)))
                    .thenAnswer(call -> closedBars(call.getArgument(1), close));
            when(market.quote(eq("BTCUSDT"), any())).thenReturn(Optional.of(new AssetCardMarketDataService.SpotQuote(
                    "BTCUSDT", BigDecimal.valueOf(100), BigDecimal.ONE, 8001, fixture.start, fixture.start)));
            when(market.book(eq("BTCUSDT"), any())).thenReturn(Optional.of(new AssetCardMarketDataService.SpotBook(
                    "BTCUSDT", List.of(new AssetCardMarketDataService.Level(new BigDecimal("99.99"), BigDecimal.TEN),
                    new AssetCardMarketDataService.Level(new BigDecimal("99"), BigDecimal.TEN)),
                    List.of(new AssetCardMarketDataService.Level(new BigDecimal("100.01"), BigDecimal.TEN),
                    new AssetCardMarketDataService.Level(new BigDecimal("101"), BigDecimal.TEN)), 1, fixture.start, fixture.start,
                    "EXCHANGE_EVENT", BigDecimal.valueOf(99), BigDecimal.valueOf(101))));
            try (var service = new AssetCardService(properties, market, mapper, pool, events, fixture.json)) {
                service.start();
                // Drain the startup's empty scan before introducing a controlled historical observation clock.
                ((java.util.concurrent.ScheduledExecutorService) ReflectionTestUtils.getField(service, "labelWorker"))
                        .submit(() -> {}).get(5, java.util.concurrent.TimeUnit.SECONDS);
                try (var lease = registry(service).acquire("BTCUSDT")) {
                    assertThat(lease.bundle().validated()).isFalse();
                    assertThat(lease.bundle().reason()).isEqualTo("MODEL_NOT_CONFIGURED");
                }
                service.inferClosedBar("BTCUSDT", close, fixture.start);
                var saved = mapper.selectInference("BTCUSDT", close, fixture.cutoff).orElseThrow();
                var audit = fixture.json.readTree(saved.payloadJson());
                Instant recordedSignalAt = fixture.json.treeToValue(audit.path("rawFrame").path("signalAsOf"), Instant.class);
                // Production includes real lock-wait time in signalAsOf; use its immutable identity, not the caller clock.
                java.util.function.Supplier<List<org.example.trademodel.mapper.AssetCardMapper.TypedHistory>> labels = () ->
                        mapper.selectHistory("BTCUSDT", org.example.trademodel.mapper.AssetCardMapper.HistoryKind.LABEL,
                                fixture.start, close.plusSeconds(15), fixture.cutoff, 10);
                assertThat(audit.path("outcome").asText()).isEqualTo("COMPLETED");
                assertThat(audit.path("frame").path("ready").asBoolean()).isTrue();
                assertThat(audit.path("frame").path("vector").size()).isEqualTo(AssetCardFeatureService.FEATURE_NAMES.size());
                assertThat(audit.path("state").path("auditPrediction").path("available").asBoolean()).isFalse();
                for (String name : List.of("rawLong", "rawShort", "pLong", "pShort"))
                    assertThat(audit.path("state").path("auditPrediction").path(name).isNull()).isTrue();
                assertThat(service.usesCardSignalDisplay("BTCUSDT")).isFalse();
                service.matureLabels(fixture.start.plusSeconds(3600));
                assertThat(labels.get()).isEmpty();
                assertThat(new AssetCardService.LabelPipeline(mapper, fixture.json).materialize(saved, fixture.cutoff).status())
                        .isEqualTo("MATURED");
                service.matureLabels(fixture.cutoff);
                assertThat(ReflectionTestUtils.getField(service, "labelStatuses")).isEqualTo(Map.of("BTCUSDT", "MATURED"));
                var original = labels.get();
                assertThat(original).hasSize(2);
                assertThat(original).allSatisfy(row -> assertThat(row.payloadJson()).contains("TIMEOUT"));
                Set<String> sides = new java.util.HashSet<>();
                for (var row : original) {
                    var label = fixture.json.readTree(row.payloadJson()).path("label");
                    sides.add(label.path("side").asText());
                    assertThat(fixture.json.treeToValue(label.path("signalAsOf"), Instant.class)).isEqualTo(recordedSignalAt);
                    assertThat(label.path("signalTradeId").asLong()).isEqualTo(8001L);
                }
                assertThat(sides).containsExactlyInAnyOrder("LONG", "SHORT");
                service.inferClosedBar("BTCUSDT", close, fixture.start.plusSeconds(1));
                service.matureLabels(fixture.cutoff.plusSeconds(60));
                assertThat(labels.get()).isEqualTo(original);
                assertThat(mapper.selectHistory("BTCUSDT", org.example.trademodel.mapper.AssetCardMapper.HistoryKind.INFERENCE,
                        close, close, fixture.cutoff, 10)).hasSize(1);
                assertThat(properties.getModelMode()).isEqualTo(AssetCardProperties.ModelMode.SHADOW);
                verifyNoInteractions(events);
            }
    }

    @Test
    void applicationStartActuallySchedulesMaturityWithoutAnyDashboardRequest() throws Exception {
        try (var fixture=new LabelDatabaseFixture()) {
            var properties=new AssetCardProperties(); properties.setEnabled(true); properties.setWriterEnabled(true);
            properties.setBarRetention(java.time.Duration.ofDays(1)); properties.setFeatureRetention(java.time.Duration.ofDays(1));
            properties.setTradeRetention(java.time.Duration.ofDays(1)); properties.setLabelRetention(java.time.Duration.ofDays(1));
            var cardMapper=spy(fixture.mapper);
            doReturn(new org.example.trademodel.mapper.AssetCardMapper.WriterReadiness(true,true,"ISOLATED_FIXTURE_ONLY"))
                    .when(cardMapper).inspectWriterPermissions();
            var market=mock(AssetCardMarketDataService.class);
            var pool=mock(org.example.trademodel.service.watchlistsource.AssetPoolService.class);
            var events=mock(org.example.trademodel.v41.DashboardLiveEventService.class);
            try(var service=new AssetCardService(properties,market,cardMapper,pool,events,fixture.json)) {
                service.start(); service.start();
                verify(cardMapper,timeout(5000).times(2)).saveLabel(eq("BTCUSDT"),eq(fixture.start),anyString(),anyString(),eq(8001L),
                        eq(AssetCardFeatureService.FEATURE_VERSION),eq(AssetCardService.LabelPipeline.LABEL_DEFINITION),anyString(),any(),anyString());
                assertThat(fixture.labels()).hasSize(2);
                verify(cardMapper,never()).saveInference(anyString(),any(),any(),anyString());
                verifyNoInteractions(events);
                verify(market,never()).quote(anyString(),any());
            }
        }
    }
    @Test
    void productionMaturityCallerSurvivesRestartAndTwoWritersWithoutHomeOrExternalCalls(@org.junit.jupiter.api.io.TempDir java.nio.file.Path temp) throws Exception {
        try (var fixture = new LabelDatabaseFixture()) {
            var properties = new AssetCardProperties(); properties.setEnabled(true);
            var market = mock(AssetCardMarketDataService.class);
            var pool = mock(org.example.trademodel.service.watchlistsource.AssetPoolService.class);
            var events = mock(org.example.trademodel.v41.DashboardLiveEventService.class);
            for (int restart = 0; restart < 2; restart++) try (var service = new AssetCardService(properties,market,fixture.mapper,pool,events,fixture.json)) {
                ReflectionTestUtils.setField(service,"started",true);
                ReflectionTestUtils.setField(service,"writerReady",true); // Isolated in-memory fixture, never deployment readiness.
                service.matureLabels(fixture.start.plusSeconds(3600));
                assertThat(fixture.labels()).hasSize(restart == 0 ? 0 : 2);
                service.matureLabels(fixture.cutoff);
                service.matureLabels(fixture.cutoff.plusSeconds(60));
                service.matureLabels(fixture.cutoff.plusSeconds(120));
                assertThat(fixture.labels()).hasSize(2);
            }
            var original = fixture.labels();
            var callers = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                var tasks = List.<java.util.concurrent.Callable<Void>>of(() -> { fixture.saveMatured(); return null; }, () -> { fixture.saveMatured(); return null; });
                for (var result : callers.invokeAll(tasks)) result.get();
            } finally { callers.shutdownNow(); }
            assertThat(fixture.labels()).isEqualTo(original);
            assertThat(fixture.labels()).allSatisfy(row -> {
                assertThat(row.payloadJson()).contains("LIVE_OBSERVED_MATURE_CARD_LABEL", "TIMEOUT").doesNotContain("owner", "password");
                assertThat(row.availableAt()).isAfterOrEqualTo(fixture.start.plusSeconds(14400)).isBefore(fixture.cutoff);
            });
            verifyNoInteractions(market,pool,events);
        }
    }

    @Test
    void immutableCardDatabaseExportIsRepeatableAndPythonRejectsUnqualifiedTrainingPopulation(@org.junit.jupiter.api.io.TempDir java.nio.file.Path temp) throws Exception {
        try (var fixture = new LabelDatabaseFixture()) {
            var pipeline = new AssetCardService.LabelPipeline(fixture.mapper,fixture.json);
            var before = pipeline.export("BTCUSDT",fixture.start,fixture.start.plusSeconds(1),fixture.cutoff,temp);
            assertThat(before.recordCount()).isZero();
            assertThat(before.exclusions()).containsEntry("MATURE_LABEL_NOT_PERSISTED_OR_IDENTITY_MISMATCH",1L);
            assertThat(fixture.labels()).isEmpty();
            fixture.saveMatured();
            var labels = fixture.labels();
            var a = pipeline.export("BTCUSDT",fixture.start,fixture.start.plusSeconds(1),fixture.cutoff,temp);
            var b = pipeline.export("BTCUSDT",fixture.start,fixture.start.plusSeconds(1),fixture.cutoff,temp);
            assertThat(a.recordCount()).as(a.exclusions().toString()).isZero();
            assertThat(a.exclusions()).containsEntry("COINGLASS_EVIDENCE_UNQUALIFIED",1L);
            assertThat(a.recordsSha256()).isEqualTo(b.recordsSha256());
            assertThat(a.manifestSha256()).isEqualTo(b.manifestSha256());
            assertThat(a.manifestSha256()).isNotEqualTo(before.manifestSha256()); // Distinct truthful exclusion evidence, not a fabricated eligible row.
            assertThat(a.productionModelReady()).isFalse(); assertThat(a.modelMode()).isEqualTo("SHADOW");
            assertThat(fixture.labels()).isEqualTo(labels);
            var manifest=fixture.json.readTree(java.nio.file.Files.readString(a.manifest()));
            assertThat(manifest.path("exclusions").path("COINGLASS_EVIDENCE_UNQUALIFIED").asInt()).isEqualTo(1);
            assertThat(java.nio.file.Files.readString(a.manifest().getParent().resolve("records.jsonl"))).isEmpty();
            // Unqualified source evidence remains fully available for audit/maturity, but cannot enter training export.
            var record=fixture.json.copy().disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .valueToTree(pipeline.materialize(fixture.inference,fixture.cutoff).record());
            assertThat(record.path("rawFrame").path("signalAsOf").asText()).isEqualTo(fixture.start.toString());
            assertThat(record.path("labelResults").path("LONG").path("y").intValue()).isZero();
            assertThat(record.path("horizonTrade").path("observationId").asText()).isEqualTo("9001");
            // The only optional interpreter dependency is a local pipeline test; this fixture never contributes real model evidence.
            String python=System.getProperty("assetCard.testPython");
            if (python!=null) {
                var log=temp.resolve("python-export-verification.log");
                var process=new ProcessBuilder(python,"scripts/asset_card_model.py","verify-export",a.manifest().toString())
                        .redirectErrorStream(true).redirectOutput(log.toFile());
                process.environment().put("PYTHONDONTWRITEBYTECODE","1");
                var running=process.start();
                assertThat(running.waitFor(30,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                assertThat(running.exitValue()).as(java.nio.file.Files.readString(log)).isNotZero();
                assertThat(java.nio.file.Files.readString(log)).contains("Explicit Spot and CoinGlass source identities required");
            }
        }
    }

    @Test
    void retentionPreservesPendingDependenciesAndUsesVerifiedArchiveForMaturedAuditAndExport(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path archive) throws Exception {
        try(var fixture=new LabelDatabaseFixture()) {
            Instant now=fixture.cutoff.plusSeconds(21600);
            var retention=new AssetCardService.RetentionLifecycle(fixture.mapper,fixture.json,archive,1);
            var fiveHours=java.time.Duration.ofHours(5);
            long before=fixture.mapper.storageUsage().totalRows();
            var pending=retention.run("BTCUSDT",now,fiveHours,fiveHours,fiveHours,fiveHours,500);
            assertThat(pending.status()).isEqualTo("PENDING_LABEL_DEPENDENCIES_PRESERVED");
            assertThat(pending.deleted()).isZero(); assertThat(fixture.mapper.storageUsage().totalRows()).isEqualTo(before);
            fixture.saveMatured();
            var original=new AssetCardService.LabelPipeline(fixture.mapper,fixture.json).materialize(fixture.inference,fixture.cutoff);
            var archived=retention.run("BTCUSDT",now,fiveHours,fiveHours,fiveHours,fiveHours,500);
            assertThat(archived.status()).isEqualTo("ARCHIVED_VERIFIED_AND_PRUNED");
            assertThat(archived.deleted()).isPositive().isLessThanOrEqualTo(500);
            assertThat(fixture.mapper.selectInference("BTCUSDT",fixture.inference.signalAsOf(),now)).isEmpty();
            assertThat(fixture.labels()).isEmpty();
            var restarted=new AssetCardService.LabelPipeline(fixture.mapper,fixture.json,archive);
            var restored=restarted.materialize(fixture.inference,fixture.cutoff);
            assertThat(restored.status()).isEqualTo("MATURED");
            assertThat(restored.evidenceSha256()).isEqualTo(original.evidenceSha256());
            assertThat(fixture.json.<com.fasterxml.jackson.databind.JsonNode>valueToTree(restored.record()))
                    .isEqualTo(fixture.json.<com.fasterxml.jackson.databind.JsonNode>valueToTree(original.record()));
            var export=restarted.export("BTCUSDT",fixture.start,fixture.start.plusSeconds(1),fixture.cutoff,archive);
            assertThat(export.exclusions()).containsEntry("COINGLASS_EVIDENCE_UNQUALIFIED",1L)
                    .doesNotContainKey("MATURE_LABEL_NOT_PERSISTED_OR_IDENTITY_MISMATCH");
            assertThat(export.recordCount()).isZero(); // The original unproven CoinGlass evidence stays unqualified after archival.
            var again=new AssetCardService.RetentionLifecycle(fixture.mapper,fixture.json,archive,1)
                    .run("BTCUSDT",now,fiveHours,fiveHours,fiveHours,fiveHours,500);
            assertThat(again.deleted()).isZero();
            assertThat(again.status()).isEqualTo("NOTHING_EXPIRED");
        }
    }

    @Test
    void archiveFailureLowSpaceAndActiveWindowNeverDeletePendingOrSnapshotRows(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path archive) throws Exception {
        try(var fixture=new LabelDatabaseFixture()) {
            fixture.saveMatured();
            long version=fixture.mapper.nextSnapshotVersion("BTCUSDT");
            fixture.mapper.saveSnapshot("BTCUSDT",0,version,"{\"safe\":true}",fixture.start);
            Instant now=fixture.cutoff.plusSeconds(21600); var duration=java.time.Duration.ofHours(5);
            long before=fixture.mapper.storageUsage().totalRows();
            var lowSpace=new AssetCardService.RetentionLifecycle(fixture.mapper,fixture.json,archive,Long.MAX_VALUE)
                    .run("BTCUSDT",now,duration,duration,duration,duration,100);
            assertThat(lowSpace.status()).isEqualTo("RETENTION_LOW_SPACE_DATA_PRESERVED");
            assertThat(fixture.mapper.storageUsage().totalRows()).isEqualTo(before);
            var missing=new AssetCardService.RetentionLifecycle(fixture.mapper,fixture.json,archive.resolve("missing"),1)
                    .run("BTCUSDT",now,duration,duration,duration,duration,100);
            assertThat(missing.status()).isEqualTo("RETENTION_ARCHIVE_DIRECTORY_UNAVAILABLE_DATA_PRESERVED");
            assertThat(fixture.mapper.storageUsage().totalRows()).isEqualTo(before);
            var activeWindow=new AssetCardService.RetentionLifecycle(fixture.mapper,fixture.json,archive,1)
                    .run("BTCUSDT",now,duration,duration,duration,duration,100,fixture.inference.signalAsOf().minusSeconds(300));
            assertThat(activeWindow.deleted()).isZero();
            var good=new AssetCardService.RetentionLifecycle(fixture.mapper,fixture.json,archive,1)
                    .run("BTCUSDT",now,duration,duration,duration,duration,100);
            assertThat(good.deleted()).isPositive().isLessThanOrEqualTo(100);
            assertThat(fixture.mapper.selectSnapshotJson("BTCUSDT")).isEqualTo("{\"safe\":true}");
            java.nio.file.Path stored;
            try(var files=java.nio.file.Files.list(archive.resolve("BTCUSDT"))) { stored=files.findFirst().orElseThrow(); }
            java.nio.file.Files.writeString(stored,"{\"corrupt\":true}");
            long after=fixture.mapper.storageUsage().totalRows();
            var corrupt=new AssetCardService.RetentionLifecycle(fixture.mapper,fixture.json,archive,1)
                    .run("BTCUSDT",now,duration,duration,duration,duration,100);
            assertThat(corrupt.status()).isEqualTo("RETENTION_FAILED_DATA_PRESERVED");
            assertThat(fixture.mapper.storageUsage().totalRows()).isEqualTo(after);
        }
    }

    @Test
    void failedBatchRollsBackDeletesAndRestartReusesVerifiedContentAddressedArchive(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path archive) throws Exception {
        try(var fixture=new LabelDatabaseFixture()) {
            fixture.saveMatured();
            long before=fixture.mapper.storageUsage().totalRows();
            var failingMapper=new org.example.trademodel.mapper.AssetCardMapper(fixture.jdbc) {
                @Override public int pruneArchivedBars(BarArchiveConfirmation confirmation,int limit) {
                    throw new IllegalStateException("ISOLATED_FAILURE_AFTER_HISTORY_DELETE");
                }
            };
            Instant now=fixture.cutoff.plusSeconds(21600); var duration=java.time.Duration.ofHours(5);
            var failed=new AssetCardService.RetentionLifecycle(failingMapper,fixture.json,archive,1)
                    .run("BTCUSDT",now,duration,duration,duration,duration,500);
            assertThat(failed.status()).isEqualTo("RETENTION_FAILED_DATA_PRESERVED");
            assertThat(fixture.mapper.storageUsage().totalRows()).isEqualTo(before);
            java.nio.file.Path file;
            try(var files=java.nio.file.Files.list(archive.resolve("BTCUSDT"))) { file=files.findFirst().orElseThrow(); }
            byte[] original=java.nio.file.Files.readAllBytes(file);
            var resumed=new AssetCardService.RetentionLifecycle(fixture.mapper,fixture.json,archive,1)
                    .run("BTCUSDT",now,duration,duration,duration,duration,500);
            assertThat(resumed.status()).isEqualTo("ARCHIVED_VERIFIED_AND_PRUNED");
            assertThat(resumed.deleted()).isEqualTo(before);
            assertThat(java.nio.file.Files.readAllBytes(file)).isEqualTo(original);
            try(var files=java.nio.file.Files.list(archive.resolve("BTCUSDT"))) { assertThat(files.count()).isEqualTo(1); }
            var readAfterRestart=new AssetCardService.LabelPipeline(fixture.mapper,fixture.json,archive)
                    .materialize(fixture.inference,fixture.cutoff);
            assertThat(readAfterRestart.status()).isEqualTo("MATURED");
        }
    }

    @Test
    void expiredWindowKeepsUnfinishedLabelsPendingAcrossServiceRestartWithoutLiveCalls(
            @org.junit.jupiter.api.io.TempDir java.nio.file.Path archive) throws Exception {
        try(var fixture=new LabelDatabaseFixture(false)) {
            var properties=new AssetCardProperties(); properties.setEnabled(true); properties.setExternalCallsEnabled(true);
            properties.setWriterEnabled(true); properties.setRetentionEnabled(true); properties.setArchiveDirectory(archive);
            properties.setArchiveMinimumFreeBytes(1);
            properties.setBarRetention(java.time.Duration.ofHours(5)); properties.setFeatureRetention(java.time.Duration.ofHours(5));
            properties.setTradeRetention(java.time.Duration.ofHours(5)); properties.setLabelRetention(java.time.Duration.ofHours(5));
            var window=properties.getCollectionWindow(); window.setId("expired-card-window-fixture");
            window.setStartsAt(fixture.start); window.setEndsAt(fixture.start.plusSeconds(8*3600L));
            window.setStateDirectory(archive); window.setSymbols(java.util.Set.of("BTCUSDT"));
            window.setSharedIpWeightAllowancePerMinute(500); window.setSharedIpWeightLimitPerMinute(6000);
            window.setSharedIpHeadroomConfirmedAt(fixture.start.minusSeconds(60));
            assertThat(window.valid()).isTrue();
            var market=mock(AssetCardMarketDataService.class);
            var events=mock(org.example.trademodel.v41.DashboardLiveEventService.class);
            var pool=mock(org.example.trademodel.service.watchlistsource.AssetPoolService.class);
            long originalRows=fixture.mapper.storageUsage().totalRows();
            for(int restart=0;restart<2;restart++) {
                try(var service=new AssetCardService(properties,market,fixture.mapper,pool,events,fixture.json)) {
                    org.springframework.test.util.ReflectionTestUtils.setField(service,"started",true);
                    org.springframework.test.util.ReflectionTestUtils.setField(service,"writerReady",true);
                    Instant afterEightHourWindow=fixture.start.plusSeconds(8*3600L+restart*3600L);
                    service.matureLabels(afterEightHourWindow);
                    assertThat(fixture.labels()).isEmpty();
                    @SuppressWarnings("unchecked") var statuses=(java.util.Map<String,String>)
                            org.springframework.test.util.ReflectionTestUtils.getField(service,"labelStatuses");
                    assertThat(statuses).containsEntry("BTCUSDT","MISSING_POINT_IN_TIME_HORIZON_TRADE");
                    service.retainHistory(afterEightHourWindow);
                    assertThat(fixture.mapper.storageUsage().totalRows()).isEqualTo(originalRows);
                    assertThat(fixture.mapper.selectInference("BTCUSDT",fixture.inference.signalAsOf(),afterEightHourWindow))
                            .contains(fixture.inference);
                }
            }
            verifyNoInteractions(market,events,pool);
        }
    }

    @Test
    void missingOrLateHorizonEvidenceIsPendingWithoutBackfillingFeatureInputs() throws Exception {
        try (var fixture = new LabelDatabaseFixture(false)) {
            var pipeline = new AssetCardService.LabelPipeline(fixture.mapper,fixture.json);
            var original = fixture.inference.payloadJson();
            assertThat(pipeline.materialize(fixture.inference,fixture.cutoff).status()).isEqualTo("MISSING_POINT_IN_TIME_HORIZON_TRADE");
            fixture.horizonTrade(true);
            // The column rounds this +1ns late value to the boundary; original JSON must still reject the future observation.
            assertThat(pipeline.materialize(fixture.inference,fixture.cutoff).status()).isEqualTo("INVALID_POINT_IN_TIME_HORIZON_TRADE");
            assertThat(fixture.labels()).isEmpty();
            assertThat(fixture.mapper.selectHistoryRecord("BTCUSDT", org.example.trademodel.mapper.AssetCardMapper.HistoryKind.INFERENCE,fixture.inference.recordKey()).orElseThrow().payloadJson()).isEqualTo(original);
        }
    }

    /** Synthetic fixtures exercise the complete pipeline in a random disposable DB, not production samples or model readiness. */
    private static final class LabelDatabaseFixture implements AutoCloseable {
        final Instant start=Instant.parse("2026-09-10T12:00:00.123Z"), cutoff=start.plusSeconds(15000);
        final com.fasterxml.jackson.databind.ObjectMapper json=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()
                .enable(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        final org.springframework.jdbc.core.JdbcTemplate jdbc;
        final org.example.trademodel.mapper.AssetCardMapper mapper;
        final org.example.trademodel.mapper.AssetCardMapper.TypedHistory inference;
        final boolean ownedH2;
        LabelDatabaseFixture() throws Exception { this(true); }
        LabelDatabaseFixture(boolean horizon) throws Exception { this(horizon, true); }
        LabelDatabaseFixture(boolean horizon, boolean seedInference) throws Exception {
            var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource("jdbc:h2:mem:label_"+java.util.UUID.randomUUID()+";DB_CLOSE_DELAY=-1","sa","");
            try(var connection=ds.getConnection()) { assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:label_"); }
            jdbc=new org.springframework.jdbc.core.JdbcTemplate(ds);
            ownedH2=true;
            for(String sql:java.nio.file.Files.readString(java.nio.file.Path.of("src/main/resources/db/migration/V24__asset_card_live_signal.sql")).split(";")) if(!sql.isBlank()) jdbc.execute(sql);
            mapper=new org.example.trademodel.mapper.AssetCardMapper(jdbc);
            inference=seedPipeline(horizon,seedInference);
        }
        LabelDatabaseFixture(org.example.trademodel.mapper.AssetCardMapper dedicatedMapper) throws Exception {
            mapper=dedicatedMapper; jdbc=null; ownedH2=false;
            inference=seedPipeline(true,false);
        }
        private org.example.trademodel.mapper.AssetCardMapper.TypedHistory seedPipeline(boolean horizon, boolean seedInference) throws Exception {
            Instant close=start.minusMillis(123).minusMillis(1);
            var bars=new java.util.LinkedHashMap<String,List<AssetCardFeatureService.Bar>>();
            for(String interval:AssetCardFeatureService.INTERVALS) bars.put(interval,closedBars(interval,close).stream().map(b -> new AssetCardFeatureService.Bar(
                    b.openTime(),b.closeTime(),b.availableAt(),b.open().doubleValue(),b.high().doubleValue(),b.low().doubleValue(),b.close().doubleValue(),b.volume().doubleValue(),b.takerBuyBaseVolume().doubleValue(),b.tradeCount())).toList());
            var evidence=new java.util.LinkedHashMap<String,AssetCardFeatureService.Observation>();
            for(String key:List.of("spotPrice","spreadBps","depth10Bps","depth25Bps")) evidence.put(key,new AssetCardFeatureService.Observation(
                    key.equals("spotPrice")?100.:1.,key.equals("spotPrice")?"BINANCE_SPOT_AGG_TRADE":"BINANCE_SPOT_DIFF_DEPTH",start,start,
                    AssetCardFeatureService.spotInstrument("BTCUSDT"),AssetCardFeatureService.SPOT_SOURCE_VERSION,AssetCardFeatureService.observationUnit(key),start.plusSeconds(10),key.equals("spotPrice")?"8001":null));
            evidence.put("fundingRate",new AssetCardFeatureService.Observation(.0001,"COINGLASS:COINGLASS_FUNDING:TEST_PIPELINE",start,start,
                    "BINANCE:PERPETUAL:LINEAR:BTC/USDT","TEST_PIPELINE_V1","RATE",start.plusSeconds(60)));
            var raw=new AssetCardFeatureService.RawFrame("BTCUSDT",start,bars,evidence);
            var frame=new AssetCardFeatureService().build(raw); assertThat(frame.ready()).isTrue();
            String encoded = json.writeValueAsString(Map.of("rawFrame",raw,"frame",frame,"outcome","COMPLETED","dataKind","LIVE_OBSERVED_CARD_INPUTS"));
            if (seedInference) mapper.saveInference("BTCUSDT",close,start,encoded);
            var seeded = seedInference ? mapper.selectInference("BTCUSDT",close,cutoff).orElseThrow()
                    : new org.example.trademodel.mapper.AssetCardMapper.TypedHistory("BTCUSDT",
                    org.example.trademodel.mapper.AssetCardMapper.HistoryKind.INFERENCE, "5m:" + close, close, start, encoded);
            Instant first=start.minusMillis(123);
            for(int seconds:List.of(60,300)) for(var b:labelBars(first,14700/seconds,seconds,100.5,99.5)) mapper.upsertClosedBar(new AssetCardMarketDataService.SpotBar("BTCUSDT",seconds==60?"1m":"5m",b.openTime(),b.closeTime(),
                    BigDecimal.valueOf(b.open()),BigDecimal.valueOf(b.high()),BigDecimal.valueOf(b.low()),BigDecimal.valueOf(b.close()),BigDecimal.TEN,BigDecimal.valueOf(5),10L,b.availableAt()));
            if(horizon) horizonTrade(false);
            return seeded;
        }
        void horizonTrade(boolean late) throws Exception {
            Instant end=start.plusSeconds(14400), observed=end.minusMillis(500), available=late?end.plusNanos(1):end.minusMillis(300).plusNanos(123456);
            var quote=new AssetCardMarketDataService.SpotQuote("BTCUSDT",BigDecimal.valueOf(100),BigDecimal.ONE,9001,observed,available);
            var o=new AssetCardFeatureService.Observation(100.,"BINANCE_SPOT_AGG_TRADE",observed,available,AssetCardFeatureService.spotInstrument("BTCUSDT"),AssetCardFeatureService.SPOT_SOURCE_VERSION,"QUOTE_CURRENCY",observed.plusSeconds(10),"9001");
            mapper.saveTradeObservation(quote,o.instrument(),o.sourceVersion(),json.writeValueAsString(Map.of("observation",o,"dataKind","LIVE_OBSERVED_CARD_TRADE")));
        }
        void saveMatured() { var pipeline=new AssetCardService.LabelPipeline(mapper,json); var item=pipeline.materialize(inference,cutoff); assertThat(item.status()).isEqualTo("MATURED"); pipeline.save(inference,item); }
        List<org.example.trademodel.mapper.AssetCardMapper.TypedHistory> labels() { return mapper.selectHistory("BTCUSDT",org.example.trademodel.mapper.AssetCardMapper.HistoryKind.LABEL,start,start,cutoff,10); }
        @Override public void close() { if (ownedH2) jdbc.execute("SHUTDOWN"); }
    }
    @Test
    void matureLabelsRequireCompleteFuturePathAndUseFrozenFirstPassagePerSide() {
        var start = Instant.parse("2026-09-10T10:00:00Z");
        var five = labelBars(start, 48, 300, 100.5, 99.5);
        assertThat(AssetCardService.LabelPipeline.firstTouch(100, 1, start, five, List.of(), "LONG").outcome()).isEqualTo("TIMEOUT");
        five.set(3, labelBar(start.plusSeconds(900), 300, 101.2, 99.5));
        assertThat(AssetCardService.LabelPipeline.firstTouch(100, 1, start, five, List.of(), "LONG").y()).isEqualTo(1);
        assertThat(AssetCardService.LabelPipeline.firstTouch(100, 1, start, five, List.of(), "SHORT").outcome()).isEqualTo("STOP");
        assertThat(AssetCardService.LabelPipeline.firstTouch(100, 1, start, five.subList(0, 47), List.of(), "LONG").outcome()).isEqualTo("INCOMPLETE_HORIZON");
    }

    @Test
    void sameBarTouchUsesMinuteOrderAndNeverInventsAnOrderWithinOneMinute() {
        var start = Instant.parse("2026-09-10T10:00:00Z");
        var five = labelBars(start, 48, 300, 100.5, 99.5);
        five.set(1, labelBar(start.plusSeconds(300), 300, 101.2, 99));
        var minute = labelBars(start.plusSeconds(300), 5, 60, 100.5, 99.5);
        minute.set(0, labelBar(start.plusSeconds(300), 60, 101.2, 99.5));
        minute.set(1, labelBar(start.plusSeconds(360), 60, 100.5, 99));
        assertThat(AssetCardService.LabelPipeline.firstTouch(100, 1, start, five, minute, "LONG").outcome()).isEqualTo("TARGET");
        minute.set(0, labelBar(start.plusSeconds(300), 60, 101.2, 99));
        assertThat(AssetCardService.LabelPipeline.firstTouch(100, 1, start, five, minute, "LONG").outcome()).isEqualTo("AMBIGUOUS");
        assertThat(AssetCardService.LabelPipeline.firstTouch(100, 1, start, five, List.of(), "LONG").y()).isNull();
    }

    @Test
    void actualInferenceTimeIsNotRoundedAndPartialMinuteTouchIsExcluded() {
        var boundary = Instant.parse("2026-09-10T10:00:00Z");
        var five = labelBars(boundary, 49, 300, 100.5, 99.5);
        five.set(0, labelBar(boundary, 300, 101.2, 99.5));
        var minute = labelBars(boundary, 5, 60, 100.5, 99.5);
        minute.set(0, labelBar(boundary, 60, 101.2, 99.5));
        assertThat(AssetCardService.LabelPipeline.firstTouch(100, 1, boundary.plusNanos(1), five, minute, "LONG").outcome()).isEqualTo("AMBIGUOUS");
        assertThat(AssetCardService.LabelPipeline.firstTouch(100, 1, boundary, five, minute, "LONG").outcome()).isEqualTo("TARGET");
    }

    private static java.util.ArrayList<AssetCardFeatureService.Bar> labelBars(Instant start, int count, int seconds, double high, double low) {
        var result = new java.util.ArrayList<AssetCardFeatureService.Bar>();
        for (int i = 0; i < count; i++) result.add(labelBar(start.plusSeconds((long)i * seconds), seconds, high, low));
        return result;
    }
    private static AssetCardFeatureService.Bar labelBar(Instant start, int seconds, double high, double low) {
        return new AssetCardFeatureService.Bar(start, start.plusSeconds(seconds).minusMillis(1), start.plusSeconds(seconds),
                100, high, low, 100, 10, 5.0, 10L);
    }
    @Test
    void priceOnlyPublicationHasIndependentClockAndNoModelOrWholeHomeEvent() {
        var properties = new AssetCardProperties(); properties.setEnabled(true);
        properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
        var market = org.mockito.Mockito.mock(AssetCardMarketDataService.class);
        var mapper = org.mockito.Mockito.mock(org.example.trademodel.mapper.AssetCardMapper.class);
        var events = org.mockito.Mockito.mock(org.example.trademodel.v41.DashboardLiveEventService.class);
        var pool = org.mockito.Mockito.mock(org.example.trademodel.service.watchlistsource.AssetPoolService.class);
        var at = Instant.parse("2026-09-10T10:00:00Z");
        org.mockito.Mockito.when(market.subscribedSymbols()).thenReturn(java.util.Set.of("BTCUSDT"));
        org.mockito.Mockito.when(market.quote("BTCUSDT", at)).thenReturn(java.util.Optional.of(
                new AssetCardMarketDataService.SpotQuote("BTCUSDT", new java.math.BigDecimal("100"), java.math.BigDecimal.ONE, 1, at, at)));
        org.mockito.Mockito.when(mapper.nextSnapshotVersion("BTCUSDT")).thenReturn(1L);
        org.mockito.Mockito.when(mapper.saveSnapshot(org.mockito.ArgumentMatchers.eq("BTCUSDT"),org.mockito.ArgumentMatchers.eq(0L),org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.isNull())).thenReturn(1);
        var service = new AssetCardService(properties,market,mapper,pool,events,new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());
        try {
            when(pool.listForUser(7L)).thenReturn(poolMembers("BTCUSDT"));
            service.registerCardStream(7L);
            service.flushPrices(at);
            clearInvocations(events);
            when(market.quote("BTCUSDT", at.plusSeconds(1))).thenReturn(Optional.of(
                    new AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.valueOf(101), BigDecimal.ONE, 2,
                            at.plusSeconds(1), at.plusSeconds(1))));
            service.flushPrices(at.plusSeconds(1));
            var captor = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            org.mockito.Mockito.verify(events,org.mockito.Mockito.atLeastOnce()).publishToUser(eq(7L), captor.capture());
            assertThat(captor.getAllValues()).allMatch(e -> java.util.Set.of("ASSET_CARD_PRICE","ASSET_CARD_HEALTH").contains(e.eventType()));
            var price = captor.getAllValues().stream().filter(e -> e.eventType().equals("ASSET_CARD_PRICE")).findFirst().orElseThrow();
            assertThat(price.payload()).containsEntry("spotPrice",new java.math.BigDecimal("101"))
                    .containsEntry("latestPriceAt",at.plusSeconds(1)).containsEntry("modelVersion", null)
                    .containsEntry("calibrationVersion", null).containsEntry("thresholdVersion", null)
                    .doesNotContainKeys("cardAsOf","signal","risk");
            org.mockito.Mockito.verify(mapper,org.mockito.Mockito.never()).saveFeatureHistory(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
            verify(mapper, never()).saveInference(anyString(), any(), any(), anyString());
        } finally { service.close(); }
    }

    @Test
    void sameRiskFactsAndSignalValuesDoNotAdvanceEffectiveCardClock() {
        var at = Instant.parse("2026-09-10T10:00:00Z");
        var first = new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.LONG,"VALID",null,.81,.2,"OPPORTUNITY","LONG",at);
        var later = new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.LONG,"VALID",null,.81,.2,"OPPORTUNITY","LONG",at.plusSeconds(300));
        assertThat(AssetCardService.sameEffectiveSignal(first,later)).isTrue();
        var backgroundOnly = new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.LONG,"VALID",null,.81,.2,"CONFLICT","SHORT",at.plusSeconds(300));
        assertThat(AssetCardService.sameEffectiveSignal(first,backgroundOnly)).isTrue();
        var risk = new AssetCardSnapshot.Risk("LOW", List.of(new AssetCardSnapshot.RiskItem("DATA","ASSESSED","NONE","COMPLETE","BINANCE_SPOT",at,"ready","STATE")),at);
        var renewed = new AssetCardSnapshot.Risk("LOW", List.of(new AssetCardSnapshot.RiskItem("DATA","ASSESSED","NONE","COMPLETE","BINANCE_SPOT",at.plusSeconds(1),"ready","STATE")),at.plusSeconds(1));
        assertThat(AssetCardService.sameEffectiveRisk(risk,renewed)).isTrue();
        assertThat(AssetCardService.sameEffectiveSignal(first,first.invalidated())).isFalse();
    }
    @Test
    void numericalConfidenceRequiresValidatedDirectionalProbabilityNotAnOldFallback() {
        Instant at = Instant.parse("2026-09-10T10:00:00Z");
        for (AssetCardSnapshot.Direction direction : AssetCardSnapshot.Direction.values()) {
            var signal = new AssetCardSnapshot.Signal(direction, "UNVALIDATED", 90,
                    0.81, 0.17, "观察", "震荡", at);
            assertThat(signal.calibratedConfidence()).isNull();
        }
        var valid = new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.LONG, "VALID", 90,
                0.81, 0.17, "机会", "偏多", at);
        assertThat(valid.calibratedConfidence()).isEqualTo(81);
        var noProbability = new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.LONG, "VALID", 90,
                null, null, "机会", "偏多", at);
        assertThat(noProbability.calibratedConfidence()).isNull();
    }

    @Test
    void invalidationRetainsDirectionButNeverConfidenceAndUnknownIsNotLow() {
        var signal = new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.STRONG_LONG, "INVALIDATED",
                90, 0.91, 0.12, "冲突", "偏多", Instant.EPOCH);
        assertThat(signal.direction()).isEqualTo(AssetCardSnapshot.Direction.STRONG_LONG);
        assertThat(signal.calibratedConfidence()).isNull();
        var empty = AssetCardSnapshot.unavailable("BTC", "Bitcoin", "尚未收到现货数据");
        assertThat(empty.signal().direction()).isNull();
        assertThat(empty.risk().overallLevel()).isNull();
        assertThat(empty.risk().items()).hasSize(8)
                .allMatch(item -> item.assessmentStatus().equals("UNKNOWN"));
        assertThat(empty.spotPrice()).isNull();
        assertThat(empty.cardAsOf()).isNull();
    }

    @Test
    void riskPayloadCannotBeMutatedAfterPublication() {
        var risks = new java.util.ArrayList<AssetCardSnapshot.RiskItem>();
        var risk = new AssetCardSnapshot.Risk(null, risks, null);
        risks.add(new AssetCardSnapshot.RiskItem("DATA", "UNKNOWN", null, null, null, null, "缺少数据"));
        assertThat(risk.items()).isEqualTo(List.of());
    }

    @Test
    void freshLatestFrameDoesNotInvalidatePendingConfirmationBecauseItsStructuralAnchorIsOlder() {
        Instant confirmedClose = Instant.parse("2026-09-10T11:54:59.999Z");
        Instant pendingClose = confirmedClose.plusSeconds(300), at = pendingClose.plusSeconds(20);
        try (var fixture = new RuntimeFixture()) {
            var signal = validLong(confirmedClose.plusSeconds(1));
            var state = new AssetCardSignalService.State("BTCUSDT", AssetCardSnapshot.Direction.LONG,
                    AssetCardSnapshot.Direction.SHORT, 1, pendingClose, identity(AssetCardProperties.ModelMode.ACTIVE, null),
                    null, signal, null, "WAITING_SECOND_CONSECUTIVE_CLOSE");
            fixture.seed(signal, at.minusSeconds(1));
            runtimeMap(fixture.service, "signalStates").put("BTCUSDT", state);
            runtimeMap(fixture.service, "signalFrames").put("BTCUSDT", runtimeFrame(confirmedClose));
            runtimeMap(fixture.service, "featureFrames").put("BTCUSDT", runtimeFrame(pendingClose));
            fixture.freshMarket();
            ReflectionTestUtils.invokeMethod(fixture.service, "refreshRisk", "BTCUSDT", at);
            AssetCardSignalService.State after = AssetCardServiceTest.<AssetCardSignalService.State>runtimeMap(fixture.service, "signalStates").get("BTCUSDT");
            assertThat(after.signal().status()).isEqualTo("VALID");
            assertThat(after.pendingDirection()).isEqualTo(AssetCardSnapshot.Direction.SHORT);
            assertThat(after.confirmationCount()).isEqualTo(1);
            assertThat(after.invalidatedAt()).isNull();
            assertThat(fixture.lastSnapshot().signal()).isEqualTo(signal);
        }
    }

    @Test
    void priceSourceLossInvalidatesStateAndRepeatedLossCannotAdvanceEffectiveClock() {
        Instant close = Instant.parse("2026-09-10T11:59:59.999Z"), at = close.plusSeconds(10);
        try (var fixture = new RuntimeFixture()) {
            var signal = validLong(close.plusSeconds(1));
            fixture.seed(signal, at.minusSeconds(1));
            var state = new AssetCardSignalService.State("BTCUSDT", AssetCardSnapshot.Direction.LONG, null, 0,
                    close, identity(AssetCardProperties.ModelMode.ACTIVE, null), null, signal, null, null);
            runtimeMap(fixture.service, "signalStates").put("BTCUSDT", state);
            when(fixture.market.quote(eq("BTCUSDT"), any())).thenReturn(Optional.empty());
            fixture.service.flushPrices(at);
            var first = fixture.lastSnapshot();
            AssetCardSignalService.State invalidated = AssetCardServiceTest.<AssetCardSignalService.State>runtimeMap(fixture.service, "signalStates").get("BTCUSDT");
            assertThat(invalidated.signal().status()).isEqualTo("INVALIDATED");
            assertThat(invalidated.signal().calibratedConfidence()).isNull();
            assertThat(invalidated.invalidatedAt()).isEqualTo(at);
            assertThat(first.risk().overallLevel()).isEqualTo("HIGH");
            assertThat(first.cardAsOf()).isEqualTo(at);
            fixture.service.flushPrices(at.plusSeconds(1));
            assertThat(fixture.lastSnapshot().cardAsOf()).isEqualTo(first.cardAsOf());
            AssetCardSignalService.State repeated = AssetCardServiceTest.<AssetCardSignalService.State>runtimeMap(fixture.service, "signalStates").get("BTCUSDT");
            assertThat(repeated.invalidatedAt()).isEqualTo(at);
            assertThat(repeated.pendingDirection()).isNull();
            assertThat(repeated.confirmationCount()).isZero();
            verify(fixture.mapper, never()).saveFeatureHistory(any(), any(), any(), any());
            verify(fixture.mapper, never()).saveInference(anyString(), any(), any(), anyString());
        }
    }

    @Test
    void riskRefreshFailureCannotEraseAHealthySpotPrice() {
        Instant at = Instant.now();
        try (var fixture = new RuntimeFixture()) {
            fixture.seed(validLong(at.minusSeconds(2)), at.minusSeconds(1));
            fixture.freshMarket();
            when(fixture.market.book(eq("BTCUSDT"), any())).thenThrow(new IllegalStateException("ISOLATED_RISK_FAILURE"));
            ReflectionTestUtils.invokeMethod(fixture.service, "refreshRisksSafely");
            var result = fixture.service.snapshot("BTCUSDT", "Bitcoin");
            assertThat(result.spotPrice()).isEqualByComparingTo("100");
            assertThat(result.risk().overallLevel()).isNull();
            assertThat(result.health().status()).isNotEqualTo("SOURCE_UNAVAILABLE");
            verifyNoInteractions(fixture.events);
        }
    }

    @Test
    void repeatedIdenticalRiskFactsDoNotAllocateOrPersistAnotherSnapshot() {
        Instant at = Instant.parse("2026-09-10T12:00:00Z");
        try (var fixture = new RuntimeFixture()) {
            fixture.seed(validLong(at.minusSeconds(1)), at.minusSeconds(1));
            fixture.freshMarket();
            fixture.service.refreshRisk("BTCUSDT", at);
            clearInvocations(fixture.mapper, fixture.events);
            fixture.service.refreshRisk("BTCUSDT", at.plusSeconds(1));
            verify(fixture.mapper, never()).nextSnapshotVersion(anyString());
            verify(fixture.mapper, never()).saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), any());
            verifyNoInteractions(fixture.events);
        }
    }

    @Test
    void matchingShadowAuditRestoresRuntimeFramesWithoutRepeatingInferenceOrCreatingAnotherAudit() throws Exception {
        Instant close = Instant.parse("2026-09-10T11:59:59.999Z"), at = close.plusSeconds(5);
        try (var fixture = new RuntimeFixture()) {
            var frame = runtimeFrame(close);
            var state = new AssetCardSignalService.State("BTCUSDT", null, null, 0, close,
                    identity(AssetCardProperties.ModelMode.SHADOW, null), null,
                    AssetCardSnapshot.Signal.unavailable("SHADOW", frame.signalAsOf()), null, "MODEL_UNAVAILABLE");
            when(fixture.mapper.selectHistory(eq("BTCUSDT"), eq(org.example.trademodel.mapper.AssetCardMapper.HistoryKind.INFERENCE), any(), any(), any(), anyInt()))
                    .thenReturn(List.of(fixture.audit(frame, state)));
            ReflectionTestUtils.invokeMethod(fixture.service, "recoverRuntimeState", "BTCUSDT", at);
            assertThat(AssetCardServiceTest.<AssetCardSignalService.State>runtimeMap(fixture.service, "signalStates").get("BTCUSDT")).isEqualTo(state);
            assertThat(AssetCardServiceTest.<AssetCardFeatureService.Frame>runtimeMap(fixture.service, "featureFrames").get("BTCUSDT")).isEqualTo(frame);
            assertThat(AssetCardServiceTest.<AssetCardFeatureService.Frame>runtimeMap(fixture.service, "signalFrames").get("BTCUSDT")).isEqualTo(frame);
            assertThat(fixture.lastSnapshot().signal().status()).isEqualTo("SHADOW");
            assertThat(fixture.lastSnapshot().signal().calibratedConfidence()).isNull();
            verify(fixture.mapper, never()).saveFeatureHistory(any(), any(), any(), any());
            verify(fixture.mapper, never()).saveInference(anyString(), any(), any(), anyString());
            verify(fixture.market, never()).bars(any(), any(), any(), anyInt());
            verifyNoInteractions(fixture.pool);
        }
    }

    @Test
    void recoveryRejectsMismatchedModelOrFeatureMetadataRatherThanRevivingAnotherBundle() throws Exception {
        Instant close = Instant.parse("2026-09-10T11:59:59.999Z"), at = close.plusSeconds(5);
        for (String mismatch : List.of("model", "feature")) {
            try (var fixture = new RuntimeFixture()) {
                var original = runtimeFrame(close);
                var frame = "feature".equals(mismatch) ? new AssetCardFeatureService.Frame(original.symbol(), original.closed5mAt(),
                        original.signalAsOf(), original.availableAt(), "OTHER_FEATURE_VERSION", original.featureNames(), original.vector(),
                        original.ready(), original.reasons(), original.oneHourState(), original.fourHourTrend(), original.atr(),
                        original.structuralSupport(), original.structuralResistance(), original.realInputs()) : original;
                var state = new AssetCardSignalService.State("BTCUSDT", null, null, 0, close,
                        identity(AssetCardProperties.ModelMode.SHADOW, "model".equals(mismatch) ? "OTHER_MODEL" : null), null,
                        AssetCardSnapshot.Signal.unavailable("SHADOW", frame.signalAsOf()), null, "MODEL_UNAVAILABLE");
                when(fixture.mapper.selectHistory(eq("BTCUSDT"), eq(org.example.trademodel.mapper.AssetCardMapper.HistoryKind.INFERENCE), any(), any(), any(), anyInt()))
                        .thenReturn(List.of(fixture.audit(frame, state)));
                ReflectionTestUtils.invokeMethod(fixture.service, "recoverRuntimeState", "BTCUSDT", at);
                assertThat(runtimeMap(fixture.service, "signalStates")).doesNotContainKey("BTCUSDT");
                assertThat(runtimeMap(fixture.service, "featureFrames")).doesNotContainKey("BTCUSDT");
                assertThat(runtimeMap(fixture.service, "signalFrames")).doesNotContainKey("BTCUSDT");
                verify(fixture.mapper, never()).saveFeatureHistory(any(), any(), any(), any());
                verify(fixture.mapper, never()).saveInference(anyString(), any(), any(), anyString());
                verify(fixture.market, never()).bars(any(), any(), any(), anyInt());
            }
        }
    }

    @Test
    void coincidentTimeframeCloseWaitsForCurrentHourBeforeRecordingExactlyOneFiveMinuteInference() throws Exception {
        Instant close = Instant.parse("2026-09-10T11:59:59.999Z");
        try (var fixture = new RuntimeFixture()) {
            fixture.freshMarket();
            when(fixture.market.bars(eq("BTCUSDT"), anyString(), any(), eq(24))).thenAnswer(invocation -> {
                String interval = invocation.getArgument(1);
                return closedBars(interval, "1h".equals(interval) ? close.minusSeconds(3600) : close);
            });
            fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(1));
            verify(fixture.mapper, never()).saveFeatureHistory(any(), any(), any(), any());
            verify(fixture.mapper, never()).saveInference(anyString(), any(), any(), anyString());
            assertThat(runtimeMap(fixture.service, "featureFrames")).doesNotContainKey("BTCUSDT");
            when(fixture.market.bars(eq("BTCUSDT"), anyString(), any(), eq(24)))
                    .thenAnswer(invocation -> closedBars(invocation.getArgument(1), close));
            fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(3));
            var payload = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(fixture.mapper, times(1)).saveInference(eq("BTCUSDT"), eq(close), any(), payload.capture());
            var audit = fixture.json.readTree(payload.getValue());
            var hours = audit.path("rawFrame").path("bars").path("1h");
            assertThat(hours.size()).isEqualTo(24);
            assertThat(fixture.json.treeToValue(hours.get(23).get("closeTime"), Instant.class)).isEqualTo(close);
            assertThat(fixture.json.treeToValue(audit.path("frame").get("closed5mAt"), Instant.class)).isEqualTo(close);
            fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(4));
            verify(fixture.mapper, times(1)).saveInference(eq("BTCUSDT"), eq(close), any(), any());
            verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
        }
    }

    @Test
    void releaseVisibilityIsAnExplicitCohortIndependentOfMissingModelOrSnapshot() {
        for (boolean enabled : List.of(false, true)) {
            for (var mode : AssetCardProperties.ModelMode.values()) {
                try (var fixture = new RuntimeFixture()) {
                    fixture.properties.setEnabled(enabled);
                    fixture.properties.setModelMode(mode);
                    fixture.properties.setCanarySymbols(Set.of(" btcusdt "));
                    boolean all = enabled && mode == AssetCardProperties.ModelMode.ACTIVE;
                    boolean canary = enabled && mode == AssetCardProperties.ModelMode.CANARY;
                    assertThat(fixture.service.usesCardSignalDisplay("BTCUSDT")).as(enabled + ":" + mode).isEqualTo(all || canary);
                    assertThat(fixture.service.usesCardSignalDisplay("ETHUSDT")).isEqualTo(all);
                    assertThat(fixture.service.usesCardSignalDisplay("BTCUSDC")).isEqualTo(all);
                    assertThat(fixture.service.usesCardSignalDisplay(null)).isFalse();
                    assertThat(fixture.service.usesCardSignalDisplay("BTC/USDT")).isFalse();
                    assertThat(fixture.service.usesCardSignalDisplay("BTC*")).isFalse();
                    verifyNoInteractions(fixture.mapper, fixture.market, fixture.events, fixture.pool);
                }
            }
        }
        var properties = new AssetCardProperties();
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getModelMode()).isEqualTo(AssetCardProperties.ModelMode.SHADOW);
        assertThatThrownBy(() -> properties.setCanarySymbols(Set.of("BTC*"))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> properties.setCanarySymbols(Set.of("BTC/USDT"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void disabledReleaseStartsNoWorkersAndPerformsNoBackgroundReadsOrWrites() {
        try (var fixture = new RuntimeFixture()) {
            fixture.properties.setEnabled(false);
            fixture.service.start();
            fixture.service.reconcileSubscriptions();
            fixture.service.flushPrices(Instant.EPOCH);
            fixture.service.inferClosedBar("BTCUSDT", Instant.EPOCH, Instant.EPOCH.plusSeconds(1));
            fixture.service.refreshRisk("BTCUSDT", Instant.EPOCH);
            ReflectionTestUtils.invokeMethod(fixture.service, "refreshMinuteVolatility", "BTCUSDT", Instant.EPOCH);
            ReflectionTestUtils.invokeMethod(fixture.service, "refreshRisksSafely");
            ReflectionTestUtils.invokeMethod(fixture.service, "reconcileEvidenceSafely");
            assertThat(ReflectionTestUtils.getField(fixture.service, "started")).isEqualTo(false);
            assertThat(runtimeMap(fixture.service, "snapshots")).isEmpty();
            verifyNoInteractions(fixture.mapper, fixture.market, fixture.events, fixture.pool);
        }
    }

    @Test
    void shadowClosedBarStillComputesAndPersistsPrivatePredictionAuditWithoutPublicEvents() throws Exception {
        Instant close = Instant.parse("2026-09-10T11:59:59.999Z");
        try (var fixture = new RuntimeFixture()) {
            assertThat(fixture.properties.getModelMode()).isEqualTo(AssetCardProperties.ModelMode.SHADOW);
            try (var lease = registry(fixture.service).acquire("BTCUSDT")) {
                assertThat(lease.bundle().reason()).isEqualTo("MODEL_NOT_CONFIGURED");
                assertThat(lease.bundle().validated()).isFalse();
            }
            fixture.freshMarket();
            when(fixture.market.bars(eq("BTCUSDT"), anyString(), any(), eq(24)))
                    .thenAnswer(invocation -> closedBars(invocation.getArgument(1), close));
            fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(3));

            var captured = org.mockito.ArgumentCaptor.forClass(String.class);
            var completedAt = org.mockito.ArgumentCaptor.forClass(Instant.class);
            verify(fixture.mapper).saveInference(eq("BTCUSDT"), eq(close), completedAt.capture(), captured.capture());
            assertThat(completedAt.getValue()).isBetween(close.plusSeconds(3), close.plusSeconds(15));
            verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
            var audit = fixture.json.readTree(captured.getValue());
            assertThat(audit.path("modelMode").asText()).isEqualTo("SHADOW");
            assertThat(audit.path("rawFrame").path("bars").path("5m").size()).isEqualTo(24);
            assertThat(audit.path("state").path("auditPrediction").isObject()).isTrue();
            assertThat(audit.path("state").path("auditPrediction").path("available").asBoolean()).isFalse();
            // The bundle retains its specific configuration reason; predict() classifies all non-valid bundles separately.
            assertThat(audit.path("state").path("auditPrediction").path("reasons").toString())
                    .isEqualTo("[\"MODEL_EXPIRED_OR_UNAVAILABLE\"]");
            for (String probability : List.of("rawLong", "rawShort", "pLong", "pShort"))
                assertThat(audit.path("state").path("auditPrediction").path(probability).isNull()).isTrue();
            assertThat(fixture.lastSnapshot().signal().status()).isEqualTo("SHADOW");
            assertThat(fixture.lastSnapshot().signal().calibratedConfidence()).isNull();
            assertThat(fixture.lastSnapshot().signal().direction()).isNull();
            verify(fixture.mapper).saveSnapshot(eq("BTCUSDT"), anyLong(), anyLong(), anyString(), nullable(Instant.class));
            assertThat(fixture.service.usesCardSignalDisplay("BTCUSDT")).isFalse();
            verifyNoInteractions(fixture.events);
        }
    }

    @Test
    void shadowRetainsAuditOnlyPredictionInPrivateSnapshotWhileBothPublicChannelsStayEmpty() throws Exception {
        Instant at = Instant.parse("2026-09-10T12:00:00Z");
        try (var fixture = new RuntimeFixture()) {
            // Explicit test-only numbers exercise persistence; they are not a trained model or readiness evidence.
            var prediction = new AssetCardModelBundle.Prediction(true, .81, .21, .79, .23,
                    "TEST_FIXTURE", "TEST_FIXTURE", "TEST_FIXTURE", null, List.of("TEST_FIXTURE"));
            var hidden = AssetCardSnapshot.Signal.unavailable("SHADOW", at);
            var state = new AssetCardSignalService.State("BTCUSDT", AssetCardSnapshot.Direction.LONG, null, 0,
                    at.minusSeconds(1), identity(AssetCardProperties.ModelMode.SHADOW, "TEST_FIXTURE"), prediction,
                    hidden, null, "TEST_FIXTURE");
            runtimeMap(fixture.service, "signalStates").put("BTCUSDT", state);
            runtimeMap(fixture.service, "featureFrames").put("BTCUSDT", runtimeFrame(at.minusSeconds(1)));
            fixture.seed(hidden, at);
            var before = fixture.lastSnapshot();
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", new AssetCardSnapshot(before.symbol(), before.assetName(),
                    before.spotPrice(), before.latestPriceAt(), before.signal(), before.risk(),
                    new AssetCardSnapshot.Health("INSUFFICIENT_DATA", "TEST_FIXTURE_BEFORE_PRICE_RECOVERY", at), before.cardAsOf(),
                    before.snapshotVersion(), before.featureVersion(), before.modelVersion(), before.calibrationVersion(), before.thresholdVersion()));
            fixture.freshMarket();
            fixture.service.flushPrices(at.plusSeconds(1));

            var captured = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(fixture.mapper).saveSnapshot(eq("BTCUSDT"), anyLong(), anyLong(), captured.capture(), nullable(Instant.class));
            var persisted = fixture.json.readTree(captured.getValue());
            assertThat(persisted.path("_runtime").path("state").path("auditPrediction").path("rawLong").asDouble()).isEqualTo(.81);
            assertThat(persisted.path("_runtime").path("state").path("auditPrediction").path("pLong").asDouble()).isEqualTo(.79);
            assertThat(persisted.path("signal").path("calibratedConfidence").isNull()).isTrue();
            when(fixture.pool.listForUser(7L)).thenReturn(List.of(new org.example.trademodel.dto.assetpool.AssetPoolAssetDTO(
                    1L, "BTCUSDT", "Bitcoin", "SPOT", "USDT", true, 1, "USER")));
            assertThat(fixture.service.snapshotsForUser(7L, List.of("BTCUSDT"))).isEmpty();
            verifyNoInteractions(fixture.events);
        }
    }

    @Test
    void canaryPublishesOnlyItsExactSymbolWhilePrivateSnapshotsForOtherSymbolsStillPersist() {
        Instant at = Instant.parse("2026-09-10T12:00:00Z");
        try (var fixture = new RuntimeFixture()) {
            fixture.properties.setModelMode(AssetCardProperties.ModelMode.CANARY);
            fixture.properties.setCanarySymbols(Set.of("BTCUSDT"));
            subscribeFixture(fixture);
            fixture.freshMarket();
            when(fixture.market.subscribedSymbols()).thenReturn(Set.of("BTCUSDT", "ETHUSDT"));
            when(fixture.mapper.nextSnapshotVersion("ETHUSDT")).thenReturn(20L);
            when(fixture.market.quote("ETHUSDT", at)).thenReturn(Optional.of(new AssetCardMarketDataService.SpotQuote(
                    "ETHUSDT", BigDecimal.valueOf(100), BigDecimal.ONE, 1, at, at)));
            fixture.service.flushPrices(at);

            verify(fixture.mapper).saveSnapshot(eq("BTCUSDT"), anyLong(), anyLong(), anyString(), nullable(Instant.class));
            verify(fixture.mapper).saveSnapshot(eq("ETHUSDT"), anyLong(), anyLong(), anyString(), nullable(Instant.class));
            var published = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            verify(fixture.events, atLeastOnce()).publishToUser(eq(7L), published.capture());
            assertThat(published.getAllValues()).allSatisfy(event -> assertThat(event.symbol()).isEqualTo("BTCUSDT"));
        }
    }

    @Test
    void storedAndCachedValidSignalsRequireCurrentBundleAndExactIdentityOnEveryPublicRead() throws Exception {
        for (var mode : List.of(AssetCardProperties.ModelMode.ACTIVE, AssetCardProperties.ModelMode.CANARY)) {
            for (boolean cached : List.of(false, true)) {
                for (String mismatch : List.of("missing", "closed", "feature", "model", "calibration", "asset", "threshold", "risk", "none")) {
                    try (var fixture = new RuntimeFixture()) {
                        fixture.properties.setModelMode(mode);
                        fixture.properties.setCanarySymbols(Set.of("BTCUSDT"));
                        // Exercise Linux nanosecond precision deterministically on every host.
                        Instant at = Instant.ofEpochSecond(Instant.now().getEpochSecond() - 1, 309_209_149);
                        var stored = publicSignalFixture(at, "feature".equals(mismatch) ? "OLD_FEATURE" : AssetCardFeatureService.FEATURE_VERSION,
                                "threshold".equals(mismatch) ? "OLD_THRESHOLD" : "TEST_FIXTURE_THRESHOLDS",
                                "risk".equals(mismatch) ? "OLD_RISK" : "TEST_RISK");
                        if (!"missing".equals(mismatch)) {
                            var bundle = metadataOnlyBundle("model".equals(mismatch) ? "NEW_MODEL" : "TEST_FIXTURE_MODEL",
                                    "calibration".equals(mismatch) ? "NEW_CALIBRATION" : "TEST_FIXTURE_CALIBRATION",
                                    "asset".equals(mismatch) ? Set.of("ETHUSDT") : Set.of("BTCUSDT"));
                            if ("closed".equals(mismatch)) ReflectionTestUtils.setField(bundle, "closed", true);
                            installBundle(fixture.service, "BTCUSDT", bundle);
                        }
                        if (cached) runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
                        else when(fixture.mapper.selectSnapshotJson("BTCUSDT")).thenReturn(fixture.json.writeValueAsString(stored));
                        when(fixture.pool.listForUser(7L)).thenReturn(List.of(new org.example.trademodel.dto.assetpool.AssetPoolAssetDTO(
                                1L, "BTCUSDT", "Bitcoin", "SPOT", "USDT", true, 1, "USER")));

                        var visible = fixture.service.snapshotsForUser(7L, List.of("BTCUSDT")).get(0);

                        assertThat(fixture.service.usesCardSignalDisplay("BTCUSDT")).isTrue();
                        assertThat(visible.spotPrice()).isEqualTo(stored.spotPrice());
                        assertThat(visible.latestPriceAt()).isEqualTo(stored.latestPriceAt());
                        if ("none".equals(mismatch)) assertThat(visible.risk()).isEqualTo(stored.risk());
                        else {
                            assertThat(visible.risk().overallLevel()).isNull();
                            assertThat(visible.risk().items()).hasSize(8).allMatch(item -> "UNKNOWN".equals(item.assessmentStatus()));
                            assertThat(visible.risk().matchesBasis(visible.signal())).isTrue();
                        }
                        assertThat(visible.cardAsOf()).isEqualTo(stored.cardAsOf());
                        assertThat(visible.snapshotVersion()).isEqualTo(stored.snapshotVersion());
                        if (Set.of("none", "risk").contains(mismatch)) assertThat(visible.signal()).isEqualTo(stored.signal());
                        else {
                            assertThat(visible.signal().direction()).as(mode + ":" + cached + ":" + mismatch).isNull();
                            assertThat(visible.signal().calibratedConfidence()).isNull();
                            assertThat(visible.signal().pLong()).isNull();
                            assertThat(visible.signal().pShort()).isNull();
                            assertThat(visible.signal().status()).isEqualTo("UNVALIDATED");
                            assertThat(visible.health().status()).isEqualTo("MODEL_UNAVAILABLE");
                            assertThat(visible.health().asOf()).isBetween(at, Instant.now());
                        }
                        assertThat(stored.signal().calibratedConfidence()).isEqualTo(81);
                        assertThat(fixture.json.isEnabled(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS))
                                .as("Card precision policy must not mutate the shared application mapper").isFalse();
                        verify(fixture.mapper, never()).saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), nullable(Instant.class));
                        verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
                        verify(fixture.mapper, never()).saveInference(anyString(), any(), any(), anyString());
                        verify(fixture.market, times(1)).quote(eq("BTCUSDT"), any());
                        verifyNoMoreInteractions(fixture.market);
                        verifyNoInteractions(fixture.events);
                    }
                }
            }
        }
    }

    @Test
    void priceRefreshPersistsPrivateOldSignalButPublicEventsCannotLeakRevokedModelConfidence() {
        try (var fixture = new RuntimeFixture()) {
            fixture.properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
            subscribeFixture(fixture);
            Instant at = Instant.now();
            var stored = publicSignalFixture(at.minusSeconds(1), AssetCardFeatureService.FEATURE_VERSION);
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
            fixture.freshMarket();
            fixture.service.flushPrices(at);

            // Rollout visibility is not a mutation of the original private audit/snapshot chain.
            assertThat(fixture.lastSnapshot().signal().calibratedConfidence()).isEqualTo(81);
            verify(fixture.mapper, never()).saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), nullable(Instant.class));
            var captured = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            verify(fixture.events, atLeastOnce()).publishToUser(eq(7L), captured.capture());
            assertThat(captured.getAllValues()).anySatisfy(event -> {
                assertThat(event.eventType()).isEqualTo("ASSET_CARD_PRICE");
                assertThat(event.payload()).containsEntry("spotPrice", BigDecimal.valueOf(100));
            }).anySatisfy(event -> {
                assertThat(event.eventType()).isEqualTo("ASSET_CARD_HEALTH");
                var health = (AssetCardSnapshot.Health) event.payload().get("health");
                assertThat(health.status()).isEqualTo("MODEL_UNAVAILABLE");
                assertThat(health.asOf()).isEqualTo(at);
                assertThat(event.snapshotVersion()).isEqualTo(fixture.lastSnapshot().snapshotVersion());
            });
            var directPayload = new java.util.LinkedHashMap<String, Object>();
            directPayload.put("signal", stored.signal());
            ReflectionTestUtils.invokeMethod(fixture.service, "publish", stored, "ASSET_CARD_SIGNAL", directPayload, at);
            fixture.service.flushPublications(at);
            var all = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            verify(fixture.events, atLeastOnce()).publishToUser(eq(7L), all.capture());
            assertThat(all.getAllValues()).filteredOn(event -> "ASSET_CARD_SIGNAL".equals(event.eventType()))
                    .isNotEmpty()
                    .allSatisfy(event -> {
                        var signal = (AssetCardSnapshot.Signal) event.payload().get("signal");
                        assertThat(signal.direction()).isNull();
                        assertThat(signal.calibratedConfidence()).isNull();
                        assertThat(signal.pLong()).isNull();
                        assertThat(signal.pShort()).isNull();
                    });
        }
    }

    @Test
    void readOnlyPriceIdentityPreservesNullableStoredIdAndUsesOnlyTheActualQuoteId() {
        for (Long storedId : new Long[]{null, 41L}) {
            try (var fixture = new RuntimeFixture()) {
                Instant at = Instant.now();
                var source = publicSignalFixture(at, AssetCardFeatureService.FEATURE_VERSION);
                var stored = new AssetCardSnapshot(source.symbol(), source.assetName(), source.spotPrice(), source.latestPriceAt(),
                        source.signal(), source.risk(), source.health(), source.cardAsOf(), source.snapshotVersion(),
                        source.featureVersion(), source.modelVersion(), source.calibrationVersion(), source.thresholdVersion(), storedId);
                runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);

                var withoutQuote = fixture.service.snapshot("BTCUSDT", "Bitcoin");
                assertThat(withoutQuote.priceTradeId()).isEqualTo(storedId);
                assertThat(withoutQuote.spotPrice()).isEqualTo(stored.spotPrice());
                assertThat(withoutQuote.latestPriceAt()).isEqualTo(stored.latestPriceAt());
                assertThat(withoutQuote.snapshotVersion()).isEqualTo(stored.snapshotVersion());

                var quote = new AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.valueOf(101), BigDecimal.ONE, 83L, at, at);
                when(fixture.market.quote(eq("BTCUSDT"), any())).thenReturn(Optional.of(quote));
                var withQuote = fixture.service.snapshot("BTCUSDT", "Bitcoin");
                assertThat(withQuote.priceTradeId()).isEqualTo(83L);
                assertThat(withQuote.spotPrice()).isEqualTo(quote.price());
                assertThat(withQuote.latestPriceAt()).isEqualTo(quote.observedAt());
                assertThat(withQuote.snapshotVersion()).isEqualTo(stored.snapshotVersion());
                assertThat(fixture.lastSnapshot()).isSameAs(stored);
                verify(fixture.market, times(2)).quote(eq("BTCUSDT"), any());
                verifyNoMoreInteractions(fixture.market);
                verifyNoInteractions(fixture.mapper, fixture.events, fixture.pool);
            }
        }
    }

    @Test
    void expiredSpotReadProjectsDataHighWithoutAllocatingOrWritingAnything() {
        try (var fixture = new RuntimeFixture()) {
            Instant at = Instant.now().minus(fixture.properties.getPriceTtl()).minusSeconds(2);
            var stored = publicSignalFixture(at, AssetCardFeatureService.FEATURE_VERSION);
            installBundle(fixture.service, "BTCUSDT", metadataOnlyBundle("TEST_FIXTURE_MODEL", "TEST_FIXTURE_CALIBRATION", Set.of("BTCUSDT")));
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
            var visible = fixture.service.snapshot("BTCUSDT", "Bitcoin");
            assertThat(visible.spotPrice()).isNull();
            assertThat(visible.priceTradeId()).isNull();
            assertThat(visible.signal().direction()).isEqualTo(stored.signal().direction());
            assertThat(visible.signal().status()).isEqualTo("INVALIDATED");
            assertThat(visible.signal().calibratedConfidence()).isNull();
            assertThat(visible.risk().overallLevel()).isEqualTo("HIGH");
            assertThat(visible.risk().items()).filteredOn(item -> "DATA".equals(item.type())).singleElement().satisfies(item -> {
                assertThat(item.assessmentStatus()).isEqualTo("ASSESSED");
                assertThat(item.level()).isEqualTo("HIGH");
                assertThat(item.source()).isEqualTo("BINANCE_SPOT_AGG_TRADE");
                assertThat(item.invalidatesSignal()).isTrue();
            });
            assertThat(visible.risk().matchesBasis(visible.signal())).isTrue();
            assertThat(visible.snapshotVersion()).isEqualTo(stored.snapshotVersion());
            assertThat(fixture.lastSnapshot()).isSameAs(stored);
            verify(fixture.mapper, never()).nextSnapshotVersion(anyString());
            verify(fixture.mapper, never()).saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), any());
            verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
            verify(fixture.mapper, never()).saveInference(anyString(), any(), any(), anyString());
            verify(fixture.market, times(1)).quote(eq("BTCUSDT"), any());
            verifyNoMoreInteractions(fixture.market);
            verifyNoInteractions(fixture.events, fixture.pool);
        }
    }

    @Test
    void signalWorkerFailureKeepsHealthySpotAndTheSameBoundRiskWhenModelIsStillValid() {
        try (var fixture = new RuntimeFixture()) {
            Instant at = Instant.now();
            Instant close = Instant.parse("2026-09-10T11:59:59.999Z");
            var stored = publicSignalFixture(at, AssetCardFeatureService.FEATURE_VERSION);
            installBundle(fixture.service, "BTCUSDT", metadataOnlyBundle("TEST_FIXTURE_MODEL", "TEST_FIXTURE_CALIBRATION", Set.of("BTCUSDT")));
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
            when(fixture.market.bars(eq("BTCUSDT"), anyString(), any(), anyInt())).thenThrow(new IllegalStateException("TEST_SIGNAL_INPUT_FAILURE"));
            fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(1));
            var visible = fixture.service.snapshot("BTCUSDT", "Bitcoin");
            assertThat(visible.spotPrice()).isEqualTo(stored.spotPrice());
            assertThat(visible.latestPriceAt()).isEqualTo(stored.latestPriceAt());
            assertThat(visible.signal().status()).isEqualTo("FAILED");
            assertThat(visible.signal().direction()).isEqualTo(stored.signal().direction());
            assertThat(visible.signal().signalAsOf()).isEqualTo(stored.signal().signalAsOf());
            assertThat(visible.signal().calibratedConfidence()).isNull();
            assertThat(visible.risk()).isEqualTo(stored.risk());
            assertThat(visible.risk().matchesBasis(visible.signal())).isTrue();
            verify(fixture.mapper).saveInference(eq("BTCUSDT"), eq(close), any(), contains("FAILED"));
            verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
            verifyNoInteractions(fixture.events, fixture.pool);
        }
    }

    @Test
    void directionSwitchPublishesSignalAndUnknownNewSideRiskInOneAtomicVersion() {
        try (var fixture = new RuntimeFixture()) {
            fixture.properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
            when(fixture.pool.listForUser(7L)).thenReturn(poolMembers("BTCUSDT"));
            fixture.service.registerCardStream(7L);
            Instant at = Instant.now();
            var current = publicSignalFixture(at.minusSeconds(1), AssetCardFeatureService.FEATURE_VERSION);
            installBundle(fixture.service, "BTCUSDT", metadataOnlyBundle("TEST_FIXTURE_MODEL", "TEST_FIXTURE_CALIBRATION", Set.of("BTCUSDT")));
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", current);
            var nextSignal = new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.SHORT, "VALID", null, .15, .84, "OPPORTUNITY", "SHORT", at);
            try (var lease = registry(fixture.service).acquire("BTCUSDT")) {
                ReflectionTestUtils.invokeMethod(fixture.service, "publishSignalAndRisk", current, nextSignal, current.risk(),
                        current.featureVersion(), current.modelVersion(), current.calibrationVersion(), current.thresholdVersion(), at, lease.bundle());
            }
            fixture.service.flushPublications(at);
            var next = fixture.lastSnapshot();
            assertThat(next.signal()).isEqualTo(nextSignal);
            assertThat(next.risk().overallLevel()).isNull();
            assertThat(next.risk().items()).hasSize(8).allMatch(item -> "UNKNOWN".equals(item.assessmentStatus()));
            assertThat(next.risk().riskBasisSide()).isEqualTo(AssetCardSnapshot.SignalSide.SHORT);
            assertThat(next.risk().matchesBasis(nextSignal)).isTrue();
            var events = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            verify(fixture.events, atLeastOnce()).publishToUser(eq(7L), events.capture());
            assertThat(events.getAllValues()).filteredOn(event -> !"ASSET_CARD_PRICE".equals(event.eventType()))
                    .allMatch(event -> event.snapshotVersion() == next.snapshotVersion());
            assertThat(events.getAllValues()).filteredOn(event -> "ASSET_CARD_SIGNAL".equals(event.eventType())).singleElement().satisfies(event -> {
                assertThat(event.payload()).containsEntry("signal", nextSignal).containsEntry("risk", next.risk());
                assertThat(event.payload()).containsEntry("thresholdVersion", "TEST_FIXTURE_THRESHOLDS");
                assertThat(event.payload()).containsEntry("riskVersion", "TEST_RISK");
            });
            verify(fixture.mapper).saveSnapshot(eq("BTCUSDT"), eq(current.snapshotVersion()), eq(next.snapshotVersion()), anyString(), any());
        }
    }

    @Test
    void persistedInferenceConflictRestoresTheWinningExactBarAndNeverPublishesTheLoser() throws Exception {
        Instant close = Instant.parse("2026-09-10T11:59:59.999Z");
        try (var fixture = new RuntimeFixture()) {
            fixture.freshMarket();
            when(fixture.market.bars(eq("BTCUSDT"), anyString(), any(), eq(24))).thenAnswer(call -> closedBars(call.getArgument(1), close));
            var frame = runtimeFrame(close);
            var winner = new AssetCardSignalService.State("BTCUSDT", null, null, 0, close,
                    identity(AssetCardProperties.ModelMode.SHADOW, null), null,
                    AssetCardSnapshot.Signal.unavailable("SHADOW", frame.signalAsOf()), null, "TEST_WINNING_PROCESS");
            when(fixture.mapper.selectInference(eq("BTCUSDT"), eq(close), any())).thenReturn(Optional.empty(), Optional.of(fixture.audit(frame, winner)));
            when(fixture.mapper.saveInference(eq("BTCUSDT"), eq(close), any(), anyString())).thenReturn(0);
            fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(3));
            assertThat(AssetCardServiceTest.<AssetCardSignalService.State>runtimeMap(fixture.service, "signalStates").get("BTCUSDT")).isEqualTo(winner);
            assertThat(fixture.lastSnapshot().signal()).isEqualTo(winner.signal());
            verify(fixture.mapper, times(2)).selectInference(eq("BTCUSDT"), eq(close), any());
            verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
            verifyNoInteractions(fixture.events);
        }
    }

    @Test
    void blockedSameSymbolInferenceDoesNotBlockItsIndependentPriceProjection() throws Exception {
        Instant close = Instant.parse("2026-09-10T11:59:59.999Z");
        try (var fixture = new RuntimeFixture()) {
            fixture.freshMarket();
            var entered = new java.util.concurrent.CountDownLatch(1);
            var release = new java.util.concurrent.CountDownLatch(1);
            when(fixture.market.bars(eq("BTCUSDT"), anyString(), any(), eq(24))).thenAnswer(call -> {
                entered.countDown();
                if (!release.await(3, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Price path was blocked by inference");
                return closedBars(call.getArgument(1), close);
            });
            var callers = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                var computing = callers.submit(() -> fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(1)));
                assertThat(entered.await(1, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                callers.submit(() -> fixture.service.flushPrices(close.plusSeconds(2))).get(1, java.util.concurrent.TimeUnit.SECONDS);
                assertThat(fixture.lastSnapshot().spotPrice()).isEqualByComparingTo("100");
                assertThat(fixture.lastSnapshot().latestPriceAt()).isEqualTo(close.plusSeconds(2));
                release.countDown(); computing.get(3, java.util.concurrent.TimeUnit.SECONDS);
            } finally { release.countDown(); callers.shutdownNow(); }
        }
    }

    @Test
    void synthetic128SymbolBoundaryProducesOnePrivateOutcomeEachWithBoundedRuntimeLatency() throws Exception {
        Instant close = Instant.parse("2026-09-10T11:59:59.999Z");
        try (var fixture = new RuntimeFixture()) {
            Set<String> symbols = java.util.stream.IntStream.range(0, 128).mapToObj(i -> "S" + i + "XUSDT")
                    .collect(java.util.stream.Collectors.toSet());
            when(fixture.market.subscribedSymbols()).thenReturn(symbols);
            var audits = new java.util.concurrent.ConcurrentHashMap<String, String>();
            var completionNanos = new java.util.concurrent.ConcurrentHashMap<String, Long>();
            var counters = new java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong>();
            var concurrent = new java.util.concurrent.atomic.AtomicInteger();
            var maximumConcurrent = new java.util.concurrent.atomic.AtomicInteger();
            var twoSymbolsInside = new java.util.concurrent.CountDownLatch(2);
            when(fixture.mapper.nextSnapshotVersion(anyString())).thenAnswer(call ->
                    counters.computeIfAbsent(call.getArgument(0), ignored -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet());
            when(fixture.mapper.saveInference(anyString(), eq(close), any(), anyString())).thenAnswer(call ->
                    audits.putIfAbsent(call.getArgument(0), call.getArgument(3)) == null ? 1 : 0);
            when(fixture.mapper.saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), nullable(Instant.class))).thenAnswer(call -> {
                completionNanos.putIfAbsent(call.getArgument(0), System.nanoTime()); return 1;
            });
            when(fixture.market.bars(anyString(), anyString(), any(), eq(24))).thenAnswer(call -> {
                int active = concurrent.incrementAndGet(); maximumConcurrent.accumulateAndGet(active, Math::max);
                try {
                    twoSymbolsInside.countDown();
                    if (!twoSymbolsInside.await(2, java.util.concurrent.TimeUnit.SECONDS)) throw new AssertionError("Global inference serialization detected");
                    return closedBars(call.getArgument(0), call.getArgument(1), close);
                } finally { concurrent.decrementAndGet(); }
            });
            var callers = java.util.concurrent.Executors.newFixedThreadPool(16);
            long start = System.nanoTime();
            try {
                var tasks = symbols.stream().map(symbol -> (java.util.concurrent.Callable<Void>) () -> {
                    fixture.service.inferClosedBar(symbol, close, close.plusSeconds(1)); return null;
                }).toList();
                for (var future : callers.invokeAll(tasks, 15, java.util.concurrent.TimeUnit.SECONDS)) {
                    assertThat(future.isCancelled()).isFalse(); future.get();
                }
            } finally { callers.shutdownNow(); }
            assertThat(maximumConcurrent.get()).isGreaterThan(1);
            assertThat(audits.keySet()).isEqualTo(symbols);
            assertThat(completionNanos.keySet()).isEqualTo(symbols);
            var latency = completionNanos.values().stream().map(value -> (value - start) / 1_000_000.0).sorted().toList();
            assertThat(latency.get((int) Math.ceil(latency.size() * .95) - 1)).as("Synthetic private runtime p95; NOT native-model or production readiness").isLessThanOrEqualTo(15_000);
            for (String encoded : audits.values()) {
                var audit = fixture.json.readTree(encoded);
                assertThat(audit.path("outcome").asText()).isEqualTo("COMPLETED");
                assertThat(audit.path("modelMode").asText()).isEqualTo("SHADOW");
                assertThat(audit.path("state").path("auditPrediction").path("available").asBoolean()).isFalse();
            }
            for (String symbol : symbols) fixture.service.inferClosedBar(symbol, close, close.plusSeconds(3));
            verify(fixture.mapper, times(128)).saveInference(anyString(), eq(close), any(), anyString());
            verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
            verifyNoInteractions(fixture.events, fixture.pool);
        }
    }

    @Test
    void actualWorkerQueueRecordsAll128ExpiredBoundariesInsteadOfSilentlyDroppingThem() throws Exception {
        Instant close = Instant.ofEpochMilli(Math.floorDiv(Instant.now().toEpochMilli(), 300_000) * 300_000 - 300_001);
        try (var fixture = new RuntimeFixture()) {
            fixture.enableLocalWorkers();
            Set<String> symbols = java.util.stream.IntStream.range(0, 128).mapToObj(i -> "S" + i + "XUSDT")
                    .collect(java.util.stream.Collectors.toSet());
            when(fixture.market.subscribedSymbols()).thenReturn(symbols);
            var outcomes = new java.util.concurrent.ConcurrentHashMap<String, String>();
            var completed = new java.util.concurrent.CountDownLatch(128);
            when(fixture.mapper.saveInference(anyString(), eq(close), any(), anyString())).thenAnswer(call -> {
                if (outcomes.putIfAbsent(call.getArgument(0), call.getArgument(3)) == null) { completed.countDown(); return 1; }
                return 0;
            });
            for (String symbol : symbols) fixture.service.onMarketUpdate(new AssetCardMarketDataService.MarketUpdate(symbol, "BAR", close, "5m"));
            assertThat(completed.await(15, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(outcomes).hasSize(128);
            for (String encoded : outcomes.values()) assertThat(fixture.json.readTree(encoded).path("outcome").asText()).isEqualTo("TIMED_OUT");
            verify(fixture.market, never()).bars(anyString(), anyString(), any(), anyInt());
            verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
            verifyNoInteractions(fixture.events, fixture.pool);
        }
    }

    @Test
    void queuedBarDropPersistsExplicitIdentityWithoutRunningInference() throws Exception {
        Instant close = Instant.parse("2026-09-10T11:59:59.999Z");
        try (var fixture = new RuntimeFixture()) {
            fixture.enableLocalWorkers();
            fixture.service.onMarketUpdate(new AssetCardMarketDataService.MarketUpdate("BTCUSDT", "BAR_DROPPED", close, "5m"));
            var encoded = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(fixture.mapper, timeout(2000)).saveInference(eq("BTCUSDT"), eq(close), any(), encoded.capture());
            assertThat(fixture.json.readTree(encoded.getValue()).path("outcome").asText()).contains("DROPPED");
            verify(fixture.market, never()).bars(anyString(), anyString(), any(), anyInt());
            verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
            verifyNoInteractions(fixture.events, fixture.pool);
        }
    }

    private static AssetCardSnapshot publicSignalFixture(Instant at, String featureVersion) {
        return publicSignalFixture(at, featureVersion, "TEST_FIXTURE_THRESHOLDS", "TEST_RISK");
    }

    private static AssetCardSnapshot publicSignalFixture(Instant at, String featureVersion, String thresholdVersion, String riskVersion) {
        var signal = validLong(at.minusSeconds(30));
        return new AssetCardSnapshot("BTCUSDT", "TEST_FIXTURE", BigDecimal.valueOf(100), at,
                signal, new AssetCardSnapshot.Risk("HIGH", List.of(new AssetCardSnapshot.RiskItem(
                        "CROWDING", "ASSESSED", "HIGH", "TEST_FIXTURE", "TEST_FIXTURE", at, "TEST_FIXTURE")), at,
                        AssetCardSnapshot.SignalSide.LONG, signal.direction(), signal.signalAsOf(), at, riskVersion),
                new AssetCardSnapshot.Health("HEALTHY", null, at), at.minusSeconds(30), 10,
                featureVersion, "TEST_FIXTURE_MODEL", "TEST_FIXTURE_CALIBRATION", thresholdVersion);
    }

    @Test
    void duplicateClosedBarAfterHotReplacementNeverRelabelsAnOldProbability() {
        for (String featureVersion : List.of(AssetCardFeatureService.FEATURE_VERSION, "TEST_OLD_FEATURE")) try (var fixture = new RuntimeFixture()) {
            fixture.properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
            var at = Instant.now(); var closed = at.minusSeconds(1);
            var stored = publicSignalFixture(closed, featureVersion);
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
            var identity = new AssetCardSignalService.ModelIdentity(stored.featureVersion(), stored.modelVersion(),
                    stored.calibrationVersion(), stored.thresholdVersion(), AssetCardProperties.ModelMode.ACTIVE, true, true);
            runtimeMap(fixture.service, "signalStates").put("BTCUSDT", new AssetCardSignalService.State("BTCUSDT",
                    stored.signal().direction(), null, 0, closed, identity, null, stored.signal(), null, null));
            installBundle(fixture.service, "BTCUSDT", metadataOnlyBundle("TEST_NEW_MODEL", "TEST_NEW_CAL", Set.of("BTCUSDT")));
            fixture.service.inferClosedBar("BTCUSDT", closed, at);
            assertThat(fixture.lastSnapshot().featureVersion()).isEqualTo(stored.featureVersion());
            assertThat(fixture.lastSnapshot().modelVersion()).isEqualTo(stored.modelVersion());
            assertThat(fixture.lastSnapshot().thresholdVersion()).isEqualTo(stored.thresholdVersion());
            assertThat(fixture.lastSnapshot().signal()).isEqualTo(stored.signal()); // immutable private observation preserved
            var visible = fixture.service.snapshot("BTCUSDT", "Bitcoin");
            assertThat(visible.signal().direction()).isNull(); assertThat(visible.signal().calibratedConfidence()).isNull();
            assertThat(visible.health().status()).isEqualTo("MODEL_UNAVAILABLE");
            verify(fixture.mapper, never()).saveInference(anyString(), any(), any(), anyString());
            verify(fixture.mapper, never()).saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), any());
        }
    }

    @Test
    void configuredAssetReloadFailureRevokesOnlyItsPublicCardWithoutCrossAssetFallback() throws Exception {
        try (var fixture = new RuntimeFixture()) {
            fixture.properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
            var constructor = AssetCardModelBundle.Registry.class.getDeclaredConstructor(java.util.function.Function.class);
            constructor.setAccessible(true);
            var configured = constructor.newInstance((java.util.function.Function<AssetCardModelBundle.Source, AssetCardModelBundle>) source ->
                    source.expectedManifestSha256().equals("0".repeat(64)) ? metadataOnlyBundle("TEST_FIXTURE_MODEL", "TEST_FIXTURE_CALIBRATION",
                            Set.of(source.path().getFileName().toString())) : AssetCardModelBundle.unavailable("TEST_BAD_CHECKSUM"));
            registry(fixture.service).close(); ReflectionTestUtils.setField(fixture.service, "modelRegistry", configured);
            var btc = new AssetCardModelBundle.Source(java.nio.file.Path.of("BTCUSDT"), "0".repeat(64));
            var eth = new AssetCardModelBundle.Source(java.nio.file.Path.of("ETHUSDT"), "0".repeat(64));
            fixture.properties.setModelBundles(Map.of("BTCUSDT", btc, "ETHUSDT", eth));
            fixture.service.reconcileModelsSafely();
            var stored = publicSignalFixture(Instant.now().minusSeconds(1), AssetCardFeatureService.FEATURE_VERSION);
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
            var other = new AssetCardSnapshot("ETHUSDT", "Ethereum", stored.spotPrice(), stored.latestPriceAt(), stored.signal(), stored.risk(),
                    stored.health(), stored.cardAsOf(), stored.snapshotVersion(), stored.featureVersion(), stored.modelVersion(),
                    stored.calibrationVersion(), stored.thresholdVersion(), stored.priceTradeId());
            runtimeMap(fixture.service, "snapshots").put("ETHUSDT", other);
            assertThat(fixture.service.snapshot("BTCUSDT", "Bitcoin").signal()).isEqualTo(stored.signal());
            assertThat(fixture.service.snapshot("ETHUSDT", "Ethereum").signal()).isEqualTo(other.signal());
            fixture.properties.setModelBundles(Map.of("BTCUSDT", new AssetCardModelBundle.Source(btc.path(), "f".repeat(64)), "ETHUSDT", eth));
            fixture.service.reconcileModelsSafely();
            var rejected = fixture.service.snapshot("BTCUSDT", "Bitcoin");
            assertThat(rejected.signal().calibratedConfidence()).isNull(); assertThat(rejected.signal().direction()).isNull();
            assertThat(rejected.health().status()).isEqualTo("MODEL_UNAVAILABLE");
            assertThat(fixture.service.usesCardSignalDisplay("BTCUSDT")).isTrue();
            assertThat(fixture.service.snapshot("ETHUSDT", "Ethereum").signal()).isEqualTo(other.signal());
            verifyNoInteractions(fixture.mapper, fixture.events, fixture.pool);
            verify(fixture.market, times(2)).quote(eq("BTCUSDT"), any());
            verify(fixture.market, times(2)).quote(eq("ETHUSDT"), any());
            verifyNoMoreInteractions(fixture.market);
        }
    }

    private static AssetCardModelBundle.Registry registry(AssetCardService service) {
        return (AssetCardModelBundle.Registry) ReflectionTestUtils.getField(service, "modelRegistry");
    }
    private static void installBundle(AssetCardService service, String symbol, AssetCardModelBundle bundle) {
        try {
            var constructor = AssetCardModelBundle.Registry.class.getDeclaredConstructor(java.util.function.Function.class);
            constructor.setAccessible(true);
            var replacement = constructor.newInstance((java.util.function.Function<AssetCardModelBundle.Source, AssetCardModelBundle>) ignored -> bundle);
            replacement.reconcile(Map.of(symbol, new AssetCardModelBundle.Source(java.nio.file.Path.of("TEST_ONLY_NOT_LOADED"), "0".repeat(64))));
            registry(service).close(); ReflectionTestUtils.setField(service, "modelRegistry", replacement);
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Test-only registry signature changed", failure); }
    }

    /** Metadata seam only. No native loading/inference or production model gate is bypassed at runtime. */
    private static AssetCardModelBundle metadataOnlyBundle(String modelVersion, String calibrationVersion, Set<String> assets) {
        try {
            var constructor = AssetCardModelBundle.class.getDeclaredConstructor(ml.dmlc.xgboost4j.java.Booster.class,
                    ml.dmlc.xgboost4j.java.Booster.class, AssetCardBetaCalibration.Parameters.class,
                    AssetCardBetaCalibration.Parameters.class, String.class, String.class, String.class,
                    AssetCardModelBundle.Thresholds.class, Map.class, Set.class, String.class);
            constructor.setAccessible(true);
            var calibration = new AssetCardBetaCalibration.Parameters(1, 1, 0, 1e-12);
            var bundle = constructor.newInstance(mock(ml.dmlc.xgboost4j.java.Booster.class), mock(ml.dmlc.xgboost4j.java.Booster.class),
                    calibration, calibration, modelVersion, calibrationVersion, "TEST_FIXTURE_THRESHOLDS", null, Map.of(), assets, null);
            int count = AssetCardFeatureService.FEATURE_NAMES.size();
            ReflectionTestUtils.setField(bundle, "lifecycle", new AssetCardModelBundle.Lifecycle("TEST_FIXTURE_DATA", "TEST_RISK",
                    Instant.EPOCH, Instant.EPOCH, Instant.parse("2100-01-01T00:00:00Z"), Set.of("0".repeat(count), "1".repeat(count)),
                    Collections.nCopies(count, -1_000_000.0), Collections.nCopies(count, 1_000_000.0), .5));
            return bundle;
        } catch (ReflectiveOperationException failure) { throw new AssertionError("Test-only immutable bundle signature changed", failure); }
    }

    private static AssetCardSnapshot.Signal validLong(Instant at) {
        return new AssetCardSnapshot.Signal(AssetCardSnapshot.Direction.LONG, "VALID", null, .81, .20,
                "OPPORTUNITY", "LONG", at);
    }

    private static AssetCardSignalService.ModelIdentity identity(AssetCardProperties.ModelMode mode, String model) {
        return new AssetCardSignalService.ModelIdentity(AssetCardFeatureService.FEATURE_VERSION, model, null, null,
                mode, mode == AssetCardProperties.ModelMode.ACTIVE, false);
    }

    private static AssetCardFeatureService.Frame runtimeFrame(Instant close) {
        Instant available = close.plusSeconds(1);
        return new AssetCardFeatureService.Frame("BTCUSDT", close, available, available,
                AssetCardFeatureService.FEATURE_VERSION, AssetCardFeatureService.FEATURE_NAMES,
                Collections.nCopies(AssetCardFeatureService.FEATURE_NAMES.size(), 1.0), true, List.of(),
                "OPPORTUNITY", "LONG", 2.0, 90.0, 110.0, Map.of());
    }

    @SuppressWarnings("unchecked")
    private static <T> Map<String, T> runtimeMap(AssetCardService service, String name) {
        return (Map<String, T>) ReflectionTestUtils.getField(service, name);
    }

    private static List<AssetCardMarketDataService.SpotBar> closedBars(String interval, Instant lastClose) {
        return closedBars("BTCUSDT", interval, lastClose);
    }

    private static List<AssetCardMarketDataService.SpotBar> closedBars(String symbol, String interval, Instant lastClose) {
        long seconds = switch (interval) { case "5m" -> 300; case "15m" -> 900; case "1h" -> 3600; case "4h" -> 14400;
            default -> throw new IllegalArgumentException("Unexpected fixture interval"); };
        List<AssetCardMarketDataService.SpotBar> bars = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Instant end = lastClose.minusSeconds((23L - i) * seconds);
            BigDecimal price = BigDecimal.valueOf(100 + i * .1);
            bars.add(new AssetCardMarketDataService.SpotBar(symbol, interval, end.plusMillis(1).minusSeconds(seconds), end,
                    price, price.add(BigDecimal.ONE), price.subtract(BigDecimal.ONE), price, BigDecimal.TEN,
                    BigDecimal.valueOf(6), 10L, end.plusMillis(1)));
        }
        return bars;
    }

    /** No Spring/start lifecycle, datasource, native model or provider client; tests may explicitly enqueue local workers. */
    private static final class RuntimeFixture implements AutoCloseable {
        private final AssetCardProperties properties = new AssetCardProperties();
        private final AssetCardMarketDataService market = mock(AssetCardMarketDataService.class);
        private final org.example.trademodel.mapper.AssetCardMapper mapper = mock(org.example.trademodel.mapper.AssetCardMapper.class);
        private final org.example.trademodel.v41.DashboardLiveEventService events = mock(org.example.trademodel.v41.DashboardLiveEventService.class);
        private final org.example.trademodel.service.watchlistsource.AssetPoolService pool = mock(org.example.trademodel.service.watchlistsource.AssetPoolService.class);
        private final com.fasterxml.jackson.databind.ObjectMapper json = new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
        private final AssetCardService service;
        private RuntimeFixture() {
            properties.setEnabled(true); // Manual pure methods only; start() is never invoked.
            service = new AssetCardService(properties, market, mapper, pool, events, json);
            when(market.subscribedSymbols()).thenReturn(Set.of("BTCUSDT"));
            var counter = new java.util.concurrent.atomic.AtomicLong(10);
            when(mapper.nextSnapshotVersion("BTCUSDT")).thenAnswer(ignored -> counter.incrementAndGet());
            when(mapper.saveSnapshot(anyString(), anyLong(), anyLong(), anyString(), nullable(Instant.class))).thenReturn(1);
            when(mapper.saveInference(anyString(), any(), any(), anyString())).thenReturn(1);
            when(mapper.selectInference(anyString(), any(), any())).thenReturn(Optional.empty());
        }
        private void freshMarket() {
            when(market.quote(eq("BTCUSDT"), any())).thenAnswer(invocation -> {
                Instant at = invocation.getArgument(1);
                return Optional.of(new AssetCardMarketDataService.SpotQuote("BTCUSDT", BigDecimal.valueOf(100), BigDecimal.ONE, 1, at, at));
            });
            when(market.book(eq("BTCUSDT"), any())).thenAnswer(invocation -> {
                Instant at = invocation.getArgument(1);
                return Optional.of(new AssetCardMarketDataService.SpotBook("BTCUSDT",
                        List.of(new AssetCardMarketDataService.Level(new BigDecimal("99.99"), BigDecimal.TEN)),
                        List.of(new AssetCardMarketDataService.Level(new BigDecimal("100.01"), BigDecimal.TEN)),
                        1, at, at, "EXCHANGE_EVENT", BigDecimal.valueOf(99), BigDecimal.valueOf(101)));
            });
        }
        private void enableLocalWorkers() {
            ReflectionTestUtils.setField(service, "started", true);
            ReflectionTestUtils.setField(service, "writerReady", true); // Isolated mock-permission fixture, never runtime readiness evidence.
        }
        private void seed(AssetCardSnapshot.Signal signal, Instant priceAt) {
            var snapshot = new AssetCardSnapshot("BTCUSDT", "Bitcoin", BigDecimal.valueOf(100), priceAt, signal,
                    AssetCardSnapshot.Risk.unknownFor(signal, AssetCardRiskService.RULE_VERSION, "Fixture has no calibrated risk distributions"),
                    new AssetCardSnapshot.Health("HEALTHY", null, priceAt), signal.signalAsOf(), 10,
                    AssetCardFeatureService.FEATURE_VERSION, null, null);
            runtimeMap(service, "snapshots").put("BTCUSDT", snapshot);
        }
        private AssetCardSnapshot lastSnapshot() { return AssetCardServiceTest.<AssetCardSnapshot>runtimeMap(service, "snapshots").get("BTCUSDT"); }
        private org.example.trademodel.mapper.AssetCardMapper.TypedHistory audit(AssetCardFeatureService.Frame frame,
                                                                                 AssetCardSignalService.State state) throws Exception {
            String encoded = json.writeValueAsString(Map.of("frame", frame, "signalFrame", frame, "state", state,
                    "modelMode", properties.getModelMode(), "dataKind", "LIVE_OBSERVED_CARD_INPUTS"));
            return new org.example.trademodel.mapper.AssetCardMapper.TypedHistory("BTCUSDT",
                    org.example.trademodel.mapper.AssetCardMapper.HistoryKind.INFERENCE, "5m:" + frame.closed5mAt(),
                    frame.closed5mAt(), frame.availableAt(), encoded);
        }
        @Override public void close() { service.close(); }
    }
}
