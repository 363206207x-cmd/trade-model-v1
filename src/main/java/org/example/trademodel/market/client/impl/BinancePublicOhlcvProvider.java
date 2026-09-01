package org.example.trademodel.market.client.impl;

import org.example.trademodel.dto.ohlcv.OhlcvBarInput;
import org.example.trademodel.dto.ohlcv.OhlcvIngestionBatch;
import org.example.trademodel.dto.ohlcv.OhlcvSourceState;
import org.example.trademodel.dto.ohlcv.PublicKlineFetchResult;
import org.example.trademodel.dto.ohlcv.PublicOhlcvProviderResult;
import org.example.trademodel.service.PublicOhlcvProvider;
import org.example.trademodel.service.RealMarketDataFetcherService;
import org.example.trademodel.providercall.ProviderFailureClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Public Binance spot K-line adapter. No credential, account, position, or order surface is used. */
@Service
public class BinancePublicOhlcvProvider implements PublicOhlcvProvider {
    private static final Logger log = LoggerFactory.getLogger(BinancePublicOhlcvProvider.class);
    static final String SOURCE_ENDPOINT = "/api/v3/klines";
    private static final Set<String> SUPPORTED_TIMEFRAMES = Set.of("5m", "15m", "1h", "4h");
    private static final int RAW_REQUEST_HEADROOM = 2;
    private static final int MAX_REQUEST_LIMIT = 500;

    private final RealMarketDataFetcherService fetcher;
    private final boolean providerEnabled;
    private final boolean externalCallsEnabled;
    private final long settlementDelayMs;
    private final AtomicBoolean geoRestrictedCircuitOpen = new AtomicBoolean(false);

    public BinancePublicOhlcvProvider(
            RealMarketDataFetcherService fetcher,
            @Value("${trade-model.ohlcv.binance.enabled:${trade-model.ohlcv.public-provider.enabled:false}}") boolean providerEnabled,
            @Value("${trade-model.ohlcv.binance.external-calls-enabled:${trade-model.ohlcv.public-provider.external-calls-enabled:false}}") boolean externalCallsEnabled,
            @Value("${trade-model.ohlcv.freshness-tolerance-ms:30000}") long settlementDelayMs
    ) {
        this.fetcher = fetcher;
        this.providerEnabled = providerEnabled;
        this.externalCallsEnabled = externalCallsEnabled;
        this.settlementDelayMs = Math.max(0L, settlementDelayMs);
    }

    @Override
    public PublicOhlcvProviderResult fetchClosedBars(
            String symbol,
            String timeframe,
            int limit,
            String ingestionRunId
    ) {
        if (geoRestrictedCircuitOpen.get()) {
            return result(OhlcvSourceState.ERROR, "REGION_RESTRICTED", null);
        }
        if (!providerEnabled) {
            return result(OhlcvSourceState.DISABLED, "PUBLIC_OHLCV_PROVIDER_DISABLED", null);
        }
        if (!externalCallsEnabled) {
            return result(OhlcvSourceState.NOT_CONFIGURED, "PUBLIC_OHLCV_EXTERNAL_CALL_NOT_ENABLED", null);
        }
        if (symbol == null || symbol.isBlank() || !SUPPORTED_TIMEFRAMES.contains(timeframe)) {
            return result(OhlcvSourceState.DEGRADED, "PUBLIC_OHLCV_REQUEST_INVALID", null);
        }
        if (limit <= 0 || limit > MAX_REQUEST_LIMIT) {
            return result(OhlcvSourceState.DEGRADED, "PUBLIC_OHLCV_LIMIT_INVALID", null);
        }

        int rawRequestLimit = Math.min(limit + RAW_REQUEST_HEADROOM, MAX_REQUEST_LIMIT);
        PublicKlineFetchResult fetched = fetcher.fetchKlinesDetailed(symbol, timeframe, rawRequestLimit);
        if (fetched == null) {
            return result(OhlcvSourceState.ERROR, "PUBLIC_OHLCV_PROVIDER_RESULT_MISSING", null);
        }
        if (fetched.sourceState() != OhlcvSourceState.READY) {
            if (ProviderFailureClassifier.isRegionRestricted(fetched.httpStatus(), fetched.reasonCode())) {
                geoRestrictedCircuitOpen.set(true);
                log.warn("BINANCE_REGION_RESTRICTED symbol={} reasonCode={}", symbol, fetched.reasonCode());
                return result(OhlcvSourceState.ERROR, "REGION_RESTRICTED", null);
            }
            return result(fetched.sourceState(), fetched.reasonCode(), null);
        }

        List<OhlcvBarInput> bars = new ArrayList<>();
        try {
            long fetchTimeMs = fetched.fetchTime().toEpochMilli();
            for (String[] row : fetched.rows()) {
                if (row == null || row.length < 11) {
                    return result(OhlcvSourceState.DEGRADED, "PUBLIC_OHLCV_ROW_MALFORMED", null);
                }
                long closeTimeMs = Long.parseLong(row[6]);
                if (closeTimeMs > fetchTimeMs || fetchTimeMs - closeTimeMs < settlementDelayMs) {
                    continue;
                }
                bars.add(new OhlcvBarInput(
                        symbol.trim().toUpperCase(Locale.ROOT),
                        timeframe,
                        Long.parseLong(row[0]),
                        closeTimeMs,
                        decimal(row[1]),
                        decimal(row[2]),
                        decimal(row[3]),
                        decimal(row[4]),
                        decimal(row[5]),
                        decimal(row[7]),
                        Long.parseLong(row[8]),
                        decimal(row[9]),
                        decimal(row[10]),
                        true));
            }
        } catch (RuntimeException e) {
            return result(OhlcvSourceState.DEGRADED, "PUBLIC_OHLCV_ROW_PARSE_FAILED", null);
        }
        if (bars.size() < limit) {
            return result(OhlcvSourceState.WAITING_SYNC,
                    "PUBLIC_OHLCV_INSUFFICIENT_SETTLED_BARS", null);
        }

        bars.sort(Comparator.comparingLong(OhlcvBarInput::closeTimeMs)
                .thenComparingLong(OhlcvBarInput::openTimeMs));
        if (bars.size() > limit) {
            bars = new ArrayList<>(bars.subList(bars.size() - limit, bars.size()));
        }

        OhlcvIngestionBatch batch = new OhlcvIngestionBatch(
                "BINANCE_PUBLIC",
                "SPOT",
                SOURCE_ENDPOINT,
                OhlcvSourceState.READY,
                fetched.fetchTime(),
                "binance-public-kline-v1",
                1,
                ingestionRunId,
                ingestionRunId,
                bars);
        return result(OhlcvSourceState.READY, null, batch);
    }

    public boolean isGeoRestrictedCircuitOpen() {
        return geoRestrictedCircuitOpen.get();
    }

    private static BigDecimal decimal(String value) {
        return new BigDecimal(value);
    }

    private static PublicOhlcvProviderResult result(
            OhlcvSourceState state,
            String reason,
            OhlcvIngestionBatch batch
    ) {
        return new PublicOhlcvProviderResult(state, reason, batch);
    }
}
