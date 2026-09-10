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
        org.mockito.Mockito.when(mapper.saveSnapshot(org.mockito.ArgumentMatchers.eq("BTCUSDT"),org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.anyString(),org.mockito.ArgumentMatchers.isNull())).thenReturn(1);
        var service = new AssetCardService(properties,market,mapper,pool,events,new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules());
        try {
            service.flushPrices(at);
            var captor = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            org.mockito.Mockito.verify(events,org.mockito.Mockito.atLeastOnce()).publish(captor.capture());
            assertThat(captor.getAllValues()).allMatch(e -> java.util.Set.of("ASSET_CARD_PRICE","ASSET_CARD_HEALTH").contains(e.eventType()));
            var price = captor.getAllValues().stream().filter(e -> e.eventType().equals("ASSET_CARD_PRICE")).findFirst().orElseThrow();
            assertThat(price.payload()).containsEntry("spotPrice",new java.math.BigDecimal("100"))
                    .containsEntry("latestPriceAt",at).doesNotContainKeys("cardAsOf","signal","risk","modelVersion");
            org.mockito.Mockito.verify(mapper,org.mockito.Mockito.never()).saveFeatureHistory(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.any());
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
            when(fixture.mapper.selectFeatureHistory(eq("BTCUSDT"), any(), any(), any(), anyInt()))
                    .thenReturn(List.of(fixture.audit(frame, state)));
            ReflectionTestUtils.invokeMethod(fixture.service, "recoverRuntimeState", "BTCUSDT", at);
            assertThat(AssetCardServiceTest.<AssetCardSignalService.State>runtimeMap(fixture.service, "signalStates").get("BTCUSDT")).isEqualTo(state);
            assertThat(AssetCardServiceTest.<AssetCardFeatureService.Frame>runtimeMap(fixture.service, "featureFrames").get("BTCUSDT")).isEqualTo(frame);
            assertThat(AssetCardServiceTest.<AssetCardFeatureService.Frame>runtimeMap(fixture.service, "signalFrames").get("BTCUSDT")).isEqualTo(frame);
            assertThat(fixture.lastSnapshot().signal().status()).isEqualTo("SHADOW");
            assertThat(fixture.lastSnapshot().signal().calibratedConfidence()).isNull();
            verify(fixture.mapper, never()).saveFeatureHistory(any(), any(), any(), any());
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
                when(fixture.mapper.selectFeatureHistory(eq("BTCUSDT"), any(), any(), any(), anyInt()))
                        .thenReturn(List.of(fixture.audit(frame, state)));
                ReflectionTestUtils.invokeMethod(fixture.service, "recoverRuntimeState", "BTCUSDT", at);
                assertThat(runtimeMap(fixture.service, "signalStates")).doesNotContainKey("BTCUSDT");
                assertThat(runtimeMap(fixture.service, "featureFrames")).doesNotContainKey("BTCUSDT");
                assertThat(runtimeMap(fixture.service, "signalFrames")).doesNotContainKey("BTCUSDT");
                verify(fixture.mapper, never()).saveFeatureHistory(any(), any(), any(), any());
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
            assertThat(runtimeMap(fixture.service, "featureFrames")).doesNotContainKey("BTCUSDT");
            when(fixture.market.bars(eq("BTCUSDT"), anyString(), any(), eq(24)))
                    .thenAnswer(invocation -> closedBars(invocation.getArgument(1), close));
            fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(3));
            var payload = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(fixture.mapper, times(1)).saveFeatureHistory(eq("BTCUSDT"), any(), any(), payload.capture());
            var audit = fixture.json.readTree(payload.getValue());
            var hours = audit.path("rawFrame").path("bars").path("1h");
            assertThat(hours.size()).isEqualTo(24);
            assertThat(fixture.json.treeToValue(hours.get(23).get("closeTime"), Instant.class)).isEqualTo(close);
            assertThat(fixture.json.treeToValue(audit.path("frame").get("closed5mAt"), Instant.class)).isEqualTo(close);
            fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(4));
            verify(fixture.mapper, times(1)).saveFeatureHistory(eq("BTCUSDT"), any(), any(), any());
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
            fixture.freshMarket();
            when(fixture.market.bars(eq("BTCUSDT"), anyString(), any(), eq(24)))
                    .thenAnswer(invocation -> closedBars(invocation.getArgument(1), close));
            fixture.service.inferClosedBar("BTCUSDT", close, close.plusSeconds(3));

            var captured = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(fixture.mapper).saveFeatureHistory(eq("BTCUSDT"), eq(close.plusSeconds(3)), eq(close.plusSeconds(3)), captured.capture());
            var audit = fixture.json.readTree(captured.getValue());
            assertThat(audit.path("modelMode").asText()).isEqualTo("SHADOW");
            assertThat(audit.path("rawFrame").path("bars").path("5m").size()).isEqualTo(24);
            assertThat(audit.path("state").path("auditPrediction").isObject()).isTrue();
            assertThat(audit.path("state").path("auditPrediction").path("available").asBoolean()).isFalse();
            assertThat(audit.path("state").path("auditPrediction").path("reasons").toString()).contains("MODEL_NOT_CONFIGURED");
            assertThat(fixture.lastSnapshot().signal().status()).isEqualTo("SHADOW");
            verify(fixture.mapper).saveSnapshot(eq("BTCUSDT"), anyLong(), anyString(), nullable(Instant.class));
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
            fixture.freshMarket();
            fixture.service.flushPrices(at.plusSeconds(1));

            var captured = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(fixture.mapper).saveSnapshot(eq("BTCUSDT"), anyLong(), captured.capture(), nullable(Instant.class));
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
            fixture.freshMarket();
            when(fixture.market.subscribedSymbols()).thenReturn(Set.of("BTCUSDT", "ETHUSDT"));
            when(fixture.mapper.nextSnapshotVersion("ETHUSDT")).thenReturn(20L);
            when(fixture.market.quote("ETHUSDT", at)).thenReturn(Optional.of(new AssetCardMarketDataService.SpotQuote(
                    "ETHUSDT", BigDecimal.valueOf(100), BigDecimal.ONE, 1, at, at)));
            fixture.service.flushPrices(at);

            verify(fixture.mapper).saveSnapshot(eq("BTCUSDT"), anyLong(), anyString(), nullable(Instant.class));
            verify(fixture.mapper).saveSnapshot(eq("ETHUSDT"), anyLong(), anyString(), nullable(Instant.class));
            var published = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            verify(fixture.events, atLeastOnce()).publish(published.capture());
            assertThat(published.getAllValues()).allSatisfy(event -> assertThat(event.symbol()).isEqualTo("BTCUSDT"));
        }
    }

    @Test
    void storedAndCachedValidSignalsRequireCurrentBundleAndExactIdentityOnEveryPublicRead() throws Exception {
        for (var mode : List.of(AssetCardProperties.ModelMode.ACTIVE, AssetCardProperties.ModelMode.CANARY)) {
            for (boolean cached : List.of(false, true)) {
                for (String mismatch : List.of("missing", "closed", "feature", "model", "calibration", "asset", "none")) {
                    try (var fixture = new RuntimeFixture()) {
                        fixture.properties.setModelMode(mode);
                        fixture.properties.setCanarySymbols(Set.of("BTCUSDT"));
                        Instant at = Instant.now();
                        var stored = publicSignalFixture(at, "feature".equals(mismatch) ? "OLD_FEATURE" : AssetCardFeatureService.FEATURE_VERSION);
                        if (!"missing".equals(mismatch)) {
                            var bundle = metadataOnlyBundle("model".equals(mismatch) ? "NEW_MODEL" : "TEST_FIXTURE_MODEL",
                                    "calibration".equals(mismatch) ? "NEW_CALIBRATION" : "TEST_FIXTURE_CALIBRATION",
                                    "asset".equals(mismatch) ? Set.of("ETHUSDT") : Set.of("BTCUSDT"));
                            if ("closed".equals(mismatch)) ReflectionTestUtils.setField(bundle, "closed", true);
                            ReflectionTestUtils.setField(fixture.service, "model", bundle);
                        }
                        if (cached) runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
                        else when(fixture.mapper.selectSnapshotJson("BTCUSDT")).thenReturn(fixture.json.writeValueAsString(stored));
                        when(fixture.pool.listForUser(7L)).thenReturn(List.of(new org.example.trademodel.dto.assetpool.AssetPoolAssetDTO(
                                1L, "BTCUSDT", "Bitcoin", "SPOT", "USDT", true, 1, "USER")));

                        var visible = fixture.service.snapshotsForUser(7L, List.of("BTCUSDT")).get(0);

                        assertThat(fixture.service.usesCardSignalDisplay("BTCUSDT")).isTrue();
                        assertThat(visible.spotPrice()).isEqualTo(stored.spotPrice());
                        assertThat(visible.latestPriceAt()).isEqualTo(stored.latestPriceAt());
                        assertThat(visible.risk()).isEqualTo(stored.risk());
                        assertThat(visible.cardAsOf()).isEqualTo(stored.cardAsOf());
                        assertThat(visible.snapshotVersion()).isEqualTo(stored.snapshotVersion());
                        if ("none".equals(mismatch)) assertThat(visible.signal()).isEqualTo(stored.signal());
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
                        verify(fixture.mapper, never()).saveSnapshot(anyString(), anyLong(), anyString(), nullable(Instant.class));
                        verify(fixture.mapper, never()).saveFeatureHistory(anyString(), any(), any(), anyString());
                        verifyNoInteractions(fixture.market, fixture.events);
                    }
                }
            }
        }
    }

    @Test
    void priceRefreshPersistsPrivateOldSignalButPublicEventsCannotLeakRevokedModelConfidence() {
        try (var fixture = new RuntimeFixture()) {
            fixture.properties.setModelMode(AssetCardProperties.ModelMode.ACTIVE);
            Instant at = Instant.now();
            var stored = publicSignalFixture(at.minusSeconds(1), AssetCardFeatureService.FEATURE_VERSION);
            runtimeMap(fixture.service, "snapshots").put("BTCUSDT", stored);
            fixture.freshMarket();
            fixture.service.flushPrices(at);

            // Rollout visibility is not a mutation of the original private audit/snapshot chain.
            assertThat(fixture.lastSnapshot().signal().calibratedConfidence()).isEqualTo(81);
            var captured = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            verify(fixture.events, atLeastOnce()).publish(captured.capture());
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
            var all = org.mockito.ArgumentCaptor.forClass(org.example.trademodel.v41.DashboardLiveEvent.class);
            verify(fixture.events, atLeastOnce()).publish(all.capture());
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

    private static AssetCardSnapshot publicSignalFixture(Instant at, String featureVersion) {
        return new AssetCardSnapshot("BTCUSDT", "TEST_FIXTURE", BigDecimal.valueOf(100), at,
                validLong(at.minusSeconds(30)), new AssetCardSnapshot.Risk("HIGH", List.of(new AssetCardSnapshot.RiskItem(
                        "CROWDING", "ASSESSED", "HIGH", "TEST_FIXTURE", "TEST_FIXTURE", at, "TEST_FIXTURE")), at),
                new AssetCardSnapshot.Health("HEALTHY", null, at), at.minusSeconds(30), 10,
                featureVersion, "TEST_FIXTURE_MODEL", "TEST_FIXTURE_CALIBRATION");
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
            return constructor.newInstance(mock(ml.dmlc.xgboost4j.java.Booster.class), mock(ml.dmlc.xgboost4j.java.Booster.class),
                    calibration, calibration, modelVersion, calibrationVersion, "TEST_FIXTURE_THRESHOLDS", null, Map.of(), assets, null);
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
        long seconds = switch (interval) { case "5m" -> 300; case "15m" -> 900; case "1h" -> 3600; case "4h" -> 14400;
            default -> throw new IllegalArgumentException("Unexpected fixture interval"); };
        List<AssetCardMarketDataService.SpotBar> bars = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Instant end = lastClose.minusSeconds((23L - i) * seconds);
            BigDecimal price = BigDecimal.valueOf(100 + i * .1);
            bars.add(new AssetCardMarketDataService.SpotBar("BTCUSDT", interval, end.plusMillis(1).minusSeconds(seconds), end,
                    price, price.add(BigDecimal.ONE), price.subtract(BigDecimal.ONE), price, BigDecimal.TEN,
                    BigDecimal.valueOf(6), 10L, end.plusMillis(1)));
        }
        return bars;
    }

    /** No Spring context, datasource, native model, provider client or scheduled lifecycle is started. */
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
            when(mapper.saveSnapshot(anyString(), anyLong(), anyString(), nullable(Instant.class))).thenReturn(1);
            when(mapper.saveFeatureHistory(anyString(), any(), any(), anyString())).thenReturn(1);
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
        private void seed(AssetCardSnapshot.Signal signal, Instant priceAt) {
            var snapshot = new AssetCardSnapshot("BTCUSDT", "Bitcoin", BigDecimal.valueOf(100), priceAt, signal,
                    AssetCardSnapshot.Risk.unknown("Fixture has no calibrated risk distributions"),
                    new AssetCardSnapshot.Health("HEALTHY", null, priceAt), signal.signalAsOf(), 10,
                    AssetCardFeatureService.FEATURE_VERSION, null, null);
            runtimeMap(service, "snapshots").put("BTCUSDT", snapshot);
        }
        private AssetCardSnapshot lastSnapshot() { return AssetCardServiceTest.<AssetCardSnapshot>runtimeMap(service, "snapshots").get("BTCUSDT"); }
        private org.example.trademodel.mapper.AssetCardMapper.FeatureHistory audit(AssetCardFeatureService.Frame frame,
                                                                                 AssetCardSignalService.State state) throws Exception {
            String encoded = json.writeValueAsString(Map.of("frame", frame, "signalFrame", frame, "state", state,
                    "modelMode", properties.getModelMode(), "dataKind", "LIVE_OBSERVED_CARD_INPUTS"));
            return new org.example.trademodel.mapper.AssetCardMapper.FeatureHistory("BTCUSDT", frame.signalAsOf(), frame.availableAt(), encoded);
        }
        @Override public void close() { service.close(); }
    }
}
