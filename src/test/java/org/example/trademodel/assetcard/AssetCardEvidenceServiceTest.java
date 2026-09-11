package org.example.trademodel.assetcard;

import org.example.trademodel.providercall.*;
import org.example.trademodel.providercall.coinglass.*;
import org.example.trademodel.mapper.MacroEventMapper;
import org.example.trademodel.mapper.NewsEventMapper;
import org.example.trademodel.entity.ExternalContextEventDO;
import org.example.trademodel.entity.MacroEventDO;
import org.example.trademodel.entity.NewsEventDO;
import org.example.trademodel.providercall.instrument.CanonicalInstrumentId;
import org.example.trademodel.providercall.instrument.ContractType;
import org.example.trademodel.providercall.instrument.MarketType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@org.junit.jupiter.api.Tag("core-regression")
class AssetCardEvidenceServiceTest {
    private static final Instant AT = Instant.parse("2026-09-10T12:00:00Z");
    private static final String RATIO_FIELD = "CG_V4_GLOBAL_ACCOUNT_LONG_SHORT_RATIO:data.latest.global_account_long_short_ratio";

    @Test
    void actualAdapterShapesDoNotRelabelAggregateUsdOrUnknownFundingAsBinanceUsdt() throws Exception {
        var f = fixture();
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var validator = new CoinGlassV4ResponseValidator();
        var mapped = new CoinGlassSymbolMapper().map("BTCUSDT");
        var fetched = AT.minusSeconds(1);
        var oi = validator.openInterest(json.readTree("[{\"symbol\":\"BTC\",\"exchange\":\"All\",\"open_interest_usd\":123456,\"open_interest_change_percent_1h\":2}]"), mapped, fetched);
        var funding = validator.funding(json.readTree("[{\"time\":" + AT.minusSeconds(60).toEpochMilli() + ",\"close\":-0.01}]"), mapped, fetched);
        var rows = json.createArrayNode();
        for (int minute = 1; minute <= 5; minute++) rows.addObject().put("time", AT.minusSeconds(minute * 60L).toEpochMilli())
                .put("aggregated_long_liquidation_usd", 10).put("aggregated_short_liquidation_usd", 4);
        var liquidation = validator.liquidation(rows, mapped, fetched);
        var ratio = validator.longShort(json.readTree("[{\"time\":" + AT.minusSeconds(60).toEpochMilli() + ",\"global_account_long_short_ratio\":1.25}]"), mapped, fetched, "Binance");
        when(f.oi.peek(anyString(), any(), any(), anyString())).thenReturn(result(oi.payload(), ProviderDatasetType.COINGLASS_OPEN_INTEREST, oi.providerDataTime(), fetched));
        when(f.funding.peek(anyString(), any(), any(), anyString())).thenReturn(result(funding.payload(), ProviderDatasetType.COINGLASS_FUNDING, funding.providerDataTime(), fetched));
        when(f.liquidation.peek(anyString(), any(), any(), anyString())).thenReturn(result(liquidation.payload(), ProviderDatasetType.COINGLASS_LIQUIDATION, liquidation.providerDataTime(), fetched));
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(ratio.payload(), ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, ratio.providerDataTime(), fetched));
        var read = f.service.read("BTCUSDT", AT);
        assertThat(read.facts()).containsOnlyKeys("longShortRatio");
        assertThat(read.facts().get("longShortRatio").unit()).isEqualTo("RATIO");
        assertThat(read.facts().get("longShortRatio").value()).isEqualTo(1.25);
        assertThat(read.missingReasons().get("openInterest")).contains("跨交易所", "USD", "实际可用时间");
        assertThat(read.missingReasons().get("fundingRate")).contains("加权", "单位");
        assertThat(read.missingReasons().get("longLiquidation")).contains("5分钟", "USD", "交易所");
        assertThat(read.observations(AT)).containsOnlyKeys("longShortRatio");
        verify(f.oi, never()).get(anyString(), any(), any(), anyString());
        verify(f.funding, never()).get(anyString(), any(), any(), anyString());
        verify(f.ratio, never()).get(anyString(), any(), any(), anyString());
        verify(f.liquidation, never()).get(anyString(), any(), any(), anyString());
    }

    @Test
    void ratioNeedsRealFieldAndExchangeNotOnlyRegistryIdentity() {
        for (String ratioSource : List.of("OKX_GLOBAL_ACCOUNT_RATIO", "BINANCE", "UNKNOWN")) {
            var f = fixture();
            when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    new CoinGlassLongShortSnapshot("BTCUSDT", BigDecimal.ONE, ratioSource, AT.minusSeconds(2), Map.of("longShortRatio", RATIO_FIELD)),
                    ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, AT.minusSeconds(2), AT));
            assertThat(f.service.read("BTCUSDT", AT).facts()).isEmpty();
        }
        for (var invalidRatio : List.of(BigDecimal.ZERO, BigDecimal.ONE.negate())) {
            var invalid = fixture();
            when(invalid.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    new CoinGlassLongShortSnapshot("BTCUSDT", invalidRatio, "BINANCE_GLOBAL_ACCOUNT_RATIO", AT.minusSeconds(2), Map.of("longShortRatio", RATIO_FIELD)),
                    ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, AT.minusSeconds(2), AT));
            assertThat(invalid.service.read("BTCUSDT", AT).facts()).isEmpty();
        }
        var f = fixture();
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(ratio(AT.minusSeconds(2)),
                ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, AT.minusSeconds(2), AT));
        assertThat(f.service.read("BTCUSDT", AT).facts()).containsOnlyKeys("longShortRatio");
        var properties = new CoinGlassProperties(); properties.setLongShortExchange("OKX");
        var wrongConfig = new AssetCardEvidenceService(f.oi, f.funding, f.ratio, f.liquidation, properties, f.macro, f.news);
        assertThat(wrongConfig.read("BTCUSDT", AT).facts()).isEmpty();
        properties.setLongShortExchange("Binance"); properties.setBaseUrl("https://untrusted.invalid");
        assertThat(wrongConfig.read("BTCUSDT", AT).facts()).isEmpty();
    }
    @Test
    void missingDatasetIsUnknownAndReadNeverCallsProviderGet() {
        var oi = mock(CoinGlassOpenInterestSnapshotService.class);
        var funding = mock(CoinGlassFundingSnapshotService.class);
        var ratio = mock(CoinGlassLongShortSnapshotService.class);
        var liquidation = mock(CoinGlassLiquidationSnapshotService.class);
        var service = new AssetCardEvidenceService(oi, funding, ratio, liquidation,
                new CoinGlassProperties(), mock(MacroEventMapper.class), mock(NewsEventMapper.class));
        var frame = service.read("BTCUSDT", Instant.parse("2026-09-10T12:00:00Z"));
        assertThat(frame.facts()).isEmpty();
        assertThat(frame.missingReasons()).containsKeys("openInterest", "fundingRate", "longShortRatio", "longLiquidation");
        assertThat(frame.eventRisk()).isNull();
        verify(oi, never()).get(anyString(), any(), any(), anyString());
        verify(funding, never()).get(anyString(), any(), any(), anyString());
        verify(ratio, never()).get(anyString(), any(), any(), anyString());
        verify(liquidation, never()).get(anyString(), any(), any(), anyString());
    }

    @Test
    void futureStaleAndUnverifiedMetadataCannotBecomeRiskEvidence() {
        var at = Instant.parse("2026-09-10T12:00:00Z");
        var metadata = new ProviderSnapshotMetadata("COINGLASS", ProviderDatasetType.COINGLASS_OPEN_INTEREST,
                "BTCUSDT", "CURRENT", at, at.plusSeconds(1), at.plusSeconds(65),
                UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, "fixture", "fixture", false, false, null, List.of());
        assertThat(AssetCardEvidenceService.usable(metadata, "BTCUSDT", at)).isFalse();
        assertThat(AssetCardEvidenceService.usable(metadata, "ETHUSDT", at.plusSeconds(2))).isFalse();
        assertThat(AssetCardEvidenceService.usable(metadata, "BTCUSDT", at.plusSeconds(2))).isFalse();
    }

    @Test
    void arbitraryFieldLabelsNeverProveMarketUnitOrWindowAndOnlyReadPeekCaches() {
        var f = fixture();
        var observed = AT.minusSeconds(2);
        var oi = oi(observed, Map.of("openInterestUsd", "fixture-oi-value", "openInterestChange1h", "fixture-oi-change"));
        when(f.oi.peek(anyString(), any(), any(), anyString())).thenReturn(result(oi, ProviderDatasetType.COINGLASS_OPEN_INTEREST, observed, AT.minusSeconds(1)));
        when(f.funding.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                new CoinGlassFundingSnapshot("BTCUSDT", new BigDecimal(".01"), observed, Map.of("weightedFundingRate", "fixture-funding")),
                ProviderDatasetType.COINGLASS_FUNDING, observed, AT.minusSeconds(1)));
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                new CoinGlassLongShortSnapshot("BTCUSDT", new BigDecimal("1.2"), "fixture-ratio", observed, Map.of("longShortRatio", "fixture-ratio")),
                ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed, AT.minusSeconds(1)));
        when(f.liquidation.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                new CoinGlassLiquidationSnapshot("BTCUSDT", null, BigDecimal.TEN, null, null, null, BigDecimal.ONE, null, null,
                        observed, Map.of("longLiquidationUsd5m", "fixture-long-liquidation", "shortLiquidationUsd5m", "fixture-short-liquidation")),
                ProviderDatasetType.COINGLASS_LIQUIDATION, observed, AT.minusSeconds(1)));
        var read = f.service.read("BTCUSDT", AT);
        assertThat(read.facts()).isEmpty();
        assertThat(read.missingReasons()).containsOnlyKeys("openInterest", "openInterestChange1h", "fundingRate", "longShortRatio", "longLiquidation", "shortLiquidation");
        assertThat(read.observations(AT)).isEmpty();
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(ratio(observed),
                ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed, AT.minusSeconds(1)));
        var verified = f.service.read("BTCUSDT", AT);
        assertThat(verified.facts()).containsOnlyKeys("longShortRatio");
        var observation=verified.observations(AT).get("longShortRatio");
        assertThat(observation.instrument()).isEqualTo("BINANCE:PERPETUAL:LINEAR:BTC/USDT");
        assertThat(observation.sourceVersion()).isEqualTo(verified.facts().get("longShortRatio").sourceVersion());
        assertThat(observation.expiresAt()).isEqualTo(verified.facts().get("longShortRatio").expiresAt());
        assertThat(observation.unit()).isEqualTo("RATIO");
        assertThat(AssetCardFeatureService.verifiedCoinGlassObservation("BTCUSDT","longShortRatio",observation,AT)).isTrue();
        assertThat(verified.observations(AT.plusSeconds(61))).isEmpty();
        verify(f.oi, never()).get(anyString(), any(), any(), anyString());
        verify(f.funding, never()).get(anyString(), any(), any(), anyString());
        verify(f.ratio, never()).get(anyString(), any(), any(), anyString());
        verify(f.liquidation, never()).get(anyString(), any(), any(), anyString());
    }

    @Test
    void datasetPayloadTimestampAndActualPayloadFieldKeysMustMatch() {
        var f = fixture();
        var observed = AT.minusSeconds(2);
        var payload = ratio(observed);
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(payload, ProviderDatasetType.COINGLASS_FUNDING, observed, AT));
        assertThat(f.service.read("BTCUSDT", AT).facts()).isEmpty();
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(payload, ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed.minusSeconds(1), AT));
        assertThat(f.service.read("BTCUSDT", AT).facts()).isEmpty();
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(new CoinGlassLongShortSnapshot("BTCUSDT", BigDecimal.ONE,
                "BINANCE_GLOBAL_ACCOUNT_RATIO", observed, Map.of("wrong-projection-key", RATIO_FIELD)), ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed, AT));
        assertThat(f.service.read("BTCUSDT", AT).facts()).isEmpty();
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(payload, ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed, AT));
        assertThat(f.service.read("BTCUSDT", AT).facts()).containsOnlyKeys("longShortRatio");
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(new CoinGlassLongShortSnapshot("BTCUSDT", BigDecimal.ONE,
                "BINANCE_GLOBAL_ACCOUNT_RATIO", observed, Map.of("longShortRatio", " ")), ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed, AT));
        assertThat(f.service.read("BTCUSDT", AT).facts()).isEmpty();
    }

    @Test
    void availabilityCannotPrecedeTheProviderObservation() {
        var f = fixture();
        var observed = AT.minusSeconds(2);
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                ratio(observed), ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO,
                observed, observed.minusSeconds(1)));
        assertThat(f.service.read("BTCUSDT", AT).facts()).isEmpty();
        assertThat(f.service.read("BTCUSDT", AT).missingReasons()).containsKey("longShortRatio");
    }

    @Test
    void eventCandidatesAreSharedAcrossSymbolsButWindowAndScopeAreRecheckedEveryRead() {
        var f = fixture();
        var event = event();
        event.setWindowEnd(utc(AT.plusMillis(500)));
        when(f.macro.selectWindowCandidates(any(), eq(500))).thenReturn(List.of(event));
        var risk = f.service.readEventRisk("BTCUSDT", AT);
        assertThat(risk.level()).isEqualTo("HIGH");
        assertThat(risk.unit()).isEqualTo("EVENT_ID");
        assertThat(risk.asOf()).isEqualTo(AT.minusSeconds(3));
        assertThat(f.service.readEventRisk("ETHUSDT", AT.plusMillis(100))).isNull();
        assertThat(f.service.readEventRisk("BTCUSDT", AT.plusMillis(600))).isNull();
        assertThat(f.service.read("BTCUSDT", AT.plusMillis(600)).eventRisk()).isNull();
        verify(f.macro, times(1)).selectWindowCandidates(any(), eq(500));
        verify(f.news, times(1)).selectWindowCandidates(any(), eq(500));
    }

    @Test
    void cancellationAndRetractionBecomeUnknownAfterAtMostOneSecondNotFixedNone() {
        for (String status : List.of("CANCELLED", "RETRACTED")) {
            var f = fixture();
            var active = event(); var withdrawn = event();
            withdrawn.setStatus(status);
            withdrawn.setUpdateTime(utc(AT.plusMillis(700)));
            when(f.macro.selectWindowCandidates(any(), eq(500))).thenReturn(List.of(active), List.of(withdrawn));
            assertThat(f.service.readEventRisk("BTCUSDT", AT)).isNotNull();
            f.nanos.set(1_000_000_000L);
            assertThat(f.service.readEventRisk("BTCUSDT", AT.plusSeconds(1))).isNull();
            assertThat(f.service.readEventRisk("ETHUSDT", AT.plusSeconds(1))).isNull();
            verify(f.macro, times(2)).selectWindowCandidates(any(), eq(500));
            verify(f.news, times(2)).selectWindowCandidates(any(), eq(500));
        }
    }

    @Test
    void eventSourceAndEveryAvailabilityTimestampMustExistAndNotBeFromTheFuture() {
        List<Consumer<MacroEventDO>> corruptions = List.of(
                e -> e.setProvider(null), e -> e.setSourceReference(null), e -> e.setUpdateTime(null),
                e -> e.setUpdateTime(utc(AT.plusSeconds(1))), e -> e.setCreateTime(utc(AT.plusSeconds(1))),
                e -> e.setSourcePublishedAt(utc(AT.plusSeconds(1))), e -> e.setSourcePublishedAtReasonCode(null),
                e -> e.setSourcePublishedAtReasonCode("MACRO_SOURCE_PUBLISHED_AT_FALLBACK_EVENT_TIME"));
        for (var corrupt : corruptions) {
            var f = fixture(); var event = event(); corrupt.accept(event);
            when(f.macro.selectWindowCandidates(any(), eq(500))).thenReturn(List.of(event));
            assertThat(f.service.readEventRisk("BTCUSDT", AT)).isNull();
        }
        var f = fixture();
        assertThat(f.service.readEventRisk("BTCUSDT", AT)).isNull();
        verifyNoInteractions(f.oi, f.funding, f.ratio, f.liquidation);
    }

    @Test
    void fullCanonicalIdentityAndProviderMappingVersionMustMatchTheInjectedMapper() {
        var f = fixture();
        var observed = AT.minusSeconds(2);
        var valid = result(ratio(observed), ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed, AT);
        for (var identity : List.of(
                new CanonicalInstrumentId("BTC", "USDC", MarketType.PERPETUAL, "BINANCE", ContractType.LINEAR),
                new CanonicalInstrumentId("BTC", "USDT", MarketType.PERPETUAL, "OTHER_VENUE", ContractType.LINEAR),
                new CanonicalInstrumentId("BTC", "USDT", MarketType.SPOT, "BINANCE", ContractType.NONE))) {
            when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(new ProviderCallResult<>(valid.payload(),
                    metadata(identity, "COINGLASS_MAPPING_V1", observed, AT), null));
            assertThat(f.service.read("BTCUSDT", AT).facts()).isEmpty();
        }
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(new ProviderCallResult<>(valid.payload(),
                metadata(valid.metadata().canonicalInstrumentId(), "old-mapping-version", observed, AT), null));
        assertThat(f.service.read("BTCUSDT", AT).facts()).isEmpty();
        var mapper = mock(CoinGlassSymbolMapper.class);
        when(mapper.map("BTCUSDT")).thenReturn(new CoinGlassSymbolMapper.CoinGlassSymbol("BTCUSDT", "BTC",
                valid.metadata().canonicalInstrumentId(), "fixture-injected-mapping"));
        var injected = new AssetCardEvidenceService(f.oi, f.funding, f.ratio, f.liquidation,
                new CoinGlassProperties(), f.macro, f.news, mapper);
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(new ProviderCallResult<>(valid.payload(),
                metadata(valid.metadata().canonicalInstrumentId(), "fixture-injected-mapping", observed, AT), null));
        assertThat(injected.read("BTCUSDT", AT).facts()).containsOnlyKeys("longShortRatio");
        verify(mapper).map("BTCUSDT");
    }

    @Test
    void eachPeekFailureOnlyRemovesItsOwnDatasetAndDoesNotEraseOtherFacts() {
        for (String failed : List.of("oi", "funding", "ratio", "liquidation")) {
            var f = fixture(); var observed = AT.minusSeconds(2);
            when(f.oi.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    oi(observed, Map.of("openInterestUsd", "fixture-oi", "openInterestChange1h", "fixture-oi-change")),
                    ProviderDatasetType.COINGLASS_OPEN_INTEREST, observed, AT));
            when(f.funding.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    new CoinGlassFundingSnapshot("BTCUSDT", BigDecimal.ONE, observed, Map.of("weightedFundingRate", "fixture-funding")),
                    ProviderDatasetType.COINGLASS_FUNDING, observed, AT));
            when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    ratio(observed),
                    ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed, AT));
            when(f.liquidation.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    new CoinGlassLiquidationSnapshot("BTCUSDT", null, BigDecimal.TEN, null, null, null, BigDecimal.ONE, null, null,
                            observed, Map.of("longLiquidationUsd5m", "fixture-long", "shortLiquidationUsd5m", "fixture-short")),
                    ProviderDatasetType.COINGLASS_LIQUIDATION, observed, AT));
            var failure = new IllegalStateException("fixture-cache-unavailable");
            switch (failed) {
                case "oi" -> when(f.oi.peek(anyString(), any(), any(), anyString())).thenThrow(failure);
                case "funding" -> when(f.funding.peek(anyString(), any(), any(), anyString())).thenThrow(failure);
                case "ratio" -> when(f.ratio.peek(anyString(), any(), any(), anyString())).thenThrow(failure);
                default -> when(f.liquidation.peek(anyString(), any(), any(), anyString())).thenThrow(failure);
            }
            var read = f.service.read("BTCUSDT", AT);
            if ("ratio".equals(failed)) assertThat(read.facts()).isEmpty();
            else assertThat(read.facts()).containsOnlyKeys("longShortRatio");
            assertThat(read.missingReasons()).isNotEmpty();
            verify(f.oi, never()).get(anyString(), any(), any(), anyString());
            verify(f.funding, never()).get(anyString(), any(), any(), anyString());
            verify(f.ratio, never()).get(anyString(), any(), any(), anyString());
            verify(f.liquidation, never()).get(anyString(), any(), any(), anyString());
        }
    }

    @Test
    void oneEventQueryFailurePreservesTheOtherRealHighRiskAndCoinGlassFactsWithoutExtraQueries() {
        for (String failed : List.of("macro", "news")) {
            var f = fixture(); var observed = AT.minusSeconds(2);
            when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    ratio(observed), ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed, AT));
            var macro = event(); macro.setEventId("fixture-macro-high");
            var news = completeEvent(new NewsEventDO()); news.setEventId("fixture-news-high");
            when(f.macro.selectWindowCandidates(any(), eq(500))).thenReturn(List.of(macro));
            when(f.news.selectWindowCandidates(any(), eq(500))).thenReturn(List.of(news));
            if ("macro".equals(failed)) {
                when(f.macro.selectWindowCandidates(any(), eq(500))).thenThrow(new IllegalStateException("fixture-macro-unavailable"));
            } else {
                when(f.news.selectWindowCandidates(any(), eq(500))).thenThrow(new IllegalStateException("fixture-news-unavailable"));
            }
            var read = f.service.read("BTCUSDT", AT);
            assertThat(read.facts()).containsOnlyKeys("longShortRatio");
            assertThat(read.eventRisk().level()).isEqualTo("HIGH");
            assertThat(read.eventRisk().evidenceValue()).isEqualTo("macro".equals(failed) ? "fixture-news-high" : "fixture-macro-high");
            assertThat(f.service.readEventRisk("BTCUSDT", AT.plusMillis(500))).isEqualTo(read.eventRisk());
            assertThat(f.service.readEventRisk("ETHUSDT", AT.plusMillis(500))).isNull();
            verify(f.macro, times(1)).selectWindowCandidates(any(), eq(500));
            verify(f.news, times(1)).selectWindowCandidates(any(), eq(500));
            verify(f.oi, never()).get(anyString(), any(), any(), anyString());
        }
    }

    @Test
    void failedEventQueriesCannotReplayAnOlderHighAssessmentOrEraseUnrelatedFacts() {
        var f = fixture(); var observed = AT.minusSeconds(2);
        when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                ratio(observed), ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, observed, AT));
        when(f.macro.selectWindowCandidates(any(), eq(500))).thenReturn(List.of(event()))
                .thenThrow(new IllegalStateException("fixture-macro-unavailable"));
        when(f.news.selectWindowCandidates(any(), eq(500))).thenThrow(new IllegalStateException("fixture-news-unavailable"));
        assertThat(f.service.read("BTCUSDT", AT).eventRisk().level()).isEqualTo("HIGH");
        f.nanos.set(1_000_000_000L);
        var read = f.service.read("BTCUSDT", AT.plusSeconds(1));
        assertThat(read.facts()).containsOnlyKeys("longShortRatio");
        assertThat(read.eventRisk()).isNull();
        assertThat(f.service.readEventRisk("BTCUSDT", AT.plusMillis(1500))).isNull();
        verify(f.macro, times(2)).selectWindowCandidates(any(), eq(500));
        verify(f.news, times(2)).selectWindowCandidates(any(), eq(500));
    }

    @Test
    void everyDatasetRejectsMissingOrDifferentWindowAndAcceptsOnlyItsOwnerTimeframe() {
        for (var dataset : List.of(ProviderDatasetType.COINGLASS_OPEN_INTEREST, ProviderDatasetType.COINGLASS_FUNDING,
                ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, ProviderDatasetType.COINGLASS_LIQUIDATION)) {
            String expected = dataset == ProviderDatasetType.COINGLASS_OPEN_INTEREST ? "CURRENT" : "1M";
            for (String wrong : Arrays.asList(null, "", "5M", "1H", expected.toLowerCase(), "CURRENT".equals(expected) ? "1M" : "CURRENT")) {
                var f = fixture();
                stubDataset(f, dataset, wrong);
                var read = f.service.read("BTCUSDT", AT);
                assertThat(read.facts()).as("%s rejects timeframe %s", dataset, wrong).isEmpty();
                assertThat(read.missingReasons()).isNotEmpty();
            }
            var f = fixture();
            stubDataset(f, dataset, expected);
            assertThat(f.service.read("BTCUSDT", AT).facts()).as("%s needs exact window plus proven market and unit", dataset)
                    .hasSize(dataset == ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO ? 1 : 0);
        }
    }

    @Test
    void cardReadPassesOriginalSixtySecondCadenceAndLeavesFreshnessExpansionToCacheOwners() {
        var f = fixture();
        f.service.read("BTCUSDT", AT);
        verify(f.oi).peek(eq("BTCUSDT"), eq(AssetPriority.P1_WATCHLIST), eq(Duration.ofSeconds(60)), eq("asset-card-read"));
        verify(f.funding).peek(eq("BTCUSDT"), eq(AssetPriority.P1_WATCHLIST), eq(Duration.ofSeconds(60)), eq("asset-card-read"));
        verify(f.ratio).peek(eq("BTCUSDT"), eq(AssetPriority.P1_WATCHLIST), eq(Duration.ofSeconds(60)), eq("asset-card-read"));
        verify(f.liquidation).peek(eq("BTCUSDT"), eq(AssetPriority.P1_WATCHLIST), eq(Duration.ofSeconds(60)), eq("asset-card-read"));
        verify(f.oi, never()).get(anyString(), any(), any(), anyString());
        verify(f.funding, never()).get(anyString(), any(), any(), anyString());
        verify(f.ratio, never()).get(anyString(), any(), any(), anyString());
        verify(f.liquidation, never()).get(anyString(), any(), any(), anyString());
    }

    private static void stubDataset(Fixture f, ProviderDatasetType dataset, String timeframe) {
        var observed = AT.minusSeconds(2);
        switch (dataset) {
            case COINGLASS_OPEN_INTEREST -> when(f.oi.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    oi(observed, Map.of("openInterestUsd", "fixture-oi", "openInterestChange1h", "fixture-oi-change")),
                    dataset, timeframe, observed, AT));
            case COINGLASS_FUNDING -> when(f.funding.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    new CoinGlassFundingSnapshot("BTCUSDT", BigDecimal.ONE, observed, Map.of("weightedFundingRate", "fixture-funding")),
                    dataset, timeframe, observed, AT));
            case COINGLASS_LONG_SHORT_RATIO -> when(f.ratio.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    ratio(observed),
                    dataset, timeframe, observed, AT));
            case COINGLASS_LIQUIDATION -> when(f.liquidation.peek(anyString(), any(), any(), anyString())).thenReturn(result(
                    new CoinGlassLiquidationSnapshot("BTCUSDT", null, BigDecimal.TEN, null, null, null, BigDecimal.ONE, null, null,
                            observed, Map.of("longLiquidationUsd5m", "fixture-long", "shortLiquidationUsd5m", "fixture-short")),
                    dataset, timeframe, observed, AT));
            default -> throw new IllegalArgumentException("Unsupported fixture dataset");
        }
    }

    private record Fixture(AssetCardEvidenceService service, CoinGlassOpenInterestSnapshotService oi,
                           CoinGlassFundingSnapshotService funding, CoinGlassLongShortSnapshotService ratio,
                           CoinGlassLiquidationSnapshotService liquidation, MacroEventMapper macro, NewsEventMapper news,
                           AtomicLong nanos) {}

    private static Fixture fixture() {
        var oi = mock(CoinGlassOpenInterestSnapshotService.class);
        var funding = mock(CoinGlassFundingSnapshotService.class);
        var ratio = mock(CoinGlassLongShortSnapshotService.class);
        var liquidation = mock(CoinGlassLiquidationSnapshotService.class);
        var macro = mock(MacroEventMapper.class); var news = mock(NewsEventMapper.class);
        var nanos = new AtomicLong();
        var service = spy(new AssetCardEvidenceService(oi, funding, ratio, liquidation, new CoinGlassProperties(), macro, news));
        doAnswer(call -> nanos.get()).when(service).monotonicNanos();
        return new Fixture(service, oi, funding, ratio, liquidation, macro, news, nanos);
    }

    private static CoinGlassOpenInterestSnapshot oi(Instant at, Map<String, String> sources) {
        return new CoinGlassOpenInterestSnapshot("BTCUSDT", BigDecimal.TEN, null, null, null, BigDecimal.ONE,
                null, List.of(), at, sources);
    }

    private static CoinGlassLongShortSnapshot ratio(Instant at) {
        return new CoinGlassLongShortSnapshot("BTCUSDT", new BigDecimal("1.2"), "BINANCE_GLOBAL_ACCOUNT_RATIO", at,
                Map.of("longShortRatio", RATIO_FIELD));
    }

    private static <T> ProviderCallResult<T> result(T payload, ProviderDatasetType dataset, Instant observed, Instant fetched) {
        return result(payload, dataset, dataset == ProviderDatasetType.COINGLASS_OPEN_INTEREST ? "CURRENT" : "1M", observed, fetched);
    }

    private static <T> ProviderCallResult<T> result(T payload, ProviderDatasetType dataset, String timeframe, Instant observed, Instant fetched) {
        var identity = new CoinGlassSymbolMapper().map("BTCUSDT");
        var metadata = new ProviderSnapshotMetadata("COINGLASS", dataset, identity.canonicalInstrumentId(), "BTCUSDT", timeframe, observed, fetched,
                AT.plusSeconds(60), 0L, UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH,
                "fixture-trace", "fixture-request", identity.sourceVersion(), true, false, null, List.of());
        return new ProviderCallResult<>(payload, metadata, null);
    }

    private static ProviderSnapshotMetadata metadata(CanonicalInstrumentId identity, String sourceVersion, Instant observed, Instant fetched) {
        return new ProviderSnapshotMetadata("COINGLASS", ProviderDatasetType.COINGLASS_LONG_SHORT_RATIO, identity,
                "BTCUSDT", "1M", observed, fetched, AT.plusSeconds(60), 0L,
                UnifiedSourceStatus.READY, SnapshotFreshnessStatus.FRESH, "fixture-trace", "fixture-request",
                sourceVersion, true, false, null, List.of());
    }

    private static MacroEventDO event() {
        var event = completeEvent(new MacroEventDO());
        event.setSourcePublishedAtReasonCode("SOURCE_PUBLISHED_AT_PROVIDED");
        return event;
    }

    private static <T extends ExternalContextEventDO> T completeEvent(T event) {
        event.setEventId("fixture-event-id"); event.setAffectedSymbols("BTCUSDT"); event.setMarketScope("CRYPTO");
        event.setWindowStart(utc(AT.minusSeconds(10))); event.setWindowEnd(utc(AT.plusSeconds(30)));
        event.setStatus("ACTIVE"); event.setSeverity("HIGH"); event.setProvider("fixture-event-provider");
        event.setSourceType("FIXTURE"); event.setSourceReference("fixture-event-reference");
        event.setSourceTraceId("fixture-event-trace"); event.setSourceEventId("fixture-source-id");
        event.setSourcePublishedAt(utc(AT.minusSeconds(3))); event.setCreateTime(utc(AT.minusSeconds(2)));
        event.setUpdateTime(utc(AT.minusSeconds(1)));
        return event;
    }
    private static LocalDateTime utc(Instant instant) { return LocalDateTime.ofInstant(instant, ZoneOffset.UTC); }
}
