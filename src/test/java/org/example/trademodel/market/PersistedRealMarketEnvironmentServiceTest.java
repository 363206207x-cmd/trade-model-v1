package org.example.trademodel.market;

import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessResult;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvReadinessStatus;
import org.example.trademodel.dto.ohlcv.PersistedOhlcvStaleReasonCode;
import org.example.trademodel.entity.PersistedOhlcvBarDO;
import org.example.trademodel.service.PersistedOhlcvQueryService;
import org.example.trademodel.service.impl.RuntimeKlineContextAssemblyServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersistedRealMarketEnvironmentServiceTest {

    @Test
    void fourFreshKrakenTimeframesPassRealMarketGate() {
        PersistedOhlcvQueryService query = allFreshKrakenQuery();
        PersistedRealMarketEnvironmentService service = service(query);

        PersistedRealMarketEnvironmentAssessment result = service.assess("BTCUSDT", "5m");

        assertThat(result.ready()).isTrue();
        assertThat(result.provider()).isEqualTo("KRAKEN");
        assertThat(result.sourceType()).isEqualTo("KRAKEN_PERSISTED_OHLCV");
        assertThat(result.timeframeContexts()).containsKeys("5m", "15m", "1h", "4h");
        assertThat(result.closedBarCount()).isEqualTo(400);
        assertThat(result.sourceTraceRefs()).isNotEmpty();
        assertThat(result.environment().getSummary()).contains("Real persisted OHLCV", "KRAKEN SPOT");
    }

    @Test
    void missingProviderDoesNotPassRealMarketGate() {
        PersistedOhlcvQueryService query = allFreshKrakenQuery();
        PersistedOhlcvReadinessResult invalid = fresh("BTCUSDT", "15m", "KRAKEN");
        invalid.getBars().forEach(bar -> bar.setProvider(null));
        when(query.evaluateReadiness(eq("BTCUSDT"), eq("15m"), eq(100), anyLong())).thenReturn(invalid);

        PersistedRealMarketEnvironmentAssessment result = service(query).assess("BTCUSDT", "5m");

        assertThat(result.ready()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("REAL_MARKET_PROVENANCE_INCOMPLETE");
    }

    @Test
    void missingSourceTraceDoesNotPassRealMarketGate() {
        PersistedOhlcvQueryService query = allFreshKrakenQuery();
        PersistedOhlcvReadinessResult invalid = fresh("BTCUSDT", "1h", "KRAKEN");
        invalid.getBars().forEach(bar -> bar.setSourceTraceId(null));
        when(query.evaluateReadiness(eq("BTCUSDT"), eq("1h"), eq(100), anyLong())).thenReturn(invalid);

        PersistedRealMarketEnvironmentAssessment result = service(query).assess("BTCUSDT", "5m");

        assertThat(result.ready()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("REAL_MARKET_PROVENANCE_INCOMPLETE");
    }

    @Test
    void staleBarsDoNotPassRealMarketGate() {
        PersistedOhlcvQueryService query = allFreshKrakenQuery();
        PersistedOhlcvReadinessResult stale = fresh("BTCUSDT", "4h", "KRAKEN");
        stale.setStatus(PersistedOhlcvReadinessStatus.STALE);
        stale.setStaleReasonCode(PersistedOhlcvStaleReasonCode.LATEST_BAR_TOO_OLD);
        stale.setMissingFields(List.of("klineFreshness"));
        when(query.evaluateReadiness(eq("BTCUSDT"), eq("4h"), eq(100), anyLong())).thenReturn(stale);

        PersistedRealMarketEnvironmentAssessment result = service(query).assess("BTCUSDT", "5m");

        assertThat(result.ready()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("MARKET_DATA_NOT_READY");
    }

    private static PersistedRealMarketEnvironmentService service(PersistedOhlcvQueryService query) {
        return new PersistedRealMarketEnvironmentService(query, new RuntimeKlineContextAssemblyServiceImpl());
    }

    private static PersistedOhlcvQueryService allFreshKrakenQuery() {
        PersistedOhlcvQueryService query = mock(PersistedOhlcvQueryService.class);
        for (String timeframe : PersistedRealMarketEnvironmentService.REQUIRED_TIMEFRAMES) {
            when(query.evaluateReadiness(eq("BTCUSDT"), eq(timeframe), eq(100), anyLong()))
                    .thenReturn(fresh("BTCUSDT", timeframe, "KRAKEN"));
        }
        return query;
    }

    private static PersistedOhlcvReadinessResult fresh(String symbol, String timeframe, String provider) {
        long interval = switch (timeframe) {
            case "5m" -> 300_000L;
            case "15m" -> 900_000L;
            case "1h" -> 3_600_000L;
            case "4h" -> 14_400_000L;
            default -> throw new IllegalArgumentException(timeframe);
        };
        List<PersistedOhlcvBarDO> bars = new ArrayList<>();
        for (int index = 99; index >= 0; index--) {
            long openTime = index * interval;
            PersistedOhlcvBarDO bar = new PersistedOhlcvBarDO();
            bar.setSymbol(symbol);
            bar.setTimeframe(timeframe);
            bar.setOpenTimeMs(openTime);
            bar.setCloseTimeMs(openTime + interval - 1);
            BigDecimal price = BigDecimal.valueOf(100 + index);
            bar.setOpenPrice(price);
            bar.setHighPrice(price.add(BigDecimal.valueOf(2)));
            bar.setLowPrice(price.subtract(BigDecimal.ONE));
            bar.setClosePrice(price.add(BigDecimal.ONE));
            bar.setVolume(BigDecimal.valueOf(1000));
            bar.setClosed(true);
            bar.setProvider(provider);
            bar.setProviderMarketType("SPOT");
            bar.setSourceEndpoint("/0/public/OHLC");
            bar.setSourceBatchId("batch-" + timeframe);
            bar.setSourceTraceId("trace-" + timeframe);
            bar.setSourceVersion(1);
            bar.setFetchTime(LocalDateTime.of(2026, 7, 13, 1, 0));
            bar.setSourceStatus("READY");
            bar.setFreshnessStatus("FRESH");
            bar.setProvenanceVersion("kraken-public-ohlc-v1");
            bar.setIngestionRunId("run-" + timeframe);
            bar.setIngestedAt(LocalDateTime.of(2026, 7, 13, 1, 0));
            bar.setQualityStatus("OK");
            bar.setIsDeleted(0);
            bars.add(bar);
        }
        PersistedOhlcvReadinessResult result = new PersistedOhlcvReadinessResult();
        result.setSymbol(symbol);
        result.setTimeframe(timeframe);
        result.setRequiredWindowSize(100);
        result.setStatus(PersistedOhlcvReadinessStatus.FRESH);
        result.setStaleReasonCode(PersistedOhlcvStaleReasonCode.NONE);
        result.setStaleReasonText("fresh real Kraken window");
        result.setMissingFields(List.of());
        result.setBars(bars);
        result.setLatestCloseTimeMs(bars.get(0).getCloseTimeMs());
        result.setLatestIngestedAt(LocalDateTime.of(2026, 7, 13, 1, 0));
        result.setManualReviewRequired(true);
        result.setNotTradeInstruction(true);
        return result;
    }
}
